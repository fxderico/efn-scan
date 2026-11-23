package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.movement.JesusModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinJesusLivingEntity {
   public MixinJesusLivingEntity() {
   }

   @ModifyReturnValue(
      method = {"method_26319"},
      at = {@At("RETURN")}
   )
   private boolean efn$jesusCanStandOnFluid(boolean var1, FluidState var2) {
      if (var1) {
         return true;
      } else {
         JesusModule var3 = JesusModule.active;
         return var3 != null && this == MinecraftClient.method_1551().field_1724 ? var3.canWalkOnFluid(var2) : false;
      }
   }
}
