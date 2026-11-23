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
   private static final PaletteProvider<BlockState> STRATEGY = PaletteProvider.forBlockStates(Block.STATE_IDS);
   private final Map<Long, PalettedContainer<BlockState>[]> snapshots = new HashMap<>();
   private final BlockState air;

   public ChunkSnapshotStore(BlockState blockState) {
      this.air = blockState;
   }

   public static long chunkKey(int i, int j) {
      return (long)i << 32 | j & 4294967295L;
   }

   public void store(int i, int j, PalettedContainer<BlockState>[] palettedContainers) {
      this.snapshots.put(chunkKey(i, j), palettedContainers);
   }

   public void retain(LongPredicate longPredicate) {
      this.snapshots.keySet().removeIf(long_ -> !longPredicate.test(long_));
   }

   public void clear() {
      this.snapshots.clear();
   }

   public void update(int i, int j, int k, int l, int m, int n, BlockState blockState) {
      if (k >= 6 && k <= 23) {
         PalettedContainer[] var8 = this.snapshots.get(chunkKey(i, j));
         if (var8 != null && k < var8.length) {
            PalettedContainer var9 = var8[k];
            if (var9 == null) {
               if (blockState == this.air) {
                  return;
               }

               var8[k] = var9 = new PalettedContainer<>(this.air, STRATEGY);
            }

            var9.set(l, m, n, blockState);
         }
      }
   }

   public List<ChunkSnapshotStore.BlockChange> compare(int i, int j, PalettedContainer<BlockState>[] palettedContainers, int k) {
      ArrayList var5 = new ArrayList();
      PalettedContainer[] var6 = this.snapshots.get(chunkKey(i, j));
      if (this.isAllAir(var6)) {
         return var5;
      } else {
         for (int var7 = 6; var7 <= 23; var7++) {
            PalettedContainer var9 = var7 < var6.length ? var6[var7] : null;
            PalettedContainer var8 = var7 < palettedContainers.length ? palettedContainers[var7] : null;
            if (var9 != null || var8 != null) {
               int var11 = (var7 + k) * 16;

               for (int var12 = 0; var12 < 16; var12++) {
                  for (int var13 = 0; var13 < 16; var13++) {
                     for (int var14 = 0; var14 < 16; var14++) {
                        BlockState var16 = var9 != null ? (BlockState)var9.get(var14, var12, var13) : this.air;
                        BlockState var15 = var8 != null ? (BlockState)var8.get(var14, var12, var13) : this.air;
                        if (var16 != var15) {
                           var5.add(new ChunkSnapshotStore.BlockChange(i * 16 + var14, var11 + var12, j * 16 + var13, var15, var16));
                        }
                     }
                  }
               }
            }
         }

         return var5;
      }
   }

   private boolean isAllAir(PalettedContainer<BlockState>[] palettedContainers) {
      if (palettedContainers == null) {
         return true;
      } else {
         for (int var2 = 6; var2 <= 23 && var2 < palettedContainers.length; var2++) {
            if (palettedContainers[var2] != null) {
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
