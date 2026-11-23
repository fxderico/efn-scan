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
      String var1,
      KnifeSkin.Part[] var2,
      KnifeSkin.Streak[] var3,
      float var4,
      float var5,
      float var6,
      Vector3f var7,
      Vector3f var8,
      Vector3f var9,
      InspectAnimation.Clip var10,
      InspectAnimation.Clip var11
   ) {
      this.name = var1;
      this.parts = var2;
      this.streaks = var3;
      this.voxel = var4;
      this.gripX = var5;
      this.gripY = var6;
      this.fist = var7;
      this.inspect = var10;
      this.deploy = var11;
      Vector3f var12 = new Vector3f(var8).normalize();
      Vector3f var13 = new Vector3f(var9).fma(-var9.dot(var12), var12).normalize();
      this.rest = new Quaternionf().setFromNormalized(new Matrix3f(var12, var13, new Vector3f(var12).cross(var13)));
      var7.fma(0.6F, FirstPersonArm.FOREARM, this.elbow);
   }

   static KnifeSkin of(String var0) {
      return switch (var0) {
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
      var2.centre = var0x -> var0x < 3.0F ? 0.5F : KnifeModels.katanaCentre(var0x);
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

   float streakStrength(int var1) {
      return this.streaks[var1].strength;
   }

   void draw(MatrixStack var1, float[] var2, float var3, boolean var4, float var5, KnifeSkin.Sink var6, float[] var7) {
      Matrix4f var10 = this.knifeMatrix(var2, var3, var5, new Matrix4f());
      Vector3f var11 = new Vector3f(this.fist).add(var2[0] * var3, var2[1] * var3, var2[2] * var3);
      FirstPersonArm.place(var1, var11, this.elbow, var4, var1x -> var6.arm(var1x, false));
      Matrix4f var12 = this.scabbard != null && var3 >= 0.999F && var2[13] > 0.01F ? this.scabbardMatrix(var2, var5) : null;
      Vector3f var13 = null;
      if (var12 != null) {
         var13 = var12.transformPosition(this.saddleX, this.saddleY, 0.5F, new Vector3f());
      } else if (this.centre != null && var2[14] * var3 > 0.01F) {
         float var14 = MathHelper.method_15363(var2[14] * var3, 0.0F, 1.0F);
         float var15 = var2[15];
         float var16 = this.centre.y(var15) + (var15 >= this.bladeFrom ? 3.0F : 0.0F);
         var13 = var10.transformPosition(var15, var16, 0.5F, new Vector3f()).fma(1.0F - var14 * var14 * (3.0F - 2.0F * var14), OFF_AWAY);
      }

      if (var13 != null) {
         var1.method_22903();
         var1.method_22905(-1.0F, 1.0F, 1.0F);
         Vector3f var17 = new Vector3f(-var13.x, var13.y, var13.z);
         FirstPersonArm.place(var1, var17, new Vector3f(var17).fma(0.6F, OFF_FOREARM), var4, var1x -> var6.arm(var1x, true));
         var1.method_22909();
      }

      KnifeMesh.Shine var8;
      KnifeMesh.Shine var9;
      if ((var9 = var8 = this.wipe(var2, var3)) == null) {
         float var18 = shinePosition(var2[10]);
         var9 = new KnifeMesh.Shine(var18, var18 > 0.0F ? MathHelper.method_15374(3.1415927F * var18) * var3 : 0.0F, 0.0F);
      }

      var1.method_22903();
      var1.method_34425(var10);

      for (int var19 = 0; var19 < this.parts.length; var19++) {
         KnifeSkin.Part var21 = this.parts[var19];
         var1.method_22903();
         float var20;
         if (var21.channel >= 0 && (var20 = MathHelper.method_15393(var2[var21.channel]) * var3) != 0.0F) {
            var1.method_49278(RotationAxis.field_40718.rotationDegrees(var20), var21.pivotX, var21.pivotY, 0.5F);
         }

         var6.part(var1, var21.mesh, var9);
         if (var8 != null) {
            var6.glow(var1, var21.mesh, var8);
         }

         if (var7 != null) {
            this.streaks(var19, var1.method_23760().method_23761(), var7);
         }

         var1.method_22909();
      }

      var1.method_22909();
      if (var12 != null) {
         var1.method_22903();
         var1.method_34425(var12);
         var6.part(var1, this.scabbard, KnifeMesh.Shine.NONE);
         var1.method_22909();
      }
   }

   private void streaks(int var1, Matrix4f var2, float[] var3) {
      Vector3f var4 = new Vector3f();

      for (int var5 = 0; var5 < this.streaks.length; var5++) {
         KnifeSkin.Streak var6 = this.streaks[var5];
         if (var6.part == var1) {
            var2.transformPosition(var6.baseX, var6.baseY, 0.5F, var4);
            var3[var5 * 6] = var4.x;
            var3[var5 * 6 + 1] = var4.y;
            var3[var5 * 6 + 2] = var4.z;
            var2.transformPosition(var6.tipX, var6.tipY, 0.5F, var4);
            var3[var5 * 6 + 3] = var4.x;
            var3[var5 * 6 + 4] = var4.y;
            var3[var5 * 6 + 5] = var4.z;
         }
      }
   }

   private Matrix4f knifeMatrix(float[] var1, float var2, float var3, Matrix4f var4) {
      float var5 = 0.017453292F * var2;
      float var6 = this.voxel * var3;
      var4.translation(this.fist.x + var1[0] * var2, this.fist.y + var1[1] * var2, this.fist.z + var1[2] * var2)
         .rotate(
            new Quaternionf()
               .rotateY(MathHelper.method_15393(var1[3]) * var5)
               .rotateX(MathHelper.method_15393(var1[4]) * var5)
               .rotateZ(MathHelper.method_15393(var1[5]) * var5)
         )
         .rotate(this.rest);
      float var7 = MathHelper.method_15393(var1[6]) * var5;
      if (var7 != 0.0F) {
         var4.rotateX(var7);
      }

      return var4.scale(var6).translate(var1[11] * var2 - this.gripX, -this.gripY, -0.5F);
   }

   private Matrix4f scabbardMatrix(float[] var1, float var2) {
      boolean var3 = var1[13] >= 1.0F;
      float[] var4 = var3 ? var1 : this.letGo;
      float var5 = 312.5F;
      Matrix4f var6 = this.knifeMatrix(var4, 1.0F, var2, new Matrix4f())
         .rotateAround(new Quaternionf().rotationZ(var4[12] / var5), 3.0F, var5, 0.5F)
         .translate(0.05F, 0.0F, 0.0F);
      if (!var3) {
         float var7 = 1.0F - Math.max(var1[13], 0.0F);
         var7 *= var7;
         Vector3f var8 = var6.transformPosition(this.saddleX, this.saddleY, 0.5F, new Vector3f());
         var6.translateLocal(-var8.x, -var8.y, -var8.z)
            .rotateLocalZ(0.6981317F * var7)
            .translateLocal(var8.x + SCABBARD_FALL.x * var7, var8.y + SCABBARD_FALL.y * var7, var8.z + SCABBARD_FALL.z * var7);
      }

      return var6;
   }

   private KnifeMesh.Shine wipe(float[] var1, float var2) {
      float var3 = this.centre != null ? MathHelper.method_15363((var1[14] * var2 - 0.7F) / 0.3F, 0.0F, 1.0F) : 0.0F;
      if (!(var3 <= 0.01F) && !(var1[15] < this.bladeFrom)) {
         float var5 = (var1[15] - this.bladeFrom) / (this.bladeTo - this.bladeFrom);
         return new KnifeMesh.Shine(var5, var3 * var3 * (3.0F - 2.0F * var3), 16.0F);
      } else {
         return null;
      }
   }

   float sparkle(float[] var1, float var2) {
      float var3 = sparkle(var1[10]);
      if (this.centre != null && var1[15] >= this.bladeFrom) {
         float var4 = (var1[15] - this.bladeFrom) / (this.bladeTo - this.bladeFrom);
         float var5 = MathHelper.method_15363((var4 - 0.96F) / 0.06F, 0.0F, 1.0F) * MathHelper.method_15363((1.3F - var4) / 0.15F, 0.0F, 1.0F);
         var3 = Math.max(var3, var5 * MathHelper.method_15363(var1[14] * 1.6F, 0.0F, 1.0F));
      }

      return var3 * var2;
   }

   static float shinePosition(float var0) {
      if (var0 <= 0.0F) {
         return 0.0F;
      } else {
         float var1 = var0 - 2.0F * MathHelper.method_15375(var0 / 2.0F);
         return var1 < 1.0F ? var1 : 0.0F;
      }
   }

   static float sparkle(float var0) {
      float var1 = shinePosition(var0);
      return MathHelper.method_15363((var1 - 0.72F) / 0.16F, 0.0F, 1.0F) * MathHelper.method_15363((1.0F - var1) / 0.07F, 0.0F, 1.0F);
   }

   @Environment(EnvType.CLIENT)
   private interface Centre {
      float y(float var1);
   }

   @Environment(EnvType.CLIENT)
   private record Part(KnifeMesh mesh, int channel, float pivotX, float pivotY) {
   }

   @Environment(EnvType.CLIENT)
   interface Sink {
      void part(MatrixStack var1, KnifeMesh var2, KnifeMesh.Shine var3);

      void glow(MatrixStack var1, KnifeMesh var2, KnifeMesh.Shine var3);

      void arm(MatrixStack var1, boolean var2);
   }

   @Environment(EnvType.CLIENT)
   record Streak(int part, float baseX, float baseY, float tipX, float tipY, float strength) {
   }
}
