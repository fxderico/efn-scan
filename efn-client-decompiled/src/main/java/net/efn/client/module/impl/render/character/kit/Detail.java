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

   private Detail(String string2, int j, float f) {
      this.label = string2;
      this.budget = j;
      this.step = f;
   }

   public static Detail byLabel(String string) {
      for (Detail var4 : values()) {
         if (var4.label.equalsIgnoreCase(string) || var4.name().equalsIgnoreCase(string)) {
            return var4;
         }
      }

      return MEDIUM;
   }
}
