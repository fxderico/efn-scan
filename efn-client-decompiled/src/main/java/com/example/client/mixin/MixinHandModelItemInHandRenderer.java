package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.render.HandModelModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinHandModelItemInHandRenderer {
   public MixinHandModelItemInHandRenderer() {
   }

   @WrapOperation(
      method = {"renderArmHoldingItem", "renderArm"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
      )}
   )
   private void efn$longRightArm(
      PlayerEntityRenderer<?> playerEntityRenderer,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      Identifier identifier,
      boolean bl,
      Operation<Void> operation
   ) {
      operation.call(playerEntityRenderer, matrixStack, orderedRenderCommandQueue, i, identifier, bl);
      if (HandModelModule.active != null) {
         matrixStack.push();
         HandModelModule.shoulder(matrixStack, true, playerEntityRenderer);
         operation.call(playerEntityRenderer, matrixStack, orderedRenderCommandQueue, i, identifier, bl);
         matrixStack.pop();
      }
   }

   @WrapOperation(
      method = {"renderArmHoldingItem", "renderArm"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
      )}
   )
   private void efn$longLeftArm(
      PlayerEntityRenderer<?> playerEntityRenderer,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      Identifier identifier,
      boolean bl,
      Operation<Void> operation
   ) {
      operation.call(playerEntityRenderer, matrixStack, orderedRenderCommandQueue, i, identifier, bl);
      if (HandModelModule.active != null) {
         matrixStack.push();
         HandModelModule.shoulder(matrixStack, false, playerEntityRenderer);
         operation.call(playerEntityRenderer, matrixStack, orderedRenderCommandQueue, i, identifier, bl);
         matrixStack.pop();
      }
   }
}
