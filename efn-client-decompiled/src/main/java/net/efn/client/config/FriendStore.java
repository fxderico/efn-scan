package net.efn.client.config;

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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class FriendStore {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final FriendStore INSTANCE = new FriendStore();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 1;
   private static final Pattern VALID_NAME = Pattern.compile("[.*]?[A-Za-z0-9_]{1,16}");
   private final Map<String, String> friends = new LinkedHashMap<>();
   private boolean loaded;
   private boolean readOnly;

   private FriendStore() {
   }

   public static FriendStore getInstance() {
      return INSTANCE;
   }

   public static boolean isValidName(String string) {
      return string != null && VALID_NAME.matcher(string).matches();
   }

   public synchronized boolean isFriend(String string) {
      this.ensureLoaded();
      return string != null && this.friends.containsKey(string.toLowerCase(Locale.ROOT));
   }

   public synchronized List<String> all() {
      this.ensureLoaded();
      return List.copyOf(this.friends.values());
   }

   public synchronized boolean add(String string) {
      this.ensureLoaded();
      String var2 = string == null ? "" : string.trim();
      if (!isValidName(var2)) {
         return false;
      } else if (this.friends.putIfAbsent(var2.toLowerCase(Locale.ROOT), var2) != null) {
         return false;
      } else {
         this.save();
         return true;
      }
   }

   public synchronized boolean remove(String string) {
      this.ensureLoaded();
      if (string != null && this.friends.remove(string.toLowerCase(Locale.ROOT)) != null) {
         this.save();
         return true;
      } else {
         return false;
      }
   }

   public synchronized void replaceAll(Collection<String> collection) {
      this.ensureLoaded();
      this.friends.clear();

      for (String var3 : collection) {
         String var4 = var3 == null ? "" : var3.trim();
         if (isValidName(var4)) {
            this.friends.putIfAbsent(var4.toLowerCase(Locale.ROOT), var4);
         }
      }

      this.save();
   }

   public synchronized void load() {
      if (!this.loaded) {
         this.loaded = true;
         this.friends.clear();
         Path var1 = this.configPath();
         if (Files.isRegularFile(var1)) {
            try {
               try (BufferedReader var2 = Files.newBufferedReader(var1)) {
                  JsonObject var4 = JsonParser.parseReader(var2).getAsJsonObject();
                  int var3 = var4.has("version") ? var4.get("version").getAsInt() : 0;
                  if (var3 > 1) {
                     this.readOnly = true;
                     LOGGER.warn("[EFN] Friend list version {} is newer than supported {}; skipping load", var3, 1);
                     return;
                  }

                  if (var4.has("friends") && var4.get("friends").isJsonArray()) {
                     for (JsonElement var7 : var4.getAsJsonArray("friends")) {
                        String var8;
                        if (var7.isJsonPrimitive() && isValidName(var8 = var7.getAsString().trim())) {
                           this.friends.putIfAbsent(var8.toLowerCase(Locale.ROOT), var8);
                        }
                     }

                     return;
                  }
               }
            } catch (RuntimeException | IOException var11) {
               LOGGER.warn("[EFN] Failed to load friend list; preserving the file", (Throwable)var11);
               this.friends.clear();
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

         for (String var4 : this.friends.values()) {
            var2.add(var4);
         }

         var1.add("friends", var2);
         Path var12 = this.configPath();

         try {
            Files.createDirectories(var12.getParent());
            Path var13 = var12.resolveSibling(var12.getFileName().toString() + ".tmp");

            try (BufferedWriter var5 = Files.newBufferedWriter(var13)) {
               GSON.toJson((JsonElement)var1, var5);
            }

            try {
               Files.move(var13, var12, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException var9) {
               Files.move(var13, var12, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException var11) {
            LOGGER.warn("[EFN] Failed to save friend list", (Throwable)var11);
         }
      }
   }

   private void ensureLoaded() {
      if (!this.loaded) {
         this.load();
      }
   }

   private Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("friends.json");
   }
}
