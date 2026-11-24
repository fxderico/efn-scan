package net.efn.client.module.impl.render;

import java.util.Set;
import java.util.function.LongPredicate;
import net.efn.client.esp.BlockLayer;
import net.efn.client.esp.ChunkScanner;
import net.efn.client.esp.EspStyle;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BlockColorListSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class BlockEspModule extends ClientModule implements ChunkScanner.Consumer {
   private final EspStyle style = new EspStyle((string, setting) -> (S)this.addSetting(string, setting), "Outline", 0.2F, 2048.0F);
   private final BooleanSetting sTracers = this.addSetting("tracers", new BooleanSetting("Tracers", false));
   private final BlockColorListSetting sBlockColors = this.addSetting(BlockColorListSetting.createDefault());
   private final BlockLayer layer = new BlockLayer(new BlockLayer.Palette() {
      @Override
      public int color(Object object) {
         return BlockEspModule.this.sBlockColors.colorArgb((Block)object);
      }

      @Override
      public int tracerColor(Object object) {
         return BlockEspModule.this.sBlockColors.tracerColorArgb((Block)object);
      }
   }, () -> MinecraftClient.getInstance().world == null ? null : MinecraftClient.getInstance().world.getRegistryKey());
   private Set<String> scannedIds = Set.of();

   public BlockEspModule() {
      super("block-esp", "Block ESP", "Highlights chosen blocks; touching blocks merge into one shape.", ModuleCategory.RENDER);
      this.sBlockColors.onChange(this::rescan);
   }

   private void rescan() {
      if (this.isEnabled()) {
         Set var1 = this.sBlockColors.enabledIds();
         if (!var1.equals(this.scannedIds)) {
            boolean var2 = !var1.containsAll(this.scannedIds);
            this.scannedIds = var1;
            if (var2) {
               this.layer.clear();
            }

            ChunkScanner.rescan();
         }
      }
   }

   @Override
   protected void onEnable() {
      this.scannedIds = this.sBlockColors.enabledIds();
      ChunkScanner.register(this);
   }

   @Override
   protected void onDisable() {
      ChunkScanner.unregister(this);
      this.layer.clear();
   }

   @Override
   public boolean wants(BlockState blockState) {
      return this.sBlockColors.isEnabled(blockState.getBlock());
   }

   @Override
   public void found(long l, BlockState blockState) {
      this.layer.put(l, blockState.getBlock());
   }

   @Override
   public void removed(long l) {
      this.layer.remove(l);
   }

   @Override
   public void reset() {
      this.layer.clear();
   }

   @Override
   public void chunkScanned(long l, LongPredicate longPredicate) {
      this.layer.retainChunk(l, longPredicate);
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      this.layer.render(render3DEvent, this.style);
      if (this.sTracers.get() && this.sBlockColors.hasAnyTracerEnabled()) {
         this.layer.renderTracers(this.style);
      }
   }
}
