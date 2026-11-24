package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.VertexSorter;
import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.chunk.BlockBufferAllocatorStorage;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.client.render.chunk.SectionBuilder.RenderData;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({SectionBuilder.class})
public abstract class MixinSectionCompiler {
   public MixinSectionCompiler() {
   }

   @WrapOperation(
      method = {"build"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/chunk/ChunkRendererRegion;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"
      )}
   )
   private BlockState efn$xrayBlockState(ChunkRendererRegion chunkRendererRegion, BlockPos blockPos, Operation<BlockState> operation) {
      BlockState var5 = (BlockState)operation.call(chunkRendererRegion, blockPos);
      XrayModule.Snapshot var6 = XrayModule.active;
      if (var6 != null && !var5.isAir()) {
         float[] var7 = XrayModule.CURRENT_ALPHA.get();
         float var4;
         var7[0] = var4 = var6.alphaFor(chunkRendererRegion, var5, blockPos);
         return var4 <= 0.0F ? Blocks.AIR.getDefaultState() : var5;
      } else {
         return var5;
      }
   }

   @WrapOperation(
      method = {"build"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/BlockRenderLayers;getBlockLayer(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/BlockRenderLayer;"
      )}
   )
   private BlockRenderLayer efn$xrayBlockLayer(BlockState blockState, Operation<BlockRenderLayer> operation) {
      if (XrayModule.active == null) {
         return (BlockRenderLayer)operation.call(blockState);
      } else {
         float var3 = XrayModule.CURRENT_ALPHA.get()[0];
         return var3 > 0.0F && var3 < 1.0F ? BlockRenderLayer.TRANSLUCENT : (BlockRenderLayer)operation.call(blockState);
      }
   }

   @WrapOperation(
      method = {"build"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/BlockRenderLayers;getFluidLayer(Lnet/minecraft/fluid/FluidState;)Lnet/minecraft/client/render/BlockRenderLayer;"
      )}
   )
   private BlockRenderLayer efn$xrayFluidLayer(FluidState fluidState, Operation<BlockRenderLayer> operation) {
      if (XrayModule.active == null) {
         return (BlockRenderLayer)operation.call(fluidState);
      } else {
         float var3 = XrayModule.CURRENT_ALPHA.get()[0];
         return var3 > 0.0F && var3 < 1.0F ? BlockRenderLayer.TRANSLUCENT : (BlockRenderLayer)operation.call(fluidState);
      }
   }

   @Inject(
      method = {"build"},
      at = {@At("RETURN")}
   )
   private void efn$xrayResetAlpha(
      ChunkSectionPos chunkSectionPos,
      ChunkRendererRegion chunkRendererRegion,
      VertexSorter vertexSorter,
      BlockBufferAllocatorStorage blockBufferAllocatorStorage,
      CallbackInfoReturnable<RenderData> callbackInfoReturnable
   ) {
      XrayModule.CURRENT_ALPHA.get()[0] = 1.0F;
   }
}
