package net.efn.client.util.pvp;

import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class ServerRotation {
   private static Rotation requested;
   private static Rotation committed;
   private static Rotation server;
   private static boolean silent;
   private static boolean steered;
   private static ClientPlayerEntity owner;
   private static float ownYaw;
   private static float ownPitch;
   private static boolean swapped;
   private static Rotation walked;
   private static final float MAX_STEP = 25.0F;
   private static final int EASE_TICKS = 10;
   private static final float EASE_SPEED = 5.0F;
   private static final float EASE_DONE = 0.5F;
   private static final float EASE_GIVE_UP = 150.0F;
   private static float requestedSpeed;
   private static Rotation stepped;
   private static boolean requestedLastTick;
   private static int easeLeft;

   private ServerRotation() {
   }

   public static void request(Rotation rotation) {
      request(rotation, 0.0F);
   }

   public static void request(Rotation rotation, float f) {
      if (requested == null && committed == null) {
         requested = rotation;
         requestedSpeed = f;
      }
   }

   public static boolean pending() {
      return requested != null || committed != null;
   }

   public static Rotation nextStep(ClientPlayerEntity clientPlayerEntity, Rotation rotation) {
      return step(clientPlayerEntity, rotation, 0.0F);
   }

   public static Rotation sent() {
      return server;
   }

   public static boolean desynced() {
      return silent;
   }

   public static boolean canRotate(ClientPlayerEntity clientPlayerEntity) {
      return !clientPlayerEntity.isGliding()
         && !clientPlayerEntity.isUsingRiptide()
         && !clientPlayerEntity.hasVehicle()
         && !clientPlayerEntity.isSwimming()
         && clientPlayerEntity.networkHandler.isLoaded();
   }

   public static Rotation next(ClientPlayerEntity clientPlayerEntity, Rotation rotation) {
      Rotation var2 = server != null ? server : new Rotation(clientPlayerEntity.getYaw(), clientPlayerEntity.getPitch());
      double var3 = RotationUtils.mouseScale(MinecraftClient.getInstance(), clientPlayerEntity);
      float var5 = (float)var3 * 0.15F;
      long var6 = Math.round(MathHelper.wrapDegrees(rotation.yaw() - var2.yaw()) / var5);
      long var8 = Math.round((rotation.pitch() - var2.pitch()) / var5);
      float var10 = var2.yaw() + (float)(var6 * var3) * 0.15F;
      float var11 = MathHelper.clamp(var2.pitch() + (float)(var8 * var3) * 0.15F, -90.0F, 90.0F);
      return new Rotation(var10, var11);
   }

   public static void startTick() {
      ClientPlayerEntity var0 = MinecraftClient.getInstance().player;
      if (var0 != owner) {
         owner = var0;
         server = null;
      }

      requestedLastTick = requested != null;
      requested = null;
      committed = null;
      stepped = null;
      if (requestedLastTick) {
         easeLeft = 10;
      } else if (easeLeft > 0) {
         easeLeft--;
      }

      if (easeLeft > 0 && (var0 == null || server == null || closeToCamera(var0))) {
         easeLeft = 0;
      }

      if (var0 != null && server != null && steered && !(Math.abs(var0.getYaw() - server.yaw()) <= 180.0F)) {
         float var1 = server.yaw() + MathHelper.wrapDegrees(var0.getYaw() - server.yaw()) - var0.getYaw();
         var0.setYaw(var0.getYaw() + var1);
         var0.lastYaw += var1;
         var0.renderYaw += var1;
         var0.lastRenderYaw += var1;
         var0.headYaw += var1;
         var0.lastHeadYaw += var1;
         var0.bodyYaw += var1;
         var0.lastBodyYaw += var1;
      }
   }

   public static void noteSent(Packet<?> packet) {
      PlayerMoveC2SPacket var1;
      if (packet instanceof PlayerMoveC2SPacket && (var1 = (PlayerMoveC2SPacket)packet).changesLook() && MinecraftClient.getInstance().isOnThread()) {
         ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
         server = new Rotation(var1.getYaw(0.0F), var1.getPitch(0.0F));
         silent = swapped;
         steered = swapped || var2 != null && (server.yaw() != var2.getYaw() || server.pitch() != var2.getPitch());
         owner = var2;
      }
   }

   public static Rotation commit(ClientPlayerEntity clientPlayerEntity) {
      if (committed == null) {
         Rotation var1 = canRotate(clientPlayerEntity) ? target(clientPlayerEntity) : null;
         committed = var1 != null ? var1 : new Rotation(clientPlayerEntity.getYaw(), clientPlayerEntity.getPitch());
      }

      return committed;
   }

   public static boolean isSwapped() {
      return swapped;
   }

   public static void applyForMovement(ClientPlayerEntity clientPlayerEntity) {
      if (!swapped && canRotate(clientPlayerEntity)) {
         Rotation var1 = target(clientPlayerEntity);
         if (var1 != null && (var1.yaw() != clientPlayerEntity.getYaw() || var1.pitch() != clientPlayerEntity.getPitch())) {
            ownYaw = clientPlayerEntity.getYaw();
            ownPitch = clientPlayerEntity.getPitch();
            clientPlayerEntity.setYaw(var1.yaw());
            clientPlayerEntity.setPitch(var1.pitch());
            walked = var1;
            swapped = true;
         }
      }
   }

   public static PlayerInput correctInput(PlayerInput playerInput) {
      if (walked == null) {
         return playerInput;
      } else {
         int var1 = impulse(playerInput.forward(), playerInput.backward());
         int var2 = impulse(playerInput.left(), playerInput.right());
         if (var1 == 0 && var2 == 0) {
            return playerInput;
         } else {
            double[] var3 = heading(ownYaw, var2, var1);
            int var4 = var1;
            int var5 = var2;
            double var6 = -1.0 / 0.0;

            for (int var8 = -1; var8 <= 1; var8++) {
               for (int var9 = -1; var9 <= 1; var9++) {
                  double[] var10;
                  double var11;
                  if ((var8 != 0 || var9 != 0) && (var11 = (var10 = heading(walked.yaw(), var9, var8))[0] * var3[0] + var10[1] * var3[1]) > var6) {
                     var6 = var11;
                     var4 = var8;
                     var5 = var9;
                  }
               }
            }

            return var4 == var1 && var5 == var2
               ? playerInput
               : new PlayerInput(var4 > 0, var4 < 0, var5 > 0, var5 < 0, playerInput.jump(), playerInput.sneak(), playerInput.sprint());
         }
      }
   }

   public static boolean misses(ClientPlayerEntity clientPlayerEntity, Box box, double d) {
      if (server == null || !canRotate(clientPlayerEntity)) {
         return false;
      } else if (!pending()) {
         endEase();
         return false;
      } else {
         Vec3d var4 = clientPlayerEntity.getEyePos();
         if (box.contains(var4)) {
            return false;
         } else {
            Rotation var5 = target(clientPlayerEntity);
            if (var5 == null) {
               var5 = new Rotation(clientPlayerEntity.getYaw(), clientPlayerEntity.getPitch());
            }

            return !hits(var4, box, d, var5.yaw(), var5.pitch())
               && !hits(var4, box, d, server.yaw(), var5.pitch())
               && !hits(var4, box, d, server.yaw(), server.pitch());
         }
      }
   }

   public static boolean missesBlock(ClientPlayerEntity clientPlayerEntity, Box box, double d) {
      if (server == null) {
         return false;
      } else {
         if (!pending()) {
            endEase();
         }

         if (!silent) {
            if (!pending() || !canRotate(clientPlayerEntity)) {
               return false;
            }

            Rotation var5 = target(clientPlayerEntity);
            if (var5 == null || var5.yaw() == clientPlayerEntity.getYaw() && var5.pitch() == clientPlayerEntity.getPitch()) {
               return false;
            }
         }

         Vec3d var4;
         return !box.contains(var4 = clientPlayerEntity.getEyePos()) && !hits(var4, box, d, server.yaw(), server.pitch());
      }
   }

   public static void afterSend(ClientPlayerEntity clientPlayerEntity) {
      if (swapped) {
         restore(clientPlayerEntity);
      }
   }

   public static void finishTick(ClientPlayerEntity clientPlayerEntity) {
      if (swapped) {
         restore(clientPlayerEntity);
      }
   }

   private static Rotation target(ClientPlayerEntity clientPlayerEntity) {
      if (committed != null) {
         return committed;
      } else if (stepped != null) {
         return stepped;
      } else {
         if (requested != null) {
            stepped = step(clientPlayerEntity, requested, requestedSpeed);
         } else if (easeLeft > 0 && server != null) {
            stepped = step(clientPlayerEntity, new Rotation(clientPlayerEntity.getYaw(), clientPlayerEntity.getPitch()), 5.0F);
         }

         return stepped;
      }
   }

   private static Rotation step(ClientPlayerEntity clientPlayerEntity, Rotation rotation, float f) {
      Rotation var7 = server != null ? server : new Rotation(clientPlayerEntity.getYaw(), clientPlayerEntity.getPitch());
      double var8 = RotationUtils.mouseScale(MinecraftClient.getInstance(), clientPlayerEntity) * 0.15;
      double var10 = MathHelper.wrapDegrees(rotation.yaw() - var7.yaw());
      double var5;
      double var12 = Math.hypot(var10, var5 = rotation.pitch() - var7.pitch());
      if (var12 < 1.0E-4) {
         return next(clientPlayerEntity, rotation);
      } else {
         double var14 = 1.0;
         if (f > 0.0F) {
            double var16 = Math.min(var12, 0.313 * var12 + 1.56 * f);
            if (var16 < var12) {
               var16 *= 0.85 + 0.15 * ThreadLocalRandom.current().nextDouble();
            }

            var14 = var16 / var12;
         }

         double var3;
         if ((var3 = Math.max(Math.abs(var10), Math.abs(var5)) * var14) > 25.0) {
            var14 *= 25.0 / var3;
         }

         long var22 = Math.max(1L, (long)Math.floor(25.0 / var8));
         long var18 = MathHelper.clamp(Math.round(var10 * var14 / var8), -var22, var22);
         long var20 = MathHelper.clamp(Math.round(var5 * var14 / var8), -var22, var22);
         return new Rotation(var7.yaw() + (float)(var18 * var8), MathHelper.clamp(var7.pitch() + (float)(var20 * var8), -90.0F, 90.0F));
      }
   }

   private static void endEase() {
      if (easeLeft != 0 && committed == null) {
         easeLeft = 0;
         stepped = null;
      }
   }

   private static boolean closeToCamera(ClientPlayerEntity clientPlayerEntity) {
      double var3 = RotationUtils.mouseScale(MinecraftClient.getInstance(), clientPlayerEntity) * 0.15;
      double var5 = Math.abs(MathHelper.wrapDegrees(clientPlayerEntity.getYaw() - server.yaw()));
      double var7 = Math.max(var5, (double)Math.abs(clientPlayerEntity.getPitch() - server.pitch()));
      return var7 <= Math.max(0.5, 2.0 * var3) || var7 > 150.0;
   }

   private static int impulse(boolean bl, boolean bl2) {
      return bl == bl2 ? 0 : (bl ? 1 : -1);
   }

   private static double[] heading(float f, int i, int j) {
      double var3 = MathHelper.sin(f * 0.017453292F);
      double var5 = MathHelper.cos(f * 0.017453292F);
      double var7 = i * var5 - j * var3;
      double var9 = j * var5 + i * var3;
      double var11 = Math.sqrt(var7 * var7 + var9 * var9);
      return new double[]{var7 / var11, var9 / var11};
   }

   private static boolean hits(Vec3d vec3d, Box box, double d, float f, float g) {
      Vec3d var6 = Vec3d.fromPolar(g, f);
      return box.raycast(vec3d, vec3d.add(var6.multiply(d))).isPresent();
   }

   private static void restore(ClientPlayerEntity clientPlayerEntity) {
      clientPlayerEntity.renderYaw = clientPlayerEntity.renderYaw + (ownYaw - walked.yaw()) * 0.5F;
      clientPlayerEntity.renderPitch = clientPlayerEntity.renderPitch + (ownPitch - walked.pitch()) * 0.5F;
      walked = null;
      clientPlayerEntity.setYaw(ownYaw);
      clientPlayerEntity.setPitch(ownPitch);
      swapped = false;
   }
}
