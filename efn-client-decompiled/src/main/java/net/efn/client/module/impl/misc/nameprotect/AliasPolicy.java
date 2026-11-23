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

   public static String sanitize(String string) {
      if (string == null) {
         return "";
      } else {
         StringBuilder var1 = new StringBuilder(Math.min(string.length(), 16));

         for (int var2 = 0; var2 < string.length() && var1.length() < 16; var2++) {
            char var3 = string.charAt(var2);
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

   public static String skeleton(CharSequence charSequence) {
      StringBuilder var1 = new StringBuilder(charSequence.length());

      for (int var2 = 0; var2 < charSequence.length(); var2++) {
         char var3 = Character.toLowerCase(charSequence.charAt(var2));
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

   public static AliasPolicy.Refusal check(String string, Predicate<String> predicate) {
      String var2 = skeleton(string);

      for (String var6 : STAFF_FRAGMENTS) {
         if (var2.contains(var6)) {
            return AliasPolicy.Refusal.STAFF_TITLE;
         }
      }

      if (STAFF_WORDS.contains(var2)) {
         return AliasPolicy.Refusal.STAFF_TITLE;
      } else if (predicate.test(var2)) {
         return AliasPolicy.Refusal.PLAYER_NAME;
      } else {
         for (int var8 = 4; var8 < var2.length(); var8++) {
            if (predicate.test(var2.substring(0, var8)) || predicate.test(var2.substring(var2.length() - var8))) {
               return AliasPolicy.Refusal.PLAYER_NAME;
            }
         }

         for (String var12 : string.split("[ _]+")) {
            if (!var12.isEmpty()) {
               String var7 = skeleton(var12);
               if (STAFF_WORDS.contains(var7)) {
                  return AliasPolicy.Refusal.STAFF_TITLE;
               }

               if (predicate.test(var7)) {
                  return AliasPolicy.Refusal.PLAYER_NAME;
               }
            }
         }

         return null;
      }
   }

   private static String[] skeletons(String... strings) {
      String[] var1 = new String[strings.length];

      for (int var2 = 0; var2 < strings.length; var2++) {
         var1[var2] = skeleton(strings[var2].toLowerCase(Locale.ROOT));
      }

      return var1;
   }

   @Environment(EnvType.CLIENT)
   public static enum Refusal {
      STAFF_TITLE("it reads like a staff or server title"),
      PLAYER_NAME("it is the name of a player seen on this server");

      private final String reason;

      private Refusal(String string2) {
         this.reason = string2;
      }

      public String reason() {
         return this.reason;
      }
   }
}
