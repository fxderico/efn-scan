package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.ChunkAreas;
import net.efn.client.util.ChunkOverlay;
import net.efn.client.util.GeodeGlowScan;
import net.efn.client.util.HiddenAmethyst;
import net.efn.client.util.LevelUtil;
import net.efn.client.util.PerformanceBudget;
import net.efn.client.util.ServerBlockLight;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BuddingAmethystBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;

@Environment(EnvType.CLIENT)
public final class SusChunkFinderModule extends ClientModule {
   private static final long SCAN_BUDGET_NANOS = 2000000L;
   private static final int MIN_SCANNED_NEIGHBOURS = 3;
   private static final int NO_PACKET_SENSITIVITY = 1;
   private static final int VISIBLE_REFRESH_TICKS = 20;
   private static final int LIGHT_RESCAN_TICKS = 20;
   private static final int FULL_RESCAN_TICKS = 1200;
   private static final int VISIBLE_MIN_TICKS = 5;
   private static final int LIGHT_WAIT_LIMIT_TICKS = 200;
   private static final int PRUNE_TICKS = 200;
   private static final int PRUNE_MARGIN = 4;
   private static final int PACKET_MIN_Y = -16;
   private static final int PACKET_MAX_Y = 30;
   private static final String METHOD_CLASSIC = "Classic";
   private static final String METHOD_ANBS = "ANBS+ Scan";
   private static final int GLOW_CELLS_PER_POINT = 12;
   private static final int GEODE_TOP_Y = 47;
   private static final int MIN_EXPOSED_BUDDING = 10;
   private static final int MIN_EXPOSED_LINING = 160;
   private static final int GEODE_SETTLE_TICKS = 40;
   private static final int GEODE_CELL_SHIFT = 2;
   private static final int TORCH_LIGHT = 14;
   private static final Direction[] SIDES = Direction.values();
   private static final int AMETHYST = 0;
   private static final int KELP = 1;
   private static final int CAVE_VINES = 2;
   private static final int VINES = 3;
   private static final int BAMBOO = 4;
   private static final int COCOA = 5;
   private static final int BEE_NEST = 6;
   private static final int GROWTH_KINDS = 7;
   private static final int GROWN = 1;
   private static final int UNGROWN = -1;
   private static final int NO_SIGN = 0;
   private static final int BAMBOO_GROWTH_LIGHT = 9;
   private static final String DONUT_BRAND = "DonutFolia";
   private static final int STYLE_VOTES = 3;
   private final ModeSetting method = this.addSetting("method", new ModeSetting("Method", "Classic", "Classic", "ANBS+ Scan"));
   private final NumberSetting spreadRadius = this.addSetting("spread-radius", new NumberSetting("Simulation Distance", 5.0F, 2.0F, 16.0F).step(1.0F));
   private final NumberSetting sensitivity = this.addSetting("packet-sensitivity", new NumberSetting("Sensitivity", 6.0F, 1.0F, 20.0F).step(1.0F));
   private final BooleanSetting kelp = this.growthSign("growth-kelp", "Kelp");
   private final BooleanSetting caveVines = this.growthSign("growth-cave-vines", "Cave Vines");
   private final BooleanSetting vines = this.growthSign("growth-vines", "Vines");
   private final BooleanSetting bamboo = this.growthSign("growth-bamboo", "Bamboo");
   private final BooleanSetting cocoa = this.growthSign("growth-cocoa", "Cocoa");
   private final BooleanSetting beeNests = this.growthSign("growth-bee-nests", "Bee Nests");
   private final BooleanSetting[] growthSigns = new BooleanSetting[]{null, this.kelp, this.caveVines, this.vines, this.bamboo, this.cocoa, this.beeNests};
   private final ColorSetting chunkColor = this.addSetting("chunk-color", new ColorSetting("Chunk Color", 1627325444));
   private final BooleanSetting tracers = this.addSetting("tracers", new BooleanSetting("Tracers", true));
   private final BooleanSetting detectMined = this.addSetting("detect-mined-clusters", new BooleanSetting("Detect Mined Clusters", false));
   private final ColorSetting minedColor = this.addSetting("mined-color", new ColorSetting("Mined Color", 1621511679));
   private final ChunkAreas chunkAreas = new ChunkAreas();
   private final ChunkOverlay chunkOverlay = new ChunkOverlay();
   private final ChunkAreas minedAreas = new ChunkAreas();
   private final ChunkOverlay minedOverlay = new ChunkOverlay();
   private final GeodeGlowScan glow = new GeodeGlowScan();
   private final ServerBlockLight serverLight = new ServerBlockLight();
   private int darkVotes;
   private int glowVotes;
   private final Long2IntOpenHashMap[] heat = perKind();
   private final Long2IntOpenHashMap[] vetoes = perKind();
   private final Long2ObjectOpenHashMap<int[]> chunkGrown = new Long2ObjectOpenHashMap();
   private final Long2IntOpenHashMap chunkUngrown = new Long2IntOpenHashMap();
   private final Long2BooleanOpenHashMap chunkHasPacket = new Long2BooleanOpenHashMap();
   private final LongOpenHashSet scannedChunks = new LongOpenHashSet();
   private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
   private final LongOpenHashSet queued = new LongOpenHashSet();
   private final LongOpenHashSet lightDirty = new LongOpenHashSet();
   private final Long2IntOpenHashMap waitingSince = new Long2IntOpenHashMap();
   private final Long2IntOpenHashMap bestLightClusters = new Long2IntOpenHashMap();
   private final Long2IntOpenHashMap bestLightBuds = new Long2IntOpenHashMap();
   private final LongOpenHashSet geodeChunks = new LongOpenHashSet();
   private Long2IntOpenHashMap visible = new Long2IntOpenHashMap();
   private boolean visibleDirty = true;
   private int visibleAge;
   private ClientWorld lastLevel;
   private int appliedRadius;
   private final Long2ObjectOpenHashMap<SusChunkFinderModule.GeodeSlice> geodeSlices = new Long2ObjectOpenHashMap();
   private final Long2IntOpenHashMap geodeWaiting = new Long2IntOpenHashMap();
   private final Long2IntOpenHashMap geodeScanTicks = new Long2IntOpenHashMap();
   private List<SusChunkFinderModule.GeodeCluster> geodeClusters = List.of();
   private boolean geodeClustersDirty;
   private LongOpenHashSet minedChunks = new LongOpenHashSet();
   private int minedAge;
   private int ticks;

   public SusChunkFinderModule() {
      super(
         "sus-chunk-finder",
         "Sus Chunk Finder",
         "Finds chunks with signs of player activity from grown amethyst, kelp, vines, bamboo, cocoa and bee nests.",
         ModuleCategory.BASE_RAID
      );
      this.method.onChange(this::rescan);
      this.sensitivity.onChange(() -> this.visibleDirty = true);

      for (BooleanSetting var4 : this.growthSigns) {
         if (var4 != null) {
            var4.visibleWhen(() -> this.method.is("Classic"));
            var4.onChange(this::rescan);
         }
      }

      this.detectMined.onChange(this::restartGeodes);
      this.minedColor.visibleWhen(this.detectMined::get);
   }

   private BooleanSetting growthSign(String var1, String var2) {
      return this.addSetting(var1, new BooleanSetting(var2, true));
   }

   private static Long2IntOpenHashMap[] perKind() {
      Long2IntOpenHashMap[] var0 = new Long2IntOpenHashMap[7];

      for (int var1 = 0; var1 < 7; var1++) {
         var0[var1] = new Long2IntOpenHashMap();
      }

      return var0;
   }

   private int countedKinds() {
      int var1 = 1;

      for (int var2 = 0; var2 < 7; var2++) {
         if (this.growthSigns[var2] != null && this.growthSigns[var2].get()) {
            var1 |= 1 << var2;
         }
      }

      return var1;
   }

   private void rescan() {
      this.clearAll();
      this.queueLoadedChunks();
   }

   private void restartGeodes() {
      this.clearGeodes();
      if (this.detectMined.get()) {
         this.queueLoadedChunks();
      }
   }

   @Override
   protected void onEnable() {
      this.clearAll();
      this.resetLight();
      this.lastLevel = MinecraftClient.method_1551().field_1687;
      this.queueLoadedChunks();
   }

   @Override
   protected void onDisable() {
      this.clearAll();
      this.resetLight();
      this.lastLevel = null;
   }

   private void resetLight() {
      this.serverLight.clear();
      this.darkVotes = 0;
      this.glowVotes = 0;
   }

   private void clearAll() {
      for (int var1 = 0; var1 < 7; var1++) {
         this.heat[var1].clear();
         this.vetoes[var1].clear();
      }

      this.chunkGrown.clear();
      this.chunkUngrown.clear();
      this.chunkHasPacket.clear();
      this.scannedChunks.clear();
      this.queue.clear();
      this.queued.clear();
      this.lightDirty.clear();
      this.waitingSince.clear();
      this.bestLightClusters.clear();
      this.bestLightBuds.clear();
      this.geodeChunks.clear();
      this.visible = new Long2IntOpenHashMap();
      this.visibleDirty = true;
      this.appliedRadius = this.spreadRadius.getInt();
      this.clearGeodes();
   }

   private void clearGeodes() {
      this.geodeSlices.clear();
      this.geodeWaiting.clear();
      this.geodeScanTicks.clear();
      this.geodeClusters = List.of();
      this.geodeClustersDirty = false;
      this.minedChunks = new LongOpenHashSet();
      this.minedAge = 0;
   }

   private void queueLoadedChunks() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (var1.field_1687 != null && var1.field_1724 != null) {
         int var2 = (Integer)var1.field_1690.method_42503().method_41753() + 1;
         ChunkPos var3 = var1.field_1724.method_31476();

         for (int var4 = var3.field_9181 - var2; var4 <= var3.field_9181 + var2; var4++) {
            for (int var5 = var3.field_9180 - var2; var5 <= var3.field_9180 + var2; var5++) {
               if (LevelUtil.isLoaded(var1.field_1687, var4, var5)) {
                  this.enqueue(ChunkPos.method_8331(var4, var5));
               }
            }
         }
      }
   }

   private void enqueue(long var1) {
      if (this.queued.add(var1)) {
         this.queue.enqueue(var1);
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      if (var1.packet instanceof ChunkDataS2CPacket var4) {
         if (var2 != null) {
            this.serverLight.onChunk(var2, var4.method_11523(), var4.method_11524(), var4.method_38599());
         }

         this.enqueue(ChunkPos.method_8331(var4.method_11523(), var4.method_11524()));
         this.markLightDirty(var4.method_11523(), var4.method_11524());
      } else if (var1.packet instanceof LightUpdateS2CPacket var6) {
         if (var2 != null) {
            this.serverLight.onLight(var2, var6.method_11558(), var6.method_11554(), var6.method_38600());
         }

         this.markLightDirty(var6.method_11558(), var6.method_11554());
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent var1) {
      this.serverLight.onBlockChanged(var1.pos);
   }

   private void markLightDirty(int var1, int var2) {
      for (int var3 = -1; var3 <= 1; var3++) {
         for (int var4 = -1; var4 <= 1; var4++) {
            long var5 = ChunkPos.method_8331(var1 + var3, var2 + var4);
            if (this.geodeChunks.contains(var5) || !this.scannedChunks.contains(var5)) {
               this.lightDirty.add(var5);
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      ClientWorld var5 = var4.field_1687;
      if (var5 != this.lastLevel) {
         this.clearAll();
         this.resetLight();
         this.lastLevel = var5;
         this.queueLoadedChunks();
      }

      if (var5 != null) {
         this.ticks++;
         if (this.spreadRadius.getInt() != this.appliedRadius) {
            this.clearAll();
            this.queueLoadedChunks();
         }

         if (this.ticks % 1200 == 0) {
            this.lightDirty.clear();
            this.queueLoadedChunks();
         } else if (this.ticks % 20 == 0 && !this.lightDirty.isEmpty()) {
            LongIterator var6 = this.lightDirty.iterator();

            while (var6.hasNext()) {
               long var7 = var6.next();
               if (LevelUtil.isLoaded(var5, ChunkPos.method_8325(var7), ChunkPos.method_8332(var7))) {
                  this.enqueue(var7);
               }
            }

            this.lightDirty.clear();
         }

         boolean var15 = PerformanceModule.optimizationsEnabled();
         long var16 = System.nanoTime();
         long var2 = var15 ? PerformanceBudget.acquire(2000000L) : var16 + 2000000L;

         while (!this.queue.isEmpty() && System.nanoTime() < var2) {
            long var11 = this.queue.dequeueLong();
            this.queued.remove(var11);
            int var13 = ChunkPos.method_8325(var11);
            int var14 = ChunkPos.method_8332(var11);
            if (!LevelUtil.isLoaded(var5, var13, var14)) {
               this.waitingSince.remove(var11);
            } else if (this.method.is("ANBS+ Scan") && !this.lightSettled(var5, var11)) {
               this.lightDirty.add(var11);
            } else {
               this.waitingSince.remove(var11);
               this.updateChunk(var11, var5.method_8497(var13, var14), var5);
               this.scannedChunks.add(var11);
               this.visibleDirty = true;
            }
         }

         if (var15) {
            PerformanceBudget.record(var16);
         }

         this.visibleAge++;
         if (this.visibleDirty && this.visibleAge >= 5 || this.visibleAge >= 20) {
            this.visible = this.computeVisible(var5);
            this.visibleDirty = false;
            this.visibleAge = 0;
         }

         if (this.ticks % 200 == 0) {
            this.pruneFar(MinecraftClient.method_1551());
         }

         if (this.detectMined.get() && ++this.minedAge >= 20) {
            this.minedChunks = this.computeMined(var5);
            this.minedAge = 0;
         }
      }
   }

   private boolean lightSettled(ClientWorld var1, long var2) {
      if (!this.waitingSince.containsKey(var2)) {
         this.waitingSince.put(var2, this.ticks);
      }

      if (this.ticks - this.waitingSince.get(var2) >= 200) {
         return true;
      } else {
         LightingProvider var4 = var1.method_22336();
         if (var4.method_15518()) {
            return false;
         } else {
            int var5 = ChunkPos.method_8325(var2);
            int var6 = ChunkPos.method_8332(var2);

            for (int var7 = -1; var7 <= 1; var7++) {
               for (int var8 = -1; var8 <= 1; var8++) {
                  if (!LevelUtil.isLoaded(var1, var5 + var7, var6 + var8)) {
                     return false;
                  }

                  if (!var4.method_62874(ChunkSectionPos.method_51687(var5 + var7, var6 + var8))) {
                     return false;
                  }
               }
            }

            return true;
         }
      }
   }

   private void pruneFar(MinecraftClient var1) {
      if (var1.field_1724 != null) {
         int var2 = var1.field_1690.method_38521() + 2 * this.spreadRadius.getInt() + 4;
         int var3 = var1.field_1724.method_31477() >> 4;
         int var4 = var1.field_1724.method_31479() >> 4;
         LongArrayList var5 = new LongArrayList();
         LongIterator var6 = this.scannedChunks.iterator();

         while (var6.hasNext()) {
            long var7 = var6.next();
            if (Math.max(Math.abs(ChunkPos.method_8325(var7) - var3), Math.abs(ChunkPos.method_8332(var7) - var4)) > var2) {
               var5.add(var7);
            }
         }

         for (int var9 = 0; var9 < var5.size(); var9++) {
            this.forget(var5.getLong(var9));
         }

         if (!var5.isEmpty()) {
            this.visibleDirty = true;
         }

         if (var1.field_1687 != null) {
            this.serverLight.forgetUnloaded(var1.field_1687);
         }
      }
   }

   private void forget(long var1) {
      int var3 = this.spreadRadius.getInt();
      int var4 = ChunkPos.method_8325(var1);
      int var5 = ChunkPos.method_8332(var1);
      this.withdraw(var4, var5, var3, (int[])this.chunkGrown.remove(var1), this.chunkUngrown.remove(var1));
      this.chunkHasPacket.remove(var1);
      this.scannedChunks.remove(var1);
      this.bestLightClusters.remove(var1);
      this.bestLightBuds.remove(var1);
      this.geodeChunks.remove(var1);
      this.waitingSince.remove(var1);
      this.lightDirty.remove(var1);
   }

   private void updateChunk(long var1, WorldChunk var3, ClientWorld var4) {
      int var5 = this.spreadRadius.getInt();
      int var6 = ChunkPos.method_8325(var1);
      int var7 = ChunkPos.method_8332(var1);
      this.withdraw(var6, var7, var5, (int[])this.chunkGrown.remove(var1), this.chunkUngrown.remove(var1));
      this.chunkHasPacket.put(var1, hasBlocksInRange(var3, -16, 30));
      if (hasLining(var3)) {
         this.geodeChunks.add(var1);
      } else {
         this.geodeChunks.remove(var1);
      }

      SusChunkFinderModule.ChunkScore var8 = this.scoreChunk(var3, var4);
      int var9 = this.bestLightClusters.get(var1);
      int var10 = this.bestLightBuds.get(var1);
      if (var8.lightClusters() > var9 || var8.lightClusters() == var9 && var8.lightBuds() > var10) {
         var9 = var8.lightClusters();
         var10 = var8.lightBuds();
         this.bestLightClusters.put(var1, var9);
         this.bestLightBuds.put(var1, var10);
      }

      int[] var11 = var8.grown();
      var11[0] += var9;
      int var12 = var10 > 0 ? var8.ungrown() | 1 : var8.ungrown();
      this.chunkGrown.put(var1, var11);
      this.chunkUngrown.put(var1, var12);
      this.spread(var6, var7, var5, var11, var12);
      if (this.detectMined.get()) {
         this.collectGeode(var1, var3, var4);
      }
   }

   private void spread(int var1, int var2, int var3, int[] var4, int var5) {
      for (int var6 = 0; var6 < 7; var6++) {
         Long2IntOpenHashMap var7 = this.heat[var6];
         Long2IntOpenHashMap var8 = this.vetoes[var6];
         int var9 = var4[var6];
         if (var9 > 0) {
            forArea(var1, var2, var3, var2x -> var7.addTo(var2x, var9));
         }

         if ((var5 & 1 << var6) != 0) {
            forArea(var1, var2, var3, var1x -> var8.addTo(var1x, 1));
         }
      }
   }

   private void withdraw(int var1, int var2, int var3, int[] var4, int var5) {
      for (int var6 = 0; var6 < 7; var6++) {
         Long2IntOpenHashMap var8 = this.heat[var6];
         Long2IntOpenHashMap var9 = this.vetoes[var6];
         int var7 = var4 == null ? 0 : var4[var6];
         if (var7 > 0) {
            forArea(var1, var2, var3, var2x -> subtract(var8, var2x, var7));
         }

         if ((var5 & 1 << var6) != 0) {
            forArea(var1, var2, var3, var1x -> subtract(var9, var1x, 1));
         }
      }
   }

   private static void forArea(int var0, int var1, int var2, LongConsumer var3) {
      for (int var4 = -var2; var4 <= var2; var4++) {
         for (int var5 = -var2; var5 <= var2; var5++) {
            var3.accept(ChunkPos.method_8331(var0 + var4, var1 + var5));
         }
      }
   }

   private static void subtract(Long2IntOpenHashMap var0, long var1, int var3) {
      int var4 = var0.get(var1) - var3;
      if (var4 > 0) {
         var0.put(var1, var4);
      } else {
         var0.remove(var1);
      }
   }

   private static boolean hasLining(WorldChunk var0) {
      for (ChunkSection var4 : var0.method_12006()) {
         if (var4 != null && !var4.method_38292() && var4.method_19523(SusChunkFinderModule::isLining)) {
            return true;
         }
      }

      return false;
   }

   private static boolean hasBlocksInRange(WorldChunk var0, int var1, int var2) {
      ChunkSection[] var3 = var0.method_12006();
      int var4 = var0.method_32891();

      for (int var5 = 0; var5 < var3.length; var5++) {
         ChunkSection var7 = var3[var5];
         int var6;
         if (var7 != null && !var7.method_38292() && (var6 = var4 + var5 << 4) + 15 >= var1 && var6 <= var2) {
            for (int var8 = 0; var8 < 16; var8++) {
               int var9 = var6 + var8;
               if (var9 >= var1 && var9 <= var2) {
                  for (int var10 = 0; var10 < 16; var10++) {
                     for (int var11 = 0; var11 < 16; var11++) {
                        if (!var7.method_12254(var10, var8, var11).method_26215()) {
                           return true;
                        }
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private SusChunkFinderModule.ChunkScore scoreChunk(WorldChunk var1, ClientWorld var2) {
      int[] var3 = new int[7];
      if (this.method.is("ANBS+ Scan")) {
         var3[0] = this.glow.scan(var2, var1, null) / 12;
         return new SusChunkFinderModule.ChunkScore(var3, 0, 0, 0);
      } else {
         ChunkSection[] var4 = var1.method_12006();
         int var5 = var1.method_32891();
         ChunkPos var6 = var1.method_12004();
         int var7 = this.countedKinds();
         int var8 = 0;
         Mutable var9 = new Mutable();

         for (int var10 = 0; var10 < var4.length; var10++) {
            ChunkSection var11 = var4[var10];
            if (var11 != null && !var11.method_38292() && var11.method_19523(var1x -> isCounted(var1x, var7))) {
               int var12 = var5 + var10 << 4;

               for (int var13 = 0; var13 < 16; var13++) {
                  for (int var14 = 0; var14 < 16; var14++) {
                     for (int var15 = 0; var15 < 16; var15++) {
                        BlockState var16 = var11.method_12254(var13, var14, var15);
                        int var17 = growthKind(var16);
                        if (var17 >= 0 && (var7 & 1 << var17) != 0) {
                           var9.method_10103(var6.method_8326() + var13, var12 + var14, var6.method_8328() + var15);
                           int var18 = readSign(var2, var9, var16, var17);
                           if (var18 == 1) {
                              var3[var17]++;
                           } else if (var18 == -1) {
                              var8 |= 1 << var17;
                           }
                        }
                     }
                  }
               }
            }
         }

         HiddenAmethyst.Reading var20 = HiddenAmethyst.read(var2, var1, this.serverLight, this.hiddenSolids());
         this.darkVotes = this.darkVotes + var20.darkVotes();
         this.glowVotes = this.glowVotes + var20.glowVotes();
         return new SusChunkFinderModule.ChunkScore(var3, var8, var20.clusters(), var20.buds());
      }
   }

   private Boolean hiddenSolids() {
      if (onDonut()) {
         return Boolean.TRUE;
      } else if (this.darkVotes >= 3 && this.darkVotes > this.glowVotes) {
         return Boolean.TRUE;
      } else {
         return this.glowVotes >= 3 && this.glowVotes > this.darkVotes ? Boolean.FALSE : null;
      }
   }

   private static boolean onDonut() {
      ClientPlayNetworkHandler var0 = MinecraftClient.method_1551().method_1562();
      String var1 = var0 == null ? null : var0.method_52790();
      return var1 != null && var1.contains("DonutFolia");
   }

   private static boolean isCounted(BlockState var0, int var1) {
      int var2 = growthKind(var0);
      return var2 >= 0 && (var1 & 1 << var2) != 0;
   }

   private static int growthKind(BlockState var0) {
      Block var1 = var0.method_26204();
      if (var1 == Blocks.field_27161 || isBud(var0)) {
         return 0;
      } else if (var1 == Blocks.field_9993) {
         return 1;
      } else if (var1 == Blocks.field_28675) {
         return 2;
      } else if (var1 == Blocks.field_10597) {
         return 3;
      } else if (var1 == Blocks.field_10211) {
         return 4;
      } else if (var1 == Blocks.field_10302) {
         return 5;
      } else {
         return var1 == Blocks.field_20421 ? 6 : -1;
      }
   }

   private static int readSign(ClientWorld var0, Mutable var1, BlockState var2, int var3) {
      return switch (var3) {
         case 0 -> {
            if (var2.method_27852(Blocks.field_27161)) {
               yield 1;
            } else if (!var2.method_28498(Properties.field_12525)) {
               yield 0;
            } else {
               Direction var5 = (Direction)var2.method_11654(Properties.field_12525);
               yield var0.method_8320(var1.method_10098(var5.method_10153())).method_27852(Blocks.field_27160) ? -1 : 1;
            }
         }
         case 1 -> var2.method_11654(Properties.field_12517) != 25
               && var0.method_8320(var1.method_10098(Direction.field_11036)).method_27852(Blocks.field_10382)
            ? -1
            : 1;
         case 2 -> var2.method_11654(Properties.field_12517) != 25 && var0.method_8320(var1.method_10098(Direction.field_11033)).method_26215() ? -1 : 1;
         case 3 -> {
            BlockState var4 = var0.method_8320(var1.method_10098(Direction.field_11033));
            yield var4.method_27852(Blocks.field_10597) ? 0 : (!var4.method_26215() ? 1 : (hangsFromSide(var2) ? -1 : 1));
         }
         case 4 -> bambooSign(var0, var1, var2);
         case 5 -> var2.method_11654(Properties.field_12556) == 2 ? 1 : -1;
         case 6 -> var2.method_11654(BeehiveBlock.field_20420) == 5 ? 1 : -1;
         default -> 0;
      };
   }

   private static boolean hangsFromSide(BlockState var0) {
      return (Boolean)var0.method_11654(Properties.field_12489)
         || (Boolean)var0.method_11654(Properties.field_12487)
         || (Boolean)var0.method_11654(Properties.field_12540)
         || (Boolean)var0.method_11654(Properties.field_12527);
   }

   private static int bambooSign(ClientWorld var0, Mutable var1, BlockState var2) {
      int var4 = var1.method_10263();
      int var5 = var1.method_10264();
      int var6 = var1.method_10260();
      BlockState var7 = var0.method_8320(var1.method_10098(Direction.field_11036));
      if (var7.method_27852(Blocks.field_10211)) {
         return 0;
      } else if ((Integer)var2.method_11654(Properties.field_12549) == 1) {
         return 1;
      } else if (var7.method_26215() && var0.method_22335(var1, 0) >= 9) {
         int var3 = 1;

         while (var3 < 16 && var0.method_8320(var1.method_10103(var4, var5 - var3, var6)).method_27852(Blocks.field_10211)) {
            var3++;
         }

         return var3 < 16 ? -1 : 0;
      } else {
         return 0;
      }
   }

   private void collectGeode(long var1, WorldChunk var3, ClientWorld var4) {
      this.geodeScanTicks.put(var1, this.ticks);
      if (this.geodeSlices.remove(var1) != null) {
         this.geodeClustersDirty = true;
      }

      this.geodeWaiting.remove(var1);
      int var6 = ChunkPos.method_8325(var1);
      int var7 = ChunkPos.method_8332(var1);

      for (int var8 = -1; var8 <= 1; var8++) {
         for (int var9 = -1; var9 <= 1; var9++) {
            long var10 = ChunkPos.method_8331(var6 + var8, var7 + var9);
            if ((var8 != 0 || var9 != 0) && (this.geodeWaiting.get(var10) & waitBit(-var8, -var9)) != 0) {
               this.enqueue(var10);
            }
         }
      }

      if (LevelUtil.isOverworld(var4)) {
         ChunkSection[] var38 = var3.method_12006();
         int var39 = var3.method_32891();
         int var40 = var6 << 4;
         int var11 = var7 << 4;
         LongArrayList var12 = new LongArrayList();
         LongArrayList var13 = new LongArrayList();
         Mutable var14 = new Mutable();
         int var15 = 0;

         int var5;
         for (int var16 = 0; var16 < var38.length && (var5 = var39 + var16 << 4) <= 47; var16++) {
            ChunkSection var17 = var38[var16];
            if (var17 != null && !var17.method_38292() && var17.method_19523(SusChunkFinderModule::isLining)) {
               for (int var18 = 0; var18 < 16; var18++) {
                  for (int var19 = 0; var19 < 16; var19++) {
                     for (int var20 = 0; var20 < 16; var20++) {
                        BlockState var21 = var17.method_12254(var20, var18, var19);
                        if (isLining(var21)) {
                           int var22 = var40 + var20;
                           int var23 = var5 + var18;
                           int var24 = var11 + var19;
                           boolean var25 = false;
                           int var26 = 0;

                           for (Direction var30 : SIDES) {
                              int var32 = var22 + var30.method_10148();
                              int var33 = var23 + var30.method_10164();
                              int var34 = var24 + var30.method_10165();
                              if (!var4.method_31601(var33)) {
                                 int var35 = var32 >> 4;
                                 int var36 = var34 >> 4;
                                 boolean var31 = var35 == var6 && var36 == var7;
                                 if (!var31 && !LevelUtil.isLoaded(var4, var35, var36)) {
                                    var26 |= waitBit(var35 - var6, var36 - var7);
                                 } else {
                                    var14.method_10103(var32, var33, var34);
                                    if (isOpen(var31 ? var3.method_8320(var14) : var4.method_8320(var14))) {
                                       var25 = true;
                                       break;
                                    }
                                 }
                              }
                           }

                           if (!var25) {
                              var15 |= var26;
                           } else {
                              long var41 = BlockPos.method_10064(var22, var23, var24);
                              var12.add(var41);
                              if (var21.method_27852(Blocks.field_27160)) {
                                 var13.add(var41);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (var15 != 0) {
            this.geodeWaiting.put(var1, var15);
         }

         if (!var12.isEmpty()) {
            this.geodeSlices.put(var1, new SusChunkFinderModule.GeodeSlice(var12.toLongArray(), var13.toLongArray()));
            this.geodeClustersDirty = true;
         }
      }
   }

   private static int waitBit(int var0, int var1) {
      return 1 << (var0 + 1) * 3 + var1 + 1;
   }

   private LongOpenHashSet computeMined(ClientWorld var1) {
      if (this.geodeSlices.keySet().removeIf(var1x -> !LevelUtil.isLoaded(var1, ChunkPos.method_8325(var1x), ChunkPos.method_8332(var1x)))) {
         this.geodeClustersDirty = true;
      }

      this.geodeWaiting.keySet().removeIf(var1x -> !LevelUtil.isLoaded(var1, ChunkPos.method_8325(var1x), ChunkPos.method_8332(var1x)));
      this.geodeScanTicks.keySet().removeIf(var1x -> !LevelUtil.isLoaded(var1, ChunkPos.method_8325(var1x), ChunkPos.method_8332(var1x)));
      if (this.geodeClustersDirty) {
         this.geodeClusters = this.clusterGeodes();
         this.geodeClustersDirty = false;
      }

      LongOpenHashSet var2 = new LongOpenHashSet();

      for (SusChunkFinderModule.GeodeCluster var4 : this.geodeClusters) {
         boolean var5 = var4.budding() > 0 ? var4.budding() >= 10 : var4.lining().length >= 160;
         if (var5 && this.settled(var1, var4) && unlit(var1, var4.lining())) {
            var2.add(var4.chunk());
         }
      }

      return var2;
   }

   private List<SusChunkFinderModule.GeodeCluster> clusterGeodes() {
      Long2ObjectOpenHashMap var3 = new Long2ObjectOpenHashMap();
      LongOpenHashSet var4 = new LongOpenHashSet();
      ObjectIterator var5 = this.geodeSlices.values().iterator();

      while (var5.hasNext()) {
         SusChunkFinderModule.GeodeSlice var6 = (SusChunkFinderModule.GeodeSlice)var5.next();

         for (long var10 : var6.lining()) {
            long var1 = BlockPos.method_10064(BlockPos.method_10061(var10) >> 2, BlockPos.method_10071(var10) >> 2, BlockPos.method_10083(var10) >> 2);
            LongArrayList var12 = (LongArrayList)var3.get(var1);
            if (var12 == null) {
               var12 = new LongArrayList();
               var3.put(var1, var12);
            }

            var12.add(var10);
         }

         for (long var29 : var6.budding()) {
            var4.add(var29);
         }
      }

      ArrayList var21 = new ArrayList();
      LongOpenHashSet var22 = new LongOpenHashSet();
      LongArrayFIFOQueue var24 = new LongArrayFIFOQueue();
      LongIterator var26 = var3.keySet().iterator();

      while (var26.hasNext()) {
         long var28 = var26.next();
         if (var22.add(var28)) {
            LongArrayList var11 = new LongArrayList();
            var24.enqueue(var28);

            while (!var24.isEmpty()) {
               long var20 = var24.dequeueLong();
               var11.addAll((LongList)var3.get(var20));
               int var30 = BlockPos.method_10061(var20);
               int var13 = BlockPos.method_10071(var20);
               int var14 = BlockPos.method_10083(var20);

               for (int var15 = -1; var15 <= 1; var15++) {
                  for (int var16 = -1; var16 <= 1; var16++) {
                     for (int var17 = -1; var17 <= 1; var17++) {
                        long var18 = BlockPos.method_10064(var30 + var15, var13 + var16, var14 + var17);
                        if (var3.containsKey(var18) && var22.add(var18)) {
                           var24.enqueue(var18);
                        }
                     }
                  }
               }
            }

            var21.add(toCluster(var11.toLongArray(), var4));
         }
      }

      return var21;
   }

   private static SusChunkFinderModule.GeodeCluster toCluster(long[] var0, LongOpenHashSet var1) {
      int var2 = 0;
      int var3 = 2147483647;
      int var4 = 2147483647;
      int var5 = -2147483648;
      int var6 = -2147483648;
      long var7 = 0L;
      long var9 = 0L;

      for (long var14 : var0) {
         if (var1.contains(var14)) {
            var2++;
         }

         int var16 = BlockPos.method_10061(var14);
         int var17 = BlockPos.method_10083(var14);
         var3 = Math.min(var3, var16);
         var4 = Math.min(var4, var17);
         var5 = Math.max(var5, var16);
         var6 = Math.max(var6, var17);
         var7 += var16;
         var9 += var17;
      }

      int var18 = (int)Math.floorDiv(var7, var0.length);
      int var19 = (int)Math.floorDiv(var9, var0.length);
      return new SusChunkFinderModule.GeodeCluster(var0, var2, var3, var4, var5, var6, ChunkPos.method_8331(var18 >> 4, var19 >> 4));
   }

   private boolean settled(ClientWorld var1, SusChunkFinderModule.GeodeCluster var2) {
      for (int var3 = var2.minX() - 1 >> 4; var3 <= var2.maxX() + 1 >> 4; var3++) {
         for (int var4 = var2.minZ() - 1 >> 4; var4 <= var2.maxZ() + 1 >> 4; var4++) {
            long var5 = ChunkPos.method_8331(var3, var4);
            if (!LevelUtil.isLoaded(var1, var3, var4) || !this.geodeScanTicks.containsKey(var5)) {
               return false;
            }

            if (this.ticks - this.geodeScanTicks.get(var5) < 40) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean unlit(ClientWorld var0, long[] var1) {
      Mutable var2 = new Mutable();
      Long2IntOpenHashMap var3 = new Long2IntOpenHashMap();

      for (long var7 : var1) {
         int var9 = BlockPos.method_10061(var7);
         int var10 = BlockPos.method_10071(var7);
         int var11 = BlockPos.method_10083(var7);

         for (Direction var15 : SIDES) {
            var2.method_10103(var9 + var15.method_10148(), var10 + var15.method_10164(), var11 + var15.method_10165());
            if (isAmethystGrowth(var0.method_8320(var2))) {
               return false;
            }

            int var16 = var0.method_8314(LightType.field_9282, var2);
            if (var16 > 0 && sourceLight(var0, var2, var16, var3) != 14) {
               return false;
            }
         }
      }

      return true;
   }

   private static int sourceLight(ClientWorld var0, BlockPos var1, int var2, Long2IntOpenHashMap var3) {
      LongArrayList var5 = new LongArrayList();
      Mutable var6 = var1.method_25503();
      Mutable var7 = new Mutable();

      int var4;
      while (true) {
         long var8;
         if (var3.containsKey(var8 = var6.method_10063())) {
            var4 = var3.get(var8);
            break;
         }

         var5.add(var8);
         Direction var10 = null;
         int var11 = var2;

         for (Direction var15 : SIDES) {
            int var16 = var0.method_8314(LightType.field_9282, var7.method_25505(var6, var15));
            if (var16 > var11) {
               var11 = var16;
               var10 = var15;
            }
         }

         if (var10 == null) {
            var4 = var2;
            break;
         }

         var6.method_10098(var10);
         var2 = var11;
      }

      for (int var17 = 0; var17 < var5.size(); var17++) {
         var3.put(var5.getLong(var17), var4);
      }

      return var4;
   }

   private static boolean isLining(BlockState var0) {
      return var0.method_27852(Blocks.field_27159) || var0.method_27852(Blocks.field_27160);
   }

   private static boolean isOpen(BlockState var0) {
      return BuddingAmethystBlock.method_31626(var0) || isAmethystGrowth(var0);
   }

   private static boolean isBud(BlockState var0) {
      Block var1 = var0.method_26204();
      return var1 == Blocks.field_27164 || var1 == Blocks.field_27163 || var1 == Blocks.field_27162;
   }

   private static boolean isAmethystGrowth(BlockState var0) {
      return var0.method_27852(Blocks.field_27161) || isBud(var0);
   }

   private Long2IntOpenHashMap computeVisible(ClientWorld var1) {
      Long2IntOpenHashMap var2 = new Long2IntOpenHashMap();

      for (int var3 = 0; var3 < 7; var3++) {
         Long2IntOpenHashMap var4 = this.vetoes[var3];
         ObjectIterator var5 = this.heat[var3].long2IntEntrySet().iterator();

         while (var5.hasNext()) {
            Entry var6 = (Entry)var5.next();
            if (!var4.containsKey(var6.getLongKey())) {
               var2.addTo(var6.getLongKey(), var6.getIntValue());
            }
         }
      }

      Long2IntOpenHashMap var13 = new Long2IntOpenHashMap();
      ObjectIterator var14 = var2.long2IntEntrySet().iterator();

      while (var14.hasNext()) {
         Entry var16 = (Entry)var14.next();
         long var7 = var16.getLongKey();
         int var9 = ChunkPos.method_8325(var7);
         int var18;
         if (LevelUtil.isLoaded(var1, var9, var18 = ChunkPos.method_8332(var7)) && this.scannedNeighbours(var9, var18) >= 3) {
            var13.put(var7, var16.getIntValue());
         }
      }

      int var15 = this.sensitivity.getInt();
      int var17 = this.appliedRadius;
      Long2IntOpenHashMap var19 = new Long2IntOpenHashMap();
      ObjectIterator var20 = var13.long2IntEntrySet().iterator();

      while (var20.hasNext()) {
         Entry var8 = (Entry)var20.next();
         long var21 = var8.getLongKey();
         int var11 = var8.getIntValue();
         boolean var12 = var11 >= var15 || !this.chunkHasPacket.getOrDefault(var21, true) && this.amethystHeat(var21) >= 1;
         if (var12 && isPeak(var13, var21, var11, var17)) {
            var19.put(var21, var11);
         }
      }

      return var19;
   }

   private int amethystHeat(long var1) {
      return this.vetoes[0].containsKey(var1) ? 0 : this.heat[0].get(var1);
   }

   private static boolean isPeak(Long2IntOpenHashMap var0, long var1, int var3, int var4) {
      int var5 = ChunkPos.method_8325(var1);
      int var6 = ChunkPos.method_8332(var1);

      for (int var7 = -var4; var7 <= var4; var7++) {
         for (int var8 = -var4; var8 <= var4; var8++) {
            if ((var7 != 0 || var8 != 0) && var0.get(ChunkPos.method_8331(var5 + var7, var6 + var8)) > var3) {
               return false;
            }
         }
      }

      return true;
   }

   private int scannedNeighbours(int var1, int var2) {
      int var3 = 0;

      for (int var4 = -1; var4 <= 1; var4++) {
         for (int var5 = -1; var5 <= 1; var5++) {
            if ((var4 != 0 || var5 != 0) && this.scannedChunks.contains(ChunkPos.method_8331(var1 + var4, var2 + var5))) {
               var3++;
            }
         }
      }

      return var3;
   }

   public LongSet flaggedChunks() {
      return LongSets.unmodifiable(this.visible.keySet());
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      if (MinecraftClient.method_1551().field_1687 != null) {
         this.chunkOverlay.draw(this.visible.keySet(), this.chunkColor.argb(), var1.partialTick);
         if (this.tracers.get()) {
            this.chunkAreas.drawTracers(this.visible.keySet(), 63.0, ColorHelper.method_61334(this.chunkColor.argb()));
         }

         this.minedOverlay.draw(this.minedChunks, this.minedColor.argb(), var1.partialTick);
         if (this.tracers.get()) {
            this.minedAreas.drawTracers(this.minedChunks, 63.0, ColorHelper.method_61334(this.minedColor.argb()));
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record ChunkScore(int[] grown, int ungrown, int lightClusters, int lightBuds) {
   }

   @Environment(EnvType.CLIENT)
   private record GeodeCluster(long[] lining, int budding, int minX, int minZ, int maxX, int maxZ, long chunk) {
   }

   @Environment(EnvType.CLIENT)
   private record GeodeSlice(long[] lining, long[] budding) {
   }
}
