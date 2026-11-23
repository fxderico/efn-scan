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

   public static void dispatchAsync(Packet<?> packet) {
      dispatchAsync(packet, null);
   }

   public static void dispatchAsync(Packet<?> packet, BooleanSupplier booleanSupplier) {
      if (TYPE.hasListeners()) {
         PacketReceiveEvent var2 = TYPE.pool.acquire();
         var2.packet = packet;
         var2.connectionActive = booleanSupplier;
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
