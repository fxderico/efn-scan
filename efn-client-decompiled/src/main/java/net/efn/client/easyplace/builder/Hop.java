package net.efn.client.easyplace.builder;

import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
final class Hop {
   private static final int TIME_LIMIT = 60;
   private static final double CENTERED = 0.15;
   private static final double STEP = 0.55;
   private static final double LINED_UP = 0.25;
   private static final double LEAP_AT = 0.3;
   private static final double LANDED = 0.55;
   private static final int SAFE_DROP = 3;
   private static final float TURN = 22.0F;
   private static final float WALK_OFF = 30.0F;
   private static final double JUMP_AT = 0.15;
   private final BlockPos spot;
   private final boolean through;
   private final boolean leap;
   private boolean sprint;
   private boolean careful;
   private boolean slow;
   private BlockPos from;
   private double floor = 0.0 / 0.0;
   private boolean centered;
   private int ticks;

   Hop(BlockPos var1) {
      this(var1, false, false);
   }

   Hop(BlockPos var1, boolean var2) {
      this(var1, var2, false);
   }

   Hop(BlockPos var1, boolean var2, boolean var3) {
      this.spot = var1;
      this.through = var2;
      this.leap = var3;
   }

   BlockPos spot() {
      return this.spot;
   }

   Hop sprinting() {
      this.sprint = true;
      return this;
   }

   Hop careful() {
      this.careful = true;
      this.sprint = false;
      return this;
   }

   Hop slow() {
      this.slow = true;
      this.sprint = false;
      return this;
   }

   static BlockPos spot(ClientWorld var0, ClientPlayerEntity var1, Predicate<BlockPos> var2) {
      if (!var1.method_24828()) {
         return null;
      } else {
         BlockPos var3 = var1.method_24515();
         double var4 = Walker.floorOr(var0, var3);
         BlockPos var6 = null;
         double var7 = 0.45;

         for (BlockPos var10 : Walker.steps(var0, var3)) {
            double var11 = Walker.floor(var0, var10) - var4;
            if (var11 > var7 && !var2.test(var10)) {
               var7 = var11;
               var6 = var10;
            }
         }

         return var6;
      }
   }

   Hop.Result tick(MinecraftClient var1) {
      float var3 = 0.0F;
      boolean var4 = false;
      ClientPlayerEntity var5 = var1.field_1724;
      if (var5 == null) {
         return Hop.Result.FAILED;
      } else {
         if (this.from == null) {
            this.from = var5.method_24515();
         }

         if (Double.isNaN(this.floor) && var1.field_1687 != null) {
            this.floor = Walker.floorOr(var1.field_1687, this.spot);
         }

         Vec3d var2 = var5.method_73189();
         var1.field_1690.field_1832.method_23481(this.careful);
         double var6 = this.spot.method_10263() + 0.5 - var2.field_1352;
         double var8 = this.spot.method_10260() + 0.5 - var2.field_1350;
         double var10 = this.through ? 0.35 : 0.12;
         boolean var12 = Math.abs(var6) < var10 && Math.abs(var8) < var10;
         boolean var13 = this.through || var5.method_18798().method_37268() < 0.0025;
         boolean var14 = var2.field_1351 < this.floor + 0.55;
         if (var12 && var13 && var14 && var5.method_24828() && var2.field_1351 >= this.floor - 0.05) {
            if (!this.through) {
               this.release(var1);
            }

            return Hop.Result.DONE;
         } else if (++this.ticks > 60) {
            this.release(var1);
            return Hop.Result.FAILED;
         } else if (var1.field_1687 != null && var5.method_24828() && this.intoHole(var1.field_1687, var5, var2)) {
            var1.field_1690.field_1894.method_23481(false);
            var1.field_1690.field_1867.method_23481(false);
            var1.field_1690.field_1903.method_23481(false);
            var1.field_1690.field_1832.method_23481(true);
            return Hop.Result.RUNNING;
         } else {
            if (!this.centered) {
               double var15 = this.from.method_10263() + 0.5 - var2.field_1352;
               double var17 = this.from.method_10260() + 0.5 - var2.field_1350;
               double var19 = this.spot.method_10263() != this.from.method_10263() ? Math.abs(var17) : Math.abs(var15);
               boolean var21 = this.floor - var2.field_1351 <= 0.55 && var19 < 0.25;
               if (!var21 && (!(Math.abs(var15) < 0.15) || !(Math.abs(var17) < 0.15)) && (var5.method_24828() || this.ticks <= 10)) {
                  var3 = Aim.turn(var1, (float)Math.toDegrees(Math.atan2(-var15, var17)), var5.method_36455(), 22.0F);
                  var1.field_1690.field_1894.method_23481(var3 < 30.0F);
                  var1.field_1690.field_1903.method_23481(false);
                  return Hop.Result.RUNNING;
               }

               this.centered = true;
            }

            var3 = Aim.turn(var1, (float)Math.toDegrees(Math.atan2(-var6, var8)), var5.method_36455(), 22.0F);
            boolean var26 = var5.method_18798().method_37268() > 4.0E-4;
            var1.field_1690.field_1867.method_23481(this.sprint && !var12 && var3 < 10.0F);
            var4 = (!var12 || !var14) && (this.through || Math.hypot(var6, var8) > 0.3 || !var26 || !var14);
            if (this.slow) {
               boolean var16 = var5.method_24828() && var2.field_1351 < this.floor + 0.05 && Math.hypot(var6, var8) < 0.45;
               boolean var27;
               if (var4) {
                  double var18 = var5.method_18798().method_37267();
                  double var20 = var16 ? 0.03 : 0.08;
                  var27 = var18 < var20;
               } else {
                  var27 = false;
               }

               var4 = var27;
            }

            var1.field_1690.field_1894.method_23481(var4 && var3 < 30.0F);
            double var25 = (var2.field_1352 - (this.from.method_10263() + 0.5)) * Integer.signum(this.spot.method_10263() - this.from.method_10263())
               + (var2.field_1350 - (this.from.method_10260() + 0.5)) * Integer.signum(this.spot.method_10260() - this.from.method_10260());
            boolean var7 = var1.field_1724.field_5976
               || var25 > 0.15
               || this.spot.method_10263() == this.from.method_10263() && this.spot.method_10260() == this.from.method_10260();
            var1.field_1690
               .field_1903
               .method_23481(
                  var1.field_1724.method_24828() && this.floor - var2.field_1351 > 0.55 && var7
                     || this.leap && var1.field_1724.method_24828() && var25 > 0.3 && var25 < 1.0
               );
            return Hop.Result.RUNNING;
         }
      }
   }

   private boolean intoHole(ClientWorld var1, ClientPlayerEntity var2, Vec3d var3) {
      Vec3d var4 = var2.method_18798();
      double var5 = var4.method_37267();
      if (var5 < 0.02) {
         return false;
      } else {
         double var7 = var5 * 1.3 + 0.35;
         BlockPos var9 = BlockPos.method_49637(
            var3.field_1352 + var4.field_1352 / var5 * var7, var3.field_1351 + 0.01, var3.field_1350 + var4.field_1350 / var5 * var7
         );
         BlockPos var10 = var2.method_24515();
         return (var9.method_10263() != this.spot.method_10263() || var9.method_10260() != this.spot.method_10260())
               && (var9.method_10263() != var10.method_10263() || var9.method_10260() != var10.method_10260())
            ? Walker.dropAt(var1, var9) > 3
            : false;
      }
   }

   void release(MinecraftClient var1) {
      var1.field_1690.field_1894.method_23481(false);
      var1.field_1690.field_1903.method_23481(false);
      var1.field_1690.field_1867.method_23481(false);
      if (this.careful) {
         var1.field_1690.field_1832.method_23481(false);
      }
   }

   @Environment(EnvType.CLIENT)
   static enum Result {
      RUNNING,
      DONE,
      FAILED;

      private Result() {
      }
   }
}
