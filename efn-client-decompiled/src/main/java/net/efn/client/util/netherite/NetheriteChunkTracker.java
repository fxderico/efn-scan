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
   private static final int ANCIENT_STATE_ID = Block.method_9507(Blocks.field_22109.method_9564());
   private static final RegistryKey<World> DONUT_NETHER = RegistryKey.method_29179(RegistryKeys.field_41223, Identifier.method_60655("worlds", "smp_nether"));
   private static final Set<NetheriteChunkTracker.Candidate> CANDIDATES = new LinkedHashSet<>();
   private static WeakReference<ClientWorld> lastLevel = new WeakReference<>(null);
   private static RegistryKey<World> lastDimension;
   private static NetheriteChunkTracker.CandidateListener listener;
   private static NetheriteChunkTracker.RevealListener revealListener;

   private NetheriteChunkTracker() {
   }

   public static void onPacketApplied(Packet<?> var0) {
      ClientWorld var1 = MinecraftClient.method_1551().field_1687;
      if (var1 != null && isNether(var1)) {
         if (var0 instanceof ChunkDataS2CPacket var2) {
            trackLevel(var1);

            for (int var4 : HiddenDebrisScanner.findHiddenSections(var2.method_38598(), var1.method_32890(), var1.method_32891(), ANCIENT_STATE_ID)) {
               add(new NetheriteChunkTracker.Candidate(var2.method_11523(), var2.method_11524(), var4));
            }
         } else if (var0 instanceof BlockUpdateS2CPacket var5) {
            onBlockUpdate(var5.method_11309(), var5.method_11308());
         } else if (var0 instanceof ChunkDeltaUpdateS2CPacket var6) {
            var6.method_30621(NetheriteChunkTracker::onBlockUpdate);
         }
      }
   }

   private static void onBlockUpdate(BlockPos var0, BlockState var1) {
      if (Block.method_9507(var1) == ANCIENT_STATE_ID) {
         if (revealListener != null) {
            revealListener.onRevealed(var0.method_10062());
         }

         CANDIDATES.remove(new NetheriteChunkTracker.Candidate(var0.method_10263() >> 4, var0.method_10260() >> 4, var0.method_10264() >> 4));
      }
   }

   private static void add(NetheriteChunkTracker.Candidate var0) {
      if (!CANDIDATES.contains(var0)) {
         if (CANDIDATES.size() >= 512) {
            Iterator var1 = CANDIDATES.iterator();
            var1.next();
            var1.remove();
         }

         CANDIDATES.add(var0);
         if (listener != null) {
            listener.onCandidate(var0);
         }
      }
   }

   private static void trackLevel(ClientWorld var0) {
      RegistryKey var1 = var0.method_27983();
      if (var0 != lastLevel.get() || !var1.equals(lastDimension)) {
         CANDIDATES.clear();
         lastLevel = new WeakReference<>(var0);
         lastDimension = var1;
      }
   }

   public static boolean isNether(ClientWorld var0) {
      RegistryKey var1 = var0.method_27983();
      return var1 == World.field_25180 || DONUT_NETHER.equals(var1) || var0.method_40134().method_40225(DimensionTypes.field_37667);
   }

   public static List<NetheriteChunkTracker.Candidate> snapshot() {
      return MinecraftClient.method_1551().field_1687 != lastLevel.get() ? List.of() : List.copyOf(CANDIDATES);
   }

   public static void setListener(NetheriteChunkTracker.CandidateListener var0) {
      listener = var0;
   }

   public static void setRevealListener(NetheriteChunkTracker.RevealListener var0) {
      revealListener = var0;
   }

   @Environment(EnvType.CLIENT)
   public record Candidate(int chunkX, int chunkZ, int sectionY) {
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface CandidateListener {
      void onCandidate(NetheriteChunkTracker.Candidate var1);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface RevealListener {
      void onRevealed(BlockPos var1);
   }
}
