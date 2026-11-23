package net.efn.client.module.impl.movement.elytrafly;

import net.efn.client.event.impl.PlayerMoveEvent;
import net.efn.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public abstract class ElytraFlightMode {
   protected final MinecraftClient mc = MinecraftClient.getInstance();
   protected final ElytraFlyModule elytraFly;
   protected boolean lastJumpPressed;
   protected boolean incrementJumpTimer;
   public boolean lastForwardPressed;
   protected int jumpTimer;
   public double velX;
   public double velY;
   public double velZ;
   protected double ticksLeft;
   public Vec3d forward;
   public Vec3d right;
   protected double acceleration;

   protected ElytraFlightMode(ElytraFlyModule elytraFlyModule) {
      this.elytraFly = elytraFlyModule;
   }

   public void onTick() {
      ClientPlayerEntity var3 = this.mc.player;
      if (var3 != null) {
         int var2;
         if (this.elytraFly.autoReplenish.get()
            && (var2 = ElytraFlyInventory.find(itemStack -> itemStack.getItem() == Items.FIREWORK_ROCKET)) != -1
            && !ElytraFlyInventory.isHotbar(var2)) {
            ElytraFlyInventory.moveToHotbar(var2, this.elytraFly.replenishSlot.getInt() - 1);
         }

         ItemStack var1;
         if (this.elytraFly.replace.get() && (var1 = var3.getEquippedStack(EquipmentSlot.CHEST)).getItem() == Items.ELYTRA) {
            int var4 = this.elytraFly.replaceDurability.getInt();
            if (var1.getMaxDamage() - var1.getDamage() <= var4) {
               int var5 = ElytraFlyInventory.find(itemStack -> itemStack.getMaxDamage() - itemStack.getDamage() > var4 && itemStack.getItem() == Items.ELYTRA);
               ElytraFlyInventory.moveToChest(var5);
            }
         }
      }
   }

   public void onPreTick() {
   }

   public void onPacketSend(Packet<?> packet) {
   }

   public void onPositionPacket() {
   }

   public void onPlayerMove() {
   }

   public void onActivate() {
      this.lastJumpPressed = false;
      this.jumpTimer = 0;
      this.ticksLeft = 0.0;
      this.acceleration = 0.0;
   }

   public void onDeactivate() {
   }

   public void autoTakeoff() {
      ClientPlayerEntity var1 = this.mc.player;
      if (this.incrementJumpTimer) {
         this.jumpTimer++;
      }

      boolean var2 = this.mc.options.jumpKey.isPressed();
      ElytraFlightModes var3 = this.elytraFly.flightModeType();
      if (this.elytraFly.autoTakeOff.get() && var3 != ElytraFlightModes.Pitch40 && var3 != ElytraFlightModes.Bounce
         || !this.elytraFly.manualTakeoff.get() && var3 == ElytraFlightModes.Bounce && var2) {
         if (!this.lastJumpPressed && !var1.isGliding()) {
            this.jumpTimer = 0;
            this.incrementJumpTimer = true;
         }

         if (this.jumpTimer >= 8) {
            this.jumpTimer = 0;
            this.incrementJumpTimer = false;
            var1.setJumping(false);
            var1.setSprinting(true);
            var1.jump();
            var1.networkHandler.sendPacket(new ClientCommandC2SPacket(var1, Mode.START_FALL_FLYING));
         }
      }

      this.lastJumpPressed = var2;
   }

   public void handleAutopilot() {
      ClientPlayerEntity var1 = this.mc.player;
      if (var1.isGliding()) {
         if (this.elytraFly.autoPilot.get()
            && var1.getY() > this.elytraFly.autoPilotMinimumHeight.get()
            && this.elytraFly.flightModeType() != ElytraFlightModes.Bounce) {
            this.mc.options.forwardKey.setPressed(true);
            this.lastForwardPressed = true;
         }

         if (this.elytraFly.useFireworks.get()) {
            if (this.ticksLeft <= 0.0) {
               this.ticksLeft = this.elytraFly.autoPilotFireworkDelay.get() * 20.0F;
               int var2 = ElytraFlyInventory.findInHotbar(itemStack -> itemStack.getItem() == Items.FIREWORK_ROCKET);
               if (var2 == -1 || this.mc.interactionManager == null) {
                  return;
               }

               if (var2 == 40) {
                  this.mc.interactionManager.interactItem(var1, Hand.OFF_HAND);
                  var1.swingHand(Hand.OFF_HAND);
               } else {
                  ElytraFlyInventory.swap(var2, true);
                  this.mc.interactionManager.interactItem(var1, Hand.MAIN_HAND);
                  var1.swingHand(Hand.MAIN_HAND);
                  ElytraFlyInventory.swapBack();
               }
            }

            this.ticksLeft--;
         }
      }
   }

   public void handleHorizontalSpeed(PlayerMoveEvent playerMoveEvent) {
      boolean var2 = false;
      boolean var3 = false;
      if (this.mc.options.forwardKey.isPressed()) {
         this.velX = this.velX + this.forward.x * this.getSpeed() * 10.0;
         this.velZ = this.velZ + this.forward.z * this.getSpeed() * 10.0;
         var2 = true;
      } else if (this.mc.options.backKey.isPressed()) {
         this.velX = this.velX - this.forward.x * this.getSpeed() * 10.0;
         this.velZ = this.velZ - this.forward.z * this.getSpeed() * 10.0;
         var2 = true;
      }

      if (this.mc.options.rightKey.isPressed()) {
         this.velX = this.velX + this.right.x * this.getSpeed() * 10.0;
         this.velZ = this.velZ + this.right.z * this.getSpeed() * 10.0;
         var3 = true;
      } else if (this.mc.options.leftKey.isPressed()) {
         this.velX = this.velX - this.right.x * this.getSpeed() * 10.0;
         this.velZ = this.velZ - this.right.z * this.getSpeed() * 10.0;
         var3 = true;
      }

      if (var2 && var3) {
         double var4 = 1.0 / Math.sqrt(2.0);
         this.velX *= var4;
         this.velZ *= var4;
      }
   }

   public void handleVerticalSpeed(PlayerMoveEvent playerMoveEvent) {
      if (this.mc.options.jumpKey.isPressed()) {
         this.velY = this.velY + 0.5 * this.elytraFly.verticalSpeed.get();
      } else if (this.mc.options.sneakKey.isPressed()) {
         this.velY = this.velY - 0.5 * this.elytraFly.verticalSpeed.get();
      }
   }

   public void handleFallMultiplier() {
      if (this.velY < 0.0) {
         this.velY = this.velY * this.elytraFly.fallMultiplier.get();
      } else if (this.velY > 0.0) {
         this.velY = 0.0;
      }
   }

   public void handleAcceleration() {
      if (this.elytraFly.acceleration.get()) {
         if (!this.isMoving()) {
            this.acceleration = 0.0;
         }

         this.acceleration = Math.min(
            this.acceleration + this.elytraFly.accelerationMin.get() + this.elytraFly.accelerationStep.get() * 0.1,
            (double)this.elytraFly.horizontalSpeed.get()
         );
      } else {
         this.acceleration = 0.0;
      }
   }

   public void zeroAcceleration() {
      this.acceleration = 0.0;
   }

   protected double getSpeed() {
      return this.elytraFly.acceleration.get() ? this.acceleration : this.elytraFly.horizontalSpeed.get();
   }

   private boolean isMoving() {
      ClientPlayerEntity var1 = this.mc.player;
      return var1.forwardSpeed != 0.0F || var1.sidewaysSpeed != 0.0F;
   }
}
