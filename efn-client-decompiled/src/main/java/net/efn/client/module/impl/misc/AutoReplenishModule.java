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
   private ItemStack activeReference = ItemStack.field_8037;
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
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 != null && var2.field_1761 != null) {
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
                     if (var2.field_1755 != null && !(var2.field_1755 instanceof InventoryScreen)) {
                        return;
                     }

                     if (var3.method_56992() || AutoInvTotemModule.busy()) {
                        return;
                     }

                     if (this.isMovingFast(var3) || ((AccessorLocalPlayer)var3).efn$lastSentInput().comp_3164()) {
                        return;
                     }

                     if (var3.method_6115() || var2.field_1761.method_2923()) {
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
                     this.pendingRefill[var11] = ItemStack.field_8037;
                     this.wasScreenOpen = var2.field_1755 instanceof InventoryScreen;
                     this.stage = this.wasScreenOpen ? 2 : 1;
                     break;
                  case 1:
                     var2.method_1507(new InventoryScreen(var3));
                     this.stage = 2;
                     this.delayTimer = HumanInventoryTiming.openGuiDelayTicks();
                     return;
                  case 2:
                     if (!(var2.field_1755 instanceof InventoryScreen var8)) {
                        if (this.activeSlot >= 0 && !this.activeReference.method_7960() && this.pendingRefill[this.activeSlot].method_7960()) {
                           this.pendingRefill[this.activeSlot] = this.activeReference;
                        }

                        this.resetJob();
                        return;
                     }

                     boolean var6 = AutoInvTotemModule.clickSafe(var3) && !AutoInvTotemModule.busy();
                     if (!var6 && ++this.waitTicks <= 10) {
                        return;
                     }

                     if (!var6 && this.activeSlot >= 0 && this.pendingRefill[this.activeSlot].method_7960()) {
                        this.pendingRefill[this.activeSlot] = this.activeReference;
                     }

                     Slot var5;
                     if (var6 && this.stillNeedsRefill(var3) && (var5 = this.findSourceSlot(var8.method_17577(), var3)) != null) {
                        int var10 = this.activeSlot == 9 ? 40 : this.activeSlot;
                        ((AccessorElytraFlyMultiPlayerGameMode)var2.field_1761).efn$elytraFlySyncSelected();
                        var2.field_1761
                           .method_2906(((PlayerScreenHandler)var8.method_17577()).field_7763, var5.field_7874, var10, SlotActionType.field_7791, var3);
                     }

                     this.stage = 3;
                     this.delayTimer = HumanInventoryTiming.closeGuiDelayTicks();
                     return;
                  case 3:
                     if (!this.wasScreenOpen && var2.field_1755 instanceof InventoryScreen) {
                        var2.field_1724.method_7346();
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

   private void refillNow(MinecraftClient var1, ClientPlayerEntity var2) {
      if (var1.field_1755 == null && !var2.method_56992() && !var2.method_6115()) {
         if (AutoInvTotemModule.clickSafe(var2) && !AutoInvTotemModule.busy()) {
            int var3 = this.nextPending(var2);
            if (var3 >= 0) {
               this.activeSlot = var3;
               this.activeReference = this.pendingRefill[var3];
               this.pendingRefill[var3] = ItemStack.field_8037;
               PlayerScreenHandler var4 = var2.field_7498;
               Slot var5 = this.findSourceSlot(var4, var2);
               if (var5 != null && var4.method_34255().method_7960()) {
                  int var6 = this.activeSlot == 9 ? 40 : this.activeSlot;
                  ((AccessorElytraFlyMultiPlayerGameMode)var1.field_1761).efn$elytraFlySyncSelected();
                  var1.field_1761.method_2906(var4.field_7763, var5.field_7874, var6, SlotActionType.field_7791, var2);
                  this.delayTimer = this.delay.getInt();
               }

               this.activeSlot = -1;
               this.activeReference = ItemStack.field_8037;
            }
         }
      }
   }

   private void resync(ClientPlayerEntity var1) {
      this.trackedPlayer = var1;

      for (int var2 = 0; var2 < 10; var2++) {
         ItemStack var3 = var2 == 9 ? var1.method_6079() : var1.method_31548().method_5438(var2);
         this.lastKnown[var2] = var3.method_7972();
         this.pendingRefill[var2] = ItemStack.field_8037;
      }

      this.resetJob();
   }

   private void updateTracking(MinecraftClient var1, ClientPlayerEntity var2) {
      boolean var3 = this.stage == 0 && var1.field_1755 instanceof HandledScreen;
      boolean var4 = var2.method_56992();
      boolean var5 = false;
      int var6 = var2.method_31548().method_67532();
      if (var6 != this.lastSelectedSlot) {
         this.lastSelectedSlot = var6;
         var5 = true;
      }

      for (int var7 = 0; var7 < 10; var7++) {
         if (var7 == 9 && !this.offhand.get()) {
            this.lastKnown[var7] = var2.method_6079().method_7972();
            this.pendingRefill[var7] = ItemStack.field_8037;
         } else {
            ItemStack var9 = this.lastKnown[var7];
            ItemStack var10 = var7 == 9 ? var2.method_6079() : var2.method_31548().method_5438(var7);
            if (!ItemStack.method_7973(var9, var10)) {
               var5 = true;
            }

            this.lastKnown[var7] = var10.method_7972();
            if (var10.method_31574(Items.field_8288) || var9.method_31574(Items.field_8288)) {
               this.pendingRefill[var7] = ItemStack.field_8037;
            } else if (var4) {
               this.pendingRefill[var7] = ItemStack.field_8037;
            } else if (!var3) {
               boolean var8 = var9.method_7960() || var10.method_7909() == var9.method_7909();
               if (var10.method_7960() && !var9.method_7960()) {
                  this.pendingRefill[var7] = var9.method_7972();
               } else if (!var9.method_7960() && !var9.method_7946() && !var8) {
                  this.pendingRefill[var7] = var9.method_7972();
               } else if (!var10.method_7946()
                  || var10.method_7960()
                  || !var8
                  || var10.method_7947() > this.threshold.getInt()
                  || !this.pendingRefill[var7].method_7960() && this.pendingRefill[var7].method_7909() != var10.method_7909()) {
                  if (!var10.method_7960() && var10.method_7947() > this.threshold.getInt()) {
                     this.pendingRefill[var7] = ItemStack.field_8037;
                  }
               } else {
                  this.pendingRefill[var7] = var10.method_7972();
               }
            }
         }
      }

      this.quietTicks = var5 ? 0 : Math.min(this.quietTicks + 1, 1000);
   }

   private int nextPending(ClientPlayerEntity var1) {
      for (int var2 = 0; var2 < 10; var2++) {
         if (!this.pendingRefill[var2].method_7960() && this.hasSource(var1, var2, this.pendingRefill[var2])) {
            return var2;
         }
      }

      return -1;
   }

   private boolean stillNeedsRefill(ClientPlayerEntity var1) {
      ItemStack var2 = this.activeSlot == 9 ? var1.method_6079() : var1.method_31548().method_5438(this.activeSlot);
      if (var2.method_7960()) {
         return true;
      } else {
         return var2.method_7909() != this.activeReference.method_7909() ? true : var2.method_7946() && var2.method_7947() <= this.threshold.getInt();
      }
   }

   private boolean hasSource(ClientPlayerEntity var1, int var2, ItemStack var3) {
      ItemStack var4 = var2 == 9 ? var1.method_6079() : var1.method_31548().method_5438(var2);

      for (int var5 = 9; var5 <= 35; var5++) {
         if (isViableSource(var1.method_31548().method_5438(var5), var4, var3)) {
            return true;
         }
      }

      return false;
   }

   private Slot findSourceSlot(ScreenHandler var1, ClientPlayerEntity var2) {
      ItemStack var3 = this.activeSlot == 9 ? var2.method_6079() : var2.method_31548().method_5438(this.activeSlot);
      int var4 = ItemStack.method_7984(var3, this.activeReference) ? var3.method_7947() : 0;
      Slot var5 = null;

      for (Slot var7 : var1.field_7761) {
         ItemStack var8;
         if (var7.field_7874 >= 9 && var7.field_7874 <= 35 && matchesReference(var8 = var7.method_7677(), this.activeReference) && var8.method_7947() > var4) {
            var5 = var7;
            var4 = var8.method_7947();
         }
      }

      return var5;
   }

   private static boolean isViableSource(ItemStack var0, ItemStack var1, ItemStack var2) {
      int var3 = ItemStack.method_7984(var1, var2) ? var1.method_7947() : 0;
      return matchesReference(var0, var2) && var0.method_7947() > var3;
   }

   private static boolean matchesReference(ItemStack var0, ItemStack var1) {
      if (var0.method_7960()) {
         return false;
      } else {
         return var1.method_7946() ? ItemStack.method_31577(var0, var1) : ItemStack.method_75222(var0, var1, var0x -> var0x == DataComponentTypes.field_49629);
      }
   }

   private boolean isMovingFast(ClientPlayerEntity var1) {
      if (var1.method_5624()) {
         return true;
      } else if (var1.method_5765()) {
         Entity var4 = var1.method_5668();
         double var5 = var4.method_23317() - var4.field_6014;
         double var7;
         return var5 * var5 + (var7 = var4.method_23321() - var4.field_5969) * var7 > 0.06;
      } else {
         Vec3d var2 = var1.method_18798();
         return var2.field_1352 * var2.field_1352 + var2.field_1350 * var2.field_1350 > 0.06;
      }
   }

   private void resetJob() {
      this.stage = 0;
      this.delayTimer = 0;
      this.waitTicks = 0;
      this.activeSlot = -1;
      this.activeReference = ItemStack.field_8037;
      this.wasScreenOpen = false;
   }
}
