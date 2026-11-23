package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinElytraFlyLivingEntity {
   public MixinElytraFlyLivingEntity() {
   }

   @ModifyReturnValue(
      method = {"method_6128"},
      at = {@At("RETURN")}
   )
   private boolean efn$elytraFlyIsFallFlying(boolean var1) {
      ElytraFlyModule var2 = ElytraFlyModule.active;
      if (var2 == null) {
         return var1;
      } else {
         MinecraftClient var3 = MinecraftClient.method_1551();
         ClientPlayerEntity var4 = var3.field_1724;
         return this == var4 && var3.method_18854() ? var2.modifyFallFlying(var4, var1) : var1;
      }
   }
}
