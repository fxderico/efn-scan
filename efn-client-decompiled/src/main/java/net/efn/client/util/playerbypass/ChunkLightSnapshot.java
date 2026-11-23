package net.efn.client.util.playerbypass;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ChunkLightSnapshot {
   public static final int BASE_DETECT_CEILING_Y = 62;
   public final int sectionCount;
   public final int bottomSectionY;
   public final LightSectionStatus[] sky;

   public ChunkLightSnapshot(int var1, int var2) {
      this.sectionCount = var1;
      this.bottomSectionY = var2;
      this.sky = new LightSectionStatus[var1 + 2];
      Arrays.fill(this.sky, LightSectionStatus.UNKNOWN);
   }

   public boolean hasBaseSignal() {
      for (int var1 = 1; var1 < this.sectionCount + 1; var1++) {
         if (candidate(var1, this.bottomSectionY) && this.sky[var1] == LightSectionStatus.ZEROED_PRESENT) {
            return true;
         }
      }

      return false;
   }

   static boolean candidate(int var0, int var1) {
      return (var1 + var0 - 1) * 16 + 15 < 62;
   }
}
