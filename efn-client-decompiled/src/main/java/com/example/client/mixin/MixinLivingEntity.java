package com.example.client.mixin;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.SlowSwingModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinLivingEntity {
   public MixinLivingEntity() {
   }

   @Inject(
      method = {"getHandSwingDuration"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void efn$modifySwingDuration(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
      SlowSwingModule var2 = ModuleRegistry.get(SlowSwingModule.class);
      if (var2 != null && var2.isEnabled()) {
         if (this == MinecraftClient.getInstance().player) {
            callbackInfoReturnable.setReturnValue(var2.getModifiedSwingDuration((Integer)callbackInfoReturnable.getReturnValue()));
         }
      }
   }
}
