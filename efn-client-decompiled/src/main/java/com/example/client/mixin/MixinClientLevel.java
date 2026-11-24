package com.example.client.mixin;

import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.WorldEntityAddEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientWorld.class})
public abstract class MixinClientLevel {
   public MixinClientLevel() {
   }

   @Inject(
      method = {"addEntity"},
      at = {@At("HEAD")}
   )
   private void efn$onEntityAdd(Entity entity, CallbackInfo callbackInfo) {
      WorldEntityAddEvent.dispatch(entity);
   }

   @Inject(
      method = {"updateListeners"},
      at = {@At("HEAD")}
   )
   private void efn$onBlockUpdated(BlockPos blockPos, BlockState blockState, BlockState blockState2, int i, CallbackInfo callbackInfo) {
      BlockUpdateEvent.dispatch(blockPos, blockState, blockState2);
   }
}
