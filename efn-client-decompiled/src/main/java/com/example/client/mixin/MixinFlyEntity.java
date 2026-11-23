package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.movement.FlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({Entity.class})
public abstract class MixinFlyEntity {
   public MixinFlyEntity() {
   }

   @ModifyReturnValue(
      method = {"isTouchingWater"},
      at = {@At("RETURN")}
   )
   private boolean efn$flyIsInWater(boolean bl) {
      return bl && !this.efn$flyAffects();
   }

   @ModifyReturnValue(
      method = {"isInLava"},
      at = {@At("RETURN")}
   )
   private boolean efn$flyIsInLava(boolean bl) {
      return bl && !this.efn$flyAffects();
   }

   @ModifyExpressionValue(
      method = {"updateSwimming"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;isSubmergedInWater()Z"
      )}
   )
   private boolean efn$flyIsUnderWater(boolean bl) {
      return bl && !this.efn$flyAffects();
   }

   @Unique
   private boolean efn$flyAffects() {
      return FlyModule.active != null && this == MinecraftClient.getInstance().player;
   }
}
