package net.efn.client.module.impl.movement.speed;

import net.efn.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class StrafeSpeedMode extends SpeedMode {
   private long timer = 0L;

   public StrafeSpeedMode(SpeedModule speedModule) {
      super(speedModule);
   }

   @Override
   public Vec3d onMove(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      boolean var3 = SpeedModule.isMoving(clientPlayerEntity);
      double var4 = vec3d.y;
      switch (this.stage) {
         case 0:
            if (var3) {
               this.stage++;
               this.speed = 1.1799999475479126 * this.getDefaultSpeed(clientPlayerEntity) - 0.01;
            }
         case 1:
            if (var3 && clientPlayerEntity.isOnGround()) {
               var4 = this.getHop(clientPlayerEntity, 0.40123128);
               this.speed = this.speed * this.settings.strafeSpeed();
               this.stage++;
            }
            break;
         case 2:
            this.speed = this.distance - 0.76 * (this.distance - this.getDefaultSpeed(clientPlayerEntity));
            this.stage++;
            break;
         case 3:
            if (!clientPlayerEntity.getEntityWorld().isSpaceEmpty(clientPlayerEntity.getBoundingBox().offset(0.0, clientPlayerEntity.getVelocity().y, 0.0))
               || clientPlayerEntity.verticalCollision && this.stage > 0) {
               this.stage = 0;
            }

            this.speed = this.distance - this.distance / 159.0;
      }

      this.speed = Math.max(this.speed, this.getDefaultSpeed(clientPlayerEntity));
      if (this.settings.speedLimit()) {
         if (System.currentTimeMillis() - this.timer > 2500L) {
            this.timer = System.currentTimeMillis();
         }

         this.speed = Math.min(this.speed, System.currentTimeMillis() - this.timer > 1250L ? 0.44 : 0.43);
      }

      double[] var6 = transformStrafe(clientPlayerEntity, this.speed);
      return new Vec3d(var6[0], var4, var6[1]);
   }

   private static double[] transformStrafe(ClientPlayerEntity clientPlayerEntity, double d) {
      Vec2f var3 = clientPlayerEntity.input.getMovementInput();
      float var4 = Math.signum(var3.y);
      float var5 = Math.signum(var3.x);
      float var6 = clientPlayerEntity.getLerpedYaw(MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true));
      if (var4 == 0.0F && var5 == 0.0F) {
         return new double[2];
      } else {
         float var7 = 90.0F * var5;
         if (var4 != 0.0F) {
            var7 *= var4 * 0.5F;
         }

         var6 -= var7;
         if (var4 < 0.0F) {
            var6 -= 180.0F;
         }

         double var8 = Math.toRadians(var6);
         return new double[]{-Math.sin(var8) * d, Math.cos(var8) * d};
      }
   }

   @Override
   public void onTick(ClientPlayerEntity clientPlayerEntity) {
      double var2 = clientPlayerEntity.getX() - clientPlayerEntity.lastX;
      double var4 = clientPlayerEntity.getZ() - clientPlayerEntity.lastZ;
      this.distance = Math.sqrt(var2 * var2 + var4 * var4);
   }
}
