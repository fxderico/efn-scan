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

   public static Vec3d faceCenterCursor(BlockPos var0, Direction var1) {
      return var0.method_46558().method_1031(var1.method_10148() * 0.5, var1.method_10164() * 0.5, var1.method_10165() * 0.5);
   }

   public static float[] calcRotation(Vec3d var0, Vec3d var1) {
      double var2 = var1.field_1352 - var0.field_1352;
      double var4 = var1.field_1351 - var0.field_1351;
      double var6 = var1.field_1350 - var0.field_1350;
      double var8 = Math.sqrt(var2 * var2 + var6 * var6);
      float var10 = (float)Math.toDegrees(Math.atan2(-var2, var6));
      float var11 = (float)Math.toDegrees(-Math.atan2(var4, var8));
      return new float[]{MathHelper.method_15393(var10), MathHelper.method_15363(var11, -90.0F, 90.0F)};
   }

   public static BlockHitResult rayTraceTowards(PlayerEntity var0, float var1, float var2, double var3) {
      return rayTraceFrom(var0, getEyePos(var0), var1, var2, var3);
   }

   public static BlockHitResult rayTraceFrom(PlayerEntity var0, Vec3d var1, float var2, float var3, double var4) {
      ClientWorld var6 = MinecraftClient.method_1551().field_1687;
      if (var6 == null) {
         return null;
      } else {
         double var7 = Math.toRadians(var2);
         double var9 = Math.cos(Math.toRadians(var3));
         double var11 = Math.sin(Math.toRadians(var3));
         Vec3d var13 = new Vec3d(-Math.sin(var7) * var9, -var11, Math.cos(var7) * var9);
         Vec3d var14 = var1.method_1019(var13.method_1021(var4));
         return var6.method_17742(new RaycastContext(var1, var14, ShapeType.field_17559, FluidHandling.field_1348, var0));
      }
   }

   public static Vec3d getEyePos(PlayerEntity var0) {
      return new Vec3d(var0.method_23317(), var0.method_23320(), var0.method_23321());
   }

   public static Direction getClickFace(Direction var0) {
      return var0.method_10153();
   }

   public static Direction getPlaceSide(BlockPos var0, Vec3d var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      if (var2 == null) {
         return null;
      } else {
         Direction var3 = null;
         double var4 = -1.7976931348623157E308;

         for (Direction var9 : Direction.values()) {
            BlockPos var10 = var0.method_10093(var9);
            BlockState var11 = var2.method_8320(var10);
            if (!var11.method_26215() && !var11.method_26220(var2, var10).method_1110()) {
               Vec3d var12 = var1.method_1020(var10.method_46558());
               double var13 = var12.field_1352 * var9.method_10148() + var12.field_1351 * var9.method_10164() + var12.field_1350 * var9.method_10165();
               if (var13 > var4) {
                  var4 = var13;
                  var3 = var9;
               }
            }
         }

         return var3;
      }
   }

   public static boolean faceMatchesAxis(BlockState var0, Direction var1) {
      return !var0.method_28498(Properties.field_12496) || var0.method_11654(Properties.field_12496) == var1.method_10166();
   }

   public static boolean isFaceVisibleToEye(BlockPos var0, Direction var1, Vec3d var2) {
      double var3 = 0.05;

      return switch (var1) {
         case field_11043 -> var2.field_1350 < var0.method_10260() - 0.05;
         case field_11035 -> var2.field_1350 > var0.method_10260() + 1.0 + 0.05;
         case field_11039 -> var2.field_1352 < var0.method_10263() - 0.05;
         case field_11034 -> var2.field_1352 > var0.method_10263() + 1.0 + 0.05;
         case field_11036 -> var2.field_1351 > var0.method_10264() + 1.0 + 0.05;
         case field_11033 -> var2.field_1351 < var0.method_10264() - 0.05;
         default -> throw new MatchException(null, null);
      };
   }
}
