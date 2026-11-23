package net.efn.client.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SessionWorker<T> implements AutoCloseable {
   private final String name;
   private ExecutorService executor;
   private Future<T> pending;

   public SessionWorker(String string) {
      this.name = string;
   }

   public boolean busy() {
      return this.pending != null;
   }

   public void submit(Callable<T> callable) {
      if (this.pending != null) {
         throw new IllegalStateException("Worker is busy");
      } else {
         if (this.executor == null) {
            this.executor = Executors.newSingleThreadExecutor(runnable -> {
               Thread var2 = new Thread(runnable, this.name);
               var2.setDaemon(true);
               return var2;
            });
         }

         this.pending = this.executor.submit(callable);
      }
   }

   public void poll(Consumer<T> consumer) {
      if (this.pending != null && this.pending.isDone()) {
         Future var2 = this.pending;
         this.pending = null;

         try {
            consumer.accept(var2.get());
         } catch (InterruptedException var6) {
            Thread.currentThread().interrupt();
         } catch (ExecutionException var7) {
            if (var7.getCause() instanceof Error var5) {
               throw var5;
            }

            throw new IllegalStateException("Snapshot worker failed", var7.getCause());
         } catch (CancellationException var8) {
         }
      }
   }

   @Override
   public void close() {
      if (this.pending != null) {
         this.pending.cancel(true);
      }

      this.pending = null;
      if (this.executor != null) {
         this.executor.shutdownNow();
      }

      this.executor = null;
   }
}
