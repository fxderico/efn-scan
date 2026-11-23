package net.efn.client.module.impl.combat;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.crystal.LootLatch;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.misc.AutoEatModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class CrystalMacroModule extends ClientModule {
   private static final int COUNTER_CAP = 1000;
   private static final int WEAPON_SETTLE_TICKS = 2;
   private static final int MAX_BREAK_ATTEMPTS = 3;
   private static final int GHOST_REST_TICKS = 100;
   private final NumberSetting breakDelay = this.addSetting("break-delay", new NumberSetting("Break Delay", 0.0F, 0.0F, 10.0F).step(1.0F));
   private final NumberSetting placeDelay = this.addSetting("place-delay", new NumberSetting("Place Delay", 0.0F, 0.0F, 10.0F).step(1.0F));
   private final BooleanSetting autoObsidian = this.addSetting("auto-obsidian", new BooleanSetting("Auto Obsidian", true));
   private final NumberSetting obsidianDelay = this.addSetting("obsidian-delay", new NumberSetting("Obsidian Delay", 2.0F, 0.0F, 10.0F).step(1.0F));
   private final BooleanSetting switchBack = this.addSetting("switch-back", new BooleanSetting("Switch Back", true));
   private final NumberSetting maxSelfDamage = this.addSetting("max-self-damage", new NumberSetting("Max Self Damage", 8.0F, 0.0F, 20.0F).step(0.5F));
   private final BooleanSetting humanize = this.addSetting("humanize", new BooleanSetting("Human Pace", true));
   private boolean engaged;
   private int savedSlot = -1;
   private int ownSlot = -1;
   private int ticksSinceAction = 1000;
   private int actionGap;
   private final Int2IntOpenHashMap breakAttempts = new Int2IntOpenHashMap();
   private final Int2IntOpenHashMap restUntil = new Int2IntOpenHashMap();
   private ItemStack lastMain;
   private int mainAge = 1000;
   private int pendingBreakId = -1;
   private int pendingBreakAge = 1000;
   private BlockPos pendingCrystalBase;
   private int pendingCrystalAge = 1000;
   private BlockPos pendingObsidian;
   private int pendingObsidianAge = 1000;

   public CrystalMacroModule() {
      super("crystal-macro", "Crystal Macro", "Breaks and places crystals while you hold right-click.", ModuleCategory.COMBAT);
      this.obsidianDelay.visibleWhen(this.autoObsidian::get);
   }

   @Override
   public int settingsVersion() {
      return 1;
   }

   @Override
   public void migrateSettings(int i) {
      if (i < 1) {
         this.breakDelay.set(this.breakDelay.get() - 1.0F);
         this.placeDelay.set(this.placeDelay.get() - 1.0F);
         this.obsidianDelay.set(this.obsidianDelay.get() - 1.0F);
      }
   }

   @Override
   protected void onEnable() {
      this.resetState();
   }

   @Override
   protected void onDisable() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      this.endHold(var1);
      ClientPlayerEntity var2 = var1.player;
      if (var2 != null && !hotbarTakenByOther()) {
         this.restoreSlot(var2);
      }

      this.resetState();
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      this.ageCounters();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null && var2.world != null && var2.interactionManager != null && !var3.isDead()) {
         ItemStack var4 = var3.getMainHandStack();
         if (var4 != this.lastMain) {
            this.lastMain = var4;
            this.mainAge = 0;
         }

         this.forgetGone(var2.world);
         LootLatch.update(var2, var3);
         if (LootLatch.latched()) {
            this.endHold(var2);
            if (!var3.isUsingItem() && !AutoEatModule.isEating() && (this.ownsSelection(var3) || activationAllows(var3))) {
               suppressVanillaUse(var2);
            }
         } else if (!this.holdActive(var2, var3)) {
            this.endHold(var2);
            if (var2.currentScreen == null && !hotbarTakenByOther() && !MeleeAttacks.actedThisTick(var3)) {
               this.restoreSlot(var3);
            }
         } else if (!AutoInvTotemModule.wantsHotbar() && !CombatMacroLock.heldByOther(this)) {
            if (this.engaged) {
               if (!this.keepEngaged(var3)) {
                  this.endHold(var2);
                  return;
               }
            } else if (var3.isUsingItem() || !this.ownsSelection(var3) && !activationAllows(var3)) {
               return;
            }

            CrystalMacroModule.Plan var5 = this.plan(var2, var3);
            if (this.engaged || var5 != null || this.ownsSelection(var3)) {
               this.engaged = true;
               CombatMacroLock.tryAcquire(this);
               suppressVanillaUse(var2);
               if (var5 != null
                  && MeleeAttacks.handsFree(var2, var3)
                  && !MeleeAttacks.actedThisTick(var3)
                  && !ServerRotation.pending()
                  && !ServerRotation.desynced()) {
                  switch (var5.step()) {
                     case BREAK:
                        this.breakCrystal(var2, var3, var5);
                        break;
                     case CRYSTAL:
                        if (this.useOnCrosshair(var2, var3, this.placeDelay, var5)) {
                           this.pendingCrystalBase = var5.target();
                           this.pendingCrystalAge = 0;
                        }
                        break;
                     case OBSIDIAN:
                        if (this.useOnCrosshair(var2, var3, this.obsidianDelay, var5)) {
                           this.pendingObsidian = var5.target();
                           this.pendingObsidianAge = 0;
                        }
                  }
               }
            }
         } else {
            CombatMacroLock.release(this);
            if (this.engaged) {
               suppressVanillaUse(var2);
            }
         }
      } else {
         this.endHold(var2);
         this.forgetSlots();
      }
   }

   @EventHandler
   public void onAttack(PlayerAttackEntityEvent playerAttackEntityEvent) {
      Entity var3 = playerAttackEntityEvent.target;
      EndCrystalEntity var2;
      if (var3 instanceof EndCrystalEntity && (var2 = (EndCrystalEntity)var3).getEntityWorld().isClient()) {
         this.markBreak(var2);
      }
   }

   private boolean holdActive(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      return MeleeAttacks.canAct(minecraftClient)
         && minecraftClient.isWindowFocused()
         && !clientPlayerEntity.isSpectator()
         && Crystals.isPhysicallyDown(minecraftClient, minecraftClient.options.useKey);
   }

   private static boolean hotbarTakenByOther() {
      return AutoEatModule.isEating() || AutoInvTotemModule.wantsHotbar();
   }

   private boolean keepEngaged(ClientPlayerEntity clientPlayerEntity) {
      if (this.ownsSelection(clientPlayerEntity)) {
         return true;
      } else {
         this.forgetSlots();
         return activationAllows(clientPlayerEntity);
      }
   }

   private boolean ownsSelection(ClientPlayerEntity clientPlayerEntity) {
      return this.ownSlot != -1 && clientPlayerEntity.getInventory().getSelectedSlot() == this.ownSlot;
   }

   private static boolean activationAllows(ClientPlayerEntity clientPlayerEntity) {
      ItemStack var1 = clientPlayerEntity.getMainHandStack();
      ItemStack var2 = clientPlayerEntity.getOffHandStack();
      return !var1.isOf(Items.END_CRYSTAL) && (!var2.isOf(Items.END_CRYSTAL) || !Crystals.isCombatItem(var1))
         ? (var1.isIn(ItemTags.SWORDS) || var1.isIn(ItemTags.AXES) || var1.isOf(Items.MACE))
            && (var2.isEmpty() || var2.isOf(Items.TOTEM_OF_UNDYING))
            && Crystals.hotbarSlotOf(clientPlayerEntity.getInventory(), Items.END_CRYSTAL) != -1
         : true;
   }

   private void endHold(MinecraftClient minecraftClient) {
      CombatMacroLock.release(this);
      if (this.engaged) {
         this.engaged = false;
         minecraftClient.options
            .useKey
            .setPressed(minecraftClient.currentScreen == null && Crystals.isPhysicallyDown(minecraftClient, minecraftClient.options.useKey));
      }
   }

   private void restoreSlot(ClientPlayerEntity clientPlayerEntity) {
      if (this.ownSlot != -1) {
         PlayerInventory var2 = clientPlayerEntity.getInventory();
         if (this.switchBack.get()
            && PlayerInventory.isValidHotbarIndex(this.savedSlot)
            && this.savedSlot != this.ownSlot
            && var2.getSelectedSlot() == this.ownSlot) {
            var2.setSelectedSlot(this.savedSlot);
         }

         this.forgetSlots();
      }
   }

   private void forgetSlots() {
      this.savedSlot = -1;
      this.ownSlot = -1;
   }

   private void resetState() {
      this.engaged = false;
      this.forgetSlots();
      this.ticksSinceAction = 1000;
      this.actionGap = 0;
      this.breakAttempts.clear();
      this.restUntil.clear();
      this.lastMain = null;
      this.mainAge = 1000;
      this.pendingBreakId = -1;
      this.pendingBreakAge = 1000;
      this.pendingCrystalBase = null;
      this.pendingCrystalAge = 1000;
      this.pendingObsidian = null;
      this.pendingObsidianAge = 1000;
      CombatMacroLock.release(this);
   }

   private static void suppressVanillaUse(MinecraftClient minecraftClient) {
      KeyBinding var1 = minecraftClient.options.useKey;
      var1.setPressed(false);

      while (var1.wasPressed()) {
      }
   }

   private CrystalMacroModule.Plan plan(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      HitResult var4 = clientPlayerEntity.getCrosshairTarget(1.0F, clientPlayerEntity);
      int var5 = Crystals.confirmWindow(minecraftClient, clientPlayerEntity);
      if (this.pendingCrystalBase != null && Crystals.hasCrystal(minecraftClient.world, this.pendingCrystalBase, null)) {
         this.pendingCrystalBase = null;
      }

      if (var4 instanceof EntityHitResult var12) {
         Entity var13 = var12.getEntity();
         EndCrystalEntity var11;
         if (!(var13 instanceof EndCrystalEntity) || !(var11 = (EndCrystalEntity)var13).isAlive()) {
            return null;
         } else if (var11.getId() == this.pendingBreakId && this.pendingBreakAge < var5) {
            return null;
         } else if (this.restUntil.get(var11.getId()) > clientPlayerEntity.age) {
            return null;
         } else {
            float var15 = CrystalDamage.estimate(var11.getEntityPos(), clientPlayerEntity);
            if (!(var15 > this.maxSelfDamage.get()) && !CrystalDamage.endangers(clientPlayerEntity, var15)) {
               int var16 = Crystals.weaponRoute(clientPlayerEntity);
               return var16 == -1 ? null : CrystalMacroModule.Plan.forBreak(var11, var16);
            } else {
               return null;
            }
         }
      } else {
         BlockHitResult var3;
         if (var4 instanceof BlockHitResult && (var3 = (BlockHitResult)var4).getType() == Type.BLOCK) {
            ClientWorld var6 = minecraftClient.world;
            BlockPos var7 = var3.getBlockPos();
            if (!var6.getWorldBorder().contains(var7)) {
               return null;
            } else {
               BlockState var8 = var6.getBlockState(var7);
               if (Crystals.isBase(var8)) {
                  if (var7.equals(this.pendingCrystalBase) && this.pendingCrystalAge < var5) {
                     return null;
                  } else if (!Crystals.canHost(var6, var7, null)) {
                     return null;
                  } else {
                     int var14 = Crystals.route(clientPlayerEntity, Items.END_CRYSTAL, var8, this.savedSlot);
                     return var14 == -1 ? null : CrystalMacroModule.Plan.forPlace(CrystalMacroModule.Step.CRYSTAL, var3, var7, var14);
                  }
               } else if (!this.autoObsidian.get()) {
                  return null;
               } else if (Crystals.route(clientPlayerEntity, Items.END_CRYSTAL, Blocks.OBSIDIAN.getDefaultState(), this.savedSlot) == -1) {
                  return null;
               } else {
                  int var9 = Crystals.route(clientPlayerEntity, Items.OBSIDIAN, var8, this.savedSlot);
                  if (var9 == -1) {
                     return null;
                  } else {
                     BlockPos var10 = Crystals.obsidianTarget(var6, clientPlayerEntity, var3, var8);
                     return var10 != null && (!var10.equals(this.pendingObsidian) || this.pendingObsidianAge >= var5)
                        ? CrystalMacroModule.Plan.forPlace(CrystalMacroModule.Step.OBSIDIAN, var3, var10, var9)
                        : null;
                  }
               }
            }
         } else {
            return null;
         }
      }
   }

   private void breakCrystal(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, CrystalMacroModule.Plan plan) {
      if (this.due(this.breakDelay)) {
         if (plan.route() != -2) {
            this.select(clientPlayerEntity, plan.route());
         }

         if (this.mainAge >= 2
            || Crystals.enchantsAddDamage(clientPlayerEntity.getMainHandStack())
            || Crystals.dealsDamage(clientPlayerEntity, ItemStack.EMPTY)) {
            if (Crystals.strike(minecraftClient, clientPlayerEntity, plan.crystal())) {
               MeleeAttacks.claimTick(minecraftClient);
               this.markBreak(plan.crystal());
               int var4 = plan.crystal().getId();
               if (this.breakAttempts.addTo(var4, 1) + 1 >= 3) {
                  this.breakAttempts.remove(var4);
                  this.restUntil.put(var4, clientPlayerEntity.age + 100);
               }

               this.acted();
            }
         }
      }
   }

   private boolean useOnCrosshair(
      MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, NumberSetting numberSetting, CrystalMacroModule.Plan plan
   ) {
      if (!this.due(numberSetting)) {
         return false;
      } else {
         if (plan.route() != -2) {
            this.select(clientPlayerEntity, plan.route());
         }

         if (!Crystals.rightClick(minecraftClient, clientPlayerEntity, plan.blockHit())) {
            return false;
         } else {
            MeleeAttacks.claimTick(minecraftClient);
            this.acted();
            return true;
         }
      }
   }

   private boolean due(NumberSetting numberSetting) {
      return this.ticksSinceAction > Math.max(numberSetting.getInt(), this.actionGap - 1);
   }

   private void acted() {
      this.ticksSinceAction = 0;
      this.actionGap = this.humanize.get() ? Crystals.humanGap() : 0;
   }

   private void forgetGone(ClientWorld clientWorld) {
      this.breakAttempts.keySet().removeIf(i -> clientWorld.getEntityById(i) == null);
      this.restUntil.keySet().removeIf(i -> clientWorld.getEntityById(i) == null);
   }

   private void select(ClientPlayerEntity clientPlayerEntity, int i) {
      PlayerInventory var3 = clientPlayerEntity.getInventory();
      if (this.savedSlot == -1) {
         this.savedSlot = var3.getSelectedSlot();
      }

      var3.setSelectedSlot(i);
      this.ownSlot = i;
      this.lastMain = clientPlayerEntity.getMainHandStack();
      this.mainAge = 0;
   }

   private void markBreak(EndCrystalEntity endCrystalEntity) {
      this.pendingBreakId = endCrystalEntity.getId();
      this.pendingBreakAge = 0;
   }

   private void ageCounters() {
      this.ticksSinceAction = Math.min(this.ticksSinceAction + 1, 1000);
      this.mainAge = Math.min(this.mainAge + 1, 1000);
      this.pendingBreakAge = Math.min(this.pendingBreakAge + 1, 1000);
      this.pendingCrystalAge = Math.min(this.pendingCrystalAge + 1, 1000);
      this.pendingObsidianAge = Math.min(this.pendingObsidianAge + 1, 1000);
   }

   @Environment(EnvType.CLIENT)
   private record Plan(CrystalMacroModule.Step step, EndCrystalEntity crystal, BlockHitResult blockHit, BlockPos target, int route) {
      static CrystalMacroModule.Plan forBreak(EndCrystalEntity endCrystalEntity, int i) {
         return new CrystalMacroModule.Plan(CrystalMacroModule.Step.BREAK, endCrystalEntity, null, null, i);
      }

      static CrystalMacroModule.Plan forPlace(CrystalMacroModule.Step step, BlockHitResult blockHitResult, BlockPos blockPos, int i) {
         return new CrystalMacroModule.Plan(step, null, blockHitResult, blockPos, i);
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Step {
      BREAK,
      CRYSTAL,
      OBSIDIAN;

      private Step() {
      }
   }
}
