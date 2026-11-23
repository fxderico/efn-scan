package net.efn.client.module.impl.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Optional;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class SpearLungeModule extends ClientModule {
   private static final int BUFFER_TICKS = 10;
   private static final double POKE_RANGE = 4.4;
   private static final double POKE_MARGIN = 0.125;
   private final KeySetting key = this.addSetting("lunge-key", new KeySetting("Lunge Key", -1));
   private final BooleanSetting reachPoke = this.addSetting("reach-poke", new BooleanSetting("Reach Poke", false));
   private final BooleanSetting switchBack = this.addSetting("switch-back", new BooleanSetting("Switch Back", true));
   private final SlotReturn slots = new SlotReturn(this);
   private boolean pending;
   private int pendingTicks;
   private ClientPlayerEntity owner;

   public SpearLungeModule() {
      super("spear-swap", "Spear Swap", "Swaps to your Lunge spear and lunges. Reach Poke stabs with a spear past sword reach.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.pending = false;
      this.owner = MinecraftClient.getInstance().player;
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
      this.on(TickEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onTick);
   }

   @Override
   protected void onDisable() {
      this.pending = false;
      this.owner = null;
      this.slots.drop(MinecraftClient.getInstance(), this.switchBack.get());
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
      int var2 = this.key.value();
      if (ModuleKeybinds.isBindable(var2) && i == var2) {
         MinecraftClient var3 = MinecraftClient.getInstance();
         if (var3.currentScreen == null && var3.player != null && var3.world != null) {
            if (bestSpear(var3.player.getInventory(), true) < 0) {
               ModuleChat.actionBar(this, ModuleChat.body().warn("No Lunge spear").text(" in your hotbar"));
            } else {
               this.pending = true;
               this.pendingTicks = 0;
            }
         }
      }
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      this.slots.tick(var2, this.switchBack.get());
      ClientPlayerEntity var3 = var2.player;
      if (var3 != this.owner) {
         this.owner = var3;
         this.pending = false;
      }

      if (var3 != null && var2.world != null && var2.interactionManager != null) {
         if (this.pending) {
            this.tryLunge(var2, var3);
         }

         if (this.reachPoke.get() && MeleeAttacks.clickPending(var2)) {
            this.tryPoke(var2, var3);
         }
      }
   }

   private void tryLunge(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (++this.pendingTicks > 10) {
         this.pending = false;
      } else if (this.canStab(minecraftClient, clientPlayerEntity)
         && !clientPlayerEntity.isTouchingWater()
         && !clientPlayerEntity.isGliding()
         && !clientPlayerEntity.hasVehicle()) {
         if (clientPlayerEntity.getHungerManager().canSprint() || clientPlayerEntity.getAbilities().allowFlying) {
            PlayerInventory var3 = clientPlayerEntity.getInventory();
            int var4 = bestSpear(var3, true);
            if (var4 < 0) {
               this.pending = false;
            } else {
               ItemStack var5 = var3.getStack(var4);
               if (canJab(minecraftClient, clientPlayerEntity, var5) && this.slots.take(clientPlayerEntity, var4)) {
                  this.pending = false;
                  stab(minecraftClient, clientPlayerEntity, var5.get(DataComponentTypes.PIERCING_WEAPON));
               }
            }
         }
      }
   }

   private void tryPoke(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (this.canStab(minecraftClient, clientPlayerEntity) && !clientPlayerEntity.getMainHandStack().contains(DataComponentTypes.PIERCING_WEAPON)) {
         if (MeleeAttacks.crosshairHit(minecraftClient) == null) {
            PlayerInventory var3 = clientPlayerEntity.getInventory();
            int var4 = bestSpear(var3, false);
            if (var4 >= 0) {
               ItemStack var5 = var3.getStack(var4);
               if (canJab(minecraftClient, clientPlayerEntity, var5) && enemyPastReach(clientPlayerEntity) && this.slots.take(clientPlayerEntity, var4)) {
                  stab(minecraftClient, clientPlayerEntity, var5.get(DataComponentTypes.PIERCING_WEAPON));
               }
            }
         }
      }
   }

   private static void stab(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, PiercingWeaponComponent piercingWeaponComponent) {
      minecraftClient.interactionManager.attackWithPiercingWeapon(piercingWeaponComponent);
      if (MeleeAttacks.actedThisTick(clientPlayerEntity)) {
         clientPlayerEntity.swingHand(Hand.MAIN_HAND);
         MeleeAttacks.claimTick(minecraftClient);
      }
   }

   private boolean canStab(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      return MeleeAttacks.canAct(minecraftClient)
            && MeleeAttacks.handsFree(minecraftClient, clientPlayerEntity)
            && !MeleeAttacks.actedThisTick(clientPlayerEntity)
         ? !CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()
         : false;
   }

   private static boolean canJab(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, ItemStack itemStack) {
      return minecraftClient.attackCooldown <= 0 && !minecraftClient.interactionManager.isFlyingLocked()
         ? itemStack.isItemEnabled(minecraftClient.world.getEnabledFeatures()) && !clientPlayerEntity.isBelowMinimumAttackCharge(itemStack, 0)
         : false;
   }

   private static boolean enemyPastReach(ClientPlayerEntity clientPlayerEntity) {
      Rotation var1 = ServerRotation.sent();
      Vec3d var2 = var1 != null ? Vec3d.fromPolar(var1.pitch(), var1.yaw()) : clientPlayerEntity.getRotationVec(1.0F);
      Vec3d var3 = clientPlayerEntity.getEyePos();
      Vec3d var4 = var3.add(var2.multiply(4.4));
      double var5 = clientPlayerEntity.getEntityInteractionRange();
      Box var7 = clientPlayerEntity.getBoundingBox().stretch(var2.multiply(4.4)).expand(1.0);

      for (Entity var9 : clientPlayerEntity.getEntityWorld().getOtherEntities(clientPlayerEntity, var7)) {
         Optional var10;
         if (MeleeAttacks.isEnemy(clientPlayerEntity, var9)
            && (var10 = var9.getBoundingBox().expand(0.125).raycast(var3, var4)).isPresent()
            && var3.distanceTo((Vec3d)var10.get()) > var5
            && MeleeAttacks.lineIsClear(clientPlayerEntity, var3, (Vec3d)var10.get())) {
            return true;
         }
      }

      return false;
   }

   private static int bestSpear(PlayerInventory playerInventory, boolean bl) {
      int var2 = playerInventory.getSelectedSlot();
      int var3 = -1;
      int var4 = 0;

      for (int var5 = 0; var5 < PlayerInventory.getHotbarSize(); var5++) {
         ItemStack var6 = playerInventory.getStack(var5);
         if (var6.contains(DataComponentTypes.PIERCING_WEAPON) && !var6.willBreakNextUse()) {
            int var7 = lungeLevel(var6);
            boolean var8 = !bl ? var3 < 0 || var7 < var4 : var7 > var4 || var7 > 0 && var7 == var4 && var5 == var2;
            if (var8) {
               var3 = var5;
               var4 = var7;
            }
         }
      }

      return var3;
   }

   private static int lungeLevel(ItemStack itemStack) {
      for (Entry var2 : itemStack.getEnchantments().getEnchantmentEntries()) {
         if (((RegistryEntry)var2.getKey()).matchesKey(Enchantments.LUNGE)) {
            return var2.getIntValue();
         }
      }

      return 0;
   }
}
