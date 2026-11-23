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

   public void begin(Camera var1) {
      this.quads = 0;
      this.lines = 0;
      Vec3d var2 = var1.method_71156();
      this.camX = var2.field_1352;
      this.camY = var2.field_1351;
      this.camZ = var2.field_1350;
      Vector3fc var3 = var1.method_35689();
      Vector3fc var4 = var1.method_19336();
      Vector3fc var5 = var1.method_19335();
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

   public void submit(boolean var1) {
      if (this.quads > 0) {
         WorldQuads.layer(var1).quads(this.quadPos, this.quadColor, this.quads);
      }

      this.quads = 0;
      if (this.lines != 0) {
         VisibilityConfigurable var2 = GizmoDrawing.method_75553(this);
         if (var1) {
            var2.method_75533();
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

   public double depth(double var1, double var3, double var5) {
      return (var1 - this.camX) * this.lookX + (var3 - this.camY) * this.lookY + (var5 - this.camZ) * this.lookZ;
   }

   public double distanceSqr(double var1, double var3, double var5) {
      double var7 = var1 - this.camX;
      double var9 = var3 - this.camY;
      double var11 = var5 - this.camZ;
      return var7 * var7 + var9 * var9 + var11 * var11;
   }

   public void quad(
      double var1,
      double var3,
      double var5,
      double var7,
      double var9,
      double var11,
      double var13,
      double var15,
      double var17,
      double var19,
      double var21,
      double var23,
      int var25
   ) {
      if (ColorHelper.method_61320(var25) >= 3 && this.reserveQuad()) {
         int var26 = this.quads * 12;
         double[] var27 = this.quadPos;
         var27[var26] = var1;
         var27[var26 + 1] = var3;
         var27[var26 + 2] = var5;
         var27[var26 + 3] = var7;
         var27[var26 + 4] = var9;
         var27[var26 + 5] = var11;
         var27[var26 + 6] = var13;
         var27[var26 + 7] = var15;
         var27[var26 + 8] = var17;
         var27[var26 + 9] = var19;
         var27[var26 + 10] = var21;
         var27[var26 + 11] = var23;
         this.quadColor[this.quads++] = var25;
      }
   }

   public void outwardTriangle(
      double var1,
      double var3,
      double var5,
      double var7,
      double var9,
      double var11,
      double var13,
      double var15,
      double var17,
      double var19,
      double var21,
      double var23,
      int var25
   ) {
      double var26 = var9 - var3;
      double var28 = var17 - var5;
      double var30 = var11 - var5;
      double var32 = var15 - var3;
      double var34 = var26 * var28 - var30 * var32;
      double var36 = (var1 + var7 + var13) / 3.0 - var19;
      double var38 = var13 - var1;
      double var40 = var7 - var1;
      double var42 = var30 * var38 - var40 * var28;
      double var44 = (var3 + var9 + var15) / 3.0 - var21;
      double var46 = var40 * var32 - var26 * var38;
      double var48 = (var5 + var11 + var17) / 3.0 - var23;
      if (var34 * var36 + var42 * var44 + var46 * var48 >= 0.0) {
         this.quad(var1, var3, var5, var7, var9, var11, var13, var15, var17, var13, var15, var17, var25);
      } else {
         this.quad(var1, var3, var5, var13, var15, var17, var7, var9, var11, var7, var9, var11, var25);
      }
   }

   public void line(double var1, double var3, double var5, double var7, double var9, double var11, int var13, float var14) {
      if (ColorHelper.method_61320(var13) >= 3 && this.reserveLine()) {
         int var15 = this.lines * 6;
         double[] var16 = this.linePos;
         var16[var15] = var1;
         var16[var15 + 1] = var3;
         var16[var15 + 2] = var5;
         var16[var15 + 3] = var7;
         var16[var15 + 4] = var9;
         var16[var15 + 5] = var11;
         this.lineColor[this.lines] = var13;
         this.lineWidth[this.lines++] = var14;
      }
   }

   public void ribbon(double var1, double var3, double var5, double var7, double var9, double var11, int var13, float var14, double var15) {
      double var17 = this.depth(var1, var3, var5);
      double var19 = this.depth(var7, var9, var11);
      if (!(var17 < 0.05) || !(var19 < 0.05)) {
         if (var17 < 0.05) {
            double var21 = (0.05 - var17) / (var19 - var17);
            var1 += (var7 - var1) * var21;
            var3 += (var9 - var3) * var21;
            var5 += (var11 - var5) * var21;
            var17 = 0.05;
         } else if (var19 < 0.05) {
            double var47 = (0.05 - var19) / (var17 - var19);
            var7 += (var1 - var7) * var47;
            var9 += (var3 - var9) * var47;
            var11 += (var5 - var11) * var47;
            var19 = 0.05;
         }

         double var48 = var1 - this.camX;
         double var23 = var3 - this.camY;
         double var25 = var5 - this.camZ;
         double var27 = var7 - var1;
         double var29 = var9 - var3;
         double var31 = var11 - var5;
         double var33 = var23 * var31 - var25 * var29;
         double var35 = var25 * var27 - var48 * var31;
         double var37 = var48 * var29 - var23 * var27;
         double var39 = Math.sqrt(var33 * var33 + var35 * var35 + var37 * var37);
         if (!(var39 < 1.0E-9)) {
            double var41 = var14 * 0.5 * var15 / var39;
            double var43 = var41 * var17;
            double var45 = var41 * var19;
            this.quad(
               var1 + var33 * var43,
               var3 + var35 * var43,
               var5 + var37 * var43,
               var7 + var33 * var45,
               var9 + var35 * var45,
               var11 + var37 * var45,
               var7 - var33 * var45,
               var9 - var35 * var45,
               var11 - var37 * var45,
               var1 - var33 * var43,
               var3 - var35 * var43,
               var5 - var37 * var43,
               var13
            );
         }
      }
   }

   public void shape(
      float[] var1, double var2, double var4, double var6, double var8, double var10, double var12, double var14, double var16, double var18, int var20
   ) {
      if (ColorHelper.method_61320(var20) >= 3) {
         for (byte var21 = 0; var21 + 7 < var1.length; var21 += 8) {
            float var22 = var1[var21];
            float var23 = var1[var21 + 1];
            float var24 = var1[var21 + 2];
            float var25 = var1[var21 + 3];
            float var26 = var1[var21 + 4];
            float var27 = var1[var21 + 5];
            float var28 = var1[var21 + 6];
            float var29 = var1[var21 + 7];
            this.quad(
               var2 + var8 * var22 + var14 * var23,
               var4 + var10 * var22 + var16 * var23,
               var6 + var12 * var22 + var18 * var23,
               var2 + var8 * var24 + var14 * var25,
               var4 + var10 * var24 + var16 * var25,
               var6 + var12 * var24 + var18 * var25,
               var2 + var8 * var26 + var14 * var27,
               var4 + var10 * var26 + var16 * var27,
               var6 + var12 * var26 + var18 * var27,
               var2 + var8 * var28 + var14 * var29,
               var4 + var10 * var28 + var16 * var29,
               var6 + var12 * var28 + var18 * var29,
               var20
            );
         }
      }
   }

   public void sprite(float[] var1, double var2, double var4, double var6, float var8, float var9, int var10) {
      this.sprite(var1, var2, var4, var6, var8, var8, var9, var10);
   }

   public void sprite(float[] var1, double var2, double var4, double var6, float var8, float var9, float var10, int var11) {
      this.sprite(var1, var2, var4, var6, var8, var9, var10, 0.0F, 0.0F, var11);
   }

   public void sprite(float[] var1, double var2, double var4, double var6, float var8, float var9, float var10, float var11, float var12, int var13) {
      if (ColorHelper.method_61320(var13) >= 3) {
         float var14 = MathHelper.method_15362(var10);
         float var15 = MathHelper.method_15374(var10);
         double var16 = (this.rightX * var14 + this.upX * var15) * var8;
         double var18 = (this.rightY * var14 + this.upY * var15) * var8;
         double var20 = (this.rightZ * var14 + this.upZ * var15) * var8;
         double var22 = (this.upX * var14 - this.rightX * var15) * var9;
         double var24 = (this.upY * var14 - this.rightY * var15) * var9;
         double var26 = (this.upZ * var14 - this.rightZ * var15) * var9;
         this.shape(
            var1,
            var2 + var16 * var11 + var22 * var12,
            var4 + var18 * var11 + var24 * var12,
            var6 + var20 * var11 + var26 * var12,
            var16,
            var18,
            var20,
            var22,
            var24,
            var26,
            var13
         );
      }
   }

   public void flat(float[] var1, double var2, double var4, double var6, float var8, float var9, int var10) {
      float var11 = MathHelper.method_15362(var9) * var8;
      float var12 = MathHelper.method_15374(var9) * var8;
      double var13 = this.camY >= var4 ? -1.0 : 1.0;
      this.shape(var1, var2, var4, var6, var11, 0.0, var13 * var12, -var12, 0.0, var13 * var11, var10);
   }

   public void band(double var1, double var3, double var5, float var7, float var8, float var9, float var10, int var11, int var12) {
      if (!(var8 <= var7) && var11 > 0 && ColorHelper.method_61320(var12) >= 3) {
         double var13 = this.camY >= var3 ? -1.0 : 1.0;
         float var15 = (var10 - var9) / var11;
         float var16 = MathHelper.method_15362(var9);
         float var17 = MathHelper.method_15374(var9);

         for (int var18 = 1; var18 <= var11; var18++) {
            float var19 = var9 + var15 * var18;
            float var20 = MathHelper.method_15362(var19);
            float var21 = MathHelper.method_15374(var19);
            this.quad(
               var1 + var16 * var7,
               var3,
               var5 + var13 * var17 * var7,
               var1 + var16 * var8,
               var3,
               var5 + var13 * var17 * var8,
               var1 + var20 * var8,
               var3,
               var5 + var13 * var21 * var8,
               var1 + var20 * var7,
               var3,
               var5 + var13 * var21 * var7,
               var12
            );
            var16 = var20;
            var17 = var21;
         }
      }
   }

   public void ring(double var1, double var3, double var5, float var7, float var8, int var9, int var10) {
      this.band(var1, var3, var5, var7, var8, 0.0F, 6.2831855F, var9, var10);
   }

   public void method_75531(GizmoDrawer var1, float var2) {
      boolean var3 = var2 < 1.0F;
      double[] var4 = this.linePos;
      int var5 = 0;

      for (byte var6 = 0; var5 < this.lines; var6 += 6) {
         int var7 = var3 ? ColorHelper.method_75601(this.lineColor[var5], var2) : this.lineColor[var5];
         var1.method_75473(
            this.vertex(var4[var6], var4[var6 + 1], var4[var6 + 2]), this.vertex(var4[var6 + 3], var4[var6 + 4], var4[var6 + 5]), var7, this.lineWidth[var5]
         );
         var5++;
      }
   }

   private Vec3d vertex(double var1, double var3, double var5) {
      long var8 = Double.doubleToRawLongBits(var1) * -7046029254386353131L
         ^ Double.doubleToRawLongBits(var3) * -4417276706812531889L
         ^ Double.doubleToRawLongBits(var5) * 1609587929392839161L;
      int var10 = (int)(var8 >>> 55);
      Vec3d var11 = this.vertices[var10];
      if (var11 != null && var11.field_1352 == var1 && var11.field_1351 == var3 && var11.field_1350 == var5) {
         return var11;
      } else {
         Vec3d var7;
         this.vertices[var10] = var7 = new Vec3d(var1, var3, var5);
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
