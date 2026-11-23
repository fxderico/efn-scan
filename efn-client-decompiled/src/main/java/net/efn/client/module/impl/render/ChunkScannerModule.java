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
   private final Long2ObjectOpenHashMap<ChunkScannerModule.Find> flagged = new Long2ObjectOpenHashMap();
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
   public void migrateSettings(int var1) {
      if (var1 < 1 && this.chunkColor.argb() == 1614011733) {
         this.chunkColor.setArgb(1627389951);
      }
   }

   @Override
   protected void onEnable() {
      this.startLevel(MinecraftClient.method_1551().field_1687);
   }

   @Override
   protected void onDisable() {
      this.startLevel(null);
   }

   private void startLevel(ClientWorld var1) {
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
      this.lastLevel = var1;
      boolean var2 = this.scanning = var1 != null && var1.method_27983() != World.field_25180 && var1.method_27983() != World.field_25181;
      if (this.scanning) {
         ClientPlayNetworkHandler var3 = MinecraftClient.method_1551().method_1562();
         if (var3 != this.probeServer.get()) {
            this.probeServer = new WeakReference<>(var3);
            this.probing = true;
            this.probed.clear();
         }

         this.appliedConfig = this.currentConfig(var1);
         this.queueLoadedChunks(var1);
      }
   }

   private ChunkScannerModule.ScanConfig currentConfig(ClientWorld var1) {
      return new ChunkScannerModule.ScanConfig(Math.max(0, var1.method_31607()), Math.min(32, var1.method_31600()), this.stackHeight.getInt());
   }

   private void queueLoadedChunks(ClientWorld var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 != null) {
         int var3 = (Integer)var2.field_1690.method_42503().method_41753() + 1;
         ChunkPos var4 = var2.field_1724.method_31476();

         for (int var5 = var4.field_9181 - var3; var5 <= var4.field_9181 + var3; var5++) {
            for (int var6 = var4.field_9180 - var3; var6 <= var4.field_9180 + var3; var6++) {
               if (LevelUtil.isLoaded(var1, var5, var6)) {
                  this.enqueue(ChunkPos.method_8331(var5, var6));
               }
            }
         }
      }
   }

   private void enqueue(long var1) {
      if (this.scanning && !this.advancedScanner.get() && this.queued.add(var1)) {
         this.queue.enqueue(var1);
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent var1) {
      if (this.advanced != null) {
         Packet var2 = var1.packet;
         if (var1.packet instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket var3 = (ChunkDataS2CPacket)var2;
            this.advanced.chunkArrived(var3.method_11523(), var3.method_11524());
         }
      }

      Packet var6 = var1.packet;
      if (var1.packet instanceof ChunkDataS2CPacket) {
         ChunkDataS2CPacket var7 = (ChunkDataS2CPacket)var6;
         this.shownDirty = true;
         this.enqueue(ChunkPos.method_8331(var7.method_11523(), var7.method_11524()));

         for (int var4 = -1; var4 <= 1; var4++) {
            for (int var5 = -1; var5 <= 1; var5++) {
               if (var4 != 0 || var5 != 0) {
                  this.enqueue(ChunkPos.method_8331(var7.method_11523() + var4, var7.method_11524() + var5));
               }
            }
         }
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent var1) {
      if (this.scanning && var1.pos != null) {
         if (this.advanced != null) {
            this.advanced.blockChanged(var1.pos);
         }

         int var2;
         if ((var2 = var1.pos.method_10264()) >= this.appliedConfig.minY() - 1 && var2 <= this.appliedConfig.maxY() + 1) {
            if (matters(var1.oldState) || matters(var1.newState)) {
               int var3 = var1.pos.method_10263();
               int var4 = var1.pos.method_10260();

               for (int var5 = var3 - 2 >> 4; var5 <= var3 + 1 >> 4; var5++) {
                  for (int var6 = var4 - 2 >> 4; var6 <= var4 + 2 >> 4; var6++) {
                     this.enqueue(ChunkPos.method_8331(var5, var6));
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      ClientWorld var5 = MinecraftClient.method_1551().field_1687;
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
               int var12 = ChunkPos.method_8325(var10);
               int var13 = ChunkPos.method_8332(var10);
               if (LevelUtil.isLoaded(var5, var12, var13)) {
                  ChunkScannerModule.Find var14 = this.scan(var5, var12, var13, var4);
                  if (this.sawBlob) {
                     this.probe(var10);
                  }

                  if (var14 != UNDECIDED) {
                     ChunkScannerModule.Find var15 = (ChunkScannerModule.Find)this.flagged.get(var10);
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

   private void refile(long var1, ChunkScannerModule.Find var3, ChunkScannerModule.Find var4) {
      this.shownDirty = true;
      if (var3 != null) {
         this.spread(var1, var3, -1);
      }

      if (var4 == null) {
         this.flagged.remove(var1);
      } else {
         this.flagged.put(var1, var4);
         this.spread(var1, var4, 1);
      }
   }

   private void spread(long var1, ChunkScannerModule.Find var3, int var4) {
      int var5 = var4 * var3.stacks().length;
      int var6 = this.appliedRadius;
      int var7 = ChunkPos.method_8325(var1);
      int var8 = ChunkPos.method_8332(var1);

      for (int var9 = var7 - var6; var9 <= var7 + var6; var9++) {
         for (int var10 = var8 - var6; var10 <= var8 + var6; var10++) {
            long var11 = ChunkPos.method_8331(var9, var10);
            if (this.heat.addTo(var11, var5) + var5 == 0) {
               this.heat.remove(var11);
            }
         }
      }
   }

   private LongOpenHashSet computeShown(ClientWorld var1) {
      Long2IntOpenHashMap var2 = new Long2IntOpenHashMap();
      ObjectIterator var3 = Long2IntMaps.fastIterable(this.heat).iterator();

      while (var3.hasNext()) {
         it.unimi.dsi.fastutil.longs.Long2IntMap.Entry var4 = (it.unimi.dsi.fastutil.longs.Long2IntMap.Entry)var3.next();
         long var5 = var4.getLongKey();
         if (LevelUtil.isLoaded(var1, ChunkPos.method_8325(var5), ChunkPos.method_8332(var5))) {
            var2.put(var5, var4.getIntValue());
         }
      }

      return this.appliedRadius == 0 ? new LongOpenHashSet(var2.keySet()) : hottestPerPatch(var2);
   }

   private static LongOpenHashSet hottestPerPatch(Long2IntOpenHashMap var0) {
      LongOpenHashSet var1 = new LongOpenHashSet();
      LongOpenHashSet var2 = new LongOpenHashSet();
      LongArrayList var3 = new LongArrayList();
      LongArrayFIFOQueue var4 = new LongArrayFIFOQueue();
      LongIterator var5 = var0.keySet().iterator();

      while (var5.hasNext()) {
         long var6 = var5.next();
         if (var2.add(var6)) {
            var3.clear();
            var4.enqueue(var6);
            int var8 = 0;

            while (!var4.isEmpty()) {
               long var9 = var4.dequeueLong();
               var3.add(var9);
               var8 = Math.max(var8, var0.get(var9));
               int var11 = ChunkPos.method_8325(var9);
               int var12 = ChunkPos.method_8332(var9);

               for (int var13 = 0; var13 < 4; var13++) {
                  long var14 = ChunkPos.method_8331(var11 + SIDE_X[var13], var12 + SIDE_Z[var13]);
                  if (var0.containsKey(var14) && var2.add(var14)) {
                     var4.enqueue(var14);
                  }
               }
            }

            for (int var16 = 0; var16 < var3.size(); var16++) {
               if (var0.get(var3.getLong(var16)) == var8) {
                  var1.add(var3.getLong(var16));
               }
            }
         }
      }

      return var1;
   }

   private ChunkScannerModule.Find scan(ClientWorld var1, int var2, int var3, ChunkScannerModule.ScanConfig var4) {
      for (int var5 = -1; var5 <= 1; var5++) {
         for (int var6 = -1; var6 <= 1; var6++) {
            this.around[(var5 + 1) * 3 + var6 + 1] = var1.method_2935().method_21730(var2 + var5, var3 + var6);
         }
      }

      this.sawBlob = false;
      this.scanX = var2 << 4;
      this.scanZ = var3 << 4;
      this.keepHeight = var4.stackHeight();
      this.keptStackCount = 0;
      this.tallest = 0;
      this.bottom = 0L;
      this.unsure = false;

      ChunkScannerModule.Find var29;
      try {
         WorldChunk var26 = this.around[4];
         ChunkSection[] var7 = var26.method_12006();
         int var8 = var26.method_32891();
         Arrays.fill(this.stackTop, -2147483648);
         boolean var9 = true;

         for (int var24 = 0; var24 < var7.length; var24++) {
            ChunkSection var11 = var7[var24];
            int var10;
            if (var11 != null && !var11.method_38292() && (var10 = var8 + var24 << 4) + 15 >= var4.minY() && var10 <= var4.maxY()) {
               int var12 = Math.max(var4.minY() - var10, 0);
               int var13 = Math.min(var4.maxY() - var10, 15);
               if (var9) {
                  var9 = !hasBlocks(var11, var12, var13);
               }

               if (var11.method_19523(var0 -> var0.method_27852(Blocks.field_10340)) && var11.method_19523(var0 -> isBlob(var0.method_26204()))) {
                  for (int var14 = var12; var14 <= var13; var14++) {
                     for (int var15 = 0; var15 < 16; var15++) {
                        boolean var16 = false;

                        for (int var17 = 0; var17 < 16; var17++) {
                           Block var19 = var11.method_12254(var17, var14, var15).method_26204();
                           boolean var18 = var19 == Blocks.field_10340;
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

         if (this.tallest < var4.stackHeight()) {
            return !this.unsure && !var9 ? null : UNDECIDED;
         }

         ChunkScannerModule.Find var27 = new ChunkScannerModule.Find(this.bottom, this.tallest, Arrays.copyOf(this.keptStacks, this.keptStackCount));
         var29 = var27;
      } finally {
         Arrays.fill(this.around, null);
      }

      return var29;
   }

   private void stack(int var1, int var2, int var3) {
      int var5 = var3 << 4 | var1;
      int var6 = this.shape(var1, var3);
      boolean var4 = this.stackTop[var5] == var2 - 1 && this.stackShape[var5] == var6 && this.stackWall[var5] == this.wall;
      if (!var4) {
         this.keep(var5);
      }

      this.stackTop[var5] = var2;
      this.stackShape[var5] = var6;
      this.stackWall[var5] = this.wall;
      this.stackAreas[var5] = var4 ? this.stackAreas[var5] + 1 : 1;
      this.stackGuessed[var5] = this.guessed || var4 && this.stackGuessed[var5];
   }

   private void keep(int var1) {
      int var2 = this.stackAreas[var1];
      int var3 = this.stackTop[var1];
      if (var3 != -2147483648 && var2 >= this.keepHeight) {
         int var4 = this.stackShape[var1];
         if (this.goesOn(var1 & 15, var3 + 1, var1 >> 4, var4) && this.goesOn(var1 & 15, var3 - var2, var1 >> 4, var4)) {
            if (this.stackGuessed[var1]) {
               this.unsure = true;
            } else {
               int var5 = this.scanX + (var1 & 15);
               int var6 = this.scanZ + (var1 >> 4);
               long var7 = BlockPos.method_10064(var5, var3 - var2 + 1, var6);
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

   private boolean goesOn(int var1, int var2, int var3, int var4) {
      for (int var5 = 0; var5 < 6; var5++) {
         if ((var4 >> var5 & 1) != 0) {
            this.kind(var1 + var5 / 3, var2, var3 + var5 % 3 - 1);
            if (!carriesStone(this.lastBlock)) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean before(long var0, long var2) {
      if (BlockPos.method_10071(var0) != BlockPos.method_10071(var2)) {
         return BlockPos.method_10071(var0) < BlockPos.method_10071(var2);
      } else {
         return BlockPos.method_10083(var0) != BlockPos.method_10083(var2)
            ? BlockPos.method_10083(var0) < BlockPos.method_10083(var2)
            : BlockPos.method_10061(var0) < BlockPos.method_10061(var2);
      }
   }

   private int shape(int var1, int var2) {
      int var3 = 0;

      for (int var4 = 0; var4 < this.groupSize; var4++) {
         var3 |= 1 << (this.groupX[var4] - var1) * 3 + this.groupZ[var4] - var2 + 1;
      }

      return var3;
   }

   private boolean isArea(int var1, int var2, int var3) {
      this.guessed = false;
      this.wall = null;
      if (this.seen(var1 - 1, var2, var3) == 2 && this.sameWall() && this.seen(var1, var2, var3 - 1) == 2 && this.sameWall()) {
         this.groupX[0] = var1;
         this.groupZ[0] = var3;
         int var7 = 1;
         int var8 = var1;
         int var9 = var3;
         int var10 = var3;

         for (int var6 = 0; var6 < var7; var6++) {
            for (int var11 = 0; var11 < 4; var11++) {
               int var5 = this.groupX[var6] + SIDE_X[var11];
               int var4 = this.groupZ[var6] + SIDE_Z[var11];
               if (!this.inGroup(var5, var4, var7)) {
                  byte var12 = this.seen(var5, var2, var4);
                  if (var12 == 2) {
                     if (!this.sameWall()) {
                        return false;
                     }
                  } else {
                     if (var12 != 1) {
                        return false;
                     }

                     if (var5 < var1 || var5 == var1 && var4 < var3) {
                        return false;
                     }

                     var8 = Math.max(var8, var5);
                     var9 = Math.min(var9, var4);
                     var10 = Math.max(var10, var4);
                     if (var8 - var1 > 1 || var10 - var9 > 1) {
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
               if (!this.inGroup(var14, var13, var7) && (this.seen(var14, var2, var13) != 2 || !this.sameWall())) {
                  return false;
               }
            }
         }

         for (int var16 = 0; var16 < var7; var16++) {
            if (this.kind(this.groupX[var16], var2 + 1, this.groupZ[var16]) == 3 || this.kind(this.groupX[var16], var2 - 1, this.groupZ[var16]) == 3) {
               return false;
            }
         }

         this.groupSize = var7;
         return true;
      } else {
         return false;
      }
   }

   private boolean inGroup(int var1, int var2, int var3) {
      for (int var4 = 0; var4 < var3; var4++) {
         if (this.groupX[var4] == var1 && this.groupZ[var4] == var2) {
            return true;
         }
      }

      return false;
   }

   private byte seen(int var1, int var2, int var3) {
      byte var4 = this.kind(var1, var2, var3);
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

   private byte kind(int var1, int var2, int var3) {
      WorldChunk var4 = this.around[((var1 >> 4) + 1) * 3 + (var3 >> 4) + 1];
      this.lastBlock = null;
      if (var4 == null) {
         return 4;
      } else {
         int var5 = var4.method_31602(var2);
         if (var5 >= 0 && var5 < var4.method_32890()) {
            BlockState var6 = var4.method_38259(var5).method_12254(var1 & 15, var2 & 15, var3 & 15);
            this.lastBlock = var6.method_26204();
            return kindOf(var6);
         } else {
            return 0;
         }
      }
   }

   private boolean enclosed(int var1, int var2, int var3) {
      return this.full(var1 + 1, var2, var3)
         && this.full(var1 - 1, var2, var3)
         && this.full(var1, var2, var3 + 1)
         && this.full(var1, var2, var3 - 1)
         && this.full(var1, var2 + 1, var3)
         && this.full(var1, var2 - 1, var3);
   }

   private boolean full(int var1, int var2, int var3) {
      WorldChunk var4 = this.around[((var1 >> 4) + 1) * 3 + (var3 >> 4) + 1];
      if (var4 == null) {
         return false;
      } else {
         int var5 = var4.method_31602(var2);
         return var5 >= 0 && var5 < var4.method_32890() && var4.method_38259(var5).method_12254(var1 & 15, var2 & 15, var3 & 15).method_26216();
      }
   }

   private static boolean hasBlocks(ChunkSection var0, int var1, int var2) {
      if (!var0.method_19523(var0x -> !var0x.method_26215())) {
         return false;
      } else {
         for (int var3 = var1; var3 <= var2; var3++) {
            for (int var4 = 0; var4 < 16; var4++) {
               for (int var5 = 0; var5 < 16; var5++) {
                  if (!var0.method_12254(var5, var3, var4).method_26215()) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   private static byte kindOf(BlockState var0) {
      Block var1 = var0.method_26204();
      if (var1 == Blocks.field_10340) {
         return 1;
      } else if (isBlob(var1)) {
         return 2;
      } else {
         return (byte)(var0.method_26215() ? 3 : 0);
      }
   }

   private static boolean isBlob(Block var0) {
      return var0 == Blocks.field_10508 || var0 == Blocks.field_10115 || var0 == Blocks.field_10474 || var0 == Blocks.field_10255;
   }

   private static boolean carriesStone(Block var0) {
      return var0 == Blocks.field_10340 || var0 == Blocks.field_28888;
   }

   private static boolean matters(BlockState var0) {
      return var0 != null && (kindOf(var0) != 0 || carriesStone(var0.method_26204()));
   }

   private void probe(long var1) {
      if (this.probing && this.probed.add(var1) && this.probed.size() >= 48) {
         this.probing = false;
         this.probed.clear();
         ModuleChat.send(this, ModuleChat.body().text("This server ").warn("hides").text(" ores with anti-xray, so patched shafts can't be seen"));
      }
   }

   private void announce(List<ChunkScannerModule.Find> var1) {
      if (this.notificationsOn()) {
         if (!var1.isEmpty()) {
            ItemStack var2 = new ItemStack(this.advancedScanner.get() ? Items.field_20401 : Items.field_27070);
            ChallengeToast.addOrUpdate(var2, this.name(), Text.method_43470("Suspect Chunk Scanned"), this.sound.get());
         }
      }
   }

   private void tickAdvanced(ClientWorld var1, long var2) {
      String var4 = this.seed.get();
      if (!var4.equals(this.seedText)) {
         this.seedStill = this.seedText == null ? 20 : 0;
         this.seedText = var4;
      } else if (this.seedStill < 20) {
         this.seedStill++;
      }

      if (this.seedStill >= 20) {
         OptionalLong var5 = GeneratorOptions.method_46720(var4);
         if (var5.isEmpty()) {
            if (this.advanced != null) {
               this.startLevel(var1);
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
               this.startLevel(var1);
            }

            this.advanced = AdvancedScanner.start(var5.getAsLong());
         }
      }

      if (this.advanced != null) {
         for (AdvancedScanner.ChunkResult var6 : this.advanced.tick(var1, var2)) {
            ChunkScannerModule.Find var7 = var6.flagged() ? new ChunkScannerModule.Find(var6.anchors()[0], var6.blocks(), var6.anchors()) : null;
            ChunkScannerModule.Find var8 = (ChunkScannerModule.Find)this.flagged.get(var6.chunk());
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
   public void onRender3D(Render3DEvent var1) {
      if (MinecraftClient.method_1551().field_1687 != null) {
         this.chunkOverlay.draw(this.shown, this.chunkColor.argb(), var1.partialTick);
         if (this.tracers.get()) {
            this.chunkAreas.drawTracers(this.shown, 63.0, ColorHelper.method_61334(this.chunkColor.argb()));
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record Find(long pos, int height, long[] stacks) {
      boolean sameAs(ChunkScannerModule.Find var1) {
         return this.pos == var1.pos && this.height == var1.height && Arrays.equals(this.stacks, var1.stacks);
      }
   }

   @Environment(EnvType.CLIENT)
   private record ScanConfig(int minY, int maxY, int stackHeight) {
   }
}
