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
      method = {"getRenderState"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$xraySkipHiddenBlockEntity(
      BlockEntity blockEntity, float f, CrumblingOverlayCommand crumblingOverlayCommand, CallbackInfoReturnable<BlockEntityRenderState> callbackInfoReturnable
   ) {
      XrayModule.Snapshot var5 = XrayModule.active;
      if (var5 != null) {
         World var6 = blockEntity.getWorld();
         if (var6 != null) {
            BlockPos var7 = blockEntity.getPos();
            BlockState var8 = blockEntity.getCachedState();
            if (var5.alphaFor(var6, var8, var7) <= 0.0F) {
               callbackInfoReturnable.setReturnValue(null);
            }
         }
      }
   }
}
