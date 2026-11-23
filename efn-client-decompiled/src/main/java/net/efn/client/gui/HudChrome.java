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

   static void card(ImDrawList var0, Surface var1, float var2, float var3, float var4, float var5, float var6, float var7, boolean var8, float var9) {
      if (!(var7 <= 0.001F)) {
         if (var8) {
            shadow(var0, var2, var3, var4, var5, var6, var7, var9);
         }

         var0.addRectFilled(var2, var3, var2 + var4, var3 + var5, scaleAlpha(GuiPalette.fill(var1), var7), var6);
         SurfaceBackground.draw(var0, var1, var2, var3, var4, var5, var6, var7);
         var0.addRect(var2, var3, var2 + var4, var3 + var5, scaleAlpha(402653183, var7), var6, 0, 1.0F);
         int var10 = GuiPalette.accent();
         float var11 = Math.max(1.5F, 1.5F * var9);
         float var12 = Math.min(var4 * 0.5F, 56.0F * Math.max(1.0F, var9));
         float var13 = var2 + Math.min(var6, var4 * 0.2F);
         var0.addRectFilledMultiColor(
            var13,
            var3,
            var13 + var12,
            var3 + var11,
            scaleAlpha(var10, var7 * 0.95F),
            scaleAlpha(var10, 0.0F),
            scaleAlpha(var10, 0.0F),
            scaleAlpha(var10, var7 * 0.95F)
         );
      }
   }

   static void shadow(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      float var8 = Math.max(1.0F, var7);
      var0.addRectFilled(var1 - var8, var2 - 0.5F * var8, var1 + var3 + var8, var2 + var4 + 1.5F * var8, scaleAlpha(771751936, var6), var5 + var8);
      var0.addRectFilled(
         var1 - 2.0F * var8, var2 + 0.5F * var8, var1 + var3 + 2.0F * var8, var2 + var4 + 3.5F * var8, scaleAlpha(385875968, var6), var5 + 2.0F * var8
      );
      var0.addRectFilled(
         var1 - 3.0F * var8, var2 + 1.5F * var8, var1 + var3 + 3.0F * var8, var2 + var4 + 6.0F * var8, scaleAlpha(201326592, var6), var5 + 3.0F * var8
      );
   }

   static void divider(ImDrawList var0, float var1, float var2, float var3, float var4) {
      float var5 = Math.round(var1);
      float var6 = Math.round(var2 - var3 * 0.5F);
      var0.addRectFilled(var5, var6, var5 + 1.0F, var6 + Math.round(var3), scaleAlpha(536870911, var4));
   }

   static boolean needsTextGuard(Surface var0) {
      return var0 != null && (var0.opacity.get() < 0.55F || var0.isImage());
   }

   static int px(float var0, float var1) {
      return Math.max(6, Math.round(var0 * var1));
   }

   static float width(ImFont var0, int var1, String var2) {
      return var0 != null && !var2.isEmpty() ? var0.calcTextSizeA(var1, 3.4028235E38F, 0.0F, var2).x : var2.length() * var1 * 0.55F;
   }

   static float ascent(ImFont var0, int var1) {
      return var0 != null && !(var0.getFontSize() <= 0.0F) ? var0.getAscent() * var1 / var0.getFontSize() : var1 * 0.77F;
   }

   static float baselineOffset(ImFont var0, int var1, ImFont var2, int var3) {
      return Math.round(ascent(var0, var1) - ascent(var2, var3));
   }

   static void text(ImDrawList var0, ImFont var1, int var2, float var3, float var4, int var5, String var6, boolean var7) {
      if (!var6.isEmpty() && (var5 >>> 24 & 0xFF) != 0) {
         if (var7) {
            raw(var0, var1, var2, var3 + 1.0F, var4 + 1.0F, scaleAlpha(-1946157056, (var5 >>> 24 & 0xFF) / 255.0F), var6);
         }

         raw(var0, var1, var2, var3, var4, var5, var6);
      }
   }

   private static void raw(ImDrawList var0, ImFont var1, int var2, float var3, float var4, int var5, String var6) {
      if (var1 != null) {
         var0.addText(var1, var2, var3, var4, var5, var6);
      } else {
         var0.addText(var3, var4, var5, var6);
      }
   }

   private static float digitCell(ImFont var0, int var1) {
      if (var0 == null) {
         return var1 * 0.6F;
      } else {
         long var2 = (long)System.identityHashCode(var0) << 16 ^ var1;
         float var4 = DIGIT_CELLS.get(var2);
         if (!Float.isNaN(var4)) {
            return var4;
         } else {
            float var5 = 0.0F;

            for (char var6 = '0'; var6 <= '9'; var6++) {
               var5 = Math.max(var5, advance(var0, var1, var6));
            }

            if (DIGIT_CELLS.size() > 64) {
               DIGIT_CELLS.clear();
            }

            DIGIT_CELLS.put(var2, var5);
            return var5;
         }
      }
   }

   private static float advance(ImFont var0, int var1, char var2) {
      return var0.getCharAdvance(var2) * var1 / var0.getFontSize();
   }

   static float tabularWidth(ImFont var0, int var1, String var2) {
      if (var0 == null) {
         return width(null, var1, var2);
      } else {
         float var3 = digitCell(var0, var1);
         float var4 = 0.0F;

         for (int var5 = 0; var5 < var2.length(); var5++) {
            char var6 = var2.charAt(var5);
            var4 += Character.isDigit(var6) ? var3 : advance(var0, var1, var6);
         }

         return var4;
      }
   }

   static void tabular(ImDrawList var0, ImFont var1, int var2, float var3, float var4, int var5, String var6, boolean var7) {
      if (var1 == null) {
         text(var0, null, var2, var3, var4, var5, var6, var7);
      } else {
         float var8 = digitCell(var1, var2);
         float var9 = var3;

         for (int var10 = 0; var10 < var6.length(); var10++) {
            char var11 = var6.charAt(var10);
            float var12 = advance(var1, var2, var11);
            if (Character.isDigit(var11)) {
               text(var0, var1, var2, var9 + (var8 - var12) * 0.5F, var4, var5, glyph(var11), var7);
               var9 += var8;
            } else {
               if (var11 != ' ') {
                  text(var0, var1, var2, var9, var4, var5, glyph(var11), var7);
               }

               var9 += var12;
            }
         }
      }
   }

   private static String glyph(char var0) {
      return var0 < ASCII.length ? ASCII[var0] : String.valueOf(var0);
   }

   static int scaleAlpha(int var0, float var1) {
      int var2 = var0 >>> 24 & 0xFF;
      int var3 = Math.round(var2 * Math.max(0.0F, Math.min(1.0F, var1)));
      return var0 & 16777215 | var3 << 24;
   }

   static {
      DIGIT_CELLS.defaultReturnValue(0.0F / 0.0F);

      for (char var0 = 0; var0 < ASCII.length; var0++) {
         ASCII[var0] = String.valueOf(var0);
      }
   }
}
