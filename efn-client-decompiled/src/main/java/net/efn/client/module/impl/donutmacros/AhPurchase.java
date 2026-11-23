package net.efn.client.module.impl.donutmacros;

import java.util.function.Predicate;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventPriority;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class AhPurchase {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private final ClientModule owner;
   private final MacroWorker worker = new MacroWorker("EFN AH Purchase");
   private final SniperBot bot;
   private final ListenerHandle<PacketReceiveNettyEvent> chat;
   private volatile AhPurchase.Status status = AhPurchase.Status.RUNNING;

   private AhPurchase(ClientModule var1, SnipeTarget var2, int var3, long var4, long var6) {
      this.owner = var1;
      this.bot = SniperBot.quota(var1, this::stopped, this.worker, var2, var3, var4, var6);
      this.chat = PacketReceiveNettyEvent.TYPE.listen(this, Stage.PRE, EventPriority.NORMAL, var1x -> {
         if (this.status == AhPurchase.Status.RUNNING) {
            Packet var3x = var1x.packet;
            GameMessageS2CPacket var2x;
            if (!(var3x instanceof GameMessageS2CPacket) || (var2x = (GameMessageS2CPacket)var3x).comp_763() == null) {
               Packet var5 = var1x.packet;
               OverlayMessageS2CPacket var4x;
               if (var5 instanceof OverlayMessageS2CPacket && (var4x = (OverlayMessageS2CPacket)var5).comp_2279() != null) {
                  this.bot.onNote(var4x.comp_2279().getString());
               }
            } else if (var2x.comp_906()) {
               this.bot.onNote(var2x.comp_763().getString());
            } else {
               this.bot.onChat(var2x.comp_763().getString());
            }
         }
      });
      this.worker.start(this.bot::run, var2x -> {
         LOGGER.error("[EFN] AH purchase stopped", var2x);
         ModuleChat.send(var1, ModuleChat.body().danger("AH purchase stopped").text(": " + var2x));
         this.stopped(this.bot);
      });
   }

   public static AhPurchase start(ClientModule var0, Item var1, int var2, long var3, long var5, long var7) {
      SnipeTarget var9 = SnipeTarget.matching(
         var1,
         new ItemStack(var1).method_7964().getString(),
         Product.searchText(var1),
         var1x -> var1x.method_31574(var1) && !var1x.method_57826(DataComponentTypes.field_49623) && !var1x.method_57826(DataComponentTypes.field_49611),
         var3
      );
      return new AhPurchase(var0, var9, var2, var5, var7);
   }

   public static AhPurchase startAny(
      ClientModule var0, Item var1, String var2, String var3, Predicate<ItemStack> var4, int var5, long var6, long var8, long var10
   ) {
      return new AhPurchase(var0, SnipeTarget.matching(var1, var2, var3, var4, var6), var5, var8, var10);
   }

   public static double unitPrice(ItemStack var0) {
      double var1 = ServerText.askingPrice(var0);
      return var1 <= 0.0 ? 1.7976931348623157E308 : var1 / Math.max(1, var0.method_7947());
   }

   public static long parseMoney(String var0) {
      return ServerText.parseMoney(var0);
   }

   public static String money(double var0) {
      return ServerText.shortMoney(var0);
   }

   public AhPurchase.Status status() {
      return this.status;
   }

   public int boughtItems() {
      return this.bot.boughtItems();
   }

   public long spent() {
      return this.bot.spent();
   }

   public void cancel() {
      if (this.status == AhPurchase.Status.RUNNING) {
         this.finish(AhPurchase.Status.FAILED);
      }
   }

   private void stopped(SniperBot var1) {
      this.finish(var1.complete() ? AhPurchase.Status.DONE : AhPurchase.Status.FAILED);
   }

   private synchronized void finish(AhPurchase.Status var1) {
      if (this.status == AhPurchase.Status.RUNNING) {
         this.status = var1;
         this.chat.unregister();
         this.worker.stop();
         MinecraftClient.method_1551().execute(() -> {
            MinecraftClient var0 = MinecraftClient.method_1551();
            ClientPlayerEntity var1x = var0.field_1724;
            if (var1x != null) {
               if (var1x.field_7512 != var1x.field_7498) {
                  var1x.method_7346();
               } else {
                  Dialogs.close(var0);
               }
            }
         });
         if (var1 == AhPurchase.Status.FAILED) {
            LOGGER.info("[EFN] AH purchase for {} ended without finishing", this.owner.name());
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Status {
      RUNNING,
      DONE,
      FAILED;

      private Status() {
      }
   }
}
