package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CharacterModel {
   public final String id;
   public final String name;
   public final Detail detail;
   public final float scale;
   public final PartMesh[] parts;
   public final int motionSlots;
   public final int atlasWidth;
   public final int atlasHeight;
   public final int[][] atlasMips;
   public final float whiteU;
   public final float whiteV;
   public final float minY;
   public final float maxY;
   public final float hipY;
   public final boolean firstPersonArms;
   public final long buildMillis;

   CharacterModel(
      String string,
      String string2,
      Detail detail,
      float f,
      PartMesh[] partMeshs,
      int i,
      int j,
      int k,
      int[][] is,
      float g,
      float h,
      float l,
      float m,
      float n,
      boolean bl,
      long o
   ) {
      this.id = string;
      this.name = string2;
      this.detail = detail;
      this.scale = f;
      this.parts = partMeshs;
      this.motionSlots = i;
      this.atlasWidth = j;
      this.atlasHeight = k;
      this.atlasMips = is;
      this.whiteU = g;
      this.whiteV = h;
      this.minY = l;
      this.maxY = m;
      this.hipY = n;
      this.firstPersonArms = bl;
      this.buildMillis = o;
   }

   public int triangles() {
      int var1 = 0;

      for (PartMesh var5 : this.parts) {
         var1 += var5.triangles();
      }

      return var1;
   }

   public PartMesh part(Bone bone) {
      for (PartMesh var5 : this.parts) {
         if (!var5.extra && var5.bone == bone) {
            return var5;
         }
      }

      return null;
   }
}
