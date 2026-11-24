package net.efn.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.efn.client.config.BundledDefaults;
import net.efn.client.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class PanelStateStore {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final PanelStateStore INSTANCE = new PanelStateStore();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 1;
   private final Set<String> collapsed = new LinkedHashSet<>();
   private final Map<String, float[]> homes = new LinkedHashMap<>();
   private boolean loaded;
   private boolean readOnly;

   private PanelStateStore() {
   }

   public static PanelStateStore getInstance() {
      return INSTANCE;
   }

   public synchronized boolean isCollapsed(ModuleCategory moduleCategory) {
      this.ensureLoaded();
      return this.collapsed.contains(moduleCategory.name());
   }

   public synchronized void setCollapsed(ModuleCategory moduleCategory, boolean bl) {
      this.ensureLoaded();
      boolean var3 = bl ? this.collapsed.add(moduleCategory.name()) : this.collapsed.remove(moduleCategory.name());
      if (var3) {
         this.save();
      }
   }

   public synchronized Map<String, float[]> panelHomes() {
      this.ensureLoaded();
      HashMap var1 = new HashMap();
      this.homes.forEach((string, fs) -> var1.put(string, (float[])fs.clone()));
      return var1;
   }

   public synchronized void setPanelHomes(Map<String, float[]> map) {
      this.ensureLoaded();
      this.homes.clear();
      map.forEach((string, fs) -> this.homes.put(string, (float[])fs.clone()));
      this.save();
   }

   public synchronized void load() {
      if (!this.loaded) {
         this.loaded = true;
         this.collapsed.clear();
         this.homes.clear();
         Path var1 = this.configPath();
         if (!Files.isRegularFile(var1)) {
            this.collapsed.addAll(BundledDefaults.collapsedPanels());
         } else {
            try {
               try (BufferedReader var2 = Files.newBufferedReader(var1)) {
                  JsonObject var4 = JsonParser.parseReader(var2).getAsJsonObject();
                  int var3 = var4.has("version") ? var4.get("version").getAsInt() : 0;
                  if (var3 <= 1) {
                     if (var4.has("collapsed") && var4.get("collapsed").isJsonArray()) {
                        for (JsonElement var7 : var4.getAsJsonArray("collapsed")) {
                           if (var7.isJsonPrimitive()) {
                              this.collapsed.add(var7.getAsString());
                           }
                        }
                     }

                     if (var4.has("panels") && var4.get("panels").isJsonObject()) {
                        for (Entry var15 : var4.getAsJsonObject("panels").entrySet()) {
                           if (((JsonElement)var15.getValue()).isJsonArray() && ((JsonElement)var15.getValue()).getAsJsonArray().size() == 2) {
                              JsonArray var8 = ((JsonElement)var15.getValue()).getAsJsonArray();
                              float var9 = var8.get(0).getAsFloat();
                              float var10 = var8.get(1).getAsFloat();
                              if (Float.isFinite(var9) && Float.isFinite(var10)) {
                                 this.homes.put((String)var15.getKey(), new float[]{var9, var10});
                              }
                           }
                        }
                     }

                     return;
                  }

                  this.readOnly = true;
                  LOGGER.warn("[EFN] ClickGUI state version {} is newer than supported {}; skipping load", var3, 1);
               }
            } catch (RuntimeException | IOException var13) {
               LOGGER.warn("[EFN] Failed to load the ClickGUI state; preserving the file", (Throwable)var13);
               this.collapsed.clear();
               this.homes.clear();
               this.readOnly = true;
            }
         }
      }
   }

   public synchronized void save() {
      if (!this.readOnly) {
         JsonObject var1 = new JsonObject();
         var1.addProperty("version", 1);
         JsonArray var2 = new JsonArray();

         for (String var4 : this.collapsed) {
            var2.add(var4);
         }

         var1.add("collapsed", var2);
         JsonObject var13 = new JsonObject();
         this.homes.forEach((string, fs) -> {
            JsonArray var3 = new JsonArray();
            var3.add(fs[0]);
            var3.add(fs[1]);
            var13.add(string, var3);
         });
         var1.add("panels", var13);
         Path var14 = this.configPath();

         try {
            Files.createDirectories(var14.getParent());
            Path var5 = var14.resolveSibling(var14.getFileName().toString() + ".tmp");

            try (BufferedWriter var6 = Files.newBufferedWriter(var5)) {
               GSON.toJson((JsonElement)var1, var6);
            }

            try {
               Files.move(var5, var14, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException var10) {
               Files.move(var5, var14, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException var12) {
            LOGGER.warn("[EFN] Failed to save the ClickGUI state", (Throwable)var12);
         }
      }
   }

   private void ensureLoaded() {
      if (!this.loaded) {
         this.load();
      }
   }

   private Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("clickgui.json");
   }
}
