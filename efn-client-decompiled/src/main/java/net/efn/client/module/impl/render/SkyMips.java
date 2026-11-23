package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class SkyMips {
   private static final int MAX_LEVELS = 6;

   private SkyMips() {
   }

   static int size(int var0, int var1) {
      return Math.max(1, var0 >> var1);
   }

   static int[][] chain(int[] var0, int var1, int var2) {
      int var3 = 1;

      while (var3 < 6 && Math.min(var1, var2) >> var3 >= 1) {
         var3++;
      }

      int[][] var4 = new int[var3][];
      var4[0] = var0;

      for (int var5 = 1; var5 < var3; var5++) {
         int var6 = size(var1, var5 - 1);
         int var7 = size(var2, var5 - 1);
         int var8 = size(var1, var5);
         int var9 = size(var2, var5);
         int[] var10 = var4[var5 - 1];
         int[] var11 = new int[var8 * var9];

         for (int var12 = 0; var12 < var9; var12++) {
            for (int var13 = 0; var13 < var8; var13++) {
               float var14 = 0.0F;
               float var15 = 0.0F;
               float var16 = 0.0F;
               float var17 = 0.0F;
               float var18 = 0.0F;
               float var19 = 0.0F;
               float var20 = 0.0F;

               for (int var21 = 0; var21 < 2; var21++) {
                  int var22 = Math.min(var7 - 1, var12 * 2 + var21);

                  for (int var23 = 0; var23 < 2; var23++) {
                     int var24 = var10[var22 * var6 + Math.min(var6 - 1, var13 * 2 + var23)];
                     float var25 = (var24 >>> 24) / 255.0F;
                     var14 += var25;
                     var15 += var25 * (var24 >> 16 & 0xFF);
                     var16 += var25 * (var24 >> 8 & 0xFF);
                     var17 += var25 * (var24 & 0xFF);
                     var18 += var24 >> 16 & 0xFF;
                     var19 += var24 >> 8 & 0xFF;
                     var20 += var24 & 0xFF;
                  }
               }

               int var26 = Math.min(255, Math.round(var14 * 0.25F * 255.0F));
               if (var14 <= 1.0E-4F) {
                  var14 = 4.0F;
                  var15 = var18;
                  var16 = var19;
                  var17 = var20;
               }

               var11[var12 * var8 + var13] = var26 << 24
                  | Math.min(255, Math.round(var15 / var14)) << 16
                  | Math.min(255, Math.round(var16 / var14)) << 8
                  | Math.min(255, Math.round(var17 / var14));
            }
         }

         var4[var5] = var11;
      }

      return var4;
   }
}
