package com.example.client.mixin;

import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.BlockModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin({BlockModelRenderer.class})
public abstract class MixinModelBlockRenderer {
   public MixinModelBlockRenderer() {
   }

   @ModifyArg(
      method = {"renderQuad"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/VertexConsumer;quad(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;[FFFFF[II)V"
      )},
      index = 6
   )
   private float efn$xrayAlpha(float f) {
      return XrayModule.active == null ? f : f * XrayModule.CURRENT_ALPHA.get()[0];
   }
}
