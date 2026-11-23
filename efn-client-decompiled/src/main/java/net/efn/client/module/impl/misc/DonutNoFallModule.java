package net.efn.client.module.impl.misc;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;

@Environment(EnvType.CLIENT)
public final class DonutNoFallModule extends ClientModule {
   private static final double TRIGGER_FALL_DISTANCE = 3.0;
   private static final int GROUND_PACKETS_TO_RESET = 4;
   private boolean noFall;
   private double lastFallDistance;
   private int groundTicks;
   private boolean sendingReplacement;

   public DonutNoFallModule() {
      super("donut-nofall", "Donut NoFall", "Take no fall damage on DonutSMP.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      this.noFall = false;
      this.lastFallDistance = 0.0;
      this.groundTicks = 0;
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var2 != null) {
         if (this.noFall) {
            var2.setOnGround(false);
         }

         if (this.lastFallDistance > 3.0) {
            this.noFall = true;
         }

         this.lastFallDistance = var2.fallDistance;
      }
   }

   @EventHandler
   public void onPacketSend(PacketSendEvent packetSendEvent) {
      if (!this.sendingReplacement) {
         Packet var2 = packetSendEvent.packet;
         if (packetSendEvent.packet instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket var3 = (PlayerMoveC2SPacket)var2;
            MinecraftClient var4 = MinecraftClient.getInstance();
            ClientPlayerEntity var5 = var4.player;
            ClientPlayNetworkHandler var6 = var4.getNetworkHandler();
            if (var5 != null && var6 != null) {
               packetSendEvent.setCancelled(true);
               if (this.noFall && var3.isOnGround()) {
                  this.groundTicks++;
               }

               if (this.groundTicks % 2 == 1) {
                  var5.setPos(var5.getX(), var5.getY() + 1.0E-8, var5.getZ());
               }

               boolean var7 = !this.noFall && var5.isOnGround();
               this.sendingReplacement = true;

               try {
                  var6.sendPacket(new Full(var5.getX(), var5.getY(), var5.getZ(), var5.getYaw(), var5.getPitch(), var7, false));
               } finally {
                  this.sendingReplacement = false;
               }

               if (this.groundTicks == 4) {
                  this.noFall = false;
                  this.groundTicks = 0;
               }

               return;
            }

            return;
         }
      }
   }
}
