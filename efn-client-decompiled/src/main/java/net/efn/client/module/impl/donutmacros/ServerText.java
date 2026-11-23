package net.efn.client.module.impl.donutmacros;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
final class ServerText {
   private static final Pattern FORMATTING = Pattern.compile("\u00a7.");
   private static final Pattern MONEY_EXACT = Pattern.compile("^\\$?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kmb])?$");
   private static final Pattern MONEY_ANY = Pattern.compile("\\$?[ \\t]*([0-9][0-9,]*(?:\\.[0-9]+)?)[ \\t]*([kmb])?(?![a-z])");
   private static final Pattern DOLLARS = Pattern.compile("\\$[ \\t]*([0-9][0-9,]*(?:\\.[0-9]+)?)[ \\t]*([kmb])?(?![a-z])");
   private static final String[] NOT_PRICES = new String[]{"balance", "you have", "your money", "wallet", "bank"};
   private static final String SMALL_CAPS = "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u0280\u0455\ua731\u1d1b\u1d1c\u1d20\u1d21\u028f\u1d22";
   private static final String SMALL_CAPS_PLAIN = "abcdefghijklmnoprsstuvwyz";
   private static final String[] ROMAN = new String[]{"i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x"};

   private ServerText() {
   }

   static String normalize(String var0) {
      if (var0 == null) {
         return "";
      } else {
         String var1 = FORMATTING.matcher(var0).replaceAll("");
         StringBuilder var2 = new StringBuilder(var1.length());

         for (int var3 = 0; var3 < var1.length(); var3++) {
            char var4 = var1.charAt(var3);
            int var5 = "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u0280\u0455\ua731\u1d1b\u1d1c\u1d20\u1d21\u028f\u1d22"
               .indexOf(var4);
            if (var5 >= 0) {
               var2.append("abcdefghijklmnoprsstuvwyz".charAt(var5));
            } else if (var4 != 10140 && var4 != 8594) {
               var2.append(var4);
            } else {
               var2.append('-');
            }
         }

         return var2.toString().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
      }
   }

   static String normalize(Text var0) {
      return var0 == null ? "" : normalize(var0.getString());
   }

   static String key(String var0) {
      String var1 = normalize(var0);
      StringBuilder var2 = new StringBuilder(var1.length());

      for (int var3 = 0; var3 < var1.length(); var3++) {
         char var4 = var1.charAt(var3);
         if (var4 >= 'a' && var4 <= 'z' || var4 >= '0' && var4 <= '9') {
            var2.append(var4);
         }
      }

      return var2.toString();
   }

   static long parseMoney(String var0) {
      if (var0 == null) {
         return -1L;
      } else {
         Matcher var1 = MONEY_EXACT.matcher(var0.trim().toLowerCase(Locale.ROOT));
         return !var1.matches() ? -1L : Math.round(scale(var1.group(1), var1.group(2)));
      }
   }

   static List<Double> moneyAll(String var0) {
      ArrayList var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         Matcher var2 = MONEY_ANY.matcher(normalize(var0));

         while (var2.find()) {
            double var3 = scale(var2.group(1), var2.group(2));
            if (var3 > 0.0) {
               var1.add(var3);
            }
         }

         return var1;
      }
   }

   static List<Double> dollars(String var0) {
      ArrayList var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         Matcher var2 = DOLLARS.matcher(normalize(var0));

         while (var2.find()) {
            double var3 = scale(var2.group(1), var2.group(2));
            if (var3 > 0.0) {
               var1.add(var3);
            }
         }

         return var1;
      }
   }

   static double askingPrice(List<String> var0) {
      double var1 = -1.0;

      for (String var4 : var0) {
         String var5 = normalize(var4);
         boolean var6 = false;

         for (String var10 : NOT_PRICES) {
            if (var5.contains(var10)) {
               var6 = true;
               break;
            }
         }

         if (!var6) {
            for (double var13 : var5.contains("$") ? dollars(var5) : (!var5.contains("price") && !var5.contains("cost") ? List.of() : moneyAll(var5))) {
               if (var13 > var1) {
                  var1 = var13;
               }
            }
         }
      }

      return var1;
   }

   static double askingPrice(ItemStack var0) {
      return askingPrice(lore(var0));
   }

   static String moneyToken(String var0) {
      Matcher var1 = MONEY_ANY.matcher(normalize(var0));
      if (!var1.find()) {
         return null;
      } else {
         String var2 = var1.group(2);
         return var1.group(1).replace(",", "") + (var2 == null ? "" : var2);
      }
   }

   static double displayStep(String var0) {
      Matcher var1 = MONEY_ANY.matcher(normalize(var0));
      if (!var1.find()) {
         return 1.0;
      } else {
         String var2 = var1.group(1);
         int var3 = var2.indexOf(46);
         int var4 = var3 < 0 ? 0 : var2.length() - var3 - 1;
         return Math.max(1.0, scale("1", var1.group(2)) / Math.pow(10.0, var4));
      }
   }

   private static double scale(String var0, String var1) {
      double var2;
      try {
         var2 = Double.parseDouble(var0.replace(",", ""));
      } catch (NumberFormatException var6) {
         return -1.0;
      }

      if (var1 == null) {
         return var2;
      } else {
         return switch (var1) {
            case "k" -> var2 * 1000.0;
            case "m" -> var2 * 1000000.0;
            case "b" -> var2 * 1.0E9;
            default -> var2;
         };
      }
   }

   static String exactMoney(long var0) {
      if (var0 >= 1000000000L && var0 % 1000000000L == 0L) {
         return var0 / 1000000000L + "b";
      } else if (var0 >= 1000000L && var0 % 1000000L == 0L) {
         return var0 / 1000000L + "m";
      } else if (var0 >= 1000000L && var0 % 100000L == 0L) {
         return var0 / 1000000L + "." + var0 % 1000000L / 100000L + "m";
      } else {
         return var0 >= 1000L && var0 % 1000L == 0L ? var0 / 1000L + "k" : Long.toString(var0);
      }
   }

   static String shortMoney(double var0) {
      if (var0 >= 1.0E9) {
         return trim(var0 / 1.0E9) + "b";
      } else if (var0 >= 1000000.0) {
         return trim(var0 / 1000000.0) + "m";
      } else {
         return var0 >= 1000.0 ? trim(var0 / 1000.0) + "k" : Long.toString(Math.round(var0));
      }
   }

   private static String trim(double var0) {
      String var2 = String.format(Locale.ROOT, "%.2f", var0);
      return var2.contains(".") ? var2.replaceAll("0+$", "").replaceAll("\\.$", "") : var2;
   }

   static int roman(String var0) {
      String var1 = normalize(var0);

      for (int var2 = 0; var2 < ROMAN.length; var2++) {
         if (ROMAN[var2].equals(var1)) {
            return var2 + 1;
         }
      }

      return 0;
   }

   static String withoutRoman(String var0) {
      String var1 = normalize(var0);
      int var2 = var1.lastIndexOf(32);
      return var2 > 0 && roman(var1.substring(var2 + 1)) > 0 ? var1.substring(0, var2) : var1;
   }

   static List<String> lore(ItemStack var0) {
      ArrayList var1 = new ArrayList();

      for (Text var3 : ((LoreComponent)var0.method_58695(DataComponentTypes.field_49632, LoreComponent.field_49340)).comp_2400()) {
         var1.add(var3.getString());
      }

      return var1;
   }

   static double listingPrice(ItemStack var0) {
      List var1 = lore(var0);

      for (String var3 : var1) {
         String var4 = withoutBullet(normalize(var3));
         if (var4.startsWith("price")) {
            List var5 = var4.contains("$") ? dollars(var4) : moneyAll(var4.substring("price".length()));
            return var5.isEmpty() ? -1.0 : (Double)var5.get(0);
         }
      }

      double var10 = -1.0;

      for (String var12 : var1) {
         String var6 = normalize(var12);
         if (var6.contains("$") && !var6.contains("balance")) {
            for (double var8 : dollars(var6)) {
               if (var10 > 0.0 && Math.abs(var10 - var8) >= 1.0) {
                  return -1.0;
               }

               var10 = var8;
            }
         }
      }

      return var10;
   }

   private static String withoutBullet(String var0) {
      int var1 = 0;

      while (var1 < var0.length() && !Character.isLetterOrDigit(var0.charAt(var1))) {
         var1++;
      }

      return var0.substring(var1);
   }

   static double orderPrice(ItemStack var0) {
      List var1 = lore(var0);
      double var2 = -1.0;

      for (String var5 : var1) {
         String var6 = normalize(var5);
         if (var5.contains("$") && (var6.contains("price") || var6.contains("each") || var6.contains("per item") || var6.contains("pay"))) {
            for (double var8 : moneyAll(var5)) {
               if (var8 > 0.0 && (var2 < 0.0 || var8 < var2)) {
                  var2 = var8;
               }
            }
         }
      }

      if (var2 > 0.0) {
         return var2;
      } else {
         for (String var11 : var1) {
            List var12;
            if (var11.contains("$") && !(var12 = moneyAll(var11)).isEmpty()) {
               return (Double)var12.get(0);
            }
         }

         return -1.0;
      }
   }
}
