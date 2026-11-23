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

   SellRoutine(ClientModule var1, StringListSetting var2, BiPredicate<Integer, ItemStack> var3, Runnable var4) {
      this.owner = var1;
      this.neverSell = var2;
      this.keep = var3;
      this.onCommand = var4;
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

   void commandRejected(long var1) {
      if (this.state == SellRoutine.State.OPENING) {
         this.rejected = true;
         this.retryAt = var1;
      }
   }

   void abort(MinecraftClient var1) {
      if (this.state != SellRoutine.State.IDLE) {
         ClientPlayerEntity var2 = var1.field_1724;
         if (var2 != null && var2.field_7512 != var2.field_7498 && var2.field_7512.field_7763 == this.menuId) {
            var2.method_7346();
         }

         this.state = SellRoutine.State.IDLE;
         this.menuId = -1;
         this.offered.clear();
         this.nextCheckAt = System.currentTimeMillis() + 500L;
      }
   }

   boolean step(MinecraftClient var1, ClientPlayerEntity var2, ClientPlayNetworkHandler var3, long var4, boolean var6) {
      return switch (this.state) {
         case IDLE -> this.stepIdle(var1, var2, var3, var4, var6);
         case OPENING -> this.stepOpening(var1, var2, var3, var4);
         case MOVING -> this.stepMoving(var1, var2, var4);
         case CONFIRMING -> this.stepConfirming(var1, var2, var4);
      };
   }

   private boolean stepIdle(MinecraftClient var1, ClientPlayerEntity var2, ClientPlayNetworkHandler var3, long var4, boolean var6) {
      if (var4 - this.lastCommandAt < 5000L && sellMenuOpen(var1, var2)) {
         var2.method_7346();
         return true;
      } else if (var4 < this.nextCheckAt) {
         return false;
      } else {
         this.nextCheckAt = var4 + 500L;
         if (var1.field_1755 != null || !var6 || var4 - this.lastCommandAt < 4000L) {
            return false;
         } else if (!this.hasSellable(var2.method_31548(), var4)) {
            return false;
         } else {
            this.resends = 0;
            this.send(var3, var4);
            return true;
         }
      }
   }

   private void send(ClientPlayNetworkHandler var1, long var2) {
      var1.method_45730("sell");
      this.lastCommandAt = var2;
      this.onCommand.run();
      this.deadline = var2 + 10000L;
      this.rejected = false;
      this.state = SellRoutine.State.OPENING;
   }

   private boolean stepOpening(MinecraftClient var1, ClientPlayerEntity var2, ClientPlayNetworkHandler var3, long var4) {
      if (sellMenuOpen(var1, var2)) {
         this.menuId = var2.field_7512.field_7763;
         this.state = SellRoutine.State.MOVING;
         return true;
      } else if (this.rejected && var4 >= this.retryAt && this.resends < 3) {
         this.resends++;
         this.send(var3, var4);
         return true;
      } else if (var4 < this.deadline) {
         return true;
      } else {
         this.state = SellRoutine.State.IDLE;
         ModuleChat.send(this.owner, ModuleChat.body().warn("The sell menu did not open").text(", trying again in 30s."));
         this.nextCheckAt = var4 + 30000L;
         return false;
      }
   }

   private boolean stepMoving(MinecraftClient var1, ClientPlayerEntity var2, long var3) {
      ScreenHandler var5 = var2.field_7512;
      if (!this.ours(var2)) {
         return this.finish(null, var3);
      } else {
         int var6 = 0;

         for (Slot var8 : var5.field_7761) {
            int var9;
            if (var8.field_7871 instanceof PlayerInventory
               && var8.method_7681()
               && (var9 = var8.method_34266()) < 36
               && this.sellable(var9, var8.method_7677(), var3)) {
               var1.field_1761.method_2906(var5.field_7763, var8.field_7874, 0, SlotActionType.field_7794, var2);
               var6++;
            }
         }

         if (var6 == 0) {
            return this.close(var2, null, var3);
         } else {
            this.offered.clear();
            this.offered.putAll(waitingItems(var5));
            this.confirms = 0;
            this.deadline = var3;
            this.state = SellRoutine.State.CONFIRMING;
            return true;
         }
      }
   }

   private boolean stepConfirming(MinecraftClient var1, ClientPlayerEntity var2, long var3) {
      if (!this.ours(var2)) {
         return this.finish(null, var3);
      } else if (var3 < this.deadline) {
         return true;
      } else {
         ScreenHandler var5 = var2.field_7512;
         if (this.confirms < 3 && itemsWaiting(var5)) {
            var1.field_1761.method_2906(var5.field_7763, 53, 0, SlotActionType.field_7790, var2);
            this.confirms++;
            this.deadline = var3 + (this.confirms == 1 ? 250L : 900L);
            return true;
         } else {
            return this.close(var2, waitingItems(var5), var3);
         }
      }
   }

   private boolean close(ClientPlayerEntity var1, Map<Item, Integer> var2, long var3) {
      if (this.ours(var1)) {
         var1.method_7346();
      }

      return this.finish(var2, var3);
   }

   private boolean finish(Map<Item, Integer> var1, long var2) {
      if (var1 != null) {
         this.settle(var1, var2);
      }

      this.offered.clear();
      this.menuId = -1;
      this.state = SellRoutine.State.IDLE;
      this.nextCheckAt = var2 + 500L;
      return false;
   }

   private void settle(Map<Item, Integer> var1, long var2) {
      int var4 = 0;

      for (Entry var6 : this.offered.entrySet()) {
         Item var7 = (Item)var6.getKey();
         int var8 = var1.getOrDefault(var7, 0);
         var4 += Math.max(0, (Integer)var6.getValue() - var8);
         if (var8 <= 0) {
            this.strikes.remove(var7);
         } else if (this.strikes.merge(var7, 1, Integer::sum) >= 2) {
            this.strikes.remove(var7);
            this.skipUntil.put(var7, var2 + 600000L);
            ModuleChat.send(this.owner, ModuleChat.body().warn("Could not sell ").name(var7.method_63680().getString()).text(", skipping it for 10 minutes."));
         }
      }

      if (var4 > 0) {
         ModuleChat.send(this.owner, ModuleChat.body().text("Sold ").value(var4).text(var4 == 1 ? " item." : " items."));
      }
   }

   private boolean ours(ClientPlayerEntity var1) {
      return var1.field_7512 != var1.field_7498 && var1.field_7512.field_7763 == this.menuId;
   }

   private boolean hasSellable(PlayerInventory var1, long var2) {
      for (int var4 = 0; var4 < 36; var4++) {
         ItemStack var5 = var1.method_5438(var4);
         if (!var5.method_7960() && this.sellable(var4, var5, var2)) {
            return true;
         }
      }

      return false;
   }

   private boolean sellable(int var1, ItemStack var2, long var3) {
      return !this.skipped(var2.method_7909(), var3) && !this.keep.test(var1, var2) && !this.blacklisted(var2);
   }

   private boolean skipped(Item var1, long var2) {
      Long var4 = this.skipUntil.get(var1);
      if (var4 == null) {
         return false;
      } else if (var2 < var4) {
         return true;
      } else {
         this.skipUntil.remove(var1);
         return false;
      }
   }

   private boolean blacklisted(ItemStack var1) {
      return neverSell(var1, this.neverSell.get());
   }

   static boolean neverSell(ItemStack var0, List<String> var1) {
      String var2 = Registries.field_41178.method_10221(var0.method_7909()).toString().toLowerCase(Locale.ROOT);
      String var3 = var2.substring(var2.indexOf(58) + 1);

      for (String var5 : var1) {
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

   private static boolean itemsWaiting(ScreenHandler var0) {
      for (Slot var2 : var0.field_7761) {
         if (!(var2.field_7871 instanceof PlayerInventory) && var2.field_7874 != 53 && var2.method_7681()) {
            return true;
         }
      }

      return false;
   }

   private static Map<Item, Integer> waitingItems(ScreenHandler var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Slot var3 : var0.field_7761) {
         if (!(var3.field_7871 instanceof PlayerInventory) && var3.field_7874 != 53 && var3.method_7681()) {
            var1.merge(var3.method_7677().method_7909(), var3.method_7677().method_7947(), Integer::sum);
         }
      }

      return var1;
   }

   private static boolean sellMenuOpen(MinecraftClient var0, ClientPlayerEntity var1) {
      ScreenHandler var2 = var1.field_7512;
      return var2 != var1.field_7498 && var0.field_1755 instanceof HandledScreen && containerSlots(var2) > 53;
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
