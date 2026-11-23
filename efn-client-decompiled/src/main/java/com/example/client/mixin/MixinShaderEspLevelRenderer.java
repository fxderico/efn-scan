package com.example.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.efn.client.module.impl.render.ShaderEspModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({WorldRenderer.class})
public abstract class MixinShaderEspLevelRenderer {
   public MixinShaderEspLevelRenderer() {
   }

   @Inject(
      method = {"getAndUpdateRenderState"},
      at = {@At("RETURN")}
   )
   private void efn$glowEspKeepState(Entity entity, float f, CallbackInfoReturnable<EntityRenderState> callbackInfoReturnable) {
      if (ShaderEspModule.active != null) {
         ShaderEspModule.extracted(entity, (EntityRenderState)callbackInfoReturnable.getReturnValue());
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void efn$glowEsp(
      CallbackInfo callbackInfo,
      @Local(argsOnly = true) Camera camera,
      @Local(argsOnly = true,ordinal = 0) Matrix4f matrix4f,
      @Local(argsOnly = true,ordinal = 1) Matrix4f matrix4f2
   ) {
      if (ShaderEspModule.active != null) {
         ShaderEspModule.afterLevel(camera, matrix4f, matrix4f2);
      }
   }
}
