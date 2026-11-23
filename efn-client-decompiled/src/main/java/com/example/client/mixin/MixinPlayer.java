package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.module.impl.movement.FlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntity.class})
public abstract class MixinPlayer {
   public MixinPlayer() {
   }

   @Inject(
      method = {"method_7324"},
      at = {@At("HEAD")}
   )
   private void efn$onAttack(Entity var1, CallbackInfo var2) {
      if (this == MinecraftClient.method_1551().field_1724) {
         PlayerAttackEntityEvent.dispatch(var1);
      }
   }

   @ModifyReturnValue(
      method = {"method_49484"},
      at = {@At("RETURN")}
   )
   private float efn$flySpeed(float var1) {
      FlyModule var2 = FlyModule.active;
      if (var2 == null) {
         return var1;
      } else {
         ClientPlayerEntity var3 = MinecraftClient.method_1551().field_1724;
         if (var3 != null && this == var3) {
            float var4 = var2.getOffGroundSpeed(var3);
            return var4 != -1.0F ? var4 : var1;
         } else {
            return var1;
         }
      }
   }
}
