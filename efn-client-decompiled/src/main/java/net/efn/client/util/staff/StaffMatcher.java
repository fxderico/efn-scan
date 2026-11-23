package net.efn.client.util.staff;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class StaffMatcher {
   public static final String DEFAULT_RANK_WORDS = "owner, co-owner, manager, admin, administrator, developer, dev, sr-admin, sr-mod, senior-mod, moderator, mod, trial-mod, sr-helper, senior-helper, helper, trial-helper, support, staff";
   public static final String DEFAULT_MARKERS = "U+2605 U+2606 U+2726 U+2727 U+272A U+272F U+2730 U+2B50";
   public static final String MODE_ALL = "Ranks + Markers + Team";
   public static final String MODE_RANKS = "Ranks";
   public static final String MODE_MARKERS = "Markers";
   public static final String MODE_TEAM = "Team Rank";
   public static final int DEFAULT_TEAM_RANK_MAX = 5;
   private static final String GENERIC_RANK = "Staff";
   private static final Pattern CODEPOINT = Pattern.compile("(?i)(?:u\\+|\\\\u)([0-9a-f]{4,6})");
   private static final Pattern TEAM_RANK = Pattern.compile("^(\\d{2})-");
   private static final int MAX_SPAN = 3;
   private static final int TEAM_RANK_SLOTS = 100;
   private static final Map<String, String> LABELS = Map.ofEntries(
      Map.entry("owner", "Owner"),
      Map.entry("coowner", "Co-Owner"),
      Map.entry("manager", "Manager"),
      Map.entry("admin", "Admin"),
      Map.entry("administrator", "Admin"),
      Map.entry("sradmin", "Sr. Admin"),
      Map.entry("developer", "Dev"),
      Map.entry("dev", "Dev"),
      Map.entry("srmod", "Sr. Mod"),
      Map.entry("seniormod", "Sr. Mod"),
      Map.entry("moderator", "Mod"),
      Map.entry("mod", "Mod"),
      Map.entry("trialmod", "Trial Mod"),
      Map.entry("srhelper", "Sr. Helper"),
      Map.entry("seniorhelper", "Sr. Helper"),
      Map.entry("helper", "Helper"),
      Map.entry("trialhelper", "Trial Helper"),
      Map.entry("support", "Support"),
      Map.entry("staff", "Staff"),
      Map.entry("builder", "Builder")
   );
   private static final Map<Integer, Character> SMALL_CAPS = smallCaps();
   private static final char SMALL_CAPS_Q = '\u01eb';
   private final boolean useRanks;
   private final boolean useMarkers;
   private final boolean useTeamRank;
   private final int teamRankMax;
   private final Map<String, Integer> wordIndex = new HashMap<>();
   private final List<StaffMatcher.Word> words = new ArrayList<>();
   private final Set<Integer> markers;
   private final boolean iconGlyphs;
   private final Set<String> knownNames;
   private final Set<String> ignoredNames;

   public StaffMatcher(String var1, String var2, String var3, boolean var4, int var5, String var6, String var7) {
      this.useRanks = "Ranks + Markers + Team".equals(var1) || "Ranks".equals(var1);
      this.useMarkers = "Ranks + Markers + Team".equals(var1) || "Markers".equals(var1);
      this.useTeamRank = "Ranks + Markers + Team".equals(var1) || "Team Rank".equals(var1);
      this.teamRankMax = var5;
      this.iconGlyphs = var4;

      for (String var11 : var2.split(",")) {
         String var12 = var11.trim();
         String var13 = fold(var12).replace(" ", "");
         if (!var13.isEmpty() && !this.wordIndex.containsKey(var13)) {
            this.wordIndex.put(var13, this.words.size());
            this.words.add(new StaffMatcher.Word(var13, LABELS.getOrDefault(var13, capitalize(var12))));
         }
      }

      this.markers = parseMarkers(var3);
      this.knownNames = parseNames(var6);
      this.ignoredNames = parseNames(var7);
   }

   public StaffMatcher.Match match(String var1, String var2, String... var3) {
      if (var1 != null && !var1.isEmpty()) {
         String var6 = var1.toLowerCase(Locale.ROOT);
         if (this.ignoredNames.contains(var6)) {
            return null;
         } else {
            StringBuilder var7 = new StringBuilder();

            for (String var11 : var3) {
               if (var11 != null && !var11.isEmpty()) {
                  var7.append(var11).append(' ');
               }
            }

            if (var2 != null && !var2.isEmpty()) {
               var7.append(var2).append(' ');
            }

            String var12 = withoutName(stripLegacyCodes(var7.toString()), var1);
            StaffMatcher.Word var5 = this.useRanks ? this.bestRank(var12) : null;
            if (var5 != null) {
               return new StaffMatcher.Match(var5.label(), this.wordIndex.get(var5.folded()));
            } else {
               int var4 = this.useTeamRank ? teamRank(var2) : -1;
               if (var4 >= 0 && var4 <= this.teamRankMax) {
                  return new StaffMatcher.Match(teamRankLabel(var4), this.words.size() + var4);
               } else {
                  return (!this.useMarkers || !this.hasMarker(var12)) && !this.knownNames.contains(var6)
                     ? null
                     : new StaffMatcher.Match("Staff", this.words.size() + 100);
               }
            }
         }
      } else {
         return null;
      }
   }

   public static int teamRank(String var0) {
      if (var0 != null && var0.length() >= 3) {
         Matcher var1 = TEAM_RANK.matcher(var0);
         return var1.lookingAt() ? Integer.parseInt(var1.group(1)) : -1;
      } else {
         return -1;
      }
   }

   public static String teamRankLabel(int var0) {
      return var0 < 10 ? "0" + var0 : Integer.toString(var0);
   }

   private StaffMatcher.Word bestRank(String var1) {
      if (this.words.isEmpty()) {
         return null;
      } else {
         List var3 = tokens(var1);
         int var4 = 2147483647;
         int var5 = 0;

         while (var5 < var3.size()) {
            int var2 = 0;

            for (int var6 = Math.min(3, var3.size() - var5); var6 >= 1; var6--) {
               Integer var7 = this.wordIndex.get(String.join("", var3.subList(var5, var5 + var6)));
               if (var7 != null) {
                  var4 = Math.min(var4, var7);
                  var2 = var6;
                  break;
               }
            }

            var5 += Math.max(1, var2);
         }

         return var4 == 2147483647 ? null : this.words.get(var4);
      }
   }

   static List<String> tokens(String var0) {
      String var1 = fold(var0);
      ArrayList var2 = new ArrayList();
      StringBuilder var3 = new StringBuilder();

      for (int var4 = 0; var4 < var1.length(); var4++) {
         char var5 = var1.charAt(var4);
         if (var5 >= 'a' && var5 <= 'z') {
            var3.append(var5);
         } else if (!var3.isEmpty()) {
            var2.add(var3.toString());
            var3.setLength(0);
         }
      }

      if (!var3.isEmpty()) {
         var2.add(var3.toString());
      }

      return var2;
   }

   static String stripLegacyCodes(String var0) {
      if (var0.indexOf(167) < 0) {
         return var0;
      } else {
         StringBuilder var1 = new StringBuilder(var0.length());

         for (int var2 = 0; var2 < var0.length(); var2++) {
            char var3 = var0.charAt(var2);
            if (var3 == 167) {
               var2++;
            } else {
               var1.append(var3);
            }
         }

         return var1.toString();
      }
   }

   static String fold(String var0) {
      String var2 = Normalizer.normalize(var0.replace('\u01eb', 'q'), Form.NFKD);
      StringBuilder var3 = new StringBuilder(var2.length());
      int var4 = 0;

      while (var4 < var2.length()) {
         int var1 = var2.codePointAt(var4);
         if (Character.getType(var1) != 6) {
            Character var5 = SMALL_CAPS.get(var1);
            if (var5 != null) {
               var3.append(var5.charValue());
            } else {
               int var6 = Character.toLowerCase(var1);
               var3.append(var6 >= 97 && var6 <= 122 ? (char)var6 : ' ');
            }
         }

         var4 += Character.charCount(var1);
      }

      return var3.toString();
   }

   private boolean hasMarker(String var1) {
      int var3 = 0;

      while (var3 < var1.length()) {
         int var2 = var1.codePointAt(var3);
         if (this.markers.contains(var2) || this.iconGlyphs && isPrivateUse(var2)) {
            return true;
         }

         var3 += Character.charCount(var2);
      }

      return false;
   }

   public static boolean isPrivateUse(int var0) {
      return var0 >= 57344 && var0 <= 63743 || var0 >= 983040 && var0 <= 1048573 || var0 >= 1048576 && var0 <= 1114109;
   }

   static Set<Integer> parseMarkers(String var0) {
      HashSet var1 = new HashSet();

      for (String var5 : var0.split("[,\\s]+")) {
         if (!var5.isEmpty()) {
            Matcher var6 = CODEPOINT.matcher(var5);
            if (var6.matches()) {
               int var7 = Integer.parseInt(var6.group(1), 16);
               if (Character.isValidCodePoint(var7)) {
                  var1.add(var7);
               }
            } else {
               var5.codePoints().forEach(var1::add);
            }
         }
      }

      return var1;
   }

   private static Set<String> parseNames(String var0) {
      HashSet var1 = new HashSet();

      for (String var5 : var0.split("[,\\s]+")) {
         if (!var5.isEmpty()) {
            var1.add(var5.toLowerCase(Locale.ROOT));
         }
      }

      return var1;
   }

   private static String withoutName(String var0, String var1) {
      String var3 = var0.toLowerCase(Locale.ROOT);
      String var2;
      if (!var3.contains(var2 = var1.toLowerCase(Locale.ROOT))) {
         return var0;
      } else {
         StringBuilder var4 = new StringBuilder(var0.length());
         int var5 = 0;

         for (int var6 = var3.indexOf(var2); var6 >= 0; var6 = var3.indexOf(var2, var5)) {
            var4.append(var0, var5, var6).append(' ');
            var5 = var6 + var2.length();
         }

         return var4.append(var0, var5, var0.length()).toString();
      }
   }

   private static String capitalize(String var0) {
      String var1 = var0.trim();
      return var1.isEmpty() ? "Staff" : var1.substring(0, 1).toUpperCase(Locale.ROOT) + var1.substring(1);
   }

   private static Map<Integer, Character> smallCaps() {
      int[] var0 = new int[]{
         7424, 665, 7428, 7429, 7431, 42800, 610, 668, 618, 7434, 7435, 671, 7437, 628, 7439, 7448, 42927, 640, 42801, 7451, 7452, 7456, 7457, 655, 7458
      };
      String var1 = "abcdefghijklmnopqrstuvwyz";
      HashMap var2 = new HashMap();

      for (int var3 = 0; var3 < var0.length; var3++) {
         var2.put(var0[var3], var1.charAt(var3));
      }

      return Map.copyOf(var2);
   }

   @Environment(EnvType.CLIENT)
   public record Match(String rank, int seniority) {
   }

   @Environment(EnvType.CLIENT)
   private record Word(String folded, String label) {
   }
}
