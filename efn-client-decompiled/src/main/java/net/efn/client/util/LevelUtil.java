package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;

@Environment(EnvType.CLIENT)
public final class LevelUtil {
   private LevelUtil() {
   }

   public static boolean isOverworld(ClientWorld clientWorld) {
      return clientWorld.getRegistryKey() == World.OVERWORLD || clientWorld.getDimensionEntry().matchesKey(DimensionTypes.OVERWORLD);
   }

   public static boolean isLoaded(ClientWorld clientWorld, int i, int j) {
      return clientWorld.getChunkManager().isChunkLoaded(i, j);
   }
}
