package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.util.pvp.InputControl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class JumpResetModule extends ClientModule {
   private static final double MIN_KNOCKBACK = 0.1;
   private boolean pending;

   public JumpResetModule() {
      super("jump-reset", "Jump Reset", "Jumps the tick knockback lands to take less of it.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onDisable() {
      this.pending = false;
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent packetAppliedEvent) {
      if (packetAppliedEvent.packet instanceof EntityVelocityUpdateS2CPacket var3) {
         ClientPlayerEntity var4 = MinecraftClient.getInstance().player;
         if (var4 != null && var3.getEntityId() == var4.getId()) {
            Vec3d var5 = var3.getVelocity();
            if (var5.horizontalLength() >= 0.1 && var5.y > 0.0) {
               this.pending = true;
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      if (this.pending) {
         this.pending = false;
         MinecraftClient var2 = MinecraftClient.getInstance();
         ClientPlayerEntity var3 = var2.player;
         if (var3 != null && var2.currentScreen == null) {
            if (var3.isOnGround() && var3.isSprinting() && var3.input.playerInput.forward()) {
               if (!var3.isTouchingWater() && !var3.isInLava() && !var3.isClimbing() && !var3.hasVehicle() && !var3.isGliding()) {
                  InputControl.jump();
               }
            }
         }
      }
   }
}
