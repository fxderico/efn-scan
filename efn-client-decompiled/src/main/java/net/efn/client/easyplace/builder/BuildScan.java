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
      ClientWorld var0,
      WorldSchematic var1,
      List<SchematicManager.ActiveBox> var2,
      Predicate<BlockPos> var3,
      Predicate<BlockPos> var4,
      Predicate<BlockPos> var5,
      boolean var6,
      Set<Long> var7,
      Set<Long> var8,
      boolean var9,
      boolean var10,
      boolean var11
   ) {
      BuildScan var12 = new BuildScan();
      var12.placeTnt = var10;
      var12.machineLast = var11;
      if (var2.isEmpty()) {
         return var12;
      } else {
         int var13 = 2147483647;
         int var14 = -2147483648;

         for (SchematicManager.ActiveBox var16 : var2) {
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

               for (SchematicManager.ActiveBox var18 : var2) {
                  if (var22 >= var18.minY() && var22 <= var18.maxY()) {
                     for (int var19 = var18.minX(); var19 <= var18.maxX(); var19++) {
                        for (int var20 = var18.minZ(); var20 <= var18.maxZ(); var20++) {
                           var21.method_10103(var19, var22, var20);
                           var12.visit(var0, var1, var21, var3, var4, var5, var6, var7, var8, var9);
                        }
                     }
                  }
               }
            }

            if (var12.wholeSchematic) {
               break;
            }

            var12.keepApproachesOpen(var0, var1, var4, var3);
         } while (var12.layer == 2147483647);

         var12.releaseStages(var4);
         var12.releaseTnt(var0, var1, var3, var4, var7);
         var12.keepApproachesOpen(var0, var1, var4, var3);
         return var12;
      }
   }

   private void releaseStages(Predicate<BlockPos> var1) {
      int var2 = 2147483647;

      for (Entry var4 : this.staged.entrySet()) {
         if (!var1.test(BlockPos.method_10092((Long)var4.getKey()))) {
            var2 = Math.min(var2, (Integer)var4.getValue());
         }
      }

      for (Entry var6 : this.staged.entrySet()) {
         if ((Integer)var6.getValue() > var2) {
            this.waiting.add((Long)var6.getKey());
         }
      }
   }

   private void keepApproachesOpen(ClientWorld var1, WorldSchematic var2, Predicate<BlockPos> var3, Predicate<BlockPos> var4) {
      HashSet var5 = new HashSet();

      for (BuildScan.Cell var7 : this.missing) {
         var5.add(var7.pos().method_10063());
      }

      HashSet var12 = new HashSet();

      for (BuildScan.Cell var8 : this.lateCells) {
         var5.add(var8.pos().method_10063());
         var12.add(var8.pos().method_10063());
      }

      var12.addAll(this.staged.keySet());
      ArrayList var14 = new ArrayList<>(this.missing);
      var14.addAll(this.lateCells);

      for (BuildScan.Cell var9 : var14) {
         if (!this.waiting.contains(var9.pos().method_10063()) || var12.contains(var9.pos().method_10063())) {
            if (StateRules.lookFor(var9.expected()) == Direction.field_11036) {
               BlockPos var11 = lowStand(var1, var2, var9.pos(), var5);
               if (var11 != null) {
                  if (var5.contains(var11.method_10063())) {
                     this.waiting.add(var11.method_10063());
                  }

                  if (var5.contains(var11.method_10084().method_10063())) {
                     this.waiting.add(var11.method_10084().method_10063());
                  }
               } else {
                  this.waiting.add(var9.pos().method_10063());
               }
            } else {
               BlockPos var10;
               if (!var3.test(var9.pos()) && (var10 = approach(var9.expected(), var9.pos())) != null && var5.contains(var10.method_10063())) {
                  BlockPos var18 = approach(var2.method_8320(var10), var10);
                  if (!var9.pos().equals(var18)) {
                     this.waiting.add(var10.method_10063());
                  }
               }
            }
         }
      }

      this.layer = 2147483647;

      for (BuildScan.Cell var17 : this.missing) {
         if (!this.waiting.contains(var17.pos().method_10063()) && !var4.test(var17.pos())) {
            this.layer = Math.min(this.layer, var17.pos().method_10264());
         }
      }
   }

   private static BlockPos lowStand(ClientWorld var0, WorldSchematic var1, BlockPos var2, Set<Long> var3) {
      BlockPos var4 = null;
      int var5 = -1;

      for (Direction var7 : Type.field_11062) {
         BlockPos var9 = var2.method_10074().method_10093(var7);
         BlockPos var10 = var9.method_10074();
         boolean var11 = !var0.method_8320(var10).method_26220(var0, var10).method_1110();
         BlockState var12 = var1.method_8320(var10);
         int var8;
         if ((var11 || !var12.method_26215() && var12.method_26227().method_15769())
            && freeCell(var0, var1, var9, var3)
            && freeCell(var0, var1, var9.method_10084(), var3)
            && StateRules.lookFor(var1.method_8320(var9.method_10084())) != Direction.field_11036
            && (var8 = (var11 ? 2 : 0) + (var3.contains(var9.method_10063()) ? 0 : 1)) > var5) {
            var5 = var8;
            var4 = var9;
         }
      }

      return var4;
   }

   private static boolean freeCell(ClientWorld var0, WorldSchematic var1, BlockPos var2, Set<Long> var3) {
      BlockState var4 = var0.method_8320(var2);
      if (var4.method_45474() && var4.method_26227().method_15769()) {
         BlockState var5 = var1.method_8320(var2);
         return var5.method_26204() instanceof FluidBlock ? false : var5.method_26215() || var3.contains(var2.method_10063());
      } else {
         return false;
      }
   }

   private static BlockPos approach(BlockState var0, BlockPos var1) {
      Direction var3 = StateRules.lookFor(var0);
      if (var3 == null) {
         var3 = StateRules.flatLookFor(var0);
      }

      Direction var2;
      if (var3 == null && var0.method_26204() instanceof HopperBlock && (var2 = (Direction)var0.method_11654(Properties.field_12545)) != Direction.field_11033) {
         var3 = var2;
      }

      return var3 == null ? null : var1.method_10093(var3.method_10153());
   }

   static Map<Item, Integer> remainingItems(ClientWorld var0, WorldSchematic var1, List<SchematicManager.ActiveBox> var2) {
      HashMap var3 = new HashMap();
      Mutable var4 = new Mutable();

      for (SchematicManager.ActiveBox var6 : var2) {
         for (int var7 = var6.minY(); var7 <= var6.maxY(); var7++) {
            for (int var8 = var6.minX(); var8 <= var6.maxX(); var8++) {
               for (int var9 = var6.minZ(); var9 <= var6.maxZ(); var9++) {
                  var4.method_10103(var8, var7, var9);
                  BlockState var11 = var1.method_8320(var4);
                  if (!var11.method_26215() && var0.method_22340(var4)) {
                     BlockState var12 = var0.method_8320(var4);
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

   private void releaseTnt(ClientWorld var1, WorldSchematic var2, Predicate<BlockPos> var3, Predicate<BlockPos> var4, Set<Long> var5) {
      if (!this.powerLeft && var5.isEmpty()) {
         for (BuildScan.Cell var7 : this.tntCells) {
            if (BuildOrder.waits(var1, var2, var7.pos(), var7.expected(), var4, this.machineLast)) {
               this.poweredTnt.add(var7.pos());
            } else {
               this.waiting.remove(var7.pos().method_10063());
               if (!var3.test(var7.pos()) && (this.layer == 2147483647 || var7.pos().method_10264() < this.layer)) {
                  this.layer = var7.pos().method_10264();
               }
            }
         }
      }
   }

   private void visit(
      ClientWorld var1,
      WorldSchematic var2,
      Mutable var3,
      Predicate<BlockPos> var4,
      Predicate<BlockPos> var5,
      Predicate<BlockPos> var6,
      boolean var7,
      Set<Long> var8,
      Set<Long> var9,
      boolean var10
   ) {
      BlockState var11 = var2.method_8320(var3);
      if (!var11.method_26215()) {
         if (!var1.method_22340(var3)) {
            this.unloaded++;
         } else {
            BlockState var12 = var1.method_8320(var3);
            CellFiller.Kind var13 = needsLiquid(var11, var12);
            if (var13 != null) {
               BlockPos var14 = var3.method_10062();
               boolean var15 = var13 == CellFiller.Kind.LAVA_SOURCE && capped(var2, var14);
               this.liquids.add(new BuildScan.Liquid(var14, var13, var15));
               if ((var13 != CellFiller.Kind.LAVA_SOURCE || var15) && var6.test(var14)) {
                  this.waterBelow.add(var14.method_10063());
               }
            }

            if (var11.method_26227().method_15769() || var11.method_26204().method_8389() != Items.field_8162) {
               if (BlockStateUtil.isPrimaryPlacementState(var11)) {
                  if (BlockStateUtil.requiredItem(var11) == null) {
                     this.noItem++;
                  } else {
                     TargetStatus var20 = BlockStateUtil.classify(var11, var12);
                     switch (var20) {
                        case MISSING:
                           BlockPos var23 = var3.method_10062();
                           boolean var17 = var11.method_26204() instanceof TntBlock;
                           boolean var22 = !var17 && BuildOrder.pushedIntoTnt(var2, var23, var11) != null;
                           if (!this.placeTnt && (var17 || var22)) {
                              (var17 ? this.tntForPlayer : this.afterTntForPlayer).add(var23);
                              this.waiting.add(var23.method_10063());
                              return;
                           }

                           int var19 = BuildOrder.stage(var2, var23, var11, this.machineLast);
                           if (var7 && !var17 && !var22 && var19 > 0) {
                              this.powerLeft = true;
                              this.staged.put(var23.method_10063(), var19);
                           }

                           if (var7 && var17) {
                              this.missing.add(new BuildScan.Cell(var23, var11, var12));
                              this.tntCells.add(new BuildScan.Cell(var23, var11, var12));
                              this.waiting.add(var23.method_10063());
                              return;
                           }

                           if (!var7 && var19 > 0) {
                              this.late++;
                              this.lateCells.add(new BuildScan.Cell(var23, var11, var12));
                              this.waiting.add(var23.method_10063());
                              return;
                           }

                           this.missing.add(new BuildScan.Cell(var23, var11, var12));
                           if (this.waterBelow.contains(var23.method_10074().method_10063())
                              || BuildOrder.waits(var1, var2, var23, var11, var5, this.machineLast)) {
                              this.waiting.add(var23.method_10063());
                              return;
                           }

                           if (this.layer == 2147483647 && !var4.test(var23)) {
                              this.layer = var23.method_10264();
                           }
                           break;
                        case ADJUST:
                           this.adjust.add(new BuildScan.Cell(var3.method_10062(), var11, var12));
                           break;
                        case CORRECT:
                           if (var11.method_26204() instanceof LeverBlock
                              && (Boolean)var11.method_11654(Properties.field_12484)
                              && !(Boolean)var12.method_11654(Properties.field_12484)) {
                              this.leversOff++;
                              this.levers.add(var3.method_10062());
                           }

                           if (BuildOrder.railShapeOff(var12, var11)) {
                              this.railsOff++;
                           }

                           boolean[] var21;
                           if (var10
                              && var11.method_26204() instanceof CrafterBlock
                              && !var9.contains(var3.method_10063())
                              && (var21 = disabledSlots(var2, var3)) != null) {
                              BlockPos var16 = var3.method_10062();
                              this.crafters.add(var16);
                              this.crafterSlots.put(var16, var21);
                           }
                           break;
                        case WRONG:
                           if (!var8.contains(var3.method_10063())) {
                              this.wrong++;
                           }
                     }
                  }
               }
            }
         }
      }
   }

   static CellFiller.Kind needsLiquid(BlockState var0, BlockState var1) {
      FluidState var2 = var0.method_26227();
      if (!var2.method_15769() && var2.method_15771()) {
         FluidState var3 = var1.method_26227();
         if (var2.method_15767(FluidTags.field_15518)) {
            if (var3.method_15771() && var3.method_15767(FluidTags.field_15518)) {
               return null;
            } else {
               return !var1.method_26215() && (!var1.method_45474() || !var3.method_15769() && !var3.method_15767(FluidTags.field_15518))
                  ? null
                  : CellFiller.Kind.LAVA_SOURCE;
            }
         } else if (!var2.method_15767(FluidTags.field_15517)) {
            return null;
         } else if (var0.method_28498(Properties.field_12508)) {
            return StateRules.placed(var1, var0) && !var1.method_11654(Properties.field_12508) ? CellFiller.Kind.WATERLOG : null;
         } else if (var3.method_15771() && var3.method_15767(FluidTags.field_15517)) {
            return null;
         } else {
            return !var1.method_26215() && !var1.method_45474() ? null : CellFiller.Kind.WATER_SOURCE;
         }
      } else {
         return null;
      }
   }

   private static boolean capped(WorldSchematic var0, BlockPos var1) {
      Mutable var2 = var1.method_25503();

      for (int var3 = 0; var3 < 64; var3++) {
         var2.method_10098(Direction.field_11036);
         BlockState var4 = var0.method_8320(var2);
         if (!var4.method_26227().method_15767(FluidTags.field_15518)) {
            return !var4.method_26215() && var4.method_26227().method_15769();
         }
      }

      return false;
   }

   private static boolean[] disabledSlots(WorldSchematic var0, BlockPos var1) {
      if (!(var0.method_8321(var1) instanceof CrafterBlockEntity var3)) {
         return null;
      } else {
         boolean[] var4 = new boolean[9];
         boolean var5 = false;

         for (int var6 = 0; var6 < 9; var6++) {
            var4[var6] = var3.method_54483(var6);
            var5 |= var4[var6];
         }

         return var5 ? var4 : null;
      }
   }

   Map<Item, Integer> needs(int var1) {
      HashMap var2 = new HashMap();

      for (BuildScan.Cell var4 : this.missing) {
         Item var5;
         if (var4.pos().method_10264() <= var1 && (var5 = BlockStateUtil.requiredItem(var4.expected())) != null) {
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
