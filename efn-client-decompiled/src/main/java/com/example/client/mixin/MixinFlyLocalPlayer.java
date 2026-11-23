package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.movement.FlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerEntity.class})
public abstract class MixinFlyLocalPlayer {
   public MixinFlyLocalPlayer() {
   }

   @ModifyReturnValue(
      method = {"isSneaking"},
      at = {@At("RETURN")}
   )
   private boolean efn$flyNoSneak(boolean bl) {
      if (!bl) {
         return false;
      } else {
         FlyModule var2 = FlyModule.active;
         return var2 == null || !var2.noSneak();
      }
   }

   @ModifyExpressionValue(
      method = {"tickMovement"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerAbilities;getFlySpeed()F"
      )}
   )
   private float efn$flyFreecamVertical(float f) {
      FlyModule var2 = FlyModule.active;
      return var2 != null && var2.suppressFlightVerticalInput() ? 0.0F : f;
   }
}
