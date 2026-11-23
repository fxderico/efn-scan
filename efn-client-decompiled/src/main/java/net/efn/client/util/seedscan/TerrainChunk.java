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

   private TerrainChunk(int var1, int var2) {
      this.chunkX = var1;
      this.chunkZ = var2;
   }

   static TerrainChunk of(ProtoChunk var0) {
      TerrainChunk var1 = new TerrainChunk(var0.method_12004().field_9181, var0.method_12004().field_9180);

      for (int var2 = 0; var2 < 24; var2++) {
         ChunkSection var3 = var0.method_38259(var2);
         if (var3.method_38292()) {
            var1.uniform[var2] = 0;
         } else {
            byte var4 = VBlock.of(var3.method_12254(0, 0, 0));
            byte[] var5 = null;

            for (int var6 = 0; var6 < 4096; var6++) {
               BlockState var7 = var3.method_12254(var6 & 15, var6 >> 8, var6 >> 4 & 15);
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
            var1.oceanFloor[var11 + 16 * var10] = (short)(var0.method_12005(Type.field_13195, var11, var10) + 1);
         }
      }

      return var1;
   }

   private static void put(byte[] var0, int var1, byte var2) {
      int var3 = var1 >> 1;
      var0[var3] = (var1 & 1) == 0 ? (byte)(var0[var3] & 240 | var2) : (byte)(var0[var3] & 15 | var2 << 4);
   }

   public byte kind(int var1, int var2, int var3) {
      if (var2 >= -64 && var2 < 320) {
         int var4 = var2 - -64 >> 4;
         byte[] var5 = this.nibbles[var4];
         if (var5 == null) {
            return this.uniform[var4];
         } else {
            int var6 = (var2 & 15) << 8 | (var3 & 15) << 4 | var1 & 15;
            byte var7 = var5[var6 >> 1];
            return (byte)((var6 & 1) == 0 ? var7 & 15 : var7 >> 4 & 15);
         }
      } else {
         return 0;
      }
   }

   public int oceanFloor(int var1, int var2) {
      return this.oceanFloor[(var1 & 15) + 16 * (var2 & 15)];
   }
}
