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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.shape.VoxelShape;
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
      ClientWorld clientWorld,
      ClientPlayerEntity clientPlayerEntity,
      List<StandPlanner.Target> list,
      Predicate<BlockPos> predicate,
      StandPlanner.Pillar pillar,
      Predicate<BlockPos> predicate2,
      StandPlanner.Exact exact,
      StandPlanner.Support support
   ) {
      this.level = clientWorld;
      this.player = clientPlayerEntity;
      this.targets = list;
      this.avoid = predicate;
      this.canPillar = pillar;
      this.canBridge = predicate2;
      this.exact = exact;
      this.support = support;
      this.reach = clientPlayerEntity.getBlockInteractionRange() - 0.25;
      this.start = clientPlayerEntity.getBlockPos();

      for (StandPlanner.Target var10 : list) {
         BlockState var11 = var10.expected();
         if (var10.use() == StandPlanner.Use.PLACE
            && StateRules.lookFor(var11) == null
            && StateRules.flatLookFor(var11) == null
            && !(var11.getBlock() instanceof FallingBlock)
            && var11.isFullCube(clientWorld, var10.pos())) {
            this.bridgeTargets.put(var10.pos().asLong(), var10);
         }
      }

      Box var12 = null;

      for (StandPlanner.Target var14 : list) {
         var12 = var12 == null ? new Box(var14.pos()) : var12.union(new Box(var14.pos()));
      }

      this.maxFeetY = var12 == null ? this.start.getY() : (int)var12.maxY + 1;
      this.around = var12 == null ? new Box(this.start) : var12.union(new Box(this.start)).expand(8.0);
      this.targetBox = var12 == null ? new Box(this.start) : var12.expand(8.0);
      this.startDistance = this.workDistance(this.start);
      this.open.add(new StandPlanner.Node(this.start.asLong(), 0.0, 0.0));
      this.best.put(this.start.asLong(), 0.0);
      this.parent.put(this.start.asLong(), -9223372036854775808L);
      this.leaves.put(this.start.asLong(), NO_LEAVES);
   }

   int visited() {
      return this.visited;
   }

   int lowestTargetY() {
      return (int)Math.round(this.targetBox.minY) + 8;
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
         BlockPos var11 = BlockPos.fromLong((Long)var10.getKey());
         var1 = Math.min(var1, var11.getX());
         var2 = Math.max(var2, var11.getX());
         var3 = Math.min(var3, var11.getY());
         var4 = Math.max(var4, var11.getY());
         var5 = Math.min(var5, var11.getZ());
         var6 = Math.max(var6, var11.getZ());
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
      boolean var1 = this.around.contains(this.start.toCenterPos());
      StringBuilder var2 = new StringBuilder(
         "near=" + var1 + " around=" + this.around + " maxFeetY=" + this.maxFeetY + " avoidUp=" + this.avoid.test(this.start.up(2))
      );
      if (this.canPillar != null) {
         var2.append(" pillar=").append(this.canPillar.possible(this.start, false));
      }

      var2.append(" floor=").append(Walker.floor(this.level, this.start));
      if (this.canBridge != null) {
         for (Direction var4 : Type.HORIZONTAL) {
            BlockPos var5 = this.start.offset(var4);
            var2.append(' ')
               .append(var4)
               .append(":side=")
               .append(Block.isFaceFullSquare(this.level.getBlockState(this.start.down()).getOutlineShape(this.level, this.start.down()), var4))
               .append(",gap=")
               .append(Double.isNaN(Walker.floor(this.level, var5)))
               .append(",room=")
               .append(Walker.roomAt(this.level, var5))
               .append(",fits=")
               .append(this.canBridge.test(var5.down()))
               .append(",avoid=")
               .append(this.avoid.test(var5));
         }
      }

      return var2.toString();
   }

   StandPlanner falls(double d) {
      this.maxFall = d;
      return this;
   }

   List<StandPlanner.Step> route() {
      return this.route;
   }

   StandPlanner.Target target() {
      return this.found;
   }

   StandPlanner.Status step(long l) {
      while (!this.open.isEmpty()) {
         if (System.nanoTime() > l) {
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

            BlockPos var5 = BlockPos.fromLong(var4.pos());
            if (!var5.equals(this.start)) {
               double var6 = this.workDistance(var5);
               if (var6 < this.closestDistance) {
                  this.closestDistance = var6;
                  this.closest = var5;
               }

               long[] var8 = this.leaves.getOrDefault(var4.pos(), NO_LEAVES);
               StandPlanner.Target var9 = this.moves.get(var4.pos()) == StandPlanner.Move.BRIDGE ? this.bridgeTargets.get(var5.down().asLong()) : null;
               this.propped = false;
               if (var9 == null) {
                  var9 = this.workFrom(var5, var8);
               }

               boolean var10 = this.propped;
               if (var9 != null) {
                  double var11 = this.countFrom(var5, var8) * 2.0 - var4.cost() - (var10 && this.bridgeTargets.containsKey(var9.pos().asLong()) ? 8.0 : 0.0);
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
               int var17 = var7.getY() - var5.getY();
               double var21 = (this.level.getFluidState(var7).isEmpty() ? 0.0 : 2.5) + (Walker.icy(this.level, var7) ? 2.0 : 0.0);
               this.relax(var5, var7, var4.cost() + 1.0 + (var17 > 0 ? 0.6 : -var17 * 2.0) + var21, StandPlanner.Move.WALK);
            }

            if (this.maxFall > 0.0) {
               for (Direction var16 : Type.HORIZONTAL) {
                  BlockPos var18 = Walker.fall(this.level, var5, var16, this.maxFall);
                  if (var18 != null) {
                     this.relax(var5, var18, var4.cost() + 40.0 + (var5.getY() - var18.getY()) * 2.0, StandPlanner.Move.WALK);
                  }
               }
            }

            double var15 = Walker.floorOr(this.level, var5);

            for (Direction var22 : Type.HORIZONTAL) {
               for (int var24 = 1; var24 <= 1; var24++) {
                  if (Walker.leap(this.level, var5, var15, var22, var24)) {
                     this.relax(var5, var5.offset(var22, var24 + 1), var4.cost() + 2.5 + var24, StandPlanner.Move.LEAP);
                  }
               }
            }

            boolean var20 = this.around.contains(var5.toCenterPos()) || var5.getManhattanDistance(this.start) <= 4;
            boolean var23 = var20
               || var5.getY() < this.targetBox.minY
                  && var5.getX() + 0.5 >= this.targetBox.minX
                  && var5.getX() + 0.5 <= this.targetBox.maxX
                  && var5.getZ() + 0.5 >= this.targetBox.minZ
                  && var5.getZ() + 0.5 <= this.targetBox.maxZ;
            boolean var3 = this.moves.get(var4.pos()) == StandPlanner.Move.PILLAR || this.moves.get(var4.pos()) == StandPlanner.Move.BRIDGE;
            if (var23 && this.canPillar != null && var5.getY() + 1 <= this.maxFeetY && this.canPillar.possible(var5, var3)) {
               if (var5.getY() + 2 <= this.maxFeetY) {
                  this.relax(var5, var5.up(2), var4.cost() + 8.0, StandPlanner.Move.PILLAR);
               }

               if (var5.getY() + 1 >= this.lowestTargetY()) {
                  this.relax(var5, var5.up(1), var4.cost() + 4.8, StandPlanner.Move.PILLAR);
               }
            }

            if (var20 && var5.getY() >= this.targetBox.minY && this.canBridge != null && this.bridges.getOrDefault(var4.pos(), 0) < 10) {
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

   private int countFrom(BlockPos blockPos, long[] ls) {
      double var5 = Walker.floorOr(this.level, blockPos);
      Vec3d var7 = new Vec3d(blockPos.getX() + 0.5, var5 + 1.62, blockPos.getZ() + 0.5);
      Box var8 = new Box(blockPos.getX() + 0.2, var5, blockPos.getZ() + 0.2, blockPos.getX() + 0.8, var5 + 1.8, blockPos.getZ() + 0.8);
      int var9 = 0;
      Iterator var10 = this.targets.iterator();

      while (var10.hasNext()) {
         StandPlanner.Target var3;
         BlockPos var4;
         if (!((var4 = (var3 = (StandPlanner.Target)var10.next()).pos()).toCenterPos().distanceTo(var7) > Math.min(this.reach, var3.within()))
            && !var8.intersects(new Box(var4))
            && (var3.use() == StandPlanner.Use.PLACE ? this.seenFrom(var7, var3) || this.onPlanned(var7, var3, ls) : this.placeableFrom(var7, var3, ls))) {
            if (++var9 >= 24) {
               break;
            }
         }
      }

      return var9;
   }

   private boolean seenFrom(Vec3d vec3d, StandPlanner.Target target) {
      Direction var3 = StateRules.lookFor(target.expected());
      Direction var4 = StateRules.flatLookFor(target.expected());

      for (Direction var8 : Direction.values()) {
         BlockPos var10 = target.pos().offset(var8);
         BlockState var11 = this.level.getBlockState(var10);
         VoxelShape var9;
         if (!var11.isReplaceable() && !(var9 = var11.getOutlineShape(this.level, var10)).isEmpty()) {
            Direction var12 = var8.getOpposite();
            Box var13 = var9.getBoundingBox().offset(var10);
            if (this.seen(vec3d, var10, var13, var12, var3, var4)) {
               return true;
            }
         }
      }

      return false;
   }

   private void bridgeFrom(BlockPos blockPos, double d) {
      StandPlanner.Move var5 = this.moves.get(blockPos.asLong());
      boolean var4 = var5 == StandPlanner.Move.BRIDGE || var5 == StandPlanner.Move.PILLAR;
      if (var4 || Walker.floor(this.level, blockPos) == blockPos.getY()) {
         BlockPos var7 = blockPos.down();

         for (Direction var9 : Type.HORIZONTAL) {
            boolean var10;
            BlockPos var11;
            if ((var4 || Block.isFaceFullSquare(this.level.getBlockState(var7).getOutlineShape(this.level, var7), var9))
               && Double.isNaN(Walker.floor(this.level, var11 = blockPos.offset(var9)))
               && Walker.roomAt(this.level, var11)
               && ((var10 = this.bridgeTargets.containsKey(var11.down().asLong())) || this.canBridge.test(var11.down()))) {
               this.relax(blockPos, var11, d + (var10 ? 2.0 : 5.0), StandPlanner.Move.BRIDGE);
            }
         }
      }
   }

   private void relax(BlockPos blockPos, BlockPos blockPos2, double d, StandPlanner.Move move) {
      if (!this.avoid.test(blockPos2)) {
         if (Math.abs(blockPos2.getX() - this.start.getX()) <= 40
            && Math.abs(blockPos2.getZ() - this.start.getZ()) <= 40
            && Math.abs(blockPos2.getY() - this.start.getY()) <= 40
            && blockPos2.getY() <= this.maxFeetY + 1) {
            long var6 = blockPos2.asLong();
            if (!(d >= this.best.getOrDefault(var6, 1.7976931348623157E308))) {
               this.best.put(var6, d);
               this.parent.put(var6, blockPos.asLong());
               this.moves.put(var6, move);
               this.bridges.put(var6, this.bridges.getOrDefault(blockPos.asLong(), 0) + (move == StandPlanner.Move.BRIDGE ? 1 : 0));
               long[] var8 = this.leaves.getOrDefault(blockPos.asLong(), NO_LEAVES);
               this.leaves
                  .put(
                     var6,
                     move != StandPlanner.Move.BRIDGE && move != StandPlanner.Move.PILLAR
                        ? var8
                        : withLeaf(var8, blockPos2.down(), move == StandPlanner.Move.PILLAR)
                  );
               this.open.add(new StandPlanner.Node(var6, d, d + this.guess(blockPos2)));
            }
         }
      }
   }

   private double workDistance(BlockPos blockPos) {
      Vec3d var2 = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1.62, blockPos.getZ() + 0.5);
      double var3 = 1.7976931348623157E308;

      for (int var5 = 0; var5 < Math.min(32, this.targets.size()); var5++) {
         var3 = Math.min(var3, this.targets.get(var5).pos().toCenterPos().distanceTo(var2));
      }

      return var3;
   }

   private double guess(BlockPos blockPos) {
      Vec3d var2 = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1.62, blockPos.getZ() + 0.5);
      double var3 = Math.max(0.0, Math.max(this.targetBox.minX + 8.0 - var2.x, var2.x - (this.targetBox.maxX - 8.0)));
      double var5 = Math.max(0.0, Math.max(this.targetBox.minY + 8.0 - var2.y, var2.y - (this.targetBox.maxY - 8.0))) * 6.5;
      double var7 = Math.max(0.0, Math.max(this.targetBox.minZ + 8.0 - var2.z, var2.z - (this.targetBox.maxZ - 8.0)));
      return Math.max(0.0, Math.sqrt(var3 * var3 + var5 * var5 + var7 * var7) - this.reach) * 0.6;
   }

   private List<StandPlanner.Step> path(BlockPos blockPos) {
      ArrayList var2 = new ArrayList();
      long var3 = blockPos.asLong();

      while (var3 != this.start.asLong() && var3 != -9223372036854775808L) {
         var2.add(new StandPlanner.Step(BlockPos.fromLong(var3), this.moves.getOrDefault(var3, StandPlanner.Move.WALK)));
         var3 = this.parent.get(var3);
      }

      Collections.reverse(var2);
      return var2;
   }

   private StandPlanner.Target workFrom(BlockPos blockPos, long[] ls) {
      if ((this.level.getFluidState(blockPos).isEmpty() || this.level.getFluidState(blockPos).isStill()) && !Walker.nearLava(this.level, blockPos)) {
         double var3 = Walker.floorOr(this.level, blockPos);
         Vec3d var5 = new Vec3d(blockPos.getX() + 0.5, var3 + 1.62, blockPos.getZ() + 0.5);
         Box var6 = new Box(blockPos.getX() + 0.2, var3, blockPos.getZ() + 0.2, blockPos.getX() + 0.8, var3 + 1.8, blockPos.getZ() + 0.8);

         for (StandPlanner.Target var8 : this.targets) {
            BlockPos var9 = var8.pos();
            if (!(var9.toCenterPos().squaredDistanceTo(var5) > (this.reach + 0.9) * (this.reach + 0.9))
               && !(var9.toCenterPos().distanceTo(var5) > var8.within())
               && (!var6.intersects(new Box(var9)) || var8.use() == StandPlanner.Use.PLACE && var8.expected().getCollisionShape(this.level, var9).isEmpty())
               && this.placeableFrom(var5, var8, ls)) {
               return var8;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean placeableFrom(Vec3d vec3d, StandPlanner.Target target, long[] ls) {
      BlockPos var4 = target.pos();
      switch (target.use()) {
         case WATERLOG:
         case BREAK:
         case CLICK:
            if (var4.toCenterPos().distanceTo(vec3d) > this.reach) {
               return false;
            } else {
               VoxelShape var16 = this.level.getBlockState(var4).getOutlineShape(this.level, var4);
               if (var16.isEmpty()) {
                  return false;
               } else {
                  Box var17 = var16.getBoundingBox().offset(var4);

                  for (Direction var21 : Direction.values()) {
                     if (facesEye(var17, var21, vec3d) && this.seen(vec3d, var4, var17, var21, null, null)) {
                        return true;
                     }
                  }

                  return false;
               }
            }
         case NEAR:
            return var4.toCenterPos().distanceTo(vec3d) <= this.reach - 0.5 && this.support != null && this.support.possible(var4, vec3d);
         default:
            Direction var5 = target.use() == StandPlanner.Use.PLACE ? StateRules.lookFor(target.expected()) : null;
            Direction var6 = target.use() == StandPlanner.Use.PLACE ? StateRules.flatLookFor(target.expected()) : null;

            for (Direction var10 : Direction.values()) {
               BlockPos var12 = var4.offset(var10);
               BlockState var13 = this.level.getBlockState(var12);
               VoxelShape var11;
               if (!var13.isReplaceable() && !(var11 = var13.getOutlineShape(this.level, var12)).isEmpty()) {
                  Direction var14 = var10.getOpposite();
                  Box var15 = var11.getBoundingBox().offset(var12);
                  if (this.seen(vec3d, var12, var15, var14, var5, var6)
                     && (target.use() != StandPlanner.Use.PLACE || this.exact == null || this.exact.placeable(target.expected(), var4, vec3d))) {
                     return true;
                  }
               }
            }

            if (this.onPlanned(vec3d, target, ls)) {
               return true;
            } else if (target.use() != StandPlanner.Use.PLACE) {
               return false;
            } else if (StateRules.slabHalf(this.level.getBlockState(var4), target.expected())) {
               return this.exact != null && this.exact.placeable(target.expected(), var4, vec3d);
            } else {
               this.propped = this.support != null && this.support.behind(target.expected(), var4, vec3d);
               return this.propped;
            }
      }
   }

   private boolean onPlanned(Vec3d vec3d, StandPlanner.Target target, long[] ls) {
      if (ls.length == 0) {
         return false;
      } else if (target.use() != StandPlanner.Use.PLACE || StateRules.lookFor(target.expected()) == null && StateRules.flatLookFor(target.expected()) == null) {
         BlockPos var4 = target.pos();

         for (long var8 : ls) {
            BlockPos var12 = BlockPos.fromLong(var8);
            Direction var10;
            Box var11;
            if (var12.getManhattanDistance(var4) == 1
               && this.level.getBlockState(var12).isReplaceable()
               && facesEye(
                  var11 = new Box(var12),
                  var10 = Direction.getFacing((float)(var4.getX() - var12.getX()), (float)(var4.getY() - var12.getY()), (float)(var4.getZ() - var12.getZ())),
                  vec3d
               )) {
               for (double[] var16 : FACE_POINTS) {
                  Vec3d var18 = facePoint(var11, var10, var16[0], var16[1]);
                  if (!(var18.distanceTo(vec3d) > this.reach)
                     && this.level
                           .raycast(
                              new RaycastContext(
                                 vec3d, var18.subtract(var18.subtract(vec3d).normalize().multiply(0.05)), ShapeType.OUTLINE, FluidHandling.NONE, this.player
                              )
                           )
                           .getType()
                        == net.minecraft.util.hit.HitResult.Type.MISS) {
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

   private static long[] withLeaf(long[] ls, BlockPos blockPos, boolean bl) {
      long[] var3 = new long[Math.min(ls.length + 1, 10)];
      int var4 = 0;

      for (int var5 = Math.max(0, ls.length + 1 - 10); var5 < ls.length; var5++) {
         BlockPos var6 = BlockPos.fromLong(ls[var5]);
         if (!bl || var6.getX() != blockPos.getX() || var6.getZ() != blockPos.getZ() || var6.getY() >= blockPos.getY()) {
            var3[var4++] = ls[var5];
         }
      }

      var3[var4++] = blockPos.asLong();
      return var4 == var3.length ? var3 : Arrays.copyOf(var3, var4);
   }

   private boolean seen(Vec3d vec3d, BlockPos blockPos, Box box, Direction direction, Direction direction2, Direction direction3) {
      return this.seenFrom(vec3d, blockPos, box, direction, direction2, direction3, FACE_POINTS, 0.2)
         ? true
         : direction2 == Direction.UP
            && this.seenFrom(vec3d.subtract(0.0, 0.3500000000000001, 0.0), blockPos, box, direction, direction2, direction3, EDGE_POINTS, 0.05);
   }

   private boolean seenFrom(Vec3d vec3d, BlockPos blockPos, Box box, Direction direction, Direction direction2, Direction direction3, double[][] ds, double d) {
      if (!facesEye(box, direction, vec3d)) {
         return false;
      } else {
         for (double[] var13 : ds) {
            Vec3d var15 = facePoint(box, direction, var13[0], var13[1]);
            BlockHitResult var14;
            if (!(var15.distanceTo(vec3d) > this.reach)
               && (direction2 == null || Walker.dominant(var15.subtract(vec3d), direction2, d))
               && (direction3 == null || Walker.facingFlat(var15.subtract(vec3d), direction3))
               && (var14 = this.level
                        .raycast(
                           new RaycastContext(
                              vec3d, var15.add(var15.subtract(vec3d).normalize().multiply(0.05)), ShapeType.OUTLINE, FluidHandling.NONE, this.player
                           )
                        ))
                     .getType()
                  == net.minecraft.util.hit.HitResult.Type.BLOCK
               && var14.getBlockPos().equals(blockPos)
               && var14.getSide() == direction) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean facesEye(Box box, Direction direction, Vec3d vec3d) {
      double var3 = 0.05;

      return switch (direction) {
         case UP -> vec3d.y > box.maxY + var3;
         case DOWN -> vec3d.y < box.minY - var3;
         case NORTH -> vec3d.z < box.minZ - var3;
         case SOUTH -> vec3d.z > box.maxZ + var3;
         case WEST -> vec3d.x < box.minX - var3;
         case EAST -> vec3d.x > box.maxX + var3;
      };
   }

   private static Vec3d facePoint(Box box, Direction direction, double d, double e) {
      double var6 = box.minX + (box.maxX - box.minX) * d;
      double var8 = box.minY + (box.maxY - box.minY) * e;

      return switch (direction) {
         case UP -> new Vec3d(var6, box.maxY, box.minZ + (box.maxZ - box.minZ) * e);
         case DOWN -> new Vec3d(var6, box.minY, box.minZ + (box.maxZ - box.minZ) * e);
         case NORTH -> new Vec3d(var6, var8, box.minZ);
         case SOUTH -> new Vec3d(var6, var8, box.maxZ);
         case WEST -> new Vec3d(box.minX, var8, box.minZ + (box.maxZ - box.minZ) * d);
         case EAST -> new Vec3d(box.maxX, var8, box.minZ + (box.maxZ - box.minZ) * d);
      };
   }

   @Environment(EnvType.CLIENT)
   interface Exact {
      boolean placeable(BlockState blockState, BlockPos blockPos, Vec3d vec3d);
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
      boolean possible(BlockPos blockPos, boolean bl);
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
      boolean possible(BlockPos blockPos, Vec3d vec3d);

      boolean behind(BlockState blockState, BlockPos blockPos, Vec3d vec3d);
   }

   @Environment(EnvType.CLIENT)
   record Target(BlockPos pos, BlockState expected, StandPlanner.Use use, double within) {
      Target(BlockPos blockPos, BlockState blockState, StandPlanner.Use use) {
         this(blockPos, blockState, use, 1.7976931348623157E308);
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
