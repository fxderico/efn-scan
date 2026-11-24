package net.efn.client.module.impl.combat.crystal;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class LootLatch {
   private static final double RANGE_SQR = 100.0;
   private static final int GRACE_TICKS = 10;
   private static final long ACTIVE_MS = 250L;
   private static final Int2IntOpenHashMap DEAD = new Int2IntOpenHashMap();
   private static ClientPlayerEntity self;
   private static int selfTick = -1;
   private static int clock;
   private static boolean reported;
   private static boolean latched;
   private static boolean killed;
   private static long updatedAt;

   private LootLatch() {
   }

   public static void update(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity != self || clientPlayerEntity.age != selfTick) {
         self = clientPlayerEntity;
         selfTick = clientPlayerEntity.age;
         KeyBinding var2 = minecraftClient.options.useKey;
         evaluate(minecraftClient.world, clientPlayerEntity, Crystals.isPhysicallyDown(minecraftClient, var2), MeleeAttacks.clickPending(var2));
      }
   }

   public static boolean latched() {
      return latched;
   }

   public static boolean killedNow() {
      return killed;
   }

   public static boolean active() {
      return latched && Util.getMeasuringTimeMs() - updatedAt < 250L;
   }

   private static void evaluate(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, boolean bl, boolean bl2) {
      int var4 = ++clock;
      updatedAt = Util.getMeasuringTimeMs();
      DEAD.int2IntEntrySet()
         .removeIf(
            entry -> {
               PlayerEntity var3;
               Entity var4x;
               return var4 - entry.getIntValue() > 10
                  && (!((var4x = clientWorld.getEntityById(entry.getIntKey())) instanceof PlayerEntity) || !(var3 = (PlayerEntity)var4x).isDead());
            }
         );
      boolean var5 = reported;
      reported = false;

      for (PlayerEntity var7 : clientWorld.getPlayers()) {
         if (var7 != clientPlayerEntity && var7.isDead() && !DEAD.containsKey(var7.getId())) {
            DEAD.put(var7.getId(), var4);
            if (var7.squaredDistanceTo(clientPlayerEntity) <= 100.0) {
               var5 = true;
            }
         }
      }

      killed = var5;
      if (var5 && bl) {
         latched = true;
      } else if (!bl || latched && bl2) {
         latched = false;
      }
   }

   private static void onPacket(PacketAppliedEvent packetAppliedEvent) {
      Packet var3 = packetAppliedEvent.packet;
      EntityStatusS2CPacket var2;
      if (var3 instanceof EntityStatusS2CPacket && (var2 = (EntityStatusS2CPacket)var3).getStatus() == 3) {
         MinecraftClient var4 = MinecraftClient.getInstance();
         if (var4.world != null && var4.player != null) {
            Entity var5 = var2.getEntity(var4.world);
            PlayerEntity var1;
            if (var5 instanceof PlayerEntity && (var1 = (PlayerEntity)var5) != var4.player && !DEAD.containsKey(var1.getId())) {
               DEAD.put(var1.getId(), clock);
               if (var1.squaredDistanceTo(var4.player) <= 100.0) {
                  reported = true;
               }
            }
         }
      }
   }

   static {
      PacketAppliedEvent.TYPE.listen(LootLatch.class, LootLatch::onPacket);
   }
}
