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
      method = {"method_53875"},
      at = {@At("HEAD")}
   )
   private void efn$onEntityAdd(Entity var1, CallbackInfo var2) {
      WorldEntityAddEvent.dispatch(var1);
   }

   @Inject(
      method = {"method_8413"},
      at = {@At("HEAD")}
   )
   private void efn$onBlockUpdated(BlockPos var1, BlockState var2, BlockState var3, int var4, CallbackInfo var5) {
      BlockUpdateEvent.dispatch(var1, var2, var3);
   }
}
