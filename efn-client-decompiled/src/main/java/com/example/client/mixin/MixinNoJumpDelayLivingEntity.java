package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.movement.NoJumpDelayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinNoJumpDelayLivingEntity {
   @Shadow
   private int jumpingCooldown;

   public MixinNoJumpDelayLivingEntity() {
   }

   @WrapOperation(
      method = {"tickMovement"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/entity/LivingEntity;jumpingCooldown:I",
         opcode = 181
      )}
   )
   private void efn$noJumpDelay(LivingEntity livingEntity, int i, Operation<Void> operation) {
      NoJumpDelayModule var4 = NoJumpDelayModule.active;
      if (var4 != null && i > this.jumpingCooldown && livingEntity == MinecraftClient.getInstance().player) {
         i = var4.rearm(i);
      }

      operation.call(livingEntity, i);
   }
}
