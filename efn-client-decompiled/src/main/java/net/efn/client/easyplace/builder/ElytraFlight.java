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

   static float bearing(double d, double e, double f, double g) {
      return (float)(MathHelper.atan2(g - e, f - d) * 57.2957763671875) - 90.0F;
   }

   static double cost(ElytraFlight.Glide glide, ElytraFlight.Goal goal, ElytraFlight.Terrain terrain, float f, float g, boolean bl, float h) {
      ElytraFlight.Glide var9 = glide.copy();
      if (bl) {
         var9.boost = 45;
      }

      double var10 = Math.hypot(goal.x() - var9.x, goal.z() - var9.z);
      double var12 = 0.0;
      double var14 = 0.0;
      double var16 = 0.0;
      double var18 = 0.0;

      for (int var20 = 1; var20 <= 60; var20++) {
         var9.step(f, g, h);
         double var21 = var9.y - terrain.under(var9.x, var9.z);
         if (var21 < 0.05) {
            return 1000000.0 + (60 - var20) * 1000.0;
         }

         double var23 = Math.max(goal.minClear(), Math.max(var9.speed() * 5.0, -var9.vy * 25.0));
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

         if (var9.y < goal.floor()) {
            var16 += (goal.floor() - var9.y) * 4.0;
         }

         var18 += Math.abs(var9.y - goal.altitude()) * (var20 < 20 ? 0.4 : 1.0);
      }

      double var27 = var12 / 60.0 * 10.0 + var14 / 60.0 * 10.0 + var16 / 60.0 + var18 / 60.0 * 0.4;
      double var22 = var9.y - goal.altitude();
      var27 += var22 < 0.0 ? -var22 * 2.2 : var22 * (goal.landing() ? 1.6 : 0.6);
      double var24 = Math.hypot(goal.x() - var9.x, goal.z() - var9.z);
      var27 -= var10 - var24;
      if (bl) {
         var27 += goal.rocketCost();
      }

      double var7;
      if ((var7 = var9.flatSpeed()) < 0.55) {
         var27 += (0.55 - var7) * 60.0;
      }

      if (goal.landing() && var24 < 120.0 && var7 > goal.maxSpeed()) {
         var27 += (var7 - goal.maxSpeed()) * 40.0;
      }

      return var27;
   }

   static boolean clear(ElytraFlight.Glide glide, ElytraFlight.Goal goal, ElytraFlight.Terrain terrain, float f, float g, boolean bl, float h) {
      ElytraFlight.Glide var7 = glide.copy();
      if (bl) {
         var7.boost = 45;
      }

      for (int var8 = 1; var8 <= 60; var8++) {
         var7.step(f, g, h);
         if (var7.y - terrain.under(var7.x, var7.z) < goal.minClear()) {
            return false;
         }
      }

      return var7.flatSpeed() > 0.4;
   }

   static ElytraFlight.Plan search(ElytraFlight.Glide glide, ElytraFlight.Goal goal, ElytraFlight.Terrain terrain, boolean bl, float f) {
      float var5 = bearing(glide.x, glide.z, goal.x(), goal.z());
      ElytraFlight.Plan var6 = pass(glide, goal, terrain, var5, NEAR_YAWS, bl, f, false, null);
      if (var6.safe()) {
         return var6;
      } else {
         ElytraFlight.Plan var7;
         return (var7 = pass(glide, goal, terrain, var5, NEAR_YAWS, bl, f, true, var6)).safe()
            ? var7
            : pass(glide, goal, terrain, var5, WIDE_YAWS, bl, f, true, var7);
      }
   }

   private static ElytraFlight.Plan pass(
      ElytraFlight.Glide glide,
      ElytraFlight.Goal goal,
      ElytraFlight.Terrain terrain,
      float f,
      float[] fs,
      boolean bl,
      float g,
      boolean bl2,
      ElytraFlight.Plan plan
   ) {
      for (float var12 : fs) {
         float var13 = MathHelper.wrapDegrees(f + var12);

         for (float var17 : PITCHES) {
            for (int var18 = 0; var18 < 2; var18++) {
               boolean var19 = var18 == 1;
               if (!var19 || goal.rockets() && !bl && (bl2 || !(var17 > -30.0F))) {
                  double var21 = cost(glide, goal, terrain, var13, var17, var19, g) + Math.abs(var12) * 0.08;
                  if (plan == null || var21 < plan.cost()) {
                     plan = new ElytraFlight.Plan(var13, var17, var19, var21, var21 < 400.0);
                  }
               }
            }
         }
      }

      return plan;
   }

   static ElytraFlight.Plan touchdown(ElytraFlight.Glide glide, BlockPos blockPos, ElytraFlight.Terrain terrain, float f) {
      float var4 = bearing(glide.x, glide.z, blockPos.getX() + 0.5, blockPos.getZ() + 0.5);
      ElytraFlight.Plan var5 = null;

      for (float var9 : LAND_YAWS) {
         float var10 = MathHelper.wrapDegrees(var4 + var9);

         for (float var14 : LAND_PITCHES) {
            double var15 = landing(glide, blockPos, terrain, var10, var14, f) + Math.abs(var9) * 0.15;
            if (var5 == null || var15 < var5.cost()) {
               var5 = new ElytraFlight.Plan(var10, var14, false, var15, var15 < 900.0);
            }
         }
      }

      return var5;
   }

   private static double landing(ElytraFlight.Glide glide, BlockPos blockPos, ElytraFlight.Terrain terrain, float f, float g, float h) {
      ElytraFlight.Glide var6 = glide.copy();
      double var7 = terrain.under(var6.x, var6.z);

      for (int var9 = 1; var9 <= 200; var9++) {
         double var10 = var7;
         var6.step(f, g, h);
         var7 = terrain.under(var6.x, var6.z);
         if (!(var6.y > var7)) {
            if (var7 - var10 > 0.6 && var6.flatSpeed() > 0.3) {
               return 1000000.0;
            }

            double var12 = Math.hypot(var6.x - (blockPos.getX() + 0.5), var6.z - (blockPos.getZ() + 0.5));
            if (var6.vy < -0.5) {
               var12 += 200.0;
            }

            if (var6.flatSpeed() > 1.0) {
               var12 += (var6.flatSpeed() - 1.0) * 20.0;
            }

            if (!goodFloor(terrain.level(), BlockPos.ofFloored(var6.x, var7 - 1.0, var6.z))) {
               var12 += 900.0;
            }

            return var12;
         }
      }

      return 1000000.0;
   }

   static boolean goodFloor(ClientWorld clientWorld, BlockPos blockPos) {
      if (blockPos.getY() < clientWorld.getBottomY()) {
         return false;
      } else {
         BlockState var2 = clientWorld.getBlockState(blockPos);
         if (!var2.getFluidState().isEmpty() || var2.isIn(BlockTags.LEAVES) || hurts(var2)) {
            return false;
         } else if (!var2.isOf(Blocks.SLIME_BLOCK) && !var2.isOf(Blocks.HONEY_BLOCK) && !var2.isOf(Blocks.POWDER_SNOW)) {
            VoxelShape var3 = var2.getCollisionShape(clientWorld, blockPos);
            if (var3.isEmpty()) {
               return false;
            } else {
               Box var4 = var3.getBoundingBox();
               if (!(var4.maxY < 0.45) && !(var4.maxY > 1.01) && !(var4.getLengthX() < 0.9) && !(var4.getLengthZ() < 0.9)) {
                  BlockPos var5 = blockPos.up();
                  BlockState var6 = clientWorld.getBlockState(var5);
                  return var6.getCollisionShape(clientWorld, var5).isEmpty() && var6.getFluidState().isEmpty() && !hurts(var6);
               } else {
                  return false;
               }
            }
         } else {
            return false;
         }
      }
   }

   private static boolean hurts(BlockState blockState) {
      return blockState.isOf(Blocks.MAGMA_BLOCK)
         || blockState.isIn(BlockTags.CAMPFIRES)
         || blockState.isOf(Blocks.CACTUS)
         || blockState.isOf(Blocks.SWEET_BERRY_BUSH)
         || blockState.isIn(BlockTags.FIRE)
         || blockState.isOf(Blocks.POINTED_DRIPSTONE)
         || blockState.isOf(Blocks.WITHER_ROSE)
         || blockState.isOf(Blocks.COBWEB)
         || blockState.isOf(Blocks.LAVA);
   }

   static BlockPos landingSpot(ElytraFlight.Terrain terrain, int i, int j, int k, boolean bl, Predicate<BlockPos> predicate) {
      ClientWorld var6 = terrain.level();

      for (byte var7 = 0; var7 <= k; var7 += 2) {
         for (int var8 = -var7; var8 <= var7; var8 += 2) {
            for (int var9 = -var7; var9 <= var7; var9 += 2) {
               int var10;
               BlockPos var11;
               int var12;
               int var13;
               if (Math.max(Math.abs(var8), Math.abs(var9)) == var7
                  && terrain.known((var13 = i + var8) - 1, (var12 = j + var9) - 1)
                  && terrain.known(var13 + 1, var12 + 1)
                  && !predicate.test(var11 = new BlockPos(var13, var10 = var6.getTopY(Type.MOTION_BLOCKING, var13, var12), var12))
                  && flat(var6, var13, var12, var10, bl)) {
                  return var11;
               }
            }
         }
      }

      return null;
   }

   private static boolean flat(ClientWorld clientWorld, int i, int j, int k, boolean bl) {
      if (!goodFloor(clientWorld, new BlockPos(i, k - 1, j))) {
         return false;
      } else {
         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               if (var5 != 0 || var6 != 0) {
                  int var7 = clientWorld.getTopY(Type.MOTION_BLOCKING, i + var5, j + var6);
                  if (bl ? var7 != k : Math.abs(var7 - k) > 1) {
                     return false;
                  }

                  if (!goodFloor(clientWorld, new BlockPos(i + var5, var7 - 1, j + var6))) {
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

      static ElytraFlight.Glide of(ClientPlayerEntity clientPlayerEntity, int i) {
         ElytraFlight.Glide var2 = new ElytraFlight.Glide();
         var2.x = clientPlayerEntity.getX();
         var2.y = clientPlayerEntity.getY();
         var2.z = clientPlayerEntity.getZ();
         Vec3d var3 = clientPlayerEntity.getVelocity();
         var2.vx = var3.x;
         var2.vy = var3.y;
         var2.vz = var3.z;
         var2.yaw = clientPlayerEntity.getYaw();
         var2.pitch = clientPlayerEntity.getPitch();
         var2.boost = i;
         var2.gravity = clientPlayerEntity.hasStatusEffect(StatusEffects.SLOW_FALLING) ? 0.01 : 0.08;
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

      void step(float f, float g, float h) {
         this.yaw = this.yaw + MathHelper.clamp(MathHelper.wrapDegrees(f - this.yaw), -h, h);
         this.pitch = MathHelper.clamp(this.pitch + MathHelper.clamp(g - this.pitch, -h, h), -90.0F, 90.0F);
         Vec3d var4 = Vec3d.fromPolar(this.pitch, this.yaw);
         double var5 = this.pitch * 0.017453292F;
         double var7 = Math.sqrt(var4.x * var4.x + var4.z * var4.z);
         double var9 = Math.sqrt(this.vx * this.vx + this.vz * this.vz);
         double var11 = Math.cos(var5);
         var11 *= var11;
         double var13 = this.vy <= 0.0 ? this.gravity : 0.08;
         this.vy += var13 * (-1.0 + var11 * 0.75);
         if (this.vy < 0.0 && var7 > 0.0) {
            double var15 = this.vy * -0.1 * var11;
            this.vx = this.vx + var4.x * var15 / var7;
            this.vy += var15;
            this.vz = this.vz + var4.z * var15 / var7;
         }

         if (var5 < 0.0 && var7 > 0.0) {
            double var18 = var9 * -Math.sin(var5) * 0.04;
            this.vx = this.vx - var4.x * var18 / var7;
            this.vy += var18 * 3.2;
            this.vz = this.vz - var4.z * var18 / var7;
         }

         if (var7 > 0.0) {
            this.vx = this.vx + (var4.x / var7 * var9 - this.vx) * 0.1;
            this.vz = this.vz + (var4.z / var7 * var9 - this.vz) * 0.1;
         }

         this.vx *= 0.99;
         this.vy *= 0.98;
         this.vz *= 0.99;
         if (this.boost > 0) {
            this.vx = this.vx + (var4.x * 0.1 + (var4.x * 1.5 - this.vx) * 0.5);
            this.vy = this.vy + (var4.y * 0.1 + (var4.y * 1.5 - this.vy) * 0.5);
            this.vz = this.vz + (var4.z * 0.1 + (var4.z * 1.5 - this.vz) * 0.5);
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

      Terrain(ClientWorld clientWorld, double d) {
         this.level = clientWorld;
         this.unknown = d;
         this.heights.defaultReturnValue(0.0 / 0.0);
      }

      double surface(double d, double e) {
         int var6 = MathHelper.floor(d);
         int var5;
         long var7 = BlockPos.asLong(var6, 0, var5 = MathHelper.floor(e));
         double var9 = this.heights.get(var7);
         if (Double.isNaN(var9)) {
            var9 = this.known(var6, var5) ? this.level.getTopY(Type.MOTION_BLOCKING, var6, var5) : this.unknown;
            this.heights.put(var7, var9);
         }

         return var9;
      }

      double under(double d, double e) {
         double var5 = this.surface(d, e);
         var5 = Math.max(var5, this.surface(d + 0.3, e + 0.3));
         var5 = Math.max(var5, this.surface(d - 0.3, e - 0.3));
         var5 = Math.max(var5, this.surface(d + 0.3, e - 0.3));
         return Math.max(var5, this.surface(d - 0.3, e + 0.3));
      }

      boolean known(int i, int j) {
         return this.level.getChunkManager().isChunkLoaded(i >> 4, j >> 4);
      }

      ClientWorld level() {
         return this.level;
      }
   }
}
