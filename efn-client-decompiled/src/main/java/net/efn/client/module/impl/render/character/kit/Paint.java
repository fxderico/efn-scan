package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class Paint {
   static final Paint DEFAULT = of(-4934476);

   public Paint() {
   }

   public abstract void apply(float f, float g, float h, Sample sample);

   public static Paint of(int i) {
      return new Paint.Solid(i);
   }

   public static Paint gradient(int i, float f, float g, float h, int j, float k, float l, float m) {
      return new Paint.Linear(i, f, g, h, j, k, l, m);
   }

   public static Paint vertical(int i, float f, int j, float g) {
      return new Paint.Linear(i, 0.0F, f, 0.0F, j, 0.0F, g, 0.0F);
   }

   public static Paint radial(int i, float f, float g, float h, float j, int k) {
      return new Paint.Radial(i, f, g, h, j, k);
   }

   public static Paint wood(int i, int j, float f, float g, float h, float k, float l, float m, float n) {
      return new Paint.Wood(i, j, f, g, h, k, l, m, n);
   }

   public static Paint field(Paint.Field field) {
      return new Paint.Custom(field);
   }

   public Paint noise(int i, float f, float g) {
      return new Paint.Blotch(this, i, f, g);
   }

   public Paint grain(float f, float g) {
      return new Paint.Grain(this, f, g);
   }

   public Paint band(int i, float f, float g, float h, float j, float k, float l, float m, float n) {
      return new Paint.Band(this, i, f, g, h, j, k, l, m, n);
   }

   public Paint glow() {
      return this.glow(1.0F);
   }

   public Paint glow(float f) {
      return new Paint.Glow(this, f);
   }

   public Paint flatLight(float f) {
      return new Paint.Shading(this, Math.max(0.0F, Math.min(1.0F, f)), 0.0F / 0.0F);
   }

   public Paint occlusion(float f) {
      return new Paint.Shading(this, 0.0F / 0.0F, Math.max(0.0F, Math.min(1.0F, f)));
   }

   static float smooth(float f, float g, float h) {
      float var3 = (h - f) / (g - f);
      var3 = var3 < 0.0F ? 0.0F : Math.min(var3, 1.0F);
      return var3 * var3 * (3.0F - 2.0F * var3);
   }

   @Environment(EnvType.CLIENT)
   private static final class Band extends Paint {
      private final Paint base;
      private final int argb;
      private final float px;
      private final float py;
      private final float pz;
      private final float nx;
      private final float ny;
      private final float nz;
      private final float half;
      private final float soft;

      Band(Paint paint, int i, float f, float g, float h, float j, float k, float l, float m, float n) {
         this.base = paint;
         this.argb = i;
         this.px = f;
         this.py = g;
         this.pz = h;
         float var11 = (float)Math.sqrt(j * j + k * k + l * l);
         this.nx = var11 < 1.0E-6F ? 0.0F : j / var11;
         this.ny = var11 < 1.0E-6F ? 1.0F : k / var11;
         this.nz = var11 < 1.0E-6F ? 0.0F : l / var11;
         this.half = m;
         this.soft = Math.max(0.001F, n);
      }

      @Override
      public void apply(float f, float g, float h, Sample sample) {
         this.base.apply(f, g, h, sample);
         float var5 = Math.abs((f - this.px) * this.nx + (g - this.py) * this.ny + (h - this.pz) * this.nz);
         float var6 = 1.0F - smooth(this.half, this.half + this.soft, var5);
         if (var6 > 0.0F) {
            float var7 = sample.glow;
            sample.toward(this.argb, var6 * ((this.argb >>> 24) / 255.0F));
            sample.glow = var7;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Blotch extends Paint {
      private final Paint base;
      private final int other;
      private final float freq;
      private final float amount;

      Blotch(Paint paint, int i, float f, float g) {
         this.base = paint;
         this.other = i;
         this.freq = 1.0F / Math.max(0.01F, f);
         this.amount = g;
      }

      @Override
      public void apply(float f, float g, float h, Sample sample) {
         this.base.apply(f, g, h, sample);
         float var5 = Noise.fbm(f * this.freq, g * this.freq, h * this.freq, 3) * 1.6F + 0.5F;
         float var6 = (var5 < 0.0F ? 0.0F : Math.min(var5, 1.0F)) * this.amount;
         float var7 = sample.glow;
         sample.toward(this.other, var6);
         sample.glow = var7;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Custom extends Paint {
      private final Paint.Field field;

      Custom(Paint.Field field) {
         this.field = field;
      }

      @Override
      public void apply(float f, float g, float h, Sample sample) {
         sample.set(this.field.argb(f, g, h));
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Field {
      int argb(float f, float g, float h);
   }

   @Environment(EnvType.CLIENT)
   private static final class Glow extends Paint {
      private final Paint base;
      private final float amount;

      Glow(Paint paint, float f) {
         this.base = paint;
         this.amount = f;
      }

      @Override
      public void apply(float f, float g, float h, Sample sample) {
         this.base.apply(f, g, h, sample);
         sample.glow = this.amount;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Grain extends Paint {
      private final Paint base;
      private final float freq;
      private final float amount;

      Grain(Paint paint, float f, float g) {
         this.base = paint;
         this.freq = 1.0F / Math.max(0.01F, f);
         this.amount = g;
      }

      @Override
      public void apply(float f, float g, float h, Sample sample) {
         this.base.apply(f, g, h, sample);
         float var5 = 1.0F + Noise.fbm(f * this.freq, g * this.freq, h * this.freq, 2) * 2.0F * this.amount;
         sample.r *= var5;
         sample.g *= var5;
         sample.b *= var5;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Linear extends Paint {
      private final int from;
      private final int to;
      private final float ax;
      private final float ay;
      private final float az;
      private final float dx;
      private final float dy;
      private final float dz;
      private final float inv;

      Linear(int i, float f, float g, float h, int j, float k, float l, float m) {
         this.from = i;
         this.to = j;
         this.ax = f;
         this.ay = g;
         this.az = h;
         this.dx = k - f;
         this.dy = l - g;
         this.dz = m - h;
         float var9 = this.dx * this.dx + this.dy * this.dy + this.dz * this.dz;
         this.inv = var9 < 1.0E-8F ? 0.0F : 1.0F / var9;
      }

      @Override
      public void apply(float f, float g, float h, Sample sample) {
         float var5 = ((f - this.ax) * this.dx + (g - this.ay) * this.dy + (h - this.az) * this.dz) * this.inv;
         var5 = var5 < 0.0F ? 0.0F : Math.min(var5, 1.0F);
         sample.set(this.from);
         sample.toward(this.to, var5);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Radial extends Paint {
      private final int inner;
      private final int outer;
      private final float cx;
      private final float cy;
      private final float cz;
      private final float radius;

      Radial(int i, float f, float g, float h, float j, int k) {
         this.inner = i;
         this.outer = k;
         this.cx = f;
         this.cy = g;
         this.cz = h;
         this.radius = Math.max(1.0E-4F, j);
      }

      @Override
      public void apply(float f, float g, float h, Sample sample) {
         float var5 = f - this.cx;
         float var6 = g - this.cy;
         float var7 = h - this.cz;
         float var8 = (float)Math.sqrt(var5 * var5 + var6 * var6 + var7 * var7) / this.radius;
         sample.set(this.inner);
         sample.toward(this.outer, var8 < 0.0F ? 0.0F : Math.min(var8, 1.0F));
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Shading extends Paint {
      private final Paint base;
      private final float flat;
      private final float occlusion;

      Shading(Paint paint, float f, float g) {
         this.base = paint;
         this.flat = f;
         this.occlusion = g;
      }

      @Override
      public void apply(float f, float g, float h, Sample sample) {
         this.base.apply(f, g, h, sample);
         if (this.flat == this.flat) {
            sample.flat = this.flat;
            sample.flatWeight = 1.0F;
         }

         if (this.occlusion == this.occlusion) {
            sample.occlusion = this.occlusion;
            sample.occlusionWeight = 1.0F;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Solid extends Paint {
      private final int argb;

      Solid(int i) {
         this.argb = i;
      }

      @Override
      public void apply(float f, float g, float h, Sample sample) {
         sample.set(this.argb);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Wood extends Paint {
      private final int light;
      private final int dark;
      private final float ax;
      private final float ay;
      private final float az;
      private final float dx;
      private final float dy;
      private final float dz;
      private final float spacing;

      Wood(int i, int j, float f, float g, float h, float k, float l, float m, float n) {
         this.light = i;
         this.dark = j;
         this.ax = f;
         this.ay = g;
         this.az = h;
         float var10 = (float)Math.sqrt(k * k + l * l + m * m);
         this.dx = var10 < 1.0E-6F ? 0.0F : k / var10;
         this.dy = var10 < 1.0E-6F ? 1.0F : l / var10;
         this.dz = var10 < 1.0E-6F ? 0.0F : m / var10;
         this.spacing = Math.max(0.05F, n);
      }

      @Override
      public void apply(float f, float g, float h, Sample sample) {
         float var5 = f - this.ax;
         float var6 = g - this.ay;
         float var7 = h - this.az;
         float var8 = var5 * this.dx + var6 * this.dy + var7 * this.dz;
         float var9 = var5 - this.dx * var8;
         float var10 = var6 - this.dy * var8;
         float var11 = var7 - this.dz * var8;
         float var12 = (float)Math.sqrt(var9 * var9 + var10 * var10 + var11 * var11);
         float var13 = 1.0F / this.spacing;
         float var14 = Noise.fbm(var9 * 0.3F + var8 * 0.021F, var10 * 0.3F + var8 * 0.017F, var11 * 0.3F + var8 * 0.013F, 2);
         float var15 = Noise.fbm(var9 * var13 * 1.6F + var14, var10 * var13 * 1.6F + var8 * 0.045F, var11 * var13 * 1.6F - var14, 3);
         float var16 = var12 * var13 + var14 * 2.2F;
         float var17 = var16 - (float)Math.floor(var16);
         float var18 = smooth(0.7F, 0.85F, var17) * (1.0F - smooth(0.88F, 1.0F, var17));
         float var19 = 0.18F + var15 * 0.9F + var18 * 0.22F;
         sample.set(this.light);
         sample.toward(this.dark, var19 < 0.0F ? 0.0F : Math.min(var19, 1.0F));
      }
   }
}
