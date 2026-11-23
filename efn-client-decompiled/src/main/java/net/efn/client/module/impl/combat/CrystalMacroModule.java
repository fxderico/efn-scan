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
   public void migrateSettings(int var1) {
      if (var1 < 1) {
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
      MinecraftClient var1 = MinecraftClient.method_1551();
      this.endHold(var1);
      ClientPlayerEntity var2 = var1.field_1724;
      if (var2 != null && !hotbarTakenByOther()) {
         this.restoreSlot(var2);
      }

      this.resetState();
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      this.ageCounters();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 != null && var2.field_1687 != null && var2.field_1761 != null && !var3.method_29504()) {
         ItemStack var4 = var3.method_6047();
         if (var4 != this.lastMain) {
            this.lastMain = var4;
            this.mainAge = 0;
         }

         this.forgetGone(var2.field_1687);
         LootLatch.update(var2, var3);
         if (LootLatch.latched()) {
            this.endHold(var2);
            if (!var3.method_6115() && !AutoEatModule.isEating() && (this.ownsSelection(var3) || activationAllows(var3))) {
               suppressVanillaUse(var2);
            }
         } else if (!this.holdActive(var2, var3)) {
            this.endHold(var2);
            if (var2.field_1755 == null && !hotbarTakenByOther() && !MeleeAttacks.actedThisTick(var3)) {
               this.restoreSlot(var3);
            }
         } else if (!AutoInvTotemModule.wantsHotbar() && !CombatMacroLock.heldByOther(this)) {
            if (this.engaged) {
               if (!this.keepEngaged(var3)) {
                  this.endHold(var2);
                  return;
               }
            } else if (var3.method_6115() || !this.ownsSelection(var3) && !activationAllows(var3)) {
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
   public void onAttack(PlayerAttackEntityEvent var1) {
      Entity var3 = var1.target;
      EndCrystalEntity var2;
      if (var3 instanceof EndCrystalEntity && (var2 = (EndCrystalEntity)var3).method_73183().method_8608()) {
         this.markBreak(var2);
      }
   }

   private boolean holdActive(MinecraftClient var1, ClientPlayerEntity var2) {
      return MeleeAttacks.canAct(var1) && var1.method_1569() && !var2.method_7325() && Crystals.isPhysicallyDown(var1, var1.field_1690.field_1904);
   }

   private static boolean hotbarTakenByOther() {
      return AutoEatModule.isEating() || AutoInvTotemModule.wantsHotbar();
   }

   private boolean keepEngaged(ClientPlayerEntity var1) {
      if (this.ownsSelection(var1)) {
         return true;
      } else {
         this.forgetSlots();
         return activationAllows(var1);
      }
   }

   private boolean ownsSelection(ClientPlayerEntity var1) {
      return this.ownSlot != -1 && var1.method_31548().method_67532() == this.ownSlot;
   }

   private static boolean activationAllows(ClientPlayerEntity var0) {
      ItemStack var1 = var0.method_6047();
      ItemStack var2 = var0.method_6079();
      return !var1.method_31574(Items.field_8301) && (!var2.method_31574(Items.field_8301) || !Crystals.isCombatItem(var1))
         ? (var1.method_31573(ItemTags.field_42611) || var1.method_31573(ItemTags.field_42612) || var1.method_31574(Items.field_49814))
            && (var2.method_7960() || var2.method_31574(Items.field_8288))
            && Crystals.hotbarSlotOf(var0.method_31548(), Items.field_8301) != -1
         : true;
   }

   private void endHold(MinecraftClient var1) {
      CombatMacroLock.release(this);
      if (this.engaged) {
         this.engaged = false;
         var1.field_1690.field_1904.method_23481(var1.field_1755 == null && Crystals.isPhysicallyDown(var1, var1.field_1690.field_1904));
      }
   }

   private void restoreSlot(ClientPlayerEntity var1) {
      if (this.ownSlot != -1) {
         PlayerInventory var2 = var1.method_31548();
         if (this.switchBack.get() && PlayerInventory.method_7380(this.savedSlot) && this.savedSlot != this.ownSlot && var2.method_67532() == this.ownSlot) {
            var2.method_61496(this.savedSlot);
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

   private static void suppressVanillaUse(MinecraftClient var0) {
      KeyBinding var1 = var0.field_1690.field_1904;
      var1.method_23481(false);

      while (var1.method_1436()) {
      }
   }

   private CrystalMacroModule.Plan plan(MinecraftClient var1, ClientPlayerEntity var2) {
      HitResult var4 = var2.method_76762(1.0F, var2);
      int var5 = Crystals.confirmWindow(var1, var2);
      if (this.pendingCrystalBase != null && Crystals.hasCrystal(var1.field_1687, this.pendingCrystalBase, null)) {
         this.pendingCrystalBase = null;
      }

      if (var4 instanceof EntityHitResult var12) {
         Entity var13 = var12.method_17782();
         EndCrystalEntity var11;
         if (!(var13 instanceof EndCrystalEntity) || !(var11 = (EndCrystalEntity)var13).method_5805()) {
            return null;
         } else if (var11.method_5628() == this.pendingBreakId && this.pendingBreakAge < var5) {
            return null;
         } else if (this.restUntil.get(var11.method_5628()) > var2.field_6012) {
            return null;
         } else {
            float var15 = CrystalDamage.estimate(var11.method_73189(), var2);
            if (!(var15 > this.maxSelfDamage.get()) && !CrystalDamage.endangers(var2, var15)) {
               int var16 = Crystals.weaponRoute(var2);
               return var16 == -1 ? null : CrystalMacroModule.Plan.forBreak(var11, var16);
            } else {
               return null;
            }
         }
      } else {
         BlockHitResult var3;
         if (var4 instanceof BlockHitResult && (var3 = (BlockHitResult)var4).method_17783() == Type.field_1332) {
            ClientWorld var6 = var1.field_1687;
            BlockPos var7 = var3.method_17777();
            if (!var6.method_8621().method_11952(var7)) {
               return null;
            } else {
               BlockState var8 = var6.method_8320(var7);
               if (Crystals.isBase(var8)) {
                  if (var7.equals(this.pendingCrystalBase) && this.pendingCrystalAge < var5) {
                     return null;
                  } else if (!Crystals.canHost(var6, var7, null)) {
                     return null;
                  } else {
                     int var14 = Crystals.route(var2, Items.field_8301, var8, this.savedSlot);
                     return var14 == -1 ? null : CrystalMacroModule.Plan.forPlace(CrystalMacroModule.Step.CRYSTAL, var3, var7, var14);
                  }
               } else if (!this.autoObsidian.get()) {
                  return null;
               } else if (Crystals.route(var2, Items.field_8301, Blocks.field_10540.method_9564(), this.savedSlot) == -1) {
                  return null;
               } else {
                  int var9 = Crystals.route(var2, Items.field_8281, var8, this.savedSlot);
                  if (var9 == -1) {
                     return null;
                  } else {
                     BlockPos var10 = Crystals.obsidianTarget(var6, var2, var3, var8);
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

   private void breakCrystal(MinecraftClient var1, ClientPlayerEntity var2, CrystalMacroModule.Plan var3) {
      if (this.due(this.breakDelay)) {
         if (var3.route() != -2) {
            this.select(var2, var3.route());
         }

         if (this.mainAge >= 2 || Crystals.enchantsAddDamage(var2.method_6047()) || Crystals.dealsDamage(var2, ItemStack.field_8037)) {
            if (Crystals.strike(var1, var2, var3.crystal())) {
               MeleeAttacks.claimTick(var1);
               this.markBreak(var3.crystal());
               int var4 = var3.crystal().method_5628();
               if (this.breakAttempts.addTo(var4, 1) + 1 >= 3) {
                  this.breakAttempts.remove(var4);
                  this.restUntil.put(var4, var2.field_6012 + 100);
               }

               this.acted();
            }
         }
      }
   }

   private boolean useOnCrosshair(MinecraftClient var1, ClientPlayerEntity var2, NumberSetting var3, CrystalMacroModule.Plan var4) {
      if (!this.due(var3)) {
         return false;
      } else {
         if (var4.route() != -2) {
            this.select(var2, var4.route());
         }

         if (!Crystals.rightClick(var1, var2, var4.blockHit())) {
            return false;
         } else {
            MeleeAttacks.claimTick(var1);
            this.acted();
            return true;
         }
      }
   }

   private boolean due(NumberSetting var1) {
      return this.ticksSinceAction > Math.max(var1.getInt(), this.actionGap - 1);
   }

   private void acted() {
      this.ticksSinceAction = 0;
      this.actionGap = this.humanize.get() ? Crystals.humanGap() : 0;
   }

   private void forgetGone(ClientWorld var1) {
      this.breakAttempts.keySet().removeIf(var1x -> var1.method_8469(var1x) == null);
      this.restUntil.keySet().removeIf(var1x -> var1.method_8469(var1x) == null);
   }

   private void select(ClientPlayerEntity var1, int var2) {
      PlayerInventory var3 = var1.method_31548();
      if (this.savedSlot == -1) {
         this.savedSlot = var3.method_67532();
      }

      var3.method_61496(var2);
      this.ownSlot = var2;
      this.lastMain = var1.method_6047();
      this.mainAge = 0;
   }

   private void markBreak(EndCrystalEntity var1) {
      this.pendingBreakId = var1.method_5628();
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
      static CrystalMacroModule.Plan forBreak(EndCrystalEntity var0, int var1) {
         return new CrystalMacroModule.Plan(CrystalMacroModule.Step.BREAK, var0, null, null, var1);
      }

      static CrystalMacroModule.Plan forPlace(CrystalMacroModule.Step var0, BlockHitResult var1, BlockPos var2, int var3) {
         return new CrystalMacroModule.Plan(var0, null, var1, var2, var3);
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
