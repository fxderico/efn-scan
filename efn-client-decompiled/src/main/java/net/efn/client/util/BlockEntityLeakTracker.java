package net.efn.client.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import net.efn.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class BlockEntityLeakTracker {
   private static final double ON_EDGE = 0.07;
   private static final Map<BlockPos, BlockEntityLeakTracker.Entry> FOUND = new ConcurrentHashMap<>();
   private static final ConcurrentLinkedQueue<BlockPos> ADDED = new ConcurrentLinkedQueue<>();
   private static final AtomicInteger GENERATION = new AtomicInteger();
   private static final AtomicIntegerArray COUNTS = new AtomicIntegerArray(BlockEntityLeakTracker.Source.values().length);
   private static volatile boolean recordAdditions;

   private BlockEntityLeakTracker() {
   }

   public static Map<BlockPos, BlockEntityLeakTracker.Entry> view() {
      return FOUND;
   }

   public static boolean contains(BlockPos blockPos) {
      return FOUND.containsKey(blockPos);
   }

   public static boolean containsReal(BlockPos blockPos) {
      BlockEntityLeakTracker.Entry var1 = FOUND.get(blockPos);
      return var1 != null && var1.real();
   }

   public static int count(BlockEntityLeakTracker.Source source) {
      return COUNTS.get(source.ordinal());
   }

   public static void clear() {
      FOUND.clear();
      ADDED.clear();

      for (int var0 = 0; var0 < COUNTS.length(); var0++) {
         COUNTS.set(var0, 0);
      }

      GENERATION.incrementAndGet();
   }

   public static int generation() {
      return GENERATION.get();
   }

   public static void recordAdditions(boolean bl) {
      recordAdditions = bl;
      if (!bl) {
         ADDED.clear();
      }
   }

   public static BlockPos pollAdded() {
      return ADDED.poll();
   }

   public static void ingest(Packet<?> packet) {
      if (packet instanceof ChunkDataS2CPacket var1) {
         ChunkData var2 = var1.getChunkData();

         try {
            var2.getBlockEntities(var1.getChunkX(), var1.getChunkZ()).accept((blockPos, blockEntityType, nbtCompound) -> {
               if (blockEntityType != null) {
                  put(blockPos, blockEntityType, nbtCompound, BlockEntityLeakTracker.Source.CHUNK);
               }
            });
         } catch (Throwable var4) {
         }
      } else if (packet instanceof BlockEntityUpdateS2CPacket var5) {
         put(var5.getPos(), var5.getBlockEntityType(), var5.getNbt(), BlockEntityLeakTracker.Source.DATA);
      } else if (packet instanceof BlockEventS2CPacket var6) {
         if (var6.getBlock() instanceof BlockEntityProvider) {
            hint(var6.getPos(), var6.getBlock(), BlockEntityLeakTracker.Source.EVENT);
         }
      } else if (packet instanceof WorldEventS2CPacket var7) {
         Block var10 = eventBlock(var7.getEventId());
         if (var10 != null && !var7.isGlobal()) {
            hint(var7.getPos(), var10, BlockEntityLeakTracker.Source.EVENT);
         }
      } else if (packet instanceof PlaySoundS2CPacket var8) {
         Block var11 = soundBlock(var8.getSound());
         if (var11 != null) {
            hintAt(var8.getX(), var8.getY(), var8.getZ(), var11);
         }
      } else if (packet instanceof BlockUpdateS2CPacket var9) {
         dropRevealedHint(var9.getPos(), var9.getState());
      } else if (packet instanceof GameJoinS2CPacket || packet instanceof PlayerRespawnS2CPacket) {
         clear();
      }
   }

   public static void hint(BlockPos blockPos, Block block, BlockEntityLeakTracker.Source source) {
      BlockPos var3 = blockPos.toImmutable();
      BlockEntityLeakTracker.Entry var4 = FOUND.get(var3);
      if (var4 == null || !var4.real() && var4.block() == Blocks.AIR && block != Blocks.AIR) {
         FOUND.put(var3, new BlockEntityLeakTracker.Entry(block, null, source));
         COUNTS.incrementAndGet(source.ordinal());
         if (recordAdditions) {
            ADDED.add(var3);
         }
      }
   }

   private static void put(BlockPos blockPos, BlockEntityType<?> blockEntityType, NbtCompound nbtCompound, BlockEntityLeakTracker.Source source) {
      if (!PerformanceModule.optimizationsEnabled() || !containsReal(blockPos)) {
         BlockPos var4 = blockPos.toImmutable();
         NbtCompound var5 = nbtCompound != null && !nbtCompound.isEmpty() ? nbtCompound : null;
         BlockEntityLeakTracker.Entry var6 = FOUND.get(var4);
         if (var6 == null || !var6.real()) {
            FOUND.put(var4, new BlockEntityLeakTracker.Entry(blockFromType(blockEntityType), var5, source));
            COUNTS.incrementAndGet(source.ordinal());
            if (recordAdditions) {
               ADDED.add(var4);
            }
         } else if (var5 != null && !var5.equals(var6.tag())) {
            FOUND.replace(var4, var6, new BlockEntityLeakTracker.Entry(var6.block(), var5, var6.source()));
         }
      }
   }

   private static void dropRevealedHint(BlockPos blockPos, BlockState blockState) {
      BlockEntityLeakTracker.Entry var2 = FOUND.get(blockPos);
      if (var2 != null && !var2.real()) {
         if (!blockState.isAir()
            && !blockState.isOf(Blocks.STONE)
            && !blockState.isOf(Blocks.DEEPSLATE)
            && !(blockState.getBlock() instanceof BlockEntityProvider)) {
            if (FOUND.remove(blockPos, var2)) {
               GENERATION.incrementAndGet();
            }
         }
      }
   }

   private static void hintAt(double d, double e, double f, Block block) {
      int var8 = (int)Math.floor(d);
      int var9 = (int)Math.floor(e);
      int var10 = (int)Math.floor(f);
      double var11 = d - var8;
      double var13 = e - var9;
      double var15 = f - var10;
      if (!(Math.abs(var13 - 0.5) > 0.07)) {
         boolean var17 = var11 < 0.07;
         boolean var7 = var15 < 0.07;
         if ((var17 || !(Math.abs(var11 - 0.5) > 0.07)) && (var7 || !(Math.abs(var15 - 0.5) > 0.07)) && (!var17 || !var7)) {
            hint(new BlockPos(var8, var9, var10), block, BlockEntityLeakTracker.Source.SOUND);
            if (var17) {
               hint(new BlockPos(var8 - 1, var9, var10), block, BlockEntityLeakTracker.Source.SOUND);
            }

            if (var7) {
               hint(new BlockPos(var8, var9, var10 - 1), block, BlockEntityLeakTracker.Source.SOUND);
            }
         }
      }
   }

   private static Block eventBlock(int i) {
      return switch (i) {
         case 1000, 1001, 1002, 2000 -> Blocks.DISPENSER;
         case 1035 -> Blocks.BREWING_STAND;
         case 1049, 1050, 2010 -> Blocks.CRAFTER;
         default -> null;
      };
   }

   private static Block soundBlock(RegistryEntry<SoundEvent> registryEntry) {
      Identifier var2 = ((SoundEvent)registryEntry.value()).id();
      if (!var2.getNamespace().equals("minecraft")) {
         return null;
      } else {
         String var3 = var2.getPath();
         if (!var3.startsWith("block.")) {
            return null;
         } else {
            String var4 = var3.substring("block.".length());
            int var5 = var4.indexOf(46);
            if (var5 < 0) {
               return null;
            } else {
               String var6 = var4.substring(0, var5);

               return switch (var6) {
                  case "chest" -> Blocks.CHEST;
                  case "ender_chest" -> Blocks.ENDER_CHEST;
                  case "barrel" -> Blocks.BARREL;
                  case "shulker_box" -> Blocks.SHULKER_BOX;
                  case "beacon" -> Blocks.BEACON;
                  case "vault" -> Blocks.VAULT;
                  case "trial_spawner" -> Blocks.TRIAL_SPAWNER;
                  case "chiseled_bookshelf" -> Blocks.CHISELED_BOOKSHELF;
                  case "copper_chest" -> (Block)Registries.BLOCK.get(Identifier.ofVanilla("copper_chest"));
                  default -> null;
               };
            }
         }
      }
   }

   public static Block blockFromType(BlockEntityType<?> blockEntityType) {
      Identifier var3 = Registries.BLOCK_ENTITY_TYPE.getId(blockEntityType);
      if (var3 == null) {
         return Blocks.AIR;
      } else {
         String var4 = var3.getPath();

         Block var1 = switch (var4) {
            case "mob_spawner" -> Blocks.SPAWNER;
            case "piston" -> Blocks.MOVING_PISTON;
            case "sign" -> Blocks.OAK_SIGN;
            case "hanging_sign" -> Blocks.OAK_HANGING_SIGN;
            case "skull" -> Blocks.SKELETON_SKULL;
            case "banner" -> Blocks.WHITE_BANNER;
            case "bed" -> Blocks.RED_BED;
            case "brushable_block" -> Blocks.SUSPICIOUS_SAND;
            default -> null;
         };
         return var1 != null ? var1 : Registries.BLOCK.getEntry(var3).map(RegistryEntry::value).orElse(Blocks.AIR);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Entry(Block block, NbtCompound tag, BlockEntityLeakTracker.Source source) {
      public boolean real() {
         return this.source == BlockEntityLeakTracker.Source.CHUNK || this.source == BlockEntityLeakTracker.Source.DATA;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Source {
      CHUNK,
      DATA,
      EVENT,
      SOUND,
      LIGHT;

      private Source() {
      }
   }
}
