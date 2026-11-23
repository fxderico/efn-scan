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
      Blocks.field_10036,
      Blocks.field_10110,
      Blocks.field_10418,
      Blocks.field_29219,
      Blocks.field_10212,
      Blocks.field_29027,
      Blocks.field_27120,
      Blocks.field_29221,
      Blocks.field_10571,
      Blocks.field_29026,
      Blocks.field_10080,
      Blocks.field_29030,
      Blocks.field_10013,
      Blocks.field_29220,
      Blocks.field_10090,
      Blocks.field_29028,
      Blocks.field_10442,
      Blocks.field_29029,
      Blocks.field_10503,
      Blocks.field_10098,
      Blocks.field_10539,
      Blocks.field_10335,
      Blocks.field_10035,
      Blocks.field_54714,
      Blocks.field_42731,
      Blocks.field_28673,
      Blocks.field_28674,
      Blocks.field_9989,
      Blocks.field_33509,
      Blocks.field_10340,
      Blocks.field_10282,
      Blocks.field_10450,
      Blocks.field_10377,
      Blocks.field_10523,
      Blocks.field_10524,
      Blocks.field_10091,
      Blocks.field_10228
   );
   private static final Set<Block> IGNORED_OLD_BLOCKS = Set.of(Blocks.field_27159, Blocks.field_10340, Blocks.field_10110);
   private final BooleanSetting renderChunks = this.addSetting("render-chunks", new BooleanSetting("Render Chunks", true));
   private final BooleanSetting clearOnToggle = this.addSetting("clear-chunks-on-toggle", new BooleanSetting("Clear On Toggle", true));
   private final BooleanSetting highlightSources = this.addSetting("render-flagged-chunk", new BooleanSetting("Highlight Source Chunks", true));
   private final ColorSetting chunkFill = this.addSetting("chunk-side-color", new ColorSetting("Chunk Fill Color", 687810600));
   private final ColorSetting chunkLine = this.addSetting("chunk-line-color", new ColorSetting("Chunk Line Color", -55256));
   private final BooleanSetting advancedDebug = this.addSetting("advanced-debug", new BooleanSetting("Advanced Debug", false));
   private final BooleanSetting render = this.addSetting("render", new BooleanSetting("Debug Render", true));
   private final ColorSetting fillColor = this.addSetting("side-color", new ColorSetting("Debug Fill Color", 687810600));
   private final ColorSetting lineColor = this.addSetting("line-color", new ColorSetting("Debug Line Color", -55256));
   private final ChunkSnapshotStore snapshots = new ChunkSnapshotStore(Blocks.field_10124.method_9564());
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

   private void ensureSession(ClientWorld var1) {
      this.ensureConnection();
      if (var1 != this.sessionLevel) {
         this.sessionLevel = var1;
         this.resetSession();
      }
   }

   private void ensureConnection() {
      ClientPlayNetworkHandler var1 = MinecraftClient.method_1551().method_1562();
      if (var1 != this.connection) {
         this.connection = var1;
         this.flaggedChunks.clear();
         this.sourceChunks.clear();
      }
   }

   @EventHandler(
      stage = Stage.POST
   )
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 != null && var2.field_1687 != null) {
         this.ensureSession(var2.field_1687);
         if (LevelUtil.isOverworld(var2.field_1687)) {
            ChunkPos var3 = var2.field_1724.method_31476();
            if (this.lastPos != null && !this.lastPos.equals(var3)) {
               Set var4 = chunksInRange(var3);

               for (ChunkPos var6 : var4) {
                  if (!this.previousRange.contains(var6)) {
                     this.snapshots.store(var6.field_9181, var6.field_9180, extractSections(var2.field_1687.method_8497(var6.field_9181, var6.field_9180)));
                  }
               }

               this.justSent.removeIf(var1x -> !var4.contains(var1x));
               HashSet var8 = new HashSet();

               for (ChunkPos var7 : var4) {
                  var8.add(ChunkSnapshotStore.chunkKey(var7.field_9181, var7.field_9180));
               }

               this.snapshots.retain(var8::contains);
               this.previousRange = var4;
            }

            this.lastPos = var3;
         }
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent var1) {
      if (var1.packet instanceof ChunkDataS2CPacket var3) {
         MinecraftClient var4 = MinecraftClient.method_1551();
         ClientWorld var5 = var4.field_1687;
         if (var4.field_1724 != null && var5 != null) {
            this.ensureSession(var5);
            if (LevelUtil.isOverworld(var5)) {
               ChunkPos var6 = new ChunkPos(var3.method_11523(), var3.method_11524());
               if (chunksInRange(var4.field_1724.method_31476()).contains(var6)) {
                  if (LevelUtil.isLoaded(var5, var6.field_9181, var6.field_9180)) {
                     PalettedContainer[] var7 = extractSections(var5.method_8497(var6.field_9181, var6.field_9180));

                     for (ChunkSnapshotStore.BlockChange var10 : this.snapshots.compare(var6.field_9181, var6.field_9180, var7, var5.method_32891())) {
                        Block var11 = var10.newState().method_26204();
                        Block var12 = var10.oldState().method_26204();
                        if (!IGNORED_NEW_BLOCKS.contains(var11) && !IGNORED_OLD_BLOCKS.contains(var12)) {
                           this.onActivity(var6, var10, var11, var12);
                           break;
                        }
                     }

                     this.justSent.add(var6);
                     this.snapshots.store(var6.field_9181, var6.field_9180, var7);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      if (var2 != null && LevelUtil.isOverworld(var2)) {
         BlockPos var3 = var1.pos;
         int var4 = (var3.method_10264() >> 4) - var2.method_32891();
         this.snapshots
            .update(
               var3.method_10263() >> 4,
               var3.method_10260() >> 4,
               var4,
               var3.method_10263() & 15,
               var3.method_10264() & 15,
               var3.method_10260() & 15,
               var1.newState
            );
      }
   }

   private void onActivity(ChunkPos var1, ChunkSnapshotStore.BlockChange var2, Block var3, Block var4) {
      ModuleChat.Body var5 = ModuleChat.body().text("Activity ").good("detected").text(" in chunk ").value(var1.field_9181 + ", " + var1.field_9180);
      if (this.advancedDebug.get()) {
         var5.text(": ").name(var4.method_9518().getString()).text(" changed to ").name(var3.method_9518().getString());
      }

      ModuleChat.send(this, var5);
      this.changedBlocks.add(new BlockPos(var2.worldX(), var2.worldY(), var2.worldZ()));
      this.flagArea(var1);
   }

   private void flagArea(ChunkPos var1) {
      this.ensureConnection();
      this.sourceChunks.add(var1);
      ArrayList var2 = new ArrayList();

      for (int var3 = -9; var3 <= 9; var3++) {
         for (int var4 = -9; var4 <= 9; var4++) {
            var2.add(new ChunkPos(var1.field_9181 + var3, var1.field_9180 + var4));
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
   public void onRender3D(Render3DEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1724 != null && var2.field_1687 != null && LevelUtil.isOverworld(var2.field_1687)) {
         this.ensureConnection();
         if (this.renderChunks.get()) {
            this.renderFlaggedChunks();
         }

         if (this.render.get() && this.advancedDebug.get()) {
            DrawStyle var3 = DrawStyle.method_75537(this.lineColor.argb(), 1.5F, this.fillColor.argb());

            for (BlockPos var5 : this.changedBlocks) {
               GizmoDrawing.method_75541(new Box(var5), var3).method_75533();
            }

            DrawStyle var7 = DrawStyle.method_75537(-16711936, 1.5F, 671153920);

            for (ChunkPos var6 : chunksInRange(var2.field_1724.method_31476())) {
               if (this.justSent.contains(var6)) {
                  GizmoDrawing.method_75541(marker(var6), var7).method_75533();
               }
            }
         }
      }
   }

   private void renderFlaggedChunks() {
      DrawStyle var1 = DrawStyle.method_75537(this.chunkLine.argb(), 1.5F, this.chunkFill.argb());
      boolean var2 = this.highlightSources.get();

      for (ChunkPos var4 : this.flaggedChunks) {
         if (!var2 || !this.sourceChunks.contains(var4)) {
            GizmoDrawing.method_75541(marker(var4), var1).method_75533();
         }
      }

      if (var2) {
         DrawStyle var6 = DrawStyle.method_75537(-16711681, 1.5F, 671154175);

         for (ChunkPos var5 : this.sourceChunks) {
            GizmoDrawing.method_75541(marker(var5), var6).method_75533();
         }
      }
   }

   private static Box marker(ChunkPos var0) {
      double var1 = var0.field_9181 * 16.0;
      double var3 = var0.field_9180 * 16.0;
      return new Box(var1, 63.0, var3, var1 + 16.0, 63.0, var3 + 16.0);
   }

   static Set<ChunkPos> chunksInRange(ChunkPos var0) {
      LinkedHashSet var1 = new LinkedHashSet();

      for (int var2 = -8; var2 <= 8; var2++) {
         int var3 = Math.max(0, 3 - (8 - Math.abs(var2)));
         int var4 = 8 - var3;

         for (int var5 = -var4; var5 <= var4; var5++) {
            var1.add(new ChunkPos(var0.field_9181 + var5, var0.field_9180 + var2));
         }
      }

      return var1;
   }

   private static PalettedContainer<BlockState>[] extractSections(WorldChunk var0) {
      ChunkSection[] var1 = var0.method_12006();
      PalettedContainer[] var2 = new PalettedContainer[var1.length];

      for (int var3 = 6; var3 <= 23 && var3 < var1.length; var3++) {
         ChunkSection var4 = var1[var3];
         if (var4 != null && !var4.method_38292()) {
            var2[var3] = var4.method_12265().method_39957();
         }
      }

      return var2;
   }
}
