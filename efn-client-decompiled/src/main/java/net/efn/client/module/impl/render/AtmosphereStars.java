package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class AtmosphereStars {
   private static final float RADIUS = 100.0F;
   private static final int STARS = 2600;
   private static final float GLOW_ABOVE = 0.8F;
   private static final long TWINKLE_NANOS = 50000000L;
   private static final int BAND_COLUMNS = 180;
   private static final int BAND_ROWS = 12;
   private static final float BAND_HALF_WIDTH = 17.0F;
   private static final int NEBULA_COLUMNS = 72;
   private static final int NEBULA_ROWS = 36;
   private static final Vector3f BAND_NORMAL = new Vector3f(0.42F, 0.5F, 0.76F).normalize();
   private final SkyMesh starMesh = new SkyMesh("EFN stars", SkyMesh.GLOW, 5200);
   private final SkyMesh bandMesh = new SkyMesh("EFN milky way", SkyMesh.GLOW, 2160);
   private final SkyMesh nebulaMesh = new SkyMesh("EFN end nebulae", SkyMesh.GLOW, 2592);
   private final float[] dir = new float[7800];
   private final float[] size = new float[2600];
   private final float[] light = new float[2600];
   private final float[] roll = new float[2600];
   private final float[] phase = new float[2600];
   private final float[] speed = new float[2600];
   private final int[] rgb = new int[2600];
   private boolean placed;
   private long lastTwinkle;
   private float builtBrightness = -1.0F;
   private int bandTint;

   AtmosphereStars() {
   }

   void renderStars(MatrixStack matrixStack, float f, float g) {
      long var4 = System.nanoTime();
      if (var4 - this.lastTwinkle > 50000000L || g != this.builtBrightness) {
         this.lastTwinkle = var4;
         this.builtBrightness = g;
         this.buildStars((float)(var4 % 3600000000000L) / 1.0E9F, g);
      }

      this.starMesh.draw(matrixStack, f);
   }

   void renderBand(MatrixStack matrixStack, float f, int i) {
      if (!this.bandMesh.isBuilt() || i != this.bandTint) {
         this.bandTint = i;
         this.buildBand(i);
      }

      this.bandMesh.draw(matrixStack, f);
   }

   void renderNebulae(MatrixStack matrixStack, float f) {
      if (!this.nebulaMesh.isBuilt()) {
         this.buildNebulae();
      }

      this.nebulaMesh.draw(matrixStack, f);
   }

   private void place() {
      Random var1 = Random.create(10842L);
      Vector3f var2 = new Vector3f();
      Vector3f var3 = new Vector3f();
      bandAxes(var2, var3);

      for (int var4 = 0; var4 < 2600; var4++) {
         float var5;
         float var6;
         float var7;
         if (var1.nextFloat() < 0.3F) {
            float var8 = var1.nextFloat() * 6.2831855F;
            float var9 = (float)var1.nextGaussian() * 9.0F * 0.017453292F;
            float var10 = MathHelper.cos(var9);
            float var11 = MathHelper.sin(var9);
            var7 = var10 * (MathHelper.cos(var8) * var2.x + MathHelper.sin(var8) * var3.x) + var11 * BAND_NORMAL.x;
            var6 = var10 * (MathHelper.cos(var8) * var2.y + MathHelper.sin(var8) * var3.y) + var11 * BAND_NORMAL.y;
            var5 = var10 * (MathHelper.cos(var8) * var2.z + MathHelper.sin(var8) * var3.z) + var11 * BAND_NORMAL.z;
         } else {
            var6 = var1.nextFloat() * 2.0F - 1.0F;
            float var12 = var1.nextFloat() * 6.2831855F;
            float var14 = MathHelper.sqrt(1.0F - var6 * var6);
            var7 = var14 * MathHelper.cos(var12);
            var5 = var14 * MathHelper.sin(var12);
         }

         this.dir[var4 * 3] = var7;
         this.dir[var4 * 3 + 1] = var6;
         this.dir[var4 * 3 + 2] = var5;
         float var13 = (float)Math.pow(var1.nextFloat(), 6.0);
         this.size[var4] = 0.1F + 0.5F * var13;
         this.light[var4] = MathHelper.clamp(0.3F + 0.7F * var13 + 0.25F * var1.nextFloat(), 0.0F, 1.0F);
         this.roll[var4] = var1.nextFloat() * 6.2831855F;
         this.phase[var4] = var1.nextFloat() * 6.2831855F;
         this.speed[var4] = 1.5F + 3.5F * var1.nextFloat();
         this.rgb[var4] = starColour(var1.nextFloat());
      }

      this.placed = true;
   }

   private static int starColour(float f) {
      if (f < 0.18F) {
         return ColorHelper.lerp(f / 0.18F, 16757870, 16774116);
      } else {
         return f < 0.75F ? 16775408 : ColorHelper.lerp((f - 0.75F) / 0.25F, 16774134, 11125503);
      }
   }

   private void buildStars(float f, float g) {
      if (!this.placed) {
         this.place();
      }

      BufferBuilder var3 = this.starMesh.begin();

      for (int var4 = 0; var4 < 2600; var4++) {
         float var5 = 0.62F + 0.38F * MathHelper.sin(f * this.speed[var4] + this.phase[var4]);
         float var6 = MathHelper.clamp(this.light[var4] * var5 * g, 0.0F, 1.0F);
         this.star(var3, var4, this.size[var4], var6);
         if (this.light[var4] > 0.8F) {
            this.star(var3, var4, this.size[var4] * 4.0F, var6 * 0.12F);
         }
      }

      this.starMesh.upload(var3);
   }

   private void star(BufferBuilder bufferBuilder, int i, float f, float g) {
      float var8 = this.dir[i * 3];
      float var9 = this.dir[i * 3 + 1];
      float var10 = this.dir[i * 3 + 2];
      float var5;
      float var6;
      float var7;
      if (Math.abs(var9) < 0.9F) {
         var7 = var10;
         var6 = 0.0F;
         var5 = -var8;
      } else {
         var7 = 0.0F;
         var6 = -var10;
         var5 = var9;
      }

      float var11 = MathHelper.sqrt(var7 * var7 + var6 * var6 + var5 * var5);
      float var27;
      float var28;
      float var12 = var9 * (var27 = var5 / var11) - var10 * (var28 = var6 / var11);
      float var29;
      float var13 = var10 * (var29 = var7 / var11) - var8 * var27;
      float var14 = var8 * var28 - var9 * var29;
      float var15 = MathHelper.cos(this.roll[i]) * f;
      float var16 = MathHelper.sin(this.roll[i]) * f;
      float var17 = var29 * var15 + var12 * var16;
      float var18 = var28 * var15 + var13 * var16;
      float var19 = var27 * var15 + var14 * var16;
      float var20 = var12 * var15 - var29 * var16;
      float var21 = var13 * var15 - var28 * var16;
      float var22 = var14 * var15 - var27 * var16;
      float var23 = var8 * 100.0F;
      float var24 = var9 * 100.0F;
      float var25 = var10 * 100.0F;
      int var26 = Math.round(g * 255.0F) << 24 | this.rgb[i];
      SkyMesh.vertex(bufferBuilder, var23 - var17 - var20, var24 - var18 - var21, var25 - var19 - var22, var26);
      SkyMesh.vertex(bufferBuilder, var23 + var17 - var20, var24 + var18 - var21, var25 + var19 - var22, var26);
      SkyMesh.vertex(bufferBuilder, var23 + var17 + var20, var24 + var18 + var21, var25 + var19 + var22, var26);
      SkyMesh.vertex(bufferBuilder, var23 - var17 + var20, var24 - var18 + var21, var25 - var19 + var22, var26);
   }

   private static void bandAxes(Vector3f vector3f, Vector3f vector3f2) {
      new Vector3f(0.0F, 1.0F, 0.0F).cross(BAND_NORMAL, vector3f).normalize();
      new Vector3f(BAND_NORMAL).cross(vector3f, vector3f2).normalize();
   }

   private void buildBand(int i) {
      Vector3f var2 = new Vector3f();
      Vector3f var3 = new Vector3f();
      bandAxes(var2, var3);
      float[][] var4 = new float[181][13];
      float[][] var5 = new float[181][13];
      float[][] var6 = new float[181][13];
      int[][] var7 = new int[181][13];

      for (int var8 = 0; var8 <= 180; var8++) {
         float var9 = var8 / 180.0F * 6.2831855F;
         float var10 = MathHelper.cos(var9) * var2.x + MathHelper.sin(var9) * var3.x;
         float var11 = MathHelper.cos(var9) * var2.y + MathHelper.sin(var9) * var3.y;
         float var12 = MathHelper.cos(var9) * var2.z + MathHelper.sin(var9) * var3.z;
         float var13 = 0.55F + 0.45F * fbm(var10 * 1.7F, var11 * 1.7F, var12 * 1.7F);
         float var14 = 3.0F * (fbm(var10 * 2.3F + 5.0F, var11 * 2.3F, var12 * 2.3F) - 0.5F);
         float var15 = MathHelper.clamp(fbm(var10 * 1.4F, var11 * 1.4F + 9.0F, var12 * 1.4F) * 1.6F - 0.35F, 0.0F, 1.0F);

         for (int var16 = 0; var16 <= 12; var16++) {
            float var17 = (var16 / 12.0F * 2.0F - 1.0F) * 17.0F;
            float var18 = var17 * 0.017453292F;
            float var19 = MathHelper.cos(var18);
            float var20 = MathHelper.sin(var18);
            float var21 = var19 * var10 + var20 * BAND_NORMAL.x;
            float var22 = var19 * var11 + var20 * BAND_NORMAL.y;
            float var23 = var19 * var12 + var20 * BAND_NORMAL.z;
            var4[var8][var16] = var21 * 100.0F;
            var5[var8][var16] = var22 * 100.0F;
            var6[var8][var16] = var23 * 100.0F;
            float var24 = var17 / (17.0F * var13);
            float var25 = (float)Math.exp(-var24 * var24 * 2.2F);
            float var26 = 0.35F + 0.65F * fbm(var21 * 6.0F, var22 * 6.0F, var23 * 6.0F);
            float var27 = 1.0F - 0.75F * var15 * (float)Math.exp(-sq((var17 - var14) / 2.6F));
            float var28 = MathHelper.clamp(var25 * var26 * var27, 0.0F, 1.0F);
            int var29 = ColorHelper.lerp(var25, 7306440, i) & 16777215;
            var7[var8][var16] = Math.round(var28 * 0.42F * 255.0F) << 24 | var29;
         }
      }

      BufferBuilder var30 = this.bandMesh.begin();

      for (int var31 = 0; var31 < 180; var31++) {
         for (int var32 = 0; var32 < 12; var32++) {
            SkyMesh.vertex(var30, var4[var31][var32], var5[var31][var32], var6[var31][var32], var7[var31][var32]);
            SkyMesh.vertex(var30, var4[var31 + 1][var32], var5[var31 + 1][var32], var6[var31 + 1][var32], var7[var31 + 1][var32]);
            SkyMesh.vertex(var30, var4[var31 + 1][var32 + 1], var5[var31 + 1][var32 + 1], var6[var31 + 1][var32 + 1], var7[var31 + 1][var32 + 1]);
            SkyMesh.vertex(var30, var4[var31][var32 + 1], var5[var31][var32 + 1], var6[var31][var32 + 1], var7[var31][var32 + 1]);
         }
      }

      this.bandMesh.upload(var30);
   }

   private void buildNebulae() {
      float[][] var2 = new float[73][37];
      float[][] var3 = new float[73][37];
      float[][] var4 = new float[73][37];
      int[][] var5 = new int[73][37];

      for (int var6 = 0; var6 <= 72; var6++) {
         float var7 = var6 / 72.0F * 6.2831855F;

         for (int var1 = 0; var1 <= 36; var1++) {
            float var8 = (var1 / 36.0F - 0.5F) * 3.1415927F;
            float var9 = MathHelper.cos(var8) * MathHelper.cos(var7);
            float var10 = MathHelper.sin(var8);
            float var11 = MathHelper.cos(var8) * MathHelper.sin(var7);
            var2[var6][var1] = var9 * 100.0F;
            var3[var6][var1] = var10 * 100.0F;
            var4[var6][var1] = var11 * 100.0F;
            float var12 = AtmosphereAurora.smoothstep((fbm(var9 * 2.2F, var10 * 2.2F, var11 * 2.2F) - 0.42F) / 0.35F);
            float var13 = fbm(var9 * 1.3F + 11.0F, var10 * 1.3F, var11 * 1.3F);
            int var14 = var13 < 0.5F ? ColorHelper.lerp(var13 * 2.0F, 8011007, 16732104) : ColorHelper.lerp((var13 - 0.5F) * 2.0F, 16732104, 3989712);
            var5[var6][var1] = Math.round(var12 * 0.55F * 255.0F) << 24 | var14 & 16777215;
         }
      }

      BufferBuilder var16 = this.nebulaMesh.begin();

      for (int var17 = 0; var17 < 72; var17++) {
         for (int var15 = 0; var15 < 36; var15++) {
            SkyMesh.vertex(var16, var2[var17][var15], var3[var17][var15], var4[var17][var15], var5[var17][var15]);
            SkyMesh.vertex(var16, var2[var17 + 1][var15], var3[var17 + 1][var15], var4[var17 + 1][var15], var5[var17 + 1][var15]);
            SkyMesh.vertex(var16, var2[var17 + 1][var15 + 1], var3[var17 + 1][var15 + 1], var4[var17 + 1][var15 + 1], var5[var17 + 1][var15 + 1]);
            SkyMesh.vertex(var16, var2[var17][var15 + 1], var3[var17][var15 + 1], var4[var17][var15 + 1], var5[var17][var15 + 1]);
         }
      }

      this.nebulaMesh.upload(var16);
   }

   static float fbm(float f, float g, float h) {
      float var3 = 0.0F;
      float var4 = 0.5F;
      float var5 = 0.0F;

      for (int var6 = 0; var6 < 4; var6++) {
         var3 += var4 * noise(f, g, h);
         var5 += var4;
         f *= 2.03F;
         g *= 2.03F;
         h *= 2.03F;
         var4 *= 0.5F;
      }

      return var3 / var5;
   }

   private static float noise(float f, float g, float h) {
      int var3 = MathHelper.floor(f);
      int var4 = MathHelper.floor(g);
      int var5 = MathHelper.floor(h);
      float var6 = f - var3;
      float var7 = g - var4;
      float var8 = h - var5;
      var6 = var6 * var6 * (3.0F - 2.0F * var6);
      var7 = var7 * var7 * (3.0F - 2.0F * var7);
      var8 = var8 * var8 * (3.0F - 2.0F * var8);
      float var9 = MathHelper.lerp(var6, hash(var3, var4, var5), hash(var3 + 1, var4, var5));
      float var10 = MathHelper.lerp(var6, hash(var3, var4 + 1, var5), hash(var3 + 1, var4 + 1, var5));
      float var11 = MathHelper.lerp(var6, hash(var3, var4, var5 + 1), hash(var3 + 1, var4, var5 + 1));
      float var12 = MathHelper.lerp(var6, hash(var3, var4 + 1, var5 + 1), hash(var3 + 1, var4 + 1, var5 + 1));
      return MathHelper.lerp(var8, MathHelper.lerp(var7, var9, var10), MathHelper.lerp(var7, var11, var12));
   }

   private static float hash(int i, int j, int k) {
      int var3 = i * 374761393 + j * 668265263 + k * 1274126177;
      var3 = (var3 ^ var3 >>> 13) * 1274126177;
      return ((var3 ^ var3 >>> 16) & 16777215) / 1.6777215E7F;
   }

   private static float sq(float f) {
      return f * f;
   }

   void close() {
      this.starMesh.close();
      this.bandMesh.close();
      this.nebulaMesh.close();
      this.builtBrightness = -1.0F;
   }
}
