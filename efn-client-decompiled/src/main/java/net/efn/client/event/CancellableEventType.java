package net.efn.client.event;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class CancellableEventType<E extends CancellableEvent> extends EventType<E> {
   public CancellableEventType() {
   }

   public CancellableEventType(String var1) {
      super(var1);
   }

   public void dispatchPre(E var1) {
      dispatchPreCancellable(this.list, (E)var1);
   }

   public void dispatchPost(E var1) {
      dispatchPostGuarded(this.list, (E)var1);
   }

   static <E extends CancellableEvent> void dispatchPreCancellable(ListenerList<E> var0, E var1) {
      Consumer[] var2 = var0.preCallbacks();

      for (int var3 = 0; var3 < var2.length; var3++) {
         try {
            var2[var3].accept(var1);
         } catch (Exception var5) {
            ListenerErrorReporter.report(var0.handleOf(Stage.PRE, var2[var3]), var1, var5);
         }

         if (var1.cancelled) {
            break;
         }
      }
   }

   static <E extends CancellableEvent> void dispatchPostGuarded(ListenerList<E> var0, E var1) {
      var1.postStage = true;

      try {
         var0.dispatchPost(var1);
      } finally {
         var1.postStage = false;
      }
   }
}
