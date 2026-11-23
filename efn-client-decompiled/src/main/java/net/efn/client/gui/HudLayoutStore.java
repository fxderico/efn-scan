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

   synchronized HudLayoutStore.HudElementState element(String var1) {
      this.loadIfNeeded();
      HudLayoutStore.HudElementState var2 = this.elements.get(var1);
      if (var2 == null) {
         var2 = defaultElement(var1);
         this.elements.put(var1, var2);
      }

      return var2;
   }

   synchronized List<HudLayoutStore.HudElementState> orderedElements() {
      this.loadIfNeeded();
      return List.copyOf(this.elements.values());
   }

   synchronized void updateBounds(String var1, float var2, float var3, float var4, float var5) {
      HudLayoutStore.HudElementState var6 = this.element(var1);
      float var7 = Math.max(1.0F, var2);
      float var8 = Math.max(1.0F, var3);
      if (var6.positioned && var6.autoPlaced && var4 > 0.0F && var5 > 0.0F && dockMoved(var6, var4, var5)) {
         var6.positioned = false;
      }

      if (var6.positioned && !(var4 <= 0.0F) && !(var5 <= 0.0F)) {
         if (!var6.anchored) {
            this.adoptLoadedPosition(var6, var4, var5);
         }

         rebaseToScreen(var6, var4, var5);
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
         clampToScreen(var6, var4, var5);
      } else {
         var6.width = var7;
         var6.height = var8;
         if (!var6.positioned && var4 > 0.0F && var5 > 0.0F && !isCollapsed(var6)) {
            this.placeDefault(var6, var4, var5);
         }
      }
   }

   private void adoptLoadedPosition(HudLayoutStore.HudElementState var1, float var2, float var3) {
      if (var1.refScreenW > 0.0F && var1.refScreenH > 0.0F) {
         var1.anchored = true;
      } else {
         if ("info_bar".equals(var1.id)) {
            var1.x = (var2 - var1.width) * 0.5F;
         } else if (var1.x > var2 - var1.width || var1.y > var3 - var1.height) {
            this.placeDefault(var1, var2, var3);
            return;
         }

         computeAnchor(var1, var2, var3);
      }
   }

   private static void rebaseToScreen(HudLayoutStore.HudElementState var0, float var1, float var2) {
      float var3 = var1 - var0.refScreenW;
      float var4 = var2 - var0.refScreenH;
      if (var3 != 0.0F || var4 != 0.0F) {
         if (var0.anchorX == 2) {
            var0.x += var3;
         } else if (var0.anchorX == 1) {
            var0.x += var3 * 0.5F;
         }

         if (var0.anchorBottom) {
            var0.y += var4;
         }

         var0.refScreenW = var1;
         var0.refScreenH = var2;
      }
   }

   synchronized void moveTo(String var1, float var2, float var3, float var4, float var5) {
      HudLayoutStore.HudElementState var6 = this.element(var1);
      var6.x = var2;
      var6.y = var3;
      var6.positioned = true;
      var6.autoPlaced = false;
      clampToScreen(var6, var4, var5);
      computeAnchor(var6, var4, var5);
   }

   private static void computeAnchor(HudLayoutStore.HudElementState var0, float var1, float var2) {
      float var3 = var0.x + var0.width * 0.5F;
      var0.anchorX = Math.abs(var3 - var1 * 0.5F) <= 24.0F * uiScale() ? 1 : (var3 > var1 * 0.5F ? 2 : 0);
      var0.anchorBottom = var0.y + var0.height * 0.5F > var2 * 0.5F;
      var0.refScreenW = var1;
      var0.refScreenH = var2;
      var0.anchored = true;
   }

   private static float uiScale() {
      return ImGuiManager.getInstance().uiScale();
   }

   synchronized void resetToDefaults(float var1, float var2) {
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
            this.placeDefault(var13, var1, var2);
            clampToScreen(var13, var1, var2);
         }
      }

      this.save();
   }

   public synchronized JsonObject profileSnapshot() {
      this.loadIfNeeded();
      return GSON.toJsonTree(this.buildConfigFile()).getAsJsonObject();
   }

   public synchronized boolean applyProfile(JsonObject var1) {
      this.loadIfNeeded();
      HudLayoutStore.ConfigFile var2 = parse(var1);
      if (var2 == null) {
         return false;
      } else {
         this.applyConfig(var2);
         this.save();
         return true;
      }
   }

   public static JsonObject shareable(JsonObject var0) {
      HudLayoutStore.ConfigFile var1 = parse(var0);
      return var1 == null ? null : GSON.toJsonTree(var1).getAsJsonObject();
   }

   private static HudLayoutStore.ConfigFile parse(JsonObject var0) {
      HudLayoutStore.ConfigFile var1;
      try {
         var1 = (HudLayoutStore.ConfigFile)GSON.fromJson(var0, HudLayoutStore.ConfigFile.class);
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

   private static boolean finite(HudLayoutStore.HudElementState var0) {
      return finite(var0.x)
         && finite(var0.y)
         && finite(var0.width)
         && finite(var0.height)
         && finite(var0.scale)
         && finite(var0.refScreenW)
         && finite(var0.refScreenH)
         && finite(var0.placedGuiScale)
         && finite(var0.placedUiScale);
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
         EfnClientMod.LOGGER.warn("[EFN] Failed to save HUD layout config", var10);
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
                  HudLayoutStore.ConfigFile var3 = (HudLayoutStore.ConfigFile)GSON.fromJson(var2, HudLayoutStore.ConfigFile.class);
                  if (var3 != null && var3.elements != null) {
                     this.applyConfig(var3);
                     return;
                  }
               }
            } catch (RuntimeException | IOException var7) {
               EfnClientMod.LOGGER.warn("[EFN] Failed to load HUD layout config; using defaults", var7);
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

   private void applyConfig(HudLayoutStore.ConfigFile var1) {
      this.elements.clear();
      this.installDefaults();
      boolean var2 = var1.version < 11;

      for (HudLayoutStore.HudElementState var4 : var1.elements) {
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

   private static HudLayoutStore.HudElementState defaultElement(String var0) {
      HudLayoutStore.HudElementState var1 = new HudLayoutStore.HudElementState();
      var1.id = var0;
      var1.x = 0.0F;
      var1.y = 0.0F;
      var1.width = fallbackWidth(var0);
      var1.height = 26.0F;
      var1.visible = true;
      var1.scale = 1.0F;
      var1.positioned = false;
      return var1;
   }

   private static void normalize(HudLayoutStore.HudElementState var0) {
      var0.scale = finite(var0.scale) ? clamp(var0.scale, 0.5F, 3.0F) : 1.0F;
      var0.width = finite(var0.width) && var0.width > 0.0F ? var0.width : 1.0F;
      var0.height = finite(var0.height) && var0.height > 0.0F ? var0.height : 1.0F;
      boolean var1 = var0.positioned = finite(var0.x) && finite(var0.y);
      if (var0.anchorX < 0 || var0.anchorX > 2 || !finite(var0.refScreenW) || !finite(var0.refScreenH)) {
         var0.refScreenW = 0.0F;
         var0.refScreenH = 0.0F;
      }
   }

   private void placeDefault(HudLayoutStore.HudElementState var1, float var2, float var3) {
      float var4 = Math.round(10.0F * uiScale());
      float var5 = Math.round(6.0F * uiScale());
      float var6 = var4 + this.existingHeight("coordinates", 28.0F) + var5;
      String var7 = var1.id;
      switch (var7) {
         case "potion_tracker":
            var1.x = Math.max(var4, var2 - var4 - Math.max(1.0F, var1.width));
            var1.y = Math.max(var4, var3 - var4 - Math.max(1.0F, var1.height));
            break;
         case "info_bar":
            var1.x = Math.max(var4, (var2 - var1.width) * 0.5F);
            var1.y = var4;
            break;
         case "module_list":
            var1.x = Math.max(var4, var2 - var4 - Math.max(1.0F, var1.width));
            var1.y = var4;
            break;
         case "region_map":
            var1.x = var4;
            var1.y = this.firstFreeY(var1, var6, var5);
            break;
         case "anti_vanish":
            var1.x = Math.max(var4, (var2 - var1.width) * 0.5F);
            var1.y = var4 + this.existingHeight("info_bar", 28.0F) + var5;
            break;
         case "spotify":
            var1.x = var4;
            var1.y = this.firstFreeY(var1, var6, var5);
            break;
         case "staff_list":
            var1.x = var4;
            var1.y = this.firstFreeY(var1, Math.round(var3 * 0.38F), var5);
            break;
         case "hotkeys":
            var1.x = Math.max(var4, var2 - var4 - Math.max(1.0F, var1.width));
            var1.y = this.firstFreeY(var1, Math.round(var3 * 0.36F), var5);
            break;
         case "radar":
            var1.x = Math.max(var4, var2 - var4 - Math.max(1.0F, var1.width));
            var1.y = this.firstFreeY(var1, Math.round(var3 * 0.58F), var5);
            break;
         case "target_card":
            var1.x = Math.round(var2 * 0.5F + 44.0F * uiScale());
            var1.y = this.firstFreeY(var1, Math.round(var3 * 0.5F + 30.0F * uiScale()), var5);
            break;
         case "armor_status":
            var1.x = Math.round(var2 * 0.5F + 119.0F * vanillaGuiScale());
            var1.y = Math.max(var4, var3 - var4 - Math.max(1.0F, var1.height));
            break;
         case "item_cooldowns":
            var1.x = Math.max(var4, (float)Math.round((var2 - var1.width) * 0.5F));
            var1.y = Math.max(var4, var3 - Math.round(76.0F * vanillaGuiScale()) - Math.max(1.0F, var1.height));
            break;
         default:
            var1.x = var4;
            var1.y = var4;
      }

      var1.positioned = true;
      clampToScreen(var1, var2, var3);
      computeAnchor(var1, var2, var3);
      var1.autoPlaced = followsVanillaHud(var1.id);
      if (var1.autoPlaced) {
         var1.anchorX = "item_cooldowns".equals(var1.id) ? 1 : 0;
         var1.placedGuiScale = vanillaGuiScale();
         var1.placedUiScale = uiScale();
      }
   }

   private static boolean followsVanillaHud(String var0) {
      return "target_card".equals(var0) || "armor_status".equals(var0) || "item_cooldowns".equals(var0);
   }

   private static boolean dockMoved(HudLayoutStore.HudElementState var0, float var1, float var2) {
      return var1 != var0.refScreenW || var2 != var0.refScreenH || vanillaGuiScale() != var0.placedGuiScale || uiScale() != var0.placedUiScale;
   }

   private static float fallbackWidth(String var0) {
      return switch (var0) {
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
      return Math.max(1, MinecraftClient.method_1551().method_22683().method_4495());
   }

   private float firstFreeY(HudLayoutStore.HudElementState var1, float var2, float var3) {
      float var4 = var2;
      float var5 = Math.max(1.0F, var1.width);
      float var6 = Math.max(1.0F, var1.height);

      for (int var7 = 0; var7 < this.elements.size(); var7++) {
         boolean var8 = false;

         for (HudLayoutStore.HudElementState var10 : this.elements.values()) {
            if (var10 != var1
               && var10.positioned
               && !isCollapsed(var10)
               && var1.x < var10.x + var10.width
               && var10.x < var1.x + var5
               && var4 < var10.y + var10.height
               && var10.y < var4 + var6) {
               var4 = var10.y + var10.height + var3;
               var8 = true;
            }
         }

         if (!var8) {
            break;
         }
      }

      return var4;
   }

   private static boolean isCollapsed(HudLayoutStore.HudElementState var0) {
      return var0.width <= 1.0F;
   }

   private static void clampToScreen(HudLayoutStore.HudElementState var0, float var1, float var2) {
      float var3 = Math.max(0.0F, var1 - Math.max(1.0F, var0.width));
      float var4 = Math.max(0.0F, var2 - Math.max(1.0F, var0.height));
      var0.x = clamp(var0.x, 0.0F, var3);
      var0.y = clamp(var0.y, 0.0F, var4);
   }

   private float existingWidth(String var1, float var2) {
      HudLayoutStore.HudElementState var3 = this.elements.get(var1);
      return var3 != null && finite(var3.width) && var3.width > 0.0F ? var3.width : var2;
   }

   private float existingHeight(String var1, float var2) {
      HudLayoutStore.HudElementState var3 = this.elements.get(var1);
      return var3 != null && finite(var3.height) && var3.height > 0.0F ? var3.height : var2;
   }

   private static Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("hud.json");
   }

   private static boolean isKnownId(String var0) {
      for (String var4 : KNOWN_IDS) {
         if (var4.equals(var0)) {
            return true;
         }
      }

      return false;
   }

   private static boolean finite(float var0) {
      return !Float.isNaN(var0) && !Float.isInfinite(var0);
   }

   private static float clamp(float var0, float var1, float var2) {
      return Math.max(var1, Math.min(var2, var0));
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
