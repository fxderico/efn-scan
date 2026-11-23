package net.efn.client.easyplace.util;

import net.efn.client.easyplace.schematic.TargetStatus;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class BlockStateUtil {
   private BlockStateUtil() {
   }

   public static boolean statesMatch(BlockState var0, BlockState var1) {
      return StateRules.placed(var1, var0) || StateRules.slabHalf(var1, var0);
   }

   public static TargetStatus classify(BlockState var0, BlockState var1) {
      if (StateRules.done(var1, var0)) {
         return TargetStatus.CORRECT;
      } else if (StateRules.placed(var1, var0)) {
         return TargetStatus.ADJUST;
      } else {
         return !var1.method_26215() && !var1.method_45474() && !StateRules.slabHalf(var1, var0) ? TargetStatus.WRONG : TargetStatus.MISSING;
      }
   }

   public static Item requiredItem(BlockState var0) {
      Item var1 = StateRules.plant(var0.method_26204()).method_8389();
      return var1 == Items.field_8162 ? null : var1;
   }

   public static boolean isPrimaryPlacementState(BlockState var0) {
      return var0.method_28498(Properties.field_12533) && var0.method_11654(Properties.field_12533) == DoubleBlockHalf.field_12609
         ? false
         : !var0.method_28498(Properties.field_12483) || var0.method_11654(Properties.field_12483) != BedPart.field_12560;
   }

   public static boolean isSimpleFullBlock(BlockState var0, World var1, BlockPos var2) {
      return var0.method_26234(var1, var2) && var0.method_26204().method_8389() != Items.field_8162;
   }
}
