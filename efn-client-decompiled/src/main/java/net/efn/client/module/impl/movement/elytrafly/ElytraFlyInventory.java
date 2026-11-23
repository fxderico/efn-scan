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

   public static int find(Predicate<ItemStack> predicate) {
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      return var1 == null ? -1 : find(predicate, 0, var1.getInventory().size() - 1);
   }

   public static int find(Predicate<ItemStack> predicate, int i, int j) {
      ClientPlayerEntity var3 = MinecraftClient.getInstance().player;
      if (var3 == null) {
         return -1;
      } else {
         PlayerInventory var4 = var3.getInventory();

         for (int var5 = i; var5 <= j; var5++) {
            if (predicate.test(var4.getStack(var5))) {
               return var5;
            }
         }

         return -1;
      }
   }

   public static int findInHotbar(Predicate<ItemStack> predicate) {
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      if (var1 == null) {
         return -1;
      } else if (predicate.test(var1.getOffHandStack())) {
         return 40;
      } else {
         return predicate.test(var1.getMainHandStack()) ? var1.getInventory().getSelectedSlot() : find(predicate, 0, 8);
      }
   }

   public static boolean isHotbar(int i) {
      return i >= 0 && i <= 8;
   }

   public static void moveToHotbar(int i, int j) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var2 != null) {
         move(var2, indexToId(var2, i), indexToId(var2, j));
      }
   }

   public static void moveToChest(int i) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var2 != null) {
         int var1 = indexToId(var2, i);
         move(var2, var1, var1 == -1 ? -1 : 6);
      }
   }

   private static void move(ClientPlayerEntity clientPlayerEntity, int i, int j) {
      ClientPlayerInteractionManager var3 = MinecraftClient.getInstance().interactionManager;
      if (var3 != null && i != -1 && j != -1) {
         ScreenHandler var4 = clientPlayerEntity.currentScreenHandler;
         boolean var5 = var4.getCursorStack().isEmpty();
         var3.clickSlot(var4.syncId, i, 0, SlotActionType.PICKUP, clientPlayerEntity);
         var3.clickSlot(var4.syncId, j, 0, SlotActionType.PICKUP, clientPlayerEntity);
         if (var5 && !var4.getCursorStack().isEmpty()) {
            var3.clickSlot(var4.syncId, i, 0, SlotActionType.PICKUP, clientPlayerEntity);
         }
      }
   }

   private static int indexToId(ClientPlayerEntity clientPlayerEntity, int i) {
      if (clientPlayerEntity.currentScreenHandler != clientPlayerEntity.playerScreenHandler) {
         return -1;
      } else if (i >= 0 && i <= 8) {
         return 36 + i;
      } else if (i >= 9 && i < 36) {
         return i;
      } else if (i >= 36 && i <= 39) {
         return 8 - (i - 36);
      } else {
         return i == 40 ? 45 : -1;
      }
   }

   public static boolean swap(int i, boolean bl) {
      if (i == 40) {
         return true;
      } else if (i >= 0 && i <= 8) {
         MinecraftClient var2 = MinecraftClient.getInstance();
         ClientPlayerEntity var3 = var2.player;
         if (var3 != null && var2.interactionManager != null) {
            if (bl && previousSlot == -1) {
               previousSlot = var3.getInventory().getSelectedSlot();
            } else if (!bl) {
               previousSlot = -1;
            }

            var3.getInventory().setSelectedSlot(i);
            ((AccessorElytraFlyMultiPlayerGameMode)var2.interactionManager).efn$elytraFlySyncSelected();
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
      ClientPlayerEntity var0 = MinecraftClient.getInstance().player;
      if (var0 != null) {
         ItemStack var1 = var0.getEquippedStack(EquipmentSlot.CHEST);
         EquippableComponent var2 = var1.get(DataComponentTypes.EQUIPPABLE);
         if (var1.contains(DataComponentTypes.GLIDER)) {
            equipChestplate(var0);
         } else if (var2 != null && var2.slot().getEntitySlotId() == EquipmentSlot.CHEST.getEntitySlotId()) {
            equipElytra(var0);
         } else if (!equipChestplate(var0)) {
            equipElytra(var0);
         }
      }
   }

   private static boolean equipChestplate(ClientPlayerEntity clientPlayerEntity) {
      DefaultedList var1 = clientPlayerEntity.getInventory().getMainStacks();
      int var2 = -1;

      for (int var3 = 0; var3 < var1.size(); var3++) {
         Item var4 = ((ItemStack)var1.get(var3)).getItem();
         if (var4 == Items.DIAMOND_CHESTPLATE) {
            var2 = var3;
         } else if (var4 == Items.NETHERITE_CHESTPLATE) {
            var2 = var3;
            break;
         }
      }

      if (var2 != -1) {
         equip(clientPlayerEntity, var2);
      }

      return var2 != -1;
   }

   private static void equipElytra(ClientPlayerEntity clientPlayerEntity) {
      DefaultedList var1 = clientPlayerEntity.getInventory().getMainStacks();

      for (int var2 = 0; var2 < var1.size(); var2++) {
         if (((ItemStack)var1.get(var2)).contains(DataComponentTypes.GLIDER)) {
            equip(clientPlayerEntity, var2);
            break;
         }
      }
   }

   private static void equip(ClientPlayerEntity clientPlayerEntity, int i) {
      if (indexToId(clientPlayerEntity, i) != -1) {
         moveToChest(i);
         clientPlayerEntity.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
      }
   }
}
