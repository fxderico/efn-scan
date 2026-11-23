package net.efn.client.module.impl.render;

import net.efn.client.config.ThemeStore;
import net.efn.client.gui.ThemeTint;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.client.HudModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class CosmeticAccent {
   private CosmeticAccent() {
   }

   static int argb() {
      HudModule var0 = ModuleRegistry.get(HudModule.class);
      int var1 = var0 != null && var0.customAccent() ? var0.accentArgb() : ThemeStore.get().accent.argb();
      return ThemeTint.accent(var1) | 0xFF000000;
   }
}
