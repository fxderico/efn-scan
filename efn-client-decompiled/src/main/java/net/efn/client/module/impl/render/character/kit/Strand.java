package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Strand extends Shape {
   private static final int STRIDE = 20;
   private final int count;
   private final float[] seg;
   private float ratio = 1.0F;
   private int flatMode;
   private float fx;
   private float fy;
   private float fz;
   private float minHalf;

   private Strand(float[] var1, int var2) {
      this.seg = var1;
      this.count = var2;
      float var3 = 100000.0F;
      float var4 = 100000.0F;
      float var5 = 100000.0F;
      float var6 = -100000.0F;
      float var7 = -100000.0F;
      float var8 = -100000.0F;

      for (int var9 = 0; var9 < var2; var9++) {
         int var10 = var9 * 20;
         var3 = Math.min(var3, var1[var10 + 14]);
         var4 = Math.min(var4, var1[var10 + 15]);
         var5 = Math.min(var5, var1[var10 + 16]);
         var6 = Math.max(var6, var1[var10 + 17]);
         var7 = Math.max(var7, var1[var10 + 18]);
         var8 = Math.max(var8, var1[var10 + 19]);
      }

      this.bounds(var3, var4, var5, var6, var7, var8);
   }

   static Strand tapered(float var0, float var1, float[] var2) {
      if (var2.length >= 6 && var2.length % 3 == 0) {
         int var3 = var2.length / 3;
         float[] var4 = new float[var3];

         for (int var5 = 1; var5 < var3; var5++) {
            float var6 = var2[var5 * 3] - var2[var5 * 3 - 3];
            float var7 = var2[var5 * 3 + 1] - var2[var5 * 3 - 2];
            float var8 = var2[var5 * 3 + 2] - var2[var5 * 3 - 1];
            var4[var5] = var4[var5 - 1] + (float)Math.sqrt(var6 * var6 + var7 * var7 + var8 * var8);
         }

         float var9 = Math.max(1.0E-6F, var4[var3 - 1]);
         float[] var10 = new float[var3 * 4];

         for (int var11 = 0; var11 < var3; var11++) {
            var10[var11 * 4] = var2[var11 * 3];
            var10[var11 * 4 + 1] = var2[var11 * 3 + 1];
            var10[var11 * 4 + 2] = var2[var11 * 3 + 2];
            var10[var11 * 4 + 3] = var0 + (var1 - var0) * var4[var11] / var9;
         }

         return of(var10, false);
      } else {
         throw new IllegalArgumentException("strand needs at least two points given as x,y,z triples");
      }
   }

   static Strand of(float[] var0, boolean var1) {
      if (var0.length >= 8 && var0.length % 4 == 0) {
         int var2 = var0.length / 4;
         if (var1) {
            float[] var18 = new float[20];
            fill(var18, 0, var0[0], var0[1], var0[2], var0[3], var0[4], var0[5], var0[6], var0[7], var0[8], var0[9], var0[10], var0[11]);
            return new Strand(var18, 1);
         } else if (var2 == 2) {
            float[] var17 = new float[20];
            fill(
               var17,
               0,
               var0[0],
               var0[1],
               var0[2],
               var0[3],
               (var0[0] + var0[4]) * 0.5F,
               (var0[1] + var0[5]) * 0.5F,
               (var0[2] + var0[6]) * 0.5F,
               (var0[3] + var0[7]) * 0.5F,
               var0[4],
               var0[5],
               var0[6],
               var0[7]
            );
            return new Strand(var17, 1);
         } else {
            int var3 = var2 - 2;
            float[] var4 = new float[var3 * 20];

            for (int var5 = 1; var5 <= var3; var5++) {
               int var14 = (var5 - 1) * 4;
               int var15 = var5 * 4;
               int var16 = (var5 + 1) * 4;
               float var10;
               float var11;
               float var12;
               float var13;
               if (var5 == 1) {
                  var13 = var0[0];
                  var12 = var0[1];
                  var11 = var0[2];
                  var10 = var0[3];
               } else {
                  var13 = (var0[var14] + var0[var15]) * 0.5F;
                  var12 = (var0[var14 + 1] + var0[var15 + 1]) * 0.5F;
                  var11 = (var0[var14 + 2] + var0[var15 + 2]) * 0.5F;
                  var10 = (var0[var14 + 3] + var0[var15 + 3]) * 0.5F;
               }

               float var6;
               float var7;
               float var8;
               float var9;
               if (var5 == var3) {
                  var9 = var0[var16];
                  var8 = var0[var16 + 1];
                  var7 = var0[var16 + 2];
                  var6 = var0[var16 + 3];
               } else {
                  var9 = (var0[var15] + var0[var16]) * 0.5F;
                  var8 = (var0[var15 + 1] + var0[var16 + 1]) * 0.5F;
                  var7 = (var0[var15 + 2] + var0[var16 + 2]) * 0.5F;
                  var6 = (var0[var15 + 3] + var0[var16 + 3]) * 0.5F;
               }

               fill(var4, (var5 - 1) * 20, var13, var12, var11, var10, var0[var15], var0[var15 + 1], var0[var15 + 2], var0[var15 + 3], var9, var8, var7, var6);
            }

            return new Strand(var4, var3);
         }
      } else {
         throw new IllegalArgumentException("tube needs at least two points given as x,y,z,radius quadruples");
      }
   }

   private static void fill(
      float[] var0,
      int var1,
      float var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13
   ) {
      var0[var1] = var2;
      var0[var1 + 1] = var3;
      var0[var1 + 2] = var4;
      var0[var1 + 3] = var6 - var2;
      var0[var1 + 4] = var7 - var3;
      var0[var1 + 5] = var8 - var4;
      var0[var1 + 6] = var2 - 2.0F * var6 + var10;
      var0[var1 + 7] = var3 - 2.0F * var7 + var11;
      var0[var1 + 8] = var4 - 2.0F * var8 + var12;
      var0[var1 + 9] = var5;
      var0[var1 + 10] = var9;
      var0[var1 + 11] = var13;
      float var14 = var0[var1 + 6] * var0[var1 + 6] + var0[var1 + 7] * var0[var1 + 7] + var0[var1 + 8] * var0[var1 + 8];
      float var15 = var10 - var2;
      float var16 = var11 - var3;
      float var17 = var12 - var4;
      float var18 = var15 * var15 + var16 * var16 + var17 * var17;
      boolean var19 = var14 < 1.0E-5F * Math.max(var18, 1.0E-6F);
      float var20 = var0[var1 + 12] = var19 ? -1.0F : 1.0F / var14;
      var0[var1 + 13] = var19 ? (var18 > 1.0E-12F ? 1.0F / var18 : 0.0F) : 0.0F;
      float var21 = Math.max(var5, Math.max(var9, var13));
      var0[var1 + 14] = Math.min(var2, Math.min(var6, var10)) - var21;
      var0[var1 + 15] = Math.min(var3, Math.min(var7, var11)) - var21;
      var0[var1 + 16] = Math.min(var4, Math.min(var8, var12)) - var21;
      var0[var1 + 17] = Math.max(var2, Math.max(var6, var10)) + var21;
      var0[var1 + 18] = Math.max(var3, Math.max(var7, var11)) + var21;
      var0[var1 + 19] = Math.max(var4, Math.max(var8, var12)) + var21;
   }

   Strand thickened(float var1) {
      if (var1 <= this.minHalf) {
         return this;
      } else {
         Strand var2 = new Strand(this.seg, this.count);
         var2.ratio = this.ratio;
         var2.flatMode = this.flatMode;
         var2.fx = this.fx;
         var2.fy = this.fy;
         var2.fz = this.fz;
         var2.minHalf = var1;
         var2.paint = this.paint;
         return var2;
      }
   }

   public Strand flatten(float var1, float var2, float var3, float var4) {
      float var5 = (float)Math.sqrt(var2 * var2 + var3 * var3 + var4 * var4);
      if (!(var5 < 1.0E-6F) && !(var1 <= 1.0F)) {
         this.ratio = var1;
         this.flatMode = 1;
         this.fx = var2 / var5;
         this.fy = var3 / var5;
         this.fz = var4 / var5;
         return this;
      } else {
         this.flatMode = 0;
         return this;
      }
   }

   public Strand flattenFrom(float var1, float var2, float var3, float var4) {
      if (var1 <= 1.0F) {
         this.flatMode = 0;
         return this;
      } else {
         this.ratio = var1;
         this.flatMode = 2;
         this.fx = var2;
         this.fy = var3;
         this.fz = var4;
         return this;
      }
   }

   @Override
   public float distance(float var1, float var2, float var3) {
      return this.distance(var1, var2, var3, 3.4028235E38F);
   }

   @Override
   float distance(float var1, float var2, float var3, float var4) {
      float[] var5 = this.seg;
      int var6 = 0;
      float var7 = 3.4028235E38F;

      for (int var8 = 0; var8 < this.count; var8++) {
         float var9 = gap(var5, var8 * 20, var1, var2, var3);
         if (var9 < var7) {
            var7 = var9;
            var6 = var8;
         }
      }

      if (var7 >= var4) {
         return var7;
      } else {
         float var11 = this.segment(var5, var6 * 20, var1, var2, var3);

         for (int var12 = 0; var12 < this.count; var12++) {
            float var10;
            if (var12 != var6
               && !(gap(var5, var12 * 20, var1, var2, var3) >= Math.min(var11, var4))
               && (var10 = this.segment(var5, var12 * 20, var1, var2, var3)) < var11) {
               var11 = var10;
            }
         }

         return var11;
      }
   }

   private static float gap(float[] var0, int var1, float var2, float var3, float var4) {
      float var5 = var2 < var0[var1 + 14] ? var0[var1 + 14] - var2 : (var2 > var0[var1 + 17] ? var2 - var0[var1 + 17] : 0.0F);
      float var6 = var3 < var0[var1 + 15] ? var0[var1 + 15] - var3 : (var3 > var0[var1 + 18] ? var3 - var0[var1 + 18] : 0.0F);
      float var7 = var4 < var0[var1 + 16] ? var0[var1 + 16] - var4 : (var4 > var0[var1 + 19] ? var4 - var0[var1 + 19] : 0.0F);
      return (float)Math.sqrt(var5 * var5 + var6 * var6 + var7 * var7);
   }

   private float segment(float[] var1, int var2, float var3, float var4, float var5) {
      float var10 = var1[var2];
      float var11 = var1[var2 + 1];
      float var12 = var1[var2 + 2];
      float var13 = var1[var2 + 3];
      float var14 = var1[var2 + 4];
      float var15 = var1[var2 + 5];
      float var16 = var1[var2 + 6];
      float var17 = var1[var2 + 7];
      float var18 = var1[var2 + 8];
      float var19 = var1[var2 + 12];
      float var9;
      if (var19 < 0.0F) {
         float var20 = 2.0F * var13 + var16;
         float var21 = 2.0F * var14 + var17;
         float var22 = 2.0F * var15 + var18;
         var9 = Shapes.clamp01(((var3 - var10) * var20 + (var4 - var11) * var21 + (var5 - var12) * var22) * var1[var2 + 13]);
      } else {
         float var46 = var19 * (var13 * var16 + var14 * var17 + var15 * var18);
         float var48 = var10 - var3;
         float var50 = var11 - var4;
         float var23 = var12 - var5;
         float var24 = var19 * (2.0F * (var13 * var13 + var14 * var14 + var15 * var15) + var48 * var16 + var50 * var17 + var23 * var18) / 3.0F;
         float var25 = var19 * (var48 * var13 + var50 * var14 + var23 * var15);
         float var26 = var46 * (2.0F * var46 * var46 - 3.0F * var24) + var25;
         float var27 = var24 - var46 * var46;
         float var28 = var27 * var27 * var27;
         float var29 = var26 * var26 + 4.0F * var28;
         if (var29 >= 0.0F) {
            var29 = (float)Math.sqrt(var29);
            var9 = Shapes.clamp01((float)(Math.cbrt((var29 - var26) * 0.5F) + Math.cbrt((-var29 - var26) * 0.5F)) - var46);
         } else {
            float var30 = (float)Math.sqrt(-var27);
            float var31 = var26 / (var27 * var30 * 2.0F);
            var31 = var31 < -1.0F ? -1.0F : Math.min(var31, 1.0F);
            float var32 = (float)Math.acos(var31) / 3.0F;
            float var33 = (float)Math.cos(var32);
            float var34 = (float)Math.sin(var32) * 1.7320508F;
            float var35 = Shapes.clamp01((var33 + var33) * var30 - var46);
            float var36 = Shapes.clamp01((-var34 - var33) * var30 - var46);
            float var37 = var48 + (2.0F * var13 + var16 * var35) * var35;
            float var38 = var50 + (2.0F * var14 + var17 * var35) * var35;
            float var39 = var23 + (2.0F * var15 + var18 * var35) * var35;
            float var40 = var48 + (2.0F * var13 + var16 * var36) * var36;
            float var41 = var50 + (2.0F * var14 + var17 * var36) * var36;
            float var42 = var23 + (2.0F * var15 + var18 * var36) * var36;
            var9 = var37 * var37 + var38 * var38 + var39 * var39 <= var40 * var40 + var41 * var41 + var42 * var42 ? var35 : var36;
         }
      }

      float var47 = var10 + (2.0F * var13 + var16 * var9) * var9;
      float var49 = var11 + (2.0F * var14 + var17 * var9) * var9;
      float var51 = var12 + (2.0F * var15 + var18 * var9) * var9;
      float var52 = var3 - var47;
      float var53 = var4 - var49;
      float var54 = var5 - var51;
      float var55 = 1.0F - var9;
      float var56 = var55 * var55 * var1[var2 + 9] + 2.0F * var55 * var9 * var1[var2 + 10] + var9 * var9 * var1[var2 + 11];
      float var58 = var52 * var52 + var53 * var53 + var54 * var54;
      if (this.flatMode == 0) {
         return (float)Math.sqrt(var58) - Math.max(var56, this.minHalf);
      } else {
         var56 = Math.max(var56, this.minHalf * this.ratio);
         float var60 = 2.0F * (var13 + var16 * var9);
         float var61 = 2.0F * (var14 + var17 * var9);
         float var63 = 2.0F * (var15 + var18 * var9);
         float var64 = (float)Math.sqrt(var60 * var60 + var61 * var61 + var63 * var63);
         if (var64 > 1.0E-6F) {
            var60 /= var64;
            var61 /= var64;
            var63 /= var64;
         }

         float var6;
         float var7;
         float var8;
         if (this.flatMode == 1) {
            var8 = this.fx;
            var7 = this.fy;
            var6 = this.fz;
         } else {
            var8 = var47 - this.fx;
            var7 = var49 - this.fy;
            var6 = var51 - this.fz;
         }

         float var65 = var8 * var60 + var7 * var61 + var6 * var63;
         float var43;
         float var44;
         float var45;
         float var66 = (float)Math.sqrt(
            (var45 = var8 - var65 * var60) * var45 + (var44 = var7 - var65 * var61) * var44 + (var43 = var6 - var65 * var63) * var43
         );
         if (var66 < 1.0E-6F) {
            return (float)Math.sqrt(var58) - var56;
         } else {
            float var67 = (var52 * var45 + var53 * var44 + var54 * var43) / var66;
            float var68 = Math.max(0.0F, var58 - var67 * var67);
            return ((float)Math.sqrt(var68 + this.ratio * this.ratio * var67 * var67) - var56) / this.ratio;
         }
      }
   }
}
