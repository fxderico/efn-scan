package net.efn.client.module.impl.movement.entitycontrol;

import net.efn.client.input.EfnKeyPolling;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.movement.BoatFlyModule;
import net.efn.client.module.impl.movement.BoatNoclipModule;
import net.efn.client.module.impl.movement.EntityControlModule;
import net.efn.client.module.impl.movement.EntitySpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class EntityControlVehicleHook {
   private static final double DIAGONAL = 1.0 / Math.sqrt(2.0);
   public static volatile boolean anyActive;

   private EntityControlVehicleHook() {
   }

   public static void refresh() {
      anyActive = EntityControlModule.active != null || EntitySpeedModule.active != null || BoatFlyModule.active != null || BoatNoclipModule.active != null;
   }

   public static Vec3d onEntityMove(Entity var0, MovementType var1, Vec3d var2) {
      if (var1 == MovementType.field_6308 && var0.method_73183().method_8608()) {
         ClientPlayerEntity var3 = MinecraftClient.method_1551().field_1724;
         if (var3 != null && var3.method_5854() == var0 && var0.method_5642() == var3) {
            Vec3d var4 = var0.method_18798();
            BoatNoclipModule var5 = BoatNoclipModule.active;
            if (var5 != null && var0 instanceof AbstractBoatEntity var18) {
               Vec3d var19 = var5.boatMovement(var18, var3);
               var0.method_36456(var3.method_36454());
               if (var2 == var4) {
                  var0.method_18799(var19);
               }

               return var19;
            } else {
               EntityControlModule var6 = EntityControlModule.active;
               EntitySpeedModule var7 = EntitySpeedModule.active;
               BoatFlyModule var8 = BoatFlyModule.active;
               double var9 = var4.field_1352;
               double var11 = var4.field_1351;
               double var13 = var4.field_1350;
               boolean var15 = false;
               if (var7 != null) {
                  if (var7.appliesTo(var0)) {
                     Vec3d var16 = horizontalVelocity(var3, var7.horizontalSpeed());
                     var9 = var16.field_1352;
                     var13 = var16.field_1350;
                     var15 = true;
                  }
               } else if (var8 != null) {
                  Vec3d var20 = horizontalVelocity(var3, var8.horizontalSpeed());
                  var9 = var20.field_1352;
                  var13 = var20.field_1350;
                  var15 = true;
               }

               if (var8 != null) {
                  var11 = var8.verticalVelocity();
                  var15 = true;
               }

               if (var8 != null || var6 != null && var6.locksYaw(var0)) {
                  var0.method_36456(var3.method_36454());
               }

               if (!var15) {
                  return var2;
               } else {
                  Vec3d var17 = new Vec3d(var9, var11, var13);
                  if (var2 == var4) {
                     var0.method_18799(var17);
                  }

                  return var17;
               }
            }
         } else {
            return var2;
         }
      } else {
         return var2;
      }
   }

   public static boolean locksBoatTurning(AbstractBoatEntity var0) {
      if (!anyActive || !var0.method_73183().method_8608()) {
         return false;
      } else if (BoatFlyModule.active == null && BoatNoclipModule.active == null) {
         EntityControlModule var1 = EntityControlModule.active;
         return var1 != null && var1.locksYaw(var0);
      } else {
         return true;
      }
   }

   public static Vec3d horizontalVelocity(ClientPlayerEntity var0, double var1) {
      float var3 = var0.method_36454();
      Vec3d var4 = Vec3d.method_1030(0.0F, var3);
      Vec3d var5 = Vec3d.method_1030(0.0F, var3 + 90.0F);
      PlayerInput var6 = var0.field_3913.field_54155;
      double var7 = 0.0;
      double var9 = 0.0;
      boolean var11 = false;
      if (var6.comp_3159()) {
         var7 += var4.field_1352 / 20.0 * var1;
         var9 += var4.field_1350 / 20.0 * var1;
         var11 = true;
      }

      if (var6.comp_3160()) {
         var7 -= var4.field_1352 / 20.0 * var1;
         var9 -= var4.field_1350 / 20.0 * var1;
         var11 = true;
      }

      boolean var12 = false;
      if (var6.comp_3162()) {
         var7 += var5.field_1352 / 20.0 * var1;
         var9 += var5.field_1350 / 20.0 * var1;
         var12 = true;
      }

      if (var6.comp_3161()) {
         var7 -= var5.field_1352 / 20.0 * var1;
         var9 -= var5.field_1350 / 20.0 * var1;
         var12 = true;
      }

      if (var11 && var12) {
         var7 *= DIAGONAL;
         var9 *= DIAGONAL;
      }

      return new Vec3d(var7, 0.0, var9);
   }

   public static boolean isPhysicallyDown(KeyBinding var0) {
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (var1.field_1755 == null && !var0.method_1415() && !isFreecamActive()) {
         Key var2 = KeyBindingHelper.getBoundKeyOf(var0);
         Type var3 = var2.method_1442();
         if (var3 == Type.field_1668) {
            return EfnKeyPolling.isKeyDown(var1.method_22683(), var2.method_1444());
         } else {
            return var3 == Type.field_1672 ? GLFW.glfwGetMouseButton(var1.method_22683().method_4490(), var2.method_1444()) == 1 : var0.method_1434();
         }
      } else {
         return false;
      }
   }

   public static boolean isOnAir(Entity var0) {
      return var0.method_73183().method_29546(var0.method_5829().method_1014(0.0625).method_1012(0.0, -0.55, 0.0)).allMatch(AbstractBlockState::method_26215);
   }

   private static boolean isFreecamActive() {
      FreecamModule var0 = ModuleRegistry.get(FreecamModule.class);
      return var0 != null && var0.isEnabled();
   }
}
