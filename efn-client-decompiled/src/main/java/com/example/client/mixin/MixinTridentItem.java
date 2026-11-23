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
      method = {"method_7836", "method_7840"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1657;method_5721()Z"
      )},
      require = 0
   )
   private boolean efn$allowOutOfWater(boolean var1) {
      return var1 || TridentUtilModule.allowOutOfWater();
   }

   @ModifyExpressionValue(
      method = {"method_7840"},
      at = {@At(
         value = "CONSTANT",
         args = {"intValue=10"}
      )},
      require = 0
   )
   private int efn$scaleMinCharge(int var1) {
      return TridentUtilModule.minChargeTicks(var1);
   }
}
