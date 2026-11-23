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

   public static Shape sphere(float var0, float var1, float var2, float var3) {
      return new Shapes.Sphere(var0, var1, var2, var3);
   }

   public static Shape ellipsoid(float var0, float var1, float var2, float var3, float var4, float var5) {
      return new Shapes.Ellipsoid(var0, var1, var2, var3, var4, var5);
   }

   public static Shape box(float var0, float var1, float var2, float var3, float var4, float var5) {
      return new Shapes.Box(var0, var1, var2, var3, var4, var5, 0.0F);
   }

   public static Shape box(float var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      return new Shapes.Box(var0, var1, var2, var3, var4, var5, var6);
   }

   public static Shape capsule(float var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      return new Shapes.RoundCone(var0, var1, var2, var6, var3, var4, var5, var6);
   }

   public static Shape cone(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      return new Shapes.RoundCone(var0, var1, var2, var3, var4, var5, var6, var7);
   }

   public static Shape cylinder(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      return new Shapes.Cylinder(var0, var1, var2, var3, var4, var5, var6, var7);
   }

   public static Shape torus(float var0, float var1, float var2, float var3, float var4) {
      return new Shapes.Torus(var0, var1, var2, var3, var4);
   }

   public static Shape plane(float var0, float var1, float var2, float var3, float var4, float var5) {
      return new Shapes.Plane(var0, var1, var2, var3, var4, var5);
   }

   public static Shape below(float var0) {
      return new Shapes.Plane(0.0F, var0, 0.0F, 0.0F, 1.0F, 0.0F);
   }

   public static Shape above(float var0) {
      return new Shapes.Plane(0.0F, var0, 0.0F, 0.0F, -1.0F, 0.0F);
   }

   public static Strand strand(float var0, float var1, float... var2) {
      return Strand.tapered(var0, var1, var2);
   }

   public static Strand tube(float... var0) {
      return Strand.of(var0, false);
   }

   public static Shape extrude(Shape2D var0, float var1, float var2) {
      return new Shapes.Extrude(var0, var1, var2);
   }

   public static Shape field(Shapes.Field var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      return new Shapes.Custom(var0, var1, var2, var3, var4, var5, var6);
   }

   public static Shape union(Shape... var0) {
      return (Shape)(var0.length == 1 ? var0[0] : new Shapes.Union(0.0F, var0));
   }

   public static Shape blend(float var0, Shape... var1) {
      return (Shape)(var1.length == 1 ? var1[0] : new Shapes.Union(var0, var1));
   }

   public static Shape subtract(Shape var0, Shape var1) {
      return new Shapes.Subtract(0.0F, var0, var1);
   }

   public static Shape subtract(float var0, Shape var1, Shape var2) {
      return new Shapes.Subtract(var0, var1, var2);
   }

   public static Shape intersect(Shape var0, Shape var1) {
      return new Shapes.Intersect(0.0F, var0, var1);
   }

   public static Shape intersect(float var0, Shape var1, Shape var2) {
      return new Shapes.Intersect(var0, var1, var2);
   }

   static float clamp01(float var0) {
      return var0 < 0.0F ? 0.0F : Math.min(var0, 1.0F);
   }

   static float length(float var0, float var1, float var2) {
      return (float)Math.sqrt(var0 * var0 + var1 * var1 + var2 * var2);
   }

   static float boxLowerBound(Shape var0, float var1, float var2, float var3) {
      float var4 = (var0.minX + var0.maxX) * 0.5F;
      float var5 = (var0.minY + var0.maxY) * 0.5F;
      float var6 = (var0.minZ + var0.maxZ) * 0.5F;
      float var7 = Math.abs(var1 - var4) - (var0.maxX - var0.minX) * 0.5F;
      float var8 = Math.abs(var2 - var5) - (var0.maxY - var0.minY) * 0.5F;
      float var9 = Math.abs(var3 - var6) - (var0.maxZ - var0.minZ) * 0.5F;
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

      Box(float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
         this.cx = var1;
         this.cy = var2;
         this.cz = var3;
         this.r = Math.max(0.0F, Math.min(var7, Math.min(var4, Math.min(var5, var6))));
         this.hx = var4 - this.r;
         this.hy = var5 - this.r;
         this.hz = var6 - this.r;
         this.bounds(var1 - var4, var2 - var5, var3 - var6, var1 + var4, var2 + var5, var3 + var6);
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         float var4 = Math.abs(var1 - this.cx) - this.hx;
         float var5 = Math.abs(var2 - this.cy) - this.hy;
         float var6 = Math.abs(var3 - this.cz) - this.hz;
         float var7 = Math.max(var4, 0.0F);
         float var8 = Math.max(var5, 0.0F);
         float var9 = Math.max(var6, 0.0F);
         return Shapes.length(var7, var8, var9) + Math.min(Math.max(var4, Math.max(var5, var6)), 0.0F) - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Custom extends Shape {
      private final Shapes.Field field;

      Custom(Shapes.Field var1, float var2, float var3, float var4, float var5, float var6, float var7) {
         this.field = var1;
         this.bounds(var2, var3, var4, var5, var6, var7);
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         return this.field.distance(var1, var2, var3);
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

      Cylinder(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
         float var9 = var4 - var1;
         float var10 = var5 - var2;
         float var11 = var6 - var3;
         float var12 = Shapes.length(var9, var10, var11);
         float var13 = Math.max(0.0F, Math.min(var8, Math.min(var7, var12 * 0.5F)));
         float var14 = var12 > 1.0E-6F ? var9 / var12 : 0.0F;
         float var15 = var12 > 1.0E-6F ? var10 / var12 : 1.0F;
         float var16 = var12 > 1.0E-6F ? var11 / var12 : 0.0F;
         this.ax = var1 + var14 * var13;
         this.ay = var2 + var15 * var13;
         this.az = var3 + var16 * var13;
         this.bax = var4 - var14 * var13 - this.ax;
         this.bay = var5 - var15 * var13 - this.ay;
         this.baz = var6 - var16 * var13 - this.az;
         this.baba = Math.max(1.0E-8F, this.bax * this.bax + this.bay * this.bay + this.baz * this.baz);
         this.r = var7 - var13;
         this.round = var13;
         float var17 = var7 * (float)Math.sqrt(Math.max(0.0F, 1.0F - var14 * var14));
         float var18 = var7 * (float)Math.sqrt(Math.max(0.0F, 1.0F - var15 * var15));
         float var19 = var7 * (float)Math.sqrt(Math.max(0.0F, 1.0F - var16 * var16));
         this.bounds(
            Math.min(var1, var4) - var17,
            Math.min(var2, var5) - var18,
            Math.min(var3, var6) - var19,
            Math.max(var1, var4) + var17,
            Math.max(var2, var5) + var18,
            Math.max(var3, var6) + var19
         );
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         float var4 = var1 - this.ax;
         float var5 = var2 - this.ay;
         float var6 = var3 - this.az;
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

      Ellipsoid(float var1, float var2, float var3, float var4, float var5, float var6) {
         this.cx = var1;
         this.cy = var2;
         this.cz = var3;
         this.ix = 1.0F / var4;
         this.iy = 1.0F / var5;
         this.iz = 1.0F / var6;
         this.iix = this.ix * this.ix;
         this.iiy = this.iy * this.iy;
         this.iiz = this.iz * this.iz;
         this.rmin = Math.min(var4, Math.min(var5, var6));
         this.bounds(var1 - var4, var2 - var5, var3 - var6, var1 + var4, var2 + var5, var3 + var6);
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         float var4 = var1 - this.cx;
         float var5 = var2 - this.cy;
         float var6 = var3 - this.cz;
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

      Extrude(Shape2D var1, float var2, float var3) {
         this.profile = var1;
         this.r = Math.max(0.0F, Math.min(var3, var2));
         this.h = var2;
         this.bounds(var1.minX, var1.minY, -var2, var1.maxX, var1.maxY, var2);
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         float var4 = this.profile.distance(var1, var2) + this.r;
         float var5 = Math.abs(var3) - this.h + this.r;
         float var6 = Math.max(var4, 0.0F);
         float var7 = Math.max(var5, 0.0F);
         return Math.min(Math.max(var4, var5), 0.0F) + (float)Math.sqrt(var6 * var6 + var7 * var7) - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Field {
      float distance(float var1, float var2, float var3);
   }

   @Environment(EnvType.CLIENT)
   static final class Intersect extends Shape {
      private final Shape a;
      private final Shape b;
      private final float k;

      Intersect(float var1, Shape var2, Shape var3) {
         this.k = Math.max(0.0F, var1);
         this.a = var2;
         this.b = var3;
         if (var2.bounded() && var3.bounded()) {
            this.bounds(
               Math.max(var2.minX, var3.minX),
               Math.max(var2.minY, var3.minY),
               Math.max(var2.minZ, var3.minZ),
               Math.min(var2.maxX, var3.maxX),
               Math.min(var2.maxY, var3.maxY),
               Math.min(var2.maxZ, var3.maxZ)
            );
         } else if (var2.bounded()) {
            this.boundsOf(var2, 0.0F);
         } else if (var3.bounded()) {
            this.boundsOf(var3, 0.0F);
         }
      }

      @Override
      Shape prune(float var1, float var2, float var3, float var4, float var5, float var6) {
         Shape var7 = this.a.prune(var1, var2, var3, var4, var5, var6);
         Shape var8 = this.b.prune(var1, var2, var3, var4, var5, var6);
         return var7 == this.a && var8 == this.b ? this : new Shapes.Intersect(this.k, var7, var8);
      }

      @Override
      Shape map(UnaryOperator<Shape> var1) {
         Shape var2 = this.a.map(var1);
         Shape var3 = this.b.map(var1);
         return (Shape)var1.apply(var2 == this.a && var3 == this.b ? this : new Shapes.Intersect(this.k, var2, var3).inherit(this));
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         float var4 = this.a.distance(var1, var2, var3);
         float var5 = this.b.distance(var1, var2, var3);
         if (this.k <= 0.0F) {
            return Math.max(var4, var5);
         } else {
            float var6 = Shapes.clamp01(0.5F - 0.5F * (var5 - var4) / this.k);
            return var5 + (var4 - var5) * var6 + this.k * var6 * (1.0F - var6);
         }
      }

      @Override
      float shade(float var1, float var2, float var3, Sample var4) {
         float var5 = this.a.sample(var1, var2, var3, var4);
         if (this.b.paint == null) {
            float var9 = this.b.distance(var1, var2, var3);
            if (this.k <= 0.0F) {
               return Math.max(var5, var9);
            } else {
               float var10 = Shapes.clamp01(0.5F - 0.5F * (var9 - var5) / this.k);
               return var9 + (var5 - var9) * var10 + this.k * var10 * (1.0F - var10);
            }
         } else {
            Sample var6 = new Sample();
            float var7 = this.b.sample(var1, var2, var3, var6);
            if (this.k <= 0.0F) {
               if (var7 > var5) {
                  var4.set(var6);
                  return var7;
               } else {
                  return var5;
               }
            } else {
               float var8 = Shapes.clamp01(0.5F - 0.5F * (var7 - var5) / this.k);
               var4.mix(var6, 1.0F - var8);
               return var7 + (var5 - var7) * var8 + this.k * var8 * (1.0F - var8);
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Mirror extends Shape {
      private final Shape child;
      private final boolean keep;

      Mirror(Shape var1, boolean var2) {
         this.child = var1;
         this.keep = var2;
         if (var1.bounded()) {
            if (var2) {
               this.bounds(Math.min(var1.minX, -var1.maxX), var1.minY, var1.minZ, Math.max(var1.maxX, -var1.minX), var1.maxY, var1.maxZ);
            } else {
               this.bounds(-var1.maxX, var1.minY, var1.minZ, -var1.minX, var1.maxY, var1.maxZ);
            }
         }
      }

      @Override
      Shape prune(float var1, float var2, float var3, float var4, float var5, float var6) {
         Shape var7 = this.keep
            ? this.child.prune(Math.min(var1, -var4), var2, var3, Math.max(var4, -var1), var5, var6)
            : this.child.prune(-var4, var2, var3, -var1, var5, var6);
         return var7 == this.child ? this : new Shapes.Mirror(var7, this.keep);
      }

      @Override
      Shape map(UnaryOperator<Shape> var1) {
         Shape var2 = this.child.map(var1);
         return (Shape)var1.apply(var2 == this.child ? this : new Shapes.Mirror(var2, this.keep).inherit(this));
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         if (!this.keep) {
            return this.child.distance(-var1, var2, var3);
         } else {
            float var4 = this.child.distance(var1, var2, var3);
            return Shapes.boxLowerBound(this.child, -var1, var2, var3) >= var4 ? var4 : Math.min(var4, this.child.distance(-var1, var2, var3));
         }
      }

      @Override
      float shade(float var1, float var2, float var3, Sample var4) {
         if (!this.keep) {
            return this.child.sample(-var1, var2, var3, var4);
         } else {
            float var5 = this.child.distance(var1, var2, var3);
            float var6 = Shapes.boxLowerBound(this.child, -var1, var2, var3) >= var5 ? 3.4028235E38F : this.child.distance(-var1, var2, var3);
            return var6 < var5 ? this.child.sample(-var1, var2, var3, var4) : this.child.sample(var1, var2, var3, var4);
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

      Plane(float var1, float var2, float var3, float var4, float var5, float var6) {
         float var7 = Shapes.length(var4, var5, var6);
         this.px = var1;
         this.py = var2;
         this.pz = var3;
         this.nx = var7 < 1.0E-8F ? 0.0F : var4 / var7;
         this.ny = var7 < 1.0E-8F ? 1.0F : var5 / var7;
         this.nz = var7 < 1.0E-8F ? 0.0F : var6 / var7;
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         return (var1 - this.px) * this.nx + (var2 - this.py) * this.ny + (var3 - this.pz) * this.nz;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Revolve extends Shape {
      private final Shape2D profile;

      Revolve(Shape2D var1) {
         this.profile = var1;
         float var2 = Math.max(Math.abs(var1.minX), Math.abs(var1.maxX));
         this.bounds(-var2, var1.minY, -var2, var2, var1.maxY, var2);
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         return this.profile.distance((float)Math.sqrt(var1 * var1 + var3 * var3), var2);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Rotate extends Shape {
      private final Shape child;
      private final Mat3 inv;
      private final float cx;
      private final float cy;
      private final float cz;

      Rotate(Shape var1, Mat3 var2, float var3, float var4, float var5) {
         this.child = var1;
         this.inv = var2.transpose();
         this.cx = var3;
         this.cy = var4;
         this.cz = var5;
         if (var1.bounded()) {
            float var6 = 100000.0F;
            float var7 = 100000.0F;
            float var8 = 100000.0F;
            float var9 = -100000.0F;
            float var10 = -100000.0F;
            float var11 = -100000.0F;

            for (int var12 = 0; var12 < 8; var12++) {
               float var13 = ((var12 & 1) == 0 ? var1.minX : var1.maxX) - var3;
               float var14 = ((var12 & 2) == 0 ? var1.minY : var1.maxY) - var4;
               float var15 = ((var12 & 4) == 0 ? var1.minZ : var1.maxZ) - var5;
               float var16 = var2.x(var13, var14, var15) + var3;
               float var17 = var2.y(var13, var14, var15) + var4;
               float var18 = var2.z(var13, var14, var15) + var5;
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
      Shape prune(float var1, float var2, float var3, float var4, float var5, float var6) {
         float var7 = 100000.0F;
         float var8 = 100000.0F;
         float var9 = 100000.0F;
         float var10 = -100000.0F;
         float var11 = -100000.0F;
         float var12 = -100000.0F;

         for (int var13 = 0; var13 < 8; var13++) {
            float var14 = ((var13 & 1) == 0 ? var1 : var4) - this.cx;
            float var15 = ((var13 & 2) == 0 ? var2 : var5) - this.cy;
            float var16 = ((var13 & 4) == 0 ? var3 : var6) - this.cz;
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
      Shape map(UnaryOperator<Shape> var1) {
         Shape var2 = this.child.map(var1);
         return (Shape)var1.apply(var2 == this.child ? this : new Shapes.Rotate(var2, this.inv.transpose(), this.cx, this.cy, this.cz).inherit(this));
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         float var4 = var1 - this.cx;
         float var5 = var2 - this.cy;
         float var6 = var3 - this.cz;
         return this.child.distance(this.inv.x(var4, var5, var6) + this.cx, this.inv.y(var4, var5, var6) + this.cy, this.inv.z(var4, var5, var6) + this.cz);
      }

      @Override
      float shade(float var1, float var2, float var3, Sample var4) {
         float var5 = var1 - this.cx;
         float var6 = var2 - this.cy;
         float var7 = var3 - this.cz;
         return this.child.sample(this.inv.x(var5, var6, var7) + this.cx, this.inv.y(var5, var6, var7) + this.cy, this.inv.z(var5, var6, var7) + this.cz, var4);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Roughen extends Shape {
      private final Shape child;
      private final float amp;
      private final float freq;

      Roughen(Shape var1, float var2, float var3) {
         this.child = var1;
         this.amp = var2;
         this.freq = 1.0F / Math.max(0.01F, var3);
         this.boundsOf(var1, Math.abs(var2));
      }

      @Override
      Shape prune(float var1, float var2, float var3, float var4, float var5, float var6) {
         Shape var7 = this.child.prune(var1, var2, var3, var4, var5, var6);
         return var7 == this.child ? this : new Shapes.Roughen(var7, this.amp, 1.0F / this.freq);
      }

      @Override
      Shape map(UnaryOperator<Shape> var1) {
         Shape var2 = this.child.map(var1);
         return (Shape)var1.apply(var2 == this.child ? this : new Shapes.Roughen(var2, this.amp, 1.0F / this.freq).inherit(this));
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         float var4 = this.child.distance(var1, var2, var3);
         return var4 > this.amp * 1.5F + 0.5F ? var4 : var4 + this.amp * Noise.noise(var1 * this.freq, var2 * this.freq, var3 * this.freq);
      }

      @Override
      float shade(float var1, float var2, float var3, Sample var4) {
         return this.child.sample(var1, var2, var3, var4) + this.amp * Noise.noise(var1 * this.freq, var2 * this.freq, var3 * this.freq);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Round extends Shape {
      private final Shape child;
      private final float r;

      Round(Shape var1, float var2) {
         this.child = var1;
         this.r = var2;
         this.boundsOf(var1, Math.max(0.0F, var2));
      }

      @Override
      Shape prune(float var1, float var2, float var3, float var4, float var5, float var6) {
         Shape var7 = this.child.prune(var1, var2, var3, var4, var5, var6);
         return var7 == this.child ? this : new Shapes.Round(var7, this.r);
      }

      @Override
      Shape map(UnaryOperator<Shape> var1) {
         Shape var2 = this.child.map(var1);
         return (Shape)var1.apply(var2 == this.child ? this : new Shapes.Round(var2, this.r).inherit(this));
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         return this.child.distance(var1, var2, var3) - this.r;
      }

      @Override
      float shade(float var1, float var2, float var3, Sample var4) {
         return this.child.sample(var1, var2, var3, var4) - this.r;
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

      RoundCone(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
         this.ax = var1;
         this.ay = var2;
         this.az = var3;
         this.ra = var4;
         this.rb = var8;
         this.bax = var5 - var1;
         this.bay = var6 - var2;
         this.baz = var7 - var3;
         this.l2 = this.bax * this.bax + this.bay * this.bay + this.baz * this.baz;
         this.rr = var4 - var8;
         this.a2 = this.l2 - this.rr * this.rr;
         this.il2 = this.l2 > 1.0E-12F ? 1.0F / this.l2 : 0.0F;
         this.sphere = this.a2 <= 1.0E-6F;
         boolean var9 = var4 >= var8;
         this.sx = var9 ? var1 : var5;
         this.sy = var9 ? var2 : var6;
         this.sz = var9 ? var3 : var7;
         this.sr = Math.max(var4, var8);
         this.bounds(
            Math.min(var1 - var4, var5 - var8),
            Math.min(var2 - var4, var6 - var8),
            Math.min(var3 - var4, var7 - var8),
            Math.max(var1 + var4, var5 + var8),
            Math.max(var2 + var4, var6 + var8),
            Math.max(var3 + var4, var7 + var8)
         );
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         if (this.sphere) {
            return Shapes.length(var1 - this.sx, var2 - this.sy, var3 - this.sz) - this.sr;
         } else {
            float var4 = var1 - this.ax;
            float var5 = var2 - this.ay;
            float var6 = var3 - this.az;
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

      Scale(Shape var1, float var2, float var3, float var4, float var5) {
         this.child = var1;
         this.k = var2;
         this.ik = 1.0F / var2;
         this.cx = var3;
         this.cy = var4;
         this.cz = var5;
         if (var1.bounded()) {
            this.bounds(
               (var1.minX - var3) * var2 + var3,
               (var1.minY - var4) * var2 + var4,
               (var1.minZ - var5) * var2 + var5,
               (var1.maxX - var3) * var2 + var3,
               (var1.maxY - var4) * var2 + var4,
               (var1.maxZ - var5) * var2 + var5
            );
         }
      }

      @Override
      Shape prune(float var1, float var2, float var3, float var4, float var5, float var6) {
         float var7 = (var1 - this.cx) * this.ik + this.cx;
         float var8 = (var4 - this.cx) * this.ik + this.cx;
         float var9 = (var2 - this.cy) * this.ik + this.cy;
         float var10 = (var5 - this.cy) * this.ik + this.cy;
         float var11 = (var3 - this.cz) * this.ik + this.cz;
         float var12 = (var6 - this.cz) * this.ik + this.cz;
         Shape var13 = this.child
            .prune(Math.min(var7, var8), Math.min(var9, var10), Math.min(var11, var12), Math.max(var7, var8), Math.max(var9, var10), Math.max(var11, var12));
         return var13 == this.child ? this : new Shapes.Scale(var13, this.k, this.cx, this.cy, this.cz);
      }

      @Override
      Shape map(UnaryOperator<Shape> var1) {
         Shape var2 = this.child.map(var1);
         return (Shape)var1.apply(var2 == this.child ? this : new Shapes.Scale(var2, this.k, this.cx, this.cy, this.cz).inherit(this));
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         return this.child.distance((var1 - this.cx) * this.ik + this.cx, (var2 - this.cy) * this.ik + this.cy, (var3 - this.cz) * this.ik + this.cz) * this.k;
      }

      @Override
      float shade(float var1, float var2, float var3, Sample var4) {
         return this.child.sample((var1 - this.cx) * this.ik + this.cx, (var2 - this.cy) * this.ik + this.cy, (var3 - this.cz) * this.ik + this.cz, var4)
            * this.k;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Scratch {
      final float[][] pool = new float[64][];
      int depth;

      private Scratch() {
      }

      float[] take(int var1) {
         int var2;
         if ((var2 = this.depth++) >= this.pool.length) {
            return new float[var1];
         } else {
            float[] var3 = this.pool[var2];
            if (var3 == null || var3.length < var1) {
               var3 = new float[Math.max(var1, 16)];
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

      Shell(Shape var1, float var2) {
         this.child = var1;
         this.half = Math.abs(var2) * 0.5F;
         this.boundsOf(var1, this.half);
      }

      @Override
      Shape prune(float var1, float var2, float var3, float var4, float var5, float var6) {
         Shape var7 = this.child.prune(var1, var2, var3, var4, var5, var6);
         return var7 == this.child ? this : new Shapes.Shell(var7, this.half * 2.0F);
      }

      @Override
      Shape map(UnaryOperator<Shape> var1) {
         Shape var2 = this.child.map(var1);
         return (Shape)var1.apply(var2 == this.child ? this : new Shapes.Shell(var2, this.half * 2.0F).inherit(this));
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         return Math.abs(this.child.distance(var1, var2, var3)) - this.half;
      }

      @Override
      float shade(float var1, float var2, float var3, Sample var4) {
         return Math.abs(this.child.sample(var1, var2, var3, var4)) - this.half;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Sphere extends Shape {
      private final float cx;
      private final float cy;
      private final float cz;
      private final float r;

      Sphere(float var1, float var2, float var3, float var4) {
         this.cx = var1;
         this.cy = var2;
         this.cz = var3;
         this.r = var4;
         this.bounds(var1 - var4, var2 - var4, var3 - var4, var1 + var4, var2 + var4, var3 + var4);
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         return Shapes.length(var1 - this.cx, var2 - this.cy, var3 - this.cz) - this.r;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Subtract extends Shape {
      private final Shape base;
      private final Shape cutter;
      private final float k;

      Subtract(float var1, Shape var2, Shape var3) {
         this.k = Math.max(0.0F, var1);
         this.base = var2;
         this.cutter = var3;
         this.boundsOf(var2, this.k * 0.3F);
      }

      @Override
      Shape prune(float var1, float var2, float var3, float var4, float var5, float var6) {
         Shape var7 = this.base.prune(var1, var2, var3, var4, var5, var6);
         Shape var8 = this.cutter.prune(var1, var2, var3, var4, var5, var6);
         return var7 == this.base && var8 == this.cutter ? this : new Shapes.Subtract(this.k, var7, var8);
      }

      @Override
      Shape map(UnaryOperator<Shape> var1) {
         Shape var2 = this.base.map(var1);
         Shape var3 = this.cutter.map(var1);
         return (Shape)var1.apply(var2 == this.base && var3 == this.cutter ? this : new Shapes.Subtract(this.k, var2, var3).inherit(this));
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         float var4 = this.base.distance(var1, var2, var3);
         if (Shapes.boxLowerBound(this.cutter, var1, var2, var3) >= this.k - var4) {
            return var4;
         } else {
            float var5 = this.cutter.distance(var1, var2, var3);
            if (this.k <= 0.0F) {
               return Math.max(var4, -var5);
            } else {
               float var6 = Shapes.clamp01(0.5F - 0.5F * (var4 + var5) / this.k);
               return var4 + (-var5 - var4) * var6 + this.k * var6 * (1.0F - var6);
            }
         }
      }

      @Override
      float shade(float var1, float var2, float var3, Sample var4) {
         float var5 = this.base.sample(var1, var2, var3, var4);
         if (Shapes.boxLowerBound(this.cutter, var1, var2, var3) >= this.k - var5) {
            return var5;
         } else if (this.cutter.paint == null) {
            float var9 = this.cutter.distance(var1, var2, var3);
            if (this.k <= 0.0F) {
               return Math.max(var5, -var9);
            } else {
               float var10 = Shapes.clamp01(0.5F - 0.5F * (var5 + var9) / this.k);
               return var5 + (-var9 - var5) * var10 + this.k * var10 * (1.0F - var10);
            }
         } else {
            Sample var6 = new Sample();
            float var7 = this.cutter.sample(var1, var2, var3, var6);
            if (this.k <= 0.0F) {
               if (-var7 > var5) {
                  var4.set(var6);
                  return -var7;
               } else {
                  return var5;
               }
            } else {
               float var8 = Shapes.clamp01(0.5F - 0.5F * (var5 + var7) / this.k);
               var4.mix(var6, var8);
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

      Torus(float var1, float var2, float var3, float var4, float var5) {
         this.cx = var1;
         this.cy = var2;
         this.cz = var3;
         this.major = var4;
         this.minor = var5;
         float var6 = var4 + var5;
         this.bounds(var1 - var6, var2 - var5, var3 - var6, var1 + var6, var2 + var5, var3 + var6);
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         float var4 = var1 - this.cx;
         float var5 = var3 - this.cz;
         float var6 = (float)Math.sqrt(var4 * var4 + var5 * var5) - this.major;
         float var7 = var2 - this.cy;
         return (float)Math.sqrt(var6 * var6 + var7 * var7) - this.minor;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Translate extends Shape {
      private final Shape child;
      private final float dx;
      private final float dy;
      private final float dz;

      Translate(Shape var1, float var2, float var3, float var4) {
         this.child = var1;
         this.dx = var2;
         this.dy = var3;
         this.dz = var4;
         if (var1.bounded()) {
            this.bounds(var1.minX + var2, var1.minY + var3, var1.minZ + var4, var1.maxX + var2, var1.maxY + var3, var1.maxZ + var4);
         }
      }

      @Override
      Shape prune(float var1, float var2, float var3, float var4, float var5, float var6) {
         Shape var7 = this.child.prune(var1 - this.dx, var2 - this.dy, var3 - this.dz, var4 - this.dx, var5 - this.dy, var6 - this.dz);
         return var7 == this.child ? this : new Shapes.Translate(var7, this.dx, this.dy, this.dz);
      }

      @Override
      Shape map(UnaryOperator<Shape> var1) {
         Shape var2 = this.child.map(var1);
         return (Shape)var1.apply(var2 == this.child ? this : new Shapes.Translate(var2, this.dx, this.dy, this.dz).inherit(this));
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         return this.child.distance(var1 - this.dx, var2 - this.dy, var3 - this.dz);
      }

      @Override
      float shade(float var1, float var2, float var3, Sample var4) {
         return this.child.sample(var1 - this.dx, var2 - this.dy, var3 - this.dz, var4);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Twist extends Shape {
      private final Shape child;
      private final float k;
      private final float cx;
      private final float cz;

      Twist(Shape var1, float var2, float var3, float var4) {
         this.child = var1;
         this.k = (float)Math.toRadians(var2);
         this.cx = var3;
         this.cz = var4;
         if (var1.bounded()) {
            float var5 = 0.0F;

            for (int var6 = 0; var6 < 4; var6++) {
               float var7 = ((var6 & 1) == 0 ? var1.minX : var1.maxX) - var3;
               float var8 = ((var6 & 2) == 0 ? var1.minZ : var1.maxZ) - var4;
               var5 = Math.max(var5, (float)Math.sqrt(var7 * var7 + var8 * var8));
            }

            this.bounds(var3 - var5, var1.minY, var4 - var5, var3 + var5, var1.maxY, var4 + var5);
         }
      }

      @Override
      Shape map(UnaryOperator<Shape> var1) {
         Shape var2 = this.child.map(var1);
         return (Shape)var1.apply(var2 == this.child ? this : new Shapes.Twist(var2, (float)Math.toDegrees(this.k), this.cx, this.cz).inherit(this));
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         float var4 = -this.k * var2;
         float var5 = (float)Math.cos(var4);
         float var6 = (float)Math.sin(var4);
         float var7 = var1 - this.cx;
         float var8 = var3 - this.cz;
         return this.child.distance(var5 * var7 + var6 * var8 + this.cx, var2, -var6 * var7 + var5 * var8 + this.cz);
      }

      @Override
      float shade(float var1, float var2, float var3, Sample var4) {
         float var5 = -this.k * var2;
         float var6 = (float)Math.cos(var5);
         float var7 = (float)Math.sin(var5);
         float var8 = var1 - this.cx;
         float var9 = var3 - this.cz;
         return this.child.sample(var6 * var8 + var7 * var9 + this.cx, var2, -var7 * var8 + var6 * var9 + this.cz, var4);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Union extends Shape {
      private final Shape[] kids;
      private final float k;

      Union(float var1, Shape[] var2) {
         if (var2.length == 0) {
            throw new IllegalArgumentException("union needs at least one shape");
         } else {
            this.k = Math.max(0.0F, var1);
            this.kids = (Shape[])var2.clone();
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
      Shape map(UnaryOperator<Shape> var1) {
         Shape[] var2 = new Shape[this.kids.length];
         boolean var3 = false;

         for (int var4 = 0; var4 < this.kids.length; var4++) {
            var2[var4] = this.kids[var4].map(var1);
            var3 |= var2[var4] != this.kids[var4];
         }

         return (Shape)var1.apply(var3 ? new Shapes.Union(this.k, var2).inherit(this) : this);
      }

      @Override
      Shape prune(float var1, float var2, float var3, float var4, float var5, float var6) {
         float var7 = (var1 + var4) * 0.5F;
         float var8 = (var2 + var5) * 0.5F;
         float var9 = (var3 + var6) * 0.5F;
         float var10 = Shapes.length(var4 - var1, var5 - var2, var6 - var3) * 0.5F * 1.2F;
         float var11 = 3.4028235E38F;

         for (Shape var15 : this.kids) {
            if (!(var15.boxGap(var1, var2, var3, var4, var5, var6) >= var11 + this.k)) {
               var11 = Math.min(var11, var15.distance(var7, var8, var9) + var10);
            }
         }

         Shape[] var20 = new Shape[this.kids.length];
         int var21 = 0;
         boolean var22 = false;

         for (Shape var18 : this.kids) {
            if (var18.boxGap(var1, var2, var3, var4, var5, var6) >= var11 + this.k) {
               var22 = true;
            } else {
               Shape var19 = var18.prune(var1, var2, var3, var4, var5, var6);
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

      private int nearest(float var1, float var2, float var3, float[] var4) {
         int var5 = 0;
         float var6 = 3.4028235E38F;

         for (int var7 = 0; var7 < this.kids.length; var7++) {
            float var8 = Shapes.boxLowerBound(this.kids[var7], var1, var2, var3);
            if (var4 != null) {
               var4[var7] = var8;
            }

            if (var8 < var6) {
               var6 = var8;
               var5 = var7;
            }
         }

         return var5;
      }

      @Override
      public float distance(float var1, float var2, float var3) {
         Shape[] var4 = this.kids;
         if (var4.length <= 4) {
            return this.distance(var1, var2, var3, null);
         } else {
            Shapes.Scratch var5 = Shapes.SCRATCH.get();
            float[] var6 = var5.take(var4.length);

            float var8;
            try {
               float var7 = this.distance(var1, var2, var3, var6);
               var8 = var7;
            } finally {
               var5.depth--;
            }

            return var8;
         }
      }

      @Override
      float distance(float var1, float var2, float var3, float var4) {
         Shape[] var5 = this.kids;
         if (var5.length <= 4) {
            return this.evaluate(var1, var2, var3, null, var4);
         } else {
            Shapes.Scratch var6 = Shapes.SCRATCH.get();
            float[] var7 = var6.take(var5.length);

            float var9;
            try {
               float var8 = this.evaluate(var1, var2, var3, var7, var4);
               var9 = var8;
            } finally {
               var6.depth--;
            }

            return var9;
         }
      }

      private float distance(float var1, float var2, float var3, float[] var4) {
         return this.evaluate(var1, var2, var3, var4, 3.4028235E38F);
      }

      private float evaluate(float var1, float var2, float var3, float[] var4, float var5) {
         Shape[] var7 = this.kids;
         int var8 = this.nearest(var1, var2, var3, var4);
         float var6 = var4 != null ? var4[var8] : Shapes.boxLowerBound(var7[var8], var1, var2, var3);
         if (var6 >= var5) {
            return var6;
         } else {
            float var10 = this.k;
            float var11 = var7[var8].distance(var1, var2, var3, var5 == 3.4028235E38F ? 3.4028235E38F : var5 + var10);

            for (int var12 = 0; var12 < var7.length; var12++) {
               if (var12 != var8) {
                  float var13 = var4 != null ? var4[var12] : Shapes.boxLowerBound(var7[var12], var1, var2, var3);
                  if (!(var13 >= var11 + var10)) {
                     float var15 = var7[var12].distance(var1, var2, var3, var11 + var10);
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
      float shade(float var1, float var2, float var3, Sample var4) {
         Shape[] var5 = this.kids;
         int var6 = this.nearest(var1, var2, var3, null);
         float var7 = var5[var6].sample(var1, var2, var3, var4);
         Sample var8 = null;
         float var9 = this.k;

         for (int var10 = 0; var10 < var5.length; var10++) {
            Shape var11;
            if (var10 != var6 && !(Shapes.boxLowerBound(var11 = var5[var10], var1, var2, var3) >= var7 + var9)) {
               if (var8 == null) {
                  var8 = new Sample();
               }

               float var12 = var11.sample(var1, var2, var3, var8);
               if (var9 > 0.0F) {
                  float var13 = Shapes.clamp01(0.5F + 0.5F * (var12 - var7) / var9);
                  var7 = var12 + (var7 - var12) * var13 - var9 * var13 * (1.0F - var13);
                  var4.mix(var8, 1.0F - var13);
               } else if (var12 < var7) {
                  var7 = var12;
                  var4.set(var8);
               }
            }
         }

         return var7;
      }
   }
}
