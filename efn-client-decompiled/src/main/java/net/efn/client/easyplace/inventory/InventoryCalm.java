package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class InventoryCalm {
   private static final double STOPPED = 0.0025;
   private static final double HALTED = 1.0E-4;

   private InventoryCalm() {
   }

   public static boolean idle(ClientPlayerEntity var0) {
      PlayerInput var1 = var0.field_3913.field_54155;
      return !moving(var1) && !var1.comp_3164();
   }

   public static boolean still(ClientPlayerEntity var0) {
      return idle(var0) && var0.method_24828() && var0.method_18798().method_37268() < 0.0025;
   }

   public static boolean settle(MinecraftClient var0, ClientPlayerEntity var1) {
      if (still(var1)) {
         return true;
      } else {
         GameOptions var2 = var0.field_1690;
         var2.field_1894.method_23481(false);
         var2.field_1881.method_23481(false);
         var2.field_1913.method_23481(false);
         var2.field_1849.method_23481(false);
         var2.field_1903.method_23481(false);
         var2.field_1867.method_23481(false);
         var2.field_1904.method_23481(false);
         var2.field_1886.method_23481(false);
         PlayerInput var3 = var1.field_3913.field_54155;
         if (moving(var3) || !var1.method_24828() || var1.method_18798().method_37268() >= 1.0E-4) {
            return false;
         } else if (!overGround(var1)) {
            return true;
         } else {
            var2.field_1832.method_23481(false);
            return false;
         }
      }
   }

   private static boolean moving(PlayerInput var0) {
      return var0.comp_3159() || var0.comp_3160() || var0.comp_3161() || var0.comp_3162() || var0.comp_3163() || var0.comp_3165();
   }

   private static boolean overGround(ClientPlayerEntity var0) {
      BlockPos var1 = BlockPos.method_49637(var0.method_23317(), var0.method_23318() - 0.05, var0.method_23321());
      return !var0.method_73183().method_8320(var1).method_26220(var0.method_73183(), var1).method_1110();
   }
}
