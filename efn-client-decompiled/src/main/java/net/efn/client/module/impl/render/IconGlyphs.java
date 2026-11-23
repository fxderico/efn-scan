package net.efn.client.module.impl.render;

import java.util.Arrays;
import net.efn.client.util.glyph.GlyphShapes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class IconGlyphs {
   static final float[] BOLT = GlyphShapes.BOLT;
   static final float[] GEM = GlyphShapes.GEM;
   static final float[] DOLLAR = dollar(16);
   static final float[] EURO = euro(26);
   static final float[] BITCOIN = bitcoin(12, 44);
   static final float[] LETTER_E = letterE();
   static final float[] LETTER_F = letterF();
   static final float[] LETTER_N = letterN();

   private IconGlyphs() {
   }

   private static void sq(IconGlyphs.Sketch var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = 0.16F;
      var0.quad(var1 - var9 * var2, var2, var3 - var9 * var4, var4, var5 - var9 * var6, var6, var7 - var9 * var8, var8);
   }

   private static float[] letterE() {
      IconGlyphs.Sketch var0 = new IconGlyphs.Sketch();
      sq(var0, -0.55F, -0.9F, -0.2F, -0.9F, -0.2F, 0.9F, -0.55F, 0.9F);
      sq(var0, -0.2F, -0.9F, 0.55F, -0.9F, 0.55F, -0.58F, -0.2F, -0.58F);
      sq(var0, -0.2F, -0.16F, 0.45F, -0.16F, 0.45F, 0.16F, -0.2F, 0.16F);
      sq(var0, -0.2F, 0.58F, 0.55F, 0.58F, 0.55F, 0.9F, -0.2F, 0.9F);
      return var0.build();
   }

   private static float[] letterF() {
      IconGlyphs.Sketch var0 = new IconGlyphs.Sketch();
      sq(var0, -0.55F, -0.9F, -0.2F, -0.9F, -0.2F, 0.9F, -0.55F, 0.9F);
      sq(var0, -0.2F, -0.9F, 0.55F, -0.9F, 0.55F, -0.58F, -0.2F, -0.58F);
      sq(var0, -0.2F, -0.16F, 0.45F, -0.16F, 0.45F, 0.16F, -0.2F, 0.16F);
      return var0.build();
   }

   private static float[] letterN() {
      IconGlyphs.Sketch var0 = new IconGlyphs.Sketch();
      sq(var0, -0.6F, -0.9F, -0.25F, -0.9F, -0.25F, 0.9F, -0.6F, 0.9F);
      sq(var0, 0.25F, -0.9F, 0.6F, -0.9F, 0.6F, 0.9F, 0.25F, 0.9F);
      sq(var0, -0.25F, -0.9F, -0.0F, -0.9F, 0.25F, 0.9F, 0.0F, 0.9F);
      return var0.build();
   }

   static float[] mobile(float[] var0) {
      if (var0 == DOLLAR) {
         return IconGlyphs.Mobile.DOLLAR;
      } else if (var0 == EURO) {
         return IconGlyphs.Mobile.EURO;
      } else {
         return var0 == BITCOIN ? IconGlyphs.Mobile.BITCOIN : var0;
      }
   }

   private static float[] dollar(int var0) {
      float var1 = 0.34F;
      float[] var2 = arc(0.0F, var1, var1, 28.0F, 270.0F, var0);
      float[] var3 = arc(0.0F, -var1, var1, 90.0F, -152.0F, var0);
      IconGlyphs.Sketch var4 = new IconGlyphs.Sketch();
      var4.stroke(0.17F, join(var2, Arrays.copyOfRange(var3, 2, var3.length)));
      var4.quad(-0.055F, -0.98F, 0.055F, -0.98F, 0.055F, 0.98F, -0.055F, 0.98F);
      return var4.build();
   }

   private static float[] euro(int var0) {
      IconGlyphs.Sketch var1 = new IconGlyphs.Sketch();
      var1.stroke(0.17F, arc(0.1F, 0.0F, 0.62F, 48.0F, 312.0F, var0));
      var1.quad(-0.82F, 0.06F, 0.16F, 0.06F, 0.16F, 0.2F, -0.82F, 0.2F);
      var1.quad(-0.82F, -0.22F, 0.1F, -0.22F, 0.1F, -0.08F, -0.82F, -0.08F);
      return var1.build();
   }

   private static float[] bitcoin(int var0, int var1) {
      IconGlyphs.Sketch var2 = new IconGlyphs.Sketch();
      var2.quad(-0.3F, -0.56F, -0.17F, -0.56F, -0.17F, 0.56F, -0.3F, 0.56F);
      var2.stroke(0.13F, join(new float[]{-0.24F, 0.5F}, arc(0.06F, 0.27F, 0.23F, 90.0F, -90.0F, var0), new float[]{-0.24F, 0.04F}));
      var2.stroke(0.13F, join(new float[]{-0.24F, 0.04F}, arc(0.1F, -0.24F, 0.28F, 90.0F, -90.0F, var0), new float[]{-0.24F, -0.52F}));
      var2.quad(-0.21F, 0.5F, -0.13F, 0.5F, -0.13F, 0.74F, -0.21F, 0.74F);
      var2.quad(-0.03F, 0.5F, 0.05F, 0.5F, 0.05F, 0.74F, -0.03F, 0.74F);
      var2.quad(-0.21F, -0.74F, -0.13F, -0.74F, -0.13F, -0.5F, -0.21F, -0.5F);
      var2.quad(-0.03F, -0.74F, 0.05F, -0.74F, 0.05F, -0.5F, -0.03F, -0.5F);
      float[] var3 = var2.build();
      float var4 = (float)Math.cos(Math.toRadians(-14.0));
      float var5 = (float)Math.sin(Math.toRadians(-14.0));

      for (byte var6 = 0; var6 < var3.length; var6 += 2) {
         float var7 = var3[var6] + 0.04F;
         float var8 = var3[var6 + 1];
         var3[var6] = var7 * var4 - var8 * var5;
         var3[var6 + 1] = var7 * var5 + var8 * var4;
      }

      IconGlyphs.Sketch var9 = new IconGlyphs.Sketch();
      var9.stroke(0.1F, arc(0.0F, 0.0F, 0.92F, 0.0F, 360.0F, var1));
      return join(var9.build(), var3);
   }

   private static float[] arc(float var0, float var1, float var2, float var3, float var4, int var5) {
      float[] var6 = new float[(var5 + 1) * 2];

      for (int var7 = 0; var7 <= var5; var7++) {
         double var8 = Math.toRadians(var3 + (var4 - var3) * var7 / var5);
         var6[var7 * 2] = var0 + var2 * (float)Math.cos(var8);
         var6[var7 * 2 + 1] = var1 + var2 * (float)Math.sin(var8);
      }

      return var6;
   }

   private static float[] join(float[]... var0) {
      int var1 = 0;

      for (float[] var5 : var0) {
         var1 += var5.length;
      }

      float[] var8 = new float[var1];
      int var9 = 0;

      for (float[] var7 : var0) {
         System.arraycopy(var7, 0, var8, var9, var7.length);
         var9 += var7.length;
      }

      return var8;
   }

   @Environment(EnvType.CLIENT)
   private static final class Mobile {
      static final float[] DOLLAR = IconGlyphs.dollar(10);
      static final float[] EURO = IconGlyphs.euro(16);
      static final float[] BITCOIN = IconGlyphs.bitcoin(8, 24);

      private Mobile() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Sketch {
      private float[] data = new float[64];
      private int size;

      private Sketch() {
      }

      void quad(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
         if (this.size + 8 > this.data.length) {
            this.data = Arrays.copyOf(this.data, this.data.length * 2);
         }

         this.data[this.size++] = var1;
         this.data[this.size++] = var2;
         this.data[this.size++] = var3;
         this.data[this.size++] = var4;
         this.data[this.size++] = var5;
         this.data[this.size++] = var6;
         this.data[this.size++] = var7;
         this.data[this.size++] = var8;
      }

      void stroke(float var1, float... var2) {
         float[] var3 = new float[var2.length / 2];
         float[] var4 = new float[var2.length / 2];
         Arrays.fill(var3, -var1 * 0.5F);
         Arrays.fill(var4, var1 * 0.5F);
         this.band(var2, var3, var4);
      }

      void band(float[] var1, float[] var2, float[] var3) {
         int var5 = var1.length / 2;
         float[] var6 = new float[var5];
         float[] var7 = new float[var5];

         for (int var4 = 0; var4 < var5; var4++) {
            int var8 = Math.max(0, var4 - 1);
            int var9 = Math.min(var5 - 1, var4 + 1);
            float var10 = var1[var9 * 2] - var1[var8 * 2];
            float var11 = var1[var9 * 2 + 1] - var1[var8 * 2 + 1];
            float var12 = (float)Math.sqrt(var10 * var10 + var11 * var11);
            var6[var4] = -var11 / var12;
            var7[var4] = var10 / var12;
         }

         int var13 = 0;

         for (int var14 = 1; var14 < var5; var14++) {
            float var15 = var1[var13 * 2];
            float var16 = var1[var13 * 2 + 1];
            float var17 = var1[var14 * 2];
            float var18 = var1[var14 * 2 + 1];
            this.quad(
               var15 + var6[var13] * var2[var13],
               var16 + var7[var13] * var2[var13],
               var17 + var6[var14] * var2[var14],
               var18 + var7[var14] * var2[var14],
               var17 + var6[var14] * var3[var14],
               var18 + var7[var14] * var3[var14],
               var15 + var6[var13] * var3[var13],
               var16 + var7[var13] * var3[var13]
            );
            var13++;
         }
      }

      float[] build() {
         return Arrays.copyOf(this.data, this.size);
      }
   }
}
