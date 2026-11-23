package net.efn.client.module.impl.misc;

import java.util.Locale;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class AutoReconnectModule extends ClientModule {
   private static final long HOLD_MS = 15000L;
   private static volatile long holdUntil;
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 5.0F, 3.0F, 60.0F).step(1.0F));
   private ServerInfo lastServer;
   private Screen activeScreen;
   private ButtonWidget reconnectButton;
   private int ticksLeft;
   private boolean heldScreen;

   public static void holdOff() {
      holdUntil = Util.getMeasuringTimeMs() + 15000L;
   }

   public AutoReconnectModule() {
      super("auto-reconnect", "Auto Reconnect", "Reconnects to the last server after an unexpected disconnect.", ModuleCategory.MISC);
      TickEvent.TYPE.listen(this, this::trackServer);
      ScreenEvents.AFTER_INIT.register(this::onScreenInit);
   }

   private void trackServer(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null && var2.world != null) {
         ServerInfo var3 = var2.isInSingleplayer() ? null : var2.getCurrentServerEntry();
         this.lastServer = var3 != null && !var3.isLocal() && !var3.isRealm() ? var3 : null;
      }
   }

   private void onScreenInit(MinecraftClient minecraftClient, Screen screen, int i, int j) {
      if (screen instanceof TitleScreen || screen instanceof MultiplayerScreen) {
         this.lastServer = null;
      } else if (screen instanceof DisconnectedScreen && this.lastServer != null) {
         ServerInfo var6 = this.lastServer;
         if (screen == this.activeScreen) {
            Screens.getButtons(screen).remove(this.reconnectButton);
         } else {
            this.activeScreen = screen;
            this.ticksLeft = Math.round(this.delay.get() * 20.0F);
            this.heldScreen = Util.getMeasuringTimeMs() < holdUntil || isBan(screen);
            holdUntil = 0L;
         }

         boolean var7 = this.isEnabled() && !this.heldScreen;
         ButtonWidget var5;
         this.reconnectButton = var5 = ButtonWidget.builder(reconnectLabel(var7, this.ticksLeft), buttonWidget -> {
            buttonWidget.active = false;
            this.reconnect(minecraftClient, var6);
         }).dimensions(i - 150 - 10, j - 20 - 10, 150, 20).build();
         Screens.getButtons(screen).add(var5);
         if (var7) {
            ScreenEvents.afterTick(screen).register(screenx -> {
               this.ticksLeft--;
               if (this.ticksLeft <= 0) {
                  var5.active = false;
                  this.reconnect(minecraftClient, var6);
               } else {
                  var5.setMessage(reconnectLabel(true, this.ticksLeft));
               }
            });
         }
      }
   }

   private static boolean isBan(Screen screen) {
      String var1 = screen.getNarratedTitle().getString().toLowerCase(Locale.ROOT);
      return var1.contains("banned") || var1.contains("blacklisted");
   }

   private void reconnect(MinecraftClient minecraftClient, ServerInfo serverInfo) {
      ServerAddress var3 = ServerAddress.parse(serverInfo.address);
      ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), minecraftClient, var3, serverInfo, false, null);
   }

   private static Text reconnectLabel(boolean bl, int i) {
      return bl ? Text.literal(String.format(Locale.ROOT, "Reconnect (%.1f)", i / 20.0)) : Text.literal("Reconnect");
   }
}
