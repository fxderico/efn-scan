package net.efn.client.module.impl.render.character.kit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class Atlas {
   static final int WHITE = 16;
   private static final int PAD = 3;
   private static final int MAX = 2048;
   private static final int LEVELS = 5;
   final int width;
   final int height;
   final int[][] mips;
   private final Map<Canvas, int[]> rects;

   private Atlas(int i, int j, int[][] is, Map<Canvas, int[]> map) {
      this.width = i;
      this.height = j;
      this.mips = is;
      this.rects = map;
   }

   float whiteU() {
      return 8.0F / this.width;
   }

   float whiteV() {
      return 8.0F / this.height;
   }

   float u(Canvas canvas, float f) {
      int[] var3 = this.rects.get(canvas);
      return (var3[0] + f * var3[2]) / this.width;
   }

   float v(Canvas canvas, float f) {
      int[] var3 = this.rects.get(canvas);
      return (var3[1] + f * var3[3]) / this.height;
   }

   static Atlas pack(List<Canvas> list) {
      ArrayList var1 = new ArrayList();

      for (Canvas var3 : list) {
         if (!var1.contains(var3)) {
            var1.add(var3);
         }
      }

      var1.sort((canvas, canvas2) -> Integer.compare(canvas2.height(), canvas.height()));
      long var14 = 484L;
      int var4 = 22;

      for (Canvas var6 : var1) {
         var14 += (long)(var6.width() + 6) * (var6.height() + 6);
         var4 = Math.max(var4, var6.width() + 6);
      }

      int var15 = pow2(Math.max(var4, (int)Math.ceil(Math.sqrt(var14))));

      while (true) {
         IdentityHashMap var16 = new IdentityHashMap();
         int var7 = 22;
         int var8 = 0;
         int var9 = 22;

         for (Canvas var11 : var1) {
            int var12 = var11.width() + 6;
            int var13 = var11.height() + 6;
            if (var7 + var12 > var15) {
               var7 = 0;
               var8 += var9;
               var9 = 0;
            }

            var16.put(var11, new int[]{var7 + 3, var8 + 3, var11.width(), var11.height()});
            var7 += var12;
            var9 = Math.max(var9, var13);
         }

         int var17 = pow2(Math.max(22, var8 + var9));
         if (var17 <= 2048 || var15 >= 2048) {
            if (var17 > 2048) {
               throw new IllegalStateException("decals do not fit into a 2048x2048 atlas");
            } else {
               return render(var15, var17, var1, var16);
            }
         }

         var15 *= 2;
      }
   }

   private static Atlas render(int i, int j, List<Canvas> list, Map<Canvas, int[]> map) {
      int[] var4 = new int[i * j];

      for (int var5 = 0; var5 < 19; var5++) {
         for (int var6 = 0; var6 < 19; var6++) {
            var4[var5 * i + var6] = -1;
         }
      }

      for (Canvas var14 : list) {
         int[] var7 = (int[])map.get(var14);
         int[] var8 = dilate(var14);

         for (int var9 = -3; var9 < var14.height() + 3; var9++) {
            int var10 = Math.max(0, Math.min(var14.height() - 1, var9));

            for (int var11 = -3; var11 < var14.width() + 3; var11++) {
               int var12 = Math.max(0, Math.min(var14.width() - 1, var11));
               var4[(var7[1] + var9) * i + var7[0] + var11] = var8[var10 * var14.width() + var12];
            }
         }
      }

      return new Atlas(i, j, mips(var4, i, j), map);
   }

   static int[][] mips(int[] is, int i, int j) {
      int var3 = Math.min(5, 1 + (31 - Integer.numberOfLeadingZeros(Math.min(i, j))));
      int[][] var4 = new int[var3][];
      var4[0] = is;
      int var5 = i;
      int var6 = j;

      for (int var7 = 1; var7 < var3; var7++) {
         var4[var7] = down(var4[var7 - 1], var5, var6);
         var5 = Math.max(1, var5 / 2);
         var6 = Math.max(1, var6 / 2);
      }

      return var4;
   }

   private static int[] dilate(Canvas canvas) {
      int var1 = canvas.width();
      int var2 = canvas.height();
      int[] var3 = Arrays.copyOf(canvas.pixels, canvas.pixels.length);
      boolean[] var4 = new boolean[var3.length];

      for (int var5 = 0; var5 < var3.length; var5++) {
         var4[var5] = var3[var5] >>> 24 != 0;
      }

      for (int var19 = 0; var19 < 4; var19++) {
         boolean[] var6 = (boolean[])var4.clone();

         for (int var7 = 0; var7 < var2; var7++) {
            for (int var8 = 0; var8 < var1; var8++) {
               int var9 = var7 * var1 + var8;
               if (!var4[var9]) {
                  int var10 = 0;
                  int var11 = 0;
                  int var12 = 0;
                  int var13 = 0;

                  for (int var14 = -1; var14 <= 1; var14++) {
                     for (int var15 = -1; var15 <= 1; var15++) {
                        int var17 = var8 + var15;
                        int var18 = var7 + var14;
                        int var16;
                        if (var17 >= 0 && var18 >= 0 && var17 < var1 && var18 < var2 && var4[var16 = var18 * var1 + var17]) {
                           var10 += var3[var16] >> 16 & 0xFF;
                           var11 += var3[var16] >> 8 & 0xFF;
                           var12 += var3[var16] & 0xFF;
                           var13++;
                        }
                     }
                  }

                  if (var13 != 0) {
                     var3[var9] = var10 / var13 << 16 | var11 / var13 << 8 | var12 / var13;
                     var6[var9] = true;
                  }
               }
            }
         }

         var4 = var6;
      }

      return var3;
   }

   private static int[] down(int[] is, int i, int j) {
      int var3 = Math.max(1, i / 2);
      int var4 = Math.max(1, j / 2);
      int[] var5 = new int[var3 * var4];

      for (int var6 = 0; var6 < var4; var6++) {
         for (int var7 = 0; var7 < var3; var7++) {
            float var11 = 0.0F;
            float var12 = 0.0F;
            float var13 = 0.0F;
            float var14 = 0.0F;
            float var15 = 0.0F;
            float var16 = 0.0F;
            float var17 = 0.0F;

            for (int var18 = 0; var18 < 4; var18++) {
               int var19 = Math.min(i - 1, var7 * 2 + (var18 & 1));
               int var20 = Math.min(j - 1, var6 * 2 + (var18 >> 1));
               int var21 = is[var20 * i + var19];
               float var22 = (var21 >>> 24) / 255.0F;
               var11 += var22;
               var12 += (var21 >> 16 & 0xFF) * var22;
               var13 += (var21 >> 8 & 0xFF) * var22;
               var14 += (var21 & 0xFF) * var22;
               var15 += var21 >> 16 & 0xFF;
               var16 += var21 >> 8 & 0xFF;
               var17 += var21 & 0xFF;
            }

            int var23 = Math.round(var11 / 4.0F * 255.0F);
            int var8;
            int var9;
            int var10;
            if (var11 > 1.0E-4F) {
               var10 = Math.round(var12 / var11);
               var9 = Math.round(var13 / var11);
               var8 = Math.round(var14 / var11);
            } else {
               var10 = Math.round(var15 / 4.0F);
               var9 = Math.round(var16 / 4.0F);
               var8 = Math.round(var17 / 4.0F);
            }

            var5[var6 * var3 + var7] = Colors.clamp(var23) << 24 | Colors.clamp(var10) << 16 | Colors.clamp(var9) << 8 | Colors.clamp(var8);
         }
      }

      return var5;
   }

   private static int pow2(int i) {
      byte var1 = 16;

      while (var1 < i) {
         var1 <<= 1;
      }

      return var1;
   }
}
