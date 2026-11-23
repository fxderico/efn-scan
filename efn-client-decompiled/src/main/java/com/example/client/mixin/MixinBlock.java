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
      method = {"method_9607"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void efn$xrayForceFace(BlockState var0, BlockState var1, Direction var2, CallbackInfoReturnable<Boolean> var3) {
      XrayModule.Snapshot var4 = XrayModule.active;
      if (var4 != null && var4.isWhitelisted(var0.method_26204()) && XrayModule.CURRENT_ALPHA.get()[0] >= 1.0F) {
         var3.setReturnValue(true);
      }
   }
}
