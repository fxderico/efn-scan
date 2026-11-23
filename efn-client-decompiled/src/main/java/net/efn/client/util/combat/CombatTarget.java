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
      PlayerAttackEntityEvent.TYPE.listen(OWNER, Stage.PRE, EventPriority.LOWEST, var0 -> {
         if (!var0.isCancelled()) {
            Entity var1 = var0.target;
            if (var0.target instanceof LivingEntity) {
               LivingEntity var2 = (LivingEntity)var1;
               MinecraftClient var3 = MinecraftClient.method_1551();
               if (var3.method_18854() && var2.method_73183() == var3.field_1687) {
                  lastHit = var2;
                  lastHitAt = Util.method_658();
                  return;
               }

               return;
            }
         }
      });
      TickEvent.TYPE.listen(OWNER, var0 -> forgetStale());
   }

   private static void forgetStale() {
      LivingEntity var0 = lastHit;
      if (var0 != null) {
         if (!var0.method_5805() || var0.method_73183() != MinecraftClient.method_1551().field_1687 || Util.method_658() - lastHitAt > 30000L) {
            lastHit = null;
         }
      }
   }

   public static LivingEntity lastHit(long var0) {
      LivingEntity var2 = lastHit;
      if (var2 != null && var2.method_5805() && var2.method_73183() == MinecraftClient.method_1551().field_1687) {
         return Util.method_658() - lastHitAt > var0 ? null : var2;
      } else {
         return null;
      }
   }

   public static LivingEntity current(long var0) {
      LivingEntity var3 = lastHit(var0);
      if (var3 != null) {
         return var3;
      } else {
         Entity var4 = MinecraftClient.method_1551().field_1692;
         LivingEntity var2;
         return var4 instanceof LivingEntity && (var2 = (LivingEntity)var4).method_5805() ? var2 : null;
      }
   }
}
