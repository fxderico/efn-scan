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
      method = {"method_2918"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_636;method_2911()V"
      )}
   )
   private void efn$beforeAttack(PlayerEntity var1, Entity var2, CallbackInfo var3) {
      WTapRecodeModule var4 = ModuleRegistry.get(WTapRecodeModule.class);
      if (var4 != null && var4.isEnabled()) {
         var4.beforeAttack(var1, var2);
      }
   }

   @Inject(
      method = {"method_2918"},
      at = {@At("RETURN")}
   )
   private void efn$afterAttack(PlayerEntity var1, Entity var2, CallbackInfo var3) {
      if (var1 == MinecraftClient.method_1551().field_1724) {
         MeleeAttacks.noteAction(var1);
      }

      WTapRecodeModule var4;
      if ((var4 = ModuleRegistry.get(WTapRecodeModule.class)) != null && var4.isEnabled()) {
         var4.afterAttack();
      }
   }

   @Inject(
      method = {"method_75407"},
      at = {@At("RETURN")}
   )
   private void efn$afterStab(PiercingWeaponComponent var1, CallbackInfo var2) {
      MeleeAttacks.noteAction(MinecraftClient.method_1551().field_1724);
      WTapRecodeModule var3 = ModuleRegistry.get(WTapRecodeModule.class);
      if (var3 != null && var3.isEnabled()) {
         var3.afterStab();
      }
   }

   @Inject(
      method = {"method_2919"},
      at = {@At("RETURN")}
   )
   private void efn$afterUseItem(PlayerEntity var1, Hand var2, CallbackInfoReturnable<ActionResult> var3) {
      if (var1 == MinecraftClient.method_1551().field_1724) {
         MeleeAttacks.noteAction(var1);
      }
   }

   @Inject(
      method = {"method_2896"},
      at = {@At("RETURN")}
   )
   private void efn$afterUseItemOn(ClientPlayerEntity var1, Hand var2, BlockHitResult var3, CallbackInfoReturnable<ActionResult> var4) {
      if (var1 == MinecraftClient.method_1551().field_1724) {
         MeleeAttacks.noteAction(var1);
      }
   }

   @Inject(
      method = {"method_2905"},
      at = {@At("RETURN")}
   )
   private void efn$afterInteract(PlayerEntity var1, Entity var2, Hand var3, CallbackInfoReturnable<ActionResult> var4) {
      if (var1 == MinecraftClient.method_1551().field_1724) {
         MeleeAttacks.noteAction(var1);
      }
   }

   @Inject(
      method = {"method_2917"},
      at = {@At("RETURN")}
   )
   private void efn$afterInteractAt(PlayerEntity var1, Entity var2, EntityHitResult var3, Hand var4, CallbackInfoReturnable<ActionResult> var5) {
      if (var1 == MinecraftClient.method_1551().field_1724) {
         MeleeAttacks.noteAction(var1);
      }
   }

   @Inject(
      method = {"method_2897"},
      at = {@At("RETURN")}
   )
   private void efn$afterRelease(PlayerEntity var1, CallbackInfo var2) {
      if (var1 == MinecraftClient.method_1551().field_1724) {
         MeleeAttacks.noteAction(var1);
      }
   }
}
