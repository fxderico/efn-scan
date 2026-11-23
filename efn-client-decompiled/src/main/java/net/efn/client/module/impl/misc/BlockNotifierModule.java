package net.efn.client.module.impl.misc;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.efn.client.esp.ChunkScanner;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BlockColorListSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class BlockNotifierModule extends ClientModule implements ChunkScanner.Consumer {
   private static final int MAX_KNOWN = 50000;
   private final BlockColorListSetting blocks = this.addSetting("blocks", BlockColorListSetting.blockList("Blocks", List.of("minecraft:spawner")));
   private final BooleanSetting sound = this.addSetting("sound", new BooleanSetting("Sound", true));
   private final LongLinkedOpenHashSet known = new LongLinkedOpenHashSet();
   private final Map<Block, BlockNotifierModule.Finds> pending = new LinkedHashMap<>();
   private Set<String> scannedIds = Set.of();
   private RegistryKey<World> dimension;

   public BlockNotifierModule() {
      super(
         "block-notifier", "Block Notifier", "Shows a toast with the block and its coordinates when one of your chosen blocks shows up.", ModuleCategory.MISC
      );
      this.blocks.onChange(this::rescan);
   }

   @Override
   protected void onEnable() {
      this.known.clear();
      this.pending.clear();
      this.dimension = null;
      this.scannedIds = this.blocks.enabledIds();
      ChunkScanner.register(this);
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
   }

   @Override
   protected void onDisable() {
      ChunkScanner.unregister(this);
      this.known.clear();
      this.pending.clear();
   }

   private void rescan() {
      if (this.isEnabled()) {
         Set var1 = this.blocks.enabledIds();
         if (!var1.equals(this.scannedIds)) {
            this.scannedIds = var1;
            ChunkScanner.rescan();
         }
      }
   }

   @Override
   public boolean wants(BlockState var1) {
      return this.blocks.isEnabled(var1.method_26204());
   }

   @Override
   public void found(long var1, BlockState var3) {
      MinecraftClient var6 = MinecraftClient.method_1551();
      if (var6.field_1687 != null) {
         this.syncDimension(var6);
         if (this.known.add(var1)) {
            if (this.known.size() > 50000) {
               this.known.removeFirstLong();
            }

            BlockPos var7 = BlockPos.method_10092(var1);
            BlockNotifierModule.Finds var8 = this.pending.computeIfAbsent(var3.method_26204(), var0 -> new BlockNotifierModule.Finds());
            var8.count++;
            double var4 = var6.field_1724 == null ? 0.0 : var6.field_1724.method_5707(Vec3d.method_24953(var7));
            if (var4 < var8.nearestSq) {
               var8.nearestSq = var4;
               var8.nearest = var7;
            }
         }
      }
   }

   @Override
   public void removed(long var1) {
      this.known.remove(var1);
   }

   @Override
   public void reset() {
      this.known.clear();
      this.pending.clear();
   }

   private void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1687 != null) {
         this.syncDimension(var2);
         if (!this.pending.isEmpty()) {
            for (Entry var4 : this.pending.entrySet()) {
               this.notify((Block)var4.getKey(), (BlockNotifierModule.Finds)var4.getValue());
            }

            this.pending.clear();
         }
      }
   }

   private void syncDimension(MinecraftClient var1) {
      RegistryKey var2 = var1.field_1687.method_27983();
      if (!var2.equals(this.dimension)) {
         this.dimension = var2;
         this.known.clear();
         this.pending.clear();
      }
   }

   private void notify(Block var1, BlockNotifierModule.Finds var2) {
      Item var3 = var1.method_8389();
      ItemStack var4 = new ItemStack(var3 == Items.field_8162 ? Items.field_8251 : var3);
      String var5 = var1.method_9518().getString();
      BlockPos var6 = var2.nearest;
      MutableText var7 = Text.method_43470("at " + var6.method_10263() + ", " + var6.method_10264() + ", " + var6.method_10260());

      for (int var8 = 0; var8 < var2.count; var8++) {
         ChallengeToast.addOrUpdate(var4, var5, var7, this.sound.get());
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Finds {
      int count;
      BlockPos nearest;
      double nearestSq = 1.7976931348623157E308;

      private Finds() {
      }
   }
}
