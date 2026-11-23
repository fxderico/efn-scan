package com.example.client.mixin;

import net.efn.client.module.impl.render.AtmosphereModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({AtmosphericFogModifier.class})
public abstract class MixinAtmosphereRainFog {
   @Shadow
   private float field_60589;

   public MixinAtmosphereRainFog() {
   }

   @Inject(
      method = {"method_76304"},
      at = {@At("TAIL")}
   )
   private void efn$keepClearInRain(Camera var1, ClientWorld var2, RenderTickCounter var3, CallbackInfo var4) {
      AtmosphereModule var5 = AtmosphereModule.get();
      if (var5 != null && var5.keepsSkyInRain()) {
         this.field_60589 = 0.0F;
      }
   }
}
