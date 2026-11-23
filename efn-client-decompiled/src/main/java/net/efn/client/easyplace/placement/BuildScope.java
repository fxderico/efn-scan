package net.efn.client.easyplace.placement;

import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public record BuildScope(int maxY, double lowWeight, Predicate<BlockPos> skip) {
   public boolean allows(BlockPos var1) {
      return var1.method_10264() <= this.maxY && !this.skip.test(var1);
   }
}
