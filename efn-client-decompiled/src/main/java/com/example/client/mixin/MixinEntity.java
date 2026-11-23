package com.example.client.mixin;

import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.EspModule;
import net.efn.client.module.impl.render.ItemEspModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({Entity.class})
public class MixinEntity {
   public MixinEntity() {
   }

   @Inject(
      method = {"isGlowing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$isCurrentlyGlowing(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      Entity var2 = (Entity)this;
      if (var2.getEntityWorld().isClient()) {
         if (EspModule.shouldGlow(var2) || ItemEspModule.shouldGlow(var2)) {
            callbackInfoReturnable.setReturnValue(true);
         }
      }
   }

   @Inject(
      method = {"getTeamColorValue"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$getTeamColor(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
      Entity var2 = (Entity)this;
      if (var2.getEntityWorld().isClient()) {
         if (EspModule.shouldGlow(var2)) {
            callbackInfoReturnable.setReturnValue(EspModule.glowColor(var2));
         } else if (ItemEspModule.shouldGlow(var2)) {
            callbackInfoReturnable.setReturnValue(ItemEspModule.glowColor(var2));
         }
      }
   }

   @Inject(
      method = {"isInvisibleTo"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$freecamOwnBody(PlayerEntity playerEntity, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      FreecamModule var3 = FreecamModule.active;
      if (var3 != null && var3.drawsSelf((Entity)this, playerEntity)) {
         callbackInfoReturnable.setReturnValue(false);
      }
   }
}
