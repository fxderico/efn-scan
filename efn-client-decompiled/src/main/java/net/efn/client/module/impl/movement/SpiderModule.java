package net.efn.client.module.impl.movement;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class SpiderModule extends ClientModule {
   private static final double CLIMB_VELOCITY_THRESHOLD = 0.2;
   private final NumberSetting climbSpeed = this.addSetting("climb-speed", new NumberSetting("Climb Speed", 0.2F, 0.0F, 10.0F).step(0.1F));

   public SpiderModule() {
      super("spider", "Spider", "Allows you to climb walls like a spider.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
   }

   private void onPostTick(TickEvent var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 != null && var2.field_5976) {
         Vec3d var3 = var2.method_18798();
         if (!(var3.field_1351 >= 0.2)) {
            var2.method_18800(var3.field_1352, this.climbSpeed.get(), var3.field_1350);
         }
      }
   }
}
