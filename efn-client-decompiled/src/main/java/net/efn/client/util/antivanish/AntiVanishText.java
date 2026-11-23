package net.efn.client.util.antivanish;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AntiVanishText {
   private AntiVanishText() {
   }

   public static String normalize(String string) {
      if (string != null && !string.isBlank()) {
         String var2 = Normalizer.normalize(stripLegacyCodes(string), Form.NFKD).toLowerCase(Locale.ROOT);
         StringBuilder var3 = new StringBuilder(var2.length());
         boolean var4 = true;
         int var5 = 0;

         while (var5 < var2.length()) {
            int var1 = var2.codePointAt(var5);
            if (!isMark(var1) && !isFormatCode(var1)) {
               int var6 = foldConfusable(var1);
               if (var6 >= 97 && var6 <= 122 || var6 >= 48 && var6 <= 57) {
                  var3.appendCodePoint(var6);
                  var4 = false;
               }
            }

            var5 += Character.charCount(var1);
         }

         var5 = var3.length();
         if (var5 > 0 && var3.charAt(var5 - 1) == ' ') {
            var3.setLength(var5 - 1);
         }

         return var3.toString();
      } else {
         return "";
      }
   }

   public static boolean containsPlayerName(String string, String string2) {
      String var2 = stripLegacyCodes(string == null ? "" : string).toLowerCase(Locale.ROOT);
      String var3 = stripLegacyCodes(string2 == null ? "" : string2).trim().toLowerCase(Locale.ROOT);
      if (var3.matches("[a-z0-9_]{1,16}")) {
         int var10 = 0;

         while (var10 <= var2.length() - var3.length()) {
            int var6 = var2.indexOf(var3, var10);
            if (var6 < 0) {
               return false;
            }

            int var7 = var6 + var3.length();
            boolean var8 = var6 == 0 || !isUsernameCharacter(var2.charAt(var6 - 1));
            boolean var11 = var7 == var2.length() || !isUsernameCharacter(var2.charAt(var7));
            if (var8 && var11) {
               return true;
            }

            var10 = var6 + 1;
         }

         return false;
      } else {
         String var4 = normalize(string);
         String var5 = normalize(string2);
         return !var4.isBlank() && !var5.isBlank() ? var4.equals(var5) || (" " + var4 + " ").contains(" " + var5 + " ") : false;
      }
   }

   public static boolean looksLikeLeaveMessage(String string, String string2) {
      if (!containsPlayerName(string, string2)) {
         return false;
      } else {
         String var2 = " " + normalize(string) + " ";
         String var3 = normalize(string2);
         if (var3.isBlank()) {
            return false;
         } else {
            String var4 = " " + var3 + " ";
            return var2.contains(var4 + "left the game ")
               || var2.contains(var4 + "left ")
               || var2.contains(var4 + "has left ")
               || var2.contains(var4 + "quit ")
               || var2.contains(var4 + "has quit ")
               || var2.contains(var4 + "disconnected ")
               || var2.contains(var4 + "logged out ");
         }
      }
   }

   public static boolean isPlausiblePlayerName(String string) {
      if (string == null) {
         return false;
      } else {
         String var1 = string.trim();
         int var2 = var1.length();
         return var2 >= 3 && var2 <= 16 ? var1.indexOf(32) < 0 : false;
      }
   }

   public static boolean isUsername(String string) {
      if (string == null) {
         return false;
      } else {
         int var1 = string.length();
         if (var1 >= 2 && var1 <= 32) {
            boolean var2 = false;

            for (int var3 = 0; var3 < var1; var3++) {
               char var5 = string.charAt(var3);
               boolean var4 = var5 >= 'A' && var5 <= 'Z' || var5 >= 'a' && var5 <= 'z' || var5 >= '0' && var5 <= '9';
               if (var4) {
                  var2 = true;
               } else if (var5 != '_' && var5 != '.' && var5 != ' ') {
                  return false;
               }
            }

            return var2;
         } else {
            return false;
         }
      }
   }

   private static boolean isUsernameCharacter(char c) {
      return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_';
   }

   private static boolean isMark(int i) {
      int var1 = Character.getType(i);
      return var1 == 6 || var1 == 8 || var1 == 7;
   }

   private static boolean isFormatCode(int i) {
      return i == 167 || i == 8203 || i == 8204 || i == 8205 || i == 65279;
   }

   private static String stripLegacyCodes(String string) {
      if (string != null && !string.isEmpty()) {
         StringBuilder var1 = new StringBuilder(string.length());

         for (int var2 = 0; var2 < string.length(); var2++) {
            char var4 = string.charAt(var2);
            if (var4 == 167 && var2 + 1 < string.length() && "0123456789abcdefklmnorx".indexOf(Character.toLowerCase(string.charAt(var2 + 1))) >= 0) {
               var2++;
            } else {
               var1.append(var4);
            }
         }

         return var1.toString();
      } else {
         return "";
      }
   }

   private static int foldConfusable(int i) {
      return switch (i) {
         case 593, 945, 1072, 7424 -> 97;
         case 609, 610 -> 103;
         case 618, 953, 1110 -> 105;
         case 628, 1400 -> 110;
         case 640 -> 114;
         case 655, 1091 -> 121;
         case 665, 946, 1074 -> 98;
         case 668, 1085 -> 104;
         case 671 -> 108;
         case 949, 1077, 7431 -> 101;
         case 954, 1082, 7435 -> 107;
         case 957, 7456 -> 118;
         case 959, 1086, 7439 -> 111;
         case 961, 1088, 7448 -> 112;
         case 964, 1090, 7451 -> 116;
         case 965, 7452 -> 117;
         case 1010, 1089, 7428 -> 99;
         case 1084, 7437 -> 109;
         case 1093 -> 120;
         case 1109, 42801 -> 115;
         case 1112, 7434 -> 106;
         case 1121, 7457 -> 119;
         case 1281, 7429 -> 100;
         case 1307 -> 113;
         case 7458, 7459 -> 122;
         case 42800 -> 102;
         default -> i;
      };
   }
}
