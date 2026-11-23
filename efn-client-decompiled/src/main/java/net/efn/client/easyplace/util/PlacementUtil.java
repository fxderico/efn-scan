package net.efn.client.easyplace.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class PlacementUtil {
   private PlacementUtil() {
   }

   public static Vec3d faceCenterCursor(BlockPos blockPos, Direction direction) {
      return blockPos.toCenterPos().add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5);
   }

   public static float[] calcRotation(Vec3d vec3d, Vec3d vec3d2) {
      double var2 = vec3d2.x - vec3d.x;
      double var4 = vec3d2.y - vec3d.y;
      double var6 = vec3d2.z - vec3d.z;
      double var8 = Math.sqrt(var2 * var2 + var6 * var6);
      float var10 = (float)Math.toDegrees(Math.atan2(-var2, var6));
      float var11 = (float)Math.toDegrees(-Math.atan2(var4, var8));
      return new float[]{MathHelper.wrapDegrees(var10), MathHelper.clamp(var11, -90.0F, 90.0F)};
   }

   public static BlockHitResult rayTraceTowards(PlayerEntity playerEntity, float f, float g, double d) {
      return rayTraceFrom(playerEntity, getEyePos(playerEntity), f, g, d);
   }

   public static BlockHitResult rayTraceFrom(PlayerEntity playerEntity, Vec3d vec3d, float f, float g, double d) {
      ClientWorld var6 = MinecraftClient.getInstance().world;
      if (var6 == null) {
         return null;
      } else {
         double var7 = Math.toRadians(f);
         double var9 = Math.cos(Math.toRadians(g));
         double var11 = Math.sin(Math.toRadians(g));
         Vec3d var13 = new Vec3d(-Math.sin(var7) * var9, -var11, Math.cos(var7) * var9);
         Vec3d var14 = vec3d.add(var13.multiply(d));
         return var6.raycast(new RaycastContext(vec3d, var14, ShapeType.OUTLINE, FluidHandling.NONE, playerEntity));
      }
   }

   public static Vec3d getEyePos(PlayerEntity playerEntity) {
      return new Vec3d(playerEntity.getX(), playerEntity.getEyeY(), playerEntity.getZ());
   }

   public static Direction getClickFace(Direction direction) {
      return direction.getOpposite();
   }

   public static Direction getPlaceSide(BlockPos blockPos, Vec3d vec3d) {
      ClientWorld var2 = MinecraftClient.getInstance().world;
      if (var2 == null) {
         return null;
      } else {
         Direction var3 = null;
         double var4 = -1.7976931348623157E308;

         for (Direction var9 : Direction.values()) {
            BlockPos var10 = blockPos.offset(var9);
            BlockState var11 = var2.getBlockState(var10);
            if (!var11.isAir() && !var11.getCollisionShape(var2, var10).isEmpty()) {
               Vec3d var12 = vec3d.subtract(var10.toCenterPos());
               double var13 = var12.x * var9.getOffsetX() + var12.y * var9.getOffsetY() + var12.z * var9.getOffsetZ();
               if (var13 > var4) {
                  var4 = var13;
                  var3 = var9;
               }
            }
         }

         return var3;
      }
   }

   public static boolean faceMatchesAxis(BlockState blockState, Direction direction) {
      return !blockState.contains(Properties.AXIS) || blockState.get(Properties.AXIS) == direction.getAxis();
   }

   public static boolean isFaceVisibleToEye(BlockPos blockPos, Direction direction, Vec3d vec3d) {
      double var3 = 0.05;

      return switch (direction) {
         case NORTH -> vec3d.z < blockPos.getZ() - 0.05;
         case SOUTH -> vec3d.z > blockPos.getZ() + 1.0 + 0.05;
         case WEST -> vec3d.x < blockPos.getX() - 0.05;
         case EAST -> vec3d.x > blockPos.getX() + 1.0 + 0.05;
         case UP -> vec3d.y > blockPos.getY() + 1.0 + 0.05;
         case DOWN -> vec3d.y < blockPos.getY() - 0.05;
      };
   }
}
