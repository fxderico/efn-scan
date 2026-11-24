package net.efn.client.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CombatMacroLock {
   private static Object holder;

   private CombatMacroLock() {
   }

   public static boolean tryAcquire(Object object) {
      Object var2 = holder;
      ClientModule var1;
      if (var2 instanceof ClientModule && !(var1 = (ClientModule)var2).isEnabled()) {
         holder = null;
      }

      if (holder != null && holder != object) {
         return false;
      } else {
         holder = object;
         return true;
      }
   }

   public static void take(Object object) {
      holder = object;
   }

   public static void release(Object object) {
      if (holder == object) {
         holder = null;
      }
   }

   public static boolean heldByOther(Object object) {
      Object var2 = holder;
      ClientModule var1;
      if (var2 instanceof ClientModule && !(var1 = (ClientModule)var2).isEnabled()) {
         holder = null;
      }

      return holder != null && holder != object;
   }
}
