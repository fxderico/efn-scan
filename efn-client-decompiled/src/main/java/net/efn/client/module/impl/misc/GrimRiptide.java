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
      MinecraftClient var0 = MinecraftClient.getInstance();
      ClientPlayerEntity var1 = var0.player;
      if (var1 != null && var0.world != null && var0.isOnThread()) {
         if (!var1.isTouchingWaterOrRain() && !var0.world.isRaining()) {
            Box var2 = var1.getBoundingBox().expand(0.1);
            BlockPos var3 = BlockPos.ofFloored(var2.minX, var2.minY, var2.minZ);
            BlockPos var4 = BlockPos.ofFloored(var2.maxX, var2.maxY, var2.maxZ);

            for (BlockPos var6 : BlockPos.iterate(var3, var4)) {
               BlockState var7 = var0.world.getBlockState(var6);
               if (var7.getFluidState().isOf(Fluids.WATER)) {
                  return true;
               }

               if (var7.getFluidState().isOf(Fluids.FLOWING_WATER)) {
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

   public static boolean isRiptide(ItemStack itemStack) {
      if (!itemStack.isOf(Items.TRIDENT)) {
         return false;
      } else {
         for (Entry var2 : itemStack.getEnchantments().getEnchantmentEntries()) {
            RegistryEntry var3 = (RegistryEntry)var2.getKey();
            if (var3.matchesKey(Enchantments.RIPTIDE) && var2.getIntValue() > 0) {
               return true;
            }
         }

         return false;
      }
   }
}
