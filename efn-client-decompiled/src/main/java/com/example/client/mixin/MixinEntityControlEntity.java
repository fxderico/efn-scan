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
      method = {"method_5784"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private Vec3d efn$entityControlVehicleMove(Vec3d var1, MovementType var2) {
      return !EntityControlVehicleHook.anyActive ? var1 : EntityControlVehicleHook.onEntityMove((Entity)this, var2, var1);
   }
}
