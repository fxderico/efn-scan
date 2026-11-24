package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.ChunkOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PropaguleBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.biome.Biome.Precipitation;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightProvider;

@Environment(EnvType.CLIENT)
public final class BetterSusChunkModule extends ClientModule {
   private static final String DONUT_BRAND = "DonutFolia";
   private static final int KELP = 0;
   private static final int CAVE_VINES = 1;
   private static final int WEEPING_VINES = 2;
   private static final int BAMBOO = 3;
   private static final int SUGAR_CANE = 4;
   private static final int CACTUS = 5;
   private static final int COCOA = 6;
   private static final int BERRY = 7;
   private static final int NETHER_WART = 8;
   private static final int CROP = 9;
   private static final int STEM = 10;
   private static final int AMETHYST = 11;
   private static final int VINE = 12;
   private static final int LEAVES = 13;
   private static final int SOIL = 14;
   private static final int PROPAGULE = 15;
   private static final int EYEBLOSSOM = 16;
   private static final int SNOW = 17;
   private static final int ICE = 18;
   private static final int EVIDENCE_KINDS = 19;
   private static final int GROWTH_LIGHT = 9;
   private static final int FULL_LIGHT = 15;
   private static final int MELT_LIGHT = 10;
   private static final int BAMBOO_LIMIT = 16;
   private static final int SHORT_COLUMN = 3;
   private static final int KELP_MAX_AGE = 25;
   private static final int DAY_TICKS = 24000;
   private static final int EYEBLOSSOM_OPENS = 12600;
   private static final int EYEBLOSSOM_CLOSES = 23401;
   private static final int EYEBLOSSOM_SETTLE = 4000;
   private static final int EYEBLOSSOM_MIN = 3;
   private static final int HOLE_DEPTH = 2;
   private static final int ENCLOSED_SIDES = 3;
   private static final int REFRESH_TICKS = 20;
   private static final int LOADED_MARGIN = 3;
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final int[][] SIDES = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
   private static final int FILL_ALPHA = 100;
   private final NumberSetting simDistance = this.addSetting("sim-distance", new NumberSetting("Simulation Distance", 4.0F, 2.0F, 16.0F).step(1.0F));
   private final ColorSetting chunkColor = this.addSetting("chunk-color", new ColorSetting("Chunk Color", -65536));
   private final NumberSetting sensitivity = this.addSetting("min-flag", new NumberSetting("Sensitivity", 40.0F, 1.0F, 500.0F).step(1.0F));
   private final BooleanSetting holeFilter = this.addSetting("hole-filter", new BooleanSetting("Hole Filter", false));
   private final NumberSetting holeDistance = this.addSetting("hole-distance", new NumberSetting("Hole Distance", 2.0F, 0.0F, 8.0F).step(1.0F));
   private final BooleanSetting customEvidence = this.addSetting("custom-evidence", new BooleanSetting("Custom Flags", false));
   private final BooleanSetting[] evidence = new BooleanSetting[]{
      this.evidence("count-kelp", "Kelp"),
      this.evidence("count-cave-vines", "Cave Vines"),
      this.evidence("count-weeping-vines", "Weeping Vines"),
      this.evidence("count-bamboo", "Bamboo"),
      this.evidence("count-sugar-cane", "Sugar Cane"),
      this.evidence("count-cactus", "Cactus"),
      this.evidence("count-cocoa", "Cocoa"),
      this.evidence("count-sweet-berries", "Sweet Berries"),
      this.evidence("count-nether-wart", "Nether Wart"),
      this.evidence("count-crops", "Crops"),
      this.evidence("count-stems", "Stems"),
      this.evidence("count-amethyst", "Amethyst"),
      this.evidence("count-vines", "Vines"),
      this.evidence("count-leaves", "Leaves"),
      this.evidence("count-grass-dirt", "Grass & Dirt"),
      this.evidence("count-propagules", "Mangrove Propagules"),
      this.evidence("count-eyeblossoms", "Eyeblossoms"),
      this.evidence("count-snow", "Snow"),
      this.evidence("count-ice", "Ice")
   };
   private final ChunkOverlay primaryOverlay = new ChunkOverlay();
   private final Object lock = new Object();
   private final Long2ObjectOpenHashMap<BetterSusChunkModule.Record> records = new Long2ObjectOpenHashMap<>();
   private final Long2IntOpenHashMap clearCoverage = new Long2IntOpenHashMap();
   private final Long2IntOpenHashMap support = new Long2IntOpenHashMap();
   private final LongOpenHashSet scanned = new LongOpenHashSet();
   private final LongArrayList distant = new LongArrayList();
   private final Set<Long> pending = ConcurrentHashMap.newKeySet();
   private final Set<Long> kelpChunks = ConcurrentHashMap.newKeySet();
   private final Set<Long> seenKelp = ConcurrentHashMap.newKeySet();
   private volatile LongSet primary = LongSets.EMPTY_SET;
   private volatile int generation;
   private volatile boolean dirty;
   private volatile boolean[] counted = new boolean[19];
   private ExecutorService executor;
   private RegistryKey<World> worldKey;
   private int tickCounter;

   public BetterSusChunkModule() {
      super(
         "better-sus-chunk",
         "Better Sus Chunk",
         "Marks chunks no player has been near: areas with overdue natural growth rule themselves out.",
         ModuleCategory.BASE_RAID
      );
      this.holeDistance.visibleWhen(this.holeFilter::get);
      this.simDistance.onChange(() -> this.dirty = true);
      this.holeFilter.onChange(() -> {
         if (this.holeFilter.get()) {
            this.dirty = true;
         }
      });
      this.customEvidence.onChange(this::refreshEvidence);

      for (BooleanSetting var4 : this.evidence) {
         var4.visibleWhen(this.customEvidence::get);
         var4.onChange(this::refreshEvidence);
      }

      this.refreshEvidence();
   }

   private BooleanSetting evidence(String string, String string2) {
      return this.addSetting(string, new BooleanSetting(string2, true));
   }

   private void refreshEvidence() {
      boolean var1 = this.customEvidence.get();
      boolean[] var2 = new boolean[19];

      for (int var3 = 0; var3 < var2.length; var3++) {
         var2[var3] = !var1 || this.evidence[var3].get();
      }

      this.counted = var2;
      this.dirty = true;
   }

   @Override
   protected void onEnable() {
      this.resetState();
      this.executor = Executors.newSingleThreadExecutor(runnable -> {
         Thread var1x = new Thread(runnable, "EFN Better Sus Chunk");
         var1x.setDaemon(true);
         return var1x;
      });
      ClientWorld var1 = MinecraftClient.getInstance().world;
      this.worldKey = var1 == null ? null : var1.getRegistryKey();
      this.dirty = false;
      this.rescanLoadedChunks();
   }

   @Override
   protected void onDisable() {
      if (this.executor != null) {
         this.executor.shutdownNow();
         this.executor = null;
      }

      this.resetState();
      this.worldKey = null;
   }

   private void resetState() {
      Object var1 = this.lock;
      synchronized (var1) {
         this.generation++;
         this.clearCoverage.clear();
         this.support.clear();
         this.records.clear();
         this.scanned.clear();
         this.pending.clear();
         this.kelpChunks.clear();
         this.seenKelp.clear();
      }

      this.primary = LongSets.EMPTY_SET;
      this.tickCounter = 0;
   }

   private void rescanLoadedChunks() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      ClientWorld var2 = var1.world;
      if (this.isEnabled() && var1.player != null && var2 != null) {
         this.resetState();
         int var3 = var1.options.getClampedViewDistance() + 3;
         ChunkPos var4 = var1.player.getChunkPos();

         for (int var5 = var4.x - var3; var5 <= var4.x + var3; var5++) {
            for (int var6 = var4.z - var3; var6 <= var4.z + var3; var6++) {
               if (var2.isChunkLoaded(var5, var6)) {
                  this.scheduleScan(ChunkPos.toLong(var5, var6));
               }
            }
         }
      }
   }

   private void scheduleScan(long l) {
      ExecutorService var3 = this.executor;
      if (var3 != null && !var3.isShutdown()) {
         if (this.pending.add(l)) {
            int var4 = this.generation;
            var3.submit(() -> this.runScan(l, var4));
         }
      }
   }

   private void runScan(long l, int i) {
      this.pending.remove(l);
      if (i == this.generation && this.isEnabled()) {
         ClientWorld var5 = MinecraftClient.getInstance().world;
         if (var5 != null) {
            int var6 = ChunkPos.getPackedX(l);
            int var4;
            if (var5.isChunkLoaded(var6, var4 = ChunkPos.getPackedZ(l))) {
               WorldChunk var7 = var5.getChunk(var6, var4);
               if (var7 != null) {
                  this.rescan(var5, l, var7, i);
               }
            }
         }
      }
   }

   private void rescan(ClientWorld clientWorld, long l, WorldChunk worldChunk, int i) {
      BetterSusChunkModule.Record var6 = this.removeRecord(l);
      BetterSusChunkModule.Source var7 = this.scanChunk(clientWorld, l, worldChunk);
      Object var8 = this.lock;
      synchronized (var8) {
         if (i == this.generation) {
            BetterSusChunkModule.Record var12 = new BetterSusChunkModule.Record(
               var7.clears() || var6 != null && var6.clears(),
               this.simDistance.getInt(),
               var7.hole() || var6 != null && var6.hole(),
               Math.max(var7.weight(), var6 == null ? 0 : var6.weight())
            );
            this.scanned.add(l);
            this.records.put(l, var12);
            if (var12.clears()) {
               int var13 = ChunkPos.getPackedX(l);
               int var14 = ChunkPos.getPackedZ(l);
               int var15 = 2 * var12.radius() + 2;

               for (int var11 = -var12.radius(); var11 <= var12.radius(); var11++) {
                  for (int var10 = -var12.radius(); var10 <= var12.radius(); var10++) {
                     this.clearCoverage.addTo(ChunkPos.toLong(var13 + var11, var14 + var10), 1);
                  }
               }

               for (int var19 = -var15; var19 <= var15; var19++) {
                  for (int var18 = -var15; var18 <= var15; var18++) {
                     this.support.addTo(ChunkPos.toLong(var13 + var19, var14 + var18), var12.weight());
                  }
               }
            }
         }
      }
   }

   private BetterSusChunkModule.Record removeRecord(long l) {
      Object var3 = this.lock;
      synchronized (var3) {
         BetterSusChunkModule.Record var7 = this.records.remove(l);
         this.scanned.remove(l);
         if (var7 != null && var7.clears()) {
            int var8 = ChunkPos.getPackedX(l);
            int var9 = ChunkPos.getPackedZ(l);
            int var10 = 2 * var7.radius() + 2;

            for (int var6 = -var7.radius(); var6 <= var7.radius(); var6++) {
               for (int var5 = -var7.radius(); var5 <= var7.radius(); var5++) {
                  subtract(this.clearCoverage, ChunkPos.toLong(var8 + var6, var9 + var5), 1);
               }
            }

            for (int var14 = -var10; var14 <= var10; var14++) {
               for (int var13 = -var10; var13 <= var10; var13++) {
                  subtract(this.support, ChunkPos.toLong(var8 + var14, var9 + var13), var7.weight());
               }
            }

            return var7;
         } else {
            return var7;
         }
      }
   }

   private static void subtract(Long2IntOpenHashMap long2IntOpenHashMap, long l, int i) {
      if (long2IntOpenHashMap.containsKey(l)) {
         int var4 = long2IntOpenHashMap.get(l);
         if (var4 <= i) {
            long2IntOpenHashMap.remove(l);
         } else {
            long2IntOpenHashMap.put(l, var4 - i);
         }
      }
   }

   private BetterSusChunkModule.Source scanChunk(ClientWorld clientWorld, long l, WorldChunk worldChunk) {
      boolean var5 = donutServer();
      boolean[] var6 = this.counted;
      int var7 = var5 && var6[0] && this.kelpChunks.contains(l) ? 1 : 0;
      BetterSusChunkModule.BlockReader var8 = new BetterSusChunkModule.BlockReader(clientWorld);
      ChunkPos var9 = worldChunk.getPos();
      int var10 = var9.getStartX();
      int var11 = var9.getStartZ();
      Boolean var12 = eyeblossomsOpen(clientWorld);
      int var13 = 0;
      int var14 = 0;
      ChunkSection[] var15 = worldChunk.getSectionArray();

      for (int var16 = 0; var16 < var15.length; var16++) {
         ChunkSection var17 = var15[var16];
         if (var17 != null && !var17.isEmpty() && var17.hasAny(blockState -> kindOf(blockState, var6) >= 0)) {
            int var18 = worldChunk.getBottomY() + (var16 << 4);

            for (int var19 = 0; var19 < 16; var19++) {
               for (int var20 = 0; var20 < 16; var20++) {
                  for (int var21 = 0; var21 < 16; var21++) {
                     BlockState var22 = var17.getBlockState(var21, var19, var20);
                     int var23 = kindOf(var22, var6);
                     if (var23 >= 0) {
                        if (var23 == 15) {
                           if (var22.get(PropaguleBlock.AGE) < 4) {
                              var7++;
                           }
                        } else if (var23 == 16) {
                           if (var12 != null) {
                              if (var22.isOf(Blocks.OPEN_EYEBLOSSOM) == var12) {
                                 var14++;
                              } else {
                                 var13++;
                              }
                           }
                        } else if (owed(var8, var10 + var21, var18 + var19, var11 + var20, var22, var23, var5)) {
                           var7++;
                        }
                     }
                  }
               }
            }
         }
      }

      if (var14 == 0 && var13 >= 3) {
         var7 += var13;
      }

      if ((var6[17] || var6[18]) && worldChunk.hasHeightmap(Type.MOTION_BLOCKING)) {
         Heightmap var24 = worldChunk.getHeightmap(Type.MOTION_BLOCKING);
         int var26 = clientWorld.getSeaLevel();

         for (int var27 = 0; var27 < 16; var27++) {
            for (int var28 = 0; var28 < 16; var28++) {
               int var29 = var24.get(var28, var27);
               if (surfaceOwed(clientWorld, var8, var10 + var28, var29, var11 + var27, var26, var5, var6)) {
                  var7++;
               }
            }
         }
      }

      boolean var25 = this.holeFilter.get() && hasHole(clientWorld, var9);
      return new BetterSusChunkModule.Source(var7 > 0, var25, var7);
   }

   private static int kindOf(BlockState blockState, boolean[] bls) {
      int var2 = classify(blockState);
      return var2 >= 0 && bls[var2] ? var2 : -1;
   }

   private static int classify(BlockState blockState) {
      Block var1 = blockState.getBlock();
      if (var1 == Blocks.KELP) {
         return 0;
      } else if (var1 == Blocks.CAVE_VINES) {
         return 1;
      } else if (var1 == Blocks.WEEPING_VINES) {
         return 2;
      } else if (var1 == Blocks.BAMBOO) {
         return 3;
      } else if (var1 == Blocks.SUGAR_CANE) {
         return 4;
      } else if (var1 == Blocks.CACTUS) {
         return 5;
      } else if (var1 == Blocks.COCOA) {
         return 6;
      } else if (var1 == Blocks.SWEET_BERRY_BUSH) {
         return 7;
      } else if (var1 == Blocks.NETHER_WART) {
         return 8;
      } else if (var1 == Blocks.PUMPKIN_STEM || var1 == Blocks.MELON_STEM) {
         return 10;
      } else if (var1 == Blocks.VINE) {
         return 12;
      } else if (var1 == Blocks.BUDDING_AMETHYST || var1 == Blocks.AMETHYST_CLUSTER || isBud(blockState)) {
         return 11;
      } else if (var1 == Blocks.MANGROVE_PROPAGULE) {
         return blockState.get(PropaguleBlock.HANGING) ? 15 : -1;
      } else if (var1 == Blocks.OPEN_EYEBLOSSOM || var1 == Blocks.CLOSED_EYEBLOSSOM) {
         return 16;
      } else if (var1 instanceof CropBlock) {
         return 9;
      } else if (var1 instanceof LeavesBlock) {
         return 13;
      } else {
         return !isGrass(blockState) && !blockState.isOf(Blocks.DIRT) ? -1 : 14;
      }
   }

   private static Boolean eyeblossomsOpen(ClientWorld clientWorld) {
      if (clientWorld.getRegistryKey() == World.OVERWORLD && !clientWorld.getDimension().hasFixedTime()) {
         int var1 = Math.floorMod(clientWorld.getTimeOfDay(), 24000);
         boolean var2 = var1 >= 12600 && var1 < 23401;
         int var3 = var2 ? var1 - 12600 : Math.floorMod(var1 - 23401, 24000);
         return var3 >= 4000 ? var2 : null;
      } else {
         return null;
      }
   }

   private static boolean owed(BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, BlockState blockState, int l, boolean bl) {
      if (l == 14) {
         return soilOwed(blockReader, i, j, k, blockState);
      } else {
         if (bl) {
            if (l == 0) {
               return blockReader.state(i, j + 1, k).isOf(Blocks.WATER) && column(blockReader, i, j, k, Blocks.KELP_PLANT, 3) < 3;
            }

            if (l == 11) {
               return isBud(blockState);
            }

            if (l == 1) {
               return false;
            }
         }

         return growthOwed(blockReader, i, j, k, blockState);
      }
   }

   private static boolean growthOwed(BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, BlockState blockState) {
      Block var5 = blockState.getBlock();
      if (var5 == Blocks.KELP) {
         return blockState.get(Properties.AGE_25) < 25 && blockReader.state(i, j + 1, k).isOf(Blocks.WATER);
      } else if (var5 == Blocks.CAVE_VINES || var5 == Blocks.WEEPING_VINES) {
         return blockState.get(Properties.AGE_25) < 25 && blockReader.state(i, j - 1, k).isAir();
      } else if (var5 == Blocks.BAMBOO) {
         return blockState.get(Properties.STAGE) == 0
            && blockReader.state(i, j + 1, k).isAir()
            && bambooBelow(blockReader, i, j, k) + 1 < 16
            && blockReader.light(i, j + 1, k) >= 9;
      } else if (var5 == Blocks.SUGAR_CANE) {
         return blockReader.state(i, j + 1, k).isAir() && column(blockReader, i, j, k, Blocks.SUGAR_CANE, 3) < 3;
      } else if (var5 == Blocks.CACTUS) {
         return blockReader.state(i, j + 1, k).isAir() && (blockState.get(Properties.AGE_15) < 15 || column(blockReader, i, j, k, Blocks.CACTUS, 3) < 3);
      } else if (var5 == Blocks.COCOA) {
         return blockState.get(Properties.AGE_2) < 2;
      } else if (var5 == Blocks.NETHER_WART) {
         return blockState.get(Properties.AGE_3) < 3;
      } else if (var5 == Blocks.SWEET_BERRY_BUSH) {
         return blockState.get(Properties.AGE_3) < 3 && blockReader.light(i, j + 1, k) >= 9;
      } else if (var5 == Blocks.PUMPKIN_STEM || var5 == Blocks.MELON_STEM) {
         return blockState.get(Properties.AGE_7) < 7 && blockReader.light(i, j, k) >= 9;
      } else if (var5 instanceof CropBlock var6) {
         return !var6.isMature(blockState) && blockReader.light(i, j, k) >= 9;
      } else if (var5 == Blocks.VINE) {
         return blockReader.state(i, j - 1, k).isAir()
            && (blockState.get(Properties.NORTH) || blockState.get(Properties.EAST) || blockState.get(Properties.SOUTH) || blockState.get(Properties.WEST));
      } else {
         return !(var5 instanceof LeavesBlock)
            ? amethystOwed(blockReader, i, j, k, blockState)
            : blockState.contains(Properties.DISTANCE_1_7)
               && blockState.contains(Properties.PERSISTENT)
               && blockState.get(Properties.DISTANCE_1_7) == 7
               && !blockState.get(Properties.PERSISTENT);
      }
   }

   private static boolean amethystOwed(BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, BlockState blockState) {
      if (blockState.isOf(Blocks.BUDDING_AMETHYST)) {
         for (Direction var8 : DIRECTIONS) {
            BlockState var9 = blockReader.state(i + var8.getOffsetX(), j + var8.getOffsetY(), k + var8.getOffsetZ());
            if (var9.isAir()) {
               return true;
            }

            if (var9.isOf(Blocks.WATER) && var9.getFluidState().getLevel() == 8) {
               return true;
            }
         }

         return false;
      } else if (isBud(blockState) && blockState.contains(Properties.FACING)) {
         Direction var5 = blockState.get(Properties.FACING);
         return blockReader.state(i - var5.getOffsetX(), j - var5.getOffsetY(), k - var5.getOffsetZ()).isOf(Blocks.BUDDING_AMETHYST);
      } else {
         return false;
      }
   }

   private static int column(BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, Block block, int l) {
      int var6 = 1;

      while (var6 < l && blockReader.state(i, j - var6, k).isOf(block)) {
         var6++;
      }

      return var6;
   }

   private static int bambooBelow(BetterSusChunkModule.BlockReader blockReader, int i, int j, int k) {
      int var4 = 0;

      while (var4 < 16 && blockReader.state(i, j - var4 - 1, k).isOf(Blocks.BAMBOO)) {
         var4++;
      }

      return var4;
   }

   private static boolean soilOwed(BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, BlockState blockState) {
      BlockState var5 = blockReader.state(i, j + 1, k);
      if (isGrass(blockState)) {
         return smothers(var5);
      } else if (blockState.isOf(Blocks.DIRT) && !smothers(var5) && !var5.getFluidState().isIn(FluidTags.WATER)) {
         for (int var6 = -1; var6 <= 1; var6++) {
            for (int var7 = -1; var7 <= 1; var7++) {
               for (int var8 = j - 1; var8 <= j + 3; var8++) {
                  if ((var6 != 0 || var7 != 0 || var8 != j)
                     && isGrass(blockReader.state(i + var6, var8, k + var7))
                     && !smothers(blockReader.state(i + var6, var8 + 1, k + var7))
                     && blockReader.light(i + var6, var8 + 1, k + var7) >= 9) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean smothers(BlockState blockState) {
      if (blockState.isOf(Blocks.SNOW) && blockState.get(SnowBlock.LAYERS) == 1) {
         return false;
      } else {
         return blockState.getFluidState().getLevel() == 8
            ? true
            : ChunkLightProvider.getRealisticOpacity(Blocks.GRASS_BLOCK.getDefaultState(), blockState, Direction.UP, blockState.getOpacity()) >= 15;
      }
   }

   private static boolean surfaceOwed(
      ClientWorld clientWorld, BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, int l, boolean bl, boolean[] bls
   ) {
      boolean var8 = bls[17] && (!bl || !blockReader.state(i, j - 1, k).isOf(Blocks.DEEPSLATE));
      return var8 && snowOwed(clientWorld, blockReader, i, j, k, l) || bls[18] && iceOwed(clientWorld, blockReader, i, j, k, l);
   }

   private static boolean snowOwed(ClientWorld clientWorld, BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, int l) {
      if (!clientWorld.isOutOfHeightLimit(j) && blockReader.state(i, j, k).isAir()) {
         if (!holdsSnow(clientWorld, blockReader.state(i, j - 1, k), new BlockPos(i, j - 1, k))) {
            return false;
         } else {
            BlockPos var6 = new BlockPos(i, j, k);
            return clientWorld.getBiome(var6).value().getPrecipitation(var6, l) != Precipitation.SNOW ? false : calmLight(clientWorld, blockReader, i, j, k);
         }
      } else {
         return false;
      }
   }

   private static boolean holdsSnow(ClientWorld clientWorld, BlockState blockState, BlockPos blockPos) {
      if (blockState.isIn(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON)) {
         return false;
      } else if (blockState.isIn(BlockTags.SNOW_LAYER_CAN_SURVIVE_ON)) {
         return true;
      } else {
         return blockState.isOf(Blocks.SNOW) && blockState.get(SnowBlock.LAYERS) == 8
            ? true
            : Block.isFaceFullSquare(blockState.getCollisionShape(clientWorld, blockPos), Direction.UP);
      }
   }

   private static boolean iceOwed(ClientWorld clientWorld, BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, int l) {
      int var6 = j - 1;
      if (clientWorld.isOutOfHeightLimit(var6)) {
         return false;
      } else {
         BlockState var7 = blockReader.state(i, var6, k);
         if (var7.getBlock() instanceof FluidBlock && var7.getFluidState().getFluid() == Fluids.WATER) {
            if (!clientWorld.isChunkLoaded(i - 1 >> 4, k >> 4)
               || !clientWorld.isChunkLoaded(i + 1 >> 4, k >> 4)
               || !clientWorld.isChunkLoaded(i >> 4, k - 1 >> 4)
               || !clientWorld.isChunkLoaded(i >> 4, k + 1 >> 4)) {
               return false;
            } else if (isWater(blockReader, i - 1, var6, k)
               && isWater(blockReader, i + 1, var6, k)
               && isWater(blockReader, i, var6, k - 1)
               && isWater(blockReader, i, var6, k + 1)) {
               return false;
            } else {
               return clientWorld.getBiome(new BlockPos(i, j, k)).value().doesNotSnow(new BlockPos(i, var6, k), l)
                  ? false
                  : calmLight(clientWorld, blockReader, i, var6, k);
            }
         } else {
            return false;
         }
      }
   }

   private static boolean isWater(BetterSusChunkModule.BlockReader blockReader, int i, int j, int k) {
      return blockReader.state(i, j, k).getFluidState().isIn(FluidTags.WATER);
   }

   private static boolean calmLight(ClientWorld clientWorld, BetterSusChunkModule.BlockReader blockReader, int i, int j, int k) {
      byte var5 = 5;

      for (int var6 = i - var5 >> 4; var6 <= i + var5 >> 4; var6++) {
         for (int var7 = k - var5 >> 4; var7 <= k + var5 >> 4; var7++) {
            if (!clientWorld.isChunkLoaded(var6, var7)) {
               return false;
            }
         }
      }

      for (int var12 = -var5; var12 <= var5; var12++) {
         int var13 = var5 - Math.abs(var12);

         for (int var8 = -var13; var8 <= var13; var8++) {
            int var9 = var13 - Math.abs(var8);

            for (int var10 = -var9; var10 <= var9; var10++) {
               int var11 = blockReader.state(i + var12, j + var8, k + var10).getLuminance();
               if (var11 != 0 && var11 - Math.abs(var12) - Math.abs(var8) - Math.abs(var10) >= 10) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private static boolean hasHole(ClientWorld clientWorld, ChunkPos chunkPos) {
      BetterSusChunkModule.BlockReader var2 = new BetterSusChunkModule.BlockReader(clientWorld);
      int var3 = clientWorld.getBottomY();
      int var4 = var3 + clientWorld.getHeight();
      int var5 = chunkPos.getStartX();
      int var6 = chunkPos.getStartZ();

      for (int var7 = var5; var7 < var5 + 16; var7++) {
         for (int var8 = var6; var8 < var6 + 16; var8++) {
            int var9 = 0;
            int var10 = 0;
            int var11 = 0;

            for (int var12 = var3; var12 < var4; var12++) {
               if (singleShaft(clientWorld, var2, var7, var12, var8)) {
                  var9++;
               } else {
                  if (shaftEnds(clientWorld, var2, var7, var12, var8, var9)) {
                     return true;
                  }

                  var9 = 0;
               }

               if (wideShaft(clientWorld, var2, var7, var12, var8, 1, 0)) {
                  var10++;
               } else {
                  if (shaftEnds(clientWorld, var2, var7, var12, var8, var10)) {
                     return true;
                  }

                  var10 = 0;
               }

               if (wideShaft(clientWorld, var2, var7, var12, var8, 0, 1)) {
                  var11++;
               } else {
                  if (shaftEnds(clientWorld, var2, var7, var12, var8, var11)) {
                     return true;
                  }

                  var11 = 0;
               }
            }
         }
      }

      return false;
   }

   private static boolean singleShaft(ClientWorld clientWorld, BetterSusChunkModule.BlockReader blockReader, int i, int j, int k) {
      return open(clientWorld, blockReader, i, j, k)
         && !open(clientWorld, blockReader, i - 1, j, k)
         && !open(clientWorld, blockReader, i + 1, j, k)
         && !open(clientWorld, blockReader, i, j, k - 1)
         && !open(clientWorld, blockReader, i, j, k + 1);
   }

   private static boolean wideShaft(ClientWorld clientWorld, BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, int l, int m) {
      for (int var7 = 0; var7 < 3; var7++) {
         if (!open(clientWorld, blockReader, i + l * var7, j, k + m * var7)) {
            return false;
         }
      }

      if (!open(clientWorld, blockReader, i - l, j, k - m) && !open(clientWorld, blockReader, i + l * 3, j, k + m * 3)) {
         for (int var10 = 0; var10 < 3; var10++) {
            int var8 = i + l * var10;
            int var9 = k + m * var10;
            if (open(clientWorld, blockReader, var8 - m, j, var9 - l) || open(clientWorld, blockReader, var8 + m, j, var9 + l)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean shaftEnds(ClientWorld clientWorld, BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, int l) {
      return l >= 2 && open(clientWorld, blockReader, i, j, k) && !open(clientWorld, blockReader, i, j - l - 1, k);
   }

   private static boolean open(ClientWorld clientWorld, BetterSusChunkModule.BlockReader blockReader, int i, int j, int k) {
      return blockReader.state(i, j, k).getCollisionShape(clientWorld, BlockPos.ORIGIN).isEmpty();
   }

   private static boolean isBud(BlockState blockState) {
      Block var1 = blockState.getBlock();
      return var1 == Blocks.SMALL_AMETHYST_BUD || var1 == Blocks.MEDIUM_AMETHYST_BUD || var1 == Blocks.LARGE_AMETHYST_BUD;
   }

   private static boolean isGrass(BlockState blockState) {
      return blockState.isOf(Blocks.GRASS_BLOCK) || blockState.isOf(Blocks.MYCELIUM);
   }

   private static boolean donutServer() {
      ClientPlayNetworkHandler var0 = MinecraftClient.getInstance().getNetworkHandler();
      if (var0 == null) {
         return false;
      } else {
         String var1 = var0.getBrand();
         return var1 != null && var1.contains("DonutFolia");
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent packetAppliedEvent) {
      if (packetAppliedEvent.packet instanceof ChunkDataS2CPacket var3) {
         this.scheduleScan(ChunkPos.toLong(var3.getChunkX(), var3.getChunkZ()));
      } else if (packetAppliedEvent.packet instanceof GameJoinS2CPacket) {
         this.resetState();
         this.worldKey = null;
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent blockUpdateEvent) {
      ClientWorld var2 = MinecraftClient.getInstance().world;
      if (var2 != null && this.counted[0] && blockUpdateEvent.newState.isOf(Blocks.KELP) && donutServer()) {
         int var3 = blockUpdateEvent.newState.get(Properties.AGE_25);
         if (this.seenKelp.add(blockUpdateEvent.pos.asLong())) {
            if (var3 > 0 && var3 < 25) {
               if (var2.getBlockState(blockUpdateEvent.pos.up()).isOf(Blocks.WATER)) {
                  long var4 = ChunkPos.toLong(blockUpdateEvent.pos.getX() >> 4, blockUpdateEvent.pos.getZ() >> 4);
                  if (this.kelpChunks.add(var4)) {
                     this.scheduleScan(var4);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientWorld var3 = var2.world;
      if (var3 != null && var2.player != null) {
         RegistryKey var4 = var3.getRegistryKey();
         if (var4 != this.worldKey) {
            this.resetState();
            this.worldKey = var4;
         }

         if (this.dirty) {
            this.dirty = false;
            this.rescanLoadedChunks();
         }

         this.pruneDistant(var2);
         if (++this.tickCounter >= 20) {
            this.tickCounter = 0;
            this.rebuildDisplaySets(var3);
         }
      }
   }

   private void pruneDistant(MinecraftClient minecraftClient) {
      int var2 = minecraftClient.options.getClampedViewDistance() + 2 * this.simDistance.getInt() + 2;
      int var3 = minecraftClient.player.getBlockX() >> 4;
      int var4 = minecraftClient.player.getBlockZ() >> 4;
      Object var5 = this.lock;
      synchronized (var5) {
         this.distant.clear();
         LongIterator var7 = this.records.keySet().iterator();

         while (var7.hasNext()) {
            long var8 = var7.nextLong();
            if (Math.abs(ChunkPos.getPackedX(var8) - var3) > var2 || Math.abs(ChunkPos.getPackedZ(var8) - var4) > var2) {
               this.distant.add(var8);
            }
         }

         for (int var12 = 0; var12 < this.distant.size(); var12++) {
            this.removeRecord(this.distant.getLong(var12));
         }
      }

      this.kelpChunks.removeIf(long_ -> Math.abs(ChunkPos.getPackedX(long_) - var3) > var2 || Math.abs(ChunkPos.getPackedZ(long_) - var4) > var2);
      this.seenKelp.removeIf(long_ -> Math.abs((BlockPos.unpackLongX(long_) >> 4) - var3) > var2 || Math.abs((BlockPos.unpackLongZ(long_) >> 4) - var4) > var2);
   }

   private void rebuildDisplaySets(ClientWorld clientWorld) {
      boolean var2 = this.holeFilter.get();
      int var3 = this.sensitivity.getInt();
      int var4 = this.holeDistance.getInt();
      int var5 = 2 * this.simDistance.getInt() + 2;
      LongOpenHashSet var6 = new LongOpenHashSet();
      Object var7 = this.lock;
      synchronized (var7) {
         LongIterator var9 = this.scanned.iterator();

         while (var9.hasNext()) {
            long var11 = var9.nextLong();
            int var13 = ChunkPos.getPackedX(var11);
            int var10;
            if (clientWorld.isChunkLoaded(var13, var10 = ChunkPos.getPackedZ(var11))
               && !this.clearCoverage.containsKey(var11)
               && (!var2 || !this.holeNearby(var13, var10, var4))
               && this.support.get(var11) >= var3
               && this.coveredSides(var13, var10, var5) >= 3) {
               var6.add(var11);
            }
         }
      }

      this.primary = var6;
   }

   private boolean holeNearby(int i, int j, int k) {
      for (int var4 = -k; var4 <= k; var4++) {
         for (int var5 = -k; var5 <= k; var5++) {
            BetterSusChunkModule.Record var6 = this.records.get(ChunkPos.toLong(i + var4, j + var5));
            if (var6 != null && var6.hole()) {
               return true;
            }
         }
      }

      return false;
   }

   private int coveredSides(int i, int j, int k) {
      int var4 = 0;

      for (int[] var8 : SIDES) {
         if (this.stripCleared(i, j, var8[0], var8[1], k)) {
            var4++;
         }
      }

      return var4;
   }

   private boolean stripCleared(int i, int j, int k, int l, int m) {
      for (int var6 = 1; var6 <= m; var6++) {
         for (int var7 = -m; var7 <= m; var7++) {
            long var8 = ChunkPos.toLong(i + k * var6 + l * var7, j + l * var6 + k * var7);
            if (this.clearCoverage.containsKey(var8)) {
               return true;
            }
         }
      }

      return false;
   }

   public LongSet flaggedChunks() {
      return LongSets.unmodifiable(this.primary);
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      if (MinecraftClient.getInstance().world != null) {
         this.primaryOverlay.drawTiles(this.primary, this.chunkColor.argbWithAlpha(100), this.chunkColor.argbWithAlpha(255), render3DEvent.partialTick);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class BlockReader {
      private final ClientWorld level;
      private WorldChunk chunk;

      private BlockReader(ClientWorld clientWorld) {
         this.level = clientWorld;
      }

      private BlockState state(int i, int j, int k) {
         if (this.level.isOutOfHeightLimit(j)) {
            return Blocks.VOID_AIR.getDefaultState();
         } else {
            WorldChunk var4 = this.chunkAt(i >> 4, k >> 4);
            if (var4 == null) {
               return Blocks.VOID_AIR.getDefaultState();
            } else {
               ChunkSection var5 = var4.getSectionArray()[var4.getSectionIndex(j)];
               return var5 == null ? Blocks.VOID_AIR.getDefaultState() : var5.getBlockState(i & 15, j & 15, k & 15);
            }
         }
      }

      private boolean emptySection(int i, int j, int k) {
         WorldChunk var4 = this.chunkAt(i >> 4, k >> 4);
         if (var4 == null) {
            return true;
         } else {
            ChunkSection var5 = var4.getSectionArray()[var4.getSectionIndex(j)];
            return var5 == null || var5.isEmpty();
         }
      }

      private WorldChunk chunkAt(int i, int j) {
         if (this.chunk != null && this.chunk.getPos().x == i && this.chunk.getPos().z == j) {
            return this.chunk;
         } else {
            WorldChunk var3 = this.level.getChunkManager().getWorldChunk(i, j);
            if (var3 != null) {
               this.chunk = var3;
            }

            return var3;
         }
      }

      private int light(int i, int j, int k) {
         int var4 = Math.max(this.rawLight(LightType.BLOCK, i, j, k), this.rawLight(LightType.SKY, i, j, k));
         if (var4 < 15 && this.level.getDimension().hasSkyLight()) {
            return this.openSky(i, j, k) ? 15 : var4;
         } else {
            return var4;
         }
      }

      private int rawLight(LightType lightType, int i, int j, int k) {
         ChunkNibbleArray var5 = this.level.getLightingProvider().get(lightType).getLightSection(ChunkSectionPos.from(i >> 4, j >> 4, k >> 4));
         return var5 != null && !var5.isArrayUninitialized() ? var5.get(i & 15, j & 15, k & 15) : 0;
      }

      private boolean openSky(int i, int j, int k) {
         int var4 = this.level.getTopYInclusive();

         for (int var5 = j + 1; var5 <= var4; var5++) {
            if (this.emptySection(i, var5, k)) {
               var5 |= 15;
            } else if (this.state(i, var5, k).getOpacity() > 0) {
               return false;
            }
         }

         return true;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Record(boolean clears, int radius, boolean hole, int weight) {
   }

   @Environment(EnvType.CLIENT)
   private record Source(boolean clears, boolean hole, int weight) {
   }
}
