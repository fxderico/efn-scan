package net.efn.client.module.impl.misc;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.StaffGhost;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.util.NameFilter;
import net.efn.client.util.staff.ShardTracker;
import net.efn.client.util.staff.StaffAlarm;
import net.efn.client.util.staff.StaffMatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

@Environment(EnvType.CLIENT)
public final class StaffListModule extends ClientModule {
   public static final List<String> DONUT_STAFF = List.of(
      "FluffyMaster07",
      "archivePedro",
      "Munkerlich",
      "Frenk_Btw",
      "Napooo_",
      "auzzitech",
      "CryptoDaveYT",
      "W1zox_",
      "ZEEF69",
      "showered",
      "CaptainMoose35",
      "BobIsFound",
      "NoahvdAa",
      "0GSummer",
      "LzouzMP5",
      "Pastagamer08",
      "u_vv",
      "Owen1212055",
      "Splaterd",
      "Fallerfly",
      "Bloodspulse",
      "_chaon",
      "Dough4",
      "evify"
   );
   private static final Set<String> STAFF_KEYS = staffKeys();
   private static final StaffMatcher RANKS = new StaffMatcher(
      "Ranks + Markers + Team",
      "owner, co-owner, manager, admin, administrator, developer, dev, sr-admin, sr-mod, senior-mod, moderator, mod, trial-mod, sr-helper, senior-helper, helper, trial-helper, support, staff",
      "U+2605 U+2606 U+2726 U+2727 U+272A U+272F U+2730 U+2B50",
      false,
      5,
      String.join(",", DONUT_STAFF),
      ""
   );
   private static final int SCAN_INTERVAL_TICKS = 10;
   private static final int SETTLE_TICKS = 60;
   private static final int VANISH_CONFIRM_SCANS = 2;
   private static final int GROUP_THRESHOLD = 3;
   private static final int ALERT_GATHER_TICKS = 5;
   private static final int SWITCH_ECHO_TICKS = 20;
   public static final long ALERT_HIGHLIGHT_MS = 4000L;
   private static final long ALERT_COOLDOWN_MS = 120000L;
   private static final long SERVER_MARK_MS = 600000L;
   private static final int MAX_ROWS = 8;
   private static final float ALARM_SECONDS = 3.0F;
   private static final long SHARED_ALARM_MS = 5000L;
   private static final long NEARBY_GRACE_MS = 10000L;
   private static final long SIGHTING_GRACE_MS = 10000L;
   private static final long WATCH_GRACE_MS = 10000L;
   private static final Pattern ACCOUNT_NAME = Pattern.compile("[.*]?[A-Za-z0-9_]{1,16}");
   private final BooleanSetting regionAlert = this.addSetting("server-alert", new BooleanSetting("Region Alert", true));
   private final Map<UUID, StaffListModule.Staff> staff = new LinkedHashMap<>();
   private List<StaffListModule.Staff> sorted = List.of();
   private boolean resort;
   private final Map<UUID, StaffListModule.Classified> classified = new HashMap<>();
   private final Map<UUID, StaffListModule.Sighting> sightings = new HashMap<>();
   private final Queue<StaffListModule.GameModeSwitch> switches = new ConcurrentLinkedQueue<>();
   private final Map<UUID, String> pendingAlerts = new LinkedHashMap<>();
   private final StaffAlarm alarm = new StaffAlarm();
   private ClientPlayNetworkHandler connection;
   private final ShardTracker shard = new ShardTracker();
   private int ticksToScan;
   private int settleTicks;
   private boolean primed;
   private int alertGather;
   private int tickCount;

   public StaffListModule() {
      super("staff-list", "Staff List", "Lists online DonutSMP staff.", ModuleCategory.DONUT);
      this.usesNotifications();
      this.regionAlert.onChange(() -> {
         if (!this.regionAlert.get()) {
            this.forgetShard();
         }
      });
   }

   @Override
   protected void onEnable() {
      this.forget();
      MinecraftClient var1 = MinecraftClient.getInstance();
      this.connection = var1.getNetworkHandler();
      this.shard.clear();
      this.settleTicks = 0;
      this.ticksToScan = 0;
      this.primed = false;
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      this.on(PacketReceiveNettyEvent.TYPE, this::onPacketNetty);
   }

   @Override
   protected void onDisable() {
      this.forget();
      this.connection = null;
      this.shard.clear();
   }

   private void forget() {
      this.staff.clear();
      this.sorted = List.of();
      this.classified.clear();
      this.forgetShard();
   }

   private void forgetShard() {
      this.switches.clear();
      this.sightings.clear();
      this.pendingAlerts.clear();
      this.alarm.stop();
      this.resort = true;
   }

   public static boolean shouldShowHud() {
      StaffListModule var0 = instance();
      return var0 != null && var0.isEnabled() && !var0.sorted.isEmpty();
   }

   public static StaffListModule.HudList hudRows() {
      StaffListModule var0 = instance();
      if (var0 != null && var0.isEnabled()) {
         List var1 = var0.sorted;
         int var2 = var1.size() <= 8 ? var1.size() : 7;
         long var3 = System.currentTimeMillis();
         ArrayList var5 = new ArrayList(var2);

         for (int var6 = 0; var6 < var2; var6++) {
            StaffListModule.Staff var7 = (StaffListModule.Staff)var1.get(var6);
            StaffListModule.Sighting var8 = var0.sightings.get(var7.id);
            boolean var9 = var8 != null && var8.watching(var3);
            boolean var10 = var8 != null && var8.nearby;
            float var11 = var8 == null ? 0.0F : Math.max(0.0F, Math.min(1.0F, (float)(var8.highlightUntil - var3) / 4000.0F));
            String var12 = NameFilter.apply(var7.name);
            var5.add(
               new StaffListModule.HudRow(
                  var12,
                  var7.id,
                  var7.presence,
                  var9,
                  var9 ? Math.max(0L, var3 - var8.watchingSince) : -1L,
                  var10,
                  var10 ? var8.distance : -1.0F,
                  var11,
                  !var12.equals(var7.name)
               )
            );
         }

         return new StaffListModule.HudList(var5, var1.size() - var2);
      } else {
         return StaffListModule.HudList.EMPTY;
      }
   }

   public static String formatAge(long l) {
      long var2 = Math.max(0L, l) / 1000L;
      long var4 = var2 / 3600L;
      long var6 = var2 / 60L % 60L;
      long var8 = var2 % 60L;
      StringBuilder var10 = new StringBuilder(8);
      if (var4 > 0L) {
         var10.append(var4).append(var6 < 10L ? ":0" : ":");
      }

      return var10.append(var6).append(var8 < 10L ? ":0" : ":").append(var8).toString();
   }

   public static boolean isStaffName(String string) {
      return string != null && STAFF_KEYS.contains(string.toLowerCase(Locale.ROOT));
   }

   public static boolean isInYourRegion(UUID uUID) {
      StaffListModule var1 = instance();
      if (var1 != null && var1.isEnabled() && uUID != null) {
         StaffListModule.Sighting var2 = var1.sightings.get(uUID);
         return var2 != null && var2.marked(System.currentTimeMillis());
      } else {
         return false;
      }
   }

   public static void provenHere(UUID uUID, String string) {
      StaffListModule var2 = instance();
      if (var2 != null && var2.isEnabled() && var2.regionAlert.get() && uUID != null && string != null) {
         long var3 = System.currentTimeMillis();
         StaffListModule.Sighting var5 = var2.sightings.computeIfAbsent(uUID, uUIDx -> new StaffListModule.Sighting());
         if (!var5.marked(var3)) {
            var2.resort = true;
         }

         var5.prove(var3);
         if (!var5.coolingDown(var3)) {
            var2.alert(uUID, var5, var3, string);
         }
      }
   }

   public static boolean alarmedRecently(UUID uUID) {
      StaffListModule var1 = instance();
      if (var1 != null && var1.isEnabled() && uUID != null) {
         StaffListModule.Sighting var2 = var1.sightings.get(uUID);
         return var2 != null && var2.alertedAt != 0L && System.currentTimeMillis() - var2.alertedAt < 5000L;
      } else {
         return false;
      }
   }

   private static Set<String> staffKeys() {
      HashSet var0 = new HashSet();

      for (String var2 : DONUT_STAFF) {
         var0.add(var2.toLowerCase(Locale.ROOT));
      }

      return Set.copyOf(var0);
   }

   private static StaffListModule instance() {
      return ModuleRegistry.get(StaffListModule.class);
   }

   private void onPacketNetty(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      if (this.regionAlert.get()) {
         if (packetReceiveNettyEvent.packet instanceof PlayerListS2CPacket var3) {
            this.queueSwitches(var3);
         } else if (packetReceiveNettyEvent.packet instanceof BundleS2CPacket var8) {
            for (Packet var5 : var8.getPackets()) {
               if (var5 instanceof PlayerListS2CPacket var6) {
                  this.queueSwitches(var6);
               }
            }
         }
      }
   }

   private void queueSwitches(PlayerListS2CPacket playerListS2CPacket) {
      EnumSet var2 = playerListS2CPacket.getActions();
      if (var2.contains(Action.UPDATE_GAME_MODE) && !var2.contains(Action.ADD_PLAYER)) {
         for (Entry var4 : playerListS2CPacket.getEntries()) {
            if (var4 != null && var4.profileId() != null) {
               this.switches.offer(new StaffListModule.GameModeSwitch(var4.profileId(), var4.gameMode()));
            }
         }
      }
   }

   private void onTick(TickEvent tickEvent) {
      this.alarm.tick();
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayNetworkHandler var3 = var2.getNetworkHandler();
      if (var3 != null && var2.player != null && var2.world != null) {
         boolean var4 = this.shard.moved(var2);
         if (var3 != this.connection) {
            this.connection = var3;
            this.forget();
            this.settleTicks = 60;
            this.primed = false;
         } else if (var4) {
            this.forgetShard();
         }

         if (this.settleTicks > 0) {
            this.settleTicks--;
         }

         this.tickCount++;
         long var5 = System.currentTimeMillis();
         if (this.regionAlert.get()) {
            this.drainSwitches(var3, var2.player.getUuid(), var5);
            this.trackBodies(var2, var5);
         }

         if (--this.ticksToScan <= 0) {
            this.ticksToScan = 10;
            StaffListModule.Changes var7 = this.scan(var2, var3, var5);
            if (this.primed) {
               this.announce(var2, var7);
            } else if (this.settleTicks == 0) {
               this.primed = true;
            }
         }

         if (!this.pendingAlerts.isEmpty() && --this.alertGather <= 0) {
            this.raise();
         }

         if (this.resort) {
            this.sortView(var5);
         }
      } else {
         if (this.connection != null || !this.staff.isEmpty() || !this.classified.isEmpty() || !this.sightings.isEmpty()) {
            this.forget();
         }

         this.switches.clear();
         this.connection = null;
         this.shard.clear();
         this.primed = false;
      }
   }

   private StaffListModule.Changes scan(MinecraftClient minecraftClient, ClientPlayNetworkHandler clientPlayNetworkHandler, long l) {
      UUID var5 = minecraftClient.player.getUuid();
      HashSet var6 = new HashSet();

      for (PlayerListEntry var8 : clientPlayNetworkHandler.getListedPlayerListEntries()) {
         var6.add(var8.getProfile().id());
      }

      StaffListModule.Changes var18 = new StaffListModule.Changes();
      HashSet var19 = new HashSet();

      for (PlayerListEntry var10 : clientPlayNetworkHandler.getPlayerList()) {
         GameProfile var11 = var10.getProfile();
         UUID var12 = var11.id();
         String var13 = var11.name();
         if (var12 != null && !var12.equals(var5) && isStaffName(var13) && isAccount(var12, var13)) {
            var19.add(var12);
            StaffMatcher.Match var14 = this.rank(var10, var12, var13);
            StaffListModule.Presence var15 = var10.getGameMode() == GameMode.SPECTATOR
               ? StaffListModule.Presence.SPECTATOR
               : (var6.contains(var12) ? StaffListModule.Presence.ONLINE : StaffListModule.Presence.VANISHED);
            StaffListModule.Staff var16 = this.staff.get(var12);
            if (var16 == null) {
               var16 = new StaffListModule.Staff(var12);
               this.staff.put(var12, var16);
               var16.update(var13, var14, var15);
               var18.joined.add(var16);
            } else {
               StaffListModule.Presence var17 = var16.presence;
               var16.update(var13, var14, var15);
               var16.offTabScans = 0;
               if (var17 != var15) {
                  var18.changed.add(var16);
               }
            }
         }
      }

      this.classified.keySet().retainAll(var19);
      Iterator var20 = this.staff.values().iterator();

      while (var20.hasNext()) {
         StaffListModule.Staff var21 = (StaffListModule.Staff)var20.next();
         if (!var19.contains(var21.id)) {
            if (minecraftClient.world.getPlayerByUuid(var21.id) != null) {
               if (++var21.offTabScans >= 2 && var21.presence != StaffListModule.Presence.VANISHED) {
                  var21.presence = StaffListModule.Presence.VANISHED;
                  var18.changed.add(var21);
               }
            } else {
               var20.remove();
               var18.left.add(var21);
            }
         }
      }

      this.pruneSightings(l);
      this.sortView(l);
      return var18;
   }

   private void sortView(long l) {
      ArrayList var3 = new ArrayList<>(this.staff.values());
      var3.sort(
         Comparator.<StaffListModule.Staff>comparingInt(staff -> this.group(staff, l))
            .thenComparingInt(staff -> staff.seniority)
            .thenComparing(staff -> staff.name, String.CASE_INSENSITIVE_ORDER)
      );
      this.sorted = List.copyOf(var3);
      this.resort = false;
   }

   private int group(StaffListModule.Staff staff, long l) {
      StaffListModule.Sighting var4 = this.sightings.get(staff.id);
      if (var4 != null && var4.nearby) {
         return 0;
      } else if (var4 != null && var4.marked(l)) {
         return 1;
      } else {
         return staff.presence != StaffListModule.Presence.ONLINE ? 2 : 3;
      }
   }

   private static boolean isAccount(UUID uUID, String string) {
      return string != null && uUID.version() != 2 && ACCOUNT_NAME.matcher(string).matches();
   }

   private StaffMatcher.Match rank(PlayerListEntry playerListEntry, UUID uUID, String string) {
      Team var4 = playerListEntry.getScoreboardTeam();
      Text var5 = playerListEntry.getDisplayName();
      Text var6 = var4 == null ? null : var4.getPrefix();
      Text var7 = var4 == null ? null : var4.getSuffix();
      StaffListModule.Classified var8 = this.classified.get(uUID);
      if (var8 != null && var8.isFor(string, var5, var4, var6, var7)) {
         return var8.match();
      } else {
         StaffMatcher.Match var9 = RANKS.match(string, var4 == null ? null : var4.getName(), plain(var5), plain(var6), plain(var7));
         this.classified.put(uUID, new StaffListModule.Classified(string, var5, var4, var6, var7, var9));
         return var9;
      }
   }

   private static String plain(Text text) {
      return text == null ? null : text.getString();
   }

   private void drainSwitches(ClientPlayNetworkHandler clientPlayNetworkHandler, UUID uUID, long l) {
      StaffListModule.GameModeSwitch var5;
      while ((var5 = this.switches.poll()) != null) {
         UUID var7 = var5.id();
         if (!var7.equals(uUID)) {
            StaffListModule.Staff var8 = this.staff.get(var7);
            String var6;
            if (var8 != null) {
               var6 = var8.name;
            } else {
               PlayerListEntry var9 = clientPlayNetworkHandler.getPlayerListEntry(var7);
               var6 = var9 == null ? null : var9.getProfile().name();
               if (!isStaffName(var6) || !isAccount(var7, var6)) {
                  continue;
               }
            }

            StaffListModule.Sighting var11 = this.sightings.computeIfAbsent(var7, uUIDx -> new StaffListModule.Sighting());
            if (!var11.marked(l)) {
               this.resort = true;
            }

            var11.prove(l);
            if (!var11.coolingDown(l)) {
               GameMode var10 = var5.mode();
               var11.switchAlertTick = this.tickCount;
               var11.switchedToSpectator = var10 == GameMode.SPECTATOR;
               this.alert(var7, var11, l, var6);
            }
         }
      }
   }

   private void trackBodies(MinecraftClient minecraftClient, long l) {
      ClientPlayerEntity var4 = minecraftClient.player;

      for (AbstractClientPlayerEntity var6 : minecraftClient.world.getPlayers()) {
         StaffListModule.Staff var7;
         if (var6 != var4 && (var7 = this.staff.get(var6.getUuid())) != null) {
            StaffListModule.Sighting var8 = this.sightings.computeIfAbsent(var7.id, uUID -> new StaffListModule.Sighting());
            var8.prove(l);
            var8.bodyTick = this.tickCount;
            var8.distance = var4.distanceTo(var6);
            if (!var8.nearby) {
               var8.nearby = true;
               this.resort = true;
               boolean var9 = var8.bodyLostAt == 0L || l - var8.bodyLostAt > 10000L;
               if (var9) {
                  this.alert(var7.id, var8, l, var7.name);
               }
            }
         }
      }

      for (StaffListModule.Sighting var11 : this.sightings.values()) {
         if (var11.nearby && var11.bodyTick != this.tickCount) {
            var11.nearby = false;
            var11.bodyLostAt = l;
            this.resort = true;
         }
      }
   }

   private void pruneSightings(long l) {
      Iterator var3 = this.sightings.entrySet().iterator();

      while (var3.hasNext()) {
         java.util.Map.Entry var4 = (java.util.Map.Entry)var3.next();
         StaffListModule.Sighting var5 = (StaffListModule.Sighting)var4.getValue();
         if (this.staff.containsKey(var4.getKey())) {
            var5.untrackedSince = 0L;
         } else if (var5.untrackedSince == 0L) {
            var5.untrackedSince = l;
         } else if (l - var5.untrackedSince > 10000L) {
            var3.remove();
            this.resort = true;
         }
      }
   }

   private void alert(UUID uUID, StaffListModule.Sighting sighting, long l, String string) {
      sighting.alertedAt = l;
      sighting.highlightUntil = l + 4000L;
      if (this.pendingAlerts.isEmpty()) {
         this.alertGather = 5;
      }

      this.pendingAlerts.put(uUID, string);
   }

   private void raise() {
      java.util.Map.Entry var1 = this.pendingAlerts.entrySet().iterator().next();
      UUID var2 = (UUID)var1.getKey();
      String var3 = (String)var1.getValue();
      int var4 = this.pendingAlerts.size() - 1;
      this.pendingAlerts.clear();
      this.alarm.start("Guardian", 3.0F, 1.0F);
      StaffGhost.watching(var2, var3, var4);
   }

   private void announce(MinecraftClient minecraftClient, StaffListModule.Changes changes) {
      changes.changed.removeIf(this::echoesSwitchAlert);
      if (!changes.isEmpty()) {
         ArrayList var4 = new ArrayList();
         describe(var4, changes.joined, ModuleChat.body().warn("joined").build(), (body, staff) -> {
            body.warn("joined");
            String var2 = note(staff);
            if (var2 != null) {
               body.text(" ").danger(var2);
            }
         });
         describe(var4, changes.changed, ModuleChat.body().warn("changed visibility").build(), (body, staff) -> {
            switch (staff.presence) {
               case ONLINE:
                  body.text("is ").good("visible again");
                  break;
               case SPECTATOR:
                  switchedTo(body, GameMode.SPECTATOR.getId());
                  break;
               case VANISHED:
                  body.danger("vanished").text(staff.offTabScans > 0 ? " (off TAB, still nearby)" : " (hidden from TAB)");
            }
         });
         describe(var4, changes.left, ModuleChat.body().good("left").build(), (body, staff) -> {
            if (staff.presence == StaffListModule.Presence.VANISHED) {
               body.text("is ").warn("out of sight");
            } else {
               body.good("left");
            }
         });

         for (ModuleChat.Body var6 : var4) {
            ModuleChat.send(this, var6);
         }

         boolean var3 = !changes.joined.isEmpty() || changes.changed.stream().anyMatch(staff -> staff.presence != StaffListModule.Presence.ONLINE);
         if (var3 && minecraftClient.getSoundManager() != null) {
            minecraftClient.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_CHIME, 1.25F));
         }
      }
   }

   private boolean echoesSwitchAlert(StaffListModule.Staff staff) {
      StaffListModule.Sighting var2 = this.sightings.get(staff.id);
      if (var2 != null && var2.switchAlertTick >= 0 && this.tickCount - var2.switchAlertTick <= 20) {
         return staff.presence == StaffListModule.Presence.SPECTATOR
            ? var2.switchedToSpectator
            : staff.presence == StaffListModule.Presence.ONLINE && !var2.switchedToSpectator;
      } else {
         return false;
      }
   }

   private static void describe(
      List<ModuleChat.Body> list, List<StaffListModule.Staff> list2, Text text, BiConsumer<ModuleChat.Body, StaffListModule.Staff> biConsumer
   ) {
      if (!list2.isEmpty()) {
         if (list2.size() > 3) {
            ArrayList var7 = new ArrayList(list2.size());

            for (StaffListModule.Staff var9 : list2) {
               var7.add(label(var9));
            }

            list.add(list(ModuleChat.body().value(list2.size()).text(" staff ").append(text).text(": "), var7));
         } else {
            for (StaffListModule.Staff var5 : list2) {
               ModuleChat.Body var6 = ModuleChat.body().append(label(var5)).text(" ");
               biConsumer.accept(var6, var5);
               list.add(var6);
            }
         }
      }
   }

   private static Text label(StaffListModule.Staff staff) {
      return label(staff.name, staff.rank);
   }

   private static Text label(String string, String string2) {
      return ModuleChat.body().name(NameFilter.apply(string)).text(" (" + string2 + ")").build();
   }

   private static String note(StaffListModule.Staff staff) {
      return switch (staff.presence) {
         case ONLINE -> null;
         case SPECTATOR -> "spectating";
         case VANISHED -> "vanished";
      };
   }

   private static ModuleChat.Body list(ModuleChat.Body body, List<Text> list) {
      for (int var2 = 0; var2 < list.size(); var2++) {
         if (var2 > 0) {
            body.text(", ");
         }

         body.append((Text)list.get(var2));
      }

      return body;
   }

   static ModuleChat.Body switchedTo(ModuleChat.Body body, String string) {
      if (string == null) {
         return body.text("switched game mode");
      } else {
         body.text("switched to ");
         if (GameMode.SPECTATOR.getId().equals(string)) {
            return body.danger(string);
         } else {
            return GameMode.CREATIVE.getId().equals(string) ? body.warn(string) : body.name(string);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Changes {
      final List<StaffListModule.Staff> joined = new ArrayList<>();
      final List<StaffListModule.Staff> changed = new ArrayList<>();
      final List<StaffListModule.Staff> left = new ArrayList<>();

      private Changes() {
      }

      boolean isEmpty() {
         return this.joined.isEmpty() && this.changed.isEmpty() && this.left.isEmpty();
      }
   }

   @Environment(EnvType.CLIENT)
   private record Classified(String name, Text display, Team team, Text prefix, Text suffix, StaffMatcher.Match match) {
      boolean isFor(String string, Text text, Team team, Text text2, Text text3) {
         return this.display == text && this.team == team && this.prefix == text2 && this.suffix == text3 && this.name.equals(string);
      }
   }

   @Environment(EnvType.CLIENT)
   private record GameModeSwitch(UUID id, GameMode mode) {
   }

   @Environment(EnvType.CLIENT)
   public record HudList(List<StaffListModule.HudRow> rows, int more) {
      static final StaffListModule.HudList EMPTY = new StaffListModule.HudList(List.of(), 0);
   }

   @Environment(EnvType.CLIENT)
   public record HudRow(
      String name,
      UUID id,
      StaffListModule.Presence presence,
      boolean watching,
      long watchingForMs,
      boolean nearby,
      float distance,
      float alertLevel,
      boolean nameHidden
   ) {
      public String watchingFor() {
         return this.watching ? StaffListModule.formatAge(this.watchingForMs) : "";
      }

      public String distanceLabel() {
         return this.nearby ? Math.round(this.distance) + "m" : "";
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Presence {
      ONLINE,
      SPECTATOR,
      VANISHED;

      private Presence() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Sighting {
      long provenAt;
      long watchingSince;
      long alertedAt;
      long highlightUntil;
      boolean nearby;
      float distance;
      long bodyLostAt;
      int bodyTick;
      int switchAlertTick = -1;
      boolean switchedToSpectator;
      long untrackedSince;

      private Sighting() {
      }

      boolean marked(long l) {
         return this.nearby || this.provenAt != 0L && l - this.provenAt < 600000L;
      }

      void prove(long l) {
         if (!this.watching(l)) {
            this.watchingSince = l;
         }

         this.provenAt = l;
      }

      boolean watching(long l) {
         return this.nearby || this.provenAt != 0L && l - this.provenAt < 10000L;
      }

      boolean coolingDown(long l) {
         return this.alertedAt != 0L && l - this.alertedAt < 120000L;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Staff {
      final UUID id;
      String name = "";
      String rank = "";
      int seniority;
      StaffListModule.Presence presence = StaffListModule.Presence.ONLINE;
      int offTabScans;

      Staff(UUID uUID) {
         this.id = uUID;
      }

      void update(String string, StaffMatcher.Match match, StaffListModule.Presence presence) {
         this.name = string;
         this.rank = match.rank();
         this.seniority = match.seniority();
         this.presence = presence;
      }
   }
}
