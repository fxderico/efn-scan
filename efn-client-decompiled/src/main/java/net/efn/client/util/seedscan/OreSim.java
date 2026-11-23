package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class OreSim {
   static final byte NO = 0;
   static final byte YES = 1;
   static final byte MAYBE = 2;
   static final byte CERTAIN = 1;
   static final byte POSSIBLE = 2;
   static final int MAX_STATES = Integer.getInteger("efn.oreStates", 1024);
   static final int MAX_WORK = Integer.getInteger("efn.oreWork", 30000);
   static final int MIN_Y = -64;
   static final int MAX_Y = 319;
   private static final int[][] SIDES = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

   private OreSim() {
   }

   static OreSim.Chain run(OreSpec var0, int var1, int var2, OreSim.World var3) {
      WgRandom var4 = new WgRandom();
      long var5 = var4.setDecorationSeed(var3.seed(), var1 << 4, var2 << 4);
      var4.setFeatureSeed(var5, var0.index(), var0.step());
      OreSim.Chain var7 = new OreSim.Chain(var0, var1, var2);
      int var8 = var0.sampleCount(var4);
      Object var9 = new ArrayList();
      var9.add(var4);
      Long2ByteOpenHashMap var10 = var7.writes;
      Long2ByteOpenHashMap var11 = new Long2ByteOpenHashMap();
      int var12 = 0;

      for (int var13 = 0; var13 < var8; var13++) {
         ArrayList var14 = new ArrayList(var9.size() + 2);
         Long2ByteOpenHashMap var15 = new Long2ByteOpenHashMap();
         LongOpenHashSet var16 = null;

         for (WgRandom var18 : var9) {
            var11.clear();
            attempt(var0, var1, var2, var3, var18, var10, var11, var14);
            LongOpenHashSet var19 = new LongOpenHashSet();
            ObjectIterator var20 = var11.long2ByteEntrySet().fastIterator();

            while (var20.hasNext()) {
               Entry var21 = (Entry)var20.next();
               var15.put(var21.getLongKey(), (byte)2);
               if (var21.getByteValue() == 1) {
                  var19.add(var21.getLongKey());
               }
            }

            if (var16 == null) {
               var16 = var19;
            } else {
               var16.retainAll(var19);
            }
         }

         ObjectIterator var22 = var15.long2ByteEntrySet().fastIterator();

         while (var22.hasNext()) {
            long var23 = ((Entry)var22.next()).getLongKey();
            if (var10.get(var23) != 1) {
               var10.put(var23, (byte)2);
            }
         }

         if (var16 != null) {
            LongIterator var24 = var16.iterator();

            while (var24.hasNext()) {
               long var25 = var24.next();
               var10.put(var25, (byte)1);
            }
         }

         if (((var9 = distinct(var14)).size() > MAX_STATES || (var12 += var9.size()) > MAX_WORK) && var13 + 1 < var8) {
            var7.fogFrom = var13 + 1;
            break;
         }
      }

      return var7;
   }

   private static void attempt(
      OreSpec var0, int var1, int var2, OreSim.World var3, WgRandom var4, Long2ByteOpenHashMap var5, Long2ByteOpenHashMap var6, List<WgRandom> var7
   ) {
      int var8 = var4.nextInt(16) + (var1 << 4);
      int var9 = var4.nextInt(16) + (var2 << 4);
      int var10 = var0.sampleY(var4);
      if (!var3.inBiome(var0, var8, var10, var9)) {
         var7.add(var4);
      } else {
         int var11 = var0.size();
         float var12 = var4.nextFloat() * 3.1415927F;
         float var13 = var11 / 8.0F;
         int var14 = MathHelper.method_15386((var11 / 16.0F * 2.0F + 1.0F) / 2.0F);
         double var15 = var8 + Math.sin(var12) * var13;
         double var17 = var8 - Math.sin(var12) * var13;
         double var19 = var9 + Math.cos(var12) * var13;
         double var21 = var9 - Math.cos(var12) * var13;
         double var23 = var10 + var4.nextInt(3) - 2;
         double var25 = var10 + var4.nextInt(3) - 2;
         int var27 = var8 - MathHelper.method_15386(var13) - var14;
         int var28 = var10 - 2 - var14;
         int var29 = var9 - MathHelper.method_15386(var13) - var14;
         int var30 = 2 * (MathHelper.method_15386(var13) + var14);
         int var31 = 2 * (2 + var14);
         byte var32 = gate(var3, var28, var27, var29, var30);
         if (var32 == 0) {
            var7.add(var4);
         } else {
            if (var32 == 2) {
               var7.add(var4.copy());
            }

            doPlace(var0, var1, var2, var3, var4, var5, var6, var32 == 2, var15, var17, var19, var21, var23, var25, var27, var28, var29, var30, var31, var7);
         }
      }
   }

   private static byte gate(OreSim.World var0, int var1, int var2, int var3, int var4) {
      boolean var5 = false;

      for (int var6 = var2; var6 <= var2 + var4; var6++) {
         for (int var7 = var3; var7 <= var3 + var4; var7++) {
            if (var1 <= var0.gateLow(var6, var7)) {
               return 1;
            }

            if (var1 <= var0.gateHigh(var6, var7)) {
               var5 = true;
            }
         }
      }

      return (byte)(var5 ? 2 : 0);
   }

   private static void doPlace(
      OreSpec var0,
      int var1,
      int var2,
      OreSim.World var3,
      WgRandom var4,
      Long2ByteOpenHashMap var5,
      Long2ByteOpenHashMap var6,
      boolean var7,
      double var8,
      double var10,
      double var12,
      double var14,
      double var16,
      double var18,
      int var20,
      int var21,
      int var22,
      int var23,
      int var24,
      List<WgRandom> var25
   ) {
      int var33 = var0.size();
      double[] var34 = new double[var33 * 4];

      for (int var35 = 0; var35 < var33; var35++) {
         float var36 = (float)var35 / var33;
         double var31 = MathHelper.method_16436(var36, var8, var10);
         double var29 = MathHelper.method_16436(var36, var16, var18);
         double var27 = MathHelper.method_16436(var36, var12, var14);
         double var37 = var4.nextDouble() * var33 / 16.0;
         double var39 = ((MathHelper.method_15374(3.1415927F * var36) + 1.0F) * var37 + 1.0) / 2.0;
         var34[var35 * 4] = var31;
         var34[var35 * 4 + 1] = var29;
         var34[var35 * 4 + 2] = var27;
         var34[var35 * 4 + 3] = var39;
      }

      for (int var76 = 0; var76 < var33 - 1; var76++) {
         if (!(var34[var76 * 4 + 3] <= 0.0)) {
            for (int var78 = var76 + 1; var78 < var33; var78++) {
               double var73;
               double var74;
               double var75;
               double var80;
               if (!(var34[var78 * 4 + 3] <= 0.0)
                  && (var80 = var34[var76 * 4 + 3] - var34[var78 * 4 + 3]) * var80
                     > (var75 = var34[var76 * 4] - var34[var78 * 4]) * var75
                        + (var74 = var34[var76 * 4 + 1] - var34[var78 * 4 + 1]) * var74
                        + (var73 = var34[var76 * 4 + 2] - var34[var78 * 4 + 2]) * var73) {
                  if (var80 > 0.0) {
                     var34[var78 * 4 + 3] = -1.0;
                  } else {
                     var34[var76 * 4 + 3] = -1.0;
                  }
               }
            }
         }
      }

      boolean var77 = var0.drawsOnTerrain();
      float var79 = var0.discard();
      boolean var81 = VBlock.isBase(var0.kind());
      WgRandom var38 = var77 ? var4.copy() : null;
      int var82 = 0;
      int var40 = 0;
      boolean var41 = false;
      BitSet var42 = new BitSet(var23 * var24 * var23);

      for (int var26 = 0; var26 < var33; var26++) {
         double var43 = var34[var26 * 4 + 3];
         if (!(var43 < 0.0)) {
            double var45 = var34[var26 * 4];
            double var47 = var34[var26 * 4 + 1];
            double var49 = var34[var26 * 4 + 2];
            int var51 = Math.max(MathHelper.method_15357(var45 - var43), var20);
            int var52 = Math.max(MathHelper.method_15357(var47 - var43), var21);
            int var53 = Math.max(MathHelper.method_15357(var49 - var43), var22);
            int var54 = Math.max(MathHelper.method_15357(var45 + var43), var51);
            int var55 = Math.max(MathHelper.method_15357(var47 + var43), var52);
            int var56 = Math.max(MathHelper.method_15357(var49 + var43), var53);

            for (int var57 = var51; var57 <= var54; var57++) {
               double var58 = (var57 + 0.5 - var45) / var43;
               if (!(var58 * var58 >= 1.0)) {
                  for (int var60 = var52; var60 <= var55; var60++) {
                     double var61 = (var60 + 0.5 - var47) / var43;
                     if (!(var58 * var58 + var61 * var61 >= 1.0)) {
                        for (int var63 = var53; var63 <= var56; var63++) {
                           double var69 = (var63 + 0.5 - var49) / var43;
                           int var68;
                           if (!(var58 * var58 + var61 * var61 + var69 * var69 >= 1.0)
                              && var60 >= -64
                              && var60 <= 319
                              && !var42.get(var68 = var57 - var20 + (var60 - var21) * var23 + (var63 - var22) * var23 * var24)) {
                              var42.set(var68);
                              long var65;
                              byte var67;
                              if (Math.abs((var57 >> 4) - var1) <= 1
                                 && Math.abs((var63 >> 4) - var2) <= 1
                                 && (var67 = baseNow(var3, var5, var81, var57, var60, var63, var65 = BlockPos.method_10064(var57, var60, var63))) != 0) {
                                 if (var67 == 2) {
                                    if (var77) {
                                       var40++;
                                       var41 = true;
                                    }

                                    var6.put(var65, (byte)2);
                                 } else {
                                    int var64;
                                    if (var79 <= 0.0F) {
                                       var64 = 1;
                                    } else if (var79 >= 1.0F) {
                                       var64 = clearOfAir(var3, var5, var57, var60, var63);
                                    } else {
                                       var82++;
                                       if (!var41) {
                                          var64 = var4.nextFloat() >= var79 ? 1 : clearOfAir(var3, var5, var57, var60, var63);
                                       } else {
                                          var64 = clearOfAir(var3, var5, var57, var60, var63) == 1 ? 1 : 2;
                                       }
                                    }

                                    if (var64 == 1) {
                                       var6.put(var65, (byte)(var7 ? 2 : 1));
                                    } else if (var64 == 2) {
                                       var6.put(var65, (byte)2);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (!var77) {
         var25.add(var4);
      } else {
         for (int var72 = 0; var72 <= var40; var72++) {
            WgRandom var83 = var72 == var40 ? var38 : var38.copy();
            var83.skip(var82 + var72);
            var25.add(var83);
         }
      }
   }

   private static byte baseNow(OreSim.World var0, Long2ByteOpenHashMap var1, boolean var2, int var3, int var4, int var5, long var6) {
      byte var8 = var1.get(var6);
      if (var8 != 0 && !var2) {
         return (byte)(var8 == 1 ? 0 : 2);
      } else {
         return var0.base(var3, var4, var5);
      }
   }

   private static byte clearOfAir(OreSim.World var0, Long2ByteOpenHashMap var1, int var2, int var3, int var4) {
      byte var5 = 1;

      for (int[] var9 : SIDES) {
         int var10 = var2 + var9[0];
         int var11 = var3 + var9[1];
         int var12 = var4 + var9[2];
         if (var11 < -64 || var11 > 319) {
            return 0;
         }

         if (var1.get(BlockPos.method_10064(var10, var11, var12)) != 1) {
            byte var13 = var0.air(var10, var11, var12);
            if (var13 == 1) {
               return 0;
            }

            if (var13 == 2) {
               var5 = 2;
            }
         }
      }

      return (byte)var5;
   }

   private static List<WgRandom> distinct(List<WgRandom> var0) {
      if (var0.size() <= 1) {
         return var0;
      } else {
         HashMap var1 = new HashMap();
         ArrayList var2 = new ArrayList(var0.size());

         for (WgRandom var4 : var0) {
            List var5 = var1.computeIfAbsent(var4.stateHash(), var0x -> new ArrayList(1));
            boolean var6 = false;

            for (WgRandom var8 : var5) {
               if (var8.sameState(var4)) {
                  var6 = true;
                  break;
               }
            }

            if (!var6) {
               var5.add(var4);
               var2.add(var4);
            }
         }

         return var2;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Chain {
      final OreSpec spec;
      final int chunkX;
      final int chunkZ;
      final Long2ByteOpenHashMap writes = new Long2ByteOpenHashMap();
      int fogFrom = -1;

      Chain(OreSpec var1, int var2, int var3) {
         this.spec = var1;
         this.chunkX = var2;
         this.chunkZ = var3;
      }

      boolean fogged() {
         return this.fogFrom >= 0;
      }

      int[] fogBox() {
         int var1 = this.spec.halfWidth();
         int var2 = this.spec.reach();
         int var3 = this.chunkX << 4;
         int var4 = this.chunkZ << 4;
         return new int[]{
            Math.max(var3 - var1, var3 - 16),
            Math.max(Math.min(this.spec.yMin(), this.spec.yMax()) - 2 - var2, -64),
            Math.max(var4 - var1, var4 - 16),
            Math.min(var3 + 15 + var1, var3 + 31),
            Math.min(Math.max(this.spec.yMin(), this.spec.yMax()) + 2 + var2, 319),
            Math.min(var4 + 15 + var1, var4 + 31)
         };
      }
   }

   @Environment(EnvType.CLIENT)
   interface World {
      long seed();

      int gateLow(int var1, int var2);

      int gateHigh(int var1, int var2);

      byte base(int var1, int var2, int var3);

      byte air(int var1, int var2, int var3);

      boolean inBiome(OreSpec var1, int var2, int var3, int var4);
   }
}
