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

   float update(float var1, float var2) {
      if (!(this.current < 0.0F) && !(var1 >= this.current)) {
         this.narrowerFor += var2;
         if (this.narrowerFor < 1.5F) {
            return this.current;
         } else {
            this.current = this.current + (var1 - this.current) * (1.0F - (float)Math.exp(-12.0F * var2));
            if (Math.abs(this.current - var1) < 0.25F) {
               this.current = var1;
            }

            return this.current;
         }
      } else {
         this.current = var1;
         this.narrowerFor = 0.0F;
         return this.current;
      }
   }
}
