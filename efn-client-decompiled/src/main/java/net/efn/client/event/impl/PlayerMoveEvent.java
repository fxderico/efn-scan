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

   public static void move(Vec3d vec3d, Consumer<Vec3d> consumer) {
      if (!TYPE.hasListeners()) {
         consumer.accept(vec3d);
      } else {
         PlayerMoveEvent var2 = TYPE.pool.acquire();
         var2.x = vec3d.x;
         var2.y = vec3d.y;
         var2.z = vec3d.z;
         var2.resetCancellation();

         try {
            TYPE.dispatchPre(var2);
            if (var2.isCancelled()) {
               return;
            }

            if (Double.isFinite(var2.x) && Double.isFinite(var2.y) && Double.isFinite(var2.z)) {
               Vec3d var3 = var2.x == vec3d.x && var2.y == vec3d.y && var2.z == vec3d.z ? vec3d : new Vec3d(var2.x, var2.y, var2.z);
               consumer.accept(var3);
               TYPE.dispatchPost(var2);
               return;
            }
         } finally {
            TYPE.pool.release(var2);
         }
      }
   }
}
