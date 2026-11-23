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

   boolean needed(ClientPlayerEntity var1, boolean var2) {
      return next(var1, wants(var2)) != null;
   }

   boolean refill(MinecraftClient var1, ClientPlayerEntity var2, boolean var3) {
      if (var1.field_1761 != null
         && var1.field_1755 == null
         && var2.field_7512 == var2.field_7498
         && var2.field_7498.method_34255().method_7960()
         && !AutoInvTotemModule.busy()
         && !var2.method_6115()) {
         int[] var4 = next(var2, wants(var3));
         if (var4 == null) {
            return false;
         } else {
            int var5 = var2.field_6012;
            if (InventoryCalm.settle(var1, var2) && (var5 < this.lastClick || var5 - this.lastClick >= 4)) {
               var1.field_1761.method_2906(var2.field_7498.field_7763, var4[0], var4[1], SlotActionType.field_7791, var2);
               var2.field_3944.method_52787(new CloseHandledScreenC2SPacket(var2.field_7498.field_7763));
               MeleeAttacks.noteAction(var2);
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

   private static List<BotKit.Want> wants(boolean var0) {
      ArrayList var1 = new ArrayList();
      var1.add(new BotKit.Want(var0x -> var0x.method_31574(Items.field_8301), 16, 32));
      var1.add(new BotKit.Want(var0x -> var0x.method_31574(Items.field_8281), 8, 16));
      var1.add(new BotKit.Want(MeleeAttacks::isMeleeWeapon, 1, 1));
      var1.add(new BotKit.Want(var0x -> var0x.method_31574(Items.field_8367) || var0x.method_31574(Items.field_8463), 1, 1));
      var1.add(new BotKit.Want(var0x -> var0x.method_31574(Items.field_8634), 1, 1));
      var1.add(new BotKit.Want(var0x -> var0x.method_31573(ItemTags.field_42614), 1, 1));
      var1.add(new BotKit.Want(var0x -> var0x.method_31574(Items.field_8287), 1, 1));
      if (var0) {
         var1.add(new BotKit.Want(var0x -> var0x.method_31574(Items.field_23141), 2, 4));
         var1.add(new BotKit.Want(var0x -> var0x.method_31574(Items.field_8801), 2, 4));
      }

      return var1;
   }

   private static int[] next(ClientPlayerEntity var0, List<BotKit.Want> var1) {
      PlayerInventory var2 = var0.method_31548();

      for (BotKit.Want var4 : var1) {
         int var6 = 0;
         int var7 = -1;

         for (int var8 = 0; var8 < 9; var8++) {
            ItemStack var9 = var2.method_5438(var8);
            if (var4.item().test(var9)) {
               var6 += var9.method_7947();
               if (var7 < 0 || var9.method_7947() < var2.method_5438(var7).method_7947()) {
                  var7 = var8;
               }
            }
         }

         if (var6 < var4.low()) {
            int var11 = -1;

            for (int var12 = 9; var12 < 36; var12++) {
               ItemStack var10 = var2.method_5438(var12);
               if (var4.item().test(var10) && (var11 < 0 || var10.method_7947() > var2.method_5438(var11).method_7947())) {
                  var11 = var12;
               }
            }

            if (var11 >= 0) {
               int var13 = var2.method_5438(var11).method_7947();
               if (var7 >= 0 && var13 > var6 && (var6 < var4.topUp() || var13 >= var6 + 8)) {
                  return new int[]{var11, var7};
               }

               int var5;
               if (var7 < 0 && (var5 = freeSlot(var2, var1)) >= 0) {
                  return new int[]{var11, var5};
               }
            }
         }
      }

      return null;
   }

   private static int freeSlot(PlayerInventory var0, List<BotKit.Want> var1) {
      for (int var2 = 0; var2 < 9; var2++) {
         if (var0.method_5438(var2).method_7960()) {
            return var2;
         }
      }

      for (int var7 = 8; var7 >= 0; var7--) {
         ItemStack var3 = var0.method_5438(var7);
         if (!var3.method_31574(Items.field_8288)) {
            boolean var4 = false;

            for (BotKit.Want var6 : var1) {
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
