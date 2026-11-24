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
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class HudTheme {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final HudTheme INSTANCE = new HudTheme();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 1;
   private static final String THEME_KEY = "efnHudTheme";
   private static final int DEFAULT_COLOR = -15329251;
   private static final float DEFAULT_OPACITY = 0.85F;
   private static final float DEFAULT_IMAGE_OPACITY = 0.4F;
   private static final float ROWS_OPACITY = 0.6F;
   public final NumberSetting rounding = new NumberSetting("Corner radius", 8.0F, 0.0F, 16.0F).step(1.0F);
   public final ModeSetting font = new ModeSetting("Font", UiFont.GEIST.label, UiFont.labels());
   public final Surface infoBar = surface("hud_info_bar", "Info Bar");
   public final Surface coordinates = surface("hud_coordinates", "Coordinates", 0.6F);
   public final Surface moduleList = surface("hud_module_list", "Module List", 0.6F);
   public final Surface potions = surface("hud_potions", "Potions", 0.6F);
   public final Surface regionMap = surface("hud_region_map", "Region Map");
   public final Surface antiVanish = surface("hud_anti_vanish", "Anti Vanish", 0.6F);
   public final Surface staffList = surface("hud_staff_list", "Staff List", 0.6F);
   public final Surface spotify = surface("hud_spotify", "Spotify");
   public final Surface hotkeys = surface("hud_hotkeys", "Hotkeys", 0.6F);
   public final Surface radar = surface("hud_radar", "Radar", 0.6F);
   public final Surface toasts = surface("hud_toasts", "Toggle Toasts");
   public final Surface targetCard = surface("hud_target_card", "Target HUD");
   public final Surface armorStatus = surface("hud_armor_status", "Armor HUD", 0.6F);
   public final Surface itemCooldowns = surface("hud_item_cooldowns", "Cooldown", 0.6F);
   private final List<Surface> surfaces = List.of(
      this.infoBar,
      this.coordinates,
      this.moduleList,
      this.potions,
      this.regionMap,
      this.antiVanish,
      this.staffList,
      this.spotify,
      this.hotkeys,
      this.radar,
      this.toasts,
      this.targetCard,
      this.armorStatus,
      this.itemCooldowns
   );
   private final List<Setting> settings;
   private final JsonObject defaults;
   private boolean loaded;
   private boolean readOnly;

   private HudTheme() {
      ArrayList var1 = new ArrayList<>(List.of(this.rounding, this.font));

      for (Surface var3 : this.surfaces) {
         var1.addAll(var3.settings());
      }

      this.settings = List.copyOf(var1);
      this.defaults = this.snapshot(true);
   }

   private static Surface surface(String string, String string2) {
      return surface(string, string2, 0.85F);
   }

   private static Surface surface(String string, String string2, float f) {
      return new Surface(string, string2, false, -15329251, f, 0.4F);
   }

   public static HudTheme get() {
      INSTANCE.ensureLoaded();
      return INSTANCE;
   }

   public List<Surface> surfaces() {
      return this.surfaces;
   }

   public Surface forElementId(String string) {
      if ("info_bar".equals(string)) {
         return this.infoBar;
      } else if ("coordinates".equals(string)) {
         return this.coordinates;
      } else if ("module_list".equals(string)) {
         return this.moduleList;
      } else if ("potion_tracker".equals(string)) {
         return this.potions;
      } else if ("region_map".equals(string)) {
         return this.regionMap;
      } else if ("anti_vanish".equals(string)) {
         return this.antiVanish;
      } else if ("staff_list".equals(string)) {
         return this.staffList;
      } else if ("spotify".equals(string)) {
         return this.spotify;
      } else if ("hotkeys".equals(string)) {
         return this.hotkeys;
      } else if ("radar".equals(string)) {
         return this.radar;
      } else if ("toggle_toasts".equals(string)) {
         return this.toasts;
      } else if ("target_card".equals(string)) {
         return this.targetCard;
      } else if ("armor_status".equals(string)) {
         return this.armorStatus;
      } else {
         return "item_cooldowns".equals(string) ? this.itemCooldowns : null;
      }
   }

   public void resetToDefaults() {
      this.apply(this.defaults, true);
   }

   public String exportString() {
      JsonObject var1 = new JsonObject();
      var1.add("efnHudTheme", this.snapshot(false));
      return var1.toString();
   }

   public boolean importString(String string) {
      if (string != null && !string.isBlank()) {
         try {
            JsonElement var2 = JsonParser.parseString(string.trim());
            if (var2.isJsonObject() && var2.getAsJsonObject().has("efnHudTheme")) {
               JsonElement var3 = var2.getAsJsonObject().get("efnHudTheme");
               if (!var3.isJsonObject()) {
                  return false;
               } else {
                  this.apply(var3.getAsJsonObject(), false);
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
      return this.snapshot(true);
   }

   public void applyProfile(JsonObject jsonObject) {
      for (Setting var3 : this.settings) {
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

      for (Setting var4 : this.settings) {
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
                  if (var3 <= 1) {
                     if (var4.has("values") && var4.get("values").isJsonObject()) {
                        this.apply(var4.getAsJsonObject("values"), true);
                     }

                     return;
                  }

                  this.readOnly = true;
                  LOGGER.warn("[EFN] HUD theme version {} is newer than supported {}; skipping load", var3, 1);
               }
            } catch (RuntimeException | IOException var8) {
               LOGGER.warn("[EFN] Failed to load HUD theme; preserving the file", (Throwable)var8);
               this.readOnly = true;
            }
         }
      }
   }

   public synchronized void save() {
      if (!this.readOnly && this.loaded) {
         JsonObject var1 = new JsonObject();
         var1.addProperty("version", 1);
         var1.add("values", this.snapshot(true));
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
            LOGGER.warn("[EFN] Failed to save HUD theme", (Throwable)var10);
         }
      }
   }

   private JsonObject snapshot(boolean bl) {
      JsonObject var2 = new JsonObject();

      for (Setting var4 : this.settings) {
         if (bl || !var4.isPrivate()) {
            var2.add(var4.id(), var4.serialize());
         }
      }

      return var2;
   }

   private void apply(JsonObject jsonObject, boolean bl) {
      for (Setting var4 : this.settings) {
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
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("hud_theme.json");
   }
}
