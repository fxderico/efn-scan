package com.example.client.mixin;

import net.efn.client.event.impl.ChatSendEvent;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinClientPacketListener {
   public MixinClientPacketListener() {
   }

   @Inject(
      method = {"method_45729"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$onSendChat(String var1, CallbackInfo var2) {
      if (ChatSendEvent.dispatch(var1)) {
         var2.cancel();
      }
   }

   @Inject(
      method = {"method_11148"},
      at = {@At("TAIL")}
   )
   private void efn$afterEntityEvent(EntityStatusS2CPacket var1, CallbackInfo var2) {
      PacketAppliedEvent.dispatch(var1);
   }

   @Inject(
      method = {"method_11120"},
      at = {@At("TAIL")}
   )
   private void efn$afterLogin(GameJoinS2CPacket var1, CallbackInfo var2) {
      PacketAppliedEvent.dispatch(var1);
   }

   @Inject(
      method = {"method_11128"},
      at = {@At("TAIL")}
   )
   private void efn$afterChunkData(ChunkDataS2CPacket var1, CallbackInfo var2) {
      PacketAppliedEvent.dispatch(var1);
   }

   @Inject(
      method = {"method_11136"},
      at = {@At("TAIL")}
   )
   private void efn$afterBlockUpdate(BlockUpdateS2CPacket var1, CallbackInfo var2) {
      PacketAppliedEvent.dispatch(var1);
   }

   @Inject(
      method = {"method_11100"},
      at = {@At("TAIL")}
   )
   private void efn$afterSectionUpdate(ChunkDeltaUpdateS2CPacket var1, CallbackInfo var2) {
      PacketAppliedEvent.dispatch(var1);
   }

   @Inject(
      method = {"method_11143"},
      at = {@At("TAIL")}
   )
   private void efn$afterLightUpdate(LightUpdateS2CPacket var1, CallbackInfo var2) {
      PacketAppliedEvent.dispatch(var1);
   }

   @Inject(
      method = {"method_11132"},
      at = {@At("TAIL")}
   )
   private void efn$afterEntityMotion(EntityVelocityUpdateS2CPacket var1, CallbackInfo var2) {
      PacketAppliedEvent.dispatch(var1);
   }
}
