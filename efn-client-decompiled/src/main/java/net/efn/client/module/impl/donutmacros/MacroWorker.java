package net.efn.client.module.impl.donutmacros;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
final class MacroWorker {
   private static final long POLL_MS = 50L;
   private static final long CALL_TIMEOUT_SECONDS = 10L;
   private final String name;
   private volatile Thread thread;

   MacroWorker(String string) {
      this.name = string;
   }

   synchronized void start(Runnable runnable, Consumer<Throwable> consumer) {
      this.stop();
      Thread var3 = new Thread(() -> {
         try {
            runnable.run();
         } catch (MacroWorker.Cancelled var23) {
            synchronized (this) {
               if (this.thread == Thread.currentThread()) {
                  this.thread = null;
               }
            }
         } catch (Throwable var24) {
            if (this.thread == Thread.currentThread()) {
               consumer.accept(var24);
            }
         } finally {
            synchronized (this) {
               if (this.thread == Thread.currentThread()) {
                  this.thread = null;
               }
            }
         }
      }, this.name);
      var3.setDaemon(true);
      this.thread = var3;
      var3.start();
   }

   synchronized void stop() {
      Thread var1 = this.thread;
      this.thread = null;
      if (var1 != null && var1 != Thread.currentThread()) {
         var1.interrupt();
      }
   }

   boolean running() {
      return this.thread != null;
   }

   void check() {
      if (this.thread != Thread.currentThread() || Thread.currentThread().isInterrupted()) {
         throw new MacroWorker.Cancelled();
      }
   }

   void sleep(long l) {
      this.check();

      try {
         Thread.sleep(Math.max(0L, l));
      } catch (InterruptedException var4) {
         Thread.currentThread().interrupt();
         throw new MacroWorker.Cancelled();
      }

      this.check();
   }

   void ticks(int i) {
      this.sleep(i * 50L);
   }

   <T> T call(Supplier<T> supplier) {
      this.check();
      Thread var2 = Thread.currentThread();
      CompletableFuture var3 = MinecraftClient.getInstance().submit(() -> {
         if (this.thread != var2) {
            throw new MacroWorker.Cancelled();
         } else {
            return supplier.get();
         }
      });

      try {
         return (T)var3.get(10L, TimeUnit.SECONDS);
      } catch (InterruptedException var7) {
         var3.cancel(false);
         Thread.currentThread().interrupt();
         throw new MacroWorker.Cancelled();
      } catch (ExecutionException var8) {
         Throwable var5 = var8.getCause();
         if (var5 instanceof RuntimeException var10) {
            throw var10;
         } else if (var5 instanceof Error var6) {
            throw var6;
         } else {
            throw new IllegalStateException(var5);
         }
      } catch (TimeoutException var9) {
         var3.cancel(false);
         throw new IllegalStateException("The game did not respond");
      }
   }

   void run(Runnable runnable) {
      this.call(() -> {
         runnable.run();
         return Boolean.TRUE;
      });
   }

   boolean waitFor(BooleanSupplier booleanSupplier, long l) {
      long var4 = System.currentTimeMillis() + l;

      while (!this.call(booleanSupplier::getAsBoolean)) {
         if (System.currentTimeMillis() >= var4) {
            return false;
         }

         this.sleep(50L);
      }

      return true;
   }

   @Environment(EnvType.CLIENT)
   static final class Cancelled extends RuntimeException {
      Cancelled() {
         super(null, null, false, false);
      }
   }
}
