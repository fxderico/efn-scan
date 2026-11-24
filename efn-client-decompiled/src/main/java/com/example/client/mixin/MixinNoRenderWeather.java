package com.example.client.mixin;

import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({WeatherRendering.class})
public abstract class MixinNoRenderWeather {
   public MixinNoRenderWeather() {
   }

   @Inject(
      method = {"renderPrecipitation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$hideWeather(VertexConsumerProvider vertexConsumerProvider, Vec3d vec3d, WeatherRenderState weatherRenderState, CallbackInfo callbackInfo) {
      if (NoRenderModule.hideWeather()) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"addParticlesAndSound"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$hideRainSplashes(ClientWorld clientWorld, Camera camera, int i, ParticlesMode particlesMode, int j, CallbackInfo callbackInfo) {
      if (NoRenderModule.hideWeather()) {
         callbackInfo.cancel();
      }
   }
}
