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

   public static Item resolve(String string) {
      String var2 = string == null ? "" : string.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
      if (var2.isEmpty()) {
         return null;
      } else {
         if (var2.indexOf(58) < 0) {
            var2 = "minecraft:" + var2;
         }

         Identifier var1;
         if ((var1 = Identifier.tryParse(var2)) != null && Registries.ITEM.containsId(var1)) {
            Item var4 = Registries.ITEM.get(var1);
            return var4 == Items.AIR ? null : var4;
         } else {
            return null;
         }
      }
   }

   public static String stored(Item item) {
      Identifier var1 = Registries.ITEM.getId(item);
      return "minecraft".equals(var1.getNamespace()) ? var1.getPath() : var1.toString();
   }

   public static boolean supports(Item item) {
      if (item == null) {
         return false;
      } else if (item == Items.ENCHANTED_BOOK) {
         return true;
      } else {
         List var1 = all();
         ItemStack var2 = item.getDefaultStack();
         if (var1.isEmpty()) {
            return var2.contains(DataComponentTypes.ENCHANTABLE);
         } else {
            for (RegistryEntry var4 : var1) {
               if (((Enchantment)var4.value()).isSupportedItem(var2)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public static List<RegistryEntry<Enchantment>> applicable(Item item) {
      ArrayList var1 = new ArrayList();
      if (item == null) {
         return var1;
      } else {
         ItemStack var2 = item.getDefaultStack();
         boolean var3 = item == Items.ENCHANTED_BOOK;

         for (RegistryEntry var5 : all()) {
            if (var3 || ((Enchantment)var5.value()).isSupportedItem(var2)) {
               var1.add(var5);
            }
         }

         var1.sort(Comparator.comparing(registryEntry -> ((Enchantment)registryEntry.value()).description().getString(), String.CASE_INSENSITIVE_ORDER));
         return var1;
      }
   }

   public static List<RegistryEntry<Enchantment>> all() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0.world == null) {
         return List.of();
      } else {
         DynamicRegistryManager var1 = var0.world.getRegistryManager();
         if (var1 != cachedFor) {
            ArrayList var2 = new ArrayList();
            var1.getOrThrow(RegistryKeys.ENCHANTMENT).streamEntries().forEach(var2::add);
            cached = List.copyOf(var2);
            cachedFor = var1;
         }

         return cached;
      }
   }

   public static String path(RegistryEntry<Enchantment> registryEntry) {
      return registryEntry.getKey().map(registryKey -> registryKey.getValue().getPath()).orElse("");
   }

   public static String key(String string) {
      String var1 = string == null ? "" : string.toLowerCase(Locale.ROOT);
      StringBuilder var2 = new StringBuilder(var1.length());

      for (int var3 = 0; var3 < var1.length(); var3++) {
         char var4 = var1.charAt(var3);
         if (var4 >= 'a' && var4 <= 'z' || var4 >= '0' && var4 <= '9') {
            var2.append(var4);
         }
      }

      return var2.toString();
   }

   public static Map<String, Integer> parse(String string) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (string == null) {
         return var1;
      } else {
         for (String var5 : string.split("[,;]")) {
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

   private static int level(String string) {
      try {
         int var1 = Integer.parseInt(string);
         return var1 >= 1 && var1 <= 10 ? var1 : 0;
      } catch (NumberFormatException var4) {
         String var2 = string.toLowerCase(Locale.ROOT);

         for (int var3 = 0; var3 < ROMAN.length; var3++) {
            if (ROMAN[var3].equals(var2)) {
               return var3 + 1;
            }
         }

         return 0;
      }
   }

   public static String roman(int i) {
      return i >= 1 && i <= ROMAN.length ? ROMAN[i - 1].toUpperCase(Locale.ROOT) : Integer.toString(i);
   }
}
