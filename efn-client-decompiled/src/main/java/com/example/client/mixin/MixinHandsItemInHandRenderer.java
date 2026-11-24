package com.example.client.mixin;

import net.efn.client.module.impl.render.HandsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinHandsItemInHandRenderer {
   public MixinHandsItemInHandRenderer() {
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
         shift = Shift.AFTER
      )}
   )
   private void efn$handsPose(
      AbstractClientPlayerEntity abstractClientPlayerEntity,
      float f,
      float g,
      Hand hand,
      float h,
      ItemStack itemStack,
      float i,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int j,
      CallbackInfo callbackInfo
   ) {
      if (HandsModule.live != null) {
         boolean var12 = hand == Hand.MAIN_HAND;
         Arm var13 = var12 ? abstractClientPlayerEntity.getMainArm() : abstractClientPlayerEntity.getMainArm().getOpposite();
         HandsModule.shape(matrixStack, var12, var13 == Arm.RIGHT ? 1 : -1);
      }
   }
}
