package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum Detail {
   LOW("Low", 7500, 0.42F),
   MEDIUM("Medium", 15000, 0.3F),
   HIGH("High", 30000, 0.21F);

   public final String label;
   public final int budget;
   public final float step;

   private Detail(String nullxx, int nullxxx, float nullxxxx) {
      this.label = nullxx;
      this.budget = nullxxx;
      this.step = nullxxxx;
   }

   public static Detail byLabel(String var0) {
      for (Detail var4 : values()) {
         if (var4.label.equalsIgnoreCase(var0) || var4.name().equalsIgnoreCase(var0)) {
            return var4;
         }
      }

      return MEDIUM;
   }
}
