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

   public abstract float distance(float f, float g);

   protected final void bounds(float f, float g, float h, float i) {
      this.minX = Math.min(f, h);
      this.minY = Math.min(g, i);
      this.maxX = Math.max(f, h);
      this.maxY = Math.max(g, i);
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

   public static Shape2D circle(float f, float g, float h) {
      return new Shape2D.Circle(f, g, h);
   }

   public static Shape2D ellipse(float f, float g, float h, float i) {
      return new Shape2D.Ellipse(f, g, h, i);
   }

   public static Shape2D box(float f, float g, float h, float i, float j) {
      return new Shape2D.Box(f, g, h, i, j);
   }

   public static Shape2D line(float f, float g, float h, float i, float j) {
      return new Shape2D.Taper(f, g, j * 0.5F, h, i, j * 0.5F);
   }

   public static Shape2D taper(float f, float g, float h, float i, float j, float k) {
      return new Shape2D.Taper(f, g, h * 0.5F, i, j, k * 0.5F);
   }

   public static Shape2D curve(float f, float g, float h, float i, float j, float k, float l, float m) {
      return new Shape2D.Path(new float[]{f, g, h, i, j, k}, l * 0.5F, m * 0.5F, true);
   }

   public static Shape2D path(float f, float g, float... fs) {
      return new Shape2D.Path(fs, f * 0.5F, g * 0.5F, false);
   }

   public static Shape2D polygon(float... fs) {
      return new Shape2D.Polygon(fs);
   }

   public static Shape2D arc(float f, float g, float h, float i, float j, float k) {
      int var6 = Math.max(3, (int)Math.ceil(Math.abs(j - i) / 20.0F) + 1);
      float[] var7 = new float[(var6 + 1) * 2];

      for (int var8 = 0; var8 <= var6; var8++) {
         double var9 = Math.toRadians(i + (j - i) * var8 / var6);
         var7[var8 * 2] = f + h * (float)Math.cos(var9);
         var7[var8 * 2 + 1] = g + h * (float)Math.sin(var9);
      }

      return new Shape2D.Path(var7, k * 0.5F, k * 0.5F, false);
   }

   public static Shape2D star(float f, float g, float h, float i, int j) {
      float[] var5 = new float[j * 4];

      for (int var6 = 0; var6 < j * 2; var6++) {
         double var7 = 3.141592653589793 * var6 / j - 1.5707963267948966;
         float var9 = (var6 & 1) == 0 ? h : i;
         var5[var6 * 2] = f + var9 * (float)Math.cos(var7);
         var5[var6 * 2 + 1] = g + var9 * (float)Math.sin(var7);
      }

      return new Shape2D.Polygon(var5);
   }

   public static Shape2D field(Shape2D.Field field, float f, float g, float h, float i) {
      return new Shape2D.Custom(field, f, g, h, i);
   }

   public static Shape2D union(Shape2D... shape2Ds) {
      return (Shape2D)(shape2Ds.length == 1 ? shape2Ds[0] : new Shape2D.Union(0.0F, shape2Ds));
   }

   public static Shape2D blend(float f, Shape2D... shape2Ds) {
      return (Shape2D)(shape2Ds.length == 1 ? shape2Ds[0] : new Shape2D.Union(f, shape2Ds));
   }

   public Shape2D union(Shape2D shape2D) {
      return new Shape2D.Union(0.0F, new Shape2D[]{this, shape2D});
   }

   public Shape2D blend(float f, Shape2D shape2D) {
      return new Shape2D.Union(f, new Shape2D[]{this, shape2D});
   }

   public Shape2D subtract(Shape2D shape2D) {
      return new Shape2D.Op(this, shape2D, 1, 0.0F);
   }

   public Shape2D subtract(float f, Shape2D shape2D) {
      return new Shape2D.Op(this, shape2D, 1, f);
   }

   public Shape2D intersect(Shape2D shape2D) {
      return new Shape2D.Op(this, shape2D, 2, 0.0F);
   }

   public Shape2D intersect(float f, Shape2D shape2D) {
      return new Shape2D.Op(this, shape2D, 2, f);
   }

   public Shape2D inflate(float f) {
      return new Shape2D.Offset(this, f, false);
   }

   public Shape2D outline(float f) {
      return new Shape2D.Offset(this, f * 0.5F, true);
   }

   public Shape2D move(float f, float g) {
      return new Shape2D.Transform(this, 1.0F, 0.0F, f, g, 0.0F, 0.0F, 1.0F);
   }

   public Shape2D rotate(float f, float g, float h) {
      double var4 = Math.toRadians(f);
      return new Shape2D.Transform(this, (float)Math.cos(var4), (float)Math.sin(var4), 0.0F, 0.0F, g, h, 1.0F);
   }

   public Shape2D scale(float f, float g, float h) {
      return new Shape2D.Transform(this, 1.0F, 0.0F, 0.0F, 0.0F, g, h, f);
   }

   public Shape2D mirrorX(float f) {
      return new Shape2D.Flip(this, f);
   }

   static float clamp01(float f) {
      return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
   }

   @Environment(EnvType.CLIENT)
   static final class Box extends Shape2D {
      private final float cx;
      private final float cy;
      private final float hx;
      private final float hy;
      private final float r;

      Box(float f, float g, float h, float i, float j) {
         this.cx = f;
         this.cy = g;
         this.r = Math.max(0.0F, Math.min(j, Math.min(h, i)));
         this.hx = h - this.r;
         this.hy = i - this.r;
         this.bounds(f - h, g - i, f + h, g + i);
      }

      @Override
      public float distance(float f, float g) {
         float var3 = Math.abs(f - this.cx) - this.hx;
         float var4 = Math.abs(g - this.cy) - this.hy;
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

      Circle(float f, float g, float h) {
         this.cx = f;
         this.cy = g;
         this.r = h;
         this.bounds(f - h, g - h, f + h, g + h);
      }

      @Override
      public float distance(float f, float g) {
         float var3 = f - this.cx;
         float var4 = g - this.cy;
         return (float)Math.sqrt(var3 * var3 + var4 * var4) - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Custom extends Shape2D {
      private final Shape2D.Field field;

      Custom(Shape2D.Field field, float f, float g, float h, float i) {
         this.field = field;
         this.bounds(f, g, h, i);
      }

      @Override
      public float distance(float f, float g) {
         return this.field.distance(f, g);
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

      Ellipse(float f, float g, float h, float i) {
         this.cx = f;
         this.cy = g;
         this.ix = 1.0F / h;
         this.iy = 1.0F / i;
         this.iix = this.ix * this.ix;
         this.iiy = this.iy * this.iy;
         this.rmin = Math.min(h, i);
         this.bounds(f - h, g - i, f + h, g + i);
      }

      @Override
      public float distance(float f, float g) {
         float var3 = f - this.cx;
         float var4 = g - this.cy;
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
      float distance(float f, float g);
   }

   @Environment(EnvType.CLIENT)
   static final class Flip extends Shape2D {
      private final Shape2D child;
      private final float axis;

      Flip(Shape2D shape2D, float f) {
         this.child = shape2D;
         this.axis = f;
         this.bounds(2.0F * f - shape2D.maxX, shape2D.minY, 2.0F * f - shape2D.minX, shape2D.maxY);
      }

      @Override
      public float distance(float f, float g) {
         return this.child.distance(2.0F * this.axis - f, g);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Heart extends Shape2D {
      private final float cx;
      private final float cy;
      private final float size;

      Heart(float f, float g, float h) {
         this.cx = f;
         this.cy = g;
         this.size = h;
         this.bounds(f - h * 0.7F, g - h * 0.35F, f + h * 0.7F, g + h * 0.8F);
      }

      @Override
      public float distance(float f, float g) {
         float var4 = Math.abs(f - this.cx) / this.size;
         float var3;
         if (var4 + (var3 = (this.cy + this.size * 0.75F - g) / this.size) > 1.0F) {
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

      Offset(Shape2D shape2D, float f, boolean bl) {
         this.child = shape2D;
         this.r = f;
         this.ring = bl;
         float var4 = Math.abs(f);
         this.bounds(shape2D.minX - var4, shape2D.minY - var4, shape2D.maxX + var4, shape2D.maxY + var4);
      }

      @Override
      public float distance(float f, float g) {
         float var3 = this.child.distance(f, g);
         return this.ring ? Math.abs(var3) - this.r : var3 - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Op extends Shape2D {
      private final Shape2D a;
      private final Shape2D b;
      private final int mode;
      private final float k;

      Op(Shape2D shape2D, Shape2D shape2D2, int i, float f) {
         this.a = shape2D;
         this.b = shape2D2;
         this.mode = i;
         this.k = Math.max(0.0F, f);
         if (i == 2) {
            this.bounds(
               Math.max(shape2D.minX, shape2D2.minX),
               Math.max(shape2D.minY, shape2D2.minY),
               Math.min(shape2D.maxX, shape2D2.maxX),
               Math.min(shape2D.maxY, shape2D2.maxY)
            );
         } else {
            this.bounds(shape2D.minX, shape2D.minY, shape2D.maxX, shape2D.maxY);
         }
      }

      @Override
      public float distance(float f, float g) {
         float var3 = this.a.distance(f, g);
         float var4 = this.b.distance(f, g);
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

      Path(float[] fs, float f, float g, boolean bl) {
         if (fs.length >= 4 && fs.length % 2 == 0) {
            int var6 = fs.length / 2;
            float[] var7 = new float[var6];

            for (int var8 = 1; var8 < var6; var8++) {
               float var9 = fs[var8 * 2] - fs[var8 * 2 - 2];
               float var10 = fs[var8 * 2 + 1] - fs[var8 * 2 - 1];
               var7[var8] = var7[var8 - 1] + (float)Math.sqrt(var9 * var9 + var10 * var10);
            }

            float var17 = Math.max(1.0E-6F, var7[var6 - 1]);
            float[] var18 = new float[var6];

            for (int var5 = 0; var5 < var6; var5++) {
               var18[var5] = f + (g - f) * var7[var5] / var17;
            }

            if (!bl && var6 != 2) {
               this.count = var6 - 2;
               this.seg = new float[this.count * 12];

               for (int var16 = 1; var16 <= this.count; var16++) {
                  float var19 = var16 == 1 ? fs[0] : (fs[var16 * 2 - 2] + fs[var16 * 2]) * 0.5F;
                  float var11 = var16 == 1 ? fs[1] : (fs[var16 * 2 - 1] + fs[var16 * 2 + 1]) * 0.5F;
                  float var12 = var16 == 1 ? var18[0] : (var18[var16 - 1] + var18[var16]) * 0.5F;
                  float var13 = var16 == this.count ? fs[var16 * 2 + 2] : (fs[var16 * 2] + fs[var16 * 2 + 2]) * 0.5F;
                  float var14 = var16 == this.count ? fs[var16 * 2 + 3] : (fs[var16 * 2 + 1] + fs[var16 * 2 + 3]) * 0.5F;
                  float var15 = var16 == this.count ? var18[var16 + 1] : (var18[var16] + var18[var16 + 1]) * 0.5F;
                  fill(this.seg, (var16 - 1) * 12, var19, var11, var12, fs[var16 * 2], fs[var16 * 2 + 1], var18[var16], var13, var14, var15);
               }
            } else {
               this.count = 1;
               this.seg = new float[12];
               if (var6 == 2) {
                  fill(
                     this.seg, 0, fs[0], fs[1], var18[0], (fs[0] + fs[2]) * 0.5F, (fs[1] + fs[3]) * 0.5F, (var18[0] + var18[1]) * 0.5F, fs[2], fs[3], var18[1]
                  );
               } else {
                  fill(this.seg, 0, fs[0], fs[1], f, fs[2], fs[3], (f + g) * 0.5F, fs[4], fs[5], g);
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

      private static void fill(float[] fs, int i, float f, float g, float h, float j, float k, float l, float m, float n, float o) {
         fs[i] = f;
         fs[i + 1] = g;
         fs[i + 2] = j;
         fs[i + 3] = k;
         fs[i + 4] = m;
         fs[i + 5] = n;
         fs[i + 6] = h;
         fs[i + 7] = o;
         float var11 = Math.max(h, Math.max(l, o));
         fs[i + 8] = Math.min(f, Math.min(j, m)) - var11;
         fs[i + 9] = Math.min(g, Math.min(k, n)) - var11;
         fs[i + 10] = Math.max(f, Math.max(j, m)) + var11;
         fs[i + 11] = Math.max(g, Math.max(k, n)) + var11;
      }

      @Override
      public float distance(float f, float g) {
         float var3 = 3.4028235E38F;

         for (int var4 = 0; var4 < this.count; var4++) {
            int var6 = var4 * 12;
            float var7 = f < this.seg[var6 + 8] ? this.seg[var6 + 8] - f : (f > this.seg[var6 + 10] ? f - this.seg[var6 + 10] : 0.0F);
            float var8 = g < this.seg[var6 + 9] ? this.seg[var6 + 9] - g : (g > this.seg[var6 + 11] ? g - this.seg[var6 + 11] : 0.0F);
            float var5;
            if ((!(var3 > 0.0F) || var3 == 3.4028235E38F || !(var7 * var7 + var8 * var8 >= var3 * var3)) && (var5 = this.segment(var6, f, g)) < var3) {
               var3 = var5;
            }
         }

         return var3;
      }

      private float segment(int i, float f, float g) {
         float var5 = this.seg[i];
         float var6 = this.seg[i + 1];
         float var7 = this.seg[i + 2];
         float var8 = this.seg[i + 3];
         float var9 = this.seg[i + 4];
         float var10 = this.seg[i + 5];
         float var11 = var7 - var5;
         float var12 = var8 - var6;
         float var13 = var5 - 2.0F * var7 + var9;
         float var14 = var6 - 2.0F * var8 + var10;
         float var15 = var13 * var13 + var14 * var14;
         float var16 = var5 - f;
         float var17 = var6 - g;
         float var18 = var9 - var5;
         float var19 = var10 - var6;
         float var20 = var18 * var18 + var19 * var19;
         float var4;
         if (var15 < 1.0E-5F * Math.max(var20, 1.0E-6F)) {
            var4 = var20 > 1.0E-12F ? clamp01(((f - var5) * var18 + (g - var6) * var19) / var20) : 0.0F;
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
         float var42 = this.seg[i + 6] + (this.seg[i + 7] - this.seg[i + 6]) * var4;
         return (float)Math.sqrt(var40 * var40 + var41 * var41) - var42;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Polygon extends Shape2D {
      private final float[] v;
      private final int n;

      Polygon(float[] fs) {
         if (fs.length >= 6 && fs.length % 2 == 0) {
            this.v = (float[])fs.clone();
            this.n = fs.length / 2;
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
      public float distance(float f, float g) {
         float var3 = f - this.v[0];
         float var4 = g - this.v[1];
         float var5 = var3 * var3 + var4 * var4;
         float var6 = 1.0F;
         int var7 = 0;

         for (int var8 = this.n - 1; var7 < this.n; var8 = var7++) {
            float var10 = this.v[var8 * 2] - this.v[var7 * 2];
            float var11 = this.v[var8 * 2 + 1] - this.v[var7 * 2 + 1];
            float var12 = f - this.v[var7 * 2];
            float var13 = g - this.v[var7 * 2 + 1];
            float var14 = var10 * var10 + var11 * var11;
            float var15 = var14 > 1.0E-12F ? clamp01((var12 * var10 + var13 * var11) / var14) : 0.0F;
            float var16 = var12 - var10 * var15;
            float var17 = var13 - var11 * var15;
            var5 = Math.min(var5, var16 * var16 + var17 * var17);
            boolean var18 = g >= this.v[var7 * 2 + 1];
            boolean var19 = g < this.v[var8 * 2 + 1];
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

      Taper(float f, float g, float h, float i, float j, float k) {
         this.ax = f;
         this.ay = g;
         this.ra = h;
         this.rb = k;
         this.bax = i - f;
         this.bay = j - g;
         this.l2 = this.bax * this.bax + this.bay * this.bay;
         this.rr = h - k;
         this.a2 = this.l2 - this.rr * this.rr;
         this.il2 = this.l2 > 1.0E-12F ? 1.0F / this.l2 : 0.0F;
         this.circle = this.a2 <= 1.0E-6F;
         boolean var7 = h >= k;
         this.sx = var7 ? f : i;
         this.sy = var7 ? g : j;
         this.sr = Math.max(h, k);
         this.bounds(Math.min(f - h, i - k), Math.min(g - h, j - k), Math.max(f + h, i + k), Math.max(g + h, j + k));
      }

      @Override
      public float distance(float f, float g) {
         if (this.circle) {
            float var13 = f - this.sx;
            float var14 = g - this.sy;
            return (float)Math.sqrt(var13 * var13 + var14 * var14) - this.sr;
         } else {
            float var3 = f - this.ax;
            float var4 = g - this.ay;
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

      Transform(Shape2D shape2D, float f, float g, float h, float i, float j, float k, float l) {
         this.child = shape2D;
         this.c = f;
         this.s = g;
         this.dx = h;
         this.dy = i;
         this.cx = j;
         this.cy = k;
         this.k = l;
         float var9 = 100000.0F;
         float var10 = 100000.0F;
         float var11 = -100000.0F;
         float var12 = -100000.0F;

         for (int var13 = 0; var13 < 4; var13++) {
            float var14 = ((var13 & 1) == 0 ? shape2D.minX : shape2D.maxX) - j;
            float var15 = ((var13 & 2) == 0 ? shape2D.minY : shape2D.maxY) - k;
            float var16 = (f * var14 - g * var15) * l + j + h;
            float var17 = (g * var14 + f * var15) * l + k + i;
            var9 = Math.min(var9, var16);
            var10 = Math.min(var10, var17);
            var11 = Math.max(var11, var16);
            var12 = Math.max(var12, var17);
         }

         this.bounds(var9, var10, var11, var12);
      }

      @Override
      public float distance(float f, float g) {
         float var3 = (f - this.dx - this.cx) / this.k;
         float var4 = (g - this.dy - this.cy) / this.k;
         return this.child.distance(this.c * var3 + this.s * var4 + this.cx, -this.s * var3 + this.c * var4 + this.cy) * this.k;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Union extends Shape2D {
      private final Shape2D[] kids;
      private final float k;

      Union(float f, Shape2D[] shape2Ds) {
         this.kids = (Shape2D[])shape2Ds.clone();
         this.k = Math.max(0.0F, f);
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
      public float distance(float f, float g) {
         float var3 = this.kids[0].distance(f, g);

         for (int var4 = 1; var4 < this.kids.length; var4++) {
            float var5 = this.kids[var4].distance(f, g);
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
