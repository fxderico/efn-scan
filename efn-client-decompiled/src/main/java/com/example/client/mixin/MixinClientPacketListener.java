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
      method = {"sendChatMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$onSendChat(String string, CallbackInfo callbackInfo) {
      if (ChatSendEvent.dispatch(string)) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = {"onEntityStatus"},
      at = {@At("TAIL")}
   )
   private void efn$afterEntityEvent(EntityStatusS2CPacket entityStatusS2CPacket, CallbackInfo callbackInfo) {
      PacketAppliedEvent.dispatch(entityStatusS2CPacket);
   }

   @Inject(
      method = {"onGameJoin"},
      at = {@At("TAIL")}
   )
   private void efn$afterLogin(GameJoinS2CPacket gameJoinS2CPacket, CallbackInfo callbackInfo) {
      PacketAppliedEvent.dispatch(gameJoinS2CPacket);
   }

   @Inject(
      method = {"onChunkData"},
      at = {@At("TAIL")}
   )
   private void efn$afterChunkData(ChunkDataS2CPacket chunkDataS2CPacket, CallbackInfo callbackInfo) {
      PacketAppliedEvent.dispatch(chunkDataS2CPacket);
   }

   @Inject(
      method = {"onBlockUpdate"},
      at = {@At("TAIL")}
   )
   private void efn$afterBlockUpdate(BlockUpdateS2CPacket blockUpdateS2CPacket, CallbackInfo callbackInfo) {
      PacketAppliedEvent.dispatch(blockUpdateS2CPacket);
   }

   @Inject(
      method = {"onChunkDeltaUpdate"},
      at = {@At("TAIL")}
   )
   private void efn$afterSectionUpdate(ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket, CallbackInfo callbackInfo) {
      PacketAppliedEvent.dispatch(chunkDeltaUpdateS2CPacket);
   }

   @Inject(
      method = {"onLightUpdate"},
      at = {@At("TAIL")}
   )
   private void efn$afterLightUpdate(LightUpdateS2CPacket lightUpdateS2CPacket, CallbackInfo callbackInfo) {
      PacketAppliedEvent.dispatch(lightUpdateS2CPacket);
   }

   @Inject(
      method = {"onEntityVelocityUpdate"},
      at = {@At("TAIL")}
   )
   private void efn$afterEntityMotion(EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket, CallbackInfo callbackInfo) {
      PacketAppliedEvent.dispatch(entityVelocityUpdateS2CPacket);
   }
}
