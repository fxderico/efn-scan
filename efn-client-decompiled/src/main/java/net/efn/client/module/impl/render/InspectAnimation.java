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

   static InspectAnimation.Clip clip(String string) {
      return "Twirl".equals(string) ? TWIRL_CLIP : CLASSIC_CLIP;
   }

   static void place(MatrixStack matrixStack, float[] fs, float f, int i, float g, float h, float j) {
      float var7 = 0.017453292F * f;
      float var8 = MathHelper.wrapDegrees(fs[3]) * var7 * i;
      float var9 = MathHelper.wrapDegrees(fs[4]) * var7;
      float var10 = MathHelper.wrapDegrees(fs[5]) * var7 * i;
      float var11 = MathHelper.wrapDegrees(fs[6]) * var7 * i;
      float var12 = MathHelper.wrapDegrees(fs[7]) * var7;
      matrixStack.translate(fs[0] * f * i, fs[1] * f, fs[2] * f);
      float var13 = 0.07F * i;
      matrixStack.multiply(new Quaternionf().rotateX(var9).rotateZ(var10).rotateY(var8), var13, -0.05F, -0.02F);
      matrixStack.multiply(new Quaternionf().rotateX(var12), g * i, h, j);
      matrixStack.multiply(new Quaternionf().rotateAxis(var11, 0.0F, 0.94F, 0.342F), var13, -0.05F, -0.02F);
   }

   static float[] sample(float[][] fs, float f, float[] gs) {
      f = MathHelper.clamp(f, 0.0F, 1.0F);
      int var3 = 0;

      while (var3 < fs.length - 2 && f > fs[var3 + 1][0]) {
         var3++;
      }

      float[] var4 = fs[var3 + 1];
      float[] var5 = fs[var3];
      float var6 = var4[0] - var5[0];
      float var7 = var6 <= 0.0F ? 1.0F : (f - var5[0]) / var6;

      for (int var8 = 0; var8 < 16; var8++) {
         gs[var8] = hermite(value(var5, var8), value(var4, var8), slope(fs, var3, var8) * var6, slope(fs, var3 + 1, var8) * var6, var7);
      }

      return gs;
   }

   static float[] sampleLoop(float[][] fs, float f, float[] gs) {
      f -= (float)Math.floor(f);
      int var4 = fs.length;
      int var3 = 0;

      while (var3 < var4 - 2 && f > fs[var3 + 1][0]) {
         var3++;
      }

      float[] var5 = fs[var3 + 1];
      float[] var6 = fs[var3];
      float var7 = var5[0] - var6[0];
      float var8 = var7 <= 0.0F ? 1.0F : (f - var6[0]) / var7;

      for (int var9 = 0; var9 < 16; var9++) {
         gs[var9] = hermite(value(var6, var9), value(var5, var9), loopSlope(fs, var3, var9) * var7, loopSlope(fs, var3 + 1, var9) * var7, var8);
      }

      return gs;
   }

   private static float loopSlope(float[][] fs, int i, int j) {
      int var7 = fs.length;
      float var8 = value(fs[var7 - 1], j) - value(fs[0], j);
      float var5;
      float var6;
      if (i == 0) {
         var6 = fs[var7 - 2][0] - 1.0F;
         var5 = value(fs[var7 - 2], j) - var8;
      } else {
         var6 = fs[i - 1][0];
         var5 = value(fs[i - 1], j);
      }

      float var3;
      float var4;
      if (i == var7 - 1) {
         var4 = fs[1][0] + 1.0F;
         var3 = value(fs[1], j) + var8;
      } else {
         var4 = fs[i + 1][0];
         var3 = value(fs[i + 1], j);
      }

      return (var3 - var5) / (var4 - var6);
   }

   private static float value(float[] fs, int i) {
      return i + 1 < fs.length ? fs[i + 1] : 0.0F;
   }

   private static float slope(float[][] fs, int i, int j) {
      if (i != 0 && i != fs.length - 1) {
         float[] var3 = fs[i - 1];
         float[] var4 = fs[i + 1];
         return (value(var4, j) - value(var3, j)) / (var4[0] - var3[0]);
      } else {
         return 0.0F;
      }
   }

   private static float hermite(float f, float g, float h, float i, float j) {
      float var5 = j * j;
      float var6 = var5 * j;
      return (2.0F * var6 - 3.0F * var5 + 1.0F) * f + (var6 - 2.0F * var5 + j) * h + (-2.0F * var6 + 3.0F * var5) * g + (var6 - var5) * i;
   }

   @Environment(EnvType.CLIENT)
   static final class Clip {
      final float[][] keys;
      final float seconds;
      final float loopEnter;
      final float loopExit;
      private final float[][] loop;

      Clip(float[][] fs, float f) {
         this(fs, f, 0.0F, 0.0F);
      }

      Clip(float[][] fs, float f, float g, float h) {
         this.keys = fs;
         this.seconds = f;
         this.loopEnter = g;
         this.loopExit = h;
         this.loop = h > g ? cut(fs, g, h) : null;
      }

      boolean loops() {
         return this.loop != null;
      }

      float[] sample(float f, float[] fs) {
         return this.loop != null && f > this.loopEnter && f < this.loopExit
            ? InspectAnimation.sampleLoop(this.loop, (f - this.loopEnter) / (this.loopExit - this.loopEnter), fs)
            : InspectAnimation.sample(this.keys, f, fs);
      }

      private static float[][] cut(float[][] fs, float f, float g) {
         int var3 = -1;
         int var4 = -1;

         for (int var5 = 0; var5 < fs.length; var5++) {
            if (Math.abs(fs[var5][0] - f) < 1.0E-4F) {
               var3 = var5;
            }

            if (Math.abs(fs[var5][0] - g) < 1.0E-4F) {
               var4 = var5;
            }
         }

         if (var3 >= 0 && var4 > var3) {
            float[][] var7 = new float[var4 - var3 + 1][];

            for (int var6 = var3; var6 <= var4; var6++) {
               var7[var6 - var3] = (float[])fs[var6].clone();
               var7[var6 - var3][0] = (fs[var6][0] - f) / (g - f);
            }

            return var7;
         } else {
            throw new IllegalArgumentException("a loop starts and ends on keys");
         }
      }
   }
}
