package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PartMesh {
   public final String name;
   public final Bone bone;
   public final int parent;
   public final boolean extra;
   public final float pivotX;
   public final float pivotY;
   public final float pivotZ;
   public final float gripX;
   public final float gripY;
   public final float gripZ;
   public final Motion motion;
   public final Surface base;
   public final Surface outline;
   public final Surface decals;
   public final int motionSlot;
   public final float swingScale;
   public final float swingLimit;
   public final boolean jointed;
   public final float jointX;
   public final float jointY;
   public final float jointZ;
   public final boolean neck;
   public final float lookPitch;
   public final float lookYaw;
   public final float lookLimit;

   PartMesh(
      String var1,
      Bone var2,
      int var3,
      boolean var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      Motion var11,
      Surface var12,
      Surface var13,
      Surface var14,
      int var15,
      float var16,
      float var17,
      boolean var18,
      float var19,
      float var20,
      float var21,
      boolean var22,
      float var23,
      float var24,
      float var25
   ) {
      this.name = var1;
      this.bone = var2;
      this.parent = var3;
      this.extra = var4;
      this.pivotX = var5;
      this.pivotY = var6;
      this.pivotZ = var7;
      this.gripX = var8;
      this.gripY = var9;
      this.gripZ = var10;
      this.motion = var11;
      this.base = var12;
      this.outline = var13;
      this.decals = var14;
      this.motionSlot = var15;
      this.swingScale = var16;
      this.swingLimit = var17;
      this.jointed = var18;
      this.jointX = var19;
      this.jointY = var20;
      this.jointZ = var21;
      this.neck = var22;
      this.lookPitch = var23;
      this.lookYaw = var24;
      this.lookLimit = var25;
   }

   public boolean bends() {
      return this.motion != null && this.motion.bendPower() > 0.0F;
   }

   public boolean swings() {
      return this.swingScale != 1.0F || this.swingLimit > 0.0F;
   }

   public float swingPitch(float var1, float var2, float var3) {
      if (var3 > 0.0F && this.swings()) {
         float var4 = Motion.soft(var2 * this.swingScale, this.swingLimit);
         return var1 + Math.min(1.0F, var3) * (var4 - var2);
      } else {
         return var1;
      }
   }

   public int triangles() {
      return (this.base == null ? 0 : this.base.triangles)
         + (this.outline == null ? 0 : this.outline.triangles)
         + (this.decals == null ? 0 : this.decals.triangles);
   }
}
