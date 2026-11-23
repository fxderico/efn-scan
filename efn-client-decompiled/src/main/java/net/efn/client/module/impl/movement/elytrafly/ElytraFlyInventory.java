package net.efn.client.module.impl.movement.elytrafly;

import com.example.client.mixin.AccessorElytraFlyMultiPlayerGameMode;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

@Environment(EnvType.CLIENT)
public final class ElytraFlyInventory {
   public static final int OFFHAND = 40;
   private static final int CHEST_ARMOR_ID = 6;
   private static int previousSlot = -1;

   private ElytraFlyInventory() {
   }

   public static int find(Predicate<ItemStack> var0) {
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      return var1 == null ? -1 : find(var0, 0, var1.method_31548().method_5439() - 1);
   }

   public static int find(Predicate<ItemStack> var0, int var1, int var2) {
      ClientPlayerEntity var3 = MinecraftClient.method_1551().field_1724;
      if (var3 == null) {
         return -1;
      } else {
         PlayerInventory var4 = var3.method_31548();

         for (int var5 = var1; var5 <= var2; var5++) {
            if (var0.test(var4.method_5438(var5))) {
               return var5;
            }
         }

         return -1;
      }
   }

   public static int findInHotbar(Predicate<ItemStack> var0) {
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      if (var1 == null) {
         return -1;
      } else if (var0.test(var1.method_6079())) {
         return 40;
      } else {
         return var0.test(var1.method_6047()) ? var1.method_31548().method_67532() : find(var0, 0, 8);
      }
   }

   public static boolean isHotbar(int var0) {
      return var0 >= 0 && var0 <= 8;
   }

   public static void moveToHotbar(int var0, int var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 != null) {
         move(var2, indexToId(var2, var0), indexToId(var2, var1));
      }
   }

   public static void moveToChest(int var0) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 != null) {
         int var1 = indexToId(var2, var0);
         move(var2, var1, var1 == -1 ? -1 : 6);
      }
   }

   private static void move(ClientPlayerEntity var0, int var1, int var2) {
      ClientPlayerInteractionManager var3 = MinecraftClient.method_1551().field_1761;
      if (var3 != null && var1 != -1 && var2 != -1) {
         ScreenHandler var4 = var0.field_7512;
         boolean var5 = var4.method_34255().method_7960();
         var3.method_2906(var4.field_7763, var1, 0, SlotActionType.field_7790, var0);
         var3.method_2906(var4.field_7763, var2, 0, SlotActionType.field_7790, var0);
         if (var5 && !var4.method_34255().method_7960()) {
            var3.method_2906(var4.field_7763, var1, 0, SlotActionType.field_7790, var0);
         }
      }
   }

   private static int indexToId(ClientPlayerEntity var0, int var1) {
      if (var0.field_7512 != var0.field_7498) {
         return -1;
      } else if (var1 >= 0 && var1 <= 8) {
         return 36 + var1;
      } else if (var1 >= 9 && var1 < 36) {
         return var1;
      } else if (var1 >= 36 && var1 <= 39) {
         return 8 - (var1 - 36);
      } else {
         return var1 == 40 ? 45 : -1;
      }
   }

   public static boolean swap(int var0, boolean var1) {
      if (var0 == 40) {
         return true;
      } else if (var0 >= 0 && var0 <= 8) {
         MinecraftClient var2 = MinecraftClient.method_1551();
         ClientPlayerEntity var3 = var2.field_1724;
         if (var3 != null && var2.field_1761 != null) {
            if (var1 && previousSlot == -1) {
               previousSlot = var3.method_31548().method_67532();
            } else if (!var1) {
               previousSlot = -1;
            }

            var3.method_31548().method_61496(var0);
            ((AccessorElytraFlyMultiPlayerGameMode)var2.field_1761).efn$elytraFlySyncSelected();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean swapBack() {
      if (previousSlot == -1) {
         return false;
      } else {
         boolean var0 = swap(previousSlot, false);
         previousSlot = -1;
         return var0;
      }
   }

   public static void chestSwap() {
      ClientPlayerEntity var0 = MinecraftClient.method_1551().field_1724;
      if (var0 != null) {
         ItemStack var1 = var0.method_6118(EquipmentSlot.field_6174);
         EquippableComponent var2 = (EquippableComponent)var1.method_58694(DataComponentTypes.field_54196);
         if (var1.method_57826(DataComponentTypes.field_54197)) {
            equipChestplate(var0);
         } else if (var2 != null && var2.comp_3174().method_5927() == EquipmentSlot.field_6174.method_5927()) {
            equipElytra(var0);
         } else if (!equipChestplate(var0)) {
            equipElytra(var0);
         }
      }
   }

   private static boolean equipChestplate(ClientPlayerEntity var0) {
      DefaultedList var1 = var0.method_31548().method_67533();
      int var2 = -1;

      for (int var3 = 0; var3 < var1.size(); var3++) {
         Item var4 = ((ItemStack)var1.get(var3)).method_7909();
         if (var4 == Items.field_8058) {
            var2 = var3;
         } else if (var4 == Items.field_22028) {
            var2 = var3;
            break;
         }
      }

      if (var2 != -1) {
         equip(var0, var2);
      }

      return var2 != -1;
   }

   private static void equipElytra(ClientPlayerEntity var0) {
      DefaultedList var1 = var0.method_31548().method_67533();

      for (int var2 = 0; var2 < var1.size(); var2++) {
         if (((ItemStack)var1.get(var2)).method_57826(DataComponentTypes.field_54197)) {
            equip(var0, var2);
            break;
         }
      }
   }

   private static void equip(ClientPlayerEntity var0, int var1) {
      if (indexToId(var0, var1) != -1) {
         moveToChest(var1);
         var0.field_3944.method_52787(new CloseHandledScreenC2SPacket(0));
      }
   }
}
