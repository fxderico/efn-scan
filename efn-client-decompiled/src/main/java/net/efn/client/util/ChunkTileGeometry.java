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

   private ChunkTileGeometry(double d, double e, double f, double g, double h, int i) {
      this.x0 = d;
      this.z0 = e;
      this.x1 = f;
      this.z1 = g;
      this.radius = h;
      this.mask = i;
      boolean var12 = (i & 1) != 0;
      boolean var13 = (i & 2) != 0;
      boolean var14 = (i & 4) != 0;
      boolean var15 = (i & 8) != 0;
      double var16 = var12 && var14 ? h : 0.0;
      double var18 = var12 && var15 ? h : 0.0;
      double var20 = var13 && var15 ? h : 0.0;
      double var22 = var13 && var14 ? h : 0.0;
      ArrayList var24 = new ArrayList(20);
      ArrayList var25 = new ArrayList(32);
      ArrayList var26 = new ArrayList(8);
      corner(var24, var25, d + var16, e + var16, var16, 180.0);
      corner(var24, var25, f - var18, e + var18, var18, 270.0);
      corner(var24, var25, f - var20, g - var20, var20, 0.0);
      corner(var24, var25, d + var22, g - var22, var22, 90.0);
      if (var12) {
         edge(var26, d + var16, e, f - var18, e);
      }

      if (var13) {
         edge(var26, d + var22, g, f - var20, g);
      }

      if (var14) {
         edge(var26, d, e + var16, d, g - var22);
      }

      if (var15) {
         edge(var26, f, e + var18, f, g - var20);
      }

      this.outline = var24.toArray(new Vec3d[0]);
      this.curves = var25.toArray(new Vec3d[0]);
      this.edges = var26.toArray(new Vec3d[0]);
   }

   private boolean matches(double d, double e, double f, double g, double h, int i) {
      return this.mask == i
         && Double.compare(this.x0, d) == 0
         && Double.compare(this.z0, e) == 0
         && Double.compare(this.x1, f) == 0
         && Double.compare(this.z1, g) == 0
         && Double.compare(this.radius, h) == 0;
   }

   private static void corner(List<Vec3d> list, List<Vec3d> list2, double d, double e, double f, double g) {
      if (f <= 0.0) {
         list.add(new Vec3d(d, 63.0, e));
      } else {
         Vec3d var10 = null;

         for (int var11 = 0; var11 <= 4; var11++) {
            double var12 = Math.toRadians(g + 90.0 * var11 / 4.0);
            Vec3d var14 = new Vec3d(d + Math.cos(var12) * f, 63.0, e + Math.sin(var12) * f);
            list.add(var14);
            if (var10 != null) {
               list2.add(var10);
               list2.add(var14);
            }

            var10 = var14;
         }
      }
   }

   private static void edge(List<Vec3d> list, double d, double e, double f, double g) {
      list.add(new Vec3d(d, 63.0, e));
      list.add(new Vec3d(f, 63.0, g));
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

      ChunkTileGeometry get(long l, double d, double e, double f, double g, double h, int i) {
         ChunkTileGeometry var14 = this.tiles.get(l);
         if (var14 != null && var14.matches(d, e, f, g, h, i)) {
            var14.usedFrame = this.frame;
            return var14;
         } else {
            ChunkTileGeometry var15 = new ChunkTileGeometry(d, e, f, g, h, i);
            var15.usedFrame = this.frame;
            if (var14 == null && this.tiles.size() >= 512) {
               Iterator var16 = this.tiles.entrySet().iterator();
               Entry var17 = (Entry)var16.next();
               if (((ChunkTileGeometry)var17.getValue()).usedFrame < this.frame - 1L) {
                  var16.remove();
                  this.tiles.put(l, var15);
               }
            } else {
               this.tiles.put(l, var15);
            }

            return var15;
         }
      }
   }
}
