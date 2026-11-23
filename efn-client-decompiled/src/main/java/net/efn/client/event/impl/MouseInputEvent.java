package net.efn.client.event.impl;

import net.efn.client.event.CancellableEvent;
import net.efn.client.event.CancellableEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MouseInputEvent extends CancellableEvent {
   public static final CancellableEventType<MouseInputEvent> TYPE = new CancellableEventType<>("MouseInputEvent");
   public long window;
   public int button;
   public int action;
   public int mods;
   private static final MouseInputEvent INSTANCE = new MouseInputEvent();

   private MouseInputEvent() {
   }

   public static boolean dispatch(long var0, int var2, int var3, int var4) {
      if (!TYPE.hasPreListeners()) {
         return false;
      } else {
         INSTANCE.window = var0;
         INSTANCE.button = var2;
         INSTANCE.action = var3;
         INSTANCE.mods = var4;
         INSTANCE.resetCancellation();
         TYPE.dispatch(INSTANCE);
         return INSTANCE.isCancelled();
      }
   }
}
