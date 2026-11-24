package net.efn.client.module.impl.render.character.kit;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Part {
   final String name;
   final Bone bone;
   final Part parent;
   final boolean extra;
   Shape shape;
   float pivotX;
   float pivotY;
   float pivotZ;
   boolean pivotSet;
   float gripX;
   float gripY;
   float gripZ;
   boolean gripSet;
   int outlineColor;
   float outline = -1.0F;
   boolean outlineColorSet;
   final List<Decal> decals = new ArrayList<>();
   Motion motion;
   float detail = 1.0F;
   float resolution = 1.0F;
   float flat = -1.0F;
   float occlusion = -1.0F;
   float swing = 1.0F;
   float swingLimit;
   float jointX;
   float jointY;
   float jointZ;
   float jointDirX;
   float jointDirY = -1.0F;
   float jointDirZ;
   float jointBlend;
   boolean jointSet;
   boolean neck;
   float lookPitch;
   float lookYaw;
   float lookLimit;

   Part(String string, Bone bone, Part part, boolean bl) {
      this.name = string;
      this.bone = bone;
      this.parent = part;
      this.extra = bl;
      if (part == null) {
         this.pivotX = bone.pivotX;
         this.pivotY = bone.pivotY;
         this.pivotZ = bone.pivotZ;
      } else {
         this.pivotX = part.pivotX;
         this.pivotY = part.pivotY;
         this.pivotZ = part.pivotZ;
      }
   }

   public String name() {
      return this.name;
   }

   public Bone bone() {
      return this.bone;
   }

   public boolean extra() {
      return this.extra;
   }

   public Shape shape() {
      return this.shape;
   }

   public Part shape(Shape shape) {
      this.shape = shape;
      return this;
   }

   public Part pivot(float f, float g, float h) {
      this.pivotX = f;
      this.pivotY = g;
      this.pivotZ = h;
      this.pivotSet = true;
      return this;
   }

   public Vec3 pivot() {
      return new Vec3(this.pivotX, this.pivotY, this.pivotZ);
   }

   public Part grip(float f, float g, float h) {
      this.gripX = f;
      this.gripY = g;
      this.gripZ = h;
      this.gripSet = true;
      return this;
   }

   public Part outline(float f) {
      this.outline = Math.max(0.0F, f);
      return this;
   }

   public Part outline(int i, float f) {
      this.outlineColor = i;
      this.outlineColorSet = true;
      this.outline = Math.max(0.0F, f);
      return this;
   }

   public Part noOutline() {
      this.outline = 0.0F;
      return this;
   }

   public Part decal(Decal decal) {
      this.decals.add(decal);
      return this;
   }

   public Part motion(Motion motion) {
      this.motion = motion;
      return this;
   }

   public Part detail(float f) {
      this.detail = Math.max(0.05F, f);
      return this;
   }

   public Part resolution(float f) {
      this.resolution = Math.max(0.35F, Math.min(3.0F, f));
      return this;
   }

   public Part flatLight(float f) {
      this.flat = Math.max(0.0F, Math.min(1.0F, f));
      return this;
   }

   public Part occlusion(float f) {
      this.occlusion = Math.max(0.0F, Math.min(1.0F, f));
      return this;
   }

   public Part swing(float f) {
      return this.swing(f, 0.0F);
   }

   public Part swing(float f, float g) {
      if (!this.extra && (this.bone.arm() || this.bone.leg())) {
         this.swing = Math.max(0.0F, Math.min(2.0F, f));
         this.swingLimit = Math.max(0.0F, Math.min(90.0F, g));
         return this;
      } else {
         throw new IllegalStateException(this.name + ": swing() only applies to the arm and leg bone parts");
      }
   }

   public Part joint(float f, float g, float h) {
      return this.joint(f, g, h, 0.7F);
   }

   public Part joint(float f, float g, float h, float i) {
      if (!this.extra && (this.bone.arm() || this.bone.leg())) {
         this.jointX = f;
         this.jointY = g;
         this.jointZ = h;
         this.jointDirX = 0.0F;
         this.jointDirY = -1.0F;
         this.jointDirZ = 0.0F;
         this.jointBlend = Math.max(0.1F, i);
         this.jointSet = true;
         this.neck = false;
         return this;
      } else {
         throw new IllegalStateException(this.name + ": joint() only applies to the arm and leg bone parts");
      }
   }

   public Part neck(float f, float g, float h, float i, float j, float k, float l) {
      if (!this.bone.arm() && !this.bone.leg()) {
         float var8 = (float)Math.sqrt(i * i + j * j + k * k);
         if (!(var8 > 1.0E-4F)) {
            throw new IllegalArgumentException(this.name + ": neck() needs a direction");
         } else {
            this.jointX = f;
            this.jointY = g;
            this.jointZ = h;
            this.jointDirX = i / var8;
            this.jointDirY = j / var8;
            this.jointDirZ = k / var8;
            this.jointBlend = Math.max(0.1F, l);
            this.jointSet = true;
            this.neck = true;
            return this;
         }
      } else {
         throw new IllegalStateException(this.name + ": limbs bend with joint(), not neck()");
      }
   }

   public Part look(float f, float g, float h) {
      if (!this.neck) {
         throw new IllegalStateException(this.name + ": look() needs a neck()");
      } else {
         this.lookPitch = f;
         this.lookYaw = g;
         this.lookLimit = Math.max(0.0F, h);
         return this;
      }
   }

   public Part mirrorOf(Part part) {
      this.shape = part.shape == null ? null : part.shape.mirrored();
      this.pivotX = -part.pivotX;
      this.pivotY = part.pivotY;
      this.pivotZ = part.pivotZ;
      this.pivotSet = true;
      if (part.gripSet) {
         this.gripX = -part.gripX;
         this.gripY = part.gripY;
         this.gripZ = part.gripZ;
         this.gripSet = true;
      }

      this.outline = part.outline;
      this.outlineColor = part.outlineColor;
      this.outlineColorSet = part.outlineColorSet;
      this.decals.clear();

      for (Decal var3 : part.decals) {
         this.decals.add(var3.mirrored());
      }

      this.motion = part.motion == null ? null : part.motion.mirrored();
      this.detail = part.detail;
      this.resolution = part.resolution;
      this.flat = part.flat;
      this.occlusion = part.occlusion;
      this.swing = part.swing;
      this.swingLimit = part.swingLimit;
      this.jointX = -part.jointX;
      this.jointY = part.jointY;
      this.jointZ = part.jointZ;
      this.jointDirX = -part.jointDirX;
      this.jointDirY = part.jointDirY;
      this.jointDirZ = part.jointDirZ;
      this.jointBlend = part.jointBlend;
      this.jointSet = part.jointSet;
      this.neck = part.neck;
      this.lookPitch = part.lookPitch;
      this.lookYaw = part.lookYaw;
      this.lookLimit = part.lookLimit;
      return this;
   }

   float gripX() {
      return this.gripSet ? this.gripX : this.pivotX + this.bone.gripX;
   }

   float gripY() {
      return this.gripSet ? this.gripY : this.pivotY + this.bone.gripY;
   }

   float gripZ() {
      return this.gripSet ? this.gripZ : this.pivotZ + this.bone.gripZ;
   }
}
