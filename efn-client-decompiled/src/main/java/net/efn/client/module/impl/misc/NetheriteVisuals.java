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
   private final Random random = Random.create();
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

   void burst(BlockPos blockPos, int i) {
      double var4 = blockPos.getX() + 0.5;
      double var6 = blockPos.getY() + 0.5;
      double var8 = blockPos.getZ() + 0.5;
      if (this.waves.size() >= 12) {
         this.waves.removeFirst();
      }

      this.waves.add(new NetheriteVisuals.Wave(var4, var6, var8, System.nanoTime()));
      int var10 = GlyphTint.lift(i, 0.55F);

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
               this.random.nextFloat(),
               var10,
               128
            );
      }

      for (int var14 = 0; var14 < 10; var14++) {
         float var11 = this.random.nextFloat() * 6.2831855F;
         float var12 = 2.2F + this.random.nextFloat() * 1.6F;
         int var13 = GlyphTint.mix(i, -1, this.random.nextFloat() * 0.5F);
         this.swarm
            .spawn(
               (byte)3,
               var4,
               var6,
               var8,
               MathHelper.cos(var11) * var12,
               1.2F + this.random.nextFloat() * 1.5F,
               MathHelper.sin(var11) * var12,
               0.8F,
               0.09F,
               var11,
               2.8F,
               this.random.nextFloat(),
               var13,
               128
            );
      }

      MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.3F, 0.9F));
   }

   void render(
      MinecraftClient minecraftClient,
      float f,
      Vec3d vec3d,
      List<NetheriteVisuals.Section> list,
      List<BlockPos> list2,
      AdvancedFinder advancedFinder,
      NetheriteVisuals.Options options
   ) {
      long var8 = System.nanoTime();
      float var10 = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(var8 - this.lastFrame) / 1.0E9F);
      this.lastFrame = var8;
      this.swarm.step(var10);
      this.canvas.begin(minecraftClient.gameRenderer.getCamera());
      this.pixel = FoglessLines.pixelScale(f);
      int var11 = GlyphTint.alpha(options.near(), 1.0F);
      int var12 = GlyphTint.lift(var11, 0.55F);
      float var13 = 0.5F + 0.5F * MathHelper.sin((float)(var8 % 1400000000L) / 1.4E9F * 6.2831855F);
      this.labels.clear();
      this.drawRevealed(vec3d, list2, options, var11, var12, var13);
      this.drawWaves(var8, var11, var12);
      if (options.showProbes()) {
         this.drawProbes(vec3d, advancedFinder, options, var11, var12, var13);
      }

      this.drawSections(list, options, var12, var13);
      this.drawTracers(list, options);
      this.swarm.draw(this.canvas, 1.0F, true);
      this.canvas.submit(true);
   }

   void overlay(ImDrawList imDrawList, float f, float g) {
      MinecraftClient var4 = MinecraftClient.getInstance();

      for (NetheriteVisuals.Label var6 : this.labels) {
         float[] var7 = WorldLabel.project(var4, var6.pos(), f, g);
         if (var7 != null) {
            WorldLabel.drawText(imDrawList, var7[0], var7[1], var6.primary(), var6.secondary(), 1.0F);
         }
      }
   }

   boolean hasLabels() {
      return !this.labels.isEmpty();
   }

   private static int tint(NetheriteVisuals.Options options, double d) {
      float var3 = (float)MathHelper.clamp(d / options.fade(), 0.0, 1.0);
      return GlyphTint.alpha(GlyphTint.mix(options.near(), options.far(), var3), 1.0F);
   }

   private void drawSections(List<NetheriteVisuals.Section> list, NetheriteVisuals.Options options, int i, float f) {
      for (NetheriteVisuals.Section var6 : list) {
         if (var6.progress() >= 0.0F) {
            this.drawScanning(var6, i, f);
         }
      }

      for (NetheriteVisuals.Section var8 : list) {
         if (var8.progress() < 0.0F) {
            this.drawCandidate(var8, options);
         }
      }
   }

   private void drawCandidate(NetheriteVisuals.Section section, NetheriteVisuals.Options options) {
      double var3 = MathHelper.clamp(section.distance() / options.fade(), 0.0, 1.0);
      int var5 = tint(options, section.distance());
      this.box(section.box(), GlyphTint.alpha(var5, (float)(0.95 - 0.3 * var3)), (float)(2.4 - 0.8 * var3));
      this.faces(section.box(), GlyphTint.alpha(var5, (float)(0.07 - 0.03 * var3)));
   }

   private void drawScanning(NetheriteVisuals.Section section, int i, float f) {
      Box var4 = section.box();
      this.box(var4, GlyphTint.alpha(i, 0.6F + 0.4F * f), 3.2F);
      this.faces(var4, GlyphTint.alpha(i, 0.05F));
      double var5 = var4.minY + (var4.maxY - var4.minY) * section.progress();
      int var7 = GlyphTint.alpha(i, 0.2F);
      this.canvas.quad(var4.minX, var5, var4.minZ, var4.maxX, var5, var4.minZ, var4.maxX, var5, var4.maxZ, var4.minX, var5, var4.maxZ, var7);
      this.canvas.quad(var4.minX, var5, var4.minZ, var4.minX, var5, var4.maxZ, var4.maxX, var5, var4.maxZ, var4.maxX, var5, var4.minZ, var7);
      int var8 = GlyphTint.alpha(i, 0.95F);
      this.line(var4.minX, var5, var4.minZ, var4.maxX, var5, var4.minZ, var8, 2.4F);
      this.line(var4.maxX, var5, var4.minZ, var4.maxX, var5, var4.maxZ, var8, 2.4F);
      this.line(var4.maxX, var5, var4.maxZ, var4.minX, var5, var4.maxZ, var8, 2.4F);
      this.line(var4.minX, var5, var4.maxZ, var4.minX, var5, var4.minZ, var8, 2.4F);
   }

   private void drawTracers(List<NetheriteVisuals.Section> list, NetheriteVisuals.Options options) {
      int var3 = 0;

      for (NetheriteVisuals.Section var5 : list) {
         if (var3 >= options.tracerCount()) {
            break;
         }

         if (!(var5.distance() < 1.0)) {
            Vec3d var6 = var5.box().getCenter();
            TracerEngine.draw(var6, GlyphTint.alpha(tint(options, var5.distance()), 0.85F));
            String var7 = var5.progress() >= 0.0F ? Math.round(var5.progress() * 100.0F) + "%" : null;
            this.labels.add(new NetheriteVisuals.Label(var6, String.format(Locale.ROOT, "%.0fm", var5.distance()), var7));
            var3++;
         }
      }
   }

   private void drawProbes(Vec3d vec3d, AdvancedFinder advancedFinder, NetheriteVisuals.Options options, int i, int j, float f) {
      int var7 = GlyphTint.mix(j, -1, 0.5F);
      advancedFinder.forEachMissing(
         vec3d,
         32.0,
         1600,
         options.missingFrom(),
         (jx, k, l) -> this.canvas.sprite(GlyphShapes.DIAMOND, jx + 0.5, k + 0.5, l + 0.5, 0.11F + 0.05F * f, 0.0F, GlyphTint.alpha(var7, 0.5F + 0.45F * f))
      );
      int var8 = GlyphTint.mix(j, -1, 0.6F);
      String var9 = options.probeStyle();
      int var10 = "Cubes".equals(var9) ? 450 : 1800;
      advancedFinder.forEachProbe(
         vec3d,
         12.0,
         var10,
         options.missingFrom(),
         (k, l, m, n) -> {
            float var8x = Math.min(1.0F, n / 14.0F);
            int var9x = GlyphTint.mix(var8, i, var8x);
            double var10x = k + 0.5;
            double var12 = l + 0.5;
            double var14 = m + 0.5;
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

   private void drawRevealed(Vec3d vec3d, List<BlockPos> list, NetheriteVisuals.Options options, int i, int j, float f) {
      ArrayList var10 = new ArrayList(list);
      var10.sort(Comparator.comparingDouble(blockPos -> blockPos.getSquaredDistance(vec3d)));

      BlockPos var7;
      double var8;
      for (int var11 = 0;
         var11 < var10.size() && !((var8 = Math.sqrt((var7 = (BlockPos)var10.get(var11)).getSquaredDistance(vec3d))) > options.range());
         var11++
      ) {
         Box var12 = new Box(var7);
         if (var11 < 32) {
            for (int var13 = 0; var13 < SHELL_INFLATE.length; var13++) {
               this.faces(var12.expand(SHELL_INFLATE[var13]), GlyphTint.alpha(i, SHELL_ALPHA[var13] * (0.7F + 0.3F * f)));
            }
         }

         this.faces(var12, GlyphTint.alpha(i, 0.3F));
         this.box(var12, GlyphTint.alpha(j, 0.95F), 2.6F);
         Vec3d var14 = var12.getCenter();
         if (var11 < 3) {
            TracerEngine.draw(var14, GlyphTint.alpha(i, 0.8F));
         }

         if (var11 < 8) {
            this.labels.add(new NetheriteVisuals.Label(var14.add(0.0, 0.9, 0.0), String.format(Locale.ROOT, "%.0fm", var8), null));
         }
      }
   }

   private void drawWaves(long l, int i, int j) {
      Iterator var5 = this.waves.iterator();

      while (var5.hasNext()) {
         NetheriteVisuals.Wave var6 = (NetheriteVisuals.Wave)var5.next();
         float var7 = (float)(l - var6.born()) / 1.0E9F / 0.9F;
         if (var7 >= 1.0F) {
            var5.remove();
         } else {
            float var8 = 1.0F - (1.0F - var7) * (1.0F - var7) * (1.0F - var7);
            float var9 = 0.4F + 2.6F * var8;
            float var10 = (1.0F - var7) * (1.0F - var7);
            this.canvas.ring(var6.x(), var6.y(), var6.z(), var9 * 0.86F, var9, 48, GlyphTint.alpha(j, var10 * 0.9F));
            this.canvas.ring(var6.x(), var6.y(), var6.z(), var9, var9 * 1.25F, 48, GlyphTint.alpha(i, var10 * 0.3F));
         }
      }
   }

   private void line(double d, double e, double f, double g, double h, double i, int j, float k) {
      this.canvas.ribbon(d, e, f, g, h, i, j, k, this.pixel);
   }

   private void box(Box box, int i, float f) {
      this.line(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, i, f);
      this.line(box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, i, f);
      this.line(box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, i, f);
      this.line(box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, i, f);
      this.line(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, i, f);
      this.line(box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, i, f);
      this.line(box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, i, f);
      this.line(box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, i, f);
      this.line(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, i, f);
      this.line(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, i, f);
      this.line(box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, i, f);
      this.line(box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, i, f);
   }

   private void faces(Box box, int i) {
      this.face(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, i);
      this.face(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, i);
      this.face(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ, i);
      this.face(box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, i);
      this.face(box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, i);
      this.face(box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ, i);
   }

   private void face(double d, double e, double f, double g, double h, double i, double j, double k, double l, double m, double n, double o, int p) {
      this.canvas.quad(d, e, f, g, h, i, j, k, l, m, n, o, p);
      this.canvas.quad(d, e, f, m, n, o, j, k, l, g, h, i, p);
   }

   private float jitter(float f) {
      return (this.random.nextFloat() * 2.0F - 1.0F) * f;
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
