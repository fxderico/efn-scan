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

   void wrap(IntBuffer var1, int var2, int var3, int var4) {
      this.pixels = var1;
      this.stride = var2;
      this.width = var3;
      this.height = var4;
   }

   @Override
   public void rect(int var1, int var2, int var3, int var4, int var5) {
      int var6 = var5 >>> 24;
      if (var6 != 0) {
         int var7 = Math.max(var1, 0);
         int var8 = Math.max(var2, 0);
         int var9 = Math.min(var1 + var3, this.width);
         int var10 = Math.min(var2 + var4, this.height);
         if (var7 < var9 && var8 < var10) {
            int var11 = var6 == 255 ? var5 : premultiply(var5, var6);

            for (int var12 = var8; var12 < var10; var12++) {
               int var13 = var12 * this.stride;

               for (int var14 = var7; var14 < var9; var14++) {
                  this.pixels.put(var13 + var14, var6 == 255 ? var11 : over(var11, this.pixels.get(var13 + var14)));
               }
            }
         }
      }
   }

   void copy(int[] var1, int var2, int var3, int var4, int var5) {
      for (int var6 = 0; var6 < var3; var6++) {
         int var9 = var5 + var6;
         int var7;
         int var8;
         if (var9 >= 0 && var9 < this.height && (var8 = Math.max(0, -var4)) < (var7 = Math.min(var2, this.width - var4))) {
            this.pixels.put(var9 * this.stride + var4 + var8, var1, var6 * var2 + var8, var7 - var8);
         }
      }
   }

   void blend(int[] var1, int var2, int var3, int var4, int var5, int var6) {
      for (int var7 = 0; var7 < var3; var7++) {
         for (int var8 = 0; var8 < var2; var8++) {
            this.blendPixel(var1[var7 * var2 + var8], var4 + var8 * var6, var5 + var7 * var6, var6);
         }
      }
   }

   void blendPixel(int var1, int var2, int var3, int var4) {
      if (var1 >>> 24 != 0) {
         int var5 = Math.max(var2, 0);
         int var6 = Math.max(var3, 0);
         int var7 = Math.min(var2 + var4, this.width);
         int var8 = Math.min(var3 + var4, this.height);

         for (int var9 = var6; var9 < var8; var9++) {
            int var10 = var9 * this.stride;

            for (int var11 = var5; var11 < var7; var11++) {
               this.pixels.put(var10 + var11, over(var1, this.pixels.get(var10 + var11)));
            }
         }
      }
   }

   static int premultiply(int var0, int var1) {
      return var1 << 24 | (var0 >>> 16 & 0xFF) * var1 / 255 << 16 | (var0 >>> 8 & 0xFF) * var1 / 255 << 8 | (var0 & 0xFF) * var1 / 255;
   }

   static int over(int var0, int var1) {
      int var2 = var0 >>> 24;
      if (var2 == 255) {
         return var0;
      } else if (var2 == 0) {
         return var1;
      } else {
         int var3 = 255 - var2;
         return var2 + (var1 >>> 24) * var3 / 255 << 24
            | (var0 >>> 16 & 0xFF) + (var1 >>> 16 & 0xFF) * var3 / 255 << 16
            | (var0 >>> 8 & 0xFF) + (var1 >>> 8 & 0xFF) * var3 / 255 << 8
            | (var0 & 0xFF) + (var1 & 0xFF) * var3 / 255;
      }
   }
}
