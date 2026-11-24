package net.efn.client.module.impl.render.character.designs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
public final class Aoi extends CharacterDesign {
   private static final float TAU = 6.2831855F;
   private static final float NECK = 24.4F;
   private static final float HEAD_K = 1.16F;
   private static final float HEAD_DY = -0.25F;
   private static final float FACE_X = 2.25F;
   private static final float FACE_TOP = 28.6F;
   private static final float FACE_LOW = 26.08F;
   private static final float FACE_PX = 80.0F;
   private static final float YOKE = 21.75F;
   private static final float BEND = 13.0F;
   private static final int SKIN = -9274;
   private static final int SKIN_SHADE = -1331558;
   private static final int SKIN_LINE = -6657438;
   private static final int HAIR = -14140794;
   private static final int HAIR_DEEP = -15063975;
   private static final int HAIR_TOP = -12756032;
   private static final int HAIR_BLUE = -12948262;
   private static final int HAIR_TIP = -13246209;
   private static final int HAIR_INNER = -13646610;
   private static final int HAIR_SHINE = -7363350;
   private static final int HAIR_LINE = -15327158;
   private static final int CYAN = -13703425;
   private static final int NAVY = -14932394;
   private static final int JACKET = -12959662;
   private static final int JACKET_DARK = -14210250;
   private static final int JACKET_WHITE = -1446155;
   private static final int JACKET_SHADE = -3945254;
   private static final int LINE = -15065558;
   private static final int TOP = -525828;
   private static final int TOP_SHADE = -2498584;
   private static final int SKIRT = -12892555;
   private static final int SKIRT_DARK = -14472632;
   private static final int BLACK = -15263457;
   private static final int SILVER = -3550757;
   private static final int SOCK = -14012606;
   private static final int SOLE = -920586;
   private static final float SKIRT_ZC = -0.05F;
   private static final float SKIRT_TOP = 17.3F;
   private static final float SKIRT_HEM = 11.25F;
   private static final int PLEATS = 18;
   private static final float[][] MAIN_LOCKS = new float[][]{
      {0.95F, -1.55F, 29.85F, -2.0F, 29.5F, -2.3F, 28.45F, -2.24F, 27.65F, -2.0F, 27.42F},
      {0.95F, -1.15F, 29.95F, -1.38F, 29.5F, -1.6F, 28.6F, -1.55F, 28.12F, -1.3F, 27.93F},
      {0.85F, -0.64F, 29.95F, -0.45F, 29.4F, -0.08F, 28.25F, 0.16F, 27.4F, 0.04F, 27.02F},
      {1.0F, -0.08F, 30.0F, 0.35F, 29.6F, 0.9F, 28.7F, 1.2F, 28.22F, 0.98F, 28.0F},
      {1.05F, 0.56F, 30.0F, 1.15F, 29.7F, 1.8F, 28.75F, 2.08F, 27.8F, 1.85F, 27.45F}
   };
   private static final float[][] ACCENT_LOCKS = new float[][]{
      {0.55F, -0.95F, 29.85F, -1.0F, 29.3F, -0.9F, 28.5F, -0.76F, 28.3F},
      {0.55F, -0.22F, 29.85F, 0.14F, 29.35F, 0.52F, 28.72F, 0.44F, 28.55F},
      {0.5F, 0.5F, 29.85F, 1.02F, 29.4F, 1.5F, 28.5F, 1.44F, 28.2F}
   };

   public Aoi() {
      super("aoi", "Aoi");
   }

   @Override
   protected void define(Rig rig) {
      rig.scale(1.0F).outline(-15065558, 0.13F).occlusion(0.35F);
      Canvas var2 = stripe();
      Shape var3 = headShape();
      List var4 = fringeLocks(var3, MAIN_LOCKS, 0.2F, 0.17F);
      List var5 = fringeLocks(var3, ACCENT_LOCKS, 0.2F, 0.17F);
      rig.part(Bone.HEAD)
         .pivot(0.0F, 24.4F, 0.0F)
         .shape(grow(var3.color(-9274)))
         .detail(1.1F)
         .resolution(0.85F)
         .outline(-6657438, 0.09F)
         .flatLight(1.0F)
         .occlusion(0.2F)
         .decal(face(var4));
      hair(rig, var4, var5);
      ponytail(rig);
      Shape var6 = body(rig, var2);
      skirt(rig, var2);
      arms(rig, var2);
      legs(rig);
      gear(rig, var6);
   }

   private static Shape grow(Shape shape) {
      return shape.scale(1.16F, 0.0F, 24.4F, 0.0F).move(0.0F, -0.25F, 0.0F);
   }

   private static float hy(float f) {
      return 24.4F + (f - 24.4F) * 1.16F + -0.25F;
   }

   private static Shape headShape() {
      Shape var0 = Shapes.ellipsoid(0.0F, 28.52F, -0.15F, 2.28F, 2.24F, 2.32F);
      Shape var1 = Shapes.ellipsoid(0.0F, 27.4F, 0.7F, 1.95F, 1.1F, 1.45F);
      Shape var2 = Shapes.cone(0.0F, 27.25F, 0.42F, 1.68F, 0.0F, 26.15F, 1.18F, 0.55F);
      Shape var3 = Shapes.extrude(
         Shape2D.polygon(
            -2.6F,
            31.5F,
            -2.6F,
            29.0F,
            -2.24F,
            27.6F,
            -2.11F,
            27.2F,
            -1.9F,
            26.8F,
            -1.62F,
            26.44F,
            -1.28F,
            26.12F,
            -0.92F,
            25.91F,
            -0.54F,
            25.79F,
            -0.25F,
            25.75F,
            0.0F,
            25.74F,
            0.25F,
            25.75F,
            0.54F,
            25.79F,
            0.92F,
            25.91F,
            1.28F,
            26.12F,
            1.62F,
            26.44F,
            1.9F,
            26.8F,
            2.11F,
            27.2F,
            2.24F,
            27.6F,
            2.6F,
            29.0F,
            2.6F,
            31.5F
         ),
         4.0F,
         0.0F
      );
      Shape var4 = Shapes.blend(0.9F, var0, var1, var2);
      Shape var5 = var4.intersect(0.3F, var3);
      return Shapes.field((f, g, h) -> {
         float var5x = g > 28.8F ? var4.distance(f, g, h) : var5.distance(f, g, h);
         return Math.max(var5x, Math.max(-1.0F - h, g - 30.1F));
      }, var4.minX(), var4.minY(), -1.1F, var4.maxX(), 30.2F, var4.maxZ());
   }

   private static List<float[]> fringeLocks(Shape shape, float[][] fs, float f, float g) {
      ArrayList var4 = new ArrayList();

      for (float[] var8 : fs) {
         int var9 = (var8.length - 1) / 2;
         float[] var10 = new float[var9 * 4];

         for (int var11 = 0; var11 < var9; var11++) {
            float var12 = var8[1 + var11 * 2];
            float var13 = var8[2 + var11 * 2];
            float var14 = 1.0F - var12 * var12 / 6.76F - (var13 - 28.62F) * (var13 - 28.62F) / 7.08F;
            float var15 = Math.max(frontZ(shape, var12, var13, 5.0F), -0.5F + 2.64F * (float)Math.sqrt(Math.max(0.0F, var14)));
            float var17 = var13 - 28.3F;
            float var18 = var15 + 0.4F;
            float var19 = (float)Math.sqrt(var12 * var12 + var17 * var17 + var18 * var18);
            float var20 = var11 / (var9 - 1.0F);
            float var21 = (var11 == 0 ? 0.1F : (var11 == var9 - 1 ? g : f)) + 0.22F * smooth(1.3F, 2.2F, Math.abs(var12));
            var10[var11 * 4] = var12 + var12 / var19 * var21;
            var10[1 + var11 * 4] = var13 + var17 / var19 * var21;
            var10[2 + var11 * 4] = var15 + var18 / var19 * var21;
            var10[3 + var11 * 4] = var8[0] * (float)Math.pow(1.0F - var20, 1.2000000476837158) + 0.05F * var20;
         }

         var4.add(var10);
      }

      return var4;
   }

   private static void hair(Rig rig, List<float[]> list, List<float[]> list2) {
      ArrayList var3 = new ArrayList();
      var3.add(
         Shapes.subtract(0.35F, Shapes.ellipsoid(0.0F, 28.62F, -0.2F, 2.6F, 2.66F, 2.64F), Shapes.ellipsoid(0.0F, 26.75F, 2.75F, 2.45F, 2.85F, 2.72F))
            .intersect(Shapes.above(26.0F))
      );

      for (float var7 : new float[]{-155.0F, -118.0F, -80.0F, 80.0F, 118.0F, 155.0F}) {
         var3.add(flowLock(var7));
      }

      rig.extra("hair", Bone.HEAD)
         .shape(grow(Shapes.union(var3.toArray(new Shape[0])).paint(capPaint())))
         .detail(0.8F)
         .resolution(0.95F)
         .outline(-15327158, 0.1F)
         .flatLight(0.5F)
         .occlusion(0.4F);
      Part var10 = rig.extra("fringe", Bone.HEAD)
         .shape(grow(fringe(list, list2)))
         .detail(0.4F)
         .resolution(0.55F)
         .outline(-15327158, 0.09F)
         .flatLight(0.5F)
         .occlusion(0.2F);
      shine(var10, list);
      Motion var11 = Motion.hair()
         .idle(1.5F, 0.0F, 1.5F, 0.35F)
         .walk(3.0F, 0.0F, 3.0F, 2.0F)
         .drag(8.0F)
         .hang(0.7F)
         .turn(10.0F, 0.0F)
         .spring(60.0F, 7.0F)
         .bend(1.3F);
      Paint var12 = Paint.field((f, g, h) -> Colors.mix(Colors.mix(-14140794, -12948262, smooth(28.2F, 25.8F, g)), -13246209, smooth(26.2F, 24.4F, g)));
      Paint var13 = Paint.field((f, g, h) -> Colors.mix(-13646610, -13246209, smooth(27.0F, 25.0F, g)));
      Shape var8 = Shapes.union(
         Shapes.tube(
               -1.55F,
               29.95F,
               1.0F,
               0.4F,
               -2.32F,
               28.5F,
               1.4F,
               0.55F,
               -2.55F,
               27.1F,
               1.3F,
               0.52F,
               -2.48F,
               25.9F,
               1.15F,
               0.42F,
               -2.3F,
               24.9F,
               1.2F,
               0.28F,
               -2.1F,
               24.3F,
               1.34F,
               0.04F
            )
            .flattenFrom(2.0F, 0.0F, 27.5F, -0.5F)
            .paint(var12),
         Shapes.tube(
               -1.7F, 29.7F, 0.7F, 0.36F, -2.28F, 28.2F, 1.05F, 0.44F, -2.36F, 26.8F, 0.95F, 0.42F, -2.22F, 25.6F, 0.9F, 0.3F, -2.05F, 24.55F, 1.0F, 0.04F
            )
            .flattenFrom(2.0F, 0.0F, 27.5F, -0.5F)
            .paint(var13)
      );
      Part var9 = rig.extra("lock_r", Bone.HEAD)
         .pivot(-2.32F, hy(29.2F), 1.16F)
         .shape(grow(var8))
         .motion(var11)
         .detail(0.7F)
         .resolution(0.7F)
         .outline(-15327158, 0.1F)
         .flatLight(0.5F)
         .occlusion(0.35F);
      rig.extra("lock_l", Bone.HEAD).mirrorOf(var9);
   }

   private static Shape flowLock(float f) {
      float var1 = 0.0F;
      float var2 = 28.62F;
      float var3 = -0.2F;
      float var4 = 2.6F;
      float var5 = 2.66F;
      float var6 = 2.64F;
      float var7 = (float)Math.toRadians(f);
      float var8 = Math.abs(f);
      float var9 = 29.1F - 1.7F * smooth(20.0F, 90.0F, var8) - 1.2F * smooth(95.0F, 160.0F, var8);
      float var10 = (float)Math.asin(Math.max(-0.95F, Math.min(0.95F, (var9 - var2) / var5)));
      float var11 = (float)(Math.sin(var7) * Math.cos(var10));
      float var12 = (float)Math.sin(var10);
      float var13 = (float)(Math.cos(var7) * Math.cos(var10));
      float var14 = 0.0F;
      float var15 = 30.25F - var2;
      float var16 = -1.95F - var3;
      float var17 = (float)Math.sqrt(var14 * var14 + var15 * var15 + var16 * var16);
      var14 /= var17;
      var15 /= var17;
      var16 /= var17;
      float[] var18 = new float[]{0.0F, 0.35F, 0.7F, 1.0F};
      float[] var19 = new float[]{0.5F, 0.56F, 0.5F, 0.32F};
      float[] var20 = new float[16];

      for (int var21 = 0; var21 < 4; var21++) {
         float var22 = var11 + (var14 - var11) * var18[var21];
         float var23 = var12 + (var15 - var12) * var18[var21];
         float var24 = var13 + (var16 - var13) * var18[var21];
         float var25 = (float)Math.sqrt(var22 * var22 + var23 * var23 + var24 * var24);
         float var31;
         float var32;
         float var33;
         float var26 = 1.0F
            / (float)Math.sqrt(
               (var31 = var22 / var25) * var31 / (var4 * var4)
                  + (var32 = var23 / var25) * var32 / (var5 * var5)
                  + (var33 = var24 / var25) * var33 / (var6 * var6)
            );
         float var27 = var21 == 3 ? -0.1F : 0.04F;
         var20[var21 * 4] = var1 + var31 * (var26 + var27);
         var20[var21 * 4 + 1] = var2 + var32 * (var26 + var27);
         var20[var21 * 4 + 2] = var3 + var33 * (var26 + var27);
         var20[var21 * 4 + 3] = var19[var21];
      }

      return Shapes.tube(var20).flattenFrom(2.4F, var1, var2, var3);
   }

   private static Paint capPaint() {
      return Paint.field((f, g, h) -> Colors.mix(Colors.mix(-15063975, -14140794, smooth(26.2F, 28.4F, g)), -12756032, smooth(29.4F, 31.2F, g) * 0.8F));
   }

   private static Shape fringe(List<float[]> list, List<float[]> list2) {
      Shape[] var2 = new Shape[list.size() + list2.size()];

      for (int var3 = 0; var3 < var2.length; var3++) {
         boolean var4 = var3 >= list.size();
         float[] var5 = var4 ? (float[])list2.get(var3 - list.size()) : (float[])list.get(var3);
         int var6 = var5.length / 4;
         float[] var7 = new float[var6 * 2];

         for (int var8 = 0; var8 < var6; var8++) {
            var7[var8 * 2] = var5[var8 * 4];
            var7[var8 * 2 + 1] = var5[var8 * 4 + 1];
         }

         float var9 = var4 ? 1.12F : ((var3 & 1) == 0 ? 0.93F : 1.04F);
         var2[var3] = Shapes.tube(var5)
            .flattenFrom(2.6F, 0.0F, 28.4F, -0.3F)
            .intersect(Shapes.extrude(Shape2D.path(var5[3] * 2.6F, 0.02F, var7), 4.0F, 0.0F))
            .paint(
               Paint.field(
                  (g, h, i) -> Colors.shade(
                     Colors.mix(Colors.mix(-14140794, -12756032, smooth(29.6F, 30.6F, h) * 0.5F), -12948262, smooth(28.9F, 27.4F, h) * 0.85F), var9
                  )
               )
            );
      }

      return Shapes.union(var2);
   }

   private static void shine(Part part, List<float[]> list) {
      for (int var2 = 0; var2 < list.size(); var2++) {
         float[] var3 = (float[])list.get(var2);
         int var4 = var3.length / 4;

         for (int var5 = 1; var5 < var4; var5++) {
            float var6 = var3[var5 * 4 - 3];
            float var7 = var3[var5 * 4 + 1];
            if (!(var6 < 29.15F) && !(var7 > 29.15F)) {
               float var8 = (var6 - 29.15F) / (var6 - var7);
               float var9 = var3[var5 * 4 - 4] + (var3[var5 * 4] - var3[var5 * 4 - 4]) * var8;
               float var10 = var3[var5 * 4 - 2] + (var3[var5 * 4 + 2] - var3[var5 * 4 - 2]) * var8;
               float var11 = var3[var5 * 4 - 1] + (var3[var5 * 4 + 3] - var3[var5 * 4 - 1]) * var8;
               float var12 = (float)Math.atan2(var9, var10 + 0.2F);
               if (!(Math.abs(var12) > 0.96F)) {
                  float var13 = var11 * 1.1F / (float)Math.sqrt(var9 * var9 + (var10 + 0.2F) * (var10 + 0.2F));
                  float var14 = 7.0F + (float)Math.toDegrees(var13) * 0.25F;
                  float var15 = 64.0F / (var13 * 1.5F);
                  float[] var16 = new float[6];
                  float[] var17 = new float[6];

                  for (int var18 = 0; var18 < 3; var18++) {
                     float var19 = var13 * 0.6F * (var18 * 0.5F - 1.0F);
                     float var20 = var13 * 0.6F * var18 * 0.5F;
                     var16[var18 * 2] = 32.0F + var19 * var15;
                     var16[var18 * 2 + 1] = 13.0F + 25.0F * ((float)Math.cos(var12) - (float)Math.cos(var12 + var19));
                     var17[var18 * 2] = 32.0F + var20 * var15;
                     var17[var18 * 2 + 1] = 13.0F + 25.0F * ((float)Math.cos(var12) - (float)Math.cos(var12 + var20));
                  }

                  Canvas var21 = new Canvas(64, 26);
                  var21.fill(Shape2D.path(var14 * 0.2F, var14, var16).union(Shape2D.path(var14, var14 * 0.2F, var17)), Colors.withAlpha(-7363350, 0.5F), 5.0F);
                  float var22 = hy(29.1F) + 0.081F - 0.29F * (1.0F - (float)Math.cos(var12)) + ((var2 & 1) * 2 - 1) * 0.03F;
                  part.decal(
                     Decal.wrap(
                           var21,
                           0.0F,
                           var22,
                           -0.232F,
                           0.0F,
                           1.0F,
                           0.0F,
                           (float)Math.sin(var12),
                           0.0F,
                           (float)Math.cos(var12),
                           (float)Math.toDegrees(var13 * 1.5F),
                           0.30159998F
                        )
                        .radius(1.8F, 4.2F)
                        .flatLight(0.8F)
                  );
               }
            }
         }
      }
   }

   private static void ponytail(Rig rig) {
      float[][] var2 = new float[][]{
         {30.4F, -2.0F},
         {31.1F, -2.5F},
         {31.35F, -3.25F},
         {30.95F, -3.95F},
         {29.7F, -4.3F},
         {27.6F, -4.38F},
         {25.1F, -4.22F},
         {22.5F, -4.0F},
         {19.9F, -3.8F},
         {17.5F, -3.68F},
         {15.3F, -3.74F}
      };
      int var3 = var2.length;
      float[] var4 = new float[]{0.0F, 0.55F, 1.0F, 1.18F, 1.22F, 1.18F, 1.08F, 0.96F, 0.86F, 0.98F, 1.35F};
      float[] var5 = new float[]{0.45F, 0.88F, 1.05F, 1.1F, 1.1F, 1.05F, 0.98F, 0.86F, 0.7F, 0.52F, 0.34F};
      float[] var6 = new float[var3];
      float[] var7 = new float[var3];

      for (int var8 = 0; var8 < var3; var8++) {
         int var9 = Math.max(0, var8 - 1);
         int var10 = Math.min(var3 - 1, var8 + 1);
         float var11 = var2[var10][0] - var2[var9][0];
         float var12 = var2[var10][1] - var2[var9][1];
         float var13 = (float)Math.sqrt(var11 * var11 + var12 * var12);
         var6[var8] = var12 / var13;
         var7[var8] = -var11 / var13;
      }

      float[][] var23 = new float[][]{
         {-1.02F, 0.25F, 10.1F, 0.68F, 0.0F, -1.0F},
         {-0.5F, -0.3F, 10.25F, 0.72F, 1.1F, -1.0F},
         {0.02F, 0.3F, 10.8F, 0.74F, 2.3F, 0.0F},
         {0.54F, -0.28F, 9.7F, 0.72F, 0.6F, 1.0F},
         {1.03F, 0.22F, 9.55F, 0.66F, 1.7F, 1.0F},
         {-0.72F, 0.62F, 9.4F, 0.62F, 2.9F, -1.0F},
         {0.74F, 0.6F, 9.5F, 0.62F, 0.3F, 1.0F},
         {-0.05F, -0.62F, 10.6F, 0.64F, 2.0F, 0.0F}
      };
      ArrayList var24 = new ArrayList();

      for (float[] var34 : var23) {
         int var14 = Math.min((int)Math.floor(var34[2]), var3 - 1);
         float var15 = var34[2] - var14;
         float[] var16 = new float[(var14 + 2) * 4];

         for (int var17 = 0; var17 <= var14; var17++) {
            float var18 = var4[var17];
            float var19 = smooth(6.5F, 8.6F, var17);
            float var20 = 0.14F * (float)Math.sin(var17 * 0.9F + var34[4]) * smooth(1.0F, 4.0F, var17) * (1.0F - var19);
            float var21 = var34[0] * var18 + var20;
            float var22 = var34[1] * var18 * 0.75F * (1.0F - 0.6F * var19);
            var16[var17 * 4] = var21 + (var34[5] * 1.15F * var18 + var34[0] * 0.2F * var18 - var21) * var19;
            var16[var17 * 4 + 1] = var2[var17][0] + var6[var17] * var22;
            var16[var17 * 4 + 2] = var2[var17][1] + var7[var17] * var22;
            var16[var17 * 4 + 3] = var34[3] * 1.08F * var5[var17];
         }

         int var38 = (var14 + 1) * 4;
         int var39 = Math.min(var14 + 1, var3 - 1);
         float var40 = Math.max(var15, 0.45F);
         var16[var38] = var16[var14 * 4] * 1.12F + var34[5] * 0.2F;
         var16[var38 + 1] = var2[var14][0] + (var2[var39][0] - var2[var14][0]) * var40 - 1.1F;
         var16[var38 + 2] = var16[var14 * 4 + 2] + (var2[var39][1] - var2[var14][1]) * var40 - 0.1F;
         var16[var38 + 3] = 0.04F;
         split(var24, var16, 6, 1.6F, true);
      }

      float[] var26 = new float[36];

      for (int var28 = 0; var28 < 9; var28++) {
         var26[var28 * 4 + 1] = var2[var28][0];
         var26[var28 * 4 + 2] = var2[var28][1];
         var26[var28 * 4 + 3] = 0.64F * var5[var28];
      }

      split(var24, var26, 5, 1.4F, false);

      for (float[] var37 : new float[][]{
         {0.05F, 30.55F, -2.15F, 0.1F, 31.45F, -2.6F, 0.15F, 32.05F, -3.35F, 0.1F, 31.85F, -4.15F},
         {0.3F, 30.5F, -2.1F, 0.72F, 31.25F, -2.7F, 0.98F, 31.72F, -3.45F, 1.08F, 31.42F, -4.05F},
         {-0.35F, 30.5F, -2.1F, -0.76F, 31.2F, -2.75F, -1.02F, 31.62F, -3.4F, -1.12F, 31.32F, -4.0F}
      }) {
         var24.add(
            Shapes.tube(
                  var37[0],
                  var37[1],
                  var37[2],
                  0.36F,
                  var37[3],
                  var37[4],
                  var37[5],
                  0.44F,
                  var37[6],
                  var37[7],
                  var37[8],
                  0.3F,
                  var37[9],
                  var37[10],
                  var37[11],
                  0.04F
               )
               .flattenFrom(1.8F, 0.0F, 26.0F, 0.5F)
         );
      }

      Paint var30 = Paint.field((f, g, h) -> {
         int var3x = Colors.mix(-14140794, -12948262, smooth(27.5F, 21.5F, g));
         var3x = Colors.mix(var3x, -13246209, smooth(21.5F, 16.0F, g));
         float var4x = smooth(30.9F, 31.5F, g) * smooth(-2.4F, -3.2F, h);
         return Colors.mix(var3x, -7363350, var4x * 0.5F);
      });
      Shape var33 = Shapes.union(var24.toArray(new Shape[0])).paint(var30);
      Shape var36 = ring(0.0F, 30.38F, -2.05F, 0.0F, 0.55F, -0.83F, 0.52F, 0.38F).roughen(0.05F, 0.3F).color(-13703425);
      rig.extra("ponytail", Bone.HEAD)
         .pivot(0.0F, hy(30.35F), -2.3779998F)
         .shape(grow(Shapes.union(var33, var36)))
         .detail(1.0F)
         .resolution(1.3F)
         .outline(-15327158, 0.12F)
         .flatLight(0.4F)
         .occlusion(0.4F)
         .motion(
            Motion.hair()
               .idle(3.0F, 0.0F, 2.5F, 0.3F)
               .walk(7.0F, 0.0F, 5.0F, 2.0F)
               .drag(22.0F)
               .hang(0.3F)
               .follow(Bone.BODY, 0.6F)
               .turn(18.0F, 4.0F)
               .spring(36.0F, 4.2F)
               .bend(1.5F)
               .limit(70.0F)
         );
   }

   private static void split(List<Shape> list, float[] fs, int i, float f, boolean bl) {
      int var5 = fs.length / 4;
      if (var5 < i + 3) {
         list.add(Shapes.tube(fs).flattenFrom(f, 0.0F, 26.0F, 0.5F));
      } else {
         list.add(Shapes.tube(Arrays.copyOfRange(fs, 0, (i + 2) * 4)).flattenFrom(f, 0.0F, 26.0F, 0.5F));
         Object var6 = Shapes.tube(Arrays.copyOfRange(fs, (i - 1) * 4, var5 * 4)).flattenFrom(f, 0.0F, 26.0F, 0.5F);
         if (bl) {
            int var7 = (var5 - 3) * 4;
            int var8 = (var5 - 2) * 4;
            int var9 = (var5 - 1) * 4;
            Shape2D var10 = Shape2D.path(2.4F, 0.02F, fs[var7], fs[var7 + 1], fs[var8], fs[var8 + 1], fs[var9], fs[var9 + 1]);
            var6 = ((Shape)var6).intersect(Shapes.union(Shapes.above(fs[var8 + 1]), Shapes.extrude(var10, 8.0F, 0.0F)));
         }

         list.add(var6);
      }
   }

   private static Shape body(Rig rig, Canvas canvas) {
      Paint var2 = Paint.field((f, g, h) -> Colors.mix(-9274, -1331558, smooth(24.9F, 25.5F, g))).flatLight(1.0F).occlusion(0.1F);
      Shape var3 = Shapes.cone(0.0F, 23.2F, -0.22F, 0.66F, 0.0F, 26.3F, 0.0F, 0.58F).paint(var2);
      Shape var4 = Shapes.blend(
            1.0F,
            Shapes.ellipsoid(0.0F, 21.15F, 0.0F, 2.25F, 2.05F, 1.45F),
            Shapes.ellipsoid(0.0F, 18.7F, 0.0F, 1.78F, 1.75F, 1.22F),
            Shapes.ellipsoid(0.0F, 16.6F, -0.05F, 2.0F, 1.5F, 1.4F),
            Shapes.capsule(-2.2F, 22.3F, -0.18F, 0.0F, 22.62F, -0.18F, 0.88F),
            Shapes.capsule(0.0F, 22.62F, -0.18F, 2.2F, 22.3F, -0.18F, 0.88F)
         )
         .blend(0.6F, Shapes.ellipsoid(0.82F, 20.85F, 0.55F, 0.88F, 0.82F, 0.85F).mirrorX());
      var4.paint(Paint.vertical(-2498584, 17.2F, -525828, 19.4F).flatLight(0.4F));
      Shape var5 = Shapes.cone(0.0F, 23.0F, -0.2F, 0.9F, 0.0F, 25.3F, -0.12F, 0.74F).intersect(0.06F, Shapes.below(25.1F)).color(-15263457);
      Shape var6 = Shapes.ellipsoid(0.0F, 14.95F, -0.05F, 1.95F, 1.2F, 1.35F).color(-15263457);
      Shape var7 = Shapes.blend(
            1.1F,
            Shapes.ellipsoid(0.0F, 21.0F, -0.1F, 2.7F, 2.4F, 1.98F),
            Shapes.capsule(-2.62F, 22.25F, -0.22F, 0.0F, 22.6F, -0.22F, 1.04F),
            Shapes.capsule(0.0F, 22.6F, -0.22F, 2.62F, 22.25F, -0.22F, 1.04F),
            oval(-0.1F, 18.3F, 20.3F, 2.45F, 1.97F, 2.55F, 1.97F)
         )
         .paint(Paint.field((f, g, h) -> Colors.mix(-12959662, -1446155, smooth(21.72F, 21.78F, g))).flatLight(0.3F).occlusion(0.18F));
      Shape var8 = oval(-0.1F, 17.42F, 18.45F, 2.45F, 2.03F, 2.47F, 2.02F).color(-14210250);
      Shape var9 = Shapes.torus(0.0F, 23.52F, -0.28F, 1.28F, 0.38F).rotate(16.0F, 0.0F, 0.0F, 0.0F, 23.52F, -0.28F).color(-14210250);
      Shape var10 = Shapes.union(var7, var8, var9);
      Shape var11 = Shapes.subtract(0.06F, var10, opening(0.0F));
      float var12 = frontZ(var10, -1.85F, 18.95F, 6.0F);
      float var13 = 16.95F;
      float var14 = flare(skirtT(var13));
      float var15 = 1.85F + 1.6F * var14 + 0.22F;
      float var16 = 1.5F + 1.4F * var14 + 0.22F;
      Shape var17 = ovalRing(-0.05F, var13, 0.42F, var15, var16, 0.26F).color(-15263457);
      float var18 = -0.05F + var16 * (float)Math.sqrt(1.0F - 0.3025F / (var15 * var15)) + 0.26F;
      Shape var19 = Shapes.box(0.55F, var13, var18 + 0.05F, 0.46F, 0.36F, 0.14F, 0.1F).color(-13749702);
      Part var20 = rig.part(Bone.BODY)
         .pivot(0.0F, 24.4F, 0.0F)
         .shape(Shapes.union(var3, var5, var4, var6, var11, var17, var19))
         .detail(1.0F)
         .outline(-15065558, 0.13F)
         .occlusion(0.3F);
      Canvas var21 = backStripe();
      Canvas var22 = band();
      var20.decal(topGraphic())
         .decal(Decal.back(var21, 2.02F, 19.6F, -2.0F, 0.6F, 4.0F).depth(1.4F, 1.4F))
         .decal(Decal.back(var21, -2.02F, 19.6F, -2.0F, 0.6F, 4.0F).depth(1.4F, 1.4F))
         .decal(Decal.wrap(canvas, 0.0F, 18.12F, -0.1F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 360.0F, 0.3F).radius(1.95F, 3.2F))
         .decal(Decal.front(pocket(), -1.85F, 18.95F, var12, 0.95F, 0.36F).depth(0.5F, 0.5F).maxAngle(70.0F))
         .decal(Decal.front(pocket().mirrored(), 1.85F, 18.95F, var12, 0.95F, 0.36F).depth(0.5F, 0.5F).maxAngle(70.0F))
         .decal(Decal.front(var22, 0.0F, 17.62F, 1.9F, 2.0F, 0.28F).depth(1.2F, 0.4F).maxAngle(70.0F))
         .decal(Decal.wrap(var22, 0.0F, 24.8F, -0.14F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 240.0F, 0.26F).radius(0.4F, 1.05F))
         .decal(Decal.front(buckle(), 0.55F, var13, var18 + 0.19F, 0.92F, 0.72F).depth(0.4F, 0.3F).maxAngle(60.0F));
      Shape var23 = Shapes.union(
         Shapes.tube(
               0.98F, 16.7F, var18 + 0.02F, 0.28F, 1.02F, 15.8F, var18 + 0.26F, 0.28F, 1.05F, 14.9F, var18 + 0.45F, 0.27F, 1.07F, 14.3F, var18 + 0.55F, 0.26F
            )
            .flatten(2.2F, 0.0F, 0.0F, 1.0F)
            .color(-15263457),
         Shapes.box(1.07F, 14.15F, var18 + 0.56F, 0.28F, 0.2F, 0.13F, 0.08F).color(-3550757)
      );
      rig.extra("strap", Bone.BODY)
         .pivot(0.98F, 16.7F, var18)
         .shape(var23)
         .resolution(0.7F)
         .detail(0.3F)
         .noOutline()
         .motion(Motion.cloth().walk(8.0F, 0.0F, 5.0F, 2.0F).drag(6.0F).spring(40.0F, 5.0F).bend(1.2F));
      return var10;
   }

   private static float openingWidth(float f) {
      return 0.85F + 0.65F * (24.9F - f) / 10.1F;
   }

   private static Shape opening(float f) {
      return Shapes.extrude(Shape2D.polygon(-0.85F - f, 24.9F, 0.85F + f, 24.9F, 1.5F + f, 14.8F, -1.5F - f, 14.8F), 2.6F, 0.0F).move(0.0F, 0.0F, 3.15F);
   }

   private static Shape piping(Shape shape, float f, float g, float h) {
      float[] var4 = new float[24];

      for (int var5 = 0; var5 < 8; var5++) {
         float var6 = g + (h - g) * var5 / 7.0F;
         float var7 = openingWidth(var6);
         var4[var5 * 3] = f * (var7 + 0.09F);
         var4[var5 * 3 + 1] = var6;
         var4[var5 * 3 + 2] = frontZ(shape, f * (var7 + 0.1F), var6, 6.0F) - 0.06F;
      }

      return Shapes.strand(0.13F, 0.13F, var4);
   }

   private static void skirt(Rig rig, Canvas canvas) {
      Paint var2 = Paint.field((f, g, h) -> {
         float var3x = (float)Math.atan2(f, h - -0.05F);
         float var4x = pleat(var3x * 18.0F / 6.2831855F);
         return Colors.mix(-14472632, -12892555, 0.4F + 0.6F * (0.5F + 0.5F * var4x));
      }).flatLight(0.15F);
      Decal var3 = Decal.wrap(canvas, 0.0F, 11.77F, -0.05F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 360.0F, 0.27F).radius(2.4F, 4.6F);
      float var4 = (float)Math.toRadians(30.0);
      float var5 = (float)Math.toRadians(14.0);
      String[] var6 = new String[]{"skirt_f", "skirt_s", "skirt_b"};
      float[] var7 = new float[]{-30.0F, -90.0F, -150.0F};
      float[] var8 = new float[]{0.5F, 0.42F, 0.5F};
      float[] var9 = new float[]{16.0F, 13.0F, 15.0F};

      for (int var10 = 0; var10 < 3; var10++) {
         float var11 = (float)Math.toRadians(var7[var10]);
         float var12 = 1.8F * (float)Math.sin(var11);
         float var13 = -0.05F + 1.45F * (float)Math.cos(var11);
         Motion var14 = Motion.skirt(Bone.RIGHT_LEG).follow(Bone.RIGHT_LEG, var8[var10], var9[var10]).drag(var10 == 0 ? 0.0F : 3.0F);
         Part var15 = rig.extra(var6[var10] + "_r", Bone.BODY)
            .pivot(var12, 16.9F, var13)
            .shape(skirtPanel(var11, var4, var5).paint(var2))
            .motion(var14)
            .detail(0.32F)
            .resolution(1.0F)
            .occlusion(0.3F)
            .noOutline()
            .decal(var3);
         rig.extra(var6[var10] + "_l", Bone.BODY).mirrorOf(var15);
      }
   }

   private static float skirtT(float f) {
      return clamp01((17.3F - f) / 6.049999F);
   }

   private static float flare(float f) {
      return f * (1.25F - 0.25F * f);
   }

   private static float skirtRadius(float f, float g, float h, float i, float j) {
      float var6 = skirtT(j);
      float var7 = flare(var6);
      float var8 = 1.5F + 1.4F * var7;
      float var5;
      float var9 = (float)Math.sqrt(var8 * var8 * f * f + (var5 = 1.85F + 1.6F * var7) * var5 * g * g);
      float var10 = var9 < 1.0E-5F ? var5 : var5 * var8 * h / var9;
      return var10 + (0.05F + 0.24F * var6) * pleat(i * 18.0F / 6.2831855F);
   }

   private static Shape skirtPanel(float f, float g, float h) {
      float var3 = 0.0F;
      float var4 = 0.0F;
      float var5 = -0.05F;
      float var6 = -0.05F;

      for (int var7 = 0; var7 <= 16; var7++) {
         float var8 = f - g - h + (g + h) * 2.0F * var7 / 16.0F;

         for (float var12 : new float[]{1.4F, 4.05F}) {
            var3 = Math.min(var3, var12 * (float)Math.sin(var8));
            var4 = Math.max(var4, var12 * (float)Math.sin(var8));
            var5 = Math.min(var5, -0.05F + var12 * (float)Math.cos(var8));
            var6 = Math.max(var6, -0.05F + var12 * (float)Math.cos(var8));
         }
      }

      return Shapes.field((i, j, k) -> {
         float var6x = k - -0.05F;
         float var7x = (float)Math.atan2(i, var6x);
         float var8x = var7x - f;
         if (var8x > 3.141592653589793) {
            var8x -= 6.2831855F;
         }

         if (var8x < -3.141592653589793) {
            var8x += 6.2831855F;
         }

         float var9 = Math.abs(var8x) - g;
         float var10 = (float)Math.sqrt(i * i + var6x * var6x);
         float var11 = skirtRadius(i, var6x, var10, var7x, j) - 0.32F * smooth(0.0F, 0.05F, var9);
         float var12x = Math.abs(var10 - var11) - 0.42F;
         float var13 = Math.max(j - 17.3F, 11.25F - j);
         float var14 = Math.max(-1.2F, Math.min(1.2F, var9 - h));
         float var15 = var10 * var14 * (1.0F - var14 * var14 / 6.0F);
         return Math.max(var12x, Math.max(var13, var15)) * 0.8F;
      }, var3 - 0.2F, 11.05F, var5 - 0.2F, var4 + 0.2F, 17.5F, var6 + 0.2F);
   }

   private static void arms(Rig rig, Canvas canvas) {
      float var2 = -3.68F;
      float var3 = 18.4F;
      float var4 = -0.1F;
      float var5 = var2 + 3.3F;
      float var6 = var3 - 21.8F;
      float var7 = var4 + 0.12F;
      float var8 = (float)Math.sqrt(var5 * var5 + var6 * var6 + var7 * var7);
      Shape var9 = Shapes.blend(0.45F, Shapes.sphere(-3.22F, 22.2F, -0.15F, 1.1F), Shapes.cone(-3.3F, 21.8F, -0.12F, 1.07F, var2, var3, var4, 0.96F));
      float var31;
      float var32;
      float var33;
      Shape var10 = var9.intersect(Shapes.plane(var2, var3, var4, var31 = var5 / var8, var32 = var6 / var8, var33 = var7 / var8).color(-14210250))
         .paint(Paint.field((f, g, h) -> Colors.mix(-1446155, -3945254, smooth(-3.2F, -2.6F, f) * 0.7F)).flatLight(0.3F));
      float var11 = var2 - var31 * 0.05F;
      float var12 = var3 - var32 * 0.05F;
      float var13 = var4 - var33 * 0.05F;
      Shape var14 = ring(var11, var12, var13, var31, var32, var33, 0.86F, 0.34F).color(-14210250);
      Paint var15 = Paint.of(-9274).flatLight(1.0F).occlusion(0.25F);
      Shape var16 = Shapes.cone(var2, var3 - 0.1F, var4 + 0.05F, 0.55F, -3.92F, 14.66F, 0.15F, 0.39F).paint(var15).rotate(-13.0F, 0.0F, 0.0F, var2, var3, var4);
      float var17 = -1.5F;
      float var18 = (float)Math.cos(Math.toRadians(var17));
      float var19 = (float)Math.sin(Math.toRadians(var17));
      float var20 = -3.3F + (var11 + 3.3F) * var18 - (var12 - 22.4F) * var19;
      float var21 = 22.4F + (var11 + 3.3F) * var19 + (var12 - 22.4F) * var18;
      float var22 = var31 * var18 - var32 * var19;
      float var23 = var31 * var19 + var32 * var18;
      float[] var24 = bent(-4.15F, 12.75F, 0.45F, var2, var3, var4);
      float var25 = -3.3F + (var24[0] + 3.3F) * var18 - (var24[1] - 22.4F) * var19;
      float var26 = 22.4F + (var24[0] + 3.3F) * var19 + (var24[1] - 22.4F) * var18;
      Part var27 = rig.part(Bone.RIGHT_ARM)
         .pivot(-3.3F, 22.4F, -0.1F)
         .grip(var25, var26, var24[2])
         .shape(Shapes.union(var10, var14, var16).rotate(0.0F, 0.0F, var17, -3.3F, 22.4F, -0.1F))
         .outline(-12959918, 0.12F)
         .occlusion(0.3F)
         .swing(0.8F)
         .joint(-3.78F, 18.41F, -0.1F, 0.55F);
      Part var28 = rig.part(Bone.LEFT_ARM).mirrorOf(var27);
      var27.decal(Decal.wrap(canvas, var20, var21, var13, -var22, -var23, -var33, -1.0F, 0.0F, 0.3F, 250.0F, 0.4F).radius(0.95F, 1.6F));
      var28.decal(Decal.wrap(canvas, -var20, var21, var13, var22, -var23, -var33, 1.0F, 0.0F, 0.3F, 250.0F, 0.4F).radius(0.95F, 1.6F));
      var28.decal(Decal.left(sleevePatch(), 4.45F, 20.3F, -0.1F, 1.05F, 1.05F).depth(0.8F, 0.8F).maxAngle(65.0F));
      var27.decal(Decal.right(sleeveText(), -4.5F, 20.2F, -0.1F, 0.62F, 1.75F).depth(0.8F, 0.8F).maxAngle(65.0F));
      Part var29 = rig.extra("hand_r", Bone.RIGHT_ARM)
         .shape(hand().rotate(-13.0F, 0.0F, 0.0F, var2, var3, var4).rotate(0.0F, 0.0F, var17, -3.3F, 22.4F, -0.1F))
         .resolution(0.45F)
         .detail(0.5F)
         .outline(-6657438, 0.1F)
         .flatLight(1.0F)
         .occlusion(0.25F);
      rig.extra("hand_l", Bone.LEFT_ARM).mirrorOf(var29);
      Part var30 = rig.extra("wrist_r", Bone.RIGHT_ARM)
         .shape(
            Shapes.ellipsoid(-3.97F, 14.12F, 0.16F, 0.31F, 0.5F, 0.37F)
               .color(-9274)
               .rotate(-13.0F, 0.0F, 0.0F, var2, var3, var4)
               .rotate(0.0F, 0.0F, var17, -3.3F, 22.4F, -0.1F)
         )
         .resolution(0.6F)
         .detail(1.4F)
         .outline(-6657438, 0.1F)
         .flatLight(1.0F)
         .occlusion(0.25F);
      rig.extra("wrist_l", Bone.LEFT_ARM).mirrorOf(var30);
   }

   private static float[] bent(float f, float g, float h, float i, float j, float k) {
      double var6 = Math.toRadians(-13.0);
      float var8 = (float)Math.cos(var6);
      float var9 = (float)Math.sin(var6);
      float var10 = g - j;
      float var11 = h - k;
      return new float[]{f, j + var10 * var8 - var11 * var9, k + var10 * var9 + var11 * var8};
   }

   private static Shape hand() {
      Shape var0 = Shapes.ellipsoid(-4.0F, 13.2F, 0.14F, 0.3F, 0.68F, 0.54F);
      Strand var1 = Shapes.strand(0.2F, 0.15F, -3.86F, 13.35F, 0.6F, -3.72F, 12.9F, 0.92F, -3.62F, 12.5F, 0.98F);
      Shape var2 = Shapes.union(
         Shapes.strand(0.17F, 0.15F, -3.98F, 12.72F, 0.56F, -3.92F, 12.1F, 0.72F, -3.8F, 11.66F, 0.8F),
         Shapes.strand(0.17F, 0.15F, -4.0F, 12.74F, 0.24F, -3.88F, 12.12F, 0.28F, -3.66F, 11.8F, 0.3F),
         Shapes.strand(0.165F, 0.145F, -4.0F, 12.72F, -0.08F, -3.88F, 12.18F, -0.08F, -3.66F, 11.88F, -0.06F),
         Shapes.strand(0.15F, 0.13F, -3.99F, 12.66F, -0.36F, -3.88F, 12.3F, -0.36F, -3.7F, 12.06F, -0.33F)
      );
      return Shapes.union(
         Shapes.blend(0.12F, var0, var1, var2).color(-9274), Shapes.cylinder(-3.95F, 14.22F, 0.18F, -3.93F, 14.9F, 0.15F, 0.5F, 0.1F).color(-13703425)
      );
   }

   private static void legs(Rig rig) {
      Shape var1 = Shapes.cone(-1.24F, 14.1F, 0.0F, 1.3F, -1.1F, 8.3F, 0.1F, 0.84F);
      Shape var2 = Shapes.sphere(-1.07F, 7.68F, 0.18F, 0.75F);
      Shape var3 = Shapes.sphere(-1.06F, 7.8F, 0.5F, 0.52F);
      Shape var4 = Shapes.cone(-1.07F, 7.3F, 0.06F, 0.78F, -1.12F, 2.6F, -0.1F, 0.5F);
      Shape var5 = Shapes.ellipsoid(-1.12F, 5.8F, -0.2F, 0.72F, 1.5F, 0.74F);
      Shape var6 = Shapes.sphere(-1.1F, 14.4F, -0.05F, 1.32F);
      Paint var7 = Paint.field((f, g, h) -> Colors.mix(Colors.mix(-14012606, -9274, smooth(6.7F, 6.95F, g)), -15263457, smooth(12.2F, 12.4F, g)));
      Shape var8 = Shapes.union(Shapes.blend(0.5F, var1, var2, var4, var5).blend(0.3F, var3), var6).paint(var7);
      Shape var9 = Shapes.union(Shapes.blend(0.5F, var4, var5).inflate(0.1F).intersect(Shapes.below(6.95F)), Shapes.torus(-1.075F, 6.9F, 0.0F, 0.86F, 0.15F))
         .color(-14012606);
      Part var10 = rig.part(Bone.RIGHT_LEG)
         .pivot(-1.2F, 14.3F, 0.0F)
         .shape(var8)
         .outline(-11914188, 0.12F)
         .flatLight(0.7F)
         .occlusion(0.3F)
         .swing(0.55F, 30.0F)
         .joint(-1.07F, 7.68F, 0.18F, 0.75F);
      rig.part(Bone.LEFT_LEG).mirrorOf(var10);
      Part var11 = rig.extra("sock_r", Bone.RIGHT_LEG).shape(var9).resolution(0.7F).detail(0.45F).outline(-15460836, 0.12F);
      Part var12 = rig.extra("sock_l", Bone.LEFT_LEG).mirrorOf(var11);
      Canvas var13 = socks();
      var11.decal(Decal.wrap(var13, -1.1F, 6.42F, -0.05F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 360.0F, 0.3F).radius(0.3F, 1.4F));
      var12.decal(Decal.wrap(var13, 1.1F, 6.42F, -0.05F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 360.0F, 0.3F).radius(0.3F, 1.4F));
      float var14 = -1.14F;
      float var15 = -7.0F;
      Part var16 = rig.extra("shoe_r", Bone.RIGHT_LEG)
         .shape(shoe(var14).rotate(0.0F, var15, 0.0F, var14, 1.0F, 0.3F))
         .resolution(0.8F)
         .detail(0.55F)
         .outline(-12959664, 0.12F);
      Part var17 = rig.extra("shoe_l", Bone.LEFT_LEG).mirrorOf(var16);
      Canvas var18 = laces();
      double var19 = Math.toRadians(var15);
      float var21 = (float)Math.cos(var19);
      float var22 = (float)Math.sin(var19);
      float var23 = var14 + 0.65F * var22;
      float var24 = 0.3F + 0.65F * var21;
      var16.decal(
         Decal.project(var18, var23, 2.1F, var24, 0.87F * var22, 0.5F, 0.87F * var21, -0.5F * var22, 0.87F, -0.5F * var21, 0.8F, 1.28F)
            .depth(0.7F, 0.7F)
            .maxAngle(75.0F)
      );
      var17.decal(
         Decal.project(var18, -var23, 2.1F, var24, -0.87F * var22, 0.5F, 0.87F * var21, 0.5F * var22, 0.87F, -0.5F * var21, 0.8F, 1.28F)
            .depth(0.7F, 0.7F)
            .maxAngle(75.0F)
      );
   }

   private static Shape shoe(float f) {
      Shape var1 = Shapes.blend(
         0.35F,
         Shapes.box(f, 0.42F, 0.4F, 0.88F, 0.42F, 1.45F, 0.36F),
         Shapes.ellipsoid(f, 0.44F, 1.62F, 0.88F, 0.44F, 0.95F),
         Shapes.ellipsoid(f, 0.44F, -0.85F, 0.84F, 0.44F, 0.7F)
      );
      Shape var2 = Shapes.ellipsoid(f, 1.12F, 1.35F, 0.76F, 0.58F, 1.1F);
      Shape var3 = Shapes.capsule(f, 1.3F, 0.85F, f, 2.05F, 0.1F, 0.7F);
      Shape var4 = Shapes.ellipsoid(f, 1.45F, -0.62F, 0.8F, 0.82F, 0.82F);
      Shape var5 = Shapes.union(
         Shapes.cone(f, 1.8F, -0.3F, 0.84F, f, 3.5F, -0.17F, 0.74F).intersect(Shapes.below(3.2F)), Shapes.torus(f, 3.18F, -0.19F, 0.66F, 0.15F)
      );
      Shape var6 = Shapes.ellipsoid(f, 3.05F, 0.42F, 0.46F, 0.72F, 0.3F);
      Shape var7 = Shapes.blend(0.4F, var2, var3, var4, var5).blend(0.15F, var6);
      Paint var8 = Paint.field(
         (fx, g, h) -> g < 0.9F ? Colors.mix(-14012872, -920586, smooth(0.05F, 0.24F, g)) : Colors.mix(-591621, -14210250, smooth(2.8F, 3.05F, g))
      );
      return Shapes.union(var1, var7).paint(var8);
   }

   private static void gear(Rig rig, Shape shape) {
      ArrayList var2 = new ArrayList();
      Canvas var3 = phoneRing();
      Part var4 = rig.extra("gear", Bone.BODY);

      for (byte var5 = -1; var5 <= 1; var5 += 2) {
         float var6 = var5 * 0.35F;
         float var7 = 0.45F;
         float var8 = 0.82F;
         float var9 = (float)Math.sqrt(var6 * var6 + var7 * var7 + var8 * var8);
         var6 /= var9;
         var7 /= var9;
         var8 /= var9;
         float var10 = var5 * 1.55F;
         float var11 = 23.5F;
         float var12 = 1.12F;
         float var14 = var7;
         Shape var19 = Shapes.cylinder(
               var10 - var6 * 0.1F, var11 - var7 * 0.1F, var12 - var8 * 0.1F, var10 + var6 * 0.16F, var11 + var7 * 0.16F, var12 + var8 * 0.16F, 0.86F, 0.12F
            )
            .paint(Paint.field((l, m, n) -> {
               float var9x = (l - var10) * var6 + (m - var11) * var14 + (n - var12) * var8;
               return Colors.mix(-14210250, -723206, smooth(0.03F, 0.1F, var9x));
            }));
         Shape var20 = Shapes.cylinder(
               var10 - var6 * 0.36F, var11 - var7 * 0.36F, var12 - var8 * 0.36F, var10 - var6 * 0.08F, var11 - var7 * 0.08F, var12 - var8 * 0.08F, 0.78F, 0.1F
            )
            .color(-14210250);
         Shape var21 = Shapes.capsule(var10 - var6 * 0.1F - var5 * 0.1F, var11 + 0.8F, var12 - var8 * 0.1F - 0.2F, var5 * 1.3F, 24.4F, 0.5F, 0.2F)
            .color(-14210250);
         var2.add(Shapes.union(var19, var20, var21));
         var4.decal(
            Decal.project(var3, var10 + var6 * 0.17F, var11 + var7 * 0.17F, var12 + var8 * 0.17F, var6, var7, var8, 0.0F, 1.0F, 0.0F, 1.3F, 1.3F)
               .depth(0.4F, 0.4F)
               .maxAngle(70.0F)
               .glow()
         );
      }

      var2.add(
         Shapes.tube(
               1.3F,
               24.4F,
               0.5F,
               0.24F,
               1.22F,
               24.85F,
               -0.35F,
               0.26F,
               0.72F,
               25.02F,
               -1.2F,
               0.26F,
               0.0F,
               25.08F,
               -1.42F,
               0.26F,
               -0.72F,
               25.02F,
               -1.2F,
               0.26F,
               -1.22F,
               24.85F,
               -0.35F,
               0.26F,
               -1.3F,
               24.4F,
               0.5F,
               0.24F
            )
            .flattenFrom(1.8F, 0.0F, 24.9F, -0.2F)
            .color(-14210250)
      );
      var2.add(Shapes.union(piping(shape, 1.0F, 23.3F, 17.5F), piping(shape, -1.0F, 23.3F, 17.5F)).color(-13703425));
      var4.shape(Shapes.union(var2.toArray(new Shape[0]))).resolution(0.7F).detail(0.7F).outline(-15065820, 0.1F).flatLight(0.7F).occlusion(0.15F);
      float[] var22 = new float[]{1.75F, 22.65F, 1.85F, 21.8F, 1.92F, 20.9F, 1.95F, 20.15F};
      float[] var24 = new float[12];

      for (int var26 = 0; var26 < 4; var26++) {
         var24[var26 * 3] = var22[var26 * 2];
         var24[var26 * 3 + 1] = var22[var26 * 2 + 1];
         var24[var26 * 3 + 2] = var26 == 0 ? 1.3F : frontZ(shape, var22[var26 * 2], var22[var26 * 2 + 1], 6.0F) + 0.16F;
      }

      float var27 = var24[11];
      Shape var29 = Shapes.union(
         Shapes.strand(0.13F, 0.13F, var24).color(-7563613),
         Shapes.capsule(1.95F, 20.2F, var27, 1.96F, 19.65F, var27 + 0.02F, 0.19F).color(-12959662),
         Shapes.capsule(1.96F, 19.6F, var27 + 0.02F, 1.96F, 19.3F, var27 + 0.03F, 0.12F).color(-3550757)
      );
      rig.extra("cable", Bone.BODY)
         .pivot(1.75F, 22.65F, 1.3F)
         .shape(var29)
         .resolution(0.5F)
         .detail(0.25F)
         .noOutline()
         .motion(Motion.cloth().idle(1.0F, 0.0F, 1.5F, 0.35F).walk(4.0F, 0.0F, 5.0F, 2.0F).drag(-2.0F).hang(0.4F).spring(45.0F, 5.0F).bend(1.1F));
   }

   private static float fu(float f) {
      return (f + 2.25F) * 80.0F;
   }

   private static float fv(float f) {
      return (28.6F - f) * 80.0F;
   }

   private static Decal face(List<float[]> list) {
      Canvas var2 = new Canvas(Math.round(360.0F), Math.round(201.60004F));

      for (float[] var4 : list) {
         Shape2D var5 = null;

         for (byte var6 = 0; var6 + 7 < var4.length; var6 += 4) {
            if (!(var4[var6 + 1] > 29.2F) || !(var4[var6 + 5] > 29.2F)) {
               Shape2D var7 = Shape2D.taper(
                  fu(var4[var6]),
                  fv(var4[var6 + 1] - 0.22F),
                  var4[var6 + 3] * 80.0F * 1.7F,
                  fu(var4[var6 + 4]),
                  fv(var4[var6 + 5] - 0.22F),
                  Math.max(2.0F, var4[var6 + 7] * 80.0F * 1.5F)
               );
               var5 = var5 == null ? var7 : var5.union(var7);
            }
         }

         if (var5 != null) {
            var2.fill(var5, 2028380826, 3.0F);
         }
      }

      for (byte var1 = -1; var1 <= 1; var1 += 2) {
         eye(var2, var1);
      }

      var2.fill(Shape2D.taper(fu(0.06F), fv(26.8F), 4.0F, fu(-0.02F), fv(26.64F), 2.0F), -1714123650, 0.0F);
      var2.fill(Shape2D.curve(fu(-0.22F), fv(26.2F), fu(0.0F), fv(26.1F), fu(0.2F), fv(26.23F), 3.4F, 2.2F), -7455931, 0.0F);

      for (byte var8 = -1; var8 <= 1; var8 += 2) {
         var2.fill(Shape2D.ellipse(fu(var8 * 1.25F), fv(26.42F), 32.0F, 11.0F), 1442807955, 10.0F);
      }

      float var9 = 27.34F;
      return Decal.front(var2, 0.0F, hy(var9), 3.132F, 5.22F, 2.9232004F).depth(3.0159998F, 0.8F).maxAngle(72.0F).flatLight(1.0F).occlusion(0.3F);
   }

   private static void eye(Canvas canvas, int i) {
      float var2 = fu(i * 0.98F);
      float var3 = fv(27.2F);
      float var4 = var2 - i * 2.0F;
      float var5 = var3 + 4.0F;
      float var6 = var2 - i * 46.0F;
      float var7 = var2 + i * 47.0F;
      float var8 = var3 - 40.0F;
      float var9 = var3 - 50.0F;
      float var10 = var2 - i * 2.0F;
      float var11 = var3 - 49.0F;
      Shape2D var12 = Shape2D.polygon(
         var6 - i * 30.0F,
         var8 + 2.0F,
         var6,
         var8,
         var10,
         var11,
         var7,
         var9,
         var7 + i * 30.0F,
         var9,
         var7 + i * 30.0F,
         var3 + 90.0F,
         var6 - i * 30.0F,
         var3 + 90.0F
      );
      Shape2D var13 = Shape2D.ellipse(var2, var3 - 2.0F, 47.0F, 53.0F).intersect(var12);
      canvas.fill(var13, Paint2D.linear(var2, var3 - 50.0F, var2, var3 + 50.0F, -2498062, -1, -1));
      Shape2D var14 = Shape2D.ellipse(var4, var5, 32.0F, 43.0F);
      Shape2D var15 = var14.intersect(var13);
      canvas.fill(var15, Paint2D.linear(var4, var5 - 43.0F, var4, var5 + 43.0F, -16050107, -15449954, -14711060, -10687489));
      canvas.fill(Shape2D.ellipse(var4, var5 + 25.0F, 22.0F, 14.0F).intersect(var15), -1871645953, 6.0F);
      canvas.fill(Shape2D.ellipse(var4, var5 + 3.0F, 12.0F, 19.0F), -16315098, 0.0F);
      canvas.fill(var14.outline(4.5F).intersect(var13), -536210893, 0.0F);
      canvas.fill(Shape2D.ellipse(var2, var3 - 44.0F, 62.0F, 20.0F).intersect(var13), 1714438256, 8.0F);
      canvas.fill(Shape2D.box(var4 - 12.0F, var5 - 15.0F, 6.5F, 10.5F, 3.5F).rotate(-14.0F, var4 - 12.0F, var5 - 15.0F), -1, 0.0F);
      canvas.fill(Shape2D.box(var4 - 2.0F, var5 - 20.0F, 2.2F, 5.0F, 1.5F).rotate(-14.0F, var4 - 2.0F, var5 - 20.0F), -855638017, 0.0F);
      canvas.fill(Shape2D.circle(var4 + 13.0F, var5 + 21.0F, 4.2F), -570425345, 0.0F);
      int var16 = -15724002;
      canvas.fill(Shape2D.path(5.0F, 9.0F, var6, var8 + 1.0F, var10, var11 - 1.0F, var7, var9), var16, 0.0F);
      canvas.fill(Shape2D.taper(var7 - i * 6.0F, var9 + 1.0F, 7.5F, var7 + i * 12.0F, var9 - 7.0F, 1.5F), var16, 0.0F);
      canvas.fill(Shape2D.curve(var2 + i * 6.0F, var3 + 51.0F, var2 + i * 32.0F, var3 + 46.0F, var7 - i * 3.0F, var3 + 22.0F, 1.2F, 3.0F), -1072030672, 0.0F);
      canvas.fill(Shape2D.curve(var2 - i * 40.0F, var3 - 72.0F, var2 + i * 2.0F, var3 - 80.0F, var2 + i * 42.0F, var3 - 72.0F, 3.6F, 2.0F), -14011280, 0.0F);
   }

   private static Canvas stripe() {
      Canvas var0 = new Canvas(64, 10);
      var0.fill(Shape2D.box(32.0F, 3.3F, 40.0F, 2.8F, 0.0F), -13703425);
      var0.fill(Shape2D.box(32.0F, 8.2F, 40.0F, 0.8F, 0.0F), -1);
      return var0;
   }

   private static Canvas band() {
      Canvas var0 = new Canvas(64, 8);
      var0.fill(Shape2D.box(32.0F, 4.0F, 40.0F, 3.0F, 0.0F), -13703425);
      return var0;
   }

   private static Canvas socks() {
      Canvas var0 = new Canvas(64, 12);
      var0.fill(Shape2D.box(32.0F, 2.5F, 40.0F, 2.0F, 0.0F), -13703425);
      var0.fill(Shape2D.box(32.0F, 9.5F, 40.0F, 2.0F, 0.0F), -13703425);
      return var0;
   }

   private static Canvas phoneRing() {
      Canvas var0 = new Canvas(96, 96);
      var0.fill(Shape2D.circle(48.0F, 48.0F, 33.0F).outline(5.0F), -13703425);
      return var0;
   }

   private static Decal topGraphic() {
      Canvas var0 = new Canvas(152, 120);
      var0.fill(Shape2D.circle(76.0F, 60.0F, 42.0F).outline(10.0F), -13703425);
      float[] var1 = new float[]{16.0F, 30.0F, 50.0F, 72.0F, 94.0F, 72.0F, 50.0F, 30.0F, 16.0F};

      for (int var2 = 0; var2 < var1.length; var2++) {
         var0.fill(Shape2D.box(76.0F + (var2 - 4) * 15.0F, 60.0F, 4.6F, var1[var2] * 0.5F, 3.0F), -14932394);
      }

      return Decal.front(var0, 0.0F, 20.3F, 1.9F, 1.7F, 1.34F).depth(0.75F, 0.45F).maxAngle(62.0F);
   }

   private static Canvas backStripe() {
      Canvas var0 = new Canvas(12, 80);
      var0.fill(Shape2D.box(6.0F, 40.0F, 3.1F, 39.0F, 0.9F), -13703425);
      var0.fill(Shape2D.box(0.9F, 40.0F, 0.7F, 39.0F, 0.35F), -1512206);
      var0.fill(Shape2D.box(11.1F, 40.0F, 0.7F, 39.0F, 0.35F), -1512206);
      return var0;
   }

   private static Canvas buckle() {
      Canvas var0 = new Canvas(64, 48);
      var0.fill(Shape2D.box(32.0F, 24.0F, 31.0F, 23.0F, 7.0F), Paint2D.linear(0.0F, 2.0F, 0.0F, 46.0F, -1643535, -7563613));
      var0.fill(Shape2D.box(32.0F, 24.0F, 21.0F, 12.0F, 4.0F), -14276045);
      var0.fill(Shape2D.box(32.0F, 24.0F, 14.0F, 3.5F, 1.5F), -13703425);
      var0.fill(Shape2D.box(8.5F, 24.0F, 2.0F, 9.0F, 1.0F), -14276045);
      var0.fill(Shape2D.box(55.5F, 24.0F, 2.0F, 9.0F, 1.0F), -14276045);
      return var0;
   }

   private static Canvas pocket() {
      Canvas var0 = new Canvas(64, 24);
      var0.fill(Shape2D.box(32.0F, 12.0F, 28.0F, 3.4F, 3.0F), -15460837);
      var0.fill(Shape2D.box(32.0F, 12.0F, 25.0F, 0.8F, 0.4F), -13703425);
      var0.fill(Shape2D.box(54.0F, 15.5F, 2.2F, 5.0F, 1.2F), -3550757);
      return var0;
   }

   private static Canvas sleeveText() {
      float var0 = 0.43F;
      Canvas var1 = new Canvas(41, 117);
      Shape2D var2 = Shape2D.union(
         Shape2D.line(26.0F, 86.0F, 48.0F, 14.0F, 13.0F), Shape2D.line(48.0F, 14.0F, 70.0F, 86.0F, 13.0F), Shape2D.line(36.0F, 62.0F, 60.0F, 62.0F, 10.0F)
      );
      Shape2D var3 = Shape2D.ellipse(48.0F, 136.0F, 22.0F, 34.0F).outline(13.0F);
      Shape2D var4 = Shape2D.union(
         Shape2D.line(48.0F, 190.0F, 48.0F, 258.0F, 13.0F), Shape2D.line(32.0F, 190.0F, 64.0F, 190.0F, 9.0F), Shape2D.line(32.0F, 258.0F, 64.0F, 258.0F, 9.0F)
      );
      Shape2D var5 = Shape2D.union(var2, var3, var4);
      ink(var1, var0, var5.move(3.0F, 3.0F), -13703425, 0.0F);
      ink(var1, var0, var5, -14932394, 0.0F);
      return var1;
   }

   private static Canvas sleevePatch() {
      float var0 = 0.625F;
      Canvas var1 = new Canvas(80, 80);
      ink(var1, var0, Shape2D.circle(64.0F, 64.0F, 60.0F), -14210250, 0.0F);
      ink(var1, var0, Shape2D.circle(64.0F, 64.0F, 52.0F).outline(6.0F), -13703425, 0.0F);
      ink(var1, var0, Shape2D.arc(64.0F, 70.0F, 30.0F, 180.0F, 360.0F, 8.0F), -591621, 0.0F);
      ink(var1, var0, Shape2D.box(36.0F, 78.0F, 8.0F, 14.0F, 5.0F), -591621, 0.0F);
      ink(var1, var0, Shape2D.box(92.0F, 78.0F, 8.0F, 14.0F, 5.0F), -591621, 0.0F);
      return var1;
   }

   private static Canvas laces() {
      float var0 = 0.5F;
      Canvas var1 = new Canvas(48, 80);

      for (int var2 = 0; var2 < 4; var2++) {
         float var3 = 30.0F + var2 * 32.0F;
         ink(var1, var0, Shape2D.line(20.0F, var3, 76.0F, var3 + 16.0F, 9.0F), -13703425, 0.0F);
         ink(var1, var0, Shape2D.line(76.0F, var3, 20.0F, var3 + 16.0F, 9.0F), -13703425, 0.0F);
         ink(var1, var0, Shape2D.circle(14.0F, var3, 5.0F), -3550757, 0.0F);
         ink(var1, var0, Shape2D.circle(82.0F, var3, 5.0F), -3550757, 0.0F);
      }

      return var1;
   }

   private static void ink(Canvas canvas, float f, Shape2D shape2D, int i, float g) {
      canvas.fill(shape2D.scale(f, 0.0F, 0.0F), i, g * f);
   }

   private static float frontZ(Shape shape, float f, float g, float h) {
      float var5 = h;

      for (int var6 = 0; var6 < 400 && var5 > -8.0F; var6++) {
         float var4 = shape.distance(f, g, var5);
         if (var4 < 0.003F) {
            return var5;
         }

         var5 -= Math.max(0.005F, var4 * 0.7F);
      }

      return var5;
   }

   private static Shape ring(float f, float g, float h, float i, float j, float k, float l, float m) {
      float var8 = (float)Math.sqrt(i * i + j * j + k * k);
      float var9 = i / var8;
      float var10 = j / var8;
      float var11 = k / var8;
      Shape var12 = Shapes.torus(f, g, h, l, m);
      if (var9 * var9 + var11 * var11 < 1.0E-8F) {
         return var12;
      } else {
         float var13 = (float)Math.toDegrees(Math.acos(Math.max(-1.0F, Math.min(1.0F, var10))));
         return var12.rotateAxis(var11, 0.0F, -var9, var13, f, g, h);
      }
   }

   private static Shape oval(float f, float g, float h, float i, float j, float k, float l) {
      float var7 = Math.max(i, k);
      float var8 = Math.max(j, l);
      return Shapes.field((m, n, o) -> {
         float var10 = clamp01((n - g) / (h - g));
         float var11 = ellipse(m, o - f, i + (k - i) * var10, j + (l - j) * var10);
         return Math.max(var11 * 0.95F, Math.max(g - n, n - h));
      }, -var7, g, f - var8, var7, h, f + var8);
   }

   private static Shape ovalRing(float f, float g, float h, float i, float j, float k) {
      float var6 = Math.min(k, h) * 0.5F;
      return Shapes.field((m, n, o) -> {
         float var10 = Math.abs(ellipse(m, o - f, i, j)) - k + var6;
         float var11 = Math.abs(n - g) - h + var6;
         float var12 = Math.max(var10, 0.0F);
         float var13 = Math.max(var11, 0.0F);
         return ((float)Math.sqrt(var12 * var12 + var13 * var13) + Math.min(Math.max(var10, var11), 0.0F) - var6) * 0.95F;
      }, -i - k, g - h, f - j - k, i + k, g + h, f + j + k);
   }

   private static float ellipse(float f, float g, float h, float i) {
      float var4 = (float)Math.sqrt(f * f / (h * h) + g * g / (i * i));
      float var5 = (float)Math.sqrt(f * f / (h * h * h * h) + g * g / (i * i * i * i));
      return var5 < 1.0E-6F ? -Math.min(h, i) : var4 * (var4 - 1.0F) / var5;
   }

   private static float pleat(float f) {
      float var1 = tri(f);
      return var1 * (1.5F - 0.5F * var1 * var1);
   }

   private static float tri(float f) {
      float var1 = f - (float)Math.floor(f);
      return 4.0F * Math.min(var1, 1.0F - var1) - 1.0F;
   }

   private static float clamp01(float f) {
      return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
   }

   private static float smooth(float f, float g, float h) {
      float var3 = clamp01((h - f) / (g - f));
      return var3 * var3 * (3.0F - 2.0F * var3);
   }
}
