package net.efn.client.easyplace.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
final class StandPlanner {
   private static final double PILLAR_COST = 8.0;
   private static final double BRIDGE_COST = 5.0;
   private static final double BUILD_BRIDGE_COST = 2.0;
   private static final double PROP_PENALTY = 8.0;
   private static final double LEAP_COST = 2.5;
   private static final double WADE_COST = 2.5;
   private static final double ICE_COST = 2.0;
   private static final int MAX_GAP = 1;
   private static final double FALL_COST = 40.0;
   private static final double DROP_COST = 2.0;
   private static final int BRIDGE_LIMIT = 10;
   private static final double EYE = 1.62;
   private static final double CROUCH_EYE = 1.27;
   private static final int MAX_SPOTS = 12000;
   private static final double SLACK = 10.0;
   private static final double GAIN = 2.0;
   private static final int MAX_CANDIDATES = 16;
   private static final int COUNT_CAP = 24;
   private static final int RANGE = 40;
   private static final int AROUND = 8;
   private static final double[][] FACE_POINTS = new double[][]{{0.5, 0.5}, {0.2, 0.5}, {0.8, 0.5}, {0.5, 0.2}, {0.5, 0.8}};
   private static final double[][] EDGE_POINTS = new double[][]{{0.5, 0.5}, {0.03, 0.5}, {0.97, 0.5}, {0.5, 0.03}, {0.5, 0.97}};
   private static final int MAX_LEAVES = 10;
   private static final long[] NO_LEAVES = new long[0];
   private final ClientWorld level;
   private final ClientPlayerEntity player;
   private final List<StandPlanner.Target> targets;
   private final Predicate<BlockPos> avoid;
   private final double reach;
   private final StandPlanner.Pillar canPillar;
   private final Predicate<BlockPos> canBridge;
   private final StandPlanner.Exact exact;
   private final StandPlanner.Support support;
   private double maxFall;
   private final int maxFeetY;
   private final Box around;
   private final Box targetBox;
   private final BlockPos start;
   private final PriorityQueue<StandPlanner.Node> open = new PriorityQueue<>(Comparator.comparingDouble(StandPlanner.Node::rank));
   private final Map<Long, Double> best = new HashMap<>();
   private final Map<Long, Long> parent = new HashMap<>();
   private final Map<Long, StandPlanner.Move> moves = new HashMap<>();
   private final Map<Long, Integer> bridges = new HashMap<>();
   private final Map<Long, long[]> leaves = new HashMap<>();
   private final Map<Long, StandPlanner.Target> bridgeTargets = new HashMap<>();
   private int visited;
   private List<StandPlanner.Step> route;
   private StandPlanner.Target found;
   private BlockPos bestSpot;
   private double bestValue = -1.7976931348623157E308;
   private double firstCost = -1.0;
   private int candidates;
   private boolean propped;
   private BlockPos closest;
   private double closestDistance = 1.7976931348623157E308;
   private final double startDistance;

   StandPlanner(
      ClientWorld var1,
      ClientPlayerEntity var2,
      List<StandPlanner.Target> var3,
      Predicate<BlockPos> var4,
      StandPlanner.Pillar var5,
      Predicate<BlockPos> var6,
      StandPlanner.Exact var7,
      StandPlanner.Support var8
   ) {
      this.level = var1;
      this.player = var2;
      this.targets = var3;
      this.avoid = var4;
      this.canPillar = var5;
      this.canBridge = var6;
      this.exact = var7;
      this.support = var8;
      this.reach = var2.method_55754() - 0.25;
      this.start = var2.method_24515();

      for (StandPlanner.Target var10 : var3) {
         BlockState var11 = var10.expected();
         if (var10.use() == StandPlanner.Use.PLACE
            && StateRules.lookFor(var11) == null
            && StateRules.flatLookFor(var11) == null
            && !(var11.method_26204() instanceof FallingBlock)
            && var11.method_26234(var1, var10.pos())) {
            this.bridgeTargets.put(var10.pos().method_10063(), var10);
         }
      }

      Box var12 = null;

      for (StandPlanner.Target var14 : var3) {
         var12 = var12 == null ? new Box(var14.pos()) : var12.method_991(new Box(var14.pos()));
      }

      this.maxFeetY = var12 == null ? this.start.method_10264() : (int)var12.field_1325 + 1;
      this.around = var12 == null ? new Box(this.start) : var12.method_991(new Box(this.start)).method_1014(8.0);
      this.targetBox = var12 == null ? new Box(this.start) : var12.method_1014(8.0);
      this.startDistance = this.workDistance(this.start);
      this.open.add(new StandPlanner.Node(this.start.method_10063(), 0.0, 0.0));
      this.best.put(this.start.method_10063(), 0.0);
      this.parent.put(this.start.method_10063(), -9223372036854775808L);
      this.leaves.put(this.start.method_10063(), NO_LEAVES);
   }

   int visited() {
      return this.visited;
   }

   int lowestTargetY() {
      return (int)Math.round(this.targetBox.field_1322) + 8;
   }

   String extent() {
      int var1 = 2147483647;
      int var2 = -2147483648;
      int var3 = 2147483647;
      int var4 = -2147483648;
      int var5 = 2147483647;
      int var6 = -2147483648;
      int var7 = 0;
      int var8 = 0;

      for (Entry var10 : this.moves.entrySet()) {
         BlockPos var11 = BlockPos.method_10092((Long)var10.getKey());
         var1 = Math.min(var1, var11.method_10263());
         var2 = Math.max(var2, var11.method_10263());
         var3 = Math.min(var3, var11.method_10264());
         var4 = Math.max(var4, var11.method_10264());
         var5 = Math.min(var5, var11.method_10260());
         var6 = Math.max(var6, var11.method_10260());
         if (var10.getValue() == StandPlanner.Move.PILLAR) {
            var7++;
         }

         if (var10.getValue() == StandPlanner.Move.BRIDGE) {
            var8++;
         }
      }

      return "x "
         + var1
         + ".."
         + var2
         + " y "
         + var3
         + ".."
         + var4
         + " z "
         + var5
         + ".."
         + var6
         + " pillars "
         + var7
         + " bridges "
         + var8
         + " targetBox "
         + this.targetBox
         + " maxFeetY "
         + this.maxFeetY
         + " | "
         + this.whyStuck();
   }

   String whyStuck() {
      boolean var1 = this.around.method_1006(this.start.method_46558());
      StringBuilder var2 = new StringBuilder(
         "near=" + var1 + " around=" + this.around + " maxFeetY=" + this.maxFeetY + " avoidUp=" + this.avoid.test(this.start.method_10086(2))
      );
      if (this.canPillar != null) {
         var2.append(" pillar=").append(this.canPillar.possible(this.start, false));
      }

      var2.append(" floor=").append(Walker.floor(this.level, this.start));
      if (this.canBridge != null) {
         for (Direction var4 : Type.field_11062) {
            BlockPos var5 = this.start.method_10093(var4);
            var2.append(' ')
               .append(var4)
               .append(":side=")
               .append(
                  Block.method_9501(this.level.method_8320(this.start.method_10074()).method_26218((BlockView)this.level, this.start.method_10074()), var4)
               )
               .append(",gap=")
               .append(Double.isNaN(Walker.floor(this.level, var5)))
               .append(",room=")
               .append(Walker.roomAt(this.level, var5))
               .append(",fits=")
               .append(this.canBridge.test(var5.method_10074()))
               .append(",avoid=")
               .append(this.avoid.test(var5));
         }
      }

      return var2.toString();
   }

   StandPlanner falls(double var1) {
      this.maxFall = var1;
      return this;
   }

   List<StandPlanner.Step> route() {
      return this.route;
   }

   StandPlanner.Target target() {
      return this.found;
   }

   StandPlanner.Status step(long var1) {
      while (!this.open.isEmpty()) {
         if (System.nanoTime() > var1) {
            return StandPlanner.Status.RUNNING;
         }

         StandPlanner.Node var4 = this.open.poll();
         if (!(var4.cost() > this.best.getOrDefault(var4.pos(), 1.7976931348623157E308))) {
            if (this.firstCost >= 0.0 && (var4.cost() > this.firstCost + 10.0 || this.candidates >= 16)) {
               return this.finish();
            }

            if (++this.visited > 12000) {
               return this.finish();
            }

            BlockPos var5 = BlockPos.method_10092(var4.pos());
            if (!var5.equals(this.start)) {
               double var6 = this.workDistance(var5);
               if (var6 < this.closestDistance) {
                  this.closestDistance = var6;
                  this.closest = var5;
               }

               long[] var8 = this.leaves.getOrDefault(var4.pos(), NO_LEAVES);
               StandPlanner.Target var9 = this.moves.get(var4.pos()) == StandPlanner.Move.BRIDGE
                  ? this.bridgeTargets.get(var5.method_10074().method_10063())
                  : null;
               this.propped = false;
               if (var9 == null) {
                  var9 = this.workFrom(var5, var8);
               }

               boolean var10 = this.propped;
               if (var9 != null) {
                  double var11 = this.countFrom(var5, var8) * 2.0
                     - var4.cost()
                     - (var10 && this.bridgeTargets.containsKey(var9.pos().method_10063()) ? 8.0 : 0.0);
                  if (this.firstCost < 0.0) {
                     this.firstCost = var4.cost();
                  }

                  this.candidates++;
                  if (var11 > this.bestValue) {
                     this.bestValue = var11;
                     this.bestSpot = var5;
                     this.found = var9;
                  }
               }
            }

            for (BlockPos var7 : Walker.steps(this.level, var5)) {
               int var17 = var7.method_10264() - var5.method_10264();
               double var21 = (this.level.method_8316(var7).method_15769() ? 0.0 : 2.5) + (Walker.icy(this.level, var7) ? 2.0 : 0.0);
               this.relax(var5, var7, var4.cost() + 1.0 + (var17 > 0 ? 0.6 : -var17 * 2.0) + var21, StandPlanner.Move.WALK);
            }

            if (this.maxFall > 0.0) {
               for (Direction var16 : Type.field_11062) {
                  BlockPos var18 = Walker.fall(this.level, var5, var16, this.maxFall);
                  if (var18 != null) {
                     this.relax(var5, var18, var4.cost() + 40.0 + (var5.method_10264() - var18.method_10264()) * 2.0, StandPlanner.Move.WALK);
                  }
               }
            }

            double var15 = Walker.floorOr(this.level, var5);

            for (Direction var22 : Type.field_11062) {
               for (int var24 = 1; var24 <= 1; var24++) {
                  if (Walker.leap(this.level, var5, var15, var22, var24)) {
                     this.relax(var5, var5.method_10079(var22, var24 + 1), var4.cost() + 2.5 + var24, StandPlanner.Move.LEAP);
                  }
               }
            }

            boolean var20 = this.around.method_1006(var5.method_46558()) || var5.method_19455((Vec3i)this.start) <= 4;
            boolean var23 = var20
               || var5.method_10264() < this.targetBox.field_1322
                  && var5.method_10263() + 0.5 >= this.targetBox.field_1323
                  && var5.method_10263() + 0.5 <= this.targetBox.field_1320
                  && var5.method_10260() + 0.5 >= this.targetBox.field_1321
                  && var5.method_10260() + 0.5 <= this.targetBox.field_1324;
            boolean var3 = this.moves.get(var4.pos()) == StandPlanner.Move.PILLAR || this.moves.get(var4.pos()) == StandPlanner.Move.BRIDGE;
            if (var23 && this.canPillar != null && var5.method_10264() + 1 <= this.maxFeetY && this.canPillar.possible(var5, var3)) {
               if (var5.method_10264() + 2 <= this.maxFeetY) {
                  this.relax(var5, var5.method_10086(2), var4.cost() + 8.0, StandPlanner.Move.PILLAR);
               }

               if (var5.method_10264() + 1 >= this.lowestTargetY()) {
                  this.relax(var5, var5.method_10086(1), var4.cost() + 4.8, StandPlanner.Move.PILLAR);
               }
            }

            if (var20 && var5.method_10264() >= this.targetBox.field_1322 && this.canBridge != null && this.bridges.getOrDefault(var4.pos(), 0) < 10) {
               this.bridgeFrom(var5, var4.cost());
            }
         }
      }

      return this.finish();
   }

   private StandPlanner.Status finish() {
      if (this.bestSpot == null) {
         if (this.closest != null && !(this.closestDistance > this.startDistance - 2.0)) {
            this.route = this.path(this.closest);
            return StandPlanner.Status.APPROACH;
         } else {
            return StandPlanner.Status.NONE;
         }
      } else {
         this.route = this.path(this.bestSpot);
         return StandPlanner.Status.FOUND;
      }
   }

   private int countFrom(BlockPos var1, long[] var2) {
      double var5 = Walker.floorOr(this.level, var1);
      Vec3d var7 = new Vec3d(var1.method_10263() + 0.5, var5 + 1.62, var1.method_10260() + 0.5);
      Box var8 = new Box(var1.method_10263() + 0.2, var5, var1.method_10260() + 0.2, var1.method_10263() + 0.8, var5 + 1.8, var1.method_10260() + 0.8);
      int var9 = 0;
      Iterator var10 = this.targets.iterator();

      while (var10.hasNext()) {
         StandPlanner.Target var3;
         BlockPos var4;
         if (!((var4 = (var3 = (StandPlanner.Target)var10.next()).pos()).method_46558().method_1022(var7) > Math.min(this.reach, var3.within()))
            && !var8.method_994(new Box(var4))
            && (var3.use() == StandPlanner.Use.PLACE ? this.seenFrom(var7, var3) || this.onPlanned(var7, var3, var2) : this.placeableFrom(var7, var3, var2))) {
            if (++var9 >= 24) {
               break;
            }
         }
      }

      return var9;
   }

   private boolean seenFrom(Vec3d var1, StandPlanner.Target var2) {
      Direction var3 = StateRules.lookFor(var2.expected());
      Direction var4 = StateRules.flatLookFor(var2.expected());

      for (Direction var8 : Direction.values()) {
         BlockPos var10 = var2.pos().method_10093(var8);
         BlockState var11 = this.level.method_8320(var10);
         VoxelShape var9;
         if (!var11.method_45474() && !(var9 = var11.method_26218((BlockView)this.level, var10)).method_1110()) {
            Direction var12 = var8.method_10153();
            Box var13 = var9.method_1107().method_996(var10);
            if (this.seen(var1, var10, var13, var12, var3, var4)) {
               return true;
            }
         }
      }

      return false;
   }

   private void bridgeFrom(BlockPos var1, double var2) {
      StandPlanner.Move var5 = this.moves.get(var1.method_10063());
      boolean var4 = var5 == StandPlanner.Move.BRIDGE || var5 == StandPlanner.Move.PILLAR;
      if (var4 || Walker.floor(this.level, var1) == var1.method_10264()) {
         BlockPos var7 = var1.method_10074();

         for (Direction var9 : Type.field_11062) {
            boolean var10;
            BlockPos var11;
            if ((var4 || Block.method_9501(this.level.method_8320(var7).method_26218((BlockView)this.level, var7), var9))
               && Double.isNaN(Walker.floor(this.level, var11 = var1.method_10093(var9)))
               && Walker.roomAt(this.level, var11)
               && ((var10 = this.bridgeTargets.containsKey(var11.method_10074().method_10063())) || this.canBridge.test(var11.method_10074()))) {
               this.relax(var1, var11, var2 + (var10 ? 2.0 : 5.0), StandPlanner.Move.BRIDGE);
            }
         }
      }
   }

   private void relax(BlockPos var1, BlockPos var2, double var3, StandPlanner.Move var5) {
      if (!this.avoid.test(var2)) {
         if (Math.abs(var2.method_10263() - this.start.method_10263()) <= 40
            && Math.abs(var2.method_10260() - this.start.method_10260()) <= 40
            && Math.abs(var2.method_10264() - this.start.method_10264()) <= 40
            && var2.method_10264() <= this.maxFeetY + 1) {
            long var6 = var2.method_10063();
            if (!(var3 >= this.best.getOrDefault(var6, 1.7976931348623157E308))) {
               this.best.put(var6, var3);
               this.parent.put(var6, var1.method_10063());
               this.moves.put(var6, var5);
               this.bridges.put(var6, this.bridges.getOrDefault(var1.method_10063(), 0) + (var5 == StandPlanner.Move.BRIDGE ? 1 : 0));
               long[] var8 = this.leaves.getOrDefault(var1.method_10063(), NO_LEAVES);
               this.leaves
                  .put(
                     var6,
                     var5 != StandPlanner.Move.BRIDGE && var5 != StandPlanner.Move.PILLAR
                        ? var8
                        : withLeaf(var8, var2.method_10074(), var5 == StandPlanner.Move.PILLAR)
                  );
               this.open.add(new StandPlanner.Node(var6, var3, var3 + this.guess(var2)));
            }
         }
      }
   }

   private double workDistance(BlockPos var1) {
      Vec3d var2 = new Vec3d(var1.method_10263() + 0.5, var1.method_10264() + 1.62, var1.method_10260() + 0.5);
      double var3 = 1.7976931348623157E308;

      for (int var5 = 0; var5 < Math.min(32, this.targets.size()); var5++) {
         var3 = Math.min(var3, this.targets.get(var5).pos().method_46558().method_1022(var2));
      }

      return var3;
   }

   private double guess(BlockPos var1) {
      Vec3d var2 = new Vec3d(var1.method_10263() + 0.5, var1.method_10264() + 1.62, var1.method_10260() + 0.5);
      double var3 = Math.max(0.0, Math.max(this.targetBox.field_1323 + 8.0 - var2.field_1352, var2.field_1352 - (this.targetBox.field_1320 - 8.0)));
      double var5 = Math.max(0.0, Math.max(this.targetBox.field_1322 + 8.0 - var2.field_1351, var2.field_1351 - (this.targetBox.field_1325 - 8.0))) * 6.5;
      double var7 = Math.max(0.0, Math.max(this.targetBox.field_1321 + 8.0 - var2.field_1350, var2.field_1350 - (this.targetBox.field_1324 - 8.0)));
      return Math.max(0.0, Math.sqrt(var3 * var3 + var5 * var5 + var7 * var7) - this.reach) * 0.6;
   }

   private List<StandPlanner.Step> path(BlockPos var1) {
      ArrayList var2 = new ArrayList();
      long var3 = var1.method_10063();

      while (var3 != this.start.method_10063() && var3 != -9223372036854775808L) {
         var2.add(new StandPlanner.Step(BlockPos.method_10092(var3), this.moves.getOrDefault(var3, StandPlanner.Move.WALK)));
         var3 = this.parent.get(var3);
      }

      Collections.reverse(var2);
      return var2;
   }

   private StandPlanner.Target workFrom(BlockPos var1, long[] var2) {
      if ((this.level.method_8316(var1).method_15769() || this.level.method_8316(var1).method_15771()) && !Walker.nearLava(this.level, var1)) {
         double var3 = Walker.floorOr(this.level, var1);
         Vec3d var5 = new Vec3d(var1.method_10263() + 0.5, var3 + 1.62, var1.method_10260() + 0.5);
         Box var6 = new Box(var1.method_10263() + 0.2, var3, var1.method_10260() + 0.2, var1.method_10263() + 0.8, var3 + 1.8, var1.method_10260() + 0.8);

         for (StandPlanner.Target var8 : this.targets) {
            BlockPos var9 = var8.pos();
            if (!(var9.method_46558().method_1025(var5) > (this.reach + 0.9) * (this.reach + 0.9))
               && !(var9.method_46558().method_1022(var5) > var8.within())
               && (
                  !var6.method_994(new Box(var9))
                     || var8.use() == StandPlanner.Use.PLACE && var8.expected().method_26220((BlockView)this.level, var9).method_1110()
               )
               && this.placeableFrom(var5, var8, var2)) {
               return var8;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean placeableFrom(Vec3d var1, StandPlanner.Target var2, long[] var3) {
      BlockPos var4 = var2.pos();
      switch (var2.use()) {
         case WATERLOG:
         case BREAK:
         case CLICK:
            if (var4.method_46558().method_1022(var1) > this.reach) {
               return false;
            } else {
               VoxelShape var16 = this.level.method_8320(var4).method_26218((BlockView)this.level, var4);
               if (var16.method_1110()) {
                  return false;
               } else {
                  Box var17 = var16.method_1107().method_996(var4);

                  for (Direction var21 : Direction.values()) {
                     if (facesEye(var17, var21, var1) && this.seen(var1, var4, var17, var21, null, null)) {
                        return true;
                     }
                  }

                  return false;
               }
            }
         case NEAR:
            return var4.method_46558().method_1022(var1) <= this.reach - 0.5 && this.support != null && this.support.possible(var4, var1);
         default:
            Direction var5 = var2.use() == StandPlanner.Use.PLACE ? StateRules.lookFor(var2.expected()) : null;
            Direction var6 = var2.use() == StandPlanner.Use.PLACE ? StateRules.flatLookFor(var2.expected()) : null;

            for (Direction var10 : Direction.values()) {
               BlockPos var12 = var4.method_10093(var10);
               BlockState var13 = this.level.method_8320(var12);
               VoxelShape var11;
               if (!var13.method_45474() && !(var11 = var13.method_26218((BlockView)this.level, var12)).method_1110()) {
                  Direction var14 = var10.method_10153();
                  Box var15 = var11.method_1107().method_996(var12);
                  if (this.seen(var1, var12, var15, var14, var5, var6)
                     && (var2.use() != StandPlanner.Use.PLACE || this.exact == null || this.exact.placeable(var2.expected(), var4, var1))) {
                     return true;
                  }
               }
            }

            if (this.onPlanned(var1, var2, var3)) {
               return true;
            } else if (var2.use() != StandPlanner.Use.PLACE) {
               return false;
            } else if (StateRules.slabHalf(this.level.method_8320(var4), var2.expected())) {
               return this.exact != null && this.exact.placeable(var2.expected(), var4, var1);
            } else {
               this.propped = this.support != null && this.support.behind(var2.expected(), var4, var1);
               return this.propped;
            }
      }
   }

   private boolean onPlanned(Vec3d var1, StandPlanner.Target var2, long[] var3) {
      if (var3.length == 0) {
         return false;
      } else if (var2.use() != StandPlanner.Use.PLACE || StateRules.lookFor(var2.expected()) == null && StateRules.flatLookFor(var2.expected()) == null) {
         BlockPos var4 = var2.pos();

         for (long var8 : var3) {
            BlockPos var12 = BlockPos.method_10092(var8);
            Direction var10;
            Box var11;
            if (var12.method_19455(var4) == 1
               && this.level.method_8320(var12).method_45474()
               && facesEye(
                  var11 = new Box(var12),
                  var10 = Direction.method_10147(
                     var4.method_10263() - var12.method_10263(), var4.method_10264() - var12.method_10264(), var4.method_10260() - var12.method_10260()
                  ),
                  var1
               )) {
               for (double[] var16 : FACE_POINTS) {
                  Vec3d var18 = facePoint(var11, var10, var16[0], var16[1]);
                  if (!(var18.method_1022(var1) > this.reach)
                     && this.level
                           .method_17742(
                              new RaycastContext(
                                 var1,
                                 var18.method_1020(var18.method_1020(var1).method_1029().method_1021(0.05)),
                                 ShapeType.field_17559,
                                 FluidHandling.field_1348,
                                 (Entity)this.player
                              )
                           )
                           .method_17783()
                        == net.minecraft.util.hit.HitResult.Type.field_1333) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static long[] withLeaf(long[] var0, BlockPos var1, boolean var2) {
      long[] var3 = new long[Math.min(var0.length + 1, 10)];
      int var4 = 0;

      for (int var5 = Math.max(0, var0.length + 1 - 10); var5 < var0.length; var5++) {
         BlockPos var6 = BlockPos.method_10092(var0[var5]);
         if (!var2 || var6.method_10263() != var1.method_10263() || var6.method_10260() != var1.method_10260() || var6.method_10264() >= var1.method_10264()) {
            var3[var4++] = var0[var5];
         }
      }

      var3[var4++] = var1.method_10063();
      return var4 == var3.length ? var3 : Arrays.copyOf(var3, var4);
   }

   private boolean seen(Vec3d var1, BlockPos var2, Box var3, Direction var4, Direction var5, Direction var6) {
      return this.seenFrom(var1, var2, var3, var4, var5, var6, FACE_POINTS, 0.2)
         ? true
         : var5 == Direction.field_11036 && this.seenFrom(var1.method_1023(0.0, 0.3500000000000001, 0.0), var2, var3, var4, var5, var6, EDGE_POINTS, 0.05);
   }

   private boolean seenFrom(Vec3d var1, BlockPos var2, Box var3, Direction var4, Direction var5, Direction var6, double[][] var7, double var8) {
      if (!facesEye(var3, var4, var1)) {
         return false;
      } else {
         for (double[] var13 : var7) {
            Vec3d var15 = facePoint(var3, var4, var13[0], var13[1]);
            BlockHitResult var14;
            if (!(var15.method_1022(var1) > this.reach)
               && (var5 == null || Walker.dominant(var15.method_1020(var1), var5, var8))
               && (var6 == null || Walker.facingFlat(var15.method_1020(var1), var6))
               && (var14 = this.level
                        .method_17742(
                           new RaycastContext(
                              var1,
                              var15.method_1019(var15.method_1020(var1).method_1029().method_1021(0.05)),
                              ShapeType.field_17559,
                              FluidHandling.field_1348,
                              (Entity)this.player
                           )
                        ))
                     .method_17783()
                  == net.minecraft.util.hit.HitResult.Type.field_1332
               && var14.method_17777().equals(var2)
               && var14.method_17780() == var4) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean facesEye(Box var0, Direction var1, Vec3d var2) {
      double var3 = 0.05;

      return switch (var1) {
         case field_11036 -> var2.field_1351 > var0.field_1325 + var3;
         case field_11033 -> var2.field_1351 < var0.field_1322 - var3;
         case field_11043 -> var2.field_1350 < var0.field_1321 - var3;
         case field_11035 -> var2.field_1350 > var0.field_1324 + var3;
         case field_11039 -> var2.field_1352 < var0.field_1323 - var3;
         case field_11034 -> var2.field_1352 > var0.field_1320 + var3;
         default -> throw new MatchException(null, null);
      };
   }

   private static Vec3d facePoint(Box var0, Direction var1, double var2, double var4) {
      double var6 = var0.field_1323 + (var0.field_1320 - var0.field_1323) * var2;
      double var8 = var0.field_1322 + (var0.field_1325 - var0.field_1322) * var4;

      return switch (var1) {
         case field_11036 -> new Vec3d(var6, var0.field_1325, var0.field_1321 + (var0.field_1324 - var0.field_1321) * var4);
         case field_11033 -> new Vec3d(var6, var0.field_1322, var0.field_1321 + (var0.field_1324 - var0.field_1321) * var4);
         case field_11043 -> new Vec3d(var6, var8, var0.field_1321);
         case field_11035 -> new Vec3d(var6, var8, var0.field_1324);
         case field_11039 -> new Vec3d(var0.field_1323, var8, var0.field_1321 + (var0.field_1324 - var0.field_1321) * var2);
         case field_11034 -> new Vec3d(var0.field_1320, var8, var0.field_1321 + (var0.field_1324 - var0.field_1321) * var2);
         default -> throw new MatchException(null, null);
      };
   }

   @Environment(EnvType.CLIENT)
   interface Exact {
      boolean placeable(BlockState var1, BlockPos var2, Vec3d var3);
   }

   @Environment(EnvType.CLIENT)
   static enum Move {
      WALK,
      PILLAR,
      BRIDGE,
      LEAP;

      private Move() {
      }
   }

   @Environment(EnvType.CLIENT)
   private record Node(long pos, double cost, double rank) {
   }

   @Environment(EnvType.CLIENT)
   interface Pillar {
      boolean possible(BlockPos var1, boolean var2);
   }

   @Environment(EnvType.CLIENT)
   static enum Status {
      RUNNING,
      FOUND,
      NONE,
      APPROACH;

      private Status() {
      }
   }

   @Environment(EnvType.CLIENT)
   record Step(BlockPos pos, StandPlanner.Move move) {
   }

   @Environment(EnvType.CLIENT)
   interface Support {
      boolean possible(BlockPos var1, Vec3d var2);

      boolean behind(BlockState var1, BlockPos var2, Vec3d var3);
   }

   @Environment(EnvType.CLIENT)
   record Target(BlockPos pos, BlockState expected, StandPlanner.Use use, double within) {
      Target(BlockPos var1, BlockState var2, StandPlanner.Use var3) {
         this(var1, var2, var3, 1.7976931348623157E308);
      }
   }

   @Environment(EnvType.CLIENT)
   static enum Use {
      PLACE,
      POUR,
      WATERLOG,
      NEAR,
      BREAK,
      CLICK;

      private Use() {
      }
   }
}
