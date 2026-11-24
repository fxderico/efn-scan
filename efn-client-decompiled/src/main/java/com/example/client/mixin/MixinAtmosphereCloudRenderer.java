package com.example.client.mixin;

import net.efn.client.module.impl.render.AtmosphereModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin({CloudRenderer.class})
public abstract class MixinAtmosphereCloudRenderer {
   public MixinAtmosphereCloudRenderer() {
   }

   @ModifyVariable(
      method = {"renderClouds"},
      at = {@At("HEAD")},
      argsOnly = true,
      ordinal = 0
   )
   private int efn$cloudColour(int i) {
      AtmosphereModule var2 = AtmosphereModule.get();
      return var2 != null ? var2.clouds(i) : i;
   }
}
