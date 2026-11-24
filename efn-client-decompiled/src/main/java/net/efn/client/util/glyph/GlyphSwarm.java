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

   public GlyphSwarm(int i) {
      this.capacity = i;
      this.x = new double[i];
      this.y = new double[i];
      this.z = new double[i];
      this.vx = new float[i];
      this.vy = new float[i];
      this.vz = new float[i];
      this.age = new float[i];
      this.life = new float[i];
      this.size = new float[i];
      this.rot = new float[i];
      this.spin = new float[i];
      this.seed = new float[i];
      this.kind = new byte[i];
      this.color = new int[i];
   }

   public int count() {
      return this.count;
   }

   public void clear() {
      this.count = 0;
   }

   public void spawn(byte b, double d, double e, double f, float g, float h, float i, float j, float k, float l, float m, float n, int o, int p) {
      int var18 = Math.min(this.capacity, Math.max(1, p));
      int var19 = this.count < var18 ? this.count++ : this.oldest();
      this.x[var19] = d;
      this.y[var19] = e;
      this.z[var19] = f;
      this.vx[var19] = g;
      this.vy[var19] = h;
      this.vz[var19] = i;
      this.age[var19] = 0.0F;
      this.life[var19] = Math.max(0.05F, j);
      this.size[var19] = k;
      this.rot[var19] = l;
      this.spin[var19] = m;
      this.seed[var19] = n;
      this.kind[var19] = b;
      this.color[var19] = o;
   }

   public void trim(int i) {
      while (this.count > Math.max(0, i)) {
         this.remove(this.oldest());
      }
   }

   public void step(float f) {
      if (!(f <= 0.0F)) {
         for (int var2 = this.count - 1; var2 >= 0; var2--) {
            float var5 = this.age[var2] + f;
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
                     this.vx[var2] = this.vx[var2] + MathHelper.sin(var5 * 2.3F + this.seed[var2] * 6.0F) * 0.35F * f;
                     this.vz[var2] = this.vz[var2] + MathHelper.cos(var5 * 1.9F + this.seed[var2] * 6.0F) * 0.35F * f;
                     break;
                  case 1:
                     var4 = 0.8F;
                     var3 = -1.1F;
                     float var6 = (this.seed[var2] > 0.5F ? 1.8F : -1.8F) * f;
                     float var7 = MathHelper.cos(var6);
                     float var8 = MathHelper.sin(var6);
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

               float var13 = Math.max(0.0F, 1.0F - var4 * f);
               this.vx[var2] = this.vx[var2] * var13;
               this.vy[var2] = this.vy[var2] * var13 + var3 * f;
               this.vz[var2] = this.vz[var2] * var13;
               this.x[var2] = this.x[var2] + this.vx[var2] * f;
               this.y[var2] = this.y[var2] + this.vy[var2] * f;
               this.z[var2] = this.z[var2] + this.vz[var2] * f;
               this.rot[var2] = this.rot[var2] + this.spin[var2] * f;
            }
         }
      }
   }

   public void draw(GlyphCanvas glyphCanvas, float f, boolean bl) {
      for (int var4 = 0; var4 < this.count; var4++) {
         double var9 = this.x[var4];
         double var11 = this.y[var4];
         double var13 = this.z[var4];
         double var7;
         if (!(glyphCanvas.depth(var9, var11, var13) < 0.05) && !((var7 = glyphCanvas.distanceSqr(var9, var11, var13)) > 9216.0)) {
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
                  var18 = 0.72F + 0.28F * Math.abs(MathHelper.sin(this.age[var4] * 15.0F + this.seed[var4] * 9.0F));
                  break;
               case 2:
                  var6 = GlyphShapes.GRAIN;
                  break;
               default:
                  var6 = GlyphShapes.SPARK;
                  var17 *= 1.0F - 0.6F * var15;
                  var16 *= 0.75F + 0.25F * MathHelper.sin(this.age[var4] * 23.0F + this.seed[var4] * 12.0F);
            }

            float var19 = var16 * f;
            if (!(var19 < 0.012F)) {
               int var20 = this.color[var4];
               boolean var5 = var7 < 484.0;
               if (bl && var5) {
                  glyphCanvas.sprite(GlyphShapes.OCTAGON, var9, var11, var13, var17 * 2.3F, 0.0F, GlyphTint.alpha(var20, var19 * 0.17F));
               }

               if (!var5 && var6 != GlyphShapes.GRAIN) {
                  var6 = GlyphShapes.DIAMOND;
               }

               glyphCanvas.sprite(var6, var9, var11, var13, var17 * var18, var17, this.rot[var4], GlyphTint.alpha(var20, var19));
            }
         }
      }
   }

   private static float bloom(float f) {
      float var1 = f - 1.0F;
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

   private void remove(int i) {
      int var2;
      if (i != (var2 = --this.count)) {
         this.x[i] = this.x[var2];
         this.y[i] = this.y[var2];
         this.z[i] = this.z[var2];
         this.vx[i] = this.vx[var2];
         this.vy[i] = this.vy[var2];
         this.vz[i] = this.vz[var2];
         this.age[i] = this.age[var2];
         this.life[i] = this.life[var2];
         this.size[i] = this.size[var2];
         this.rot[i] = this.rot[var2];
         this.spin[i] = this.spin[var2];
         this.seed[i] = this.seed[var2];
         this.kind[i] = this.kind[var2];
         this.color[i] = this.color[var2];
      }
   }
}
