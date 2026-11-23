package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ThemeTint {
   private static final int PINK = 16732080;
   private static final int DUSK = 2754594;
   private static final int WARM_ROSE = 16751309;
   private static final float SURFACE_SHARE = 0.45F;
   private static final float WORLD_SHARE = 0.3F;
   private static volatile float pink;

   private ThemeTint() {
   }

   public static void pink(float var0) {
      pink = Math.max(0.0F, Math.min(1.0F, var0));
   }

   public static float pink() {
      return pink;
   }

   public static int accent(int var0) {
      return mix(var0, 16732080, pink);
   }

   public static int surface(int var0) {
      return mix(var0, 2754594, pink * 0.45F);
   }

   public static float world() {
      float var0 = pink;
      return var0 * var0 * (3.0F - 2.0F * var0) * 0.3F;
   }

   public static int world(int var0) {
      float var1 = world();
      if (var1 <= 0.0F) {
         return var0;
      } else {
         float var2 = warmth(var0);
         int var3 = var2 > 0.0F ? mix(16732080, 16751309, var2) & 16777215 : 16732080;
         return mix(var0, var3, var1 * (1.0F - 0.4F * var2));
      }
   }

   private static float warmth(int var0) {
      int var2 = var0 >> 16 & 0xFF;
      int var3 = var0 >> 8 & 0xFF;
      int var4 = var0 & 0xFF;
      int var5 = Math.max(var2, Math.max(var3, var4));
      int var6 = Math.min(var2, Math.min(var3, var4));
      if (var5 != 0 && !(var5 - var6 < 0.25F * var5) && var5 != var4) {
         float var1 = var5 == var2 ? 60.0F * (var3 - var4) / (var5 - var6) : 60.0F * ((float)(var4 - var2) / (var5 - var6) + 2.0F);
         if (var1 < 0.0F) {
            var1 += 360.0F;
         }

         return Math.min(ramp(var1, 10.0F, 20.0F), 1.0F - ramp(var1, 60.0F, 70.0F));
      } else {
         return 0.0F;
      }
   }

   private static float ramp(float var0, float var1, float var2) {
      return Math.max(0.0F, Math.min(1.0F, (var0 - var1) / (var2 - var1)));
   }

   private static int mix(int var0, int var1, float var2) {
      if (var2 <= 0.0F) {
         return var0;
      } else {
         int var3 = lerp(var0 >> 16 & 0xFF, var1 >> 16 & 0xFF, var2);
         int var4 = lerp(var0 >> 8 & 0xFF, var1 >> 8 & 0xFF, var2);
         int var5 = lerp(var0 & 0xFF, var1 & 0xFF, var2);
         return var0 & 0xFF000000 | var3 << 16 | var4 << 8 | var5;
      }
   }

   private static int lerp(int var0, int var1, float var2) {
      return Math.round(var0 + (var1 - var0) * var2);
   }
}
