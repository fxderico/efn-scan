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
      method = {"method_23067"},
      at = {@At(
         value = "NEW",
         target = "()Lnet/minecraft/class_4587;"
      )}
   )
   private MatrixStack efn$undoCustomFov(MatrixStack var1) {
      float var2 = CustomFovModule.screenEffectScale();
      if (var2 != 1.0F) {
         var1.method_23760().method_23761().scale(var2, var2, 1.0F);
      }

      return var1;
   }

   @Inject(
      method = {"method_23070"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void efn$cancelFireOverlay(MatrixStack var0, VertexConsumerProvider var1, Sprite var2, CallbackInfo var3) {
      if (NoRenderModule.hideFireOverlay()) {
         var3.cancel();
      }
   }

   @Inject(
      method = {"method_24225"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void efn$noWallOverlayInNoclipBoat(PlayerEntity var0, CallbackInfoReturnable<BlockState> var1) {
      if (BoatNoclipModule.hidesWallOverlay(var0)) {
         var1.setReturnValue(null);
      }
   }
}
