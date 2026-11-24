package net.efn.client.easyplace.hopper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

@Environment(EnvType.CLIENT)
public final class HopperPlacementHandler {
   public HopperPlacementHandler() {
   }

   public boolean isHopper(BlockState blockState) {
      return blockState.getBlock() instanceof HopperBlock;
   }

   public boolean faceCanProduceExpectedFacing(BlockState blockState, Direction direction) {
      if (this.isHopper(blockState) && blockState.contains(Properties.HOPPER_FACING)) {
         Direction var3 = blockState.get(Properties.HOPPER_FACING);
         Direction var4 = direction.getOpposite();
         if (var4.getAxis() == Axis.Y) {
            var4 = Direction.DOWN;
         }

         return var4 == var3;
      } else {
         return true;
      }
   }

   public int facingScore(BlockState blockState, Direction direction) {
      return this.faceCanProduceExpectedFacing(blockState, direction) ? 1000 : -1000;
   }
}
