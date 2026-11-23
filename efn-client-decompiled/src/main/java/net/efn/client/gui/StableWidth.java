package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class StableWidth {
   static final float SHRINK_DELAY = 1.5F;
   private static final float SHRINK_RATE = 12.0F;
   private static final float SNAP = 0.25F;
   private float current = -1.0F;
   private float narrowerFor;

   StableWidth() {
   }

   float update(float f, float g) {
      if (!(this.current < 0.0F) && !(f >= this.current)) {
         this.narrowerFor += g;
         if (this.narrowerFor < 1.5F) {
            return this.current;
         } else {
            this.current = this.current + (f - this.current) * (1.0F - (float)Math.exp(-12.0F * g));
            if (Math.abs(this.current - f) < 0.25F) {
               this.current = f;
            }

            return this.current;
         }
      } else {
         this.current = f;
         this.narrowerFor = 0.0F;
         return this.current;
      }
   }
}
