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

   public static int col(int i, float f) {
      int var2 = Math.round((i >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, f)));
      return GuiPalette.abgr(var2 << 24 | i & 16777215);
   }

   public static int mix(int i, int j, float f) {
      f = Math.max(0.0F, Math.min(1.0F, f));
      int var3 = Math.round((i >>> 24 & 0xFF) + ((j >>> 24 & 0xFF) - (i >>> 24 & 0xFF)) * f);
      int var4 = Math.round((i >> 16 & 0xFF) + ((j >> 16 & 0xFF) - (i >> 16 & 0xFF)) * f);
      int var5 = Math.round((i >> 8 & 0xFF) + ((j >> 8 & 0xFF) - (i >> 8 & 0xFF)) * f);
      int var6 = Math.round((i & 0xFF) + ((j & 0xFF) - (i & 0xFF)) * f);
      return var3 << 24 | var4 << 16 | var5 << 8 | var6;
   }

   public static void glow(ImDrawList imDrawList, float f, float g, float h, int i, float j) {
      byte var6 = 18;

      for (int var7 = 0; var7 < var6; var7++) {
         float var8 = 1.0F - (float)var7 / var6;
         imDrawList.addCircleFilled(f, g, h * var8, col(i, j / var6 * 2.2F), 48);
      }
   }

   public static void sunDisk(ImDrawList imDrawList, float f, float g, float h, float i, float j, float k) {
      glow(imDrawList, f, g, h * 1.9F, -1466054, 0.16F * k);
      imDrawList.addCircleFilled(f, g, h, col(-14018806, 0.55F * k), 96);
      imDrawList.addCircle(f, g, h, col(-15408912, 0.55F * k), 96, Math.max(1.0F, 1.4F * j));
      imDrawList.addCircle(f, g, h * 0.93F, col(-15408912, 0.22F * k), 96, Math.max(1.0F, j));
      byte var7 = 48;

      for (int var8 = 0; var8 < var7; var8++) {
         float var9 = i + (float)(var8 * 3.141592653589793 * 2.0 / var7);
         float var10 = h * 1.07F;
         float var11 = h * (var8 % 2 == 0 ? 1.34F : 1.2F);
         float var12 = (float)Math.cos(var9);
         float var13 = (float)Math.sin(var9);
         imDrawList.addLine(
            f + var12 * var10,
            g + var13 * var10,
            f + var12 * var11,
            g + var13 * var11,
            col(-15408912, (var8 % 2 == 0 ? 0.28F : 0.16F) * k),
            Math.max(1.0F, 1.3F * j)
         );
      }
   }

   public static void text(ImDrawList imDrawList, ImFont imFont, float f, float g, String string, int i) {
      if (imFont != null) {
         imDrawList.addText(imFont, Math.round(imFont.getFontSize()), f, g, i, string);
      } else {
         imDrawList.addText(f, g, i, string);
      }
   }

   public static float textWidth(ImFont imFont, String string) {
      return imFont == null ? string.length() * 7.0F : imFont.calcTextSizeAX(imFont.getFontSize(), 3.4028235E38F, 0.0F, string);
   }

   public static float spacedWidth(ImFont imFont, String string, float f) {
      float var3 = 0.0F;

      for (int var4 = 0; var4 < string.length(); var4++) {
         var3 += textWidth(imFont, String.valueOf(string.charAt(var4)));
      }

      return var3 + f * Math.max(0, string.length() - 1);
   }

   public static void drawSpaced(ImDrawList imDrawList, ImFont imFont, float f, float g, String string, float h, int i, float j, boolean bl) {
      float var9 = f - spacedWidth(imFont, string, h) * 0.5F;

      for (int var10 = 0; var10 < string.length(); var10++) {
         String var11 = String.valueOf(string.charAt(var10));
         if (bl) {
            text(imDrawList, imFont, var9 + 1.0F, g + 1.0F, var11, col(-16777216, 0.7F * j));
         }

         text(imDrawList, imFont, var9, g, var11, col(i, j));
         var9 += textWidth(imFont, var11) + h;
      }
   }

   public static ThemeImages.Texture texture(String string) {
      if (TEXTURES.containsKey(string)) {
         return TEXTURES.get(string);
      } else {
         ThemeImages.Texture var1 = null;

         try (InputStream var2 = HudDraw.class.getResourceAsStream(string)) {
            if (var2 != null) {
               var1 = ThemeImages.decode(var2.readAllBytes(), string, false);
            } else {
               LOGGER.warn("[EFN+] Missing texture {}", string);
            }
         } catch (RuntimeException | IOException var7) {
            LOGGER.warn("[EFN+] Failed to load {}", string, var7);
         }

         TEXTURES.put(string, var1);
         return var1;
      }
   }

   public static float intro(float f, float g) {
      float var2 = Math.max(0.0F, Math.min(1.0F, (f - g) / 0.6F));
      float var3 = 1.0F - var2;
      return 1.0F - var3 * var3 * var3;
   }
}
