package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.util.HashMap;
import java.util.Map;
import net.efn.client.esp.BlockLayer;
import net.efn.client.esp.ChunkScanner;
import net.efn.client.esp.EspStyle;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

@Environment(EnvType.CLIENT)
public final class StorageEspModule extends ClientModule implements ChunkScanner.Consumer {
   private final EspStyle style = new EspStyle((string, setting) -> (S)this.addSetting(string, setting), "Outline", 0.25F, 256.0F);
   private final BooleanSetting sTracers = this.addSetting("tracers", new BooleanSetting("Tracers", false));
   private final StorageEspModule.Type chest = new StorageEspModule.Type(
      "Chest", true, -6530304, "chests", "chest-color", "chest-tracers", "chest-tracer-color"
   );
   private final StorageEspModule.Type enderChest = new StorageEspModule.Type(
      "Ender Chest", true, -9109249, "ender-chests", "ender-chest-color", "ender-chest-tracers", "ender-chest-tracer-color"
   );
   private final StorageEspModule.Type shulker = new StorageEspModule.Type(
      "Shulker Box", true, -7995234, "shulker-boxes", "shulker-box-color", "shulker-tracers", "shulker-tracer-color"
   );
   private final StorageEspModule.Type spawner = new StorageEspModule.Type(
      "Spawner", true, -7700826, "spawners", "spawner-color", "spawner-tracers", "spawner-tracer-color"
   );
   private final StorageEspModule.Type furnace = new StorageEspModule.Type(
      "Furnace", false, -10197916, "furnaces", "furnace-color", "furnace-tracers", "furnace-tracer-color"
   );
   private final StorageEspModule.Type barrel = new StorageEspModule.Type(
      "Barrel", true, -10204366, "barrels", "barrel-color", "barrel-tracers", "barrel-tracer-color"
   );
   private final StorageEspModule.Type enchanting = new StorageEspModule.Type(
      "Enchanting Table", true, -3670016, "enchanting-tables", "enchanting-table-color", "enchanting-tracers", "enchanting-tracer-color"
   );
   private final StorageEspModule.Type hopper = new StorageEspModule.Type(
      "Hopper", false, -11513776, "hoppers", "hopper-color", "hopper-tracers", "hopper-tracer-color"
   );
   private final StorageEspModule.Type[] types = new StorageEspModule.Type[]{
      this.chest, this.enderChest, this.shulker, this.spawner, this.furnace, this.barrel, this.enchanting, this.hopper
   };
   private final BlockLayer layer = new BlockLayer(new BlockLayer.Palette() {
      @Override
      public int color(Object object) {
         return ((StorageEspModule.Type)object).color.argb();
      }

      @Override
      public int tracerColor(Object object) {
         StorageEspModule.Type var2 = (StorageEspModule.Type)object;
         return var2.tracers.get() ? var2.tracerColor.argb() : 0;
      }
   }, () -> MinecraftClient.getInstance().world == null ? null : MinecraftClient.getInstance().world.getRegistryKey());
   private final Map<RegistryKey<World>, Long2ObjectOpenHashMap<StorageEspModule.Type>> detections = new HashMap<>();
   private RegistryKey<World> shownDimension;

   public StorageEspModule() {
      super("storage-esp", "Storage ESP Bypass", "Highlights storage blocks and spawners with per-type colours.", ModuleCategory.BASE_RAID);
   }

   private StorageEspModule.Type typeOf(Block block) {
      if (block instanceof ChestBlock) {
         return this.chest;
      } else if (block instanceof EnderChestBlock) {
         return this.enderChest;
      } else if (block instanceof ShulkerBoxBlock) {
         return this.shulker;
      } else if (block instanceof SpawnerBlock) {
         return this.spawner;
      } else if (block instanceof AbstractFurnaceBlock) {
         return this.furnace;
      } else if (block instanceof BarrelBlock) {
         return this.barrel;
      } else if (block instanceof EnchantingTableBlock) {
         return this.enchanting;
      } else {
         return block instanceof HopperBlock ? this.hopper : null;
      }
   }

   private StorageEspModule.Type typeOf(BlockEntityType<?> blockEntityType) {
      if (blockEntityType == BlockEntityType.CHEST || blockEntityType == BlockEntityType.TRAPPED_CHEST) {
         return this.chest;
      } else if (blockEntityType == BlockEntityType.ENDER_CHEST) {
         return this.enderChest;
      } else if (blockEntityType == BlockEntityType.SHULKER_BOX) {
         return this.shulker;
      } else if (blockEntityType == BlockEntityType.MOB_SPAWNER) {
         return this.spawner;
      } else if (blockEntityType == BlockEntityType.FURNACE || blockEntityType == BlockEntityType.BLAST_FURNACE || blockEntityType == BlockEntityType.SMOKER) {
         return this.furnace;
      } else if (blockEntityType == BlockEntityType.BARREL) {
         return this.barrel;
      } else if (blockEntityType == BlockEntityType.ENCHANTING_TABLE) {
         return this.enchanting;
      } else {
         return blockEntityType == BlockEntityType.HOPPER ? this.hopper : null;
      }
   }

   private StorageEspModule.Type typeOf(BlockEntityType<?> blockEntityType, NbtCompound nbtCompound) {
      StorageEspModule.Type var3 = this.typeOf(blockEntityType);
      if (var3 == null && nbtCompound != null) {
         Identifier var4 = nbtCompound.getString("id").map(Identifier::tryParse).orElse(null);
         return var4 == null ? null : this.typeOf(Registries.BLOCK_ENTITY_TYPE.get(var4));
      } else {
         return var3;
      }
   }

   private void rescan() {
      if (this.isEnabled()) {
         this.layer.clear();
         ClientWorld var2 = MinecraftClient.getInstance().world;
         this.shownDimension = var2 == null ? null : var2.getRegistryKey();
         Long2ObjectOpenHashMap var1 = this.shownDimension == null ? null : this.detections.get(this.shownDimension);
         if (var1 != null) {
            for (Entry var5 : var1.long2ObjectEntrySet()) {
               if (((StorageEspModule.Type)var5.getValue()).enabled.get()) {
                  this.layer.put(var5.getLongKey(), var5.getValue());
               }
            }
         }
      }
   }

   private boolean anyTracer() {
      if (!this.sTracers.get()) {
         return false;
      } else {
         for (StorageEspModule.Type var4 : this.types) {
            if (var4.enabled.get() && var4.tracers.get()) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   protected void onEnable() {
      this.detections.clear();
      this.layer.clear();
      ChunkScanner.register(this);
      ClientWorld var1 = MinecraftClient.getInstance().world;
      RegistryKey var2 = this.shownDimension = var1 == null ? null : var1.getRegistryKey();
      if (var1 != null && MinecraftClient.getInstance().player != null) {
         ChunkPos var3 = MinecraftClient.getInstance().player.getChunkPos();
         int var4 = MinecraftClient.getInstance().options.getClampedViewDistance() + 1;

         for (int var5 = var3.x - var4; var5 <= var3.x + var4; var5++) {
            for (int var6 = var3.z - var4; var6 <= var3.z + var4; var6++) {
               WorldChunk var7 = var1.getChunkManager().getWorldChunk(var5, var6);
               if (var7 != null) {
                  this.scanBlockEntities(var7);
               }
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      ChunkScanner.unregister(this);
      this.detections.clear();
      this.layer.clear();
      this.shownDimension = null;
   }

   private void found(BlockPos blockPos, StorageEspModule.Type type) {
      ClientWorld var3 = MinecraftClient.getInstance().world;
      if (type != null && var3 != null) {
         long var4 = blockPos.asLong();
         StorageEspModule.Type var6 = this.detections.computeIfAbsent(var3.getRegistryKey(), registryKey -> new Long2ObjectOpenHashMap<>()).put(var4, type);
         if (var6 != type) {
            if (var6 != null) {
               this.layer.remove(var4);
            }

            if (type.enabled.get()) {
               this.layer.put(var4, type);
            }
         }
      }
   }

   private void gone(BlockPos blockPos) {
      ClientWorld var2 = MinecraftClient.getInstance().world;
      if (var2 != null) {
         Long2ObjectOpenHashMap var3 = this.detections.get(var2.getRegistryKey());
         if (var3 != null && var3.remove(blockPos.asLong()) != null) {
            this.layer.remove(blockPos.asLong());
         }
      }
   }

   @Override
   public boolean wants(BlockState blockState) {
      return this.typeOf(blockState.getBlock()) != null;
   }

   @Override
   public void found(long l, BlockState blockState) {
      this.found(BlockPos.fromLong(l), this.typeOf(blockState.getBlock()));
   }

   @Override
   public void removed(long l) {
      ClientWorld var3 = MinecraftClient.getInstance().world;
      if (var3 != null && var3.getBlockState(BlockPos.fromLong(l)).isAir()) {
         this.gone(BlockPos.fromLong(l));
      }
   }

   @Override
   public void reset() {
      this.detections.clear();
      this.layer.clear();
   }

   private void scanBlockEntities(WorldChunk worldChunk) {
      for (BlockEntity var3 : worldChunk.getBlockEntities().values()) {
         this.found(var3.getPos(), this.typeOf(var3.getType()));
      }
   }

   private void blockChanged(BlockPos blockPos, BlockState blockState) {
      StorageEspModule.Type var3 = this.typeOf(blockState.getBlock());
      if (var3 != null) {
         this.found(blockPos, var3);
      } else if (blockState.isAir()) {
         this.gone(blockPos);
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent packetAppliedEvent) {
      ClientWorld var3 = MinecraftClient.getInstance().world;
      if (var3 != null) {
         if (var3.getRegistryKey() != this.shownDimension) {
            this.rescan();
         }

         Packet var2 = packetAppliedEvent.packet;
         if (packetAppliedEvent.packet instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket var4 = (ChunkDataS2CPacket)var2;
            var4.getChunkData()
               .getBlockEntities(var4.getChunkX(), var4.getChunkZ())
               .accept((blockPos, blockEntityType, nbtCompound) -> this.found(blockPos, this.typeOf(blockEntityType, nbtCompound)));
            WorldChunk var5 = var3.getChunkManager().getWorldChunk(var4.getChunkX(), var4.getChunkZ());
            if (var5 != null) {
               this.scanBlockEntities(var5);
            }
         } else if (packetAppliedEvent.packet instanceof BlockUpdateS2CPacket var8) {
            this.blockChanged(var8.getPos(), var8.getState());
         } else if (packetAppliedEvent.packet instanceof ChunkDeltaUpdateS2CPacket var9) {
            var9.visitUpdates(this::blockChanged);
         } else if (packetAppliedEvent.packet instanceof GameJoinS2CPacket) {
            this.detections.clear();
            this.layer.clear();
         }
      }
   }

   @EventHandler
   public void onPacketReceiveNetty(PacketReceiveNettyEvent packetReceiveNettyEvent) {
      if (packetReceiveNettyEvent.packet instanceof BlockEntityUpdateS2CPacket var3) {
         BlockPos var4 = var3.getPos();
         StorageEspModule.Type var5 = this.typeOf(var3.getBlockEntityType(), var3.getNbt());
         if (var5 != null) {
            MinecraftClient.getInstance().execute(() -> {
               if (this.isEnabled()) {
                  this.found(var4, var5);
               }
            });
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent render3DEvent) {
      ClientWorld var2 = MinecraftClient.getInstance().world;
      if (var2 != null && var2.getRegistryKey() != this.shownDimension) {
         this.rescan();
      }

      this.layer.render(render3DEvent, this.style);
      if (this.anyTracer()) {
         this.layer.renderTracers(this.style);
      }
   }

   @Environment(EnvType.CLIENT)
   private final class Type {
      final BooleanSetting enabled;
      final BooleanSetting tracers;
      final ColorSetting color;
      final ColorSetting tracerColor;

      Type(String string, boolean bl, int i, String string2, String string3, String string4, String string5) {
         this.enabled = StorageEspModule.this.addSetting(string2, new BooleanSetting(string, bl));
         this.color = StorageEspModule.this.addSetting(string3, new ColorSetting(string + " Color", i));
         this.tracers = StorageEspModule.this.addSetting(string4, new BooleanSetting(string + " Tracers", false));
         this.tracerColor = StorageEspModule.this.addSetting(string5, new ColorSetting(string + " Tracer Color", i));
         this.color.visibleWhen(this.enabled::get);
         this.tracers.visibleWhen(() -> StorageEspModule.this.sTracers.get() && this.enabled.get());
         this.tracerColor.visibleWhen(() -> StorageEspModule.this.sTracers.get() && this.enabled.get() && this.tracers.get());
         this.enabled.onChange(StorageEspModule.this::rescan);
      }
   }
}
