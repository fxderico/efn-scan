package net.efn.client.module.impl.donutmacros;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.efn.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
final class SellRoutine {
   private static final int INVENTORY_SLOTS = 36;
   private static final int CONFIRM_SLOT = 53;
   private static final int MAX_CONFIRMS = 3;
   private static final int STRIKES_TO_SKIP = 2;
   private static final int MAX_RESENDS = 3;
   private static final long CHECK_EVERY_MS = 500L;
   private static final long COMMAND_COOLDOWN_MS = 4000L;
   private static final long OPEN_TIMEOUT_MS = 10000L;
   private static final long STRAY_MENU_MS = 5000L;
   private static final long FAIL_COOLDOWN_MS = 30000L;
   private static final long CONFIRM_COOLDOWN_MS = 900L;
   private static final long POST_CONFIRM_MS = 250L;
   private static final long SKIP_MS = 600000L;
   private final ClientModule owner;
   private final StringListSetting neverSell;
   private final BiPredicate<Integer, ItemStack> keep;
   private final Runnable onCommand;
   private SellRoutine.State state = SellRoutine.State.IDLE;
   private long nextCheckAt;
   private long lastCommandAt;
   private long deadline;
   private boolean rejected;
   private long retryAt;
   private int resends;
   private int menuId = -1;
   private int confirms;
   private final Map<Item, Integer> offered = new LinkedHashMap<>();
   private final Map<Item, Integer> strikes = new HashMap<>();
   private final Map<Item, Long> skipUntil = new HashMap<>();

   SellRoutine(ClientModule clientModule, StringListSetting stringListSetting, BiPredicate<Integer, ItemStack> biPredicate, Runnable runnable) {
      this.owner = clientModule;
      this.neverSell = stringListSetting;
      this.keep = biPredicate;
      this.onCommand = runnable;
   }

   void reset() {
      this.state = SellRoutine.State.IDLE;
      this.nextCheckAt = 0L;
      this.lastCommandAt = 0L;
      this.rejected = false;
      this.menuId = -1;
      this.offered.clear();
      this.strikes.clear();
      this.skipUntil.clear();
   }

   boolean active() {
      return this.state != SellRoutine.State.IDLE;
   }

   void commandRejected(long l) {
      if (this.state == SellRoutine.State.OPENING) {
         this.rejected = true;
         this.retryAt = l;
      }
   }

   void abort(MinecraftClient minecraftClient) {
      if (this.state != SellRoutine.State.IDLE) {
         ClientPlayerEntity var2 = minecraftClient.player;
         if (var2 != null && var2.currentScreenHandler != var2.playerScreenHandler && var2.currentScreenHandler.syncId == this.menuId) {
            var2.closeHandledScreen();
         }

         this.state = SellRoutine.State.IDLE;
         this.menuId = -1;
         this.offered.clear();
         this.nextCheckAt = System.currentTimeMillis() + 500L;
      }
   }

   boolean step(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, ClientPlayNetworkHandler clientPlayNetworkHandler, long l, boolean bl) {
      return switch (this.state) {
         case IDLE -> this.stepIdle(minecraftClient, clientPlayerEntity, clientPlayNetworkHandler, l, bl);
         case OPENING -> this.stepOpening(minecraftClient, clientPlayerEntity, clientPlayNetworkHandler, l);
         case MOVING -> this.stepMoving(minecraftClient, clientPlayerEntity, l);
         case CONFIRMING -> this.stepConfirming(minecraftClient, clientPlayerEntity, l);
      };
   }

   private boolean stepIdle(
      MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, ClientPlayNetworkHandler clientPlayNetworkHandler, long l, boolean bl
   ) {
      if (l - this.lastCommandAt < 5000L && sellMenuOpen(minecraftClient, clientPlayerEntity)) {
         clientPlayerEntity.closeHandledScreen();
         return true;
      } else if (l < this.nextCheckAt) {
         return false;
      } else {
         this.nextCheckAt = l + 500L;
         if (minecraftClient.currentScreen != null || !bl || l - this.lastCommandAt < 4000L) {
            return false;
         } else if (!this.hasSellable(clientPlayerEntity.getInventory(), l)) {
            return false;
         } else {
            this.resends = 0;
            this.send(clientPlayNetworkHandler, l);
            return true;
         }
      }
   }

   private void send(ClientPlayNetworkHandler clientPlayNetworkHandler, long l) {
      clientPlayNetworkHandler.sendChatCommand("sell");
      this.lastCommandAt = l;
      this.onCommand.run();
      this.deadline = l + 10000L;
      this.rejected = false;
      this.state = SellRoutine.State.OPENING;
   }

   private boolean stepOpening(
      MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, ClientPlayNetworkHandler clientPlayNetworkHandler, long l
   ) {
      if (sellMenuOpen(minecraftClient, clientPlayerEntity)) {
         this.menuId = clientPlayerEntity.currentScreenHandler.syncId;
         this.state = SellRoutine.State.MOVING;
         return true;
      } else if (this.rejected && l >= this.retryAt && this.resends < 3) {
         this.resends++;
         this.send(clientPlayNetworkHandler, l);
         return true;
      } else if (l < this.deadline) {
         return true;
      } else {
         this.state = SellRoutine.State.IDLE;
         ModuleChat.send(this.owner, ModuleChat.body().warn("The sell menu did not open").text(", trying again in 30s."));
         this.nextCheckAt = l + 30000L;
         return false;
      }
   }

   private boolean stepMoving(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, long l) {
      ScreenHandler var5 = clientPlayerEntity.currentScreenHandler;
      if (!this.ours(clientPlayerEntity)) {
         return this.finish(null, l);
      } else {
         int var6 = 0;

         for (Slot var8 : var5.slots) {
            int var9;
            if (var8.inventory instanceof PlayerInventory && var8.hasStack() && (var9 = var8.getIndex()) < 36 && this.sellable(var9, var8.getStack(), l)) {
               minecraftClient.interactionManager.clickSlot(var5.syncId, var8.id, 0, SlotActionType.QUICK_MOVE, clientPlayerEntity);
               var6++;
            }
         }

         if (var6 == 0) {
            return this.close(clientPlayerEntity, null, l);
         } else {
            this.offered.clear();
            this.offered.putAll(waitingItems(var5));
            this.confirms = 0;
            this.deadline = l;
            this.state = SellRoutine.State.CONFIRMING;
            return true;
         }
      }
   }

   private boolean stepConfirming(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, long l) {
      if (!this.ours(clientPlayerEntity)) {
         return this.finish(null, l);
      } else if (l < this.deadline) {
         return true;
      } else {
         ScreenHandler var5 = clientPlayerEntity.currentScreenHandler;
         if (this.confirms < 3 && itemsWaiting(var5)) {
            minecraftClient.interactionManager.clickSlot(var5.syncId, 53, 0, SlotActionType.PICKUP, clientPlayerEntity);
            this.confirms++;
            this.deadline = l + (this.confirms == 1 ? 250L : 900L);
            return true;
         } else {
            return this.close(clientPlayerEntity, waitingItems(var5), l);
         }
      }
   }

   private boolean close(ClientPlayerEntity clientPlayerEntity, Map<Item, Integer> map, long l) {
      if (this.ours(clientPlayerEntity)) {
         clientPlayerEntity.closeHandledScreen();
      }

      return this.finish(map, l);
   }

   private boolean finish(Map<Item, Integer> map, long l) {
      if (map != null) {
         this.settle(map, l);
      }

      this.offered.clear();
      this.menuId = -1;
      this.state = SellRoutine.State.IDLE;
      this.nextCheckAt = l + 500L;
      return false;
   }

   private void settle(Map<Item, Integer> map, long l) {
      int var4 = 0;

      for (Entry var6 : this.offered.entrySet()) {
         Item var7 = (Item)var6.getKey();
         int var8 = map.getOrDefault(var7, 0);
         var4 += Math.max(0, (Integer)var6.getValue() - var8);
         if (var8 <= 0) {
            this.strikes.remove(var7);
         } else if (this.strikes.merge(var7, 1, Integer::sum) >= 2) {
            this.strikes.remove(var7);
            this.skipUntil.put(var7, l + 600000L);
            ModuleChat.send(this.owner, ModuleChat.body().warn("Could not sell ").name(var7.getName().getString()).text(", skipping it for 10 minutes."));
         }
      }

      if (var4 > 0) {
         ModuleChat.send(this.owner, ModuleChat.body().text("Sold ").value(var4).text(var4 == 1 ? " item." : " items."));
      }
   }

   private boolean ours(ClientPlayerEntity clientPlayerEntity) {
      return clientPlayerEntity.currentScreenHandler != clientPlayerEntity.playerScreenHandler && clientPlayerEntity.currentScreenHandler.syncId == this.menuId;
   }

   private boolean hasSellable(PlayerInventory playerInventory, long l) {
      for (int var4 = 0; var4 < 36; var4++) {
         ItemStack var5 = playerInventory.getStack(var4);
         if (!var5.isEmpty() && this.sellable(var4, var5, l)) {
            return true;
         }
      }

      return false;
   }

   private boolean sellable(int i, ItemStack itemStack, long l) {
      return !this.skipped(itemStack.getItem(), l) && !this.keep.test(i, itemStack) && !this.blacklisted(itemStack);
   }

   private boolean skipped(Item item, long l) {
      Long var4 = this.skipUntil.get(item);
      if (var4 == null) {
         return false;
      } else if (l < var4) {
         return true;
      } else {
         this.skipUntil.remove(item);
         return false;
      }
   }

   private boolean blacklisted(ItemStack itemStack) {
      return neverSell(itemStack, this.neverSell.get());
   }

   static boolean neverSell(ItemStack itemStack, List<String> list) {
      String var2 = Registries.ITEM.getId(itemStack.getItem()).toString().toLowerCase(Locale.ROOT);
      String var3 = var2.substring(var2.indexOf(58) + 1);

      for (String var5 : list) {
         String var6 = var5.trim().toLowerCase(Locale.ROOT);
         if (!var6.isEmpty()) {
            if (var2.equals(var6) || var3.equals(var6)) {
               return true;
            }

            if (var6.indexOf(58) < 0 && var2.contains(var6)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean itemsWaiting(ScreenHandler screenHandler) {
      for (Slot var2 : screenHandler.slots) {
         if (!(var2.inventory instanceof PlayerInventory) && var2.id != 53 && var2.hasStack()) {
            return true;
         }
      }

      return false;
   }

   private static Map<Item, Integer> waitingItems(ScreenHandler screenHandler) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Slot var3 : screenHandler.slots) {
         if (!(var3.inventory instanceof PlayerInventory) && var3.id != 53 && var3.hasStack()) {
            var1.merge(var3.getStack().getItem(), var3.getStack().getCount(), Integer::sum);
         }
      }

      return var1;
   }

   private static boolean sellMenuOpen(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      ScreenHandler var2 = clientPlayerEntity.currentScreenHandler;
      return var2 != clientPlayerEntity.playerScreenHandler && minecraftClient.currentScreen instanceof HandledScreen && containerSlots(var2) > 53;
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

   @Environment(EnvType.CLIENT)
   private static enum State {
      IDLE,
      OPENING,
      MOVING,
      CONFIRMING;

      private State() {
      }
   }
}
