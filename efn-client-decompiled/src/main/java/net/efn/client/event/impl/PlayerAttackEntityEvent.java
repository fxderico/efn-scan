package net.efn.client.event.impl;

import net.efn.client.event.CancellableEvent;
import net.efn.client.event.CancellableEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;

@Environment(EnvType.CLIENT)
public final class PlayerAttackEntityEvent extends CancellableEvent {
   public static final CancellableEventType<PlayerAttackEntityEvent> TYPE = new CancellableEventType<>("PlayerAttackEntityEvent");
   public Entity target;
   private static final PlayerAttackEntityEvent INSTANCE = new PlayerAttackEntityEvent();

   private PlayerAttackEntityEvent() {
   }

   public static boolean dispatch(Entity var0) {
      if (!TYPE.hasPreListeners()) {
         return false;
      } else {
         INSTANCE.target = var0;
         INSTANCE.resetCancellation();
         TYPE.dispatch(INSTANCE);
         boolean var1 = INSTANCE.isCancelled();
         INSTANCE.target = null;
         return var1;
      }
   }
}
