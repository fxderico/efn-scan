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
   public PacketMode(ElytraFlyModule var1) {
      super(var1);
   }

   @Override
   public void onDeactivate() {
      ClientPlayerEntity var1 = this.mc.field_1724;
      if (var1 != null) {
         PlayerAbilities var2 = var1.method_31549();
         var2.field_7479 = false;
         if (!var2.field_7477 && !var1.method_7325()) {
            var2.field_7478 = false;
         }
      }
   }

   @Override
   public void onTick() {
      super.onTick();
      ClientPlayerEntity var1 = this.mc.field_1724;
      if (var1 != null) {
         if (var1.method_6118(EquipmentSlot.field_6174).method_7909() == Items.field_8833
            && !(var1.field_6017 <= 0.2)
            && !this.mc.field_1690.field_1832.method_1434()) {
            var1.method_18799(Vec3d.field_1353);
            var1.field_3944.method_52787(new ClientCommandC2SPacket(var1, Mode.field_12982));
            var1.field_3944.method_52787(new OnGroundOnly(true, var1.field_5976));
         }
      }
   }

   @Override
   public void onPacketSend(Packet<?> var1) {
      if (var1 instanceof PlayerMoveC2SPacket) {
         ClientPlayerEntity var2 = this.mc.field_1724;
         var2.field_3944.method_52787(new ClientCommandC2SPacket(var2, Mode.field_12982));
      }
   }

   @Override
   public void onPlayerMove() {
      PlayerAbilities var1 = this.mc.field_1724.method_31549();
      var1.field_7479 = true;
      var1.method_7248(this.elytraFly.horizontalSpeed.get() / 20.0F);
   }
}
