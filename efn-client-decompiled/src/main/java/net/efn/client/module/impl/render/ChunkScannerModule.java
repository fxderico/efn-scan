package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.ChunkAreas;
import net.efn.client.util.ChunkOverlay;
import net.efn.client.util.LevelUtil;
import net.efn.client.util.PerformanceBudget;
import net.efn.client.util.seedscan.AdvancedScanner;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.GeneratorOptions;

@Environment(EnvType.CLIENT)
public final class ChunkScannerModule extends ClientModule {
   private static final int PROBE_CHUNKS = 48;
   private static final byte OTHER = 0;
   private static final byte STONE = 1;
   private static final byte BLOB = 2;
   private static final byte AIR = 3;
   private static final byte MISSING = 4;
   private static final int[] SIDE_X = new int[]{1, -1, 0, 0};
   private static final int[] SIDE_Z = new int[]{0, 0, 1, -1};
   private static final int[] CORNER_X = new int[]{1, 1, -1, -1};
   private static final int[] CORNER_Z = new int[]{1, -1, 1, -1};
   private static final int MIN_Y = 0;
   private static final int MAX_Y = 32;
   private static final int CHUNK_COLOR = 1627389951;
   private static final int OLD_CHUNK_COLOR = 1614011733;
   private static final int SEED_SETTLE_TICKS = 20;
   private final BooleanSetting advancedScanner = this.addSetting("advanced-scanner", new BooleanSetting("Advanced Scanner", false));
   private final StringSetting seed = this.addSetting("seed", new StringSetting("Seed", ""));
   private final NumberSetting stackHeight = this.addSetting("stack-height", new NumberSetting("Sensitivity", 2.0F, 2.0F, 5.0F).step(1.0F));
   private final NumberSetting spreadRadius = this.addSetting("spread-radius", new NumberSetting("Spread Radius", 1.0F, 0.0F, 16.0F).step(1.0F));
   private final ColorSetting chunkColor = this.addSetting("chunk-color", new ColorSetting("Chunk Color", 1627389951));
   private final BooleanSetting tracers = this.addSetting("tracers", new BooleanSetting("Tracers", true));
   private final BooleanSetting sound = this.addSetting("sound", new BooleanSetting("Sound", true));
   private final ChunkAreas chunkAreas = new ChunkAreas();
   private final ChunkOverlay chunkOverlay = new ChunkOverlay();
   private static final ChunkScannerModule.Find UNDECIDED = new ChunkScannerModule.Find(0L, 0, new long[0]);
   private final Long2ObjectOpenHashMap<ChunkScannerModule.Find> flagged = new Long2ObjectOpenHashMap<>();
   private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
   private final LongOpenHashSet queued = new LongOpenHashSet();
   private final List<ChunkScannerModule.Find> fresh = new ArrayList<>();
   private final Long2IntOpenHashMap heat = new Long2IntOpenHashMap();
   private int appliedRadius;
   private LongOpenHashSet shown = new LongOpenHashSet();
   private boolean shownDirty = true;
   private int shownRefreshTicks;
   private ClientWorld lastLevel;
   private boolean scanning;
   private AdvancedScanner advanced;
   private boolean appliedAdvanced;
   private String seedText;
   private int seedStill;
   private boolean seedAsked;
   private ChunkScannerModule.ScanConfig appliedConfig;
   private WeakReference<ClientPlayNetworkHandler> probeServer = new WeakReference<>(null);
   private boolean probing;
   private final LongOpenHashSet probed = new LongOpenHashSet();
   private final WorldChunk[] around = new WorldChunk[9];
   private final int[] groupX = new int[4];
   private final int[] groupZ = new int[4];
   private int groupSize;
   private boolean guessed;
   private Block wall;
   private Block lastBlock;
   private final int[] stackTop = new int[256];
   private final int[] stackShape = new int[256];
   private final int[] stackAreas = new int[256];
   private final boolean[] stackGuessed = new boolean[256];
   private final Block[] stackWall = new Block[256];
   private boolean sawBlob;
   private int scanX;
   private int scanZ;
   private int keepHeight;
   private long[] keptStacks = new long[16];
   private int keptStackCount;
   private int tallest;
   private long bottom;
   private boolean unsure;

   public ChunkScannerModule() {
      super("chunk-scanner", "Chunk Scanner", "Marks suspicious chunks with signs of player activity.", ModuleCategory.BASE_RAID);
      BooleanSetting var1 = this.usesNotifications();
      this.sound.visibleWhen(var1::get);
      this.stackHeight.visibleWhen(() -> !this.advancedScanner.get());
      this.seed.visibleWhen(this.advancedScanner::get);
      this.seed.privateValue();
   }

   @Override
   public int settingsVersion() {
      return 1;
   }

   @Override
   public void migrateSettings(int i) {
      if (i < 1 && this.chunkColor.argb() == 1614011733) {
         this.chunkColor.setArgb(1627389951);
      }
   }

   @Override
   protected void onEnable() {
      this.startLevel(MinecraftClient.getInstance().world);
   }

   @Override
   protected void onDisable() {
      this.startLevel(null);
   }

   private void startLevel(ClientWorld clientWorld) {
      this.closeAdvanced();
      this.appliedAdvanced = this.advancedScanner.get();
      this.flagged.clear();
      this.heat.clear();
      this.appliedRadius = this.spreadRadius.getInt();
      this.queue.clear();
      this.queued.clear();
      this.fresh.clear();
      this.shown = new LongOpenHashSet();
      this.shownDirty = true;
      this.shownRefreshTicks = 0;
      this.lastLevel = clientWorld;
      boolean var2 = this.scanning = clientWorld != null && clientWorld.getRegistryKey() != World.NETHER && clientWorld.getRegistryKey() != World.END;
      if (this.scanning) {
         ClientPlayNetworkHandler var3 = MinecraftClient.getInstance().getNetworkHandler();
         if (var3 != this.probeServer.get()) {
            this.probeServer = new WeakReference<>(var3);
            this.probing = true;
            this.probed.clear();
         }

         this.appliedConfig = this.currentConfig(clientWorld);
         this.queueLoadedChunks(clientWorld);
      }
   }

   private ChunkScannerModule.ScanConfig currentConfig(ClientWorld clientWorld) {
      return new ChunkScannerModule.ScanConfig(Math.max(0, clientWorld.getBottomY()), Math.min(32, clientWorld.getTopYInclusive()), this.stackHeight.getInt());
   }

   private void queueLoadedChunks(ClientWorld clientWorld) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null) {
         int var3 = var2.options.getViewDistance().getValue() + 1;
         ChunkPos var4 = var2.player.getChunkPos();

         for (int var5 = var4.x - var3; var5 <= var4.x + var3; var5++) {
            for (int var6 = var4.z - var3; var6 <= var4.z + var3; var6++) {
               if (LevelUtil.isLoaded(clientWorld, var5, var6)) {
                  this.enqueue(ChunkPos.toLong(var5, var6));
               }
            }
         }
      }
   }

   private void enqueue(long l) {
      if (this.scanning && !this.advancedScanner.get() && this.queued.add(l)) {
         this.queue.enqueue(l);
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent packetAppliedEvent) {
      if (this.advanced != null) {
         Packet var2 = packetAppliedEvent.packet;
         if (packetAppliedEvent.packet instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket var3 = (ChunkDataS2CPacket)var2;
            this.advanced.chunkArrived(var3.getChunkX(), var3.getChunkZ());
         }
      }

      Packet var6 = packetAppliedEvent.packet;
      if (packetAppliedEvent.packet instanceof ChunkDataS2CPacket) {
         ChunkDataS2CPacket var7 = (ChunkDataS2CPacket)var6;
         this.shownDirty = true;
         this.enqueue(ChunkPos.toLong(var7.getChunkX(), var7.getChunkZ()));

         for (int var4 = -1; var4 <= 1; var4++) {
            for (int var5 = -1; var5 <= 1; var5++) {
               if (var4 != 0 || var5 != 0) {
                  this.enqueue(ChunkPos.toLong(var7.getChunkX() + var4, var7.getChunkZ() + var5));
               }
            }
         }
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent blockUpdateEvent) {
      if (this.scanning && blockUpdateEvent.pos != null) {
         if (this.advanced != null) {
            this.advanced.blockChanged(blockUpdateEvent.pos);
         }

         int var2;
         if ((var2 = blockUpdateEvent.pos.getY()) >= this.appliedConfig.minY() - 1 && var2 <= this.appliedConfig.maxY() + 1) {
            if (matters(blockUpdateEvent.oldState) || matters(blockUpdateEvent.newState)) {
               int var3 = blockUpdateEvent.pos.getX();
               int var4 = blockUpdateEvent.pos.getZ();

               for (int var5 = var3 - 2 >> 4; var5 <= var3 + 1 >> 4; var5++) {
                  for (int var6 = var4 - 2 >> 4; var6 <= var4 + 2 >> 4; var6++) {
                     this.enqueue(ChunkPos.toLong(var5, var6));
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      ClientWorld var5 = MinecraftClient.getInstance().world;
      if (var5 != this.lastLevel) {
         this.startLevel(var5);
      }

      if (this.scanning) {
         if (this.advancedScanner.get() != this.appliedAdvanced) {
            this.startLevel(var5);
         }

         ChunkScannerModule.ScanConfig var4;
         if (!(var4 = this.currentConfig(var5)).equals(this.appliedConfig)) {
            this.appliedConfig = var4;
            this.queueLoadedChunks(var5);
         }

         int var3;
         if ((var3 = this.spreadRadius.getInt()) != this.appliedRadius) {
            this.appliedRadius = var3;
            this.heat.clear();
            this.shownDirty = true;
            ObjectIterator var6 = this.flagged.long2ObjectEntrySet().fastIterator();

            while (var6.hasNext()) {
               Entry var7 = (Entry)var6.next();
               this.spread(var7.getLongKey(), (ChunkScannerModule.Find)var7.getValue(), 1);
            }
         }

         boolean var2;
         long var19 = (var2 = PerformanceModule.optimizationsEnabled())
            ? PerformanceBudget.acquire(PerformanceModule.scanBudgetNanos())
            : System.nanoTime() + PerformanceModule.scanBudgetNanos();
         long var8 = System.nanoTime();

         try {
            if (this.advancedScanner.get() && var8 < var19) {
               this.tickAdvanced(var5, var19);
            }

            while (!this.queue.isEmpty() && System.nanoTime() < var19) {
               long var10 = this.queue.dequeueLong();
               this.queued.remove(var10);
               int var12 = ChunkPos.getPackedX(var10);
               int var13 = ChunkPos.getPackedZ(var10);
               if (LevelUtil.isLoaded(var5, var12, var13)) {
                  ChunkScannerModule.Find var14 = this.scan(var5, var12, var13, var4);
                  if (this.sawBlob) {
                     this.probe(var10);
                  }

                  if (var14 != UNDECIDED) {
                     ChunkScannerModule.Find var15 = this.flagged.get(var10);
                     if (var14 == null ? var15 != null : var15 == null || !var15.sameAs(var14)) {
                        this.refile(var10, var15, var14);
                        if (var15 == null) {
                           this.fresh.add(var14);
                        }
                     }
                  }
               }
            }

            this.announce(this.fresh);
            this.fresh.clear();
            if (!var2 || this.shownDirty || ++this.shownRefreshTicks >= 5) {
               this.shown = this.computeShown(var5);
               this.shownDirty = false;
               this.shownRefreshTicks = 0;
            }
         } finally {
            if (var2) {
               PerformanceBudget.record(var8);
            }
         }
      }
   }

   private void refile(long l, ChunkScannerModule.Find find, ChunkScannerModule.Find find2) {
      this.shownDirty = true;
      if (find != null) {
         this.spread(l, find, -1);
      }

      if (find2 == null) {
         this.flagged.remove(l);
      } else {
         this.flagged.put(l, find2);
         this.spread(l, find2, 1);
      }
   }

   private void spread(long l, ChunkScannerModule.Find find, int i) {
      int var5 = i * find.stacks().length;
      int var6 = this.appliedRadius;
      int var7 = ChunkPos.getPackedX(l);
      int var8 = ChunkPos.getPackedZ(l);

      for (int var9 = var7 - var6; var9 <= var7 + var6; var9++) {
         for (int var10 = var8 - var6; var10 <= var8 + var6; var10++) {
            long var11 = ChunkPos.toLong(var9, var10);
            if (this.heat.addTo(var11, var5) + var5 == 0) {
               this.heat.remove(var11);
            }
         }
      }
   }

   private LongOpenHashSet computeShown(ClientWorld clientWorld) {
      Long2IntOpenHashMap var2 = new Long2IntOpenHashMap();

      for (it.unimi.dsi.fastutil.longs.Long2IntMap.Entry var4 : Long2IntMaps.fastIterable(this.heat)) {
         long var5 = var4.getLongKey();
         if (LevelUtil.isLoaded(clientWorld, ChunkPos.getPackedX(var5), ChunkPos.getPackedZ(var5))) {
            var2.put(var5, var4.getIntValue());
         }
      }

      return this.appliedRadius == 0 ? new LongOpenHashSet(var2.keySet()) : hottestPerPatch(var2);
   }

   private static LongOpenHashSet hottestPerPatch(Long2IntOpenHashMap long2IntOpenHashMap) {
      LongOpenHashSet var1 = new LongOpenHashSet();
      LongOpenHashSet var2 = new LongOpenHashSet();
      LongArrayList var3 = new LongArrayList();
      LongArrayFIFOQueue var4 = new LongArrayFIFOQueue();
      LongIterator var5 = long2IntOpenHashMap.keySet().iterator();

      while (var5.hasNext()) {
         long var6 = var5.next();
         if (var2.add(var6)) {
            var3.clear();
            var4.enqueue(var6);
            int var8 = 0;

            while (!var4.isEmpty()) {
               long var9 = var4.dequeueLong();
               var3.add(var9);
               var8 = Math.max(var8, long2IntOpenHashMap.get(var9));
               int var11 = ChunkPos.getPackedX(var9);
               int var12 = ChunkPos.getPackedZ(var9);

               for (int var13 = 0; var13 < 4; var13++) {
                  long var14 = ChunkPos.toLong(var11 + SIDE_X[var13], var12 + SIDE_Z[var13]);
                  if (long2IntOpenHashMap.containsKey(var14) && var2.add(var14)) {
                     var4.enqueue(var14);
                  }
               }
            }

            for (int var16 = 0; var16 < var3.size(); var16++) {
               if (long2IntOpenHashMap.get(var3.getLong(var16)) == var8) {
                  var1.add(var3.getLong(var16));
               }
            }
         }
      }

      return var1;
   }

   private ChunkScannerModule.Find scan(ClientWorld clientWorld, int i, int j, ChunkScannerModule.ScanConfig scanConfig) {
      for (int var5 = -1; var5 <= 1; var5++) {
         for (int var6 = -1; var6 <= 1; var6++) {
            this.around[(var5 + 1) * 3 + var6 + 1] = clientWorld.getChunkManager().getWorldChunk(i + var5, j + var6);
         }
      }

      this.sawBlob = false;
      this.scanX = i << 4;
      this.scanZ = j << 4;
      this.keepHeight = scanConfig.stackHeight();
      this.keptStackCount = 0;
      this.tallest = 0;
      this.bottom = 0L;
      this.unsure = false;

      ChunkScannerModule.Find var29;
      try {
         WorldChunk var26 = this.around[4];
         ChunkSection[] var7 = var26.getSectionArray();
         int var8 = var26.getBottomSectionCoord();
         Arrays.fill(this.stackTop, -2147483648);
         boolean var9 = true;

         for (int var24 = 0; var24 < var7.length; var24++) {
            ChunkSection var11 = var7[var24];
            int var10;
            if (var11 != null && !var11.isEmpty() && (var10 = var8 + var24 << 4) + 15 >= scanConfig.minY() && var10 <= scanConfig.maxY()) {
               int var12 = Math.max(scanConfig.minY() - var10, 0);
               int var13 = Math.min(scanConfig.maxY() - var10, 15);
               if (var9) {
                  var9 = !hasBlocks(var11, var12, var13);
               }

               if (var11.hasAny(blockState -> blockState.isOf(Blocks.STONE)) && var11.hasAny(blockState -> isBlob(blockState.getBlock()))) {
                  for (int var14 = var12; var14 <= var13; var14++) {
                     for (int var15 = 0; var15 < 16; var15++) {
                        boolean var16 = false;

                        for (int var17 = 0; var17 < 16; var17++) {
                           Block var19 = var11.getBlockState(var17, var14, var15).getBlock();
                           boolean var18 = var19 == Blocks.STONE;
                           if (var18) {
                              if (!var16 && this.isArea(var17, var10 + var14, var15)) {
                                 this.stack(var17, var10 + var14, var15);
                              }
                           } else if (this.probing && isBlob(var19)) {
                              this.sawBlob = true;
                              if (this.enclosed(var17, var10 + var14, var15)) {
                                 this.probing = false;
                                 this.probed.clear();
                              }
                           }

                           var16 = var18;
                        }
                     }
                  }
               }
            }
         }

         for (int var25 = 0; var25 < 256; var25++) {
            this.keep(var25);
         }

         if (this.tallest < scanConfig.stackHeight()) {
            return !this.unsure && !var9 ? null : UNDECIDED;
         }

         ChunkScannerModule.Find var27 = new ChunkScannerModule.Find(this.bottom, this.tallest, Arrays.copyOf(this.keptStacks, this.keptStackCount));
         var29 = var27;
      } finally {
         Arrays.fill(this.around, null);
      }

      return var29;
   }

   private void stack(int i, int j, int k) {
      int var5 = k << 4 | i;
      int var6 = this.shape(i, k);
      boolean var4 = this.stackTop[var5] == j - 1 && this.stackShape[var5] == var6 && this.stackWall[var5] == this.wall;
      if (!var4) {
         this.keep(var5);
      }

      this.stackTop[var5] = j;
      this.stackShape[var5] = var6;
      this.stackWall[var5] = this.wall;
      this.stackAreas[var5] = var4 ? this.stackAreas[var5] + 1 : 1;
      this.stackGuessed[var5] = this.guessed || var4 && this.stackGuessed[var5];
   }

   private void keep(int i) {
      int var2 = this.stackAreas[i];
      int var3 = this.stackTop[i];
      if (var3 != -2147483648 && var2 >= this.keepHeight) {
         int var4 = this.stackShape[i];
         if (this.goesOn(i & 15, var3 + 1, i >> 4, var4) && this.goesOn(i & 15, var3 - var2, i >> 4, var4)) {
            if (this.stackGuessed[i]) {
               this.unsure = true;
            } else {
               int var5 = this.scanX + (i & 15);
               int var6 = this.scanZ + (i >> 4);
               long var7 = BlockPos.asLong(var5, var3 - var2 + 1, var6);
               if (var2 > this.tallest || var2 == this.tallest && before(var7, this.bottom)) {
                  this.tallest = var2;
                  this.bottom = var7;
               }

               if (this.keptStackCount == this.keptStacks.length) {
                  this.keptStacks = Arrays.copyOf(this.keptStacks, this.keptStacks.length * 2);
               }

               this.keptStacks[this.keptStackCount++] = var7;
            }
         }
      }
   }

   private boolean goesOn(int i, int j, int k, int l) {
      for (int var5 = 0; var5 < 6; var5++) {
         if ((l >> var5 & 1) != 0) {
            this.kind(i + var5 / 3, j, k + var5 % 3 - 1);
            if (!carriesStone(this.lastBlock)) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean before(long l, long m) {
      if (BlockPos.unpackLongY(l) != BlockPos.unpackLongY(m)) {
         return BlockPos.unpackLongY(l) < BlockPos.unpackLongY(m);
      } else {
         return BlockPos.unpackLongZ(l) != BlockPos.unpackLongZ(m)
            ? BlockPos.unpackLongZ(l) < BlockPos.unpackLongZ(m)
            : BlockPos.unpackLongX(l) < BlockPos.unpackLongX(m);
      }
   }

   private int shape(int i, int j) {
      int var3 = 0;

      for (int var4 = 0; var4 < this.groupSize; var4++) {
         var3 |= 1 << (this.groupX[var4] - i) * 3 + this.groupZ[var4] - j + 1;
      }

      return var3;
   }

   private boolean isArea(int i, int j, int k) {
      this.guessed = false;
      this.wall = null;
      if (this.seen(i - 1, j, k) == 2 && this.sameWall() && this.seen(i, j, k - 1) == 2 && this.sameWall()) {
         this.groupX[0] = i;
         this.groupZ[0] = k;
         int var7 = 1;
         int var8 = i;
         int var9 = k;
         int var10 = k;

         for (int var6 = 0; var6 < var7; var6++) {
            for (int var11 = 0; var11 < 4; var11++) {
               int var5 = this.groupX[var6] + SIDE_X[var11];
               int var4 = this.groupZ[var6] + SIDE_Z[var11];
               if (!this.inGroup(var5, var4, var7)) {
                  byte var12 = this.seen(var5, j, var4);
                  if (var12 == 2) {
                     if (!this.sameWall()) {
                        return false;
                     }
                  } else {
                     if (var12 != 1) {
                        return false;
                     }

                     if (var5 < i || var5 == i && var4 < k) {
                        return false;
                     }

                     var8 = Math.max(var8, var5);
                     var9 = Math.min(var9, var4);
                     var10 = Math.max(var10, var4);
                     if (var8 - i > 1 || var10 - var9 > 1) {
                        return false;
                     }

                     this.groupX[var7] = var5;
                     this.groupZ[var7] = var4;
                     var7++;
                  }
               }
            }
         }

         for (int var15 = 0; var15 < var7; var15++) {
            for (int var17 = 0; var17 < 4; var17++) {
               int var14 = this.groupX[var15] + CORNER_X[var17];
               int var13 = this.groupZ[var15] + CORNER_Z[var17];
               if (!this.inGroup(var14, var13, var7) && (this.seen(var14, j, var13) != 2 || !this.sameWall())) {
                  return false;
               }
            }
         }

         for (int var16 = 0; var16 < var7; var16++) {
            if (this.kind(this.groupX[var16], j + 1, this.groupZ[var16]) == 3 || this.kind(this.groupX[var16], j - 1, this.groupZ[var16]) == 3) {
               return false;
            }
         }

         this.groupSize = var7;
         return true;
      } else {
         return false;
      }
   }

   private boolean inGroup(int i, int j, int k) {
      for (int var4 = 0; var4 < k; var4++) {
         if (this.groupX[var4] == i && this.groupZ[var4] == j) {
            return true;
         }
      }

      return false;
   }

   private byte seen(int i, int j, int k) {
      byte var4 = this.kind(i, j, k);
      if (var4 != 4) {
         return var4;
      } else {
         this.guessed = true;
         return 2;
      }
   }

   private boolean sameWall() {
      if (this.lastBlock == null) {
         return true;
      } else {
         if (this.wall == null) {
            this.wall = this.lastBlock;
         }

         return this.lastBlock == this.wall;
      }
   }

   private byte kind(int i, int j, int k) {
      WorldChunk var4 = this.around[((i >> 4) + 1) * 3 + (k >> 4) + 1];
      this.lastBlock = null;
      if (var4 == null) {
         return 4;
      } else {
         int var5 = var4.getSectionIndex(j);
         if (var5 >= 0 && var5 < var4.countVerticalSections()) {
            BlockState var6 = var4.getSection(var5).getBlockState(i & 15, j & 15, k & 15);
            this.lastBlock = var6.getBlock();
            return kindOf(var6);
         } else {
            return 0;
         }
      }
   }

   private boolean enclosed(int i, int j, int k) {
      return this.full(i + 1, j, k)
         && this.full(i - 1, j, k)
         && this.full(i, j, k + 1)
         && this.full(i, j, k - 1)
         && this.full(i, j + 1, k)
         && this.full(i, j - 1, k);
   }

   private boolean full(int i, int j, int k) {
      WorldChunk var4 = this.around[((i >> 4) + 1) * 3 + (k >> 4) + 1];
      if (var4 == null) {
         return false;
      } else {
         int var5 = var4.getSectionIndex(j);
         return var5 >= 0 && var5 < var4.countVerticalSections() && var4.getSection(var5).getBlockState(i & 15, j & 15, k & 15).isOpaqueFullCube();
      }
   }

   private static boolean hasBlocks(ChunkSection chunkSection, int i, int j) {
      if (!chunkSection.hasAny(blockState -> !blockState.isAir())) {
         return false;
      } else {
         for (int var3 = i; var3 <= j; var3++) {
            for (int var4 = 0; var4 < 16; var4++) {
               for (int var5 = 0; var5 < 16; var5++) {
                  if (!chunkSection.getBlockState(var5, var3, var4).isAir()) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private static byte kindOf(BlockState blockState) {
      Block var1 = blockState.getBlock();
      if (var1 == Blocks.STONE) {
         return 1;
      } else if (isBlob(var1)) {
         return 2;
      } else {
         return (byte)(blockState.isAir() ? 3 : 0);
      }
   }

   private static boolean isBlob(Block block) {
      return block == Blocks.DIORITE || block == Blocks.ANDESITE || block == Blocks.GRANITE || block == Blocks.GRAVEL;
   }

   private static boolean carriesStone(Block block) {
      return block == Blocks.STONE || block == Blocks.DEEPSLATE;
   }

   private static boolean matters(BlockState blockState) {
      return blockState != null && (kindOf(blockState) != 0 || carriesStone(blockState.getBlock()));
   }

   private void probe(long l) {
      if (this.probing && this.probed.add(l) && this.probed.size() >= 48) {
         this.probing = false;
         this.probed.clear();
         ModuleChat.send(this, ModuleChat.body().text("This server ").warn("hides").text(" ores with anti-xray, so patched shafts can't be seen"));
      }
   }

   private void announce(List<ChunkScannerModule.Find> list) {
      if (this.notificationsOn()) {
         if (!list.isEmpty()) {
            ItemStack var2 = new ItemStack(this.advancedScanner.get() ? Items.DIORITE : Items.SPYGLASS);
            ChallengeToast.addOrUpdate(var2, this.name(), Text.literal("Suspect Chunk Scanned"), this.sound.get());
         }
      }
   }

   private void tickAdvanced(ClientWorld clientWorld, long l) {
      String var4 = this.seed.get();
      if (!var4.equals(this.seedText)) {
         this.seedStill = this.seedText == null ? 20 : 0;
         this.seedText = var4;
      } else if (this.seedStill < 20) {
         this.seedStill++;
      }

      if (this.seedStill >= 20) {
         OptionalLong var5 = GeneratorOptions.parseSeed(var4);
         if (var5.isEmpty()) {
            if (this.advanced != null) {
               this.startLevel(clientWorld);
            }

            if (!this.seedAsked) {
               this.seedAsked = true;
               ModuleChat.send(this, ModuleChat.body().text("Advanced Scanner needs the world seed: ").warn("set Seed"));
            }

            return;
         }

         this.seedAsked = false;
         if (this.advanced == null || this.advanced.seed() != var5.getAsLong()) {
            if (this.advanced != null) {
               this.startLevel(clientWorld);
            }

            this.advanced = AdvancedScanner.start(var5.getAsLong());
         }
      }

      if (this.advanced != null) {
         for (AdvancedScanner.ChunkResult var6 : this.advanced.tick(clientWorld, l)) {
            ChunkScannerModule.Find var7 = var6.flagged() ? new ChunkScannerModule.Find(var6.anchors()[0], var6.blocks(), var6.anchors()) : null;
            ChunkScannerModule.Find var8 = this.flagged.get(var6.chunk());
            if (var7 == null ? var8 != null : var8 == null || !var8.sameAs(var7)) {
               this.refile(var6.chunk(), var8, var7);
               if (var8 == null && var7 != null) {
                  this.fresh.add(var7);
               }
            }
         }

         for (String var11 : this.advanced.drainNotices()) {
            ModuleChat.send(this, var11);
         }
      }
   }

   private void closeAdvanced() {
      if (this.advanced != null) {
         this.advanced.close();
      }

      this.advanced = null;
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      if (MinecraftClient.getInstance().world != null) {
         this.chunkOverlay.draw(this.shown, this.chunkColor.argb(), render3DEvent.partialTick);
         if (this.tracers.get()) {
            this.chunkAreas.drawTracers(this.shown, 63.0, ColorHelper.fullAlpha(this.chunkColor.argb()));
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record Find(long pos, int height, long[] stacks) {
      boolean sameAs(ChunkScannerModule.Find find) {
         return this.pos == find.pos && this.height == find.height && Arrays.equals(this.stacks, find.stacks);
      }
   }

   @Environment(EnvType.CLIENT)
   private record ScanConfig(int minY, int maxY, int stackHeight) {
   }
}
