package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.movement.JesusModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({PowderSnowBlock.class})
public abstract class MixinJesusPowderSnowBlock {
   public MixinJesusPowderSnowBlock() {
   }

   @ModifyReturnValue(
      method = {"method_32355"},
      at = {@At("RETURN")}
   )
   private static boolean efn$jesusWalkOnPowderSnow(boolean var0, Entity var1) {
      if (var0) {
         return true;
      } else {
         JesusModule var2 = JesusModule.active;
         return var2 != null && var1 == MinecraftClient.method_1551().field_1724 && var2.canWalkOnPowderSnow();
      }
   }
}
