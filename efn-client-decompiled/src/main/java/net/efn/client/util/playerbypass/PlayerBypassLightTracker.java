package net.efn.client.util.playerbypass;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class PlayerBypassLightTracker {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final Long2ObjectOpenHashMap<ChunkLightSnapshot> SNAPSHOTS = new Long2ObjectOpenHashMap<>();
   private static final LongOpenHashSet ALERTED = new LongOpenHashSet();
   private static final LongOpenHashSet SUSPECT = new LongOpenHashSet();
   static final int SUSPECT_TURNED_DARK = 1;
   static final int SUSPECT_AIRLESS = 2;
   private static final Long2IntOpenHashMap SENT_NOT_DARK = new Long2IntOpenHashMap();
   private static final int MAX_REMEMBERED_CHUNKS = 250000;
   private static String sentPlace;
   private static final Long2ObjectOpenHashMap<ChunkLightSnapshot> HELD = new Long2ObjectOpenHashMap<>();
   private static final ChunkEntryGuard ENTRY_GUARD = new ChunkEntryGuard();
   private static final ProtectedChunkStore PROTECTED_CHUNKS = ProtectedChunkStore.get();
   private static ClientWorld lastLevel;
   private static PlayerBypassLightTracker.SpottedListener spottedListener;
   private static long visibilityRevision;

   private PlayerBypassLightTracker() {
   }

   public static void onTick() {
      MinecraftClient var0 = MinecraftClient.getInstance();
      syncLevel(var0);
      ClientPlayerEntity var1 = var0.player;
      if (lastLevel != null && var1 != null) {
         ENTRY_GUARD.update(var1.getChunkPos().toLong(), PlayerBypassLightTracker::isLoaded, PlayerBypassLightTracker::isFlaggedOrHeld, PROTECTED_CHUNKS);
      }

      PROTECTED_CHUNKS.tickAutosave();
   }

   public static void onPacketApplied(Packet<?> packet) {
      LightData var1;
      int var2;
      int var3;
      if (packet instanceof ChunkDataS2CPacket var4) {
         var3 = var4.getChunkX();
         var2 = var4.getChunkZ();
         var1 = var4.getLightData();
      } else {
         if (!(packet instanceof LightUpdateS2CPacket var6)) {
            return;
         }

         var3 = var6.getChunkX();
         var2 = var6.getChunkZ();
         var1 = var6.getData();
      }

      MinecraftClient var7 = MinecraftClient.getInstance();
      ClientWorld var5 = var7.world;
      syncLevel(var7);
      if (var5 != null && var1 != null) {
         if (var7.player != null && var7.player.getY() < 0.0) {
            hold(var3, var2, var1, var5.countVerticalSections(), var5.getBottomSectionCoord());
         } else {
            ingest(var3, var2, var1, var5.countVerticalSections(), var5.getBottomSectionCoord());
         }

         rememberNotDark(var3, var2, var1, var5.countVerticalSections(), var5.getBottomSectionCoord());
         visibilityRevision++;
      }
   }

   private static void rememberNotDark(int i, int j, LightData lightData, int k, int l) {
      int var5 = LightDataClassifier.sentNotDark(lightData, k, l);
      if (var5 != 0) {
         if (SENT_NOT_DARK.size() >= 250000) {
            SENT_NOT_DARK.clear();
         }

         long var6 = ChunkPos.toLong(i, j);
         SENT_NOT_DARK.put(var6, SENT_NOT_DARK.get(var6) | var5);
      }
   }

   private static void syncLevel(MinecraftClient minecraftClient) {
      ClientWorld var2 = minecraftClient.world;
      if (var2 != lastLevel) {
         visibilityRevision++;
         SNAPSHOTS.clear();
         ALERTED.clear();
         SUSPECT.clear();
         HELD.clear();
         ENTRY_GUARD.reset();
         lastLevel = var2;
         PROTECTED_CHUNKS.flush();
         if (var2 == null) {
            PROTECTED_CHUNKS.select(null, null);
         } else {
            String var3 = worldKey(minecraftClient);
            String var1;
            String var4 = var3 + "\n" + (var1 = var2.getRegistryKey().getValue().toString());
            if (!var4.equals(sentPlace)) {
               SENT_NOT_DARK.clear();
               sentPlace = var4;
            }

            PROTECTED_CHUNKS.select(var3, var1);
         }
      }
   }

   private static String worldKey(MinecraftClient minecraftClient) {
      IntegratedServer var1 = minecraftClient.getServer();
      if (var1 != null) {
         Path var3 = var1.getSavePath(WorldSavePath.ROOT).normalize().getFileName();
         return ProtectedChunkStore.singleplayerKey(var3 == null ? "world" : var3.toString());
      } else {
         ServerInfo var2 = minecraftClient.getCurrentServerEntry();
         return ProtectedChunkStore.serverKey(var2 == null ? null : var2.address);
      }
   }

   private static void ingest(int i, int j, LightData lightData, int k, int l) {
      long var8 = ChunkPos.toLong(i, j);
      if (HELD.containsKey(var8)) {
         hold(i, j, lightData, k, l);
      }

      ChunkLightSnapshot var6;
      boolean var7 = (var6 = SNAPSHOTS.get(var8)) != null && isFlagged(var8, var6);
      if (var6 == null || var6.sectionCount != k || var6.bottomSectionY != l) {
         var6 = new ChunkLightSnapshot(k, l);
         SNAPSHOTS.put(var8, var6);
      }

      try {
         LightDataClassifier.apply(lightData, var6);
      } catch (Throwable var13) {
         LOGGER.error("Player Bypass light ingest failed for chunk {}, {}", i, j, var13);
         return;
      }

      boolean var11 = var6.hasBaseSignal();
      if (var11 && !PROTECTED_CHUNKS.flagAllowed(var8, var7)) {
         SNAPSHOTS.remove(var8);
      } else {
         int var5 = var11 ? suspicion(var8, i, j, var6) : 0;
         if (var11 && var5 != 0) {
            SUSPECT.add(var8);
         } else if (var11) {
            SUSPECT.remove(var8);
            if (spottedListener != null && ALERTED.add(var8)) {
               spottedListener.onChunkSpotted(i, j);
            }
         } else if (!ALERTED.contains(var8)) {
            SNAPSHOTS.remove(var8);
            SUSPECT.remove(var8);
         }
      }
   }

   private static int suspicion(long l, int i, int j, ChunkLightSnapshot chunkLightSnapshot) {
      int var5 = SENT_NOT_DARK.get(l);
      byte var6 = 0;

      for (int var7 = 1; var7 < chunkLightSnapshot.sectionCount + 1; var7++) {
         if (ChunkLightSnapshot.candidate(var7, chunkLightSnapshot.bottomSectionY) && chunkLightSnapshot.sky[var7] == LightSectionStatus.ZEROED_PRESENT) {
            if (var7 < 32 && (var5 >>> var7 & 1) != 0) {
               var6 |= 1;
            } else {
               if (lastLevel == null || !DarkSectionCheck.airlessAround(lastLevel, i, chunkLightSnapshot.bottomSectionY + var7 - 1, j)) {
                  return 0;
               }

               var6 |= 2;
            }
         }
      }

      return var6;
   }

   private static void hold(int i, int j, LightData lightData, int k, int l) {
      long var5 = ChunkPos.toLong(i, j);
      ChunkLightSnapshot var7 = HELD.get(var5);
      if (var7 == null || var7.sectionCount != k || var7.bottomSectionY != l) {
         var7 = new ChunkLightSnapshot(k, l);
      }

      try {
         LightDataClassifier.apply(lightData, var7);
      } catch (Throwable var9) {
         LOGGER.error("Player Bypass light hold failed for chunk {}, {}", i, j, var9);
         return;
      }

      if (var7.hasBaseSignal() && PROTECTED_CHUNKS.flagAllowed(var5, false)) {
         HELD.put(var5, var7);
      } else {
         HELD.remove(var5);
      }
   }

   public static void setSpottedListener(PlayerBypassLightTracker.SpottedListener spottedListener) {
      PlayerBypassLightTracker.spottedListener = spottedListener;
   }

   public static Long2ObjectMap<ChunkLightSnapshot> snapshots() {
      return SNAPSHOTS;
   }

   public static long visibilityRevision() {
      return visibilityRevision;
   }

   public static boolean isSuspect(long l) {
      return SUSPECT.contains(l);
   }

   public static boolean isFlagged(long l, ChunkLightSnapshot chunkLightSnapshot) {
      return ALERTED.contains(l) || chunkLightSnapshot.hasBaseSignal();
   }

   public static boolean isFlagged(long l) {
      ChunkLightSnapshot var2 = SNAPSHOTS.get(l);
      return var2 != null && isFlagged(l, var2);
   }

   private static boolean isFlaggedOrHeld(long l) {
      return isFlagged(l) || HELD.containsKey(l);
   }

   private static boolean isLoaded(long l) {
      return lastLevel != null && lastLevel.getChunkManager().isChunkLoaded(ChunkPos.getPackedX(l), ChunkPos.getPackedZ(l));
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface SpottedListener {
      void onChunkSpotted(int i, int j);
   }
}
