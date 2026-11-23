package net.efn.client.module;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class StableId {
   private StableId() {
   }

   public static String validate(String var0) {
      if (!isValid(var0)) {
         throw new IllegalArgumentException("Invalid stable id: " + var0);
      } else {
         return var0;
      }
   }

   public static String legacy(String var0) {
      String var1 = var0.toLowerCase(Locale.ROOT);
      StringBuilder var2 = new StringBuilder(var1.length());
      boolean var3 = false;

      for (int var4 = 0; var4 < var1.length(); var4++) {
         char var5 = var1.charAt(var4);
         if (var5 >= 'a' && var5 <= 'z' || var5 >= '0' && var5 <= '9') {
            if (var3 && !var2.isEmpty()) {
               var2.append('-');
            }

            var3 = false;
            var2.append(var5);
         } else {
            var3 = true;
         }
      }

      return validate(var2.toString());
   }

   private static boolean isValid(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         boolean var1 = true;

         for (int var2 = 0; var2 < var0.length(); var2++) {
            char var3 = var0.charAt(var2);
            if ((var3 < 'a' || var3 > 'z') && (var3 < '0' || var3 > '9')) {
               if (var3 != '.' && var3 != '_' && var3 != '-') {
                  return false;
               }

               if (var1) {
                  return false;
               }

               var1 = true;
            } else {
               var1 = false;
            }
         }

         return !var1;
      } else {
         return false;
      }
   }
}
