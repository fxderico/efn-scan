package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class Mesh {
   final float[] pos;
   final int vertexCount;
   final int[] tris;
   final int triCount;

   Mesh(float[] var1, int var2, int[] var3, int var4) {
      this.pos = var1;
      this.vertexCount = var2;
      this.tris = var3;
      this.triCount = var4;
   }
}
