package net.efn.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class PearlTrajectorySimulator {
   private static final double SPAWN_EYE_OFFSET = 0.1;
   private static final double GRAVITY = 0.03;
   private static final float AIR_DRAG = 0.99F;
   private static final float WATER_DRAG = 0.8F;
   private static final int MAX_TICKS = 300;
   private static final double ENTITY_MARGIN = 0.3;

   private PearlTrajectorySimulator() {
   }

   public static PearlTrajectorySimulator.SimulationResult simulate(World world, ClientPlayerEntity clientPlayerEntity, float f) {
      Vec3d var3 = clientPlayerEntity.getCameraPosVec(f);
      Vec3d var4 = new Vec3d(var3.x, var3.y - 0.1, var3.z);
      Vec3d var5 = clientPlayerEntity.getRotationVec(f).multiply(EnderPearlItem.POWER);
      Vec3d var6 = clientPlayerEntity.getMovement();
      var5 = var5.add(var6.x, clientPlayerEntity.isOnGround() ? 0.0 : var6.y, var6.z);
      return simulate(world, var4, var5, clientPlayerEntity, null);
   }

   public static PearlTrajectorySimulator.SimulationResult simulate(World world, ProjectileEntity projectileEntity) {
      return simulate(world, projectileEntity.getEntityPos(), projectileEntity.getVelocity(), projectileEntity, projectileEntity.getOwner());
   }

   public static PearlTrajectorySimulator.SimulationResult simulate(World world, Vec3d vec3d, Vec3d vec3d2, Entity entity, Entity entity2) {
      ArrayList var5 = new ArrayList(64);
      var5.add(vec3d);
      Vec3d var6 = vec3d;
      BlockHitResult var7 = null;
      Entity var8 = null;
      int var9 = 0;

      while (var9 < 300) {
         var9++;
         Vec3d var16 = vec3d2.add(0.0, -0.03, 0.0);
         Vec3d var11 = var6.add(vec3d2 = var16.multiply(isInWater(world, var6) ? 0.800000011920929 : 0.9900000095367432));
         BlockHitResult var12 = world.raycast(new RaycastContext(var6, var11, ShapeType.COLLIDER, FluidHandling.NONE, entity));
         Vec3d var13 = var12.getType() != Type.MISS ? var12.getPos() : var11;
         Entity var14 = firstEntityOnSegment(world, var6, var13, entity, entity2);
         if (var14 != null) {
            Vec3d var15 = var14.getBoundingBox().expand(0.3).raycast(var6, var13).orElse(var13);
            var5.add(var15);
            var8 = var14;
            break;
         }

         if (var12 instanceof BlockHitResult && var12.getType() != Type.MISS) {
            var5.add(var13);
            var7 = var12;
            break;
         }

         var6 = var11;
         var5.add(var11);
         if (var11.y < world.getBottomY() - 64) {
            break;
         }
      }

      return new PearlTrajectorySimulator.SimulationResult(List.copyOf(var5), (Vec3d)var5.getLast(), var7, var8, var9);
   }

   private static Entity firstEntityOnSegment(World world, Vec3d vec3d, Vec3d vec3d2, Entity entity, Entity entity2) {
      Box var5 = new Box(vec3d, vec3d2).expand(1.0);
      Entity var6 = null;
      double var7 = 1.7976931348623157E308;

      for (Entity var10 : world.getOtherEntities(entity, var5, PearlTrajectorySimulator::canHit)) {
         double var11;
         Optional var13;
         if (var10 != entity2
            && !(var13 = var10.getBoundingBox().expand(0.3).raycast(vec3d, vec3d2)).isEmpty()
            && (var11 = vec3d.squaredDistanceTo((Vec3d)var13.get())) < var7) {
            var7 = var11;
            var6 = var10;
         }
      }

      return var6;
   }

   private static boolean canHit(Entity entity) {
      return EntityPredicates.EXCEPT_SPECTATOR.test(entity) && entity.canHit() && !(entity instanceof ClientPlayerEntity);
   }

   private static boolean isInWater(World world, Vec3d vec3d) {
      return world.getFluidState(BlockPos.ofFloored(vec3d)).isIn(FluidTags.WATER);
   }

   public static boolean isHoldingPearl(ClientPlayerEntity clientPlayerEntity) {
      return clientPlayerEntity.getMainHandStack().isOf(Items.ENDER_PEARL) || clientPlayerEntity.getOffHandStack().isOf(Items.ENDER_PEARL);
   }

   @Environment(EnvType.CLIENT)
   public record SimulationResult(List<Vec3d> points, Vec3d landing, BlockHitResult blockHit, Entity entityHit, int ticks) {
      public boolean landed() {
         return this.blockHit != null || this.entityHit != null;
      }
   }
}
