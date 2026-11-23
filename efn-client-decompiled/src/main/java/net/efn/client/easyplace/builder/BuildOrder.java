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

   static boolean powerOrTnt(BlockState blockState) {
      Block var1 = blockState.getBlock();
      return var1 == Blocks.REDSTONE_BLOCK || var1 instanceof RedstoneTorchBlock || var1 instanceof DaylightDetectorBlock || var1 instanceof TntBlock;
   }

   static boolean late(WorldSchematic worldSchematic, BlockPos blockPos, BlockState blockState, boolean bl) {
      return stage(worldSchematic, blockPos, blockState, bl) > 0;
   }

   static int stage(WorldSchematic worldSchematic, BlockPos blockPos, BlockState blockState, boolean bl) {
      int var4 = ownStage(worldSchematic, blockPos, blockState, bl);
      if (var4 != 0) {
         return var4;
      } else {
         BlockPos var5 = hangsOn(blockState, blockPos);
         return var5 == null ? 0 : ownStage(worldSchematic, var5, worldSchematic.getBlockState(var5), bl);
      }
   }

   private static int ownStage(WorldSchematic worldSchematic, BlockPos blockPos, BlockState blockState, boolean bl) {
      if (powerOrTnt(blockState) || pushedIntoTnt(worldSchematic, blockPos, blockState) != null) {
         return 3;
      } else if (!bl) {
         return 0;
      } else {
         Block var4 = blockState.getBlock();
         if (var4 instanceof PistonBlock) {
            return 1;
         } else {
            return var4 instanceof ObserverBlock ? 2 : 0;
         }
      }
   }

   static boolean pistonWouldFire(ClientWorld clientWorld, BlockPos blockPos) {
      return clientWorld.isReceivingRedstonePower(blockPos) || clientWorld.isReceivingRedstonePower(blockPos.up());
   }

   static BlockPos pushedIntoTnt(WorldSchematic worldSchematic, BlockPos blockPos, BlockState blockState) {
      if (!blockState.isAir() && !(blockState.getBlock() instanceof TntBlock)) {
         for (Direction var6 : Direction.values()) {
            BlockState var8 = worldSchematic.getBlockState(blockPos.offset(var6.getOpposite()));
            BlockPos var7;
            if (var8.getBlock() instanceof PistonBlock
               && var8.get(Properties.FACING) == var6
               && worldSchematic.getBlockState(var7 = blockPos.offset(var6)).getBlock() instanceof TntBlock) {
               return var7;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   static BlockPos railBefore(ClientWorld clientWorld, WorldSchematic worldSchematic, BlockPos blockPos, BlockState blockState, Predicate<BlockPos> predicate) {
      List var5 = track(worldSchematic, blockPos);
      int var6 = var5.indexOf(blockPos);
      if (var6 <= 0) {
         return null;
      } else {
         BlockPos var7 = (BlockPos)var5.get(var6 - 1);
         return !predicate.test(var7) && !(clientWorld.getBlockState(var7).getBlock() instanceof AbstractRailBlock) ? var7 : null;
      }
   }

   static boolean railShapeOff(BlockState blockState, BlockState blockState2) {
      RailShape var2 = railShape(blockState2);
      RailShape var3 = railShape(blockState);
      return var2 != null && var3 != null && var2 != var3;
   }

   private static List<BlockPos> track(WorldSchematic worldSchematic, BlockPos blockPos) {
      HashSet var2 = new HashSet();
      ArrayList var3 = new ArrayList<>(List.of(blockPos));
      BlockPos var4 = null;

      while (!var3.isEmpty() && var2.size() < 512) {
         BlockPos var5 = (BlockPos)var3.remove(var3.size() - 1);
         if (var2.add(var5)) {
            List var6 = links(worldSchematic, var5);
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

         for (BlockPos var10 : links(worldSchematic, var7)) {
            if (!var14.contains(var10) && (var8 == null || before(var10, var8))) {
               var8 = var10;
            }
         }

         var7 = var8;
      }

      return var12;
   }

   private static boolean before(BlockPos blockPos, BlockPos blockPos2) {
      if (blockPos.getY() != blockPos2.getY()) {
         return blockPos.getY() < blockPos2.getY();
      } else {
         return blockPos.getX() != blockPos2.getX() ? blockPos.getX() < blockPos2.getX() : blockPos.getZ() < blockPos2.getZ();
      }
   }

   private static List<BlockPos> links(WorldSchematic worldSchematic, BlockPos blockPos) {
      ArrayList var2 = new ArrayList(2);
      RailShape var3 = railShape(worldSchematic.getBlockState(blockPos));
      if (var3 == null) {
         return var2;
      } else {
         for (BlockPos var5 : ends(blockPos, var3)) {
            BlockPos var6 = railAt(worldSchematic, var5);
            if (var6 != null) {
               RailShape var7 = railShape(worldSchematic.getBlockState(var6));

               for (BlockPos var9 : ends(var6, var7)) {
                  if (blockPos.equals(railAt(worldSchematic, var9))) {
                     var2.add(var6);
                     break;
                  }
               }
            }
         }

         return var2;
      }
   }

   private static BlockPos railAt(WorldSchematic worldSchematic, BlockPos blockPos) {
      if (railShape(worldSchematic.getBlockState(blockPos)) != null) {
         return blockPos;
      } else {
         return railShape(worldSchematic.getBlockState(blockPos.down())) != null ? blockPos.down() : null;
      }
   }

   private static List<BlockPos> ends(BlockPos blockPos, RailShape railShape) {
      return switch (railShape) {
         case NORTH_SOUTH -> List.of(blockPos.north(), blockPos.south());
         case EAST_WEST -> List.of(blockPos.east(), blockPos.west());
         case ASCENDING_EAST -> List.of(blockPos.east().up(), blockPos.west());
         case ASCENDING_WEST -> List.of(blockPos.west().up(), blockPos.east());
         case ASCENDING_NORTH -> List.of(blockPos.north().up(), blockPos.south());
         case ASCENDING_SOUTH -> List.of(blockPos.south().up(), blockPos.north());
         case SOUTH_EAST -> List.of(blockPos.south(), blockPos.east());
         case SOUTH_WEST -> List.of(blockPos.south(), blockPos.west());
         case NORTH_WEST -> List.of(blockPos.north(), blockPos.west());
         case NORTH_EAST -> List.of(blockPos.north(), blockPos.east());
      };
   }

   private static RailShape railShape(BlockState blockState) {
      if (!(blockState.getBlock() instanceof AbstractRailBlock var2)) {
         return null;
      } else {
         Property var3 = var2.getShapeProperty();
         return blockState.contains(var3) ? blockState.get(var3) : null;
      }
   }

   static boolean lookOnly(BlockState blockState) {
      Block var1 = blockState.getBlock();
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

   static List<BlockState> turnedVariants(BlockState blockState) {
      ArrayList var1 = new ArrayList();

      for (Property var3 : List.of(Properties.FACING, Properties.HORIZONTAL_FACING, Properties.AXIS, Properties.HORIZONTAL_AXIS)) {
         if (blockState.contains(var3)) {
            for (Object var5 : var3.getValues()) {
               BlockState var6 = with(blockState, var3, var5);
               if (!var6.equals(blockState)) {
                  var1.add(var6);
               }
            }
         }
      }

      return var1;
   }

   private static BlockState with(BlockState blockState, Property property, Object object) {
      return blockState.with(property, (Comparable)object);
   }

   static BlockPos hangsOn(BlockState blockState, BlockPos blockPos) {
      Block var2 = blockState.getBlock();
      if (var2 instanceof WallTorchBlock || var2 instanceof WallRedstoneTorchBlock || var2 instanceof WallSignBlock || var2 instanceof LadderBlock) {
         return blockPos.offset(blockState.get(Properties.HORIZONTAL_FACING).getOpposite());
      } else if (var2 instanceof WallMountedBlock) {
         return switch ((BlockFace)blockState.get(Properties.BLOCK_FACE)) {
            case FLOOR -> blockPos.down();
            case CEILING -> blockPos.up();
            case WALL -> blockPos.offset(blockState.get(Properties.HORIZONTAL_FACING).getOpposite());
         };
      } else {
         return !(var2 instanceof RedstoneWireBlock)
               && !(var2 instanceof AbstractRedstoneGateBlock)
               && !(var2 instanceof AbstractRailBlock)
               && !(var2 instanceof CarpetBlock)
               && !(var2 instanceof AbstractPressurePlateBlock)
               && !(var2 instanceof AbstractTorchBlock)
            ? null
            : blockPos.down();
      }
   }

   static List<BlockPos> supportCells(BlockState blockState, BlockPos blockPos) {
      Direction var2 = StateRules.lookFor(blockState);
      if (var2 == null) {
         var2 = StateRules.flatLookFor(blockState);
      }

      if (var2 != null) {
         ArrayList var6 = new ArrayList<>(List.of(blockPos.offset(var2), blockPos.down()));

         for (Direction var5 : Type.HORIZONTAL) {
            if (var5 != var2) {
               var6.add(blockPos.offset(var5));
            }
         }

         if (var2 != Direction.UP) {
            var6.add(blockPos.up());
         }

         return var6;
      } else {
         Block var3 = blockState.getBlock();
         if (var3 instanceof HopperBlock) {
            Direction var7 = blockState.get(Properties.HOPPER_FACING);
            return var7 == Direction.DOWN ? List.of(blockPos.down(), blockPos.up()) : List.of(blockPos.offset(var7));
         } else if (hangsOn(blockState, blockPos) != null) {
            return List.of();
         } else if (blockState.contains(Properties.AXIS)) {
            Axis var4 = blockState.get(Properties.AXIS);
            return List.of(blockPos.offset(Direction.from(var4, AxisDirection.NEGATIVE)), blockPos.offset(Direction.from(var4, AxisDirection.POSITIVE)));
         } else if (blockState.contains(Properties.SLAB_TYPE)) {
            return blockState.get(Properties.SLAB_TYPE) == SlabType.TOP ? List.of(blockPos.up()) : List.of(blockPos.down());
         } else if (blockState.contains(Properties.BLOCK_HALF)) {
            return blockState.get(Properties.BLOCK_HALF) == BlockHalf.TOP ? List.of(blockPos.up()) : List.of(blockPos.down());
         } else {
            return List.of(blockPos.down(), blockPos.north(), blockPos.south(), blockPos.east(), blockPos.west(), blockPos.up());
         }
      }
   }

   static boolean waits(
      ClientWorld clientWorld, WorldSchematic worldSchematic, BlockPos blockPos, BlockState blockState, Predicate<BlockPos> predicate, boolean bl
   ) {
      Block var6 = blockState.getBlock();
      BlockPos var7 = pushedIntoTnt(worldSchematic, blockPos, blockState);
      if (var7 != null && !(clientWorld.getBlockState(var7).getBlock() instanceof TntBlock)) {
         return true;
      } else if (var6 instanceof PistonBlock) {
         return pistonWouldFire(clientWorld, blockPos);
      } else if (var6 instanceof AbstractRailBlock && railBefore(clientWorld, worldSchematic, blockPos, blockState, predicate) != null) {
         return true;
      } else if (var6 instanceof ObserverBlock) {
         BlockPos var8 = blockPos.offset(blockState.get(Properties.FACING));
         BlockState var9 = worldSchematic.getBlockState(var8);
         if (BlockStateUtil.requiredItem(var9) == null || stage(worldSchematic, var8, var9, bl) > 2 || predicate.test(var8)) {
            return false;
         } else {
            return var9.getBlock() instanceof ObserverBlock && var8.offset(var9.get(Properties.FACING)).equals(blockPos)
               ? false
               : !StateRules.placed(clientWorld.getBlockState(var8), var9);
         }
      } else if (var6 instanceof FallingBlock) {
         return FallingBlock.canFallThrough(clientWorld.getBlockState(blockPos.down()));
      } else if (!blockState.contains(Properties.WATERLOGGED) && blockState.getFluidState().isStill() && blockState.getFluidState().isIn(FluidTags.WATER)) {
         return !clientWorld.getFluidState(blockPos).isStill() || !clientWorld.getFluidState(blockPos).isIn(FluidTags.WATER);
      } else {
         return var6 instanceof TntBlock ? clientWorld.isReceivingRedstonePower(blockPos) : false;
      }
   }
}
