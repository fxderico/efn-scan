package net.efn.client.module.impl.misc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.util.LevelUtil;
import net.efn.client.util.quantum.ChunkSnapshotStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public final class QuantumModule extends ClientModule {
   private static final int SCAN_RADIUS = 8;
   private static final int FLAG_RADIUS = 9;
   private static final double MARKER_Y = 63.0;
   private static final float LINE_WIDTH = 1.5F;
   private static final int CYAN = 65535;
   private static final int SOURCE_FILL_ALPHA = 40;
   private static final Set<Block> IGNORED_NEW_BLOCKS = Set.of(
      Blocks.FIRE,
      Blocks.FROSTED_ICE,
      Blocks.COAL_ORE,
      Blocks.DEEPSLATE_COAL_ORE,
      Blocks.IRON_ORE,
      Blocks.DEEPSLATE_IRON_ORE,
      Blocks.COPPER_ORE,
      Blocks.DEEPSLATE_COPPER_ORE,
      Blocks.GOLD_ORE,
      Blocks.DEEPSLATE_GOLD_ORE,
      Blocks.REDSTONE_ORE,
      Blocks.DEEPSLATE_REDSTONE_ORE,
      Blocks.EMERALD_ORE,
      Blocks.DEEPSLATE_EMERALD_ORE,
      Blocks.LAPIS_ORE,
      Blocks.DEEPSLATE_LAPIS_ORE,
      Blocks.DIAMOND_ORE,
      Blocks.DEEPSLATE_DIAMOND_ORE,
      Blocks.OAK_LEAVES,
      Blocks.ACACIA_LEAVES,
      Blocks.BIRCH_LEAVES,
      Blocks.JUNGLE_LEAVES,
      Blocks.DARK_OAK_LEAVES,
      Blocks.PALE_OAK_LEAVES,
      Blocks.CHERRY_LEAVES,
      Blocks.AZALEA_LEAVES,
      Blocks.FLOWERING_AZALEA_LEAVES,
      Blocks.MOSSY_COBBLESTONE,
      Blocks.RAW_COPPER_BLOCK,
      Blocks.STONE,
      Blocks.OBSERVER,
      Blocks.REPEATER,
      Blocks.COMPARATOR,
      Blocks.REDSTONE_TORCH,
      Blocks.REDSTONE_LAMP,
      Blocks.REDSTONE_WIRE,
      Blocks.DROPPER
   );
   private static final Set<Block> IGNORED_OLD_BLOCKS = Set.of(Blocks.AMETHYST_BLOCK, Blocks.STONE, Blocks.FROSTED_ICE);
   private final BooleanSetting renderChunks = this.addSetting("render-chunks", new BooleanSetting("Render Chunks", true));
   private final BooleanSetting clearOnToggle = this.addSetting("clear-chunks-on-toggle", new BooleanSetting("Clear On Toggle", true));
   private final BooleanSetting highlightSources = this.addSetting("render-flagged-chunk", new BooleanSetting("Highlight Source Chunks", true));
   private final ColorSetting chunkFill = this.addSetting("chunk-side-color", new ColorSetting("Chunk Fill Color", 687810600));
   private final ColorSetting chunkLine = this.addSetting("chunk-line-color", new ColorSetting("Chunk Line Color", -55256));
   private final BooleanSetting advancedDebug = this.addSetting("advanced-debug", new BooleanSetting("Advanced Debug", false));
   private final BooleanSetting render = this.addSetting("render", new BooleanSetting("Debug Render", true));
   private final ColorSetting fillColor = this.addSetting("side-color", new ColorSetting("Debug Fill Color", 687810600));
   private final ColorSetting lineColor = this.addSetting("line-color", new ColorSetting("Debug Line Color", -55256));
   private final ChunkSnapshotStore snapshots = new ChunkSnapshotStore(Blocks.AIR.getDefaultState());
   private final Set<ChunkPos> justSent = new HashSet<>();
   private final List<BlockPos> changedBlocks = new ArrayList<>();
   private final Set<ChunkPos> flaggedChunks = new HashSet<>();
   private final Set<ChunkPos> sourceChunks = new HashSet<>();
   private Set<ChunkPos> previousRange = Set.of();
   private ChunkPos lastPos;
   private ClientWorld sessionLevel;
   private ClientPlayNetworkHandler connection;

   public QuantumModule() {
      super("quantum", "Quantum & Render", "Finds player activity in resent chunks and marks the area around it.", ModuleCategory.BASE_RAID);
      this.usesNotifications().set(false);
      this.highlightSources.visibleWhen(this.renderChunks::get);
      this.chunkFill.visibleWhen(this.renderChunks::get);
      this.chunkLine.visibleWhen(this.renderChunks::get);
      this.render.visibleWhen(this.advancedDebug::get);
      this.fillColor.visibleWhen(() -> this.render.get() && this.advancedDebug.get());
      this.lineColor.visibleWhen(() -> this.render.get() && this.advancedDebug.get());
   }

   @Override
   protected void onEnable() {
      this.sessionLevel = null;
      this.resetSession();
      if (this.clearOnToggle.get()) {
         this.flaggedChunks.clear();
         this.sourceChunks.clear();
      }
   }

   @Override
   protected void onDisable() {
      this.snapshots.clear();
   }

   private void resetSession() {
      this.snapshots.clear();
      this.justSent.clear();
      this.changedBlocks.clear();
      this.previousRange = Set.of();
      this.lastPos = null;
   }

   private void ensureSession(ClientWorld clientWorld) {
      this.ensureConnection();
      if (clientWorld != this.sessionLevel) {
         this.sessionLevel = clientWorld;
         this.resetSession();
      }
   }

   private void ensureConnection() {
      ClientPlayNetworkHandler var1 = MinecraftClient.getInstance().getNetworkHandler();
      if (var1 != this.connection) {
         this.connection = var1;
         this.flaggedChunks.clear();
         this.sourceChunks.clear();
      }
   }

   @EventHandler(
      stage = Stage.POST
   )
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null && var2.world != null) {
         this.ensureSession(var2.world);
         if (LevelUtil.isOverworld(var2.world)) {
            ChunkPos var3 = var2.player.getChunkPos();
            if (this.lastPos != null && !this.lastPos.equals(var3)) {
               Set var4 = chunksInRange(var3);

               for (ChunkPos var6 : var4) {
                  if (!this.previousRange.contains(var6)) {
                     this.snapshots.store(var6.x, var6.z, extractSections(var2.world.getChunk(var6.x, var6.z)));
                  }
               }

               this.justSent.removeIf(chunkPos -> !var4.contains(chunkPos));
               HashSet var8 = new HashSet();

               for (ChunkPos var7 : var4) {
                  var8.add(ChunkSnapshotStore.chunkKey(var7.x, var7.z));
               }

               this.snapshots.retain(var8::contains);
               this.previousRange = var4;
            }

            this.lastPos = var3;
         }
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent packetAppliedEvent) {
      if (packetAppliedEvent.packet instanceof ChunkDataS2CPacket var3) {
         MinecraftClient var4 = MinecraftClient.getInstance();
         ClientWorld var5 = var4.world;
         if (var4.player != null && var5 != null) {
            this.ensureSession(var5);
            if (LevelUtil.isOverworld(var5)) {
               ChunkPos var6 = new ChunkPos(var3.getChunkX(), var3.getChunkZ());
               if (chunksInRange(var4.player.getChunkPos()).contains(var6)) {
                  if (LevelUtil.isLoaded(var5, var6.x, var6.z)) {
                     PalettedContainer[] var7 = extractSections(var5.getChunk(var6.x, var6.z));

                     for (ChunkSnapshotStore.BlockChange var10 : this.snapshots.compare(var6.x, var6.z, var7, var5.getBottomSectionCoord())) {
                        Block var11 = var10.newState().getBlock();
                        Block var12 = var10.oldState().getBlock();
                        if (!IGNORED_NEW_BLOCKS.contains(var11) && !IGNORED_OLD_BLOCKS.contains(var12)) {
                           this.onActivity(var6, var10, var11, var12);
                           break;
                        }
                     }

                     this.justSent.add(var6);
                     this.snapshots.store(var6.x, var6.z, var7);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent blockUpdateEvent) {
      ClientWorld var2 = MinecraftClient.getInstance().world;
      if (var2 != null && LevelUtil.isOverworld(var2)) {
         BlockPos var3 = blockUpdateEvent.pos;
         int var4 = (var3.getY() >> 4) - var2.getBottomSectionCoord();
         this.snapshots.update(var3.getX() >> 4, var3.getZ() >> 4, var4, var3.getX() & 15, var3.getY() & 15, var3.getZ() & 15, blockUpdateEvent.newState);
      }
   }

   private void onActivity(ChunkPos chunkPos, ChunkSnapshotStore.BlockChange blockChange, Block block, Block block2) {
      ModuleChat.Body var5 = ModuleChat.body().text("Activity ").good("detected").text(" in chunk ").value(chunkPos.x + ", " + chunkPos.z);
      if (this.advancedDebug.get()) {
         var5.text(": ").name(block2.getName().getString()).text(" changed to ").name(block.getName().getString());
      }

      ModuleChat.send(this, var5);
      this.changedBlocks.add(new BlockPos(blockChange.worldX(), blockChange.worldY(), blockChange.worldZ()));
      this.flagArea(chunkPos);
   }

   private void flagArea(ChunkPos chunkPos) {
      this.ensureConnection();
      this.sourceChunks.add(chunkPos);
      ArrayList var2 = new ArrayList();

      for (int var3 = -9; var3 <= 9; var3++) {
         for (int var4 = -9; var4 <= 9; var4++) {
            var2.add(new ChunkPos(chunkPos.x + var3, chunkPos.z + var4));
         }
      }

      if (this.flaggedChunks.isEmpty()) {
         this.flaggedChunks.addAll(var2);
      } else {
         ArrayList var6 = new ArrayList();

         for (ChunkPos var5 : var2) {
            if (this.flaggedChunks.contains(var5)) {
               var6.add(var5);
            }
         }

         this.flaggedChunks.clear();
         this.flaggedChunks.addAll(var6.isEmpty() ? var2 : var6);
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.player != null && var2.world != null && LevelUtil.isOverworld(var2.world)) {
         this.ensureConnection();
         if (this.renderChunks.get()) {
            this.renderFlaggedChunks();
         }

         if (this.render.get() && this.advancedDebug.get()) {
            DrawStyle var3 = DrawStyle.filledAndStroked(this.lineColor.argb(), 1.5F, this.fillColor.argb());

            for (BlockPos var5 : this.changedBlocks) {
               GizmoDrawing.box(new Box(var5), var3).ignoreOcclusion();
            }

            DrawStyle var7 = DrawStyle.filledAndStroked(-16711936, 1.5F, 671153920);

            for (ChunkPos var6 : chunksInRange(var2.player.getChunkPos())) {
               if (this.justSent.contains(var6)) {
                  GizmoDrawing.box(marker(var6), var7).ignoreOcclusion();
               }
            }
         }
      }
   }

   private void renderFlaggedChunks() {
      DrawStyle var1 = DrawStyle.filledAndStroked(this.chunkLine.argb(), 1.5F, this.chunkFill.argb());
      boolean var2 = this.highlightSources.get();

      for (ChunkPos var4 : this.flaggedChunks) {
         if (!var2 || !this.sourceChunks.contains(var4)) {
            GizmoDrawing.box(marker(var4), var1).ignoreOcclusion();
         }
      }

      if (var2) {
         DrawStyle var6 = DrawStyle.filledAndStroked(-16711681, 1.5F, 671154175);

         for (ChunkPos var5 : this.sourceChunks) {
            GizmoDrawing.box(marker(var5), var6).ignoreOcclusion();
         }
      }
   }

   private static Box marker(ChunkPos chunkPos) {
      double var1 = chunkPos.x * 16.0;
      double var3 = chunkPos.z * 16.0;
      return new Box(var1, 63.0, var3, var1 + 16.0, 63.0, var3 + 16.0);
   }

   static Set<ChunkPos> chunksInRange(ChunkPos chunkPos) {
      LinkedHashSet var1 = new LinkedHashSet();

      for (int var2 = -8; var2 <= 8; var2++) {
         int var3 = Math.max(0, 3 - (8 - Math.abs(var2)));
         int var4 = 8 - var3;

         for (int var5 = -var4; var5 <= var4; var5++) {
            var1.add(new ChunkPos(chunkPos.x + var5, chunkPos.z + var2));
         }
      }

      return var1;
   }

   private static PalettedContainer<BlockState>[] extractSections(WorldChunk worldChunk) {
      ChunkSection[] var1 = worldChunk.getSectionArray();
      PalettedContainer[] var2 = new PalettedContainer[var1.length];

      for (int var3 = 6; var3 <= 23 && var3 < var1.length; var3++) {
         ChunkSection var4 = var1[var3];
         if (var4 != null && !var4.isEmpty()) {
            var2[var3] = var4.getBlockStateContainer().copy();
         }
      }

      return var2;
   }
}
