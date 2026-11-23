package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.efn.client.config.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class HudChrome {
   static final int TEXT_VALUE = GuiPalette.abgr(-657929);
   static final int TEXT_NAME = GuiPalette.abgr(-1447442);
   static final int TEXT_LABEL = GuiPalette.abgr(-6052949);
   static final int WARN = GuiPalette.abgr(-47814);
   static final int BORDER = 402653183;
   static final int DIVIDER = 536870911;
   static final int TRACK = 452984831;
   static final int CHIP_FILL = GuiPalette.abgr(-350873571);
   private static final int TEXT_GUARD = -1946157056;
   private static final Long2FloatOpenHashMap DIGIT_CELLS = new Long2FloatOpenHashMap();
   private static final String[] ASCII = new String[128];

   private HudChrome() {
   }

   static void card(ImDrawList imDrawList, Surface surface, float f, float g, float h, float i, float j, float k, boolean bl, float l) {
      if (!(k <= 0.001F)) {
         if (bl) {
            shadow(imDrawList, f, g, h, i, j, k, l);
         }

         imDrawList.addRectFilled(f, g, f + h, g + i, scaleAlpha(GuiPalette.fill(surface), k), j);
         SurfaceBackground.draw(imDrawList, surface, f, g, h, i, j, k);
         imDrawList.addRect(f, g, f + h, g + i, scaleAlpha(402653183, k), j, 0, 1.0F);
         int var10 = GuiPalette.accent();
         float var11 = Math.max(1.5F, 1.5F * l);
         float var12 = Math.min(h * 0.5F, 56.0F * Math.max(1.0F, l));
         float var13 = f + Math.min(j, h * 0.2F);
         imDrawList.addRectFilledMultiColor(
            var13, g, var13 + var12, g + var11, scaleAlpha(var10, k * 0.95F), scaleAlpha(var10, 0.0F), scaleAlpha(var10, 0.0F), scaleAlpha(var10, k * 0.95F)
         );
      }
   }

   static void shadow(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k, float l) {
      float var8 = Math.max(1.0F, l);
      imDrawList.addRectFilled(f - var8, g - 0.5F * var8, f + h + var8, g + i + 1.5F * var8, scaleAlpha(771751936, k), j + var8);
      imDrawList.addRectFilled(f - 2.0F * var8, g + 0.5F * var8, f + h + 2.0F * var8, g + i + 3.5F * var8, scaleAlpha(385875968, k), j + 2.0F * var8);
      imDrawList.addRectFilled(f - 3.0F * var8, g + 1.5F * var8, f + h + 3.0F * var8, g + i + 6.0F * var8, scaleAlpha(201326592, k), j + 3.0F * var8);
   }

   static void divider(ImDrawList imDrawList, float f, float g, float h, float i) {
      float var5 = Math.round(f);
      float var6 = Math.round(g - h * 0.5F);
      imDrawList.addRectFilled(var5, var6, var5 + 1.0F, var6 + Math.round(h), scaleAlpha(536870911, i));
   }

   static boolean needsTextGuard(Surface surface) {
      return surface != null && (surface.opacity.get() < 0.55F || surface.isImage());
   }

   static int px(float f, float g) {
      return Math.max(6, Math.round(f * g));
   }

   static float width(ImFont imFont, int i, String string) {
      return imFont != null && !string.isEmpty() ? imFont.calcTextSizeA(i, 3.4028235E38F, 0.0F, string).x : string.length() * i * 0.55F;
   }

   static float ascent(ImFont imFont, int i) {
      return imFont != null && !(imFont.getFontSize() <= 0.0F) ? imFont.getAscent() * i / imFont.getFontSize() : i * 0.77F;
   }

   static float baselineOffset(ImFont imFont, int i, ImFont imFont2, int j) {
      return Math.round(ascent(imFont, i) - ascent(imFont2, j));
   }

   static void text(ImDrawList imDrawList, ImFont imFont, int i, float f, float g, int j, String string, boolean bl) {
      if (!string.isEmpty() && (j >>> 24 & 0xFF) != 0) {
         if (bl) {
            raw(imDrawList, imFont, i, f + 1.0F, g + 1.0F, scaleAlpha(-1946157056, (j >>> 24 & 0xFF) / 255.0F), string);
         }

         raw(imDrawList, imFont, i, f, g, j, string);
      }
   }

   private static void raw(ImDrawList imDrawList, ImFont imFont, int i, float f, float g, int j, String string) {
      if (imFont != null) {
         imDrawList.addText(imFont, i, f, g, j, string);
      } else {
         imDrawList.addText(f, g, j, string);
      }
   }

   private static float digitCell(ImFont imFont, int i) {
      if (imFont == null) {
         return i * 0.6F;
      } else {
         long var2 = (long)System.identityHashCode(imFont) << 16 ^ i;
         float var4 = DIGIT_CELLS.get(var2);
         if (!Float.isNaN(var4)) {
            return var4;
         } else {
            float var5 = 0.0F;

            for (char var6 = '0'; var6 <= '9'; var6++) {
               var5 = Math.max(var5, advance(imFont, i, var6));
            }

            if (DIGIT_CELLS.size() > 64) {
               DIGIT_CELLS.clear();
            }

            DIGIT_CELLS.put(var2, var5);
            return var5;
         }
      }
   }

   private static float advance(ImFont imFont, int i, char c) {
      return imFont.getCharAdvance(c) * i / imFont.getFontSize();
   }

   static float tabularWidth(ImFont imFont, int i, String string) {
      if (imFont == null) {
         return width(null, i, string);
      } else {
         float var3 = digitCell(imFont, i);
         float var4 = 0.0F;

         for (int var5 = 0; var5 < string.length(); var5++) {
            char var6 = string.charAt(var5);
            var4 += Character.isDigit(var6) ? var3 : advance(imFont, i, var6);
         }

         return var4;
      }
   }

   static void tabular(ImDrawList imDrawList, ImFont imFont, int i, float f, float g, int j, String string, boolean bl) {
      if (imFont == null) {
         text(imDrawList, null, i, f, g, j, string, bl);
      } else {
         float var8 = digitCell(imFont, i);
         float var9 = f;

         for (int var10 = 0; var10 < string.length(); var10++) {
            char var11 = string.charAt(var10);
            float var12 = advance(imFont, i, var11);
            if (Character.isDigit(var11)) {
               text(imDrawList, imFont, i, var9 + (var8 - var12) * 0.5F, g, j, glyph(var11), bl);
               var9 += var8;
            } else {
               if (var11 != ' ') {
                  text(imDrawList, imFont, i, var9, g, j, glyph(var11), bl);
               }

               var9 += var12;
            }
         }
      }
   }

   private static String glyph(char c) {
      return c < ASCII.length ? ASCII[c] : String.valueOf(c);
   }

   static int scaleAlpha(int i, float f) {
      int var2 = i >>> 24 & 0xFF;
      int var3 = Math.round(var2 * Math.max(0.0F, Math.min(1.0F, f)));
      return i & 16777215 | var3 << 24;
   }

   static {
      DIGIT_CELLS.defaultReturnValue(0.0F / 0.0F);

      for (char var0 = 0; var0 < ASCII.length; var0++) {
         ASCII[var0] = String.valueOf(var0);
      }
   }
}
