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
   private final BooleanSupplier efn$connectionActive = this::method_10758;

   public MixinClientConnection() {
   }

   @Shadow
   public abstract boolean method_10758();

   @Shadow
   public abstract NetworkSide method_36121();

   @Inject(
      method = {"method_10770"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$onPacketReceive(ChannelHandlerContext var1, Packet<?> var2, CallbackInfo var3) {
      if (this.method_36121() == NetworkSide.field_11942) {
         if (PacketReceiveNettyEvent.dispatchSync(var2)) {
            var3.cancel();
         } else {
            PacketReceiveEvent.dispatchAsync(var2, this.efn$connectionActive);
         }
      }
   }

   @Inject(
      method = {"method_52906"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void efn$onPacketSend(Packet<?> var1, ChannelFutureListener var2, boolean var3, CallbackInfo var4) {
      if (this.method_36121() == NetworkSide.field_11942) {
         if (PayGuard.blocks(var1)) {
            var4.cancel();
         } else if (PacketSendEvent.dispatchSync(var1)) {
            var4.cancel();
         } else {
            ServerRotation.noteSent(var1);
         }
      }
   }
}
