package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import java.net.InetSocketAddress;
import net.efn.client.proxy.ProxyRouting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({ClientConnection.class})
public abstract class MixinProxyConnection {
   public MixinProxyConnection() {
   }

   @WrapOperation(
      method = {"method_52271"},
      at = {@At(
         value = "INVOKE",
         target = "Lio/netty/bootstrap/Bootstrap;handler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/AbstractBootstrap;"
      )}
   )
   private static AbstractBootstrap<?, ?> efn$routeThroughProxy(
      Bootstrap var0, ChannelHandler var1, Operation<AbstractBootstrap<?, ?>> var2, @Local(argsOnly = true) InetSocketAddress var3
   ) {
      return (AbstractBootstrap<?, ?>)var2.call(new Object[]{var0, ProxyRouting.route(var3, var1)});
   }
}
