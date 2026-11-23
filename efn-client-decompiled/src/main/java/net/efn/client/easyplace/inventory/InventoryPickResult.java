package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public record InventoryPickResult(InventoryPickResult.Status status, Item item, Hand hand, int slot) {
   public static InventoryPickResult ready(Item var0, Hand var1, int var2) {
      return new InventoryPickResult(InventoryPickResult.Status.READY, var0, var1, var2);
   }

   public static InventoryPickResult waiting(Item var0, int var1) {
      return new InventoryPickResult(InventoryPickResult.Status.WAITING, var0, null, var1);
   }

   public static InventoryPickResult missing(Item var0) {
      return new InventoryPickResult(InventoryPickResult.Status.MISSING, var0, null, -1);
   }

   @Environment(EnvType.CLIENT)
   public static enum Status {
      READY,
      WAITING,
      MISSING;

      private Status() {
      }
   }
}
