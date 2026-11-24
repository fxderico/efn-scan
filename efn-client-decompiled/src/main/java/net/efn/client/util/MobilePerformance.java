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

   public static int segments(int i, int j) {
      return reduce(i, j);
   }

   public static int particles(int i, int j) {
      return reduce(i, j);
   }

   public static int trailSamples(int i, int j) {
      return reduce(i, j);
   }

   public static int uiSegments(int i, float f) {
      if (active() && i > 12 && Float.isFinite(f) && !(f <= 0.0F)) {
         double var2 = Math.acos(Math.max(-1.0, 1.0 - 0.35 / f));
         if (var2 <= 0.0) {
            return i;
         } else {
            int var4 = (int)Math.ceil(3.141592653589793 / var2);
            return var4 >= i ? i : Math.min(i, Math.max(12, (var4 + 3) / 4 * 4));
         }
      } else {
         return i;
      }
   }

   private static int reduce(int i, int j) {
      return active() && i > 0 ? Math.min(i, Math.max(j, i / 2 + i % 2)) : i;
   }
}
