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

   public static PearlTrajectorySimulator.SimulationResult simulate(World var0, ClientPlayerEntity var1, float var2) {
      Vec3d var3 = var1.method_5836(var2);
      Vec3d var4 = new Vec3d(var3.field_1352, var3.field_1351 - 0.1, var3.field_1350);
      Vec3d var5 = var1.method_5828(var2).method_1021(EnderPearlItem.field_55033);
      Vec3d var6 = var1.method_60478();
      var5 = var5.method_1031(var6.field_1352, var1.method_24828() ? 0.0 : var6.field_1351, var6.field_1350);
      return simulate(var0, var4, var5, var1, null);
   }

   public static PearlTrajectorySimulator.SimulationResult simulate(World var0, ProjectileEntity var1) {
      return simulate(var0, var1.method_73189(), var1.method_18798(), var1, var1.method_24921());
   }

   public static PearlTrajectorySimulator.SimulationResult simulate(World var0, Vec3d var1, Vec3d var2, Entity var3, Entity var4) {
      ArrayList var5 = new ArrayList(64);
      var5.add(var1);
      Vec3d var6 = var1;
      BlockHitResult var7 = null;
      Entity var8 = null;
      int var9 = 0;

      while (var9 < 300) {
         var9++;
         Vec3d var16 = var2.method_1031(0.0, -0.03, 0.0);
         Vec3d var11 = var6.method_1019(var2 = var16.method_1021(isInWater(var0, var6) ? 0.800000011920929 : 0.9900000095367432));
         BlockHitResult var12 = var0.method_17742(new RaycastContext(var6, var11, ShapeType.field_17558, FluidHandling.field_1348, var3));
         Vec3d var13 = var12.method_17783() != Type.field_1333 ? var12.method_17784() : var11;
         Entity var14 = firstEntityOnSegment(var0, var6, var13, var3, var4);
         if (var14 != null) {
            Vec3d var15 = var14.method_5829().method_1014(0.3).method_992(var6, var13).orElse(var13);
            var5.add(var15);
            var8 = var14;
            break;
         }

         if (var12 instanceof BlockHitResult && var12.method_17783() != Type.field_1333) {
            var5.add(var13);
            var7 = var12;
            break;
         }

         var6 = var11;
         var5.add(var11);
         if (var11.field_1351 < var0.method_31607() - 64) {
            break;
         }
      }

      return new PearlTrajectorySimulator.SimulationResult(List.copyOf(var5), (Vec3d)var5.getLast(), var7, var8, var9);
   }

   private static Entity firstEntityOnSegment(World var0, Vec3d var1, Vec3d var2, Entity var3, Entity var4) {
      Box var5 = new Box(var1, var2).method_1014(1.0);
      Entity var6 = null;
      double var7 = 1.7976931348623157E308;

      for (Entity var10 : var0.method_8333(var3, var5, PearlTrajectorySimulator::canHit)) {
         double var11;
         Optional var13;
         if (var10 != var4
            && !(var13 = var10.method_5829().method_1014(0.3).method_992(var1, var2)).isEmpty()
            && (var11 = var1.method_1025((Vec3d)var13.get())) < var7) {
            var7 = var11;
            var6 = var10;
         }
      }

      return var6;
   }

   private static boolean canHit(Entity var0) {
      return EntityPredicates.field_6155.test(var0) && var0.method_5863() && !(var0 instanceof ClientPlayerEntity);
   }

   private static boolean isInWater(World var0, Vec3d var1) {
      return var0.method_8316(BlockPos.method_49638(var1)).method_15767(FluidTags.field_15517);
   }

   public static boolean isHoldingPearl(ClientPlayerEntity var0) {
      return var0.method_6047().method_31574(Items.field_8634) || var0.method_6079().method_31574(Items.field_8634);
   }

   @Environment(EnvType.CLIENT)
   public record SimulationResult(List<Vec3d> points, Vec3d landing, BlockHitResult blockHit, Entity entityHit, int ticks) {
      public boolean landed() {
         return this.blockHit != null || this.entityHit != null;
      }
   }
}
