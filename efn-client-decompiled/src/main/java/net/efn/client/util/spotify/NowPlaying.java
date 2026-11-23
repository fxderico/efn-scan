package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record NowPlaying(
   NowPlaying.Status status,
   String title,
   String artist,
   String album,
   NowPlaying.Source source,
   long durationMs,
   long positionMs,
   long sampledAtMs,
   double rate,
   AlbumArt art
) {
   private static final String SEPARATOR = " - ";
   static final long LYRICS_MIN_LENGTH_MS = 30000L;
   static final long LYRICS_MAX_LENGTH_MS = 3600000L;
   public static final NowPlaying UNSUPPORTED = idle(NowPlaying.Status.UNSUPPORTED);
   public static final NowPlaying WAITING = idle(NowPlaying.Status.WAITING);
   public static final NowPlaying NOT_RUNNING = idle(NowPlaying.Status.NOT_RUNNING);

   public NowPlaying(
      NowPlaying.Status status,
      String title,
      String artist,
      String album,
      NowPlaying.Source source,
      long durationMs,
      long positionMs,
      long sampledAtMs,
      double rate,
      AlbumArt art
   ) {
      if (status == null) {
         status = NowPlaying.Status.NOT_RUNNING;
      }

      if (source == null) {
         source = NowPlaying.Source.NONE;
      }

      title = title == null ? "" : title;
      artist = artist == null ? "" : artist;
      album = album == null ? "" : album;
      durationMs = Math.max(0L, durationMs);
      positionMs = durationMs > 0L ? Math.max(0L, Math.min(positionMs, durationMs)) : Math.max(0L, positionMs);
      if (!(rate > 0.0) || rate > 16.0) {
         rate = 1.0;
      }

      this.status = status;
      this.title = title;
      this.artist = artist;
      this.album = album;
      this.source = source;
      this.durationMs = durationMs;
      this.positionMs = positionMs;
      this.sampledAtMs = sampledAtMs;
      this.rate = rate;
      this.art = art;
   }

   public NowPlaying(NowPlaying.Status var1, String var2, String var3) {
      this(var1, var2, var3, "", NowPlaying.Source.WINDOW, 0L, 0L, 0L, 1.0, null);
   }

   private static NowPlaying idle(NowPlaying.Status var0) {
      return new NowPlaying(var0, "", "", "", NowPlaying.Source.NONE, 0L, 0L, 0L, 1.0, null);
   }

   public boolean playing() {
      return this.status == NowPlaying.Status.PLAYING;
   }

   public boolean hasTrack() {
      return !this.title.isEmpty();
   }

   public boolean active() {
      return this.status == NowPlaying.Status.PLAYING || this.status == NowPlaying.Status.PAUSED;
   }

   public boolean hasTimeline() {
      return this.source == NowPlaying.Source.SESSION && this.durationMs > 0L;
   }

   public boolean lyricsEligible() {
      if (!this.hasTimeline() || !this.hasTrack() || this.artist.isBlank()) {
         return false;
      } else {
         return this.durationMs >= 30000L && this.durationMs <= 3600000L
            ? !this.artist.equalsIgnoreCase("Spotify") && !this.title.equalsIgnoreCase("Advertisement")
            : false;
      }
   }

   public boolean sameTrack(NowPlaying var1) {
      return var1 != null && this.title.equals(var1.title) && this.artist.equals(var1.artist) && this.album.equals(var1.album);
   }

   public long positionAt(long var1) {
      if (!this.hasTimeline()) {
         return 0L;
      } else if (!this.playing()) {
         return this.positionMs;
      } else {
         long var3 = this.positionMs + (long)(Math.max(0L, var1 - this.sampledAtMs) * this.rate);
         return Math.min(var3, this.durationMs);
      }
   }

   public NowPlaying toggled() {
      long var1 = System.currentTimeMillis();

      return switch (this.status) {
         case PAUSED -> this.hasTrack()
            ? new NowPlaying(
               NowPlaying.Status.PLAYING, this.title, this.artist, this.album, this.source, this.durationMs, this.positionMs, var1, this.rate, this.art
            )
            : this;
         case PLAYING -> new NowPlaying(
            NowPlaying.Status.PAUSED, this.title, this.artist, this.album, this.source, this.durationMs, this.positionAt(var1), var1, this.rate, this.art
         );
         default -> this;
      };
   }

   public static NowPlaying fromSession(MediaSession.Snapshot var0, AlbumArt var1, NowPlaying var2) {
      NowPlaying.Status var3 = switch (var0.state()) {
         case PLAYING -> NowPlaying.Status.PLAYING;
         case CLOSED -> NowPlaying.Status.NOT_RUNNING;
         case CHANGING -> {
            if (var2 != null && var2.source == NowPlaying.Source.SESSION && var2.playing()) {
               yield NowPlaying.Status.PLAYING;
            } else {
               yield NowPlaying.Status.PAUSED;
            }
         }
         default -> NowPlaying.Status.PAUSED;
      };
      return var3 == NowPlaying.Status.NOT_RUNNING
         ? NOT_RUNNING
         : new NowPlaying(
            var3,
            var0.title(),
            var0.artist(),
            var0.album(),
            NowPlaying.Source.SESSION,
            var0.durationMs(),
            var0.positionMs(),
            var0.sampledAtMs(),
            var0.rate(),
            var1
         );
   }

   public static NowPlaying fromWindowTitle(String var0, NowPlaying var1) {
      if (var0 == null) {
         return NOT_RUNNING;
      } else {
         String var2 = var0.strip();
         if (var2.isEmpty()) {
            return NOT_RUNNING;
         } else {
            int var3 = var2.indexOf(" - ");
            if (var3 < 0 && (var2.equals("Spotify") || var2.startsWith("Spotify "))) {
               return var1 != null && var1.hasTrack()
                  ? new NowPlaying(NowPlaying.Status.PAUSED, var1.title, var1.artist)
                  : new NowPlaying(NowPlaying.Status.PAUSED, "", "");
            } else {
               return var3 > 0
                  ? new NowPlaying(NowPlaying.Status.PLAYING, var2.substring(var3 + " - ".length()).strip(), var2.substring(0, var3).strip())
                  : new NowPlaying(NowPlaying.Status.PLAYING, var2, "");
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Source {
      NONE,
      WINDOW,
      SESSION;

      private Source() {
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Status {
      UNSUPPORTED,
      WAITING,
      NOT_RUNNING,
      PAUSED,
      PLAYING;

      private Status() {
      }
   }
}
