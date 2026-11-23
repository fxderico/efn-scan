package net.efn.client.event.impl;

import net.efn.client.event.Event;
import net.efn.client.event.EventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;

@Environment(EnvType.CLIENT)
public final class WorldEntityRemoveEvent extends Event {
   public static final EventType<WorldEntityRemoveEvent> TYPE = new EventType<>("WorldEntityRemoveEvent");
   public Entity entity;
   private static final WorldEntityRemoveEvent INSTANCE = new WorldEntityRemoveEvent();

   private WorldEntityRemoveEvent() {
   }

   public static void dispatch(Entity var0) {
      if (TYPE.hasPreListeners()) {
         INSTANCE.entity = var0;
         TYPE.dispatch(INSTANCE);
         INSTANCE.entity = null;
      }
   }
}
