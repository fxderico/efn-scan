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
   private final EspStyle style = new EspStyle((var1, var2) -> (S)this.addSetting(var1, var2), "Outline", 0.2F, 2048.0F);
   private final BooleanSetting sTracers = this.addSetting("tracers", new BooleanSetting("Tracers", false));
   private final BlockColorListSetting sBlockColors = this.addSetting(BlockColorListSetting.createDefault());
   private final BlockLayer layer = new BlockLayer(new BlockLayer.Palette() {
      @Override
      public int color(Object var1) {
         return BlockEspModule.this.sBlockColors.colorArgb((Block)var1);
      }

      @Override
      public int tracerColor(Object var1) {
         return BlockEspModule.this.sBlockColors.tracerColorArgb((Block)var1);
      }
   }, () -> MinecraftClient.method_1551().field_1687 == null ? null : MinecraftClient.method_1551().field_1687.method_27983());
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
   public boolean wants(BlockState var1) {
      return this.sBlockColors.isEnabled(var1.method_26204());
   }

   @Override
   public void found(long var1, BlockState var3) {
      this.layer.put(var1, var3.method_26204());
   }

   @Override
   public void removed(long var1) {
      this.layer.remove(var1);
   }

   @Override
   public void reset() {
      this.layer.clear();
   }

   @Override
   public void chunkScanned(long var1, LongPredicate var3) {
      this.layer.retainChunk(var1, var3);
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      this.layer.render(var1, this.style);
      if (this.sTracers.get() && this.sBlockColors.hasAnyTracerEnabled()) {
         this.layer.renderTracers(this.style);
      }
   }
}
