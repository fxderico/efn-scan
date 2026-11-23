package net.efn.client.util;

import java.util.function.LongSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TickWorkBudget {
   private final LongSupplier clock;
   private long limit;
   private long used;

   public TickWorkBudget(LongSupplier longSupplier) {
      this.clock = longSupplier;
   }

   public void reset(long l) {
      this.limit = Math.max(0L, l);
      this.used = 0L;
   }

   public long acquire(long l) {
      long var3 = this.clock.getAsLong();
      long var5 = Math.max(0L, this.limit - this.used);
      long var7 = Math.min(Math.max(0L, l), Math.min(var5, this.limit / 3L));
      return var3 + var7;
   }

   public void record(long l) {
      this.used = this.used + Math.max(0L, this.clock.getAsLong() - l);
   }

   public long usedNanos() {
      return this.used;
   }
}
