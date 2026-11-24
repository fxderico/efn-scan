package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.util.ChunkAreas;
import net.efn.client.util.ChunkOverlay;
import net.efn.client.util.LevelUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CaveVines;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonExtensionBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.RedstoneBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.TargetBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;

@Environment(EnvType.CLIENT)
public final class PrimeChunkFinderModule extends ClientModule {
   private static final int SECTION_BYTES = 2048;
   private static final int MAX_FLAGGED_CHUNKS = 256;
   private static final int AMETHYST_MAX_LIGHT = 5;
   private static final int AMETHYST_REACH = 5;
   private static final long REDSTONE_NOTIFY_COOLDOWN_MS = 15000L;
   private static final int ORE_LIGHT = 9;
   private static final long[] NO_ORES = new long[0];
   private static final int LAMP_LIGHT = 14;
   private static final int[] NO_LAMPS = new int[0];
   private static final long GONE_LAMP_MS = 5000L;
   private static final int MAX_TRACKED_CHUNKS = 5000;
   private static final int SNAPSHOT_RADIUS = 32;
   private static final byte[] ZERO_SECTION = new byte[2048];
   private static final double MAX_Y = 0.0;
   private final ColorSetting chunkColor = this.addSetting("chunk-color", new ColorSetting("Chunk Color", 1342242650));
   private final BooleanSetting tracers = this.addSetting("tracers", new BooleanSetting("Tracers", true));
   private final BooleanSetting sound = this.addSetting("sound", new BooleanSetting("Sound", true));
   private final LinkedHashMap<Long, Map<Integer, byte[]>> chunkLight = new LinkedHashMap<>(256, 0.75F, true);
   private final LongOpenHashSet passedChunks = new LongOpenHashSet();
   private final LongLinkedOpenHashSet flaggedChunks = new LongLinkedOpenHashSet();
   private final LongOpenHashSet shownChunks = new LongOpenHashSet();
   private final ChunkAreas chunkAreas = new ChunkAreas();
   private final ChunkOverlay chunkOverlay = new ChunkOverlay();
   private final LongOpenHashSet checkedSections = new LongOpenHashSet();
   private final LongOpenHashSet amethystSections = new LongOpenHashSet();
   private final Long2ObjectOpenHashMap<long[]> oreSections = new Long2ObjectOpenHashMap<>();
   private final Long2ObjectOpenHashMap<int[]> lampSections = new Long2ObjectOpenHashMap<>();
   private final Long2LongOpenHashMap goneLamps = new Long2LongOpenHashMap();
   private final Mutable cursor = new Mutable();
   private final Long2LongOpenHashMap redstoneNotified = new Long2LongOpenHashMap();

   public PrimeChunkFinderModule() {
      super("prime-chunk-finder", "Prime Chunk Finder", "Flags chunks with light and redstone changes below Y 0.", ModuleCategory.BASE_RAID);
      BooleanSetting var1 = this.usesNotifications();
      this.sound.visibleWhen(var1::get);
      this.redstoneNotified.defaultReturnValue(-4611686018427387904L);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.reset();
      this.snapshotLoadedChunks();
      this.on(PacketReceiveNettyEvent.TYPE, this::onPacket);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void onPacket(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (packetReceiveNettyEvent.packet instanceof GameJoinS2CPacket) {
         var2.execute(() -> {
            if (this.isEnabled()) {
               this.reset();
            }
         });
      } else if (packetReceiveNettyEvent.packet instanceof ChunkDataS2CPacket var4) {
         int var5 = var4.getChunkX();
         int var6 = var4.getChunkZ();
         LightData var7 = var4.getLightData();
         var2.execute(() -> {
            if (this.isEnabled() && var2.world != null) {
               this.baselineChunk(var5, var6, var7);
            }
         });
      } else if (packetReceiveNettyEvent.packet instanceof LightUpdateS2CPacket var10) {
         int var11 = var10.getChunkX();
         int var12 = var10.getChunkZ();
         LightData var8 = var10.getData();
         var2.execute(() -> {
            if (this.isEnabled() && var2.world != null) {
               this.detectChanges(var11, var12, var8);
            }
         });
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent blockUpdateEvent) {
      if (blockUpdateEvent.oldState != blockUpdateEvent.newState && MinecraftClient.getInstance().world != null) {
         this.rememberGoneLamp(blockUpdateEvent.pos, blockUpdateEvent.oldState, blockUpdateEvent.newState);
         this.detectRedstone(blockUpdateEvent.pos, blockUpdateEvent.oldState, blockUpdateEvent.newState);
      }
   }

   @EventHandler(
      stage = Stage.POST
   )
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world == null) {
         this.reset();
      } else {
         if (var2.player != null && var2.player.getY() < 0.0) {
            this.passedChunks.add(var2.player.getChunkPos().toLong());
         }

         this.forgetOldLamps();
         this.refreshShown(var2.world);
      }
   }

   private void refreshShown(ClientWorld clientWorld) {
      this.shownChunks.clear();
      LongListIterator var2 = this.flaggedChunks.iterator();

      while (var2.hasNext()) {
         long var3 = var2.next();
         if (LevelUtil.isLoaded(clientWorld, ChunkPos.getPackedX(var3), ChunkPos.getPackedZ(var3))) {
            this.shownChunks.add(var3);
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      if (!this.shownChunks.isEmpty() && MinecraftClient.getInstance().world != null) {
         int var2 = this.chunkColor.argb();
         this.chunkOverlay.draw(this.shownChunks, var2, render3DEvent.partialTick);
         if (this.tracers.get()) {
            this.chunkAreas.drawTracers(this.shownChunks, 63.0, ColorHelper.fullAlpha(var2));
         }
      }
   }

   private void flagChunk(long l) {
      this.flaggedChunks.addAndMoveToLast(l);
      this.shownChunks.add(l);

      while (this.flaggedChunks.size() > 256) {
         this.flaggedChunks.removeFirstLong();
      }
   }

   private void snapshotLoadedChunks() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1.world != null && var1.player != null) {
         int var2 = var1.player.getBlockX() >> 4;
         int var3 = var1.player.getBlockZ() >> 4;

         for (int var4 = var2 - 32; var4 <= var2 + 32; var4++) {
            for (int var5 = var3 - 32; var5 <= var3 + 32; var5++) {
               Map var6 = this.snapshotChunk(var4, var5);
               if (var6 != null) {
                  this.chunkLight.put(ChunkPos.toLong(var4, var5), var6);
               }
            }
         }

         this.trimChunkCache();
      }
   }

   private Map<Integer, byte[]> snapshotChunk(int i, int j) {
      ClientWorld var3 = MinecraftClient.getInstance().world;
      if (var3 == null) {
         return null;
      } else {
         ClientChunkManager var4 = var3.getChunkManager();
         if (!var4.isChunkLoaded(i, j)) {
            return null;
         } else {
            ChunkLightingView var5 = var4.getLightingProvider().get(LightType.BLOCK);
            Map var6 = this.createZeroedChunk();

            for (int var7 = this.bottomSection(); var7 <= this.topSection(); var7++) {
               ChunkNibbleArray var8 = var5.getLightSection(ChunkSectionPos.from(i, var7, j));
               if (var8 != null && !var8.isUninitialized(0)) {
                  var6.put(var7, Arrays.copyOf(var8.asByteArray(), 2048));
               }
            }

            return var6;
         }
      }
   }

   private void baselineChunk(int i, int j, LightData lightData) {
      Map var4 = this.createZeroedChunk();
      this.applyInitializedSections(lightData, var4, i, j, false, null);
      this.applyUninitializedSections(lightData, var4, i, j, false, null);
      this.chunkLight.put(ChunkPos.toLong(i, j), var4);
      this.trimChunkCache();
   }

   private void detectChanges(int i, int j, LightData lightData) {
      long var5 = ChunkPos.toLong(i, j);
      this.checkedSections.clear();
      this.amethystSections.clear();
      this.oreSections.clear();
      this.lampSections.clear();
      Map var7 = this.chunkLight.get(var5);
      if (var7 == null) {
         var7 = this.snapshotChunk(i, j);
      }

      boolean var4 = var7 != null && !this.isOwnChunk(var5);
      if (var7 == null) {
         var7 = this.createZeroedChunk();
      }

      PrimeChunkFinderModule.ChangeSummary var9 = new PrimeChunkFinderModule.ChangeSummary();
      this.applyInitializedSections(lightData, var7, i, j, var4, var9);
      this.applyUninitializedSections(lightData, var7, i, j, var4, var9);
      this.chunkLight.put(var5, var7);
      this.trimChunkCache();
      if (var9.count != 0) {
         this.flagChunk(var5);
         if (this.notificationsOn()) {
            ChallengeToast.addOrUpdate(new ItemStack(Items.EMERALD_BLOCK), this.name(), Text.literal(var9.x + ", " + var9.y + ", " + var9.z), this.sound.get());
         }
      }
   }

   private void rememberGoneLamp(BlockPos blockPos, BlockState blockState, BlockState blockState2) {
      if (isLamp(blockState)) {
         int var4 = blockState.getLuminance();
         if (!isLamp(blockState2) || blockState2.getLuminance() < var4) {
            this.goneLamps.put(blockPos.asLong(), Util.getMeasuringTimeMs() + 5000L << 4 | var4);
         }
      }
   }

   private void forgetOldLamps() {
      if (!this.goneLamps.isEmpty()) {
         long var1 = Util.getMeasuringTimeMs();
         LongIterator var3 = this.goneLamps.values().iterator();

         while (var3.hasNext()) {
            if (var3.nextLong() >>> 4 < var1) {
               var3.remove();
            }
         }
      }
   }

   private void detectRedstone(BlockPos blockPos, BlockState blockState, BlockState blockState2) {
      if (!(blockPos.getY() >= 0.0) && (isRedstone(blockState2) || isRedstone(blockState))) {
         long var4 = ChunkPos.toLong(blockPos.getX() >> 4, blockPos.getZ() >> 4);
         if (!this.isOwnChunk(var4)) {
            this.flagChunk(var4);
            long var6 = Util.getMeasuringTimeMs();
            if (this.notificationsOn() && var6 - this.redstoneNotified.get(var4) >= 15000L) {
               this.redstoneNotified.put(var4, var6);
               ChallengeToast.addOrUpdate(
                  new ItemStack(Items.REDSTONE),
                  this.name(),
                  Text.literal("Redstone " + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ()),
                  this.sound.get()
               );
            }
         }
      }
   }

   private static boolean isRedstone(BlockState blockState) {
      Block var1 = blockState.getBlock();
      return var1 instanceof RedstoneWireBlock
         || var1 instanceof AbstractRedstoneGateBlock
         || var1 instanceof RedstoneTorchBlock
         || var1 instanceof RedstoneLampBlock
         || var1 instanceof ObserverBlock
         || var1 instanceof PistonBlock
         || var1 instanceof PistonHeadBlock
         || var1 instanceof PistonExtensionBlock
         || var1 instanceof DispenserBlock
         || var1 instanceof HopperBlock
         || var1 instanceof NoteBlock
         || var1 instanceof DaylightDetectorBlock
         || var1 instanceof LeverBlock
         || var1 instanceof ButtonBlock
         || var1 instanceof AbstractPressurePlateBlock
         || var1 instanceof TripwireHookBlock
         || var1 instanceof TargetBlock
         || var1 instanceof CrafterBlock
         || var1 instanceof RedstoneBlock;
   }

   private boolean isOwnChunk(long l) {
      if (this.passedChunks.contains(l)) {
         return true;
      } else {
         MinecraftClient var3 = MinecraftClient.getInstance();
         return var3.player != null && var3.player.getChunkPos().toLong() == l;
      }
   }

   private void applyInitializedSections(
      LightData lightData, Map<Integer, byte[]> map, int i, int j, boolean bl, PrimeChunkFinderModule.ChangeSummary changeSummary
   ) {
      BitSet var7 = lightData.getInitedBlock();
      List var8 = lightData.getBlockNibbles();
      int var9 = 0;

      for (int var10 = var7.nextSetBit(0); var10 >= 0 && var9 < var8.size(); var10 = var7.nextSetBit(var10 + 1)) {
         byte[] var11 = (byte[])var8.get(var9++);
         int var12 = this.lightSectionY(var10);
         if (this.isWorldSection(var12) && var11.length == 2048) {
            this.updateSection(map, i, j, var12, Arrays.copyOf(var11, 2048), bl, changeSummary);
         }
      }
   }

   private void applyUninitializedSections(
      LightData lightData, Map<Integer, byte[]> map, int i, int j, boolean bl, PrimeChunkFinderModule.ChangeSummary changeSummary
   ) {
      BitSet var7 = lightData.getUninitedBlock();

      for (int var8 = var7.nextSetBit(0); var8 >= 0; var8 = var7.nextSetBit(var8 + 1)) {
         int var9 = this.lightSectionY(var8);
         if (this.isWorldSection(var9)) {
            this.updateSection(map, i, j, var9, ZERO_SECTION, bl, changeSummary);
         }
      }
   }

   private void updateSection(Map<Integer, byte[]> map, int i, int j, int k, byte[] bs, boolean bl, PrimeChunkFinderModule.ChangeSummary changeSummary) {
      byte[] var8 = (byte[])map.get(k);
      if (bl && var8 != null && changeSummary != null) {
         this.compareSection(var8, bs, i, j, k, changeSummary);
      }

      map.put(k, bs);
   }

   private void compareSection(byte[] bs, byte[] cs, int i, int j, int k, PrimeChunkFinderModule.ChangeSummary changeSummary) {
      for (int var7 = 0; var7 < 2048; var7++) {
         int var8 = bs[var7] & 255;
         int var9 = cs[var7] & 255;
         int var10 = var8 ^ var9;
         if ((var10 & 15) != 0) {
            this.recordChange(i, j, k, var7 << 1, var8 & 15, var9 & 15, changeSummary);
         }

         if ((var10 & 240) != 0) {
            this.recordChange(i, j, k, (var7 << 1) + 1, var8 >>> 4 & 15, var9 >>> 4 & 15, changeSummary);
         }
      }
   }

   private void recordChange(int i, int j, int k, int l, int m, int n, PrimeChunkFinderModule.ChangeSummary changeSummary) {
      int var8 = (i << 4) + (l & 15);
      int var9 = (k << 4) + (l >>> 8 & 15);
      int var10 = (j << 4) + (l >>> 4 & 15);
      int var11 = Math.max(m, n);
      if (!this.fromAmethyst(var8, var9, var10, m, n) && !this.fromRedstoneOre(var8, var9, var10, var11) && !this.fromLamp(var8, var9, var10, var11)) {
         changeSummary.add(var8, var9, var10, m, n);
      }
   }

   private boolean fromLamp(int i, int j, int k, int l) {
      if (l > 14) {
         return false;
      } else {
         ClientWorld var5 = MinecraftClient.getInstance().world;
         if (var5 == null) {
            return false;
         } else {
            int var6 = 14 - l;

            for (int var7 = i - var6 >> 4; var7 <= i + var6 >> 4; var7++) {
               for (int var8 = j - var6 >> 4; var8 <= j + var6 >> 4; var8++) {
                  for (int var9 = k - var6 >> 4; var9 <= k + var6 >> 4; var9++) {
                     for (int var13 : this.lamps(var5, var7, var8, var9)) {
                        int var14 = var13 >>> 4;
                        int var15 = (var7 << 4) + (var14 & 15);
                        int var16 = (var8 << 4) + (var14 >>> 8);
                        int var17 = (var9 << 4) + (var14 >>> 4 & 15);
                        if (Math.abs(var15 - i) + Math.abs(var16 - j) + Math.abs(var17 - k) <= (var13 & 15) - l) {
                           return true;
                        }
                     }
                  }
               }
            }

            long var18 = Util.getMeasuringTimeMs();

            for (Entry var20 : this.goneLamps.long2LongEntrySet()) {
               long var22 = var20.getLongValue();
               long var21;
               if (var22 >>> 4 >= var18
                  && Math.abs(BlockPos.unpackLongX(var21 = var20.getLongKey()) - i)
                        + Math.abs(BlockPos.unpackLongY(var21) - j)
                        + Math.abs(BlockPos.unpackLongZ(var21) - k)
                     <= (int)(var22 & 15L) - l) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private int[] lamps(ClientWorld clientWorld, int i, int j, int k) {
      long var5 = ChunkSectionPos.asLong(i, j, k);
      int[] var7 = this.lampSections.get(var5);
      if (var7 != null) {
         return var7;
      } else {
         int[] var8 = scanLamps(clientWorld, i, j, k);
         this.lampSections.put(var5, var8);
         return var8;
      }
   }

   private static int[] scanLamps(ClientWorld clientWorld, int i, int j, int k) {
      if (j >= clientWorld.getBottomSectionCoord() && j <= clientWorld.getTopSectionCoord()) {
         WorldChunk var4 = clientWorld.getChunkManager().getChunk(i, k, ChunkStatus.FULL, false);
         if (var4 == null) {
            return NO_LAMPS;
         } else {
            ChunkSection var5 = var4.getSection(var4.sectionCoordToIndex(j));
            if (!var5.isEmpty() && var5.getBlockStateContainer().hasAny(PrimeChunkFinderModule::isLamp)) {
               IntArrayList var6 = new IntArrayList();

               for (int var7 = 0; var7 < 4096; var7++) {
                  BlockState var8 = var5.getBlockState(var7 & 15, var7 >>> 8, var7 >>> 4 & 15);
                  if (isLamp(var8)) {
                     var6.add(var7 << 4 | var8.getLuminance());
                  }
               }

               return var6.isEmpty() ? NO_LAMPS : var6.toIntArray();
            } else {
               return NO_LAMPS;
            }
         }
      } else {
         return NO_LAMPS;
      }
   }

   private static boolean isLamp(BlockState blockState) {
      return blockState.isOf(Blocks.TORCH)
         || blockState.isOf(Blocks.WALL_TORCH)
         || blockState.isOf(Blocks.SOUL_TORCH)
         || blockState.isOf(Blocks.SOUL_WALL_TORCH)
         || blockState.isOf(Blocks.COPPER_TORCH)
         || blockState.isOf(Blocks.COPPER_WALL_TORCH)
         || CaveVines.hasBerries(blockState);
   }

   private boolean fromAmethyst(int i, int j, int k, int l, int m) {
      if (Math.max(l, m) > 5) {
         return false;
      } else {
         ClientWorld var6 = MinecraftClient.getInstance().world;
         if (var6 != null && this.amethystSectionNear(var6, i, j, k)) {
            for (int var7 = -5; var7 <= 5; var7++) {
               int var8 = 5 - Math.abs(var7);

               for (int var9 = -var8; var9 <= var8; var9++) {
                  int var10 = var8 - Math.abs(var9);

                  for (int var11 = -var10; var11 <= var10; var11++) {
                     if (isAmethyst(var6.getBlockState(this.cursor.set(i + var7, j + var9, k + var11)))) {
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
   }

   private boolean amethystSectionNear(ClientWorld clientWorld, int i, int j, int k) {
      for (int var5 = i - 5 >> 4; var5 <= i + 5 >> 4; var5++) {
         for (int var6 = j - 5 >> 4; var6 <= j + 5 >> 4; var6++) {
            for (int var7 = k - 5 >> 4; var7 <= k + 5 >> 4; var7++) {
               long var8 = ChunkSectionPos.asLong(var5, var6, var7);
               if (this.checkedSections.add(var8) && sectionHasAmethyst(clientWorld, var5, var6, var7)) {
                  this.amethystSections.add(var8);
               }

               if (this.amethystSections.contains(var8)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static boolean sectionHasAmethyst(ClientWorld clientWorld, int i, int j, int k) {
      if (j >= clientWorld.getBottomSectionCoord() && j <= clientWorld.getTopSectionCoord()) {
         WorldChunk var4 = clientWorld.getChunkManager().getChunk(i, k, ChunkStatus.FULL, false);
         if (var4 == null) {
            return false;
         } else {
            ChunkSection var5 = var4.getSection(var4.sectionCoordToIndex(j));
            return !var5.isEmpty() && var5.getBlockStateContainer().hasAny(PrimeChunkFinderModule::isAmethyst);
         }
      } else {
         return false;
      }
   }

   private boolean fromRedstoneOre(int i, int j, int k, int l) {
      if (l > 9) {
         return false;
      } else {
         ClientWorld var5 = MinecraftClient.getInstance().world;
         if (var5 == null) {
            return false;
         } else {
            int var6 = 9 - l;

            for (int var7 = i - var6 >> 4; var7 <= i + var6 >> 4; var7++) {
               for (int var8 = j - var6 >> 4; var8 <= j + var6 >> 4; var8++) {
                  for (int var9 = k - var6 >> 4; var9 <= k + var6 >> 4; var9++) {
                     for (long var13 : this.exposedOres(var5, var7, var8, var9)) {
                        int var15 = Math.abs(BlockPos.unpackLongX(var13) - i)
                           + Math.abs(BlockPos.unpackLongY(var13) - j)
                           + Math.abs(BlockPos.unpackLongZ(var13) - k);
                        if (var15 <= var6) {
                           return true;
                        }
                     }
                  }
               }
            }

            return false;
         }
      }
   }

   private long[] exposedOres(ClientWorld clientWorld, int i, int j, int k) {
      long var5 = ChunkSectionPos.asLong(i, j, k);
      long[] var7 = this.oreSections.get(var5);
      if (var7 != null) {
         return var7;
      } else {
         long[] var8 = this.scanExposedOres(clientWorld, i, j, k);
         this.oreSections.put(var5, var8);
         return var8;
      }
   }

   private long[] scanExposedOres(ClientWorld clientWorld, int i, int j, int k) {
      if (j >= clientWorld.getBottomSectionCoord() && j <= clientWorld.getTopSectionCoord()) {
         WorldChunk var5 = clientWorld.getChunkManager().getChunk(i, k, ChunkStatus.FULL, false);
         if (var5 == null) {
            return NO_ORES;
         } else {
            ChunkSection var6 = var5.getSection(var5.sectionCoordToIndex(j));
            if (!var6.isEmpty() && var6.getBlockStateContainer().hasAny(PrimeChunkFinderModule::isRedstoneOre)) {
               LongArrayList var7 = new LongArrayList();

               for (int var8 = 0; var8 < 16; var8++) {
                  for (int var9 = 0; var9 < 16; var9++) {
                     for (int var10 = 0; var10 < 16; var10++) {
                        int var11;
                        int var12;
                        int var13;
                        if (isRedstoneOre(var6.getBlockState(var10, var8, var9))
                           && this.exposed(clientWorld, var13 = (i << 4) + var10, var12 = (j << 4) + var8, var11 = (k << 4) + var9)) {
                           var7.add(BlockPos.asLong(var13, var12, var11));
                        }
                     }
                  }
               }

               return var7.isEmpty() ? NO_ORES : var7.toLongArray();
            } else {
               return NO_ORES;
            }
         }
      } else {
         return NO_ORES;
      }
   }

   private boolean exposed(ClientWorld clientWorld, int i, int j, int k) {
      for (Direction var8 : Direction.values()) {
         BlockState var9 = clientWorld.getBlockState(this.cursor.set(i + var8.getOffsetX(), j + var8.getOffsetY(), k + var8.getOffsetZ()));
         if (var9.isAir() || !var9.isOpaque()) {
            return true;
         }
      }

      return false;
   }

   private static boolean isRedstoneOre(BlockState blockState) {
      return blockState.isOf(Blocks.REDSTONE_ORE) || blockState.isOf(Blocks.DEEPSLATE_REDSTONE_ORE);
   }

   private static boolean isAmethyst(BlockState blockState) {
      return blockState.isOf(Blocks.AMETHYST_CLUSTER)
         || blockState.isOf(Blocks.LARGE_AMETHYST_BUD)
         || blockState.isOf(Blocks.MEDIUM_AMETHYST_BUD)
         || blockState.isOf(Blocks.SMALL_AMETHYST_BUD)
         || blockState.isOf(Blocks.BUDDING_AMETHYST)
         || blockState.isOf(Blocks.AMETHYST_BLOCK);
   }

   private Map<Integer, byte[]> createZeroedChunk() {
      HashMap var1 = new HashMap();

      for (int var2 = this.bottomSection(); var2 <= this.topSection(); var2++) {
         var1.put(var2, ZERO_SECTION);
      }

      return var1;
   }

   private int lightSectionY(int i) {
      ClientWorld var2 = MinecraftClient.getInstance().world;
      return var2 == null ? -2147483648 : var2.getChunkManager().getLightingProvider().getBottomY() + i;
   }

   private int bottomSection() {
      return MinecraftClient.getInstance().world.getBottomSectionCoord();
   }

   private int topSection() {
      return MinecraftClient.getInstance().world.getTopSectionCoord();
   }

   private boolean isWorldSection(int i) {
      return i >= this.bottomSection() && i <= this.topSection();
   }

   private void trimChunkCache() {
      while (this.chunkLight.size() > 5000) {
         Iterator var1 = this.chunkLight.keySet().iterator();
         var1.next();
         var1.remove();
      }
   }

   private void reset() {
      this.chunkLight.clear();
      this.passedChunks.clear();
      this.flaggedChunks.clear();
      this.shownChunks.clear();
      this.redstoneNotified.clear();
      this.lampSections.clear();
      this.goneLamps.clear();
   }

   @Environment(EnvType.CLIENT)
   private static final class ChangeSummary {
      int count;
      int x;
      int y;
      int z;
      int oldValue;
      int newValue;

      private ChangeSummary() {
      }

      void add(int i, int j, int k, int l, int m) {
         if (this.count == 0 || Math.abs(m - l) > Math.abs(this.newValue - this.oldValue)) {
            this.x = i;
            this.y = j;
            this.z = k;
            this.oldValue = l;
            this.newValue = m;
         }

         this.count++;
      }
   }
}
