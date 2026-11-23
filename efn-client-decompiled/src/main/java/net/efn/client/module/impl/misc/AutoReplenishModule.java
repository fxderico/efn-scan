package net.efn.client.module.impl.misc;

import com.example.client.mixin.AccessorElytraFlyMultiPlayerGameMode;
import com.example.client.mixin.AccessorLocalPlayer;
import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.service.humanize.HumanInventoryTiming;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class AutoReplenishModule extends ClientModule {
   private static final int TRACKED_SLOTS = 10;
   private static final int OFFHAND_TRACKED_SLOT = 9;
   private static final int OFFHAND_SWAP_BUTTON = 40;
   private static final int MAIN_INVENTORY_FIRST = 9;
   private static final int MAIN_INVENTORY_LAST = 35;
   private static final double FAST_MOVEMENT_THRESHOLD_SQ = 0.06;
   private static final String MODE_INSTANT = "Instant";
   private static final String MODE_LEGIT = "Legit";
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Instant", "Instant", "Legit"));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 1.0F, 0.0F, 20.0F).step(1.0F));
   private final NumberSetting threshold = this.addSetting("threshold", new NumberSetting("Threshold", 8.0F, 1.0F, 64.0F).step(1.0F));
   private final BooleanSetting offhand = this.addSetting("offhand", new BooleanSetting("Offhand", true));
   private final ItemStack[] lastKnown = new ItemStack[10];
   private final ItemStack[] pendingRefill = new ItemStack[10];
   private static final int QUIET_TICKS_BEFORE_OPEN = 10;
   private static final int MAX_WAIT_TICKS = 10;
   private ClientPlayerEntity trackedPlayer;
   private int stage;
   private int delayTimer;
   private int quietTicks;
   private int waitTicks;
   private int lastSelectedSlot = -1;
   private int activeSlot = -1;
   private ItemStack activeReference = ItemStack.EMPTY;
   private boolean wasScreenOpen;

   public AutoReplenishModule() {
      super("auto-replenish", "Auto Replenish", "Refills a hotbar or offhand slot from the main inventory once it runs low.", ModuleCategory.MISC);
      this.delay.visibleWhen(() -> this.mode.is("Instant"));
      this.mode.onChange(this::resetJob);
   }

   @Override
   protected void onEnable() {
      this.resetJob();
   }

   @Override
   protected void onDisable() {
      this.resetJob();
      this.trackedPlayer = null;
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null && var2.interactionManager != null) {
         if (var3 != this.trackedPlayer) {
            this.resync(var3);
         }

         this.updateTracking(var2, var3);
         if (this.delayTimer > 0) {
            this.delayTimer--;
         } else if (this.mode.is("Instant")) {
            this.refillNow(var2, var3);
         } else {
            for (int var4 = 0; var4 < 3; var4++) {
               switch (this.stage) {
                  case 0:
                     if (var2.currentScreen != null && !(var2.currentScreen instanceof InventoryScreen)) {
                        return;
                     }

                     if (var3.isInCreativeMode() || AutoInvTotemModule.busy()) {
                        return;
                     }

                     if (this.isMovingFast(var3) || ((AccessorLocalPlayer)var3).efn$lastSentInput().sneak()) {
                        return;
                     }

                     if (var3.isUsingItem() || var2.interactionManager.isBreakingBlock()) {
                        return;
                     }

                     if (this.quietTicks < 10) {
                        return;
                     }

                     int var11 = this.nextPending(var3);
                     if (var11 < 0) {
                        return;
                     }

                     this.activeSlot = var11;
                     this.activeReference = this.pendingRefill[var11];
                     this.pendingRefill[var11] = ItemStack.EMPTY;
                     this.wasScreenOpen = var2.currentScreen instanceof InventoryScreen;
                     this.stage = this.wasScreenOpen ? 2 : 1;
                     break;
                  case 1:
                     var2.setScreen(new InventoryScreen(var3));
                     this.stage = 2;
                     this.delayTimer = HumanInventoryTiming.openGuiDelayTicks();
                     return;
                  case 2:
                     if (!(var2.currentScreen instanceof InventoryScreen var8)) {
                        if (this.activeSlot >= 0 && !this.activeReference.isEmpty() && this.pendingRefill[this.activeSlot].isEmpty()) {
                           this.pendingRefill[this.activeSlot] = this.activeReference;
                        }

                        this.resetJob();
                        return;
                     }

                     boolean var6 = AutoInvTotemModule.clickSafe(var3) && !AutoInvTotemModule.busy();
                     if (!var6 && ++this.waitTicks <= 10) {
                        return;
                     }

                     if (!var6 && this.activeSlot >= 0 && this.pendingRefill[this.activeSlot].isEmpty()) {
                        this.pendingRefill[this.activeSlot] = this.activeReference;
                     }

                     Slot var5;
                     if (var6 && this.stillNeedsRefill(var3) && (var5 = this.findSourceSlot(var8.getScreenHandler(), var3)) != null) {
                        int var10 = this.activeSlot == 9 ? 40 : this.activeSlot;
                        ((AccessorElytraFlyMultiPlayerGameMode)var2.interactionManager).efn$elytraFlySyncSelected();
                        var2.interactionManager.clickSlot(var8.getScreenHandler().syncId, var5.id, var10, SlotActionType.SWAP, var3);
                     }

                     this.stage = 3;
                     this.delayTimer = HumanInventoryTiming.closeGuiDelayTicks();
                     return;
                  case 3:
                     if (!this.wasScreenOpen && var2.currentScreen instanceof InventoryScreen) {
                        var2.player.closeHandledScreen();
                     }

                     this.resetJob();
                     return;
                  default:
                     this.resetJob();
               }
            }
         }
      }
   }

   private void refillNow(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (minecraftClient.currentScreen == null && !clientPlayerEntity.isInCreativeMode() && !clientPlayerEntity.isUsingItem()) {
         if (AutoInvTotemModule.clickSafe(clientPlayerEntity) && !AutoInvTotemModule.busy()) {
            int var3 = this.nextPending(clientPlayerEntity);
            if (var3 >= 0) {
               this.activeSlot = var3;
               this.activeReference = this.pendingRefill[var3];
               this.pendingRefill[var3] = ItemStack.EMPTY;
               PlayerScreenHandler var4 = clientPlayerEntity.playerScreenHandler;
               Slot var5 = this.findSourceSlot(var4, clientPlayerEntity);
               if (var5 != null && var4.getCursorStack().isEmpty()) {
                  int var6 = this.activeSlot == 9 ? 40 : this.activeSlot;
                  ((AccessorElytraFlyMultiPlayerGameMode)minecraftClient.interactionManager).efn$elytraFlySyncSelected();
                  minecraftClient.interactionManager.clickSlot(var4.syncId, var5.id, var6, SlotActionType.SWAP, clientPlayerEntity);
                  this.delayTimer = this.delay.getInt();
               }

               this.activeSlot = -1;
               this.activeReference = ItemStack.EMPTY;
            }
         }
      }
   }

   private void resync(ClientPlayerEntity clientPlayerEntity) {
      this.trackedPlayer = clientPlayerEntity;

      for (int var2 = 0; var2 < 10; var2++) {
         ItemStack var3 = var2 == 9 ? clientPlayerEntity.getOffHandStack() : clientPlayerEntity.getInventory().getStack(var2);
         this.lastKnown[var2] = var3.copy();
         this.pendingRefill[var2] = ItemStack.EMPTY;
      }

      this.resetJob();
   }

   private void updateTracking(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      boolean var3 = this.stage == 0 && minecraftClient.currentScreen instanceof HandledScreen;
      boolean var4 = clientPlayerEntity.isInCreativeMode();
      boolean var5 = false;
      int var6 = clientPlayerEntity.getInventory().getSelectedSlot();
      if (var6 != this.lastSelectedSlot) {
         this.lastSelectedSlot = var6;
         var5 = true;
      }

      for (int var7 = 0; var7 < 10; var7++) {
         if (var7 == 9 && !this.offhand.get()) {
            this.lastKnown[var7] = clientPlayerEntity.getOffHandStack().copy();
            this.pendingRefill[var7] = ItemStack.EMPTY;
         } else {
            ItemStack var9 = this.lastKnown[var7];
            ItemStack var10 = var7 == 9 ? clientPlayerEntity.getOffHandStack() : clientPlayerEntity.getInventory().getStack(var7);
            if (!ItemStack.areEqual(var9, var10)) {
               var5 = true;
            }

            this.lastKnown[var7] = var10.copy();
            if (var10.isOf(Items.TOTEM_OF_UNDYING) || var9.isOf(Items.TOTEM_OF_UNDYING)) {
               this.pendingRefill[var7] = ItemStack.EMPTY;
            } else if (var4) {
               this.pendingRefill[var7] = ItemStack.EMPTY;
            } else if (!var3) {
               boolean var8 = var9.isEmpty() || var10.getItem() == var9.getItem();
               if (var10.isEmpty() && !var9.isEmpty()) {
                  this.pendingRefill[var7] = var9.copy();
               } else if (!var9.isEmpty() && !var9.isStackable() && !var8) {
                  this.pendingRefill[var7] = var9.copy();
               } else if (!var10.isStackable()
                  || var10.isEmpty()
                  || !var8
                  || var10.getCount() > this.threshold.getInt()
                  || !this.pendingRefill[var7].isEmpty() && this.pendingRefill[var7].getItem() != var10.getItem()) {
                  if (!var10.isEmpty() && var10.getCount() > this.threshold.getInt()) {
                     this.pendingRefill[var7] = ItemStack.EMPTY;
                  }
               } else {
                  this.pendingRefill[var7] = var10.copy();
               }
            }
         }
      }

      this.quietTicks = var5 ? 0 : Math.min(this.quietTicks + 1, 1000);
   }

   private int nextPending(ClientPlayerEntity clientPlayerEntity) {
      for (int var2 = 0; var2 < 10; var2++) {
         if (!this.pendingRefill[var2].isEmpty() && this.hasSource(clientPlayerEntity, var2, this.pendingRefill[var2])) {
            return var2;
         }
      }

      return -1;
   }

   private boolean stillNeedsRefill(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var2 = this.activeSlot == 9 ? clientPlayerEntity.getOffHandStack() : clientPlayerEntity.getInventory().getStack(this.activeSlot);
      if (var2.isEmpty()) {
         return true;
      } else {
         return var2.getItem() != this.activeReference.getItem() ? true : var2.isStackable() && var2.getCount() <= this.threshold.getInt();
      }
   }

   private boolean hasSource(ClientPlayerEntity clientPlayerEntity, int i, ItemStack itemStack) {
      ItemStack var4 = i == 9 ? clientPlayerEntity.getOffHandStack() : clientPlayerEntity.getInventory().getStack(i);

      for (int var5 = 9; var5 <= 35; var5++) {
         if (isViableSource(clientPlayerEntity.getInventory().getStack(var5), var4, itemStack)) {
            return true;
         }
      }

      return false;
   }

   private Slot findSourceSlot(ScreenHandler screenHandler, ClientPlayerEntity clientPlayerEntity) {
      ItemStack var3 = this.activeSlot == 9 ? clientPlayerEntity.getOffHandStack() : clientPlayerEntity.getInventory().getStack(this.activeSlot);
      int var4 = ItemStack.areItemsEqual(var3, this.activeReference) ? var3.getCount() : 0;
      Slot var5 = null;

      for (Slot var7 : screenHandler.slots) {
         ItemStack var8;
         if (var7.id >= 9 && var7.id <= 35 && matchesReference(var8 = var7.getStack(), this.activeReference) && var8.getCount() > var4) {
            var5 = var7;
            var4 = var8.getCount();
         }
      }

      return var5;
   }

   private static boolean isViableSource(ItemStack itemStack, ItemStack itemStack2, ItemStack itemStack3) {
      int var3 = ItemStack.areItemsEqual(itemStack2, itemStack3) ? itemStack2.getCount() : 0;
      return matchesReference(itemStack, itemStack3) && itemStack.getCount() > var3;
   }

   private static boolean matchesReference(ItemStack itemStack, ItemStack itemStack2) {
      if (itemStack.isEmpty()) {
         return false;
      } else {
         return itemStack2.isStackable()
            ? ItemStack.areItemsAndComponentsEqual(itemStack, itemStack2)
            : ItemStack.shouldSkipHandAnimationOnSwap(itemStack, itemStack2, componentType -> componentType == DataComponentTypes.DAMAGE);
      }
   }

   private boolean isMovingFast(ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.isSprinting()) {
         return true;
      } else if (clientPlayerEntity.hasVehicle()) {
         Entity var4 = clientPlayerEntity.getRootVehicle();
         double var5 = var4.getX() - var4.lastX;
         double var7;
         return var5 * var5 + (var7 = var4.getZ() - var4.lastZ) * var7 > 0.06;
      } else {
         Vec3d var2 = clientPlayerEntity.getVelocity();
         return var2.x * var2.x + var2.z * var2.z > 0.06;
      }
   }

   private void resetJob() {
      this.stage = 0;
      this.delayTimer = 0;
      this.waitTicks = 0;
      this.activeSlot = -1;
      this.activeReference = ItemStack.EMPTY;
      this.wasScreenOpen = false;
   }
}
