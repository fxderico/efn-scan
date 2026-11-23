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

   private static float[] curve(float var0, float var1, float var2, float var3, float var4, float var5, int var6) {
      float[] var7 = new float[(var6 + 1) * 2];

      for (int var8 = 0; var8 <= var6; var8++) {
         float var9 = (float)var8 / var6;
         float var10 = 1.0F - var9;
         var7[var8 * 2] = var10 * var10 * var0 + 2.0F * var10 * var9 * var2 + var9 * var9 * var4;
         var7[var8 * 2 + 1] = var10 * var10 * var1 + 2.0F * var10 * var9 * var3 + var9 * var9 * var5;
      }

      return var7;
   }

   private static float[] append(float[] var0, float var1, float var2) {
      float[] var3 = Arrays.copyOf(var0, var0.length + 2);
      var3[var0.length] = var1;
      var3[var0.length + 1] = var2;
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

      GlyphShapes.Builder offset(float var1, float var2) {
         this.offsetX = var1;
         this.offsetY = var2;
         return this;
      }

      GlyphShapes.Builder quad(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
         if (this.size + 8 > this.data.length) {
            this.data = Arrays.copyOf(this.data, this.data.length * 2);
         }

         this.data[this.size++] = var1 + this.offsetX;
         this.data[this.size++] = var2 + this.offsetY;
         this.data[this.size++] = var3 + this.offsetX;
         this.data[this.size++] = var4 + this.offsetY;
         this.data[this.size++] = var5 + this.offsetX;
         this.data[this.size++] = var6 + this.offsetY;
         this.data[this.size++] = var7 + this.offsetX;
         this.data[this.size++] = var8 + this.offsetY;
         return this;
      }

      GlyphShapes.Builder arc(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, int var9) {
         for (int var10 = 0; var10 < var9; var10++) {
            double var11 = var7 + (double)((var8 - var7) * var10) / var9;
            double var13 = var7 + (double)((var8 - var7) * (var10 + 1)) / var9;
            float var15 = (float)Math.cos(var11);
            float var16 = (float)Math.sin(var11);
            float var17 = (float)Math.cos(var13);
            float var18 = (float)Math.sin(var13);
            this.quad(
               var1 + var15 * var3,
               var2 + var16 * var4,
               var1 + var15 * var5,
               var2 + var16 * var6,
               var1 + var17 * var5,
               var2 + var18 * var6,
               var1 + var17 * var3,
               var2 + var18 * var4
            );
         }

         return this;
      }

      GlyphShapes.Builder disk(float var1, float var2, float var3, float var4, int var5) {
         float[] var7 = new float[var5 * 2];

         for (int var6 = 0; var6 < var5; var6++) {
            double var8 = 6.283185307179586 * var6 / var5 + 3.141592653589793 / var5;
            var7[var6 * 2] = var1 + (float)Math.cos(var8) * var3;
            var7[var6 * 2 + 1] = var2 + (float)Math.sin(var8) * var4;
         }

         for (byte var10 = 1; var10 + 1 < var5; var10 += 2) {
            int var11 = Math.min(var10 + 2, var5 - 1);
            this.quad(
               var7[0], var7[1], var7[var10 * 2], var7[var10 * 2 + 1], var7[(var10 + 1) * 2], var7[(var10 + 1) * 2 + 1], var7[var11 * 2], var7[var11 * 2 + 1]
            );
         }

         return this;
      }

      GlyphShapes.Builder stroke(float var1, float... var2) {
         int var4 = var2.length / 2;
         if (var4 < 2) {
            return this;
         } else {
            float var5 = var1 * 0.5F;
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
                  && (var11 = (float)Math.sqrt((var10 = var2[var3 * 2] - var2[var3 * 2 - 2]) * var10 + (var9 = var2[var3 * 2 + 1] - var2[var3 * 2 - 1]) * var9))
                     > 1.0E-6F) {
                  var14 = -var9 / var11;
                  var15 = var10 / var11;
                  var12 += var14;
                  var13 += var15;
               }

               if (var3 < var4 - 1
                  && (var11 = (float)Math.sqrt((var10 = var2[var3 * 2 + 2] - var2[var3 * 2]) * var10 + (var9 = var2[var3 * 2 + 3] - var2[var3 * 2 + 1]) * var9))
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
               float var18 = var2[var17 * 2];
               float var20 = var2[var17 * 2 + 1];
               float var22 = var2[var17 * 2 + 2];
               float var24 = var2[var17 * 2 + 3];
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
