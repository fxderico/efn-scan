package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum InventoryPickSourceMode {
   SERVER_RANGE("Server Range"),
   HOTBAR_ONLY("Hotbar Only");

   private final String title;

   private InventoryPickSourceMode(String nullxx) {
      this.title = nullxx;
   }

   public String title() {
      return this.title;
   }

   public static String[] titles() {
      InventoryPickSourceMode[] var0 = values();
      String[] var1 = new String[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = var0[var2].title;
      }

      return var1;
   }

   public static InventoryPickSourceMode of(String var0) {
      for (InventoryPickSourceMode var4 : values()) {
         if (var4.title.equals(var0)) {
            return var4;
         }
      }

      return HOTBAR_ONLY;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
