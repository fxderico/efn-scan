package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.render.FullbrightModule;
import net.efn.client.module.impl.render.NoRenderModule;
import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({LightmapTextureManager.class})
public abstract class MixinLightTexture {
   @Shadow
   @Final
   private GpuTexture glTexture;

   public MixinLightTexture() {
   }

   @Inject(
      method = {"update"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/render/LightmapTextureManager;dirty:Z",
         opcode = 181,
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void efn$xrayFullbright(float f, CallbackInfo callbackInfo) {
      if (XrayModule.active != null) {
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.glTexture, -1);
         callbackInfo.cancel();
      }
   }

   @ModifyExpressionValue(
      method = {"update"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEffectFadeFactor(Lnet/minecraft/registry/entry/RegistryEntry;F)F"
      )}
   )
   private float efn$hideDarknessPulse(float f) {
      return NoRenderModule.hideBlindnessDarkness() ? 0.0F : f;
   }

   @ModifyArg(
      method = {"update"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
         ordinal = 6
      )},
      index = 0
   )
   private float efn$fullbrightGamma(float f) {
      FullbrightModule var2 = ModuleRegistry.get(FullbrightModule.class);
      return var2 != null && var2.isEnabled() ? var2.gamma() : f;
   }
}
