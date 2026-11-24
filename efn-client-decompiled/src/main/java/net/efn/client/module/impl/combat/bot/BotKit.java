package net.efn.client.module.impl.combat.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.efn.client.easyplace.inventory.InventoryCalm;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
final class BotKit {
   private static final int CLICK_GAP = 4;
   private int lastClick = -4;

   BotKit() {
   }

   boolean needed(ClientPlayerEntity clientPlayerEntity, boolean bl) {
      return next(clientPlayerEntity, wants(bl)) != null;
   }

   boolean refill(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, boolean bl) {
      if (minecraftClient.interactionManager != null
         && minecraftClient.currentScreen == null
         && clientPlayerEntity.currentScreenHandler == clientPlayerEntity.playerScreenHandler
         && clientPlayerEntity.playerScreenHandler.getCursorStack().isEmpty()
         && !AutoInvTotemModule.busy()
         && !clientPlayerEntity.isUsingItem()) {
         int[] var4 = next(clientPlayerEntity, wants(bl));
         if (var4 == null) {
            return false;
         } else {
            int var5 = clientPlayerEntity.age;
            if (InventoryCalm.settle(minecraftClient, clientPlayerEntity) && (var5 < this.lastClick || var5 - this.lastClick >= 4)) {
               minecraftClient.interactionManager
                  .clickSlot(clientPlayerEntity.playerScreenHandler.syncId, var4[0], var4[1], SlotActionType.SWAP, clientPlayerEntity);
               clientPlayerEntity.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(clientPlayerEntity.playerScreenHandler.syncId));
               MeleeAttacks.noteAction(clientPlayerEntity);
               this.lastClick = var5;
               return true;
            } else {
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static List<BotKit.Want> wants(boolean bl) {
      ArrayList var1 = new ArrayList();
      var1.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.END_CRYSTAL), 16, 32));
      var1.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.OBSIDIAN), 8, 16));
      var1.add(new BotKit.Want(MeleeAttacks::isMeleeWeapon, 1, 1));
      var1.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE) || itemStack.isOf(Items.GOLDEN_APPLE), 1, 1));
      var1.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.ENDER_PEARL), 1, 1));
      var1.add(new BotKit.Want(itemStack -> itemStack.isIn(ItemTags.PICKAXES), 1, 1));
      var1.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.EXPERIENCE_BOTTLE), 1, 1));
      if (bl) {
         var1.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.RESPAWN_ANCHOR), 2, 4));
         var1.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.GLOWSTONE), 2, 4));
      }

      return var1;
   }

   private static int[] next(ClientPlayerEntity clientPlayerEntity, List<BotKit.Want> list) {
      PlayerInventory var2 = clientPlayerEntity.getInventory();

      for (BotKit.Want var4 : list) {
         int var6 = 0;
         int var7 = -1;

         for (int var8 = 0; var8 < 9; var8++) {
            ItemStack var9 = var2.getStack(var8);
            if (var4.item().test(var9)) {
               var6 += var9.getCount();
               if (var7 < 0 || var9.getCount() < var2.getStack(var7).getCount()) {
                  var7 = var8;
               }
            }
         }

         if (var6 < var4.low()) {
            int var11 = -1;

            for (int var12 = 9; var12 < 36; var12++) {
               ItemStack var10 = var2.getStack(var12);
               if (var4.item().test(var10) && (var11 < 0 || var10.getCount() > var2.getStack(var11).getCount())) {
                  var11 = var12;
               }
            }

            if (var11 >= 0) {
               int var13 = var2.getStack(var11).getCount();
               if (var7 >= 0 && var13 > var6 && (var6 < var4.topUp() || var13 >= var6 + 8)) {
                  return new int[]{var11, var7};
               }

               int var5;
               if (var7 < 0 && (var5 = freeSlot(var2, list)) >= 0) {
                  return new int[]{var11, var5};
               }
            }
         }
      }

      return null;
   }

   private static int freeSlot(PlayerInventory playerInventory, List<BotKit.Want> list) {
      for (int var2 = 0; var2 < 9; var2++) {
         if (playerInventory.getStack(var2).isEmpty()) {
            return var2;
         }
      }

      for (int var7 = 8; var7 >= 0; var7--) {
         ItemStack var3 = playerInventory.getStack(var7);
         if (!var3.isOf(Items.TOTEM_OF_UNDYING)) {
            boolean var4 = false;

            for (BotKit.Want var6 : list) {
               var4 |= var6.item().test(var3);
            }

            if (!var4) {
               return var7;
            }
         }
      }

      return -1;
   }

   @Environment(EnvType.CLIENT)
   private record Want(Predicate<ItemStack> item, int low, int topUp) {
   }
}
