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
      method = {"canWalkOnFluid"},
      at = {@At("RETURN")}
   )
   private boolean efn$jesusCanStandOnFluid(boolean bl, FluidState fluidState) {
      if (bl) {
         return true;
      } else {
         JesusModule var3 = JesusModule.active;
         return var3 != null && this == MinecraftClient.getInstance().player ? var3.canWalkOnFluid(fluidState) : false;
      }
   }
}
