package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Decal {
   final Canvas canvas;
   final boolean wrap;
   final float cx;
   final float cy;
   final float cz;
   final float dx;
   final float dy;
   final float dz;
   final float ux;
   final float uy;
   final float uz;
   final float rx;
   final float ry;
   final float rz;
   final float width;
   final float height;
   float near = -3.4028235E38F;
   float far = 3.4028235E38F;
   float cosLimit = (float)Math.cos(Math.toRadians(78.0));
   float offset = 0.035F;
   boolean glow;
   float flat = 0.0F / 0.0F;
   float occlusion = 1.0F;

   private Decal(Canvas canvas, boolean bl, float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, float p) {
      if (canvas == null) {
         throw new IllegalArgumentException("decal needs a canvas");
      } else if (!(o <= 0.0F) && !(p <= 0.0F)) {
         this.canvas = canvas;
         this.wrap = bl;
         this.cx = f;
         this.cy = g;
         this.cz = h;
         float var14 = (float)Math.sqrt(i * i + j * j + k * k);
         if (var14 < 1.0E-6F) {
            throw new IllegalArgumentException("decal direction must not be zero");
         } else {
            this.dx = i / var14;
            this.dy = j / var14;
            this.dz = k / var14;
            float var15 = l * this.dx + m * this.dy + n * this.dz;
            float var16 = l - var15 * this.dx;
            float var17 = m - var15 * this.dy;
            float var18 = n - var15 * this.dz;
            float var19 = (float)Math.sqrt(var16 * var16 + var17 * var17 + var18 * var18);
            if (var19 < 1.0E-6F) {
               throw new IllegalArgumentException("decal up/front direction must not be parallel to its axis");
            } else {
               this.ux = var16 / var19;
               this.uy = var17 / var19;
               this.uz = var18 / var19;
               if (bl) {
                  this.rx = this.dy * this.uz - this.dz * this.uy;
                  this.ry = this.dz * this.ux - this.dx * this.uz;
                  this.rz = this.dx * this.uy - this.dy * this.ux;
               } else {
                  this.rx = this.uy * this.dz - this.uz * this.dy;
                  this.ry = this.uz * this.dx - this.ux * this.dz;
                  this.rz = this.ux * this.dy - this.uy * this.dx;
               }

               this.width = o;
               this.height = p;
            }
         }
      } else {
         throw new IllegalArgumentException("decal size must be positive");
      }
   }

   public static Decal front(Canvas canvas, float f, float g, float h, float i, float j) {
      return new Decal(canvas, false, f, g, h, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, i, j);
   }

   public static Decal back(Canvas canvas, float f, float g, float h, float i, float j) {
      return new Decal(canvas, false, f, g, h, 0.0F, 0.0F, -1.0F, 0.0F, 1.0F, 0.0F, i, j);
   }

   public static Decal left(Canvas canvas, float f, float g, float h, float i, float j) {
      return new Decal(canvas, false, f, g, h, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, i, j);
   }

   public static Decal right(Canvas canvas, float f, float g, float h, float i, float j) {
      return new Decal(canvas, false, f, g, h, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, i, j);
   }

   public static Decal top(Canvas canvas, float f, float g, float h, float i, float j) {
      return new Decal(canvas, false, f, g, h, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, i, j);
   }

   public static Decal project(Canvas canvas, float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, float p) {
      return new Decal(canvas, false, f, g, h, i, j, k, l, m, n, o, p);
   }

   public static Decal wrap(Canvas canvas, float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, float p) {
      return new Decal(canvas, true, f, g, h, i, j, k, l, m, n, Math.min(360.0F, o), p);
   }

   public Decal depth(float f, float g) {
      this.near = -Math.abs(f);
      this.far = Math.abs(g);
      return this;
   }

   public Decal radius(float f, float g) {
      this.near = f;
      this.far = g;
      return this;
   }

   public Decal maxAngle(float f) {
      this.cosLimit = (float)Math.cos(Math.toRadians(Math.max(1.0F, Math.min(89.5F, f))));
      return this;
   }

   public Decal offset(float f) {
      this.offset = f;
      return this;
   }

   public Decal glow() {
      this.glow = true;
      return this;
   }

   public Decal flatLight(float f) {
      this.flat = Math.max(0.0F, Math.min(1.0F, f));
      return this;
   }

   public Decal occlusion(float f) {
      this.occlusion = Math.max(0.0F, Math.min(1.0F, f));
      return this;
   }

   Decal mirrored() {
      Decal var1 = this.wrap
         ? new Decal(this.canvas.mirrored(), true, -this.cx, this.cy, this.cz, -this.dx, this.dy, this.dz, -this.ux, this.uy, this.uz, this.width, this.height)
         : new Decal(this.canvas.mirrored(), false, -this.cx, this.cy, this.cz, -this.dx, this.dy, this.dz, -this.ux, this.uy, this.uz, this.width, this.height);
      var1.near = this.near;
      var1.far = this.far;
      var1.cosLimit = this.cosLimit;
      var1.offset = this.offset;
      var1.glow = this.glow;
      var1.flat = this.flat;
      var1.occlusion = this.occlusion;
      return var1;
   }
}
