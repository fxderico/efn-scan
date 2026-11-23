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
import net.minecraft.block.enums.Orientation;
import net.minecraft.block.enums.SlabType;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public final class StateRules {
   private static final Set<Property<?>> PLACED = Set.of(
      Properties.field_12525,
      Properties.field_12481,
      Properties.field_12545,
      Properties.field_28062,
      Properties.field_12496,
      Properties.field_12529,
      Properties.field_12518,
      Properties.field_12533,
      Properties.field_12485,
      Properties.field_12532,
      Properties.field_12555,
      Properties.field_23333,
      Properties.field_12520,
      Properties.field_12506,
      Properties.field_12483,
      Properties.field_17104,
      Properties.field_16561
   );
   private static final Set<Property<?>> CLICKED = Set.of(
      Properties.field_12494, Properties.field_12534, Properties.field_12524, Properties.field_12537, Properties.field_12501
   );
   private static final Map<Block, Block> TIPS = Map.of(
      Blocks.field_10463,
      Blocks.field_9993,
      Blocks.field_23079,
      Blocks.field_23078,
      Blocks.field_22124,
      Blocks.field_22123,
      Blocks.field_28676,
      Blocks.field_28675
   );

   private StateRules() {
   }

   public static Block plant(Block var0) {
      return TIPS.getOrDefault(var0, var0);
   }

   public static boolean placed(BlockState var0, BlockState var1) {
      if (plant(var0.method_26204()) != plant(var1.method_26204())) {
         return false;
      } else {
         for (Property var3 : var1.method_28501()) {
            if (var0.method_28498(var3) && (PLACED.contains(var3) || faceProperty(var1, var3)) && !var0.method_11654(var3).equals(var1.method_11654(var3))) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean done(BlockState var0, BlockState var1) {
      if (!placed(var0, var1)) {
         return false;
      } else {
         for (Property var3 : var1.method_28501()) {
            if (clickable(var1, var3) && var0.method_28498(var3) && !var0.method_11654(var3).equals(var1.method_11654(var3))) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean needsClicks(BlockState var0, BlockState var1) {
      return placed(var0, var1) && !done(var0, var1);
   }

   public static boolean wouldFit(BlockState var0, BlockState var1, boolean var2) {
      if (plant(var0.method_26204()) != plant(var1.method_26204())) {
         return false;
      } else {
         for (Property var4 : var1.method_28501()) {
            if (var0.method_28498(var4)
               && (!var2 || var4 != Properties.field_12506)
               && (var4 != Properties.field_12485 || var1.method_11654(Properties.field_12485) != SlabType.field_12682)
               && (
                  faceProperty(var1, var4)
                     ? (Boolean)var0.method_11654(var4) && !(Boolean)var1.method_11654(var4)
                     : PLACED.contains(var4) && !var0.method_11654(var4).equals(var1.method_11654(var4))
               )) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean slabHalf(BlockState var0, BlockState var1) {
      return var1.method_26204() instanceof SlabBlock
         && var0.method_26204() == var1.method_26204()
         && var1.method_11654(Properties.field_12485) == SlabType.field_12682
         && var0.method_11654(Properties.field_12485) != SlabType.field_12682;
   }

   public static int itemsNeeded(BlockState var0, BlockState var1) {
      if (var1.method_26204() instanceof SlabBlock && var1.method_11654(Properties.field_12485) == SlabType.field_12682) {
         return slabHalf(var0, var1) ? 1 : 2;
      } else {
         return 1;
      }
   }

   public static boolean clickable(BlockState var0, Property<?> var1) {
      if (!CLICKED.contains(var1)) {
         return false;
      } else if (var1 != Properties.field_12537) {
         return true;
      } else {
         Block var2 = var0.method_26204();
         return var2 != Blocks.field_9973 && var2 != Blocks.field_10453
            ? var2 instanceof DoorBlock || var2 instanceof TrapdoorBlock || var2 instanceof FenceGateBlock
            : false;
      }
   }

   public static Direction lookFor(BlockState var0) {
      Block var1 = var0.method_26204();
      if (var1 instanceof CrafterBlock) {
         return ((Orientation)var0.method_11654(Properties.field_23333)).method_26426().method_10153();
      } else if (var1 instanceof ObserverBlock) {
         return (Direction)var0.method_11654(Properties.field_12525);
      } else {
         return !(var1 instanceof PistonBlock) && !(var1 instanceof DispenserBlock) && !(var1 instanceof BarrelBlock)
            ? null
            : ((Direction)var0.method_11654(Properties.field_12525)).method_10153();
      }
   }

   public static Direction flatLookFor(BlockState var0) {
      if (!var0.method_28498(Properties.field_12481)) {
         return null;
      } else {
         Block var1 = var0.method_26204();
         Direction var2 = (Direction)var0.method_11654(Properties.field_12481);
         if (var1 instanceof ChestBlock || var1 instanceof AbstractFurnaceBlock || var1 instanceof AbstractRedstoneGateBlock) {
            return var2.method_10153();
         } else {
            return !(var1 instanceof StairsBlock) && !(var1 instanceof DoorBlock) && !(var1 instanceof FenceGateBlock) ? null : var2;
         }
      }
   }

   private static boolean faceProperty(BlockState var0, Property<?> var1) {
      Block var2 = var0.method_26204();
      return (var2 instanceof MultifaceBlock || var2 instanceof VineBlock)
         && var1 instanceof BooleanProperty
         && ConnectingBlock.field_11329.containsValue(var1);
   }
}
