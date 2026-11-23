package net.efn.client.util.spotify;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.function.IntPredicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class DisplayText {
   private DisplayText() {
   }

   public static String fold(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         StringBuilder var1 = null;
         boolean var2 = false;

         for (int var3 = 0; var3 < var0.length(); var3++) {
            char var4 = var0.charAt(var3);
            String var5 = replacement(var4);
            if (var5 != null || var4 == ' ' && var2) {
               if (var1 == null) {
                  var1 = new StringBuilder(var0.length()).append(var0, 0, var3);
               }

               if (var5 != null) {
                  for (int var6 = 0; var6 < var5.length(); var6++) {
                     char var7 = var5.charAt(var6);
                     if (var7 != ' ' || !var2 && !var1.isEmpty()) {
                        var1.append(var7);
                        var2 = var7 == ' ';
                     }
                  }
               }
            } else {
               if (var1 != null) {
                  var1.append(var4);
               }

               var2 = var4 == ' ';
            }
         }

         if (var1 == null) {
            String var8 = var0.strip();
            return var8.length() == var0.length() ? var0 : var8;
         } else {
            return var1.toString().strip();
         }
      } else {
         return "";
      }
   }

   public static String fold(String var0, IntPredicate var1) {
      String var2 = fold(var0);
      StringBuilder var3 = null;
      int var4 = 0;

      while (var4 < var2.length()) {
         int var6 = var2.codePointAt(var4);
         int var7 = var4 + Character.charCount(var6);
         String var5 = var6 >= 128 && Character.isLetter(var6) && !var1.test(var6) ? baseLetters(var6) : null;
         if (var5 != null && !var5.codePoints().allMatch(var1x -> var1x < 128 || var1.test(var1x))) {
            var5 = null;
         }

         if (var5 != null && var3 == null) {
            var3 = new StringBuilder(var2.length()).append(var2, 0, var4);
         }

         if (var3 != null) {
            if (var5 != null) {
               var3.append(var5);
            } else {
               var3.append(var2, var4, var7);
            }
         }

         var4 = var7;
      }

      return var3 == null ? var2 : var3.toString();
   }

   static String baseLetters(int var0) {
      String var1 = switch (var0) {
         case 272 -> "D";
         case 273 -> "d";
         case 294 -> "H";
         case 295 -> "h";
         case 306 -> "IJ";
         case 307 -> "ij";
         case 319 -> "L";
         case 320 -> "l";
         case 321 -> "L";
         case 322 -> "l";
         case 338 -> "OE";
         case 339 -> "oe";
         case 358 -> "T";
         case 359 -> "t";
         case 383 -> "s";
         default -> null;
      };
      if (var1 != null) {
         return var1;
      } else {
         String var2 = Normalizer.normalize(new String(Character.toChars(var0)), Form.NFD);
         StringBuilder var3 = new StringBuilder(var2.length());
         var2.codePoints().filter(var0x -> Character.getType(var0x) != 6).forEach(var3::appendCodePoint);
         return !var3.isEmpty() && var3.length() != var2.length() ? var3.toString() : null;
      }
   }

   private static String replacement(char var0) {
      return switch (var0) {
         case '\t', '\u00a0', '\u2000', '\u2001', '\u2002', '\u2003', '\u2004', '\u2005', '\u2006', '\u2007', '\u2008', '\u2009', '\u200a', '\u202f', '\u205f', '\u3000' -> " ";
         case '\u200b', '\u200c', '\u200d', '\u2060', '\u2669', '\u266a', '\u266b', '\u266c', '\ufeff' -> "";
         case '\u2010', '\u2011', '\u2012', '\u2013', '\u2014', '\u2015', '\u2212' -> "-";
         case '\u2018', '\u2019', '\u201a', '\u201b', '\u2032' -> "'";
         case '\u201c', '\u201d', '\u201e', '\u201f', '\u2033' -> "\"";
         case '\u2022', '\u2027', '\u2219' -> "\u00b7";
         case '\u2026' -> "...";
         default -> null;
      };
   }
}
