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

   private static void sq(IconGlyphs.Sketch sketch, float f, float g, float h, float i, float j, float k, float l, float m) {
      float var9 = 0.16F;
      sketch.quad(f - var9 * g, g, h - var9 * i, i, j - var9 * k, k, l - var9 * m, m);
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

   static float[] mobile(float[] fs) {
      if (fs == DOLLAR) {
         return IconGlyphs.Mobile.DOLLAR;
      } else if (fs == EURO) {
         return IconGlyphs.Mobile.EURO;
      } else {
         return fs == BITCOIN ? IconGlyphs.Mobile.BITCOIN : fs;
      }
   }

   private static float[] dollar(int i) {
      float var1 = 0.34F;
      float[] var2 = arc(0.0F, var1, var1, 28.0F, 270.0F, i);
      float[] var3 = arc(0.0F, -var1, var1, 90.0F, -152.0F, i);
      IconGlyphs.Sketch var4 = new IconGlyphs.Sketch();
      var4.stroke(0.17F, join(var2, Arrays.copyOfRange(var3, 2, var3.length)));
      var4.quad(-0.055F, -0.98F, 0.055F, -0.98F, 0.055F, 0.98F, -0.055F, 0.98F);
      return var4.build();
   }

   private static float[] euro(int i) {
      IconGlyphs.Sketch var1 = new IconGlyphs.Sketch();
      var1.stroke(0.17F, arc(0.1F, 0.0F, 0.62F, 48.0F, 312.0F, i));
      var1.quad(-0.82F, 0.06F, 0.16F, 0.06F, 0.16F, 0.2F, -0.82F, 0.2F);
      var1.quad(-0.82F, -0.22F, 0.1F, -0.22F, 0.1F, -0.08F, -0.82F, -0.08F);
      return var1.build();
   }

   private static float[] bitcoin(int i, int j) {
      IconGlyphs.Sketch var2 = new IconGlyphs.Sketch();
      var2.quad(-0.3F, -0.56F, -0.17F, -0.56F, -0.17F, 0.56F, -0.3F, 0.56F);
      var2.stroke(0.13F, join(new float[]{-0.24F, 0.5F}, arc(0.06F, 0.27F, 0.23F, 90.0F, -90.0F, i), new float[]{-0.24F, 0.04F}));
      var2.stroke(0.13F, join(new float[]{-0.24F, 0.04F}, arc(0.1F, -0.24F, 0.28F, 90.0F, -90.0F, i), new float[]{-0.24F, -0.52F}));
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
      var9.stroke(0.1F, arc(0.0F, 0.0F, 0.92F, 0.0F, 360.0F, j));
      return join(var9.build(), var3);
   }

   private static float[] arc(float f, float g, float h, float i, float j, int k) {
      float[] var6 = new float[(k + 1) * 2];

      for (int var7 = 0; var7 <= k; var7++) {
         double var8 = Math.toRadians(i + (j - i) * var7 / k);
         var6[var7 * 2] = f + h * (float)Math.cos(var8);
         var6[var7 * 2 + 1] = g + h * (float)Math.sin(var8);
      }

      return var6;
   }

   private static float[] join(float[]... fs) {
      int var1 = 0;

      for (float[] var5 : fs) {
         var1 += var5.length;
      }

      float[] var8 = new float[var1];
      int var9 = 0;

      for (float[] var7 : fs) {
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

      void quad(float f, float g, float h, float i, float j, float k, float l, float m) {
         if (this.size + 8 > this.data.length) {
            this.data = Arrays.copyOf(this.data, this.data.length * 2);
         }

         this.data[this.size++] = f;
         this.data[this.size++] = g;
         this.data[this.size++] = h;
         this.data[this.size++] = i;
         this.data[this.size++] = j;
         this.data[this.size++] = k;
         this.data[this.size++] = l;
         this.data[this.size++] = m;
      }

      void stroke(float f, float... fs) {
         float[] var3 = new float[fs.length / 2];
         float[] var4 = new float[fs.length / 2];
         Arrays.fill(var3, -f * 0.5F);
         Arrays.fill(var4, f * 0.5F);
         this.band(fs, var3, var4);
      }

      void band(float[] fs, float[] gs, float[] hs) {
         int var5 = fs.length / 2;
         float[] var6 = new float[var5];
         float[] var7 = new float[var5];

         for (int var4 = 0; var4 < var5; var4++) {
            int var8 = Math.max(0, var4 - 1);
            int var9 = Math.min(var5 - 1, var4 + 1);
            float var10 = fs[var9 * 2] - fs[var8 * 2];
            float var11 = fs[var9 * 2 + 1] - fs[var8 * 2 + 1];
            float var12 = (float)Math.sqrt(var10 * var10 + var11 * var11);
            var6[var4] = -var11 / var12;
            var7[var4] = var10 / var12;
         }

         int var13 = 0;

         for (int var14 = 1; var14 < var5; var14++) {
            float var15 = fs[var13 * 2];
            float var16 = fs[var13 * 2 + 1];
            float var17 = fs[var14 * 2];
            float var18 = fs[var14 * 2 + 1];
            this.quad(
               var15 + var6[var13] * gs[var13],
               var16 + var7[var13] * gs[var13],
               var17 + var6[var14] * gs[var14],
               var18 + var7[var14] * gs[var14],
               var17 + var6[var14] * hs[var14],
               var18 + var7[var14] * hs[var14],
               var15 + var6[var13] * hs[var13],
               var16 + var7[var13] * hs[var13]
            );
            var13++;
         }
      }

      float[] build() {
         return Arrays.copyOf(this.data, this.size);
      }
   }
}
