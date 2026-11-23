package com.example.client.mixin;

import net.efn.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderTickCounter.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({Dynamic.class})
public abstract class MixinSpeedDeltaTrackerTimer {
   @Shadow
   private float field_51958;

   public MixinSpeedDeltaTrackerTimer() {
   }

   @Inject(
      method = {"method_60639"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/class_9779$class_9781;field_51962:J",
         opcode = 181
      )}
   )
   private void efn$speedTimer(long var1, CallbackInfoReturnable<Integer> var3) {
      float var4 = SpeedModule.timerMultiplier;
      if (var4 != 1.0F) {
         this.field_51958 *= var4;
      }
   }
}
