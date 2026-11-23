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
import net.minecraft.entity.Entity;
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
   private static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{
      EquipmentSlot.field_6169, EquipmentSlot.field_6174, EquipmentSlot.field_6172, EquipmentSlot.field_6166
   };

   private CrystalDamage() {
   }

   public static float estimate(Vec3d var0, LivingEntity var1) {
      return new CrystalDamage.Victim(var1, Vec3d.field_1353).damage(var0);
   }

   public static float estimate(Vec3d var0, LivingEntity var1, float var2, BlockPos var3) {
      return new CrystalDamage.Victim(var1, Vec3d.field_1353).damage(var0, var2, var3);
   }

   public static float health(LivingEntity var0) {
      return var0.method_6032() + var0.method_6067();
   }

   public static boolean endangers(PlayerEntity var0, float var1) {
      return !var0.method_68878() && var1 * 1.15F + 2.0F >= health(var0);
   }

   private static float falloff(double var0) {
      return falloff(var0, 12.0);
   }

   private static float falloff(double var0, double var2) {
      return (float)((var0 * var0 + var0) / 2.0 * 7.0 * var2 + 1.0);
   }

   private static float protection(LivingEntity var0) {
      Registry var1 = var0.method_56673().method_30530(RegistryKeys.field_41265);
      int var2 = 0;

      for (EquipmentSlot var6 : ARMOR) {
         ItemStack var7 = var0.method_6118(var6);
         if (!var7.method_7960()) {
            var2 += level(var1, Enchantments.field_9111, var7) + 2 * level(var1, Enchantments.field_9107, var7);
         }
      }

      return var2;
   }

   private static int level(Registry<Enchantment> var0, RegistryKey<Enchantment> var1, ItemStack var2) {
      return var0.method_46746(var1).map(var1x -> EnchantmentHelper.method_8225(var1x, var2)).orElse(0);
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

      public Victim(LivingEntity var1, Vec3d var2) {
         this.entity = var1;
         this.position = var1.method_73189().method_1019(var2);
         this.box = var1.method_5829().method_997(var2);
         this.source = var1.method_48923().method_48819(null, null);
         this.armor = var1.method_6096();
         this.toughness = (float)var1.method_45325(EntityAttributes.field_23725);
         this.protection = CrystalDamage.protection(var1);
         StatusEffectInstance var3 = var1.method_6112(StatusEffects.field_5907);
         this.resistance = var3 == null ? 1.0F : Math.max(25 - (var3.method_5578() + 1) * 5, 0) / 25.0F;
         ItemStack var4 = var1.method_62821();
         this.shield = var4 == null ? null : (BlocksAttacksComponent)var4.method_58694(DataComponentTypes.field_56396);
      }

      public LivingEntity entity() {
         return this.entity;
      }

      public float damage(Vec3d var1) {
         return this.damage(var1, false);
      }

      public float damage(Vec3d var1, boolean var2) {
         return this.reduce(this.raw(var1, var2));
      }

      public float raw(Vec3d var1, boolean var2) {
         double var3 = Math.sqrt(this.position.method_1025(var1)) / 12.0;
         if (var3 > 1.0) {
            return 0.0F;
         } else {
            float var5 = this.scaled(CrystalDamage.falloff((1.0 - var3) * this.exposure(var1, var2)));
            return var5 - this.blocked(var1, var5);
         }
      }

      public float reduce(float var1) {
         float var2 = DamageUtil.method_5496(this.entity, var1, this.source, this.armor, this.toughness);
         return DamageUtil.method_5497(var2 * this.resistance, this.protection);
      }

      public float damage(Vec3d var1, float var2, BlockPos var3) {
         return this.reduce(this.raw(var1, var2, var3));
      }

      public float raw(Vec3d var1, float var2, BlockPos var3) {
         double var4 = var2 * 2.0;
         double var6 = Math.sqrt(this.position.method_1025(var1)) / var4;
         if (var6 > 1.0) {
            return 0.0F;
         } else {
            float var8 = this.scaled(CrystalDamage.falloff((1.0 - var6) * this.blastExposure(var1, var3), var4));
            return var8 - this.blocked(var1, var8);
         }
      }

      private float blocked(Vec3d var1, float var2) {
         if (this.shield != null && !(var2 <= 0.0F)) {
            Vec3d var3 = this.entity.method_5631(0.0F, this.entity.method_5791());
            Vec3d var4 = var1.method_1020(this.position);
            Vec3d var5 = new Vec3d(var4.field_1352, 0.0, var4.field_1350).method_1029();
            double var6 = Math.acos(MathHelper.method_15350(var5.method_1026(var3), -1.0, 1.0));
            return this.shield.method_67202(this.source, var2, var6);
         } else {
            return 0.0F;
         }
      }

      public float upperBound(Vec3d var1) {
         double var2 = Math.sqrt(this.position.method_1025(var1)) / 12.0;
         return var2 > 1.0 ? 0.0F : this.reduce(this.scaled(CrystalDamage.falloff(1.0 - var2)));
      }

      public float upperBound(Vec3d var1, float var2) {
         double var3 = var2 * 2.0;
         double var5 = Math.sqrt(this.position.method_1025(var1)) / var3;
         return var5 > 1.0 ? 0.0F : this.reduce(this.scaled(CrystalDamage.falloff(1.0 - var5, var3)));
      }

      private float exposure(Vec3d var1, boolean var2) {
         Float var3 = this.exposures.get(var1);
         if (var3 != null) {
            return var3;
         } else {
            Box var4 = var2 ? new Box(BlockPos.method_49637(var1.field_1352, var1.field_1351 - 0.5, var1.field_1350)) : null;
            float var5 = !this.openAir(var1, null) || var4 != null && this.hull(var1).method_994(var4) ? this.seenPercent(var1, var4, null) : 1.0F;
            this.exposures.put(var1, var5);
            return var5;
         }
      }

      private float blastExposure(Vec3d var1, BlockPos var2) {
         CrystalDamage.Blast var3 = new CrystalDamage.Blast(var1, var2);
         Float var4 = this.blastExposures.get(var3);
         if (var4 != null) {
            return var4;
         } else {
            float var5 = this.openAir(var1, var2) ? 1.0F : this.seenPercent(var1, null, var2);
            this.blastExposures.put(var3, var5);
            return var5;
         }
      }

      private Box hull(Vec3d var1) {
         return this.box.method_991(new Box(var1, var1));
      }

      private boolean openAir(Vec3d var1, BlockPos var2) {
         World var3 = this.entity.method_73183();
         ShapeContext var4 = ShapeContext.method_16195((Entity)this.entity);
         int var5 = MathHelper.method_15357(Math.min(this.box.field_1323, var1.field_1352));
         int var6 = MathHelper.method_15357(Math.min(this.box.field_1322, var1.field_1351));
         int var7 = MathHelper.method_15357(Math.min(this.box.field_1321, var1.field_1350));
         int var8 = MathHelper.method_15357(Math.max(this.box.field_1320, var1.field_1352));
         int var9 = MathHelper.method_15357(Math.max(this.box.field_1325, var1.field_1351));
         int var10 = MathHelper.method_15357(Math.max(this.box.field_1324, var1.field_1350));
         Mutable var11 = new Mutable();

         for (int var12 = var5; var12 <= var8; var12++) {
            for (int var13 = var6; var13 <= var9; var13++) {
               for (int var14 = var7; var14 <= var10; var14++) {
                  var11.method_10103(var12, var13, var14);
                  if (!var11.equals(var2) && !var3.method_8320(var11).method_26194(var3, var11, var4).method_1110()) {
                     return false;
                  }
               }
            }
         }

         return true;
      }

      private float seenPercent(Vec3d var1, Box var2, BlockPos var3) {
         double var4 = 1.0 / ((this.box.field_1320 - this.box.field_1323) * 2.0 + 1.0);
         double var6 = 1.0 / ((this.box.field_1325 - this.box.field_1322) * 2.0 + 1.0);
         double var8 = 1.0 / ((this.box.field_1324 - this.box.field_1321) * 2.0 + 1.0);
         double var10 = (1.0 - Math.floor(1.0 / var4) * var4) / 2.0;
         double var12 = (1.0 - Math.floor(1.0 / var8) * var8) / 2.0;
         if (!(var4 < 0.0) && !(var6 < 0.0) && !(var8 < 0.0)) {
            World var14 = this.entity.method_73183();
            int var15 = 0;
            int var16 = 0;

            for (double var17 = 0.0; var17 <= 1.0; var17 += var4) {
               for (double var19 = 0.0; var19 <= 1.0; var19 += var6) {
                  for (double var21 = 0.0; var21 <= 1.0; var21 += var8) {
                     Vec3d var23 = new Vec3d(
                        MathHelper.method_16436(var17, this.box.field_1323, this.box.field_1320) + var10,
                        MathHelper.method_16436(var19, this.box.field_1322, this.box.field_1325),
                        MathHelper.method_16436(var21, this.box.field_1321, this.box.field_1324) + var12
                     );
                     BlockHitResult var24 = var14.method_17742(
                        new RaycastContext(var23, var1, ShapeType.field_17558, FluidHandling.field_1348, (Entity)this.entity)
                     );
                     if ((var24.method_17783() == Type.field_1333 || var24.method_17777().equals(var3)) && (var2 == null || !throughBlock(var2, var23, var1))) {
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

      private static boolean throughBlock(Box var0, Vec3d var1, Vec3d var2) {
         Vec3d var3 = new Vec3d(
            MathHelper.method_16436(1.0E-7, var2.field_1352, var1.field_1352),
            MathHelper.method_16436(1.0E-7, var2.field_1351, var1.field_1351),
            MathHelper.method_16436(1.0E-7, var2.field_1350, var1.field_1350)
         );
         return var0.method_1006(var1) || var0.method_992(var1, var3).isPresent();
      }

      private float scaled(float var1) {
         if (!(this.entity instanceof PlayerEntity)) {
            return var1;
         } else {
            return switch (this.entity.method_73183().method_8407()) {
               case field_5801 -> 0.0F;
               case field_5805 -> Math.min(var1 / 2.0F + 1.0F, var1);
               case field_5802 -> var1;
               case field_5807 -> var1 * 1.5F;
               default -> throw new MatchException(null, null);
            };
         }
      }
   }
}
