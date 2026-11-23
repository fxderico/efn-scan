package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PlaybackClock {
   static final long SNAP_MS = 1200L;
   static final long CATCHUP_MS = 1500L;
   private NowPlaying sample;
   private boolean running;
   private long durationMs;
   private double rate = 1.0;
   private double base;
   private long baseAt;
   private double correction;

   public PlaybackClock() {
   }

   public void update(NowPlaying var1, long var2) {
      if (var1 != this.sample) {
         NowPlaying var4 = this.sample;
         this.sample = var1;
         if (var1 != null && var1.hasTimeline()) {
            double var5 = this.position(var2);
            double var7 = var1.positionAt(var2);
            boolean var9 = var4 != null
               && var4.hasTimeline()
               && var4.sameTrack(var1)
               && this.running
               && var1.playing()
               && Math.abs(var7 - var5) < 1200.0 * Math.min(1.0, var1.rate());
            this.base = var9 ? var5 : var7;
            this.correction = var9 ? var7 - var5 : 0.0;
            this.baseAt = var2;
            this.running = var1.playing();
            this.durationMs = var1.durationMs();
            this.rate = var1.rate();
         } else {
            this.running = false;
            this.durationMs = 0L;
            this.base = 0.0;
            this.correction = 0.0;
            this.baseAt = var2;
         }
      }
   }

   public long positionAt(long var1) {
      return this.durationMs <= 0L ? 0L : Math.max(0L, Math.min(this.durationMs, Math.round(this.position(var1))));
   }

   private double position(long var1) {
      if (this.durationMs <= 0L) {
         return 0.0;
      } else if (!this.running) {
         return this.base;
      } else {
         double var3 = Math.max(0L, var1 - this.baseAt);
         return this.base + var3 * this.rate + this.correction * Math.min(1.0, var3 / 1500.0);
      }
   }
}
