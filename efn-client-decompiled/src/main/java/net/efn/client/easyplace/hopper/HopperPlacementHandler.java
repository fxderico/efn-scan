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

   public boolean isHopper(BlockState var1) {
      return var1.method_26204() instanceof HopperBlock;
   }

   public boolean faceCanProduceExpectedFacing(BlockState var1, Direction var2) {
      if (this.isHopper(var1) && var1.method_28498(Properties.field_12545)) {
         Direction var3 = (Direction)var1.method_11654(Properties.field_12545);
         Direction var4 = var2.method_10153();
         if (var4.method_10166() == Axis.field_11052) {
            var4 = Direction.field_11033;
         }

         return var4 == var3;
      } else {
         return true;
      }
   }

   public int facingScore(BlockState var1, Direction var2) {
      return this.faceCanProduceExpectedFacing(var1, var2) ? 1000 : -1000;
   }
}
