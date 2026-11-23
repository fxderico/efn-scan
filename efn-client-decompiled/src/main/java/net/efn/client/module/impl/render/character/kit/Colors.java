package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Colors {
   public static final int WHITE = -1;
   public static final int BLACK = -16777216;

   private Colors() {
   }

   public static int rgb(int var0, int var1, int var2) {
      return 0xFF000000 | clamp(var0) << 16 | clamp(var1) << 8 | clamp(var2);
   }

   public static int argb(int var0, int var1, int var2, int var3) {
      return clamp(var0) << 24 | clamp(var1) << 16 | clamp(var2) << 8 | clamp(var3);
   }

   public static int hex(String var0) {
      String var1 = var0.startsWith("#") ? var0.substring(1) : var0;
      long var2 = Long.parseLong(var1, 16);
      return var1.length() <= 6 ? (int)(4278190080L | var2) : (int)var2;
   }

   public static int hsv(float var0, float var1, float var2) {
      float var6 = (var0 % 360.0F + 360.0F) % 360.0F / 60.0F;
      float var7 = var2 * var1;
      float var8 = var7 * (1.0F - Math.abs(var6 % 2.0F - 1.0F));
      float var3;
      float var4;
      float var5;
      if (var6 < 1.0F) {
         var5 = var7;
         var4 = var8;
         var3 = 0.0F;
      } else if (var6 < 2.0F) {
         var5 = var8;
         var4 = var7;
         var3 = 0.0F;
      } else if (var6 < 3.0F) {
         var5 = 0.0F;
         var4 = var7;
         var3 = var8;
      } else if (var6 < 4.0F) {
         var5 = 0.0F;
         var4 = var8;
         var3 = var7;
      } else if (var6 < 5.0F) {
         var5 = var8;
         var4 = 0.0F;
         var3 = var7;
      } else {
         var5 = var7;
         var4 = 0.0F;
         var3 = var8;
      }

      float var9 = var2 - var7;
      return rgb(Math.round((var5 + var9) * 255.0F), Math.round((var4 + var9) * 255.0F), Math.round((var3 + var9) * 255.0F));
   }

   public static int mix(int var0, int var1, float var2) {
      float var3 = 1.0F - var2;
      return argb(
         Math.round(alpha(var0) * var3 + alpha(var1) * var2),
         Math.round(red(var0) * var3 + red(var1) * var2),
         Math.round(green(var0) * var3 + green(var1) * var2),
         Math.round(blue(var0) * var3 + blue(var1) * var2)
      );
   }

   public static int shade(int var0, float var1) {
      if (var1 <= 1.0F) {
         return argb(alpha(var0), Math.round(red(var0) * var1), Math.round(green(var0) * var1), Math.round(blue(var0) * var1));
      } else {
         float var2 = Math.min(1.0F, var1 - 1.0F);
         return argb(
            alpha(var0),
            Math.round(red(var0) + (255 - red(var0)) * var2),
            Math.round(green(var0) + (255 - green(var0)) * var2),
            Math.round(blue(var0) + (255 - blue(var0)) * var2)
         );
      }
   }

   public static int withAlpha(int var0, float var1) {
      return clamp(Math.round(var1 * 255.0F)) << 24 | var0 & 16777215;
   }

   public static int alpha(int var0) {
      return var0 >>> 24;
   }

   public static int red(int var0) {
      return var0 >> 16 & 0xFF;
   }

   public static int green(int var0) {
      return var0 >> 8 & 0xFF;
   }

   public static int blue(int var0) {
      return var0 & 0xFF;
   }

   static int clamp(int var0) {
      return var0 < 0 ? 0 : Math.min(var0, 255);
   }
}
