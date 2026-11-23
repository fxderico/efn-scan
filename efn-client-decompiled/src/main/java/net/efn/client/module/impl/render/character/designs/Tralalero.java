package net.efn.client.module.impl.render.character.designs;

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
import net.efn.client.module.impl.render.character.kit.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Tralalero extends CharacterDesign {
   private static final float YJ = 17.9F;
   private static final float PIVOT_Z = -1.5F;
   private static final float SHIFT = 0.8F;
   private static final float LEG_X = 2.9F;
   private static final float HIP = 12.0F;
   private static final float LEG_Z = 3.4F;
   private static final float KNEE = 6.4F;
   private static final float MID_HIP = 14.2F;
   private static final float MID_Z = -7.0F;
   private static final float SHOE_SCALE = 1.15F;
   private static final float[] BODY_Z = new float[]{-12.4F, -10.7F, -8.4F, -5.7F, -2.9F, 0.0F, 2.9F, 5.8F, 8.3F, 10.3F, 11.8F, 12.8F, 13.4F};
   private static final float[] BODY_Y = new float[]{1.4F, 1.15F, 0.8F, 0.4F, 0.1F, 0.0F, 0.05F, 0.25F, 0.5F, 0.75F, 0.95F, 1.05F, 1.1F};
   private static final float[] BODY_RY = new float[]{1.0F, 2.1F, 3.3F, 4.6F, 5.5F, 5.85F, 5.75F, 5.2F, 4.35F, 3.35F, 2.3F, 1.35F, 0.45F};
   private static final float[] BODY_RX = new float[]{0.9F, 1.8F, 2.9F, 4.2F, 5.1F, 5.45F, 5.35F, 4.8F, 3.95F, 3.0F, 2.1F, 1.25F, 0.45F};
   private static final float[] LINE_Z = new float[]{-14.5F, -11.0F, -8.0F, -5.0F, -2.0F, 1.0F, 3.5F, 5.5F, 7.0F, 9.0F, 11.0F, 14.5F};
   private static final float[] LINE_Y = new float[]{19.699999F, 19.1F, 18.1F, 17.1F, 16.4F, 16.3F, 16.9F, 17.65F, 17.4F, 17.6F, 18.1F, 18.4F};
   private static final float MOUTH_DEG = 152.0F;
   private static final int MOUTH_W = 284;
   private static final int MOUTH_H = 120;
   private static final float MOUTH_PX = 25.0F;
   private static final float MOUTH_Z = 9.45F;
   private static final float CORNER = 74.0F;
   private static final float LIP_FRONT = 11.0F;
   private static final float LIP_BACK = 7.4F;
   private static final int BACK_TOP = -12621165;
   private static final int BACK = -10515274;
   private static final int BELLY = -920330;
   private static final int FIN_TIP = -13346176;
   private static final int SKIN = -10055494;
   private static final int SKIN_LIGHT = -8674358;
   private static final int SKIN_DARK = -11634525;
   private static final int SHOE = -13933608;
   private static final int SHOE_DARK = -14989656;
   private static final int SHOE_LIGHT = -9593609;
   private static final int SOLE = -657935;
   private static final int SOLE_GREY = -4604218;
   private static final int LINE = -15194566;
   private static final int MOUTH = -12972264;
   private static final int GUM = -10872282;
   private static final int TEETH = -263436;

   public Tralalero() {
      super("tralalero", "Tralalero Tralala");
   }

   @Override
   protected void define(Rig var1) {
      var1.scale(1.0F).outline(-15194566, 0.16F).occlusion(0.5F).firstPersonArms(false);
      var1.part(Bone.BODY).pivot(0.0F, 15.9F, 0.0F);
      Shape var2 = Shapes.field(Tralalero::loft, -5.8F, 11.7F, -14.6F, 5.8F, 24.099998F, 14.2F);
      Shape var3 = Shapes.field((var0, var1x, var2x) -> bellyLine(var2x) - var1x, -6.0F, 10.9F, -14.0F, 6.0F, 24.9F, 15.0F);
      Shape var4 = Shapes.blend(1.1F, Shapes.intersect(0.5F, var2, var3).paint(Paint.field(Tralalero::skin)), dorsal()).move(0.0F, 0.0F, 0.8F);
      Canvas var5 = eyeCanvas();
      Canvas var6 = gillCanvas();
      Canvas var7 = laceCanvas();
      Canvas var8 = stripeCanvas();
      Part var9 = var1.extra("shark", Bone.BODY)
         .pivot(0.0F, 17.9F, -1.5F)
         .shape(var4)
         .resolution(1.2F)
         .detail(0.78F)
         .outline(0.1F)
         .neck(0.0F, 17.9F, 6.8F, 0.0F, 0.0F, 1.0F, 3.0F)
         .look(0.45F, 0.45F, 28.0F)
         .motion(Motion.none().idle(0.7F, 0.8F, 0.0F, 0.28F).walk(2.0F, 0.0F, 4.0F, 1.0F).follow(Bone.HEAD, 0.25F).spring(70.0F, 10.0F).limit(12.0F))
         .decal(eye(var5, 1.0F))
         .decal(eye(var5.mirrored(), -1.0F))
         .decal(gills(var6, 1.0F))
         .decal(gills(var6.mirrored(), -1.0F));
      Shape var10 = Shapes.field((var0, var1x, var2x) -> var1x - bellyLine(var2x) - 0.4F, -6.0F, 10.9F, -14.0F, 6.0F, 24.9F, 15.0F);
      Shape var11 = Shapes.intersect(var2.inflate(-0.15F), var10).paint(Paint.of(-920330).flatLight(0.4F).occlusion(0.35F)).move(0.0F, 0.0F, 0.8F);
      var1.extra("belly", var9).shape(var11).resolution(1.0F).detail(1.0F).decal(mouth());
      var1.extra("tail", var9)
         .pivot(0.0F, 19.199999F, -9.9F)
         .shape(caudal())
         .resolution(1.0F)
         .motion(
            Motion.tail().idle(1.0F, 9.0F, 0.0F, 0.45F).walk(2.0F, 14.0F, 0.0F, 1.0F).drag(5.0F).hang(0.3F).turn(0.0F, 16.0F).spring(40.0F, 5.5F).bend(1.2F)
         );
      Part var12 = var1.part(Bone.RIGHT_ARM).pivot(-4.3F, 15.4F, 4.0F).grip(-6.8F, 13.4F, 2.7F).shape(pectoral()).resolution(0.9F).detail(0.8F).swing(0.35F);
      var1.part(Bone.LEFT_ARM).mirrorOf(var12);
      Part var13 = var1.part(Bone.RIGHT_LEG)
         .pivot(-2.9F, 12.0F, 3.4F)
         .shape(leg(-2.9F, 12.0F, 3.4F, -1.0F))
         .resolution(1.0F)
         .detail(1.4F)
         .swing(0.5F, 35.0F)
         .joint(-2.8000002F, 6.5F, 3.5500002F, 0.8F);
      var1.part(Bone.LEFT_LEG).mirrorOf(var13);
      Part var14 = var1.extra("shoe_r", Bone.RIGHT_LEG)
         .shape(shoe(-2.9F, 3.4F))
         .resolution(1.0F)
         .detail(1.6F)
         .decal(laces(var7, -2.9F, 3.4F))
         .decal(stripe(var8, -2.9F, 3.4F, -1.0F));
      var1.extra("shoe_l", Bone.LEFT_LEG).mirrorOf(var14);
      Part var15 = var1.extra("leg_mid", Bone.BODY)
         .pivot(0.0F, 14.2F, -7.0F)
         .shape(leg(0.0F, 14.2F, -7.0F, 1.0F))
         .resolution(1.0F)
         .detail(1.4F)
         .motion(Motion.none().follow(Bone.LEFT_LEG, 0.5F).walk(22.0F, 0.0F, 0.0F, 1.0F).hang(0.5F).limit(45.0F));
      var1.extra("shoe_mid", var15)
         .shape(shoe(0.0F, -7.0F))
         .resolution(1.0F)
         .detail(1.6F)
         .decal(laces(var7, 0.0F, -7.0F))
         .decal(stripe(var8, 0.0F, -7.0F, -1.0F))
         .decal(stripe(var8.mirrored(), 0.0F, -7.0F, 1.0F));
   }

   private static float smooth(float var0, float var1, float var2) {
      float var3 = (var2 - var0) / (var1 - var0);
      var3 = var3 < 0.0F ? 0.0F : Math.min(var3, 1.0F);
      return var3 * var3 * (3.0F - 2.0F * var3);
   }

   private static int segment(float[] var0, float var1) {
      int var2 = 0;

      while (var2 < var0.length - 2 && var1 > var0[var2 + 1]) {
         var2++;
      }

      return var2;
   }

   private static float spline(float[] var0, float[] var1, int var2, float var3, boolean var4) {
      int var5 = var0.length;
      float var6 = var0[var2 + 1] - var0[var2];
      float var7 = Math.max(0.0F, Math.min(1.0F, (var3 - var0[var2]) / var6));
      float var8 = var1[Math.max(0, var2 - 1)];
      float var9 = var1[var2];
      float var10 = var1[var2 + 1];
      float var11 = var1[Math.min(var5 - 1, var2 + 2)];
      float var12 = 2.0F * var8 - 5.0F * var9 + 4.0F * var10 - var11;
      float var13 = 3.0F * var9 - var8 - 3.0F * var10 + var11;
      return var4
         ? 0.5F * (var10 - var8 + 2.0F * var12 * var7 + 3.0F * var13 * var7 * var7) / var6
         : 0.5F * (2.0F * var9 + (var10 - var8) * var7 + var12 * var7 * var7 + var13 * var7 * var7 * var7);
   }

   private static float bellyLine(float var0) {
      return spline(LINE_Z, LINE_Y, segment(LINE_Z, var0), var0, false);
   }

   private static float loft(float var0, float var1, float var2) {
      int var3 = BODY_Z.length;
      if (var2 > BODY_Z[var3 - 1]) {
         float var18 = var1 - 17.9F - BODY_Y[var3 - 1];
         float var20 = var2 - BODY_Z[var3 - 1];
         return (float)Math.sqrt(var0 * var0 + var18 * var18 + var20 * var20) - BODY_RY[var3 - 1];
      } else if (var2 < BODY_Z[0]) {
         float var17 = var1 - 17.9F - BODY_Y[0];
         float var19 = var2 - BODY_Z[0];
         return (float)Math.sqrt(var0 * var0 + var17 * var17 + var19 * var19) - BODY_RX[0];
      } else {
         int var4 = segment(BODY_Z, var2);
         float var5 = 17.9F + spline(BODY_Z, BODY_Y, var4, var2, false);
         float var6 = spline(BODY_Z, BODY_RY, var4, var2, false);
         float var7 = spline(BODY_Z, BODY_RX, var4, var2, false);
         float var8 = spline(BODY_Z, BODY_RY, var4, var2, true);
         float var9 = spline(BODY_Z, BODY_RX, var4, var2, true);
         float var10 = spline(BODY_Z, BODY_Y, var4, var2, true);
         float var11 = Math.max(Math.abs(var9), Math.abs(var8)) + Math.abs(var10);
         float var12 = var0 / var7;
         float var13 = (var1 - var5) / var6;
         float var14 = (float)Math.sqrt(var12 * var12 + var13 * var13);
         float var15 = (float)Math.sqrt(var12 * var12 / (var7 * var7) + var13 * var13 / (var6 * var6));
         float var16 = var15 < 1.0E-6F ? -Math.min(var7, var6) : var14 * (var14 - 1.0F) / var15;
         return var16 / (float)Math.sqrt(1.0F + var11 * var11);
      }
   }

   private static int skin(float var0, float var1, float var2) {
      return Colors.mix(-10515274, -12621165, smooth(bellyLine(var2) + 0.5F, 23.4F, var1));
   }

   private static Shape2D outline(float var0, float... var1) {
      float[] var2 = (float[])var1.clone();

      for (byte var3 = 1; var3 < var2.length; var3 += 2) {
         var2[var3] += var0;
      }

      return Shape2D.polygon(var2);
   }

   private static Shape blade(Shape2D var0, float var1, float var2, float var3, float var4) {
      Shape var5 = Shapes.extrude(var0, var3, var3);
      float var6 = (float)Math.sqrt(1.0F + var4 * var4);
      return Shapes.field((var6x, var7, var8) -> {
         float var9 = (float)Math.sqrt((var6x - var1) * (var6x - var1) + (var7 - var2) * (var7 - var2));
         return Math.max(var5.distance(var6x, var7, var8), (Math.abs(var8) - var3 + var4 * var9) / var6);
      }, var0.minX(), var0.minY(), -var3, var0.maxX(), var0.maxY(), var3);
   }

   private static Shape side(Shape var0) {
      return orient(var0, 0.0F, 0.0F, 0.0F, new Vec3(0.0F, 0.0F, -1.0F), Vec3.UP);
   }

   private static Shape dorsal() {
      Shape2D var0 = outline(
         17.9F,
         -2.6F,
         4.2F,
         -1.9F,
         6.2F,
         -1.0F,
         7.9F,
         0.1F,
         9.4F,
         1.4F,
         10.6F,
         2.6F,
         11.5F,
         3.7F,
         12.1F,
         3.2F,
         11.2F,
         2.75F,
         10.0F,
         2.45F,
         8.7F,
         2.4F,
         7.4F,
         2.6F,
         6.2F,
         3.1F,
         5.0F,
         3.6F,
         4.2F
      );
      Shape2D var1 = outline(17.9F, 8.3F, 2.2F, 8.9F, 3.6F, 9.7F, 4.7F, 10.6F, 5.5F, 10.2F, 4.5F, 10.1F, 3.4F, 10.4F, 2.2F);
      Shape var2 = Shapes.union(side(blade(var0.inflate(0.1F), 0.4F, 22.5F, 1.5F, 0.125F)), side(blade(var1, 9.4F, 20.699999F, 0.72F, 0.14F)));
      return var2.paint(Paint.vertical(-12621165, 22.9F, -13346176, 29.4F));
   }

   private static Shape caudal() {
      Shape2D var0 = outline(
         17.9F,
         12.0F,
         1.4F,
         11.7F,
         2.2F,
         12.4F,
         3.8F,
         13.2F,
         5.4F,
         14.0F,
         7.0F,
         14.8F,
         8.3F,
         15.4F,
         9.1F,
         15.0F,
         7.9F,
         14.6F,
         6.3F,
         14.3F,
         4.6F,
         14.1F,
         3.0F,
         14.2F,
         1.6F,
         14.4F,
         0.2F,
         14.7F,
         -1.3F,
         14.95F,
         -2.7F,
         15.2F,
         -3.9F,
         14.5F,
         -3.2F,
         13.6F,
         -2.2F,
         12.8F,
         -1.0F,
         11.9F,
         0.4F
      );
      Shape var1 = side(blade(var0.inflate(0.08F), 12.0F, 19.3F, 0.82F, 0.062F));
      Shape var2 = Shapes.cone(0.0F, 19.1F, -10.3F, 1.5F, 0.0F, 19.3F, -12.5F, 1.0F);
      Shape var3 = Shapes.ellipsoid(0.0F, 19.3F, -12.2F, 1.3F, 0.55F, 1.4F);
      return Shapes.blend(0.7F, var2, var3, var1).paint(Paint.radial(-10515274, 0.0F, 19.3F, -12.2F, 5.5F, -13346176)).move(0.0F, 0.0F, 0.8F);
   }

   private static Shape pectoral() {
      Shape2D var0 = Shape2D.polygon(
         -1.2F,
         2.0F,
         0.5F,
         2.1F,
         2.0F,
         1.7F,
         3.5F,
         1.0F,
         5.0F,
         0.1F,
         6.3F,
         -0.9F,
         7.0F,
         -1.6F,
         6.0F,
         -1.5F,
         4.6F,
         -1.35F,
         3.2F,
         -1.3F,
         1.8F,
         -1.5F,
         0.4F,
         -1.9F,
         -1.2F,
         -2.2F
      );
      Shape var1 = orient(blade(var0, 0.0F, 0.0F, 0.62F, 0.05F), -4.3F, 15.4F, 3.2F, new Vec3(-0.8F, -0.5F, -0.33F), new Vec3(0.25F, 0.05F, 1.0F));
      return var1.paint(Paint.radial(-10515274, -4.3F, 15.4F, 3.2F, 6.0F, -13346176)).move(0.0F, 0.0F, 0.8F);
   }

   private static Paint legPaint(float var0) {
      return Paint.field((var1, var2, var3) -> {
         float var4 = smooth(var0 + 0.3F, var0 + 1.2F, var3);
         float var5 = smooth(1.3F, 0.3F, Math.abs(var2 - 6.4F)) * smooth(var0 - 0.2F, var0 - 0.8F, var3);
         return Colors.mix(Colors.mix(-10055494, -8674358, var4), -11634525, var5);
      });
   }

   private static Shape leg(float var0, float var1, float var2, float var3) {
      float var4 = 6.4F;
      float var5 = var1 - 6.4F;
      Shape var6 = Shapes.capsule(var0, var1, var2, var0, var1 + 2.8F, var2, 1.85F);
      Shape var7 = Shapes.cone(var0, var1 + 0.3F, var2, 2.0F, var0 - var3 * 0.15F, var4 + 0.9F, var2 + 0.15F, 1.12F);
      Shape var8 = Shapes.ellipsoid(var0 + var3 * 0.05F, var4 + 0.56F * var5, var2 + 0.85F, 1.18F, 0.42F * var5, 1.08F);
      Shape var9 = Shapes.ellipsoid(var0 + var3 * 0.7F, var4 + 0.55F * var5, var2, 0.85F, 0.45F * var5, 1.05F);
      Shape var10 = Shapes.ellipsoid(var0 - var3 * 0.6F, var4 + 1.35F, var2 + 0.55F, 0.8F, 1.0F, 0.78F);
      Shape var11 = Shapes.ellipsoid(var0, var4 + 0.55F * var5, var2 - 0.7F, 1.2F, 0.45F * var5, 0.95F);
      Shape var12 = Shapes.sphere(var0 - var3 * 0.1F, var4 + 0.1F, var2 + 0.15F, 0.9F);
      Shape var13 = Shapes.ellipsoid(var0 - var3 * 0.05F, var4 + 0.25F, var2 + 0.95F, 0.55F, 0.62F, 0.4F);
      Shape var14 = Shapes.cone(var0 - var3 * 0.1F, var4 - 0.3F, var2 + 0.25F, 0.86F, var0, 2.4F, var2 + 0.05F, 0.56F);
      Shape var15 = Shapes.ellipsoid(var0 - var3 * 0.36F, var4 - 1.55F, var2 - 0.62F, 0.84F, 1.55F, 0.98F);
      Shape var16 = Shapes.ellipsoid(var0 + var3 * 0.36F, var4 - 1.25F, var2 - 0.55F, 0.76F, 1.35F, 0.88F);
      Shape var17 = Shapes.cone(var0, var4 - 2.7F, var2 - 0.55F, 0.5F, var0, 2.2F, var2 - 0.5F, 0.42F);
      Shape var18 = Shapes.sphere(var0, 2.3F, var2, 0.58F);
      Shape var19 = Shapes.union(var6, Shapes.blend(0.3F, var7, var8, var9, var10, var11, var12, var13, var14, var15, var16, var17, var18))
         .paint(legPaint(var2));
      Shape var20 = flat(foot(0.0F), var0, 0.6F, var2, 0.6F, 0.52F).paint(Paint.vertical(-4604218, 0.2F, -657935, 0.3F));
      return Shapes.union(var19, var20.scale(1.15F, var0, 0.0F, var2));
   }

   private static Shape orient(Shape var0, float var1, float var2, float var3, Vec3 var4, Vec3 var5) {
      Vec3 var6 = var4.normalize();
      Vec3 var7 = var6.cross(var5).normalize();
      Vec3 var8 = var7.cross(var6);
      float var9 = (float)Math.acos(Math.max(-1.0F, Math.min(1.0F, (var6.x() + var8.y() + var7.z() - 1.0F) * 0.5F)));
      Shape var10 = var9 < 1.0E-4F
         ? var0
         : var0.rotateAxis(var8.z() - var7.y(), var7.x() - var6.z(), var6.y() - var8.x(), (float)Math.toDegrees(var9), 0.0F, 0.0F, 0.0F);
      return var10.move(var1, var2, var3);
   }

   private static Shape flat(Shape2D var0, float var1, float var2, float var3, float var4, float var5) {
      return orient(Shapes.extrude(var0, var4, var5), var1, var2, var3, Vec3.LEFT, Vec3.FORWARD);
   }

   private static Shape2D foot(float var0) {
      Shape2D var1 = Shape2D.circle(0.0F, -1.05F, 1.0F);
      Shape2D var2 = Shape2D.circle(0.0F, 1.4F, 1.22F);
      Shape2D var3 = Shape2D.circle(0.0F, 2.35F, 1.05F);
      Shape2D var4 = Shape2D.blend(2.2F, var1, var2, var3);
      return var0 > 0.0F ? var4.inflate(-var0) : var4;
   }

   private static Shape shoe(float var0, float var1) {
      Shape var2 = Shapes.blend(
         0.8F, Shapes.cone(var0, 1.55F, var1 - 0.85F, 1.3F, var0, 1.25F, var1 + 1.1F, 1.3F), Shapes.ellipsoid(var0, 0.95F, var1 + 1.95F, 1.22F, 0.92F, 1.5F)
      );
      Paint var3 = Paint.field((var1x, var2x, var3x) -> Colors.mix(-13933608, -14989656, smooth(var1 - 0.6F, var1 - 1.3F, var3x)));
      Shape var4 = Shapes.intersect(0.15F, var2, flat(foot(0.1F), var0, 2.0F, var1, 1.3F, 0.25F)).paint(var3);
      Shape var5 = Shapes.torus(var0, 2.95F, var1 - 0.5F, 0.78F, 0.34F).rotate(-14.0F, 0.0F, 0.0F, var0, 2.95F, var1 - 0.5F).color(-14989656);
      Shape var6 = Shapes.ellipsoid(var0, 2.9F, var1 + 0.35F, 0.52F, 0.4F, 0.7F).rotate(20.0F, 0.0F, 0.0F, var0, 2.9F, var1 + 0.35F).color(-9593609);
      Shape var7 = Shapes.ellipsoid(var0, 3.15F, var1 - 1.25F, 0.26F, 0.45F, 0.2F).color(-14989656);
      return Shapes.blend(0.2F, var4, var5, var6, var7).scale(1.15F, var0, 0.0F, var1);
   }

   private static Canvas laceCanvas() {
      Canvas var0 = new Canvas(60, 76);

      for (int var1 = 0; var1 < 4; var1++) {
         float var2 = 6.5F + var1 * 20.25F;
         float var3 = 22.5F - var1 * 1.9F;
         var0.fill(Shape2D.box(30.0F, var2, var3, 4.1F, 4.1F), -1);
         var0.fill(Shape2D.circle(30.0F - var3 - 2.3F, var2, 2.4F), -14274500);
         var0.fill(Shape2D.circle(30.0F + var3 + 2.3F, var2, 2.4F), -14274500);
      }

      return var0;
   }

   private static Canvas stripeCanvas() {
      Canvas var0 = new Canvas(160, 30);
      var0.fill(Shape2D.line(20.0F, 9.0F, 140.0F, 21.0F, 13.0F), -1);
      return var0;
   }

   private static Decal stripe(Canvas var0, float var1, float var2, float var3) {
      float var4 = 1.15F;
      return Decal.project(var0, var1 + var3 * 1.3F * var4, 1.55F * var4, var2 + 0.35F * var4, var3, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 3.6F * var4, 0.68F * var4)
         .depth(0.9F, 0.9F)
         .maxAngle(70.0F)
         .flatLight(0.6F)
         .occlusion(0.4F);
   }

   private static Decal laces(Canvas var0, float var1, float var2) {
      float var3 = 1.15F;
      return Decal.project(var0, var1, 2.7F * var3, var2 + 1.3F * var3, 0.0F, 0.79F, 0.61F, 0.0F, 1.0F, 0.0F, 1.25F * var3, 1.58F * var3)
         .depth(0.9F, 0.9F)
         .flatLight(0.6F)
         .occlusion(0.4F);
   }

   private static Canvas eyeCanvas() {
      Canvas var0 = new Canvas(96, 96);
      float var1 = 48.0F;
      float var2 = 50.0F;
      Shape2D var3 = Shape2D.ellipse(var1, var2, 21.0F, 20.0F);
      var0.fill(var3.inflate(4.0F), 1428832344, 3.0F);
      var0.fill(var3, Paint2D.radial(var1 - 4.5F, var2 - 6.0F, 26.0F, -14011322, -16316147));
      var0.fill(Shape2D.circle(var1 - 7.5F, var2 - 7.0F, 5.6F), -1);
      var0.fill(Shape2D.circle(var1 + 7.5F, var2 + 8.0F, 2.4F), -1325400065);
      var0.fill(Shape2D.arc(var1, var2 + 2.0F, 22.0F, 200.0F, 340.0F, 4.0F), -2144713630, 2.0F);
      return var0;
   }

   private static Decal eye(Canvas var0, float var1) {
      return Decal.project(var0, var1 * 3.3F, 20.1F, 10.2F, var1 * 0.84F, 0.36F, 0.4F, 0.0F, 1.0F, 0.0F, 2.2F, 2.2F)
         .depth(1.2F, 1.2F)
         .maxAngle(75.0F)
         .flatLight(0.7F);
   }

   private static Canvas gillCanvas() {
      Canvas var0 = new Canvas(120, 120);

      for (int var1 = 0; var1 < 5; var1++) {
         float var2 = 16.0F + var1 * 22.0F;
         float var3 = 42.0F - var1 * 3.0F;
         var0.fill(Shape2D.curve(var2, 58.0F - var3, var2 + 7.0F, 58.0F, var2, 58.0F + var3, 3.4F, 3.4F), -802799780);
      }

      return var0;
   }

   private static Decal gills(Canvas var0, float var1) {
      return Decal.project(var0, var1 * 5.1F, 18.5F, 6.0F, var1, 0.05F, 0.2F, 0.0F, 1.0F, 0.0F, 2.8F, 2.8F).depth(1.5F, 1.5F);
   }

   private static float mouthX(float var0) {
      return 142.0F + var0 * 284.0F / 152.0F;
   }

   private static float arch(float var0) {
      float var1 = Math.min(1.0F, Math.abs(var0) / 74.0F);
      float var2 = 11.0F - 3.6F * (float)Math.pow(var1, 1.8);
      return 60.0F - (var2 - 9.45F) * 25.0F;
   }

   private static float gape(float var0) {
      float var1 = Math.min(1.0F, Math.abs(var0) / 74.0F);
      return 15.000001F * (1.0F - var1 * var1);
   }

   private static Decal mouth() {
      Canvas var0 = new Canvas(284, 120);
      float var1 = 0.53521127F;
      float[] var2 = new float[284];
      float[] var3 = new float[284];
      float[] var4 = new float[284];

      for (int var5 = 0; var5 < 284; var5++) {
         float var6 = (var5 + 0.5F - 142.0F) * var1;
         float var7 = (arch(var6 + 0.5F) - arch(var6 - 0.5F)) * var1;
         var2[var5] = arch(var6);
         var3[var5] = Math.abs(var6) > 75.0F ? -1.0F : gape(var6);
         var4[var5] = 1.0F / (float)Math.sqrt(1.0F + var7 * var7);
      }

      Shape2D var15 = Shape2D.field((var3x, var4x) -> {
         int var5x = Math.max(0, Math.min(283, (int)var3x));
         return var3[var5x] < 0.0F ? 4.0F : (Math.abs(var4x - var2[var5x]) - var3[var5x]) * var4[var5x];
      }, mouthX(-76.0F), 3.0F, mouthX(76.0F), 117.0F);
      var0.fill(var15, Paint2D.linear(0.0F, 0.0F, 0.0F, 120.0F, -10872282, -12972264));
      float var16 = 0.75F;
      float var17 = 2.5F;

      for (int var8 = -4; var8 <= 4; var8++) {
         float var9 = var8 * 15.0F;
         float var10 = gape(var9);
         if (!(var10 < var17)) {
            Shape2D var11 = Shape2D.polygon(
               mouthX(var9 - 6.0F),
               arch(var9 - 6.0F) - gape(var9 - 6.0F) - var16,
               mouthX(var9 + 6.0F),
               arch(var9 + 6.0F) - gape(var9 + 6.0F) - var16,
               mouthX(var9),
               arch(var9) + var10 * 0.5F
            );
            var0.fill(var11.intersect(var15), -263436);
            float var12 = var9 + 7.5F;
            float var13 = gape(var12);
            if (!(var13 < var17) && var8 != 4) {
               Shape2D var14 = Shape2D.polygon(
                  mouthX(var12 - 5.0F),
                  arch(var12 - 5.0F) + gape(var12 - 5.0F) + var16,
                  mouthX(var12 + 5.0F),
                  arch(var12 + 5.0F) + gape(var12 + 5.0F) + var16,
                  mouthX(var12),
                  arch(var12) - var13 * 0.35F
               );
               var0.fill(var14.intersect(var15), -1185056);
            }
         }
      }

      var0.fill(var15.outline(2.5F), -15194566);
      return Decal.wrap(var0, 0.0F, 18.35F, 10.25F, 0.0F, 0.0F, 1.0F, 0.0F, -1.0F, 0.0F, 152.0F, 4.8F).maxAngle(80.0F).flatLight(1.0F).occlusion(0.3F);
   }
}
