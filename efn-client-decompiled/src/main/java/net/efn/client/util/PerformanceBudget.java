package net.efn.client.util;

import net.efn.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PerformanceBudget {
   private static final TickWorkBudget WORK = new TickWorkBudget(System::nanoTime);
   private static long lastTick;

   private PerformanceBudget() {
   }

   public static void beginTick() {
      lastTick = WORK.usedNanos();
      WORK.reset(PerformanceModule.scanBudgetNanos());
   }

   public static long acquire(long var0) {
      return PerformanceModule.optimizationsEnabled() ? WORK.acquire(var0) : System.nanoTime() + Math.max(0L, var0);
   }

   public static void record(long var0) {
      WORK.record(var0);
   }

   public static long lastTickNanos() {
      return lastTick;
   }
}
