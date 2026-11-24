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

   Shuffle(Vec3d vec3d, Vec3d vec3d2, boolean bl) {
      this.spot = vec3d;
      this.look = vec3d2;
      this.slow = bl ? 0.0144 : 0.0025;
   }

   boolean quick() {
      return this.slow == 0.0144;
   }

   Vec3d spot() {
      return this.spot;
   }

   Shuffle.Result tick(MinecraftClient minecraftClient) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 == null) {
         return Shuffle.Result.FAILED;
      } else {
         float[] var3 = PlacementUtil.calcRotation(var2.getEyePos(), this.look);
         Aim.turn(minecraftClient, var3[0], var3[1], 22.0F);
         double var4 = this.spot.x - var2.getX();
         double var6 = this.spot.z - var2.getZ();
         Vec3d var8 = var2.getVelocity();
         double var9 = Math.hypot(var4, var6);
         if (var9 < 0.3 && var8.horizontalLengthSquared() < this.slow) {
            this.release(minecraftClient);
            return Shuffle.Result.DONE;
         } else if (++this.ticks > 60) {
            this.release(minecraftClient);
            return var9 < 0.45 ? Shuffle.Result.DONE : Shuffle.Result.FAILED;
         } else {
            double var11 = Math.toRadians(var2.getYaw());
            double var13 = Math.sin(var11);
            double var15 = Math.cos(var11);
            double var17 = -var13 * var4 + var15 * var6 - 1.2 * (-var13 * var8.x + var15 * var8.z);
            double var19 = var15 * var4 + var13 * var6 - 1.2 * (var15 * var8.x + var13 * var8.z);
            minecraftClient.options.forwardKey.setPressed(var17 > 0.12);
            minecraftClient.options.backKey.setPressed(var17 < -0.12);
            minecraftClient.options.leftKey.setPressed(var19 > 0.12);
            minecraftClient.options.rightKey.setPressed(var19 < -0.12);
            return Shuffle.Result.RUNNING;
         }
      }
   }

   void release(MinecraftClient minecraftClient) {
      minecraftClient.options.forwardKey.setPressed(false);
      minecraftClient.options.backKey.setPressed(false);
      minecraftClient.options.leftKey.setPressed(false);
      minecraftClient.options.rightKey.setPressed(false);
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
