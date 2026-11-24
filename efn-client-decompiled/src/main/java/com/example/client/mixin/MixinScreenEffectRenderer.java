package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.movement.BoatNoclipModule;
import net.efn.client.module.impl.render.CustomFovModule;
import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({InGameOverlayRenderer.class})
public abstract class MixinScreenEffectRenderer {
   public MixinScreenEffectRenderer() {
   }

   @ModifyExpressionValue(
      method = {"renderOverlays"},
      at = {@At(
         value = "NEW",
         target = "()Lnet/minecraft/client/util/math/MatrixStack;"
      )}
   )
   private MatrixStack efn$undoCustomFov(MatrixStack matrixStack) {
      float var2 = CustomFovModule.screenEffectScale();
      if (var2 != 1.0F) {
         matrixStack.peek().getPositionMatrix().scale(var2, var2, 1.0F);
      }

      return matrixStack;
   }

   @Inject(
      method = {"renderFireOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void efn$cancelFireOverlay(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, Sprite sprite, CallbackInfo callbackInfo) {
      if (NoRenderModule.hideFireOverlay()) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"getInWallBlockState"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void efn$noWallOverlayInNoclipBoat(PlayerEntity playerEntity, CallbackInfoReturnable<BlockState> callbackInfoReturnable) {
      if (BoatNoclipModule.hidesWallOverlay(playerEntity)) {
         callbackInfoReturnable.setReturnValue(null);
      }
   }
}
