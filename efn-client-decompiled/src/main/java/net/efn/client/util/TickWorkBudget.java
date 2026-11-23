package net.efn.client.util;

import java.util.function.LongSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TickWorkBudget {
   private final LongSupplier clock;
   private long limit;
   private long used;

   public TickWorkBudget(LongSupplier var1) {
      this.clock = var1;
   }

   public void reset(long var1) {
      this.limit = Math.max(0L, var1);
      this.used = 0L;
   }

   public long acquire(long var1) {
      long var3 = this.clock.getAsLong();
      long var5 = Math.max(0L, this.limit - this.used);
      long var7 = Math.min(Math.max(0L, var1), Math.min(var5, this.limit / 3L));
      return var3 + var7;
   }

   public void record(long var1) {
      this.used = this.used + Math.max(0L, this.clock.getAsLong() - var1);
   }

   public long usedNanos() {
      return this.used;
   }
}
