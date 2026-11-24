package net.efn.client.module.impl.combat.bot;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
final class AirTrack {
   private static final double GRAVITY = 0.08;
   private static final double VERTICAL_DRAG = 0.98;
   private static final double AIR_DRAG = 0.91;
   private static final double HIT_KNOCK = 0.4;
   private static final double SPRINT_KNOCK = 0.5;
   private static final double MAX_LIFT = 0.4;
   private static final double KEY_PUSH = 0.026;
   private static final double MAX_PUSH = 0.045;
   private static final double MAX_LEARNED = 0.1;
   static final int LAND_TICKS = 40;
   private static final int KICK_WAIT = 8;
   private static final double BLAST_NEAR = 10.0;
   private PlayerEntity target;
   private RivalBook.Rival rival;
   private final List<AirTrack.Sample> flight = new ArrayList<>();
   private Vec3d lastSeen;
   private boolean airborne;
   private Vec3d pushSum = Vec3d.ZERO;
   private int pushes;
   private Vec3d early;
   private Vec3d kickFrom;
   private Vec3d kick;
   private Vec3d kickWay;
   private int kickAt = -1;
   private boolean kickSeen;
   private int kickSeenAt;
   private boolean kickSpoiled;
   AirTrack.Landing bare;
   private double gain;
   AirTrack.Landing guessed;

   AirTrack() {
   }

   void forget() {
      this.target = null;
      this.rival = null;
      this.flight.clear();
      this.lastSeen = null;
      this.airborne = false;
      this.pushSum = Vec3d.ZERO;
      this.pushes = 0;
      this.kickAt = -1;
      this.guessed = null;
      this.bare = null;
   }

   void note(PlayerEntity playerEntity, int i, RivalBook.Rival rival) {
      if (playerEntity != this.target) {
         this.forget();
         this.target = playerEntity;
      }

      this.rival = rival;
      Vec3d var5 = server(playerEntity);
      boolean var6 = !var5.equals(this.lastSeen);
      this.lastSeen = var5;
      if (this.kickAt >= 0 && !this.kickSeen) {
         if (var5.y > this.kickFrom.y + 0.05 || var5.subtract(this.kickFrom).horizontalLengthSquared() > 0.01) {
            this.kickSeen = true;
            this.kickSeenAt = i;
         } else if (i - this.kickAt > 8) {
            this.kickAt = -1;
         }
      }

      if (this.kickAt >= 0 && i - this.kickAt > 40) {
         this.kickAt = -1;
      }

      boolean var4 = !playerEntity.isOnGround() && !floating(playerEntity);
      if (var4) {
         if (!this.airborne) {
            this.airborne = true;
            this.restart();
         }

         if (var6) {
            this.flight.add(new AirTrack.Sample(i, var5));
            if (this.flight.size() == 2) {
               this.readEarly();
            }

            this.read();
         }
      } else if (this.airborne) {
         this.airborne = false;
         if (this.kickAt >= 0 && this.kickSeen) {
            this.learn(var5);
         }

         this.kickAt = -1;
         this.restart();
      }
   }

   void kicked(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, float f, boolean bl, int i) {
      if (playerEntity == this.target) {
         this.kickAt = -1;
         double var6 = 1.0 - resistance(playerEntity);
         if (!(var6 <= 0.0)) {
            Vec3d var8 = server(playerEntity);
            Vec3d var9 = var8.subtract(clientPlayerEntity.getEntityPos()).multiply(1.0, 0.0, 1.0);
            if (!(var9.lengthSquared() < 1.0E-6)) {
               var9 = var9.normalize();
               Vec3d var10 = var9.multiply(0.4 * var6);
               double var11 = Math.min(0.4, 0.4 * var6);
               if (bl) {
                  double var13 = Math.toRadians(f);
                  Vec3d var15 = new Vec3d(-Math.sin(var13), 0.0, Math.cos(var13));
                  var10 = var10.multiply(0.5).add(var15.multiply(0.5 * var6));
                  var11 = Math.min(0.4, var11 / 2.0 + 0.5 * var6);
               }

               this.kick = new Vec3d(var10.x, var11, var10.z);
               this.kickWay = var10.lengthSquared() < 1.0E-6 ? var9 : var10.normalize();
               this.kickFrom = var8;
               this.kickAt = i;
               this.kickSpoiled = false;
               this.kickSeen = false;
               this.early = null;
               World var18 = playerEntity.getEntityWorld();
               AirTrack.Stop var14 = fly(var18, playerEntity, var8, this.kick, Vec3d.ZERO, true, 40);
               AirTrack.Stop var19 = fly(var18, playerEntity, var8, this.kick, this.kickWay.multiply(0.01), true, 40);
               this.bare = var14.down() ? new AirTrack.Landing(var14.at(), var14.ticks()) : null;
               this.gain = var14.down() && var19.down() ? var19.at().subtract(var14.at()).dotProduct(this.kickWay) / 0.01 : 0.0;
               AirTrack.Stop var16 = fly(var18, playerEntity, var8, this.kick, this.learned(0.1), true, 40);
               this.guessed = var16.down() ? new AirTrack.Landing(var16.at(), var16.ticks()) : null;
            }
         }
      }
   }

   void blast(Vec3d vec3d) {
      if (this.target != null && !(server(this.target).distanceTo(vec3d) > 10.0)) {
         this.kickSpoiled = true;
         if (!this.kickSeen) {
            this.kickAt = -1;
         }

         this.restart();
      }
   }

   AirTrack.Landing landing(int i) {
      AirTrack.Stop var2 = this.path(40, i);
      return var2 != null && var2.down() ? new AirTrack.Landing(var2.at(), var2.ticks()) : null;
   }

   Vec3d lead(int i, int j) {
      if (this.target == null || i <= 0) {
         return null;
      } else if (this.kickAt < 0 && this.pushes < 2) {
         return null;
      } else {
         AirTrack.Stop var3 = this.path(i, j);
         return var3 == null ? null : var3.at().subtract(this.target.getEntityPos());
      }
   }

   private AirTrack.Stop path(int i, int j) {
      if (this.target != null && !floating(this.target)) {
         World var4 = this.target.getEntityWorld();
         Vec3d var3 = this.airborne ? this.velocity() : null;
         if (this.kickAt < 0 || this.kickSeen && var3 != null) {
            if (var3 == null) {
               return null;
            } else {
               AirTrack.Sample var9 = this.flight.get(this.flight.size() - 1);
               int var10 = Math.min(3, j - var9.tick());
               AirTrack.Stop var8 = fly(var4, this.target, var9.at(), var3, this.push(var3), false, i + var10);
               return new AirTrack.Stop(var8.at(), Math.max(0, var8.ticks() - var10), var8.down());
            }
         } else {
            int var6 = this.kickSeen ? j - this.kickSeenAt + 1 : 0;
            AirTrack.Stop var7 = fly(var4, this.target, this.kickFrom, this.kick, this.learned(0.1), true, i + var6);
            return new AirTrack.Stop(var7.at(), Math.max(0, var7.ticks() - var6), var7.down());
         }
      } else {
         return null;
      }
   }

   private Vec3d push(Vec3d vec3d) {
      if (this.pushes >= 2) {
         return limit(this.pushSum.multiply(1.0 / this.pushes), 0.045);
      } else {
         boolean var4 = this.kickAt >= 0 && this.rival != null && this.rival.knowsFlight();
         Vec3d var3 = this.kickAt >= 0 ? this.learned(0.026) : limit(new Vec3d(vec3d.x, 0.0, vec3d.z).multiply(0.08999999999999997), 0.026);
         Vec3d var2 = this.pushes == 1 ? (this.early != null ? this.pushSum.add(this.early).multiply(0.5) : this.pushSum) : this.early;
         return var2 == null ? var3 : limit(var4 ? var2.add(var3).multiply(0.5) : var2, 0.045);
      }
   }

   private Vec3d learned(double d) {
      return this.rival != null && this.rival.knowsFlight() && this.kickWay != null
         ? limit(this.kickWay.multiply(this.rival.airAlong).add(right(this.kickWay).multiply(this.rival.airAcross)), d)
         : Vec3d.ZERO;
   }

   private void learn(Vec3d vec3d) {
      if (this.rival != null && !this.kickSpoiled && this.bare != null && !(this.gain < 1.0)) {
         if (!(Math.abs(vec3d.y - this.bare.at().y) > 0.6)) {
            Vec3d var2 = vec3d.subtract(this.bare.at());
            double var3 = var2.dotProduct(this.kickWay) / this.gain;
            double var5 = var2.dotProduct(right(this.kickWay)) / this.gain;
            if (!(Math.abs(var3) > 0.15000000000000002) && !(Math.abs(var5) > 0.15000000000000002)) {
               this.rival.flew(var3, var5);
            }
         }
      }
   }

   private void read() {
      int var3 = this.flight.size();
      if (var3 >= 3) {
         AirTrack.Sample var4 = this.flight.get(var3 - 3);
         AirTrack.Sample var5 = this.flight.get(var3 - 2);
         AirTrack.Sample var6 = this.flight.get(var3 - 1);
         int var7 = var5.tick() - var4.tick();
         int var8 = var6.tick() - var5.tick();
         if (var7 > 0 && var8 > 0 && var7 <= 4 && var8 <= 4) {
            Vec3d var9 = var5.at().subtract(var4.at()).multiply(1.0 / var7);
            Vec3d var10 = var6.at().subtract(var5.at()).multiply(1.0 / var8);
            double var11 = Math.pow(0.91, (var7 + var8) / 2.0);
            double var1;
            Vec3d var13 = new Vec3d((var10.x - var11 * var9.x) * (var1 = 0.08999999999999997 / (1.0 - var11)), 0.0, (var10.z - var11 * var9.z) * var1);
            if (!(var13.horizontalLength() > 0.045)) {
               this.pushSum = this.pushSum.add(var13);
               this.pushes++;
            }
         }
      }
   }

   private void readEarly() {
      if (this.kickAt >= 0 && this.kickSeen && !this.kickSpoiled) {
         AirTrack.Sample var3 = this.flight.get(0);
         AirTrack.Sample var4 = this.flight.get(1);
         int var5 = var4.tick() - var3.tick();
         if (var5 > 0 && var5 <= 4) {
            Vec3d var6 = var4.at().subtract(var3.at()).multiply(1.0 / var5);
            double var7 = friction(this.target.getEntityWorld(), this.kickFrom) * 0.91;
            byte var9 = 20;
            double[] var10 = new double[var9 + 1];
            double[] var11 = new double[var9 + 1];
            double[] var12 = new double[var9 + 1];
            double var13 = this.kick.y;
            double var15 = 1.0;
            double var17 = 1.0;

            for (int var19 = 1; var19 <= var9; var19++) {
               if (var19 > 1) {
                  double var20 = var19 == 2 ? var7 : 0.91;
                  var15 *= var20;
                  var17 = var17 * var20 + 1.0;
                  var13 = (var13 - 0.08) * 0.98;
               }

               var10[var19] = var13;
               var11[var19] = var15;
               var12[var19] = var17;
            }

            int var25 = -1;
            double var26 = 1.7976931348623157E308;

            for (int var22 = var5; var22 <= var9; var22++) {
               double var23 = Math.abs(window(var10, var22, var5) - var6.y);
               if (var23 < var26) {
                  var26 = var23;
                  var25 = var22;
               }
            }

            if (var25 >= 0 && !(var26 > 0.04)) {
               double var27 = window(var11, var25, var5);
               Vec3d var24 = new Vec3d(var6.x - this.kick.x * var27, 0.0, var6.z - this.kick.z * var27).multiply(1.0 / window(var12, var25, var5));
               if (var24.horizontalLength() <= 0.1) {
                  this.early = var24;
               }
            }
         }
      }
   }

   private static double window(double[] ds, int i, int j) {
      double var3 = 0.0;

      for (int var5 = i - j + 1; var5 <= i; var5++) {
         var3 += ds[var5];
      }

      return var3 / j;
   }

   private Vec3d velocity() {
      int var1 = this.flight.size();
      if (var1 < 2) {
         return null;
      } else {
         AirTrack.Sample var2 = this.flight.get(var1 - 2);
         AirTrack.Sample var3 = this.flight.get(var1 - 1);
         int var4 = var3.tick() - var2.tick();
         if (var4 > 0 && var4 <= 4) {
            Vec3d var5 = var3.at().subtract(var2.at()).multiply(1.0 / var4);
            double var6 = (var4 - 1) / 2.0;
            double var8 = Math.pow(0.91, var6);
            return new Vec3d(var5.x * var8, var5.y - 0.0784 * var6, var5.z * var8);
         } else {
            return null;
         }
      }
   }

   private void restart() {
      this.flight.clear();
      this.pushSum = Vec3d.ZERO;
      this.pushes = 0;
      this.early = null;
   }

   private static AirTrack.Stop fly(World world, PlayerEntity playerEntity, Vec3d vec3d, Vec3d vec3d2, Vec3d vec3d3, boolean bl, int i) {
      Box var7 = playerEntity.getBoundingBox().offset(vec3d.subtract(playerEntity.getEntityPos()));
      double var8 = bl ? friction(world, vec3d) * 0.91 : 0.91;
      double var10 = vec3d2.x;
      double var12 = vec3d2.y;
      double var14 = vec3d2.z;
      Vec3d var16 = vec3d;

      for (int var17 = 1; var17 <= i; var17++) {
         if (var17 > 1 || !bl) {
            double var18 = bl && var17 == 2 ? var8 : 0.91;
            var10 *= var18;
            var14 *= var18;
            var12 = (var12 - 0.08) * 0.98;
         }

         Vec3d var20 = Entity.adjustMovementForCollisions(playerEntity, new Vec3d(var10 += vec3d3.x, var12, var14 += vec3d3.z), var7, world, List.of());
         var7 = var7.offset(var20);
         var16 = var16.add(var20);
         if (var20.x != var10) {
            var10 = 0.0;
         }

         if (var20.z != var14) {
            var14 = 0.0;
         }

         if (var20.y != var12) {
            if (var12 < 0.0) {
               return new AirTrack.Stop(var16, var17, true);
            }

            var12 = 0.0;
         }
      }

      return new AirTrack.Stop(var16, i, false);
   }

   private static double friction(World world, Vec3d vec3d) {
      return world.getBlockState(BlockPos.ofFloored(vec3d.x, vec3d.y - 0.5000001, vec3d.z)).getBlock().getSlipperiness();
   }

   private static double resistance(PlayerEntity playerEntity) {
      double[] var1 = new double[]{0.0};

      for (EquipmentSlot var5 : EquipmentSlot.values()) {
         if (var5.getType() == Type.HUMANOID_ARMOR) {
            playerEntity.getEquippedStack(var5).applyAttributeModifiers(var5, (registryEntry, entityAttributeModifier) -> {
               if (registryEntry.value() == EntityAttributes.KNOCKBACK_RESISTANCE.value() && entityAttributeModifier.operation() == Operation.ADD_VALUE) {
                  var1[0] += entityAttributeModifier.value();
               }
            });
         }
      }

      return Math.min(1.0, Math.max(0.0, var1[0]));
   }

   private static boolean floating(PlayerEntity playerEntity) {
      return playerEntity.isGliding()
         || playerEntity.isInFluid()
         || playerEntity.isClimbing()
         || playerEntity.hasVehicle()
         || playerEntity.getAbilities().flying
         || playerEntity.hasStatusEffect(StatusEffects.LEVITATION)
         || playerEntity.hasStatusEffect(StatusEffects.SLOW_FALLING);
   }

   private static Vec3d server(PlayerEntity playerEntity) {
      PositionInterpolator var1 = playerEntity.getInterpolator();
      return var1 == null ? playerEntity.getEntityPos() : var1.getLerpedPos();
   }

   private static Vec3d right(Vec3d vec3d) {
      return new Vec3d(-vec3d.z, 0.0, vec3d.x);
   }

   private static Vec3d limit(Vec3d vec3d, double d) {
      double var3 = vec3d.horizontalLength();
      return var3 <= d ? vec3d : vec3d.multiply(d / var3);
   }

   @Environment(EnvType.CLIENT)
   record Landing(Vec3d at, int ticks) {
   }

   @Environment(EnvType.CLIENT)
   private record Sample(int tick, Vec3d at) {
   }

   @Environment(EnvType.CLIENT)
   private record Stop(Vec3d at, int ticks, boolean down) {
   }
}
