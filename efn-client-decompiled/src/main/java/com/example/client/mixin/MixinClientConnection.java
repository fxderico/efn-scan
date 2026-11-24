package com.example.client.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import java.util.function.BooleanSupplier;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.security.PayGuard;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientConnection.class})
public abstract class MixinClientConnection {
   @Unique
   private final BooleanSupplier efn$connectionActive = this::isOpen;

   public MixinClientConnection() {
   }

   @Shadow
   public abstract boolean isOpen();

   @Shadow
   public abstract NetworkSide getSide();

   @Inject(
      method = {"channelRead0"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$onPacketReceive(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo callbackInfo) {
      if (this.getSide() == NetworkSide.CLIENTBOUND) {
         if (PacketReceiveNettyEvent.dispatchSync(packet)) {
            callbackInfo.cancel();
         } else {
            PacketReceiveEvent.dispatchAsync(packet, this.efn$connectionActive);
         }
      }
   }

   @Inject(
      method = {"send"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$onPacketSend(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean bl, CallbackInfo callbackInfo) {
      if (this.getSide() == NetworkSide.CLIENTBOUND) {
         if (PayGuard.blocks(packet)) {
            callbackInfo.cancel();
         } else if (PacketSendEvent.dispatchSync(packet)) {
            callbackInfo.cancel();
         } else {
            ServerRotation.noteSent(packet);
         }
      }
   }
}
