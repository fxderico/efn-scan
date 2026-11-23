package com.example.client.mixin;

import net.efn.client.module.impl.render.character.CharacterLayer;
import net.efn.client.module.impl.render.character.CharacterModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntityRenderer.class})
public abstract class MixinCharacterAvatarRenderer<T extends PlayerLikeEntity> extends LivingEntityRenderer<T, PlayerEntityRenderState, PlayerEntityModel> {
   protected MixinCharacterAvatarRenderer(Context context, PlayerEntityModel playerEntityModel, float f) {
      super(context, playerEntityModel, f);
   }

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void efn$addCharacterLayer(Context context, boolean bl, CallbackInfo callbackInfo) {
      this.addFeature(new CharacterLayer(this));
   }

   @Inject(
      method = {"updateRenderState"},
      at = {@At("TAIL")}
   )
   private void efn$extractCharacter(PlayerLikeEntity playerLikeEntity, PlayerEntityRenderState playerEntityRenderState, float f, CallbackInfo callbackInfo) {
      CharacterModule.extract(playerLikeEntity, playerEntityRenderState);
   }

   @Inject(
      method = {"renderArm"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$characterFirstPersonHand(
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      Identifier identifier,
      ModelPart modelPart,
      boolean bl,
      CallbackInfo callbackInfo
   ) {
      if (CharacterLayer.firstPersonHand(this.getModel(), matrixStack, orderedRenderCommandQueue, i, modelPart)) {
         callbackInfo.cancel();
      }
   }
}
