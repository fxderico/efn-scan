package net.efn.client.event.impl;

import net.efn.client.event.Event;
import net.efn.client.event.EventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Frustum;

@Environment(EnvType.CLIENT)
public final class Render3DEvent extends Event {
   public static final EventType<Render3DEvent> TYPE = new EventType<>("Render3DEvent");
   public float partialTick;
   public Frustum frustum;
   private static final Render3DEvent INSTANCE = new Render3DEvent();
   private static boolean rendering;
   private static long frame;
   private static long lastDurationNanos;

   private Render3DEvent() {
   }

   public static void dispatch(float var0) {
      dispatch(var0, null);
   }

   public static void dispatch(float var0, Frustum var1) {
      if (!TYPE.hasListeners()) {
         lastDurationNanos = 0L;
      } else if (rendering) {
         throw new IllegalStateException("Render3DEvent cannot be nested");
      } else {
         rendering = true;
         long var2 = System.nanoTime();
         INSTANCE.partialTick = var0;
         INSTANCE.frustum = var1;

         try {
            TYPE.dispatchPre(INSTANCE);
            TYPE.dispatchPost(INSTANCE);
         } finally {
            INSTANCE.frustum = null;
            lastDurationNanos = System.nanoTime() - var2;
            frame++;
            rendering = false;
         }
      }
   }

   public static long frame() {
      return frame;
   }

   public static long lastDurationNanos() {
      return lastDurationNanos;
   }
}
