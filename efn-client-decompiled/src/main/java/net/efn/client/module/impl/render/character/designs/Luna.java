package net.efn.client.module.impl.render.character.designs;

import java.util.ArrayList;
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
import net.efn.client.module.impl.render.character.kit.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Luna extends CharacterDesign {
   private static final int SKIN = -6958;
   private static final int SKIN_SHADE = -1331558;
   private static final int SKIN_LINE = -5737368;
   private static final int HAIR = -1912065;
   private static final int HAIR_MID = -4940554;
   private static final int HAIR_TIP = -7706904;
   private static final int HAIR_ROOT = -6519080;
   private static final int HAIR_LIGHT = -1647873;
   private static final int HAIR_LINE = -10794346;
   private static final int NAVY = -14603682;
   private static final int NAVY_DARK = -15460295;
   private static final int NAVY_LINE = -15921370;
   private static final int BODICE = -13945223;
   private static final int TIER = -12693598;
   private static final int LINING = -7571752;
   private static final int RIBBON = -4544530;
   private static final int PETTICOAT = -4611086;
   private static final int LACE = -1036;
   private static final int GOLD = -1326758;
   private static final int GOLD_DARK = -5011921;
   private static final int GOLD_PALE = -2868;
   private static final int GOLD_LINE = -8760296;
   private static final int TIGHTS = -12959880;
   private static final int TIGHTS_SHEEN = -10656082;
   private static final int BOOT = -7710146;
   private static final int BOOT_DARK = -9550799;
   private static final int BOOT_CUFF = -6395064;
   private static final int LEATHER = -8762574;
   private static final int LEATHER_DARK = -11653344;
   private static final int MOON = -4688;
   private static final float HEAD_K = 1.05F;
   private static final float NECK_Y = 24.7F;
   private static final float NECK_Z = -0.1F;
   private static final float AXIS_Z = -0.3F;
   private static final float HAT_TILT_X = -8.0F;
   private static final float HAT_TILT_Z = 4.0F;
   private static final Vec3 HAT_CENTRE = new Vec3(0.0F, 29.15F, -0.25F);
   private static final Shape2D BRIM = Shape2D.taper(2.9F, 29.25F, 0.82F, 5.72F, 29.0F, 0.5F);
   private static final float BRIM_WAVE = 0.3F;
   private static final float RIM_R = 5.86F;
   private static final Vec3 ELBOW = new Vec3(-3.72F, 18.4F, -0.05F);
   private static final float ELBOW_PITCH = -12.0F;
   private static final float ELBOW_ROLL = 3.0F;
   private static final float FACE_PPU = 41.48148F;
   private static final float FACE_LEFT = -2.3142858F;
   private static final float FACE_TOP = 28.810715F;
   private static final float[] CAPE_Y = new float[]{7.0F, 10.0F, 13.5F, 17.0F, 20.5F, 23.4F};
   private static final float[] CAPE_RX = new float[]{5.15F, 4.85F, 4.15F, 3.45F, 3.2F, 3.15F};
   private static final float[] CAPE_RZ = new float[]{4.45F, 4.2F, 3.5F, 2.3F, 1.62F, 1.3F};
   private static final float[] CAPE_CZ = new float[]{-0.8F, -0.85F, -0.85F, -0.6F, -0.45F, -0.35F};
   private static final float[] CAPE_WRAP = new float[]{104.0F, 102.0F, 100.0F, 105.0F, 118.0F, 132.0F};
   private static final int CAPE_STEPS = 96;
   private static final float[] CAPE_LUT = capeTable();
   private static final float[] CURTAIN_Y = new float[]{17.0F, 20.0F, 22.5F, 24.5F, 27.6F, 29.3F};
   private static final float[] CURTAIN_RX = new float[]{3.2F, 3.05F, 2.95F, 2.95F, 3.12F, 2.35F};
   private static final float[] CURTAIN_RZ = new float[]{3.0F, 2.65F, 2.25F, 2.55F, 3.05F, 2.35F};
   private static final float[] CURTAIN_CZ = new float[]{-0.95F, -0.95F, -0.9F, -0.65F, -0.3F, -0.3F};

   public Luna() {
      super("luna", "Luna");
   }

   @Override
   protected void define(Rig var1) {
      var1.scale(1.0F).outline(-15396047, 0.15F).occlusion(0.35F);
      Canvas var2 = new Canvas(128, 12);
      paintBand(var2);
      Shape var3 = faceShape();
      Shape var4 = Shapes.ellipsoid(0.0F, 27.55F, -0.35F, 2.82F, 2.94F, 2.88F).intersect(Shapes.plane(0.0F, 24.6F, -2.7F, 0.0F, -1.0F, 0.86F));
      List var5 = bangs(var3);
      head(var1, var3, var5);
      hair(var1, var4, var5);
      hat(var1);
      body(var1);
      cape(var1);
      skirt(var1, var2);
      arms(var1, var2);
      legs(var1);
   }

   private static Shape faceShape() {
      Shape var0 = Shapes.ellipsoid(0.0F, 27.5F, -0.3F, 2.38F, 2.5F, 2.42F);
      Shape var1 = Shapes.ellipsoid(0.0F, 26.3F, 0.3F, 1.84F, 1.32F, 1.84F);
      Shape var2 = Shapes.cone(0.0F, 25.75F, 0.4F, 1.28F, 0.0F, 24.74F, 1.02F, 0.34F);
      return Shapes.blend(1.0F, var0, var1, var2);
   }

   private static void head(Rig var0, Shape var1, List<Luna.Lock> var2) {
      var0.part(Bone.HEAD)
         .pivot(0.0F, 24.7F, -0.1F)
         .shape(hs(var1.paint(Paint.of(-6958))))
         .resolution(0.9F)
         .detail(1.5F)
         .flatLight(1.0F)
         .occlusion(0.2F)
         .outline(-5737368, 0.08F)
         .decal(faceDecal(var2));
   }

   private static Shape hs(Shape var0) {
      return var0.scale(1.05F, 0.0F, 24.7F, -0.1F);
   }

   private static Vec3 hp(float var0, float var1, float var2) {
      return new Vec3(var0 * 1.05F, 24.7F + (var1 - 24.7F) * 1.05F, -0.1F + (var2 - -0.1F) * 1.05F);
   }

   private static Part at(Part var0, Vec3 var1) {
      return var0.pivot(var1.x(), var1.y(), var1.z());
   }

   private static List<Luna.Lock> bangs(Shape var0) {
      float[][] var1 = new float[][]{
         {-50.0F, 26.5F, 0.75F, 0.3F, 0.0F},
         {-24.0F, 27.6F, 0.72F, 0.25F, 0.0F},
         {4.0F, 26.9F, 0.8F, -0.2F, 0.0F},
         {27.0F, 27.8F, 0.7F, -0.25F, 0.0F},
         {52.0F, 26.62F, 0.75F, -0.3F, 0.0F},
         {-37.0F, 27.2F, 0.36F, 0.2F, 1.0F},
         {40.0F, 27.02F, 0.36F, -0.2F, 1.0F}
      };
      ArrayList var2 = new ArrayList();

      for (float[] var6 : var1) {
         float[] var7 = new float[10];
         float[] var8 = new float[10];

         for (int var9 = 0; var9 < 10; var9++) {
            var7[var9] = var6[1] - 0.3F + (29.6F - var6[1] + 0.3F) * var9 / 9.0F;
            var8[var9] = surfaceR(var0, var6[0], var7[var9]);
         }

         var2.add(
            var6[4] > 0.0F
               ? new Luna.Lock(var6[0], 29.4F, var6[1], var6[2], var6[3], 0.7F, 0.75F, 0.1F, 0.3F, 0.2F, var7, var8)
               : new Luna.Lock(var6[0], 29.4F, var6[1], var6[2], var6[3], 0.5F, 0.45F, 0.05F, 0.4F, 0.22F, var7, var8)
         );
      }

      return var2;
   }

   private static float interp(float[] var0, float[] var1, float var2) {
      int var3 = var0.length;
      if (var2 <= var0[0]) {
         return var1[0];
      } else if (var2 >= var0[var3 - 1]) {
         return var1[var3 - 1];
      } else {
         int var4 = 0;

         while (var2 > var0[var4 + 1]) {
            var4++;
         }

         return var1[var4] + (var1[var4 + 1] - var1[var4]) * (var2 - var0[var4]) / (var0[var4 + 1] - var0[var4]);
      }
   }

   private static float surfaceR(Shape var0, float var1, float var2) {
      double var4 = Math.toRadians(var1);
      float var6 = (float)Math.sin(var4);
      float var7 = (float)Math.cos(var4);
      float var8 = 4.5F;

      float var3;
      for (int var9 = 0; var9 < 80 && !(Math.abs(var3 = var0.distance(var6 * var8, var2, -0.3F + var7 * var8)) < 0.001F); var9++) {
         var8 -= var3;
      }

      return var8;
   }

   private static void hair(Rig var0, Shape var1, List<Luna.Lock> var2) {
      ArrayList var4 = new ArrayList();
      var4.add(var1);

      for (byte var5 = -1; var5 <= 1; var5 += 2) {
         var4.add(
            Shapes.strand(0.74F, 0.24F, var5 * 2.35F, 29.2F, -0.9F, var5 * 3.04F, 27.6F, -0.72F, var5 * 3.02F, 26.0F, -0.5F, var5 * 2.78F, 24.5F, -0.35F)
               .flattenFrom(1.5F, 0.0F, 26.8F, -0.6F)
         );
      }

      Paint var23 = Paint.field(
         (var0x, var1x, var2x) -> shine(Colors.mix(-4940554, -1912065, clamp01((var1x - 24.6F) / 3.6F)), var0x, var1x, var2x, 28.1F, 0.26F, 7, 0.3F)
      );
      var0.extra("hair", Bone.HEAD).shape(hs(Shapes.union(var4.toArray(new Shape[0])).paint(var23))).resolution(1.3F).detail(0.6F).flatLight(0.4F).noOutline();
      ArrayList var6 = new ArrayList();

      for (Luna.Lock var8 : var2) {
         var6.add(var8.shape());
      }

      Paint var24 = Paint.field(
         (var0x, var1x, var2x) -> shine(Colors.mix(-1647873, -6519080, clamp01((var1x - 27.1F) / 1.9F)), var0x, var1x, var2x, 28.45F, 0.14F, 11, 0.9F)
      );
      var0.extra("bangs", Bone.HEAD)
         .shape(hs(Shapes.union(var6.toArray(new Shape[0])).paint(var24)))
         .resolution(0.45F)
         .detail(1.3F)
         .flatLight(0.7F)
         .occlusion(0.15F)
         .outline(-10794346, 0.08F);
      Luna.Lock var25 = new Luna.Lock(
         -72.0F,
         29.4F,
         24.3F,
         0.62F,
         0.9F,
         0.0F,
         0.0F,
         0.0F,
         0.45F,
         0.3F,
         new float[]{24.0F, 25.0F, 26.2F, 27.6F, 28.8F, 29.6F},
         new float[]{2.26F, 2.54F, 2.72F, 2.7F, 2.42F, 2.0F}
      );
      Paint var9 = Paint.field(
         (var0x, var1x, var2x) -> var1x > 27.2F
            ? Colors.mix(-1647873, -6519080, clamp01((var1x - 27.2F) / 1.8F))
            : Colors.mix(-4940554, -1647873, clamp01((var1x - 24.3F) / 2.9F))
      );
      var0.extra("locks", Bone.HEAD)
         .shape(hs(var25.shape().mirrorX().paint(var9)))
         .resolution(0.45F)
         .detail(0.7F)
         .flatLight(0.5F)
         .occlusion(0.3F)
         .outline(-10794346, 0.08F);
      ArrayList var10 = new ArrayList();
      ArrayList var11 = new ArrayList();

      for (int var3 = 0; var3 < 5; var3++) {
         var10.add(backLock(118.0F + 31.0F * var3, -0.22F, 18.2F + var3 % 2 * 0.35F, 1.25F, 0.5F, 2.6F, var3 * 1.7F));
      }

      for (int var22 = 0; var22 < 7; var22++) {
         float var12 = var22 / 6.0F;
         float var13 = 16.9F + 1.4F * (float)Math.pow(Math.abs(var12 - 0.5F) * 2.0F, 1.5) + var22 % 3 * 0.3F;
         var11.add(backLock(116.0F + 128.0F * var12, var22 % 2 == 0 ? 0.14F : 0.06F, var13, 0.66F, 0.12F, 1.7F, var22 * 2.3F));
      }

      Shape var26 = Shapes.union(var10.toArray(new Shape[0])).paint(Paint.vertical(-9152312, 18.0F, -5993238, 26.0F));
      Shape var27 = Shapes.union(var11.toArray(new Shape[0]))
         .paint(Paint.field((var0x, var1x, var2x) -> shine(backHair(var1x), var0x, var1x, var2x, 27.25F, 0.3F, 6, 1.1F)));
      at(var0.extra("hair_back", Bone.HEAD), hp(0.0F, 28.2F, -1.6F))
         .shape(hs(Shapes.union(var26, var27)))
         .motion(Motion.hair().hang(0.8F).drag(18.0F))
         .resolution(1.15F)
         .detail(0.85F)
         .flatLight(0.35F)
         .outline(-10794346, 0.1F);
      Strand var14 = Shapes.tube(
            -2.3F,
            29.0F,
            -0.2F,
            0.52F,
            -2.8F,
            27.4F,
            -0.05F,
            0.58F,
            -2.95F,
            25.6F,
            0.25F,
            0.56F,
            -2.8F,
            23.8F,
            1.0F,
            0.5F,
            -2.66F,
            22.2F,
            1.7F,
            0.46F,
            -2.62F,
            20.8F,
            2.02F,
            0.42F
         )
         .flattenFrom(1.45F, 0.0F, 22.0F, -0.5F);
      byte var15 = 20;
      float[] var16 = new float[var15 * 4];

      for (int var17 = 0; var17 < var15; var17++) {
         float var18 = var17 / (var15 - 1.0F);
         double var19 = 3.141592653589793 + var18 * 3.141592653589793 * 5.0;
         float var21 = 0.8F - 0.22F * var18;
         var16[var17 * 4] = -1.82F + var21 * (float)Math.cos(var19);
         var16[var17 * 4 + 1] = 20.8F - 3.3F * var18;
         var16[var17 * 4 + 2] = 2.02F + var21 * (float)Math.sin(var19);
         var16[var17 * 4 + 3] = 0.42F - 0.18F * var18;
      }

      Shape var28 = Shapes.union(var14, Shapes.tube(var16)).paint(Paint.vertical(-7706904, 17.4F, -1912065, 25.0F));
      Part var29 = at(var0.extra("curl_r", Bone.HEAD), hp(-2.35F, 28.7F, -0.15F))
         .shape(hs(var28))
         .resolution(0.9F)
         .motion(Motion.hair().drag(4.0F).walk(3.0F, 0.0F, 3.5F, 2.0F).hang(0.7F).turn(10.0F, 0.0F).bend(1.4F))
         .detail(0.9F)
         .flatLight(0.4F)
         .outline(-10794346, 0.1F);
      var0.extra("curl_l", Bone.HEAD).mirrorOf(var29);
   }

   private static int backHair(float var0) {
      if (var0 > 26.2F) {
         return Colors.mix(-1912065, -6519080, clamp01((var0 - 26.2F) / 1.9F));
      } else {
         return var0 > 22.0F ? Colors.mix(-4940554, -1912065, clamp01((var0 - 22.0F) / 4.2F)) : Colors.mix(-7706904, -4940554, clamp01((var0 - 17.2F) / 4.8F));
      }
   }

   private static int shine(int var0, float var1, float var2, float var3, float var4, float var5, int var6, float var7) {
      float var8 = (float)Math.atan2(var1, var3 - -0.3F);
      float var9 = Math.abs(var2 - var4 + 0.25F * (1.0F - (float)Math.cos(var8)));
      if (var9 > var5) {
         return var0;
      } else {
         float var10 = (float)Math.sin(var8 * var6 + var7) + 0.2F;
         if (var10 <= 0.0F) {
            return var0;
         } else {
            float var11 = (1.0F - var9 / var5) * Math.min(1.0F, var10 * 3.0F) * 0.8F;
            return Colors.mix(var0, Colors.mix(var0, -1, 0.5F), Math.min(1.0F, var11 * 1.6F));
         }
      }
   }

   private static Shape backLock(float var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      double var7 = Math.toRadians(var0);
      float var9 = (float)Math.sin(var7);
      float var10 = (float)Math.cos(var7);
      float[] var11 = new float[]{29.3F, 27.6F, 25.3F, 23.0F, 20.8F, 18.9F, var2};
      float[] var12 = new float[var11.length * 3];

      for (int var13 = 0; var13 < var11.length; var13++) {
         float var14 = var11[var13];
         float var15 = spline(CURTAIN_Y, CURTAIN_RX, var14) + var1;
         float var16 = spline(CURTAIN_Y, CURTAIN_RZ, var14) + var1;
         float var17 = spline(CURTAIN_Y, CURTAIN_CZ, var14);
         float var18 = 0.32F * clamp01((26.5F - var14) / 5.0F) * (float)Math.sin(var14 * 0.95F + var6);
         float var19 = var13 == var11.length - 1 ? 0.16F : 0.0F;
         var12[var13 * 3] = (var15 + var19) * var9 + var18 * var10;
         var12[var13 * 3 + 1] = var14;
         var12[var13 * 3 + 2] = var17 + (var16 + var19) * var10 - var18 * var9;
      }

      return Shapes.strand(var3, var4, var12).flatten(var5, var9, 0.0F, var10);
   }

   private static void hat(Rig var0) {
      Paint var1 = Paint.vertical(-15460295, 28.6F, -14603682, 31.0F);
      Shape var2 = Shapes.field(Luna::brimDistance, -6.1F, 28.1F, -6.35F, 6.1F, 30.1F, 5.85F).paint(var1);
      var0.extra("brim", Bone.HEAD).shape(worn(var2)).resolution(1.2F).detail(0.65F).outline(-15921370, 0.12F);
      Shape var3 = Shapes.field(Luna::rimDistance, -6.2F, 28.35F, -6.45F, 6.2F, 29.65F, 5.95F).paint(Paint.of(-1326758).grain(0.6F, 0.08F));
      Shape var4 = Shapes.cone(0.0F, 28.7F, -0.25F, 3.43F, 0.0F, 31.2F, -0.3F, 3.08F)
         .intersect(Shapes.box(0.0F, 29.95F, -0.27F, 3.6F, 0.5F, 3.6F))
         .subtract(Shapes.cone(0.0F, 28.7F, -0.25F, 2.75F, 0.0F, 31.2F, -0.3F, 2.4F))
         .paint(Paint.vertical(-6650664, 29.45F, -4544530, 30.3F));
      var0.extra("hat_trim", Bone.HEAD).shape(worn(Shapes.union(var3, var4))).resolution(0.95F).detail(0.35F).noOutline();
      Shape var5 = Shapes.cone(0.0F, 28.7F, -0.25F, 3.2F, 0.0F, 31.2F, -0.3F, 2.85F).intersect(Shapes.box(0.0F, 29.6F, -0.27F, 3.5F, 0.75F, 3.5F));
      var0.extra("hat", Bone.HEAD).shape(worn(var5.paint(var1))).resolution(1.2F).detail(0.3F).outline(-15921370, 0.15F);
      var0.extra("hat_pin", Bone.HEAD).shape(worn(pin())).resolution(0.4F).detail(0.9F).outline(-8760296, 0.06F);
      Shape var6 = Shapes.tube(
            0.0F,
            30.2F,
            -0.29F,
            2.8F,
            0.0F,
            32.2F,
            -0.42F,
            2.24F,
            0.0F,
            34.1F,
            -0.74F,
            1.56F,
            0.04F,
            35.3F,
            -1.42F,
            1.02F,
            0.14F,
            36.0F,
            -2.45F,
            0.64F,
            0.3F,
            36.15F,
            -3.65F,
            0.36F
         )
         .intersect(Shapes.above(30.15F))
         .subtract(0.25F, Shapes.ellipsoid(0.0F, 34.45F, -2.3F, 0.9F, 0.35F, 0.45F))
         .paint(Paint.vertical(-14603682, 30.0F, -14011792, 37.0F));
      Vec3 var7 = hatPoint(0.0F, 30.4F, -0.3F);
      Part var8 = at(var0.extra("hat_tip", var0.part(Bone.HEAD)), hp(var7.x(), var7.y(), var7.z()))
         .shape(worn(var6))
         .resolution(1.25F)
         .motion(
            new Motion()
               .idle(2.0F, 0.0F, 2.0F, 0.3F)
               .walk(6.0F, 0.0F, 4.0F, 2.0F)
               .drag(7.0F)
               .hang(0.25F)
               .turn(10.0F, 0.0F)
               .spring(38.0F, 4.5F)
               .bend(1.7F)
               .limit(35.0F)
         )
         .detail(0.75F)
         .outline(-15921370, 0.15F);
      Vec3 var9 = hatPoint(0.28F, 35.9F, -3.5F);
      Shape var10 = Shapes.strand(0.08F, 0.08F, var9.x(), var9.y(), var9.z(), var9.x(), var9.y() - 0.8F, var9.z()).color(-5011921);
      Shape var11 = Shapes.extrude(Shape2D.star(0.0F, 0.0F, 0.62F, 0.27F, 5).rotate(180.0F, 0.0F, 0.0F), 0.14F, 0.05F)
         .rotate(0.0F, 90.0F, 0.0F)
         .move(var9.x(), var9.y() - 1.3F, var9.z())
         .paint(Paint.of(-9872).glow());
      at(var0.extra("hat_charm", var8), hp(var9.x(), var9.y(), var9.z()))
         .shape(hs(Shapes.union(var10, var11)))
         .motion(Motion.hair().hang(0.9F).spring(60.0F, 6.0F))
         .resolution(0.45F)
         .detail(1.2F)
         .outline(-8760296, 0.05F);
   }

   private static float brimDistance(float var0, float var1, float var2) {
      float var3 = var2 + 0.25F;
      float var4 = (float)Math.sqrt(var0 * var0 + var3 * var3);
      return BRIM.distance(var4, var1 - wave(var0, var3, var4, clamp01((var4 - 2.9F) / 2.9F)));
   }

   private static float rimDistance(float var0, float var1, float var2) {
      float var3 = var2 + 0.25F;
      float var4 = (float)Math.sqrt(var0 * var0 + var3 * var3);
      float var5 = var4 - 5.86F;
      float var6 = var1 - 29.0F - wave(var0, var3, var4, 1.0F);
      return (float)Math.sqrt(var5 * var5 + var6 * var6) - 0.31F;
   }

   private static float wave(float var0, float var1, float var2, float var3) {
      return var2 < 0.001F ? 0.0F : 0.3F * var3 * var3 * (var0 * var0 - var1 * var1) / (var2 * var2);
   }

   private static Shape pin() {
      Shape2D var0 = Shape2D.circle(0.0F, 0.0F, 0.68F).subtract(Shape2D.circle(0.34F, 0.18F, 0.52F));
      Shape2D var1 = Shape2D.star(0.46F, 0.14F, 0.38F, 0.17F, 5).rotate(180.0F, 0.46F, 0.14F);
      Shape var2 = Shapes.union(
         Shapes.extrude(var0, 0.14F, 0.06F).paint(Paint.radial(-4688, -0.3F, -0.2F, 0.0F, 0.8F, -1326758).glow()),
         Shapes.extrude(var1, 0.12F, 0.05F).move(0.0F, 0.0F, 0.04F).paint(Paint.of(-9872).glow())
      );
      double var3 = Math.toRadians(38.0);
      float var5 = 3.38F;
      return var2.rotate(-8.0F, 38.0F, 0.0F).move(var5 * (float)Math.sin(var3), 29.95F, -0.25F + var5 * (float)Math.cos(var3));
   }

   private static Shape worn(Shape var0) {
      return hs(var0.rotate(-8.0F, 0.0F, 4.0F, HAT_CENTRE.x(), HAT_CENTRE.y(), HAT_CENTRE.z()));
   }

   private static Vec3 hatPoint(float var0, float var1, float var2) {
      return new Vec3(var0, var1, var2).rotate(-8.0F, 0.0F, 4.0F, HAT_CENTRE);
   }

   private static void body(Rig var0) {
      Shape var1 = Shapes.ellipsoid(0.0F, 21.35F, 0.05F, 2.2F, 1.75F, 1.4F);
      Shape var2 = Shapes.capsule(0.0F, 22.95F, -0.15F, -2.45F, 22.4F, -0.15F, 0.72F).mirrorX();
      Shape var3 = Shapes.ellipsoid(0.0F, 19.0F, 0.0F, 1.62F, 1.5F, 1.15F);
      Shape var4 = Shapes.ellipsoid(0.0F, 16.8F, -0.05F, 2.2F, 1.75F, 1.5F);
      Shape var5 = Shapes.blend(1.1F, var1, var2, var3, var4);
      Shape var6 = var5.paint(Paint.vertical(-14933168, 16.5F, -13945223, 22.5F));
      Shape var7 = Shapes.capsule(0.0F, 22.9F, -0.15F, 0.0F, 25.1F, -0.05F, 0.6F)
         .paint(Paint.vertical(-6958, 23.7F, -1331558, 24.2F).flatLight(1.0F).occlusion(0.25F));
      Shape var8 = Shapes.cone(0.0F, 22.5F, -0.12F, 1.02F, 0.0F, 24.3F, -0.1F, 0.78F).intersect(Shapes.box(0.0F, 23.45F, -0.1F, 1.3F, 0.63F, 1.3F));
      Shape var9 = var5.inflate(0.22F).intersect(Shapes.box(0.0F, 18.45F, 0.0F, 3.0F, 0.6F, 3.0F)).paint(Paint.vertical(-11653344, 17.9F, -8762574, 18.9F));
      var0.part(Bone.BODY)
         .pivot(0.0F, 24.5F, 0.0F)
         .shape(Shapes.union(var6, var7, var9))
         .resolution(1.2F)
         .detail(0.9F)
         .occlusion(0.3F)
         .outline(-15921370, 0.14F)
         .decal(bodiceTrim());
      var0.extra("collar", Bone.BODY)
         .shape(var8.paint(Paint.vertical(-15460295, 22.9F, -13945223, 24.05F)))
         .resolution(0.6F)
         .detail(0.35F)
         .occlusion(0.3F)
         .outline(-15921370, 0.08F);
      ArrayList var10 = new ArrayList();
      float var11 = surfaceZ(var5, 0.0F, 23.1F, 4.0F);
      float var12 = Math.max(var11, surfaceZ(var8, 0.0F, 23.1F, 4.0F));
      var10.add(Shapes.ellipsoid(0.0F, 23.08F, var12 + 0.12F, 0.3F, 0.26F, 0.2F).color(-4544530));
      var10.add(Shapes.ellipsoid(0.6F, 23.14F, var12 + 0.02F, 0.5F, 0.3F, 0.18F).rotate(0.0F, -18.0F, -14.0F, 0.6F, 23.14F, var12).mirrorX().color(-4544530));

      for (byte var13 = -1; var13 <= 1; var13 += 2) {
         float var14 = surfaceZ(var5, var13 * 0.35F, 22.3F, 4.0F);
         float var15 = surfaceZ(var5, var13 * 0.55F, 21.6F, 4.0F);
         var10.add(
            Shapes.strand(0.22F, 0.16F, var13 * 0.12F, 22.95F, var12 + 0.08F, var13 * 0.35F, 22.3F, var14 + 0.14F, var13 * 0.55F, 21.6F, var15 + 0.12F)
               .flatten(1.7F, 0.0F, 0.0F, 1.0F)
               .color(-4544530)
         );
      }

      var10.add(Shapes.sphere(0.0F, 23.08F, var12 + 0.3F, 0.17F).paint(Paint.of(-4678401).glow()));
      float var16 = surfaceZ(var9, 0.0F, 18.45F, 4.0F);
      var10.add(
         Shapes.extrude(Shape2D.star(0.0F, 0.0F, 0.56F, 0.25F, 5).rotate(180.0F, 0.0F, 0.0F), 0.17F, 0.05F).move(0.0F, 18.45F, var16 + 0.08F).color(-1326758)
      );
      var0.extra("trim", Bone.BODY).shape(Shapes.union(var10.toArray(new Shape[0]))).resolution(0.5F).detail(0.55F).noOutline();
   }

   private static void cape(Rig var0) {
      Shape var1 = Shapes.field(Luna::capeDistance, -6.0F, 6.3F, -6.2F, 6.0F, 25.4F, 1.3F).paint(Paint.field(Luna::capeColor));
      var0.extra("cape", Bone.BODY)
         .pivot(0.0F, 23.0F, -1.0F)
         .shape(var1)
         .resolution(1.35F)
         .motion(Motion.cloth().drag(12.0F).hang(0.6F))
         .detail(0.45F)
         .occlusion(0.3F)
         .noOutline()
         .decal(capeStars())
         .decal(capeHem());
   }

   private static float capeAt(int var0, float var1) {
      float var2 = (var1 - CAPE_Y[0]) / (CAPE_Y[CAPE_Y.length - 1] - CAPE_Y[0]) * 96.0F;
      var2 = var2 < 0.0F ? 0.0F : Math.min(var2, 96.0F);
      int var3 = Math.min((int)var2, 95);
      float var4 = CAPE_LUT[var3 * 4 + var0];
      return var4 + (CAPE_LUT[var3 * 4 + 4 + var0] - var4) * (var2 - var3);
   }

   private static float[] capeTable() {
      float[] var0 = new float[388];

      for (int var1 = 0; var1 <= 96; var1++) {
         float var2 = CAPE_Y[0] + (CAPE_Y[CAPE_Y.length - 1] - CAPE_Y[0]) * var1 / 96.0F;
         var0[var1 * 4] = spline(CAPE_Y, CAPE_RX, var2);
         var0[var1 * 4 + 1] = spline(CAPE_Y, CAPE_RZ, var2);
         var0[var1 * 4 + 2] = spline(CAPE_Y, CAPE_CZ, var2);
         var0[var1 * 4 + 3] = spline(CAPE_Y, CAPE_WRAP, var2);
      }

      return var0;
   }

   private static float capeDistance(float var0, float var1, float var2) {
      float var4 = capeAt(0, var1);
      float var5 = capeAt(1, var1);
      float var6 = capeAt(2, var1);
      float var7 = Math.abs(ellipse(var0, var2 - var6, var4, var5)) - 0.43F;
      if (var7 < 0.6F) {
         float var8 = (float)Math.toDegrees(Math.atan2(Math.abs(var0) / var4, -(var2 - var6) / var5));
         var7 = Math.max(var7, (var8 - capeAt(3, var1)) * 0.0174533F * (var4 + var5) * 0.5F);
      }

      var7 = Math.max(var7, Math.max(var1 - 23.4F, 7.0F - var1));
      if (var1 < 22.5F) {
         return var7;
      } else {
         float var13 = clamp01((var1 - 22.9F) / 2.3F);
         float var9 = 1.45F + 0.35F * var13;
         float var3;
         float var10 = Math.abs(ellipse(var0, var2 + 0.3F, var9, var3 = 1.3F + 0.3F * var13)) - 0.38F;
         if (var10 < 0.6F) {
            float var11 = (float)Math.toDegrees(Math.atan2(Math.abs(var0) / var9, -(var2 + 0.3F) / var3));
            var10 = Math.max(var10, (var11 - 118.0F) * 0.0174533F * 1.5F);
         }

         var10 = Math.max(var10, Math.max(var1 - 25.2F, 22.8F - var1));
         return Math.min(var7, var10);
      }
   }

   private static int capeColor(float var0, float var1, float var2) {
      float var5 = capeAt(0, var1);
      float var6 = capeAt(1, var1);
      float var7 = capeAt(2, var1);
      float var3;
      float var4;
      if (var1 > 22.9F
         && Math.abs(var4 = ellipse(var0, var2 + 0.3F, 1.45F + 0.35F * (var3 = clamp01((var1 - 22.9F) / 2.3F)), 1.3F + 0.3F * var3))
            < Math.abs(ellipse(var0, var2 - var7, var5, var6))) {
         return var4 > 0.0F ? -14603682 : -7571752;
      } else {
         float var8 = ellipse(var0, var2 - var7, var5, var6);
         return var8 < 0.0F ? -7571752 : Colors.mix(-15460295, -14603682, clamp01((var1 - 7.0F) / 14.0F));
      }
   }

   private static Decal capeStars() {
      Canvas var0 = new Canvas(176, 176);
      long var1 = 7L;

      for (int var3 = 0; var3 < 36; var3++) {
         long var9 = var1 * 6364136223846793005L + 1442695040888963407L;
         float var4 = 7.0F + (float)((var9 >>> 33) % 162L);
         long var10 = var9 * 6364136223846793005L + 1442695040888963407L;
         float var5 = 8.0F + (float)((var10 >>> 33) % 160L);
         var1 = var10 * 6364136223846793005L + 1442695040888963407L;
         float var6 = 2.8F + (float)((var1 >>> 33) % 3L);
         boolean var7 = (var1 >>> 40) % 3L == 0L;
         Shape2D var8 = var7 ? Shape2D.star(var4, var5, var6 * 1.3F, var6 * 0.3F, 4) : Shape2D.star(var4, var5, var6, var6 * 0.46F, 5);
         var0.fill(var8, var7 ? -598638 : -1326758);
      }

      var0.fill(Shape2D.circle(88.0F, 92.0F, 19.0F).subtract(Shape2D.circle(97.0F, 86.0F, 16.0F)), -1326758);
      var0.fill(Shape2D.star(97.0F, 88.0F, 7.5F, 3.2F, 5), -598638);
      return Decal.back(var0, 0.0F, 12.5F, -3.0F, 11.0F, 11.0F).depth(4.0F, 4.0F).maxAngle(62.0F);
   }

   private static Decal capeHem() {
      Canvas var0 = new Canvas(192, 12);
      paintBand(var0);
      return Decal.wrap(var0, 0.0F, 7.27F, -0.8F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, 230.0F, 0.48F).offset(0.03F);
   }

   private static void paintBand(Canvas var0) {
      float var1 = var0.height();
      float var2 = var1 * 2.0F;
      var0.fillAll((var2x, var3) -> {
         float var4 = var3 / var1;
         if (var4 < 0.1F || var4 > 0.9F) {
            return 0;
         } else if (!(var4 < 0.24F) && !(var4 > 0.76F)) {
            float var5 = ((float)Math.floor(var2x / var2) + 0.5F) * var2;
            return Math.abs(var2x - var5) + Math.abs(var3 - var1 * 0.5F) < var1 * 0.22F ? -2868 : -1326758;
         } else {
            return -5011921;
         }
      });
   }

   private static Canvas lace(int var0, int var1) {
      Canvas var2 = new Canvas(var0, 16);
      float var3 = (float)var0 / var1;
      var2.fill(Shape2D.box(var0 * 0.5F, 3.0F, var0 * 0.5F, 3.0F, 0.0F), -1036);

      for (int var4 = 0; var4 < var1; var4++) {
         float var5 = (var4 + 0.5F) * var3;
         var2.fill(Shape2D.circle(var5, 6.5F, var3 * 0.52F), -1036);
         var2.erase(Shape2D.circle(var5, 6.8F, var3 * 0.16F));
      }

      return var2;
   }

   private static void skirt(Rig var0, Canvas var1) {
      Canvas var2 = lace(448, 34);

      for (byte var3 = -1; var3 <= 1; var3 += 2) {
         Bone var4 = var3 < 0 ? Bone.RIGHT_LEG : Bone.LEFT_LEG;
         Shape var5 = Shapes.union(
            tier(var3, 18.5F, 12.4F, 1.75F, 3.45F, 2.5F, 11.0F, 0.24F, 0.78F, 6.0F, 16.0F).shape(-13945223, -15460295),
            tier(var3, 13.4F, 9.1F, 3.1F, 3.95F, 1.3F, 14.0F, 0.28F, 0.74F, 14.0F, 22.0F).shape(-12693598, -14603682),
            tier(var3, 9.55F, 8.6F, 3.7F, 4.05F, 1.0F, 20.0F, 0.16F, 0.74F, 20.0F, 24.0F).shape(-4611086, -7571752)
         );
         var0.extra(var3 < 0 ? "skirt_r" : "skirt_l", Bone.BODY)
            .pivot(0.0F, 18.2F, 0.0F)
            .shape(var5)
            .resolution(1.15F)
            .motion(Motion.skirt(var4).follow(var4, 0.3F, 12.0F))
            .detail(0.4F)
            .occlusion(0.3F)
            .noOutline()
            .decal(hemBand(var1, 12.58F, var3 * 76.0F, 140.0F, 0.36F))
            .decal(hemBand(var1, 9.28F, var3 * 79.0F, 134.0F, 0.36F))
            .decal(hemBand(var2, 8.8F, var3 * 80.0F, 132.0F, 0.34F));
      }

      Luna.Tier var6 = new Luna.Tier(18.2F, 8.7F, 1.6F, 2.9F, 1.6F, 16.0F, 0.12F, 0.66F, -42.0F, -42.0F, 42.0F, 42.0F);
      var0.extra("underskirt", Bone.BODY)
         .pivot(0.0F, 18.2F, 0.2F)
         .shape(var6.shape(-4611086, -7571752))
         .resolution(1.1F)
         .motion(new Motion().drag(-8.0F).walk(-5.0F, 0.0F, 0.0F, 2.0F).walkPhase(90.0F).spring(120.0F, 14.0F).bend(1.0F))
         .detail(0.4F)
         .occlusion(0.3F)
         .noOutline()
         .decal(underskirtStars());
   }

   private static Luna.Tier tier(
      int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10
   ) {
      return var0 > 0
         ? new Luna.Tier(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, 188.0F, 188.0F)
         : new Luna.Tier(var1, var2, var3, var4, var5, var6, var7, var8, -188.0F, -188.0F, -var9, -var10);
   }

   private static Decal hemBand(Canvas var0, float var1, float var2, float var3, float var4) {
      double var5 = Math.toRadians(var2);
      return Decal.wrap(var0, 0.0F, var1, 0.0F, 0.0F, 1.0F, 0.0F, (float)Math.sin(var5), 0.0F, (float)Math.cos(var5), var3, var4).offset(0.03F);
   }

   private static Decal underskirtStars() {
      Canvas var2 = new Canvas(192, 288);
      long var3 = 3L;

      for (int var1 = 0; var1 < 20; var1++) {
         long var8 = var3 * 6364136223846793005L + 1442695040888963407L;
         float var0 = 12.0F + (float)((var8 >>> 33) % 168L);
         var3 = var8 * 6364136223846793005L + 1442695040888963407L;
         float var5 = 88.0F + (float)((var3 >>> 33) % 170L);
         var2.fill(Shape2D.star(var0, var5, 5.4F, 1.7F, 4), -1326758);
      }

      for (int var7 = 0; var7 < 18; var7++) {
         float var6 = (var7 + 0.5F) * 192.0F / 18.0F;
         var2.fill(Shape2D.circle(var6, 278.0F, 6.2F), -1036);
         var2.erase(Shape2D.circle(var6, 278.5F, 1.9F));
      }

      var2.fill(Shape2D.box(96.0F, 272.0F, 96.0F, 4.0F, 0.0F), -1036);
      return Decal.front(var2, 0.0F, 13.5F, 3.0F, 6.4F, 9.6F).depth(3.5F, 3.0F).maxAngle(66.0F);
   }

   private static Decal bodiceTrim() {
      Canvas var0 = new Canvas(64, 96);

      for (byte var1 = -1; var1 <= 1; var1 += 2) {
         var0.fill(Shape2D.path(2.0F, 2.0F, 32.0F + var1 * 10.5F, 2.0F, 32.0F + var1 * 9.0F, 48.0F, 32.0F + var1 * 9.8F, 94.0F), -1326758);
      }

      for (int var3 = 0; var3 < 4; var3++) {
         float var2 = 13.0F + var3 * 21.0F;
         var0.fill(Shape2D.circle(32.0F, var2, 5.0F), -5011921);
         var0.fill(Shape2D.circle(32.0F, var2, 4.0F), -1326758);
         var0.fill(Shape2D.circle(30.8F, var2 - 1.3F, 1.5F), -2868);
      }

      return Decal.front(var0, 0.0F, 20.6F, 3.0F, 2.0F, 3.0F).depth(2.5F, 2.0F).maxAngle(70.0F);
   }

   private static Shape fore(Shape var0, int var1) {
      return var0.rotate(-12.0F, 0.0F, var1 * 3.0F, var1 * ELBOW.x(), ELBOW.y(), ELBOW.z());
   }

   private static Vec3 fore(float var0, float var1, float var2) {
      return new Vec3(var0, var1, var2).rotate(-12.0F, 0.0F, 3.0F, ELBOW);
   }

   private static void arms(Rig var0, Canvas var1) {
      Paint var2 = Paint.of(-6958).flatLight(1.0F).occlusion(0.25F);
      Shape var3 = Shapes.ellipsoid(-3.5F, 22.0F, -0.1F, 0.92F, 1.0F, 0.9F);
      Shape var4 = Shapes.cone(-3.55F, 21.5F, -0.1F, 0.66F, -3.72F, 18.45F, -0.05F, 0.55F);
      Shape var5 = Shapes.sphere(-3.72F, 18.4F, -0.05F, 0.55F);
      Shape var6 = fore(Shapes.cone(-3.72F, 18.3F, -0.05F, 0.54F, -3.84F, 15.6F, 0.12F, 0.5F), 1);
      Shape var7 = Shapes.blend(0.35F, var3, var4, var5, var6).paint(Paint.vertical(-14867372, 15.0F, -13945223, 22.0F));
      Shape var8 = fore(Shapes.cone(-3.84F, 16.1F, 0.12F, 0.52F, -3.92F, 14.3F, 0.2F, 1.02F).intersect(Shapes.above(14.3F)), 1).color(-13945223);
      Shape var9 = fore(Shapes.cone(-3.84F, 15.7F, 0.12F, 0.2F, -3.93F, 14.0F, 0.2F, 0.5F), 1).color(-7571752);
      Shape var10 = Shapes.union(var7, Shapes.subtract(var8, var9));
      Vec3 var11 = fore(-4.0F, 13.3F, 0.42F);
      Vec3 var12 = fore(-3.91F, 14.52F, 0.2F);
      Vec3 var13 = new Vec3(-0.04F, 1.0F, 0.05F).rotate(-12.0F, 0.0F, 3.0F);
      Vec3 var14 = new Vec3(0.0F, 0.0F, 1.0F).rotate(-12.0F, 0.0F, 3.0F);
      Part var15 = var0.part(Bone.RIGHT_ARM)
         .pivot(-3.5F, 22.3F, -0.1F)
         .grip(var11.x(), var11.y(), var11.z())
         .shape(var10)
         .resolution(0.95F)
         .detail(0.75F)
         .occlusion(0.3F)
         .outline(-15921370, 0.13F)
         .swing(0.8F)
         .joint(ELBOW.x(), ELBOW.y(), ELBOW.z(), 0.55F)
         .decal(
            Decal.wrap(var1, var12.x(), var12.y(), var12.z(), var13.x(), var13.y(), var13.z(), var14.x(), var14.y(), var14.z(), 360.0F, 0.36F).offset(0.03F)
         );
      var0.part(Bone.LEFT_ARM).mirrorOf(var15);
      ArrayList var16 = new ArrayList();
      var16.add(Shapes.cone(3.92F, 14.6F, 0.22F, 0.33F, 3.97F, 13.8F, 0.28F, 0.33F));
      var16.add(Shapes.ellipsoid(4.0F, 13.25F, 0.32F, 0.27F, 0.6F, 0.52F));
      float[] var17 = new float[]{-24.0F, -19.0F, -14.0F, -8.0F};

      for (int var18 = 0; var18 < 4; var18++) {
         float var19 = 0.02F + var18 * 0.21F;
         float var20 = var18 != 0 && var18 != 3 ? 0.8F : 0.62F;
         var16.add(Shapes.capsule(4.02F, 12.8F, var19, 3.94F, 12.8F - var20, var19 + 0.1F, 0.12F).rotate(0.0F, 0.0F, var17[var18], 4.02F, 12.85F, var19));
      }

      var16.add(Shapes.capsule(3.86F, 13.35F, 0.76F, 3.74F, 12.75F, 0.96F, 0.14F));
      Part var21 = var0.extra("hand_l", Bone.LEFT_ARM)
         .shape(fore(Shapes.blend(0.18F, var16.toArray(new Shape[0])).paint(var2), -1))
         .resolution(0.5F)
         .detail(0.6F)
         .outline(-5737368, 0.08F);
      var0.extra("hand_r", Bone.RIGHT_ARM).mirrorOf(var21);
   }

   private static void legs(Rig var0) {
      Shape var1 = Shapes.cone(-1.3F, 15.3F, 0.0F, 1.18F, -1.1F, 8.6F, 0.08F, 0.82F);
      Shape var2 = Shapes.sphere(-1.08F, 8.25F, 0.14F, 0.78F);
      Shape var3 = Shapes.sphere(-1.08F, 8.3F, 0.45F, 0.55F);
      Shape var4 = Shapes.cone(-1.08F, 8.0F, 0.1F, 0.8F, -1.14F, 2.1F, -0.1F, 0.5F);
      Shape var5 = Shapes.ellipsoid(-1.12F, 5.9F, -0.3F, 0.85F, 1.5F, 0.9F);
      Shape var6 = Shapes.blend(0.45F, var1, var2, var3, var4, var5).paint(Paint.field(Luna::tights));
      Shape var7 = Shapes.cone(-1.12F, 4.9F, -0.08F, 0.8F, -1.1F, 1.5F, -0.1F, 0.62F).intersect(Shapes.below(5.0F));
      Shape var8 = Shapes.ellipsoid(-1.1F, 0.48F, 1.0F, 0.6F, 0.48F, 0.82F);
      Shape var9 = Shapes.ellipsoid(-1.1F, 1.0F, 0.25F, 0.64F, 0.62F, 0.9F);
      Shape var10 = Shapes.blend(0.35F, var7, var8, var9).paint(Paint.vertical(-9550799, 0.5F, -7710146, 4.6F));
      Shape var11 = Shapes.box(-1.1F, 0.42F, -0.5F, 0.4F, 0.42F, 0.36F, 0.14F).color(-12704232);
      Shape var12 = Shapes.box(-1.1F, 0.12F, 0.97F, 0.64F, 0.12F, 0.8F, 0.1F).color(-12966888);
      Shape var13 = Shapes.torus(-1.1F, 1.75F, -0.05F, 0.62F, 0.26F).color(-11653344);
      Shape var14 = Shapes.ellipsoid(-1.72F, 1.75F, 0.57F, 0.24F, 0.3F, 0.14F).rotate(0.0F, -45.0F, 0.0F, -1.72F, 1.75F, 0.57F).color(-1326758);
      Shape var15 = Shapes.union(var10, var11, var12, var13, var14).rotate(0.0F, -7.0F, 0.0F, -1.12F, 0.0F, -0.1F);
      Shape var16 = Shapes.union(var6, var15).intersect(Shapes.above(0.0F));
      Part var17 = var0.part(Bone.RIGHT_LEG)
         .pivot(-1.3F, 15.3F, 0.0F)
         .shape(var16)
         .resolution(1.2F)
         .detail(0.75F)
         .occlusion(0.3F)
         .outline(-14411226, 0.13F)
         .swing(0.55F, 30.0F)
         .joint(-1.08F, 8.25F, 0.14F, 0.8F);
      var0.part(Bone.LEFT_LEG).mirrorOf(var17);
      Shape var18 = Shapes.cylinder(-1.12F, 4.45F, -0.08F, -1.12F, 5.18F, -0.08F, 0.94F, 0.18F).paint(Paint.vertical(-7710146, 4.5F, -6395064, 5.1F));
      Part var19 = var0.extra("cuff_r", Bone.RIGHT_LEG).shape(var18).resolution(0.7F).detail(0.9F).occlusion(0.3F).outline(-12966888, 0.08F);
      var0.extra("cuff_l", Bone.LEFT_LEG).mirrorOf(var19);
   }

   private static int tights(float var0, float var1, float var2) {
      float var3 = -1.1F - 0.2F * clamp01((var1 - 8.0F) / 7.0F);
      float var4 = 0.05F;
      float var5 = (float)Math.atan2(var0 - var3, var2 - var4);
      float var6 = (float)Math.exp(-(var5 + 0.35F) * (var5 + 0.35F) / 0.16F);
      int var7 = Colors.mix(Colors.shade(-12959880, 0.82F), -12959880, clamp01((var1 - 3.0F) / 6.0F));
      return Colors.mix(var7, -10656082, 0.55F * var6);
   }

   private static Decal faceDecal(List<Luna.Lock> var0) {
      Canvas var1 = new Canvas(192, 158);
      Shape2D var2 = null;

      for (Luna.Lock var4 : var0) {
         Shape2D var5 = var4.shadow(0.28F).inflate(3.7333333F);
         var2 = var2 == null ? var5 : var2.union(var5);
      }

      var1.fill(var2, Colors.withAlpha(-1199194, 0.55F), 4.0F);
      var1.draw(features(), 12, 44);
      Vec3 var6 = hp(0.0F, 26.90625F, 3.0F);
      return Decal.front(var1, var6.x(), var6.y(), var6.z(), 4.8599997F, 3.9993749F).depth(3.0F, 3.0F).maxAngle(74.0F).occlusion(0.3F).flatLight(1.0F);
   }

   private static Canvas features() {
      float var0 = 0.5833333F;
      Canvas var1 = new Canvas(168, 114);
      Canvas var2 = new Canvas(64, 80);
      eye(new Luna.Pen(var2, var0, 60.0F, 96.0F), 122.0F, 181.0F, -1);
      var1.draw(var2, 7, 0);
      var1.draw(var2.mirrored(), 97, 0);
      Luna.Pen var3 = new Luna.Pen(var1, var0, 48.0F, 96.0F);

      for (byte var4 = -1; var4 <= 1; var4 += 2) {
         float var5 = 192.0F + var4 * 70.0F;
         float var6 = 181.0F;
         var3.fill(Shape2D.star(var5 - 10.0F, var6 - 13.0F, 14.0F, 3.8F, 4), -1);
         var3.fill(Shape2D.circle(var5 - 10.0F, var6 - 13.0F, 4.8F), -1);
         var3.fill(Shape2D.star(var5 + 12.0F, var6 + 17.0F, 7.0F, 2.1F, 4), -285212673);
         var3.fill(Shape2D.circle(var5 + 10.0F, var6 - 2.0F, 2.3F), -1191182337);
         var3.fill(Shape2D.ellipse(192.0F + var4 * 96.0F, 226.0F, 27.0F, 12.0F), 1895793815, 12.0F);
      }

      var3.fill(Shape2D.curve(191.0F, 232.0F, 195.0F, 238.0F, 190.0F, 242.0F, 2.4F, 1.6F), -2135459206);
      var3.fill(Shape2D.curve(180.0F, 275.0F, 192.0F, 284.0F, 204.0F, 275.0F, 3.2F, 3.2F), -7060390);
      var3.fill(Shape2D.curve(188.0F, 285.0F, 192.0F, 287.0F, 196.0F, 285.0F, 1.8F, 1.8F), 1438670960);
      return var1;
   }

   private static void eye(Luna.Pen var0, float var1, float var2, int var3) {
      Shape2D var4 = Shape2D.ellipse(var1, var2 + 2.0F, 37.0F, 46.0F);
      var0.fill(var4, Paint2D.linear(var1, var2 - 44.0F, var1, var2 - 8.0F, -3160852, -1));
      Paint2D var5 = Paint2D.linear(var1, var2 - 36.0F, var1, var2 + 44.0F, -14282676, -11194192, -6656782, -2044673);
      var0.fill(Shape2D.ellipse(var1, var2 + 4.0F, 29.0F, 41.0F).intersect(var4), (var3x, var4x) -> {
         float var5x = (var3x - var1) / 32.0F;
         float var6 = (var4x - var2 + 28.0F) / 19.0F;
         float var7 = (var3x - var1) / 18.0F;
         float var8 = (var4x - var2 - 27.0F) / 10.0F;
         float var9 = (var3x - var1) / 29.0F;
         float var10 = (var4x - var2 - 4.0F) / 41.0F;
         float var11 = clamp01((1.0F - (float)Math.sqrt(var5x * var5x + var6 * var6)) * 8.0F) * 0.47F;
         float var12 = clamp01((1.0F - (float)Math.sqrt(var7 * var7 + var8 * var8)) * 5.0F) * 0.5F;
         float var13 = clamp01(((float)Math.sqrt(var9 * var9 + var10 * var10) - 0.88F) / 0.06F) * 0.82F;
         int var14 = Colors.mix(Colors.mix(var5.argb(var3x, var4x), -14676416, var11), -726785, var12);
         return Colors.mix(var14, -14479041, var13);
      });
      var0.fill(Shape2D.ellipse(var1, var2 + 5.0F, 12.0F, 17.0F), -15136210);
      var0.fill(
         Shape2D.path(
            6.0F,
            12.0F,
            var1 - var3 * 37.0F,
            var2 - 6.0F,
            var1 - var3 * 24.0F,
            var2 - 36.0F,
            var1 + var3 * 2.0F,
            var2 - 48.0F,
            var1 + var3 * 25.0F,
            var2 - 42.0F,
            var1 + var3 * 39.0F,
            var2 - 26.0F,
            var1 + var3 * 50.0F,
            var2 - 32.0F
         ),
         -14938064
      );
      var0.fill(Shape2D.curve(var1 + var3 * 40.0F, var2 - 28.0F, var1 + var3 * 52.0F, var2 - 40.0F, var1 + var3 * 58.0F, var2 - 50.0F, 5.0F, 1.0F), -14938064);
      var0.fill(Shape2D.curve(var1 + var3 * 30.0F, var2 - 38.0F, var1 + var3 * 38.0F, var2 - 50.0F, var1 + var3 * 40.0F, var2 - 58.0F, 3.5F, 1.0F), -14938064);
      var0.fill(Shape2D.curve(var1 + var3 * 6.0F, var2 + 48.0F, var1 + var3 * 26.0F, var2 + 44.0F, var1 + var3 * 35.0F, var2 + 28.0F, 1.5F, 4.0F), -1069931696);
      var0.fill(Shape2D.path(2.0F, 2.0F, var1 - var3 * 18.0F, var2 - 55.0F, var1 + var3 * 4.0F, var2 - 60.0F, var1 + var3 * 27.0F, var2 - 53.0F), 1883909461);
      var0.fill(Shape2D.path(5.5F, 2.0F, var1 - var3 * 28.0F, var2 - 70.0F, var1 + var3 * 2.0F, var2 - 82.0F, var1 + var3 * 36.0F, var2 - 76.0F), -7703368);
   }

   private static float ellipse(float var0, float var1, float var2, float var3) {
      float var4 = var0 / var2;
      float var5 = var1 / var3;
      float var6 = (float)Math.sqrt(var4 * var4 + var5 * var5);
      float var7 = (float)Math.sqrt(var4 * var4 / (var2 * var2) + var5 * var5 / (var3 * var3));
      return var7 < 1.0E-6F ? -Math.min(var2, var3) : var6 * (var6 - 1.0F) / var7;
   }

   private static float spline(float[] var0, float[] var1, float var2) {
      int var4 = var0.length;
      if (var2 <= var0[0]) {
         return var1[0];
      } else if (var2 >= var0[var4 - 1]) {
         return var1[var4 - 1];
      } else {
         int var3 = 0;

         while (var3 < var4 - 2 && var2 > var0[var3 + 1]) {
            var3++;
         }

         float var5 = (var2 - var0[var3]) / (var0[var3 + 1] - var0[var3]);
         float var6 = var0[var3 + 1] - var0[var3];
         float var7 = var3 == 0 ? (var1[1] - var1[0]) / (var0[1] - var0[0]) : (var1[var3 + 1] - var1[var3 - 1]) / (var0[var3 + 1] - var0[var3 - 1]);
         float var8 = var3 + 2 >= var4
            ? (var1[var4 - 1] - var1[var4 - 2]) / (var0[var4 - 1] - var0[var4 - 2])
            : (var1[var3 + 2] - var1[var3]) / (var0[var3 + 2] - var0[var3]);
         float var9 = var5 * var5;
         float var10 = var9 * var5;
         return (2.0F * var10 - 3.0F * var9 + 1.0F) * var1[var3]
            + (var10 - 2.0F * var9 + var5) * var6 * var7
            + (-2.0F * var10 + 3.0F * var9) * var1[var3 + 1]
            + (var10 - var9) * var6 * var8;
      }
   }

   private static float surfaceZ(Shape var0, float var1, float var2, float var3) {
      float var5 = var3;

      float var4;
      for (int var6 = 0; var6 < 80 && !(Math.abs(var4 = var0.distance(var1, var2, var5)) < 0.001F); var6++) {
         var5 -= var4;
      }

      return var5;
   }

   private static float clamp01(float var0) {
      return var0 < 0.0F ? 0.0F : Math.min(var0, 1.0F);
   }

   @Environment(EnvType.CLIENT)
   private static final class Lock {
      private static final float REF = 2.6F;
      private final float rad;
      private final float top;
      private final float tip;
      private final float width;
      private final float hook;
      private final float g0;
      private final float lift;
      private final float g1;
      private final float t0;
      private final float t1;
      private final float[] ys;
      private final float[] rs;
      private final float[] poly;
      private final Shape2D outline;

      Lock(
         float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float[] var11, float[] var12
      ) {
         this.rad = (float)Math.toRadians(var1);
         this.top = var2;
         this.tip = var3;
         this.width = var4;
         this.hook = var5;
         this.g0 = var6;
         this.lift = var7;
         this.g1 = var8;
         this.t0 = var9;
         this.t1 = var10;
         this.ys = var11;
         this.rs = var12;
         byte var15 = 7;
         float var16 = var5 > 0.0F ? -1.0F : 1.0F;
         this.poly = new float[(2 * var15 + 3) * 2];
         int var17 = 0;
         this.poly[var17++] = var16 * var4;
         this.poly[var17++] = var2 + 0.5F;

         for (int var14 = 0; var14 <= var15; var14++) {
            float var13 = 1.0F - (float)Math.pow(1.0F - (float)var14 / var15, 1.399999976158142);
            this.poly[var17++] = var5 * var13 * var13 + var16 * var4 * (float)Math.pow(1.0F - var13, 0.800000011920929);
            this.poly[var17++] = var2 + (var3 - var2) * var13;
         }

         for (int var19 = var15 - 1; var19 >= 0; var19--) {
            float var18 = 1.0F - (float)Math.pow(1.0F - (float)var19 / var15, 1.399999976158142);
            this.poly[var17++] = var5 * var18 * var18 - var16 * var4 * (float)Math.pow(1.0F - var18, 1.25);
            this.poly[var17++] = var2 + (var3 - var2) * var18;
         }

         this.poly[var17++] = -var16 * var4;
         this.poly[var17] = var2 + 0.5F;
         this.outline = Shape2D.polygon(this.poly);
      }

      private float gap(float var1) {
         float var2 = 1.0F - var1;
         return this.g0 * var2 * var2 + 2.0F * this.lift * var1 * var2 + this.g1 * var1 * var1;
      }

      float distance(float var1, float var2, float var3) {
         float var5 = var3 - -0.3F;
         float var6 = (float)Math.sqrt(var1 * var1 + var5 * var5);
         float var7 = Luna.clamp01((this.top - var2) / (this.top - this.tip));
         float var8 = Luna.interp(this.ys, this.rs, var2) + this.gap(var7);
         float var4;
         float var9 = Math.max(var8 - var6, var6 - var8 - (var4 = this.t0 + (this.t1 - this.t0) * var7));
         if (var9 > 0.8F) {
            return var9;
         } else {
            float var10 = 2.6F * ((float)Math.atan2(var1, var5) - this.rad);
            float var11 = this.outline.distance(var10, var2);
            if (var11 > 0.8F) {
               return var11;
            } else {
               float var12 = Math.min(1.0F, Math.abs(var10 - this.hook * var7 * var7) / (this.width * (1.0F - var7) + 0.05F));
               var9 = Math.max(var8 - var6, var6 - var8 - var4 * (1.0F - 0.35F * var12 * var12));
               float var13 = Luna.clamp01(0.5F - 0.5F * (var9 - var11) / 0.07F);
               return var9 + (var11 - var9) * var13 + 0.07F * var13 * (1.0F - var13);
            }
         }
      }

      Shape shape() {
         float var1 = 3.4028235E38F;
         float var2 = -3.4028235E38F;
         float var3 = 3.4028235E38F;
         float var4 = 0.0F;

         for (byte var5 = 0; var5 < this.poly.length; var5 += 2) {
            var1 = Math.min(var1, this.poly[var5]);
            var2 = Math.max(var2, this.poly[var5]);
         }

         for (float var8 : this.rs) {
            var3 = Math.min(var3, var8);
            var4 = Math.max(var4, var8);
         }

         var3 = Math.max(0.2F, var3 + Math.min(this.g0, this.g1) - 0.15F);
         var4 += Math.max(this.g0, Math.max(this.lift, this.g1)) + this.t0 + 0.15F;
         float var19 = 3.4028235E38F;
         float var20 = -3.4028235E38F;
         float var21 = 3.4028235E38F;
         float var22 = -3.4028235E38F;

         for (int var9 = 0; var9 <= 12; var9++) {
            double var10 = this.rad + (var1 + (var2 - var1) * var9 / 12.0F) / 2.6F;

            for (float var15 : new float[]{var3, var4}) {
               var19 = Math.min(var19, var15 * (float)Math.sin(var10));
               var20 = Math.max(var20, var15 * (float)Math.sin(var10));
               var21 = Math.min(var21, -0.3F + var15 * (float)Math.cos(var10));
               var22 = Math.max(var22, -0.3F + var15 * (float)Math.cos(var10));
            }
         }

         return Shapes.field(this::distance, var19 - 0.1F, this.tip - 0.15F, var21 - 0.1F, var20 + 0.1F, this.top + 0.6F, var22 + 0.1F);
      }

      Shape2D shadow(float var1) {
         float[] var2 = (float[])this.poly.clone();

         for (byte var3 = 0; var3 < var2.length; var3 += 2) {
            float var4 = var2[var3 + 1];
            float var5 = Luna.interp(this.ys, this.rs, var4) + this.gap(Luna.clamp01((this.top - var4) / (this.top - this.tip))) + this.t0 * 0.5F;
            var2[var3] = (var5 * (float)Math.sin(this.rad + var2[var3] / 2.6F) - -2.3142858F) * 41.48148F;
            var2[var3 + 1] = (28.810715F - var4 + var1) * 41.48148F;
         }

         return Shape2D.polygon(var2);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Pen {
      private final Canvas canvas;
      private final float k;
      private final float ox;
      private final float oy;

      Pen(Canvas var1, float var2, float var3, float var4) {
         this.canvas = var1;
         this.k = var2;
         this.ox = var3;
         this.oy = var4;
      }

      private Shape2D map(Shape2D var1) {
         float var2 = this.k / (this.k - 1.0F);
         return var1.scale(this.k, this.ox * var2, this.oy * var2);
      }

      void fill(Shape2D var1, int var2) {
         this.canvas.fill(this.map(var1), var2);
      }

      void fill(Shape2D var1, int var2, float var3) {
         this.canvas.fill(this.map(var1), var2, var3 * this.k);
      }

      void fill(Shape2D var1, Paint2D var2) {
         this.canvas.fill(this.map(var1), (var2x, var3) -> var2.argb(var2x / this.k + this.ox, var3 / this.k + this.oy));
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Tier {
      private static final int STEPS = 48;
      private final float yTop;
      private final float yBot;
      private final float rTop;
      private final float rBot;
      private final float folds;
      private final float amp;
      private final float half;
      private final float a0Top;
      private final float a0Bot;
      private final float a1Top;
      private final float a1Bot;
      private final float[] shape = new float[98];

      Tier(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12) {
         this.yTop = var1;
         this.yBot = var2;
         this.rTop = var3;
         this.rBot = var4;
         this.folds = var6;
         this.amp = var7;
         this.half = var8 * 0.5F;
         this.a0Top = var9;
         this.a0Bot = var10;
         this.a1Top = var11;
         this.a1Bot = var12;

         for (int var13 = 0; var13 <= 48; var13++) {
            float var14 = Math.min(var13 / 48.0F, 0.999F);
            this.shape[var13 * 2] = 1.0F - (float)Math.pow(1.0F - var14, var5);
            this.shape[var13 * 2 + 1] = var5 * (float)Math.pow(1.0F - var14, var5 - 1.0F);
         }
      }

      private float u(float var1) {
         return Luna.clamp01((this.yTop - var1) / (this.yTop - this.yBot));
      }

      private float table(int var1, float var2) {
         float var3 = var2 * 48.0F;
         int var4 = Math.min((int)var3, 47);
         float var5 = this.shape[var4 * 2 + var1];
         return var5 + (this.shape[var4 * 2 + 2 + var1] - var5) * (var3 - var4);
      }

      float radius(float var1, float var2, float var3) {
         float var4 = this.u(var2);
         return this.rTop + (this.rBot - this.rTop) * this.table(0, var4) + this.amp * var4 * (float)Math.cos(this.folds * Math.atan2(var1, var3));
      }

      float distance(float var1, float var2, float var3) {
         float var7 = this.u(var2);
         float var8 = this.yTop - this.yBot;
         float var9 = (float)Math.sqrt(var1 * var1 + var3 * var3);
         float var6;
         float var10 = Math.abs(var9 - (var6 = this.rTop + (this.rBot - this.rTop) * this.table(0, var7))) - this.half - this.amp;
         if (var10 > 0.6F) {
            return Math.max(var10 * 0.7F, Math.max(var2 - this.yTop, this.yBot - var2));
         } else {
            double var11 = Math.atan2(var1, var3);
            float var13 = (float)Math.cos(this.folds * var11);
            float var14 = (float)Math.sin(this.folds * var11);
            float var15 = var6 + this.amp * var7 * var13;
            float var16 = ((this.rBot - this.rTop) * this.table(1, var7) + this.amp * var13) / var8;
            float var17 = this.amp * var7 * this.folds * var14 / Math.max(var9, 0.5F);
            float var18 = (Math.abs(var9 - var15) - this.half) / (float)Math.sqrt(1.0F + var16 * var16 + var17 * var17);
            var18 = Math.max(var18, Math.max(var2 - this.yTop, this.yBot - var2));
            double var19 = Math.toRadians(this.a0Top + (this.a0Bot - this.a0Top) * var7);
            double var21 = Math.toRadians(this.a1Top + (this.a1Bot - this.a1Top) * var7);
            double var4 = var11 - (var19 + var21) * 0.5;

            while (var4 > 3.141592653589793) {
               var4 -= 6.283185307179586;
            }

            while (var4 < -3.141592653589793) {
               var4 += 6.283185307179586;
            }

            return Math.max(var18, (float)(Math.abs(var4) - (var21 - var19) * 0.5) * var9);
         }
      }

      Shape shape(int var1, int var2) {
         float var3 = Math.max(this.rTop, this.rBot) + this.amp + this.half + 0.2F;
         float var4 = Math.min(this.a0Top, this.a0Bot);
         float var5 = Math.max(this.a1Top, this.a1Bot);
         float var6 = 0.0F;
         float var7 = 0.0F;
         float var8 = 0.0F;
         float var9 = 0.0F;

         for (int var10 = 0; var10 <= 64; var10++) {
            double var11 = Math.toRadians(var4 + (var5 - var4) * var10 / 64.0F);
            var6 = Math.min(var6, (float)Math.sin(var11) * var3);
            var7 = Math.max(var7, (float)Math.sin(var11) * var3);
            var8 = Math.min(var8, (float)Math.cos(var11) * var3);
            var9 = Math.max(var9, (float)Math.cos(var11) * var3);
         }

         Shape var13 = Shapes.field(this::distance, var6 - 0.3F, this.yBot - 0.2F, var8 - 0.3F, var7 + 0.3F, this.yTop + 0.2F, var9 + 0.3F);
         return var13.paint(
            Paint.field(
               (var3x, var4x, var5x) -> {
                  float var6x = (float)Math.sqrt(var3x * var3x + var5x * var5x);
                  return var6x < this.radius(var3x, var4x, var5x)
                     ? var2
                     : Colors.mix(Colors.shade(var1, 0.8F), var1, Luna.clamp01((var4x - this.yBot) / (this.yTop - this.yBot) * 1.6F));
               }
            )
         );
      }
   }
}
