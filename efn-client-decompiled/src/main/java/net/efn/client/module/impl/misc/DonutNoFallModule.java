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
   public void onTick(TickEvent var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 != null) {
         if (this.noFall) {
            var2.method_24830(false);
         }

         if (this.lastFallDistance > 3.0) {
            this.noFall = true;
         }

         this.lastFallDistance = var2.field_6017;
      }
   }

   @EventHandler
   public void onPacketSend(PacketSendEvent var1) {
      if (!this.sendingReplacement) {
         Packet var2 = var1.packet;
         if (var1.packet instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket var3 = (PlayerMoveC2SPacket)var2;
            MinecraftClient var4 = MinecraftClient.method_1551();
            ClientPlayerEntity var5 = var4.field_1724;
            ClientPlayNetworkHandler var6 = var4.method_1562();
            if (var5 != null && var6 != null) {
               var1.setCancelled(true);
               if (this.noFall && var3.method_12273()) {
                  this.groundTicks++;
               }

               if (this.groundTicks % 2 == 1) {
                  var5.method_23327(var5.method_23317(), var5.method_23318() + 1.0E-8, var5.method_23321());
               }

               boolean var7 = !this.noFall && var5.method_24828();
               this.sendingReplacement = true;

               try {
                  var6.method_52787(
                     new Full(var5.method_23317(), var5.method_23318(), var5.method_23321(), var5.method_36454(), var5.method_36455(), var7, false)
                  );
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
