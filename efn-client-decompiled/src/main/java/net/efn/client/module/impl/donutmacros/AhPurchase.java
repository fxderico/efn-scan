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

   private AhPurchase(ClientModule clientModule, SnipeTarget snipeTarget, int i, long l, long m) {
      this.owner = clientModule;
      this.bot = SniperBot.quota(clientModule, this::stopped, this.worker, snipeTarget, i, l, m);
      this.chat = PacketReceiveNettyEvent.TYPE.listen(this, Stage.PRE, EventPriority.NORMAL, packetReceiveNettyEvent -> {
         if (this.status == AhPurchase.Status.RUNNING) {
            Packet var3 = packetReceiveNettyEvent.packet;
            GameMessageS2CPacket var2;
            if (!(var3 instanceof GameMessageS2CPacket) || (var2 = (GameMessageS2CPacket)var3).content() == null) {
               Packet var5 = packetReceiveNettyEvent.packet;
               OverlayMessageS2CPacket var4;
               if (var5 instanceof OverlayMessageS2CPacket && (var4 = (OverlayMessageS2CPacket)var5).text() != null) {
                  this.bot.onNote(var4.text().getString());
               }
            } else if (var2.overlay()) {
               this.bot.onNote(var2.content().getString());
            } else {
               this.bot.onChat(var2.content().getString());
            }
         }
      });
      this.worker.start(this.bot::run, throwable -> {
         LOGGER.error("[EFN] AH purchase stopped", throwable);
         ModuleChat.send(clientModule, ModuleChat.body().danger("AH purchase stopped").text(": " + throwable));
         this.stopped(this.bot);
      });
   }

   public static AhPurchase start(ClientModule clientModule, Item item, int i, long l, long m, long n) {
      SnipeTarget var9 = SnipeTarget.matching(
         item,
         new ItemStack(item).getName().getString(),
         Product.searchText(item),
         itemStack -> itemStack.isOf(item) && !itemStack.contains(DataComponentTypes.BLOCK_STATE) && !itemStack.contains(DataComponentTypes.BLOCK_ENTITY_DATA),
         l
      );
      return new AhPurchase(clientModule, var9, i, m, n);
   }

   public static AhPurchase startAny(
      ClientModule clientModule, Item item, String string, String string2, Predicate<ItemStack> predicate, int i, long l, long m, long n
   ) {
      return new AhPurchase(clientModule, SnipeTarget.matching(item, string, string2, predicate, l), i, m, n);
   }

   public static double unitPrice(ItemStack itemStack) {
      double var1 = ServerText.askingPrice(itemStack);
      return var1 <= 0.0 ? 1.7976931348623157E308 : var1 / Math.max(1, itemStack.getCount());
   }

   public static long parseMoney(String string) {
      return ServerText.parseMoney(string);
   }

   public static String money(double d) {
      return ServerText.shortMoney(d);
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

   private void stopped(SniperBot sniperBot) {
      this.finish(sniperBot.complete() ? AhPurchase.Status.DONE : AhPurchase.Status.FAILED);
   }

   private synchronized void finish(AhPurchase.Status status) {
      if (this.status == AhPurchase.Status.RUNNING) {
         this.status = status;
         this.chat.unregister();
         this.worker.stop();
         MinecraftClient.getInstance().execute(() -> {
            MinecraftClient var0 = MinecraftClient.getInstance();
            ClientPlayerEntity var1 = var0.player;
            if (var1 != null) {
               if (var1.currentScreenHandler != var1.playerScreenHandler) {
                  var1.closeHandledScreen();
               } else {
                  Dialogs.close(var0);
               }
            }
         });
         if (status == AhPurchase.Status.FAILED) {
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
