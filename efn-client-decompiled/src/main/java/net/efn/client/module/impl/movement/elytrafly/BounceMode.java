package net.efn.client.module.impl.movement.elytrafly;

import net.efn.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.registry.tag.BlockTags;

@Environment(EnvType.CLIENT)
public final class BounceMode extends ElytraFlightMode {
   private boolean rubberbanded = false;
   private int tickDelay;
   private double prevFov;

   public BounceMode(ElytraFlyModule elytraFlyModule) {
      super(elytraFlyModule);
      this.tickDelay = elytraFlyModule.restartDelay.getInt();
   }

   @Override
   public void onTick() {
      super.onTick();
      ClientPlayerEntity var1 = this.mc.player;
      if (var1 != null) {
         if (this.mc.options.jumpKey.isPressed() && !var1.isGliding() && !this.elytraFly.manualTakeoff.get()) {
            sendStartFallFlying(var1);
         }

         if (checkConditions(var1)) {
            if (!this.rubberbanded) {
               if (this.prevFov != 0.0 && !this.elytraFly.sprint.get()) {
                  this.mc.options.getFovEffectScale().setValue(0.0);
               }

               if (this.elytraFly.autoJump.get()) {
                  this.mc.options.jumpKey.setPressed(true);
               }

               this.mc.options.forwardKey.setPressed(true);
               var1.setYaw(this.getYawDirection(var1));
               if (this.elytraFly.lockPitch.get()) {
                  var1.setPitch(this.elytraFly.pitch.get());
               }
            }

            if (!this.elytraFly.sprint.get()) {
               if (var1.isGliding()) {
                  var1.setSprinting(var1.isOnGround());
               } else {
                  var1.setSprinting(true);
               }
            }

            if (this.rubberbanded && this.elytraFly.restart.get()) {
               if (this.tickDelay > 0) {
                  this.tickDelay--;
               } else {
                  sendStartFallFlying(var1);
                  this.rubberbanded = false;
                  this.tickDelay = this.elytraFly.restartDelay.getInt();
               }
            }
         }
      }
   }

   @Override
   public void onPreTick() {
      super.onPreTick();
      ClientPlayerEntity var1 = this.mc.player;
      if (var1 != null && checkConditions(var1) && this.elytraFly.sprint.get()) {
         var1.setSprinting(true);
      }
   }

   private void unpress() {
      this.mc.options.forwardKey.setPressed(false);
      if (this.elytraFly.autoJump.get()) {
         this.mc.options.jumpKey.setPressed(false);
      }
   }

   @Override
   public void onPositionPacket() {
      this.rubberbanded = true;
      ClientPlayerEntity var1 = this.mc.player;
      if (var1 != null) {
         var1.stopGliding();
      }
   }

   @Override
   public void onPacketSend(Packet<?> packet) {
      ClientCommandC2SPacket var2;
      if (packet instanceof ClientCommandC2SPacket
         && (var2 = (ClientCommandC2SPacket)packet).getMode() == Mode.START_FALL_FLYING
         && !this.elytraFly.sprint.get()) {
         this.mc.player.setSprinting(true);
      }
   }

   public static boolean recastElytra(ClientPlayerEntity clientPlayerEntity) {
      if (checkConditions(clientPlayerEntity) && startGliding(clientPlayerEntity)) {
         sendStartFallFlying(clientPlayerEntity);
         return true;
      } else {
         return false;
      }
   }

   public static boolean checkConditions(ClientPlayerEntity clientPlayerEntity) {
      BlockState var1 = clientPlayerEntity.getBlockStateAtPos();
      boolean var2 = var1.isIn(BlockTags.CLIMBABLE) && !var1.isIn(BlockTags.CAN_GLIDE_THROUGH);
      return !clientPlayerEntity.getAbilities().flying
         && !clientPlayerEntity.hasVehicle()
         && !var2
         && !clientPlayerEntity.isTouchingWater()
         && !clientPlayerEntity.hasStatusEffect(StatusEffects.LEVITATION);
   }

   private static boolean startGliding(ClientPlayerEntity clientPlayerEntity) {
      for (EquipmentSlot var2 : EquipmentSlot.VALUES) {
         if (LivingEntity.canGlideWith(clientPlayerEntity.getEquippedStack(var2), var2)) {
            MinecraftClient.getInstance().submitAndJoin(() -> clientPlayerEntity.startGliding());
            return true;
         }
      }

      return false;
   }

   private static void sendStartFallFlying(ClientPlayerEntity clientPlayerEntity) {
      clientPlayerEntity.networkHandler.sendPacket(new ClientCommandC2SPacket(clientPlayerEntity, Mode.START_FALL_FLYING));
   }

   private float getYawDirection(ClientPlayerEntity clientPlayerEntity) {
      String var2 = this.elytraFly.yawLockMode.get();

      return switch (var2) {
         case "Smart" -> Math.round((clientPlayerEntity.getYaw() + 1.0F) / 45.0F) * 45.0F;
         case "Simple" -> this.elytraFly.yaw.get();
         default -> clientPlayerEntity.getYaw();
      };
   }

   @Override
   public void onActivate() {
      this.prevFov = this.mc.options.getFovEffectScale().getValue();
   }

   @Override
   public void onDeactivate() {
      this.unpress();
      this.rubberbanded = false;
      if (this.prevFov != 0.0 && !this.elytraFly.sprint.get()) {
         this.mc.options.getFovEffectScale().setValue(this.prevFov);
      }
   }
}
