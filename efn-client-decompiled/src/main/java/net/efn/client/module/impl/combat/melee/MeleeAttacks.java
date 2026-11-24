package net.efn.client.module.impl.combat.melee;

import com.example.client.mixin.AccessorCombatMinecraft;
import com.example.client.mixin.AccessorLocalPlayer;
import com.example.client.mixin.AccessorTriggerbotKeyMapping;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.efn.client.config.FriendStore;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.ShieldBreakerModule;
import net.efn.client.module.impl.misc.AutoEatModule;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.component.type.WeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class MeleeAttacks {
   public static final float CRIT_CHARGE = 0.9F;
   private static final double LINE_EPSILON_SQ = 0.001;
   private static final int SHIELD_BROKEN_TICKS = 20;
   private static int actorId;
   private static int actionTick = -1;
   private static int shieldBrokenId;
   private static int shieldBrokenUntil;

   private MeleeAttacks() {
   }

   public static void noteAction(PlayerEntity playerEntity) {
      actorId = playerEntity.getId();
      actionTick = playerEntity.age;
   }

   public static boolean actedThisTick(PlayerEntity playerEntity) {
      return actionTick == playerEntity.age && actorId == playerEntity.getId();
   }

   public static boolean handsFree(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      return !clientPlayerEntity.isUsingItem()
         && !clientPlayerEntity.isRiding()
         && !minecraftClient.interactionManager.isBreakingBlock()
         && !rowing(clientPlayerEntity)
         && !clickPending(minecraftClient.options.swapHandsKey)
         && !clickPending(minecraftClient.options.dropKey);
   }

   private static boolean rowing(ClientPlayerEntity clientPlayerEntity) {
      PlayerInput var1 = ((AccessorLocalPlayer)clientPlayerEntity).efn$lastSentInput();
      return clientPlayerEntity.getVehicle() instanceof AbstractBoatEntity && (var1.forward() != var1.backward() || var1.left() != var1.right());
   }

   public static boolean clickPending(MinecraftClient minecraftClient) {
      return clickPending(minecraftClient.options.attackKey);
   }

   public static boolean clickPending(KeyBinding keyBinding) {
      return ((AccessorTriggerbotKeyMapping)keyBinding).efn$triggerbotClickCount() > 0;
   }

   public static boolean rightClickComing(MinecraftClient minecraftClient) {
      return minecraftClient.options.useKey.isPressed() || clickPending(minecraftClient.options.useKey);
   }

   public static void takeAttackClicks(MinecraftClient minecraftClient) {
      KeyBinding var1 = minecraftClient.options.attackKey;

      while (var1.wasPressed()) {
      }
   }

   public static void claimTick(MinecraftClient minecraftClient) {
      takeAttackClicks(minecraftClient);
      KeyBinding var1 = minecraftClient.options.useKey;

      while (var1.wasPressed()) {
      }

      minecraftClient.attackCooldown = Math.max(minecraftClient.attackCooldown, 1);
      AccessorCombatMinecraft var2 = (AccessorCombatMinecraft)minecraftClient;
      var2.efn$setRightClickDelay(Math.max(var2.efn$rightClickDelay(), 2));
   }

   public static boolean canAct(MinecraftClient minecraftClient) {
      if (minecraftClient.player == null
         || minecraftClient.world == null
         || minecraftClient.interactionManager == null
         || minecraftClient.currentScreen != null) {
         return false;
      } else if (AutoEatModule.isEating()) {
         return false;
      } else {
         FreecamModule var1 = ModuleRegistry.get(FreecamModule.class);
         return var1 == null || !var1.isEnabled();
      }
   }

   public static EntityHitResult crosshairHit(MinecraftClient minecraftClient) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (var2 != null && minecraftClient.world != null) {
         HitResult var3 = var2.getCrosshairTarget(1.0F, var2);
         EntityHitResult var1;
         if (var3 instanceof EntityHitResult && (var1 = (EntityHitResult)var3).getType() == Type.ENTITY) {
            return lineIsClear(var2, var2.getEyePos(), var1.getPos()) ? var1 : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static boolean lineIsClear(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, Vec3d vec3d2) {
      BlockHitResult var3 = clientPlayerEntity.getEntityWorld()
         .raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, clientPlayerEntity));
      return var3.getType() == Type.MISS || var3.getPos().squaredDistanceTo(vec3d) >= vec3d.squaredDistanceTo(vec3d2) - 0.001;
   }

   public static boolean attack(MinecraftClient minecraftClient, EntityHitResult entityHitResult) {
      return attack(minecraftClient, entityHitResult, 0.0);
   }

   public static boolean attack(MinecraftClient minecraftClient, EntityHitResult entityHitResult, double d) {
      ClientPlayerEntity var5 = minecraftClient.player;
      if (var5 == null || minecraftClient.world == null || minecraftClient.interactionManager == null) {
         return false;
      } else if (minecraftClient.attackCooldown > 0 || actedThisTick(var5)) {
         return false;
      } else if (handsFree(minecraftClient, var5) && !var5.isSpectator()) {
         if (var5.isSprinting() != ((AccessorLocalPlayer)var5).efn$wasSprinting()) {
            return false;
         } else {
            ItemStack var6 = var5.getMainHandStack();
            if (var6.contains(DataComponentTypes.PIERCING_WEAPON)) {
               return false;
            } else if (var6.isItemEnabled(minecraftClient.world.getEnabledFeatures()) && !var5.isBelowMinimumAttackCharge(var6, 0)) {
               Entity var7 = entityHitResult.getEntity();
               if (var7.isRemoved()) {
                  return false;
               } else {
                  AttackRangeComponent var8 = var6.get(DataComponentTypes.ATTACK_RANGE);
                  boolean var4 = var8 != null ? var8.isWithinRange(var5, entityHitResult.getPos()) : var5.canInteractWithEntityIn(var7.getBoundingBox(), d);
                  if (!var4) {
                     return false;
                  } else {
                     minecraftClient.interactionManager.attackEntity(var5, var7);
                     if (!actedThisTick(var5)) {
                        return false;
                     } else {
                        var5.swingHand(Hand.MAIN_HAND);
                        return true;
                     }
                  }
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean stab(MinecraftClient minecraftClient, EntityHitResult entityHitResult) {
      ClientPlayerEntity var3 = minecraftClient.player;
      if (var3 == null || minecraftClient.world == null || minecraftClient.interactionManager == null) {
         return false;
      } else if (minecraftClient.attackCooldown > 0 || actedThisTick(var3)) {
         return false;
      } else if (handsFree(minecraftClient, var3) && !var3.isSpectator()) {
         if (var3.isSprinting() != ((AccessorLocalPlayer)var3).efn$wasSprinting()) {
            return false;
         } else {
            ItemStack var4 = var3.getMainHandStack();
            PiercingWeaponComponent var5 = var4.get(DataComponentTypes.PIERCING_WEAPON);
            if (var5 == null) {
               return false;
            } else if (var4.isItemEnabled(minecraftClient.world.getEnabledFeatures()) && !var3.isBelowMinimumAttackCharge(var4, 0)) {
               Entity var6 = entityHitResult.getEntity();
               if (var6.isRemoved()) {
                  return false;
               } else {
                  AttackRangeComponent var7 = var4.get(DataComponentTypes.ATTACK_RANGE);
                  boolean var2 = var7 != null ? var7.isWithinRange(var3, entityHitResult.getPos()) : var3.canInteractWithEntityIn(var6.getBoundingBox(), 0.0);
                  if (!var2) {
                     return false;
                  } else {
                     minecraftClient.interactionManager.attackWithPiercingWeapon(var5);
                     if (!actedThisTick(var3)) {
                        return false;
                     } else {
                        var3.swingHand(Hand.MAIN_HAND);
                        return true;
                     }
                  }
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static Entity stabTarget(ClientPlayerEntity clientPlayerEntity, Predicate<Entity> predicate) {
      AttackRangeComponent var2 = clientPlayerEntity.getAttackRange();
      Rotation var3 = ServerRotation.sent();
      Vec3d var4 = var3 != null ? Vec3d.fromPolar(var3.pitch(), var3.yaw()) : clientPlayerEntity.getRotationVec(1.0F);
      Vec3d var5 = clientPlayerEntity.getEyePos();
      Vec3d var6 = var5.add(var4.multiply(var2.getEffectiveMinRange(clientPlayerEntity)));
      Vec3d var7 = var5.add(var4.multiply(var2.getEffectiveMaxRange(clientPlayerEntity) + Math.max(0.0, clientPlayerEntity.getMovement().dotProduct(var4))));
      BlockHitResult var8 = clientPlayerEntity.getEntityWorld()
         .raycast(new RaycastContext(var5, var7, ShapeType.COLLIDER, FluidHandling.NONE, clientPlayerEntity));
      if (var8.getType() != Type.MISS) {
         var7 = var8.getPos();
      }

      float var9 = var2.hitboxMargin();

      for (Entity var11 : clientPlayerEntity.getEntityWorld().getOtherEntities(clientPlayerEntity, new Box(var5, var7).expand(var9 + 1.0), predicate)) {
         Box var12 = var11.getBoundingBox().expand(var9);
         if (var12.contains(var6) || var12.raycast(var6, var7).isPresent()) {
            return var11;
         }
      }

      return null;
   }

   private static boolean isHittable(ClientPlayerEntity clientPlayerEntity, Entity entity) {
      if (!(entity instanceof LivingEntity var3 && entity != clientPlayerEntity && !(entity instanceof ArmorStandEntity))) {
         return false;
      } else {
         PlayerEntity var2;
         return var3.isAlive() && entity.isAttackable() && clientPlayerEntity.getVehicle() != entity
            ? !(entity instanceof PlayerEntity) || !(var2 = (PlayerEntity)entity).isSpectator() && !var2.isCreative()
            : false;
      }
   }

   public static boolean isEnemy(ClientPlayerEntity clientPlayerEntity, Entity entity) {
      PlayerEntity var2;
      return isHittable(clientPlayerEntity, entity) && !isFriend(entity)
         ? !(entity instanceof PlayerEntity)
            || clientPlayerEntity.shouldDamagePlayer(var2 = (PlayerEntity)entity)
               && clientPlayerEntity.networkHandler.getPlayerListEntry(var2.getUuid()) != null
         : false;
   }

   public static boolean isHostile(LivingEntity livingEntity) {
      MobEntity var1;
      return livingEntity instanceof Monster
         && (!(livingEntity instanceof Angerable) || livingEntity instanceof MobEntity && (var1 = (MobEntity)livingEntity).isAttacking());
   }

   public static float charge(PlayerEntity playerEntity) {
      return playerEntity.getAttackCooldownProgress(0.5F);
   }

   public static int ticksToFull(PlayerEntity playerEntity) {
      return Math.max(0, MathHelper.ceil((1.0F - charge(playerEntity)) * playerEntity.getAttackCooldownProgressPerTick()));
   }

   public static boolean airborne(PlayerEntity playerEntity) {
      return !playerEntity.isOnGround()
         && !playerEntity.isClimbing()
         && !playerEntity.isTouchingWater()
         && !playerEntity.hasBlindnessEffect()
         && !playerEntity.hasVehicle()
         && !playerEntity.getAbilities().flying
         && !playerEntity.isGliding();
   }

   public static boolean wouldCrit(PlayerEntity playerEntity, Entity entity) {
      return charge(playerEntity) > 0.9F && playerEntity.fallDistance > 0.0 && critStance(playerEntity, entity);
   }

   public static boolean critStance(PlayerEntity playerEntity, Entity entity) {
      return !playerEntity.isOnGround()
         && !playerEntity.isClimbing()
         && !playerEntity.isTouchingWater()
         && !playerEntity.hasBlindnessEffect()
         && !playerEntity.hasVehicle()
         && !playerEntity.isSprinting()
         && entity instanceof LivingEntity;
   }

   public static boolean shieldBlocks(LivingEntity livingEntity, PlayerEntity playerEntity) {
      return shieldBlocks(livingEntity, playerEntity, livingEntity.getBlockingItem());
   }

   public static boolean shieldBlocks(LivingEntity livingEntity, PlayerEntity playerEntity, ItemStack itemStack) {
      if (itemStack == null) {
         return false;
      } else {
         BlocksAttacksComponent var3 = itemStack.get(DataComponentTypes.BLOCKS_ATTACKS);
         if (var3 == null) {
            return false;
         } else {
            DamageSource var4 = playerEntity.getEntityWorld().getDamageSources().playerAttack(playerEntity);
            if (var3.bypassedBy().map(tagKey -> var4.isIn((TagKey<DamageType>)tagKey)).orElse(false)) {
               return false;
            } else {
               Vec3d var5 = livingEntity.getRotationVector(0.0F, livingEntity.getHeadYaw());
               Vec3d var6 = playerEntity.getEntityPos().subtract(livingEntity.getEntityPos());
               Vec3d var7 = new Vec3d(var6.x, 0.0, var6.z).normalize();
               double var8 = Math.acos(MathHelper.clamp(var7.dotProduct(var5), -1.0, 1.0));
               return var3.getDamageReductionAmount(var4, 1.0F, var8) > 0.0F;
            }
         }
      }
   }

   public static boolean wastedOnShield(ClientPlayerEntity clientPlayerEntity, LivingEntity livingEntity) {
      if (shieldBlocks(livingEntity, clientPlayerEntity) && !shieldBroken(livingEntity) && !disablesShields(clientPlayerEntity.getMainHandStack())) {
         ShieldBreakerModule var2 = ModuleRegistry.get(ShieldBreakerModule.class);
         if (var2 != null && var2.isEnabled()) {
            PlayerInventory var3 = clientPlayerEntity.getInventory();

            for (int var4 = 0; var4 < PlayerInventory.getHotbarSize(); var4++) {
               if (disablesShields(var3.getStack(var4))) {
                  return false;
               }
            }

            return true;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static void noteShieldBroken(LivingEntity livingEntity) {
      shieldBrokenId = livingEntity.getId();
      shieldBrokenUntil = livingEntity.age + 20;
   }

   public static boolean shieldBroken(LivingEntity livingEntity) {
      return livingEntity.getId() == shieldBrokenId && livingEntity.age < shieldBrokenUntil;
   }

   public static boolean disablesShields(ItemStack itemStack) {
      WeaponComponent var1 = itemStack.get(DataComponentTypes.WEAPON);
      return var1 != null && var1.disableBlockingForSeconds() > 0.0F;
   }

   public static boolean isMeleeWeapon(ItemStack itemStack) {
      return itemStack.isIn(ItemTags.SWORDS)
         || itemStack.isIn(ItemTags.AXES)
         || itemStack.isIn(ItemTags.SPEARS)
         || itemStack.isOf(Items.MACE)
         || itemStack.isOf(Items.TRIDENT);
   }

   public static double attackDamage(ItemStack itemStack) {
      double[] var1 = new double[]{0.0};
      itemStack.applyAttributeModifiers(EquipmentSlot.MAINHAND, (registryEntry, entityAttributeModifier) -> {
         if (EntityAttributes.ATTACK_DAMAGE.equals(registryEntry) && entityAttributeModifier.operation() == Operation.ADD_VALUE) {
            var1[0] += entityAttributeModifier.value();
         }
      });
      return var1[0];
   }

   public static boolean isFriend(Entity entity) {
      return !(entity instanceof PlayerEntity var1) ? false : FriendStore.getInstance().isFriend(var1.getGameProfile().name());
   }

   public static int spread(int i) {
      return i > 0 ? ThreadLocalRandom.current().nextInt(i + 1) : 0;
   }
}
