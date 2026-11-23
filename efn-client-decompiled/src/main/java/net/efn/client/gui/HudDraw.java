package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class HudDraw {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   public static final int GOLD = -15408912;
   public static final int GOLD_LIGHT = -6621958;
   public static final int GOLD_TEXT = -6557705;
   public static final int LACQUER = -16381684;
   public static final int TAN = -3812390;
   public static final int TAN_PALE = -1839888;
   public static final int TAN_DARK = -6772301;
   public static final int DEEP = -13738055;
   public static final int DEEP_LIGHT = -12750874;
   public static final int DEEP_DEEP = -14994064;
   public static final int TURQUOISE = -12794702;
   public static final int MALACHITE = -12600956;
   public static final int CARNELIAN = -2537681;
   public static final int TEXT = -1575946;
   public static final int MUTED = -7693914;
   public static final int BLACK = -16777216;
   private static final Map<String, ThemeImages.Texture> TEXTURES = new HashMap<>();

   private HudDraw() {
   }

   public static int col(int var0, float var1) {
      int var2 = Math.round((var0 >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, var1)));
      return GuiPalette.abgr(var2 << 24 | var0 & 16777215);
   }

   public static int mix(int var0, int var1, float var2) {
      var2 = Math.max(0.0F, Math.min(1.0F, var2));
      int var3 = Math.round((var0 >>> 24 & 0xFF) + ((var1 >>> 24 & 0xFF) - (var0 >>> 24 & 0xFF)) * var2);
      int var4 = Math.round((var0 >> 16 & 0xFF) + ((var1 >> 16 & 0xFF) - (var0 >> 16 & 0xFF)) * var2);
      int var5 = Math.round((var0 >> 8 & 0xFF) + ((var1 >> 8 & 0xFF) - (var0 >> 8 & 0xFF)) * var2);
      int var6 = Math.round((var0 & 0xFF) + ((var1 & 0xFF) - (var0 & 0xFF)) * var2);
      return var3 << 24 | var4 << 16 | var5 << 8 | var6;
   }

   public static void glow(ImDrawList var0, float var1, float var2, float var3, int var4, float var5) {
      byte var6 = 18;

      for (int var7 = 0; var7 < var6; var7++) {
         float var8 = 1.0F - (float)var7 / var6;
         var0.addCircleFilled(var1, var2, var3 * var8, col(var4, var5 / var6 * 2.2F), 48);
      }
   }

   public static void sunDisk(ImDrawList var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      glow(var0, var1, var2, var3 * 1.9F, -1466054, 0.16F * var6);
      var0.addCircleFilled(var1, var2, var3, col(-14018806, 0.55F * var6), 96);
      var0.addCircle(var1, var2, var3, col(-15408912, 0.55F * var6), 96, Math.max(1.0F, 1.4F * var5));
      var0.addCircle(var1, var2, var3 * 0.93F, col(-15408912, 0.22F * var6), 96, Math.max(1.0F, var5));
      byte var7 = 48;

      for (int var8 = 0; var8 < var7; var8++) {
         float var9 = var4 + (float)(var8 * 3.141592653589793 * 2.0 / var7);
         float var10 = var3 * 1.07F;
         float var11 = var3 * (var8 % 2 == 0 ? 1.34F : 1.2F);
         float var12 = (float)Math.cos(var9);
         float var13 = (float)Math.sin(var9);
         var0.addLine(
            var1 + var12 * var10,
            var2 + var13 * var10,
            var1 + var12 * var11,
            var2 + var13 * var11,
            col(-15408912, (var8 % 2 == 0 ? 0.28F : 0.16F) * var6),
            Math.max(1.0F, 1.3F * var5)
         );
      }
   }

   public static void text(ImDrawList var0, ImFont var1, float var2, float var3, String var4, int var5) {
      if (var1 != null) {
         var0.addText(var1, Math.round(var1.getFontSize()), var2, var3, var5, var4);
      } else {
         var0.addText(var2, var3, var5, var4);
      }
   }

   public static float textWidth(ImFont var0, String var1) {
      return var0 == null ? var1.length() * 7.0F : var0.calcTextSizeAX(var0.getFontSize(), 3.4028235E38F, 0.0F, var1);
   }

   public static float spacedWidth(ImFont var0, String var1, float var2) {
      float var3 = 0.0F;

      for (int var4 = 0; var4 < var1.length(); var4++) {
         var3 += textWidth(var0, String.valueOf(var1.charAt(var4)));
      }

      return var3 + var2 * Math.max(0, var1.length() - 1);
   }

   public static void drawSpaced(ImDrawList var0, ImFont var1, float var2, float var3, String var4, float var5, int var6, float var7, boolean var8) {
      float var9 = var2 - spacedWidth(var1, var4, var5) * 0.5F;

      for (int var10 = 0; var10 < var4.length(); var10++) {
         String var11 = String.valueOf(var4.charAt(var10));
         if (var8) {
            text(var0, var1, var9 + 1.0F, var3 + 1.0F, var11, col(-16777216, 0.7F * var7));
         }

         text(var0, var1, var9, var3, var11, col(var6, var7));
         var9 += textWidth(var1, var11) + var5;
      }
   }

   public static ThemeImages.Texture texture(String var0) {
      if (TEXTURES.containsKey(var0)) {
         return TEXTURES.get(var0);
      } else {
         ThemeImages.Texture var1 = null;

         try (InputStream var2 = HudDraw.class.getResourceAsStream(var0)) {
            if (var2 != null) {
               var1 = ThemeImages.decode(var2.readAllBytes(), var0, false);
            } else {
               LOGGER.warn("[EFN+] Missing texture {}", var0);
            }
         } catch (RuntimeException | IOException var7) {
            LOGGER.warn("[EFN+] Failed to load {}", var0, var7);
         }

         TEXTURES.put(var0, var1);
         return var1;
      }
   }

   public static float intro(float var0, float var1) {
      float var2 = Math.max(0.0F, Math.min(1.0F, (var0 - var1) / 0.6F));
      float var3 = 1.0F - var2;
      return 1.0F - var3 * var3 * var3;
   }
}
