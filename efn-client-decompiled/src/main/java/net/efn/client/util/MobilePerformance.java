package net.efn.client.util;

import imgui.EfnAndroidNative;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MobilePerformance {
   private static final boolean ANDROID = EfnAndroidNative.isAndroidRuntime();

   private MobilePerformance() {
   }

   public static boolean isAndroid() {
      return ANDROID;
   }

   public static boolean active() {
      return ANDROID;
   }

   public static int segments(int var0, int var1) {
      return reduce(var0, var1);
   }

   public static int particles(int var0, int var1) {
      return reduce(var0, var1);
   }

   public static int trailSamples(int var0, int var1) {
      return reduce(var0, var1);
   }

   public static int uiSegments(int var0, float var1) {
      if (active() && var0 > 12 && Float.isFinite(var1) && !(var1 <= 0.0F)) {
         double var2 = Math.acos(Math.max(-1.0, 1.0 - 0.35 / var1));
         if (var2 <= 0.0) {
            return var0;
         } else {
            int var4 = (int)Math.ceil(3.141592653589793 / var2);
            return var4 >= var0 ? var0 : Math.min(var0, Math.max(12, (var4 + 3) / 4 * 4));
         }
      } else {
         return var0;
      }
   }

   private static int reduce(int var0, int var1) {
      return active() && var0 > 0 ? Math.min(var0, Math.max(var1, var0 / 2 + var0 % 2)) : var0;
   }
}
