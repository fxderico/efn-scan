package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.misc.SpeedMineModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinSpeedMineMultiPlayerGameMode {
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   private int blockBreakingCooldown;

   public MixinSpeedMineMultiPlayerGameMode() {
   }

   @Inject(
      method = {"updateBlockBreakingProgress"},
      at = {@At("HEAD")}
   )
   private void efn$speedMineDelay(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      ClientPlayerEntity var4 = this.client.player;
      if (this.blockBreakingCooldown > 0 && var4 != null && this.client.world != null && !var4.getAbilities().creativeMode) {
         BlockState var5 = this.client.world.getBlockState(blockPos);
         if (SpeedMineModule.dropsDelay(var5, var5.calcBlockBreakingDelta(var4, this.client.world, blockPos))) {
            this.blockBreakingCooldown = 0;
         }
      }
   }

   @WrapOperation(
      method = {"updateBlockBreakingProgress"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F"
      )}
   )
   private float efn$speedMineProgress(BlockState blockState, PlayerEntity playerEntity, BlockView blockView, BlockPos blockPos, Operation<Float> operation) {
      return SpeedMineModule.scaleProgress(blockState, (Float)operation.call(blockState, playerEntity, blockView, blockPos));
   }
}
