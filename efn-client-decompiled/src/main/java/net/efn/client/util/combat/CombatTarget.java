package net.efn.client.util.combat;

import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.event.impl.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class CombatTarget {
   private static final Object OWNER = new Object();
   private static final long MEMORY_CAP_MS = 30000L;
   private static LivingEntity lastHit;
   private static long lastHitAt;

   private CombatTarget() {
   }

   public static void install() {
      PlayerAttackEntityEvent.TYPE.listen(OWNER, Stage.PRE, EventPriority.LOWEST, playerAttackEntityEvent -> {
         if (!playerAttackEntityEvent.isCancelled()) {
            Entity var1 = playerAttackEntityEvent.target;
            if (playerAttackEntityEvent.target instanceof LivingEntity) {
               LivingEntity var2 = (LivingEntity)var1;
               MinecraftClient var3 = MinecraftClient.getInstance();
               if (var3.isOnThread() && var2.getEntityWorld() == var3.world) {
                  lastHit = var2;
                  lastHitAt = Util.getMeasuringTimeMs();
                  return;
               }

               return;
            }
         }
      });
      TickEvent.TYPE.listen(OWNER, tickEvent -> forgetStale());
   }

   private static void forgetStale() {
      LivingEntity var0 = lastHit;
      if (var0 != null) {
         if (!var0.isAlive() || var0.getEntityWorld() != MinecraftClient.getInstance().world || Util.getMeasuringTimeMs() - lastHitAt > 30000L) {
            lastHit = null;
         }
      }
   }

   public static LivingEntity lastHit(long l) {
      LivingEntity var2 = lastHit;
      if (var2 != null && var2.isAlive() && var2.getEntityWorld() == MinecraftClient.getInstance().world) {
         return Util.getMeasuringTimeMs() - lastHitAt > l ? null : var2;
      } else {
         return null;
      }
   }

   public static LivingEntity current(long l) {
      LivingEntity var3 = lastHit(l);
      if (var3 != null) {
         return var3;
      } else {
         Entity var4 = MinecraftClient.getInstance().targetedEntity;
         LivingEntity var2;
         return var4 instanceof LivingEntity && (var2 = (LivingEntity)var4).isAlive() ? var2 : null;
      }
   }
}
