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
      method = {"method_62320"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$hideWeather(VertexConsumerProvider var1, Vec3d var2, WeatherRenderState var3, CallbackInfo var4) {
      if (NoRenderModule.hideWeather()) {
         var4.cancel();
      }
   }

   @Inject(
      method = {"method_62319"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$hideRainSplashes(ClientWorld var1, Camera var2, int var3, ParticlesMode var4, int var5, CallbackInfo var6) {
      if (NoRenderModule.hideWeather()) {
         var6.cancel();
      }
   }
}
