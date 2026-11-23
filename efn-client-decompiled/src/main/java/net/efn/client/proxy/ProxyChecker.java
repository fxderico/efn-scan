package net.efn.client.proxy;

import com.google.common.net.InetAddresses;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ProxyChecker {
   private static final InetSocketAddress TARGET = new InetSocketAddress(InetAddresses.forString("1.1.1.1"), 443);
   private static final int TIMEOUT_MS = 8000;
   private static final int IN_FLIGHT = 32;
   private static final ProxyChecker INSTANCE = new ProxyChecker();
   private final Set<Proxy> checking = ConcurrentHashMap.newKeySet();
   private final Deque<Proxy> queue = new ArrayDeque<>();
   private int running;
   private EventLoopGroup group;

   private ProxyChecker() {
   }

   public static ProxyChecker get() {
      return INSTANCE;
   }

   public synchronized void check(Collection<Proxy> var1) {
      for (Proxy var3 : var1) {
         if (this.checking.add(var3)) {
            this.queue.add(var3);
         }
      }

      this.pump();
   }

   public boolean isChecking(Proxy var1) {
      return this.checking.contains(var1);
   }

   public int pending() {
      return this.checking.size();
   }

   private synchronized void pump() {
      if (this.group == null) {
         this.group = new MultiThreadIoEventLoopGroup(2, new DefaultThreadFactory("EFN proxy check", true), NioIoHandler.newFactory());
      }

      while (this.running < 32 && !this.queue.isEmpty()) {
         Proxy var1 = this.queue.poll();
         this.running++;
         this.group.execute(() -> this.start(var1));
      }
   }

   private void start(Proxy var1) {
      AtomicBoolean var3 = new AtomicBoolean();
      if (!var1.isValid()) {
         this.finish(var1, -1, var3);
      } else {
         final ProxyHandler var2;
         try {
            var2 = ProxyRouting.handler(var1);
         } catch (RuntimeException var7) {
            this.finish(var1, -1, var3);
            return;
         }

         var2.setConnectTimeoutMillis(8000L);
         long var4 = System.nanoTime();
         ChannelFuture var6 = ((Bootstrap)((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group(this.group)).channel(NioSocketChannel.class))
                  .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000))
               .handler(new ChannelInitializer<Channel>() {
                  protected void initChannel(Channel var1) {
                     var1.pipeline().addLast(new ChannelHandler[]{var2, new ChannelInboundHandlerAdapter() {
                        public void exceptionCaught(ChannelHandlerContext var1, Throwable var2x) {
                           var1.close();
                        }
                     }});
                  }
               }))
            .connect(TARGET);
         var6.addListener(var3x -> {
            if (!var3x.isSuccess()) {
               this.finish(var1, -1, var3);
            }
         });
         var2.connectFuture().addListener(var6x -> {
            int var7x = (int)((System.nanoTime() - var4) / 1000000L);
            this.finish(var1, var6x.isSuccess() ? var7x : -1, var3);
            var6.channel().close();
         });
      }
   }

   private void finish(Proxy var1, int var2, AtomicBoolean var3) {
      if (var3.compareAndSet(false, true)) {
         var1.ping = var2;
         this.checking.remove(var1);
         boolean var4;
         synchronized (this) {
            this.running--;
            var4 = this.running == 0 && this.queue.isEmpty();
            this.pump();
         }

         if (var4) {
            Proxies.get().checked();
         }
      }
   }
}
