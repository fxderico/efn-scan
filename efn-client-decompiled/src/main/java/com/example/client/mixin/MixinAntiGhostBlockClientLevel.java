package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.misc.AntiGhostBlockModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientWorld.class})
public abstract class MixinAntiGhostBlockClientLevel {
   public MixinAntiGhostBlockClientLevel() {
   }

   @WrapOperation(
      method = {"handleBlockUpdate"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/PendingUpdateManager;hasPendingUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"
      )}
   )
   private boolean efn$serverAnswer(PendingUpdateManager pendingUpdateManager, BlockPos blockPos, BlockState blockState, Operation<Boolean> operation) {
      boolean var5 = (Boolean)operation.call(pendingUpdateManager, blockPos, blockState);
      ClientWorld var6 = (ClientWorld)this;
      return var5 && !AntiGhostBlockModule.serverAnswered(var6, blockPos);
   }

   @Inject(
      method = {"handlePlayerActionResponse"},
      at = {@At("TAIL")}
   )
   private void efn$acknowledged(int i, CallbackInfo callbackInfo) {
      ClientWorld var3 = (ClientWorld)this;
      AntiGhostBlockModule.acknowledged(var3, i);
   }
}
