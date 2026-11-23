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

   public static boolean statesMatch(BlockState blockState, BlockState blockState2) {
      return StateRules.placed(blockState2, blockState) || StateRules.slabHalf(blockState2, blockState);
   }

   public static TargetStatus classify(BlockState blockState, BlockState blockState2) {
      if (StateRules.done(blockState2, blockState)) {
         return TargetStatus.CORRECT;
      } else if (StateRules.placed(blockState2, blockState)) {
         return TargetStatus.ADJUST;
      } else {
         return !blockState2.isAir() && !blockState2.isReplaceable() && !StateRules.slabHalf(blockState2, blockState)
            ? TargetStatus.WRONG
            : TargetStatus.MISSING;
      }
   }

   public static Item requiredItem(BlockState blockState) {
      Item var1 = StateRules.plant(blockState.getBlock()).asItem();
      return var1 == Items.AIR ? null : var1;
   }

   public static boolean isPrimaryPlacementState(BlockState blockState) {
      return blockState.contains(Properties.DOUBLE_BLOCK_HALF) && blockState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
         ? false
         : !blockState.contains(Properties.BED_PART) || blockState.get(Properties.BED_PART) != BedPart.HEAD;
   }

   public static boolean isSimpleFullBlock(BlockState blockState, World world, BlockPos blockPos) {
      return blockState.isFullCube(world, blockPos) && blockState.getBlock().asItem() != Items.AIR;
   }
}
