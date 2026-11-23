package net.efn.client.util.spotify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SyncedLyrics {
   public static final SyncedLyrics EMPTY = new SyncedLyrics(new SyncedLyrics.Line[0]);
   private static final Pattern TIMESTAMP = Pattern.compile("(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?");
   private static final Pattern WORD_TIMESTAMP = Pattern.compile("<(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?>");
   private static final Pattern METADATA = Pattern.compile("[A-Za-z#][A-Za-z0-9_ ]*:.*", 32);
   private static final Pattern OFFSET = Pattern.compile("offset\\s*:\\s*([+-]?\\d{1,7})\\s*");
   private static final Pattern WHITESPACE = Pattern.compile("\\s+");
   private static final long WORD_EARLY_MS = 1000L;
   private final SyncedLyrics.Line[] lines;
   private final long[] times;

   private SyncedLyrics(SyncedLyrics.Line[] lines) {
      this.lines = lines;
      this.times = new long[lines.length];

      for (int var2 = 0; var2 < lines.length; var2++) {
         this.times[var2] = lines[var2].timeMs();
      }
   }

   public static SyncedLyrics parse(String string) {
      if (string != null && !string.isBlank()) {
         ArrayList var1 = new ArrayList();
         ArrayList var2 = new ArrayList(4);
         int var3 = 0;

         for (String var7 : string.split("\r\n|\r|\n")) {
            String var9 = var7.strip();
            if (!var9.isEmpty() && var9.charAt(0) == '\ufeff') {
               var9 = var9.substring(1).strip();
            }

            var2.clear();
            int var10 = 0;

            int var8;
            while (var10 < var9.length() && var9.charAt(var10) == '[' && (var8 = var9.indexOf(93, var10 + 1)) >= 0) {
               String var11 = var9.substring(var10 + 1, var8).strip();
               Matcher var12 = TIMESTAMP.matcher(var11);
               if (var12.matches()) {
                  var2.add(toMillis(var12));
               } else {
                  if (!METADATA.matcher(var11).matches()) {
                     break;
                  }

                  Matcher var13 = OFFSET.matcher(var11.toLowerCase(Locale.ROOT));
                  if (var13.matches()) {
                     var3 = Integer.parseInt(var13.group(1));
                  }
               }

               var10 = var8 + 1;

               while (var10 < var9.length() && var9.charAt(var10) == ' ') {
                  var10++;
               }
            }

            if (!var2.isEmpty()) {
               String var22 = var9.substring(var10);
               String var23 = cleanText(var22);
               long var24 = Collections.min(var2);
               List var15 = wordTimes(var22, var23, var24);

               for (long var17 : var2) {
                  var1.add(new SyncedLyrics.Line(var17, var23, shift(var15, var17 - var24)));
               }
            }
         }

         if (var1.isEmpty()) {
            return EMPTY;
         } else {
            SyncedLyrics.Line[] var19 = var1.toArray(new SyncedLyrics.Line[0]);
            if (var3 != 0) {
               for (int var20 = 0; var20 < var19.length; var20++) {
                  SyncedLyrics.Line var21 = var19[var20];
                  var19[var20] = new SyncedLyrics.Line(Math.max(0L, var21.timeMs() - var3), var21.text(), shift(var21.wordTimes(), -var3));
               }
            }

            Arrays.sort(var19, Comparator.comparingLong(SyncedLyrics.Line::timeMs));
            return new SyncedLyrics(var19);
         }
      } else {
         return EMPTY;
      }
   }

   public int indexAt(long l) {
      int var3 = 0;
      int var4 = this.times.length;

      while (var3 < var4) {
         int var5 = var3 + var4 >>> 1;
         if (this.times[var5] <= l) {
            var3 = var5 + 1;
         } else {
            var4 = var5;
         }
      }

      return var3 - 1;
   }

   public int firstWithTimeOf(int i) {
      if (i >= 0 && i < this.times.length) {
         long var2 = this.times[i];

         while (i > 0 && this.times[i - 1] == var2) {
            i--;
         }

         return i;
      } else {
         return i;
      }
   }

   public int lastWithTimeOf(int i) {
      if (i >= 0 && i < this.times.length) {
         long var2 = this.times[i];

         while (i + 1 < this.times.length && this.times[i + 1] == var2) {
            i++;
         }

         return i;
      } else {
         return i;
      }
   }

   public SyncedLyrics.Line line(int i) {
      return this.lines[i];
   }

   public int size() {
      return this.lines.length;
   }

   public boolean isEmpty() {
      return this.lines.length == 0;
   }

   public List<String> texts() {
      ArrayList var1 = new ArrayList(this.lines.length);

      for (SyncedLyrics.Line var5 : this.lines) {
         if (!var5.isGap()) {
            var1.add(var5.text());
         }
      }

      return List.copyOf(var1);
   }

   private static long toMillis(Matcher matcher) {
      long var1 = Long.parseLong(matcher.group(1));
      long var3 = Long.parseLong(matcher.group(2));
      String var5 = matcher.group(3);
      long var6 = 0L;
      if (var5 != null) {
         var6 = Long.parseLong(var5);
         if (var5.length() == 1) {
            var6 *= 100L;
         } else if (var5.length() == 2) {
            var6 *= 10L;
         }
      }

      return var1 * 60000L + var3 * 1000L + var6;
   }

   private static String cleanText(String string) {
      if (string.indexOf(60) >= 0) {
         string = WORD_TIMESTAMP.matcher(string).replaceAll("");
      }

      return WHITESPACE.matcher(string).replaceAll(" ").strip();
   }

   private static List<Long> wordTimes(String string, String string2, long l) {
      if (string.indexOf(60) >= 0 && !string2.isEmpty()) {
         ArrayList var4 = new ArrayList();
         Matcher var5 = WORD_TIMESTAMP.matcher(string);
         long var6 = -1L;
         boolean var8 = false;
         int var9 = 0;

         while (var9 < string.length()) {
            if (string.charAt(var9) == '<' && var5.region(var9, string.length()).lookingAt()) {
               var9 = var5.end();
               boolean var11 = var8 && var9 < string.length() && string.charAt(var9) > ' ';
               if (!var11) {
                  var6 = toMillis(var5);
               }
            } else if (string.charAt(var9++) <= ' ') {
               var8 = false;
            } else if (!var8) {
               var8 = true;
               if (var6 < 0L) {
                  return List.of();
               }

               if (var4.isEmpty() ? var6 < l - 1000L : var6 < (Long)var4.getLast()) {
                  return List.of();
               }

               var4.add(var6);
               var6 = -1L;
            }
         }

         int var10 = 1;

         for (int var12 = 0; var12 < string2.length(); var12++) {
            if (string2.charAt(var12) == ' ') {
               var10++;
            }
         }

         return (List<Long>)(var4.size() == var10 ? var4 : List.of());
      } else {
         return List.of();
      }
   }

   private static List<Long> shift(List<Long> list, long l) {
      if (l != 0L && !list.isEmpty()) {
         ArrayList var3 = new ArrayList(list.size());

         for (long var5 : list) {
            var3.add(Math.max(0L, var5 + l));
         }

         return var3;
      } else {
         return list;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Line(long timeMs, String text, List<Long> wordTimes) {
      public Line(long timeMs, String text, List<Long> wordTimes) {
         text = text == null ? "" : text;
         wordTimes = wordTimes == null ? List.of() : List.copyOf(wordTimes);
         this.timeMs = timeMs;
         this.text = text;
         this.wordTimes = wordTimes;
      }

      public boolean isGap() {
         return this.text.isEmpty();
      }
   }
}
