package net.efn.client.util;

import com.example.client.mixin.AccessorGameRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public final class FoglessLines {
   private static final double NEAR = 0.05;
   private final Vec3d eye;
   private final Vector3fc forward;
   private final double pixelPerDepth;

   private FoglessLines(Vec3d var1, Vector3fc var2, double var3) {
      this.eye = var1;
      this.forward = var2;
      this.pixelPerDepth = var3;
   }

   public static FoglessLines forFrame(float var0) {
      Camera var1 = MinecraftClient.method_1551().field_1773.method_19418();
      return new FoglessLines(var1.method_71156(), var1.method_19335(), pixelScale(var0));
   }

   public static double pixelScale(float var0) {
      MinecraftClient var1 = MinecraftClient.method_1551();
      Camera var2 = var1.field_1773.method_19418();
      float var3 = ((AccessorGameRenderer)var1.field_1773).efn$getFov(var2, var0, true);
      return 2.0 * Math.tan(Math.toRadians(var3) * 0.5) / Math.max(1, var1.method_22683().method_4506());
   }

   public static FoglessLines forView(Vec3d var0, Vector3fc var1, double var2) {
      return new FoglessLines(var0, var1, var2);
   }

   public void line(Vec3d var1, Vec3d var2, int var3, float var4) {
      this.line(var1.field_1352, var1.field_1351, var1.field_1350, var2.field_1352, var2.field_1351, var2.field_1350, var3, var4);
   }

   public void line(double var1, double var3, double var5, double var7, double var9, double var11, int var13, float var14) {
      double var15 = this.depth(var1, var3, var5);
      double var17 = this.depth(var7, var9, var11);
      if (!(var15 < 0.05) || !(var17 < 0.05)) {
         if (var15 < 0.05) {
            double var19 = (0.05 - var15) / (var17 - var15);
            var1 += (var7 - var1) * var19;
            var3 += (var9 - var3) * var19;
            var5 += (var11 - var5) * var19;
            var15 = 0.05;
         } else if (var17 < 0.05) {
            double var45 = (0.05 - var17) / (var15 - var17);
            var7 += (var1 - var7) * var45;
            var9 += (var3 - var9) * var45;
            var11 += (var5 - var11) * var45;
            var17 = 0.05;
         }

         double var46 = var1 - this.eye.field_1352;
         double var21 = var3 - this.eye.field_1351;
         double var23 = var5 - this.eye.field_1350;
         double var25 = var7 - var1;
         double var27 = var9 - var3;
         double var29 = var11 - var5;
         double var31 = var21 * var29 - var23 * var27;
         double var33 = var23 * var25 - var46 * var29;
         double var35 = var46 * var27 - var21 * var25;
         double var37 = Math.sqrt(var31 * var31 + var33 * var33 + var35 * var35);
         if (!(var37 < 1.0E-9)) {
            double var39 = var14 * 0.5 * this.pixelPerDepth / var37;
            double var41 = var39 * var15;
            double var43 = var39 * var17;
            WorldQuads.layer(true)
               .quad(
                  var1 + var31 * var41,
                  var3 + var33 * var41,
                  var5 + var35 * var41,
                  var7 + var31 * var43,
                  var9 + var33 * var43,
                  var11 + var35 * var43,
                  var7 - var31 * var43,
                  var9 - var33 * var43,
                  var11 - var35 * var43,
                  var1 - var31 * var41,
                  var3 - var33 * var41,
                  var5 - var35 * var41,
                  var13
               );
         }
      }
   }

   public void box(Box var1, int var2, float var3) {
      double var4 = var1.field_1323;
      double var6 = var1.field_1322;
      double var8 = var1.field_1321;
      double var10 = var1.field_1320;
      double var12 = var1.field_1325;
      double var14 = var1.field_1324;

      for (double var19 : new double[]{var6, var12}) {
         this.line(new Vec3d(var4, var19, var8), new Vec3d(var10, var19, var8), var2, var3);
         this.line(new Vec3d(var10, var19, var8), new Vec3d(var10, var19, var14), var2, var3);
         this.line(new Vec3d(var10, var19, var14), new Vec3d(var4, var19, var14), var2, var3);
         this.line(new Vec3d(var4, var19, var14), new Vec3d(var4, var19, var8), var2, var3);
      }

      this.line(new Vec3d(var4, var6, var8), new Vec3d(var4, var12, var8), var2, var3);
      this.line(new Vec3d(var10, var6, var8), new Vec3d(var10, var12, var8), var2, var3);
      this.line(new Vec3d(var10, var6, var14), new Vec3d(var10, var12, var14), var2, var3);
      this.line(new Vec3d(var4, var6, var14), new Vec3d(var4, var12, var14), var2, var3);
   }

   private double depth(double var1, double var3, double var5) {
      return (var1 - this.eye.field_1352) * this.forward.x()
         + (var3 - this.eye.field_1351) * this.forward.y()
         + (var5 - this.eye.field_1350) * this.forward.z();
   }
}
