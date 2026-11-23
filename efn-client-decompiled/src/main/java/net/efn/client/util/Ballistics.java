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

      private Body(double d, double e, double f, boolean bl, double g) {
         this.gravity = d;
         this.airKeep = e;
         this.waterKeep = f;
         this.movesFirst = bl;
         this.size = g;
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
      private Direction face = Direction.UP;
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

      public Ballistics.Flight launch(Ballistics.Body body, Vec3d vec3d, Vec3d vec3d2) {
         this.body = body;
         this.px = vec3d.x;
         this.py = vec3d.y;
         this.pz = vec3d.z;
         this.vx = vec3d2.x;
         this.vy = vec3d2.y;
         this.vz = vec3d2.z;
         this.gravity = body.gravity;
         this.width = body.size;
         this.height = body.size;
         this.wet = false;
         this.age = 0;
         this.lifetime = 240;
         this.shooter = null;
         this.self = null;
         this.leftShooter = false;
         return this;
      }

      public Ballistics.Flight gravity(double d) {
         this.gravity = d;
         return this;
      }

      public Ballistics.Flight bounds(double d, double e) {
         this.width = d;
         this.height = e;
         return this;
      }

      public Ballistics.Flight wet(boolean bl) {
         this.wet = bl;
         return this;
      }

      public Ballistics.Flight age(int i) {
         this.age = i;
         return this;
      }

      public Ballistics.Flight lifetime(int i) {
         this.lifetime = Math.max(1, i);
         return this;
      }

      public Ballistics.Flight ignoring(Entity entity, Entity entity2) {
         this.shooter = entity;
         this.self = entity2;
         return this;
      }

      public Ballistics.Flight leftShooter(boolean bl) {
         this.leftShooter = bl;
         return this;
      }

      public Ballistics.Flight solve(World world) {
         this.size = 0;
         this.struck = null;
         this.outcome = Ballistics.Outcome.OPEN;
         this.face = Direction.UP;
         this.trace(world);
         this.strike(world);
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

      public double x(int i) {
         return this.xs[i];
      }

      public double y(int i) {
         return this.ys[i];
      }

      public double z(int i) {
         return this.zs[i];
      }

      public Vec3d point(int i) {
         return new Vec3d(this.xs[i], this.ys[i], this.zs[i]);
      }

      public Vec3d impact() {
         return this.point(this.size - 1);
      }

      public Vec3d heading() {
         if (this.size < 2) {
            return Vec3d.ZERO;
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

      private void trace(World world) {
         ShapeContext var2 = this.self != null ? ShapeContext.of(this.self) : ShapeContext.absent();
         double var3 = world.getBottomY() - 64.0;
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
            if ((var25 = world.getCollisionsIncludingWorldBorder(
                     new RaycastContext(
                        new Vec3d(var5, var7, var9),
                        new Vec3d(var23 = var5 + var11, var21 = var7 + var13, var19 = var9 + var15),
                        ShapeType.COLLIDER,
                        FluidHandling.NONE,
                        var2
                     )
                  ))
                  .getType()
               != Type.MISS) {
               Vec3d var30 = var25.getPos();
               this.push(var30.x, var30.y, var30.z);
               this.face = var25.getSide();
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

            var17 = this.submerged(world, var23, var21, var19);
            if (var21 < var3) {
               break;
            }
         }

         this.ticks = var18;
         if (this.body == Ballistics.Body.ROCKET && var18 >= this.lifetime) {
            this.outcome = Ballistics.Outcome.BURST;
         }
      }

      private void strike(World world) {
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

            Box var40 = new Box(var3, var5, var7, var9, var11, var13).expand(1.3);
            List var16 = world.getOtherEntities(this.self, var40, this::canStrike);
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
                  double var30 = MathHelper.clamp((this.age + var17 - 2) / 20.0, 0.0, 0.3);
                  double var32 = 1.7976931348623157E308;
                  Entity var34 = null;

                  for (int var35 = 0; var35 < var16.size(); var35++) {
                     Entity var39 = (Entity)var16.get(var35);
                     double var37;
                     if ((var2 || var39.getRootVehicle() != this.shooterRoot)
                        && (var37 = this.entry(var18, var20, var22, var24, var26, var28, var39.getBoundingBox(), var30)) < var32) {
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

      private boolean canStrike(Entity entity) {
         return !entity.isSpectator() && entity.canBeHitByProjectile();
      }

      private boolean departs() {
         Entity var1 = this.shooterRoot = this.shooter != null ? this.shooter.getRootVehicle() : null;
         if (this.shooterRoot == null) {
            return true;
         } else {
            this.shooterBoxes.clear();
            if (EntityPredicates.CAN_HIT.test(this.shooterRoot)) {
               this.shooterBoxes.add(this.shooterRoot.getBoundingBox());
            }

            for (Entity var3 : this.shooterRoot.getPassengersDeep()) {
               if (EntityPredicates.CAN_HIT.test(var3)) {
                  this.shooterBoxes.add(var3.getBoundingBox());
               }
            }

            return this.outside(0);
         }
      }

      private boolean outside(int i) {
         double var2 = this.xs[i];
         double var4 = this.ys[i];
         double var6 = this.zs[i];
         double var8 = this.xs[i + 1] - var2;
         double var10 = this.ys[i + 1] - var4;
         double var12 = this.zs[i + 1] - var6;
         double var14 = this.width * 0.5;
         double var16 = var2 - var14 + Math.min(var8, 0.0) - 1.0;
         double var18 = var2 + var14 + Math.max(var8, 0.0) + 1.0;
         double var20 = var4 + Math.min(var10, 0.0) - 1.0;
         double var22 = var4 + this.height + Math.max(var10, 0.0) + 1.0;
         double var24 = var6 - var14 + Math.min(var12, 0.0) - 1.0;
         double var26 = var6 + var14 + Math.max(var12, 0.0) + 1.0;

         for (int var28 = 0; var28 < this.shooterBoxes.size(); var28++) {
            if (this.shooterBoxes.get(var28).intersects(var16, var20, var24, var18, var22, var26)) {
               return false;
            }
         }

         return true;
      }

      private double entry(double d, double e, double f, double g, double h, double i, Box box, double j) {
         this.near = 0.0;
         this.far = 1.0;
         if (!this.slab(d, g, box.minX - j, box.maxX + j)) {
            return 1.7976931348623157E308;
         } else if (!this.slab(e, h, box.minY - j, box.maxY + j)) {
            return 1.7976931348623157E308;
         } else if (!this.slab(f, i, box.minZ - j, box.maxZ + j)) {
            return 1.7976931348623157E308;
         } else {
            return this.near > 0.0 ? this.near : 1.7976931348623157E308;
         }
      }

      private boolean slab(double d, double e, double f, double g) {
         if (!(Math.abs(e) < 1.0E-12)) {
            double var9 = (f - d) / e;
            double var11 = (g - d) / e;
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
            return d >= f && d <= g;
         }
      }

      private boolean submerged(World world, double d, double e, double f) {
         double var8 = this.width * 0.5 - 0.001;
         double var10 = e + 0.001;
         int var12 = MathHelper.floor(d - var8);
         int var13 = MathHelper.floor(d + var8);
         int var14 = MathHelper.floor(var10);
         int var15 = MathHelper.floor(e + this.height - 0.001);
         int var16 = MathHelper.floor(f - var8);
         int var17 = MathHelper.floor(f + var8);

         for (int var18 = var12; var18 <= var13; var18++) {
            for (int var19 = var14; var19 <= var15; var19++) {
               for (int var20 = var16; var20 <= var17; var20++) {
                  FluidState var21 = world.getFluidState(this.cursor.set(var18, var19, var20));
                  if (var21.isIn(FluidTags.WATER) && var19 + var21.getHeight(world, this.cursor) >= var10) {
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

      private void push(double d, double e, double f) {
         if (this.size == this.xs.length) {
            int var7 = this.size * 2;
            this.xs = Arrays.copyOf(this.xs, var7);
            this.ys = Arrays.copyOf(this.ys, var7);
            this.zs = Arrays.copyOf(this.zs, var7);
         }

         this.xs[this.size] = d;
         this.ys[this.size] = e;
         this.zs[this.size] = f;
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
