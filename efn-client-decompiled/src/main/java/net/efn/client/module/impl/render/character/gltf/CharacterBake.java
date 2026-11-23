package net.efn.client.module.impl.render.character.gltf;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CharacterBake {
   static final int HEAD = 0;
   static final int BODY = 1;
   static final int RIGHT_ARM = 2;
   static final int LEFT_ARM = 3;
   static final int RIGHT_LEG = 4;
   static final int LEFT_LEG = 5;
   private static final int[] BUDGETS = new int[]{7500, 15000, 30000};
   private static final int DENSE = 150000;
   private static final float[] STICK = new float[]{0.7F, 0.0F, 0.6F, 0.6F, 0.3F, 0.3F};

   private CharacterBake() {
   }

   public static CharacterBake.Result bake(byte[] var0, CharacterBake.Options var1, ImageDecoder var2) throws IOException {
      GltfFile var4 = GltfFile.read(var0);
      PosedMesh var5 = PosedMesh.pose(var4, Map.of());
      Skeleton var6 = Skeleton.find(var5, var1.bones);
      Map var7 = var6.armTurns();
      if (!var7.isEmpty()) {
         var5 = PosedMesh.pose(var4, var7);
         var6 = Skeleton.find(var5, var1.bones);
      }

      var5.normalise(var1.height, var6.facesBack);
      var5.cluster(150000);
      CharacterBake.Cut var3 = var6.found() ? skeletonCut(var5, var6, var1) : ShapeCut.cut(var5);
      if (var3 == null) {
         var3 = still(var5);
      }

      List var9 = split(var5, var3.part());
      reduce(var5, var9);
      TextureAtlas var10 = TextureAtlas.pack(var4, uvBounds(var5, var9), var2);
      ByteArrayOutputStream var11 = new ByteArrayOutputStream();
      write(new DataOutputStream(var11), var5, var3, var9, var10, var1);
      ArrayList var12 = new ArrayList();
      if (has(var9, 0)) {
         var12.add("head");
      }

      if (has(var9, 2) || has(var9, 3)) {
         var12.add("arms");
      }

      if (has(var9, 4) || has(var9, 5)) {
         var12.add("legs");
      }

      return new CharacterBake.Result(var11.toByteArray(), var12);
   }

   public static String describe(byte[] var0, CharacterBake.Options var1) throws IOException {
      GltfFile var2 = GltfFile.read(var0);
      PosedMesh var3 = PosedMesh.pose(var2, Map.of());
      Skeleton var4 = Skeleton.find(var3, var1.bones);
      StringBuilder var5 = new StringBuilder(
         String.format(Locale.ROOT, "%d vertices, %d triangles, %d bones%n", var3.vertices, var3.triangles, var3.joints.size())
      );
      var5.append(var4.found() ? var4.describe() : "no usable skeleton, limbs are cut by shape\n");
      HashSet var6 = new HashSet();

      for (int var7 = 0; var7 < var3.vertices; var7++) {
         var6.add(var3.material[var7]);
      }

      for (int var8 : var6) {
         var5.append("material ").append(var2.materialName(var8)).append('\n');
      }

      return var5.toString();
   }

   private static CharacterBake.Cut still(PosedMesh var0) {
      float[][] var1 = new float[6][];
      var1[1] = new float[]{0.0F, 24.0F, 0.0F};
      return new CharacterBake.Cut(new int[var0.vertices], var1, new float[6][], new float[6][], new float[6][]);
   }

   private static CharacterBake.Cut skeletonCut(PosedMesh var0, Skeleton var1, CharacterBake.Options var2) {
      int[] var3 = var1.groups();
      boolean var4 = var1.thigh[0] >= 0 && var1.thigh[1] >= 0;
      float var5 = var2.floor;
      if (var5 < 0.0F && var1.shin[0] >= 0 && var1.shin[1] >= 0) {
         var5 = Math.min(var0.joint(var1.shin[0])[1], var0.joint(var1.shin[1])[1]);
      }

      boolean var6 = !var2.headMaterials.isEmpty() && var1.head >= 0;
      int[] var7 = new int[var0.vertices];
      float[] var8 = new float[6];

      for (int var9 = 0; var9 < var0.vertices; var9++) {
         float var11 = var0.pos[var9 * 3];
         float var12 = var0.pos[var9 * 3 + 1];
         if (var6 && var2.headMaterials.contains(var0.file.materialName(var0.material[var9]))) {
            var7[var9] = var12 < var2.neck ? 1 : 0;
         } else {
            Arrays.fill(var8, 0.0F);

            for (int var13 = 0; var13 < 4; var13++) {
               int var14 = var0.influence[var9 * 4 + var13];
               if (var14 >= 0) {
                  int var15 = var3[var14];
                  var8[var15] += var0.weight[var9 * 4 + var13];
               }
            }

            int var23 = 1;

            for (int var25 = 0; var25 < 6; var25++) {
               if (var25 != 1 && var8[var25] >= STICK[var25] && var8[var25] > var8[var23] && (var25 != 0 || !var6)) {
                  var23 = var25;
               }
            }

            boolean var10 = var23 == 1 || var23 == 4 || var23 == 5;
            if (var4 && var10 && var2.hip > 0.0F) {
               var23 = var12 >= var2.hip ? 1 : (var11 < 0.0F ? 4 : 5);
            } else if (var4 && var23 == 1 && var12 < var5) {
               var23 = var11 < 0.0F ? 4 : 5;
            }

            var7[var9] = var23;
         }
      }

      float[][] var19 = new float[6][];
      float[][] var20 = new float[6][];
      float[][] var21 = new float[6][];
      float[][] var22 = new float[6][];
      var19[1] = new float[]{0.0F, 24.0F, 0.0F};
      if (var1.head >= 0) {
         var19[0] = var0.joint(var1.head);
      }

      for (int var24 = 0; var24 < 2; var24++) {
         int var27 = var24 == 0 ? 2 : 3;
         int var26 = var24 == 0 ? 4 : 5;
         if (var1.upperArm[var24] >= 0) {
            var19[var27] = var0.joint(var1.upperArm[var24]);
            int var17 = var1.hand[var24] >= 0 ? var1.hand[var24] : var1.forearm[var24];
            float[] var18 = var20[var27] = var17 >= 0 ? var0.joint(var17) : var19[var27];
            if (var1.forearm[var24] >= 0) {
               var21[var27] = var0.joint(var1.forearm[var24]);
               var22[var27] = bend(var0, var1.forearm[var24], var1.upperArm[var24]);
            }
         }

         if (var1.thigh[var24] >= 0) {
            var19[var26] = var0.joint(var1.thigh[var24]);
            if (var1.shin[var24] >= 0) {
               var21[var26] = var0.joint(var1.shin[var24]);
               var22[var26] = bend(var0, var1.shin[var24], var1.thigh[var24]);
            }
         }
      }

      return new CharacterBake.Cut(var7, var19, var20, var21, var22);
   }

   static int owner(int[] var0, int var1, int var2, int var3) {
      return var0[var2] == var0[var3] ? var0[var2] : var0[var1];
   }

   private static List<CharacterBake.Piece> split(PosedMesh var0, int[] var1) {
      CharacterBake.Piece[][] var2 = new CharacterBake.Piece[6][2];

      for (int var3 = 0; var3 < var0.triangles; var3++) {
         int var5 = var0.tris[var3 * 3];
         int var6 = var0.tris[var3 * 3 + 1];
         int var7 = var0.tris[var3 * 3 + 2];
         if (!(Mat4.area(var0.pos, var5, var6, var7) < 1.0E-9F)) {
            int var8 = owner(var1, var5, var6, var7);
            int var4;
            CharacterBake.Piece var9 = var2[var8][var4 = var0.file.translucent(var0.material[var5]) ? 1 : 0];
            if (var9 == null) {
               CharacterBake.Piece var10 = new CharacterBake.Piece(var8, var4);
               var2[var8][var4] = var10;
               var9 = var10;
            }

            var9.source.add(var3);
            if (var4 == 1 && var0.file.doubleSided(var0.material[var5])) {
               var9.source.add(~var3);
            }
         }
      }

      ArrayList var12 = new ArrayList();
      CharacterBake.Piece[][] var13 = var2;
      int var14 = var2.length;

      for (int var15 = 0; var15 < var14; var15++) {
         CharacterBake.Piece[] var16;
         for (CharacterBake.Piece var11 : var16 = var13[var15]) {
            if (var11 != null) {
               var12.add(var11);
            }
         }
      }

      return var12;
   }

   private static void reduce(PosedMesh var0, List<CharacterBake.Piece> var1) {
      double var2 = 0.0;

      for (CharacterBake.Piece var5 : var1) {
         var2 += var5.source.size() * (var5.bone == 0 ? 2.0 : 1.0);
      }

      for (CharacterBake.Piece var11 : var1) {
         int[] var6 = new int[BUDGETS.length];

         for (int var7 = 0; var7 < BUDGETS.length; var7++) {
            double var8 = var11.source.size() * (var11.bone == 0 ? 2.0 : 1.0) / var2;
            var6[var7] = (int)Math.min((long)var11.source.size(), Math.max(16L, Math.round(BUDGETS[var7] * var8)));
         }

         boolean var12 = var6[BUDGETS.length - 1] < var11.source.size();
         var11.levels = new MeshReducer(var0, var11.source, var12).reduce(var6);
      }
   }

   private static Map<Integer, float[]> uvBounds(PosedMesh var0, List<CharacterBake.Piece> var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (CharacterBake.Piece var4 : var1) {
         for (int var6 : var4.source) {
            int var7 = var6 < 0 ? ~var6 : var6;

            for (int var8 = 0; var8 < 3; var8++) {
               int var9 = var0.tris[var7 * 3 + var8];
               float[] var10 = var2.computeIfAbsent(var0.material[var9], var0x -> new float[]{1.0E9F, 1.0E9F, -1.0E9F, -1.0E9F});
               var10[0] = Math.min(var10[0], var0.uv[var9 * 2]);
               var10[1] = Math.min(var10[1], var0.uv[var9 * 2 + 1]);
               var10[2] = Math.max(var10[2], var0.uv[var9 * 2]);
               var10[3] = Math.max(var10[3], var0.uv[var9 * 2 + 1]);
            }
         }
      }

      return var2;
   }

   private static boolean has(List<CharacterBake.Piece> var0, int var1) {
      for (CharacterBake.Piece var3 : var0) {
         if (var3.bone == var1) {
            return true;
         }
      }

      return false;
   }

   private static CharacterBake.Piece find(List<CharacterBake.Piece> var0, int var1, int var2) {
      for (CharacterBake.Piece var4 : var0) {
         if (var4.bone == var1 && var4.layer == var2) {
            return var4;
         }
      }

      return null;
   }

   private static void write(
      DataOutputStream var0, PosedMesh var1, CharacterBake.Cut var2, List<CharacterBake.Piece> var3, TextureAtlas var4, CharacterBake.Options var5
   ) throws IOException {
      float[][] var7 = var2.pivot();
      float[][] var8 = var2.grip();
      float[][] var9 = var2.bendAt();
      float[][] var10 = var2.bendWeights();
      float[] var11 = var1.bounds();
      float var12 = var7[4] != null && var7[5] != null ? (var7[4][1] + var7[5][1]) * 0.5F : 12.0F;
      var0.writeInt(1095648072);
      var0.writeInt(3);
      var0.writeFloat(var11[1]);
      var0.writeFloat(var11[4]);
      var0.writeFloat(var12);
      var0.writeFloat(var5.flat);
      var0.writeBoolean(true);
      var0.writeBoolean(var1.colored);
      var0.writeInt(BUDGETS.length);
      int var13 = 0;

      for (int var6 = 0; var6 < 6; var6++) {
         if (has(var3, var6)) {
            var13++;
         }
      }

      var0.writeInt(var13);

      for (int var22 = 0; var22 < 6; var22++) {
         if (has(var3, var22)) {
            var0.writeByte(var22);

            for (float var19 : var7[var22]) {
               var0.writeFloat(var19);
            }

            for (float var33 : var8[var22] != null ? var8[var22] : var7[var22]) {
               var0.writeFloat(var33);
            }

            float[] var15;
            for (float var34 : var15 = bounds(var1, var3, var22)) {
               var0.writeFloat(var34);
            }

            boolean var26 = var22 == 2 || var22 == 3;
            boolean var14 = var22 == 4 || var22 == 5;
            var0.writeFloat(var26 ? 0.6F : (var14 ? 0.55F : 1.0F));
            var0.writeFloat(var26 ? 40.0F : (var14 ? 30.0F : 0.0F));
            var0.writeBoolean(var9[var22] != null);
            if (var9[var22] != null) {
               for (float var21 : var9[var22]) {
                  var0.writeFloat(var21);
               }
            }

            for (int var32 = 0; var32 < BUDGETS.length; var32++) {
               for (int var36 = 0; var36 < 2; var36++) {
                  writeLevel(var0, var1, var4, find(var3, var22, var36), var32, var15, var10[var22], var9[var22] != null);
               }
            }
         }
      }

      byte[] var23 = var4.packed();
      var0.writeInt(var4.width);
      var0.writeInt(var4.height);
      var0.writeInt(var23.length);
      var0.write(var23);
      var0.flush();
   }

   private static float[] bend(PosedMesh var0, int var1, int var2) {
      float[] var3 = var0.joint(var1);
      float[] var4 = var0.below(var1);
      float[] var5 = var0.below(var2);

      for (int var6 = 0; var6 < var0.vertices; var6++) {
         if (!(var5[var6] >= 0.5F)) {
            float var7 = Math.max(0.0F, Math.min(1.0F, var3[1] - var0.pos[var6 * 3 + 1] + 0.5F));
            var4[var6] = var7 * var7 * (3.0F - 2.0F * var7);
         }
      }

      return var4;
   }

   private static float[] bounds(PosedMesh var0, List<CharacterBake.Piece> var1, int var2) {
      float[] var3 = new float[]{1.0E9F, 1.0E9F, 1.0E9F, -1.0E9F, -1.0E9F, -1.0E9F};

      for (CharacterBake.Piece var5 : var1) {
         if (var5.bone == var2) {
            for (int var7 : var5.source) {
               int var8 = var7 < 0 ? ~var7 : var7;

               for (int var9 = 0; var9 < 3; var9++) {
                  int var10 = var0.tris[var8 * 3 + var9];

                  for (int var11 = 0; var11 < 3; var11++) {
                     var3[var11] = Math.min(var3[var11], var0.pos[var10 * 3 + var11]);
                     var3[var11 + 3] = Math.max(var3[var11 + 3], var0.pos[var10 * 3 + var11]);
                  }
               }
            }
         }
      }

      for (int var12 = 0; var12 < 3; var12++) {
         if (var3[var12 + 3] - var3[var12] < 0.001F) {
            var3[var12 + 3] = var3[var12] + 0.001F;
         }
      }

      return var3;
   }

   private static void writeLevel(
      DataOutputStream var0, PosedMesh var1, TextureAtlas var2, CharacterBake.Piece var3, int var4, float[] var5, float[] var6, boolean var7
   ) throws IOException {
      if (var3 == null) {
         var0.writeInt(0);
         var0.writeInt(0);
      } else {
         int[] var9 = var3.levels[var4];
         HashMap var10 = new HashMap();
         ArrayList var11 = new ArrayList();
         int[] var12 = new int[var9.length];

         for (int var13 = 0; var13 < var9.length; var13++) {
            Integer var14 = (Integer)var10.get(var9[var13]);
            if (var14 == null) {
               var14 = var11.size();
               var10.put(var9[var13], var14);
               var11.add(var9[var13]);
            }

            var12[var13] = var14;
         }

         var0.writeInt(var11.size());

         for (int var26 : var11) {
            int var8 = var26 < 0 ? ~var26 : var26;

            for (int var15 = 0; var15 < 3; var15++) {
               float var16 = (var1.pos[var8 * 3 + var15] - var5[var15]) / (var5[var15 + 3] - var5[var15]);
               var0.writeChar(Math.round(Math.max(0.0F, Math.min(1.0F, var16)) * 65535.0F));
            }
         }

         for (int var27 : var11) {
            int var19 = var27 < 0 ? ~var27 : var27;
            float var32 = var27 < 0 ? -1.0F : 1.0F;

            for (int var36 = 0; var36 < 3; var36++) {
               var0.writeByte(Math.round(var1.nrm[var19 * 3 + var36] * var32 * 127.0F));
            }
         }

         for (int var28 : var11) {
            int var20 = var28 < 0 ? ~var28 : var28;
            float[] var33 = var2.map(var1.material[var20], var1.uv[var20 * 2], var1.uv[var20 * 2 + 1]);
            var0.writeChar(Math.round(Math.max(0.0F, Math.min(1.0F, var33[0])) * 65535.0F));
            var0.writeChar(Math.round(Math.max(0.0F, Math.min(1.0F, var33[1])) * 65535.0F));
         }

         if (var1.colored) {
            for (int var29 : var11) {
               int var34 = var1.color[var29 < 0 ? ~var29 : var29];
               var0.writeByte(var34 >> 16);
               var0.writeByte(var34 >> 8);
               var0.writeByte(var34);
            }
         }

         if (var7) {
            for (int var30 : var11) {
               var0.writeByte(Math.round(Math.max(0.0F, Math.min(1.0F, var6[var30 < 0 ? ~var30 : var30])) * 255.0F));
            }
         }

         var0.writeInt(var12.length / 3);
         boolean var31 = var11.size() > 65535;

         for (int var18 : var12) {
            if (var31) {
               var0.writeInt(var18);
            } else {
               var0.writeChar(var18);
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   record Cut(int[] part, float[][] pivot, float[][] grip, float[][] bendAt, float[][] bendWeights) {
   }

   @Environment(EnvType.CLIENT)
   public static final class Options {
      public float height = 30.0F;
      public float flat;
      public final Map<String, String> bones = new HashMap<>();
      public final Set<String> headMaterials = new HashSet<>();
      public float neck = -1.0F;
      public float hip = -1.0F;
      public float floor = -1.0F;

      public Options() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Piece {
      final int bone;
      final int layer;
      final List<Integer> source = new ArrayList<>();
      int[][] levels;

      Piece(int var1, int var2) {
         this.bone = var1;
         this.layer = var2;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Result(byte[] data, List<String> limbs) {
   }
}
