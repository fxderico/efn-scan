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
   private int blockBreakingCooldown;

   public MixinAntiGhostBlockMultiPlayerGameMode() {
   }

   @Shadow
   public abstract void cancelBlockBreaking();

   @Inject(
      method = {"breakBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$holdBreak(BlockPos blockPos, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (AntiGhostBlockModule.holdBreak(blockPos)) {
         callbackInfoReturnable.setReturnValue(false);
      }
   }

   @Inject(
      method = {"attackBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$waitBeforeStart(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (AntiGhostBlockModule.waiting(blockPos)) {
         this.cancelBlockBreaking();
         callbackInfoReturnable.setReturnValue(false);
      }
   }

   @Inject(
      method = {"updateBlockBreakingProgress"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 1100
   )
   private void efn$waitBeforeContinue(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (AntiGhostBlockModule.waiting(blockPos)) {
         if (this.blockBreakingCooldown > 0) {
            this.blockBreakingCooldown--;
         }

         this.cancelBlockBreaking();
         callbackInfoReturnable.setReturnValue(false);
      }
   }
}
