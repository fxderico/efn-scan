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

   static int skyWidth(int var0, int var1) {
      return scaledSize(var0, Math.max(var0, var1));
   }

   static int skyHeight(int var0, int var1) {
      return scaledSize(var1, Math.max(var0, var1));
   }

   private static int scaledSize(int var0, int var1) {
      double var2 = Math.min(0.5, 640.0 / Math.max(1, var1));
      return Math.max(1, (int)Math.round(var0 * var2));
   }
}
