package net.efn.client.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public final class HandAuraModule extends ClientModule {
   public static volatile HandAuraModule active;
   private static final String COLOR_THEME = "Theme";
   private static final String COLOR_MANUAL = "Manual";
   private static final int WHITE = -1;
   private static final float SHEEN_LIFT = 0.55F;
   private static final long BREATH_NANOS = 3200000000L;
   private static final long SHEEN_NANOS = 4400000000L;
   private final ModeSetting colorMode = this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));
   private final ColorSetting glowColor = this.addSetting("glow-color", new ColorSetting("Glow Color", -13338378));
   private final NumberSetting aura = this.addSetting("aura", new NumberSetting("Aura", 65.0F, 0.0F, 100.0F).step(5.0F));
   private final NumberSetting reach = this.addSetting("reach", new NumberSetting("Reach", 8.0F, 2.0F, 20.0F).step(1.0F));
   private final NumberSetting rim = this.addSetting("rim", new NumberSetting("Rim", 40.0F, 0.0F, 100.0F).step(5.0F));
   private final NumberSetting sheen = this.addSetting("sheen", new NumberSetting("Sheen", 25.0F, 0.0F, 100.0F).step(5.0F));
   private final ColorSetting sheenColor = this.addSetting("sheen-color", new ColorSetting("Sheen Color", -3153665));
   private final BooleanSetting breathe = this.addSetting("breathe", new BooleanSetting("Breathe", true));

   public HandAuraModule() {
      super("hand-aura", "Hand Aura", "Adds a glowing outline to your hand and held item.", ModuleCategory.VISUALS);
      this.glowColor.visibleWhen(this::manual);
      this.reach.visibleWhen(() -> this.aura.get() > 0.0F);
      this.sheenColor.visibleWhen(() -> this.sheen.get() > 0.0F && this.manual());
   }

   @Override
   protected void onEnable() {
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      if (RenderSystem.isOnRenderThread()) {
         HandAuraPass.release();
      } else {
         MinecraftClient.method_1551().execute(HandAuraModule::releaseIfOff);
      }
   }

   private static void releaseIfOff() {
      if (active == null) {
         HandAuraPass.release();
      }
   }

   public static boolean beginCapture() {
      HandAuraModule var0 = active;
      return var0 != null && var0.shows() && HandAuraPass.begin();
   }

   public static void endCapture(boolean var0) {
      if (HandAuraPass.end() && var0) {
         HandAuraModule var1 = active;
         float var2 = Math.max(1.0F, MinecraftClient.method_1551().method_1522().field_1481 / 1080.0F);
         if (var1 == null) {
            HandAuraPass.compose(-1, 0.0F, 2.0F, 0.0F, 2.0F * var2, -1, 0.0F, 0.0F);
         } else {
            long var3 = System.nanoTime();
            float var5 = (float)(var3 % 4400000000L / 4.4E9);
            int var6 = var1.glowArgb();
            HandAuraPass.compose(
               var6,
               var1.auraStrength(var3),
               var1.reach.get() * var2,
               var1.rim.get() / 100.0F,
               2.0F * var2,
               var1.sheenArgb(var6),
               var1.sheen.get() / 100.0F,
               var5
            );
         }
      }
   }

   private boolean manual() {
      return this.colorMode.is("Manual");
   }

   private int glowArgb() {
      return this.manual() ? this.glowColor.argb() : CosmeticAccent.argb();
   }

   private int sheenArgb(int var1) {
      return this.manual() ? this.sheenColor.argb() : ColorHelper.method_61319(0.55F, var1, -1);
   }

   private boolean shows() {
      return this.aura.get() > 0.0F || this.rim.get() > 0.0F;
   }

   private float auraStrength(long var1) {
      float var3 = this.aura.get() / 100.0F;
      if (!this.breathe.get()) {
         return var3;
      } else {
         double var4 = var1 % 3200000000L / 3.2E9;
         float var6 = (float)Math.sin(var4 * 3.141592653589793);
         return var3 * (0.6F + 0.4F * var6 * var6);
      }
   }
}
