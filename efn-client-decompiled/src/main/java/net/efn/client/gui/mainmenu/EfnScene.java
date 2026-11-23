package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EfnScene {
   private static final float SLANT = 0.25F;
   private static final int BEAMS = 12;
   private static final int BEAM_SEGMENTS = 10;
   private static final int MOTES = 90;

   private EfnScene() {
   }

   public static int rgba(int var0, int var1, int var2, float var3) {
      return Math.round(Math.max(0.0F, Math.min(1.0F, var3)) * 255.0F) << 24 | var2 << 16 | var1 << 8 | var0;
   }

   public static int mix(int var0, int var1, float var2) {
      var2 = Math.max(0.0F, Math.min(1.0F, var2));
      int var3 = Math.round((var0 & 0xFF) + ((var1 & 0xFF) - (var0 & 0xFF)) * var2);
      int var4 = Math.round((var0 >> 8 & 0xFF) + ((var1 >> 8 & 0xFF) - (var0 >> 8 & 0xFF)) * var2);
      int var5 = Math.round((var0 >> 16 & 0xFF) + ((var1 >> 16 & 0xFF) - (var0 >> 16 & 0xFF)) * var2);
      return 0xFF000000 | var5 << 16 | var4 << 8 | var3;
   }

   public static float rnd(int var0, int var1) {
      int var2 = var0 * 374761393 + var1 * 668265263;
      var2 = (var2 ^ var2 >>> 13) * 1274126177;
      var2 ^= var2 >>> 16;
      return (var2 & 16777215) / 1.6777216E7F;
   }

   private static float frac(float var0) {
      return var0 - (float)Math.floor(var0);
   }

   public static void glow(ImDrawList var0, float var1, float var2, float var3, int var4, float var5) {
      byte var6 = 28;

      for (int var7 = 0; var7 < var6; var7++) {
         float var8 = (float)var7 / var6;
         float var9 = var3 * (1.0F - var8);
         float var10 = var5 / var6 * (0.5F + 1.6F * var8);
         var0.addCircleFilled(var1, var2, var9, EfnLogo.alpha(var4, var10), 48);
      }
   }

   public static void draw(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7, float var8) {
      int var9 = rgba(7, 7, 10, var8);
      int var10 = rgba(12, 14, 19, var8);
      var0.addRectFilledMultiColor(0.0F, 0.0F, var1, var2, var9, var9, var10, var10);
      int var11 = mix(var7, -41923, 0.55F);
      float var12 = Math.max(var1, var2);
      glow(
         var0,
         var1 * (0.24F + 0.05F * (float)Math.sin(var4 * 0.11F)) - var5 * 26.0F * var3,
         var2 * (0.3F + 0.05F * (float)Math.cos(var4 * 0.13F)) - var6 * 14.0F * var3,
         var12 * 0.55F,
         var7,
         0.1F * var8
      );
      glow(
         var0,
         var1 * (0.8F + 0.04F * (float)Math.cos(var4 * 0.09F)) - var5 * 34.0F * var3,
         var2 * (0.74F + 0.05F * (float)Math.sin(var4 * 0.1F)) - var6 * 18.0F * var3,
         var12 * 0.5F,
         var11,
         0.09F * var8
      );
      ghost(var0, var1, var2, var3, var4, var5, var6, var8);
      beams(var0, var1, var2, var3, var4, var5, var7, var8);
      streak(var0, var1, var2, var3, var4, var7, var8);
      motes(var0, var1, var2, var3, var4, var5, var6, var7, var8);
      vignette(var0, var1, var2, var8);
   }

   private static void ghost(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = var2 * 1.25F;
      float var9 = var1 * 0.66F - var5 * 30.0F * var3;
      float var10 = var2 * 0.5F - var8 * 0.5F + (float)Math.sin(var4 * 0.2F) * 10.0F * var3 - var6 * 16.0F * var3;
      EfnLogo.mark(var0, var9, var10, var8, rgba(255, 255, 255, 0.04F * var7));
   }

   private static void beams(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, int var6, float var7) {
      for (int var8 = 0; var8 < 12; var8++) {
         float var9 = 0.4F + 0.6F * rnd(var8, 1);
         float var10 = 0.004F + 0.01F * rnd(var8, 2);
         float var11 = (24.0F + 90.0F * rnd(var8, 3)) * var3 * var9;
         float var12 = (frac(rnd(var8, 4) + var4 * var10) * 1.5F - 0.25F) * var1 - var5 * 40.0F * var3 * var9;
         float var13 = 0.55F + 0.45F * (float)Math.sin(var4 * (0.25F + 0.4F * rnd(var8, 5)) + rnd(var8, 6) * 6.28F);
         float var14 = (0.045F + 0.075F * rnd(var8, 7)) * var13 * var7;
         int var15 = var8 % 3 == 0 ? var6 : -1;

         for (int var16 = 0; var16 < 10; var16++) {
            float var17 = var2 * var16 / 10.0F;
            float var18 = var2 * (var16 + 1) / 10.0F + 1.0F;
            float var19 = (float)Math.sin(3.141592653589793 * (var16 + 0.5) / 10.0);
            int var20 = EfnLogo.alpha(var15, var14 * var19 * var19);
            float var21 = var12 + (var2 - var17) * 0.25F;
            float var22 = var12 + (var2 - var18) * 0.25F;
            var0.addQuadFilled(var21, var17, var21 + var11, var17, var22 + var11, var18, var22, var18, var20);
         }
      }
   }

   static void streak(ImDrawList var0, float var1, float var2, float var3, float var4, int var5, float var6) {
      float var7 = 7.0F;
      float var8 = var4 % var7 / var7 * 2.0F;
      if (!(var8 > 1.0F)) {
         float var9 = var8 * var8 * (3.0F - 2.0F * var8);
         float var10 = -0.3F * var1 + var9 * 1.6F * var1;
         float var11 = (float)Math.sin(3.141592653589793 * var8);

         for (int var12 = 0; var12 < 2; var12++) {
            float var13 = var12 == 0 ? 3.0F * var3 : 46.0F * var3;
            float var14 = (var12 == 0 ? 0.55F : 0.07F) * var11 * var6;

            for (int var15 = 0; var15 < 10; var15++) {
               float var16 = var2 * var15 / 10.0F;
               float var17 = var2 * (var15 + 1) / 10.0F + 1.0F;
               float var18 = (float)Math.sin(3.141592653589793 * (var15 + 0.5) / 10.0);
               int var19 = EfnLogo.alpha(var5, var14 * var18);
               float var20 = var10 + (var2 - var16) * 0.25F;
               float var21 = var10 + (var2 - var17) * 0.25F;
               var0.addQuadFilled(var20, var16, var20 + var13, var16, var21 + var13, var17, var21, var17, var19);
            }
         }
      }
   }

   static void motes(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7, float var8) {
      for (int var9 = 0; var9 < 90; var9++) {
         float var10 = 0.3F + 0.7F * rnd(var9, 11);
         float var11 = frac(rnd(var9, 12) + var4 * 0.004F * var10) * var1 - var5 * 24.0F * var3 * var10;
         float var12 = frac(rnd(var9, 13) - var4 * 0.01F * var10) * var2 - var6 * 12.0F * var3 * var10;
         float var13 = 0.5F + 0.5F * (float)Math.sin(var4 * (0.6F + rnd(var9, 14) * 1.6F) + rnd(var9, 15) * 6.28F);
         float var14 = (0.7F + 1.5F * var10) * var3;
         int var15 = var9 % 4 == 0 ? var7 : -1;
         var0.addCircleFilled(var11, var12, var14, EfnLogo.alpha(var15, (0.1F + 0.5F * var10 * var13) * var8), 8);
      }
   }

   static void vignette(ImDrawList var0, float var1, float var2, float var3) {
      int var4 = rgba(4, 4, 6, 0.62F * var3);
      int var5 = rgba(4, 4, 6, 0.0F);
      float var6 = var1 * 0.24F;
      float var7 = var2 * 0.3F;
      var0.addRectFilledMultiColor(0.0F, 0.0F, var6, var2, var4, var5, var5, var4);
      var0.addRectFilledMultiColor(var1 - var6, 0.0F, var1, var2, var5, var4, var4, var5);
      var0.addRectFilledMultiColor(0.0F, var2 - var7, var1, var2, var5, var5, var4, var4);
      var0.addRectFilledMultiColor(0.0F, 0.0F, var1, var2 * 0.18F, var4, var4, var5, var5);
   }
}
