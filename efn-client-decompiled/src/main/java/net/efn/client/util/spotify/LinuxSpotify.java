package net.efn.client.util.spotify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class LinuxSpotify extends ScriptedPlayer {
   private static final long TIMEOUT_MS = 2000L;
   private static final long LOOKUP_GAP_MS = 2000L;
   private static final String BUS_PREFIX = "org.mpris.MediaPlayer2.spotify";
   private static final String OBJECT_PATH = "/org/mpris/MediaPlayer2";
   private static final String PLAYER = "org.mpris.MediaPlayer2.Player";
   private String busName = "org.mpris.MediaPlayer2.spotify";
   private long nextLookupAt;

   LinuxSpotify() {
   }

   @Override
   protected ScriptedPlayer.Reading read() throws IOException, InterruptedException {
      String var4 = this.busName(false);
      if (var4 == null) {
         return null;
      } else {
         String var3;
         try {
            var3 = run(
               2000L,
               new String[]{
                  "dbus-send",
                  "--session",
                  "--print-reply",
                  "--dest=" + var4,
                  "/org/mpris/MediaPlayer2",
                  "org.freedesktop.DBus.Properties.GetAll",
                  "string:org.mpris.MediaPlayer2.Player"
               }
            );
         } catch (ScriptedPlayer.CommandFailed var12) {
            if (!gone(var12)) {
               throw var12;
            }

            this.busName = null;
            return null;
         }

         if (!(LinuxSpotify.DbusReply.parse(var3) instanceof Map var6)) {
            throw new IOException("unexpected MPRIS reply");
         } else {
            Object var7 = var6.get("Metadata");
            Map var2;
            Map var8 = var7 instanceof Map ? (var2 = (Map)var7) : Map.of();
            String var10 = text(var6.get("PlaybackStatus"));

            MediaSession.PlaybackState var9 = switch (var10) {
               case "Playing" -> MediaSession.PlaybackState.PLAYING;
               case "Paused" -> MediaSession.PlaybackState.PAUSED;
               default -> MediaSession.PlaybackState.STOPPED;
            };
            return new ScriptedPlayer.Reading(
               var9,
               text(var8.get("xesam:title")),
               text(var8.get("xesam:artist")),
               text(var8.get("xesam:album")),
               micros(var8.get("mpris:length")),
               micros(var6.get("Position")),
               text(var8.get("mpris:artUrl"))
            );
         }
      }
   }

   @Override
   protected boolean send(ScriptedPlayer.Command var1) throws IOException, InterruptedException {
      String var2 = this.busName(true);
      if (var2 == null) {
         return false;
      } else {
         String var3 = switch (var1) {
            case PLAY_PAUSE -> "PlayPause";
            case NEXT -> "Next";
            case PREVIOUS -> "Previous";
         };

         try {
            run(
               2000L,
               new String[]{"dbus-send", "--session", "--print-reply", "--dest=" + var2, "/org/mpris/MediaPlayer2", "org.mpris.MediaPlayer2.Player." + var3}
            );
            return true;
         } catch (ScriptedPlayer.CommandFailed var5) {
            if (!gone(var5)) {
               throw var5;
            } else {
               this.busName = null;
               return false;
            }
         }
      }
   }

   private String busName(boolean var1) throws IOException, InterruptedException {
      if (this.busName != null) {
         return this.busName;
      } else {
         long var2 = System.currentTimeMillis();
         if (!var1 && var2 < this.nextLookupAt) {
            return null;
         } else {
            this.nextLookupAt = var2 + 2000L;
            String var4 = run(
               2000L,
               new String[]{
                  "dbus-send", "--session", "--print-reply", "--dest=org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus.ListNames"
               }
            );
            Object var5 = LinuxSpotify.DbusReply.parse(var4);
            if (var5 instanceof List) {
               for (Object var8 : (List)var5) {
                  String var9;
                  if (var8 instanceof String && (var9 = (String)var8).startsWith("org.mpris.MediaPlayer2.spotify")) {
                     this.busName = var9;
                     return var9;
                  }
               }
            }

            return null;
         }
      }
   }

   private static boolean gone(ScriptedPlayer.CommandFailed var0) {
      String var1 = var0.getMessage();
      return var1 != null && (var1.contains("ServiceUnknown") || var1.contains("NameHasNoOwner"));
   }

   private static String text(Object var0) {
      if (var0 instanceof String var6) {
         return var6.strip();
      } else if (var0 instanceof List var1) {
         ArrayList var2 = new ArrayList();

         for (Object var4 : var1) {
            String var5 = text(var4);
            if (!var5.isEmpty()) {
               var2.add(var5);
            }
         }

         return String.join(", ", var2);
      } else {
         return "";
      }
   }

   private static long micros(Object var0) {
      long var1;
      if (var0 instanceof Number var3) {
         var1 = Math.max(0L, var3.longValue() / 1000L);
      } else {
         var1 = 0L;
      }

      return var1;
   }

   @Environment(EnvType.CLIENT)
   static final class DbusReply {
      private final String[] lines;
      private int at;

      private DbusReply(String[] var1) {
         this.lines = var1;
      }

      static Object parse(String var0) {
         String[] var1 = var0.strip().split("\\R");
         LinuxSpotify.DbusReply var2 = new LinuxSpotify.DbusReply(var1);
         if (var1.length > 0 && var1[0].startsWith("method return")) {
            var2.at = 1;
         }

         return var2.at < var1.length ? var2.value(var2.next()) : null;
      }

      private String next() {
         return this.at < this.lines.length ? this.lines[this.at++].strip() : "]";
      }

      private Object value(String var1) {
         if (var1.startsWith("variant")) {
            var1 = var1.substring("variant".length()).strip();
         }

         if (var1.equals("array [")) {
            return this.array();
         } else if (!var1.startsWith("string ") && !var1.startsWith("object path ")) {
            int var2 = var1.lastIndexOf(32);
            String var3 = var2 < 0 ? var1 : var1.substring(0, var2);
            String var4 = var2 < 0 ? "" : var1.substring(var2 + 1);

            try {
               return switch (var3) {
                  case "int64", "uint64", "int32", "uint32", "int16", "uint16", "byte" -> Long.parseLong(var4);
                  case "double" -> Double.parseDouble(var4);
                  case "boolean" -> Boolean.parseBoolean(var4);
                  default -> var1;
               };
            } catch (NumberFormatException var7) {
               return var1;
            }
         } else {
            return unquote(var1);
         }
      }

      private Object array() {
         ArrayList var2 = new ArrayList();
         LinkedHashMap var3 = new LinkedHashMap();

         String var1;
         while (this.at < this.lines.length && !(var1 = this.next()).equals("]")) {
            if (var1.equals("dict entry(")) {
               Object var4 = this.value(this.next());
               Object var5 = this.value(this.next());
               this.next();
               if (var4 instanceof String var6) {
                  var3.put(var6, var5);
               }
            } else {
               var2.add(this.value(var1));
            }
         }

         return var3.isEmpty() ? var2 : var3;
      }

      private static String unquote(String var0) {
         int var1 = var0.indexOf(34);
         int var2 = var0.lastIndexOf(34);
         return var1 >= 0 && var2 > var1 ? var0.substring(var1 + 1, var2).replace("\\\"", "\"") : "";
      }
   }
}
