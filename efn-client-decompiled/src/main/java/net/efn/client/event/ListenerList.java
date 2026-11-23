package net.efn.client.event;

import java.util.Arrays;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ListenerList<E extends Event> {
   private static final Consumer[] NONE = new Consumer[0];
   private volatile Consumer<E>[] pre = none();
   private volatile Consumer<E>[] post = none();
   private final ListenerList.Handles<E> preHandles = new ListenerList.Handles<>();
   private final ListenerList.Handles<E> postHandles = new ListenerList.Handles<>();

   public ListenerList() {
   }

   public synchronized void register(ListenerHandle<E> listenerHandle) {
      if (listenerHandle.stage() == Stage.PRE) {
         this.pre = this.preHandles.insert(this.pre, listenerHandle);
      } else {
         this.post = this.postHandles.insert(this.post, listenerHandle);
      }
   }

   public synchronized void unregister(ListenerHandle<E> listenerHandle) {
      if (listenerHandle.stage() == Stage.PRE) {
         this.pre = this.preHandles.remove(this.pre, listenerHandle);
      } else {
         this.post = this.postHandles.remove(this.post, listenerHandle);
      }
   }

   public void dispatchPre(E event) {
      Consumer[] var2 = this.pre;

      for (int var3 = 0; var3 < var2.length; var3++) {
         try {
            var2[var3].accept(event);
         } catch (Exception var5) {
            ListenerErrorReporter.report(this.handleOf(Stage.PRE, var2[var3]), event, var5);
         }
      }
   }

   public void dispatchPost(E event) {
      Consumer[] var2 = this.post;

      for (int var3 = 0; var3 < var2.length; var3++) {
         try {
            var2[var3].accept(event);
         } catch (Exception var5) {
            ListenerErrorReporter.report(this.handleOf(Stage.POST, var2[var3]), event, var5);
         }
      }
   }

   Consumer<E>[] preCallbacks() {
      return this.pre;
   }

   Consumer<E>[] postCallbacks() {
      return this.post;
   }

   synchronized ListenerHandle<E> handleOf(Stage stage, Consumer<E> consumer) {
      ListenerHandle var3 = stage == Stage.PRE ? this.preHandles.of(this.pre, consumer) : this.postHandles.of(this.post, consumer);
      return var3 != null ? var3 : new ListenerHandle<>(null, stage, EventPriority.NORMAL, null, consumer);
   }

   private static <E extends Event> Consumer<E>[] none() {
      return NONE;
   }

   @Environment(EnvType.CLIENT)
   private static final class Handles<E extends Event> {
      private ListenerHandle<E>[] handles = new ListenerHandle[4];

      private Handles() {
      }

      Consumer<E>[] insert(Consumer<E>[] consumers, ListenerHandle<E> listenerHandle) {
         int var3 = consumers.length;
         int var4 = listenerHandle.priority().value;
         int var5 = 0;
         int var6 = var3;

         while (var5 < var6) {
            int var7 = var5 + var6 >>> 1;
            if (this.handles[var7].priority().value >= var4) {
               var5 = var7 + 1;
            } else {
               var6 = var7;
            }
         }

         if (var3 == this.handles.length) {
            this.handles = Arrays.copyOf(this.handles, var3 * 2);
         }

         System.arraycopy(this.handles, var5, this.handles, var5 + 1, var3 - var5);
         this.handles[var5] = listenerHandle;
         Consumer[] var8 = new Consumer[var3 + 1];
         System.arraycopy(consumers, 0, var8, 0, var5);
         var8[var5] = listenerHandle.callback();
         System.arraycopy(consumers, var5, var8, var5 + 1, var3 - var5);
         return var8;
      }

      Consumer<E>[] remove(Consumer<E>[] consumers, ListenerHandle<E> listenerHandle) {
         int var3 = consumers.length;
         int var4 = this.indexOf(consumers, listenerHandle);
         if (var4 < 0) {
            return consumers;
         } else {
            System.arraycopy(this.handles, var4 + 1, this.handles, var4, var3 - var4 - 1);
            this.handles[var3 - 1] = null;
            if (var3 == 1) {
               return ListenerList.none();
            } else {
               Consumer[] var5 = new Consumer[var3 - 1];
               System.arraycopy(consumers, 0, var5, 0, var4);
               System.arraycopy(consumers, var4 + 1, var5, var4, var3 - var4 - 1);
               return var5;
            }
         }
      }

      private int indexOf(Consumer<E>[] consumers, ListenerHandle<E> listenerHandle) {
         Consumer var3 = listenerHandle.callback();

         for (int var4 = 0; var4 < consumers.length; var4++) {
            if (consumers[var4] == var3 && this.handles[var4] == listenerHandle) {
               return var4;
            }
         }

         return -1;
      }

      ListenerHandle<E> of(Consumer<E>[] consumers, Consumer<E> consumer) {
         for (int var3 = 0; var3 < consumers.length; var3++) {
            if (consumers[var3] == consumer) {
               return this.handles[var3];
            }
         }

         return null;
      }
   }
}
