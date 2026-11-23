package com.example.client.mixin;

import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.BlockModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin({BlockModelRenderer.class})
public abstract class MixinModelBlockRenderer {
   public MixinModelBlockRenderer() {
   }

   @ModifyArg(
      method = {"method_23073"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_4588;method_22920(Lnet/minecraft/class_4587$class_4665;Lnet/minecraft/class_777;[FFFFF[II)V"
      )},
      index = 6
   )
   private float efn$xrayAlpha(float var1) {
      return XrayModule.active == null ? var1 : var1 * XrayModule.CURRENT_ALPHA.get()[0];
   }
}
