package net.efn.client.event;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class CancellableEventType<E extends CancellableEvent> extends EventType<E> {
   public CancellableEventType() {
   }

   public CancellableEventType(String string) {
      super(string);
   }

   public void dispatchPre(E cancellableEvent) {
      dispatchPreCancellable(this.list, (E)cancellableEvent);
   }

   public void dispatchPost(E cancellableEvent) {
      dispatchPostGuarded(this.list, (E)cancellableEvent);
   }

   static <E extends CancellableEvent> void dispatchPreCancellable(ListenerList<E> listenerList, E cancellableEvent) {
      Consumer[] var2 = listenerList.preCallbacks();

      for (int var3 = 0; var3 < var2.length; var3++) {
         try {
            var2[var3].accept(cancellableEvent);
         } catch (Exception var5) {
            ListenerErrorReporter.report(listenerList.handleOf(Stage.PRE, var2[var3]), cancellableEvent, var5);
         }

         if (cancellableEvent.cancelled) {
            break;
         }
      }
   }

   static <E extends CancellableEvent> void dispatchPostGuarded(ListenerList<E> listenerList, E cancellableEvent) {
      cancellableEvent.postStage = true;

      try {
         listenerList.dispatchPost(cancellableEvent);
      } finally {
         cancellableEvent.postStage = false;
      }
   }
}
