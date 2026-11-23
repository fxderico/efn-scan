package net.efn.client.module.impl.render;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class CustomFovModule extends ClientModule {
   public static volatile CustomFovModule active;
   private static float screenEffectScale = 1.0F;
   private final NumberSetting fov = this.addSetting("fov", new NumberSetting("FOV", 110.0F, 30.0F, 160.0F).step(1.0F));
   private int culledFov = -2147483648;

   public CustomFovModule() {
      super("custom-fov", "Custom Fov", "Changes your field of view (FOV).", ModuleCategory.VISUALS);
      this.fov.onChange(this::onFovChanged);
   }

   @Override
   protected void onEnable() {
      active = this;
      this.culledFov = this.fov.getInt();
      scheduleTerrainUpdate();
   }

   @Override
   protected void onDisable() {
      active = null;
      this.culledFov = -2147483648;
      scheduleTerrainUpdate();
   }

   public float fov() {
      return this.fov.getInt();
   }

   public static void hudFovApplied(float f, float g) {
      screenEffectScale = f == g ? 1.0F : (float)(Math.tan(Math.toRadians(g) * 0.5) / Math.tan(Math.toRadians(f) * 0.5));
   }

   public static float screenEffectScale() {
      return screenEffectScale;
   }

   private void onFovChanged() {
      if (active == this) {
         int var1 = this.fov.getInt();
         if (var1 != this.culledFov) {
            this.culledFov = var1;
            scheduleTerrainUpdate();
         }
      }
   }

   private static void scheduleTerrainUpdate() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (var0 != null) {
         if (!var0.isOnThread()) {
            var0.execute(CustomFovModule::scheduleTerrainUpdate);
         } else {
            if (var0.world != null && var0.worldRenderer != null) {
               var0.worldRenderer.scheduleTerrainUpdate();
            }
         }
      }
   }
}
