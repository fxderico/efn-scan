package net.efn.client.util.glyph;

import java.util.Arrays;
import net.efn.client.util.WorldQuads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.Gizmo;
import net.minecraft.world.debug.gizmo.GizmoDrawer;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.VisibilityConfigurable;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public final class GlyphCanvas implements Gizmo {
   public static final int MAX_QUADS = 12000;
   private static final int MAX_LINES = 4000;
   private static final int MIN_ALPHA = 3;
   private static final int VERTEX_BITS = 9;
   private static final double RIBBON_NEAR = 0.05;
   private final Vec3d[] vertices = new Vec3d[512];
   private double[] quadPos = new double[3072];
   private int[] quadColor = new int[256];
   private int quads;
   private double[] linePos = new double[384];
   private int[] lineColor = new int[64];
   private float[] lineWidth = new float[64];
   private int lines;
   private double camX;
   private double camY;
   private double camZ;
   private double rightX;
   private double rightY;
   private double rightZ;
   private double upX;
   private double upY;
   private double upZ;
   private double lookX;
   private double lookY;
   private double lookZ;

   public GlyphCanvas() {
   }

   public void begin(Camera camera) {
      this.quads = 0;
      this.lines = 0;
      Vec3d var2 = camera.getCameraPos();
      this.camX = var2.x;
      this.camY = var2.y;
      this.camZ = var2.z;
      Vector3fc var3 = camera.getDiagonalPlane();
      Vector3fc var4 = camera.getVerticalPlane();
      Vector3fc var5 = camera.getHorizontalPlane();
      this.rightX = -var3.x();
      this.rightY = -var3.y();
      this.rightZ = -var3.z();
      this.upX = var4.x();
      this.upY = var4.y();
      this.upZ = var4.z();
      this.lookX = var5.x();
      this.lookY = var5.y();
      this.lookZ = var5.z();
   }

   public void submit(boolean bl) {
      if (this.quads > 0) {
         WorldQuads.layer(bl).quads(this.quadPos, this.quadColor, this.quads);
      }

      this.quads = 0;
      if (this.lines != 0) {
         VisibilityConfigurable var2 = GizmoDrawing.collect(this);
         if (bl) {
            var2.ignoreOcclusion();
         }
      }
   }

   public double camX() {
      return this.camX;
   }

   public double camY() {
      return this.camY;
   }

   public double camZ() {
      return this.camZ;
   }

   public double depth(double d, double e, double f) {
      return (d - this.camX) * this.lookX + (e - this.camY) * this.lookY + (f - this.camZ) * this.lookZ;
   }

   public double distanceSqr(double d, double e, double f) {
      double var7 = d - this.camX;
      double var9 = e - this.camY;
      double var11 = f - this.camZ;
      return var7 * var7 + var9 * var9 + var11 * var11;
   }

   public void quad(double d, double e, double f, double g, double h, double i, double j, double k, double l, double m, double n, double o, int p) {
      if (ColorHelper.getAlpha(p) >= 3 && this.reserveQuad()) {
         int var26 = this.quads * 12;
         double[] var27 = this.quadPos;
         var27[var26] = d;
         var27[var26 + 1] = e;
         var27[var26 + 2] = f;
         var27[var26 + 3] = g;
         var27[var26 + 4] = h;
         var27[var26 + 5] = i;
         var27[var26 + 6] = j;
         var27[var26 + 7] = k;
         var27[var26 + 8] = l;
         var27[var26 + 9] = m;
         var27[var26 + 10] = n;
         var27[var26 + 11] = o;
         this.quadColor[this.quads++] = p;
      }
   }

   public void outwardTriangle(double d, double e, double f, double g, double h, double i, double j, double k, double l, double m, double n, double o, int p) {
      double var26 = h - e;
      double var28 = l - f;
      double var30 = i - f;
      double var32 = k - e;
      double var34 = var26 * var28 - var30 * var32;
      double var36 = (d + g + j) / 3.0 - m;
      double var38 = j - d;
      double var40 = g - d;
      double var42 = var30 * var38 - var40 * var28;
      double var44 = (e + h + k) / 3.0 - n;
      double var46 = var40 * var32 - var26 * var38;
      double var48 = (f + i + l) / 3.0 - o;
      if (var34 * var36 + var42 * var44 + var46 * var48 >= 0.0) {
         this.quad(d, e, f, g, h, i, j, k, l, j, k, l, p);
      } else {
         this.quad(d, e, f, j, k, l, g, h, i, g, h, i, p);
      }
   }

   public void line(double d, double e, double f, double g, double h, double i, int j, float k) {
      if (ColorHelper.getAlpha(j) >= 3 && this.reserveLine()) {
         int var15 = this.lines * 6;
         double[] var16 = this.linePos;
         var16[var15] = d;
         var16[var15 + 1] = e;
         var16[var15 + 2] = f;
         var16[var15 + 3] = g;
         var16[var15 + 4] = h;
         var16[var15 + 5] = i;
         this.lineColor[this.lines] = j;
         this.lineWidth[this.lines++] = k;
      }
   }

   public void ribbon(double d, double e, double f, double g, double h, double i, int j, float k, double l) {
      double var17 = this.depth(d, e, f);
      double var19 = this.depth(g, h, i);
      if (!(var17 < 0.05) || !(var19 < 0.05)) {
         if (var17 < 0.05) {
            double var21 = (0.05 - var17) / (var19 - var17);
            d += (g - d) * var21;
            e += (h - e) * var21;
            f += (i - f) * var21;
            var17 = 0.05;
         } else if (var19 < 0.05) {
            double var47 = (0.05 - var19) / (var17 - var19);
            g += (d - g) * var47;
            h += (e - h) * var47;
            i += (f - i) * var47;
            var19 = 0.05;
         }

         double var48 = d - this.camX;
         double var23 = e - this.camY;
         double var25 = f - this.camZ;
         double var27 = g - d;
         double var29 = h - e;
         double var31 = i - f;
         double var33 = var23 * var31 - var25 * var29;
         double var35 = var25 * var27 - var48 * var31;
         double var37 = var48 * var29 - var23 * var27;
         double var39 = Math.sqrt(var33 * var33 + var35 * var35 + var37 * var37);
         if (!(var39 < 1.0E-9)) {
            double var41 = k * 0.5 * l / var39;
            double var43 = var41 * var17;
            double var45 = var41 * var19;
            this.quad(
               d + var33 * var43,
               e + var35 * var43,
               f + var37 * var43,
               g + var33 * var45,
               h + var35 * var45,
               i + var37 * var45,
               g - var33 * var45,
               h - var35 * var45,
               i - var37 * var45,
               d - var33 * var43,
               e - var35 * var43,
               f - var37 * var43,
               j
            );
         }
      }
   }

   public void shape(float[] fs, double d, double e, double f, double g, double h, double i, double j, double k, double l, int m) {
      if (ColorHelper.getAlpha(m) >= 3) {
         for (byte var21 = 0; var21 + 7 < fs.length; var21 += 8) {
            float var22 = fs[var21];
            float var23 = fs[var21 + 1];
            float var24 = fs[var21 + 2];
            float var25 = fs[var21 + 3];
            float var26 = fs[var21 + 4];
            float var27 = fs[var21 + 5];
            float var28 = fs[var21 + 6];
            float var29 = fs[var21 + 7];
            this.quad(
               d + g * var22 + j * var23,
               e + h * var22 + k * var23,
               f + i * var22 + l * var23,
               d + g * var24 + j * var25,
               e + h * var24 + k * var25,
               f + i * var24 + l * var25,
               d + g * var26 + j * var27,
               e + h * var26 + k * var27,
               f + i * var26 + l * var27,
               d + g * var28 + j * var29,
               e + h * var28 + k * var29,
               f + i * var28 + l * var29,
               m
            );
         }
      }
   }

   public void sprite(float[] fs, double d, double e, double f, float g, float h, int i) {
      this.sprite(fs, d, e, f, g, g, h, i);
   }

   public void sprite(float[] fs, double d, double e, double f, float g, float h, float i, int j) {
      this.sprite(fs, d, e, f, g, h, i, 0.0F, 0.0F, j);
   }

   public void sprite(float[] fs, double d, double e, double f, float g, float h, float i, float j, float k, int l) {
      if (ColorHelper.getAlpha(l) >= 3) {
         float var14 = MathHelper.cos(i);
         float var15 = MathHelper.sin(i);
         double var16 = (this.rightX * var14 + this.upX * var15) * g;
         double var18 = (this.rightY * var14 + this.upY * var15) * g;
         double var20 = (this.rightZ * var14 + this.upZ * var15) * g;
         double var22 = (this.upX * var14 - this.rightX * var15) * h;
         double var24 = (this.upY * var14 - this.rightY * var15) * h;
         double var26 = (this.upZ * var14 - this.rightZ * var15) * h;
         this.shape(fs, d + var16 * j + var22 * k, e + var18 * j + var24 * k, f + var20 * j + var26 * k, var16, var18, var20, var22, var24, var26, l);
      }
   }

   public void flat(float[] fs, double d, double e, double f, float g, float h, int i) {
      float var11 = MathHelper.cos(h) * g;
      float var12 = MathHelper.sin(h) * g;
      double var13 = this.camY >= e ? -1.0 : 1.0;
      this.shape(fs, d, e, f, var11, 0.0, var13 * var12, -var12, 0.0, var13 * var11, i);
   }

   public void band(double d, double e, double f, float g, float h, float i, float j, int k, int l) {
      if (!(h <= g) && k > 0 && ColorHelper.getAlpha(l) >= 3) {
         double var13 = this.camY >= e ? -1.0 : 1.0;
         float var15 = (j - i) / k;
         float var16 = MathHelper.cos(i);
         float var17 = MathHelper.sin(i);

         for (int var18 = 1; var18 <= k; var18++) {
            float var19 = i + var15 * var18;
            float var20 = MathHelper.cos(var19);
            float var21 = MathHelper.sin(var19);
            this.quad(
               d + var16 * g,
               e,
               f + var13 * var17 * g,
               d + var16 * h,
               e,
               f + var13 * var17 * h,
               d + var20 * h,
               e,
               f + var13 * var21 * h,
               d + var20 * g,
               e,
               f + var13 * var21 * g,
               l
            );
            var16 = var20;
            var17 = var21;
         }
      }
   }

   public void ring(double d, double e, double f, float g, float h, int i, int j) {
      this.band(d, e, f, g, h, 0.0F, 6.2831855F, i, j);
   }

   @Override
   public void draw(GizmoDrawer consumer, float opacity) {
      boolean var3 = opacity < 1.0F;
      double[] var4 = this.linePos;
      int var5 = 0;

      for (byte var6 = 0; var5 < this.lines; var6 += 6) {
         int var7 = var3 ? ColorHelper.scaleAlpha(this.lineColor[var5], opacity) : this.lineColor[var5];
         consumer.addLine(
            this.vertex(var4[var6], var4[var6 + 1], var4[var6 + 2]), this.vertex(var4[var6 + 3], var4[var6 + 4], var4[var6 + 5]), var7, this.lineWidth[var5]
         );
         var5++;
      }
   }

   private Vec3d vertex(double d, double e, double f) {
      long var8 = Double.doubleToRawLongBits(d) * -7046029254386353131L
         ^ Double.doubleToRawLongBits(e) * -4417276706812531889L
         ^ Double.doubleToRawLongBits(f) * 1609587929392839161L;
      int var10 = (int)(var8 >>> 55);
      Vec3d var11 = this.vertices[var10];
      if (var11 != null && var11.x == d && var11.y == e && var11.z == f) {
         return var11;
      } else {
         Vec3d var7;
         this.vertices[var10] = var7 = new Vec3d(d, e, f);
         return var7;
      }
   }

   private boolean reserveQuad() {
      if (this.quads < this.quadColor.length) {
         return true;
      } else if (this.quads >= 12000) {
         return false;
      } else {
         int var1 = Math.min(12000, this.quadColor.length * 2);
         this.quadColor = Arrays.copyOf(this.quadColor, var1);
         this.quadPos = Arrays.copyOf(this.quadPos, var1 * 12);
         return true;
      }
   }

   private boolean reserveLine() {
      if (this.lines < this.lineColor.length) {
         return true;
      } else if (this.lines >= 4000) {
         return false;
      } else {
         int var1 = Math.min(4000, this.lineColor.length * 2);
         this.lineColor = Arrays.copyOf(this.lineColor, var1);
         this.lineWidth = Arrays.copyOf(this.lineWidth, var1);
         this.linePos = Arrays.copyOf(this.linePos, var1 * 6);
         return true;
      }
   }
}
