package net.efn.client.module.impl.misc;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayDeque;
import java.util.Arrays;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.AmethystLight;
import net.efn.client.util.GeodeGlowScan;
import net.efn.client.util.LevelUtil;
import net.efn.client.util.PerformanceBudget;
import net.efn.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public final class AmethystBypassModule extends ClientModule {
   private static final int COLOR = -3636481;
   private static final float LINE_WIDTH = 1.5F;
   private static final int RESCAN_TICKS = 20;
   private static final long BUDGET_NANOS = 2000000L;
   private static final String ANBS_SCAN = "ANBS+ Scan";
   private static final int MAX_GLOW_MARKS = 200;
   private static final int FULL_REFRESH_TICKS = 600;
   private final ModeSetting method = this.addSetting("method", new ModeSetting("Method", "Bypass", "Bypass", "Light", "ANBS+ Scan"));
   private final NumberSetting minCells = this.addSetting("min-cells", new NumberSetting("Min Cells", 12.0F, 1.0F, 100.0F).step(1.0F));
   private final BooleanSetting tracer = this.addSetting("tracer", new BooleanSetting("Tracer", false));
   private final ArrayDeque<ChunkPos> queue = new ArrayDeque<>();
   private final LongOpenHashSet found = new LongOpenHashSet();
   private final LongOpenHashSet bypassScanned = new LongOpenHashSet();
   private final Long2ObjectOpenHashMap<BlockPos> bypassMarkers = new Long2ObjectOpenHashMap<>();
   private final Long2ObjectOpenHashMap<long[]> chunkHits = new Long2ObjectOpenHashMap<>();
   private final LongOpenHashSet lightScanned = new LongOpenHashSet();
   private final LongOpenHashSet chunkFound = new LongOpenHashSet();
   private boolean hitsChanged;
   private int sinceRefresh;
   private final GeodeGlowScan glow = new GeodeGlowScan();
   private final LongArrayList glowCells = new LongArrayList();
   private long[] hits = new long[0];
   private int tick;

   public AmethystBypassModule() {
      super("amethyst-bypass", "Amethyst Bypass", "Shows amethyst geodes hidden by anti-xray.", ModuleCategory.BASE_RAID);
      this.method.onChange(this::restart);
      this.minCells.visibleWhen(() -> this.method.is("ANBS+ Scan"));
   }

   @Override
   protected void onEnable() {
      this.restart();
   }

   @Override
   protected void onDisable() {
      this.restart();
   }

   private void restart() {
      this.queue.clear();
      this.found.clear();
      this.bypassScanned.clear();
      this.bypassMarkers.clear();
      this.chunkHits.clear();
      this.lightScanned.clear();
      this.hitsChanged = false;
      this.sinceRefresh = 0;
      this.hits = new long[0];
      this.tick = 0;
   }

   @EventHandler(
      stage = Stage.POST
   )
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      ClientWorld var5 = var4.world;
      if (var5 != null && var4.player != null) {
         boolean var6 = this.method.is("Bypass");
         if (this.queue.isEmpty()) {
            if (++this.tick < 20) {
               return;
            }

            this.tick = 0;
            ChunkPos var7 = var4.player.getChunkPos();
            int var8 = var4.options.getViewDistance().getValue();
            if (var6) {
               this.bypassScanned.removeIf(l -> !inRange(l, var7, var8) || !LevelUtil.isLoaded(var5, ChunkPos.getPackedX(l), ChunkPos.getPackedZ(l)));
               this.bypassMarkers.keySet().removeIf(l -> !this.bypassScanned.contains(l));

               for (int var9 = var7.x - var8; var9 <= var7.x + var8; var9++) {
                  for (int var10 = var7.z - var8; var10 <= var7.z + var8; var10++) {
                     long var11 = ChunkPos.toLong(var9, var10);
                     if (LevelUtil.isLoaded(var5, var9, var10) && !this.bypassScanned.contains(var11)) {
                        this.queue.add(new ChunkPos(var9, var10));
                     }
                  }
               }
            } else {
               if (++this.sinceRefresh * 20 >= 600) {
                  this.sinceRefresh = 0;
                  this.lightScanned.clear();
               }

               this.hitsChanged = this.hitsChanged
                  | this.chunkHits.keySet().removeIf(l -> !inRange(l, var7, var8) || !LevelUtil.isLoaded(var5, ChunkPos.getPackedX(l), ChunkPos.getPackedZ(l)));
               this.lightScanned.removeIf(l -> !inRange(l, var7, var8));

               for (int var21 = var7.x - var8; var21 <= var7.x + var8; var21++) {
                  for (int var22 = var7.z - var8; var22 <= var7.z + var8; var22++) {
                     if (LevelUtil.isLoaded(var5, var21, var22) && !this.lightScanned.contains(ChunkPos.toLong(var21, var22))) {
                        this.queue.add(new ChunkPos(var21, var22));
                     }
                  }
               }
            }
         }

         boolean var19 = PerformanceModule.optimizationsEnabled();
         long var20 = System.nanoTime();
         long var2 = var19 ? PerformanceBudget.acquire(2000000L) : var20 + 2000000L;

         while (!this.queue.isEmpty() && System.nanoTime() < var2) {
            ChunkPos var12 = this.queue.poll();
            if (LevelUtil.isLoaded(var5, var12.x, var12.z)) {
               WorldChunk var13 = var5.getChunk(var12.x, var12.z);
               if (var6) {
                  collectPaletteLeak(var13, this.bypassMarkers);
                  this.bypassScanned.add(var12.toLong());
               } else {
                  this.chunkFound.clear();
                  if (this.method.is("ANBS+ Scan")) {
                     this.collectGlow(var5, var13, this.chunkFound);
                  } else {
                     AmethystLight.scanSources(var5, var13, (l, i) -> this.chunkFound.add(l));
                  }

                  long var14 = var12.toLong();
                  long[] var16 = this.chunkHits.get(var14);
                  long[] var17 = this.chunkFound.toLongArray();
                  if (var16 != null && var17.length < var16.length) {
                     this.lightScanned.add(var14);
                  } else {
                     if (var17.length == 0) {
                        if (this.chunkHits.remove(var14) != null) {
                           this.hitsChanged = true;
                        }
                     } else if (var16 == null || !Arrays.equals(var16, var17)) {
                        this.chunkHits.put(var14, var17);
                        this.hitsChanged = true;
                     }

                     this.lightScanned.add(var14);
                  }
               }
            }
         }

         if (var19) {
            PerformanceBudget.record(var20);
         }

         if (var6) {
            if (this.queue.isEmpty()) {
               this.hits = markersToLongArray(this.bypassMarkers);
            }
         } else if (this.hitsChanged) {
            this.hitsChanged = false;
            this.found.clear();
            ObjectIterator var23 = this.chunkHits.values().iterator();

            while (var23.hasNext()) {
               for (long var26 : (long[])var23.next()) {
                  this.found.add(var26);
               }
            }

            this.hits = this.found.toLongArray();
         }
      }
   }

   private void collectGlow(ClientWorld clientWorld, WorldChunk worldChunk, LongOpenHashSet longOpenHashSet) {
      this.glowCells.clear();
      if (this.glow.scan(clientWorld, worldChunk, l -> this.glowCells.add(l)) >= this.minCells.getInt()) {
         int var4 = Math.min(this.glowCells.size(), 200);

         for (int var5 = 0; var5 < var4; var5++) {
            longOpenHashSet.add(this.glowCells.getLong(var5));
         }
      }
   }

   private static boolean inRange(long l, ChunkPos chunkPos, int i) {
      int var4 = ChunkPos.getPackedX(l);
      int var5 = ChunkPos.getPackedZ(l);
      return Math.abs(var4 - chunkPos.x) <= i && Math.abs(var5 - chunkPos.z) <= i;
   }

   private static long[] markersToLongArray(Long2ObjectOpenHashMap<BlockPos> long2ObjectOpenHashMap) {
      long[] var1 = new long[long2ObjectOpenHashMap.size()];
      int var2 = 0;

      for (BlockPos var4 : long2ObjectOpenHashMap.values()) {
         var1[var2++] = var4.asLong();
      }

      return var1;
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent packetAppliedEvent) {
      if (!this.method.is("Bypass")) {
         if (packetAppliedEvent.packet instanceof ChunkDataS2CPacket var12) {
            this.dirtyAround(var12.getChunkX(), var12.getChunkZ());
         } else if (packetAppliedEvent.packet instanceof LightUpdateS2CPacket var13) {
            this.dirtyAround(var13.getChunkX(), var13.getChunkZ());
         } else if (packetAppliedEvent.packet instanceof BlockUpdateS2CPacket var14) {
            this.dirtyAround(var14.getPos().getX() >> 4, var14.getPos().getZ() >> 4);
         } else if (packetAppliedEvent.packet instanceof ChunkDeltaUpdateS2CPacket var15) {
            var15.visitUpdates((blockPos, blockState) -> this.dirtyAround(blockPos.getX() >> 4, blockPos.getZ() >> 4));
         }
      } else {
         if (packetAppliedEvent.packet instanceof ChunkDataS2CPacket var3) {
            this.dirtyBypassChunk(var3.getChunkX(), var3.getChunkZ());
         } else if (packetAppliedEvent.packet instanceof BlockUpdateS2CPacket var10) {
            this.dirtyBypassChunk(var10.getPos().getX() >> 4, var10.getPos().getZ() >> 4);
         } else if (packetAppliedEvent.packet instanceof ChunkDeltaUpdateS2CPacket var11) {
            var11.visitUpdates((blockPos, blockState) -> this.dirtyBypassChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4));
         }
      }
   }

   private void dirtyAround(int i, int j) {
      for (int var3 = -1; var3 <= 1; var3++) {
         for (int var4 = -1; var4 <= 1; var4++) {
            this.lightScanned.remove(ChunkPos.toLong(i + var3, j + var4));
         }
      }
   }

   private void dirtyBypassChunk(int i, int j) {
      long var3 = ChunkPos.toLong(i, j);
      if (this.bypassScanned.remove(var3)) {
         this.bypassMarkers.remove(var3);
      }
   }

   private static void collectPaletteLeak(WorldChunk worldChunk, Long2ObjectOpenHashMap<BlockPos> long2ObjectOpenHashMap) {
      ChunkSection[] var2 = worldChunk.getSectionArray();
      int var3 = worldChunk.getBottomSectionCoord();
      ChunkPos var4 = worldChunk.getPos();

      for (int var5 = 0; var5 < var2.length; var5++) {
         ChunkSection var7 = var2[var5];
         PalettedContainer var6;
         if (var7 != null && !(var6 = var7.getBlockStateContainer()).hasAny(blockState -> false) && var6.hasAny(AmethystBypassModule::isBud)) {
            int var8 = (var3 + var5) * 16;
            BlockPos var9 = pickMarker(worldChunk, var7, var8, var4);
            long2ObjectOpenHashMap.put(var4.toLong(), var9 != null ? var9 : new BlockPos(var4.getStartX() + 8, var8 + 8, var4.getStartZ() + 8));
            return;
         }
      }
   }

   private static BlockPos pickMarker(WorldChunk worldChunk, ChunkSection chunkSection, int i, ChunkPos chunkPos) {
      Mutable var4 = new Mutable();
      BlockPos var5 = null;
      int var6 = 2147483647;

      for (int var7 = 0; var7 < 16; var7++) {
         int var8 = i + var7;

         for (int var9 = 0; var9 < 16; var9++) {
            for (int var10 = 0; var10 < 16; var10++) {
               if (chunkSection.getBlockState(var10, var7, var9).isAir()) {
                  int var12 = chunkPos.getStartX() + var10;
                  int var13 = chunkPos.getStartZ() + var9;
                  boolean var14 = false;

                  for (Direction var18 : Direction.values()) {
                     int var19 = var12 + var18.getOffsetX();
                     int var20 = var13 + var18.getOffsetZ();
                     if (var19 >> 4 == chunkPos.x && var20 >> 4 == chunkPos.z) {
                        var4.set(var19, var8 + var18.getOffsetY(), var20);
                        Block var21 = worldChunk.getBlockState(var4).getBlock();
                        if (var21 == Blocks.AMETHYST_BLOCK || var21 == Blocks.BUDDING_AMETHYST) {
                           var14 = true;
                           break;
                        }
                     }
                  }

                  int var11;
                  if (var14 && (var11 = scatter(var12, var8, var13)) < var6) {
                     var6 = var11;
                     var5 = new BlockPos(var12, var8, var13);
                  }
               }
            }
         }
      }

      return var5;
   }

   static int scatter(int i, int j, int k) {
      long var3 = i * -7046029254386353131L ^ j * -4417276706812531889L ^ k * 1609587929392839161L;
      var3 ^= var3 >>> 29;
      var3 *= -4658895280553007687L;
      var3 ^= var3 >>> 32;
      return Math.floorMod(var3, 100);
   }

   private static boolean isBud(BlockState blockState) {
      Block var1 = blockState.getBlock();
      return var1 == Blocks.SMALL_AMETHYST_BUD || var1 == Blocks.MEDIUM_AMETHYST_BUD || var1 == Blocks.LARGE_AMETHYST_BUD;
   }

   private static boolean isGeode(BlockState blockState) {
      return blockState.isOf(Blocks.AMETHYST_BLOCK) || blockState.isOf(Blocks.BUDDING_AMETHYST);
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      ClientWorld var2 = MinecraftClient.getInstance().world;
      long[] var3 = this.hits;
      if (var2 != null && var3.length != 0) {
         DrawStyle var4 = DrawStyle.filledAndStroked(-3636481, 1.5F, -3636481);
         boolean var5 = this.tracer.get();

         for (long var9 : var3) {
            BlockPos var11 = BlockPos.fromLong(var9);
            if (LevelUtil.isLoaded(var2, var11.getX() >> 4, var11.getZ() >> 4)) {
               Box var12 = new Box(var11);
               if (render3DEvent.frustum == null || render3DEvent.frustum.isVisible(var12)) {
                  GizmoDrawing.box(var12, var4).ignoreOcclusion();
               }

               if (var5) {
                  TracerEngine.draw(Vec3d.ofCenter(var11), -3636481);
               }
            }
         }
      }
   }
}
