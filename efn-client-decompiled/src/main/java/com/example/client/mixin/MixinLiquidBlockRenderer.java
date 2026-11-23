package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.render.PinkWater;
import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.FluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin({FluidRenderer.class})
public abstract class MixinLiquidBlockRenderer {
   public MixinLiquidBlockRenderer() {
   }

   @ModifyArg(
      method = {"method_23072"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_4588;method_22915(FFFF)Lnet/minecraft/class_4588;"
      )},
      index = 3
   )
   private float efn$xrayFluidAlpha(float var1) {
      return XrayModule.active == null ? var1 : var1 * XrayModule.CURRENT_ALPHA.get()[0];
   }

   @ModifyReturnValue(
      method = {"method_3343"},
      at = {@At("RETURN")}
   )
   private int efn$pinkWaterGlow(int var1) {
      return PinkWater.light(var1);
   }
}
