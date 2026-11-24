package com.example.client.mixin;

import net.efn.client.module.impl.render.JumpCirclesModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinJumpCirclesLivingEntity {
   public MixinJumpCirclesLivingEntity() {
   }

   @Inject(
      method = {"jump"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V"
      )}
   )
   private void efn$jumpCircle(CallbackInfo callbackInfo) {
      if (JumpCirclesModule.active != null) {
         JumpCirclesModule.onJump((LivingEntity)this);
      }
   }
}
