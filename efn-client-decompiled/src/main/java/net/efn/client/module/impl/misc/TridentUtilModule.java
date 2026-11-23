package net.efn.client.module.impl.misc;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TridentUtilModule extends ClientModule {
   private final NumberSetting chargeScale = this.addSetting("scale", new NumberSetting("Charge Scale", 0.0F, 0.0F, 1.0F).step(0.1F));
   private final BooleanSetting noWater = this.addSetting("no-water", new BooleanSetting("No Water", true));

   public TridentUtilModule() {
      super("trident-util", "Trident Util", "Use tridents out of water and charge them faster.", ModuleCategory.MOVEMENT);
   }

   private static TridentUtilModule active() {
      TridentUtilModule var0 = ModuleRegistry.get(TridentUtilModule.class);
      return var0 != null && var0.isEnabled() ? var0 : null;
   }

   public static boolean allowOutOfWater() {
      TridentUtilModule var0 = active();
      return var0 != null && var0.noWater.get();
   }

   public static int minChargeTicks(int var0) {
      TridentUtilModule var1 = active();
      return var1 == null ? var0 : Math.max(1, Math.round(var0 * var1.chargeScale.get()));
   }
}
