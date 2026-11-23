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
public final class Sakura extends CharacterDesign {
   private static final int SKIN = -9016;
   private static final int SKIN_SHADE = -1331558;
   private static final int SKIN_LINE = -5740452;
   private static final int HAIR_ROOT = -2592100;
   private static final int HAIR = -677434;
   private static final int HAIR_TIP = -12321;
   private static final int HAIR_SHADE = -3908470;
   private static final int HAIR_LINE = -6864801;
   private static final int SHINE = -855643406;
   private static final int WHITE = -263689;
   private static final int WHITE_SHADE = -2305053;
   private static final int SLEEVE_IN = -2502430;
   private static final int CLOTH_LINE = -7568744;
   private static final int RED = -2674372;
   private static final int RED_DARK = -5955793;
   private static final int RED_IN = -10087904;
   private static final int RED_LINE = -8514270;
   private static final int CORD = -1816774;
   private static final int GOLD = -1459895;
   private static final int BEAD = -2608577;
   private static final int STRAW = -1320012;
   private static final int SOLE = -10600144;
   private static final int PETAL = -8984;
   private static final int PETAL_CORE = -2078596;
   private static final float HAKAMA_TOP = 20.0F;
   private static final float HAKAMA_HEM = 2.5F;
   private static final float HS = 1.12F;
   private static final float DROP = 0.4F;
   private static final float HX = 0.0F;
   private static final float HY = 28.9F;
   private static final float HZ = -0.3F;
   private static final float PPU = 56.88889F;
   private static final float FACE_TOP = 29.95F;
   private static final float[] PLEATS = new float[]{0.21F, 0.7F, 2.76F};
   private static final float[][] LOCKS = new float[][]{
      {-0.08F, 28.38F, -2.0F, 0.95F, 0.12F, 0.34F},
      {-0.98F, 28.82F, -25.0F, 0.95F, 0.28F, 0.3F},
      {-1.92F, 28.5F, -47.0F, 0.92F, 0.26F, 0.3F},
      {0.92F, 28.9F, 24.0F, 0.95F, 0.28F, 0.3F},
      {1.9F, 28.62F, 46.0F, 0.92F, 0.25F, 0.3F},
      {-0.55F, 28.58F, -13.0F, 0.45F, 0.16F, 0.44F},
      {0.46F, 28.7F, 11.0F, 0.45F, 0.18F, 0.44F},
      {-1.46F, 28.72F, -36.0F, 0.42F, 0.2F, 0.42F},
      {1.42F, 28.8F, 35.0F, 0.42F, 0.2F, 0.42F}
   };
   private static final Shape2D SLEEVE_SIDE = Shape2D.polygon(
         0.85F,
         23.95F,
         0.62F,
         19.0F,
         0.12F,
         15.7F,
         -0.2F,
         14.7F,
         -1.2F,
         14.25F,
         -2.25F,
         14.0F,
         -2.9F,
         14.2F,
         -3.2F,
         14.8F,
         -3.25F,
         16.0F,
         -2.2F,
         22.4F,
         -1.0F,
         24.2F
      )
      .inflate(0.3F);

   public Sakura() {
      super("sakura", "Sakura");
   }

   @Override
   protected void define(Rig var1) {
      var1.scale(1.0F).outline(-11914176, 0.12F).occlusion(0.3F).flatLight(0.55F);
      float[][] var2 = lockPaths();
      Paint var3 = hairPaint();
      head(var1, var2);
      hair(var1, var3);
      fringe(var1, var2);
      kanzashi(var1);
      body(var1);
      hakama(var1);
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
      Shape var2 = Shapes.capsule(0.0F, 23.3F, -0.45F, 0.0F, 25.9F, -0.3F, 0.6F);
      float var3 = ty(25.82F);
      float var4 = tz(1.2F);
      Paint var5 = Paint.field((var2x, var3x, var4x) -> {
         float var5x = var3 + Math.max(0.0F, var4 - var4x) * 0.62F;
         float var6x = smooth(var5x + 0.05F, var5x - 0.2F, var3x) * (0.6F + 0.4F * smooth(23.4F, 24.8F, var3x));
         return Colors.mix(-9016, -1331558, var6x);
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
         int var4x = Colors.mix(Colors.mix(-2592100, -677434, 0.45F), -677434, smooth(30.9F, 29.7F, var2x));
         var4x = Colors.mix(var4x, -12321, 0.5F * smooth(29.5F, 28.5F, var2x));
         return Colors.mix(var4x, -3908470, 0.6F * (1.0F - smooth(0.12F, 0.42F, var9.distance(var1x, var2x, var3x))));
      });
      var0.extra("fringe", Bone.HEAD)
         .shape(up(var8.paint(var10)))
         .resolution(0.45F)
         .detail(1.05F)
         .flatLight(0.7F)
         .occlusion(0.1F)
         .outline(-6864801, 0.09F)
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
            -855643406
         );
         var1.fill(
            Shape2D.curve(140.0F + var8 * 2.0F, ringY(var8), 140.0F + var8 + var7, ringY((var8 + var7) * 0.5F), 140.0F + var7 * 2.0F, ringY(var7), var9, 1.0F),
            -855643406
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

      Shape var12 = Shapes.box(0.0F, 26.3F, 2.3F, 2.95F, 3.3F, 2.8F, 0.9F);
      Shape var13 = Shapes.blend(
            0.3F,
            Shapes.strand(0.62F, 0.44F, -2.3F, 29.6F, -0.05F, -2.48F, 28.1F, -0.12F, -2.45F, 26.5F, -0.22F).flatten(1.6F, 1.0F, 0.0F, 0.0F),
            Shapes.strand(0.68F, 0.44F, -2.25F, 29.5F, -0.95F, -2.46F, 27.9F, -1.05F, -2.45F, 26.3F, -1.12F).flatten(1.6F, 1.0F, 0.0F, 0.0F)
         )
         .mirrorX();
      Shape var14 = Shapes.blend(0.25F, Shapes.subtract(Shapes.blend(0.3F, var2.toArray(new Shape[0])), var12).paint(var1), var13.paint(var1.occlusion(0.35F)));
      var0.extra("hair", Bone.HEAD).shape(up(var14)).detail(1.1F).flatLight(0.55F).occlusion(0.22F).outline(-6864801, 0.09F).decal(shine());
      Strand var15 = Shapes.tube(
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
            -2.41F,
            25.9F,
            0.72F,
            0.3F,
            -2.3F,
            25.1F,
            0.74F,
            0.36F,
            -2.12F,
            24.7F,
            0.78F,
            0.32F
         )
         .flatten(1.8F, 1.0F, 0.0F, 0.3F);
      Strand var7 = Shapes.tube(
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
            -2.41F,
            25.9F,
            0.56F,
            0.3F,
            -2.28F,
            25.0F,
            0.54F,
            0.36F,
            -2.2F,
            24.35F,
            0.56F,
            0.32F
         )
         .flatten(1.8F, 1.0F, 0.0F, 0.15F);
      Shape var8 = Shapes.cylinder(-2.41F, 25.38F, 0.64F, -2.41F, 25.94F, 0.64F, 0.5F, 0.12F).color(-263689);
      Shape var9 = Shapes.union(Shapes.blend(0.25F, var15, var7).paint(var1), var8);
      Motion var10 = new Motion().idle(1.5F, 0.0F, 0.6F, 0.35F).walk(3.0F, 0.0F, 1.0F, 2.0F).drag(8.0F).hang(0.6F).spring(60.0F, 8.0F).bend(1.4F);
      Part var11 = var0.extra("sidelock_r", Bone.HEAD)
         .pivot(tx(-2.3F), ty(30.0F), tz(0.6F))
         .shape(up(var9))
         .motion(var10)
         .resolution(0.7F)
         .detail(0.6F)
         .flatLight(0.5F)
         .occlusion(0.35F)
         .outline(-6864801, 0.09F)
         .decal(tieCord());
      var0.extra("sidelock_l", Bone.HEAD).mirrorOf(var11);
      backHair(var0, var1);
   }

   private static Decal tieCord() {
      Canvas var0 = new Canvas(40, 20);
      var0.fill(Shape2D.box(20.0F, 10.0F, 20.0F, 2.8F, 0.0F), -1816774);
      var0.fill(Shape2D.ellipse(20.0F, 10.0F, 4.0F, 5.0F), -1816774);
      return Decal.wrap(var0, tx(-2.41F), ty(25.67F), tz(0.63F), 0.0F, 1.0F, 0.0F, -1.0F, 0.0F, 0.45F, 220.0F, 0.56F).radius(0.2F, 0.9F);
   }

   private static void backHair(Rig var0, Paint var1) {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = new ArrayList();

      for (int var4 = 0; var4 < 7; var4++) {
         float var5 = (var4 - 3.0F) / 3.0F;
         float var6 = 180.0F - var5 * 72.0F;
         float var7 = var4 % 2 == 0 ? 0.1F : 0.0F;
         float[] var8 = onHead(26.0F, var6, -0.02F);
         float[] var9 = onHead(60.0F, var6, 0.22F + var7);
         float[] var10 = onHead(88.0F, var6, 0.34F + var7);
         float[] var11 = onHead(114.0F, var6, 0.2F + var7);
         var2.add(
            Shapes.strand(0.78F, 0.72F, var8[0], var8[1], var8[2], var9[0], var9[1], var9[2], var10[0], var10[1], var10[2], var11[0], var11[1], var11[2])
               .flattenFrom(2.4F, 0.0F, 28.9F, -0.3F)
         );
         float[] var12 = onHead(104.0F, var6, 0.32F);
         var3.add(
            Shapes.strand(0.8F, 0.64F, var12[0], var12[1], var12[2], var5 * 2.85F, 25.0F, -2.52F - 0.3F * (1.0F - var5 * var5), var5 * 1.02F, 21.9F, -2.8F)
               .flatten(2.0F, 0.0F, 0.0F, 1.0F)
         );
      }

      ArrayList var13 = new ArrayList();
      float[] var14 = new float[]{14.1F, 13.72F, 13.58F, 13.78F, 14.16F};

      for (int var15 = 0; var15 < 5; var15++) {
         float var17 = (var15 - 2.0F) / 2.0F;
         var13.add(
            Shapes.strand(
                  0.8F, 0.14F, var17 * 0.72F, 22.3F, -2.85F, var17 * 1.02F, 18.9F, -3.08F, var17 * 0.84F, 16.0F, -3.25F, var17 * 0.2F, var14[var15], -3.32F
               )
               .flatten(1.6F, 0.0F, 0.0F, 1.0F)
         );
      }

      Shape var16 = Shapes.union(
            Shapes.blend(0.1F, var2.toArray(new Shape[0])), Shapes.blend(0.2F, var3.toArray(new Shape[0])), Shapes.blend(0.15F, var13.toArray(new Shape[0]))
         )
         .paint(var1);
      Shape var18 = Shapes.box(0.0F, 21.9F, -2.82F, 1.42F, 0.45F, 0.62F, 0.32F).color(-263689);
      Shape var19 = Shapes.box(0.0F, 21.9F, -2.82F, 1.5F, 0.2F, 0.7F, 0.17F).color(-1816774);
      Shape var20 = Shapes.ellipsoid(-1.4F, 22.2F, -3.5F, 1.2F, 0.66F, 0.38F)
         .paint(Paint.radial(-2305053, -0.3F, 22.0F, -3.6F, 1.6F, -263689))
         .rotate(0.0F, 0.0F, -16.0F, -1.4F, 22.2F, -3.5F);
      Shape var21 = Shapes.ellipsoid(0.0F, 22.0F, -3.58F, 0.48F, 0.52F, 0.36F).color(-1816774);
      Shape var22 = Shapes.strand(0.44F, 0.38F, -0.3F, 21.6F, -3.5F, -0.8F, 20.5F, -3.45F, -1.2F, 19.4F, -3.4F).flatten(2.2F, 0.0F, 0.0F, 1.0F).color(-263689);
      Shape var23 = Shapes.union(var18, var19, var20, var20.mirrored(), var21, var22, var22.mirrored());
      var0.extra("hair_back", Bone.HEAD)
         .pivot(0.0F, ty(29.0F), tz(-1.6F))
         .shape(up(Shapes.union(var16, var23)))
         .motion(Motion.hair().hang(0.0F).follow(Bone.BODY, 0.85F).drag(12.0F))
         .detail(0.58F)
         .flatLight(0.5F)
         .occlusion(0.35F)
         .outline(-6864801, 0.09F)
         .decal(tailLines());
   }

   private static Decal tailLines() {
      Canvas var0 = new Canvas(48, 108);
      float var1 = 12.0F;
      float var2 = 22.0F;
      int var3 = 1890076012;

      for (int var4 = 0; var4 < 4; var4++) {
         float var5 = (var4 - 1.5F) / 2.0F;
         float var6 = var5 * 0.72F;
         float var7 = var5 * 1.02F;
         float var8 = var5 * 0.84F;
         float var9 = var5 * 0.2F;
         float var10 = (var7 + var8) * 0.5F;
         float var11 = 14.6F + Math.abs(var5) * 0.4F;
         var0.fill(
            Shape2D.curve(
               24.0F - var1 * var6, (var2 - 21.4F) * var1, 24.0F - var1 * var7, (var2 - 18.9F) * var1, 24.0F - var1 * var10, (var2 - 17.45F) * var1, 0.4F, 1.5F
            ),
            var3
         );
         var0.fill(
            Shape2D.curve(
               24.0F - var1 * var10, (var2 - 17.45F) * var1, 24.0F - var1 * var8, (var2 - 16.0F) * var1, 24.0F - var1 * var9, (var2 - var11) * var1, 1.5F, 0.3F
            ),
            var3
         );
      }

      return Decal.back(var0, 0.0F, ty(var2 - 54.0F / var1), tz(-3.4F), 4.48F, 10.08F).depth(0.9F, 1.0F).maxAngle(70.0F);
   }

   private static void kanzashi(Rig var0) {
      Shape var1 = Shapes.union(
         blossom(1.6F).rotate(-22.0F, 42.0F, 0.0F).move(2.08F, 30.9F, 1.18F), blossom(0.95F).rotate(-12.0F, 70.0F, 0.0F).move(2.66F, 30.02F, 0.12F)
      );
      var0.extra("kanzashi", Bone.HEAD).shape(up(var1)).resolution(0.42F).detail(0.5F).flatLight(0.4F).outline(-6669732, 0.08F);
      ArrayList var2 = new ArrayList();
      float[][] var3 = new float[][]{{2.86F, 29.84F, 1.06F, 2.0F}, {3.2F, 29.74F, 0.52F, 3.0F}};

      for (int var4 = 0; var4 < var3.length; var4++) {
         float var5 = var3[var4][0];
         float var6 = var3[var4][1];
         float var7 = var3[var4][2];
         int var8 = (int)var3[var4][3];
         float var9 = var6 - 0.55F * var8;
         var2.add(Shapes.capsule(var5, var6 + 0.05F, var7, var5 + 0.03F, var9, var7 + 0.02F, 0.07F).color(-1459895));

         for (int var10 = 0; var10 < var8; var10++) {
            float var11 = var10 == var8 - 1 ? 0.23F : 0.17F;
            var2.add(Shapes.sphere(var5 + 0.01F * var10, var6 - 0.55F * (var10 + 1), var7, var11).color((var10 + var4) % 2 == 0 ? -2608577 : -1459895));
         }
      }

      var0.extra("bira", Bone.HEAD)
         .pivot(tx(3.08F), ty(29.86F), tz(0.8F))
         .shape(up(Shapes.union(var2.toArray(new Shape[0]))))
         .motion(Motion.hair().hang(1.0F).spring(60.0F, 6.0F))
         .resolution(0.4F)
         .detail(1.5F)
         .flatLight(0.5F)
         .outline(-8767416, 0.05F);
   }

   private static Shape blossom(float var0) {
      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < 5; var2++) {
         float var3 = 90.0F + 72.0F * var2;
         double var4 = Math.toRadians(var3);
         float var6 = (float)Math.cos(var4);
         float var7 = (float)Math.sin(var4);
         float var8 = var6 * 0.47F * var0;
         float var9 = var7 * 0.47F * var0;
         Shape var10 = Shapes.ellipsoid(var8, var9, 0.0F, 0.3F * var0, 0.44F * var0, 0.2F).rotate(0.0F, 0.0F, var3 - 90.0F, var8, var9, 0.0F);
         var1.add(Shapes.subtract(var10, Shapes.sphere(var6 * 0.93F * var0, var7 * 0.93F * var0, 0.0F, 0.12F * var0)));
      }

      Shape var11 = Shapes.union(var1.toArray(new Shape[0])).paint(Paint.radial(-2078596, 0.0F, 0.0F, 0.1F, 0.4F * var0, -8984));
      return Shapes.union(var11, Shapes.sphere(0.0F, 0.0F, 0.16F, 0.15F * var0).color(-1459895));
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
         int var6 = Colors.mix(-2592100, -677434, smooth(0.8F, 2.9F, var5));
         return Colors.mix(var6, -12321, smooth(5.5F, 17.5F, var5) * 0.9F);
      });
   }

   private static void body(Rig var0) {
      Shape var1 = Shapes.union(
         Shapes.capsule(0.0F, 23.95F, -0.15F, -2.55F, 23.3F, -0.15F, 0.95F), Shapes.capsule(0.0F, 23.95F, -0.15F, 2.55F, 23.3F, -0.15F, 0.95F)
      );
      Shape var2 = Shapes.ellipsoid(0.0F, 21.7F, 0.05F, 2.55F, 2.1F, 1.7F);
      Shape var3 = Shapes.ellipsoid(0.86F, 21.6F, 0.6F, 0.92F, 0.82F, 0.98F).mirrorX();
      Shape var4 = Shapes.ellipsoid(0.0F, 19.6F, 0.0F, 2.15F, 1.7F, 1.5F);
      Shape var5 = Shapes.ellipsoid(0.0F, 17.8F, 0.0F, 2.2F, 1.6F, 1.55F);
      Shape var6 = Shapes.blend(1.0F, var1, Shapes.blend(0.8F, var2, var3), var4, var5).intersect(Shapes.above(17.2F));
      Strand var7 = Shapes.strand(0.42F, 0.4F, 0.0F, 24.75F, -1.1F, -0.95F, 24.62F, -0.72F, -1.22F, 24.3F, 0.35F, -0.75F, 23.35F, 1.45F, 0.12F, 22.45F, 1.92F)
         .flattenFrom(2.2F, 0.0F, 22.5F, -1.2F);
      Strand var8 = Shapes.strand(
            0.42F, 0.4F, 0.0F, 24.75F, -1.1F, 0.95F, 24.62F, -0.72F, 1.22F, 24.3F, 0.35F, 0.75F, 23.35F, 1.5F, -0.3F, 22.15F, 2.02F, -1.35F, 20.6F, 1.88F
         )
         .flattenFrom(2.2F, 0.0F, 22.5F, -1.2F);
      Paint var9 = Paint.field((var0x, var1x, var2x) -> {
         float var3x = smooth(0.5F, 1.3F, var2x) * smooth(19.9F, 20.5F, var1x) * (1.0F - smooth(20.6F, 21.1F, var1x));
         return Colors.mix(-263689, -2305053, var3x * 0.55F);
      });
      var0.part(Bone.BODY).shape(Shapes.blend(0.14F, var6, var7, var8).paint(var9)).outline(-7568744, 0.1F).detail(0.85F).occlusion(0.2F).decal(collar());
      Shape var10 = Shapes.subtract(
         Shapes.ellipsoid(0.0F, 19.2F, 0.0F, 2.82F, 2.6F, 2.32F).intersect(Shapes.box(0.0F, 19.2F, 0.0F, 3.2F, 0.3F, 2.7F, 0.12F)),
         Shapes.ellipsoid(0.0F, 19.2F, 0.0F, 2.4F, 2.6F, 1.9F)
      );
      Shape var11 = Shapes.ellipsoid(0.0F, 19.22F, 2.36F, 0.36F, 0.36F, 0.24F);
      Shape var12 = Shapes.ellipsoid(-1.0F, 19.4F, 2.28F, 1.0F, 0.44F, 0.2F).rotate(0.0F, -8.0F, -12.0F, -1.0F, 19.4F, 2.28F);
      Strand var13 = Shapes.strand(0.27F, 0.22F, -0.12F, 19.0F, 2.38F, -0.42F, 18.1F, 2.44F, -0.64F, 17.2F, 2.48F).flatten(2.2F, 0.0F, 0.0F, 1.0F);
      Shape var14 = Shapes.union(var11, var12, var12.mirrored(), var13, var13.mirrored());
      var0.extra("himo", Bone.BODY).shape(Shapes.union(var10, var14).color(-263689)).resolution(0.6F).detail(0.36F).outline(-7568744, 0.09F);
   }

   private static Decal collar() {
      Canvas var0 = new Canvas(92, 78);
      float[] var1 = uv(0.0F, 25.1F);
      float[] var2 = uv(0.0F, 23.2F);
      var0.fill(
         Shape2D.polygon(uv(-0.95F, 25.1F, -0.84F, 24.49F, -0.41F, 23.59F, 0.02F, 23.16F, 0.4F, 23.59F, 0.84F, 24.49F, 0.95F, 25.1F)),
         Paint2D.linear(0.0F, var1[1], 0.0F, var2[1], -9016, -9016, -1331558)
      );
      segments(var0, 6.6F, -2674372, -0.9F, 25.1F, -0.84F, 24.49F, -0.41F, 23.59F, 0.05F, 23.12F);
      segments(var0, 6.6F, -2674372, 0.9F, 25.1F, 0.84F, 24.49F, 0.4F, 23.59F, -0.02F, 23.1F);
      float[] var3 = uv(0.02F, 23.12F);
      var0.fill(Shape2D.circle(var3[0], var3[1], 4.2F), -2674372);
      int var4 = 1484418696;
      segments(var0, 1.8F, var4, -1.65F, 24.25F, -1.6F, 24.11F, -1.13F, 23.16F, -0.6F, 22.6F);
      segments(var0, 1.8F, var4, 1.65F, 24.25F, 1.6F, 24.11F, 1.09F, 23.12F, 0.55F, 22.45F);
      return Decal.front(var0, 0.0F, 23.8F, 2.0F, 3.8333333F, 3.25F).depth(1.6F, 1.2F).maxAngle(86.0F).flatLight(0.9F).occlusion(0.4F);
   }

   private static float[] uv(float... var0) {
      float[] var1 = new float[var0.length];

      for (byte var2 = 0; var2 < var0.length; var2 += 2) {
         var1[var2] = (var0[var2] + 1.9166666F) * 24.0F;
         var1[var2 + 1] = (25.425F - var0[var2 + 1]) * 24.0F;
      }

      return var1;
   }

   private static void segments(Canvas var0, float var1, int var2, float... var3) {
      float[] var4 = uv(var3);

      for (byte var5 = 0; var5 + 3 < var4.length; var5 += 2) {
         var0.fill(Shape2D.line(var4[var5], var4[var5 + 1], var4[var5 + 2], var4[var5 + 3], var1), var2);
      }
   }

   private static void hakama(Rig var0) {
      Shape var1 = Shapes.extrude(Shape2D.polygon(-0.3F, 20.4F, 0.3F, 20.4F, 0.0F, 16.3F), 0.7F, 0.04F)
         .rotate(0.0F, 90.0F, 0.0F)
         .move(-2.95F, 0.0F, 0.0F)
         .color(-263689);
      Shape var2 = Shapes.subtract(Shapes.field(Sakura::hakamaField, -5.1F, 2.25F, -3.9F, 2.1F, 20.25F, 3.9F).paint(Paint.field(Sakura::hakamaColor)), var1);
      Motion var3 = new Motion().follow(Bone.RIGHT_LEG, 0.62F, 13.0F).drag(3.0F).hang(0.5F).spring(80.0F, 11.0F).bend(1.6F).limit(24.0F);
      Part var4 = var0.extra("hakama_r", Bone.BODY)
         .pivot(-1.0F, 19.6F, 0.0F)
         .shape(var2)
         .motion(var3)
         .resolution(1.1F)
         .detail(0.42F)
         .flatLight(0.35F)
         .occlusion(0.2F)
         .outline(-8514270, 0.12F);
      var4.decal(pleatLine(PLEATS[0])).decal(pleatLine(PLEATS[1]));
      var0.extra("hakama_l", Bone.BODY).mirrorOf(var4);
   }

   private static Decal pleatLine(float var0) {
      Canvas var1 = new Canvas(10, 96);
      var1.fill(Shape2D.taper(5.0F, 1.0F, 1.2F, 5.0F, 95.0F, 6.0F), Paint2D.linear(0.0F, 0.0F, 0.0F, 40.0F, 8262946, -1719790302));
      float var2 = (float)Math.sin(var0);
      float var3 = (float)Math.cos(var0);
      return Decal.wrap(var1, -1.14F, 8.0F, 0.0F, 1.15F, 17.5F, 0.0F, -var2, 0.0F, var3, 3.6F, 11.0F);
   }

   private static float hakamaField(float var0, float var1, float var2) {
      float var3 = clamp01((20.0F - var1) / 17.5F);
      float var4 = (float)Math.pow(var3, 0.8);
      float var5 = -(0.35F + 1.15F * var3);
      float var6 = 2.05F + 1.15F * var4;
      float var7 = 1.9F + 1.5F * var4;
      float var8 = var0 - var5;
      float var9 = ellipse(var8, var2, var6, var7);
      float var10 = (float)Math.atan2(-var8, var2);
      float var11 = 0.05F + 0.17F * var3;

      for (float var15 : PLEATS) {
         var9 -= var11 * pleat(var10 - var15);
      }

      float var16 = Math.max(var1 - 20.0F, 2.5F - var1);
      return smax(var9, var16, 0.2F) * 0.9F;
   }

   private static int hakamaColor(float var0, float var1, float var2) {
      float var3 = clamp01((20.0F - var1) / 17.5F);
      float var4 = (float)Math.pow(var3, 0.8);
      float var5 = var0 + 0.35F + 1.15F * var3;
      if (var1 < 2.8F && ellipse(var5, var2, 2.05F + 1.15F * var4, 1.9F + 1.5F * var4) < -0.2F) {
         return -10087904;
      } else {
         float var6 = (float)Math.atan2(-var5, var2);
         float var7 = 0.0F;

         for (float var11 : PLEATS) {
            float var12 = var6 - var11;
            if (var12 >= 0.0F && var12 < 0.16F) {
               var7 = Math.max(var7, 1.0F - smooth(0.02F, 0.16F, var12));
            }
         }

         int var13 = Colors.mix(-2674372, -5955793, smooth(9.0F, 2.5F, var1) * 0.45F);
         return Colors.mix(var13, -5955793, var7 * 0.35F * smooth(0.02F, 0.2F, var3));
      }
   }

   private static float pleat(float var0) {
      if (!(var0 < -0.4F) && !(var0 > 0.07F)) {
         return var0 < 0.0F ? smooth(-0.4F, 0.0F, var0) : 1.0F - smooth(0.0F, 0.07F, var0);
      } else {
         return 0.0F;
      }
   }

   private static float ellipse(float var0, float var1, float var2, float var3) {
      float var4 = var0 / var2;
      float var5 = var1 / var3;
      float var6 = (float)Math.sqrt(var4 * var4 + var5 * var5);
      float var7 = var0 / (var2 * var2);
      float var8 = var1 / (var3 * var3);
      float var9 = (float)Math.sqrt(var7 * var7 + var8 * var8);
      return var9 < 1.0E-6F ? -Math.min(var2, var3) : var6 * (var6 - 1.0F) / var9;
   }

   private static void arms(Rig var0) {
      float var1 = -3.62F;
      float var2 = 23.0F;
      float var3 = -0.12F;
      float var4 = -4.05F;
      float var5 = 19.45F;
      float var6 = -0.3F;
      float var7 = -4.28F;
      float var8 = 15.95F;
      float var9 = 0.45F;
      float var10 = (float)Math.sqrt((var7 - var4) * (var7 - var4) + (var8 - var5) * (var8 - var5) + (var9 - var6) * (var9 - var6));
      float var11 = (var7 - var4) / var10;
      float var12 = (var8 - var5) / var10;
      float var13 = (var9 - var6) / var10;
      Shape var14 = Shapes.blend(
         0.35F, Shapes.cone(var1, var2, var3, 0.66F, var4, var5, var6, 0.58F), Shapes.cone(var4, var5, var6, 0.58F, var7, var8, var9, 0.66F)
      );
      Shape var15 = Shapes.sphere(var7 + 0.3F * var11, var8 + 0.3F * var12, var9 + 0.3F * var13, 0.46F).color(-2502430);
      Part var16 = var0.part(Bone.RIGHT_ARM)
         .pivot(-3.5F, 23.25F, -0.1F)
         .grip(-4.32F, 15.05F, 0.72F)
         .shape(Shapes.subtract(0.08F, var14.color(-263689), var15))
         .resolution(0.7F)
         .detail(0.7F)
         .outline(-7568744, 0.1F)
         .swing(0.55F)
         .joint(var4, var5, var6, 0.6F);
      var0.part(Bone.LEFT_ARM).mirrorOf(var16);
      Shape var17 = Shapes.field(
            (var0x, var1x, var2x) -> {
               float var3x = SLEEVE_SIDE.distance(var2x, var1x);
               float var4x = -4.12F - 0.085F * (23.8F - var1x);
               float var5x = 0.3F
                  + 0.16F * clamp01((22.0F - var1x) / 8.0F)
                  + 0.26F * smooth(0.0F, 1.3F, -var3x)
                  + 0.07F * (float)Math.sin(var2x * 2.3F + 0.6F) * smooth(22.0F, 17.0F, var1x);
               return smax(var3x, Math.abs(var0x - var4x) - var5x, 0.28F);
            },
            -6.1F,
            13.2F,
            -3.9F,
            -3.3F,
            24.7F,
            1.6F
         )
         .paint(Paint.vertical(-1053966, 13.5F, -263689, 19.0F));
      Shape var18 = Shapes.union(
         Shapes.sphere(-5.0F, 14.14F, -3.42F, 0.23F).color(-1816774),
         Shapes.cone(-5.0F, 13.98F, -3.44F, 0.15F, -5.01F, 13.58F, -3.46F, 0.27F).paint(Paint.vertical(-4705748, 13.3F, -1816774, 13.9F))
      );
      Motion var19 = Motion.cloth().hang(0.7F).drag(8.0F).spring(40.0F, 5.0F).bend(1.3F).limit(35.0F);
      Part var20 = var0.extra("sleeve_r", Bone.RIGHT_ARM)
         .shape(Shapes.union(var17, var18))
         .motion(var19)
         .resolution(1.0F)
         .detail(0.7F)
         .outline(-7568744, 0.1F)
         .decal(sleeveTrim());
      var0.extra("sleeve_l", Bone.LEFT_ARM).mirrorOf(var20);
      hands(var0, var7, var8, var9);
   }

   private static void hands(Rig var0, float var1, float var2, float var3) {
      float var4 = 4.3F;
      float var5 = 15.25F;
      float var6 = 0.64F;
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
         .shape(Shapes.blend(0.12F, var7.toArray(new Shape[0])).color(-9016))
         .resolution(0.45F)
         .detail(0.35F)
         .flatLight(1.0F)
         .occlusion(0.25F)
         .outline(-5740452, 0.1F);
      var0.extra("hand_r", Bone.RIGHT_ARM).mirrorOf(var8);
   }

   private static Decal sleeveTrim() {
      Canvas var0 = new Canvas(64, 24);
      float var1 = 16.0F;
      float[] var2 = new float[]{-0.3F, 14.75F, -1.2F, 14.3F, -2.25F, 14.05F, -2.85F, 14.25F, -3.12F, 14.8F};

      for (byte var3 = 0; var3 + 3 < var2.length; var3 += 2) {
         var0.fill(
            Shape2D.line((var2[var3] + 3.75F) * var1, (15.3F - var2[var3 + 1]) * var1, (var2[var3 + 2] + 3.75F) * var1, (15.3F - var2[var3 + 3]) * var1, 3.0F),
            -1816774
         );
      }

      return Decal.right(var0, -6.1F, 15.3F - 12.0F / var1, -3.75F + 32.0F / var1, 64.0F / var1, 24.0F / var1).depth(1.3F, 1.2F).maxAngle(48.0F);
   }

   private static void legs(Rig var0) {
      float var1 = -1.3F;
      Shape var2 = Shapes.blend(
         0.35F,
         Shapes.sphere(var1, 1.62F, -0.12F, 0.52F),
         Shapes.ellipsoid(var1, 0.98F, -0.45F, 0.56F, 0.54F, 0.62F),
         Shapes.ellipsoid(var1, 0.84F, 0.55F, 0.6F, 0.43F, 1.22F),
         Shapes.ellipsoid(var1 + 0.33F, 0.76F, 1.57F, 0.25F, 0.31F, 0.43F),
         Shapes.ellipsoid(var1 - 0.18F, 0.73F, 1.47F, 0.37F, 0.29F, 0.46F)
      );
      Shape var3 = Shapes.box(var1 + 0.1F, 0.8F, 1.97F, 0.05F, 0.4F, 0.55F, 0.02F).color(-2305053);
      Shape var4 = Shapes.cone(var1, 1.7F, -0.1F, 0.5F, var1 + 0.02F, 3.3F, -0.08F, 0.54F);
      Shape var5 = Shapes.subtract(Shapes.blend(0.3F, var2, var4).color(-263689), var3);
      Shape var6 = Shapes.box(var1, 0.22F, 0.52F, 0.74F, 0.22F, 1.64F, 0.2F).paint(Paint.field((var0x, var1x, var2x) -> var1x > 0.36F ? -1320012 : -10600144));
      Shape var7 = Shapes.union(
            Shapes.strand(0.2F, 0.2F, var1 + 0.13F, 0.5F, 1.5F, var1 + 0.13F, 0.95F, 1.3F),
            Shapes.strand(0.2F, 0.2F, var1 + 0.13F, 0.95F, 1.3F, var1 - 0.35F, 1.08F, 0.85F, var1 - 0.62F, 0.55F, 0.35F),
            Shapes.strand(0.2F, 0.2F, var1 + 0.13F, 0.95F, 1.3F, var1 + 0.35F, 1.08F, 0.85F, var1 + 0.6F, 0.55F, 0.35F)
         )
         .color(-1816774);
      Shape var8 = Shapes.union(var5, var6, var7).rotate(0.0F, -7.0F, 0.0F, var1, 0.0F, -0.1F);
      Part var9 = var0.part(Bone.RIGHT_LEG)
         .pivot(var1, 14.2F, 0.0F)
         .shape(var8)
         .resolution(0.6F)
         .detail(0.5F)
         .flatLight(0.4F)
         .outline(-10860992, 0.1F)
         .swing(0.38F, 18.0F);
      var0.part(Bone.LEFT_LEG).mirrorOf(var9);
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
      var1.fill(var4, Paint2D.linear(var2, var3 - 33.0F, var2, var3 + 10.0F, -923920, -1));
      var1.fill(Shape2D.ellipse(var2, var3 - 34.0F, 34.0F, 22.0F).intersect(var4), -1713846054, 2.0F);
      Shape2D var5 = Shape2D.ellipse(var2, var3 + 3.5F, 20.5F, 27.5F);
      Shape2D var6 = var5.intersect(var4);
      var1.fill(var6, Paint2D.linear(var2, var3 - 24.0F, var2, var3 + 30.0F, -11923926, -2996102, -19508));
      var1.fill(var5.outline(2.6F).intersect(var4), -1337323990);
      var1.fill(Shape2D.ellipse(var2, var3 + 1.5F, 9.0F, 12.5F), -14023146);
      var1.fill(Shape2D.ellipse(var2, var3 + 17.0F, 12.0F, 6.5F).intersect(var6), -2130716956, 3.0F);
      var1.fill(Shape2D.curve(var2 + 26.5F, var3 - 13.0F, var2 + 2.0F, var3 - 46.0F, var2 - 30.0F, var3 - 6.0F, 4.2F, 8.4F), -13757408);
      var1.fill(Shape2D.taper(var2 - 25.0F, var3 - 9.0F, 6.0F, var2 - 35.0F, var3 - 6.0F, 1.4F), -13757408);
      var1.fill(Shape2D.curve(var2 - 5.0F, var3 + 32.0F, var2 - 20.0F, var3 + 31.0F, var2 - 26.5F, var3 + 16.0F, 1.6F, 2.8F), -864407480);
      var1.fill(Shape2D.curve(var2 + 15.0F, var3 - 38.0F, var2, var3 - 48.0F, var2 - 23.0F, var3 - 33.0F, 1.5F, 1.5F), 2009102984);
      var1.fill(Shape2D.curve(var2 + 20.0F, var3 - 48.0F, var2 + 2.0F, var3 - 57.0F, var2 - 22.0F, var3 - 51.0F, 4.0F, 2.6F), -4892806);
      var1.fill(Shape2D.ellipse(var2 - 4.0F, var3 + 46.0F, 21.0F, 9.0F), 1728023196, 10.0F);
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
      var7.fill(Shape2D.union(var9.toArray(new Shape2D[0])), -1930845035, 3.0F);
      var7.draw(var1, 28, var8);
      var7.draw(var1.mirrored(), 140, var8);

      for (byte var16 = -1; var16 <= 1; var16 += 2) {
         float var17 = 128.0F + var16 * 60.0F;
         var7.fill(Shape2D.ellipse(var17 - 7.5F, var8 + 60.0F, 7.0F, 8.8F), -1);
         var7.fill(Shape2D.circle(var17 + 8.0F, var8 + 83.0F, 3.0F), -570425345);
         var7.fill(Shape2D.circle(var17 - 12.0F, var8 + 76.0F, 1.6F), -1426063361);
      }

      var7.fill(Shape2D.line(130.0F, var8 + 119.0F, 127.0F, var8 + 124.5F, 2.4F), -1713795968);
      var7.fill(Shape2D.curve(119.0F, var8 + 157.0F, 128.0F, var8 + 163.0F, 137.0F, var8 + 157.0F, 2.6F, 2.6F), -4896678);
      return Decal.front(var7, 0.0F, ty(28.016407F), tz(2.1F), 5.04F, 4.33125F).depth(2.0F, 1.8F).maxAngle(72.0F).flatLight(1.0F).occlusion(0.3F);
   }

   private static float smax(float var0, float var1, float var2) {
      float var3 = clamp01(0.5F - 0.5F * (var1 - var0) / var2);
      return var1 + (var0 - var1) * var3 + var2 * var3 * (1.0F - var3);
   }

   private static float smooth(float var0, float var1, float var2) {
      float var3 = clamp01((var2 - var0) / (var1 - var0));
      return var3 * var3 * (3.0F - 2.0F * var3);
   }

   private static float clamp01(float var0) {
      return var0 < 0.0F ? 0.0F : Math.min(var0, 1.0F);
   }
}
