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

   public VanillaSpeedMode(SpeedModule var1) {
      super(var1);
   }

   @Override
   public Vec3d onMove(ClientPlayerEntity var1, Vec3d var2) {
      Vec3d var3 = horizontalVelocity(var1, this.settings.vanillaSpeed());
      double var4 = var3.field_1352;
      double var6 = var3.field_1350;
      StatusEffectInstance var8 = var1.method_6112(StatusEffects.field_5904);
      if (var8 != null) {
         double var9 = (var8.method_5578() + 1) * 0.205;
         var4 += var4 * var9;
         var6 += var6 * var9;
      }

      return new Vec3d(var4, var2.field_1351, var6);
   }

   private static Vec3d horizontalVelocity(ClientPlayerEntity var0, double var1) {
      float var3 = var0.method_36454();
      Vec3d var4 = Vec3d.method_1030(0.0F, var3);
      Vec3d var5 = Vec3d.method_1030(0.0F, var3 + 90.0F);
      PlayerInput var6 = var0.field_3913.field_54155;
      double var7 = 0.0;
      double var9 = 0.0;
      boolean var11 = false;
      if (var6.comp_3159()) {
         var7 += var4.field_1352 / 20.0 * var1;
         var9 += var4.field_1350 / 20.0 * var1;
         var11 = true;
      }

      if (var6.comp_3160()) {
         var7 -= var4.field_1352 / 20.0 * var1;
         var9 -= var4.field_1350 / 20.0 * var1;
         var11 = true;
      }

      boolean var12 = false;
      if (var6.comp_3162()) {
         var7 += var5.field_1352 / 20.0 * var1;
         var9 += var5.field_1350 / 20.0 * var1;
         var12 = true;
      }

      if (var6.comp_3161()) {
         var7 -= var5.field_1352 / 20.0 * var1;
         var9 -= var5.field_1350 / 20.0 * var1;
         var12 = true;
      }

      if (var11 && var12) {
         var7 *= DIAGONAL;
         var9 *= DIAGONAL;
      }

      return new Vec3d(var7, 0.0, var9);
   }
}
