package net.efn.client.module.impl.misc;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

@Environment(EnvType.CLIENT)
public final class GrimRiptide {
   public static final int MIN_TICKS = 9;

   private GrimRiptide() {
   }

   public static boolean clientAccepts() {
      MinecraftClient var0 = MinecraftClient.method_1551();
      ClientPlayerEntity var1 = var0.field_1724;
      if (var1 != null && var0.field_1687 != null && var0.method_18854()) {
         if (!var1.method_5721() && !var0.field_1687.method_8419()) {
            Box var2 = var1.method_5829().method_1014(0.1);
            BlockPos var3 = BlockPos.method_49637(var2.field_1323, var2.field_1322, var2.field_1321);
            BlockPos var4 = BlockPos.method_49637(var2.field_1320, var2.field_1325, var2.field_1324);

            for (BlockPos var6 : BlockPos.method_10097(var3, var4)) {
               BlockState var7 = var0.field_1687.method_8320(var6);
               if (var7.method_26227().method_39360(Fluids.field_15910)) {
                  return true;
               }

               if (var7.method_26227().method_39360(Fluids.field_15909)) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean isRiptide(ItemStack var0) {
      if (!var0.method_31574(Items.field_8547)) {
         return false;
      } else {
         for (Entry var2 : var0.method_58657().method_57539()) {
            RegistryEntry var3 = (RegistryEntry)var2.getKey();
            if (var3.method_40225(Enchantments.field_9104) && var2.getIntValue() > 0) {
               return true;
            }
         }

         return false;
      }
   }
}
