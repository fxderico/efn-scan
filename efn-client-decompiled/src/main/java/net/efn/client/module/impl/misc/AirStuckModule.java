package net.efn.client.module.impl.misc;

import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@Environment(EnvType.CLIENT)
public final class AirStuckModule extends ClientModule {
   public static volatile AirStuckModule active;

   public AirStuckModule() {
      super("air-stuck", "Air Stuck", "Freezes you in place, even mid-air.", ModuleCategory.MOVEMENT);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      active = this;
      this.on(PacketSendEvent.TYPE, this::onPacketSend);
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   private void onPacketSend(PacketSendEvent packetSendEvent) {
      if (packetSendEvent.packet instanceof ClientTickEndC2SPacket || packetSendEvent.packet instanceof PlayerMoveC2SPacket) {
         packetSendEvent.setCancelled(true);
      }
   }
}
