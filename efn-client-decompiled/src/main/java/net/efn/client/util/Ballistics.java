package net.efn.client.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class Ballistics {
   public static final int DEFAULT_LIFETIME = 240;
   private static final double VOID_DEPTH = 64.0;
   private static final double FULL_MARGIN = 0.3;
   private static final double SEARCH_PAD = 1.3;
   private static final double FLUID_SKIN = 0.001;
   private static final double SHOOTER_CLEARANCE = 1.0;

   private Ballistics() {
   }

   @Environment(EnvType.CLIENT)
   public static enum Body {
      ARROW(0.05, 0.99, 0.6, true, 0.5),
      TRIDENT(0.05, 0.99, 0.99, true, 0.5),
      ORB(0.03, 0.99, 0.8, false, 0.25),
      FLASK(0.05, 0.99, 0.8, false, 0.25),
      BOTTLE(0.07, 0.99, 0.8, false, 0.25),
      ROCKET(0.0, 1.0, 1.0, true, 0.25);

      private final double gravity;
      private final double airKeep;
      private final double waterKeep;
      private final boolean movesFirst;
      private final double size;

      private Body(double nullxx, double nullxxx, double nullxxxx, boolean nullxxxxx, double nullxxxxxx) {
         this.gravity = nullxx;
         this.airKeep = nullxxx;
         this.waterKeep = nullxxxx;
         this.movesFirst = nullxxxxx;
         this.size = nullxxxxxx;
      }

      public double gravity() {
         return this.gravity;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Flight {
      private double[] xs = new double[96];
      private double[] ys = new double[96];
      private double[] zs = new double[96];
      private int size;
      private int ticks;
      private double length;
      private Ballistics.Outcome outcome = Ballistics.Outcome.OPEN;
      private Direction face = Direction.field_11036;
      private Entity struck;
      private Ballistics.Body body = Ballistics.Body.ARROW;
      private double px;
      private double py;
      private double pz;
      private double vx;
      private double vy;
      private double vz;
      private double gravity;
      private double width;
      private double height;
      private boolean wet;
      private int age;
      private int lifetime = 240;
      private Entity shooter;
      private Entity self;
      private boolean leftShooter;
      private Entity shooterRoot;
      private final List<Box> shooterBoxes = new ArrayList<>();
      private final Mutable cursor = new Mutable();
      private double near;
      private double far;

      public Flight() {
      }

      public Ballistics.Flight launch(Ballistics.Body var1, Vec3d var2, Vec3d var3) {
         this.body = var1;
         this.px = var2.field_1352;
         this.py = var2.field_1351;
         this.pz = var2.field_1350;
         this.vx = var3.field_1352;
         this.vy = var3.field_1351;
         this.vz = var3.field_1350;
         this.gravity = var1.gravity;
         this.width = var1.size;
         this.height = var1.size;
         this.wet = false;
         this.age = 0;
         this.lifetime = 240;
         this.shooter = null;
         this.self = null;
         this.leftShooter = false;
         return this;
      }

      public Ballistics.Flight gravity(double var1) {
         this.gravity = var1;
         return this;
      }

      public Ballistics.Flight bounds(double var1, double var3) {
         this.width = var1;
         this.height = var3;
         return this;
      }

      public Ballistics.Flight wet(boolean var1) {
         this.wet = var1;
         return this;
      }

      public Ballistics.Flight age(int var1) {
         this.age = var1;
         return this;
      }

      public Ballistics.Flight lifetime(int var1) {
         this.lifetime = Math.max(1, var1);
         return this;
      }

      public Ballistics.Flight ignoring(Entity var1, Entity var2) {
         this.shooter = var1;
         this.self = var2;
         return this;
      }

      public Ballistics.Flight leftShooter(boolean var1) {
         this.leftShooter = var1;
         return this;
      }

      public Ballistics.Flight solve(World var1) {
         this.size = 0;
         this.struck = null;
         this.outcome = Ballistics.Outcome.OPEN;
         this.face = Direction.field_11036;
         this.trace(var1);
         this.strike(var1);
         this.measure();
         this.release();
         return this;
      }

      public void clear() {
         this.size = 0;
         this.length = 0.0;
         this.struck = null;
         this.leftShooter = false;
         this.outcome = Ballistics.Outcome.OPEN;
         this.release();
      }

      private void release() {
         this.shooter = null;
         this.self = null;
         this.shooterRoot = null;
         this.shooterBoxes.clear();
      }

      public int size() {
         return this.size;
      }

      public double x(int var1) {
         return this.xs[var1];
      }

      public double y(int var1) {
         return this.ys[var1];
      }

      public double z(int var1) {
         return this.zs[var1];
      }

      public Vec3d point(int var1) {
         return new Vec3d(this.xs[var1], this.ys[var1], this.zs[var1]);
      }

      public Vec3d impact() {
         return this.point(this.size - 1);
      }

      public Vec3d heading() {
         if (this.size < 2) {
            return Vec3d.field_1353;
         } else {
            int var1 = this.size - 1;
            return new Vec3d(this.xs[var1] - this.xs[var1 - 1], this.ys[var1] - this.ys[var1 - 1], this.zs[var1] - this.zs[var1 - 1]);
         }
      }

      public int ticks() {
         return this.ticks;
      }

      public double length() {
         return this.length;
      }

      public Ballistics.Outcome outcome() {
         return this.outcome;
      }

      public Direction face() {
         return this.face;
      }

      public Entity struck() {
         return this.struck;
      }

      public boolean hasLeftShooter() {
         return this.leftShooter;
      }

      private void trace(World var1) {
         ShapeContext var2 = this.self != null ? ShapeContext.method_16195(this.self) : ShapeContext.method_16194();
         double var3 = var1.method_31607() - 64.0;
         double var5 = this.px;
         double var7 = this.py;
         double var9 = this.pz;
         double var11 = this.vx;
         double var13 = this.vy;
         double var15 = this.vz;
         boolean var17 = this.wet;
         this.push(var5, var7, var9);
         int var18 = 0;

         while (var18 < this.lifetime) {
            var18++;
            if (!this.body.movesFirst) {
               var13 -= this.gravity;
               double var26 = var17 ? this.body.waterKeep : this.body.airKeep;
               var11 *= var26;
               var13 *= var26;
               var15 *= var26;
            }

            double var19;
            double var21;
            double var23;
            BlockHitResult var25;
            if ((var25 = var1.method_61717(
                     new RaycastContext(
                        new Vec3d(var5, var7, var9),
                        new Vec3d(var23 = var5 + var11, var21 = var7 + var13, var19 = var9 + var15),
                        ShapeType.field_17558,
                        FluidHandling.field_1348,
                        var2
                     )
                  ))
                  .method_17783()
               != Type.field_1333) {
               Vec3d var30 = var25.method_17784();
               this.push(var30.field_1352, var30.field_1351, var30.field_1350);
               this.face = var25.method_17780();
               this.outcome = Ballistics.Outcome.BLOCK;
               this.ticks = var18;
               return;
            }

            var5 = var23;
            var7 = var21;
            var9 = var19;
            this.push(var23, var21, var19);
            if (this.body.movesFirst) {
               double var29 = var17 ? this.body.waterKeep : this.body.airKeep;
               var11 *= var29;
               var13 = var13 * var29 - this.gravity;
               var15 *= var29;
            }

            var17 = this.submerged(var1, var23, var21, var19);
            if (var21 < var3) {
               break;
            }
         }

         this.ticks = var18;
         if (this.body == Ballistics.Body.ROCKET && var18 >= this.lifetime) {
            this.outcome = Ballistics.Outcome.BURST;
         }
      }

      private void strike(World var1) {
         if (this.size >= 2) {
            boolean var2;
            this.leftShooter = var2 = this.leftShooter || this.departs();
            double var3 = this.xs[0];
            double var5 = this.ys[0];
            double var7 = this.zs[0];
            double var9 = var3;
            double var11 = var5;
            double var13 = var7;

            for (int var15 = 1; var15 < this.size; var15++) {
               var3 = Math.min(var3, this.xs[var15]);
               var5 = Math.min(var5, this.ys[var15]);
               var7 = Math.min(var7, this.zs[var15]);
               var9 = Math.max(var9, this.xs[var15]);
               var11 = Math.max(var11, this.ys[var15]);
               var13 = Math.max(var13, this.zs[var15]);
            }

            Box var40 = new Box(var3, var5, var7, var9, var11, var13).method_1014(1.3);
            List var16 = var1.method_8333(this.self, var40, this::canStrike);
            if (!var16.isEmpty()) {
               for (int var17 = 1; var17 < this.size; var17++) {
                  if (!var2 && var17 > 1) {
                     var2 = this.outside(var17 - 1);
                  }

                  double var18 = this.xs[var17 - 1];
                  double var20 = this.ys[var17 - 1];
                  double var22 = this.zs[var17 - 1];
                  double var24 = this.xs[var17] - var18;
                  double var26 = this.ys[var17] - var20;
                  double var28 = this.zs[var17] - var22;
                  double var30 = MathHelper.method_15350((this.age + var17 - 2) / 20.0, 0.0, 0.3);
                  double var32 = 1.7976931348623157E308;
                  Entity var34 = null;

                  for (int var35 = 0; var35 < var16.size(); var35++) {
                     Entity var39 = (Entity)var16.get(var35);
                     double var37;
                     if ((var2 || var39.method_5668() != this.shooterRoot)
                        && (var37 = this.entry(var18, var20, var22, var24, var26, var28, var39.method_5829(), var30)) < var32) {
                        var32 = var37;
                        var34 = var39;
                     }
                  }

                  if (var34 != null) {
                     this.xs[var17] = var18 + var24 * var32;
                     this.ys[var17] = var20 + var26 * var32;
                     this.zs[var17] = var22 + var28 * var32;
                     this.size = var17 + 1;
                     this.ticks = var17;
                     this.struck = var34;
                     this.outcome = Ballistics.Outcome.ENTITY;
                     return;
                  }
               }
            }
         }
      }

      private boolean canStrike(Entity var1) {
         return !var1.method_7325() && var1.method_49108();
      }

      private boolean departs() {
         Entity var1 = this.shooterRoot = this.shooter != null ? this.shooter.method_5668() : null;
         if (this.shooterRoot == null) {
            return true;
         } else {
            this.shooterBoxes.clear();
            if (EntityPredicates.field_52443.test(this.shooterRoot)) {
               this.shooterBoxes.add(this.shooterRoot.method_5829());
            }

            for (Entity var3 : this.shooterRoot.method_5736()) {
               if (EntityPredicates.field_52443.test(var3)) {
                  this.shooterBoxes.add(var3.method_5829());
               }
            }

            return this.outside(0);
         }
      }

      private boolean outside(int var1) {
         double var2 = this.xs[var1];
         double var4 = this.ys[var1];
         double var6 = this.zs[var1];
         double var8 = this.xs[var1 + 1] - var2;
         double var10 = this.ys[var1 + 1] - var4;
         double var12 = this.zs[var1 + 1] - var6;
         double var14 = this.width * 0.5;
         double var16 = var2 - var14 + Math.min(var8, 0.0) - 1.0;
         double var18 = var2 + var14 + Math.max(var8, 0.0) + 1.0;
         double var20 = var4 + Math.min(var10, 0.0) - 1.0;
         double var22 = var4 + this.height + Math.max(var10, 0.0) + 1.0;
         double var24 = var6 - var14 + Math.min(var12, 0.0) - 1.0;
         double var26 = var6 + var14 + Math.max(var12, 0.0) + 1.0;

         for (int var28 = 0; var28 < this.shooterBoxes.size(); var28++) {
            if (this.shooterBoxes.get(var28).method_1003(var16, var20, var24, var18, var22, var26)) {
               return false;
            }
         }

         return true;
      }

      private double entry(double var1, double var3, double var5, double var7, double var9, double var11, Box var13, double var14) {
         this.near = 0.0;
         this.far = 1.0;
         if (!this.slab(var1, var7, var13.field_1323 - var14, var13.field_1320 + var14)) {
            return 1.7976931348623157E308;
         } else if (!this.slab(var3, var9, var13.field_1322 - var14, var13.field_1325 + var14)) {
            return 1.7976931348623157E308;
         } else if (!this.slab(var5, var11, var13.field_1321 - var14, var13.field_1324 + var14)) {
            return 1.7976931348623157E308;
         } else {
            return this.near > 0.0 ? this.near : 1.7976931348623157E308;
         }
      }

      private boolean slab(double var1, double var3, double var5, double var7) {
         if (!(Math.abs(var3) < 1.0E-12)) {
            double var9 = (var5 - var1) / var3;
            double var11 = (var7 - var1) / var3;
            if (var9 > var11) {
               double var13 = var9;
               var9 = var11;
               var11 = var13;
            }

            if (var9 > this.near) {
               this.near = var9;
            }

            if (var11 < this.far) {
               this.far = var11;
            }

            return this.near <= this.far;
         } else {
            return var1 >= var5 && var1 <= var7;
         }
      }

      private boolean submerged(World var1, double var2, double var4, double var6) {
         double var8 = this.width * 0.5 - 0.001;
         double var10 = var4 + 0.001;
         int var12 = MathHelper.method_15357(var2 - var8);
         int var13 = MathHelper.method_15357(var2 + var8);
         int var14 = MathHelper.method_15357(var10);
         int var15 = MathHelper.method_15357(var4 + this.height - 0.001);
         int var16 = MathHelper.method_15357(var6 - var8);
         int var17 = MathHelper.method_15357(var6 + var8);

         for (int var18 = var12; var18 <= var13; var18++) {
            for (int var19 = var14; var19 <= var15; var19++) {
               for (int var20 = var16; var20 <= var17; var20++) {
                  FluidState var21 = var1.method_8316((BlockPos)this.cursor.method_10103(var18, var19, var20));
                  if (var21.method_15767(FluidTags.field_15517) && var19 + var21.method_15763(var1, (BlockPos)this.cursor) >= var10) {
                     return true;
                  }
               }
            }
         }

         return false;
      }

      private void measure() {
         double var1 = 0.0;

         for (int var3 = 1; var3 < this.size; var3++) {
            double var4 = this.xs[var3] - this.xs[var3 - 1];
            double var6 = this.ys[var3] - this.ys[var3 - 1];
            double var8 = this.zs[var3] - this.zs[var3 - 1];
            var1 += Math.sqrt(var4 * var4 + var6 * var6 + var8 * var8);
         }

         this.length = var1;
      }

      private void push(double var1, double var3, double var5) {
         if (this.size == this.xs.length) {
            int var7 = this.size * 2;
            this.xs = Arrays.copyOf(this.xs, var7);
            this.ys = Arrays.copyOf(this.ys, var7);
            this.zs = Arrays.copyOf(this.zs, var7);
         }

         this.xs[this.size] = var1;
         this.ys[this.size] = var3;
         this.zs[this.size] = var5;
         this.size++;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Outcome {
      OPEN,
      BLOCK,
      ENTITY,
      BURST;

      private Outcome() {
      }
   }
}
