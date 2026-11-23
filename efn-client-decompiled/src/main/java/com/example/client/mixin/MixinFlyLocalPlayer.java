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
      method = {"method_5715"},
      at = {@At("RETURN")}
   )
   private boolean efn$flyNoSneak(boolean var1) {
      if (!var1) {
         return false;
      } else {
         FlyModule var2 = FlyModule.active;
         return var2 == null || !var2.noSneak();
      }
   }

   @ModifyExpressionValue(
      method = {"method_6007"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1656;method_7252()F"
      )}
   )
   private float efn$flyFreecamVertical(float var1) {
      FlyModule var2 = FlyModule.active;
      return var2 != null && var2.suppressFlightVerticalInput() ? 0.0F : var1;
   }
}
