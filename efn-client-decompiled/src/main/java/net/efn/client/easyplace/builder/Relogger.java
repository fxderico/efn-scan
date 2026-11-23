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

   boolean start(MinecraftClient var1) {
      ServerInfo var2 = var1.method_1542() ? null : var1.method_1558();
      if (var2 != null && !var2.method_52811() && var1.field_1687 != null) {
         this.server = var2;
         this.out = false;
         this.wait = 60;
         this.tries = 0;
         var1.method_63588(() -> {
            if (this.server == var2 && var1.field_1687 != null) {
               var1.method_73360(ClientWorld.field_61021);
               this.out = true;
            }
         });
         return true;
      } else {
         return false;
      }
   }

   void tick(MinecraftClient var1) {
      if (this.server != null && this.out) {
         if (var1.field_1687 != null) {
            this.server = null;
         } else if (!(var1.field_1755 instanceof ConnectScreen) && --this.wait <= 0) {
            if (++this.tries > 5) {
               this.server = null;
            } else {
               ServerInfo var2 = this.server;
               Object var3 = var1.field_1755 instanceof MultiplayerScreen ? var1.field_1755 : new MultiplayerScreen(new TitleScreen());
               this.wait = 180;
               var1.method_63588(() -> {
                  if (var1.field_1687 == null) {
                     ConnectScreen.method_36877(var3, var1, ServerAddress.method_2950(var2.field_3761), var2, false, null);
                  }
               });
            }
         }
      }
   }
}
