package net.efn.client.module.impl.client;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

@Environment(EnvType.CLIENT)
public final class RadarModule extends ClientModule {
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 48.0F, 8.0F, 256.0F).step(1.0F));
   private final NumberSetting size = this.addSetting("size", new NumberSetting("Size", 150.0F, 80.0F, 320.0F).step(1.0F));
   private final BooleanSetting rotate = this.addSetting("rotate", new BooleanSetting("Rotate", true));
   private final ColorSetting dotColor = this.addSetting("dot-color", new ColorSetting("Player Color", -47814));

   public RadarModule() {
      super("radar", "Radar", "A round radar of the players around you.", ModuleCategory.CLIENT);
   }

   public static RadarModule get() {
      return ModuleRegistry.get(RadarModule.class);
   }

   public static boolean shouldShowHud() {
      RadarModule var0 = get();
      return var0 != null && var0.isEnabled();
   }

   public float range() {
      return this.range.get();
   }

   public float size() {
      return this.size.get();
   }

   public boolean rotate() {
      return this.rotate.get();
   }

   public int dotColor() {
      return this.dotColor.argb();
   }

   public boolean shows(PlayerEntity playerEntity) {
      return !(playerEntity instanceof ClientPlayerEntity) && !playerEntity.isRemoved() && !playerEntity.isDead() && !playerEntity.isSpectator();
   }
}
