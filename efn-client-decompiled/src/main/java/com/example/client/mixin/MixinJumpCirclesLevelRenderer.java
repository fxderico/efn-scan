package com.example.client.mixin;

import net.efn.client.module.impl.render.JumpCirclesModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({WorldRenderer.class})
public abstract class MixinJumpCirclesLevelRenderer {
   public MixinJumpCirclesLevelRenderer() {
   }

   @Inject(
      method = {"method_62206"},
      at = {@At("HEAD")}
   )
   private void efn$jumpCircles(MatrixStack var1, Immediate var2, WorldRenderState var3, CallbackInfo var4) {
      if (JumpCirclesModule.active != null) {
         JumpCirclesModule.renderLevel(var3.field_63082);
      }
   }
}
