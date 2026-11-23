package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EfnTheme {
   public static final float CATEGORY_FONT_SIZE = 17.0F;
   public static final float MODULE_FONT_SIZE = 16.0F;
   public static final float TITLE_FONT_SIZE = 24.0F;
   public static final float SMALL_FONT_SIZE = 13.0F;
   public static final int GUI_SURFACE = -13226454;
   public static final int GUI_SURFACE_HOVER = -12240588;
   public static final int GUI_DIVIDER = 268435455;
   public static final int GUI_TEXT = -461582;
   public static final int GUI_TEXT_ROW = -2042416;
   public static final int GUI_TEXT_MUTED = -4542808;
   public static final int GUI_SWITCH_OFF = -11911366;
   public static final int GUI_KNOB = -461067;
   public static final int GUI_SHADOW = 335544320;
   public static final int HUD_LABEL = -7106930;

   private EfnTheme() {
   }

   public static int lerpColor(int var0, int var1, float var2) {
      int var3 = var0 & 0xFF;
      int var4 = var1 & 0xFF;
      int var5 = var0 >> 8 & 0xFF;
      int var6 = var1 >> 8 & 0xFF;
      int var7 = var0 >> 16 & 0xFF;
      int var8 = var1 >> 16 & 0xFF;
      int var9 = var0 >>> 24;
      int var10 = var1 >>> 24;
      return (int)(var9 + (var10 - var9) * var2) << 24
         | (int)(var7 + (var8 - var7) * var2) << 16
         | (int)(var5 + (var6 - var5) * var2) << 8
         | (int)(var3 + (var4 - var3) * var2);
   }
}
