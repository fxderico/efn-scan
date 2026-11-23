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
      method = {"method_5851"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$isCurrentlyGlowing(CallbackInfoReturnable<Boolean> var1) {
      Entity var2 = (Entity)this;
      if (var2.method_73183().method_8608()) {
         if (EspModule.shouldGlow(var2) || ItemEspModule.shouldGlow(var2)) {
            var1.setReturnValue(true);
         }
      }
   }

   @Inject(
      method = {"method_22861"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$getTeamColor(CallbackInfoReturnable<Integer> var1) {
      Entity var2 = (Entity)this;
      if (var2.method_73183().method_8608()) {
         if (EspModule.shouldGlow(var2)) {
            var1.setReturnValue(EspModule.glowColor(var2));
         } else if (ItemEspModule.shouldGlow(var2)) {
            var1.setReturnValue(ItemEspModule.glowColor(var2));
         }
      }
   }

   @Inject(
      method = {"method_5756"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$freecamOwnBody(PlayerEntity var1, CallbackInfoReturnable<Boolean> var2) {
      FreecamModule var3 = FreecamModule.active;
      if (var3 != null && var3.drawsSelf((Entity)this, var1)) {
         var2.setReturnValue(false);
      }
   }
}
