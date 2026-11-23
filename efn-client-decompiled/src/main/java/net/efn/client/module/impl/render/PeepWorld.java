package net.efn.client.module.impl.render;

import net.efn.client.gui.ThemeTint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class PeepWorld {
   private static final float PINK_R = 1.0F;
   private static final float PINK_G = 0.30980393F;
   private static final float PINK_B = 0.6901961F;
   private static final float PINK_LUMA = luma(1.0F, 0.30980393F, 0.6901961F);
   private static final long HORIZON_STALE_NANOS = 250000000L;
   private static volatile int horizon;
   private static volatile float horizonShare;
   private static volatile long horizonAt;

   private PeepWorld() {
   }

   static void horizon(int var0, float var1) {
      horizon = var0;
      horizonShare = var1;
      horizonAt = System.nanoTime();
   }

   public static void fog(Vector4f var0) {
      float var1 = horizonShare;
      if (var1 > 0.0F && System.nanoTime() - horizonAt > 250000000L) {
         var1 = 0.0F;
      }

      fog(var0, horizon, var1);
   }

   private static void fog(Vector4f var0, int var1, float var2) {
      float var3 = ThemeTint.world();
      if (var3 > 0.0F) {
         float var4 = luma(var0.x, var0.y, var0.z) / PINK_LUMA;
         var0.x = var0.x + (Math.min(1.0F, 1.0F * var4) - var0.x) * var3;
         var0.y = var0.y + (Math.min(1.0F, 0.30980393F * var4) - var0.y) * var3;
         var0.z = var0.z + (Math.min(1.0F, 0.6901961F * var4) - var0.z) * var3;
      }

      if (var2 > 0.0F) {
         var0.x = var0.x + ((var1 >> 16 & 0xFF) / 255.0F - var0.x) * var2;
         var0.y = var0.y + ((var1 >> 8 & 0xFF) / 255.0F - var0.y) * var2;
         var0.z = var0.z + ((var1 & 0xFF) / 255.0F - var0.z) * var2;
      }
   }

   static int fog(int var0) {
      return fog(var0, 0, 0.0F);
   }

   static int fog(int var0, int var1, float var2) {
      Vector4f var3 = new Vector4f((var0 >> 16 & 0xFF) / 255.0F, (var0 >> 8 & 0xFF) / 255.0F, (var0 & 0xFF) / 255.0F, 1.0F);
      fog(var3, var1, var2);
      return channel(var3.x) << 16 | channel(var3.y) << 8 | channel(var3.z);
   }

   private static float luma(float var0, float var1, float var2) {
      return 0.2126F * var0 + 0.7152F * var1 + 0.0722F * var2;
   }

   private static int channel(float var0) {
      return Math.max(0, Math.min(255, Math.round(var0 * 255.0F)));
   }
}
