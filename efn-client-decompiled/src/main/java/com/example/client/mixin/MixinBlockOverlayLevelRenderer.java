package com.example.client.mixin;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.render.BlockOverlayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({WorldRenderer.class})
public abstract class MixinBlockOverlayLevelRenderer {
   public MixinBlockOverlayLevelRenderer() {
   }

   @Inject(
      method = {"renderTargetBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$hideVanillaOutline(CallbackInfo callbackInfo) {
      BlockOverlayModule var2 = ModuleRegistry.get(BlockOverlayModule.class);
      if (var2 != null && var2.isEnabled()) {
         callbackInfo.cancel();
      }
   }
}
