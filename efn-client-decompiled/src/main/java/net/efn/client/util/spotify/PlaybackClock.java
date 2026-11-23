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

   public void update(NowPlaying nowPlaying, long l) {
      if (nowPlaying != this.sample) {
         NowPlaying var4 = this.sample;
         this.sample = nowPlaying;
         if (nowPlaying != null && nowPlaying.hasTimeline()) {
            double var5 = this.position(l);
            double var7 = nowPlaying.positionAt(l);
            boolean var9 = var4 != null
               && var4.hasTimeline()
               && var4.sameTrack(nowPlaying)
               && this.running
               && nowPlaying.playing()
               && Math.abs(var7 - var5) < 1200.0 * Math.min(1.0, nowPlaying.rate());
            this.base = var9 ? var5 : var7;
            this.correction = var9 ? var7 - var5 : 0.0;
            this.baseAt = l;
            this.running = nowPlaying.playing();
            this.durationMs = nowPlaying.durationMs();
            this.rate = nowPlaying.rate();
         } else {
            this.running = false;
            this.durationMs = 0L;
            this.base = 0.0;
            this.correction = 0.0;
            this.baseAt = l;
         }
      }
   }

   public long positionAt(long l) {
      return this.durationMs <= 0L ? 0L : Math.max(0L, Math.min(this.durationMs, Math.round(this.position(l))));
   }

   private double position(long l) {
      if (this.durationMs <= 0L) {
         return 0.0;
      } else if (!this.running) {
         return this.base;
      } else {
         double var3 = Math.max(0L, l - this.baseAt);
         return this.base + var3 * this.rate + this.correction * Math.min(1.0, var3 / 1500.0);
      }
   }
}
