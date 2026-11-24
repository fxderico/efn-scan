package net.efn.client.util.netherite;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;

@Environment(EnvType.CLIENT)
public final class NetheriteChunkTracker {
   static final int MAXIMUM_CANDIDATES = 512;
   private static final int ANCIENT_STATE_ID = Block.getRawIdFromState(Blocks.ANCIENT_DEBRIS.getDefaultState());
   private static final RegistryKey<World> DONUT_NETHER = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("worlds", "smp_nether"));
   private static final Set<NetheriteChunkTracker.Candidate> CANDIDATES = new LinkedHashSet<>();
   private static WeakReference<ClientWorld> lastLevel = new WeakReference<>(null);
   private static RegistryKey<World> lastDimension;
   private static NetheriteChunkTracker.CandidateListener listener;
   private static NetheriteChunkTracker.RevealListener revealListener;

   private NetheriteChunkTracker() {
   }

   public static void onPacketApplied(Packet<?> packet) {
      ClientWorld var1 = MinecraftClient.getInstance().world;
      if (var1 != null && isNether(var1)) {
         if (packet instanceof ChunkDataS2CPacket var2) {
            trackLevel(var1);

            for (int var4 : HiddenDebrisScanner.findHiddenSections(
               var2.getChunkData(), var1.countVerticalSections(), var1.getBottomSectionCoord(), ANCIENT_STATE_ID
            )) {
               add(new NetheriteChunkTracker.Candidate(var2.getChunkX(), var2.getChunkZ(), var4));
            }
         } else if (packet instanceof BlockUpdateS2CPacket var5) {
            onBlockUpdate(var5.getPos(), var5.getState());
         } else if (packet instanceof ChunkDeltaUpdateS2CPacket var6) {
            var6.visitUpdates(NetheriteChunkTracker::onBlockUpdate);
         }
      }
   }

   private static void onBlockUpdate(BlockPos blockPos, BlockState blockState) {
      if (Block.getRawIdFromState(blockState) == ANCIENT_STATE_ID) {
         if (revealListener != null) {
            revealListener.onRevealed(blockPos.toImmutable());
         }

         CANDIDATES.remove(new NetheriteChunkTracker.Candidate(blockPos.getX() >> 4, blockPos.getZ() >> 4, blockPos.getY() >> 4));
      }
   }

   private static void add(NetheriteChunkTracker.Candidate candidate) {
      if (!CANDIDATES.contains(candidate)) {
         if (CANDIDATES.size() >= 512) {
            Iterator var1 = CANDIDATES.iterator();
            var1.next();
            var1.remove();
         }

         CANDIDATES.add(candidate);
         if (listener != null) {
            listener.onCandidate(candidate);
         }
      }
   }

   private static void trackLevel(ClientWorld clientWorld) {
      RegistryKey var1 = clientWorld.getRegistryKey();
      if (clientWorld != lastLevel.get() || !var1.equals(lastDimension)) {
         CANDIDATES.clear();
         lastLevel = new WeakReference<>(clientWorld);
         lastDimension = var1;
      }
   }

   public static boolean isNether(ClientWorld clientWorld) {
      RegistryKey var1 = clientWorld.getRegistryKey();
      return var1 == World.NETHER || DONUT_NETHER.equals(var1) || clientWorld.getDimensionEntry().matchesKey(DimensionTypes.THE_NETHER);
   }

   public static List<NetheriteChunkTracker.Candidate> snapshot() {
      return MinecraftClient.getInstance().world != lastLevel.get() ? List.of() : List.copyOf(CANDIDATES);
   }

   public static void setListener(NetheriteChunkTracker.CandidateListener candidateListener) {
      listener = candidateListener;
   }

   public static void setRevealListener(NetheriteChunkTracker.RevealListener revealListener) {
      NetheriteChunkTracker.revealListener = revealListener;
   }

   @Environment(EnvType.CLIENT)
   public record Candidate(int chunkX, int chunkZ, int sectionY) {
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface CandidateListener {
      void onCandidate(NetheriteChunkTracker.Candidate candidate);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface RevealListener {
      void onRevealed(BlockPos blockPos);
   }
}
