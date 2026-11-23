package net.efn.client.esp;

import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EspStyle {
   public static final String OUTLINE = "Outline";
   public static final String BOXES = "Boxes";
   public final ModeSetting shape;
   public final NumberSetting boxSize;
   public final NumberSetting fillAlpha;
   public final NumberSetting lineWidth;
   public final BooleanSetting glow;
   public final BooleanSetting fade;
   public final BooleanSetting reveal;
   public final NumberSetting range;

   public EspStyle(EspStyle.Registrar var1, String var2, float var3, float var4) {
      this.shape = var1.add("shape", new ModeSetting("Shape", var2, "Outline", "Boxes"));
      this.boxSize = var1.add("box-size", new NumberSetting("Box Size", 1.0F, 0.1F, 2.0F).step(0.1F));
      this.fillAlpha = var1.add("fill-alpha", new NumberSetting("Fill Alpha", var3, 0.0F, 1.0F).step(0.05F));
      this.lineWidth = var1.add("line-width", new NumberSetting("Line Width", 1.5F, 0.5F, 5.0F).step(0.5F));
      this.glow = var1.add("glow", new BooleanSetting("Glow", true));
      this.fade = var1.add("distance-fade", new BooleanSetting("Distance Fade", true));
      this.reveal = var1.add("reveal", new BooleanSetting("Reveal Animation", true));
      this.range = var1.add("range", new NumberSetting("Range", var4, 16.0F, 2048.0F).step(1.0F));
      this.boxSize.visibleWhen(() -> this.shape.is("Boxes"));
   }

   public boolean outline() {
      return this.shape.is("Outline");
   }

   @Environment(EnvType.CLIENT)
   public interface Registrar {
      <S extends Setting> S add(String var1, S var2);
   }
}
