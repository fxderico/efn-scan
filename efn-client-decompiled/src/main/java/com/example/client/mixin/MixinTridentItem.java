package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.misc.TridentUtilModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({TridentItem.class})
public abstract class MixinTridentItem {
   public MixinTridentItem() {
   }

   @ModifyExpressionValue(
      method = {"use", "onStoppedUsing"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z"
      )},
      require = 0
   )
   private boolean efn$allowOutOfWater(boolean bl) {
      return bl || TridentUtilModule.allowOutOfWater();
   }

   @ModifyExpressionValue(
      method = {"onStoppedUsing"},
      at = {@At(
         value = "CONSTANT",
         args = {"intValue=10"}
      )},
      require = 0
   )
   private int efn$scaleMinCharge(int i) {
      return TridentUtilModule.minChargeTicks(i);
   }
}
