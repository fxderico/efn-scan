package net.efn.client.module.impl.misc;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import org.apache.commons.lang3.RandomStringUtils;

@Environment(EnvType.CLIENT)
public final class SpamModule extends ClientModule {
   public static volatile SpamModule active;
   private static final int MAX_MESSAGE_LENGTH = 256;
   private final StringListSetting messages = this.addSetting("messages", new StringListSetting("Messages", "EFN was here!"));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 20.0F, 0.0F, 1200.0F).step(1.0F));
   private final BooleanSetting disableOnLeave = this.addSetting("disable-on-leave", new BooleanSetting("Disable On Leave", true));
   private final BooleanSetting disableOnDisconnect = this.addSetting("disable-on-disconnect", new BooleanSetting("Disable On Disconnect", true));
   private final BooleanSetting random = this.addSetting("randomise", new BooleanSetting("Randomise", false));
   private final BooleanSetting autoSplitMessages = this.addSetting("auto-split-messages", new BooleanSetting("Auto Split Messages", false));
   private final NumberSetting splitLength = this.addSetting("split-length", new NumberSetting("Split Length", 256.0F, 1.0F, 256.0F).step(1.0F));
   private final NumberSetting autoSplitDelay = this.addSetting("split-delay", new NumberSetting("Split Delay", 20.0F, 0.0F, 200.0F).step(1.0F));
   private final BooleanSetting bypass = this.addSetting("bypass", new BooleanSetting("Bypass", false));
   private final BooleanSetting uppercase = this.addSetting("include-uppercase-characters", new BooleanSetting("Include Uppercase Characters", true));
   private final NumberSetting length = this.addSetting("length", new NumberSetting("Length", 16.0F, 1.0F, 256.0F).step(1.0F));
   private final Random rng = new Random();
   private int messageI;
   private int timer;
   private int splitNum;
   private String text;
   private boolean resetPending;

   public SpamModule() {
      super("spam", "Spam", "Spams specified messages in chat.", ModuleCategory.MISC);
      this.splitLength.visibleWhen(this.autoSplitMessages::get);
      this.autoSplitDelay.visibleWhen(this.autoSplitMessages::get);
      this.uppercase.visibleWhen(this.bypass::get);
      this.length.visibleWhen(this.bypass::get);
   }

   @Override
   public boolean inConfigs() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.resetPending = true;
      active = this;
      this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
   }

   @Override
   protected void onDisable() {
      active = null;
   }

   public static void onGameLeft() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (!var0.isOnThread()) {
         var0.execute(SpamModule::onGameLeft);
      } else {
         SpamModule var1 = active;
         if (var1 != null) {
            var1.handleGameLeft();
         }
      }
   }

   public static void onDisconnectedScreen() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      if (!var0.isOnThread()) {
         var0.execute(SpamModule::onDisconnectedScreen);
      } else {
         SpamModule var1 = active;
         if (var1 != null && var1.isEnabled() && var1.disableOnDisconnect.get()) {
            var1.requestDisable();
         }
      }
   }

   private void handleGameLeft() {
      if (this.isEnabled()) {
         if (this.disableOnLeave.get()) {
            this.requestDisable();
         } else {
            this.resetPending = true;
         }
      }
   }

   private void onPostTick(TickEvent tickEvent) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      ClientPlayerEntity var4 = var3.player;
      if (var4 != null && var3.world != null) {
         if (this.resetPending) {
            this.resetPending = false;
            this.timer = this.delay.getInt();
            this.messageI = 0;
            this.splitNum = 0;
         }

         List var2;
         if (!(var2 = this.messages.get()).isEmpty()) {
            if (this.timer > 0) {
               this.timer--;
            } else {
               if (this.text == null) {
                  int var5;
                  if (this.random.get()) {
                     var5 = this.rng.nextInt(var2.size());
                  } else {
                     if (this.messageI >= var2.size()) {
                        this.messageI = 0;
                     }

                     var5 = this.messageI++;
                  }

                  this.text = (String)var2.get(var5);
                  if (this.bypass.get()) {
                     String var6 = RandomStringUtils.insecure().nextAlphabetic(this.length.getInt());
                     if (!this.uppercase.get()) {
                        var6 = var6.toLowerCase(Locale.ROOT);
                     }

                     this.text = this.text + " " + var6;
                  }
               }

               int var9 = this.splitLength.getInt();
               if (this.autoSplitMessages.get() && this.text.length() > var9) {
                  int var10 = (this.text.length() + var9 - 1) / var9;
                  if (this.splitNum >= var10) {
                     this.splitNum = 0;
                  }

                  int var7 = this.splitNum * var9;
                  int var8 = Math.min(var7 + var9, this.text.length());
                  sendPlayerMsg(var3, var4.networkHandler, this.text.substring(var7, var8));
                  this.splitNum = (this.splitNum + 1) % var10;
                  this.timer = this.autoSplitDelay.getInt();
                  if (this.splitNum == 0) {
                     this.timer = this.delay.getInt();
                     this.text = null;
                  }
               } else {
                  if (this.text.length() > 256) {
                     this.text = this.text.substring(0, 256);
                  }

                  sendPlayerMsg(var3, var4.networkHandler, this.text);
                  this.timer = this.delay.getInt();
                  this.text = null;
               }
            }
         }
      }
   }

   private static void sendPlayerMsg(MinecraftClient minecraftClient, ClientPlayNetworkHandler clientPlayNetworkHandler, String string) {
      minecraftClient.inGameHud.getChatHud().addToMessageHistory(string);
      if (string.startsWith("/")) {
         clientPlayNetworkHandler.sendChatCommand(string.substring(1));
      } else {
         clientPlayNetworkHandler.sendChatMessage(string);
      }
   }
}
