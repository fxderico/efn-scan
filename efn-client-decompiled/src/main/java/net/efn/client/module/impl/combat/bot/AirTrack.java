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
   private Vec3d pushSum = Vec3d.field_1353;
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
      this.pushSum = Vec3d.field_1353;
      this.pushes = 0;
      this.kickAt = -1;
      this.guessed = null;
      this.bare = null;
   }

   void note(PlayerEntity var1, int var2, RivalBook.Rival var3) {
      if (var1 != this.target) {
         this.forget();
         this.target = var1;
      }

      this.rival = var3;
      Vec3d var5 = server(var1);
      boolean var6 = !var5.equals(this.lastSeen);
      this.lastSeen = var5;
      if (this.kickAt >= 0 && !this.kickSeen) {
         if (var5.field_1351 > this.kickFrom.field_1351 + 0.05 || var5.method_1020(this.kickFrom).method_37268() > 0.01) {
            this.kickSeen = true;
            this.kickSeenAt = var2;
         } else if (var2 - this.kickAt > 8) {
            this.kickAt = -1;
         }
      }

      if (this.kickAt >= 0 && var2 - this.kickAt > 40) {
         this.kickAt = -1;
      }

      boolean var4 = !var1.method_24828() && !floating(var1);
      if (var4) {
         if (!this.airborne) {
            this.airborne = true;
            this.restart();
         }

         if (var6) {
            this.flight.add(new AirTrack.Sample(var2, var5));
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

   void kicked(ClientPlayerEntity var1, PlayerEntity var2, float var3, boolean var4, int var5) {
      if (var2 == this.target) {
         this.kickAt = -1;
         double var6 = 1.0 - resistance(var2);
         if (!(var6 <= 0.0)) {
            Vec3d var8 = server(var2);
            Vec3d var9 = var8.method_1020(var1.method_73189()).method_18805(1.0, 0.0, 1.0);
            if (!(var9.method_1027() < 1.0E-6)) {
               var9 = var9.method_1029();
               Vec3d var10 = var9.method_1021(0.4 * var6);
               double var11 = Math.min(0.4, 0.4 * var6);
               if (var4) {
                  double var13 = Math.toRadians(var3);
                  Vec3d var15 = new Vec3d(-Math.sin(var13), 0.0, Math.cos(var13));
                  var10 = var10.method_1021(0.5).method_1019(var15.method_1021(0.5 * var6));
                  var11 = Math.min(0.4, var11 / 2.0 + 0.5 * var6);
               }

               this.kick = new Vec3d(var10.field_1352, var11, var10.field_1350);
               this.kickWay = var10.method_1027() < 1.0E-6 ? var9 : var10.method_1029();
               this.kickFrom = var8;
               this.kickAt = var5;
               this.kickSpoiled = false;
               this.kickSeen = false;
               this.early = null;
               World var18 = var2.method_73183();
               AirTrack.Stop var14 = fly(var18, var2, var8, this.kick, Vec3d.field_1353, true, 40);
               AirTrack.Stop var19 = fly(var18, var2, var8, this.kick, this.kickWay.method_1021(0.01), true, 40);
               this.bare = var14.down() ? new AirTrack.Landing(var14.at(), var14.ticks()) : null;
               this.gain = var14.down() && var19.down() ? var19.at().method_1020(var14.at()).method_1026(this.kickWay) / 0.01 : 0.0;
               AirTrack.Stop var16 = fly(var18, var2, var8, this.kick, this.learned(0.1), true, 40);
               this.guessed = var16.down() ? new AirTrack.Landing(var16.at(), var16.ticks()) : null;
            }
         }
      }
   }

   void blast(Vec3d var1) {
      if (this.target != null && !(server(this.target).method_1022(var1) > 10.0)) {
         this.kickSpoiled = true;
         if (!this.kickSeen) {
            this.kickAt = -1;
         }

         this.restart();
      }
   }

   AirTrack.Landing landing(int var1) {
      AirTrack.Stop var2 = this.path(40, var1);
      return var2 != null && var2.down() ? new AirTrack.Landing(var2.at(), var2.ticks()) : null;
   }

   Vec3d lead(int var1, int var2) {
      if (this.target == null || var1 <= 0) {
         return null;
      } else if (this.kickAt < 0 && this.pushes < 2) {
         return null;
      } else {
         AirTrack.Stop var3 = this.path(var1, var2);
         return var3 == null ? null : var3.at().method_1020(this.target.method_73189());
      }
   }

   private AirTrack.Stop path(int var1, int var2) {
      if (this.target != null && !floating(this.target)) {
         World var4 = this.target.method_73183();
         Vec3d var3 = this.airborne ? this.velocity() : null;
         if (this.kickAt < 0 || this.kickSeen && var3 != null) {
            if (var3 == null) {
               return null;
            } else {
               AirTrack.Sample var9 = this.flight.get(this.flight.size() - 1);
               int var10 = Math.min(3, var2 - var9.tick());
               AirTrack.Stop var8 = fly(var4, this.target, var9.at(), var3, this.push(var3), false, var1 + var10);
               return new AirTrack.Stop(var8.at(), Math.max(0, var8.ticks() - var10), var8.down());
            }
         } else {
            int var6 = this.kickSeen ? var2 - this.kickSeenAt + 1 : 0;
            AirTrack.Stop var7 = fly(var4, this.target, this.kickFrom, this.kick, this.learned(0.1), true, var1 + var6);
            return new AirTrack.Stop(var7.at(), Math.max(0, var7.ticks() - var6), var7.down());
         }
      } else {
         return null;
      }
   }

   private Vec3d push(Vec3d var1) {
      if (this.pushes >= 2) {
         return limit(this.pushSum.method_1021(1.0 / this.pushes), 0.045);
      } else {
         boolean var4 = this.kickAt >= 0 && this.rival != null && this.rival.knowsFlight();
         Vec3d var3 = this.kickAt >= 0 ? this.learned(0.026) : limit(new Vec3d(var1.field_1352, 0.0, var1.field_1350).method_1021(0.08999999999999997), 0.026);
         Vec3d var2 = this.pushes == 1 ? (this.early != null ? this.pushSum.method_1019(this.early).method_1021(0.5) : this.pushSum) : this.early;
         return var2 == null ? var3 : limit(var4 ? var2.method_1019(var3).method_1021(0.5) : var2, 0.045);
      }
   }

   private Vec3d learned(double var1) {
      return this.rival != null && this.rival.knowsFlight() && this.kickWay != null
         ? limit(this.kickWay.method_1021(this.rival.airAlong).method_1019(right(this.kickWay).method_1021(this.rival.airAcross)), var1)
         : Vec3d.field_1353;
   }

   private void learn(Vec3d var1) {
      if (this.rival != null && !this.kickSpoiled && this.bare != null && !(this.gain < 1.0)) {
         if (!(Math.abs(var1.field_1351 - this.bare.at().field_1351) > 0.6)) {
            Vec3d var2 = var1.method_1020(this.bare.at());
            double var3 = var2.method_1026(this.kickWay) / this.gain;
            double var5 = var2.method_1026(right(this.kickWay)) / this.gain;
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
            Vec3d var9 = var5.at().method_1020(var4.at()).method_1021(1.0 / var7);
            Vec3d var10 = var6.at().method_1020(var5.at()).method_1021(1.0 / var8);
            double var11 = Math.pow(0.91, (var7 + var8) / 2.0);
            double var1;
            Vec3d var13 = new Vec3d(
               (var10.field_1352 - var11 * var9.field_1352) * (var1 = 0.08999999999999997 / (1.0 - var11)),
               0.0,
               (var10.field_1350 - var11 * var9.field_1350) * var1
            );
            if (!(var13.method_37267() > 0.045)) {
               this.pushSum = this.pushSum.method_1019(var13);
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
            Vec3d var6 = var4.at().method_1020(var3.at()).method_1021(1.0 / var5);
            double var7 = friction(this.target.method_73183(), this.kickFrom) * 0.91;
            byte var9 = 20;
            double[] var10 = new double[var9 + 1];
            double[] var11 = new double[var9 + 1];
            double[] var12 = new double[var9 + 1];
            double var13 = this.kick.field_1351;
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
               double var23 = Math.abs(window(var10, var22, var5) - var6.field_1351);
               if (var23 < var26) {
                  var26 = var23;
                  var25 = var22;
               }
            }

            if (var25 >= 0 && !(var26 > 0.04)) {
               double var27 = window(var11, var25, var5);
               Vec3d var24 = new Vec3d(var6.field_1352 - this.kick.field_1352 * var27, 0.0, var6.field_1350 - this.kick.field_1350 * var27)
                  .method_1021(1.0 / window(var12, var25, var5));
               if (var24.method_37267() <= 0.1) {
                  this.early = var24;
               }
            }
         }
      }
   }

   private static double window(double[] var0, int var1, int var2) {
      double var3 = 0.0;

      for (int var5 = var1 - var2 + 1; var5 <= var1; var5++) {
         var3 += var0[var5];
      }

      return var3 / var2;
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
            Vec3d var5 = var3.at().method_1020(var2.at()).method_1021(1.0 / var4);
            double var6 = (var4 - 1) / 2.0;
            double var8 = Math.pow(0.91, var6);
            return new Vec3d(var5.field_1352 * var8, var5.field_1351 - 0.0784 * var6, var5.field_1350 * var8);
         } else {
            return null;
         }
      }
   }

   private void restart() {
      this.flight.clear();
      this.pushSum = Vec3d.field_1353;
      this.pushes = 0;
      this.early = null;
   }

   private static AirTrack.Stop fly(World var0, PlayerEntity var1, Vec3d var2, Vec3d var3, Vec3d var4, boolean var5, int var6) {
      Box var7 = var1.method_5829().method_997(var2.method_1020(var1.method_73189()));
      double var8 = var5 ? friction(var0, var2) * 0.91 : 0.91;
      double var10 = var3.field_1352;
      double var12 = var3.field_1351;
      double var14 = var3.field_1350;
      Vec3d var16 = var2;

      for (int var17 = 1; var17 <= var6; var17++) {
         if (var17 > 1 || !var5) {
            double var18 = var5 && var17 == 2 ? var8 : 0.91;
            var10 *= var18;
            var14 *= var18;
            var12 = (var12 - 0.08) * 0.98;
         }

         Vec3d var20 = Entity.method_20736(var1, new Vec3d(var10 += var4.field_1352, var12, var14 += var4.field_1350), var7, var0, List.of());
         var7 = var7.method_997(var20);
         var16 = var16.method_1019(var20);
         if (var20.field_1352 != var10) {
            var10 = 0.0;
         }

         if (var20.field_1350 != var14) {
            var14 = 0.0;
         }

         if (var20.field_1351 != var12) {
            if (var12 < 0.0) {
               return new AirTrack.Stop(var16, var17, true);
            }

            var12 = 0.0;
         }
      }

      return new AirTrack.Stop(var16, var6, false);
   }

   private static double friction(World var0, Vec3d var1) {
      return var0.method_8320(BlockPos.method_49637(var1.field_1352, var1.field_1351 - 0.5000001, var1.field_1350)).method_26204().method_9499();
   }

   private static double resistance(PlayerEntity var0) {
      double[] var1 = new double[]{0.0};

      for (EquipmentSlot var5 : EquipmentSlot.values()) {
         if (var5.method_5925() == Type.field_6178) {
            var0.method_6118(var5).method_57354(var5, (var1x, var2) -> {
               if (var1x.comp_349() == EntityAttributes.field_23718.comp_349() && var2.comp_2450() == Operation.field_6328) {
                  var1[0] += var2.comp_2449();
               }
            });
         }
      }

      return Math.min(1.0, Math.max(0.0, var1[0]));
   }

   private static boolean floating(PlayerEntity var0) {
      return var0.method_6128()
         || var0.method_52535()
         || var0.method_6101()
         || var0.method_5765()
         || var0.method_31549().field_7479
         || var0.method_6059(StatusEffects.field_5902)
         || var0.method_6059(StatusEffects.field_5906);
   }

   private static Vec3d server(PlayerEntity var0) {
      PositionInterpolator var1 = var0.method_66233();
      return var1 == null ? var0.method_73189() : var1.method_66265();
   }

   private static Vec3d right(Vec3d var0) {
      return new Vec3d(-var0.field_1350, 0.0, var0.field_1352);
   }

   private static Vec3d limit(Vec3d var0, double var1) {
      double var3 = var0.method_37267();
      return var3 <= var1 ? var0 : var0.method_1021(var1 / var3);
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
