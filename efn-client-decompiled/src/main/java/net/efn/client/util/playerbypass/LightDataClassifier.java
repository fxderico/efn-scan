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

   public static LightSectionStatus classifyPayload(byte[] bs) {
      if (bs != null && bs.length == 2048) {
         boolean var1 = true;
         boolean var2 = true;

         for (byte var6 : bs) {
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

   public static void apply(LightData lightData, ChunkLightSnapshot chunkLightSnapshot) {
      applyTrack(lightData.getInitedSky(), lightData.getUninitedSky(), lightData.getSkyNibbles(), chunkLightSnapshot.sky);
   }

   static int sentNotDark(LightData lightData, int i, int j) {
      BitSet var3 = lightData.getInitedSky();
      BitSet var4 = lightData.getUninitedSky();
      List var5 = lightData.getSkyNibbles();
      int var6 = 0;
      int var7 = 0;

      for (int var8 = 0; var8 < i + 2; var8++) {
         boolean var10 = var3.get(var8);
         byte[] var9 = var10 && var7 < var5.size() ? (byte[])var5.get(var7) : null;
         if (var10) {
            var7++;
         }

         if (var8 >= 1 && var8 <= i && var8 < 32 && ChunkLightSnapshot.candidate(var8, j)) {
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

   static void applyTrack(BitSet bitSet, BitSet bitSet2, List<byte[]> list, LightSectionStatus[] lightSectionStatuss) {
      int var4 = 0;

      for (int var5 = 0; var5 < lightSectionStatuss.length; var5++) {
         if (bitSet.get(var5)) {
            byte[] var6 = var4 < list.size() ? (byte[])list.get(var4) : null;
            var4++;
            lightSectionStatuss[var5] = classifyPayload(var6);
         } else if (bitSet2.get(var5)) {
            lightSectionStatuss[var5] = LightSectionStatus.EMPTY_MASK;
         }
      }
   }
}
