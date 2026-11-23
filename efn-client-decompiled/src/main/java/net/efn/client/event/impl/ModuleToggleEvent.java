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

   private ModuleToggleEvent(ClientModule var1, boolean var2) {
      this.module = var1;
      this.enabled = var2;
   }

   public static void dispatch(ClientModule var0) {
      if (silentDepth <= 0 && TYPE.hasPreListeners()) {
         TYPE.dispatch(new ModuleToggleEvent(var0, var0.isEnabled()));
      }
   }

   public static void silently(Runnable var0) {
      silentDepth++;

      try {
         var0.run();
      } finally {
         silentDepth--;
      }
   }
}
