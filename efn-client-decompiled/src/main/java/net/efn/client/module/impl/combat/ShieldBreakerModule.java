package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class ShieldBreakerModule extends ClientModule {
   private static final String MODE_PASSIVE = "Passive";
   private static final String MODE_AUTO = "Auto";
   private static final int REARM_TICKS = 10;
   private static final int MAX_LEAD_TICKS = 4;
   private static final int HOLD_TICKS = 2;
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Passive", "Passive", "Auto"));
   private final BooleanSetting switchBack = this.addSetting("switch-back", new BooleanSetting("Switch Back", true));
   private final SlotReturn slots = new SlotReturn(this);
   private int ticks;
   private int rearmTick;
   private int takenTick;

   public ShieldBreakerModule() {
      super("shield-breaker", "Shield Breaker", "Swaps to an axe for any hit into a raised shield. Auto also makes the hit.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.ticks = 0;
      this.rearmTick = 0;
   }

   @Override
   protected void onDisable() {
      this.slots.drop(MinecraftClient.method_1551(), this.switchBack.get());
   }

   public void beforeAttack(ClientPlayerEntity var1, Entity var2) {
      if (var2 instanceof PlayerEntity var3 && !MeleeAttacks.disablesShields(var1.method_6047())) {
         if (fightingHand(var1)) {
            if (!MeleeAttacks.shieldBroken(var3) && blocksSoon(var1, var3)) {
               int var4 = bestAxe(var1);
               if (var4 >= 0 && this.slots.take(var1, var4)) {
                  this.takenTick = this.ticks;
                  MeleeAttacks.noteShieldBroken(var3);
               }
            }
         }
      }
   }

   private static boolean fightingHand(ClientPlayerEntity var0) {
      ItemStack var1 = var0.method_6047();
      return var1.method_7960() || MeleeAttacks.isMeleeWeapon(var1);
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onTick(TickEvent var1) {
      this.ticks++;
      if (this.ticks - this.takenTick >= 2) {
         this.slots.tick(MinecraftClient.method_1551(), this.switchBack.get());
      }
   }

   @EventHandler
   public void onAutoTick(TickEvent var1) {
      if (this.mode.is("Auto")) {
         this.tryHit(MinecraftClient.method_1551());
      }
   }

   private void tryHit(MinecraftClient var1) {
      if (this.ticks >= this.rearmTick && MeleeAttacks.canAct(var1)) {
         if (!CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()) {
            ClientPlayerEntity var4 = var1.field_1724;
            if (!MeleeAttacks.actedThisTick(var4) && !ServerRotation.pending() && !MeleeAttacks.clickPending(var1)) {
               EntityHitResult var5 = MeleeAttacks.crosshairHit(var1);
               PlayerEntity var2;
               Entity var3;
               if (var5 != null && (var3 = var5.method_17782()) instanceof PlayerEntity && MeleeAttacks.isEnemy(var4, var2 = (PlayerEntity)var3)) {
                  if (!MeleeAttacks.shieldBroken(var2) && blocksSoon(var4, var2)) {
                     if (MeleeAttacks.disablesShields(var4.method_6047()) || bestAxe(var4) >= 0 && fightingHand(var4)) {
                        if (MeleeAttacks.attack(var1, var5)) {
                           this.rearmTick = this.ticks + 10;
                           MeleeAttacks.claimTick(var1);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean blocksSoon(ClientPlayerEntity var0, PlayerEntity var1) {
      if (!var1.method_6115()) {
         return false;
      } else {
         ItemStack var2 = var1.method_6030();
         BlocksAttacksComponent var3 = (BlocksAttacksComponent)var2.method_58694(DataComponentTypes.field_56396);
         return var3 != null && var1.method_6048() + leadTicks(var0) >= var3.method_67197() ? MeleeAttacks.shieldBlocks(var1, var0, var2) : false;
      }
   }

   private static int leadTicks(ClientPlayerEntity var0) {
      PlayerListEntry var1 = var0.field_3944.method_2871(var0.method_5667());
      return var1 == null ? 0 : MathHelper.method_15340(Math.round(var1.method_2959() / 50.0F), 0, 4);
   }

   static int bestAxe(ClientPlayerEntity var0) {
      PlayerInventory var1 = var0.method_31548();
      int var2 = -1;
      double var3 = -1.0 / 0.0;

      for (int var5 = 0; var5 < PlayerInventory.method_7368(); var5++) {
         ItemStack var8 = var1.method_5438(var5);
         double var6;
         if (MeleeAttacks.disablesShields(var8) && !var8.method_63692() && (var6 = MeleeAttacks.attackDamage(var8)) > var3) {
            var2 = var5;
            var3 = var6;
         }
      }

      return var2;
   }
}
