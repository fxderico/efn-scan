package net.efn.client.module.impl.donutmacros;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
record Product(
   Item item,
   String id,
   String search,
   String displayName,
   long minPrice,
   List<Product.Enchant> enchants,
   boolean restock,
   int orderAmount,
   boolean beatTopOrder,
   long fixedPrice,
   long bidIncrement,
   long maxPrice,
   Predicate<ItemStack> filter
) {
   Product(Item item, String string, String string2, String string3, long l, List<Product.Enchant> list, boolean bl, int i, boolean bl2, long m, long n, long o) {
      this(item, string, string2, string3, l, list, bl, i, bl2, m, n, o, null);
   }

   static Item resolveItem(String string) {
      String var2 = string == null ? "" : string.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
      if (var2.isEmpty()) {
         return null;
      } else {
         if (!var2.contains(":")) {
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

   static String searchText(Item item) {
      return Registries.ITEM.getId(item).getPath().replace('_', ' ');
   }

   static List<Product.Enchant> parseEnchants(String string) {
      ArrayList var1 = new ArrayList();
      if (string == null) {
         return var1;
      } else {
         for (String var5 : string.split("[,;]")) {
            String var9 = var5.trim();
            if (!var9.isEmpty()) {
               int var10 = 1;
               int var11 = var9.lastIndexOf(32);
               int var8;
               if (var11 > 0 && (var8 = parseLevel(var9.substring(var11 + 1).trim())) > 0) {
                  var10 = var8;
                  var9 = var9.substring(0, var11).trim();
               }

               String var6;
               if (!(var6 = ServerText.key(var9)).isEmpty()) {
                  var1.add(new Product.Enchant(var6, var9, var10));
               }
            }
         }

         return var1;
      }
   }

   private static int parseLevel(String string) {
      try {
         int var1 = Integer.parseInt(string);
         return var1 >= 1 && var1 <= 10 ? var1 : 0;
      } catch (NumberFormatException var2) {
         return ServerText.roman(string);
      }
   }

   boolean matches(ItemStack itemStack) {
      if (!itemStack.isEmpty() && itemStack.isOf(this.item)) {
         if (this.filter != null) {
            return this.filter.test(itemStack);
         } else if (this.enchants.isEmpty()) {
            return true;
         } else {
            ItemEnchantmentsComponent var2 = EnchantmentHelper.getEnchantments(itemStack);

            for (Product.Enchant var4 : this.enchants) {
               if (levelOf(var2, var4.key()) < var4.level()) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   static boolean enchantsMatch(ItemStack itemStack, List<Product.Enchant> list, boolean bl) {
      ItemEnchantmentsComponent var3 = EnchantmentHelper.getEnchantments(itemStack);
      if (bl && var3.getSize() != list.size()) {
         return false;
      } else {
         for (Product.Enchant var5 : list) {
            int var6 = levelOf(var3, var5.key());
            if (bl ? var6 != var5.level() : var6 < var5.level()) {
               return false;
            }
         }

         return true;
      }
   }

   private static int levelOf(ItemEnchantmentsComponent itemEnchantmentsComponent, String string) {
      for (Entry var3 : itemEnchantmentsComponent.getEnchantmentEntries()) {
         String var4 = ((RegistryEntry)var3.getKey()).getKey().map(registryKey -> registryKey.getValue().getPath()).orElse("");
         if (ServerText.key(var4).equals(string)) {
            return var3.getIntValue();
         }
      }

      return 0;
   }

   boolean nameMatches(String string, boolean bl) {
      String var3 = ServerText.key(string);
      String var4 = ServerText.key(Registries.ITEM.getId(this.item).getPath());
      String var5 = ServerText.key(this.displayName);
      return !var3.equals(var4) && !var3.equals(var5) ? bl && (var3.endsWith(var4) || !var5.isEmpty() && var3.endsWith(var5)) : true;
   }

   String describe() {
      StringBuilder var1 = new StringBuilder(this.displayName);
      if (!this.enchants.isEmpty()) {
         ArrayList var2 = new ArrayList();

         for (Product.Enchant var4 : this.enchants) {
            var2.add(var4.label());
         }

         var1.append(" (").append(String.join(", ", var2)).append(')');
      }

      return var1.toString();
   }

   @Environment(EnvType.CLIENT)
   record Enchant(String key, String name, int level) {
      String label() {
         return this.name + (this.level > 1 ? " " + this.level : "");
      }
   }
}
