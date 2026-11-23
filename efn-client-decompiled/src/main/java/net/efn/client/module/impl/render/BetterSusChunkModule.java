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
import net.minecraft.world.biome.Biome;
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
   private final Long2ObjectOpenHashMap<BetterSusChunkModule.Record> records = new Long2ObjectOpenHashMap();
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

   private BooleanSetting evidence(String var1, String var2) {
      return this.addSetting(var1, new BooleanSetting(var2, true));
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
      this.executor = Executors.newSingleThreadExecutor(var0 -> {
         Thread var1x = new Thread(var0, "EFN Better Sus Chunk");
         var1x.setDaemon(true);
         return var1x;
      });
      ClientWorld var1 = MinecraftClient.method_1551().field_1687;
      this.worldKey = var1 == null ? null : var1.method_27983();
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
      MinecraftClient var1 = MinecraftClient.method_1551();
      ClientWorld var2 = var1.field_1687;
      if (this.isEnabled() && var1.field_1724 != null && var2 != null) {
         this.resetState();
         int var3 = var1.field_1690.method_38521() + 3;
         ChunkPos var4 = var1.field_1724.method_31476();

         for (int var5 = var4.field_9181 - var3; var5 <= var4.field_9181 + var3; var5++) {
            for (int var6 = var4.field_9180 - var3; var6 <= var4.field_9180 + var3; var6++) {
               if (var2.method_8393(var5, var6)) {
                  this.scheduleScan(ChunkPos.method_8331(var5, var6));
               }
            }
         }
      }
   }

   private void scheduleScan(long var1) {
      ExecutorService var3 = this.executor;
      if (var3 != null && !var3.isShutdown()) {
         if (this.pending.add(var1)) {
            int var4 = this.generation;
            var3.submit(() -> this.runScan(var1, var4));
         }
      }
   }

   private void runScan(long var1, int var3) {
      this.pending.remove(var1);
      if (var3 == this.generation && this.isEnabled()) {
         ClientWorld var5 = MinecraftClient.method_1551().field_1687;
         if (var5 != null) {
            int var6 = ChunkPos.method_8325(var1);
            int var4;
            if (var5.method_8393(var6, var4 = ChunkPos.method_8332(var1))) {
               WorldChunk var7 = var5.method_8497(var6, var4);
               if (var7 != null) {
                  this.rescan(var5, var1, var7, var3);
               }
            }
         }
      }
   }

   private void rescan(ClientWorld var1, long var2, WorldChunk var4, int var5) {
      BetterSusChunkModule.Record var6 = this.removeRecord(var2);
      BetterSusChunkModule.Source var7 = this.scanChunk(var1, var2, var4);
      Object var8 = this.lock;
      synchronized (var8) {
         if (var5 == this.generation) {
            BetterSusChunkModule.Record var12 = new BetterSusChunkModule.Record(
               var7.clears() || var6 != null && var6.clears(),
               this.simDistance.getInt(),
               var7.hole() || var6 != null && var6.hole(),
               Math.max(var7.weight(), var6 == null ? 0 : var6.weight())
            );
            this.scanned.add(var2);
            this.records.put(var2, var12);
            if (var12.clears()) {
               int var13 = ChunkPos.method_8325(var2);
               int var14 = ChunkPos.method_8332(var2);
               int var15 = 2 * var12.radius() + 2;

               for (int var11 = -var12.radius(); var11 <= var12.radius(); var11++) {
                  for (int var10 = -var12.radius(); var10 <= var12.radius(); var10++) {
                     this.clearCoverage.addTo(ChunkPos.method_8331(var13 + var11, var14 + var10), 1);
                  }
               }

               for (int var19 = -var15; var19 <= var15; var19++) {
                  for (int var18 = -var15; var18 <= var15; var18++) {
                     this.support.addTo(ChunkPos.method_8331(var13 + var19, var14 + var18), var12.weight());
                  }
               }
            }
         }
      }
   }

   private BetterSusChunkModule.Record removeRecord(long var1) {
      Object var3 = this.lock;
      synchronized (var3) {
         BetterSusChunkModule.Record var7 = (BetterSusChunkModule.Record)this.records.remove(var1);
         this.scanned.remove(var1);
         if (var7 != null && var7.clears()) {
            int var8 = ChunkPos.method_8325(var1);
            int var9 = ChunkPos.method_8332(var1);
            int var10 = 2 * var7.radius() + 2;

            for (int var6 = -var7.radius(); var6 <= var7.radius(); var6++) {
               for (int var5 = -var7.radius(); var5 <= var7.radius(); var5++) {
                  subtract(this.clearCoverage, ChunkPos.method_8331(var8 + var6, var9 + var5), 1);
               }
            }

            for (int var14 = -var10; var14 <= var10; var14++) {
               for (int var13 = -var10; var13 <= var10; var13++) {
                  subtract(this.support, ChunkPos.method_8331(var8 + var14, var9 + var13), var7.weight());
               }
            }

            return var7;
         } else {
            return var7;
         }
      }
   }

   private static void subtract(Long2IntOpenHashMap var0, long var1, int var3) {
      if (var0.containsKey(var1)) {
         int var4 = var0.get(var1);
         if (var4 <= var3) {
            var0.remove(var1);
         } else {
            var0.put(var1, var4 - var3);
         }
      }
   }

   private BetterSusChunkModule.Source scanChunk(ClientWorld var1, long var2, WorldChunk var4) {
      boolean var5 = donutServer();
      boolean[] var6 = this.counted;
      int var7 = var5 && var6[0] && this.kelpChunks.contains(var2) ? 1 : 0;
      BetterSusChunkModule.BlockReader var8 = new BetterSusChunkModule.BlockReader(var1);
      ChunkPos var9 = var4.method_12004();
      int var10 = var9.method_8326();
      int var11 = var9.method_8328();
      Boolean var12 = eyeblossomsOpen(var1);
      int var13 = 0;
      int var14 = 0;
      ChunkSection[] var15 = var4.method_12006();

      for (int var16 = 0; var16 < var15.length; var16++) {
         ChunkSection var17 = var15[var16];
         if (var17 != null && !var17.method_38292() && var17.method_19523(var1x -> kindOf(var1x, var6) >= 0)) {
            int var18 = var4.method_31607() + (var16 << 4);

            for (int var19 = 0; var19 < 16; var19++) {
               for (int var20 = 0; var20 < 16; var20++) {
                  for (int var21 = 0; var21 < 16; var21++) {
                     BlockState var22 = var17.method_12254(var21, var19, var20);
                     int var23 = kindOf(var22, var6);
                     if (var23 >= 0) {
                        if (var23 == 15) {
                           if ((Integer)var22.method_11654(PropaguleBlock.field_37588) < 4) {
                              var7++;
                           }
                        } else if (var23 == 16) {
                           if (var12 != null) {
                              if (var22.method_27852(Blocks.field_55060) == var12) {
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

      if ((var6[17] || var6[18]) && var4.method_39295(Type.field_13197)) {
         Heightmap var24 = var4.method_12032(Type.field_13197);
         int var26 = var1.method_8615();

         for (int var27 = 0; var27 < 16; var27++) {
            for (int var28 = 0; var28 < 16; var28++) {
               int var29 = var24.method_12603(var28, var27);
               if (surfaceOwed(var1, var8, var10 + var28, var29, var11 + var27, var26, var5, var6)) {
                  var7++;
               }
            }
         }
      }

      boolean var25 = this.holeFilter.get() && hasHole(var1, var9);
      return new BetterSusChunkModule.Source(var7 > 0, var25, var7);
   }

   private static int kindOf(BlockState var0, boolean[] var1) {
      int var2 = classify(var0);
      return var2 >= 0 && var1[var2] ? var2 : -1;
   }

   private static int classify(BlockState var0) {
      Block var1 = var0.method_26204();
      if (var1 == Blocks.field_9993) {
         return 0;
      } else if (var1 == Blocks.field_28675) {
         return 1;
      } else if (var1 == Blocks.field_22123) {
         return 2;
      } else if (var1 == Blocks.field_10211) {
         return 3;
      } else if (var1 == Blocks.field_10424) {
         return 4;
      } else if (var1 == Blocks.field_10029) {
         return 5;
      } else if (var1 == Blocks.field_10302) {
         return 6;
      } else if (var1 == Blocks.field_16999) {
         return 7;
      } else if (var1 == Blocks.field_9974) {
         return 8;
      } else if (var1 == Blocks.field_46286 || var1 == Blocks.field_46287) {
         return 10;
      } else if (var1 == Blocks.field_10597) {
         return 12;
      } else if (var1 == Blocks.field_27160 || var1 == Blocks.field_27161 || isBud(var0)) {
         return 11;
      } else if (var1 == Blocks.field_37544) {
         return var0.method_11654(PropaguleBlock.field_37591) ? 15 : -1;
      } else if (var1 == Blocks.field_55060 || var1 == Blocks.field_55061) {
         return 16;
      } else if (var1 instanceof CropBlock) {
         return 9;
      } else if (var1 instanceof LeavesBlock) {
         return 13;
      } else {
         return !isGrass(var0) && !var0.method_27852(Blocks.field_10566) ? -1 : 14;
      }
   }

   private static Boolean eyeblossomsOpen(ClientWorld var0) {
      if (var0.method_27983() == World.field_25179 && !var0.method_8597().comp_5219()) {
         int var1 = Math.floorMod(var0.method_8532(), 24000);
         boolean var2 = var1 >= 12600 && var1 < 23401;
         int var3 = var2 ? var1 - 12600 : Math.floorMod(var1 - 23401, 24000);
         return var3 >= 4000 ? var2 : null;
      } else {
         return null;
      }
   }

   private static boolean owed(BetterSusChunkModule.BlockReader var0, int var1, int var2, int var3, BlockState var4, int var5, boolean var6) {
      if (var5 == 14) {
         return soilOwed(var0, var1, var2, var3, var4);
      } else {
         if (var6) {
            if (var5 == 0) {
               return var0.state(var1, var2 + 1, var3).method_27852(Blocks.field_10382) && column(var0, var1, var2, var3, Blocks.field_10463, 3) < 3;
            }

            if (var5 == 11) {
               return isBud(var4);
            }

            if (var5 == 1) {
               return false;
            }
         }

         return growthOwed(var0, var1, var2, var3, var4);
      }
   }

   private static boolean growthOwed(BetterSusChunkModule.BlockReader var0, int var1, int var2, int var3, BlockState var4) {
      Block var5 = var4.method_26204();
      if (var5 == Blocks.field_9993) {
         return (Integer)var4.method_11654(Properties.field_12517) < 25 && var0.state(var1, var2 + 1, var3).method_27852(Blocks.field_10382);
      } else if (var5 == Blocks.field_28675 || var5 == Blocks.field_22123) {
         return (Integer)var4.method_11654(Properties.field_12517) < 25 && var0.state(var1, var2 - 1, var3).method_26215();
      } else if (var5 == Blocks.field_10211) {
         return (Integer)var4.method_11654(Properties.field_12549) == 0
            && var0.state(var1, var2 + 1, var3).method_26215()
            && bambooBelow(var0, var1, var2, var3) + 1 < 16
            && var0.light(var1, var2 + 1, var3) >= 9;
      } else if (var5 == Blocks.field_10424) {
         return var0.state(var1, var2 + 1, var3).method_26215() && column(var0, var1, var2, var3, Blocks.field_10424, 3) < 3;
      } else if (var5 == Blocks.field_10029) {
         return var0.state(var1, var2 + 1, var3).method_26215()
            && ((Integer)var4.method_11654(Properties.field_12498) < 15 || column(var0, var1, var2, var3, Blocks.field_10029, 3) < 3);
      } else if (var5 == Blocks.field_10302) {
         return (Integer)var4.method_11654(Properties.field_12556) < 2;
      } else if (var5 == Blocks.field_9974) {
         return (Integer)var4.method_11654(Properties.field_12497) < 3;
      } else if (var5 == Blocks.field_16999) {
         return (Integer)var4.method_11654(Properties.field_12497) < 3 && var0.light(var1, var2 + 1, var3) >= 9;
      } else if (var5 == Blocks.field_46286 || var5 == Blocks.field_46287) {
         return (Integer)var4.method_11654(Properties.field_12550) < 7 && var0.light(var1, var2, var3) >= 9;
      } else if (var5 instanceof CropBlock var6) {
         return !var6.method_9825(var4) && var0.light(var1, var2, var3) >= 9;
      } else if (var5 == Blocks.field_10597) {
         return var0.state(var1, var2 - 1, var3).method_26215()
            && (
               (Boolean)var4.method_11654(Properties.field_12489)
                  || (Boolean)var4.method_11654(Properties.field_12487)
                  || (Boolean)var4.method_11654(Properties.field_12540)
                  || (Boolean)var4.method_11654(Properties.field_12527)
            );
      } else {
         return !(var5 instanceof LeavesBlock)
            ? amethystOwed(var0, var1, var2, var3, var4)
            : var4.method_28498(Properties.field_12541)
               && var4.method_28498(Properties.field_12514)
               && (Integer)var4.method_11654(Properties.field_12541) == 7
               && !(Boolean)var4.method_11654(Properties.field_12514);
      }
   }

   private static boolean amethystOwed(BetterSusChunkModule.BlockReader var0, int var1, int var2, int var3, BlockState var4) {
      if (var4.method_27852(Blocks.field_27160)) {
         for (Direction var8 : DIRECTIONS) {
            BlockState var9 = var0.state(var1 + var8.method_10148(), var2 + var8.method_10164(), var3 + var8.method_10165());
            if (var9.method_26215()) {
               return true;
            }

            if (var9.method_27852(Blocks.field_10382) && var9.method_26227().method_15761() == 8) {
               return true;
            }
         }

         return false;
      } else if (isBud(var4) && var4.method_28498(Properties.field_12525)) {
         Direction var5 = (Direction)var4.method_11654(Properties.field_12525);
         return var0.state(var1 - var5.method_10148(), var2 - var5.method_10164(), var3 - var5.method_10165()).method_27852(Blocks.field_27160);
      } else {
         return false;
      }
   }

   private static int column(BetterSusChunkModule.BlockReader var0, int var1, int var2, int var3, Block var4, int var5) {
      int var6 = 1;

      while (var6 < var5 && var0.state(var1, var2 - var6, var3).method_27852(var4)) {
         var6++;
      }

      return var6;
   }

   private static int bambooBelow(BetterSusChunkModule.BlockReader var0, int var1, int var2, int var3) {
      int var4 = 0;

      while (var4 < 16 && var0.state(var1, var2 - var4 - 1, var3).method_27852(Blocks.field_10211)) {
         var4++;
      }

      return var4;
   }

   private static boolean soilOwed(BetterSusChunkModule.BlockReader var0, int var1, int var2, int var3, BlockState var4) {
      BlockState var5 = var0.state(var1, var2 + 1, var3);
      if (isGrass(var4)) {
         return smothers(var5);
      } else if (var4.method_27852(Blocks.field_10566) && !smothers(var5) && !var5.method_26227().method_15767(FluidTags.field_15517)) {
         for (int var6 = -1; var6 <= 1; var6++) {
            for (int var7 = -1; var7 <= 1; var7++) {
               for (int var8 = var2 - 1; var8 <= var2 + 3; var8++) {
                  if ((var6 != 0 || var7 != 0 || var8 != var2)
                     && isGrass(var0.state(var1 + var6, var8, var3 + var7))
                     && !smothers(var0.state(var1 + var6, var8 + 1, var3 + var7))
                     && var0.light(var1 + var6, var8 + 1, var3 + var7) >= 9) {
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

   private static boolean smothers(BlockState var0) {
      if (var0.method_27852(Blocks.field_10477) && (Integer)var0.method_11654(SnowBlock.field_11518) == 1) {
         return false;
      } else {
         return var0.method_26227().method_15761() == 8
            ? true
            : ChunkLightProvider.method_20049(Blocks.field_10219.method_9564(), var0, Direction.field_11036, var0.method_26193()) >= 15;
      }
   }

   private static boolean surfaceOwed(
      ClientWorld var0, BetterSusChunkModule.BlockReader var1, int var2, int var3, int var4, int var5, boolean var6, boolean[] var7
   ) {
      boolean var8 = var7[17] && (!var6 || !var1.state(var2, var3 - 1, var4).method_27852(Blocks.field_28888));
      return var8 && snowOwed(var0, var1, var2, var3, var4, var5) || var7[18] && iceOwed(var0, var1, var2, var3, var4, var5);
   }

   private static boolean snowOwed(ClientWorld var0, BetterSusChunkModule.BlockReader var1, int var2, int var3, int var4, int var5) {
      if (!var0.method_31601(var3) && var1.state(var2, var3, var4).method_26215()) {
         if (!holdsSnow(var0, var1.state(var2, var3 - 1, var4), new BlockPos(var2, var3 - 1, var4))) {
            return false;
         } else {
            BlockPos var6 = new BlockPos(var2, var3, var4);
            return ((Biome)var0.method_23753(var6).comp_349()).method_48162(var6, var5) != Precipitation.field_9383
               ? false
               : calmLight(var0, var1, var2, var3, var4);
         }
      } else {
         return false;
      }
   }

   private static boolean holdsSnow(ClientWorld var0, BlockState var1, BlockPos var2) {
      if (var1.method_26164(BlockTags.field_39104)) {
         return false;
      } else if (var1.method_26164(BlockTags.field_39105)) {
         return true;
      } else {
         return var1.method_27852(Blocks.field_10477) && var1.method_11654(SnowBlock.field_11518) == 8
            ? true
            : Block.method_9501(var1.method_26220(var0, var2), Direction.field_11036);
      }
   }

   private static boolean iceOwed(ClientWorld var0, BetterSusChunkModule.BlockReader var1, int var2, int var3, int var4, int var5) {
      int var6 = var3 - 1;
      if (var0.method_31601(var6)) {
         return false;
      } else {
         BlockState var7 = var1.state(var2, var6, var4);
         if (var7.method_26204() instanceof FluidBlock && var7.method_26227().method_15772() == Fluids.field_15910) {
            if (!var0.method_8393(var2 - 1 >> 4, var4 >> 4)
               || !var0.method_8393(var2 + 1 >> 4, var4 >> 4)
               || !var0.method_8393(var2 >> 4, var4 - 1 >> 4)
               || !var0.method_8393(var2 >> 4, var4 + 1 >> 4)) {
               return false;
            } else if (isWater(var1, var2 - 1, var6, var4)
               && isWater(var1, var2 + 1, var6, var4)
               && isWater(var1, var2, var6, var4 - 1)
               && isWater(var1, var2, var6, var4 + 1)) {
               return false;
            } else {
               return ((Biome)var0.method_23753(new BlockPos(var2, var3, var4)).comp_349()).method_39927(new BlockPos(var2, var6, var4), var5)
                  ? false
                  : calmLight(var0, var1, var2, var6, var4);
            }
         } else {
            return false;
         }
      }
   }

   private static boolean isWater(BetterSusChunkModule.BlockReader var0, int var1, int var2, int var3) {
      return var0.state(var1, var2, var3).method_26227().method_15767(FluidTags.field_15517);
   }

   private static boolean calmLight(ClientWorld var0, BetterSusChunkModule.BlockReader var1, int var2, int var3, int var4) {
      byte var5 = 5;

      for (int var6 = var2 - var5 >> 4; var6 <= var2 + var5 >> 4; var6++) {
         for (int var7 = var4 - var5 >> 4; var7 <= var4 + var5 >> 4; var7++) {
            if (!var0.method_8393(var6, var7)) {
               return false;
            }
         }
      }

      for (int var12 = -var5; var12 <= var5; var12++) {
         int var13 = var5 - Math.abs(var12);

         for (int var8 = -var13; var8 <= var13; var8++) {
            int var9 = var13 - Math.abs(var8);

            for (int var10 = -var9; var10 <= var9; var10++) {
               int var11 = var1.state(var2 + var12, var3 + var8, var4 + var10).method_26213();
               if (var11 != 0 && var11 - Math.abs(var12) - Math.abs(var8) - Math.abs(var10) >= 10) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private static boolean hasHole(ClientWorld var0, ChunkPos var1) {
      BetterSusChunkModule.BlockReader var2 = new BetterSusChunkModule.BlockReader(var0);
      int var3 = var0.method_31607();
      int var4 = var3 + var0.method_31605();
      int var5 = var1.method_8326();
      int var6 = var1.method_8328();

      for (int var7 = var5; var7 < var5 + 16; var7++) {
         for (int var8 = var6; var8 < var6 + 16; var8++) {
            int var9 = 0;
            int var10 = 0;
            int var11 = 0;

            for (int var12 = var3; var12 < var4; var12++) {
               if (singleShaft(var0, var2, var7, var12, var8)) {
                  var9++;
               } else {
                  if (shaftEnds(var0, var2, var7, var12, var8, var9)) {
                     return true;
                  }

                  var9 = 0;
               }

               if (wideShaft(var0, var2, var7, var12, var8, 1, 0)) {
                  var10++;
               } else {
                  if (shaftEnds(var0, var2, var7, var12, var8, var10)) {
                     return true;
                  }

                  var10 = 0;
               }

               if (wideShaft(var0, var2, var7, var12, var8, 0, 1)) {
                  var11++;
               } else {
                  if (shaftEnds(var0, var2, var7, var12, var8, var11)) {
                     return true;
                  }

                  var11 = 0;
               }
            }
         }
      }

      return false;
   }

   private static boolean singleShaft(ClientWorld var0, BetterSusChunkModule.BlockReader var1, int var2, int var3, int var4) {
      return open(var0, var1, var2, var3, var4)
         && !open(var0, var1, var2 - 1, var3, var4)
         && !open(var0, var1, var2 + 1, var3, var4)
         && !open(var0, var1, var2, var3, var4 - 1)
         && !open(var0, var1, var2, var3, var4 + 1);
   }

   private static boolean wideShaft(ClientWorld var0, BetterSusChunkModule.BlockReader var1, int var2, int var3, int var4, int var5, int var6) {
      for (int var7 = 0; var7 < 3; var7++) {
         if (!open(var0, var1, var2 + var5 * var7, var3, var4 + var6 * var7)) {
            return false;
         }
      }

      if (!open(var0, var1, var2 - var5, var3, var4 - var6) && !open(var0, var1, var2 + var5 * 3, var3, var4 + var6 * 3)) {
         for (int var10 = 0; var10 < 3; var10++) {
            int var8 = var2 + var5 * var10;
            int var9 = var4 + var6 * var10;
            if (open(var0, var1, var8 - var6, var3, var9 - var5) || open(var0, var1, var8 + var6, var3, var9 + var5)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean shaftEnds(ClientWorld var0, BetterSusChunkModule.BlockReader var1, int var2, int var3, int var4, int var5) {
      return var5 >= 2 && open(var0, var1, var2, var3, var4) && !open(var0, var1, var2, var3 - var5 - 1, var4);
   }

   private static boolean open(ClientWorld var0, BetterSusChunkModule.BlockReader var1, int var2, int var3, int var4) {
      return var1.state(var2, var3, var4).method_26220(var0, BlockPos.field_10980).method_1110();
   }

   private static boolean isBud(BlockState var0) {
      Block var1 = var0.method_26204();
      return var1 == Blocks.field_27164 || var1 == Blocks.field_27163 || var1 == Blocks.field_27162;
   }

   private static boolean isGrass(BlockState var0) {
      return var0.method_27852(Blocks.field_10219) || var0.method_27852(Blocks.field_10402);
   }

   private static boolean donutServer() {
      ClientPlayNetworkHandler var0 = MinecraftClient.method_1551().method_1562();
      if (var0 == null) {
         return false;
      } else {
         String var1 = var0.method_52790();
         return var1 != null && var1.contains("DonutFolia");
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent var1) {
      if (var1.packet instanceof ChunkDataS2CPacket var3) {
         this.scheduleScan(ChunkPos.method_8331(var3.method_11523(), var3.method_11524()));
      } else if (var1.packet instanceof GameJoinS2CPacket) {
         this.resetState();
         this.worldKey = null;
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      if (var2 != null && this.counted[0] && var1.newState.method_27852(Blocks.field_9993) && donutServer()) {
         int var3 = (Integer)var1.newState.method_11654(Properties.field_12517);
         if (this.seenKelp.add(var1.pos.method_10063())) {
            if (var3 > 0 && var3 < 25) {
               if (var2.method_8320(var1.pos.method_10084()).method_27852(Blocks.field_10382)) {
                  long var4 = ChunkPos.method_8331(var1.pos.method_10263() >> 4, var1.pos.method_10260() >> 4);
                  if (this.kelpChunks.add(var4)) {
                     this.scheduleScan(var4);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientWorld var3 = var2.field_1687;
      if (var3 != null && var2.field_1724 != null) {
         RegistryKey var4 = var3.method_27983();
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

   private void pruneDistant(MinecraftClient var1) {
      int var2 = var1.field_1690.method_38521() + 2 * this.simDistance.getInt() + 2;
      int var3 = var1.field_1724.method_31477() >> 4;
      int var4 = var1.field_1724.method_31479() >> 4;
      Object var5 = this.lock;
      synchronized (var5) {
         this.distant.clear();
         LongIterator var7 = this.records.keySet().iterator();

         while (var7.hasNext()) {
            long var8 = var7.nextLong();
            if (Math.abs(ChunkPos.method_8325(var8) - var3) > var2 || Math.abs(ChunkPos.method_8332(var8) - var4) > var2) {
               this.distant.add(var8);
            }
         }

         for (int var12 = 0; var12 < this.distant.size(); var12++) {
            this.removeRecord(this.distant.getLong(var12));
         }
      }

      this.kelpChunks.removeIf(var3x -> Math.abs(ChunkPos.method_8325(var3x) - var3) > var2 || Math.abs(ChunkPos.method_8332(var3x) - var4) > var2);
      this.seenKelp
         .removeIf(var3x -> Math.abs((BlockPos.method_10061(var3x) >> 4) - var3) > var2 || Math.abs((BlockPos.method_10083(var3x) >> 4) - var4) > var2);
   }

   private void rebuildDisplaySets(ClientWorld var1) {
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
            int var13 = ChunkPos.method_8325(var11);
            int var10;
            if (var1.method_8393(var13, var10 = ChunkPos.method_8332(var11))
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

   private boolean holeNearby(int var1, int var2, int var3) {
      for (int var4 = -var3; var4 <= var3; var4++) {
         for (int var5 = -var3; var5 <= var3; var5++) {
            BetterSusChunkModule.Record var6 = (BetterSusChunkModule.Record)this.records.get(ChunkPos.method_8331(var1 + var4, var2 + var5));
            if (var6 != null && var6.hole()) {
               return true;
            }
         }
      }

      return false;
   }

   private int coveredSides(int var1, int var2, int var3) {
      int var4 = 0;

      for (int[] var8 : SIDES) {
         if (this.stripCleared(var1, var2, var8[0], var8[1], var3)) {
            var4++;
         }
      }

      return var4;
   }

   private boolean stripCleared(int var1, int var2, int var3, int var4, int var5) {
      for (int var6 = 1; var6 <= var5; var6++) {
         for (int var7 = -var5; var7 <= var5; var7++) {
            long var8 = ChunkPos.method_8331(var1 + var3 * var6 + var4 * var7, var2 + var4 * var6 + var3 * var7);
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
   public void onRender3D(Render3DEvent var1) {
      if (MinecraftClient.method_1551().field_1687 != null) {
         this.primaryOverlay.drawTiles(this.primary, this.chunkColor.argbWithAlpha(100), this.chunkColor.argbWithAlpha(255), var1.partialTick);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class BlockReader {
      private final ClientWorld level;
      private WorldChunk chunk;

      private BlockReader(ClientWorld var1) {
         this.level = var1;
      }

      private BlockState state(int var1, int var2, int var3) {
         if (this.level.method_31601(var2)) {
            return Blocks.field_10243.method_9564();
         } else {
            WorldChunk var4 = this.chunkAt(var1 >> 4, var3 >> 4);
            if (var4 == null) {
               return Blocks.field_10243.method_9564();
            } else {
               ChunkSection var5 = var4.method_12006()[var4.method_31602(var2)];
               return var5 == null ? Blocks.field_10243.method_9564() : var5.method_12254(var1 & 15, var2 & 15, var3 & 15);
            }
         }
      }

      private boolean emptySection(int var1, int var2, int var3) {
         WorldChunk var4 = this.chunkAt(var1 >> 4, var3 >> 4);
         if (var4 == null) {
            return true;
         } else {
            ChunkSection var5 = var4.method_12006()[var4.method_31602(var2)];
            return var5 == null || var5.method_38292();
         }
      }

      private WorldChunk chunkAt(int var1, int var2) {
         if (this.chunk != null && this.chunk.method_12004().field_9181 == var1 && this.chunk.method_12004().field_9180 == var2) {
            return this.chunk;
         } else {
            WorldChunk var3 = this.level.method_2935().method_21730(var1, var2);
            if (var3 != null) {
               this.chunk = var3;
            }

            return var3;
         }
      }

      private int light(int var1, int var2, int var3) {
         int var4 = Math.max(this.rawLight(LightType.field_9282, var1, var2, var3), this.rawLight(LightType.field_9284, var1, var2, var3));
         if (var4 < 15 && this.level.method_8597().comp_642()) {
            return this.openSky(var1, var2, var3) ? 15 : var4;
         } else {
            return var4;
         }
      }

      private int rawLight(LightType var1, int var2, int var3, int var4) {
         ChunkNibbleArray var5 = this.level.method_22336().method_15562(var1).method_15544(ChunkSectionPos.method_18676(var2 >> 4, var3 >> 4, var4 >> 4));
         return var5 != null && !var5.method_51380() ? var5.method_12139(var2 & 15, var3 & 15, var4 & 15) : 0;
      }

      private boolean openSky(int var1, int var2, int var3) {
         int var4 = this.level.method_31600();

         for (int var5 = var2 + 1; var5 <= var4; var5++) {
            if (this.emptySection(var1, var5, var3)) {
               var5 |= 15;
            } else if (this.state(var1, var5, var3).method_26193() > 0) {
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
