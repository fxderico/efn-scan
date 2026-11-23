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

   public static void noteAction(PlayerEntity var0) {
      actorId = var0.method_5628();
      actionTick = var0.field_6012;
   }

   public static boolean actedThisTick(PlayerEntity var0) {
      return actionTick == var0.field_6012 && actorId == var0.method_5628();
   }

   public static boolean handsFree(MinecraftClient var0, ClientPlayerEntity var1) {
      return !var1.method_6115()
         && !var1.method_3144()
         && !var0.field_1761.method_2923()
         && !rowing(var1)
         && !clickPending(var0.field_1690.field_1831)
         && !clickPending(var0.field_1690.field_1869);
   }

   private static boolean rowing(ClientPlayerEntity var0) {
      PlayerInput var1 = ((AccessorLocalPlayer)var0).efn$lastSentInput();
      return var0.method_5854() instanceof AbstractBoatEntity && (var1.comp_3159() != var1.comp_3160() || var1.comp_3161() != var1.comp_3162());
   }

   public static boolean clickPending(MinecraftClient var0) {
      return clickPending(var0.field_1690.field_1886);
   }

   public static boolean clickPending(KeyBinding var0) {
      return ((AccessorTriggerbotKeyMapping)var0).efn$triggerbotClickCount() > 0;
   }

   public static boolean rightClickComing(MinecraftClient var0) {
      return var0.field_1690.field_1904.method_1434() || clickPending(var0.field_1690.field_1904);
   }

   public static void takeAttackClicks(MinecraftClient var0) {
      KeyBinding var1 = var0.field_1690.field_1886;

      while (var1.method_1436()) {
      }
   }

   public static void claimTick(MinecraftClient var0) {
      takeAttackClicks(var0);
      KeyBinding var1 = var0.field_1690.field_1904;

      while (var1.method_1436()) {
      }

      var0.field_1771 = Math.max(var0.field_1771, 1);
      AccessorCombatMinecraft var2 = (AccessorCombatMinecraft)var0;
      var2.efn$setRightClickDelay(Math.max(var2.efn$rightClickDelay(), 2));
   }

   public static boolean canAct(MinecraftClient var0) {
      if (var0.field_1724 == null || var0.field_1687 == null || var0.field_1761 == null || var0.field_1755 != null) {
         return false;
      } else if (AutoEatModule.isEating()) {
         return false;
      } else {
         FreecamModule var1 = ModuleRegistry.get(FreecamModule.class);
         return var1 == null || !var1.isEnabled();
      }
   }

   public static EntityHitResult crosshairHit(MinecraftClient var0) {
      ClientPlayerEntity var2 = var0.field_1724;
      if (var2 != null && var0.field_1687 != null) {
         HitResult var3 = var2.method_76762(1.0F, var2);
         EntityHitResult var1;
         if (var3 instanceof EntityHitResult && (var1 = (EntityHitResult)var3).method_17783() == Type.field_1331) {
            return lineIsClear(var2, var2.method_33571(), var1.method_17784()) ? var1 : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static boolean lineIsClear(ClientPlayerEntity var0, Vec3d var1, Vec3d var2) {
      BlockHitResult var3 = var0.method_73183().method_17742(new RaycastContext(var1, var2, ShapeType.field_17558, FluidHandling.field_1348, var0));
      return var3.method_17783() == Type.field_1333 || var3.method_17784().method_1025(var1) >= var1.method_1025(var2) - 0.001;
   }

   public static boolean attack(MinecraftClient var0, EntityHitResult var1) {
      return attack(var0, var1, 0.0);
   }

   public static boolean attack(MinecraftClient var0, EntityHitResult var1, double var2) {
      ClientPlayerEntity var5 = var0.field_1724;
      if (var5 == null || var0.field_1687 == null || var0.field_1761 == null) {
         return false;
      } else if (var0.field_1771 > 0 || actedThisTick(var5)) {
         return false;
      } else if (handsFree(var0, var5) && !var5.method_7325()) {
         if (var5.method_5624() != ((AccessorLocalPlayer)var5).efn$wasSprinting()) {
            return false;
         } else {
            ItemStack var6 = var5.method_6047();
            if (var6.method_57826(DataComponentTypes.field_63631)) {
               return false;
            } else if (var6.method_45435(var0.field_1687.method_45162()) && !var5.method_75202(var6, 0)) {
               Entity var7 = var1.method_17782();
               if (var7.method_31481()) {
                  return false;
               } else {
                  AttackRangeComponent var8 = (AttackRangeComponent)var6.method_58694(DataComponentTypes.field_64680);
                  boolean var4 = var8 != null ? var8.method_76736(var5, var1.method_17784()) : var5.method_56092(var7.method_5829(), var2);
                  if (!var4) {
                     return false;
                  } else {
                     var0.field_1761.method_2918(var5, var7);
                     if (!actedThisTick(var5)) {
                        return false;
                     } else {
                        var5.method_6104(Hand.field_5808);
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

   public static boolean stab(MinecraftClient var0, EntityHitResult var1) {
      ClientPlayerEntity var3 = var0.field_1724;
      if (var3 == null || var0.field_1687 == null || var0.field_1761 == null) {
         return false;
      } else if (var0.field_1771 > 0 || actedThisTick(var3)) {
         return false;
      } else if (handsFree(var0, var3) && !var3.method_7325()) {
         if (var3.method_5624() != ((AccessorLocalPlayer)var3).efn$wasSprinting()) {
            return false;
         } else {
            ItemStack var4 = var3.method_6047();
            PiercingWeaponComponent var5 = (PiercingWeaponComponent)var4.method_58694(DataComponentTypes.field_63631);
            if (var5 == null) {
               return false;
            } else if (var4.method_45435(var0.field_1687.method_45162()) && !var3.method_75202(var4, 0)) {
               Entity var6 = var1.method_17782();
               if (var6.method_31481()) {
                  return false;
               } else {
                  AttackRangeComponent var7 = (AttackRangeComponent)var4.method_58694(DataComponentTypes.field_64680);
                  boolean var2 = var7 != null ? var7.method_76736(var3, var1.method_17784()) : var3.method_56092(var6.method_5829(), 0.0);
                  if (!var2) {
                     return false;
                  } else {
                     var0.field_1761.method_75407(var5);
                     if (!actedThisTick(var3)) {
                        return false;
                     } else {
                        var3.method_6104(Hand.field_5808);
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

   public static Entity stabTarget(ClientPlayerEntity var0, Predicate<Entity> var1) {
      AttackRangeComponent var2 = var0.method_76693();
      Rotation var3 = ServerRotation.sent();
      Vec3d var4 = var3 != null ? Vec3d.method_1030(var3.pitch(), var3.yaw()) : var0.method_5828(1.0F);
      Vec3d var5 = var0.method_33571();
      Vec3d var6 = var5.method_1019(var4.method_1021(var2.method_76732(var0)));
      Vec3d var7 = var5.method_1019(var4.method_1021(var2.method_76739(var0) + Math.max(0.0, var0.method_60478().method_1026(var4))));
      BlockHitResult var8 = var0.method_73183().method_17742(new RaycastContext(var5, var7, ShapeType.field_17558, FluidHandling.field_1348, var0));
      if (var8.method_17783() != Type.field_1333) {
         var7 = var8.method_17784();
      }

      float var9 = var2.comp_5262();

      for (Entity var11 : var0.method_73183().method_8333(var0, new Box(var5, var7).method_1014(var9 + 1.0), var1)) {
         Box var12 = var11.method_5829().method_1014(var9);
         if (var12.method_1006(var6) || var12.method_992(var6, var7).isPresent()) {
            return var11;
         }
      }

      return null;
   }

   private static boolean isHittable(ClientPlayerEntity var0, Entity var1) {
      if (!(var1 instanceof LivingEntity var3 && var1 != var0 && !(var1 instanceof ArmorStandEntity))) {
         return false;
      } else {
         PlayerEntity var2;
         return var3.method_5805() && var1.method_5732() && var0.method_5854() != var1
            ? !(var1 instanceof PlayerEntity) || !(var2 = (PlayerEntity)var1).method_7325() && !var2.method_68878()
            : false;
      }
   }

   public static boolean isEnemy(ClientPlayerEntity var0, Entity var1) {
      PlayerEntity var2;
      return isHittable(var0, var1) && !isFriend(var1)
         ? !(var1 instanceof PlayerEntity) || var0.method_7256(var2 = (PlayerEntity)var1) && var0.field_3944.method_2871(var2.method_5667()) != null
         : false;
   }

   public static boolean isHostile(LivingEntity var0) {
      MobEntity var1;
      return var0 instanceof Monster && (!(var0 instanceof Angerable) || var0 instanceof MobEntity && (var1 = (MobEntity)var0).method_6510());
   }

   public static float charge(PlayerEntity var0) {
      return var0.method_7261(0.5F);
   }

   public static int ticksToFull(PlayerEntity var0) {
      return Math.max(0, MathHelper.method_15386((1.0F - charge(var0)) * var0.method_7279()));
   }

   public static boolean airborne(PlayerEntity var0) {
      return !var0.method_24828()
         && !var0.method_6101()
         && !var0.method_5799()
         && !var0.method_74025()
         && !var0.method_5765()
         && !var0.method_31549().field_7479
         && !var0.method_6128();
   }

   public static boolean wouldCrit(PlayerEntity var0, Entity var1) {
      return charge(var0) > 0.9F && var0.field_6017 > 0.0 && critStance(var0, var1);
   }

   public static boolean critStance(PlayerEntity var0, Entity var1) {
      return !var0.method_24828()
         && !var0.method_6101()
         && !var0.method_5799()
         && !var0.method_74025()
         && !var0.method_5765()
         && !var0.method_5624()
         && var1 instanceof LivingEntity;
   }

   public static boolean shieldBlocks(LivingEntity var0, PlayerEntity var1) {
      return shieldBlocks(var0, var1, var0.method_62821());
   }

   public static boolean shieldBlocks(LivingEntity var0, PlayerEntity var1, ItemStack var2) {
      if (var2 == null) {
         return false;
      } else {
         BlocksAttacksComponent var3 = (BlocksAttacksComponent)var2.method_58694(DataComponentTypes.field_56396);
         if (var3 == null) {
            return false;
         } else {
            DamageSource var4 = var1.method_73183().method_48963().method_48802(var1);
            if (var3.comp_3637().map(var1x -> var4.method_48789(var1x)).orElse(false)) {
               return false;
            } else {
               Vec3d var5 = var0.method_5631(0.0F, var0.method_5791());
               Vec3d var6 = var1.method_73189().method_1020(var0.method_73189());
               Vec3d var7 = new Vec3d(var6.field_1352, 0.0, var6.field_1350).method_1029();
               double var8 = Math.acos(MathHelper.method_15350(var7.method_1026(var5), -1.0, 1.0));
               return var3.method_67202(var4, 1.0F, var8) > 0.0F;
            }
         }
      }
   }

   public static boolean wastedOnShield(ClientPlayerEntity var0, LivingEntity var1) {
      if (shieldBlocks(var1, var0) && !shieldBroken(var1) && !disablesShields(var0.method_6047())) {
         ShieldBreakerModule var2 = ModuleRegistry.get(ShieldBreakerModule.class);
         if (var2 != null && var2.isEnabled()) {
            PlayerInventory var3 = var0.method_31548();

            for (int var4 = 0; var4 < PlayerInventory.method_7368(); var4++) {
               if (disablesShields(var3.method_5438(var4))) {
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

   public static void noteShieldBroken(LivingEntity var0) {
      shieldBrokenId = var0.method_5628();
      shieldBrokenUntil = var0.field_6012 + 20;
   }

   public static boolean shieldBroken(LivingEntity var0) {
      return var0.method_5628() == shieldBrokenId && var0.field_6012 < shieldBrokenUntil;
   }

   public static boolean disablesShields(ItemStack var0) {
      WeaponComponent var1 = (WeaponComponent)var0.method_58694(DataComponentTypes.field_55878);
      return var1 != null && var1.comp_3602() > 0.0F;
   }

   public static boolean isMeleeWeapon(ItemStack var0) {
      return var0.method_31573(ItemTags.field_42611)
         || var0.method_31573(ItemTags.field_42612)
         || var0.method_31573(ItemTags.field_63257)
         || var0.method_31574(Items.field_49814)
         || var0.method_31574(Items.field_8547);
   }

   public static double attackDamage(ItemStack var0) {
      double[] var1 = new double[]{0.0};
      var0.method_57354(EquipmentSlot.field_6173, (var1x, var2) -> {
         if (EntityAttributes.field_23721.equals(var1x) && var2.comp_2450() == Operation.field_6328) {
            var1[0] += var2.comp_2449();
         }
      });
      return var1[0];
   }

   public static boolean isFriend(Entity var0) {
      return !(var0 instanceof PlayerEntity var1) ? false : FriendStore.getInstance().isFriend(var1.method_7334().name());
   }

   public static int spread(int var0) {
      return var0 > 0 ? ThreadLocalRandom.current().nextInt(var0 + 1) : 0;
   }
}
