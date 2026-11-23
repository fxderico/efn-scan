package net.efn.client.module.impl.misc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.StaffGhost;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.util.NameFilter;
import net.efn.client.util.antivanish.AntiVanishHeuristics;
import net.efn.client.util.antivanish.AntiVanishText;
import net.efn.client.util.staff.ShardTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket.Operation;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

@Environment(EnvType.CLIENT)
public final class AntiVanishModule extends ClientModule {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static final int SENSOR_RANGE = 64;
   private static final long SIGNAL_WINDOW_MS = 15000L;
   private static final long DETECTION_TTL_MS = 20000L;
   private static final long CRITICAL_COOLDOWN_MS = 12000L;
   private static final long ANNOUNCE_COOLDOWN_MS = 1500L;
   private static final long ANNOUNCE_WINDOW_MS = 4000L;
   private static final int MAX_ANNOUNCE_PER_WINDOW = 6;
   private static final int MAX_OBSERVATIONS_PER_TICK = 512;
   private static final int CRITICAL_SCORE = 35;
   private static final long SELF_PLACE_TTL_MS = 6000L;
   private static final long CONTAINER_SELF_GRACE_MS = 2500L;
   private static final long SPELL_SETTLE_MS = 1000L;
   private static final long GOLIATH_QUIET_MS = 4000L;
   private static final long HINT_HOLD_MS = 1000L;
   private static final long TAB_PAIR_MS = 1000L;
   private static final long TEAM_BATCH_MS = 50L;
   private static final int TEAM_BATCH = 5;
   private static final long HINT_MEMORY_MS = 5000L;
   private static final long ARRIVAL_REPEAT_MS = 60000L;
   private static final int STAFF_RANK_MAX = 8;
   private static final Pattern RANK_TEAM = Pattern.compile("(\\d{2})-(.+)-(?:true|false)");
   private static final Set<Action> ORDER_ONLY = EnumSet.of(Action.UPDATE_DISPLAY_NAME, Action.UPDATE_LIST_ORDER);
   private final ConcurrentLinkedQueue<AntiVanishModule.Hint> hints = new ConcurrentLinkedQueue<>();
   private final List<AntiVanishModule.Hint> heldHints = new ArrayList<>();
   private final Deque<AntiVanishModule.Hint> tabMoves = new ArrayDeque<>();
   private final Deque<Long> teamChanges = new ArrayDeque<>();
   private final Map<UUID, String> tabNames = new HashMap<>();
   private final Map<String, Long> arrivals = new HashMap<>();
   private volatile long quietUntilMs;
   private final ConcurrentLinkedQueue<AntiVanishModule.Observation> observations = new ConcurrentLinkedQueue<>();
   private final Map<UUID, AntiVanishModule.KnownPlayer> knownPlayers = new HashMap<>();
   private final Map<String, AntiVanishModule.Detection> detections = new LinkedHashMap<>();
   private final Map<String, Long> signalCooldowns = new HashMap<>();
   private final Map<String, Long> announceCooldowns = new HashMap<>();
   private final Deque<Long> announceTimes = new ArrayDeque<>();
   private final Map<Long, Long> selfPlacedBlocks = new ConcurrentHashMap<>();
   private final Map<UUID, Long> confirmedDepartures = new HashMap<>();
   private final Map<Long, Long> automatedMechanisms = new HashMap<>();
   private final Map<String, Deque<Long>> weakParticleBursts = new HashMap<>();
   private final Deque<AntiVanishModule.Signal> signals = new ArrayDeque<>();
   private final Deque<AntiVanishModule.ExplosionEvent> recentExplosions = new ArrayDeque<>();
   private final Deque<AntiVanishModule.PendingVanish> pendingVanishes = new ArrayDeque<>();
   private final Map<String, AntiVanishModule.HiddenSpell> hiddenSpells = new LinkedHashMap<>();
   private final Deque<AntiVanishModule.RecentMessage> recentMessages = new ArrayDeque<>();
   private boolean serverSendsLeaveMessages;
   private static final long RECENT_MESSAGE_TTL_MS = 8000L;
   private static final long TRUSTED_LISTED_MS = 1500L;
   private final Map<UUID, Long> listedSinceMs = new HashMap<>();
   private final ShardTracker shard = new ShardTracker();
   private int tickCounter;
   private volatile long lastLocalActionMs;
   private long lastContainerActivityMs;
   private long lastCriticalMs;
   private long criticalUntilMs;
   private String criticalWatcher = "";
   private final Set<UUID> pendingChimes = new HashSet<>();
   private UUID ghostId;
   private String ghostName;

   public AntiVanishModule() {
      super("anti-vanish", "Anti Vanish", "Detects vanished players.", ModuleCategory.DONUT);
      this.usesNotifications();
   }

   @Override
   protected void onEnable() {
      this.resetRuntime();
      if (MC.player != null && MC.world != null) {
         this.trackListedPlayers();
      }
   }

   @Override
   protected void onDisable() {
      this.resetRuntime();
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      if (MC.player != null && MC.world != null && MC.getNetworkHandler() != null) {
         if (this.shard.moved(MC)) {
            this.resetRuntime();
         }

         this.playPendingChime();
         this.tickCounter++;
         if (MC.player.currentScreenHandler != MC.player.playerScreenHandler) {
            this.lastContainerActivityMs = System.currentTimeMillis();
         }

         this.drainObservations();
         this.processPendingVanishes();
         this.judgeHints();
         if (this.tickCounter % 10 == 0) {
            this.trackListedPlayers();
            this.reviewSpells();
         }

         if (this.tickCounter % 5 == 0) {
            this.scanInvisiblePlayers();
         }

         this.pruneState();
      } else {
         this.resetRuntime();
      }
   }

   @EventHandler
   public void onPacketSend(PacketSendEvent packetSendEvent) {
      this.handleOutbound(packetSendEvent.packet);
   }

   private void handleOutbound(Packet<?> packet) {
      if (packet != null) {
         if (packet instanceof PlayerInteractBlockC2SPacket var2) {
            this.lastLocalActionMs = System.currentTimeMillis();
            if (var2.getBlockHitResult() != null && var2.getBlockHitResult().getBlockPos() != null) {
               long var3 = System.currentTimeMillis() + 6000L;
               BlockPos var5 = var2.getBlockHitResult().getBlockPos();
               BlockPos var6 = var5.offset(var2.getBlockHitResult().getSide());
               this.selfPlacedBlocks.put(var5.asLong(), var3);
               this.selfPlacedBlocks.put(var6.asLong(), var3);
               String var7 = "";
               if (MC.player != null && var2.getHand() != null) {
                  Identifier var8 = Registries.ITEM.getId(MC.player.getStackInHand(var2.getHand()).getItem());
                  var7 = AntiVanishHeuristics.path(var8 == null ? "" : var8.toString());
               }

               markSelfMultiBlockFootprint(var5, var7, this.selfPlacedBlocks, var3);
               markSelfMultiBlockFootprint(var6, var7, this.selfPlacedBlocks, var3);
            }
         } else {
            if (packet instanceof PlayerActionC2SPacket
               || packet instanceof PlayerInteractItemC2SPacket
               || packet instanceof PlayerInteractEntityC2SPacket
               || packet instanceof HandSwingC2SPacket) {
               this.lastLocalActionMs = System.currentTimeMillis();
            }
         }
      }
   }

   static void markSelfMultiBlockFootprint(BlockPos blockPos, String string, Map<Long, Long> map, long l) {
      if (blockPos != null && map != null) {
         map.put(blockPos.asLong(), l);
         String var6 = string == null ? "" : string;
         boolean var5 = var6.endsWith("_door") && !var6.endsWith("trapdoor")
            || var6.contains("sunflower")
            || var6.contains("lilac")
            || var6.contains("rose_bush")
            || var6.contains("peony")
            || var6.contains("tall_grass")
            || var6.contains("large_fern")
            || var6.contains("pitcher_plant");
         if (var5) {
            map.put(blockPos.up().asLong(), l);
            map.put(blockPos.down().asLong(), l);
         }

         if (var6.endsWith("_bed")) {
            map.put(blockPos.add(1, 0, 0).asLong(), l);
            map.put(blockPos.add(-1, 0, 0).asLong(), l);
            map.put(blockPos.add(0, 0, 1).asLong(), l);
            map.put(blockPos.add(0, 0, -1).asLong(), l);
         }
      }
   }

   @EventHandler
   public void onPacketReceiveNetty(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      if (this.handleInbound(packetReceiveNettyEvent.packet)) {
         packetReceiveNettyEvent.setCancelled(true);
      }
   }

   private boolean handleInbound(Packet<?> packet) {
      if (packet instanceof BundleS2CPacket var15) {
         for (Packet var24 : var15.getPackets()) {
            this.handleInbound(var24);
         }

         return false;
      } else {
         if (packet instanceof PlayerRemoveS2CPacket var2) {
            if (var2.profileIds().size() < 4) {
               for (UUID var4 : var2.profileIds()) {
                  this.observations.offer(AntiVanishModule.Observation.tabRemove(var4));
               }
            }

            long var16 = System.currentTimeMillis();

            for (UUID var6 : var2.profileIds()) {
               this.hints.offer(new AntiVanishModule.Hint(AntiVanishModule.HintKind.TAB_OUT, var6, null, -1, var16));
            }
         } else if (packet instanceof PlayerListS2CPacket var7) {
            this.queueTabHints(var7);
            if (var7.getActions().contains(Action.UPDATE_LISTED)) {
               int var17 = 0;

               for (Entry var25 : var7.getEntries()) {
                  if (var25 != null && !var25.listed()) {
                     var17++;
                  }
               }

               if (var17 > 0 && var17 < 4) {
                  for (Entry var26 : var7.getEntries()) {
                     if (var26 != null && !var26.listed()) {
                        this.observations.offer(AntiVanishModule.Observation.tabHide(var26.profileId()));
                     }
                  }
               }
            }

            if (var7.getActions().contains(Action.UPDATE_GAME_MODE)) {
               this.queueGamemodeUpdates(var7);
            }
         } else if (packet instanceof GameMessageS2CPacket var23) {
            String var18 = var23.content() == null ? "" : var23.content().getString();
            if (!var18.isBlank()) {
               this.observations.offer(AntiVanishModule.Observation.systemChat(var18));
            }

            String var8;
            if (!(var8 = departedPlayerName(var23.content())).isBlank()) {
               this.observations.offer(AntiVanishModule.Observation.playerLeft(var8));
            }
         } else if (packet instanceof EntityTrackerUpdateS2CPacket var9) {
            this.observations.offer(AntiVanishModule.Observation.entityMetadata(var9.id()));
         } else if (packet instanceof ExplosionS2CPacket var10) {
            this.observations.offer(AntiVanishModule.Observation.explosion(var10.center(), var10.radius()));
         } else if (packet instanceof PlaySoundFromEntityS2CPacket var11) {
            this.observations.offer(AntiVanishModule.Observation.entitySound(var11.getEntityId(), soundId(var11.getSound().value().id())));
         } else if (packet instanceof ParticleS2CPacket var12) {
            Identifier var19 = Registries.PARTICLE_TYPE.getId(var12.getParameters().getType());
            this.observations
               .offer(
                  AntiVanishModule.Observation.position(
                     AntiVanishModule.ObservationType.PARTICLE, var12.getX(), var12.getY(), var12.getZ(), var19 == null ? "" : var19.toString()
                  )
               );
         } else if (packet instanceof PlaySoundS2CPacket var13) {
            this.onSoundPacket(var13);
         } else if (packet instanceof TeamS2CPacket var14) {
            this.queueTeamHint(var14);
         }

         return false;
      }
   }

   private void queueTabHints(PlayerListS2CPacket playerListS2CPacket) {
      long var2 = System.currentTimeMillis();
      if (!playerListS2CPacket.getActions().contains(Action.ADD_PLAYER)) {
         if (ORDER_ONLY.equals(playerListS2CPacket.getActions()) && playerListS2CPacket.getEntries().size() < 4) {
            for (Entry var5 : playerListS2CPacket.getEntries()) {
               if (var5 != null && var5.profileId() != null) {
                  this.hints.offer(new AntiVanishModule.Hint(AntiVanishModule.HintKind.ORDER, var5.profileId(), null, -1, var2));
               }
            }
         }
      } else {
         for (Entry var7 : playerListS2CPacket.getEntries()) {
            if (var7 != null && var7.profile() != null && var7.profile().name() != null) {
               this.hints.offer(new AntiVanishModule.Hint(AntiVanishModule.HintKind.TAB_IN, var7.profileId(), var7.profile().name(), -1, var2));
            }
         }
      }
   }

   private void queueTeamHint(TeamS2CPacket teamS2CPacket) {
      Operation var4 = teamS2CPacket.getTeamOperation();
      Matcher var3 = var4 == null ? null : RANK_TEAM.matcher(teamS2CPacket.getTeamName());
      if (var3 != null && var3.matches()) {
         String var6 = var3.group(2);
         boolean var2 = var4 == Operation.ADD;
         if (!var2 || teamS2CPacket.getPlayerNames().isEmpty() || !teamS2CPacket.getPlayerNames().stream().noneMatch(var6::equalsIgnoreCase)) {
            this.hints
               .offer(
                  new AntiVanishModule.Hint(
                     var2 ? AntiVanishModule.HintKind.TEAM_UP : AntiVanishModule.HintKind.TEAM_DOWN,
                     null,
                     var6,
                     Integer.parseInt(var3.group(1)),
                     System.currentTimeMillis()
                  )
               );
         }
      }
   }

   private void judgeHints() {
      AntiVanishModule.Hint var1;
      while ((var1 = this.hints.poll()) != null) {
         switch (var1.kind()) {
            case ORDER:
               this.heldHints.add(var1);
               break;
            case TEAM_UP:
            case TEAM_DOWN:
               this.teamChanges.addLast(var1.atMs());
               this.heldHints.add(var1);
               break;
            case TAB_IN:
               this.tabMoves.addLast(var1);
               break;
            case TAB_OUT:
               String var2 = this.tabNames.get(var1.id());
               if (var2 != null) {
                  this.tabMoves.addLast(new AntiVanishModule.Hint(AntiVanishModule.HintKind.TAB_OUT, var1.id(), var2, -1, var1.atMs()));
               }
         }
      }

      long var7 = System.currentTimeMillis();
      Iterator var4 = this.heldHints.iterator();

      while (var4.hasNext()) {
         AntiVanishModule.Hint var5 = (AntiVanishModule.Hint)var4.next();
         if (var7 - var5.atMs() >= 1000L) {
            var4.remove();
            this.judge(var5);
         }
      }

      while (!this.tabMoves.isEmpty() && var7 - this.tabMoves.peekFirst().atMs() > 5000L) {
         this.tabMoves.removeFirst();
      }

      while (!this.teamChanges.isEmpty() && var7 - this.teamChanges.peekFirst() > 5000L) {
         this.teamChanges.removeFirst();
      }

      if (this.tabNames.size() > 4096) {
         this.tabNames.clear();
      }

      for (PlayerListEntry var6 : MC.getNetworkHandler().getPlayerList()) {
         if (var6.getProfile() != null && var6.getProfile().name() != null) {
            this.tabNames.put(var6.getProfile().id(), var6.getProfile().name());
         }
      }
   }

   private void judge(AntiVanishModule.Hint hint) {
      if (hint.atMs() >= this.quietUntilMs) {
         PlayerListEntry var3 = hint.id() != null
            ? MC.getNetworkHandler().getPlayerListEntry(hint.id())
            : MC.getNetworkHandler().getCaseInsensitivePlayerInfo(hint.name());
         String var2 = hint.name() != null ? hint.name() : (var3 != null && var3.getProfile() != null ? var3.getProfile().name() : null);
         if (var2 != null && !var2.equalsIgnoreCase(MC.player.getGameProfile().name())) {
            int var5 = hint.rank() >= 0 ? hint.rank() : teamRank(var3);
            if (StaffListModule.isStaffName(var2) || var5 >= 0 && var5 <= 8) {
               UUID var6 = var3 != null && var3.getProfile() != null ? var3.getProfile().id() : hint.id();
               switch (hint.kind()) {
                  case ORDER:
                     this.arrived(var6, var2, "tab re-sort");
                     break;
                  case TEAM_UP:
                     if (this.teamOnly(hint)) {
                        this.arrived(var6, var2, "team created");
                     }
                     break;
                  case TEAM_DOWN:
                     if (this.teamOnly(hint)) {
                        this.departed(var2, "team removed");
                     }
               }
            }
         }
      }
   }

   private boolean teamOnly(AntiVanishModule.Hint hint) {
      int var2 = 0;

      for (long var4 : this.teamChanges) {
         if (Math.abs(var4 - hint.atMs()) <= 50L) {
            if (++var2 >= 5) {
               return false;
            }
         }
      }

      for (AntiVanishModule.Hint var5 : this.tabMoves) {
         if (var5.name().equalsIgnoreCase(hint.name()) && Math.abs(var5.atMs() - hint.atMs()) <= 1000L) {
            return false;
         }
      }

      return true;
   }

   private static int teamRank(PlayerListEntry playerListEntry) {
      Team var1 = playerListEntry == null ? null : playerListEntry.getScoreboardTeam();
      Matcher var2 = var1 == null ? null : RANK_TEAM.matcher(var1.getName());
      return var2 != null && var2.matches() ? Integer.parseInt(var2.group(1)) : -1;
   }

   private void arrived(UUID uUID, String string, String string2) {
      long var4 = System.currentTimeMillis();
      Long var6 = this.arrivals.put(spellKey(string), var4);
      if (uUID != null) {
         StaffListModule.provenHere(uUID, string);
      }

      if (var6 == null || var4 - var6 >= 60000L) {
         this.news(string, ModuleChat.body().text("is ").danger("on your goliath").build(), string2);
         this.watchingYou(string, new AntiVanishModule.RegionProof(uUID, -1));
      }
   }

   private void departed(String string, String string2) {
      if (this.arrivals.remove(spellKey(string)) != null) {
         this.news(string, ModuleChat.body().good("left your goliath").build(), string2);
      }
   }

   private void onSoundPacket(PlaySoundS2CPacket playSoundS2CPacket) {
      if (playSoundS2CPacket != null && playSoundS2CPacket.getSound() != null && playSoundS2CPacket.getSound().value() != null) {
         String var2 = soundId(playSoundS2CPacket.getSound().value().id());
         this.observations
            .offer(
               AntiVanishModule.Observation.position(
                  AntiVanishModule.ObservationType.POSITIONAL_SOUND, playSoundS2CPacket.getX(), playSoundS2CPacket.getY(), playSoundS2CPacket.getZ(), var2
               )
            );
      }
   }

   public static boolean shouldShowHud() {
      AntiVanishModule var0 = instance();
      return var0 != null && var0.isEnabled() && var0.hasHudContent();
   }

   private boolean hasHudContent() {
      long var1 = System.currentTimeMillis();
      if (var1 < this.criticalUntilMs) {
         return true;
      } else {
         for (AntiVanishModule.Detection var4 : this.detections.values()) {
            if (var4.expiresAt > var1 && detectionWorthShowing(var4)) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean detectionWorthShowing(AntiVanishModule.Detection detection) {
      if (detection == null) {
         return false;
      } else {
         String var1 = detection.name == null ? "" : detection.name.trim();
         return !var1.isBlank() && !"Unknown".equalsIgnoreCase(var1) && !"You".equalsIgnoreCase(var1) && !"CRITICAL".equalsIgnoreCase(var1);
      }
   }

   public static String hudTag(AntiVanishModule.HudEntry hudEntry) {
      if (hudEntry == null) {
         return "WATCH";
      } else if ("CRITICAL".equalsIgnoreCase(hudEntry.name())) {
         return "ALERT";
      } else {
         String var1 = hudEntry.reason() == null ? "" : hudEntry.reason().toLowerCase(Locale.ROOT);
         if (var1.startsWith("vanish event")) {
            return "VANISH";
         } else if (var1.startsWith("invisible entity")) {
            return "INVIS";
         } else if (var1.startsWith("suspicious sound")) {
            return "SOUND";
         } else {
            return var1.startsWith("ghost particle") ? "PARTICLE" : "WATCH";
         }
      }
   }

   public static String hudValue(AntiVanishModule.HudEntry hudEntry) {
      if (hudEntry == null) {
         return "Staff";
      } else if ("CRITICAL".equalsIgnoreCase(hudEntry.name())) {
         return shortName(hudEntry.reason());
      } else {
         String var1 = hudEntry.name() == null ? "" : hudEntry.name().trim();
         return !var1.isBlank() && !"Unknown".equalsIgnoreCase(var1) && !"You".equalsIgnoreCase(var1) ? shortName(var1) : "Staff";
      }
   }

   private static String shortName(String string) {
      String var1 = string == null ? "" : NameFilter.apply(string.trim());
      return var1.length() <= 16 ? var1 : var1.substring(0, 16);
   }

   public static List<AntiVanishModule.HudEntry> hudEntries() {
      AntiVanishModule var0 = instance();
      return var0 != null && shouldShowHud() ? var0.hudSnapshot() : List.of();
   }

   private static AntiVanishModule instance() {
      return ModuleRegistry.get(AntiVanishModule.class);
   }

   private void drainObservations() {
      AntiVanishModule.Observation var1;
      for (int var2 = 0; var2 < 512 && (var1 = this.observations.poll()) != null; var2++) {
         this.processObservation(var1);
      }

      while (this.observations.size() > 4096) {
         this.observations.poll();
      }
   }

   private void processObservation(AntiVanishModule.Observation observation) {
      switch (observation.type) {
         case TAB_REMOVE:
            this.handleTabRemoval(observation.profileId);
            break;
         case TAB_HIDE:
            this.handleTabHidden(observation.profileId);
            break;
         case PLAYER_LEFT:
            this.serverSendsLeaveMessages = true;
            this.confirmDeparture(observation.detail);
            break;
         case SYSTEM_CHAT:
            this.cacheRecentMessage(observation.detail);
            break;
         case ENTITY_METADATA:
            this.inspectInvisibleEntity(observation.entityId);
            break;
         case POSITIONAL_SOUND:
            this.inspectPositionalSound(observation);
            break;
         case ENTITY_SOUND:
            this.inspectEntitySound(observation);
            break;
         case PARTICLE:
            this.inspectParticle(observation);
            break;
         case EXPLOSION:
            this.rememberExplosion(observation);
            break;
         case GAMEMODE:
            this.announceGamemode(observation.profileId, observation.detail);
      }
   }

   private void trackListedPlayers() {
      if (MC.getNetworkHandler() != null) {
         long var1 = System.currentTimeMillis();

         for (PlayerListEntry var4 : MC.getNetworkHandler().getListedPlayerListEntries()) {
            if (var4 != null && var4.getProfile() != null && var4.getProfile().id() != null) {
               UUID var5 = var4.getProfile().id();
               if (MC.player == null || !MC.player.getUuid().equals(var5)) {
                  this.listedSinceMs.putIfAbsent(var5, var1);
                  String var6 = var4.getProfile().name();
                  if (var6 != null && !this.knownPlayers.containsKey(var5)) {
                     this.knownPlayers.put(var5, new AntiVanishModule.KnownPlayer(var5, var6));
                  }

                  if (var6 != null && !this.hiddenSpells.isEmpty()) {
                     this.endSpell(var6, false, 1000L);
                  }
               }
            }
         }

         if (this.listedSinceMs.size() > 1024 || this.knownPlayers.size() > 1024) {
            HashSet var7 = new HashSet();

            for (PlayerListEntry var9 : MC.getNetworkHandler().getPlayerList()) {
               if (var9 != null && var9.getProfile() != null) {
                  var7.add(var9.getProfile().id());
               }
            }

            this.listedSinceMs.keySet().retainAll(var7);
            this.knownPlayers.keySet().retainAll(var7);
         }
      }
   }

   private boolean trustedListed(UUID uUID) {
      Long var2 = uUID == null ? null : this.listedSinceMs.get(uUID);
      return var2 != null && System.currentTimeMillis() - var2 >= 1500L;
   }

   private boolean credibleSubject(UUID uUID, String string) {
      return uUID != null && AntiVanishText.isUsername(string) && StaffListModule.isStaffName(string) && this.trustedListed(uUID);
   }

   private void handleTabRemoval(UUID uUID) {
      if (uUID != null && !MC.player.getUuid().equals(uUID)) {
         String var2 = this.knownName(uUID);
         if (this.credibleSubject(uUID, var2) && AntiVanishText.isPlausiblePlayerName(var2)) {
            this.pendingVanishes.removeIf(pendingVanish -> pendingVanish.uuid.equals(uUID));
            this.pendingVanishes.addLast(new AntiVanishModule.PendingVanish(uUID, var2, this.tickCounter + 20));
         }
      }
   }

   private void handleTabHidden(UUID uUID) {
      if (uUID != null && !MC.player.getUuid().equals(uUID)) {
         AntiVanishModule.KnownPlayer var3 = this.knownPlayers.get(uUID);
         String var2 = var3 != null && var3.name != null ? var3.name : this.knownName(uUID);
         if (this.credibleSubject(uUID, var2) && AntiVanishText.isPlausiblePlayerName(var2)) {
            if (!this.recentMessageNames(var2)) {
               this.reportHidden(uUID, var2, false, "Vanish Event: hidden from TAB", "hidden from TAB", 100);
            }
         }
      }
   }

   private String knownName(UUID uUID) {
      AntiVanishModule.KnownPlayer var3 = this.knownPlayers.get(uUID);
      if (var3 != null && var3.name != null && !var3.name.isBlank()) {
         return var3.name;
      } else {
         PlayerListEntry var2;
         return MC.getNetworkHandler() != null && (var2 = MC.getNetworkHandler().getPlayerListEntry(uUID)) != null && var2.getProfile() != null
            ? var2.getProfile().name()
            : null;
      }
   }

   private void confirmDeparture(String string) {
      long var2 = System.currentTimeMillis();

      for (AntiVanishModule.KnownPlayer var5 : this.knownPlayers.values()) {
         if (AntiVanishText.containsPlayerName(string, var5.name)) {
            this.confirmedDepartures.put(var5.uuid, var2 + 5000L);
            this.pendingVanishes.removeIf(pendingVanish -> pendingVanish.uuid.equals(var5.uuid));
            this.hiddenSpells.remove(spellKey(var5.name));
            AntiVanishModule.Detection var6 = this.detections.get(var5.uuid.toString());
            if (var6 != null && tabDepartureReason(var6.reason)) {
               this.detections.remove(var5.uuid.toString());
            }

            this.signals
               .removeIf(
                  signal -> signal.type == AntiVanishModule.SignalType.VANISH
                     && signal.subject.equalsIgnoreCase(var5.name)
                     && tabDepartureReason(signal.reason)
               );
         }
      }
   }

   private void processPendingVanishes() {
      while (!this.pendingVanishes.isEmpty() && this.pendingVanishes.peekFirst().dueTick <= this.tickCounter) {
         AntiVanishModule.PendingVanish var1 = this.pendingVanishes.removeFirst();
         if (MC.getNetworkHandler().getPlayerListEntry(var1.uuid) == null) {
            long var2 = System.currentTimeMillis();
            if (this.confirmedDepartures.getOrDefault(var1.uuid, 0L) <= var2) {
               if (this.recentMessageNames(var1.name)) {
                  this.serverSendsLeaveMessages = true;
               } else {
                  PlayerEntity var4 = MC.world.getPlayerByUuid(var1.uuid);
                  if (var4 != null && !var4.isRemoved()) {
                     this.reportHidden(var1.uuid, var1.name, false, "Vanish Event: entity remained", "off TAB, still nearby", 100);
                  } else {
                     String var5 = "Vanish Event: silent TAB disappearance";
                     int var6 = silentTabRemovalScore(this.serverSendsLeaveMessages);
                     if (this.serverSendsLeaveMessages) {
                        this.reportHidden(var1.uuid, var1.name, false, var5, "left TAB silently", var6);
                     } else {
                        AntiVanishModule.RegionProof var7 = this.regionProof(var1.uuid);
                        if (var7 != null) {
                           this.signals.addLast(new AntiVanishModule.Signal(AntiVanishModule.SignalType.VANISH, var1.name, var7, var5, var6, var2));
                           this.evaluateCritical();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static int silentTabRemovalScore(boolean bl) {
      return bl ? 100 : 30;
   }

   static boolean tabDepartureReason(String string) {
      if (string == null) {
         return false;
      } else {
         String var1 = string.toLowerCase(Locale.ROOT);
         return var1.startsWith("vanish event:") && (var1.contains("tab") || var1.contains("entity remained") || var1.contains("no leave packet"));
      }
   }

   private void cacheRecentMessage(String string) {
      if (string != null && !string.isBlank()) {
         long var2 = System.currentTimeMillis();
         this.recentMessages.addLast(new AntiVanishModule.RecentMessage(string, var2));

         while (!this.recentMessages.isEmpty() && (var2 - this.recentMessages.peekFirst().atMs > 8000L || this.recentMessages.size() > 64)) {
            this.recentMessages.removeFirst();
         }
      }
   }

   private boolean recentMessageNames(String string) {
      if (string != null && !string.isBlank()) {
         long var2 = System.currentTimeMillis();

         for (AntiVanishModule.RecentMessage var5 : this.recentMessages) {
            if (var2 - var5.atMs <= 8000L && AntiVanishText.looksLikeLeaveMessage(var5.text, string)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static String departedPlayerName(Text text) {
      TranslatableTextContent var2;
      TextContent var3;
      if (text != null
         && (var3 = text.getContent()) instanceof TranslatableTextContent
         && "multiplayer.player.left".equals((var2 = (TranslatableTextContent)var3).getKey())) {
         Object[] var4 = var2.getArgs();
         if (var4.length != 0 && var4[0] != null) {
            String var1;
            if (var4[0] instanceof Text var6) {
               var1 = var6.getString();
            } else {
               var1 = String.valueOf(var4[0]);
            }

            return var1;
         } else {
            return "";
         }
      } else {
         return "";
      }
   }

   private void reportHidden(UUID uUID, String string, boolean bl, String string2, String string3, int i) {
      this.upsertDetection(detectionKey(uUID, string), string, string2, i, System.currentTimeMillis() + 20000L);
      AntiVanishModule.HiddenSpell var7 = this.hiddenSpells.get(spellKey(string));
      if (var7 == null || var7.spectator != bl) {
         this.hiddenSpells.put(spellKey(string), new AntiVanishModule.HiddenSpell(uUID, string, bl, string3, this.regionProof(uUID), this.tickCounter));
      }
   }

   private void reviewSpells() {
      Iterator var1 = this.hiddenSpells.values().iterator();

      while (var1.hasNext()) {
         AntiVanishModule.HiddenSpell var2 = (AntiVanishModule.HiddenSpell)var1.next();
         if (var2.spectator && MC.getNetworkHandler().getPlayerListEntry(var2.uuid) == null) {
            var1.remove();
         } else if (!var2.watched && var2.sinceTick < this.tickCounter) {
            AntiVanishModule.RegionProof var3 = this.regionProof(var2.uuid);
            if (var3 == null && !var2.told) {
               var3 = var2.proof;
            }

            if (var3 != null) {
               var2.told = true;
               var2.watched = true;
               this.watchingYou(var2.name, var3);
            } else if (!var2.told) {
               var2.told = true;
               this.news(var2.name, var2.how(), var2.detail);
            }
         }
      }
   }

   private boolean endSpell(String string, boolean bl, long l) {
      AntiVanishModule.HiddenSpell var5 = this.hiddenSpells.get(spellKey(string));
      if (var5 != null && var5.spectator == bl && System.currentTimeMillis() - var5.sinceMs >= l) {
         this.hiddenSpells.remove(spellKey(string));
         this.detections.remove(detectionKey(var5.uuid, var5.name));
         if (var5.told) {
            this.news(var5.name, ModuleChat.body().text("is ").good("visible again").build(), "");
         }

         return true;
      } else {
         return false;
      }
   }

   private AntiVanishModule.RegionProof regionProof(UUID uUID) {
      if (uUID == null) {
         return null;
      } else {
         PlayerEntity var2 = MC.world.getPlayerByUuid(uUID);
         if (var2 != null && var2 != MC.player) {
            return new AntiVanishModule.RegionProof(uUID, Math.round(var2.distanceTo(MC.player)));
         } else {
            return StaffListModule.isInYourRegion(uUID) ? new AntiVanishModule.RegionProof(uUID, -1) : null;
         }
      }
   }

   private static String spellKey(String string) {
      return string.toLowerCase(Locale.ROOT);
   }

   private static String detectionKey(UUID uUID, String string) {
      return uUID != null ? uUID.toString() : string;
   }

   private void scanInvisiblePlayers() {
      double var1 = this.sensorRangeSq();

      for (PlayerEntity var4 : MC.world.getPlayers()) {
         if (var4 != null && var4 != MC.player && var4.isInvisible() && !(var4.squaredDistanceTo(MC.player) > var1) && this.realPlayer(var4)) {
            this.triggerSensor(AntiVanishModule.SignalType.INVISIBLE, var4, "Invisible Entity: metadata flag", 35, 5000L);
         }
      }
   }

   private void inspectInvisibleEntity(int i) {
      Entity var3 = MC.world.getEntityById(i);
      PlayerEntity var2;
      if (var3 instanceof PlayerEntity && (var2 = (PlayerEntity)var3) != MC.player && var2.isInvisible()) {
         if (!(var2.squaredDistanceTo(MC.player) > this.sensorRangeSq())) {
            if (this.realPlayer(var2)) {
               this.triggerSensor(AntiVanishModule.SignalType.INVISIBLE, var2, "Invisible Entity: metadata flag", 35, 5000L);
            }
         }
      }
   }

   private boolean realPlayer(PlayerEntity playerEntity) {
      return this.credibleSubject(playerEntity.getUuid(), profileName(playerEntity));
   }

   private static String profileName(PlayerEntity playerEntity) {
      return playerEntity.getGameProfile() == null ? playerEntity.getName().getString() : playerEntity.getGameProfile().name();
   }

   private void inspectPositionalSound(AntiVanishModule.Observation observation) {
      if (AntiVanishHeuristics.suspiciousSound(observation.detail)) {
         Vec3d var2 = observation.position();
         long var3 = System.currentTimeMillis();
         if (this.nearPlayer(var2)
            && !this.hasVisibleCause(var2)
            && !this.isExplosionRelated(var2)
            && !this.isPoweredMechanism(var2, observation.detail)
            && var3 - this.lastLocalActionMs >= 1000L
            && !this.recentlySelfPlaced(var2)
            && !this.villagerToggledDoor(observation.detail, var2)
            && (!this.selfContainerActive() || !isContainerSignal(observation.detail))) {
            this.triggerSensor(AntiVanishModule.SignalType.SOUND, this.locatedSubject(var2), "Suspicious Sound: " + shortId(observation.detail), 14, 3000L);
         }
      }
   }

   private void inspectEntitySound(AntiVanishModule.Observation observation) {
      if (AntiVanishHeuristics.suspiciousSound(observation.detail)) {
         Entity var3 = MC.world.getEntityById(observation.entityId);
         PlayerEntity var2;
         if (var3 instanceof PlayerEntity
            && (var2 = (PlayerEntity)var3) != MC.player
            && var2.isInvisible()
            && var2.squaredDistanceTo(MC.player) <= this.sensorRangeSq()
            && this.realPlayer(var2)) {
            this.triggerSensor(AntiVanishModule.SignalType.SOUND, var2, "Suspicious Sound: invisible source", 16, 3000L);
         }
      }
   }

   private void inspectParticle(AntiVanishModule.Observation observation) {
      if (AntiVanishHeuristics.suspiciousParticle(observation.detail)) {
         Vec3d var2 = observation.position();
         long var3 = System.currentTimeMillis();
         if (this.nearPlayer(var2)
            && !this.hasVisibleCause(var2)
            && !this.isExplosionRelated(var2)
            && var3 - this.lastLocalActionMs >= 900L
            && !this.hasAmbientParticleSource(var2, observation.detail)) {
            String var5 = shortId(observation.detail);
            if (!var5.equals("block") && !var5.contains("smoke") || !this.nearSelf(var2, 6.25)) {
               if (!var5.equals("block") && !var5.contains("smoke") || this.particleBurstReady(var5, var3)) {
                  this.triggerSensor(
                     AntiVanishModule.SignalType.PARTICLE, this.locatedSubject(var2), "Ghost Particle: " + shortId(observation.detail), 16, 3000L
                  );
               }
            }
         }
      }
   }

   private boolean villagerToggledDoor(String string, Vec3d vec3d) {
      String var4 = AntiVanishHeuristics.path(string);
      boolean var3 = var4.contains("door") && !var4.contains("trapdoor") || var4.contains("fence_gate");
      if (var3 && vec3d != null && MC.world != null) {
         for (Entity var7 : MC.world.getEntities()) {
            if (var7.getType() == EntityType.VILLAGER && var7.getEntityPos().squaredDistanceTo(vec3d) <= 9.0) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean recentlySelfPlaced(Vec3d vec3d) {
      if (vec3d == null) {
         return false;
      } else {
         Long var2 = this.selfPlacedBlocks.get(BlockPos.ofFloored(vec3d).asLong());
         return var2 != null && var2 > System.currentTimeMillis();
      }
   }

   private void rememberExplosion(AntiVanishModule.Observation observation) {
      long var2 = System.currentTimeMillis();
      this.recentExplosions
         .addLast(new AntiVanishModule.ExplosionEvent(observation.position(), Math.max(2.0, parseDouble(observation.detail, 4.0) + 4.0), var2));

      while (this.recentExplosions.size() > 8) {
         this.recentExplosions.removeFirst();
      }
   }

   private boolean particleBurstReady(String string, long l) {
      Deque var4 = this.weakParticleBursts.computeIfAbsent(string, stringx -> new ArrayDeque<>());
      var4.addLast(l);

      while (!var4.isEmpty() && l - var4.peekFirst() > 2000L) {
         var4.removeFirst();
      }

      if (var4.size() < 2) {
         return false;
      } else {
         var4.clear();
         return true;
      }
   }

   private void triggerSensor(AntiVanishModule.SignalType signalType, String string, String string2, int i, long l) {
      this.addSignal(signalType, string, null, string2, i, l);
   }

   private void triggerSensor(AntiVanishModule.SignalType signalType, PlayerEntity playerEntity, String string, int i, long l) {
      this.addSignal(signalType, profileName(playerEntity), this.regionProof(playerEntity.getUuid()), string, i, l);
   }

   private void addSignal(AntiVanishModule.SignalType signalType, String string, AntiVanishModule.RegionProof regionProof, String string2, int i, long l) {
      long var11 = System.currentTimeMillis();
      String var8;
      if (var11 - this.signalCooldowns.getOrDefault(var8 = signalType.name() + "|" + string.toLowerCase(Locale.ROOT), 0L) >= l) {
         this.signalCooldowns.put(var8, var11);
         this.signals.addLast(new AntiVanishModule.Signal(signalType, string, regionProof, string2, i, var11));
         this.upsertDetection("signal:" + var8, string, string2, i, var11 + 20000L);
         this.announceTrigger(var8, string, regionProof != null, string2);
         this.evaluateCritical();
      }
   }

   private void evaluateCritical() {
      long var1 = System.currentTimeMillis();
      this.pruneSignals(var1);
      EnumMap var3 = new EnumMap<>(AntiVanishModule.SignalType.class);
      AntiVanishModule.Signal var4 = null;

      for (AntiVanishModule.Signal var6 : this.signals) {
         var3.merge(var6.type, var6.weight, Math::max);
         if (var6.proof != null) {
            var4 = var6;
         }
      }

      int var7 = var3.values().stream().mapToInt(Integer::intValue).sum();
      if (var4 != null && var3.size() >= 2 && var7 >= 35) {
         if (var1 - this.lastCriticalMs >= 12000L) {
            this.watchingYou(var4.subject, var4.proof);
         }
      }
   }

   private void watchingYou(String string, AntiVanishModule.RegionProof regionProof) {
      long var3;
      this.lastCriticalMs = var3 = System.currentTimeMillis();
      this.criticalUntilMs = var3 + 7000L;
      this.criticalWatcher = string;
      this.pendingChimes.add(regionProof.id());
      this.ghostId = regionProof.id();
      this.ghostName = string;
   }

   private void playPendingChime() {
      if (!this.pendingChimes.isEmpty()) {
         boolean var1 = this.pendingChimes.stream().allMatch(StaffListModule::alarmedRecently);
         this.pendingChimes.clear();
         if (!var1) {
            if (MC.getSoundManager() != null) {
               MC.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 1.0F, 1.0F));
            }

            StaffGhost.watching(this.ghostId, this.ghostName, 0);
         }
      }
   }

   private void news(String string, Text text, String string2) {
      ModuleChat.Body var4 = ModuleChat.body().name(NameFilter.apply(string)).text(" ").append(text);
      if (!string2.isEmpty()) {
         var4.text(" (" + string2 + ")");
      }

      ModuleChat.send(this, var4);
   }

   private void queueGamemodeUpdates(PlayerListS2CPacket playerListS2CPacket) {
      if (!playerListS2CPacket.getActions().contains(Action.ADD_PLAYER)) {
         for (Entry var3 : playerListS2CPacket.getEntries()) {
            if (var3 != null && var3.gameMode() != null && var3.profileId() != null) {
               this.observations.offer(AntiVanishModule.Observation.gamemode(var3.profileId(), var3.gameMode().getId()));
            }
         }
      }
   }

   private void announceGamemode(UUID uUID, String string) {
      if (uUID != null && (MC.player == null || !MC.player.getUuid().equals(uUID))) {
         AntiVanishModule.KnownPlayer var3 = this.knownPlayers.get(uUID);
         if (var3 != null && StaffListModule.isStaffName(var3.name)) {
            this.arrivals.put(spellKey(var3.name), System.currentTimeMillis());
            if (GameMode.SPECTATOR.getId().equals(string)) {
               if (this.credibleSubject(uUID, var3.name)) {
                  this.reportHidden(uUID, var3.name, true, "Vanish Event: switched to spectator", "", 100);
                  return;
               }
            } else if (this.endSpell(var3.name, true, 0L)) {
               return;
            }

            ModuleChat.send(this, StaffListModule.switchedTo(ModuleChat.body().name(NameFilter.apply(var3.name)).text(" "), string));
         }
      }
   }

   private void announceTrigger(String string, String string2, boolean bl, String string3) {
      long var6 = System.currentTimeMillis();
      Long var8 = this.announceCooldowns.get(string);
      if (var8 == null || var6 - var8 >= 1500L) {
         while (!this.announceTimes.isEmpty() && var6 - this.announceTimes.peekFirst() > 4000L) {
            this.announceTimes.removeFirst();
         }

         if (this.announceTimes.size() < 6) {
            this.announceCooldowns.put(string, var6);
            this.announceTimes.addLast(var6);
            int var9 = string3.indexOf(": ");
            ModuleChat.Body var10 = var9 < 0
               ? ModuleChat.body().warn(string3)
               : ModuleChat.body().warn(string3.substring(0, var9)).text(string3.substring(var9));
            boolean var5 = string2 != null && !string2.isBlank() && !"Unknown".equalsIgnoreCase(string2) && !"CRITICAL".equalsIgnoreCase(string2);
            if (var5) {
               var10.text(" (");
               if (bl) {
                  var10.name(NameFilter.apply(string2));
               } else {
                  var10.value(string2);
               }

               var10.text(")");
            }

            ModuleChat.send(this, var10);
         }
      }
   }

   private void upsertDetection(String string, String string2, String string3, int i, long l) {
      AntiVanishModule.Detection var7 = this.detections.get(string);
      if (var7 == null) {
         this.detections.put(string, new AntiVanishModule.Detection(string2, string3, i, l));
      } else {
         var7.name = string2;
         var7.expiresAt = Math.max(var7.expiresAt, l);
         if (i >= var7.score) {
            var7.reason = string3;
            var7.score = i;
         }
      }
   }

   private List<AntiVanishModule.HudEntry> hudSnapshot() {
      long var1 = System.currentTimeMillis();
      ArrayList var3 = new ArrayList();
      if (var1 < this.criticalUntilMs) {
         var3.add(new AntiVanishModule.HudEntry("CRITICAL", this.criticalWatcher));
      }

      HashSet var4 = new HashSet();
      this.detections
         .values()
         .stream()
         .filter(detection -> detection.expiresAt > var1)
         .filter(AntiVanishModule::detectionWorthShowing)
         .sorted(
            Comparator.<AntiVanishModule.Detection>comparingInt(detection -> detection.score)
               .reversed()
               .thenComparing(detection -> detection.name, String.CASE_INSENSITIVE_ORDER)
         )
         .map(detection -> new AntiVanishModule.HudEntry(detection.name, detection.reason))
         .filter(hudEntry -> var4.add(hudTag(hudEntry) + "\u0000" + hudValue(hudEntry)))
         .limit(4L)
         .forEach(var3::add);
      return List.copyOf(var3);
   }

   private void pruneState() {
      long var1 = System.currentTimeMillis();
      this.detections.entrySet().removeIf(entry -> entry.getValue().expiresAt <= var1);
      this.signalCooldowns.entrySet().removeIf(entry -> var1 - entry.getValue() > 60000L);
      this.announceCooldowns.entrySet().removeIf(entry -> var1 - entry.getValue() > 60000L);
      this.selfPlacedBlocks.entrySet().removeIf(entry -> entry.getValue() <= var1);
      this.confirmedDepartures.entrySet().removeIf(entry -> entry.getValue() <= var1);
      this.automatedMechanisms.entrySet().removeIf(entry -> entry.getValue() <= var1);
      this.weakParticleBursts.values().removeIf(deque -> {
         while (!deque.isEmpty() && var1 - deque.peekFirst() > 2000L) {
            deque.removeFirst();
         }

         return deque.isEmpty();
      });

      while (!this.recentExplosions.isEmpty() && var1 - this.recentExplosions.peekFirst().timeMs > 3000L) {
         this.recentExplosions.removeFirst();
      }

      this.pruneSignals(var1);
   }

   private void pruneSignals(long l) {
      while (!this.signals.isEmpty() && l - this.signals.peekFirst().timeMs > 15000L) {
         this.signals.removeFirst();
      }
   }

   private boolean nearPlayer(Vec3d vec3d) {
      return vec3d != null && vec3d.squaredDistanceTo(MC.player.getEntityPos()) <= this.sensorRangeSq();
   }

   private boolean nearSelf(Vec3d vec3d, double d) {
      return vec3d != null && MC.player != null && vec3d.squaredDistanceTo(MC.player.getEntityPos()) < d;
   }

   private boolean selfContainerActive() {
      return System.currentTimeMillis() - this.lastContainerActivityMs < 2500L;
   }

   private static boolean isContainerSignal(String string) {
      String var1 = AntiVanishHeuristics.path(string);
      return var1.contains("chest") || var1.contains("barrel") || var1.contains("shulker");
   }

   private String locatedSubject(Vec3d vec3d) {
      if (vec3d != null && MC.player != null) {
         Vec3d var2 = MC.player.getEntityPos();
         double var3 = vec3d.x - var2.x;
         double var5 = vec3d.y - var2.y;
         double var7 = vec3d.z - var2.z;
         long var9 = Math.round(Math.sqrt(var3 * var3 + var5 * var5 + var7 * var7));
         String var11 = compass(var3, var7);
         return var11.isEmpty() ? var9 + "m" : var9 + "m " + var11;
      } else {
         return "nearby";
      }
   }

   private static String compass(double d, double e) {
      String var4 = e < -1.0 ? "N" : (e > 1.0 ? "S" : "");
      String var5 = d > 1.0 ? "E" : (d < -1.0 ? "W" : "");
      return var4 + var5;
   }

   private boolean hasVisibleCause(Vec3d vec3d) {
      if (vec3d == null) {
         return true;
      } else {
         for (Entity var3 : MC.world.getEntities()) {
            PlayerEntity var4;
            if (var3 != MC.player
               && (
                  var3 instanceof PlayerEntity
                     ? !(var4 = (PlayerEntity)var3).isInvisible() && var3.getEntityPos().squaredDistanceTo(vec3d) <= 16.0
                     : var3 instanceof ProjectileEntity && var3.getEntityPos().squaredDistanceTo(vec3d) <= 16.0
               )) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean isExplosionRelated(Vec3d vec3d) {
      if (vec3d == null) {
         return false;
      } else {
         long var2 = System.currentTimeMillis();

         while (!this.recentExplosions.isEmpty() && var2 - this.recentExplosions.peekFirst().timeMs > 3000L) {
            this.recentExplosions.removeFirst();
         }

         for (AntiVanishModule.ExplosionEvent var5 : this.recentExplosions) {
            if (var5.center.squaredDistanceTo(vec3d) <= var5.radius * var5.radius) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean hasAmbientParticleSource(Vec3d vec3d, String string) {
      if (vec3d != null && shortId(string).contains("smoke")) {
         BlockPos var3 = BlockPos.ofFloored(vec3d);

         for (int var4 = -2; var4 <= 2; var4++) {
            for (int var5 = -2; var5 <= 2; var5++) {
               for (int var6 = -2; var6 <= 2; var6++) {
                  BlockPos var7 = var3.add(var4, var5, var6);
                  Identifier var8 = Registries.BLOCK.getId(MC.world.getBlockState(var7).getBlock());
                  String var9 = shortId(var8 == null ? "" : var8.toString());
                  if (var9.contains("campfire")
                     || var9.contains("furnace")
                     || var9.contains("smoker")
                     || var9.contains("torch")
                     || var9.contains("fire")
                     || var9.contains("candle")
                     || var9.contains("respawn_anchor")) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isPoweredMechanism(Vec3d vec3d, String string) {
      String var3 = shortId(string);
      if (!var3.contains("door") && !var3.contains("trapdoor")) {
         return false;
      } else {
         BlockPos var4 = BlockPos.ofFloored(vec3d);
         long var5 = System.currentTimeMillis();

         for (int var7 = -1; var7 <= 1; var7++) {
            BlockPos var9 = var4.add(0, var7, 0);
            BlockState var10 = MC.world.getBlockState(var9);
            long var11 = var9.asLong();
            boolean var8 = var10.contains(Properties.POWERED) && var10.get(Properties.POWERED);
            if (var8 || MC.world.isReceivingRedstonePower(var9)) {
               this.automatedMechanisms.put(var11, var5 + 5000L);
               return true;
            }

            if (this.automatedMechanisms.getOrDefault(var11, 0L) > var5) {
               return true;
            }
         }

         return false;
      }
   }

   private double sensorRangeSq() {
      double var1 = 64.0;
      return var1 * var1;
   }

   private static double parseDouble(String string, double d) {
      try {
         return Double.parseDouble(string);
      } catch (Exception var4) {
         return d;
      }
   }

   private static String soundId(Identifier identifier) {
      return identifier == null ? "" : identifier.toString();
   }

   private static String shortId(String string) {
      return AntiVanishHeuristics.path(string);
   }

   private void resetRuntime() {
      this.observations.clear();
      this.knownPlayers.clear();
      this.detections.clear();
      this.signalCooldowns.clear();
      this.announceCooldowns.clear();
      this.announceTimes.clear();
      this.selfPlacedBlocks.clear();
      this.confirmedDepartures.clear();
      this.automatedMechanisms.clear();
      this.weakParticleBursts.clear();
      this.signals.clear();
      this.recentExplosions.clear();
      this.pendingVanishes.clear();
      this.hiddenSpells.clear();
      this.shard.clear();
      this.tickCounter = 0;
      this.lastLocalActionMs = 0L;
      this.lastContainerActivityMs = 0L;
      this.lastCriticalMs = 0L;
      this.criticalUntilMs = 0L;
      this.criticalWatcher = "";
      this.pendingChimes.clear();
      this.ghostId = null;
      this.ghostName = null;
      this.recentMessages.clear();
      this.listedSinceMs.clear();
      this.serverSendsLeaveMessages = false;
      this.hints.clear();
      this.heldHints.clear();
      this.tabMoves.clear();
      this.teamChanges.clear();
      this.tabNames.clear();
      this.arrivals.clear();
      this.quietUntilMs = System.currentTimeMillis() + 4000L;
   }

   @Environment(EnvType.CLIENT)
   private static final class Detection {
      String name;
      String reason;
      int score;
      long expiresAt;

      Detection(String string, String string2, int i, long l) {
         this.name = string;
         this.reason = string2;
         this.score = i;
         this.expiresAt = l;
      }
   }

   @Environment(EnvType.CLIENT)
   private record ExplosionEvent(Vec3d center, double radius, long timeMs) {
   }

   @Environment(EnvType.CLIENT)
   private static final class HiddenSpell {
      final UUID uuid;
      final String name;
      final boolean spectator;
      final String detail;
      final AntiVanishModule.RegionProof proof;
      final int sinceTick;
      final long sinceMs = System.currentTimeMillis();
      boolean told;
      boolean watched;

      HiddenSpell(UUID uUID, String string, boolean bl, String string2, AntiVanishModule.RegionProof regionProof, int i) {
         this.uuid = uUID;
         this.name = string;
         this.spectator = bl;
         this.detail = string2;
         this.proof = regionProof;
         this.sinceTick = i;
      }

      Text how() {
         return this.spectator
            ? StaffListModule.switchedTo(ModuleChat.body(), GameMode.SPECTATOR.getId()).build()
            : ModuleChat.body().danger("vanished").build();
      }
   }

   @Environment(EnvType.CLIENT)
   private record Hint(AntiVanishModule.HintKind kind, UUID id, String name, int rank, long atMs) {
   }

   @Environment(EnvType.CLIENT)
   private static enum HintKind {
      ORDER,
      TEAM_UP,
      TEAM_DOWN,
      TAB_IN,
      TAB_OUT;

      private HintKind() {
      }
   }

   @Environment(EnvType.CLIENT)
   public record HudEntry(String name, String reason) {
   }

   @Environment(EnvType.CLIENT)
   private record KnownPlayer(UUID uuid, String name) {
   }

   @Environment(EnvType.CLIENT)
   private record Observation(AntiVanishModule.ObservationType type, UUID profileId, int entityId, double x, double y, double z, String detail) {
      static AntiVanishModule.Observation tabRemove(UUID uUID) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.TAB_REMOVE, uUID, -1, 0.0, 0.0, 0.0, "");
      }

      static AntiVanishModule.Observation tabHide(UUID uUID) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.TAB_HIDE, uUID, -1, 0.0, 0.0, 0.0, "");
      }

      static AntiVanishModule.Observation gamemode(UUID uUID, String string) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.GAMEMODE, uUID, -1, 0.0, 0.0, 0.0, string);
      }

      static AntiVanishModule.Observation playerLeft(String string) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.PLAYER_LEFT, null, -1, 0.0, 0.0, 0.0, string);
      }

      static AntiVanishModule.Observation systemChat(String string) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.SYSTEM_CHAT, null, -1, 0.0, 0.0, 0.0, string);
      }

      static AntiVanishModule.Observation entityMetadata(int i) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.ENTITY_METADATA, null, i, 0.0, 0.0, 0.0, "");
      }

      static AntiVanishModule.Observation explosion(Vec3d vec3d, float f) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.EXPLOSION, null, -1, vec3d.x, vec3d.y, vec3d.z, Float.toString(f));
      }

      static AntiVanishModule.Observation entitySound(int i, String string) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.ENTITY_SOUND, null, i, 0.0, 0.0, 0.0, string);
      }

      static AntiVanishModule.Observation position(AntiVanishModule.ObservationType observationType, double d, double e, double f, String string) {
         return new AntiVanishModule.Observation(observationType, null, -1, d, e, f, string);
      }

      Vec3d position() {
         return new Vec3d(this.x, this.y, this.z);
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum ObservationType {
      TAB_REMOVE,
      TAB_HIDE,
      PLAYER_LEFT,
      SYSTEM_CHAT,
      ENTITY_METADATA,
      POSITIONAL_SOUND,
      ENTITY_SOUND,
      PARTICLE,
      EXPLOSION,
      GAMEMODE;

      private ObservationType() {
      }
   }

   @Environment(EnvType.CLIENT)
   private record PendingVanish(UUID uuid, String name, int dueTick) {
   }

   @Environment(EnvType.CLIENT)
   private record RecentMessage(String text, long atMs) {
   }

   @Environment(EnvType.CLIENT)
   private record RegionProof(UUID id, int distance) {
   }

   @Environment(EnvType.CLIENT)
   private record Signal(AntiVanishModule.SignalType type, String subject, AntiVanishModule.RegionProof proof, String reason, int weight, long timeMs) {
   }

   @Environment(EnvType.CLIENT)
   private static enum SignalType {
      VANISH,
      INVISIBLE,
      PARTICLE,
      SOUND;

      private SignalType() {
      }
   }
}
