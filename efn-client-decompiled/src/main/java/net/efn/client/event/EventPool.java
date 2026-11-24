package net.efn.client.event;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EventPool<E extends Event> {
   private final ArrayBlockingQueue<E> pool;
   private final Supplier<E> factory;

   public EventPool(Supplier<E> supplier, int i) {
      this.factory = supplier;
      this.pool = new ArrayBlockingQueue<>(i);
   }

   public E acquire() {
      Event var1 = this.pool.poll();
      if (var1 == null) {
         var1 = this.factory.get();
      }

      return (E)var1;
   }

   public void release(E event) {
      this.pool.offer((E)event);
   }
}
