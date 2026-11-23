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

   public static boolean contains(BlockPos var0) {
      return FOUND.containsKey(var0);
   }

   public static boolean containsReal(BlockPos var0) {
      BlockEntityLeakTracker.Entry var1 = FOUND.get(var0);
      return var1 != null && var1.real();
   }

   public static int count(BlockEntityLeakTracker.Source var0) {
      return COUNTS.get(var0.ordinal());
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

   public static void recordAdditions(boolean var0) {
      recordAdditions = var0;
      if (!var0) {
         ADDED.clear();
      }
   }

   public static BlockPos pollAdded() {
      return ADDED.poll();
   }

   public static void ingest(Packet<?> var0) {
      if (var0 instanceof ChunkDataS2CPacket var1) {
         ChunkData var2 = var1.method_38598();

         try {
            var2.method_38587(var1.method_11523(), var1.method_11524()).accept((var0x, var1x, var2x) -> {
               if (var1x != null) {
                  put(var0x, var1x, var2x, BlockEntityLeakTracker.Source.CHUNK);
               }
            });
         } catch (Throwable var4) {
         }
      } else if (var0 instanceof BlockEntityUpdateS2CPacket var5) {
         put(var5.method_11293(), var5.method_11291(), var5.method_11290(), BlockEntityLeakTracker.Source.DATA);
      } else if (var0 instanceof BlockEventS2CPacket var6) {
         if (var6.method_11295() instanceof BlockEntityProvider) {
            hint(var6.method_11298(), var6.method_11295(), BlockEntityLeakTracker.Source.EVENT);
         }
      } else if (var0 instanceof WorldEventS2CPacket var7) {
         Block var10 = eventBlock(var7.method_11532());
         if (var10 != null && !var7.method_11533()) {
            hint(var7.method_11531(), var10, BlockEntityLeakTracker.Source.EVENT);
         }
      } else if (var0 instanceof PlaySoundS2CPacket var8) {
         Block var11 = soundBlock(var8.method_11894());
         if (var11 != null) {
            hintAt(var8.method_11890(), var8.method_11889(), var8.method_11893(), var11);
         }
      } else if (var0 instanceof BlockUpdateS2CPacket var9) {
         dropRevealedHint(var9.method_11309(), var9.method_11308());
      } else if (var0 instanceof GameJoinS2CPacket || var0 instanceof PlayerRespawnS2CPacket) {
         clear();
      }
   }

   public static void hint(BlockPos var0, Block var1, BlockEntityLeakTracker.Source var2) {
      BlockPos var3 = var0.method_10062();
      BlockEntityLeakTracker.Entry var4 = FOUND.get(var3);
      if (var4 == null || !var4.real() && var4.block() == Blocks.field_10124 && var1 != Blocks.field_10124) {
         FOUND.put(var3, new BlockEntityLeakTracker.Entry(var1, null, var2));
         COUNTS.incrementAndGet(var2.ordinal());
         if (recordAdditions) {
            ADDED.add(var3);
         }
      }
   }

   private static void put(BlockPos var0, BlockEntityType<?> var1, NbtCompound var2, BlockEntityLeakTracker.Source var3) {
      if (!PerformanceModule.optimizationsEnabled() || !containsReal(var0)) {
         BlockPos var4 = var0.method_10062();
         NbtCompound var5 = var2 != null && !var2.method_33133() ? var2 : null;
         BlockEntityLeakTracker.Entry var6 = FOUND.get(var4);
         if (var6 == null || !var6.real()) {
            FOUND.put(var4, new BlockEntityLeakTracker.Entry(blockFromType(var1), var5, var3));
            COUNTS.incrementAndGet(var3.ordinal());
            if (recordAdditions) {
               ADDED.add(var4);
            }
         } else if (var5 != null && !var5.equals(var6.tag())) {
            FOUND.replace(var4, var6, new BlockEntityLeakTracker.Entry(var6.block(), var5, var6.source()));
         }
      }
   }

   private static void dropRevealedHint(BlockPos var0, BlockState var1) {
      BlockEntityLeakTracker.Entry var2 = FOUND.get(var0);
      if (var2 != null && !var2.real()) {
         if (!var1.method_26215()
            && !var1.method_27852(Blocks.field_10340)
            && !var1.method_27852(Blocks.field_28888)
            && !(var1.method_26204() instanceof BlockEntityProvider)) {
            if (FOUND.remove(var0, var2)) {
               GENERATION.incrementAndGet();
            }
         }
      }
   }

   private static void hintAt(double var0, double var2, double var4, Block var6) {
      int var8 = (int)Math.floor(var0);
      int var9 = (int)Math.floor(var2);
      int var10 = (int)Math.floor(var4);
      double var11 = var0 - var8;
      double var13 = var2 - var9;
      double var15 = var4 - var10;
      if (!(Math.abs(var13 - 0.5) > 0.07)) {
         boolean var17 = var11 < 0.07;
         boolean var7 = var15 < 0.07;
         if ((var17 || !(Math.abs(var11 - 0.5) > 0.07)) && (var7 || !(Math.abs(var15 - 0.5) > 0.07)) && (!var17 || !var7)) {
            hint(new BlockPos(var8, var9, var10), var6, BlockEntityLeakTracker.Source.SOUND);
            if (var17) {
               hint(new BlockPos(var8 - 1, var9, var10), var6, BlockEntityLeakTracker.Source.SOUND);
            }

            if (var7) {
               hint(new BlockPos(var8, var9, var10 - 1), var6, BlockEntityLeakTracker.Source.SOUND);
            }
         }
      }
   }

   private static Block eventBlock(int var0) {
      return switch (var0) {
         case 1000, 1001, 1002, 2000 -> Blocks.field_10200;
         case 1035 -> Blocks.field_10333;
         case 1049, 1050, 2010 -> Blocks.field_46797;
         default -> null;
      };
   }

   private static Block soundBlock(RegistryEntry<SoundEvent> var0) {
      Identifier var2 = ((SoundEvent)var0.comp_349()).comp_3319();
      if (!var2.method_12836().equals("minecraft")) {
         return null;
      } else {
         String var3 = var2.method_12832();
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
                  case "chest" -> Blocks.field_10034;
                  case "ender_chest" -> Blocks.field_10443;
                  case "barrel" -> Blocks.field_16328;
                  case "shulker_box" -> Blocks.field_10603;
                  case "beacon" -> Blocks.field_10327;
                  case "vault" -> Blocks.field_48851;
                  case "trial_spawner" -> Blocks.field_47336;
                  case "chiseled_bookshelf" -> Blocks.field_40276;
                  case "copper_chest" -> (Block)Registries.field_41175.method_63535(Identifier.method_60656("copper_chest"));
                  default -> null;
               };
            }
         }
      }
   }

   public static Block blockFromType(BlockEntityType<?> var0) {
      Identifier var3 = Registries.field_41181.method_10221(var0);
      if (var3 == null) {
         return Blocks.field_10124;
      } else {
         String var4 = var3.method_12832();

         Block var1 = switch (var4) {
            case "mob_spawner" -> Blocks.field_10260;
            case "piston" -> Blocks.field_10008;
            case "sign" -> Blocks.field_10121;
            case "hanging_sign" -> Blocks.field_40262;
            case "skull" -> Blocks.field_10481;
            case "banner" -> Blocks.field_10154;
            case "bed" -> Blocks.field_10069;
            case "brushable_block" -> Blocks.field_42728;
            default -> null;
         };
         return var1 != null ? var1 : Registries.field_41175.method_10223(var3).<Block>map(RegistryEntry::comp_349).orElse(Blocks.field_10124);
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
