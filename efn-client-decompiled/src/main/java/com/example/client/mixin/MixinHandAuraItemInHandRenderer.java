package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.render.HandAuraModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinHandAuraItemInHandRenderer {
   public MixinHandAuraItemInHandRenderer() {
   }

   @WrapOperation(
      method = {"renderItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/command/RenderDispatcher;render()V"
      )}
   )
   private void efn$handAura(RenderDispatcher renderDispatcher, Operation<Void> operation) {
      if (HandAuraModule.active != null && HandAuraModule.beginCapture()) {
         boolean var3 = false;

         try {
            operation.call(renderDispatcher);
            MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
            var3 = true;
         } finally {
            HandAuraModule.endCapture(var3);
         }
      } else {
         operation.call(renderDispatcher);
      }
   }
}
