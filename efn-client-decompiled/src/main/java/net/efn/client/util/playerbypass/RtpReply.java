package net.efn.client.util.playerbypass;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record RtpReply(RtpReply.Kind kind, long millis, long nextUseMs) {
   public static final RtpReply NONE = new RtpReply(RtpReply.Kind.NONE, 0L);
   private static final Pattern FORMATTING = Pattern.compile("\u00a7.");
   private static final Pattern DURATION = Pattern.compile(
      "(\\d+(?:[.,]\\d+)?)\\s*(hours?|hrs?|saat|h|minutes?|mins?|dakika|dk|m|seconds?|secs?|saniye|sn|s)(?![a-z])"
   );
   private static final Pattern CLOCK = Pattern.compile("(?<![\\d:])(\\d{1,2}):(\\d{2})(?![\\d:])");
   private static final Pattern BRACKET_TIME = Pattern.compile("[\\[(<]\\s*\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:[ap]m)?\\s*[\\])>]");
   private static final Pattern LEADING_TIME = Pattern.compile("^\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s?[ap]m)?(?![\\d:])(?!\\s*(?:left|remaining|kaldi|kalan))");
   private static final Pattern SENTENCE = Pattern.compile("\\.(?!\\d)|[!?|;]");
   private static final String SMALL_CAPS = "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u01eb\u0280\u0455\ua731\u1d1b\u1d1c\u1d20\u1d21\u028f\u1d22";
   private static final String SMALL_CAPS_PLAIN = "abcdefghijklmnopqrsstuvwyz";
   private static final String TURKISH = "\u0131\u0130\u015f\u015e\u011f\u011e\u00fc\u00dc\u00f6\u00d6\u00e7\u00c7";
   private static final String TURKISH_PLAIN = "iissgguuoocc";
   private static final String CHAT_SEPARATORS = ":>\u00bb\u203a\u226b\u27a4\u279c\u2192\u21d2|";
   private static final String[] UNKNOWN = new String[]{
      "unknown command", "unknown or incomplete command", "bilinmeyen komut", "komut bulunamadi", "boyle bir komut yok"
   };
   private static final String[] NO_PERMISSION = new String[]{
      "no permission",
      "don't have permission",
      "dont have permission",
      "do not have permission",
      "not have permission",
      "insufficient permission",
      "not allowed to use",
      "yetkin yok",
      "yetkiniz yok",
      "iznin yok",
      "izniniz yok",
      "yetkiniz bulunmuyor",
      "izniniz bulunmuyor"
   };
   private static final String[] COMBAT = new String[]{"combat", "savasta", "savas modunda", "pvp modunda", "pvp'de", "pvpde", "kombat"};
   private static final String[] COMBAT_STRONG = new String[]{
      "in combat", "combat tag", "combat-tag", "combattag", "savasta", "savas modunda", "pvp modunda", "pvp'de", "pvpde"
   };
   private static final String[] COMBAT_OVER = new String[]{
      "no longer in combat", "out of combat", "left combat", "not in combat", "savastan ciktin", "artik savasta degil"
   };
   private static final String[] COOLDOWN_STRONG = new String[]{
      "cooldown", "cool down", "again", "another", "next teleport", "next rtp", "next use", "tekrar", "yeniden", "bekleme suresi"
   };
   private static final String[] COOLDOWN_WEAK = new String[]{"wait", "bekle", "before", "until", "remaining", "kalan", "sonra"};
   private static final String[] WARMUP = new String[]{
      "teleporting in",
      "teleporting you in",
      "teleport in",
      "teleported in",
      "teleportation in",
      "teleportation will",
      "teleport will",
      "will be teleported",
      "will teleport",
      "before teleporting",
      "before you teleport",
      "before being teleported",
      "don't move",
      "dont move",
      "do not move",
      "stand still",
      "isinlaniyor",
      "isinlanacak",
      "isinlanmaya",
      "hareket etme"
   };
   private static final String[] PROGRESS = new String[]{"teleporting", "searching", "looking for a", "finding a", "araniyor", "konum bul"};
   private static final String[] CANCEL = new String[]{"cancel", "iptal", "you moved", "hareket ettin", "aborted"};

   public RtpReply(RtpReply.Kind var1, long var2) {
      this(var1, var2, -1L);
   }

   public static RtpReply parse(String var0) {
      String var1 = normalize(var0);
      if (var1.isEmpty()) {
         return NONE;
      } else if (containsAny(var1, UNKNOWN)) {
         return new RtpReply(RtpReply.Kind.UNKNOWN_COMMAND, 0L);
      } else if (containsAny(var1, NO_PERMISSION)) {
         return new RtpReply(RtpReply.Kind.NO_PERMISSION, 0L);
      } else {
         var1 = LEADING_TIME.matcher(BRACKET_TIME.matcher(var1).replaceAll(" ").trim()).replaceFirst("").trim();
         String[] var2 = SENTENCE.split(var1);
         long var3 = -1L;
         long var5 = -1L;
         long var7 = -1L;
         long var9 = -1L;

         for (String var14 : var2) {
            long var15 = durationMs(var14);
            if (var15 >= 0L) {
               if (var3 < 0L) {
                  var3 = var15;
               }

               if (containsAny(var14, COOLDOWN_STRONG)) {
                  if (var5 < 0L) {
                     var5 = var15;
                  }
               } else if (containsAny(var14, WARMUP)) {
                  if (var7 < 0L) {
                     var7 = var15;
                  }
               } else if (containsAny(var14, COOLDOWN_WEAK) && var9 < 0L) {
                  var9 = var15;
               }
            }
         }

         boolean var18 = containsAny(var1, WARMUP);
         if (!containsAny(var1, COMBAT) || containsAny(var1, COMBAT_OVER) || var18 && (var7 >= 0L || !containsAny(var1, COMBAT_STRONG))) {
            boolean var19 = containsAny(var1, CANCEL);
            if (var3 < 0L) {
               if (var19) {
                  return new RtpReply(RtpReply.Kind.CANCELLED, 0L);
               } else {
                  return !var18 && !containsAny(var1, PROGRESS) ? NONE : new RtpReply(RtpReply.Kind.WARMUP, 0L);
               }
            } else if (var7 >= 0L && var5 >= 0L) {
               return new RtpReply(RtpReply.Kind.WARMUP, var7, var5);
            } else if (containsAny(var1, COOLDOWN_STRONG)) {
               return new RtpReply(RtpReply.Kind.COOLDOWN, var5 >= 0L ? var5 : var3);
            } else if (var18) {
               return new RtpReply(RtpReply.Kind.WARMUP, var7 >= 0L ? var7 : var3);
            } else if (var19) {
               return new RtpReply(RtpReply.Kind.CANCELLED, 0L);
            } else {
               return containsAny(var1, COOLDOWN_WEAK) ? new RtpReply(RtpReply.Kind.COOLDOWN, var9 >= 0L ? var9 : var3) : NONE;
            }
         } else {
            return new RtpReply(RtpReply.Kind.COMBAT, 0L);
         }
      }
   }

   public static long durationMs(String var0) {
      Matcher var1 = DURATION.matcher(var0);
      double var2 = 0.0;
      boolean var4 = false;

      while (var1.find()) {
         double var5;
         try {
            var5 = Double.parseDouble(var1.group(1).replace(',', '.'));
         } catch (NumberFormatException var8) {
            continue;
         }

         var2 += var5 * unitMs(var1.group(2));
         var4 = true;
      }

      if (var4) {
         return Math.round(var2);
      } else {
         Matcher var9 = CLOCK.matcher(var0);
         return var9.find() ? (Integer.parseInt(var9.group(1)) * 60L + Integer.parseInt(var9.group(2))) * 1000L : -1L;
      }
   }

   private static long unitMs(String var0) {
      return switch (var0) {
         case "h", "hr", "hrs", "hour", "hours", "saat" -> 3600000L;
         case "m", "min", "mins", "minute", "minutes", "dakika", "dk" -> 60000L;
         default -> 1000L;
      };
   }

   public static boolean looksLikePlayerChat(String var0, Iterable<String> var1) {
      String var2 = normalize(var0);
      if (var2.isEmpty()) {
         return false;
      } else {
         for (String var4 : var1) {
            if (var4 != null && !var4.isBlank()) {
               String var5 = var4.toLowerCase(Locale.ROOT);

               for (int var6 = var2.indexOf(var5); var6 >= 0; var6 = var2.indexOf(var5, var6 + 1)) {
                  int var7 = var6 + var5.length();
                  if ((var6 <= 0 || !nameChar(var2.charAt(var6 - 1))) && (var7 >= var2.length() || !nameChar(var2.charAt(var7))) && separatorAfter(var2, var7)) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private static boolean nameChar(char var0) {
      return var0 >= 'a' && var0 <= 'z' || var0 >= '0' && var0 <= '9' || var0 == '_';
   }

   private static boolean separatorAfter(String var0, int var1) {
      int var2 = var1;

      while (var2 < var0.length() && var2 - var1 < 3 && " ])}".indexOf(var0.charAt(var2)) >= 0) {
         var2++;
      }

      if (var2 >= var0.length()) {
         return false;
      } else {
         char var3 = var0.charAt(var2);
         return ":>\u00bb\u203a\u226b\u27a4\u279c\u2192\u21d2|".indexOf(var3) >= 0
            ? true
            : (var3 == '-' || var3 == '=') && var2 + 1 < var0.length() && var0.charAt(var2 + 1) == '>';
      }
   }

   public static String normalize(String var0) {
      if (var0 == null) {
         return "";
      } else {
         String var1 = FORMATTING.matcher(var0).replaceAll("");
         StringBuilder var2 = new StringBuilder(var1.length());

         for (int var3 = 0; var3 < var1.length(); var3++) {
            char var4 = var1.charAt(var3);
            int var5 = "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u01eb\u0280\u0455\ua731\u1d1b\u1d1c\u1d20\u1d21\u028f\u1d22"
               .indexOf(var4);
            int var6 = "\u0131\u0130\u015f\u015e\u011f\u011e\u00fc\u00dc\u00f6\u00d6\u00e7\u00c7".indexOf(var4);
            if (var5 >= 0) {
               var2.append("abcdefghijklmnopqrsstuvwyz".charAt(var5));
            } else if (var6 >= 0) {
               var2.append("iissgguuoocc".charAt(var6));
            } else if (var4 == 8217 || var4 == 8216) {
               var2.append('\'');
            } else if (var4 != 775) {
               var2.append(var4);
            }
         }

         return var2.toString().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
      }
   }

   private static boolean containsAny(String var0, String[] var1) {
      for (String var5 : var1) {
         if (var0.contains(var5)) {
            return true;
         }
      }

      return false;
   }

   @Environment(EnvType.CLIENT)
   public static enum Kind {
      NONE,
      COOLDOWN,
      WARMUP,
      CANCELLED,
      UNKNOWN_COMMAND,
      NO_PERMISSION,
      COMBAT;

      private Kind() {
      }
   }
}
