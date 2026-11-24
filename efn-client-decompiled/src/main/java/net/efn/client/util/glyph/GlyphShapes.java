package net.efn.client.util.glyph;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GlyphShapes {
   public static final float[] BOLT = bolt();
   public static final float[] GEM = gem();
   public static final float[] SPARK = spark();
   public static final float[] GRAIN = grain();
   public static final float[] DIAMOND = diamond();
   public static final float[] DISK = new GlyphShapes.Builder().disk(0.0F, 0.0F, 1.0F, 1.0F, 12).build();
   public static final float[] OCTAGON = new GlyphShapes.Builder().disk(0.0F, 0.0F, 1.0F, 1.0F, 8).build();
   public static final float[] HALO = new GlyphShapes.Builder().arc(0.0F, 0.0F, 0.84F, 0.84F, 1.0F, 1.0F, 0.0F, 6.2831855F, 20).build();

   private GlyphShapes() {
   }

   private static float[] bolt() {
      GlyphShapes.Builder var0 = new GlyphShapes.Builder();
      var0.quad(-0.17F, -1.0F, 0.17F, -1.0F, 0.09F, 0.06F, -0.09F, 0.06F);
      var0.quad(-0.56F, -0.02F, -0.09F, 0.04F, -0.09F, 0.2F, -0.56F, 0.26F);
      var0.quad(-0.09F, 0.04F, 0.09F, 0.04F, 0.09F, 0.2F, -0.09F, 0.2F);
      var0.quad(0.09F, 0.04F, 0.56F, -0.02F, 0.56F, 0.26F, 0.09F, 0.2F);
      var0.arc(0.0F, 0.6F, 0.19F, 0.28F, 0.3F, 0.42F, -1.5707964F, 4.712389F, 10);
      return var0.build();
   }

   private static float[] gem() {
      GlyphShapes.Builder var0 = new GlyphShapes.Builder();
      float var1 = 0.035F;
      float var2 = -0.12F;
      float var3 = 0.56F;
      float var4 = 0.78F;
      byte var5 = 5;

      for (int var6 = 0; var6 < var5; var6++) {
         double var7 = 1.5707963267948966 + 3.141592653589793 * var6 / var5;
         double var9 = 1.5707963267948966 + 3.141592653589793 * (var6 + 1) / var5;
         float var11 = var2 + (float)Math.sin(var7) * var4;
         float var12 = var2 + (float)Math.sin(var9) * var4;
         float var13 = (float)Math.cos(var7) * var3 - var1;
         float var14 = (float)Math.cos(var9) * var3 - var1;
         var0.quad(-var1, var11, var13, var11, var14, var12, -var1, var12);
         var0.quad(var1, -var11 + 2.0F * var2, -var13, -var11 + 2.0F * var2, -var14, -var12 + 2.0F * var2, var1, -var12 + 2.0F * var2);
      }

      var0.quad(-0.44F, 0.62F, 0.44F, 0.62F, 0.33F, 0.86F, -0.33F, 0.86F);
      var0.quad(-0.24F, 0.86F, 0.24F, 0.86F, 0.13F, 1.0F, -0.13F, 1.0F);
      var0.stroke(0.055F, -0.34F, 0.7F, -0.62F, 0.8F, -0.74F, 0.98F);
      var0.stroke(0.055F, 0.34F, 0.7F, 0.62F, 0.8F, 0.74F, 0.98F);
      var0.stroke(0.055F, -0.5F, 0.12F, -0.82F, 0.2F, -0.92F, 0.04F);
      var0.stroke(0.055F, 0.5F, 0.12F, 0.82F, 0.2F, 0.92F, 0.04F);
      var0.stroke(0.055F, -0.44F, -0.52F, -0.74F, -0.72F, -0.8F, -0.96F);
      var0.stroke(0.055F, 0.44F, -0.52F, 0.74F, -0.72F, 0.8F, -0.96F);
      return var0.build();
   }

   private static float[] spark() {
      GlyphShapes.Builder var0 = new GlyphShapes.Builder();
      var0.quad(0.0F, -1.0F, 0.15F, 0.0F, 0.0F, 1.0F, -0.15F, 0.0F);
      var0.quad(-0.62F, 0.0F, 0.0F, -0.13F, 0.62F, 0.0F, 0.0F, 0.13F);
      return var0.build();
   }

   private static float[] grain() {
      return new GlyphShapes.Builder().quad(-0.8F, -0.62F, 0.72F, -0.88F, 0.9F, 0.46F, -0.52F, 0.82F).build();
   }

   private static float[] diamond() {
      return new GlyphShapes.Builder().quad(0.0F, -1.0F, 1.0F, 0.0F, 0.0F, 1.0F, -1.0F, 0.0F).build();
   }

   private static float[] curve(float f, float g, float h, float i, float j, float k, int l) {
      float[] var7 = new float[(l + 1) * 2];

      for (int var8 = 0; var8 <= l; var8++) {
         float var9 = (float)var8 / l;
         float var10 = 1.0F - var9;
         var7[var8 * 2] = var10 * var10 * f + 2.0F * var10 * var9 * h + var9 * var9 * j;
         var7[var8 * 2 + 1] = var10 * var10 * g + 2.0F * var10 * var9 * i + var9 * var9 * k;
      }

      return var7;
   }

   private static float[] append(float[] fs, float f, float g) {
      float[] var3 = Arrays.copyOf(fs, fs.length + 2);
      var3[fs.length] = f;
      var3[fs.length + 1] = g;
      return var3;
   }

   @Environment(EnvType.CLIENT)
   private static final class Builder {
      private float[] data = new float[64];
      private int size;
      private float offsetX;
      private float offsetY;

      private Builder() {
      }

      GlyphShapes.Builder offset(float f, float g) {
         this.offsetX = f;
         this.offsetY = g;
         return this;
      }

      GlyphShapes.Builder quad(float f, float g, float h, float i, float j, float k, float l, float m) {
         if (this.size + 8 > this.data.length) {
            this.data = Arrays.copyOf(this.data, this.data.length * 2);
         }

         this.data[this.size++] = f + this.offsetX;
         this.data[this.size++] = g + this.offsetY;
         this.data[this.size++] = h + this.offsetX;
         this.data[this.size++] = i + this.offsetY;
         this.data[this.size++] = j + this.offsetX;
         this.data[this.size++] = k + this.offsetY;
         this.data[this.size++] = l + this.offsetX;
         this.data[this.size++] = m + this.offsetY;
         return this;
      }

      GlyphShapes.Builder arc(float f, float g, float h, float i, float j, float k, float l, float m, int n) {
         for (int var10 = 0; var10 < n; var10++) {
            double var11 = l + (double)((m - l) * var10) / n;
            double var13 = l + (double)((m - l) * (var10 + 1)) / n;
            float var15 = (float)Math.cos(var11);
            float var16 = (float)Math.sin(var11);
            float var17 = (float)Math.cos(var13);
            float var18 = (float)Math.sin(var13);
            this.quad(f + var15 * h, g + var16 * i, f + var15 * j, g + var16 * k, f + var17 * j, g + var18 * k, f + var17 * h, g + var18 * i);
         }

         return this;
      }

      GlyphShapes.Builder disk(float f, float g, float h, float i, int j) {
         float[] var7 = new float[j * 2];

         for (int var6 = 0; var6 < j; var6++) {
            double var8 = 6.283185307179586 * var6 / j + 3.141592653589793 / j;
            var7[var6 * 2] = f + (float)Math.cos(var8) * h;
            var7[var6 * 2 + 1] = g + (float)Math.sin(var8) * i;
         }

         for (byte var10 = 1; var10 + 1 < j; var10 += 2) {
            int var11 = Math.min(var10 + 2, j - 1);
            this.quad(
               var7[0], var7[1], var7[var10 * 2], var7[var10 * 2 + 1], var7[(var10 + 1) * 2], var7[(var10 + 1) * 2 + 1], var7[var11 * 2], var7[var11 * 2 + 1]
            );
         }

         return this;
      }

      GlyphShapes.Builder stroke(float f, float... fs) {
         int var4 = fs.length / 2;
         if (var4 < 2) {
            return this;
         } else {
            float var5 = f * 0.5F;
            float[] var6 = new float[var4];
            float[] var7 = new float[var4];

            for (int var3 = 0; var3 < var4; var3++) {
               float var12 = 0.0F;
               float var13 = 0.0F;
               float var14 = 0.0F;
               float var15 = 0.0F;
               float var9;
               float var10;
               float var11;
               if (var3 > 0
                  && (var11 = (float)Math.sqrt((var10 = fs[var3 * 2] - fs[var3 * 2 - 2]) * var10 + (var9 = fs[var3 * 2 + 1] - fs[var3 * 2 - 1]) * var9))
                     > 1.0E-6F) {
                  var14 = -var9 / var11;
                  var15 = var10 / var11;
                  var12 += var14;
                  var13 += var15;
               }

               if (var3 < var4 - 1
                  && (var11 = (float)Math.sqrt((var10 = fs[var3 * 2 + 2] - fs[var3 * 2]) * var10 + (var9 = fs[var3 * 2 + 3] - fs[var3 * 2 + 1]) * var9))
                     > 1.0E-6F) {
                  var12 += -var9 / var11;
                  var13 += var10 / var11;
                  if (var3 == 0) {
                     var14 = -var9 / var11;
                     var15 = var10 / var11;
                  }
               }

               float var8;
               if ((var8 = (float)Math.sqrt(var12 * var12 + var13 * var13)) < 1.0E-6F) {
                  var12 = var14;
                  var13 = var15;
                  var8 = 1.0F;
               }

               float var25;
               float var26;
               float var16 = Math.max(0.5F, (var25 = var12 / var8) * var14 + (var26 = var13 / var8) * var15);
               var6[var3] = var25 * var5 / var16;
               var7[var3] = var26 * var5 / var16;
            }

            for (int var17 = 0; var17 < var4 - 1; var17++) {
               float var18 = fs[var17 * 2];
               float var20 = fs[var17 * 2 + 1];
               float var22 = fs[var17 * 2 + 2];
               float var24 = fs[var17 * 2 + 3];
               this.quad(
                  var18 - var6[var17],
                  var20 - var7[var17],
                  var22 - var6[var17 + 1],
                  var24 - var7[var17 + 1],
                  var22 + var6[var17 + 1],
                  var24 + var7[var17 + 1],
                  var18 + var6[var17],
                  var20 + var7[var17]
               );
            }

            return this;
         }
      }

      float[] build() {
         return Arrays.copyOf(this.data, this.size);
      }
   }
}
