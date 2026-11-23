package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.movement.BoatNoclipModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({AbstractBoatEntity.class})
public abstract class MixinBoatNoclipAbstractBoat {
   public MixinBoatNoclipAbstractBoat() {
   }

   @ModifyReturnValue(
      method = {"collidesWith"},
      at = {@At("RETURN")}
   )
   private boolean efn$boatNoclipCollide(boolean bl) {
      if (bl && BoatNoclipModule.active != null) {
         AbstractBoatEntity var2 = (AbstractBoatEntity)this;
         return !var2.getEntityWorld().isClient() || var2.getControllingPassenger() != MinecraftClient.getInstance().player;
      } else {
         return bl;
      }
   }
}
