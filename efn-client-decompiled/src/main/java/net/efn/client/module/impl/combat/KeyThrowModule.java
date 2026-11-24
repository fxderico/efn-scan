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
      this.slots.drop(MinecraftClient.getInstance(), true);
   }

   private void onKey(KeyInputEvent keyInputEvent) {
      if (keyInputEvent.action == 1) {
         this.onPress(keyInputEvent.key);
      }
   }

   private void onMouse(MouseInputEvent mouseInputEvent) {
      if (mouseInputEvent.action == 1) {
         this.onPress(1000 + mouseInputEvent.button);
      }
   }

   private void onPress(int i) {
      if (ModuleKeybinds.isBindable(i)) {
         Item var2 = i == this.pearlKey.value() ? Items.ENDER_PEARL : (i == this.windChargeKey.value() ? Items.WIND_CHARGE : null);
         MinecraftClient var3 = MinecraftClient.getInstance();
         if (var2 != null && var3.currentScreen == null && var3.player != null) {
            this.pending = var2;
            this.pendingTicks = 0;
         }
      }
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      this.slots.tick(var2, true);
      if (var2.player == null) {
         this.pending = null;
      } else if (this.pending != null) {
         this.tryThrow(var2, var2.player);
      }
   }

   private void tryThrow(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (++this.pendingTicks > 10) {
         this.pending = null;
      } else if (MeleeAttacks.canAct(minecraftClient)
         && MeleeAttacks.handsFree(minecraftClient, clientPlayerEntity)
         && !MeleeAttacks.actedThisTick(clientPlayerEntity)) {
         if (!CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()) {
            PlayerInventory var8 = clientPlayerEntity.getInventory();
            boolean var9 = clientPlayerEntity.getOffHandStack().isOf(this.pending);
            int var7 = clientPlayerEntity.getMainHandStack().isOf(this.pending) ? var8.getSelectedSlot() : Crystals.hotbarSlotOf(var8, this.pending);
            if (!var9 && var7 < 0) {
               this.pending = null;
            } else {
               ItemStack var6 = var9 ? clientPlayerEntity.getOffHandStack() : var8.getStack(var7);
               if (!clientPlayerEntity.getItemCooldownManager().isCoolingDown(var6)) {
                  boolean var5 = this.pending == Items.WIND_CHARGE;
                  if (!ServerRotation.pending() && (!var5 || ServerRotation.canRotate(clientPlayerEntity))) {
                     if (var9 || this.slots.take(clientPlayerEntity, var7)) {
                        this.pending = null;
                        if (var5) {
                           ServerRotation.request(new Rotation(clientPlayerEntity.getYaw(), 90.0F));
                           if (clientPlayerEntity.isOnGround()) {
                              InputControl.jump();
                           }
                        }

                        Hand var3;
                        ActionResult var4;
                        if ((var4 = minecraftClient.interactionManager.interactItem(clientPlayerEntity, var3 = var9 ? Hand.OFF_HAND : Hand.MAIN_HAND)) instanceof Success
                           )
                         {
                           Success var13 = (Success)var4;
                           if (var13.swingSource() == SwingSource.CLIENT) {
                              clientPlayerEntity.swingHand(var3);
                           }

                           minecraftClient.gameRenderer.firstPersonRenderer.resetEquipProgress(var3);
                        }

                        MeleeAttacks.claimTick(minecraftClient);
                     }
                  }
               }
            }
         }
      }
   }
}
