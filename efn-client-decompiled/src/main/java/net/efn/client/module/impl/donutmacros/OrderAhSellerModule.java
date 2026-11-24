package net.efn.client.module.impl.donutmacros;

import java.util.List;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.ItemEnchants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class OrderAhSellerModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final int PRODUCTS = 5;
   private static final String PRICE_BEAT = "Beat Top Order";
   private static final String PRICE_FIXED = "Fixed";
   private final ModeSetting productSlot = this.addSetting("product", new ModeSetting("Product", "1", "1", "2", "3", "4", "5"));
   private final StringSetting[] items = new StringSetting[5];
   private final StringSetting[] minPrices = new StringSetting[5];
   private final StringSetting[] enchants = new StringSetting[5];
   private final BooleanSetting[] restock = new BooleanSetting[5];
   private final StringSetting[] orderAmounts = new StringSetting[5];
   private final ModeSetting[] priceModes = new ModeSetting[5];
   private final StringSetting[] fixedPrices = new StringSetting[5];
   private final StringSetting[] bidIncrements = new StringSetting[5];
   private final StringSetting[] maxPrices = new StringSetting[5];
   private final StringSetting undercut;
   private final NumberSetting priceCheck;
   private final NumberSetting salesCheck;
   private final NumberSetting perListing;
   private final NumberSetting maxListings;
   private final NumberSetting listingDelay;
   private final NumberSetting relistGap;
   private final NumberSetting ignoreDrops;
   private final NumberSetting collectSlots;
   private final NumberSetting orderCheck;
   private final MacroWorker worker = new MacroWorker("EFN Order AH Trader");
   private volatile OrderAhBot bot;

   public OrderAhSellerModule() {
      super(
         "order-ah-seller",
         "Order AH Trader",
         "Sells an item on the Auction House, undercuts other sellers and restocks through orders.",
         ModuleCategory.DONUT_MACROS
      );
      this.usesNotifications();

      for (int var1 = 0; var1 < 5; var1++) {
         int var3 = var1 + 1;
         String var4 = "product-" + var3 + "-";
         this.items[var1] = this.addSetting(
            var4 + "item", new StringSetting("Item", var3 == 1 ? "barrel" : "").hint("e.g. barrel or diamond_chestplate").itemPicker()
         );
         this.minPrices[var1] = this.addSetting(var4 + "min-price", new StringSetting("Min Price (each)", var3 == 1 ? "3k" : "1k").hint("e.g. 3k"));
         this.enchants[var1] = this.addSetting(var4 + "enchants", new StringSetting("Enchantments", "").enchantsFor(this.items[var1]));
         this.restock[var1] = this.addSetting(var4 + "restock", new BooleanSetting("Restock With Orders", false));
         this.orderAmounts[var1] = this.addSetting(var4 + "order-amount", new StringSetting("Order Amount", "64").hint("e.g. 64"));
         this.priceModes[var1] = this.addSetting(var4 + "order-price", new ModeSetting("Order Price", "Beat Top Order", "Beat Top Order", "Fixed"));
         this.fixedPrices[var1] = this.addSetting(var4 + "fixed-price", new StringSetting("Fixed Order Price (each)", "100").hint("e.g. 150k"));
         this.bidIncrements[var1] = this.addSetting(var4 + "bid-increment", new StringSetting("Beat By", "1").hint("e.g. 1 or 100"));
         this.maxPrices[var1] = this.addSetting(var4 + "max-price", new StringSetting("Max Order Price (each)", "").hint("Empty = no limit"));
         int var5 = var1;

         for (Setting var9 : new Setting[]{
            this.items[var1],
            this.minPrices[var1],
            this.enchants[var1],
            this.restock[var1],
            this.orderAmounts[var1],
            this.priceModes[var1],
            this.fixedPrices[var1],
            this.bidIncrements[var1],
            this.maxPrices[var1]
         }) {
            var9.visibleWhen(() -> this.selected() == var5);
         }

         this.enchants[var1].visibleWhen(() -> this.selected() == var5 && ItemEnchants.supports(Product.resolveItem(this.items[var5].get())));
         this.orderAmounts[var1].visibleWhen(() -> this.selected() == var5 && this.restock[var5].get());
         this.priceModes[var1].visibleWhen(() -> this.selected() == var5 && this.restock[var5].get());
         this.fixedPrices[var1].visibleWhen(() -> this.selected() == var5 && this.restock[var5].get() && this.priceModes[var5].is("Fixed"));
         this.bidIncrements[var1].visibleWhen(() -> this.selected() == var5 && this.restock[var5].get() && this.priceModes[var5].is("Beat Top Order"));
         this.maxPrices[var1].visibleWhen(() -> this.selected() == var5 && this.restock[var5].get());
      }

      this.undercut = this.addSetting("undercut", new StringSetting("Undercut By", "100").hint("e.g. 100"));
      this.priceCheck = this.addSetting("price-check", new NumberSetting("Price Check (s)", 120.0F, 10.0F, 600.0F).step(10.0F));
      this.salesCheck = this.addSetting("sales-check", new NumberSetting("Sales Check (s)", 20.0F, 5.0F, 300.0F).step(5.0F));
      this.perListing = this.addSetting("per-listing", new NumberSetting("Items Per Listing", 1.0F, 1.0F, 64.0F).step(1.0F));
      this.maxListings = this.addSetting("max-listings", new NumberSetting("Max Listings", 25.0F, 1.0F, 50.0F).step(1.0F));
      this.listingDelay = this.addSetting("listing-delay", new NumberSetting("Listing Delay (s)", 0.0F, 0.0F, 5.0F).step(0.25F));
      this.relistGap = this.addSetting("relist-gap", new NumberSetting("Relist Higher At (x undercut)", 3.0F, 1.0F, 20.0F).step(0.5F));
      this.ignoreDrops = this.addSetting("ignore-drops", new NumberSetting("Ignore Drops Over (x undercut)", 20.0F, 1.0F, 100.0F).step(1.0F));
      this.collectSlots = this.addSetting("collect-slots", new NumberSetting("Collect Up To (slots)", 18.0F, 1.0F, 36.0F).step(1.0F));
      this.orderCheck = this.addSetting("order-check", new NumberSetting("Order Check (s)", 15.0F, 5.0F, 300.0F).step(5.0F));
      this.collectSlots.visibleWhen(this::anyRestock);
      this.orderCheck.visibleWhen(this::anyRestock);
   }

   private int selected() {
      return Integer.parseInt(this.productSlot.get()) - 1;
   }

   private boolean anyRestock() {
      return this.restock[this.selected()].get();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null && var2.getSession() != null) {
         String var3 = this.validate();
         if (var3 != null) {
            this.info(ModuleChat.body().warn(var3));
            this.requestDisable();
         } else {
            OrderAhBot var1;
            this.bot = var1 = new OrderAhBot(this, this.worker, this.buildProduct(), this.buildConfig(), var2.getSession().getUsername());
            this.worker.start(var1::run, throwable -> {
               LOGGER.error("[EFN] Order AH Trader stopped", throwable);
               this.info(ModuleChat.body().danger("Stopped").text(": " + throwable));
               var2.execute(() -> {
                  if (this.bot == var1) {
                     this.setEnabled(false);
                  }
               });
            });
         }
      } else {
         this.info(ModuleChat.body().warn("Join a server first."));
         this.requestDisable();
      }
   }

   @Override
   protected void onDisable() {
      OrderAhBot var1 = this.bot;
      this.bot = null;
      this.worker.stop();
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var1 != null && var3 != null) {
         if (var3.currentScreenHandler != var3.playerScreenHandler) {
            var3.closeHandledScreen();
         } else {
            Dialogs.close(var2);
         }

         this.info(ModuleChat.body().text(var1.summary()));
      }
   }

   @EventHandler
   public void onPacket(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      OrderAhBot var4 = this.bot;
      if (var4 != null) {
         Packet var3 = packetReceiveNettyEvent.packet;
         GameMessageS2CPacket var2;
         if (packetReceiveNettyEvent.packet instanceof GameMessageS2CPacket && (var2 = (GameMessageS2CPacket)var3).content() != null) {
            var4.onChat(var2.content().getString());
         }
      }
   }

   private String validate() {
      int var1 = this.selected();
      Item var2 = Product.resolveItem(this.items[var1].get());
      if (var2 == null) {
         return "Product " + (var1 + 1) + ": unknown item '" + this.items[var1].get() + "'.";
      } else if (ServerText.parseMoney(this.minPrices[var1].get()) <= 0L) {
         return "Product " + (var1 + 1) + ": Min Price must be a price like 3k.";
      } else if (ServerText.parseMoney(this.undercut.get()) < 0L) {
         return "Undercut By must be a price like 100.";
      } else if (!this.restock[var1].get()) {
         return null;
      } else if (parseAmount(this.orderAmounts[var1].get()) <= 0) {
         return "Product " + (var1 + 1) + ": Order Amount must be a whole number.";
      } else if (this.priceModes[var1].is("Fixed") && ServerText.parseMoney(this.fixedPrices[var1].get()) <= 0L) {
         return "Product " + (var1 + 1) + ": Fixed Order Price must be a price like 150k.";
      } else if (this.priceModes[var1].is("Beat Top Order") && ServerText.parseMoney(this.bidIncrements[var1].get()) <= 0L) {
         return "Product " + (var1 + 1) + ": Beat By must be a price like 1.";
      } else {
         return !this.maxPrices[var1].get().isBlank() && ServerText.parseMoney(this.maxPrices[var1].get()) <= 0L
            ? "Product " + (var1 + 1) + ": Max Order Price must be empty or a price like 200k."
            : null;
      }
   }

   private Product buildProduct() {
      int var1 = this.selected();
      Item var2 = Product.resolveItem(this.items[var1].get());
      long var3 = this.maxPrices[var1].get().isBlank() ? 0L : ServerText.parseMoney(this.maxPrices[var1].get());
      return new Product(
         var2,
         this.items[var1].get().trim(),
         Product.searchText(var2),
         new ItemStack(var2).getName().getString(),
         ServerText.parseMoney(this.minPrices[var1].get()),
         ItemEnchants.supports(var2) ? Product.parseEnchants(this.enchants[var1].get()) : List.of(),
         this.restock[var1].get(),
         parseAmount(this.orderAmounts[var1].get()),
         this.priceModes[var1].is("Beat Top Order"),
         Math.max(1L, ServerText.parseMoney(this.fixedPrices[var1].get())),
         Math.max(1L, ServerText.parseMoney(this.bidIncrements[var1].get())),
         Math.max(0L, var3)
      );
   }

   private AuctionHouse.Config buildConfig() {
      return new AuctionHouse.Config(
         Math.max(0L, ServerText.parseMoney(this.undercut.get())),
         (long)(this.priceCheck.get() * 1000.0F),
         (long)(this.salesCheck.get() * 1000.0F),
         this.perListing.getInt(),
         this.maxListings.getInt(),
         (long)(this.listingDelay.get() * 1000.0F),
         this.relistGap.get(),
         this.ignoreDrops.get(),
         this.collectSlots.getInt(),
         (long)(this.orderCheck.get() * 1000.0F)
      );
   }

   private static int parseAmount(String string) {
      try {
         int var1 = Integer.parseInt(string.trim());
         return var1 > 0 ? var1 : -1;
      } catch (NumberFormatException var2) {
         return -1;
      }
   }

   private void info(ModuleChat.Body body) {
      ModuleChat.send(this, body);
   }
}
