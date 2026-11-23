package com.example.client.mixin;

import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({AbstractBlockState.class})
public abstract class MixinBlockStateBase {
   public MixinBlockStateBase() {
   }

   @Inject(
      method = {"method_26210"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$xrayNoAmbientOcclusion(BlockView var1, BlockPos var2, CallbackInfoReturnable<Float> var3) {
      if (XrayModule.active != null) {
         var3.setReturnValue(1.0F);
      }
   }
}
