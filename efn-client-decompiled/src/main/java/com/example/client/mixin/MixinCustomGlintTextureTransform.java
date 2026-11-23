package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.render.CustomGlintModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin({TextureTransform.class})
public abstract class MixinCustomGlintTextureTransform {
   public MixinCustomGlintTextureTransform() {
   }

   @ModifyVariable(
      method = {"method_76031"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private static float efn$glintPatternScale(float var0) {
      CustomGlintModule var1 = CustomGlintModule.active;
      return var1 == null ? var0 : var0 * var1.uvScaleMultiplier();
   }

   @ModifyExpressionValue(
      method = {"method_76031"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_156;method_658()J"
      )}
   )
   private static long efn$glintClock(long var0) {
      CustomGlintModule var2 = CustomGlintModule.active;
      return var2 == null ? var0 : var2.glintMillis(var0);
   }
}
