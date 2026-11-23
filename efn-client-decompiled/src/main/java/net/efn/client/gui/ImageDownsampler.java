package net.efn.client.gui;

import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ImageDownsampler {
   private ImageDownsampler() {
   }

   public static int[] dimensions(int var0, int var1, int var2) {
      if (var0 > 0 && var1 > 0 && var2 > 0) {
         double var3 = Math.min(1.0, (double)var2 / Math.max(var0, var1));
         return new int[]{Math.max(1, (int)Math.round(var0 * var3)), Math.max(1, (int)Math.round(var1 * var3))};
      } else {
         throw new IllegalArgumentException("Image dimensions");
      }
   }

   public static void rgba(ByteBuffer var0, int var1, int var2, ByteBuffer var3, int var4, int var5) {
      if (var4 > 0 && var5 > 0 && var4 <= var1 && var5 <= var2) {
         double var6 = (double)var1 / var4;
         double var8 = (double)var2 / var5;

         for (int var10 = 0; var10 < var5; var10++) {
            double var11 = var10 * var8;
            double var13 = (var10 + 1) * var8;

            for (int var15 = 0; var15 < var4; var15++) {
               double var16 = var15 * var6;
               double var18 = (var15 + 1) * var6;
               double var20 = 0.0;
               double var22 = 0.0;
               double var24 = 0.0;
               double var26 = 0.0;

               for (int var28 = (int)var11; var28 < Math.min(var2, (int)Math.ceil(var13)); var28++) {
                  double var29 = Math.min(var13, var28 + 1.0) - Math.max(var11, (double)var28);

                  for (int var31 = (int)var16; var31 < Math.min(var1, (int)Math.ceil(var18)); var31++) {
                     double var32 = var29 * (Math.min(var18, var31 + 1.0) - Math.max(var16, (double)var31));
                     int var34 = (var28 * var1 + var31) * 4;
                     double var35 = (var0.get(var34 + 3) & 255) * var32;
                     var20 += var35;
                     var22 += (var0.get(var34) & 255) * var35;
                     var24 += (var0.get(var34 + 1) & 255) * var35;
                     var26 += (var0.get(var34 + 2) & 255) * var35;
                  }
               }

               var3.put((byte)(var20 == 0.0 ? 0L : Math.round(var22 / var20)));
               var3.put((byte)(var20 == 0.0 ? 0L : Math.round(var24 / var20)));
               var3.put((byte)(var20 == 0.0 ? 0L : Math.round(var26 / var20)));
               var3.put((byte)Math.round(var20 / (var6 * var8)));
            }
         }

         var3.flip();
      } else {
         throw new IllegalArgumentException("Only image reduction is supported");
      }
   }
}
