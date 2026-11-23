package net.efn.client.module.impl.donutmacros;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@Environment(EnvType.CLIENT)
public final class TpMacroModule extends ClientModule {
   private static final Pattern RATE_LIMIT = Pattern.compile("^you need to wait another ([\\d.]+) seconds? to execute a command");
   private static final Pattern VALID_NAME = Pattern.compile("\\.?[A-Za-z0-9_]{1,16}");
   private final ModeSetting command = this.addSetting("command", new ModeSetting("Command", "tpa", "tpa", "tpahere"));
   private final StringSetting target = this.addSetting("player", new StringSetting("Player", "").hint("Player name"));
   private final NumberSetting interval = this.addSetting("interval", new NumberSetting("Interval (s)", 1.0F, 0.25F, 60.0F).step(0.25F));
   private final TeleportConfirm confirm = new TeleportConfirm(this);
   private volatile long holdUntil;
   private long nextAt;
   private int sent;

   public TpMacroModule() {
      super("tp-macro", "Tp Macro", "Sends /tpa or /tpahere to one player on a timer.", ModuleCategory.DONUT_MACROS);
      this.usesNotifications();
      this.target.privateValue();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      String var1 = this.target.get().trim();
      if (!VALID_NAME.matcher(var1).matches()) {
         ModuleChat.send(this, ModuleChat.body().warn("Set a valid Player name first."));
         this.requestDisable();
      } else {
         this.confirm.reset();
         this.holdUntil = 0L;
         this.nextAt = 0L;
         this.sent = 0;
         ModuleChat.send(
            this,
            ModuleChat.body().text("Sending ").value("/" + this.command.get() + " " + var1).text(" every ").value(this.interval.formatted() + "s").text(".")
         );
      }
   }

   @Override
   protected void onDisable() {
      if (this.sent > 0) {
         ModuleChat.send(this, ModuleChat.body().text("Sent ").value(this.sent).text(this.sent == 1 ? " request." : " requests."));
      }
   }

   @EventHandler
   public void onPacket(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      Packet var3 = packetReceiveNettyEvent.packet;
      GameMessageS2CPacket var2;
      if (var3 instanceof GameMessageS2CPacket && (var2 = (GameMessageS2CPacket)var3).content() != null) {
         Matcher var4 = RATE_LIMIT.matcher(ServerText.normalize(var2.content().getString()));
         if (var4.find()) {
            try {
               this.holdUntil = System.currentTimeMillis() + (long)Math.ceil(Math.min(30.0, Double.parseDouble(var4.group(1))) * 1000.0) + 80L;
            } catch (NumberFormatException var6) {
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null && var2.getNetworkHandler() != null && !var3.isDead()) {
         this.confirm.tick(var2);
         long var4 = System.currentTimeMillis();
         if (var4 >= this.nextAt && var4 >= this.holdUntil) {
            var3.networkHandler.sendChatCommand(this.command.get() + " " + this.target.get().trim());
            this.confirm.sent();
            this.sent++;
            this.nextAt = var4 + (long)(this.interval.get() * 1000.0F);
         }
      }
   }
}
