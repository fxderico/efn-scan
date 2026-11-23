package net.efn.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.efn.client.gui.HudLayoutStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ConfigProfiles {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final ConfigProfiles INSTANCE = new ConfigProfiles();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Pattern VALID_NAME = Pattern.compile("[\\p{L}\\p{Nd} _()-]{1,32}");
   private static final Pattern NUMBERED = Pattern.compile("(.+) \\(\\d{1,3}\\)");
   private static final Set<String> RESERVED = Set.of(
      "con",
      "prn",
      "aux",
      "nul",
      "com1",
      "com2",
      "com3",
      "com4",
      "com5",
      "com6",
      "com7",
      "com8",
      "com9",
      "lpt1",
      "lpt2",
      "lpt3",
      "lpt4",
      "lpt5",
      "lpt6",
      "lpt7",
      "lpt8",
      "lpt9"
   );
   private static final String EXTENSION = ".json";
   private static final String FORMAT_KEY = "efnProfile";
   private static final int FORMAT = 2;
   private static final int OLDEST_FORMAT = 2;
   public static final String TOO_OLD = "Made by an old EFN - old configs no longer work";
   private static final String UPGRADED_MARKER = ".format2";
   private static final String PRESETS_MARKER = ".presets";
   private static final String PRIVATE_MARKER = ".private-values-removed";
   private static final int MAX_NAME = 32;
   private static final int MAX_FRIENDS = 1000;
   private static final long MAX_LOCAL_BYTES = 33554432L;
   public static final int MAX_SERVERS = 16;
   public static final String NAME_RULE = "Use 1-32 letters, digits, spaces, _ - ( )";
   public static final String BACKUP_NAME = "Auto backup";
   private final ModuleConfigStore store;
   private final Map<String, ConfigProfiles.Header> headers = new HashMap<>();
   private List<ConfigProfiles.Profile> cached;
   private boolean upgradeChecked;
   private boolean presetsChecked;
   private boolean privateChecked;

   private ConfigProfiles() {
      this(ModuleConfigStore.getInstance());
   }

   private ConfigProfiles(ModuleConfigStore moduleConfigStore) {
      this.store = moduleConfigStore;
   }

   public static ConfigProfiles getInstance() {
      return INSTANCE;
   }

   public static boolean isValidName(String string) {
      return string != null && string.equals(string.trim()) && VALID_NAME.matcher(string).matches() && !RESERVED.contains(string.toLowerCase(Locale.ROOT));
   }

   public Path directory() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("profiles").toAbsolutePath().normalize();
   }

   public synchronized List<ConfigProfiles.Profile> list() {
      if (this.cached == null) {
         this.cached = this.scan();
      }

      return this.cached;
   }

   public synchronized void refresh() {
      this.cached = null;
   }

   public synchronized ConfigProfiles.Profile find(String string) {
      if (string == null) {
         return null;
      } else {
         for (ConfigProfiles.Profile var3 : this.list()) {
            if (var3.name().equalsIgnoreCase(string)) {
               return var3;
            }
         }

         return null;
      }
   }

   public synchronized String create(String string) throws ProfileException {
      String var2 = string == null ? "" : string.trim();
      if (!isValidName(var2)) {
         throw new ProfileException("Use 1-32 letters, digits, spaces, _ - ( )");
      } else {
         ConfigProfiles.Profile var4 = this.find(var2);
         if (var4 != null) {
            throw new ProfileException(var4.name() + " already exists - use its Save button");
         } else if (Files.exists(this.file(var2))) {
            this.cached = null;
            throw new ProfileException(var2 + " already exists - use its Save button");
         } else {
            ConfigProfiles.Contents var5 = new ConfigProfiles.Contents();
            var5.data.put(ConfigProfiles.Part.MODULES, this.capture(ConfigProfiles.Part.MODULES));
            this.write(var2, var5);
            this.activate(var2);
            return var2;
         }
      }
   }

   public synchronized Set<ConfigProfiles.Part> save(String string) throws ProfileException {
      ConfigProfiles.Profile var2 = this.require(string);
      ConfigProfiles.Contents var3 = this.read(var2.name());
      EnumSet var4 = var3.data.isEmpty() ? EnumSet.of(ConfigProfiles.Part.MODULES) : EnumSet.copyOf(var3.data.keySet());

      for (ConfigProfiles.Part var6 : var4) {
         var3.data.put(var6, this.capture(var6));
      }

      this.write(var2.name(), var3);
      this.activate(var2.name());
      return Collections.unmodifiableSet(var4);
   }

   public synchronized Set<ConfigProfiles.Part> load(String string) throws ProfileException {
      ConfigProfiles.Profile var2 = this.require(string);
      ConfigProfiles.Contents var3 = this.read(var2.name());
      EnumSet var4 = EnumSet.noneOf(ConfigProfiles.Part.class);

      for (Entry var6 : var3.data.entrySet()) {
         if (this.apply((ConfigProfiles.Part)var6.getKey(), (JsonElement)var6.getValue())) {
            var4.add((ConfigProfiles.Part)var6.getKey());
         }
      }

      if (var4.isEmpty()) {
         throw new ProfileException("Nothing in " + var2.name() + " could be applied");
      } else {
         this.activate(var2.name());
         return Collections.unmodifiableSet(var4);
      }
   }

   public synchronized void delete(String string) throws ProfileException {
      ConfigProfiles.Profile var2 = this.require(string);

      try {
         Files.deleteIfExists(this.file(var2.name()));
      } catch (IOException var4) {
         LOGGER.warn("[EFN] Failed to delete profile '{}'", var2.name(), var4);
         throw new ProfileException("Could not delete " + var2.name());
      }

      this.cached = null;
      if (var2.name().equalsIgnoreCase(this.store.activeProfile())) {
         this.store.setActiveProfile(null);
         this.store.save();
      }
   }

   public synchronized String rename(String string, String string2) throws ProfileException {
      ConfigProfiles.Profile var4 = this.require(string);
      String var3 = string2 == null ? "" : string2.trim();
      if (var3.equals(var4.name())) {
         return var3;
      } else if (!isValidName(var3)) {
         throw new ProfileException("Use 1-32 letters, digits, spaces, _ - ( )");
      } else {
         ConfigProfiles.Profile var7 = this.find(var3);
         boolean var5 = var7 != null && var7.name().equals(var4.name());
         if (var7 != null && !var5) {
            throw new ProfileException(var7.name() + " already exists");
         } else {
            Path var9 = this.file(var4.name());
            Path var10 = this.file(var3);

            try {
               if (var5) {
                  Path var11 = var9.resolveSibling(var4.name() + ".rename.tmp");
                  Files.move(var9, var11, StandardCopyOption.REPLACE_EXISTING);

                  try {
                     Files.move(var11, var10);
                  } catch (IOException var13) {
                     Files.move(var11, var9, StandardCopyOption.REPLACE_EXISTING);
                     throw var13;
                  }
               } else {
                  Files.move(var9, var10);
               }
            } catch (IOException var14) {
               LOGGER.warn("[EFN] Failed to rename profile '{}' to '{}'", var4.name(), var3, var14);
               throw new ProfileException("Could not rename " + var4.name());
            }

            this.cached = null;
            if (var4.name().equalsIgnoreCase(this.store.activeProfile())) {
               this.store.setActiveProfile(var3);
               this.store.save();
            }

            return var3;
         }
      }
   }

   public synchronized void setPart(String string, ConfigProfiles.Part part, boolean bl) throws ProfileException {
      ConfigProfiles.Profile var4 = this.require(string);
      ConfigProfiles.Contents var5 = this.read(var4.name());
      if (bl != var5.data.containsKey(part)) {
         if (bl) {
            var5.data.put(part, this.capture(part));
         } else {
            if (var5.data.size() == 1) {
               throw new ProfileException("A profile needs at least one part");
            }

            var5.data.remove(part);
         }

         this.write(var4.name(), var5);
      }
   }

   public synchronized String addServer(String string, String string2) throws ProfileException {
      ServerAddresses.Address var3 = ServerAddresses.parse(string2);
      if (var3 == null) {
         throw new ProfileException("That isn't a server address");
      } else {
         ConfigProfiles.Profile var4 = this.require(string);
         ConfigProfiles.Contents var5 = this.read(var4.name());
         String var6 = var3.toString();
         if (var5.servers.contains(var6)) {
            throw new ProfileException(var6 + " is already on the list");
         } else if (var5.servers.size() >= 16) {
            throw new ProfileException("A profile can list up to 16 servers");
         } else {
            var5.servers.add(var6);
            this.write(var4.name(), var5);
            return var6;
         }
      }
   }

   public synchronized void removeServer(String string, String string2) throws ProfileException {
      ConfigProfiles.Profile var3 = this.require(string);
      ConfigProfiles.Contents var4 = this.read(var3.name());
      if (var4.servers.remove(string2)) {
         this.write(var3.name(), var4);
      }
   }

   public synchronized void exportTo(String string, Path path) throws ProfileException {
      JsonObject var3 = this.exportJson(string);

      try {
         Files.writeString(path, GSON.toJson((JsonElement)var3), StandardCharsets.UTF_8);
      } catch (RuntimeException | IOException var5) {
         LOGGER.warn("[EFN] Failed to export profile '{}' to {}", string, path, var5);
         throw new ProfileException("Could not write " + path.getFileName());
      }
   }

   public synchronized String shareCode(String string) throws ProfileException {
      return ProfileCodes.encode(this.exportJson(string));
   }

   public synchronized String importFile(Path path) throws ProfileException {
      String var2;
      try {
         if (!Files.isRegularFile(path)) {
            throw new ProfileException("That file doesn't exist");
         }

         if (Files.size(path) > 4194304L) {
            throw new ProfileException("That file is too large to be a profile");
         }

         var2 = Files.readString(path, StandardCharsets.UTF_8);
      } catch (RuntimeException | IOException var6) {
         throw new ProfileException("Couldn't read " + path.getFileName());
      }

      String var3 = path.getFileName().toString();
      int var4 = var3.lastIndexOf(46);
      String var5 = var4 > 0 ? var3.substring(0, var4) : var3;
      return this.importRoot(ProfileCodes.parseObject(var2, "That file isn't an EFN profile"), var5);
   }

   public synchronized String importCode(String string) throws ProfileException {
      try {
         return this.importRoot(ProfileCodes.decode(string), "Shared profile");
      } catch (ProfileException var3) {
         LOGGER.warn("[EFN] Share code import failed ({} characters on the clipboard): {}", string == null ? 0 : string.length(), var3.getMessage());
         throw var3;
      }
   }

   public synchronized void backup(Set<ConfigProfiles.Part> set) throws ProfileException {
      ConfigProfiles.Contents var2 = new ConfigProfiles.Contents();

      for (ConfigProfiles.Part var4 : set) {
         var2.data.put(var4, this.capture(var4));
      }

      if (var2.data.isEmpty()) {
         var2.data.put(ConfigProfiles.Part.MODULES, this.capture(ConfigProfiles.Part.MODULES));
      }

      this.write("Auto backup", var2);
      this.cached = null;
   }

   public synchronized List<String> loadForServer(String string) {
      ServerAddresses.Address var2 = ServerAddresses.parse(string);
      if (var2 == null) {
         return List.of();
      } else {
         this.cached = null;
         ArrayList var3 = new ArrayList();

         for (ConfigProfiles.Profile var5 : this.list()) {
            if (!var5.broken()) {
               int var6 = 0;
               int var7 = 0;

               for (String var9 : var5.servers()) {
                  ServerAddresses.Address var10 = ServerAddresses.parse(var9);
                  int var11 = ServerAddresses.score(var10, var2);
                  if (var11 > var6 || var11 == var6 && var11 > 0 && var10.host().length() > var7) {
                     var6 = var11;
                     var7 = var10.host().length();
                  }
               }

               if (var6 > 0) {
                  var3.add(new ConfigProfiles.Match(var5.name(), var6, var7));
               }
            }
         }

         var3.sort(
            Comparator.comparingInt(ConfigProfiles.Match::score)
               .thenComparingInt(ConfigProfiles.Match::length)
               .thenComparing(match -> match.name().toLowerCase(Locale.ROOT))
         );
         ArrayList var13 = new ArrayList();

         for (ConfigProfiles.Match var15 : var3) {
            try {
               this.load(var15.name());
               var13.add(var15.name());
            } catch (ProfileException var12) {
               LOGGER.warn("[EFN] Could not load profile '{}' for {}: {}", var15.name(), var2, var12.getMessage());
            }
         }

         return var13;
      }
   }

   private ConfigProfiles.Profile require(String string) throws ProfileException {
      ConfigProfiles.Profile var2 = this.find(string);
      if (var2 == null) {
         throw new ProfileException("That profile no longer exists");
      } else {
         return var2;
      }
   }

   private void activate(String string) {
      this.store.setActiveProfile(string);
      this.store.save();
   }

   private JsonObject exportJson(String string) throws ProfileException {
      ConfigProfiles.Profile var2 = this.require(string);
      ConfigProfiles.Contents var3 = this.shareable(this.read(var2.name()), "Could not prepare " + var2.name() + " for sharing");
      if (var3.data.isEmpty()) {
         throw new ProfileException(var2.name() + " has nothing to share");
      } else {
         JsonObject var4 = new JsonObject();
         var4.addProperty("efnProfile", 2);
         var4.addProperty("name", var2.name());
         var4.add("data", data(var3));
         return var4;
      }
   }

   private String importRoot(JsonObject jsonObject, String string) throws ProfileException {
      ConfigProfiles.Contents var3 = this.shareable(parse(jsonObject, false, false), "That profile is damaged");
      if (var3.data.isEmpty()) {
         throw new ProfileException("That profile has nothing EFN can use");
      } else {
         JsonElement var4 = jsonObject.get("name");
         String var5 = var4 != null && var4.isJsonPrimitive() ? var4.getAsString() : string;
         String var6 = this.uniqueName(var5);
         this.write(var6, var3);
         return var6;
      }
   }

   private JsonElement capture(ConfigProfiles.Part part) throws ProfileException {
      try {
         return (JsonElement)(switch (part) {
            case MODULES -> this.store.snapshot();
            case HUD -> {
               JsonObject var6 = new JsonObject();
               var6.add("layout", HudLayoutStore.getInstance().profileSnapshot());
               var6.add("theme", HudTheme.get().profileSnapshot());
               yield var6;
            }
            case THEME -> ThemeStore.get().profileSnapshot();
            case FRIENDS -> {
               JsonArray var2 = new JsonArray();

               for (String var4 : FriendStore.getInstance().all()) {
                  var2.add(var4);
               }

               yield var2;
            }
         });
      } catch (RuntimeException var5) {
         LOGGER.warn("[EFN] Failed to capture the {} part of a profile", part.key, var5);
         throw new ProfileException("Could not read your current " + part.label);
      }
   }

   private boolean apply(ConfigProfiles.Part part, JsonElement jsonElement) {
      try {
         return switch (part) {
            case MODULES -> jsonElement.isJsonObject() && this.store.applyProfile(jsonElement.getAsJsonObject());
            case HUD -> applyHud(jsonElement);
            case THEME -> {
               if (!jsonElement.isJsonObject()) {
                  yield false;
               } else {
                  ThemeStore var3 = ThemeStore.get();
                  var3.applyProfile(jsonElement.getAsJsonObject());
                  var3.save();
                  yield true;
               }
            }
            case FRIENDS -> {
               if (!jsonElement.isJsonArray()) {
                  yield false;
               } else {
                  FriendStore.getInstance().replaceAll(friendNames(jsonElement.getAsJsonArray()));
                  yield true;
               }
            }
         };
      } catch (RuntimeException var4) {
         LOGGER.warn("[EFN] Failed to apply the {} part of a profile", part.key, var4);
         return false;
      }
   }

   private static boolean applyHud(JsonElement jsonElement) {
      if (!jsonElement.isJsonObject()) {
         return false;
      } else {
         JsonObject var1 = jsonElement.getAsJsonObject();
         boolean var2 = false;
         if (var1.has("layout") && var1.get("layout").isJsonObject()) {
            var2 = HudLayoutStore.getInstance().applyProfile(var1.getAsJsonObject("layout"));
         }

         if (var1.has("theme") && var1.get("theme").isJsonObject()) {
            HudTheme var3 = HudTheme.get();
            var3.applyProfile(var1.getAsJsonObject("theme"));
            var3.save();
            var2 = true;
         }

         return var2;
      }
   }

   private ConfigProfiles.Contents shareable(ConfigProfiles.Contents contents, String string) throws ProfileException {
      ConfigProfiles.Contents var3 = new ConfigProfiles.Contents();

      try {
         for (Entry var5 : contents.data.entrySet()) {
            JsonElement var6 = this.shareable((ConfigProfiles.Part)var5.getKey(), (JsonElement)var5.getValue());
            if (var6 != null) {
               var3.data.put((ConfigProfiles.Part)var5.getKey(), var6);
            }
         }

         return var3;
      } catch (RuntimeException var7) {
         LOGGER.warn("[EFN] Failed to filter a profile for sharing", (Throwable)var7);
         throw new ProfileException(string);
      }
   }

   private JsonElement shareable(ConfigProfiles.Part part, JsonElement jsonElement) {
      return (JsonElement)(switch (part) {
         case MODULES -> jsonElement.isJsonObject() ? this.store.shareable(jsonElement.getAsJsonObject()) : null;
         case HUD -> {
            if (!jsonElement.isJsonObject()) {
               yield null;
            } else {
               JsonObject var7 = jsonElement.getAsJsonObject();
               JsonObject var8 = new JsonObject();
               JsonObject var6;
               if (var7.has("layout") && var7.get("layout").isJsonObject() && (var6 = HudLayoutStore.shareable(var7.getAsJsonObject("layout"))) != null) {
                  var8.add("layout", var6);
               }

               if (var7.has("theme") && var7.get("theme").isJsonObject()) {
                  var8.add("theme", HudTheme.get().shareable(var7.getAsJsonObject("theme")));
               }

               yield var8.size() == 0 ? null : var8;
            }
         }
         case THEME -> jsonElement.isJsonObject() ? ThemeStore.get().shareable(jsonElement.getAsJsonObject()) : null;
         case FRIENDS -> {
            if (!jsonElement.isJsonArray()) {
               yield null;
            } else {
               JsonArray var3 = new JsonArray();

               for (String var5 : friendNames(jsonElement.getAsJsonArray())) {
                  var3.add(var5);
               }

               yield var3;
            }
         }
      });
   }

   private static List<String> friendNames(JsonArray jsonArray) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (JsonElement var3 : jsonArray) {
         if (var1.size() >= 1000) {
            break;
         }

         String var4;
         if (var3.isJsonPrimitive() && FriendStore.isValidName(var4 = var3.getAsString().trim())) {
            var1.putIfAbsent(var4.toLowerCase(Locale.ROOT), var4);
         }
      }

      return List.copyOf(var1.values());
   }

   private static ConfigProfiles.Contents parse(JsonObject jsonObject, boolean bl, boolean bl2) throws ProfileException {
      ConfigProfiles.Contents var3 = new ConfigProfiles.Contents();
      if (jsonObject.has("efnProfile")) {
         int var4;
         try {
            var4 = jsonObject.get("efnProfile").getAsInt();
         } catch (RuntimeException var11) {
            throw new ProfileException("That isn't an EFN profile");
         }

         if (var4 > 2) {
            throw new ProfileException("Made by a newer EFN - update to open it");
         }

         if (var4 < 1) {
            throw new ProfileException("That isn't an EFN profile");
         }

         if (var4 < 2 && !bl2) {
            throw new ProfileException("Made by an old EFN - old configs no longer work");
         }

         JsonElement var5 = jsonObject.get("data");
         if (var5 != null && var5.isJsonObject()) {
            for (ConfigProfiles.Part var9 : ConfigProfiles.Part.values()) {
               JsonElement var10 = var5.getAsJsonObject().get(var9.key);
               if (var10 != null && (var9 == ConfigProfiles.Part.FRIENDS ? var10.isJsonArray() : var10.isJsonObject())) {
                  var3.data.put(var9, var10);
               }
            }
         }

         JsonElement var12 = jsonObject.get("loadOnJoin");
         if (bl && var12 != null && var12.isJsonArray()) {
            for (JsonElement var14 : var12.getAsJsonArray()) {
               if (var14.isJsonPrimitive()) {
                  addServer(var3.servers, var14.getAsString());
               }
            }
         }
      } else {
         if (!jsonObject.has("modules") || !jsonObject.get("modules").isJsonObject()) {
            throw new ProfileException("That isn't an EFN profile");
         }

         if (!bl2) {
            throw new ProfileException("Made by an old EFN - old configs no longer work");
         }

         var3.data.put(ConfigProfiles.Part.MODULES, jsonObject);
      }

      return var3;
   }

   private static void addServer(List<String> list, String string) {
      ServerAddresses.Address var2 = ServerAddresses.parse(string);
      if (var2 != null && list.size() < 16) {
         String var3 = var2.toString();
         if (!list.contains(var3)) {
            list.add(var3);
         }
      }
   }

   private static JsonObject data(ConfigProfiles.Contents contents) {
      JsonObject var1 = new JsonObject();

      for (Entry var3 : contents.data.entrySet()) {
         var1.add(((ConfigProfiles.Part)var3.getKey()).key, (JsonElement)var3.getValue());
      }

      return var1;
   }

   private ConfigProfiles.Contents read(String string) throws ProfileException {
      Path var3 = this.file(string);

      try {
         if (Files.size(var3) > 33554432L) {
            throw new ProfileException(string + " is too large to open");
         } else {
            ConfigProfiles.Contents var2;
            try (BufferedReader var4 = Files.newBufferedReader(var3, StandardCharsets.UTF_8)) {
               JsonElement var5 = JsonParser.parseReader(var4);
               if (!var5.isJsonObject()) {
                  throw new ProfileException(string + " isn't a profile file");
               }

               var2 = parse(var5.getAsJsonObject(), true, false);
            }

            return var2;
         }
      } catch (RuntimeException | IOException var9) {
         LOGGER.warn("[EFN] Failed to read profile '{}'", string, var9);
         throw new ProfileException("Could not read " + string);
      }
   }

   private void write(String string, ConfigProfiles.Contents contents) throws ProfileException {
      JsonElement var3 = contents.data.get(ConfigProfiles.Part.MODULES);
      if (var3 != null && var3.isJsonObject()) {
         contents.data.put(ConfigProfiles.Part.MODULES, ModuleConfigStore.withoutPrivateValues(var3.getAsJsonObject()));
      }

      JsonObject var4 = new JsonObject();
      var4.addProperty("efnProfile", 2);
      JsonArray var5 = new JsonArray();

      for (String var7 : contents.servers) {
         var5.add(var7);
      }

      var4.add("loadOnJoin", var5);
      var4.add("data", data(contents));
      Path var15 = this.file(string);

      try {
         Files.createDirectories(var15.getParent());
         Path var16 = var15.resolveSibling(string + ".json.tmp");

         try (BufferedWriter var8 = Files.newBufferedWriter(var16, StandardCharsets.UTF_8)) {
            GSON.toJson((JsonElement)var4, var8);
         }

         try {
            Files.move(var16, var15, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException var12) {
            Files.move(var16, var15, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (RuntimeException | IOException var14) {
         LOGGER.warn("[EFN] Failed to save profile '{}'", string, var14);
         throw new ProfileException("Could not save " + string);
      }

      this.cached = null;
   }

   private Path file(String string) throws ProfileException {
      if (!isValidName(string)) {
         throw new ProfileException("Use 1-32 letters, digits, spaces, _ - ( )");
      } else {
         Path var3 = this.directory();
         Path var2;
         if (!var3.equals((var2 = var3.resolve(string + ".json").normalize()).getParent())) {
            throw new ProfileException("Use 1-32 letters, digits, spaces, _ - ( )");
         } else {
            return var2;
         }
      }
   }

   private String uniqueName(String string) throws ProfileException {
      String var2 = cleanName(string);
      if (!this.taken(var2)) {
         return var2;
      } else {
         Matcher var3 = NUMBERED.matcher(var2);
         String var4 = var3.matches() ? var3.group(1) : var2;

         for (int var5 = 2; var5 < 1000; var5++) {
            String var6 = " (" + var5 + ")";
            String var7 = cut(var4, 32 - var6.length()).stripTrailing() + var6;
            if (isValidName(var7) && !this.taken(var7)) {
               return var7;
            }
         }

         throw new ProfileException("Too many profiles share that name");
      }
   }

   private boolean taken(String string) {
      return this.find(string) != null || Files.exists(this.directory().resolve(string + ".json"));
   }

   private static String cleanName(String string) {
      StringBuilder var2 = new StringBuilder();
      String var3 = string == null ? "" : string;
      int var4 = 0;

      while (var4 < var3.length()) {
         int var1 = var3.codePointAt(var4);
         if (Character.isLetter(var1) || Character.isDigit(var1) || "_-()".indexOf(var1) >= 0) {
            var2.appendCodePoint(var1);
         }

         var4 += Character.charCount(var1);
      }

      String var5 = cut(var2.toString().replaceAll(" {2,}", " ").trim(), 32).trim();
      return isValidName(var5) ? var5 : "Imported";
   }

   private static String cut(String string, int i) {
      return string.codePointCount(0, string.length()) <= i ? string : string.substring(0, string.offsetByCodePoints(0, i));
   }

   private List<ConfigProfiles.Profile> scan() {
      Path var1 = this.directory();
      this.upgradeLocalProfiles(var1);
      this.removePrivateValues(var1);
      this.installPresets(var1);
      if (!Files.isDirectory(var1)) {
         this.headers.clear();
         return List.of();
      } else {
         ArrayList var2 = new ArrayList();
         HashSet var3 = new HashSet();

         try (DirectoryStream var4 = Files.newDirectoryStream(var1, "*.json")) {
            for (Path var6 : var4) {
               String var7 = var6.getFileName().toString();
               String var8 = var7.substring(0, var7.length() - ".json".length());
               if (isValidName(var8) && Files.isRegularFile(var6) && var3.add(var8.toLowerCase(Locale.ROOT))) {
                  var2.add(this.header(var8, var6));
               }
            }
         } catch (IOException var11) {
            LOGGER.warn("[EFN] Failed to list profiles", (Throwable)var11);
         }

         HashSet var12 = new HashSet();

         for (ConfigProfiles.Profile var14 : var2) {
            var12.add(var14.name());
         }

         this.headers.keySet().retainAll(var12);
         var2.sort(Comparator.comparing(profile -> profile.name().toLowerCase(Locale.ROOT)));
         return List.copyOf(var2);
      }
   }

   private void upgradeLocalProfiles(Path path) {
      if (!this.upgradeChecked) {
         this.upgradeChecked = true;
         Path var2 = path.resolve(".format2");

         try {
            if (Files.exists(var2)) {
               return;
            }

            Files.createDirectories(path);

            try (DirectoryStream var3 = Files.newDirectoryStream(path, "*.json")) {
               for (Path var5 : var3) {
                  String var6 = var5.getFileName().toString();
                  String var7 = var6.substring(0, var6.length() - ".json".length());
                  if (isValidName(var7) && Files.isRegularFile(var5) && Files.size(var5) <= 33554432L) {
                     this.upgrade(var7, var5);
                  }
               }
            }

            Files.writeString(var2, "Profiles here were moved to format 2.\n", StandardCharsets.UTF_8);
         } catch (RuntimeException | IOException var10) {
            LOGGER.warn("[EFN] Failed to move old profiles to the current format", (Throwable)var10);
         }
      }
   }

   private void removePrivateValues(Path path) {
      if (!this.privateChecked) {
         this.privateChecked = true;
         Path var2 = path.resolve(".private-values-removed");

         try {
            if (Files.exists(var2) || !Files.isDirectory(path)) {
               return;
            }

            try (DirectoryStream var3 = Files.newDirectoryStream(path, "*.json")) {
               for (Path var5 : var3) {
                  String var6 = var5.getFileName().toString();
                  String var7 = var6.substring(0, var6.length() - ".json".length());
                  if (isValidName(var7) && Files.isRegularFile(var5) && Files.size(var5) <= 33554432L) {
                     try {
                        ConfigProfiles.Contents var8 = this.read(var7);
                        JsonElement var9 = var8.data.get(ConfigProfiles.Part.MODULES);
                        if (var9 != null && var9.isJsonObject() && !ModuleConfigStore.withoutPrivateValues(var9.getAsJsonObject()).equals(var9)) {
                           this.write(var7, var8);
                        }
                     } catch (ProfileException var11) {
                        LOGGER.warn("[EFN] Couldn't clean private values out of profile '{}'", var7);
                     }
                  }
               }
            }

            Files.writeString(var2, "Private values were removed from the profiles here.\n", StandardCharsets.UTF_8);
         } catch (RuntimeException | IOException var13) {
            LOGGER.warn("[EFN] Failed to clean private values out of the profiles", (Throwable)var13);
         }
      }
   }

   private void installPresets(Path path) {
      if (!this.presetsChecked) {
         this.presetsChecked = true;
         List var2 = BundledDefaults.presets();
         if (!var2.isEmpty()) {
            Path var3 = path.resolve(".presets");

            try {
               ArrayList var4 = new ArrayList();
               if (Files.isRegularFile(var3)) {
                  for (String var6 : Files.readAllLines(var3, StandardCharsets.UTF_8)) {
                     if (!var6.isBlank()) {
                        var4.add(var6.trim().toLowerCase(Locale.ROOT));
                     }
                  }
               }

               int var15 = var4.size();

               for (JsonObject var7 : var2) {
                  JsonElement var10 = var7.get("name");
                  String var8;
                  String var9;
                  if (var10 != null && var10.isJsonPrimitive() && !var4.contains(var9 = (var8 = var10.getAsString()).toLowerCase(Locale.ROOT))) {
                     try {
                        Path var11 = this.file(var8);
                        ConfigProfiles.Contents var12 = this.shareable(parse(var7, false, false), "That profile is damaged");
                        if (!Files.exists(var11) && !var12.data.isEmpty()) {
                           this.write(var8, var12);
                        }

                        var4.add(var9);
                     } catch (ProfileException var13) {
                        LOGGER.warn("[EFN] Could not add the bundled profile '{}': {}", var8, var13.getMessage());
                     }
                  }
               }

               if (var4.size() == var15) {
                  return;
               }

               Files.createDirectories(path);
               Files.write(var3, var4, StandardCharsets.UTF_8);
            } catch (RuntimeException | IOException var14) {
               LOGGER.warn("[EFN] Failed to add the bundled profiles", (Throwable)var14);
            }
         }
      }
   }

   private void upgrade(String string, Path path) {
      try {
         JsonElement var3;
         try (BufferedReader var4 = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var3 = JsonParser.parseReader(var4);
         }

         if (!var3.isJsonObject()) {
            return;
         }

         JsonObject var10 = var3.getAsJsonObject();
         JsonElement var5 = var10.get("efnProfile");
         if (var5 != null && var5.isJsonPrimitive() && var5.getAsInt() >= 2) {
            return;
         }

         this.write(string, parse(var10, true, true));
         LOGGER.info("[EFN] Moved profile '{}' to the current format", string);
      } catch (RuntimeException | ProfileException | IOException var9) {
         LOGGER.warn("[EFN] Could not move profile '{}' to the current format", string, var9);
      }
   }

   private ConfigProfiles.Profile header(String string, Path path) {
      try {
         BasicFileAttributes var3 = Files.readAttributes(path, BasicFileAttributes.class);
         ConfigProfiles.Header var4 = this.headers.get(string);
         if (var4 != null && var4.modified().equals(var3.lastModifiedTime()) && var4.size() == var3.size()) {
            return var4.profile();
         } else {
            ConfigProfiles.Profile var5 = readHeader(string, path);
            this.headers.put(string, new ConfigProfiles.Header(var3.lastModifiedTime(), var3.size(), var5));
            return var5;
         }
      } catch (IOException var6) {
         return new ConfigProfiles.Profile(string, Set.of(), List.of(), true);
      }
   }

   private static ConfigProfiles.Profile readHeader(String string, Path path) {
      EnumSet var2 = EnumSet.noneOf(ConfigProfiles.Part.class);
      ArrayList var3 = new ArrayList();
      int var4 = -1;
      boolean var5 = false;

      try (JsonReader var6 = new JsonReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
         var6.beginObject();

         while (var6.hasNext()) {
            String var7 = var6.nextName();
            switch (var7) {
               case "efnProfile":
                  var4 = var6.nextInt();
                  break;
               case "loadOnJoin":
                  readServers(var6, var3);
                  break;
               case "data":
                  readParts(var6, var2);
                  break;
               case "modules":
                  var5 = var6.peek() == JsonToken.BEGIN_OBJECT;
                  var6.skipValue();
                  break;
               default:
                  var6.skipValue();
            }
         }

         var6.endObject();
      } catch (RuntimeException | IOException var11) {
         LOGGER.warn("[EFN] Failed to read profile '{}'", string, var11);
         return new ConfigProfiles.Profile(string, Set.of(), List.of(), true);
      }

      if (var4 < 0 && var5) {
         return new ConfigProfiles.Profile(string, Collections.unmodifiableSet(EnumSet.of(ConfigProfiles.Part.MODULES)), List.of(), true);
      } else {
         boolean var12 = var4 < 2 || var4 > 2 || var2.isEmpty();
         return new ConfigProfiles.Profile(string, Collections.unmodifiableSet(var2), List.copyOf(var3), var12);
      }
   }

   private static void readServers(JsonReader jsonReader, List<String> list) throws IOException {
      if (jsonReader.peek() != JsonToken.BEGIN_ARRAY) {
         jsonReader.skipValue();
      } else {
         jsonReader.beginArray();

         while (jsonReader.hasNext()) {
            if (jsonReader.peek() == JsonToken.STRING) {
               addServer(list, jsonReader.nextString());
            } else {
               jsonReader.skipValue();
            }
         }

         jsonReader.endArray();
      }
   }

   private static void readParts(JsonReader jsonReader, Set<ConfigProfiles.Part> set) throws IOException {
      if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
         jsonReader.skipValue();
      } else {
         jsonReader.beginObject();

         while (jsonReader.hasNext()) {
            String var2 = jsonReader.nextName();
            JsonToken var3 = jsonReader.peek();

            for (ConfigProfiles.Part var7 : ConfigProfiles.Part.values()) {
               if (var7.key.equals(var2) && var3 == (var7 == ConfigProfiles.Part.FRIENDS ? JsonToken.BEGIN_ARRAY : JsonToken.BEGIN_OBJECT)) {
                  set.add(var7);
               }
            }

            jsonReader.skipValue();
         }

         jsonReader.endObject();
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Contents {
      final List<String> servers = new ArrayList<>();
      final Map<ConfigProfiles.Part, JsonElement> data = new EnumMap<>(ConfigProfiles.Part.class);

      private Contents() {
      }
   }

   @Environment(EnvType.CLIENT)
   private record Header(FileTime modified, long size, ConfigProfiles.Profile profile) {
   }

   @Environment(EnvType.CLIENT)
   private record Match(String name, int score, int length) {
   }

   @Environment(EnvType.CLIENT)
   public static enum Part {
      MODULES("modules", "Modules"),
      HUD("hud", "HUD"),
      THEME("theme", "Theme"),
      FRIENDS("friends", "Friends");

      public final String key;
      public final String label;

      private Part(String string2, String string3) {
         this.key = string2;
         this.label = string3;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Profile(String name, Set<ConfigProfiles.Part> parts, List<String> servers, boolean broken) {
   }
}
