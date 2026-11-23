package net.efn.client.module.impl.misc.noinvleak;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
final class OverlayArt {
   private static final int GLYPH = 8;
   private static final int SHADOW_MASK = 16579836;
   private static OverlayArt.Image selection;
   private static OverlayArt.Slice tooltipBackground;
   private static OverlayArt.Slice tooltipFrame;
   private static boolean[] glyphs;
   private static boolean loaded;

   private OverlayArt() {
   }

   static void reset() {
      selection = null;
      tooltipBackground = null;
      tooltipFrame = null;
      glyphs = null;
      loaded = false;
   }

   static OverlayArt.Image selection() {
      load();
      return selection;
   }

   static OverlayArt.Slice tooltipBackground() {
      load();
      return tooltipBackground;
   }

   static OverlayArt.Slice tooltipFrame() {
      load();
      return tooltipFrame;
   }

   private static void load() {
      if (!loaded) {
         loaded = true;
         selection = image("textures/gui/sprites/hud/hotbar_selection.png");
         tooltipBackground = slice("textures/gui/sprites/tooltip/background.png", 9, false);
         tooltipFrame = slice("textures/gui/sprites/tooltip/frame.png", 10, true);
         OverlayArt.Image var0 = image("textures/font/ascii.png");
         if (var0 != null) {
            glyphs = new boolean[var0.width() * var0.height()];

            for (int var1 = 0; var1 < glyphs.length; var1++) {
               glyphs[var1] = var0.pixels()[var1] >>> 24 != 0;
            }
         }
      }
   }

   private static OverlayArt.Image image(String string) {
      Resource var1 = MinecraftClient.getInstance().getResourceManager().getResource(Identifier.ofVanilla(string)).orElse(null);
      if (var1 == null) {
         return null;
      } else {
         try {
            OverlayArt.Image var14;
            try (InputStream var2 = var1.getInputStream()) {
               NativeImage var4 = NativeImage.read(var2);

               OverlayArt.Image var3;
               label58: {
                  try {
                     int[] var5 = var4.copyPixelsAbgr();
                     int[] var6 = new int[var5.length];

                     for (int var7 = 0; var7 < var5.length; var7++) {
                        int var8 = var5[var7] >>> 24;
                        var6[var7] = Canvas.premultiply(
                           var8 << 24 | (var5[var7] & 0xFF) << 16 | (var5[var7] >>> 8 & 0xFF) << 8 | var5[var7] >>> 16 & 0xFF, var8
                        );
                     }

                     var3 = new OverlayArt.Image(var6, var4.getWidth(), var4.getHeight());
                     if (var4 == null) {
                        break label58;
                     }
                  } catch (Throwable var11) {
                     if (var4 != null) {
                        try {
                           var4.close();
                        } catch (Throwable var10) {
                           var11.addSuppressed(var10);
                        }
                     }

                     throw var11;
                  }

                  var4.close();
               }

               var14 = var3;
            }

            return var14;
         } catch (RuntimeException | IOException var13) {
            return null;
         }
      }
   }

   private static OverlayArt.Slice slice(String string, int i, boolean bl) {
      OverlayArt.Image var3 = image(string);
      if (var3 == null) {
         return null;
      } else {
         Resource var4 = MinecraftClient.getInstance().getResourceManager().getResource(Identifier.ofVanilla(string + ".mcmeta")).orElse(null);
         if (var4 != null) {
            try (BufferedReader var5 = var4.getReader()) {
               JsonObject var6 = JsonParser.parseReader(var5).getAsJsonObject().getAsJsonObject("gui").getAsJsonObject("scaling");
               if (var6.has("border")) {
                  i = var6.get("border").getAsInt();
               }

               bl = var6.has("stretch_inner") && var6.get("stretch_inner").getAsBoolean();
            } catch (RuntimeException | IOException var10) {
            }
         }

         return new OverlayArt.Slice(var3, i, bl);
      }
   }

   static void slice(Canvas canvas, OverlayArt.Slice slice, int i, int j, int k, int l, int m) {
      if (slice != null) {
         OverlayArt.Image var8 = slice.image();
         int var9 = slice.border();
         int var10 = var8.width() - 2 * var9;
         int var11 = var8.height() - 2 * var9;
         if (var10 > 0 && var11 > 0) {
            int[] var12 = new int[k];
            int[] var13 = new int[l];

            for (int var14 = 0; var14 < k; var14++) {
               var12[var14] = sourceAt(var14, k, var9, var10, slice.stretch());
            }

            for (int var7 = 0; var7 < l; var7++) {
               var13[var7] = sourceAt(var7, l, var9, var11, slice.stretch());
            }

            for (int var16 = 0; var16 < l; var16++) {
               for (int var17 = 0; var17 < k; var17++) {
                  int var15 = var8.pixels()[var13[var16] * var8.width() + var12[var17]];
                  if (var15 >>> 24 != 0) {
                     canvas.blendPixel(var15, (i + var17) * m, (j + var16) * m, m);
                  }
               }
            }
         }
      }
   }

   private static int sourceAt(int i, int j, int k, int l, boolean bl) {
      if (i < k) {
         return i;
      } else if (i >= j - k) {
         return k + l + (i - (j - k));
      } else {
         int var5 = i - k;
         int var6 = j - 2 * k;
         return k + (bl ? var5 * l / Math.max(var6, 1) : var5 % l);
      }
   }

   static void text(Canvas canvas, Text text, int i, int j, int k, int l, boolean bl) {
      load();
      if (glyphs != null) {
         int[] var7 = new int[]{i};
         text.visit((style, string) -> {
            int var8 = style.getColor() != null ? l & 0xFF000000 | style.getColor().getRgb() : l;
            int var9 = 0;

            while (var9 < string.length()) {
               int var10 = string.codePointAt(var9);
               var9 += Character.charCount(var10);
               int var11 = advance(var10, style);
               if (var10 < 128 && var10 > 32 && !style.isObfuscated()) {
                  if (bl) {
                     glyph(canvas, var10, var7[0] + 1, j + 1, k, var8 & 0xFF000000 | (var8 & 16579836) >> 2, style);
                  }

                  glyph(canvas, var10, var7[0], j, k, var8, style);
               }

               if (style.isStrikethrough()) {
                  canvas.rect(var7[0] * k, (j + 4) * k, var11 * k, k, var8);
               }

               if (style.isUnderlined()) {
                  canvas.rect(var7[0] * k, (j + 8) * k, var11 * k, k, var8);
               }

               var7[0] += var11;
            }

            return Optional.empty();
         }, Style.EMPTY);
      }
   }

   private static int advance(int i, Style style) {
      return MinecraftClient.getInstance().textRenderer.getWidth(StringVisitable.styled(new String(Character.toChars(i)), style));
   }

   private static void glyph(Canvas canvas, int i, int j, int k, int l, int m, Style style) {
      int var7 = i % 16 * 8;
      int var8 = i / 16 * 8;
      short var9 = 128;

      for (int var10 = 0; var10 < 8; var10++) {
         int var11 = style.isItalic() && var10 < 4 ? 1 : 0;
         int var12 = 0;

         while (var12 < 8) {
            if (!glyphs[(var8 + var10) * var9 + var7 + var12]) {
               var12++;
            } else {
               int var13 = var12;

               while (var12 < 8 && glyphs[(var8 + var10) * var9 + var7 + var12]) {
                  var12++;
               }

               canvas.rect((j + var13 + var11) * l, (k + var10) * l, (var12 - var13) * l, l, m);
               if (style.isBold()) {
                  canvas.rect((j + var13 + var11 + 1) * l, (k + var10) * l, (var12 - var13) * l, l, m);
               }
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   record Image(int[] pixels, int width, int height) {
   }

   @Environment(EnvType.CLIENT)
   record Slice(OverlayArt.Image image, int border, boolean stretch) {
   }
}
