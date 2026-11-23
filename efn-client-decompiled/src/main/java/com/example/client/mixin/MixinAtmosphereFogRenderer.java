package com.example.client.mixin;

import net.efn.client.module.impl.render.AtmosphereModule;
import net.efn.client.module.impl.render.PeepWorld;
import net.efn.client.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({FogRenderer.class})
public abstract class MixinAtmosphereFogRenderer {
   public MixinAtmosphereFogRenderer() {
   }

   @Inject(
      method = {"getFogColor"},
      at = {@At("RETURN")}
   )
   private void efn$fogColour(Camera camera, float f, ClientWorld clientWorld, int i, float g, CallbackInfoReturnable<Vector4f> callbackInfoReturnable) {
      CameraSubmersionType var8 = camera.getSubmersionType();
      if (var8 == CameraSubmersionType.WATER) {
         PinkWater.underwater((Vector4f)callbackInfoReturnable.getReturnValue());
      }

      if (var8 == CameraSubmersionType.NONE && !(g > 0.0F)) {
         Entity var9 = camera.getFocusedEntity();
         LivingEntity var7;
         if (!(var9 instanceof LivingEntity)
            || !(var7 = (LivingEntity)var9).hasStatusEffect(StatusEffects.BLINDNESS) && !var7.hasStatusEffect(StatusEffects.DARKNESS)) {
            AtmosphereModule var10 = AtmosphereModule.get();
            if (var10 != null) {
               var10.fog(camera, clientWorld, (Vector4f)callbackInfoReturnable.getReturnValue());
            }

            PeepWorld.fog((Vector4f)callbackInfoReturnable.getReturnValue());
         }
      }
   }
}
