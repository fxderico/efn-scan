package net.efn.client.event.impl;

import net.efn.client.event.CancellableEvent;
import net.efn.client.event.CancellableEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class KeyEvent extends CancellableEvent {
   public static final CancellableEventType<KeyEvent> TYPE = new CancellableEventType<>("KeyEvent");
   public int key;
   public int scancode;
   public int action;
   public int mods;
   private static final KeyEvent INSTANCE = new KeyEvent();

   private KeyEvent() {
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
