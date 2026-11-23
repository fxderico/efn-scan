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
      method = {"method_30949"},
      at = {@At("RETURN")}
   )
   private boolean efn$boatNoclipCollide(boolean var1) {
      if (var1 && BoatNoclipModule.active != null) {
         AbstractBoatEntity var2 = (AbstractBoatEntity)this;
         return !var2.method_73183().method_8608() || var2.method_5642() != MinecraftClient.method_1551().field_1724;
      } else {
         return var1;
      }
   }
}
