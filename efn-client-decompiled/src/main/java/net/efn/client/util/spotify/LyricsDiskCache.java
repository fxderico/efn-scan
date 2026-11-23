package net.efn.client.util.spotify;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class LyricsDiskCache {
   private static final int FORMAT = 1;
   private static final String SUFFIX = ".json";
   private static final Pattern KEY = Pattern.compile("[0-9a-f]{16,64}");
   private static final long MAX_FILE_BYTES = 524288L;
   private final Path dir;
   private final int maxFiles;
   private final long maxBytes;

   LyricsDiskCache(Path path, int i, long l) {
      this.dir = path;
      this.maxFiles = i;
      this.maxBytes = l;
   }

   LyricsDiskCache.Entry read(String string) {
      Path var2 = this.file(string);
      if (var2 == null) {
         return null;
      } else {
         try {
            if (Files.size(var2) > 524288L) {
               return null;
            } else {
               JsonElement var3 = JsonParser.parseString(Files.readString(var2, StandardCharsets.UTF_8));
               if (!var3.isJsonObject()) {
                  return null;
               } else {
                  JsonObject var4 = var3.getAsJsonObject();
                  if (intValue(var4, "format") != 1) {
                     return null;
                  } else {
                     long var5 = longValue(var4, "fetchedAt");
                     if (var5 <= 0L) {
                        return null;
                     } else {
                        LrclibTrack var7 = var4.has("track") ? LrclibTrack.fromJson(var4.get("track")) : null;
                        touch(var2);
                        return new LyricsDiskCache.Entry(var7, var5);
                     }
                  }
               }
            }
         } catch (IOException | IllegalStateException | UnsupportedOperationException | JsonParseException var8) {
            return null;
         }
      }
   }

   void write(String string, LrclibTrack lrclibTrack, long l) {
      Path var5 = this.file(string);
      if (var5 != null) {
         JsonObject var6 = new JsonObject();
         var6.addProperty("format", 1);
         var6.addProperty("fetchedAt", l);
         if (lrclibTrack != null) {
            var6.add("track", lrclibTrack.toJson());
         }

         Path var7 = var5.resolveSibling(var5.getFileName() + ".tmp");

         try {
            Files.createDirectories(this.dir);
            Files.writeString(var7, var6.toString(), StandardCharsets.UTF_8);

            try {
               Files.move(var7, var5, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException var9) {
               Files.move(var7, var5, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException var10) {
            delete(var7);
         }
      }
   }

   void prune() {
      if (Files.isDirectory(this.dir)) {
         ArrayList var1 = new ArrayList();
         long var2 = 0L;

         try (DirectoryStream var4 = Files.newDirectoryStream(this.dir)) {
            for (Path var6 : var4) {
               String var8 = var6.getFileName().toString();
               if (var8.endsWith(".json.tmp")) {
                  delete(var6);
               } else {
                  BasicFileAttributes var7;
                  if (var8.endsWith(".json")
                     && KEY.matcher(var8.substring(0, var8.length() - ".json".length())).matches()
                     && (var7 = Files.readAttributes(var6, BasicFileAttributes.class)).isRegularFile()) {
                     var1.add(new LyricsDiskCache.Stored(var6, var7.size(), var7.lastModifiedTime().toMillis()));
                     var2 += var7.size();
                  }
               }
            }
         } catch (IOException var11) {
            return;
         }

         if (var1.size() > this.maxFiles || var2 > this.maxBytes) {
            var1.sort(Comparator.comparingLong(LyricsDiskCache.Stored::usedAt));
            int var12 = var1.size();

            for (LyricsDiskCache.Stored var14 : var1) {
               if (var12 <= this.maxFiles && var2 <= this.maxBytes) {
                  break;
               }

               if (delete(var14.file())) {
                  var12--;
                  var2 -= var14.size();
               }
            }
         }
      }
   }

   private Path file(String string) {
      return string != null && KEY.matcher(string).matches() ? this.dir.resolve(string + ".json") : null;
   }

   private static boolean delete(Path path) {
      try {
         Files.deleteIfExists(path);
         return true;
      } catch (IOException var2) {
         return false;
      }
   }

   private static void touch(Path path) {
      try {
         Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
      } catch (IOException var2) {
      }
   }

   private static int intValue(JsonObject jsonObject, String string) {
      JsonElement var3 = jsonObject.get(string);
      JsonPrimitive var2;
      return var3 instanceof JsonPrimitive && (var2 = (JsonPrimitive)var3).isNumber() ? var2.getAsInt() : -1;
   }

   private static long longValue(JsonObject jsonObject, String string) {
      JsonElement var3 = jsonObject.get(string);
      JsonPrimitive var2;
      return var3 instanceof JsonPrimitive && (var2 = (JsonPrimitive)var3).isNumber() ? var2.getAsLong() : -1L;
   }

   @Environment(EnvType.CLIENT)
   record Entry(LrclibTrack track, long fetchedAt) {
      boolean found() {
         return this.track != null;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Stored(Path file, long size, long usedAt) {
   }
}
