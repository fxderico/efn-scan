package net.efn.client.easyplace.builder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
final class Relogger {
   private static final int WAIT_TICKS = 60;
   private static final int TRIES = 5;
   private ServerInfo server;
   private boolean out;
   private int wait;
   private int tries;

   Relogger() {
   }

   boolean active() {
      return this.server != null;
   }

   boolean start(MinecraftClient minecraftClient) {
      ServerInfo var2 = minecraftClient.isInSingleplayer() ? null : minecraftClient.getCurrentServerEntry();
      if (var2 != null && !var2.isRealm() && minecraftClient.world != null) {
         this.server = var2;
         this.out = false;
         this.wait = 60;
         this.tries = 0;
         minecraftClient.send(() -> {
            if (this.server == var2 && minecraftClient.world != null) {
               minecraftClient.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT);
               this.out = true;
            }
         });
         return true;
      } else {
         return false;
      }
   }

   void tick(MinecraftClient minecraftClient) {
      if (this.server != null && this.out) {
         if (minecraftClient.world != null) {
            this.server = null;
         } else if (!(minecraftClient.currentScreen instanceof ConnectScreen) && --this.wait <= 0) {
            if (++this.tries > 5) {
               this.server = null;
            } else {
               ServerInfo var2 = this.server;
               Object var3 = minecraftClient.currentScreen instanceof MultiplayerScreen
                  ? minecraftClient.currentScreen
                  : new MultiplayerScreen(new TitleScreen());
               this.wait = 180;
               minecraftClient.send(() -> {
                  if (minecraftClient.world == null) {
                     ConnectScreen.connect(var3, minecraftClient, ServerAddress.parse(var2.address), var2, false, null);
                  }
               });
            }
         }
      }
   }
}
