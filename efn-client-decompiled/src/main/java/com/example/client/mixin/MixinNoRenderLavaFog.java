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
      method = {"method_42591"},
      at = {@At("TAIL")}
   )
   private void efn$clearLavaFog(FogData var1, Camera var2, ClientWorld var3, float var4, RenderTickCounter var5, CallbackInfo var6) {
      if (NoRenderModule.hideLavaFog()) {
         var1.field_60582 = -8.0F;
         var1.field_60584 = var4;
         var1.field_60099 = var4;
         var1.field_60100 = var4;
      }
   }
}
