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

   public static void log(String string, PlacementState placementState, PlacementTarget placementTarget, float[] fs, ServerAckManager.Result result, int i) {
      ClientPlayerEntity var6 = MinecraftClient.getInstance().player;
      float var7 = var6 != null ? var6.getYaw() : 0.0F / 0.0F;
      float var8 = var6 != null ? var6.getPitch() : 0.0F / 0.0F;
      EasyPlace.LOG
         .info(
            "EasyPlace Debug | event={} | Face={} | PlaceSide={} | Yaw={} | Pitch={} | PlayerYaw={} | PlayerPitch={} | State={} | ACK={} | Retry={}",
            string,
            placementTarget == null ? "-" : placementTarget.clickedFace(),
            placementTarget == null ? "-" : placementTarget.placeSide(),
            fs == null ? "-" : String.format(Locale.ROOT, "%.2f", fs[0]),
            fs == null ? "-" : String.format(Locale.ROOT, "%.2f", fs[1]),
            String.format(Locale.ROOT, "%.2f", var7),
            String.format(Locale.ROOT, "%.2f", var8),
            placementState,
            result,
            i
         );
   }
}
