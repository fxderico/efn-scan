package net.efn.client.gui;

import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ImageDownsampler {
   private ImageDownsampler() {
   }

   public static int[] dimensions(int i, int j, int k) {
      if (i > 0 && j > 0 && k > 0) {
         double var3 = Math.min(1.0, (double)k / Math.max(i, j));
         return new int[]{Math.max(1, (int)Math.round(i * var3)), Math.max(1, (int)Math.round(j * var3))};
      } else {
         throw new IllegalArgumentException("Image dimensions");
      }
   }

   public static void rgba(ByteBuffer byteBuffer, int i, int j, ByteBuffer byteBuffer2, int k, int l) {
      if (k > 0 && l > 0 && k <= i && l <= j) {
         double var6 = (double)i / k;
         double var8 = (double)j / l;

         for (int var10 = 0; var10 < l; var10++) {
            double var11 = var10 * var8;
            double var13 = (var10 + 1) * var8;

            for (int var15 = 0; var15 < k; var15++) {
               double var16 = var15 * var6;
               double var18 = (var15 + 1) * var6;
               double var20 = 0.0;
               double var22 = 0.0;
               double var24 = 0.0;
               double var26 = 0.0;

               for (int var28 = (int)var11; var28 < Math.min(j, (int)Math.ceil(var13)); var28++) {
                  double var29 = Math.min(var13, var28 + 1.0) - Math.max(var11, (double)var28);

                  for (int var31 = (int)var16; var31 < Math.min(i, (int)Math.ceil(var18)); var31++) {
                     double var32 = var29 * (Math.min(var18, var31 + 1.0) - Math.max(var16, (double)var31));
                     int var34 = (var28 * i + var31) * 4;
                     double var35 = (byteBuffer.get(var34 + 3) & 255) * var32;
                     var20 += var35;
                     var22 += (byteBuffer.get(var34) & 255) * var35;
                     var24 += (byteBuffer.get(var34 + 1) & 255) * var35;
                     var26 += (byteBuffer.get(var34 + 2) & 255) * var35;
                  }
               }

               byteBuffer2.put((byte)(var20 == 0.0 ? 0L : Math.round(var22 / var20)));
               byteBuffer2.put((byte)(var20 == 0.0 ? 0L : Math.round(var24 / var20)));
               byteBuffer2.put((byte)(var20 == 0.0 ? 0L : Math.round(var26 / var20)));
               byteBuffer2.put((byte)Math.round(var20 / (var6 * var8)));
            }
         }

         byteBuffer2.flip();
      } else {
         throw new IllegalArgumentException("Only image reduction is supported");
      }
   }
}
