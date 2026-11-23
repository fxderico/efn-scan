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

   private static OverlayArt.Image image(String var0) {
      Resource var1 = (Resource)MinecraftClient.method_1551().method_1478().method_14486(Identifier.method_60656(var0)).orElse(null);
      if (var1 == null) {
         return null;
      } else {
         try {
            OverlayArt.Image var14;
            try (InputStream var2 = var1.method_14482()) {
               NativeImage var4 = NativeImage.method_4309(var2);

               OverlayArt.Image var3;
               label58: {
                  try {
                     int[] var5 = var4.method_48463();
                     int[] var6 = new int[var5.length];

                     for (int var7 = 0; var7 < var5.length; var7++) {
                        int var8 = var5[var7] >>> 24;
                        var6[var7] = Canvas.premultiply(
                           var8 << 24 | (var5[var7] & 0xFF) << 16 | (var5[var7] >>> 8 & 0xFF) << 8 | var5[var7] >>> 16 & 0xFF, var8
                        );
                     }

                     var3 = new OverlayArt.Image(var6, var4.method_4307(), var4.method_4323());
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

   private static OverlayArt.Slice slice(String var0, int var1, boolean var2) {
      OverlayArt.Image var3 = image(var0);
      if (var3 == null) {
         return null;
      } else {
         Resource var4 = (Resource)MinecraftClient.method_1551().method_1478().method_14486(Identifier.method_60656(var0 + ".mcmeta")).orElse(null);
         if (var4 != null) {
            try (BufferedReader var5 = var4.method_43039()) {
               JsonObject var6 = JsonParser.parseReader(var5).getAsJsonObject().getAsJsonObject("gui").getAsJsonObject("scaling");
               if (var6.has("border")) {
                  var1 = var6.get("border").getAsInt();
               }

               var2 = var6.has("stretch_inner") && var6.get("stretch_inner").getAsBoolean();
            } catch (RuntimeException | IOException var10) {
            }
         }

         return new OverlayArt.Slice(var3, var1, var2);
      }
   }

   static void slice(Canvas var0, OverlayArt.Slice var1, int var2, int var3, int var4, int var5, int var6) {
      if (var1 != null) {
         OverlayArt.Image var8 = var1.image();
         int var9 = var1.border();
         int var10 = var8.width() - 2 * var9;
         int var11 = var8.height() - 2 * var9;
         if (var10 > 0 && var11 > 0) {
            int[] var12 = new int[var4];
            int[] var13 = new int[var5];

            for (int var14 = 0; var14 < var4; var14++) {
               var12[var14] = sourceAt(var14, var4, var9, var10, var1.stretch());
            }

            for (int var7 = 0; var7 < var5; var7++) {
               var13[var7] = sourceAt(var7, var5, var9, var11, var1.stretch());
            }

            for (int var16 = 0; var16 < var5; var16++) {
               for (int var17 = 0; var17 < var4; var17++) {
                  int var15 = var8.pixels()[var13[var16] * var8.width() + var12[var17]];
                  if (var15 >>> 24 != 0) {
                     var0.blendPixel(var15, (var2 + var17) * var6, (var3 + var16) * var6, var6);
                  }
               }
            }
         }
      }
   }

   private static int sourceAt(int var0, int var1, int var2, int var3, boolean var4) {
      if (var0 < var2) {
         return var0;
      } else if (var0 >= var1 - var2) {
         return var2 + var3 + (var0 - (var1 - var2));
      } else {
         int var5 = var0 - var2;
         int var6 = var1 - 2 * var2;
         return var2 + (var4 ? var5 * var3 / Math.max(var6, 1) : var5 % var3);
      }
   }

   static void text(Canvas var0, Text var1, int var2, int var3, int var4, int var5, boolean var6) {
      load();
      if (glyphs != null) {
         int[] var7 = new int[]{var2};
         var1.method_27658((var6x, var7x) -> {
            int var8 = var6x.method_10973() != null ? var5 & 0xFF000000 | var6x.method_10973().method_27716() : var5;
            int var9 = 0;

            while (var9 < var7x.length()) {
               int var10 = var7x.codePointAt(var9);
               var9 += Character.charCount(var10);
               int var11 = advance(var10, var6x);
               if (var10 < 128 && var10 > 32 && !var6x.method_10987()) {
                  if (var6) {
                     glyph(var0, var10, var7[0] + 1, var3 + 1, var4, var8 & 0xFF000000 | (var8 & 16579836) >> 2, var6x);
                  }

                  glyph(var0, var10, var7[0], var3, var4, var8, var6x);
               }

               if (var6x.method_10986()) {
                  var0.rect(var7[0] * var4, (var3 + 4) * var4, var11 * var4, var4, var8);
               }

               if (var6x.method_10965()) {
                  var0.rect(var7[0] * var4, (var3 + 8) * var4, var11 * var4, var4, var8);
               }

               var7[0] += var11;
            }

            return Optional.empty();
         }, Style.field_24360);
      }
   }

   private static int advance(int var0, Style var1) {
      return MinecraftClient.method_1551().field_1772.method_27525(StringVisitable.method_29431(new String(Character.toChars(var0)), var1));
   }

   private static void glyph(Canvas var0, int var1, int var2, int var3, int var4, int var5, Style var6) {
      int var7 = var1 % 16 * 8;
      int var8 = var1 / 16 * 8;
      short var9 = 128;

      for (int var10 = 0; var10 < 8; var10++) {
         int var11 = var6.method_10966() && var10 < 4 ? 1 : 0;
         int var12 = 0;

         while (var12 < 8) {
            if (!glyphs[(var8 + var10) * var9 + var7 + var12]) {
               var12++;
            } else {
               int var13 = var12;

               while (var12 < 8 && glyphs[(var8 + var10) * var9 + var7 + var12]) {
                  var12++;
               }

               var0.rect((var2 + var13 + var11) * var4, (var3 + var10) * var4, (var12 - var13) * var4, var4, var5);
               if (var6.method_10984()) {
                  var0.rect((var2 + var13 + var11 + 1) * var4, (var3 + var10) * var4, (var12 - var13) * var4, var4, var5);
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
