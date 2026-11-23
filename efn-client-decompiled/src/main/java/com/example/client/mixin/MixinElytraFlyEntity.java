package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({Entity.class})
public abstract class MixinElytraFlyEntity {
   public MixinElytraFlyEntity() {
   }

   @ModifyReturnValue(
      method = {"getPose"},
      at = {@At("RETURN")}
   )
   private EntityPose efn$elytraFlyGetPose(EntityPose entityPose) {
      ElytraFlyModule var2 = ElytraFlyModule.active;
      if (var2 == null) {
         return entityPose;
      } else {
         MinecraftClient var3 = MinecraftClient.getInstance();
         ClientPlayerEntity var4 = var3.player;
         if (this == var4 && var3.isOnThread()) {
            return var2.canPacketEfly(var4) ? EntityPose.GLIDING : entityPose;
         } else {
            return entityPose;
         }
      }
   }
}
