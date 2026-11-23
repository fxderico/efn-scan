package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MenuIcons {
   private MenuIcons() {
   }

   public static void draw(ImDrawList imDrawList, MenuAction menuAction, float f, float g, float h, int i, float j) {
      switch (menuAction) {
         case LANGUAGE:
            globe(imDrawList, f, g, h, i, j);
            break;
         case ACCESSIBILITY:
            figure(imDrawList, f, g, h, i, j);
            break;
         case EFN:
            bolt(imDrawList, f, g, h, i, j);
            break;
         case MODS:
            folder(imDrawList, f, g, h, i, j);
            break;
         case ACCOUNTS:
            person(imDrawList, f, g, h, i, j);
            break;
         case CREDITS:
            scroll(imDrawList, f, g, h, i, j);
            break;
         default:
            imDrawList.addCircleFilled(f, g, h * 0.25F, i, 12);
      }
   }

   public static void person(ImDrawList imDrawList, float f, float g, float h, int i, float j) {
      imDrawList.addCircle(f, g - h * 0.38F, h * 0.42F, i, 24, j);
      imDrawList.pathClear();
      imDrawList.pathArcTo(f, g + h * 0.95F, h * 0.82F, 3.1415927F, 6.2831855F, 20);
      imDrawList.pathStroke(i, 0, j);
   }

   public static void bolt(ImDrawList imDrawList, float f, float g, float h, int i, float j) {
      ellipse(imDrawList, f, g - h * 0.55F, h * 0.36F, h * 0.45F, i, j);
      imDrawList.addLine(f - h * 0.72F, g - h * 0.05F, f + h * 0.72F, g - h * 0.05F, i, j * 1.2F);
      imDrawList.addLine(f, g - h * 0.1F, f, g + h * 1.05F, i, j * 1.2F);
   }

   public static void globe(ImDrawList imDrawList, float f, float g, float h, int i, float j) {
      imDrawList.addCircle(f, g, h, i, 32, j);
      ellipse(imDrawList, f, g, h * 0.45F, h, i, j);
      imDrawList.addLine(f - h, g, f + h, g, i, j);
   }

   public static void figure(ImDrawList imDrawList, float f, float g, float h, int i, float j) {
      imDrawList.addCircleFilled(f, g - h * 0.8F, h * 0.22F, i, 16);
      imDrawList.addLine(f - h * 0.85F, g - h * 0.35F, f + h * 0.85F, g - h * 0.35F, i, j);
      imDrawList.addLine(f, g - h * 0.35F, f, g + h * 0.3F, i, j);
      imDrawList.addLine(f, g + h * 0.3F, f - h * 0.5F, g + h, i, j);
      imDrawList.addLine(f, g + h * 0.3F, f + h * 0.5F, g + h, i, j);
   }

   public static void folder(ImDrawList imDrawList, float f, float g, float h, int i, float j) {
      float var6 = f - h;
      float var7 = f + h;
      float var8 = g - h * 0.7F;
      float var9 = g + h * 0.75F;
      imDrawList.pathClear();
      imDrawList.pathLineTo(var6, var9);
      imDrawList.pathLineTo(var6, var8);
      imDrawList.pathLineTo(f - h * 0.2F, var8);
      imDrawList.pathLineTo(f, var8 + h * 0.3F);
      imDrawList.pathLineTo(var7, var8 + h * 0.3F);
      imDrawList.pathLineTo(var7, var9);
      imDrawList.pathStroke(i, 1, j);
   }

   public static void scroll(ImDrawList imDrawList, float f, float g, float h, int i, float j) {
      float var6 = f - h * 0.62F;
      float var7 = f + h * 0.62F;
      float var8 = g - h * 0.78F;
      float var9 = g + h * 0.78F;
      imDrawList.addRect(var6, var8, var7, var9, i, 0.0F, 0, j);
      imDrawList.addLine(var6 - h * 0.28F, var8, var7 + h * 0.28F, var8, i, j * 1.3F);
      imDrawList.addLine(var6 - h * 0.28F, var9, var7 + h * 0.28F, var9, i, j * 1.3F);
      imDrawList.addLine(var6 + h * 0.3F, g - h * 0.22F, var7 - h * 0.3F, g - h * 0.22F, i, j);
      imDrawList.addLine(var6 + h * 0.3F, g + h * 0.22F, var7 - h * 0.3F, g + h * 0.22F, i, j);
   }

   public static void ellipse(ImDrawList imDrawList, float f, float g, float h, float i, int j, float k) {
      imDrawList.pathClear();
      byte var7 = 28;

      for (int var8 = 0; var8 < var7; var8++) {
         float var9 = (float)(var8 * 3.141592653589793 * 2.0 / var7);
         imDrawList.pathLineTo(f + (float)Math.cos(var9) * h, g + (float)Math.sin(var9) * i);
      }

      imDrawList.pathStroke(j, 1, k);
   }
}
