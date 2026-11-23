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

   public static void pink(float f) {
      pink = Math.max(0.0F, Math.min(1.0F, f));
   }

   public static float pink() {
      return pink;
   }

   public static int accent(int i) {
      return mix(i, 16732080, pink);
   }

   public static int surface(int i) {
      return mix(i, 2754594, pink * 0.45F);
   }

   public static float world() {
      float var0 = pink;
      return var0 * var0 * (3.0F - 2.0F * var0) * 0.3F;
   }

   public static int world(int i) {
      float var1 = world();
      if (var1 <= 0.0F) {
         return i;
      } else {
         float var2 = warmth(i);
         int var3 = var2 > 0.0F ? mix(16732080, 16751309, var2) & 16777215 : 16732080;
         return mix(i, var3, var1 * (1.0F - 0.4F * var2));
      }
   }

   private static float warmth(int i) {
      int var2 = i >> 16 & 0xFF;
      int var3 = i >> 8 & 0xFF;
      int var4 = i & 0xFF;
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

   private static float ramp(float f, float g, float h) {
      return Math.max(0.0F, Math.min(1.0F, (f - g) / (h - g)));
   }

   private static int mix(int i, int j, float f) {
      if (f <= 0.0F) {
         return i;
      } else {
         int var3 = lerp(i >> 16 & 0xFF, j >> 16 & 0xFF, f);
         int var4 = lerp(i >> 8 & 0xFF, j >> 8 & 0xFF, f);
         int var5 = lerp(i & 0xFF, j & 0xFF, f);
         return i & 0xFF000000 | var3 << 16 | var4 << 8 | var5;
      }
   }

   private static int lerp(int i, int j, float f) {
      return Math.round(i + (j - i) * f);
   }
}
