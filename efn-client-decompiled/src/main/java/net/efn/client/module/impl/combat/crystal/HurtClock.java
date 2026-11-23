package net.efn.client.module.impl.combat.crystal;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class HurtClock {
   private static final int NEVER = -1000;
   private static final int WINDOW = 10;
   private static final int MAX_WAIT = 20;
   private static final int MAX_MARGIN = 2;
   private static final int RELAX_STREAK = 8;
   private static final int REMOVE_GRACE = 3;
   private static final double ANCHOR_MATCH_SQR = 0.25;
   private static final Int2ObjectOpenHashMap<HurtClock.Mark> MARKS = new Int2ObjectOpenHashMap();
   private static ClientPlayerEntity lastPlayer;
   private static World lastLevel;
   private static Packet<?> lastPacket;
   private static int now;
   private static int margin;
   private static int streak;

   private HurtClock() {
   }

   public static void note(ClientPlayerEntity var0, Packet<?> var1) {
      if (var1 != lastPacket) {
         lastPacket = var1;
         if (var1 instanceof BundleS2CPacket var2) {
            for (Packet var4 : var2.method_48324()) {
               handle(var0, var4);
            }
         } else {
            handle(var0, var1);
         }
      }
   }

   public static void tick(ClientPlayerEntity var0) {
      now = var0.field_6012;
      if (var0 != lastPlayer || var0.method_73183() != lastLevel) {
         MARKS.clear();
         margin = 0;
         streak = 0;
         lastPlayer = var0;
         lastLevel = var0.method_73183();
      }

      ObjectIterator var1 = MARKS.int2ObjectEntrySet().iterator();

      while (var1.hasNext()) {
         Entry var3 = (Entry)var1.next();
         Entity var4 = lastLevel.method_8469(var3.getIntKey());
         LivingEntity var2;
         if (var4 instanceof LivingEntity && (var2 = (LivingEntity)var4).method_5805()) {
            HurtClock.Mark var5 = (HurtClock.Mark)var3.getValue();
            if (var5.removedAt != -1000 && !var5.confirmed && now - var5.removedAt > 3) {
               margin = Math.min(2, margin + 1);
               streak = 0;
               var5.shotTick = -1000;
               var5.removedAt = -1000;
            }
         } else {
            var1.remove();
         }
      }
   }

   public static boolean freshAt(LivingEntity var0, int var1, int var2) {
      if (var0.field_6008 - var1 - var2 > 10) {
         return false;
      } else {
         HurtClock.Mark var3 = (HurtClock.Mark)MARKS.get(var0.method_5628());
         return var3 == null || var3.shotTick == -1000 || now + var1 - var3.shotTick >= 10 + margin;
      }
   }

   public static int opensIn(LivingEntity var0, int var1) {
      for (int var2 = 0; var2 < 20; var2++) {
         if (freshAt(var0, var2, var1)) {
            return var2;
         }
      }

      return 20;
   }

   public static float effective(CrystalDamage.Victim var0, float var1, int var2, int var3) {
      LivingEntity var4 = var0.entity();
      if (freshAt(var4, var2, var3)) {
         return var0.reduce(var1);
      } else {
         HurtClock.Mark var5 = (HurtClock.Mark)MARKS.get(var4.method_5628());
         float var6 = var5 == null ? 3.4028235E38F : var5.lastRaw;
         return var1 > var6 ? var0.reduce(var1 - var6) : 0.0F;
      }
   }

   public static void shot(ClientPlayerEntity var0, LivingEntity var1, float var2, Object var3, int var4) {
      HurtClock.Mark var5 = (HurtClock.Mark)MARKS.computeIfAbsent(var1.method_5628(), var0x -> new HurtClock.Mark());
      if (freshAt(var1, 0, var4)) {
         var5.shotTick = var0.field_6012;
         var5.shotRaw = var2;
         var5.lastRaw = var2;
         var5.shotKey = var3;
         var5.confirmed = false;
         var5.removedAt = -1000;
      } else {
         var5.lastRaw = Math.max(var5.lastRaw, var2);
      }
   }

   private static void handle(ClientPlayerEntity var0, Packet<?> var1) {
      if (var1 instanceof EntityDamageS2CPacket var2) {
         damaged(var0, var2);
      } else if (var1 instanceof EntitiesDestroyS2CPacket var3) {
         removed(var3.method_36548());
      }
   }

   private static void damaged(ClientPlayerEntity var0, EntityDamageS2CPacket var1) {
      Entity var3 = var0.method_73183().method_8469(var1.comp_1267());
      PlayerEntity var2;
      if (var3 instanceof PlayerEntity && MeleeAttacks.isEnemy(var0, var2 = (PlayerEntity)var3)) {
         HurtClock.Mark var4 = (HurtClock.Mark)MARKS.computeIfAbsent(var2.method_5628(), var0x -> new HurtClock.Mark());
         if (var4.shotTick != -1000 && !var4.confirmed && ours(var0, var4, var1)) {
            var4.lastRaw = var4.shotRaw;
            var4.confirmed = true;
            if (var4.shotKey instanceof Integer && ++streak >= 8) {
               margin = Math.max(0, margin - 1);
               streak = 0;
            }
         } else {
            RegistryEntry var5 = var1.comp_1268();
            var4.shotTick = -1000;
            var4.removedAt = -1000;
            var4.lastRaw = !var5.method_40225(DamageTypes.field_42320) && !var5.method_40225(DamageTypes.field_42360) ? 3.4028235E38F : 0.0F;
         }
      }
   }

   private static boolean ours(ClientPlayerEntity var0, HurtClock.Mark var1, EntityDamageS2CPacket var2) {
      RegistryEntry var3 = var2.comp_1268();
      if (var3.method_40225(DamageTypes.field_42332)) {
         if (var2.comp_1269() != var0.method_5628()) {
            return false;
         } else {
            return !(var1.shotKey instanceof Integer var7) ? false : var2.comp_1270() == var7;
         }
      } else if (!var3.method_40225(DamageTypes.field_42334)) {
         return false;
      } else {
         return !(var1.shotKey instanceof Vec3d var5) ? false : var2.comp_1271().filter(var1x -> var1x.method_1025(var5) < 0.25).isPresent();
      }
   }

   private static void removed(IntList var0) {
      ObjectIterator var1 = MARKS.values().iterator();

      while (var1.hasNext()) {
         HurtClock.Mark var2 = (HurtClock.Mark)var1.next();
         if (var2.shotTick != -1000 && !var2.confirmed) {
            Object var4 = var2.shotKey;
            Integer var3;
            if (var2.shotKey instanceof Integer && var0.contains(var3 = (Integer)var4)) {
               var2.removedAt = now;
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Mark {
      float lastRaw = 3.4028235E38F;
      int shotTick = -1000;
      float shotRaw;
      Object shotKey;
      boolean confirmed;
      int removedAt = -1000;

      private Mark() {
      }
   }
}
