package com.example.client.mixin;

import net.efn.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin({Entity.class})
public abstract class MixinEntityControlEntity {
   public MixinEntityControlEntity() {
   }

   @ModifyVariable(
      method = {"move"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private Vec3d efn$entityControlVehicleMove(Vec3d vec3d, MovementType movementType) {
      return !EntityControlVehicleHook.anyActive ? vec3d : EntityControlVehicleHook.onEntityMove((Entity)this, movementType, vec3d);
   }
}
