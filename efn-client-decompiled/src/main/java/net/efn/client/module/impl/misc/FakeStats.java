package net.efn.client.module.impl.misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.ScoreboardCriterion.RenderType;
import net.minecraft.scoreboard.number.FixedNumberFormat;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class FakeStats extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final String OBJECTIVE = "efn_fs";
   private static final String SHOWN_BY_DEFAULT = "efn.lol";
   private static final int LINES = 15;
   private static final Comparator<ScoreboardEntry> ORDER = Comparator.<ScoreboardEntry>comparingInt(ScoreboardEntry::comp_2128)
      .reversed()
      .thenComparing(ScoreboardEntry::comp_2127, String.CASE_INSENSITIVE_ORDER);
   private static final Pattern MONEY = Pattern.compile("\\$\\s?(\\d[\\d.,]*[KkMmBbTtQq]?)");
   private static final String SMALL_CAPS = "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u01eb\u0280\ua731\u1d1b\u1d1c\u1d20\u1d21x\u028f\u1d22";
   private static volatile FakeStats active;
   private final StringSetting playerName = this.addSetting("name", new StringSetting("Name", "efn.lol"));
   private final FakeStats.Stat money = this.stat("Money", "money", "balance", "$");
   private final List<FakeStats.Stat> stats = List.of(
      this.money,
      this.stat("Shards", "shards", "\u2605"),
      this.stat("Kills", "kills", "\ud83d\udde1", "\u2694"),
      this.stat("Deaths", "deaths", "\u2620"),
      this.stat("Playtime", "playtime", "\u231a"),
      this.stat("Team", "team")
   );
   private long seed;
   private boolean applying;
   private Scoreboard board;
   private ScoreboardObjective fake;
   private ScoreboardObjective serverSidebar;
   private String sidebarMoney;
   private boolean nameReported;

   public FakeStats() {
      super("fake-stats", "Fake Stats", "Shows your own name and stats on the sidebar and tab list. Blank stats get scrambled.", ModuleCategory.DONUT);
   }

   private FakeStats.Stat stat(String var1, String... var2) {
      return new FakeStats.Stat(this.addSetting(var1.toLowerCase(Locale.ROOT), new StringSetting(var1, "efn.lol")), var2);
   }

   @Override
   public boolean enabledByDefault() {
      return true;
   }

   public static boolean redirect(Scoreboard var0, ScoreboardDisplaySlot var1, ScoreboardObjective var2) {
      FakeStats var3 = active;
      if (var3 == null || var3.applying || var1 != ScoreboardDisplaySlot.field_45157) {
         return false;
      } else if (var2 != null && "efn_fs".equals(var2.method_1113())) {
         return false;
      } else {
         var3.serverSidebar = var2;
         if (var2 == null) {
            return false;
         } else {
            ScoreboardObjective var4 = var3.install(var0);
            if (var4 == null) {
               return false;
            } else {
               var3.applying = true;

               try {
                  var0.method_1158(ScoreboardDisplaySlot.field_45157, var4);
               } finally {
                  var3.applying = false;
               }

               return true;
            }
         }
      }
   }

   public static Text tabName(UUID var0, Text var1) {
      FakeStats var2 = active;
      return var2 != null && var1 != null && isSelf(var0) ? var2.fakeTabMoney(var1) : var1;
   }

   public static StringVisitable tabText(StringVisitable var0) {
      FakeStats var2 = active;
      Object var1;
      if (var2 != null && var0 instanceof Text var3) {
         var1 = var2.fakeTabMoney(var3);
      } else {
         var1 = var0;
      }

      return (StringVisitable)var1;
   }

   public static ReadableScoreboardScore tabScore(ScoreHolder var0, final ReadableScoreboardScore var1) {
      final FakeStats var2 = active;
      ClientPlayerEntity var3 = MinecraftClient.method_1551().field_1724;
      return var2 != null && var1 != null && var3 != null && var3.method_5820().equals(var0.method_5820()) ? new ReadableScoreboardScore() {
         public int method_55397() {
            return var1.method_55397();
         }

         public boolean method_1131() {
            return var1.method_1131();
         }

         public NumberFormat method_55400() {
            return var1.method_55400();
         }

         public MutableText method_55399(NumberFormat var1x) {
            return Text.method_43473().method_10852(var2.fakeTabMoney(var1.method_55399(var1x)));
         }
      } : var1;
   }

   @Override
   protected void onEnable() {
      this.seed = ThreadLocalRandom.current().nextLong();
      active = this;
      this.on(TickEvent.TYPE, Stage.POST, var1 -> this.refresh());
      this.refresh();
   }

   @Override
   protected void onDisable() {
      active = null;
      this.restore();
   }

   private void refresh() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (var1.field_1687 == null) {
         this.board = null;
         this.fake = null;
         this.serverSidebar = null;
         this.sidebarMoney = null;
      } else {
         Scoreboard var2 = var1.field_1687.method_8428();
         if (var2 != this.board) {
            this.board = var2;
            this.fake = null;
            this.serverSidebar = null;
         }

         ScoreboardObjective var3 = var2.method_1189(ScoreboardDisplaySlot.field_45157);
         if (this.serverSidebar == null || this.serverSidebar.method_1117() != var2) {
            ScoreboardObjective var4 = this.serverSidebar = var3 != null && !"efn_fs".equals(var3.method_1113()) ? var3 : null;
         }

         if (this.serverSidebar == null) {
            this.sidebarMoney = null;
            if (this.fake != null && var3 == this.fake) {
               this.applying = true;

               try {
                  var2.method_1158(ScoreboardDisplaySlot.field_45157, null);
               } finally {
                  this.applying = false;
               }
            }
         } else {
            ScoreboardObjective var13 = this.install(var2);
            if (var13 != null) {
               if (var2.method_1189(ScoreboardDisplaySlot.field_45157) != var13) {
                  this.applying = true;

                  try {
                     var2.method_1158(ScoreboardDisplaySlot.field_45157, var13);
                  } finally {
                     this.applying = false;
                  }
               }
            }
         }
      }
   }

   private ScoreboardObjective install(Scoreboard var1) {
      ScoreboardObjective var2 = this.serverSidebar;
      if (var2 != null && var2.method_1117() == var1) {
         ScoreboardObjective var3 = var1.method_1170("efn_fs");
         if (var3 == null) {
            var3 = var1.method_1168("efn_fs", ScoreboardCriterion.field_1468, Text.method_43473(), RenderType.field_1472, false, var2.method_55384());
         }

         this.fake = var3;
         this.board = var1;
         var3.method_55382(var2.method_55384());
         this.writeRows(var1, var3, var2);
         return var3;
      } else {
         return this.fake;
      }
   }

   private void writeRows(Scoreboard var1, ScoreboardObjective var2, ScoreboardObjective var3) {
      ArrayList var7 = new ArrayList();

      for (ScoreboardEntry var9 : var1.method_1184(var3)) {
         if (!var9.method_55385()) {
            var7.add(var9);
         }
      }

      var7.sort(ORDER);
      if (var7.size() > 15) {
         var7.subList(15, var7.size()).clear();
      }

      Text var5;
      Text var6;
      var2.method_1121((var6 = this.renamed(var5 = visible(var3.method_1114()))) != null ? var6 : this.mask(var5, 0));
      boolean var21 = var6 != null;
      NumberFormat var22 = var3.method_55380(StyledNumberFormat.field_47567);
      this.sidebarMoney = null;

      for (int var4 = 0; var4 < var7.size(); var4++) {
         ScoreboardEntry var10 = (ScoreboardEntry)var7.get(var4);
         Text var11 = visible(Team.method_1142(var1.method_1164(var10.comp_2127()), var10.method_55387()));
         Object var12 = visible(var10.method_55386(var22));
         Text var13 = this.renamed(var11);
         FakeStats.Hit var14 = var13 == null ? this.find(var11.getString(), var12.getString()) : null;
         String var15 = var14 != null ? var14.stat().value().get().strip() : "";
         String var16 = null;
         if (var13 != null) {
            var11 = var13;
            var21 = true;
         } else if (var15.isEmpty()) {
            var11 = this.mask(var11, var4 + 1);
            var12 = this.mask((Text)var12, var4 + 1);
            if (var14 != null) {
               var16 = (var14.inNumber() ? var12 : var11).getString().substring(var14.start(), var14.end());
            }
         } else {
            Object var17 = var14.inNumber() ? var12 : var11;
            var16 = fit(var17.getString().substring(var14.start(), var14.end()), var15);
            MutableText var18 = replace((Text)var17, List.of(new int[]{var14.start(), var14.end()}), var16);
            var11 = (Text)(var14.inNumber() ? this.mask(var11, var4 + 1) : var18);
            var12 = var14.inNumber() ? var18 : this.mask((Text)var12, var4 + 1);
         }

         if (var14 != null && var14.stat() == this.money) {
            this.sidebarMoney = var16;
         }

         ScoreHolder var25 = ScoreHolder.method_55422(holderName(var4));
         var1.method_1195(var25.method_5820());
         ScoreAccess var26 = var1.method_1180(var25, var2);
         var26.method_55410(var10.comp_2128());
         var26.method_55411(var11);
         var26.method_55412(new FixedNumberFormat((Text)var12));
      }

      for (int var20 = var7.size(); var20 < 15; var20++) {
         ScoreHolder var23 = ScoreHolder.method_55422(holderName(var20));
         if (var1.method_55430(var23, var2) != null) {
            var1.method_1155(var23, var2);
         }
      }

      if (!var21 && !this.playerName.get().isBlank() && !this.nameReported) {
         this.nameReported = true;
         this.reportMissingName(var1, var7, var22, var5);
      }
   }

   private void reportMissingName(Scoreboard var1, List<ScoreboardEntry> var2, NumberFormat var3, Text var4) {
      StringBuilder var5 = new StringBuilder("title: ").append(escaped(var4.getString()));

      for (ScoreboardEntry var7 : var2) {
         Text var8 = visible(Team.method_1142(var1.method_1164(var7.comp_2127()), var7.method_55387()));
         var5.append("\n  ").append(escaped(var8.getString())).append(" | ").append(escaped(visible(var7.method_55386(var3)).getString()));
      }

      LOGGER.info("Fake Stats: no sidebar line shows the name {}\n{}", selfName(), var5);
   }

   private static String escaped(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length());

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (var3 >= ' ' && var3 < 127) {
            var1.append(var3);
         } else {
            var1.append(String.format("\\u%04X", var3));
         }
      }

      return var1.toString();
   }

   private void restore() {
      Scoreboard var2 = this.board;
      ScoreboardObjective var3 = this.serverSidebar;
      ScoreboardObjective var4 = this.fake;
      this.board = null;
      this.fake = null;
      this.serverSidebar = null;
      this.sidebarMoney = null;
      if (var2 != null) {
         if (var2.method_1189(ScoreboardDisplaySlot.field_45157) == var4) {
            ScoreboardObjective var5 = var3 != null && var3.method_1117() == var2 ? var3 : null;
            var2.method_1158(ScoreboardDisplaySlot.field_45157, var5);
         }

         ScoreboardObjective var1;
         if ((var1 = var2.method_1170("efn_fs")) != null) {
            var2.method_1194(var1);
         }
      }
   }

   private FakeStats.Hit find(String var1, String var2) {
      for (FakeStats.Stat var4 : this.stats) {
         int[] var5 = valueAfter(var1, var4);
         if (var5 != null && var5[0] < var5[1]) {
            return new FakeStats.Hit(var4, false, var5[0], var5[1]);
         }

         if (var5 != null) {
            return new FakeStats.Hit(var4, true, 0, var2.length());
         }

         var5 = valueAfter(var2, var4);
         if (var5 != null) {
            return new FakeStats.Hit(var4, true, var5[0], var5[1]);
         }
      }

      return null;
   }

   private static int[] valueAfter(String var0, FakeStats.Stat var1) {
      for (String var5 : var1.markers()) {
         int var7 = markerAt(var0, var5);
         if (var7 >= 0) {
            int var8 = var0.length();
            int var6 = var7 + var5.length();

            while (var6 < var8 && (Character.isWhitespace(var0.charAt(var6)) || var0.charAt(var6) == ':')) {
               var6++;
            }

            while (var8 > var6 && Character.isWhitespace(var0.charAt(var8 - 1))) {
               var8--;
            }

            return new int[]{var6, var8};
         }
      }

      return null;
   }

   private static int markerAt(String var0, String var1) {
      String var2 = folded(var0);
      boolean var3 = wordChar(var1.charAt(0));
      boolean var4 = wordChar(var1.charAt(var1.length() - 1));

      for (int var5 = 0; var5 + var1.length() <= var2.length(); var5++) {
         int var6 = var5 + var1.length();
         if (var2.regionMatches(true, var5, var1, 0, var1.length())
            && (!var3 || var5 <= 0 || !wordChar(var2.charAt(var5 - 1)))
            && (!var4 || var6 >= var2.length() || !wordChar(var2.charAt(var6)))) {
            return var5;
         }
      }

      return -1;
   }

   private static String folded(String var0) {
      char[] var1 = var0.toCharArray();

      for (int var2 = 0; var2 < var1.length; var2++) {
         int var3 = "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u01eb\u0280\ua731\u1d1b\u1d1c\u1d20\u1d21x\u028f\u1d22"
            .indexOf(var1[var2]);
         if (var3 >= 0) {
            var1[var2] = (char)(97 + var3);
         }
      }

      return new String(var1);
   }

   private static boolean wordChar(char var0) {
      return Character.isLetterOrDigit(var0) || var0 == '_';
   }

   private static String selfName() {
      ClientPlayerEntity var0 = MinecraftClient.method_1551().field_1724;
      return var0 != null ? var0.method_7334().name() : null;
   }

   private Text renamed(Text var1) {
      String var3 = selfName();
      int var2 = var3 != null ? markerAt(var1.getString(), var3) : -1;
      if (var2 < 0) {
         return null;
      } else {
         String var5 = this.playerName.get().strip();
         return (Text)(var5.isEmpty() ? var1 : replace(var1, List.of(new int[]{var2, var2 + var3.length()}), var5));
      }
   }

   private static String fit(String var0, String var1) {
      String var2 = var1.startsWith("$") ? var1.substring(1).strip() : var1;
      return var0.startsWith("$") ? "$" + var2 : var2;
   }

   private String tabMoney() {
      String var2 = this.money.value().get().strip();
      String var1 = var2.isEmpty() ? this.sidebarMoney : var2;
      if (var1 == null) {
         return null;
      } else {
         var1 = var1.startsWith("$") ? var1.substring(1).strip() : var1.strip();
         return var1.isEmpty() ? null : var1;
      }
   }

   private Text fakeTabMoney(Text var1) {
      String var2 = this.tabMoney();
      if (var2 != null && var1.getString().indexOf(36) >= 0) {
         Text var3 = visible(var1);
         Matcher var4 = MONEY.matcher(var3.getString());
         ArrayList var5 = new ArrayList();

         while (var4.find()) {
            var5.add(new int[]{var4.start(1), var4.end(1)});
         }

         return (Text)(var5.isEmpty() ? var1 : replace(var3, var5, var2));
      } else {
         return var1;
      }
   }

   private static boolean isSelf(UUID var0) {
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      return var1 != null && var1.method_5667().equals(var0);
   }

   private static Text visible(Text var0) {
      if (var0.getString().indexOf(167) < 0) {
         return var0;
      } else {
         MutableText var1 = Text.method_43473();
         StringBuilder var2 = new StringBuilder();
         Style[] var3 = new Style[]{null};
         TextVisitFactory.method_27476(var0, Style.field_24360, (var3x, var4, var5) -> {
            if (var3[0] != null && !var4.equals(var3[0])) {
               var1.method_10852(Text.method_43470(var2.toString()).method_10862(var3[0]));
               var2.setLength(0);
            }

            var3[0] = var4;
            var2.appendCodePoint(var5);
            return true;
         });
         if (!var2.isEmpty()) {
            var1.method_10852(Text.method_43470(var2.toString()).method_10862(var3[0]));
         }

         return var1;
      }
   }

   private static MutableText replace(Text var0, List<int[]> var1, String var2) {
      if (var0.getString().isEmpty()) {
         return Text.method_43470(var2);
      } else {
         MutableText var3 = Text.method_43473();
         int[] var4 = new int[]{0};
         int[] var5 = new int[]{0};
         var0.method_27658((var5x, var6) -> {
            int var7 = var4[0];
            int var8 = var7 + var6.length();
            int var9 = var7;

            while (var9 < var8) {
               int[] var10 = var5[0] < var1.size() ? (int[])var1.get(var5[0]) : null;
               if (var10 == null || var10[0] >= var8) {
                  var3.method_10852(Text.method_43470(var6.substring(var9 - var7)).method_10862(var5x));
                  var9 = var8;
               } else if (var9 < var10[0]) {
                  var3.method_10852(Text.method_43470(var6.substring(var9 - var7, var10[0] - var7)).method_10862(var5x));
                  var9 = var10[0];
               } else {
                  if (var9 == var10[0]) {
                     var3.method_10852(Text.method_43470(var2).method_10862(var5x));
                  }

                  if ((var9 = Math.min(var8, var10[1])) == var10[1]) {
                     var5[0]++;
                  }
               }
            }

            var4[0] = var8;
            return Optional.empty();
         }, Style.field_24360);
         return var3;
      }
   }

   private static String holderName(int var0) {
      return new String(new char[]{'\ue000', (char)(57360 + var0)});
   }

   private Text mask(Text var1, int var2) {
      if (var1 == null) {
         return Text.method_43473();
      } else {
         MutableText var3 = Text.method_43473();
         boolean[] var4 = new boolean[]{false};
         int[] var5 = new int[]{0};
         var1.method_27658((var5x, var6) -> {
            String var7 = this.maskText(var6, var2, var5);
            if (!var7.equals(var6)) {
               var4[0] = true;
            }

            var3.method_10852(Text.method_43470(var7).method_10862(var5x));
            return Optional.empty();
         }, Style.field_24360);
         return (Text)(var4[0] ? var3 : var1);
      }
   }

   private String maskText(String var1, int var2, int[] var3) {
      StringBuilder var4 = null;
      int var5 = 0;

      while (var5 < var1.length()) {
         int var6 = var1.codePointAt(var5);
         if (!Character.isDigit(var6)) {
            if (var4 != null) {
               var4.appendCodePoint(var6);
            }

            var5 += Character.charCount(var6);
         } else {
            int var8 = var5;

            while (true) {
               if (var8 < var1.length()) {
                  int var9 = var1.codePointAt(var8);
                  int var10 = Character.charCount(var9);
                  if (Character.isDigit(var9)) {
                     var8 += var10;
                     continue;
                  }

                  if ((var9 == 44 || var9 == 46) && var8 + var10 < var1.length() && Character.isDigit(var1.codePointAt(var8 + var10))) {
                     var8 += var10;
                     continue;
                  }
               }

               String var12 = var1.substring(var5, var8);
               int var13 = var3[0]++;
               String var11 = this.fakeNumber(var2, var13, var12);
               if (var4 == null) {
                  if (!var11.equals(var12)) {
                     var4 = new StringBuilder(var1.length()).append(var1, 0, var5).append(var11);
                  }
               } else {
                  var4.append(var11);
               }

               var5 = var8;
               break;
            }
         }
      }

      return var4 == null ? var1 : var4.toString();
   }

   private String fakeNumber(int var1, int var2, String var3) {
      int var8 = 0;

      for (int var9 = 0; var9 < var3.length(); var9++) {
         char var10 = var3.charAt(var9);
         if (var10 >= '0' && var10 <= '9') {
            var8++;
         }
      }

      if (var8 == 0) {
         return var3;
      } else if (var8 > 15) {
         return this.shiftWide(var1, var2, var3);
      } else {
         long var19 = 0L;

         for (int var11 = 0; var11 < var3.length(); var11++) {
            char var12 = var3.charAt(var11);
            if (var12 >= '0' && var12 <= '9') {
               var19 = var19 * 10L + (var12 - '0');
            }
         }

         long var20 = pow10(var8);
         long var13 = Long.remainderUnsigned(mix(this.seed ^ var1 * 1315423911L ^ var2 * 2654435761L ^ var8), var20);
         if (var13 == 0L) {
            var13 = 1L;
         }

         boolean var5;
         long var6;
         if ((var6 = shifted(var19, var13, var20, var8, var5 = var3.charAt(0) == '0' && var8 > 1)) == var19) {
            var6 = shifted(var19, var13 == var20 - 1L ? 1L : var13 + 1L, var20, var8, var5);
         }

         String var4;
         if ((var4 = Long.toString(var6)).length() < var8) {
            var4 = "0".repeat(var8 - var4.length()) + var4;
         }

         StringBuilder var15 = new StringBuilder(var3.length());
         int var16 = 0;

         for (int var17 = 0; var17 < var3.length(); var17++) {
            char var18 = var3.charAt(var17);
            if (var18 >= '0' && var18 <= '9') {
               var15.append(var4.charAt(var16++));
            } else {
               var15.append(var18);
            }
         }

         return var15.toString();
      }
   }

   private static long shifted(long var0, long var2, long var4, int var6, boolean var7) {
      long var8 = Math.floorMod(var0 + var2, var4);
      if (!var7 && var6 > 1 && var8 < var4 / 10L) {
         var8 += var4 / 10L;
      }

      return var8;
   }

   private String shiftWide(int var1, int var2, String var3) {
      long var4 = mix(this.seed ^ var1 * 1315423911L ^ var2);
      StringBuilder var6 = new StringBuilder(var3.length());
      int var7 = 0;
      boolean var8 = true;

      for (int var9 = 0; var9 < var3.length(); var9++) {
         char var10 = var3.charAt(var9);
         if (var10 >= '0' && var10 <= '9') {
            int var11 = (int)Long.remainderUnsigned(mix(var4 + var7), 9L) + 1;
            int var12 = (var10 - '0' + var11) % 10;
            if (var8 && var12 == 0) {
               var12 = var11;
            }

            var8 = false;
            var6.append((char)(48 + var12));
            var7++;
         } else {
            var6.append(var10);
         }
      }

      return var6.toString();
   }

   private static long pow10(int var0) {
      long var1 = 1L;

      for (int var3 = 0; var3 < var0; var3++) {
         var1 *= 10L;
      }

      return var1;
   }

   private static long mix(long var0) {
      var0 = (var0 ^ var0 >>> 30) * -4658895280553007687L;
      var0 = (var0 ^ var0 >>> 27) * -7723592293110705685L;
      return var0 ^ var0 >>> 31;
   }

   @Environment(EnvType.CLIENT)
   private record Hit(FakeStats.Stat stat, boolean inNumber, int start, int end) {
   }

   @Environment(EnvType.CLIENT)
   private record Stat(StringSetting value, String[] markers) {
   }
}
