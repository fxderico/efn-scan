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

   public Rig scale(float var1) {
      if (var1 > 0.05F && var1 < 8.0F) {
         this.scale = var1;
         return this;
      } else {
         throw new IllegalArgumentException("scale must be between 0.05 and 8");
      }
   }

   public Rig outline(int var1, float var2) {
      this.outlineColor = var1;
      this.outline = Math.max(0.0F, var2);
      return this;
   }

   public Rig occlusion(float var1) {
      this.occlusion = Math.max(0.0F, Math.min(1.0F, var1));
      return this;
   }

   public Rig flatLight(float var1) {
      this.flat = Math.max(0.0F, Math.min(1.0F, var1));
      return this;
   }

   public Rig firstPersonArms(boolean var1) {
      this.firstPersonArms = var1;
      return this;
   }

   public Part part(Bone var1) {
      Part var2 = this.bones[var1.ordinal()];
      if (var2 == null) {
         this.bones[var1.ordinal()] = var2 = new Part(var1.name(), var1, null, false);
      }

      return var2;
   }

   public Part extra(String var1, Bone var2) {
      return this.extra(var1, this.part(var2));
   }

   public Part extra(String var1, Part var2) {
      if (var1 != null && !var1.isEmpty()) {
         for (Part var4 : this.extras) {
            if (var4.name.equals(var1)) {
               throw new IllegalArgumentException("duplicate extra name: " + var1);
            }
         }

         for (Bone var6 : Bone.values()) {
            if (var6.name().equals(var1)) {
               throw new IllegalArgumentException("extra name clashes with a bone: " + var1);
            }
         }

         Part var8 = new Part(var1, var2.bone, var2, true);
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
