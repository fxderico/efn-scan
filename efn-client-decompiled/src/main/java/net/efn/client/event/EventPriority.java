package net.efn.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum EventPriority {
   LOWEST(-100),
   LOW(-50),
   NORMAL(0),
   HIGH(50),
   HIGHEST(100);

   public final int value;

   private EventPriority(int nullxx) {
      this.value = nullxx;
   }

   public static EventPriority fromInt(int var0) {
      if (var0 >= 75) {
         return HIGHEST;
      } else if (var0 >= 25) {
         return HIGH;
      } else if (var0 >= -25) {
         return NORMAL;
      } else {
         return var0 >= -75 ? LOW : LOWEST;
      }
   }
}
