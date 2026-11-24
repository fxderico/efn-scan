package net.efn.client.easyplace.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.efn.client.easyplace.compat.BaritoneCompat;
import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.shape.VoxelShape;

@Environment(EnvType.CLIENT)
final class Walker {
   private static final int STILL_LIMIT = 60;
   private static final int GRACE = 40;
   private static final int TIME_LIMIT = 600;
   private static final double EYE = 1.62;
   private static final double HEIGHT = 1.8;
   private static final double JUMP = 1.2;
   private static final float SWIM_DEPTH = 0.4F;
   private static final double FULL = 0.999;
   private static final double STEP_UP = 0.55;
   private static final double MAX_DROP = 3.0;
   private static final double LEAP_ROOM = 2.6;
   private BlockPos goal;
   private long started;
   private long lastMove;
   private Vec3d lastPos;

   Walker() {
   }

   boolean active() {
      return this.goal != null;
   }

   BlockPos goal() {
      return this.goal;
   }

   void go(ClientPlayerEntity clientPlayerEntity, BlockPos blockPos, long l) {
      this.goal = blockPos;
      this.started = l;
      this.lastMove = l;
      this.lastPos = clientPlayerEntity.getEntityPos();
      BaritoneCompat.goTo(blockPos);
   }

   Walker.Result tick(ClientPlayerEntity clientPlayerEntity, long l) {
      if (this.goal == null) {
         return Walker.Result.FAILED;
      } else {
         Vec3d var6 = clientPlayerEntity.getEntityPos();
         double var7 = var6.x - (this.goal.getX() + 0.5);
         double var4;
         if (var7 * var7 + (var4 = var6.z - (this.goal.getZ() + 0.5)) * var4 < 0.36
            && Math.abs(var6.y - this.goal.getY()) < 0.6
            && clientPlayerEntity.isOnGround()) {
            this.goal = null;
            return Walker.Result.ARRIVED;
         } else {
            if (var6.squaredDistanceTo(this.lastPos) > 0.0025) {
               this.lastPos = var6;
               this.lastMove = l;
            }

            if ((l - this.started <= 40L || l - this.lastMove <= 60L) && l - this.started <= 600L) {
               return Walker.Result.RUNNING;
            } else {
               this.stop();
               return Walker.Result.FAILED;
            }
         }
      }
   }

   void stop() {
      if (this.goal != null) {
         BaritoneCompat.cancel();
      }

      this.goal = null;
   }

   static BlockPos standFor(
      ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos, BlockState blockState, Predicate<BlockPos> predicate
   ) {
      return standFor(clientWorld, clientPlayerEntity, blockPos, blockState, predicate, clientPlayerEntity.getBlockInteractionRange() - 0.7);
   }

   static BlockPos standFor(
      ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos, BlockState blockState, Predicate<BlockPos> predicate, double d
   ) {
      Direction var7 = StateRules.lookFor(blockState);
      Direction var8 = StateRules.flatLookFor(blockState);
      Vec3d var9 = clientPlayerEntity.getEntityPos();
      int var10 = clientPlayerEntity.getBlockPos().getY();
      BlockPos var11 = null;
      double var12 = 1.7976931348623157E308;

      for (int var14 = -1; var14 <= 2; var14++) {
         for (int var15 = -3; var15 <= 3; var15++) {
            for (int var16 = -3; var16 <= 3; var16++) {
               BlockPos var18 = blockPos.add(var15, var14, var16);
               if (!var18.equals(blockPos) && !var18.up().equals(blockPos) && !predicate.test(var18) && standable(clientWorld, var18)) {
                  Vec3d var19 = new Vec3d(var18.getX() + 0.5, floor(clientWorld, var18) + 1.62, var18.getZ() + 0.5);
                  Vec3d var20 = blockPos.toCenterPos().subtract(var19);
                  if (!(var20.length() > d) && (var7 == null || dominant(var20, var7)) && (var8 == null || facingFlat(var20, var8))) {
                     double var21 = Math.hypot(var9.x - (var18.getX() + 0.5), var9.z - (var18.getZ() + 0.5));
                     int var17;
                     double var23 = var21 + ((var17 = var18.getY() - var10) > 0 ? var17 * 4.0 : -var17 * 0.3) + Math.abs(var14) * 0.2;
                     if (var23 < var12) {
                        var12 = var23;
                        var11 = var18;
                     }
                  }
               }
            }
         }
      }

      return var11;
   }

   static BlockPos stepToward(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos, Predicate<BlockPos> predicate) {
      if (!clientPlayerEntity.isOnGround()) {
         return null;
      } else {
         BlockPos var4 = clientPlayerEntity.getBlockPos();
         Vec3d var5 = blockPos.toCenterPos();
         double var6 = clientPlayerEntity.getEyePos().distanceTo(var5);
         BlockPos var8 = null;
         double var9 = var6 - 0.3;

         for (BlockPos var12 : steps(clientWorld, var4)) {
            double var13;
            if (!predicate.test(var12) && (var13 = new Vec3d(var12.getX() + 0.5, floor(clientWorld, var12) + 1.62, var12.getZ() + 0.5).distanceTo(var5)) < var9
               )
             {
               var9 = var13;
               var8 = var12;
            }
         }

         return var8;
      }
   }

   static List<BlockPos> steps(ClientWorld clientWorld, BlockPos blockPos) {
      return steps(clientWorld, blockPos, true);
   }

   private static List<BlockPos> steps(ClientWorld clientWorld, BlockPos blockPos, boolean bl) {
      ArrayList var3 = new ArrayList();
      double var4 = floorOr(clientWorld, blockPos);

      for (Direction var7 : Type.HORIZONTAL) {
         BlockPos var8 = blockPos.offset(var7);

         for (int var9 = 1; var9 >= -3; var9--) {
            BlockPos var11 = var8.up(var9);
            double var12 = floor(clientWorld, var11);
            if (!Double.isNaN(var12)) {
               boolean var14 = var4 - var12 > 0.6 && !clientWorld.getFluidState(var11).isEmpty();
               boolean var10 = bl && var4 - var12 > 0.01 && steps(clientWorld, var11, false).isEmpty();
               if (var12 - var4 <= 1.2
                  && var4 - var12 <= 3.0
                  && !var14
                  && !var10
                  && !nearLava(clientWorld, var11)
                  && passage(clientWorld, blockPos, var4, var11, var12)) {
                  var3.add(var11);
               }
               break;
            }
         }
      }

      return var3;
   }

   static String explain(ClientWorld clientWorld, BlockPos blockPos) {
      StringBuilder var2 = new StringBuilder("from " + blockPos.toShortString() + " floor " + floor(clientWorld, blockPos) + ":");
      double var3 = floorOr(clientWorld, blockPos);

      for (Direction var6 : Type.HORIZONTAL) {
         BlockPos var7 = blockPos.offset(var6);
         var2.append(" ").append(var6).append("[");

         for (int var8 = 1; var8 >= -3; var8--) {
            BlockPos var9 = var7.up(var8);
            double var10 = floor(clientWorld, var9);
            var2.append(var8)
               .append(":")
               .append(
                  Double.isNaN(var10)
                     ? "-"
                     : String.format("%.2f%s", var10, var10 - var3 <= 1.2 && passage(clientWorld, blockPos, var3, var9, var10) ? "ok" : "x")
               )
               .append(" ");
         }

         var2.append("]");
      }

      return var2.toString();
   }

   static BlockPos fall(ClientWorld clientWorld, BlockPos blockPos, Direction direction, double d) {
      double var5 = floorOr(clientWorld, blockPos);
      BlockPos var7 = blockPos.offset(direction);
      if (Double.isNaN(floor(clientWorld, var7)) && roomAt(clientWorld, var7)) {
         for (int var8 = 1; var8 <= Math.ceil(d) + 1.0; var8++) {
            BlockPos var9 = var7.down(var8);
            double var10 = floor(clientWorld, var9);
            if (!clientWorld.getFluidState(var9).isEmpty()) {
               return null;
            }

            if (!Double.isNaN(var10)) {
               if (!(var5 - var10 <= 3.0) && !(var5 - var10 > d) && !nearLava(clientWorld, var9) && passage(clientWorld, blockPos, var5, var9, var10)) {
                  return var9;
               }

               return null;
            }

            if (!clientWorld.getBlockState(var9).getCollisionShape(clientWorld, var9).isEmpty()) {
               return null;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   static double floor(ClientWorld clientWorld, BlockPos blockPos) {
      if (!clientWorld.isChunkLoaded(blockPos)) {
         return 0.0 / 0.0;
      } else if (clientWorld.getFluidState(blockPos).isIn(FluidTags.WATER) && clientWorld.getFluidState(blockPos).getHeight(clientWorld, blockPos) > 0.4F) {
         return 0.0 / 0.0;
      } else {
         double var6 = topUnderFoot(clientWorld, blockPos);
         double var4;
         if (!Double.isNaN(var6)) {
            if (var6 > 0.999) {
               return 0.0 / 0.0;
            }

            var4 = blockPos.getY() + var6;
         } else {
            if (clientWorld.getFluidState(blockPos).isIn(FluidTags.LAVA) || clientWorld.getBlockState(blockPos).isOf(Blocks.BUBBLE_COLUMN)) {
               return 0.0 / 0.0;
            }

            BlockPos var8 = blockPos.down();
            double var9 = topUnderFoot(clientWorld, var8);
            if (Double.isNaN(var9) || var9 <= 0.999) {
               return 0.0 / 0.0;
            }

            var4 = var8.getY() + var9;
         }

         if (!clientWorld.getFluidState(blockPos.up()).isEmpty()) {
            return 0.0 / 0.0;
         } else {
            double var11 = blockPos.getX();
            double var2;
            if (!clientWorld.isSpaceEmpty(new Box(var11 + 0.2, var4 + 0.01, (var2 = blockPos.getZ()) + 0.2, var11 + 0.8, var4 + 1.8, var2 + 0.8))) {
               return 0.0 / 0.0;
            } else {
               return clientWorld.isSpaceEmpty(new Box(var11 + 0.2, var4 - 0.05, var2 + 0.2, var11 + 0.8, var4 - 0.001, var2 + 0.8)) ? 0.0 / 0.0 : var4;
            }
         }
      }
   }

   static boolean roomAt(ClientWorld clientWorld, BlockPos blockPos) {
      if (clientWorld.isChunkLoaded(blockPos) && clientWorld.getFluidState(blockPos).isEmpty() && clientWorld.getFluidState(blockPos.up()).isEmpty()) {
         double var2 = blockPos.getX();
         double var4 = blockPos.getY();
         double var6 = blockPos.getZ();
         return clientWorld.isSpaceEmpty(new Box(var2 + 0.2, var4 + 0.01, var6 + 0.2, var2 + 0.8, var4 + 1.8, var6 + 0.8));
      } else {
         return false;
      }
   }

   static boolean leap(ClientWorld clientWorld, BlockPos blockPos, double d, Direction direction) {
      return leap(clientWorld, blockPos, d, direction, 1);
   }

   static boolean leap(ClientWorld clientWorld, BlockPos blockPos, double d, Direction direction, int i) {
      BlockPos var6 = blockPos.offset(direction, i + 1);
      double var7 = floor(clientWorld, var6);
      if (!Double.isNaN(var7) && !(Math.abs(var7 - d) > 0.1)) {
         for (int var9 = 1; var9 <= i; var9++) {
            BlockPos var10 = blockPos.offset(direction, var9);
            if (!Double.isNaN(floor(clientWorld, var10)) || !clientWorld.getFluidState(var10).isEmpty()) {
               return false;
            }

            if (!safeFall(clientWorld, var10)) {
               return false;
            }

            for (Direction var14 : new Direction[]{direction.rotateYClockwise(), direction.rotateYCounterclockwise()}) {
               BlockPos var15 = var10.offset(var14);
               if (Double.isNaN(floor(clientWorld, var15)) && !safeFall(clientWorld, var15)) {
                  return false;
               }
            }
         }

         BlockPos var16 = var6.offset(direction);
         if (Double.isNaN(floor(clientWorld, var16)) && !safeFall(clientWorld, var16)) {
            return false;
         } else {
            Box var17 = column(blockPos, d, d).union(column(var6, d, d)).withMaxY(d + 2.6);
            return clientWorld.isSpaceEmpty(var17);
         }
      } else {
         return false;
      }
   }

   private static boolean safeFall(ClientWorld clientWorld, BlockPos blockPos) {
      for (int var2 = 1; var2 <= 3; var2++) {
         BlockPos var3 = blockPos.down(var2);
         if (clientWorld.getFluidState(var3).isIn(FluidTags.LAVA) || clientWorld.getBlockState(var3).isIn(BlockTags.FIRE)) {
            return false;
         }

         if (!clientWorld.getBlockState(var3).getCollisionShape(clientWorld, var3).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private static double topUnderFoot(ClientWorld clientWorld, BlockPos blockPos) {
      VoxelShape var2 = clientWorld.getBlockState(blockPos).getCollisionShape(clientWorld, blockPos);
      if (var2.isEmpty()) {
         return 0.0 / 0.0;
      } else {
         double var3 = 0.0 / 0.0;

         for (Box var6 : var2.getBoundingBoxes()) {
            if (!(var6.maxX <= 0.2) && !(var6.minX >= 0.8) && !(var6.maxZ <= 0.2) && !(var6.minZ >= 0.8)) {
               var3 = Double.isNaN(var3) ? var6.maxY : Math.max(var3, var6.maxY);
            }
         }

         return var3;
      }
   }

   static boolean risky(ClientWorld clientWorld, BlockPos blockPos) {
      for (int var2 = -1; var2 <= 1; var2++) {
         for (int var3 = -1; var3 <= 1; var3++) {
            BlockPos var4;
            if ((var2 != 0 || var3 != 0) && Double.isNaN(floor(clientWorld, var4 = blockPos.add(var2, 0, var3)))) {
               boolean var5 = var2 == 0 || var3 == 0;

               for (int var6 = 0; var6 <= 24; var6++) {
                  BlockPos var8 = var4.down(var6);
                  if (clientWorld.getFluidState(var8).isIn(FluidTags.LAVA) || clientWorld.getBlockState(var8).isIn(BlockTags.FIRE)) {
                     if (var6 <= 4) {
                        return true;
                     }
                     break;
                  }

                  boolean var7 = var6 > (var5 ? 4 : 8);
                  if (clientWorld.getFluidState(var8).isIn(FluidTags.WATER)) {
                     if (var7) {
                        return true;
                     }
                     break;
                  }

                  if (!clientWorld.getBlockState(var8).getCollisionShape(clientWorld, var8).isEmpty()) {
                     if (var7) {
                        return true;
                     }
                     break;
                  }

                  if (var6 == 24) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   static boolean icy(ClientWorld clientWorld, BlockPos blockPos) {
      return clientWorld.getBlockState(blockPos.down()).isIn(BlockTags.ICE) || clientWorld.getBlockState(blockPos).isIn(BlockTags.ICE);
   }

   static int dropAt(ClientWorld clientWorld, BlockPos blockPos) {
      if (!clientWorld.isChunkLoaded(blockPos)) {
         return 0;
      } else if (clientWorld.getFluidState(blockPos).isIn(FluidTags.LAVA)) {
         return 24;
      } else if (!clientWorld.getBlockState(blockPos).getCollisionShape(clientWorld, blockPos).isEmpty()) {
         return 0;
      } else {
         int var2 = 0;

         while (var2 < 24) {
            BlockPos var3 = blockPos.down(var2 + 1);
            if (!clientWorld.getFluidState(var3).isIn(FluidTags.LAVA) && !clientWorld.getBlockState(var3).isIn(BlockTags.FIRE)) {
               if (clientWorld.getFluidState(var3).isEmpty() && clientWorld.getBlockState(var3).getCollisionShape(clientWorld, var3).isEmpty()) {
                  var2++;
                  continue;
               }

               return var2;
            }

            return 24;
         }

         return 24;
      }
   }

   static boolean nearLava(ClientWorld clientWorld, BlockPos blockPos) {
      for (BlockPos var5 : new BlockPos[]{blockPos, blockPos.up(), blockPos.down(), blockPos.north(), blockPos.south(), blockPos.east(), blockPos.west()}) {
         if (clientWorld.getFluidState(var5).isIn(FluidTags.LAVA)) {
            return true;
         }
      }

      return false;
   }

   static double floorOr(ClientWorld clientWorld, BlockPos blockPos) {
      double var2 = floor(clientWorld, blockPos);
      return Double.isNaN(var2) ? blockPos.getY() : var2;
   }

   private static boolean passage(ClientWorld clientWorld, BlockPos blockPos, double d, BlockPos blockPos2, double e) {
      double var7 = Math.max(d, e);
      Box var9 = column(blockPos, d, var7);
      Box var10 = column(blockPos2, e, var7);
      Box var11 = new Box(
         Math.min(var9.minX, var10.minX),
         var7 + 0.55,
         Math.min(var9.minZ, var10.minZ),
         Math.max(var9.maxX, var10.maxX),
         var7 + 1.8,
         Math.max(var9.maxZ, var10.maxZ)
      );
      return clientWorld.isSpaceEmpty(var9) && clientWorld.isSpaceEmpty(var10) && clientWorld.isSpaceEmpty(var11);
   }

   private static Box column(BlockPos blockPos, double d, double e) {
      return new Box(blockPos.getX() + 0.2, d + 0.01, blockPos.getZ() + 0.2, blockPos.getX() + 0.8, e + 1.8, blockPos.getZ() + 0.8);
   }

   static BlockPos stepAside(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, Predicate<BlockPos> predicate) {
      BlockPos var3 = clientPlayerEntity.getBlockPos();
      BlockPos var4 = null;
      double var5 = 1.7976931348623157E308;

      for (int var7 = -1; var7 <= 1; var7++) {
         for (int var8 = -2; var8 <= 2; var8++) {
            for (int var9 = -2; var9 <= 2; var9++) {
               double var10;
               BlockPos var12;
               if ((var8 != 0 || var9 != 0)
                  && !predicate.test(var12 = var3.add(var8, var7, var9))
                  && standable(clientWorld, var12)
                  && (var10 = Math.abs(var8) + Math.abs(var9) + Math.abs(var7) * 1.5) < var5) {
                  var5 = var10;
                  var4 = var12;
               }
            }
         }
      }

      return var4;
   }

   static boolean standable(ClientWorld clientWorld, BlockPos blockPos) {
      return !Double.isNaN(floor(clientWorld, blockPos));
   }

   static boolean facingFlat(Vec3d vec3d, Direction direction) {
      double var4 = vec3d.x * direction.getOffsetX() + vec3d.z * direction.getOffsetZ();
      return var4 > Math.abs(vec3d.x * direction.getOffsetZ()) + Math.abs(vec3d.z * direction.getOffsetX()) + 0.1;
   }

   static boolean dominant(Vec3d vec3d, Direction direction) {
      return dominant(vec3d, direction, 0.2);
   }

   static boolean dominant(Vec3d vec3d, Direction direction, double d) {
      double var4 = vec3d.x * direction.getOffsetX() + vec3d.y * direction.getOffsetY() + vec3d.z * direction.getOffsetZ();
      if (var4 <= 0.0) {
         return false;
      } else {
         double var6 = Math.abs(vec3d.x);
         double var8 = Math.abs(vec3d.y);
         double var10 = Math.abs(vec3d.z);

         return switch (direction.getAxis()) {
            case X -> var6 > var8 + d && var6 > var10 + d;
            case Y -> var8 > var6 + d && var8 > var10 + d;
            case Z -> var10 > var6 + d && var10 > var8 + d;
         };
      }
   }

   @Environment(EnvType.CLIENT)
   static enum Result {
      RUNNING,
      ARRIVED,
      FAILED;

      private Result() {
      }
   }
}
