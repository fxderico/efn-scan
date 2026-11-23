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
      method = {"method_72914"},
      at = {@At("RETURN")}
   )
   private void efn$hideEntityFire(Entity var1, float var2, CallbackInfoReturnable<EntityRenderState> var3) {
      EntityRenderState var4 = (EntityRenderState)var3.getReturnValue();
      if (var4 != null && NoRenderModule.hideEntityFire()) {
         var4.field_53335 = false;
      }
   }
}
