package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.movement.BoatFlyModule;
import net.efn.client.module.impl.movement.EntityControlModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.JumpingMount;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerEntity.class})
public abstract class MixinEntityControlLocalPlayer {
   public MixinEntityControlLocalPlayer() {
   }

   @ModifyReturnValue(
      method = {"getMountJumpStrength"},
      at = {@At("RETURN")}
   )
   private float efn$entityControlMaxJump(float f) {
      return EntityControlModule.maxJump() ? 1.0F : f;
   }

   @ModifyReturnValue(
      method = {"getJumpingMount"},
      at = {@At("RETURN")}
   )
   private JumpingMount efn$boatFlyCancelJump(JumpingMount jumpingMount) {
      return jumpingMount != null && BoatFlyModule.cancelsMountJump((ClientPlayerEntity)this) ? null : jumpingMount;
   }
}
