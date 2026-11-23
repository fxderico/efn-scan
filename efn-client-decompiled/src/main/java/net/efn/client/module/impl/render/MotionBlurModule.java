package net.efn.client.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class MotionBlurModule extends ClientModule {
   public static volatile MotionBlurModule active;
   private final NumberSetting strength = this.addSetting("strength", new NumberSetting("Strength", 50.0F, 10.0F, 90.0F).step(5.0F));

   public MotionBlurModule() {
      super("motion-blur", "Motion Blur", "Adds motion blur when you turn.", ModuleCategory.VISUALS);
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      if (RenderSystem.isOnRenderThread()) {
         MotionBlurPass.release();
      } else {
         MinecraftClient.method_1551().execute(MotionBlurModule::releaseIfOff);
      }
   }

   private static void releaseIfOff() {
      if (active == null) {
         MotionBlurPass.release();
      }
   }

   public static void afterWorld(boolean var0) {
      MotionBlurModule var1 = active;
      if (var1 != null) {
         MotionBlurPass.apply(var0, var1.strength.get() / 100.0F);
      }
   }
}
