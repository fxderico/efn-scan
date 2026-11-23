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
      String string,
      Bone bone,
      int i,
      boolean bl,
      float f,
      float g,
      float h,
      float j,
      float k,
      float l,
      Motion motion,
      Surface surface,
      Surface surface2,
      Surface surface3,
      int m,
      float n,
      float o,
      boolean bl2,
      float p,
      float q,
      float r,
      boolean bl3,
      float s,
      float t,
      float u
   ) {
      this.name = string;
      this.bone = bone;
      this.parent = i;
      this.extra = bl;
      this.pivotX = f;
      this.pivotY = g;
      this.pivotZ = h;
      this.gripX = j;
      this.gripY = k;
      this.gripZ = l;
      this.motion = motion;
      this.base = surface;
      this.outline = surface2;
      this.decals = surface3;
      this.motionSlot = m;
      this.swingScale = n;
      this.swingLimit = o;
      this.jointed = bl2;
      this.jointX = p;
      this.jointY = q;
      this.jointZ = r;
      this.neck = bl3;
      this.lookPitch = s;
      this.lookYaw = t;
      this.lookLimit = u;
   }

   public boolean bends() {
      return this.motion != null && this.motion.bendPower() > 0.0F;
   }

   public boolean swings() {
      return this.swingScale != 1.0F || this.swingLimit > 0.0F;
   }

   public float swingPitch(float f, float g, float h) {
      if (h > 0.0F && this.swings()) {
         float var4 = Motion.soft(g * this.swingScale, this.swingLimit);
         return f + Math.min(1.0F, h) * (var4 - g);
      } else {
         return f;
      }
   }

   public int triangles() {
      return (this.base == null ? 0 : this.base.triangles)
         + (this.outline == null ? 0 : this.outline.triangles)
         + (this.decals == null ? 0 : this.decals.triangles);
   }
}
