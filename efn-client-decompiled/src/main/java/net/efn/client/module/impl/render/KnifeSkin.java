package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class KnifeSkin {
   static final String NONE = "None";
   static final String BUTTERFLY = "Butterfly";
   static final String KARAMBIT = "Karambit";
   static final String KATANA = "Katana";
   static final int MAX_STREAKS = 2;
   private static final Vector3f OFF_FOREARM = new Vector3f(0.4F, -0.85F, 0.35F).normalize();
   private static final Vector3f OFF_AWAY = new Vector3f(-0.3F, -0.75F, 0.1F);
   private static final float WIPE_LIFT = 3.0F;
   private static final float WIPE_TAIL = 16.0F;
   private static final Vector3f SCABBARD_FALL = new Vector3f(-0.3F, -0.9F, 0.25F);
   private static final float SCABBARD_TURN = 0.6981317F;
   private static final float[][] BUTTERFLY_INSPECT = new float[][]{
      {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {0.08F, -0.07F, 0.09F, 0.05F, -8.0F, 8.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 8.0F},
      {0.14F, -0.09F, 0.12F, 0.06F, -12.0F, 8.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 12.0F},
      {0.19F, -0.09F, 0.12F, 0.06F, -10.0F, 10.0F, 2.0F, -18.0F, 0.0F, 90.0F, 180.0F, 0.0F, 12.0F},
      {0.24F, -0.09F, 0.13F, 0.06F, -12.0F, 8.0F, 10.0F, 0.0F, 0.0F, 180.0F, 360.0F, 0.0F, 12.0F},
      {0.29F, -0.09F, 0.12F, 0.06F, -10.0F, 10.0F, 2.0F, 18.0F, 0.0F, 270.0F, 540.0F, 0.0F, 12.0F},
      {0.34F, -0.09F, 0.12F, 0.06F, -12.0F, 8.0F, 14.0F, 0.0F, 0.0F, 360.0F, 720.0F, 0.0F, 12.0F},
      {0.39F, -0.09F, 0.12F, 0.06F, -10.0F, 10.0F, 2.0F, -18.0F, 0.0F, 450.0F, 900.0F, 0.0F, 12.0F},
      {0.44F, -0.09F, 0.13F, 0.06F, -12.0F, 8.0F, 10.0F, 0.0F, 0.0F, 540.0F, 1080.0F, 0.0F, 12.0F},
      {0.49F, -0.09F, 0.12F, 0.06F, -10.0F, 10.0F, 2.0F, 18.0F, 0.0F, 630.0F, 1260.0F, 0.0F, 12.0F},
      {0.54F, -0.09F, 0.12F, 0.06F, -12.0F, 8.0F, 14.0F, 0.0F, 0.0F, 720.0F, 1440.0F, 0.0F, 12.0F},
      {0.62F, -0.1F, 0.12F, 0.07F, -35.0F, 6.0F, 20.0F, 0.0F, 0.0F, 720.0F, 1440.0F, 0.0F, 0.0F},
      {0.76F, -0.1F, 0.13F, 0.07F, 25.0F, 6.0F, 16.0F, 0.0F, 0.0F, 720.0F, 1440.0F, 1.0F, 0.0F},
      {0.88F, -0.05F, 0.06F, 0.03F, 8.0F, 3.0F, 6.0F, 0.0F, 0.0F, 720.0F, 1440.0F, 1.0F, 0.0F},
      {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 720.0F, 1440.0F, 1.0F, 0.0F}
   };
   private static final float[][] BUTTERFLY_DEPLOY = new float[][]{
      {0.0F, 0.02F, -0.14F, 0.02F, 0.0F, -30.0F, -20.0F, 0.0F, 0.0F, 180.0F, 0.0F, 0.0F, 10.0F},
      {0.3F, 0.01F, -0.04F, 0.01F, 0.0F, -10.0F, -10.0F, 0.0F, 0.0F, 220.0F, 100.0F, 0.0F, 10.0F},
      {0.62F, 0.0F, 0.01F, 0.0F, 0.0F, 4.0F, 6.0F, 0.0F, 0.0F, 350.0F, 340.0F, 0.0F, 3.0F},
      {0.8F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, -2.0F, 0.0F, 0.0F, 365.0F, 368.0F, 0.0F, 0.0F},
      {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 360.0F, 0.0F, 0.0F}
   };
   private static final float[][] KARAMBIT_INSPECT = new float[][]{
      {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {0.1F, -0.12F, 0.14F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, -25.0F, 0.0F, 0.0F},
      {0.14F, -0.14F, 0.16F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
      {0.205F, -0.14F, 0.17F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, 180.0F, 0.0F, 0.0F},
      {0.27F, -0.14F, 0.16F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, 360.0F, 0.0F, 0.0F},
      {0.335F, -0.14F, 0.17F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, 540.0F, 0.0F, 0.0F},
      {0.4F, -0.14F, 0.16F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, 720.0F, 0.0F, 0.0F},
      {0.48F, -0.13F, 0.16F, 0.06F, -10.0F, 12.0F, 10.0F, 0.0F, 0.0F, 750.0F, 0.0F, 0.0F},
      {0.56F, -0.1F, 0.12F, 0.06F, 20.0F, 10.0F, -70.0F, 0.0F, 0.0F, 720.0F, 0.0F, 0.0F},
      {0.72F, -0.1F, 0.13F, 0.06F, -10.0F, 10.0F, -60.0F, 0.0F, 0.0F, 720.0F, 0.0F, 1.0F},
      {0.86F, -0.06F, 0.08F, 0.05F, -6.0F, 4.0F, 8.0F, 0.0F, 0.0F, 360.0F, 0.0F, 1.0F},
      {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F, 1.0F}
   };
   private static final float[][] KARAMBIT_DEPLOY = new float[][]{
      {0.0F, 0.02F, -0.14F, 0.02F, 0.0F, -30.0F, -20.0F, 0.0F, 0.0F, -540.0F, 0.0F, 0.0F},
      {0.55F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 4.0F, 0.0F, 0.0F, -60.0F, 0.0F, 0.0F},
      {0.78F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, -2.0F, 0.0F, 0.0F, 12.0F, 0.0F, 0.0F},
      {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}
   };
   private static final float[][] KATANA_INSPECT = new float[][]{
      {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F},
      {0.1F, -0.06F, 0.12F, 0.06F, 0.0F, 0.0F, 60.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.6F, 4.0F},
      {0.15F, -0.07F, 0.14F, 0.08F, -2.0F, 0.0F, 60.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 4.0F},
      {0.2F, -0.07F, 0.14F, 0.08F, -2.0F, 0.0F, 60.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 12.0F},
      {0.36F, -0.07F, 0.14F, 0.08F, 0.0F, 0.0F, 58.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 44.0F},
      {0.4F, -0.07F, 0.14F, 0.08F, 0.0F, 0.0F, 58.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 53.0F},
      {0.44F, -0.06F, 0.13F, 0.06F, 2.0F, 1.0F, 54.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.6F, 58.0F},
      {0.48F, -0.05F, 0.11F, 0.03F, 4.0F, 2.0F, 42.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 62.0F},
      {0.52F, -0.04F, 0.1F, 0.02F, 0.0F, 4.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 62.0F},
      {0.6F, -0.04F, 0.12F, 0.02F, 0.0F, 4.0F, -160.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 62.0F},
      {0.68F, -0.04F, 0.1F, 0.02F, 0.0F, 4.0F, -340.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 62.0F},
      {0.78F, 0.04F, -0.03F, 0.0F, 0.0F, -6.0F, -400.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 62.0F},
      {0.88F, 0.0F, 0.02F, 0.0F, 0.0F, 2.0F, -365.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 62.0F},
      {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -360.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 62.0F}
   };
   private static final float[][] KATANA_DEPLOY = new float[][]{
      {0.0F, 0.06F, 0.0F, 0.02F, -10.0F, 0.0F, 45.0F, 180.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F},
      {0.05F, 0.05F, 0.01F, 0.01F, -12.0F, 0.0F, 47.0F, 180.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 0.0F},
      {0.18F, 0.15F, 0.0F, 0.05F, -10.0F, 0.0F, 45.0F, 180.0F, 0.0F, 0.0F, 0.0F, 0.3F, 0.0F, 16.0F, 1.0F, 0.0F},
      {0.3F, 0.3F, -0.02F, 0.1F, -8.0F, 0.0F, 43.0F, 180.0F, 0.0F, 0.0F, 0.0F, 0.9F, 0.0F, 50.0F, 1.0F, 0.0F},
      {0.33F, 0.3F, 0.0F, 0.1F, -4.0F, 4.0F, 36.0F, 180.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 52.0F, 1.0F, 0.0F},
      {0.41F, 0.08F, 0.12F, 0.04F, 0.0F, 10.0F, -40.0F, 180.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 52.0F, 0.25F, 0.0F},
      {0.47F, -0.02F, 0.1F, 0.02F, 5.0F, 8.0F, -100.0F, 180.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 52.0F, 0.0F, 0.0F},
      {0.62F, -0.02F, 0.08F, 0.02F, 0.0F, 6.0F, -460.0F, 180.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 52.0F, 0.0F, 0.0F},
      {0.71F, 0.04F, -0.04F, 0.0F, 0.0F, -6.0F, -520.0F, 180.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 52.0F, 0.0F, 0.0F},
      {0.82F, 0.0F, 0.02F, 0.0F, 0.0F, 2.0F, -370.0F, 300.0F, 0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 52.0F, 0.0F, 0.0F},
      {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -360.0F, 360.0F, 0.0F, 0.0F, 0.0F, 3.0F, 0.0F, 52.0F, 0.0F, 0.0F}
   };
   private static KnifeSkin butterfly;
   private static KnifeSkin karambit;
   private static KnifeSkin katana;
   final String name;
   final InspectAnimation.Clip inspect;
   final InspectAnimation.Clip deploy;
   private final KnifeSkin.Part[] parts;
   private final KnifeSkin.Streak[] streaks;
   private final float voxel;
   private final float gripX;
   private final float gripY;
   private final Vector3f fist;
   private final Vector3f elbow = new Vector3f();
   private final Quaternionf rest;
   private KnifeMesh scabbard;
   private float saddleX;
   private float saddleY;
   private float[] letGo;
   private KnifeSkin.Centre centre;
   private float bladeFrom;
   private float bladeTo;

   private KnifeSkin(
      String string,
      KnifeSkin.Part[] parts,
      KnifeSkin.Streak[] streaks,
      float f,
      float g,
      float h,
      Vector3f vector3f,
      Vector3f vector3f2,
      Vector3f vector3f3,
      InspectAnimation.Clip clip,
      InspectAnimation.Clip clip2
   ) {
      this.name = string;
      this.parts = parts;
      this.streaks = streaks;
      this.voxel = f;
      this.gripX = g;
      this.gripY = h;
      this.fist = vector3f;
      this.inspect = clip;
      this.deploy = clip2;
      Vector3f var12 = new Vector3f(vector3f2).normalize();
      Vector3f var13 = new Vector3f(vector3f3).fma(-vector3f3.dot(var12), var12).normalize();
      this.rest = new Quaternionf().setFromNormalized(new Matrix3f(var12, var13, new Vector3f(var12).cross(var13)));
      vector3f.fma(0.6F, FirstPersonArm.FOREARM, this.elbow);
   }

   static KnifeSkin of(String string) {
      return switch (string) {
         case "Butterfly" -> butterfly != null ? butterfly : (butterfly = butterfly());
         case "Karambit" -> karambit != null ? karambit : (karambit = karambit());
         case "Katana" -> katana != null ? katana : (katana = katana());
         default -> null;
      };
   }

   private static KnifeSkin butterfly() {
      float var0 = -1.5F;
      float var1 = 0.5F;
      KnifeSkin.Part[] var2 = new KnifeSkin.Part[]{
         new KnifeSkin.Part(KnifeModels.butterflyHandle(1), -1, var0, var1),
         new KnifeSkin.Part(KnifeModels.butterflyBlade(), 8, var0, var1),
         new KnifeSkin.Part(KnifeModels.butterflyHandle(-1), 9, var0, var1)
      };
      KnifeSkin.Streak[] var3 = new KnifeSkin.Streak[]{
         new KnifeSkin.Streak(1, 2.5F, 0.5F, 21.5F, -0.5F, 1.0F), new KnifeSkin.Streak(2, -4.0F, 0.5F, -27.5F, 0.5F, 0.8F)
      };
      return new KnifeSkin(
         "Butterfly",
         var2,
         var3,
         0.021F,
         -10.0F,
         0.5F,
         new Vector3f(-0.06F, 0.06F, 0.02F),
         new Vector3f(-0.15F, 1.0F, -0.1F),
         new Vector3f(1.0F, 0.15F, 0.3F),
         new InspectAnimation.Clip(BUTTERFLY_INSPECT, 3.8F, 0.14F, 0.54F),
         new InspectAnimation.Clip(BUTTERFLY_DEPLOY, 0.9F)
      );
   }

   private static KnifeSkin karambit() {
      float var0 = -16.0F;
      float var1 = -1.0F;
      KnifeSkin.Part[] var2 = new KnifeSkin.Part[]{new KnifeSkin.Part(KnifeModels.karambit(), 8, var0, var1)};
      float[] var3 = KnifeModels.KARAMBIT_TIP;
      byte var4 = 2;
      KnifeSkin.Streak[] var5 = new KnifeSkin.Streak[]{new KnifeSkin.Streak(0, 6.0F * var4, 0.5F * var4, var3[0], var3[1], 1.0F)};
      return new KnifeSkin(
         "Karambit",
         var2,
         var5,
         0.022F / var4,
         var0 + 2.0F,
         var1,
         new Vector3f(-0.06F, 0.1F, 0.02F),
         new Vector3f(-0.5F, -0.8F, -0.3F),
         new Vector3f(0.8F, -0.5F, 0.35F),
         new InspectAnimation.Clip(KARAMBIT_INSPECT, 3.2F, 0.14F, 0.4F),
         new InspectAnimation.Clip(KARAMBIT_DEPLOY, 0.8F)
      );
   }

   private static KnifeSkin katana() {
      KnifeSkin.Part[] var0 = new KnifeSkin.Part[]{new KnifeSkin.Part(KnifeModels.katana(), -1, 0.0F, 0.0F)};
      KnifeSkin.Streak[] var1 = new KnifeSkin.Streak[]{
         new KnifeSkin.Streak(0, 6.0F, KnifeModels.katanaBow(6) + 0.5F, 48.5F, KnifeModels.katanaBow(48) + 1.5F, 1.0F)
      };
      KnifeSkin var2 = new KnifeSkin(
         "Katana",
         var0,
         var1,
         0.028F,
         -5.0F,
         0.5F,
         new Vector3f(-0.06F, 0.02F, 0.02F),
         new Vector3f(-0.6F, 0.8F, -0.2F),
         new Vector3f(0.6F, 0.45F, 0.1F),
         new InspectAnimation.Clip(KATANA_INSPECT, 4.4F, 0.52F, 0.68F),
         new InspectAnimation.Clip(KATANA_DEPLOY, 2.0F)
      );
      var2.scabbard = KnifeModels.katanaScabbard();
      var2.saddleX = 20.0F;
      var2.saddleY = 0.0F;
      var2.centre = f -> f < 3.0F ? 0.5F : KnifeModels.katanaCentre(f);
      var2.bladeFrom = 3.0F;
      var2.bladeTo = 49.0F;
      var2.letGo = var2.findLetGo();
      return var2;
   }

   private float[] findLetGo() {
      float[] var1 = new float[16];
      float[] var2 = new float[16];

      for (int var3 = 0; var3 <= 2000; var3++) {
         this.deploy.sample(var3 / 2000.0F, var1);
         if (var1[13] < 1.0F) {
            break;
         }

         System.arraycopy(var1, 0, var2, 0, 16);
      }

      return var2;
   }

   int streakCount() {
      return this.streaks.length;
   }

   float streakStrength(int i) {
      return this.streaks[i].strength;
   }

   void draw(MatrixStack matrixStack, float[] fs, float f, boolean bl, float g, KnifeSkin.Sink sink, float[] gs) {
      Matrix4f var10 = this.knifeMatrix(fs, f, g, new Matrix4f());
      Vector3f var11 = new Vector3f(this.fist).add(fs[0] * f, fs[1] * f, fs[2] * f);
      FirstPersonArm.place(matrixStack, var11, this.elbow, bl, matrixStackx -> sink.arm(matrixStackx, false));
      Matrix4f var12 = this.scabbard != null && f >= 0.999F && fs[13] > 0.01F ? this.scabbardMatrix(fs, g) : null;
      Vector3f var13 = null;
      if (var12 != null) {
         var13 = var12.transformPosition(this.saddleX, this.saddleY, 0.5F, new Vector3f());
      } else if (this.centre != null && fs[14] * f > 0.01F) {
         float var14 = MathHelper.clamp(fs[14] * f, 0.0F, 1.0F);
         float var15 = fs[15];
         float var16 = this.centre.y(var15) + (var15 >= this.bladeFrom ? 3.0F : 0.0F);
         var13 = var10.transformPosition(var15, var16, 0.5F, new Vector3f()).fma(1.0F - var14 * var14 * (3.0F - 2.0F * var14), OFF_AWAY);
      }

      if (var13 != null) {
         matrixStack.push();
         matrixStack.scale(-1.0F, 1.0F, 1.0F);
         Vector3f var17 = new Vector3f(-var13.x, var13.y, var13.z);
         FirstPersonArm.place(matrixStack, var17, new Vector3f(var17).fma(0.6F, OFF_FOREARM), bl, matrixStackx -> sink.arm(matrixStackx, true));
         matrixStack.pop();
      }

      KnifeMesh.Shine var8;
      KnifeMesh.Shine var9;
      if ((var9 = var8 = this.wipe(fs, f)) == null) {
         float var18 = shinePosition(fs[10]);
         var9 = new KnifeMesh.Shine(var18, var18 > 0.0F ? MathHelper.sin(3.1415927F * var18) * f : 0.0F, 0.0F);
      }

      matrixStack.push();
      matrixStack.multiplyPositionMatrix(var10);

      for (int var19 = 0; var19 < this.parts.length; var19++) {
         KnifeSkin.Part var21 = this.parts[var19];
         matrixStack.push();
         float var20;
         if (var21.channel >= 0 && (var20 = MathHelper.wrapDegrees(fs[var21.channel]) * f) != 0.0F) {
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(var20), var21.pivotX, var21.pivotY, 0.5F);
         }

         sink.part(matrixStack, var21.mesh, var9);
         if (var8 != null) {
            sink.glow(matrixStack, var21.mesh, var8);
         }

         if (gs != null) {
            this.streaks(var19, matrixStack.peek().getPositionMatrix(), gs);
         }

         matrixStack.pop();
      }

      matrixStack.pop();
      if (var12 != null) {
         matrixStack.push();
         matrixStack.multiplyPositionMatrix(var12);
         sink.part(matrixStack, this.scabbard, KnifeMesh.Shine.NONE);
         matrixStack.pop();
      }
   }

   private void streaks(int i, Matrix4f matrix4f, float[] fs) {
      Vector3f var4 = new Vector3f();

      for (int var5 = 0; var5 < this.streaks.length; var5++) {
         KnifeSkin.Streak var6 = this.streaks[var5];
         if (var6.part == i) {
            matrix4f.transformPosition(var6.baseX, var6.baseY, 0.5F, var4);
            fs[var5 * 6] = var4.x;
            fs[var5 * 6 + 1] = var4.y;
            fs[var5 * 6 + 2] = var4.z;
            matrix4f.transformPosition(var6.tipX, var6.tipY, 0.5F, var4);
            fs[var5 * 6 + 3] = var4.x;
            fs[var5 * 6 + 4] = var4.y;
            fs[var5 * 6 + 5] = var4.z;
         }
      }
   }

   private Matrix4f knifeMatrix(float[] fs, float f, float g, Matrix4f matrix4f) {
      float var5 = 0.017453292F * f;
      float var6 = this.voxel * g;
      matrix4f.translation(this.fist.x + fs[0] * f, this.fist.y + fs[1] * f, this.fist.z + fs[2] * f)
         .rotate(
            new Quaternionf()
               .rotateY(MathHelper.wrapDegrees(fs[3]) * var5)
               .rotateX(MathHelper.wrapDegrees(fs[4]) * var5)
               .rotateZ(MathHelper.wrapDegrees(fs[5]) * var5)
         )
         .rotate(this.rest);
      float var7 = MathHelper.wrapDegrees(fs[6]) * var5;
      if (var7 != 0.0F) {
         matrix4f.rotateX(var7);
      }

      return matrix4f.scale(var6).translate(fs[11] * f - this.gripX, -this.gripY, -0.5F);
   }

   private Matrix4f scabbardMatrix(float[] fs, float f) {
      boolean var3 = fs[13] >= 1.0F;
      float[] var4 = var3 ? fs : this.letGo;
      float var5 = 312.5F;
      Matrix4f var6 = this.knifeMatrix(var4, 1.0F, f, new Matrix4f())
         .rotateAround(new Quaternionf().rotationZ(var4[12] / var5), 3.0F, var5, 0.5F)
         .translate(0.05F, 0.0F, 0.0F);
      if (!var3) {
         float var7 = 1.0F - Math.max(fs[13], 0.0F);
         var7 *= var7;
         Vector3f var8 = var6.transformPosition(this.saddleX, this.saddleY, 0.5F, new Vector3f());
         var6.translateLocal(-var8.x, -var8.y, -var8.z)
            .rotateLocalZ(0.6981317F * var7)
            .translateLocal(var8.x + SCABBARD_FALL.x * var7, var8.y + SCABBARD_FALL.y * var7, var8.z + SCABBARD_FALL.z * var7);
      }

      return var6;
   }

   private KnifeMesh.Shine wipe(float[] fs, float f) {
      float var3 = this.centre != null ? MathHelper.clamp((fs[14] * f - 0.7F) / 0.3F, 0.0F, 1.0F) : 0.0F;
      if (!(var3 <= 0.01F) && !(fs[15] < this.bladeFrom)) {
         float var5 = (fs[15] - this.bladeFrom) / (this.bladeTo - this.bladeFrom);
         return new KnifeMesh.Shine(var5, var3 * var3 * (3.0F - 2.0F * var3), 16.0F);
      } else {
         return null;
      }
   }

   float sparkle(float[] fs, float f) {
      float var3 = sparkle(fs[10]);
      if (this.centre != null && fs[15] >= this.bladeFrom) {
         float var4 = (fs[15] - this.bladeFrom) / (this.bladeTo - this.bladeFrom);
         float var5 = MathHelper.clamp((var4 - 0.96F) / 0.06F, 0.0F, 1.0F) * MathHelper.clamp((1.3F - var4) / 0.15F, 0.0F, 1.0F);
         var3 = Math.max(var3, var5 * MathHelper.clamp(fs[14] * 1.6F, 0.0F, 1.0F));
      }

      return var3 * f;
   }

   static float shinePosition(float f) {
      if (f <= 0.0F) {
         return 0.0F;
      } else {
         float var1 = f - 2.0F * MathHelper.floor(f / 2.0F);
         return var1 < 1.0F ? var1 : 0.0F;
      }
   }

   static float sparkle(float f) {
      float var1 = shinePosition(f);
      return MathHelper.clamp((var1 - 0.72F) / 0.16F, 0.0F, 1.0F) * MathHelper.clamp((1.0F - var1) / 0.07F, 0.0F, 1.0F);
   }

   @Environment(EnvType.CLIENT)
   private interface Centre {
      float y(float f);
   }

   @Environment(EnvType.CLIENT)
   private record Part(KnifeMesh mesh, int channel, float pivotX, float pivotY) {
   }

   @Environment(EnvType.CLIENT)
   interface Sink {
      void part(MatrixStack matrixStack, KnifeMesh knifeMesh, KnifeMesh.Shine shine);

      void glow(MatrixStack matrixStack, KnifeMesh knifeMesh, KnifeMesh.Shine shine);

      void arm(MatrixStack matrixStack, boolean bl);
   }

   @Environment(EnvType.CLIENT)
   record Streak(int part, float baseX, float baseY, float tipX, float tipY, float strength) {
   }
}
