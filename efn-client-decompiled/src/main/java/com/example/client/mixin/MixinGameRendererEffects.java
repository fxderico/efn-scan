package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({GameRenderer.class})
public abstract class MixinGameRendererEffects {
   public MixinGameRendererEffects() {
   }

   @ModifyExpressionValue(
      method = {"renderHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/option/Perspective;isFirstPerson()Z"
      )}
   )
   private boolean efn$freecamHands(boolean bl) {
      FreecamModule var2 = FreecamModule.active;
      return var2 != null && var2.keepHands() || bl;
   }

   @Inject(
      method = {"showFloatingItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$cancelTotemAnimation(ItemStack itemStack, CallbackInfo callbackInfo) {
      if (NoRenderModule.hideTotemAnimation()) {
         callbackInfo.cancel();
      }
   }

   @WrapOperation(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/lang/Math;max(FF)F"
      )}
   )
   private float efn$cancelNauseaWobble(float f, float g, Operation<Float> operation) {
      return NoRenderModule.hideNauseaPortal() ? 0.0F : (Float)operation.call(f, g);
   }
}
