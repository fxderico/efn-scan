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
import net.minecraft.sound.SoundEvent;
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
   private static final MinecraftClient MC = MinecraftClient.method_1551();
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
   private static final Set<Action> ORDER_ONLY = EnumSet.of(Action.field_29139, Action.field_52324);
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
      if (MC.field_1724 != null && MC.field_1687 != null) {
         this.trackListedPlayers();
      }
   }

   @Override
   protected void onDisable() {
      this.resetRuntime();
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      if (MC.field_1724 != null && MC.field_1687 != null && MC.method_1562() != null) {
         if (this.shard.moved(MC)) {
            this.resetRuntime();
         }

         this.playPendingChime();
         this.tickCounter++;
         if (MC.field_1724.field_7512 != MC.field_1724.field_7498) {
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
   public void onPacketSend(PacketSendEvent var1) {
      this.handleOutbound(var1.packet);
   }

   private void handleOutbound(Packet<?> var1) {
      if (var1 != null) {
         if (var1 instanceof PlayerInteractBlockC2SPacket var2) {
            this.lastLocalActionMs = System.currentTimeMillis();
            if (var2.method_12543() != null && var2.method_12543().method_17777() != null) {
               long var3 = System.currentTimeMillis() + 6000L;
               BlockPos var5 = var2.method_12543().method_17777();
               BlockPos var6 = var5.method_10093(var2.method_12543().method_17780());
               this.selfPlacedBlocks.put(var5.method_10063(), var3);
               this.selfPlacedBlocks.put(var6.method_10063(), var3);
               String var7 = "";
               if (MC.field_1724 != null && var2.method_12546() != null) {
                  Identifier var8 = Registries.field_41178.method_10221(MC.field_1724.method_5998(var2.method_12546()).method_7909());
                  var7 = AntiVanishHeuristics.path(var8 == null ? "" : var8.toString());
               }

               markSelfMultiBlockFootprint(var5, var7, this.selfPlacedBlocks, var3);
               markSelfMultiBlockFootprint(var6, var7, this.selfPlacedBlocks, var3);
            }
         } else {
            if (var1 instanceof PlayerActionC2SPacket
               || var1 instanceof PlayerInteractItemC2SPacket
               || var1 instanceof PlayerInteractEntityC2SPacket
               || var1 instanceof HandSwingC2SPacket) {
               this.lastLocalActionMs = System.currentTimeMillis();
            }
         }
      }
   }

   static void markSelfMultiBlockFootprint(BlockPos var0, String var1, Map<Long, Long> var2, long var3) {
      if (var0 != null && var2 != null) {
         var2.put(var0.method_10063(), var3);
         String var6 = var1 == null ? "" : var1;
         boolean var5 = var6.endsWith("_door") && !var6.endsWith("trapdoor")
            || var6.contains("sunflower")
            || var6.contains("lilac")
            || var6.contains("rose_bush")
            || var6.contains("peony")
            || var6.contains("tall_grass")
            || var6.contains("large_fern")
            || var6.contains("pitcher_plant");
         if (var5) {
            var2.put(var0.method_10084().method_10063(), var3);
            var2.put(var0.method_10074().method_10063(), var3);
         }

         if (var6.endsWith("_bed")) {
            var2.put(var0.method_10069(1, 0, 0).method_10063(), var3);
            var2.put(var0.method_10069(-1, 0, 0).method_10063(), var3);
            var2.put(var0.method_10069(0, 0, 1).method_10063(), var3);
            var2.put(var0.method_10069(0, 0, -1).method_10063(), var3);
         }
      }
   }

   @EventHandler
   public void onPacketReceiveNetty(PacketReceiveNettyEvent var1) {
      if (this.handleInbound(var1.packet)) {
         var1.setCancelled(true);
      }
   }

   private boolean handleInbound(Packet<?> var1) {
      if (var1 instanceof BundleS2CPacket var15) {
         for (Packet var24 : var15.method_48324()) {
            this.handleInbound(var24);
         }

         return false;
      } else {
         if (var1 instanceof PlayerRemoveS2CPacket var2) {
            if (var2.comp_1105().size() < 4) {
               for (UUID var4 : var2.comp_1105()) {
                  this.observations.offer(AntiVanishModule.Observation.tabRemove(var4));
               }
            }

            long var16 = System.currentTimeMillis();

            for (UUID var6 : var2.comp_1105()) {
               this.hints.offer(new AntiVanishModule.Hint(AntiVanishModule.HintKind.TAB_OUT, var6, null, -1, var16));
            }
         } else if (var1 instanceof PlayerListS2CPacket var7) {
            this.queueTabHints(var7);
            if (var7.method_46327().contains(Action.field_40700)) {
               int var17 = 0;

               for (Entry var25 : var7.method_46329()) {
                  if (var25 != null && !var25.comp_1108()) {
                     var17++;
                  }
               }

               if (var17 > 0 && var17 < 4) {
                  for (Entry var26 : var7.method_46329()) {
                     if (var26 != null && !var26.comp_1108()) {
                        this.observations.offer(AntiVanishModule.Observation.tabHide(var26.comp_1106()));
                     }
                  }
               }
            }

            if (var7.method_46327().contains(Action.field_29137)) {
               this.queueGamemodeUpdates(var7);
            }
         } else if (var1 instanceof GameMessageS2CPacket var23) {
            String var18 = var23.comp_763() == null ? "" : var23.comp_763().getString();
            if (!var18.isBlank()) {
               this.observations.offer(AntiVanishModule.Observation.systemChat(var18));
            }

            String var8;
            if (!(var8 = departedPlayerName(var23.comp_763())).isBlank()) {
               this.observations.offer(AntiVanishModule.Observation.playerLeft(var8));
            }
         } else if (var1 instanceof EntityTrackerUpdateS2CPacket var9) {
            this.observations.offer(AntiVanishModule.Observation.entityMetadata(var9.comp_1127()));
         } else if (var1 instanceof ExplosionS2CPacket var10) {
            this.observations.offer(AntiVanishModule.Observation.explosion(var10.comp_2883(), var10.comp_4594()));
         } else if (var1 instanceof PlaySoundFromEntityS2CPacket var11) {
            this.observations
               .offer(AntiVanishModule.Observation.entitySound(var11.method_11883(), soundId(((SoundEvent)var11.method_11882().comp_349()).comp_3319())));
         } else if (var1 instanceof ParticleS2CPacket var12) {
            Identifier var19 = Registries.field_41180.method_10221(var12.method_11551().method_10295());
            this.observations
               .offer(
                  AntiVanishModule.Observation.position(
                     AntiVanishModule.ObservationType.PARTICLE,
                     var12.method_11544(),
                     var12.method_11547(),
                     var12.method_11546(),
                     var19 == null ? "" : var19.toString()
                  )
               );
         } else if (var1 instanceof PlaySoundS2CPacket var13) {
            this.onSoundPacket(var13);
         } else if (var1 instanceof TeamS2CPacket var14) {
            this.queueTeamHint(var14);
         }

         return false;
      }
   }

   private void queueTabHints(PlayerListS2CPacket var1) {
      long var2 = System.currentTimeMillis();
      if (!var1.method_46327().contains(Action.field_29136)) {
         if (ORDER_ONLY.equals(var1.method_46327()) && var1.method_46329().size() < 4) {
            for (Entry var5 : var1.method_46329()) {
               if (var5 != null && var5.comp_1106() != null) {
                  this.hints.offer(new AntiVanishModule.Hint(AntiVanishModule.HintKind.ORDER, var5.comp_1106(), null, -1, var2));
               }
            }
         }
      } else {
         for (Entry var7 : var1.method_46329()) {
            if (var7 != null && var7.comp_1107() != null && var7.comp_1107().name() != null) {
               this.hints.offer(new AntiVanishModule.Hint(AntiVanishModule.HintKind.TAB_IN, var7.comp_1106(), var7.comp_1107().name(), -1, var2));
            }
         }
      }
   }

   private void queueTeamHint(TeamS2CPacket var1) {
      Operation var4 = var1.method_34176();
      Matcher var3 = var4 == null ? null : RANK_TEAM.matcher(var1.method_34177());
      if (var3 != null && var3.matches()) {
         String var6 = var3.group(2);
         boolean var2 = var4 == Operation.field_29155;
         if (!var2 || var1.method_34178().isEmpty() || !var1.method_34178().stream().noneMatch(var6::equalsIgnoreCase)) {
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

      for (PlayerListEntry var6 : MC.method_1562().method_2880()) {
         if (var6.method_2966() != null && var6.method_2966().name() != null) {
            this.tabNames.put(var6.method_2966().id(), var6.method_2966().name());
         }
      }
   }

   private void judge(AntiVanishModule.Hint var1) {
      if (var1.atMs() >= this.quietUntilMs) {
         PlayerListEntry var3 = var1.id() != null ? MC.method_1562().method_2871(var1.id()) : MC.method_1562().method_73471(var1.name());
         String var2 = var1.name() != null ? var1.name() : (var3 != null && var3.method_2966() != null ? var3.method_2966().name() : null);
         if (var2 != null && !var2.equalsIgnoreCase(MC.field_1724.method_7334().name())) {
            int var5 = var1.rank() >= 0 ? var1.rank() : teamRank(var3);
            if (StaffListModule.isStaffName(var2) || var5 >= 0 && var5 <= 8) {
               UUID var6 = var3 != null && var3.method_2966() != null ? var3.method_2966().id() : var1.id();
               switch (var1.kind()) {
                  case ORDER:
                     this.arrived(var6, var2, "tab re-sort");
                     break;
                  case TEAM_UP:
                     if (this.teamOnly(var1)) {
                        this.arrived(var6, var2, "team created");
                     }
                     break;
                  case TEAM_DOWN:
                     if (this.teamOnly(var1)) {
                        this.departed(var2, "team removed");
                     }
               }
            }
         }
      }
   }

   private boolean teamOnly(AntiVanishModule.Hint var1) {
      int var2 = 0;

      for (long var4 : this.teamChanges) {
         if (Math.abs(var4 - var1.atMs()) <= 50L) {
            if (++var2 >= 5) {
               return false;
            }
         }
      }

      for (AntiVanishModule.Hint var5 : this.tabMoves) {
         if (var5.name().equalsIgnoreCase(var1.name()) && Math.abs(var5.atMs() - var1.atMs()) <= 1000L) {
            return false;
         }
      }

      return true;
   }

   private static int teamRank(PlayerListEntry var0) {
      Team var1 = var0 == null ? null : var0.method_2955();
      Matcher var2 = var1 == null ? null : RANK_TEAM.matcher(var1.method_1197());
      return var2 != null && var2.matches() ? Integer.parseInt(var2.group(1)) : -1;
   }

   private void arrived(UUID var1, String var2, String var3) {
      long var4 = System.currentTimeMillis();
      Long var6 = this.arrivals.put(spellKey(var2), var4);
      if (var1 != null) {
         StaffListModule.provenHere(var1, var2);
      }

      if (var6 == null || var4 - var6 >= 60000L) {
         this.news(var2, ModuleChat.body().text("is ").danger("on your goliath").build(), var3);
         this.watchingYou(var2, new AntiVanishModule.RegionProof(var1, -1));
      }
   }

   private void departed(String var1, String var2) {
      if (this.arrivals.remove(spellKey(var1)) != null) {
         this.news(var1, ModuleChat.body().good("left your goliath").build(), var2);
      }
   }

   private void onSoundPacket(PlaySoundS2CPacket var1) {
      if (var1 != null && var1.method_11894() != null && var1.method_11894().comp_349() != null) {
         String var2 = soundId(((SoundEvent)var1.method_11894().comp_349()).comp_3319());
         this.observations
            .offer(
               AntiVanishModule.Observation.position(
                  AntiVanishModule.ObservationType.POSITIONAL_SOUND, var1.method_11890(), var1.method_11889(), var1.method_11893(), var2
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

   private static boolean detectionWorthShowing(AntiVanishModule.Detection var0) {
      if (var0 == null) {
         return false;
      } else {
         String var1 = var0.name == null ? "" : var0.name.trim();
         return !var1.isBlank() && !"Unknown".equalsIgnoreCase(var1) && !"You".equalsIgnoreCase(var1) && !"CRITICAL".equalsIgnoreCase(var1);
      }
   }

   public static String hudTag(AntiVanishModule.HudEntry var0) {
      if (var0 == null) {
         return "WATCH";
      } else if ("CRITICAL".equalsIgnoreCase(var0.name())) {
         return "ALERT";
      } else {
         String var1 = var0.reason() == null ? "" : var0.reason().toLowerCase(Locale.ROOT);
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

   public static String hudValue(AntiVanishModule.HudEntry var0) {
      if (var0 == null) {
         return "Staff";
      } else if ("CRITICAL".equalsIgnoreCase(var0.name())) {
         return shortName(var0.reason());
      } else {
         String var1 = var0.name() == null ? "" : var0.name().trim();
         return !var1.isBlank() && !"Unknown".equalsIgnoreCase(var1) && !"You".equalsIgnoreCase(var1) ? shortName(var1) : "Staff";
      }
   }

   private static String shortName(String var0) {
      String var1 = var0 == null ? "" : NameFilter.apply(var0.trim());
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

   private void processObservation(AntiVanishModule.Observation var1) {
      switch (var1.type) {
         case TAB_REMOVE:
            this.handleTabRemoval(var1.profileId);
            break;
         case TAB_HIDE:
            this.handleTabHidden(var1.profileId);
            break;
         case PLAYER_LEFT:
            this.serverSendsLeaveMessages = true;
            this.confirmDeparture(var1.detail);
            break;
         case SYSTEM_CHAT:
            this.cacheRecentMessage(var1.detail);
            break;
         case ENTITY_METADATA:
            this.inspectInvisibleEntity(var1.entityId);
            break;
         case POSITIONAL_SOUND:
            this.inspectPositionalSound(var1);
            break;
         case ENTITY_SOUND:
            this.inspectEntitySound(var1);
            break;
         case PARTICLE:
            this.inspectParticle(var1);
            break;
         case EXPLOSION:
            this.rememberExplosion(var1);
            break;
         case GAMEMODE:
            this.announceGamemode(var1.profileId, var1.detail);
      }
   }

   private void trackListedPlayers() {
      if (MC.method_1562() != null) {
         long var1 = System.currentTimeMillis();

         for (PlayerListEntry var4 : MC.method_1562().method_45732()) {
            if (var4 != null && var4.method_2966() != null && var4.method_2966().id() != null) {
               UUID var5 = var4.method_2966().id();
               if (MC.field_1724 == null || !MC.field_1724.method_5667().equals(var5)) {
                  this.listedSinceMs.putIfAbsent(var5, var1);
                  String var6 = var4.method_2966().name();
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

            for (PlayerListEntry var9 : MC.method_1562().method_2880()) {
               if (var9 != null && var9.method_2966() != null) {
                  var7.add(var9.method_2966().id());
               }
            }

            this.listedSinceMs.keySet().retainAll(var7);
            this.knownPlayers.keySet().retainAll(var7);
         }
      }
   }

   private boolean trustedListed(UUID var1) {
      Long var2 = var1 == null ? null : this.listedSinceMs.get(var1);
      return var2 != null && System.currentTimeMillis() - var2 >= 1500L;
   }

   private boolean credibleSubject(UUID var1, String var2) {
      return var1 != null && AntiVanishText.isUsername(var2) && StaffListModule.isStaffName(var2) && this.trustedListed(var1);
   }

   private void handleTabRemoval(UUID var1) {
      if (var1 != null && !MC.field_1724.method_5667().equals(var1)) {
         String var2 = this.knownName(var1);
         if (this.credibleSubject(var1, var2) && AntiVanishText.isPlausiblePlayerName(var2)) {
            this.pendingVanishes.removeIf(var1x -> var1x.uuid.equals(var1));
            this.pendingVanishes.addLast(new AntiVanishModule.PendingVanish(var1, var2, this.tickCounter + 20));
         }
      }
   }

   private void handleTabHidden(UUID var1) {
      if (var1 != null && !MC.field_1724.method_5667().equals(var1)) {
         AntiVanishModule.KnownPlayer var3 = this.knownPlayers.get(var1);
         String var2 = var3 != null && var3.name != null ? var3.name : this.knownName(var1);
         if (this.credibleSubject(var1, var2) && AntiVanishText.isPlausiblePlayerName(var2)) {
            if (!this.recentMessageNames(var2)) {
               this.reportHidden(var1, var2, false, "Vanish Event: hidden from TAB", "hidden from TAB", 100);
            }
         }
      }
   }

   private String knownName(UUID var1) {
      AntiVanishModule.KnownPlayer var3 = this.knownPlayers.get(var1);
      if (var3 != null && var3.name != null && !var3.name.isBlank()) {
         return var3.name;
      } else {
         PlayerListEntry var2;
         return MC.method_1562() != null && (var2 = MC.method_1562().method_2871(var1)) != null && var2.method_2966() != null
            ? var2.method_2966().name()
            : null;
      }
   }

   private void confirmDeparture(String var1) {
      long var2 = System.currentTimeMillis();

      for (AntiVanishModule.KnownPlayer var5 : this.knownPlayers.values()) {
         if (AntiVanishText.containsPlayerName(var1, var5.name)) {
            this.confirmedDepartures.put(var5.uuid, var2 + 5000L);
            this.pendingVanishes.removeIf(var1x -> var1x.uuid.equals(var5.uuid));
            this.hiddenSpells.remove(spellKey(var5.name));
            AntiVanishModule.Detection var6 = this.detections.get(var5.uuid.toString());
            if (var6 != null && tabDepartureReason(var6.reason)) {
               this.detections.remove(var5.uuid.toString());
            }

            this.signals
               .removeIf(
                  var1x -> var1x.type == AntiVanishModule.SignalType.VANISH && var1x.subject.equalsIgnoreCase(var5.name) && tabDepartureReason(var1x.reason)
               );
         }
      }
   }

   private void processPendingVanishes() {
      while (!this.pendingVanishes.isEmpty() && this.pendingVanishes.peekFirst().dueTick <= this.tickCounter) {
         AntiVanishModule.PendingVanish var1 = this.pendingVanishes.removeFirst();
         if (MC.method_1562().method_2871(var1.uuid) == null) {
            long var2 = System.currentTimeMillis();
            if (this.confirmedDepartures.getOrDefault(var1.uuid, 0L) <= var2) {
               if (this.recentMessageNames(var1.name)) {
                  this.serverSendsLeaveMessages = true;
               } else {
                  PlayerEntity var4 = MC.field_1687.method_18470(var1.uuid);
                  if (var4 != null && !var4.method_31481()) {
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

   static int silentTabRemovalScore(boolean var0) {
      return var0 ? 100 : 30;
   }

   static boolean tabDepartureReason(String var0) {
      if (var0 == null) {
         return false;
      } else {
         String var1 = var0.toLowerCase(Locale.ROOT);
         return var1.startsWith("vanish event:") && (var1.contains("tab") || var1.contains("entity remained") || var1.contains("no leave packet"));
      }
   }

   private void cacheRecentMessage(String var1) {
      if (var1 != null && !var1.isBlank()) {
         long var2 = System.currentTimeMillis();
         this.recentMessages.addLast(new AntiVanishModule.RecentMessage(var1, var2));

         while (!this.recentMessages.isEmpty() && (var2 - this.recentMessages.peekFirst().atMs > 8000L || this.recentMessages.size() > 64)) {
            this.recentMessages.removeFirst();
         }
      }
   }

   private boolean recentMessageNames(String var1) {
      if (var1 != null && !var1.isBlank()) {
         long var2 = System.currentTimeMillis();

         for (AntiVanishModule.RecentMessage var5 : this.recentMessages) {
            if (var2 - var5.atMs <= 8000L && AntiVanishText.looksLikeLeaveMessage(var5.text, var1)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static String departedPlayerName(Text var0) {
      TranslatableTextContent var2;
      TextContent var3;
      if (var0 != null
         && (var3 = var0.method_10851()) instanceof TranslatableTextContent
         && "multiplayer.player.left".equals((var2 = (TranslatableTextContent)var3).method_11022())) {
         Object[] var4 = var2.method_11023();
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

   private void reportHidden(UUID var1, String var2, boolean var3, String var4, String var5, int var6) {
      this.upsertDetection(detectionKey(var1, var2), var2, var4, var6, System.currentTimeMillis() + 20000L);
      AntiVanishModule.HiddenSpell var7 = this.hiddenSpells.get(spellKey(var2));
      if (var7 == null || var7.spectator != var3) {
         this.hiddenSpells.put(spellKey(var2), new AntiVanishModule.HiddenSpell(var1, var2, var3, var5, this.regionProof(var1), this.tickCounter));
      }
   }

   private void reviewSpells() {
      Iterator var1 = this.hiddenSpells.values().iterator();

      while (var1.hasNext()) {
         AntiVanishModule.HiddenSpell var2 = (AntiVanishModule.HiddenSpell)var1.next();
         if (var2.spectator && MC.method_1562().method_2871(var2.uuid) == null) {
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

   private boolean endSpell(String var1, boolean var2, long var3) {
      AntiVanishModule.HiddenSpell var5 = this.hiddenSpells.get(spellKey(var1));
      if (var5 != null && var5.spectator == var2 && System.currentTimeMillis() - var5.sinceMs >= var3) {
         this.hiddenSpells.remove(spellKey(var1));
         this.detections.remove(detectionKey(var5.uuid, var5.name));
         if (var5.told) {
            this.news(var5.name, ModuleChat.body().text("is ").good("visible again").build(), "");
         }

         return true;
      } else {
         return false;
      }
   }

   private AntiVanishModule.RegionProof regionProof(UUID var1) {
      if (var1 == null) {
         return null;
      } else {
         PlayerEntity var2 = MC.field_1687.method_18470(var1);
         if (var2 != null && var2 != MC.field_1724) {
            return new AntiVanishModule.RegionProof(var1, Math.round(var2.method_5739(MC.field_1724)));
         } else {
            return StaffListModule.isInYourRegion(var1) ? new AntiVanishModule.RegionProof(var1, -1) : null;
         }
      }
   }

   private static String spellKey(String var0) {
      return var0.toLowerCase(Locale.ROOT);
   }

   private static String detectionKey(UUID var0, String var1) {
      return var0 != null ? var0.toString() : var1;
   }

   private void scanInvisiblePlayers() {
      double var1 = this.sensorRangeSq();

      for (PlayerEntity var4 : MC.field_1687.method_18456()) {
         if (var4 != null && var4 != MC.field_1724 && var4.method_5767() && !(var4.method_5858(MC.field_1724) > var1) && this.realPlayer(var4)) {
            this.triggerSensor(AntiVanishModule.SignalType.INVISIBLE, var4, "Invisible Entity: metadata flag", 35, 5000L);
         }
      }
   }

   private void inspectInvisibleEntity(int var1) {
      Entity var3 = MC.field_1687.method_8469(var1);
      PlayerEntity var2;
      if (var3 instanceof PlayerEntity && (var2 = (PlayerEntity)var3) != MC.field_1724 && var2.method_5767()) {
         if (!(var2.method_5858(MC.field_1724) > this.sensorRangeSq())) {
            if (this.realPlayer(var2)) {
               this.triggerSensor(AntiVanishModule.SignalType.INVISIBLE, var2, "Invisible Entity: metadata flag", 35, 5000L);
            }
         }
      }
   }

   private boolean realPlayer(PlayerEntity var1) {
      return this.credibleSubject(var1.method_5667(), profileName(var1));
   }

   private static String profileName(PlayerEntity var0) {
      return var0.method_7334() == null ? var0.method_5477().getString() : var0.method_7334().name();
   }

   private void inspectPositionalSound(AntiVanishModule.Observation var1) {
      if (AntiVanishHeuristics.suspiciousSound(var1.detail)) {
         Vec3d var2 = var1.position();
         long var3 = System.currentTimeMillis();
         if (this.nearPlayer(var2)
            && !this.hasVisibleCause(var2)
            && !this.isExplosionRelated(var2)
            && !this.isPoweredMechanism(var2, var1.detail)
            && var3 - this.lastLocalActionMs >= 1000L
            && !this.recentlySelfPlaced(var2)
            && !this.villagerToggledDoor(var1.detail, var2)
            && (!this.selfContainerActive() || !isContainerSignal(var1.detail))) {
            this.triggerSensor(AntiVanishModule.SignalType.SOUND, this.locatedSubject(var2), "Suspicious Sound: " + shortId(var1.detail), 14, 3000L);
         }
      }
   }

   private void inspectEntitySound(AntiVanishModule.Observation var1) {
      if (AntiVanishHeuristics.suspiciousSound(var1.detail)) {
         Entity var3 = MC.field_1687.method_8469(var1.entityId);
         PlayerEntity var2;
         if (var3 instanceof PlayerEntity
            && (var2 = (PlayerEntity)var3) != MC.field_1724
            && var2.method_5767()
            && var2.method_5858(MC.field_1724) <= this.sensorRangeSq()
            && this.realPlayer(var2)) {
            this.triggerSensor(AntiVanishModule.SignalType.SOUND, var2, "Suspicious Sound: invisible source", 16, 3000L);
         }
      }
   }

   private void inspectParticle(AntiVanishModule.Observation var1) {
      if (AntiVanishHeuristics.suspiciousParticle(var1.detail)) {
         Vec3d var2 = var1.position();
         long var3 = System.currentTimeMillis();
         if (this.nearPlayer(var2)
            && !this.hasVisibleCause(var2)
            && !this.isExplosionRelated(var2)
            && var3 - this.lastLocalActionMs >= 900L
            && !this.hasAmbientParticleSource(var2, var1.detail)) {
            String var5 = shortId(var1.detail);
            if (!var5.equals("block") && !var5.contains("smoke") || !this.nearSelf(var2, 6.25)) {
               if (!var5.equals("block") && !var5.contains("smoke") || this.particleBurstReady(var5, var3)) {
                  this.triggerSensor(AntiVanishModule.SignalType.PARTICLE, this.locatedSubject(var2), "Ghost Particle: " + shortId(var1.detail), 16, 3000L);
               }
            }
         }
      }
   }

   private boolean villagerToggledDoor(String var1, Vec3d var2) {
      String var4 = AntiVanishHeuristics.path(var1);
      boolean var3 = var4.contains("door") && !var4.contains("trapdoor") || var4.contains("fence_gate");
      if (var3 && var2 != null && MC.field_1687 != null) {
         for (Entity var7 : MC.field_1687.method_18112()) {
            if (var7.method_5864() == EntityType.field_6077 && var7.method_73189().method_1025(var2) <= 9.0) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean recentlySelfPlaced(Vec3d var1) {
      if (var1 == null) {
         return false;
      } else {
         Long var2 = this.selfPlacedBlocks.get(BlockPos.method_49638(var1).method_10063());
         return var2 != null && var2 > System.currentTimeMillis();
      }
   }

   private void rememberExplosion(AntiVanishModule.Observation var1) {
      long var2 = System.currentTimeMillis();
      this.recentExplosions.addLast(new AntiVanishModule.ExplosionEvent(var1.position(), Math.max(2.0, parseDouble(var1.detail, 4.0) + 4.0), var2));

      while (this.recentExplosions.size() > 8) {
         this.recentExplosions.removeFirst();
      }
   }

   private boolean particleBurstReady(String var1, long var2) {
      Deque var4 = this.weakParticleBursts.computeIfAbsent(var1, var0 -> new ArrayDeque<>());
      var4.addLast(var2);

      while (!var4.isEmpty() && var2 - var4.peekFirst() > 2000L) {
         var4.removeFirst();
      }

      if (var4.size() < 2) {
         return false;
      } else {
         var4.clear();
         return true;
      }
   }

   private void triggerSensor(AntiVanishModule.SignalType var1, String var2, String var3, int var4, long var5) {
      this.addSignal(var1, var2, null, var3, var4, var5);
   }

   private void triggerSensor(AntiVanishModule.SignalType var1, PlayerEntity var2, String var3, int var4, long var5) {
      this.addSignal(var1, profileName(var2), this.regionProof(var2.method_5667()), var3, var4, var5);
   }

   private void addSignal(AntiVanishModule.SignalType var1, String var2, AntiVanishModule.RegionProof var3, String var4, int var5, long var6) {
      long var11 = System.currentTimeMillis();
      String var8;
      if (var11 - this.signalCooldowns.getOrDefault(var8 = var1.name() + "|" + var2.toLowerCase(Locale.ROOT), 0L) >= var6) {
         this.signalCooldowns.put(var8, var11);
         this.signals.addLast(new AntiVanishModule.Signal(var1, var2, var3, var4, var5, var11));
         this.upsertDetection("signal:" + var8, var2, var4, var5, var11 + 20000L);
         this.announceTrigger(var8, var2, var3 != null, var4);
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

   private void watchingYou(String var1, AntiVanishModule.RegionProof var2) {
      long var3;
      this.lastCriticalMs = var3 = System.currentTimeMillis();
      this.criticalUntilMs = var3 + 7000L;
      this.criticalWatcher = var1;
      this.pendingChimes.add(var2.id());
      this.ghostId = var2.id();
      this.ghostName = var1;
   }

   private void playPendingChime() {
      if (!this.pendingChimes.isEmpty()) {
         boolean var1 = this.pendingChimes.stream().allMatch(StaffListModule::alarmedRecently);
         this.pendingChimes.clear();
         if (!var1) {
            if (MC.method_1483() != null) {
               MC.method_1483().method_4873(PositionedSoundInstance.method_4757(SoundEvents.field_15203, 1.0F, 1.0F));
            }

            StaffGhost.watching(this.ghostId, this.ghostName, 0);
         }
      }
   }

   private void news(String var1, Text var2, String var3) {
      ModuleChat.Body var4 = ModuleChat.body().name(NameFilter.apply(var1)).text(" ").append(var2);
      if (!var3.isEmpty()) {
         var4.text(" (" + var3 + ")");
      }

      ModuleChat.send(this, var4);
   }

   private void queueGamemodeUpdates(PlayerListS2CPacket var1) {
      if (!var1.method_46327().contains(Action.field_29136)) {
         for (Entry var3 : var1.method_46329()) {
            if (var3 != null && var3.comp_1110() != null && var3.comp_1106() != null) {
               this.observations.offer(AntiVanishModule.Observation.gamemode(var3.comp_1106(), var3.comp_1110().method_8381()));
            }
         }
      }
   }

   private void announceGamemode(UUID var1, String var2) {
      if (var1 != null && (MC.field_1724 == null || !MC.field_1724.method_5667().equals(var1))) {
         AntiVanishModule.KnownPlayer var3 = this.knownPlayers.get(var1);
         if (var3 != null && StaffListModule.isStaffName(var3.name)) {
            this.arrivals.put(spellKey(var3.name), System.currentTimeMillis());
            if (GameMode.field_9219.method_8381().equals(var2)) {
               if (this.credibleSubject(var1, var3.name)) {
                  this.reportHidden(var1, var3.name, true, "Vanish Event: switched to spectator", "", 100);
                  return;
               }
            } else if (this.endSpell(var3.name, true, 0L)) {
               return;
            }

            ModuleChat.send(this, StaffListModule.switchedTo(ModuleChat.body().name(NameFilter.apply(var3.name)).text(" "), var2));
         }
      }
   }

   private void announceTrigger(String var1, String var2, boolean var3, String var4) {
      long var6 = System.currentTimeMillis();
      Long var8 = this.announceCooldowns.get(var1);
      if (var8 == null || var6 - var8 >= 1500L) {
         while (!this.announceTimes.isEmpty() && var6 - this.announceTimes.peekFirst() > 4000L) {
            this.announceTimes.removeFirst();
         }

         if (this.announceTimes.size() < 6) {
            this.announceCooldowns.put(var1, var6);
            this.announceTimes.addLast(var6);
            int var9 = var4.indexOf(": ");
            ModuleChat.Body var10 = var9 < 0 ? ModuleChat.body().warn(var4) : ModuleChat.body().warn(var4.substring(0, var9)).text(var4.substring(var9));
            boolean var5 = var2 != null && !var2.isBlank() && !"Unknown".equalsIgnoreCase(var2) && !"CRITICAL".equalsIgnoreCase(var2);
            if (var5) {
               var10.text(" (");
               if (var3) {
                  var10.name(NameFilter.apply(var2));
               } else {
                  var10.value(var2);
               }

               var10.text(")");
            }

            ModuleChat.send(this, var10);
         }
      }
   }

   private void upsertDetection(String var1, String var2, String var3, int var4, long var5) {
      AntiVanishModule.Detection var7 = this.detections.get(var1);
      if (var7 == null) {
         this.detections.put(var1, new AntiVanishModule.Detection(var2, var3, var4, var5));
      } else {
         var7.name = var2;
         var7.expiresAt = Math.max(var7.expiresAt, var5);
         if (var4 >= var7.score) {
            var7.reason = var3;
            var7.score = var4;
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
         .filter(var2 -> var2.expiresAt > var1)
         .filter(AntiVanishModule::detectionWorthShowing)
         .sorted(
            Comparator.<AntiVanishModule.Detection>comparingInt(var0 -> var0.score).reversed().thenComparing(var0 -> var0.name, String.CASE_INSENSITIVE_ORDER)
         )
         .map(var0 -> new AntiVanishModule.HudEntry(var0.name, var0.reason))
         .filter(var1x -> var4.add(hudTag(var1x) + "\u0000" + hudValue(var1x)))
         .limit(4L)
         .forEach(var3::add);
      return List.copyOf(var3);
   }

   private void pruneState() {
      long var1 = System.currentTimeMillis();
      this.detections.entrySet().removeIf(var2 -> var2.getValue().expiresAt <= var1);
      this.signalCooldowns.entrySet().removeIf(var2 -> var1 - var2.getValue() > 60000L);
      this.announceCooldowns.entrySet().removeIf(var2 -> var1 - var2.getValue() > 60000L);
      this.selfPlacedBlocks.entrySet().removeIf(var2 -> var2.getValue() <= var1);
      this.confirmedDepartures.entrySet().removeIf(var2 -> var2.getValue() <= var1);
      this.automatedMechanisms.entrySet().removeIf(var2 -> var2.getValue() <= var1);
      this.weakParticleBursts.values().removeIf(var2 -> {
         while (!var2.isEmpty() && var1 - var2.peekFirst() > 2000L) {
            var2.removeFirst();
         }

         return var2.isEmpty();
      });

      while (!this.recentExplosions.isEmpty() && var1 - this.recentExplosions.peekFirst().timeMs > 3000L) {
         this.recentExplosions.removeFirst();
      }

      this.pruneSignals(var1);
   }

   private void pruneSignals(long var1) {
      while (!this.signals.isEmpty() && var1 - this.signals.peekFirst().timeMs > 15000L) {
         this.signals.removeFirst();
      }
   }

   private boolean nearPlayer(Vec3d var1) {
      return var1 != null && var1.method_1025(MC.field_1724.method_73189()) <= this.sensorRangeSq();
   }

   private boolean nearSelf(Vec3d var1, double var2) {
      return var1 != null && MC.field_1724 != null && var1.method_1025(MC.field_1724.method_73189()) < var2;
   }

   private boolean selfContainerActive() {
      return System.currentTimeMillis() - this.lastContainerActivityMs < 2500L;
   }

   private static boolean isContainerSignal(String var0) {
      String var1 = AntiVanishHeuristics.path(var0);
      return var1.contains("chest") || var1.contains("barrel") || var1.contains("shulker");
   }

   private String locatedSubject(Vec3d var1) {
      if (var1 != null && MC.field_1724 != null) {
         Vec3d var2 = MC.field_1724.method_73189();
         double var3 = var1.field_1352 - var2.field_1352;
         double var5 = var1.field_1351 - var2.field_1351;
         double var7 = var1.field_1350 - var2.field_1350;
         long var9 = Math.round(Math.sqrt(var3 * var3 + var5 * var5 + var7 * var7));
         String var11 = compass(var3, var7);
         return var11.isEmpty() ? var9 + "m" : var9 + "m " + var11;
      } else {
         return "nearby";
      }
   }

   private static String compass(double var0, double var2) {
      String var4 = var2 < -1.0 ? "N" : (var2 > 1.0 ? "S" : "");
      String var5 = var0 > 1.0 ? "E" : (var0 < -1.0 ? "W" : "");
      return var4 + var5;
   }

   private boolean hasVisibleCause(Vec3d var1) {
      if (var1 == null) {
         return true;
      } else {
         for (Entity var3 : MC.field_1687.method_18112()) {
            PlayerEntity var4;
            if (var3 != MC.field_1724
               && (
                  var3 instanceof PlayerEntity
                     ? !(var4 = (PlayerEntity)var3).method_5767() && var3.method_73189().method_1025(var1) <= 16.0
                     : var3 instanceof ProjectileEntity && var3.method_73189().method_1025(var1) <= 16.0
               )) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean isExplosionRelated(Vec3d var1) {
      if (var1 == null) {
         return false;
      } else {
         long var2 = System.currentTimeMillis();

         while (!this.recentExplosions.isEmpty() && var2 - this.recentExplosions.peekFirst().timeMs > 3000L) {
            this.recentExplosions.removeFirst();
         }

         for (AntiVanishModule.ExplosionEvent var5 : this.recentExplosions) {
            if (var5.center.method_1025(var1) <= var5.radius * var5.radius) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean hasAmbientParticleSource(Vec3d var1, String var2) {
      if (var1 != null && shortId(var2).contains("smoke")) {
         BlockPos var3 = BlockPos.method_49638(var1);

         for (int var4 = -2; var4 <= 2; var4++) {
            for (int var5 = -2; var5 <= 2; var5++) {
               for (int var6 = -2; var6 <= 2; var6++) {
                  BlockPos var7 = var3.method_10069(var4, var5, var6);
                  Identifier var8 = Registries.field_41175.method_10221(MC.field_1687.method_8320(var7).method_26204());
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

   private boolean isPoweredMechanism(Vec3d var1, String var2) {
      String var3 = shortId(var2);
      if (!var3.contains("door") && !var3.contains("trapdoor")) {
         return false;
      } else {
         BlockPos var4 = BlockPos.method_49638(var1);
         long var5 = System.currentTimeMillis();

         for (int var7 = -1; var7 <= 1; var7++) {
            BlockPos var9 = var4.method_10069(0, var7, 0);
            BlockState var10 = MC.field_1687.method_8320(var9);
            long var11 = var9.method_10063();
            boolean var8 = var10.method_28498(Properties.field_12484) && (Boolean)var10.method_11654(Properties.field_12484);
            if (var8 || MC.field_1687.method_49803(var9)) {
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

   private static double parseDouble(String var0, double var1) {
      try {
         return Double.parseDouble(var0);
      } catch (Exception var4) {
         return var1;
      }
   }

   private static String soundId(Identifier var0) {
      return var0 == null ? "" : var0.toString();
   }

   private static String shortId(String var0) {
      return AntiVanishHeuristics.path(var0);
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

      Detection(String var1, String var2, int var3, long var4) {
         this.name = var1;
         this.reason = var2;
         this.score = var3;
         this.expiresAt = var4;
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

      HiddenSpell(UUID var1, String var2, boolean var3, String var4, AntiVanishModule.RegionProof var5, int var6) {
         this.uuid = var1;
         this.name = var2;
         this.spectator = var3;
         this.detail = var4;
         this.proof = var5;
         this.sinceTick = var6;
      }

      Text how() {
         return this.spectator
            ? StaffListModule.switchedTo(ModuleChat.body(), GameMode.field_9219.method_8381()).build()
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
      static AntiVanishModule.Observation tabRemove(UUID var0) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.TAB_REMOVE, var0, -1, 0.0, 0.0, 0.0, "");
      }

      static AntiVanishModule.Observation tabHide(UUID var0) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.TAB_HIDE, var0, -1, 0.0, 0.0, 0.0, "");
      }

      static AntiVanishModule.Observation gamemode(UUID var0, String var1) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.GAMEMODE, var0, -1, 0.0, 0.0, 0.0, var1);
      }

      static AntiVanishModule.Observation playerLeft(String var0) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.PLAYER_LEFT, null, -1, 0.0, 0.0, 0.0, var0);
      }

      static AntiVanishModule.Observation systemChat(String var0) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.SYSTEM_CHAT, null, -1, 0.0, 0.0, 0.0, var0);
      }

      static AntiVanishModule.Observation entityMetadata(int var0) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.ENTITY_METADATA, null, var0, 0.0, 0.0, 0.0, "");
      }

      static AntiVanishModule.Observation explosion(Vec3d var0, float var1) {
         return new AntiVanishModule.Observation(
            AntiVanishModule.ObservationType.EXPLOSION, null, -1, var0.field_1352, var0.field_1351, var0.field_1350, Float.toString(var1)
         );
      }

      static AntiVanishModule.Observation entitySound(int var0, String var1) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.ENTITY_SOUND, null, var0, 0.0, 0.0, 0.0, var1);
      }

      static AntiVanishModule.Observation position(AntiVanishModule.ObservationType var0, double var1, double var3, double var5, String var7) {
         return new AntiVanishModule.Observation(var0, null, -1, var1, var3, var5, var7);
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
