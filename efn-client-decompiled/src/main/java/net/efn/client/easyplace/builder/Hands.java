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

   static boolean hold(MinecraftClient var0, ClientPlayerEntity var1, Predicate<ItemStack> var2) {
      PlayerInventory var4 = var1.method_31548();
      if (var2.test(var1.method_6047())) {
         return true;
      } else {
         for (int var3 = 0; var3 < 9; var3++) {
            if (var2.test(var4.method_5438(var3))) {
               var4.method_61496(var3);
               return true;
            }
         }

         if (var1.field_7512 == var1.field_7498 && var0.field_1761 != null) {
            for (int var5 = 9; var5 < 36; var5++) {
               if (var2.test(var4.method_5438(var5))) {
                  if (!InventoryCalm.settle(var0, var1)) {
                     return false;
                  }

                  var0.field_1761.method_2906(var1.field_7498.field_7763, var5, var4.method_67532(), SlotActionType.field_7791, var1);
                  close(var0, var1);
                  return false;
               }
            }

            return false;
         } else {
            return false;
         }
      }
   }

   static boolean has(ClientPlayerEntity var0, Predicate<ItemStack> var1) {
      PlayerInventory var2 = var0.method_31548();

      for (int var3 = 0; var3 < 36; var3++) {
         if (var1.test(var2.method_5438(var3))) {
            return true;
         }
      }

      return var1.test(var0.method_6079());
   }

   static boolean toOffhand(MinecraftClient var0, ClientPlayerEntity var1, Predicate<ItemStack> var2) {
      if (var1.field_7512 == var1.field_7498 && var0.field_1761 != null && var0.method_1562() != null && var1.field_7498.method_34255().method_7960()) {
         PlayerInventory var4 = var1.method_31548();
         if (var2.test(var1.method_6047())) {
            long var5 = System.nanoTime();
            if (var5 - swapSentAt < 1000000000L) {
               return true;
            } else {
               swapSentAt = var5;
               var0.method_1562().method_52787(new PlayerActionC2SPacket(Action.field_12969, BlockPos.field_10980, Direction.field_11033));
               return true;
            }
         } else {
            for (int var3 = 0; var3 < 9; var3++) {
               if (var2.test(var4.method_5438(var3))) {
                  var4.method_61496(var3);
                  return true;
               }
            }

            for (int var7 = 9; var7 < 36; var7++) {
               if (var2.test(var4.method_5438(var7))) {
                  if (!InventoryCalm.settle(var0, var1)) {
                     return true;
                  }

                  var0.field_1761.method_2906(var1.field_7498.field_7763, var7, 40, SlotActionType.field_7791, var1);
                  close(var0, var1);
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static void close(MinecraftClient var0, ClientPlayerEntity var1) {
      if (var0.field_1755 == null) {
         var1.field_3944.method_52787(new CloseHandledScreenC2SPacket(var1.field_7498.field_7763));
      }
   }
}
