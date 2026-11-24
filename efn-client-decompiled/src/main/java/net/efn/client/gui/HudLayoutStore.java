package net.efn.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.efn.client.EfnClientMod;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class HudLayoutStore {
   public static final String INFO_BAR_ID = "info_bar";
   public static final String COORDINATES_ID = "coordinates";
   public static final String POTION_TRACKER_ID = "potion_tracker";
   public static final String MODULE_LIST_ID = "module_list";
   public static final String REGION_MAP_ID = "region_map";
   public static final String ANTI_VANISH_ID = "anti_vanish";
   public static final String STAFF_LIST_ID = "staff_list";
   public static final String SPOTIFY_ID = "spotify";
   public static final String RADAR_ID = "radar";
   public static final String HOTKEYS_ID = "hotkeys";
   public static final String TARGET_CARD_ID = "target_card";
   public static final String ARMOR_STATUS_ID = "armor_status";
   public static final String ITEM_COOLDOWNS_ID = "item_cooldowns";
   private static final HudLayoutStore INSTANCE = new HudLayoutStore();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 11;
   private static final float DEFAULT_MARGIN = 10.0F;
   private static final float DEFAULT_GAP = 6.0F;
   private static final float CENTER_TOLERANCE = 24.0F;
   static final int ANCHOR_LEFT = 0;
   static final int ANCHOR_CENTER = 1;
   static final int ANCHOR_RIGHT = 2;
   private static final String[] KNOWN_IDS = new String[]{
      "info_bar",
      "coordinates",
      "module_list",
      "potion_tracker",
      "region_map",
      "anti_vanish",
      "staff_list",
      "spotify",
      "hotkeys",
      "radar",
      "target_card",
      "armor_status",
      "item_cooldowns"
   };
   private final Map<String, HudLayoutStore.HudElementState> elements = new LinkedHashMap<>();
   private boolean loaded;

   private HudLayoutStore() {
   }

   public static HudLayoutStore getInstance() {
      return INSTANCE;
   }

   synchronized HudLayoutStore.HudElementState element(String string) {
      this.loadIfNeeded();
      HudLayoutStore.HudElementState var2 = this.elements.get(string);
      if (var2 == null) {
         var2 = defaultElement(string);
         this.elements.put(string, var2);
      }

      return var2;
   }

   synchronized List<HudLayoutStore.HudElementState> orderedElements() {
      this.loadIfNeeded();
      return List.copyOf(this.elements.values());
   }

   synchronized void updateBounds(String string, float f, float g, float h, float i) {
      HudLayoutStore.HudElementState var6 = this.element(string);
      float var7 = Math.max(1.0F, f);
      float var8 = Math.max(1.0F, g);
      if (var6.positioned && var6.autoPlaced && h > 0.0F && i > 0.0F && dockMoved(var6, h, i)) {
         var6.positioned = false;
      }

      if (var6.positioned && !(h <= 0.0F) && !(i <= 0.0F)) {
         if (!var6.anchored) {
            this.adoptLoadedPosition(var6, h, i);
         }

         rebaseToScreen(var6, h, i);
         float var9 = var6.width - var7;
         if (var6.anchorX == 2) {
            var6.x += var9;
         } else if (var6.anchorX == 1) {
            var6.x += var9 * 0.5F;
         }

         if (var6.anchorBottom) {
            var6.y = var6.y + (var6.height - var8);
         }

         var6.width = var7;
         var6.height = var8;
         clampToScreen(var6, h, i);
      } else {
         var6.width = var7;
         var6.height = var8;
         if (!var6.positioned && h > 0.0F && i > 0.0F && !isCollapsed(var6)) {
            this.placeDefault(var6, h, i);
         }
      }
   }

   private void adoptLoadedPosition(HudLayoutStore.HudElementState hudElementState, float f, float g) {
      if (hudElementState.refScreenW > 0.0F && hudElementState.refScreenH > 0.0F) {
         hudElementState.anchored = true;
      } else {
         if ("info_bar".equals(hudElementState.id)) {
            hudElementState.x = (f - hudElementState.width) * 0.5F;
         } else if (hudElementState.x > f - hudElementState.width || hudElementState.y > g - hudElementState.height) {
            this.placeDefault(hudElementState, f, g);
            return;
         }

         computeAnchor(hudElementState, f, g);
      }
   }

   private static void rebaseToScreen(HudLayoutStore.HudElementState hudElementState, float f, float g) {
      float var3 = f - hudElementState.refScreenW;
      float var4 = g - hudElementState.refScreenH;
      if (var3 != 0.0F || var4 != 0.0F) {
         if (hudElementState.anchorX == 2) {
            hudElementState.x += var3;
         } else if (hudElementState.anchorX == 1) {
            hudElementState.x += var3 * 0.5F;
         }

         if (hudElementState.anchorBottom) {
            hudElementState.y += var4;
         }

         hudElementState.refScreenW = f;
         hudElementState.refScreenH = g;
      }
   }

   synchronized void moveTo(String string, float f, float g, float h, float i) {
      HudLayoutStore.HudElementState var6 = this.element(string);
      var6.x = f;
      var6.y = g;
      var6.positioned = true;
      var6.autoPlaced = false;
      clampToScreen(var6, h, i);
      computeAnchor(var6, h, i);
   }

   private static void computeAnchor(HudLayoutStore.HudElementState hudElementState, float f, float g) {
      float var3 = hudElementState.x + hudElementState.width * 0.5F;
      hudElementState.anchorX = Math.abs(var3 - f * 0.5F) <= 24.0F * uiScale() ? 1 : (var3 > f * 0.5F ? 2 : 0);
      hudElementState.anchorBottom = hudElementState.y + hudElementState.height * 0.5F > g * 0.5F;
      hudElementState.refScreenW = f;
      hudElementState.refScreenH = g;
      hudElementState.anchored = true;
   }

   private static float uiScale() {
      return ImGuiManager.getInstance().uiScale();
   }

   synchronized void resetToDefaults(float f, float g) {
      LinkedHashMap var3 = new LinkedHashMap();

      for (String var7 : KNOWN_IDS) {
         var3.put(var7, new HudLayoutStore.Dimensions(this.existingWidth(var7, fallbackWidth(var7)), this.existingHeight(var7, 26.0F)));
      }

      this.elements.clear();

      for (String var15 : KNOWN_IDS) {
         HudLayoutStore.HudElementState var8 = defaultElement(var15);
         HudLayoutStore.Dimensions var9 = (HudLayoutStore.Dimensions)var3.get(var15);
         var8.width = var9.width();
         var8.height = var9.height();
         this.elements.put(var8.id, var8);
      }

      for (HudLayoutStore.HudElementState var13 : this.elements.values()) {
         if (!isCollapsed(var13)) {
            this.placeDefault(var13, f, g);
            clampToScreen(var13, f, g);
         }
      }

      this.save();
   }

   public synchronized JsonObject profileSnapshot() {
      this.loadIfNeeded();
      return GSON.toJsonTree(this.buildConfigFile()).getAsJsonObject();
   }

   public synchronized boolean applyProfile(JsonObject jsonObject) {
      this.loadIfNeeded();
      HudLayoutStore.ConfigFile var2 = parse(jsonObject);
      if (var2 == null) {
         return false;
      } else {
         this.applyConfig(var2);
         this.save();
         return true;
      }
   }

   public static JsonObject shareable(JsonObject jsonObject) {
      HudLayoutStore.ConfigFile var1 = parse(jsonObject);
      return var1 == null ? null : GSON.toJsonTree(var1).getAsJsonObject();
   }

   private static HudLayoutStore.ConfigFile parse(JsonObject jsonObject) {
      HudLayoutStore.ConfigFile var1;
      try {
         var1 = GSON.fromJson(jsonObject, HudLayoutStore.ConfigFile.class);
      } catch (RuntimeException var5) {
         return null;
      }

      if (var1 != null && var1.elements != null) {
         ArrayList var2 = new ArrayList();

         for (HudLayoutStore.HudElementState var4 : var1.elements) {
            if (var4 != null && var4.id != null && isKnownId(var4.id) && finite(var4)) {
               var2.add(var4);
            }
         }

         var1.elements = var2;
         return var1;
      } else {
         return null;
      }
   }

   private static boolean finite(HudLayoutStore.HudElementState hudElementState) {
      return finite(hudElementState.x)
         && finite(hudElementState.y)
         && finite(hudElementState.width)
         && finite(hudElementState.height)
         && finite(hudElementState.scale)
         && finite(hudElementState.refScreenW)
         && finite(hudElementState.refScreenH)
         && finite(hudElementState.placedGuiScale)
         && finite(hudElementState.placedUiScale);
   }

   public synchronized void save() {
      this.loadIfNeeded();
      HudLayoutStore.ConfigFile var1 = this.buildConfigFile();
      Path var2 = configPath();

      try {
         Files.createDirectories(var2.getParent());
         Path var3 = var2.resolveSibling(var2.getFileName().toString() + ".tmp");

         try (BufferedWriter var4 = Files.newBufferedWriter(var3)) {
            GSON.toJson(var1, var4);
         }

         try {
            Files.move(var3, var2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException var8) {
            Files.move(var3, var2, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException var10) {
         EfnClientMod.LOGGER.warn("[EFN] Failed to save HUD layout config", (Throwable)var10);
      }
   }

   private void loadIfNeeded() {
      if (!this.loaded) {
         this.loaded = true;
         this.installDefaults();
         Path var1 = configPath();
         if (Files.isRegularFile(var1)) {
            try {
               try (BufferedReader var2 = Files.newBufferedReader(var1)) {
                  HudLayoutStore.ConfigFile var3 = GSON.fromJson(var2, HudLayoutStore.ConfigFile.class);
                  if (var3 != null && var3.elements != null) {
                     this.applyConfig(var3);
                     return;
                  }
               }
            } catch (RuntimeException | IOException var7) {
               EfnClientMod.LOGGER.warn("[EFN] Failed to load HUD layout config; using defaults", (Throwable)var7);
               this.elements.clear();
               this.installDefaults();
            }
         }
      }
   }

   private HudLayoutStore.ConfigFile buildConfigFile() {
      HudLayoutStore.ConfigFile var1 = new HudLayoutStore.ConfigFile();
      var1.version = 11;
      var1.elements = new ArrayList<>();

      for (HudLayoutStore.HudElementState var3 : this.elements.values()) {
         if (var3.positioned) {
            var1.elements.add(var3);
         }
      }

      return var1;
   }

   private void applyConfig(HudLayoutStore.ConfigFile configFile) {
      this.elements.clear();
      this.installDefaults();
      boolean var2 = configFile.version < 11;

      for (HudLayoutStore.HudElementState var4 : configFile.elements) {
         if (var4 != null && var4.id != null && !var4.id.isBlank() && isKnownId(var4.id)) {
            normalize(var4);
            this.elements.put(var4.id, var4);
         }
      }

      this.ensureKnownElements();
      if (var2) {
         this.resetPositionsForMigration();
      }
   }

   private void installDefaults() {
      for (String var4 : KNOWN_IDS) {
         this.elements.put(var4, defaultElement(var4));
      }
   }

   private void ensureKnownElements() {
      for (String var4 : KNOWN_IDS) {
         this.elements.computeIfAbsent(var4, HudLayoutStore::defaultElement);
      }
   }

   private void resetPositionsForMigration() {
      for (String var4 : KNOWN_IDS) {
         HudLayoutStore.HudElementState var5 = this.elements.get(var4);
         if (var5 != null) {
            var5.positioned = false;
         }
      }
   }

   private static HudLayoutStore.HudElementState defaultElement(String string) {
      HudLayoutStore.HudElementState var1 = new HudLayoutStore.HudElementState();
      var1.id = string;
      var1.x = 0.0F;
      var1.y = 0.0F;
      var1.width = fallbackWidth(string);
      var1.height = 26.0F;
      var1.visible = true;
      var1.scale = 1.0F;
      var1.positioned = false;
      return var1;
   }

   private static void normalize(HudLayoutStore.HudElementState hudElementState) {
      hudElementState.scale = finite(hudElementState.scale) ? clamp(hudElementState.scale, 0.5F, 3.0F) : 1.0F;
      hudElementState.width = finite(hudElementState.width) && hudElementState.width > 0.0F ? hudElementState.width : 1.0F;
      hudElementState.height = finite(hudElementState.height) && hudElementState.height > 0.0F ? hudElementState.height : 1.0F;
      boolean var1 = hudElementState.positioned = finite(hudElementState.x) && finite(hudElementState.y);
      if (hudElementState.anchorX < 0 || hudElementState.anchorX > 2 || !finite(hudElementState.refScreenW) || !finite(hudElementState.refScreenH)) {
         hudElementState.refScreenW = 0.0F;
         hudElementState.refScreenH = 0.0F;
      }
   }

   private void placeDefault(HudLayoutStore.HudElementState hudElementState, float f, float g) {
      float var4 = Math.round(10.0F * uiScale());
      float var5 = Math.round(6.0F * uiScale());
      float var6 = var4 + this.existingHeight("coordinates", 28.0F) + var5;
      String var7 = hudElementState.id;
      switch (var7) {
         case "potion_tracker":
            hudElementState.x = Math.max(var4, f - var4 - Math.max(1.0F, hudElementState.width));
            hudElementState.y = Math.max(var4, g - var4 - Math.max(1.0F, hudElementState.height));
            break;
         case "info_bar":
            hudElementState.x = Math.max(var4, (f - hudElementState.width) * 0.5F);
            hudElementState.y = var4;
            break;
         case "module_list":
            hudElementState.x = Math.max(var4, f - var4 - Math.max(1.0F, hudElementState.width));
            hudElementState.y = var4;
            break;
         case "region_map":
            hudElementState.x = var4;
            hudElementState.y = this.firstFreeY(hudElementState, var6, var5);
            break;
         case "anti_vanish":
            hudElementState.x = Math.max(var4, (f - hudElementState.width) * 0.5F);
            hudElementState.y = var4 + this.existingHeight("info_bar", 28.0F) + var5;
            break;
         case "spotify":
            hudElementState.x = var4;
            hudElementState.y = this.firstFreeY(hudElementState, var6, var5);
            break;
         case "staff_list":
            hudElementState.x = var4;
            hudElementState.y = this.firstFreeY(hudElementState, Math.round(g * 0.38F), var5);
            break;
         case "hotkeys":
            hudElementState.x = Math.max(var4, f - var4 - Math.max(1.0F, hudElementState.width));
            hudElementState.y = this.firstFreeY(hudElementState, Math.round(g * 0.36F), var5);
            break;
         case "radar":
            hudElementState.x = Math.max(var4, f - var4 - Math.max(1.0F, hudElementState.width));
            hudElementState.y = this.firstFreeY(hudElementState, Math.round(g * 0.58F), var5);
            break;
         case "target_card":
            hudElementState.x = Math.round(f * 0.5F + 44.0F * uiScale());
            hudElementState.y = this.firstFreeY(hudElementState, Math.round(g * 0.5F + 30.0F * uiScale()), var5);
            break;
         case "armor_status":
            hudElementState.x = Math.round(f * 0.5F + 119.0F * vanillaGuiScale());
            hudElementState.y = Math.max(var4, g - var4 - Math.max(1.0F, hudElementState.height));
            break;
         case "item_cooldowns":
            hudElementState.x = Math.max(var4, (float)Math.round((f - hudElementState.width) * 0.5F));
            hudElementState.y = Math.max(var4, g - Math.round(76.0F * vanillaGuiScale()) - Math.max(1.0F, hudElementState.height));
            break;
         default:
            hudElementState.x = var4;
            hudElementState.y = var4;
      }

      hudElementState.positioned = true;
      clampToScreen(hudElementState, f, g);
      computeAnchor(hudElementState, f, g);
      hudElementState.autoPlaced = followsVanillaHud(hudElementState.id);
      if (hudElementState.autoPlaced) {
         hudElementState.anchorX = "item_cooldowns".equals(hudElementState.id) ? 1 : 0;
         hudElementState.placedGuiScale = vanillaGuiScale();
         hudElementState.placedUiScale = uiScale();
      }
   }

   private static boolean followsVanillaHud(String string) {
      return "target_card".equals(string) || "armor_status".equals(string) || "item_cooldowns".equals(string);
   }

   private static boolean dockMoved(HudLayoutStore.HudElementState hudElementState, float f, float g) {
      return f != hudElementState.refScreenW
         || g != hudElementState.refScreenH
         || vanillaGuiScale() != hudElementState.placedGuiScale
         || uiScale() != hudElementState.placedUiScale;
   }

   private static float fallbackWidth(String string) {
      return switch (string) {
         case "module_list" -> 170.0F;
         case "info_bar" -> 290.0F;
         case "coordinates" -> 210.0F;
         case "potion_tracker" -> 190.0F;
         case "region_map" -> 240.0F;
         case "anti_vanish" -> 180.0F;
         case "staff_list" -> 190.0F;
         case "spotify" -> 320.0F;
         case "hotkeys" -> 190.0F;
         case "radar" -> 150.0F;
         case "target_card" -> 244.0F;
         case "armor_status" -> 120.0F;
         case "item_cooldowns" -> 80.0F;
         default -> 110.0F;
      };
   }

   private static float vanillaGuiScale() {
      return Math.max(1, MinecraftClient.getInstance().getWindow().getScaleFactor());
   }

   private float firstFreeY(HudLayoutStore.HudElementState hudElementState, float f, float g) {
      float var4 = f;
      float var5 = Math.max(1.0F, hudElementState.width);
      float var6 = Math.max(1.0F, hudElementState.height);

      for (int var7 = 0; var7 < this.elements.size(); var7++) {
         boolean var8 = false;

         for (HudLayoutStore.HudElementState var10 : this.elements.values()) {
            if (var10 != hudElementState
               && var10.positioned
               && !isCollapsed(var10)
               && hudElementState.x < var10.x + var10.width
               && var10.x < hudElementState.x + var5
               && var4 < var10.y + var10.height
               && var10.y < var4 + var6) {
               var4 = var10.y + var10.height + g;
               var8 = true;
            }
         }

         if (!var8) {
            break;
         }
      }

      return var4;
   }

   private static boolean isCollapsed(HudLayoutStore.HudElementState hudElementState) {
      return hudElementState.width <= 1.0F;
   }

   private static void clampToScreen(HudLayoutStore.HudElementState hudElementState, float f, float g) {
      float var3 = Math.max(0.0F, f - Math.max(1.0F, hudElementState.width));
      float var4 = Math.max(0.0F, g - Math.max(1.0F, hudElementState.height));
      hudElementState.x = clamp(hudElementState.x, 0.0F, var3);
      hudElementState.y = clamp(hudElementState.y, 0.0F, var4);
   }

   private float existingWidth(String string, float f) {
      HudLayoutStore.HudElementState var3 = this.elements.get(string);
      return var3 != null && finite(var3.width) && var3.width > 0.0F ? var3.width : f;
   }

   private float existingHeight(String string, float f) {
      HudLayoutStore.HudElementState var3 = this.elements.get(string);
      return var3 != null && finite(var3.height) && var3.height > 0.0F ? var3.height : f;
   }

   private static Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("hud.json");
   }

   private static boolean isKnownId(String string) {
      for (String var4 : KNOWN_IDS) {
         if (var4.equals(string)) {
            return true;
         }
      }

      return false;
   }

   private static boolean finite(float f) {
      return !Float.isNaN(f) && !Float.isInfinite(f);
   }

   private static float clamp(float f, float g, float h) {
      return Math.max(g, Math.min(h, f));
   }

   @Environment(EnvType.CLIENT)
   private static final class ConfigFile {
      int version;
      List<HudLayoutStore.HudElementState> elements;

      private ConfigFile() {
      }
   }

   @Environment(EnvType.CLIENT)
   private record Dimensions(float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   static final class HudElementState {
      String id;
      float x;
      float y;
      float width;
      float height;
      boolean visible;
      float scale;
      transient boolean positioned;
      int anchorX;
      boolean anchorBottom;
      float refScreenW;
      float refScreenH;
      transient boolean anchored;
      boolean autoPlaced;
      float placedGuiScale;
      float placedUiScale;

      HudElementState() {
      }
   }
}
