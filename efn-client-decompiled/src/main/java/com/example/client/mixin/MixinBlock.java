package com.example.client.mixin;

import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({Block.class})
public abstract class MixinBlock {
   public MixinBlock() {
   }

   @Inject(
      method = {"shouldDrawSide"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void efn$xrayForceFace(
      BlockState blockState, BlockState blockState2, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable
   ) {
      XrayModule.Snapshot var4 = XrayModule.active;
      if (var4 != null && var4.isWhitelisted(blockState.getBlock()) && XrayModule.CURRENT_ALPHA.get()[0] >= 1.0F) {
         callbackInfoReturnable.setReturnValue(true);
      }
   }
}
