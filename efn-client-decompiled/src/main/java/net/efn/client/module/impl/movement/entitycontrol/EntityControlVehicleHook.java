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

   public static Vec3d onEntityMove(Entity entity, MovementType movementType, Vec3d vec3d) {
      if (movementType == MovementType.SELF && entity.getEntityWorld().isClient()) {
         ClientPlayerEntity var3 = MinecraftClient.getInstance().player;
         if (var3 != null && var3.getVehicle() == entity && entity.getControllingPassenger() == var3) {
            Vec3d var4 = entity.getVelocity();
            BoatNoclipModule var5 = BoatNoclipModule.active;
            if (var5 != null && entity instanceof AbstractBoatEntity var18) {
               Vec3d var19 = var5.boatMovement(var18, var3);
               entity.setYaw(var3.getYaw());
               if (vec3d == var4) {
                  entity.setVelocity(var19);
               }

               return var19;
            } else {
               EntityControlModule var6 = EntityControlModule.active;
               EntitySpeedModule var7 = EntitySpeedModule.active;
               BoatFlyModule var8 = BoatFlyModule.active;
               double var9 = var4.x;
               double var11 = var4.y;
               double var13 = var4.z;
               boolean var15 = false;
               if (var7 != null) {
                  if (var7.appliesTo(entity)) {
                     Vec3d var16 = horizontalVelocity(var3, var7.horizontalSpeed());
                     var9 = var16.x;
                     var13 = var16.z;
                     var15 = true;
                  }
               } else if (var8 != null) {
                  Vec3d var20 = horizontalVelocity(var3, var8.horizontalSpeed());
                  var9 = var20.x;
                  var13 = var20.z;
                  var15 = true;
               }

               if (var8 != null) {
                  var11 = var8.verticalVelocity();
                  var15 = true;
               }

               if (var8 != null || var6 != null && var6.locksYaw(entity)) {
                  entity.setYaw(var3.getYaw());
               }

               if (!var15) {
                  return vec3d;
               } else {
                  Vec3d var17 = new Vec3d(var9, var11, var13);
                  if (vec3d == var4) {
                     entity.setVelocity(var17);
                  }

                  return var17;
               }
            }
         } else {
            return vec3d;
         }
      } else {
         return vec3d;
      }
   }

   public static boolean locksBoatTurning(AbstractBoatEntity abstractBoatEntity) {
      if (!anyActive || !abstractBoatEntity.getEntityWorld().isClient()) {
         return false;
      } else if (BoatFlyModule.active == null && BoatNoclipModule.active == null) {
         EntityControlModule var1 = EntityControlModule.active;
         return var1 != null && var1.locksYaw(abstractBoatEntity);
      } else {
         return true;
      }
   }

   public static Vec3d horizontalVelocity(ClientPlayerEntity clientPlayerEntity, double d) {
      float var3 = clientPlayerEntity.getYaw();
      Vec3d var4 = Vec3d.fromPolar(0.0F, var3);
      Vec3d var5 = Vec3d.fromPolar(0.0F, var3 + 90.0F);
      PlayerInput var6 = clientPlayerEntity.input.playerInput;
      double var7 = 0.0;
      double var9 = 0.0;
      boolean var11 = false;
      if (var6.forward()) {
         var7 += var4.x / 20.0 * d;
         var9 += var4.z / 20.0 * d;
         var11 = true;
      }

      if (var6.backward()) {
         var7 -= var4.x / 20.0 * d;
         var9 -= var4.z / 20.0 * d;
         var11 = true;
      }

      boolean var12 = false;
      if (var6.right()) {
         var7 += var5.x / 20.0 * d;
         var9 += var5.z / 20.0 * d;
         var12 = true;
      }

      if (var6.left()) {
         var7 -= var5.x / 20.0 * d;
         var9 -= var5.z / 20.0 * d;
         var12 = true;
      }

      if (var11 && var12) {
         var7 *= DIAGONAL;
         var9 *= DIAGONAL;
      }

      return new Vec3d(var7, 0.0, var9);
   }

   public static boolean isPhysicallyDown(KeyBinding keyBinding) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.currentScreen == null && !keyBinding.isUnbound() && !isFreecamActive()) {
         Key var2 = KeyBindingHelper.getBoundKeyOf(keyBinding);
         Type var3 = var2.getCategory();
         if (var3 == Type.KEYSYM) {
            return EfnKeyPolling.isKeyDown(var1.getWindow(), var2.getCode());
         } else {
            return var3 == Type.MOUSE ? GLFW.glfwGetMouseButton(var1.getWindow().getHandle(), var2.getCode()) == 1 : keyBinding.isPressed();
         }
      } else {
         return false;
      }
   }

   public static boolean isOnAir(Entity entity) {
      return entity.getEntityWorld().getStatesInBox(entity.getBoundingBox().expand(0.0625).stretch(0.0, -0.55, 0.0)).allMatch(AbstractBlockState::isAir);
   }

   private static boolean isFreecamActive() {
      FreecamModule var0 = ModuleRegistry.get(FreecamModule.class);
      return var0 != null && var0.isEnabled();
   }
}
