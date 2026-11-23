package com.example.client.mixin;

import net.efn.client.module.impl.misc.SpamModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.EnterReconfigurationS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinSpamClientPacketListener {
   @Shadow
   private ClientWorld field_3699;

   public MixinSpamClientPacketListener() {
   }

   @Inject(
      method = {"method_11120"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$spamRelogin(GameJoinS2CPacket var1, CallbackInfo var2) {
      if (SpamModule.active != null && this.field_3699 != null) {
         SpamModule.onGameLeft();
      }
   }

   @Inject(
      method = {"method_52798"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/class_2600;method_11074(Lnet/minecraft/class_2596;Lnet/minecraft/class_2547;Lnet/minecraft/class_11980;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$spamReconfiguration(EnterReconfigurationS2CPacket var1, CallbackInfo var2) {
      if (SpamModule.active != null) {
         SpamModule.onGameLeft();
      }
   }
}
