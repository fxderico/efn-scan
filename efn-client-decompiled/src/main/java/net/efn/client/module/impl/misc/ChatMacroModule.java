package net.efn.client.module.impl.misc;

import java.util.Locale;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.StringHelper;
import org.apache.commons.lang3.StringUtils;

@Environment(EnvType.CLIENT)
public final class ChatMacroModule extends ClientModule {
   public static final int MAX_MACROS = 8;
   private static final String ACTION_SEND = "Send";
   private static final String ACTION_OPEN = "Open in Chat";
   private final ModeSetting slots = this.addSetting("slots", new ModeSetting("Slots", "3", slotChoices()));
   private final ModeSetting pressAction = this.addSetting("on-press", new ModeSetting("On Press", "Send", "Send", "Open in Chat"));
   private final NumberSetting cooldown = this.addSetting("cooldown", new NumberSetting("Cooldown (s)", 1.0F, 1.0F, 10.0F).step(0.5F));
   private final KeySetting[] keys = new KeySetting[8];
   private final StringSetting[] texts = new StringSetting[8];
   private int pendingSlot = -1;
   private long lastSendNanos;
   private boolean sentAny;

   public ChatMacroModule() {
      super("chat-macro", "Chat Macro", "Sends chat messages or commands with a key press.", ModuleCategory.MISC);
      this.usesNotifications();
      this.cooldown.visibleWhen(() -> this.pressAction.is("Send"));

      for (int var1 = 0; var1 < 8; var1++) {
         int var2 = var1 + 1;
         this.keys[var1] = this.addSetting("macro-" + var2 + "-key", new KeySetting("Macro " + var2 + " Key", -1));
         this.texts[var1] = this.addSetting("macro-" + var2 + "-text", new StringSetting("Macro " + var2 + " Text", ""));
         this.texts[var1].privateValue();
         this.keys[var1].visibleWhen(() -> var2 <= this.slotCount());
         this.texts[var1].visibleWhen(() -> var2 <= this.slotCount());
      }
   }

   @Override
   public boolean inConfigs() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.pendingSlot = -1;
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
      this.on(TickEvent.TYPE, Stage.PRE, this::onTick);
   }

   @Override
   protected void onDisable() {
      this.pendingSlot = -1;
   }

   private void onKey(KeyInputEvent var1) {
      if (var1.action == 1) {
         this.onPress(var1.key);
      }
   }

   private void onMouse(MouseInputEvent var1) {
      if (var1.action == 1) {
         this.onPress(1000 + var1.button);
      }
   }

   private void onPress(int var1) {
      if (ModuleKeybinds.isBindable(var1) && this.pendingSlot == -1) {
         MinecraftClient var2 = MinecraftClient.method_1551();
         if (var2.field_1755 == null && var2.field_1724 != null) {
            int var3 = this.slotCount();

            for (int var4 = 0; var4 < var3; var4++) {
               if (this.keys[var4].value() == var1 && !this.texts[var4].get().isBlank()) {
                  this.pendingSlot = var4;
                  return;
               }
            }
         }
      }
   }

   private void onTick(TickEvent var1) {
      int var2 = this.pendingSlot;
      if (var2 >= 0) {
         this.pendingSlot = -1;
         MinecraftClient var3 = MinecraftClient.method_1551();
         ClientPlayNetworkHandler var4 = var3.method_1562();
         if (var3.field_1724 != null && var4 != null && var3.field_1755 == null) {
            String var5 = normalize(this.texts[var2].get());
            if (!var5.isEmpty() && !var5.equals("/")) {
               if (this.pressAction.is("Open in Chat")) {
                  var3.method_1507(new ChatScreen(var5, false));
               } else {
                  long var6 = System.nanoTime();
                  long var8 = (long)(this.cooldown.get() * 1.0E9) - (var6 - this.lastSendNanos);
                  if (this.sentAny && var8 > 0L) {
                     double var10 = Math.ceil(var8 / 1.0E8) / 10.0;
                     ModuleChat.actionBar(
                        this, ModuleChat.body().warn("On cooldown").text(", ").value(String.format(Locale.ROOT, "%.1fs", var10)).text(" left")
                     );
                  } else {
                     this.lastSendNanos = var6;
                     this.sentAny = true;
                     var3.field_1705.method_1743().method_1803(var5);
                     if (var5.startsWith("/")) {
                        var4.method_45730(var5.substring(1));
                     } else {
                        var4.method_45729(var5);
                     }
                  }
               }
            }
         }
      }
   }

   static String normalize(String var0) {
      String var1 = StringHelper.method_57180(var0);
      return StringHelper.method_43681(StringUtils.normalizeSpace(var1.trim()));
   }

   private int slotCount() {
      return Integer.parseInt(this.slots.get());
   }

   private static String[] slotChoices() {
      String[] var0 = new String[8];

      for (int var1 = 0; var1 < 8; var1++) {
         var0[var1] = Integer.toString(var1 + 1);
      }

      return var0;
   }
}
