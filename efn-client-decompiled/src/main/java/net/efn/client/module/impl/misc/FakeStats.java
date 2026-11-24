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
   private static final Comparator<ScoreboardEntry> ORDER = Comparator.comparingInt(ScoreboardEntry::value)
      .reversed()
      .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER);
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

   private FakeStats.Stat stat(String string, String... strings) {
      return new FakeStats.Stat(this.addSetting(string.toLowerCase(Locale.ROOT), new StringSetting(string, "efn.lol")), strings);
   }

   @Override
   public boolean enabledByDefault() {
      return true;
   }

   public static boolean redirect(Scoreboard scoreboard, ScoreboardDisplaySlot scoreboardDisplaySlot, ScoreboardObjective scoreboardObjective) {
      FakeStats var3 = active;
      if (var3 == null || var3.applying || scoreboardDisplaySlot != ScoreboardDisplaySlot.SIDEBAR) {
         return false;
      } else if (scoreboardObjective != null && "efn_fs".equals(scoreboardObjective.getName())) {
         return false;
      } else {
         var3.serverSidebar = scoreboardObjective;
         if (scoreboardObjective == null) {
            return false;
         } else {
            ScoreboardObjective var4 = var3.install(scoreboard);
            if (var4 == null) {
               return false;
            } else {
               var3.applying = true;

               try {
                  scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, var4);
               } finally {
                  var3.applying = false;
               }

               return true;
            }
         }
      }
   }

   public static Text tabName(UUID uUID, Text text) {
      FakeStats var2 = active;
      return var2 != null && text != null && isSelf(uUID) ? var2.fakeTabMoney(text) : text;
   }

   public static StringVisitable tabText(StringVisitable stringVisitable) {
      FakeStats var2 = active;
      Object var1;
      if (var2 != null && stringVisitable instanceof Text var3) {
         var1 = var2.fakeTabMoney(var3);
      } else {
         var1 = stringVisitable;
      }

      return (StringVisitable)var1;
   }

   public static ReadableScoreboardScore tabScore(ScoreHolder scoreHolder, ReadableScoreboardScore readableScoreboardScore) {
      final FakeStats var2 = active;
      ClientPlayerEntity var3 = MinecraftClient.getInstance().player;
      return var2 != null && readableScoreboardScore != null && var3 != null && var3.getNameForScoreboard().equals(scoreHolder.getNameForScoreboard())
         ? new ReadableScoreboardScore() {
            @Override
            public int getScore() {
               return readableScoreboardScore.getScore();
            }

            @Override
            public boolean isLocked() {
               return readableScoreboardScore.isLocked();
            }

            @Override
            public NumberFormat getNumberFormat() {
               return readableScoreboardScore.getNumberFormat();
            }

            @Override
            public MutableText getFormattedScore(NumberFormat fallbackFormat) {
               return Text.empty().append(var2.fakeTabMoney(readableScoreboardScore.getFormattedScore(fallbackFormat)));
            }
         }
         : readableScoreboardScore;
   }

   @Override
   protected void onEnable() {
      this.seed = ThreadLocalRandom.current().nextLong();
      active = this;
      this.on(TickEvent.TYPE, Stage.POST, tickEvent -> this.refresh());
      this.refresh();
   }

   @Override
   protected void onDisable() {
      active = null;
      this.restore();
   }

   private void refresh() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.world == null) {
         this.board = null;
         this.fake = null;
         this.serverSidebar = null;
         this.sidebarMoney = null;
      } else {
         Scoreboard var2 = var1.world.getScoreboard();
         if (var2 != this.board) {
            this.board = var2;
            this.fake = null;
            this.serverSidebar = null;
         }

         ScoreboardObjective var3 = var2.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         if (this.serverSidebar == null || this.serverSidebar.getScoreboard() != var2) {
            ScoreboardObjective var4 = this.serverSidebar = var3 != null && !"efn_fs".equals(var3.getName()) ? var3 : null;
         }

         if (this.serverSidebar == null) {
            this.sidebarMoney = null;
            if (this.fake != null && var3 == this.fake) {
               this.applying = true;

               try {
                  var2.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, null);
               } finally {
                  this.applying = false;
               }
            }
         } else {
            ScoreboardObjective var13 = this.install(var2);
            if (var13 != null) {
               if (var2.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) != var13) {
                  this.applying = true;

                  try {
                     var2.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, var13);
                  } finally {
                     this.applying = false;
                  }
               }
            }
         }
      }
   }

   private ScoreboardObjective install(Scoreboard scoreboard) {
      ScoreboardObjective var2 = this.serverSidebar;
      if (var2 != null && var2.getScoreboard() == scoreboard) {
         ScoreboardObjective var3 = scoreboard.getNullableObjective("efn_fs");
         if (var3 == null) {
            var3 = scoreboard.addObjective("efn_fs", ScoreboardCriterion.DUMMY, Text.empty(), RenderType.INTEGER, false, var2.getNumberFormat());
         }

         this.fake = var3;
         this.board = scoreboard;
         var3.setNumberFormat(var2.getNumberFormat());
         this.writeRows(scoreboard, var3, var2);
         return var3;
      } else {
         return this.fake;
      }
   }

   private void writeRows(Scoreboard scoreboard, ScoreboardObjective scoreboardObjective, ScoreboardObjective scoreboardObjective2) {
      ArrayList var7 = new ArrayList();

      for (ScoreboardEntry var9 : scoreboard.getScoreboardEntries(scoreboardObjective2)) {
         if (!var9.hidden()) {
            var7.add(var9);
         }
      }

      var7.sort(ORDER);
      if (var7.size() > 15) {
         var7.subList(15, var7.size()).clear();
      }

      Text var5;
      Text var6;
      scoreboardObjective.setDisplayName((var6 = this.renamed(var5 = visible(scoreboardObjective2.getDisplayName()))) != null ? var6 : this.mask(var5, 0));
      boolean var21 = var6 != null;
      NumberFormat var22 = scoreboardObjective2.getNumberFormatOr(StyledNumberFormat.RED);
      this.sidebarMoney = null;

      for (int var4 = 0; var4 < var7.size(); var4++) {
         ScoreboardEntry var10 = (ScoreboardEntry)var7.get(var4);
         Text var11 = visible(Team.decorateName(scoreboard.getScoreHolderTeam(var10.owner()), var10.name()));
         Object var12 = visible(var10.formatted(var22));
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

         ScoreHolder var25 = ScoreHolder.fromName(holderName(var4));
         scoreboard.clearTeam(var25.getNameForScoreboard());
         ScoreAccess var26 = scoreboard.getOrCreateScore(var25, scoreboardObjective);
         var26.setScore(var10.value());
         var26.setDisplayText(var11);
         var26.setNumberFormat(new FixedNumberFormat((Text)var12));
      }

      for (int var20 = var7.size(); var20 < 15; var20++) {
         ScoreHolder var23 = ScoreHolder.fromName(holderName(var20));
         if (scoreboard.getScore(var23, scoreboardObjective) != null) {
            scoreboard.removeScore(var23, scoreboardObjective);
         }
      }

      if (!var21 && !this.playerName.get().isBlank() && !this.nameReported) {
         this.nameReported = true;
         this.reportMissingName(scoreboard, var7, var22, var5);
      }
   }

   private void reportMissingName(Scoreboard scoreboard, List<ScoreboardEntry> list, NumberFormat numberFormat, Text text) {
      StringBuilder var5 = new StringBuilder("title: ").append(escaped(text.getString()));

      for (ScoreboardEntry var7 : list) {
         Text var8 = visible(Team.decorateName(scoreboard.getScoreHolderTeam(var7.owner()), var7.name()));
         var5.append("\n  ").append(escaped(var8.getString())).append(" | ").append(escaped(visible(var7.formatted(numberFormat)).getString()));
      }

      LOGGER.info("Fake Stats: no sidebar line shows the name {}\n{}", selfName(), var5);
   }

   private static String escaped(String string) {
      StringBuilder var1 = new StringBuilder(string.length());

      for (int var2 = 0; var2 < string.length(); var2++) {
         char var3 = string.charAt(var2);
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
         if (var2.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) == var4) {
            ScoreboardObjective var5 = var3 != null && var3.getScoreboard() == var2 ? var3 : null;
            var2.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, var5);
         }

         ScoreboardObjective var1;
         if ((var1 = var2.getNullableObjective("efn_fs")) != null) {
            var2.removeObjective(var1);
         }
      }
   }

   private FakeStats.Hit find(String string, String string2) {
      for (FakeStats.Stat var4 : this.stats) {
         int[] var5 = valueAfter(string, var4);
         if (var5 != null && var5[0] < var5[1]) {
            return new FakeStats.Hit(var4, false, var5[0], var5[1]);
         }

         if (var5 != null) {
            return new FakeStats.Hit(var4, true, 0, string2.length());
         }

         var5 = valueAfter(string2, var4);
         if (var5 != null) {
            return new FakeStats.Hit(var4, true, var5[0], var5[1]);
         }
      }

      return null;
   }

   private static int[] valueAfter(String string, FakeStats.Stat stat) {
      for (String var5 : stat.markers()) {
         int var7 = markerAt(string, var5);
         if (var7 >= 0) {
            int var8 = string.length();
            int var6 = var7 + var5.length();

            while (var6 < var8 && (Character.isWhitespace(string.charAt(var6)) || string.charAt(var6) == ':')) {
               var6++;
            }

            while (var8 > var6 && Character.isWhitespace(string.charAt(var8 - 1))) {
               var8--;
            }

            return new int[]{var6, var8};
         }
      }

      return null;
   }

   private static int markerAt(String string, String string2) {
      String var2 = folded(string);
      boolean var3 = wordChar(string2.charAt(0));
      boolean var4 = wordChar(string2.charAt(string2.length() - 1));

      for (int var5 = 0; var5 + string2.length() <= var2.length(); var5++) {
         int var6 = var5 + string2.length();
         if (var2.regionMatches(true, var5, string2, 0, string2.length())
            && (!var3 || var5 <= 0 || !wordChar(var2.charAt(var5 - 1)))
            && (!var4 || var6 >= var2.length() || !wordChar(var2.charAt(var6)))) {
            return var5;
         }
      }

      return -1;
   }

   private static String folded(String string) {
      char[] var1 = string.toCharArray();

      for (int var2 = 0; var2 < var1.length; var2++) {
         int var3 = "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u01eb\u0280\ua731\u1d1b\u1d1c\u1d20\u1d21x\u028f\u1d22"
            .indexOf(var1[var2]);
         if (var3 >= 0) {
            var1[var2] = (char)(97 + var3);
         }
      }

      return new String(var1);
   }

   private static boolean wordChar(char c) {
      return Character.isLetterOrDigit(c) || c == '_';
   }

   private static String selfName() {
      ClientPlayerEntity var0 = MinecraftClient.getInstance().player;
      return var0 != null ? var0.getGameProfile().name() : null;
   }

   private Text renamed(Text text) {
      String var3 = selfName();
      int var2 = var3 != null ? markerAt(text.getString(), var3) : -1;
      if (var2 < 0) {
         return null;
      } else {
         String var5 = this.playerName.get().strip();
         return (Text)(var5.isEmpty() ? text : replace(text, List.of(new int[]{var2, var2 + var3.length()}), var5));
      }
   }

   private static String fit(String string, String string2) {
      String var2 = string2.startsWith("$") ? string2.substring(1).strip() : string2;
      return string.startsWith("$") ? "$" + var2 : var2;
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

   private Text fakeTabMoney(Text text) {
      String var2 = this.tabMoney();
      if (var2 != null && text.getString().indexOf(36) >= 0) {
         Text var3 = visible(text);
         Matcher var4 = MONEY.matcher(var3.getString());
         ArrayList var5 = new ArrayList();

         while (var4.find()) {
            var5.add(new int[]{var4.start(1), var4.end(1)});
         }

         return (Text)(var5.isEmpty() ? text : replace(var3, var5, var2));
      } else {
         return text;
      }
   }

   private static boolean isSelf(UUID uUID) {
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      return var1 != null && var1.getUuid().equals(uUID);
   }

   private static Text visible(Text text) {
      if (text.getString().indexOf(167) < 0) {
         return text;
      } else {
         MutableText var1 = Text.empty();
         StringBuilder var2 = new StringBuilder();
         Style[] var3 = new Style[]{null};
         TextVisitFactory.visitFormatted(text, Style.EMPTY, (i, style, j) -> {
            if (var3[0] != null && !style.equals(var3[0])) {
               var1.append(Text.literal(var2.toString()).setStyle(var3[0]));
               var2.setLength(0);
            }

            var3[0] = style;
            var2.appendCodePoint(j);
            return true;
         });
         if (!var2.isEmpty()) {
            var1.append(Text.literal(var2.toString()).setStyle(var3[0]));
         }

         return var1;
      }
   }

   private static MutableText replace(Text text, List<int[]> list, String string) {
      if (text.getString().isEmpty()) {
         return Text.literal(string);
      } else {
         MutableText var3 = Text.empty();
         int[] var4 = new int[]{0};
         int[] var5 = new int[]{0};
         text.visit((style, string2) -> {
            int var7 = var4[0];
            int var8 = var7 + string2.length();
            int var9 = var7;

            while (var9 < var8) {
               int[] var10 = var5[0] < list.size() ? (int[])list.get(var5[0]) : null;
               if (var10 == null || var10[0] >= var8) {
                  var3.append(Text.literal(string2.substring(var9 - var7)).setStyle(style));
                  var9 = var8;
               } else if (var9 < var10[0]) {
                  var3.append(Text.literal(string2.substring(var9 - var7, var10[0] - var7)).setStyle(style));
                  var9 = var10[0];
               } else {
                  if (var9 == var10[0]) {
                     var3.append(Text.literal(string).setStyle(style));
                  }

                  if ((var9 = Math.min(var8, var10[1])) == var10[1]) {
                     var5[0]++;
                  }
               }
            }

            var4[0] = var8;
            return Optional.empty();
         }, Style.EMPTY);
         return var3;
      }
   }

   private static String holderName(int i) {
      return new String(new char[]{'\ue000', (char)(57360 + i)});
   }

   private Text mask(Text text, int i) {
      if (text == null) {
         return Text.empty();
      } else {
         MutableText var3 = Text.empty();
         boolean[] var4 = new boolean[]{false};
         int[] var5 = new int[]{0};
         text.visit((style, string) -> {
            String var7 = this.maskText(string, i, var5);
            if (!var7.equals(string)) {
               var4[0] = true;
            }

            var3.append(Text.literal(var7).setStyle(style));
            return Optional.empty();
         }, Style.EMPTY);
         return (Text)(var4[0] ? var3 : text);
      }
   }

   private String maskText(String string, int i, int[] is) {
      StringBuilder var4 = null;
      int var5 = 0;

      while (var5 < string.length()) {
         int var6 = string.codePointAt(var5);
         if (!Character.isDigit(var6)) {
            if (var4 != null) {
               var4.appendCodePoint(var6);
            }

            var5 += Character.charCount(var6);
         } else {
            int var8 = var5;

            while (true) {
               if (var8 < string.length()) {
                  int var9 = string.codePointAt(var8);
                  int var10 = Character.charCount(var9);
                  if (Character.isDigit(var9)) {
                     var8 += var10;
                     continue;
                  }

                  if ((var9 == 44 || var9 == 46) && var8 + var10 < string.length() && Character.isDigit(string.codePointAt(var8 + var10))) {
                     var8 += var10;
                     continue;
                  }
               }

               String var12 = string.substring(var5, var8);
               int var13 = is[0]++;
               String var11 = this.fakeNumber(i, var13, var12);
               if (var4 == null) {
                  if (!var11.equals(var12)) {
                     var4 = new StringBuilder(string.length()).append(string, 0, var5).append(var11);
                  }
               } else {
                  var4.append(var11);
               }

               var5 = var8;
               break;
            }
         }
      }

      return var4 == null ? string : var4.toString();
   }

   private String fakeNumber(int i, int j, String string) {
      int var8 = 0;

      for (int var9 = 0; var9 < string.length(); var9++) {
         char var10 = string.charAt(var9);
         if (var10 >= '0' && var10 <= '9') {
            var8++;
         }
      }

      if (var8 == 0) {
         return string;
      } else if (var8 > 15) {
         return this.shiftWide(i, j, string);
      } else {
         long var19 = 0L;

         for (int var11 = 0; var11 < string.length(); var11++) {
            char var12 = string.charAt(var11);
            if (var12 >= '0' && var12 <= '9') {
               var19 = var19 * 10L + (var12 - '0');
            }
         }

         long var20 = pow10(var8);
         long var13 = Long.remainderUnsigned(mix(this.seed ^ i * 1315423911L ^ j * 2654435761L ^ var8), var20);
         if (var13 == 0L) {
            var13 = 1L;
         }

         boolean var5;
         long var6;
         if ((var6 = shifted(var19, var13, var20, var8, var5 = string.charAt(0) == '0' && var8 > 1)) == var19) {
            var6 = shifted(var19, var13 == var20 - 1L ? 1L : var13 + 1L, var20, var8, var5);
         }

         String var4;
         if ((var4 = Long.toString(var6)).length() < var8) {
            var4 = "0".repeat(var8 - var4.length()) + var4;
         }

         StringBuilder var15 = new StringBuilder(string.length());
         int var16 = 0;

         for (int var17 = 0; var17 < string.length(); var17++) {
            char var18 = string.charAt(var17);
            if (var18 >= '0' && var18 <= '9') {
               var15.append(var4.charAt(var16++));
            } else {
               var15.append(var18);
            }
         }

         return var15.toString();
      }
   }

   private static long shifted(long l, long m, long n, int i, boolean bl) {
      long var8 = Math.floorMod(l + m, n);
      if (!bl && i > 1 && var8 < n / 10L) {
         var8 += n / 10L;
      }

      return var8;
   }

   private String shiftWide(int i, int j, String string) {
      long var4 = mix(this.seed ^ i * 1315423911L ^ j);
      StringBuilder var6 = new StringBuilder(string.length());
      int var7 = 0;
      boolean var8 = true;

      for (int var9 = 0; var9 < string.length(); var9++) {
         char var10 = string.charAt(var9);
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

   private static long pow10(int i) {
      long var1 = 1L;

      for (int var3 = 0; var3 < i; var3++) {
         var1 *= 10L;
      }

      return var1;
   }

   private static long mix(long l) {
      l = (l ^ l >>> 30) * -4658895280553007687L;
      l = (l ^ l >>> 27) * -7723592293110705685L;
      return l ^ l >>> 31;
   }

   @Environment(EnvType.CLIENT)
   private record Hit(FakeStats.Stat stat, boolean inNumber, int start, int end) {
   }

   @Environment(EnvType.CLIENT)
   private record Stat(StringSetting value, String[] markers) {
   }
}
