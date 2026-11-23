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
      method = {"method_7840"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1657;method_5762(DDD)V"
      )},
      require = 0
   )
   private void efn$tridentBoostPush(PlayerEntity var1, double var2, double var4, double var6, Operation<Void> var8) {
      double var9 = TridentBoostModule.riptideMultiplier();
      var8.call(new Object[]{var1, var2 * var9, var4 * var9, var6 * var9});
   }

   @ModifyExpressionValue(
      method = {"method_7836"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1657;method_5721()Z"
      )},
      require = 0
   )
   private boolean efn$tridentBoostUseOutOfWater(boolean var1) {
      return var1 || TridentBoostModule.allowOutOfWater();
   }

   @ModifyExpressionValue(
      method = {"method_7840"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1657;method_5721()Z"
      )},
      require = 0
   )
   private boolean efn$tridentBoostReleaseOutOfWater(boolean var1) {
      return var1 || TridentBoostModule.allowOutOfWater();
   }
}
