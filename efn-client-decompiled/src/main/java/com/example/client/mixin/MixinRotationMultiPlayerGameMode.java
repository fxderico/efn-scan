package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.function.Supplier;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinRotationMultiPlayerGameMode {
   @Shadow
   @Final
   private MinecraftClient client;

   public MixinRotationMultiPlayerGameMode() {
   }

   @WrapMethod(
      method = {"interactItem"}
   )
   private ActionResult efn$useItemRotation(PlayerEntity playerEntity, Hand hand, Operation<ActionResult> operation) {
      return this.efn$asSent(playerEntity, () -> (ActionResult)operation.call(playerEntity, hand));
   }

   @WrapMethod(
      method = {"stopUsingItem"}
   )
   private void efn$releaseRotation(PlayerEntity playerEntity, Operation<Void> operation) {
      this.efn$asSent(playerEntity, () -> (Void)operation.call(playerEntity));
   }

   private <T> T efn$asSent(PlayerEntity playerEntity, Supplier<T> supplier) {
      ClientPlayerEntity var3 = this.client.player;
      if (playerEntity == var3 && !ServerRotation.isSwapped()) {
         Rotation var4 = ServerRotation.commit(var3);
         float var5 = var3.getYaw();
         float var6 = var3.getPitch();
         if (var4.yaw() == var5 && var4.pitch() == var6) {
            return (T)supplier.get();
         } else {
            var3.setYaw(var4.yaw());
            var3.setPitch(var4.pitch());

            Object var8;
            try {
               Object var7 = supplier.get();
               var8 = var7;
            } finally {
               var3.setYaw(var5);
               var3.setPitch(var6);
            }

            return (T)var8;
         }
      } else {
         return (T)supplier.get();
      }
   }

   @Inject(
      method = {"attackEntity"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 950
   )
   private void efn$attackRotationGate(PlayerEntity playerEntity, Entity entity, CallbackInfo callbackInfo) {
      ClientPlayerEntity var4 = this.client.player;
      if (playerEntity == var4) {
         Box var5 = entity.getBoundingBox();
         double var6 = var4.getEntityInteractionRange();
         AttackRangeComponent var8 = var4.getMainHandStack().get(DataComponentTypes.ATTACK_RANGE);
         if (var8 != null) {
            var5 = var5.expand(var8.hitboxMargin());
            var6 = var8.getEffectiveMaxRange(var4);
         }

         if (ServerRotation.misses(var4, var5, var6)) {
            playerEntity.resetTicksSince();
            callbackInfo.cancel();
         }
      }
   }

   @Inject(
      method = {"interactEntity"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 950
   )
   private void efn$interactRotationGate(PlayerEntity playerEntity, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> callbackInfoReturnable) {
      ClientPlayerEntity var5 = this.client.player;
      if (playerEntity == var5 && ServerRotation.misses(var5, entity.getBoundingBox(), var5.getEntityInteractionRange())) {
         callbackInfoReturnable.setReturnValue(ActionResult.PASS);
      }
   }

   @Inject(
      method = {"interactEntityAtLocation"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 950
   )
   private void efn$interactAtRotationGate(
      PlayerEntity playerEntity, Entity entity, EntityHitResult entityHitResult, Hand hand, CallbackInfoReturnable<ActionResult> callbackInfoReturnable
   ) {
      ClientPlayerEntity var6 = this.client.player;
      if (playerEntity == var6 && ServerRotation.misses(var6, entity.getBoundingBox(), var6.getEntityInteractionRange())) {
         callbackInfoReturnable.setReturnValue(ActionResult.PASS);
      }
   }

   @Inject(
      method = {"interactBlock"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 950
   )
   private void efn$useItemOnRotationGate(
      ClientPlayerEntity clientPlayerEntity, Hand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> callbackInfoReturnable
   ) {
      if (clientPlayerEntity == this.client.player
         && ServerRotation.missesBlock(clientPlayerEntity, new Box(blockHitResult.getBlockPos()), clientPlayerEntity.getBlockInteractionRange())) {
         callbackInfoReturnable.setReturnValue(ActionResult.FAIL);
      }
   }

   @Inject(
      method = {"attackBlock"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 950
   )
   private void efn$startDestroyRotationGate(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      ClientPlayerEntity var4 = this.client.player;
      if (var4 != null && ServerRotation.missesBlock(var4, new Box(blockPos), var4.getBlockInteractionRange())) {
         callbackInfoReturnable.setReturnValue(false);
      }
   }
}
