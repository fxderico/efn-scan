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

   CrownMesh(CrownMesh.Builder builder) {
      this.quads = builder.quads;
      this.pos = Arrays.copyOf(builder.pos, this.quads * 12);
      this.normal = Arrays.copyOf(builder.normal, this.quads * 3);
      this.sweep = Arrays.copyOf(builder.sweep, this.quads * 8);
      this.material = Arrays.copyOf(builder.material, this.quads);
      this.tone = Arrays.copyOf(builder.tone, this.quads);
   }

   boolean isEmpty() {
      return this.quads == 0;
   }

   void emit(Entry entry, VertexConsumer vertexConsumer, int i, CrownMesh.Look look) {
      int[] var5 = null;
      if (MobilePerformance.active()) {
         if (this.cachedColors == null) {
            this.cachedColors = new int[this.quads * 4];
         }

         if (this.coloredLook != look) {
            this.colorVertices(look, this.cachedColors);
            this.coloredLook = look;
         }

         var5 = this.cachedColors;
      } else {
         this.cachedColors = null;
         this.coloredLook = null;
      }

      int[] var6 = look.palette();
      float var7 = look.sweepGain();
      boolean var8 = var7 > 0.004F;
      float var9 = look.sweepCos();
      float var10 = look.sweepSin();

      for (int var11 = 0; var11 < this.quads; var11++) {
         byte var12 = this.material[var11];
         int var13 = var6[var12];
         float var14 = var12 == 2 ? this.tone[var11] * look.gemPulse() : this.tone[var11];
         boolean var15 = var8 && (var12 == 0 || var12 == 3);
         int var16 = var12 == 2 && look.glow() ? 15728880 : i;
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

            vertexConsumer.vertex(entry, this.pos[var24], this.pos[var24 + 1], this.pos[var24 + 2])
               .color(var5 == null ? tint(var13, var25) : var5[var11 * 4 + var21])
               .texture(CORNER_U[var21], CORNER_V[var21])
               .overlay(OverlayTexture.DEFAULT_UV)
               .light(var16)
               .normal(entry, var18, var19, var20);
         }
      }
   }

   private void colorVertices(CrownMesh.Look look, int[] is) {
      int[] var3 = look.palette();
      float var4 = look.sweepGain();
      float var5 = look.sweepCos();
      float var6 = look.sweepSin();
      boolean var7 = var4 > 0.004F;

      for (int var8 = 0; var8 < this.quads; var8++) {
         byte var9 = this.material[var8];
         int var10 = var3[var9];
         float var11 = var9 == 2 ? this.tone[var8] * look.gemPulse() : this.tone[var8];
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

            is[var8 * 4 + var13] = tint(var10, var16);
         }
      }
   }

   private static int tint(int i, float f) {
      int var2 = i >> 16 & 0xFF;
      int var3 = i >> 8 & 0xFF;
      int var4 = i & 0xFF;
      if (f <= 1.0F) {
         var2 = (int)(var2 * f);
         var3 = (int)(var3 * f);
         var4 = (int)(var4 * f);
      } else {
         float var5 = Math.min(1.0F, f - 1.0F);
         var2 += (int)((255 - var2) * var5);
         var3 += (int)((255 - var3) * var5);
         var4 += (int)((255 - var4) * var5);
      }

      return 0xFF000000 | var2 << 16 | var3 << 8 | var4;
   }

   static CrownMesh head(int i, int j, boolean bl) {
      CrownMesh.Builder var3 = new CrownMesh.Builder();
      float var4 = bl ? 5.35F : 4.8F;
      float var5 = -4.0F - var4;
      switch (i) {
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

   private static void spike(CrownMesh.Builder builder, float f, float g, float h, float i, float j, int k) {
      builder.slab(-6.6F, f - h, f + h, g - i, g + i, -6.6F - j, f - 0.1F, f + 0.1F, g - 0.1F, g + 0.1F, k);
   }

   private static void crown(CrownMesh.Builder builder, float f) {
      float var2 = f + 0.075F;
      float var3 = f + 0.45F;
      spike(builder, 0.0F, -var2, 1.05F, 0.55F, 7.17F, 2);

      for (byte var4 = -1; var4 <= 1; var4 += 2) {
         spike(builder, var4 * 2.9F, -var2, 0.95F, 0.55F, 4.99F, 0);
         spike(builder, var4 * (var2 - 0.15F), -(var2 - 0.15F), 0.85F, 0.85F, 3.84F, 3);
         spike(builder, var4 * var2, 0.0F, 0.55F, 1.05F, 4.61F, 0);
         spike(builder, var4 * var2, 2.9F, 0.55F, 0.95F, 3.58F, 0);
         spike(builder, var4 * (var2 - 0.15F), var2 - 0.15F, 0.85F, 0.85F, 3.33F, 3);
         spike(builder, var4 * 2.9F, var2, 0.95F, 0.55F, 3.84F, 0);
      }

      spike(builder, 0.0F, var2, 1.05F, 0.55F, 4.61F, 0);
      builder.box(-0.65F, -6.45F, -var3 - 0.2F, 0.65F, -5.45F, -var3 + 0.05F, 2);
   }

   private static void halo(CrownMesh.Builder builder, float f) {
      float var2 = f - 2.2F;
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
         builder.quad(var18, var3, var19, var22, var3, var23, var24, var3, var25, var20, var3, var21, var16, var2, var17, 0);
         builder.quad(var18, var4, var19, var22, var4, var23, var24, var4, var25, var20, var4, var21, var16, var2, var17, 3);
         builder.quad(var22, var3, var23, var24, var3, var25, var24, var4, var25, var22, var4, var23, var16, var2, var17, 0);
         builder.quad(var18, var3, var19, var20, var3, var21, var20, var4, var21, var18, var4, var19, var16, var2, var17, 3);
      }

      for (int var26 = 0; var26 < 4; var26++) {
         double var27 = 1.5707963267948966 * var26;
         float var28 = var7 * (float)Math.cos(var27);
         float var13 = var7 * (float)Math.sin(var27);
         builder.box(var28 - 0.55F, var2 - 0.5F, var13 - 0.55F, var28 + 0.55F, var2 + 0.5F, var13 + 0.55F, 2);
      }
   }

   private static void shards(CrownMesh.Builder builder, float f) {
      byte var2 = 9;
      float var3 = 5.9F;

      for (int var4 = 0; var4 < var2; var4++) {
         double var5 = 6.283185307179586 * var4 / var2;
         float var7 = var3 * (float)Math.cos(var5);
         float var8 = var3 * (float)Math.sin(var5);
         float var9 = 0.85F + 0.3F * (var4 * 5 % 3) / 2.0F;
         float var10 = f - 0.4F - (var4 % 2 == 0 ? 0.0F : 0.9F);
         float var11 = 0.95F * var9;
         builder.slab(
            var10, var7 - var11, var7 + var11, var8 - var11, var8 + var11, var10 - 2.8F * var9, var7 - 0.05F, var7 + 0.05F, var8 - 0.05F, var8 + 0.05F, 2
         );
         builder.slab(
            var10, var7 - var11, var7 + var11, var8 - var11, var8 + var11, var10 + 1.7F * var9, var7 - 0.05F, var7 + 0.05F, var8 - 0.05F, var8 + 0.05F, 2
         );
      }
   }

   static CrownMesh broadCollar(boolean bl, boolean bl2) {
      return new CrownMesh(new CrownMesh.Builder());
   }

   private static float lift(boolean bl) {
      return bl ? 1.15F : 0.2F;
   }

   private static void brow(CrownMesh.Builder builder, float f) {
      float var2 = f + 0.45F;
      float var3 = f - 0.3F;
      builder.box(-var2, -6.6F, -var2, var2, -5.3F, -var3, 0);
      builder.box(-var2, -6.6F, var3, var2, -5.3F, var2, 0);
      builder.box(-var2, -6.6F, -var3, -var3, -5.3F, var3, 0);
      builder.box(var3, -6.6F, -var3, var2, -5.3F, var3, 0);

      for (byte var4 = -1; var4 <= 1; var4 += 2) {
         for (int var5 = 0; var5 < 2; var5++) {
            float var6 = var4 * (1.7F + 1.6F * var5);
            builder.box(var6 - 0.45F, -6.2999997F, -var2 - 0.12F, var6 + 0.45F, -5.6000004F, -var2 + 0.05F, 1);
         }
      }
   }

   private static void ring(float[] fs, float f, float g, float h, float i, float j) {
      set(fs, 0, g, f, i);
      set(fs, 1, h, f, i);
      set(fs, 2, h, f, j);
      set(fs, 3, g, f, j);
   }

   private static void set(float[] fs, int i, float f, float g, float h) {
      fs[i * 3] = f;
      fs[i * 3 + 1] = g;
      fs[i * 3 + 2] = h;
   }

   private static float lerp(float f, float g, float h) {
      return f + (g - f) * h;
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

      void shade(float f) {
         this.shade = f;
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

      void quad(float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, float p, float q, float r, float s, float t, int u) {
         float var17 = m - g;
         float var18 = q - k;
         float var19 = n - h;
         float var20 = p - j;
         float var21 = var17 * var18 - var19 * var20;
         float var22 = o - i;
         float var23 = l - f;
         float var24 = var19 * var22 - var23 * var18;
         float var25 = var23 * var20 - var17 * var22;
         float var26 = (float)Math.sqrt(var21 * var21 + var24 * var24 + var25 * var25);
         if (!(var26 < 1.0E-6F)) {
            float var27 = (f + i + l + o) * 0.25F - r;
            float var28 = (g + j + m + p) * 0.25F - s;
            float var29 = (h + k + n + q) * 0.25F - t;
            if ((var21 = var21 / var26) * var27 + (var24 = var24 / var26) * var28 + (var25 = var25 / var26) * var29 < 0.0F) {
               var21 = -var21;
               var24 = -var24;
               var25 = -var25;
            }

            this.ensure();
            int var30 = this.quads * 12;
            this.vertex(var30, f, g, h);
            this.vertex(var30 + 3, i, j, k);
            this.vertex(var30 + 6, l, m, n);
            this.vertex(var30 + 9, o, p, q);
            this.normal[this.quads * 3] = var21;
            this.normal[this.quads * 3 + 1] = var24;
            this.normal[this.quads * 3 + 2] = var25;
            this.material[this.quads] = (byte)u;
            int var31 = this.quads * -1640531535;
            var31 ^= var31 >>> 15;
            this.tone[this.quads] = this.shade * (0.955F + 0.065F * ((var31 & 0xFF) / 255.0F));
            this.quads++;
         }
      }

      private void vertex(int i, float f, float g, float h) {
         this.pos[i] = f * 0.0625F;
         this.pos[i + 1] = g * 0.0625F;
         this.pos[i + 2] = h * 0.0625F;
         double var5 = Math.atan2(f, -h) + g * 0.12F;
         int var7 = this.quads * 8 + (i - this.quads * 12) / 3 * 2;
         this.sweep[var7] = (float)Math.cos(var5);
         this.sweep[var7 + 1] = (float)Math.sin(var5);
      }

      void prism(float[] fs, int i) {
         float var3 = 0.0F;
         float var4 = 0.0F;
         float var5 = 0.0F;

         for (int var6 = 0; var6 < 8; var6++) {
            var3 += fs[var6 * 3];
            var4 += fs[var6 * 3 + 1];
            var5 += fs[var6 * 3 + 2];
         }

         float var7;
         float var8;
         float var9;
         this.face(fs, 0, 1, 2, 3, var7 = var3 * 0.125F, var8 = var4 * 0.125F, var9 = var5 * 0.125F, i);
         this.face(fs, 4, 5, 6, 7, var7, var8, var9, i);
         this.face(fs, 0, 1, 5, 4, var7, var8, var9, i);
         this.face(fs, 1, 2, 6, 5, var7, var8, var9, i);
         this.face(fs, 2, 3, 7, 6, var7, var8, var9, i);
         this.face(fs, 3, 0, 4, 7, var7, var8, var9, i);
      }

      private void face(float[] fs, int i, int j, int k, int l, float f, float g, float h, int m) {
         this.quad(
            fs[i * 3],
            fs[i * 3 + 1],
            fs[i * 3 + 2],
            fs[j * 3],
            fs[j * 3 + 1],
            fs[j * 3 + 2],
            fs[k * 3],
            fs[k * 3 + 1],
            fs[k * 3 + 2],
            fs[l * 3],
            fs[l * 3 + 1],
            fs[l * 3 + 2],
            f,
            g,
            h,
            m
         );
      }

      void slab(float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, int p) {
         float[] var12 = this.corners;
         CrownMesh.set(var12, 0, g, f, i);
         CrownMesh.set(var12, 1, h, f, i);
         CrownMesh.set(var12, 2, h, f, j);
         CrownMesh.set(var12, 3, g, f, j);
         CrownMesh.set(var12, 4, l, k, n);
         CrownMesh.set(var12, 5, m, k, n);
         CrownMesh.set(var12, 6, m, k, o);
         CrownMesh.set(var12, 7, l, k, o);
         this.prism(var12, p);
      }

      void box(float f, float g, float h, float i, float j, float k, int l) {
         this.slab(g, f, i, h, k, j, f, i, h, k, l);
      }

      void stripedSlab(float f, float g, float h, float i, float j, float k, float l, float m, float n, float o, int p, int q, int r) {
         float[] var14 = new float[12];
         float[] var15 = new float[12];
         CrownMesh.ring(var14, f, g, h, i, j);
         CrownMesh.ring(var15, k, l, m, n, o);
         this.striped(var14, var15, p, q, r);
      }

      void striped(float[] fs, float[] gs, int i, int j, int k) {
         float[] var6 = this.corners;

         for (int var7 = 0; var7 < i; var7++) {
            float var8 = (float)var7 / i;
            float var9 = (float)(var7 + 1) / i;

            for (int var10 = 0; var10 < 12; var10++) {
               var6[var10] = CrownMesh.lerp(fs[var10], gs[var10], var8);
               var6[12 + var10] = CrownMesh.lerp(fs[var10], gs[var10], var9);
            }

            this.prism(var6, (var7 & 1) == 0 ? j : k);
         }
      }

      void studs(
         float f,
         float g,
         float h,
         float i,
         float j,
         float k,
         float l,
         float m,
         float n,
         float o,
         float p,
         float q,
         boolean bl,
         boolean bl2,
         float r,
         float s,
         float t
      ) {
         int var18 = bl ? 3 : 2;

         for (int var19 = 0; var19 < var18; var19++) {
            float var20 = bl ? 0.2F + 0.3F * var19 : 0.35F + 0.3F * var19;
            if (!bl2 || !bl || var19 != 1) {
               this.stud(f, g, h, i, j, k, l, m, n, o, p, q, var20, 0.5F, r, s, t);
            }
         }
      }

      private void stud(
         float f,
         float g,
         float h,
         float i,
         float j,
         float k,
         float l,
         float m,
         float n,
         float o,
         float p,
         float q,
         float r,
         float s,
         float t,
         float u,
         float v
      ) {
         float var21 = CrownMesh.lerp(CrownMesh.lerp(f, i, r), CrownMesh.lerp(o, l, r), s);
         float var22 = CrownMesh.lerp(CrownMesh.lerp(g, j, r), CrownMesh.lerp(p, m, r), s);
         float var23 = CrownMesh.lerp(CrownMesh.lerp(h, k, r), CrownMesh.lerp(q, n, r), s);
         float var24 = i - f;
         float var25 = j - g;
         float var26 = k - h;
         float var27 = (float)Math.sqrt(var24 * var24 + var25 * var25 + var26 * var26);
         float var28 = o - f;
         float var29 = p - g;
         float var30 = q - h;
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
               if ((var32 = var32 / var20) * (var21 - t) + (var19 = var19 / var20) * (var22 - u) + (var18 = var18 / var20) * (var23 - v) < 0.0F) {
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

      void tube(float[] fs, float[] gs, int i, int j) {
         float[] var5 = this.corners;

         for (int var6 = 0; var6 + 1 < i; var6++) {
            section(var5, 0, fs, gs, i, var6);
            section(var5, 4, fs, gs, i, var6 + 1);
            this.prism(var5, var6 + 2 == i ? 3 : j);
         }
      }

      private static void section(float[] fs, int i, float[] gs, float[] hs, int j, int k) {
         int var7 = Math.max(0, k - 1);
         int var8 = Math.min(j - 1, k + 1);
         float var9 = gs[var8 * 3] - gs[var7 * 3];
         float var6;
         float var10 = (float)Math.sqrt(var9 * var9 + (var6 = gs[var8 * 3 + 1] - gs[var7 * 3 + 1]) * var6);
         float var11 = var10 > 1.0E-5F ? -var6 / var10 : 1.0F;
         float var12 = var10 > 1.0E-5F ? var9 / var10 : 0.0F;
         float var13 = hs[k];
         float var14 = gs[k * 3];
         float var15 = gs[k * 3 + 1];
         float var16 = gs[k * 3 + 2];
         CrownMesh.set(fs, i, var14 + var11 * var13, var15 + var12 * var13, var16 - var13);
         CrownMesh.set(fs, i + 1, var14 + var11 * var13, var15 + var12 * var13, var16 + var13);
         CrownMesh.set(fs, i + 2, var14 - var11 * var13, var15 - var12 * var13, var16 + var13);
         CrownMesh.set(fs, i + 3, var14 - var11 * var13, var15 - var12 * var13, var16 - var13);
      }

      void disc(float f, float g, float h, float i, float j, int k, int l, int m) {
         float var9 = h - j;
         float var10 = h + j;

         for (int var11 = 0; var11 < k; var11++) {
            double var12 = 6.283185307179586 * var11 / k;
            double var14 = 6.283185307179586 * (var11 + 1) / k;
            float var16 = f + i * (float)Math.cos(var12);
            float var17 = g + i * (float)Math.sin(var12);
            float var18 = f + i * (float)Math.cos(var14);
            float var19 = g + i * (float)Math.sin(var14);
            this.quad(f, g, var9, var16, var17, var9, var18, var19, var9, var18, var19, var9, f, g, h, l);
            this.quad(f, g, var10, var16, var17, var10, var18, var19, var10, var18, var19, var10, f, g, h, l);
            this.quad(var16, var17, var9, var18, var19, var9, var18, var19, var10, var16, var17, var10, f, g, h, m);
         }
      }

      void annulus(float f, float g, float h, float i, float j, int k, int l, float m) {
         for (int var9 = 0; var9 < k; var9++) {
            double var10 = 6.283185307179586 * var9 / k;
            double var12 = 6.283185307179586 * (var9 + 1) / k;
            float var14 = (float)Math.cos(var10);
            float var15 = (float)Math.sin(var10);
            float var16 = (float)Math.cos(var12);
            float var17 = (float)Math.sin(var12);
            this.quad(
               f + var14 * i, g + var15 * i, h, f + var14 * j, g + var15 * j, h, f + var16 * j, g + var17 * j, h, f + var16 * i, g + var17 * i, h, f, g, m, l
            );
         }
      }
   }

   @Environment(EnvType.CLIENT)
   record Look(int[] palette, boolean glow, float sweepCos, float sweepSin, float sweepGain, float gemPulse) {
   }
}
