package net.efn.client.easyplace.builder;

import fi.dy.masa.litematica.world.WorldSchematic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.efn.client.easyplace.schematic.SchematicManager;
import net.efn.client.easyplace.schematic.TargetStatus;
import net.efn.client.easyplace.util.BlockStateUtil;
import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.TntBlock;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Type;

@Environment(EnvType.CLIENT)
final class BuildScan {
   static final int NONE = 2147483647;
   final List<BuildScan.Cell> missing = new ArrayList<>();
   final List<BuildScan.Cell> adjust = new ArrayList<>();
   final List<BuildScan.Liquid> liquids = new ArrayList<>();
   final Set<Long> waiting = new HashSet<>();
   final List<BlockPos> poweredTnt = new ArrayList<>();
   final List<BlockPos> tntForPlayer = new ArrayList<>();
   final List<BlockPos> afterTntForPlayer = new ArrayList<>();
   private final List<BuildScan.Cell> tntCells = new ArrayList<>();
   private boolean powerLeft;
   private final Set<Long> waterBelow = new HashSet<>();
   private final Map<Long, Integer> staged = new HashMap<>();
   private final List<BuildScan.Cell> lateCells = new ArrayList<>();
   private boolean machineLast;
   int late;
   int leversOff;
   final List<BlockPos> levers = new ArrayList<>();
   int railsOff;
   final List<BlockPos> crafters = new ArrayList<>();
   final Map<BlockPos, boolean[]> crafterSlots = new HashMap<>();
   int layer = 2147483647;
   int wrong;
   int noItem;
   int unloaded;
   boolean wholeSchematic = true;
   private boolean placeTnt;

   BuildScan() {
   }

   static BuildScan run(
      ClientWorld clientWorld,
      WorldSchematic worldSchematic,
      List<SchematicManager.ActiveBox> list,
      Predicate<BlockPos> predicate,
      Predicate<BlockPos> predicate2,
      Predicate<BlockPos> predicate3,
      boolean bl,
      Set<Long> set,
      Set<Long> set2,
      boolean bl2,
      boolean bl3,
      boolean bl4
   ) {
      BuildScan var12 = new BuildScan();
      var12.placeTnt = bl3;
      var12.machineLast = bl4;
      if (list.isEmpty()) {
         return var12;
      } else {
         int var13 = 2147483647;
         int var14 = -2147483648;

         for (SchematicManager.ActiveBox var16 : list) {
            var13 = Math.min(var13, var16.minY());
            var14 = Math.max(var14, var16.maxY());
         }

         Mutable var21 = new Mutable();
         int var22 = var13;

         do {
            for (var12.wholeSchematic = true; var22 <= var14; var22++) {
               if (var12.layer != 2147483647 && var22 > var12.layer + 1) {
                  var12.wholeSchematic = false;
                  break;
               }

               for (SchematicManager.ActiveBox var18 : list) {
                  if (var22 >= var18.minY() && var22 <= var18.maxY()) {
                     for (int var19 = var18.minX(); var19 <= var18.maxX(); var19++) {
                        for (int var20 = var18.minZ(); var20 <= var18.maxZ(); var20++) {
                           var21.set(var19, var22, var20);
                           var12.visit(clientWorld, worldSchematic, var21, predicate, predicate2, predicate3, bl, set, set2, bl2);
                        }
                     }
                  }
               }
            }

            if (var12.wholeSchematic) {
               break;
            }

            var12.keepApproachesOpen(clientWorld, worldSchematic, predicate2, predicate);
         } while (var12.layer == 2147483647);

         var12.releaseStages(predicate2);
         var12.releaseTnt(clientWorld, worldSchematic, predicate, predicate2, set);
         var12.keepApproachesOpen(clientWorld, worldSchematic, predicate2, predicate);
         return var12;
      }
   }

   private void releaseStages(Predicate<BlockPos> predicate) {
      int var2 = 2147483647;

      for (Entry var4 : this.staged.entrySet()) {
         if (!predicate.test(BlockPos.fromLong((Long)var4.getKey()))) {
            var2 = Math.min(var2, (Integer)var4.getValue());
         }
      }

      for (Entry var6 : this.staged.entrySet()) {
         if ((Integer)var6.getValue() > var2) {
            this.waiting.add((Long)var6.getKey());
         }
      }
   }

   private void keepApproachesOpen(ClientWorld clientWorld, WorldSchematic worldSchematic, Predicate<BlockPos> predicate, Predicate<BlockPos> predicate2) {
      HashSet var5 = new HashSet();

      for (BuildScan.Cell var7 : this.missing) {
         var5.add(var7.pos().asLong());
      }

      HashSet var12 = new HashSet();

      for (BuildScan.Cell var8 : this.lateCells) {
         var5.add(var8.pos().asLong());
         var12.add(var8.pos().asLong());
      }

      var12.addAll(this.staged.keySet());
      ArrayList var14 = new ArrayList<>(this.missing);
      var14.addAll(this.lateCells);

      for (BuildScan.Cell var9 : var14) {
         if (!this.waiting.contains(var9.pos().asLong()) || var12.contains(var9.pos().asLong())) {
            if (StateRules.lookFor(var9.expected()) == Direction.UP) {
               BlockPos var11 = lowStand(clientWorld, worldSchematic, var9.pos(), var5);
               if (var11 != null) {
                  if (var5.contains(var11.asLong())) {
                     this.waiting.add(var11.asLong());
                  }

                  if (var5.contains(var11.up().asLong())) {
                     this.waiting.add(var11.up().asLong());
                  }
               } else {
                  this.waiting.add(var9.pos().asLong());
               }
            } else {
               BlockPos var10;
               if (!predicate.test(var9.pos()) && (var10 = approach(var9.expected(), var9.pos())) != null && var5.contains(var10.asLong())) {
                  BlockPos var18 = approach(worldSchematic.getBlockState(var10), var10);
                  if (!var9.pos().equals(var18)) {
                     this.waiting.add(var10.asLong());
                  }
               }
            }
         }
      }

      this.layer = 2147483647;

      for (BuildScan.Cell var17 : this.missing) {
         if (!this.waiting.contains(var17.pos().asLong()) && !predicate2.test(var17.pos())) {
            this.layer = Math.min(this.layer, var17.pos().getY());
         }
      }
   }

   private static BlockPos lowStand(ClientWorld clientWorld, WorldSchematic worldSchematic, BlockPos blockPos, Set<Long> set) {
      BlockPos var4 = null;
      int var5 = -1;

      for (Direction var7 : Type.HORIZONTAL) {
         BlockPos var9 = blockPos.down().offset(var7);
         BlockPos var10 = var9.down();
         boolean var11 = !clientWorld.getBlockState(var10).getCollisionShape(clientWorld, var10).isEmpty();
         BlockState var12 = worldSchematic.getBlockState(var10);
         int var8;
         if ((var11 || !var12.isAir() && var12.getFluidState().isEmpty())
            && freeCell(clientWorld, worldSchematic, var9, set)
            && freeCell(clientWorld, worldSchematic, var9.up(), set)
            && StateRules.lookFor(worldSchematic.getBlockState(var9.up())) != Direction.UP
            && (var8 = (var11 ? 2 : 0) + (set.contains(var9.asLong()) ? 0 : 1)) > var5) {
            var5 = var8;
            var4 = var9;
         }
      }

      return var4;
   }

   private static boolean freeCell(ClientWorld clientWorld, WorldSchematic worldSchematic, BlockPos blockPos, Set<Long> set) {
      BlockState var4 = clientWorld.getBlockState(blockPos);
      if (var4.isReplaceable() && var4.getFluidState().isEmpty()) {
         BlockState var5 = worldSchematic.getBlockState(blockPos);
         return var5.getBlock() instanceof FluidBlock ? false : var5.isAir() || set.contains(blockPos.asLong());
      } else {
         return false;
      }
   }

   private static BlockPos approach(BlockState blockState, BlockPos blockPos) {
      Direction var3 = StateRules.lookFor(blockState);
      if (var3 == null) {
         var3 = StateRules.flatLookFor(blockState);
      }

      Direction var2;
      if (var3 == null && blockState.getBlock() instanceof HopperBlock && (var2 = blockState.get(Properties.HOPPER_FACING)) != Direction.DOWN) {
         var3 = var2;
      }

      return var3 == null ? null : blockPos.offset(var3.getOpposite());
   }

   static Map<Item, Integer> remainingItems(ClientWorld clientWorld, WorldSchematic worldSchematic, List<SchematicManager.ActiveBox> list) {
      HashMap var3 = new HashMap();
      Mutable var4 = new Mutable();

      for (SchematicManager.ActiveBox var6 : list) {
         for (int var7 = var6.minY(); var7 <= var6.maxY(); var7++) {
            for (int var8 = var6.minX(); var8 <= var6.maxX(); var8++) {
               for (int var9 = var6.minZ(); var9 <= var6.maxZ(); var9++) {
                  var4.set(var8, var7, var9);
                  BlockState var11 = worldSchematic.getBlockState(var4);
                  if (!var11.isAir() && clientWorld.isChunkLoaded(var4)) {
                     BlockState var12 = clientWorld.getBlockState(var4);
                     CellFiller.Kind var13 = needsLiquid(var11, var12);
                     if (var13 != null) {
                        var3.merge(var13.item, 1, Integer::sum);
                     }

                     Item var10;
                     if (BlockStateUtil.isPrimaryPlacementState(var11)
                        && (var10 = BlockStateUtil.requiredItem(var11)) != null
                        && BlockStateUtil.classify(var11, var12) == TargetStatus.MISSING) {
                        var3.merge(var10, StateRules.itemsNeeded(var12, var11), Integer::sum);
                     }
                  }
               }
            }
         }
      }

      return var3;
   }

   private void releaseTnt(ClientWorld clientWorld, WorldSchematic worldSchematic, Predicate<BlockPos> predicate, Predicate<BlockPos> predicate2, Set<Long> set) {
      if (!this.powerLeft && set.isEmpty()) {
         for (BuildScan.Cell var7 : this.tntCells) {
            if (BuildOrder.waits(clientWorld, worldSchematic, var7.pos(), var7.expected(), predicate2, this.machineLast)) {
               this.poweredTnt.add(var7.pos());
            } else {
               this.waiting.remove(var7.pos().asLong());
               if (!predicate.test(var7.pos()) && (this.layer == 2147483647 || var7.pos().getY() < this.layer)) {
                  this.layer = var7.pos().getY();
               }
            }
         }
      }
   }

   private void visit(
      ClientWorld clientWorld,
      WorldSchematic worldSchematic,
      Mutable mutable,
      Predicate<BlockPos> predicate,
      Predicate<BlockPos> predicate2,
      Predicate<BlockPos> predicate3,
      boolean bl,
      Set<Long> set,
      Set<Long> set2,
      boolean bl2
   ) {
      BlockState var11 = worldSchematic.getBlockState(mutable);
      if (!var11.isAir()) {
         if (!clientWorld.isChunkLoaded(mutable)) {
            this.unloaded++;
         } else {
            BlockState var12 = clientWorld.getBlockState(mutable);
            CellFiller.Kind var13 = needsLiquid(var11, var12);
            if (var13 != null) {
               BlockPos var14 = mutable.toImmutable();
               boolean var15 = var13 == CellFiller.Kind.LAVA_SOURCE && capped(worldSchematic, var14);
               this.liquids.add(new BuildScan.Liquid(var14, var13, var15));
               if ((var13 != CellFiller.Kind.LAVA_SOURCE || var15) && predicate3.test(var14)) {
                  this.waterBelow.add(var14.asLong());
               }
            }

            if (var11.getFluidState().isEmpty() || var11.getBlock().asItem() != Items.AIR) {
               if (BlockStateUtil.isPrimaryPlacementState(var11)) {
                  if (BlockStateUtil.requiredItem(var11) == null) {
                     this.noItem++;
                  } else {
                     TargetStatus var20 = BlockStateUtil.classify(var11, var12);
                     switch (var20) {
                        case MISSING:
                           BlockPos var23 = mutable.toImmutable();
                           boolean var17 = var11.getBlock() instanceof TntBlock;
                           boolean var22 = !var17 && BuildOrder.pushedIntoTnt(worldSchematic, var23, var11) != null;
                           if (!this.placeTnt && (var17 || var22)) {
                              (var17 ? this.tntForPlayer : this.afterTntForPlayer).add(var23);
                              this.waiting.add(var23.asLong());
                              return;
                           }

                           int var19 = BuildOrder.stage(worldSchematic, var23, var11, this.machineLast);
                           if (bl && !var17 && !var22 && var19 > 0) {
                              this.powerLeft = true;
                              this.staged.put(var23.asLong(), var19);
                           }

                           if (bl && var17) {
                              this.missing.add(new BuildScan.Cell(var23, var11, var12));
                              this.tntCells.add(new BuildScan.Cell(var23, var11, var12));
                              this.waiting.add(var23.asLong());
                              return;
                           }

                           if (!bl && var19 > 0) {
                              this.late++;
                              this.lateCells.add(new BuildScan.Cell(var23, var11, var12));
                              this.waiting.add(var23.asLong());
                              return;
                           }

                           this.missing.add(new BuildScan.Cell(var23, var11, var12));
                           if (this.waterBelow.contains(var23.down().asLong())
                              || BuildOrder.waits(clientWorld, worldSchematic, var23, var11, predicate2, this.machineLast)) {
                              this.waiting.add(var23.asLong());
                              return;
                           }

                           if (this.layer == 2147483647 && !predicate.test(var23)) {
                              this.layer = var23.getY();
                           }
                           break;
                        case ADJUST:
                           this.adjust.add(new BuildScan.Cell(mutable.toImmutable(), var11, var12));
                           break;
                        case CORRECT:
                           if (var11.getBlock() instanceof LeverBlock && var11.get(Properties.POWERED) && !var12.get(Properties.POWERED)) {
                              this.leversOff++;
                              this.levers.add(mutable.toImmutable());
                           }

                           if (BuildOrder.railShapeOff(var12, var11)) {
                              this.railsOff++;
                           }

                           boolean[] var21;
                           if (bl2
                              && var11.getBlock() instanceof CrafterBlock
                              && !set2.contains(mutable.asLong())
                              && (var21 = disabledSlots(worldSchematic, mutable)) != null) {
                              BlockPos var16 = mutable.toImmutable();
                              this.crafters.add(var16);
                              this.crafterSlots.put(var16, var21);
                           }
                           break;
                        case WRONG:
                           if (!set.contains(mutable.asLong())) {
                              this.wrong++;
                           }
                     }
                  }
               }
            }
         }
      }
   }

   static CellFiller.Kind needsLiquid(BlockState blockState, BlockState blockState2) {
      FluidState var2 = blockState.getFluidState();
      if (!var2.isEmpty() && var2.isStill()) {
         FluidState var3 = blockState2.getFluidState();
         if (var2.isIn(FluidTags.LAVA)) {
            if (var3.isStill() && var3.isIn(FluidTags.LAVA)) {
               return null;
            } else {
               return !blockState2.isAir() && (!blockState2.isReplaceable() || !var3.isEmpty() && !var3.isIn(FluidTags.LAVA))
                  ? null
                  : CellFiller.Kind.LAVA_SOURCE;
            }
         } else if (!var2.isIn(FluidTags.WATER)) {
            return null;
         } else if (blockState.contains(Properties.WATERLOGGED)) {
            return StateRules.placed(blockState2, blockState) && !blockState2.get(Properties.WATERLOGGED) ? CellFiller.Kind.WATERLOG : null;
         } else if (var3.isStill() && var3.isIn(FluidTags.WATER)) {
            return null;
         } else {
            return !blockState2.isAir() && !blockState2.isReplaceable() ? null : CellFiller.Kind.WATER_SOURCE;
         }
      } else {
         return null;
      }
   }

   private static boolean capped(WorldSchematic worldSchematic, BlockPos blockPos) {
      Mutable var2 = blockPos.mutableCopy();

      for (int var3 = 0; var3 < 64; var3++) {
         var2.move(Direction.UP);
         BlockState var4 = worldSchematic.getBlockState(var2);
         if (!var4.getFluidState().isIn(FluidTags.LAVA)) {
            return !var4.isAir() && var4.getFluidState().isEmpty();
         }
      }

      return false;
   }

   private static boolean[] disabledSlots(WorldSchematic worldSchematic, BlockPos blockPos) {
      if (!(worldSchematic.getBlockEntity(blockPos) instanceof CrafterBlockEntity var3)) {
         return null;
      } else {
         boolean[] var4 = new boolean[9];
         boolean var5 = false;

         for (int var6 = 0; var6 < 9; var6++) {
            var4[var6] = var3.isSlotDisabled(var6);
            var5 |= var4[var6];
         }

         return var5 ? var4 : null;
      }
   }

   Map<Item, Integer> needs(int i) {
      HashMap var2 = new HashMap();

      for (BuildScan.Cell var4 : this.missing) {
         Item var5;
         if (var4.pos().getY() <= i && (var5 = BlockStateUtil.requiredItem(var4.expected())) != null) {
            var2.merge(var5, StateRules.itemsNeeded(var4.actual(), var4.expected()), Integer::sum);
         }
      }

      return var2;
   }

   @Environment(EnvType.CLIENT)
   record Cell(BlockPos pos, BlockState expected, BlockState actual) {
   }

   @Environment(EnvType.CLIENT)
   record Liquid(BlockPos pos, CellFiller.Kind kind, boolean capped) {
   }
}
