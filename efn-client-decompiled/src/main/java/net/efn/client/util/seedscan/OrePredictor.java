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

   static boolean baseish(byte b) {
      return VBlock.isBase(b) || b == 11;
   }

   static boolean disagreesOnBase(OrePredictor.Inputs inputs, byte b, int i, int j, int k) {
      byte var5 = inputs.shown(i, j, k);
      if (var5 == 15) {
         return true;
      } else {
         boolean var6 = baseish(b);
         return var5 != 11 ? var6 != VBlock.isBase(var5) : !var6 || b != 11 && !inputs.predictedOre(i, j, k);
      }
   }

   static boolean disagreesOnAir(OrePredictor.Inputs inputs, byte b, int i, int j, int k) {
      byte var5 = inputs.shown(i, j, k);
      return var5 == 15 ? true : b == 0 != (var5 == 0) || disagreesOnBase(inputs, b, i, j, k);
   }

   public static ChunkDecoration predict(SeedWorldgen seedWorldgen, int i, int j, OrePredictor.Inputs inputs) {
      OrePredictor.View var4 = new OrePredictor.View(seedWorldgen, i, j, inputs);
      Iterator var5 = seedWorldgen.specs().iterator();

      while (var5.hasNext()) {
         OreSpec var6;
         var4.spec = var6 = (OreSpec)var5.next();
         OreSim.Chain var7 = OreSim.run(var6, i, j, var4);
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
      TerrainChunk terrain(int i, int j);

      GateHeights gateHeights(int i, int j);

      boolean othersMayWrite(int i, int j, int k);

      byte shown(int i, int j, int k);

      boolean predictedOre(int i, int j, int k);
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

      View(SeedWorldgen seedWorldgen, int i, int j, OrePredictor.Inputs inputs) {
         this.world = seedWorldgen;
         this.chunkX = i;
         this.chunkZ = j;
         this.inputs = inputs;
         this.builder = new ChunkDecoration.Builder(i, j);

         for (int var5 = -1; var5 <= 1; var5++) {
            for (int var6 = -1; var6 <= 1; var6++) {
               this.terrains[(var5 + 1) * 3 + var6 + 1] = inputs.terrain(i + var5, j + var6);
               this.heights[(var5 + 1) * 3 + var6 + 1] = inputs.gateHeights(i + var5, j + var6);
            }
         }

         this.generation = ++this.answers.generation;
         this.minX = i - 1 << 4;
         this.minZ = j - 1 << 4;
      }

      private int around(int i, int j) {
         int var3 = (i >> 4) - this.chunkX;
         int var4 = (j >> 4) - this.chunkZ;
         if (var3 >= -1 && var3 <= 1 && var4 >= -1 && var4 <= 1) {
            return (var3 + 1) * 3 + var4 + 1;
         } else {
            throw new IllegalStateException("read outside the 3x3 chunks at " + i + ", " + j);
         }
      }

      private TerrainChunk terrain(int i, int j) {
         return this.terrains[this.around(i, j)];
      }

      private byte answers(int i, int j, int k) {
         int var4 = k - this.minZ;
         int var5 = i - this.minX;
         int var6 = ((j - -64) * 48 + var4) * 48 + var5;
         if (this.answers.stamp[var6] == this.generation) {
            return this.answers.answer[var6];
         } else {
            byte var7 = this.terrain(i, k).kind(i, j, k);
            int var8 = VBlock.isBase(var7) ? 1 : 0;
            boolean var9 = OrePredictor.disagreesOnBase(this.inputs, var7, i, j, k);
            int var10 = var9 ? 2 : var8;
            int var11 = var10;
            if (var10 == 1 && this.inputs.othersMayWrite(i, j, k)) {
               var11 = 2;
            }

            int var12 = OrePredictor.disagreesOnAir(this.inputs, var7, i, j, k) ? 2 : (var7 == 0 ? 1 : 0);
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
      public int gateLow(int i, int j) {
         return this.heights[this.around(i, j)].low(i, j);
      }

      @Override
      public int gateHigh(int i, int j) {
         return this.heights[this.around(i, j)].high(i, j);
      }

      @Override
      public byte base(int i, int j, int k) {
         int var5;
         if (this.spec.drawsOnTerrain()) {
            var5 = this.answers(i, j, k) & 3;
            if (var5 == 1 && this.ownFogCovers(i, j, k)) {
               var5 = 2;
            }
         } else if (this.spec.discard() > 0.0F) {
            var5 = (byte)(this.answers(i, j, k) >> 2 & 3);
         } else {
            var5 = VBlock.isBase(this.terrain(i, k).kind(i, j, k)) ? 1 : 0;
         }

         ShortArrayList var4;
         if ((var4 = this.builder.at(BlockPos.asLong(i, j, k))) != null) {
            for (int var6 = 0; var6 < var4.size(); var6++) {
               short var7 = var4.getShort(var6);
               if (!VBlock.isBase(ChunkDecoration.kind(var7))) {
                  var5 = ChunkDecoration.possible(var7) && var5 != 0 ? 2 : 0;
               }
            }
         }

         return (byte)var5;
      }

      private boolean ownFogCovers(int i, int j, int k) {
         for (ChunkDecoration.Fog var5 : this.builder.fogList()) {
            if (!VBlock.isBase(var5.kind()) && var5.contains(i, j, k)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public byte air(int i, int j, int k) {
         if (j >= -64 && j <= 319) {
            if (this.spec.discard() > 0.0F) {
               return (byte)(this.answers(i, j, k) >> 4 & 3);
            } else {
               return (byte)(this.terrain(i, k).kind(i, j, k) == 0 ? 1 : 0);
            }
         } else {
            return 1;
         }
      }

      @Override
      public boolean inBiome(OreSpec oreSpec, int i, int j, int k) {
         return oreSpec.biomes() == null || oreSpec.inBiome(this.world.biomeAt(i, j, k));
      }
   }
}
