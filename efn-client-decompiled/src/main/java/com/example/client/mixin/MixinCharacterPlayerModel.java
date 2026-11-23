package com.example.client.mixin;

import net.efn.client.module.impl.render.character.CharacterLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntityModel.class})
public abstract class MixinCharacterPlayerModel {
   public MixinCharacterPlayerModel() {
   }

   @Inject(
      method = {"method_72859"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$characterHand(PlayerEntityRenderState var1, Arm var2, MatrixStack var3, CallbackInfo var4) {
      if (CharacterLayer.translateToHand((PlayerEntityModel)this, var1, var2, var3)) {
         var4.cancel();
      }
   }
}
