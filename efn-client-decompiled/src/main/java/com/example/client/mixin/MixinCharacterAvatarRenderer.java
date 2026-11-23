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
   protected MixinCharacterAvatarRenderer(Context var1, PlayerEntityModel var2, float var3) {
      super(var1, var2, var3);
   }

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void efn$addCharacterLayer(Context var1, boolean var2, CallbackInfo var3) {
      this.method_4046(new CharacterLayer(this));
   }

   @Inject(
      method = {"method_62604"},
      at = {@At("TAIL")}
   )
   private void efn$extractCharacter(PlayerLikeEntity var1, PlayerEntityRenderState var2, float var3, CallbackInfo var4) {
      CharacterModule.extract(var1, var2);
   }

   @Inject(
      method = {"method_23205"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$characterFirstPersonHand(
      MatrixStack var1, OrderedRenderCommandQueue var2, int var3, Identifier var4, ModelPart var5, boolean var6, CallbackInfo var7
   ) {
      if (CharacterLayer.firstPersonHand((PlayerEntityModel)this.method_4038(), var1, var2, var3, var5)) {
         var7.cancel();
      }
   }
}
