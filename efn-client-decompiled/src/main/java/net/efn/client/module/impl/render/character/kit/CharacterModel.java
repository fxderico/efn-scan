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
      String var1,
      String var2,
      Detail var3,
      float var4,
      PartMesh[] var5,
      int var6,
      int var7,
      int var8,
      int[][] var9,
      float var10,
      float var11,
      float var12,
      float var13,
      float var14,
      boolean var15,
      long var16
   ) {
      this.id = var1;
      this.name = var2;
      this.detail = var3;
      this.scale = var4;
      this.parts = var5;
      this.motionSlots = var6;
      this.atlasWidth = var7;
      this.atlasHeight = var8;
      this.atlasMips = var9;
      this.whiteU = var10;
      this.whiteV = var11;
      this.minY = var12;
      this.maxY = var13;
      this.hipY = var14;
      this.firstPersonArms = var15;
      this.buildMillis = var16;
   }

   public int triangles() {
      int var1 = 0;

      for (PartMesh var5 : this.parts) {
         var1 += var5.triangles();
      }

      return var1;
   }

   public PartMesh part(Bone var1) {
      for (PartMesh var5 : this.parts) {
         if (!var5.extra && var5.bone == var1) {
            return var5;
         }
      }

      return null;
   }
}
