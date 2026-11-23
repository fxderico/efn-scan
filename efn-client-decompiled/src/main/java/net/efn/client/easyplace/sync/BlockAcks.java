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

   public static void acked(int i) {
      ClientWorld var1 = MinecraftClient.getInstance().world;
      if (var1 != level) {
         level = var1;
         lastAcked = -1;
      }

      lastAcked = Math.max(lastAcked, i);
   }

   public static int lastSent(ClientWorld clientWorld) {
      return ((AccessorClientLevel)clientWorld).easyplace$predictions().getSequence();
   }

   public static boolean answered(ClientWorld clientWorld, int i) {
      return clientWorld == level && lastAcked >= i;
   }
}
