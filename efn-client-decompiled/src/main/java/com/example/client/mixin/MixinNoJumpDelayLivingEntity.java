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
   private int field_6228;

   public MixinNoJumpDelayLivingEntity() {
   }

   @WrapOperation(
      method = {"method_6007"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/class_1309;field_6228:I",
         opcode = 181
      )}
   )
   private void efn$noJumpDelay(LivingEntity var1, int var2, Operation<Void> var3) {
      NoJumpDelayModule var4 = NoJumpDelayModule.active;
      if (var4 != null && var2 > this.field_6228 && var1 == MinecraftClient.method_1551().field_1724) {
         var2 = var4.rearm(var2);
      }

      var3.call(new Object[]{var1, var2});
   }
}
