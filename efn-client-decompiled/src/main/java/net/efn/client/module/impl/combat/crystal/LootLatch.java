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

   public static void update(MinecraftClient var0, ClientPlayerEntity var1) {
      if (var1 != self || var1.field_6012 != selfTick) {
         self = var1;
         selfTick = var1.field_6012;
         KeyBinding var2 = var0.field_1690.field_1904;
         evaluate(var0.field_1687, var1, Crystals.isPhysicallyDown(var0, var2), MeleeAttacks.clickPending(var2));
      }
   }

   public static boolean latched() {
      return latched;
   }

   public static boolean killedNow() {
      return killed;
   }

   public static boolean active() {
      return latched && Util.method_658() - updatedAt < 250L;
   }

   private static void evaluate(ClientWorld var0, ClientPlayerEntity var1, boolean var2, boolean var3) {
      int var4 = ++clock;
      updatedAt = Util.method_658();
      DEAD.int2IntEntrySet()
         .removeIf(
            var2x -> {
               PlayerEntity var3x;
               Entity var4x;
               return var4 - var2x.getIntValue() > 10
                  && (!((var4x = var0.method_8469(var2x.getIntKey())) instanceof PlayerEntity) || !(var3x = (PlayerEntity)var4x).method_29504());
            }
         );
      boolean var5 = reported;
      reported = false;

      for (PlayerEntity var7 : var0.method_18456()) {
         if (var7 != var1 && var7.method_29504() && !DEAD.containsKey(var7.method_5628())) {
            DEAD.put(var7.method_5628(), var4);
            if (var7.method_5858(var1) <= 100.0) {
               var5 = true;
            }
         }
      }

      killed = var5;
      if (var5 && var2) {
         latched = true;
      } else if (!var2 || latched && var3) {
         latched = false;
      }
   }

   private static void onPacket(PacketAppliedEvent var0) {
      Packet var3 = var0.packet;
      EntityStatusS2CPacket var2;
      if (var3 instanceof EntityStatusS2CPacket && (var2 = (EntityStatusS2CPacket)var3).method_11470() == 3) {
         MinecraftClient var4 = MinecraftClient.method_1551();
         if (var4.field_1687 != null && var4.field_1724 != null) {
            Entity var5 = var2.method_11469(var4.field_1687);
            PlayerEntity var1;
            if (var5 instanceof PlayerEntity && (var1 = (PlayerEntity)var5) != var4.field_1724 && !DEAD.containsKey(var1.method_5628())) {
               DEAD.put(var1.method_5628(), clock);
               if (var1.method_5858(var4.field_1724) <= 100.0) {
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
