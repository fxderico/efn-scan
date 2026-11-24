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

   public static long acquire(long l) {
      return PerformanceModule.optimizationsEnabled() ? WORK.acquire(l) : System.nanoTime() + Math.max(0L, l);
   }

   public static void record(long l) {
      WORK.record(l);
   }

   public static long lastTickNanos() {
      return lastTick;
   }
}
