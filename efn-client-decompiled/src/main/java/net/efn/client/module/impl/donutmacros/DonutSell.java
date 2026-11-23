package net.efn.client.module.impl.donutmacros;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.efn.client.event.EventPriority;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@Environment(EnvType.CLIENT)
public final class DonutSell {
   private static final Pattern RATE_LIMIT = Pattern.compile("you need to wait another ([\\d.]+) seconds?");
   private static final long GIVE_UP_MS = 45000L;
   private final SellRoutine routine;
   private final ListenerHandle<PacketReceiveNettyEvent> chat;
   private final long started = System.currentTimeMillis();
   private boolean sent;
   private boolean done;
   private int ticks;

   private DonutSell(ClientModule clientModule, Map<Integer, Item> map) {
      Map var3 = Map.copyOf(map);
      this.routine = new SellRoutine(
         clientModule,
         new StringListSetting("Never Sell"),
         (integer, itemStack) -> var3.get(integer) == null || !itemStack.isOf((Item)var3.get(integer)),
         () -> this.sent = true
      );
      this.chat = PacketReceiveNettyEvent.TYPE.listen(this, Stage.PRE, EventPriority.NORMAL, packetReceiveNettyEvent -> {
         Packet var3x = packetReceiveNettyEvent.packet;
         GameMessageS2CPacket var2;
         if (var3x instanceof GameMessageS2CPacket && (var2 = (GameMessageS2CPacket)var3x).content() != null) {
            Matcher var4 = RATE_LIMIT.matcher(ServerText.normalize(var2.content().getString()));
            if (var4.find()) {
               try {
                  long var5 = System.currentTimeMillis() + (long)Math.ceil(Double.parseDouble(var4.group(1)) * 1000.0) + 100L;
                  MinecraftClient.getInstance().execute(() -> this.routine.commandRejected(var5));
               } catch (NumberFormatException var7) {
               }
            }
         }
      });
   }

   public static DonutSell start(ClientModule clientModule, Map<Integer, Item> map) {
      return new DonutSell(clientModule, map);
   }

   public boolean tick() {
      if (this.done) {
         return false;
      } else {
         MinecraftClient var1 = MinecraftClient.getInstance();
         ClientPlayerEntity var2 = var1.player;
         if (var2 != null && var1.getNetworkHandler() != null && var1.interactionManager != null) {
            long var3 = System.currentTimeMillis();
            boolean var5 = this.routine.step(var1, var2, var1.getNetworkHandler(), var3, true);
            this.ticks++;
            if (!var5 && !this.routine.active()) {
               if (this.sent || this.ticks > 3 || var3 - this.started > 45000L) {
                  this.finish();
               }

               return !this.done;
            } else {
               if (var3 - this.started > 45000L) {
                  this.cancel();
               }

               return !this.done;
            }
         } else {
            this.cancel();
            return false;
         }
      }
   }

   public void cancel() {
      if (!this.done) {
         this.routine.abort(MinecraftClient.getInstance());
         this.finish();
      }
   }

   private void finish() {
      this.done = true;
      this.chat.unregister();
   }
}
