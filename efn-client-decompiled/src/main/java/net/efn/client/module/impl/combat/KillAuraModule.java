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
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var5 = MinecraftClient.getInstance();
      ClientPlayerEntity var6 = var5.player;
      if (MeleeAttacks.canAct(var5)
         && !var6.isDead()
         && !var6.isSpectator()
         && var5.attackCooldown <= 0
         && MeleeAttacks.handsFree(var5, var6)
         && !CombatMacroLock.heldByOther(this)
         && !MeleeAttacks.actedThisTick(var6)
         && !MeleeAttacks.rightClickComing(var5)) {
         boolean var7 = var6.hasVehicle();
         ItemStack var8 = var6.getMainHandStack();
         boolean var9 = var8.contains(DataComponentTypes.PIERCING_WEAPON);
         double var10 = var9 ? var6.getAttackRange().getEffectiveMaxRange(var6) - 0.01 : var6.getEntityInteractionRange() - 0.01 + (var7 ? 2.5 : 0.0);
         Vec3d var12 = var6.getEyePos();
         this.target = this.pick(var5, var6, var12, var10);
         if (this.target == null) {
            this.critWait = 0;
         } else {
            MaceModule var13 = ModuleRegistry.get(MaceModule.class);
            boolean var4 = !var9 && var13 != null && var13.smashReady(var6, this.target);
            boolean var15 = var9 ? !var6.isBelowMinimumAttackCharge(var8, 0) : var4 || MeleeAttacks.charge(var6) >= 1.0F;
            boolean var16 = !var15 && MeleeAttacks.ticksToFull(var6) <= 1;
            boolean var3 = !var9 && this.crits.is("Jump") && !var7 && var6.isOnGround() && !var6.isTouchingWater() && MeleeAttacks.ticksToFull(var6) <= 7;
            if (var3) {
               InputControl.jump();
            }

            Box var18 = this.target.getBoundingBox();
            if (var16) {
               var18 = var18.offset(stride(this.target));
            }

            Vec3d var2;
            if ((var2 = aimPoint(var6, var12, var18)) != null) {
               double var19 = Math.min(0.03, var18.getLengthX() * 0.15);
               Box var21 = var18.contract(var19, 0.01, var19);
               Rotation var22 = RotationUtils.fromTo(var12, var2);
               Rotation var23 = !var7 && !var21.contains(var12) && ServerRotation.canRotate(var6) && !ServerRotation.pending()
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
                        } else if (!hitsOthers(var6, this.target, var12, var25) && MeleeAttacks.stab(var5, new EntityHitResult(this.target, var26))) {
                           MeleeAttacks.claimTick(var5);
                        }
                     } else {
                        boolean var27 = var4 ? var13.holdsSmash(var6, this.target) : !this.crits.is("Off") && !var7 && this.waitForCrit(var6, var3);
                        if (var27) {
                           if (var4) {
                              InputControl.dropSprint();
                           }

                           if (var24 != null && !var5.options.attackKey.isPressed() && !MeleeAttacks.clickPending(var5)) {
                              ServerRotation.request(var22);
                           }
                        } else {
                           if (var26 == null) {
                              ServerRotation.request(var22);
                              var26 = var24;
                           }

                           if (MeleeAttacks.attack(var5, new EntityHitResult(this.target, var26), var7 ? 2.5 : 0.0)) {
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

   private boolean waitForCrit(ClientPlayerEntity clientPlayerEntity, boolean bl) {
      if (MeleeAttacks.airborne(clientPlayerEntity)) {
         if (clientPlayerEntity.isSprinting() || !(clientPlayerEntity.fallDistance > 0.0) && this.critWait++ < 8) {
            InputControl.dropSprint();
            return true;
         } else {
            return false;
         }
      } else {
         return clientPlayerEntity.isOnGround() && (bl || clientPlayerEntity.input.playerInput.jump()) && this.critWait++ < 8;
      }
   }

   private LivingEntity pick(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, double d) {
      LivingEntity var6 = null;
      double var7 = 0.0;
      double var9 = 0.0;

      for (Entity var12 : minecraftClient.world.getEntities()) {
         Vec3d var13;
         LivingEntity var14;
         Box var15;
         double var16;
         if (var12 instanceof LivingEntity
            && !((var16 = vec3d.distanceTo(TargetingUtils.closestPoint(vec3d, var15 = (var14 = (LivingEntity)var12).getBoundingBox()))) > d)
            && this.wanted(clientPlayerEntity, var14)
            && (var13 = aimPoint(clientPlayerEntity, vec3d, var15)) != null) {
            if (var14 == this.target) {
               return var14;
            }

            double var18 = this.rank(clientPlayerEntity, vec3d, var14, var13, var16);
            if (var6 != null) {
               int var20 = Double.compare(var18, var7);
               if (var20 == 0) {
                  var20 = Boolean.compare(var14.hurtTime > 0, var6.hurtTime > 0);
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

   private double rank(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, LivingEntity livingEntity, Vec3d vec3d2, double d) {
      if (this.priority.is("Health")) {
         return CrystalDamage.health(livingEntity);
      } else if (!this.priority.is("Angle")) {
         return d;
      } else {
         Rotation var7 = ServerRotation.sent();
         Rotation var8 = var7 != null ? var7 : RotationUtils.entityRotation(clientPlayerEntity);
         return RotationUtils.angleTo(var8, RotationUtils.fromTo(vec3d, vec3d2));
      }
   }

   private boolean wanted(ClientPlayerEntity clientPlayerEntity, LivingEntity livingEntity) {
      if (!MeleeAttacks.isEnemy(clientPlayerEntity, livingEntity) || MeleeAttacks.wastedOnShield(clientPlayerEntity, livingEntity)) {
         return false;
      } else {
         return livingEntity instanceof PlayerEntity ? this.players.get() : this.hostiles.get() && MeleeAttacks.isHostile(livingEntity);
      }
   }

   private static Vec3d aimPoint(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, Box box) {
      double var3 = Math.min(0.15, box.getLengthX() * 0.3);
      Vec3d var5 = TargetingUtils.closestPoint(vec3d, box.contract(var3, Math.min(0.2, box.getLengthY() * 0.25), var3));
      if (MeleeAttacks.lineIsClear(clientPlayerEntity, vec3d, var5)) {
         return var5;
      } else {
         Vec3d var6 = box.getCenter();

         for (double var10 : AIM_HEIGHTS) {
            Vec3d var12 = new Vec3d(var6.x, box.minY + box.getLengthY() * var10, var6.z);
            if (MeleeAttacks.lineIsClear(clientPlayerEntity, vec3d, var12)) {
               return var12;
            }
         }

         return null;
      }
   }

   private static Vec3d stride(LivingEntity livingEntity) {
      double var3 = livingEntity.getX() - livingEntity.lastX;
      double var1;
      return var3 * var3 + (var1 = livingEntity.getZ() - livingEntity.lastZ) * var1 > 1.0 ? Vec3d.ZERO : new Vec3d(var3, 0.0, var1);
   }

   private static Vec3d hits(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, Box box, double d, Rotation rotation) {
      if (rotation == null) {
         return null;
      } else {
         Vec3d var6 = Vec3d.fromPolar(rotation.pitch(), rotation.yaw());
         Vec3d var7 = box.raycast(vec3d, vec3d.add(var6.multiply(d))).orElse(box.contains(vec3d) ? vec3d : null);
         return var7 != null && vec3d.distanceTo(var7) <= d && MeleeAttacks.lineIsClear(clientPlayerEntity, vec3d, var7) ? var7 : null;
      }
   }

   private static boolean hitsOthers(ClientPlayerEntity clientPlayerEntity, Entity entity, Vec3d vec3d, Rotation rotation) {
      AttackRangeComponent var4 = clientPlayerEntity.getAttackRange();
      Vec3d var5 = Vec3d.fromPolar(rotation.pitch(), rotation.yaw());
      Vec3d var6 = vec3d.add(var5.multiply(var4.getEffectiveMinRange(clientPlayerEntity)));
      double var7 = Math.max(0.0, clientPlayerEntity.getMovement().dotProduct(var5));
      Vec3d var9 = vec3d.add(var5.multiply(var4.getEffectiveMaxRange(clientPlayerEntity) + var7));

      for (Entity var11 : clientPlayerEntity.getEntityWorld().getOtherEntities(clientPlayerEntity, new Box(var6, var9).expand(1.0))) {
         Box var12;
         if (var11 != entity
            && var11 instanceof LivingEntity
            && PiercingWeaponComponent.canHit(clientPlayerEntity, var11)
            && !MeleeAttacks.isEnemy(clientPlayerEntity, var11)
            && ((var12 = var11.getBoundingBox().expand(var4.hitboxMargin())).contains(var6) || var12.raycast(var6, var9).isPresent())) {
            return true;
         }
      }

      return false;
   }
}
