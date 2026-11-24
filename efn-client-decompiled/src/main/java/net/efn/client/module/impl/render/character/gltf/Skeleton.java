package net.efn.client.module.impl.render.character.gltf;

import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class Skeleton {
   static final int RIGHT = 0;
   static final int LEFT = 1;
   private static final Set<String> NOISE = Set.of(
      "j", "bip", "bip01", "def", "deform", "mch", "org", "jnt", "jt", "bone", "bn", "b", "c", "x", "stretch", "valve", "biped", "mixamorig"
   );
   private static final Set<String> LEFT_WORDS = Set.of("left", "l", "lf", "lft");
   private static final Set<String> RIGHT_WORDS = Set.of("right", "r", "rt", "rgt");
   private static final List<String> UPPER_ARM = List.of("upperarm", "uparm", "armupper");
   private static final List<String> FOREARM = List.of("forearm", "lowerarm", "elbow", "armlower", "loarm");
   private static final List<String> HAND = List.of("hand", "wrist");
   private static final List<String> THIGH = List.of("thigh", "upleg", "upperleg", "legupper");
   private static final List<String> SHIN = List.of("calf", "shin", "knee", "lowerleg", "leglower", "loleg");
   int head = -1;
   final int[] upperArm = new int[]{-1, -1};
   final int[] forearm = new int[]{-1, -1};
   final int[] hand = new int[]{-1, -1};
   final int[] thigh = new int[]{-1, -1};
   final int[] shin = new int[]{-1, -1};
   boolean facesBack;
   private final PosedMesh mesh;
   private final String[] core;
   private final int[] named;

   private Skeleton(PosedMesh posedMesh) {
      this.mesh = posedMesh;
      int var2 = posedMesh.file.nodes();
      this.core = new String[var2];
      this.named = new int[var2];

      for (int var3 = 0; var3 < var2; var3++) {
         List var4 = words(posedMesh.file.nodeName(var3));
         this.named[var3] = -1;
         StringBuilder var5 = new StringBuilder();

         for (String var7 : var4) {
            if (LEFT_WORDS.contains(var7)) {
               this.named[var3] = 1;
            } else if (RIGHT_WORDS.contains(var7)) {
               this.named[var3] = 0;
            } else if (!NOISE.contains(var7)) {
               var5.append(var7);
            }
         }

         this.core[var3] = var5.toString().replaceAll("\\d+$", "");
      }
   }

   static Skeleton find(PosedMesh posedMesh, Map<String, String> map) throws IOException {
      Skeleton var2 = new Skeleton(posedMesh);
      if (map.isEmpty()) {
         var2.facesBack = var2.facingBack();
         var2.head = var2.shallowest(i -> var2.core[i].startsWith("head"));

         for (int var12 = 0; var12 < 2; var12++) {
            int var6 = var12;
            int var5;
            int var7 = var5 = var2.shallowest(j -> var2.side(j) == var6 && var2.is(j, FOREARM));
            int var8 = var2.shallowest(
               k -> var2.side(k) == var6 && (var2.is(k, UPPER_ARM) || var2.core[k].equals("arm")) && (var5 < 0 || posedMesh.under(var5, k) && k != var5)
            );
            if (var8 < 0 && var7 >= 0) {
               var8 = posedMesh.parent[var7];
            }

            if (var7 < 0 && var8 >= 0) {
               var7 = var2.mainChild(var8);
            }

            var2.upperArm[var6] = var8;
            var2.forearm[var6] = var7;
            if (var7 >= 0) {
               int var10 = var2.shallowest(j -> j != var7 && posedMesh.under(j, var7) && var2.is(j, HAND));
               var2.hand[var6] = var10 >= 0 ? var10 : var2.mainChild(var7);
            }

            int var13;
            int var9 = var13 = var2.shallowest(j -> var2.side(j) == var6 && var2.is(j, SHIN));
            int var14 = var2.shallowest(
               k -> var2.side(k) == var6 && (var2.is(k, THIGH) || var2.core[k].equals("leg")) && (var13 < 0 || posedMesh.under(var13, k) && k != var13)
            );
            if (var9 < 0 && var14 >= 0 && (var9 = var2.shallowest(j -> j != var14 && posedMesh.under(j, var14) && var2.core[j].equals("leg"))) < 0) {
               var9 = var2.mainChild(var14);
            }

            if (var14 < 0 && var9 >= 0) {
               var14 = posedMesh.parent[var9];
            }

            var2.thigh[var6] = var14;
            var2.shin[var6] = var9;
         }

         return var2;
      } else {
         var2.head = var2.named(map, "head");

         for (int var3 = 0; var3 < 2; var3++) {
            String var4 = var3 == 0 ? "right " : "left ";
            var2.upperArm[var3] = var2.named(map, var4 + "arm");
            var2.forearm[var3] = var2.named(map, var4 + "elbow");
            var2.hand[var3] = var2.named(map, var4 + "hand");
            var2.thigh[var3] = var2.named(map, var4 + "leg");
            var2.shin[var3] = var2.named(map, var4 + "knee");
         }

         var2.facesBack = var2.upperArm[1] >= 0 && var2.upperArm[0] >= 0 && posedMesh.world(var2.upperArm[1])[0] < posedMesh.world(var2.upperArm[0])[0];
         return var2;
      }
   }

   private boolean is(int i, List<String> list) {
      for (String var4 : list) {
         if (this.core[i].contains(var4)) {
            return true;
         }
      }

      return false;
   }

   private int named(Map<String, String> map, String string) throws IOException {
      String var3 = (String)map.get(string);
      if (var3 == null) {
         return -1;
      } else {
         for (int var4 = 0; var4 < this.core.length; var4++) {
            if (this.mesh.file.nodeName(var4).equalsIgnoreCase(var3)) {
               return var4;
            }
         }

         String var6 = var3.toLowerCase(Locale.ROOT);

         for (int var5 = 0; var5 < this.core.length; var5++) {
            if (this.mesh.file.nodeName(var5).toLowerCase(Locale.ROOT).startsWith(var6)) {
               return var5;
            }
         }

         throw new IOException("no bone named " + var3);
      }
   }

   private static List<String> words(String string) {
      String var1 = string.replaceAll("_\\d+$", "")
         .replaceAll("([a-z])([A-Z])", "$1 $2")
         .toLowerCase(Locale.ROOT)
         .replace("\u624b\u9996", " wrist ")
         .replace("\u8db3\u9996", " ankle ")
         .replace("\u5de6", " left ")
         .replace("\u53f3", " right ")
         .replace("\u3072\u3058", " elbow ")
         .replace("\u3072\u3056", " knee ")
         .replace("\u8155", " arm ")
         .replace("\u8db3", " leg ")
         .replace("\u982d", " head ")
         .replace("\u9996", " neck ");
      ArrayList var2 = new ArrayList();

      for (String var6 : var1.split("[^a-z0-9]+")) {
         if (!var6.isEmpty()) {
            var2.add(var6);
         }
      }

      return var2;
   }

   private boolean facingBack() {
      int var1 = this.shallowest(i -> this.named[i] == 1 && this.is(i, HAND));
      int var2 = this.shallowest(i -> this.named[i] == 0 && this.is(i, HAND));
      if (var1 < 0 || var2 < 0) {
         var1 = this.shallowest(i -> this.named[i] == 1 && this.is(i, SHIN));
         var2 = this.shallowest(i -> this.named[i] == 0 && this.is(i, SHIN));
      }

      if (var1 >= 0 && var2 >= 0) {
         return this.mesh.world(var1)[0] < this.mesh.world(var2)[0];
      } else {
         float[] var3 = this.mesh.bounds();
         float var4 = var3[4] - var3[1];
         float var5 = 0.0F;
         float var6 = 0.0F;
         int var7 = 0;
         int var8 = 0;

         for (int var9 = 0; var9 < this.mesh.vertices; var9++) {
            float var10 = this.mesh.pos[var9 * 3 + 1] - var3[1];
            float var11 = this.mesh.pos[var9 * 3 + 2];
            if (var10 < var4 * 0.04F) {
               var5 += var11;
               var7++;
            } else if (var10 > var4 * 0.1F && var10 < var4 * 0.2F) {
               var6 += var11;
               var8++;
            }
         }

         return var7 > 0 && var8 > 0 && var5 / var7 < var6 / var8 - var4 * 0.01F;
      }
   }

   private int side(int i) {
      if (this.named[i] >= 0) {
         return this.named[i];
      } else {
         return this.mesh.world(i)[0] < 0.0F != this.facesBack ? 0 : 1;
      }
   }

   private int shallowest(IntPredicate intPredicate) {
      int var2 = -1;
      int var3 = 2147483647;

      for (int var5 : this.mesh.joints) {
         if (intPredicate.test(var5)) {
            int var7 = this.mesh.parent[var5];

            int var6;
            for (var6 = 0; var7 >= 0 && var6 <= this.core.length; var6++) {
               var7 = this.mesh.parent[var7];
            }

            if (var6 < var3 || var6 == var3 && var5 < var2) {
               var2 = var5;
               var3 = var6;
            }
         }
      }

      return var2;
   }

   private int mainChild(int i) {
      int var2 = -1;
      int var3 = -1;

      for (JsonElement var5 : GltfFile.array(this.mesh.file.node(i), "children")) {
         int var6 = var5.getAsInt();
         int var7 = 0;

         for (int var8 = 0; var8 < this.core.length; var8++) {
            if (this.mesh.under(var8, var6)) {
               var7++;
            }
         }

         if (var7 > var3) {
            var2 = var6;
            var3 = var7;
         }
      }

      return var2;
   }

   Map<Integer, Float> armTurns() {
      HashMap var1 = new HashMap();

      for (int var2 = 0; var2 < 2; var2++) {
         if (this.upperArm[var2] >= 0 && this.forearm[var2] >= 0) {
            float[] var3 = this.mesh.world(this.upperArm[var2]);
            float[] var4 = this.mesh.world(this.forearm[var2]);
            float var5 = var4[0] - var3[0];
            float var6 = var4[1] - var3[1];
            float var7 = (float)Math.toDegrees(Math.atan2(Math.abs(var5), -var6));
            if (var7 > 40.0F) {
               var1.put(this.upperArm[var2], (var5 < 0.0F ? 1.0F : -1.0F) * (var7 - 10.0F));
            }
         }
      }

      return var1;
   }

   int[] groups() {
      int[] var1 = new int[this.core.length];

      for (int var2 = 0; var2 < var1.length; var2++) {
         var1[var2] = 1;
         if (this.head >= 0 && this.mesh.under(var2, this.head)) {
            var1[var2] = 0;
         } else if (this.upperArm[0] >= 0 && this.mesh.under(var2, this.upperArm[0])) {
            var1[var2] = 2;
         } else if (this.upperArm[1] >= 0 && this.mesh.under(var2, this.upperArm[1])) {
            var1[var2] = 3;
         } else if (this.thigh[0] >= 0 && this.mesh.under(var2, this.thigh[0])) {
            var1[var2] = 4;
         } else if (this.thigh[1] >= 0 && this.mesh.under(var2, this.thigh[1])) {
            var1[var2] = 5;
         }
      }

      return var1;
   }

   boolean found() {
      return this.head >= 0 || this.upperArm[0] >= 0 || this.upperArm[1] >= 0 || this.thigh[0] >= 0 || this.thigh[1] >= 0;
   }

   String describe() {
      StringBuilder var1 = new StringBuilder();
      var1.append("facing ").append(this.facesBack ? "-z (turned around)" : "+z").append('\n');
      this.line(var1, "head", this.head);

      for (int var2 = 0; var2 < 2; var2++) {
         String var3 = var2 == 0 ? "right " : "left ";
         this.line(var1, var3 + "arm", this.upperArm[var2]);
         this.line(var1, var3 + "elbow", this.forearm[var2]);
         this.line(var1, var3 + "hand", this.hand[var2]);
         this.line(var1, var3 + "leg", this.thigh[var2]);
         this.line(var1, var3 + "knee", this.shin[var2]);
      }

      return var1.toString();
   }

   private void line(StringBuilder stringBuilder, String string, int i) {
      stringBuilder.append(String.format(Locale.ROOT, "%-11s %s%n", string, i < 0 ? "-" : this.mesh.file.nodeName(i)));
   }
}
