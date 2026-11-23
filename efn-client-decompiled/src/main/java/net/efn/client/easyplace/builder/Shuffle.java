package net.efn.client.easyplace.builder;

import net.efn.client.easyplace.util.PlacementUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
final class Shuffle {
   private static final double CLOSE = 0.3;
   private static final double SLOW = 0.0025;
   private static final double KEY = 0.12;
   private static final double SLIDE = 1.2;
   private static final int TIME_LIMIT = 60;
   private static final double QUICK_SLOW = 0.0144;
   private final Vec3d spot;
   private final Vec3d look;
   private final double slow;
   private int ticks;

   Shuffle(Vec3d var1, Vec3d var2, boolean var3) {
      this.spot = var1;
      this.look = var2;
      this.slow = var3 ? 0.0144 : 0.0025;
   }

   boolean quick() {
      return this.slow == 0.0144;
   }

   Vec3d spot() {
      return this.spot;
   }

   Shuffle.Result tick(MinecraftClient var1) {
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 == null) {
         return Shuffle.Result.FAILED;
      } else {
         float[] var3 = PlacementUtil.calcRotation(var2.method_33571(), this.look);
         Aim.turn(var1, var3[0], var3[1], 22.0F);
         double var4 = this.spot.field_1352 - var2.method_23317();
         double var6 = this.spot.field_1350 - var2.method_23321();
         Vec3d var8 = var2.method_18798();
         double var9 = Math.hypot(var4, var6);
         if (var9 < 0.3 && var8.method_37268() < this.slow) {
            this.release(var1);
            return Shuffle.Result.DONE;
         } else if (++this.ticks > 60) {
            this.release(var1);
            return var9 < 0.45 ? Shuffle.Result.DONE : Shuffle.Result.FAILED;
         } else {
            double var11 = Math.toRadians(var2.method_36454());
            double var13 = Math.sin(var11);
            double var15 = Math.cos(var11);
            double var17 = -var13 * var4 + var15 * var6 - 1.2 * (-var13 * var8.field_1352 + var15 * var8.field_1350);
            double var19 = var15 * var4 + var13 * var6 - 1.2 * (var15 * var8.field_1352 + var13 * var8.field_1350);
            var1.field_1690.field_1894.method_23481(var17 > 0.12);
            var1.field_1690.field_1881.method_23481(var17 < -0.12);
            var1.field_1690.field_1913.method_23481(var19 > 0.12);
            var1.field_1690.field_1849.method_23481(var19 < -0.12);
            return Shuffle.Result.RUNNING;
         }
      }
   }

   void release(MinecraftClient var1) {
      var1.field_1690.field_1894.method_23481(false);
      var1.field_1690.field_1881.method_23481(false);
      var1.field_1690.field_1913.method_23481(false);
      var1.field_1690.field_1849.method_23481(false);
   }

   @Environment(EnvType.CLIENT)
   static enum Result {
      RUNNING,
      DONE,
      FAILED;

      private Result() {
      }
   }
}
