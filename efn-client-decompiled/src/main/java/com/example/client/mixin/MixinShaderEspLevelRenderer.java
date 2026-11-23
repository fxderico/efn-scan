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
      method = {"method_72914"},
      at = {@At("RETURN")}
   )
   private void efn$glowEspKeepState(Entity var1, float var2, CallbackInfoReturnable<EntityRenderState> var3) {
      if (ShaderEspModule.active != null) {
         ShaderEspModule.extracted(var1, (EntityRenderState)var3.getReturnValue());
      }
   }

   @Inject(
      method = {"method_22710"},
      at = {@At("TAIL")}
   )
   private void efn$glowEsp(
      CallbackInfo var1,
      @Local(argsOnly = true) Camera var2,
      @Local(argsOnly = true,ordinal = 0) Matrix4f var3,
      @Local(argsOnly = true,ordinal = 1) Matrix4f var4
   ) {
      if (ShaderEspModule.active != null) {
         ShaderEspModule.afterLevel(var2, var3, var4);
      }
   }
}
