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

   public Mat3(float f, float g, float h, float i, float j, float k, float l, float m, float n) {
      this.m00 = f;
      this.m01 = g;
      this.m02 = h;
      this.m10 = i;
      this.m11 = j;
      this.m12 = k;
      this.m20 = l;
      this.m21 = m;
      this.m22 = n;
   }

   public static Mat3 euler(float f, float g, float h) {
      return radians((float)Math.toRadians(f), (float)Math.toRadians(g), (float)Math.toRadians(h));
   }

   public static Mat3 radians(float f, float g, float h) {
      float var3 = (float)Math.cos(f);
      float var4 = (float)Math.sin(f);
      float var5 = (float)Math.cos(g);
      float var6 = (float)Math.sin(g);
      float var7 = (float)Math.cos(h);
      float var8 = (float)Math.sin(h);
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

   public static Mat3 about(float f, float g, float h, float i) {
      float var4 = (float)Math.sqrt(f * f + g * g + h * h);
      if (var4 < 1.0E-8F) {
         return IDENTITY;
      } else {
         float var5 = (float)Math.toRadians(i);
         float var6 = (float)Math.cos(var5);
         float var7 = (float)Math.sin(var5);
         float var8 = 1.0F - var6;
         float var9;
         float var10;
         float var11;
         return new Mat3(
            var8 * f * f + var6,
            var8 * f * g - var7 * (var11 = h / var4),
            var8 * f * var11 + var7 * (var10 = g / var4),
            var8 * (var9 = f / var4) * var10 + var7 * var11,
            var8 * var10 * var10 + var6,
            var8 * var10 * var11 - var7 * var9,
            var8 * var9 * var11 - var7 * var10,
            var8 * var10 * var11 + var7 * var9,
            var8 * var11 * var11 + var6
         );
      }
   }

   public Mat3 mul(Mat3 mat3) {
      return new Mat3(
         this.m00 * mat3.m00 + this.m01 * mat3.m10 + this.m02 * mat3.m20,
         this.m00 * mat3.m01 + this.m01 * mat3.m11 + this.m02 * mat3.m21,
         this.m00 * mat3.m02 + this.m01 * mat3.m12 + this.m02 * mat3.m22,
         this.m10 * mat3.m00 + this.m11 * mat3.m10 + this.m12 * mat3.m20,
         this.m10 * mat3.m01 + this.m11 * mat3.m11 + this.m12 * mat3.m21,
         this.m10 * mat3.m02 + this.m11 * mat3.m12 + this.m12 * mat3.m22,
         this.m20 * mat3.m00 + this.m21 * mat3.m10 + this.m22 * mat3.m20,
         this.m20 * mat3.m01 + this.m21 * mat3.m11 + this.m22 * mat3.m21,
         this.m20 * mat3.m02 + this.m21 * mat3.m12 + this.m22 * mat3.m22
      );
   }

   public Mat3 transpose() {
      return new Mat3(this.m00, this.m10, this.m20, this.m01, this.m11, this.m21, this.m02, this.m12, this.m22);
   }

   public Vec3 apply(Vec3 vec3) {
      return new Vec3(this.x(vec3.x(), vec3.y(), vec3.z()), this.y(vec3.x(), vec3.y(), vec3.z()), this.z(vec3.x(), vec3.y(), vec3.z()));
   }

   public float x(float f, float g, float h) {
      return this.m00 * f + this.m01 * g + this.m02 * h;
   }

   public float y(float f, float g, float h) {
      return this.m10 * f + this.m11 * g + this.m12 * h;
   }

   public float z(float f, float g, float h) {
      return this.m20 * f + this.m21 * g + this.m22 * h;
   }
}
