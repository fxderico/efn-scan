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

   private Strand(float[] fs, int i) {
      this.seg = fs;
      this.count = i;
      float var3 = 100000.0F;
      float var4 = 100000.0F;
      float var5 = 100000.0F;
      float var6 = -100000.0F;
      float var7 = -100000.0F;
      float var8 = -100000.0F;

      for (int var9 = 0; var9 < i; var9++) {
         int var10 = var9 * 20;
         var3 = Math.min(var3, fs[var10 + 14]);
         var4 = Math.min(var4, fs[var10 + 15]);
         var5 = Math.min(var5, fs[var10 + 16]);
         var6 = Math.max(var6, fs[var10 + 17]);
         var7 = Math.max(var7, fs[var10 + 18]);
         var8 = Math.max(var8, fs[var10 + 19]);
      }

      this.bounds(var3, var4, var5, var6, var7, var8);
   }

   static Strand tapered(float f, float g, float[] fs) {
      if (fs.length >= 6 && fs.length % 3 == 0) {
         int var3 = fs.length / 3;
         float[] var4 = new float[var3];

         for (int var5 = 1; var5 < var3; var5++) {
            float var6 = fs[var5 * 3] - fs[var5 * 3 - 3];
            float var7 = fs[var5 * 3 + 1] - fs[var5 * 3 - 2];
            float var8 = fs[var5 * 3 + 2] - fs[var5 * 3 - 1];
            var4[var5] = var4[var5 - 1] + (float)Math.sqrt(var6 * var6 + var7 * var7 + var8 * var8);
         }

         float var9 = Math.max(1.0E-6F, var4[var3 - 1]);
         float[] var10 = new float[var3 * 4];

         for (int var11 = 0; var11 < var3; var11++) {
            var10[var11 * 4] = fs[var11 * 3];
            var10[var11 * 4 + 1] = fs[var11 * 3 + 1];
            var10[var11 * 4 + 2] = fs[var11 * 3 + 2];
            var10[var11 * 4 + 3] = f + (g - f) * var4[var11] / var9;
         }

         return of(var10, false);
      } else {
         throw new IllegalArgumentException("strand needs at least two points given as x,y,z triples");
      }
   }

   static Strand of(float[] fs, boolean bl) {
      if (fs.length >= 8 && fs.length % 4 == 0) {
         int var2 = fs.length / 4;
         if (bl) {
            float[] var18 = new float[20];
            fill(var18, 0, fs[0], fs[1], fs[2], fs[3], fs[4], fs[5], fs[6], fs[7], fs[8], fs[9], fs[10], fs[11]);
            return new Strand(var18, 1);
         } else if (var2 == 2) {
            float[] var17 = new float[20];
            fill(
               var17,
               0,
               fs[0],
               fs[1],
               fs[2],
               fs[3],
               (fs[0] + fs[4]) * 0.5F,
               (fs[1] + fs[5]) * 0.5F,
               (fs[2] + fs[6]) * 0.5F,
               (fs[3] + fs[7]) * 0.5F,
               fs[4],
               fs[5],
               fs[6],
               fs[7]
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
                  var13 = fs[0];
                  var12 = fs[1];
                  var11 = fs[2];
                  var10 = fs[3];
               } else {
                  var13 = (fs[var14] + fs[var15]) * 0.5F;
                  var12 = (fs[var14 + 1] + fs[var15 + 1]) * 0.5F;
                  var11 = (fs[var14 + 2] + fs[var15 + 2]) * 0.5F;
                  var10 = (fs[var14 + 3] + fs[var15 + 3]) * 0.5F;
               }

               float var6;
               float var7;
               float var8;
               float var9;
               if (var5 == var3) {
                  var9 = fs[var16];
                  var8 = fs[var16 + 1];
                  var7 = fs[var16 + 2];
                  var6 = fs[var16 + 3];
               } else {
                  var9 = (fs[var15] + fs[var16]) * 0.5F;
                  var8 = (fs[var15 + 1] + fs[var16 + 1]) * 0.5F;
                  var7 = (fs[var15 + 2] + fs[var16 + 2]) * 0.5F;
                  var6 = (fs[var15 + 3] + fs[var16 + 3]) * 0.5F;
               }

               fill(var4, (var5 - 1) * 20, var13, var12, var11, var10, fs[var15], fs[var15 + 1], fs[var15 + 2], fs[var15 + 3], var9, var8, var7, var6);
            }

            return new Strand(var4, var3);
         }
      } else {
         throw new IllegalArgumentException("tube needs at least two points given as x,y,z,radius quadruples");
      }
   }

   private static void fill(float[] fs, int i, float f, float g, float h, float j, float k, float l, float m, float n, float o, float p, float q, float r) {
      fs[i] = f;
      fs[i + 1] = g;
      fs[i + 2] = h;
      fs[i + 3] = k - f;
      fs[i + 4] = l - g;
      fs[i + 5] = m - h;
      fs[i + 6] = f - 2.0F * k + o;
      fs[i + 7] = g - 2.0F * l + p;
      fs[i + 8] = h - 2.0F * m + q;
      fs[i + 9] = j;
      fs[i + 10] = n;
      fs[i + 11] = r;
      float var14 = fs[i + 6] * fs[i + 6] + fs[i + 7] * fs[i + 7] + fs[i + 8] * fs[i + 8];
      float var15 = o - f;
      float var16 = p - g;
      float var17 = q - h;
      float var18 = var15 * var15 + var16 * var16 + var17 * var17;
      boolean var19 = var14 < 1.0E-5F * Math.max(var18, 1.0E-6F);
      float var20 = fs[i + 12] = var19 ? -1.0F : 1.0F / var14;
      fs[i + 13] = var19 ? (var18 > 1.0E-12F ? 1.0F / var18 : 0.0F) : 0.0F;
      float var21 = Math.max(j, Math.max(n, r));
      fs[i + 14] = Math.min(f, Math.min(k, o)) - var21;
      fs[i + 15] = Math.min(g, Math.min(l, p)) - var21;
      fs[i + 16] = Math.min(h, Math.min(m, q)) - var21;
      fs[i + 17] = Math.max(f, Math.max(k, o)) + var21;
      fs[i + 18] = Math.max(g, Math.max(l, p)) + var21;
      fs[i + 19] = Math.max(h, Math.max(m, q)) + var21;
   }

   Strand thickened(float f) {
      if (f <= this.minHalf) {
         return this;
      } else {
         Strand var2 = new Strand(this.seg, this.count);
         var2.ratio = this.ratio;
         var2.flatMode = this.flatMode;
         var2.fx = this.fx;
         var2.fy = this.fy;
         var2.fz = this.fz;
         var2.minHalf = f;
         var2.paint = this.paint;
         return var2;
      }
   }

   public Strand flatten(float f, float g, float h, float i) {
      float var5 = (float)Math.sqrt(g * g + h * h + i * i);
      if (!(var5 < 1.0E-6F) && !(f <= 1.0F)) {
         this.ratio = f;
         this.flatMode = 1;
         this.fx = g / var5;
         this.fy = h / var5;
         this.fz = i / var5;
         return this;
      } else {
         this.flatMode = 0;
         return this;
      }
   }

   public Strand flattenFrom(float f, float g, float h, float i) {
      if (f <= 1.0F) {
         this.flatMode = 0;
         return this;
      } else {
         this.ratio = f;
         this.flatMode = 2;
         this.fx = g;
         this.fy = h;
         this.fz = i;
         return this;
      }
   }

   @Override
   public float distance(float f, float g, float h) {
      return this.distance(f, g, h, 3.4028235E38F);
   }

   @Override
   float distance(float f, float g, float h, float i) {
      float[] var5 = this.seg;
      int var6 = 0;
      float var7 = 3.4028235E38F;

      for (int var8 = 0; var8 < this.count; var8++) {
         float var9 = gap(var5, var8 * 20, f, g, h);
         if (var9 < var7) {
            var7 = var9;
            var6 = var8;
         }
      }

      if (var7 >= i) {
         return var7;
      } else {
         float var11 = this.segment(var5, var6 * 20, f, g, h);

         for (int var12 = 0; var12 < this.count; var12++) {
            float var10;
            if (var12 != var6 && !(gap(var5, var12 * 20, f, g, h) >= Math.min(var11, i)) && (var10 = this.segment(var5, var12 * 20, f, g, h)) < var11) {
               var11 = var10;
            }
         }

         return var11;
      }
   }

   private static float gap(float[] fs, int i, float f, float g, float h) {
      float var5 = f < fs[i + 14] ? fs[i + 14] - f : (f > fs[i + 17] ? f - fs[i + 17] : 0.0F);
      float var6 = g < fs[i + 15] ? fs[i + 15] - g : (g > fs[i + 18] ? g - fs[i + 18] : 0.0F);
      float var7 = h < fs[i + 16] ? fs[i + 16] - h : (h > fs[i + 19] ? h - fs[i + 19] : 0.0F);
      return (float)Math.sqrt(var5 * var5 + var6 * var6 + var7 * var7);
   }

   private float segment(float[] fs, int i, float f, float g, float h) {
      float var10 = fs[i];
      float var11 = fs[i + 1];
      float var12 = fs[i + 2];
      float var13 = fs[i + 3];
      float var14 = fs[i + 4];
      float var15 = fs[i + 5];
      float var16 = fs[i + 6];
      float var17 = fs[i + 7];
      float var18 = fs[i + 8];
      float var19 = fs[i + 12];
      float var9;
      if (var19 < 0.0F) {
         float var20 = 2.0F * var13 + var16;
         float var21 = 2.0F * var14 + var17;
         float var22 = 2.0F * var15 + var18;
         var9 = Shapes.clamp01(((f - var10) * var20 + (g - var11) * var21 + (h - var12) * var22) * fs[i + 13]);
      } else {
         float var46 = var19 * (var13 * var16 + var14 * var17 + var15 * var18);
         float var48 = var10 - f;
         float var50 = var11 - g;
         float var23 = var12 - h;
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
      float var52 = f - var47;
      float var53 = g - var49;
      float var54 = h - var51;
      float var55 = 1.0F - var9;
      float var56 = var55 * var55 * fs[i + 9] + 2.0F * var55 * var9 * fs[i + 10] + var9 * var9 * fs[i + 11];
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
