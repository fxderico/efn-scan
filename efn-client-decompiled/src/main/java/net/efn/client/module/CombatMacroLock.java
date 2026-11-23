package net.efn.client.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CombatMacroLock {
   private static Object holder;

   private CombatMacroLock() {
   }

   public static boolean tryAcquire(Object var0) {
      Object var2 = holder;
      ClientModule var1;
      if (var2 instanceof ClientModule && !(var1 = (ClientModule)var2).isEnabled()) {
         holder = null;
      }

      if (holder != null && holder != var0) {
         return false;
      } else {
         holder = var0;
         return true;
      }
   }

   public static void take(Object var0) {
      holder = var0;
   }

   public static void release(Object var0) {
      if (holder == var0) {
         holder = null;
      }
   }

   public static boolean heldByOther(Object var0) {
      Object var2 = holder;
      ClientModule var1;
      if (var2 instanceof ClientModule && !(var1 = (ClientModule)var2).isEnabled()) {
         holder = null;
      }

      return holder != null && holder != var0;
   }
}
