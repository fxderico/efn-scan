package net.efn.client.module.impl.donutmacros;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
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
public final class AhSellModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final int SLOTS = 9;
   private static final String FIXED = "Fixed";
   private static final String AUTO = "Auto (beat cheapest)";
   private final ModeSetting count = this.addSetting("items", new ModeSetting("Items", "1", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
   private final ModeSetting pricing = this.addSetting("pricing", new ModeSetting("Pricing", "Fixed", "Fixed", "Auto (beat cheapest)"));
   private final StringSetting[] items = new StringSetting[9];
   private final StringSetting[] enchants = new StringSetting[9];
   private final StringSetting[] prices = new StringSetting[9];
   private final StringSetting undercut;
   private final NumberSetting priceCheck;
   private final NumberSetting relistGap;
   private final NumberSetting ignoreDrops;
   private final NumberSetting salesCheck;
   private final NumberSetting perListing;
   private final NumberSetting maxListings;
   private final NumberSetting listingDelay;
   private final MacroWorker worker = new MacroWorker("EFN AH Sell");
   private volatile AhSellBot bot;

   public AhSellModule() {
      super(
         "ah-sell", "AH Sell", "Lists up to 9 items from your inventory on the Auction House at your price or the cheapest price.", ModuleCategory.DONUT_MACROS
      );
      this.usesNotifications();

      for (int var1 = 0; var1 < 9; var1++) {
         int var2 = var1 + 1;
         int var3 = var1;
         this.items[var1] = this.addSetting("item-" + var2, new StringSetting("Item " + var2, "").hint("e.g. diamond_block").itemPicker());
         this.enchants[var1] = this.addSetting("enchants-" + var2, new StringSetting("Enchantments " + var2, "").enchantsFor(this.items[var1]));
         this.prices[var1] = this.addSetting("price-" + var2, new StringSetting("Price " + var2 + " (each)", "").hint("e.g. 5k (Auto: lowest price)"));
         this.items[var1].visibleWhen(() -> var2 <= this.slotCount());
         this.enchants[var1].visibleWhen(() -> var2 <= this.slotCount() && ItemEnchants.supports(Product.resolveItem(this.items[var3].get())));
         this.prices[var1].visibleWhen(() -> var2 <= this.slotCount());
      }

      this.undercut = this.addSetting("undercut", new StringSetting("Undercut By", "100").hint("e.g. 100"));
      this.priceCheck = this.addSetting("price-check", new NumberSetting("Price Check (s)", 120.0F, 10.0F, 600.0F).step(10.0F));
      this.relistGap = this.addSetting("relist-gap", new NumberSetting("Relist Higher At (x undercut)", 3.0F, 1.0F, 20.0F).step(0.5F));
      this.ignoreDrops = this.addSetting("ignore-drops", new NumberSetting("Ignore Drops Over (x undercut)", 20.0F, 1.0F, 100.0F).step(1.0F));
      this.salesCheck = this.addSetting("sales-check", new NumberSetting("Sales Check (s)", 20.0F, 5.0F, 300.0F).step(5.0F));
      this.perListing = this.addSetting("per-listing", new NumberSetting("Items Per Listing", 1.0F, 1.0F, 64.0F).step(1.0F));
      this.maxListings = this.addSetting("max-listings", new NumberSetting("Max Listings", 25.0F, 1.0F, 50.0F).step(1.0F));
      this.listingDelay = this.addSetting("listing-delay", new NumberSetting("Listing Delay (s)", 0.0F, 0.0F, 5.0F).step(0.25F));
      this.undercut.visibleWhen(this::auto);
      this.priceCheck.visibleWhen(this::auto);
      this.relistGap.visibleWhen(this::auto);
      this.ignoreDrops.visibleWhen(this::auto);
   }

   private int slotCount() {
      return Integer.parseInt(this.count.get());
   }

   private boolean auto() {
      return this.pricing.is("Auto (beat cheapest)");
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 != null && var2.method_1548() != null) {
         ArrayList var3 = new ArrayList();
         String var4 = this.buildProducts(var3);
         if (var4 != null) {
            this.info(ModuleChat.body().warn(var4));
            this.requestDisable();
         } else {
            AuctionHouse.Config var5 = new AuctionHouse.Config(
               Math.max(0L, ServerText.parseMoney(this.undercut.get())),
               (long)(this.priceCheck.get() * 1000.0F),
               (long)(this.salesCheck.get() * 1000.0F),
               this.perListing.getInt(),
               this.maxListings.getInt(),
               (long)(this.listingDelay.get() * 1000.0F),
               this.relistGap.get(),
               this.ignoreDrops.get(),
               36,
               15000L
            );
            AhSellBot var1;
            this.bot = var1 = new AhSellBot(this, this.worker, var3, !this.auto(), var5, var2.method_1548().method_1676());
            this.worker.start(var1::run, var3x -> {
               LOGGER.error("[EFN] AH Sell stopped", var3x);
               this.info(ModuleChat.body().danger("Stopped").text(": " + var3x));
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
      AhSellBot var1 = this.bot;
      this.bot = null;
      this.worker.stop();
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var1 != null && var3 != null) {
         if (var3.field_7512 != var3.field_7498) {
            var3.method_7346();
         } else {
            Dialogs.close(var2);
         }

         this.info(ModuleChat.body().text(var1.summary()));
      }
   }

   @EventHandler
   public void onPacket(PacketReceiveNettyEvent var1) {
      AhSellBot var4 = this.bot;
      if (var4 != null) {
         Packet var3 = var1.packet;
         GameMessageS2CPacket var2;
         if (var1.packet instanceof GameMessageS2CPacket && (var2 = (GameMessageS2CPacket)var3).comp_763() != null) {
            var4.onChat(var2.comp_763().getString());
         }
      }
   }

   private String buildProducts(List<Product> var1) {
      if (this.auto() && ServerText.parseMoney(this.undercut.get()) < 0L) {
         return "Undercut By must be a price like 100.";
      } else {
         HashSet var2 = new HashSet();

         for (int var3 = 0; var3 < this.slotCount(); var3++) {
            String var4 = this.items[var3].get().trim();
            if (!var4.isEmpty()) {
               Item var5 = Product.resolveItem(var4);
               if (var5 == null) {
                  return "Item " + (var3 + 1) + ": unknown item '" + var4 + "'.";
               }

               if (!var2.add(var5)) {
                  return "Item " + (var3 + 1) + ": " + var4 + " is listed twice.";
               }

               long var6 = ServerText.parseMoney(this.prices[var3].get());
               if (var6 <= 0L) {
                  return "Item " + (var3 + 1) + ": set a price like 5k" + (this.auto() ? " (the lowest price Auto may use)." : ".");
               }

               List var8 = ItemEnchants.supports(var5) ? Product.parseEnchants(this.enchants[var3].get()) : List.of();
               var1.add(
                  new Product(var5, var4, Product.searchText(var5), new ItemStack(var5).method_7964().getString(), var6, var8, false, 1, true, 1L, 1L, 0L)
               );
            }
         }

         return var1.isEmpty() ? "Set at least one item." : null;
      }
   }

   private void info(ModuleChat.Body var1) {
      ModuleChat.send(this, var1);
   }
}
