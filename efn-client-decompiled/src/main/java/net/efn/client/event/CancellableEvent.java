package net.efn.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class CancellableEvent extends Event {
   boolean cancelled;
   boolean postStage;

   public CancellableEvent() {
   }

   public final void setCancelled(boolean var1) {
      if (!this.postStage) {
         this.cancelled = var1;
      }
   }

   public final boolean isCancelled() {
      return this.cancelled;
   }

   protected void resetCancellation() {
      this.cancelled = false;
      this.postStage = false;
   }
}
