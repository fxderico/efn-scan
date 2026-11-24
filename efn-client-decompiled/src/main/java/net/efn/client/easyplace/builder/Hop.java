package net.efn.client.easyplace.builder;

import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
final class Hop {
   private static final int TIME_LIMIT = 60;
   private static final double CENTERED = 0.15;
   private static final double STEP = 0.55;
   private static final double LINED_UP = 0.25;
   private static final double LEAP_AT = 0.3;
   private static final double LANDED = 0.55;
   private static final int SAFE_DROP = 3;
   private static final float TURN = 22.0F;
   private static final float WALK_OFF = 30.0F;
   private static final double JUMP_AT = 0.15;
   private final BlockPos spot;
   private final boolean through;
   private final boolean leap;
   private boolean sprint;
   private boolean careful;
   private boolean slow;
   private BlockPos from;
   private double floor = 0.0 / 0.0;
   private boolean centered;
   private int ticks;

   Hop(BlockPos blockPos) {
      this(blockPos, false, false);
   }

   Hop(BlockPos blockPos, boolean bl) {
      this(blockPos, bl, false);
   }

   Hop(BlockPos blockPos, boolean bl, boolean bl2) {
      this.spot = blockPos;
      this.through = bl;
      this.leap = bl2;
   }

   BlockPos spot() {
      return this.spot;
   }

   Hop sprinting() {
      this.sprint = true;
      return this;
   }

   Hop careful() {
      this.careful = true;
      this.sprint = false;
      return this;
   }

   Hop slow() {
      this.slow = true;
      this.sprint = false;
      return this;
   }

   static BlockPos spot(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, Predicate<BlockPos> predicate) {
      if (!clientPlayerEntity.isOnGround()) {
         return null;
      } else {
         BlockPos var3 = clientPlayerEntity.getBlockPos();
         double var4 = Walker.floorOr(clientWorld, var3);
         BlockPos var6 = null;
         double var7 = 0.45;

         for (BlockPos var10 : Walker.steps(clientWorld, var3)) {
            double var11 = Walker.floor(clientWorld, var10) - var4;
            if (var11 > var7 && !predicate.test(var10)) {
               var7 = var11;
               var6 = var10;
            }
         }

         return var6;
      }
   }

   Hop.Result tick(MinecraftClient minecraftClient) {
      float var3 = 0.0F;
      boolean var4 = false;
      ClientPlayerEntity var5 = minecraftClient.player;
      if (var5 == null) {
         return Hop.Result.FAILED;
      } else {
         if (this.from == null) {
            this.from = var5.getBlockPos();
         }

         if (Double.isNaN(this.floor) && minecraftClient.world != null) {
            this.floor = Walker.floorOr(minecraftClient.world, this.spot);
         }

         Vec3d var2 = var5.getEntityPos();
         minecraftClient.options.sneakKey.setPressed(this.careful);
         double var6 = this.spot.getX() + 0.5 - var2.x;
         double var8 = this.spot.getZ() + 0.5 - var2.z;
         double var10 = this.through ? 0.35 : 0.12;
         boolean var12 = Math.abs(var6) < var10 && Math.abs(var8) < var10;
         boolean var13 = this.through || var5.getVelocity().horizontalLengthSquared() < 0.0025;
         boolean var14 = var2.y < this.floor + 0.55;
         if (var12 && var13 && var14 && var5.isOnGround() && var2.y >= this.floor - 0.05) {
            if (!this.through) {
               this.release(minecraftClient);
            }

            return Hop.Result.DONE;
         } else if (++this.ticks > 60) {
            this.release(minecraftClient);
            return Hop.Result.FAILED;
         } else if (minecraftClient.world != null && var5.isOnGround() && this.intoHole(minecraftClient.world, var5, var2)) {
            minecraftClient.options.forwardKey.setPressed(false);
            minecraftClient.options.sprintKey.setPressed(false);
            minecraftClient.options.jumpKey.setPressed(false);
            minecraftClient.options.sneakKey.setPressed(true);
            return Hop.Result.RUNNING;
         } else {
            if (!this.centered) {
               double var15 = this.from.getX() + 0.5 - var2.x;
               double var17 = this.from.getZ() + 0.5 - var2.z;
               double var19 = this.spot.getX() != this.from.getX() ? Math.abs(var17) : Math.abs(var15);
               boolean var21 = this.floor - var2.y <= 0.55 && var19 < 0.25;
               if (!var21 && (!(Math.abs(var15) < 0.15) || !(Math.abs(var17) < 0.15)) && (var5.isOnGround() || this.ticks <= 10)) {
                  var3 = Aim.turn(minecraftClient, (float)Math.toDegrees(Math.atan2(-var15, var17)), var5.getPitch(), 22.0F);
                  minecraftClient.options.forwardKey.setPressed(var3 < 30.0F);
                  minecraftClient.options.jumpKey.setPressed(false);
                  return Hop.Result.RUNNING;
               }

               this.centered = true;
            }

            var3 = Aim.turn(minecraftClient, (float)Math.toDegrees(Math.atan2(-var6, var8)), var5.getPitch(), 22.0F);
            boolean var26 = var5.getVelocity().horizontalLengthSquared() > 4.0E-4;
            minecraftClient.options.sprintKey.setPressed(this.sprint && !var12 && var3 < 10.0F);
            var4 = (!var12 || !var14) && (this.through || Math.hypot(var6, var8) > 0.3 || !var26 || !var14);
            if (this.slow) {
               boolean var16 = var5.isOnGround() && var2.y < this.floor + 0.05 && Math.hypot(var6, var8) < 0.45;
               boolean var27;
               if (var4) {
                  double var18 = var5.getVelocity().horizontalLength();
                  double var20 = var16 ? 0.03 : 0.08;
                  var27 = var18 < var20;
               } else {
                  var27 = false;
               }

               var4 = var27;
            }

            minecraftClient.options.forwardKey.setPressed(var4 && var3 < 30.0F);
            double var25 = (var2.x - (this.from.getX() + 0.5)) * Integer.signum(this.spot.getX() - this.from.getX())
               + (var2.z - (this.from.getZ() + 0.5)) * Integer.signum(this.spot.getZ() - this.from.getZ());
            boolean var7 = minecraftClient.player.horizontalCollision
               || var25 > 0.15
               || this.spot.getX() == this.from.getX() && this.spot.getZ() == this.from.getZ();
            minecraftClient.options
               .jumpKey
               .setPressed(
                  minecraftClient.player.isOnGround() && this.floor - var2.y > 0.55 && var7
                     || this.leap && minecraftClient.player.isOnGround() && var25 > 0.3 && var25 < 1.0
               );
            return Hop.Result.RUNNING;
         }
      }
   }

   private boolean intoHole(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      Vec3d var4 = clientPlayerEntity.getVelocity();
      double var5 = var4.horizontalLength();
      if (var5 < 0.02) {
         return false;
      } else {
         double var7 = var5 * 1.3 + 0.35;
         BlockPos var9 = BlockPos.ofFloored(vec3d.x + var4.x / var5 * var7, vec3d.y + 0.01, vec3d.z + var4.z / var5 * var7);
         BlockPos var10 = clientPlayerEntity.getBlockPos();
         return (var9.getX() != this.spot.getX() || var9.getZ() != this.spot.getZ()) && (var9.getX() != var10.getX() || var9.getZ() != var10.getZ())
            ? Walker.dropAt(clientWorld, var9) > 3
            : false;
      }
   }

   void release(MinecraftClient minecraftClient) {
      minecraftClient.options.forwardKey.setPressed(false);
      minecraftClient.options.jumpKey.setPressed(false);
      minecraftClient.options.sprintKey.setPressed(false);
      if (this.careful) {
         minecraftClient.options.sneakKey.setPressed(false);
      }
   }

   @Environment(EnvType.CLIENT)
   static enum Result {
      RUNNING,
      DONE,
      FAILED;

      private Result() {
      }
   }
}
