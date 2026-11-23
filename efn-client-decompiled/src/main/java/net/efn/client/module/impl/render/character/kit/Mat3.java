package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Mat3 {
   public static final Mat3 IDENTITY = new Mat3(1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
   public final float m00;
   public final float m01;
   public final float m02;
   public final float m10;
   public final float m11;
   public final float m12;
   public final float m20;
   public final float m21;
   public final float m22;

   public Mat3(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      this.m00 = var1;
      this.m01 = var2;
      this.m02 = var3;
      this.m10 = var4;
      this.m11 = var5;
      this.m12 = var6;
      this.m20 = var7;
      this.m21 = var8;
      this.m22 = var9;
   }

   public static Mat3 euler(float var0, float var1, float var2) {
      return radians((float)Math.toRadians(var0), (float)Math.toRadians(var1), (float)Math.toRadians(var2));
   }

   public static Mat3 radians(float var0, float var1, float var2) {
      float var3 = (float)Math.cos(var0);
      float var4 = (float)Math.sin(var0);
      float var5 = (float)Math.cos(var1);
      float var6 = (float)Math.sin(var1);
      float var7 = (float)Math.cos(var2);
      float var8 = (float)Math.sin(var2);
      return new Mat3(
         var7 * var5,
         var7 * var6 * var4 - var8 * var3,
         var7 * var6 * var3 + var8 * var4,
         var8 * var5,
         var8 * var6 * var4 + var7 * var3,
         var8 * var6 * var3 - var7 * var4,
         -var6,
         var5 * var4,
         var5 * var3
      );
   }

   public static Mat3 about(float var0, float var1, float var2, float var3) {
      float var4 = (float)Math.sqrt(var0 * var0 + var1 * var1 + var2 * var2);
      if (var4 < 1.0E-8F) {
         return IDENTITY;
      } else {
         float var5 = (float)Math.toRadians(var3);
         float var6 = (float)Math.cos(var5);
         float var7 = (float)Math.sin(var5);
         float var8 = 1.0F - var6;
         float var9;
         float var10;
         float var11;
         return new Mat3(
            var8 * var0 * var0 + var6,
            var8 * var0 * var1 - var7 * (var11 = var2 / var4),
            var8 * var0 * var11 + var7 * (var10 = var1 / var4),
            var8 * (var9 = var0 / var4) * var10 + var7 * var11,
            var8 * var10 * var10 + var6,
            var8 * var10 * var11 - var7 * var9,
            var8 * var9 * var11 - var7 * var10,
            var8 * var10 * var11 + var7 * var9,
            var8 * var11 * var11 + var6
         );
      }
   }

   public Mat3 mul(Mat3 var1) {
      return new Mat3(
         this.m00 * var1.m00 + this.m01 * var1.m10 + this.m02 * var1.m20,
         this.m00 * var1.m01 + this.m01 * var1.m11 + this.m02 * var1.m21,
         this.m00 * var1.m02 + this.m01 * var1.m12 + this.m02 * var1.m22,
         this.m10 * var1.m00 + this.m11 * var1.m10 + this.m12 * var1.m20,
         this.m10 * var1.m01 + this.m11 * var1.m11 + this.m12 * var1.m21,
         this.m10 * var1.m02 + this.m11 * var1.m12 + this.m12 * var1.m22,
         this.m20 * var1.m00 + this.m21 * var1.m10 + this.m22 * var1.m20,
         this.m20 * var1.m01 + this.m21 * var1.m11 + this.m22 * var1.m21,
         this.m20 * var1.m02 + this.m21 * var1.m12 + this.m22 * var1.m22
      );
   }

   public Mat3 transpose() {
      return new Mat3(this.m00, this.m10, this.m20, this.m01, this.m11, this.m21, this.m02, this.m12, this.m22);
   }

   public Vec3 apply(Vec3 var1) {
      return new Vec3(this.x(var1.x(), var1.y(), var1.z()), this.y(var1.x(), var1.y(), var1.z()), this.z(var1.x(), var1.y(), var1.z()));
   }

   public float x(float var1, float var2, float var3) {
      return this.m00 * var1 + this.m01 * var2 + this.m02 * var3;
   }

   public float y(float var1, float var2, float var3) {
      return this.m10 * var1 + this.m11 * var2 + this.m12 * var3;
   }

   public float z(float var1, float var2, float var3) {
      return this.m20 * var1 + this.m21 * var2 + this.m22 * var3;
   }
}
