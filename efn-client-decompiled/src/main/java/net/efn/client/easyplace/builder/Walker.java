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

   void go(ClientPlayerEntity var1, BlockPos var2, long var3) {
      this.goal = var2;
      this.started = var3;
      this.lastMove = var3;
      this.lastPos = var1.method_73189();
      BaritoneCompat.goTo(var2);
   }

   Walker.Result tick(ClientPlayerEntity var1, long var2) {
      if (this.goal == null) {
         return Walker.Result.FAILED;
      } else {
         Vec3d var6 = var1.method_73189();
         double var7 = var6.field_1352 - (this.goal.method_10263() + 0.5);
         double var4;
         if (var7 * var7 + (var4 = var6.field_1350 - (this.goal.method_10260() + 0.5)) * var4 < 0.36
            && Math.abs(var6.field_1351 - this.goal.method_10264()) < 0.6
            && var1.method_24828()) {
            this.goal = null;
            return Walker.Result.ARRIVED;
         } else {
            if (var6.method_1025(this.lastPos) > 0.0025) {
               this.lastPos = var6;
               this.lastMove = var2;
            }

            if ((var2 - this.started <= 40L || var2 - this.lastMove <= 60L) && var2 - this.started <= 600L) {
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

   static BlockPos standFor(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2, BlockState var3, Predicate<BlockPos> var4) {
      return standFor(var0, var1, var2, var3, var4, var1.method_55754() - 0.7);
   }

   static BlockPos standFor(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2, BlockState var3, Predicate<BlockPos> var4, double var5) {
      Direction var7 = StateRules.lookFor(var3);
      Direction var8 = StateRules.flatLookFor(var3);
      Vec3d var9 = var1.method_73189();
      int var10 = var1.method_24515().method_10264();
      BlockPos var11 = null;
      double var12 = 1.7976931348623157E308;

      for (int var14 = -1; var14 <= 2; var14++) {
         for (int var15 = -3; var15 <= 3; var15++) {
            for (int var16 = -3; var16 <= 3; var16++) {
               BlockPos var18 = var2.method_10069(var15, var14, var16);
               if (!var18.equals(var2) && !var18.method_10084().equals(var2) && !var4.test(var18) && standable(var0, var18)) {
                  Vec3d var19 = new Vec3d(var18.method_10263() + 0.5, floor(var0, var18) + 1.62, var18.method_10260() + 0.5);
                  Vec3d var20 = var2.method_46558().method_1020(var19);
                  if (!(var20.method_1033() > var5) && (var7 == null || dominant(var20, var7)) && (var8 == null || facingFlat(var20, var8))) {
                     double var21 = Math.hypot(var9.field_1352 - (var18.method_10263() + 0.5), var9.field_1350 - (var18.method_10260() + 0.5));
                     int var17;
                     double var23 = var21 + ((var17 = var18.method_10264() - var10) > 0 ? var17 * 4.0 : -var17 * 0.3) + Math.abs(var14) * 0.2;
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

   static BlockPos stepToward(ClientWorld var0, ClientPlayerEntity var1, BlockPos var2, Predicate<BlockPos> var3) {
      if (!var1.method_24828()) {
         return null;
      } else {
         BlockPos var4 = var1.method_24515();
         Vec3d var5 = var2.method_46558();
         double var6 = var1.method_33571().method_1022(var5);
         BlockPos var8 = null;
         double var9 = var6 - 0.3;

         for (BlockPos var12 : steps(var0, var4)) {
            double var13;
            if (!var3.test(var12)
               && (var13 = new Vec3d(var12.method_10263() + 0.5, floor(var0, var12) + 1.62, var12.method_10260() + 0.5).method_1022(var5)) < var9) {
               var9 = var13;
               var8 = var12;
            }
         }

         return var8;
      }
   }

   static List<BlockPos> steps(ClientWorld var0, BlockPos var1) {
      return steps(var0, var1, true);
   }

   private static List<BlockPos> steps(ClientWorld var0, BlockPos var1, boolean var2) {
      ArrayList var3 = new ArrayList();
      double var4 = floorOr(var0, var1);

      for (Direction var7 : Type.field_11062) {
         BlockPos var8 = var1.method_10093(var7);

         for (int var9 = 1; var9 >= -3; var9--) {
            BlockPos var11 = var8.method_10086(var9);
            double var12 = floor(var0, var11);
            if (!Double.isNaN(var12)) {
               boolean var14 = var4 - var12 > 0.6 && !var0.method_8316(var11).method_15769();
               boolean var10 = var2 && var4 - var12 > 0.01 && steps(var0, var11, false).isEmpty();
               if (var12 - var4 <= 1.2 && var4 - var12 <= 3.0 && !var14 && !var10 && !nearLava(var0, var11) && passage(var0, var1, var4, var11, var12)) {
                  var3.add(var11);
               }
               break;
            }
         }
      }

      return var3;
   }

   static String explain(ClientWorld var0, BlockPos var1) {
      StringBuilder var2 = new StringBuilder("from " + var1.method_23854() + " floor " + floor(var0, var1) + ":");
      double var3 = floorOr(var0, var1);

      for (Direction var6 : Type.field_11062) {
         BlockPos var7 = var1.method_10093(var6);
         var2.append(" ").append(var6).append("[");

         for (int var8 = 1; var8 >= -3; var8--) {
            BlockPos var9 = var7.method_10086(var8);
            double var10 = floor(var0, var9);
            var2.append(var8)
               .append(":")
               .append(Double.isNaN(var10) ? "-" : String.format("%.2f%s", var10, var10 - var3 <= 1.2 && passage(var0, var1, var3, var9, var10) ? "ok" : "x"))
               .append(" ");
         }

         var2.append("]");
      }

      return var2.toString();
   }

   static BlockPos fall(ClientWorld var0, BlockPos var1, Direction var2, double var3) {
      double var5 = floorOr(var0, var1);
      BlockPos var7 = var1.method_10093(var2);
      if (Double.isNaN(floor(var0, var7)) && roomAt(var0, var7)) {
         for (int var8 = 1; var8 <= Math.ceil(var3) + 1.0; var8++) {
            BlockPos var9 = var7.method_10087(var8);
            double var10 = floor(var0, var9);
            if (!var0.method_8316(var9).method_15769()) {
               return null;
            }

            if (!Double.isNaN(var10)) {
               if (!(var5 - var10 <= 3.0) && !(var5 - var10 > var3) && !nearLava(var0, var9) && passage(var0, var1, var5, var9, var10)) {
                  return var9;
               }

               return null;
            }

            if (!var0.method_8320(var9).method_26220(var0, var9).method_1110()) {
               return null;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   static double floor(ClientWorld var0, BlockPos var1) {
      if (!var0.method_22340(var1)) {
         return 0.0 / 0.0;
      } else if (var0.method_8316(var1).method_15767(FluidTags.field_15517) && var0.method_8316(var1).method_15763(var0, var1) > 0.4F) {
         return 0.0 / 0.0;
      } else {
         double var6 = topUnderFoot(var0, var1);
         double var4;
         if (!Double.isNaN(var6)) {
            if (var6 > 0.999) {
               return 0.0 / 0.0;
            }

            var4 = var1.method_10264() + var6;
         } else {
            if (var0.method_8316(var1).method_15767(FluidTags.field_15518) || var0.method_8320(var1).method_27852(Blocks.field_10422)) {
               return 0.0 / 0.0;
            }

            BlockPos var8 = var1.method_10074();
            double var9 = topUnderFoot(var0, var8);
            if (Double.isNaN(var9) || var9 <= 0.999) {
               return 0.0 / 0.0;
            }

            var4 = var8.method_10264() + var9;
         }

         if (!var0.method_8316(var1.method_10084()).method_15769()) {
            return 0.0 / 0.0;
         } else {
            double var11 = var1.method_10263();
            double var2;
            if (!var0.method_18026(new Box(var11 + 0.2, var4 + 0.01, (var2 = var1.method_10260()) + 0.2, var11 + 0.8, var4 + 1.8, var2 + 0.8))) {
               return 0.0 / 0.0;
            } else {
               return var0.method_18026(new Box(var11 + 0.2, var4 - 0.05, var2 + 0.2, var11 + 0.8, var4 - 0.001, var2 + 0.8)) ? 0.0 / 0.0 : var4;
            }
         }
      }
   }

   static boolean roomAt(ClientWorld var0, BlockPos var1) {
      if (var0.method_22340(var1) && var0.method_8316(var1).method_15769() && var0.method_8316(var1.method_10084()).method_15769()) {
         double var2 = var1.method_10263();
         double var4 = var1.method_10264();
         double var6 = var1.method_10260();
         return var0.method_18026(new Box(var2 + 0.2, var4 + 0.01, var6 + 0.2, var2 + 0.8, var4 + 1.8, var6 + 0.8));
      } else {
         return false;
      }
   }

   static boolean leap(ClientWorld var0, BlockPos var1, double var2, Direction var4) {
      return leap(var0, var1, var2, var4, 1);
   }

   static boolean leap(ClientWorld var0, BlockPos var1, double var2, Direction var4, int var5) {
      BlockPos var6 = var1.method_10079(var4, var5 + 1);
      double var7 = floor(var0, var6);
      if (!Double.isNaN(var7) && !(Math.abs(var7 - var2) > 0.1)) {
         for (int var9 = 1; var9 <= var5; var9++) {
            BlockPos var10 = var1.method_10079(var4, var9);
            if (!Double.isNaN(floor(var0, var10)) || !var0.method_8316(var10).method_15769()) {
               return false;
            }

            if (!safeFall(var0, var10)) {
               return false;
            }

            for (Direction var14 : new Direction[]{var4.method_10170(), var4.method_10160()}) {
               BlockPos var15 = var10.method_10093(var14);
               if (Double.isNaN(floor(var0, var15)) && !safeFall(var0, var15)) {
                  return false;
               }
            }
         }

         BlockPos var16 = var6.method_10093(var4);
         if (Double.isNaN(floor(var0, var16)) && !safeFall(var0, var16)) {
            return false;
         } else {
            Box var17 = column(var1, var2, var2).method_991(column(var6, var2, var2)).method_35578(var2 + 2.6);
            return var0.method_18026(var17);
         }
      } else {
         return false;
      }
   }

   private static boolean safeFall(ClientWorld var0, BlockPos var1) {
      for (int var2 = 1; var2 <= 3; var2++) {
         BlockPos var3 = var1.method_10087(var2);
         if (var0.method_8316(var3).method_15767(FluidTags.field_15518) || var0.method_8320(var3).method_26164(BlockTags.field_21952)) {
            return false;
         }

         if (!var0.method_8320(var3).method_26220(var0, var3).method_1110()) {
            return true;
         }
      }

      return false;
   }

   private static double topUnderFoot(ClientWorld var0, BlockPos var1) {
      VoxelShape var2 = var0.method_8320(var1).method_26220(var0, var1);
      if (var2.method_1110()) {
         return 0.0 / 0.0;
      } else {
         double var3 = 0.0 / 0.0;

         for (Box var6 : var2.method_1090()) {
            if (!(var6.field_1320 <= 0.2) && !(var6.field_1323 >= 0.8) && !(var6.field_1324 <= 0.2) && !(var6.field_1321 >= 0.8)) {
               var3 = Double.isNaN(var3) ? var6.field_1325 : Math.max(var3, var6.field_1325);
            }
         }

         return var3;
      }
   }

   static boolean risky(ClientWorld var0, BlockPos var1) {
      for (int var2 = -1; var2 <= 1; var2++) {
         for (int var3 = -1; var3 <= 1; var3++) {
            BlockPos var4;
            if ((var2 != 0 || var3 != 0) && Double.isNaN(floor(var0, var4 = var1.method_10069(var2, 0, var3)))) {
               boolean var5 = var2 == 0 || var3 == 0;

               for (int var6 = 0; var6 <= 24; var6++) {
                  BlockPos var8 = var4.method_10087(var6);
                  if (var0.method_8316(var8).method_15767(FluidTags.field_15518) || var0.method_8320(var8).method_26164(BlockTags.field_21952)) {
                     if (var6 <= 4) {
                        return true;
                     }
                     break;
                  }

                  boolean var7 = var6 > (var5 ? 4 : 8);
                  if (var0.method_8316(var8).method_15767(FluidTags.field_15517)) {
                     if (var7) {
                        return true;
                     }
                     break;
                  }

                  if (!var0.method_8320(var8).method_26220(var0, var8).method_1110()) {
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

   static boolean icy(ClientWorld var0, BlockPos var1) {
      return var0.method_8320(var1.method_10074()).method_26164(BlockTags.field_15467) || var0.method_8320(var1).method_26164(BlockTags.field_15467);
   }

   static int dropAt(ClientWorld var0, BlockPos var1) {
      if (!var0.method_22340(var1)) {
         return 0;
      } else if (var0.method_8316(var1).method_15767(FluidTags.field_15518)) {
         return 24;
      } else if (!var0.method_8320(var1).method_26220(var0, var1).method_1110()) {
         return 0;
      } else {
         int var2 = 0;

         while (var2 < 24) {
            BlockPos var3 = var1.method_10087(var2 + 1);
            if (!var0.method_8316(var3).method_15767(FluidTags.field_15518) && !var0.method_8320(var3).method_26164(BlockTags.field_21952)) {
               if (var0.method_8316(var3).method_15769() && var0.method_8320(var3).method_26220(var0, var3).method_1110()) {
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

   static boolean nearLava(ClientWorld var0, BlockPos var1) {
      for (BlockPos var5 : new BlockPos[]{
         var1, var1.method_10084(), var1.method_10074(), var1.method_10095(), var1.method_10072(), var1.method_10078(), var1.method_10067()
      }) {
         if (var0.method_8316(var5).method_15767(FluidTags.field_15518)) {
            return true;
         }
      }

      return false;
   }

   static double floorOr(ClientWorld var0, BlockPos var1) {
      double var2 = floor(var0, var1);
      return Double.isNaN(var2) ? var1.method_10264() : var2;
   }

   private static boolean passage(ClientWorld var0, BlockPos var1, double var2, BlockPos var4, double var5) {
      double var7 = Math.max(var2, var5);
      Box var9 = column(var1, var2, var7);
      Box var10 = column(var4, var5, var7);
      Box var11 = new Box(
         Math.min(var9.field_1323, var10.field_1323),
         var7 + 0.55,
         Math.min(var9.field_1321, var10.field_1321),
         Math.max(var9.field_1320, var10.field_1320),
         var7 + 1.8,
         Math.max(var9.field_1324, var10.field_1324)
      );
      return var0.method_18026(var9) && var0.method_18026(var10) && var0.method_18026(var11);
   }

   private static Box column(BlockPos var0, double var1, double var3) {
      return new Box(var0.method_10263() + 0.2, var1 + 0.01, var0.method_10260() + 0.2, var0.method_10263() + 0.8, var3 + 1.8, var0.method_10260() + 0.8);
   }

   static BlockPos stepAside(ClientWorld var0, ClientPlayerEntity var1, Predicate<BlockPos> var2) {
      BlockPos var3 = var1.method_24515();
      BlockPos var4 = null;
      double var5 = 1.7976931348623157E308;

      for (int var7 = -1; var7 <= 1; var7++) {
         for (int var8 = -2; var8 <= 2; var8++) {
            for (int var9 = -2; var9 <= 2; var9++) {
               double var10;
               BlockPos var12;
               if ((var8 != 0 || var9 != 0)
                  && !var2.test(var12 = var3.method_10069(var8, var7, var9))
                  && standable(var0, var12)
                  && (var10 = Math.abs(var8) + Math.abs(var9) + Math.abs(var7) * 1.5) < var5) {
                  var5 = var10;
                  var4 = var12;
               }
            }
         }
      }

      return var4;
   }

   static boolean standable(ClientWorld var0, BlockPos var1) {
      return !Double.isNaN(floor(var0, var1));
   }

   static boolean facingFlat(Vec3d var0, Direction var1) {
      double var4 = var0.field_1352 * var1.method_10148() + var0.field_1350 * var1.method_10165();
      return var4 > Math.abs(var0.field_1352 * var1.method_10165()) + Math.abs(var0.field_1350 * var1.method_10148()) + 0.1;
   }

   static boolean dominant(Vec3d var0, Direction var1) {
      return dominant(var0, var1, 0.2);
   }

   static boolean dominant(Vec3d var0, Direction var1, double var2) {
      double var4 = var0.field_1352 * var1.method_10148() + var0.field_1351 * var1.method_10164() + var0.field_1350 * var1.method_10165();
      if (var4 <= 0.0) {
         return false;
      } else {
         double var6 = Math.abs(var0.field_1352);
         double var8 = Math.abs(var0.field_1351);
         double var10 = Math.abs(var0.field_1350);

         return switch (var1.method_10166()) {
            case field_11048 -> var6 > var8 + var2 && var6 > var10 + var2;
            case field_11052 -> var8 > var6 + var2 && var8 > var10 + var2;
            case field_11051 -> var10 > var6 + var2 && var10 > var8 + var2;
            default -> throw new MatchException(null, null);
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
