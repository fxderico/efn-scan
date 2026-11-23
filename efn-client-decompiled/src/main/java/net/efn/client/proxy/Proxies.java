package net.efn.client.proxy;

import com.google.common.net.InetAddresses;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class Proxies {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final Proxies INSTANCE = new Proxies();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 1;
   private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}){3}");
   private static final Pattern HOST_NAME = Pattern.compile("[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*");
   private final List<Proxy> proxies = new CopyOnWriteArrayList<>();
   private final List<Proxy> view = Collections.unmodifiableList(this.proxies);
   private volatile boolean loaded;
   private boolean readOnly;

   private Proxies() {
   }

   public static Proxies get() {
      return INSTANCE;
   }

   public List<Proxy> list() {
      this.ensureLoaded();
      return this.view;
   }

   public Proxy enabled() {
      this.ensureLoaded();

      for (Proxy var2 : this.proxies) {
         if (var2.enabled) {
            return var2;
         }
      }

      return null;
   }

   public synchronized void setEnabled(Proxy proxy, boolean bl) {
      this.ensureLoaded();
      if (this.proxies.contains(proxy)) {
         if (bl) {
            if (!proxy.isValid()) {
               return;
            }

            for (Proxy var4 : this.proxies) {
               var4.enabled = var4 == proxy;
            }
         } else {
            proxy.enabled = false;
         }

         this.save();
      }
   }

   public synchronized void remove(Proxy proxy) {
      this.ensureLoaded();
      if (this.proxies.remove(proxy)) {
         this.save();
      }
   }

   public synchronized void removeAll(Collection<Proxy> collection) {
      this.ensureLoaded();
      if (this.proxies.removeAll(Set.copyOf(collection))) {
         this.save();
      }
   }

   public synchronized void checked() {
      this.ensureLoaded();
      this.save();
   }

   public synchronized int importText(String string) {
      this.ensureLoaded();
      if (string != null && !string.isBlank()) {
         HashSet var2 = new HashSet();

         for (Proxy var4 : this.proxies) {
            var2.add(key(var4));
         }

         ArrayList var9 = new ArrayList();

         for (String var7 : string.split("\\R")) {
            Proxy var8 = parseLine(var7);
            if (var8 != null && var2.add(key(var8))) {
               var9.add(var8);
            }
         }

         if (var9.isEmpty()) {
            return 0;
         } else {
            this.proxies.addAll(var9);
            this.save();
            return var9.size();
         }
      } else {
         return 0;
      }
   }

   synchronized Proxy enabledCopy() {
      Proxy var1 = this.enabled();
      return var1 == null ? null : var1.copy();
   }

   private void keepOneEnabled() {
      Proxy var1 = null;

      for (Proxy var3 : this.proxies) {
         if (var1 == null && var3.enabled && var3.isValid()) {
            var1 = var3;
         }
      }

      for (Proxy var6 : this.proxies) {
         boolean var4 = var6 == var1;
         var6.enabled = var4;
      }
   }

   private static String key(Proxy proxy) {
      return proxy.displayAddress().toLowerCase(Locale.ROOT);
   }

   static Proxy parseLine(String string) {
      String var4 = string == null ? "" : string.strip();
      if (!var4.isEmpty() && !var4.startsWith("#")) {
         ProxyType var6 = ProxyType.SOCKS5;
         int var7 = var4.indexOf("://");
         if (var7 >= 0) {
            var6 = ProxyType.parse(var4.substring(0, var7));
            if (var6 == null) {
               return null;
            }

            if ((var4 = var4.substring(var7 + 3)).endsWith("/")) {
               var4 = var4.substring(0, var4.length() - 1);
            }
         }

         String var8 = "";
         String var9 = "";
         int var10 = var4.lastIndexOf(64);
         boolean var3 = var10 >= 0;
         if (var3) {
            String var12 = var4.substring(0, var10);
            var4 = var4.substring(var10 + 1);
            int var2 = var12.indexOf(58);
            var8 = var2 < 0 ? var12 : var12.substring(0, var2);
            var9 = var2 < 0 ? "" : var12.substring(var2 + 1);
         }

         String var1;
         if (var4.startsWith("[")) {
            int var18 = var4.indexOf(93);
            if (var18 < 0 || var18 + 1 >= var4.length() || var4.charAt(var18 + 1) != ':') {
               return null;
            }

            var1 = var4.substring(1, var18);
            var4 = var4.substring(var18 + 2);
         } else {
            int var16 = var4.indexOf(58);
            if (var16 < 0) {
               return null;
            }

            var1 = var4.substring(0, var16);
            var4 = var4.substring(var16 + 1);
         }

         String[] var19 = var4.split(":", 3);
         int var13 = parsePort(var19[0]);
         var1 = normalizeHost(var1);
         if (var13 >= 0 && var1 != null) {
            if (var19.length > 1) {
               if (var3 || var19[1].isEmpty()) {
                  return null;
               }

               var8 = var19[1];
               var9 = var19.length > 2 ? var19[2] : "";
            }

            Proxy var14 = new Proxy();
            var14.type = var6;
            var14.address = var1;
            var14.port = var13;
            var14.username = var8;
            var14.password = var9;
            var14.name = var14.displayAddress();
            return var14;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static int parsePort(String string) {
      if (!string.isEmpty() && string.length() <= 5) {
         for (int var1 = 0; var1 < string.length(); var1++) {
            if (string.charAt(var1) < '0' || string.charAt(var1) > '9') {
               return -1;
            }
         }

         int var2 = Integer.parseInt(string);
         return var2 >= 1 && var2 <= 65535 ? var2 : -1;
      } else {
         return -1;
      }
   }

   private static String normalizeHost(String string) {
      if (IPV4.matcher(string).matches()) {
         StringBuilder var1 = new StringBuilder();

         for (String var5 : string.split("\\.")) {
            int var6 = Integer.parseInt(var5);
            if (var6 > 255) {
               return null;
            }

            if (!var1.isEmpty()) {
               var1.append('.');
            }

            var1.append(var6);
         }

         return var1.toString();
      } else if (string.indexOf(58) >= 0) {
         return InetAddresses.isInetAddress(string) ? string : null;
      } else {
         return HOST_NAME.matcher(string).matches() ? string : null;
      }
   }

   private void ensureLoaded() {
      if (!this.loaded) {
         this.load();
      }
   }

   private synchronized void load() {
      if (!this.loaded) {
         Path var1 = configPath();
         ArrayList var2 = new ArrayList();
         if (Files.isRegularFile(var1)) {
            try (BufferedReader var3 = Files.newBufferedReader(var1)) {
               JsonObject var4 = JsonParser.parseReader(var3).getAsJsonObject();
               int var5 = integer(var4, "version");
               if (var5 > 1) {
                  this.readOnly = true;
                  LOGGER.warn("[EFN] Proxy list version {} is newer than supported {}; skipping load", var5, 1);
               } else {
                  JsonElement var6 = var4.get("proxies");
                  if (var6 instanceof JsonArray) {
                     for (JsonElement var9 : (JsonArray)var6) {
                        if (var9 instanceof JsonObject var10) {
                           var2.add(readEntry(var10));
                        }
                     }
                  }
               }
            } catch (RuntimeException | IOException var13) {
               LOGGER.warn("[EFN] Failed to load the proxy list; starting empty", (Throwable)var13);
               var2.clear();
               this.setAside(var1);
            }
         }

         this.proxies.addAll(var2);
         this.keepOneEnabled();
         this.loaded = true;
      }
   }

   private void setAside(Path path) {
      try {
         Files.move(path, path.resolveSibling(path.getFileName() + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException var3) {
         LOGGER.warn("[EFN] Couldn't move the broken proxy list aside; not saving proxies this session", (Throwable)var3);
         this.readOnly = true;
      }
   }

   private static Proxy readEntry(JsonObject jsonObject) {
      Proxy var3 = new Proxy();
      var3.name = string(jsonObject, "name");
      ProxyType var4 = ProxyType.parse(string(jsonObject, "type"));
      var3.type = var4 == null ? ProxyType.SOCKS5 : var4;
      var3.address = string(jsonObject, "address");
      var3.port = integer(jsonObject, "port");
      var3.username = string(jsonObject, "username");
      var3.password = string(jsonObject, "password");
      JsonElement var5 = jsonObject.get("enabled");
      JsonPrimitive var2;
      var3.enabled = var5 instanceof JsonPrimitive && (var2 = (JsonPrimitive)var5).isBoolean() && var2.getAsBoolean();
      var5 = jsonObject.get("ping");
      JsonPrimitive var1;
      if (var5 instanceof JsonPrimitive && (var1 = (JsonPrimitive)var5).isNumber()) {
         var3.ping = Math.max(-1, var1.getAsInt());
      }

      return var3;
   }

   private static String string(JsonObject jsonObject, String string) {
      String var2;
      if (jsonObject.get(string) instanceof JsonPrimitive var4) {
         var2 = var4.getAsString();
      } else {
         var2 = "";
      }

      return var2;
   }

   private static int integer(JsonObject jsonObject, String string) {
      JsonElement var3 = jsonObject.get(string);
      JsonPrimitive var2;
      return var3 instanceof JsonPrimitive && (var2 = (JsonPrimitive)var3).isNumber() ? var2.getAsInt() : 0;
   }

   private void save() {
      if (!this.readOnly) {
         JsonObject var1 = new JsonObject();
         var1.addProperty("version", 1);
         JsonArray var2 = new JsonArray();

         for (Proxy var4 : this.proxies) {
            JsonObject var5 = new JsonObject();
            var5.addProperty("name", var4.name == null ? "" : var4.name);
            var5.addProperty("type", (var4.type == null ? ProxyType.SOCKS5 : var4.type).name());
            var5.addProperty("address", var4.address == null ? "" : var4.address.trim());
            var5.addProperty("port", var4.port);
            if (var4.username != null && !var4.username.isEmpty()) {
               var5.addProperty("username", var4.username);
            }

            if (var4.password != null && !var4.password.isEmpty()) {
               var5.addProperty("password", var4.password);
            }

            var5.addProperty("enabled", var4.enabled);
            if (var4.ping != -2) {
               var5.addProperty("ping", var4.ping);
            }

            var2.add(var5);
         }

         var1.add("proxies", var2);
         Path var12 = configPath();

         try {
            Files.createDirectories(var12.getParent());
            Path var13 = var12.resolveSibling(var12.getFileName().toString() + ".tmp");

            try (BufferedWriter var14 = Files.newBufferedWriter(var13)) {
               GSON.toJson((JsonElement)var1, var14);
            }

            try {
               Files.move(var13, var12, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException var9) {
               Files.move(var13, var12, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException var11) {
            LOGGER.warn("[EFN] Failed to save the proxy list", (Throwable)var11);
         }
      }
   }

   private static Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("proxies.json");
   }
}
