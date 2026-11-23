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

   private DonutSell(ClientModule var1, Map<Integer, Item> var2) {
      Map var3 = Map.copyOf(var2);
      this.routine = new SellRoutine(
         var1,
         new StringListSetting("Never Sell"),
         (var1x, var2x) -> var3.get(var1x) == null || !var2x.method_31574((Item)var3.get(var1x)),
         () -> this.sent = true
      );
      this.chat = PacketReceiveNettyEvent.TYPE.listen(this, Stage.PRE, EventPriority.NORMAL, var1x -> {
         Packet var3x = var1x.packet;
         GameMessageS2CPacket var2x;
         if (var3x instanceof GameMessageS2CPacket && (var2x = (GameMessageS2CPacket)var3x).comp_763() != null) {
            Matcher var4 = RATE_LIMIT.matcher(ServerText.normalize(var2x.comp_763().getString()));
            if (var4.find()) {
               try {
                  long var5 = System.currentTimeMillis() + (long)Math.ceil(Double.parseDouble(var4.group(1)) * 1000.0) + 100L;
                  MinecraftClient.method_1551().execute(() -> this.routine.commandRejected(var5));
               } catch (NumberFormatException var7) {
               }
            }
         }
      });
   }

   public static DonutSell start(ClientModule var0, Map<Integer, Item> var1) {
      return new DonutSell(var0, var1);
   }

   public boolean tick() {
      if (this.done) {
         return false;
      } else {
         MinecraftClient var1 = MinecraftClient.method_1551();
         ClientPlayerEntity var2 = var1.field_1724;
         if (var2 != null && var1.method_1562() != null && var1.field_1761 != null) {
            long var3 = System.currentTimeMillis();
            boolean var5 = this.routine.step(var1, var2, var1.method_1562(), var3, true);
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
         this.routine.abort(MinecraftClient.method_1551());
         this.finish();
      }
   }

   private void finish() {
      this.done = true;
      this.chat.unregister();
   }
}
