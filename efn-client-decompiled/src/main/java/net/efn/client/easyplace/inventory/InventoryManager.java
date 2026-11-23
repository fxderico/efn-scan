package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;

@Environment(EnvType.CLIENT)
public final class InventoryManager {
   private final MinecraftClient mc;
   private final ServerRangeInventorySource serverRange;

   public InventoryManager(MinecraftClient var1) {
      this.mc = var1;
      this.serverRange = new ServerRangeInventorySource(var1);
   }

   public InventoryPickResult prepare(InventoryPickSourceMode var1, Item var2, long var3, boolean var5) {
      return switch (var1) {
         case SERVER_RANGE -> this.serverRange.prepare(var2, var3, var5);
         case HOTBAR_ONLY -> this.hotbarOnly(var2);
      };
   }

   private InventoryPickResult hotbarOnly(Item var1) {
      if (this.mc.field_1724 == null) {
         return InventoryPickResult.missing(var1);
      } else {
         for (int var2 = 0; var2 < 9; var2++) {
            if (this.mc.field_1724.method_31548().method_5438(var2).method_7909() == var1) {
               return InventoryPickResult.ready(var1, null, var2);
            }
         }

         return InventoryPickResult.missing(var1);
      }
   }

   public void reset() {
      this.serverRange.reset();
   }
}
