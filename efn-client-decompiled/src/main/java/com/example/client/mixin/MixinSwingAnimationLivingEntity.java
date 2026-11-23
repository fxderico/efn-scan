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
      method = {"method_6119", "method_23667"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1309;method_6028()I"
      )}
   )
   private int efn$swingTempo(int var1) {
      SwingAnimationModule var2 = SwingAnimationModule.active;
      return var2 != null && this == MinecraftClient.method_1551().field_1724 ? var2.swingTicks(var1) : var1;
   }

   @Inject(
      method = {"method_23667"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/class_1309;field_6252:Z",
         opcode = 181
      )}
   )
   private void efn$latchSwing(Hand var1, boolean var2, CallbackInfo var3) {
      SwingAnimationModule var4 = SwingAnimationModule.active;
      if (var4 != null && this == MinecraftClient.method_1551().field_1724) {
         var4.latch();
      }
   }
}
