package net.efn.client.util.spotify;

import java.io.IOException;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class MacSpotify extends ScriptedPlayer {
   private static final long TIMEOUT_MS = 2000L;
   private static final String APP_BINARY = "/Contents/MacOS/Spotify";
   private static final String NOT_RUNNING = "not running";
   private static final String STOPPED = "stopped";
   private static final char FIELD = '\u001f';
   private static final int FIELDS = 7;
   private static final String[] READ = new String[]{
      "osascript",
      "-e",
      "if application \"Spotify\" is not running then return \"not running\"",
      "-e",
      "tell application \"Spotify\"",
      "-e",
      "set s to (player state) as text",
      "-e",
      "if s is \"stopped\" then return \"stopped\"",
      "-e",
      "set t to current track",
      "-e",
      "set d to character id 31",
      "-e",
      "return s & d & (name of t) & d & (artist of t) & d & (album of t) & d & (duration of t) & d & (player position) & d & (artwork url of t)",
      "-e",
      "end tell"
   };

   MacSpotify() {
   }

   @Override
   protected ScriptedPlayer.Reading read() throws IOException, InterruptedException {
      if (!appRunning()) {
         return null;
      } else {
         String var1 = run(2000L, READ).strip();
         if (var1.equals("not running")) {
            return null;
         } else if (var1.equals("stopped")) {
            return new ScriptedPlayer.Reading(MediaSession.PlaybackState.STOPPED, "", "", "", 0L, 0L, "");
         } else {
            String[] var2 = var1.split(String.valueOf('\u001f'), -1);
            if (var2.length < 7) {
               throw new IOException("unexpected Spotify reply: " + var1);
            } else {
               MediaSession.PlaybackState var3 = var2[0].equals("playing") ? MediaSession.PlaybackState.PLAYING : MediaSession.PlaybackState.PAUSED;
               return new ScriptedPlayer.Reading(
                  var3,
                  var2[1].strip(),
                  var2[2].strip(),
                  var2[3].strip(),
                  Math.max(0L, (long)number(var2[4])),
                  Math.max(0L, Math.round(number(var2[5]) * 1000.0)),
                  var2[6].strip()
               );
            }
         }
      }
   }

   @Override
   protected boolean send(ScriptedPlayer.Command var1) throws IOException, InterruptedException {
      if (!appRunning()) {
         return false;
      } else {
         String var2 = switch (var1) {
            case PLAY_PAUSE -> "playpause";
            case NEXT -> "next track";
            case PREVIOUS -> "previous track";
         };
         run(2000L, new String[]{"osascript", "-e", "if application \"Spotify\" is running then tell application \"Spotify\" to " + var2});
         return true;
      }
   }

   private static boolean appRunning() {
      return ProcessHandle.allProcesses().anyMatch(var0 -> {
         Optional var1 = var0.info().command();
         return var1.isPresent() && ((String)var1.get()).endsWith("/Contents/MacOS/Spotify");
      });
   }

   private static double number(String var0) {
      try {
         return Double.parseDouble(var0.strip().replace(',', '.'));
      } catch (NumberFormatException var2) {
         return 0.0;
      }
   }
}
