package com.example.client.mixin;

import net.efn.client.module.impl.misc.AirStuckModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerEntity.class})
public abstract class MixinAirStuckLocalPlayer {
   public MixinAirStuckLocalPlayer() {
   }

   @Inject(
      method = {"method_6007"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$airStuck(CallbackInfo var1) {
      if (AirStuckModule.active != null) {
         var1.cancel();
      }
   }
}
