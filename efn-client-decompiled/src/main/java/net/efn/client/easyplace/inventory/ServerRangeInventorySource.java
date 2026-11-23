package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public final class ServerRangeInventorySource implements InventoryPickSource {
   private static final int SYNC_TIMEOUT_TICKS = 12;
   private static final int HOTBAR_SIZE = 9;
   private static final int MAIN_END = 35;
   private static final int OFFHAND_SLOT = 40;
   private final MinecraftClient mc;
   private ServerRangeInventorySource.PendingPick pending;

   public ServerRangeInventorySource(MinecraftClient var1) {
      this.mc = var1;
   }

   @Override
   public InventoryPickResult prepare(Item var1, long var2) {
      return this.prepare(var1, var2, false);
   }

   public InventoryPickResult prepare(Item var1, long var2, boolean var4) {
      ClientPlayerEntity var5 = this.mc.field_1724;
      if (var5 != null && this.mc.field_1761 != null) {
         if (this.pending != null) {
            if (this.pending.item == var1 && var2 - this.pending.startedTick <= 12L) {
               return this.pollPending();
            }

            this.pending = null;
         }

         if (var5.method_6079().method_31574(var1)) {
            return InventoryPickResult.ready(var1, Hand.field_5810, 40);
         } else {
            PlayerInventory var6 = var5.method_31548();
            if (var5.method_6047().method_31574(var1)) {
               return InventoryPickResult.ready(var1, Hand.field_5808, var6.method_67532());
            } else {
               int var7 = find(var6, var1);
               if (var7 < 0) {
                  return InventoryPickResult.missing(var1);
               } else if (var7 < 9) {
                  return InventoryPickResult.ready(var1, Hand.field_5808, var7);
               } else if (var5.field_7512 != var5.field_7498) {
                  return InventoryPickResult.missing(var1);
               } else {
                  int var8 = findHotbarDestination(var6);
                  if (var4 ? InventoryCalm.settle(this.mc, var5) : InventoryCalm.idle(var5)) {
                     this.mc.field_1761.method_2906(var5.field_7498.field_7763, var7, var8, SlotActionType.field_7791, var5);
                     if (this.mc.field_1755 == null) {
                        var5.field_3944.method_52787(new CloseHandledScreenC2SPacket(var5.field_7498.field_7763));
                     }

                     this.pending = new ServerRangeInventorySource.PendingPick(var1, var8, var2);
                     return InventoryPickResult.waiting(var1, var8);
                  } else {
                     return InventoryPickResult.waiting(var1, var8);
                  }
               }
            }
         }
      } else {
         return InventoryPickResult.missing(var1);
      }
   }

   private InventoryPickResult pollPending() {
      if (!this.mc.field_1724.method_31548().method_5438(this.pending.hotbarSlot).method_31574(this.pending.item)) {
         return InventoryPickResult.waiting(this.pending.item, this.pending.hotbarSlot);
      } else {
         InventoryPickResult var1 = InventoryPickResult.ready(this.pending.item, Hand.field_5808, this.pending.hotbarSlot);
         this.pending = null;
         return var1;
      }
   }

   private static int find(PlayerInventory var0, Item var1) {
      for (int var2 = 0; var2 <= 35; var2++) {
         if (var0.method_5438(var2).method_31574(var1)) {
            return var2;
         }
      }

      return -1;
   }

   private static int findHotbarDestination(PlayerInventory var0) {
      for (int var1 = 0; var1 < 9; var1++) {
         if (var0.method_5438(var1).method_7960()) {
            return var1;
         }
      }

      return var0.method_67532();
   }

   @Override
   public void reset() {
      this.pending = null;
   }

   @Environment(EnvType.CLIENT)
   private record PendingPick(Item item, int hotbarSlot, long startedTick) {
   }
}
