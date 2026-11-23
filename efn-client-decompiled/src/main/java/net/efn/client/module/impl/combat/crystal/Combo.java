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

   public static List<LivingEntity> enemies(MinecraftClient var0, ClientPlayerEntity var1) {
      ArrayList var2 = new ArrayList();

      for (AbstractClientPlayerEntity var4 : var0.field_1687.method_18456()) {
         if (MeleeAttacks.isEnemy(var1, var4) && var1.method_5858(var4) < 100.0) {
            var2.add(var4);
         }
      }

      LivingEntity var5 = var1.field_6012 <= preferredUntil ? preferred : null;
      var2.sort(
         Comparator.comparingDouble(
            var2x -> var1.method_5739(var2x) - (var2x == focus ? 2.0 : 0.0) - (var2x == var5 ? 6.0 : 0.0) - (CrystalDamage.health(var2x) <= 10.0F ? 2.0 : 0.0)
         )
      );
      focus = var2.isEmpty() ? null : (LivingEntity)var2.get(0);
      return (List<LivingEntity>)(var2.size() > 3 ? var2.subList(0, 3) : var2);
   }

   public static void prefer(ClientPlayerEntity var0, LivingEntity var1) {
      preferred = var1;
      preferredUntil = var0.field_6012 + 2;
   }

   public static void flight(ClientPlayerEntity var0, LivingEntity var1, IntFunction<Vec3d> var2) {
      flying = var1;
      flight = var2;
      flightUntil = var0.field_6012 + 2;
   }

   public static List<LivingEntity> friends(MinecraftClient var0, ClientPlayerEntity var1) {
      ArrayList var2 = new ArrayList();

      for (AbstractClientPlayerEntity var4 : var0.field_1687.method_18456()) {
         if (var4 != var1 && var4.method_5805() && MeleeAttacks.isFriend(var4) && var1.method_5858(var4) < 100.0) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public static Vec3d lead(LivingEntity var0, int var1) {
      PositionInterpolator var3 = var0.method_66233();
      Vec3d var4 = var3 == null ? var0.method_73189() : var3.method_66265();
      Vec3d var5 = var4.method_1020(var0.method_73189());
      int var6 = var1;
      Vec3d var2;
      if (var0 instanceof ClientPlayerEntity) {
         var2 = var0.method_73189().method_1023(var0.field_6014, var0.field_6036, var0.field_5969);
      } else {
         Combo.Track var7 = TRACKS.computeIfAbsent(var0, var0x -> new Combo.Track());
         var7.note(var0, var4);
         var2 = var7.velocity(var0);
         var6 = var1 + Math.min(var7.age(var0), var0.method_5864().method_18388());
         Vec3d var8 = flown(var0, var1);
         if (var8 != null) {
            return var8;
         }
      }

      return var1 > 0 && !(var2.method_37268() > 1.0) ? var5.method_1019(travel(var0, var4, var2, var6)) : var5;
   }

   private static Vec3d flown(LivingEntity var0, int var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      return var1 > 0 && var0 == flying && flight != null && var2 != null && var2.field_6012 <= flightUntil ? flight.apply(var1) : null;
   }

   private static Vec3d travel(LivingEntity var0, Vec3d var1, Vec3d var2, int var3) {
      Box var4 = var0.method_5829().method_997(var1.method_1020(var0.method_73189()));
      boolean var5 = var0.method_6128() || var0.method_52535() || var0.method_6101() || var0.method_5740() || var0.method_5765();
      double var6 = var2.field_1352;
      double var8 = var2.field_1351;
      double var10 = var2.field_1350;
      Vec3d var12 = Vec3d.field_1353;

      for (int var13 = 0; var13 < var3; var13++) {
         if (!var5) {
            var8 = (var8 - 0.08) * 0.98;
         }

         Vec3d var14 = Entity.method_20736(var0, new Vec3d(var6, var8, var10), var4, var0.method_73183(), List.of());
         if (var14.field_1352 != var6) {
            var6 = 0.0;
         }

         if (var14.field_1351 != var8) {
            var8 = 0.0;
         }

         if (var14.field_1350 != var10) {
            var10 = 0.0;
         }

         var4 = var4.method_997(var14);
         var12 = var12.method_1019(var14);
      }

      return var12;
   }

   public static boolean inHole(LivingEntity var0) {
      BlockPos var1 = var0.method_24515();

      for (Direction var3 : Type.field_11062) {
         BlockState var4 = var0.method_73183().method_8320(var1.method_10093(var3));
         if (var4.method_26204().method_9520() < 600.0F) {
            return false;
         }
      }

      return true;
   }

   public static boolean kills(float var0, LivingEntity var1) {
      return var0 >= CrystalDamage.health(var1);
   }

   public static float value(float var0, LivingEntity var1) {
      float var2 = CrystalDamage.health(var1);
      float var3 = var0 < var2 ? 0.0F : (var1.method_24518(Items.field_8288) ? 500.0F : 1000.0F);
      return Math.min(var0, var2) + var3;
   }

   public static Combo.Shot pick(List<Combo.Shot> var0) {
      Combo.Shot var1 = best(var0, Combo.Kind.BREAK);
      Combo.Shot var2 = best(var0, Combo.Kind.DETONATE);
      if (var1 == null && var2 == null) {
         for (Combo.Kind var6 : SETUP) {
            Combo.Shot var7 = best(var0, var6);
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

   private static Combo.Shot best(List<Combo.Shot> var0, Combo.Kind var1) {
      Combo.Shot var2 = null;

      for (Combo.Shot var4 : var0) {
         if (var4.kind() == var1 && (var2 == null || var4.value() > var2.value() || var4.value() == var2.value() && var4.readyIn() < var2.readyIn())) {
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
      private Vec3d velocity = Vec3d.field_1353;

      private Track() {
      }

      void note(LivingEntity var1, Vec3d var2) {
         int var3 = var1.field_6012;
         if (var3 < this.sampledAt) {
            this.last = null;
         }

         boolean var4 = var3 - this.sampledAt <= 1;
         this.sampledAt = var3;
         if (!var2.equals(this.last)) {
            int var5 = var1.method_5864().method_18388();
            int var6 = var4 ? var5 : Math.max(var5, var3 - this.lastAt);
            this.velocity = this.last == null ? Vec3d.field_1353 : var2.method_1020(this.last).method_1021(1.0 / var6);
            this.last = var2;
            this.lastAt = var3;
         }
      }

      int age(LivingEntity var1) {
         return var1.field_6012 - this.lastAt;
      }

      Vec3d velocity(LivingEntity var1) {
         return this.age(var1) > var1.method_5864().method_18388() + 1 ? Vec3d.field_1353 : this.velocity;
      }
   }
}
