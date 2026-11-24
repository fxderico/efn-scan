package net.efn.client.util;

import java.util.function.UnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class NameFilter {
   private static volatile UnaryOperator<String> filter = UnaryOperator.identity();

   private NameFilter() {
   }

   public static String apply(String string) {
      if (string == null) {
         return null;
      } else {
         String var1 = filter.apply(string);
         return var1 != null ? var1 : string;
      }
   }

   public static void set(UnaryOperator<String> unaryOperator) {
      filter = unaryOperator != null ? unaryOperator : UnaryOperator.identity();
   }
}
