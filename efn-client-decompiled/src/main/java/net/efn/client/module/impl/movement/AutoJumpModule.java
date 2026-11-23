package net.efn.client.module.impl.movement;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class AutoJumpModule extends ClientModule {
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Jump", "Jump", "LowHop"));
   private final ModeSetting jumpIf = this.addSetting("jump-if", new ModeSetting("Jump If", "Always", "Sprinting", "Walking", "Always"));
   private final NumberSetting velocityHeight = this.addSetting("velocity-height", new NumberSetting("Velocity Height", 0.25F, 0.0F, 2.0F).step(0.05F));

   public AutoJumpModule() {
      super("auto-jump", "Auto Jump", "Automatically jumps.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
   }

   private void onPreTick(TickEvent var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 != null) {
         if (var2.method_24828() && !var2.method_5715() && this.shouldJump(var2)) {
            if (this.mode.is("Jump")) {
               var2.method_6043();
            } else {
               Vec3d var3 = var2.method_18798();
               var2.method_18800(var3.field_1352, this.velocityHeight.get(), var3.field_1350);
            }
         }
      }
   }

   private boolean shouldJump(ClientPlayerEntity var1) {
      boolean var2 = var1.field_6250 != 0.0F || var1.field_6212 != 0.0F;
      String var3 = this.jumpIf.get();

      return switch (var3) {
         case "Sprinting" -> var1.method_5624() && var2;
         case "Walking" -> var2;
         default -> true;
      };
   }
}
