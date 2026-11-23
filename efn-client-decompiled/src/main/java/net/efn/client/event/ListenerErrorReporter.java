package net.efn.client.event;

import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class ListenerErrorReporter {
   private static final int FULL_TRACE_LIMIT = 4;
   private static final int SUMMARY_INTERVAL = 256;
   private static final Map<ListenerHandle<?>, Integer> failureCounts = new IdentityHashMap<>();

   private ListenerErrorReporter() {
   }

   static synchronized void report(ListenerHandle<?> listenerHandle, Event event, Exception exception) {
      int var4 = failureCounts.merge(listenerHandle, 1, Integer::sum);
      String var3 = listenerHandle.owner() == null ? "<static>" : listenerHandle.owner().getClass().getName();
      if (var4 <= 4) {
         System.err
            .println(
               "[EfnEventBus] Listener threw (failure #"
                  + var4
                  + "): owner="
                  + var3
                  + " event="
                  + event.getClass().getSimpleName()
                  + " stage="
                  + listenerHandle.stage()
                  + " priority="
                  + listenerHandle.priority()
            );
         exception.printStackTrace();
      } else if (var4 % 256 == 0) {
         System.err
            .println(
               "[EfnEventBus] Listener still failing (" + var4 + " total): owner=" + var3 + " event=" + event.getClass().getSimpleName() + ": " + exception
            );
      }
   }
}
