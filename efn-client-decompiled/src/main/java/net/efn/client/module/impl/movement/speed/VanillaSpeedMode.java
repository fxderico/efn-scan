package net.efn.client.module.impl.movement.speed;

import net.efn.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class VanillaSpeedMode extends SpeedMode {
   private static final double DIAGONAL = 1.0 / Math.sqrt(2.0);

   public VanillaSpeedMode(SpeedModule speedModule) {
      super(speedModule);
   }

   @Override
   public Vec3d onMove(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      Vec3d var3 = horizontalVelocity(clientPlayerEntity, this.settings.vanillaSpeed());
      double var4 = var3.x;
      double var6 = var3.z;
      StatusEffectInstance var8 = clientPlayerEntity.getStatusEffect(StatusEffects.SPEED);
      if (var8 != null) {
         double var9 = (var8.getAmplifier() + 1) * 0.205;
         var4 += var4 * var9;
         var6 += var6 * var9;
      }

      return new Vec3d(var4, vec3d.y, var6);
   }

   private static Vec3d horizontalVelocity(ClientPlayerEntity clientPlayerEntity, double d) {
      float var3 = clientPlayerEntity.getYaw();
      Vec3d var4 = Vec3d.fromPolar(0.0F, var3);
      Vec3d var5 = Vec3d.fromPolar(0.0F, var3 + 90.0F);
      PlayerInput var6 = clientPlayerEntity.input.playerInput;
      double var7 = 0.0;
      double var9 = 0.0;
      boolean var11 = false;
      if (var6.forward()) {
         var7 += var4.x / 20.0 * d;
         var9 += var4.z / 20.0 * d;
         var11 = true;
      }

      if (var6.backward()) {
         var7 -= var4.x / 20.0 * d;
         var9 -= var4.z / 20.0 * d;
         var11 = true;
      }

      boolean var12 = false;
      if (var6.right()) {
         var7 += var5.x / 20.0 * d;
         var9 += var5.z / 20.0 * d;
         var12 = true;
      }

      if (var6.left()) {
         var7 -= var5.x / 20.0 * d;
         var9 -= var5.z / 20.0 * d;
         var12 = true;
      }

      if (var11 && var12) {
         var7 *= DIAGONAL;
         var9 *= DIAGONAL;
      }

      return new Vec3d(var7, 0.0, var9);
   }
}
