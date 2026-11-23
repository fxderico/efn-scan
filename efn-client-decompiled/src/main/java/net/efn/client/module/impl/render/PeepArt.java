package net.efn.client.module.impl.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D.Float;
import java.awt.image.BufferedImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class PeepArt {
   static final int STREAK = 0;
   static final int DROP = 1;
   static final int PETAL = 2;
   static final int HEART_LEFT = 3;
   static final int HEART_RIGHT = 4;
   static final int FEATHER = 5;
   static final int WISP = 6;
   static final int BEAM = 7;
   static final int TWINKLE = 8;
   static final int ARC = 9;
   static final int MIST = 10;
   static final int BOLT = 11;
   static final int BOLT_FORK = 12;
   static final int HEART = 13;
   static final int CONE = 14;
   static final int PETAL_CURL = 15;
   static final int GLINT = 16;
   static final int COUNT = 17;
   static final float ARC_RADIUS = 0.421875F;
   static final float CONE_FOOT = 0.05F;
   static final float CONE_END = 0.9F;
   static final float FEATHER_ASPECT = 0.28846154F;

   private PeepArt() {
   }

   static PeepArt.Art[] drawAll() {
      PeepArt.Art[] var0 = new PeepArt.Art[17];
      var0[0] = streak();
      var0[1] = drop();
      var0[2] = petal(false);
      var0[15] = petal(true);
      PeepArt.Art[] var1 = heart();
      var0[13] = var1[0];
      var0[3] = var1[1];
      var0[4] = var1[2];
      var0[5] = feather();
      var0[6] = wisp();
      var0[7] = beam();
      var0[8] = twinkle();
      var0[9] = arc();
      var0[10] = mist();
      var0[11] = bolt(7, false);
      var0[12] = bolt(23, true);
      var0[14] = cone();
      var0[16] = glint();
      return var0;
   }

   static int[] rim(int[] is, int i, int j) {
      float[] var3 = new float[i * j];

      for (int var4 = 0; var4 < var3.length; var4++) {
         var3[var4] = (is[var4] >>> 24) / 255.0F;
      }

      float[] var11 = PeepSky.blur(var3, i, j, 3);
      float[] var5 = PeepSky.blur(var3, i, j, 7);
      float[] var6 = new float[var3.length];

      for (int var7 = 0; var7 < var6.length; var7++) {
         float var8 = MathHelper.clamp(var11[var7] * 2.2F - smooth(0.55F, 0.95F, var3[var7]) * 1.2F, 0.0F, 1.0F) * (1.0F - smooth(0.6F, 0.8F, var5[var7]));
         float var9 = var3[var7] * smooth(0.4F, 0.6F, var5[var7]) * (1.0F - smooth(0.85F, 0.97F, var5[var7]));
         float var10 = var3[var7] * (1.0F - smooth(0.45F, 0.6F, var5[var7]));
         var6[var7] = Math.max(var8, Math.max(var9, var10));
      }

      return white(var6);
   }

   static int[] gold(int[] is, int i, int j) {
      float[] var3 = new float[i * j];

      for (int var4 = 0; var4 < var3.length; var4++) {
         var3[var4] = smooth(0.3F, 0.65F, (is[var4] >>> 24) / 255.0F);
      }

      float[] var17 = PeepSky.blur(var3, i, j, 7);
      float[] var5 = PeepSky.blur(var3, i, j, 12);
      float[] var6 = PeepSky.blur(var3, i, j, 2);
      int[] var7 = new int[i * j];

      for (int var8 = 0; var8 < j; var8++) {
         for (int var9 = 0; var9 < i; var9++) {
            int var10 = var8 * i + var9;
            float var11 = var3[var10] * (1.0F - smooth(0.88F, 0.98F, var5[var10]));
            if (!((var11 = Math.max(var11, Math.min(1.0F, var6[var10] * 1.8F) * (1.0F - smooth(0.25F, 0.45F, var17[var10])))) <= 0.002F)) {
               float var12 = var6[Math.min(j - 1, var8 + 2) * i + Math.min(i - 1, var9 + 2)] - var6[Math.max(0, var8 - 2) * i + Math.max(0, var9 - 2)];
               float var13 = MathHelper.clamp(0.5F + 1.1F * var12, 0.0F, 1.0F);
               int var14 = Math.round(MathHelper.lerp(var13, 0.85F, 1.0F) * 255.0F);
               int var15 = Math.round(MathHelper.lerp(var13, 0.8F, 1.0F) * 255.0F);
               int var16 = Math.round(MathHelper.lerp(var13, 0.45F, 1.0F) * 255.0F);
               var7[var10] = Math.round(MathHelper.clamp(var11, 0.0F, 1.0F) * 255.0F) << 24 | var14 << 16 | var15 << 8 | var16;
            }
         }
      }

      return var7;
   }

   static int[] veil(int[] is, int i, int j) {
      float[] var3 = new float[i * j];

      for (int var4 = 0; var4 < var3.length; var4++) {
         var3[var4] = (is[var4] >>> 24) / 255.0F;
      }

      float[] var11 = PeepSky.blur(var3, i, j, 3);
      float[] var5 = PeepSky.blur(var3, i, j, 7);
      float[] var6 = PeepSky.blur(var3, i, j, 11);
      float[] var7 = new float[var3.length];

      for (int var8 = 0; var8 < var7.length; var8++) {
         float var9 = 1.0F - 0.85F * smooth(0.6F, 0.9F, var5[var8]);
         float var10 = Math.min(1.0F, var11[var8] * 1.15F) * var9;
         var7[var8] = Math.max(var10, Math.min(1.0F, var6[var8] * 2.2F) * 0.45F * (0.4F + 0.6F * var9));
      }

      return white(var7);
   }

   private static PeepArt.Art streak() {
      short var0 = 256;
      byte var1 = 32;
      float[] var2 = new float[var0 * var1];
      float var3 = 0.93F;

      for (int var4 = 0; var4 < var1; var4++) {
         for (int var5 = 0; var5 < var0; var5++) {
            float var6 = (var5 + 0.5F) / var0;
            float var7 = var4 + 0.5F - var1 * 0.5F;
            float var8 = var6 < var3 ? (float)Math.pow(var6 / var3, 2.6) : (float)Math.cos((var6 - var3) / (1.0F - var3) * 1.5707964F);
            float var9 = 0.7F + 2.6F * (float)Math.pow(var6, 1.6);
            float var10 = var8 * gauss(var7, var9);
            float var11 = 0.9F * gauss((float)Math.hypot(var5 + 0.5F - var0 * 0.9F, var7), 3.4F);
            var2[var4 * var0 + var5] = Math.min(1.0F, var10 + var11);
         }
      }

      return new PeepArt.Art(var0, var1, white(fadeBorder(var2, var0, var1, 2)));
   }

   private static PeepArt.Art drop() {
      byte var0 = 12;
      byte var1 = 96;
      float[] var2 = new float[var0 * var1];

      for (int var3 = 0; var3 < var1; var3++) {
         float var4 = (var3 + 0.5F) / var1;
         float var5 = (float)Math.pow(Math.sin(3.141592653589793 * var4), 0.7) * (0.3F + 0.7F * var4);

         for (int var6 = 0; var6 < var0; var6++) {
            var2[var3 * var0 + var6] = var5 * gauss(var6 + 0.5F - var0 * 0.5F, 1.15F);
         }
      }

      return new PeepArt.Art(var0, var1, white(fadeBorder(var2, var0, var1, 1)));
   }

   private static PeepArt.Art petal(boolean bl) {
      byte var1 = 60;
      byte var2 = 68;
      byte var3 = 6;
      Float var4 = new Float();
      if (bl) {
         var4.moveTo(0.52, 0.99);
         var4.curveTo(0.2, 0.84, 0.03, 0.52, 0.12, 0.22);
         var4.curveTo(0.17, 0.06, 0.31, 0.02, 0.41, 0.13);
         var4.curveTo(0.56, 0.25, 0.76, 0.21, 0.93, 0.07);
         var4.curveTo(1.0, 0.42, 0.85, 0.8, 0.52, 0.99);
      } else {
         var4.moveTo(0.46, 0.99);
         var4.curveTo(0.3, 0.9, 0.06, 0.62, 0.05, 0.38);
         var4.curveTo(0.04, 0.14, 0.24, 0.02, 0.45, 0.04);
         var4.curveTo(0.55, 0.05, 0.6, 0.075, 0.64, 0.07);
         var4.curveTo(0.8, 0.05, 0.97, 0.18, 0.95, 0.4);
         var4.curveTo(0.93, 0.62, 0.7, 0.88, 0.54, 0.99);
      }

      var4.closePath();
      float[] var5 = fill(var4, var1, var2, var3);
      float[] var6 = PeepSky.blur(var5, var1, var2, 1);
      float[] var7 = PeepSky.blur(var5, var1, var2, 4);
      int[] var8 = new int[var1 * var2];

      for (int var9 = 0; var9 < var2; var9++) {
         for (int var10 = 0; var10 < var1; var10++) {
            int var11 = var9 * var1 + var10;
            float var12 = var6[var11];
            if (!(var12 <= 0.002F)) {
               float var13 = (var10 + 0.5F - var3) / (var1 - var3 * 2.0F);
               float var14 = (var9 + 0.5F - var3) / (var2 - var3 * 2.0F);
               float var15 = (float)Math.hypot((var13 - 0.5F) * 1.2F, var14 - 0.42F) / 0.6F;
               int var16 = mix(16763356, 16088488, smooth(0.08F, 0.75F, var15));
               var16 = mix(var16, 13582964, smooth(0.7F, 1.15F, var15) * 0.65F);
               float var17 = smooth(0.64F, 1.0F, var14);
               var16 = mix(var16, 11020378, var17 * 0.5F);
               float var18 = (float)Math.atan2(var13 - 0.5F, 1.0F - var14);
               float var19 = (float)Math.pow(Math.abs(Math.cos(var18 * 11.0F)), 24.0) * (1.0F - var17) * smooth(0.1F, 0.5F, 1.0F - var14);
               var16 = mix(var16, 16768232, var19 * 0.2F);
               float var20 = (1.0F - smooth(0.55F, 0.9F, var7[var11])) * (1.0F - smooth(0.4F, 0.9F, var14));
               var16 = mix(var16, 16758996, var20 * 0.55F);
               if (bl) {
                  float var21 = (1.0F - smooth(0.5F, 0.85F, var7[var11])) * (1.0F - smooth(0.16F, 0.4F, var14));
                  float var22 = smooth(0.14F, 0.26F, var14) * (1.0F - smooth(0.26F, 0.46F, var14)) * smooth(0.3F, 0.8F, var7[var11]);
                  var16 = mix(mix(var16, 16767462, var21 * 0.7F), 11546719, var22 * 0.35F);
               }

               var8[var11] = ColorHelper.withAlpha(MathHelper.clamp(var12 * 0.95F, 0.0F, 1.0F), var16);
            }
         }
      }

      return new PeepArt.Art(var1, var2, var8);
   }

   private static PeepArt.Art[] heart() {
      short var0 = 128;
      byte var1 = 120;
      byte var2 = 16;
      Float var3 = new Float();
      var3.moveTo(0.5, 0.95);
      var3.curveTo(0.13, 0.68, 0.0, 0.44, 0.08, 0.24);
      var3.curveTo(0.17, 0.04, 0.42, 0.03, 0.5, 0.24);
      var3.curveTo(0.58, 0.03, 0.83, 0.04, 0.92, 0.24);
      var3.curveTo(1.0, 0.44, 0.87, 0.68, 0.5, 0.95);
      var3.closePath();
      float[] var4 = fill(var3, var0, var1, var2);
      float[][] var5 = new float[][]{var4, new float[var0 * var1], new float[var0 * var1]};

      for (int var6 = 0; var6 < var1; var6++) {
         float var7 = var0 * 0.5F + crack((var6 + 0.5F - var2) / (var1 - var2 * 2.0F)) * (var0 - var2 * 2.0F);

         for (int var8 = 0; var8 < var0; var8++) {
            var5[var8 + 0.5F < var7 ? 1 : 2][var6 * var0 + var8] = var4[var6 * var0 + var8];
         }
      }

      PeepArt.Art[] var26 = new PeepArt.Art[var5.length];

      for (int var27 = 0; var27 < var5.length; var27++) {
         float[] var28 = var5[var27];
         float[] var9 = PeepSky.blur(var28, var0, var1, 6);
         float[] var10 = PeepSky.blur(var28, var0, var1, 7);
         int[] var11 = new int[var0 * var1];

         for (int var12 = 0; var12 < var1; var12++) {
            for (int var13 = 0; var13 < var0; var13++) {
               int var15 = var12 * var0 + var13;
               float var16 = (var13 + 0.5F - var2) / (var0 - var2 * 2.0F);
               float var17 = (var12 + 0.5F - var2) / (var1 - var2 * 2.0F);
               float var18 = var28[var15];
               float var19 = MathHelper.clamp((var18 - var9[var15]) * 2.6F, 0.0F, 1.0F) * var18;
               float var20 = gauss((float)Math.hypot(var16 - 0.3F, var17 - 0.27F), 0.09F) * var18;
               int var21 = mix(12720235, 16732080, 0.25F + var20 * 0.5F);
               float var22 = Math.max(var18 * (0.16F + 0.1F * (1.0F - var17)), var19);
               float var23 = var22 + (1.0F - var18) * Math.min(1.0F, var10[var15] * 1.5F) * 0.65F;
               if (!(var23 <= 0.002F)) {
                  int var24 = mix(16751320, 16770806, smooth(0.75F, 1.0F, var19) * 0.45F);
                  int var25 = mix(16732080, mix(var21, var24, var19 / Math.max(var22, 1.0E-4F)), var22 / var23);
                  var11[var15] = ColorHelper.withAlpha(MathHelper.clamp(var23, 0.0F, 1.0F), var25);
               }
            }
         }

         var26[var27] = new PeepArt.Art(var0, var1, var11);
      }

      return var26;
   }

   static float crack(float f) {
      float[] var1 = new float[]{0.2F, 0.34F, 0.47F, 0.6F, 0.73F, 0.86F, 1.0F};
      float[] var2 = new float[]{0.0F, -0.05F, 0.045F, -0.04F, 0.04F, -0.025F, 0.0F};
      if (f <= var1[0]) {
         return 0.0F;
      } else {
         for (int var3 = 1; var3 < var1.length; var3++) {
            if (f <= var1[var3]) {
               return MathHelper.lerp((f - var1[var3 - 1]) / (var1[var3] - var1[var3 - 1]), var2[var3 - 1], var2[var3]);
            }
         }

         return 0.0F;
      }
   }

   private static PeepArt.Art feather() {
      int var0 = Math.round(60.0F);
      short var1 = 208;
      BufferedImage var2 = new BufferedImage(var0, var1, 2);
      Graphics2D var3 = smooth(var2);
      java.awt.geom.QuadCurve2D.Float var4 = new java.awt.geom.QuadCurve2D.Float(
         var0 * 0.42F, var1 * 0.97F, var0 * 0.56F, var1 * 0.5F, var0 * 0.44F, var1 * 0.035F
      );
      byte var5 = 120;

      for (int var6 = 0; var6 < var5; var6++) {
         float var7 = 0.06F + 0.92F * var6 / (var5 - 1.0F);
         float var8 = quad(var4.x1, var4.ctrlx, var4.x2, var7);
         float var9 = quad(var4.y1, var4.ctrly, var4.y2, var7);
         float var10 = quadSlope(var4.x1, var4.ctrlx, var4.x2, var7);
         float var11 = quadSlope(var4.y1, var4.ctrly, var4.y2, var7);
         float var12 = (float)Math.hypot(var10, var11);
         var10 /= var12;
         var11 /= var12;
         float var13 = (float)Math.pow(Math.sin(3.141592653589793 * Math.min(1.0F, (var7 - 0.02F) / 0.97F)), 0.75);
         boolean var14 = var7 < 0.3F;

         for (byte var15 = -1; var15 <= 1; var15 += 2) {
            float var16 = PeepSky.hash01(var6 * 7 + var15 * 3 + 11);
            if ((var14 || !(var16 < 0.15F)) && (var15 != 1 || var6 < 50 || var6 > 53) && (var15 != -1 || var6 < 82 || var6 > 85)) {
               float var17 = var0 * (var14 ? 0.36F : 0.4F) * var13 * (0.9F + 0.1F * var16) * (var15 < 0 ? 0.7F : 1.05F);
               float var18 = (var14 ? 0.55F : 0.95F) * var15;
               float var19 = var10 * (float)Math.cos(var18) - var11 * (float)Math.sin(var18);
               float var20 = var10 * (float)Math.sin(var18) + var11 * (float)Math.cos(var18);
               float var21 = var14 ? 0.35F : 0.12F;
               var3.setColor(new Color(1.0F, 1.0F, 1.0F, var14 ? 0.32F : 0.72F));
               var3.setStroke(new BasicStroke(var14 ? 0.9F : 1.05F, 1, 1));
               var3.draw(
                  new java.awt.geom.QuadCurve2D.Float(
                     var8,
                     var9,
                     var8 + var19 * var17 * 0.55F,
                     var9 + var20 * var17 * 0.55F,
                     var8 + var19 * var17 - var15 * var11 * var17 * var21 * 0.0F + var10 * var17 * var21,
                     var9 + var20 * var17 + var11 * var17 * var21
                  )
               );
            }
         }
      }

      var3.dispose();
      BufferedImage var23 = new BufferedImage(var0, var1, 2);
      var3 = smooth(var23);
      var3.setColor(new Color(1.0F, 1.0F, 1.0F, 1.0F));
      var3.setStroke(new BasicStroke(1.7F, 1, 1));
      var3.draw(var4);
      var3.dispose();
      float[] var24 = alpha(var2);
      float[] var25 = alpha(var23);
      float[] var26 = new float[var24.length];

      for (int var28 = 0; var28 < var26.length; var28++) {
         var26[var28] = var25[var28] + var24[var28] * (1.0F - var25[var28]);
      }

      float[] var29 = PeepSky.blur(var26, var0, var1, 1);

      for (int var31 = 0; var31 < var26.length; var31++) {
         var26[var31] = Math.min(1.0F, var26[var31] * 0.8F + var29[var31] * 0.45F);
      }

      fadeBorder(var26, var0, var1, 2);
      float[] var32 = PeepSky.blur(var25, var0, var1, 1);
      int[] var33 = new int[var0 * var1];

      for (int var34 = 0; var34 < var33.length; var34++) {
         var33[var34] = ColorHelper.withAlpha(MathHelper.clamp(var26[var34], 0.0F, 1.0F), mix(16764134, 16777215, Math.min(1.0F, var32[var34] * 1.6F)));
      }

      return new PeepArt.Art(var0, var1, var33);
   }

   private static PeepArt.Art wisp() {
      byte var0 = 96;
      short var1 = 256;
      float[] var2 = new float[var0 * var1];

      for (int var3 = 0; var3 < var1; var3++) {
         float var4 = (var3 + 0.5F) / var1;
         float var5 = smooth(0.0F, 0.2F, var4) * (float)Math.pow(1.0F - var4, 1.5);
         float var6 = (float)Math.pow(var4, 1.2);
         float var7 = 0.5F + 0.13F * (float)Math.sin(var4 * 6.0F + 0.4F) * var6;
         float var8 = 0.5F + 0.13F * (float)Math.sin(var4 * 6.0F + 2.3F) * var6;
         float var9 = var0 * (0.12F * (float)Math.pow(1.0F - var4, 0.8) + 0.018F);

         for (int var10 = 0; var10 < var0; var10++) {
            float var11 = (var10 + 0.5F) / var0;
            float var12 = gauss((var11 - var7) * var0, var9);
            float var13 = 0.5F * gauss((var11 - var8) * var0, var9 * 0.6F) * smooth(0.22F, 0.5F, var4);
            float var14 = 0.55F * gauss((var11 - var7) * var0, var9 * 0.35F) * (1.0F - var4);
            var2[var3 * var0 + var10] = Math.min(1.0F, var5 * (0.7F * var12 + var13 + var14));
         }
      }

      return new PeepArt.Art(var0, var1, white(fadeBorder(PeepSky.blur(var2, var0, var1, 3), var0, var1, 3)));
   }

   private static PeepArt.Art beam() {
      short var0 = 128;
      short var1 = 512;
      float[] var2 = new float[var0 * var1];

      for (int var3 = 0; var3 < var1; var3++) {
         float var4 = 1.0F - (var3 + 0.5F) / var1;
         float var5 = smooth(0.0F, 0.09F, var4) * (float)Math.pow(1.0F - var4, 1.25);
         float var6 = 1.6F + 27.0F * var4;
         float var7 = 0.7F + 3.2F * var4;

         for (int var8 = 0; var8 < var0; var8++) {
            float var9 = var8 + 0.5F - var0 * 0.5F;
            var2[var3 * var0 + var8] = Math.min(1.0F, var5 * (0.7F * gauss(var9, var6) + 0.45F * gauss(var9, var7) * (1.0F - var4)));
         }
      }

      return new PeepArt.Art(var0, var1, white(fadeBorder(var2, var0, var1, 2)));
   }

   private static PeepArt.Art cone() {
      short var0 = 256;
      short var1 = 512;
      float[] var2 = new float[var0 * var1];

      for (int var3 = 0; var3 < var1; var3++) {
         float var4 = 1.0F - (var3 + 0.5F) / var1;
         float var5 = smooth(0.0F, 0.035F, var4) * (1.0F - smooth(0.3F, 1.0F, var4)) * (0.6F + 0.4F * (1.0F - var4));
         float var6 = (0.05F + 0.84999996F * var4) * var0 * 0.5F;

         for (int var7 = 0; var7 < var0; var7++) {
            float var8 = (var7 + 0.5F - var0 * 0.5F) / var6;
            float var9 = Math.abs(var8);
            if (!(var9 >= 1.2F)) {
               float var10 = 1.0F - smooth(0.5F, 1.05F, var9);
               float var11 = 0.35F * gauss(var9, 0.2F);
               float var12 = 0.88F + 0.12F * valueNoise((var8 + 1.2F) * 3.5F, var4 * 2.2F, 64, 1);
               var2[var3 * var0 + var7] = Math.min(1.0F, var5 * (0.7F * var10 + var11) * var12);
            }
         }
      }

      return new PeepArt.Art(var0, var1, white(fadeBorder(var2, var0, var1, 2)));
   }

   private static PeepArt.Art glint() {
      byte var0 = 32;
      byte var1 = 32;
      float[] var2 = new float[var0 * var1];

      for (int var3 = 0; var3 < var1; var3++) {
         for (int var4 = 0; var4 < var0; var4++) {
            float var5 = var4 + 0.5F - var0 * 0.5F;
            float var6 = var3 + 0.5F - var1 * 0.5F;
            float var7 = (float)Math.hypot(var5, var6);
            float var8 = glintRay(var5, var6) + glintRay(var6, var5);
            float var9 = gauss(var7, 1.6F) + 0.4F * gauss(var7, 4.0F);
            var2[var3 * var0 + var4] = Math.min(1.0F, var8 + var9);
         }
      }

      return new PeepArt.Art(var0, var1, white(fadeBorder(var2, var0, var1, 1)));
   }

   private static float glintRay(float f, float g) {
      float var2 = Math.max(0.0F, 1.0F - Math.abs(f) / 15.0F);
      return gauss(g, 0.7F + 0.9F * var2) * var2 * var2 * 0.9F;
   }

   private static PeepArt.Art twinkle() {
      byte var0 = 64;
      byte var1 = 64;
      float[] var2 = new float[var0 * var1];

      for (int var3 = 0; var3 < var1; var3++) {
         for (int var4 = 0; var4 < var0; var4++) {
            float var5 = var4 + 0.5F - var0 * 0.5F;
            float var6 = var3 + 0.5F - var1 * 0.5F;
            float var7 = ray(var5, var6, 29.0F) + ray(var6, var5, 29.0F);
            float var8 = 0.35F * (ray((var5 + var6) * 0.7071F, (var5 - var6) * 0.7071F, 14.0F) + ray((var5 - var6) * 0.7071F, (var5 + var6) * 0.7071F, 14.0F));
            float var9 = gauss((float)Math.hypot(var5, var6), 2.6F) + 0.35F * gauss((float)Math.hypot(var5, var6), 7.0F);
            var2[var3 * var0 + var4] = Math.min(1.0F, var7 + var8 + var9);
         }
      }

      return new PeepArt.Art(var0, var1, white(fadeBorder(var2, var0, var1, 2)));
   }

   private static PeepArt.Art arc() {
      short var0 = 256;
      short var1 = 256;
      float var2 = 0.421875F * var0;
      float var3 = 2.6F;
      float[] var4 = new float[var0 * var1];

      for (int var5 = 0; var5 < var1; var5++) {
         for (int var6 = 0; var6 < var0; var6++) {
            float var8 = var6 + 0.5F - var0 * 0.5F;
            float var9 = var5 + 0.5F - var1 * 0.5F;
            float var10 = (float)Math.hypot(var8, var9);
            float var11 = (float)(-Math.atan2(var9, var8));
            if (var11 < 0.0F) {
               var11 += 6.2831855F;
            }

            float var7 = var11 < 0.06F ? 1.0F : (var11 > var3 ? 0.0F : (float)Math.pow(1.0F - var11 / var3, 1.6));
            if (var11 > 6.2031856F) {
               var7 = (float)Math.cos((6.2831855F - var11) / 0.08F * 1.5707964F);
            }

            float var12 = gauss(var10 - var2, 1.3F) + 0.3F * gauss(var10 - var2, 4.5F);
            var4[var5 * var0 + var6] = Math.min(1.0F, var12 * var7);
         }
      }

      return new PeepArt.Art(var0, var1, white(fadeBorder(var4, var0, var1, 2)));
   }

   private static PeepArt.Art mist() {
      short var1 = 512;
      byte var2 = 96;
      float[] var3 = new float[var1 * var2];

      for (int var4 = 0; var4 < var2; var4++) {
         float var5 = (var4 + 0.5F) / var2;
         float var6 = (float)Math.pow(Math.sin(3.141592653589793 * var5), 1.4) * (0.55F + 0.45F * var5);

         for (int var0 = 0; var0 < var1; var0++) {
            float var7 = 0.0F;
            float var8 = 0.55F;
            float var9 = 0.0F;

            for (int var10 = 0; var10 < 4; var10++) {
               int var11 = 4 << var10;
               var7 += var8 * valueNoise((float)var0 / var1 * var11, var5 * (2.5F + var10 * 2.5F) + var10 * 7.3F, var11, var10);
               var9 += var8;
               var8 *= 0.5F;
            }

            float var16;
            var3[var4 * var1 + var0] = var6 * smooth(0.28F, 0.78F, var16 = var7 / var9);
         }
      }

      float[] var13 = PeepSky.blur(var3, var1, var2, 1);

      for (int var14 = 0; var14 < var2; var14++) {
         float var15 = smooth(0.0F, 3.0F, var14) * smooth(0.0F, 3.0F, var2 - 1 - var14);

         for (int var12 = 0; var12 < var1; var12++) {
            int var17 = var14 * var1 + var12;
            var13[var17] *= var15;
         }
      }

      return new PeepArt.Art(var1, var2, white(var13));
   }

   private static PeepArt.Art bolt(int i, boolean bl) {
      short var2 = 128;
      short var3 = 256;
      BufferedImage var4 = new BufferedImage(var2, var3, 2);
      Graphics2D var5 = smooth(var4);
      var5.setColor(Color.WHITE);
      float[] var6 = new float[33];
      float[] var7 = new float[33];
      jag(var6, var7, var2 * 0.5F, 12.0F, var2 * 0.5F + (PeepSky.hash01(i) - 0.5F) * 30.0F, var3 - 12.0F, i);
      var5.setStroke(new BasicStroke(2.6F, 1, 1));
      var5.draw(polyline(var6, var7));
      float[] var8 = new float[17];
      float[] var9 = new float[17];
      int var10 = bl ? 2 : 1;

      for (int var11 = 0; var11 < var10; var11++) {
         int var12 = 9 + var11 * 8;
         float var13 = (var11 % 2 == 0 ? 1.0F : -1.0F) * (PeepSky.hash01(i + var11) < 0.5F ? 1.0F : -1.0F);
         jag(var8, var9, var6[var12], var7[var12], var6[var12] + var13 * (26.0F + 14.0F * var11), var7[var12] + 58.0F - 10.0F * var11, i * 3 + 1 + var11);
         var5.setStroke(new BasicStroke(1.5F - 0.3F * var11, 1, 1));
         var5.draw(polyline(var8, var9));
      }

      var5.dispose();
      float[] var16 = alpha(var4);

      for (int var17 = 0; var17 < var3; var17++) {
         float var19 = smooth(12.0F, 44.0F, var17);

         for (int var14 = 0; var14 < var2; var14++) {
            int var15 = var17 * var2 + var14;
            var16[var15] *= var19;
         }
      }

      float[] var18 = PeepSky.blur(var16, var2, var3, 6);
      int[] var20 = new int[var2 * var3];

      for (int var21 = 0; var21 < var20.length; var21++) {
         float var22 = Math.min(1.0F, var18[var21] * 3.0F);
         var20[var21] = ColorHelper.withAlpha(MathHelper.clamp(Math.max(var16[var21], var22 * 0.7F), 0.0F, 1.0F), mix(16732080, 16774395, var16[var21]));
      }

      return new PeepArt.Art(var2, var3, var20);
   }

   private static void jag(float[] fs, float[] gs, float f, float g, float h, float i, int j) {
      int var7 = fs.length - 1;
      fs[0] = f;
      gs[0] = g;
      fs[var7] = h;
      gs[var7] = i;
      float var8 = (float)Math.hypot(h - f, i - g) * 0.32F;

      for (int var9 = var7; var9 > 1; var9 /= 2) {
         for (int var10 = 0; var10 + var9 <= var7; var10 += var9) {
            int var11 = var10 + var9 / 2;
            fs[var11] = (fs[var10] + fs[var10 + var9]) * 0.5F + (PeepSky.hash01(j * 131 + var11 * 17 + var9) - 0.5F) * var8;
            float var12 = (gs[var10] + gs[var10 + var9]) * 0.5F + (PeepSky.hash01(j * 151 + var11 * 13 + var9) - 0.5F) * var8 * 0.25F;
            float var13 = gs[var10] + 0.3F * (gs[var10 + var9] - gs[var10]);
            float var14 = gs[var10] + 0.7F * (gs[var10 + var9] - gs[var10]);
            gs[var11] = MathHelper.clamp(var12, Math.min(var13, var14), Math.max(var13, var14));
         }

         var8 *= 0.62F;
      }
   }

   private static Float polyline(float[] fs, float[] gs) {
      Float var2 = new Float();
      var2.moveTo(fs[0], gs[0]);

      for (int var3 = 1; var3 < fs.length; var3++) {
         var2.lineTo(fs[var3], gs[var3]);
      }

      return var2;
   }

   private static float ray(float f, float g, float h) {
      float var3 = Math.max(0.0F, 1.0F - Math.abs(f) / h);
      return gauss(g, 0.85F + 0.6F * var3) * var3 * var3;
   }

   private static float valueNoise(float f, float g, int i, int j) {
      int var4 = (int)Math.floor(f);
      int var5 = (int)Math.floor(g);
      float var6 = f - var4;
      float var7 = g - var5;
      var6 = var6 * var6 * (3.0F - 2.0F * var6);
      var7 = var7 * var7 * (3.0F - 2.0F * var7);
      float var8 = lattice(var4, var5, i, j);
      float var9 = lattice(var4 + 1, var5, i, j);
      float var10 = lattice(var4, var5 + 1, i, j);
      float var11 = lattice(var4 + 1, var5 + 1, i, j);
      return MathHelper.lerp(var7, MathHelper.lerp(var6, var8, var9), MathHelper.lerp(var6, var10, var11));
   }

   private static float lattice(int i, int j, int k, int l) {
      return PeepSky.hash01(Math.floorMod(i, k) * 7919 + j * 104729 + l * 15485863);
   }

   private static float[] fill(Float float_, int i, int j, int k) {
      BufferedImage var4 = new BufferedImage(i, j, 2);
      Graphics2D var5 = smooth(var4);
      var5.translate(k, k);
      var5.scale(i - k * 2.0, j - k * 2.0);
      var5.setColor(Color.WHITE);
      var5.fill(float_);
      var5.dispose();
      return alpha(var4);
   }

   private static Graphics2D smooth(BufferedImage bufferedImage) {
      Graphics2D var1 = bufferedImage.createGraphics();
      var1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      var1.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      var1.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      return var1;
   }

   private static float[] alpha(BufferedImage bufferedImage) {
      int var1 = bufferedImage.getWidth();
      int var2 = bufferedImage.getHeight();
      int[] var3 = bufferedImage.getRGB(0, 0, var1, var2, null, 0, var1);
      float[] var4 = new float[var3.length];

      for (int var5 = 0; var5 < var4.length; var5++) {
         var4[var5] = (var3[var5] >>> 24) / 255.0F;
      }

      return var4;
   }

   private static float[] fadeBorder(float[] fs, int i, int j, int k) {
      for (int var4 = 0; var4 < j; var4++) {
         for (int var5 = 0; var5 < i; var5++) {
            int var6 = Math.min(Math.min(var5, i - 1 - var5), Math.min(var4, j - 1 - var4));
            if (var6 < k) {
               int var7 = var4 * i + var5;
               fs[var7] *= (float)var6 / k;
            }
         }
      }

      return fs;
   }

   private static int[] white(float[] fs) {
      int[] var1 = new int[fs.length];

      for (int var2 = 0; var2 < fs.length; var2++) {
         var1[var2] = Math.round(MathHelper.clamp(fs[var2], 0.0F, 1.0F) * 255.0F) << 24 | 16777215;
      }

      return var1;
   }

   private static float quad(float f, float g, float h, float i) {
      float var4 = 1.0F - i;
      return var4 * var4 * f + 2.0F * var4 * i * g + i * i * h;
   }

   private static float quadSlope(float f, float g, float h, float i) {
      return 2.0F * (1.0F - i) * (g - f) + 2.0F * i * (h - g);
   }

   private static float gauss(float f, float g) {
      return (float)Math.exp(-f * f / (2.0F * g * g));
   }

   private static float smooth(float f, float g, float h) {
      float var3 = MathHelper.clamp((h - f) / (g - f), 0.0F, 1.0F);
      return var3 * var3 * (3.0F - 2.0F * var3);
   }

   private static int mix(int i, int j, float f) {
      return ColorHelper.lerp(MathHelper.clamp(f, 0.0F, 1.0F), i, j) & 16777215;
   }

   @Environment(EnvType.CLIENT)
   record Art(int width, int height, int[] argb) {
   }
}
