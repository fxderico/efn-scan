package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.efn.client.module.impl.movement.SneakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerEntity.class})
public abstract class MixinSneakLocalPlayer {
   public MixinSneakLocalPlayer() {
   }

   @ModifyExpressionValue(
      method = {"method_5773"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/class_744;field_54155:Lnet/minecraft/class_10185;",
         opcode = 180
      )},
      require = 3,
      allow = 3
   )
   private PlayerInput efn$packetSneak(PlayerInput var1) {
      SneakModule var2 = SneakModule.active;
      return var2 != null && !var1.comp_3164() && var2.doPacket()
         ? new PlayerInput(var1.comp_3159(), var1.comp_3160(), var1.comp_3161(), var1.comp_3162(), var1.comp_3163(), true, var1.comp_3165())
         : var1;
   }
}
