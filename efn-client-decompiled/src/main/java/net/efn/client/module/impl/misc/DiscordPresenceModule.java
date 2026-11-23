package net.efn.client.module.impl.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import net.efn.client.discord.DiscordIpc;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientConfigurationPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class DiscordPresenceModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final String APPLICATION_ID = "1560799535184216114";
   private static final String LOGO_ASSET = "logo";
   private static final String LOGO_TEXT = "EFN Client";
   private static final String BUTTON_LABEL = "Discord";
   private static final String IN_MENU = "In Main Menu";
   private static final String SINGLEPLAYER = "Singleplayer";
   private static final String MULTIPLAYER = "Multiplayer";
   private static final String LAN_WORLD = "LAN World";
   private static final String REALMS = "Realms";
   private static final long GAME_STARTED = ProcessHandle.current().info().startInstant().orElseGet(Instant::now).getEpochSecond();
   private static final long RETRY_MIN_MS = 5000L;
   private static final long RETRY_MAX_MS = 30000L;
   private static final long IDLE_POLL_MS = 1000L;
   private static final long MIN_UPDATE_GAP_MS = 4000L;
   private static final long KEEP_ALIVE_MS = 60000L;
   private final BooleanSetting showServerIp = this.addSetting("show-server-ip", new BooleanSetting("Show Server IP", true));
   private final BooleanSetting discordButton = this.addSetting("discord-button", new BooleanSetting("Discord Button", true));
   private volatile DiscordPresenceModule.Card card;
   private DiscordPresenceModule.Link link;
   private ClientConnection session;
   private long joinedAt;
   private boolean loggedIdle;

   public DiscordPresenceModule() {
      super("discord-presence", "Discord Presence", "Shows EFN Client and the server you are on in your Discord status.", ModuleCategory.CLIENT);
   }

   @Override
   public boolean enabledByDefault() {
      return true;
   }

   @Override
   protected void onEnable() {
      if ("1560799535184216114".isBlank()) {
         if (!this.loggedIdle) {
            this.loggedIdle = true;
            LOGGER.info("[EFN] Discord Presence: no Discord application id set (DiscordPresenceModule.APPLICATION_ID); idle");
         }
      } else {
         this.session = null;
         this.card = new DiscordPresenceModule.Card("In Main Menu", GAME_STARTED, this.discordButton.get());
         this.link = new DiscordPresenceModule.Link(this.link);
         this.refresh();
         this.on(TickEvent.TYPE, var1 -> this.refresh());
         this.link.start();
      }
   }

   @Override
   protected void onDisable() {
      if (this.link != null) {
         this.link.stop();
      }

      this.session = null;
   }

   private void refresh() {
      MinecraftClient var4 = MinecraftClient.method_1551();
      ClientPlayNetworkHandler var5 = var4.method_1562();
      long var1;
      String var3;
      if (var5 != null) {
         if (var5.method_48296() != this.session) {
            this.session = var5.method_48296();
            this.joinedAt = System.currentTimeMillis() / 1000L;
         }

         var3 = this.place(var4, var5.method_45734());
         var1 = this.joinedAt;
      } else {
         if (this.session != null && this.session.method_10758() && this.session.method_10744() instanceof ClientConfigurationPacketListener) {
            return;
         }

         this.session = null;
         var3 = "In Main Menu";
         var1 = GAME_STARTED;
      }

      boolean var6 = this.discordButton.get();
      DiscordPresenceModule.Card var7 = this.card;
      if (var1 != var7.startedAt() || var6 != var7.discordButton() || !var3.equals(var7.details())) {
         this.card = new DiscordPresenceModule.Card(var3, var1, var6);
         this.link.wake();
      }
   }

   private String place(MinecraftClient var1, ServerInfo var2) {
      if (var1.method_1542()) {
         return "Singleplayer";
      } else if (var2 == null) {
         return "Multiplayer";
      } else if (var2.method_52811()) {
         return "Realms";
      } else if (var2.method_2994()) {
         return "LAN World";
      } else {
         return this.showServerIp.get() ? var2.field_3761 : "Multiplayer";
      }
   }

   private static JsonObject activity(DiscordPresenceModule.Card var0) {
      JsonObject var1 = new JsonObject();
      var1.addProperty("name", "EFN Client");
      var1.addProperty("details", var0.details());
      var1.addProperty("state", "efn.lol");
      JsonObject var2 = new JsonObject();
      var2.addProperty("start", var0.startedAt());
      var1.add("timestamps", var2);
      JsonObject var3 = new JsonObject();
      var3.addProperty("large_image", "logo");
      var3.addProperty("large_text", "EFN Client");
      var1.add("assets", var3);
      if (var0.discordButton()) {
         JsonObject var4 = new JsonObject();
         var4.addProperty("label", "Discord");
         var4.addProperty("url", "https://discord.com/invite/pxHtFp5b6h");
         JsonArray var5 = new JsonArray();
         var5.add(var4);
         var1.add("buttons", var5);
      }

      return var1;
   }

   @Environment(EnvType.CLIENT)
   private record Card(String details, long startedAt, boolean discordButton) {
   }

   @Environment(EnvType.CLIENT)
   private final class Link implements Runnable {
      private final Thread thread = new Thread(this, "EFN Discord Presence");
      private volatile boolean running = true;
      private Thread previous;
      private DiscordIpc ipc;
      private DiscordPresenceModule.Card sent;
      private long sentAt;
      private long retryMs = 5000L;
      private String lastProblem;

      Link(DiscordPresenceModule.Link nullx) {
         this.previous = nullx != null ? nullx.thread : null;
         this.thread.setDaemon(true);
      }

      void start() {
         this.thread.start();
      }

      void wake() {
         LockSupport.unpark(this.thread);
      }

      void stop() {
         this.running = false;
         LockSupport.unpark(this.thread);
      }

      @Override
      public void run() {
         this.awaitPrevious();

         while (this.running) {
            if (this.ipc == null && !this.connect()) {
               this.pause(this.retryMs);
               this.retryMs = Math.min(this.retryMs * 2L, 30000L);
            } else {
               try {
                  this.ipc.poll();
                  DiscordPresenceModule.Card var1 = DiscordPresenceModule.this.card;
                  long var2 = System.currentTimeMillis() - this.sentAt;
                  if (this.sent == null || var2 >= (var1.equals(this.sent) ? 60000L : 4000L)) {
                     this.send(var1);
                  }
               } catch (IOException var4) {
                  this.report("lost Discord", var4);
                  this.hangUp();
                  continue;
               }

               this.pause(1000L);
            }
         }

         if (this.ipc != null) {
            this.clear();
         }
      }

      private void awaitPrevious() {
         if (this.previous != null) {
            try {
               this.previous.join();
            } catch (InterruptedException var2) {
               this.running = false;
            }

            this.previous = null;
         }
      }

      private boolean connect() {
         try {
            this.ipc = DiscordIpc.connect("1560799535184216114");
         } catch (IOException var2) {
            this.report("could not connect", var2);
            return false;
         }

         if (this.ipc == null) {
            return false;
         } else {
            DiscordPresenceModule.LOGGER.info("[EFN] Discord Presence: connected to Discord");
            this.retryMs = 5000L;
            this.lastProblem = null;
            this.sent = null;
            return true;
         }
      }

      private void send(DiscordPresenceModule.Card var1) throws IOException {
         try {
            this.ipc.setActivity(DiscordPresenceModule.activity(var1));
         } catch (DiscordIpc.RefusedException var3) {
            this.report("Discord refused the card", var3);
         }

         this.sent = var1;
         this.sentAt = System.currentTimeMillis();
      }

      private void clear() {
         try {
            this.ipc.setActivity(null);
         } catch (DiscordIpc.RefusedException | IOException var2) {
         }

         this.hangUp();
      }

      private void hangUp() {
         this.ipc.close();
         this.ipc = null;
         this.sent = null;
      }

      private void pause(long var1) {
         LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(var1));
      }

      private void report(String var1, Exception var2) {
         String var3 = var1 + " (" + var2 + ")";
         if (!var3.equals(this.lastProblem)) {
            this.lastProblem = var3;
            DiscordPresenceModule.LOGGER.warn("[EFN] Discord Presence: {}", var3);
         }
      }
   }
}
