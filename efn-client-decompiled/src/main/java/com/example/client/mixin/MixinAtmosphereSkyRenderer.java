package com.example.client.mixin;

import net.efn.client.module.impl.render.AtmosphereModule;
import net.efn.client.module.impl.render.SkyLyrics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.MoonPhase;
import net.minecraft.world.dimension.DimensionType.Skybox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({SkyRendering.class})
public abstract class MixinAtmosphereSkyRenderer {
   public MixinAtmosphereSkyRenderer() {
   }

   @Inject(
      method = {"updateRenderState"},
      at = {@At("TAIL")}
   )
   private void efn$readSky(ClientWorld clientWorld, float f, Camera camera, SkyRenderState skyRenderState, CallbackInfo callbackInfo) {
      AtmosphereModule var6 = AtmosphereModule.get();
      if (var6 != null && skyRenderState.skybox == Skybox.OVERWORLD) {
         var6.extract(clientWorld, f, skyRenderState);
      }
   }

   @Inject(
      method = {"renderTopSky"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$dome(int i, CallbackInfo callbackInfo) {
      AtmosphereModule var3 = AtmosphereModule.get();
      if (var3 != null && var3.skyDisc()) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"renderTopSky"},
      at = {@At("TAIL")}
   )
   private void efn$afterDome(int i, CallbackInfo callbackInfo) {
      AtmosphereModule var3 = AtmosphereModule.get();
      if (var3 != null) {
         var3.afterSkyDisc();
      }
   }

   @Inject(
      method = {"renderGlowingSky"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$noSunriseFan(MatrixStack matrixStack, float f, int i, CallbackInfo callbackInfo) {
      AtmosphereModule var5 = AtmosphereModule.get();
      if (var5 != null && var5.replacesSunrise()) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"renderStars"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$stars(float f, MatrixStack matrixStack, CallbackInfo callbackInfo) {
      AtmosphereModule var4 = AtmosphereModule.get();
      if (var4 != null && var4.stars(matrixStack)) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"renderCelestialBodies"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/SkyRendering;renderMoon(Lnet/minecraft/world/MoonPhase;FLnet/minecraft/client/util/math/MatrixStack;)V"
      )}
   )
   private void efn$beforeMoon(MatrixStack matrixStack, float f, float g, float h, MoonPhase moonPhase, float i, float j, CallbackInfo callbackInfo) {
      AtmosphereModule var9 = AtmosphereModule.get();
      if (var9 != null) {
         var9.beforeMoon(matrixStack);
      }
   }

   @Inject(
      method = {"renderCelestialBodies"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/SkyRendering;renderMoon(Lnet/minecraft/world/MoonPhase;FLnet/minecraft/client/util/math/MatrixStack;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$afterMoon(MatrixStack matrixStack, float f, float g, float h, MoonPhase moonPhase, float i, float j, CallbackInfo callbackInfo) {
      AtmosphereModule var9 = AtmosphereModule.get();
      if (var9 != null) {
         var9.afterMoon(matrixStack);
      }
   }

   @Inject(
      method = {"renderCelestialBodies"},
      at = {@At("TAIL")}
   )
   private void efn$afterCelestials(MatrixStack matrixStack, float f, float g, float h, MoonPhase moonPhase, float i, float j, CallbackInfo callbackInfo) {
      AtmosphereModule var9 = AtmosphereModule.get();
      if (var9 != null) {
         var9.afterCelestials();
      }

      SkyLyrics.render();
   }

   @Inject(
      method = {"renderEndSky"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$endSky(CallbackInfo callbackInfo) {
      AtmosphereModule var2 = AtmosphereModule.get();
      if (var2 != null && var2.endSky()) {
         callbackInfo.cancel();
      }
   }
}
