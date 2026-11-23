package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.function.LongPredicate;
import net.efn.client.EfnClientMod;
import net.efn.client.chat.ModuleChat;
import net.efn.client.esp.BlockLayer;
import net.efn.client.esp.ChunkScanner;
import net.efn.client.esp.EspStyle;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class SpawnerNotifierModule extends ClientModule implements ChunkScanner.Consumer {
   private static final String SPAWNER = "Spawner";
   private final BooleanSetting sSound = this.addSetting("sound", new BooleanSetting("Sound", true));
   private final BooleanSetting sRender = this.addSetting("render", new BooleanSetting("Render", true));
   private final ColorSetting sColor = this.addSetting("color", new ColorSetting("Color", -587246251));
   private final EspStyle style = new EspStyle((var1, var2) -> (S)this.addSetting(var1, var2), "Outline", 0.1F, 256.0F);
   private final BlockLayer layer = new BlockLayer(new BlockLayer.Palette() {
      @Override
      public int color(Object var1) {
         return SpawnerNotifierModule.this.sColor.argb();
      }

      @Override
      public int tracerColor(Object var1) {
         return 0;
      }
   }, () -> MinecraftClient.method_1551().field_1687 == null ? null : MinecraftClient.method_1551().field_1687.method_27983());
   private final LongOpenHashSet notified = new LongOpenHashSet();

   public SpawnerNotifierModule() {
      super("spawner-notifier", "Spawner Notifier", "Scans chunks for mob spawners and highlights them.", ModuleCategory.DONUT);
      this.usesNotifications();
   }

   @Override
   protected void onEnable() {
      ChunkScanner.register(this);
   }

   @Override
   protected void onDisable() {
      ChunkScanner.unregister(this);
      this.layer.clear();
      this.notified.clear();
   }

   @Override
   public boolean wants(BlockState var1) {
      return var1.method_27852(Blocks.field_10260);
   }

   @Override
   public void found(long var1, BlockState var3) {
      this.layer.put(var1, "Spawner");
      if (this.notified.add(var1)) {
         this.notify(BlockPos.method_10092(var1));
      }
   }

   @Override
   public void removed(long var1) {
      this.layer.remove(var1);
   }

   @Override
   public void reset() {
      this.layer.clear();
      this.notified.clear();
   }

   @Override
   public void chunkScanned(long var1, LongPredicate var3) {
      this.layer.retainChunk(var1, var3);
   }

   private void notify(BlockPos var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (this.notificationsOn()) {
         ModuleChat.send(this, foundMessage(var2, var1));
      }

      if (this.sSound.get() && var2.field_1724 != null && var2.field_1687 != null) {
         var2.method_1483().method_4873(PositionedSoundInstance.method_4757(EfnClientMod.SPAWNER_NOTIFY_SOUND, 1.0F, 1.0F));
      }
   }

   private static ModuleChat.Body foundMessage(MinecraftClient var0, BlockPos var1) {
      String var2 = entityName(var0, var1);
      ModuleChat.Body var3 = var2 == null ? ModuleChat.body().warn("Unknown") : ModuleChat.body().name(var2);
      var3.text(" spawner ").good("found").text(" at ").coords(var1.method_10263(), var1.method_10264(), var1.method_10260());
      if (var0.field_1724 != null) {
         long var4 = Math.round(var0.field_1724.method_73189().method_1022(Vec3d.method_24953(var1)));
         var3.text(" (").value(var4 + "m").text(")");
      }

      return var3;
   }

   private static String entityName(MinecraftClient var0, BlockPos var1) {
      BlockEntity var2;
      if (var0.field_1687 != null && (var2 = var0.field_1687.method_8321(var1)) instanceof MobSpawnerBlockEntity) {
         MobSpawnerBlockEntity var3 = (MobSpawnerBlockEntity)var2;
         Entity var4 = var3.method_11390().method_8283(var0.field_1687, var1);
         return var4 == null ? null : var4.method_5864().method_5897().getString();
      } else {
         return null;
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      if (this.sRender.get()) {
         this.layer.render(var1, this.style);
      }
   }
}
