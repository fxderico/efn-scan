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

   public void set(int i) {
      this.r = (i >> 16 & 0xFF) / 255.0F;
      this.g = (i >> 8 & 0xFF) / 255.0F;
      this.b = (i & 0xFF) / 255.0F;
      this.glow = 0.0F;
      this.flat = 0.0F;
      this.flatWeight = 0.0F;
      this.occlusion = 0.0F;
      this.occlusionWeight = 0.0F;
   }

   public void set(Sample sample) {
      this.r = sample.r;
      this.g = sample.g;
      this.b = sample.b;
      this.glow = sample.glow;
      this.flat = sample.flat;
      this.flatWeight = sample.flatWeight;
      this.occlusion = sample.occlusion;
      this.occlusionWeight = sample.occlusionWeight;
   }

   public void mix(Sample sample, float f) {
      float var3 = 1.0F - f;
      this.r = this.r * var3 + sample.r * f;
      this.g = this.g * var3 + sample.g * f;
      this.b = this.b * var3 + sample.b * f;
      this.glow = this.glow * var3 + sample.glow * f;
      this.flat = this.flat * var3 + sample.flat * f;
      this.flatWeight = this.flatWeight * var3 + sample.flatWeight * f;
      this.occlusion = this.occlusion * var3 + sample.occlusion * f;
      this.occlusionWeight = this.occlusionWeight * var3 + sample.occlusionWeight * f;
   }

   public void toward(int i, float f) {
      float var3 = 1.0F - f;
      this.r = this.r * var3 + (i >> 16 & 0xFF) / 255.0F * f;
      this.g = this.g * var3 + (i >> 8 & 0xFF) / 255.0F * f;
      this.b = this.b * var3 + (i & 0xFF) / 255.0F * f;
   }

   public int argb() {
      return 0xFF000000 | channel(this.r) << 16 | channel(this.g) << 8 | channel(this.b);
   }

   float flat(float f) {
      return blend(f, this.flat, this.flatWeight);
   }

   float occlusion(float f) {
      return blend(f, this.occlusion, this.occlusionWeight);
   }

   private static float blend(float f, float g, float h) {
      if (h <= 1.0E-4F) {
         return f;
      } else {
         float var3 = Math.min(1.0F, h);
         return Math.max(0.0F, Math.min(1.0F, f * (1.0F - var3) + g * (var3 / h)));
      }
   }

   static int channel(float f) {
      int var1 = Math.round(f * 255.0F);
      return var1 < 0 ? 0 : Math.min(var1, 255);
   }
}
