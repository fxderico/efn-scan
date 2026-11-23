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
import net.minecraft.client.network.MultiplayerServerListPinger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(
   value = {MultiplayerServerListPinger.class},
   priority = 1100
)
public abstract class MixinProxyServerStatusPinger {
   public MixinProxyServerStatusPinger() {
   }

   @WrapOperation(
      method = {"method_3001"},
      at = {@At(
         value = "INVOKE",
         target = "Lio/netty/bootstrap/Bootstrap;handler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/AbstractBootstrap;"
      )},
      require = 0
   )
   private AbstractBootstrap<?, ?> efn$routeLegacyPing(
      Bootstrap var1, ChannelHandler var2, Operation<AbstractBootstrap<?, ?>> var3, @Local(argsOnly = true) InetSocketAddress var4
   ) {
      return (AbstractBootstrap<?, ?>)var3.call(new Object[]{var1, ProxyRouting.route(var4, var2)});
   }
}
