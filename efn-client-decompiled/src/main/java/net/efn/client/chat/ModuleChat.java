package net.efn.client.chat;

import net.efn.client.config.ThemeStore;
import net.efn.client.gui.ThemeTint;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class ModuleChat {
   private static final String HEADER = "EFN Client";
   private static final int DARK_GREY = 5592405;
   private static final int GREY = 11184810;
   private static final int WHITE = 16777215;
   private static final int DANGER = 16733525;
   private static final int GOOD = 5635925;
   private static final int WARN = 16755200;
   private static final int VALUE = 5636095;

   private ModuleChat() {
   }

   public static ModuleChat.Body body() {
      return new ModuleChat.Body();
   }

   public static void send(ClientModule clientModule, Text text) {
      onClientThread(() -> {
         if (clientModule.notificationsOn()) {
            addLine(clientModule, text);
         }
      });
   }

   public static void send(ClientModule clientModule, ModuleChat.Body body) {
      send(clientModule, body.build());
   }

   public static void send(ClientModule clientModule, String string) {
      send(clientModule, Text.literal(string));
   }

   public static void actionBar(ClientModule clientModule, ModuleChat.Body body) {
      Text var2 = body.build();
      onClientThread(() -> {
         MinecraftClient var2x = MinecraftClient.getInstance();
         if (clientModule.notificationsOn() && var2x.inGameHud != null) {
            var2x.inGameHud.setOverlayMessage(line(clientModule, var2), false);
         }
      });
   }

   public static void sendAs(String string, ModuleChat.Body body) {
      Text var2 = body.build();
      onClientThread(() -> {
         MinecraftClient var2x = MinecraftClient.getInstance();
         if (var2x.inGameHud != null) {
            var2x.inGameHud.getChatHud().addMessage(line(string, var2));
         }
      });
   }

   public static void sendAlways(ClientModule clientModule, ModuleChat.Body body) {
      Text var2 = body.build();
      onClientThread(() -> addLine(clientModule, var2));
   }

   public static void actionBarAlways(ClientModule clientModule, ModuleChat.Body body) {
      Text var2 = body.build();
      onClientThread(() -> {
         MinecraftClient var2x = MinecraftClient.getInstance();
         if (var2x.inGameHud != null) {
            var2x.inGameHud.setOverlayMessage(line(clientModule, var2), false);
         }
      });
   }

   private static void addLine(ClientModule clientModule, Text text) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.inGameHud != null) {
         var2.inGameHud.getChatHud().addMessage(line(clientModule, text));
      }
   }

   private static MutableText line(ClientModule clientModule, Text text) {
      return line(clientModule.name(), text);
   }

   private static MutableText line(String string, Text text) {
      return Text.empty()
         .append(Text.literal("[").withColor(5592405))
         .append(Text.literal("EFN Client").withColor(ThemeTint.accent(ThemeStore.get().accent.argb()) & 16777215))
         .append(Text.literal("] ").withColor(5592405))
         .append(Text.literal(string).withColor(16777215))
         .append(Text.literal(" : ").withColor(5592405))
         .append(Text.empty().withColor(11184810).append(text));
   }

   private static void onClientThread(Runnable runnable) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.isOnThread()) {
         runnable.run();
      } else {
         var1.execute(runnable);
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Body {
      private final MutableText root = Text.empty().withColor(11184810);

      private Body() {
      }

      public ModuleChat.Body text(String string) {
         this.root.append(Text.literal(string));
         return this;
      }

      public ModuleChat.Body name(String string) {
         return this.part(string, 16777215);
      }

      public ModuleChat.Body danger(String string) {
         return this.part(string, 16733525);
      }

      public ModuleChat.Body good(String string) {
         return this.part(string, 5635925);
      }

      public ModuleChat.Body warn(String string) {
         return this.part(string, 16755200);
      }

      public ModuleChat.Body value(Object object) {
         return this.part(String.valueOf(object), 5636095);
      }

      public ModuleChat.Body coords(int i, int j, int k) {
         return this.part(i + ", " + j + ", " + k, 5636095);
      }

      public ModuleChat.Body append(Text text) {
         this.root.append(text);
         return this;
      }

      public Text build() {
         return this.root.copy();
      }

      private ModuleChat.Body part(String string, int i) {
         this.root.append(Text.literal(string).withColor(i));
         return this;
      }
   }
}
