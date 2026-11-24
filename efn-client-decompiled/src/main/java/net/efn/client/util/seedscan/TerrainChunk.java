package net.efn.client.util.seedscan;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;

@Environment(EnvType.CLIENT)
public final class TerrainChunk {
   public static final int MIN_Y = -64;
   public static final int HEIGHT = 384;
   public static final int SECTIONS = 24;
   public final int chunkX;
   public final int chunkZ;
   private final byte[][] nibbles = new byte[24][];
   private final byte[] uniform = new byte[24];
   private final short[] oceanFloor = new short[256];

   private TerrainChunk(int i, int j) {
      this.chunkX = i;
      this.chunkZ = j;
   }

   static TerrainChunk of(ProtoChunk protoChunk) {
      TerrainChunk var1 = new TerrainChunk(protoChunk.getPos().x, protoChunk.getPos().z);

      for (int var2 = 0; var2 < 24; var2++) {
         ChunkSection var3 = protoChunk.getSection(var2);
         if (var3.isEmpty()) {
            var1.uniform[var2] = 0;
         } else {
            byte var4 = VBlock.of(var3.getBlockState(0, 0, 0));
            byte[] var5 = null;

            for (int var6 = 0; var6 < 4096; var6++) {
               BlockState var7 = var3.getBlockState(var6 & 15, var6 >> 8, var6 >> 4 & 15);
               byte var8 = VBlock.of(var7);
               if (var5 == null) {
                  if (var8 == var4) {
                     continue;
                  }

                  var5 = new byte[2048];

                  for (int var9 = 0; var9 < var6; var9++) {
                     put(var5, var9, var4);
                  }
               }

               put(var5, var6, var8);
            }

            var1.uniform[var2] = var4;
            var1.nibbles[var2] = var5;
         }
      }

      for (int var10 = 0; var10 < 16; var10++) {
         for (int var11 = 0; var11 < 16; var11++) {
            var1.oceanFloor[var11 + 16 * var10] = (short)(protoChunk.sampleHeightmap(Type.OCEAN_FLOOR_WG, var11, var10) + 1);
         }
      }

      return var1;
   }

   private static void put(byte[] bs, int i, byte b) {
      int var3 = i >> 1;
      bs[var3] = (i & 1) == 0 ? (byte)(bs[var3] & 240 | b) : (byte)(bs[var3] & 15 | b << 4);
   }

   public byte kind(int i, int j, int k) {
      if (j >= -64 && j < 320) {
         int var4 = j - -64 >> 4;
         byte[] var5 = this.nibbles[var4];
         if (var5 == null) {
            return this.uniform[var4];
         } else {
            int var6 = (j & 15) << 8 | (k & 15) << 4 | i & 15;
            byte var7 = var5[var6 >> 1];
            return (byte)((var6 & 1) == 0 ? var7 & 15 : var7 >> 4 & 15);
         }
      } else {
         return 0;
      }
   }

   public int oceanFloor(int i, int j) {
      return this.oceanFloor[(i & 15) + 16 * (j & 15)];
   }
}
