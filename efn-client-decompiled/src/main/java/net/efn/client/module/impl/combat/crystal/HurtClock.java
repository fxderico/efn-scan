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
   private static final Int2ObjectOpenHashMap<HurtClock.Mark> MARKS = new Int2ObjectOpenHashMap<>();
   private static ClientPlayerEntity lastPlayer;
   private static World lastLevel;
   private static Packet<?> lastPacket;
   private static int now;
   private static int margin;
   private static int streak;

   private HurtClock() {
   }

   public static void note(ClientPlayerEntity clientPlayerEntity, Packet<?> packet) {
      if (packet != lastPacket) {
         lastPacket = packet;
         if (packet instanceof BundleS2CPacket var2) {
            for (Packet var4 : var2.getPackets()) {
               handle(clientPlayerEntity, var4);
            }
         } else {
            handle(clientPlayerEntity, packet);
         }
      }
   }

   public static void tick(ClientPlayerEntity clientPlayerEntity) {
      now = clientPlayerEntity.age;
      if (clientPlayerEntity != lastPlayer || clientPlayerEntity.getEntityWorld() != lastLevel) {
         MARKS.clear();
         margin = 0;
         streak = 0;
         lastPlayer = clientPlayerEntity;
         lastLevel = clientPlayerEntity.getEntityWorld();
      }

      ObjectIterator var1 = MARKS.int2ObjectEntrySet().iterator();

      while (var1.hasNext()) {
         Entry var3 = (Entry)var1.next();
         Entity var4 = lastLevel.getEntityById(var3.getIntKey());
         LivingEntity var2;
         if (var4 instanceof LivingEntity && (var2 = (LivingEntity)var4).isAlive()) {
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

   public static boolean freshAt(LivingEntity livingEntity, int i, int j) {
      if (livingEntity.timeUntilRegen - i - j > 10) {
         return false;
      } else {
         HurtClock.Mark var3 = MARKS.get(livingEntity.getId());
         return var3 == null || var3.shotTick == -1000 || now + i - var3.shotTick >= 10 + margin;
      }
   }

   public static int opensIn(LivingEntity livingEntity, int i) {
      for (int var2 = 0; var2 < 20; var2++) {
         if (freshAt(livingEntity, var2, i)) {
            return var2;
         }
      }

      return 20;
   }

   public static float effective(CrystalDamage.Victim victim, float f, int i, int j) {
      LivingEntity var4 = victim.entity();
      if (freshAt(var4, i, j)) {
         return victim.reduce(f);
      } else {
         HurtClock.Mark var5 = MARKS.get(var4.getId());
         float var6 = var5 == null ? 3.4028235E38F : var5.lastRaw;
         return f > var6 ? victim.reduce(f - var6) : 0.0F;
      }
   }

   public static void shot(ClientPlayerEntity clientPlayerEntity, LivingEntity livingEntity, float f, Object object, int i) {
      HurtClock.Mark var5 = MARKS.computeIfAbsent(livingEntity.getId(), ix -> new HurtClock.Mark());
      if (freshAt(livingEntity, 0, i)) {
         var5.shotTick = clientPlayerEntity.age;
         var5.shotRaw = f;
         var5.lastRaw = f;
         var5.shotKey = object;
         var5.confirmed = false;
         var5.removedAt = -1000;
      } else {
         var5.lastRaw = Math.max(var5.lastRaw, f);
      }
   }

   private static void handle(ClientPlayerEntity clientPlayerEntity, Packet<?> packet) {
      if (packet instanceof EntityDamageS2CPacket var2) {
         damaged(clientPlayerEntity, var2);
      } else if (packet instanceof EntitiesDestroyS2CPacket var3) {
         removed(var3.getEntityIds());
      }
   }

   private static void damaged(ClientPlayerEntity clientPlayerEntity, EntityDamageS2CPacket entityDamageS2CPacket) {
      Entity var3 = clientPlayerEntity.getEntityWorld().getEntityById(entityDamageS2CPacket.entityId());
      PlayerEntity var2;
      if (var3 instanceof PlayerEntity && MeleeAttacks.isEnemy(clientPlayerEntity, var2 = (PlayerEntity)var3)) {
         HurtClock.Mark var4 = MARKS.computeIfAbsent(var2.getId(), i -> new HurtClock.Mark());
         if (var4.shotTick != -1000 && !var4.confirmed && ours(clientPlayerEntity, var4, entityDamageS2CPacket)) {
            var4.lastRaw = var4.shotRaw;
            var4.confirmed = true;
            if (var4.shotKey instanceof Integer && ++streak >= 8) {
               margin = Math.max(0, margin - 1);
               streak = 0;
            }
         } else {
            RegistryEntry var5 = entityDamageS2CPacket.sourceType();
            var4.shotTick = -1000;
            var4.removedAt = -1000;
            var4.lastRaw = !var5.matchesKey(DamageTypes.PLAYER_ATTACK) && !var5.matchesKey(DamageTypes.MOB_ATTACK) ? 3.4028235E38F : 0.0F;
         }
      }
   }

   private static boolean ours(ClientPlayerEntity clientPlayerEntity, HurtClock.Mark mark, EntityDamageS2CPacket entityDamageS2CPacket) {
      RegistryEntry var3 = entityDamageS2CPacket.sourceType();
      if (var3.matchesKey(DamageTypes.PLAYER_EXPLOSION)) {
         if (entityDamageS2CPacket.sourceCauseId() != clientPlayerEntity.getId()) {
            return false;
         } else {
            return !(mark.shotKey instanceof Integer var7) ? false : entityDamageS2CPacket.sourceDirectId() == var7;
         }
      } else if (!var3.matchesKey(DamageTypes.BAD_RESPAWN_POINT)) {
         return false;
      } else {
         return !(mark.shotKey instanceof Vec3d var5)
            ? false
            : entityDamageS2CPacket.sourcePosition().filter(vec3d2 -> vec3d2.squaredDistanceTo(var5) < 0.25).isPresent();
      }
   }

   private static void removed(IntList intList) {
      for (HurtClock.Mark var2 : MARKS.values()) {
         if (var2.shotTick != -1000 && !var2.confirmed) {
            Object var4 = var2.shotKey;
            Integer var3;
            if (var2.shotKey instanceof Integer && intList.contains((var3 = (Integer)var4).intValue())) {
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
