package net.efn.client.module.impl.render.character.kit;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Surface {
   public final float[] pos;
   public final float[] nrm;
   public final float[] uv;
   public final int[] color;
   public final byte[] glow;
   public final float[] flat;
   public final int[] quads;
   public final int triangles;
   public float[] weight;
   public float[] joint;

   Surface(float[] fs, float[] gs, float[] hs, int[] is, byte[] bs, float[] js, int[] ks, int i) {
      this.pos = fs;
      this.nrm = gs;
      this.uv = hs;
      this.color = is;
      this.glow = bs;
      this.flat = js;
      this.quads = ks;
      this.triangles = i;
   }

   public int vertexCount() {
      return this.pos.length / 3;
   }

   public int quadCount() {
      return this.quads.length / 4;
   }

   public boolean glows() {
      return this.glow != null;
   }

   @Environment(EnvType.CLIENT)
   static final class Builder {
      float[] pos = new float[768];
      float[] nrm = new float[768];
      float[] uv = new float[512];
      int[] color = new int[256];
      byte[] glow = new byte[256];
      float[] flat = new float[256];
      boolean anyGlow;
      boolean anyFlat;
      int verts;
      int[] quads = new int[1024];
      int quadCount;
      int triangles;

      Builder() {
      }

      int vertex(float f, float g, float h, float i, float j, float k, float l, float m, int n, boolean bl) {
         return this.vertex(f, g, h, i, j, k, l, m, n, bl, 0.0F);
      }

      int vertex(float f, float g, float h, float i, float j, float k, float l, float m, int n, boolean bl, float o) {
         if (this.verts == this.color.length) {
            int var12 = this.verts * 2;
            this.pos = Arrays.copyOf(this.pos, var12 * 3);
            this.nrm = Arrays.copyOf(this.nrm, var12 * 3);
            this.uv = Arrays.copyOf(this.uv, var12 * 2);
            this.color = Arrays.copyOf(this.color, var12);
            this.glow = Arrays.copyOf(this.glow, var12);
            this.flat = Arrays.copyOf(this.flat, var12);
         }

         int var13 = this.verts++;
         this.pos[var13 * 3] = f;
         this.pos[var13 * 3 + 1] = g;
         this.pos[var13 * 3 + 2] = h;
         this.nrm[var13 * 3] = i;
         this.nrm[var13 * 3 + 1] = j;
         this.nrm[var13 * 3 + 2] = k;
         this.uv[var13 * 2] = l;
         this.uv[var13 * 2 + 1] = m;
         this.color[var13] = n;
         this.glow[var13] = (byte)(bl ? 1 : 0);
         this.flat[var13] = o;
         this.anyGlow |= bl;
         this.anyFlat |= o > 0.0F;
         return var13;
      }

      void quad(int i, int j, int k, int l, int m) {
         if (this.quadCount * 4 + 4 > this.quads.length) {
            this.quads = Arrays.copyOf(this.quads, this.quads.length * 2);
         }

         this.quads[this.quadCount * 4] = i;
         this.quads[this.quadCount * 4 + 1] = j;
         this.quads[this.quadCount * 4 + 2] = k;
         this.quads[this.quadCount * 4 + 3] = l;
         this.quadCount++;
         this.triangles += m;
      }

      Surface build() {
         return this.verts != 0 && this.quadCount != 0
            ? new Surface(
               Arrays.copyOf(this.pos, this.verts * 3),
               Arrays.copyOf(this.nrm, this.verts * 3),
               Arrays.copyOf(this.uv, this.verts * 2),
               Arrays.copyOf(this.color, this.verts),
               this.anyGlow ? Arrays.copyOf(this.glow, this.verts) : null,
               this.anyFlat ? Arrays.copyOf(this.flat, this.verts) : null,
               Arrays.copyOf(this.quads, this.quadCount * 4),
               this.triangles
            )
            : null;
      }
   }
}
