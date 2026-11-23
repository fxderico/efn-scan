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
   private static final Direction[] ORDER = new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};
   private static final double[][] FACE_POINTS = new double[][]{{0.5, 0.5}, {0.5, 0.85}, {0.5, 0.15}, {0.15, 0.5}, {0.85, 0.5}};
   private static final float SETTLED = 0.6F;
   private static final float RETARGET = 25.0F;
   private static Aim.Motion motion;
   private static final Random HAND = new Random();
   public static final float HAND_TURN = 22.0F;

   private Aim() {
   }

   static Aim.Shot at(MinecraftClient minecraftClient, BlockPos blockPos) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 != null && minecraftClient.world != null) {
         BlockState var3 = minecraftClient.world.getBlockState(blockPos);
         VoxelShape var4 = var3.getOutlineShape(minecraftClient.world, blockPos);
         if (var4.isEmpty()) {
            return null;
         } else {
            Box var5 = var4.getBoundingBox().offset(blockPos);
            Vec3d var6 = var2.getEyePos();
            double var7 = var2.getBlockInteractionRange();

            for (Direction var12 : ORDER) {
               float[] var13;
               BlockHitResult var14;
               Vec3d var15;
               if (faces(var5, var12, var6)
                  && !((var15 = faceCenter(var5, var12)).distanceTo(var6) > var7)
                  && (var14 = trace(minecraftClient, var2, (var13 = PlacementUtil.calcRotation(var6, var15))[0], var13[1], var7)) != null
                  && var14.getType() == Type.BLOCK
                  && var14.getBlockPos().equals(blockPos)
                  && var14.getSide() == var12) {
                  return new Aim.Shot(var14, var13[0], var13[1]);
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   static Aim.Shot against(MinecraftClient minecraftClient, BlockPos blockPos, Fluid fluid) {
      return minecraftClient.player == null ? null : against(minecraftClient, blockPos, fluid, minecraftClient.player.getEyePos());
   }

   static Aim.Shot against(MinecraftClient minecraftClient, BlockPos blockPos, Fluid fluid, Vec3d vec3d) {
      ClientPlayerEntity var4 = minecraftClient.player;
      if (var4 != null && minecraftClient.world != null) {
         double var5 = var4.getBlockInteractionRange();

         for (Direction var10 : ORDER) {
            BlockPos var15 = blockPos.offset(var10.getOpposite());
            BlockState var16 = minecraftClient.world.getBlockState(var15);
            Box var11;
            VoxelShape var12;
            FluidFillable var13;
            Block var14;
            if (!var16.isReplaceable()
               && !clickable(minecraftClient, var15, var16)
               && (
                  fluid == null
                     || !((var14 = var16.getBlock()) instanceof FluidFillable)
                     || !(var13 = (FluidFillable)var14).canFillWithFluid(var4, minecraftClient.world, var15, var16, fluid)
               )
               && !(var12 = var16.getOutlineShape(minecraftClient.world, var15)).isEmpty()
               && faces(var11 = var12.getBoundingBox().offset(var15), var10, vec3d)) {
               for (double[] var20 : FACE_POINTS) {
                  Vec3d var23 = facePoint(var11, var10, var20[0], var20[1]);
                  float[] var21;
                  BlockHitResult var22;
                  if (!(var23.distanceTo(vec3d) > var5)
                     && (var22 = PlacementUtil.rayTraceFrom(var4, vec3d, (var21 = PlacementUtil.calcRotation(vec3d, var23))[0], var21[1], var5)) != null
                     && var22.getType() == Type.BLOCK
                     && var22.getBlockPos().equals(var15)
                     && var22.getSide() == var10) {
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

   private static Vec3d facePoint(Box box, Direction direction, double d, double e) {
      double var6 = box.minX + (box.maxX - box.minX) * d;
      double var8 = box.minZ + (box.maxZ - box.minZ) * (direction.getAxis() == Axis.Y ? e : d);
      double var10 = box.minY + (box.maxY - box.minY) * e;

      return switch (direction) {
         case UP -> new Vec3d(var6, box.maxY, var8);
         case DOWN -> new Vec3d(var6, box.minY, var8);
         case NORTH -> new Vec3d(var6, var10, box.minZ);
         case SOUTH -> new Vec3d(var6, var10, box.maxZ);
         case WEST -> new Vec3d(box.minX, var10, var8);
         case EAST -> new Vec3d(box.maxX, var10, var8);
      };
   }

   static Aim.Shot side(MinecraftClient minecraftClient, BlockPos blockPos, Direction direction) {
      return side(minecraftClient, blockPos, direction, new double[][]{{0.5, 0.85}, {0.5, 0.5}, {0.5, 0.2}});
   }

   static Aim.Shot mine(MinecraftClient minecraftClient, BlockPos blockPos, Direction direction, double d, double e) {
      return side(minecraftClient, blockPos, direction, new double[][]{{d, e}, {0.5, 0.5}, {0.5, 0.85}, {0.5, 0.2}});
   }

   private static Aim.Shot side(MinecraftClient minecraftClient, BlockPos blockPos, Direction direction, double[][] ds) {
      ClientPlayerEntity var4 = minecraftClient.player;
      if (var4 != null && minecraftClient.world != null) {
         VoxelShape var5 = minecraftClient.world.getBlockState(blockPos).getOutlineShape(minecraftClient.world, blockPos);
         if (var5.isEmpty()) {
            return null;
         } else {
            Box var6 = var5.getBoundingBox().offset(blockPos);
            Vec3d var7 = var4.getEyePos();
            double var8 = var4.getBlockInteractionRange();
            if (!faces(var6, direction, var7)) {
               return null;
            } else {
               for (double[] var13 : ds) {
                  Vec3d var16 = facePoint(var6, direction, var13[0], var13[1]);
                  float[] var14;
                  BlockHitResult var15;
                  if (!(var16.distanceTo(var7) > var8)
                     && (var15 = trace(minecraftClient, var4, (var14 = PlacementUtil.calcRotation(var7, var16))[0], var14[1], var8)) != null
                     && var15.getType() == Type.BLOCK
                     && var15.getBlockPos().equals(blockPos)
                     && var15.getSide() == direction) {
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

   static Aim.Shot water(MinecraftClient minecraftClient, BlockPos blockPos) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 != null && minecraftClient.world != null) {
         Vec3d var3 = var2.getEyePos();
         double var4 = var2.getBlockInteractionRange();
         Vec3d var6 = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.85, blockPos.getZ() + 0.5);
         if (!(var3.y <= var6.y) && !(var6.distanceTo(var3) > var4)) {
            float[] var7 = PlacementUtil.calcRotation(var3, var6);
            BlockHitResult var8 = trace(minecraftClient, var2, var7[0], var7[1], var4, FluidHandling.SOURCE_ONLY);
            return var8 != null && var8.getType() == Type.BLOCK && var8.getBlockPos().equals(blockPos) ? new Aim.Shot(var8, var7[0], var7[1]) : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   static boolean clickable(MinecraftClient minecraftClient, BlockPos blockPos) {
      return clickable(minecraftClient, blockPos, minecraftClient.world.getBlockState(blockPos));
   }

   private static boolean clickable(MinecraftClient minecraftClient, BlockPos blockPos, BlockState blockState) {
      Block var3 = blockState.getBlock();
      return minecraftClient.world.getBlockEntity(blockPos) != null
         || var3 instanceof DoorBlock
         || var3 instanceof TrapdoorBlock
         || var3 instanceof FenceGateBlock
         || var3 instanceof ButtonBlock
         || var3 instanceof LeverBlock
         || var3 instanceof AbstractRedstoneGateBlock
         || var3 instanceof NoteBlock;
   }

   static void look(MinecraftClient minecraftClient, float f, float g) {
      ClientPlayerEntity var3 = minecraftClient.player;
      if (var3 != null) {
         clearMouse(minecraftClient);
         motion = null;
         float var4 = var3.getYaw();
         float var5 = mouseStep(minecraftClient);
         var3.setYaw(var4 + mouse(MathHelper.wrapDegrees(f - var4), var5));
         var3.setPitch(MathHelper.clamp(var3.getPitch() + mouse(g - var3.getPitch(), var5), -90.0F, 90.0F));
      }
   }

   static float mouseStep(MinecraftClient minecraftClient) {
      double var1 = minecraftClient.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
      return (float)(var1 * var1 * var1 * 8.0 * 0.15);
   }

   private static float mouse(float f, float g) {
      return g <= 0.0F ? f : Math.round(f / g) * g;
   }

   public static float turn(MinecraftClient minecraftClient, float f, float g, float h) {
      ClientPlayerEntity var6 = minecraftClient.player;
      if (var6 == null) {
         return 0.0F;
      } else {
         clearMouse(minecraftClient);
         float var7 = var6.getYaw();
         float var8 = var6.getPitch();
         float var9 = MathHelper.wrapDegrees(f - var7);
         float var10 = g - var8;
         float var4;
         float var5;
         if (Math.abs(var9) < 0.6F && Math.abs(var10) < 0.6F) {
            motion = null;
            var5 = var9;
            var4 = var10;
         } else {
            if (motion != null && !motion.done() && !motion.drifted(f, g)) {
               motion.retarget(f, g);
            } else {
               motion = Aim.Motion.start(var7, var8, f, g, h);
            }

            float[] var11 = motion.next();
            var5 = MathHelper.clamp(MathHelper.wrapDegrees(var11[0] - var7), -h, h);
            var4 = MathHelper.clamp(var11[1] - var8, -h, h);
         }

         float var12 = mouseStep(minecraftClient);
         var6.setYaw(var7 + mouse(var5, var12));
         var6.setPitch(MathHelper.clamp(var8 + mouse(var4, var12), -90.0F, 90.0F));
         return Math.abs(MathHelper.wrapDegrees(f - var6.getYaw()));
      }
   }

   private static void clearMouse(MinecraftClient minecraftClient) {
      if (minecraftClient.mouse != null) {
         AccessorMouseHandler var1 = (AccessorMouseHandler)minecraftClient.mouse;
         var1.easyplace$setAccumulatedDX(0.0);
         var1.easyplace$setAccumulatedDY(0.0);
      }
   }

   static boolean looking(ClientPlayerEntity clientPlayerEntity, float f, float g) {
      return Math.abs(MathHelper.wrapDegrees(clientPlayerEntity.getYaw() - f)) < 0.5F && Math.abs(clientPlayerEntity.getPitch() - g) < 0.5F;
   }

   static BlockHitResult trace(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, float f, float g, double d) {
      return trace(minecraftClient, clientPlayerEntity, f, g, d, FluidHandling.NONE);
   }

   private static BlockHitResult trace(
      MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, float f, float g, double d, FluidHandling fluidHandling
   ) {
      Vec3d var7 = clientPlayerEntity.getEyePos();
      double var8 = Math.toRadians(f);
      double var10 = Math.cos(Math.toRadians(g));
      Vec3d var12 = new Vec3d(-Math.sin(var8) * var10, -Math.sin(Math.toRadians(g)), Math.cos(var8) * var10);
      return minecraftClient.world.raycast(new RaycastContext(var7, var7.add(var12.multiply(d)), ShapeType.OUTLINE, fluidHandling, clientPlayerEntity));
   }

   private static boolean faces(Box box, Direction direction, Vec3d vec3d) {
      double var3 = 0.02;

      return switch (direction) {
         case UP -> vec3d.y > box.maxY + var3;
         case DOWN -> vec3d.y < box.minY - var3;
         case NORTH -> vec3d.z < box.minZ - var3;
         case SOUTH -> vec3d.z > box.maxZ + var3;
         case WEST -> vec3d.x < box.minX - var3;
         case EAST -> vec3d.x > box.maxX + var3;
      };
   }

   private static Vec3d faceCenter(Box box, Direction direction) {
      Vec3d var2 = box.getCenter();

      return switch (direction) {
         case UP -> new Vec3d(var2.x, box.maxY, var2.z);
         case DOWN -> new Vec3d(var2.x, box.minY, var2.z);
         case NORTH -> new Vec3d(var2.x, var2.y, box.minZ);
         case SOUTH -> new Vec3d(var2.x, var2.y, box.maxZ);
         case WEST -> new Vec3d(box.minX, var2.y, var2.z);
         case EAST -> new Vec3d(box.maxX, var2.y, var2.z);
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

      private Motion(float f, float g, float h, float i, float j, float k, float l, int m) {
         this.startYaw = f;
         this.startPitch = g;
         this.endYaw = h;
         this.endPitch = i;
         this.targetYaw = j;
         this.targetPitch = k;
         this.bend = l;
         this.ticks = m;
      }

      static Aim.Motion start(float f, float g, float h, float i, float j) {
         float var5 = MathHelper.wrapDegrees(h - f);
         float var6 = i - g;
         double var7 = Math.hypot(var5, var6);
         double var9 = (0.07 + 0.055 * Math.log(1.0 + var7 / 3.0) / Math.log(2.0)) * Math.exp(Aim.HAND.nextGaussian() * 0.22);
         int var11 = (int)Math.max(Math.ceil(var7 * 1.9 / j), (double)Math.max(1L, Math.round(var9 * 20.0)));
         double var12 = var7 > 6.0 ? Aim.HAND.nextGaussian() * 0.045 : 0.0;
         double var14 = Math.min(1.2, var7 * 0.015);
         float var16 = (float)(f + var5 * (1.0 + var12) + Aim.HAND.nextGaussian() * var14);
         float var17 = (float)(g + var6 * (1.0 + var12) + Aim.HAND.nextGaussian() * var14 * 0.7);
         float var18 = (float)(Aim.HAND.nextGaussian() * 0.06);
         return new Aim.Motion(f, g, var16, var17, f + var5, i, var18, var11);
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

      boolean drifted(float f, float g) {
         return Math.hypot(MathHelper.wrapDegrees(f - this.targetYaw), g - this.targetPitch) > 25.0;
      }

      void retarget(float f, float g) {
         float var3 = MathHelper.wrapDegrees(f - this.targetYaw);
         float var4 = g - this.targetPitch;
         this.endYaw += var3;
         this.endPitch += var4;
         this.targetYaw += var3;
         this.targetPitch = g;
      }
   }

   @Environment(EnvType.CLIENT)
   record Shot(BlockHitResult hit, float yaw, float pitch) {
   }
}
