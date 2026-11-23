package net.efn.client.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class BlockClusterGeometry {
   private static final int[][] AXES = new int[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

   private BlockClusterGeometry() {
   }

   public static BlockClusterGeometry.Mesh build(Collection<BlockPos> collection, BlockClusterGeometry.Membership membership) {
      ArrayList var2 = new ArrayList(collection.size() * 4);
      ArrayList var3 = new ArrayList(collection.size() * 3);

      for (BlockPos var5 : collection) {
         int[] var6 = new int[]{var5.getX(), var5.getY(), var5.getZ()};
         addFaces(var6, membership, var3);
         addEdges(var6, membership, var2);
      }

      return new BlockClusterGeometry.Mesh(mergeCollinear(var2), var3);
   }

   private static List<BlockClusterGeometry.Segment> mergeCollinear(List<int[]> list) {
      HashMap var1 = new HashMap();

      for (int[] var3 : list) {
         int var4 = var3[0];
         int var5 = (var4 + 1) % 3;
         int var6 = (var4 + 2) % 3;
         var1.computeIfAbsent(List.of(var4, var3[1 + var5], var3[1 + var6]), listx -> new ArrayList<>()).add(var3[1 + var4]);
      }

      ArrayList var14 = new ArrayList(list.size());

      for (Entry var16 : var1.entrySet()) {
         int var17 = (Integer)((List)var16.getKey()).get(0);
         int var18 = (var17 + 1) % 3;
         int var7 = (var17 + 2) % 3;
         List var8 = (List)var16.getValue();
         var8.sort(null);
         int var9 = (Integer)var8.get(0);
         int var10 = var9 + 1;

         for (int var11 = 1; var11 <= var8.size(); var11++) {
            if (var11 < var8.size() && (Integer)var8.get(var11) == var10) {
               var10++;
            } else {
               int[] var12 = new int[3];
               int[] var13 = new int[3];
               var12[var18] = var13[var18] = (Integer)((List)var16.getKey()).get(1);
               var12[var7] = var13[var7] = (Integer)((List)var16.getKey()).get(2);
               var12[var17] = var9;
               var13[var17] = var10;
               var14.add(new BlockClusterGeometry.Segment(vec(var12), vec(var13)));
               if (var11 < var8.size()) {
                  var9 = (Integer)var8.get(var11);
                  var10 = var9 + 1;
               }
            }
         }
      }

      return var14;
   }

   public static List<List<BlockPos>> connectedComponents(Collection<BlockPos> collection) {
      HashSet var1 = new HashSet(collection);
      ArrayList var2 = new ArrayList();
      ArrayDeque var3 = new ArrayDeque();

      while (!var1.isEmpty()) {
         BlockPos var4 = (BlockPos)var1.iterator().next();
         var1.remove(var4);
         ArrayList var5 = new ArrayList();
         var3.add(var4);

         while (!var3.isEmpty()) {
            BlockPos var6 = (BlockPos)var3.poll();
            var5.add(var6);

            for (int[] var10 : AXES) {
               for (byte var11 = -1; var11 <= 1; var11 += 2) {
                  BlockPos var12 = var6.add(var10[0] * var11, var10[1] * var11, var10[2] * var11);
                  if (var1.remove(var12)) {
                     var3.add(var12);
                  }
               }
            }
         }

         var2.add(var5);
      }

      return var2;
   }

   private static void addFaces(int[] is, BlockClusterGeometry.Membership membership, List<BlockClusterGeometry.Face> list) {
      for (int var3 = 0; var3 < 3; var3++) {
         int var4 = (var3 + 1) % 3;
         int var5 = (var3 + 2) % 3;

         for (byte var6 = -1; var6 <= 1; var6 += 2) {
            if (!membership.contains(is[0] + AXES[var3][0] * var6, is[1] + AXES[var3][1] * var6, is[2] + AXES[var3][2] * var6)) {
               int[] var7 = (int[])is.clone();
               if (var6 > 0) {
                  var7[var3]++;
               }

               list.add(
                  new BlockClusterGeometry.Face(
                     corner(var7, var4, 0, var5, 0), corner(var7, var4, 1, var5, 0), corner(var7, var4, 1, var5, 1), corner(var7, var4, 0, var5, 1)
                  )
               );
            }
         }
      }
   }

   private static void addEdges(int[] is, BlockClusterGeometry.Membership membership, List<int[]> list) {
      for (int var3 = 0; var3 < 3; var3++) {
         int var4 = (var3 + 1) % 3;
         int var5 = (var3 + 2) % 3;

         for (byte var6 = -1; var6 <= 1; var6 += 2) {
            for (byte var7 = -1; var7 <= 1; var7 += 2) {
               boolean var9 = contains(membership, is, var4, var6, -1, 0);
               boolean var10 = contains(membership, is, var5, var7, -1, 0);
               boolean var11 = contains(membership, is, var4, var6, var5, var7);
               boolean var12 = !var9 && !var10;
               boolean var8 = var9 && var10 && !var11;
               if (var12 || var8) {
                  int[] var14 = (int[])is.clone();
                  if (var6 > 0) {
                     var14[var4]++;
                  }

                  if (var7 > 0) {
                     var14[var5]++;
                  }

                  list.add(new int[]{var3, var14[0], var14[1], var14[2]});
               }
            }
         }
      }
   }

   private static boolean contains(BlockClusterGeometry.Membership membership, int[] is, int i, int j, int k, int l) {
      int[] var6 = (int[])is.clone();
      var6[i] += j;
      if (k >= 0) {
         var6[k] += l;
      }

      return membership.contains(var6[0], var6[1], var6[2]);
   }

   private static Vec3d corner(int[] is, int i, int j, int k, int l) {
      int[] var5 = (int[])is.clone();
      var5[i] += j;
      var5[k] += l;
      return vec(var5);
   }

   private static Vec3d vec(int[] is) {
      return new Vec3d(is[0], is[1], is[2]);
   }

   @Environment(EnvType.CLIENT)
   public record Face(Vec3d a, Vec3d b, Vec3d c, Vec3d d) {
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Membership {
      boolean contains(int i, int j, int k);
   }

   @Environment(EnvType.CLIENT)
   public record Mesh(List<BlockClusterGeometry.Segment> edges, List<BlockClusterGeometry.Face> faces) {
   }

   @Environment(EnvType.CLIENT)
   public record Segment(Vec3d a, Vec3d b) {
   }
}
