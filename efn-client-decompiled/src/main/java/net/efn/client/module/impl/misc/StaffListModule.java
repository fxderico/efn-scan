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
      MinecraftClient var1 = MinecraftClient.method_1551();
      this.connection = var1.method_1562();
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

   public static String formatAge(long var0) {
      long var2 = Math.max(0L, var0) / 1000L;
      long var4 = var2 / 3600L;
      long var6 = var2 / 60L % 60L;
      long var8 = var2 % 60L;
      StringBuilder var10 = new StringBuilder(8);
      if (var4 > 0L) {
         var10.append(var4).append(var6 < 10L ? ":0" : ":");
      }

      return var10.append(var6).append(var8 < 10L ? ":0" : ":").append(var8).toString();
   }

   public static boolean isStaffName(String var0) {
      return var0 != null && STAFF_KEYS.contains(var0.toLowerCase(Locale.ROOT));
   }

   public static boolean isInYourRegion(UUID var0) {
      StaffListModule var1 = instance();
      if (var1 != null && var1.isEnabled() && var0 != null) {
         StaffListModule.Sighting var2 = var1.sightings.get(var0);
         return var2 != null && var2.marked(System.currentTimeMillis());
      } else {
         return false;
      }
   }

   public static void provenHere(UUID var0, String var1) {
      StaffListModule var2 = instance();
      if (var2 != null && var2.isEnabled() && var2.regionAlert.get() && var0 != null && var1 != null) {
         long var3 = System.currentTimeMillis();
         StaffListModule.Sighting var5 = var2.sightings.computeIfAbsent(var0, var0x -> new StaffListModule.Sighting());
         if (!var5.marked(var3)) {
            var2.resort = true;
         }

         var5.prove(var3);
         if (!var5.coolingDown(var3)) {
            var2.alert(var0, var5, var3, var1);
         }
      }
   }

   public static boolean alarmedRecently(UUID var0) {
      StaffListModule var1 = instance();
      if (var1 != null && var1.isEnabled() && var0 != null) {
         StaffListModule.Sighting var2 = var1.sightings.get(var0);
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

   private void onPacketNetty(PacketReceiveNettyEvent var1) {
      if (this.regionAlert.get()) {
         if (var1.packet instanceof PlayerListS2CPacket var3) {
            this.queueSwitches(var3);
         } else if (var1.packet instanceof BundleS2CPacket var8) {
            for (Packet var5 : var8.method_48324()) {
               if (var5 instanceof PlayerListS2CPacket var6) {
                  this.queueSwitches(var6);
               }
            }
         }
      }
   }

   private void queueSwitches(PlayerListS2CPacket var1) {
      EnumSet var2 = var1.method_46327();
      if (var2.contains(Action.field_29137) && !var2.contains(Action.field_29136)) {
         for (Entry var4 : var1.method_46329()) {
            if (var4 != null && var4.comp_1106() != null) {
               this.switches.offer(new StaffListModule.GameModeSwitch(var4.comp_1106(), var4.comp_1110()));
            }
         }
      }
   }

   private void onTick(TickEvent var1) {
      this.alarm.tick();
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayNetworkHandler var3 = var2.method_1562();
      if (var3 != null && var2.field_1724 != null && var2.field_1687 != null) {
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
            this.drainSwitches(var3, var2.field_1724.method_5667(), var5);
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

   private StaffListModule.Changes scan(MinecraftClient var1, ClientPlayNetworkHandler var2, long var3) {
      UUID var5 = var1.field_1724.method_5667();
      HashSet var6 = new HashSet();

      for (PlayerListEntry var8 : var2.method_45732()) {
         var6.add(var8.method_2966().id());
      }

      StaffListModule.Changes var18 = new StaffListModule.Changes();
      HashSet var19 = new HashSet();

      for (PlayerListEntry var10 : var2.method_2880()) {
         GameProfile var11 = var10.method_2966();
         UUID var12 = var11.id();
         String var13 = var11.name();
         if (var12 != null && !var12.equals(var5) && isStaffName(var13) && isAccount(var12, var13)) {
            var19.add(var12);
            StaffMatcher.Match var14 = this.rank(var10, var12, var13);
            StaffListModule.Presence var15 = var10.method_2958() == GameMode.field_9219
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
            if (var1.field_1687.method_18470(var21.id) != null) {
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

      this.pruneSightings(var3);
      this.sortView(var3);
      return var18;
   }

   private void sortView(long var1) {
      ArrayList var3 = new ArrayList<>(this.staff.values());
      var3.sort(
         Comparator.<StaffListModule.Staff>comparingInt(var3x -> this.group(var3x, var1))
            .thenComparingInt(var0 -> var0.seniority)
            .thenComparing(var0 -> var0.name, String.CASE_INSENSITIVE_ORDER)
      );
      this.sorted = List.copyOf(var3);
      this.resort = false;
   }

   private int group(StaffListModule.Staff var1, long var2) {
      StaffListModule.Sighting var4 = this.sightings.get(var1.id);
      if (var4 != null && var4.nearby) {
         return 0;
      } else if (var4 != null && var4.marked(var2)) {
         return 1;
      } else {
         return var1.presence != StaffListModule.Presence.ONLINE ? 2 : 3;
      }
   }

   private static boolean isAccount(UUID var0, String var1) {
      return var1 != null && var0.version() != 2 && ACCOUNT_NAME.matcher(var1).matches();
   }

   private StaffMatcher.Match rank(PlayerListEntry var1, UUID var2, String var3) {
      Team var4 = var1.method_2955();
      Text var5 = var1.method_2971();
      Text var6 = var4 == null ? null : var4.method_1144();
      Text var7 = var4 == null ? null : var4.method_1136();
      StaffListModule.Classified var8 = this.classified.get(var2);
      if (var8 != null && var8.isFor(var3, var5, var4, var6, var7)) {
         return var8.match();
      } else {
         StaffMatcher.Match var9 = RANKS.match(var3, var4 == null ? null : var4.method_1197(), plain(var5), plain(var6), plain(var7));
         this.classified.put(var2, new StaffListModule.Classified(var3, var5, var4, var6, var7, var9));
         return var9;
      }
   }

   private static String plain(Text var0) {
      return var0 == null ? null : var0.getString();
   }

   private void drainSwitches(ClientPlayNetworkHandler var1, UUID var2, long var3) {
      StaffListModule.GameModeSwitch var5;
      while ((var5 = this.switches.poll()) != null) {
         UUID var7 = var5.id();
         if (!var7.equals(var2)) {
            StaffListModule.Staff var8 = this.staff.get(var7);
            String var6;
            if (var8 != null) {
               var6 = var8.name;
            } else {
               PlayerListEntry var9 = var1.method_2871(var7);
               var6 = var9 == null ? null : var9.method_2966().name();
               if (!isStaffName(var6) || !isAccount(var7, var6)) {
                  continue;
               }
            }

            StaffListModule.Sighting var11 = this.sightings.computeIfAbsent(var7, var0 -> new StaffListModule.Sighting());
            if (!var11.marked(var3)) {
               this.resort = true;
            }

            var11.prove(var3);
            if (!var11.coolingDown(var3)) {
               GameMode var10 = var5.mode();
               var11.switchAlertTick = this.tickCount;
               var11.switchedToSpectator = var10 == GameMode.field_9219;
               this.alert(var7, var11, var3, var6);
            }
         }
      }
   }

   private void trackBodies(MinecraftClient var1, long var2) {
      ClientPlayerEntity var4 = var1.field_1724;

      for (AbstractClientPlayerEntity var6 : var1.field_1687.method_18456()) {
         StaffListModule.Staff var7;
         if (var6 != var4 && (var7 = this.staff.get(var6.method_5667())) != null) {
            StaffListModule.Sighting var8 = this.sightings.computeIfAbsent(var7.id, var0 -> new StaffListModule.Sighting());
            var8.prove(var2);
            var8.bodyTick = this.tickCount;
            var8.distance = var4.method_5739(var6);
            if (!var8.nearby) {
               var8.nearby = true;
               this.resort = true;
               boolean var9 = var8.bodyLostAt == 0L || var2 - var8.bodyLostAt > 10000L;
               if (var9) {
                  this.alert(var7.id, var8, var2, var7.name);
               }
            }
         }
      }

      for (StaffListModule.Sighting var11 : this.sightings.values()) {
         if (var11.nearby && var11.bodyTick != this.tickCount) {
            var11.nearby = false;
            var11.bodyLostAt = var2;
            this.resort = true;
         }
      }
   }

   private void pruneSightings(long var1) {
      Iterator var3 = this.sightings.entrySet().iterator();

      while (var3.hasNext()) {
         java.util.Map.Entry var4 = (java.util.Map.Entry)var3.next();
         StaffListModule.Sighting var5 = (StaffListModule.Sighting)var4.getValue();
         if (this.staff.containsKey(var4.getKey())) {
            var5.untrackedSince = 0L;
         } else if (var5.untrackedSince == 0L) {
            var5.untrackedSince = var1;
         } else if (var1 - var5.untrackedSince > 10000L) {
            var3.remove();
            this.resort = true;
         }
      }
   }

   private void alert(UUID var1, StaffListModule.Sighting var2, long var3, String var5) {
      var2.alertedAt = var3;
      var2.highlightUntil = var3 + 4000L;
      if (this.pendingAlerts.isEmpty()) {
         this.alertGather = 5;
      }

      this.pendingAlerts.put(var1, var5);
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

   private void announce(MinecraftClient var1, StaffListModule.Changes var2) {
      var2.changed.removeIf(this::echoesSwitchAlert);
      if (!var2.isEmpty()) {
         ArrayList var4 = new ArrayList();
         describe(var4, var2.joined, ModuleChat.body().warn("joined").build(), (var0, var1x) -> {
            var0.warn("joined");
            String var2x = note(var1x);
            if (var2x != null) {
               var0.text(" ").danger(var2x);
            }
         });
         describe(var4, var2.changed, ModuleChat.body().warn("changed visibility").build(), (var0, var1x) -> {
            switch (var1x.presence) {
               case ONLINE:
                  var0.text("is ").good("visible again");
                  break;
               case SPECTATOR:
                  switchedTo(var0, GameMode.field_9219.method_8381());
                  break;
               case VANISHED:
                  var0.danger("vanished").text(var1x.offTabScans > 0 ? " (off TAB, still nearby)" : " (hidden from TAB)");
            }
         });
         describe(var4, var2.left, ModuleChat.body().good("left").build(), (var0, var1x) -> {
            if (var1x.presence == StaffListModule.Presence.VANISHED) {
               var0.text("is ").warn("out of sight");
            } else {
               var0.good("left");
            }
         });

         for (ModuleChat.Body var6 : var4) {
            ModuleChat.send(this, var6);
         }

         boolean var3 = !var2.joined.isEmpty() || var2.changed.stream().anyMatch(var0 -> var0.presence != StaffListModule.Presence.ONLINE);
         if (var3 && var1.method_1483() != null) {
            var1.method_1483().method_4873(PositionedSoundInstance.method_47978(SoundEvents.field_14725, 1.25F));
         }
      }
   }

   private boolean echoesSwitchAlert(StaffListModule.Staff var1) {
      StaffListModule.Sighting var2 = this.sightings.get(var1.id);
      if (var2 != null && var2.switchAlertTick >= 0 && this.tickCount - var2.switchAlertTick <= 20) {
         return var1.presence == StaffListModule.Presence.SPECTATOR
            ? var2.switchedToSpectator
            : var1.presence == StaffListModule.Presence.ONLINE && !var2.switchedToSpectator;
      } else {
         return false;
      }
   }

   private static void describe(
      List<ModuleChat.Body> var0, List<StaffListModule.Staff> var1, Text var2, BiConsumer<ModuleChat.Body, StaffListModule.Staff> var3
   ) {
      if (!var1.isEmpty()) {
         if (var1.size() > 3) {
            ArrayList var7 = new ArrayList(var1.size());

            for (StaffListModule.Staff var9 : var1) {
               var7.add(label(var9));
            }

            var0.add(list(ModuleChat.body().value(var1.size()).text(" staff ").append(var2).text(": "), var7));
         } else {
            for (StaffListModule.Staff var5 : var1) {
               ModuleChat.Body var6 = ModuleChat.body().append(label(var5)).text(" ");
               var3.accept(var6, var5);
               var0.add(var6);
            }
         }
      }
   }

   private static Text label(StaffListModule.Staff var0) {
      return label(var0.name, var0.rank);
   }

   private static Text label(String var0, String var1) {
      return ModuleChat.body().name(NameFilter.apply(var0)).text(" (" + var1 + ")").build();
   }

   private static String note(StaffListModule.Staff var0) {
      return switch (var0.presence) {
         case ONLINE -> null;
         case SPECTATOR -> "spectating";
         case VANISHED -> "vanished";
      };
   }

   private static ModuleChat.Body list(ModuleChat.Body var0, List<Text> var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         if (var2 > 0) {
            var0.text(", ");
         }

         var0.append((Text)var1.get(var2));
      }

      return var0;
   }

   static ModuleChat.Body switchedTo(ModuleChat.Body var0, String var1) {
      if (var1 == null) {
         return var0.text("switched game mode");
      } else {
         var0.text("switched to ");
         if (GameMode.field_9219.method_8381().equals(var1)) {
            return var0.danger(var1);
         } else {
            return GameMode.field_9220.method_8381().equals(var1) ? var0.warn(var1) : var0.name(var1);
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
      boolean isFor(String var1, Text var2, Team var3, Text var4, Text var5) {
         return this.display == var2 && this.team == var3 && this.prefix == var4 && this.suffix == var5 && this.name.equals(var1);
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

      boolean marked(long var1) {
         return this.nearby || this.provenAt != 0L && var1 - this.provenAt < 600000L;
      }

      void prove(long var1) {
         if (!this.watching(var1)) {
            this.watchingSince = var1;
         }

         this.provenAt = var1;
      }

      boolean watching(long var1) {
         return this.nearby || this.provenAt != 0L && var1 - this.provenAt < 10000L;
      }

      boolean coolingDown(long var1) {
         return this.alertedAt != 0L && var1 - this.alertedAt < 120000L;
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

      Staff(UUID var1) {
         this.id = var1;
      }

      void update(String var1, StaffMatcher.Match var2, StaffListModule.Presence var3) {
         this.name = var1;
         this.rank = var2.rank();
         this.seniority = var2.seniority();
         this.presence = var3;
      }
   }
}
