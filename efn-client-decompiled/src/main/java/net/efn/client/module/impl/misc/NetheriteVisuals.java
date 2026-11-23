package net.efn.client.module.impl.misc;

import imgui.ImDrawList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.efn.client.gui.WorldLabel;
import net.efn.client.util.FoglessLines;
import net.efn.client.util.TracerEngine;
import net.efn.client.util.glyph.GlyphCanvas;
import net.efn.client.util.glyph.GlyphShapes;
import net.efn.client.util.glyph.GlyphSwarm;
import net.efn.client.util.glyph.GlyphTint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
final class NetheriteVisuals {
   static final String DOTS = "Dots";
   static final String CUBES = "Cubes";
   static final String PULSES = "Pulses";
   private static final long PULSE_NANOS = 1400000000L;
   private static final float WAVE_SECONDS = 0.9F;
   private static final int MAX_WAVES = 12;
   private static final int GLOW_LIMIT = 32;
   private static final int DEBRIS_TRACERS = 3;
   private static final int LABEL_LIMIT = 8;
   private static final double PROBE_RADIUS = 12.0;
   private static final int DOT_LIMIT = 1800;
   private static final int CUBE_LIMIT = 450;
   private static final double MISSING_RADIUS = 32.0;
   private static final int MISSING_LIMIT = 1600;
   private static final float PROBE_FRESH_TICKS = 14.0F;
   private static final float[] SHELL_INFLATE = new float[]{0.06F, 0.16F, 0.3F};
   private static final float[] SHELL_ALPHA = new float[]{0.16F, 0.08F, 0.04F};
   private final GlyphCanvas canvas = new GlyphCanvas();
   private final GlyphSwarm swarm = new GlyphSwarm(128);
   private final Random random = Random.method_43047();
   private final List<NetheriteVisuals.Wave> waves = new ArrayList<>();
   private final List<NetheriteVisuals.Label> labels = new ArrayList<>();
   private long lastFrame;
   private double pixel;

   NetheriteVisuals() {
   }

   void clear() {
      this.swarm.clear();
      this.waves.clear();
      this.labels.clear();
      this.lastFrame = 0L;
   }

   void burst(BlockPos var1, int var2) {
      double var4 = var1.method_10263() + 0.5;
      double var6 = var1.method_10264() + 0.5;
      double var8 = var1.method_10260() + 0.5;
      if (this.waves.size() >= 12) {
         this.waves.removeFirst();
      }

      this.waves.add(new NetheriteVisuals.Wave(var4, var6, var8, System.nanoTime()));
      int var10 = GlyphTint.lift(var2, 0.55F);

      for (int var3 = 0; var3 < 2; var3++) {
         this.swarm
            .spawn(
               (byte)0,
               var4 + this.jitter(0.3F),
               var6 + 0.6,
               var8 + this.jitter(0.3F),
               this.jitter(0.2F),
               1.1F,
               this.jitter(0.2F),
               1.8F,
               0.22F,
               this.jitter(0.3F),
               this.jitter(0.5F),
               this.random.method_43057(),
               var10,
               128
            );
      }

      for (int var14 = 0; var14 < 10; var14++) {
         float var11 = this.random.method_43057() * 6.2831855F;
         float var12 = 2.2F + this.random.method_43057() * 1.6F;
         int var13 = GlyphTint.mix(var2, -1, this.random.method_43057() * 0.5F);
         this.swarm
            .spawn(
               (byte)3,
               var4,
               var6,
               var8,
               MathHelper.method_15362(var11) * var12,
               1.2F + this.random.method_43057() * 1.5F,
               MathHelper.method_15374(var11) * var12,
               0.8F,
               0.09F,
               var11,
               2.8F,
               this.random.method_43057(),
               var13,
               128
            );
      }

      MinecraftClient.method_1551().method_1483().method_4873(PositionedSoundInstance.method_4757(SoundEvents.field_26980, 1.3F, 0.9F));
   }

   void render(
      MinecraftClient var1,
      float var2,
      Vec3d var3,
      List<NetheriteVisuals.Section> var4,
      List<BlockPos> var5,
      AdvancedFinder var6,
      NetheriteVisuals.Options var7
   ) {
      long var8 = System.nanoTime();
      float var10 = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(var8 - this.lastFrame) / 1.0E9F);
      this.lastFrame = var8;
      this.swarm.step(var10);
      this.canvas.begin(var1.field_1773.method_19418());
      this.pixel = FoglessLines.pixelScale(var2);
      int var11 = GlyphTint.alpha(var7.near(), 1.0F);
      int var12 = GlyphTint.lift(var11, 0.55F);
      float var13 = 0.5F + 0.5F * MathHelper.method_15374((float)(var8 % 1400000000L) / 1.4E9F * 6.2831855F);
      this.labels.clear();
      this.drawRevealed(var3, var5, var7, var11, var12, var13);
      this.drawWaves(var8, var11, var12);
      if (var7.showProbes()) {
         this.drawProbes(var3, var6, var7, var11, var12, var13);
      }

      this.drawSections(var4, var7, var12, var13);
      this.drawTracers(var4, var7);
      this.swarm.draw(this.canvas, 1.0F, true);
      this.canvas.submit(true);
   }

   void overlay(ImDrawList var1, float var2, float var3) {
      MinecraftClient var4 = MinecraftClient.method_1551();

      for (NetheriteVisuals.Label var6 : this.labels) {
         float[] var7 = WorldLabel.project(var4, var6.pos(), var2, var3);
         if (var7 != null) {
            WorldLabel.drawText(var1, var7[0], var7[1], var6.primary(), var6.secondary(), 1.0F);
         }
      }
   }

   boolean hasLabels() {
      return !this.labels.isEmpty();
   }

   private static int tint(NetheriteVisuals.Options var0, double var1) {
      float var3 = (float)MathHelper.method_15350(var1 / var0.fade(), 0.0, 1.0);
      return GlyphTint.alpha(GlyphTint.mix(var0.near(), var0.far(), var3), 1.0F);
   }

   private void drawSections(List<NetheriteVisuals.Section> var1, NetheriteVisuals.Options var2, int var3, float var4) {
      for (NetheriteVisuals.Section var6 : var1) {
         if (var6.progress() >= 0.0F) {
            this.drawScanning(var6, var3, var4);
         }
      }

      for (NetheriteVisuals.Section var8 : var1) {
         if (var8.progress() < 0.0F) {
            this.drawCandidate(var8, var2);
         }
      }
   }

   private void drawCandidate(NetheriteVisuals.Section var1, NetheriteVisuals.Options var2) {
      double var3 = MathHelper.method_15350(var1.distance() / var2.fade(), 0.0, 1.0);
      int var5 = tint(var2, var1.distance());
      this.box(var1.box(), GlyphTint.alpha(var5, (float)(0.95 - 0.3 * var3)), (float)(2.4 - 0.8 * var3));
      this.faces(var1.box(), GlyphTint.alpha(var5, (float)(0.07 - 0.03 * var3)));
   }

   private void drawScanning(NetheriteVisuals.Section var1, int var2, float var3) {
      Box var4 = var1.box();
      this.box(var4, GlyphTint.alpha(var2, 0.6F + 0.4F * var3), 3.2F);
      this.faces(var4, GlyphTint.alpha(var2, 0.05F));
      double var5 = var4.field_1322 + (var4.field_1325 - var4.field_1322) * var1.progress();
      int var7 = GlyphTint.alpha(var2, 0.2F);
      this.canvas
         .quad(
            var4.field_1323,
            var5,
            var4.field_1321,
            var4.field_1320,
            var5,
            var4.field_1321,
            var4.field_1320,
            var5,
            var4.field_1324,
            var4.field_1323,
            var5,
            var4.field_1324,
            var7
         );
      this.canvas
         .quad(
            var4.field_1323,
            var5,
            var4.field_1321,
            var4.field_1323,
            var5,
            var4.field_1324,
            var4.field_1320,
            var5,
            var4.field_1324,
            var4.field_1320,
            var5,
            var4.field_1321,
            var7
         );
      int var8 = GlyphTint.alpha(var2, 0.95F);
      this.line(var4.field_1323, var5, var4.field_1321, var4.field_1320, var5, var4.field_1321, var8, 2.4F);
      this.line(var4.field_1320, var5, var4.field_1321, var4.field_1320, var5, var4.field_1324, var8, 2.4F);
      this.line(var4.field_1320, var5, var4.field_1324, var4.field_1323, var5, var4.field_1324, var8, 2.4F);
      this.line(var4.field_1323, var5, var4.field_1324, var4.field_1323, var5, var4.field_1321, var8, 2.4F);
   }

   private void drawTracers(List<NetheriteVisuals.Section> var1, NetheriteVisuals.Options var2) {
      int var3 = 0;

      for (NetheriteVisuals.Section var5 : var1) {
         if (var3 >= var2.tracerCount()) {
            break;
         }

         if (!(var5.distance() < 1.0)) {
            Vec3d var6 = var5.box().method_1005();
            TracerEngine.draw(var6, GlyphTint.alpha(tint(var2, var5.distance()), 0.85F));
            String var7 = var5.progress() >= 0.0F ? Math.round(var5.progress() * 100.0F) + "%" : null;
            this.labels.add(new NetheriteVisuals.Label(var6, String.format(Locale.ROOT, "%.0fm", var5.distance()), var7));
            var3++;
         }
      }
   }

   private void drawProbes(Vec3d var1, AdvancedFinder var2, NetheriteVisuals.Options var3, int var4, int var5, float var6) {
      int var7 = GlyphTint.mix(var5, -1, 0.5F);
      var2.forEachMissing(
         var1,
         32.0,
         1600,
         var3.missingFrom(),
         (var3x, var4x, var5x) -> this.canvas
            .sprite(GlyphShapes.DIAMOND, var3x + 0.5, var4x + 0.5, var5x + 0.5, 0.11F + 0.05F * var6, 0.0F, GlyphTint.alpha(var7, 0.5F + 0.45F * var6))
      );
      int var8 = GlyphTint.mix(var5, -1, 0.6F);
      String var9 = var3.probeStyle();
      int var10 = "Cubes".equals(var9) ? 450 : 1800;
      var2.forEachProbe(
         var1,
         12.0,
         var10,
         var3.missingFrom(),
         (var4x, var5x, var6x, var7x) -> {
            float var8x = Math.min(1.0F, var7x / 14.0F);
            int var9x = GlyphTint.mix(var8, var4, var8x);
            double var10x = var4x + 0.5;
            double var12 = var5x + 0.5;
            double var14 = var6x + 0.5;
            if ("Cubes".equals(var9)) {
               double var16 = 0.2 - 0.1 * var8x;
               this.faces(
                  new Box(var10x - var16, var12 - var16, var14 - var16, var10x + var16, var12 + var16, var14 + var16),
                  GlyphTint.alpha(var9x, 0.65F - 0.3F * var8x)
               );
            } else {
               this.canvas.sprite(GlyphShapes.DIAMOND, var10x, var12, var14, 0.13F - 0.06F * var8x, 0.0F, GlyphTint.alpha(var9x, 0.95F - 0.5F * var8x));
               if ("Pulses".equals(var9) && var8x < 1.0F) {
                  this.canvas
                     .sprite(
                        GlyphShapes.HALO, var10x, var12, var14, 0.15F + 0.55F * var8x, 0.0F, GlyphTint.alpha(var8, 0.55F * (1.0F - var8x) * (1.0F - var8x))
                     );
               }
            }
         }
      );
   }

   private void drawRevealed(Vec3d var1, List<BlockPos> var2, NetheriteVisuals.Options var3, int var4, int var5, float var6) {
      ArrayList var10 = new ArrayList(var2);
      var10.sort(Comparator.comparingDouble(var1x -> var1x.method_19770(var1)));

      BlockPos var7;
      double var8;
      for (int var11 = 0; var11 < var10.size() && !((var8 = Math.sqrt((var7 = (BlockPos)var10.get(var11)).method_19770(var1))) > var3.range()); var11++) {
         Box var12 = new Box(var7);
         if (var11 < 32) {
            for (int var13 = 0; var13 < SHELL_INFLATE.length; var13++) {
               this.faces(var12.method_1014(SHELL_INFLATE[var13]), GlyphTint.alpha(var4, SHELL_ALPHA[var13] * (0.7F + 0.3F * var6)));
            }
         }

         this.faces(var12, GlyphTint.alpha(var4, 0.3F));
         this.box(var12, GlyphTint.alpha(var5, 0.95F), 2.6F);
         Vec3d var14 = var12.method_1005();
         if (var11 < 3) {
            TracerEngine.draw(var14, GlyphTint.alpha(var4, 0.8F));
         }

         if (var11 < 8) {
            this.labels.add(new NetheriteVisuals.Label(var14.method_1031(0.0, 0.9, 0.0), String.format(Locale.ROOT, "%.0fm", var8), null));
         }
      }
   }

   private void drawWaves(long var1, int var3, int var4) {
      Iterator var5 = this.waves.iterator();

      while (var5.hasNext()) {
         NetheriteVisuals.Wave var6 = (NetheriteVisuals.Wave)var5.next();
         float var7 = (float)(var1 - var6.born()) / 1.0E9F / 0.9F;
         if (var7 >= 1.0F) {
            var5.remove();
         } else {
            float var8 = 1.0F - (1.0F - var7) * (1.0F - var7) * (1.0F - var7);
            float var9 = 0.4F + 2.6F * var8;
            float var10 = (1.0F - var7) * (1.0F - var7);
            this.canvas.ring(var6.x(), var6.y(), var6.z(), var9 * 0.86F, var9, 48, GlyphTint.alpha(var4, var10 * 0.9F));
            this.canvas.ring(var6.x(), var6.y(), var6.z(), var9, var9 * 1.25F, 48, GlyphTint.alpha(var3, var10 * 0.3F));
         }
      }
   }

   private void line(double var1, double var3, double var5, double var7, double var9, double var11, int var13, float var14) {
      this.canvas.ribbon(var1, var3, var5, var7, var9, var11, var13, var14, this.pixel);
   }

   private void box(Box var1, int var2, float var3) {
      this.line(var1.field_1323, var1.field_1322, var1.field_1321, var1.field_1320, var1.field_1322, var1.field_1321, var2, var3);
      this.line(var1.field_1320, var1.field_1322, var1.field_1321, var1.field_1320, var1.field_1322, var1.field_1324, var2, var3);
      this.line(var1.field_1320, var1.field_1322, var1.field_1324, var1.field_1323, var1.field_1322, var1.field_1324, var2, var3);
      this.line(var1.field_1323, var1.field_1322, var1.field_1324, var1.field_1323, var1.field_1322, var1.field_1321, var2, var3);
      this.line(var1.field_1323, var1.field_1325, var1.field_1321, var1.field_1320, var1.field_1325, var1.field_1321, var2, var3);
      this.line(var1.field_1320, var1.field_1325, var1.field_1321, var1.field_1320, var1.field_1325, var1.field_1324, var2, var3);
      this.line(var1.field_1320, var1.field_1325, var1.field_1324, var1.field_1323, var1.field_1325, var1.field_1324, var2, var3);
      this.line(var1.field_1323, var1.field_1325, var1.field_1324, var1.field_1323, var1.field_1325, var1.field_1321, var2, var3);
      this.line(var1.field_1323, var1.field_1322, var1.field_1321, var1.field_1323, var1.field_1325, var1.field_1321, var2, var3);
      this.line(var1.field_1320, var1.field_1322, var1.field_1321, var1.field_1320, var1.field_1325, var1.field_1321, var2, var3);
      this.line(var1.field_1320, var1.field_1322, var1.field_1324, var1.field_1320, var1.field_1325, var1.field_1324, var2, var3);
      this.line(var1.field_1323, var1.field_1322, var1.field_1324, var1.field_1323, var1.field_1325, var1.field_1324, var2, var3);
   }

   private void faces(Box var1, int var2) {
      this.face(
         var1.field_1323,
         var1.field_1322,
         var1.field_1321,
         var1.field_1320,
         var1.field_1322,
         var1.field_1321,
         var1.field_1320,
         var1.field_1322,
         var1.field_1324,
         var1.field_1323,
         var1.field_1322,
         var1.field_1324,
         var2
      );
      this.face(
         var1.field_1323,
         var1.field_1325,
         var1.field_1321,
         var1.field_1320,
         var1.field_1325,
         var1.field_1321,
         var1.field_1320,
         var1.field_1325,
         var1.field_1324,
         var1.field_1323,
         var1.field_1325,
         var1.field_1324,
         var2
      );
      this.face(
         var1.field_1323,
         var1.field_1322,
         var1.field_1321,
         var1.field_1320,
         var1.field_1322,
         var1.field_1321,
         var1.field_1320,
         var1.field_1325,
         var1.field_1321,
         var1.field_1323,
         var1.field_1325,
         var1.field_1321,
         var2
      );
      this.face(
         var1.field_1323,
         var1.field_1322,
         var1.field_1324,
         var1.field_1320,
         var1.field_1322,
         var1.field_1324,
         var1.field_1320,
         var1.field_1325,
         var1.field_1324,
         var1.field_1323,
         var1.field_1325,
         var1.field_1324,
         var2
      );
      this.face(
         var1.field_1323,
         var1.field_1322,
         var1.field_1321,
         var1.field_1323,
         var1.field_1322,
         var1.field_1324,
         var1.field_1323,
         var1.field_1325,
         var1.field_1324,
         var1.field_1323,
         var1.field_1325,
         var1.field_1321,
         var2
      );
      this.face(
         var1.field_1320,
         var1.field_1322,
         var1.field_1321,
         var1.field_1320,
         var1.field_1322,
         var1.field_1324,
         var1.field_1320,
         var1.field_1325,
         var1.field_1324,
         var1.field_1320,
         var1.field_1325,
         var1.field_1321,
         var2
      );
   }

   private void face(
      double var1,
      double var3,
      double var5,
      double var7,
      double var9,
      double var11,
      double var13,
      double var15,
      double var17,
      double var19,
      double var21,
      double var23,
      int var25
   ) {
      this.canvas.quad(var1, var3, var5, var7, var9, var11, var13, var15, var17, var19, var21, var23, var25);
      this.canvas.quad(var1, var3, var5, var19, var21, var23, var13, var15, var17, var7, var9, var11, var25);
   }

   private float jitter(float var1) {
      return (this.random.method_43057() * 2.0F - 1.0F) * var1;
   }

   @Environment(EnvType.CLIENT)
   private record Label(Vec3d pos, String primary, String secondary) {
   }

   @Environment(EnvType.CLIENT)
   record Options(int near, int far, double fade, double range, String probeStyle, float missingFrom, int tracerCount, boolean showProbes) {
   }

   @Environment(EnvType.CLIENT)
   record Section(Box box, double distance, float progress) {
   }

   @Environment(EnvType.CLIENT)
   private record Wave(double x, double y, double z, long born) {
   }
}
