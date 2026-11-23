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
   private final Long2ObjectOpenHashMap<BlockPos> bypassMarkers = new Long2ObjectOpenHashMap();
   private final Long2ObjectOpenHashMap<long[]> chunkHits = new Long2ObjectOpenHashMap();
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
   public void onTick(TickEvent var1) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      ClientWorld var5 = var4.field_1687;
      if (var5 != null && var4.field_1724 != null) {
         boolean var6 = this.method.is("Bypass");
         if (this.queue.isEmpty()) {
            if (++this.tick < 20) {
               return;
            }

            this.tick = 0;
            ChunkPos var7 = var4.field_1724.method_31476();
            int var8 = (Integer)var4.field_1690.method_42503().method_41753();
            if (var6) {
               this.bypassScanned
                  .removeIf(var3 -> !inRange(var3, var7, var8) || !LevelUtil.isLoaded(var5, ChunkPos.method_8325(var3), ChunkPos.method_8332(var3)));
               this.bypassMarkers.keySet().removeIf(var1x -> !this.bypassScanned.contains(var1x));

               for (int var9 = var7.field_9181 - var8; var9 <= var7.field_9181 + var8; var9++) {
                  for (int var10 = var7.field_9180 - var8; var10 <= var7.field_9180 + var8; var10++) {
                     long var11 = ChunkPos.method_8331(var9, var10);
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
                  | this.chunkHits
                     .keySet()
                     .removeIf(var3 -> !inRange(var3, var7, var8) || !LevelUtil.isLoaded(var5, ChunkPos.method_8325(var3), ChunkPos.method_8332(var3)));
               this.lightScanned.removeIf(var2x -> !inRange(var2x, var7, var8));

               for (int var21 = var7.field_9181 - var8; var21 <= var7.field_9181 + var8; var21++) {
                  for (int var22 = var7.field_9180 - var8; var22 <= var7.field_9180 + var8; var22++) {
                     if (LevelUtil.isLoaded(var5, var21, var22) && !this.lightScanned.contains(ChunkPos.method_8331(var21, var22))) {
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
            if (LevelUtil.isLoaded(var5, var12.field_9181, var12.field_9180)) {
               WorldChunk var13 = var5.method_8497(var12.field_9181, var12.field_9180);
               if (var6) {
                  collectPaletteLeak(var13, this.bypassMarkers);
                  this.bypassScanned.add(var12.method_8324());
               } else {
                  this.chunkFound.clear();
                  if (this.method.is("ANBS+ Scan")) {
                     this.collectGlow(var5, var13, this.chunkFound);
                  } else {
                     AmethystLight.scanSources(var5, var13, (var1x, var3) -> this.chunkFound.add(var1x));
                  }

                  long var14 = var12.method_8324();
                  long[] var16 = (long[])this.chunkHits.get(var14);
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

   private void collectGlow(ClientWorld var1, WorldChunk var2, LongOpenHashSet var3) {
      this.glowCells.clear();
      if (this.glow.scan(var1, var2, var1x -> this.glowCells.add(var1x)) >= this.minCells.getInt()) {
         int var4 = Math.min(this.glowCells.size(), 200);

         for (int var5 = 0; var5 < var4; var5++) {
            var3.add(this.glowCells.getLong(var5));
         }
      }
   }

   private static boolean inRange(long var0, ChunkPos var2, int var3) {
      int var4 = ChunkPos.method_8325(var0);
      int var5 = ChunkPos.method_8332(var0);
      return Math.abs(var4 - var2.field_9181) <= var3 && Math.abs(var5 - var2.field_9180) <= var3;
   }

   private static long[] markersToLongArray(Long2ObjectOpenHashMap<BlockPos> var0) {
      long[] var1 = new long[var0.size()];
      int var2 = 0;
      ObjectIterator var3 = var0.values().iterator();

      while (var3.hasNext()) {
         BlockPos var4 = (BlockPos)var3.next();
         var1[var2++] = var4.method_10063();
      }

      return var1;
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent var1) {
      if (!this.method.is("Bypass")) {
         if (var1.packet instanceof ChunkDataS2CPacket var12) {
            this.dirtyAround(var12.method_11523(), var12.method_11524());
         } else if (var1.packet instanceof LightUpdateS2CPacket var13) {
            this.dirtyAround(var13.method_11558(), var13.method_11554());
         } else if (var1.packet instanceof BlockUpdateS2CPacket var14) {
            this.dirtyAround(var14.method_11309().method_10263() >> 4, var14.method_11309().method_10260() >> 4);
         } else if (var1.packet instanceof ChunkDeltaUpdateS2CPacket var15) {
            var15.method_30621((var1x, var2) -> this.dirtyAround(var1x.method_10263() >> 4, var1x.method_10260() >> 4));
         }
      } else {
         if (var1.packet instanceof ChunkDataS2CPacket var3) {
            this.dirtyBypassChunk(var3.method_11523(), var3.method_11524());
         } else if (var1.packet instanceof BlockUpdateS2CPacket var10) {
            this.dirtyBypassChunk(var10.method_11309().method_10263() >> 4, var10.method_11309().method_10260() >> 4);
         } else if (var1.packet instanceof ChunkDeltaUpdateS2CPacket var11) {
            var11.method_30621((var1x, var2) -> this.dirtyBypassChunk(var1x.method_10263() >> 4, var1x.method_10260() >> 4));
         }
      }
   }

   private void dirtyAround(int var1, int var2) {
      for (int var3 = -1; var3 <= 1; var3++) {
         for (int var4 = -1; var4 <= 1; var4++) {
            this.lightScanned.remove(ChunkPos.method_8331(var1 + var3, var2 + var4));
         }
      }
   }

   private void dirtyBypassChunk(int var1, int var2) {
      long var3 = ChunkPos.method_8331(var1, var2);
      if (this.bypassScanned.remove(var3)) {
         this.bypassMarkers.remove(var3);
      }
   }

   private static void collectPaletteLeak(WorldChunk var0, Long2ObjectOpenHashMap<BlockPos> var1) {
      ChunkSection[] var2 = var0.method_12006();
      int var3 = var0.method_32891();
      ChunkPos var4 = var0.method_12004();

      for (int var5 = 0; var5 < var2.length; var5++) {
         ChunkSection var7 = var2[var5];
         PalettedContainer var6;
         if (var7 != null && !(var6 = var7.method_12265()).method_19526(var0x -> false) && var6.method_19526(AmethystBypassModule::isBud)) {
            int var8 = (var3 + var5) * 16;
            BlockPos var9 = pickMarker(var0, var7, var8, var4);
            var1.put(var4.method_8324(), var9 != null ? var9 : new BlockPos(var4.method_8326() + 8, var8 + 8, var4.method_8328() + 8));
            return;
         }
      }
   }

   private static BlockPos pickMarker(WorldChunk var0, ChunkSection var1, int var2, ChunkPos var3) {
      Mutable var4 = new Mutable();
      BlockPos var5 = null;
      int var6 = 2147483647;

      for (int var7 = 0; var7 < 16; var7++) {
         int var8 = var2 + var7;

         for (int var9 = 0; var9 < 16; var9++) {
            for (int var10 = 0; var10 < 16; var10++) {
               if (var1.method_12254(var10, var7, var9).method_26215()) {
                  int var12 = var3.method_8326() + var10;
                  int var13 = var3.method_8328() + var9;
                  boolean var14 = false;

                  for (Direction var18 : Direction.values()) {
                     int var19 = var12 + var18.method_10148();
                     int var20 = var13 + var18.method_10165();
                     if (var19 >> 4 == var3.field_9181 && var20 >> 4 == var3.field_9180) {
                        var4.method_10103(var19, var8 + var18.method_10164(), var20);
                        Block var21 = var0.method_8320(var4).method_26204();
                        if (var21 == Blocks.field_27159 || var21 == Blocks.field_27160) {
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

   static int scatter(int var0, int var1, int var2) {
      long var3 = var0 * -7046029254386353131L ^ var1 * -4417276706812531889L ^ var2 * 1609587929392839161L;
      var3 ^= var3 >>> 29;
      var3 *= -4658895280553007687L;
      var3 ^= var3 >>> 32;
      return Math.floorMod(var3, 100);
   }

   private static boolean isBud(BlockState var0) {
      Block var1 = var0.method_26204();
      return var1 == Blocks.field_27164 || var1 == Blocks.field_27163 || var1 == Blocks.field_27162;
   }

   private static boolean isGeode(BlockState var0) {
      return var0.method_27852(Blocks.field_27159) || var0.method_27852(Blocks.field_27160);
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      long[] var3 = this.hits;
      if (var2 != null && var3.length != 0) {
         DrawStyle var4 = DrawStyle.method_75537(-3636481, 1.5F, -3636481);
         boolean var5 = this.tracer.get();

         for (long var9 : var3) {
            BlockPos var11 = BlockPos.method_10092(var9);
            if (LevelUtil.isLoaded(var2, var11.method_10263() >> 4, var11.method_10260() >> 4)) {
               Box var12 = new Box(var11);
               if (var1.frustum == null || var1.frustum.method_23093(var12)) {
                  GizmoDrawing.method_75541(var12, var4).method_75533();
               }

               if (var5) {
                  TracerEngine.draw(Vec3d.method_24953(var11), -3636481);
               }
            }
         }
      }
   }
}
