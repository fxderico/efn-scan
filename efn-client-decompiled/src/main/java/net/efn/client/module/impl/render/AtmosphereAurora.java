package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class AtmosphereAurora {
   private static final float RADIUS = 100.0F;
   private static final int COLUMNS = 64;
   private static final int ROWS = 6;
   private static final float[][] CURTAINS = new float[][]{{-40.0F, 22.0F, 78.0F, 18.0F}, {6.0F, 28.0F, 100.0F, 23.0F}, {54.0F, 20.0F, 70.0F, 16.0F}};
   private final SkyMesh mesh = new SkyMesh("EFN aurora", SkyMesh.GLOW, CURTAINS.length * 64 * 6);
   private final float[] x = new float[455];
   private final float[] y = new float[this.x.length];
   private final float[] z = new float[this.x.length];
   private final int[] color = new int[this.x.length];

   AtmosphereAurora() {
   }

   void render(MatrixStack var1, float var2, float var3, int var4, int var5) {
      BufferBuilder var6 = this.mesh.begin();

      for (int var7 = 0; var7 < CURTAINS.length; var7++) {
         this.curtain(var6, CURTAINS[var7], var7, var3, var4, var5);
      }

      this.mesh.upload(var6);
      this.mesh.draw(var1, var2);
   }

   private void curtain(BufferBuilder var1, float[] var2, int var3, float var4, int var5, int var6) {
      float var8 = var2[0];
      float var9 = var2[1];
      float var10 = var2[2];
      float var11 = var2[3];

      for (int var7 = 0; var7 <= 64; var7++) {
         float var12 = var7 / 64.0F;
         float var13 = var8 + (var12 - 0.5F) * var10 + 3.0F * wave(var12 * 2.1F + var4 * 0.05F + var3);
         float var14 = var9 + 5.0F * MathHelper.method_15374(3.1415927F * var12) + 3.0F * wave(var12 * 3.7F - var4 * 0.07F + var3 * 2.0F);
         float var15 = smoothstep(var12 / 0.25F) * smoothstep((1.0F - var12) / 0.25F);
         float var16 = var15 * (0.55F + 0.45F * wave01(var12 * 5.3F + var4 * 0.11F + var3 * 1.3F));
         float var17 = 0.72F + 0.28F * wave01(var12 * 23.0F + var4 * 0.35F + var3);
         float var18 = var11 * (0.8F + 0.35F * wave01(var12 * 4.1F + var4 * 0.06F + var3 * 3.0F));
         float var19 = MathHelper.method_15362(var13 * 0.017453292F);
         float var20 = MathHelper.method_15374(var13 * 0.017453292F);

         for (int var21 = 0; var21 <= 6; var21++) {
            float var22 = var21 / 6.0F;
            float var23 = (var14 + var22 * var18) * 0.017453292F;
            float var24 = MathHelper.method_15362(var23);
            int var25 = var7 * 7 + var21;
            this.x[var25] = 100.0F * var24 * var20;
            this.y[var25] = 100.0F * MathHelper.method_15374(var23);
            this.z[var25] = -100.0F * var24 * var19;
            float var26 = var22 < 0.12F ? smoothstep(var22 / 0.12F) : (float)Math.pow(1.0F - (var22 - 0.12F) / 0.88F, 1.5);
            float var27 = MathHelper.method_15363(var16 * var17 * var26 * 0.8F, 0.0F, 1.0F);
            int var28 = ColorHelper.method_61319((float)Math.pow(var22, 0.7), var5, var6) & 16777215;
            this.color[var25] = Math.round(var27 * 255.0F) << 24 | var28;
         }
      }

      for (int var29 = 0; var29 < 64; var29++) {
         for (int var30 = 0; var30 < 6; var30++) {
            int var31 = var29 * 7 + var30;
            int var32 = (var29 + 1) * 7 + var30;
            this.vertex(var1, var31);
            this.vertex(var1, var32);
            this.vertex(var1, var32 + 1);
            this.vertex(var1, var31 + 1);
         }
      }
   }

   private void vertex(BufferBuilder var1, int var2) {
      SkyMesh.vertex(var1, this.x[var2], this.y[var2], this.z[var2], this.color[var2]);
   }

   static float wave(float var0) {
      return 0.5F * MathHelper.method_15374(var0) + 0.3F * MathHelper.method_15374(var0 * 2.13F + 1.3F) + 0.2F * MathHelper.method_15374(var0 * 4.37F + 2.9F);
   }

   static float wave01(float var0) {
      return 0.5F + 0.5F * wave(var0);
   }

   static float smoothstep(float var0) {
      var0 = MathHelper.method_15363(var0, 0.0F, 1.0F);
      return var0 * var0 * (3.0F - 2.0F * var0);
   }

   void close() {
      this.mesh.close();
   }
}
