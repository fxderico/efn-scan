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
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterTick;
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
      holdUntil = Util.method_658() + 15000L;
   }

   public AutoReconnectModule() {
      super("auto-reconnect", "Auto Reconnect", "Reconnects to the last server after an unexpected disconnect.", ModuleCategory.MISC);
      TickEvent.TYPE.listen(this, this::trackServer);
      ScreenEvents.AFTER_INIT.register(this::onScreenInit);
   }

   private void trackServer(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 != null && var2.field_1687 != null) {
         ServerInfo var3 = var2.method_1542() ? null : var2.method_1558();
         this.lastServer = var3 != null && !var3.method_2994() && !var3.method_52811() ? var3 : null;
      }
   }

   private void onScreenInit(MinecraftClient var1, Screen var2, int var3, int var4) {
      if (var2 instanceof TitleScreen || var2 instanceof MultiplayerScreen) {
         this.lastServer = null;
      } else if (var2 instanceof DisconnectedScreen && this.lastServer != null) {
         ServerInfo var6 = this.lastServer;
         if (var2 == this.activeScreen) {
            Screens.getButtons(var2).remove(this.reconnectButton);
         } else {
            this.activeScreen = var2;
            this.ticksLeft = Math.round(this.delay.get() * 20.0F);
            this.heldScreen = Util.method_658() < holdUntil || isBan(var2);
            holdUntil = 0L;
         }

         boolean var7 = this.isEnabled() && !this.heldScreen;
         ButtonWidget var5;
         this.reconnectButton = var5 = ButtonWidget.method_46430(reconnectLabel(var7, this.ticksLeft), var3x -> {
            var3x.field_22763 = false;
            this.reconnect(var1, var6);
         }).method_46434(var3 - 150 - 10, var4 - 20 - 10, 150, 20).method_46431();
         Screens.getButtons(var2).add(var5);
         if (var7) {
            ScreenEvents.afterTick(var2).register((AfterTick)var4x -> {
               this.ticksLeft--;
               if (this.ticksLeft <= 0) {
                  var5.field_22763 = false;
                  this.reconnect(var1, var6);
               } else {
                  var5.method_25355(reconnectLabel(true, this.ticksLeft));
               }
            });
         }
      }
   }

   private static boolean isBan(Screen var0) {
      String var1 = var0.method_25435().getString().toLowerCase(Locale.ROOT);
      return var1.contains("banned") || var1.contains("blacklisted");
   }

   private void reconnect(MinecraftClient var1, ServerInfo var2) {
      ServerAddress var3 = ServerAddress.method_2950(var2.field_3761);
      ConnectScreen.method_36877(new MultiplayerScreen(new TitleScreen()), var1, var3, var2, false, null);
   }

   private static Text reconnectLabel(boolean var0, int var1) {
      return var0 ? Text.method_43470(String.format(Locale.ROOT, "Reconnect (%.1f)", var1 / 20.0)) : Text.method_43470("Reconnect");
   }
}
