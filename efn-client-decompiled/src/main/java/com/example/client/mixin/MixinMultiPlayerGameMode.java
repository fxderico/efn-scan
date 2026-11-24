package com.example.client.mixin;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.WTapRecodeModule;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinMultiPlayerGameMode {
   public MixinMultiPlayerGameMode() {
   }

   @Inject(
      method = {"attackEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;syncSelectedSlot()V"
      )}
   )
   private void efn$beforeAttack(PlayerEntity playerEntity, Entity entity, CallbackInfo callbackInfo) {
      WTapRecodeModule var4 = ModuleRegistry.get(WTapRecodeModule.class);
      if (var4 != null && var4.isEnabled()) {
         var4.beforeAttack(playerEntity, entity);
      }
   }

   @Inject(
      method = {"attackEntity"},
      at = {@At("RETURN")}
   )
   private void efn$afterAttack(PlayerEntity playerEntity, Entity entity, CallbackInfo callbackInfo) {
      if (playerEntity == MinecraftClient.getInstance().player) {
         MeleeAttacks.noteAction(playerEntity);
      }

      WTapRecodeModule var4;
      if ((var4 = ModuleRegistry.get(WTapRecodeModule.class)) != null && var4.isEnabled()) {
         var4.afterAttack();
      }
   }

   @Inject(
      method = {"attackWithPiercingWeapon"},
      at = {@At("RETURN")}
   )
   private void efn$afterStab(PiercingWeaponComponent piercingWeaponComponent, CallbackInfo callbackInfo) {
      MeleeAttacks.noteAction(MinecraftClient.getInstance().player);
      WTapRecodeModule var3 = ModuleRegistry.get(WTapRecodeModule.class);
      if (var3 != null && var3.isEnabled()) {
         var3.afterStab();
      }
   }

   @Inject(
      method = {"interactItem"},
      at = {@At("RETURN")}
   )
   private void efn$afterUseItem(PlayerEntity playerEntity, Hand hand, CallbackInfoReturnable<ActionResult> callbackInfoReturnable) {
      if (playerEntity == MinecraftClient.getInstance().player) {
         MeleeAttacks.noteAction(playerEntity);
      }
   }

   @Inject(
      method = {"interactBlock"},
      at = {@At("RETURN")}
   )
   private void efn$afterUseItemOn(
      ClientPlayerEntity clientPlayerEntity, Hand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> callbackInfoReturnable
   ) {
      if (clientPlayerEntity == MinecraftClient.getInstance().player) {
         MeleeAttacks.noteAction(clientPlayerEntity);
      }
   }

   @Inject(
      method = {"interactEntity"},
      at = {@At("RETURN")}
   )
   private void efn$afterInteract(PlayerEntity playerEntity, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> callbackInfoReturnable) {
      if (playerEntity == MinecraftClient.getInstance().player) {
         MeleeAttacks.noteAction(playerEntity);
      }
   }

   @Inject(
      method = {"interactEntityAtLocation"},
      at = {@At("RETURN")}
   )
   private void efn$afterInteractAt(
      PlayerEntity playerEntity, Entity entity, EntityHitResult entityHitResult, Hand hand, CallbackInfoReturnable<ActionResult> callbackInfoReturnable
   ) {
      if (playerEntity == MinecraftClient.getInstance().player) {
         MeleeAttacks.noteAction(playerEntity);
      }
   }

   @Inject(
      method = {"stopUsingItem"},
      at = {@At("RETURN")}
   )
   private void efn$afterRelease(PlayerEntity playerEntity, CallbackInfo callbackInfo) {
      if (playerEntity == MinecraftClient.getInstance().player) {
         MeleeAttacks.noteAction(playerEntity);
      }
   }
}
