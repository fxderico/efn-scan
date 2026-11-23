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

   private void onPostTick(TickEvent var1) {
      MinecraftClient var5 = MinecraftClient.method_1551();
      ClientPlayerEntity var6 = var5.field_1724;
      ClientWorld var7 = var5.field_1687;
      if (var6 != null && var7 != null) {
         if (var6.method_31548().method_7391().method_7976() == UseAction.field_8952) {
            if (var5.field_1690.field_1904.method_1434()) {
               Camera var8 = var5.field_1773.method_19418();
               Vec3d var9 = var8.method_71156();
               Vec3d var10 = Vec3d.method_1030(var8.method_19329(), var8.method_19330());
               FreecamModule var11 = ModuleRegistry.get(FreecamModule.class);
               boolean var12 = var11 != null && var11.isEnabled();
               HitResult var4 = var12 ? pickFromCamera(var6, var7, var9, var10) : var5.field_1765;
               if (var4 != null) {
                  if (var4.method_17783() == Type.field_1331
                     && var6.method_7287(((EntityHitResult)var4).method_17782(), Hand.field_5808) != ActionResult.field_5811) {
                     return;
                  }

                  if (var4.method_17783() == Type.field_1332 && var6.method_6047().method_7909() instanceof BlockItem) {
                     return;
                  }
               }

               BlockHitResult var3;
               if ((var3 = var7.method_17742(
                        new RaycastContext(var9, var9.method_1019(var10.method_1021(210.0)), ShapeType.field_17559, FluidHandling.field_1348, var6)
                     ))
                     .method_17783()
                  == Type.field_1332) {
                  FreelookModule var2;
                  if (!var3.method_17781() || var12 || (var2 = ModuleRegistry.get(FreelookModule.class)) == null || !var2.isEnabled()) {
                     BlockPos var14 = var3.method_17777();
                     Direction var15 = var3.method_17780();
                     if (var7.method_8320(var14).method_55781(var7, var6, var3) == ActionResult.field_5811) {
                        BlockState var16 = var7.method_8320(var14);
                        VoxelShape var17 = var16.method_26220(var7, var14);
                        if (var17.method_1110()) {
                           var17 = var16.method_26218(var7, var14);
                        }

                        double var18 = var17.method_1110() ? 1.0 : var17.method_1105(Axis.field_11052);
                        Vec3d var20 = new Vec3d(
                           var14.method_10263() + 0.5 + var15.method_10148(), var14.method_10264() + var18, var14.method_10260() + 0.5 + var15.method_10165()
                        );
                        int var21 = (int)Math.ceil(var6.method_73189().method_1022(var20) / 10.0) - 1;
                        if (var21 > 19) {
                           var21 = 0;
                        }

                        for (int var22 = 0; var22 < var21; var22++) {
                           var6.field_3944.method_52787(new OnGroundOnly(true, true));
                        }

                        var6.field_3944.method_52787(new PositionAndOnGround(var20.field_1352, var20.field_1351, var20.field_1350, true, true));
                        var6.method_33574(var20);
                     }
                  }
               }
            }
         }
      }
   }

   private static HitResult pickFromCamera(ClientPlayerEntity var0, ClientWorld var1, Vec3d var2, Vec3d var3) {
      double var7 = var0.method_55754();
      double var9 = var0.method_55755();
      double var11 = Math.max(var7, var9);
      double var13 = var11 * var11;
      BlockHitResult var15 = var1.method_17742(
         new RaycastContext(var2, var2.method_1019(var3.method_1021(var11)), ShapeType.field_17559, FluidHandling.field_1348, var0)
      );
      double var16 = var15.method_17784().method_1025(var2);
      if (var15.method_17783() != Type.field_1333) {
         var13 = var16;
         var11 = Math.sqrt(var16);
      }

      Vec3d var5;
      EntityHitResult var6;
      if ((
               var6 = ProjectileUtil.method_18075(
                  var0, var2, var5 = var2.method_1019(var3.method_1021(var11)), new Box(var2, var5).method_1014(1.0), EntityPredicates.field_52443, var13
               )
            )
            != null
         && var6.method_17784().method_1025(var2) < var16) {
         return var6.method_17784().method_24802(var2, var9) ? var6 : null;
      } else {
         return var15.method_17784().method_24802(var2, var7) ? var15 : null;
      }
   }
}
