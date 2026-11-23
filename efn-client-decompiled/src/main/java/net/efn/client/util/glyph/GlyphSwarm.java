package net.efn.client.util.glyph;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class GlyphSwarm {
   public static final byte BOLT = 0;
   public static final byte GEM = 1;
   public static final byte GRAIN = 2;
   public static final byte SPARK = 3;
   public static final int WORST_QUADS = (
         Math.max(
               Math.max(GlyphShapes.BOLT.length, GlyphShapes.GEM.length),
               Math.max(GlyphShapes.SPARK.length, Math.max(GlyphShapes.GRAIN.length, GlyphShapes.DIAMOND.length))
            )
            + GlyphShapes.OCTAGON.length
      )
      / 8;
   private static final double CULL_DISTANCE_SQR = 9216.0;
   private static final double DETAIL_DISTANCE_SQR = 484.0;
   private static final float HALO_SCALE = 2.3F;
   private static final float HALO_ALPHA = 0.17F;
   private final int capacity;
   private final double[] x;
   private final double[] y;
   private final double[] z;
   private final float[] vx;
   private final float[] vy;
   private final float[] vz;
   private final float[] age;
   private final float[] life;
   private final float[] size;
   private final float[] rot;
   private final float[] spin;
   private final float[] seed;
   private final byte[] kind;
   private final int[] color;
   private int count;

   public GlyphSwarm(int var1) {
      this.capacity = var1;
      this.x = new double[var1];
      this.y = new double[var1];
      this.z = new double[var1];
      this.vx = new float[var1];
      this.vy = new float[var1];
      this.vz = new float[var1];
      this.age = new float[var1];
      this.life = new float[var1];
      this.size = new float[var1];
      this.rot = new float[var1];
      this.spin = new float[var1];
      this.seed = new float[var1];
      this.kind = new byte[var1];
      this.color = new int[var1];
   }

   public int count() {
      return this.count;
   }

   public void clear() {
      this.count = 0;
   }

   public void spawn(
      byte var1,
      double var2,
      double var4,
      double var6,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13,
      float var14,
      float var15,
      int var16,
      int var17
   ) {
      int var18 = Math.min(this.capacity, Math.max(1, var17));
      int var19 = this.count < var18 ? this.count++ : this.oldest();
      this.x[var19] = var2;
      this.y[var19] = var4;
      this.z[var19] = var6;
      this.vx[var19] = var8;
      this.vy[var19] = var9;
      this.vz[var19] = var10;
      this.age[var19] = 0.0F;
      this.life[var19] = Math.max(0.05F, var11);
      this.size[var19] = var12;
      this.rot[var19] = var13;
      this.spin[var19] = var14;
      this.seed[var19] = var15;
      this.kind[var19] = var1;
      this.color[var19] = var16;
   }

   public void trim(int var1) {
      while (this.count > Math.max(0, var1)) {
         this.remove(this.oldest());
      }
   }

   public void step(float var1) {
      if (!(var1 <= 0.0F)) {
         for (int var2 = this.count - 1; var2 >= 0; var2--) {
            float var5 = this.age[var2] + var1;
            if (var5 >= this.life[var2]) {
               this.remove(var2);
            } else {
               this.age[var2] = var5;
               float var3;
               float var4;
               switch (this.kind[var2]) {
                  case 0:
                     var4 = 1.6F;
                     var3 = 0.7F;
                     this.vx[var2] = this.vx[var2] + MathHelper.method_15374(var5 * 2.3F + this.seed[var2] * 6.0F) * 0.35F * var1;
                     this.vz[var2] = this.vz[var2] + MathHelper.method_15362(var5 * 1.9F + this.seed[var2] * 6.0F) * 0.35F * var1;
                     break;
                  case 1:
                     var4 = 0.8F;
                     var3 = -1.1F;
                     float var6 = (this.seed[var2] > 0.5F ? 1.8F : -1.8F) * var1;
                     float var7 = MathHelper.method_15362(var6);
                     float var8 = MathHelper.method_15374(var6);
                     float var9 = this.vx[var2] * var7 - this.vz[var2] * var8;
                     this.vz[var2] = this.vx[var2] * var8 + this.vz[var2] * var7;
                     this.vx[var2] = var9;
                     break;
                  case 2:
                     var4 = 0.9F;
                     var3 = -6.5F;
                     break;
                  default:
                     var4 = 3.4F;
                     var3 = -1.6F;
               }

               float var13 = Math.max(0.0F, 1.0F - var4 * var1);
               this.vx[var2] = this.vx[var2] * var13;
               this.vy[var2] = this.vy[var2] * var13 + var3 * var1;
               this.vz[var2] = this.vz[var2] * var13;
               this.x[var2] = this.x[var2] + this.vx[var2] * var1;
               this.y[var2] = this.y[var2] + this.vy[var2] * var1;
               this.z[var2] = this.z[var2] + this.vz[var2] * var1;
               this.rot[var2] = this.rot[var2] + this.spin[var2] * var1;
            }
         }
      }
   }

   public void draw(GlyphCanvas var1, float var2, boolean var3) {
      for (int var4 = 0; var4 < this.count; var4++) {
         double var9 = this.x[var4];
         double var11 = this.y[var4];
         double var13 = this.z[var4];
         double var7;
         if (!(var1.depth(var9, var11, var13) < 0.05) && !((var7 = var1.distanceSqr(var9, var11, var13)) > 9216.0)) {
            float var15 = this.age[var4] / this.life[var4];
            float var16 = Math.min(1.0F, var15 * 9.0F) * (1.0F - var15 * var15);
            float var17 = this.size[var4];
            float var18 = 1.0F;
            float[] var6;
            switch (this.kind[var4]) {
               case 0:
                  var6 = GlyphShapes.BOLT;
                  var17 *= bloom(Math.min(1.0F, var15 * 5.0F)) * (1.0F - 0.35F * var15);
                  break;
               case 1:
                  var6 = GlyphShapes.GEM;
                  var18 = 0.72F + 0.28F * Math.abs(MathHelper.method_15374(this.age[var4] * 15.0F + this.seed[var4] * 9.0F));
                  break;
               case 2:
                  var6 = GlyphShapes.GRAIN;
                  break;
               default:
                  var6 = GlyphShapes.SPARK;
                  var17 *= 1.0F - 0.6F * var15;
                  var16 *= 0.75F + 0.25F * MathHelper.method_15374(this.age[var4] * 23.0F + this.seed[var4] * 12.0F);
            }

            float var19 = var16 * var2;
            if (!(var19 < 0.012F)) {
               int var20 = this.color[var4];
               boolean var5 = var7 < 484.0;
               if (var3 && var5) {
                  var1.sprite(GlyphShapes.OCTAGON, var9, var11, var13, var17 * 2.3F, 0.0F, GlyphTint.alpha(var20, var19 * 0.17F));
               }

               if (!var5 && var6 != GlyphShapes.GRAIN) {
                  var6 = GlyphShapes.DIAMOND;
               }

               var1.sprite(var6, var9, var11, var13, var17 * var18, var17, this.rot[var4], GlyphTint.alpha(var20, var19));
            }
         }
      }
   }

   private static float bloom(float var0) {
      float var1 = var0 - 1.0F;
      return 1.0F + 2.2F * var1 * var1 * var1 + 1.2F * var1 * var1;
   }

   private int oldest() {
      int var1 = 0;
      float var2 = -1.0F;

      for (int var3 = 0; var3 < this.count; var3++) {
         float var4 = this.age[var3] / this.life[var3];
         if (var4 > var2) {
            var2 = var4;
            var1 = var3;
         }
      }

      return var1;
   }

   private void remove(int var1) {
      int var2;
      if (var1 != (var2 = --this.count)) {
         this.x[var1] = this.x[var2];
         this.y[var1] = this.y[var2];
         this.z[var1] = this.z[var2];
         this.vx[var1] = this.vx[var2];
         this.vy[var1] = this.vy[var2];
         this.vz[var1] = this.vz[var2];
         this.age[var1] = this.age[var2];
         this.life[var1] = this.life[var2];
         this.size[var1] = this.size[var2];
         this.rot[var1] = this.rot[var2];
         this.spin[var1] = this.spin[var2];
         this.seed[var1] = this.seed[var2];
         this.kind[var1] = this.kind[var2];
         this.color[var1] = this.color[var2];
      }
   }
}
