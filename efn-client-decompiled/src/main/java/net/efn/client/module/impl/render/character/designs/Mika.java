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
import net.efn.client.module.impl.render.character.kit.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Mika extends CharacterDesign {
   private static final int SKIN = -9532;
   private static final int SKIN_SHADE = -1331558;
   private static final int SKIN_LINE = -5740966;
   private static final int HAIR = -681403;
   private static final int HAIR_DARK = -3647457;
   private static final int HAIR_TIP = -15750;
   private static final int HAIR_LINE = -6142180;
   private static final int EAR_TIP = -4697060;
   private static final int EAR_PINK = -22088;
   private static final int TUFT = -5162;
   private static final int HOODIE = -4200487;
   private static final int HOODIE_SHADE = -7092297;
   private static final int RIB = -5710645;
   private static final int CUFF_IN = -9461870;
   private static final int HOODIE_LINE = -10774650;
   private static final int LINING = -744532;
   private static final int SKIRT = -12892555;
   private static final int SKIRT_DARK = -14275250;
   private static final int SKIRT_LINE = -14999232;
   private static final int TIGHTS = -12965330;
   private static final int TIGHTS_SHEEN = -9808288;
   private static final int TIGHTS_LINE = -15068908;
   private static final int SHOE = -542005;
   private static final int SOLE = -264207;
   private static final int MINT = -5839921;
   private static final int FISH = -8398652;
   private static final int FISH_LINE = -12811148;
   private static final int SHOE_LINE = -6659470;
   private static final int GOLD = -867761;
   private static final int TAIL_TIP = -3618;
   private static final int LASH = -13887470;
   private static final int BROW = -4691398;
   private static final float HX = 0.0F;
   private static final float HY = 27.95F;
   private static final float HZ = -0.15F;
   private static final float RX = 2.3F;
   private static final float RY = 2.3F;
   private static final float RZ = 2.35F;
   private static final float DROP = 0.4F;
   private static final float HS = 1.12F;
   private static final float NY = 24.9F;
   private static final float NZ = -0.3F;
   private static final float PPU = 55.652176F;
   private static final float TOP = 29.0F;
   private static final int PLEATS = 16;
   private static final float[][] FRINGE = new float[][]{
      {1.0F, -1.0F, 30.15F, -1.5F, 29.6F, -2.0F, 28.3F, -1.8F, 27.0F, 0.3F},
      {0.95F, -0.42F, 30.25F, -0.75F, 29.75F, -1.05F, 28.6F, -0.85F, 27.9F, -0.35F},
      {1.05F, -0.28F, 30.3F, 0.0F, 29.8F, 0.5F, 28.4F, 0.42F, 27.2F, 0.4F},
      {0.95F, 0.55F, 30.25F, 0.9F, 29.75F, 1.25F, 28.6F, 1.02F, 27.65F, -0.3F},
      {1.0F, 1.1F, 30.15F, 1.6F, 29.6F, 2.05F, 28.3F, 1.85F, 26.75F, 0.35F},
      {0.65F, -0.75F, 30.2F, -1.2F, 29.6F, -1.5F, 28.3F, -1.3F, 27.45F, 0.3F},
      {0.5F, 0.28F, 30.25F, 0.6F, 29.7F, 0.88F, 28.5F, 0.75F, 27.5F, -0.3F},
      {0.5F, -0.45F, 30.25F, -0.4F, 29.7F, -0.3F, 28.3F, -0.18F, 27.6F, 0.3F}
   };
   private static final float[][] CROWN = new float[][]{
      {-0.413F, 0.766F, 0.492F},
      {-0.992F, 0.087F, -0.087F},
      {-0.539F, -0.342F, -0.77F},
      {0.464F, -0.375F, -0.803F},
      {0.975F, 0.139F, -0.172F},
      {0.353F, 0.788F, 0.504F}
   };

   public Mika() {
      super("mika", "Mika");
   }

   @Override
   protected void define(Rig var1) {
      var1.scale(1.0F).outline(-12967400, 0.12F).occlusion(0.3F);
      body(var1);
      head(var1);
      hair(var1);
      ears(var1);
      arms(var1);
      legs(var1);
      tail(var1);
   }

   private static Shape toHead(Shape var0) {
      return var0.move(0.0F, -0.4F, 0.0F).scale(1.12F, 0.0F, 24.9F, -0.3F);
   }

   private static float wx(float var0) {
      return var0 * 1.12F;
   }

   private static float wy(float var0) {
      return 24.9F + (var0 - 0.4F - 24.9F) * 1.12F;
   }

   private static float wz(float var0) {
      return -0.3F + (var0 - -0.3F) * 1.12F;
   }

   private static float u(float var0) {
      return 128.0F + var0 * 55.652176F;
   }

   private static float v(float var0) {
      return (29.0F - var0) * 55.652176F;
   }

   private static float capZ(float var0, float var1, float var2) {
      float var3 = var0 / (2.72F + var2);
      float var4 = (var1 - 28.25F) / (2.8F + var2);
      return -0.28F + (2.85F + var2) * (float)Math.sqrt(Math.max(0.02F, 1.0F - var3 * var3 - var4 * var4));
   }

   private static float[] onCap(float var0, float var1, float var2, float var3) {
      float var4 = (float)Math.sqrt(var0 * var0 + var1 * var1 + var2 * var2);
      float var9;
      float var5 = (var9 = var0 / var4) / (2.72F + var3);
      float var10;
      float var6 = (var10 = var1 / var4) / (2.8F + var3);
      float var11;
      float var7 = (var11 = var2 / var4) / (2.85F + var3);
      float var8 = 1.0F / (float)Math.sqrt(var5 * var5 + var6 * var6 + var7 * var7);
      return new float[]{var9 * var8, 28.25F + var10 * var8, -0.28F + var11 * var8};
   }

   private static Shape crownClump(float[] var0, float var1) {
      float var2 = 0.15F;
      float var3 = 0.93F;
      float var4 = -0.35F;
      float[] var5 = new float[16];
      float[] var6 = new float[]{0.0F, 0.38F, 0.72F, 1.0F};
      float[] var7 = new float[]{0.2F, 0.85F * var1, 0.8F * var1, 0.3F};
      float[] var8 = new float[]{-0.12F, -0.07F, -0.07F, -0.14F};

      for (int var9 = 0; var9 < 4; var9++) {
         float[] var10 = onCap(var2 + (var0[0] - var2) * var6[var9], var3 + (var0[1] - var3) * var6[var9], var4 + (var0[2] - var4) * var6[var9], var8[var9]);
         var5[var9 * 4] = var10[0];
         var5[var9 * 4 + 1] = var10[1];
         var5[var9 * 4 + 2] = var10[2];
         var5[var9 * 4 + 3] = var7[var9];
      }

      return Shapes.tube(var5).flattenFrom(3.2F, 0.0F, 28.25F, -0.28F);
   }

   private static float surf(float var0, float var1, float var2) {
      float var3 = var0 / (2.3F + var2);
      float var4 = (var1 - 27.95F) / (2.3F + var2);
      return -0.15F + (2.35F + var2) * (float)Math.sqrt(Math.max(0.02F, 1.0F - var3 * var3 - var4 * var4));
   }

   private static void head(Rig var0) {
      Shape var1 = Shapes.ellipsoid(0.0F, 27.95F, -0.15F, 2.3F, 2.3F, 2.35F);
      Shape var2 = Shapes.ellipsoid(0.0F, 26.72F, 0.35F, 1.93F, 1.3F, 1.7F);
      Shape var3 = Shapes.cone(0.0F, 26.2F, 0.45F, 1.2F, 0.0F, 25.25F, 0.98F, 0.3F);
      Shape var4 = Shapes.union(Shapes.box(0.0F, 29.0F, -1.8F, 3.0F, 2.3F, 1.5F), Shapes.box(0.0F, 30.3F, 0.5F, 3.0F, 1.3F, 3.0F));
      Shape var5 = Shapes.subtract(Shapes.blend(0.7F, var1, var2, var3), var4).color(-9532);
      var0.part(Bone.HEAD).pivot(0.0F, 24.9F, -0.3F).shape(toHead(var5)).detail(1.5F).flatLight(1.0F).occlusion(0.2F).outline(-5740966, 0.1F).decal(face());
      Paint var6 = Paint.vertical(-9532, 23.7F, -1331558, 24.3F).flatLight(1.0F).occlusion(0.25F);
      var0.extra("neck", Bone.BODY).shape(Shapes.capsule(0.0F, 23.1F, -0.28F, 0.0F, 25.2F, -0.2F, 0.62F).paint(var6)).outline(-5740966, 0.08F);
   }

   private static void hair(Rig var0) {
      Paint var1 = Paint.field(Mika::hairColor);
      Shape var2 = Shapes.ellipsoid(0.0F, 28.25F, -0.28F, 2.72F, 2.8F, 2.85F)
         .intersect(Shapes.plane(0.0F, 27.4F, 1.2F, 0.0F, -0.6F, 1.0F))
         .intersect(Shapes.above(26.4F));
      Shape[] var3 = new Shape[CROWN.length];

      for (int var4 = 0; var4 < CROWN.length; var4++) {
         var3[var4] = crownClump(CROWN[var4], var4 % 2 == 0 ? 1.0F : 0.9F);
      }

      Shape var16 = Shapes.blend(0.3F, var2, Shapes.union(var3));
      Part var5 = var0.extra("hair", Bone.HEAD)
         .shape(toHead(Shapes.union(var16, backLock(180.0F, 24.95F, 1.05F, 0.25F)).paint(var1)))
         .detail(0.9F)
         .resolution(0.95F)
         .flatLight(0.5F)
         .occlusion(0.3F)
         .outline(-6142180, 0.09F);
      float[][] var6 = new float[][]{
         {-54.0F, 25.95F, 0.9F, 0.3F},
         {-75.0F, 25.45F, 1.0F, -0.3F},
         {-97.0F, 25.8F, 1.05F, 0.25F},
         {-119.0F, 25.3F, 1.05F, -0.2F},
         {-140.0F, 25.5F, 1.05F, 0.3F},
         {-161.0F, 25.1F, 1.0F, -0.3F}
      };
      ArrayList var7 = new ArrayList();

      for (float[] var11 : var6) {
         var7.add(backLock(var11[0], var11[1], var11[2], var11[3]));
      }

      var7.add(topLock(-84.0F, 26.25F));
      var7.add(topLock(-128.0F, 26.5F));
      Part var17 = var0.extra("hair_r", Bone.HEAD)
         .pivot(0.0F, wy(29.8F), wz(-0.4F))
         .shape(toHead(Shapes.union(var7.toArray(new Shape[0])).paint(var1)))
         .detail(1.0F)
         .resolution(0.85F)
         .flatLight(0.5F)
         .occlusion(0.35F)
         .outline(-6142180, 0.09F)
         .motion(Motion.hair().idle(1.2F, 0.0F, 1.0F, 0.35F).walk(2.5F, 0.0F, 2.0F, 2.0F).drag(6.0F).hang(0.2F).turn(5.0F, 0.0F).spring(60.0F, 8.0F).bend(1.6F));
      var0.extra("hair_l", Bone.HEAD).mirrorOf(var17);
      Shape[] var18 = new Shape[FRINGE.length];

      for (int var19 = 0; var19 < FRINGE.length; var19++) {
         var18[var19] = fringeLock(FRINGE[var19]);
      }

      Shape var20 = Shapes.ellipsoid(0.0F, 28.25F, -0.28F, 2.56F, 2.64F, 2.69F).intersect(Shapes.above(29.3F));
      Shape var21 = Shapes.subtract(Shapes.union(var18), var20);
      Part var12 = var0.extra("fringe", Bone.HEAD)
         .shape(toHead(var21.paint(Paint.vertical(-15750, 26.9F, -681403, 29.2F))))
         .detail(1.3F)
         .resolution(0.5F)
         .flatLight(0.5F)
         .occlusion(0.25F)
         .outline(-6142180, 0.08F);
      shine(var5, var12);
      clip(var0);
      Strand var13 = Shapes.tube(
            -1.95F,
            29.5F,
            0.85F,
            0.42F,
            -2.36F,
            28.4F,
            1.05F,
            0.56F,
            -2.42F,
            27.1F,
            1.08F,
            0.6F,
            -2.3F,
            25.9F,
            1.02F,
            0.5F,
            -2.26F,
            25.0F,
            0.98F,
            0.34F,
            -2.5F,
            24.45F,
            0.95F,
            0.05F
         )
         .flatten(2.3F, -0.55F, 0.0F, 0.83F);
      Part var14 = var0.extra("lock_r", Bone.HEAD)
         .pivot(wx(-1.95F), wy(29.4F), wz(0.85F))
         .shape(toHead(pointed(var13, -2.5F, 24.45F, 0.95F, 0.4F, 1.0F, 0.3F).paint(var1)))
         .detail(0.8F)
         .resolution(0.55F)
         .flatLight(0.5F)
         .occlusion(0.35F)
         .outline(-6142180, 0.09F)
         .motion(Motion.hair().drag(10.0F).hang(0.35F));
      var0.extra("lock_l", Bone.HEAD).mirrorOf(var14);
      Strand var15 = Shapes.tube(
            0.05F,
            30.8F,
            0.1F,
            0.24F,
            0.03F,
            31.45F,
            0.2F,
            0.26F,
            0.3F,
            32.05F,
            0.32F,
            0.22F,
            0.8F,
            32.38F,
            0.42F,
            0.17F,
            1.34F,
            32.35F,
            0.5F,
            0.11F,
            1.72F,
            32.02F,
            0.55F,
            0.05F
         )
         .flatten(1.6F, 0.0F, 0.0F, 1.0F);
      var0.extra("ahoge", Bone.HEAD)
         .pivot(wx(0.05F), wy(30.85F), wz(0.1F))
         .shape(toHead(var15.paint(Paint.vertical(-681403, 31.2F, -15750, 32.1F))))
         .resolution(0.5F)
         .detail(0.9F)
         .flatLight(0.5F)
         .occlusion(0.3F)
         .outline(-6142180, 0.08F)
         .motion(Motion.hair().idle(4.0F, 0.0F, 6.0F, 0.5F).walk(8.0F, 0.0F, 8.0F, 2.0F).drag(6.0F).hang(0.3F).spring(70.0F, 5.0F).bend(1.2F));
   }

   private static int hairColor(float var0, float var1, float var2) {
      int var3 = Colors.mix(-15750, -681403, smooth(25.0F, 27.6F, var1));
      float var4 = (float)Math.sqrt(var0 * var0 + (var2 + 0.25F) * (var2 + 0.25F));
      float var5 = (1.0F - smooth(1.9F, 2.55F, var4)) * (1.0F - smooth(26.4F, 27.6F, var1));
      return Colors.mix(var3, -3647457, var5 * 0.75F);
   }

   private static Shape fringeLock(float[] var0) {
      float var1 = var0[0] < 0.7F ? 0.52F : (var0[9] > 0.0F ? 0.4F : 0.28F);
      Strand var2 = Shapes.strand(
            var0[0],
            0.05F,
            var0[1],
            var0[2],
            capZ(var0[1], var0[2], -0.3F),
            var0[3],
            var0[4],
            capZ(var0[3], var0[4], 0.1F),
            var0[5],
            var0[6],
            surf(var0[5], var0[6], var1),
            var0[7],
            var0[8],
            surf(var0[7], var0[8], 0.12F)
         )
         .flattenFrom(2.8F, 0.0F, 27.95F, -0.15F);
      float var3 = var0[5] - var0[7];
      float var4 = var0[6] - var0[8];
      float var5 = (float)Math.sqrt(var3 * var3 + var4 * var4);
      float var6 = 0.9F * (1.0F + var0[9]);
      float var7 = 0.9F * (1.0F - var0[9]);
      float var8 = 0.34F;
      float var10;
      float var11;
      Shape2D var9 = Shape2D.polygon(
         var0[7],
         var0[8],
         var0[7] + (var10 = var3 / var5) * var6 - (var11 = var4 / var5) * var8,
         var0[8] + var11 * var6 + var10 * var8,
         var0[7] + var10 * 8.0F - var11 * 5.0F,
         var0[8] + var11 * 8.0F + var10 * 5.0F,
         var0[7] + var10 * 8.0F + var11 * 5.0F,
         var0[8] + var11 * 8.0F - var10 * 5.0F,
         var0[7] + var10 * var7 + var11 * var8,
         var0[8] + var11 * var7 - var10 * var8
      );
      return var2.intersect(Shapes.extrude(var9, 6.0F, 0.0F));
   }

   private static void clip(Rig var0) {
      Shape2D var1 = Shape2D.blend(0.08F, Shape2D.ellipse(-0.1F, 0.0F, 0.5F, 0.32F), Shape2D.polygon(0.3F, 0.0F, 0.86F, 0.36F, 0.66F, 0.0F, 0.86F, -0.36F));
      Shape var2 = Shapes.extrude(var1, 0.15F, 0.11F).scale(1.3F).paint(Paint.gradient(-3869978, 0.0F, 0.35F, 0.0F, -8398652, 0.0F, -0.35F, 0.0F));
      float var3 = 1.62F;
      float var4 = 28.7F;
      float var5 = 0.75F;
      float var6 = surf(var3, var4, var5);
      float var7 = 2.3F + var5;
      float var8 = 2.3F + var5;
      float var9 = 2.35F + var5;
      float var10 = var3 / (var7 * var7);
      float var11 = (var4 - 27.95F) / (var8 * var8);
      float var12 = (var6 - -0.15F) / (var9 * var9);
      float var13 = (float)Math.sqrt(var10 * var10 + var11 * var11 + var12 * var12);
      float var14 = (float)Math.toDegrees(Math.atan2(var10, var12));
      float var15 = (float)(-Math.toDegrees(Math.asin(var11 / var13)));
      Shape var16 = var2.rotate(0.0F, 0.0F, 35.0F).rotate(var15, var14, 0.0F).move(var3, var4, var6);
      Vec3 var17 = new Vec3(-0.5F, 0.08F, 0.2F).rotate(0.0F, 0.0F, 35.0F).rotate(var15, var14, 0.0F).add(var3, var4, var6);
      Vec3 var18 = new Vec3(0.0F, 0.0F, 1.0F).rotate(0.0F, 0.0F, 35.0F).rotate(var15, var14, 0.0F);
      Canvas var19 = new Canvas(32, 32);
      var19.fill(Shape2D.circle(16.0F, 16.0F, 7.0F), -14796236);
      var19.fill(Shape2D.circle(13.5F, 13.5F, 2.2F), -855638017);
      Decal var20 = Decal.project(
            var19, wx(var17.x()), wy(var17.y()), wz(var17.z()), var18.x(), var18.y(), var18.z(), 0.0F, 1.0F, 0.0F, 0.33600003F, 0.33600003F
         )
         .depth(0.25F, 0.25F);
      var0.extra("clip", Bone.HEAD).shape(toHead(var16)).resolution(0.4F).detail(0.5F).flatLight(0.4F).outline(-12811148, 0.1F).decal(var20);
   }

   private static void shine(Part var0, Part var1) {
      Canvas var3 = new Canvas(96, 24);
      var3.fill(Shape2D.union(Shape2D.taper(3.0F, 12.0F, 1.0F, 48.0F, 12.0F, 17.0F), Shape2D.taper(48.0F, 12.0F, 17.0F, 93.0F, 12.0F, 1.0F)), -419442022, 1.5F);

      for (float[] var7 : new float[][]{
         {-52.0F, 0.72F, 0.12F, 6.0F}, {-24.0F, 0.9F, -0.06F, -4.0F}, {4.0F, 0.66F, 0.08F, 5.0F}, {27.0F, 0.82F, -0.08F, -6.0F}, {57.0F, 0.7F, 0.1F, 3.0F}
      }) {
         double var8 = Math.toRadians(var7[0]);
         float var10 = (float)Math.sin(var8);
         float var11 = (float)Math.cos(var8);
         float[] var12 = onCap(var10, 0.36F + 0.3F * (1.0F - var11) + var7[2], var11, 0.28F);
         float var13 = var12[0];
         float var14 = var12[1] - 28.25F;
         float var15 = var12[2] + 0.28F;
         Vec3 var16 = new Vec3(0.0F, 1.0F, 0.0F).rotate(0.0F, 0.0F, var7[3]);
         Decal var17 = Decal.project(
               var3, wx(var12[0]), wy(var12[1]), wz(var12[2]), var13, var14 * 0.6F, var15, var16.x(), var16.y(), var16.z(), var7[1] * 1.12F, 0.2464F
            )
            .depth(0.896F, 0.896F)
            .maxAngle(80.0F)
            .flatLight(0.8F)
            .occlusion(0.0F);
         var0.decal(var17);
         var1.decal(var17);
      }
   }

   private static Shape pointed(Strand var0, float var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = -0.25F;
      float var8 = (float)Math.toDegrees(Math.atan2(var1, var3 - var7));
      float var9 = var5 * (1.0F + var6);
      float var10 = var5 * (1.0F - var6);
      Shape2D var11 = Shape2D.polygon(-5.0F, var2 + 8.0F, 5.0F, var2 + 8.0F, var4, var2 + var9, 0.0F, var2, -var4, var2 + var10);
      return var0.intersect(Shapes.extrude(var11, 5.0F, 0.0F).rotate(0.0F, var8, 0.0F, 0.0F, 0.0F, var7));
   }

   private static Shape backLock(float var0, float var1, float var2, float var3) {
      double var4 = Math.toRadians(var0);
      float var6 = (float)Math.sin(var4);
      float var7 = (float)Math.cos(var4);
      float var8 = Math.max(0.0F, Math.min(1.0F, (Math.abs(var0) - 100.0F) / 70.0F));
      float[] var9 = new float[]{30.1F, 29.1F, 27.9F, 26.9F, (26.9F + var1) * 0.5F - 0.1F, var1};
      float[] var10 = new float[]{2.04F, 2.59F, 2.7F, 2.52F, 2.42F, 2.32F};
      float[] var11 = new float[]{2.14F, 2.72F, 2.83F, 2.62F, 2.48F, 2.35F};
      float[] var12 = new float[]{-0.34F, -0.06F, 0.04F, 0.04F, 0.02F, -0.06F - 0.24F * var8};
      float[] var13 = new float[]{0.7F, 1.05F, 1.05F, 0.95F, 0.64F, 0.06F};
      float[] var14 = new float[24];

      for (int var15 = 0; var15 < 6; var15++) {
         var14[var15 * 4] = var6 * (var10[var15] + var12[var15]);
         var14[var15 * 4 + 1] = var9[var15];
         var14[var15 * 4 + 2] = -0.28F + var7 * (var11[var15] + var12[var15]);
         var14[var15 * 4 + 3] = var13[var15] * (var15 == 5 ? 1.0F : var2);
      }

      return pointed(Shapes.tube(var14).flattenFrom(2.1F, 0.0F, 27.95F, -0.15F), var14[20], var1, var14[22], 0.62F, 1.25F, var3);
   }

   private static Shape topLock(float var0, float var1) {
      double var2 = Math.toRadians(var0);
      float var4 = (float)Math.sin(var2);
      float var5 = (float)Math.cos(var2);
      float[] var6 = new float[]{2.2F, 2.8F, 2.98F, 3.0F};
      float[] var7 = new float[]{29.4F, 28.2F, 27.2F, var1};
      float[] var8 = new float[]{0.55F, 0.75F, 0.55F, 0.08F};
      float[] var9 = new float[16];

      for (int var10 = 0; var10 < 4; var10++) {
         var9[var10 * 4] = var4 * var6[var10];
         var9[var10 * 4 + 1] = var7[var10];
         var9[var10 * 4 + 2] = -0.25F + var5 * var6[var10] * 1.05F;
         var9[var10 * 4 + 3] = var8[var10];
      }

      return pointed(Shapes.tube(var9).flattenFrom(2.2F, 0.0F, 27.95F, -0.15F), var9[12], var1, var9[14], 0.6F, 1.15F, 0.2F);
   }

   private static void ears(Rig var0) {
      Shape var1 = Shapes.extrude(Shape2D.polygon(-1.1F, 0.0F, 1.1F, 0.0F, 0.08F, 2.55F), 0.64F, 0.32F);
      Shape var2 = Shapes.extrude(Shape2D.polygon(0.6F, -0.3F, -0.62F, -0.3F, -0.15F, 2.7F, 0.2F, 2.7F), 1.3F, 0.2F).rotate(0.0F, 90.0F, 0.0F);
      Shape var3 = Shapes.intersect(var1, var2).paint(Paint.vertical(-681403, 1.45F, -4697060, 2.25F));
      Shape var4 = Shapes.extrude(Shape2D.polygon(-0.66F, 0.3F, 0.68F, 0.3F, 0.07F, 1.95F), 0.5F, 0.14F)
         .move(0.0F, 0.0F, 0.62F)
         .paint(Paint.of(-22088).flatLight(0.7F));
      Shape var5 = Shapes.union(
            Shapes.strand(0.14F, 0.05F, -0.34F, 0.28F, 0.25F, -0.46F, 0.6F, 0.44F, -0.38F, 0.9F, 0.5F),
            Shapes.strand(0.14F, 0.05F, 0.3F, 0.28F, 0.25F, 0.46F, 0.58F, 0.44F, 0.56F, 0.86F, 0.48F)
         )
         .paint(Paint.of(-5162).flatLight(0.7F));
      Shape var6 = Shapes.union(Shapes.subtract(0.1F, var3, var4), var5).scale(1.18F).rotate(-6.0F, -18.0F, 24.0F).move(-1.6F, 29.75F, -0.15F);
      Part var7 = var0.extra("ear_r", Bone.HEAD)
         .pivot(wx(-1.6F), wy(29.9F), wz(-0.15F))
         .shape(toHead(var6))
         .resolution(0.58F)
         .detail(0.9F)
         .flatLight(0.4F)
         .outline(-6142180, 0.09F)
         .motion(earMotion(0.0F, -1.0F));
      var0.extra("ear_l", Bone.HEAD).mirrorOf(var7).motion(earMotion(140.0F, 1.0F));
   }

   private static Motion earMotion(float var0, float var1) {
      return Motion.none().idle(2.5F, 0.0F, 5.0F * var1, 0.55F).idlePhase(var0).walk(3.0F, 0.0F, 3.0F * var1, 2.0F).spring(90.0F, 7.0F);
   }

   private static void body(Rig var0) {
      Shape var1 = Shapes.union(
         Shapes.cone(0.0F, 22.6F, -0.2F, 1.02F, -1.95F, 22.2F, -0.2F, 0.9F), Shapes.cone(0.0F, 22.6F, -0.2F, 1.02F, 1.95F, 22.2F, -0.2F, 0.9F)
      );
      Shape var2 = Shapes.ellipsoid(0.0F, 20.5F, -0.1F, 2.3F, 2.4F, 1.72F);
      Shape var3 = Shapes.ellipsoid(0.0F, 17.8F, -0.02F, 2.25F, 2.9F, 1.7F);
      Shape var4 = Shapes.ellipsoid(0.0F, 15.25F, -0.05F, 2.45F, 1.25F, 1.86F);
      Shape var5 = Shapes.blend(1.0F, var1, var2, var3, var4).paint(Paint.field(Mika::hoodie));
      Shape var6 = Shapes.intersect(
            Shapes.ellipsoid(0.0F, 16.3F, -0.02F, 2.6F, 2.9F, 2.02F),
            Shapes.extrude(Shape2D.polygon(-1.6F, 17.3F, 1.6F, 17.3F, 2.15F, 14.75F, -2.15F, 14.75F), 2.0F, 0.0F).move(0.0F, 0.0F, 2.0F)
         )
         .color(-4200487);
      Shape var7 = Shapes.extrude(Shape2D.ellipse(0.0F, 0.0F, 2.42F, 1.8F), 0.4F, 0.3F).rotate(90.0F, 0.0F, 0.0F).move(0.0F, 14.2F, -0.05F).color(-5710645);
      var0.part(Bone.BODY)
         .pivot(0.0F, 23.4F, -0.1F)
         .shape(Shapes.union(Shapes.blend(0.25F, var5, var6), var7))
         .resolution(1.15F)
         .detail(1.0F)
         .outline(-10774650, 0.12F)
         .occlusion(0.3F)
         .flatLight(0.2F)
         .decal(pocketDecal())
         .decal(pawDecal())
         .decal(hemRib());
      var0.extra("collar", Bone.BODY)
         .shape(rim().paint(Paint.field(Mika::rimColor)))
         .resolution(0.7F)
         .detail(0.8F)
         .occlusion(0.3F)
         .flatLight(0.2F)
         .outline(-10774650, 0.1F);
      Shape var8 = Shapes.blend(
         0.8F, Shapes.ellipsoid(0.0F, 21.7F, -1.85F, 2.2F, 2.2F, 0.85F), Shapes.cone(0.0F, 20.6F, -2.3F, 0.8F, 0.0F, 19.1F, -2.2F, 0.28F)
      );
      Shape var9 = Shapes.ellipsoid(0.0F, 0.0F, 0.0F, 1.55F, 0.35F, 0.85F).rotate(-26.6F, 0.0F, 0.0F).move(0.0F, 22.95F, -1.95F);
      Shape var10 = Shapes.subtract(0.15F, var8.intersect(Shapes.plane(0.0F, 22.7F, -2.2F, 0.0F, 1.0F, -0.5F)), var9).paint(Paint.field(Mika::hoodColor));
      var0.extra("hood", Bone.BODY).shape(var10).resolution(0.85F).detail(0.6F).occlusion(0.3F).flatLight(0.2F).outline(-10774650, 0.12F);
      var0.extra("skirt", Bone.BODY)
         .pivot(0.0F, 14.6F, 0.0F)
         .shape(skirt())
         .resolution(0.9F)
         .detail(0.8F)
         .occlusion(0.35F)
         .outline(-14999232, 0.12F)
         .decal(skirtStripe())
         .motion(Motion.none().walk(0.0F, 8.0F, 0.0F, 1.0F).walkPhase(-90.0F).drag(2.0F).spring(80.0F, 10.0F));
      Shape var11 = Shapes.tube(-0.32F, 23.5F, 1.45F, 0.13F, -0.38F, 22.6F, 1.75F, 0.13F, -0.44F, 21.3F, 1.95F, 0.13F, -0.48F, 20.1F, 2.0F, 0.12F)
         .color(-264984);
      Shape var12 = Shapes.sphere(-0.48F, 19.6F, 2.05F, 0.45F).paint(Paint.radial(-3920, -0.62F, 19.8F, 2.35F, 0.6F, -867761));
      Part var13 = var0.extra("cord_r", Bone.BODY)
         .pivot(-0.32F, 23.5F, 1.45F)
         .shape(Shapes.union(var11, var12))
         .resolution(0.5F)
         .detail(0.5F)
         .outline(-10774650, 0.08F)
         .decal(bellDecal(-0.48F))
         .motion(
            Motion.none()
               .rest(-3.0F, 0.0F, 0.0F)
               .idle(1.5F, 0.0F, 2.0F, 0.5F)
               .walk(5.0F, 0.0F, 6.0F, 2.0F)
               .drag(-4.0F)
               .hang(0.9F)
               .spring(60.0F, 6.0F)
               .bend(0.8F)
         );
      var0.extra("cord_l", Bone.BODY).mirrorOf(var13);
   }

   private static Shape rim() {
      return Shapes.tube(
            0.02F,
            23.64F,
            1.44F,
            0.36F,
            -0.5F,
            23.84F,
            1.3F,
            0.41F,
            -1.12F,
            24.08F,
            0.8F,
            0.43F,
            -1.42F,
            24.2F,
            -0.18F,
            0.43F,
            -1.1F,
            24.3F,
            -1.14F,
            0.43F,
            0.1F,
            24.34F,
            -1.68F,
            0.43F
         )
         .flattenFrom(1.25F, 0.0F, 24.1F, -0.25F)
         .mirrorX();
   }

   private static int hoodie(float var0, float var1, float var2) {
      float var3 = (float)Math.sqrt(var0 * var0 + (var2 + 0.2F) * (var2 + 0.2F));
      float var4 = smooth(22.4F, 23.3F, var1) * (1.0F - smooth(1.7F, 2.3F, var3)) * 0.6F;
      float var5 = (1.0F - smooth(14.6F, 15.3F, var1)) * 0.55F;
      return Colors.mix(-4200487, -7092297, Math.max(var4, var5));
   }

   private static int rimColor(float var0, float var1, float var2) {
      float var3 = (float)Math.sqrt(var0 * var0 + (var2 + 0.2F) * (var2 + 0.2F));
      float var4 = smooth(0.28F, 0.36F, var1 - 24.0F + 0.22F * var2);
      return Colors.mix(-4200487, -744532, Math.max(var4, 1.0F - smooth(1.12F, 1.38F, var3)));
   }

   private static int hoodColor(float var0, float var1, float var2) {
      float var3 = var0 / 2.2F;
      float var4 = (var1 - 21.7F) / 2.2F;
      float var5 = (var2 + 1.85F) / 0.85F;
      float var6 = (var1 - 22.7F - 0.5F * (var2 + 2.2F)) * 0.894F;
      return var6 > -0.5F && var3 * var3 + var4 * var4 + var5 * var5 < 0.8F ? -744532 : -4200487;
   }

   private static Shape skirt() {
      return Shapes.field(Mika::skirtDistance, -3.3F, 11.8F, -2.8F, 3.3F, 14.4F, 2.8F).paint(Paint.field(Mika::skirtColor));
   }

   private static float flare(float var0) {
      float var1 = Math.max(0.0F, Math.min(1.0F, (14.0F - var0) / 2.1F));
      return var1 * (2.0F - var1);
   }

   private static float pleat(float var0, float var1, float var2) {
      float var3 = 2.2F + 0.75F * var2;
      float var4 = 1.66F + 0.84F * var2;
      double var5 = Math.atan2(var0 / var3, var1 / var4) * 16.0 / 6.283185307179586;
      var5 -= Math.floor(var5);
      return (float)Math.abs(2.0 * var5 - 1.0);
   }

   private static float skirtDistance(float var0, float var1, float var2) {
      float var3 = flare(var1);
      float var4 = 2.2F + 0.75F * var3;
      float var5 = 1.66F + 0.84F * var3;
      float var6 = var0 / var4;
      float var7 = var2 / var5;
      float var8 = (float)Math.sqrt(var6 * var6 + var7 * var7);
      float var9 = (var8 - 1.0F) * Math.min(var4, var5) * 0.72F - 0.2F * var3 * (pleat(var0, var2, var3) - 0.5F);
      return Math.max(var9, Math.max(var1 - 14.3F, 11.9F - var1));
   }

   private static int skirtColor(float var0, float var1, float var2) {
      float var3 = flare(var1);
      return Colors.mix(-14275250, -12892555, 0.45F + 0.55F * (1.0F - var3 + var3 * pleat(var0, var2, var3)));
   }

   private static Decal skirtStripe() {
      Canvas var0 = new Canvas(512, 24);
      var0.fill(Shape2D.box(256.0F, 12.0F, 256.0F, 7.0F, 0.0F), -5839921);
      return Decal.wrap(var0, 0.0F, 12.35F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 360.0F, 0.16F).radius(2.2F, 3.2F);
   }

   private static void arms(Rig var0) {
      Shape var1 = Shapes.sphere(-2.72F, 21.95F, -0.15F, 0.95F);
      Shape var2 = Shapes.cone(-2.85F, 21.6F, -0.15F, 0.9F, -3.35F, 18.4F, -0.2F, 0.88F);
      Shape var3 = Shapes.sphere(-3.38F, 18.3F, -0.18F, 0.9F);
      Shape var4 = Shapes.cone(-3.38F, 18.2F, -0.18F, 0.9F, -3.72F, 15.0F, 0.45F, 1.12F);
      Shape var5 = Shapes.blend(0.4F, var1, var2, var3, var4).paint(Paint.field(Mika::sleeve));
      Shape var6 = Shapes.cylinder(-3.74F, 15.05F, 0.47F, -3.8F, 13.4F, 0.69F, 0.9F, 0.28F).paint(Paint.field(Mika::cuff));
      Shape var7 = Shapes.blend(0.15F, var5, var6);
      Part var8 = var0.part(Bone.RIGHT_ARM)
         .pivot(-2.72F, 21.95F, -0.15F)
         .grip(-3.8F, 12.9F, 0.9F)
         .shape(var7)
         .detail(0.9F)
         .resolution(1.05F)
         .outline(-10774650, 0.12F)
         .occlusion(0.3F)
         .flatLight(0.2F)
         .swing(0.65F, 40.0F)
         .joint(-3.38F, 18.3F, -0.18F, 0.7F)
         .decal(cuffRib());
      var0.part(Bone.LEFT_ARM).mirrorOf(var8);
      Part var9 = var0.extra("hand_r", Bone.RIGHT_ARM).shape(hand()).resolution(0.4F).detail(2.6F).flatLight(1.0F).occlusion(0.25F).outline(-5740966, 0.08F);
      var0.extra("hand_l", Bone.LEFT_ARM).mirrorOf(var9);
   }

   private static int sleeve(float var0, float var1, float var2) {
      float var3 = Math.max(0.0F, Math.min(1.0F, (21.9F - var1) / 6.9F));
      float var4 = smooth(0.25F, 0.85F, var0 + 2.8F + 0.94F * var3) * 0.5F;
      float var5 = (1.0F - smooth(0.2F, 0.55F, Math.abs(var1 - 18.25F))) * smooth(0.2F, 0.7F, var2) * 0.45F;
      return Colors.mix(-4200487, -7092297, Math.max(var4, var5));
   }

   private static int cuff(float var0, float var1, float var2) {
      float var3 = -0.06F;
      float var4 = -1.65F;
      float var5 = 0.22F;
      float var6 = (float)Math.sqrt(var3 * var3 + var4 * var4 + var5 * var5);
      float var7 = var0 + 3.8F;
      float var8 = var1 - 13.4F;
      float var9 = var2 - 0.69F;
      float var10 = -(var7 * var3 + var8 * var4 + var9 * var5) / var6;
      float var11 = var7 + var10 * var3 / var6;
      float var12 = var8 + var10 * var4 / var6;
      float var13 = var9 + var10 * var5 / var6;
      float var14 = (float)Math.sqrt(var11 * var11 + var12 * var12 + var13 * var13);
      return Colors.mix(-5710645, -9461870, (1.0F - smooth(0.04F, 0.12F, var10)) * (1.0F - smooth(0.55F, 0.68F, var14)));
   }

   private static Shape hand() {
      float[][] var0 = new float[][]{{-3.47F, 0.86F, 13.06F}, {-3.79F, 0.95F, 12.97F}, {-4.1F, 0.86F, 13.04F}};
      Shape[] var1 = new Shape[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = Shapes.capsule(var0[var2][0], 13.5F, var0[var2][1] - 0.12F, var0[var2][0], var0[var2][2], var0[var2][1], 0.16F);
      }

      Shape var3 = Shapes.capsule(-3.36F, 13.48F, 0.64F, -3.3F, 13.2F, 0.76F, 0.15F);
      return Shapes.union(Shapes.union(var1), var3).paint(Paint.of(-9532));
   }

   private static void legs(Rig var0) {
      Shape var1 = Shapes.sphere(-1.12F, 13.2F, -0.05F, 1.0F);
      Shape var2 = Shapes.cone(-1.18F, 12.8F, 0.0F, 1.13F, -1.1F, 7.9F, 0.1F, 0.88F);
      Shape var3 = Shapes.sphere(-1.08F, 7.45F, 0.12F, 0.82F);
      Shape var4 = Shapes.sphere(-1.07F, 7.5F, 0.48F, 0.5F);
      Strand var5 = Shapes.tube(-1.08F, 7.2F, 0.1F, 0.84F, -1.1F, 5.6F, -0.15F, 0.93F, -1.13F, 3.6F, -0.02F, 0.64F, -1.15F, 2.2F, 0.0F, 0.52F);
      Shape var6 = Shapes.blend(0.45F, var1, var2, var3, var4, var5).paint(Paint.field(Mika::tights));
      Part var7 = var0.part(Bone.RIGHT_LEG)
         .pivot(-1.2F, 13.7F, 0.0F)
         .shape(var6)
         .detail(0.85F)
         .resolution(1.1F)
         .outline(-15068908, 0.12F)
         .occlusion(0.35F)
         .swing(0.55F, 30.0F)
         .joint(-1.08F, 7.45F, 0.12F, 0.8F);
      var0.part(Bone.LEFT_LEG).mirrorOf(var7);
      Part var8 = var0.extra("shoe_r", Bone.RIGHT_LEG)
         .shape(sneaker().rotate(0.0F, -7.0F, 0.0F, -1.2F, 0.0F, 0.2F))
         .resolution(0.75F)
         .detail(1.0F)
         .outline(-6659470, 0.12F)
         .decal(laces());
      var0.extra("shoe_l", Bone.LEFT_LEG).mirrorOf(var8);
   }

   private static int tights(float var0, float var1, float var2) {
      double var3 = Math.atan2(-(var0 + 1.1F), var2 - 0.02F) - 0.35;
      float var5 = smooth(0.6F, 0.95F, (float)Math.cos(var3)) * smooth(2.4F, 4.5F, var1) * 0.5F;
      float var6 = (float)Math.sqrt((var1 - 7.5F) * (var1 - 7.5F) + (var0 + 1.07F) * (var0 + 1.07F) * 1.4F);
      float var7 = (1.0F - smooth(0.3F, 0.8F, var6)) * smooth(0.2F, 0.7F, var2) * 0.55F;
      return Colors.mix(-12965330, -9808288, Math.max(var5, var7));
   }

   private static Shape sneaker() {
      Shape var0 = Shapes.blend(0.25F, Shapes.box(-1.2F, 0.3F, 0.5F, 0.9F, 0.3F, 1.84F, 0.26F), Shapes.ellipsoid(-1.2F, 0.36F, 2.05F, 0.82F, 0.34F, 0.5F))
         .intersect(Shapes.above(0.0F))
         .paint(Paint.of(-264207).band(-1011800, 0.0F, 0.46F, 0.0F, 0.0F, 1.0F, 0.0F, 0.07F, 0.05F));
      Shape var1 = Shapes.ellipsoid(-1.2F, 0.9F, 1.15F, 0.74F, 0.55F, 1.12F);
      Shape var2 = Shapes.ellipsoid(-1.2F, 1.28F, -0.48F, 0.75F, 0.95F, 0.84F);
      Shape var3 = Shapes.blend(0.45F, var1, var2).color(-542005);
      Shape var4 = Shapes.ellipsoid(-1.2F, 0.72F, 1.86F, 0.68F, 0.36F, 0.5F).color(-264207);
      Shape var5 = Shapes.torus(-1.2F, 2.1F, -0.4F, 0.6F, 0.26F).color(-5839921);
      Shape var6 = Shapes.box(-1.2F, 1.75F, 0.35F, 0.42F, 0.42F, 0.3F, 0.18F).rotate(-25.0F, 0.0F, 0.0F, -1.2F, 1.75F, 0.35F).color(-207907);
      Shape var7 = Shapes.capsule(-1.2F, 1.8F, -1.2F, -1.2F, 2.45F, -1.02F, 0.17F).color(-5839921);
      return Shapes.union(var0, Shapes.blend(0.12F, var3, var4), var5, var6, var7);
   }

   private static void tail(Rig var0) {
      float[] var1 = new float[]{
         0.0F,
         13.95F,
         -1.45F,
         0.58F,
         0.15F,
         13.35F,
         -2.7F,
         0.76F,
         0.6F,
         13.2F,
         -4.1F,
         0.92F,
         1.6F,
         14.0F,
         -5.15F,
         1.06F,
         2.6F,
         15.9F,
         -5.5F,
         1.15F,
         3.2F,
         18.1F,
         -5.15F,
         1.15F,
         3.35F,
         19.9F,
         -4.35F,
         1.04F,
         3.0F,
         21.0F,
         -3.45F,
         0.72F,
         2.55F,
         21.45F,
         -2.85F,
         0.26F
      };
      Mika.Curve var2 = new Mika.Curve(var1);
      Shape var3 = Shapes.union(
         Shapes.strand(0.24F, 0.04F, 2.68F, 21.32F, -3.02F, 2.36F, 21.62F, -2.68F, 1.96F, 21.62F, -2.62F),
         Shapes.strand(0.24F, 0.04F, 2.68F, 21.32F, -3.02F, 2.46F, 21.6F, -2.62F, 2.42F, 21.62F, -2.2F),
         Shapes.strand(0.24F, 0.04F, 2.68F, 21.32F, -3.02F, 2.44F, 21.7F, -2.72F, 2.4F, 22.12F, -2.62F)
      );
      Part var4 = var0.extra("tail", Bone.BODY)
         .pivot(0.0F, 13.95F, -1.45F)
         .shape(Shapes.union(Shapes.tube(var1), var3).paint(Paint.field(var2::tailColor)))
         .detail(1.0F)
         .resolution(1.1F)
         .flatLight(0.3F)
         .outline(-6142180, 0.1F)
         .motion(Motion.tail().idle(3.0F, 14.0F, 4.0F, 0.4F).walk(4.0F, 12.0F, 3.0F, 1.0F).spring(30.0F, 4.0F).bend(1.4F));
      Canvas var5 = new Canvas(128, 96);
      var5.fill(Shape2D.union(Shape2D.line(10.0F, 80.0F, 64.0F, 26.0F, 24.0F), Shape2D.line(64.0F, 26.0F, 118.0F, 80.0F, 24.0F)), -1999082968, 6.0F);

      for (float var9 : new float[]{0.3F, 0.43F, 0.56F}) {
         var4.decal(var2.chevron(var5, var9));
      }
   }

   private static float smooth(float var0, float var1, float var2) {
      float var3 = Math.max(0.0F, Math.min(1.0F, (var2 - var0) / (var1 - var0)));
      return var3 * var3 * (3.0F - 2.0F * var3);
   }

   private static Decal face() {
      Canvas var0 = new Canvas(256, 200);
      float var1 = 13.356522F;
      Shape2D[] var2 = new Shape2D[FRINGE.length];

      for (int var3 = 0; var3 < FRINGE.length; var3++) {
         float[] var4 = FRINGE[var3];
         float var5 = var4[7] + (var4[5] - var4[7]) * 0.12F;
         float var6 = var4[8] + (var4[6] - var4[8]) * 0.12F;
         var2[var3] = Shape2D.path(var4[0] * 1.7F * 55.652176F, 9.0F, u(var4[3]), v(var4[4]) + var1, u(var4[5]), v(var4[6]) + var1, u(var5), v(var6) + var1);
      }

      var0.fill(Shape2D.union(var2), -1713793398, 3.0F);

      for (byte var7 = -1; var7 <= 1; var7 += 2) {
         eye(var0, 128.0F + var7 * 53.0F, 114.0F, var7);
      }

      var0.fill(Shape2D.curve(125.5F, 146.5F, 128.5F, 150.5F, 131.5F, 149.0F, 2.2F, 1.4F), -1713795968);
      var0.fill(Shape2D.path(3.0F, 3.0F, 117.0F, 177.0F, 122.5F, 182.5F, 128.0F, 178.5F, 133.5F, 182.5F, 139.0F, 177.0F), -7455954);
      return Decal.front(var0, 0.0F, wy(27.203125F), wz(2.1F), 5.152F, 4.0249996F).depth(1.4F, 1.4F).maxAngle(72.0F).flatLight(1.0F).occlusion(0.3F);
   }

   private static void eye(Canvas var0, float var1, float var2, int var3) {
      Shape2D var4 = Shape2D.ellipse(var1, var2 + 10.0F, 31.0F, 40.0F).rotate(var3 * 12.0F, var1, var2 + 10.0F);
      Shape2D var5 = Shape2D.ellipse(var1, var2 + 2.0F, 26.0F, 31.0F).intersect(var4);
      var0.fill(var5, Paint2D.linear(var1, var2 - 28.0F, var1, var2 + 4.0F, -2571046, -776));
      Shape2D var6 = Shape2D.ellipse(var1 + var3, var2 + 5.0F, 21.0F, 27.0F);
      var0.fill(var6.intersect(var5), Paint2D.linear(var1, var2 - 22.0F, var1, var2 + 32.0F, -11920890, -5748206, -1535446, -11414));
      var0.fill(Shape2D.ellipse(var1 + var3, var2 + 6.0F, 9.0F, 21.0F).intersect(var5), -1711287446, 4.0F);
      var0.fill(var6.outline(2.5F).intersect(var5), -1338371580);
      float var7 = var1 + var3;
      float var8 = var2 + 6.0F;
      var0.fill(Shape2D.circle(var7 - 42.2F, var8, 46.7F).intersect(Shape2D.circle(var7 + 42.2F, var8, 46.7F)), -14414846);
      var0.fill(Shape2D.ellipse(var1 - 8.0F, var2 - 8.0F, 7.5F, 9.0F).rotate(-20.0F, var1 - 8.0F, var2 - 8.0F), -1);
      var0.fill(Shape2D.circle(var1 + 9.0F, var2 + 17.0F, 3.5F), -419430401);
      var0.fill(Shape2D.curve(var1 - var3 * 28.0F, var2 - 8.0F, var1 - var3 * 2.0F, var2 - 42.0F, var1 + var3 * 30.0F, var2 - 24.0F, 4.0F, 9.5F), -13887470);
      var0.fill(Shape2D.taper(var1 + var3 * 24.0F, var2 - 24.0F, 7.0F, var1 + var3 * 40.0F, var2 - 36.0F, 1.5F), -13887470);
      var0.fill(Shape2D.curve(var1 + var3 * 8.0F, var2 + 34.0F, var1 + var3 * 21.0F, var2 + 31.0F, var1 + var3 * 26.0F, var2 + 17.0F, 1.2F, 3.0F), -866506210);
      var0.fill(Shape2D.curve(var1 - var3 * 17.0F, var2 - 38.0F, var1 + var3 * 2.0F, var2 - 48.0F, var1 + var3 * 24.0F, var2 - 38.0F, 1.5F, 2.2F), 1721786442);
      var0.fill(Shape2D.curve(var1 - var3 * 16.0F, var2 - 50.0F, var1 + var3 * 2.0F, var2 - 58.0F, var1 + var3 * 22.0F, var2 - 53.0F, 3.0F, 2.0F), -4691398);
      float var9 = var1 + var3 * 10.0F;
      float var10 = var2 + 42.0F;
      var0.fill(Shape2D.ellipse(var9, var10, 20.0F, 9.0F), 1627357068, 9.0F);

      for (int var11 = 0; var11 < 3; var11++) {
         var0.fill(Shape2D.line(var9 - 8.0F + var11 * 7.0F, var10 - 4.0F, var9 - 12.0F + var11 * 7.0F, var10 + 4.0F, 1.6F), 1894277230);
      }
   }

   private static Decal pocketDecal() {
      Canvas var1 = new Canvas(240, 144);
      int var2 = -1722777742;

      for (int var0 = 0; var0 < 18; var0++) {
         var1.fill(Shape2D.box(43.5F + var0 * 9.0F, 16.5F, 2.6F, 0.75F, 0.7F), var2);
      }

      for (int var6 = 0; var6 < 23; var6++) {
         var1.fill(Shape2D.box(21.0F + var6 * 9.0F, 129.0F, 2.6F, 0.75F, 0.7F), var2);
      }

      for (byte var3 = -1; var3 <= 1; var3 += 2) {
         float var4 = 120.0F + var3 * 78.0F;
         float var5 = 120.0F + var3 * 102.75F;
         var1.fill(Shape2D.taper(var4, 13.5F, 6.0F, var5, 132.0F, 3.75F), -1872719998);
         var1.fill(Shape2D.taper(var4 + var3 * 2.25F, 13.5F, 2.25F, var5 + var3 * 2.25F, 132.0F, 1.5F), -1605733274);
         var1.fill(Shape2D.taper(var4 - var3 * 4.5F, 16.5F, 1.9F, var5 - var3 * 4.5F, 129.0F, 1.5F), 1627389951);
      }

      return Decal.front(var1, 0.0F, 16.03F, 2.3F, 5.0F, 3.0F).depth(1.2F, 1.2F).maxAngle(75.0F);
   }

   private static Decal pawDecal() {
      Canvas var0 = new Canvas(128, 128);
      int var1 = -877910;
      Shape2D var2 = Shape2D.blend(8.0F, Shape2D.ellipse(64.0F, 90.0F, 27.0F, 19.0F), Shape2D.circle(48.0F, 78.0F, 15.0F), Shape2D.circle(80.0F, 78.0F, 15.0F));
      var0.fill(var2, var1);
      var0.fill(Shape2D.ellipse(28.0F, 52.0F, 11.0F, 14.0F).rotate(-25.0F, 28.0F, 52.0F), var1);
      var0.fill(Shape2D.ellipse(51.0F, 33.0F, 11.0F, 14.5F).rotate(-8.0F, 51.0F, 33.0F), var1);
      var0.fill(Shape2D.ellipse(77.0F, 33.0F, 11.0F, 14.5F).rotate(8.0F, 77.0F, 33.0F), var1);
      var0.fill(Shape2D.ellipse(100.0F, 52.0F, 11.0F, 14.0F).rotate(25.0F, 100.0F, 52.0F), var1);
      return Decal.front(var0, 1.2F, 20.6F, 1.6F, 1.5F, 1.5F).depth(1.5F, 1.5F).maxAngle(70.0F);
   }

   private static Canvas ribs(int var0, int var1) {
      Canvas var2 = new Canvas(var1, 16);
      float var3 = (float)var1 / var0;

      for (int var4 = 0; var4 < var0; var4++) {
         float var5 = var4 * var3 + var3 * 0.5F;
         var2.fill(Shape2D.box(var5, 8.0F, var3 * 0.18F, 8.0F, 0.0F), 1078491756);
      }

      return var2;
   }

   private static Decal hemRib() {
      return Decal.wrap(ribs(64, 506), 0.0F, 14.2F, -0.05F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 360.0F, 0.62F).radius(1.2F, 3.3F);
   }

   private static Decal cuffRib() {
      return Decal.wrap(ribs(24, 256), -3.77F, 14.3F, 0.57F, 0.06F, 1.65F, -0.22F, 0.0F, 0.0F, 1.0F, 360.0F, 1.2F).radius(0.6F, 1.4F);
   }

   private static Decal bellDecal(float var0) {
      Canvas var1 = new Canvas(64, 64);
      var1.fill(Shape2D.box(32.0F, 30.0F, 30.0F, 2.0F, 1.0F), -5737444);
      var1.fill(Shape2D.circle(32.0F, 40.0F, 4.5F), -11915254);
      var1.fill(Shape2D.line(32.0F, 40.0F, 32.0F, 60.0F, 3.5F), -11915254);
      var1.fill(Shape2D.ellipse(24.0F, 20.0F, 5.0F, 3.5F), -1426063361);
      return Decal.front(var1, var0, 19.6F, 2.5F, 1.0F, 1.0F).depth(0.6F, 0.6F).maxAngle(70.0F);
   }

   private static Decal laces() {
      Canvas var0 = new Canvas(96, 128);
      int var1 = -7710102;

      for (int var2 = 0; var2 < 4; var2++) {
         float var3 = 30.0F + var2 * 22.0F;
         var0.fill(Shape2D.line(24.0F, var3, 72.0F, var3 + 14.0F, 6.0F), -1);
         var0.fill(Shape2D.line(72.0F, var3, 24.0F, var3 + 14.0F, 6.0F), -1);
         var0.fill(Shape2D.circle(20.0F, var3, 3.5F), var1);
         var0.fill(Shape2D.circle(76.0F, var3, 3.5F), var1);
      }

      var0.fill(Shape2D.ellipse(36.0F, 14.0F, 11.0F, 6.0F).outline(4.0F), -1);
      var0.fill(Shape2D.ellipse(60.0F, 14.0F, 11.0F, 6.0F).outline(4.0F), -1);
      var0.fill(Shape2D.circle(48.0F, 16.0F, 4.0F), -1);
      Vec3 var6 = new Vec3(-1.2F, 0.0F, 0.2F);
      Vec3 var7 = new Vec3(-1.2F, 1.62F, 0.72F).rotate(0.0F, -7.0F, 0.0F, var6);
      Vec3 var4 = new Vec3(0.0F, 0.8F, 0.6F).rotate(0.0F, -7.0F, 0.0F);
      Vec3 var5 = new Vec3(0.0F, 0.6F, -0.8F).rotate(0.0F, -7.0F, 0.0F);
      return Decal.project(var0, var7.x(), var7.y(), var7.z(), var4.x(), var4.y(), var4.z(), var5.x(), var5.y(), var5.z(), 0.95F, 1.25F)
         .depth(0.6F, 0.6F)
         .maxAngle(65.0F);
   }

   @Environment(EnvType.CLIENT)
   private static final class Curve {
      private final float[] xs;
      private final float[] ys;
      private final float[] zs;
      private final float[] len;
      private final int n;

      Curve(float[] var1) {
         int var2 = var1.length / 4 - 2;
         byte var3 = 12;
         this.n = var2 * var3 + 1;
         this.xs = new float[this.n];
         this.ys = new float[this.n];
         this.zs = new float[this.n];
         this.len = new float[this.n];
         int var4 = 0;

         for (int var5 = 1; var5 <= var2; var5++) {
            int var6 = (var5 - 1) * 4;
            int var7 = var5 * 4;
            int var8 = (var5 + 1) * 4;
            float var9 = var5 == 1 ? var1[0] : (var1[var6] + var1[var7]) * 0.5F;
            float var10 = var5 == 1 ? var1[1] : (var1[var6 + 1] + var1[var7 + 1]) * 0.5F;
            float var11 = var5 == 1 ? var1[2] : (var1[var6 + 2] + var1[var7 + 2]) * 0.5F;
            float var12 = var5 == var2 ? var1[var8] : (var1[var7] + var1[var8]) * 0.5F;
            float var13 = var5 == var2 ? var1[var8 + 1] : (var1[var7 + 1] + var1[var8 + 1]) * 0.5F;
            float var14 = var5 == var2 ? var1[var8 + 2] : (var1[var7 + 2] + var1[var8 + 2]) * 0.5F;

            for (int var15 = 0; var15 < var3 || var5 == var2 && var15 == var3; var15++) {
               float var16 = (float)var15 / var3;
               float var17 = 1.0F - var16;
               this.xs[var4] = var17 * var17 * var9 + 2.0F * var17 * var16 * var1[var7] + var16 * var16 * var12;
               this.ys[var4] = var17 * var17 * var10 + 2.0F * var17 * var16 * var1[var7 + 1] + var16 * var16 * var13;
               this.zs[var4] = var17 * var17 * var11 + 2.0F * var17 * var16 * var1[var7 + 2] + var16 * var16 * var14;
               if (var4 > 0) {
                  float var18 = this.xs[var4] - this.xs[var4 - 1];
                  float var19 = this.ys[var4] - this.ys[var4 - 1];
                  float var20 = this.zs[var4] - this.zs[var4 - 1];
                  this.len[var4] = this.len[var4 - 1] + (float)Math.sqrt(var18 * var18 + var19 * var19 + var20 * var20);
               }

               var4++;
            }
         }
      }

      float param(float var1, float var2, float var3) {
         int var4 = 0;
         float var5 = 3.4028235E38F;

         for (int var6 = 0; var6 < this.n; var6++) {
            float var7 = var1 - this.xs[var6];
            float var8 = var2 - this.ys[var6];
            float var9 = var3 - this.zs[var6];
            float var10 = var7 * var7 + var8 * var8 + var9 * var9;
            if (var10 < var5) {
               var5 = var10;
               var4 = var6;
            }
         }

         return this.len[var4] / this.len[this.n - 1];
      }

      int tailColor(float var1, float var2, float var3) {
         float var4 = this.param(var1, var2, var3);
         return Colors.mix(Colors.mix(-3647457, -681403, Mika.smooth(0.04F, 0.22F, var4)), -3618, Mika.smooth(0.72F, 0.8F, var4));
      }

      Decal chevron(Canvas var1, float var2) {
         float var4 = var2 * this.len[this.n - 1];
         int var3 = 1;

         while (var3 < this.n - 2 && this.len[var3] < var4) {
            var3++;
         }

         float var5 = this.xs[var3 + 1] - this.xs[var3 - 1];
         float var6 = this.ys[var3 + 1] - this.ys[var3 - 1];
         float var7 = this.zs[var3 + 1] - this.zs[var3 - 1];
         return Decal.wrap(
               var1,
               this.xs[var3],
               this.ys[var3],
               this.zs[var3],
               var5,
               var6,
               var7,
               this.xs[var3] - 1.8F,
               this.ys[var3] - 17.6F,
               this.zs[var3] + 3.3F,
               140.0F,
               0.9F
            )
            .radius(0.3F, 1.6F);
      }
   }
}
