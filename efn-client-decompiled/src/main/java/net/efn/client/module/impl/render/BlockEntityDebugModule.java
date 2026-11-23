package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Set;
import java.util.Map.Entry;
import net.efn.client.chat.ModuleChat;
import net.efn.client.esp.BlockLayer;
import net.efn.client.esp.EspStyle;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BlockColorListSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.util.BlockEntityLeakTracker;
import net.efn.client.util.LightLeakScanner;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class BlockEntityDebugModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("EFN");
   private static final int SETTINGS_VERSION = 1;
   private static final int SUMMARY_TICKS = 1200;
   private static final float FILL_ALPHA = 0.8F;
   private static final int MAX_CHAT_PER_TICK = 5;
   private static final int MAX_NBT_CHARS = 200;
   private static final Set<String> NEW_IN_TABLE = Set.of("minecraft:jukebox", "minecraft:piston", "minecraft:sticky_piston");
   private final ModeSetting sDetection = this.addSetting("detection", new ModeSetting("Detection", "All", "All", "Below Zero", "Below -20", "Below -30"));
   private final BooleanSetting sBlockFilter = this.addSetting("block-filter", new BooleanSetting("Block Filter", false));
   private final BooleanSetting sChat = this.addSetting("chat", new BooleanSetting("Chat Notifications", false));
   private final BooleanSetting sShowNbt = this.addSetting("show-nbt", new BooleanSetting("Show NBT", true));
   private final BooleanSetting sHints = this.addSetting("hidden-hints", new BooleanSetting("Hidden Block Hints", true));
   private final BooleanSetting sLightLeaks = this.addSetting("light-leaks", new BooleanSetting("Light Leaks", true));
   private final EspStyle style = new EspStyle((var1, var2) -> (S)this.addSetting(var1, var2), "Boxes", 0.8F, 256.0F);
   private final BooleanSetting sTracers = this.addSetting("tracers", new BooleanSetting("Tracers", true));
   private final BooleanSetting sCustomTracerColor = this.addSetting("custom-tracer-color", new BooleanSetting("Custom Tracer Color", false));
   private final ColorSetting sTracerColor = this.addSetting("tracer-color", new ColorSetting("Tracer Color", -1));
   private final BlockColorListSetting sBlockColors = this.addSetting(BlockColorListSetting.createForBlockEntityDebug());
   private final BlockLayer layer = new BlockLayer(new BlockLayer.Palette() {
      @Override
      public int color(Object var1) {
         return BlockEntityDebugModule.this.sBlockColors.colorArgb((Block)var1);
      }

      @Override
      public int tracerColor(Object var1) {
         return BlockEntityDebugModule.this.tracerColorOf((Block)var1);
      }
   }, () -> MinecraftClient.method_1551().field_1687 == null ? null : MinecraftClient.method_1551().field_1687.method_27983());
   private final LongOpenHashSet spawners = new LongOpenHashSet();
   private final LightLeakScanner lightScanner = new LightLeakScanner();
   private int ticks;
   private String lastSummary = "";
   private int seenGeneration = -1;
   private Set<String> scannedIds = Set.of();

   public BlockEntityDebugModule() {
      super(
         "block-entity-debug",
         "Block Entity Debug",
         "Renders every leaked block entity as ESP (always-on Netty capture), plus blocks the server hides but still gives away through chest/crafter events, sounds and light.",
         ModuleCategory.BASE_RAID
      );
      this.sShowNbt.visibleWhen(this.sChat::get);
      this.sLightLeaks.visibleWhen(this.sHints::get);
      this.sHints.onChange(this::resync);
      this.sLightLeaks.onChange(this::restartLightScan);
      this.sCustomTracerColor.visibleWhen(this.sTracers::get);
      this.sTracerColor.visibleWhen(() -> this.sTracers.get() && this.sCustomTracerColor.get());
      this.sDetection.onChange(this::resync);
      this.sBlockFilter.onChange(this::resync);
      this.sBlockColors.onChange(this::onBlockColorsChanged);
   }

   @Override
   public int settingsVersion() {
      return 1;
   }

   @Override
   public void migrateSettings(int var1) {
      this.style.shape.set("Boxes");
      this.style.boxSize.set(1.0F);
      this.style.fillAlpha.set(0.8F);
      this.sBlockColors.resetColors();
      int var2 = BlockColorListSetting.rgbaToArgb(this.sBlockColors.defaultColor());
      boolean var3 = this.sBlockFilter.get();

      for (BlockColorListSetting.BlockEntry var5 : this.sBlockColors.entries()) {
         if (BlockColorListSetting.rgbaToArgb(var5.color()) != var2 && (!var3 || NEW_IN_TABLE.contains(var5.id()))) {
            this.sBlockColors.select(var5);
         }
      }
   }

   private int tracerColorOf(Block var1) {
      int var2 = this.sBlockColors.isEnabled(var1) ? this.sBlockColors.tracerColorArgb(var1) : this.sBlockColors.colorArgb(var1);
      if (var2 == 0) {
         return 0;
      } else {
         return this.sCustomTracerColor.get() ? this.sTracerColor.argb() : var2;
      }
   }

   private void onBlockColorsChanged() {
      if (this.isEnabled() && this.sBlockFilter.get() && !this.sBlockColors.enabledIds().equals(this.scannedIds)) {
         this.resync();
      }
   }

   @Override
   protected void onEnable() {
      BlockEntityLeakTracker.recordAdditions(true);
      this.resync();
      this.restartLightScan();
   }

   @Override
   protected void onDisable() {
      BlockEntityLeakTracker.recordAdditions(false);
      this.lightScanner.clear();
      this.layer.clear();
      this.spawners.clear();
   }

   private void restartLightScan() {
      this.lightScanner.clear();
      MinecraftClient var1 = MinecraftClient.method_1551();
      if (this.isEnabled() && this.lightLeaksOn() && var1.field_1687 != null && var1.field_1724 != null) {
         this.lightScanner.queueLoaded(var1.field_1687, var1.field_1724.method_24515(), var1.field_1690.method_38521());
      }
   }

   private boolean lightLeaksOn() {
      return this.sHints.get() && this.sLightLeaks.get();
   }

   private void resync() {
      if (this.isEnabled()) {
         this.layer.clear();
         this.spawners.clear();
         this.scannedIds = this.sBlockColors.enabledIds();
         this.seenGeneration = BlockEntityLeakTracker.generation();

         while (BlockEntityLeakTracker.pollAdded() != null) {
         }

         for (Entry var2 : BlockEntityLeakTracker.view().entrySet()) {
            this.accept((BlockPos)var2.getKey(), (BlockEntityLeakTracker.Entry)var2.getValue());
         }
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent var1) {
      if (this.lightLeaksOn()) {
         this.lightScanner.onPacketApplied(var1.packet);
      }
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      if (this.lightLeaksOn() && var3.field_1687 != null) {
         this.lightScanner.tick(var3.field_1687);
      }

      if (++this.ticks % 1200 == 0) {
         this.logSources();
      }

      if (BlockEntityLeakTracker.generation() != this.seenGeneration) {
         this.resync();
      } else {
         int var4 = 0;
         int var5 = 0;

         BlockPos var2;
         while ((var2 = BlockEntityLeakTracker.pollAdded()) != null) {
            BlockEntityLeakTracker.Entry var6 = BlockEntityLeakTracker.view().get(var2);
            if (var6 != null && this.accept(var2, var6) && this.sChat.get()) {
               if (var4 < 5) {
                  this.announce(var2, var6);
                  var4++;
               } else {
                  var5++;
               }
            }
         }

         if (var5 > 0) {
            ModuleChat.send(this, ModuleChat.body().text("+").value(var5).text(" more"));
         }
      }
   }

   private void logSources() {
      String var1 = "chunk="
         + BlockEntityLeakTracker.count(BlockEntityLeakTracker.Source.CHUNK)
         + " data="
         + BlockEntityLeakTracker.count(BlockEntityLeakTracker.Source.DATA)
         + " event="
         + BlockEntityLeakTracker.count(BlockEntityLeakTracker.Source.EVENT)
         + " sound="
         + BlockEntityLeakTracker.count(BlockEntityLeakTracker.Source.SOUND)
         + " light="
         + BlockEntityLeakTracker.count(BlockEntityLeakTracker.Source.LIGHT);
      if (!var1.equals(this.lastSummary)) {
         this.lastSummary = var1;
         LOGGER.info("[EFN] Block Entity Debug sources: {}", var1);
      }
   }

   private boolean accept(BlockPos var1, BlockEntityLeakTracker.Entry var2) {
      if (var2.real() || this.sHints.get() && (var2.source() != BlockEntityLeakTracker.Source.LIGHT || this.sLightLeaks.get())) {
         Block var3 = var2.block();
         if (!this.passesDetection(var1)) {
            return false;
         } else if (this.sBlockFilter.get() && var3 != Blocks.field_10124 && !this.sBlockColors.isEnabled(var3)) {
            return false;
         } else {
            this.layer.put(var1.method_10063(), var3);
            if (var3 == Blocks.field_10260) {
               this.spawners.add(var1.method_10063());
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private void announce(BlockPos var1, BlockEntityLeakTracker.Entry var2) {
      NbtCompound var4 = var2.tag();
      ModuleChat.Body var5 = ModuleChat.body().name(var2.block() == Blocks.field_10124 ? "Hidden block" : var2.block().method_9518().getString());
      if (!var2.real()) {
         var5.text(" (from " + var2.source().name().toLowerCase() + ")");
      }

      String var3 = var4 == null
         ? null
         : var4.method_10562("SpawnData").flatMap(var0 -> var0.method_10562("entity")).flatMap(var0 -> var0.method_10558("id")).orElse(null);
      if (var3 != null) {
         var5.text(" (").value(var3.replace("minecraft:", "")).text(")");
      }

      var5.text(" at ").coords(var1.method_10263(), var1.method_10264(), var1.method_10260());
      if (this.sShowNbt.get() && var4 != null) {
         String var7 = var4.toString();
         if (var7.length() > 200) {
            var7 = var7.substring(0, 200) + "...";
         }

         Text var8 = NbtHelper.method_32270(var4);
         var5.append(Text.method_43470(" " + var7).method_27694(var1x -> var1x.method_10949(new ShowText(var8))));
      }

      ModuleChat.send(this, var5);
   }

   private boolean passesDetection(BlockPos var1) {
      String var2 = this.sDetection.get();

      return switch (var2) {
         case "Below Zero" -> var1.method_10264() < 0;
         case "Below -20" -> var1.method_10264() < -20;
         case "Below -30" -> var1.method_10264() < -30;
         default -> true;
      };
   }

   @EventHandler
   public void onPacketReceiveNetty(PacketReceiveNettyEvent var1) {
      if (var1.packet instanceof BlockUpdateS2CPacket var3) {
         if (BlockEntityLeakTracker.containsReal(var3.method_11309())) {
            BlockState var4 = var3.method_11308();
            if (var4.method_26215() || var4.method_27852(Blocks.field_10340) || var4.method_27852(Blocks.field_28888)) {
               var1.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      this.layer.render(var1, this.style);
      if (this.sTracers.get()) {
         this.layer.renderTracers(this.style);
      }

      if (!this.spawners.isEmpty()) {
         MinecraftClient var2 = MinecraftClient.method_1551();
         if (var2.field_1724 != null) {
            Vec3d var3 = var2.field_1724.method_5836(var1.partialTick);
            double var4 = (double)this.style.range.get() * this.style.range.get();
            int var6 = this.sBlockColors.colorArgb(Blocks.field_10260);
            DrawStyle var7 = DrawStyle.method_75536(var6, this.style.lineWidth.get());
            LongIterator var8 = this.spawners.iterator();

            while (var8.hasNext()) {
               long var9 = var8.next();
               BlockPos var11 = BlockPos.method_10092(var9);
               if (!(var3.method_1028(var11.method_10263() + 0.5, var11.method_10264() + 1.0, var11.method_10260() + 0.5) > var4)) {
                  GizmoDrawing.method_75541(
                        new Box(
                           var11.method_10263() + 0.3,
                           var11.method_10264() + 1.0,
                           var11.method_10260() + 0.3,
                           var11.method_10263() + 0.7,
                           var11.method_10264() + 257.0,
                           var11.method_10260() + 0.7
                        ),
                        var7
                     )
                     .method_75533();
               }
            }
         }
      }
   }
}
