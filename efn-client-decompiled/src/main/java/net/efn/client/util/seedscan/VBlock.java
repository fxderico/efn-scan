package net.efn.client.util.seedscan;

import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

@Environment(EnvType.CLIENT)
public final class VBlock {
   public static final byte AIR = 0;
   public static final byte WATER = 1;
   public static final byte LAVA = 2;
   public static final byte STONE = 3;
   public static final byte GRANITE = 4;
   public static final byte DIORITE = 5;
   public static final byte ANDESITE = 6;
   public static final byte TUFF = 7;
   public static final byte GRAVEL = 8;
   public static final byte DIRT = 9;
   public static final byte CLAY = 10;
   public static final byte ORE = 11;
   public static final byte OTHER = 12;
   public static final byte UNKNOWN = 15;
   public static final int FAMILY_GRANITE = 1;
   public static final int FAMILY_DIORITE = 2;
   public static final int FAMILY_ANDESITE = 4;
   public static final int FAMILY_GRAVEL = 8;
   private static volatile Map<Block, Byte> kinds;

   private VBlock() {
   }

   public static boolean isBase(byte var0) {
      return var0 >= 3 && var0 <= 7;
   }

   public static boolean isOpen(byte var0) {
      return var0 == 0 || var0 == 1 || var0 == 2;
   }

   public static int familyBit(byte var0) {
      return switch (var0) {
         case 4 -> 1;
         case 5 -> 2;
         case 6 -> 4;
         default -> 0;
         case 8 -> 8;
      };
   }

   public static Block familyBlock(byte var0) {
      return switch (var0) {
         case 4 -> Blocks.field_10474;
         case 5 -> Blocks.field_10508;
         case 6 -> Blocks.field_10115;
         default -> throw new IllegalArgumentException("not a blob kind: " + var0);
         case 8 -> Blocks.field_10255;
      };
   }

   public static byte of(BlockState var0) {
      if (var0.method_26215()) {
         return 0;
      } else {
         Byte var1 = kinds().get(var0.method_26204());
         return var1 != null ? var1 : 12;
      }
   }

   private static Map<Block, Byte> kinds() {
      Map var0 = kinds;
      if (var0 != null) {
         return var0;
      } else {
         IdentityHashMap var5 = new IdentityHashMap();
         var5.put(Blocks.field_10340, (byte)3);
         var5.put(Blocks.field_28888, (byte)3);
         var5.put(Blocks.field_10474, (byte)4);
         var5.put(Blocks.field_10508, (byte)5);
         var5.put(Blocks.field_10115, (byte)6);
         var5.put(Blocks.field_27165, (byte)7);
         var5.put(Blocks.field_10255, (byte)8);
         var5.put(Blocks.field_10566, (byte)9);
         var5.put(Blocks.field_10460, (byte)10);
         var5.put(Blocks.field_10382, (byte)1);
         var5.put(Blocks.field_10164, (byte)2);

         for (Block var4 : new Block[]{
            Blocks.field_10418,
            Blocks.field_29219,
            Blocks.field_10212,
            Blocks.field_29027,
            Blocks.field_27120,
            Blocks.field_29221,
            Blocks.field_10571,
            Blocks.field_29026,
            Blocks.field_10080,
            Blocks.field_29030,
            Blocks.field_10090,
            Blocks.field_29028,
            Blocks.field_10442,
            Blocks.field_29029,
            Blocks.field_10013,
            Blocks.field_29220,
            Blocks.field_33508,
            Blocks.field_33509,
            Blocks.field_10277,
            Blocks.field_29224
         }) {
            var5.put(var4, (byte)11);
         }

         kinds = var5;
         return var5;
      }
   }
}
