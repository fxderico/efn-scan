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

   public static void send(ClientModule var0, Text var1) {
      onClientThread(() -> {
         if (var0.notificationsOn()) {
            addLine(var0, var1);
         }
      });
   }

   public static void send(ClientModule var0, ModuleChat.Body var1) {
      send(var0, var1.build());
   }

   public static void send(ClientModule var0, String var1) {
      send(var0, Text.method_43470(var1));
   }

   public static void actionBar(ClientModule var0, ModuleChat.Body var1) {
      Text var2 = var1.build();
      onClientThread(() -> {
         MinecraftClient var2x = MinecraftClient.method_1551();
         if (var0.notificationsOn() && var2x.field_1705 != null) {
            var2x.field_1705.method_1758(line(var0, var2), false);
         }
      });
   }

   public static void sendAs(String var0, ModuleChat.Body var1) {
      Text var2 = var1.build();
      onClientThread(() -> {
         MinecraftClient var2x = MinecraftClient.method_1551();
         if (var2x.field_1705 != null) {
            var2x.field_1705.method_1743().method_1812(line(var0, var2));
         }
      });
   }

   public static void sendAlways(ClientModule var0, ModuleChat.Body var1) {
      Text var2 = var1.build();
      onClientThread(() -> addLine(var0, var2));
   }

   public static void actionBarAlways(ClientModule var0, ModuleChat.Body var1) {
      Text var2 = var1.build();
      onClientThread(() -> {
         MinecraftClient var2x = MinecraftClient.method_1551();
         if (var2x.field_1705 != null) {
            var2x.field_1705.method_1758(line(var0, var2), false);
         }
      });
   }

   private static void addLine(ClientModule var0, Text var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1705 != null) {
         var2.field_1705.method_1743().method_1812(line(var0, var1));
      }
   }

   private static MutableText line(ClientModule var0, Text var1) {
      return line(var0.name(), var1);
   }

   private static MutableText line(String var0, Text var1) {
      return Text.method_43473()
         .method_10852(Text.method_43470("[").method_54663(5592405))
         .method_10852(Text.method_43470("EFN Client").method_54663(ThemeTint.accent(ThemeStore.get().accent.argb()) & 16777215))
         .method_10852(Text.method_43470("] ").method_54663(5592405))
         .method_10852(Text.method_43470(var0).method_54663(16777215))
         .method_10852(Text.method_43470(" : ").method_54663(5592405))
         .method_10852(Text.method_43473().method_54663(11184810).method_10852(var1));
   }

   private static void onClientThread(Runnable var0) {
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (var1.method_18854()) {
         var0.run();
      } else {
         var1.execute(var0);
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Body {
      private final MutableText root = Text.method_43473().method_54663(11184810);

      private Body() {
      }

      public ModuleChat.Body text(String var1) {
         this.root.method_10852(Text.method_43470(var1));
         return this;
      }

      public ModuleChat.Body name(String var1) {
         return this.part(var1, 16777215);
      }

      public ModuleChat.Body danger(String var1) {
         return this.part(var1, 16733525);
      }

      public ModuleChat.Body good(String var1) {
         return this.part(var1, 5635925);
      }

      public ModuleChat.Body warn(String var1) {
         return this.part(var1, 16755200);
      }

      public ModuleChat.Body value(Object var1) {
         return this.part(String.valueOf(var1), 5636095);
      }

      public ModuleChat.Body coords(int var1, int var2, int var3) {
         return this.part(var1 + ", " + var2 + ", " + var3, 5636095);
      }

      public ModuleChat.Body append(Text var1) {
         this.root.method_10852(var1);
         return this;
      }

      public Text build() {
         return this.root.method_27661();
      }

      private ModuleChat.Body part(String var1, int var2) {
         this.root.method_10852(Text.method_43470(var1).method_54663(var2));
         return this;
      }
   }
}
