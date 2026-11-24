package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
final class AtmosphereSky {
   private static final float RADIUS = 100.0F;
   private static final float[] RINGS = new float[]{-30.0F, -12.0F, -4.0F, 0.0F, 4.0F, 9.0F, 16.0F, 26.0F, 40.0F, 58.0F, 76.0F, 90.0F};
   private static final int SEGMENTS = 48;
   private static final int HALO_RINGS = 4;
   private static final int HALO_SEGMENTS = 32;
   private static final int METEOR_SEGMENTS = 10;
   private static final float[] KEYS = new float[]{-0.35F, -0.08F, 0.06F, 0.3F};
   private static final int GOLDEN_ZENITH = 5210048;
   private static final int GOLDEN_HORIZON = 16032864;
   private static final int GOLDEN_GLOW = 16747069;
   private static final int DAY_ZENITH = 3108822;
   private static final int DAY_HORIZON = 11850996;
   private static final int DAY_GLOW = 16774362;
   private static final int BLUE_HOUR_ZENITH = 1319497;
   private final SkyMesh dome = new SkyMesh("EFN sky dome", SkyMesh.PAINT, (RINGS.length - 1) * 48);
   private final SkyMesh endDome = new SkyMesh("EFN end sky", SkyMesh.PAINT, (RINGS.length - 1) * 48);
   private final SkyMesh halo = new SkyMesh("EFN sun and moon glow", SkyMesh.GLOW, 256);
   private final SkyMesh meteorMesh = new SkyMesh("EFN shooting stars", SkyMesh.GLOW, 12);
   private final Random random = Random.create();
   private static final float DOME_SUN_STEP = 0.002F;
   private static final float DOME_STRENGTH_STEP = 0.004F;
   private int zenith;
   private int horizon;
   private int glow;
   private float glowStrength;
   private int builtZenith;
   private int builtHorizon;
   private int builtGlow;
   private float builtStrength;
   private float builtSunX = 0.0F / 0.0F;
   private float builtSunY = 0.0F / 0.0F;
   private float sunX;
   private float sunY;
   private float sunAngle;
   private float moonAngle;
   private boolean ready;
   private long meteorStart;
   private long meteorEnd;
   private long nextMeteor;
   private float mx;
   private float my;
   private float mz;
   private float tx;
   private float ty;
   private float tz;

   AtmosphereSky() {
   }

   void update(float f, float g, float h, float i, float j, int k, int l, int m) {
      this.sunAngle = f;
      this.moonAngle = g;
      this.sunX = -MathHelper.sin(f);
      this.sunY = MathHelper.cos(f);
      int var10 = ColorHelper.lerp(0.3F, 1319497, k);
      int[] var11 = new int[]{k, var10, 5210048, 3108822};
      int[] var12 = new int[]{l, scale(m, 0.75F), 16032864, 11850996};
      int[] var13 = new int[]{l, m, 16747069, 16774362};
      float[] var14 = new float[]{0.0F, 0.55F, 1.0F, 0.25F};
      int var9 = 0;

      while (var9 < KEYS.length - 2 && this.sunY > KEYS[var9 + 1]) {
         var9++;
      }

      float var15 = AtmosphereAurora.smoothstep((this.sunY - KEYS[var9]) / (KEYS[var9 + 1] - KEYS[var9]));
      this.zenith = ColorHelper.lerp(var15, var11[var9], var11[var9 + 1]);
      this.horizon = ColorHelper.lerp(var15, var12[var9], var12[var9 + 1]);
      this.glow = ColorHelper.lerp(var15, var13[var9], var13[var9 + 1]);
      this.glowStrength = MathHelper.lerp(var15, var14[var9], var14[var9 + 1]) * (1.0F - h);
      this.zenith = weather(this.zenith, h, i, j);
      this.horizon = weather(this.horizon, h, i, j);
      this.ready = true;
   }

   private static int weather(int i, float f, float g, float h) {
      int var4 = scale(luma(i), 0.85F);
      i = ColorHelper.lerp(f * 0.65F, i, var4);
      i = scale(i, 1.0F - 0.35F * f - 0.25F * g);
      return h > 0.0F ? ColorHelper.lerp(h * 0.45F, i, 15265023) : i;
   }

   boolean ready() {
      return this.ready;
   }

   int horizonToward(float f, float g) {
      float var3 = MathHelper.sqrt(f * f + g * g);
      return var3 < 1.0E-4F ? this.horizon : this.colourAt(f / var3, 0.02F, g / var3);
   }

   float golden() {
      return 1.0F - Math.min(1.0F, Math.abs(this.sunY - 0.06F) / 0.22F);
   }

   float nightness() {
      return AtmosphereAurora.smoothstep((0.05F - this.sunY) / 0.4F);
   }

   int twilightGlow() {
      return this.glow;
   }

   void renderDome(MatrixStack matrixStack) {
      if (!this.dome.isBuilt() || this.domeStale()) {
         BufferBuilder var2 = this.dome.begin();
         this.domeQuads(var2, false);
         this.dome.upload(var2);
         this.builtZenith = this.zenith;
         this.builtHorizon = this.horizon;
         this.builtGlow = this.glow;
         this.builtStrength = this.glowStrength;
         this.builtSunX = this.sunX;
         this.builtSunY = this.sunY;
      }

      this.dome.draw(matrixStack, 1.0F);
   }

   private boolean domeStale() {
      return this.zenith != this.builtZenith
         || this.horizon != this.builtHorizon
         || this.glow != this.builtGlow
         || Math.abs(this.glowStrength - this.builtStrength) > 0.004F
         || Math.abs(this.sunX - this.builtSunX) > 0.002F
         || Math.abs(this.sunY - this.builtSunY) > 0.002F;
   }

   void renderEndDome(MatrixStack matrixStack) {
      if (!this.endDome.isBuilt()) {
         BufferBuilder var2 = this.endDome.begin();
         this.domeQuads(var2, true);
         this.endDome.upload(var2);
      }

      this.endDome.draw(matrixStack, 1.0F);
   }

   private void domeQuads(BufferBuilder bufferBuilder, boolean bl) {
      for (int var3 = 0; var3 < RINGS.length - 1; var3++) {
         for (int var4 = 0; var4 < 48; var4++) {
            this.corner(bufferBuilder, RINGS[var3], var4, bl);
            this.corner(bufferBuilder, RINGS[var3], var4 + 1, bl);
            this.corner(bufferBuilder, RINGS[var3 + 1], var4 + 1, bl);
            this.corner(bufferBuilder, RINGS[var3 + 1], var4, bl);
         }
      }
   }

   private void corner(BufferBuilder bufferBuilder, float f, int i, boolean bl) {
      float var5 = f * 0.017453292F;
      float var6 = i / 48.0F * 6.2831855F;
      float var7 = MathHelper.cos(var5) * MathHelper.cos(var6);
      float var8 = MathHelper.sin(var5);
      float var9 = MathHelper.cos(var5) * MathHelper.sin(var6);
      int var10 = bl ? endColourAt(var8) : this.colourAt(var7, var8, var9);
      SkyMesh.vertex(bufferBuilder, var7 * 100.0F, var8 * 100.0F, var9 * 100.0F, 0xFF000000 | var10);
   }

   private int colourAt(float f, float g, float h) {
      int var4 = ColorHelper.lerp((float)Math.sqrt(Math.max(g, 0.0F)), this.horizon, this.zenith);
      if (g < 0.0F) {
         var4 = scale(var4, 1.0F - 0.35F * AtmosphereAurora.smoothstep(-g / 0.4F));
      }

      if (this.glowStrength > 0.001F) {
         float var5 = Math.max(0.0F, f * this.sunX + g * this.sunY);
         float var6 = Math.max(0.0F, this.sunX < 0.0F ? -f : f);
         float var7 = (float)(Math.pow(var5, 8.0) * 0.9 + Math.pow(var5, 2.0) * 0.25);
         float var8 = (float)(Math.exp(-Math.abs(g) * 9.0F) * Math.pow(var6, 1.5));
         float var9 = MathHelper.clamp(this.glowStrength * (var7 + var8 * 0.6F), 0.0F, 1.0F);
         var4 = ColorHelper.lerp(var9, var4, this.glow);
      }

      return var4 & 16777215;
   }

   private static int endColourAt(float f) {
      return ColorHelper.lerp((float)Math.sqrt(Math.max(f, 0.0F)), 1839152, 459789) & 16777215;
   }

   void renderHalos(MatrixStack matrixStack, float f, float g) {
      BufferBuilder var4 = this.halo.begin();
      float var5 = 1.0F - AtmosphereAurora.smoothstep(this.sunY / 0.35F);
      float var6 = AtmosphereAurora.smoothstep((this.sunY + 0.12F) / 0.12F) * f * (0.45F + 0.35F * var5);
      int var7 = ColorHelper.lerp(var5, 16773584, 16742963);
      haloQuads(var4, this.sunAngle, 28.0F * (1.0F + 0.6F * var5), var7, var6);
      haloQuads(var4, this.moonAngle, 34.0F, 11520255, 0.32F * g * f);
      this.halo.upload(var4);
      this.halo.draw(matrixStack, 1.0F);
   }

   private static void haloQuads(BufferBuilder bufferBuilder, float f, float g, int i, float h) {
      if (!(h <= 0.002F)) {
         float[] var5 = new float[]{0.0F, g * 0.16F, g * 0.38F, g * 0.66F, g};
         float[] var6 = new float[]{1.0F, 0.55F, 0.22F, 0.07F, 0.0F};
         float var7 = MathHelper.cos(f);
         float var8 = MathHelper.sin(f);

         for (int var9 = 0; var9 < 4; var9++) {
            for (int var10 = 0; var10 < 32; var10++) {
               haloVertex(bufferBuilder, var5[var9], var10, var7, var8, i, var6[var9] * h);
               haloVertex(bufferBuilder, var5[var9], var10 + 1, var7, var8, i, var6[var9] * h);
               haloVertex(bufferBuilder, var5[var9 + 1], var10 + 1, var7, var8, i, var6[var9 + 1] * h);
               haloVertex(bufferBuilder, var5[var9 + 1], var10, var7, var8, i, var6[var9 + 1] * h);
            }
         }
      }
   }

   private static void haloVertex(BufferBuilder bufferBuilder, float f, int i, float g, float h, int j, float k) {
      float var7 = i / 32.0F * 6.2831855F;
      float var8 = MathHelper.cos(var7) * f;
      float var9 = 100.0F;
      float var10 = MathHelper.sin(var7) * f;
      float var11 = var9 * g - var10 * h;
      float var12 = var9 * h + var10 * g;
      SkyMesh.vertex(bufferBuilder, -var12, var11, var8, Math.round(MathHelper.clamp(k, 0.0F, 1.0F) * 255.0F) << 24 | j & 16777215);
   }

   void renderMeteors(MatrixStack matrixStack, float f, float g) {
      long var4 = System.nanoTime();
      if (var4 >= this.meteorEnd) {
         if (this.nextMeteor == 0L) {
            this.nextMeteor = var4 + seconds(4.0F + 10.0F * this.random.nextFloat());
         }

         if (var4 < this.nextMeteor || f < 0.6F || g < 0.8F) {
            return;
         }

         this.launch(var4);
      }

      float var6 = (float)(var4 - this.meteorStart) / (float)(this.meteorEnd - this.meteorStart);
      float var7 = MathHelper.sin(3.1415927F * MathHelper.clamp(var6, 0.0F, 1.0F)) * f * g;
      BufferBuilder var8 = this.meteorMesh.begin();
      float var9 = var6 * 0.55F;
      float var10 = 0.16F;
      float[] var11 = null;

      for (int var12 = 0; var12 <= 10; var12++) {
         float var13 = var9 - var10 * var12 / 10.0F;
         float[] var14 = new float[]{(this.mx + this.tx * var13) * 100.0F, (this.my + this.ty * var13) * 100.0F, (this.mz + this.tz * var13) * 100.0F};
         if (var11 != null) {
            streak(var8, var11, var14, var7 * (1.0F - (var12 - 1.0F) / 10.0F), var7 * (1.0F - var12 / 10.0F));
         }

         var11 = var14;
      }

      this.meteorMesh.upload(var8);
      this.meteorMesh.draw(matrixStack, 1.0F);
   }

   private void launch(long l) {
      float var3 = (30.0F + 40.0F * this.random.nextFloat()) * 0.017453292F;
      float var4 = this.random.nextFloat() * 6.2831855F;
      this.mx = MathHelper.cos(var3) * MathHelper.cos(var4);
      this.my = MathHelper.sin(var3);
      this.mz = MathHelper.cos(var3) * MathHelper.sin(var4);
      float var5 = -MathHelper.sin(var4);
      float var6 = MathHelper.cos(var4);
      float var7 = -0.6F - 0.4F * this.random.nextFloat();
      float var8 = var5 * this.mx + var7 * this.my + var6 * this.mz;
      this.tx = var5 - var8 * this.mx;
      this.ty = var7 - var8 * this.my;
      this.tz = var6 - var8 * this.mz;
      float var9 = MathHelper.sqrt(this.tx * this.tx + this.ty * this.ty + this.tz * this.tz);
      this.tx /= var9;
      this.ty /= var9;
      this.tz /= var9;
      this.meteorStart = l;
      this.meteorEnd = l + seconds(0.7F + 0.5F * this.random.nextFloat());
      this.nextMeteor = this.meteorEnd + seconds(6.0F + 18.0F * this.random.nextFloat());
   }

   private static void streak(BufferBuilder bufferBuilder, float[] fs, float[] gs, float f, float g) {
      float var5 = gs[2] - fs[2];
      float var6 = gs[1] - fs[1];
      float var7 = fs[1] * var5 - fs[2] * var6;
      float var8 = gs[0] - fs[0];
      float var9 = fs[2] * var8 - fs[0] * var5;
      float var10 = fs[0] * var6 - fs[1] * var8;
      float var11 = MathHelper.sqrt(var7 * var7 + var9 * var9 + var10 * var10);
      if (!(var11 < 1.0E-5F)) {
         float var12 = 0.22F / var11;
         int var13 = Math.round(MathHelper.clamp(f, 0.0F, 1.0F) * 255.0F) << 24 | 15397631;
         int var14 = Math.round(MathHelper.clamp(g, 0.0F, 1.0F) * 255.0F) << 24 | 15397631;
         float var15;
         float var16;
         float var17;
         SkyMesh.vertex(bufferBuilder, fs[0] - (var15 = var7 * var12), fs[1] - (var16 = var9 * var12), fs[2] - (var17 = var10 * var12), var13);
         SkyMesh.vertex(bufferBuilder, fs[0] + var15, fs[1] + var16, fs[2] + var17, var13);
         SkyMesh.vertex(bufferBuilder, gs[0] + var15, gs[1] + var16, gs[2] + var17, var14);
         SkyMesh.vertex(bufferBuilder, gs[0] - var15, gs[1] - var16, gs[2] - var17, var14);
      }
   }

   private static long seconds(float f) {
      return (long)(f * 1.0E9F);
   }

   static int scale(int i, float f) {
      int var2 = MathHelper.clamp(Math.round((i >> 16 & 0xFF) * f), 0, 255);
      int var3 = MathHelper.clamp(Math.round((i >> 8 & 0xFF) * f), 0, 255);
      int var4 = MathHelper.clamp(Math.round((i & 0xFF) * f), 0, 255);
      return i & 0xFF000000 | var2 << 16 | var3 << 8 | var4;
   }

   static int luma(int i) {
      int var1 = Math.round(0.2126F * (i >> 16 & 0xFF) + 0.7152F * (i >> 8 & 0xFF) + 0.0722F * (i & 0xFF));
      return i & 0xFF000000 | var1 << 16 | var1 << 8 | var1;
   }

   void close() {
      this.dome.close();
      this.endDome.close();
      this.halo.close();
      this.meteorMesh.close();
      this.ready = false;
   }
}
