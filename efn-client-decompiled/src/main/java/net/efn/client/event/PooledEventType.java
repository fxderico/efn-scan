package net.efn.client.event;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class PooledEventType<E extends Event> extends EventType<E> {
   public final EventPool<E> pool;

   public PooledEventType(Supplier<E> supplier, int i) {
      this.pool = new EventPool<>(supplier, i);
   }

   public PooledEventType(String string, Supplier<E> supplier, int i) {
      super(string);
      this.pool = new EventPool<>(supplier, i);
   }
}
