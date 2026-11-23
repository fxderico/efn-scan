package net.efn.client.module.impl.misc.nameprotect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class NameMatcher {
   public static final NameMatcher EMPTY = new NameMatcher(Map.of());
   public static final int MAX_NAME_LENGTH = 32;
   private final Map<String, String> mapping;
   private final String[] names;
   private final String[] aliases;
   private final int mask;
   private final long lengths;

   public NameMatcher(Map<String, String> var1) {
      HashMap var2 = new HashMap();
      ArrayList var3 = new ArrayList(var1.size());
      ArrayList var4 = new ArrayList(var1.size());

      for (Entry var6 : var1.entrySet()) {
         String var7 = (String)var6.getKey();
         String var8 = (String)var6.getValue();
         if (var7 != null && !var7.isEmpty() && var7.length() <= 32 && var8 != null && var2.putIfAbsent(var7.toLowerCase(Locale.ROOT), var8) == null) {
            var3.add(var7);
            var4.add(var8);
         }
      }

      int var13 = Integer.highestOneBit(Math.max(1, var3.size()) * 2) * 2;
      String[] var14 = new String[var13];
      String[] var15 = new String[var13];
      long var16 = 0L;

      for (int var10 = 0; var10 < var3.size(); var10++) {
         String var11 = (String)var3.get(var10);
         int var12 = hash(var11, 0, var11.length()) & var13 - 1;

         while (var14[var12] != null) {
            var12 = var12 + 1 & var13 - 1;
         }

         var14[var12] = var11;
         var15[var12] = (String)var4.get(var10);
         var16 |= 1L << var11.length();
      }

      this.mapping = Map.copyOf(var2);
      this.names = var14;
      this.aliases = var15;
      this.mask = var13 - 1;
      this.lengths = var16;
   }

   public boolean isEmpty() {
      return this.mapping.isEmpty();
   }

   public boolean sameMapping(NameMatcher var1) {
      return var1 != null && this.mapping.equals(var1.mapping);
   }

   public static boolean isNameChar(char var0) {
      return var0 >= 'a' && var0 <= 'z' || var0 >= 'A' && var0 <= 'Z' || var0 >= '0' && var0 <= '9' || var0 == '_';
   }

   public static String nameCore(String var0) {
      if (var0 == null) {
         return null;
      } else {
         int var2 = var0.length();
         int var1 = 0;

         while (var1 < var2 && !isNameChar(var0.charAt(var1))) {
            var1++;
         }

         while (var2 > var1 && !isNameChar(var0.charAt(var2 - 1))) {
            var2--;
         }

         if (var1 != var2 && var2 - var1 <= 32) {
            for (int var3 = var1; var3 < var2; var3++) {
               if (!isNameChar(var0.charAt(var3))) {
                  return null;
               }
            }

            return var0.substring(var1, var2);
         } else {
            return null;
         }
      }
   }

   public String aliasAt(String var1, int var2, int var3) {
      int var4 = var3 - var2;
      if (var4 > 0 && var4 <= 32 && (this.lengths & 1L << var4) != 0L) {
         String var6;
         for (int var5 = hash(var1, var2, var3) & this.mask; (var6 = this.names[var5]) != null; var5 = var5 + 1 & this.mask) {
            if (var6.length() == var4 && var6.regionMatches(true, 0, var1, var2, var4)) {
               return this.aliases[var5];
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public String replace(String var1) {
      if (var1 != null && !var1.isEmpty() && !this.isEmpty()) {
         List var2 = this.scan(var1, new int[]{0, var1.length()}, 1);
         if (var2.isEmpty()) {
            return var1;
         } else {
            StringBuilder var3 = new StringBuilder(var1.length() + 16);
            int var4 = 0;

            for (NameMatcher.Match var6 : var2) {
               var3.append(var1, var4, var6.start()).append(var6.alias());
               var4 = var6.end();
            }

            return var3.append(var1, var4, var1.length()).toString();
         }
      } else {
         return var1;
      }
   }

   public boolean mentionsAny(String var1) {
      return var1 != null && !var1.isEmpty() && !this.isEmpty() && !this.scan(var1, new int[]{0, var1.length()}, 1).isEmpty();
   }

   public NameMatcher newNamesSince(NameMatcher var1) {
      HashMap var2 = null;

      for (Entry var4 : this.mapping.entrySet()) {
         if (var1 == null || !((String)var4.getValue()).equals(var1.mapping.get(var4.getKey()))) {
            if (var2 == null) {
               var2 = new HashMap();
            }

            var2.put((String)var4.getKey(), (String)var4.getValue());
         }
      }

      return var2 != null ? new NameMatcher(var2) : EMPTY;
   }

   public List<NameMatcher.Match> scan(String var1, int[] var2, int var3) {
      if (!this.isEmpty() && !var1.isEmpty()) {
         List var4 = null;
         int var5 = var1.length();
         int var6 = 0;
         int var7 = 0;

         while (var7 < var5) {
            while (var6 < var3 - 1 && var2[var6 + 1] <= var7) {
               var6++;
            }

            int var9 = var2[var6 + 1];
            char var10 = var1.charAt(var7);
            if (var10 == 167) {
               var7 += var7 + 1 < var9 ? 2 : 1;
            } else if (!isNameChar(var10)) {
               var7++;
            } else {
               int var8 = var7 + 1;

               while (var8 < var5 && isNameChar(var1.charAt(var8))) {
                  var8++;
               }

               String var11 = this.aliasAt(var1, var7, var8);
               if (var11 != null) {
                  int var12 = bedrockPrefix(var1, var7);
                  var4 = add(var4, var12, var8, var12 == var7 ? var11 : var1.substring(var12, var7).replace(".", "") + var11);
               } else if (var9 < var8) {
                  for (int var15 = var6 + 1; var15 < var3 && var2[var15] < var8; var15++) {
                     int var13 = var2[var15];
                     String var14 = this.aliasAt(var1, var13, var8);
                     if (var14 != null) {
                        var4 = add(var4, var13, var8, var14);
                        break;
                     }
                  }
               }

               var7 = var8;
            }
         }

         return var4 != null ? var4 : List.of();
      } else {
         return List.of();
      }
   }

   private static int bedrockPrefix(String var0, int var1) {
      int var2 = var1;
      int var3 = var1;

      while (true) {
         while (var2 >= 2 && var0.charAt(var2 - 2) == 167) {
            var2 -= 2;
         }

         if (var2 < 1 || var0.charAt(var2 - 1) != '.') {
            if (var3 == var1) {
               return var1;
            } else {
               boolean var4 = var3 >= 1 && isNameChar(var0.charAt(var3 - 1)) && (var3 < 2 || var0.charAt(var3 - 2) != 167);
               return var4 ? var1 : var3;
            }
         }

         var3 = --var2;
      }
   }

   private static List<NameMatcher.Match> add(List<NameMatcher.Match> var0, int var1, int var2, String var3) {
      Object var4 = var0 != null ? var0 : new ArrayList(4);
      var4.add(new NameMatcher.Match(var1, var2, var3));
      return (List<NameMatcher.Match>)var4;
   }

   private static int hash(String var0, int var1, int var2) {
      int var3 = 0;

      for (int var4 = var1; var4 < var2; var4++) {
         char var5 = var0.charAt(var4);
         if (var5 >= 'A' && var5 <= 'Z') {
            var5 = (char)(var5 + ' ');
         }

         var3 = 31 * var3 + var5;
      }

      return var3 ^ var3 >>> 16;
   }

   @Environment(EnvType.CLIENT)
   public record Match(int start, int end, String alias) {
   }
}
