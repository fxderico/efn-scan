package net.efn.client.module.impl.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import imgui.ImDrawList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import net.efn.client.chat.ModuleChat;
import net.efn.client.gui.HudPanel;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.OpenUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class StaffLiveModule extends ClientModule implements HudOverlayProvider {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final String BROWSER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36";
   private static final Pattern CHANNEL = Pattern.compile("^(yt|tt):@?([A-Za-z0-9._-]+)$", 2);
   private static final String CANONICAL = "<link rel=\"canonical\" href=\"";
   private static final String LIVE_NOW = "\"isLiveNow\":true";
   private static final String META_TITLE = "<meta name=\"title\" content=\"";
   private static final String START = "\"startTimestamp\":\"";
   private static final int MAX_TITLE_CHARS = 48;
   private final StringListSetting channels = this.addSetting(
      "channels", new StringListSetting("Channels", "yt:wizoxlol", "yt:pastagamer08", "tt:pastagamer08", "tt:theauzzi", "tt:u_vv7_", "yt:DrDonutt")
   );
   private final NumberSetting interval = this.addSetting("check-every", new NumberSetting("Check Every (s)", 60.0F, 15.0F, 600.0F).step(5.0F));
   private final NumberSetting scale = this.addSetting("scale", new NumberSetting("Scale", 1.0F, 0.5F, 3.0F).step(0.05F));
   private final NumberSetting opacity = this.addSetting("opacity", HudPanel.opacitySetting());
   private final HudPanel panel = new HudPanel(
      this.addSetting("pos-x", HudPanel.coordinate("X")), this.addSetting("pos-y", HudPanel.coordinate("Y")), this.scale, this.opacity
   );
   private final HttpClient http = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10L)).build();
   private final Map<String, Boolean> live = new ConcurrentHashMap<>();
   private final Map<String, StaffLiveModule.Live> shown = new ConcurrentHashMap<>();
   private volatile Thread worker;

   public StaffLiveModule() {
      super(
         "staff-live",
         "Staff Live",
         "Tells you when DonutSMP staff go live on YouTube or TikTok: a chat line with a watch link, a chime, and a panel with the stream title while they are live. Channels are yt:handle or tt:handle.",
         ModuleCategory.DONUT
      );
      this.usesNotifications();
   }

   @Override
   protected void onEnable() {
      this.live.clear();
      this.shown.clear();
      Thread var1 = new Thread(this::run, "EFN Staff Live");
      var1.setDaemon(true);
      this.worker = var1;
      var1.start();
   }

   @Override
   protected void onDisable() {
      Thread var1 = this.worker;
      this.worker = null;
      if (var1 != null) {
         var1.interrupt();
      }

      this.shown.clear();
   }

   private void run() {
      long var1 = 0L;

      while (this.worker == Thread.currentThread()) {
         if (System.currentTimeMillis() >= var1) {
            HashSet var3 = new HashSet();

            for (StaffLiveModule.Channel var5 : this.channels()) {
               if (this.worker != Thread.currentThread()) {
                  return;
               }

               var3.add(var5.key());
               this.check(var5);
            }

            this.live.keySet().retainAll(var3);
            this.shown.keySet().retainAll(var3);
            var1 = System.currentTimeMillis() + (long)(this.interval.get() * 1000.0F);
         }

         try {
            Thread.sleep(1000L);
         } catch (InterruptedException var6) {
            return;
         }
      }
   }

   private List<StaffLiveModule.Channel> channels() {
      ArrayList var1 = new ArrayList();

      for (String var3 : this.channels.get()) {
         Matcher var4 = CHANNEL.matcher(var3.trim());
         if (var4.find()) {
            var1.add(new StaffLiveModule.Channel(var4.group(1).equalsIgnoreCase("yt"), var4.group(2)));
         }
      }

      return var1;
   }

   private void check(StaffLiveModule.Channel channel) {
      StaffLiveModule.Stream var2;
      try {
         var2 = channel.youtube() ? this.youtube(channel.handle()) : this.tiktok(channel.handle());
      } catch (InterruptedException var4) {
         Thread.currentThread().interrupt();
         return;
      } catch (Exception var5) {
         LOGGER.debug("[Staff Live] check of {} failed: {}", channel.key(), var5.toString());
         return;
      }

      if (var2 != null) {
         Boolean var3 = this.live.put(channel.key(), var2.live());
         if (var2.live() && !Boolean.TRUE.equals(var3)) {
            this.announce(channel, var2);
         }

         if (!var2.live()) {
            this.shown.remove(channel.key());
         }
      }
   }

   private StaffLiveModule.Stream youtube(String string) throws Exception {
      HttpRequest var2 = HttpRequest.newBuilder(URI.create("https://www.youtube.com/@" + string + "/live"))
         .timeout(Duration.ofSeconds(20L))
         .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36")
         .header("Accept-Language", "en-US,en;q=0.9")
         .header("Accept-Encoding", "gzip")
         .header("Cookie", "SOCS=CAI; CONSENT=YES+")
         .build();
      HttpResponse var3 = this.http.send(var2, BodyHandlers.ofInputStream());

      StaffLiveModule.Stream var23;
      try (InputStream var4 = (InputStream)var3.body()) {
         if (var3.statusCode() != 200) {
            return null;
         }

         boolean var6 = var3.headers().firstValue("Content-Encoding").map(stringx -> stringx.equalsIgnoreCase("gzip")).orElse(false);
         InputStreamReader var7 = new InputStreamReader((InputStream)(var6 ? new GZIPInputStream(var4) : var4), StandardCharsets.UTF_8);
         char[] var8 = new char[16384];
         StringBuilder var9 = new StringBuilder();
         String var10 = null;
         String var11 = null;
         boolean var12 = false;

         int var5;
         while ((var5 = var7.read(var8)) > 0) {
            var9.append(var8, 0, var5);
            String var13;
            if (var10 == null && (var13 = after(var9, "<link rel=\"canonical\" href=\"")) != null) {
               if (!var13.contains("/watch?v=")) {
                  return StaffLiveModule.Stream.OFFLINE;
               }

               var10 = var13;
            }

            if (var10 != null && var11 == null) {
               var11 = after(var9, "<meta name=\"title\" content=\"");
            }

            if (var10 != null && !var12) {
               var12 = var9.indexOf("\"isLiveNow\":true") >= 0;
            }

            if (var12) {
               String var14 = after(var9, "\"startTimestamp\":\"");
               if (var14 != null || var9.length() > 4096) {
                  return new StaffLiveModule.Stream(true, clean(var11), var10, null, parseStart(var14));
               }
            } else if (var9.length() > 4096) {
               var9.delete(0, var9.length() - 512);
            }
         }

         StaffLiveModule.Stream var21 = var12 ? new StaffLiveModule.Stream(true, clean(var11), var10, null, 0L) : StaffLiveModule.Stream.OFFLINE;
         var23 = var21;
      }

      return var23;
   }

   private StaffLiveModule.Stream tiktok(String string) throws Exception {
      String var2 = "https://www.tiktok.com/api-live/user/room/?aid=1988&app_language=en&app_name=tiktok_web&sourceType=54&uniqueId="
         + URLEncoder.encode(string, StandardCharsets.UTF_8);
      HttpRequest var3 = HttpRequest.newBuilder(URI.create(var2))
         .timeout(Duration.ofSeconds(20L))
         .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36")
         .header("Accept", "application/json")
         .build();
      HttpResponse var4 = this.http.send(var3, BodyHandlers.ofString());
      if (var4.statusCode() != 200) {
         return null;
      } else {
         JsonElement var5 = JsonParser.parseString((String)var4.body());
         if (!var5.isJsonObject()) {
            return null;
         } else {
            JsonObject var6 = object(var5.getAsJsonObject(), "data");
            JsonObject var7 = object(var6, "liveRoom");
            if (var7 != null && var7.has("status")) {
               boolean var8 = var7.get("status").getAsInt() == 2;
               JsonObject var9 = object(var6, "user");
               String var10 = var9 != null && var9.has("nickname") ? var9.get("nickname").getAsString() : null;
               String var11 = var7.has("title") ? var7.get("title").getAsString() : null;
               long var12 = var7.has("startTime") ? var7.get("startTime").getAsLong() * 1000L : 0L;
               return new StaffLiveModule.Stream(var8, clean(var11), "https://www.tiktok.com/@" + string + "/live", var10, var12);
            } else {
               return null;
            }
         }
      }
   }

   private void announce(StaffLiveModule.Channel channel, StaffLiveModule.Stream stream) {
      String var3 = stream.link() != null ? stream.link() : "https://www.youtube.com/@" + channel.handle() + "/live";
      String var4 = stream.nick() != null && !stream.nick().equalsIgnoreCase(channel.handle())
         ? channel.handle() + " (" + stream.nick() + ")"
         : channel.handle();
      String var5 = channel.youtube() ? "YouTube" : "TikTok";
      long var6 = System.currentTimeMillis();
      long var8 = stream.started() > 0L && stream.started() <= var6 ? stream.started() : var6;
      this.shown.putIfAbsent(channel.key(), new StaffLiveModule.Live(var4, var5, stream.title(), var8, var6));
      LOGGER.info("[Staff Live] {} is live on {}", var4, var5);
      MinecraftClient var10 = MinecraftClient.getInstance();
      var10.execute(() -> this.lambda$announce$2(var10, var4, var5, stream, (String)var3));
   }

   private List<StaffLiveModule.Live> rows() {
      return !this.isEnabled() ? List.of() : this.shown.values().stream().sorted(Comparator.comparingLong(StaffLiveModule.Live::shownAt)).toList();
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && MinecraftClient.getInstance().player != null && !this.shown.isEmpty();
   }

   @Override
   public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
      List var4 = this.rows();
      if (!var4.isEmpty()) {
         long var5 = System.currentTimeMillis();
         ArrayList var7 = new ArrayList();

         for (StaffLiveModule.Live var9 : var4) {
            var7.add(new HudPanel.Line(0, var9.who(), -1, var9.platform() + "  \u00b7  " + ago(var5 - var9.since()), -47814));
            if (var9.title() != null && !var9.title().isBlank()) {
               var7.add(new HudPanel.Line(shorten(var9.title()), -6052949));
            }
         }

         this.panel.card(imDrawList, "LIVE", String.valueOf(var4.size()), -47814, var7, "staff_list", f, g, 0.5F, 10.0F);
      }
   }

   private static String shorten(String string) {
      return string.length() <= 48 ? string : string.substring(0, 47).stripTrailing() + "\u2026";
   }

   private static String ago(long l) {
      long var2 = l / 60000L;
      return var2 < 1L ? "now" : (var2 < 60L ? var2 + "m" : var2 / 60L + "h " + var2 % 60L + "m");
   }

   private static String after(CharSequence charSequence, String string) {
      String var2 = charSequence.toString();
      int var3 = var2.indexOf(string);
      int var4 = var3 < 0 ? -1 : var2.indexOf(34, var3 + string.length());
      return var4 < 0 ? null : var2.substring(var3 + string.length(), var4);
   }

   private static String clean(String string) {
      if (string == null) {
         return null;
      } else {
         string = string.replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
         StringBuilder var1 = new StringBuilder(string.length());
         string.codePoints().filter(i -> i >= 32 && i < 8192 || i >= 8208 && i < 9728).forEach(var1::appendCodePoint);
         return var1.toString().replaceAll("\\s+", " ").trim();
      }
   }

   private static long parseStart(String string) {
      try {
         return string == null ? 0L : OffsetDateTime.parse(string).toInstant().toEpochMilli();
      } catch (RuntimeException var2) {
         return 0L;
      }
   }

   private static JsonObject object(JsonObject jsonObject, String string) {
      if (jsonObject == null) {
         return null;
      } else {
         JsonElement var2 = jsonObject.get(string);
         return var2 != null && var2.isJsonObject() ? var2.getAsJsonObject() : null;
      }
   }

   private void lambda$announce$2(MinecraftClient minecraftClient, String string, String string2, StaffLiveModule.Stream stream, String string3) {
      if (this.isEnabled() && minecraftClient.player != null) {
         ModuleChat.Body var6 = ModuleChat.body().name(string).text(" is ").danger("LIVE").text(" on " + string2);
         if (stream.title() != null && !stream.title().isBlank()) {
            var6.text(": " + stream.title());
         }

         var6.text(" ").append(Text.literal("[watch]").styled(style -> style.withClickEvent(new OpenUrl(URI.create(string3))).withUnderline(true)));
         ModuleChat.send(this, var6);
         if (this.notificationsOn()) {
            minecraftClient.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_CHIME, 1.25F));
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record Channel(boolean youtube, String handle) {
      String key() {
         return (this.youtube ? "yt:" : "tt:") + this.handle.toLowerCase(Locale.ROOT);
      }
   }

   @Environment(EnvType.CLIENT)
   private record Live(String who, String platform, String title, long since, long shownAt) {
   }

   @Environment(EnvType.CLIENT)
   private record Stream(boolean live, String title, String link, String nick, long started) {
      static final StaffLiveModule.Stream OFFLINE = new StaffLiveModule.Stream(false, null, null, null, 0L);
   }
}
