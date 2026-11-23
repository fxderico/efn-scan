package net.efn.client.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ProxyRouting {
   private static final String HANDLER_NAME = "efn_proxy";

   private ProxyRouting() {
   }

   public static ChannelHandler route(InetSocketAddress var0, final ChannelHandler var1) {
      if (var0 != null && !isLocal(var0)) {
         Proxy var2 = Proxies.get().enabledCopy();
         if (var2 != null && var2.isValid()) {
            final ProxyHandler var3 = handler(var2);
            return new ChannelInitializer<Channel>() {
               protected void initChannel(Channel var1x) {
                  var1x.pipeline().addFirst("efn_proxy", var3);
                  var1x.pipeline().addLast(new ChannelHandler[]{var1});
               }
            };
         } else {
            return var1;
         }
      } else {
         return var1;
      }
   }

   static ProxyHandler handler(Proxy var0) {
      InetSocketAddress var1 = new InetSocketAddress(var0.address.trim(), var0.port);
      String var2 = var0.username != null && !var0.username.isEmpty() ? var0.username : null;
      String var3 = var0.password != null && !var0.password.isEmpty() ? var0.password : null;

      return (ProxyHandler)(switch (var0.type) {
         case SOCKS4 -> new Socks4ProxyHandler(var1, var2);
         case SOCKS5 -> new Socks5ProxyHandler(var1, var2, var3);
      });
   }

   private static boolean isLocal(InetSocketAddress var0) {
      InetAddress var1 = var0.getAddress();
      if (var1 == null) {
         return "localhost".equalsIgnoreCase(var0.getHostString());
      } else if (!var1.isLoopbackAddress() && !var1.isAnyLocalAddress() && !var1.isLinkLocalAddress() && !var1.isSiteLocalAddress()) {
         byte[] var2 = var1.getAddress();
         return var2.length == 16 && (var2[0] & 254) == 252;
      } else {
         return true;
      }
   }
}
