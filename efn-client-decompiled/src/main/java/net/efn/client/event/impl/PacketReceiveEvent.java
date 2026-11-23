package net.efn.client.event.impl;

import java.util.function.BooleanSupplier;
import net.efn.client.event.Event;
import net.efn.client.event.EventBus;
import net.efn.client.event.PooledEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.Packet;

@Environment(EnvType.CLIENT)
public final class PacketReceiveEvent extends Event implements Runnable {
   public static final PooledEventType<PacketReceiveEvent> TYPE = new PooledEventType<>("PacketReceiveEvent", PacketReceiveEvent::new, 1024);
   public Packet<?> packet;
   private BooleanSupplier connectionActive;

   private PacketReceiveEvent() {
   }

   public static void dispatchAsync(Packet<?> var0) {
      dispatchAsync(var0, null);
   }

   public static void dispatchAsync(Packet<?> var0, BooleanSupplier var1) {
      if (TYPE.hasListeners()) {
         PacketReceiveEvent var2 = TYPE.pool.acquire();
         var2.packet = var0;
         var2.connectionActive = var1;
         if (!EventBus.INSTANCE.postAsync(var2)) {
            var2.release();
         }
      }
   }

   @Override
   public void run() {
      try {
         if (this.connectionActive == null || this.connectionActive.getAsBoolean()) {
            TYPE.dispatch(this);
            TYPE.dispatchPost(this);
            return;
         }
      } finally {
         this.release();
      }
   }

   private void release() {
      this.packet = null;
      this.connectionActive = null;
      TYPE.pool.release(this);
   }
}
