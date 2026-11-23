package net.efn.client.module.impl.render.character.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class PosedMesh {
   private static final int MAX_TRIANGLES = 4000000;
   final GltfFile file;
   final float[][] globals;
   final int[] parent;
   final Set<Integer> joints = new HashSet<>();
   float[] pos = new float[0];
   float[] nrm = new float[0];
   float[] uv = new float[0];
   int[] material = new int[0];
   int[] color = new int[0];
   boolean colored;
   int[] influence = new int[0];
   float[] weight = new float[0];
   int[] tris = new int[0];
   int vertices;
   int triangles;
   private float scale = 1.0F;
   private float shiftX;
   private float shiftY;
   private float shiftZ;
   private boolean flipped;

   private PosedMesh(GltfFile var1) {
      this.file = var1;
      this.globals = new float[var1.nodes()][];
      this.parent = new int[var1.nodes()];
      Arrays.fill(this.parent, -1);
   }

   static PosedMesh pose(GltfFile var0, Map<Integer, Float> var1) throws IOException {
      PosedMesh var2 = new PosedMesh(var0);

      for (int var3 = 0; var3 < var0.nodes(); var3++) {
         for (JsonElement var5 : GltfFile.array(var0.node(var3), "children")) {
            var2.parent[var5.getAsInt()] = var3;
         }
      }

      JsonArray var9 = GltfFile.array(var0.json, "scenes");
      JsonArray var10 = new JsonArray();
      if (var9.isEmpty()) {
         for (int var11 = 0; var11 < var0.nodes(); var11++) {
            if (var2.parent[var11] < 0) {
               var10.add(var11);
            }
         }
      } else {
         var10 = GltfFile.array(var9.get(GltfFile.integer(var0.json, "scene", 0)).getAsJsonObject(), "nodes");
      }

      for (JsonElement var6 : var10) {
         var2.walk(var6.getAsInt(), Mat4.identity(), var1);
      }

      for (JsonElement var15 : GltfFile.array(var0.json, "skins")) {
         for (JsonElement var8 : GltfFile.array(var15.getAsJsonObject(), "joints")) {
            var2.joints.add(var8.getAsInt());
         }
      }

      for (JsonElement var16 : var10) {
         var2.collect(var16.getAsInt());
      }

      if (var2.triangles == 0) {
         throw new IOException("it has no triangles");
      } else {
         return var2;
      }
   }

   private void walk(int var1, float[] var2, Map<Integer, Float> var3) {
      float[] var4 = Mat4.mul(var2, Mat4.local(this.file.node(var1)));
      Float var5 = (Float)var3.get(var1);
      if (var5 != null) {
         var4 = Mat4.mul(Mat4.rollAbout(var5, Mat4.point(var4, 0.0F, 0.0F, 0.0F)), var4);
      }

      this.globals[var1] = var4;

      for (JsonElement var7 : GltfFile.array(this.file.node(var1), "children")) {
         this.walk(var7.getAsInt(), var4, var3);
      }
   }

   private void collect(int var1) throws IOException {
      JsonObject var2 = this.file.node(var1);
      if (var2.has("mesh")) {
         JsonObject var3 = GltfFile.array(this.file.json, "meshes").get(var2.get("mesh").getAsInt()).getAsJsonObject();
         int var4 = GltfFile.integer(var2, "skin", -1);

         for (JsonElement var6 : GltfFile.array(var3, "primitives")) {
            this.primitive(var6.getAsJsonObject(), var1, var4);
         }
      }

      for (JsonElement var8 : GltfFile.array(var2, "children")) {
         this.collect(var8.getAsInt());
      }
   }

   private void primitive(JsonObject var1, int var2, int var3) throws IOException {
      if (GltfFile.integer(var1, "mode", 4) == 4) {
         JsonObject var5 = GltfFile.object(var1, "attributes");
         int var6 = GltfFile.integer(var1, "material", -1);
         float[] var7 = this.file.floats(var5.get("POSITION").getAsInt());
         int var8 = var7.length / 3;
         float[] var9 = var5.has("NORMAL") ? this.file.floats(var5.get("NORMAL").getAsInt()) : null;
         String var10 = "TEXCOORD_" + this.file.texCoord(var6);
         float[] var11 = var5.has(var10) ? this.file.floats(var5.get(var10).getAsInt()) : new float[var8 * 2];
         float[] var12 = var5.has("COLOR_0") ? this.file.floats(var5.get("COLOR_0").getAsInt()) : null;
         int var13 = var12 == null ? 0 : var12.length / var8;
         this.colored |= var12 != null;
         int[] var4 = var1.has("indices") ? this.file.ints(var1.get("indices").getAsInt()) : null;
         if (var4 == null) {
            var4 = new int[var8];
            int var15 = 0;

            while (var15 < var8) {
               var4[var15] = var15++;
            }
         }

         for (int var18 : var4) {
            if (var18 < 0 || var18 >= var8) {
               throw new IOException("a mesh has broken indices");
            }
         }

         if (this.triangles + var4.length / 3 > 4000000) {
            throw new IOException("it has more than four million triangles");
         } else {
            float[][] var40 = null;
            int[] var41 = null;
            int[] var42 = null;
            float[] var43 = null;
            if (var3 >= 0 && var5.has("JOINTS_0") && var5.has("WEIGHTS_0")) {
               JsonObject var19 = GltfFile.array(this.file.json, "skins").get(var3).getAsJsonObject();
               JsonArray var20 = GltfFile.array(var19, "joints");
               var42 = new int[var20.size()];
               float[] var21 = var19.has("inverseBindMatrices") ? this.file.floats(var19.get("inverseBindMatrices").getAsInt()) : null;
               var40 = new float[var20.size()][];

               for (int var22 = 0; var22 < var20.size(); var22++) {
                  var42[var22] = var20.get(var22).getAsInt();
                  float[] var23 = var21 == null ? Mat4.identity() : Arrays.copyOfRange(var21, var22 * 16, var22 * 16 + 16);
                  float[] var24 = this.globals[var42[var22]];
                  var40[var22] = Mat4.mul(var24 == null ? Mat4.identity() : var24, var23);
               }

               var41 = this.file.ints(var5.get("JOINTS_0").getAsInt());
               var43 = this.file.floats(var5.get("WEIGHTS_0").getAsInt());
            }

            float[] var44 = this.globals[var2];
            int var45 = this.vertices;
            this.grow(var8, var4.length / 3);

            for (int var46 = 0; var46 < var8; var46++) {
               float var52 = var7[var46 * 3];
               float var25 = var7[var46 * 3 + 1];
               float var26 = var7[var46 * 3 + 2];
               float var27 = var9 == null ? 0.0F : var9[var46 * 3];
               float var28 = var9 == null ? 1.0F : var9[var46 * 3 + 1];
               float var29 = var9 == null ? 0.0F : var9[var46 * 3 + 2];
               int var30 = var45 + var46;
               Arrays.fill(this.influence, var30 * 4, var30 * 4 + 4, -1);
               float[] var49;
               float[] var51;
               if (var40 != null) {
                  var51 = new float[3];
                  var49 = new float[3];

                  for (int var31 = 0; var31 < 4; var31++) {
                     float var32 = var43[var46 * 4 + var31];
                     int var33 = var41[var46 * 4 + var31];
                     if (!(var32 <= 0.0F) && var33 >= 0 && var33 < var40.length) {
                        float[] var34 = Mat4.point(var40[var33], var52, var25, var26);
                        float[] var35 = Mat4.dir(var40[var33], var27, var28, var29);

                        for (int var36 = 0; var36 < 3; var36++) {
                           var51[var36] += var34[var36] * var32;
                           var49[var36] += var35[var36] * var32;
                        }

                        this.influence[var30 * 4 + var31] = var42[var33];
                        this.weight[var30 * 4 + var31] = var32;
                     }
                  }
               } else {
                  var51 = Mat4.point(var44, var52, var25, var26);
                  var49 = Mat4.dir(var44, var27, var28, var29);
                  this.influence[var30 * 4] = var2;
                  this.weight[var30 * 4] = 1.0F;
               }

               System.arraycopy(var51, 0, this.pos, var30 * 3, 3);
               Mat4.normalise(var49);
               System.arraycopy(var49, 0, this.nrm, var30 * 3, 3);
               this.uv[var30 * 2] = var11[var46 * 2];
               this.uv[var30 * 2 + 1] = var11[var46 * 2 + 1];
               this.material[var30] = var6;
               this.color[var30] = var12 == null
                  ? -1
                  : 0xFF000000 | srgb(var12[var46 * var13]) << 16 | srgb(var12[var46 * var13 + 1]) << 8 | srgb(var12[var46 * var13 + 2]);
            }

            for (int var47 = 0; var47 < var4.length / 3; var47++) {
               for (int var50 = 0; var50 < 3; var50++) {
                  this.tris[(this.triangles + var47) * 3 + var50] = var45 + var4[var47 * 3 + var50];
               }
            }

            int var48 = this.triangles;
            this.vertices += var8;
            this.triangles += var4.length / 3;
            if (var9 == null) {
               this.smoothNormals(var45, var8, var48);
            }
         }
      }
   }

   private void smoothNormals(int var1, int var2, int var3) {
      Arrays.fill(this.nrm, var1 * 3, (var1 + var2) * 3, 0.0F);

      for (int var4 = var3; var4 < this.triangles; var4++) {
         float[] var5 = Mat4.faceNormal(this.pos, this.tris[var4 * 3], this.tris[var4 * 3 + 1], this.tris[var4 * 3 + 2]);

         for (int var6 = 0; var6 < 3; var6++) {
            int var7 = this.tris[var4 * 3 + var6];

            for (int var8 = 0; var8 < 3; var8++) {
               int var9 = var7 * 3 + var8;
               this.nrm[var9] = this.nrm[var9] + var5[var8];
            }
         }
      }

      for (int var10 = var1; var10 < var1 + var2; var10++) {
         float[] var11 = new float[]{this.nrm[var10 * 3], this.nrm[var10 * 3 + 1], this.nrm[var10 * 3 + 2]};
         Mat4.normalise(var11);
         System.arraycopy(var11, 0, this.nrm, var10 * 3, 3);
      }
   }

   private void grow(int var1, int var2) {
      int var3 = this.vertices + var1;
      int var4 = this.triangles + var2;
      this.pos = Arrays.copyOf(this.pos, var3 * 3);
      this.nrm = Arrays.copyOf(this.nrm, var3 * 3);
      this.uv = Arrays.copyOf(this.uv, var3 * 2);
      this.influence = Arrays.copyOf(this.influence, var3 * 4);
      this.weight = Arrays.copyOf(this.weight, var3 * 4);
      this.material = Arrays.copyOf(this.material, var3);
      this.color = Arrays.copyOf(this.color, var3);
      this.tris = Arrays.copyOf(this.tris, var4 * 3);
   }

   private static int srgb(float var0) {
      return Math.round((float)Math.pow(Math.max(0.0F, Math.min(1.0F, var0)), 0.45454545454545453) * 255.0F);
   }

   void cluster(int var1) {
      if (this.triangles > var1) {
         float[] var3 = this.bounds();
         float var4 = Math.max(var3[3] - var3[0], Math.max(var3[4] - var3[1], var3[5] - var3[2])) / 600.0F;
         int[] var5 = new int[this.vertices];

         while (this.weld(var4, var5) > var1) {
            var4 *= 1.25F;
         }

         int var6 = 0;

         for (int var10 : var5) {
            var6 = Math.max(var6, var10 + 1);
         }

         float[] var24 = new float[var6 * 3];
         float[] var25 = new float[var6 * 3];
         float[] var26 = new float[var6 * 2];
         int[] var27 = new int[var6];
         int[] var11 = new int[var6];
         int[] var12 = new int[var6 * 4];
         float[] var13 = new float[var6 * 4];
         long[] var14 = new long[var6 * 3];

         for (int var15 = 0; var15 < this.vertices; var15++) {
            int var2;
            int var16 = var2 = var5[var15];
            int var17 = var27[var16]++;
            if (var17 == 0) {
               var11[var2] = this.material[var15];
               System.arraycopy(this.influence, var15 * 4, var12, var2 * 4, 4);
               System.arraycopy(this.weight, var15 * 4, var13, var2 * 4, 4);
            }

            for (int var18 = 0; var18 < 3; var18++) {
               int var19 = var2 * 3 + var18;
               var24[var19] += this.pos[var15 * 3 + var18];
               int var20 = var2 * 3 + var18;
               var25[var20] += this.nrm[var15 * 3 + var18];
               int var21 = var2 * 3 + var18;
               var14[var21] += this.color[var15] >> 16 - var18 * 8 & 0xFF;
            }

            int var35 = var2 * 2;
            var26[var35] += this.uv[var15 * 2];
            int var40 = var2 * 2 + 1;
            var26[var40] += this.uv[var15 * 2 + 1];
         }

         int[] var28 = new int[var6];

         for (int var23 = 0; var23 < var6; var23++) {
            for (int var29 = 0; var29 < 3; var29++) {
               int var32 = var23 * 3 + var29;
               var24[var32] /= var27[var23];
            }

            int var30 = var23 * 2;
            var26[var30] /= var27[var23];
            int var33 = var23 * 2 + 1;
            var26[var33] /= var27[var23];
            float[] var36 = new float[]{var25[var23 * 3], var25[var23 * 3 + 1], var25[var23 * 3 + 2]};
            Mat4.normalise(var36);
            System.arraycopy(var36, 0, var25, var23 * 3, 3);
            var28[var23] = 0xFF000000
               | (int)(var14[var23 * 3] / var27[var23]) << 16
               | (int)(var14[var23 * 3 + 1] / var27[var23]) << 8
               | (int)(var14[var23 * 3 + 2] / var27[var23]);
         }

         HashSet var31 = new HashSet();
         int[] var34 = new int[this.triangles * 3];
         int var37 = 0;

         for (int var41 = 0; var41 < this.triangles; var41++) {
            int var42 = var5[this.tris[var41 * 3]];
            int var43 = var5[this.tris[var41 * 3 + 1]];
            int var22 = var5[this.tris[var41 * 3 + 2]];
            if (var42 != var43 && var43 != var22 && var42 != var22 && var31.add(key(var42, var43, var22))) {
               var34[var37++] = var42;
               var34[var37++] = var43;
               var34[var37++] = var22;
            }
         }

         this.pos = var24;
         this.nrm = var25;
         this.uv = var26;
         this.material = var11;
         this.influence = var12;
         this.weight = var13;
         this.color = var28;
         this.tris = Arrays.copyOf(var34, var37);
         this.vertices = var6;
         this.triangles = var37 / 3;
      }
   }

   private int weld(float var1, int[] var2) {
      HashMap var3 = new HashMap();

      for (int var4 = 0; var4 < this.vertices; var4++) {
         String var5 = (int)Math.floor(this.pos[var4 * 3] / var1)
            + ","
            + (int)Math.floor(this.pos[var4 * 3 + 1] / var1)
            + ","
            + (int)Math.floor(this.pos[var4 * 3 + 2] / var1)
            + ","
            + this.material[var4]
            + ","
            + Math.round(this.uv[var4 * 2] * 64.0F)
            + ","
            + Math.round(this.uv[var4 * 2 + 1] * 64.0F);
         Integer var6 = var3.putIfAbsent(var5, var3.size());
         var2[var4] = var6 == null ? var3.size() - 1 : var6;
      }

      HashSet var9 = new HashSet();

      for (int var10 = 0; var10 < this.triangles; var10++) {
         int var11 = var2[this.tris[var10 * 3]];
         int var7 = var2[this.tris[var10 * 3 + 1]];
         int var8 = var2[this.tris[var10 * 3 + 2]];
         if (var11 != var7 && var7 != var8 && var11 != var8) {
            var9.add(key(var11, var7, var8));
         }
      }

      return var9.size();
   }

   private static long key(int var0, int var1, int var2) {
      int var3 = Math.min(var0, Math.min(var1, var2));
      int var4 = Math.max(var0, Math.max(var1, var2));
      int var5 = var0 + var1 + var2 - var3 - var4;
      return (var3 * 2654435761L + var5) * 2246822519L + var4;
   }

   int[] separate(int[] var1) {
      HashMap var3 = new HashMap();
      ArrayList var4 = new ArrayList();
      ArrayList var5 = new ArrayList();

      for (int var6 = 0; var6 < this.triangles; var6++) {
         int var7 = CharacterBake.owner(var1, this.tris[var6 * 3], this.tris[var6 * 3 + 1], this.tris[var6 * 3 + 2]);

         for (int var8 = 0; var8 < 3; var8++) {
            int var2 = this.tris[var6 * 3 + var8];
            if (var1[var2] != var7) {
               Integer var9 = (Integer)var3.get(var2 * 8L + var7);
               if (var9 == null) {
                  var9 = this.vertices + var4.size();
                  var3.put(var2 * 8L + var7, var9);
                  var4.add(var2);
                  var5.add(var7);
               }

               this.tris[var6 * 3 + var8] = var9;
            }
         }
      }

      int var11 = this.vertices;
      this.grow(var4.size(), 0);
      int[] var12 = Arrays.copyOf(var1, var11 + var4.size());

      for (int var13 = 0; var13 < var4.size(); var13++) {
         int var10 = (Integer)var4.get(var13);
         int var14 = var11 + var13;
         System.arraycopy(this.pos, var10 * 3, this.pos, var14 * 3, 3);
         System.arraycopy(this.nrm, var10 * 3, this.nrm, var14 * 3, 3);
         System.arraycopy(this.uv, var10 * 2, this.uv, var14 * 2, 2);
         System.arraycopy(this.influence, var10 * 4, this.influence, var14 * 4, 4);
         System.arraycopy(this.weight, var10 * 4, this.weight, var14 * 4, 4);
         this.material[var14] = this.material[var10];
         this.color[var14] = this.color[var10];
         var12[var14] = (Integer)var5.get(var13);
      }

      this.vertices = this.vertices + var4.size();
      return var12;
   }

   void normalise(float var1, boolean var2) {
      this.flipped = var2;
      if (var2) {
         for (int var3 = 0; var3 < this.vertices; var3++) {
            this.pos[var3 * 3] = -this.pos[var3 * 3];
            this.pos[var3 * 3 + 2] = -this.pos[var3 * 3 + 2];
            this.nrm[var3 * 3] = -this.nrm[var3 * 3];
            this.nrm[var3 * 3 + 2] = -this.nrm[var3 * 3 + 2];
         }
      }

      float[] var5 = this.bounds();
      this.scale = var1 / Math.max(1.0E-6F, var5[4] - var5[1]);
      this.shiftX = -(var5[0] + var5[3]) * 0.5F;
      this.shiftY = -var5[1];
      this.shiftZ = -(var5[2] + var5[5]) * 0.5F;

      for (int var4 = 0; var4 < this.vertices; var4++) {
         this.pos[var4 * 3] = (this.pos[var4 * 3] + this.shiftX) * this.scale;
         this.pos[var4 * 3 + 1] = (this.pos[var4 * 3 + 1] + this.shiftY) * this.scale;
         this.pos[var4 * 3 + 2] = (this.pos[var4 * 3 + 2] + this.shiftZ) * this.scale;
      }
   }

   float[] bounds() {
      float[] var1 = new float[]{3.4028235E38F, 3.4028235E38F, 3.4028235E38F, -3.4028235E38F, -3.4028235E38F, -3.4028235E38F};

      for (int var2 = 0; var2 < this.vertices; var2++) {
         for (int var3 = 0; var3 < 3; var3++) {
            var1[var3] = Math.min(var1[var3], this.pos[var2 * 3 + var3]);
            var1[var3 + 3] = Math.max(var1[var3 + 3], this.pos[var2 * 3 + var3]);
         }
      }

      return var1;
   }

   float[] world(int var1) {
      float[] var2 = this.globals[var1];
      return var2 == null ? new float[3] : Mat4.point(var2, 0.0F, 0.0F, 0.0F);
   }

   float[] joint(int var1) {
      float[] var2 = this.world(var1);
      float var3 = this.flipped ? -var2[0] : var2[0];
      float var4 = this.flipped ? -var2[2] : var2[2];
      return new float[]{(var3 + this.shiftX) * this.scale, (var2[1] + this.shiftY) * this.scale, (var4 + this.shiftZ) * this.scale};
   }

   boolean under(int var1, int var2) {
      int var3 = var1;

      for (int var4 = 0; var3 >= 0 && var4 <= this.parent.length; var4++) {
         if (var3 == var2) {
            return true;
         }

         var3 = this.parent[var3];
      }

      return false;
   }

   float[] below(int var1) {
      float[] var2 = new float[this.vertices];

      for (int var3 = 0; var3 < this.vertices; var3++) {
         for (int var4 = 0; var4 < 4; var4++) {
            int var5 = this.influence[var3 * 4 + var4];
            if (var5 >= 0 && this.under(var5, var1)) {
               var2[var3] += this.weight[var3 * 4 + var4];
            }
         }
      }

      return var2;
   }
}
