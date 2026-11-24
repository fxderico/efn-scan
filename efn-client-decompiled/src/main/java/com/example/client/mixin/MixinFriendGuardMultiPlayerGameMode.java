package com.example.client.mixin;

import net.efn.client.module.impl.combat.FriendGuardModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinFriendGuardMultiPlayerGameMode {
   public MixinFriendGuardMultiPlayerGameMode() {
   }

   @Inject(
      method = {"attackEntity"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 900
   )
   private void efn$friendGuard(PlayerEntity playerEntity, Entity entity, CallbackInfo callbackInfo) {
      if (FriendGuardModule.intercepts(playerEntity, entity)) {
         playerEntity.resetTicksSince();
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"attackWithPiercingWeapon"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 900
   )
   private void efn$friendGuardStab(PiercingWeaponComponent piercingWeaponComponent, CallbackInfo callbackInfo) {
      if (FriendGuardModule.interceptsStab(MinecraftClient.getInstance().player)) {
         callbackInfo.cancel();
      }
   }
}
