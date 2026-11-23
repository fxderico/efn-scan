package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.fog.WaterFogModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({WaterFogModifier.class})
public abstract class MixinPinkWaterFog {
   public MixinPinkWaterFog() {
   }

   @ModifyReturnValue(
      method = {"method_71654"},
      at = {@At("RETURN")}
   )
   private int efn$pinkWaterFog(int var1) {
      return PinkWater.waterFog(var1);
   }
}
