package net.efn.client.module.impl.misc;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.efn.client.gui.GlintHeadRenderer;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.ChunkAreas;
import net.efn.client.util.FoglessLines;
import net.efn.client.util.playerbypass.ChunkLightSnapshot;
import net.efn.client.util.playerbypass.PlayerBypassLightTracker;
import net.efn.client.util.playerbypass.VisibleChunkCache;
import net.efn.client.util.playerbypass2.SimulationSignals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.dimension.DimensionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class PlayerBypassModule extends ClientModule {
   private static final int RENDER_DISTANCE_CHUNKS = 16;
   private static final double MARKER_Y = 64.0;
   private static final float LINE_WIDTH = 2.0F;
   private static final int MARK_FILL_ALPHA = 34;
   private static final int BASE_COLOR = -2516480;
   private static final int SUSPECT_COLOR = -5766;
   private static final int ACTIVITY_COLOR = -11205;
   private static final int MEMORY_SECONDS = 45;
   private static final float FALLOFF = 0.3F;
   private static final float MIN_SOURCE = 0.5F;
   private static final int MAX_FOUND = 256;
   private static final int ALERT_SPACING = 4;
   private static final int PIN_SPACING = 1;
   private static final float FROZEN_NEIGHBOUR = 0.5F;
   private static final int REPORT_SOURCES = 4;
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private final NumberSetting opacity = this.addSetting("opacity", new NumberSetting("Opacity", 140.0F, 10.0F, 255.0F).step(1.0F));
   private final BooleanSetting tracers = this.addSetting("tracers", new BooleanSetting("Tracers", false));
   private final NumberSetting sensitivity = this.addSetting("sens", new NumberSetting("Sensitivity", 5.0F, 1.0F, 10.0F).step(1.0F));
   private final ChunkAreas chunkAreas = new ChunkAreas();
   private final VisibleChunkCache visibleChunks = new VisibleChunkCache(16);
   private final BooleanSetting sound = this.addSetting("sound", new BooleanSetting("Sound", true));
   private final BooleanSetting showCoordinates = this.addSetting("show-coordinates", new BooleanSetting("Show Coordinates", true));
   private final BooleanSetting disconnect = this.addSetting("disconnect-on-spotted", new BooleanSetting("Auto Disconnect", false));
   private final BooleanSetting autoRtp = this.addSetting("auto-rtp", new BooleanSetting("Auto RTP", false));
   private final StringSetting rtpCommand = this.addSetting("rtp-command", new StringSetting("RTP Command", "rtp").hint("Without the slash, e.g. rtp"));
   private final NumberSetting rtpScanTime = this.addSetting("rtp-scan-time", new NumberSetting("Scan Time (s)", 5.0F, 2.0F, 30.0F).step(0.5F));
   private final NumberSetting rtpDelay = this.addSetting("rtp-delay", new NumberSetting("RTP Delay (s)", 6.0F, 3.0F, 120.0F).step(0.5F));
   private final NumberSetting rtpTimeout = this.addSetting("rtp-max-timeout", new NumberSetting("RTP Timeout (s)", 10.0F, 3.0F, 60.0F).step(1.0F));
   private final NumberSetting rtpMax = this.addSetting("rtp-max", new NumberSetting("Max RTPs (0 = unlimited)", 100.0F, 0.0F, 500.0F).step(1.0F));
   private final BooleanSetting rtpStopOnSuspect = this.addSetting("rtp-stop-on-suspect", new BooleanSetting("Stop On Suspect", false));
   private final StringSetting rtpMenuItem = this.addSetting(
      "rtp-menu-item", new StringSetting("Menu Item", "").hint("Clicked if the command opens a menu, e.g. overworld")
   );
   private final PlayerBypassAutoRtp autoRtpRunner = new PlayerBypassAutoRtp(
      this,
      this.autoRtp,
      this.rtpCommand,
      this.rtpScanTime,
      this.rtpDelay,
      this.rtpTimeout,
      this.rtpMax,
      this.rtpStopOnSuspect,
      this.rtpMenuItem,
      this.sound,
      this.showCoordinates
   );
   private final LongOpenHashSet peaks = new LongOpenHashSet();
   private final Map<RegistryKey<World>, LongLinkedOpenHashSet> foundByDimension = new HashMap<>();
   private LongLinkedOpenHashSet found = new LongLinkedOpenHashSet();
   private int session = -1;
   private final LongOpenHashSet shown = new LongOpenHashSet();
   private final Long2FloatOpenHashMap field = new Long2FloatOpenHashMap();
   private final Long2FloatOpenHashMap raw = new Long2FloatOpenHashMap();
   private int ticks;

   public PlayerBypassModule() {
      super(
         "player-bypass",
         "Player Bypass",
         "Finds hidden players from light data and from chunks that keep ticking outside your simulation distance.",
         ModuleCategory.BASE_RAID
      );
      BooleanSetting var2 = this.usesNotifications();
      this.sound.visibleWhen(() -> var2.get() || this.autoRtp.get());
      this.showCoordinates.visibleWhen(() -> var2.get() || this.disconnect.get() || this.autoRtp.get());
      this.autoRtp.sessionOnly();

      for (Setting var6 : new Setting[]{
         this.rtpCommand, this.rtpScanTime, this.rtpDelay, this.rtpTimeout, this.rtpMax, this.rtpStopOnSuspect, this.rtpMenuItem
      }) {
         var6.visibleWhen(this.autoRtp::get);
      }

      this.autoRtp.onChange(() -> {
         if (this.isEnabled()) {
            if (this.autoRtp.get()) {
               this.autoRtpRunner.start();
            } else {
               this.autoRtpRunner.stopByUser();
            }
         }
      });
   }

   @Override
   protected void onEnable() {
      this.visibleChunks.invalidate();
      PlayerBypassLightTracker.setSpottedListener(this::onChunkSpotted);
      if (this.autoRtp.get()) {
         this.autoRtpRunner.start();
      }

      this.foundByDimension.clear();
      this.found = new LongLinkedOpenHashSet();
      this.session = SimulationSignals.session();
      SimulationSignals.clear();
      this.configure();
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
   }

   @Override
   protected void onDisable() {
      this.visibleChunks.invalidate();
      PlayerBypassLightTracker.setSpottedListener(null);
      this.autoRtpRunner.stopByUser();
      this.autoRtpRunner.forgetCooldown();
      this.autoRtp.set(false);
      SimulationSignals.configure(false, 0, 0, true, 30);
      this.peaks.clear();
      this.foundByDimension.clear();
      this.found = new LongLinkedOpenHashSet();
      this.shown.clear();
      this.field.clear();
      this.raw.clear();
   }

   private void onChunkSpotted(int var1, int var2) {
      this.spotted((var1 << 4) + 8, (var2 << 4) + 8, new ItemStack(Items.field_8575));
   }

   private void spotted(int var1, int var2, ItemStack var3) {
      MinecraftClient var5 = MinecraftClient.method_1551();
      boolean var6 = this.showCoordinates.get();
      if (!this.autoRtpRunner.baseSpotted(var1, var2, this.disconnect.get()) && this.notificationsOn()) {
         ChallengeToast.addOrUpdate(var3, "Player Bypass", var6 ? Text.method_43470("at " + var1 + ", " + var2) : Text.method_43473(), this.sound.get());
      }

      ClientPlayNetworkHandler var4;
      if (this.disconnect.get() && (var4 = var5.method_1562()) != null && var4.method_48296().method_10758()) {
         ModuleChat.send(this, ModuleChat.body().text("Auto Disconnect ").warn("triggered"));
         AutoReconnectModule.holdOff();
         var4.method_48296().method_10747(Text.method_43470("[Player Bypass] Player detected" + (var6 ? " at " + var1 + ", " + var2 : "")));
      }
   }

   private void configure() {
      SimulationSignals.configure(true, 0, 0, true, 45);
   }

   private static boolean netherOrEnd(World var0) {
      RegistryKey var1 = var0.method_27983();
      return var1.equals(World.field_25180)
         || var1.equals(World.field_25181)
         || var0.method_40134().method_40225(DimensionTypes.field_37667)
         || var0.method_40134().method_40225(DimensionTypes.field_37668)
         || !var0.method_8597().comp_642();
   }

   private float threshold() {
      return 8.0F * (float)Math.pow(2.0, (5.0 - this.sensitivity.get()) / 2.5);
   }

   private void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1687 != null && var2.field_1724 != null) {
         if (this.session != SimulationSignals.session()) {
            this.session = SimulationSignals.session();
            this.foundByDimension.clear();
            this.peaks.clear();
            this.shown.clear();
         }

         this.found = this.foundByDimension.computeIfAbsent(var2.field_1687.method_27983(), var0 -> new LongLinkedOpenHashSet());
         this.configure();
         SimulationSignals.tick();
         if (netherOrEnd(var2.field_1687)) {
            SimulationSignals.dropPins();
            this.shown.clear();
         } else {
            this.drainPins();
            if (++this.ticks % 5 == 0) {
               this.collect(var2.field_1724);
               this.alert();
               this.refreshShown(var2.field_1724);
            }
         }
      }
   }

   private void collect(ClientPlayerEntity var1) {
      this.peaks.clear();
      this.field.clear();
      this.raw.clear();
      long var2 = SimulationSignals.now();
      int var4 = SimulationSignals.simulation();
      int var5 = var1.method_24515().method_10263() >> 4;
      int var6 = var1.method_24515().method_10260() >> 4;
      int var7 = SimulationSignals.ringStart();
      int var8 = SimulationSignals.view();
      int var9 = 2 * var4 + 1;
      float[] var10 = new float[var9 * var9];
      float var11 = var4 * 1.4143F + 1.0F;

      for (int var12 = -var4; var12 <= var4; var12++) {
         for (int var13 = -var4; var13 <= var4; var13++) {
            var10[(var12 + var4) * var9 + var13 + var4] = 1.0F - 0.3F * (float)Math.sqrt(var12 * var12 + var13 * var13) / var11;
         }
      }

      ObjectIterator var24 = SimulationSignals.chunks().long2ObjectEntrySet().iterator();

      while (var24.hasNext()) {
         Entry var26 = (Entry)var24.next();
         long var16 = var26.getLongKey();
         int var18 = ChunkPos.method_8325(var16);
         int var15;
         int var19 = SimulationSignals.distance(var18, var15 = ChunkPos.method_8332(var16), var5, var6);
         float var14;
         if (var19 >= var7 && var19 <= var8 && !((var14 = ((SimulationSignals.ChunkSignal)var26.getValue()).score(var2)) < 0.5F)) {
            for (int var20 = -var4; var20 <= var4; var20++) {
               for (int var21 = -var4; var21 <= var4; var21++) {
                  long var22 = ChunkPos.method_8331(var18 + var20, var15 + var21);
                  this.field.addTo(var22, var14 * var10[(var20 + var4) * var9 + var21 + var4]);
                  this.raw.addTo(var22, var14);
               }
            }
         }
      }

      LongIterator var25 = SimulationSignals.frozen().iterator();

      while (var25.hasNext()) {
         long var27 = var25.next();
         this.field.remove(var27);
         this.raw.remove(var27);
         int var30 = ChunkPos.method_8325(var27);
         int var32 = ChunkPos.method_8332(var27);

         for (int var17 = -1; var17 <= 1; var17++) {
            for (int var34 = -1; var34 <= 1; var34++) {
               long var35 = ChunkPos.method_8331(var30 + var17, var32 + var34);
               if (this.field.containsKey(var35)) {
                  this.field.put(var35, this.field.get(var35) * 0.5F);
                  this.raw.put(var35, this.raw.get(var35) * 0.5F);
               }
            }
         }
      }

      float var28 = this.threshold();
      ObjectIterator var29 = this.field.long2FloatEntrySet().iterator();

      while (var29.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry var31 = (it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry)var29.next();
         long var33 = var31.getLongKey();
         if (!(this.raw.get(var33) < var28) && this.isPeak(var33, var31.getFloatValue(), var4)) {
            this.peaks.add(var33);
         }
      }
   }

   private boolean isPeak(long var1, float var3, int var4) {
      int var5 = ChunkPos.method_8325(var1);
      int var6 = ChunkPos.method_8332(var1);

      for (int var7 = -var4; var7 <= var4; var7++) {
         for (int var8 = -var4; var8 <= var4; var8++) {
            long var9;
            float var11;
            if ((var7 != 0 || var8 != 0)
               && ((var11 = this.field.get(var9 = ChunkPos.method_8331(var5 + var7, var6 + var8))) > var3 || var11 == var3 && var9 < var1)) {
               return false;
            }
         }
      }

      return true;
   }

   private void drainPins() {
      SimulationSignals.Pin var1;
      while ((var1 = SimulationSignals.pollPin()) != null) {
         BlockPos var2 = var1.pos();
         long var3 = ChunkPos.method_8331(var2.method_10263() >> 4, var2.method_10260() >> 4);
         LOGGER.info("[Player Bypass] {}", var1.detail());
         if (!this.near(var3, 1)) {
            this.remember(var3);
            ModuleChat.send(this, var1.detail());
            this.spotted(var2.method_10263(), var2.method_10260(), GlintHeadRenderer.stack());
         }
      }
   }

   private void alert() {
      long var1 = SimulationSignals.now();
      int var3 = Math.max(4, 2 * SimulationSignals.simulation());
      LongIterator var4 = this.peaks.iterator();

      while (var4.hasNext()) {
         long var5 = var4.next();
         if (!this.near(var5, var3)) {
            this.remember(var5);
            int var7 = (ChunkPos.method_8325(var5) << 4) + 8;
            int var8 = (ChunkPos.method_8332(var5) << 4) + 8;
            this.report(var5, var7, var8, var1);
            this.spotted(var7, var8, GlintHeadRenderer.stack());
         }
      }
   }

   private boolean near(long var1, int var3) {
      LongListIterator var4 = this.found.iterator();

      while (var4.hasNext()) {
         long var5 = var4.next();
         if (SimulationSignals.distance(ChunkPos.method_8325(var5), ChunkPos.method_8332(var5), ChunkPos.method_8325(var1), ChunkPos.method_8332(var1)) <= var3
            )
          {
            return true;
         }
      }

      return false;
   }

   private void remember(long var1) {
      this.found.add(var1);

      while (this.found.size() > 256) {
         this.found.removeFirstLong();
      }
   }

   private void refreshShown(ClientPlayerEntity var1) {
      this.shown.clear();
      int var2 = var1.method_24515().method_10263() >> 4;
      int var3 = var1.method_24515().method_10260() >> 4;
      int var4 = SimulationSignals.view() + SimulationSignals.simulation() + 2;
      LongListIterator var5 = this.found.iterator();

      while (var5.hasNext()) {
         long var6 = var5.next();
         if (SimulationSignals.distance(ChunkPos.method_8325(var6), ChunkPos.method_8332(var6), var2, var3) <= var4) {
            this.shown.add(var6);
         }
      }
   }

   private void report(long var1, int var3, int var4, long var5) {
      int var7 = SimulationSignals.simulation();
      int var8 = ChunkPos.method_8325(var1);
      int var9 = ChunkPos.method_8332(var1);
      ArrayList var10 = new ArrayList();
      ObjectIterator var11 = SimulationSignals.chunks().long2ObjectEntrySet().iterator();

      while (var11.hasNext()) {
         Entry var12 = (Entry)var11.next();
         long var13 = var12.getLongKey();
         if (SimulationSignals.distance(ChunkPos.method_8325(var13), ChunkPos.method_8332(var13), var8, var9) <= var7
            && !(((SimulationSignals.ChunkSignal)var12.getValue()).score(var5) < 0.5F)) {
            var10.add(var12);
         }
      }

      var10.sort(
         (var2, var3x) -> Float.compare(
            ((SimulationSignals.ChunkSignal)var3x.getValue()).score(var5), ((SimulationSignals.ChunkSignal)var2.getValue()).score(var5)
         )
      );
      ArrayList var21 = new ArrayList();
      var21.add(
         "Found "
            + var3
            + ", "
            + var4
            + " from "
            + var10.size()
            + " chunks (sim "
            + var7
            + ", ring "
            + SimulationSignals.ringStart()
            + "-"
            + SimulationSignals.view()
            + ")"
      );

      for (int var22 = 0; var22 < Math.min(4, var10.size()); var22++) {
         SimulationSignals.ChunkSignal var24 = (SimulationSignals.ChunkSignal)((Entry)var10.get(var22)).getValue();
         long var14 = ((Entry)var10.get(var22)).getLongKey();
         StringBuilder var16 = new StringBuilder();

         for (SimulationSignals.Kind var20 : SimulationSignals.Kind.values()) {
            if (var24.count(var20) > 0) {
               var16.append(var16.isEmpty() ? "" : ", ").append(var20.title).append(" x").append(var24.count(var20));
            }
         }

         var21.add(
            " chunk "
               + ((ChunkPos.method_8325(var14) << 4) + 8)
               + ", "
               + ((ChunkPos.method_8332(var14) << 4) + 8)
               + String.format(Locale.ROOT, " score %.1f: ", var24.score(var5))
               + var16
         );

         for (String var27 : var24.recent()) {
            var21.add("   " + var27);
         }
      }

      for (String var25 : var21) {
         LOGGER.info("[Player Bypass] {}", var25);
         ModuleChat.send(this, var25);
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1687 != null && var2.field_1724 != null) {
         LongOpenHashSet var3 = new LongOpenHashSet();
         LongOpenHashSet var4 = new LongOpenHashSet();
         Long2ObjectMap var5 = PlayerBypassLightTracker.snapshots();
         if (!var5.isEmpty()) {
            int var6 = var2.field_1724.method_24515().method_10263() >> 4;
            int var7 = var2.field_1724.method_24515().method_10260() >> 4;
            if (PerformanceModule.optimizationsEnabled()) {
               this.visibleChunks
                  .update(
                     PlayerBypassLightTracker.visibilityRevision(), var6, var7, var5, PlayerBypassLightTracker::isFlagged, PlayerBypassLightTracker::isSuspect
                  );
               var3.addAll(this.visibleChunks.bases());
               var4.addAll(this.visibleChunks.suspects());
            } else {
               this.visibleChunks.invalidate();
               ObjectIterator var8 = var5.long2ObjectEntrySet().iterator();

               while (var8.hasNext()) {
                  Entry var9 = (Entry)var8.next();
                  long var10 = var9.getLongKey();
                  if (Math.abs(ChunkPos.method_8325(var10) - var6) <= 16
                     && Math.abs(ChunkPos.method_8332(var10) - var7) <= 16
                     && PlayerBypassLightTracker.isFlagged(var10, (ChunkLightSnapshot)var9.getValue())) {
                     (PlayerBypassLightTracker.isSuspect(var10) ? var4 : var3).add(var10);
                  }
               }
            }
         }

         LongOpenHashSet var12 = new LongOpenHashSet();
         if (!netherOrEnd(var2.field_1687)) {
            LongIterator var13 = this.shown.iterator();

            while (var13.hasNext()) {
               long var15 = var13.next();
               if (!var3.contains(var15) && !var4.contains(var15)) {
                  var12.add(var15);
               }
            }
         }

         if (!var3.isEmpty() || !var4.isEmpty() || !var12.isEmpty()) {
            FoglessLines var14 = FoglessLines.forFrame(var1.partialTick);
            double var16 = var2.field_1687.method_31607();
            double var17 = var2.field_1687.method_31600() + 1;
            this.mark(var14, var4, -5766, var16, var17);
            this.mark(var14, var12, -11205, var16, var17);
            this.mark(var14, var3, -2516480, var16, var17);
            if (this.tracers.get()) {
               this.chunkAreas.drawTracers(var3, 64.0, ColorHelper.method_61334(-2516480));
               this.chunkAreas.drawTracers(var4, 64.0, ColorHelper.method_61334(-5766));
               this.chunkAreas.drawTracers(var12, 64.0, ColorHelper.method_61334(-11205));
            }
         }
      }
   }

   private void mark(FoglessLines var1, LongOpenHashSet var2, int var3, double var4, double var6) {
      if (!var2.isEmpty()) {
         float var8 = Math.max(10, Math.min(255, this.opacity.getInt())) / 255.0F;
         DrawStyle var9 = DrawStyle.method_75539(ColorHelper.method_61330(Math.round(34.0F * var8), var3));
         int var10 = ColorHelper.method_61330(Math.round(255.0F * var8), var3);
         LongIterator var11 = var2.iterator();

         while (var11.hasNext()) {
            long var12 = var11.next();
            double var14 = ChunkPos.method_8325(var12) << 4;
            double var16 = ChunkPos.method_8332(var12) << 4;
            double var18 = var14 + 16.0;
            double var20 = var16 + 16.0;
            GizmoDrawing.method_75541(new Box(var14, var4, var16, var18, var6, var20), var9).method_75533();
            column(var1, var14, var16, var18, var20, var4, var6, var10);
         }
      }
   }

   private static void column(FoglessLines var0, double var1, double var3, double var5, double var7, double var9, double var11, int var13) {
      double[][] var14 = new double[][]{{var1, var3}, {var5, var3}, {var5, var7}, {var1, var7}};

      for (int var15 = 0; var15 < var14.length; var15++) {
         double[] var16 = var14[var15];
         double[] var17 = var14[(var15 + 1) % var14.length];
         var0.line(new Vec3d(var16[0], var9, var16[1]), new Vec3d(var16[0], var11, var16[1]), var13, 2.0F);
         var0.line(new Vec3d(var16[0], var9, var16[1]), new Vec3d(var17[0], var9, var17[1]), var13, 2.0F);
         var0.line(new Vec3d(var16[0], var11, var16[1]), new Vec3d(var17[0], var11, var17[1]), var13, 2.0F);
      }
   }
}
