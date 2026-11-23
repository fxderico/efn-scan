package net.efn.client.easyplace.builder;

import fi.dy.masa.litematica.world.WorldSchematic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import net.efn.client.easyplace.util.BlockStateUtil;
import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.AbstractTorchBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.GlazedTerracottaBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.LoomBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.block.TntBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.RailShape;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Direction.Type;

@Environment(EnvType.CLIENT)
final class BuildOrder {
   static final int STAGE_NORMAL = 0;
   static final int STAGE_PISTON = 1;
   static final int STAGE_OBSERVER = 2;
   static final int STAGE_POWER = 3;
   private static final int MAX_TRACK = 512;

   private BuildOrder() {
   }

   static boolean powerOrTnt(BlockState var0) {
      Block var1 = var0.method_26204();
      return var1 == Blocks.field_10002 || var1 instanceof RedstoneTorchBlock || var1 instanceof DaylightDetectorBlock || var1 instanceof TntBlock;
   }

   static boolean late(WorldSchematic var0, BlockPos var1, BlockState var2, boolean var3) {
      return stage(var0, var1, var2, var3) > 0;
   }

   static int stage(WorldSchematic var0, BlockPos var1, BlockState var2, boolean var3) {
      int var4 = ownStage(var0, var1, var2, var3);
      if (var4 != 0) {
         return var4;
      } else {
         BlockPos var5 = hangsOn(var2, var1);
         return var5 == null ? 0 : ownStage(var0, var5, var0.method_8320(var5), var3);
      }
   }

   private static int ownStage(WorldSchematic var0, BlockPos var1, BlockState var2, boolean var3) {
      if (powerOrTnt(var2) || pushedIntoTnt(var0, var1, var2) != null) {
         return 3;
      } else if (!var3) {
         return 0;
      } else {
         Block var4 = var2.method_26204();
         if (var4 instanceof PistonBlock) {
            return 1;
         } else {
            return var4 instanceof ObserverBlock ? 2 : 0;
         }
      }
   }

   static boolean pistonWouldFire(ClientWorld var0, BlockPos var1) {
      return var0.method_49803(var1) || var0.method_49803(var1.method_10084());
   }

   static BlockPos pushedIntoTnt(WorldSchematic var0, BlockPos var1, BlockState var2) {
      if (!var2.method_26215() && !(var2.method_26204() instanceof TntBlock)) {
         for (Direction var6 : Direction.values()) {
            BlockState var8 = var0.method_8320(var1.method_10093(var6.method_10153()));
            BlockPos var7;
            if (var8.method_26204() instanceof PistonBlock
               && var8.method_11654(Properties.field_12525) == var6
               && var0.method_8320(var7 = var1.method_10093(var6)).method_26204() instanceof TntBlock) {
               return var7;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   static BlockPos railBefore(ClientWorld var0, WorldSchematic var1, BlockPos var2, BlockState var3, Predicate<BlockPos> var4) {
      List var5 = track(var1, var2);
      int var6 = var5.indexOf(var2);
      if (var6 <= 0) {
         return null;
      } else {
         BlockPos var7 = (BlockPos)var5.get(var6 - 1);
         return !var4.test(var7) && !(var0.method_8320(var7).method_26204() instanceof AbstractRailBlock) ? var7 : null;
      }
   }

   static boolean railShapeOff(BlockState var0, BlockState var1) {
      RailShape var2 = railShape(var1);
      RailShape var3 = railShape(var0);
      return var2 != null && var3 != null && var2 != var3;
   }

   private static List<BlockPos> track(WorldSchematic var0, BlockPos var1) {
      HashSet var2 = new HashSet();
      ArrayList var3 = new ArrayList<>(List.of(var1));
      BlockPos var4 = null;

      while (!var3.isEmpty() && var2.size() < 512) {
         BlockPos var5 = (BlockPos)var3.remove(var3.size() - 1);
         if (var2.add(var5)) {
            List var6 = links(var0, var5);
            if (var6.size() < 2 && (var4 == null || before(var5, var4))) {
               var4 = var5;
            }

            var3.addAll(var6);
         }
      }

      if (var4 == null) {
         for (BlockPos var13 : var2) {
            if (var4 == null || before(var13, var4)) {
               var4 = var13;
            }
         }
      }

      ArrayList var12 = new ArrayList();
      HashSet var14 = new HashSet();
      BlockPos var7 = var4;

      while (var7 != null && var14.add(var7)) {
         var12.add(var7);
         BlockPos var8 = null;

         for (BlockPos var10 : links(var0, var7)) {
            if (!var14.contains(var10) && (var8 == null || before(var10, var8))) {
               var8 = var10;
            }
         }

         var7 = var8;
      }

      return var12;
   }

   private static boolean before(BlockPos var0, BlockPos var1) {
      if (var0.method_10264() != var1.method_10264()) {
         return var0.method_10264() < var1.method_10264();
      } else {
         return var0.method_10263() != var1.method_10263() ? var0.method_10263() < var1.method_10263() : var0.method_10260() < var1.method_10260();
      }
   }

   private static List<BlockPos> links(WorldSchematic var0, BlockPos var1) {
      ArrayList var2 = new ArrayList(2);
      RailShape var3 = railShape(var0.method_8320(var1));
      if (var3 == null) {
         return var2;
      } else {
         for (BlockPos var5 : ends(var1, var3)) {
            BlockPos var6 = railAt(var0, var5);
            if (var6 != null) {
               RailShape var7 = railShape(var0.method_8320(var6));

               for (BlockPos var9 : ends(var6, var7)) {
                  if (var1.equals(railAt(var0, var9))) {
                     var2.add(var6);
                     break;
                  }
               }
            }
         }

         return var2;
      }
   }

   private static BlockPos railAt(WorldSchematic var0, BlockPos var1) {
      if (railShape(var0.method_8320(var1)) != null) {
         return var1;
      } else {
         return railShape(var0.method_8320(var1.method_10074())) != null ? var1.method_10074() : null;
      }
   }

   private static List<BlockPos> ends(BlockPos var0, RailShape var1) {
      return switch (var1) {
         case field_12665 -> List.of(var0.method_10095(), var0.method_10072());
         case field_12674 -> List.of(var0.method_10078(), var0.method_10067());
         case field_12667 -> List.of(var0.method_10078().method_10084(), var0.method_10067());
         case field_12666 -> List.of(var0.method_10067().method_10084(), var0.method_10078());
         case field_12670 -> List.of(var0.method_10095().method_10084(), var0.method_10072());
         case field_12668 -> List.of(var0.method_10072().method_10084(), var0.method_10095());
         case field_12664 -> List.of(var0.method_10072(), var0.method_10078());
         case field_12671 -> List.of(var0.method_10072(), var0.method_10067());
         case field_12672 -> List.of(var0.method_10095(), var0.method_10067());
         case field_12663 -> List.of(var0.method_10095(), var0.method_10078());
         default -> throw new MatchException(null, null);
      };
   }

   private static RailShape railShape(BlockState var0) {
      if (!(var0.method_26204() instanceof AbstractRailBlock var2)) {
         return null;
      } else {
         Property var3 = var2.method_9474();
         return var0.method_28498(var3) ? (RailShape)var0.method_11654(var3) : null;
      }
   }

   static boolean lookOnly(BlockState var0) {
      Block var1 = var0.method_26204();
      return var1 instanceof StairsBlock
         || var1 instanceof AbstractFurnaceBlock
         || var1 instanceof BarrelBlock
         || var1 instanceof PillarBlock
         || var1 instanceof CarvedPumpkinBlock
         || var1 instanceof GlazedTerracottaBlock
         || var1 instanceof FenceGateBlock
         || var1 instanceof LoomBlock
         || var1 instanceof StonecutterBlock
         || var1 instanceof LecternBlock
         || var1 instanceof BeehiveBlock
         || var1 instanceof AnvilBlock;
   }

   static List<BlockState> turnedVariants(BlockState var0) {
      ArrayList var1 = new ArrayList();

      for (Property var3 : List.of(Properties.field_12525, Properties.field_12481, Properties.field_12496, Properties.field_12529)) {
         if (var0.method_28498(var3)) {
            for (Object var5 : var3.method_11898()) {
               BlockState var6 = with(var0, var3, var5);
               if (!var6.equals(var0)) {
                  var1.add(var6);
               }
            }
         }
      }

      return var1;
   }

   private static BlockState with(BlockState var0, Property var1, Object var2) {
      return (BlockState)var0.method_11657(var1, (Comparable)var2);
   }

   static BlockPos hangsOn(BlockState var0, BlockPos var1) {
      Block var2 = var0.method_26204();
      if (var2 instanceof WallTorchBlock || var2 instanceof WallRedstoneTorchBlock || var2 instanceof WallSignBlock || var2 instanceof LadderBlock) {
         return var1.method_10093(((Direction)var0.method_11654(Properties.field_12481)).method_10153());
      } else if (var2 instanceof WallMountedBlock) {
         return switch ((BlockFace)var0.method_11654(Properties.field_12555)) {
            case field_12475 -> var1.method_10074();
            case field_12473 -> var1.method_10084();
            case field_12471 -> var1.method_10093(((Direction)var0.method_11654(Properties.field_12481)).method_10153());
            default -> throw new MatchException(null, null);
         };
      } else {
         return !(var2 instanceof RedstoneWireBlock)
               && !(var2 instanceof AbstractRedstoneGateBlock)
               && !(var2 instanceof AbstractRailBlock)
               && !(var2 instanceof CarpetBlock)
               && !(var2 instanceof AbstractPressurePlateBlock)
               && !(var2 instanceof AbstractTorchBlock)
            ? null
            : var1.method_10074();
      }
   }

   static List<BlockPos> supportCells(BlockState var0, BlockPos var1) {
      Direction var2 = StateRules.lookFor(var0);
      if (var2 == null) {
         var2 = StateRules.flatLookFor(var0);
      }

      if (var2 != null) {
         ArrayList var6 = new ArrayList<>(List.of(var1.method_10093(var2), var1.method_10074()));

         for (Direction var5 : Type.field_11062) {
            if (var5 != var2) {
               var6.add(var1.method_10093(var5));
            }
         }

         if (var2 != Direction.field_11036) {
            var6.add(var1.method_10084());
         }

         return var6;
      } else {
         Block var3 = var0.method_26204();
         if (var3 instanceof HopperBlock) {
            Direction var7 = (Direction)var0.method_11654(Properties.field_12545);
            return var7 == Direction.field_11033 ? List.of(var1.method_10074(), var1.method_10084()) : List.of(var1.method_10093(var7));
         } else if (hangsOn(var0, var1) != null) {
            return List.of();
         } else if (var0.method_28498(Properties.field_12496)) {
            Axis var4 = (Axis)var0.method_11654(Properties.field_12496);
            return List.of(
               var1.method_10093(Direction.method_10169(var4, AxisDirection.field_11060)),
               var1.method_10093(Direction.method_10169(var4, AxisDirection.field_11056))
            );
         } else if (var0.method_28498(Properties.field_12485)) {
            return var0.method_11654(Properties.field_12485) == SlabType.field_12679 ? List.of(var1.method_10084()) : List.of(var1.method_10074());
         } else if (var0.method_28498(Properties.field_12518)) {
            return var0.method_11654(Properties.field_12518) == BlockHalf.field_12619 ? List.of(var1.method_10084()) : List.of(var1.method_10074());
         } else {
            return List.of(var1.method_10074(), var1.method_10095(), var1.method_10072(), var1.method_10078(), var1.method_10067(), var1.method_10084());
         }
      }
   }

   static boolean waits(ClientWorld var0, WorldSchematic var1, BlockPos var2, BlockState var3, Predicate<BlockPos> var4, boolean var5) {
      Block var6 = var3.method_26204();
      BlockPos var7 = pushedIntoTnt(var1, var2, var3);
      if (var7 != null && !(var0.method_8320(var7).method_26204() instanceof TntBlock)) {
         return true;
      } else if (var6 instanceof PistonBlock) {
         return pistonWouldFire(var0, var2);
      } else if (var6 instanceof AbstractRailBlock && railBefore(var0, var1, var2, var3, var4) != null) {
         return true;
      } else if (var6 instanceof ObserverBlock) {
         BlockPos var8 = var2.method_10093((Direction)var3.method_11654(Properties.field_12525));
         BlockState var9 = var1.method_8320(var8);
         if (BlockStateUtil.requiredItem(var9) == null || stage(var1, var8, var9, var5) > 2 || var4.test(var8)) {
            return false;
         } else {
            return var9.method_26204() instanceof ObserverBlock && var8.method_10093((Direction)var9.method_11654(Properties.field_12525)).equals(var2)
               ? false
               : !StateRules.placed(var0.method_8320(var8), var9);
         }
      } else if (var6 instanceof FallingBlock) {
         return FallingBlock.method_10128(var0.method_8320(var2.method_10074()));
      } else if (!var3.method_28498(Properties.field_12508) && var3.method_26227().method_15771() && var3.method_26227().method_15767(FluidTags.field_15517)) {
         return !var0.method_8316(var2).method_15771() || !var0.method_8316(var2).method_15767(FluidTags.field_15517);
      } else {
         return var6 instanceof TntBlock ? var0.method_49803(var2) : false;
      }
   }
}
