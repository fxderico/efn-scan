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
   Product(
      Item var1,
      String var2,
      String var3,
      String var4,
      long var5,
      List<Product.Enchant> var7,
      boolean var8,
      int var9,
      boolean var10,
      long var11,
      long var13,
      long var15
   ) {
      this(var1, var2, var3, var4, var5, var7, var8, var9, var10, var11, var13, var15, null);
   }

   static Item resolveItem(String var0) {
      String var2 = var0 == null ? "" : var0.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
      if (var2.isEmpty()) {
         return null;
      } else {
         if (!var2.contains(":")) {
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

   static String searchText(Item var0) {
      return Registries.field_41178.method_10221(var0).method_12832().replace('_', ' ');
   }

   static List<Product.Enchant> parseEnchants(String var0) {
      ArrayList var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         for (String var5 : var0.split("[,;]")) {
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

   private static int parseLevel(String var0) {
      try {
         int var1 = Integer.parseInt(var0);
         return var1 >= 1 && var1 <= 10 ? var1 : 0;
      } catch (NumberFormatException var2) {
         return ServerText.roman(var0);
      }
   }

   boolean matches(ItemStack var1) {
      if (!var1.method_7960() && var1.method_31574(this.item)) {
         if (this.filter != null) {
            return this.filter.test(var1);
         } else if (this.enchants.isEmpty()) {
            return true;
         } else {
            ItemEnchantmentsComponent var2 = EnchantmentHelper.method_57532(var1);

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

   static boolean enchantsMatch(ItemStack var0, List<Product.Enchant> var1, boolean var2) {
      ItemEnchantmentsComponent var3 = EnchantmentHelper.method_57532(var0);
      if (var2 && var3.method_57541() != var1.size()) {
         return false;
      } else {
         for (Product.Enchant var5 : var1) {
            int var6 = levelOf(var3, var5.key());
            if (var2 ? var6 != var5.level() : var6 < var5.level()) {
               return false;
            }
         }

         return true;
      }
   }

   private static int levelOf(ItemEnchantmentsComponent var0, String var1) {
      for (Entry var3 : var0.method_57539()) {
         String var4 = ((RegistryEntry)var3.getKey()).method_40230().map(var0x -> var0x.method_29177().method_12832()).orElse("");
         if (ServerText.key(var4).equals(var1)) {
            return var3.getIntValue();
         }
      }

      return 0;
   }

   boolean nameMatches(String var1, boolean var2) {
      String var3 = ServerText.key(var1);
      String var4 = ServerText.key(Registries.field_41178.method_10221(this.item).method_12832());
      String var5 = ServerText.key(this.displayName);
      return !var3.equals(var4) && !var3.equals(var5) ? var2 && (var3.endsWith(var4) || !var5.isEmpty() && var3.endsWith(var5)) : true;
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
