package com.example.client.mixin;

import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({BlockEntityRenderManager.class})
public abstract class MixinBlockEntityRenderDispatcher {
   public MixinBlockEntityRenderDispatcher() {
   }

   @Inject(
      method = {"method_74348"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$xraySkipHiddenBlockEntity(BlockEntity var1, float var2, CrumblingOverlayCommand var3, CallbackInfoReturnable<BlockEntityRenderState> var4) {
      XrayModule.Snapshot var5 = XrayModule.active;
      if (var5 != null) {
         World var6 = var1.method_10997();
         if (var6 != null) {
            BlockPos var7 = var1.method_11016();
            BlockState var8 = var1.method_11010();
            if (var5.alphaFor(var6, var8, var7) <= 0.0F) {
               var4.setReturnValue(null);
            }
         }
      }
   }
}
