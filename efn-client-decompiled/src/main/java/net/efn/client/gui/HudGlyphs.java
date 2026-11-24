package net.efn.client.gui;

import imgui.ImDrawList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class HudGlyphs {
   private static final float TAU = 6.2831855F;

   private HudGlyphs() {
   }

   static int ink(int i, float f) {
      return HudDraw.col(i, f);
   }

   static int vitality(float f) {
      float var1 = Math.max(0.0F, Math.min(1.0F, f));
      return var1 >= 0.55F ? HudDraw.mix(-15408912, -12600956, (var1 - 0.55F) / 0.45F) : HudDraw.mix(-2537681, -15408912, var1 / 0.55F);
   }

   static float approach(float f, float g, float h, float i) {
      float var4 = f + (g - f) * (1.0F - (float)Math.exp(-h * i));
      return Math.abs(var4 - g) < 5.0E-4F ? g : var4;
   }

   static float easeOutQuint(float f) {
      float var1 = 1.0F - Math.max(0.0F, Math.min(1.0F, f));
      return 1.0F - var1 * var1 * var1 * var1 * var1;
   }

   static void register(ImDrawList imDrawList, float f, float g, float h, float i, int j) {
      if (!(g - f < 2.0F)) {
         float var6 = (f + g) * 0.5F;
         int var7 = j & 16777215;
         imDrawList.addRectFilledMultiColor(f, h, var6, h + i, var7, j, j, var7);
         imDrawList.addRectFilledMultiColor(var6, h, g, h + i, j, var7, var7, j);
      }
   }

   static void arc(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, int l) {
      if (!(Math.abs(j) < 0.002F) && (l >>> 24 & 0xFF) != 0) {
         int var8 = Math.max(6, Math.round(Math.abs(j) / 6.2831855F * 40.0F));
         imDrawList.pathClear();
         imDrawList.pathArcTo(f, g, h, i, i + j, var8);
         imDrawList.pathStroke(l, 0, k);
      }
   }

   static void bolt(ImDrawList imDrawList, float f, float g, float h, float i, int j) {
      imDrawList.addTriangleFilled(f + h * 0.18F, g - h * 0.5F, f - h * 0.26F, g + h * 0.06F, f + h * 0.08F, g + h * 0.06F, j);
      imDrawList.addTriangleFilled(f - h * 0.08F, g - h * 0.06F, f + h * 0.26F, g - h * 0.06F, f - h * 0.18F, g + h * 0.5F, j);
   }

   static void sparkle(ImDrawList imDrawList, float f, float g, float h, int i) {
      float var5 = h * 0.3F;
      imDrawList.addQuadFilled(f, g - h, f + var5, g, f, g + h, f - var5, g, i);
      imDrawList.addQuadFilled(f - h, g, f, g - var5, f + h, g, f, g + var5, i);
   }

   static void hourglass(ImDrawList imDrawList, float f, float g, float h, float i, int j) {
      float var6 = h * 0.5F;
      float var7 = h * 0.36F;
      imDrawList.addLine(f - var7, g - var6, f + var7, g - var6, j, i);
      imDrawList.addLine(f - var7, g + var6, f + var7, g + var6, j, i);
      imDrawList.addTriangleFilled(f - var7 * 0.8F, g - var6, f + var7 * 0.8F, g - var6, f, g, j);
      imDrawList.addTriangle(f - var7 * 0.8F, g + var6, f + var7 * 0.8F, g + var6, f, g, j, i);
   }

   static void reticle(ImDrawList imDrawList, float f, float g, float h, float i, int j) {
      byte var6 = 28;
      float var7 = h * 0.82F;

      for (int var8 = 0; var8 < var6; var8++) {
         double var9 = 6.283185307179586 * var8 / var6;
         double var11 = 6.283185307179586 * (var8 + 1) / var6;
         imDrawList.addLine(
            f + var7 * (float)Math.cos(var9), g + var7 * (float)Math.sin(var9), f + var7 * (float)Math.cos(var11), g + var7 * (float)Math.sin(var11), j, i
         );
      }

      imDrawList.addCircleFilled(f, g, h * 0.16F, j, 12);
      imDrawList.addLine(f - h * 1.05F, g, f - h * 0.5F, g, j, i);
      imDrawList.addLine(f + h * 0.5F, g, f + h * 1.05F, g, j, i);
      imDrawList.addLine(f, g - h * 1.05F, f, g - h * 0.5F, j, i);
      imDrawList.addLine(f, g + h * 0.5F, f, g + h * 1.05F, j, i);
   }
}
