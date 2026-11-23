package net.efn.client.module.impl.render;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
enum PeepSong {
   STAR_SHOPPING("starshopping"),
   CRYBABY("crybaby"),
   CRY_ALONE("cryalone"),
   HELLBOY("hellboy"),
   BEAMER_BOY("beamerboy"),
   SPOTLIGHT("spotlight"),
   THE_BRIGHTSIDE("thebrightside", "brightside"),
   SUNLIGHT_ON_YOUR_SKIN("sunlightonyourskin"),
   LIFE_IS_BEAUTIFUL("lifeisbeautiful"),
   FALLING_DOWN("fallingdown"),
   SAVE_THAT_SHIT("savethatshit", "savethatsht"),
   AWFUL_THINGS("awfulthings"),
   WITCHBLADES("witchblades"),
   FOUR_GOLD_CHAINS("4goldchains", "fourgoldchains"),
   GHOST_BOY("ghostboy"),
   GHOST_GIRL("ghostgirl");

   static final PeepSong[] ALL = values();
   private static final Pattern SPACES = Pattern.compile("[\\s\\p{Z}]+");
   private static final Pattern BRACKETS = Pattern.compile("[(\\[{][^)\\]}]*[)\\]}]");
   private static final Pattern TAIL = Pattern.compile("\\s+(?:[-\u2013\u2014|]|feat\\.?|ft\\.?|with)\\s.*$");
   private static final Pattern VERSION = Pattern.compile("\\s*[-\u2013\u2014]\\s*(?:\\d{4}\\s*)?(?:remaster(?:ed)?|remix(?:ed)?|live)\\b.*$");
   private static final Pattern NOT_NAME = Pattern.compile("[^a-z0-9]");
   private final String[] names;

   private PeepSong(String... nullxx) {
      this.names = nullxx;
   }

   static PeepSong of(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = Normalizer.normalize(var0, Form.NFKC).toLowerCase(Locale.ROOT);
         var1 = SPACES.matcher(var1).replaceAll(" ");
         var1 = BRACKETS.matcher(var1).replaceAll(" ");
         var1 = TAIL.matcher(var1.strip()).replaceAll("");
         var1 = VERSION.matcher(var1).replaceAll("");
         var1 = NOT_NAME.matcher(var1).replaceAll("");

         for (PeepSong var5 : ALL) {
            for (String var9 : var5.names) {
               if (var9.equals(var1)) {
                  return var5;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
