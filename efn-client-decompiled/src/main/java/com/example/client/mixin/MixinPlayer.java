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
      method = {"attack"},
      at = {@At("HEAD")}
   )
   private void efn$onAttack(Entity entity, CallbackInfo callbackInfo) {
      if (this == MinecraftClient.getInstance().player) {
         PlayerAttackEntityEvent.dispatch(entity);
      }
   }

   @ModifyReturnValue(
      method = {"getOffGroundSpeed"},
      at = {@At("RETURN")}
   )
   private float efn$flySpeed(float f) {
      FlyModule var2 = FlyModule.active;
      if (var2 == null) {
         return f;
      } else {
         ClientPlayerEntity var3 = MinecraftClient.getInstance().player;
         if (var3 != null && this == var3) {
            float var4 = var2.getOffGroundSpeed(var3);
            return var4 != -1.0F ? var4 : f;
         } else {
            return f;
         }
      }
   }
}
