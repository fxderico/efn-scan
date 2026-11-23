package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.render.SwingAnimationModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinSwingAnimationLivingEntity {
   public MixinSwingAnimationLivingEntity() {
   }

   @ModifyExpressionValue(
      method = {"tickHandSwing", "swingHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getHandSwingDuration()I"
      )}
   )
   private int efn$swingTempo(int i) {
      SwingAnimationModule var2 = SwingAnimationModule.active;
      return var2 != null && this == MinecraftClient.getInstance().player ? var2.swingTicks(i) : i;
   }

   @Inject(
      method = {"swingHand"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/entity/LivingEntity;handSwinging:Z",
         opcode = 181
      )}
   )
   private void efn$latchSwing(Hand hand, boolean bl, CallbackInfo callbackInfo) {
      SwingAnimationModule var4 = SwingAnimationModule.active;
      if (var4 != null && this == MinecraftClient.getInstance().player) {
         var4.latch();
      }
   }
}
