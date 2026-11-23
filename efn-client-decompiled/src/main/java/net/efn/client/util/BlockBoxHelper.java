package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

@Environment(EnvType.CLIENT)
public final class BlockBoxHelper {
   private static final double HALF_BLOCK = 0.5;
   private static final float MIN_SIZE = 0.1F;
   private static final float MAX_SIZE = 2.0F;

   private BlockBoxHelper() {
   }

   public static Box scaledBlockBox(BlockPos var0, float var1) {
      float var2 = Math.max(0.1F, Math.min(2.0F, var1));
      double var3 = var2 * 0.5;
      double var5 = var0.method_10263() + 0.5;
      double var7 = var0.method_10264() + 0.5;
      double var9 = var0.method_10260() + 0.5;
      return new Box(var5 - var3, var7 - var3, var9 - var3, var5 + var3, var7 + var3, var9 + var3);
   }
}
