package com.example.client.mixin;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.SkinChangeModule;
import net.efn.client.module.impl.render.NametagsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntityRenderer.class})
public abstract class MixinPlayerRenderer {
   public MixinPlayerRenderer() {
   }

   @Inject(
      method = {"updateRenderState"},
      at = {@At("TAIL")}
   )
   private void efn$injectSkin(PlayerLikeEntity playerLikeEntity, PlayerEntityRenderState playerEntityRenderState, float f, CallbackInfo callbackInfo) {
      SkinChangeModule var5 = ModuleRegistry.get(SkinChangeModule.class);
      if (var5 != null && var5.isEnabled()) {
         playerEntityRenderState.skinTextures = var5.skinFor(playerEntityRenderState.id, playerEntityRenderState.skinTextures);
      }
   }

   @Inject(
      method = {"renderLabelIfPresent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$suppressVanillaNameTag(
      PlayerEntityRenderState playerEntityRenderState,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      CameraRenderState cameraRenderState,
      CallbackInfo callbackInfo
   ) {
      if (NametagsModule.hideVanillaNametags()) {
         callbackInfo.cancel();
      }
   }
}
