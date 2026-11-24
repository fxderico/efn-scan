package net.efn.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.efn.client.imgui.UiFont;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ThemeStore {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final ThemeStore INSTANCE = new ThemeStore();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 3;
   private static final String THEME_KEY = "efnTheme";
   public static final List<ThemeStore.Preset> PRESETS = List.of(
      new ThemeStore.Preset("Cyan", -15408912, -15592938, -15263972),
      new ThemeStore.Preset("Azure", -12743681, -15592938, -15263972),
      new ThemeStore.Preset("Cobalt", -11903489, -15592938, -15263972),
      new ThemeStore.Preset("Lime", -4721846, -15592938, -15263972),
      new ThemeStore.Preset("Ember", -42442, -15592938, -15263972),
      new ThemeStore.Preset("Mono", -1513238, -15592938, -15263972)
   );
   public final ColorSetting accent = new ColorSetting("Accent", -15408912);
   public final NumberSetting rounding = new NumberSetting("Corner radius", 16.0F, 0.0F, 16.0F).step(1.0F);
   public final NumberSetting backgroundDim = new NumberSetting("Background dim", 0.55F, 0.0F, 0.8F).step(0.05F);
   public final BooleanSetting menuBlur = new BooleanSetting("Menu blur", true);
   public final BooleanSetting moduleDividers = new BooleanSetting("Module dividers", true);
   public final NumberSetting uiScale = new NumberSetting("UI scale", 1.0F, 0.75F, 1.5F).step(0.05F);
   public final ModeSetting font = new ModeSetting("Font", UiFont.GEIST.label, UiFont.labels());
   public final NumberSetting animationSpeed = new NumberSetting("Animation speed", 1.3F, 0.0F, 2.0F).step(0.1F);
   public final BooleanSetting animateLines = new BooleanSetting("Animate patterns", true);
   public final BooleanSetting customMainMenu = new BooleanSetting("Custom menus", true);
   public final BooleanSetting communityPopup = new BooleanSetting("Community popup", true);
   public final Surface panels = new Surface("panels", "Panels", false, -14736856, 1.0F, 0.45F, "Flow");
   public final Surface cards = new Surface("cards", "Cards", true, -15263458, 1.0F, 0.85F);
   public final Surface tabBar = new Surface("tabbar", "Tab bar", false, -15263458, 1.0F, 0.5F, "Flow");
   private final List<Surface> surfaces = List.of(this.panels, this.cards, this.tabBar);
   private final List<Setting> shared;
   private final List<Setting> settings;
   private final JsonObject defaults;
   private boolean loaded;
   private boolean readOnly;

   private ThemeStore() {
      ArrayList var1 = new ArrayList<>(
         List.of(
            this.accent, this.rounding, this.backgroundDim, this.menuBlur, this.moduleDividers, this.uiScale, this.font, this.animationSpeed, this.animateLines
         )
      );

      for (Surface var3 : this.surfaces) {
         var1.addAll(var3.settings());
      }

      this.shared = List.copyOf(var1);
      var1.add(this.customMainMenu);
      var1.add(this.communityPopup);
      this.settings = List.copyOf(var1);
      this.defaults = snapshot(this.settings, true);
      this.customMainMenu.onChange(() -> {
         MinecraftClient var0 = MinecraftClient.getInstance();
         if (var0 != null && var0.getWindow() != null) {
            var0.updateWindowTitle();
         }
      });
   }

   public static ThemeStore get() {
      INSTANCE.ensureLoaded();
      return INSTANCE;
   }

   public List<Surface> surfaces() {
      return this.surfaces;
   }

   public void applyPreset(ThemeStore.Preset preset) {
      this.accent.setArgb(preset.accent());
      this.panels.color.setArgb(preset.panel());
      this.cards.color.setArgb(preset.card());
      this.tabBar.color.setArgb(preset.card());
   }

   public boolean matchesPreset(ThemeStore.Preset preset) {
      return this.accent.rgb() == (preset.accent() & 16777215)
         && this.panels.color.rgb() == (preset.panel() & 16777215)
         && this.cards.color.rgb() == (preset.card() & 16777215);
   }

   public void resetToDefaults() {
      apply(this.shared, this.defaults, true);
   }

   public static Path backgroundsDir() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("backgrounds");
   }

   public String exportString() {
      JsonObject var1 = new JsonObject();
      var1.add("efnTheme", snapshot(this.shared, false));
      return var1.toString();
   }

   public boolean importString(String string) {
      if (string != null && !string.isBlank()) {
         try {
            JsonElement var2 = JsonParser.parseString(string.trim());
            if (var2.isJsonObject() && var2.getAsJsonObject().has("efnTheme")) {
               JsonElement var3 = var2.getAsJsonObject().get("efnTheme");
               if (!var3.isJsonObject()) {
                  return false;
               } else {
                  apply(this.shared, var3.getAsJsonObject(), false);
                  return true;
               }
            } else {
               return false;
            }
         } catch (RuntimeException var4) {
            return false;
         }
      } else {
         return false;
      }
   }

   public JsonObject profileSnapshot() {
      return snapshot(this.shared, true);
   }

   public void applyProfile(JsonObject jsonObject) {
      for (Setting var3 : this.shared) {
         JsonElement var4 = jsonObject.get(var3.id());
         if (var4 == null && !var3.isPrivate()) {
            var4 = this.defaults.get(var3.id());
         }

         if (var4 != null) {
            var3.deserialize(var4);
         }
      }
   }

   public JsonObject shareable(JsonObject jsonObject) {
      JsonObject var2 = new JsonObject();

      for (Setting var4 : this.shared) {
         JsonElement var5 = jsonObject.get(var4.id());
         if (var5 != null && !var4.isPrivate() && !Setting.sameValue(var5, this.defaults.get(var4.id()))) {
            var2.add(var4.id(), var5.deepCopy());
         }
      }

      return var2;
   }

   public synchronized void load() {
      if (!this.loaded) {
         this.loaded = true;
         Path var1 = this.configPath();
         if (Files.isRegularFile(var1)) {
            try {
               try (BufferedReader var2 = Files.newBufferedReader(var1)) {
                  JsonObject var4 = JsonParser.parseReader(var2).getAsJsonObject();
                  int var3 = var4.has("version") ? var4.get("version").getAsInt() : 0;
                  if (var3 <= 3) {
                     if (var4.has("values") && var4.get("values").isJsonObject()) {
                        apply(this.settings, var4.getAsJsonObject("values"), true);
                     }

                     if (var3 < 3) {
                        this.adoptNewDefaults();
                     }

                     return;
                  }

                  this.readOnly = true;
                  LOGGER.warn("[EFN] Theme version {} is newer than supported {}; skipping load", var3, 3);
               }
            } catch (RuntimeException | IOException var8) {
               LOGGER.warn("[EFN] Failed to load theme; preserving the file", (Throwable)var8);
               this.readOnly = true;
            }
         }
      }
   }

   private void adoptNewDefaults() {
      if (this.panels.background.is("None")) {
         this.panels.background.set("Flow");
      }

      if (Math.abs(this.panels.opacity.get() - 0.92F) < 0.001F) {
         this.panels.opacity.set(1.0F);
      }

      for (Surface var2 : List.of(this.cards, this.tabBar)) {
         if (Math.abs(var2.opacity.get() - 0.97F) < 0.001F) {
            var2.opacity.set(1.0F);
         }
      }
   }

   public synchronized void save() {
      if (!this.readOnly && this.loaded) {
         JsonObject var1 = new JsonObject();
         var1.addProperty("version", 3);
         var1.add("values", snapshot(this.settings, true));
         Path var2 = this.configPath();

         try {
            Files.createDirectories(var2.getParent());
            Path var3 = var2.resolveSibling(var2.getFileName().toString() + ".tmp");

            try (BufferedWriter var4 = Files.newBufferedWriter(var3)) {
               GSON.toJson((JsonElement)var1, var4);
            }

            try {
               Files.move(var3, var2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException var8) {
               Files.move(var3, var2, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException var10) {
            LOGGER.warn("[EFN] Failed to save theme", (Throwable)var10);
         }
      }
   }

   private static JsonObject snapshot(List<Setting> list, boolean bl) {
      JsonObject var2 = new JsonObject();

      for (Setting var4 : list) {
         if (bl || !var4.isPrivate()) {
            var2.add(var4.id(), var4.serialize());
         }
      }

      return var2;
   }

   private static void apply(List<Setting> list, JsonObject jsonObject, boolean bl) {
      for (Setting var4 : list) {
         if ((bl || !var4.isPrivate()) && jsonObject.has(var4.id())) {
            var4.deserialize(jsonObject.get(var4.id()));
         }
      }
   }

   private void ensureLoaded() {
      if (!this.loaded) {
         this.load();
      }
   }

   private Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("theme.json");
   }

   @Environment(EnvType.CLIENT)
   public record Preset(String name, int accent, int panel, int card) {
   }
}
