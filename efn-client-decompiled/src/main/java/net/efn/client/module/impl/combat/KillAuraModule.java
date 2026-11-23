package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class KillAuraModule extends ClientModule {
   private static final String PRIORITY_DISTANCE = "Distance";
   private static final String PRIORITY_HEALTH = "Health";
   private static final String PRIORITY_ANGLE = "Angle";
   private static final String CRITS_OFF = "Off";
   private static final String CRITS_WAIT = "Wait";
   private static final String CRITS_JUMP = "Jump";
   private static final int CRIT_HOLD_TICKS = 8;
   private static final int JUMP_LEAD = 7;
   private static final double REACH_MARGIN = 0.01;
   private static final double RIDING_BONUS = 2.5;
   private static final double MAX_STEP_SQ = 1.0;
   private static final double[] AIM_HEIGHTS = new double[]{0.85, 0.5, 0.15};
   private final BooleanSetting players = this.addSetting("players", new BooleanSetting("Players", true));
   private final BooleanSetting hostiles = this.addSetting("hostiles", new BooleanSetting("Hostiles", false));
   private final ModeSetting priority = this.addSetting("priority", new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle"));
   private final ModeSetting crits = this.addSetting("crits", new ModeSetting("Crits", "Wait", "Off", "Wait", "Jump"));
   private LivingEntity target;
   private int critWait;

   public KillAuraModule() {
      super("kill-aura", "Kill Aura", "Attacks nearby targets without moving your camera.", ModuleCategory.COMBAT);
   }

   @Override
   protected void onDisable() {
      this.target = null;
      this.critWait = 0;
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onTick(TickEvent var1) {
      MinecraftClient var5 = MinecraftClient.method_1551();
      ClientPlayerEntity var6 = var5.field_1724;
      if (MeleeAttacks.canAct(var5)
         && !var6.method_29504()
         && !var6.method_7325()
         && var5.field_1771 <= 0
         && MeleeAttacks.handsFree(var5, var6)
         && !CombatMacroLock.heldByOther(this)
         && !MeleeAttacks.actedThisTick(var6)
         && !MeleeAttacks.rightClickComing(var5)) {
         boolean var7 = var6.method_5765();
         ItemStack var8 = var6.method_6047();
         boolean var9 = var8.method_57826(DataComponentTypes.field_63631);
         double var10 = var9 ? var6.method_76693().method_76739(var6) - 0.01 : var6.method_55755() - 0.01 + (var7 ? 2.5 : 0.0);
         Vec3d var12 = var6.method_33571();
         this.target = this.pick(var5, var6, var12, var10);
         if (this.target == null) {
            this.critWait = 0;
         } else {
            MaceModule var13 = ModuleRegistry.get(MaceModule.class);
            boolean var4 = !var9 && var13 != null && var13.smashReady(var6, this.target);
            boolean var15 = var9 ? !var6.method_75202(var8, 0) : var4 || MeleeAttacks.charge(var6) >= 1.0F;
            boolean var16 = !var15 && MeleeAttacks.ticksToFull(var6) <= 1;
            boolean var3 = !var9 && this.crits.is("Jump") && !var7 && var6.method_24828() && !var6.method_5799() && MeleeAttacks.ticksToFull(var6) <= 7;
            if (var3) {
               InputControl.jump();
            }

            Box var18 = this.target.method_5829();
            if (var16) {
               var18 = var18.method_997(stride(this.target));
            }

            Vec3d var2;
            if ((var2 = aimPoint(var6, var12, var18)) != null) {
               double var19 = Math.min(0.03, var18.method_17939() * 0.15);
               Box var21 = var18.method_35580(var19, 0.01, var19);
               Rotation var22 = RotationUtils.fromTo(var12, var2);
               Rotation var23 = !var7 && !var21.method_1006(var12) && ServerRotation.canRotate(var6) && !ServerRotation.pending()
                  ? ServerRotation.nextStep(var6, var22)
                  : null;
               Vec3d var24 = hits(var6, var12, var21, var10, var23);
               if (!var15) {
                  if (var16 && var24 != null) {
                     ServerRotation.request(var22);
                  }
               } else {
                  Rotation var25 = var7 ? RotationUtils.entityRotation(var6) : ServerRotation.sent();
                  Vec3d var26 = hits(var6, var12, var21, var10, var25);
                  if (var26 != null || var24 != null) {
                     if (var9) {
                        if (var26 == null) {
                           ServerRotation.request(var22);
                        } else if (!hitsOthers(var6, (Entity)this.target, var12, var25)
                           && MeleeAttacks.stab(var5, new EntityHitResult((Entity)this.target, var26))) {
                           MeleeAttacks.claimTick(var5);
                        }
                     } else {
                        boolean var27 = var4 ? var13.holdsSmash(var6, this.target) : !this.crits.is("Off") && !var7 && this.waitForCrit(var6, var3);
                        if (var27) {
                           if (var4) {
                              InputControl.dropSprint();
                           }

                           if (var24 != null && !var5.field_1690.field_1886.method_1434() && !MeleeAttacks.clickPending(var5)) {
                              ServerRotation.request(var22);
                           }
                        } else {
                           if (var26 == null) {
                              ServerRotation.request(var22);
                              var26 = var24;
                           }

                           if (MeleeAttacks.attack(var5, new EntityHitResult((Entity)this.target, var26), var7 ? 2.5 : 0.0)) {
                              this.critWait = 0;
                              if (var4) {
                                 var13.noteSmash();
                              }

                              MeleeAttacks.claimTick(var5);
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.critWait = 0;
      }
   }

   private boolean waitForCrit(ClientPlayerEntity var1, boolean var2) {
      if (MeleeAttacks.airborne(var1)) {
         if (var1.method_5624() || !(var1.field_6017 > 0.0) && this.critWait++ < 8) {
            InputControl.dropSprint();
            return true;
         } else {
            return false;
         }
      } else {
         return var1.method_24828() && (var2 || var1.field_3913.field_54155.comp_3163()) && this.critWait++ < 8;
      }
   }

   private LivingEntity pick(MinecraftClient var1, ClientPlayerEntity var2, Vec3d var3, double var4) {
      LivingEntity var6 = null;
      double var7 = 0.0;
      double var9 = 0.0;

      for (Entity var12 : var1.field_1687.method_18112()) {
         Vec3d var13;
         LivingEntity var14;
         Box var15;
         double var16;
         if (var12 instanceof LivingEntity
            && !((var16 = var3.method_1022(TargetingUtils.closestPoint(var3, var15 = (var14 = (LivingEntity)var12).method_5829()))) > var4)
            && this.wanted(var2, var14)
            && (var13 = aimPoint(var2, var3, var15)) != null) {
            if (var14 == this.target) {
               return var14;
            }

            double var18 = this.rank(var2, var3, var14, var13, var16);
            if (var6 != null) {
               int var20 = Double.compare(var18, var7);
               if (var20 == 0) {
                  var20 = Boolean.compare(var14.field_6235 > 0, var6.field_6235 > 0);
               }

               if (var20 == 0) {
                  var20 = Double.compare(var16, var9);
               }

               if (var20 >= 0) {
                  continue;
               }
            }

            var6 = var14;
            var7 = var18;
            var9 = var16;
         }
      }

      return var6;
   }

   private double rank(ClientPlayerEntity var1, Vec3d var2, LivingEntity var3, Vec3d var4, double var5) {
      if (this.priority.is("Health")) {
         return CrystalDamage.health(var3);
      } else if (!this.priority.is("Angle")) {
         return var5;
      } else {
         Rotation var7 = ServerRotation.sent();
         Rotation var8 = var7 != null ? var7 : RotationUtils.entityRotation(var1);
         return RotationUtils.angleTo(var8, RotationUtils.fromTo(var2, var4));
      }
   }

   private boolean wanted(ClientPlayerEntity var1, LivingEntity var2) {
      if (!MeleeAttacks.isEnemy(var1, var2) || MeleeAttacks.wastedOnShield(var1, var2)) {
         return false;
      } else {
         return var2 instanceof PlayerEntity ? this.players.get() : this.hostiles.get() && MeleeAttacks.isHostile(var2);
      }
   }

   private static Vec3d aimPoint(ClientPlayerEntity var0, Vec3d var1, Box var2) {
      double var3 = Math.min(0.15, var2.method_17939() * 0.3);
      Vec3d var5 = TargetingUtils.closestPoint(var1, var2.method_35580(var3, Math.min(0.2, var2.method_17940() * 0.25), var3));
      if (MeleeAttacks.lineIsClear(var0, var1, var5)) {
         return var5;
      } else {
         Vec3d var6 = var2.method_1005();

         for (double var10 : AIM_HEIGHTS) {
            Vec3d var12 = new Vec3d(var6.field_1352, var2.field_1322 + var2.method_17940() * var10, var6.field_1350);
            if (MeleeAttacks.lineIsClear(var0, var1, var12)) {
               return var12;
            }
         }

         return null;
      }
   }

   private static Vec3d stride(LivingEntity var0) {
      double var3 = var0.method_23317() - var0.field_6014;
      double var1;
      return var3 * var3 + (var1 = var0.method_23321() - var0.field_5969) * var1 > 1.0 ? Vec3d.field_1353 : new Vec3d(var3, 0.0, var1);
   }

   private static Vec3d hits(ClientPlayerEntity var0, Vec3d var1, Box var2, double var3, Rotation var5) {
      if (var5 == null) {
         return null;
      } else {
         Vec3d var6 = Vec3d.method_1030(var5.pitch(), var5.yaw());
         Vec3d var7 = var2.method_992(var1, var1.method_1019(var6.method_1021(var3))).orElse(var2.method_1006(var1) ? var1 : null);
         return var7 != null && var1.method_1022(var7) <= var3 && MeleeAttacks.lineIsClear(var0, var1, var7) ? var7 : null;
      }
   }

   private static boolean hitsOthers(ClientPlayerEntity var0, Entity var1, Vec3d var2, Rotation var3) {
      AttackRangeComponent var4 = var0.method_76693();
      Vec3d var5 = Vec3d.method_1030(var3.pitch(), var3.yaw());
      Vec3d var6 = var2.method_1019(var5.method_1021(var4.method_76732(var0)));
      double var7 = Math.max(0.0, var0.method_60478().method_1026(var5));
      Vec3d var9 = var2.method_1019(var5.method_1021(var4.method_76739(var0) + var7));

      for (Entity var11 : var0.method_73183().method_8335(var0, new Box(var6, var9).method_1014(1.0))) {
         Box var12;
         if (var11 != var1
            && var11 instanceof LivingEntity
            && PiercingWeaponComponent.method_75244(var0, var11)
            && !MeleeAttacks.isEnemy(var0, var11)
            && ((var12 = var11.method_5829().method_1014(var4.comp_5262())).method_1006(var6) || var12.method_992(var6, var9).isPresent())) {
            return true;
         }
      }

      return false;
   }
}
