package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.efn.client.module.impl.movement.EntityControlModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({MobEntity.class})
public abstract class MixinEntityControlMob {
   public MixinEntityControlMob() {
   }

   @ModifyReturnValue(
      method = {"method_66672"},
      at = {@At("RETURN")}
   )
   private boolean efn$entityControlSpoofSaddle(boolean var1) {
      return var1 || EntityControlModule.spoofSaddle();
   }
}
