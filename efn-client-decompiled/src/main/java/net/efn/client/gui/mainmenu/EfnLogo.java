package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EfnLogo {
   private static final float[][] BARS = new float[][]{
      {14.0F, 12.0F, 50.0F, 22.0F}, {14.0F, 22.0F, 27.0F, 42.0F}, {27.0F, 27.0F, 44.0F, 37.0F}, {14.0F, 42.0F, 50.0F, 52.0F}
   };
   private static final int TILE = -14410470;

   private EfnLogo() {
   }

   public static int alpha(int i, float f) {
      int var2 = Math.round((i >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, f)));
      return var2 << 24 | i & 16777215;
   }

   public static void tile(ImDrawList imDrawList, float f, float g, float h, int i, float j) {
      float var6 = h / 64.0F;
      imDrawList.addRectFilled(f, g, f + h, g + h, alpha(-14410470, j), 15.0F * var6);
      imDrawList.addRect(f, g, f + h, g + h, alpha(-1, 0.07F * j), 15.0F * var6, 0, Math.max(1.0F, var6));
      mark(imDrawList, f, g, h, alpha(i, j));
   }

   public static void tileFilling(ImDrawList imDrawList, float f, float g, float h, int i, float j, float k) {
      float var7 = h / 64.0F;
      imDrawList.addRectFilled(f, g, f + h, g + h, alpha(-14410470, k), 15.0F * var7);
      imDrawList.addRect(f, g, f + h, g + h, alpha(-1, 0.07F * k), 15.0F * var7, 0, Math.max(1.0F, var7));
      mark(imDrawList, f, g, h, alpha(i, 0.16F * k));
      float var8 = g + h * (1.0F - Math.max(0.0F, Math.min(1.0F, j)));
      imDrawList.pushClipRect(f - h, var8, f + h * 2.0F, g + h * 2.0F, true);
      mark(imDrawList, f, g, h, alpha(i, k));
      imDrawList.popClipRect();
   }

   public static void mark(ImDrawList imDrawList, float f, float g, float h, int i) {
      float var5 = h / 64.0F;

      for (float[] var9 : BARS) {
         float var10 = var9[0];
         float var11 = var9[1];
         float var12 = var9[2];
         float var13 = var9[3];
         float var14 = f + (var10 - 0.25F * var11 + 8.0F) * var5;
         float var15 = g + var11 * var5;
         float var16 = f + (var12 - 0.25F * var11 + 8.0F) * var5;
         float var17 = f + (var12 - 0.25F * var13 + 8.0F) * var5;
         float var18 = g + var13 * var5;
         float var19 = f + (var10 - 0.25F * var13 + 8.0F) * var5;
         imDrawList.addQuadFilled(var14, var15, var16, var15, var17, var18, var19, var18, i);
      }
   }
}
