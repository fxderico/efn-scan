package net.efn.client.module.impl.movement.speed;

import net.efn.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public abstract class SpeedMode {
   private static final double BASE_SPEED = 0.2873;
   protected final SpeedModule settings;
   protected int stage;
   protected double distance;
   protected double speed;

   protected SpeedMode(SpeedModule speedModule) {
      this.settings = speedModule;
      this.reset();
   }

   public void onTick(ClientPlayerEntity clientPlayerEntity) {
   }

   public Vec3d onMove(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      return vec3d;
   }

   public void onRubberband() {
      this.reset();
   }

   public void onActivate() {
   }

   public void onDeactivate() {
   }

   protected double getDefaultSpeed(ClientPlayerEntity clientPlayerEntity) {
      double var3 = 0.2873;
      StatusEffectInstance var5 = clientPlayerEntity.getStatusEffect(StatusEffects.SPEED);
      if (var5 != null) {
         var3 *= 1.0 + 0.2 * (var5.getAmplifier() + 1);
      }

      StatusEffectInstance var2;
      if ((var2 = clientPlayerEntity.getStatusEffect(StatusEffects.SLOWNESS)) != null) {
         var3 /= 1.0 + 0.2 * (var2.getAmplifier() + 1);
      }

      return var3;
   }

   protected void reset() {
      this.stage = 0;
      this.distance = 0.0;
      this.speed = 0.2873;
   }

   protected double getHop(ClientPlayerEntity clientPlayerEntity, double d) {
      StatusEffectInstance var4 = clientPlayerEntity.getStatusEffect(StatusEffects.JUMP_BOOST);
      if (var4 != null) {
         d += (var4.getAmplifier() + 1) * 0.1F;
      }

      return d;
   }
}
