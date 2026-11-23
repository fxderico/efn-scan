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
         ClientPlayerEntity var1 = MinecraftClient.getInstance().player;
         if (var1 != null && !var1.isSpectator() && !var1.isInCreativeMode()) {
            PlayerInventory var2 = var1.getInventory();
            return var0.step != AutoInvTotemModule.Step.IDLE
               || var0.totemHand.holding()
               || var0.awaitingServer(var1)
               || var0.clickedAt == var0.tick
               || var1.getOffHandStack().isEmpty() && (findStoredTotem(var2) != -1 || var0.hotbarSource(var2) != -1);
         } else {
            return false;
         }
      } else {
         return AutoTotemModule.busy();
      }
   }

   public static boolean clickSafe(ClientPlayerEntity clientPlayerEntity) {
      AccessorLocalPlayer var1 = (AccessorLocalPlayer)clientPlayerEntity;
      PlayerInput var2 = var1.efn$lastSentInput();
      return !var1.efn$wasSprinting() && !var2.forward() && !var2.backward() && !var2.left() && !var2.right() && !var2.jump() && !var2.sneak();
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
      MinecraftClient var1 = MinecraftClient.getInstance();
      ClientPlayerEntity var2 = var1.player;
      if (var2 != null) {
         PlayerInventory var3 = var2.getInventory();
         if (this.openedInventory && var1.currentScreen instanceof InventoryScreen) {
            var2.closeHandledScreen();
         }

         if ((this.step == AutoInvTotemModule.Step.HOTBAR_SWAP || this.step == AutoInvTotemModule.Step.HOTBAR_RETURN) && var1.currentScreen == null) {
            this.restoreSlot(var3);
         }

         this.totemHand.putBack(var3);
      }

      this.reset();
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onTick(TickEvent tickEvent) {
      this.tick++;
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null
         && var2.world != null
         && var2.interactionManager != null
         && var2.getNetworkHandler() != null
         && !var3.isDead()
         && !var3.isSpectator()
         && !var3.isInCreativeMode()) {
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
         if (var3 != null && this.openedInventory && var2.currentScreen instanceof InventoryScreen) {
            var3.closeHandledScreen();
         }

         this.reset();
      }
   }

   private void tryStart(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var4 = clientPlayerEntity.getInventory();
      if (!clientPlayerEntity.getOffHandStack().isEmpty()) {
         this.confirmDeadline = -1;
      }

      boolean var3 = clientPlayerEntity.getOffHandStack().isEmpty()
         && !this.awaitingServer(clientPlayerEntity)
         && (findStoredTotem(var4) != -1 || this.hotbarSource(var4) != -1);
      if (!var3 && !this.refillDue(minecraftClient, clientPlayerEntity)) {
         this.reactionAt = -1;
      } else {
         if (this.reactionAt < 0) {
            this.reactionAt = this.tick + this.reactionDelay.getInt();
         }

         if (this.tick >= this.reactionAt && this.tick >= this.idleUntil) {
            if (minecraftClient.currentScreen instanceof HandledScreen) {
               this.startPass(false);
            } else if (minecraftClient.currentScreen == null) {
               boolean var6 = !minecraftClient.interactionManager.hasRidingInventory() && !sneakForced();
               if (!var3 || this.hotbarSource(var4) == -1 || !this.mode.is("Hotbar Swap") && var6) {
                  if (var6) {
                     minecraftClient.setScreen(new InventoryScreen(clientPlayerEntity));
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

   private boolean refillDue(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var3 = clientPlayerEntity.getInventory();
      return !clientPlayerEntity.getOffHandStack().isEmpty() && this.refillWanted(var3) && findStoredTotem(var3) != -1
         ? minecraftClient.currentScreen instanceof HandledScreen
            || minecraftClient.currentScreen == null && clickSafe(clientPlayerEntity) && !this.totemHand.holding() && !CombatMacroLock.heldByOther(this)
         : false;
   }

   private boolean refillWanted(PlayerInventory playerInventory) {
      return this.hotbarRefill.get() && !playerInventory.getStack(this.hotbarSlot.getInt() - 1).isOf(Items.TOTEM_OF_UNDYING);
   }

   private static boolean sneakForced() {
      SneakModule var0 = SneakModule.active;
      return var0 != null && (var0.doVanilla() || var0.doPacket());
   }

   private void startPass(boolean bl) {
      this.openedInventory = bl;
      this.passSwaps = 0;
      this.unsafeTicks = 0;
      this.schedule(AutoInvTotemModule.Step.INVENTORY_SWAP, bl ? Math.max(1, this.swapDelay.getInt()) : this.swapDelay.getInt());
   }

   private void inventorySwap(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (minecraftClient.currentScreen instanceof HandledScreen var3 && (!this.openedInventory || minecraftClient.currentScreen instanceof InventoryScreen)) {
         ScreenHandler var7 = var3.getScreenHandler();
         if (clickSafe(clientPlayerEntity) && var7.getCursorStack().isEmpty()) {
            int var5 = this.passSource(clientPlayerEntity);
            if (var5 != -1 && this.passSwaps < 2) {
               int var6 = clientPlayerEntity.getOffHandStack().isEmpty() ? 40 : this.hotbarSlot.getInt() - 1;
               var7.getSlotIndex(clientPlayerEntity.getInventory(), var5)
                  .ifPresent(j -> minecraftClient.interactionManager.clickSlot(var7.syncId, j, var6, SlotActionType.SWAP, clientPlayerEntity));
               this.clickedAt = this.tick;
               this.passSwaps++;
               if (this.passSource(clientPlayerEntity) != -1 && this.passSwaps < 2) {
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

   private void closeInventory(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (this.passSource(clientPlayerEntity) != -1 && this.passSwaps < 2 && this.unsafeTicks <= 10 && minecraftClient.currentScreen instanceof HandledScreen) {
         this.schedule(AutoInvTotemModule.Step.INVENTORY_SWAP, Math.max(1, this.swapDelay.getInt()));
      } else {
         if (this.openedInventory && minecraftClient.currentScreen instanceof InventoryScreen) {
            if (!clickSafe(clientPlayerEntity) && ++this.unsafeTicks <= 10) {
               this.readyAt = this.tick + 1;
               return;
            }

            clientPlayerEntity.closeHandledScreen();
         }

         this.finishPass();
      }
   }

   private void finishPass() {
      this.idleUntil = this.tick + 4;
      this.endSequence();
   }

   private int passSource(ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var2 = clientPlayerEntity.getInventory();
      int var3 = findStoredTotem(var2);
      if (clientPlayerEntity.getOffHandStack().isEmpty()) {
         return var3 != -1 ? var3 : this.hotbarSource(var2);
      } else {
         return this.refillWanted(var2) ? var3 : -1;
      }
   }

   private void startHotbarSwap(PlayerInventory playerInventory) {
      this.totemSlot = this.hotbarSource(playerInventory);
      this.previousSlot = playerInventory.getSelectedSlot();
      this.reselects = 0;
      playerInventory.setSelectedSlot(this.totemSlot);
      this.schedule(AutoInvTotemModule.Step.HOTBAR_SWAP, this.swapDelay.getInt());
   }

   private void hotbarSwap(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (minecraftClient.currentScreen != null) {
         this.readyAt = this.tick + 1;
      } else if (!clientPlayerEntity.getOffHandStack().isEmpty()) {
         this.schedule(AutoInvTotemModule.Step.HOTBAR_RETURN, 0);
      } else {
         PlayerInventory var3 = clientPlayerEntity.getInventory();
         if (var3.getSelectedSlot() == this.totemSlot && clientPlayerEntity.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            ClientPlayNetworkHandler var5 = minecraftClient.getNetworkHandler();
            ((AccessorElytraFlyMultiPlayerGameMode)minecraftClient.interactionManager).efn$elytraFlySyncSelected();

            while (minecraftClient.options.swapHandsKey.wasPressed()) {
            }

            var5.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
            this.confirmDeadline = this.tick + confirmWindow(var5, clientPlayerEntity);
            this.swappedAt = this.tick;
            this.schedule(AutoInvTotemModule.Step.HOTBAR_RETURN, this.switchBackDelay.getInt());
         } else {
            int var4 = this.hotbarSource(var3);
            if (var4 != -1 && this.reselects++ < 2) {
               this.totemSlot = var4;
               var3.setSelectedSlot(var4);
               this.schedule(AutoInvTotemModule.Step.HOTBAR_SWAP, this.swapDelay.getInt());
            } else {
               this.idleUntil = this.tick + 10;
               this.schedule(AutoInvTotemModule.Step.HOTBAR_RETURN, 0);
            }
         }
      }
   }

   private void hotbarReturn(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (minecraftClient.currentScreen == null
         && (this.swappedAt != this.tick || !clientPlayerEntity.isUsingItem() || clientPlayerEntity.getActiveHand() != Hand.MAIN_HAND)) {
         this.restoreSlot(clientPlayerEntity.getInventory());
         this.endSequence();
      } else {
         this.readyAt = this.tick + 1;
      }
   }

   private void restoreSlot(PlayerInventory playerInventory) {
      if (PlayerInventory.isValidHotbarIndex(this.previousSlot) && this.previousSlot != this.totemSlot && playerInventory.getSelectedSlot() == this.totemSlot) {
         playerInventory.setSelectedSlot(this.previousSlot);
      }
   }

   private int hotbarSource(PlayerInventory playerInventory) {
      int var2 = playerInventory.getSelectedSlot();
      int var3 = this.hotbarRefill.get() ? this.hotbarSlot.getInt() - 1 : -1;
      int var4 = -1;

      for (int var5 = 0; var5 < 9; var5++) {
         if (var5 != var2 && playerInventory.getStack(var5).isOf(Items.TOTEM_OF_UNDYING)) {
            if (var5 != var3) {
               return var5;
            }

            var4 = var5;
         }
      }

      if (var4 != -1) {
         return var4;
      } else {
         return !this.totemHand.holding() && playerInventory.getStack(var2).isOf(Items.TOTEM_OF_UNDYING) ? var2 : -1;
      }
   }

   private static int findStoredTotem(PlayerInventory playerInventory) {
      for (int var1 = 9; var1 < 36; var1++) {
         if (playerInventory.getStack(var1).isOf(Items.TOTEM_OF_UNDYING)) {
            return var1;
         }
      }

      return -1;
   }

   private static int confirmWindow(ClientPlayNetworkHandler clientPlayNetworkHandler, ClientPlayerEntity clientPlayerEntity) {
      PlayerListEntry var2 = clientPlayNetworkHandler.getPlayerListEntry(clientPlayerEntity.getUuid());
      int var3 = var2 != null ? var2.getLatency() : 0;
      return MathHelper.clamp(var3 / 25 + 4, 10, 40);
   }

   private boolean awaitingServer(ClientPlayerEntity clientPlayerEntity) {
      return this.confirmDeadline >= 0 && this.tick < this.confirmDeadline && !clientPlayerEntity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
   }

   private void schedule(AutoInvTotemModule.Step step, int i) {
      this.step = step;
      this.readyAt = this.tick + i;
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
