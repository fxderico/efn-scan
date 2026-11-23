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
      method = {"method_74926"},
      at = {@At("TAIL")}
   )
   private void efn$readSky(ClientWorld var1, float var2, Camera var3, SkyRenderState var4, CallbackInfo var5) {
      AtmosphereModule var6 = AtmosphereModule.get();
      if (var6 != null && var4.field_64464 == Skybox.field_64386) {
         var6.extract(var1, var2, var4);
      }
   }

   @Inject(
      method = {"method_62302"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$dome(int var1, CallbackInfo var2) {
      AtmosphereModule var3 = AtmosphereModule.get();
      if (var3 != null && var3.skyDisc()) {
         var2.cancel();
      }
   }

   @Inject(
      method = {"method_62302"},
      at = {@At("TAIL")}
   )
   private void efn$afterDome(int var1, CallbackInfo var2) {
      AtmosphereModule var3 = AtmosphereModule.get();
      if (var3 != null) {
         var3.afterSkyDisc();
      }
   }

   @Inject(
      method = {"method_62306"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$noSunriseFan(MatrixStack var1, float var2, int var3, CallbackInfo var4) {
      AtmosphereModule var5 = AtmosphereModule.get();
      if (var5 != null && var5.replacesSunrise()) {
         var4.cancel();
      }
   }

   @Inject(
      method = {"method_62310"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$stars(float var1, MatrixStack var2, CallbackInfo var3) {
      AtmosphereModule var4 = AtmosphereModule.get();
      if (var4 != null && var4.stars(var2)) {
         var3.cancel();
      }
   }

   @Inject(
      method = {"method_62307"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_9975;method_62304(Lnet/minecraft/class_12131;FLnet/minecraft/class_4587;)V"
      )}
   )
   private void efn$beforeMoon(MatrixStack var1, float var2, float var3, float var4, MoonPhase var5, float var6, float var7, CallbackInfo var8) {
      AtmosphereModule var9 = AtmosphereModule.get();
      if (var9 != null) {
         var9.beforeMoon(var1);
      }
   }

   @Inject(
      method = {"method_62307"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_9975;method_62304(Lnet/minecraft/class_12131;FLnet/minecraft/class_4587;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$afterMoon(MatrixStack var1, float var2, float var3, float var4, MoonPhase var5, float var6, float var7, CallbackInfo var8) {
      AtmosphereModule var9 = AtmosphereModule.get();
      if (var9 != null) {
         var9.afterMoon(var1);
      }
   }

   @Inject(
      method = {"method_62307"},
      at = {@At("TAIL")}
   )
   private void efn$afterCelestials(MatrixStack var1, float var2, float var3, float var4, MoonPhase var5, float var6, float var7, CallbackInfo var8) {
      AtmosphereModule var9 = AtmosphereModule.get();
      if (var9 != null) {
         var9.afterCelestials();
      }

      SkyLyrics.render();
   }

   @Inject(
      method = {"method_62312"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$endSky(CallbackInfo var1) {
      AtmosphereModule var2 = AtmosphereModule.get();
      if (var2 != null && var2.endSky()) {
         var1.cancel();
      }
   }
}
