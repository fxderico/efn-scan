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
      this.owner = MinecraftClient.method_1551().field_1724;
      this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
      this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
      this.on(TickEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onTick);
   }

   @Override
   protected void onDisable() {
      this.pending = false;
      this.owner = null;
      this.slots.drop(MinecraftClient.method_1551(), this.switchBack.get());
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
      int var2 = this.key.value();
      if (ModuleKeybinds.isBindable(var2) && var1 == var2) {
         MinecraftClient var3 = MinecraftClient.method_1551();
         if (var3.field_1755 == null && var3.field_1724 != null && var3.field_1687 != null) {
            if (bestSpear(var3.field_1724.method_31548(), true) < 0) {
               ModuleChat.actionBar(this, ModuleChat.body().warn("No Lunge spear").text(" in your hotbar"));
            } else {
               this.pending = true;
               this.pendingTicks = 0;
            }
         }
      }
   }

   private void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      this.slots.tick(var2, this.switchBack.get());
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 != this.owner) {
         this.owner = var3;
         this.pending = false;
      }

      if (var3 != null && var2.field_1687 != null && var2.field_1761 != null) {
         if (this.pending) {
            this.tryLunge(var2, var3);
         }

         if (this.reachPoke.get() && MeleeAttacks.clickPending(var2)) {
            this.tryPoke(var2, var3);
         }
      }
   }

   private void tryLunge(MinecraftClient var1, ClientPlayerEntity var2) {
      if (++this.pendingTicks > 10) {
         this.pending = false;
      } else if (this.canStab(var1, var2) && !var2.method_5799() && !var2.method_6128() && !var2.method_5765()) {
         if (var2.method_7344().method_75882() || var2.method_31549().field_7478) {
            PlayerInventory var3 = var2.method_31548();
            int var4 = bestSpear(var3, true);
            if (var4 < 0) {
               this.pending = false;
            } else {
               ItemStack var5 = var3.method_5438(var4);
               if (canJab(var1, var2, var5) && this.slots.take(var2, var4)) {
                  this.pending = false;
                  stab(var1, var2, (PiercingWeaponComponent)var5.method_58694(DataComponentTypes.field_63631));
               }
            }
         }
      }
   }

   private void tryPoke(MinecraftClient var1, ClientPlayerEntity var2) {
      if (this.canStab(var1, var2) && !var2.method_6047().method_57826(DataComponentTypes.field_63631)) {
         if (MeleeAttacks.crosshairHit(var1) == null) {
            PlayerInventory var3 = var2.method_31548();
            int var4 = bestSpear(var3, false);
            if (var4 >= 0) {
               ItemStack var5 = var3.method_5438(var4);
               if (canJab(var1, var2, var5) && enemyPastReach(var2) && this.slots.take(var2, var4)) {
                  stab(var1, var2, (PiercingWeaponComponent)var5.method_58694(DataComponentTypes.field_63631));
               }
            }
         }
      }
   }

   private static void stab(MinecraftClient var0, ClientPlayerEntity var1, PiercingWeaponComponent var2) {
      var0.field_1761.method_75407(var2);
      if (MeleeAttacks.actedThisTick(var1)) {
         var1.method_6104(Hand.field_5808);
         MeleeAttacks.claimTick(var0);
      }
   }

   private boolean canStab(MinecraftClient var1, ClientPlayerEntity var2) {
      return MeleeAttacks.canAct(var1) && MeleeAttacks.handsFree(var1, var2) && !MeleeAttacks.actedThisTick(var2)
         ? !CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()
         : false;
   }

   private static boolean canJab(MinecraftClient var0, ClientPlayerEntity var1, ItemStack var2) {
      return var0.field_1771 <= 0 && !var0.field_1761.method_2928() ? var2.method_45435(var0.field_1687.method_45162()) && !var1.method_75202(var2, 0) : false;
   }

   private static boolean enemyPastReach(ClientPlayerEntity var0) {
      Rotation var1 = ServerRotation.sent();
      Vec3d var2 = var1 != null ? Vec3d.method_1030(var1.pitch(), var1.yaw()) : var0.method_5828(1.0F);
      Vec3d var3 = var0.method_33571();
      Vec3d var4 = var3.method_1019(var2.method_1021(4.4));
      double var5 = var0.method_55755();
      Box var7 = var0.method_5829().method_18804(var2.method_1021(4.4)).method_1014(1.0);

      for (Entity var9 : var0.method_73183().method_8335(var0, var7)) {
         Optional var10;
         if (MeleeAttacks.isEnemy(var0, var9)
            && (var10 = var9.method_5829().method_1014(0.125).method_992(var3, var4)).isPresent()
            && var3.method_1022((Vec3d)var10.get()) > var5
            && MeleeAttacks.lineIsClear(var0, var3, (Vec3d)var10.get())) {
            return true;
         }
      }

      return false;
   }

   private static int bestSpear(PlayerInventory var0, boolean var1) {
      int var2 = var0.method_67532();
      int var3 = -1;
      int var4 = 0;

      for (int var5 = 0; var5 < PlayerInventory.method_7368(); var5++) {
         ItemStack var6 = var0.method_5438(var5);
         if (var6.method_57826(DataComponentTypes.field_63631) && !var6.method_63692()) {
            int var7 = lungeLevel(var6);
            boolean var8 = !var1 ? var3 < 0 || var7 < var4 : var7 > var4 || var7 > 0 && var7 == var4 && var5 == var2;
            if (var8) {
               var3 = var5;
               var4 = var7;
            }
         }
      }

      return var3;
   }

   private static int lungeLevel(ItemStack var0) {
      for (Entry var2 : var0.method_58657().method_57539()) {
         if (((RegistryEntry)var2.getKey()).method_40225(Enchantments.field_63420)) {
            return var2.getIntValue();
         }
      }

      return 0;
   }
}
