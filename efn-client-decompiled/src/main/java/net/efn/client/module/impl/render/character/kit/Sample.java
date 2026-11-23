package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Sample {
   public float r;
   public float g;
   public float b;
   public float glow;
   public float flat;
   public float flatWeight;
   public float occlusion;
   public float occlusionWeight;

   public Sample() {
   }

   public void set(int var1) {
      this.r = (var1 >> 16 & 0xFF) / 255.0F;
      this.g = (var1 >> 8 & 0xFF) / 255.0F;
      this.b = (var1 & 0xFF) / 255.0F;
      this.glow = 0.0F;
      this.flat = 0.0F;
      this.flatWeight = 0.0F;
      this.occlusion = 0.0F;
      this.occlusionWeight = 0.0F;
   }

   public void set(Sample var1) {
      this.r = var1.r;
      this.g = var1.g;
      this.b = var1.b;
      this.glow = var1.glow;
      this.flat = var1.flat;
      this.flatWeight = var1.flatWeight;
      this.occlusion = var1.occlusion;
      this.occlusionWeight = var1.occlusionWeight;
   }

   public void mix(Sample var1, float var2) {
      float var3 = 1.0F - var2;
      this.r = this.r * var3 + var1.r * var2;
      this.g = this.g * var3 + var1.g * var2;
      this.b = this.b * var3 + var1.b * var2;
      this.glow = this.glow * var3 + var1.glow * var2;
      this.flat = this.flat * var3 + var1.flat * var2;
      this.flatWeight = this.flatWeight * var3 + var1.flatWeight * var2;
      this.occlusion = this.occlusion * var3 + var1.occlusion * var2;
      this.occlusionWeight = this.occlusionWeight * var3 + var1.occlusionWeight * var2;
   }

   public void toward(int var1, float var2) {
      float var3 = 1.0F - var2;
      this.r = this.r * var3 + (var1 >> 16 & 0xFF) / 255.0F * var2;
      this.g = this.g * var3 + (var1 >> 8 & 0xFF) / 255.0F * var2;
      this.b = this.b * var3 + (var1 & 0xFF) / 255.0F * var2;
   }

   public int argb() {
      return 0xFF000000 | channel(this.r) << 16 | channel(this.g) << 8 | channel(this.b);
   }

   float flat(float var1) {
      return blend(var1, this.flat, this.flatWeight);
   }

   float occlusion(float var1) {
      return blend(var1, this.occlusion, this.occlusionWeight);
   }

   private static float blend(float var0, float var1, float var2) {
      if (var2 <= 1.0E-4F) {
         return var0;
      } else {
         float var3 = Math.min(1.0F, var2);
         return Math.max(0.0F, Math.min(1.0F, var0 * (1.0F - var3) + var1 * (var3 / var2)));
      }
   }

   static int channel(float var0) {
      int var1 = Math.round(var0 * 255.0F);
      return var1 < 0 ? 0 : Math.min(var1, 255);
   }
}
