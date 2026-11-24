package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;

@Environment(EnvType.CLIENT)
public interface InventoryPickSource {
   InventoryPickResult prepare(Item item, long l);

   void reset();
}
