package net.efn.client.event.impl;

import net.efn.client.event.CancellableEvent;
import net.efn.client.event.CancellableEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class KeyInputEvent extends CancellableEvent {
   public static final CancellableEventType<KeyInputEvent> TYPE = new CancellableEventType<>("KeyInputEvent");
   public int key;
   public int scancode;
   public int action;
   public int mods;
   private static final KeyInputEvent INSTANCE = new KeyInputEvent();

   private KeyInputEvent() {
   }

   public static boolean dispatch(int var0, int var1, int var2, int var3) {
      if (!TYPE.hasPreListeners()) {
         return false;
      } else {
         INSTANCE.key = var0;
         INSTANCE.scancode = var1;
         INSTANCE.action = var2;
         INSTANCE.mods = var3;
         INSTANCE.resetCancellation();
         TYPE.dispatch(INSTANCE);
         return INSTANCE.isCancelled();
      }
   }
}
