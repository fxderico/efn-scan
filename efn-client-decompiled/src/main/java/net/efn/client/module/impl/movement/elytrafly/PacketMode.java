package net.efn.client.module.impl.movement.elytrafly;

import net.efn.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class PacketMode extends ElytraFlightMode {
   public PacketMode(ElytraFlyModule elytraFlyModule) {
      super(elytraFlyModule);
   }

   @Override
   public void onDeactivate() {
      ClientPlayerEntity var1 = this.mc.player;
      if (var1 != null) {
         PlayerAbilities var2 = var1.getAbilities();
         var2.flying = false;
         if (!var2.creativeMode && !var1.isSpectator()) {
            var2.allowFlying = false;
         }
      }
   }

   @Override
   public void onTick() {
      super.onTick();
      ClientPlayerEntity var1 = this.mc.player;
      if (var1 != null) {
         if (var1.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && !(var1.fallDistance <= 0.2) && !this.mc.options.sneakKey.isPressed()) {
            var1.setVelocity(Vec3d.ZERO);
            var1.networkHandler.sendPacket(new ClientCommandC2SPacket(var1, Mode.START_FALL_FLYING));
            var1.networkHandler.sendPacket(new OnGroundOnly(true, var1.horizontalCollision));
         }
      }
   }

   @Override
   public void onPacketSend(Packet<?> packet) {
      if (packet instanceof PlayerMoveC2SPacket) {
         ClientPlayerEntity var2 = this.mc.player;
         var2.networkHandler.sendPacket(new ClientCommandC2SPacket(var2, Mode.START_FALL_FLYING));
      }
   }

   @Override
   public void onPlayerMove() {
      PlayerAbilities var1 = this.mc.player.getAbilities();
      var1.flying = true;
      var1.setFlySpeed(this.elytraFly.horizontalSpeed.get() / 20.0F);
   }
}
