package net.efn.client.easyplace.sync;

import net.efn.client.easyplace.mixin.AccessorClientLevel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public final class BlockAcks {
   private static ClientWorld level;
   private static int lastAcked = -1;

   private BlockAcks() {
   }

   public static void acked(int var0) {
      ClientWorld var1 = MinecraftClient.method_1551().field_1687;
      if (var1 != level) {
         level = var1;
         lastAcked = -1;
      }

      lastAcked = Math.max(lastAcked, var0);
   }

   public static int lastSent(ClientWorld var0) {
      return ((AccessorClientLevel)var0).easyplace$predictions().method_41942();
   }

   public static boolean answered(ClientWorld var0, int var1) {
      return var0 == level && lastAcked >= var1;
   }
}
