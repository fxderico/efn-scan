package net.efn.client.module.impl.render;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class NoRenderModule extends ClientModule {
   private static NoRenderModule instance;
   private final BooleanSetting sFireOverlay = this.addSetting("fire-overlay", new BooleanSetting("Fire Overlay", true));
   private final BooleanSetting sTotemAnimation = this.addSetting("totem-animation", new BooleanSetting("Totem Animation", true));
   private final BooleanSetting sPumpkinPowderSnow = this.addSetting("pumpkin-powder-snow", new BooleanSetting("Pumpkin & Powder Snow", true));
   private final BooleanSetting sNauseaPortal = this.addSetting("nausea-portal", new BooleanSetting("Nausea & Portal", true));
   private final BooleanSetting sBlindnessDarkness = this.addSetting("blindness-darkness", new BooleanSetting("Blindness & Darkness", true));
   private final BooleanSetting sSpawnerMob = this.addSetting("spawner-mob", new BooleanSetting("Spawner Mob", true));
   private final BooleanSetting sFallingBlocks = this.addSetting("falling-blocks", new BooleanSetting("Falling Blocks", false));
   private final BooleanSetting sWeather = this.addSetting("weather", new BooleanSetting("Weather", true));
   private final BooleanSetting sLavaFog = this.addSetting("lava-fog", new BooleanSetting("Lava Fog", true));
   private final BooleanSetting sEntityFire = this.addSetting("entity-fire", new BooleanSetting("Entity Fire", false));

   public NoRenderModule() {
      super("no-render", "No Render", "Hide selected overlays, screen effects and entity renders.", ModuleCategory.RENDER);
      instance = this;
   }

   public static boolean hideFireOverlay() {
      NoRenderModule var0 = instance;
      return var0 != null && var0.isEnabled() && var0.sFireOverlay.get();
   }

   public static boolean hideTotemAnimation() {
      NoRenderModule var0 = instance;
      return var0 != null && var0.isEnabled() && var0.sTotemAnimation.get();
   }

   public static boolean hidePumpkinPowderSnow() {
      NoRenderModule var0 = instance;
      return var0 != null && var0.isEnabled() && var0.sPumpkinPowderSnow.get();
   }

   public static boolean hideNauseaPortal() {
      NoRenderModule var0 = instance;
      return var0 != null && var0.isEnabled() && var0.sNauseaPortal.get();
   }

   public static boolean hideBlindnessDarkness() {
      NoRenderModule var0 = instance;
      return var0 != null && var0.isEnabled() && var0.sBlindnessDarkness.get();
   }

   public static boolean hideSpawnerMob() {
      NoRenderModule var0 = instance;
      return var0 != null && var0.isEnabled() && var0.sSpawnerMob.get();
   }

   public static boolean hideFallingBlocks() {
      NoRenderModule var0 = instance;
      return var0 != null && var0.isEnabled() && var0.sFallingBlocks.get();
   }

   public static boolean hideWeather() {
      NoRenderModule var0 = instance;
      return var0 != null && var0.isEnabled() && var0.sWeather.get();
   }

   public static boolean hideLavaFog() {
      NoRenderModule var0 = instance;
      return var0 != null && var0.isEnabled() && var0.sLavaFog.get();
   }

   public static boolean hideEntityFire() {
      NoRenderModule var0 = instance;
      return var0 != null && var0.isEnabled() && var0.sEntityFire.get();
   }
}
