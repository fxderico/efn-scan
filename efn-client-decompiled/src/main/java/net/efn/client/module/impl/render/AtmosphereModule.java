package net.efn.client.module.impl.render;

import com.example.client.mixin.AccessorAtmosphereClientLevel;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.joml.Vector3fc;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class AtmosphereModule extends ClientModule {
   private static final String CUSTOM = "Custom";
   private static final String AURORA_ALWAYS = "Always";
   private static final String AURORA_COLD = "Cold Biomes";
   private static final String AURORA_OFF = "Off";
   private static final float MOON_SCALE = 1.4F;
   private static final AtmosphereModule.Theme[] THEMES = new AtmosphereModule.Theme[]{
      new AtmosphereModule.Theme("Borealis", 198414, 729648, 13073302, 3080090, 9063935, 13163263),
      new AtmosphereModule.Theme("Nebula", 459791, 2233916, 14053304, 16732120, 5991679, 15124735),
      new AtmosphereModule.Theme("EFN", 657157, 2890512, 15239498, 16757306, 16730670, 16769200),
      new AtmosphereModule.Theme("Frost", 264724, 1454154, 11057384, 8382719, 15135999, 14544639),
      new AtmosphereModule.Theme("Crimson", 721926, 3018260, 15227490, 16724821, 16751164, 16765640)
   };
   private final ModeSetting theme = this.addSetting("theme", new ModeSetting("Theme", "Borealis", "Borealis", "Nebula", "EFN", "Frost", "Crimson", "Custom"));
   private final ColorSetting nightColor = this.addSetting("night-color", new ColorSetting("Night Sky Color", -16047568));
   private final ColorSetting sunsetColor = this.addSetting("sunset-color", new ColorSetting("Sunset Color", -3703914));
   private final ColorSetting auroraColor = this.addSetting("aurora-color", new ColorSetting("Aurora Color", -13697126));
   private final ColorSetting auroraTopColor = this.addSetting("aurora-top-color", new ColorSetting("Aurora Top Color", -7713281));
   private final ModeSetting skyShader = this.addSetting(
      "sky-shader", new ModeSetting("Sky Shader", "Off", "Off", "Space", "Northern Lights", "Ringed Planet", "Black Hole")
   );
   private final NumberSetting shaderDaytime = this.addSetting("sky-shader-daytime", new NumberSetting("Daytime Strength", 1.0F, 0.0F, 1.0F).step(0.05F));
   private final NumberSetting shaderBrightness = this.addSetting("sky-shader-brightness", new NumberSetting("Shader Brightness", 1.0F, 0.2F, 2.0F).step(0.1F));
   private final NumberSetting shaderSpeed = this.addSetting("sky-shader-speed", new NumberSetting("Shader Speed", 1.0F, 0.0F, 3.0F).step(0.1F));
   private final NumberSetting shaderStars = this.addSetting("sky-shader-stars", new NumberSetting("Shader Stars", 1.0F, 0.0F, 2.0F).step(0.1F));
   private final NumberSetting shaderEffect = this.addSetting("sky-shader-effect", new NumberSetting("Nebula / Aurora Strength", 1.0F, 0.0F, 2.0F).step(0.1F));
   private final NumberSetting shaderBand = this.addSetting("sky-shader-galaxy", new NumberSetting("Galaxy Band", 1.0F, 0.0F, 2.0F).step(0.1F));
   private final BooleanSetting gradientSky = this.addSetting("gradient-sky", new BooleanSetting("Gradient Sky", true));
   private final BooleanSetting keepInRain = this.addSetting("keep-in-rain", new BooleanSetting("Keep In Rain", true));
   private final BooleanSetting celestialGlow = this.addSetting("sun-moon-glow", new BooleanSetting("Sun & Moon Glow", true));
   private final BooleanSetting stars = this.addSetting("stars", new BooleanSetting("Stars", true));
   private final NumberSetting starBrightness = this.addSetting("star-brightness", new NumberSetting("Star Brightness", 1.3F, 0.5F, 2.0F).step(0.1F));
   private final BooleanSetting milkyWay = this.addSetting("milky-way", new BooleanSetting("Milky Way", true));
   private final NumberSetting milkyWayBrightness = this.addSetting(
      "milky-way-brightness", new NumberSetting("Milky Way Brightness", 1.0F, 0.2F, 2.0F).step(0.1F)
   );
   private final BooleanSetting shootingStars = this.addSetting("shooting-stars", new BooleanSetting("Shooting Stars", true));
   private final ModeSetting aurora = this.addSetting("aurora", new ModeSetting("Aurora", "Always", "Always", "Cold Biomes", "Off"));
   private final NumberSetting auroraBrightness = this.addSetting("aurora-brightness", new NumberSetting("Aurora Brightness", 1.0F, 0.2F, 2.0F).step(0.1F));
   private final NumberSetting auroraSpeed = this.addSetting("aurora-speed", new NumberSetting("Aurora Speed", 1.0F, 0.2F, 3.0F).step(0.1F));
   private final BooleanSetting cloudColors = this.addSetting("cloud-colors", new BooleanSetting("Cloud Colors", true));
   private final BooleanSetting endSky = this.addSetting("end-sky", new BooleanSetting("End Sky", true));
   private final BooleanSetting netherColors = this.addSetting("nether-colors", new BooleanSetting("Nether Colors", true));
   private final AtmosphereSky sky = new AtmosphereSky();
   private final AtmosphereStars starField = new AtmosphereStars();
   private final AtmosphereAurora auroraMesh = new AtmosphereAurora();
   private final AtmosphereShaderSky shaderSky = new AtmosphereShaderSky();
   private final MatrixStack world = new MatrixStack();
   private float night;
   private float rainBrightness = 1.0F;
   private float auroraHere;
   private long lastFrame;
   private boolean moonScaled;
   private boolean shaderRendered;

   public AtmosphereModule() {
      super("atmosphere", "Atmosphere", "A custom sky with stars and an aurora.", ModuleCategory.VISUALS);
      this.nightColor.visibleWhen(() -> this.theme.is("Custom"));
      this.sunsetColor.visibleWhen(() -> this.theme.is("Custom"));
      this.auroraColor.visibleWhen(() -> this.theme.is("Custom"));
      this.auroraTopColor.visibleWhen(() -> this.theme.is("Custom"));
      this.shaderDaytime.visibleWhen(this::shaderOn);
      this.shaderBrightness.visibleWhen(this::shaderOn);
      this.shaderSpeed.visibleWhen(this::shaderOn);
      this.shaderStars.visibleWhen(this::shaderOn);
      this.shaderEffect.visibleWhen(this::shaderOn);
      this.shaderBand.visibleWhen(this::shaderOn);
      this.stars.visibleWhen(this::ownSky);
      this.starBrightness.visibleWhen(() -> this.stars.get() && this.ownSky());
      this.milkyWay.visibleWhen(this::ownSky);
      this.milkyWayBrightness.visibleWhen(() -> this.milkyWay.get() && this.ownSky());
      this.aurora.visibleWhen(this::ownSky);
      this.auroraBrightness.visibleWhen(() -> !this.aurora.is("Off") && this.ownSky());
      this.auroraSpeed.visibleWhen(() -> !this.aurora.is("Off") && this.ownSky());
   }

   public static AtmosphereModule get() {
      AtmosphereModule var0 = ModuleRegistry.get(AtmosphereModule.class);
      return var0 != null && var0.isEnabled() ? var0 : null;
   }

   @Override
   protected void onDisable() {
      this.sky.close();
      this.starField.close();
      this.auroraMesh.close();
      this.shaderSky.close();
      this.shaderRendered = false;
      this.lastFrame = 0L;
      this.auroraHere = 0.0F;
   }

   public void extract(ClientWorld var1, float var2, SkyRenderState var3) {
      this.shaderRendered = false;
      this.night = MathHelper.method_15363(var3.field_63094 * 2.0F, 0.0F, 1.0F);
      float var4 = 1.0F - var3.field_63093;
      float var5 = var1.method_8478(var2);
      if (this.keepInRain.get()) {
         this.night = Math.max(this.night, clearNight(var3.field_63091) * var4);
         var3.field_63094 = Math.max(var3.field_63094, this.night * 0.5F);
         var3.field_63093 = 1.0F;
         var4 = 0.0F;
         var5 = 0.0F;
      }

      this.rainBrightness = 1.0F - var4;
      AtmosphereModule.Theme var6 = this.currentTheme();
      int var7 = ((AccessorAtmosphereClientLevel)var1).efn$atmosphereSkyFlash();
      this.sky
         .update(
            var3.field_63091,
            var3.field_64465,
            var4,
            var5,
            var7 > 0 ? Math.min(1.0F, (var7 - var2) / 2.0F) : 0.0F,
            var6.nightZenith,
            var6.nightHorizon,
            var6.twilight
         );
   }

   public boolean keepsSkyInRain() {
      return this.keepInRain.get();
   }

   private static float clearNight(float var0) {
      float var1 = MathHelper.method_15363(0.75F - 2.0F * MathHelper.method_15362(var0), 0.0F, 1.0F);
      return var1 * var1;
   }

   public boolean skyDisc() {
      if (this.gradientSky.get() && this.sky.ready()) {
         this.sky.renderDome(this.world);
         this.renderShaderSky();
         return true;
      } else {
         return false;
      }
   }

   public void afterSkyDisc() {
      this.renderShaderSky();
   }

   private boolean ownSky() {
      return !this.shaderOn();
   }

   public int lyricGlow() {
      return this.currentTheme().auroraHigh;
   }

   private void renderShaderSky() {
      if (this.shaderOn()) {
         AtmosphereModule.Theme var1 = this.currentTheme();
         this.shaderRendered = this.shaderSky
            .render(
               this.skyShader.get(),
               this.shaderVisibility(),
               this.shaderBrightness.get(),
               this.shaderSpeed.get(),
               this.shaderStars.get(),
               this.shaderEffect.get(),
               this.shaderBand.get(),
               var1.auroraLow,
               var1.auroraHigh,
               var1.milkyWay,
               var1.nightZenith,
               var1.nightHorizon
            );
      }
   }

   private boolean shaderOn() {
      return !this.skyShader.is("Off");
   }

   private boolean shaderAurora() {
      return this.skyShader.is("Northern Lights");
   }

   private float shaderVisibility() {
      return MathHelper.method_16439(this.night, this.shaderDaytime.get(), 1.0F) * (0.4F + 0.6F * this.rainBrightness);
   }

   public boolean replacesSunrise() {
      return this.gradientSky.get() && this.sky.ready();
   }

   public boolean stars(MatrixStack var1) {
      if (this.shaderRendered) {
         return true;
      } else {
         float var2 = this.night * this.rainBrightness;
         if (this.milkyWay.get()) {
            this.starField.renderBand(var1, var2 * this.milkyWayBrightness.get(), this.currentTheme().milkyWay);
         }

         if (!this.stars.get()) {
            return false;
         } else {
            this.starField.renderStars(var1, var2, this.starBrightness.get());
            return true;
         }
      }
   }

   public void beforeMoon(MatrixStack var1) {
      this.moonScaled = this.celestialGlow.get();
      if (this.moonScaled) {
         var1.method_22903();
         var1.method_22905(1.4F, 1.0F, 1.4F);
      }
   }

   public void afterMoon(MatrixStack var1) {
      if (this.moonScaled) {
         var1.method_22909();
      }

      this.moonScaled = false;
   }

   public void afterCelestials() {
      long var1 = System.nanoTime();
      float var3 = this.lastFrame == 0L ? 0.0F : Math.min((float)(var1 - this.lastFrame) / 1.0E9F, 0.1F);
      this.lastFrame = var1;
      if (this.sky.ready()) {
         if (this.celestialGlow.get()) {
            this.sky.renderHalos(this.world, this.rainBrightness, this.night);
         }

         if (this.shootingStars.get()) {
            this.sky.renderMeteors(this.world, this.night, this.rainBrightness);
         }

         this.auroraHere = this.auroraHere + (this.auroraTarget() - this.auroraHere) * Math.min(1.0F, var3 * 0.5F);
         if (this.auroraHere > 0.01F && this.night > 0.01F && !this.shaderRendered) {
            AtmosphereModule.Theme var4 = this.currentTheme();
            float var5 = (float)(var1 % 3600000000000L) / 1.0E9F * this.auroraSpeed.get();
            float var6 = this.auroraHere * this.night * (0.35F + 0.65F * this.rainBrightness) * this.auroraBrightness.get();
            this.auroraMesh.render(this.world, var6, var5, var4.auroraLow, var4.auroraHigh);
         }
      }
   }

   private float auroraTarget() {
      if (this.aurora.is("Off")) {
         return 0.0F;
      } else if (this.aurora.is("Always")) {
         return 1.0F;
      } else {
         MinecraftClient var1 = MinecraftClient.method_1551();
         if (var1.field_1687 == null) {
            return 0.0F;
         } else {
            BlockPos var2 = BlockPos.method_49638(var1.field_1773.method_19418().method_71156());
            return ((Biome)var1.field_1687.method_23753(var2).comp_349()).method_33599(var2, var1.field_1687.method_8615()) ? 1.0F : 0.0F;
         }
      }
   }

   public void fog(Camera var1, ClientWorld var2, Vector4f var3) {
      if (var2.method_27983() == World.field_25179) {
         if (this.gradientSky.get() && this.sky.ready()) {
            Vector3fc var4 = var1.method_19335();
            int var5 = this.sky.horizonToward(var4.x(), var4.z());
            var3.x = (var5 >> 16 & 0xFF) / 255.0F;
            var3.y = (var5 >> 8 & 0xFF) / 255.0F;
            var3.z = (var5 & 0xFF) / 255.0F;
         }

         if (this.shaderOn()) {
            this.shaderFog(var3);
         }
      } else if (var2.method_27983() == World.field_25180 && this.netherColors.get()) {
         float var6 = 0.2126F * var3.x + 0.7152F * var3.y + 0.0722F * var3.z;
         var3.x = MathHelper.method_15363((var6 + (var3.x - var6) * 1.45F) * 1.12F, 0.0F, 1.0F);
         var3.y = MathHelper.method_15363((var6 + (var3.y - var6) * 1.45F) * 1.12F, 0.0F, 1.0F);
         var3.z = MathHelper.method_15363((var6 + (var3.z - var6) * 1.45F) * 1.12F, 0.0F, 1.0F);
      }
   }

   private void shaderFog(Vector4f var1) {
      int var2 = this.currentTheme().nightHorizon;
      float var3 = this.shaderAurora() ? 0.85F : 0.3F;
      float var4 = this.shaderVisibility() * 0.85F;
      var1.x = MathHelper.method_16439(var4, var1.x, (var2 >> 16 & 0xFF) / 255.0F * var3);
      var1.y = MathHelper.method_16439(var4, var1.y, (var2 >> 8 & 0xFF) / 255.0F * var3);
      var1.z = MathHelper.method_16439(var4, var1.z, (var2 & 0xFF) / 255.0F * var3);
   }

   public int clouds(int var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (this.cloudColors.get() && this.sky.ready() && var2.field_1687 != null && var2.field_1687.method_27983() == World.field_25179) {
         float var3 = this.sky.golden();
         float var4 = this.sky.nightness();
         int var5 = ColorHelper.method_61319(var3, ColorHelper.method_61319(var4, 16777215, this.currentTheme().nightHorizon), this.sky.twilightGlow());
         float var6 = Math.max(var3 * 0.75F, var4 * 0.55F);
         return keepBrightness(var1, var5, var6);
      } else {
         return var1;
      }
   }

   public boolean endSky() {
      if (!this.endSky.get()) {
         return false;
      } else {
         long var1 = System.nanoTime();
         this.sky.renderEndDome(this.world);
         MatrixStack var3 = new MatrixStack();
         var3.method_22907(RotationAxis.field_40716.rotationDegrees((float)(var1 % 1200000000000L) / 1.0E9F * 0.3F));
         this.starField.renderNebulae(var3, 1.0F);
         this.starField.renderStars(var3, 1.0F, 1.2F);
         return true;
      }
   }

   private AtmosphereModule.Theme currentTheme() {
      for (AtmosphereModule.Theme var4 : THEMES) {
         if (this.theme.is(var4.name)) {
            return var4;
         }
      }

      int var5 = this.nightColor.rgb();
      return new AtmosphereModule.Theme(
         "Custom", AtmosphereSky.scale(var5, 0.3F), var5, this.sunsetColor.rgb(), this.auroraColor.rgb(), this.auroraTopColor.rgb(), 15263999
      );
   }

   private static int keepBrightness(int var0, int var1, float var2) {
      if (var2 <= 0.0F) {
         return var0;
      } else {
         float var3 = (var0 >> 16 & 0xFF) / 255.0F;
         float var4 = (var0 >> 8 & 0xFF) / 255.0F;
         float var5 = (var0 & 0xFF) / 255.0F;
         float var6 = (var1 >> 16 & 0xFF) / 255.0F;
         float var7 = (var1 >> 8 & 0xFF) / 255.0F;
         float var8 = (var1 & 0xFF) / 255.0F;
         float var9 = luminance(var3, var4, var5) / Math.max(luminance(var6, var7, var8), 0.001F);
         var3 = MathHelper.method_16439(var2, var3, Math.min(1.0F, var6 * var9));
         var4 = MathHelper.method_16439(var2, var4, Math.min(1.0F, var7 * var9));
         var5 = MathHelper.method_16439(var2, var5, Math.min(1.0F, var8 * var9));
         return var0 & 0xFF000000 | channel(var3) << 16 | channel(var4) << 8 | channel(var5);
      }
   }

   private static float luminance(float var0, float var1, float var2) {
      return 0.2126F * var0 + 0.7152F * var1 + 0.0722F * var2;
   }

   private static int channel(float var0) {
      return MathHelper.method_15340(Math.round(var0 * 255.0F), 0, 255);
   }

   @Environment(EnvType.CLIENT)
   private record Theme(String name, int nightZenith, int nightHorizon, int twilight, int auroraLow, int auroraHigh, int milkyWay) {
   }
}
