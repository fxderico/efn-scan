package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EfnScape {
   private static final int LAYERS = 5;
   private static final int RAYS = 15;
   private static final int RAY_SEGMENTS = 9;
   private static final int STARS = 70;

   private EfnScape() {
   }

   private static float clamp01(float var0) {
      return Math.max(0.0F, Math.min(1.0F, var0));
   }

   private static float smooth(float var0) {
      var0 = clamp01(var0);
      return var0 * var0 * (3.0F - 2.0F * var0);
   }

   public static void draw(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7, float var8) {
      float var9 = var2 * 0.64F;
      int var10 = EfnScene.rgba(5, 7, 12, var8);
      int var11 = EfnScene.rgba(8, 20, 30, var8);
      int var12 = EfnScene.rgba(5, 7, 10, var8);
      var0.addRectFilledMultiColor(0.0F, 0.0F, var1, var9, var10, var10, var11, var11);
      var0.addRectFilled(0.0F, var9, var1, var2, var12);
      float var13 = var1 * 0.5F - var5 * 18.0F * var3;
      float var14 = var9 - var2 * 0.1F - var6 * 8.0F * var3;
      EfnScene.glow(var0, var13, var14, var2 * 1.05F, var7, 0.2F * var8);
      EfnScene.glow(var0, var13, var14, var2 * 0.38F, EfnScene.mix(var7, -1, 0.5F), 0.22F * var8);
      stars(var0, var1, var9, var3, var4, var5, var8);
      rays(var0, var13, var14, var2, var4, var7, var8);

      for (int var15 = 0; var15 < 5; var15++) {
         ridge(var0, var1, var2, var3, var4, var5, var9, var15, var7, var8);
      }

      EfnScene.streak(var0, var1, var2, var3, var4, var7, var8);
      EfnScene.motes(var0, var1, var2, var3, var4, var5, var6, var7, var8 * 0.8F);
      EfnScene.vignette(var0, var1, var2, var8);
   }

   private static void stars(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      for (int var7 = 0; var7 < 70; var7++) {
         float var8 = EfnScene.rnd(var7, 21) * var1 - var5 * 6.0F * var3;
         float var9 = EfnScene.rnd(var7, 22) * var2 * 0.8F;
         float var10 = 0.4F + 0.6F * (float)Math.sin(var4 * (0.5F + EfnScene.rnd(var7, 23) * 1.5F) + EfnScene.rnd(var7, 24) * 6.28F);
         float var11 = (0.6F + 0.9F * EfnScene.rnd(var7, 25)) * var3;
         var0.addCircleFilled(var8, var9, var11, EfnScene.rgba(220, 240, 255, (0.12F + 0.55F * var10 * EfnScene.rnd(var7, 26)) * var6), 6);
      }
   }

   private static void rays(ImDrawList var0, float var1, float var2, float var3, float var4, int var5, float var6) {
      float var7 = var3 * 1.5F;

      for (int var8 = 0; var8 < 15; var8++) {
         float var9 = (var8 / 14.0F - 0.5F) * 3.1415927F * 0.95F;
         float var10 = -1.5707964F + var9 + (float)Math.sin(var4 * 0.07F + var8) * 0.025F;
         float var11 = 0.01F + 0.03F * EfnScene.rnd(var8, 31);
         float var12 = 0.5F + 0.5F * (float)Math.sin(var4 * (0.2F + 0.3F * EfnScene.rnd(var8, 32)) + EfnScene.rnd(var8, 33) * 6.28F);
         float var13 = (0.035F + 0.065F * EfnScene.rnd(var8, 34)) * (0.4F + 0.6F * var12) * var6;

         for (int var14 = 0; var14 < 9; var14++) {
            float var15 = var7 * var14 / 9.0F;
            float var16 = var7 * (var14 + 1) / 9.0F;
            float var17 = 1.0F - var14 / 9.0F;
            int var18 = EfnLogo.alpha(var5, var13 * var17 * var17);
            float var19 = var11 * (0.3F + 0.7F * var14 / 9.0F);
            float var20 = var11 * (0.3F + 0.7F * (var14 + 1) / 9.0F);
            float var21 = var1 + (float)Math.cos(var10 - var19) * var15;
            float var22 = var2 + (float)Math.sin(var10 - var19) * var15;
            float var23 = var1 + (float)Math.cos(var10 + var19) * var15;
            float var24 = var2 + (float)Math.sin(var10 + var19) * var15;
            float var25 = var1 + (float)Math.cos(var10 + var20) * var16;
            float var26 = var2 + (float)Math.sin(var10 + var20) * var16;
            float var27 = var1 + (float)Math.cos(var10 - var20) * var16;
            float var28 = var2 + (float)Math.sin(var10 - var20) * var16;
            var0.addQuadFilled(var21, var22, var23, var24, var25, var26, var27, var28, var18);
         }
      }
   }

   private static void ridge(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7, int var8, float var9) {
      float var10 = (var7 + 1) / 5.0F;
      float var11 = var6 + (float)Math.pow(var10, 1.6) * var2 * 0.2F - var2 * 0.05F;
      float var12 = var2 * (0.58F - 0.07F * var7);
      float var13 = (24.0F + 52.0F * var10) * var3;
      float var14 = var13 * 0.85F;
      float var15 = var4 * (2.0F + 9.0F * var10) * var3;
      float var16 = -var5 * (10.0F + 46.0F * var10) * var3;
      int var17 = EfnScene.rgba(26, 74, 92, 1.0F);
      int var18 = EfnScene.rgba(5, 7, 10, 1.0F);
      int var19 = EfnLogo.alpha(EfnScene.mix(var17, var18, (float)Math.pow(var10, 0.8)), var9);
      int var20 = EfnLogo.alpha(var8, (0.55F - 0.32F * var10) * var9);
      float var21 = var1 * 0.5F;
      int var22 = (int)Math.floor((-var13 - var16 + var15) / var13);
      float var23 = var22 * var13 - var15 + var16;

      for (float var24 = var23; var24 < var1 + var13; var24 += var13) {
         int var25 = Math.round((var24 - var23) / var13) + var22;
         float var26 = 0.6F * (float)Math.sin(var25 * 0.045F + var7 * 1.7F)
            + 0.3F * (float)Math.sin(var25 * 0.11F + var7 * 3.1F)
            + 0.18F * (EfnScene.rnd(var25, 40 + var7) - 0.5F);
         float var27 = clamp01(0.55F + 0.7F * var26);
         float var28 = var24 + var13 * 0.5F;
         float var29 = 0.1F + 0.9F * smooth((Math.abs(var28 - var21) - var21 * 0.12F) / (var21 * 0.7F));
         float var30 = Math.round(var12 * var27 * var29 / var14) * var14;
         if (var30 < var14) {
            var30 = var14;
         }

         float var31 = var11 - var30;
         var0.addRectFilled(var24, var31, var24 + var13 + 1.0F, var2, var19);
         var0.addRectFilled(var24, var31, var24 + var13 + 1.0F, var31 + Math.max(1.5F, 2.0F * var3), var20);
      }

      int var32 = EfnScene.rgba(14, 44, 56, 0.0F);
      int var33 = EfnScene.rgba(14, 44, 56, (0.3F - 0.18F * var10) * var9);
      var0.addRectFilledMultiColor(0.0F, var11 - var12 * 0.55F, var1, var11 + 6.0F * var3, var32, var32, var33, var33);
   }
}
