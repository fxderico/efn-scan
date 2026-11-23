package net.efn.client.util.seedscan;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.world.chunk.PalettedContainer;

@Environment(EnvType.CLIENT)
public final class ShownChunk {
   static final int KEEP_TOP = 207;
   public final int chunkX;
   public final int chunkZ;
   private final byte[][] nibbles = new byte[24][];
   private final byte[] uniform = new byte[24];
   private final short[] terrainTop = new short[256];
   private final boolean[] iceTop = new boolean[256];
   private final short[] motionTop = new short[256];

   private ShownChunk(int var1, int var2) {
      this.chunkX = var1;
      this.chunkZ = var2;
      Arrays.fill(this.terrainTop, (short)-64);
      Arrays.fill(this.motionTop, (short)-64);
   }

   public static ShownChunk of(int var0, int var1, PalettedContainer<BlockState>[] var2) {
      ShownChunk var3 = new ShownChunk(var0, var1);

      for (int var4 = 0; var4 < 24; var4++) {
         PalettedContainer var5 = var4 < var2.length ? var2[var4] : null;
         if (var5 == null) {
            var3.uniform[var4] = 0;
         } else {
            boolean var7 = -64 + (var4 << 4) <= 207;
            byte var8 = VBlock.of((BlockState)var5.method_12321(0, 0, 0));
            byte[] var9 = null;

            for (int var10 = 0; var10 < 4096; var10++) {
               int var11 = var10 & 15;
               int var12 = var10 >> 8;
               int var13 = var10 >> 4 & 15;
               BlockState var14 = (BlockState)var5.method_12321(var11, var12, var13);
               byte var15 = VBlock.of(var14);
               int var16 = -64 + (var4 << 4) + var12 + 1;
               int var17 = var11 + 16 * var13;
               if (var15 != 0 && var14.method_51366() && var16 > var3.motionTop[var17]) {
                  var3.motionTop[var17] = (short)var16;
               }

               if (isTerrain(var14, var15) && var16 > var3.terrainTop[var17]) {
                  var3.terrainTop[var17] = (short)var16;
                  var3.iceTop[var17] = isIce(var14);
               }

               if (var7) {
                  if (var9 == null) {
                     if (var15 == var8) {
                        continue;
                     }

                     var9 = new byte[2048];

                     for (int var18 = 0; var18 < var10; var18++) {
                        put(var9, var18, var8);
                     }
                  }

                  put(var9, var10, var15);
               }
            }

            var3.uniform[var4] = var7 ? var8 : 15;
            var3.nibbles[var4] = var9;
         }
      }

      return var3;
   }

   static boolean isTerrain(BlockState var0, byte var1) {
      if (var1 == 0 || var1 == 1 || var1 == 2) {
         return false;
      } else if (var1 != 12) {
         return true;
      } else if (!var0.method_51366()) {
         return false;
      } else {
         String var2 = Registries.field_41175.method_10221(var0.method_26204()).method_12832();
         return var2.endsWith("dirt")
            || var2.equals("grass_block")
            || var2.equals("podzol")
            || var2.equals("mycelium")
            || var2.endsWith("sand")
            || var2.endsWith("sandstone")
            || var2.endsWith("terracotta")
            || var2.equals("snow_block")
            || var2.equals("packed_ice")
            || var2.equals("blue_ice")
            || var2.equals("mud")
            || var2.equals("calcite")
            || var2.equals("powder_snow")
            || var2.equals("moss_block")
            || var2.equals("clay")
            || var2.equals("dripstone_block")
            || var2.equals("magma_block")
            || var2.equals("smooth_basalt");
      }
   }

   static boolean isIce(BlockState var0) {
      return var0.method_27852(Blocks.field_10225) || var0.method_27852(Blocks.field_10384) || var0.method_27852(Blocks.field_10491);
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

   public int terrainTop(int var1, int var2) {
      int var3 = (var1 & 15) + 16 * (var2 & 15);
      return this.iceTop[var3] ? -2147483648 : this.terrainTop[var3];
   }

   public int motionTop(int var1, int var2) {
      return this.motionTop[(var1 & 15) + 16 * (var2 & 15)];
   }
}
