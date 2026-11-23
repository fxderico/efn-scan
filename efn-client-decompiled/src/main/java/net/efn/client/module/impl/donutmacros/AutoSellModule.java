package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
public final class AutoSellModule extends ClientModule {
   private static final int INVENTORY_SLOTS = 36;
   private static final int CONFIRM_SLOT = 53;
   private static final long POLL_MS = 350L;
   private static final long COMMAND_COOLDOWN_MS = 4000L;
   private static final long OPEN_TIMEOUT_MS = 10000L;
   private static final long CONFIRM_COOLDOWN_MS = 900L;
   private static final long POST_CONFIRM_MS = 250L;
   private static final int STUCK_CONFIRMS = 3;
   private final StringListSetting neverSell = this.addSetting(
      "never-sell", new StringListSetting("Never Sell", "minecraft:netherite_sword", "golden_apple", "minecraft:totem_of_undying").itemPicker()
   );
   private int menuId = -1;
   private long openingUntil;
   private long lastCommandAt;
   private long lastConfirmAt;
   private long nextPollAt;
   private boolean confirmPending;
   private String lastFingerprint = "";
   private int sameAfterConfirm;
   private int sold;
   private int confirmedCount;
   private boolean warnedNoOpen;

   public AutoSellModule() {
      super(
         "auto-sell", "Auto Sell", "Keeps the /sell menu open and keeps selling everything that is not on your Never Sell list.", ModuleCategory.DONUT_MACROS
      );
      this.usesNotifications();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.menuId = -1;
      this.openingUntil = 0L;
      this.lastCommandAt = 0L;
      this.lastConfirmAt = 0L;
      this.nextPollAt = 0L;
      this.confirmPending = false;
      this.lastFingerprint = "";
      this.sameAfterConfirm = 0;
      this.sold = 0;
      this.confirmedCount = 0;
      this.warnedNoOpen = false;
   }

   @Override
   protected void onDisable() {
      ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
      if (var1 != null && this.ours(var1)) {
         var1.method_7346();
      }

      this.menuId = -1;
      if (this.sold > 0) {
         ModuleChat.send(this, ModuleChat.body().text("Sold ").value(this.sold).text(this.sold == 1 ? " item." : " items."));
      }
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 != null && var2.method_1562() != null && var2.field_1761 != null && !var3.method_29504()) {
         long var4 = System.currentTimeMillis();
         if (this.menuId < 0 && this.openingUntil > 0L) {
            if (var3.field_7512 != var3.field_7498 && var2.field_1755 instanceof HandledScreen && containerSlots(var3.field_7512) > 53) {
               this.menuId = var3.field_7512.field_7763;
               this.openingUntil = 0L;
               this.nextPollAt = var4 + 150L;
               this.warnedNoOpen = false;
            } else if (var4 > this.openingUntil) {
               this.openingUntil = 0L;
               if (!this.warnedNoOpen) {
                  ModuleChat.send(this, ModuleChat.body().warn("The sell menu did not open").text(", retrying."));
               }

               this.warnedNoOpen = true;
            }
         } else if (!this.ours(var3)) {
            this.menuId = -1;
            if (var2.field_1755 == null && var4 - this.lastCommandAt >= 4000L) {
               var3.field_3944.method_45730("sell");
               this.lastCommandAt = var4;
               this.openingUntil = var4 + 10000L;
            }
         } else if (var4 >= this.nextPollAt) {
            this.nextPollAt = var4 + 350L;
            ScreenHandler var6 = var3.field_7512;
            if (this.confirmedCount > 0) {
               int var7 = this.confirmedCount - countWaiting(var6);
               this.confirmedCount = 0;
               if (var7 > 0) {
                  this.sold += var7;
                  ModuleChat.actionBar(this, ModuleChat.body().text("Sold ").value(this.sold).text(" items this session"));
               }
            }

            if (this.confirmPending) {
               this.confirmPending = false;
               if (waiting(var6) && var4 - this.lastConfirmAt >= 900L && this.sameAfterConfirm < 3) {
                  int var11 = countWaiting(var6);
                  var2.field_1761.method_2906(var6.field_7763, 53, 0, SlotActionType.field_7790, var3);
                  this.lastConfirmAt = var4;
                  this.lastFingerprint = fingerprint(var6);
                  this.confirmedCount = var11;
                  this.nextPollAt = var4 + 250L;
               }
            } else {
               int var10 = 0;

               for (Slot var9 : var6.field_7761) {
                  if (var9.field_7871 instanceof PlayerInventory
                     && var9.method_7681()
                     && var9.method_34266() < 36
                     && !SellRoutine.neverSell(var9.method_7677(), this.neverSell.get())) {
                     var2.field_1761.method_2906(var6.field_7763, var9.field_7874, 0, SlotActionType.field_7794, var3);
                     var10++;
                  }
               }

               String var12 = fingerprint(var6);
               if (var10 > 0 || !var12.equals(this.lastFingerprint)) {
                  this.sameAfterConfirm = 0;
               } else if (waiting(var6)) {
                  this.sameAfterConfirm++;
               }

               if (var10 > 0 || waiting(var6)) {
                  this.confirmPending = true;
                  this.nextPollAt = var4 + 50L;
               }
            }
         }
      } else {
         this.menuId = -1;
      }
   }

   private boolean ours(ClientPlayerEntity var1) {
      return this.menuId >= 0 && var1.field_7512 != var1.field_7498 && var1.field_7512.field_7763 == this.menuId;
   }

   private static boolean waiting(ScreenHandler var0) {
      return countWaiting(var0) > 0;
   }

   private static int countWaiting(ScreenHandler var0) {
      int var1 = 0;

      for (Slot var3 : var0.field_7761) {
         if (!(var3.field_7871 instanceof PlayerInventory) && var3.field_7874 != 53 && var3.method_7681()) {
            var1 += var3.method_7677().method_7947();
         }
      }

      return var1;
   }

   private static int containerSlots(ScreenHandler var0) {
      int var1 = 0;

      for (Slot var3 : var0.field_7761) {
         if (!(var3.field_7871 instanceof PlayerInventory)) {
            var1++;
         }
      }

      return var1;
   }

   private static String fingerprint(ScreenHandler var0) {
      StringBuilder var1 = new StringBuilder();

      for (Slot var3 : var0.field_7761) {
         if (var3.field_7871 instanceof PlayerInventory) {
            break;
         }

         ItemStack var4 = var3.method_7677();
         if (!var4.method_7960()) {
            var1.append(var3.field_7874).append(':').append(var4.method_7909()).append('x').append(var4.method_7947()).append(';');
         }
      }

      return var1.toString();
   }
}
