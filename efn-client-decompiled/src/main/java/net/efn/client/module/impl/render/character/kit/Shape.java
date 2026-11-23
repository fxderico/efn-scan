package net.efn.client.module.impl.render.character.kit;

import java.util.function.UnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class Shape {
   static final float FAR = 100000.0F;
   float minX = -100000.0F;
   float minY = -100000.0F;
   float minZ = -100000.0F;
   float maxX = 100000.0F;
   float maxY = 100000.0F;
   float maxZ = 100000.0F;
   Paint paint;

   protected Shape() {
   }

   public abstract float distance(float var1, float var2, float var3);

   float distance(float var1, float var2, float var3, float var4) {
      return this.distance(var1, var2, var3);
   }

   float shade(float var1, float var2, float var3, Sample var4) {
      float var5 = this.distance(var1, var2, var3);
      Paint.DEFAULT.apply(var1, var2, var3, var4);
      return var5;
   }

   public final float sample(float var1, float var2, float var3, Sample var4) {
      if (this.paint != null) {
         float var5 = this.distance(var1, var2, var3);
         this.paint.apply(var1, var2, var3, var4);
         return var5;
      } else {
         return this.shade(var1, var2, var3, var4);
      }
   }

   protected final void bounds(float var1, float var2, float var3, float var4, float var5, float var6) {
      this.minX = Math.min(var1, var4);
      this.minY = Math.min(var2, var5);
      this.minZ = Math.min(var3, var6);
      this.maxX = Math.max(var1, var4);
      this.maxY = Math.max(var2, var5);
      this.maxZ = Math.max(var3, var6);
   }

   final void boundsOf(Shape var1, float var2) {
      if (!var1.bounded()) {
         this.minZ = -100000.0F;
         this.minY = -100000.0F;
         this.minX = -100000.0F;
         this.maxZ = 100000.0F;
         this.maxY = 100000.0F;
         this.maxX = 100000.0F;
      } else {
         this.bounds(var1.minX - var2, var1.minY - var2, var1.minZ - var2, var1.maxX + var2, var1.maxY + var2, var1.maxZ + var2);
      }
   }

   public final boolean bounded() {
      return this.maxX - this.minX < 100000.0F && this.maxY - this.minY < 100000.0F && this.maxZ - this.minZ < 100000.0F;
   }

   public final float minX() {
      return this.minX;
   }

   public final float minY() {
      return this.minY;
   }

   public final float minZ() {
      return this.minZ;
   }

   public final float maxX() {
      return this.maxX;
   }

   public final float maxY() {
      return this.maxY;
   }

   public final float maxZ() {
      return this.maxZ;
   }

   Shape prune(float var1, float var2, float var3, float var4, float var5, float var6) {
      return this;
   }

   Shape map(UnaryOperator<Shape> var1) {
      return var1.apply(this);
   }

   final Shape inherit(Shape var1) {
      if (var1.paint != null && this.paint == null) {
         this.paint = var1.paint;
      }

      return this;
   }

   final float boxGap(float var1, float var2, float var3, float var4, float var5, float var6) {
      float var7 = Math.max(0.0F, Math.max(this.minX - var4, var1 - this.maxX));
      float var8 = Math.max(0.0F, Math.max(this.minY - var5, var2 - this.maxY));
      float var9 = Math.max(0.0F, Math.max(this.minZ - var6, var3 - this.maxZ));
      return (float)Math.sqrt(var7 * var7 + var8 * var8 + var9 * var9);
   }

   public Shape color(int var1) {
      this.paint = Paint.of(var1);
      return this;
   }

   public Shape paint(Paint var1) {
      this.paint = var1;
      return this;
   }

   public Shape move(float var1, float var2, float var3) {
      return new Shapes.Translate(this, var1, var2, var3);
   }

   public Shape rotate(float var1, float var2, float var3) {
      return this.rotate(var1, var2, var3, 0.0F, 0.0F, 0.0F);
   }

   public Shape rotate(float var1, float var2, float var3, float var4, float var5, float var6) {
      return new Shapes.Rotate(this, Mat3.euler(var1, var2, var3), var4, var5, var6);
   }

   public Shape rotateAxis(float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      return new Shapes.Rotate(this, Mat3.about(var1, var2, var3, var4), var5, var6, var7);
   }

   public Shape scale(float var1) {
      return this.scale(var1, 0.0F, 0.0F, 0.0F);
   }

   public Shape scale(float var1, float var2, float var3, float var4) {
      return new Shapes.Scale(this, var1, var2, var3, var4);
   }

   public Shape mirrorX() {
      return new Shapes.Mirror(this, true);
   }

   public Shape mirrored() {
      return new Shapes.Mirror(this, false);
   }

   public Shape inflate(float var1) {
      return new Shapes.Round(this, var1);
   }

   public Shape shell(float var1) {
      return new Shapes.Shell(this, var1);
   }

   public Shape roughen(float var1, float var2) {
      return new Shapes.Roughen(this, var1, var2);
   }

   public Shape union(Shape... var1) {
      return Shapes.union(this.prepend(var1));
   }

   public Shape blend(float var1, Shape... var2) {
      return Shapes.blend(var1, this.prepend(var2));
   }

   public Shape subtract(Shape var1) {
      return Shapes.subtract(this, var1);
   }

   public Shape subtract(float var1, Shape var2) {
      return Shapes.subtract(var1, this, var2);
   }

   public Shape intersect(Shape var1) {
      return Shapes.intersect(this, var1);
   }

   public Shape intersect(float var1, Shape var2) {
      return Shapes.intersect(var1, this, var2);
   }

   private Shape[] prepend(Shape[] var1) {
      Shape[] var2 = new Shape[var1.length + 1];
      var2[0] = this;
      System.arraycopy(var1, 0, var2, 1, var1.length);
      return var2;
   }
}
