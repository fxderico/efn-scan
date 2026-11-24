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

   public ServerRangeInventorySource(MinecraftClient minecraftClient) {
      this.mc = minecraftClient;
   }

   @Override
   public InventoryPickResult prepare(Item item, long l) {
      return this.prepare(item, l, false);
   }

   public InventoryPickResult prepare(Item item, long l, boolean bl) {
      ClientPlayerEntity var5 = this.mc.player;
      if (var5 != null && this.mc.interactionManager != null) {
         if (this.pending != null) {
            if (this.pending.item == item && l - this.pending.startedTick <= 12L) {
               return this.pollPending();
            }

            this.pending = null;
         }

         if (var5.getOffHandStack().isOf(item)) {
            return InventoryPickResult.ready(item, Hand.OFF_HAND, 40);
         } else {
            PlayerInventory var6 = var5.getInventory();
            if (var5.getMainHandStack().isOf(item)) {
               return InventoryPickResult.ready(item, Hand.MAIN_HAND, var6.getSelectedSlot());
            } else {
               int var7 = find(var6, item);
               if (var7 < 0) {
                  return InventoryPickResult.missing(item);
               } else if (var7 < 9) {
                  return InventoryPickResult.ready(item, Hand.MAIN_HAND, var7);
               } else if (var5.currentScreenHandler != var5.playerScreenHandler) {
                  return InventoryPickResult.missing(item);
               } else {
                  int var8 = findHotbarDestination(var6);
                  if (bl ? InventoryCalm.settle(this.mc, var5) : InventoryCalm.idle(var5)) {
                     this.mc.interactionManager.clickSlot(var5.playerScreenHandler.syncId, var7, var8, SlotActionType.SWAP, var5);
                     if (this.mc.currentScreen == null) {
                        var5.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(var5.playerScreenHandler.syncId));
                     }

                     this.pending = new ServerRangeInventorySource.PendingPick(item, var8, l);
                     return InventoryPickResult.waiting(item, var8);
                  } else {
                     return InventoryPickResult.waiting(item, var8);
                  }
               }
            }
         }
      } else {
         return InventoryPickResult.missing(item);
      }
   }

   private InventoryPickResult pollPending() {
      if (!this.mc.player.getInventory().getStack(this.pending.hotbarSlot).isOf(this.pending.item)) {
         return InventoryPickResult.waiting(this.pending.item, this.pending.hotbarSlot);
      } else {
         InventoryPickResult var1 = InventoryPickResult.ready(this.pending.item, Hand.MAIN_HAND, this.pending.hotbarSlot);
         this.pending = null;
         return var1;
      }
   }

   private static int find(PlayerInventory playerInventory, Item item) {
      for (int var2 = 0; var2 <= 35; var2++) {
         if (playerInventory.getStack(var2).isOf(item)) {
            return var2;
         }
      }

      return -1;
   }

   private static int findHotbarDestination(PlayerInventory playerInventory) {
      for (int var1 = 0; var1 < 9; var1++) {
         if (playerInventory.getStack(var1).isEmpty()) {
            return var1;
         }
      }

      return playerInventory.getSelectedSlot();
   }

   @Override
   public void reset() {
      this.pending = null;
   }

   @Environment(EnvType.CLIENT)
   private record PendingPick(Item item, int hotbarSlot, long startedTick) {
   }
}
