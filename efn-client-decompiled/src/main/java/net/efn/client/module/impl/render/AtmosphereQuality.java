package net.efn.client.module.impl.render;

import imgui.EfnAndroidNative;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class AtmosphereQuality {
   private static final boolean MOBILE = EfnAndroidNative.isAndroidRuntime();
   private static final int MOBILE_SKY_SIZE = 640;

   private AtmosphereQuality() {
   }

   static boolean mobile() {
      return MOBILE;
   }

   static int skyWidth(int i, int j) {
      return scaledSize(i, Math.max(i, j));
   }

   static int skyHeight(int i, int j) {
      return scaledSize(j, Math.max(i, j));
   }

   private static int scaledSize(int i, int j) {
      double var2 = Math.min(0.5, 640.0 / Math.max(1, j));
      return Math.max(1, (int)Math.round(i * var2));
   }
}
