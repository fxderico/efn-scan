package net.efn.client.easyplace.builder;

import java.util.Random;
import net.efn.client.easyplace.mixin.AccessorMouseHandler;
import net.efn.client.easyplace.util.PlacementUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FluidFillable;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class Aim {
   private static final Direction[] ORDER = new Direction[]{
      Direction.field_11036, Direction.field_11043, Direction.field_11035, Direction.field_11034, Direction.field_11039, Direction.field_11033
   };
   private static final double[][] FACE_POINTS = new double[][]{{0.5, 0.5}, {0.5, 0.85}, {0.5, 0.15}, {0.15, 0.5}, {0.85, 0.5}};
   private static final float SETTLED = 0.6F;
   private static final float RETARGET = 25.0F;
   private static Aim.Motion motion;
   private static final Random HAND = new Random();
   public static final float HAND_TURN = 22.0F;

   private Aim() {
   }

   static Aim.Shot at(MinecraftClient var0, BlockPos var1) {
      ClientPlayerEntity var2 = var0.field_1724;
      if (var2 != null && var0.field_1687 != null) {
         BlockState var3 = var0.field_1687.method_8320(var1);
         VoxelShape var4 = var3.method_26218(var0.field_1687, var1);
         if (var4.method_1110()) {
            return null;
         } else {
            Box var5 = var4.method_1107().method_996(var1);
            Vec3d var6 = var2.method_33571();
            double var7 = var2.method_55754();

            for (Direction var12 : ORDER) {
               float[] var13;
               BlockHitResult var14;
               Vec3d var15;
               if (faces(var5, var12, var6)
                  && !((var15 = faceCenter(var5, var12)).method_1022(var6) > var7)
                  && (var14 = trace(var0, var2, (var13 = PlacementUtil.calcRotation(var6, var15))[0], var13[1], var7)) != null
                  && var14.method_17783() == Type.field_1332
                  && var14.method_17777().equals(var1)
                  && var14.method_17780() == var12) {
                  return new Aim.Shot(var14, var13[0], var13[1]);
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   static Aim.Shot against(MinecraftClient var0, BlockPos var1, Fluid var2) {
      return var0.field_1724 == null ? null : against(var0, var1, var2, var0.field_1724.method_33571());
   }

   static Aim.Shot against(MinecraftClient var0, BlockPos var1, Fluid var2, Vec3d var3) {
      ClientPlayerEntity var4 = var0.field_1724;
      if (var4 != null && var0.field_1687 != null) {
         double var5 = var4.method_55754();

         for (Direction var10 : ORDER) {
            BlockPos var15 = var1.method_10093(var10.method_10153());
            BlockState var16 = var0.field_1687.method_8320(var15);
            Box var11;
            VoxelShape var12;
            FluidFillable var13;
            Block var14;
            if (!var16.method_45474()
               && !clickable(var0, var15, var16)
               && (
                  var2 == null
                     || !((var14 = var16.method_26204()) instanceof FluidFillable)
                     || !(var13 = (FluidFillable)var14).method_10310(var4, var0.field_1687, var15, var16, var2)
               )
               && !(var12 = var16.method_26218(var0.field_1687, var15)).method_1110()
               && faces(var11 = var12.method_1107().method_996(var15), var10, var3)) {
               for (double[] var20 : FACE_POINTS) {
                  Vec3d var23 = facePoint(var11, var10, var20[0], var20[1]);
                  float[] var21;
                  BlockHitResult var22;
                  if (!(var23.method_1022(var3) > var5)
                     && (var22 = PlacementUtil.rayTraceFrom(var4, var3, (var21 = PlacementUtil.calcRotation(var3, var23))[0], var21[1], var5)) != null
                     && var22.method_17783() == Type.field_1332
                     && var22.method_17777().equals(var15)
                     && var22.method_17780() == var10) {
                     return new Aim.Shot(var22, var21[0], var21[1]);
                  }
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static Vec3d facePoint(Box var0, Direction var1, double var2, double var4) {
      double var6 = var0.field_1323 + (var0.field_1320 - var0.field_1323) * var2;
      double var8 = var0.field_1321 + (var0.field_1324 - var0.field_1321) * (var1.method_10166() == Axis.field_11052 ? var4 : var2);
      double var10 = var0.field_1322 + (var0.field_1325 - var0.field_1322) * var4;

      return switch (var1) {
         case field_11036 -> new Vec3d(var6, var0.field_1325, var8);
         case field_11033 -> new Vec3d(var6, var0.field_1322, var8);
         case field_11043 -> new Vec3d(var6, var10, var0.field_1321);
         case field_11035 -> new Vec3d(var6, var10, var0.field_1324);
         case field_11039 -> new Vec3d(var0.field_1323, var10, var8);
         case field_11034 -> new Vec3d(var0.field_1320, var10, var8);
         default -> throw new MatchException(null, null);
      };
   }

   static Aim.Shot side(MinecraftClient var0, BlockPos var1, Direction var2) {
      return side(var0, var1, var2, new double[][]{{0.5, 0.85}, {0.5, 0.5}, {0.5, 0.2}});
   }

   static Aim.Shot mine(MinecraftClient var0, BlockPos var1, Direction var2, double var3, double var5) {
      return side(var0, var1, var2, new double[][]{{var3, var5}, {0.5, 0.5}, {0.5, 0.85}, {0.5, 0.2}});
   }

   private static Aim.Shot side(MinecraftClient var0, BlockPos var1, Direction var2, double[][] var3) {
      ClientPlayerEntity var4 = var0.field_1724;
      if (var4 != null && var0.field_1687 != null) {
         VoxelShape var5 = var0.field_1687.method_8320(var1).method_26218(var0.field_1687, var1);
         if (var5.method_1110()) {
            return null;
         } else {
            Box var6 = var5.method_1107().method_996(var1);
            Vec3d var7 = var4.method_33571();
            double var8 = var4.method_55754();
            if (!faces(var6, var2, var7)) {
               return null;
            } else {
               for (double[] var13 : var3) {
                  Vec3d var16 = facePoint(var6, var2, var13[0], var13[1]);
                  float[] var14;
                  BlockHitResult var15;
                  if (!(var16.method_1022(var7) > var8)
                     && (var15 = trace(var0, var4, (var14 = PlacementUtil.calcRotation(var7, var16))[0], var14[1], var8)) != null
                     && var15.method_17783() == Type.field_1332
                     && var15.method_17777().equals(var1)
                     && var15.method_17780() == var2) {
                     return new Aim.Shot(var15, var14[0], var14[1]);
                  }
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   static Aim.Shot water(MinecraftClient var0, BlockPos var1) {
      ClientPlayerEntity var2 = var0.field_1724;
      if (var2 != null && var0.field_1687 != null) {
         Vec3d var3 = var2.method_33571();
         double var4 = var2.method_55754();
         Vec3d var6 = new Vec3d(var1.method_10263() + 0.5, var1.method_10264() + 0.85, var1.method_10260() + 0.5);
         if (!(var3.field_1351 <= var6.field_1351) && !(var6.method_1022(var3) > var4)) {
            float[] var7 = PlacementUtil.calcRotation(var3, var6);
            BlockHitResult var8 = trace(var0, var2, var7[0], var7[1], var4, FluidHandling.field_1345);
            return var8 != null && var8.method_17783() == Type.field_1332 && var8.method_17777().equals(var1) ? new Aim.Shot(var8, var7[0], var7[1]) : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   static boolean clickable(MinecraftClient var0, BlockPos var1) {
      return clickable(var0, var1, var0.field_1687.method_8320(var1));
   }

   private static boolean clickable(MinecraftClient var0, BlockPos var1, BlockState var2) {
      Block var3 = var2.method_26204();
      return var0.field_1687.method_8321(var1) != null
         || var3 instanceof DoorBlock
         || var3 instanceof TrapdoorBlock
         || var3 instanceof FenceGateBlock
         || var3 instanceof ButtonBlock
         || var3 instanceof LeverBlock
         || var3 instanceof AbstractRedstoneGateBlock
         || var3 instanceof NoteBlock;
   }

   static void look(MinecraftClient var0, float var1, float var2) {
      ClientPlayerEntity var3 = var0.field_1724;
      if (var3 != null) {
         clearMouse(var0);
         motion = null;
         float var4 = var3.method_36454();
         float var5 = mouseStep(var0);
         var3.method_36456(var4 + mouse(MathHelper.method_15393(var1 - var4), var5));
         var3.method_36457(MathHelper.method_15363(var3.method_36455() + mouse(var2 - var3.method_36455(), var5), -90.0F, 90.0F));
      }
   }

   static float mouseStep(MinecraftClient var0) {
      double var1 = (Double)var0.field_1690.method_42495().method_41753() * 0.6 + 0.2;
      return (float)(var1 * var1 * var1 * 8.0 * 0.15);
   }

   private static float mouse(float var0, float var1) {
      return var1 <= 0.0F ? var0 : Math.round(var0 / var1) * var1;
   }

   public static float turn(MinecraftClient var0, float var1, float var2, float var3) {
      ClientPlayerEntity var6 = var0.field_1724;
      if (var6 == null) {
         return 0.0F;
      } else {
         clearMouse(var0);
         float var7 = var6.method_36454();
         float var8 = var6.method_36455();
         float var9 = MathHelper.method_15393(var1 - var7);
         float var10 = var2 - var8;
         float var4;
         float var5;
         if (Math.abs(var9) < 0.6F && Math.abs(var10) < 0.6F) {
            motion = null;
            var5 = var9;
            var4 = var10;
         } else {
            if (motion != null && !motion.done() && !motion.drifted(var1, var2)) {
               motion.retarget(var1, var2);
            } else {
               motion = Aim.Motion.start(var7, var8, var1, var2, var3);
            }

            float[] var11 = motion.next();
            var5 = MathHelper.method_15363(MathHelper.method_15393(var11[0] - var7), -var3, var3);
            var4 = MathHelper.method_15363(var11[1] - var8, -var3, var3);
         }

         float var12 = mouseStep(var0);
         var6.method_36456(var7 + mouse(var5, var12));
         var6.method_36457(MathHelper.method_15363(var8 + mouse(var4, var12), -90.0F, 90.0F));
         return Math.abs(MathHelper.method_15393(var1 - var6.method_36454()));
      }
   }

   private static void clearMouse(MinecraftClient var0) {
      if (var0.field_1729 != null) {
         AccessorMouseHandler var1 = (AccessorMouseHandler)var0.field_1729;
         var1.easyplace$setAccumulatedDX(0.0);
         var1.easyplace$setAccumulatedDY(0.0);
      }
   }

   static boolean looking(ClientPlayerEntity var0, float var1, float var2) {
      return Math.abs(MathHelper.method_15393(var0.method_36454() - var1)) < 0.5F && Math.abs(var0.method_36455() - var2) < 0.5F;
   }

   static BlockHitResult trace(MinecraftClient var0, ClientPlayerEntity var1, float var2, float var3, double var4) {
      return trace(var0, var1, var2, var3, var4, FluidHandling.field_1348);
   }

   private static BlockHitResult trace(MinecraftClient var0, ClientPlayerEntity var1, float var2, float var3, double var4, FluidHandling var6) {
      Vec3d var7 = var1.method_33571();
      double var8 = Math.toRadians(var2);
      double var10 = Math.cos(Math.toRadians(var3));
      Vec3d var12 = new Vec3d(-Math.sin(var8) * var10, -Math.sin(Math.toRadians(var3)), Math.cos(var8) * var10);
      return var0.field_1687.method_17742(new RaycastContext(var7, var7.method_1019(var12.method_1021(var4)), ShapeType.field_17559, var6, var1));
   }

   private static boolean faces(Box var0, Direction var1, Vec3d var2) {
      double var3 = 0.02;

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

   private static Vec3d faceCenter(Box var0, Direction var1) {
      Vec3d var2 = var0.method_1005();

      return switch (var1) {
         case field_11036 -> new Vec3d(var2.field_1352, var0.field_1325, var2.field_1350);
         case field_11033 -> new Vec3d(var2.field_1352, var0.field_1322, var2.field_1350);
         case field_11043 -> new Vec3d(var2.field_1352, var2.field_1351, var0.field_1321);
         case field_11035 -> new Vec3d(var2.field_1352, var2.field_1351, var0.field_1324);
         case field_11039 -> new Vec3d(var0.field_1323, var2.field_1351, var2.field_1350);
         case field_11034 -> new Vec3d(var0.field_1320, var2.field_1351, var2.field_1350);
         default -> throw new MatchException(null, null);
      };
   }

   @Environment(EnvType.CLIENT)
   private static final class Motion {
      private final float startYaw;
      private final float startPitch;
      private final float bend;
      private final int ticks;
      private float endYaw;
      private float endPitch;
      private float targetYaw;
      private float targetPitch;
      private int elapsed;

      private Motion(float var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8) {
         this.startYaw = var1;
         this.startPitch = var2;
         this.endYaw = var3;
         this.endPitch = var4;
         this.targetYaw = var5;
         this.targetPitch = var6;
         this.bend = var7;
         this.ticks = var8;
      }

      static Aim.Motion start(float var0, float var1, float var2, float var3, float var4) {
         float var5 = MathHelper.method_15393(var2 - var0);
         float var6 = var3 - var1;
         double var7 = Math.hypot(var5, var6);
         double var9 = (0.07 + 0.055 * Math.log(1.0 + var7 / 3.0) / Math.log(2.0)) * Math.exp(Aim.HAND.nextGaussian() * 0.22);
         int var11 = (int)Math.max(Math.ceil(var7 * 1.9 / var4), (double)Math.max(1L, Math.round(var9 * 20.0)));
         double var12 = var7 > 6.0 ? Aim.HAND.nextGaussian() * 0.045 : 0.0;
         double var14 = Math.min(1.2, var7 * 0.015);
         float var16 = (float)(var0 + var5 * (1.0 + var12) + Aim.HAND.nextGaussian() * var14);
         float var17 = (float)(var1 + var6 * (1.0 + var12) + Aim.HAND.nextGaussian() * var14 * 0.7);
         float var18 = (float)(Aim.HAND.nextGaussian() * 0.06);
         return new Aim.Motion(var0, var1, var16, var17, var0 + var5, var3, var18, var11);
      }

      float[] next() {
         this.elapsed++;
         double var1 = Math.min(1.0, (double)this.elapsed / this.ticks);
         double var3 = var1 * var1 * var1 * (10.0 - 15.0 * var1 + 6.0 * var1 * var1);
         double var5 = Math.sin(3.141592653589793 * var1) * this.bend;
         float var7 = this.endYaw - this.startYaw;
         float var8 = this.endPitch - this.startPitch;
         return new float[]{(float)(this.startYaw + var7 * var3 - var8 * var5), (float)(this.startPitch + var8 * var3 + var7 * var5)};
      }

      boolean done() {
         return this.elapsed >= this.ticks;
      }

      boolean drifted(float var1, float var2) {
         return Math.hypot(MathHelper.method_15393(var1 - this.targetYaw), var2 - this.targetPitch) > 25.0;
      }

      void retarget(float var1, float var2) {
         float var3 = MathHelper.method_15393(var1 - this.targetYaw);
         float var4 = var2 - this.targetPitch;
         this.endYaw += var3;
         this.endPitch += var4;
         this.targetYaw += var3;
         this.targetPitch = var2;
      }
   }

   @Environment(EnvType.CLIENT)
   record Shot(BlockHitResult hit, float yaw, float pitch) {
   }
}
