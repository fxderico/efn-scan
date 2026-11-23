package net.efn.client.easyplace.placement;

import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public record BuildScope(int maxY, double lowWeight, Predicate<BlockPos> skip) {
   public boolean allows(BlockPos blockPos) {
      return blockPos.getY() <= this.maxY && !this.skip.test(blockPos);
   }
}
