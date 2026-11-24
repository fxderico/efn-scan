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

   private FoglessLines(Vec3d vec3d, Vector3fc vector3fc, double d) {
      this.eye = vec3d;
      this.forward = vector3fc;
      this.pixelPerDepth = d;
   }

   public static FoglessLines forFrame(float f) {
      Camera var1 = MinecraftClient.getInstance().gameRenderer.getCamera();
      return new FoglessLines(var1.getCameraPos(), var1.getHorizontalPlane(), pixelScale(f));
   }

   public static double pixelScale(float f) {
      MinecraftClient var1 = MinecraftClient.getInstance();
      Camera var2 = var1.gameRenderer.getCamera();
      float var3 = ((AccessorGameRenderer)var1.gameRenderer).efn$getFov(var2, f, true);
      return 2.0 * Math.tan(Math.toRadians(var3) * 0.5) / Math.max(1, var1.getWindow().getFramebufferHeight());
   }

   public static FoglessLines forView(Vec3d vec3d, Vector3fc vector3fc, double d) {
      return new FoglessLines(vec3d, vector3fc, d);
   }

   public void line(Vec3d vec3d, Vec3d vec3d2, int i, float f) {
      this.line(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z, i, f);
   }

   public void line(double d, double e, double f, double g, double h, double i, int j, float k) {
      double var15 = this.depth(d, e, f);
      double var17 = this.depth(g, h, i);
      if (!(var15 < 0.05) || !(var17 < 0.05)) {
         if (var15 < 0.05) {
            double var19 = (0.05 - var15) / (var17 - var15);
            d += (g - d) * var19;
            e += (h - e) * var19;
            f += (i - f) * var19;
            var15 = 0.05;
         } else if (var17 < 0.05) {
            double var45 = (0.05 - var17) / (var15 - var17);
            g += (d - g) * var45;
            h += (e - h) * var45;
            i += (f - i) * var45;
            var17 = 0.05;
         }

         double var46 = d - this.eye.x;
         double var21 = e - this.eye.y;
         double var23 = f - this.eye.z;
         double var25 = g - d;
         double var27 = h - e;
         double var29 = i - f;
         double var31 = var21 * var29 - var23 * var27;
         double var33 = var23 * var25 - var46 * var29;
         double var35 = var46 * var27 - var21 * var25;
         double var37 = Math.sqrt(var31 * var31 + var33 * var33 + var35 * var35);
         if (!(var37 < 1.0E-9)) {
            double var39 = k * 0.5 * this.pixelPerDepth / var37;
            double var41 = var39 * var15;
            double var43 = var39 * var17;
            WorldQuads.layer(true)
               .quad(
                  d + var31 * var41,
                  e + var33 * var41,
                  f + var35 * var41,
                  g + var31 * var43,
                  h + var33 * var43,
                  i + var35 * var43,
                  g - var31 * var43,
                  h - var33 * var43,
                  i - var35 * var43,
                  d - var31 * var41,
                  e - var33 * var41,
                  f - var35 * var41,
                  j
               );
         }
      }
   }

   public void box(Box box, int i, float f) {
      double var4 = box.minX;
      double var6 = box.minY;
      double var8 = box.minZ;
      double var10 = box.maxX;
      double var12 = box.maxY;
      double var14 = box.maxZ;

      for (double var19 : new double[]{var6, var12}) {
         this.line(new Vec3d(var4, var19, var8), new Vec3d(var10, var19, var8), i, f);
         this.line(new Vec3d(var10, var19, var8), new Vec3d(var10, var19, var14), i, f);
         this.line(new Vec3d(var10, var19, var14), new Vec3d(var4, var19, var14), i, f);
         this.line(new Vec3d(var4, var19, var14), new Vec3d(var4, var19, var8), i, f);
      }

      this.line(new Vec3d(var4, var6, var8), new Vec3d(var4, var12, var8), i, f);
      this.line(new Vec3d(var10, var6, var8), new Vec3d(var10, var12, var8), i, f);
      this.line(new Vec3d(var10, var6, var14), new Vec3d(var10, var12, var14), i, f);
      this.line(new Vec3d(var4, var6, var14), new Vec3d(var4, var12, var14), i, f);
   }

   private double depth(double d, double e, double f) {
      return (d - this.eye.x) * this.forward.x() + (e - this.eye.y) * this.forward.y() + (f - this.eye.z) * this.forward.z();
   }
}
