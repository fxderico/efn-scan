package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImGui;
import net.efn.client.config.Surface;
import net.efn.client.config.ThemeStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SurfaceBackground {
   private SurfaceBackground() {
   }

   public static float bannerHeight(Surface var0) {
      return var0.isBanner() && isVisible(var0) ? EfnWidgets.s(var0.headerHeight.get()) : 0.0F;
   }

   public static boolean hasImageBanner(Surface var0) {
      return var0.isImage() && bannerHeight(var0) > 0.0F;
   }

   public static void draw(ImDrawList var0, Surface var1, float var2, float var3, float var4, float var5, float var6) {
      draw(var0, var1, var2, var3, var4, var5, var6, 1.0F);
   }

   public static void draw(ImDrawList var0, Surface var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      if (!(var7 <= 0.001F) && isVisible(var1)) {
         boolean var9 = var1.isBanner();
         float var8 = var9 ? Math.min(var5, EfnWidgets.s(var1.headerHeight.get())) : var5;
         if (var1.isImage()) {
            drawImage(var0, var1, var2, var3, var4, var8, var6, var9, var7, var1.imageScale.get());
         } else {
            drawPattern(var0, var1, var2, var3, var4, var8, var6, var9, var7);
         }
      }
   }

   public static void drawDisc(ImDrawList var0, Surface var1, float var2, float var3, float var4, float var5) {
      if (!(var5 <= 0.001F) && isVisible(var1)) {
         float var6 = var2 - var4;
         float var7 = var3 - var4;
         float var8 = 2.0F * var4;
         if (var1.isImage()) {
            drawImage(var0, var1, var6, var7, var8, var8, var4, false, var5, Math.max(1.0F, var1.imageScale.get()));
         } else {
            drawPattern(var0, var1, var6, var7, var8, var8, var4, false, var5);
         }
      }
   }

   private static boolean isVisible(Surface var0) {
      return var0.background.is("None") ? false : !var0.isImage() || ThemeImages.get().texture(var0.image.get()) != null;
   }

   private static void drawImage(
      ImDrawList var0, Surface var1, float var2, float var3, float var4, float var5, float var6, boolean var7, float var8, float var9
   ) {
      ThemeImages.Texture var10 = ThemeImages.get().texture(var1.image.get());
      if (var10 != null) {
         float[] var11 = var1.isScreenMapped()
            ? place(var10, var1, 0.0F, 0.0F, ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY(), var9)
            : place(var10, var1, var2, var3, var4, var5, var9);
         drawClipped(
            var0, var10.id(), var11, var2, var3, var4, var5, EfnWidgets.c(EfnWidgets.withAlpha(-1, var1.imageOpacity.get() * var8)), var6, var7 ? 48 : 240
         );
         if (var7) {
            int var12 = HudChrome.scaleAlpha(GuiPalette.fill(var1), var8);
            int var13 = var12 & 16777215;
            var0.addRectFilledMultiColor(
               var2, var3 + var5 * 0.3F, var2 + var4, var3 + var5 + 1.0F, EfnWidgets.c(var13), EfnWidgets.c(var13), EfnWidgets.c(var12), EfnWidgets.c(var12)
            );
         }
      }
   }

   public static float[] coverUv(int var0, int var1, float var2, float var3, float var4) {
      float var6 = (float)var0 / Math.max(1, var1);
      float var5;
      if (var6 > (var5 = var2 / Math.max(1.0F, var3))) {
         float var9 = var5 / var6;
         float var10 = (1.0F - var9) * 0.5F;
         return new float[]{var10, 0.0F, var10 + var9, 1.0F};
      } else {
         float var7 = var6 / var5;
         float var8 = (1.0F - var7) * Math.max(0.0F, Math.min(1.0F, var4));
         return new float[]{0.0F, var8, 1.0F, var8 + var7};
      }
   }

   public static float[] place(int var0, int var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      float var9 = Math.max(var4 / Math.max(1, var0), var5 / Math.max(1, var1)) * var6;
      float var10 = var0 * var9;
      float var11 = var1 * var9;
      float var12 = var2 + (var4 - var10) * var7;
      float var13 = var3 + (var5 - var11) * var8;
      return new float[]{var12, var13, var12 + var10, var13 + var11};
   }

   private static float[] place(ThemeImages.Texture var0, Surface var1, float var2, float var3, float var4, float var5) {
      return place(var0, var1, var2, var3, var4, var5, var1.imageScale.get());
   }

   private static float[] place(ThemeImages.Texture var0, Surface var1, float var2, float var3, float var4, float var5, float var6) {
      return place(var0.width(), var0.height(), var2, var3, var4, var5, var6, var1.imageX.get(), var1.imageY.get());
   }

   private static void drawClipped(ImDrawList var0, int var1, float[] var2, float var3, float var4, float var5, float var6, int var7, float var8, int var9) {
      float var10 = Math.max(var3, var2[0]);
      float var11 = Math.max(var4, var2[1]);
      float var12 = Math.min(var3 + var5, var2[2]);
      float var13 = Math.min(var4 + var6, var2[3]);
      if (!(var12 <= var10) && !(var13 <= var11)) {
         float var14 = var2[2] - var2[0];
         float var15 = var2[3] - var2[1];
         float var16 = (var10 - var2[0]) / var14;
         float var17 = (var11 - var2[1]) / var15;
         float var18 = (var12 - var2[0]) / var14;
         float var19 = (var13 - var2[1]) / var15;
         boolean var20 = var10 - var3 <= var8;
         boolean var21 = var11 - var4 <= var8;
         boolean var22 = var3 + var5 - var12 <= var8;
         boolean var23 = var4 + var6 - var13 <= var8;
         int var24 = (var20 && var21 ? 16 : 0) | (var22 && var21 ? 32 : 0) | (var20 && var23 ? 64 : 0) | (var22 && var23 ? 128 : 0);
         int var25 = var9 & var24;
         if (var8 > 0.0F && var25 != 0) {
            var0.addImageRounded(var1, var10, var11, var12, var13, var16, var17, var18, var19, var7, var8, var25);
         } else {
            var0.addImage(var1, var10, var11, var12, var13, var16, var17, var18, var19, var7);
         }
      }
   }

   public static void drawPart(
      ImDrawList var0,
      Surface var1,
      float var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      int var12
   ) {
      if (!(var10 <= 0.001F) && !(var8 <= 0.0F) && !(var9 <= 0.0F) && !(var4 <= 0.0F) && !(var5 <= 0.0F) && isVisible(var1)) {
         float var13 = ImGui.getIO().getDisplaySizeX();
         float var14 = ImGui.getIO().getDisplaySizeY();
         boolean var15 = var1.isScreenMapped();
         if (var1.isImage()) {
            ThemeImages.Texture var18 = ThemeImages.get().texture(var1.image.get());
            if (var18 != null) {
               float[] var17 = var15 ? place(var18, var1, 0.0F, 0.0F, var13, var14) : place(var18, var1, var2, var3, var4, var5);
               drawClipped(
                  var0, var18.id(), var17, var6, var7, var8, var9, EfnWidgets.c(EfnWidgets.withAlpha(-1, var1.imageOpacity.get() * var10)), var11, var12
               );
            }
         } else {
            SurfaceBackground.Lines var16 = new SurfaceBackground.Lines(
               var0,
               var6,
               var7,
               var8,
               var9,
               0.0F,
               var1.lineOpacity.get() * var10,
               false,
               var15 ? var6 : var6 - var2,
               var15 ? var7 : var7 - var3,
               var15 ? var13 : var4,
               false
            );
            var0.pushClipRect(var6, var7, var6 + var8, var7 + var9, true);
            drawLines(var1, var16);
            var0.popClipRect();
         }
      }
   }

   private static void drawPattern(ImDrawList var0, Surface var1, float var2, float var3, float var4, float var5, float var6, boolean var7, float var8) {
      boolean var9 = var1.isScreenMapped();
      SurfaceBackground.Lines var10 = new SurfaceBackground.Lines(
         var0,
         var2,
         var3,
         var4,
         var5,
         var6,
         var1.lineOpacity.get() * var8,
         var7,
         var9 ? var2 : 0.0F,
         var9 ? var3 : 0.0F,
         var9 ? ImGui.getIO().getDisplaySizeX() : var4,
         true
      );
      drawLines(var1, var10);
   }

   private static void drawLines(Surface var0, SurfaceBackground.Lines var1) {
      ThemeStore var2 = ThemeStore.get();
      float var3 = var2.animateLines.get() ? (float)ImGui.getTime() * GuiPalette.animationSpeed() : 0.0F;
      String var4 = var0.background.get();
      switch (var4) {
         case "Flow":
            flow(var1, var3);
            break;
         case "Topographic":
            topographic(var1, var3);
            break;
         case "Rings":
            rings(var1, var3);
            break;
         case "Grid":
            grid(var1, var3);
      }
   }

   private static void flow(SurfaceBackground.Lines var0, float var1) {
      float var2 = EfnWidgets.s(6.0F);
      float var3 = EfnWidgets.s(10.0F);
      float var4 = var1 * 0.25F;
      int var5 = (int)Math.floor((var0.originY - EfnWidgets.s(40.0F)) / var3);
      int var6 = (int)Math.ceil((var0.originY + var0.h + EfnWidgets.s(40.0F)) / var3);

      for (int var7 = var5; var7 <= var6; var7++) {
         float var8 = 0.0F;
         float var9 = 0.0F;

         for (float var10 = 0.0F; var10 <= var0.w + var2; var10 += var2) {
            float var11 = (var0.originX + var10) / EfnWidgets.s(1.0F);
            float var12 = var7 * var3
               + (float)Math.sin(var11 * 0.018F + var7 * 0.35F + var4) * EfnWidgets.s(14.0F)
               + (float)Math.sin(var11 * 0.007F - var7 * 0.2F - var4 * 0.6F) * EfnWidgets.s(10.0F);
            float var13 = var12 - var0.originY;
            if (var10 > 0.0F) {
               var0.segment(var8, var9, var10, var13);
            }

            var8 = var10;
            var9 = var13;
         }
      }
   }

   private static void rings(SurfaceBackground.Lines var0, float var1) {
      float var2 = var0.worldW + EfnWidgets.s(10.0F) - var0.originX;
      float var3 = -EfnWidgets.s(30.0F) - var0.originY;
      float var4 = EfnWidgets.s(14.0F);
      float var5 = var1 * EfnWidgets.s(4.0F) % var4;
      float var6 = (float)Math.hypot(Math.max(0.0F, Math.max(-var2, var2 - var0.w)), Math.max(0.0F, Math.max(-var3, var3 - var0.h)));
      float var7 = (float)Math.hypot(Math.max(Math.abs(var2), Math.abs(var2 - var0.w)), Math.max(Math.abs(var3), Math.abs(var3 - var0.h)));
      float var8 = EfnWidgets.s(24.0F) + var5;
      if (var6 > var8) {
         var8 += (float)Math.floor((var6 - var8) / var4) * var4;
      }

      for (float var9 = var8; var9 <= var7; var9 += var4) {
         int var10 = Math.max(24, (int)(var9 * 0.35F));
         float var11 = var2 + var9;
         float var12 = var3;

         for (int var13 = 1; var13 <= var10; var13++) {
            double var14 = 6.283185307179586 * var13 / var10;
            float var16 = var2 + (float)Math.cos(var14) * var9;
            float var17 = var3 + (float)Math.sin(var14) * var9;
            var0.segment(var11, var12, var16, var17);
            var11 = var16;
            var12 = var17;
         }
      }
   }

   private static void grid(SurfaceBackground.Lines var0, float var1) {
      float var2 = EfnWidgets.s(16.0F);
      float var3 = EfnWidgets.s(8.0F);
      float var4 = var1 * EfnWidgets.s(3.0F) % var2;
      float var5 = floorMod(var4 - var0.originX, var2);
      float var6 = floorMod(var4 - var0.originY, var2);

      for (float var7 = var5; var7 <= var0.w; var7 += var2) {
         for (float var8 = 0.0F; var8 < var0.h; var8 += var3) {
            var0.segment(var7, var8, var7, var8 + var3);
         }
      }

      for (float var9 = var6; var9 <= var0.h; var9 += var2) {
         for (float var10 = 0.0F; var10 < var0.w; var10 += var3) {
            var0.segment(var10, var9, var10 + var3, var9);
         }
      }
   }

   private static void topographic(SurfaceBackground.Lines var0, float var1) {
      float var2 = EfnWidgets.s(6.0F);
      int var3 = (int)Math.ceil(var0.w / var2) + 1;
      int var4 = (int)Math.ceil(var0.h / var2) + 1;
      float[] var5 = new float[var3 * var4];
      float var6 = var1 * 0.15F;

      for (int var7 = 0; var7 < var4; var7++) {
         for (int var8 = 0; var8 < var3; var8++) {
            float var9 = (var0.originX + var8 * var2) / EfnWidgets.s(1.0F);
            float var10 = (var0.originY + var7 * var2) / EfnWidgets.s(1.0F);
            var5[var7 * var3 + var8] = (float)(
               Math.sin(var9 * 0.021 + Math.sin(var10 * 0.03 + var6) * 1.6)
                  + Math.cos(var10 * 0.028 + Math.sin(var9 * 0.013 - var6) * 2.1)
                  + 0.6 * Math.sin((var9 + var10) * 0.017 + var6)
            );
         }
      }

      for (float var11 = -2.4F; var11 <= 2.4F; var11 += 0.4F) {
         for (int var12 = 0; var12 < var4 - 1; var12++) {
            for (int var13 = 0; var13 < var3 - 1; var13++) {
               contourCell(var0, var5, var3, var13, var12, var2, var11);
            }
         }
      }
   }

   private static void contourCell(SurfaceBackground.Lines var0, float[] var1, int var2, int var3, int var4, float var5, float var6) {
      float var10 = var1[var4 * var2 + var3];
      float var7;
      float var8;
      float var9;
      int var11 = (var10 > var6 ? 8 : 0)
         | ((var9 = var1[var4 * var2 + var3 + 1]) > var6 ? 4 : 0)
         | ((var8 = var1[(var4 + 1) * var2 + var3 + 1]) > var6 ? 2 : 0)
         | ((var7 = var1[(var4 + 1) * var2 + var3]) > var6 ? 1 : 0);
      if (var11 != 0 && var11 != 15) {
         float var12 = var3 * var5;
         float var13 = var4 * var5;
         float var14 = var12 + var5 * t(var10, var9, var6);
         float var16 = var12 + var5;
         float var17 = var13 + var5 * t(var9, var8, var6);
         float var18 = var12 + var5 * t(var7, var8, var6);
         float var19 = var13 + var5;
         float var21 = var13 + var5 * t(var10, var7, var6);
         switch (var11) {
            case 1:
            case 14:
               var0.segment(var12, var21, var18, var19);
               break;
            case 2:
            case 13:
               var0.segment(var18, var19, var16, var17);
               break;
            case 3:
            case 12:
               var0.segment(var12, var21, var16, var17);
               break;
            case 4:
            case 11:
               var0.segment(var14, var13, var16, var17);
               break;
            case 5:
               var0.segment(var12, var21, var14, var13);
               var0.segment(var18, var19, var16, var17);
               break;
            case 6:
            case 9:
               var0.segment(var14, var13, var18, var19);
               break;
            case 7:
            case 8:
               var0.segment(var12, var21, var14, var13);
               break;
            case 10:
               var0.segment(var14, var13, var16, var17);
               var0.segment(var12, var21, var18, var19);
         }
      }
   }

   private static float t(float var0, float var1, float var2) {
      float var3 = var1 - var0;
      return Math.abs(var3) < 1.0E-6F ? 0.5F : (var2 - var0) / var3;
   }

   private static float floorMod(float var0, float var1) {
      float var2 = var0 % var1;
      return var2 < 0.0F ? var2 + var1 : var2;
   }

   @Environment(EnvType.CLIENT)
   private static final class Lines {
      final ImDrawList dl;
      final float x;
      final float y;
      final float w;
      final float h;
      final float edge;
      final float originX;
      final float originY;
      final float worldW;
      final boolean banner;
      final int color;

      Lines(
         ImDrawList var1,
         float var2,
         float var3,
         float var4,
         float var5,
         float var6,
         float var7,
         boolean var8,
         float var9,
         float var10,
         float var11,
         boolean var12
      ) {
         this.dl = var1;
         this.x = var2;
         this.y = var3;
         this.w = var4;
         this.h = var5;
         this.banner = var8;
         this.originX = var9;
         this.originY = var10;
         this.worldW = var11;
         this.edge = var12 ? Math.max(EfnWidgets.s(8.0F), var6) : 0.0F;
         float var13 = var8 ? 0.24F : 0.16F;
         this.color = EfnWidgets.withAlpha(EfnTheme.lerpColor(-1, GuiPalette.accent(), 0.25F), var13 * var7);
      }

      void segment(float var1, float var2, float var3, float var4) {
         float var5 = (var1 + var3) * 0.5F;
         float var6 = (var2 + var4) * 0.5F;
         if (!(var6 < 0.0F) && !(var6 > this.h) && !(var5 < 0.0F) && !(var5 > this.w)) {
            float var7 = 1.0F;
            if (this.banner) {
               var7 = 1.0F - var6 / this.h;
               var7 *= var7;
            } else if (this.edge > 0.0F) {
               var7 *= Math.min(1.0F, Math.min(var6, this.h - var6) / this.edge);
            }

            if (this.edge > 0.0F) {
               var7 *= Math.min(1.0F, Math.min(var5, this.w - var5) / this.edge);
            }

            if (!(var7 <= 0.02F)) {
               this.dl.addLine(this.x + var1, this.y + var2, this.x + var3, this.y + var4, EfnWidgets.c(EfnWidgets.withAlpha(this.color, var7)), 1.0F);
            }
         }
      }
   }
}
