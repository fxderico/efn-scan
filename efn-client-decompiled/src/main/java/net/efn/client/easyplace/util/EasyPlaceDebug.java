package net.efn.client.easyplace.util;

import java.util.Locale;
import net.efn.client.easyplace.EasyPlace;
import net.efn.client.easyplace.placement.PlacementState;
import net.efn.client.easyplace.placement.PlacementTarget;
import net.efn.client.easyplace.sync.ServerAckManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

@Environment(EnvType.CLIENT)
public final class EasyPlaceDebug {
   private EasyPlaceDebug() {
   }

   public static void log(String var0, PlacementState var1, PlacementTarget var2, float[] var3, ServerAckManager.Result var4, int var5) {
      ClientPlayerEntity var6 = MinecraftClient.method_1551().field_1724;
      float var7 = var6 != null ? var6.method_36454() : 0.0F / 0.0F;
      float var8 = var6 != null ? var6.method_36455() : 0.0F / 0.0F;
      EasyPlace.LOG
         .info(
            "EasyPlace Debug | event={} | Face={} | PlaceSide={} | Yaw={} | Pitch={} | PlayerYaw={} | PlayerPitch={} | State={} | ACK={} | Retry={}",
            new Object[]{
               var0,
               var2 == null ? "-" : var2.clickedFace(),
               var2 == null ? "-" : var2.placeSide(),
               var3 == null ? "-" : String.format(Locale.ROOT, "%.2f", var3[0]),
               var3 == null ? "-" : String.format(Locale.ROOT, "%.2f", var3[1]),
               String.format(Locale.ROOT, "%.2f", var7),
               String.format(Locale.ROOT, "%.2f", var8),
               var1,
               var4,
               var5
            }
         );
   }
}
