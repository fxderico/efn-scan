package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.efn.client.module.impl.movement.TridentBoostModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({TridentItem.class})
public abstract class MixinTridentBoostTridentItem {
   public MixinTridentBoostTridentItem() {
   }

   @WrapOperation(
      method = {"onStoppedUsing"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;addVelocity(DDD)V"
      )},
      require = 0
   )
   private void efn$tridentBoostPush(PlayerEntity playerEntity, double d, double e, double f, Operation<Void> operation) {
      double var9 = TridentBoostModule.riptideMultiplier();
      operation.call(playerEntity, d * var9, e * var9, f * var9);
   }

   @ModifyExpressionValue(
      method = {"use"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z"
      )},
      require = 0
   )
   private boolean efn$tridentBoostUseOutOfWater(boolean bl) {
      return bl || TridentBoostModule.allowOutOfWater();
   }

   @ModifyExpressionValue(
      method = {"onStoppedUsing"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z"
      )},
      require = 0
   )
   private boolean efn$tridentBoostReleaseOutOfWater(boolean bl) {
      return bl || TridentBoostModule.allowOutOfWater();
   }
}
