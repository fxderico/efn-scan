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

   private Decal(
      Canvas var1,
      boolean var2,
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
      if (var1 == null) {
         throw new IllegalArgumentException("decal needs a canvas");
      } else if (!(var12 <= 0.0F) && !(var13 <= 0.0F)) {
         this.canvas = var1;
         this.wrap = var2;
         this.cx = var3;
         this.cy = var4;
         this.cz = var5;
         float var14 = (float)Math.sqrt(var6 * var6 + var7 * var7 + var8 * var8);
         if (var14 < 1.0E-6F) {
            throw new IllegalArgumentException("decal direction must not be zero");
         } else {
            this.dx = var6 / var14;
            this.dy = var7 / var14;
            this.dz = var8 / var14;
            float var15 = var9 * this.dx + var10 * this.dy + var11 * this.dz;
            float var16 = var9 - var15 * this.dx;
            float var17 = var10 - var15 * this.dy;
            float var18 = var11 - var15 * this.dz;
            float var19 = (float)Math.sqrt(var16 * var16 + var17 * var17 + var18 * var18);
            if (var19 < 1.0E-6F) {
               throw new IllegalArgumentException("decal up/front direction must not be parallel to its axis");
            } else {
               this.ux = var16 / var19;
               this.uy = var17 / var19;
               this.uz = var18 / var19;
               if (var2) {
                  this.rx = this.dy * this.uz - this.dz * this.uy;
                  this.ry = this.dz * this.ux - this.dx * this.uz;
                  this.rz = this.dx * this.uy - this.dy * this.ux;
               } else {
                  this.rx = this.uy * this.dz - this.uz * this.dy;
                  this.ry = this.uz * this.dx - this.ux * this.dz;
                  this.rz = this.ux * this.dy - this.uy * this.dx;
               }

               this.width = var12;
               this.height = var13;
            }
         }
      } else {
         throw new IllegalArgumentException("decal size must be positive");
      }
   }

   public static Decal front(Canvas var0, float var1, float var2, float var3, float var4, float var5) {
      return new Decal(var0, false, var1, var2, var3, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, var4, var5);
   }

   public static Decal back(Canvas var0, float var1, float var2, float var3, float var4, float var5) {
      return new Decal(var0, false, var1, var2, var3, 0.0F, 0.0F, -1.0F, 0.0F, 1.0F, 0.0F, var4, var5);
   }

   public static Decal left(Canvas var0, float var1, float var2, float var3, float var4, float var5) {
      return new Decal(var0, false, var1, var2, var3, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, var4, var5);
   }

   public static Decal right(Canvas var0, float var1, float var2, float var3, float var4, float var5) {
      return new Decal(var0, false, var1, var2, var3, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, var4, var5);
   }

   public static Decal top(Canvas var0, float var1, float var2, float var3, float var4, float var5) {
      return new Decal(var0, false, var1, var2, var3, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, var4, var5);
   }

   public static Decal project(
      Canvas var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11
   ) {
      return new Decal(var0, false, var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11);
   }

   public static Decal wrap(
      Canvas var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11
   ) {
      return new Decal(var0, true, var1, var2, var3, var4, var5, var6, var7, var8, var9, Math.min(360.0F, var10), var11);
   }

   public Decal depth(float var1, float var2) {
      this.near = -Math.abs(var1);
      this.far = Math.abs(var2);
      return this;
   }

   public Decal radius(float var1, float var2) {
      this.near = var1;
      this.far = var2;
      return this;
   }

   public Decal maxAngle(float var1) {
      this.cosLimit = (float)Math.cos(Math.toRadians(Math.max(1.0F, Math.min(89.5F, var1))));
      return this;
   }

   public Decal offset(float var1) {
      this.offset = var1;
      return this;
   }

   public Decal glow() {
      this.glow = true;
      return this;
   }

   public Decal flatLight(float var1) {
      this.flat = Math.max(0.0F, Math.min(1.0F, var1));
      return this;
   }

   public Decal occlusion(float var1) {
      this.occlusion = Math.max(0.0F, Math.min(1.0F, var1));
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
