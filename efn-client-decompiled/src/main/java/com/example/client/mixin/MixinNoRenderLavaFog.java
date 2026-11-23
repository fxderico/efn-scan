package com.example.client.mixin;

import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.LavaFogModifier;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({LavaFogModifier.class})
public abstract class MixinNoRenderLavaFog {
   public MixinNoRenderLavaFog() {
   }

   @Inject(
      method = {"applyStartEndModifier"},
      at = {@At("TAIL")}
   )
   private void efn$clearLavaFog(
      FogData fogData, Camera camera, ClientWorld clientWorld, float f, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo
   ) {
      if (NoRenderModule.hideLavaFog()) {
         fogData.environmentalStart = -8.0F;
         fogData.environmentalEnd = f;
         fogData.skyEnd = f;
         fogData.cloudEnd = f;
      }
   }
}
