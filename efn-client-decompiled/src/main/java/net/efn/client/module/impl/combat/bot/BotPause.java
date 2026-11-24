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

   BotPause.State tick(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      this.tick++;
      if (this.body != null && clientPlayerEntity != this.body) {
         this.heldUntil = Math.max(this.heldUntil, this.tick + (minecraftClient.world != this.world ? 40 : 30));
      }

      this.body = clientPlayerEntity;
      this.world = minecraftClient.world;
      if (this.touched(minecraftClient)) {
         this.touchedUntil = this.tick + 30;
      }

      long var3 = System.currentTimeMillis();

      while (!this.setbacks.isEmpty() && var3 - this.setbacks.peekFirst() > 10000L) {
         this.setbacks.pollFirst();
      }

      this.lastPosition = clientPlayerEntity.getEntityPos();
      if (this.setbacks.size() >= 6) {
         return BotPause.State.STOP;
      } else if (this.tick < this.heldUntil) {
         return BotPause.State.HOLD;
      } else {
         return this.tick < this.touchedUntil ? BotPause.State.YIELD : BotPause.State.RUN;
      }
   }

   void moved(Vec3d vec3d, int i) {
      if (this.lastPosition != null && i >= 60) {
         if (!(vec3d.distanceTo(this.lastPosition) > 6.0)) {
            this.setbacks.add(System.currentTimeMillis());
            this.heldUntil = Math.max(this.heldUntil, this.tick + 10);
         }
      }
   }

   void time(long l) {
      long var3 = System.currentTimeMillis();
      if (this.gameTimeAt >= 0L && var3 > this.timeAt && l > this.gameTimeAt) {
         float var5 = (float)(l - this.gameTimeAt) * 1000.0F / (float)(var3 - this.timeAt);
         this.tps = Math.min(20.0F, this.tps + (var5 - this.tps) * 0.5F);
      }

      this.gameTimeAt = l;
      this.timeAt = var3;
   }

   boolean slowServer() {
      return this.tps < 15.0F;
   }

   int setbacksLately() {
      return this.setbacks.size();
   }

   private boolean touched(MinecraftClient minecraftClient) {
      if (minecraftClient.currentScreen == null && minecraftClient.isWindowFocused()) {
         double var3 = minecraftClient.mouse.getX();
         double var5 = minecraftClient.mouse.getY();
         boolean var7 = !Double.isNaN(this.cursorX) && (Math.abs(var3 - this.cursorX) > 1.5 || Math.abs(var5 - this.cursorY) > 1.5);
         this.cursorX = var3;
         this.cursorY = var5;
         if (var7) {
            return true;
         } else {
            for (KeyBinding var11 : new KeyBinding[]{
               minecraftClient.options.forwardKey,
               minecraftClient.options.backKey,
               minecraftClient.options.leftKey,
               minecraftClient.options.rightKey,
               minecraftClient.options.jumpKey,
               minecraftClient.options.sneakKey,
               minecraftClient.options.attackKey,
               minecraftClient.options.useKey
            }) {
               if (Crystals.isPhysicallyDown(minecraftClient, var11)) {
                  return true;
               }
            }

            for (KeyBinding var15 : minecraftClient.options.hotbarKeys) {
               if (Crystals.isPhysicallyDown(minecraftClient, var15)) {
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
