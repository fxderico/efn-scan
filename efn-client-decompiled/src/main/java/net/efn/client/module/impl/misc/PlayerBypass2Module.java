package net.efn.client.module.impl.misc;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
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
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.FoglessLines;
import net.efn.client.util.playerbypass2.SimulationSignals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DrawStyle;
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
public final class PlayerBypass2Module extends ClientModule {
   private static final float LINE_WIDTH = 2.0F;
   private static final int MARK_COLOR = -11205;
   private static final int MARK_FILL_ALPHA = 34;
   private static final int MEMORY_SECONDS = 45;
   private static final float FALLOFF = 0.3F;
   private static final float MIN_SOURCE = 0.5F;
   private static final int MAX_FOUND = 256;
   private static final int ALERT_SPACING = 4;
   private static final int PIN_SPACING = 1;
   private static final float FROZEN_NEIGHBOUR = 0.5F;
   private static final int REPORT_SOURCES = 4;
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private final NumberSetting sensitivity = this.addSetting("sens", new NumberSetting("Sensitivity", 1.0F, 1.0F, 10.0F).step(1.0F));
   private final LongOpenHashSet peaks = new LongOpenHashSet();
   private final Map<RegistryKey<World>, LongLinkedOpenHashSet> foundByDimension = new HashMap<>();
   private LongLinkedOpenHashSet found = new LongLinkedOpenHashSet();
   private int session = -1;
   private final LongOpenHashSet shown = new LongOpenHashSet();
   private final Long2FloatOpenHashMap field = new Long2FloatOpenHashMap();
   private final Long2FloatOpenHashMap raw = new Long2FloatOpenHashMap();
   private int ticks;

   public PlayerBypass2Module() {
      super(
         "player-bypass-2", "Player Bypass 2", "Finds hidden players from chunks that keep ticking outside your simulation distance.", ModuleCategory.BASE_RAID
      );
   }

   @Override
   protected void onEnable() {
      this.foundByDimension.clear();
      this.found = new LongLinkedOpenHashSet();
      this.session = SimulationSignals.session();
      SimulationSignals.clear();
      this.configure();
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
   }

   @Override
   protected void onDisable() {
      SimulationSignals.configure(false, 0, 0, true, 30);
      this.peaks.clear();
      this.foundByDimension.clear();
      this.found = new LongLinkedOpenHashSet();
      this.shown.clear();
      this.field.clear();
      this.raw.clear();
   }

   private void configure() {
      SimulationSignals.configure(true, 0, 0, true, 45);
   }

   private static boolean netherOrEnd(World world) {
      RegistryKey var1 = world.getRegistryKey();
      return var1.equals(World.NETHER)
         || var1.equals(World.END)
         || world.getDimensionEntry().matchesKey(DimensionTypes.THE_NETHER)
         || world.getDimensionEntry().matchesKey(DimensionTypes.THE_END)
         || !world.getDimension().hasSkyLight();
   }

   private float threshold() {
      return 8.0F * (float)Math.pow(2.0, (5.0 - this.sensitivity.get()) / 2.5);
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world != null && var2.player != null) {
         if (this.session != SimulationSignals.session()) {
            this.session = SimulationSignals.session();
            this.foundByDimension.clear();
            this.peaks.clear();
            this.shown.clear();
         }

         this.found = this.foundByDimension.computeIfAbsent(var2.world.getRegistryKey(), registryKey -> new LongLinkedOpenHashSet());
         this.configure();
         SimulationSignals.tick();
         if (netherOrEnd(var2.world)) {
            SimulationSignals.dropPins();
            this.shown.clear();
         } else {
            this.drainPins();
            if (++this.ticks % 5 == 0) {
               this.collect(var2.player);
               this.alert();
               this.refreshShown(var2.player);
            }
         }
      }
   }

   private void collect(ClientPlayerEntity clientPlayerEntity) {
      this.peaks.clear();
      this.field.clear();
      this.raw.clear();
      long var2 = SimulationSignals.now();
      int var4 = SimulationSignals.simulation();
      int var5 = clientPlayerEntity.getBlockPos().getX() >> 4;
      int var6 = clientPlayerEntity.getBlockPos().getZ() >> 4;
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

      for (Entry var26 : SimulationSignals.chunks().long2ObjectEntrySet()) {
         long var16 = var26.getLongKey();
         int var18 = ChunkPos.getPackedX(var16);
         int var15;
         int var19 = SimulationSignals.distance(var18, var15 = ChunkPos.getPackedZ(var16), var5, var6);
         float var14;
         if (var19 >= var7 && var19 <= var8 && !((var14 = ((SimulationSignals.ChunkSignal)var26.getValue()).score(var2)) < 0.5F)) {
            for (int var20 = -var4; var20 <= var4; var20++) {
               for (int var21 = -var4; var21 <= var4; var21++) {
                  long var22 = ChunkPos.toLong(var18 + var20, var15 + var21);
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
         int var30 = ChunkPos.getPackedX(var27);
         int var32 = ChunkPos.getPackedZ(var27);

         for (int var17 = -1; var17 <= 1; var17++) {
            for (int var34 = -1; var34 <= 1; var34++) {
               long var35 = ChunkPos.toLong(var30 + var17, var32 + var34);
               if (this.field.containsKey(var35)) {
                  this.field.put(var35, this.field.get(var35) * 0.5F);
                  this.raw.put(var35, this.raw.get(var35) * 0.5F);
               }
            }
         }
      }

      float var28 = this.threshold();

      for (it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry var31 : this.field.long2FloatEntrySet()) {
         long var33 = var31.getLongKey();
         if (!(this.raw.get(var33) < var28) && this.isPeak(var33, var31.getFloatValue(), var4)) {
            this.peaks.add(var33);
         }
      }
   }

   private boolean isPeak(long l, float f, int i) {
      int var5 = ChunkPos.getPackedX(l);
      int var6 = ChunkPos.getPackedZ(l);

      for (int var7 = -i; var7 <= i; var7++) {
         for (int var8 = -i; var8 <= i; var8++) {
            long var9;
            float var11;
            if ((var7 != 0 || var8 != 0) && ((var11 = this.field.get(var9 = ChunkPos.toLong(var5 + var7, var6 + var8))) > f || var11 == f && var9 < l)) {
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
         long var3 = ChunkPos.toLong(var2.getX() >> 4, var2.getZ() >> 4);
         LOGGER.info("[Player Bypass 2] {}", var1.detail());
         boolean var5 = false;
         LongListIterator var6 = this.found.iterator();

         while (var6.hasNext()) {
            long var7 = var6.next();
            if (SimulationSignals.distance(ChunkPos.getPackedX(var7), ChunkPos.getPackedZ(var7), ChunkPos.getPackedX(var3), ChunkPos.getPackedZ(var3)) <= 1) {
               var5 = true;
               break;
            }
         }

         if (!var5) {
            this.found.add(var3);

            while (this.found.size() > 256) {
               this.found.removeFirstLong();
            }

            ModuleChat.send(this, var1.detail());
            ChallengeToast.addOrUpdate(
               GlintHeadRenderer.stack(), "Player Activity Detected", Text.literal("at " + var2.getX() + ", " + var2.getY() + ", " + var2.getZ()), true
            );
         }
      }
   }

   private void alert() {
      long var1 = SimulationSignals.now();
      int var3 = Math.max(4, 2 * SimulationSignals.simulation());
      LongIterator var4 = this.peaks.iterator();

      while (var4.hasNext()) {
         long var5 = var4.next();
         boolean var7 = false;
         LongListIterator var8 = this.found.iterator();

         while (var8.hasNext()) {
            long var9 = var8.next();
            if (SimulationSignals.distance(ChunkPos.getPackedX(var9), ChunkPos.getPackedZ(var9), ChunkPos.getPackedX(var5), ChunkPos.getPackedZ(var5)) <= var3) {
               var7 = true;
               break;
            }
         }

         if (!var7) {
            this.found.add(var5);

            while (this.found.size() > 256) {
               this.found.removeFirstLong();
            }

            int var11 = (ChunkPos.getPackedX(var5) << 4) + 8;
            int var10 = (ChunkPos.getPackedZ(var5) << 4) + 8;
            this.report(var5, var11, var10, var1);
            ChallengeToast.addOrUpdate(GlintHeadRenderer.stack(), "Player Activity Detected", Text.literal("at " + var11 + ", " + var10), true);
         }
      }
   }

   private void refreshShown(ClientPlayerEntity clientPlayerEntity) {
      this.shown.clear();
      int var2 = clientPlayerEntity.getBlockPos().getX() >> 4;
      int var3 = clientPlayerEntity.getBlockPos().getZ() >> 4;
      int var4 = SimulationSignals.view() + SimulationSignals.simulation() + 2;
      LongListIterator var5 = this.found.iterator();

      while (var5.hasNext()) {
         long var6 = var5.next();
         if (SimulationSignals.distance(ChunkPos.getPackedX(var6), ChunkPos.getPackedZ(var6), var2, var3) <= var4) {
            this.shown.add(var6);
         }
      }
   }

   private void report(long l, int i, int j, long m) {
      int var7 = SimulationSignals.simulation();
      int var8 = ChunkPos.getPackedX(l);
      int var9 = ChunkPos.getPackedZ(l);
      ArrayList var10 = new ArrayList();

      for (Entry var12 : SimulationSignals.chunks().long2ObjectEntrySet()) {
         long var13 = var12.getLongKey();
         if (SimulationSignals.distance(ChunkPos.getPackedX(var13), ChunkPos.getPackedZ(var13), var8, var9) <= var7
            && !(((SimulationSignals.ChunkSignal)var12.getValue()).score(m) < 0.5F)) {
            var10.add(var12);
         }
      }

      var10.sort(
         (entry, entry2) -> Float.compare(
            ((SimulationSignals.ChunkSignal)entry2.getValue()).score(m), ((SimulationSignals.ChunkSignal)entry.getValue()).score(m)
         )
      );
      ArrayList var21 = new ArrayList();
      var21.add(
         "Found "
            + i
            + ", "
            + j
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
               + ((ChunkPos.getPackedX(var14) << 4) + 8)
               + ", "
               + ((ChunkPos.getPackedZ(var14) << 4) + 8)
               + String.format(Locale.ROOT, " score %.1f: ", var24.score(m))
               + var16
         );

         for (String var27 : var24.recent()) {
            var21.add("   " + var27);
         }
      }

      for (String var25 : var21) {
         LOGGER.info("[Player Bypass 2] {}", var25);
         ModuleChat.send(this, var25);
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world != null && var2.player != null && !netherOrEnd(var2.world)) {
         if (!this.shown.isEmpty()) {
            FoglessLines var3 = FoglessLines.forFrame(render3DEvent.partialTick);
            double var4 = var2.world.getBottomY();
            double var6 = var2.world.getTopYInclusive() + 1;
            DrawStyle var8 = DrawStyle.filled(ColorHelper.withAlpha(34, -11205));
            LongIterator var9 = this.shown.iterator();

            while (var9.hasNext()) {
               long var10 = var9.next();
               double var12 = ChunkPos.getPackedX(var10) << 4;
               double var14 = ChunkPos.getPackedZ(var10) << 4;
               double var16 = var12 + 16.0;
               double var18 = var14 + 16.0;
               GizmoDrawing.box(new Box(var12, var4, var14, var16, var6, var18), var8).ignoreOcclusion();
               column(var3, var12, var14, var16, var18, var4, var6);
            }
         }
      }
   }

   private static void column(FoglessLines foglessLines, double d, double e, double f, double g, double h, double i) {
      double[][] var13 = new double[][]{{d, e}, {f, e}, {f, g}, {d, g}};

      for (int var14 = 0; var14 < var13.length; var14++) {
         double[] var15 = var13[var14];
         double[] var16 = var13[(var14 + 1) % var13.length];
         foglessLines.line(new Vec3d(var15[0], h, var15[1]), new Vec3d(var15[0], i, var15[1]), -11205, 2.0F);
         foglessLines.line(new Vec3d(var15[0], h, var15[1]), new Vec3d(var16[0], h, var16[1]), -11205, 2.0F);
         foglessLines.line(new Vec3d(var15[0], i, var15[1]), new Vec3d(var16[0], i, var16[1]), -11205, 2.0F);
      }
   }
}
