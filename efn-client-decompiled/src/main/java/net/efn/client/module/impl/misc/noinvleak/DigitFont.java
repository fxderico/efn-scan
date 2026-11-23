package net.efn.client.module.impl.misc.noinvleak;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
final class DigitFont {
   private static final byte[][] DIGITS = new byte[][]{
      {14, 17, 19, 21, 25, 17, 14},
      {4, 12, 4, 4, 4, 4, 14},
      {14, 17, 1, 6, 8, 16, 31},
      {14, 17, 1, 6, 1, 17, 14},
      {2, 6, 10, 18, 31, 2, 2},
      {31, 16, 30, 1, 1, 17, 14},
      {14, 16, 30, 17, 17, 17, 14},
      {31, 1, 2, 4, 8, 8, 8},
      {14, 17, 17, 14, 17, 17, 14},
      {14, 17, 17, 15, 1, 17, 14}
   };
   private static final int SHADOW = -12632257;
   private static final int WHITE = -1;

   private DigitFont() {
   }

   static void decorations(DigitFont.Sink var0, ItemStack var1, int var2, int var3, int var4) {
      if (var1.method_31578()) {
         var0.rect(var2 + 2 * var4, var3 + 13 * var4, 13 * var4, 2 * var4, -16777216);
         var0.rect(var2 + 2 * var4, var3 + 13 * var4, var1.method_31579() * var4, var4, 0xFF000000 | var1.method_31580());
      }

      if (var1.method_7947() != 1) {
         count(var0, var1.method_7947(), var2, var3, var4);
      }
   }

   private static void count(DigitFont.Sink var0, int var1, int var2, int var3, int var4) {
      String var5 = Integer.toString(var1);
      int var6 = var2 + (17 - 6 * var5.length()) * var4;
      int var7 = var3 + 9 * var4;

      for (int var8 = 0; var8 < var5.length(); var8++) {
         int var9 = var5.charAt(var8) - '0';
         if (var9 >= 0 && var9 <= 9) {
            int var10 = var6 + var8 * 6 * var4;
            glyph(var0, DIGITS[var9], var10 + var4, var7 + var4, var4, -12632257);
            glyph(var0, DIGITS[var9], var10, var7, var4, -1);
         }
      }
   }

   private static void glyph(DigitFont.Sink var0, byte[] var1, int var2, int var3, int var4, int var5) {
      for (int var6 = 0; var6 < var1.length; var6++) {
         int var7 = 0;

         while (var7 < 5) {
            if ((var1[var6] >> 4 - var7 & 1) == 0) {
               var7++;
            } else {
               int var8 = var7;

               while (var7 < 5 && (var1[var6] >> 4 - var7 & 1) != 0) {
                  var7++;
               }

               var0.rect(var2 + var8 * var4, var3 + var6 * var4, (var7 - var8) * var4, var4, var5);
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   interface Sink {
      void rect(int var1, int var2, int var3, int var4, int var5);
   }
}
