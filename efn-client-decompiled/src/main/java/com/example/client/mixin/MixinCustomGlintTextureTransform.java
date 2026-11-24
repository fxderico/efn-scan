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
      method = {"getGlintTransformation"},
      at = {@At("HEAD")},
      argsOnly = true
   )
   private static float efn$glintPatternScale(float f) {
      CustomGlintModule var1 = CustomGlintModule.active;
      return var1 == null ? f : f * var1.uvScaleMultiplier();
   }

   @ModifyExpressionValue(
      method = {"getGlintTransformation"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"
      )}
   )
   private static long efn$glintClock(long l) {
      CustomGlintModule var2 = CustomGlintModule.active;
      return var2 == null ? l : var2.glintMillis(l);
   }
}
