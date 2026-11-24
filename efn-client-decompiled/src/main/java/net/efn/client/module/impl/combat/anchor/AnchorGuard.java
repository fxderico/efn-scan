package net.efn.client.module.impl.combat.anchor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class AnchorGuard {
   public static final float BLAST_POWER = 5.0F;
   private static final double MIN_SHADE = 0.15;
   private static final double FACE_INSET = 0.3;
   private static final double AIM_INSET = 0.1;

   private AnchorGuard() {
   }

   public static AnchorGuard.Spot find(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos, Vec3d vec3d) {
      Vec3d var4 = Vec3d.ofCenter(blockPos);
      Box var5 = clientPlayerEntity.getBoundingBox();
      List var6 = bodyPoints(var5);
      ArrayList var7 = new ArrayList(var6.size());

      for (Vec3d var9 : var6) {
         BlockHitResult var10 = clientWorld.raycast(new RaycastContext(var9, var4, ShapeType.COLLIDER, FluidHandling.NONE, clientPlayerEntity));
         if (var10.getType() == Type.MISS || var10.getBlockPos().equals(blockPos)) {
            var7.add(var9);
         }
      }

      BlockPos var17 = clientPlayerEntity.getBlockPos();
      if (var7.size() < Math.ceil(var6.size() * 0.15)) {
         return new AnchorGuard.Spot(var17, null, null, null);
      } else {
         int var18 = (int)Math.ceil(var7.size() * 0.15);
         Vec3d var19 = clientPlayerEntity.getEyePos();
         LinkedHashSet var11 = new LinkedHashSet();

         for (int var12 = -1; var12 <= 1; var12++) {
            for (int var13 = -1; var13 <= 1; var13++) {
               if (var12 != 0 || var13 != 0) {
                  var11.add(var17.add(var12, 0, var13));
               }
            }
         }

         for (Direction var22 : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            var11.add(blockPos.offset(var22));
         }

         ArrayList var21 = new ArrayList(var11.size());

         for (BlockPos var14 : var11) {
            Box var16 = new Box(var14);
            int var15;
            if (!var14.equals(blockPos) && !var16.intersects(var5) && !var16.raycast(var19, vec3d).isPresent() && (var15 = shade(var16, var4, var7)) >= var18) {
               var21.add(new AnchorGuard.Cell(var14, var15, var16.getCenter().squaredDistanceTo(var4)));
            }
         }

         var21.sort(Comparator.comparingInt(AnchorGuard.Cell::shade).reversed().thenComparingDouble(AnchorGuard.Cell::distance));

         for (AnchorGuard.Cell var25 : var21) {
            BlockState var27 = clientWorld.getBlockState(var25.pos());
            AnchorGuard.Spot var26;
            if (var27.isReplaceable()
               && (
                     var26 = var27.getOutlineShape(clientWorld, var25.pos()).isEmpty()
                        ? supportFor(clientWorld, clientPlayerEntity, var19, var25.pos())
                        : direct(clientWorld, clientPlayerEntity, var19, var25.pos(), var27)
                  )
                  != null) {
               return var26;
            }
         }

         return null;
      }
   }

   public static BlockHitResult hitAlong(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, Rotation rotation, AnchorGuard.Spot spot) {
      BlockHitResult var4 = trace(clientWorld, clientPlayerEntity, rotation);
      if (var4 == null || !var4.getBlockPos().equals(spot.support())) {
         return null;
      } else if (spot.face() != null && var4.getSide() != spot.face()) {
         return null;
      } else {
         BlockPos var5 = Crystals.placedObsidian(clientWorld, clientPlayerEntity, var4, clientWorld.getBlockState(spot.support()));
         return spot.pos().equals(var5) ? var4 : null;
      }
   }

   public static BlockHitResult anchorHit(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      Rotation var3 = ServerRotation.sent();
      BlockHitResult var4 = var3 == null ? null : trace(clientWorld, clientPlayerEntity, var3);
      return var4 != null && var4.getBlockPos().equals(blockPos) ? var4 : null;
   }

   public static boolean isGuardBlock(ItemStack itemStack) {
      if (!(itemStack.getItem() instanceof BlockItem var2)) {
         return false;
      } else {
         Block var3 = var2.getBlock();
         return !(var3 instanceof BlockEntityProvider) && !(var3 instanceof FallingBlock) && var3 != Blocks.RESPAWN_ANCHOR && var3 != Blocks.TNT
            ? var3.getDefaultState().isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)
            : false;
      }
   }

   public static int detonatorSlot(PlayerInventory playerInventory) {
      int var1 = Crystals.hotbarSlotOf(playerInventory, Items.TOTEM_OF_UNDYING);
      if (var1 != -1) {
         return var1;
      } else {
         for (int var2 = 0; var2 < PlayerInventory.getHotbarSize(); var2++) {
            if (Crystals.isCombatItem(playerInventory.getStack(var2))) {
               return var2;
            }
         }

         return -1;
      }
   }

   public static int detonatorRoute(PlayerInventory playerInventory, ItemStack itemStack) {
      if (Crystals.isCombatItem(itemStack)) {
         return -2;
      } else {
         int var2 = detonatorSlot(playerInventory);
         return var2 == -1 ? -1 : var2;
      }
   }

   public static Vec3d anchorAim(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      Vec3d var5 = clientPlayerEntity.getEyePos();
      Box var6 = new Box(blockPos).contract(0.1);
      Rotation var7 = ServerRotation.sent();
      if (var7 != null) {
         Vec3d var8 = Vec3d.fromPolar(var7.pitch(), var7.yaw());
         double var9 = var6.getCenter().subtract(var5).dotProduct(var8);
         if (var9 > 0.0) {
            Vec3d var11 = var5.add(var8.multiply(var9));
            Vec3d var12 = new Vec3d(
               MathHelper.clamp(var11.x, var6.minX, var6.maxX),
               MathHelper.clamp(var11.y, var6.minY, var6.maxY),
               MathHelper.clamp(var11.z, var6.minZ, var6.maxZ)
            );
            BlockHitResult var13 = trace(clientWorld, clientPlayerEntity, ServerRotation.next(clientPlayerEntity, RotationUtils.fromTo(var5, var12)));
            if (var13 != null && var13.getBlockPos().equals(blockPos)) {
               return var12;
            }
         }
      }

      Vec3d var3;
      BlockHitResult var4;
      return (
                  var4 = trace(
                     clientWorld,
                     clientPlayerEntity,
                     ServerRotation.next(
                        clientPlayerEntity,
                        RotationUtils.fromTo(
                           var5,
                           var3 = new Vec3d(
                              MathHelper.clamp(var5.x, var6.minX, var6.maxX),
                              MathHelper.clamp(var5.y, var6.minY, var6.maxY),
                              MathHelper.clamp(var5.z, var6.minZ, var6.maxZ)
                           )
                        )
                     )
                  )
               )
               != null
            && var4.getBlockPos().equals(blockPos)
         ? var3
         : null;
   }

   public static void click(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, BlockHitResult blockHitResult) {
      ItemStack var4 = clientPlayerEntity.getMainHandStack();
      int var5 = var4.getCount();
      ActionResult var6 = minecraftClient.interactionManager.interactBlock(clientPlayerEntity, Hand.MAIN_HAND, blockHitResult);
      Success var3;
      if (var6 instanceof Success && (var3 = (Success)var6).swingSource() == SwingSource.CLIENT) {
         clientPlayerEntity.swingHand(Hand.MAIN_HAND);
         if (!var4.isEmpty() && (var4.getCount() != var5 || clientPlayerEntity.isInCreativeMode())) {
            minecraftClient.gameRenderer.firstPersonRenderer.resetEquipProgress(Hand.MAIN_HAND);
         }
      }

      MeleeAttacks.claimTick(minecraftClient);
   }

   private static AnchorGuard.Spot direct(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, BlockPos blockPos, BlockState blockState) {
      AnchorGuard.Spot var5 = new AnchorGuard.Spot(
         blockPos, blockPos, null, blockState.getOutlineShape(clientWorld, blockPos).getBoundingBox().offset(blockPos).getCenter()
      );
      Rotation var6 = ServerRotation.next(clientPlayerEntity, RotationUtils.fromTo(vec3d, var5.aim()));
      return hitAlong(clientWorld, clientPlayerEntity, var6, var5) != null ? var5 : null;
   }

   public static AnchorGuard.Spot supportFor(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, BlockPos blockPos) {
      AnchorGuard.Spot var4 = null;
      double var5 = 1.7976931348623157E308;

      for (Direction var10 : Direction.values()) {
         BlockPos var14 = blockPos.offset(var10);
         BlockState var15 = clientWorld.getBlockState(var14);
         VoxelShape var13;
         if (!var15.isReplaceable() && !(var13 = var15.getOutlineShape(clientWorld, var14)).isEmpty()) {
            Direction var16 = var10.getOpposite();
            AnchorGuard.Spot var17 = null;

            for (Vec3d var19 : faceAims(var13.getBoundingBox().offset(var14), var16)) {
               AnchorGuard.Spot var20 = new AnchorGuard.Spot(blockPos, var14, var16, var19);
               Rotation var21 = ServerRotation.next(clientPlayerEntity, RotationUtils.fromTo(vec3d, var19));
               if (hitAlong(clientWorld, clientPlayerEntity, var21, var20) != null) {
                  var17 = var20;
                  break;
               }
            }

            if (var17 != null) {
               double var11 = var10 == Direction.DOWN ? -1.0 : vec3d.squaredDistanceTo(var17.aim());
               if (var11 < var5) {
                  var4 = var17;
                  var5 = var11;
               }
            }
         }
      }

      return var4;
   }

   private static List<Vec3d> faceAims(Box box, Direction direction) {
      Axis var2 = direction.getAxis();
      double var3 = direction.getDirection() == AxisDirection.POSITIVE ? box.getMax(var2) : box.getMin(var2);
      Vec3d var5 = box.getCenter().withAxis(var2, var3);
      ArrayList var6 = new ArrayList(5);
      var6.add(var5);

      for (Direction var10 : Direction.values()) {
         Axis var11 = var10.getAxis();
         if (var11 != var2) {
            var6.add(var5.offset(var10, (box.getMax(var11) - box.getMin(var11)) * 0.3));
         }
      }

      return var6;
   }

   public static BlockHitResult trace(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, Rotation rotation) {
      Vec3d var4 = clientPlayerEntity.getEyePos();
      BlockHitResult var5 = clientWorld.raycast(
         new RaycastContext(
            var4,
            var4.add(Vec3d.fromPolar(rotation.pitch(), rotation.yaw()).multiply(clientPlayerEntity.getBlockInteractionRange())),
            ShapeType.OUTLINE,
            FluidHandling.NONE,
            clientPlayerEntity
         )
      );
      return var5.getType() == Type.BLOCK ? var5 : null;
   }

   private static int shade(Box box, Vec3d vec3d, List<Vec3d> list) {
      int var3 = 0;

      for (Vec3d var5 : list) {
         if (box.raycast(var5, vec3d).isPresent()) {
            var3++;
         }
      }

      return var3;
   }

   private static List<Vec3d> bodyPoints(Box box) {
      double var1 = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
      double var3 = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
      double var5 = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
      double var7 = (1.0 - Math.floor(1.0 / var1) * var1) / 2.0;
      double var9 = (1.0 - Math.floor(1.0 / var5) * var5) / 2.0;
      ArrayList var11 = new ArrayList();

      for (double var12 = 0.0; var12 <= 1.0; var12 += var1) {
         for (double var14 = 0.0; var14 <= 1.0; var14 += var3) {
            for (double var16 = 0.0; var16 <= 1.0; var16 += var5) {
               var11.add(
                  new Vec3d(
                     MathHelper.lerp(var12, box.minX, box.maxX) + var7,
                     MathHelper.lerp(var14, box.minY, box.maxY),
                     MathHelper.lerp(var16, box.minZ, box.maxZ) + var9
                  )
               );
            }
         }
      }

      return var11;
   }

   @Environment(EnvType.CLIENT)
   private record Cell(BlockPos pos, int shade, double distance) {
   }

   @Environment(EnvType.CLIENT)
   public record Spot(BlockPos pos, BlockPos support, Direction face, Vec3d aim) {
      public boolean standing() {
         return this.support == null;
      }
   }
}
