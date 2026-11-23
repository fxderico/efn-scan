package com.example.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.efn.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerEntity.class})
public abstract class MixinSpeedLocalPlayer {
   public MixinSpeedLocalPlayer() {
   }

   @ModifyVariable(
      method = {"method_5784"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private Vec3d efn$speedMove(Vec3d var1, @Local(argsOnly = true) MovementType var2) {
      SpeedModule var3 = SpeedModule.active;
      return var3 != null && var2 == MovementType.field_6308 ? var3.onSelfMove((ClientPlayerEntity)this, var1) : var1;
   }
}
