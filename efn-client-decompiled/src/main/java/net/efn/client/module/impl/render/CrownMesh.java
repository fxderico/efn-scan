package net.efn.client.module.impl.render;

import java.util.Arrays;
import net.efn.client.util.MobilePerformance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;

@Environment(EnvType.CLIENT)
final class CrownMesh {
   static final int GOLD = 0;
   static final int INLAY = 1;
   static final int GEM = 2;
   static final int BRONZE = 3;
   static final int FUR = 4;
   static final int MATERIALS = 5;
   private static final float PX = 0.0625F;
   private static final float TWIST = 0.12F;
   private static final float[] CORNER_U = new float[]{0.0F, 1.0F, 1.0F, 0.0F};
   private static final float[] CORNER_V = new float[]{0.0F, 0.0F, 1.0F, 1.0F};
   private static final float BAND_TOP = -6.6F;
   private static final float BAND_BOTTOM = -5.3F;
   private final float[] pos;
   private final float[] normal;
   private final float[] sweep;
   private final byte[] material;
   private final float[] tone;
   private final int quads;
   private CrownMesh.Look coloredLook;
   private int[] cachedColors;

   CrownMesh(CrownMesh.Builder var1) {
      this.quads = var1.quads;
      this.pos = Arrays.copyOf(var1.pos, this.quads * 12);
      this.normal = Arrays.copyOf(var1.normal, this.quads * 3);
      this.sweep = Arrays.copyOf(var1.sweep, this.quads * 8);
      this.material = Arrays.copyOf(var1.material, this.quads);
      this.tone = Arrays.copyOf(var1.tone, this.quads);
   }

   boolean isEmpty() {
      return this.quads == 0;
   }

   void emit(Entry var1, VertexConsumer var2, int var3, CrownMesh.Look var4) {
      int[] var5 = null;
      if (MobilePerformance.active()) {
         if (this.cachedColors == null) {
            this.cachedColors = new int[this.quads * 4];
         }

         if (this.coloredLook != var4) {
            this.colorVertices(var4, this.cachedColors);
            this.coloredLook = var4;
         }

         var5 = this.cachedColors;
      } else {
         this.cachedColors = null;
         this.coloredLook = null;
      }

      int[] var6 = var4.palette();
      float var7 = var4.sweepGain();
      boolean var8 = var7 > 0.004F;
      float var9 = var4.sweepCos();
      float var10 = var4.sweepSin();

      for (int var11 = 0; var11 < this.quads; var11++) {
         byte var12 = this.material[var11];
         int var13 = var6[var12];
         float var14 = var12 == 2 ? this.tone[var11] * var4.gemPulse() : this.tone[var11];
         boolean var15 = var8 && (var12 == 0 || var12 == 3);
         int var16 = var12 == 2 && var4.glow() ? 15728880 : var3;
         int var17 = var11 * 3;
         float var18 = this.normal[var17];
         float var19 = this.normal[var17 + 1];
         float var20 = this.normal[var17 + 2];

         for (int var21 = 0; var21 < 4; var21++) {
            int var24 = var11 * 12 + var21 * 3;
            float var25 = var14;
            int var22;
            float var23;
            if (var5 == null && var15 && (var23 = this.sweep[var22 = var11 * 8 + var21 * 2] * var9 + this.sweep[var22 + 1] * var10) > 0.0F) {
               var23 *= var23;
               var23 *= var23;
               var23 *= var23;
               var25 = var14 + var7 * var23;
            }

            var2.method_56824(var1, this.pos[var24], this.pos[var24 + 1], this.pos[var24 + 2])
               .method_39415(var5 == null ? tint(var13, var25) : var5[var11 * 4 + var21])
               .method_22913(CORNER_U[var21], CORNER_V[var21])
               .method_22922(OverlayTexture.field_21444)
               .method_60803(var16)
               .method_60831(var1, var18, var19, var20);
         }
      }
   }

   private void colorVertices(CrownMesh.Look var1, int[] var2) {
      int[] var3 = var1.palette();
      float var4 = var1.sweepGain();
      float var5 = var1.sweepCos();
      float var6 = var1.sweepSin();
      boolean var7 = var4 > 0.004F;

      for (int var8 = 0; var8 < this.quads; var8++) {
         byte var9 = this.material[var8];
         int var10 = var3[var9];
         float var11 = var9 == 2 ? this.tone[var8] * var1.gemPulse() : this.tone[var8];
         boolean var12 = var7 && (var9 == 0 || var9 == 3);

         for (int var13 = 0; var13 < 4; var13++) {
            float var16 = var11;
            int var14;
            float var15;
            if (var12 && (var15 = this.sweep[var14 = var8 * 8 + var13 * 2] * var5 + this.sweep[var14 + 1] * var6) > 0.0F) {
               var15 *= var15;
               var15 *= var15;
               var15 *= var15;
               var16 = var11 + var4 * var15;
            }

            var2[var8 * 4 + var13] = tint(var10, var16);
         }
      }
   }

   private static int tint(int var0, float var1) {
      int var2 = var0 >> 16 & 0xFF;
      int var3 = var0 >> 8 & 0xFF;
      int var4 = var0 & 0xFF;
      if (var1 <= 1.0F) {
         var2 = (int)(var2 * var1);
         var3 = (int)(var3 * var1);
         var4 = (int)(var4 * var1);
      } else {
         float var5 = Math.min(1.0F, var1 - 1.0F);
         var2 += (int)((255 - var2) * var5);
         var3 += (int)((255 - var3) * var5);
         var4 += (int)((255 - var4) * var5);
      }

      return 0xFF000000 | var2 << 16 | var3 << 8 | var4;
   }

   static CrownMesh head(int var0, int var1, boolean var2) {
      CrownMesh.Builder var3 = new CrownMesh.Builder();
      float var4 = var2 ? 5.35F : 4.8F;
      float var5 = -4.0F - var4;
      switch (var0) {
         case 1:
            halo(var3, var5);
            break;
         case 2:
            shards(var3, var5);
            break;
         default:
            brow(var3, var4);
            crown(var3, var4);
      }

      return new CrownMesh(var3);
   }

   private static void spike(CrownMesh.Builder var0, float var1, float var2, float var3, float var4, float var5, int var6) {
      var0.slab(-6.6F, var1 - var3, var1 + var3, var2 - var4, var2 + var4, -6.6F - var5, var1 - 0.1F, var1 + 0.1F, var2 - 0.1F, var2 + 0.1F, var6);
   }

   private static void crown(CrownMesh.Builder var0, float var1) {
      float var2 = var1 + 0.075F;
      float var3 = var1 + 0.45F;
      spike(var0, 0.0F, -var2, 1.05F, 0.55F, 7.17F, 2);

      for (byte var4 = -1; var4 <= 1; var4 += 2) {
         spike(var0, var4 * 2.9F, -var2, 0.95F, 0.55F, 4.99F, 0);
         spike(var0, var4 * (var2 - 0.15F), -(var2 - 0.15F), 0.85F, 0.85F, 3.84F, 3);
         spike(var0, var4 * var2, 0.0F, 0.55F, 1.05F, 4.61F, 0);
         spike(var0, var4 * var2, 2.9F, 0.55F, 0.95F, 3.58F, 0);
         spike(var0, var4 * (var2 - 0.15F), var2 - 0.15F, 0.85F, 0.85F, 3.33F, 3);
         spike(var0, var4 * 2.9F, var2, 0.95F, 0.55F, 3.84F, 0);
      }

      spike(var0, 0.0F, var2, 1.05F, 0.55F, 4.61F, 0);
      var0.box(-0.65F, -6.45F, -var3 - 0.2F, 0.65F, -5.45F, -var3 + 0.05F, 2);
   }

   private static void halo(CrownMesh.Builder var0, float var1) {
      float var2 = var1 - 2.2F;
      float var3 = var2 - 0.3F;
      float var4 = var2 + 0.3F;
      float var5 = 3.5F;
      float var6 = 4.6F;
      float var7 = (var5 + var6) * 0.5F;
      byte var8 = 28;

      for (int var9 = 0; var9 < var8; var9++) {
         double var10 = 6.283185307179586 * var9 / var8;
         double var12 = 6.283185307179586 * (var9 + 1) / var8;
         double var14 = (var10 + var12) * 0.5;
         float var16 = var7 * (float)Math.cos(var14);
         float var17 = var7 * (float)Math.sin(var14);
         float var18 = var5 * (float)Math.cos(var10);
         float var19 = var5 * (float)Math.sin(var10);
         float var20 = var5 * (float)Math.cos(var12);
         float var21 = var5 * (float)Math.sin(var12);
         float var22 = var6 * (float)Math.cos(var10);
         float var23 = var6 * (float)Math.sin(var10);
         float var24 = var6 * (float)Math.cos(var12);
         float var25 = var6 * (float)Math.sin(var12);
         var0.quad(var18, var3, var19, var22, var3, var23, var24, var3, var25, var20, var3, var21, var16, var2, var17, 0);
         var0.quad(var18, var4, var19, var22, var4, var23, var24, var4, var25, var20, var4, var21, var16, var2, var17, 3);
         var0.quad(var22, var3, var23, var24, var3, var25, var24, var4, var25, var22, var4, var23, var16, var2, var17, 0);
         var0.quad(var18, var3, var19, var20, var3, var21, var20, var4, var21, var18, var4, var19, var16, var2, var17, 3);
      }

      for (int var26 = 0; var26 < 4; var26++) {
         double var27 = 1.5707963267948966 * var26;
         float var28 = var7 * (float)Math.cos(var27);
         float var13 = var7 * (float)Math.sin(var27);
         var0.box(var28 - 0.55F, var2 - 0.5F, var13 - 0.55F, var28 + 0.55F, var2 + 0.5F, var13 + 0.55F, 2);
      }
   }

   private static void shards(CrownMesh.Builder var0, float var1) {
      byte var2 = 9;
      float var3 = 5.9F;

      for (int var4 = 0; var4 < var2; var4++) {
         double var5 = 6.283185307179586 * var4 / var2;
         float var7 = var3 * (float)Math.cos(var5);
         float var8 = var3 * (float)Math.sin(var5);
         float var9 = 0.85F + 0.3F * (var4 * 5 % 3) / 2.0F;
         float var10 = var1 - 0.4F - (var4 % 2 == 0 ? 0.0F : 0.9F);
         float var11 = 0.95F * var9;
         var0.slab(
            var10, var7 - var11, var7 + var11, var8 - var11, var8 + var11, var10 - 2.8F * var9, var7 - 0.05F, var7 + 0.05F, var8 - 0.05F, var8 + 0.05F, 2
         );
         var0.slab(
            var10, var7 - var11, var7 + var11, var8 - var11, var8 + var11, var10 + 1.7F * var9, var7 - 0.05F, var7 + 0.05F, var8 - 0.05F, var8 + 0.05F, 2
         );
      }
   }

   static CrownMesh broadCollar(boolean var0, boolean var1) {
      return new CrownMesh(new CrownMesh.Builder());
   }

   private static float lift(boolean var0) {
      return var0 ? 1.15F : 0.2F;
   }

   private static void brow(CrownMesh.Builder var0, float var1) {
      float var2 = var1 + 0.45F;
      float var3 = var1 - 0.3F;
      var0.box(-var2, -6.6F, -var2, var2, -5.3F, -var3, 0);
      var0.box(-var2, -6.6F, var3, var2, -5.3F, var2, 0);
      var0.box(-var2, -6.6F, -var3, -var3, -5.3F, var3, 0);
      var0.box(var3, -6.6F, -var3, var2, -5.3F, var3, 0);

      for (byte var4 = -1; var4 <= 1; var4 += 2) {
         for (int var5 = 0; var5 < 2; var5++) {
            float var6 = var4 * (1.7F + 1.6F * var5);
            var0.box(var6 - 0.45F, -6.2999997F, -var2 - 0.12F, var6 + 0.45F, -5.6000004F, -var2 + 0.05F, 1);
         }
      }
   }

   private static void ring(float[] var0, float var1, float var2, float var3, float var4, float var5) {
      set(var0, 0, var2, var1, var4);
      set(var0, 1, var3, var1, var4);
      set(var0, 2, var3, var1, var5);
      set(var0, 3, var2, var1, var5);
   }

   private static void set(float[] var0, int var1, float var2, float var3, float var4) {
      var0[var1 * 3] = var2;
      var0[var1 * 3 + 1] = var3;
      var0[var1 * 3 + 2] = var4;
   }

   private static float lerp(float var0, float var1, float var2) {
      return var0 + (var1 - var0) * var2;
   }

   @Environment(EnvType.CLIENT)
   static final class Builder {
      private float[] pos = new float[3072];
      private float[] normal = new float[768];
      private float[] sweep = new float[2048];
      private byte[] material = new byte[256];
      private float[] tone = new float[256];
      private int quads;
      private float shade = 1.0F;
      private final float[] corners = new float[24];

      Builder() {
      }

      void shade(float var1) {
         this.shade = var1;
      }

      private void ensure() {
         if (this.quads >= this.material.length) {
            int var1 = this.material.length * 2;
            this.pos = Arrays.copyOf(this.pos, var1 * 12);
            this.normal = Arrays.copyOf(this.normal, var1 * 3);
            this.sweep = Arrays.copyOf(this.sweep, var1 * 8);
            this.material = Arrays.copyOf(this.material, var1);
            this.tone = Arrays.copyOf(this.tone, var1);
         }
      }

      void quad(
         float var1,
         float var2,
         float var3,
         float var4,
         float var5,
         float var6,
         float var7,
         float var8,
         float var9,
         float var10,
         float var11,
         float var12,
         float var13,
         float var14,
         float var15,
         int var16
      ) {
         float var17 = var8 - var2;
         float var18 = var12 - var6;
         float var19 = var9 - var3;
         float var20 = var11 - var5;
         float var21 = var17 * var18 - var19 * var20;
         float var22 = var10 - var4;
         float var23 = var7 - var1;
         float var24 = var19 * var22 - var23 * var18;
         float var25 = var23 * var20 - var17 * var22;
         float var26 = (float)Math.sqrt(var21 * var21 + var24 * var24 + var25 * var25);
         if (!(var26 < 1.0E-6F)) {
            float var27 = (var1 + var4 + var7 + var10) * 0.25F - var13;
            float var28 = (var2 + var5 + var8 + var11) * 0.25F - var14;
            float var29 = (var3 + var6 + var9 + var12) * 0.25F - var15;
            if ((var21 = var21 / var26) * var27 + (var24 = var24 / var26) * var28 + (var25 = var25 / var26) * var29 < 0.0F) {
               var21 = -var21;
               var24 = -var24;
               var25 = -var25;
            }

            this.ensure();
            int var30 = this.quads * 12;
            this.vertex(var30, var1, var2, var3);
            this.vertex(var30 + 3, var4, var5, var6);
            this.vertex(var30 + 6, var7, var8, var9);
            this.vertex(var30 + 9, var10, var11, var12);
            this.normal[this.quads * 3] = var21;
            this.normal[this.quads * 3 + 1] = var24;
            this.normal[this.quads * 3 + 2] = var25;
            this.material[this.quads] = (byte)var16;
            int var31 = this.quads * -1640531535;
            var31 ^= var31 >>> 15;
            this.tone[this.quads] = this.shade * (0.955F + 0.065F * ((var31 & 0xFF) / 255.0F));
            this.quads++;
         }
      }

      private void vertex(int var1, float var2, float var3, float var4) {
         this.pos[var1] = var2 * 0.0625F;
         this.pos[var1 + 1] = var3 * 0.0625F;
         this.pos[var1 + 2] = var4 * 0.0625F;
         double var5 = Math.atan2(var2, -var4) + var3 * 0.12F;
         int var7 = this.quads * 8 + (var1 - this.quads * 12) / 3 * 2;
         this.sweep[var7] = (float)Math.cos(var5);
         this.sweep[var7 + 1] = (float)Math.sin(var5);
      }

      void prism(float[] var1, int var2) {
         float var3 = 0.0F;
         float var4 = 0.0F;
         float var5 = 0.0F;

         for (int var6 = 0; var6 < 8; var6++) {
            var3 += var1[var6 * 3];
            var4 += var1[var6 * 3 + 1];
            var5 += var1[var6 * 3 + 2];
         }

         float var7;
         float var8;
         float var9;
         this.face(var1, 0, 1, 2, 3, var7 = var3 * 0.125F, var8 = var4 * 0.125F, var9 = var5 * 0.125F, var2);
         this.face(var1, 4, 5, 6, 7, var7, var8, var9, var2);
         this.face(var1, 0, 1, 5, 4, var7, var8, var9, var2);
         this.face(var1, 1, 2, 6, 5, var7, var8, var9, var2);
         this.face(var1, 2, 3, 7, 6, var7, var8, var9, var2);
         this.face(var1, 3, 0, 4, 7, var7, var8, var9, var2);
      }

      private void face(float[] var1, int var2, int var3, int var4, int var5, float var6, float var7, float var8, int var9) {
         this.quad(
            var1[var2 * 3],
            var1[var2 * 3 + 1],
            var1[var2 * 3 + 2],
            var1[var3 * 3],
            var1[var3 * 3 + 1],
            var1[var3 * 3 + 2],
            var1[var4 * 3],
            var1[var4 * 3 + 1],
            var1[var4 * 3 + 2],
            var1[var5 * 3],
            var1[var5 * 3 + 1],
            var1[var5 * 3 + 2],
            var6,
            var7,
            var8,
            var9
         );
      }

      void slab(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, int var11) {
         float[] var12 = this.corners;
         CrownMesh.set(var12, 0, var2, var1, var4);
         CrownMesh.set(var12, 1, var3, var1, var4);
         CrownMesh.set(var12, 2, var3, var1, var5);
         CrownMesh.set(var12, 3, var2, var1, var5);
         CrownMesh.set(var12, 4, var7, var6, var9);
         CrownMesh.set(var12, 5, var8, var6, var9);
         CrownMesh.set(var12, 6, var8, var6, var10);
         CrownMesh.set(var12, 7, var7, var6, var10);
         this.prism(var12, var11);
      }

      void box(float var1, float var2, float var3, float var4, float var5, float var6, int var7) {
         this.slab(var2, var1, var4, var3, var6, var5, var1, var4, var3, var6, var7);
      }

      void stripedSlab(
         float var1,
         float var2,
         float var3,
         float var4,
         float var5,
         float var6,
         float var7,
         float var8,
         float var9,
         float var10,
         int var11,
         int var12,
         int var13
      ) {
         float[] var14 = new float[12];
         float[] var15 = new float[12];
         CrownMesh.ring(var14, var1, var2, var3, var4, var5);
         CrownMesh.ring(var15, var6, var7, var8, var9, var10);
         this.striped(var14, var15, var11, var12, var13);
      }

      void striped(float[] var1, float[] var2, int var3, int var4, int var5) {
         float[] var6 = this.corners;

         for (int var7 = 0; var7 < var3; var7++) {
            float var8 = (float)var7 / var3;
            float var9 = (float)(var7 + 1) / var3;

            for (int var10 = 0; var10 < 12; var10++) {
               var6[var10] = CrownMesh.lerp(var1[var10], var2[var10], var8);
               var6[12 + var10] = CrownMesh.lerp(var1[var10], var2[var10], var9);
            }

            this.prism(var6, (var7 & 1) == 0 ? var4 : var5);
         }
      }

      void studs(
         float var1,
         float var2,
         float var3,
         float var4,
         float var5,
         float var6,
         float var7,
         float var8,
         float var9,
         float var10,
         float var11,
         float var12,
         boolean var13,
         boolean var14,
         float var15,
         float var16,
         float var17
      ) {
         int var18 = var13 ? 3 : 2;

         for (int var19 = 0; var19 < var18; var19++) {
            float var20 = var13 ? 0.2F + 0.3F * var19 : 0.35F + 0.3F * var19;
            if (!var14 || !var13 || var19 != 1) {
               this.stud(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, var20, 0.5F, var15, var16, var17);
            }
         }
      }

      private void stud(
         float var1,
         float var2,
         float var3,
         float var4,
         float var5,
         float var6,
         float var7,
         float var8,
         float var9,
         float var10,
         float var11,
         float var12,
         float var13,
         float var14,
         float var15,
         float var16,
         float var17
      ) {
         float var21 = CrownMesh.lerp(CrownMesh.lerp(var1, var4, var13), CrownMesh.lerp(var10, var7, var13), var14);
         float var22 = CrownMesh.lerp(CrownMesh.lerp(var2, var5, var13), CrownMesh.lerp(var11, var8, var13), var14);
         float var23 = CrownMesh.lerp(CrownMesh.lerp(var3, var6, var13), CrownMesh.lerp(var12, var9, var13), var14);
         float var24 = var4 - var1;
         float var25 = var5 - var2;
         float var26 = var6 - var3;
         float var27 = (float)Math.sqrt(var24 * var24 + var25 * var25 + var26 * var26);
         float var28 = var10 - var1;
         float var29 = var11 - var2;
         float var30 = var12 - var3;
         float var31 = (float)Math.sqrt(var28 * var28 + var29 * var29 + var30 * var30);
         if (!(var27 < 1.0E-5F) && !(var31 < 1.0E-5F)) {
            float var41;
            float var42;
            float var44;
            float var45;
            float var32 = (var41 = var25 / var27) * (var45 = var30 / var31) - (var42 = var26 / var27) * (var44 = var29 / var31);
            float var18;
            float var19;
            float var20;
            if (!(
               (
                     var20 = (float)Math.sqrt(
                        var32 * var32
                           + (var19 = var42 * (var28 = var28 / var31) - (var24 = var24 / var27) * var45) * var19
                           + (var18 = var24 * var44 - var41 * var28) * var18
                     )
                  )
                  < 1.0E-5F
            )) {
               if ((var32 = var32 / var20) * (var21 - var15) + (var19 = var19 / var20) * (var22 - var16) + (var18 = var18 / var20) * (var23 - var17) < 0.0F) {
                  var32 = -var32;
                  var19 = -var19;
                  var18 = -var18;
               }

               float var33 = 0.32F;
               float var34 = 0.07F;
               float var37;
               float var38;
               float var39;
               this.quad(
                  (var37 = var21 + var32 * var34) - var24 * var33 - var28 * var33,
                  (var38 = var22 + var19 * var34) - var41 * var33 - var44 * var33,
                  (var39 = var23 + var18 * var34) - var42 * var33 - var45 * var33,
                  var37 + var24 * var33 - var28 * var33,
                  var38 + var41 * var33 - var44 * var33,
                  var39 + var42 * var33 - var45 * var33,
                  var37 + var24 * var33 + var28 * var33,
                  var38 + var41 * var33 + var44 * var33,
                  var39 + var42 * var33 + var45 * var33,
                  var37 - var24 * var33 + var28 * var33,
                  var38 - var41 * var33 + var44 * var33,
                  var39 - var42 * var33 + var45 * var33,
                  var37 - var32,
                  var38 - var19,
                  var39 - var18,
                  0
               );
            }
         }
      }

      void tube(float[] var1, float[] var2, int var3, int var4) {
         float[] var5 = this.corners;

         for (int var6 = 0; var6 + 1 < var3; var6++) {
            section(var5, 0, var1, var2, var3, var6);
            section(var5, 4, var1, var2, var3, var6 + 1);
            this.prism(var5, var6 + 2 == var3 ? 3 : var4);
         }
      }

      private static void section(float[] var0, int var1, float[] var2, float[] var3, int var4, int var5) {
         int var7 = Math.max(0, var5 - 1);
         int var8 = Math.min(var4 - 1, var5 + 1);
         float var9 = var2[var8 * 3] - var2[var7 * 3];
         float var6;
         float var10 = (float)Math.sqrt(var9 * var9 + (var6 = var2[var8 * 3 + 1] - var2[var7 * 3 + 1]) * var6);
         float var11 = var10 > 1.0E-5F ? -var6 / var10 : 1.0F;
         float var12 = var10 > 1.0E-5F ? var9 / var10 : 0.0F;
         float var13 = var3[var5];
         float var14 = var2[var5 * 3];
         float var15 = var2[var5 * 3 + 1];
         float var16 = var2[var5 * 3 + 2];
         CrownMesh.set(var0, var1, var14 + var11 * var13, var15 + var12 * var13, var16 - var13);
         CrownMesh.set(var0, var1 + 1, var14 + var11 * var13, var15 + var12 * var13, var16 + var13);
         CrownMesh.set(var0, var1 + 2, var14 - var11 * var13, var15 - var12 * var13, var16 + var13);
         CrownMesh.set(var0, var1 + 3, var14 - var11 * var13, var15 - var12 * var13, var16 - var13);
      }

      void disc(float var1, float var2, float var3, float var4, float var5, int var6, int var7, int var8) {
         float var9 = var3 - var5;
         float var10 = var3 + var5;

         for (int var11 = 0; var11 < var6; var11++) {
            double var12 = 6.283185307179586 * var11 / var6;
            double var14 = 6.283185307179586 * (var11 + 1) / var6;
            float var16 = var1 + var4 * (float)Math.cos(var12);
            float var17 = var2 + var4 * (float)Math.sin(var12);
            float var18 = var1 + var4 * (float)Math.cos(var14);
            float var19 = var2 + var4 * (float)Math.sin(var14);
            this.quad(var1, var2, var9, var16, var17, var9, var18, var19, var9, var18, var19, var9, var1, var2, var3, var7);
            this.quad(var1, var2, var10, var16, var17, var10, var18, var19, var10, var18, var19, var10, var1, var2, var3, var7);
            this.quad(var16, var17, var9, var18, var19, var9, var18, var19, var10, var16, var17, var10, var1, var2, var3, var8);
         }
      }

      void annulus(float var1, float var2, float var3, float var4, float var5, int var6, int var7, float var8) {
         for (int var9 = 0; var9 < var6; var9++) {
            double var10 = 6.283185307179586 * var9 / var6;
            double var12 = 6.283185307179586 * (var9 + 1) / var6;
            float var14 = (float)Math.cos(var10);
            float var15 = (float)Math.sin(var10);
            float var16 = (float)Math.cos(var12);
            float var17 = (float)Math.sin(var12);
            this.quad(
               var1 + var14 * var4,
               var2 + var15 * var4,
               var3,
               var1 + var14 * var5,
               var2 + var15 * var5,
               var3,
               var1 + var16 * var5,
               var2 + var17 * var5,
               var3,
               var1 + var16 * var4,
               var2 + var17 * var4,
               var3,
               var1,
               var2,
               var8,
               var7
            );
         }
      }
   }

   @Environment(EnvType.CLIENT)
   record Look(int[] palette, boolean glow, float sweepCos, float sweepSin, float sweepGain, float gemPulse) {
   }
}
