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
      ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
      if (var1 != null && this.ours(var1)) {
         var1.closeHandledScreen();
      }

      this.menuId = -1;
      if (this.sold > 0) {
         ModuleChat.send(this, ModuleChat.body().text("Sold ").value(this.sold).text(this.sold == 1 ? " item." : " items."));
      }
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null && var2.getNetworkHandler() != null && var2.interactionManager != null && !var3.isDead()) {
         long var4 = System.currentTimeMillis();
         if (this.menuId < 0 && this.openingUntil > 0L) {
            if (var3.currentScreenHandler != var3.playerScreenHandler
               && var2.currentScreen instanceof HandledScreen
               && containerSlots(var3.currentScreenHandler) > 53) {
               this.menuId = var3.currentScreenHandler.syncId;
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
            if (var2.currentScreen == null && var4 - this.lastCommandAt >= 4000L) {
               var3.networkHandler.sendChatCommand("sell");
               this.lastCommandAt = var4;
               this.openingUntil = var4 + 10000L;
            }
         } else if (var4 >= this.nextPollAt) {
            this.nextPollAt = var4 + 350L;
            ScreenHandler var6 = var3.currentScreenHandler;
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
                  var2.interactionManager.clickSlot(var6.syncId, 53, 0, SlotActionType.PICKUP, var3);
                  this.lastConfirmAt = var4;
                  this.lastFingerprint = fingerprint(var6);
                  this.confirmedCount = var11;
                  this.nextPollAt = var4 + 250L;
               }
            } else {
               int var10 = 0;

               for (Slot var9 : var6.slots) {
                  if (var9.inventory instanceof PlayerInventory
                     && var9.hasStack()
                     && var9.getIndex() < 36
                     && !SellRoutine.neverSell(var9.getStack(), this.neverSell.get())) {
                     var2.interactionManager.clickSlot(var6.syncId, var9.id, 0, SlotActionType.QUICK_MOVE, var3);
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

   private boolean ours(ClientPlayerEntity clientPlayerEntity) {
      return this.menuId >= 0
         && clientPlayerEntity.currentScreenHandler != clientPlayerEntity.playerScreenHandler
         && clientPlayerEntity.currentScreenHandler.syncId == this.menuId;
   }

   private static boolean waiting(ScreenHandler screenHandler) {
      return countWaiting(screenHandler) > 0;
   }

   private static int countWaiting(ScreenHandler screenHandler) {
      int var1 = 0;

      for (Slot var3 : screenHandler.slots) {
         if (!(var3.inventory instanceof PlayerInventory) && var3.id != 53 && var3.hasStack()) {
            var1 += var3.getStack().getCount();
         }
      }

      return var1;
   }

   private static int containerSlots(ScreenHandler screenHandler) {
      int var1 = 0;

      for (Slot var3 : screenHandler.slots) {
         if (!(var3.inventory instanceof PlayerInventory)) {
            var1++;
         }
      }

      return var1;
   }

   private static String fingerprint(ScreenHandler screenHandler) {
      StringBuilder var1 = new StringBuilder();

      for (Slot var3 : screenHandler.slots) {
         if (var3.inventory instanceof PlayerInventory) {
            break;
         }

         ItemStack var4 = var3.getStack();
         if (!var4.isEmpty()) {
            var1.append(var3.id).append(':').append(var4.getItem()).append('x').append(var4.getCount()).append(';');
         }
      }

      return var1.toString();
   }
}
