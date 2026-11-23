package net.efn.client.module.impl.render;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class ZoomModule extends ClientModule {
   private static final float MIN_ZOOM = 1.1F;
   private static final float MAX_ZOOM = 50.0F;
   private static final float SCROLL_STEP = 1.2F;
   private static final float FAST_RATE = 30.0F;
   private static final float SLOW_RATE = 5.0F;
   private static final float MAX_FRAME_SECONDS = 0.1F;
   private static volatile ZoomModule instance;
   private static float target = 1.0F;
   private static float current = 1.0F;
   private static long lastNanos;
   private final NumberSetting zoom = this.addSetting("zoom", new NumberSetting("Zoom", 4.0F, 1.5F, 20.0F).step(0.5F));
   private final NumberSetting smoothness = this.addSetting("smoothness", new NumberSetting("Smoothness", 60.0F, 0.0F, 100.0F).step(5.0F));
   private final BooleanSetting scroll = this.addSetting("scroll", new BooleanSetting("Scroll", true));
   private final BooleanSetting lowerSensitivity = this.addSetting("lower-sensitivity", new BooleanSetting("Lower Sensitivity", true));
   private final BooleanSetting cinematic = this.addSetting("cinematic", new BooleanSetting("Cinematic Camera", false));
   private boolean smoothBefore;
   private boolean smoothPushed;

   public ZoomModule() {
      super("zoom", "Zoom", "Hold the key to zoom in.", ModuleCategory.VISUALS);
      this.setKeybind(67);
      this.setHoldMode(true);
      instance = this;
   }

   @Override
   protected void onEnable() {
      target = this.zoom.get();
      if (this.cinematic.get()) {
         MinecraftClient var1 = MinecraftClient.getInstance();
         this.smoothBefore = var1.options.smoothCameraEnabled;
         var1.options.smoothCameraEnabled = true;
         this.smoothPushed = true;
      }
   }

   @Override
   protected void onDisable() {
      target = 1.0F;
      if (this.smoothPushed) {
         MinecraftClient.getInstance().options.smoothCameraEnabled = this.smoothBefore;
         this.smoothPushed = false;
      }
   }

   public static float fov(float f) {
      float var1 = advance();
      return var1 > 1.0001F ? f / var1 : f;
   }

   public static double turnScale() {
      ZoomModule var0 = instance;
      return var0 != null && var0.lowerSensitivity.get() && !(current <= 1.0001F) ? 1.0 / current : 1.0;
   }

   public static boolean scroll(double d) {
      ZoomModule var2 = instance;
      if (var2 != null && var2.isEnabled() && var2.scroll.get() && d != 0.0) {
         float var3 = target * (d > 0.0 ? 1.2F : 0.8333333F);
         target = Math.max(1.1F, Math.min(50.0F, var3));
         return true;
      } else {
         return false;
      }
   }

   private static float advance() {
      long var3 = System.nanoTime();
      float var5 = lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(var3 - lastNanos) / 1.0E9F);
      lastNanos = var3;
      ZoomModule var6 = instance;
      float var2 = var6 == null ? 0.0F : var6.smoothness.get() / 100.0F;
      if (!(var2 <= 0.0F) && current != target) {
         float var8 = 30.0F + -25.0F * var2;
         float var9 = 1.0F - (float)Math.exp(-var5 * var8);
         double var10 = Math.log(current);
         if (Math.abs((current = (float)Math.exp(var10 + (Math.log(target) - var10) * var9)) - target) < 0.002F) {
            current = target;
         }

         return current;
      } else {
         current = target;
         return current;
      }
   }
}
