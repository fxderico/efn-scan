package net.efn.client.module.impl.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Optional;
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
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class MaceModule extends ClientModule {
   private static final String PICK_DAMAGE = "Damage";
   private static final String PICK_WIND_BURST = "Wind Burst";
   private static final String SMASH_DIRECT = "Direct";
   private static final String SMASH_DELAYED = "Delayed";
   private static final double REACH_MARGIN = 0.35;
   private static final float BREACH_GAIN = 1.15F;
   private final BooleanSetting autoSwap = this.addSetting("auto-swap", new BooleanSetting("Auto Swap", true));
   private final ModeSetting pick = this.addSetting("pick", new ModeSetting("Pick", "Damage", "Damage", "Wind Burst"));
   private final BooleanSetting breachSwap = this.addSetting("breach-swap", new BooleanSetting("Breach Swap", false));
   private final BooleanSetting swapBack = this.addSetting("swap-back", new BooleanSetting("Swap Back", true));
   private final BooleanSetting smashAssist = this.addSetting("smash-assist", new BooleanSetting("Smash Assist", false));
   private final ModeSetting smashTiming = this.addSetting("smash-timing", new ModeSetting("Smash Timing", "Delayed", "Direct", "Delayed"));
   private final NumberSetting minFall = this.addSetting("min-fall", new NumberSetting("Min Fall", 3.0F, 1.5F, 30.0F).step(0.5F));
   private final BooleanSetting mobs = this.addSetting("mobs", new BooleanSetting("Mobs", false));
   private final SlotReturn slots = new SlotReturn(this);
   private boolean smashed;

   public MaceModule() {
      super("mace", "Mace", "Swaps to the strongest mace for smash hits, with an optional smash assist.", ModuleCategory.COMBAT);
      this.pick.visibleWhen(this.autoSwap::get);
      this.breachSwap.visibleWhen(this.autoSwap::get);
      this.swapBack.visibleWhen(() -> this.autoSwap.get() || this.smashAssist.get());
      this.smashTiming.visibleWhen(this.smashAssist::get);
      this.minFall.visibleWhen(this.smashAssist::get);
      this.mobs.visibleWhen(this.smashAssist::get);
   }

   @Override
   protected void onDisable() {
      this.smashed = false;
      this.slots.drop(MinecraftClient.method_1551(), this.swapBack.get());
   }

   public void beforeAttack(ClientPlayerEntity var1, Entity var2) {
      if (this.autoSwap.get() && var2 instanceof LivingEntity var3) {
         if (!MeleeAttacks.shieldBlocks(var3, var1) || MeleeAttacks.shieldBroken(var3)) {
            boolean var4 = MaceItem.method_58659(var1);
            if (var4 || this.breachSwap.get()) {
               int var5 = this.bestMace(var1, var3, var4);
               if (var5 >= 0) {
                  if (var4 || !(expected(var1, var1.method_31548().method_5438(var5), var3) < expected(var1, var1.method_6047(), var3) * 1.15F)) {
                     this.slots.take(var1, var5);
                  }
               }
            }
         }
      }
   }

   private int bestMace(ClientPlayerEntity var1, LivingEntity var2, boolean var3) {
      boolean var4 = var3 && this.pick.is("Wind Burst");
      PlayerInventory var5 = var1.method_31548();
      int var6 = -1;
      boolean var7 = false;
      float var8 = 0.0F;

      for (int var9 = 0; var9 < PlayerInventory.method_7368(); var9++) {
         ItemStack var10 = var5.method_5438(var9);
         if (var10.method_31574(Items.field_49814) && !var10.method_63692() && (var3 || level(var10, Enchantments.field_50158) != 0)) {
            boolean var11 = var4 && level(var10, Enchantments.field_50159) > 0;
            float var12 = expected(var1, var10, var2);
            if (var6 < 0 || (!var7 || var11) && (var7 != var11 || !(var12 <= var8))) {
               var6 = var9;
               var7 = var11;
               var8 = var12;
            }
         }
      }

      return var6;
   }

   private static float expected(ClientPlayerEntity var0, ItemStack var1, LivingEntity var2) {
      float var4 = MeleeAttacks.charge(var0);
      double var5 = var0.field_6017;
      float var7 = (float)(1.0 + MeleeAttacks.attackDamage(var0.method_6047())) * (0.2F + 0.8F * var4 * var4);
      if (var1.method_31574(Items.field_49814) && MaceItem.method_58659(var0)) {
         var7 += (float)(smashBonus(var5) + 0.5 * level(var1, Enchantments.field_50157) * var5);
      }

      if (MeleeAttacks.wouldCrit(var0, var2)) {
         var7 *= 1.5F;
      }

      int var3;
      if ((var3 = level(var1, Enchantments.field_9118)) > 0) {
         var7 += var4 * (0.5F * var3 + 0.5F);
      }

      return afterArmor(var2, var7, level(var1, Enchantments.field_50158));
   }

   private static double smashBonus(double var0) {
      if (var0 <= 3.0) {
         return 4.0 * var0;
      } else {
         return var0 <= 8.0 ? 12.0 + 2.0 * (var0 - 3.0) : 22.0 + var0 - 8.0;
      }
   }

   private static float afterArmor(LivingEntity var0, float var1, int var2) {
      float var3 = var0.method_6096();
      float var4 = (float)var0.method_45325(EntityAttributes.field_23725);
      float var5 = MathHelper.method_15363(var3 - var1 / (2.0F + var4 / 4.0F), var3 * 0.2F, 20.0F) / 25.0F;
      return var1 * (1.0F - MathHelper.method_15363(var5 - 0.15F * var2, 0.0F, 1.0F));
   }

   private static int level(ItemStack var0, RegistryKey<Enchantment> var1) {
      for (Entry var3 : var0.method_58657().method_57539()) {
         if (((RegistryEntry)var3.getKey()).method_40225(var1)) {
            return var3.getIntValue();
         }
      }

      return 0;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 != null && var2.field_1724.method_24828()) {
         this.smashed = false;
      }

      this.slots.tick(var2, this.swapBack.get());
   }

   @EventHandler
   public void onSmashTick(TickEvent var1) {
      if (this.smashAssist.get()) {
         this.trySmash(MinecraftClient.method_1551());
      }
   }

   private void trySmash(MinecraftClient var1) {
      if (MeleeAttacks.canAct(var1) && !CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()) {
         ClientPlayerEntity var4 = var1.field_1724;
         if (!MeleeAttacks.actedThisTick(var4)
            && !MeleeAttacks.clickPending(var1)
            && !MeleeAttacks.rightClickComing(var1)
            && !ServerRotation.pending()
            && var1.field_1771 <= 0
            && MeleeAttacks.handsFree(var1, var4)) {
            if (!(var4.method_18798().field_1351 >= 0.0)) {
               EntityHitResult var5 = MeleeAttacks.crosshairHit(var1);
               LivingEntity var2;
               Entity var3;
               if (var5 != null && (var3 = var5.method_17782()) instanceof LivingEntity && MeleeAttacks.isEnemy(var4, var2 = (LivingEntity)var3)) {
                  if (var2 instanceof PlayerEntity || this.mobs.get()) {
                     if (this.smashReady(var4, var2)) {
                        if (MeleeAttacks.shieldBlocks(var2, var4) && !MeleeAttacks.shieldBroken(var2)) {
                           int var6 = ShieldBreakerModule.bestAxe(var4);
                           if (canHoldOneTick(var4, var2) && var6 >= 0 && (this.autoSwap.get() || this.swapBack.get()) && this.slots.take(var4, var6)) {
                              if (MeleeAttacks.attack(var1, var5)) {
                                 MeleeAttacks.noteShieldBroken(var2);
                                 MeleeAttacks.claimTick(var1);
                              } else {
                                 this.slots.drop(var1, true);
                              }
                           }
                        } else if (this.holdsSmash(var4, var2)) {
                           InputControl.dropSprint();
                           var1.field_1771 = Math.max(var1.field_1771, 1);
                        } else {
                           if (MeleeAttacks.attack(var1, var5)) {
                              this.smashed = true;
                              MeleeAttacks.claimTick(var1);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void noteSmash() {
      this.smashed = true;
   }

   public boolean smashReady(ClientPlayerEntity var1, LivingEntity var2) {
      return this.isEnabled()
         && !this.smashed
         && MaceItem.method_58659(var1)
         && var1.field_6017 >= this.minFall.get()
         && (var1.method_6047().method_31574(Items.field_49814) || this.autoSwap.get() && this.bestMace(var1, var2, true) >= 0);
   }

   public boolean holdsSmash(ClientPlayerEntity var1, LivingEntity var2) {
      boolean var3 = MeleeAttacks.charge(var1) <= 0.9F && var1.method_7261(1.5F) > 0.9F;
      return canHoldOneTick(var1, var2) && (this.smashTiming.is("Delayed") || var3 || var2.field_6235 > 0 || var1.method_5624());
   }

   private static boolean canHoldOneTick(ClientPlayerEntity var0, LivingEntity var1) {
      Vec3d var2 = var0.method_18798();
      if (!var0.method_73183().method_8587(var0, var0.method_5829().method_997(var2))) {
         return false;
      } else {
         double var3 = var0.method_55755() - 0.35;
         if (var3 <= 0.0) {
            return false;
         } else {
            Vec3d var5 = var0.method_33571().method_1019(var2);
            Vec3d var6 = var1.method_73189().method_1023(var1.field_6014, var1.field_6036, var1.field_5969);
            Box var7 = var1.method_5829().method_997(var6).method_1014(var1.method_5871());
            if (var7.method_1006(var5)) {
               return true;
            } else {
               Optional var8 = var7.method_992(var5, var5.method_1019(var0.method_5828(1.0F).method_1021(var3)));
               return var8.isPresent() && MeleeAttacks.lineIsClear(var0, var5, (Vec3d)var8.get());
            }
         }
      }
   }
}
