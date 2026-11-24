package net.efn.client.module.impl.combat.bot;

import java.util.List;
import net.efn.client.util.pvp.Rotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
final class PearlFlight {
   private static final double SPEED = 1.5;
   private static final double GRAVITY = 0.03;
   private static final double DRAG = 0.99;
   private static final double WATER_DRAG = 0.8;
   private static final int MAX_TICKS = 100;
   private static final int MAX_DROP = 3;
   private static final double EDGE_MARGIN = 0.8;

   private PearlFlight() {
   }

   static PearlFlight.Landing land(ClientPlayerEntity clientPlayerEntity, float f, float g) {
      Vec3d var3 = clientPlayerEntity.getEyePos().subtract(0.0, 0.1, 0.0);
      Vec3d var4 = clientPlayerEntity.getMovement();
      Vec3d var5 = Vec3d.fromPolar(g, f).multiply(1.5).add(var4.x, clientPlayerEntity.isOnGround() ? 0.0 : var4.y, var4.z);
      return fly(clientPlayerEntity.getEntityWorld(), clientPlayerEntity, var3, var5);
   }

   static PearlFlight.Landing fly(World world, Entity entity, Vec3d vec3d, Vec3d vec3d2) {
      Vec3d var4 = vec3d;
      Vec3d var5 = vec3d2;

      for (int var6 = 1; var6 <= 100; var6++) {
         BlockPos var7 = BlockPos.ofFloored(var4);
         if (!world.getChunkManager().isChunkLoaded(var7.getX() >> 4, var7.getZ() >> 4) || var4.y < world.getBottomY()) {
            return null;
         }

         boolean var8 = world.getFluidState(var7).isIn(FluidTags.WATER);
         var5 = new Vec3d(var5.x, var5.y - 0.03, var5.z).multiply(var8 ? 0.8 : 0.99);
         Vec3d var9 = var4.add(var5);
         BlockHitResult var10 = world.raycast(new RaycastContext(var4, var9, ShapeType.COLLIDER, FluidHandling.NONE, entity));
         if (var10.getType() != Type.MISS) {
            Vec3d var11 = var10.getPos();
            BlockPos var12 = floorUnder(world, var11);
            return var12 == null ? null : new PearlFlight.Landing(var11, var12, var6);
         }

         var4 = var9;
      }

      return null;
   }

   static Rotation escape(ClientPlayerEntity clientPlayerEntity, List<? extends Entity> list, double d) {
      Vec3d var4 = Vec3d.ZERO;

      for (Entity var6 : list) {
         var4 = var4.add(var6.getEntityPos());
      }

      var4 = var4.multiply(1.0 / Math.max(1, list.size()));
      float var18 = (float)Math.toDegrees(Math.atan2(-(clientPlayerEntity.getX() - var4.x), clientPlayerEntity.getZ() - var4.z));
      Rotation var19 = null;
      double var7 = -1.0 / 0.0;

      for (int var9 = -4; var9 <= 4; var9++) {
         float var10 = MathHelper.wrapDegrees(var18 + var9 * 22.5F);

         for (float var11 = -45.0F; var11 <= 5.0F; var11 += 7.0F) {
            PearlFlight.Landing var16 = land(clientPlayerEntity, var10, var11);
            double var12;
            double var14;
            if (var16 != null
               && safe(clientPlayerEntity.getEntityWorld(), var16)
               && !((var14 = nearest(list, var16.at())) < d)
               && (var12 = var14 - 0.5 * (var16.at().y - var16.floor().getY() - 1.0) - 0.05 * var16.ticks()) > var7) {
               var7 = var12;
               var19 = new Rotation(var10, var11);
            }
         }
      }

      return var19;
   }

   static Rotation toward(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, double d, double e) {
      Vec3d var6 = clientPlayerEntity.getEyePos();
      double var7 = Math.hypot(vec3d.x - var6.x, vec3d.z - var6.z);
      float var9 = (float)Math.toDegrees(Math.atan2(-(vec3d.x - var6.x), vec3d.z - var6.z));
      double var10 = (d + e) / 2.0;

      for (boolean var15 : new boolean[]{false, true}) {
         double[] var16;
         if (var15) {
            double[] var17 = new double[]{0.0, var10, 0.0};
            var16 = var17;
            var17[2] = -var10;
         } else {
            double[] var29 = new double[]{var10, 0.0};
            var16 = var29;
            var29[1] = -var10;
         }

         for (double var20 : var16) {
            double var22 = var7 + var20;
            if (!(var22 < 1.0)) {
               for (float var27 : new float[]{0.0F, -1.5F, 1.5F, -3.0F, 3.0F}) {
                  Rotation var28 = pitchFor(clientPlayerEntity, var6, MathHelper.wrapDegrees(var9 + var27), var22, var15);
                  if (var28 != null && reaches(clientPlayerEntity, var28.yaw(), var28.pitch(), vec3d, var15 ? 0.0 : d, e)) {
                     return var28;
                  }
               }
            }
         }
      }

      return null;
   }

   private static Rotation pitchFor(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, float f, double d, boolean bl) {
      float var6 = 0.0F / 0.0F;
      float var7 = bl ? -88.0F : 15.0F;
      float var8 = bl ? -40.0F : -60.0F;
      float var9 = bl ? 4.0F : -5.0F;

      for (float var10 = var7; bl ? var10 <= var8 : var10 >= var8; var10 += var9) {
         if (!(carry(clientPlayerEntity, vec3d, f, var10) < d)) {
            if (Float.isNaN(var6)) {
               return null;
            }

            float var11 = var6;
            float var12 = var10;

            for (int var13 = 0; var13 < 8; var13++) {
               float var14 = (var11 + var12) / 2.0F;
               if (carry(clientPlayerEntity, vec3d, f, var14) >= d) {
                  var12 = var14;
               } else {
                  var11 = var14;
               }
            }

            return new Rotation(f, (var11 + var12) / 2.0F);
         }

         var6 = var10;
      }

      return null;
   }

   private static double carry(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, float f, float g) {
      PearlFlight.Landing var4 = land(clientPlayerEntity, f, g);
      return var4 == null ? 1.7976931348623157E308 : Math.hypot(var4.at().x - vec3d.x, var4.at().z - vec3d.z);
   }

   static boolean escapes(ClientPlayerEntity clientPlayerEntity, float f, float g, List<? extends Entity> list, double d) {
      PearlFlight.Landing var6 = land(clientPlayerEntity, f, g);
      return var6 != null && safe(clientPlayerEntity.getEntityWorld(), var6) && nearest(list, var6.at()) >= d;
   }

   static boolean reaches(ClientPlayerEntity clientPlayerEntity, float f, float g, Vec3d vec3d, double d, double e) {
      PearlFlight.Landing var8 = land(clientPlayerEntity, f, g);
      if (var8 != null && safe(clientPlayerEntity.getEntityWorld(), var8)) {
         double var9 = horizontal(var8.at(), vec3d);
         return var9 >= d && var9 <= e && Math.abs(var8.at().y - vec3d.y) <= 3.0;
      } else {
         return false;
      }
   }

   static int flightTicks(double d) {
      if (d <= 6.0) {
         return 5;
      } else if (d <= 10.0) {
         return 5 + (int)Math.round((d - 6.0) / 2.0);
      } else if (d <= 14.0) {
         return 7 + (int)Math.round((d - 10.0) * 0.75);
      } else {
         return d <= 20.0 ? 10 + (int)Math.round((d - 14.0) * 0.83) : 15 + (int)Math.round((d - 20.0) * 0.8);
      }
   }

   static double nearest(List<? extends Entity> list, Vec3d vec3d) {
      double var2 = 1.7976931348623157E308;

      for (Entity var5 : list) {
         var2 = Math.min(var2, var5.getEntityPos().distanceTo(vec3d));
      }

      return var2;
   }

   private static double horizontal(Vec3d vec3d, Vec3d vec3d2) {
      return Math.hypot(vec3d.x - vec3d2.x, vec3d.z - vec3d2.z);
   }

   private static BlockPos floorUnder(World world, Vec3d vec3d) {
      BlockPos var2 = BlockPos.ofFloored(vec3d.x, vec3d.y + 0.05, vec3d.z);

      for (int var3 = 0; var3 <= 4; var3++) {
         BlockPos var4 = var2.down(var3);
         if (!world.getBlockState(var4).getCollisionShape(world, var4).isEmpty()) {
            return var4;
         }
      }

      return null;
   }

   private static boolean safe(World world, PearlFlight.Landing landing) {
      BlockPos var2 = landing.floor();
      if (landing.at().y - (var2.getY() + 1.0) > 3.0) {
         return false;
      } else {
         for (double[] var6 : new double[][]{{0.8, 0.0}, {-0.8, 0.0}, {0.0, 0.8}, {0.0, -0.8}}) {
            BlockPos var7 = floorUnder(world, landing.at().add(var6[0], 0.0, var6[1]));
            if (var7 == null || var7.getY() != var2.getY()) {
               return false;
            }
         }

         if (!harmful(world.getBlockState(var2)) && !world.getBlockState(var2).isOf(Blocks.MAGMA_BLOCK)) {
            for (int var8 = 1; var8 <= 2; var8++) {
               BlockPos var9 = var2.up(var8);
               BlockState var10 = world.getBlockState(var9);
               if (harmful(var10) || !var10.getCollisionShape(world, var9).isEmpty() || !world.getFluidState(var9).isEmpty()) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private static boolean harmful(BlockState blockState) {
      return blockState.isOf(Blocks.LAVA)
         || blockState.isIn(BlockTags.FIRE)
         || blockState.isOf(Blocks.CACTUS)
         || blockState.isOf(Blocks.SWEET_BERRY_BUSH)
         || blockState.isOf(Blocks.COBWEB)
         || blockState.isOf(Blocks.POWDER_SNOW)
         || blockState.isOf(Blocks.WITHER_ROSE);
   }

   @Environment(EnvType.CLIENT)
   record Landing(Vec3d at, BlockPos floor, int ticks) {
   }
}
