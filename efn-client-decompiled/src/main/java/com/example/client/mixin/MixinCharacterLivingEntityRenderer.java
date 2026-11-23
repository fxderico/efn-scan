package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.render.character.CharacterModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntityRenderer.class})
public abstract class MixinCharacterLivingEntityRenderer {
   public MixinCharacterLivingEntityRenderer() {
   }

   @Inject(
      method = {"method_24302"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$hideModelUnderCharacter(LivingEntityRenderState var1, boolean var2, boolean var3, boolean var4, CallbackInfoReturnable<RenderLayer> var5) {
      if (CharacterModule.hidesModel(var1)) {
         var5.setReturnValue(null);
      }
   }

   @WrapOperation(
      method = {"method_4054"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_3887;method_4199(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;ILnet/minecraft/class_10017;FF)V"
      )}
   )
   private void efn$hideLayersUnderCharacter(
      FeatureRenderer<?, ?> var1,
      MatrixStack var2,
      OrderedRenderCommandQueue var3,
      int var4,
      EntityRenderState var5,
      float var6,
      float var7,
      Operation<Void> var8
   ) {
      if (!CharacterModule.hidesLayer(var1, var5)) {
         var8.call(new Object[]{var1, var2, var3, var4, var5, var6, var7});
      }
   }
}
