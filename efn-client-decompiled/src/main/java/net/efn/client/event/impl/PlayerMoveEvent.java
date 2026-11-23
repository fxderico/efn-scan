package net.efn.client.event.impl;

import java.util.function.Consumer;
import net.efn.client.event.CancellableEvent;
import net.efn.client.event.PooledCancellableEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class PlayerMoveEvent extends CancellableEvent {
   public static final PooledCancellableEventType<PlayerMoveEvent> TYPE = new PooledCancellableEventType<>("PlayerMoveEvent", PlayerMoveEvent::new, 16);
   public double x;
   public double y;
   public double z;

   private PlayerMoveEvent() {
   }

   public static void move(Vec3d var0, Consumer<Vec3d> var1) {
      if (!TYPE.hasListeners()) {
         var1.accept(var0);
      } else {
         PlayerMoveEvent var2 = TYPE.pool.acquire();
         var2.x = var0.field_1352;
         var2.y = var0.field_1351;
         var2.z = var0.field_1350;
         var2.resetCancellation();

         try {
            TYPE.dispatchPre(var2);
            if (var2.isCancelled()) {
               return;
            }

            if (Double.isFinite(var2.x) && Double.isFinite(var2.y) && Double.isFinite(var2.z)) {
               Vec3d var3 = var2.x == var0.field_1352 && var2.y == var0.field_1351 && var2.z == var0.field_1350 ? var0 : new Vec3d(var2.x, var2.y, var2.z);
               var1.accept(var3);
               TYPE.dispatchPost(var2);
               return;
            }
         } finally {
            TYPE.pool.release(var2);
         }
      }
   }
}
