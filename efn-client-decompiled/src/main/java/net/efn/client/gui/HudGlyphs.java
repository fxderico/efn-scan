package net.efn.client.gui;

import imgui.ImDrawList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class HudGlyphs {
   private static final float TAU = 6.2831855F;

   private HudGlyphs() {
   }

   static int ink(int var0, float var1) {
      return HudDraw.col(var0, var1);
   }

   static int vitality(float var0) {
      float var1 = Math.max(0.0F, Math.min(1.0F, var0));
      return var1 >= 0.55F ? HudDraw.mix(-15408912, -12600956, (var1 - 0.55F) / 0.45F) : HudDraw.mix(-2537681, -15408912, var1 / 0.55F);
   }

   static float approach(float var0, float var1, float var2, float var3) {
      float var4 = var0 + (var1 - var0) * (1.0F - (float)Math.exp(-var2 * var3));
      return Math.abs(var4 - var1) < 5.0E-4F ? var1 : var4;
   }

   static float easeOutQuint(float var0) {
      float var1 = 1.0F - Math.max(0.0F, Math.min(1.0F, var0));
      return 1.0F - var1 * var1 * var1 * var1 * var1;
   }

   static void register(ImDrawList var0, float var1, float var2, float var3, float var4, int var5) {
      if (!(var2 - var1 < 2.0F)) {
         float var6 = (var1 + var2) * 0.5F;
         int var7 = var5 & 16777215;
         var0.addRectFilledMultiColor(var1, var3, var6, var3 + var4, var7, var5, var5, var7);
         var0.addRectFilledMultiColor(var6, var3, var2, var3 + var4, var5, var7, var7, var5);
      }
   }

   static void arc(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6, int var7) {
      if (!(Math.abs(var5) < 0.002F) && (var7 >>> 24 & 0xFF) != 0) {
         int var8 = Math.max(6, Math.round(Math.abs(var5) / 6.2831855F * 40.0F));
         var0.pathClear();
         var0.pathArcTo(var1, var2, var3, var4, var4 + var5, var8);
         var0.pathStroke(var7, 0, var6);
      }
   }

   static void bolt(ImDrawList var0, float var1, float var2, float var3, float var4, int var5) {
      var0.addTriangleFilled(var1 + var3 * 0.18F, var2 - var3 * 0.5F, var1 - var3 * 0.26F, var2 + var3 * 0.06F, var1 + var3 * 0.08F, var2 + var3 * 0.06F, var5);
      var0.addTriangleFilled(var1 - var3 * 0.08F, var2 - var3 * 0.06F, var1 + var3 * 0.26F, var2 - var3 * 0.06F, var1 - var3 * 0.18F, var2 + var3 * 0.5F, var5);
   }

   static void sparkle(ImDrawList var0, float var1, float var2, float var3, int var4) {
      float var5 = var3 * 0.3F;
      var0.addQuadFilled(var1, var2 - var3, var1 + var5, var2, var1, var2 + var3, var1 - var5, var2, var4);
      var0.addQuadFilled(var1 - var3, var2, var1, var2 - var5, var1 + var3, var2, var1, var2 + var5, var4);
   }

   static void hourglass(ImDrawList var0, float var1, float var2, float var3, float var4, int var5) {
      float var6 = var3 * 0.5F;
      float var7 = var3 * 0.36F;
      var0.addLine(var1 - var7, var2 - var6, var1 + var7, var2 - var6, var5, var4);
      var0.addLine(var1 - var7, var2 + var6, var1 + var7, var2 + var6, var5, var4);
      var0.addTriangleFilled(var1 - var7 * 0.8F, var2 - var6, var1 + var7 * 0.8F, var2 - var6, var1, var2, var5);
      var0.addTriangle(var1 - var7 * 0.8F, var2 + var6, var1 + var7 * 0.8F, var2 + var6, var1, var2, var5, var4);
   }

   static void reticle(ImDrawList var0, float var1, float var2, float var3, float var4, int var5) {
      byte var6 = 28;
      float var7 = var3 * 0.82F;

      for (int var8 = 0; var8 < var6; var8++) {
         double var9 = 6.283185307179586 * var8 / var6;
         double var11 = 6.283185307179586 * (var8 + 1) / var6;
         var0.addLine(
            var1 + var7 * (float)Math.cos(var9),
            var2 + var7 * (float)Math.sin(var9),
            var1 + var7 * (float)Math.cos(var11),
            var2 + var7 * (float)Math.sin(var11),
            var5,
            var4
         );
      }

      var0.addCircleFilled(var1, var2, var3 * 0.16F, var5, 12);
      var0.addLine(var1 - var3 * 1.05F, var2, var1 - var3 * 0.5F, var2, var5, var4);
      var0.addLine(var1 + var3 * 0.5F, var2, var1 + var3 * 1.05F, var2, var5, var4);
      var0.addLine(var1, var2 - var3 * 1.05F, var1, var2 - var3 * 0.5F, var5, var4);
      var0.addLine(var1, var2 + var3 * 0.5F, var1, var2 + var3 * 1.05F, var5, var4);
   }
}
