package net.efn.client.module.impl.render.character;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class CharacterRotationTable {
   final float[] values = new float[153];

   CharacterRotationTable() {
   }

   void update(float var1, float var2, float var3) {
      for (int var4 = 0; var4 <= 16; var4++) {
         float var5 = var4 / 16.0F;
         float var6 = (float)Math.cos(var1 * var5);
         float var7 = (float)Math.sin(var1 * var5);
         float var8 = (float)Math.cos(var2 * var5);
         float var9 = (float)Math.sin(var2 * var5);
         float var10 = (float)Math.cos(var3 * var5);
         float var11 = (float)Math.sin(var3 * var5);
         int var12 = var4 * 9;
         this.values[var12] = var10 * var8;
         this.values[var12 + 1] = var10 * var9 * var7 - var11 * var6;
         this.values[var12 + 2] = var10 * var9 * var6 + var11 * var7;
         this.values[var12 + 3] = var11 * var8;
         this.values[var12 + 4] = var11 * var9 * var7 + var10 * var6;
         this.values[var12 + 5] = var11 * var9 * var6 - var10 * var7;
         this.values[var12 + 6] = -var9;
         this.values[var12 + 7] = var8 * var7;
         this.values[var12 + 8] = var8 * var6;
      }
   }
}
