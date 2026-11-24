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
   private ClientWorld world;

   public MixinSpamClientPacketListener() {
   }

   @Inject(
      method = {"onGameJoin"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$spamRelogin(GameJoinS2CPacket gameJoinS2CPacket, CallbackInfo callbackInfo) {
      if (SpamModule.active != null && this.world != null) {
         SpamModule.onGameLeft();
      }
   }

   @Inject(
      method = {"onEnterReconfiguration"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
         shift = Shift.AFTER
      )}
   )
   private void efn$spamReconfiguration(EnterReconfigurationS2CPacket enterReconfigurationS2CPacket, CallbackInfo callbackInfo) {
      if (SpamModule.active != null) {
         SpamModule.onGameLeft();
      }
   }
}
