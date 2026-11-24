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
      method = {"isGliding"},
      at = {@At("RETURN")}
   )
   private boolean efn$elytraFlyIsFallFlying(boolean bl) {
      ElytraFlyModule var2 = ElytraFlyModule.active;
      if (var2 == null) {
         return bl;
      } else {
         MinecraftClient var3 = MinecraftClient.getInstance();
         ClientPlayerEntity var4 = var3.player;
         return this == var4 && var3.isOnThread() ? var2.modifyFallFlying(var4, bl) : bl;
      }
   }
}
