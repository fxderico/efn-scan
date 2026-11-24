package net.efn.client.event;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class PooledCancellableEventType<E extends CancellableEvent> extends PooledEventType<E> {
   public PooledCancellableEventType(Supplier<E> supplier, int i) {
      super(supplier, i);
   }

   public PooledCancellableEventType(String string, Supplier<E> supplier, int i) {
      super(string, supplier, i);
   }

   public void dispatchPre(E cancellableEvent) {
      CancellableEventType.dispatchPreCancellable(this.list, (E)cancellableEvent);
   }

   public void dispatchPost(E cancellableEvent) {
      CancellableEventType.dispatchPostGuarded(this.list, (E)cancellableEvent);
   }
}
