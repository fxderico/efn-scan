package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MenuIcons {
   private MenuIcons() {
   }

   public static void draw(ImDrawList var0, MenuAction var1, float var2, float var3, float var4, int var5, float var6) {
      switch (var1) {
         case LANGUAGE:
            globe(var0, var2, var3, var4, var5, var6);
            break;
         case ACCESSIBILITY:
            figure(var0, var2, var3, var4, var5, var6);
            break;
         case EFN:
            bolt(var0, var2, var3, var4, var5, var6);
            break;
         case MODS:
            folder(var0, var2, var3, var4, var5, var6);
            break;
         case ACCOUNTS:
            person(var0, var2, var3, var4, var5, var6);
            break;
         case CREDITS:
            scroll(var0, var2, var3, var4, var5, var6);
            break;
         default:
            var0.addCircleFilled(var2, var3, var4 * 0.25F, var5, 12);
      }
   }

   public static void person(ImDrawList var0, float var1, float var2, float var3, int var4, float var5) {
      var0.addCircle(var1, var2 - var3 * 0.38F, var3 * 0.42F, var4, 24, var5);
      var0.pathClear();
      var0.pathArcTo(var1, var2 + var3 * 0.95F, var3 * 0.82F, 3.1415927F, 6.2831855F, 20);
      var0.pathStroke(var4, 0, var5);
   }

   public static void bolt(ImDrawList var0, float var1, float var2, float var3, int var4, float var5) {
      ellipse(var0, var1, var2 - var3 * 0.55F, var3 * 0.36F, var3 * 0.45F, var4, var5);
      var0.addLine(var1 - var3 * 0.72F, var2 - var3 * 0.05F, var1 + var3 * 0.72F, var2 - var3 * 0.05F, var4, var5 * 1.2F);
      var0.addLine(var1, var2 - var3 * 0.1F, var1, var2 + var3 * 1.05F, var4, var5 * 1.2F);
   }

   public static void globe(ImDrawList var0, float var1, float var2, float var3, int var4, float var5) {
      var0.addCircle(var1, var2, var3, var4, 32, var5);
      ellipse(var0, var1, var2, var3 * 0.45F, var3, var4, var5);
      var0.addLine(var1 - var3, var2, var1 + var3, var2, var4, var5);
   }

   public static void figure(ImDrawList var0, float var1, float var2, float var3, int var4, float var5) {
      var0.addCircleFilled(var1, var2 - var3 * 0.8F, var3 * 0.22F, var4, 16);
      var0.addLine(var1 - var3 * 0.85F, var2 - var3 * 0.35F, var1 + var3 * 0.85F, var2 - var3 * 0.35F, var4, var5);
      var0.addLine(var1, var2 - var3 * 0.35F, var1, var2 + var3 * 0.3F, var4, var5);
      var0.addLine(var1, var2 + var3 * 0.3F, var1 - var3 * 0.5F, var2 + var3, var4, var5);
      var0.addLine(var1, var2 + var3 * 0.3F, var1 + var3 * 0.5F, var2 + var3, var4, var5);
   }

   public static void folder(ImDrawList var0, float var1, float var2, float var3, int var4, float var5) {
      float var6 = var1 - var3;
      float var7 = var1 + var3;
      float var8 = var2 - var3 * 0.7F;
      float var9 = var2 + var3 * 0.75F;
      var0.pathClear();
      var0.pathLineTo(var6, var9);
      var0.pathLineTo(var6, var8);
      var0.pathLineTo(var1 - var3 * 0.2F, var8);
      var0.pathLineTo(var1, var8 + var3 * 0.3F);
      var0.pathLineTo(var7, var8 + var3 * 0.3F);
      var0.pathLineTo(var7, var9);
      var0.pathStroke(var4, 1, var5);
   }

   public static void scroll(ImDrawList var0, float var1, float var2, float var3, int var4, float var5) {
      float var6 = var1 - var3 * 0.62F;
      float var7 = var1 + var3 * 0.62F;
      float var8 = var2 - var3 * 0.78F;
      float var9 = var2 + var3 * 0.78F;
      var0.addRect(var6, var8, var7, var9, var4, 0.0F, 0, var5);
      var0.addLine(var6 - var3 * 0.28F, var8, var7 + var3 * 0.28F, var8, var4, var5 * 1.3F);
      var0.addLine(var6 - var3 * 0.28F, var9, var7 + var3 * 0.28F, var9, var4, var5 * 1.3F);
      var0.addLine(var6 + var3 * 0.3F, var2 - var3 * 0.22F, var7 - var3 * 0.3F, var2 - var3 * 0.22F, var4, var5);
      var0.addLine(var6 + var3 * 0.3F, var2 + var3 * 0.22F, var7 - var3 * 0.3F, var2 + var3 * 0.22F, var4, var5);
   }

   public static void ellipse(ImDrawList var0, float var1, float var2, float var3, float var4, int var5, float var6) {
      var0.pathClear();
      byte var7 = 28;

      for (int var8 = 0; var8 < var7; var8++) {
         float var9 = (float)(var8 * 3.141592653589793 * 2.0 / var7);
         var0.pathLineTo(var1 + (float)Math.cos(var9) * var3, var2 + (float)Math.sin(var9) * var4);
      }

      var0.pathStroke(var5, 1, var6);
   }
}
