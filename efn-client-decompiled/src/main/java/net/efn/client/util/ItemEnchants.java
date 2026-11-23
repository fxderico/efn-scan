package net.efn.client.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class ItemEnchants {
   private static final String[] ROMAN = new String[]{"i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x"};
   private static DynamicRegistryManager cachedFor;
   private static List<RegistryEntry<Enchantment>> cached = List.of();

   private ItemEnchants() {
   }

   public static Item resolve(String var0) {
      String var2 = var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
      if (var2.isEmpty()) {
         return null;
      } else {
         if (var2.indexOf(58) < 0) {
            var2 = "minecraft:" + var2;
         }

         Identifier var1;
         if ((var1 = Identifier.method_12829(var2)) != null && Registries.field_41178.method_10250(var1)) {
            Item var4 = (Item)Registries.field_41178.method_63535(var1);
            return var4 == Items.field_8162 ? null : var4;
         } else {
            return null;
         }
      }
   }

   public static String stored(Item var0) {
      Identifier var1 = Registries.field_41178.method_10221(var0);
      return "minecraft".equals(var1.method_12836()) ? var1.method_12832() : var1.toString();
   }

   public static boolean supports(Item var0) {
      if (var0 == null) {
         return false;
      } else if (var0 == Items.field_8598) {
         return true;
      } else {
         List var1 = all();
         ItemStack var2 = var0.method_7854();
         if (var1.isEmpty()) {
            return var2.method_57826(DataComponentTypes.field_53695);
         } else {
            for (RegistryEntry var4 : var1) {
               if (((Enchantment)var4.comp_349()).method_60046(var2)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public static List<RegistryEntry<Enchantment>> applicable(Item var0) {
      ArrayList var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         ItemStack var2 = var0.method_7854();
         boolean var3 = var0 == Items.field_8598;

         for (RegistryEntry var5 : all()) {
            if (var3 || ((Enchantment)var5.comp_349()).method_60046(var2)) {
               var1.add(var5);
            }
         }

         var1.sort(Comparator.comparing(var0x -> ((Enchantment)var0x.comp_349()).comp_2686().getString(), String.CASE_INSENSITIVE_ORDER));
         return var1;
      }
   }

   public static List<RegistryEntry<Enchantment>> all() {
      MinecraftClient var0 = MinecraftClient.method_1551();
      if (var0.field_1687 == null) {
         return List.of();
      } else {
         DynamicRegistryManager var1 = var0.field_1687.method_30349();
         if (var1 != cachedFor) {
            ArrayList var2 = new ArrayList();
            var1.method_30530(RegistryKeys.field_41265).method_42017().forEach(var2::add);
            cached = List.copyOf(var2);
            cachedFor = var1;
         }

         return cached;
      }
   }

   public static String path(RegistryEntry<Enchantment> var0) {
      return var0.method_40230().map(var0x -> var0x.method_29177().method_12832()).orElse("");
   }

   public static String key(String var0) {
      String var1 = var0 == null ? "" : var0.toLowerCase(Locale.ROOT);
      StringBuilder var2 = new StringBuilder(var1.length());

      for (int var3 = 0; var3 < var1.length(); var3++) {
         char var4 = var1.charAt(var3);
         if (var4 >= 'a' && var4 <= 'z' || var4 >= '0' && var4 <= '9') {
            var2.append(var4);
         }
      }

      return var2.toString();
   }

   public static Map<String, Integer> parse(String var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0 == null) {
         return var1;
      } else {
         for (String var5 : var0.split("[,;]")) {
            String var8 = var5.trim();
            if (!var8.isEmpty()) {
               int var9 = 1;
               int var10 = var8.lastIndexOf(32);
               int var7;
               if (var10 > 0 && (var7 = level(var8.substring(var10 + 1).trim())) > 0) {
                  var9 = var7;
                  var8 = var8.substring(0, var10).trim();
               }

               String var6;
               if (!(var6 = key(var8)).isEmpty()) {
                  var1.put(var6, var9);
               }
            }
         }

         return var1;
      }
   }

   private static int level(String var0) {
      try {
         int var1 = Integer.parseInt(var0);
         return var1 >= 1 && var1 <= 10 ? var1 : 0;
      } catch (NumberFormatException var4) {
         String var2 = var0.toLowerCase(Locale.ROOT);

         for (int var3 = 0; var3 < ROMAN.length; var3++) {
            if (ROMAN[var3].equals(var2)) {
               return var3 + 1;
            }
         }

         return 0;
      }
   }

   public static String roman(int var0) {
      return var0 >= 1 && var0 <= ROMAN.length ? ROMAN[var0 - 1].toUpperCase(Locale.ROOT) : Integer.toString(var0);
   }
}
