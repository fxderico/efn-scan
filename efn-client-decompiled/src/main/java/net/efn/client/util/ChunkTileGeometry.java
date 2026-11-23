package net.efn.client.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
final class ChunkTileGeometry {
   final Vec3d[] outline;
   final Vec3d[] curves;
   final Vec3d[] edges;
   private final double x0;
   private final double z0;
   private final double x1;
   private final double z1;
   private final double radius;
   private final int mask;
   private long usedFrame;

   private ChunkTileGeometry(double var1, double var3, double var5, double var7, double var9, int var11) {
      this.x0 = var1;
      this.z0 = var3;
      this.x1 = var5;
      this.z1 = var7;
      this.radius = var9;
      this.mask = var11;
      boolean var12 = (var11 & 1) != 0;
      boolean var13 = (var11 & 2) != 0;
      boolean var14 = (var11 & 4) != 0;
      boolean var15 = (var11 & 8) != 0;
      double var16 = var12 && var14 ? var9 : 0.0;
      double var18 = var12 && var15 ? var9 : 0.0;
      double var20 = var13 && var15 ? var9 : 0.0;
      double var22 = var13 && var14 ? var9 : 0.0;
      ArrayList var24 = new ArrayList(20);
      ArrayList var25 = new ArrayList(32);
      ArrayList var26 = new ArrayList(8);
      corner(var24, var25, var1 + var16, var3 + var16, var16, 180.0);
      corner(var24, var25, var5 - var18, var3 + var18, var18, 270.0);
      corner(var24, var25, var5 - var20, var7 - var20, var20, 0.0);
      corner(var24, var25, var1 + var22, var7 - var22, var22, 90.0);
      if (var12) {
         edge(var26, var1 + var16, var3, var5 - var18, var3);
      }

      if (var13) {
         edge(var26, var1 + var22, var7, var5 - var20, var7);
      }

      if (var14) {
         edge(var26, var1, var3 + var16, var1, var7 - var22);
      }

      if (var15) {
         edge(var26, var5, var3 + var18, var5, var7 - var20);
      }

      this.outline = var24.toArray(new Vec3d[0]);
      this.curves = var25.toArray(new Vec3d[0]);
      this.edges = var26.toArray(new Vec3d[0]);
   }

   private boolean matches(double var1, double var3, double var5, double var7, double var9, int var11) {
      return this.mask == var11
         && Double.compare(this.x0, var1) == 0
         && Double.compare(this.z0, var3) == 0
         && Double.compare(this.x1, var5) == 0
         && Double.compare(this.z1, var7) == 0
         && Double.compare(this.radius, var9) == 0;
   }

   private static void corner(List<Vec3d> var0, List<Vec3d> var1, double var2, double var4, double var6, double var8) {
      if (var6 <= 0.0) {
         var0.add(new Vec3d(var2, 63.0, var4));
      } else {
         Vec3d var10 = null;

         for (int var11 = 0; var11 <= 4; var11++) {
            double var12 = Math.toRadians(var8 + 90.0 * var11 / 4.0);
            Vec3d var14 = new Vec3d(var2 + Math.cos(var12) * var6, 63.0, var4 + Math.sin(var12) * var6);
            var0.add(var14);
            if (var10 != null) {
               var1.add(var10);
               var1.add(var14);
            }

            var10 = var14;
         }
      }
   }

   private static void edge(List<Vec3d> var0, double var1, double var3, double var5, double var7) {
      var0.add(new Vec3d(var1, 63.0, var3));
      var0.add(new Vec3d(var5, 63.0, var7));
   }

   @Environment(EnvType.CLIENT)
   static final class Cache {
      private static final int CAPACITY = 512;
      private final LinkedHashMap<Long, ChunkTileGeometry> tiles = new LinkedHashMap<>(64, 0.75F, true);
      private long frame;

      Cache() {
      }

      void beginFrame() {
         this.frame++;
      }

      void clear() {
         this.tiles.clear();
      }

      int size() {
         return this.tiles.size();
      }

      ChunkTileGeometry get(long var1, double var3, double var5, double var7, double var9, double var11, int var13) {
         ChunkTileGeometry var14 = this.tiles.get(var1);
         if (var14 != null && var14.matches(var3, var5, var7, var9, var11, var13)) {
            var14.usedFrame = this.frame;
            return var14;
         } else {
            ChunkTileGeometry var15 = new ChunkTileGeometry(var3, var5, var7, var9, var11, var13);
            var15.usedFrame = this.frame;
            if (var14 == null && this.tiles.size() >= 512) {
               Iterator var16 = this.tiles.entrySet().iterator();
               Entry var17 = (Entry)var16.next();
               if (((ChunkTileGeometry)var17.getValue()).usedFrame < this.frame - 1L) {
                  var16.remove();
                  this.tiles.put(var1, var15);
               }
            } else {
               this.tiles.put(var1, var15);
            }

            return var15;
         }
      }
   }
}
