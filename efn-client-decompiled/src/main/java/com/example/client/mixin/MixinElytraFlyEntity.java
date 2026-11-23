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
      method = {"method_18376"},
      at = {@At("RETURN")}
   )
   private EntityPose efn$elytraFlyGetPose(EntityPose var1) {
      ElytraFlyModule var2 = ElytraFlyModule.active;
      if (var2 == null) {
         return var1;
      } else {
         MinecraftClient var3 = MinecraftClient.method_1551();
         ClientPlayerEntity var4 = var3.field_1724;
         if (this == var4 && var3.method_18854()) {
            return var2.canPacketEfly(var4) ? EntityPose.field_18077 : var1;
         } else {
            return var1;
         }
      }
   }
}
