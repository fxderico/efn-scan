package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({AbstractBoatEntity.class})
public abstract class MixinEntityControlAbstractBoat {
   public MixinEntityControlAbstractBoat() {
   }

   @ModifyExpressionValue(
      method = {"method_64482"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/class_10255;field_54444:Z",
         opcode = 180
      )}
   )
   private boolean efn$entityControlInputLeft(boolean var1) {
      return var1 && !EntityControlVehicleHook.locksBoatTurning((AbstractBoatEntity)this);
   }

   @ModifyExpressionValue(
      method = {"method_64482"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/class_10255;field_54428:Z",
         opcode = 180
      )}
   )
   private boolean efn$entityControlInputRight(boolean var1) {
      return var1 && !EntityControlVehicleHook.locksBoatTurning((AbstractBoatEntity)this);
   }
}
