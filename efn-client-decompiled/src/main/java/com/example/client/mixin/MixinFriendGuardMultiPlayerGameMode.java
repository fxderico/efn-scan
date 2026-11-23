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
      method = {"method_2918"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 900
   )
   private void efn$friendGuard(PlayerEntity var1, Entity var2, CallbackInfo var3) {
      if (FriendGuardModule.intercepts(var1, var2)) {
         var1.method_7350();
         var3.cancel();
      }
   }

   @Inject(
      method = {"method_75407"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 900
   )
   private void efn$friendGuardStab(PiercingWeaponComponent var1, CallbackInfo var2) {
      if (FriendGuardModule.interceptsStab(MinecraftClient.method_1551().field_1724)) {
         var2.cancel();
      }
   }
}
