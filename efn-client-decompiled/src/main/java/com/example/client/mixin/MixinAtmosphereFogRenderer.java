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
      method = {"method_62185"},
      at = {@At("RETURN")}
   )
   private void efn$fogColour(Camera var1, float var2, ClientWorld var3, int var4, float var5, CallbackInfoReturnable<Vector4f> var6) {
      CameraSubmersionType var8 = var1.method_19334();
      if (var8 == CameraSubmersionType.field_27886) {
         PinkWater.underwater((Vector4f)var6.getReturnValue());
      }

      if (var8 == CameraSubmersionType.field_27888 && !(var5 > 0.0F)) {
         Entity var9 = var1.method_19331();
         LivingEntity var7;
         if (!(var9 instanceof LivingEntity)
            || !(var7 = (LivingEntity)var9).method_6059(StatusEffects.field_5919) && !var7.method_6059(StatusEffects.field_38092)) {
            AtmosphereModule var10 = AtmosphereModule.get();
            if (var10 != null) {
               var10.fog(var1, var3, (Vector4f)var6.getReturnValue());
            }

            PeepWorld.fog((Vector4f)var6.getReturnValue());
         }
      }
   }
}
