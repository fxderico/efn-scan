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
      method = {"method_60904"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_853;method_8320(Lnet/minecraft/class_2338;)Lnet/minecraft/class_2680;"
      )}
   )
   private BlockState efn$xrayBlockState(ChunkRendererRegion var1, BlockPos var2, Operation<BlockState> var3) {
      BlockState var5 = (BlockState)var3.call(new Object[]{var1, var2});
      XrayModule.Snapshot var6 = XrayModule.active;
      if (var6 != null && !var5.method_26215()) {
         float[] var7 = XrayModule.CURRENT_ALPHA.get();
         float var4;
         var7[0] = var4 = var6.alphaFor(var1, var5, var2);
         return var4 <= 0.0F ? Blocks.field_10124.method_9564() : var5;
      } else {
         return var5;
      }
   }

   @WrapOperation(
      method = {"method_60904"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_4696;method_23679(Lnet/minecraft/class_2680;)Lnet/minecraft/class_11515;"
      )}
   )
   private BlockRenderLayer efn$xrayBlockLayer(BlockState var1, Operation<BlockRenderLayer> var2) {
      if (XrayModule.active == null) {
         return (BlockRenderLayer)var2.call(new Object[]{var1});
      } else {
         float var3 = XrayModule.CURRENT_ALPHA.get()[0];
         return var3 > 0.0F && var3 < 1.0F ? BlockRenderLayer.field_60926 : (BlockRenderLayer)var2.call(new Object[]{var1});
      }
   }

   @WrapOperation(
      method = {"method_60904"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_4696;method_23680(Lnet/minecraft/class_3610;)Lnet/minecraft/class_11515;"
      )}
   )
   private BlockRenderLayer efn$xrayFluidLayer(FluidState var1, Operation<BlockRenderLayer> var2) {
      if (XrayModule.active == null) {
         return (BlockRenderLayer)var2.call(new Object[]{var1});
      } else {
         float var3 = XrayModule.CURRENT_ALPHA.get()[0];
         return var3 > 0.0F && var3 < 1.0F ? BlockRenderLayer.field_60926 : (BlockRenderLayer)var2.call(new Object[]{var1});
      }
   }

   @Inject(
      method = {"method_60904"},
      at = {@At("RETURN")}
   )
   private void efn$xrayResetAlpha(
      ChunkSectionPos var1, ChunkRendererRegion var2, VertexSorter var3, BlockBufferAllocatorStorage var4, CallbackInfoReturnable<RenderData> var5
   ) {
      XrayModule.CURRENT_ALPHA.get()[0] = 1.0F;
   }
}
