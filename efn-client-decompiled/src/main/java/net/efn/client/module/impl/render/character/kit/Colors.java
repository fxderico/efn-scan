package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Colors {
   public static final int WHITE = -1;
   public static final int BLACK = -16777216;

   private Colors() {
   }

   public static int rgb(int i, int j, int k) {
      return 0xFF000000 | clamp(i) << 16 | clamp(j) << 8 | clamp(k);
   }

   public static int argb(int i, int j, int k, int l) {
      return clamp(i) << 24 | clamp(j) << 16 | clamp(k) << 8 | clamp(l);
   }

   public static int hex(String string) {
      String var1 = string.startsWith("#") ? string.substring(1) : string;
      long var2 = Long.parseLong(var1, 16);
      return var1.length() <= 6 ? (int)(4278190080L | var2) : (int)var2;
   }

   public static int hsv(float f, float g, float h) {
      float var6 = (f % 360.0F + 360.0F) % 360.0F / 60.0F;
      float var7 = h * g;
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

      float var9 = h - var7;
      return rgb(Math.round((var5 + var9) * 255.0F), Math.round((var4 + var9) * 255.0F), Math.round((var3 + var9) * 255.0F));
   }

   public static int mix(int i, int j, float f) {
      float var3 = 1.0F - f;
      return argb(
         Math.round(alpha(i) * var3 + alpha(j) * f),
         Math.round(red(i) * var3 + red(j) * f),
         Math.round(green(i) * var3 + green(j) * f),
         Math.round(blue(i) * var3 + blue(j) * f)
      );
   }

   public static int shade(int i, float f) {
      if (f <= 1.0F) {
         return argb(alpha(i), Math.round(red(i) * f), Math.round(green(i) * f), Math.round(blue(i) * f));
      } else {
         float var2 = Math.min(1.0F, f - 1.0F);
         return argb(
            alpha(i), Math.round(red(i) + (255 - red(i)) * var2), Math.round(green(i) + (255 - green(i)) * var2), Math.round(blue(i) + (255 - blue(i)) * var2)
         );
      }
   }

   public static int withAlpha(int i, float f) {
      return clamp(Math.round(f * 255.0F)) << 24 | i & 16777215;
   }

   public static int alpha(int i) {
      return i >>> 24;
   }

   public static int red(int i) {
      return i >> 16 & 0xFF;
   }

   public static int green(int i) {
      return i >> 8 & 0xFF;
   }

   public static int blue(int i) {
      return i & 0xFF;
   }

   static int clamp(int i) {
      return i < 0 ? 0 : Math.min(i, 255);
   }
}
