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

   public static int alpha(int var0, float var1) {
      int var2 = Math.round((var0 >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, var1)));
      return var2 << 24 | var0 & 16777215;
   }

   public static void tile(ImDrawList var0, float var1, float var2, float var3, int var4, float var5) {
      float var6 = var3 / 64.0F;
      var0.addRectFilled(var1, var2, var1 + var3, var2 + var3, alpha(-14410470, var5), 15.0F * var6);
      var0.addRect(var1, var2, var1 + var3, var2 + var3, alpha(-1, 0.07F * var5), 15.0F * var6, 0, Math.max(1.0F, var6));
      mark(var0, var1, var2, var3, alpha(var4, var5));
   }

   public static void tileFilling(ImDrawList var0, float var1, float var2, float var3, int var4, float var5, float var6) {
      float var7 = var3 / 64.0F;
      var0.addRectFilled(var1, var2, var1 + var3, var2 + var3, alpha(-14410470, var6), 15.0F * var7);
      var0.addRect(var1, var2, var1 + var3, var2 + var3, alpha(-1, 0.07F * var6), 15.0F * var7, 0, Math.max(1.0F, var7));
      mark(var0, var1, var2, var3, alpha(var4, 0.16F * var6));
      float var8 = var2 + var3 * (1.0F - Math.max(0.0F, Math.min(1.0F, var5)));
      var0.pushClipRect(var1 - var3, var8, var1 + var3 * 2.0F, var2 + var3 * 2.0F, true);
      mark(var0, var1, var2, var3, alpha(var4, var6));
      var0.popClipRect();
   }

   public static void mark(ImDrawList var0, float var1, float var2, float var3, int var4) {
      float var5 = var3 / 64.0F;

      for (float[] var9 : BARS) {
         float var10 = var9[0];
         float var11 = var9[1];
         float var12 = var9[2];
         float var13 = var9[3];
         float var14 = var1 + (var10 - 0.25F * var11 + 8.0F) * var5;
         float var15 = var2 + var11 * var5;
         float var16 = var1 + (var12 - 0.25F * var11 + 8.0F) * var5;
         float var17 = var1 + (var12 - 0.25F * var13 + 8.0F) * var5;
         float var18 = var2 + var13 * var5;
         float var19 = var1 + (var10 - 0.25F * var13 + 8.0F) * var5;
         var0.addQuadFilled(var14, var15, var16, var15, var17, var18, var19, var18, var4);
      }
   }
}
