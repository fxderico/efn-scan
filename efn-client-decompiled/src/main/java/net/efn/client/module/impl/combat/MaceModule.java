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
      this.slots.drop(MinecraftClient.getInstance(), this.swapBack.get());
   }

   public void beforeAttack(ClientPlayerEntity clientPlayerEntity, Entity entity) {
      if (this.autoSwap.get() && entity instanceof LivingEntity var3) {
         if (!MeleeAttacks.shieldBlocks(var3, clientPlayerEntity) || MeleeAttacks.shieldBroken(var3)) {
            boolean var4 = MaceItem.shouldDealAdditionalDamage(clientPlayerEntity);
            if (var4 || this.breachSwap.get()) {
               int var5 = this.bestMace(clientPlayerEntity, var3, var4);
               if (var5 >= 0) {
                  if (var4
                     || !(
                        expected(clientPlayerEntity, clientPlayerEntity.getInventory().getStack(var5), var3)
                           < expected(clientPlayerEntity, clientPlayerEntity.getMainHandStack(), var3) * 1.15F
                     )) {
                     this.slots.take(clientPlayerEntity, var5);
                  }
               }
            }
         }
      }
   }

   private int bestMace(ClientPlayerEntity clientPlayerEntity, LivingEntity livingEntity, boolean bl) {
      boolean var4 = bl && this.pick.is("Wind Burst");
      PlayerInventory var5 = clientPlayerEntity.getInventory();
      int var6 = -1;
      boolean var7 = false;
      float var8 = 0.0F;

      for (int var9 = 0; var9 < PlayerInventory.getHotbarSize(); var9++) {
         ItemStack var10 = var5.getStack(var9);
         if (var10.isOf(Items.MACE) && !var10.willBreakNextUse() && (bl || level(var10, Enchantments.BREACH) != 0)) {
            boolean var11 = var4 && level(var10, Enchantments.WIND_BURST) > 0;
            float var12 = expected(clientPlayerEntity, var10, livingEntity);
            if (var6 < 0 || (!var7 || var11) && (var7 != var11 || !(var12 <= var8))) {
               var6 = var9;
               var7 = var11;
               var8 = var12;
            }
         }
      }

      return var6;
   }

   private static float expected(ClientPlayerEntity clientPlayerEntity, ItemStack itemStack, LivingEntity livingEntity) {
      float var4 = MeleeAttacks.charge(clientPlayerEntity);
      double var5 = clientPlayerEntity.fallDistance;
      float var7 = (float)(1.0 + MeleeAttacks.attackDamage(clientPlayerEntity.getMainHandStack())) * (0.2F + 0.8F * var4 * var4);
      if (itemStack.isOf(Items.MACE) && MaceItem.shouldDealAdditionalDamage(clientPlayerEntity)) {
         var7 += (float)(smashBonus(var5) + 0.5 * level(itemStack, Enchantments.DENSITY) * var5);
      }

      if (MeleeAttacks.wouldCrit(clientPlayerEntity, livingEntity)) {
         var7 *= 1.5F;
      }

      int var3;
      if ((var3 = level(itemStack, Enchantments.SHARPNESS)) > 0) {
         var7 += var4 * (0.5F * var3 + 0.5F);
      }

      return afterArmor(livingEntity, var7, level(itemStack, Enchantments.BREACH));
   }

   private static double smashBonus(double d) {
      if (d <= 3.0) {
         return 4.0 * d;
      } else {
         return d <= 8.0 ? 12.0 + 2.0 * (d - 3.0) : 22.0 + d - 8.0;
      }
   }

   private static float afterArmor(LivingEntity livingEntity, float f, int i) {
      float var3 = livingEntity.getArmor();
      float var4 = (float)livingEntity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
      float var5 = MathHelper.clamp(var3 - f / (2.0F + var4 / 4.0F), var3 * 0.2F, 20.0F) / 25.0F;
      return f * (1.0F - MathHelper.clamp(var5 - 0.15F * i, 0.0F, 1.0F));
   }

   private static int level(ItemStack itemStack, RegistryKey<Enchantment> registryKey) {
      for (Entry var3 : itemStack.getEnchantments().getEnchantmentEntries()) {
         if (((RegistryEntry)var3.getKey()).matchesKey(registryKey)) {
            return var3.getIntValue();
         }
      }

      return 0;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null && var2.player.isOnGround()) {
         this.smashed = false;
      }

      this.slots.tick(var2, this.swapBack.get());
   }

   @EventHandler
   public void onSmashTick(TickEvent tickEvent) {
      if (this.smashAssist.get()) {
         this.trySmash(MinecraftClient.getInstance());
      }
   }

   private void trySmash(MinecraftClient minecraftClient) {
      if (MeleeAttacks.canAct(minecraftClient) && !CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()) {
         ClientPlayerEntity var4 = minecraftClient.player;
         if (!MeleeAttacks.actedThisTick(var4)
            && !MeleeAttacks.clickPending(minecraftClient)
            && !MeleeAttacks.rightClickComing(minecraftClient)
            && !ServerRotation.pending()
            && minecraftClient.attackCooldown <= 0
            && MeleeAttacks.handsFree(minecraftClient, var4)) {
            if (!(var4.getVelocity().y >= 0.0)) {
               EntityHitResult var5 = MeleeAttacks.crosshairHit(minecraftClient);
               LivingEntity var2;
               Entity var3;
               if (var5 != null && (var3 = var5.getEntity()) instanceof LivingEntity && MeleeAttacks.isEnemy(var4, var2 = (LivingEntity)var3)) {
                  if (var2 instanceof PlayerEntity || this.mobs.get()) {
                     if (this.smashReady(var4, var2)) {
                        if (MeleeAttacks.shieldBlocks(var2, var4) && !MeleeAttacks.shieldBroken(var2)) {
                           int var6 = ShieldBreakerModule.bestAxe(var4);
                           if (canHoldOneTick(var4, var2) && var6 >= 0 && (this.autoSwap.get() || this.swapBack.get()) && this.slots.take(var4, var6)) {
                              if (MeleeAttacks.attack(minecraftClient, var5)) {
                                 MeleeAttacks.noteShieldBroken(var2);
                                 MeleeAttacks.claimTick(minecraftClient);
                              } else {
                                 this.slots.drop(minecraftClient, true);
                              }
                           }
                        } else if (this.holdsSmash(var4, var2)) {
                           InputControl.dropSprint();
                           minecraftClient.attackCooldown = Math.max(minecraftClient.attackCooldown, 1);
                        } else {
                           if (MeleeAttacks.attack(minecraftClient, var5)) {
                              this.smashed = true;
                              MeleeAttacks.claimTick(minecraftClient);
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

   public boolean smashReady(ClientPlayerEntity clientPlayerEntity, LivingEntity livingEntity) {
      return this.isEnabled()
         && !this.smashed
         && MaceItem.shouldDealAdditionalDamage(clientPlayerEntity)
         && clientPlayerEntity.fallDistance >= this.minFall.get()
         && (clientPlayerEntity.getMainHandStack().isOf(Items.MACE) || this.autoSwap.get() && this.bestMace(clientPlayerEntity, livingEntity, true) >= 0);
   }

   public boolean holdsSmash(ClientPlayerEntity clientPlayerEntity, LivingEntity livingEntity) {
      boolean var3 = MeleeAttacks.charge(clientPlayerEntity) <= 0.9F && clientPlayerEntity.getAttackCooldownProgress(1.5F) > 0.9F;
      return canHoldOneTick(clientPlayerEntity, livingEntity)
         && (this.smashTiming.is("Delayed") || var3 || livingEntity.hurtTime > 0 || clientPlayerEntity.isSprinting());
   }

   private static boolean canHoldOneTick(ClientPlayerEntity clientPlayerEntity, LivingEntity livingEntity) {
      Vec3d var2 = clientPlayerEntity.getVelocity();
      if (!clientPlayerEntity.getEntityWorld().isSpaceEmpty(clientPlayerEntity, clientPlayerEntity.getBoundingBox().offset(var2))) {
         return false;
      } else {
         double var3 = clientPlayerEntity.getEntityInteractionRange() - 0.35;
         if (var3 <= 0.0) {
            return false;
         } else {
            Vec3d var5 = clientPlayerEntity.getEyePos().add(var2);
            Vec3d var6 = livingEntity.getEntityPos().subtract(livingEntity.lastX, livingEntity.lastY, livingEntity.lastZ);
            Box var7 = livingEntity.getBoundingBox().offset(var6).expand(livingEntity.getTargetingMargin());
            if (var7.contains(var5)) {
               return true;
            } else {
               Optional var8 = var7.raycast(var5, var5.add(clientPlayerEntity.getRotationVec(1.0F).multiply(var3)));
               return var8.isPresent() && MeleeAttacks.lineIsClear(clientPlayerEntity, var5, (Vec3d)var8.get());
            }
         }
      }
   }
}
