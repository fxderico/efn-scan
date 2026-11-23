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

   public synchronized void register(ListenerHandle<E> var1) {
      if (var1.stage() == Stage.PRE) {
         this.pre = this.preHandles.insert(this.pre, var1);
      } else {
         this.post = this.postHandles.insert(this.post, var1);
      }
   }

   public synchronized void unregister(ListenerHandle<E> var1) {
      if (var1.stage() == Stage.PRE) {
         this.pre = this.preHandles.remove(this.pre, var1);
      } else {
         this.post = this.postHandles.remove(this.post, var1);
      }
   }

   public void dispatchPre(E var1) {
      Consumer[] var2 = this.pre;

      for (int var3 = 0; var3 < var2.length; var3++) {
         try {
            var2[var3].accept(var1);
         } catch (Exception var5) {
            ListenerErrorReporter.report(this.handleOf(Stage.PRE, var2[var3]), var1, var5);
         }
      }
   }

   public void dispatchPost(E var1) {
      Consumer[] var2 = this.post;

      for (int var3 = 0; var3 < var2.length; var3++) {
         try {
            var2[var3].accept(var1);
         } catch (Exception var5) {
            ListenerErrorReporter.report(this.handleOf(Stage.POST, var2[var3]), var1, var5);
         }
      }
   }

   Consumer<E>[] preCallbacks() {
      return this.pre;
   }

   Consumer<E>[] postCallbacks() {
      return this.post;
   }

   synchronized ListenerHandle<E> handleOf(Stage var1, Consumer<E> var2) {
      ListenerHandle var3 = var1 == Stage.PRE ? this.preHandles.of(this.pre, var2) : this.postHandles.of(this.post, var2);
      return var3 != null ? var3 : new ListenerHandle<>(null, var1, EventPriority.NORMAL, null, var2);
   }

   private static <E extends Event> Consumer<E>[] none() {
      return NONE;
   }

   @Environment(EnvType.CLIENT)
   private static final class Handles<E extends Event> {
      private ListenerHandle<E>[] handles = new ListenerHandle[4];

      private Handles() {
      }

      Consumer<E>[] insert(Consumer<E>[] var1, ListenerHandle<E> var2) {
         int var3 = var1.length;
         int var4 = var2.priority().value;
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
         this.handles[var5] = var2;
         Consumer[] var8 = new Consumer[var3 + 1];
         System.arraycopy(var1, 0, var8, 0, var5);
         var8[var5] = var2.callback();
         System.arraycopy(var1, var5, var8, var5 + 1, var3 - var5);
         return var8;
      }

      Consumer<E>[] remove(Consumer<E>[] var1, ListenerHandle<E> var2) {
         int var3 = var1.length;
         int var4 = this.indexOf(var1, var2);
         if (var4 < 0) {
            return var1;
         } else {
            System.arraycopy(this.handles, var4 + 1, this.handles, var4, var3 - var4 - 1);
            this.handles[var3 - 1] = null;
            if (var3 == 1) {
               return ListenerList.none();
            } else {
               Consumer[] var5 = new Consumer[var3 - 1];
               System.arraycopy(var1, 0, var5, 0, var4);
               System.arraycopy(var1, var4 + 1, var5, var4, var3 - var4 - 1);
               return var5;
            }
         }
      }

      private int indexOf(Consumer<E>[] var1, ListenerHandle<E> var2) {
         Consumer var3 = var2.callback();

         for (int var4 = 0; var4 < var1.length; var4++) {
            if (var1[var4] == var3 && this.handles[var4] == var2) {
               return var4;
            }
         }

         return -1;
      }

      ListenerHandle<E> of(Consumer<E>[] var1, Consumer<E> var2) {
         for (int var3 = 0; var3 < var1.length; var3++) {
            if (var1[var3] == var2) {
               return this.handles[var3];
            }
         }

         return null;
      }
   }
}
