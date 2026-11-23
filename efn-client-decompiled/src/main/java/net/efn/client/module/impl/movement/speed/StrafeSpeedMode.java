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

   public StrafeSpeedMode(SpeedModule var1) {
      super(var1);
   }

   @Override
   public Vec3d onMove(ClientPlayerEntity var1, Vec3d var2) {
      boolean var3 = SpeedModule.isMoving(var1);
      double var4 = var2.field_1351;
      switch (this.stage) {
         case 0:
            if (var3) {
               this.stage++;
               this.speed = 1.1799999475479126 * this.getDefaultSpeed(var1) - 0.01;
            }
         case 1:
            if (var3 && var1.method_24828()) {
               var4 = this.getHop(var1, 0.40123128);
               this.speed = this.speed * this.settings.strafeSpeed();
               this.stage++;
            }
            break;
         case 2:
            this.speed = this.distance - 0.76 * (this.distance - this.getDefaultSpeed(var1));
            this.stage++;
            break;
         case 3:
            if (!var1.method_73183().method_18026(var1.method_5829().method_989(0.0, var1.method_18798().field_1351, 0.0)) || var1.field_5992 && this.stage > 0
               )
             {
               this.stage = 0;
            }

            this.speed = this.distance - this.distance / 159.0;
      }

      this.speed = Math.max(this.speed, this.getDefaultSpeed(var1));
      if (this.settings.speedLimit()) {
         if (System.currentTimeMillis() - this.timer > 2500L) {
            this.timer = System.currentTimeMillis();
         }

         this.speed = Math.min(this.speed, System.currentTimeMillis() - this.timer > 1250L ? 0.44 : 0.43);
      }

      double[] var6 = transformStrafe(var1, this.speed);
      return new Vec3d(var6[0], var4, var6[1]);
   }

   private static double[] transformStrafe(ClientPlayerEntity var0, double var1) {
      Vec2f var3 = var0.field_3913.method_3128();
      float var4 = Math.signum(var3.field_1342);
      float var5 = Math.signum(var3.field_1343);
      float var6 = var0.method_61415(MinecraftClient.method_1551().method_61966().method_60637(true));
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
         return new double[]{-Math.sin(var8) * var1, Math.cos(var8) * var1};
      }
   }

   @Override
   public void onTick(ClientPlayerEntity var1) {
      double var2 = var1.method_23317() - var1.field_6014;
      double var4 = var1.method_23321() - var1.field_5969;
      this.distance = Math.sqrt(var2 * var2 + var4 * var4);
   }
}
