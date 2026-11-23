package net.efn.client.util.spotify;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
abstract class ScriptedPlayer implements PlayerSource {
   private static final long ART_TIMEOUT_MS = 3000L;
   private static final long ART_RETRY_MS = 10000L;
   private static final int ART_MAX_BYTES = 4194304;
   private volatile boolean closed;
   private volatile boolean unsupported;
   private volatile String lastError;
   private HttpClient http;
   private ExecutorService httpExecutor;
   private String trackKey;
   private long estimateMs;
   private long estimateAt;
   private boolean wasPlaying;
   private String artUrl = "";
   private long artRetryAt;
   private byte[] art;
   private int artVersion;

   ScriptedPlayer() {
   }

   protected abstract ScriptedPlayer.Reading read() throws IOException, InterruptedException;

   protected abstract boolean send(ScriptedPlayer.Command command) throws IOException, InterruptedException;

   @Override
   public MediaSession.Snapshot poll() {
      if (!this.closed && !this.unsupported) {
         try {
            ScriptedPlayer.Reading var1 = this.read();
            this.lastError = null;
            if (var1 == null) {
               this.forgetTrack();
               return null;
            }

            return this.snapshot(var1);
         } catch (ScriptedPlayer.MissingTool var2) {
            this.unsupported = true;
            this.lastError = var2.getMessage();
         } catch (IOException var3) {
            this.lastError = var3.getMessage();
         } catch (InterruptedException var4) {
            Thread.currentThread().interrupt();
         } catch (RuntimeException var5) {
            this.lastError = var5.toString();
         }

         return null;
      } else {
         return null;
      }
   }

   @Override
   public boolean togglePlayPause() {
      return this.command(ScriptedPlayer.Command.PLAY_PAUSE);
   }

   @Override
   public boolean skipNext() {
      return this.command(ScriptedPlayer.Command.NEXT);
   }

   @Override
   public boolean skipPrevious() {
      return this.command(ScriptedPlayer.Command.PREVIOUS);
   }

   @Override
   public String lastError() {
      return this.lastError;
   }

   @Override
   public boolean isUnsupported() {
      return this.unsupported;
   }

   @Override
   public void close() {
      this.closed = true;
      HttpClient var1 = this.http;
      ExecutorService var2 = this.httpExecutor;
      this.http = null;
      this.httpExecutor = null;
      if (var1 != null) {
         var1.shutdownNow();
      }

      if (var2 != null) {
         var2.shutdownNow();
      }
   }

   private boolean command(ScriptedPlayer.Command command) {
      if (!this.closed && !this.unsupported) {
         try {
            boolean var2 = this.send(command);
            this.lastError = null;
            return var2;
         } catch (ScriptedPlayer.MissingTool var3) {
            this.unsupported = true;
            this.lastError = var3.getMessage();
         } catch (IOException var4) {
            this.lastError = var4.getMessage();
         } catch (InterruptedException var5) {
            Thread.currentThread().interrupt();
         } catch (RuntimeException var6) {
            this.lastError = var6.toString();
         }

         return false;
      } else {
         return false;
      }
   }

   private MediaSession.Snapshot snapshot(ScriptedPlayer.Reading reading) {
      long var3 = System.currentTimeMillis();
      String var5 = reading.title() + "\n" + reading.artist() + "\n" + reading.album();
      boolean var6 = !var5.equals(this.trackKey);
      this.trackKey = var5;
      boolean var2 = reading.state() == MediaSession.PlaybackState.PLAYING;
      if (var6) {
         this.estimateMs = Math.max(0L, reading.positionMs());
      } else if (this.wasPlaying) {
         this.estimateMs = this.estimateMs + Math.max(0L, var3 - this.estimateAt);
      }

      this.estimateAt = var3;
      this.wasPlaying = var2;
      if (reading.positionMs() > 0L) {
         this.estimateMs = reading.positionMs();
      }

      long var8 = this.estimateMs;
      if (reading.durationMs() > 0L) {
         var8 = Math.min(var8, reading.durationMs());
      }

      this.updateArt(reading.artUrl(), var3);
      return new MediaSession.Snapshot(
         reading.title(),
         reading.artist(),
         reading.album(),
         reading.durationMs(),
         var8,
         var3,
         reading.state(),
         1.0,
         this.art,
         this.art == null ? "" : "image",
         this.artVersion
      );
   }

   private void forgetTrack() {
      this.trackKey = null;
      this.wasPlaying = false;
      this.estimateMs = 0L;
   }

   private void updateArt(String string, long l) {
      String var5 = string == null ? "" : string.strip();
      if (!var5.equals(this.artUrl) || this.art == null && !var5.isEmpty() && l >= this.artRetryAt) {
         this.artUrl = var5;
         byte[] var4 = var5.isEmpty() ? null : this.download(var5);
         if (var4 == null && !var5.isEmpty()) {
            this.artRetryAt = l + 10000L;
         }

         if (var4 != null || this.art != null) {
            this.art = var4;
            this.artVersion++;
         }
      }
   }

   private byte[] download(String string) {
      URI var3;
      try {
         var3 = URI.create(string);
      } catch (IllegalArgumentException var10) {
         return null;
      }

      String var2 = var3.getScheme() == null ? "" : var3.getScheme().toLowerCase(Locale.ROOT);
      if (!var2.equals("https") && !var2.equals("http")) {
         return null;
      } else {
         try {
            HttpRequest var5 = HttpRequest.newBuilder(var3).timeout(Duration.ofMillis(3000L)).GET().build();
            HttpResponse var6 = this.http().send(var5, BodyHandlers.ofByteArray());
            byte[] var7 = (byte[])var6.body();
            return var6.statusCode() == 200 && var7 != null && var7.length != 0 && var7.length <= 4194304 ? var7 : null;
         } catch (IOException var8) {
            this.lastError = "album art: " + var8.getMessage();
            return null;
         } catch (InterruptedException var9) {
            Thread.currentThread().interrupt();
            return null;
         }
      }
   }

   private HttpClient http() {
      if (this.http == null) {
         this.httpExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread var1 = new Thread(runnable, "EFN-Spotify-Art");
            var1.setDaemon(true);
            return var1;
         });
         this.http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(3000L)).followRedirects(Redirect.NORMAL).executor(this.httpExecutor).build();
      }

      return this.http;
   }

   static String run(long l, String... strings) throws IOException, InterruptedException {
      Process var3;
      try {
         var3 = new ProcessBuilder(strings).start();
      } catch (IOException var10) {
         throw new ScriptedPlayer.MissingTool(strings[0] + " is not available");
      }

      String var6;
      try {
         var3.getOutputStream().close();
         if (!var3.waitFor(l, TimeUnit.MILLISECONDS)) {
            throw new IOException(strings[0] + " did not answer within " + l + " ms");
         }

         String var4 = readAll(var3.getInputStream());
         if (var3.exitValue() != 0) {
            String var5 = readAll(var3.getErrorStream()).strip();
            throw new ScriptedPlayer.CommandFailed(var5.isEmpty() ? strings[0] + " exited with " + var3.exitValue() : var5);
         }

         var6 = var4;
      } finally {
         if (var3.isAlive()) {
            var3.destroyForcibly();
         }

         var3.getInputStream().close();
         var3.getErrorStream().close();
      }

      return var6;
   }

   private static String readAll(InputStream inputStream) throws IOException {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
   }

   @Environment(EnvType.CLIENT)
   static enum Command {
      PLAY_PAUSE,
      NEXT,
      PREVIOUS;

      private Command() {
      }
   }

   @Environment(EnvType.CLIENT)
   static class CommandFailed extends IOException {
      CommandFailed(String string) {
         super(string);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class MissingTool extends IOException {
      MissingTool(String string) {
         super(string);
      }
   }

   @Environment(EnvType.CLIENT)
   record Reading(MediaSession.PlaybackState state, String title, String artist, String album, long durationMs, long positionMs, String artUrl) {
   }
}
