package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.efn.client.module.impl.render.CustomFovModule;
import net.efn.client.module.impl.render.ZoomModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({GameRenderer.class})
public abstract class MixinCustomFovGameRenderer {
   public MixinCustomFovGameRenderer() {
   }

   @Shadow
   public abstract boolean method_35765();

   @ModifyReturnValue(
      method = {"method_3196"},
      at = {@At("RETURN")}
   )
   private float efn$customFov(float var1, @Local(argsOnly = true) boolean var2) {
      CustomFovModule var4 = CustomFovModule.active;
      float var3 = var4 != null && !this.method_35765() ? var4.fov() : var1;
      if (!var2) {
         CustomFovModule.hudFovApplied(var1, var3);
      }

      return var2 && !this.method_35765() ? ZoomModule.fov(var3) : var3;
   }
}
