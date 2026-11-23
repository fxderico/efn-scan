package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum Bone {
   HEAD(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F),
   BODY(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F),
   RIGHT_ARM(-5.0F, 22.0F, 0.0F, -1.0F, -10.0F, 2.0F),
   LEFT_ARM(5.0F, 22.0F, 0.0F, 1.0F, -10.0F, 2.0F),
   RIGHT_LEG(-1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F),
   LEFT_LEG(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F);

   public final float pivotX;
   public final float pivotY;
   public final float pivotZ;
   final float gripX;
   final float gripY;
   final float gripZ;

   private Bone(float nullxx, float nullxxx, float nullxxxx, float nullxxxxx, float nullxxxxxx, float nullxxxxxxx) {
      this.pivotX = nullxx;
      this.pivotY = nullxxx;
      this.pivotZ = nullxxxx;
      this.gripX = nullxxxxx;
      this.gripY = nullxxxxxx;
      this.gripZ = nullxxxxxxx;
   }

   public boolean arm() {
      return this == RIGHT_ARM || this == LEFT_ARM;
   }

   public boolean leg() {
      return this == RIGHT_LEG || this == LEFT_LEG;
   }

   public Bone opposite() {
      return switch (this) {
         case RIGHT_ARM -> LEFT_ARM;
         case LEFT_ARM -> RIGHT_ARM;
         case RIGHT_LEG -> LEFT_LEG;
         case LEFT_LEG -> RIGHT_LEG;
         default -> this;
      };
   }
}
