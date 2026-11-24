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

   static void decorations(DigitFont.Sink sink, ItemStack itemStack, int i, int j, int k) {
      if (itemStack.isItemBarVisible()) {
         sink.rect(i + 2 * k, j + 13 * k, 13 * k, 2 * k, -16777216);
         sink.rect(i + 2 * k, j + 13 * k, itemStack.getItemBarStep() * k, k, 0xFF000000 | itemStack.getItemBarColor());
      }

      if (itemStack.getCount() != 1) {
         count(sink, itemStack.getCount(), i, j, k);
      }
   }

   private static void count(DigitFont.Sink sink, int i, int j, int k, int l) {
      String var5 = Integer.toString(i);
      int var6 = j + (17 - 6 * var5.length()) * l;
      int var7 = k + 9 * l;

      for (int var8 = 0; var8 < var5.length(); var8++) {
         int var9 = var5.charAt(var8) - '0';
         if (var9 >= 0 && var9 <= 9) {
            int var10 = var6 + var8 * 6 * l;
            glyph(sink, DIGITS[var9], var10 + l, var7 + l, l, -12632257);
            glyph(sink, DIGITS[var9], var10, var7, l, -1);
         }
      }
   }

   private static void glyph(DigitFont.Sink sink, byte[] bs, int i, int j, int k, int l) {
      for (int var6 = 0; var6 < bs.length; var6++) {
         int var7 = 0;

         while (var7 < 5) {
            if ((bs[var6] >> 4 - var7 & 1) == 0) {
               var7++;
            } else {
               int var8 = var7;

               while (var7 < 5 && (bs[var6] >> 4 - var7 & 1) != 0) {
                  var7++;
               }

               sink.rect(i + var8 * k, j + var6 * k, (var7 - var8) * k, k, l);
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   interface Sink {
      void rect(int i, int j, int k, int l, int m);
   }
}
