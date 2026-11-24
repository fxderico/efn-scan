package net.efn.client.util.playerbypass;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AutoRtpLoop {
   public static final long MIN_DELAY_MS = 3000L;
   public static final long MIN_SCAN_MS = 2000L;
   public static final long MIN_TIMEOUT_MS = 3000L;
   public static final long COOLDOWN_MARGIN_MS = 1000L;
   public static final long MAX_COOLDOWN_MS = 900000L;
   public static final long MAX_WARMUP_MS = 60000L;
   public static final long REJECT_GRACE_MS = 2000L;
   public static final long SETTLE_MS = 1500L;
   public static final long MAX_SETTLE_MS = 8000L;
   public static final long LATE_LANDING_MS = 5000L;
   public static final int MAX_FAILS_IN_ROW = 5;
   private static final long NEVER = -2305843009213693952L;
   private AutoRtpLoop.Phase phase = AutoRtpLoop.Phase.OFF;
   private int sent;
   private int landings;
   private int failsInRow;
   private boolean rejected;
   private boolean tryLanded;
   private boolean tryRefunded;
   private long lastSendAt = -2305843009213693952L;
   private long cooldownUntil;
   private long nextSendAt;
   private long holdUntil;
   private long deadline;
   private long scanUntil;
   private long waitingSince;
   private long tryEndedAt = -2305843009213693952L;
   private long lastDataAt = -2305843009213693952L;

   public AutoRtpLoop() {
   }

   public void start(long l, AutoRtpLoop.Config config) {
      this.phase = AutoRtpLoop.Phase.WAITING;
      this.sent = 0;
      this.landings = 0;
      this.failsInRow = 0;
      this.rejected = false;
      this.tryLanded = false;
      this.tryRefunded = false;
      this.nextSendAt = this.lastSendAt == -2305843009213693952L ? l : Math.max(l, this.lastSendAt + config.delayMs());
      this.holdUntil = 0L;
      this.waitingSince = l;
      this.tryEndedAt = -2305843009213693952L;
      this.lastDataAt = -2305843009213693952L;
   }

   public void stop() {
      this.phase = AutoRtpLoop.Phase.OFF;
   }

   public void forgetCooldown() {
      this.cooldownUntil = 0L;
   }

   public AutoRtpLoop.Step tick(long l, AutoRtpLoop.Config config) {
      switch (this.phase) {
         case WAITING:
            if (l < this.sendAt()) {
               return AutoRtpLoop.Step.NONE;
            } else {
               if (config.maxRtps() > 0 && this.sent >= config.maxRtps()) {
                  return AutoRtpLoop.Step.LIMIT;
               }

               this.sent++;
               this.lastSendAt = l;
               this.deadline = l + config.timeoutMs();
               this.rejected = false;
               this.tryLanded = false;
               this.tryRefunded = false;
               this.phase = AutoRtpLoop.Phase.TELEPORTING;
               return AutoRtpLoop.Step.SEND;
            }
         case TELEPORTING:
            if (l < this.deadline) {
               return AutoRtpLoop.Step.NONE;
            } else {
               if (this.rejected) {
                  this.sent = Math.max(0, this.sent - 1);
                  this.tryRefunded = true;
               } else {
                  if (this.failsInRow + 1 >= 5) {
                     this.failsInRow = 5;
                     return AutoRtpLoop.Step.GAVE_UP;
                  }

                  this.failsInRow++;
               }

               this.toWaiting(l, config);
               return AutoRtpLoop.Step.TIMED_OUT;
            }
         case SCANNING:
            if (l < this.scanUntil) {
               return AutoRtpLoop.Step.NONE;
            }

            this.toWaiting(l, config);
            return AutoRtpLoop.Step.SCAN_CLEAR;
         default:
            return AutoRtpLoop.Step.NONE;
      }
   }

   public void landed(long l, AutoRtpLoop.Config config) {
      if (this.phase == AutoRtpLoop.Phase.TELEPORTING) {
         this.phase = AutoRtpLoop.Phase.SCANNING;
         this.landings++;
         this.failsInRow = 0;
         this.rejected = false;
         this.tryLanded = true;
         this.scanUntil = l + config.scanMs();
      }
   }

   public void relanded(long l, AutoRtpLoop.Config config) {
      if (this.phase == AutoRtpLoop.Phase.SCANNING) {
         this.scanUntil = Math.max(this.scanUntil, l + config.scanMs());
      }
   }

   public boolean landedLate(long l, AutoRtpLoop.Config config) {
      if (this.phase == AutoRtpLoop.Phase.WAITING && this.tryEndedAt != -2305843009213693952L && l - this.tryEndedAt <= 5000L) {
         if (!this.tryLanded) {
            this.landings++;
            if (this.tryRefunded) {
               this.sent++;
            }
         }

         this.phase = AutoRtpLoop.Phase.SCANNING;
         this.failsInRow = 0;
         this.rejected = false;
         this.tryLanded = true;
         this.tryRefunded = false;
         this.tryEndedAt = -2305843009213693952L;
         this.scanUntil = l + config.scanMs();
         return true;
      } else {
         return false;
      }
   }

   public void cooldown(long l, long m) {
      if (this.phase != AutoRtpLoop.Phase.OFF) {
         this.nextUseIn(l, m);
         if (this.phase == AutoRtpLoop.Phase.TELEPORTING) {
            this.rejected = true;
            this.deadline = Math.min(this.deadline, l + 2000L);
         }
      }
   }

   public void nextUseIn(long l, long m) {
      if (this.phase != AutoRtpLoop.Phase.OFF) {
         long var5 = Math.min(900000L, Math.max(0L, m)) + 1000L;
         this.cooldownUntil = Math.max(this.cooldownUntil, l + var5);
      }
   }

   public void warmup(long l, long m, AutoRtpLoop.Config config) {
      if (this.phase == AutoRtpLoop.Phase.TELEPORTING && !this.rejected) {
         this.deadline = Math.max(this.deadline, l + Math.min(60000L, Math.max(0L, m)) + config.timeoutMs());
      }
   }

   public void extend(long l, AutoRtpLoop.Config config) {
      if (this.phase == AutoRtpLoop.Phase.TELEPORTING) {
         this.deadline = Math.max(this.deadline, l + config.timeoutMs());
      }
   }

   public void cancelled(long l) {
      if (this.phase == AutoRtpLoop.Phase.TELEPORTING && !this.rejected) {
         this.deadline = Math.min(this.deadline, l);
      }
   }

   public void interrupt(long l, AutoRtpLoop.Config config) {
      if (this.phase == AutoRtpLoop.Phase.TELEPORTING || this.phase == AutoRtpLoop.Phase.SCANNING) {
         this.toWaiting(l, config);
      }
   }

   public void holdUntil(long l) {
      this.holdUntil = Math.max(this.holdUntil, l);
   }

   public void dataArrived(long l) {
      this.lastDataAt = l;
   }

   private void toWaiting(long l, AutoRtpLoop.Config config) {
      this.phase = AutoRtpLoop.Phase.WAITING;
      this.rejected = false;
      this.nextSendAt = Math.max(l, this.lastSendAt + config.delayMs());
      this.waitingSince = l;
      this.tryEndedAt = l;
   }

   private long readyAt() {
      long var1 = Math.max(this.nextSendAt, Math.max(this.holdUntil, this.cooldownUntil));
      return this.lastSendAt == -2305843009213693952L ? var1 : Math.max(var1, this.lastSendAt + 3000L);
   }

   private long settleUntil() {
      return this.lastDataAt == -2305843009213693952L ? 0L : Math.min(this.lastDataAt + 1500L, this.waitingSince + 8000L);
   }

   private long sendAt() {
      return Math.max(this.readyAt(), this.settleUntil());
   }

   public AutoRtpLoop.Phase phase() {
      return this.phase;
   }

   public boolean running() {
      return this.phase != AutoRtpLoop.Phase.OFF;
   }

   public boolean awaitingTeleport() {
      return this.phase == AutoRtpLoop.Phase.TELEPORTING && !this.rejected;
   }

   public int sent() {
      return this.sent;
   }

   public int landings() {
      return this.landings;
   }

   public boolean sentWithin(long l, long m) {
      return this.lastSendAt != -2305843009213693952L && l - this.lastSendAt <= m;
   }

   public long nextSendIn(long l) {
      return Math.max(0L, this.sendAt() - l);
   }

   public boolean settling(long l) {
      return this.phase == AutoRtpLoop.Phase.WAITING && l >= this.readyAt() && l < this.settleUntil();
   }

   public long cooldownLeft(long l) {
      return Math.max(0L, this.cooldownUntil - l);
   }

   public long scanLeft(long l) {
      return this.phase == AutoRtpLoop.Phase.SCANNING ? Math.max(0L, this.scanUntil - l) : 0L;
   }

   @Environment(EnvType.CLIENT)
   public record Config(long scanMs, long delayMs, long timeoutMs, int maxRtps) {
      public Config(long scanMs, long delayMs, long timeoutMs, int maxRtps) {
         scanMs = Math.max(2000L, scanMs);
         delayMs = Math.max(3000L, delayMs);
         timeoutMs = Math.max(3000L, timeoutMs);
         maxRtps = Math.max(0, maxRtps);
         this.scanMs = scanMs;
         this.delayMs = delayMs;
         this.timeoutMs = timeoutMs;
         this.maxRtps = maxRtps;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Phase {
      OFF,
      WAITING,
      TELEPORTING,
      SCANNING;

      private Phase() {
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Step {
      NONE,
      SEND,
      TIMED_OUT,
      SCAN_CLEAR,
      LIMIT,
      GAVE_UP;

      private Step() {
      }
   }
}
