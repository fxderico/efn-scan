package net.efn.client.module.impl.movement;

import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.efn.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;

@Environment(EnvType.CLIENT)
public final class EntityControlModule extends ClientModule {
   public static volatile EntityControlModule active;
   private final BooleanSetting boats = this.addSetting("boats", new BooleanSetting("Boats", true));
   private final BooleanSetting horses = this.addSetting("horses", new BooleanSetting("Horses", true));
   private final BooleanSetting camels = this.addSetting("camels", new BooleanSetting("Camels", true));
   private final BooleanSetting pigs = this.addSetting("pigs", new BooleanSetting("Pigs", true));
   private final BooleanSetting striders = this.addSetting("striders", new BooleanSetting("Striders", true));
   private final BooleanSetting nautiluses = this.addSetting("nautiluses", new BooleanSetting("Nautiluses", true));
   private final BooleanSetting happyGhasts = this.addSetting("happy-ghasts", new BooleanSetting("Happy Ghasts", true));
   private final BooleanSetting spoofSaddle = this.addSetting("spoof-saddle", new BooleanSetting("Spoof Saddle", false));
   private final BooleanSetting maxJump = this.addSetting("max-jump", new BooleanSetting("Max Jump", true));
   private final BooleanSetting lockYaw = this.addSetting("lock-yaw", new BooleanSetting("Lock Yaw", true));
   private final BooleanSetting cancelServerPackets = this.addSetting("cancel-server-packets", new BooleanSetting("Cancel Server Packets", false));

   public EntityControlModule() {
      super("entity-control", "Entity Control", "Lets you control rideable entities without a saddle.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      active = this;
      EntityControlVehicleHook.refresh();
      this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceive);
   }

   @Override
   protected void onDisable() {
      active = null;
      EntityControlVehicleHook.refresh();
   }

   private void onPacketReceive(PacketReceiveNettyEvent var1) {
      if (this.cancelServerPackets.get() && var1.packet instanceof VehicleMoveS2CPacket) {
         var1.setCancelled(true);
      }
   }

   public static boolean spoofSaddle() {
      EntityControlModule var0 = active;
      return var0 != null && var0.spoofSaddle.get();
   }

   public static boolean maxJump() {
      EntityControlModule var0 = active;
      return var0 != null && var0.maxJump.get();
   }

   public boolean locksYaw(Entity var1) {
      return this.lockYaw.get() && this.isTarget(var1);
   }

   private boolean isTarget(Entity var1) {
      if (var1 instanceof AbstractBoatEntity) {
         return this.boats.get();
      } else {
         EntityType var2 = var1.method_5864();
         if (var2 == EntityType.field_6139
            || var2 == EntityType.field_6067
            || var2 == EntityType.field_6057
            || var2 == EntityType.field_6075
            || var2 == EntityType.field_6048) {
            return this.horses.get();
         } else if (var2 == EntityType.field_40116 || var2 == EntityType.field_64132) {
            return this.camels.get();
         } else if (var2 == EntityType.field_6093) {
            return this.pigs.get();
         } else if (var2 == EntityType.field_23214) {
            return this.striders.get();
         } else if (var2 == EntityType.field_63289 || var2 == EntityType.field_63290) {
            return this.nautiluses.get();
         } else {
            return var2 == EntityType.field_59668 ? this.happyGhasts.get() : false;
         }
      }
   }
}
