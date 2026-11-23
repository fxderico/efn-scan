package com.example.client.mixin;

import net.efn.client.module.impl.misc.AntiGhostBlockModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinAntiGhostBlockMultiPlayerGameMode {
   @Shadow
   private int field_3716;

   public MixinAntiGhostBlockMultiPlayerGameMode() {
   }

   @Shadow
   public abstract void method_2925();

   @Inject(
      method = {"method_2899"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$holdBreak(BlockPos var1, CallbackInfoReturnable<Boolean> var2) {
      if (AntiGhostBlockModule.holdBreak(var1)) {
         var2.setReturnValue(false);
      }
   }

   @Inject(
      method = {"method_2910"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$waitBeforeStart(BlockPos var1, Direction var2, CallbackInfoReturnable<Boolean> var3) {
      if (AntiGhostBlockModule.waiting(var1)) {
         this.method_2925();
         var3.setReturnValue(false);
      }
   }

   @Inject(
      method = {"method_2902"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 1100
   )
   private void efn$waitBeforeContinue(BlockPos var1, Direction var2, CallbackInfoReturnable<Boolean> var3) {
      if (AntiGhostBlockModule.waiting(var1)) {
         if (this.field_3716 > 0) {
            this.field_3716--;
         }

         this.method_2925();
         var3.setReturnValue(false);
      }
   }
}
