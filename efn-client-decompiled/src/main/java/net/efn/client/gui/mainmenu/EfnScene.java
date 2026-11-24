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

   public static int rgba(int i, int j, int k, float f) {
      return Math.round(Math.max(0.0F, Math.min(1.0F, f)) * 255.0F) << 24 | k << 16 | j << 8 | i;
   }

   public static int mix(int i, int j, float f) {
      f = Math.max(0.0F, Math.min(1.0F, f));
      int var3 = Math.round((i & 0xFF) + ((j & 0xFF) - (i & 0xFF)) * f);
      int var4 = Math.round((i >> 8 & 0xFF) + ((j >> 8 & 0xFF) - (i >> 8 & 0xFF)) * f);
      int var5 = Math.round((i >> 16 & 0xFF) + ((j >> 16 & 0xFF) - (i >> 16 & 0xFF)) * f);
      return 0xFF000000 | var5 << 16 | var4 << 8 | var3;
   }

   public static float rnd(int i, int j) {
      int var2 = i * 374761393 + j * 668265263;
      var2 = (var2 ^ var2 >>> 13) * 1274126177;
      var2 ^= var2 >>> 16;
      return (var2 & 16777215) / 1.6777216E7F;
   }

   private static float frac(float f) {
      return f - (float)Math.floor(f);
   }

   public static void glow(ImDrawList imDrawList, float f, float g, float h, int i, float j) {
      byte var6 = 28;

      for (int var7 = 0; var7 < var6; var7++) {
         float var8 = (float)var7 / var6;
         float var9 = h * (1.0F - var8);
         float var10 = j / var6 * (0.5F + 1.6F * var8);
         imDrawList.addCircleFilled(f, g, var9, EfnLogo.alpha(i, var10), 48);
      }
   }

   public static void draw(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, int l, float m) {
      int var9 = rgba(7, 7, 10, m);
      int var10 = rgba(12, 14, 19, m);
      imDrawList.addRectFilledMultiColor(0.0F, 0.0F, f, g, var9, var9, var10, var10);
      int var11 = mix(l, -41923, 0.55F);
      float var12 = Math.max(f, g);
      glow(
         imDrawList,
         f * (0.24F + 0.05F * (float)Math.sin(i * 0.11F)) - j * 26.0F * h,
         g * (0.3F + 0.05F * (float)Math.cos(i * 0.13F)) - k * 14.0F * h,
         var12 * 0.55F,
         l,
         0.1F * m
      );
      glow(
         imDrawList,
         f * (0.8F + 0.04F * (float)Math.cos(i * 0.09F)) - j * 34.0F * h,
         g * (0.74F + 0.05F * (float)Math.sin(i * 0.1F)) - k * 18.0F * h,
         var12 * 0.5F,
         var11,
         0.09F * m
      );
      ghost(imDrawList, f, g, h, i, j, k, m);
      beams(imDrawList, f, g, h, i, j, l, m);
      streak(imDrawList, f, g, h, i, l, m);
      motes(imDrawList, f, g, h, i, j, k, l, m);
      vignette(imDrawList, f, g, m);
   }

   private static void ghost(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, float l) {
      float var8 = g * 1.25F;
      float var9 = f * 0.66F - j * 30.0F * h;
      float var10 = g * 0.5F - var8 * 0.5F + (float)Math.sin(i * 0.2F) * 10.0F * h - k * 16.0F * h;
      EfnLogo.mark(imDrawList, var9, var10, var8, rgba(255, 255, 255, 0.04F * l));
   }

   private static void beams(ImDrawList imDrawList, float f, float g, float h, float i, float j, int k, float l) {
      for (int var8 = 0; var8 < 12; var8++) {
         float var9 = 0.4F + 0.6F * rnd(var8, 1);
         float var10 = 0.004F + 0.01F * rnd(var8, 2);
         float var11 = (24.0F + 90.0F * rnd(var8, 3)) * h * var9;
         float var12 = (frac(rnd(var8, 4) + i * var10) * 1.5F - 0.25F) * f - j * 40.0F * h * var9;
         float var13 = 0.55F + 0.45F * (float)Math.sin(i * (0.25F + 0.4F * rnd(var8, 5)) + rnd(var8, 6) * 6.28F);
         float var14 = (0.045F + 0.075F * rnd(var8, 7)) * var13 * l;
         int var15 = var8 % 3 == 0 ? k : -1;

         for (int var16 = 0; var16 < 10; var16++) {
            float var17 = g * var16 / 10.0F;
            float var18 = g * (var16 + 1) / 10.0F + 1.0F;
            float var19 = (float)Math.sin(3.141592653589793 * (var16 + 0.5) / 10.0);
            int var20 = EfnLogo.alpha(var15, var14 * var19 * var19);
            float var21 = var12 + (g - var17) * 0.25F;
            float var22 = var12 + (g - var18) * 0.25F;
            imDrawList.addQuadFilled(var21, var17, var21 + var11, var17, var22 + var11, var18, var22, var18, var20);
         }
      }
   }

   static void streak(ImDrawList imDrawList, float f, float g, float h, float i, int j, float k) {
      float var7 = 7.0F;
      float var8 = i % var7 / var7 * 2.0F;
      if (!(var8 > 1.0F)) {
         float var9 = var8 * var8 * (3.0F - 2.0F * var8);
         float var10 = -0.3F * f + var9 * 1.6F * f;
         float var11 = (float)Math.sin(3.141592653589793 * var8);

         for (int var12 = 0; var12 < 2; var12++) {
            float var13 = var12 == 0 ? 3.0F * h : 46.0F * h;
            float var14 = (var12 == 0 ? 0.55F : 0.07F) * var11 * k;

            for (int var15 = 0; var15 < 10; var15++) {
               float var16 = g * var15 / 10.0F;
               float var17 = g * (var15 + 1) / 10.0F + 1.0F;
               float var18 = (float)Math.sin(3.141592653589793 * (var15 + 0.5) / 10.0);
               int var19 = EfnLogo.alpha(j, var14 * var18);
               float var20 = var10 + (g - var16) * 0.25F;
               float var21 = var10 + (g - var17) * 0.25F;
               imDrawList.addQuadFilled(var20, var16, var20 + var13, var16, var21 + var13, var17, var21, var17, var19);
            }
         }
      }
   }

   static void motes(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, int l, float m) {
      for (int var9 = 0; var9 < 90; var9++) {
         float var10 = 0.3F + 0.7F * rnd(var9, 11);
         float var11 = frac(rnd(var9, 12) + i * 0.004F * var10) * f - j * 24.0F * h * var10;
         float var12 = frac(rnd(var9, 13) - i * 0.01F * var10) * g - k * 12.0F * h * var10;
         float var13 = 0.5F + 0.5F * (float)Math.sin(i * (0.6F + rnd(var9, 14) * 1.6F) + rnd(var9, 15) * 6.28F);
         float var14 = (0.7F + 1.5F * var10) * h;
         int var15 = var9 % 4 == 0 ? l : -1;
         imDrawList.addCircleFilled(var11, var12, var14, EfnLogo.alpha(var15, (0.1F + 0.5F * var10 * var13) * m), 8);
      }
   }

   static void vignette(ImDrawList imDrawList, float f, float g, float h) {
      int var4 = rgba(4, 4, 6, 0.62F * h);
      int var5 = rgba(4, 4, 6, 0.0F);
      float var6 = f * 0.24F;
      float var7 = g * 0.3F;
      imDrawList.addRectFilledMultiColor(0.0F, 0.0F, var6, g, var4, var5, var5, var4);
      imDrawList.addRectFilledMultiColor(f - var6, 0.0F, f, g, var5, var4, var4, var5);
      imDrawList.addRectFilledMultiColor(0.0F, g - var7, f, g, var5, var5, var4, var4);
      imDrawList.addRectFilledMultiColor(0.0F, 0.0F, f, g * 0.18F, var4, var4, var5, var5);
   }
}
