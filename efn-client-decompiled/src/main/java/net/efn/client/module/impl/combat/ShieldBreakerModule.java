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
      this.slots.drop(MinecraftClient.getInstance(), this.switchBack.get());
   }

   public void beforeAttack(ClientPlayerEntity clientPlayerEntity, Entity entity) {
      if (entity instanceof PlayerEntity var3 && !MeleeAttacks.disablesShields(clientPlayerEntity.getMainHandStack())) {
         if (fightingHand(clientPlayerEntity)) {
            if (!MeleeAttacks.shieldBroken(var3) && blocksSoon(clientPlayerEntity, var3)) {
               int var4 = bestAxe(clientPlayerEntity);
               if (var4 >= 0 && this.slots.take(clientPlayerEntity, var4)) {
                  this.takenTick = this.ticks;
                  MeleeAttacks.noteShieldBroken(var3);
               }
            }
         }
      }
   }

   private static boolean fightingHand(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var1 = clientPlayerEntity.getMainHandStack();
      return var1.isEmpty() || MeleeAttacks.isMeleeWeapon(var1);
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onTick(TickEvent tickEvent) {
      this.ticks++;
      if (this.ticks - this.takenTick >= 2) {
         this.slots.tick(MinecraftClient.getInstance(), this.switchBack.get());
      }
   }

   @EventHandler
   public void onAutoTick(TickEvent tickEvent) {
      if (this.mode.is("Auto")) {
         this.tryHit(MinecraftClient.getInstance());
      }
   }

   private void tryHit(MinecraftClient minecraftClient) {
      if (this.ticks >= this.rearmTick && MeleeAttacks.canAct(minecraftClient)) {
         if (!CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()) {
            ClientPlayerEntity var4 = minecraftClient.player;
            if (!MeleeAttacks.actedThisTick(var4) && !ServerRotation.pending() && !MeleeAttacks.clickPending(minecraftClient)) {
               EntityHitResult var5 = MeleeAttacks.crosshairHit(minecraftClient);
               PlayerEntity var2;
               Entity var3;
               if (var5 != null && (var3 = var5.getEntity()) instanceof PlayerEntity && MeleeAttacks.isEnemy(var4, var2 = (PlayerEntity)var3)) {
                  if (!MeleeAttacks.shieldBroken(var2) && blocksSoon(var4, var2)) {
                     if (MeleeAttacks.disablesShields(var4.getMainHandStack()) || bestAxe(var4) >= 0 && fightingHand(var4)) {
                        if (MeleeAttacks.attack(minecraftClient, var5)) {
                           this.rearmTick = this.ticks + 10;
                           MeleeAttacks.claimTick(minecraftClient);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean blocksSoon(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      if (!playerEntity.isUsingItem()) {
         return false;
      } else {
         ItemStack var2 = playerEntity.getActiveItem();
         BlocksAttacksComponent var3 = var2.get(DataComponentTypes.BLOCKS_ATTACKS);
         return var3 != null && playerEntity.getItemUseTime() + leadTicks(clientPlayerEntity) >= var3.getBlockDelayTicks()
            ? MeleeAttacks.shieldBlocks(playerEntity, clientPlayerEntity, var2)
            : false;
      }
   }

   private static int leadTicks(ClientPlayerEntity clientPlayerEntity) {
      PlayerListEntry var1 = clientPlayerEntity.networkHandler.getPlayerListEntry(clientPlayerEntity.getUuid());
      return var1 == null ? 0 : MathHelper.clamp(Math.round(var1.getLatency() / 50.0F), 0, 4);
   }

   static int bestAxe(ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var1 = clientPlayerEntity.getInventory();
      int var2 = -1;
      double var3 = -1.0 / 0.0;

      for (int var5 = 0; var5 < PlayerInventory.getHotbarSize(); var5++) {
         ItemStack var8 = var1.getStack(var5);
         double var6;
         if (MeleeAttacks.disablesShields(var8) && !var8.willBreakNextUse() && (var6 = MeleeAttacks.attackDamage(var8)) > var3) {
            var2 = var5;
            var3 = var6;
         }
      }

      return var2;
   }
}
