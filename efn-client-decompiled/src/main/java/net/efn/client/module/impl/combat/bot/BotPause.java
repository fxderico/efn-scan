package net.efn.client.module.impl.combat.bot;

import java.util.ArrayDeque;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
final class BotPause {
   private static final int TOUCH_TICKS = 30;
   private static final int RESPAWN_TICKS = 30;
   private static final int WORLD_TICKS = 40;
   private static final int SETBACK_TICKS = 10;
   private static final int SETBACK_LIMIT = 6;
   private static final long SETBACK_SPAN_MS = 10000L;
   private static final double SETBACK_MAX_JUMP = 6.0;
   private static final double CURSOR_SLACK = 1.5;
   private final ArrayDeque<Long> setbacks = new ArrayDeque<>();
   private ClientPlayerEntity body;
   private ClientWorld world;
   private double cursorX = 0.0 / 0.0;
   private double cursorY = 0.0 / 0.0;
   private int tick;
   private int touchedUntil;
   private int heldUntil;
   private Vec3d lastPosition;
   private long timeAt;
   private long gameTimeAt = -1L;
   private float tps = 20.0F;

   BotPause() {
   }

   void reset() {
      this.setbacks.clear();
      this.body = null;
      this.world = null;
      this.cursorY = 0.0 / 0.0;
      this.cursorX = 0.0 / 0.0;
      this.heldUntil = 0;
      this.touchedUntil = 0;
      this.tick = 0;
      this.lastPosition = null;
      this.gameTimeAt = -1L;
      this.tps = 20.0F;
   }

   BotPause.State tick(MinecraftClient var1, ClientPlayerEntity var2) {
      this.tick++;
      if (this.body != null && var2 != this.body) {
         this.heldUntil = Math.max(this.heldUntil, this.tick + (var1.field_1687 != this.world ? 40 : 30));
      }

      this.body = var2;
      this.world = var1.field_1687;
      if (this.touched(var1)) {
         this.touchedUntil = this.tick + 30;
      }

      long var3 = System.currentTimeMillis();

      while (!this.setbacks.isEmpty() && var3 - this.setbacks.peekFirst() > 10000L) {
         this.setbacks.pollFirst();
      }

      this.lastPosition = var2.method_73189();
      if (this.setbacks.size() >= 6) {
         return BotPause.State.STOP;
      } else if (this.tick < this.heldUntil) {
         return BotPause.State.HOLD;
      } else {
         return this.tick < this.touchedUntil ? BotPause.State.YIELD : BotPause.State.RUN;
      }
   }

   void moved(Vec3d var1, int var2) {
      if (this.lastPosition != null && var2 >= 60) {
         if (!(var1.method_1022(this.lastPosition) > 6.0)) {
            this.setbacks.add(System.currentTimeMillis());
            this.heldUntil = Math.max(this.heldUntil, this.tick + 10);
         }
      }
   }

   void time(long var1) {
      long var3 = System.currentTimeMillis();
      if (this.gameTimeAt >= 0L && var3 > this.timeAt && var1 > this.gameTimeAt) {
         float var5 = (float)(var1 - this.gameTimeAt) * 1000.0F / (float)(var3 - this.timeAt);
         this.tps = Math.min(20.0F, this.tps + (var5 - this.tps) * 0.5F);
      }

      this.gameTimeAt = var1;
      this.timeAt = var3;
   }

   boolean slowServer() {
      return this.tps < 15.0F;
   }

   int setbacksLately() {
      return this.setbacks.size();
   }

   private boolean touched(MinecraftClient var1) {
      if (var1.field_1755 == null && var1.method_1569()) {
         double var3 = var1.field_1729.method_1603();
         double var5 = var1.field_1729.method_1604();
         boolean var7 = !Double.isNaN(this.cursorX) && (Math.abs(var3 - this.cursorX) > 1.5 || Math.abs(var5 - this.cursorY) > 1.5);
         this.cursorX = var3;
         this.cursorY = var5;
         if (var7) {
            return true;
         } else {
            for (KeyBinding var11 : new KeyBinding[]{
               var1.field_1690.field_1894,
               var1.field_1690.field_1881,
               var1.field_1690.field_1913,
               var1.field_1690.field_1849,
               var1.field_1690.field_1903,
               var1.field_1690.field_1832,
               var1.field_1690.field_1886,
               var1.field_1690.field_1904
            }) {
               if (Crystals.isPhysicallyDown(var1, var11)) {
                  return true;
               }
            }

            for (KeyBinding var15 : var1.field_1690.field_1852) {
               if (Crystals.isPhysicallyDown(var1, var15)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         this.cursorY = 0.0 / 0.0;
         this.cursorX = 0.0 / 0.0;
         return false;
      }
   }

   @Environment(EnvType.CLIENT)
   static enum State {
      RUN,
      YIELD,
      HOLD,
      STOP;

      private State() {
      }
   }
}
