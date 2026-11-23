package net.efn.client.module.impl.combat;

import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;

@Environment(EnvType.CLIENT)
public final class KeyThrowModule extends ClientModule {
   private static final int BUFFER_TICKS = 10;
   private final KeySetting pearlKey = this.addSetting("pearl-key", new KeySetting("Pearl Key", -1));
   private final KeySetting windChargeKey = this.addSetting("wind-charge-key", new KeySetting("Wind Charge Key", -1));
   private final SlotReturn slots = new SlotReturn(this);
   private Item pending;
   private int pendingTicks;

   public KeyThrowModule() {
      super("key-throw", "Key Throw", "Throws a pearl or a wind charge with a key and swaps back.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.pending = null;
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
      this.on(TickEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onTick);
   }

   @Override
   protected void onDisable() {
      this.pending = null;
      this.slots.drop(MinecraftClient.method_1551(), true);
   }

   private void onKey(KeyInputEvent var1) {
      if (var1.action == 1) {
         this.onPress(var1.key);
      }
   }

   private void onMouse(MouseInputEvent var1) {
      if (var1.action == 1) {
         this.onPress(1000 + var1.button);
      }
   }

   private void onPress(int var1) {
      if (ModuleKeybinds.isBindable(var1)) {
         Item var2 = var1 == this.pearlKey.value() ? Items.field_8634 : (var1 == this.windChargeKey.value() ? Items.field_49098 : null);
         MinecraftClient var3 = MinecraftClient.method_1551();
         if (var2 != null && var3.field_1755 == null && var3.field_1724 != null) {
            this.pending = var2;
            this.pendingTicks = 0;
         }
      }
   }

   private void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      this.slots.tick(var2, true);
      if (var2.field_1724 == null) {
         this.pending = null;
      } else if (this.pending != null) {
         this.tryThrow(var2, var2.field_1724);
      }
   }

   private void tryThrow(MinecraftClient var1, ClientPlayerEntity var2) {
      if (++this.pendingTicks > 10) {
         this.pending = null;
      } else if (MeleeAttacks.canAct(var1) && MeleeAttacks.handsFree(var1, var2) && !MeleeAttacks.actedThisTick(var2)) {
         if (!CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()) {
            PlayerInventory var8 = var2.method_31548();
            boolean var9 = var2.method_6079().method_31574(this.pending);
            int var7 = var2.method_6047().method_31574(this.pending) ? var8.method_67532() : Crystals.hotbarSlotOf(var8, this.pending);
            if (!var9 && var7 < 0) {
               this.pending = null;
            } else {
               ItemStack var6 = var9 ? var2.method_6079() : var8.method_5438(var7);
               if (!var2.method_7357().method_7904(var6)) {
                  boolean var5 = this.pending == Items.field_49098;
                  if (!ServerRotation.pending() && (!var5 || ServerRotation.canRotate(var2))) {
                     if (var9 || this.slots.take(var2, var7)) {
                        this.pending = null;
                        if (var5) {
                           ServerRotation.request(new Rotation(var2.method_36454(), 90.0F));
                           if (var2.method_24828()) {
                              InputControl.jump();
                           }
                        }

                        Hand var3;
                        ActionResult var4;
                        if ((var4 = var1.field_1761.method_2919(var2, var3 = var9 ? Hand.field_5810 : Hand.field_5808)) instanceof Success) {
                           Success var13 = (Success)var4;
                           if (var13.comp_2909() == SwingSource.field_52427) {
                              var2.method_6104(var3);
                           }

                           var1.field_1773.field_4012.method_3215(var3);
                        }

                        MeleeAttacks.claimTick(var1);
                     }
                  }
               }
            }
         }
      }
   }
}
