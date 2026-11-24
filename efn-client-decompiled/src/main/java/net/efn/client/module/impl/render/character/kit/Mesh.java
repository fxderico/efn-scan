package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class Mesh {
   final float[] pos;
   final int vertexCount;
   final int[] tris;
   final int triCount;

   Mesh(float[] fs, int i, int[] is, int j) {
      this.pos = fs;
      this.vertexCount = i;
      this.tris = is;
      this.triCount = j;
   }
}
