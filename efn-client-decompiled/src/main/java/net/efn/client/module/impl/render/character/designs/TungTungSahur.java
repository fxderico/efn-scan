package net.efn.client.module.impl.render.character.designs;

import net.efn.client.module.impl.render.character.kit.Bone;
import net.efn.client.module.impl.render.character.kit.Canvas;
import net.efn.client.module.impl.render.character.kit.CharacterDesign;
import net.efn.client.module.impl.render.character.kit.Decal;
import net.efn.client.module.impl.render.character.kit.Motion;
import net.efn.client.module.impl.render.character.kit.Noise;
import net.efn.client.module.impl.render.character.kit.Paint;
import net.efn.client.module.impl.render.character.kit.Paint2D;
import net.efn.client.module.impl.render.character.kit.Part;
import net.efn.client.module.impl.render.character.kit.Rig;
import net.efn.client.module.impl.render.character.kit.Sample;
import net.efn.client.module.impl.render.character.kit.Shape;
import net.efn.client.module.impl.render.character.kit.Shape2D;
import net.efn.client.module.impl.render.character.kit.Shapes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TungTungSahur extends CharacterDesign {
   private static final float R = 5.0F;
   private static final float BOTTOM = 12.0F;
   private static final float TOP = 34.0F;
   private static final float RIM = 0.85F;
   private static final float FIST_X = -7.0F;
   private static final float FIST_Y = 11.6F;
   private static final float FIST_Z = 0.72F;
   private static final float[] KNUCKLE_DIR = unit(-0.16F, 0.86F, 0.48F);
   private static final int WOOD = -1063542;
   private static final int WOOD_DARK = -4158378;
   private static final int WOOD_WORN = -5868993;
   private static final int FISSURE = -9551070;
   private static final int KNOT = -11653867;
   private static final int CUT = -1389672;
   private static final int LIMB = -3567528;
   private static final int LIMB_DARK = -7053254;
   private static final int INK = -15003638;
   private static final int LINE = -12967406;

   public TungTungSahur() {
      super("tung_tung_sahur", "Tung Tung Tung Sahur");
   }

   @Override
   protected void define(Rig var1) {
      var1.scale(1.0F).outline(-12967406, 0.16F).occlusion(0.3F).firstPersonArms(false);
      var1.part(Bone.HEAD).pivot(0.0F, 12.5F, 0.0F);
      Part var2 = var1.extra("upright", Bone.HEAD).motion(Motion.none().hang(1.0F).limit(100.0F));
      var1.extra("log", var2)
         .shape(log())
         .detail(1.5F)
         .resolution(1.2F)
         .flatLight(0.35F)
         .neck(0.0F, 22.5F, 0.0F, 0.0F, 1.0F, 0.0F, 4.0F)
         .look(0.6F, 0.0F, 40.0F)
         .motion(Motion.none().walk(0.0F, 4.0F, -6.0F, 1.0F).drag(6.0F))
         .decal(eyes())
         .decal(mouth())
         .decal(rings());
      Part var3 = var1.part(Bone.RIGHT_ARM).pivot(-5.3F, 22.8F, 0.2F).grip(-7.0F, 11.6F, 0.72F).shape(arm()).joint(-6.57F, 17.1F, 0.14F, 0.6F);
      var1.part(Bone.LEFT_ARM).mirrorOf(var3);
      Part var4 = var1.extra("hand_r", Bone.RIGHT_ARM).shape(hand()).resolution(0.6F).detail(0.8F);
      var1.extra("hand_l", Bone.LEFT_ARM).mirrorOf(var4);
      Part var5 = var1.part(Bone.RIGHT_LEG).pivot(-2.2F, 12.8F, 0.0F).shape(leg()).swing(0.6F, 35.0F).joint(-2.3F, 6.75F, 0.16F, 0.7F);
      var1.part(Bone.LEFT_LEG).mirrorOf(var5);
      Part var6 = var1.extra("foot_r", Bone.RIGHT_LEG).shape(foot()).resolution(0.7F).detail(0.8F);
      var1.extra("foot_l", Bone.LEFT_LEG).mirrorOf(var6);
   }

   private static Shape log() {
      Shape var0 = Shapes.field(TungTungSahur::trunk, -5.5F, 11.8F, -5.5F, 5.5F, 34.2F, 5.5F);
      TungTungSahur.Bark var1 = new TungTungSahur.Bark(
         Paint.wood(-1063542, -4158378, 0.3F, 0.0F, -0.2F, 0.0F, 1.0F, 0.0F, 0.8F).noise(-5868993, 3.5F, 0.08F).grain(0.35F, 0.04F)
      );
      Shape var2 = Shapes.blend(
         0.35F,
         var0.paint(var1),
         knot(38.0F, 17.2F, 0.8F),
         knot(165.0F, 27.5F, 0.95F),
         knot(-120.0F, 19.5F, 0.7F),
         knot(-58.0F, 14.2F, 0.55F),
         stub(128.0F, 25.2F),
         eye(1.0F, var1),
         eye(-1.0F, var1)
      );
      Shape var3 = Shapes.ellipsoid(0.0F, 25.35F, radius(0.0F, 25.35F, 1.0F) + 0.12F, 2.7F, 1.05F, 0.55F);
      return Shapes.subtract(0.12F, Shapes.subtract(0.35F, var2, var3), Shapes.union(hole(165.0F, 27.5F, 0.32F), hole(-120.0F, 19.5F, 0.24F)));
   }

   private static Shape stub(float var0, float var1) {
      float var2 = (float)Math.sin(Math.toRadians(var0));
      float var3 = (float)Math.cos(Math.toRadians(var0));
      float var4 = (float)Math.cos(Math.toRadians(32.0));
      float var5 = (float)Math.sin(Math.toRadians(32.0));
      float var6 = var2 * var4;
      float var8 = var3 * var4;
      float var9 = var2 * 3.9F;
      float var10 = var3 * 3.9F;
      float var11 = var9 + var6 * 3.05F;
      float var12 = var1 + var5 * 3.05F;
      float var13 = var10 + var8 * 3.05F;
      Shape var14 = Shapes.cone(var9, var1, var10, 0.98F, var11 + var6 * 0.4F, var12 + var5 * 0.4F, var13 + var8 * 0.4F, 0.64F)
         .paint(Paint.wood(-1063542, -4158378, var9, var1, var10, var6, var5, var8, 0.3F));
      Paint var15 = Paint.radial(-6264264, var11, var12, var13, 0.75F, -1389672);
      return Shapes.subtract(var14, Shapes.plane(var11, var12, var13, -var6, -var5, -var8).paint(var15));
   }

   private static Shape eye(float var0, Paint var1) {
      float var2 = var0 * 2.14F;
      float var3 = 28.72F;
      float var4 = var2 / 5.0F;
      float var5 = (float)Math.sqrt(1.0F - var4 * var4);
      float var6 = radius(var4, var3, var5) - 0.5F;
      float var7 = (float)Math.toDegrees(Math.asin(var4));
      return Shapes.ellipsoid(var4 * var6, var3, var5 * var6, 1.85F, 1.8F, 0.85F).rotate(0.0F, var7, 0.0F, var4 * var6, var3, var5 * var6).paint(var1);
   }

   private static Shape knot(float var0, float var1, float var2) {
      float var3 = (float)Math.sin(Math.toRadians(var0));
      float var4 = (float)Math.cos(Math.toRadians(var0));
      float var5 = radius(var3, var1, var4) + 0.28F;
      float var6 = var5 - var2;
      return Shapes.ellipsoid(var3 * var6, var1, var4 * var6, var2, var2 * 1.35F, var2)
         .paint(Paint.radial(-11653867, var3 * var5, var1, var4 * var5, var2 * 1.05F, -4158378));
   }

   private static Shape hole(float var0, float var1, float var2) {
      float var3 = (float)Math.sin(Math.toRadians(var0));
      float var4 = (float)Math.cos(Math.toRadians(var0));
      float var5 = radius(var3, var1, var4) + 0.28F;
      return Shapes.ellipsoid(var3 * var5, var1, var4 * var5, var2, var2 * 1.3F, var2).color(-11653867);
   }

   private static float radius(float var0, float var1, float var2) {
      float var3 = 0.17F * Noise.noise(var0 * 1.1F + 2.3F, var1 * 0.1F, var2 * 1.1F - 1.3F);
      return 5.0F + var3 - 0.17F * fissure(var0, var1, var2);
   }

   private static float fissure(float var0, float var1, float var2) {
      float var3 = Math.abs(Noise.noise(var0 * 3.0F - 5.1F, var1 * 0.08F, var2 * 3.0F + 0.7F));
      float var4 = smooth(0.3F, 0.62F, var2) * smooth(21.8F, 23.6F, var1);
      return Math.max(0.0F, 1.0F - var3 / 0.28F) * (1.0F - 0.88F * var4);
   }

   private static float trunk(float var0, float var1, float var2) {
      float var3 = (float)Math.sqrt(var0 * var0 + var2 * var2);
      float var4 = var3 > 1.0E-4F ? var0 / var3 : 0.0F;
      float var5 = var3 > 1.0E-4F ? var2 / var3 : 1.0F;
      float var6 = var3 - radius(var4, var1, var5) + 0.85F;
      float var7 = Math.abs(var1 - 23.0F) - 11.0F + 0.85F;
      float var8 = Math.max(var6, 0.0F);
      float var9 = Math.max(var7, 0.0F);
      return (float)Math.sqrt(var8 * var8 + var9 * var9) + Math.min(Math.max(var6, var7), 0.0F) - 0.85F;
   }

   private static Shape arm() {
      Paint var0 = stick(-5.4F, 22.6F, 0.2F, -7.0F, 11.6F, 0.72F);
      Shape var1 = Shapes.blend(0.2F, Shapes.sphere(-5.3F, 22.8F, 0.2F, 0.7F), Shapes.capsule(-5.3F, 22.8F, 0.2F, -3.9F, 22.8F, 0.2F, 0.55F));
      Shape var2 = Shapes.cone(-5.4F, 22.5F, 0.2F, 0.64F, -6.55F, 17.3F, 0.12F, 0.54F);
      Shape var3 = Shapes.sphere(-6.57F, 17.1F, 0.14F, 0.6F);
      Shape var4 = Shapes.cone(-6.6F, 16.85F, 0.14F, 0.55F, -6.93F, 12.6F, 0.5F, 0.5F);
      return Shapes.blend(0.3F, var1, var2, var3, var4).paint(var0);
   }

   private static Shape hand() {
      float var0 = KNUCKLE_DIR[0];
      float var1 = KNUCKLE_DIR[1];
      float var2 = KNUCKLE_DIR[2];
      Shape var3 = Shapes.ellipsoid(-6.92F, 11.650001F, 0.70000005F, 0.86F, 1.05F, 0.9F);
      Shape var4 = Shapes.cone(-6.93F, 12.75F, 0.5F, 0.5F, -6.95F, 12.150001F, 0.72F, 0.64F);
      Shape[] var5 = new Shape[4];

      for (int var6 = 0; var6 < 4; var6++) {
         float var7 = -0.66F + var6 * 0.44F;
         float var8 = -7.0F + var0 * var7 - 0.28F;
         float var9 = 11.6F + var1 * var7;
         float var10 = 0.72F + var2 * var7 + 0.72F;
         var5[var6] = Shapes.capsule(var8 - 0.35F, var9, var10 - 0.3F, var8 + 0.3F, var9, var10 + 0.1F, 0.26F + 0.02F * (3 - var6));
      }

      Shape var11 = Shapes.capsule(-6.38F, 12.1F, 0.92F, -6.82F, 12.320001F, 1.6700001F, 0.27F);
      return Shapes.blend(0.18F, Shapes.blend(0.3F, var3, var4), Shapes.union(var5), var11).paint(stick(-6.9F, 12.8F, 0.5F, -7.0F, 10.6F, 0.72F));
   }

   private static Shape leg() {
      Paint var0 = stick(-2.2F, 12.8F, 0.0F, -2.35F, 1.4F, 0.0F);
      Shape var1 = Shapes.sphere(-2.2F, 12.6F, 0.0F, 0.8F);
      Shape var2 = Shapes.cone(-2.2F, 12.4F, 0.0F, 0.74F, -2.3F, 7.0F, 0.12F, 0.62F);
      Shape var3 = Shapes.sphere(-2.3F, 6.75F, 0.16F, 0.68F);
      Shape var4 = Shapes.cone(-2.3F, 6.5F, 0.12F, 0.62F, -2.36F, 1.9F, 0.0F, 0.55F);
      return Shapes.blend(0.3F, var1, var2, var3, var4).paint(var0);
   }

   private static Shape foot() {
      Shape var0 = Shapes.blend(
         0.5F,
         Shapes.ellipsoid(-2.42F, 0.55F, 1.0F, 1.2F, 0.55F, 1.9F),
         Shapes.ellipsoid(-2.36F, 0.95F, -0.1F, 0.8F, 0.85F, 1.0F),
         Shapes.sphere(-2.36F, 1.75F, 0.0F, 0.55F)
      );
      Shape var1 = Shapes.union(
         Shapes.sphere(-1.7F, 0.38F, 2.55F, 0.4F),
         Shapes.sphere(-2.2F, 0.35F, 2.8F, 0.36F),
         Shapes.sphere(-2.68F, 0.34F, 2.76F, 0.33F),
         Shapes.sphere(-3.12F, 0.33F, 2.52F, 0.31F)
      );
      return Shapes.intersect(Shapes.blend(0.22F, var0, var1), Shapes.above(0.0F)).paint(stick(-2.4F, 0.6F, -1.0F, -2.4F, 0.6F, 3.0F));
   }

   private static Paint stick(float var0, float var1, float var2, float var3, float var4, float var5) {
      return Paint.wood(-3567528, -7053254, var0, var1, var2, var3 - var0, var4 - var1, var5 - var2, 0.35F).grain(0.3F, 0.05F);
   }

   private static Decal eyes() {
      Canvas var0 = new Canvas(336, 208);

      for (byte var1 = -1; var1 <= 1; var1 += 2) {
         float var2 = 168.0F + var1 * 85.5F;
         float var3 = 122.0F;
         var0.fill(Shape2D.circle(var2, var3, 75.0F), 808458248, 13.0F);
         var0.fill(Shape2D.circle(var2, var3, 66.0F), -15003638);
         var0.fill(Shape2D.circle(var2, var3, 60.75F), Paint2D.radial(var2 - 15.0F, var3 - 18.0F, 78.0F, -1, -132105, -1054240));
         float var4 = var2 - var1 * 1.5F;
         float var5 = var3 + 1.0F;
         var0.fill(Shape2D.circle(var4, var5, 12.0F), -15857658);
         var0.fill(Shape2D.circle(var4 - 3.9F, var5 - 4.4F, 3.6F), -285212673);
      }

      Shape2D var6 = Shape2D.curve(15.0F, 48.0F, 74.0F, 18.0F, 147.0F, 71.0F, 21.0F, 34.5F);
      var0.fill(var6, -15003638);
      var0.fill(var6.mirrorX(168.0F).rotate(6.0F, 321.0F, 48.0F).move(0.0F, -7.0F), -15003638);
      return Decal.front(var0, 0.0F, 29.175F, 5.0F, 8.4F, 5.2F).depth(3.6F, 1.5F).maxAngle(72.0F).flatLight(1.0F).occlusion(0.0F);
   }

   private static Decal mouth() {
      Canvas var0 = new Canvas(272, 104);
      Shape2D var1 = Shape2D.ellipse(136.0F, 19.0F, 124.5F, 78.0F).subtract(Shape2D.circle(136.0F, -482.0F, 507.0F));
      var0.fill(var1.inflate(6.0F), -15003638);
      var0.fill(var1, Paint2D.linear(136.0F, 10.0F, 136.0F, 95.5F, -15071227, -12970996));
      var0.fill(Shape2D.ellipse(136.0F, 98.5F, 75.0F, 39.0F).intersect(var1), -3646898);
      return Decal.front(var0, 0.0F, 24.675F, 5.0F, 6.8F, 2.6F).depth(3.6F, 1.5F).maxAngle(72.0F);
   }

   private static Decal rings() {
      Canvas var0 = new Canvas(168, 168);
      var0.fill(Shape2D.circle(84.0F, 84.0F, 72.0F), Paint2D.radial(81.0F, 86.0F, 72.0F, -3565470, -1917818, -1521774, -2709138), 4.0F);
      Shape2D var1 = Shape2D.field((var0x, var1x) -> {
         float var2 = var0x - 81.0F;
         float var3 = var1x - 86.0F;
         float var4 = (float)Math.sqrt(var2 * var2 + var3 * var3) + 0.001F;
         float var5 = var4 + 2.5F * Noise.noise(var2 / var4 * 1.3F, var3 / var4 * 1.3F, var4 * 0.028F);
         float var6 = var5 / 8.25F;
         float var7 = var6 - Math.round(var6);
         return Math.max(Math.max(Math.abs(var7) * 8.25F - 0.82F, var4 - 67.5F), 6.7F - var4);
      }, 12.0F, 17.0F, 150.0F, 155.0F);
      var0.fill(var1, -1602927328);
      var0.fill(Shape2D.circle(81.0F, 86.0F, 3.6F), -9813984);
      var0.fill(Shape2D.taper(87.0F, 81.0F, 3.0F, 133.0F, 52.0F, 1.0F), -1069538288);
      var0.fill(Shape2D.taper(77.0F, 91.0F, 2.2F, 47.0F, 126.0F, 1.0F), -1337973744);
      return Decal.top(var0, 0.0F, 34.0F, 0.0F, 9.678F, 9.678F).depth(0.6F, 0.6F).maxAngle(35.0F);
   }

   private static float smooth(float var0, float var1, float var2) {
      float var3 = Math.max(0.0F, Math.min(1.0F, (var2 - var0) / (var1 - var0)));
      return var3 * var3 * (3.0F - 2.0F * var3);
   }

   private static float[] unit(float var0, float var1, float var2) {
      float var3 = (float)Math.sqrt(var0 * var0 + var1 * var1 + var2 * var2);
      return new float[]{var0 / var3, var1 / var3, var2 / var3};
   }

   @Environment(EnvType.CLIENT)
   private static final class Bark extends Paint {
      private final Paint base;

      Bark(Paint var1) {
         this.base = var1;
      }

      @Override
      public void apply(float var1, float var2, float var3, Sample var4) {
         this.base.apply(var1, var2, var3, var4);
         float var5 = (float)Math.sqrt(var1 * var1 + var3 * var3);
         if (!(var5 < 0.5F)) {
            float var6 = TungTungSahur.smooth(3.95F, 4.7F, var5) * TungTungSahur.smooth(12.0F, 12.6F, var2) * (1.0F - TungTungSahur.smooth(33.4F, 34.0F, var2));
            float var7 = TungTungSahur.fissure(var1 / var5, var2, var3 / var5) * var6;
            if (var7 > 0.0F) {
               var4.toward(-9551070, Math.min(1.0F, var7 * 0.62F));
            }
         }
      }
   }
}
