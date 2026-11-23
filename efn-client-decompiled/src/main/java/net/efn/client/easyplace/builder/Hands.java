package net.efn.client.easyplace.builder;

import java.util.function.Predicate;
import net.efn.client.easyplace.inventory.InventoryCalm;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
final class Hands {
   private static final long SWAP_WAIT_NANOS = 1000000000L;
   private static long swapSentAt = System.nanoTime() - 1000000000L;

   private Hands() {
   }

   static boolean hold(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, Predicate<ItemStack> predicate) {
      PlayerInventory var4 = clientPlayerEntity.getInventory();
      if (predicate.test(clientPlayerEntity.getMainHandStack())) {
         return true;
      } else {
         for (int var3 = 0; var3 < 9; var3++) {
            if (predicate.test(var4.getStack(var3))) {
               var4.setSelectedSlot(var3);
               return true;
            }
         }

         if (clientPlayerEntity.currentScreenHandler == clientPlayerEntity.playerScreenHandler && minecraftClient.interactionManager != null) {
            for (int var5 = 9; var5 < 36; var5++) {
               if (predicate.test(var4.getStack(var5))) {
                  if (!InventoryCalm.settle(minecraftClient, clientPlayerEntity)) {
                     return false;
                  }

                  minecraftClient.interactionManager
                     .clickSlot(clientPlayerEntity.playerScreenHandler.syncId, var5, var4.getSelectedSlot(), SlotActionType.SWAP, clientPlayerEntity);
                  close(minecraftClient, clientPlayerEntity);
                  return false;
               }
            }

            return false;
         } else {
            return false;
         }
      }
   }

   static boolean has(ClientPlayerEntity clientPlayerEntity, Predicate<ItemStack> predicate) {
      PlayerInventory var2 = clientPlayerEntity.getInventory();

      for (int var3 = 0; var3 < 36; var3++) {
         if (predicate.test(var2.getStack(var3))) {
            return true;
         }
      }

      return predicate.test(clientPlayerEntity.getOffHandStack());
   }

   static boolean toOffhand(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, Predicate<ItemStack> predicate) {
      if (clientPlayerEntity.currentScreenHandler == clientPlayerEntity.playerScreenHandler
         && minecraftClient.interactionManager != null
         && minecraftClient.getNetworkHandler() != null
         && clientPlayerEntity.playerScreenHandler.getCursorStack().isEmpty()) {
         PlayerInventory var4 = clientPlayerEntity.getInventory();
         if (predicate.test(clientPlayerEntity.getMainHandStack())) {
            long var5 = System.nanoTime();
            if (var5 - swapSentAt < 1000000000L) {
               return true;
            } else {
               swapSentAt = var5;
               minecraftClient.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
               return true;
            }
         } else {
            for (int var3 = 0; var3 < 9; var3++) {
               if (predicate.test(var4.getStack(var3))) {
                  var4.setSelectedSlot(var3);
                  return true;
               }
            }

            for (int var7 = 9; var7 < 36; var7++) {
               if (predicate.test(var4.getStack(var7))) {
                  if (!InventoryCalm.settle(minecraftClient, clientPlayerEntity)) {
                     return true;
                  }

                  minecraftClient.interactionManager
                     .clickSlot(clientPlayerEntity.playerScreenHandler.syncId, var7, 40, SlotActionType.SWAP, clientPlayerEntity);
                  close(minecraftClient, clientPlayerEntity);
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static void close(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (minecraftClient.currentScreen == null) {
         clientPlayerEntity.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(clientPlayerEntity.playerScreenHandler.syncId));
      }
   }
}
