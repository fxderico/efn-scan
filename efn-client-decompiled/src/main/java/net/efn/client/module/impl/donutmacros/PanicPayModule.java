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
      MinecraftClient var1 = MinecraftClient.method_1551();
      String var2 = this.payTo.get().trim();
      if (var1.field_1724 == null || var1.method_1562() == null) {
         this.say(ModuleChat.body().warn("Join a server first."));
         this.requestDisable();
      } else if (!VALID_NAME.matcher(var2).matches()) {
         this.say(ModuleChat.body().warn("Set Pay To to a player name first."));
         this.requestDisable();
      } else if (var2.equalsIgnoreCase(var1.method_1548().method_1676())) {
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
         }, var2x -> {
            LOGGER.error("[EFN] Panic Pay stopped", var2x);
            this.say(ModuleChat.body().danger("Stopped").text(": " + var2x));
            this.finish(var1);
         });
      }
   }

   private void finish(MinecraftClient var1) {
      var1.execute(() -> {
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
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var1 && var2 != null && var2.field_7512 != var2.field_7498) {
         var2.method_7346();
      }
   }

   @EventHandler
   public void onPacket(PacketReceiveNettyEvent var1) {
      if (this.running) {
         Packet var3 = var1.packet;
         GameMessageS2CPacket var2;
         if (var1.packet instanceof GameMessageS2CPacket && (var2 = (GameMessageS2CPacket)var3).comp_763() != null) {
            String var4 = var2.comp_763().getString();
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

   private void run(Gui var1, String var2, boolean var3, boolean var4) {
      var1.closeAll();
      HashMap var5 = new HashMap();
      int var6 = 0;

      try {
         if (var3 || var4) {
            this.takeOff(var1, var3, var4, true);
         }

         var6 = this.sellRounds(var1, var5);
         if ((var3 || var4) && this.takeOff(var1, var3, var4, false)) {
            var6 += this.sellRounds(var1, var5);
         }
      } catch (AuctionHouse.Problem var8) {
         this.say(ModuleChat.body().warn(var8.getMessage()).text(", paying what you have."));
         var1.closeAll();
      }

      this.say(ModuleChat.body().text("Sold ").value(var6).text(var6 == 1 ? " item" : " items").text(", paying ").name(var2).text("."));
      this.payBalance(var2);
   }

   private int sellRounds(Gui var1, Map<Item, Integer> var2) {
      int var4 = 0;

      Map var3;
      for (int var5 = 0; var5 < 6 && !(var3 = this.worker.call(() -> inventoryCounts(var2))).isEmpty(); var5++) {
         this.openSell(var1);
         int var6 = this.worker
            .call(
               () -> {
                  ClientPlayerEntity var1x = Gui.player();
                  ArrayList var2x = new ArrayList();

                  for (Slot var4x : var1x.field_7512.field_7761) {
                     if (var4x.field_7871 instanceof PlayerInventory
                        && var4x.method_7681()
                        && var4x.method_34266() < 36
                        && var2.getOrDefault(var4x.method_7677().method_7909(), 0) < 2) {
                        var2x.add(var4x.field_7874);
                     }
                  }

                  for (int var6x : var2x) {
                     Gui.mc().field_1761.method_2906(var1x.field_7512.field_7763, var6x, 0, SlotActionType.field_7794, var1x);
                  }

                  return var2x.size();
               }
            );
         this.worker.ticks(2);
         Map var7 = var6 > 0 ? this.worker.call(PanicPayModule::waitingItems) : Map.of();
         Map var8 = var7.isEmpty() ? var7 : this.confirm(var1, var7);
         var1.closeAll();
         if (var8 == null) {
            this.worker.sleep(600L);
         } else if (!var8.isEmpty()) {
            int var9 = total(var3) - total(var7) + total(var8);
            this.worker.waitFor(() -> total(inventoryCounts(var2)) >= var9, 600L);
         }

         Map var14 = this.worker.call(() -> inventoryCounts(var2));
         int var10 = 0;

         for (Entry var12 : var3.entrySet()) {
            var10 += Math.max(0, (Integer)var12.getValue() - var14.getOrDefault(var12.getKey(), 0));
         }

         var4 += var10;

         for (Entry var16 : var3.entrySet()) {
            if (var14.getOrDefault(var16.getKey(), 0) >= (Integer)var16.getValue()) {
               int var13 = var10 > 0 ? 2 : var2.getOrDefault(var16.getKey(), 0) + 1;
               var2.put((Item)var16.getKey(), var13);
               if (var13 == 2) {
                  this.say(
                     ModuleChat.body()
                        .warn("Could not sell ")
                        .name(new ItemStack((ItemConvertible)var16.getKey()).method_7964().getString())
                        .text(", skipping it.")
                  );
               }
            }
         }
      }

      return var4;
   }

   private Map<Item, Integer> confirm(Gui var1, Map<Item, Integer> var2) {
      Map var3 = var2;

      for (int var4 = 0; var4 < 2 && var3.equals(var2); var4++) {
         if (!this.worker.call(PanicPayModule::sellMenuOpen)) {
            return null;
         }

         var1.click(53, 0, SlotActionType.field_7790);
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

      for (Slot var2 : Gui.player().field_7512.field_7761) {
         if (!(var2.field_7871 instanceof PlayerInventory) && var2.field_7874 != 53 && var2.method_7681()) {
            var0.merge(var2.method_7677().method_7909(), var2.method_7677().method_7947(), Integer::sum);
         }
      }

      return var0;
   }

   private static int total(Map<Item, Integer> var0) {
      int var1 = 0;

      for (int var3 : var0.values()) {
         var1 += var3;
      }

      return var1;
   }

   private static Map<Item, Integer> inventoryCounts(Map<Item, Integer> var0) {
      HashMap var1 = new HashMap();
      PlayerInventory var2 = Gui.player().method_31548();

      for (int var3 = 0; var3 < 36; var3++) {
         ItemStack var4 = var2.method_5438(var3);
         if (!var4.method_7960() && var0.getOrDefault(var4.method_7909(), 0) < 2) {
            var1.merge(var4.method_7909(), var4.method_7947(), Integer::sum);
         }
      }

      return var1;
   }

   private void openSell(Gui var1) {
      for (int var2 = 0; var2 < 3; var2++) {
         long var3 = this.command("sell");
         this.worker.waitFor(() -> sellMenuOpen() || this.limitedAt >= var3, 8000L);
         if (this.worker.call(PanicPayModule::sellMenuOpen)) {
            this.worker.sleep(50L);
            return;
         }

         var1.closeAll();
      }

      throw new AuctionHouse.Problem("The sell menu did not open");
   }

   private static boolean sellMenuOpen() {
      ClientPlayerEntity var0 = Gui.mc().field_1724;
      if (var0 != null && var0.field_7512 != var0.field_7498 && Gui.mc().field_1755 instanceof HandledScreen) {
         int var1 = 0;

         for (Slot var3 : var0.field_7512.field_7761) {
            if (!(var3.field_7871 instanceof PlayerInventory)) {
               var1++;
            }
         }

         return var1 > 53;
      } else {
         return false;
      }
   }

   private boolean takeOff(Gui var1, boolean var2, boolean var3, boolean var4) {
      var1.closeAll();
      boolean var5 = this.worker.call(() -> {
         ClientPlayerEntity var3x = Gui.player();
         ArrayList var4x = new ArrayList();
         if (var2) {
            for (int var8 : ARMOR_MENU_SLOTS) {
               if (var3x.field_7498.method_7611(var8).method_7681()) {
                  var4x.add(var8);
               }
            }
         }

         if (var3 && var3x.field_7498.method_7611(45).method_7681()) {
            var4x.add(45);
         }

         int var9 = 0;

         for (int var10 = 0; var10 < 36; var10++) {
            if (var3x.method_31548().method_5438(var10).method_7960()) {
               var9++;
            }
         }

         int var11 = var4 ? Math.min(var9, var4x.size()) : var4x.size();

         for (int var12 = 0; var12 < var11; var12++) {
            Gui.mc().field_1761.method_2906(var3x.field_7498.field_7763, (Integer)var4x.get(var12), 0, SlotActionType.field_7794, var3x);
         }

         return var11 > 0;
      });
      if (!var5) {
         return false;
      } else {
         var1.closeInventory();
         this.worker.ticks(2);
         return true;
      }
   }

   private void payBalance(String var1) {
      String var2 = this.readBalance();
      if (this.balance < 1.0) {
         this.say(ModuleChat.body().warn("You have no money to pay."));
      } else {
         double var3 = ServerText.displayStep(this.balanceText);
         int var5 = 0;

         for (int var6 = 0; var6 < 4; var6++) {
            PanicPayModule.Payment var7 = this.pay(var1, var2);
            if (var7 == PanicPayModule.Payment.PAID) {
               this.say(ModuleChat.body().good("Paid $" + var2).text(" to ").name(var1).text("."));
               if (!mentions(this.paidText, var1)) {
                  this.say(ModuleChat.body().danger("Check the payment").text(": the server said \"" + this.paidText + "\"."));
               }

               return;
            }

            if (var7 == PanicPayModule.Payment.POOR) {
               var5++;
               long var8 = (long)Math.floor(this.balance - var3 * var5);
               if (var5 > 2 || var8 < 1L) {
                  throw new AuctionHouse.Problem("/pay " + var1 + " " + var2 + " was refused: not enough money");
               }

               var2 = Long.toString(var8);
            } else if (var7 == PanicPayModule.Payment.FAILED) {
               throw new AuctionHouse.Problem("/pay failed: " + this.errorText);
            }
         }

         throw new AuctionHouse.Problem("No answer to /pay " + var1 + " " + var2);
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

   private PanicPayModule.Payment pay(String var1, String var2) {
      long var3 = this.command("pay " + var1 + " " + var2);
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
            Dialogs.View var2x = Dialogs.current(Gui.mc());
            if (var2x != null && var2x.dialog() != var5[0]) {
               Dialogs.Button var3x = var2x.labelled("confirm", "yes", "pay");
               if (var3x == null) {
                  return false;
               } else if (namesOtherPlayer(var2x, var1)) {
                  Dialogs.close(Gui.mc());
                  var5[0] = var2x.dialog();
                  return true;
               } else {
                  if (Dialogs.press(Gui.mc(), var2x, var3x, Map.of())) {
                     var5[0] = var2x.dialog();
                  }

                  return false;
               }
            } else {
               return false;
            }
         });
         if (var8) {
            this.errorText = "the confirmation named a different player than " + var1;
            return PanicPayModule.Payment.FAILED;
         }

         this.worker.sleep(50L);
      }

      return PanicPayModule.Payment.RETRY;
   }

   private static boolean mentions(String var0, String var1) {
      String var2 = var1.toLowerCase(Locale.ROOT);
      if (var2.startsWith(".")) {
         var2 = var2.substring(1);
      }

      for (String var6 : var0.split("[^a-z0-9_]+")) {
         if (var6.equals(var2)) {
            return true;
         }
      }

      return false;
   }

   private static boolean namesOtherPlayer(Dialogs.View var0, String var1) {
      String var2 = var1.toLowerCase(Locale.ROOT);
      if (var2.startsWith(".")) {
         var2 = var2.substring(1);
      }

      ArrayList var3 = new ArrayList<>(var0.body());
      var3.add(var0.title());

      for (String var5 : var3) {
         for (String var9 : var5.split("[^a-z0-9_]+")) {
            if (var9.length() > var2.length() && var9.startsWith(var2)) {
               return true;
            }
         }
      }

      return false;
   }

   private long command(String var1) {
      long var2 = Math.max(this.holdUntil, this.lastCommandAt + 350L) - System.currentTimeMillis();
      if (var2 > 0L) {
         this.worker.sleep(var2);
      }

      long var4 = System.currentTimeMillis();
      this.worker.run(() -> {
         PayGuard.allow(var1);
         Gui.player().field_3944.method_45730(var1);
      });
      this.lastCommandAt = var4;
      return var4;
   }

   private void say(ModuleChat.Body var1) {
      ModuleChat.send(this, var1);
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
