package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
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
   private final EspStyle style = new EspStyle((var1, var2) -> (S)this.addSetting(var1, var2), "Outline", 0.25F, 256.0F);
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
      public int color(Object var1) {
         return ((StorageEspModule.Type)var1).color.argb();
      }

      @Override
      public int tracerColor(Object var1) {
         StorageEspModule.Type var2 = (StorageEspModule.Type)var1;
         return var2.tracers.get() ? var2.tracerColor.argb() : 0;
      }
   }, () -> MinecraftClient.method_1551().field_1687 == null ? null : MinecraftClient.method_1551().field_1687.method_27983());
   private final Map<RegistryKey<World>, Long2ObjectOpenHashMap<StorageEspModule.Type>> detections = new HashMap<>();
   private RegistryKey<World> shownDimension;

   public StorageEspModule() {
      super("storage-esp", "Storage ESP Bypass", "Highlights storage blocks and spawners with per-type colours.", ModuleCategory.BASE_RAID);
   }

   private StorageEspModule.Type typeOf(Block var1) {
      if (var1 instanceof ChestBlock) {
         return this.chest;
      } else if (var1 instanceof EnderChestBlock) {
         return this.enderChest;
      } else if (var1 instanceof ShulkerBoxBlock) {
         return this.shulker;
      } else if (var1 instanceof SpawnerBlock) {
         return this.spawner;
      } else if (var1 instanceof AbstractFurnaceBlock) {
         return this.furnace;
      } else if (var1 instanceof BarrelBlock) {
         return this.barrel;
      } else if (var1 instanceof EnchantingTableBlock) {
         return this.enchanting;
      } else {
         return var1 instanceof HopperBlock ? this.hopper : null;
      }
   }

   private StorageEspModule.Type typeOf(BlockEntityType<?> var1) {
      if (var1 == BlockEntityType.field_11914 || var1 == BlockEntityType.field_11891) {
         return this.chest;
      } else if (var1 == BlockEntityType.field_11901) {
         return this.enderChest;
      } else if (var1 == BlockEntityType.field_11896) {
         return this.shulker;
      } else if (var1 == BlockEntityType.field_11889) {
         return this.spawner;
      } else if (var1 == BlockEntityType.field_11903 || var1 == BlockEntityType.field_16415 || var1 == BlockEntityType.field_16414) {
         return this.furnace;
      } else if (var1 == BlockEntityType.field_16411) {
         return this.barrel;
      } else if (var1 == BlockEntityType.field_11912) {
         return this.enchanting;
      } else {
         return var1 == BlockEntityType.field_11888 ? this.hopper : null;
      }
   }

   private StorageEspModule.Type typeOf(BlockEntityType<?> var1, NbtCompound var2) {
      StorageEspModule.Type var3 = this.typeOf(var1);
      if (var3 == null && var2 != null) {
         Identifier var4 = var2.method_10558("id").<Identifier>map(Identifier::method_12829).orElse(null);
         return var4 == null ? null : this.typeOf((BlockEntityType<?>)Registries.field_41181.method_63535(var4));
      } else {
         return var3;
      }
   }

   private void rescan() {
      if (this.isEnabled()) {
         this.layer.clear();
         ClientWorld var2 = MinecraftClient.method_1551().field_1687;
         this.shownDimension = var2 == null ? null : var2.method_27983();
         Long2ObjectOpenHashMap var1 = this.shownDimension == null ? null : this.detections.get(this.shownDimension);
         if (var1 != null) {
            ObjectIterator var4 = var1.long2ObjectEntrySet().iterator();

            while (var4.hasNext()) {
               Entry var5 = (Entry)var4.next();
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
      ClientWorld var1 = MinecraftClient.method_1551().field_1687;
      RegistryKey var2 = this.shownDimension = var1 == null ? null : var1.method_27983();
      if (var1 != null && MinecraftClient.method_1551().field_1724 != null) {
         ChunkPos var3 = MinecraftClient.method_1551().field_1724.method_31476();
         int var4 = MinecraftClient.method_1551().field_1690.method_38521() + 1;

         for (int var5 = var3.field_9181 - var4; var5 <= var3.field_9181 + var4; var5++) {
            for (int var6 = var3.field_9180 - var4; var6 <= var3.field_9180 + var4; var6++) {
               WorldChunk var7 = var1.method_2935().method_21730(var5, var6);
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

   private void found(BlockPos var1, StorageEspModule.Type var2) {
      ClientWorld var3 = MinecraftClient.method_1551().field_1687;
      if (var2 != null && var3 != null) {
         long var4 = var1.method_10063();
         StorageEspModule.Type var6 = (StorageEspModule.Type)this.detections
            .computeIfAbsent(var3.method_27983(), var0 -> new Long2ObjectOpenHashMap())
            .put(var4, var2);
         if (var6 != var2) {
            if (var6 != null) {
               this.layer.remove(var4);
            }

            if (var2.enabled.get()) {
               this.layer.put(var4, var2);
            }
         }
      }
   }

   private void gone(BlockPos var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      if (var2 != null) {
         Long2ObjectOpenHashMap var3 = this.detections.get(var2.method_27983());
         if (var3 != null && var3.remove(var1.method_10063()) != null) {
            this.layer.remove(var1.method_10063());
         }
      }
   }

   @Override
   public boolean wants(BlockState var1) {
      return this.typeOf(var1.method_26204()) != null;
   }

   @Override
   public void found(long var1, BlockState var3) {
      this.found(BlockPos.method_10092(var1), this.typeOf(var3.method_26204()));
   }

   @Override
   public void removed(long var1) {
      ClientWorld var3 = MinecraftClient.method_1551().field_1687;
      if (var3 != null && var3.method_8320(BlockPos.method_10092(var1)).method_26215()) {
         this.gone(BlockPos.method_10092(var1));
      }
   }

   @Override
   public void reset() {
      this.detections.clear();
      this.layer.clear();
   }

   private void scanBlockEntities(WorldChunk var1) {
      for (BlockEntity var3 : var1.method_12214().values()) {
         this.found(var3.method_11016(), this.typeOf(var3.method_11017()));
      }
   }

   private void blockChanged(BlockPos var1, BlockState var2) {
      StorageEspModule.Type var3 = this.typeOf(var2.method_26204());
      if (var3 != null) {
         this.found(var1, var3);
      } else if (var2.method_26215()) {
         this.gone(var1);
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent var1) {
      ClientWorld var3 = MinecraftClient.method_1551().field_1687;
      if (var3 != null) {
         if (var3.method_27983() != this.shownDimension) {
            this.rescan();
         }

         Packet var2 = var1.packet;
         if (var1.packet instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket var4 = (ChunkDataS2CPacket)var2;
            var4.method_38598()
               .method_38587(var4.method_11523(), var4.method_11524())
               .accept((var1x, var2x, var3x) -> this.found(var1x, this.typeOf(var2x, var3x)));
            WorldChunk var5 = var3.method_2935().method_21730(var4.method_11523(), var4.method_11524());
            if (var5 != null) {
               this.scanBlockEntities(var5);
            }
         } else if (var1.packet instanceof BlockUpdateS2CPacket var8) {
            this.blockChanged(var8.method_11309(), var8.method_11308());
         } else if (var1.packet instanceof ChunkDeltaUpdateS2CPacket var9) {
            var9.method_30621(this::blockChanged);
         } else if (var1.packet instanceof GameJoinS2CPacket) {
            this.detections.clear();
            this.layer.clear();
         }
      }
   }

   @EventHandler
   public void onPacketReceiveNetty(PacketReceiveNettyEvent var1) {
      if (var1.packet instanceof BlockEntityUpdateS2CPacket var3) {
         BlockPos var4 = var3.method_11293();
         StorageEspModule.Type var5 = this.typeOf(var3.method_11291(), var3.method_11290());
         if (var5 != null) {
            MinecraftClient.method_1551().execute(() -> {
               if (this.isEnabled()) {
                  this.found(var4, var5);
               }
            });
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      ClientWorld var2 = MinecraftClient.method_1551().field_1687;
      if (var2 != null && var2.method_27983() != this.shownDimension) {
         this.rescan();
      }

      this.layer.render(var1, this.style);
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

      Type(String nullx, boolean nullxx, int nullxxx, String nullxxxx, String nullxxxxx, String nullxxxxxx, String nullxxxxxxx) {
         this.enabled = StorageEspModule.this.addSetting(nullxxxx, new BooleanSetting(nullx, nullxx));
         this.color = StorageEspModule.this.addSetting(nullxxxxx, new ColorSetting(nullx + " Color", nullxxx));
         this.tracers = StorageEspModule.this.addSetting(nullxxxxxx, new BooleanSetting(nullx + " Tracers", false));
         this.tracerColor = StorageEspModule.this.addSetting(nullxxxxxxx, new ColorSetting(nullx + " Tracer Color", nullxxx));
         this.color.visibleWhen(this.enabled::get);
         this.tracers.visibleWhen(() -> StorageEspModule.this.sTracers.get() && this.enabled.get());
         this.tracerColor.visibleWhen(() -> StorageEspModule.this.sTracers.get() && this.enabled.get() && this.tracers.get());
         this.enabled.onChange(StorageEspModule.this::rescan);
      }
   }
}
