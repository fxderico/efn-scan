package net.efn.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import net.efn.client.event.impl.ModuleToggleEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.ModuleRevision;
import net.efn.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ModuleConfigStore {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final ModuleConfigStore INSTANCE = new ModuleConfigStore();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 3;
   private static final long AUTOSAVE_DELAY_NANOS = 2000000000L;
   private static final long SHUTDOWN_WRITE_WAIT_SECONDS = 10L;
   private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(var0 -> {
      Thread var1 = new Thread(var0, "EFN config writer");
      var1.setDaemon(true);
      return var1;
   });
   private static final Map<String, String> RENAMED_MODULES = Map.of("activity-bypass", "player-bypass", "Activity Bypass", "player-bypass");
   private final Map<String, JsonObject> orphanModules = new LinkedHashMap<>();
   private final Map<String, Boolean> pendingEnabled = new LinkedHashMap<>();
   private boolean loaded;
   private boolean readOnly;
   private final Map<String, JsonObject> retainedEntries = new LinkedHashMap<>();
   private String activeProfile;
   private volatile boolean writeFailed;
   private Future<?> lastWrite;
   private long savedRevision = -1L;
   private long observedRevision = -1L;
   private long observedAt;

   private ModuleConfigStore() {
   }

   public static ModuleConfigStore getInstance() {
      return INSTANCE;
   }

   public synchronized void load() {
      if (!this.loaded) {
         this.loaded = true;
         this.orphanModules.clear();
         this.pendingEnabled.clear();
         BundledDefaults.adopt(ModuleRegistry.all());
         this.readConfig();

         for (ClientModule var2 : ModuleRegistry.all()) {
            if (BundledDefaults.enabledByDefault(var2) && !this.retainedEntries.containsKey(var2.id())) {
               this.pendingEnabled.put(var2.id(), true);
            }
         }
      }
   }

   private void readConfig() {
      Path var1 = this.configPath();
      deleteQuietly(tmpPath(var1));
      if (Files.isRegularFile(var1)) {
         try {
            label87: {
               try (BufferedReader var2 = Files.newBufferedReader(var1)) {
                  JsonElement var5 = JsonParser.parseReader(var2);
                  if (var5.isJsonObject()) {
                     JsonObject var6 = var5.getAsJsonObject();
                     this.applyConfigRoot(var6);
                     if (var6.has("activeProfile") && var6.get("activeProfile").isJsonPrimitive()) {
                        this.activeProfile = var6.get("activeProfile").getAsString();
                     }

                     int var4 = var6.has("version") ? var6.get("version").getAsInt() : 1;
                     Path var3;
                     if (!this.readOnly && var4 < 3 && !Files.exists(var3 = backupPath(var1, var4))) {
                        writeJson(var3, withoutSettings(var6, Setting::isSecret));
                     }
                     break label87;
                  }

                  LOGGER.warn("[EFN] Module config isn't a JSON object; preserving the file");
                  this.readOnly = true;
               }

               return;
            }
         } catch (RuntimeException | IOException var10) {
            LOGGER.warn("[EFN] Failed to load module config; preserving the file", var10);
            this.orphanModules.clear();
            this.pendingEnabled.clear();
            this.readOnly = true;
         }

         if (!this.readOnly) {
            scrubBackups(var1);
         }
      }
   }

   private static Path backupPath(Path var0, int var1) {
      return var0.resolveSibling(var0.getFileName() + ".v" + var1 + ".bak");
   }

   private static void scrubBackups(Path var0) {
      ArrayList var1 = new ArrayList();

      for (ClientModule var3 : ModuleRegistry.all()) {
         for (Setting var5 : var3.settings()) {
            if (var5.isSecret()) {
               var1.add("\"" + var5.id() + "\"");
               var1.add("\"" + var5.name() + "\"");
            }
         }
      }

      for (int var11 = 0; var11 < 3; var11++) {
         Path var12 = backupPath(var0, var11);
         if (Files.isRegularFile(var12)) {
            try {
               String var6 = Files.readString(var12);
               boolean var7 = false;

               for (String var9 : var1) {
                  var7 |= var6.contains(var9);
               }

               JsonObject var13;
               JsonElement var14;
               if (var7
                  && (var14 = JsonParser.parseString(var6)).isJsonObject()
                  && !(var13 = withoutSettings(var14.getAsJsonObject(), Setting::isSecret)).equals(var14)) {
                  writeJson(var12, var13);
               }
            } catch (RuntimeException | IOException var10) {
               LOGGER.warn("[EFN] Couldn't clean {}", var12.getFileName(), var10);
            }
         }
      }
   }

   public static JsonObject withoutPrivateValues(JsonObject var0) {
      return withoutSettings(var0, Setting::isPrivate);
   }

   private static JsonObject withoutSettings(JsonObject var0, Predicate<Setting> var1) {
      JsonObject var2 = var0.deepCopy();
      if (!(var2.get("modules") instanceof JsonObject var4)) {
         return var2;
      } else {
         HashMap var5 = new HashMap();

         for (ClientModule var7 : ModuleRegistry.all()) {
            var5.putIfAbsent(var7.name(), var7);
         }

         for (ClientModule var16 : ModuleRegistry.all()) {
            var5.put(var16.id(), var16);
         }

         for (String var17 : List.copyOf(var4.keySet())) {
            if (var4.get(var17) instanceof JsonObject var11) {
               String var12 = renamedModuleId(var17);
               ClientModule var13 = (ClientModule)var5.get(var12 != null ? var12 : var17);
               JsonPrimitive var8;
               JsonElement var9;
               if (var13 == null && (var9 = var11.get("name")) instanceof JsonPrimitive && (var8 = (JsonPrimitive)var9).isString()) {
                  var13 = (ClientModule)var5.get(var8.getAsString());
               }

               if (var13 != null) {
                  var4.add(var17, withoutSettings(var13, var11, var1));
               }
            }
         }

         return var2;
      }
   }

   public synchronized boolean enablesOnLoad(ClientModule var1) {
      return Boolean.TRUE.equals(this.pendingEnabled.get(var1.id()));
   }

   public synchronized void applyPendingEnabled() {
      if (!this.pendingEnabled.isEmpty()) {
         ModuleToggleEvent.silently(() -> {
            for (Entry var2 : this.pendingEnabled.entrySet()) {
               ClientModule var3;
               if (Boolean.TRUE.equals(var2.getValue()) && (var3 = ModuleRegistry.resolve((String)var2.getKey())) != null && var3.persistEnabled()) {
                  var3.setEnabled(true);
               }
            }
         });
         this.pendingEnabled.clear();
      }
   }

   public synchronized void save() {
      this.write(false);
   }

   public synchronized void saveIfChanged() {
      if (!this.loaded || this.readOnly || this.writeFailed || ModuleRevision.current() != this.savedRevision) {
         this.write(false);
      }
   }

   public synchronized void saveAndWait() {
      this.write(true);
   }

   private void write(boolean var1) {
      this.ensureLoaded();
      if (!this.readOnly) {
         long var2 = ModuleRevision.current();
         JsonObject var4 = this.buildConfigRoot();
         if (this.activeProfile != null) {
            var4.addProperty("activeProfile", this.activeProfile);
         }

         Path var5 = this.configPath();
         this.writeFailed = false;
         this.savedRevision = var2;

         try {
            this.lastWrite = WRITER.submit(() -> {
               try {
                  Files.createDirectories(var5.getParent());
                  writeJson(var5, var4);
               } catch (RuntimeException | IOException var4x) {
                  this.writeFailed = true;
                  LOGGER.warn("[EFN] Failed to save module config", var4x);
               }
            });
         } catch (RuntimeException var8) {
            this.writeFailed = true;
            return;
         }

         if (var1) {
            try {
               this.lastWrite.get(10L, TimeUnit.SECONDS);
            } catch (Exception var7) {
               LOGGER.warn("[EFN] Module config was still being written at shutdown", var7);
            }
         }
      }
   }

   private static void writeJson(Path var0, JsonObject var1) throws IOException {
      Path var2 = tmpPath(var0);

      try {
         try (BufferedWriter var3 = Files.newBufferedWriter(var2)) {
            GSON.toJson(var1, var3);
         }

         try {
            Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException var12) {
            Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING);
         }
      } finally {
         deleteQuietly(var2);
      }
   }

   private static void deleteQuietly(Path var0) {
      try {
         Files.deleteIfExists(var0);
      } catch (IOException var2) {
         LOGGER.warn("[EFN] Couldn't delete {}", var0.getFileName());
      }
   }

   private static Path tmpPath(Path var0) {
      return var0.resolveSibling(var0.getFileName() + ".tmp");
   }

   public synchronized void tickAutosave() {
      if (!this.writeFailed || !this.loaded || this.readOnly || this.lastWrite != null && !this.lastWrite.isDone()) {
         long var3 = System.nanoTime();
         if (this.autosaveDue(ModuleRevision.current(), var3)) {
            this.save();
            this.observedAt = var3;
         }
      } else {
         long var1 = System.nanoTime();
         if (var1 - this.observedAt >= 2000000000L) {
            this.observedAt = var1;
            this.save();
         }
      }
   }

   private boolean autosaveDue(long var1, long var3) {
      if (!this.loaded || this.readOnly) {
         return false;
      } else if (this.savedRevision < 0L) {
         this.savedRevision = this.observedRevision = var1;
         return false;
      } else if (var1 == this.savedRevision) {
         return false;
      } else if (var1 != this.observedRevision) {
         this.observedRevision = var1;
         this.observedAt = var3;
         return false;
      } else {
         return var3 - this.observedAt >= 2000000000L;
      }
   }

   public synchronized boolean hasUnsavedChanges() {
      return !this.readOnly && this.savedRevision >= 0L && ModuleRevision.current() != this.savedRevision;
   }

   public synchronized boolean isReadOnly() {
      return this.readOnly;
   }

   public synchronized String activeProfile() {
      return this.activeProfile;
   }

   synchronized void setActiveProfile(String var1) {
      this.activeProfile = var1;
   }

   public synchronized JsonObject snapshot() {
      this.ensureLoaded();
      JsonObject var1 = this.buildConfigRoot();
      JsonObject var2 = var1.getAsJsonObject("modules");

      for (ClientModule var4 : ModuleRegistry.all()) {
         if (var4.inConfigs()) {
            var2.add(var4.id(), withoutPrivate(var4, var2.getAsJsonObject(var4.id())));
         } else {
            var2.remove(var4.id());
         }
      }

      return var1;
   }

   public synchronized boolean applyProfile(JsonObject var1) {
      this.ensureLoaded();
      int var2 = profileVersion(var1);
      if (var2 < 0) {
         return false;
      } else {
         List var3 = ModuleRegistry.all();
         Map var4 = matchEntries(var1, var2, var3);
         HashMap var5 = new HashMap();
         ModuleToggleEvent.silently(() -> {
            for (ClientModule var5x : var3) {
               if (var5x.inConfigs()) {
                  JsonObject var6 = (JsonObject)var4.get(var5x);

                  for (Setting var8 : var5x.settings()) {
                     if (!var8.isPrivate()) {
                        var8.resetToDefault();
                     }
                  }

                  var5x.setKeybind(-1);
                  var5x.setHoldMode(false);
                  JsonObject var10 = this.retainedEntries.get(var5x.id());
                  if (var6 != null) {
                     var6 = withoutPrivate(var5x, var6);
                     this.applyModuleEntry(var5x, var6, var5);
                  }

                  if ((var6 = keepSealed(var5x, var10, var6)) != null) {
                     this.retainedEntries.put(var5x.id(), var6);
                  } else {
                     this.retainedEntries.remove(var5x.id());
                  }

                  if (var5x.persistEnabled()) {
                     var5x.setEnabled(Boolean.TRUE.equals(var5.get(var5x.id())));
                  }
               }
            }
         });
         this.pendingEnabled.clear();
         return true;
      }
   }

   public synchronized JsonObject shareable(JsonObject var1) {
      int var2 = profileVersion(var1);
      if (var2 < 0) {
         return null;
      } else {
         List var3 = ModuleRegistry.all();
         Map var4 = matchEntries(var1, var2, var3);
         JsonObject var5 = new JsonObject();

         for (ClientModule var7 : var3) {
            JsonObject var8 = var7.inConfigs() ? (JsonObject)var4.get(var7) : null;
            JsonObject var9 = var8 == null ? null : shareableEntry(var7, var8);
            if (var9 != null) {
               var5.add(var7.id(), var9);
            }
         }

         JsonObject var10 = new JsonObject();
         var10.addProperty("version", 3);
         var10.add("modules", var5);
         return var10;
      }
   }

   private static int profileVersion(JsonObject var0) {
      if (var0 != null && var0.has("modules") && var0.get("modules").isJsonObject()) {
         try {
            int var1 = var0.has("version") ? var0.get("version").getAsInt() : 0;
            return var1 > 3 ? -1 : var1;
         } catch (RuntimeException var2) {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private static Map<ClientModule, JsonObject> matchEntries(JsonObject var0, int var1, List<ClientModule> var2) {
      HashMap var3 = new HashMap();

      for (ClientModule var5 : var2) {
         var3.putIfAbsent(var5.name(), var5);
      }

      for (ClientModule var11 : var2) {
         var3.put(var11.id(), var11);
      }

      HashMap var10 = new HashMap();

      for (Entry var6 : renameModules(var0.getAsJsonObject("modules"), var1).entrySet()) {
         if (((JsonElement)var6.getValue()).isJsonObject()) {
            JsonObject var7 = ((JsonElement)var6.getValue()).getAsJsonObject();
            ClientModule var8 = (ClientModule)var3.get(var6.getKey());
            if (var8 == null && var7.has("name") && var7.get("name").isJsonPrimitive()) {
               var8 = (ClientModule)var3.get(var7.get("name").getAsString());
            }

            if (var8 != null) {
               var10.putIfAbsent(var8, var7);
            }
         }
      }

      return var10;
   }

   private static JsonObject shareableEntry(ClientModule var0, JsonObject var1) {
      JsonObject var3 = new JsonObject();
      if (var0.persistEnabled() && flag(var1, "enabled")) {
         var3.addProperty("enabled", true);
      }

      int var2;
      if ((var2 = number(var1, "keybind", -1)) != -1 && ModuleKeybinds.isBindable(var2)) {
         var3.addProperty("keybind", var2);
      }

      if (flag(var1, "holdMode")) {
         var3.addProperty("holdMode", true);
      }

      JsonObject var4 = var1.has("settings") && var1.get("settings").isJsonObject() ? var1.getAsJsonObject("settings") : null;
      JsonObject var5 = new JsonObject();
      if (var4 != null) {
         for (Setting var7 : var0.settings()) {
            JsonElement var8;
            if (!var7.isPrivate()
               && (var8 = var4.has(var7.id()) ? var4.get(var7.id()) : var4.get(var7.name())) != null
               && !var8.isJsonNull()
               && !var7.isDefault(var8)) {
               var5.add(var7.id(), var7.compact(var8.deepCopy()));
            }
         }
      }

      if (var5.size() > 0) {
         var3.add("settings", var5);
      }

      if (var3.size() == 0) {
         return null;
      } else {
         int var9 = number(var1, "settingsVersion", 0);
         if (var9 > 0) {
            var3.addProperty("settingsVersion", var9);
         }

         return var3;
      }
   }

   private static JsonObject withoutPrivate(ClientModule var0, JsonObject var1) {
      return withoutSettings(var0, var1, Setting::isPrivate);
   }

   private static JsonObject withoutSettings(ClientModule var0, JsonObject var1, Predicate<Setting> var2) {
      JsonObject var3 = var1.deepCopy();
      if (var3.get("settings") instanceof JsonObject var5) {
         for (Setting var7 : var0.settings()) {
            if (var2.test(var7)) {
               var5.remove(var7.id());
               var5.remove(var7.name());
               var5.remove(sealedKey(var7));
            }
         }
      }

      return var3;
   }

   private static JsonObject keepSealed(ClientModule var0, JsonObject var1, JsonObject var2) {
      JsonElement var3;
      if (var1 != null && (var3 = var1.get("settings")) instanceof JsonObject) {
         JsonObject var4 = (JsonObject)var3;

         for (Setting var6 : var0.settings()) {
            JsonElement var10;
            if (var6.isSecret() && (var10 = var4.get(sealedKey(var6))) instanceof JsonPrimitive) {
               JsonPrimitive var11 = (JsonPrimitive)var10;
               if (var2 == null) {
                  var2 = new JsonObject();
               }

               JsonObject var7;
               JsonElement var8;
               JsonObject var9 = (var8 = var2.get("settings")) instanceof JsonObject ? (var7 = (JsonObject)var8) : null;
               if (var9 == null) {
                  var9 = new JsonObject();
                  var2.add("settings", var9);
               }

               var9.add(sealedKey(var6), var11.deepCopy());
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   private static String sealedKey(Setting var0) {
      return var0.id() + "#sealed";
   }

   private static boolean flag(JsonObject var0, String var1) {
      JsonElement var2 = var0.get(var1);
      if (var2 != null && var2.isJsonPrimitive()) {
         try {
            return var2.getAsBoolean();
         } catch (RuntimeException var4) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static int number(JsonObject var0, String var1, int var2) {
      JsonElement var3 = var0.get(var1);
      if (var3 != null && var3.isJsonPrimitive()) {
         try {
            return var3.getAsInt();
         } catch (RuntimeException var5) {
            return var2;
         }
      } else {
         return var2;
      }
   }

   public synchronized void adopt(ClientModule var1) {
      if (this.loaded) {
         JsonObject var2 = this.orphanModules.remove(var1.id());
         if (var2 != null) {
            this.retainedEntries.put(var1.id(), var2.deepCopy());
            this.applyModuleEntry(var1, var2, this.pendingEnabled);
         }
      }
   }

   public synchronized void release(ClientModule var1) {
      if (this.loaded) {
         JsonObject var2 = this.serializeModule(var1);
         if (Boolean.TRUE.equals(this.pendingEnabled.remove(var1.id()))) {
            var2.addProperty("enabled", true);
         }

         this.orphanModules.put(var1.id(), var2);
         this.retainedEntries.remove(var1.id());
      }
   }

   public static String renamedModuleId(String var0) {
      for (Entry var2 : RENAMED_MODULES.entrySet()) {
         if (((String)var2.getKey()).equalsIgnoreCase(var0)) {
            return (String)var2.getValue();
         }
      }

      return null;
   }

   private void applyConfigRoot(JsonObject var1) {
      if (var1 != null) {
         int var2 = var1.has("version") ? var1.get("version").getAsInt() : 0;
         if (var2 > 3) {
            this.readOnly = true;
            LOGGER.warn("[EFN] Module config version {} is newer than supported {}; skipping load", var2, 3);
         } else if (var1.has("modules") && var1.get("modules").isJsonObject()) {
            JsonObject var4 = renameModules(var1.getAsJsonObject("modules"), var2);

            for (Entry var6 : var4.entrySet()) {
               String var7 = (String)var6.getKey();
               if (((JsonElement)var6.getValue()).isJsonObject()) {
                  JsonObject var8 = ((JsonElement)var6.getValue()).getAsJsonObject();
                  ClientModule var9 = ModuleRegistry.resolve(var7);
                  if (var9 == null && var2 >= 2 && var8.has("name") && var8.get("name").isJsonPrimitive()) {
                     var9 = ModuleRegistry.resolve(var8.get("name").getAsString());
                  }

                  if (var9 == null) {
                     this.orphanModules.put(var7, var8.deepCopy());
                  } else {
                     this.retainedEntries.put(var9.id(), var8.deepCopy());
                     this.applyModuleEntry(var9, var8, this.pendingEnabled);
                  }
               }
            }
         }
      }
   }

   private JsonObject buildConfigRoot() {
      JsonObject var1 = new JsonObject();
      var1.addProperty("version", 3);
      JsonObject var2 = new JsonObject();

      for (ClientModule var4 : ModuleRegistry.all()) {
         var2.add(var4.id(), this.serializeModule(var4));
      }

      for (Entry var6 : this.orphanModules.entrySet()) {
         if (!var2.has((String)var6.getKey())) {
            var2.add((String)var6.getKey(), ((JsonObject)var6.getValue()).deepCopy());
         }
      }

      var1.add("modules", var2);
      return var1;
   }

   private void ensureLoaded() {
      if (!this.loaded) {
         this.load();
      }
   }

   private Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("modules.json");
   }

   private void applyModuleEntry(ClientModule var1, JsonObject var2, Map<String, Boolean> var3) {
      if (var2.has("keybind") && var2.get("keybind").isJsonPrimitive()) {
         try {
            int var5 = var2.get("keybind").getAsInt();
            if (var5 == -1 || ModuleKeybinds.isBindable(var5)) {
               var1.setKeybind(var5);
            }
         } catch (RuntimeException var11) {
         }
      }

      if (var2.has("holdMode") && var2.get("holdMode").isJsonPrimitive()) {
         try {
            var1.setHoldMode(var2.get("holdMode").getAsBoolean());
         } catch (RuntimeException var10) {
         }
      }

      if (var2.has("settings") && var2.get("settings").isJsonObject()) {
         JsonObject var12 = var2.getAsJsonObject("settings");

         for (Setting var7 : var1.settings()) {
            JsonElement var8;
            if (!var7.isSessionOnly() && (var8 = var12.has(var7.id()) ? var12.get(var7.id()) : var12.get(var7.name())) != null) {
               var7.deserialize(var8);
            }
         }
      }

      migrateSettings(var1, var2);
      if (var2.has("enabled") && var2.get("enabled").isJsonPrimitive()) {
         boolean var4;
         try {
            var4 = var2.get("enabled").getAsBoolean();
         } catch (RuntimeException var9) {
            return;
         }

         if (var1.persistEnabled()) {
            String var13 = var1.id();
            if (var4) {
               var3.put(var13, true);
            } else {
               var1.setEnabled(false);
            }
         }
      }
   }

   private static void migrateSettings(ClientModule var0, JsonObject var1) {
      int var2 = var0.settingsVersion();
      if (var2 > 0) {
         int var3 = 0;
         if (var1.has("settingsVersion") && var1.get("settingsVersion").isJsonPrimitive()) {
            try {
               var3 = var1.get("settingsVersion").getAsInt();
            } catch (RuntimeException var6) {
            }
         }

         if (var3 < var2) {
            try {
               var0.migrateSettings(var3);
            } catch (RuntimeException var5) {
               LOGGER.warn("[EFN] Failed to migrate settings of '{}' from version {}", new Object[]{var0.id(), var3, var5});
            }
         }
      }
   }

   private static JsonObject renameModules(JsonObject var0, int var1) {
      if (var1 >= 3) {
         return var0;
      } else {
         JsonObject var2 = new JsonObject();

         for (Entry var4 : var0.entrySet()) {
            String var5 = RENAMED_MODULES.get(var4.getKey());
            if (var5 == null) {
               var2.add((String)var4.getKey(), (JsonElement)var4.getValue());
            } else if (!var0.has(var5) && !var2.has(var5)) {
               var2.add(var5, (JsonElement)var4.getValue());
            }
         }

         return var2;
      }
   }

   private JsonObject serializeModule(ClientModule var1) {
      JsonObject var2 = this.retainedEntries.get(var1.id());
      JsonObject var3 = var2 == null ? new JsonObject() : var2.deepCopy();
      var3.addProperty("name", var1.name());
      var3.addProperty("enabled", var1.isEnabled());
      var3.addProperty("keybind", var1.keybind());
      var3.addProperty("holdMode", var1.isHoldMode());
      if (var1.settingsVersion() > 0) {
         var3.addProperty("settingsVersion", var1.settingsVersion());
      }

      JsonObject var4 = var3.has("settings") && var3.get("settings").isJsonObject() ? var3.getAsJsonObject("settings").deepCopy() : new JsonObject();

      for (Setting var6 : var1.settings()) {
         var4.remove(var6.name());
         var4.remove(sealedKey(var6));
         if (var6.isSessionOnly()) {
            var4.remove(var6.id());
         } else {
            var4.add(var6.id(), var6.serialize());
         }
      }

      var3.add("settings", var4);
      return var3;
   }
}
