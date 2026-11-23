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

   public BounceMode(ElytraFlyModule var1) {
      super(var1);
      this.tickDelay = var1.restartDelay.getInt();
   }

   @Override
   public void onTick() {
      super.onTick();
      ClientPlayerEntity var1 = this.mc.field_1724;
      if (var1 != null) {
         if (this.mc.field_1690.field_1903.method_1434() && !var1.method_6128() && !this.elytraFly.manualTakeoff.get()) {
            sendStartFallFlying(var1);
         }

         if (checkConditions(var1)) {
            if (!this.rubberbanded) {
               if (this.prevFov != 0.0 && !this.elytraFly.sprint.get()) {
                  this.mc.field_1690.method_42454().method_41748(0.0);
               }

               if (this.elytraFly.autoJump.get()) {
                  this.mc.field_1690.field_1903.method_23481(true);
               }

               this.mc.field_1690.field_1894.method_23481(true);
               var1.method_36456(this.getYawDirection(var1));
               if (this.elytraFly.lockPitch.get()) {
                  var1.method_36457(this.elytraFly.pitch.get());
               }
            }

            if (!this.elytraFly.sprint.get()) {
               if (var1.method_6128()) {
                  var1.method_5728(var1.method_24828());
               } else {
                  var1.method_5728(true);
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
      ClientPlayerEntity var1 = this.mc.field_1724;
      if (var1 != null && checkConditions(var1) && this.elytraFly.sprint.get()) {
         var1.method_5728(true);
      }
   }

   private void unpress() {
      this.mc.field_1690.field_1894.method_23481(false);
      if (this.elytraFly.autoJump.get()) {
         this.mc.field_1690.field_1903.method_23481(false);
      }
   }

   @Override
   public void onPositionPacket() {
      this.rubberbanded = true;
      ClientPlayerEntity var1 = this.mc.field_1724;
      if (var1 != null) {
         var1.method_66281();
      }
   }

   @Override
   public void onPacketSend(Packet<?> var1) {
      ClientCommandC2SPacket var2;
      if (var1 instanceof ClientCommandC2SPacket && (var2 = (ClientCommandC2SPacket)var1).method_12365() == Mode.field_12982 && !this.elytraFly.sprint.get()) {
         this.mc.field_1724.method_5728(true);
      }
   }

   public static boolean recastElytra(ClientPlayerEntity var0) {
      if (checkConditions(var0) && startGliding(var0)) {
         sendStartFallFlying(var0);
         return true;
      } else {
         return false;
      }
   }

   public static boolean checkConditions(ClientPlayerEntity var0) {
      BlockState var1 = var0.method_55667();
      boolean var2 = var1.method_26164(BlockTags.field_22414) && !var1.method_26164(BlockTags.field_63247);
      return !var0.method_31549().field_7479 && !var0.method_5765() && !var2 && !var0.method_5799() && !var0.method_6059(StatusEffects.field_5902);
   }

   private static boolean startGliding(ClientPlayerEntity var0) {
      for (EquipmentSlot var2 : EquipmentSlot.field_54086) {
         if (LivingEntity.method_63624(var0.method_6118(var2), var2)) {
            MinecraftClient.method_1551().method_19537(() -> var0.method_23669());
            return true;
         }
      }

      return false;
   }

   private static void sendStartFallFlying(ClientPlayerEntity var0) {
      var0.field_3944.method_52787(new ClientCommandC2SPacket(var0, Mode.field_12982));
   }

   private float getYawDirection(ClientPlayerEntity var1) {
      String var2 = this.elytraFly.yawLockMode.get();

      return switch (var2) {
         case "Smart" -> Math.round((var1.method_36454() + 1.0F) / 45.0F) * 45.0F;
         case "Simple" -> this.elytraFly.yaw.get();
         default -> var1.method_36454();
      };
   }

   @Override
   public void onActivate() {
      this.prevFov = (Double)this.mc.field_1690.method_42454().method_41753();
   }

   @Override
   public void onDeactivate() {
      this.unpress();
      this.rubberbanded = false;
      if (this.prevFov != 0.0 && !this.elytraFly.sprint.get()) {
         this.mc.field_1690.method_42454().method_41748(this.prevFov);
      }
   }
}
