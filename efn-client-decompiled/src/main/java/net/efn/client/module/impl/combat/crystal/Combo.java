package net.efn.client.module.impl.combat.crystal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntFunction;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;

@Environment(EnvType.CLIENT)
public final class Combo {
   public static final double TARGET_RANGE = 10.0;
   public static final int MAX_TARGETS = 3;
   public static final float LETHAL_RANK = 1000.0F;
   public static final float POP_RANK = 500.0F;
   public static final float SELF_WEIGHT = 0.5F;
   public static final float ANCHOR_EDGE = 1.0F;
   private static final float BLAST_PROOF = 600.0F;
   private static final double FOCUS_BONUS = 2.0;
   private static final double WEAK_BONUS = 2.0;
   private static final float WEAK_HEALTH = 10.0F;
   private static final double MAX_STEP_SQR = 1.0;
   private static final double GRAVITY = 0.08;
   private static final double AIR_DRAG = 0.98;
   private static final Combo.Kind[] SETUP = new Combo.Kind[]{Combo.Kind.CHARGE, Combo.Kind.CRYSTAL, Combo.Kind.OBSIDIAN, Combo.Kind.ANCHOR};
   private static final Map<LivingEntity, Combo.Track> TRACKS = new WeakHashMap<>();
   private static final double PREFER_BONUS = 6.0;
   private static LivingEntity focus;
   private static LivingEntity preferred;
   private static int preferredUntil;
   private static LivingEntity flying;
   private static IntFunction<Vec3d> flight;
   private static int flightUntil;

   private Combo() {
   }

   public static List<LivingEntity> enemies(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      ArrayList var2 = new ArrayList();

      for (AbstractClientPlayerEntity var4 : minecraftClient.world.getPlayers()) {
         if (MeleeAttacks.isEnemy(clientPlayerEntity, var4) && clientPlayerEntity.squaredDistanceTo(var4) < 100.0) {
            var2.add(var4);
         }
      }

      LivingEntity var5 = clientPlayerEntity.age <= preferredUntil ? preferred : null;
      var2.sort(
         Comparator.comparingDouble(
            livingEntity2 -> clientPlayerEntity.distanceTo(livingEntity2)
               - (livingEntity2 == focus ? 2.0 : 0.0)
               - (livingEntity2 == var5 ? 6.0 : 0.0)
               - (CrystalDamage.health(livingEntity2) <= 10.0F ? 2.0 : 0.0)
         )
      );
      focus = var2.isEmpty() ? null : (LivingEntity)var2.get(0);
      return (List<LivingEntity>)(var2.size() > 3 ? var2.subList(0, 3) : var2);
   }

   public static void prefer(ClientPlayerEntity clientPlayerEntity, LivingEntity livingEntity) {
      preferred = livingEntity;
      preferredUntil = clientPlayerEntity.age + 2;
   }

   public static void flight(ClientPlayerEntity clientPlayerEntity, LivingEntity livingEntity, IntFunction<Vec3d> intFunction) {
      flying = livingEntity;
      flight = intFunction;
      flightUntil = clientPlayerEntity.age + 2;
   }

   public static List<LivingEntity> friends(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      ArrayList var2 = new ArrayList();

      for (AbstractClientPlayerEntity var4 : minecraftClient.world.getPlayers()) {
         if (var4 != clientPlayerEntity && var4.isAlive() && MeleeAttacks.isFriend(var4) && clientPlayerEntity.squaredDistanceTo(var4) < 100.0) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public static Vec3d lead(LivingEntity livingEntity, int i) {
      PositionInterpolator var3 = livingEntity.getInterpolator();
      Vec3d var4 = var3 == null ? livingEntity.getEntityPos() : var3.getLerpedPos();
      Vec3d var5 = var4.subtract(livingEntity.getEntityPos());
      int var6 = i;
      Vec3d var2;
      if (livingEntity instanceof ClientPlayerEntity) {
         var2 = livingEntity.getEntityPos().subtract(livingEntity.lastX, livingEntity.lastY, livingEntity.lastZ);
      } else {
         Combo.Track var7 = TRACKS.computeIfAbsent(livingEntity, livingEntityx -> new Combo.Track());
         var7.note(livingEntity, var4);
         var2 = var7.velocity(livingEntity);
         var6 = i + Math.min(var7.age(livingEntity), livingEntity.getType().getTrackTickInterval());
         Vec3d var8 = flown(livingEntity, i);
         if (var8 != null) {
            return var8;
         }
      }

      return i > 0 && !(var2.horizontalLengthSquared() > 1.0) ? var5.add(travel(livingEntity, var4, var2, var6)) : var5;
   }

   private static Vec3d flown(LivingEntity livingEntity, int i) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      return i > 0 && livingEntity == flying && flight != null && var2 != null && var2.age <= flightUntil ? flight.apply(i) : null;
   }

   private static Vec3d travel(LivingEntity livingEntity, Vec3d vec3d, Vec3d vec3d2, int i) {
      Box var4 = livingEntity.getBoundingBox().offset(vec3d.subtract(livingEntity.getEntityPos()));
      boolean var5 = livingEntity.isGliding()
         || livingEntity.isInFluid()
         || livingEntity.isClimbing()
         || livingEntity.hasNoGravity()
         || livingEntity.hasVehicle();
      double var6 = vec3d2.x;
      double var8 = vec3d2.y;
      double var10 = vec3d2.z;
      Vec3d var12 = Vec3d.ZERO;

      for (int var13 = 0; var13 < i; var13++) {
         if (!var5) {
            var8 = (var8 - 0.08) * 0.98;
         }

         Vec3d var14 = Entity.adjustMovementForCollisions(livingEntity, new Vec3d(var6, var8, var10), var4, livingEntity.getEntityWorld(), List.of());
         if (var14.x != var6) {
            var6 = 0.0;
         }

         if (var14.y != var8) {
            var8 = 0.0;
         }

         if (var14.z != var10) {
            var10 = 0.0;
         }

         var4 = var4.offset(var14);
         var12 = var12.add(var14);
      }

      return var12;
   }

   public static boolean inHole(LivingEntity livingEntity) {
      BlockPos var1 = livingEntity.getBlockPos();

      for (Direction var3 : Type.HORIZONTAL) {
         BlockState var4 = livingEntity.getEntityWorld().getBlockState(var1.offset(var3));
         if (var4.getBlock().getBlastResistance() < 600.0F) {
            return false;
         }
      }

      return true;
   }

   public static boolean kills(float f, LivingEntity livingEntity) {
      return f >= CrystalDamage.health(livingEntity);
   }

   public static float value(float f, LivingEntity livingEntity) {
      float var2 = CrystalDamage.health(livingEntity);
      float var3 = f < var2 ? 0.0F : (livingEntity.isHolding(Items.TOTEM_OF_UNDYING) ? 500.0F : 1000.0F);
      return Math.min(f, var2) + var3;
   }

   public static Combo.Shot pick(List<Combo.Shot> list) {
      Combo.Shot var1 = best(list, Combo.Kind.BREAK);
      Combo.Shot var2 = best(list, Combo.Kind.DETONATE);
      if (var1 == null && var2 == null) {
         for (Combo.Kind var6 : SETUP) {
            Combo.Shot var7 = best(list, var6);
            if (var7 != null) {
               return var7;
            }
         }

         return null;
      } else {
         boolean var3 = var1 == null || var2 != null && (var2.value() > var1.value() + 1.0F || var2.kills() && !var1.kills());
         return var3 ? var2 : var1;
      }
   }

   private static Combo.Shot best(List<Combo.Shot> list, Combo.Kind kind) {
      Combo.Shot var2 = null;

      for (Combo.Shot var4 : list) {
         if (var4.kind() == kind && (var2 == null || var4.value() > var2.value() || var4.value() == var2.value() && var4.readyIn() < var2.readyIn())) {
            var2 = var4;
         }
      }

      return var2;
   }

   @Environment(EnvType.CLIENT)
   public static enum Kind {
      BREAK,
      CRYSTAL,
      OBSIDIAN,
      ANCHOR,
      CHARGE,
      DETONATE;

      private Kind() {
      }
   }

   @Environment(EnvType.CLIENT)
   public record Shot(Combo.Kind kind, Vec3d aim, int route, float value, boolean kills, int readyIn, Object plan) {
   }

   @Environment(EnvType.CLIENT)
   private static final class Track {
      private Vec3d last;
      private int lastAt;
      private int sampledAt;
      private Vec3d velocity = Vec3d.ZERO;

      private Track() {
      }

      void note(LivingEntity livingEntity, Vec3d vec3d) {
         int var3 = livingEntity.age;
         if (var3 < this.sampledAt) {
            this.last = null;
         }

         boolean var4 = var3 - this.sampledAt <= 1;
         this.sampledAt = var3;
         if (!vec3d.equals(this.last)) {
            int var5 = livingEntity.getType().getTrackTickInterval();
            int var6 = var4 ? var5 : Math.max(var5, var3 - this.lastAt);
            this.velocity = this.last == null ? Vec3d.ZERO : vec3d.subtract(this.last).multiply(1.0 / var6);
            this.last = vec3d;
            this.lastAt = var3;
         }
      }

      int age(LivingEntity livingEntity) {
         return livingEntity.age - this.lastAt;
      }

      Vec3d velocity(LivingEntity livingEntity) {
         return this.age(livingEntity) > livingEntity.getType().getTrackTickInterval() + 1 ? Vec3d.ZERO : this.velocity;
      }
   }
}
