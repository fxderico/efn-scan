package com.example.client.mixin;

import net.efn.client.module.impl.movement.SneakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin({PlayerInteractEntityC2SPacket.class})
public abstract class MixinSneakServerboundInteractPacket {
   public MixinSneakServerboundInteractPacket() {
   }

   @ModifyVariable(
      method = {"<init>(IZLnet/minecraft/class_2824$class_5906;)V"},
      at = {@At("HEAD")},
      ordinal = 0,
      argsOnly = true
   )
   private static boolean efn$packetSneak(boolean var0) {
      if (var0) {
         return true;
      } else {
         SneakModule var1 = SneakModule.active;
         return var1 != null && var1.doPacket();
      }
   }
}
