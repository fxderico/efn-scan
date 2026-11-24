package net.efn.client.util.spotify;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record Lyrics(Lyrics.Status status, SyncedLyrics synced, List<String> plainLines, long sourceId) {
   public static final Lyrics LOADING = new Lyrics(Lyrics.Status.LOADING, SyncedLyrics.EMPTY, List.of(), 0L);
   public static final Lyrics NOT_FOUND = new Lyrics(Lyrics.Status.NOT_FOUND, SyncedLyrics.EMPTY, List.of(), 0L);
   public static final Lyrics UNAVAILABLE = new Lyrics(Lyrics.Status.UNAVAILABLE, SyncedLyrics.EMPTY, List.of(), 0L);

   public Lyrics(Lyrics.Status status, SyncedLyrics synced, List<String> plainLines, long sourceId) {
      if (status == null) {
         status = Lyrics.Status.NOT_FOUND;
      }

      if (synced == null) {
         synced = SyncedLyrics.EMPTY;
      }

      plainLines = plainLines == null ? List.of() : List.copyOf(plainLines);
      this.status = status;
      this.synced = synced;
      this.plainLines = plainLines;
      this.sourceId = sourceId;
   }

   public static Lyrics of(LrclibTrack lrclibTrack) {
      if (lrclibTrack == null) {
         return NOT_FOUND;
      } else {
         SyncedLyrics var1 = SyncedLyrics.parse(lrclibTrack.syncedLyrics());
         List var2 = plainLines(lrclibTrack.plainLyrics());
         if (var2.isEmpty() && var1.isEmpty()) {
            var2 = plainLines(lrclibTrack.syncedLyrics());
         }

         if (!var1.isEmpty() && !var1.texts().isEmpty()) {
            return new Lyrics(Lyrics.Status.SYNCED, var1, var2.isEmpty() ? var1.texts() : var2, lrclibTrack.id());
         } else if (!var2.isEmpty()) {
            return new Lyrics(Lyrics.Status.PLAIN, SyncedLyrics.EMPTY, var2, lrclibTrack.id());
         } else {
            return !lrclibTrack.instrumental() && var1.size() <= 0
               ? NOT_FOUND
               : new Lyrics(Lyrics.Status.INSTRUMENTAL, SyncedLyrics.EMPTY, List.of(), lrclibTrack.id());
         }
      }
   }

   static List<String> plainLines(String string) {
      if (string != null && !string.isBlank()) {
         ArrayList var1 = new ArrayList();
         boolean var2 = false;

         for (String var6 : string.split("\r\n|\r|\n")) {
            String var7 = var6.strip();
            if (var7.isEmpty()) {
               var2 = !var1.isEmpty();
            } else {
               if (var2) {
                  var1.add("");
               }

               var2 = false;
               var1.add(var7);
            }
         }

         return List.copyOf(var1);
      } else {
         return List.of();
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Status {
      LOADING,
      SYNCED,
      PLAIN,
      INSTRUMENTAL,
      NOT_FOUND,
      UNAVAILABLE;

      private Status() {
      }
   }
}
