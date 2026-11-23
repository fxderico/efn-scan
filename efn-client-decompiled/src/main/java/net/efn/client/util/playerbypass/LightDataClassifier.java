package net.efn.client.util.playerbypass;

import java.util.BitSet;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.s2c.play.LightData;

@Environment(EnvType.CLIENT)
public final class LightDataClassifier {
   private static final int NIBBLES_PER_SECTION = 2048;

   private LightDataClassifier() {
   }

   public static LightSectionStatus classifyPayload(byte[] var0) {
      if (var0 != null && var0.length == 2048) {
         boolean var1 = true;
         boolean var2 = true;

         for (byte var6 : var0) {
            if (var6 != 0) {
               var1 = false;
            }

            if (var6 != -1) {
               var2 = false;
            }

            if (!var1 && !var2) {
               return LightSectionStatus.NORMAL;
            }
         }

         if (var1) {
            return LightSectionStatus.ZEROED_PRESENT;
         } else {
            return var2 ? LightSectionStatus.UNIFORM_MAX : LightSectionStatus.NORMAL;
         }
      } else {
         return LightSectionStatus.UNKNOWN;
      }
   }

   public static void apply(LightData var0, ChunkLightSnapshot var1) {
      applyTrack(var0.method_38601(), var0.method_38604(), var0.method_38606(), var1.sky);
   }

   static int sentNotDark(LightData var0, int var1, int var2) {
      BitSet var3 = var0.method_38601();
      BitSet var4 = var0.method_38604();
      List var5 = var0.method_38606();
      int var6 = 0;
      int var7 = 0;

      for (int var8 = 0; var8 < var1 + 2; var8++) {
         boolean var10 = var3.get(var8);
         byte[] var9 = var10 && var7 < var5.size() ? (byte[])var5.get(var7) : null;
         if (var10) {
            var7++;
         }

         if (var8 >= 1 && var8 <= var1 && var8 < 32 && ChunkLightSnapshot.candidate(var8, var2)) {
            if (var10) {
               LightSectionStatus var12 = classifyPayload(var9);
               if (var12 == LightSectionStatus.NORMAL || var12 == LightSectionStatus.UNIFORM_MAX) {
                  var6 |= 1 << var8;
               }
            } else if (var4.get(var8)) {
               var6 |= 1 << var8;
            }
         }
      }

      return var6;
   }

   static void applyTrack(BitSet var0, BitSet var1, List<byte[]> var2, LightSectionStatus[] var3) {
      int var4 = 0;

      for (int var5 = 0; var5 < var3.length; var5++) {
         if (var0.get(var5)) {
            byte[] var6 = var4 < var2.size() ? (byte[])var2.get(var4) : null;
            var4++;
            var3[var5] = classifyPayload(var6);
         } else if (var1.get(var5)) {
            var3[var5] = LightSectionStatus.EMPTY_MASK;
         }
      }
   }
}
