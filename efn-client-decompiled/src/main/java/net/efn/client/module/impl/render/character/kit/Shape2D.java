package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class Shape2D {
   static final float FAR = 100000.0F;
   float minX = -100000.0F;
   float minY = -100000.0F;
   float maxX = 100000.0F;
   float maxY = 100000.0F;

   protected Shape2D() {
   }

   public abstract float distance(float var1, float var2);

   protected final void bounds(float var1, float var2, float var3, float var4) {
      this.minX = Math.min(var1, var3);
      this.minY = Math.min(var2, var4);
      this.maxX = Math.max(var1, var3);
      this.maxY = Math.max(var2, var4);
   }

   public final float minX() {
      return this.minX;
   }

   public final float minY() {
      return this.minY;
   }

   public final float maxX() {
      return this.maxX;
   }

   public final float maxY() {
      return this.maxY;
   }

   public static Shape2D circle(float var0, float var1, float var2) {
      return new Shape2D.Circle(var0, var1, var2);
   }

   public static Shape2D ellipse(float var0, float var1, float var2, float var3) {
      return new Shape2D.Ellipse(var0, var1, var2, var3);
   }

   public static Shape2D box(float var0, float var1, float var2, float var3, float var4) {
      return new Shape2D.Box(var0, var1, var2, var3, var4);
   }

   public static Shape2D line(float var0, float var1, float var2, float var3, float var4) {
      return new Shape2D.Taper(var0, var1, var4 * 0.5F, var2, var3, var4 * 0.5F);
   }

   public static Shape2D taper(float var0, float var1, float var2, float var3, float var4, float var5) {
      return new Shape2D.Taper(var0, var1, var2 * 0.5F, var3, var4, var5 * 0.5F);
   }

   public static Shape2D curve(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      return new Shape2D.Path(new float[]{var0, var1, var2, var3, var4, var5}, var6 * 0.5F, var7 * 0.5F, true);
   }

   public static Shape2D path(float var0, float var1, float... var2) {
      return new Shape2D.Path(var2, var0 * 0.5F, var1 * 0.5F, false);
   }

   public static Shape2D polygon(float... var0) {
      return new Shape2D.Polygon(var0);
   }

   public static Shape2D arc(float var0, float var1, float var2, float var3, float var4, float var5) {
      int var6 = Math.max(3, (int)Math.ceil(Math.abs(var4 - var3) / 20.0F) + 1);
      float[] var7 = new float[(var6 + 1) * 2];

      for (int var8 = 0; var8 <= var6; var8++) {
         double var9 = Math.toRadians(var3 + (var4 - var3) * var8 / var6);
         var7[var8 * 2] = var0 + var2 * (float)Math.cos(var9);
         var7[var8 * 2 + 1] = var1 + var2 * (float)Math.sin(var9);
      }

      return new Shape2D.Path(var7, var5 * 0.5F, var5 * 0.5F, false);
   }

   public static Shape2D star(float var0, float var1, float var2, float var3, int var4) {
      float[] var5 = new float[var4 * 4];

      for (int var6 = 0; var6 < var4 * 2; var6++) {
         double var7 = 3.141592653589793 * var6 / var4 - 1.5707963267948966;
         float var9 = (var6 & 1) == 0 ? var2 : var3;
         var5[var6 * 2] = var0 + var9 * (float)Math.cos(var7);
         var5[var6 * 2 + 1] = var1 + var9 * (float)Math.sin(var7);
      }

      return new Shape2D.Polygon(var5);
   }

   public static Shape2D field(Shape2D.Field var0, float var1, float var2, float var3, float var4) {
      return new Shape2D.Custom(var0, var1, var2, var3, var4);
   }

   public static Shape2D union(Shape2D... var0) {
      return (Shape2D)(var0.length == 1 ? var0[0] : new Shape2D.Union(0.0F, var0));
   }

   public static Shape2D blend(float var0, Shape2D... var1) {
      return (Shape2D)(var1.length == 1 ? var1[0] : new Shape2D.Union(var0, var1));
   }

   public Shape2D union(Shape2D var1) {
      return new Shape2D.Union(0.0F, new Shape2D[]{this, var1});
   }

   public Shape2D blend(float var1, Shape2D var2) {
      return new Shape2D.Union(var1, new Shape2D[]{this, var2});
   }

   public Shape2D subtract(Shape2D var1) {
      return new Shape2D.Op(this, var1, 1, 0.0F);
   }

   public Shape2D subtract(float var1, Shape2D var2) {
      return new Shape2D.Op(this, var2, 1, var1);
   }

   public Shape2D intersect(Shape2D var1) {
      return new Shape2D.Op(this, var1, 2, 0.0F);
   }

   public Shape2D intersect(float var1, Shape2D var2) {
      return new Shape2D.Op(this, var2, 2, var1);
   }

   public Shape2D inflate(float var1) {
      return new Shape2D.Offset(this, var1, false);
   }

   public Shape2D outline(float var1) {
      return new Shape2D.Offset(this, var1 * 0.5F, true);
   }

   public Shape2D move(float var1, float var2) {
      return new Shape2D.Transform(this, 1.0F, 0.0F, var1, var2, 0.0F, 0.0F, 1.0F);
   }

   public Shape2D rotate(float var1, float var2, float var3) {
      double var4 = Math.toRadians(var1);
      return new Shape2D.Transform(this, (float)Math.cos(var4), (float)Math.sin(var4), 0.0F, 0.0F, var2, var3, 1.0F);
   }

   public Shape2D scale(float var1, float var2, float var3) {
      return new Shape2D.Transform(this, 1.0F, 0.0F, 0.0F, 0.0F, var2, var3, var1);
   }

   public Shape2D mirrorX(float var1) {
      return new Shape2D.Flip(this, var1);
   }

   static float clamp01(float var0) {
      return var0 < 0.0F ? 0.0F : Math.min(var0, 1.0F);
   }

   @Environment(EnvType.CLIENT)
   static final class Box extends Shape2D {
      private final float cx;
      private final float cy;
      private final float hx;
      private final float hy;
      private final float r;

      Box(float var1, float var2, float var3, float var4, float var5) {
         this.cx = var1;
         this.cy = var2;
         this.r = Math.max(0.0F, Math.min(var5, Math.min(var3, var4)));
         this.hx = var3 - this.r;
         this.hy = var4 - this.r;
         this.bounds(var1 - var3, var2 - var4, var1 + var3, var2 + var4);
      }

      @Override
      public float distance(float var1, float var2) {
         float var3 = Math.abs(var1 - this.cx) - this.hx;
         float var4 = Math.abs(var2 - this.cy) - this.hy;
         float var5 = Math.max(var3, 0.0F);
         float var6 = Math.max(var4, 0.0F);
         return (float)Math.sqrt(var5 * var5 + var6 * var6) + Math.min(Math.max(var3, var4), 0.0F) - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Circle extends Shape2D {
      private final float cx;
      private final float cy;
      private final float r;

      Circle(float var1, float var2, float var3) {
         this.cx = var1;
         this.cy = var2;
         this.r = var3;
         this.bounds(var1 - var3, var2 - var3, var1 + var3, var2 + var3);
      }

      @Override
      public float distance(float var1, float var2) {
         float var3 = var1 - this.cx;
         float var4 = var2 - this.cy;
         return (float)Math.sqrt(var3 * var3 + var4 * var4) - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Custom extends Shape2D {
      private final Shape2D.Field field;

      Custom(Shape2D.Field var1, float var2, float var3, float var4, float var5) {
         this.field = var1;
         this.bounds(var2, var3, var4, var5);
      }

      @Override
      public float distance(float var1, float var2) {
         return this.field.distance(var1, var2);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Ellipse extends Shape2D {
      private final float cx;
      private final float cy;
      private final float ix;
      private final float iy;
      private final float iix;
      private final float iiy;
      private final float rmin;

      Ellipse(float var1, float var2, float var3, float var4) {
         this.cx = var1;
         this.cy = var2;
         this.ix = 1.0F / var3;
         this.iy = 1.0F / var4;
         this.iix = this.ix * this.ix;
         this.iiy = this.iy * this.iy;
         this.rmin = Math.min(var3, var4);
         this.bounds(var1 - var3, var2 - var4, var1 + var3, var2 + var4);
      }

      @Override
      public float distance(float var1, float var2) {
         float var3 = var1 - this.cx;
         float var4 = var2 - this.cy;
         float var5 = var3 * this.ix;
         float var6 = var4 * this.iy;
         float var7 = (float)Math.sqrt(var5 * var5 + var6 * var6);
         float var8 = var3 * this.iix;
         float var9 = var4 * this.iiy;
         float var10 = (float)Math.sqrt(var8 * var8 + var9 * var9);
         return var10 < 1.0E-9F ? -this.rmin : var7 * (var7 - 1.0F) / var10;
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Field {
      float distance(float var1, float var2);
   }

   @Environment(EnvType.CLIENT)
   static final class Flip extends Shape2D {
      private final Shape2D child;
      private final float axis;

      Flip(Shape2D var1, float var2) {
         this.child = var1;
         this.axis = var2;
         this.bounds(2.0F * var2 - var1.maxX, var1.minY, 2.0F * var2 - var1.minX, var1.maxY);
      }

      @Override
      public float distance(float var1, float var2) {
         return this.child.distance(2.0F * this.axis - var1, var2);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Heart extends Shape2D {
      private final float cx;
      private final float cy;
      private final float size;

      Heart(float var1, float var2, float var3) {
         this.cx = var1;
         this.cy = var2;
         this.size = var3;
         this.bounds(var1 - var3 * 0.7F, var2 - var3 * 0.35F, var1 + var3 * 0.7F, var2 + var3 * 0.8F);
      }

      @Override
      public float distance(float var1, float var2) {
         float var4 = Math.abs(var1 - this.cx) / this.size;
         float var3;
         if (var4 + (var3 = (this.cy + this.size * 0.75F - var2) / this.size) > 1.0F) {
            float var5 = var4 - 0.25F;
            float var10 = var3 - 0.75F;
            return ((float)Math.sqrt(var5 * var5 + var10 * var10) - 0.35355338F) * this.size;
         } else {
            float var6 = var3 - 1.0F;
            float var7 = Math.max(var4 + var3, 0.0F) * 0.5F;
            float var8 = var4 - var7;
            float var9 = var3 - var7;
            return (float)Math.sqrt(Math.min(var4 * var4 + var6 * var6, var8 * var8 + var9 * var9)) * Math.signum(var4 - var3) * this.size;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Offset extends Shape2D {
      private final Shape2D child;
      private final float r;
      private final boolean ring;

      Offset(Shape2D var1, float var2, boolean var3) {
         this.child = var1;
         this.r = var2;
         this.ring = var3;
         float var4 = Math.abs(var2);
         this.bounds(var1.minX - var4, var1.minY - var4, var1.maxX + var4, var1.maxY + var4);
      }

      @Override
      public float distance(float var1, float var2) {
         float var3 = this.child.distance(var1, var2);
         return this.ring ? Math.abs(var3) - this.r : var3 - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Op extends Shape2D {
      private final Shape2D a;
      private final Shape2D b;
      private final int mode;
      private final float k;

      Op(Shape2D var1, Shape2D var2, int var3, float var4) {
         this.a = var1;
         this.b = var2;
         this.mode = var3;
         this.k = Math.max(0.0F, var4);
         if (var3 == 2) {
            this.bounds(Math.max(var1.minX, var2.minX), Math.max(var1.minY, var2.minY), Math.min(var1.maxX, var2.maxX), Math.min(var1.maxY, var2.maxY));
         } else {
            this.bounds(var1.minX, var1.minY, var1.maxX, var1.maxY);
         }
      }

      @Override
      public float distance(float var1, float var2) {
         float var3 = this.a.distance(var1, var2);
         float var4 = this.b.distance(var1, var2);
         if (this.mode == 1) {
            if (this.k <= 0.0F) {
               return Math.max(var3, -var4);
            } else {
               float var6 = clamp01(0.5F - 0.5F * (var3 + var4) / this.k);
               return var3 + (-var4 - var3) * var6 + this.k * var6 * (1.0F - var6);
            }
         } else if (this.k <= 0.0F) {
            return Math.max(var3, var4);
         } else {
            float var5 = clamp01(0.5F - 0.5F * (var4 - var3) / this.k);
            return var4 + (var3 - var4) * var5 + this.k * var5 * (1.0F - var5);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Path extends Shape2D {
      private final float[] seg;
      private final int count;

      Path(float[] var1, float var2, float var3, boolean var4) {
         if (var1.length >= 4 && var1.length % 2 == 0) {
            int var6 = var1.length / 2;
            float[] var7 = new float[var6];

            for (int var8 = 1; var8 < var6; var8++) {
               float var9 = var1[var8 * 2] - var1[var8 * 2 - 2];
               float var10 = var1[var8 * 2 + 1] - var1[var8 * 2 - 1];
               var7[var8] = var7[var8 - 1] + (float)Math.sqrt(var9 * var9 + var10 * var10);
            }

            float var17 = Math.max(1.0E-6F, var7[var6 - 1]);
            float[] var18 = new float[var6];

            for (int var5 = 0; var5 < var6; var5++) {
               var18[var5] = var2 + (var3 - var2) * var7[var5] / var17;
            }

            if (!var4 && var6 != 2) {
               this.count = var6 - 2;
               this.seg = new float[this.count * 12];

               for (int var16 = 1; var16 <= this.count; var16++) {
                  float var19 = var16 == 1 ? var1[0] : (var1[var16 * 2 - 2] + var1[var16 * 2]) * 0.5F;
                  float var11 = var16 == 1 ? var1[1] : (var1[var16 * 2 - 1] + var1[var16 * 2 + 1]) * 0.5F;
                  float var12 = var16 == 1 ? var18[0] : (var18[var16 - 1] + var18[var16]) * 0.5F;
                  float var13 = var16 == this.count ? var1[var16 * 2 + 2] : (var1[var16 * 2] + var1[var16 * 2 + 2]) * 0.5F;
                  float var14 = var16 == this.count ? var1[var16 * 2 + 3] : (var1[var16 * 2 + 1] + var1[var16 * 2 + 3]) * 0.5F;
                  float var15 = var16 == this.count ? var18[var16 + 1] : (var18[var16] + var18[var16 + 1]) * 0.5F;
                  fill(this.seg, (var16 - 1) * 12, var19, var11, var12, var1[var16 * 2], var1[var16 * 2 + 1], var18[var16], var13, var14, var15);
               }
            } else {
               this.count = 1;
               this.seg = new float[12];
               if (var6 == 2) {
                  fill(
                     this.seg,
                     0,
                     var1[0],
                     var1[1],
                     var18[0],
                     (var1[0] + var1[2]) * 0.5F,
                     (var1[1] + var1[3]) * 0.5F,
                     (var18[0] + var18[1]) * 0.5F,
                     var1[2],
                     var1[3],
                     var18[1]
                  );
               } else {
                  fill(this.seg, 0, var1[0], var1[1], var2, var1[2], var1[3], (var2 + var3) * 0.5F, var1[4], var1[5], var3);
               }
            }

            float var20 = 100000.0F;
            float var21 = 100000.0F;
            float var22 = -100000.0F;
            float var23 = -100000.0F;

            for (int var24 = 0; var24 < this.count; var24++) {
               int var25 = var24 * 12;
               var20 = Math.min(var20, this.seg[var25 + 8]);
               var21 = Math.min(var21, this.seg[var25 + 9]);
               var22 = Math.max(var22, this.seg[var25 + 10]);
               var23 = Math.max(var23, this.seg[var25 + 11]);
            }

            this.bounds(var20, var21, var22, var23);
         } else {
            throw new IllegalArgumentException("path needs at least two points given as x,y pairs");
         }
      }

      private static void fill(
         float[] var0, int var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10
      ) {
         var0[var1] = var2;
         var0[var1 + 1] = var3;
         var0[var1 + 2] = var5;
         var0[var1 + 3] = var6;
         var0[var1 + 4] = var8;
         var0[var1 + 5] = var9;
         var0[var1 + 6] = var4;
         var0[var1 + 7] = var10;
         float var11 = Math.max(var4, Math.max(var7, var10));
         var0[var1 + 8] = Math.min(var2, Math.min(var5, var8)) - var11;
         var0[var1 + 9] = Math.min(var3, Math.min(var6, var9)) - var11;
         var0[var1 + 10] = Math.max(var2, Math.max(var5, var8)) + var11;
         var0[var1 + 11] = Math.max(var3, Math.max(var6, var9)) + var11;
      }

      @Override
      public float distance(float var1, float var2) {
         float var3 = 3.4028235E38F;

         for (int var4 = 0; var4 < this.count; var4++) {
            int var6 = var4 * 12;
            float var7 = var1 < this.seg[var6 + 8] ? this.seg[var6 + 8] - var1 : (var1 > this.seg[var6 + 10] ? var1 - this.seg[var6 + 10] : 0.0F);
            float var8 = var2 < this.seg[var6 + 9] ? this.seg[var6 + 9] - var2 : (var2 > this.seg[var6 + 11] ? var2 - this.seg[var6 + 11] : 0.0F);
            float var5;
            if ((!(var3 > 0.0F) || var3 == 3.4028235E38F || !(var7 * var7 + var8 * var8 >= var3 * var3)) && (var5 = this.segment(var6, var1, var2)) < var3) {
               var3 = var5;
            }
         }

         return var3;
      }

      private float segment(int var1, float var2, float var3) {
         float var5 = this.seg[var1];
         float var6 = this.seg[var1 + 1];
         float var7 = this.seg[var1 + 2];
         float var8 = this.seg[var1 + 3];
         float var9 = this.seg[var1 + 4];
         float var10 = this.seg[var1 + 5];
         float var11 = var7 - var5;
         float var12 = var8 - var6;
         float var13 = var5 - 2.0F * var7 + var9;
         float var14 = var6 - 2.0F * var8 + var10;
         float var15 = var13 * var13 + var14 * var14;
         float var16 = var5 - var2;
         float var17 = var6 - var3;
         float var18 = var9 - var5;
         float var19 = var10 - var6;
         float var20 = var18 * var18 + var19 * var19;
         float var4;
         if (var15 < 1.0E-5F * Math.max(var20, 1.0E-6F)) {
            var4 = var20 > 1.0E-12F ? clamp01(((var2 - var5) * var18 + (var3 - var6) * var19) / var20) : 0.0F;
         } else {
            float var21 = 1.0F / var15;
            float var22 = var21 * (var11 * var13 + var12 * var14);
            float var23 = var21 * (2.0F * (var11 * var11 + var12 * var12) + var16 * var13 + var17 * var14) / 3.0F;
            float var24 = var21 * (var16 * var11 + var17 * var12);
            float var25 = var22 * (2.0F * var22 * var22 - 3.0F * var23) + var24;
            float var26 = var23 - var22 * var22;
            float var27 = var26 * var26 * var26;
            float var28 = var25 * var25 + 4.0F * var27;
            if (var28 >= 0.0F) {
               var28 = (float)Math.sqrt(var28);
               var4 = clamp01((float)(Math.cbrt((var28 - var25) * 0.5F) + Math.cbrt((-var28 - var25) * 0.5F)) - var22);
            } else {
               float var29 = (float)Math.sqrt(-var26);
               float var30 = var25 / (var26 * var29 * 2.0F);
               var30 = var30 < -1.0F ? -1.0F : Math.min(var30, 1.0F);
               float var31 = (float)Math.acos(var30) / 3.0F;
               float var32 = (float)Math.cos(var31);
               float var33 = (float)Math.sin(var31) * 1.7320508F;
               float var34 = clamp01((var32 + var32) * var29 - var22);
               float var35 = clamp01((-var33 - var32) * var29 - var22);
               float var36 = var16 + (2.0F * var11 + var13 * var34) * var34;
               float var37 = var17 + (2.0F * var12 + var14 * var34) * var34;
               float var38 = var16 + (2.0F * var11 + var13 * var35) * var35;
               float var39 = var17 + (2.0F * var12 + var14 * var35) * var35;
               var4 = var36 * var36 + var37 * var37 <= var38 * var38 + var39 * var39 ? var34 : var35;
            }
         }

         float var40 = var16 + (2.0F * var11 + var13 * var4) * var4;
         float var41 = var17 + (2.0F * var12 + var14 * var4) * var4;
         float var42 = this.seg[var1 + 6] + (this.seg[var1 + 7] - this.seg[var1 + 6]) * var4;
         return (float)Math.sqrt(var40 * var40 + var41 * var41) - var42;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Polygon extends Shape2D {
      private final float[] v;
      private final int n;

      Polygon(float[] var1) {
         if (var1.length >= 6 && var1.length % 2 == 0) {
            this.v = (float[])var1.clone();
            this.n = var1.length / 2;
            float var2 = 100000.0F;
            float var3 = 100000.0F;
            float var4 = -100000.0F;
            float var5 = -100000.0F;

            for (int var6 = 0; var6 < this.n; var6++) {
               var2 = Math.min(var2, this.v[var6 * 2]);
               var3 = Math.min(var3, this.v[var6 * 2 + 1]);
               var4 = Math.max(var4, this.v[var6 * 2]);
               var5 = Math.max(var5, this.v[var6 * 2 + 1]);
            }

            this.bounds(var2, var3, var4, var5);
         } else {
            throw new IllegalArgumentException("polygon needs at least three points given as x,y pairs");
         }
      }

      @Override
      public float distance(float var1, float var2) {
         float var3 = var1 - this.v[0];
         float var4 = var2 - this.v[1];
         float var5 = var3 * var3 + var4 * var4;
         float var6 = 1.0F;
         int var7 = 0;

         for (int var8 = this.n - 1; var7 < this.n; var8 = var7++) {
            float var10 = this.v[var8 * 2] - this.v[var7 * 2];
            float var11 = this.v[var8 * 2 + 1] - this.v[var7 * 2 + 1];
            float var12 = var1 - this.v[var7 * 2];
            float var13 = var2 - this.v[var7 * 2 + 1];
            float var14 = var10 * var10 + var11 * var11;
            float var15 = var14 > 1.0E-12F ? clamp01((var12 * var10 + var13 * var11) / var14) : 0.0F;
            float var16 = var12 - var10 * var15;
            float var17 = var13 - var11 * var15;
            var5 = Math.min(var5, var16 * var16 + var17 * var17);
            boolean var18 = var2 >= this.v[var7 * 2 + 1];
            boolean var19 = var2 < this.v[var8 * 2 + 1];
            boolean var9 = var10 * var13 > var11 * var12;
            if (var18 && var19 && var9 || !var18 && !var19 && !var9) {
               var6 = -var6;
            }
         }

         return var6 * (float)Math.sqrt(var5);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Taper extends Shape2D {
      private final float ax;
      private final float ay;
      private final float bax;
      private final float bay;
      private final float ra;
      private final float rb;
      private final float l2;
      private final float rr;
      private final float a2;
      private final float il2;
      private final boolean circle;
      private final float sx;
      private final float sy;
      private final float sr;

      Taper(float var1, float var2, float var3, float var4, float var5, float var6) {
         this.ax = var1;
         this.ay = var2;
         this.ra = var3;
         this.rb = var6;
         this.bax = var4 - var1;
         this.bay = var5 - var2;
         this.l2 = this.bax * this.bax + this.bay * this.bay;
         this.rr = var3 - var6;
         this.a2 = this.l2 - this.rr * this.rr;
         this.il2 = this.l2 > 1.0E-12F ? 1.0F / this.l2 : 0.0F;
         this.circle = this.a2 <= 1.0E-6F;
         boolean var7 = var3 >= var6;
         this.sx = var7 ? var1 : var4;
         this.sy = var7 ? var2 : var5;
         this.sr = Math.max(var3, var6);
         this.bounds(
            Math.min(var1 - var3, var4 - var6), Math.min(var2 - var3, var5 - var6), Math.max(var1 + var3, var4 + var6), Math.max(var2 + var3, var5 + var6)
         );
      }

      @Override
      public float distance(float var1, float var2) {
         if (this.circle) {
            float var13 = var1 - this.sx;
            float var14 = var2 - this.sy;
            return (float)Math.sqrt(var13 * var13 + var14 * var14) - this.sr;
         } else {
            float var3 = var1 - this.ax;
            float var4 = var2 - this.ay;
            float var5 = var3 * this.bax + var4 * this.bay;
            float var6 = var5 - this.l2;
            float var7 = var3 * this.l2 - this.bax * var5;
            float var8 = var4 * this.l2 - this.bay * var5;
            float var9 = var7 * var7 + var8 * var8;
            float var10 = var5 * var5 * this.l2;
            float var11 = var6 * var6 * this.l2;
            float var12 = Math.signum(this.rr) * this.rr * this.rr * var9;
            if (Math.signum(var6) * this.a2 * var11 > var12) {
               return (float)Math.sqrt(var9 + var11) * this.il2 - this.rb;
            } else {
               return Math.signum(var5) * this.a2 * var10 < var12
                  ? (float)Math.sqrt(var9 + var10) * this.il2 - this.ra
                  : ((float)Math.sqrt(var9 * this.a2 * this.il2) + var5 * this.rr) * this.il2 - this.ra;
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Transform extends Shape2D {
      private final Shape2D child;
      private final float c;
      private final float s;
      private final float dx;
      private final float dy;
      private final float cx;
      private final float cy;
      private final float k;

      Transform(Shape2D var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
         this.child = var1;
         this.c = var2;
         this.s = var3;
         this.dx = var4;
         this.dy = var5;
         this.cx = var6;
         this.cy = var7;
         this.k = var8;
         float var9 = 100000.0F;
         float var10 = 100000.0F;
         float var11 = -100000.0F;
         float var12 = -100000.0F;

         for (int var13 = 0; var13 < 4; var13++) {
            float var14 = ((var13 & 1) == 0 ? var1.minX : var1.maxX) - var6;
            float var15 = ((var13 & 2) == 0 ? var1.minY : var1.maxY) - var7;
            float var16 = (var2 * var14 - var3 * var15) * var8 + var6 + var4;
            float var17 = (var3 * var14 + var2 * var15) * var8 + var7 + var5;
            var9 = Math.min(var9, var16);
            var10 = Math.min(var10, var17);
            var11 = Math.max(var11, var16);
            var12 = Math.max(var12, var17);
         }

         this.bounds(var9, var10, var11, var12);
      }

      @Override
      public float distance(float var1, float var2) {
         float var3 = (var1 - this.dx - this.cx) / this.k;
         float var4 = (var2 - this.dy - this.cy) / this.k;
         return this.child.distance(this.c * var3 + this.s * var4 + this.cx, -this.s * var3 + this.c * var4 + this.cy) * this.k;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Union extends Shape2D {
      private final Shape2D[] kids;
      private final float k;

      Union(float var1, Shape2D[] var2) {
         this.kids = (Shape2D[])var2.clone();
         this.k = Math.max(0.0F, var1);
         float var3 = 100000.0F;
         float var4 = 100000.0F;
         float var5 = -100000.0F;
         float var6 = -100000.0F;

         for (Shape2D var10 : this.kids) {
            var3 = Math.min(var3, var10.minX);
            var4 = Math.min(var4, var10.minY);
            var5 = Math.max(var5, var10.maxX);
            var6 = Math.max(var6, var10.maxY);
         }

         this.bounds(var3 - this.k, var4 - this.k, var5 + this.k, var6 + this.k);
      }

      @Override
      public float distance(float var1, float var2) {
         float var3 = this.kids[0].distance(var1, var2);

         for (int var4 = 1; var4 < this.kids.length; var4++) {
            float var5 = this.kids[var4].distance(var1, var2);
            if (this.k > 0.0F) {
               float var6 = clamp01(0.5F + 0.5F * (var5 - var3) / this.k);
               var3 = var5 + (var3 - var5) * var6 - this.k * var6 * (1.0F - var6);
            } else if (var5 < var3) {
               var3 = var5;
            }
         }

         return var3;
      }
   }
}
