package net.efn.client.event.impl;

import net.efn.client.event.Event;
import net.efn.client.event.EventType;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModuleToggleEvent extends Event {
   public static final EventType<ModuleToggleEvent> TYPE = new EventType<>("ModuleToggleEvent");
   private static int silentDepth;
   public final ClientModule module;
   public final boolean enabled;

   private ModuleToggleEvent(ClientModule clientModule, boolean bl) {
      this.module = clientModule;
      this.enabled = bl;
   }

   public static void dispatch(ClientModule clientModule) {
      if (silentDepth <= 0 && TYPE.hasPreListeners()) {
         TYPE.dispatch(new ModuleToggleEvent(clientModule, clientModule.isEnabled()));
      }
   }

   public static void silently(Runnable runnable) {
      silentDepth++;

      try {
         runnable.run();
      } finally {
         silentDepth--;
      }
   }
}
