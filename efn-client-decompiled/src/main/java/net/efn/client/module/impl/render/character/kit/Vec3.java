package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record Vec3(float x, float y, float z) {
   public static final Vec3 ZERO = new Vec3(0.0F, 0.0F, 0.0F);
   public static final Vec3 UP = new Vec3(0.0F, 1.0F, 0.0F);
   public static final Vec3 FORWARD = new Vec3(0.0F, 0.0F, 1.0F);
   public static final Vec3 LEFT = new Vec3(1.0F, 0.0F, 0.0F);

   public static Vec3 of(float f, float g, float h) {
      return new Vec3(f, g, h);
   }

   public Vec3 add(Vec3 vec3) {
      return new Vec3(this.x + vec3.x, this.y + vec3.y, this.z + vec3.z);
   }

   public Vec3 add(float f, float g, float h) {
      return new Vec3(this.x + f, this.y + g, this.z + h);
   }

   public Vec3 sub(Vec3 vec3) {
      return new Vec3(this.x - vec3.x, this.y - vec3.y, this.z - vec3.z);
   }

   public Vec3 mul(float f) {
      return new Vec3(this.x * f, this.y * f, this.z * f);
   }

   public float dot(Vec3 vec3) {
      return this.x * vec3.x + this.y * vec3.y + this.z * vec3.z;
   }

   public Vec3 cross(Vec3 vec3) {
      return new Vec3(this.y * vec3.z - this.z * vec3.y, this.z * vec3.x - this.x * vec3.z, this.x * vec3.y - this.y * vec3.x);
   }

   public float length() {
      return (float)Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
   }

   public float distance(Vec3 vec3) {
      return this.sub(vec3).length();
   }

   public Vec3 normalize() {
      float var1 = this.length();
      return var1 < 1.0E-8F ? ZERO : this.mul(1.0F / var1);
   }

   public Vec3 lerp(Vec3 vec3, float f) {
      return new Vec3(this.x + (vec3.x - this.x) * f, this.y + (vec3.y - this.y) * f, this.z + (vec3.z - this.z) * f);
   }

   public Vec3 mirrorX() {
      return new Vec3(-this.x, this.y, this.z);
   }

   public Vec3 rotate(float f, float g, float h) {
      return Mat3.euler(f, g, h).apply(this);
   }

   public Vec3 rotate(float f, float g, float h, Vec3 vec3) {
      return Mat3.euler(f, g, h).apply(this.sub(vec3)).add(vec3);
   }
}
