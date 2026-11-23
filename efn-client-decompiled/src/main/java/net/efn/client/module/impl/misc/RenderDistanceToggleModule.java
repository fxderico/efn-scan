package net.efn.client.module.impl.misc;

import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class RenderDistanceToggleModule extends ClientModule {
   private static final int RESTORE_AFTER_TICKS = 10;
   private static final int REARM_MARGIN = 2;
   private final NumberSetting lowDistance = this.addSetting("low-distance", new NumberSetting("Low Distance", 2.0F, 2.0F, 32.0F).step(1.0F));
   private final NumberSetting resetY = this.addSetting("reset-y", new NumberSetting("Reset Y", -1.0F, -64.0F, 320.0F).step(1.0F));
   private boolean armed = true;
   private boolean restoring;
   private int timer;
   private int savedDistance = -1;

   public RenderDistanceToggleModule() {
      super("render-distance-toggle", "Render Distance Toggle", "Reloads chunks when you go below a Y level.", ModuleCategory.BASE_RAID);
   }

   @Override
   protected void onEnable() {
      this.armed = true;
      this.restoring = false;
      this.timer = 0;
      this.savedDistance = -1;
   }

   @Override
   protected void onDisable() {
      if (this.restoring) {
         this.restore(MinecraftClient.method_1551());
      }
   }

   @EventHandler(
      stage = Stage.POST
   )
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 != null && var2.field_1687 != null) {
         int var3 = var2.field_1724.method_31478();
         if (!this.restoring && var3 > this.resetY.getInt() + 2) {
            this.armed = true;
         }

         if (this.armed && var3 <= this.resetY.getInt()) {
            this.armed = false;
            this.restoring = true;
            this.timer = 0;
            this.savedDistance = (Integer)var2.field_1690.method_42503().method_41753();
            apply(var2, this.lowDistance.getInt());
         } else if (this.restoring && ++this.timer >= 10) {
            this.restore(var2);
         }
      } else {
         if (this.restoring) {
            this.restore(var2);
         }
      }
   }

   private void restore(MinecraftClient var1) {
      if (this.savedDistance >= 0) {
         apply(var1, this.savedDistance);
      }

      this.restoring = false;
   }

   private static void apply(MinecraftClient var0, int var1) {
      var0.field_1690.method_42503().method_41748(var1);
      var0.field_1690.method_1643();
   }
}
