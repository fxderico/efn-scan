package net.efn.client.config;

import java.util.List;
import net.efn.client.module.StableId;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Surface {
   public static final String BG_NONE = "None";
   public static final String BG_IMAGE = "Image";
   public static final String BG_FLOW = "Flow";
   public static final String BG_TOPOGRAPHIC = "Topographic";
   public static final String BG_RINGS = "Rings";
   public static final String BG_GRID = "Grid";
   public static final String MAP_EACH = "Each";
   public static final String MAP_SCREEN = "Screen";
   public static final String COVER_FULL = "Full";
   public static final String COVER_HEADER = "Header";
   public final String label;
   public final boolean card;
   public final ColorSetting color;
   public final NumberSetting opacity;
   public final ModeSetting background;
   public final StringSetting image = new StringSetting("Image", "");
   public final ModeSetting mapping = new ModeSetting("Mapping", "Each", "Each", "Screen");
   public final ModeSetting coverage = new ModeSetting("Coverage", "Header", "Header", "Full");
   public final NumberSetting headerHeight = new NumberSetting("Header height", 120.0F, 70.0F, 220.0F).step(1.0F);
   public final NumberSetting imageOpacity;
   public final NumberSetting imageScale = new NumberSetting("Image scale", 1.0F, 0.5F, 3.0F).step(0.1F);
   public final NumberSetting imageX = new NumberSetting("Image X", 0.5F, 0.0F, 1.0F).step(0.05F);
   public final NumberSetting imageY = new NumberSetting("Image Y", 0.5F, 0.0F, 1.0F).step(0.05F);
   public final NumberSetting lineOpacity = new NumberSetting("Line opacity", 0.5F, 0.1F, 1.0F).step(0.05F);
   private final List<Setting> settings;

   Surface(String var1, String var2, boolean var3, int var4, float var5, float var6) {
      this(var1, var2, var3, var4, var5, var6, "None");
   }

   Surface(String var1, String var2, boolean var3, int var4, float var5, float var6, String var7) {
      this.label = var2;
      this.card = var3;
      this.color = new ColorSetting("Color", var4);
      this.background = new ModeSetting("Background", var7, "None", "Image", "Flow", "Topographic", "Rings", "Grid");
      this.opacity = new NumberSetting("Opacity", var5, 0.2F, 1.0F).step(0.01F);
      this.imageOpacity = new NumberSetting("Image opacity", var6, 0.05F, 1.0F).step(0.05F);
      this.settings = List.of(
         this.color,
         this.opacity,
         this.background,
         this.image,
         this.mapping,
         this.coverage,
         this.headerHeight,
         this.imageOpacity,
         this.imageScale,
         this.imageX,
         this.imageY,
         this.lineOpacity
      );

      for (Setting var9 : this.settings) {
         String var10 = var9 == this.imageY ? "Image focus" : var9.name();
         var9.bindId(var1 + "." + StableId.legacy(var10));
      }

      this.image.visibleWhen(() -> false);
      this.image.privateValue();
      this.mapping.visibleWhen(() -> !var3 && !this.background.is("None"));
      this.coverage.visibleWhen(() -> var3 && !this.background.is("None"));
      this.headerHeight.visibleWhen(() -> var3 && !this.background.is("None") && this.coverage.is("Header"));
      this.imageOpacity.visibleWhen(this::isImage);
      this.imageScale.visibleWhen(this::isImage);
      this.imageX.visibleWhen(this::isImage);
      this.imageY.visibleWhen(this::isImage);
      this.lineOpacity.visibleWhen(this::isPattern);
   }

   public boolean isImage() {
      return this.background.is("Image");
   }

   public boolean isPattern() {
      return !this.background.is("None") && !this.background.is("Image");
   }

   public boolean isBanner() {
      return this.card && this.coverage.is("Header");
   }

   public boolean isScreenMapped() {
      return !this.card && this.mapping.is("Screen");
   }

   public List<Setting> settings() {
      return this.settings;
   }
}
