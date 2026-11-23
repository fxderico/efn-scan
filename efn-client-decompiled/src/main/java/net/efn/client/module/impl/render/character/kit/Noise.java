package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Noise {
   private static final int[] PERM = new int[512];

   private Noise() {
   }

   public static float noise(float var0, float var1, float var2) {
      int var3 = floor(var0);
      int var4 = floor(var1);
      int var5 = floor(var2);
      float var6 = var0 - var3;
      float var7 = var1 - var4;
      float var8 = var2 - var5;
      int var9 = var3 & 0xFF;
      int var10 = var4 & 0xFF;
      int var11 = var5 & 0xFF;
      float var12 = fade(var6);
      float var13 = fade(var7);
      float var14 = fade(var8);
      int var15 = PERM[var9] + var10;
      int var16 = PERM[var15] + var11;
      int var17 = PERM[var15 + 1] + var11;
      int var18 = PERM[var9 + 1] + var10;
      int var19 = PERM[var18] + var11;
      int var20 = PERM[var18 + 1] + var11;
      float var21 = lerp(grad(PERM[var16], var6, var7, var8), grad(PERM[var19], var6 - 1.0F, var7, var8), var12);
      float var22 = lerp(grad(PERM[var17], var6, var7 - 1.0F, var8), grad(PERM[var20], var6 - 1.0F, var7 - 1.0F, var8), var12);
      float var23 = lerp(var21, var22, var13);
      float var24 = lerp(grad(PERM[var16 + 1], var6, var7, var8 - 1.0F), grad(PERM[var19 + 1], var6 - 1.0F, var7, var8 - 1.0F), var12);
      float var25 = lerp(grad(PERM[var17 + 1], var6, var7 - 1.0F, var8 - 1.0F), grad(PERM[var20 + 1], var6 - 1.0F, var7 - 1.0F, var8 - 1.0F), var12);
      return lerp(var23, lerp(var24, var25, var13), var14);
   }

   public static float noise(float var0, float var1) {
      return noise(var0, var1, 0.37F);
   }

   public static float fbm(float var0, float var1, float var2, int var3) {
      float var4 = 0.0F;
      float var5 = 0.5F;
      float var6 = 0.0F;

      for (int var7 = 0; var7 < var3; var7++) {
         var4 += var5 * noise(var0, var1, var2);
         var6 += var5;
         var0 = var0 * 2.03F + 17.1F;
         var1 = var1 * 2.03F - 4.7F;
         var2 = var2 * 2.03F + 9.3F;
         var5 *= 0.5F;
      }

      return var4 / var6;
   }

   public static float fbm(float var0, float var1, int var2) {
      return fbm(var0, var1, 0.37F, var2);
   }

   public static float hash(int var0, int var1, int var2) {
      int var3 = var0 * 522133279 ^ var1 * 1540483477 ^ var2 * 668265263;
      var3 ^= var3 >>> 13;
      var3 *= -2048144789;
      var3 ^= var3 >>> 16;
      return (var3 & 65535) / 65535.0F;
   }

   private static int floor(float var0) {
      int var1 = (int)var0;
      return var0 < var1 ? var1 - 1 : var1;
   }

   private static float fade(float var0) {
      return var0 * var0 * var0 * (var0 * (var0 * 6.0F - 15.0F) + 10.0F);
   }

   private static float lerp(float var0, float var1, float var2) {
      return var0 + (var1 - var0) * var2;
   }

   private static float grad(int var0, float var1, float var2, float var3) {
      int var5 = var0 & 15;
      float var4 = var5 < 8 ? var1 : var2;
      float var7 = var5 < 4 ? var2 : (var5 != 12 && var5 != 14 ? var3 : var1);
      return ((var5 & 1) == 0 ? var4 : -var4) + ((var5 & 2) == 0 ? var7 : -var7);
   }

   static {
      int[] var1 = new int[256];
      int var2 = 0;

      while (var2 < 256) {
         var1[var2] = var2++;
      }

      long var7 = 25214903917L;

      for (int var0 = 255; var0 > 0; var0--) {
         var7 = var7 * 6364136223846793005L + 1442695040888963407L;
         int var4 = (int)((var7 >>> 33) % (var0 + 1));
         int var5 = var1[var0];
         var1[var0] = var1[var4];
         var1[var4] = var5;
      }

      for (int var6 = 0; var6 < 512; var6++) {
         PERM[var6] = var1[var6 & 0xFF];
      }
   }
}
