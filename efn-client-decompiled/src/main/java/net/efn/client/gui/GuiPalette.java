package net.efn.client.gui;

import net.efn.client.config.Surface;
import net.efn.client.config.ThemeStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GuiPalette {
   private static int accent = -624588;
   private static int accentHover = -486564;
   private static int panelBg = -349691361;
   private static int cardBg = -149022441;
   private static int tabBarBg = -149022441;
   private static float rounding = 10.0F;
   private static float animationSpeed = 1.0F;

   private GuiPalette() {
   }

   public static void refresh() {
      ThemeStore var0 = ThemeStore.get();
      accent = abgr(ThemeTint.accent(var0.accent.argbWithAlpha(255)));
      accentHover = EfnTheme.lerpColor(accent, -1, 0.2F);
      panelBg = fill(var0.panels);
      cardBg = fill(var0.cards);
      tabBarBg = fill(var0.tabBar);
      rounding = var0.rounding.get();
      animationSpeed = var0.animationSpeed.get();
   }

   public static int accent() {
      return accent;
   }

   public static int accentHover() {
      return accentHover;
   }

   public static int panelBg() {
      return panelBg;
   }

   public static int cardBg() {
      return cardBg;
   }

   public static int tabBarBg() {
      return tabBarBg;
   }

   public static int fill(Surface var0) {
      return abgr(ThemeTint.surface(var0.color.argbWithAlpha(Math.round(var0.opacity.get() * 255.0F))));
   }

   public static float rounding() {
      return rounding;
   }

   public static float animationSpeed() {
      return animationSpeed;
   }

   public static int abgr(int var0) {
      return var0 & -16711936 | var0 >> 16 & 0xFF | (var0 & 0xFF) << 16;
   }
}
