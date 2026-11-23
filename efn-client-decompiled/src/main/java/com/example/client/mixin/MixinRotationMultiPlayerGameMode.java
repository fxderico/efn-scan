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
   private MinecraftClient field_3712;

   public MixinRotationMultiPlayerGameMode() {
   }

   @WrapMethod(
      method = {"method_2919"}
   )
   private ActionResult efn$useItemRotation(PlayerEntity var1, Hand var2, Operation<ActionResult> var3) {
      return this.efn$asSent(var1, () -> (ActionResult)var3.call(new Object[]{var1, var2}));
   }

   @WrapMethod(
      method = {"method_2897"}
   )
   private void efn$releaseRotation(PlayerEntity var1, Operation<Void> var2) {
      this.efn$asSent(var1, () -> (Void)var2.call(new Object[]{var1}));
   }

   private <T> T efn$asSent(PlayerEntity var1, Supplier<T> var2) {
      ClientPlayerEntity var3 = this.field_3712.field_1724;
      if (var1 == var3 && !ServerRotation.isSwapped()) {
         Rotation var4 = ServerRotation.commit(var3);
         float var5 = var3.method_36454();
         float var6 = var3.method_36455();
         if (var4.yaw() == var5 && var4.pitch() == var6) {
            return (T)var2.get();
         } else {
            var3.method_36456(var4.yaw());
            var3.method_36457(var4.pitch());

            Object var8;
            try {
               Object var7 = var2.get();
               var8 = var7;
            } finally {
               var3.method_36456(var5);
               var3.method_36457(var6);
            }

            return (T)var8;
         }
      } else {
         return (T)var2.get();
      }
   }

   @Inject(
      method = {"method_2918"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 950
   )
   private void efn$attackRotationGate(PlayerEntity var1, Entity var2, CallbackInfo var3) {
      ClientPlayerEntity var4 = this.field_3712.field_1724;
      if (var1 == var4) {
         Box var5 = var2.method_5829();
         double var6 = var4.method_55755();
         AttackRangeComponent var8 = (AttackRangeComponent)var4.method_6047().method_58694(DataComponentTypes.field_64680);
         if (var8 != null) {
            var5 = var5.method_1014(var8.comp_5262());
            var6 = var8.method_76739(var4);
         }

         if (ServerRotation.misses(var4, var5, var6)) {
            var1.method_7350();
            var3.cancel();
         }
      }
   }

   @Inject(
      method = {"method_2905"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 950
   )
   private void efn$interactRotationGate(PlayerEntity var1, Entity var2, Hand var3, CallbackInfoReturnable<ActionResult> var4) {
      ClientPlayerEntity var5 = this.field_3712.field_1724;
      if (var1 == var5 && ServerRotation.misses(var5, var2.method_5829(), var5.method_55755())) {
         var4.setReturnValue(ActionResult.field_5811);
      }
   }

   @Inject(
      method = {"method_2917"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 950
   )
   private void efn$interactAtRotationGate(PlayerEntity var1, Entity var2, EntityHitResult var3, Hand var4, CallbackInfoReturnable<ActionResult> var5) {
      ClientPlayerEntity var6 = this.field_3712.field_1724;
      if (var1 == var6 && ServerRotation.misses(var6, var2.method_5829(), var6.method_55755())) {
         var5.setReturnValue(ActionResult.field_5811);
      }
   }

   @Inject(
      method = {"method_2896"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 950
   )
   private void efn$useItemOnRotationGate(ClientPlayerEntity var1, Hand var2, BlockHitResult var3, CallbackInfoReturnable<ActionResult> var4) {
      if (var1 == this.field_3712.field_1724 && ServerRotation.missesBlock(var1, new Box(var3.method_17777()), var1.method_55754())) {
         var4.setReturnValue(ActionResult.field_5814);
      }
   }

   @Inject(
      method = {"method_2910"},
      at = {@At("HEAD")},
      cancellable = true,
      order = 950
   )
   private void efn$startDestroyRotationGate(BlockPos var1, Direction var2, CallbackInfoReturnable<Boolean> var3) {
      ClientPlayerEntity var4 = this.field_3712.field_1724;
      if (var4 != null && ServerRotation.missesBlock(var4, new Box(var1), var4.method_55754())) {
         var3.setReturnValue(false);
      }
   }
}
