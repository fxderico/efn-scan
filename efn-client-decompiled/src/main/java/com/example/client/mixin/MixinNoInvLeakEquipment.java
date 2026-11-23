package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinNoInvLeakEquipment {
   public MixinNoInvLeakEquipment() {
   }

   @ModifyReturnValue(
      method = {"method_6118"},
      at = {@At("RETURN")}
   )
   private ItemStack efn$noInvLeakBareModel(ItemStack var1) {
      return NoInvLeakModule.modelItem((LivingEntity)this, var1);
   }
}
