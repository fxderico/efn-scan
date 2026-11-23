package net.efn.client.module.impl.combat;

import com.example.client.mixin.AccessorElytraFlyMultiPlayerGameMode;
import com.example.client.mixin.AccessorLocalPlayer;
import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.totem.DoubleHand;
import net.efn.client.module.impl.movement.SneakModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class AutoInvTotemModule extends ClientModule {
   private static final String INVENTORY = "Inventory";
   private static final String HOTBAR_SWAP = "Hotbar Swap";
   private static final int MIN_CONFIRM_TICKS = 10;
   private static final int MAX_CONFIRM_TICKS = 40;
   private static final int PASS_COOLDOWN_TICKS = 4;
   private static final int GIVE_UP_COOLDOWN_TICKS = 10;
   private static final int MAX_PASS_SWAPS = 2;
   private static final int MAX_RESELECTS = 2;
   private static final int MAX_STEPS_PER_TICK = 3;
   private static final int MAX_UNSAFE_TICKS = 10;
   private static AutoInvTotemModule instance;
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Inventory", "Inventory", "Hotbar Swap"));
   private final BooleanSetting hotbarRefill = this.addSetting("hotbar-refill", new BooleanSetting("Hotbar Refill", false));
   private final NumberSetting hotbarSlot = this.addSetting("hotbar-slot", new NumberSetting("Hotbar Slot", 1.0F, 1.0F, 9.0F).step(1.0F));
   private final NumberSetting reactionDelay = this.addSetting("reaction-delay", new NumberSetting("Reaction Delay", 0.0F, 0.0F, 10.0F).step(1.0F));
   private final NumberSetting swapDelay = this.addSetting("swap-delay", new NumberSetting("Swap Delay", 1.0F, 0.0F, 10.0F).step(1.0F));
   private final NumberSetting closeDelay = this.addSetting("close-delay", new NumberSetting("Close Delay", 0.0F, 0.0F, 10.0F).step(1.0F));
   private final NumberSetting switchBackDelay = this.addSetting("switch-back-delay", new NumberSetting("Switch Back Delay", 2.0F, 0.0F, 10.0F).step(1.0F));
   private final BooleanSetting doubleHand = this.addSetting("double-hand", new BooleanSetting("Double Hand", false));
   private final DoubleHand totemHand = new DoubleHand();
   private AutoInvTotemModule.Step step = AutoInvTotemModule.Step.IDLE;
   private int tick;
   private int readyAt;
   private int reactionAt = -1;
   private int idleUntil;
   private int confirmDeadline = -1;
   private int clickedAt = -1;
   private int swappedAt = -1;
   private boolean openedInventory;
   private int passSwaps;
   private int unsafeTicks;
   private int totemSlot = -1;
   private int previousSlot = -1;
   private int reselects;

   public AutoInvTotemModule() {
      super(
         "autoinvtotem",
         "Auto Inventory Totem",
         "Refills the offhand totem after a pop through the inventory or a hotbar swap; Double Hand also keeps one in the main hand while in danger.",
         ModuleCategory.COMBAT
      );
      instance = this;
      this.hotbarSlot.visibleWhen(this.hotbarRefill::get);
      this.switchBackDelay.visibleWhen(() -> this.mode.is("Hotbar Swap"));
   }

   public static boolean wantsHotbar() {
      AutoInvTotemModule var0 = instance;
      return var0 != null && var0.isEnabled()
         ? var0.step == AutoInvTotemModule.Step.HOTBAR_SWAP || var0.step == AutoInvTotemModule.Step.HOTBAR_RETURN || var0.totemHand.holding()
         : AutoTotemModule.wantsHotbar();
   }

   public static boolean busy() {
      AutoInvTotemModule var0 = instance;
      if (var0 != null && var0.isEnabled()) {
         ClientPlayerEntity var1 = MinecraftClient.method_1551().field_1724;
         if (var1 != null && !var1.method_7325() && !var1.method_56992()) {
            PlayerInventory var2 = var1.method_31548();
            return var0.step != AutoInvTotemModule.Step.IDLE
               || var0.totemHand.holding()
               || var0.awaitingServer(var1)
               || var0.clickedAt == var0.tick
               || var1.method_6079().method_7960() && (findStoredTotem(var2) != -1 || var0.hotbarSource(var2) != -1);
         } else {
            return false;
         }
      } else {
         return AutoTotemModule.busy();
      }
   }

   public static boolean clickSafe(ClientPlayerEntity var0) {
      AccessorLocalPlayer var1 = (AccessorLocalPlayer)var0;
      PlayerInput var2 = var1.efn$lastSentInput();
      return !var1.efn$wasSprinting()
         && !var2.comp_3159()
         && !var2.comp_3160()
         && !var2.comp_3161()
         && !var2.comp_3162()
         && !var2.comp_3163()
         && !var2.comp_3164();
   }

   @Override
   protected void onEnable() {
      AutoTotemModule var1 = ModuleRegistry.get(AutoTotemModule.class);
      if (var1 != null) {
         var1.setEnabled(false);
      }

      this.idleUntil = 0;
   }

   @Override
   protected void onDisable() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 != null) {
         PlayerInventory var3 = var2.method_31548();
         if (this.openedInventory && var1.field_1755 instanceof InventoryScreen) {
            var2.method_7346();
         }

         if ((this.step == AutoInvTotemModule.Step.HOTBAR_SWAP || this.step == AutoInvTotemModule.Step.HOTBAR_RETURN) && var1.field_1755 == null) {
            this.restoreSlot(var3);
         }

         this.totemHand.putBack(var3);
      }

      this.reset();
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onTick(TickEvent var1) {
      this.tick++;
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 != null
         && var2.field_1687 != null
         && var2.field_1761 != null
         && var2.method_1562() != null
         && !var3.method_29504()
         && !var3.method_7325()
         && !var3.method_56992()) {
         if (this.step == AutoInvTotemModule.Step.IDLE) {
            this.tryStart(var2, var3);
         }

         if (this.step == AutoInvTotemModule.Step.IDLE) {
            this.totemHand.tick(var2, var3, this.doubleHand.get());
         }

         for (int var4 = 0; var4 < 3 && this.step != AutoInvTotemModule.Step.IDLE && this.tick >= this.readyAt; var4++) {
            switch (this.step) {
               case INVENTORY_SWAP:
                  this.inventorySwap(var2, var3);
                  break;
               case INVENTORY_CLOSE:
                  this.closeInventory(var2, var3);
                  break;
               case HOTBAR_SWAP:
                  this.hotbarSwap(var2, var3);
                  break;
               case HOTBAR_RETURN:
                  this.hotbarReturn(var2, var3);
            }
         }
      } else {
         if (var3 != null && this.openedInventory && var2.field_1755 instanceof InventoryScreen) {
            var3.method_7346();
         }

         this.reset();
      }
   }

   private void tryStart(MinecraftClient var1, ClientPlayerEntity var2) {
      PlayerInventory var4 = var2.method_31548();
      if (!var2.method_6079().method_7960()) {
         this.confirmDeadline = -1;
      }

      boolean var3 = var2.method_6079().method_7960() && !this.awaitingServer(var2) && (findStoredTotem(var4) != -1 || this.hotbarSource(var4) != -1);
      if (!var3 && !this.refillDue(var1, var2)) {
         this.reactionAt = -1;
      } else {
         if (this.reactionAt < 0) {
            this.reactionAt = this.tick + this.reactionDelay.getInt();
         }

         if (this.tick >= this.reactionAt && this.tick >= this.idleUntil) {
            if (var1.field_1755 instanceof HandledScreen) {
               this.startPass(false);
            } else if (var1.field_1755 == null) {
               boolean var6 = !var1.field_1761.method_2895() && !sneakForced();
               if (!var3 || this.hotbarSource(var4) == -1 || !this.mode.is("Hotbar Swap") && var6) {
                  if (var6) {
                     var1.method_1507(new InventoryScreen(var2));
                     this.startPass(true);
                  } else {
                     this.idleUntil = this.tick + 10;
                  }
               } else {
                  this.startHotbarSwap(var4);
               }
            }
         }
      }
   }

   private boolean refillDue(MinecraftClient var1, ClientPlayerEntity var2) {
      PlayerInventory var3 = var2.method_31548();
      return !var2.method_6079().method_7960() && this.refillWanted(var3) && findStoredTotem(var3) != -1
         ? var1.field_1755 instanceof HandledScreen
            || var1.field_1755 == null && clickSafe(var2) && !this.totemHand.holding() && !CombatMacroLock.heldByOther(this)
         : false;
   }

   private boolean refillWanted(PlayerInventory var1) {
      return this.hotbarRefill.get() && !var1.method_5438(this.hotbarSlot.getInt() - 1).method_31574(Items.field_8288);
   }

   private static boolean sneakForced() {
      SneakModule var0 = SneakModule.active;
      return var0 != null && (var0.doVanilla() || var0.doPacket());
   }

   private void startPass(boolean var1) {
      this.openedInventory = var1;
      this.passSwaps = 0;
      this.unsafeTicks = 0;
      this.schedule(AutoInvTotemModule.Step.INVENTORY_SWAP, var1 ? Math.max(1, this.swapDelay.getInt()) : this.swapDelay.getInt());
   }

   private void inventorySwap(MinecraftClient var1, ClientPlayerEntity var2) {
      if (var1.field_1755 instanceof HandledScreen var3 && (!this.openedInventory || var1.field_1755 instanceof InventoryScreen)) {
         ScreenHandler var7 = var3.method_17577();
         if (clickSafe(var2) && var7.method_34255().method_7960()) {
            int var5 = this.passSource(var2);
            if (var5 != -1 && this.passSwaps < 2) {
               int var6 = var2.method_6079().method_7960() ? 40 : this.hotbarSlot.getInt() - 1;
               var7.method_37418(var2.method_31548(), var5)
                  .ifPresent(var4 -> var1.field_1761.method_2906(var7.field_7763, var4, var6, SlotActionType.field_7791, var2));
               this.clickedAt = this.tick;
               this.passSwaps++;
               if (this.passSource(var2) != -1 && this.passSwaps < 2) {
                  this.schedule(AutoInvTotemModule.Step.INVENTORY_SWAP, Math.max(1, this.swapDelay.getInt()));
               } else {
                  this.schedule(AutoInvTotemModule.Step.INVENTORY_CLOSE, this.closeDelay.getInt());
               }
            } else {
               this.schedule(AutoInvTotemModule.Step.INVENTORY_CLOSE, this.closeDelay.getInt());
            }
         } else {
            if (++this.unsafeTicks > 10) {
               this.schedule(AutoInvTotemModule.Step.INVENTORY_CLOSE, 0);
            } else {
               this.readyAt = this.tick + 1;
            }
         }
      } else {
         this.finishPass();
      }
   }

   private void closeInventory(MinecraftClient var1, ClientPlayerEntity var2) {
      if (this.passSource(var2) != -1 && this.passSwaps < 2 && this.unsafeTicks <= 10 && var1.field_1755 instanceof HandledScreen) {
         this.schedule(AutoInvTotemModule.Step.INVENTORY_SWAP, Math.max(1, this.swapDelay.getInt()));
      } else {
         if (this.openedInventory && var1.field_1755 instanceof InventoryScreen) {
            if (!clickSafe(var2) && ++this.unsafeTicks <= 10) {
               this.readyAt = this.tick + 1;
               return;
            }

            var2.method_7346();
         }

         this.finishPass();
      }
   }

   private void finishPass() {
      this.idleUntil = this.tick + 4;
      this.endSequence();
   }

   private int passSource(ClientPlayerEntity var1) {
      PlayerInventory var2 = var1.method_31548();
      int var3 = findStoredTotem(var2);
      if (var1.method_6079().method_7960()) {
         return var3 != -1 ? var3 : this.hotbarSource(var2);
      } else {
         return this.refillWanted(var2) ? var3 : -1;
      }
   }

   private void startHotbarSwap(PlayerInventory var1) {
      this.totemSlot = this.hotbarSource(var1);
      this.previousSlot = var1.method_67532();
      this.reselects = 0;
      var1.method_61496(this.totemSlot);
      this.schedule(AutoInvTotemModule.Step.HOTBAR_SWAP, this.swapDelay.getInt());
   }

   private void hotbarSwap(MinecraftClient var1, ClientPlayerEntity var2) {
      if (var1.field_1755 != null) {
         this.readyAt = this.tick + 1;
      } else if (!var2.method_6079().method_7960()) {
         this.schedule(AutoInvTotemModule.Step.HOTBAR_RETURN, 0);
      } else {
         PlayerInventory var3 = var2.method_31548();
         if (var3.method_67532() == this.totemSlot && var2.method_6047().method_31574(Items.field_8288)) {
            ClientPlayNetworkHandler var5 = var1.method_1562();
            ((AccessorElytraFlyMultiPlayerGameMode)var1.field_1761).efn$elytraFlySyncSelected();

            while (var1.field_1690.field_1831.method_1436()) {
            }

            var5.method_52787(new PlayerActionC2SPacket(Action.field_12969, BlockPos.field_10980, Direction.field_11033));
            this.confirmDeadline = this.tick + confirmWindow(var5, var2);
            this.swappedAt = this.tick;
            this.schedule(AutoInvTotemModule.Step.HOTBAR_RETURN, this.switchBackDelay.getInt());
         } else {
            int var4 = this.hotbarSource(var3);
            if (var4 != -1 && this.reselects++ < 2) {
               this.totemSlot = var4;
               var3.method_61496(var4);
               this.schedule(AutoInvTotemModule.Step.HOTBAR_SWAP, this.swapDelay.getInt());
            } else {
               this.idleUntil = this.tick + 10;
               this.schedule(AutoInvTotemModule.Step.HOTBAR_RETURN, 0);
            }
         }
      }
   }

   private void hotbarReturn(MinecraftClient var1, ClientPlayerEntity var2) {
      if (var1.field_1755 == null && (this.swappedAt != this.tick || !var2.method_6115() || var2.method_6058() != Hand.field_5808)) {
         this.restoreSlot(var2.method_31548());
         this.endSequence();
      } else {
         this.readyAt = this.tick + 1;
      }
   }

   private void restoreSlot(PlayerInventory var1) {
      if (PlayerInventory.method_7380(this.previousSlot) && this.previousSlot != this.totemSlot && var1.method_67532() == this.totemSlot) {
         var1.method_61496(this.previousSlot);
      }
   }

   private int hotbarSource(PlayerInventory var1) {
      int var2 = var1.method_67532();
      int var3 = this.hotbarRefill.get() ? this.hotbarSlot.getInt() - 1 : -1;
      int var4 = -1;

      for (int var5 = 0; var5 < 9; var5++) {
         if (var5 != var2 && var1.method_5438(var5).method_31574(Items.field_8288)) {
            if (var5 != var3) {
               return var5;
            }

            var4 = var5;
         }
      }

      if (var4 != -1) {
         return var4;
      } else {
         return !this.totemHand.holding() && var1.method_5438(var2).method_31574(Items.field_8288) ? var2 : -1;
      }
   }

   private static int findStoredTotem(PlayerInventory var0) {
      for (int var1 = 9; var1 < 36; var1++) {
         if (var0.method_5438(var1).method_31574(Items.field_8288)) {
            return var1;
         }
      }

      return -1;
   }

   private static int confirmWindow(ClientPlayNetworkHandler var0, ClientPlayerEntity var1) {
      PlayerListEntry var2 = var0.method_2871(var1.method_5667());
      int var3 = var2 != null ? var2.method_2959() : 0;
      return MathHelper.method_15340(var3 / 25 + 4, 10, 40);
   }

   private boolean awaitingServer(ClientPlayerEntity var1) {
      return this.confirmDeadline >= 0 && this.tick < this.confirmDeadline && !var1.method_6079().method_31574(Items.field_8288);
   }

   private void schedule(AutoInvTotemModule.Step var1, int var2) {
      this.step = var1;
      this.readyAt = this.tick + var2;
   }

   private void endSequence() {
      this.step = AutoInvTotemModule.Step.IDLE;
      this.reactionAt = -1;
      this.openedInventory = false;
      this.passSwaps = 0;
      this.unsafeTicks = 0;
      this.totemSlot = -1;
      this.previousSlot = -1;
      this.reselects = 0;
   }

   private void reset() {
      this.endSequence();
      this.confirmDeadline = -1;
      this.totemHand.reset();
   }

   @Environment(EnvType.CLIENT)
   private static enum Step {
      IDLE,
      INVENTORY_SWAP,
      INVENTORY_CLOSE,
      HOTBAR_SWAP,
      HOTBAR_RETURN;

      private Step() {
      }
   }
}
