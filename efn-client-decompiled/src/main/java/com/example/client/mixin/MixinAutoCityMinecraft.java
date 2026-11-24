package com.example.client.mixin;

import net.efn.client.module.impl.combat.AutoCityModule;
import net.efn.client.module.impl.combat.bot.CrystalBotModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({MinecraftClient.class})
public abstract class MixinAutoCityMinecraft {
   public MixinAutoCityMinecraft() {
   }

   @Inject(
      method = {"handleBlockBreaking"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$autoCityContinueAttack(boolean bl, CallbackInfo callbackInfo) {
      if (AutoCityModule.mining() || CrystalBotModule.digging()) {
         callbackInfo.cancel();
      }
   }
}
