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

   private void onPacketReceive(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      if (this.cancelServerPackets.get() && packetReceiveNettyEvent.packet instanceof VehicleMoveS2CPacket) {
         packetReceiveNettyEvent.setCancelled(true);
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

   public boolean locksYaw(Entity entity) {
      return this.lockYaw.get() && this.isTarget(entity);
   }

   private boolean isTarget(Entity entity) {
      if (entity instanceof AbstractBoatEntity) {
         return this.boats.get();
      } else {
         EntityType var2 = entity.getType();
         if (var2 == EntityType.HORSE
            || var2 == EntityType.DONKEY
            || var2 == EntityType.MULE
            || var2 == EntityType.SKELETON_HORSE
            || var2 == EntityType.ZOMBIE_HORSE) {
            return this.horses.get();
         } else if (var2 == EntityType.CAMEL || var2 == EntityType.CAMEL_HUSK) {
            return this.camels.get();
         } else if (var2 == EntityType.PIG) {
            return this.pigs.get();
         } else if (var2 == EntityType.STRIDER) {
            return this.striders.get();
         } else if (var2 == EntityType.NAUTILUS || var2 == EntityType.ZOMBIE_NAUTILUS) {
            return this.nautiluses.get();
         } else {
            return var2 == EntityType.HAPPY_GHAST ? this.happyGhasts.get() : false;
         }
      }
   }
}
