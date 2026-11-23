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
      method = {"method_6028"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void efn$modifySwingDuration(CallbackInfoReturnable<Integer> var1) {
      SlowSwingModule var2 = ModuleRegistry.get(SlowSwingModule.class);
      if (var2 != null && var2.isEnabled()) {
         if (this == MinecraftClient.method_1551().field_1724) {
            var1.setReturnValue(var2.getModifiedSwingDuration((Integer)var1.getReturnValue()));
         }
      }
   }
}
