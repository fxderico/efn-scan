package net.efn.client.util.spotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record LrclibTrack(
   long id, String trackName, String artistName, String albumName, double duration, boolean instrumental, String plainLyrics, String syncedLyrics
) {
   public LrclibTrack(
      long id, String trackName, String artistName, String albumName, double duration, boolean instrumental, String plainLyrics, String syncedLyrics
   ) {
      trackName = nonNull(trackName);
      artistName = nonNull(artistName);
      albumName = nonNull(albumName);
      plainLyrics = nonNull(plainLyrics);
      syncedLyrics = nonNull(syncedLyrics);
      if (!Double.isFinite(duration) || duration < 0.0) {
         duration = 0.0;
      }

      this.id = id;
      this.trackName = trackName;
      this.artistName = artistName;
      this.albumName = albumName;
      this.duration = duration;
      this.instrumental = instrumental;
      this.plainLyrics = plainLyrics;
      this.syncedLyrics = syncedLyrics;
   }

   public boolean hasSynced() {
      return !this.syncedLyrics.isBlank();
   }

   public boolean hasPlain() {
      return !this.plainLyrics.isBlank();
   }

   public boolean usable() {
      return this.instrumental || this.hasSynced() || this.hasPlain();
   }

   public static LrclibTrack fromJson(JsonElement var0) {
      if (var0 != null && var0.isJsonObject()) {
         JsonObject var1 = var0.getAsJsonObject();
         return new LrclibTrack(
            number(var1, "id", 0L),
            text(var1, "trackName"),
            text(var1, "artistName"),
            text(var1, "albumName"),
            number(var1, "duration", 0.0),
            bool(var1, "instrumental"),
            text(var1, "plainLyrics"),
            text(var1, "syncedLyrics")
         );
      } else {
         return null;
      }
   }

   public static List<LrclibTrack> listFromJson(JsonElement var0) {
      if (var0 != null && var0.isJsonArray()) {
         JsonArray var1 = var0.getAsJsonArray();
         ArrayList var2 = new ArrayList(var1.size());

         for (JsonElement var4 : var1) {
            LrclibTrack var5 = fromJson(var4);
            if (var5 != null) {
               var2.add(var5);
            }
         }

         return List.copyOf(var2);
      } else {
         return List.of();
      }
   }

   public static JsonElement parse(String var0) {
      return JsonParser.parseString(var0);
   }

   public JsonObject toJson() {
      JsonObject var1 = new JsonObject();
      var1.addProperty("id", this.id);
      var1.addProperty("trackName", this.trackName);
      var1.addProperty("artistName", this.artistName);
      var1.addProperty("albumName", this.albumName);
      var1.addProperty("duration", this.duration);
      var1.addProperty("instrumental", this.instrumental);
      var1.addProperty("plainLyrics", this.plainLyrics);
      var1.addProperty("syncedLyrics", this.syncedLyrics);
      return var1;
   }

   private static String nonNull(String var0) {
      return var0 == null ? "" : var0;
   }

   private static String text(JsonObject var0, String var1) {
      JsonElement var3 = var0.get(var1);
      JsonPrimitive var2;
      return var3 instanceof JsonPrimitive && (var2 = (JsonPrimitive)var3).isString() ? var2.getAsString() : "";
   }

   private static long number(JsonObject var0, String var1, long var2) {
      JsonElement var5 = var0.get(var1);
      JsonPrimitive var4;
      if (var5 instanceof JsonPrimitive && (var4 = (JsonPrimitive)var5).isNumber()) {
         try {
            return var4.getAsLong();
         } catch (NumberFormatException var7) {
            return var2;
         }
      } else {
         return var2;
      }
   }

   private static double number(JsonObject var0, String var1, double var2) {
      JsonElement var5 = var0.get(var1);
      JsonPrimitive var4;
      return var5 instanceof JsonPrimitive && (var4 = (JsonPrimitive)var5).isNumber() ? var4.getAsDouble() : var2;
   }

   private static boolean bool(JsonObject var0, String var1) {
      JsonElement var3 = var0.get(var1);
      JsonPrimitive var2;
      return var3 instanceof JsonPrimitive && (var2 = (JsonPrimitive)var3).isBoolean() && var2.getAsBoolean();
   }
}
