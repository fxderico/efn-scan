package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface Paint2D {
   int argb(float f, float g);

   static Paint2D solid(int i) {
      return (f, g) -> i;
   }

   static Paint2D linear(float f, float g, float h, float i, int... is) {
      if (is.length == 0) {
         throw new IllegalArgumentException("gradient needs at least one colour");
      } else {
         float var5 = h - f;
         float var6 = i - g;
         float var7 = var5 * var5 + var6 * var6;
         float var8 = var7 < 1.0E-8F ? 0.0F : 1.0F / var7;
         return (k, l) -> stop(is, ((k - f) * var5 + (l - g) * var6) * var8);
      }
   }

   static Paint2D radial(float f, float g, float h, int... is) {
      if (is.length == 0) {
         throw new IllegalArgumentException("gradient needs at least one colour");
      } else {
         float var4 = 1.0F / Math.max(1.0E-4F, h);
         return (i, j) -> {
            float var6 = i - f;
            float var7 = j - g;
            return stop(is, (float)Math.sqrt(var6 * var6 + var7 * var7) * var4);
         };
      }
   }

   static Paint2D noise(int i, int j, float f, float g) {
      float var4 = 1.0F / Math.max(0.001F, f);
      return (h, k) -> {
         float var6 = Noise.fbm(h * var4, k * var4, 4) * g + 0.5F;
         return Colors.mix(i, j, var6 < 0.0F ? 0.0F : Math.min(var6, 1.0F));
      };
   }

   static Paint2D grain(int i, int j, float f, float g, float h) {
      double var5 = Math.toRadians(f);
      float var7 = (float)Math.cos(var5);
      float var8 = (float)Math.sin(var5);
      float var9 = 1.0F / Math.max(0.1F, g);
      return (l, m) -> {
         float var9x = -l * var8 + m * var7;
         float var10 = l * var7 + m * var8;
         float var11 = Noise.fbm(var10 * 0.02F, var9x * 0.05F, 3) * h;
         float var12 = (var9x + var11) * var9;
         float var13 = var12 - (float)Math.floor(var12);
         float var14 = Paint.smooth(0.6F, 0.8F, var13) * (1.0F - Paint.smooth(0.85F, 1.0F, var13));
         float var15 = var14 * 0.8F + (Noise.noise(var10 * 0.015F, var9x * var9 * 1.7F) * 0.5F + 0.5F) * 0.3F;
         return Colors.mix(i, j, var15 < 0.0F ? 0.0F : Math.min(var15, 1.0F));
      };
   }

   private static int stop(int[] is, float f) {
      if (is.length == 1) {
         return is[0];
      } else {
         f = f < 0.0F ? 0.0F : Math.min(f, 1.0F);
         float var2 = f * (is.length - 1);
         int var3 = Math.min((int)var2, is.length - 2);
         return Colors.mix(is[var3], is[var3 + 1], var2 - var3);
      }
   }
}
