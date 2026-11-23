package net.efn.client.service;

import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TickJitter {
   private static final float VARIANCE = 0.2F;

   private TickJitter() {
   }

   public static int apply(int var0) {
      if (var0 <= 0) {
         return var0;
      } else {
         int var1 = Math.max(1, Math.round(var0 * 0.2F));
         int var2 = ThreadLocalRandom.current().nextInt(-var1, var1 + 1);
         return Math.max(1, var0 + var2);
      }
   }
}
