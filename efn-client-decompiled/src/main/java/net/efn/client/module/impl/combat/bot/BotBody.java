package net.efn.client.module.impl.combat.bot;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.efn.client.easyplace.builder.Aim;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
final class BotBody {
   private static final double KEEP_EXTRA = 8.0;
   private static final float FAR_OFF = 40.0F;
   private static final int FLIP_COOLDOWN = 8;
   private static final int DROP_SCAN = 16;
   private PlayerEntity target;
   private boolean grounded;
   private int side = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
   private int sideUntil;
   private int flippedAt = -100;
   private double sideCarry;
   private int tick;

   BotBody() {
   }

   PlayerEntity target() {
      return this.target;
   }

   int tick() {
      return this.tick;
   }

   void forget() {
      this.target = null;
   }

   void hold(PlayerEntity playerEntity) {
      this.target = playerEntity;
   }

   void stayGrounded(boolean bl) {
      this.grounded = bl;
   }

   void release(MinecraftClient minecraftClient) {
      minecraftClient.options.forwardKey.setPressed(false);
      minecraftClient.options.backKey.setPressed(false);
      minecraftClient.options.leftKey.setPressed(false);
      minecraftClient.options.rightKey.setPressed(false);
      minecraftClient.options.jumpKey.setPressed(false);
      minecraftClient.options.sprintKey.setPressed(false);
      minecraftClient.options.useKey.setPressed(false);
   }

   PlayerEntity pick(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, double d) {
      return this.pick(minecraftClient, clientPlayerEntity, d, d + 8.0);
   }

   PlayerEntity pick(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, double d, double e) {
      this.tick++;
      if (this.tick >= this.sideUntil) {
         this.side = -this.side;
         this.sideUntil = this.tick + 10 + ThreadLocalRandom.current().nextInt(13);
      }

      if (this.target != null
         && (
            !this.target.isAlive()
               || this.target.isRemoved()
               || this.target.getEntityWorld() != clientPlayerEntity.getEntityWorld()
               || this.target.distanceTo(clientPlayerEntity) > e
               || !MeleeAttacks.isEnemy(clientPlayerEntity, this.target)
         )) {
         this.target = null;
      }

      if (this.target != null) {
         return this.target;
      } else {
         PlayerEntity var7 = null;

         for (PlayerEntity var9 : minecraftClient.world.getPlayers()) {
            double var10;
            if (var9 != clientPlayerEntity
               && var9.isAlive()
               && !var9.isSpectator()
               && MeleeAttacks.isEnemy(clientPlayerEntity, var9)
               && (var10 = var9.distanceTo(clientPlayerEntity)) <= d
               && (var7 == null || var10 < var7.distanceTo(clientPlayerEntity))) {
               var7 = var9;
            }
         }

         this.target = var7;
         return var7;
      }
   }

   void flipSide() {
      if (this.tick - this.flippedAt >= 8) {
         this.flippedAt = this.tick;
         this.side = -this.side;
         this.sideUntil = this.tick + 10 + ThreadLocalRandom.current().nextInt(13);
      }
   }

   void face(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, float f) {
      Vec3d var5 = clientPlayerEntity.getEyePos();
      Vec3d var6 = playerEntity.getEntityPos().subtract(playerEntity.lastX, playerEntity.lastY, playerEntity.lastZ);
      Box var7 = playerEntity.getBoundingBox().offset(var6);
      Box var8 = var7.contract(var7.getLengthX() * 0.2, var7.getLengthY() * 0.15, var7.getLengthZ() * 0.2);
      Rotation var9 = RotationUtils.fromTo(var5, TargetingUtils.closestPoint(var5, var8));
      float var10 = Math.abs(MathHelper.wrapDegrees(var9.yaw() - clientPlayerEntity.getYaw()));
      Aim.turn(minecraftClient, var9.yaw(), var9.pitch(), (var10 > 40.0F ? 27.0F : 22.0F) * f);
   }

   void drive(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, int i, double d, boolean bl, boolean bl2) {
      float var9 = clientPlayerEntity.getYaw();
      Vec3d var10 = Vec3d.fromPolar(0.0F, var9);
      if (i > 0 && safeStep(clientPlayerEntity, var10)) {
         minecraftClient.options.forwardKey.setPressed(true);
         minecraftClient.options.sprintKey.setPressed(bl);
         if (clientPlayerEntity.horizontalCollision && clientPlayerEntity.isOnGround()) {
            bl2 = true;
         }
      } else if (i < 0 && safeStep(clientPlayerEntity, var10.multiply(-1.0))) {
         minecraftClient.options.backKey.setPressed(true);
      }

      if (bl2 && !this.grounded && clientPlayerEntity.isOnGround() && !clientPlayerEntity.isTouchingWater()) {
         minecraftClient.options.jumpKey.setPressed(true);
      }

      this.sideCarry = this.sideCarry + Math.max(0.0, Math.min(1.0, d));
      if (!(this.sideCarry < 1.0)) {
         this.sideCarry--;
         Vec3d var11 = Vec3d.fromPolar(0.0F, var9 - 90.0F);
         Vec3d var8 = this.side > 0 ? var11 : var11.multiply(-1.0);
         if (!safeStep(clientPlayerEntity, var8)) {
            this.side = -this.side;
            if (!safeStep(clientPlayerEntity, var8.multiply(-1.0))) {
               return;
            }
         }

         (this.side > 0 ? minecraftClient.options.leftKey : minecraftClient.options.rightKey).setPressed(true);
      }
   }

   void keepDistance(
      MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, double d, double e, double f, boolean bl
   ) {
      double var11 = Math.hypot(playerEntity.getX() - clientPlayerEntity.getX(), playerEntity.getZ() - clientPlayerEntity.getZ());
      int var13 = var11 > e + 0.25 ? 1 : (var11 < d ? -1 : 0);
      this.drive(minecraftClient, clientPlayerEntity, var13, bl && var11 <= f + 1.0 ? 1.0 : 0.0, var11 > f, false);
   }

   void flee(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      float var4 = RotationUtils.fromTo(playerEntity.getEyePos(), clientPlayerEntity.getEyePos()).yaw();
      Aim.turn(minecraftClient, var4, 5.0F, 27.0F);
      this.drive(minecraftClient, clientPlayerEntity, 1, 0.0, true, false);
   }

   void away(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      Vec3d var4 = clientPlayerEntity.getEntityPos().subtract(vec3d).multiply(1.0, 0.0, 1.0);
      if (var4.lengthSquared() < 1.0E-6) {
         var4 = Vec3d.fromPolar(0.0F, clientPlayerEntity.getYaw()).multiply(-1.0);
      }

      this.go(minecraftClient, clientPlayerEntity, var4);
   }

   boolean toward(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      Vec3d var4 = vec3d.subtract(clientPlayerEntity.getEntityPos()).multiply(1.0, 0.0, 1.0);
      if (var4.lengthSquared() < 0.09) {
         return true;
      } else {
         this.go(minecraftClient, clientPlayerEntity, var4);
         if (!this.grounded && clientPlayerEntity.horizontalCollision && clientPlayerEntity.isOnGround()) {
            minecraftClient.options.jumpKey.setPressed(true);
         }

         return false;
      }
   }

   void dropToward(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      Vec3d var4 = vec3d.subtract(clientPlayerEntity.getEntityPos()).multiply(1.0, 0.0, 1.0);
      if (!(var4.lengthSquared() < 0.09) && safeStep(clientPlayerEntity, var4.normalize(), 16)) {
         keys(minecraftClient, clientPlayerEntity, var4);
      }
   }

   private void go(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      if (safeStep(clientPlayerEntity, vec3d.normalize())) {
         keys(minecraftClient, clientPlayerEntity, vec3d);
      }
   }

   private static void keys(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      float var3 = (float)Math.toDegrees(Math.atan2(-vec3d.x, vec3d.z));
      float var4 = MathHelper.wrapDegrees(var3 - clientPlayerEntity.getYaw());
      float var5 = Math.abs(var4);
      if (var5 <= 67.5F) {
         minecraftClient.options.forwardKey.setPressed(true);
      }

      if (var5 >= 112.5F) {
         minecraftClient.options.backKey.setPressed(true);
      }

      if (var4 <= -22.5F && var4 >= -157.5F) {
         minecraftClient.options.leftKey.setPressed(true);
      }

      if (var4 >= 22.5F && var4 <= 157.5F) {
         minecraftClient.options.rightKey.setPressed(true);
      }
   }

   void backOff(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      this.drive(minecraftClient, clientPlayerEntity, -1, 0.0, false, false);
   }

   boolean use(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, Predicate<ItemStack> predicate) {
      int var4 = hotbarSlot(clientPlayerEntity, predicate);
      if (var4 < 0) {
         return false;
      } else {
         PlayerInventory var5 = clientPlayerEntity.getInventory();
         if (var5.getSelectedSlot() != var4) {
            var5.setSelectedSlot(var4);
            return true;
         } else {
            minecraftClient.options.useKey.setPressed(true);
            return true;
         }
      }
   }

   static int hotbarSlot(ClientPlayerEntity clientPlayerEntity, Predicate<ItemStack> predicate) {
      PlayerInventory var2 = clientPlayerEntity.getInventory();
      if (predicate.test(var2.getStack(var2.getSelectedSlot()))) {
         return var2.getSelectedSlot();
      } else {
         for (int var3 = 0; var3 < 9; var3++) {
            if (predicate.test(var2.getStack(var3))) {
               return var3;
            }
         }

         return -1;
      }
   }

   static boolean safeStep(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
      return safeStep(clientPlayerEntity, vec3d, 3);
   }

   private static boolean safeStep(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, int i) {
      World var3 = clientPlayerEntity.getEntityWorld();
      BlockPos var4 = BlockPos.ofFloored(clientPlayerEntity.getX() + vec3d.x * 0.8, clientPlayerEntity.getY() + 0.1, clientPlayerEntity.getZ() + vec3d.z * 0.8);

      for (int var5 = 0; var5 <= i; var5++) {
         BlockPos var6 = var4.down(var5);
         BlockState var7 = var3.getBlockState(var6);
         if (var7.isOf(Blocks.LAVA)
            || var7.isIn(BlockTags.FIRE)
            || var7.isOf(Blocks.CACTUS)
            || var7.isOf(Blocks.SWEET_BERRY_BUSH)
            || var7.isOf(Blocks.COBWEB)
            || var5 <= 1 && var7.isOf(Blocks.MAGMA_BLOCK)) {
            return false;
         }

         if (!var7.getCollisionShape(var3, var6).isEmpty()) {
            return true;
         }
      }

      return false;
   }
}
