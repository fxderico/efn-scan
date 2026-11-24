package net.efn.client.easyplace.util;

import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.MultifaceBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public final class StateRules {
   private static final Set<Property<?>> PLACED = Set.of(
      Properties.FACING,
      Properties.HORIZONTAL_FACING,
      Properties.HOPPER_FACING,
      Properties.VERTICAL_DIRECTION,
      Properties.AXIS,
      Properties.HORIZONTAL_AXIS,
      Properties.BLOCK_HALF,
      Properties.DOUBLE_BLOCK_HALF,
      Properties.SLAB_TYPE,
      Properties.ROTATION,
      Properties.BLOCK_FACE,
      Properties.ORIENTATION,
      Properties.DOOR_HINGE,
      Properties.CHEST_TYPE,
      Properties.BED_PART,
      Properties.ATTACHMENT,
      Properties.HANGING
   );
   private static final Set<Property<?>> CLICKED = Set.of(Properties.DELAY, Properties.COMPARATOR_MODE, Properties.NOTE, Properties.OPEN, Properties.INVERTED);
   private static final Map<Block, Block> TIPS = Map.of(
      Blocks.KELP_PLANT,
      Blocks.KELP,
      Blocks.TWISTING_VINES_PLANT,
      Blocks.TWISTING_VINES,
      Blocks.WEEPING_VINES_PLANT,
      Blocks.WEEPING_VINES,
      Blocks.CAVE_VINES_PLANT,
      Blocks.CAVE_VINES
   );

   private StateRules() {
   }

   public static Block plant(Block block) {
      return TIPS.getOrDefault(block, block);
   }

   public static boolean placed(BlockState blockState, BlockState blockState2) {
      if (plant(blockState.getBlock()) != plant(blockState2.getBlock())) {
         return false;
      } else {
         for (Property var3 : blockState2.getProperties()) {
            if (blockState.contains(var3) && (PLACED.contains(var3) || faceProperty(blockState2, var3)) && !blockState.get(var3).equals(blockState2.get(var3))) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean done(BlockState blockState, BlockState blockState2) {
      if (!placed(blockState, blockState2)) {
         return false;
      } else {
         for (Property var3 : blockState2.getProperties()) {
            if (clickable(blockState2, var3) && blockState.contains(var3) && !blockState.get(var3).equals(blockState2.get(var3))) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean needsClicks(BlockState blockState, BlockState blockState2) {
      return placed(blockState, blockState2) && !done(blockState, blockState2);
   }

   public static boolean wouldFit(BlockState blockState, BlockState blockState2, boolean bl) {
      if (plant(blockState.getBlock()) != plant(blockState2.getBlock())) {
         return false;
      } else {
         for (Property var4 : blockState2.getProperties()) {
            if (blockState.contains(var4)
               && (!bl || var4 != Properties.CHEST_TYPE)
               && (var4 != Properties.SLAB_TYPE || blockState2.get(Properties.SLAB_TYPE) != SlabType.DOUBLE)
               && (
                  faceProperty(blockState2, var4)
                     ? blockState.<Boolean>get(var4) && !blockState2.<Boolean>get(var4)
                     : PLACED.contains(var4) && !blockState.get(var4).equals(blockState2.get(var4))
               )) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean slabHalf(BlockState blockState, BlockState blockState2) {
      return blockState2.getBlock() instanceof SlabBlock
         && blockState.getBlock() == blockState2.getBlock()
         && blockState2.get(Properties.SLAB_TYPE) == SlabType.DOUBLE
         && blockState.get(Properties.SLAB_TYPE) != SlabType.DOUBLE;
   }

   public static int itemsNeeded(BlockState blockState, BlockState blockState2) {
      if (blockState2.getBlock() instanceof SlabBlock && blockState2.get(Properties.SLAB_TYPE) == SlabType.DOUBLE) {
         return slabHalf(blockState, blockState2) ? 1 : 2;
      } else {
         return 1;
      }
   }

   public static boolean clickable(BlockState blockState, Property<?> property) {
      if (!CLICKED.contains(property)) {
         return false;
      } else if (property != Properties.OPEN) {
         return true;
      } else {
         Block var2 = blockState.getBlock();
         return var2 != Blocks.IRON_DOOR && var2 != Blocks.IRON_TRAPDOOR
            ? var2 instanceof DoorBlock || var2 instanceof TrapdoorBlock || var2 instanceof FenceGateBlock
            : false;
      }
   }

   public static Direction lookFor(BlockState blockState) {
      Block var1 = blockState.getBlock();
      if (var1 instanceof CrafterBlock) {
         return blockState.get(Properties.ORIENTATION).getFacing().getOpposite();
      } else if (var1 instanceof ObserverBlock) {
         return blockState.get(Properties.FACING);
      } else {
         return !(var1 instanceof PistonBlock) && !(var1 instanceof DispenserBlock) && !(var1 instanceof BarrelBlock)
            ? null
            : blockState.get(Properties.FACING).getOpposite();
      }
   }

   public static Direction flatLookFor(BlockState blockState) {
      if (!blockState.contains(Properties.HORIZONTAL_FACING)) {
         return null;
      } else {
         Block var1 = blockState.getBlock();
         Direction var2 = blockState.get(Properties.HORIZONTAL_FACING);
         if (var1 instanceof ChestBlock || var1 instanceof AbstractFurnaceBlock || var1 instanceof AbstractRedstoneGateBlock) {
            return var2.getOpposite();
         } else {
            return !(var1 instanceof StairsBlock) && !(var1 instanceof DoorBlock) && !(var1 instanceof FenceGateBlock) ? null : var2;
         }
      }
   }

   private static boolean faceProperty(BlockState blockState, Property<?> property) {
      Block var2 = blockState.getBlock();
      return (var2 instanceof MultifaceBlock || var2 instanceof VineBlock)
         && property instanceof BooleanProperty
         && ConnectingBlock.FACING_PROPERTIES.containsValue(property);
   }
}
