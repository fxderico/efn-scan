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

   private void onPreTick(TickEvent tickEvent) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var2 != null) {
         if (var2.isOnGround() && !var2.isSneaking() && this.shouldJump(var2)) {
            if (this.mode.is("Jump")) {
               var2.jump();
            } else {
               Vec3d var3 = var2.getVelocity();
               var2.setVelocity(var3.x, this.velocityHeight.get(), var3.z);
            }
         }
      }
   }

   private boolean shouldJump(ClientPlayerEntity clientPlayerEntity) {
      boolean var2 = clientPlayerEntity.forwardSpeed != 0.0F || clientPlayerEntity.sidewaysSpeed != 0.0F;
      String var3 = this.jumpIf.get();

      return switch (var3) {
         case "Sprinting" -> clientPlayerEntity.isSprinting() && var2;
         case "Walking" -> var2;
         default -> true;
      };
   }
}
