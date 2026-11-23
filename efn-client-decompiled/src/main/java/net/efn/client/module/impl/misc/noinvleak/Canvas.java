package net.efn.client.module.impl.misc.noinvleak;

import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class Canvas implements DigitFont.Sink {
   private IntBuffer pixels;
   private int stride;
   private int width;
   private int height;

   Canvas() {
   }

   void wrap(IntBuffer intBuffer, int i, int j, int k) {
      this.pixels = intBuffer;
      this.stride = i;
      this.width = j;
      this.height = k;
   }

   @Override
   public void rect(int i, int j, int k, int l, int m) {
      int var6 = m >>> 24;
      if (var6 != 0) {
         int var7 = Math.max(i, 0);
         int var8 = Math.max(j, 0);
         int var9 = Math.min(i + k, this.width);
         int var10 = Math.min(j + l, this.height);
         if (var7 < var9 && var8 < var10) {
            int var11 = var6 == 255 ? m : premultiply(m, var6);

            for (int var12 = var8; var12 < var10; var12++) {
               int var13 = var12 * this.stride;

               for (int var14 = var7; var14 < var9; var14++) {
                  this.pixels.put(var13 + var14, var6 == 255 ? var11 : over(var11, this.pixels.get(var13 + var14)));
               }
            }
         }
      }
   }

   void copy(int[] is, int i, int j, int k, int l) {
      for (int var6 = 0; var6 < j; var6++) {
         int var9 = l + var6;
         int var7;
         int var8;
         if (var9 >= 0 && var9 < this.height && (var8 = Math.max(0, -k)) < (var7 = Math.min(i, this.width - k))) {
            this.pixels.put(var9 * this.stride + k + var8, is, var6 * i + var8, var7 - var8);
         }
      }
   }

   void blend(int[] is, int i, int j, int k, int l, int m) {
      for (int var7 = 0; var7 < j; var7++) {
         for (int var8 = 0; var8 < i; var8++) {
            this.blendPixel(is[var7 * i + var8], k + var8 * m, l + var7 * m, m);
         }
      }
   }

   void blendPixel(int i, int j, int k, int l) {
      if (i >>> 24 != 0) {
         int var5 = Math.max(j, 0);
         int var6 = Math.max(k, 0);
         int var7 = Math.min(j + l, this.width);
         int var8 = Math.min(k + l, this.height);

         for (int var9 = var6; var9 < var8; var9++) {
            int var10 = var9 * this.stride;

            for (int var11 = var5; var11 < var7; var11++) {
               this.pixels.put(var10 + var11, over(i, this.pixels.get(var10 + var11)));
            }
         }
      }
   }

   static int premultiply(int i, int j) {
      return j << 24 | (i >>> 16 & 0xFF) * j / 255 << 16 | (i >>> 8 & 0xFF) * j / 255 << 8 | (i & 0xFF) * j / 255;
   }

   static int over(int i, int j) {
      int var2 = i >>> 24;
      if (var2 == 255) {
         return i;
      } else if (var2 == 0) {
         return j;
      } else {
         int var3 = 255 - var2;
         return var2 + (j >>> 24) * var3 / 255 << 24
            | (i >>> 16 & 0xFF) + (j >>> 16 & 0xFF) * var3 / 255 << 16
            | (i >>> 8 & 0xFF) + (j >>> 8 & 0xFF) * var3 / 255 << 8
            | (i & 0xFF) + (j & 0xFF) * var3 / 255;
      }
   }
}
