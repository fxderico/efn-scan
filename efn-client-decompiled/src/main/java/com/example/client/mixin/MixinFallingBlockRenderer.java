package com.example.client.mixin;

import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;
import net.minecraft.entity.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({FallingBlockEntityRenderer.class})
public abstract class MixinFallingBlockRenderer {
   public MixinFallingBlockRenderer() {
   }

   @Inject(
      method = {"method_62436"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$cancelFallingBlock(FallingBlockEntity var1, Frustum var2, double var3, double var5, double var7, CallbackInfoReturnable<Boolean> var9) {
      if (NoRenderModule.hideFallingBlocks()) {
         var9.setReturnValue(false);
      }
   }
}
