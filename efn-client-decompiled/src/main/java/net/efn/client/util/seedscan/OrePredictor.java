package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class OrePredictor {
   private static final ThreadLocal<OrePredictor.Answers> ANSWERS = ThreadLocal.withInitial(OrePredictor.Answers::new);

   private OrePredictor() {
   }

   static boolean baseish(byte var0) {
      return VBlock.isBase(var0) || var0 == 11;
   }

   static boolean disagreesOnBase(OrePredictor.Inputs var0, byte var1, int var2, int var3, int var4) {
      byte var5 = var0.shown(var2, var3, var4);
      if (var5 == 15) {
         return true;
      } else {
         boolean var6 = baseish(var1);
         return var5 != 11 ? var6 != VBlock.isBase(var5) : !var6 || var1 != 11 && !var0.predictedOre(var2, var3, var4);
      }
   }

   static boolean disagreesOnAir(OrePredictor.Inputs var0, byte var1, int var2, int var3, int var4) {
      byte var5 = var0.shown(var2, var3, var4);
      return var5 == 15 ? true : var1 == 0 != (var5 == 0) || disagreesOnBase(var0, var1, var2, var3, var4);
   }

   public static ChunkDecoration predict(SeedWorldgen var0, int var1, int var2, OrePredictor.Inputs var3) {
      OrePredictor.View var4 = new OrePredictor.View(var0, var1, var2, var3);
      Iterator var5 = var0.specs().iterator();

      while (var5.hasNext()) {
         OreSpec var6;
         var4.spec = var6 = (OreSpec)var5.next();
         OreSim.Chain var7 = OreSim.run(var6, var1, var2, var4);
         ObjectIterator var8 = var7.writes.long2ByteEntrySet().fastIterator();

         while (var8.hasNext()) {
            Entry var9 = (Entry)var8.next();
            var4.builder.add(var9.getLongKey(), ChunkDecoration.pack(var6.order(), var6.kind(), var9.getByteValue() == 2));
         }

         if (var7.fogged()) {
            int[] var10 = var7.fogBox();
            var4.builder.fog(new ChunkDecoration.Fog(var6.order(), var6.kind(), var10[0], var10[1], var10[2], var10[3], var10[4], var10[5]));
         }
      }

      return var4.builder.build();
   }

   @Environment(EnvType.CLIENT)
   private static final class Answers {
      static final int SIZE = 884736;
      final int[] stamp = new int[884736];
      final byte[] answer = new byte[884736];
      int generation;

      private Answers() {
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Inputs {
      TerrainChunk terrain(int var1, int var2);

      GateHeights gateHeights(int var1, int var2);

      boolean othersMayWrite(int var1, int var2, int var3);

      byte shown(int var1, int var2, int var3);

      boolean predictedOre(int var1, int var2, int var3);
   }

   @Environment(EnvType.CLIENT)
   private static final class View implements OreSim.World {
      final SeedWorldgen world;
      final int chunkX;
      final int chunkZ;
      final OrePredictor.Inputs inputs;
      final TerrainChunk[] terrains = new TerrainChunk[9];
      final GateHeights[] heights = new GateHeights[9];
      final ChunkDecoration.Builder builder;
      final OrePredictor.Answers answers = OrePredictor.ANSWERS.get();
      final int generation;
      final int minX;
      final int minZ;
      OreSpec spec;

      View(SeedWorldgen var1, int var2, int var3, OrePredictor.Inputs var4) {
         this.world = var1;
         this.chunkX = var2;
         this.chunkZ = var3;
         this.inputs = var4;
         this.builder = new ChunkDecoration.Builder(var2, var3);

         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               this.terrains[(var5 + 1) * 3 + var6 + 1] = var4.terrain(var2 + var5, var3 + var6);
               this.heights[(var5 + 1) * 3 + var6 + 1] = var4.gateHeights(var2 + var5, var3 + var6);
            }
         }

         this.generation = ++this.answers.generation;
         this.minX = var2 - 1 << 4;
         this.minZ = var3 - 1 << 4;
      }

      private int around(int var1, int var2) {
         int var3 = (var1 >> 4) - this.chunkX;
         int var4 = (var2 >> 4) - this.chunkZ;
         if (var3 >= -1 && var3 <= 1 && var4 >= -1 && var4 <= 1) {
            return (var3 + 1) * 3 + var4 + 1;
         } else {
            throw new IllegalStateException("read outside the 3x3 chunks at " + var1 + ", " + var2);
         }
      }

      private TerrainChunk terrain(int var1, int var2) {
         return this.terrains[this.around(var1, var2)];
      }

      private byte answers(int var1, int var2, int var3) {
         int var4 = var3 - this.minZ;
         int var5 = var1 - this.minX;
         int var6 = ((var2 - -64) * 48 + var4) * 48 + var5;
         if (this.answers.stamp[var6] == this.generation) {
            return this.answers.answer[var6];
         } else {
            byte var7 = this.terrain(var1, var3).kind(var1, var2, var3);
            int var8 = VBlock.isBase(var7) ? 1 : 0;
            boolean var9 = OrePredictor.disagreesOnBase(this.inputs, var7, var1, var2, var3);
            int var10 = var9 ? 2 : var8;
            int var11 = var10;
            if (var10 == 1 && this.inputs.othersMayWrite(var1, var2, var3)) {
               var11 = 2;
            }

            int var12 = OrePredictor.disagreesOnAir(this.inputs, var7, var1, var2, var3) ? 2 : (var7 == 0 ? 1 : 0);
            byte var13 = (byte)(var11 | var10 << 2 | var12 << 4);
            this.answers.stamp[var6] = this.generation;
            this.answers.answer[var6] = var13;
            return var13;
         }
      }

      @Override
      public long seed() {
         return this.world.seed;
      }

      @Override
      public int gateLow(int var1, int var2) {
         return this.heights[this.around(var1, var2)].low(var1, var2);
      }

      @Override
      public int gateHigh(int var1, int var2) {
         return this.heights[this.around(var1, var2)].high(var1, var2);
      }

      @Override
      public byte base(int var1, int var2, int var3) {
         int var5;
         if (this.spec.drawsOnTerrain()) {
            var5 = this.answers(var1, var2, var3) & 3;
            if (var5 == 1 && this.ownFogCovers(var1, var2, var3)) {
               var5 = 2;
            }
         } else if (this.spec.discard() > 0.0F) {
            var5 = (byte)(this.answers(var1, var2, var3) >> 2 & 3);
         } else {
            var5 = VBlock.isBase(this.terrain(var1, var3).kind(var1, var2, var3)) ? 1 : 0;
         }

         ShortArrayList var4;
         if ((var4 = this.builder.at(BlockPos.method_10064(var1, var2, var3))) != null) {
            for (int var6 = 0; var6 < var4.size(); var6++) {
               short var7 = var4.getShort(var6);
               if (!VBlock.isBase(ChunkDecoration.kind(var7))) {
                  var5 = ChunkDecoration.possible(var7) && var5 != 0 ? 2 : 0;
               }
            }
         }

         return (byte)var5;
      }

      private boolean ownFogCovers(int var1, int var2, int var3) {
         for (ChunkDecoration.Fog var5 : this.builder.fogList()) {
            if (!VBlock.isBase(var5.kind()) && var5.contains(var1, var2, var3)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public byte air(int var1, int var2, int var3) {
         if (var2 >= -64 && var2 <= 319) {
            if (this.spec.discard() > 0.0F) {
               return (byte)(this.answers(var1, var2, var3) >> 4 & 3);
            } else {
               return (byte)(this.terrain(var1, var3).kind(var1, var2, var3) == 0 ? 1 : 0);
            }
         } else {
            return 1;
         }
      }

      @Override
      public boolean inBiome(OreSpec var1, int var2, int var3, int var4) {
         return var1.biomes() == null || var1.inBiome(this.world.biomeAt(var2, var3, var4));
      }
   }
}
