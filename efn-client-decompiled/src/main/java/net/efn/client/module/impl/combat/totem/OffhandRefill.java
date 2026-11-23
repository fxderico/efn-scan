package net.efn.client.module.impl.combat.totem;

import java.util.random.RandomGenerator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class OffhandRefill {
   private static final int MAX_ATTEMPTS = 3;
   private static final long MS = 1000000L;
   private final OffhandRefill.Driver driver;
   private final RandomGenerator random;
   private OffhandRefill.Phase phase = OffhandRefill.Phase.IDLE;
   private long due;
   private int source = -1;
   private double tempo = 1.0;
   private boolean occupiedSinceSwap;
   private OffhandRefill.Status status = OffhandRefill.Status.IDLE;
   private int attempts;
   private int sentContainer = -1;
   private int seenState = -1;
   private long startedAt;
   private long deadline;
   private long retryAt;
   private long retryPause;

   public OffhandRefill(OffhandRefill.Driver var1, RandomGenerator var2) {
      this.driver = var1;
      this.random = var2;
   }

   public void reset() {
      this.cancelSchedule();
      this.resetConfirmation();
      this.occupiedSinceSwap = false;
   }

   public boolean busy() {
      return this.phase != OffhandRefill.Phase.IDLE || this.status == OffhandRefill.Status.WAITING;
   }

   public void step(long var1, int var3) {
      if (!this.driver.canInteract()) {
         this.reset();
      } else {
         this.expire(var1);
         if (!this.driver.needsTotem()) {
            this.occupiedSinceSwap = true;
            this.cancelSchedule();
         } else {
            if (this.occupiedSinceSwap) {
               if (this.status != OffhandRefill.Status.EMPTY && this.status != OffhandRefill.Status.GAVE_UP) {
                  this.resetConfirmation();
               }

               this.occupiedSinceSwap = false;
            }

            if (!this.canAttempt(var1)) {
               this.cancelSchedule();
            } else {
               int var4 = Math.max(1, Math.min(10, var3));
               if (var4 == 1) {
                  int var5 = this.driver.findSource();
                  if (var5 >= 0 && this.driver.validSource(var5)) {
                     this.equip(var1, var5);
                  }
               } else if (var1 >= this.due) {
                  switch (this.phase) {
                     case IDLE:
                        if (this.driver.findSource() < 0) {
                           return;
                        }

                        this.tempo = 0.94 + this.random.nextDouble() * 0.12;
                        this.phase = OffhandRefill.Phase.NOTICE;
                        this.due = var1 + this.sample(10 + (var4 - 1) * 8, 20 + (var4 - 1) * 13);
                        break;
                     case NOTICE:
                        this.source = this.driver.findSource();
                        if (this.source < 0) {
                           this.cancelSchedule();
                           return;
                        }

                        this.phase = OffhandRefill.Phase.TARGET;
                        this.due = var1 + this.sample(6.0 + (var4 - 1) * 2.5, 12.0 + (var4 - 1) * 4.5);
                        break;
                     case TARGET:
                        if (!this.driver.validSource(this.source)) {
                           this.phase = OffhandRefill.Phase.NOTICE;
                           this.due = var1 + this.sample(6.0, 14.0);
                           return;
                        }

                        this.equip(var1, this.source);
                  }
               }
            }
         }
      }
   }

   public void observe(int var1, int var2, OffhandRefill.Offhand var3, long var4) {
      if (this.attempts != 0 && var4 >= this.startedAt) {
         if (var1 == -1 || var1 == this.sentContainer) {
            if (var2 == -1 || var2 >= this.seenState) {
               if (this.status == OffhandRefill.Status.WAITING
                  || this.status == OffhandRefill.Status.UNCONFIRMED
                  || this.status == OffhandRefill.Status.EMPTY
                  || this.status == OffhandRefill.Status.GAVE_UP) {
                  if (var2 != -1) {
                     this.seenState = var2;
                  }
                  this.status = switch (var3) {
                     case EMPTY -> this.attempts >= 3 ? OffhandRefill.Status.GAVE_UP : OffhandRefill.Status.EMPTY;
                     case TOTEM -> OffhandRefill.Status.CONFIRMED;
                     case OTHER -> OffhandRefill.Status.OCCUPIED;
                  };
                  this.retryAt = var4 + this.retryPause;
               }
            }
         }
      }
   }

   private void equip(long var1, int var3) {
      if (this.driver.needsTotem() && this.driver.canInteract() && this.driver.validSource(var3)) {
         this.begin(var1, this.driver.containerId(), this.driver.stateId(), this.driver.latencyMillis());
         this.cancelSchedule();
         this.driver.swap(var3);
         this.occupiedSinceSwap = !this.driver.needsTotem();
      } else {
         this.cancelSchedule();
      }
   }

   private long sample(double var1, double var3) {
      return Math.round((var1 + (var3 - var1) * (this.random.nextDouble() + this.random.nextDouble()) / 2.0) * this.tempo * 1000000.0);
   }

   private void cancelSchedule() {
      this.phase = OffhandRefill.Phase.IDLE;
      this.source = -1;
      this.due = 0L;
   }

   private void resetConfirmation() {
      this.status = OffhandRefill.Status.IDLE;
      this.attempts = 0;
      this.sentContainer = -1;
      this.seenState = -1;
      this.retryPause = 0L;
      this.retryAt = 0L;
      this.deadline = 0L;
      this.startedAt = 0L;
   }

   private void begin(long var1, int var3, int var4, int var5) {
      this.attempts++;
      this.sentContainer = var3;
      this.seenState = var4;
      this.startedAt = var1;
      long var6 = Math.max(0, var5);
      this.deadline = var1 + Math.min(5000L, Math.max(750L, var6 * 3L + 250L)) * 1000000L;
      this.retryPause = Math.min(500L, Math.max(100L, var6)) * 1000000L;
      this.status = OffhandRefill.Status.WAITING;
   }

   private void expire(long var1) {
      if (this.status == OffhandRefill.Status.WAITING && var1 >= this.deadline) {
         this.status = OffhandRefill.Status.UNCONFIRMED;
         this.retryAt = var1 + this.retryPause;
      }
   }

   private boolean canAttempt(long var1) {
      this.expire(var1);
      if (this.status == OffhandRefill.Status.IDLE) {
         return true;
      } else if (this.status != OffhandRefill.Status.EMPTY && this.status != OffhandRefill.Status.UNCONFIRMED) {
         return false;
      } else if (this.attempts >= 3) {
         this.status = OffhandRefill.Status.GAVE_UP;
         return false;
      } else {
         return var1 >= this.retryAt;
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Driver {
      boolean needsTotem();

      boolean canInteract();

      int findSource();

      boolean validSource(int var1);

      void swap(int var1);

      int containerId();

      int stateId();

      int latencyMillis();
   }

   @Environment(EnvType.CLIENT)
   public static enum Offhand {
      EMPTY,
      TOTEM,
      OTHER;

      private Offhand() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Phase {
      IDLE,
      NOTICE,
      TARGET;

      private Phase() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Status {
      IDLE,
      WAITING,
      CONFIRMED,
      EMPTY,
      OCCUPIED,
      UNCONFIRMED,
      GAVE_UP;

      private Status() {
      }
   }
}
