package com.example.client.mixin;

import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({WorldRenderer.class})
public abstract class MixinNoRenderEntityFire {
   public MixinNoRenderEntityFire() {
   }

   @Inject(
      method = {"getAndUpdateRenderState"},
      at = {@At("RETURN")}
   )
   private void efn$hideEntityFire(Entity entity, float f, CallbackInfoReturnable<EntityRenderState> callbackInfoReturnable) {
      EntityRenderState var4 = (EntityRenderState)callbackInfoReturnable.getReturnValue();
      if (var4 != null && NoRenderModule.hideEntityFire()) {
         var4.onFire = false;
      }
   }
}
