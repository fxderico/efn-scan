package net.efn.client.module.impl.misc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.efn.client.chat.ModuleChat;
import net.efn.client.config.ModuleConfigStore;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.security.PayGuard;
import net.efn.client.webhook.WebhookConfig;
import net.efn.client.webhook.WebhookEmbed;
import net.efn.client.webhook.WebhookPayloadBuilder;
import net.efn.client.webhook.WebhookService;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.SystemToast.Type;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class PlayerDetectionModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final Set<String> PERMANENT_WHITELIST = Set.of("freecamera");
   private static final long DISCONNECT_DELAY_MS = 500L;
   private static final String WEBHOOK_TITLE = "\ud83d\udea8 Player Detection Alert";
   private static final int WEBHOOK_COLOR = 15158332;
   private static final Type TOAST_ID = new Type();
   private static final ExecutorService WEBHOOK_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
      Thread var1 = new Thread(runnable, "EFN-PlayerDetection-Webhook");
      var1.setDaemon(true);
      return var1;
   });
   private final StringSetting modulesToToggle = this.addSetting("modules-to-toggle", new StringSetting("Modules To Toggle", ""));
   private final BooleanSetting enableDisconnect = this.addSetting("disconnect", new BooleanSetting("Disconnect", true));
   private final ModeSetting notificationMode = this.addSetting("notification-mode", new ModeSetting("Notification Mode", "Both", "Chat", "Toast", "Both"));
   private final BooleanSetting toggleOnPlayer = this.addSetting("toggle-on-detect", new BooleanSetting("Toggle On Detect", true));
   private final StringSetting userWhitelist = this.addSetting("user-whitelist", new StringSetting("User Whitelist", ""));
   private final BooleanSetting enableWebhook = this.addSetting("webhook", new BooleanSetting("Webhook", false));
   private final StringSetting webhookUrl = this.addSetting("webhook-url", new StringSetting("Webhook URL", ""));
   private final BooleanSetting selfPing = this.addSetting("self-ping", new BooleanSetting("Self Ping", false));
   private final StringSetting discordId = this.addSetting("discord-id", new StringSetting("Discord ID", ""));
   private final BooleanSetting enablePanicPay = this.addSetting("enable-panic-pay", new BooleanSetting("Enable Panic Pay", false));
   private final StringSetting panicPayTarget = this.addSetting("target-player", new StringSetting("Target Player", ""));
   private final StringSetting panicPayAmount = this.addSetting("amount", new StringSetting("Amount", ""));
   private final Set<String> detectedPlayers = new HashSet<>();
   private ClientWorld lastLevel;
   private String whitelistRaw;
   private Set<String> whitelist = Set.of();

   public PlayerDetectionModule() {
      super("player-detection", "Player Detection", "Detects when players are in the world.", ModuleCategory.DONUT);
      this.usesNotifications();
      this.webhookUrl.visibleWhen(this.enableWebhook::get);
      this.selfPing.visibleWhen(this.enableWebhook::get);
      this.discordId.visibleWhen(() -> this.enableWebhook.get() && this.selfPing.get());
      this.panicPayTarget.visibleWhen(this.enablePanicPay::get);
      this.panicPayAmount.visibleWhen(this.enablePanicPay::get);
      this.webhookUrl.privateValue();
      this.discordId.privateValue();
      this.enablePanicPay.privateValue();
      this.panicPayTarget.privateValue();
      this.panicPayAmount.privateValue();
      this.userWhitelist.privateValue();
      this.userWhitelist.getImString().inputData.isResizable = true;
      this.modulesToToggle.getImString().inputData.isResizable = true;
   }

   @Override
   protected void onEnable() {
      this.detectedPlayers.clear();
      this.lastLevel = null;
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
   }

   @Override
   protected void onDisable() {
      this.detectedPlayers.clear();
      this.lastLevel = null;
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      ClientWorld var4 = var2.world;
      if (var3 != null && var4 != null) {
         if (var4 != this.lastLevel) {
            this.detectedPlayers.clear();
            this.lastLevel = var4;
         }

         String var5 = var3.getGameProfile().name();
         UUID var6 = var3.getUuid();
         Set var7 = this.whitelist();
         LinkedHashSet var8 = null;

         for (AbstractClientPlayerEntity var10 : var4.getPlayers()) {
            String var11;
            String var12;
            if (var10 != var3
               && !var6.equals(var10.getUuid())
               && (var12 = var10.getGameProfile().name()) != null
               && !var12.equals(var5)
               && !PERMANENT_WHITELIST.contains(var11 = var12.toLowerCase(Locale.ROOT))
               && !var7.contains(var11)) {
               if (var8 == null) {
                  var8 = new LinkedHashSet();
               }

               var8.add(var12);
            }
         }

         if (var8 == null) {
            this.detectedPlayers.clear();
         } else if (!var8.equals(this.detectedPlayers)) {
            this.detectedPlayers.clear();
            this.detectedPlayers.addAll(var8);
            this.handlePlayerDetection(var8);
         }
      } else {
         this.detectedPlayers.clear();
         this.lastLevel = null;
      }
   }

   private void handlePlayerDetection(Set<String> set) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      String var3 = this.notificationMode.get();
      switch (var3) {
         case "Chat":
            this.announceDetection(set);
            break;
         case "Toast":
            this.showToast(var2);
            break;
         default:
            this.announceDetection(set);
            this.showToast(var2);
      }

      for (ClientModule var6 : this.resolveModulesToToggle()) {
         var6.toggle();
      }

      if (this.enablePanicPay.get()) {
         this.panicPay(var2);
      }

      if (this.enableWebhook.get()) {
         this.sendWebhookNotification(var2, set);
      }

      if (this.toggleOnPlayer.get()) {
         this.toggle();
      }

      if (this.enableDisconnect.get()) {
         this.scheduleDisconnect(var2, set);
      }
   }

   private void announceDetection(Set<String> set) {
      ModuleChat.send(this, names(ModuleChat.body().danger(set.size() == 1 ? "Player detected" : "Players detected").text(": "), set));
   }

   private static ModuleChat.Body names(ModuleChat.Body body, Collection<String> collection) {
      boolean var2 = true;

      for (String var4 : collection) {
         if (!var2) {
            body.text(", ");
         }

         body.name(var4);
         var2 = false;
      }

      return body;
   }

   private void showToast(MinecraftClient minecraftClient) {
      SystemToast.show(minecraftClient.getToastManager(), TOAST_ID, Text.literal(this.name()), Text.literal("Player Detected!"));
   }

   private List<ClientModule> resolveModulesToToggle() {
      String var1 = this.modulesToToggle.get();
      if (var1.isBlank()) {
         return List.of();
      } else {
         LinkedHashSet var2 = new LinkedHashSet();

         for (String var6 : var1.split(",")) {
            String var7 = var6.trim();
            if (!var7.isEmpty()) {
               String var8 = ModuleConfigStore.renamedModuleId(var7);
               if (var8 != null) {
                  var7 = var8;
               }

               for (ClientModule var10 : ModuleRegistry.all()) {
                  if (var10.id().equalsIgnoreCase(var7) || var10.name().equalsIgnoreCase(var7)) {
                     if (var10 != this && var10.inConfigs()) {
                        var2.add(var10);
                     }
                     break;
                  }
               }
            }
         }

         return new ArrayList<>(var2);
      }
   }

   private Set<String> whitelist() {
      String var1 = this.userWhitelist.get();
      if (!var1.equals(this.whitelistRaw)) {
         HashSet var2 = new HashSet();

         for (String var6 : var1.split(",")) {
            String var7 = var6.trim();
            if (!var7.isEmpty()) {
               var2.add(var7.toLowerCase(Locale.ROOT));
            }
         }

         this.whitelist = var2;
         this.whitelistRaw = var1;
      }

      return this.whitelist;
   }

   private void panicPay(MinecraftClient minecraftClient) {
      String var2 = this.panicPayTarget.get().trim();
      String var3 = this.panicPayAmount.get().trim();
      if (var2.isEmpty()) {
         ModuleChat.send(this, ModuleChat.body().text("Panic pay target is ").warn("not set"));
      } else if (var3.isEmpty()) {
         ModuleChat.send(this, ModuleChat.body().text("Panic pay amount is ").warn("not set"));
      } else {
         ClientPlayNetworkHandler var4 = minecraftClient.getNetworkHandler();
         if (var4 != null) {
            String var5 = String.format("/pay %s %s", var2, var3);
            if (minecraftClient.inGameHud != null) {
               minecraftClient.inGameHud.getChatHud().addToMessageHistory(var5);
            }

            PayGuard.allow(var5);
            var4.sendChatCommand(var5.substring(1));
            ModuleChat.send(this, ModuleChat.body().text("Panic pay ").good("sent").text(": ").value(var3).text(" to ").name(var2));
         }
      }
   }

   private void sendWebhookNotification(MinecraftClient minecraftClient, Set<String> set) {
      String var3 = this.webhookUrl.get().trim();
      if (var3.isEmpty()) {
         ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").warn("not set"));
      } else if (!isValidWebhookUrl(var3)) {
         ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").danger("invalid"));
      } else {
         ServerInfo var4 = minecraftClient.getCurrentServerEntry();
         String var5 = var4 != null && var4.name != null ? var4.name : "Unknown Server";
         String var6 = this.discordId.get().trim();
         String var7 = this.selfPing.get() && !var6.isEmpty() ? "<@" + var6 + ">" : "";
         String var8 = this.panicPayTarget.get().trim();
         String var9 = this.panicPayAmount.get().trim();
         long var10 = System.currentTimeMillis() / 1000L;
         StringBuilder var12 = new StringBuilder("Player(s) detected on server!\n\n")
            .append("**Players:** ")
            .append(escapeMarkdown(String.join(", ", set)))
            .append('\n')
            .append("**Server:** ")
            .append(escapeMarkdown(var5))
            .append('\n')
            .append("**Time:** <t:")
            .append(var10)
            .append(":R>");
         if (this.enablePanicPay.get() && !var8.isEmpty() && !var9.isEmpty()) {
            var12.append("\n**Panic Pay:** Activated - Target: ").append(escapeMarkdown(var8)).append(", Amount: ").append(escapeMarkdown(var9));
         }

         WebhookPayloadBuilder var13 = new WebhookPayloadBuilder()
            .content(var7)
            .addEmbed(new WebhookEmbed("\ud83d\udea8 Player Detection Alert", var12.toString(), 15158332, null, null, "Sent by EFN", null));
         WebhookConfig var14 = new WebhookConfig(var3);
         WEBHOOK_EXECUTOR.execute(
            () -> {
               boolean var3x;
               try {
                  var3x = PlayerDetectionModule.WebhookHolder.SERVICE.send(var14, var13);
               } catch (RuntimeException | LinkageError var5x) {
                  LOGGER.warn("[EFN] Player Detection webhook failed ({})", var5x.getClass().getSimpleName());
                  var3x = false;
               }

               ModuleChat.send(
                  this,
                  var3x
                     ? ModuleChat.body().text("Webhook notification ").good("sent")
                     : ModuleChat.body().text("Webhook notification ").danger("failed").text(" (see the log for details)")
               );
            }
         );
      }
   }

   private static boolean isValidWebhookUrl(String string) {
      try {
         URI var1 = new URI(string);
         String var2 = var1.getScheme();
         return var1.getHost() != null && ("https".equalsIgnoreCase(var2) || "http".equalsIgnoreCase(var2));
      } catch (URISyntaxException var3) {
         return false;
      }
   }

   private static String escapeMarkdown(String string) {
      StringBuilder var1 = new StringBuilder(string.length() + 8);

      for (int var2 = 0; var2 < string.length(); var2++) {
         char var3 = string.charAt(var2);
         if (var3 == '\\' || var3 == '*' || var3 == '_' || var3 == '~' || var3 == '`' || var3 == '|') {
            var1.append('\\');
         }

         var1.append(var3);
      }

      return var1.toString();
   }

   private void scheduleDisconnect(MinecraftClient minecraftClient, Set<String> set) {
      ClientPlayNetworkHandler var3 = minecraftClient.getNetworkHandler();
      if (var3 != null) {
         ClientConnection var4 = var3.getConnection();
         CompletableFuture.delayedExecutor(500L, TimeUnit.MILLISECONDS, minecraftClient).execute(() -> this.disconnectFromServer(minecraftClient, var4, set));
      }
   }

   private void disconnectFromServer(MinecraftClient minecraftClient, ClientConnection clientConnection, Set<String> set) {
      ClientPlayNetworkHandler var4 = minecraftClient.getNetworkHandler();
      if (minecraftClient.world != null && var4 != null && var4.getConnection() == clientConnection && clientConnection.isOpen()) {
         AutoReconnectModule.holdOff();
         clientConnection.disconnect(Text.literal("Player(s) detected: " + String.join(", ", set)));
         ModuleChat.send(
            this,
            names(ModuleChat.body().danger("Disconnected").text(" from the server (").text(set.size() == 1 ? "player" : "players").text(" detected: "), set)
               .text(")")
         );
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class WebhookHolder {
      static final WebhookService SERVICE = new WebhookService();

      private WebhookHolder() {
      }
   }
}
