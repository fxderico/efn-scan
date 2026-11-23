package com.example.client.mixin;

import net.efn.client.module.impl.render.CrownLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntityRenderer.class})
public abstract class MixinCrownAvatarRenderer<T extends PlayerLikeEntity> extends LivingEntityRenderer<T, PlayerEntityRenderState, PlayerEntityModel> {
   protected MixinCrownAvatarRenderer(Context context, PlayerEntityModel playerEntityModel, float f) {
      super(context, playerEntityModel, f);
   }

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void efn$addHeadCosmetics(Context context, boolean bl, CallbackInfo callbackInfo) {
      this.addFeature(new CrownLayer(this));
   }
}
