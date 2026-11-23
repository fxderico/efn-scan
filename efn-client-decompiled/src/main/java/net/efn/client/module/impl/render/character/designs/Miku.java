package net.efn.client.module.impl.render.character.designs;

import java.util.ArrayList;
import net.efn.client.module.impl.render.character.kit.Bone;
import net.efn.client.module.impl.render.character.kit.Canvas;
import net.efn.client.module.impl.render.character.kit.CharacterDesign;
import net.efn.client.module.impl.render.character.kit.Colors;
import net.efn.client.module.impl.render.character.kit.Decal;
import net.efn.client.module.impl.render.character.kit.Motion;
import net.efn.client.module.impl.render.character.kit.Paint;
import net.efn.client.module.impl.render.character.kit.Paint2D;
import net.efn.client.module.impl.render.character.kit.Part;
import net.efn.client.module.impl.render.character.kit.Rig;
import net.efn.client.module.impl.render.character.kit.Shape;
import net.efn.client.module.impl.render.character.kit.Shape2D;
import net.efn.client.module.impl.render.character.kit.Shapes;
import net.efn.client.module.impl.render.character.kit.Strand;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Miku extends CharacterDesign {
   private static final int SKIN = -8241;
   private static final int SKIN_SHADE = -1198942;
   private static final int SKIN_LINE = -5740452;
   private static final int HAIR_ROOT = -14705514;
   private static final int HAIR = -12793152;
   private static final int HAIR_TIP = -7936291;
   private static final int HAIR_SHADE = -15368325;
   private static final int HAIR_LINE = -15903662;
   private static final int SHINE = -857866245;
   private static final int SHIRT = -2499360;
   private static final int SHIRT_SHADE = -5525063;
   private static final int SHIRT_LINE = -10788500;
   private static final int BLACK = -13618373;
   private static final int BLACK_SHADE = -14934492;
   private static final int BLACK_LINE = -16119026;
   private static final int TEAL = -13645638;
   private static final int TEAL_DARK = -14971002;
   private static final int MAGENTA = -1882484;
   private static final int TATTOO = -2608065;
   private static final float SKIRT_TOP = 17.9F;
   private static final float SKIRT_HEM = 13.3F;
   private static final int PLEATS = 18;
   private static final float HS = 1.12F;
   private static final float DROP = 0.4F;
   private static final float HX = 0.0F;
   private static final float HY = 28.9F;
   private static final float HZ = -0.3F;
   private static final float PPU = 56.88889F;
   private static final float FACE_TOP = 29.95F;
   private static final float[][] LOCKS = new float[][]{
      {-0.1F, 28.3F, -2.0F, 0.95F, 0.12F, 0.34F},
      {-1.0F, 28.75F, -25.0F, 0.95F, 0.28F, 0.3F},
      {-1.92F, 28.4F, -47.0F, 0.92F, 0.26F, 0.3F},
      {0.9F, 28.85F, 24.0F, 0.95F, 0.28F, 0.3F},
      {1.9F, 28.5F, 46.0F, 0.92F, 0.25F, 0.3F},
      {-0.55F, 28.5F, -13.0F, 0.45F, 0.16F, 0.44F},
      {0.46F, 28.62F, 11.0F, 0.45F, 0.18F, 0.44F},
      {-1.46F, 28.65F, -36.0F, 0.42F, 0.2F, 0.42F},
      {1.42F, 28.7F, 35.0F, 0.42F, 0.2F, 0.42F}
   };

   public Miku() {
      super("miku", "Miku");
   }

   @Override
   protected void define(Rig var1) {
      var1.scale(1.0F).outline(-14011336, 0.12F).occlusion(0.3F).flatLight(0.55F);
      float[][] var2 = lockPaths();
      Paint var3 = hairPaint();
      head(var1, var2);
      hair(var1, var3);
      fringe(var1, var2);
      twinTails(var1);
      headset(var1);
      body(var1);
      skirt(var1);
      arms(var1);
      legs(var1);
   }

   private static Shape up(Shape var0) {
      return var0.scale(1.12F, 0.0F, 24.6F, -0.2F).move(0.0F, -0.4F, 0.0F);
   }

   private static float tx(float var0) {
      return var0 * 1.12F;
   }

   private static float ty(float var0) {
      return 24.6F + (var0 - 24.6F) * 1.12F - 0.4F;
   }

   private static float tz(float var0) {
      return -0.2F + (var0 + 0.2F) * 1.12F;
   }

   private static Shape headBase() {
      return Shapes.blend(
         0.85F,
         Shapes.ellipsoid(0.0F, 28.72F, -0.15F, 2.28F, 2.4F, 2.35F),
         Shapes.ellipsoid(0.0F, 27.75F, 0.55F, 2.05F, 1.3F, 1.75F),
         Shapes.cone(0.0F, 27.3F, 0.62F, 1.35F, 0.0F, 26.05F, 1.2F, 0.3F)
      );
   }

   private static void head(Rig var0, float[][] var1) {
      Shape var2 = Shapes.capsule(0.0F, 23.3F, -0.45F, 0.0F, 25.9F, -0.3F, 0.58F);
      float var3 = ty(25.82F);
      float var4 = tz(1.2F);
      Paint var5 = Paint.field((var2x, var3x, var4x) -> {
         float var5x = var3 + Math.max(0.0F, var4 - var4x) * 0.62F;
         float var6x = smooth(var5x + 0.05F, var5x - 0.2F, var3x) * (0.6F + 0.4F * smooth(23.4F, 24.8F, var3x));
         return Colors.mix(-8241, -1198942, var6x);
      });
      Shape var6 = up(headBase().intersect(Shapes.below(30.2F)));
      var0.part(Bone.HEAD)
         .pivot(0.0F, 24.6F, -0.2F)
         .shape(Shapes.blend(0.45F, var6, var2).paint(var5))
         .detail(2.0F)
         .flatLight(1.0F)
         .occlusion(0.22F)
         .outline(-5740452, 0.1F)
         .decal(face(var1));
   }

   private static float[][] lockPaths() {
      Shape var0 = headBase();
      float[][] var1 = new float[LOCKS.length][];

      for (int var2 = 0; var2 < LOCKS.length; var2++) {
         float[] var3 = LOCKS[var2];
         float[] var4 = onHead(36.0F, var3[2], -0.15F);
         float[] var5 = onHead(58.0F, var3[2], 0.05F);
         float var6 = var5[0] + (var3[0] - var5[0]) * 0.75F;
         float var7 = var3[1] + 0.68F;
         float var8 = var3[0] - Math.signum(var3[0]) * var3[4];
         var1[var2] = new float[]{
            var4[0],
            var4[1],
            var4[2],
            var5[0],
            var5[1],
            var5[2],
            var6,
            var7,
            frontZ(var0, var6, var7) + var3[5],
            var8,
            var3[1],
            frontZ(var0, var8, var3[1]) + 0.22F,
            var3[3]
         };
      }

      return var1;
   }

   private static float frontZ(Shape var0, float var1, float var2) {
      float var3 = 3.5F;

      while (var3 > -1.0F && var0.distance(var1, var2, var3) > 0.0F) {
         var3 -= 0.02F;
      }

      return var3;
   }

   private static void fringe(Rig var0, float[][] var1) {
      Shape[] var2 = new Shape[var1.length];
      Shape[] var3 = new Shape[var1.length];

      for (int var4 = 0; var4 < var1.length; var4++) {
         float[] var5 = var1[var4];
         float var6 = var5[12];
         Strand var7 = Shapes.tube(
               var5[0],
               var5[1],
               var5[2],
               0.9F * var6,
               var5[3],
               var5[4],
               var5[5],
               1.15F * var6,
               var5[6],
               var5[7],
               var5[8],
               0.95F * var6,
               var5[9],
               var5[10],
               var5[11],
               0.12F
            )
            .flattenFrom(3.0F, 0.0F, 28.7F, -0.15F);
         var3[var4] = Shapes.extrude(Shape2D.polygon(blade(var5)), 5.0F, 0.0F);
         var2[var4] = Shapes.intersect(var7, var3[var4]);
      }

      Shape var8 = Shapes.intersect(Shapes.blend(0.15F, var2), Shapes.union(var3));
      Shape var9 = headBase();
      Paint var10 = Paint.field((var1x, var2x, var3x) -> {
         int var4x = Colors.mix(Colors.mix(-14705514, -12793152, 0.45F), -12793152, smooth(30.9F, 29.7F, var2x));
         var4x = Colors.mix(var4x, -7936291, 0.45F * smooth(29.5F, 28.5F, var2x));
         return Colors.mix(var4x, -15368325, 0.6F * (1.0F - smooth(0.12F, 0.42F, var9.distance(var1x, var2x, var3x))));
      });
      var0.extra("fringe", Bone.HEAD)
         .shape(up(var8.paint(var10)))
         .resolution(0.45F)
         .detail(1.05F)
         .flatLight(0.7F)
         .occlusion(0.1F)
         .outline(-15903662, 0.09F)
         .decal(shine());
   }

   private static float[] blade(float[] var0) {
      float var1 = var0[12];
      boolean var2 = var0[9] < 0.0F;
      float var3 = var0[3];
      float var4 = 31.6F;
      float var5 = var0[6];
      float var6 = var0[7];
      float var7 = var0[9];
      float var8 = var0[10];
      byte var9 = 12;
      float[] var10 = new float[(2 * var9 + 1) * 2];

      for (int var11 = 0; var11 <= var9; var11++) {
         float var12 = (float)var11 / var9;
         float var13 = 1.0F - var12;
         float var14 = var13 * var13 * var3 + 2.0F * var13 * var12 * var5 + var12 * var12 * var7;
         float var15 = var13 * var13 * var4 + 2.0F * var13 * var12 * var6 + var12 * var12 * var8;
         float var16 = var13 * (var5 - var3) + var12 * (var7 - var5);
         float var17 = var13 * (var6 - var4) + var12 * (var8 - var6);
         float var18 = (float)Math.sqrt(var16 * var16 + var17 * var17);
         float var19 = -var17 / var18;
         float var20 = var16 / var18;
         if (var19 > 0.0F != var2) {
            var19 = -var19;
            var20 = -var20;
         }

         float var21 = var1 * (1.0F - smooth(0.6F, 1.0F, var12));
         float var22 = var1 * (1.0F - (float)Math.pow(smooth(0.3F, 1.0F, var12), 0.75));
         var10[var11 * 2] = var14 + var19 * var21;
         var10[var11 * 2 + 1] = var15 + var20 * var21;
         if (var11 < var9) {
            var10[(2 * var9 - var11) * 2] = var14 - var19 * var22;
            var10[(2 * var9 - var11) * 2 + 1] = var15 - var20 * var22;
         }
      }

      return var10;
   }

   private static Decal shine() {
      Canvas var1 = new Canvas(280, 22);

      for (float[] var5 : new float[][]{{-68.0F, -54.0F}, {-45.0F, -25.0F}, {-13.0F, 9.0F}, {20.0F, 38.0F}, {48.0F, 64.0F}}) {
         float var6 = var5[0];
         float var7 = var5[1];
         float var8 = (var6 + var7) * 0.5F;
         float var9 = 6.5F - Math.abs(var8) * 0.025F;
         var1.fill(
            Shape2D.curve(140.0F + var6 * 2.0F, ringY(var6), 140.0F + var6 + var8, ringY((var6 + var8) * 0.5F), 140.0F + var8 * 2.0F, ringY(var8), 1.0F, var9),
            -857866245
         );
         var1.fill(
            Shape2D.curve(140.0F + var8 * 2.0F, ringY(var8), 140.0F + var8 + var7, ringY((var8 + var7) * 0.5F), 140.0F + var7 * 2.0F, ringY(var7), var9, 1.0F),
            -857866245
         );
      }

      return Decal.wrap(var1, 0.0F, ty(30.16F), tz(-0.3F), 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 140.0F, 0.6272F).radius(1.2F, 3.8F).offset(0.03F);
   }

   private static float ringY(float var0) {
      float var1 = 30.4F - 0.36F * (1.0F - (float)Math.cos(Math.toRadians(var0)));
      return (30.44F - var1) * 40.0F;
   }

   private static void hair(Rig var0, Paint var1) {
      ArrayList var2 = new ArrayList();
      var2.add(Shapes.ellipsoid(0.0F, 28.9F, -0.3F, 2.58F, 2.62F, 2.7F));

      for (float var6 : new float[]{64.0F, 88.0F, 272.0F, 296.0F}) {
         var2.add(capLock(var6, 5.0F, 98.0F, 0.8F, 0.62F));
      }

      for (float var11 = 130.0F; var11 <= 230.0F; var11 += 25.0F) {
         var2.add(capLock(var11, 20.0F, 118.0F, 0.85F, 0.5F));
      }

      Shape var12 = Shapes.box(0.0F, 26.3F, 2.3F, 2.95F, 3.3F, 2.8F, 0.9F);
      Shape var13 = Shapes.blend(
            0.3F,
            Shapes.strand(0.62F, 0.44F, -2.3F, 29.6F, -0.05F, -2.48F, 28.1F, -0.12F, -2.45F, 26.5F, -0.22F).flatten(1.6F, 1.0F, 0.0F, 0.0F),
            Shapes.strand(0.68F, 0.44F, -2.25F, 29.5F, -0.95F, -2.46F, 27.9F, -1.05F, -2.45F, 26.3F, -1.12F).flatten(1.6F, 1.0F, 0.0F, 0.0F)
         )
         .mirrorX();
      Strand var14 = Shapes.strand(0.9F, 0.3F, 0.0F, 28.0F, -2.35F, 0.0F, 26.6F, -2.2F, 0.0F, 25.6F, -1.95F).flatten(2.2F, 0.0F, 0.0F, 1.0F);
      Shape var15 = Shapes.blend(
         0.25F, Shapes.subtract(Shapes.blend(0.3F, var2.toArray(new Shape[0])), var12).paint(var1), var13.paint(var1.occlusion(0.35F)), var14.paint(var1)
      );
      var0.extra("hair", Bone.HEAD).shape(up(var15)).detail(1.1F).flatLight(0.55F).occlusion(0.22F).outline(-15903662, 0.09F).decal(shine());
      Strand var7 = Shapes.tube(
            -1.95F,
            30.4F,
            0.92F,
            0.55F,
            -2.3F,
            29.3F,
            1.12F,
            0.6F,
            -2.42F,
            27.8F,
            1.0F,
            0.55F,
            -2.4F,
            25.9F,
            0.8F,
            0.46F,
            -2.3F,
            24.0F,
            0.78F,
            0.36F,
            -2.15F,
            22.6F,
            0.82F,
            0.05F
         )
         .flatten(1.8F, 1.0F, 0.0F, 0.3F);
      Strand var8 = Shapes.tube(
            -2.2F,
            30.2F,
            0.1F,
            0.58F,
            -2.52F,
            28.9F,
            0.24F,
            0.62F,
            -2.58F,
            27.3F,
            0.32F,
            0.55F,
            -2.45F,
            25.6F,
            0.5F,
            0.42F,
            -2.36F,
            23.8F,
            0.52F,
            0.25F,
            -2.3F,
            23.0F,
            0.55F,
            0.04F
         )
         .flatten(1.8F, 1.0F, 0.0F, 0.15F);
      Motion var9 = new Motion().idle(1.5F, 0.0F, 0.6F, 0.35F).walk(3.0F, 0.0F, 1.0F, 2.0F).drag(8.0F).hang(0.6F).spring(60.0F, 8.0F).bend(1.4F);
      Part var10 = var0.extra("sidelock_r", Bone.HEAD)
         .pivot(tx(-2.3F), ty(30.0F), tz(0.6F))
         .shape(up(Shapes.blend(0.25F, var7, var8).paint(var1)))
         .motion(var9)
         .resolution(0.7F)
         .detail(0.6F)
         .flatLight(0.5F)
         .occlusion(0.35F)
         .outline(-15903662, 0.09F);
      var0.extra("sidelock_l", Bone.HEAD).mirrorOf(var10);
   }

   private static void twinTails(Rig var0) {
      float var1 = tx(-2.62F);
      float var2 = ty(30.2F);
      float var3 = tz(-1.35F);
      float[][] var4 = new float[][]{
         {
               0.0F,
               0.0F,
               1.0F,
               -0.9F,
               -0.5F,
               1.35F,
               -1.5F,
               -2.6F,
               1.5F,
               -1.75F,
               -6.5F,
               1.45F,
               -1.75F,
               -11.5F,
               1.3F,
               -1.6F,
               -16.5F,
               1.05F,
               -1.45F,
               -20.5F,
               0.7F,
               -1.35F,
               -23.4F,
               0.06F
         },
         {
               0.0F,
               0.1F,
               0.8F,
               -1.0F,
               -0.4F,
               1.0F,
               -1.9F,
               -2.8F,
               1.1F,
               -2.2F,
               -7.0F,
               1.0F,
               -2.25F,
               -12.0F,
               0.9F,
               -2.05F,
               -17.0F,
               0.7F,
               -1.85F,
               -20.8F,
               0.45F,
               -1.95F,
               -22.8F,
               0.04F
         },
         {
               0.0F,
               0.1F,
               0.8F,
               -0.7F,
               -0.6F,
               1.0F,
               -1.0F,
               -2.9F,
               1.05F,
               -1.15F,
               -7.2F,
               1.0F,
               -1.1F,
               -12.2F,
               0.9F,
               -0.95F,
               -16.8F,
               0.7F,
               -0.9F,
               -20.2F,
               0.45F,
               -0.75F,
               -22.2F,
               0.04F
         }
      };
      float[] var5 = new float[]{0.0F, -0.25F, 0.2F};
      Shape[] var6 = new Shape[var4.length];

      for (int var7 = 0; var7 < var4.length; var7++) {
         float[] var8 = var4[var7];
         float[] var9 = new float[var8.length / 3 * 4];

         for (int var10 = 0; var10 < var8.length / 3; var10++) {
            float var11 = var10 / (var8.length / 3.0F - 1.0F);
            var9[var10 * 4] = var1 + var8[var10 * 3];
            var9[var10 * 4 + 1] = var2 + var8[var10 * 3 + 1] * 1.12F;
            var9[var10 * 4 + 2] = var3 - 1.15F * (float)Math.sin(Math.min(1.0F, var11 * 3.0F) * 3.141592653589793 * 0.5) + var5[var7] * var11;
            var9[var10 * 4 + 3] = var8[var10 * 3 + 2];
         }

         var6[var7] = Shapes.tube(var9).flatten(1.7F, 0.25F, 0.0F, 1.0F);
      }

      float var12 = var2 - 26.208F;
      Paint var13 = Paint.field((var3x, var4x, var5x) -> {
         float var6x = clamp01((var2 - var4x) / (var2 - var12));
         int var7x = Colors.mix(-14705514, -12793152, smooth(0.0F, 0.18F, var6x));
         var7x = Colors.mix(var7x, -7936291, 0.6F * smooth(0.55F, 1.0F, var6x));
         float var8x = smooth(0.4F, -0.6F, var3x - var1 + 1.6F) * 0.35F;
         return Colors.mix(var7x, -15368325, var8x);
      });
      Shape var14 = Shapes.union(
            Shapes.box(var1 - 0.3F, var2, var3, 0.34F, 1.0F, 0.75F, 0.14F).color(-13618373),
            Shapes.box(var1 - 0.62F, var2, var3 + 0.38F, 0.06F, 0.86F, 0.09F, 0.03F).color(-1882484),
            Shapes.box(var1 - 0.62F, var2, var3 - 0.38F, 0.06F, 0.86F, 0.09F, 0.03F).color(-1882484)
         )
         .rotate(0.0F, 0.0F, -12.0F, var1, var2, var3);
      Motion var15 = Motion.hair().hang(0.25F).follow(Bone.BODY, 0.8F).drag(12.0F).turn(6.0F, 0.0F).spring(40.0F, 6.0F).bend(1.6F).limit(35.0F);
      Part var16 = var0.extra("tail_r", Bone.HEAD)
         .pivot(var1, var2, var3)
         .shape(Shapes.union(Shapes.blend(0.35F, var6).paint(var13), var14))
         .motion(var15)
         .resolution(1.0F)
         .detail(0.9F)
         .flatLight(0.5F)
         .occlusion(0.3F)
         .outline(-15903662, 0.1F);
      var0.extra("tail_l", Bone.HEAD).mirrorOf(var16);
   }

   private static void headset(Rig var0) {
      Shape var1 = Shapes.union(
         Shapes.cylinder(-2.45F, 28.0F, 0.05F, -2.85F, 28.0F, 0.05F, 0.62F, 0.12F).color(-13618373),
         Shapes.cylinder(-2.8F, 28.0F, 0.05F, -2.95F, 28.0F, 0.05F, 0.38F, 0.06F).color(-13645638)
      );
      Shape var2 = Shapes.union(
         Shapes.tube(-2.7F, 27.7F, 0.45F, 0.08F, -2.45F, 26.8F, 1.3F, 0.08F, -1.7F, 26.25F, 2.05F, 0.08F, -0.95F, 26.15F, 2.25F, 0.08F).color(-13618373),
         Shapes.ellipsoid(-0.85F, 26.15F, 2.28F, 0.22F, 0.16F, 0.16F).color(-14934492)
      );
      var0.extra("headset", Bone.HEAD)
         .shape(up(Shapes.union(var1, var1.mirrored(), var2)))
         .resolution(0.4F)
         .detail(0.6F)
         .flatLight(0.4F)
         .outline(-16119026, 0.08F);
   }

   private static Shape capLock(float var0, float var1, float var2, float var3, float var4) {
      float[] var5 = new float[15];

      for (int var6 = 0; var6 < 5; var6++) {
         float[] var7 = onHead(var1 + (var2 - var1) * var6 / 4.0F, var0, 0.02F);
         System.arraycopy(var7, 0, var5, var6 * 3, 3);
      }

      return Shapes.strand(var3, var4, var5).flattenFrom(2.8F, 0.0F, 28.9F, -0.3F);
   }

   private static float[] onHead(float var0, float var1, float var2) {
      double var3 = Math.toRadians(var0);
      double var5 = Math.toRadians(var1);
      float var7 = (float)Math.cos(var3);
      float var8 = (float)Math.sin(var3);
      float var9 = (float)Math.cos(var5);
      float var10 = (float)Math.sin(var5);
      float var11 = var10 * var8;
      float var12 = 0.97F * var7 + 0.24F * var9 * var8;
      float var13 = -0.24F * var7 + 0.97F * var9 * var8;
      float var14 = 1.0F / (float)Math.sqrt(var11 * var11 / 7.3984003F + var12 * var12 / 7.3984003F + var13 * var13 / 8.065599F) + var2;
      return new float[]{0.0F + var11 * var14, 28.9F + var12 * var14, -0.3F + var13 * var14};
   }

   private static Paint hairPaint() {
      return Paint.field((var0, var1, var2) -> {
         float var3 = var1 - 31.2F;
         float var4 = var2 + 0.7F;
         float var5 = (float)Math.sqrt(var0 * var0 + var3 * var3 + var4 * var4);
         int var6 = Colors.mix(-14705514, -12793152, smooth(0.8F, 2.9F, var5));
         return Colors.mix(var6, -7936291, smooth(5.5F, 17.5F, var5) * 0.9F);
      });
   }

   private static void body(Rig var0) {
      Shape var1 = Shapes.union(
         Shapes.capsule(0.0F, 23.95F, -0.15F, -2.45F, 23.3F, -0.15F, 0.9F), Shapes.capsule(0.0F, 23.95F, -0.15F, 2.45F, 23.3F, -0.15F, 0.9F)
      );
      Shape var2 = Shapes.ellipsoid(0.0F, 21.7F, 0.05F, 2.45F, 2.1F, 1.65F);
      Shape var3 = Shapes.ellipsoid(0.84F, 21.6F, 0.6F, 0.9F, 0.8F, 0.95F).mirrorX();
      Shape var4 = Shapes.ellipsoid(0.0F, 19.6F, 0.0F, 2.0F, 1.7F, 1.45F);
      Shape var5 = Shapes.ellipsoid(0.0F, 17.8F, 0.0F, 2.15F, 1.6F, 1.5F);
      Shape var6 = Shapes.blend(1.0F, var1, Shapes.blend(0.8F, var2, var3), var4, var5).intersect(Shapes.above(17.2F));
      Paint var7 = Paint.field((var0x, var1x, var2x) -> {
         float var3x = smooth(0.5F, 1.3F, var2x) * smooth(19.9F, 20.5F, var1x) * (1.0F - smooth(20.6F, 21.1F, var1x));
         float var4x = smooth(1.6F, 2.3F, Math.abs(var0x)) * 0.35F;
         return Colors.mix(-2499360, -5525063, Math.max(var3x * 0.55F, var4x));
      });
      var0.part(Bone.BODY).shape(var6.paint(var7)).outline(-10788500, 0.1F).detail(0.85F).occlusion(0.2F);
      Shape2D var8 = Shape2D.polygon(0.05F, 24.5F, 1.15F, 24.55F, 1.35F, 23.7F, 0.6F, 23.15F, 0.08F, 23.55F);
      Shape var9 = Shapes.extrude(var8, 0.14F, 0.07F).rotate(-25.0F, -20.0F, 0.0F, 0.5F, 23.9F, 0.0F).move(0.0F, 0.0F, frontZ(var6, 0.6F, 23.8F) + 0.02F);
      Shape var10 = Shapes.union(var9, var9.mirrored()).color(-2499360);
      var0.extra("collar", Bone.BODY).shape(var10).resolution(0.35F).detail(0.8F).flatLight(0.3F).outline(-13645638, 0.1F);
      float[] var11 = new float[]{23.35F, 22.4F, 21.3F, 20.2F, 19.3F};
      float[] var12 = new float[]{0.26F, 0.32F, 0.38F, 0.42F, 0.44F};
      float[] var13 = new float[var11.length * 4];

      for (int var14 = 0; var14 < var11.length; var14++) {
         var13[var14 * 4 + 1] = var11[var14];
         var13[var14 * 4 + 2] = frontZ(var6, 0.0F, var11[var14]) + 0.1F;
         var13[var14 * 4 + 3] = var12[var14];
      }

      Shape2D var17 = Shape2D.polygon(-1.0F, 24.0F, 1.0F, 24.0F, 1.0F, 19.7F, 0.0F, 19.1F, -1.0F, 19.7F);
      Shape var15 = Shapes.intersect(Shapes.tube(var13).flatten(3.5F, 0.0F, 0.0F, 1.0F), Shapes.extrude(var17, 4.0F, 0.0F));
      Shape var16 = Shapes.ellipsoid(0.0F, 23.45F, frontZ(var6, 0.0F, 23.45F) + 0.12F, 0.36F, 0.3F, 0.22F);
      var0.extra("tie", Bone.BODY)
         .pivot(0.0F, 23.45F, 1.6F)
         .shape(Shapes.union(var15, var16).paint(Paint.vertical(-14971002, 19.3F, -13645638, 22.5F)))
         .motion(Motion.none().walk(3.0F, 0.0F, 2.0F, 2.0F).drag(4.0F).spring(80.0F, 8.0F))
         .resolution(0.4F)
         .detail(0.5F)
         .flatLight(0.4F)
         .outline(-15835560, 0.08F);
   }

   private static void skirt(Rig var0) {
      Shape var1 = Shapes.field(Miku::skirtDistance, -3.4F, 13.1F, -3.0F, 3.4F, 18.1F, 3.0F).paint(Paint.field(Miku::skirtColor));
      var0.extra("skirt", Bone.BODY)
         .pivot(0.0F, 17.9F, 0.0F)
         .shape(var1)
         .resolution(0.9F)
         .detail(0.8F)
         .occlusion(0.35F)
         .outline(-16119026, 0.12F)
         .decal(stripe(13.650001F, 0.26F))
         .motion(Motion.none().walk(0.0F, 8.0F, 0.0F, 1.0F).walkPhase(-90.0F).drag(2.0F).spring(80.0F, 10.0F));
   }

   private static float flare(float var0) {
      float var1 = clamp01((17.6F - var0) / 4.299999F);
      return var1 * (2.0F - var1) * 0.8F + var1 * 0.2F;
   }

   private static float pleat(float var0, float var1, float var2) {
      float var3 = 2.25F + 0.95F * var2;
      float var4 = 1.62F + 0.95F * var2;
      double var5 = Math.atan2(var0 / var3, var1 / var4) * 18.0 / 6.283185307179586;
      var5 -= Math.floor(var5);
      return (float)Math.abs(2.0 * var5 - 1.0);
   }

   private static float skirtDistance(float var0, float var1, float var2) {
      float var3 = flare(var1);
      float var4 = 2.25F + 0.95F * var3;
      float var5 = 1.62F + 0.95F * var3;
      float var6 = var0 / var4;
      float var7 = var2 / var5;
      float var8 = (float)Math.sqrt(var6 * var6 + var7 * var7);
      float var9 = (var8 - 1.0F) * Math.min(var4, var5) * 0.72F - 0.2F * var3 * (pleat(var0, var2, var3) - 0.5F);
      return Math.max(var9, Math.max(var1 - 17.9F, 13.3F - var1));
   }

   private static int skirtColor(float var0, float var1, float var2) {
      float var3 = flare(var1);
      return Colors.mix(-14934492, -13618373, 0.35F + 0.65F * (1.0F - var3 + var3 * pleat(var0, var2, var3)));
   }

   private static Decal stripe(float var0, float var1) {
      Canvas var2 = new Canvas(512, 24);
      var2.fill(Shape2D.box(256.0F, 12.0F, 256.0F, 8.0F, 0.0F), -13645638);
      return Decal.wrap(var2, 0.0F, var0, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 360.0F, var1).radius(1.8F, 3.4F);
   }

   private static void arms(Rig var0) {
      float var1 = -3.5F;
      float var2 = 23.1F;
      float var3 = -0.12F;
      float var4 = -3.95F;
      float var5 = 19.5F;
      float var6 = -0.3F;
      float var7 = -4.2F;
      float var8 = 16.0F;
      float var9 = 0.4F;
      Shape var10 = Shapes.blend(
            0.35F,
            Shapes.sphere(var1 + 0.1F, var2, var3, 0.66F),
            Shapes.cone(var1, var2, var3, 0.62F, var4, var5, var6, 0.52F),
            Shapes.cone(var4, var5, var6, 0.52F, var7, var8, var9, 0.44F)
         )
         .paint(Paint.field((var0x, var1x, var2x) -> {
            float var3x = smooth(23.4F, 21.8F, var1x);
            return Colors.mix(-1198942, -8241, 0.4F + 0.6F * var3x);
         }));
      Part var11 = var0.part(Bone.RIGHT_ARM)
         .pivot(-3.5F, 23.25F, -0.1F)
         .grip(-4.32F, 15.05F, 0.72F)
         .shape(var10)
         .resolution(0.7F)
         .detail(0.8F)
         .flatLight(0.8F)
         .outline(-5740452, 0.1F)
         .swing(0.55F)
         .joint(var4, var5, var6, 0.55F);
      var0.part(Bone.LEFT_ARM).mirrorOf(var11).decal(tattoo(-var1, var2, var3, -var4, var5, var6));
      float var12 = (float)Math.sqrt((var7 - var4) * (var7 - var4) + (var8 - var5) * (var8 - var5) + (var9 - var6) * (var9 - var6));
      float var13 = (var7 - var4) / var12;
      float var14 = (var8 - var5) / var12;
      float var15 = (var9 - var6) / var12;
      float var16 = var4 + 0.35F * (var7 - var4);
      float var17 = var5 + 0.35F * (var8 - var5) + 0.9F;
      float var18 = var6 + 0.35F * (var9 - var6);
      float var19 = var7 + 1.1F * var13;
      float var20 = var8 + 1.1F * var14;
      float var21 = var9 + 1.1F * var15;
      Shape var22 = Shapes.cone(var16, var17 + 0.3F, var18, 0.62F, var19, var20, var21, 1.15F);
      Shape var23 = Shapes.sphere(var19 + 0.25F * var13, var20 + 0.25F * var14, var21 + 0.25F * var15, 0.95F);
      float var25 = var17 + 0.3F;
      float var27 = (float)Math.sqrt((var19 - var16) * (var19 - var16) + (var20 - var25) * (var20 - var25) + (var21 - var18) * (var21 - var18));
      Shape var28 = Shapes.subtract(0.1F, var22, var23).paint(Paint.field((var7x, var8x, var9x) -> {
         float var10x = ((var7x - var16) * (var19 - var16) + (var8x - var25) * (var20 - var25) + (var9x - var18) * (var21 - var18)) / (var27 * var27);
         return Colors.mix(-13618373, -14934492, smooth(0.2F, 1.0F, var10x) * 0.5F);
      }));
      Canvas var29 = new Canvas(256, 16);
      var29.fill(Shape2D.box(128.0F, 8.0F, 128.0F, 6.0F, 0.0F), -13645638);
      float var30 = var19 - var16;
      float var31 = var20 - var25;
      float var32 = var21 - var18;
      Decal var33 = Decal.wrap(
            var29, var16 + var30 * 0.93F, var25 + var31 * 0.93F, var18 + var32 * 0.93F, var30, var31, var32, 0.0F, 0.0F, 1.0F, 360.0F, var27 * 0.1F
         )
         .radius(0.7F, 1.4F);
      Decal var34 = Decal.wrap(
            var29, var16 + var30 * 0.05F, var25 + var31 * 0.05F, var18 + var32 * 0.05F, var30, var31, var32, 0.0F, 0.0F, 1.0F, 360.0F, var27 * 0.08F
         )
         .radius(0.4F, 1.0F);
      Part var35 = var0.extra("sleeve_r", Bone.RIGHT_ARM)
         .shape(var28)
         .resolution(1.1F)
         .detail(0.8F)
         .flatLight(0.3F)
         .occlusion(0.4F)
         .outline(-16119026, 0.1F)
         .decal(var33)
         .decal(var34);
      var0.extra("sleeve_l", Bone.LEFT_ARM).mirrorOf(var35);
      hands(var0, var7, var8, var9);
   }

   private static Decal tattoo(float var0, float var1, float var2, float var3, float var4, float var5) {
      Canvas var6 = new Canvas(64, 40);
      var6.fill(Shape2D.ellipse(44.0F, 20.0F, 9.0F, 14.0F).outline(4.5F), -2608065);
      var6.fill(Shape2D.line(20.0F, 7.0F, 20.0F, 34.0F, 4.5F), -2608065);
      var6.fill(Shape2D.line(20.0F, 7.0F, 14.0F, 13.0F, 4.0F), -2608065);
      float var7 = 0.22F;
      float var8 = var0 + (var3 - var0) * var7;
      float var9 = var1 + (var4 - var1) * var7;
      float var10 = var2 + (var5 - var2) * var7;
      return Decal.left(var6, var8 + 0.55F, var9, var10 + 0.05F, 1.0F, 0.62F).depth(0.6F, 0.6F).maxAngle(70.0F).flatLight(0.8F);
   }

   private static void hands(Rig var0, float var1, float var2, float var3) {
      float var4 = 4.3F;
      float var5 = 15.25F;
      float var6 = 0.6F;
      ArrayList var7 = new ArrayList();
      var7.add(Shapes.ellipsoid(var4, var5, var6, 0.33F, 0.6F, 0.52F));
      var7.add(Shapes.capsule(var4 - 0.02F, var5 + 0.45F, var6 - 0.12F, -var1, var2 + 0.1F, var3, 0.32F));
      var7.add(
         Shapes.strand(0.15F, 0.13F, var4 + 0.02F, var5 - 0.4F, var6 + 0.36F, var4 - 0.02F, var5 - 0.86F, var6 + 0.44F, var4 - 0.2F, var5 - 1.1F, var6 + 0.4F)
      );
      var7.add(
         Shapes.strand(0.15F, 0.13F, var4 + 0.03F, var5 - 0.44F, var6 + 0.12F, var4, var5 - 0.98F, var6 + 0.14F, var4 - 0.22F, var5 - 1.26F, var6 + 0.11F)
      );
      var7.add(Shapes.strand(0.14F, 0.12F, var4 + 0.03F, var5 - 0.42F, var6 - 0.12F, var4, var5 - 0.9F, var6 - 0.13F, var4 - 0.2F, var5 - 1.16F, var6 - 0.14F));
      var7.add(
         Shapes.strand(0.13F, 0.11F, var4 + 0.02F, var5 - 0.36F, var6 - 0.34F, var4, var5 - 0.74F, var6 - 0.36F, var4 - 0.16F, var5 - 0.95F, var6 - 0.35F)
      );
      var7.add(
         Shapes.strand(0.17F, 0.14F, var4 - 0.18F, var5 + 0.1F, var6 + 0.42F, var4 - 0.3F, var5 - 0.3F, var6 + 0.58F, var4 - 0.32F, var5 - 0.54F, var6 + 0.5F)
      );
      Part var8 = var0.extra("hand_l", Bone.LEFT_ARM)
         .shape(Shapes.blend(0.12F, var7.toArray(new Shape[0])).color(-8241))
         .resolution(0.45F)
         .detail(0.35F)
         .flatLight(1.0F)
         .occlusion(0.25F)
         .outline(-5740452, 0.1F);
      var0.extra("hand_r", Bone.RIGHT_ARM).mirrorOf(var8);
   }

   private static void legs(Rig var0) {
      float var1 = -1.1F;
      Shape var2 = Shapes.sphere(var1, 14.6F, -0.05F, 1.02F);
      Shape var3 = Shapes.cone(var1 - 0.05F, 14.3F, 0.0F, 1.02F, var1, 8.4F, 0.08F, 0.76F);
      Shape var4 = Shapes.sphere(var1, 7.9F, 0.1F, 0.72F);
      Strand var5 = Shapes.tube(var1, 7.6F, 0.08F, 0.72F, var1 - 0.02F, 5.8F, -0.12F, 0.8F, var1 - 0.04F, 3.6F, -0.02F, 0.56F, var1 - 0.05F, 2.0F, 0.0F, 0.48F);
      Shape var6 = Shapes.blend(0.45F, var2, var3, var4, var5);
      Shape var7 = var6.intersect(Shapes.above(10.6F)).paint(Paint.vertical(-1198942, 10.8F, -8241, 12.5F));
      Shape var8 = Shapes.cylinder(var1, 11.05F, 0.03F, var1, 10.35F, 0.05F, 0.9F, 0.12F).color(-13645638);
      Shape var9 = Shapes.blend(0.3F, var6.intersect(Shapes.below(10.4F)).inflate(0.09F), shoe(var1))
         .paint(Paint.field((var0x, var1x, var2x) -> Colors.mix(-13618373, -14934492, smooth(-0.2F, -0.8F, var2x) * 0.6F)));
      Part var10 = var0.part(Bone.RIGHT_LEG)
         .pivot(var1, 14.2F, 0.0F)
         .shape(var7)
         .detail(0.6F)
         .resolution(1.0F)
         .flatLight(0.8F)
         .outline(-5740452, 0.1F)
         .occlusion(0.3F)
         .swing(0.55F, 30.0F)
         .joint(var1, 7.9F, 0.1F, 0.8F);
      var0.part(Bone.LEFT_LEG).mirrorOf(var10);
      Part var11 = var0.extra("boot_r", Bone.RIGHT_LEG).shape(var9).detail(0.8F).resolution(1.0F).outline(-16119026, 0.11F).occlusion(0.3F);
      var0.extra("boot_l", Bone.LEFT_LEG).mirrorOf(var11);
      Part var12 = var0.extra("boot_trim_r", Bone.RIGHT_LEG)
         .shape(Shapes.union(var8, sole(var1)))
         .detail(0.4F)
         .resolution(0.6F)
         .flatLight(0.3F)
         .outline(-15835560, 0.09F);
      var0.extra("boot_trim_l", Bone.LEFT_LEG).mirrorOf(var12);
   }

   private static Shape shoe(float var0) {
      return Shapes.blend(
         0.4F,
         Shapes.ellipsoid(var0, 1.1F, 0.95F, 0.62F, 0.6F, 1.15F),
         Shapes.ellipsoid(var0, 1.4F, -0.35F, 0.62F, 0.9F, 0.72F),
         Shapes.cone(var0, 2.3F, -0.05F, 0.5F, var0, 1.2F, 0.0F, 0.6F)
      );
   }

   private static Shape sole(float var0) {
      Shape var1 = Shapes.box(var0, 0.62F, 0.85F, 0.64F, 0.14F, 1.3F, 0.12F);
      Shape var2 = Shapes.box(var0, 0.3F, -0.55F, 0.46F, 0.3F, 0.4F, 0.1F);
      return Shapes.blend(0.12F, var1, var2).intersect(Shapes.above(0.0F)).paint(Paint.vertical(-14971002, 0.0F, -13645638, 0.7F));
   }

   private static float px(float var0) {
      return 128.0F + var0 * 56.88889F;
   }

   private static float py(float var0) {
      return (29.95F - var0) * 56.88889F;
   }

   private static Decal face(float[][] var0) {
      Canvas var1 = new Canvas(88, 136);
      float var2 = 40.0F;
      float var3 = 70.0F;
      Shape2D var4 = Shape2D.ellipse(var2, var3, 27.5F, 33.0F);
      var1.fill(var4, Paint2D.linear(var2, var3 - 33.0F, var2, var3 + 10.0F, -1904912, -1));
      var1.fill(Shape2D.ellipse(var2, var3 - 34.0F, 34.0F, 22.0F).intersect(var4), -1715153190, 2.0F);
      Shape2D var5 = Shape2D.ellipse(var2, var3 + 3.5F, 20.5F, 27.5F);
      Shape2D var6 = var5.intersect(var4);
      var1.fill(var6, Paint2D.linear(var2, var3 - 24.0F, var2, var3 + 30.0F, -16106432, -14504024, -6491416));
      var1.fill(var5.outline(2.6F).intersect(var4), -1341506496);
      var1.fill(Shape2D.ellipse(var2, var3 + 1.5F, 9.0F, 12.5F), -16439768);
      var1.fill(Shape2D.ellipse(var2, var3 + 17.0F, 12.0F, 6.5F).intersect(var6), -2133655560, 3.0F);
      var1.fill(Shape2D.curve(var2 + 26.5F, var3 - 13.0F, var2 + 2.0F, var3 - 46.0F, var2 - 30.0F, var3 - 6.0F, 4.2F, 8.4F), -14800336);
      var1.fill(Shape2D.taper(var2 - 25.0F, var3 - 9.0F, 6.0F, var2 - 35.0F, var3 - 6.0F, 1.4F), -14800336);
      var1.fill(Shape2D.curve(var2 - 5.0F, var3 + 32.0F, var2 - 20.0F, var3 + 31.0F, var2 - 26.5F, var3 + 16.0F, 1.6F, 2.8F), -868591010);
      var1.fill(Shape2D.curve(var2 + 15.0F, var3 - 38.0F, var2, var3 - 48.0F, var2 - 23.0F, var3 - 33.0F, 1.5F, 1.5F), 2005965984);
      var1.fill(Shape2D.curve(var2 + 20.0F, var3 - 48.0F, var2 + 2.0F, var3 - 57.0F, var2 - 22.0F, var3 - 51.0F, 4.0F, 2.6F), -13989243);
      var1.fill(Shape2D.ellipse(var2 - 4.0F, var3 + 46.0F, 21.0F, 9.0F), 1442810524, 10.0F);
      Canvas var7 = new Canvas(256, 220);
      int var8 = Math.round(py(28.0F)) - 70;
      ArrayList var9 = new ArrayList();

      for (float[] var13 : var0) {
         float[] var14 = blade(var13);

         for (byte var15 = 0; var15 < var14.length; var15 += 2) {
            var14[var15] = px(var14[var15]);
            var14[var15 + 1] = py(var14[var15 + 1]) + 14.0F;
         }

         var9.add(Shape2D.polygon(var14));
      }

      var9.add(Shape2D.box(128.0F, py(29.75F), 130.0F, 14.222222F, 0.0F));
      var7.fill(Shape2D.union(var9.toArray(new Shape2D[0])), -1931434602, 3.0F);
      var7.draw(var1, 28, var8);
      var7.draw(var1.mirrored(), 140, var8);

      for (byte var16 = -1; var16 <= 1; var16 += 2) {
         float var17 = 128.0F + var16 * 60.0F;
         var7.fill(Shape2D.ellipse(var17 - 7.5F, var8 + 60.0F, 7.0F, 8.8F), -1);
         var7.fill(Shape2D.circle(var17 + 8.0F, var8 + 83.0F, 3.0F), -570425345);
         var7.fill(Shape2D.circle(var17 - 12.0F, var8 + 76.0F, 1.6F), -1426063361);
      }

      var7.fill(Shape2D.line(130.0F, var8 + 119.0F, 127.0F, var8 + 124.5F, 2.4F), -1713795968);
      var7.fill(
         Shape2D.path(3.0F, 3.0F, 118.0F, var8 + 156.0F, 123.0F, var8 + 161.0F, 128.0F, var8 + 157.5F, 133.0F, var8 + 161.0F, 138.0F, var8 + 156.0F), -6668728
      );
      return Decal.front(var7, 0.0F, ty(28.016407F), tz(2.1F), 5.04F, 4.33125F).depth(2.0F, 1.8F).maxAngle(72.0F).flatLight(1.0F).occlusion(0.3F);
   }

   private static float smooth(float var0, float var1, float var2) {
      float var3 = clamp01((var2 - var0) / (var1 - var0));
      return var3 * var3 * (3.0F - 2.0F * var3);
   }

   private static float clamp01(float var0) {
      return var0 < 0.0F ? 0.0F : Math.min(var0, 1.0F);
   }
}
