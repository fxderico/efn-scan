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
   private static final Long2ObjectOpenHashMap<ChunkLightSnapshot> SNAPSHOTS = new Long2ObjectOpenHashMap();
   private static final LongOpenHashSet ALERTED = new LongOpenHashSet();
   private static final LongOpenHashSet SUSPECT = new LongOpenHashSet();
   static final int SUSPECT_TURNED_DARK = 1;
   static final int SUSPECT_AIRLESS = 2;
   private static final Long2IntOpenHashMap SENT_NOT_DARK = new Long2IntOpenHashMap();
   private static final int MAX_REMEMBERED_CHUNKS = 250000;
   private static String sentPlace;
   private static final Long2ObjectOpenHashMap<ChunkLightSnapshot> HELD = new Long2ObjectOpenHashMap();
   private static final ChunkEntryGuard ENTRY_GUARD = new ChunkEntryGuard();
   private static final ProtectedChunkStore PROTECTED_CHUNKS = ProtectedChunkStore.get();
   private static ClientWorld lastLevel;
   private static PlayerBypassLightTracker.SpottedListener spottedListener;
   private static long visibilityRevision;

   private PlayerBypassLightTracker() {
   }

   public static void onTick() {
      MinecraftClient var0 = MinecraftClient.method_1551();
      syncLevel(var0);
      ClientPlayerEntity var1 = var0.field_1724;
      if (lastLevel != null && var1 != null) {
         ENTRY_GUARD.update(var1.method_31476().method_8324(), PlayerBypassLightTracker::isLoaded, PlayerBypassLightTracker::isFlaggedOrHeld, PROTECTED_CHUNKS);
      }

      PROTECTED_CHUNKS.tickAutosave();
   }

   public static void onPacketApplied(Packet<?> var0) {
      LightData var1;
      int var2;
      int var3;
      if (var0 instanceof ChunkDataS2CPacket var4) {
         var3 = var4.method_11523();
         var2 = var4.method_11524();
         var1 = var4.method_38599();
      } else {
         if (!(var0 instanceof LightUpdateS2CPacket var6)) {
            return;
         }

         var3 = var6.method_11558();
         var2 = var6.method_11554();
         var1 = var6.method_38600();
      }

      MinecraftClient var7 = MinecraftClient.method_1551();
      ClientWorld var5 = var7.field_1687;
      syncLevel(var7);
      if (var5 != null && var1 != null) {
         if (var7.field_1724 != null && var7.field_1724.method_23318() < 0.0) {
            hold(var3, var2, var1, var5.method_32890(), var5.method_32891());
         } else {
            ingest(var3, var2, var1, var5.method_32890(), var5.method_32891());
         }

         rememberNotDark(var3, var2, var1, var5.method_32890(), var5.method_32891());
         visibilityRevision++;
      }
   }

   private static void rememberNotDark(int var0, int var1, LightData var2, int var3, int var4) {
      int var5 = LightDataClassifier.sentNotDark(var2, var3, var4);
      if (var5 != 0) {
         if (SENT_NOT_DARK.size() >= 250000) {
            SENT_NOT_DARK.clear();
         }

         long var6 = ChunkPos.method_8331(var0, var1);
         SENT_NOT_DARK.put(var6, SENT_NOT_DARK.get(var6) | var5);
      }
   }

   private static void syncLevel(MinecraftClient var0) {
      ClientWorld var2 = var0.field_1687;
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
            String var3 = worldKey(var0);
            String var1;
            String var4 = var3 + "\n" + (var1 = var2.method_27983().method_29177().toString());
            if (!var4.equals(sentPlace)) {
               SENT_NOT_DARK.clear();
               sentPlace = var4;
            }

            PROTECTED_CHUNKS.select(var3, var1);
         }
      }
   }

   private static String worldKey(MinecraftClient var0) {
      IntegratedServer var1 = var0.method_1576();
      if (var1 != null) {
         Path var3 = var1.method_27050(WorldSavePath.field_24188).normalize().getFileName();
         return ProtectedChunkStore.singleplayerKey(var3 == null ? "world" : var3.toString());
      } else {
         ServerInfo var2 = var0.method_1558();
         return ProtectedChunkStore.serverKey(var2 == null ? null : var2.field_3761);
      }
   }

   private static void ingest(int var0, int var1, LightData var2, int var3, int var4) {
      long var8 = ChunkPos.method_8331(var0, var1);
      if (HELD.containsKey(var8)) {
         hold(var0, var1, var2, var3, var4);
      }

      ChunkLightSnapshot var6;
      boolean var7 = (var6 = (ChunkLightSnapshot)SNAPSHOTS.get(var8)) != null && isFlagged(var8, var6);
      if (var6 == null || var6.sectionCount != var3 || var6.bottomSectionY != var4) {
         var6 = new ChunkLightSnapshot(var3, var4);
         SNAPSHOTS.put(var8, var6);
      }

      try {
         LightDataClassifier.apply(var2, var6);
      } catch (Throwable var13) {
         LOGGER.error("Player Bypass light ingest failed for chunk {}, {}", new Object[]{var0, var1, var13});
         return;
      }

      boolean var11 = var6.hasBaseSignal();
      if (var11 && !PROTECTED_CHUNKS.flagAllowed(var8, var7)) {
         SNAPSHOTS.remove(var8);
      } else {
         int var5 = var11 ? suspicion(var8, var0, var1, var6) : 0;
         if (var11 && var5 != 0) {
            SUSPECT.add(var8);
         } else if (var11) {
            SUSPECT.remove(var8);
            if (spottedListener != null && ALERTED.add(var8)) {
               spottedListener.onChunkSpotted(var0, var1);
            }
         } else if (!ALERTED.contains(var8)) {
            SNAPSHOTS.remove(var8);
            SUSPECT.remove(var8);
         }
      }
   }

   private static int suspicion(long var0, int var2, int var3, ChunkLightSnapshot var4) {
      int var5 = SENT_NOT_DARK.get(var0);
      byte var6 = 0;

      for (int var7 = 1; var7 < var4.sectionCount + 1; var7++) {
         if (ChunkLightSnapshot.candidate(var7, var4.bottomSectionY) && var4.sky[var7] == LightSectionStatus.ZEROED_PRESENT) {
            if (var7 < 32 && (var5 >>> var7 & 1) != 0) {
               var6 |= 1;
            } else {
               if (lastLevel == null || !DarkSectionCheck.airlessAround(lastLevel, var2, var4.bottomSectionY + var7 - 1, var3)) {
                  return 0;
               }

               var6 |= 2;
            }
         }
      }

      return var6;
   }

   private static void hold(int var0, int var1, LightData var2, int var3, int var4) {
      long var5 = ChunkPos.method_8331(var0, var1);
      ChunkLightSnapshot var7 = (ChunkLightSnapshot)HELD.get(var5);
      if (var7 == null || var7.sectionCount != var3 || var7.bottomSectionY != var4) {
         var7 = new ChunkLightSnapshot(var3, var4);
      }

      try {
         LightDataClassifier.apply(var2, var7);
      } catch (Throwable var9) {
         LOGGER.error("Player Bypass light hold failed for chunk {}, {}", new Object[]{var0, var1, var9});
         return;
      }

      if (var7.hasBaseSignal() && PROTECTED_CHUNKS.flagAllowed(var5, false)) {
         HELD.put(var5, var7);
      } else {
         HELD.remove(var5);
      }
   }

   public static void setSpottedListener(PlayerBypassLightTracker.SpottedListener var0) {
      spottedListener = var0;
   }

   public static Long2ObjectMap<ChunkLightSnapshot> snapshots() {
      return SNAPSHOTS;
   }

   public static long visibilityRevision() {
      return visibilityRevision;
   }

   public static boolean isSuspect(long var0) {
      return SUSPECT.contains(var0);
   }

   public static boolean isFlagged(long var0, ChunkLightSnapshot var2) {
      return ALERTED.contains(var0) || var2.hasBaseSignal();
   }

   public static boolean isFlagged(long var0) {
      ChunkLightSnapshot var2 = (ChunkLightSnapshot)SNAPSHOTS.get(var0);
      return var2 != null && isFlagged(var0, var2);
   }

   private static boolean isFlaggedOrHeld(long var0) {
      return isFlagged(var0) || HELD.containsKey(var0);
   }

   private static boolean isLoaded(long var0) {
      return lastLevel != null && lastLevel.method_2935().method_12123(ChunkPos.method_8325(var0), ChunkPos.method_8332(var0));
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface SpottedListener {
      void onChunkSpotted(int var1, int var2);
   }
}
