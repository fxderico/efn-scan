package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.movement.FlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({Entity.class})
public abstract class MixinFlyEntity {
   public MixinFlyEntity() {
   }

   @ModifyReturnValue(
      method = {"method_5799"},
      at = {@At("RETURN")}
   )
   private boolean efn$flyIsInWater(boolean var1) {
      return var1 && !this.efn$flyAffects();
   }

   @ModifyReturnValue(
      method = {"method_5771"},
      at = {@At("RETURN")}
   )
   private boolean efn$flyIsInLava(boolean var1) {
      return var1 && !this.efn$flyAffects();
   }

   @ModifyExpressionValue(
      method = {"method_5790"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_1297;method_5869()Z"
      )}
   )
   private boolean efn$flyIsUnderWater(boolean var1) {
      return var1 && !this.efn$flyAffects();
   }

   @Unique
   private boolean efn$flyAffects() {
      return FlyModule.active != null && this == MinecraftClient.method_1551().field_1724;
   }
}
