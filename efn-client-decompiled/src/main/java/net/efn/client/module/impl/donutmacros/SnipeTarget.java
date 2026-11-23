package net.efn.client.module.impl.donutmacros;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

@Environment(EnvType.CLIENT)
final class SnipeTarget {
   private static final Pattern DURATION = Pattern.compile("(?<!\\d)(\\d{1,5})\\s*([dhms])");
   final Item item;
   final boolean shard;
   final SnipeTarget.Preset preset;
   final String displayName;
   final String search;
   final List<Product.Enchant> enchants;
   final boolean exactEnchants;
   final long maxPrice;
   final int minMinutes;
   private final Predicate<ItemStack> filter;

   private SnipeTarget(
      Item var1,
      boolean var2,
      SnipeTarget.Preset var3,
      String var4,
      String var5,
      List<Product.Enchant> var6,
      boolean var7,
      long var8,
      int var10,
      Predicate<ItemStack> var11
   ) {
      this.item = var1;
      this.shard = var2;
      this.preset = var3;
      this.displayName = var4;
      this.search = var5;
      this.enchants = var6;
      this.exactEnchants = var7;
      this.maxPrice = var8;
      this.minMinutes = var10;
      this.filter = var11;
   }

   static SnipeTarget matching(Item var0, String var1, String var2, Predicate<ItemStack> var3, long var4) {
      return new SnipeTarget(var0, false, null, var1, var2, List.of(), false, var4, 0, var3);
   }

   static SnipeTarget parse(String var0, String var1, boolean var2, long var3, int var5) {
      List var9 = Product.parseEnchants(var1);
      SnipeTarget.Preset var10 = SnipeTarget.Preset.of(var0);
      if (var10 != null) {
         return new SnipeTarget(var10.item, true, var10, var10.label, var10.search, var9, var2, var3, var5, null);
      } else {
         String var8 = var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
         if (var8.startsWith("minecraft:")) {
            var8 = var8.substring("minecraft:".length());
         }

         boolean var6;
         Item var7;
         if ((var7 = Product.resolveItem((var6 = var8.startsWith("shard_")) ? "netherite_" + var8.substring("shard_".length()) : var8)) == null) {
            return null;
         } else {
            String var12 = var6 ? "Shard " + capitalize(var8.substring("shard_".length())) : new ItemStack(var7).method_7964().getString();
            return new SnipeTarget(var7, var6, null, var12, Product.searchText(var7), var9, var2, var3, var6 ? var5 : 0, null);
         }
      }
   }

   private static String capitalize(String var0) {
      StringBuilder var1 = new StringBuilder();

      for (String var5 : var0.split("_")) {
         if (!var5.isEmpty()) {
            if (var1.length() > 0) {
               var1.append(' ');
            }

            var1.append(Character.toUpperCase(var5.charAt(0))).append(var5.substring(1));
         }
      }

      return var1.toString();
   }

   boolean sameKind(ItemStack var1) {
      if (this.filter != null) {
         return !var1.method_7960() && this.filter.test(var1);
      } else if (!var1.method_7960() && var1.method_31574(this.item)) {
         String var2 = ServerText.key(var1.method_7964().getString());
         if (this.preset != null) {
            if (!var2.contains(this.preset.keyword)) {
               return false;
            }

            if (!this.preset.exclude.isEmpty() && var2.contains(this.preset.exclude)) {
               return false;
            }
         } else if (var2.contains("shard") != this.shard) {
            return false;
         }

         return this.enchants.isEmpty() || Product.enchantsMatch(var1, this.enchants, this.exactEnchants);
      } else {
         return false;
      }
   }

   boolean timed() {
      return this.preset != null || this.shard;
   }

   boolean matches(ItemStack var1) {
      if (!this.sameKind(var1)) {
         return false;
      } else {
         return this.preset != null ? minutesLeft(var1) >= Math.max(0, this.minMinutes) : this.minMinutes <= 0 || minutesLeft(var1) >= this.minMinutes;
      }
   }

   static int minutesLeft(ItemStack var0) {
      ArrayList var1 = new ArrayList();

      for (String var3 : ServerText.lore(var0)) {
         var1.add(ServerText.normalize(var3));
      }

      ArrayList var13 = new ArrayList();

      for (int var14 = 0; var14 < var1.size(); var14++) {
         if (((String)var1.get(var14)).contains("$") || ((String)var1.get(var14)).contains("seller")) {
            var13.add(var14);
         }
      }

      int var15 = -1;
      int var4 = -1;
      int var5 = 0;

      for (int var6 = 0; var6 < var1.size(); var6++) {
         String var8 = (String)var1.get(var6);
         boolean var9 = var8.contains("destruct");
         int var7;
         if ((var9 || var8.contains("expire")) && (var7 = duration(var8)) >= 0) {
            if (var9) {
               return var7;
            }

            var5++;
            int var10 = 2147483647;

            for (int var12 : var13) {
               var10 = Math.min(var10, Math.abs(var12 - var6));
            }

            if (var10 > var4) {
               var4 = var10;
               var15 = var7;
            }
         }
      }

      if (var15 < 0) {
         return -1;
      } else {
         return !var13.isEmpty() && var5 < 2 && var4 < 2 ? -1 : var15;
      }
   }

   private static int duration(String var0) {
      Matcher var1 = DURATION.matcher(var0);
      int var2 = 0;
      boolean var3 = false;

      while (var1.find()) {
         var3 = true;
         int var4 = Integer.parseInt(var1.group(1));
         String var5 = var1.group(2);
         switch (var5) {
            case "d":
               var2 += var4 * 1440;
               break;
            case "h":
               var2 += var4 * 60;
               break;
            case "m":
               var2 += var4;
         }
      }

      return var3 ? var2 : -1;
   }

   Product product(long var1) {
      return new Product(
         this.item,
         Registries.field_41178.method_10221(this.item).method_12832(),
         this.search,
         this.displayName,
         var1,
         this.enchants,
         false,
         1,
         false,
         1L,
         1L,
         0L,
         this::sameKind
      );
   }

   String describe() {
      StringBuilder var1 = new StringBuilder(this.displayName);
      if (!this.enchants.isEmpty()) {
         StringBuilder var2 = new StringBuilder();

         for (Product.Enchant var4 : this.enchants) {
            if (var2.length() > 0) {
               var2.append(", ");
            }

            var2.append(var4.label());
         }

         var1.append(" (").append(this.exactEnchants ? "exactly " : "at least ").append((CharSequence)var2).append(')');
      }

      return var1.toString();
   }

   @Environment(EnvType.CLIENT)
   static enum Preset {
      SELL_AXE("sell_axe", "Sell Axe", Items.field_22025, "sell axe", "sell", ""),
      DRILL("drill", "Drill", Items.field_22024, "drill", "drill", ""),
      SHARD_AXE("shard_axe", "Shard Axe", Items.field_22025, "shard axe", "shard", "sell");

      final String value;
      final String label;
      final Item item;
      final String search;
      final String keyword;
      final String exclude;

      private Preset(String nullxx, String nullxxx, Item nullxxxx, String nullxxxxx, String nullxxxxxx, String nullxxxxxxx) {
         this.value = nullxx;
         this.label = nullxxx;
         this.item = nullxxxx;
         this.search = nullxxxxx;
         this.keyword = nullxxxxxx;
         this.exclude = nullxxxxxxx;
      }

      static SnipeTarget.Preset of(String var0) {
         String var1 = var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

         for (SnipeTarget.Preset var5 : values()) {
            if (var5.value.equals(var1)) {
               return var5;
            }
         }

         return null;
      }

      static List<StringSetting.Preset> choices() {
         ArrayList var0 = new ArrayList();

         for (SnipeTarget.Preset var4 : values()) {
            var0.add(new StringSetting.Preset(var4.value, var4.label, Registries.field_41178.method_10221(var4.item).method_12832(), true));
         }

         return var0;
      }
   }
}
