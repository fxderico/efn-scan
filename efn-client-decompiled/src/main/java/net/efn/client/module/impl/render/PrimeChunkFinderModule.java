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
import it.unimi.dsi.fastutil.objects.ObjectIterator;
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
   private final Long2ObjectOpenHashMap<long[]> oreSections = new Long2ObjectOpenHashMap();
   private final Long2ObjectOpenHashMap<int[]> lampSections = new Long2ObjectOpenHashMap();
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

   private void onPacket(PacketReceiveNettyEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var1.packet instanceof GameJoinS2CPacket) {
         var2.execute(() -> {
            if (this.isEnabled()) {
               this.reset();
            }
         });
      } else if (var1.packet instanceof ChunkDataS2CPacket var4) {
         int var5 = var4.method_11523();
         int var6 = var4.method_11524();
         LightData var7 = var4.method_38599();
         var2.execute(() -> {
            if (this.isEnabled() && var2.field_1687 != null) {
               this.baselineChunk(var5, var6, var7);
            }
         });
      } else if (var1.packet instanceof LightUpdateS2CPacket var10) {
         int var11 = var10.method_11558();
         int var12 = var10.method_11554();
         LightData var8 = var10.method_38600();
         var2.execute(() -> {
            if (this.isEnabled() && var2.field_1687 != null) {
               this.detectChanges(var11, var12, var8);
            }
         });
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent var1) {
      if (var1.oldState != var1.newState && MinecraftClient.method_1551().field_1687 != null) {
         this.rememberGoneLamp(var1.pos, var1.oldState, var1.newState);
         this.detectRedstone(var1.pos, var1.oldState, var1.newState);
      }
   }

   @EventHandler(
      stage = Stage.POST
   )
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1687 == null) {
         this.reset();
      } else {
         if (var2.field_1724 != null && var2.field_1724.method_23318() < 0.0) {
            this.passedChunks.add(var2.field_1724.method_31476().method_8324());
         }

         this.forgetOldLamps();
         this.refreshShown(var2.field_1687);
      }
   }

   private void refreshShown(ClientWorld var1) {
      this.shownChunks.clear();
      LongListIterator var2 = this.flaggedChunks.iterator();

      while (var2.hasNext()) {
         long var3 = var2.next();
         if (LevelUtil.isLoaded(var1, ChunkPos.method_8325(var3), ChunkPos.method_8332(var3))) {
            this.shownChunks.add(var3);
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      if (!this.shownChunks.isEmpty() && MinecraftClient.method_1551().field_1687 != null) {
         int var2 = this.chunkColor.argb();
         this.chunkOverlay.draw(this.shownChunks, var2, var1.partialTick);
         if (this.tracers.get()) {
            this.chunkAreas.drawTracers(this.shownChunks, 63.0, ColorHelper.method_61334(var2));
         }
      }
   }

   private void flagChunk(long var1) {
      this.flaggedChunks.addAndMoveToLast(var1);
      this.shownChunks.add(var1);

      while (this.flaggedChunks.size() > 256) {
         this.flaggedChunks.removeFirstLong();
      }
   }

   private void snapshotLoadedChunks() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (var1.field_1687 != null && var1.field_1724 != null) {
         int var2 = var1.field_1724.method_31477() >> 4;
         int var3 = var1.field_1724.method_31479() >> 4;

         for (int var4 = var2 - 32; var4 <= var2 + 32; var4++) {
            for (int var5 = var3 - 32; var5 <= var3 + 32; var5++) {
               Map var6 = this.snapshotChunk(var4, var5);
               if (var6 != null) {
                  this.chunkLight.put(ChunkPos.method_8331(var4, var5), var6);
               }
            }
         }

         this.trimChunkCache();
      }
   }

   private Map<Integer, byte[]> snapshotChunk(int var1, int var2) {
      ClientWorld var3 = MinecraftClient.method_1551().field_1687;
      if (var3 == null) {
         return null;
      } else {
         ClientChunkManager var4 = var3.method_2935();
         if (!var4.method_12123(var1, var2)) {
            return null;
         } else {
            ChunkLightingView var5 = var4.method_12130().method_15562(LightType.field_9282);
            Map var6 = this.createZeroedChunk();

            for (int var7 = this.bottomSection(); var7 <= this.topSection(); var7++) {
               ChunkNibbleArray var8 = var5.method_15544(ChunkSectionPos.method_18676(var1, var7, var2));
               if (var8 != null && !var8.method_51379(0)) {
                  var6.put(var7, Arrays.copyOf(var8.method_12137(), 2048));
               }
            }

            return var6;
         }
      }
   }

   private void baselineChunk(int var1, int var2, LightData var3) {
      Map var4 = this.createZeroedChunk();
      this.applyInitializedSections(var3, var4, var1, var2, false, null);
      this.applyUninitializedSections(var3, var4, var1, var2, false, null);
      this.chunkLight.put(ChunkPos.method_8331(var1, var2), var4);
      this.trimChunkCache();
   }

   private void detectChanges(int var1, int var2, LightData var3) {
      long var5 = ChunkPos.method_8331(var1, var2);
      this.checkedSections.clear();
      this.amethystSections.clear();
      this.oreSections.clear();
      this.lampSections.clear();
      Map var7 = this.chunkLight.get(var5);
      if (var7 == null) {
         var7 = this.snapshotChunk(var1, var2);
      }

      boolean var4 = var7 != null && !this.isOwnChunk(var5);
      if (var7 == null) {
         var7 = this.createZeroedChunk();
      }

      PrimeChunkFinderModule.ChangeSummary var9 = new PrimeChunkFinderModule.ChangeSummary();
      this.applyInitializedSections(var3, var7, var1, var2, var4, var9);
      this.applyUninitializedSections(var3, var7, var1, var2, var4, var9);
      this.chunkLight.put(var5, var7);
      this.trimChunkCache();
      if (var9.count != 0) {
         this.flagChunk(var5);
         if (this.notificationsOn()) {
            ChallengeToast.addOrUpdate(
               new ItemStack(Items.field_8733), this.name(), Text.method_43470(var9.x + ", " + var9.y + ", " + var9.z), this.sound.get()
            );
         }
      }
   }

   private void rememberGoneLamp(BlockPos var1, BlockState var2, BlockState var3) {
      if (isLamp(var2)) {
         int var4 = var2.method_26213();
         if (!isLamp(var3) || var3.method_26213() < var4) {
            this.goneLamps.put(var1.method_10063(), Util.method_658() + 5000L << 4 | var4);
         }
      }
   }

   private void forgetOldLamps() {
      if (!this.goneLamps.isEmpty()) {
         long var1 = Util.method_658();
         LongIterator var3 = this.goneLamps.values().iterator();

         while (var3.hasNext()) {
            if (var3.nextLong() >>> 4 < var1) {
               var3.remove();
            }
         }
      }
   }

   private void detectRedstone(BlockPos var1, BlockState var2, BlockState var3) {
      if (!(var1.method_10264() >= 0.0) && (isRedstone(var3) || isRedstone(var2))) {
         long var4 = ChunkPos.method_8331(var1.method_10263() >> 4, var1.method_10260() >> 4);
         if (!this.isOwnChunk(var4)) {
            this.flagChunk(var4);
            long var6 = Util.method_658();
            if (this.notificationsOn() && var6 - this.redstoneNotified.get(var4) >= 15000L) {
               this.redstoneNotified.put(var4, var6);
               ChallengeToast.addOrUpdate(
                  new ItemStack(Items.field_8725),
                  this.name(),
                  Text.method_43470("Redstone " + var1.method_10263() + ", " + var1.method_10264() + ", " + var1.method_10260()),
                  this.sound.get()
               );
            }
         }
      }
   }

   private static boolean isRedstone(BlockState var0) {
      Block var1 = var0.method_26204();
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

   private boolean isOwnChunk(long var1) {
      if (this.passedChunks.contains(var1)) {
         return true;
      } else {
         MinecraftClient var3 = MinecraftClient.method_1551();
         return var3.field_1724 != null && var3.field_1724.method_31476().method_8324() == var1;
      }
   }

   private void applyInitializedSections(LightData var1, Map<Integer, byte[]> var2, int var3, int var4, boolean var5, PrimeChunkFinderModule.ChangeSummary var6) {
      BitSet var7 = var1.method_38608();
      List var8 = var1.method_38610();
      int var9 = 0;

      for (int var10 = var7.nextSetBit(0); var10 >= 0 && var9 < var8.size(); var10 = var7.nextSetBit(var10 + 1)) {
         byte[] var11 = (byte[])var8.get(var9++);
         int var12 = this.lightSectionY(var10);
         if (this.isWorldSection(var12) && var11.length == 2048) {
            this.updateSection(var2, var3, var4, var12, Arrays.copyOf(var11, 2048), var5, var6);
         }
      }
   }

   private void applyUninitializedSections(
      LightData var1, Map<Integer, byte[]> var2, int var3, int var4, boolean var5, PrimeChunkFinderModule.ChangeSummary var6
   ) {
      BitSet var7 = var1.method_38609();

      for (int var8 = var7.nextSetBit(0); var8 >= 0; var8 = var7.nextSetBit(var8 + 1)) {
         int var9 = this.lightSectionY(var8);
         if (this.isWorldSection(var9)) {
            this.updateSection(var2, var3, var4, var9, ZERO_SECTION, var5, var6);
         }
      }
   }

   private void updateSection(Map<Integer, byte[]> var1, int var2, int var3, int var4, byte[] var5, boolean var6, PrimeChunkFinderModule.ChangeSummary var7) {
      byte[] var8 = (byte[])var1.get(var4);
      if (var6 && var8 != null && var7 != null) {
         this.compareSection(var8, var5, var2, var3, var4, var7);
      }

      var1.put(var4, var5);
   }

   private void compareSection(byte[] var1, byte[] var2, int var3, int var4, int var5, PrimeChunkFinderModule.ChangeSummary var6) {
      for (int var7 = 0; var7 < 2048; var7++) {
         int var8 = var1[var7] & 255;
         int var9 = var2[var7] & 255;
         int var10 = var8 ^ var9;
         if ((var10 & 15) != 0) {
            this.recordChange(var3, var4, var5, var7 << 1, var8 & 15, var9 & 15, var6);
         }

         if ((var10 & 240) != 0) {
            this.recordChange(var3, var4, var5, (var7 << 1) + 1, var8 >>> 4 & 15, var9 >>> 4 & 15, var6);
         }
      }
   }

   private void recordChange(int var1, int var2, int var3, int var4, int var5, int var6, PrimeChunkFinderModule.ChangeSummary var7) {
      int var8 = (var1 << 4) + (var4 & 15);
      int var9 = (var3 << 4) + (var4 >>> 8 & 15);
      int var10 = (var2 << 4) + (var4 >>> 4 & 15);
      int var11 = Math.max(var5, var6);
      if (!this.fromAmethyst(var8, var9, var10, var5, var6) && !this.fromRedstoneOre(var8, var9, var10, var11) && !this.fromLamp(var8, var9, var10, var11)) {
         var7.add(var8, var9, var10, var5, var6);
      }
   }

   private boolean fromLamp(int var1, int var2, int var3, int var4) {
      if (var4 > 14) {
         return false;
      } else {
         ClientWorld var5 = MinecraftClient.method_1551().field_1687;
         if (var5 == null) {
            return false;
         } else {
            int var6 = 14 - var4;

            for (int var7 = var1 - var6 >> 4; var7 <= var1 + var6 >> 4; var7++) {
               for (int var8 = var2 - var6 >> 4; var8 <= var2 + var6 >> 4; var8++) {
                  for (int var9 = var3 - var6 >> 4; var9 <= var3 + var6 >> 4; var9++) {
                     for (int var13 : this.lamps(var5, var7, var8, var9)) {
                        int var14 = var13 >>> 4;
                        int var15 = (var7 << 4) + (var14 & 15);
                        int var16 = (var8 << 4) + (var14 >>> 8);
                        int var17 = (var9 << 4) + (var14 >>> 4 & 15);
                        if (Math.abs(var15 - var1) + Math.abs(var16 - var2) + Math.abs(var17 - var3) <= (var13 & 15) - var4) {
                           return true;
                        }
                     }
                  }
               }
            }

            long var18 = Util.method_658();
            ObjectIterator var19 = this.goneLamps.long2LongEntrySet().iterator();

            while (var19.hasNext()) {
               Entry var20 = (Entry)var19.next();
               long var22 = var20.getLongValue();
               long var21;
               if (var22 >>> 4 >= var18
                  && Math.abs(BlockPos.method_10061(var21 = var20.getLongKey()) - var1)
                        + Math.abs(BlockPos.method_10071(var21) - var2)
                        + Math.abs(BlockPos.method_10083(var21) - var3)
                     <= (int)(var22 & 15L) - var4) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private int[] lamps(ClientWorld var1, int var2, int var3, int var4) {
      long var5 = ChunkSectionPos.method_18685(var2, var3, var4);
      int[] var7 = (int[])this.lampSections.get(var5);
      if (var7 != null) {
         return var7;
      } else {
         int[] var8 = scanLamps(var1, var2, var3, var4);
         this.lampSections.put(var5, var8);
         return var8;
      }
   }

   private static int[] scanLamps(ClientWorld var0, int var1, int var2, int var3) {
      if (var2 >= var0.method_32891() && var2 <= var0.method_31597()) {
         WorldChunk var4 = var0.method_2935().method_2857(var1, var3, ChunkStatus.field_12803, false);
         if (var4 == null) {
            return NO_LAMPS;
         } else {
            ChunkSection var5 = var4.method_38259(var4.method_31603(var2));
            if (!var5.method_38292() && var5.method_12265().method_19526(PrimeChunkFinderModule::isLamp)) {
               IntArrayList var6 = new IntArrayList();

               for (int var7 = 0; var7 < 4096; var7++) {
                  BlockState var8 = var5.method_12254(var7 & 15, var7 >>> 8, var7 >>> 4 & 15);
                  if (isLamp(var8)) {
                     var6.add(var7 << 4 | var8.method_26213());
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

   private static boolean isLamp(BlockState var0) {
      return var0.method_27852(Blocks.field_10336)
         || var0.method_27852(Blocks.field_10099)
         || var0.method_27852(Blocks.field_22092)
         || var0.method_27852(Blocks.field_22093)
         || var0.method_27852(Blocks.field_61902)
         || var0.method_27852(Blocks.field_61903)
         || CaveVines.method_33618(var0);
   }

   private boolean fromAmethyst(int var1, int var2, int var3, int var4, int var5) {
      if (Math.max(var4, var5) > 5) {
         return false;
      } else {
         ClientWorld var6 = MinecraftClient.method_1551().field_1687;
         if (var6 != null && this.amethystSectionNear(var6, var1, var2, var3)) {
            for (int var7 = -5; var7 <= 5; var7++) {
               int var8 = 5 - Math.abs(var7);

               for (int var9 = -var8; var9 <= var8; var9++) {
                  int var10 = var8 - Math.abs(var9);

                  for (int var11 = -var10; var11 <= var10; var11++) {
                     if (isAmethyst(var6.method_8320(this.cursor.method_10103(var1 + var7, var2 + var9, var3 + var11)))) {
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

   private boolean amethystSectionNear(ClientWorld var1, int var2, int var3, int var4) {
      for (int var5 = var2 - 5 >> 4; var5 <= var2 + 5 >> 4; var5++) {
         for (int var6 = var3 - 5 >> 4; var6 <= var3 + 5 >> 4; var6++) {
            for (int var7 = var4 - 5 >> 4; var7 <= var4 + 5 >> 4; var7++) {
               long var8 = ChunkSectionPos.method_18685(var5, var6, var7);
               if (this.checkedSections.add(var8) && sectionHasAmethyst(var1, var5, var6, var7)) {
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

   private static boolean sectionHasAmethyst(ClientWorld var0, int var1, int var2, int var3) {
      if (var2 >= var0.method_32891() && var2 <= var0.method_31597()) {
         WorldChunk var4 = var0.method_2935().method_2857(var1, var3, ChunkStatus.field_12803, false);
         if (var4 == null) {
            return false;
         } else {
            ChunkSection var5 = var4.method_38259(var4.method_31603(var2));
            return !var5.method_38292() && var5.method_12265().method_19526(PrimeChunkFinderModule::isAmethyst);
         }
      } else {
         return false;
      }
   }

   private boolean fromRedstoneOre(int var1, int var2, int var3, int var4) {
      if (var4 > 9) {
         return false;
      } else {
         ClientWorld var5 = MinecraftClient.method_1551().field_1687;
         if (var5 == null) {
            return false;
         } else {
            int var6 = 9 - var4;

            for (int var7 = var1 - var6 >> 4; var7 <= var1 + var6 >> 4; var7++) {
               for (int var8 = var2 - var6 >> 4; var8 <= var2 + var6 >> 4; var8++) {
                  for (int var9 = var3 - var6 >> 4; var9 <= var3 + var6 >> 4; var9++) {
                     for (long var13 : this.exposedOres(var5, var7, var8, var9)) {
                        int var15 = Math.abs(BlockPos.method_10061(var13) - var1)
                           + Math.abs(BlockPos.method_10071(var13) - var2)
                           + Math.abs(BlockPos.method_10083(var13) - var3);
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

   private long[] exposedOres(ClientWorld var1, int var2, int var3, int var4) {
      long var5 = ChunkSectionPos.method_18685(var2, var3, var4);
      long[] var7 = (long[])this.oreSections.get(var5);
      if (var7 != null) {
         return var7;
      } else {
         long[] var8 = this.scanExposedOres(var1, var2, var3, var4);
         this.oreSections.put(var5, var8);
         return var8;
      }
   }

   private long[] scanExposedOres(ClientWorld var1, int var2, int var3, int var4) {
      if (var3 >= var1.method_32891() && var3 <= var1.method_31597()) {
         WorldChunk var5 = var1.method_2935().method_2857(var2, var4, ChunkStatus.field_12803, false);
         if (var5 == null) {
            return NO_ORES;
         } else {
            ChunkSection var6 = var5.method_38259(var5.method_31603(var3));
            if (!var6.method_38292() && var6.method_12265().method_19526(PrimeChunkFinderModule::isRedstoneOre)) {
               LongArrayList var7 = new LongArrayList();

               for (int var8 = 0; var8 < 16; var8++) {
                  for (int var9 = 0; var9 < 16; var9++) {
                     for (int var10 = 0; var10 < 16; var10++) {
                        int var11;
                        int var12;
                        int var13;
                        if (isRedstoneOre(var6.method_12254(var10, var8, var9))
                           && this.exposed(var1, var13 = (var2 << 4) + var10, var12 = (var3 << 4) + var8, var11 = (var4 << 4) + var9)) {
                           var7.add(BlockPos.method_10064(var13, var12, var11));
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

   private boolean exposed(ClientWorld var1, int var2, int var3, int var4) {
      for (Direction var8 : Direction.values()) {
         BlockState var9 = var1.method_8320(this.cursor.method_10103(var2 + var8.method_10148(), var3 + var8.method_10164(), var4 + var8.method_10165()));
         if (var9.method_26215() || !var9.method_26225()) {
            return true;
         }
      }

      return false;
   }

   private static boolean isRedstoneOre(BlockState var0) {
      return var0.method_27852(Blocks.field_10080) || var0.method_27852(Blocks.field_29030);
   }

   private static boolean isAmethyst(BlockState var0) {
      return var0.method_27852(Blocks.field_27161)
         || var0.method_27852(Blocks.field_27162)
         || var0.method_27852(Blocks.field_27163)
         || var0.method_27852(Blocks.field_27164)
         || var0.method_27852(Blocks.field_27160)
         || var0.method_27852(Blocks.field_27159);
   }

   private Map<Integer, byte[]> createZeroedChunk() {
      HashMap var1 = new HashMap();

      for (int var2 = this.bottomSection(); var2 <= this.topSection(); var2++) {
         var1.put(var2, ZERO_SECTION);
      }

      return var1;
   }

   private int lightSectionY(int var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      return var2 == null ? -2147483648 : var2.method_2935().method_12130().method_31929() + var1;
   }

   private int bottomSection() {
      return MinecraftClient.method_1551().field_1687.method_32891();
   }

   private int topSection() {
      return MinecraftClient.method_1551().field_1687.method_31597();
   }

   private boolean isWorldSection(int var1) {
      return var1 >= this.bottomSection() && var1 <= this.topSection();
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

      void add(int var1, int var2, int var3, int var4, int var5) {
         if (this.count == 0 || Math.abs(var5 - var4) > Math.abs(this.newValue - this.oldValue)) {
            this.x = var1;
            this.y = var2;
            this.z = var3;
            this.oldValue = var4;
            this.newValue = var5;
         }

         this.count++;
      }
   }
}
