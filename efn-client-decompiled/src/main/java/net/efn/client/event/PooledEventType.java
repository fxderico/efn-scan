package net.efn.client.event;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class PooledEventType<E extends Event> extends EventType<E> {
   public final EventPool<E> pool;

   public PooledEventType(Supplier<E> var1, int var2) {
      this.pool = new EventPool<>(var1, var2);
   }

   public PooledEventType(String var1, Supplier<E> var2, int var3) {
      super(var1);
      this.pool = new EventPool<>(var2, var3);
   }
}
