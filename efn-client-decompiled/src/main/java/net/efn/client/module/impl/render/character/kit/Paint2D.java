package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface Paint2D {
   int argb(float var1, float var2);

   static Paint2D solid(int var0) {
      return (var1, var2) -> var0;
   }

   static Paint2D linear(float var0, float var1, float var2, float var3, int... var4) {
      if (var4.length == 0) {
         throw new IllegalArgumentException("gradient needs at least one colour");
      } else {
         float var5 = var2 - var0;
         float var6 = var3 - var1;
         float var7 = var5 * var5 + var6 * var6;
         float var8 = var7 < 1.0E-8F ? 0.0F : 1.0F / var7;
         return (var6x, var7x) -> stop(var4, ((var6x - var0) * var5 + (var7x - var1) * var6) * var8);
      }
   }

   static Paint2D radial(float var0, float var1, float var2, int... var3) {
      if (var3.length == 0) {
         throw new IllegalArgumentException("gradient needs at least one colour");
      } else {
         float var4 = 1.0F / Math.max(1.0E-4F, var2);
         return (var4x, var5) -> {
            float var6 = var4x - var0;
            float var7 = var5 - var1;
            return stop(var3, (float)Math.sqrt(var6 * var6 + var7 * var7) * var4);
         };
      }
   }

   static Paint2D noise(int var0, int var1, float var2, float var3) {
      float var4 = 1.0F / Math.max(0.001F, var2);
      return (var4x, var5) -> {
         float var6 = Noise.fbm(var4x * var4, var5 * var4, 4) * var3 + 0.5F;
         return Colors.mix(var0, var1, var6 < 0.0F ? 0.0F : Math.min(var6, 1.0F));
      };
   }

   static Paint2D grain(int var0, int var1, float var2, float var3, float var4) {
      double var5 = Math.toRadians(var2);
      float var7 = (float)Math.cos(var5);
      float var8 = (float)Math.sin(var5);
      float var9 = 1.0F / Math.max(0.1F, var3);
      return (var6, var7x) -> {
         float var9x = -var6 * var8 + var7x * var7;
         float var10 = var6 * var7 + var7x * var8;
         float var11 = Noise.fbm(var10 * 0.02F, var9x * 0.05F, 3) * var4;
         float var12 = (var9x + var11) * var9;
         float var13 = var12 - (float)Math.floor(var12);
         float var14 = Paint.smooth(0.6F, 0.8F, var13) * (1.0F - Paint.smooth(0.85F, 1.0F, var13));
         float var15 = var14 * 0.8F + (Noise.noise(var10 * 0.015F, var9x * var9 * 1.7F) * 0.5F + 0.5F) * 0.3F;
         return Colors.mix(var0, var1, var15 < 0.0F ? 0.0F : Math.min(var15, 1.0F));
      };
   }

   private static int stop(int[] var0, float var1) {
      if (var0.length == 1) {
         return var0[0];
      } else {
         var1 = var1 < 0.0F ? 0.0F : Math.min(var1, 1.0F);
         float var2 = var1 * (var0.length - 1);
         int var3 = Math.min((int)var2, var0.length - 2);
         return Colors.mix(var0[var3], var0[var3 + 1], var2 - var3);
      }
   }
}
