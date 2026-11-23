package net.efn.client.util.quantum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.PaletteProvider;
import net.minecraft.world.chunk.PalettedContainer;

@Environment(EnvType.CLIENT)
public final class ChunkSnapshotStore {
   public static final int MIN_SECTION_INDEX = 6;
   public static final int MAX_SECTION_INDEX = 23;
   private static final PaletteProvider<BlockState> STRATEGY = PaletteProvider.method_74162(Block.field_10651);
   private final Map<Long, PalettedContainer<BlockState>[]> snapshots = new HashMap<>();
   private final BlockState air;

   public ChunkSnapshotStore(BlockState var1) {
      this.air = var1;
   }

   public static long chunkKey(int var0, int var1) {
      return (long)var0 << 32 | var1 & 4294967295L;
   }

   public void store(int var1, int var2, PalettedContainer<BlockState>[] var3) {
      this.snapshots.put(chunkKey(var1, var2), var3);
   }

   public void retain(LongPredicate var1) {
      this.snapshots.keySet().removeIf(var1x -> !var1.test(var1x));
   }

   public void clear() {
      this.snapshots.clear();
   }

   public void update(int var1, int var2, int var3, int var4, int var5, int var6, BlockState var7) {
      if (var3 >= 6 && var3 <= 23) {
         PalettedContainer[] var8 = this.snapshots.get(chunkKey(var1, var2));
         if (var8 != null && var3 < var8.length) {
            PalettedContainer var9 = var8[var3];
            if (var9 == null) {
               if (var7 == this.air) {
                  return;
               }

               var8[var3] = var9 = new PalettedContainer(this.air, STRATEGY);
            }

            var9.method_35321(var4, var5, var6, var7);
         }
      }
   }

   public List<ChunkSnapshotStore.BlockChange> compare(int var1, int var2, PalettedContainer<BlockState>[] var3, int var4) {
      ArrayList var5 = new ArrayList();
      PalettedContainer[] var6 = this.snapshots.get(chunkKey(var1, var2));
      if (this.isAllAir(var6)) {
         return var5;
      } else {
         for (int var7 = 6; var7 <= 23; var7++) {
            PalettedContainer var9 = var7 < var6.length ? var6[var7] : null;
            PalettedContainer var8 = var7 < var3.length ? var3[var7] : null;
            if (var9 != null || var8 != null) {
               int var11 = (var7 + var4) * 16;

               for (int var12 = 0; var12 < 16; var12++) {
                  for (int var13 = 0; var13 < 16; var13++) {
                     for (int var14 = 0; var14 < 16; var14++) {
                        BlockState var16 = var9 != null ? (BlockState)var9.method_12321(var14, var12, var13) : this.air;
                        BlockState var15 = var8 != null ? (BlockState)var8.method_12321(var14, var12, var13) : this.air;
                        if (var16 != var15) {
                           var5.add(new ChunkSnapshotStore.BlockChange(var1 * 16 + var14, var11 + var12, var2 * 16 + var13, var15, var16));
                        }
                     }
                  }
               }
            }
         }

         return var5;
      }
   }

   private boolean isAllAir(PalettedContainer<BlockState>[] var1) {
      if (var1 == null) {
         return true;
      } else {
         for (int var2 = 6; var2 <= 23 && var2 < var1.length; var2++) {
            if (var1[var2] != null) {
               return false;
            }
         }

         return true;
      }
   }

   @Environment(EnvType.CLIENT)
   public record BlockChange(int worldX, int worldY, int worldZ, BlockState newState, BlockState oldState) {
   }
}
