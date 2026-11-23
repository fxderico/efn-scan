package net.efn.client.event;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class PooledCancellableEventType<E extends CancellableEvent> extends PooledEventType<E> {
   public PooledCancellableEventType(Supplier<E> var1, int var2) {
      super(var1, var2);
   }

   public PooledCancellableEventType(String var1, Supplier<E> var2, int var3) {
      super(var1, var2, var3);
   }

   public void dispatchPre(E var1) {
      CancellableEventType.dispatchPreCancellable(this.list, (E)var1);
   }

   public void dispatchPost(E var1) {
      CancellableEventType.dispatchPostGuarded(this.list, (E)var1);
   }
}
