package net.efn.client.module.impl.misc.nameprotect;

import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AliasPolicy {
   public static final int MAX_LENGTH = 16;
   private static final int MIN_GLUED_NAME = 4;
   private static final String[] STAFF_FRAGMENTS = skeletons(
      "admin", "owner", "moderat", "staff", "helper", "founder", "manager", "develop", "console", "support", "operator", "official", "donutsmp"
   );
   private static final Set<String> STAFF_WORDS = Set.of(
      skeletons("mod", "mods", "srmod", "jrmod", "trialmod", "headmod", "dev", "devs", "op", "ops", "server", "system", "builder")
   );

   private AliasPolicy() {
   }

   public static String sanitize(String var0) {
      if (var0 == null) {
         return "";
      } else {
         StringBuilder var1 = new StringBuilder(Math.min(var0.length(), 16));

         for (int var2 = 0; var2 < var0.length() && var1.length() < 16; var2++) {
            char var3 = var0.charAt(var2);
            if (var3 == 167) {
               var2++;
            } else if (NameMatcher.isNameChar(var3)) {
               var1.append(var3);
            } else if (var1.length() > 0 && var1.charAt(var1.length() - 1) != ' ') {
               var1.append(' ');
            }
         }

         return var1.toString().trim();
      }
   }

   public static String skeleton(CharSequence var0) {
      StringBuilder var1 = new StringBuilder(var0.length());

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = Character.toLowerCase(var0.charAt(var2));
         switch (var3) {
            case ' ':
            case '_':
               break;
            case '0':
               var1.append('o');
               break;
            case '1':
            case 'i':
               var1.append('l');
               break;
            case '3':
               var1.append('e');
               break;
            case '4':
               var1.append('a');
               break;
            case '5':
               var1.append('s');
               break;
            case '7':
               var1.append('t');
               break;
            default:
               var1.append(var3);
         }
      }

      return var1.toString();
   }

   public static AliasPolicy.Refusal check(String var0, Predicate<String> var1) {
      String var2 = skeleton(var0);

      for (String var6 : STAFF_FRAGMENTS) {
         if (var2.contains(var6)) {
            return AliasPolicy.Refusal.STAFF_TITLE;
         }
      }

      if (STAFF_WORDS.contains(var2)) {
         return AliasPolicy.Refusal.STAFF_TITLE;
      } else if (var1.test(var2)) {
         return AliasPolicy.Refusal.PLAYER_NAME;
      } else {
         for (int var8 = 4; var8 < var2.length(); var8++) {
            if (var1.test(var2.substring(0, var8)) || var1.test(var2.substring(var2.length() - var8))) {
               return AliasPolicy.Refusal.PLAYER_NAME;
            }
         }

         for (String var12 : var0.split("[ _]+")) {
            if (!var12.isEmpty()) {
               String var7 = skeleton(var12);
               if (STAFF_WORDS.contains(var7)) {
                  return AliasPolicy.Refusal.STAFF_TITLE;
               }

               if (var1.test(var7)) {
                  return AliasPolicy.Refusal.PLAYER_NAME;
               }
            }
         }

         return null;
      }
   }

   private static String[] skeletons(String... var0) {
      String[] var1 = new String[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = skeleton(var0[var2].toLowerCase(Locale.ROOT));
      }

      return var1;
   }

   @Environment(EnvType.CLIENT)
   public static enum Refusal {
      STAFF_TITLE("it reads like a staff or server title"),
      PLAYER_NAME("it is the name of a player seen on this server");

      private final String reason;

      private Refusal(String nullxx) {
         this.reason = nullxx;
      }

      public String reason() {
         return this.reason;
      }
   }
}
