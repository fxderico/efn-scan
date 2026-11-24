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

   public static ChannelHandler route(InetSocketAddress inetSocketAddress, ChannelHandler channelHandler) {
      if (inetSocketAddress != null && !isLocal(inetSocketAddress)) {
         Proxy var2 = Proxies.get().enabledCopy();
         if (var2 != null && var2.isValid()) {
            final ProxyHandler var3 = handler(var2);
            return new ChannelInitializer<Channel>() {
               @Override
               protected void initChannel(Channel channel) {
                  channel.pipeline().addFirst("efn_proxy", var3);
                  channel.pipeline().addLast(channelHandler);
               }
            };
         } else {
            return channelHandler;
         }
      } else {
         return channelHandler;
      }
   }

   static ProxyHandler handler(Proxy proxy) {
      InetSocketAddress var1 = new InetSocketAddress(proxy.address.trim(), proxy.port);
      String var2 = proxy.username != null && !proxy.username.isEmpty() ? proxy.username : null;
      String var3 = proxy.password != null && !proxy.password.isEmpty() ? proxy.password : null;

      return (ProxyHandler)(switch (proxy.type) {
         case SOCKS4 -> new Socks4ProxyHandler(var1, var2);
         case SOCKS5 -> new Socks5ProxyHandler(var1, var2, var3);
      });
   }

   private static boolean isLocal(InetSocketAddress inetSocketAddress) {
      InetAddress var1 = inetSocketAddress.getAddress();
      if (var1 == null) {
         return "localhost".equalsIgnoreCase(inetSocketAddress.getHostString());
      } else if (!var1.isLoopbackAddress() && !var1.isAnyLocalAddress() && !var1.isLinkLocalAddress() && !var1.isSiteLocalAddress()) {
         byte[] var2 = var1.getAddress();
         return var2.length == 16 && (var2[0] & 254) == 252;
      } else {
         return true;
      }
   }
}
