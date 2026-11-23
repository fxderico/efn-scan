package net.efn.client.util.spotify;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class LyricsMatcher {
   static final double DURATION_TOLERANCE_SECONDS = 3.0;
   private static final int TITLE_EXACT = 40;
   private static final int TITLE_BASE = 30;
   private static final int TITLE_CONTAINS = 12;
   private static final int ARTIST_EXACT = 30;
   private static final int ARTIST_SHARED = 20;
   private static final int ARTIST_CONTAINS = 10;
   private static final int DURATION_MAX = 20;
   private static final int SYNCED = 25;
   private static final int INSTRUMENTAL = 8;
   private static final int PLAIN = 5;
   private static final Pattern MARKS = Pattern.compile("\\p{M}+");
   private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");
   private static final Pattern APOSTROPHES = Pattern.compile("['\u2019`\u00b4]");
   private static final Pattern BRACKETED = Pattern.compile(
      "\\s*[(\\[][^)\\]]*\\b(feat|ft|featuring|with|remaster(ed)?|explicit|clean|mono|stereo|bonus|deluxe|anniversary|edition|single version|album version)\\b[^)\\]]*[)\\]]",
      2
   );
   private static final Pattern DASHED = Pattern.compile(
      "\\s+[-\u2013\u2014]\\s+[^-\u2013\u2014]*\\b(remaster(ed)?|explicit|clean|mono|stereo|bonus track|deluxe|anniversary|edition|single version|album version)\\b.*$",
      2
   );
   private static final Pattern FEATURING = Pattern.compile("\\s+(feat\\.?|ft\\.|featuring)\\s+.*$", 2);
   private static final Pattern ARTIST_SPLIT = Pattern.compile(
      "\\s*(?:,|;|/|&|\u2022|\\+|\\bx\\b|\\band\\b|\\bwith\\b|\\bfeat\\b\\.?|\\bft\\b\\.?|\\bfeaturing\\b)\\s*", 2
   );
   private static final Pattern CREDIT_SPLIT = Pattern.compile("\\s*[,;\u2022]\\s*|\\s+(?:&|x|feat\\.?|ft\\.?|featuring|with)\\s+", 2);

   private LyricsMatcher() {
   }

   static LrclibTrack best(List<LrclibTrack> list, LyricsQuery lyricsQuery) {
      if (list != null && !list.isEmpty() && lyricsQuery != null) {
         LrclibTrack var2 = null;
         int var3 = -1;
         double var4 = 1.7976931348623157E308;

         for (LrclibTrack var7 : list) {
            int var8;
            if (var7 != null && (var8 = score(var7, lyricsQuery)) >= 0) {
               double var9 = drift(var7, lyricsQuery);
               if (var8 > var3 || var8 == var3 && var9 < var4) {
                  var2 = var7;
                  var3 = var8;
                  var4 = var9;
               }
            }
         }

         return var2;
      } else {
         return null;
      }
   }

   static int score(LrclibTrack lrclibTrack, LyricsQuery lyricsQuery) {
      if (!lrclibTrack.usable()) {
         return -1;
      } else {
         int var2 = durationScore(lrclibTrack, lyricsQuery);
         if (var2 < 0) {
            return -1;
         } else {
            int var3 = titleScore(lyricsQuery.title(), lrclibTrack.trackName());
            if (var3 <= 0) {
               return -1;
            } else {
               int var4 = artistScore(lyricsQuery.artist(), lrclibTrack.artistName());
               if (var4 <= 0) {
                  return -1;
               } else {
                  int var5 = lrclibTrack.hasSynced() ? 25 : (lrclibTrack.hasPlain() ? 5 : 8);
                  return var3 + var4 + var2 + var5;
               }
            }
         }
      }
   }

   private static int durationScore(LrclibTrack lrclibTrack, LyricsQuery lyricsQuery) {
      if (lyricsQuery.hasDuration() && !(lrclibTrack.duration() <= 0.0)) {
         double var2 = Math.abs(lrclibTrack.duration() - lyricsQuery.durationSeconds());
         return var2 > 3.0 ? -1 : (int)Math.round(20.0 * (1.0 - var2 / 6.0));
      } else {
         return 0;
      }
   }

   private static double drift(LrclibTrack lrclibTrack, LyricsQuery lyricsQuery) {
      return lyricsQuery.hasDuration() && !(lrclibTrack.duration() <= 0.0) ? Math.abs(lrclibTrack.duration() - lyricsQuery.durationSeconds()) : 3.0;
   }

   static int titleScore(String string, String string2) {
      String var2 = normalize(string);
      String var3 = normalize(string2);
      if (var2.isEmpty() || var3.isEmpty()) {
         return 0;
      } else if (var2.equals(var3)) {
         return 40;
      } else {
         String var4 = normalize(baseTitle(string));
         String var5 = normalize(baseTitle(string2));
         if (!var4.isEmpty() && var4.equals(var5)) {
            return 30;
         } else {
            return !containsWords(var4, var5) && !containsWords(var5, var4) ? 0 : 12;
         }
      }
   }

   static int artistScore(String string, String string2) {
      String var3 = normalize(string);
      String var4 = normalize(string2);
      if (!var3.isEmpty() && !var4.isEmpty()) {
         if (var3.equals(var4)) {
            return 30;
         } else {
            Set var5 = artists(string);
            Set var6 = artists(string2);

            for (String var8 : var5) {
               if (var6.contains(var8)) {
                  return 20;
               }
            }

            String var2 = var5.isEmpty() ? var3 : (String)var5.iterator().next();
            return !containsWords(var4, var2) && !containsWords(var3, var4) ? 0 : 10;
         }
      } else {
         return 0;
      }
   }

   static String baseTitle(String string) {
      if (string == null) {
         return "";
      } else {
         String var1 = BRACKETED.matcher(string).replaceAll("");
         var1 = DASHED.matcher(var1).replaceFirst("");
         var1 = FEATURING.matcher(var1).replaceFirst("");
         String var4;
         return (var4 = var1.strip()).isEmpty() ? string.strip() : var4;
      }
   }

   static String primaryArtist(String string) {
      if (string == null) {
         return "";
      } else {
         for (String var5 : CREDIT_SPLIT.split(string.strip())) {
            if (!var5.isBlank()) {
               return var5.strip();
            }
         }

         return string.strip();
      }
   }

   static String normalize(String string) {
      if (string != null && !string.isEmpty()) {
         String var1 = string.toLowerCase(Locale.ROOT)
            .replace('\u0131', 'i')
            .replace("\u00df", "ss")
            .replace("\u00e6", "ae")
            .replace("\u0153", "oe")
            .replace('\u00f8', 'o')
            .replace('\u0142', 'l')
            .replace('\u0111', 'd')
            .replace("&", " and ");
         var1 = MARKS.matcher(Normalizer.normalize(var1, Form.NFD)).replaceAll("");
         var1 = APOSTROPHES.matcher(var1).replaceAll("");
         return NON_WORD.matcher(var1).replaceAll(" ").strip();
      } else {
         return "";
      }
   }

   private static Set<String> artists(String string) {
      LinkedHashSet var1 = new LinkedHashSet();

      for (String var5 : ARTIST_SPLIT.split(string)) {
         String var6 = normalize(var5);
         if (!var6.isEmpty()) {
            var1.add(var6);
         }
      }

      return var1;
   }

   private static boolean containsWords(String string, String string2) {
      return string2.length() >= 3 && string.length() >= string2.length() ? (" " + string + " ").contains(" " + string2 + " ") : false;
   }

   static List<String[]> searchTerms(LyricsQuery lyricsQuery) {
      ArrayList var1 = new ArrayList(2);
      var1.add(new String[]{lyricsQuery.title(), lyricsQuery.artist()});
      String var2 = baseTitle(lyricsQuery.title());
      String var3 = primaryArtist(lyricsQuery.artist());
      if (!var2.equals(lyricsQuery.title()) || !var3.equals(lyricsQuery.artist())) {
         var1.add(new String[]{var2, var3});
      }

      return var1;
   }
}
