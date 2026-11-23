package net.efn.client.module.impl.misc;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;

@Environment(EnvType.CLIENT)
public final class SpeedMineModule extends ClientModule {
   private static volatile SpeedMineModule active;
   private final NumberSetting breakAt = this.addSetting("break-at", new NumberSetting("Break At", 0.7F, 0.3F, 1.0F).step(0.01F));
   private final BooleanSetting skipDelay = this.addSetting("skip-delay", new BooleanSetting("Skip Break Delay", true));
   private final BooleanSetting keepInstantDelay = this.addSetting("keep-instant-delay", new BooleanSetting("Keep Instant Delay", true));

   public SpeedMineModule() {
      super("speed-mine", "Donut Speed Mine", "Finishes breaking blocks before the full break time.", ModuleCategory.DONUT);
      this.keepInstantDelay.visibleWhen(this.skipDelay::get);
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   public static float scaleProgress(BlockState var0, float var1) {
      SpeedMineModule var2 = active;
      return var2 != null && affects(var0) ? var1 / var2.breakAt.get() : var1;
   }

   public static boolean dropsDelay(BlockState var0, float var1) {
      SpeedMineModule var2 = active;
      return var2 != null && var2.skipDelay.get() && affects(var0) ? !var2.keepInstantDelay.get() || var1 < 1.0F : false;
   }

   private static boolean affects(BlockState var0) {
      if (var0.method_26215() || var0.method_27852(Blocks.field_10260)) {
         return false;
      } else {
         return var0.method_27852(Blocks.field_10255) ? true : !var0.method_26164(BlockTags.field_33716) && !var0.method_26164(BlockTags.field_33713);
      }
   }
}
