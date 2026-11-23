package net.efn.client.module.impl.render;

import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class OverlaySegments {
   private List<double[]> source;
   private boolean varied;
   private int count;
   private double[] points = new double[0];
   private int[] colors = new int[0];

   OverlaySegments() {
   }

   void update(List<double[]> list, boolean bl) {
      if (this.source != list || this.varied != bl) {
         this.count = 0;

         for (double[] var4 : list) {
            this.count = this.count + subdivisions(var4, bl);
         }

         if (this.colors.length < this.count) {
            int var19 = Math.max(this.count, this.colors.length * 2);
            this.points = Arrays.copyOf(this.points, var19 * 9);
            this.colors = Arrays.copyOf(this.colors, var19);
         }

         byte var20 = 0;

         for (double[] var5 : list) {
            int var6 = subdivisions(var5, bl);

            for (int var7 = 0; var7 < var6; var7++) {
               double var8 = (double)var7 / var6;
               double var10 = (double)(var7 + 1) / var6;
               double var12 = (var8 + var10) * 0.5;

               for (int var14 = 0; var14 < 3; var14++) {
                  double var15 = var5[var14];
                  double var17 = var5[var14 + 3];
                  this.points[var20 + var14] = var15 + var8 * (var17 - var15);
                  this.points[var20 + var14 + 3] = var15 + var10 * (var17 - var15);
                  this.points[var20 + var14 + 6] = var15 + var12 * (var17 - var15);
               }

               var20 += 9;
            }
         }

         this.source = list;
         this.varied = bl;
      }
   }

   private static int subdivisions(double[] ds, boolean bl) {
      if (!bl) {
         return 1;
      } else {
         double var2 = ds[3] - ds[0];
         double var4 = ds[4] - ds[1];
         double var6 = ds[5] - ds[2];
         double var8 = Math.sqrt(var2 * var2 + var4 * var4 + var6 * var6);
         return Math.max(1, Math.min(16, (int)Math.ceil(var8 / 0.125)));
      }
   }

   int count() {
      return this.count;
   }

   double[] points() {
      return this.points;
   }

   int[] colors() {
      return this.colors;
   }

   void invalidate() {
      this.source = null;
   }

   void clear() {
      this.source = null;
      this.count = 0;
      this.points = new double[0];
      this.colors = new int[0];
   }
}
