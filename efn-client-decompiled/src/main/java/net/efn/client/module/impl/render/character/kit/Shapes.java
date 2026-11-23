package net.efn.client.module.impl.render.character.kit;

import java.util.Arrays;
import java.util.function.UnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Shapes {
   private static final ThreadLocal<Shapes.Scratch> SCRATCH = ThreadLocal.withInitial(Shapes.Scratch::new);

   private Shapes() {
   }

   public static Shape sphere(float f, float g, float h, float i) {
      return new Shapes.Sphere(f, g, h, i);
   }

   public static Shape ellipsoid(float f, float g, float h, float i, float j, float k) {
      return new Shapes.Ellipsoid(f, g, h, i, j, k);
   }

   public static Shape box(float f, float g, float h, float i, float j, float k) {
      return new Shapes.Box(f, g, h, i, j, k, 0.0F);
   }

   public static Shape box(float f, float g, float h, float i, float j, float k, float l) {
      return new Shapes.Box(f, g, h, i, j, k, l);
   }

   public static Shape capsule(float f, float g, float h, float i, float j, float k, float l) {
      return new Shapes.RoundCone(f, g, h, l, i, j, k, l);
   }

   public static Shape cone(float f, float g, float h, float i, float j, float k, float l, float m) {
      return new Shapes.RoundCone(f, g, h, i, j, k, l, m);
   }

   public static Shape cylinder(float f, float g, float h, float i, float j, float k, float l, float m) {
      return new Shapes.Cylinder(f, g, h, i, j, k, l, m);
   }

   public static Shape torus(float f, float g, float h, float i, float j) {
      return new Shapes.Torus(f, g, h, i, j);
   }

   public static Shape plane(float f, float g, float h, float i, float j, float k) {
      return new Shapes.Plane(f, g, h, i, j, k);
   }

   public static Shape below(float f) {
      return new Shapes.Plane(0.0F, f, 0.0F, 0.0F, 1.0F, 0.0F);
   }

   public static Shape above(float f) {
      return new Shapes.Plane(0.0F, f, 0.0F, 0.0F, -1.0F, 0.0F);
   }

   public static Strand strand(float f, float g, float... fs) {
      return Strand.tapered(f, g, fs);
   }

   public static Strand tube(float... fs) {
      return Strand.of(fs, false);
   }

   public static Shape extrude(Shape2D shape2D, float f, float g) {
      return new Shapes.Extrude(shape2D, f, g);
   }

   public static Shape field(Shapes.Field field, float f, float g, float h, float i, float j, float k) {
      return new Shapes.Custom(field, f, g, h, i, j, k);
   }

   public static Shape union(Shape... shapes) {
      return (Shape)(shapes.length == 1 ? shapes[0] : new Shapes.Union(0.0F, shapes));
   }

   public static Shape blend(float f, Shape... shapes) {
      return (Shape)(shapes.length == 1 ? shapes[0] : new Shapes.Union(f, shapes));
   }

   public static Shape subtract(Shape shape, Shape shape2) {
      return new Shapes.Subtract(0.0F, shape, shape2);
   }

   public static Shape subtract(float f, Shape shape, Shape shape2) {
      return new Shapes.Subtract(f, shape, shape2);
   }

   public static Shape intersect(Shape shape, Shape shape2) {
      return new Shapes.Intersect(0.0F, shape, shape2);
   }

   public static Shape intersect(float f, Shape shape, Shape shape2) {
      return new Shapes.Intersect(f, shape, shape2);
   }

   static float clamp01(float f) {
      return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
   }

   static float length(float f, float g, float h) {
      return (float)Math.sqrt(f * f + g * g + h * h);
   }

   static float boxLowerBound(Shape shape, float f, float g, float h) {
      float var4 = (shape.minX + shape.maxX) * 0.5F;
      float var5 = (shape.minY + shape.maxY) * 0.5F;
      float var6 = (shape.minZ + shape.maxZ) * 0.5F;
      float var7 = Math.abs(f - var4) - (shape.maxX - shape.minX) * 0.5F;
      float var8 = Math.abs(g - var5) - (shape.maxY - shape.minY) * 0.5F;
      float var9 = Math.abs(h - var6) - (shape.maxZ - shape.minZ) * 0.5F;
      float var10 = Math.max(var7, 0.0F);
      float var11 = Math.max(var8, 0.0F);
      float var12 = Math.max(var9, 0.0F);
      return (float)Math.sqrt(var10 * var10 + var11 * var11 + var12 * var12) + Math.min(Math.max(var7, Math.max(var8, var9)), 0.0F);
   }

   @Environment(EnvType.CLIENT)
   static final class Box extends Shape {
      private final float cx;
      private final float cy;
      private final float cz;
      private final float hx;
      private final float hy;
      private final float hz;
      private final float r;

      Box(float f, float g, float h, float i, float j, float k, float l) {
         this.cx = f;
         this.cy = g;
         this.cz = h;
         this.r = Math.max(0.0F, Math.min(l, Math.min(i, Math.min(j, k))));
         this.hx = i - this.r;
         this.hy = j - this.r;
         this.hz = k - this.r;
         this.bounds(f - i, g - j, h - k, f + i, g + j, h + k);
      }

      @Override
      public float distance(float f, float g, float h) {
         float var4 = Math.abs(f - this.cx) - this.hx;
         float var5 = Math.abs(g - this.cy) - this.hy;
         float var6 = Math.abs(h - this.cz) - this.hz;
         float var7 = Math.max(var4, 0.0F);
         float var8 = Math.max(var5, 0.0F);
         float var9 = Math.max(var6, 0.0F);
         return Shapes.length(var7, var8, var9) + Math.min(Math.max(var4, Math.max(var5, var6)), 0.0F) - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Custom extends Shape {
      private final Shapes.Field field;

      Custom(Shapes.Field field, float f, float g, float h, float i, float j, float k) {
         this.field = field;
         this.bounds(f, g, h, i, j, k);
      }

      @Override
      public float distance(float f, float g, float h) {
         return this.field.distance(f, g, h);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Cylinder extends Shape {
      private final float ax;
      private final float ay;
      private final float az;
      private final float bax;
      private final float bay;
      private final float baz;
      private final float baba;
      private final float r;
      private final float round;

      Cylinder(float f, float g, float h, float i, float j, float k, float l, float m) {
         float var9 = i - f;
         float var10 = j - g;
         float var11 = k - h;
         float var12 = Shapes.length(var9, var10, var11);
         float var13 = Math.max(0.0F, Math.min(m, Math.min(l, var12 * 0.5F)));
         float var14 = var12 > 1.0E-6F ? var9 / var12 : 0.0F;
         float var15 = var12 > 1.0E-6F ? var10 / var12 : 1.0F;
         float var16 = var12 > 1.0E-6F ? var11 / var12 : 0.0F;
         this.ax = f + var14 * var13;
         this.ay = g + var15 * var13;
         this.az = h + var16 * var13;
         this.bax = i - var14 * var13 - this.ax;
         this.bay = j - var15 * var13 - this.ay;
         this.baz = k - var16 * var13 - this.az;
         this.baba = Math.max(1.0E-8F, this.bax * this.bax + this.bay * this.bay + this.baz * this.baz);
         this.r = l - var13;
         this.round = var13;
         float var17 = l * (float)Math.sqrt(Math.max(0.0F, 1.0F - var14 * var14));
         float var18 = l * (float)Math.sqrt(Math.max(0.0F, 1.0F - var15 * var15));
         float var19 = l * (float)Math.sqrt(Math.max(0.0F, 1.0F - var16 * var16));
         this.bounds(
            Math.min(f, i) - var17, Math.min(g, j) - var18, Math.min(h, k) - var19, Math.max(f, i) + var17, Math.max(g, j) + var18, Math.max(h, k) + var19
         );
      }

      @Override
      public float distance(float f, float g, float h) {
         float var4 = f - this.ax;
         float var5 = g - this.ay;
         float var6 = h - this.az;
         float var7 = var4 * this.bax + var5 * this.bay + var6 * this.baz;
         float var8 = Shapes.length(var4 * this.baba - this.bax * var7, var5 * this.baba - this.bay * var7, var6 * this.baba - this.baz * var7)
            - this.r * this.baba;
         float var9 = Math.abs(var7 - this.baba * 0.5F) - this.baba * 0.5F;
         float var10 = var8 * var8;
         float var11 = var9 * var9 * this.baba;
         float var12 = Math.max(var8, var9) < 0.0F ? -Math.min(var10, var11) : (var8 > 0.0F ? var10 : 0.0F) + (var9 > 0.0F ? var11 : 0.0F);
         return Math.signum(var12) * (float)Math.sqrt(Math.abs(var12)) / this.baba - this.round;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Ellipsoid extends Shape {
      private final float cx;
      private final float cy;
      private final float cz;
      private final float ix;
      private final float iy;
      private final float iz;
      private final float iix;
      private final float iiy;
      private final float iiz;
      private final float rmin;

      Ellipsoid(float f, float g, float h, float i, float j, float k) {
         this.cx = f;
         this.cy = g;
         this.cz = h;
         this.ix = 1.0F / i;
         this.iy = 1.0F / j;
         this.iz = 1.0F / k;
         this.iix = this.ix * this.ix;
         this.iiy = this.iy * this.iy;
         this.iiz = this.iz * this.iz;
         this.rmin = Math.min(i, Math.min(j, k));
         this.bounds(f - i, g - j, h - k, f + i, g + j, h + k);
      }

      @Override
      public float distance(float f, float g, float h) {
         float var4 = f - this.cx;
         float var5 = g - this.cy;
         float var6 = h - this.cz;
         float var7 = Shapes.length(var4 * this.ix, var5 * this.iy, var6 * this.iz);
         float var8 = Shapes.length(var4 * this.iix, var5 * this.iiy, var6 * this.iiz);
         return var8 < 1.0E-9F ? -this.rmin : var7 * (var7 - 1.0F) / var8;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Extrude extends Shape {
      private final Shape2D profile;
      private final float h;
      private final float r;

      Extrude(Shape2D shape2D, float f, float g) {
         this.profile = shape2D;
         this.r = Math.max(0.0F, Math.min(g, f));
         this.h = f;
         this.bounds(shape2D.minX, shape2D.minY, -f, shape2D.maxX, shape2D.maxY, f);
      }

      @Override
      public float distance(float f, float g, float h) {
         float var4 = this.profile.distance(f, g) + this.r;
         float var5 = Math.abs(h) - this.h + this.r;
         float var6 = Math.max(var4, 0.0F);
         float var7 = Math.max(var5, 0.0F);
         return Math.min(Math.max(var4, var5), 0.0F) + (float)Math.sqrt(var6 * var6 + var7 * var7) - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Field {
      float distance(float f, float g, float h);
   }

   @Environment(EnvType.CLIENT)
   static final class Intersect extends Shape {
      private final Shape a;
      private final Shape b;
      private final float k;

      Intersect(float f, Shape shape, Shape shape2) {
         this.k = Math.max(0.0F, f);
         this.a = shape;
         this.b = shape2;
         if (shape.bounded() && shape2.bounded()) {
            this.bounds(
               Math.max(shape.minX, shape2.minX),
               Math.max(shape.minY, shape2.minY),
               Math.max(shape.minZ, shape2.minZ),
               Math.min(shape.maxX, shape2.maxX),
               Math.min(shape.maxY, shape2.maxY),
               Math.min(shape.maxZ, shape2.maxZ)
            );
         } else if (shape.bounded()) {
            this.boundsOf(shape, 0.0F);
         } else if (shape2.bounded()) {
            this.boundsOf(shape2, 0.0F);
         }
      }

      @Override
      Shape prune(float f, float g, float h, float i, float j, float k) {
         Shape var7 = this.a.prune(f, g, h, i, j, k);
         Shape var8 = this.b.prune(f, g, h, i, j, k);
         return var7 == this.a && var8 == this.b ? this : new Shapes.Intersect(this.k, var7, var8);
      }

      @Override
      Shape map(UnaryOperator<Shape> unaryOperator) {
         Shape var2 = this.a.map(unaryOperator);
         Shape var3 = this.b.map(unaryOperator);
         return (Shape)unaryOperator.apply(var2 == this.a && var3 == this.b ? this : new Shapes.Intersect(this.k, var2, var3).inherit(this));
      }

      @Override
      public float distance(float f, float g, float h) {
         float var4 = this.a.distance(f, g, h);
         float var5 = this.b.distance(f, g, h);
         if (this.k <= 0.0F) {
            return Math.max(var4, var5);
         } else {
            float var6 = Shapes.clamp01(0.5F - 0.5F * (var5 - var4) / this.k);
            return var5 + (var4 - var5) * var6 + this.k * var6 * (1.0F - var6);
         }
      }

      @Override
      float shade(float f, float g, float h, Sample sample) {
         float var5 = this.a.sample(f, g, h, sample);
         if (this.b.paint == null) {
            float var9 = this.b.distance(f, g, h);
            if (this.k <= 0.0F) {
               return Math.max(var5, var9);
            } else {
               float var10 = Shapes.clamp01(0.5F - 0.5F * (var9 - var5) / this.k);
               return var9 + (var5 - var9) * var10 + this.k * var10 * (1.0F - var10);
            }
         } else {
            Sample var6 = new Sample();
            float var7 = this.b.sample(f, g, h, var6);
            if (this.k <= 0.0F) {
               if (var7 > var5) {
                  sample.set(var6);
                  return var7;
               } else {
                  return var5;
               }
            } else {
               float var8 = Shapes.clamp01(0.5F - 0.5F * (var7 - var5) / this.k);
               sample.mix(var6, 1.0F - var8);
               return var7 + (var5 - var7) * var8 + this.k * var8 * (1.0F - var8);
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Mirror extends Shape {
      private final Shape child;
      private final boolean keep;

      Mirror(Shape shape, boolean bl) {
         this.child = shape;
         this.keep = bl;
         if (shape.bounded()) {
            if (bl) {
               this.bounds(Math.min(shape.minX, -shape.maxX), shape.minY, shape.minZ, Math.max(shape.maxX, -shape.minX), shape.maxY, shape.maxZ);
            } else {
               this.bounds(-shape.maxX, shape.minY, shape.minZ, -shape.minX, shape.maxY, shape.maxZ);
            }
         }
      }

      @Override
      Shape prune(float f, float g, float h, float i, float j, float k) {
         Shape var7 = this.keep ? this.child.prune(Math.min(f, -i), g, h, Math.max(i, -f), j, k) : this.child.prune(-i, g, h, -f, j, k);
         return var7 == this.child ? this : new Shapes.Mirror(var7, this.keep);
      }

      @Override
      Shape map(UnaryOperator<Shape> unaryOperator) {
         Shape var2 = this.child.map(unaryOperator);
         return (Shape)unaryOperator.apply(var2 == this.child ? this : new Shapes.Mirror(var2, this.keep).inherit(this));
      }

      @Override
      public float distance(float f, float g, float h) {
         if (!this.keep) {
            return this.child.distance(-f, g, h);
         } else {
            float var4 = this.child.distance(f, g, h);
            return Shapes.boxLowerBound(this.child, -f, g, h) >= var4 ? var4 : Math.min(var4, this.child.distance(-f, g, h));
         }
      }

      @Override
      float shade(float f, float g, float h, Sample sample) {
         if (!this.keep) {
            return this.child.sample(-f, g, h, sample);
         } else {
            float var5 = this.child.distance(f, g, h);
            float var6 = Shapes.boxLowerBound(this.child, -f, g, h) >= var5 ? 3.4028235E38F : this.child.distance(-f, g, h);
            return var6 < var5 ? this.child.sample(-f, g, h, sample) : this.child.sample(f, g, h, sample);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Plane extends Shape {
      private final float px;
      private final float py;
      private final float pz;
      private final float nx;
      private final float ny;
      private final float nz;

      Plane(float f, float g, float h, float i, float j, float k) {
         float var7 = Shapes.length(i, j, k);
         this.px = f;
         this.py = g;
         this.pz = h;
         this.nx = var7 < 1.0E-8F ? 0.0F : i / var7;
         this.ny = var7 < 1.0E-8F ? 1.0F : j / var7;
         this.nz = var7 < 1.0E-8F ? 0.0F : k / var7;
      }

      @Override
      public float distance(float f, float g, float h) {
         return (f - this.px) * this.nx + (g - this.py) * this.ny + (h - this.pz) * this.nz;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Revolve extends Shape {
      private final Shape2D profile;

      Revolve(Shape2D shape2D) {
         this.profile = shape2D;
         float var2 = Math.max(Math.abs(shape2D.minX), Math.abs(shape2D.maxX));
         this.bounds(-var2, shape2D.minY, -var2, var2, shape2D.maxY, var2);
      }

      @Override
      public float distance(float f, float g, float h) {
         return this.profile.distance((float)Math.sqrt(f * f + h * h), g);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Rotate extends Shape {
      private final Shape child;
      private final Mat3 inv;
      private final float cx;
      private final float cy;
      private final float cz;

      Rotate(Shape shape, Mat3 mat3, float f, float g, float h) {
         this.child = shape;
         this.inv = mat3.transpose();
         this.cx = f;
         this.cy = g;
         this.cz = h;
         if (shape.bounded()) {
            float var6 = 100000.0F;
            float var7 = 100000.0F;
            float var8 = 100000.0F;
            float var9 = -100000.0F;
            float var10 = -100000.0F;
            float var11 = -100000.0F;

            for (int var12 = 0; var12 < 8; var12++) {
               float var13 = ((var12 & 1) == 0 ? shape.minX : shape.maxX) - f;
               float var14 = ((var12 & 2) == 0 ? shape.minY : shape.maxY) - g;
               float var15 = ((var12 & 4) == 0 ? shape.minZ : shape.maxZ) - h;
               float var16 = mat3.x(var13, var14, var15) + f;
               float var17 = mat3.y(var13, var14, var15) + g;
               float var18 = mat3.z(var13, var14, var15) + h;
               var6 = Math.min(var6, var16);
               var7 = Math.min(var7, var17);
               var8 = Math.min(var8, var18);
               var9 = Math.max(var9, var16);
               var10 = Math.max(var10, var17);
               var11 = Math.max(var11, var18);
            }

            this.bounds(var6, var7, var8, var9, var10, var11);
         }
      }

      @Override
      Shape prune(float f, float g, float h, float i, float j, float k) {
         float var7 = 100000.0F;
         float var8 = 100000.0F;
         float var9 = 100000.0F;
         float var10 = -100000.0F;
         float var11 = -100000.0F;
         float var12 = -100000.0F;

         for (int var13 = 0; var13 < 8; var13++) {
            float var14 = ((var13 & 1) == 0 ? f : i) - this.cx;
            float var15 = ((var13 & 2) == 0 ? g : j) - this.cy;
            float var16 = ((var13 & 4) == 0 ? h : k) - this.cz;
            float var17 = this.inv.x(var14, var15, var16) + this.cx;
            float var18 = this.inv.y(var14, var15, var16) + this.cy;
            float var19 = this.inv.z(var14, var15, var16) + this.cz;
            var7 = Math.min(var7, var17);
            var8 = Math.min(var8, var18);
            var9 = Math.min(var9, var19);
            var10 = Math.max(var10, var17);
            var11 = Math.max(var11, var18);
            var12 = Math.max(var12, var19);
         }

         Shape var20 = this.child.prune(var7, var8, var9, var10, var11, var12);
         return var20 == this.child ? this : new Shapes.Rotate(var20, this.inv.transpose(), this.cx, this.cy, this.cz);
      }

      @Override
      Shape map(UnaryOperator<Shape> unaryOperator) {
         Shape var2 = this.child.map(unaryOperator);
         return (Shape)unaryOperator.apply(var2 == this.child ? this : new Shapes.Rotate(var2, this.inv.transpose(), this.cx, this.cy, this.cz).inherit(this));
      }

      @Override
      public float distance(float f, float g, float h) {
         float var4 = f - this.cx;
         float var5 = g - this.cy;
         float var6 = h - this.cz;
         return this.child.distance(this.inv.x(var4, var5, var6) + this.cx, this.inv.y(var4, var5, var6) + this.cy, this.inv.z(var4, var5, var6) + this.cz);
      }

      @Override
      float shade(float f, float g, float h, Sample sample) {
         float var5 = f - this.cx;
         float var6 = g - this.cy;
         float var7 = h - this.cz;
         return this.child
            .sample(this.inv.x(var5, var6, var7) + this.cx, this.inv.y(var5, var6, var7) + this.cy, this.inv.z(var5, var6, var7) + this.cz, sample);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Roughen extends Shape {
      private final Shape child;
      private final float amp;
      private final float freq;

      Roughen(Shape shape, float f, float g) {
         this.child = shape;
         this.amp = f;
         this.freq = 1.0F / Math.max(0.01F, g);
         this.boundsOf(shape, Math.abs(f));
      }

      @Override
      Shape prune(float f, float g, float h, float i, float j, float k) {
         Shape var7 = this.child.prune(f, g, h, i, j, k);
         return var7 == this.child ? this : new Shapes.Roughen(var7, this.amp, 1.0F / this.freq);
      }

      @Override
      Shape map(UnaryOperator<Shape> unaryOperator) {
         Shape var2 = this.child.map(unaryOperator);
         return (Shape)unaryOperator.apply(var2 == this.child ? this : new Shapes.Roughen(var2, this.amp, 1.0F / this.freq).inherit(this));
      }

      @Override
      public float distance(float f, float g, float h) {
         float var4 = this.child.distance(f, g, h);
         return var4 > this.amp * 1.5F + 0.5F ? var4 : var4 + this.amp * Noise.noise(f * this.freq, g * this.freq, h * this.freq);
      }

      @Override
      float shade(float f, float g, float h, Sample sample) {
         return this.child.sample(f, g, h, sample) + this.amp * Noise.noise(f * this.freq, g * this.freq, h * this.freq);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Round extends Shape {
      private final Shape child;
      private final float r;

      Round(Shape shape, float f) {
         this.child = shape;
         this.r = f;
         this.boundsOf(shape, Math.max(0.0F, f));
      }

      @Override
      Shape prune(float f, float g, float h, float i, float j, float k) {
         Shape var7 = this.child.prune(f, g, h, i, j, k);
         return var7 == this.child ? this : new Shapes.Round(var7, this.r);
      }

      @Override
      Shape map(UnaryOperator<Shape> unaryOperator) {
         Shape var2 = this.child.map(unaryOperator);
         return (Shape)unaryOperator.apply(var2 == this.child ? this : new Shapes.Round(var2, this.r).inherit(this));
      }

      @Override
      public float distance(float f, float g, float h) {
         return this.child.distance(f, g, h) - this.r;
      }

      @Override
      float shade(float f, float g, float h, Sample sample) {
         return this.child.sample(f, g, h, sample) - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class RoundCone extends Shape {
      private final float ax;
      private final float ay;
      private final float az;
      private final float bax;
      private final float bay;
      private final float baz;
      private final float ra;
      private final float rb;
      private final float l2;
      private final float rr;
      private final float a2;
      private final float il2;
      private final boolean sphere;
      private final float sx;
      private final float sy;
      private final float sz;
      private final float sr;

      RoundCone(float f, float g, float h, float i, float j, float k, float l, float m) {
         this.ax = f;
         this.ay = g;
         this.az = h;
         this.ra = i;
         this.rb = m;
         this.bax = j - f;
         this.bay = k - g;
         this.baz = l - h;
         this.l2 = this.bax * this.bax + this.bay * this.bay + this.baz * this.baz;
         this.rr = i - m;
         this.a2 = this.l2 - this.rr * this.rr;
         this.il2 = this.l2 > 1.0E-12F ? 1.0F / this.l2 : 0.0F;
         this.sphere = this.a2 <= 1.0E-6F;
         boolean var9 = i >= m;
         this.sx = var9 ? f : j;
         this.sy = var9 ? g : k;
         this.sz = var9 ? h : l;
         this.sr = Math.max(i, m);
         this.bounds(
            Math.min(f - i, j - m), Math.min(g - i, k - m), Math.min(h - i, l - m), Math.max(f + i, j + m), Math.max(g + i, k + m), Math.max(h + i, l + m)
         );
      }

      @Override
      public float distance(float f, float g, float h) {
         if (this.sphere) {
            return Shapes.length(f - this.sx, g - this.sy, h - this.sz) - this.sr;
         } else {
            float var4 = f - this.ax;
            float var5 = g - this.ay;
            float var6 = h - this.az;
            float var7 = var4 * this.bax + var5 * this.bay + var6 * this.baz;
            float var8 = var7 - this.l2;
            float var9 = var4 * this.l2 - this.bax * var7;
            float var10 = var5 * this.l2 - this.bay * var7;
            float var11 = var6 * this.l2 - this.baz * var7;
            float var12 = var9 * var9 + var10 * var10 + var11 * var11;
            float var13 = var7 * var7 * this.l2;
            float var14 = var8 * var8 * this.l2;
            float var15 = Math.signum(this.rr) * this.rr * this.rr * var12;
            if (Math.signum(var8) * this.a2 * var14 > var15) {
               return (float)Math.sqrt(var12 + var14) * this.il2 - this.rb;
            } else {
               return Math.signum(var7) * this.a2 * var13 < var15
                  ? (float)Math.sqrt(var12 + var13) * this.il2 - this.ra
                  : ((float)Math.sqrt(var12 * this.a2 * this.il2) + var7 * this.rr) * this.il2 - this.ra;
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Scale extends Shape {
      private final Shape child;
      private final float k;
      private final float ik;
      private final float cx;
      private final float cy;
      private final float cz;

      Scale(Shape shape, float f, float g, float h, float i) {
         this.child = shape;
         this.k = f;
         this.ik = 1.0F / f;
         this.cx = g;
         this.cy = h;
         this.cz = i;
         if (shape.bounded()) {
            this.bounds(
               (shape.minX - g) * f + g,
               (shape.minY - h) * f + h,
               (shape.minZ - i) * f + i,
               (shape.maxX - g) * f + g,
               (shape.maxY - h) * f + h,
               (shape.maxZ - i) * f + i
            );
         }
      }

      @Override
      Shape prune(float f, float g, float h, float i, float j, float k) {
         float var7 = (f - this.cx) * this.ik + this.cx;
         float var8 = (i - this.cx) * this.ik + this.cx;
         float var9 = (g - this.cy) * this.ik + this.cy;
         float var10 = (j - this.cy) * this.ik + this.cy;
         float var11 = (h - this.cz) * this.ik + this.cz;
         float var12 = (k - this.cz) * this.ik + this.cz;
         Shape var13 = this.child
            .prune(Math.min(var7, var8), Math.min(var9, var10), Math.min(var11, var12), Math.max(var7, var8), Math.max(var9, var10), Math.max(var11, var12));
         return var13 == this.child ? this : new Shapes.Scale(var13, this.k, this.cx, this.cy, this.cz);
      }

      @Override
      Shape map(UnaryOperator<Shape> unaryOperator) {
         Shape var2 = this.child.map(unaryOperator);
         return (Shape)unaryOperator.apply(var2 == this.child ? this : new Shapes.Scale(var2, this.k, this.cx, this.cy, this.cz).inherit(this));
      }

      @Override
      public float distance(float f, float g, float h) {
         return this.child.distance((f - this.cx) * this.ik + this.cx, (g - this.cy) * this.ik + this.cy, (h - this.cz) * this.ik + this.cz) * this.k;
      }

      @Override
      float shade(float f, float g, float h, Sample sample) {
         return this.child.sample((f - this.cx) * this.ik + this.cx, (g - this.cy) * this.ik + this.cy, (h - this.cz) * this.ik + this.cz, sample) * this.k;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Scratch {
      final float[][] pool = new float[64][];
      int depth;

      private Scratch() {
      }

      float[] take(int i) {
         int var2;
         if ((var2 = this.depth++) >= this.pool.length) {
            return new float[i];
         } else {
            float[] var3 = this.pool[var2];
            if (var3 == null || var3.length < i) {
               var3 = new float[Math.max(i, 16)];
               this.pool[var2] = var3;
            }

            return var3;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Shell extends Shape {
      private final Shape child;
      private final float half;

      Shell(Shape shape, float f) {
         this.child = shape;
         this.half = Math.abs(f) * 0.5F;
         this.boundsOf(shape, this.half);
      }

      @Override
      Shape prune(float f, float g, float h, float i, float j, float k) {
         Shape var7 = this.child.prune(f, g, h, i, j, k);
         return var7 == this.child ? this : new Shapes.Shell(var7, this.half * 2.0F);
      }

      @Override
      Shape map(UnaryOperator<Shape> unaryOperator) {
         Shape var2 = this.child.map(unaryOperator);
         return (Shape)unaryOperator.apply(var2 == this.child ? this : new Shapes.Shell(var2, this.half * 2.0F).inherit(this));
      }

      @Override
      public float distance(float f, float g, float h) {
         return Math.abs(this.child.distance(f, g, h)) - this.half;
      }

      @Override
      float shade(float f, float g, float h, Sample sample) {
         return Math.abs(this.child.sample(f, g, h, sample)) - this.half;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Sphere extends Shape {
      private final float cx;
      private final float cy;
      private final float cz;
      private final float r;

      Sphere(float f, float g, float h, float i) {
         this.cx = f;
         this.cy = g;
         this.cz = h;
         this.r = i;
         this.bounds(f - i, g - i, h - i, f + i, g + i, h + i);
      }

      @Override
      public float distance(float f, float g, float h) {
         return Shapes.length(f - this.cx, g - this.cy, h - this.cz) - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Subtract extends Shape {
      private final Shape base;
      private final Shape cutter;
      private final float k;

      Subtract(float f, Shape shape, Shape shape2) {
         this.k = Math.max(0.0F, f);
         this.base = shape;
         this.cutter = shape2;
         this.boundsOf(shape, this.k * 0.3F);
      }

      @Override
      Shape prune(float f, float g, float h, float i, float j, float k) {
         Shape var7 = this.base.prune(f, g, h, i, j, k);
         Shape var8 = this.cutter.prune(f, g, h, i, j, k);
         return var7 == this.base && var8 == this.cutter ? this : new Shapes.Subtract(this.k, var7, var8);
      }

      @Override
      Shape map(UnaryOperator<Shape> unaryOperator) {
         Shape var2 = this.base.map(unaryOperator);
         Shape var3 = this.cutter.map(unaryOperator);
         return (Shape)unaryOperator.apply(var2 == this.base && var3 == this.cutter ? this : new Shapes.Subtract(this.k, var2, var3).inherit(this));
      }

      @Override
      public float distance(float f, float g, float h) {
         float var4 = this.base.distance(f, g, h);
         if (Shapes.boxLowerBound(this.cutter, f, g, h) >= this.k - var4) {
            return var4;
         } else {
            float var5 = this.cutter.distance(f, g, h);
            if (this.k <= 0.0F) {
               return Math.max(var4, -var5);
            } else {
               float var6 = Shapes.clamp01(0.5F - 0.5F * (var4 + var5) / this.k);
               return var4 + (-var5 - var4) * var6 + this.k * var6 * (1.0F - var6);
            }
         }
      }

      @Override
      float shade(float f, float g, float h, Sample sample) {
         float var5 = this.base.sample(f, g, h, sample);
         if (Shapes.boxLowerBound(this.cutter, f, g, h) >= this.k - var5) {
            return var5;
         } else if (this.cutter.paint == null) {
            float var9 = this.cutter.distance(f, g, h);
            if (this.k <= 0.0F) {
               return Math.max(var5, -var9);
            } else {
               float var10 = Shapes.clamp01(0.5F - 0.5F * (var5 + var9) / this.k);
               return var5 + (-var9 - var5) * var10 + this.k * var10 * (1.0F - var10);
            }
         } else {
            Sample var6 = new Sample();
            float var7 = this.cutter.sample(f, g, h, var6);
            if (this.k <= 0.0F) {
               if (-var7 > var5) {
                  sample.set(var6);
                  return -var7;
               } else {
                  return var5;
               }
            } else {
               float var8 = Shapes.clamp01(0.5F - 0.5F * (var5 + var7) / this.k);
               sample.mix(var6, var8);
               return var5 + (-var7 - var5) * var8 + this.k * var8 * (1.0F - var8);
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Torus extends Shape {
      private final float cx;
      private final float cy;
      private final float cz;
      private final float major;
      private final float minor;

      Torus(float f, float g, float h, float i, float j) {
         this.cx = f;
         this.cy = g;
         this.cz = h;
         this.major = i;
         this.minor = j;
         float var6 = i + j;
         this.bounds(f - var6, g - j, h - var6, f + var6, g + j, h + var6);
      }

      @Override
      public float distance(float f, float g, float h) {
         float var4 = f - this.cx;
         float var5 = h - this.cz;
         float var6 = (float)Math.sqrt(var4 * var4 + var5 * var5) - this.major;
         float var7 = g - this.cy;
         return (float)Math.sqrt(var6 * var6 + var7 * var7) - this.minor;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Translate extends Shape {
      private final Shape child;
      private final float dx;
      private final float dy;
      private final float dz;

      Translate(Shape shape, float f, float g, float h) {
         this.child = shape;
         this.dx = f;
         this.dy = g;
         this.dz = h;
         if (shape.bounded()) {
            this.bounds(shape.minX + f, shape.minY + g, shape.minZ + h, shape.maxX + f, shape.maxY + g, shape.maxZ + h);
         }
      }

      @Override
      Shape prune(float f, float g, float h, float i, float j, float k) {
         Shape var7 = this.child.prune(f - this.dx, g - this.dy, h - this.dz, i - this.dx, j - this.dy, k - this.dz);
         return var7 == this.child ? this : new Shapes.Translate(var7, this.dx, this.dy, this.dz);
      }

      @Override
      Shape map(UnaryOperator<Shape> unaryOperator) {
         Shape var2 = this.child.map(unaryOperator);
         return (Shape)unaryOperator.apply(var2 == this.child ? this : new Shapes.Translate(var2, this.dx, this.dy, this.dz).inherit(this));
      }

      @Override
      public float distance(float f, float g, float h) {
         return this.child.distance(f - this.dx, g - this.dy, h - this.dz);
      }

      @Override
      float shade(float f, float g, float h, Sample sample) {
         return this.child.sample(f - this.dx, g - this.dy, h - this.dz, sample);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Twist extends Shape {
      private final Shape child;
      private final float k;
      private final float cx;
      private final float cz;

      Twist(Shape shape, float f, float g, float h) {
         this.child = shape;
         this.k = (float)Math.toRadians(f);
         this.cx = g;
         this.cz = h;
         if (shape.bounded()) {
            float var5 = 0.0F;

            for (int var6 = 0; var6 < 4; var6++) {
               float var7 = ((var6 & 1) == 0 ? shape.minX : shape.maxX) - g;
               float var8 = ((var6 & 2) == 0 ? shape.minZ : shape.maxZ) - h;
               var5 = Math.max(var5, (float)Math.sqrt(var7 * var7 + var8 * var8));
            }

            this.bounds(g - var5, shape.minY, h - var5, g + var5, shape.maxY, h + var5);
         }
      }

      @Override
      Shape map(UnaryOperator<Shape> unaryOperator) {
         Shape var2 = this.child.map(unaryOperator);
         return (Shape)unaryOperator.apply(var2 == this.child ? this : new Shapes.Twist(var2, (float)Math.toDegrees(this.k), this.cx, this.cz).inherit(this));
      }

      @Override
      public float distance(float f, float g, float h) {
         float var4 = -this.k * g;
         float var5 = (float)Math.cos(var4);
         float var6 = (float)Math.sin(var4);
         float var7 = f - this.cx;
         float var8 = h - this.cz;
         return this.child.distance(var5 * var7 + var6 * var8 + this.cx, g, -var6 * var7 + var5 * var8 + this.cz);
      }

      @Override
      float shade(float f, float g, float h, Sample sample) {
         float var5 = -this.k * g;
         float var6 = (float)Math.cos(var5);
         float var7 = (float)Math.sin(var5);
         float var8 = f - this.cx;
         float var9 = h - this.cz;
         return this.child.sample(var6 * var8 + var7 * var9 + this.cx, g, -var7 * var8 + var6 * var9 + this.cz, sample);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Union extends Shape {
      private final Shape[] kids;
      private final float k;

      Union(float f, Shape[] shapes) {
         if (shapes.length == 0) {
            throw new IllegalArgumentException("union needs at least one shape");
         } else {
            this.k = Math.max(0.0F, f);
            this.kids = (Shape[])shapes.clone();
            boolean var3 = false;
            float var4 = 100000.0F;
            float var5 = 100000.0F;
            float var6 = 100000.0F;
            float var7 = -100000.0F;
            float var8 = -100000.0F;
            float var9 = -100000.0F;

            for (Shape var13 : this.kids) {
               if (!var13.bounded()) {
                  var3 = true;
               } else {
                  var4 = Math.min(var4, var13.minX);
                  var5 = Math.min(var5, var13.minY);
                  var6 = Math.min(var6, var13.minZ);
                  var7 = Math.max(var7, var13.maxX);
                  var8 = Math.max(var8, var13.maxY);
                  var9 = Math.max(var9, var13.maxZ);
               }
            }

            if (!var3) {
               float var14 = this.k * 0.3F;
               this.bounds(var4 - var14, var5 - var14, var6 - var14, var7 + var14, var8 + var14, var9 + var14);
            }
         }
      }

      @Override
      Shape map(UnaryOperator<Shape> unaryOperator) {
         Shape[] var2 = new Shape[this.kids.length];
         boolean var3 = false;

         for (int var4 = 0; var4 < this.kids.length; var4++) {
            var2[var4] = this.kids[var4].map(unaryOperator);
            var3 |= var2[var4] != this.kids[var4];
         }

         return (Shape)unaryOperator.apply(var3 ? new Shapes.Union(this.k, var2).inherit(this) : this);
      }

      @Override
      Shape prune(float f, float g, float h, float i, float j, float k) {
         float var7 = (f + i) * 0.5F;
         float var8 = (g + j) * 0.5F;
         float var9 = (h + k) * 0.5F;
         float var10 = Shapes.length(i - f, j - g, k - h) * 0.5F * 1.2F;
         float var11 = 3.4028235E38F;

         for (Shape var15 : this.kids) {
            if (!(var15.boxGap(f, g, h, i, j, k) >= var11 + this.k)) {
               var11 = Math.min(var11, var15.distance(var7, var8, var9) + var10);
            }
         }

         Shape[] var20 = new Shape[this.kids.length];
         int var21 = 0;
         boolean var22 = false;

         for (Shape var18 : this.kids) {
            if (var18.boxGap(f, g, h, i, j, k) >= var11 + this.k) {
               var22 = true;
            } else {
               Shape var19 = var18.prune(f, g, h, i, j, k);
               var22 |= var19 != var18;
               var20[var21++] = var19;
            }
         }

         if (!var22 || var21 == 0) {
            return this;
         } else {
            return (Shape)(var21 == 1 ? var20[0] : new Shapes.Union(this.k, Arrays.copyOf(var20, var21)));
         }
      }

      private int nearest(float f, float g, float h, float[] fs) {
         int var5 = 0;
         float var6 = 3.4028235E38F;

         for (int var7 = 0; var7 < this.kids.length; var7++) {
            float var8 = Shapes.boxLowerBound(this.kids[var7], f, g, h);
            if (fs != null) {
               fs[var7] = var8;
            }

            if (var8 < var6) {
               var6 = var8;
               var5 = var7;
            }
         }

         return var5;
      }

      @Override
      public float distance(float f, float g, float h) {
         Shape[] var4 = this.kids;
         if (var4.length <= 4) {
            return this.distance(f, g, h, null);
         } else {
            Shapes.Scratch var5 = Shapes.SCRATCH.get();
            float[] var6 = var5.take(var4.length);

            float var8;
            try {
               float var7 = this.distance(f, g, h, var6);
               var8 = var7;
            } finally {
               var5.depth--;
            }

            return var8;
         }
      }

      @Override
      float distance(float f, float g, float h, float i) {
         Shape[] var5 = this.kids;
         if (var5.length <= 4) {
            return this.evaluate(f, g, h, null, i);
         } else {
            Shapes.Scratch var6 = Shapes.SCRATCH.get();
            float[] var7 = var6.take(var5.length);

            float var9;
            try {
               float var8 = this.evaluate(f, g, h, var7, i);
               var9 = var8;
            } finally {
               var6.depth--;
            }

            return var9;
         }
      }

      private float distance(float f, float g, float h, float[] fs) {
         return this.evaluate(f, g, h, fs, 3.4028235E38F);
      }

      private float evaluate(float f, float g, float h, float[] fs, float i) {
         Shape[] var7 = this.kids;
         int var8 = this.nearest(f, g, h, fs);
         float var6 = fs != null ? fs[var8] : Shapes.boxLowerBound(var7[var8], f, g, h);
         if (var6 >= i) {
            return var6;
         } else {
            float var10 = this.k;
            float var11 = var7[var8].distance(f, g, h, i == 3.4028235E38F ? 3.4028235E38F : i + var10);

            for (int var12 = 0; var12 < var7.length; var12++) {
               if (var12 != var8) {
                  float var13 = fs != null ? fs[var12] : Shapes.boxLowerBound(var7[var12], f, g, h);
                  if (!(var13 >= var11 + var10)) {
                     float var15 = var7[var12].distance(f, g, h, var11 + var10);
                     if (var10 > 0.0F) {
                        float var16 = Shapes.clamp01(0.5F + 0.5F * (var15 - var11) / var10);
                        var11 = var15 + (var11 - var15) * var16 - var10 * var16 * (1.0F - var16);
                     } else if (var15 < var11) {
                        var11 = var15;
                     }
                  }
               }
            }

            return var11;
         }
      }

      @Override
      float shade(float f, float g, float h, Sample sample) {
         Shape[] var5 = this.kids;
         int var6 = this.nearest(f, g, h, null);
         float var7 = var5[var6].sample(f, g, h, sample);
         Sample var8 = null;
         float var9 = this.k;

         for (int var10 = 0; var10 < var5.length; var10++) {
            Shape var11;
            if (var10 != var6 && !(Shapes.boxLowerBound(var11 = var5[var10], f, g, h) >= var7 + var9)) {
               if (var8 == null) {
                  var8 = new Sample();
               }

               float var12 = var11.sample(f, g, h, var8);
               if (var9 > 0.0F) {
                  float var13 = Shapes.clamp01(0.5F + 0.5F * (var12 - var7) / var9);
                  var7 = var12 + (var7 - var12) * var13 - var9 * var13 * (1.0F - var13);
                  sample.mix(var8, 1.0F - var13);
               } else if (var12 < var7) {
                  var7 = var12;
                  sample.set(var8);
               }
            }
         }

         return var7;
      }
   }
}
