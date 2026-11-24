package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;

@Environment(EnvType.CLIENT)
public final class InventoryManager {
   private final MinecraftClient mc;
   private final ServerRangeInventorySource serverRange;

   public InventoryManager(MinecraftClient minecraftClient) {
      this.mc = minecraftClient;
      this.serverRange = new ServerRangeInventorySource(minecraftClient);
   }

   public InventoryPickResult prepare(InventoryPickSourceMode inventoryPickSourceMode, Item item, long l, boolean bl) {
      return switch (inventoryPickSourceMode) {
         case SERVER_RANGE -> this.serverRange.prepare(item, l, bl);
         case HOTBAR_ONLY -> this.hotbarOnly(item);
      };
   }

   private InventoryPickResult hotbarOnly(Item item) {
      if (this.mc.player == null) {
         return InventoryPickResult.missing(item);
      } else {
         for (int var2 = 0; var2 < 9; var2++) {
            if (this.mc.player.getInventory().getStack(var2).getItem() == item) {
               return InventoryPickResult.ready(item, null, var2);
            }
         }

         return InventoryPickResult.missing(item);
      }
   }

   public void reset() {
      this.serverRange.reset();
   }
}
