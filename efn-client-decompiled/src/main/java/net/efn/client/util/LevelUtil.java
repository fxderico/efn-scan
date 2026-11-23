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

   public static boolean isOverworld(ClientWorld var0) {
      return var0.method_27983() == World.field_25179 || var0.method_40134().method_40225(DimensionTypes.field_37666);
   }

   public static boolean isLoaded(ClientWorld var0, int var1, int var2) {
      return var0.method_2935().method_12123(var1, var2);
   }
}
