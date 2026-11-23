package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Motion {
   private static final float DEG = 0.017453292F;
   private static final float TAU = 6.2831855F;
   private static final float STRIDE = 0.6662F;
   float restPitch;
   float restYaw;
   float restRoll;
   float idlePitch;
   float idleYaw;
   float idleRoll;
   float idleRate = 0.4F;
   float idlePhase;
   float walkPitch;
   float walkYaw;
   float walkRoll;
   float walkRate = 1.0F;
   float walkPhase;
   float drag;
   float hang;
   float turnRoll;
   float turnYaw;
   Bone follow;
   float followAmount;
   float followLimit;
   float stiffness;
   float damping;
   float bend;
   float limit = 80.0F;

   public Motion() {
   }

   public static Motion none() {
      return new Motion();
   }

   public static Motion hair() {
      return new Motion().idle(2.5F, 0.0F, 2.0F, 0.35F).walk(4.0F, 0.0F, 3.0F, 2.0F).drag(16.0F).hang(0.75F).turn(12.0F, 0.0F).spring(55.0F, 7.0F).bend(1.3F);
   }

   public static Motion tail() {
      return new Motion().idle(2.0F, 10.0F, 0.0F, 0.45F).walk(3.0F, 9.0F, 0.0F, 1.0F).drag(8.0F).hang(0.25F).turn(0.0F, 18.0F).spring(35.0F, 5.0F).bend(1.2F);
   }

   public static Motion skirt(Bone var0) {
      return new Motion().follow(var0, 0.45F).drag(4.0F).spring(90.0F, 12.0F).bend(1.0F);
   }

   public static Motion cloth() {
      return new Motion().idle(1.5F, 0.0F, 1.5F, 0.3F).walk(5.0F, 0.0F, 2.0F, 2.0F).drag(10.0F).hang(0.6F).turn(8.0F, 0.0F).spring(45.0F, 6.0F).bend(1.4F);
   }

   public Motion rest(float var1, float var2, float var3) {
      this.restPitch = var1;
      this.restYaw = var2;
      this.restRoll = var3;
      return this;
   }

   public Motion idle(float var1, float var2, float var3, float var4) {
      this.idlePitch = var1;
      this.idleYaw = var2;
      this.idleRoll = var3;
      this.idleRate = var4;
      return this;
   }

   public Motion idlePhase(float var1) {
      this.idlePhase = var1 * 0.017453292F;
      return this;
   }

   public Motion walk(float var1, float var2, float var3, float var4) {
      this.walkPitch = var1;
      this.walkYaw = var2;
      this.walkRoll = var3;
      this.walkRate = var4;
      return this;
   }

   public Motion walkPhase(float var1) {
      this.walkPhase = var1 * 0.017453292F;
      return this;
   }

   public Motion drag(float var1) {
      this.drag = var1;
      return this;
   }

   public Motion hang(float var1) {
      this.hang = var1;
      return this;
   }

   public Motion turn(float var1, float var2) {
      this.turnRoll = var1;
      this.turnYaw = var2;
      return this;
   }

   public Motion follow(Bone var1, float var2) {
      return this.follow(var1, var2, 0.0F);
   }

   public Motion follow(Bone var1, float var2, float var3) {
      this.follow = var1;
      this.followAmount = var2;
      this.followLimit = Math.max(0.0F, var3);
      return this;
   }

   public Motion spring(float var1, float var2) {
      this.stiffness = Math.max(0.0F, var1);
      this.damping = Math.max(0.0F, var2);
      return this;
   }

   public Motion bend(float var1) {
      this.bend = Math.max(0.0F, var1);
      return this;
   }

   public Motion limit(float var1) {
      this.limit = Math.abs(var1);
      return this;
   }

   public Motion mirrored() {
      Motion var1 = this.copy();
      var1.restYaw = -this.restYaw;
      var1.restRoll = -this.restRoll;
      var1.follow = this.follow == null ? null : this.follow.opposite();
      return var1;
   }

   public Motion copy() {
      Motion var1 = new Motion();
      var1.restPitch = this.restPitch;
      var1.restYaw = this.restYaw;
      var1.restRoll = this.restRoll;
      var1.idlePitch = this.idlePitch;
      var1.idleYaw = this.idleYaw;
      var1.idleRoll = this.idleRoll;
      var1.idleRate = this.idleRate;
      var1.idlePhase = this.idlePhase;
      var1.walkPitch = this.walkPitch;
      var1.walkYaw = this.walkYaw;
      var1.walkRoll = this.walkRoll;
      var1.walkRate = this.walkRate;
      var1.walkPhase = this.walkPhase;
      var1.drag = this.drag;
      var1.hang = this.hang;
      var1.turnRoll = this.turnRoll;
      var1.turnYaw = this.turnYaw;
      var1.follow = this.follow;
      var1.followAmount = this.followAmount;
      var1.followLimit = this.followLimit;
      var1.stiffness = this.stiffness;
      var1.damping = this.damping;
      var1.bend = this.bend;
      var1.limit = this.limit;
      return var1;
   }

   public float bendPower() {
      return this.bend;
   }

   public void target(Motion.Input var1, Bone var2, float[] var3) {
      float var4 = Math.min(1.0F, Math.max(0.0F, var1.walkSpeed));
      float var5 = (float)Math.sin(var1.time * this.idleRate * 6.2831855F + this.idlePhase);
      float var6 = (float)Math.sin(var1.walkPos * 0.6662F * this.walkRate + this.walkPhase) * var4;
      float var7 = Math.max(-1.5F, Math.min(1.5F, var1.turnRate / 180.0F));
      int var8 = var2.ordinal();
      float var9 = (this.restPitch + this.idlePitch * var5 + this.walkPitch * var6 + this.drag * var4) * 0.017453292F - this.hang * var1.pitch[var8];
      float var10 = (this.restYaw + this.idleYaw * var5 + this.walkYaw * var6 + this.turnYaw * var7) * 0.017453292F;
      float var11 = (this.restRoll + this.idleRoll * var5 + this.walkRoll * var6 + this.turnRoll * var7) * 0.017453292F;
      if (this.follow != null) {
         int var12 = this.follow.ordinal();
         var9 += soft(this.followAmount * (var1.pitch[var12] - var1.pitch[var8]), this.followLimit);
         var10 += soft(this.followAmount * (var1.yaw[var12] - var1.yaw[var8]), this.followLimit);
         var11 += soft(this.followAmount * (var1.roll[var12] - var1.roll[var8]), this.followLimit);
      }

      float var13 = this.limit * 0.017453292F;
      var3[0] = Math.max(-var13, Math.min(var13, var9));
      var3[1] = Math.max(-var13, Math.min(var13, var10));
      var3[2] = Math.max(-var13, Math.min(var13, var11));
   }

   static float soft(float var0, float var1) {
      if (var1 <= 0.0F) {
         return var0;
      } else {
         float var2 = var1 * 0.017453292F;
         return var2 * (float)Math.tanh(var0 / var2);
      }
   }

   public void step(float[] var1, int var2, float[] var3, float var4) {
      if (!(this.stiffness <= 0.0F) && !(var4 <= 0.0F) && !(var4 > 0.25F) && !Float.isNaN(var1[var2])) {
         int var11 = Math.max(1, (int)Math.ceil(var4 * 120.0F));
         float var6 = var4 / var11;

         for (int var7 = 0; var7 < var11; var7++) {
            for (int var8 = 0; var8 < 3; var8++) {
               float var9 = var1[var2 + var8];
               float var10 = var1[var2 + 3 + var8];
               var10 += (this.stiffness * (var3[var8] - var9) - this.damping * var10) * var6;
               var1[var2 + 3 + var8] = var10;
               var1[var2 + var8] = var9 + var10 * var6;
            }
         }
      } else {
         for (int var5 = 0; var5 < 3; var5++) {
            var1[var2 + var5] = var3[var5];
            var1[var2 + 3 + var5] = 0.0F;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Input {
      public float time;
      public float walkPos;
      public float walkSpeed;
      public float turnRate;
      public final float[] pitch = new float[6];
      public final float[] yaw = new float[6];
      public final float[] roll = new float[6];

      public Input() {
      }
   }
}
