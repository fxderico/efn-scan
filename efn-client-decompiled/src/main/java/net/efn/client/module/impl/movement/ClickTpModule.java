package net.efn.client.module.impl.movement;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.FreelookModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class ClickTpModule extends ClientModule {
   private static final double RAYCAST_DISTANCE = 210.0;
   private static final double BLOCKS_PER_PACKET = 10.0;
   private static final int MAX_PADDING_PACKETS = 19;

   public ClickTpModule() {
      super("click-tp", "Click TP", "Teleports you to the block you click on.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
   }

   private void onPostTick(TickEvent tickEvent) {
      MinecraftClient var5 = MinecraftClient.getInstance();
      ClientPlayerEntity var6 = var5.player;
      ClientWorld var7 = var5.world;
      if (var6 != null && var7 != null) {
         if (var6.getInventory().getSelectedStack().getUseAction() == UseAction.NONE) {
            if (var5.options.useKey.isPressed()) {
               Camera var8 = var5.gameRenderer.getCamera();
               Vec3d var9 = var8.getCameraPos();
               Vec3d var10 = Vec3d.fromPolar(var8.getPitch(), var8.getYaw());
               FreecamModule var11 = ModuleRegistry.get(FreecamModule.class);
               boolean var12 = var11 != null && var11.isEnabled();
               HitResult var4 = var12 ? pickFromCamera(var6, var7, var9, var10) : var5.crosshairTarget;
               if (var4 != null) {
                  if (var4.getType() == Type.ENTITY && var6.interact(((EntityHitResult)var4).getEntity(), Hand.MAIN_HAND) != ActionResult.PASS) {
                     return;
                  }

                  if (var4.getType() == Type.BLOCK && var6.getMainHandStack().getItem() instanceof BlockItem) {
                     return;
                  }
               }

               BlockHitResult var3;
               if ((var3 = var7.raycast(new RaycastContext(var9, var9.add(var10.multiply(210.0)), ShapeType.OUTLINE, FluidHandling.NONE, var6))).getType()
                  == Type.BLOCK) {
                  FreelookModule var2;
                  if (!var3.isInsideBlock() || var12 || (var2 = ModuleRegistry.get(FreelookModule.class)) == null || !var2.isEnabled()) {
                     BlockPos var14 = var3.getBlockPos();
                     Direction var15 = var3.getSide();
                     if (var7.getBlockState(var14).onUse(var7, var6, var3) == ActionResult.PASS) {
                        BlockState var16 = var7.getBlockState(var14);
                        VoxelShape var17 = var16.getCollisionShape(var7, var14);
                        if (var17.isEmpty()) {
                           var17 = var16.getOutlineShape(var7, var14);
                        }

                        double var18 = var17.isEmpty() ? 1.0 : var17.getMax(Axis.Y);
                        Vec3d var20 = new Vec3d(var14.getX() + 0.5 + var15.getOffsetX(), var14.getY() + var18, var14.getZ() + 0.5 + var15.getOffsetZ());
                        int var21 = (int)Math.ceil(var6.getEntityPos().distanceTo(var20) / 10.0) - 1;
                        if (var21 > 19) {
                           var21 = 0;
                        }

                        for (int var22 = 0; var22 < var21; var22++) {
                           var6.networkHandler.sendPacket(new OnGroundOnly(true, true));
                        }

                        var6.networkHandler.sendPacket(new PositionAndOnGround(var20.x, var20.y, var20.z, true, true));
                        var6.setPosition(var20);
                     }
                  }
               }
            }
         }
      }
   }

   private static HitResult pickFromCamera(ClientPlayerEntity clientPlayerEntity, ClientWorld clientWorld, Vec3d vec3d, Vec3d vec3d2) {
      double var7 = clientPlayerEntity.getBlockInteractionRange();
      double var9 = clientPlayerEntity.getEntityInteractionRange();
      double var11 = Math.max(var7, var9);
      double var13 = var11 * var11;
      BlockHitResult var15 = clientWorld.raycast(
         new RaycastContext(vec3d, vec3d.add(vec3d2.multiply(var11)), ShapeType.OUTLINE, FluidHandling.NONE, clientPlayerEntity)
      );
      double var16 = var15.getPos().squaredDistanceTo(vec3d);
      if (var15.getType() != Type.MISS) {
         var13 = var16;
         var11 = Math.sqrt(var16);
      }

      Vec3d var5;
      EntityHitResult var6;
      if ((
               var6 = ProjectileUtil.raycast(
                  clientPlayerEntity, vec3d, var5 = vec3d.add(vec3d2.multiply(var11)), new Box(vec3d, var5).expand(1.0), EntityPredicates.CAN_HIT, var13
               )
            )
            != null
         && var6.getPos().squaredDistanceTo(vec3d) < var16) {
         return var6.getPos().isInRange(vec3d, var9) ? var6 : null;
      } else {
         return var15.getPos().isInRange(vec3d, var7) ? var15 : null;
      }
   }
}
