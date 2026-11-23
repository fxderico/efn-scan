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
   protected final MinecraftClient mc = MinecraftClient.method_1551();
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

   protected ElytraFlightMode(ElytraFlyModule var1) {
      this.elytraFly = var1;
   }

   public void onTick() {
      ClientPlayerEntity var3 = this.mc.field_1724;
      if (var3 != null) {
         int var2;
         if (this.elytraFly.autoReplenish.get()
            && (var2 = ElytraFlyInventory.find(var0 -> var0.method_7909() == Items.field_8639)) != -1
            && !ElytraFlyInventory.isHotbar(var2)) {
            ElytraFlyInventory.moveToHotbar(var2, this.elytraFly.replenishSlot.getInt() - 1);
         }

         ItemStack var1;
         if (this.elytraFly.replace.get() && (var1 = var3.method_6118(EquipmentSlot.field_6174)).method_7909() == Items.field_8833) {
            int var4 = this.elytraFly.replaceDurability.getInt();
            if (var1.method_7936() - var1.method_7919() <= var4) {
               int var5 = ElytraFlyInventory.find(var1x -> var1x.method_7936() - var1x.method_7919() > var4 && var1x.method_7909() == Items.field_8833);
               ElytraFlyInventory.moveToChest(var5);
            }
         }
      }
   }

   public void onPreTick() {
   }

   public void onPacketSend(Packet<?> var1) {
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
      ClientPlayerEntity var1 = this.mc.field_1724;
      if (this.incrementJumpTimer) {
         this.jumpTimer++;
      }

      boolean var2 = this.mc.field_1690.field_1903.method_1434();
      ElytraFlightModes var3 = this.elytraFly.flightModeType();
      if (this.elytraFly.autoTakeOff.get() && var3 != ElytraFlightModes.Pitch40 && var3 != ElytraFlightModes.Bounce
         || !this.elytraFly.manualTakeoff.get() && var3 == ElytraFlightModes.Bounce && var2) {
         if (!this.lastJumpPressed && !var1.method_6128()) {
            this.jumpTimer = 0;
            this.incrementJumpTimer = true;
         }

         if (this.jumpTimer >= 8) {
            this.jumpTimer = 0;
            this.incrementJumpTimer = false;
            var1.method_6100(false);
            var1.method_5728(true);
            var1.method_6043();
            var1.field_3944.method_52787(new ClientCommandC2SPacket(var1, Mode.field_12982));
         }
      }

      this.lastJumpPressed = var2;
   }

   public void handleAutopilot() {
      ClientPlayerEntity var1 = this.mc.field_1724;
      if (var1.method_6128()) {
         if (this.elytraFly.autoPilot.get()
            && var1.method_23318() > this.elytraFly.autoPilotMinimumHeight.get()
            && this.elytraFly.flightModeType() != ElytraFlightModes.Bounce) {
            this.mc.field_1690.field_1894.method_23481(true);
            this.lastForwardPressed = true;
         }

         if (this.elytraFly.useFireworks.get()) {
            if (this.ticksLeft <= 0.0) {
               this.ticksLeft = this.elytraFly.autoPilotFireworkDelay.get() * 20.0F;
               int var2 = ElytraFlyInventory.findInHotbar(var0 -> var0.method_7909() == Items.field_8639);
               if (var2 == -1 || this.mc.field_1761 == null) {
                  return;
               }

               if (var2 == 40) {
                  this.mc.field_1761.method_2919(var1, Hand.field_5810);
                  var1.method_6104(Hand.field_5810);
               } else {
                  ElytraFlyInventory.swap(var2, true);
                  this.mc.field_1761.method_2919(var1, Hand.field_5808);
                  var1.method_6104(Hand.field_5808);
                  ElytraFlyInventory.swapBack();
               }
            }

            this.ticksLeft--;
         }
      }
   }

   public void handleHorizontalSpeed(PlayerMoveEvent var1) {
      boolean var2 = false;
      boolean var3 = false;
      if (this.mc.field_1690.field_1894.method_1434()) {
         this.velX = this.velX + this.forward.field_1352 * this.getSpeed() * 10.0;
         this.velZ = this.velZ + this.forward.field_1350 * this.getSpeed() * 10.0;
         var2 = true;
      } else if (this.mc.field_1690.field_1881.method_1434()) {
         this.velX = this.velX - this.forward.field_1352 * this.getSpeed() * 10.0;
         this.velZ = this.velZ - this.forward.field_1350 * this.getSpeed() * 10.0;
         var2 = true;
      }

      if (this.mc.field_1690.field_1849.method_1434()) {
         this.velX = this.velX + this.right.field_1352 * this.getSpeed() * 10.0;
         this.velZ = this.velZ + this.right.field_1350 * this.getSpeed() * 10.0;
         var3 = true;
      } else if (this.mc.field_1690.field_1913.method_1434()) {
         this.velX = this.velX - this.right.field_1352 * this.getSpeed() * 10.0;
         this.velZ = this.velZ - this.right.field_1350 * this.getSpeed() * 10.0;
         var3 = true;
      }

      if (var2 && var3) {
         double var4 = 1.0 / Math.sqrt(2.0);
         this.velX *= var4;
         this.velZ *= var4;
      }
   }

   public void handleVerticalSpeed(PlayerMoveEvent var1) {
      if (this.mc.field_1690.field_1903.method_1434()) {
         this.velY = this.velY + 0.5 * this.elytraFly.verticalSpeed.get();
      } else if (this.mc.field_1690.field_1832.method_1434()) {
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
      ClientPlayerEntity var1 = this.mc.field_1724;
      return var1.field_6250 != 0.0F || var1.field_6212 != 0.0F;
   }
}
