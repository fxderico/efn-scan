package net.efn.client.module.impl.donutmacros;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.security.PayGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class PanicPayModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final Pattern VALID_NAME = Pattern.compile("\\.?[A-Za-z0-9_]{1,16}");
   private static final Pattern RATE_LIMIT = Pattern.compile("^you need to wait another ([\\d.]+) seconds?");
   private static final double MAX_WAIT_SECONDS = 30.0;
   private static final int INVENTORY_SLOTS = 36;
   private static final int CONFIRM_SLOT = 53;
   private static final int MAX_STRIKES = 2;
   private static final int MAX_SELL_ROUNDS = 6;
   private static final int[] ARMOR_MENU_SLOTS = new int[]{5, 6, 7, 8};
   private static final int OFFHAND_MENU_SLOT = 45;
   private static final long COMMAND_GAP_MS = 350L;
   private static final long OPEN_TIMEOUT_MS = 8000L;
   private static final long REPLY_TIMEOUT_MS = 6000L;
   private static final long MENU_SETTLE_MS = 50L;
   private static final long CONFIRM_WAIT_MS = 700L;
   private static final long RECONFIRM_WAIT_MS = 900L;
   private static final long RETURN_WAIT_MS = 600L;
   private final StringSetting payTo = this.addSetting("pay-to", new StringSetting("Pay To", "").hint("Player name"));
   private final BooleanSetting sellArmor = this.addSetting("sell-armor", new BooleanSetting("Sell Armor", true));
   private final BooleanSetting sellOffhand = this.addSetting("sell-offhand", new BooleanSetting("Sell Offhand", true));
   private final MacroWorker worker = new MacroWorker("EFN Panic Pay");
   private volatile boolean running;
   private volatile long paidAt;
   private volatile String paidText = "";
   private volatile long poorAt;
   private volatile long errorAt;
   private volatile String errorText = "";
   private volatile long holdUntil;
   private volatile long limitedAt;
   private volatile long balanceAt;
   private volatile double balance;
   private volatile String balanceText = "";
   private long lastCommandAt;

   public PanicPayModule() {
      super("panic-pay", "Panic Pay", "Sells your whole inventory and armor with /sell, then pays all your money to one player.", ModuleCategory.DONUT_MACROS);
      this.usesNotifications();
      this.payTo.sessionOnly();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   public boolean inConfigs() {
      return false;
   }

   @Override
   protected void onEnable() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      String var2 = this.payTo.get().trim();
      if (var1.player == null || var1.getNetworkHandler() == null) {
         this.say(ModuleChat.body().warn("Join a server first."));
         this.requestDisable();
      } else if (!VALID_NAME.matcher(var2).matches()) {
         this.say(ModuleChat.body().warn("Set Pay To to a player name first."));
         this.requestDisable();
      } else if (var2.equalsIgnoreCase(var1.getSession().getUsername())) {
         this.say(ModuleChat.body().warn("Pay To is your own name."));
         this.requestDisable();
      } else {
         boolean var3 = this.sellArmor.get();
         boolean var4 = this.sellOffhand.get();
         this.running = true;
         this.lastCommandAt = 0L;
         this.say(ModuleChat.body().danger("Panic Pay started").text(": selling everything, then paying ").name(var2).text(". Press the key again to cancel."));
         this.worker.start(() -> {
            try {
               this.run(new Gui(this.worker), var2, var3, var4);
            } catch (AuctionHouse.Problem var9) {
               this.say(ModuleChat.body().warn(var9.getMessage()).text(", stopped."));
            } finally {
               this.finish(var1);
            }
         }, throwable -> {
            LOGGER.error("[EFN] Panic Pay stopped", throwable);
            this.say(ModuleChat.body().danger("Stopped").text(": " + throwable));
            this.finish(var1);
         });
      }
   }

   private void finish(MinecraftClient minecraftClient) {
      minecraftClient.execute(() -> {
         if (this.running && this.isEnabled()) {
            this.setEnabled(false);
         }
      });
   }

   @Override
   protected void onDisable() {
      boolean var1 = this.running;
      this.running = false;
      this.worker.stop();
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var1 && var2 != null && var2.currentScreenHandler != var2.playerScreenHandler) {
         var2.closeHandledScreen();
      }
   }

   @EventHandler
   public void onPacket(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      if (this.running) {
         Packet var3 = packetReceiveNettyEvent.packet;
         GameMessageS2CPacket var2;
         if (packetReceiveNettyEvent.packet instanceof GameMessageS2CPacket && (var2 = (GameMessageS2CPacket)var3).content() != null) {
            String var4 = var2.content().getString();
            String var5 = ServerText.normalize(var4);
            long var6 = System.currentTimeMillis();
            Matcher var8 = RATE_LIMIT.matcher(var5);
            if (var8.find()) {
               try {
                  this.holdUntil = var6 + (long)Math.ceil(Math.min(30.0, Double.parseDouble(var8.group(1))) * 1000.0) + 100L;
                  this.limitedAt = var6;
               } catch (NumberFormatException var12) {
               }

               return;
            }

            if (var5.startsWith("you paid ")) {
               this.paidText = var5;
               this.paidAt = var6;
            } else if (var5.contains("enough money")) {
               this.poorAt = var6;
            } else if (!var5.startsWith("you have $") && (!var5.startsWith("balance") || !var5.contains("$"))) {
               if (var5.contains("not online")
                  || var5.contains("not found")
                  || var5.contains("cannot pay")
                  || var5.contains("can't pay")
                  || var5.contains("not a valid")
                  || var5.contains("invalid")
                  || var5.contains("doesn't exist")
                  || var5.contains("does not exist")) {
                  this.errorText = var4;
                  this.errorAt = var6;
               }
            } else {
               String var9 = var5.substring(var5.indexOf(36));
               List var10 = ServerText.moneyAll(var9);
               String var11 = ServerText.moneyToken(var9);
               if (!var10.isEmpty() && var11 != null) {
                  this.balance = (Double)var10.get(0);
                  this.balanceText = var11;
                  this.balanceAt = var6;
               }
            }

            return;
         }
      }
   }

   private void run(Gui gui, String string, boolean bl, boolean bl2) {
      gui.closeAll();
      HashMap var5 = new HashMap();
      int var6 = 0;

      try {
         if (bl || bl2) {
            this.takeOff(gui, bl, bl2, true);
         }

         var6 = this.sellRounds(gui, var5);
         if ((bl || bl2) && this.takeOff(gui, bl, bl2, false)) {
            var6 += this.sellRounds(gui, var5);
         }
      } catch (AuctionHouse.Problem var8) {
         this.say(ModuleChat.body().warn(var8.getMessage()).text(", paying what you have."));
         gui.closeAll();
      }

      this.say(ModuleChat.body().text("Sold ").value(var6).text(var6 == 1 ? " item" : " items").text(", paying ").name(string).text("."));
      this.payBalance(string);
   }

   private int sellRounds(Gui gui, Map<Item, Integer> map) {
      int var4 = 0;

      Map var3;
      for (int var5 = 0; var5 < 6 && !(var3 = this.worker.call(() -> inventoryCounts(map))).isEmpty(); var5++) {
         this.openSell(gui);
         int var6 = this.worker
            .call(
               () -> {
                  ClientPlayerEntity var1 = Gui.player();
                  ArrayList var2 = new ArrayList();

                  for (Slot var4x : var1.currentScreenHandler.slots) {
                     if (var4x.inventory instanceof PlayerInventory
                        && var4x.hasStack()
                        && var4x.getIndex() < 36
                        && map.getOrDefault(var4x.getStack().getItem(), 0) < 2) {
                        var2.add(var4x.id);
                     }
                  }

                  for (int var6x : var2) {
                     Gui.mc().interactionManager.clickSlot(var1.currentScreenHandler.syncId, var6x, 0, SlotActionType.QUICK_MOVE, var1);
                  }

                  return var2.size();
               }
            );
         this.worker.ticks(2);
         Map var7 = var6 > 0 ? this.worker.call(PanicPayModule::waitingItems) : Map.of();
         Map var8 = var7.isEmpty() ? var7 : this.confirm(gui, var7);
         gui.closeAll();
         if (var8 == null) {
            this.worker.sleep(600L);
         } else if (!var8.isEmpty()) {
            int var9 = total(var3) - total(var7) + total(var8);
            this.worker.waitFor(() -> total(inventoryCounts(map)) >= var9, 600L);
         }

         Map var14 = this.worker.call(() -> inventoryCounts(map));
         int var10 = 0;

         for (Entry var12 : var3.entrySet()) {
            var10 += Math.max(0, (Integer)var12.getValue() - var14.getOrDefault(var12.getKey(), 0));
         }

         var4 += var10;

         for (Entry var16 : var3.entrySet()) {
            if (var14.getOrDefault(var16.getKey(), 0) >= (Integer)var16.getValue()) {
               int var13 = var10 > 0 ? 2 : map.getOrDefault(var16.getKey(), 0) + 1;
               map.put((Item)var16.getKey(), var13);
               if (var13 == 2) {
                  this.say(
                     ModuleChat.body()
                        .warn("Could not sell ")
                        .name(new ItemStack((ItemConvertible)var16.getKey()).getName().getString())
                        .text(", skipping it.")
                  );
               }
            }
         }
      }

      return var4;
   }

   private Map<Item, Integer> confirm(Gui gui, Map<Item, Integer> map) {
      Map var3 = map;

      for (int var4 = 0; var4 < 2 && var3.equals(map); var4++) {
         if (!this.worker.call(PanicPayModule::sellMenuOpen)) {
            return null;
         }

         gui.click(53, 0, SlotActionType.PICKUP);
         Map var5 = var3;
         this.worker.waitFor(() -> !sellMenuOpen() || !waitingItems().equals(var5), var4 == 0 ? 700L : 900L);
         var3 = this.worker.call(() -> sellMenuOpen() ? waitingItems() : null);
         if (var3 == null) {
            return null;
         }
      }

      return var3;
   }

   private static Map<Item, Integer> waitingItems() {
      HashMap var0 = new HashMap();

      for (Slot var2 : Gui.player().currentScreenHandler.slots) {
         if (!(var2.inventory instanceof PlayerInventory) && var2.id != 53 && var2.hasStack()) {
            var0.merge(var2.getStack().getItem(), var2.getStack().getCount(), Integer::sum);
         }
      }

      return var0;
   }

   private static int total(Map<Item, Integer> map) {
      int var1 = 0;

      for (int var3 : map.values()) {
         var1 += var3;
      }

      return var1;
   }

   private static Map<Item, Integer> inventoryCounts(Map<Item, Integer> map) {
      HashMap var1 = new HashMap();
      PlayerInventory var2 = Gui.player().getInventory();

      for (int var3 = 0; var3 < 36; var3++) {
         ItemStack var4 = var2.getStack(var3);
         if (!var4.isEmpty() && map.getOrDefault(var4.getItem(), 0) < 2) {
            var1.merge(var4.getItem(), var4.getCount(), Integer::sum);
         }
      }

      return var1;
   }

   private void openSell(Gui gui) {
      for (int var2 = 0; var2 < 3; var2++) {
         long var3 = this.command("sell");
         this.worker.waitFor(() -> sellMenuOpen() || this.limitedAt >= var3, 8000L);
         if (this.worker.call(PanicPayModule::sellMenuOpen)) {
            this.worker.sleep(50L);
            return;
         }

         gui.closeAll();
      }

      throw new AuctionHouse.Problem("The sell menu did not open");
   }

   private static boolean sellMenuOpen() {
      ClientPlayerEntity var0 = Gui.mc().player;
      if (var0 != null && var0.currentScreenHandler != var0.playerScreenHandler && Gui.mc().currentScreen instanceof HandledScreen) {
         int var1 = 0;

         for (Slot var3 : var0.currentScreenHandler.slots) {
            if (!(var3.inventory instanceof PlayerInventory)) {
               var1++;
            }
         }

         return var1 > 53;
      } else {
         return false;
      }
   }

   private boolean takeOff(Gui gui, boolean bl, boolean bl2, boolean bl3) {
      gui.closeAll();
      boolean var5 = this.worker.call(() -> {
         ClientPlayerEntity var3 = Gui.player();
         ArrayList var4 = new ArrayList();
         if (bl) {
            for (int var8 : ARMOR_MENU_SLOTS) {
               if (var3.playerScreenHandler.getSlot(var8).hasStack()) {
                  var4.add(var8);
               }
            }
         }

         if (bl2 && var3.playerScreenHandler.getSlot(45).hasStack()) {
            var4.add(45);
         }

         int var9 = 0;

         for (int var10 = 0; var10 < 36; var10++) {
            if (var3.getInventory().getStack(var10).isEmpty()) {
               var9++;
            }
         }

         int var11 = bl3 ? Math.min(var9, var4.size()) : var4.size();

         for (int var12 = 0; var12 < var11; var12++) {
            Gui.mc().interactionManager.clickSlot(var3.playerScreenHandler.syncId, (Integer)var4.get(var12), 0, SlotActionType.QUICK_MOVE, var3);
         }

         return var11 > 0;
      });
      if (!var5) {
         return false;
      } else {
         gui.closeInventory();
         this.worker.ticks(2);
         return true;
      }
   }

   private void payBalance(String string) {
      String var2 = this.readBalance();
      if (this.balance < 1.0) {
         this.say(ModuleChat.body().warn("You have no money to pay."));
      } else {
         double var3 = ServerText.displayStep(this.balanceText);
         int var5 = 0;

         for (int var6 = 0; var6 < 4; var6++) {
            PanicPayModule.Payment var7 = this.pay(string, var2);
            if (var7 == PanicPayModule.Payment.PAID) {
               this.say(ModuleChat.body().good("Paid $" + var2).text(" to ").name(string).text("."));
               if (!mentions(this.paidText, string)) {
                  this.say(ModuleChat.body().danger("Check the payment").text(": the server said \"" + this.paidText + "\"."));
               }

               return;
            }

            if (var7 == PanicPayModule.Payment.POOR) {
               var5++;
               long var8 = (long)Math.floor(this.balance - var3 * var5);
               if (var5 > 2 || var8 < 1L) {
                  throw new AuctionHouse.Problem("/pay " + string + " " + var2 + " was refused: not enough money");
               }

               var2 = Long.toString(var8);
            } else if (var7 == PanicPayModule.Payment.FAILED) {
               throw new AuctionHouse.Problem("/pay failed: " + this.errorText);
            }
         }

         throw new AuctionHouse.Problem("No answer to /pay " + string + " " + var2);
      }
   }

   private String readBalance() {
      for (int var1 = 0; var1 < 3; var1++) {
         long var2 = this.command("bal");
         this.worker.waitFor(() -> this.balanceAt >= var2 || this.limitedAt >= var2, 6000L);
         if (this.balanceAt >= var2) {
            return this.balanceText;
         }
      }

      throw new AuctionHouse.Problem("Could not read your balance from /bal");
   }

   private PanicPayModule.Payment pay(String string, String string2) {
      long var3 = this.command("pay " + string + " " + string2);
      Object[] var5 = new Object[]{null};
      long var6 = var3 + 6000L;

      while (System.currentTimeMillis() < var6) {
         if (this.paidAt >= var3) {
            return PanicPayModule.Payment.PAID;
         }

         if (this.poorAt >= var3) {
            return PanicPayModule.Payment.POOR;
         }

         if (this.errorAt >= var3) {
            return PanicPayModule.Payment.FAILED;
         }

         if (this.limitedAt >= var3) {
            return PanicPayModule.Payment.RETRY;
         }

         boolean var8 = this.worker.call(() -> {
            Dialogs.View var2 = Dialogs.current(Gui.mc());
            if (var2 != null && var2.dialog() != var5[0]) {
               Dialogs.Button var3x = var2.labelled("confirm", "yes", "pay");
               if (var3x == null) {
                  return false;
               } else if (namesOtherPlayer(var2, string)) {
                  Dialogs.close(Gui.mc());
                  var5[0] = var2.dialog();
                  return true;
               } else {
                  if (Dialogs.press(Gui.mc(), var2, var3x, Map.of())) {
                     var5[0] = var2.dialog();
                  }

                  return false;
               }
            } else {
               return false;
            }
         });
         if (var8) {
            this.errorText = "the confirmation named a different player than " + string;
            return PanicPayModule.Payment.FAILED;
         }

         this.worker.sleep(50L);
      }

      return PanicPayModule.Payment.RETRY;
   }

   private static boolean mentions(String string, String string2) {
      String var2 = string2.toLowerCase(Locale.ROOT);
      if (var2.startsWith(".")) {
         var2 = var2.substring(1);
      }

      for (String var6 : string.split("[^a-z0-9_]+")) {
         if (var6.equals(var2)) {
            return true;
         }
      }

      return false;
   }

   private static boolean namesOtherPlayer(Dialogs.View view, String string) {
      String var2 = string.toLowerCase(Locale.ROOT);
      if (var2.startsWith(".")) {
         var2 = var2.substring(1);
      }

      ArrayList var3 = new ArrayList<>(view.body());
      var3.add(view.title());

      for (String var5 : var3) {
         for (String var9 : var5.split("[^a-z0-9_]+")) {
            if (var9.length() > var2.length() && var9.startsWith(var2)) {
               return true;
            }
         }
      }

      return false;
   }

   private long command(String string) {
      long var2 = Math.max(this.holdUntil, this.lastCommandAt + 350L) - System.currentTimeMillis();
      if (var2 > 0L) {
         this.worker.sleep(var2);
      }

      long var4 = System.currentTimeMillis();
      this.worker.run(() -> {
         PayGuard.allow(string);
         Gui.player().networkHandler.sendChatCommand(string);
      });
      this.lastCommandAt = var4;
      return var4;
   }

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this, body);
   }

   @Environment(EnvType.CLIENT)
   private static enum Payment {
      PAID,
      POOR,
      RETRY,
      FAILED;

      private Payment() {
      }
   }
}
