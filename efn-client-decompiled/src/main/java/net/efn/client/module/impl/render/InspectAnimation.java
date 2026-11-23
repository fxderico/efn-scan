package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
final class InspectAnimation {
   static final String CLASSIC = "Classic";
   static final String TWIRL = "Twirl";
   static final int X = 0;
   static final int Y = 1;
   static final int Z = 2;
   static final int YAW = 3;
   static final int PITCH = 4;
   static final int ROLL = 5;
   static final int FLIP = 6;
   static final int SPIN = 7;
   static final int PART_A = 8;
   static final int PART_B = 9;
   static final int SHINE = 10;
   static final int SLIDE = 11;
   static final int DRAW = 12;
   static final int SHEATH = 13;
   static final int OFF_HAND = 14;
   static final int OFF_AT = 15;
   static final int CHANNELS = 16;
   static final float GRIP_X = 0.07F;
   static final float GRIP_Y = -0.05F;
   static final float GRIP_Z = -0.02F;
   static final float CENTER_X = 0.07F;
   static final float CENTER_Y = 0.2F;
   static final float CENTER_Z = 0.07F;
   static final float BLADE_Y = 0.94F;
   static final float BLADE_Z = 0.342F;
   static final float SECONDS = 3.2F;
   private static final float[][] CLASSIC_KEYS = new float[][]{
      {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {0.14F, -0.24F, 0.17F, 0.1F, 60.0F, -10.0F, 50.0F, 0.0F, 0.0F},
      {0.3F, -0.25F, 0.18F, 0.11F, 70.0F, -5.0F, 46.0F, 0.0F, 0.0F},
      {0.4F, -0.24F, 0.21F, 0.11F, 66.0F, -6.0F, 48.0F, 180.0F, 0.0F},
      {0.46F, -0.24F, 0.2F, 0.11F, 64.0F, -7.0F, 49.0F, 184.0F, 0.0F},
      {0.56F, -0.25F, 0.19F, 0.11F, 70.0F, -5.0F, 46.0F, 360.0F, 0.0F},
      {0.62F, -0.25F, 0.18F, 0.11F, 70.0F, -5.0F, 46.0F, 360.0F, 0.0F},
      {0.8F, -0.23F, 0.19F, 0.1F, 56.0F, -10.0F, 52.0F, 366.0F, 0.0F},
      {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F}
   };
   private static final float[][] TWIRL_KEYS = new float[][]{
      {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {0.12F, -0.2F, 0.15F, 0.08F, 75.0F, -6.0F, 20.0F, 0.0F, -20.0F},
      {0.22F, -0.21F, 0.2F, 0.09F, 78.0F, -6.0F, 22.0F, 0.0F, 0.0F},
      {0.34F, -0.22F, 0.3F, 0.1F, 80.0F, -4.0F, 24.0F, 0.0F, 180.0F},
      {0.46F, -0.21F, 0.2F, 0.09F, 78.0F, -6.0F, 22.0F, 0.0F, 360.0F},
      {0.7F, -0.24F, 0.17F, 0.1F, 68.0F, -8.0F, 42.0F, 0.0F, 360.0F},
      {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F}
   };
   private static final InspectAnimation.Clip CLASSIC_CLIP = new InspectAnimation.Clip(CLASSIC_KEYS, 3.2F, 0.3F, 0.62F);
   private static final InspectAnimation.Clip TWIRL_CLIP = new InspectAnimation.Clip(TWIRL_KEYS, 3.2F, 0.22F, 0.46F);

   private InspectAnimation() {
   }

   static InspectAnimation.Clip clip(String var0) {
      return "Twirl".equals(var0) ? TWIRL_CLIP : CLASSIC_CLIP;
   }

   static void place(MatrixStack var0, float[] var1, float var2, int var3, float var4, float var5, float var6) {
      float var7 = 0.017453292F * var2;
      float var8 = MathHelper.method_15393(var1[3]) * var7 * var3;
      float var9 = MathHelper.method_15393(var1[4]) * var7;
      float var10 = MathHelper.method_15393(var1[5]) * var7 * var3;
      float var11 = MathHelper.method_15393(var1[6]) * var7 * var3;
      float var12 = MathHelper.method_15393(var1[7]) * var7;
      var0.method_46416(var1[0] * var2 * var3, var1[1] * var2, var1[2] * var2);
      float var13 = 0.07F * var3;
      var0.method_49278(new Quaternionf().rotateX(var9).rotateZ(var10).rotateY(var8), var13, -0.05F, -0.02F);
      var0.method_49278(new Quaternionf().rotateX(var12), var4 * var3, var5, var6);
      var0.method_49278(new Quaternionf().rotateAxis(var11, 0.0F, 0.94F, 0.342F), var13, -0.05F, -0.02F);
   }

   static float[] sample(float[][] var0, float var1, float[] var2) {
      var1 = MathHelper.method_15363(var1, 0.0F, 1.0F);
      int var3 = 0;

      while (var3 < var0.length - 2 && var1 > var0[var3 + 1][0]) {
         var3++;
      }

      float[] var4 = var0[var3 + 1];
      float[] var5 = var0[var3];
      float var6 = var4[0] - var5[0];
      float var7 = var6 <= 0.0F ? 1.0F : (var1 - var5[0]) / var6;

      for (int var8 = 0; var8 < 16; var8++) {
         var2[var8] = hermite(value(var5, var8), value(var4, var8), slope(var0, var3, var8) * var6, slope(var0, var3 + 1, var8) * var6, var7);
      }

      return var2;
   }

   static float[] sampleLoop(float[][] var0, float var1, float[] var2) {
      var1 -= (float)Math.floor(var1);
      int var4 = var0.length;
      int var3 = 0;

      while (var3 < var4 - 2 && var1 > var0[var3 + 1][0]) {
         var3++;
      }

      float[] var5 = var0[var3 + 1];
      float[] var6 = var0[var3];
      float var7 = var5[0] - var6[0];
      float var8 = var7 <= 0.0F ? 1.0F : (var1 - var6[0]) / var7;

      for (int var9 = 0; var9 < 16; var9++) {
         var2[var9] = hermite(value(var6, var9), value(var5, var9), loopSlope(var0, var3, var9) * var7, loopSlope(var0, var3 + 1, var9) * var7, var8);
      }

      return var2;
   }

   private static float loopSlope(float[][] var0, int var1, int var2) {
      int var7 = var0.length;
      float var8 = value(var0[var7 - 1], var2) - value(var0[0], var2);
      float var5;
      float var6;
      if (var1 == 0) {
         var6 = var0[var7 - 2][0] - 1.0F;
         var5 = value(var0[var7 - 2], var2) - var8;
      } else {
         var6 = var0[var1 - 1][0];
         var5 = value(var0[var1 - 1], var2);
      }

      float var3;
      float var4;
      if (var1 == var7 - 1) {
         var4 = var0[1][0] + 1.0F;
         var3 = value(var0[1], var2) + var8;
      } else {
         var4 = var0[var1 + 1][0];
         var3 = value(var0[var1 + 1], var2);
      }

      return (var3 - var5) / (var4 - var6);
   }

   private static float value(float[] var0, int var1) {
      return var1 + 1 < var0.length ? var0[var1 + 1] : 0.0F;
   }

   private static float slope(float[][] var0, int var1, int var2) {
      if (var1 != 0 && var1 != var0.length - 1) {
         float[] var3 = var0[var1 - 1];
         float[] var4 = var0[var1 + 1];
         return (value(var4, var2) - value(var3, var2)) / (var4[0] - var3[0]);
      } else {
         return 0.0F;
      }
   }

   private static float hermite(float var0, float var1, float var2, float var3, float var4) {
      float var5 = var4 * var4;
      float var6 = var5 * var4;
      return (2.0F * var6 - 3.0F * var5 + 1.0F) * var0 + (var6 - 2.0F * var5 + var4) * var2 + (-2.0F * var6 + 3.0F * var5) * var1 + (var6 - var5) * var3;
   }

   @Environment(EnvType.CLIENT)
   static final class Clip {
      final float[][] keys;
      final float seconds;
      final float loopEnter;
      final float loopExit;
      private final float[][] loop;

      Clip(float[][] var1, float var2) {
         this(var1, var2, 0.0F, 0.0F);
      }

      Clip(float[][] var1, float var2, float var3, float var4) {
         this.keys = var1;
         this.seconds = var2;
         this.loopEnter = var3;
         this.loopExit = var4;
         this.loop = var4 > var3 ? cut(var1, var3, var4) : null;
      }

      boolean loops() {
         return this.loop != null;
      }

      float[] sample(float var1, float[] var2) {
         return this.loop != null && var1 > this.loopEnter && var1 < this.loopExit
            ? InspectAnimation.sampleLoop(this.loop, (var1 - this.loopEnter) / (this.loopExit - this.loopEnter), var2)
            : InspectAnimation.sample(this.keys, var1, var2);
      }

      private static float[][] cut(float[][] var0, float var1, float var2) {
         int var3 = -1;
         int var4 = -1;

         for (int var5 = 0; var5 < var0.length; var5++) {
            if (Math.abs(var0[var5][0] - var1) < 1.0E-4F) {
               var3 = var5;
            }

            if (Math.abs(var0[var5][0] - var2) < 1.0E-4F) {
               var4 = var5;
            }
         }

         if (var3 >= 0 && var4 > var3) {
            float[][] var7 = new float[var4 - var3 + 1][];

            for (int var6 = var3; var6 <= var4; var6++) {
               var7[var6 - var3] = (float[])var0[var6].clone();
               var7[var6 - var3][0] = (var0[var6][0] - var1) / (var2 - var1);
            }

            return var7;
         } else {
            throw new IllegalArgumentException("a loop starts and ends on keys");
         }
      }
   }
}
