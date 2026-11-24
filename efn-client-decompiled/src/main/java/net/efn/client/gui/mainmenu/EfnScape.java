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

   private static float clamp01(float f) {
      return Math.max(0.0F, Math.min(1.0F, f));
   }

   private static float smooth(float f) {
      f = clamp01(f);
      return f * f * (3.0F - 2.0F * f);
   }

   public static void draw(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, int l, float m) {
      float var9 = g * 0.64F;
      int var10 = EfnScene.rgba(5, 7, 12, m);
      int var11 = EfnScene.rgba(8, 20, 30, m);
      int var12 = EfnScene.rgba(5, 7, 10, m);
      imDrawList.addRectFilledMultiColor(0.0F, 0.0F, f, var9, var10, var10, var11, var11);
      imDrawList.addRectFilled(0.0F, var9, f, g, var12);
      float var13 = f * 0.5F - j * 18.0F * h;
      float var14 = var9 - g * 0.1F - k * 8.0F * h;
      EfnScene.glow(imDrawList, var13, var14, g * 1.05F, l, 0.2F * m);
      EfnScene.glow(imDrawList, var13, var14, g * 0.38F, EfnScene.mix(l, -1, 0.5F), 0.22F * m);
      stars(imDrawList, f, var9, h, i, j, m);
      rays(imDrawList, var13, var14, g, i, l, m);

      for (int var15 = 0; var15 < 5; var15++) {
         ridge(imDrawList, f, g, h, i, j, var9, var15, l, m);
      }

      EfnScene.streak(imDrawList, f, g, h, i, l, m);
      EfnScene.motes(imDrawList, f, g, h, i, j, k, l, m * 0.8F);
      EfnScene.vignette(imDrawList, f, g, m);
   }

   private static void stars(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k) {
      for (int var7 = 0; var7 < 70; var7++) {
         float var8 = EfnScene.rnd(var7, 21) * f - j * 6.0F * h;
         float var9 = EfnScene.rnd(var7, 22) * g * 0.8F;
         float var10 = 0.4F + 0.6F * (float)Math.sin(i * (0.5F + EfnScene.rnd(var7, 23) * 1.5F) + EfnScene.rnd(var7, 24) * 6.28F);
         float var11 = (0.6F + 0.9F * EfnScene.rnd(var7, 25)) * h;
         imDrawList.addCircleFilled(var8, var9, var11, EfnScene.rgba(220, 240, 255, (0.12F + 0.55F * var10 * EfnScene.rnd(var7, 26)) * k), 6);
      }
   }

   private static void rays(ImDrawList imDrawList, float f, float g, float h, float i, int j, float k) {
      float var7 = h * 1.5F;

      for (int var8 = 0; var8 < 15; var8++) {
         float var9 = (var8 / 14.0F - 0.5F) * 3.1415927F * 0.95F;
         float var10 = -1.5707964F + var9 + (float)Math.sin(i * 0.07F + var8) * 0.025F;
         float var11 = 0.01F + 0.03F * EfnScene.rnd(var8, 31);
         float var12 = 0.5F + 0.5F * (float)Math.sin(i * (0.2F + 0.3F * EfnScene.rnd(var8, 32)) + EfnScene.rnd(var8, 33) * 6.28F);
         float var13 = (0.035F + 0.065F * EfnScene.rnd(var8, 34)) * (0.4F + 0.6F * var12) * k;

         for (int var14 = 0; var14 < 9; var14++) {
            float var15 = var7 * var14 / 9.0F;
            float var16 = var7 * (var14 + 1) / 9.0F;
            float var17 = 1.0F - var14 / 9.0F;
            int var18 = EfnLogo.alpha(j, var13 * var17 * var17);
            float var19 = var11 * (0.3F + 0.7F * var14 / 9.0F);
            float var20 = var11 * (0.3F + 0.7F * (var14 + 1) / 9.0F);
            float var21 = f + (float)Math.cos(var10 - var19) * var15;
            float var22 = g + (float)Math.sin(var10 - var19) * var15;
            float var23 = f + (float)Math.cos(var10 + var19) * var15;
            float var24 = g + (float)Math.sin(var10 + var19) * var15;
            float var25 = f + (float)Math.cos(var10 + var20) * var16;
            float var26 = g + (float)Math.sin(var10 + var20) * var16;
            float var27 = f + (float)Math.cos(var10 - var20) * var16;
            float var28 = g + (float)Math.sin(var10 - var20) * var16;
            imDrawList.addQuadFilled(var21, var22, var23, var24, var25, var26, var27, var28, var18);
         }
      }
   }

   private static void ridge(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, int l, int m, float n) {
      float var10 = (l + 1) / 5.0F;
      float var11 = k + (float)Math.pow(var10, 1.6) * g * 0.2F - g * 0.05F;
      float var12 = g * (0.58F - 0.07F * l);
      float var13 = (24.0F + 52.0F * var10) * h;
      float var14 = var13 * 0.85F;
      float var15 = i * (2.0F + 9.0F * var10) * h;
      float var16 = -j * (10.0F + 46.0F * var10) * h;
      int var17 = EfnScene.rgba(26, 74, 92, 1.0F);
      int var18 = EfnScene.rgba(5, 7, 10, 1.0F);
      int var19 = EfnLogo.alpha(EfnScene.mix(var17, var18, (float)Math.pow(var10, 0.8)), n);
      int var20 = EfnLogo.alpha(m, (0.55F - 0.32F * var10) * n);
      float var21 = f * 0.5F;
      int var22 = (int)Math.floor((-var13 - var16 + var15) / var13);
      float var23 = var22 * var13 - var15 + var16;

      for (float var24 = var23; var24 < f + var13; var24 += var13) {
         int var25 = Math.round((var24 - var23) / var13) + var22;
         float var26 = 0.6F * (float)Math.sin(var25 * 0.045F + l * 1.7F)
            + 0.3F * (float)Math.sin(var25 * 0.11F + l * 3.1F)
            + 0.18F * (EfnScene.rnd(var25, 40 + l) - 0.5F);
         float var27 = clamp01(0.55F + 0.7F * var26);
         float var28 = var24 + var13 * 0.5F;
         float var29 = 0.1F + 0.9F * smooth((Math.abs(var28 - var21) - var21 * 0.12F) / (var21 * 0.7F));
         float var30 = Math.round(var12 * var27 * var29 / var14) * var14;
         if (var30 < var14) {
            var30 = var14;
         }

         float var31 = var11 - var30;
         imDrawList.addRectFilled(var24, var31, var24 + var13 + 1.0F, g, var19);
         imDrawList.addRectFilled(var24, var31, var24 + var13 + 1.0F, var31 + Math.max(1.5F, 2.0F * h), var20);
      }

      int var32 = EfnScene.rgba(14, 44, 56, 0.0F);
      int var33 = EfnScene.rgba(14, 44, 56, (0.3F - 0.18F * var10) * n);
      imDrawList.addRectFilledMultiColor(0.0F, var11 - var12 * 0.55F, f, var11 + 6.0F * h, var32, var32, var33, var33);
   }
}
