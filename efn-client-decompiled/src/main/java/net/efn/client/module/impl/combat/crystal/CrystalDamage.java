package net.efn.client.module.impl.combat.crystal;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ShapeContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class CrystalDamage {
   private static final double REACH = 12.0;
   private static final float SELF_SCALE = 1.15F;
   private static final float SELF_MARGIN = 2.0F;
   private static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   private CrystalDamage() {
   }

   public static float estimate(Vec3d vec3d, LivingEntity livingEntity) {
      return new CrystalDamage.Victim(livingEntity, Vec3d.ZERO).damage(vec3d);
   }

   public static float estimate(Vec3d vec3d, LivingEntity livingEntity, float f, BlockPos blockPos) {
      return new CrystalDamage.Victim(livingEntity, Vec3d.ZERO).damage(vec3d, f, blockPos);
   }

   public static float health(LivingEntity livingEntity) {
      return livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
   }

   public static boolean endangers(PlayerEntity playerEntity, float f) {
      return !playerEntity.isCreative() && f * 1.15F + 2.0F >= health(playerEntity);
   }

   private static float falloff(double d) {
      return falloff(d, 12.0);
   }

   private static float falloff(double d, double e) {
      return (float)((d * d + d) / 2.0 * 7.0 * e + 1.0);
   }

   private static float protection(LivingEntity livingEntity) {
      Registry var1 = livingEntity.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
      int var2 = 0;

      for (EquipmentSlot var6 : ARMOR) {
         ItemStack var7 = livingEntity.getEquippedStack(var6);
         if (!var7.isEmpty()) {
            var2 += level(var1, Enchantments.PROTECTION, var7) + 2 * level(var1, Enchantments.BLAST_PROTECTION, var7);
         }
      }

      return var2;
   }

   private static int level(Registry<Enchantment> registry, RegistryKey<Enchantment> registryKey, ItemStack itemStack) {
      return registry.getOptional(registryKey).map(reference -> EnchantmentHelper.getLevel(reference, itemStack)).orElse(0);
   }

   @Environment(EnvType.CLIENT)
   private record Blast(Vec3d center, BlockPos ignored) {
   }

   @Environment(EnvType.CLIENT)
   public static final class Victim {
      private final LivingEntity entity;
      private final Vec3d position;
      private final Box box;
      private final DamageSource source;
      private final float armor;
      private final float toughness;
      private final float protection;
      private final float resistance;
      private final BlocksAttacksComponent shield;
      private final Map<Vec3d, Float> exposures = new HashMap<>();
      private final Map<CrystalDamage.Blast, Float> blastExposures = new HashMap<>();

      public Victim(LivingEntity livingEntity, Vec3d vec3d) {
         this.entity = livingEntity;
         this.position = livingEntity.getEntityPos().add(vec3d);
         this.box = livingEntity.getBoundingBox().offset(vec3d);
         this.source = livingEntity.getDamageSources().explosion(null, null);
         this.armor = livingEntity.getArmor();
         this.toughness = (float)livingEntity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
         this.protection = CrystalDamage.protection(livingEntity);
         StatusEffectInstance var3 = livingEntity.getStatusEffect(StatusEffects.RESISTANCE);
         this.resistance = var3 == null ? 1.0F : Math.max(25 - (var3.getAmplifier() + 1) * 5, 0) / 25.0F;
         ItemStack var4 = livingEntity.getBlockingItem();
         this.shield = var4 == null ? null : var4.get(DataComponentTypes.BLOCKS_ATTACKS);
      }

      public LivingEntity entity() {
         return this.entity;
      }

      public float damage(Vec3d vec3d) {
         return this.damage(vec3d, false);
      }

      public float damage(Vec3d vec3d, boolean bl) {
         return this.reduce(this.raw(vec3d, bl));
      }

      public float raw(Vec3d vec3d, boolean bl) {
         double var3 = Math.sqrt(this.position.squaredDistanceTo(vec3d)) / 12.0;
         if (var3 > 1.0) {
            return 0.0F;
         } else {
            float var5 = this.scaled(CrystalDamage.falloff((1.0 - var3) * this.exposure(vec3d, bl)));
            return var5 - this.blocked(vec3d, var5);
         }
      }

      public float reduce(float f) {
         float var2 = DamageUtil.getDamageLeft(this.entity, f, this.source, this.armor, this.toughness);
         return DamageUtil.getInflictedDamage(var2 * this.resistance, this.protection);
      }

      public float damage(Vec3d vec3d, float f, BlockPos blockPos) {
         return this.reduce(this.raw(vec3d, f, blockPos));
      }

      public float raw(Vec3d vec3d, float f, BlockPos blockPos) {
         double var4 = f * 2.0;
         double var6 = Math.sqrt(this.position.squaredDistanceTo(vec3d)) / var4;
         if (var6 > 1.0) {
            return 0.0F;
         } else {
            float var8 = this.scaled(CrystalDamage.falloff((1.0 - var6) * this.blastExposure(vec3d, blockPos), var4));
            return var8 - this.blocked(vec3d, var8);
         }
      }

      private float blocked(Vec3d vec3d, float f) {
         if (this.shield != null && !(f <= 0.0F)) {
            Vec3d var3 = this.entity.getRotationVector(0.0F, this.entity.getHeadYaw());
            Vec3d var4 = vec3d.subtract(this.position);
            Vec3d var5 = new Vec3d(var4.x, 0.0, var4.z).normalize();
            double var6 = Math.acos(MathHelper.clamp(var5.dotProduct(var3), -1.0, 1.0));
            return this.shield.getDamageReductionAmount(this.source, f, var6);
         } else {
            return 0.0F;
         }
      }

      public float upperBound(Vec3d vec3d) {
         double var2 = Math.sqrt(this.position.squaredDistanceTo(vec3d)) / 12.0;
         return var2 > 1.0 ? 0.0F : this.reduce(this.scaled(CrystalDamage.falloff(1.0 - var2)));
      }

      public float upperBound(Vec3d vec3d, float f) {
         double var3 = f * 2.0;
         double var5 = Math.sqrt(this.position.squaredDistanceTo(vec3d)) / var3;
         return var5 > 1.0 ? 0.0F : this.reduce(this.scaled(CrystalDamage.falloff(1.0 - var5, var3)));
      }

      private float exposure(Vec3d vec3d, boolean bl) {
         Float var3 = this.exposures.get(vec3d);
         if (var3 != null) {
            return var3;
         } else {
            Box var4 = bl ? new Box(BlockPos.ofFloored(vec3d.x, vec3d.y - 0.5, vec3d.z)) : null;
            float var5 = !this.openAir(vec3d, null) || var4 != null && this.hull(vec3d).intersects(var4) ? this.seenPercent(vec3d, var4, null) : 1.0F;
            this.exposures.put(vec3d, var5);
            return var5;
         }
      }

      private float blastExposure(Vec3d vec3d, BlockPos blockPos) {
         CrystalDamage.Blast var3 = new CrystalDamage.Blast(vec3d, blockPos);
         Float var4 = this.blastExposures.get(var3);
         if (var4 != null) {
            return var4;
         } else {
            float var5 = this.openAir(vec3d, blockPos) ? 1.0F : this.seenPercent(vec3d, null, blockPos);
            this.blastExposures.put(var3, var5);
            return var5;
         }
      }

      private Box hull(Vec3d vec3d) {
         return this.box.union(new Box(vec3d, vec3d));
      }

      private boolean openAir(Vec3d vec3d, BlockPos blockPos) {
         World var3 = this.entity.getEntityWorld();
         ShapeContext var4 = ShapeContext.of(this.entity);
         int var5 = MathHelper.floor(Math.min(this.box.minX, vec3d.x));
         int var6 = MathHelper.floor(Math.min(this.box.minY, vec3d.y));
         int var7 = MathHelper.floor(Math.min(this.box.minZ, vec3d.z));
         int var8 = MathHelper.floor(Math.max(this.box.maxX, vec3d.x));
         int var9 = MathHelper.floor(Math.max(this.box.maxY, vec3d.y));
         int var10 = MathHelper.floor(Math.max(this.box.maxZ, vec3d.z));
         Mutable var11 = new Mutable();

         for (int var12 = var5; var12 <= var8; var12++) {
            for (int var13 = var6; var13 <= var9; var13++) {
               for (int var14 = var7; var14 <= var10; var14++) {
                  var11.set(var12, var13, var14);
                  if (!var11.equals(blockPos) && !var3.getBlockState(var11).getCollisionShape(var3, var11, var4).isEmpty()) {
                     return false;
                  }
               }
            }
         }

         return true;
      }

      private float seenPercent(Vec3d vec3d, Box box, BlockPos blockPos) {
         double var4 = 1.0 / ((this.box.maxX - this.box.minX) * 2.0 + 1.0);
         double var6 = 1.0 / ((this.box.maxY - this.box.minY) * 2.0 + 1.0);
         double var8 = 1.0 / ((this.box.maxZ - this.box.minZ) * 2.0 + 1.0);
         double var10 = (1.0 - Math.floor(1.0 / var4) * var4) / 2.0;
         double var12 = (1.0 - Math.floor(1.0 / var8) * var8) / 2.0;
         if (!(var4 < 0.0) && !(var6 < 0.0) && !(var8 < 0.0)) {
            World var14 = this.entity.getEntityWorld();
            int var15 = 0;
            int var16 = 0;

            for (double var17 = 0.0; var17 <= 1.0; var17 += var4) {
               for (double var19 = 0.0; var19 <= 1.0; var19 += var6) {
                  for (double var21 = 0.0; var21 <= 1.0; var21 += var8) {
                     Vec3d var23 = new Vec3d(
                        MathHelper.lerp(var17, this.box.minX, this.box.maxX) + var10,
                        MathHelper.lerp(var19, this.box.minY, this.box.maxY),
                        MathHelper.lerp(var21, this.box.minZ, this.box.maxZ) + var12
                     );
                     BlockHitResult var24 = var14.raycast(new RaycastContext(var23, vec3d, ShapeType.COLLIDER, FluidHandling.NONE, this.entity));
                     if ((var24.getType() == Type.MISS || var24.getBlockPos().equals(blockPos)) && (box == null || !throughBlock(box, var23, vec3d))) {
                        var15++;
                     }

                     var16++;
                  }
               }
            }

            return (float)var15 / var16;
         } else {
            return 0.0F;
         }
      }

      private static boolean throughBlock(Box box, Vec3d vec3d, Vec3d vec3d2) {
         Vec3d var3 = new Vec3d(
            MathHelper.lerp(1.0E-7, vec3d2.x, vec3d.x), MathHelper.lerp(1.0E-7, vec3d2.y, vec3d.y), MathHelper.lerp(1.0E-7, vec3d2.z, vec3d.z)
         );
         return box.contains(vec3d) || box.raycast(vec3d, var3).isPresent();
      }

      private float scaled(float f) {
         if (!(this.entity instanceof PlayerEntity)) {
            return f;
         } else {
            return switch (this.entity.getEntityWorld().getDifficulty()) {
               case PEACEFUL -> 0.0F;
               case EASY -> Math.min(f / 2.0F + 1.0F, f);
               case NORMAL -> f;
               case HARD -> f * 1.5F;
            };
         }
      }
   }
}
