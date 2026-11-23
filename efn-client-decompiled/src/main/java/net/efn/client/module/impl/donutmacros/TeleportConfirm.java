package net.efn.client.module.impl.donutmacros;

import java.util.Map;
import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
final class TeleportConfirm {
   private static final long WINDOW_MS = 3000L;
   private final ClientModule owner;
   private Object pressed;
   private boolean warned;
   private long sentAt;

   TeleportConfirm(ClientModule clientModule) {
      this.owner = clientModule;
   }

   void reset() {
      this.pressed = null;
      this.warned = false;
      this.sentAt = 0L;
   }

   void sent() {
      this.sentAt = System.currentTimeMillis();
   }

   boolean tick(MinecraftClient minecraftClient) {
      Dialogs.View var2 = Dialogs.current(minecraftClient);
      if (var2 != null && var2.titled("teleport")) {
         if (var2.dialog() == this.pressed) {
            return true;
         } else if (System.currentTimeMillis() - this.sentAt > 3000L) {
            return false;
         } else {
            Dialogs.Button var3 = var2.labelled("confirm", "yes");
            if (var3 == null) {
               return false;
            } else {
               if (!this.warned) {
                  this.warned = true;
                  ModuleChat.send(
                     this.owner,
                     ModuleChat.body()
                        .warn("Your teleport confirm menu is on")
                        .text(". Turn it off in /settings so requests go out faster. Confirming each one for now.")
                  );
               }

               this.pressed = var2.dialog();
               Dialogs.press(minecraftClient, var2, var3, Map.of());
               return true;
            }
         }
      } else {
         return false;
      }
   }
}
