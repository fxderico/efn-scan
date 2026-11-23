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

   protected SpeedMode(SpeedModule var1) {
      this.settings = var1;
      this.reset();
   }

   public void onTick(ClientPlayerEntity var1) {
   }

   public Vec3d onMove(ClientPlayerEntity var1, Vec3d var2) {
      return var2;
   }

   public void onRubberband() {
      this.reset();
   }

   public void onActivate() {
   }

   public void onDeactivate() {
   }

   protected double getDefaultSpeed(ClientPlayerEntity var1) {
      double var3 = 0.2873;
      StatusEffectInstance var5 = var1.method_6112(StatusEffects.field_5904);
      if (var5 != null) {
         var3 *= 1.0 + 0.2 * (var5.method_5578() + 1);
      }

      StatusEffectInstance var2;
      if ((var2 = var1.method_6112(StatusEffects.field_5909)) != null) {
         var3 /= 1.0 + 0.2 * (var2.method_5578() + 1);
      }

      return var3;
   }

   protected void reset() {
      this.stage = 0;
      this.distance = 0.0;
      this.speed = 0.2873;
   }

   protected double getHop(ClientPlayerEntity var1, double var2) {
      StatusEffectInstance var4 = var1.method_6112(StatusEffects.field_5913);
      if (var4 != null) {
         var2 += (var4.method_5578() + 1) * 0.1F;
      }

      return var2;
   }
}
