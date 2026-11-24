package com.example.client.mixin;

import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ChunkOcclusionDataBuilder.class})
public abstract class MixinVisGraph {
   public MixinVisGraph() {
   }

   @Inject(
      method = {"markClosed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$noOcclusion(BlockPos blockPos, CallbackInfo callbackInfo) {
      if (XrayModule.active != null || FreecamModule.active != null) {
         callbackInfo.cancel();
      }
   }
}
