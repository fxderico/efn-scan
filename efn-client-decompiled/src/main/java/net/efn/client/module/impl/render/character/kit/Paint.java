package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class Paint {
   static final Paint DEFAULT = of(-4934476);

   public Paint() {
   }

   public abstract void apply(float var1, float var2, float var3, Sample var4);

   public static Paint of(int var0) {
      return new Paint.Solid(var0);
   }

   public static Paint gradient(int var0, float var1, float var2, float var3, int var4, float var5, float var6, float var7) {
      return new Paint.Linear(var0, var1, var2, var3, var4, var5, var6, var7);
   }

   public static Paint vertical(int var0, float var1, int var2, float var3) {
      return new Paint.Linear(var0, 0.0F, var1, 0.0F, var2, 0.0F, var3, 0.0F);
   }

   public static Paint radial(int var0, float var1, float var2, float var3, float var4, int var5) {
      return new Paint.Radial(var0, var1, var2, var3, var4, var5);
   }

   public static Paint wood(int var0, int var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
      return new Paint.Wood(var0, var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public static Paint field(Paint.Field var0) {
      return new Paint.Custom(var0);
   }

   public Paint noise(int var1, float var2, float var3) {
      return new Paint.Blotch(this, var1, var2, var3);
   }

   public Paint grain(float var1, float var2) {
      return new Paint.Grain(this, var1, var2);
   }

   public Paint band(int var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
      return new Paint.Band(this, var1, var2, var3, var4, var5, var6, var7, var8, var9);
   }

   public Paint glow() {
      return this.glow(1.0F);
   }

   public Paint glow(float var1) {
      return new Paint.Glow(this, var1);
   }

   public Paint flatLight(float var1) {
      return new Paint.Shading(this, Math.max(0.0F, Math.min(1.0F, var1)), 0.0F / 0.0F);
   }

   public Paint occlusion(float var1) {
      return new Paint.Shading(this, 0.0F / 0.0F, Math.max(0.0F, Math.min(1.0F, var1)));
   }

   static float smooth(float var0, float var1, float var2) {
      float var3 = (var2 - var0) / (var1 - var0);
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

      Band(Paint var1, int var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10) {
         this.base = var1;
         this.argb = var2;
         this.px = var3;
         this.py = var4;
         this.pz = var5;
         float var11 = (float)Math.sqrt(var6 * var6 + var7 * var7 + var8 * var8);
         this.nx = var11 < 1.0E-6F ? 0.0F : var6 / var11;
         this.ny = var11 < 1.0E-6F ? 1.0F : var7 / var11;
         this.nz = var11 < 1.0E-6F ? 0.0F : var8 / var11;
         this.half = var9;
         this.soft = Math.max(0.001F, var10);
      }

      @Override
      public void apply(float var1, float var2, float var3, Sample var4) {
         this.base.apply(var1, var2, var3, var4);
         float var5 = Math.abs((var1 - this.px) * this.nx + (var2 - this.py) * this.ny + (var3 - this.pz) * this.nz);
         float var6 = 1.0F - smooth(this.half, this.half + this.soft, var5);
         if (var6 > 0.0F) {
            float var7 = var4.glow;
            var4.toward(this.argb, var6 * ((this.argb >>> 24) / 255.0F));
            var4.glow = var7;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Blotch extends Paint {
      private final Paint base;
      private final int other;
      private final float freq;
      private final float amount;

      Blotch(Paint var1, int var2, float var3, float var4) {
         this.base = var1;
         this.other = var2;
         this.freq = 1.0F / Math.max(0.01F, var3);
         this.amount = var4;
      }

      @Override
      public void apply(float var1, float var2, float var3, Sample var4) {
         this.base.apply(var1, var2, var3, var4);
         float var5 = Noise.fbm(var1 * this.freq, var2 * this.freq, var3 * this.freq, 3) * 1.6F + 0.5F;
         float var6 = (var5 < 0.0F ? 0.0F : Math.min(var5, 1.0F)) * this.amount;
         float var7 = var4.glow;
         var4.toward(this.other, var6);
         var4.glow = var7;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Custom extends Paint {
      private final Paint.Field field;

      Custom(Paint.Field var1) {
         this.field = var1;
      }

      @Override
      public void apply(float var1, float var2, float var3, Sample var4) {
         var4.set(this.field.argb(var1, var2, var3));
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Field {
      int argb(float var1, float var2, float var3);
   }

   @Environment(EnvType.CLIENT)
   private static final class Glow extends Paint {
      private final Paint base;
      private final float amount;

      Glow(Paint var1, float var2) {
         this.base = var1;
         this.amount = var2;
      }

      @Override
      public void apply(float var1, float var2, float var3, Sample var4) {
         this.base.apply(var1, var2, var3, var4);
         var4.glow = this.amount;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Grain extends Paint {
      private final Paint base;
      private final float freq;
      private final float amount;

      Grain(Paint var1, float var2, float var3) {
         this.base = var1;
         this.freq = 1.0F / Math.max(0.01F, var2);
         this.amount = var3;
      }

      @Override
      public void apply(float var1, float var2, float var3, Sample var4) {
         this.base.apply(var1, var2, var3, var4);
         float var5 = 1.0F + Noise.fbm(var1 * this.freq, var2 * this.freq, var3 * this.freq, 2) * 2.0F * this.amount;
         var4.r *= var5;
         var4.g *= var5;
         var4.b *= var5;
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

      Linear(int var1, float var2, float var3, float var4, int var5, float var6, float var7, float var8) {
         this.from = var1;
         this.to = var5;
         this.ax = var2;
         this.ay = var3;
         this.az = var4;
         this.dx = var6 - var2;
         this.dy = var7 - var3;
         this.dz = var8 - var4;
         float var9 = this.dx * this.dx + this.dy * this.dy + this.dz * this.dz;
         this.inv = var9 < 1.0E-8F ? 0.0F : 1.0F / var9;
      }

      @Override
      public void apply(float var1, float var2, float var3, Sample var4) {
         float var5 = ((var1 - this.ax) * this.dx + (var2 - this.ay) * this.dy + (var3 - this.az) * this.dz) * this.inv;
         var5 = var5 < 0.0F ? 0.0F : Math.min(var5, 1.0F);
         var4.set(this.from);
         var4.toward(this.to, var5);
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

      Radial(int var1, float var2, float var3, float var4, float var5, int var6) {
         this.inner = var1;
         this.outer = var6;
         this.cx = var2;
         this.cy = var3;
         this.cz = var4;
         this.radius = Math.max(1.0E-4F, var5);
      }

      @Override
      public void apply(float var1, float var2, float var3, Sample var4) {
         float var5 = var1 - this.cx;
         float var6 = var2 - this.cy;
         float var7 = var3 - this.cz;
         float var8 = (float)Math.sqrt(var5 * var5 + var6 * var6 + var7 * var7) / this.radius;
         var4.set(this.inner);
         var4.toward(this.outer, var8 < 0.0F ? 0.0F : Math.min(var8, 1.0F));
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Shading extends Paint {
      private final Paint base;
      private final float flat;
      private final float occlusion;

      Shading(Paint var1, float var2, float var3) {
         this.base = var1;
         this.flat = var2;
         this.occlusion = var3;
      }

      @Override
      public void apply(float var1, float var2, float var3, Sample var4) {
         this.base.apply(var1, var2, var3, var4);
         if (this.flat == this.flat) {
            var4.flat = this.flat;
            var4.flatWeight = 1.0F;
         }

         if (this.occlusion == this.occlusion) {
            var4.occlusion = this.occlusion;
            var4.occlusionWeight = 1.0F;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Solid extends Paint {
      private final int argb;

      Solid(int var1) {
         this.argb = var1;
      }

      @Override
      public void apply(float var1, float var2, float var3, Sample var4) {
         var4.set(this.argb);
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

      Wood(int var1, int var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9) {
         this.light = var1;
         this.dark = var2;
         this.ax = var3;
         this.ay = var4;
         this.az = var5;
         float var10 = (float)Math.sqrt(var6 * var6 + var7 * var7 + var8 * var8);
         this.dx = var10 < 1.0E-6F ? 0.0F : var6 / var10;
         this.dy = var10 < 1.0E-6F ? 1.0F : var7 / var10;
         this.dz = var10 < 1.0E-6F ? 0.0F : var8 / var10;
         this.spacing = Math.max(0.05F, var9);
      }

      @Override
      public void apply(float var1, float var2, float var3, Sample var4) {
         float var5 = var1 - this.ax;
         float var6 = var2 - this.ay;
         float var7 = var3 - this.az;
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
         var4.set(this.light);
         var4.toward(this.dark, var19 < 0.0F ? 0.0F : Math.min(var19, 1.0F));
      }
   }
}
