package net.efn.client.event;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EventPool<E extends Event> {
   private final ArrayBlockingQueue<E> pool;
   private final Supplier<E> factory;

   public EventPool(Supplier<E> var1, int var2) {
      this.factory = var1;
      this.pool = new ArrayBlockingQueue<>(var2);
   }

   public E acquire() {
      Event var1 = this.pool.poll();
      if (var1 == null) {
         var1 = this.factory.get();
      }

      return (E)var1;
   }

   public void release(E var1) {
      this.pool.offer((E)var1);
   }
}
