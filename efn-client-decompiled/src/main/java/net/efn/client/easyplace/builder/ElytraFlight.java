package net.efn.client.easyplace.builder;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap.Type;

@Environment(EnvType.CLIENT)
final class ElytraFlight {
   static final int BOOST_TICKS = 45;
   static final int HORIZON = 60;
   private static final double CRASH = 1000000.0;
   private static final float[] PITCHES = new float[]{-62.0F, -46.0F, -33.0F, -23.0F, -15.0F, -9.0F, -4.0F, 1.0F, 6.0F, 12.0F, 19.0F, 27.0F, 36.0F};
   private static final float[] NEAR_YAWS = new float[]{0.0F, 12.0F, -12.0F};
   private static final float[] WIDE_YAWS = new float[]{30.0F, -30.0F, 55.0F, -55.0F, 90.0F, -90.0F, 140.0F, -140.0F};
   private static final float[] LAND_PITCHES = new float[]{-28.0F, -18.0F, -10.0F, -4.0F, 0.0F, 4.0F, 8.0F, 12.0F, 16.0F, 21.0F, 27.0F, 34.0F};
   private static final float[] LAND_YAWS = new float[]{0.0F, 18.0F, -18.0F, 36.0F, -36.0F, 60.0F, -60.0F, 90.0F, -90.0F};
   private static final double FOOT = 0.3;

   private ElytraFlight() {
   }

   static float bearing(double var0, double var2, double var4, double var6) {
      return (float)(MathHelper.method_15349(var6 - var2, var4 - var0) * 57.2957763671875) - 90.0F;
   }

   static double cost(ElytraFlight.Glide var0, ElytraFlight.Goal var1, ElytraFlight.Terrain var2, float var3, float var4, boolean var5, float var6) {
      ElytraFlight.Glide var9 = var0.copy();
      if (var5) {
         var9.boost = 45;
      }

      double var10 = Math.hypot(var1.x() - var9.x, var1.z() - var9.z);
      double var12 = 0.0;
      double var14 = 0.0;
      double var16 = 0.0;
      double var18 = 0.0;

      for (int var20 = 1; var20 <= 60; var20++) {
         var9.step(var3, var4, var6);
         double var21 = var9.y - var2.under(var9.x, var9.z);
         if (var21 < 0.05) {
            return 1000000.0 + (60 - var20) * 1000.0;
         }

         double var23 = Math.max(var1.minClear(), Math.max(var9.speed() * 5.0, -var9.vy * 25.0));
         if (var21 < var23) {
            double var25 = var23 - var21;
            var12 += var25 * var25 / 2.0 + 2.0 * var25;
         }

         if (var9.vy < -0.5) {
            var14 += (-var9.vy - 0.5) * 25.0;
            if (var21 < 8.0) {
               var14 += (-var9.vy - 0.5) * 30.0;
            }
         }

         if (var9.y < var1.floor()) {
            var16 += (var1.floor() - var9.y) * 4.0;
         }

         var18 += Math.abs(var9.y - var1.altitude()) * (var20 < 20 ? 0.4 : 1.0);
      }

      double var27 = var12 / 60.0 * 10.0 + var14 / 60.0 * 10.0 + var16 / 60.0 + var18 / 60.0 * 0.4;
      double var22 = var9.y - var1.altitude();
      var27 += var22 < 0.0 ? -var22 * 2.2 : var22 * (var1.landing() ? 1.6 : 0.6);
      double var24 = Math.hypot(var1.x() - var9.x, var1.z() - var9.z);
      var27 -= var10 - var24;
      if (var5) {
         var27 += var1.rocketCost();
      }

      double var7;
      if ((var7 = var9.flatSpeed()) < 0.55) {
         var27 += (0.55 - var7) * 60.0;
      }

      if (var1.landing() && var24 < 120.0 && var7 > var1.maxSpeed()) {
         var27 += (var7 - var1.maxSpeed()) * 40.0;
      }

      return var27;
   }

   static boolean clear(ElytraFlight.Glide var0, ElytraFlight.Goal var1, ElytraFlight.Terrain var2, float var3, float var4, boolean var5, float var6) {
      ElytraFlight.Glide var7 = var0.copy();
      if (var5) {
         var7.boost = 45;
      }

      for (int var8 = 1; var8 <= 60; var8++) {
         var7.step(var3, var4, var6);
         if (var7.y - var2.under(var7.x, var7.z) < var1.minClear()) {
            return false;
         }
      }

      return var7.flatSpeed() > 0.4;
   }

   static ElytraFlight.Plan search(ElytraFlight.Glide var0, ElytraFlight.Goal var1, ElytraFlight.Terrain var2, boolean var3, float var4) {
      float var5 = bearing(var0.x, var0.z, var1.x(), var1.z());
      ElytraFlight.Plan var6 = pass(var0, var1, var2, var5, NEAR_YAWS, var3, var4, false, null);
      if (var6.safe()) {
         return var6;
      } else {
         ElytraFlight.Plan var7;
         return (var7 = pass(var0, var1, var2, var5, NEAR_YAWS, var3, var4, true, var6)).safe()
            ? var7
            : pass(var0, var1, var2, var5, WIDE_YAWS, var3, var4, true, var7);
      }
   }

   private static ElytraFlight.Plan pass(
      ElytraFlight.Glide var0,
      ElytraFlight.Goal var1,
      ElytraFlight.Terrain var2,
      float var3,
      float[] var4,
      boolean var5,
      float var6,
      boolean var7,
      ElytraFlight.Plan var8
   ) {
      for (float var12 : var4) {
         float var13 = MathHelper.method_15393(var3 + var12);

         for (float var17 : PITCHES) {
            for (int var18 = 0; var18 < 2; var18++) {
               boolean var19 = var18 == 1;
               if (!var19 || var1.rockets() && !var5 && (var7 || !(var17 > -30.0F))) {
                  double var21 = cost(var0, var1, var2, var13, var17, var19, var6) + Math.abs(var12) * 0.08;
                  if (var8 == null || var21 < var8.cost()) {
                     var8 = new ElytraFlight.Plan(var13, var17, var19, var21, var21 < 400.0);
                  }
               }
            }
         }
      }

      return var8;
   }

   static ElytraFlight.Plan touchdown(ElytraFlight.Glide var0, BlockPos var1, ElytraFlight.Terrain var2, float var3) {
      float var4 = bearing(var0.x, var0.z, var1.method_10263() + 0.5, var1.method_10260() + 0.5);
      ElytraFlight.Plan var5 = null;

      for (float var9 : LAND_YAWS) {
         float var10 = MathHelper.method_15393(var4 + var9);

         for (float var14 : LAND_PITCHES) {
            double var15 = landing(var0, var1, var2, var10, var14, var3) + Math.abs(var9) * 0.15;
            if (var5 == null || var15 < var5.cost()) {
               var5 = new ElytraFlight.Plan(var10, var14, false, var15, var15 < 900.0);
            }
         }
      }

      return var5;
   }

   private static double landing(ElytraFlight.Glide var0, BlockPos var1, ElytraFlight.Terrain var2, float var3, float var4, float var5) {
      ElytraFlight.Glide var6 = var0.copy();
      double var7 = var2.under(var6.x, var6.z);

      for (int var9 = 1; var9 <= 200; var9++) {
         double var10 = var7;
         var6.step(var3, var4, var5);
         var7 = var2.under(var6.x, var6.z);
         if (!(var6.y > var7)) {
            if (var7 - var10 > 0.6 && var6.flatSpeed() > 0.3) {
               return 1000000.0;
            }

            double var12 = Math.hypot(var6.x - (var1.method_10263() + 0.5), var6.z - (var1.method_10260() + 0.5));
            if (var6.vy < -0.5) {
               var12 += 200.0;
            }

            if (var6.flatSpeed() > 1.0) {
               var12 += (var6.flatSpeed() - 1.0) * 20.0;
            }

            if (!goodFloor(var2.level(), BlockPos.method_49637(var6.x, var7 - 1.0, var6.z))) {
               var12 += 900.0;
            }

            return var12;
         }
      }

      return 1000000.0;
   }

   static boolean goodFloor(ClientWorld var0, BlockPos var1) {
      if (var1.method_10264() < var0.method_31607()) {
         return false;
      } else {
         BlockState var2 = var0.method_8320(var1);
         if (!var2.method_26227().method_15769() || var2.method_26164(BlockTags.field_15503) || hurts(var2)) {
            return false;
         } else if (!var2.method_27852(Blocks.field_10030) && !var2.method_27852(Blocks.field_21211) && !var2.method_27852(Blocks.field_27879)) {
            VoxelShape var3 = var2.method_26220(var0, var1);
            if (var3.method_1110()) {
               return false;
            } else {
               Box var4 = var3.method_1107();
               if (!(var4.field_1325 < 0.45) && !(var4.field_1325 > 1.01) && !(var4.method_17939() < 0.9) && !(var4.method_17941() < 0.9)) {
                  BlockPos var5 = var1.method_10084();
                  BlockState var6 = var0.method_8320(var5);
                  return var6.method_26220(var0, var5).method_1110() && var6.method_26227().method_15769() && !hurts(var6);
               } else {
                  return false;
               }
            }
         } else {
            return false;
         }
      }
   }

   private static boolean hurts(BlockState var0) {
      return var0.method_27852(Blocks.field_10092)
         || var0.method_26164(BlockTags.field_23799)
         || var0.method_27852(Blocks.field_10029)
         || var0.method_27852(Blocks.field_16999)
         || var0.method_26164(BlockTags.field_21952)
         || var0.method_27852(Blocks.field_28048)
         || var0.method_27852(Blocks.field_10606)
         || var0.method_27852(Blocks.field_10343)
         || var0.method_27852(Blocks.field_10164);
   }

   static BlockPos landingSpot(ElytraFlight.Terrain var0, int var1, int var2, int var3, boolean var4, Predicate<BlockPos> var5) {
      ClientWorld var6 = var0.level();

      for (byte var7 = 0; var7 <= var3; var7 += 2) {
         for (int var8 = -var7; var8 <= var7; var8 += 2) {
            for (int var9 = -var7; var9 <= var7; var9 += 2) {
               int var10;
               BlockPos var11;
               int var12;
               int var13;
               if (Math.max(Math.abs(var8), Math.abs(var9)) == var7
                  && var0.known((var13 = var1 + var8) - 1, (var12 = var2 + var9) - 1)
                  && var0.known(var13 + 1, var12 + 1)
                  && !var5.test(var11 = new BlockPos(var13, var10 = var6.method_8624(Type.field_13197, var13, var12), var12))
                  && flat(var6, var13, var12, var10, var4)) {
                  return var11;
               }
            }
         }
      }

      return null;
   }

   private static boolean flat(ClientWorld var0, int var1, int var2, int var3, boolean var4) {
      if (!goodFloor(var0, new BlockPos(var1, var3 - 1, var2))) {
         return false;
      } else {
         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               if (var5 != 0 || var6 != 0) {
                  int var7 = var0.method_8624(Type.field_13197, var1 + var5, var2 + var6);
                  if (var4 ? var7 != var3 : Math.abs(var7 - var3) > 1) {
                     return false;
                  }

                  if (!goodFloor(var0, new BlockPos(var1 + var5, var7 - 1, var2 + var6))) {
                     return false;
                  }
               }
            }
         }

         return true;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Glide {
      double x;
      double y;
      double z;
      double vx;
      double vy;
      double vz;
      float yaw;
      float pitch;
      int boost;
      double gravity;

      Glide() {
      }

      static ElytraFlight.Glide of(ClientPlayerEntity var0, int var1) {
         ElytraFlight.Glide var2 = new ElytraFlight.Glide();
         var2.x = var0.method_23317();
         var2.y = var0.method_23318();
         var2.z = var0.method_23321();
         Vec3d var3 = var0.method_18798();
         var2.vx = var3.field_1352;
         var2.vy = var3.field_1351;
         var2.vz = var3.field_1350;
         var2.yaw = var0.method_36454();
         var2.pitch = var0.method_36455();
         var2.boost = var1;
         var2.gravity = var0.method_6059(StatusEffects.field_5906) ? 0.01 : 0.08;
         return var2;
      }

      ElytraFlight.Glide copy() {
         ElytraFlight.Glide var1 = new ElytraFlight.Glide();
         var1.x = this.x;
         var1.y = this.y;
         var1.z = this.z;
         var1.vx = this.vx;
         var1.vy = this.vy;
         var1.vz = this.vz;
         var1.yaw = this.yaw;
         var1.pitch = this.pitch;
         var1.boost = this.boost;
         var1.gravity = this.gravity;
         return var1;
      }

      void step(float var1, float var2, float var3) {
         this.yaw = this.yaw + MathHelper.method_15363(MathHelper.method_15393(var1 - this.yaw), -var3, var3);
         this.pitch = MathHelper.method_15363(this.pitch + MathHelper.method_15363(var2 - this.pitch, -var3, var3), -90.0F, 90.0F);
         Vec3d var4 = Vec3d.method_1030(this.pitch, this.yaw);
         double var5 = this.pitch * 0.017453292F;
         double var7 = Math.sqrt(var4.field_1352 * var4.field_1352 + var4.field_1350 * var4.field_1350);
         double var9 = Math.sqrt(this.vx * this.vx + this.vz * this.vz);
         double var11 = Math.cos(var5);
         var11 *= var11;
         double var13 = this.vy <= 0.0 ? this.gravity : 0.08;
         this.vy += var13 * (-1.0 + var11 * 0.75);
         if (this.vy < 0.0 && var7 > 0.0) {
            double var15 = this.vy * -0.1 * var11;
            this.vx = this.vx + var4.field_1352 * var15 / var7;
            this.vy += var15;
            this.vz = this.vz + var4.field_1350 * var15 / var7;
         }

         if (var5 < 0.0 && var7 > 0.0) {
            double var18 = var9 * -Math.sin(var5) * 0.04;
            this.vx = this.vx - var4.field_1352 * var18 / var7;
            this.vy += var18 * 3.2;
            this.vz = this.vz - var4.field_1350 * var18 / var7;
         }

         if (var7 > 0.0) {
            this.vx = this.vx + (var4.field_1352 / var7 * var9 - this.vx) * 0.1;
            this.vz = this.vz + (var4.field_1350 / var7 * var9 - this.vz) * 0.1;
         }

         this.vx *= 0.99;
         this.vy *= 0.98;
         this.vz *= 0.99;
         if (this.boost > 0) {
            this.vx = this.vx + (var4.field_1352 * 0.1 + (var4.field_1352 * 1.5 - this.vx) * 0.5);
            this.vy = this.vy + (var4.field_1351 * 0.1 + (var4.field_1351 * 1.5 - this.vy) * 0.5);
            this.vz = this.vz + (var4.field_1350 * 0.1 + (var4.field_1350 * 1.5 - this.vz) * 0.5);
            this.boost--;
         }

         this.x = this.x + this.vx;
         this.y = this.y + this.vy;
         this.z = this.z + this.vz;
      }

      double speed() {
         return Math.sqrt(this.vx * this.vx + this.vy * this.vy + this.vz * this.vz);
      }

      double flatSpeed() {
         return Math.sqrt(this.vx * this.vx + this.vz * this.vz);
      }
   }

   @Environment(EnvType.CLIENT)
   record Goal(double x, double z, double altitude, double minClear, boolean landing, boolean rockets, double rocketCost, double maxSpeed, double floor) {
   }

   @Environment(EnvType.CLIENT)
   record Plan(float yaw, float pitch, boolean rocket, double cost, boolean safe) {
   }

   @Environment(EnvType.CLIENT)
   static final class Terrain {
      private final ClientWorld level;
      private final double unknown;
      private final Long2DoubleOpenHashMap heights = new Long2DoubleOpenHashMap();

      Terrain(ClientWorld var1, double var2) {
         this.level = var1;
         this.unknown = var2;
         this.heights.defaultReturnValue(0.0 / 0.0);
      }

      double surface(double var1, double var3) {
         int var6 = MathHelper.method_15357(var1);
         int var5;
         long var7 = BlockPos.method_10064(var6, 0, var5 = MathHelper.method_15357(var3));
         double var9 = this.heights.get(var7);
         if (Double.isNaN(var9)) {
            var9 = this.known(var6, var5) ? this.level.method_8624(Type.field_13197, var6, var5) : this.unknown;
            this.heights.put(var7, var9);
         }

         return var9;
      }

      double under(double var1, double var3) {
         double var5 = this.surface(var1, var3);
         var5 = Math.max(var5, this.surface(var1 + 0.3, var3 + 0.3));
         var5 = Math.max(var5, this.surface(var1 - 0.3, var3 - 0.3));
         var5 = Math.max(var5, this.surface(var1 + 0.3, var3 - 0.3));
         return Math.max(var5, this.surface(var1 - 0.3, var3 + 0.3));
      }

      boolean known(int var1, int var2) {
         return this.level.method_2935().method_12123(var1 >> 4, var2 >> 4);
      }

      ClientWorld level() {
         return this.level;
      }
   }
}
