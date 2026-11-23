package net.efn.client.module.impl.render.character.kit;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Rig {
   float scale = 1.0F;
   int outlineColor = -14017512;
   float outline = 0.2F;
   float occlusion = 0.55F;
   float flat;
   boolean firstPersonArms = true;
   final Part[] bones = new Part[Bone.values().length];
   final List<Part> extras = new ArrayList<>();

   public Rig() {
   }

   public Rig scale(float f) {
      if (f > 0.05F && f < 8.0F) {
         this.scale = f;
         return this;
      } else {
         throw new IllegalArgumentException("scale must be between 0.05 and 8");
      }
   }

   public Rig outline(int i, float f) {
      this.outlineColor = i;
      this.outline = Math.max(0.0F, f);
      return this;
   }

   public Rig occlusion(float f) {
      this.occlusion = Math.max(0.0F, Math.min(1.0F, f));
      return this;
   }

   public Rig flatLight(float f) {
      this.flat = Math.max(0.0F, Math.min(1.0F, f));
      return this;
   }

   public Rig firstPersonArms(boolean bl) {
      this.firstPersonArms = bl;
      return this;
   }

   public Part part(Bone bone) {
      Part var2 = this.bones[bone.ordinal()];
      if (var2 == null) {
         this.bones[bone.ordinal()] = var2 = new Part(bone.name(), bone, null, false);
      }

      return var2;
   }

   public Part extra(String string, Bone bone) {
      return this.extra(string, this.part(bone));
   }

   public Part extra(String string, Part part) {
      if (string != null && !string.isEmpty()) {
         for (Part var4 : this.extras) {
            if (var4.name.equals(string)) {
               throw new IllegalArgumentException("duplicate extra name: " + string);
            }
         }

         for (Bone var6 : Bone.values()) {
            if (var6.name().equals(string)) {
               throw new IllegalArgumentException("extra name clashes with a bone: " + string);
            }
         }

         Part var8 = new Part(string, part.bone, part, true);
         this.extras.add(var8);
         return var8;
      } else {
         throw new IllegalArgumentException("extra needs a name");
      }
   }

   public float scale() {
      return this.scale;
   }
}
