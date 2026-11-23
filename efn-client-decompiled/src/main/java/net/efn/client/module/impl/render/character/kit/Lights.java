package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Lights {
   public static final Lights WORLD = new Lights(0.2F, 1.0F, -0.7F, -0.2F, 1.0F, 0.7F);
   private static final float AMBIENT = 0.4F;
   private static final float POWER = 0.6F;
   private static final float FLAT_0 = 0.853F;
   private static final float FLAT_1 = 0.273F;
   private static final float FLAT_2 = -0.126F;
   public final float x0;
   public final float y0;
   public final float z0;
   public final float x1;
   public final float y1;
   public final float z1;
   private final float dx;
   private final float dy;
   private final float dz;
   private final boolean opposite;

   public Lights(float var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = length(var1, var2, var3);
      float var8 = length(var4, var5, var6);
      if (!(var7 < 1.0E-6F) && !(var8 < 1.0E-6F)) {
         this.x0 = var1 / var7;
         this.y0 = var2 / var7;
         this.z0 = var3 / var7;
         this.x1 = var4 / var8;
         this.y1 = var5 / var8;
         this.z1 = var6 / var8;
         float var9 = this.x0 + this.x1;
         float var10 = this.y0 + this.y1;
         float var11 = this.z0 + this.z1;
         float var12 = length(var9, var10, var11);
         this.opposite = var12 < 0.25F;
         this.dx = this.opposite ? this.x0 : var9 / var12;
         this.dy = this.opposite ? this.y0 : var10 / var12;
         this.dz = this.opposite ? this.z0 : var11 / var12;
      } else {
         throw new IllegalArgumentException("light directions must not be zero");
      }
   }

   public Lights rotateY(float var1) {
      double var2 = Math.toRadians(var1);
      float var4 = (float)Math.cos(var2);
      float var5 = (float)Math.sin(var2);
      return new Lights(
         var4 * this.x0 + var5 * this.z0, this.y0, var4 * this.z0 - var5 * this.x0, var4 * this.x1 + var5 * this.z1, this.y1, var4 * this.z1 - var5 * this.x1
      );
   }

   public float brightness(float var1, float var2, float var3) {
      float var4 = Math.max(0.0F, this.x0 * var1 + this.y0 * var2 + this.z0 * var3);
      float var5 = Math.max(0.0F, this.x1 * var1 + this.y1 * var2 + this.z1 * var3);
      return Math.min(1.0F, (var4 + var5) * 0.6F + 0.4F);
   }

   public void flatten(float[] var1, int var2, float var3) {
      if (var3 > 0.0F) {
         float var4 = Math.min(1.0F, var3);
         float var5 = var1[var2];
         float var6 = var1[var2 + 1];
         float var7 = var1[var2 + 2];
         float var8 = this.dx;
         float var9 = this.dy;
         float var10 = this.dz;
         float var11 = var5 * var8 + var6 * var9 + var7 * var10;
         if (this.opposite && var11 < 0.0F) {
            var8 = -var8;
            var9 = -var9;
            var10 = -var10;
            var11 = -var11;
         }

         float var12 = var11 + var4 * (0.853F + (0.273F + -0.126F * var11) * var11 - var11);
         float var13 = var5 - var11 * var8;
         float var14 = var6 - var11 * var9;
         float var15 = var7 - var11 * var10;
         float var16 = var13 * var13 + var14 * var14 + var15 * var15;
         if (var16 < 1.0E-10F) {
            if (var11 > 0.0F) {
               return;
            }

            if (Math.abs(var8) < 0.9F) {
               var13 = 0.0F;
               var14 = var10;
               var15 = -var9;
            } else {
               var13 = var9;
               var14 = -var8;
               var15 = 0.0F;
            }

            var16 = var13 * var13 + var14 * var14 + var15 * var15;
         }

         float var17 = (float)Math.sqrt(Math.max(0.0F, 1.0F - var12 * var12) / var16);
         var1[var2] = var8 * var12 + var13 * var17;
         var1[var2 + 1] = var9 * var12 + var14 * var17;
         var1[var2 + 2] = var10 * var12 + var15 * var17;
      }
   }

   private static float length(float var0, float var1, float var2) {
      return (float)Math.sqrt(var0 * var0 + var1 * var1 + var2 * var2);
   }
}
