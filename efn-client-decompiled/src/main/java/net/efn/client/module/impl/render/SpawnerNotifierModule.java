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
   private final EspStyle style = new EspStyle((string, setting) -> (S)this.addSetting(string, setting), "Outline", 0.1F, 256.0F);
   private final BlockLayer layer = new BlockLayer(new BlockLayer.Palette() {
      @Override
      public int color(Object object) {
         return SpawnerNotifierModule.this.sColor.argb();
      }

      @Override
      public int tracerColor(Object object) {
         return 0;
      }
   }, () -> MinecraftClient.getInstance().world == null ? null : MinecraftClient.getInstance().world.getRegistryKey());
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
   public boolean wants(BlockState blockState) {
      return blockState.isOf(Blocks.SPAWNER);
   }

   @Override
   public void found(long l, BlockState blockState) {
      this.layer.put(l, "Spawner");
      if (this.notified.add(l)) {
         this.notify(BlockPos.fromLong(l));
      }
   }

   @Override
   public void removed(long l) {
      this.layer.remove(l);
   }

   @Override
   public void reset() {
      this.layer.clear();
      this.notified.clear();
   }

   @Override
   public void chunkScanned(long l, LongPredicate longPredicate) {
      this.layer.retainChunk(l, longPredicate);
   }

   private void notify(BlockPos blockPos) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (this.notificationsOn()) {
         ModuleChat.send(this, foundMessage(var2, blockPos));
      }

      if (this.sSound.get() && var2.player != null && var2.world != null) {
         var2.getSoundManager().play(PositionedSoundInstance.ui(EfnClientMod.SPAWNER_NOTIFY_SOUND, 1.0F, 1.0F));
      }
   }

   private static ModuleChat.Body foundMessage(MinecraftClient minecraftClient, BlockPos blockPos) {
      String var2 = entityName(minecraftClient, blockPos);
      ModuleChat.Body var3 = var2 == null ? ModuleChat.body().warn("Unknown") : ModuleChat.body().name(var2);
      var3.text(" spawner ").good("found").text(" at ").coords(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      if (minecraftClient.player != null) {
         long var4 = Math.round(minecraftClient.player.getEntityPos().distanceTo(Vec3d.ofCenter(blockPos)));
         var3.text(" (").value(var4 + "m").text(")");
      }

      return var3;
   }

   private static String entityName(MinecraftClient minecraftClient, BlockPos blockPos) {
      BlockEntity var2;
      if (minecraftClient.world != null && (var2 = minecraftClient.world.getBlockEntity(blockPos)) instanceof MobSpawnerBlockEntity) {
         MobSpawnerBlockEntity var3 = (MobSpawnerBlockEntity)var2;
         Entity var4 = var3.getLogic().getRenderedEntity(minecraftClient.world, blockPos);
         return var4 == null ? null : var4.getType().getName().getString();
      } else {
         return null;
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      if (this.sRender.get()) {
         this.layer.render(render3DEvent, this.style);
      }
   }
}
