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

   public static void request(Rotation var0) {
      request(var0, 0.0F);
   }

   public static void request(Rotation var0, float var1) {
      if (requested == null && committed == null) {
         requested = var0;
         requestedSpeed = var1;
      }
   }

   public static boolean pending() {
      return requested != null || committed != null;
   }

   public static Rotation nextStep(ClientPlayerEntity var0, Rotation var1) {
      return step(var0, var1, 0.0F);
   }

   public static Rotation sent() {
      return server;
   }

   public static boolean desynced() {
      return silent;
   }

   public static boolean canRotate(ClientPlayerEntity var0) {
      return !var0.method_6128() && !var0.method_6123() && !var0.method_5765() && !var0.method_5681() && var0.field_3944.method_76760();
   }

   public static Rotation next(ClientPlayerEntity var0, Rotation var1) {
      Rotation var2 = server != null ? server : new Rotation(var0.method_36454(), var0.method_36455());
      double var3 = RotationUtils.mouseScale(MinecraftClient.method_1551(), var0);
      float var5 = (float)var3 * 0.15F;
      long var6 = Math.round(MathHelper.method_15393(var1.yaw() - var2.yaw()) / var5);
      long var8 = Math.round((var1.pitch() - var2.pitch()) / var5);
      float var10 = var2.yaw() + (float)(var6 * var3) * 0.15F;
      float var11 = MathHelper.method_15363(var2.pitch() + (float)(var8 * var3) * 0.15F, -90.0F, 90.0F);
      return new Rotation(var10, var11);
   }

   public static void startTick() {
      ClientPlayerEntity var0 = MinecraftClient.method_1551().field_1724;
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

      if (var0 != null && server != null && steered && !(Math.abs(var0.method_36454() - server.yaw()) <= 180.0F)) {
         float var1 = server.yaw() + MathHelper.method_15393(var0.method_36454() - server.yaw()) - var0.method_36454();
         var0.method_36456(var0.method_36454() + var1);
         var0.field_5982 += var1;
         var0.field_3932 += var1;
         var0.field_3931 += var1;
         var0.field_6241 += var1;
         var0.field_6259 += var1;
         var0.field_6283 += var1;
         var0.field_6220 += var1;
      }
   }

   public static void noteSent(Packet<?> var0) {
      PlayerMoveC2SPacket var1;
      if (var0 instanceof PlayerMoveC2SPacket && (var1 = (PlayerMoveC2SPacket)var0).method_36172() && MinecraftClient.method_1551().method_18854()) {
         ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
         server = new Rotation(var1.method_12271(0.0F), var1.method_12270(0.0F));
         silent = swapped;
         steered = swapped || var2 != null && (server.yaw() != var2.method_36454() || server.pitch() != var2.method_36455());
         owner = var2;
      }
   }

   public static Rotation commit(ClientPlayerEntity var0) {
      if (committed == null) {
         Rotation var1 = canRotate(var0) ? target(var0) : null;
         committed = var1 != null ? var1 : new Rotation(var0.method_36454(), var0.method_36455());
      }

      return committed;
   }

   public static boolean isSwapped() {
      return swapped;
   }

   public static void applyForMovement(ClientPlayerEntity var0) {
      if (!swapped && canRotate(var0)) {
         Rotation var1 = target(var0);
         if (var1 != null && (var1.yaw() != var0.method_36454() || var1.pitch() != var0.method_36455())) {
            ownYaw = var0.method_36454();
            ownPitch = var0.method_36455();
            var0.method_36456(var1.yaw());
            var0.method_36457(var1.pitch());
            walked = var1;
            swapped = true;
         }
      }
   }

   public static PlayerInput correctInput(PlayerInput var0) {
      if (walked == null) {
         return var0;
      } else {
         int var1 = impulse(var0.comp_3159(), var0.comp_3160());
         int var2 = impulse(var0.comp_3161(), var0.comp_3162());
         if (var1 == 0 && var2 == 0) {
            return var0;
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
               ? var0
               : new PlayerInput(var4 > 0, var4 < 0, var5 > 0, var5 < 0, var0.comp_3163(), var0.comp_3164(), var0.comp_3165());
         }
      }
   }

   public static boolean misses(ClientPlayerEntity var0, Box var1, double var2) {
      if (server == null || !canRotate(var0)) {
         return false;
      } else if (!pending()) {
         endEase();
         return false;
      } else {
         Vec3d var4 = var0.method_33571();
         if (var1.method_1006(var4)) {
            return false;
         } else {
            Rotation var5 = target(var0);
            if (var5 == null) {
               var5 = new Rotation(var0.method_36454(), var0.method_36455());
            }

            return !hits(var4, var1, var2, var5.yaw(), var5.pitch())
               && !hits(var4, var1, var2, server.yaw(), var5.pitch())
               && !hits(var4, var1, var2, server.yaw(), server.pitch());
         }
      }
   }

   public static boolean missesBlock(ClientPlayerEntity var0, Box var1, double var2) {
      if (server == null) {
         return false;
      } else {
         if (!pending()) {
            endEase();
         }

         if (!silent) {
            if (!pending() || !canRotate(var0)) {
               return false;
            }

            Rotation var5 = target(var0);
            if (var5 == null || var5.yaw() == var0.method_36454() && var5.pitch() == var0.method_36455()) {
               return false;
            }
         }

         Vec3d var4;
         return !var1.method_1006(var4 = var0.method_33571()) && !hits(var4, var1, var2, server.yaw(), server.pitch());
      }
   }

   public static void afterSend(ClientPlayerEntity var0) {
      if (swapped) {
         restore(var0);
      }
   }

   public static void finishTick(ClientPlayerEntity var0) {
      if (swapped) {
         restore(var0);
      }
   }

   private static Rotation target(ClientPlayerEntity var0) {
      if (committed != null) {
         return committed;
      } else if (stepped != null) {
         return stepped;
      } else {
         if (requested != null) {
            stepped = step(var0, requested, requestedSpeed);
         } else if (easeLeft > 0 && server != null) {
            stepped = step(var0, new Rotation(var0.method_36454(), var0.method_36455()), 5.0F);
         }

         return stepped;
      }
   }

   private static Rotation step(ClientPlayerEntity var0, Rotation var1, float var2) {
      Rotation var7 = server != null ? server : new Rotation(var0.method_36454(), var0.method_36455());
      double var8 = RotationUtils.mouseScale(MinecraftClient.method_1551(), var0) * 0.15;
      double var10 = MathHelper.method_15393(var1.yaw() - var7.yaw());
      double var5;
      double var12 = Math.hypot(var10, var5 = var1.pitch() - var7.pitch());
      if (var12 < 1.0E-4) {
         return next(var0, var1);
      } else {
         double var14 = 1.0;
         if (var2 > 0.0F) {
            double var16 = Math.min(var12, 0.313 * var12 + 1.56 * var2);
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
         long var18 = MathHelper.method_53062(Math.round(var10 * var14 / var8), -var22, var22);
         long var20 = MathHelper.method_53062(Math.round(var5 * var14 / var8), -var22, var22);
         return new Rotation(var7.yaw() + (float)(var18 * var8), MathHelper.method_15363(var7.pitch() + (float)(var20 * var8), -90.0F, 90.0F));
      }
   }

   private static void endEase() {
      if (easeLeft != 0 && committed == null) {
         easeLeft = 0;
         stepped = null;
      }
   }

   private static boolean closeToCamera(ClientPlayerEntity var0) {
      double var3 = RotationUtils.mouseScale(MinecraftClient.method_1551(), var0) * 0.15;
      double var5 = Math.abs(MathHelper.method_15393(var0.method_36454() - server.yaw()));
      double var7 = Math.max(var5, (double)Math.abs(var0.method_36455() - server.pitch()));
      return var7 <= Math.max(0.5, 2.0 * var3) || var7 > 150.0;
   }

   private static int impulse(boolean var0, boolean var1) {
      return var0 == var1 ? 0 : (var0 ? 1 : -1);
   }

   private static double[] heading(float var0, int var1, int var2) {
      double var3 = MathHelper.method_15374(var0 * 0.017453292F);
      double var5 = MathHelper.method_15362(var0 * 0.017453292F);
      double var7 = var1 * var5 - var2 * var3;
      double var9 = var2 * var5 + var1 * var3;
      double var11 = Math.sqrt(var7 * var7 + var9 * var9);
      return new double[]{var7 / var11, var9 / var11};
   }

   private static boolean hits(Vec3d var0, Box var1, double var2, float var4, float var5) {
      Vec3d var6 = Vec3d.method_1030(var5, var4);
      return var1.method_992(var0, var0.method_1019(var6.method_1021(var2))).isPresent();
   }

   private static void restore(ClientPlayerEntity var0) {
      var0.field_3932 = var0.field_3932 + (ownYaw - walked.yaw()) * 0.5F;
      var0.field_3916 = var0.field_3916 + (ownPitch - walked.pitch()) * 0.5F;
      walked = null;
      var0.method_36456(ownYaw);
      var0.method_36457(ownPitch);
      swapped = false;
   }
}
