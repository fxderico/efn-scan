package net.efn.client.module.impl.movement;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TridentBoostModule extends ClientModule {
   public static volatile TridentBoostModule active;
   private final NumberSetting boost = this.addSetting("boost", new NumberSetting("Boost", 2.0F, 0.1F, 10.0F).step(0.1F));
   private final BooleanSetting outOfWater = this.addSetting("out-of-water", new BooleanSetting("Out Of Water", true));

   public TridentBoostModule() {
      super("trident-boost", "Trident Boost", "Boosts you when using riptide with a trident.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   public static double riptideMultiplier() {
      TridentBoostModule var0 = active;
      return var0 != null ? var0.boost.get() : 1.0;
   }

   public static boolean allowOutOfWater() {
      TridentBoostModule var0 = active;
      return var0 != null && var0.outOfWater.get();
   }
}
