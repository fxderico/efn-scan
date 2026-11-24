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

   public static float bannerHeight(Surface surface) {
      return surface.isBanner() && isVisible(surface) ? EfnWidgets.s(surface.headerHeight.get()) : 0.0F;
   }

   public static boolean hasImageBanner(Surface surface) {
      return surface.isImage() && bannerHeight(surface) > 0.0F;
   }

   public static void draw(ImDrawList imDrawList, Surface surface, float f, float g, float h, float i, float j) {
      draw(imDrawList, surface, f, g, h, i, j, 1.0F);
   }

   public static void draw(ImDrawList imDrawList, Surface surface, float f, float g, float h, float i, float j, float k) {
      if (!(k <= 0.001F) && isVisible(surface)) {
         boolean var9 = surface.isBanner();
         float var8 = var9 ? Math.min(i, EfnWidgets.s(surface.headerHeight.get())) : i;
         if (surface.isImage()) {
            drawImage(imDrawList, surface, f, g, h, var8, j, var9, k, surface.imageScale.get());
         } else {
            drawPattern(imDrawList, surface, f, g, h, var8, j, var9, k);
         }
      }
   }

   public static void drawDisc(ImDrawList imDrawList, Surface surface, float f, float g, float h, float i) {
      if (!(i <= 0.001F) && isVisible(surface)) {
         float var6 = f - h;
         float var7 = g - h;
         float var8 = 2.0F * h;
         if (surface.isImage()) {
            drawImage(imDrawList, surface, var6, var7, var8, var8, h, false, i, Math.max(1.0F, surface.imageScale.get()));
         } else {
            drawPattern(imDrawList, surface, var6, var7, var8, var8, h, false, i);
         }
      }
   }

   private static boolean isVisible(Surface surface) {
      return surface.background.is("None") ? false : !surface.isImage() || ThemeImages.get().texture(surface.image.get()) != null;
   }

   private static void drawImage(ImDrawList imDrawList, Surface surface, float f, float g, float h, float i, float j, boolean bl, float k, float l) {
      ThemeImages.Texture var10 = ThemeImages.get().texture(surface.image.get());
      if (var10 != null) {
         float[] var11 = surface.isScreenMapped()
            ? place(var10, surface, 0.0F, 0.0F, ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY(), l)
            : place(var10, surface, f, g, h, i, l);
         drawClipped(imDrawList, var10.id(), var11, f, g, h, i, EfnWidgets.c(EfnWidgets.withAlpha(-1, surface.imageOpacity.get() * k)), j, bl ? 48 : 240);
         if (bl) {
            int var12 = HudChrome.scaleAlpha(GuiPalette.fill(surface), k);
            int var13 = var12 & 16777215;
            imDrawList.addRectFilledMultiColor(
               f, g + i * 0.3F, f + h, g + i + 1.0F, EfnWidgets.c(var13), EfnWidgets.c(var13), EfnWidgets.c(var12), EfnWidgets.c(var12)
            );
         }
      }
   }

   public static float[] coverUv(int i, int j, float f, float g, float h) {
      float var6 = (float)i / Math.max(1, j);
      float var5;
      if (var6 > (var5 = f / Math.max(1.0F, g))) {
         float var9 = var5 / var6;
         float var10 = (1.0F - var9) * 0.5F;
         return new float[]{var10, 0.0F, var10 + var9, 1.0F};
      } else {
         float var7 = var6 / var5;
         float var8 = (1.0F - var7) * Math.max(0.0F, Math.min(1.0F, h));
         return new float[]{0.0F, var8, 1.0F, var8 + var7};
      }
   }

   public static float[] place(int i, int j, float f, float g, float h, float k, float l, float m, float n) {
      float var9 = Math.max(h / Math.max(1, i), k / Math.max(1, j)) * l;
      float var10 = i * var9;
      float var11 = j * var9;
      float var12 = f + (h - var10) * m;
      float var13 = g + (k - var11) * n;
      return new float[]{var12, var13, var12 + var10, var13 + var11};
   }

   private static float[] place(ThemeImages.Texture texture, Surface surface, float f, float g, float h, float i) {
      return place(texture, surface, f, g, h, i, surface.imageScale.get());
   }

   private static float[] place(ThemeImages.Texture texture, Surface surface, float f, float g, float h, float i, float j) {
      return place(texture.width(), texture.height(), f, g, h, i, j, surface.imageX.get(), surface.imageY.get());
   }

   private static void drawClipped(ImDrawList imDrawList, int i, float[] fs, float f, float g, float h, float j, int k, float l, int m) {
      float var10 = Math.max(f, fs[0]);
      float var11 = Math.max(g, fs[1]);
      float var12 = Math.min(f + h, fs[2]);
      float var13 = Math.min(g + j, fs[3]);
      if (!(var12 <= var10) && !(var13 <= var11)) {
         float var14 = fs[2] - fs[0];
         float var15 = fs[3] - fs[1];
         float var16 = (var10 - fs[0]) / var14;
         float var17 = (var11 - fs[1]) / var15;
         float var18 = (var12 - fs[0]) / var14;
         float var19 = (var13 - fs[1]) / var15;
         boolean var20 = var10 - f <= l;
         boolean var21 = var11 - g <= l;
         boolean var22 = f + h - var12 <= l;
         boolean var23 = g + j - var13 <= l;
         int var24 = (var20 && var21 ? 16 : 0) | (var22 && var21 ? 32 : 0) | (var20 && var23 ? 64 : 0) | (var22 && var23 ? 128 : 0);
         int var25 = m & var24;
         if (l > 0.0F && var25 != 0) {
            imDrawList.addImageRounded(i, var10, var11, var12, var13, var16, var17, var18, var19, k, l, var25);
         } else {
            imDrawList.addImage(i, var10, var11, var12, var13, var16, var17, var18, var19, k);
         }
      }
   }

   public static void drawPart(
      ImDrawList imDrawList, Surface surface, float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, int p
   ) {
      if (!(n <= 0.001F) && !(l <= 0.0F) && !(m <= 0.0F) && !(h <= 0.0F) && !(i <= 0.0F) && isVisible(surface)) {
         float var13 = ImGui.getIO().getDisplaySizeX();
         float var14 = ImGui.getIO().getDisplaySizeY();
         boolean var15 = surface.isScreenMapped();
         if (surface.isImage()) {
            ThemeImages.Texture var18 = ThemeImages.get().texture(surface.image.get());
            if (var18 != null) {
               float[] var17 = var15 ? place(var18, surface, 0.0F, 0.0F, var13, var14) : place(var18, surface, f, g, h, i);
               drawClipped(imDrawList, var18.id(), var17, j, k, l, m, EfnWidgets.c(EfnWidgets.withAlpha(-1, surface.imageOpacity.get() * n)), o, p);
            }
         } else {
            SurfaceBackground.Lines var16 = new SurfaceBackground.Lines(
               imDrawList, j, k, l, m, 0.0F, surface.lineOpacity.get() * n, false, var15 ? j : j - f, var15 ? k : k - g, var15 ? var13 : h, false
            );
            imDrawList.pushClipRect(j, k, j + l, k + m, true);
            drawLines(surface, var16);
            imDrawList.popClipRect();
         }
      }
   }

   private static void drawPattern(ImDrawList imDrawList, Surface surface, float f, float g, float h, float i, float j, boolean bl, float k) {
      boolean var9 = surface.isScreenMapped();
      SurfaceBackground.Lines var10 = new SurfaceBackground.Lines(
         imDrawList, f, g, h, i, j, surface.lineOpacity.get() * k, bl, var9 ? f : 0.0F, var9 ? g : 0.0F, var9 ? ImGui.getIO().getDisplaySizeX() : h, true
      );
      drawLines(surface, var10);
   }

   private static void drawLines(Surface surface, SurfaceBackground.Lines lines) {
      ThemeStore var2 = ThemeStore.get();
      float var3 = var2.animateLines.get() ? (float)ImGui.getTime() * GuiPalette.animationSpeed() : 0.0F;
      String var4 = surface.background.get();
      switch (var4) {
         case "Flow":
            flow(lines, var3);
            break;
         case "Topographic":
            topographic(lines, var3);
            break;
         case "Rings":
            rings(lines, var3);
            break;
         case "Grid":
            grid(lines, var3);
      }
   }

   private static void flow(SurfaceBackground.Lines lines, float f) {
      float var2 = EfnWidgets.s(6.0F);
      float var3 = EfnWidgets.s(10.0F);
      float var4 = f * 0.25F;
      int var5 = (int)Math.floor((lines.originY - EfnWidgets.s(40.0F)) / var3);
      int var6 = (int)Math.ceil((lines.originY + lines.h + EfnWidgets.s(40.0F)) / var3);

      for (int var7 = var5; var7 <= var6; var7++) {
         float var8 = 0.0F;
         float var9 = 0.0F;

         for (float var10 = 0.0F; var10 <= lines.w + var2; var10 += var2) {
            float var11 = (lines.originX + var10) / EfnWidgets.s(1.0F);
            float var12 = var7 * var3
               + (float)Math.sin(var11 * 0.018F + var7 * 0.35F + var4) * EfnWidgets.s(14.0F)
               + (float)Math.sin(var11 * 0.007F - var7 * 0.2F - var4 * 0.6F) * EfnWidgets.s(10.0F);
            float var13 = var12 - lines.originY;
            if (var10 > 0.0F) {
               lines.segment(var8, var9, var10, var13);
            }

            var8 = var10;
            var9 = var13;
         }
      }
   }

   private static void rings(SurfaceBackground.Lines lines, float f) {
      float var2 = lines.worldW + EfnWidgets.s(10.0F) - lines.originX;
      float var3 = -EfnWidgets.s(30.0F) - lines.originY;
      float var4 = EfnWidgets.s(14.0F);
      float var5 = f * EfnWidgets.s(4.0F) % var4;
      float var6 = (float)Math.hypot(Math.max(0.0F, Math.max(-var2, var2 - lines.w)), Math.max(0.0F, Math.max(-var3, var3 - lines.h)));
      float var7 = (float)Math.hypot(Math.max(Math.abs(var2), Math.abs(var2 - lines.w)), Math.max(Math.abs(var3), Math.abs(var3 - lines.h)));
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
            lines.segment(var11, var12, var16, var17);
            var11 = var16;
            var12 = var17;
         }
      }
   }

   private static void grid(SurfaceBackground.Lines lines, float f) {
      float var2 = EfnWidgets.s(16.0F);
      float var3 = EfnWidgets.s(8.0F);
      float var4 = f * EfnWidgets.s(3.0F) % var2;
      float var5 = floorMod(var4 - lines.originX, var2);
      float var6 = floorMod(var4 - lines.originY, var2);

      for (float var7 = var5; var7 <= lines.w; var7 += var2) {
         for (float var8 = 0.0F; var8 < lines.h; var8 += var3) {
            lines.segment(var7, var8, var7, var8 + var3);
         }
      }

      for (float var9 = var6; var9 <= lines.h; var9 += var2) {
         for (float var10 = 0.0F; var10 < lines.w; var10 += var3) {
            lines.segment(var10, var9, var10 + var3, var9);
         }
      }
   }

   private static void topographic(SurfaceBackground.Lines lines, float f) {
      float var2 = EfnWidgets.s(6.0F);
      int var3 = (int)Math.ceil(lines.w / var2) + 1;
      int var4 = (int)Math.ceil(lines.h / var2) + 1;
      float[] var5 = new float[var3 * var4];
      float var6 = f * 0.15F;

      for (int var7 = 0; var7 < var4; var7++) {
         for (int var8 = 0; var8 < var3; var8++) {
            float var9 = (lines.originX + var8 * var2) / EfnWidgets.s(1.0F);
            float var10 = (lines.originY + var7 * var2) / EfnWidgets.s(1.0F);
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
               contourCell(lines, var5, var3, var13, var12, var2, var11);
            }
         }
      }
   }

   private static void contourCell(SurfaceBackground.Lines lines, float[] fs, int i, int j, int k, float f, float g) {
      float var10 = fs[k * i + j];
      float var7;
      float var8;
      float var9;
      int var11 = (var10 > g ? 8 : 0)
         | ((var9 = fs[k * i + j + 1]) > g ? 4 : 0)
         | ((var8 = fs[(k + 1) * i + j + 1]) > g ? 2 : 0)
         | ((var7 = fs[(k + 1) * i + j]) > g ? 1 : 0);
      if (var11 != 0 && var11 != 15) {
         float var12 = j * f;
         float var13 = k * f;
         float var14 = var12 + f * t(var10, var9, g);
         float var16 = var12 + f;
         float var17 = var13 + f * t(var9, var8, g);
         float var18 = var12 + f * t(var7, var8, g);
         float var19 = var13 + f;
         float var21 = var13 + f * t(var10, var7, g);
         switch (var11) {
            case 1:
            case 14:
               lines.segment(var12, var21, var18, var19);
               break;
            case 2:
            case 13:
               lines.segment(var18, var19, var16, var17);
               break;
            case 3:
            case 12:
               lines.segment(var12, var21, var16, var17);
               break;
            case 4:
            case 11:
               lines.segment(var14, var13, var16, var17);
               break;
            case 5:
               lines.segment(var12, var21, var14, var13);
               lines.segment(var18, var19, var16, var17);
               break;
            case 6:
            case 9:
               lines.segment(var14, var13, var18, var19);
               break;
            case 7:
            case 8:
               lines.segment(var12, var21, var14, var13);
               break;
            case 10:
               lines.segment(var14, var13, var16, var17);
               lines.segment(var12, var21, var18, var19);
         }
      }
   }

   private static float t(float f, float g, float h) {
      float var3 = g - f;
      return Math.abs(var3) < 1.0E-6F ? 0.5F : (h - f) / var3;
   }

   private static float floorMod(float f, float g) {
      float var2 = f % g;
      return var2 < 0.0F ? var2 + g : var2;
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

      Lines(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, boolean bl, float l, float m, float n, boolean bl2) {
         this.dl = imDrawList;
         this.x = f;
         this.y = g;
         this.w = h;
         this.h = i;
         this.banner = bl;
         this.originX = l;
         this.originY = m;
         this.worldW = n;
         this.edge = bl2 ? Math.max(EfnWidgets.s(8.0F), j) : 0.0F;
         float var13 = bl ? 0.24F : 0.16F;
         this.color = EfnWidgets.withAlpha(EfnTheme.lerpColor(-1, GuiPalette.accent(), 0.25F), var13 * k);
      }

      void segment(float f, float g, float h, float i) {
         float var5 = (f + h) * 0.5F;
         float var6 = (g + i) * 0.5F;
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
               this.dl.addLine(this.x + f, this.y + g, this.x + h, this.y + i, EfnWidgets.c(EfnWidgets.withAlpha(this.color, var7)), 1.0F);
            }
         }
      }
   }
}
