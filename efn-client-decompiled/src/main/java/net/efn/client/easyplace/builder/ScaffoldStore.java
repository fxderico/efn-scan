package net.efn.client.easyplace.builder;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.efn.client.easyplace.EasyPlace;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
final class ScaffoldStore {
   private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(runnable -> {
      Thread var1 = new Thread(runnable, "EasyPlace scaffold list");
      var1.setDaemon(true);
      return var1;
   });
   private final Object lock = new Object();
   private int savedHash;
   private String savedWorld;

   ScaffoldStore() {
   }

   private static Path file() {
      return MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve("easyplace-builder-scaffold.json");
   }

   static String worldKey(MinecraftClient minecraftClient) {
      if (minecraftClient.world == null) {
         return null;
      } else {
         String var1 = minecraftClient.getServer() != null
            ? "local:" + minecraftClient.getServer().getSaveProperties().getLevelName()
            : (minecraftClient.getCurrentServerEntry() != null ? minecraftClient.getCurrentServerEntry().address.toLowerCase(Locale.ROOT) : "unknown");
         return var1 + " " + minecraftClient.world.getRegistryKey();
      }
   }

   Set<Long> load(String string) {
      HashSet var2 = new HashSet();
      Object var3 = this.lock;
      synchronized (var3) {
         JsonObject var5 = read();
         JsonElement var6 = var5.get(string);
         if (var6 instanceof JsonArray) {
            for (JsonElement var9 : (JsonArray)var6) {
               var2.add(var9.getAsLong());
            }
         }
      }

      this.savedWorld = string;
      this.savedHash = var2.hashCode();
      return var2;
   }

   void save(String string, Set<Long> set) {
      if (string != null) {
         int var3 = set.hashCode();
         if (!string.equals(this.savedWorld) || var3 != this.savedHash) {
            this.savedWorld = string;
            this.savedHash = var3;
            long[] var4 = set.stream().mapToLong(Long::longValue).toArray();
            WRITER.execute(() -> this.write(string, var4));
         }
      }
   }

   private void write(String string, long[] ls) {
      Object var3 = this.lock;
      synchronized (var3) {
         JsonObject var5 = read();
         if (ls.length == 0) {
            var5.remove(string);
         } else {
            JsonArray var6 = new JsonArray();

            for (long var10 : ls) {
               var6.add(var10);
            }

            var5.add(string, var6);
         }

         try {
            Path var15 = file();
            Files.createDirectories(var15.getParent());
            Path var16 = var15.resolveSibling(var15.getFileName() + ".tmp");
            Files.writeString(var16, new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement)var5), StandardCharsets.UTF_8);
            Files.move(var16, var15, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException var13) {
            EasyPlace.LOG.warn("Could not save the builder's scaffold list", (Throwable)var13);
         }
      }
   }

   private static JsonObject read() {
      Path var0 = file();
      if (!Files.isRegularFile(var0)) {
         return new JsonObject();
      } else {
         try {
            JsonElement var1 = JsonParser.parseString(Files.readString(var0, StandardCharsets.UTF_8));
            return var1.isJsonObject() ? var1.getAsJsonObject() : new JsonObject();
         } catch (RuntimeException | IOException var2) {
            EasyPlace.LOG.warn("Could not read the builder's scaffold list", (Throwable)var2);
            return new JsonObject();
         }
      }
   }
}
