package net.efn.client.module.impl.combat.bot;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class SwordBotModule extends ClientModule {
   private static final String PATIENT = "Patient";
   private static final String AGGRESSIVE = "Aggressive";
   private static final Predicate<ItemStack> GAPPLE = itemStack -> itemStack.isOf(Items.GOLDEN_APPLE) || itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE);
   private static final double HIT_RANGE = 3.0;
   private static final double CHASE_RANGE = 6.0;
   private static final int RESET_WINDOW = 3;
   private final ModeSetting style = this.addSetting("style", new ModeSetting("Style", "Patient", "Patient", "Aggressive"));
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Fight Range", 16.0F, 6.0F, 32.0F).step(1.0F));
   private final NumberSetting turnSpeed = this.addSetting("turn-speed", new NumberSetting("Turn Speed", 1.0F, 0.5F, 1.5F).step(0.05F));
   private final BooleanSetting strafe = this.addSetting("strafe", new BooleanSetting("Strafe", true));
   private final BooleanSetting crits = this.addSetting("crits", new BooleanSetting("Jump Crits", true));
   private final BooleanSetting jumpReset = this.addSetting("jump-reset", new BooleanSetting("Jump Reset", true));
   private final NumberSetting eatBelow = this.addSetting("eat-below", new NumberSetting("Gapple Below HP", 10.0F, 0.0F, 20.0F).step(1.0F));
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final BotBody body = new BotBody();
   private PlayerEntity fought;
   private double closing;
   private double lastTheirReach = 0.0 / 0.0;
   private double jitter;
   private int tapTicks;
   private boolean tapBack;
   private boolean critJump;
   private int landedTicks;
   private int resetUntil;
   private int lastHurt;
   private boolean eating;

   public SwordBotModule() {
      super(
         "sword-bot",
         "Sword PvP Bot",
         "Fights the nearest enemy player with a sword or axe: keeps out of their reach while charging, times crits, strafes and eats gapples.",
         ModuleCategory.COMBAT
      );
   }

   @Override
   protected void onEnable() {
      this.body.forget();
      this.newFight(null);
      this.eating = false;
      this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.tick());
   }

   @Override
   protected void onDisable() {
      this.body.release(this.mc);
      this.body.forget();
      CombatMacroLock.release(this);
   }

   private void newFight(PlayerEntity playerEntity) {
      this.fought = playerEntity;
      this.closing = 0.0;
      this.lastTheirReach = 0.0 / 0.0;
      this.jitter = randomJitter();
      this.tapTicks = 0;
      this.critJump = false;
      this.resetUntil = 0;
   }

   private void tick() {
      ClientPlayerEntity var8 = this.mc.player;
      this.body.release(this.mc);
      if (var8 != null && this.mc.world != null && MeleeAttacks.canAct(this.mc) && !var8.isDead()) {
         PlayerEntity var9 = this.body.pick(this.mc, var8, this.range.get());
         int var10 = this.body.tick();
         if (var9 != this.fought) {
            if (var9 != null) {
               ModuleChat.send(this, ModuleChat.body().text("Fighting ").name(var9.getName().getString()).text("."));
            } else if (this.fought != null) {
               ModuleChat.send(this, ModuleChat.body().text("No enemy in range, waiting."));
            }

            this.newFight(var9);
         }

         if (var9 != null) {
            this.body.face(this.mc, var8, var9, this.turnSpeed.get());
            boolean var7 = var8.getHealth() <= this.eatBelow.get() && var8.getAbsorptionAmount() < 4.0F;
            if ((var7 || this.eating && var8.isUsingItem()) && this.body.use(this.mc, var8, GAPPLE)) {
               this.eating = true;
               this.body.backOff(this.mc, var8);
            } else {
               this.eating = false;
               if (!var8.isUsingItem()) {
                  holdWeapon(var8);
                  Vec3d var12 = var8.getEyePos();
                  double var13 = var12.distanceTo(TargetingUtils.closestPoint(var12, var9.getBoundingBox()));
                  Vec3d var15 = var9.getEyePos();
                  double var16 = var15.distanceTo(TargetingUtils.closestPoint(var15, var8.getBoundingBox()));
                  if (!Double.isNaN(this.lastTheirReach)) {
                     this.closing = this.closing * 0.5 + (this.lastTheirReach - var16) * 0.5;
                  }

                  this.lastTheirReach = var16;
                  int var18 = MeleeAttacks.ticksToFull(var8);
                  float var19 = MeleeAttacks.charge(var8);
                  double var20 = facingAngle(var9, var8);
                  Vec3d var22 = new Vec3d(var9.getX() - var8.getX(), 0.0, var9.getZ() - var8.getZ()).normalize();
                  Vec3d var23 = var9.getEntityPos().subtract(var9.lastX, var9.lastY, var9.lastZ);
                  double var24 = var23.x * var22.x + var23.z * var22.z;
                  boolean var26 = var9.hurtTime > 1;
                  boolean var27 = var9.isBlocking();
                  boolean var28 = var16 <= 3.15 && var20 <= 40.0;
                  boolean var29 = var13 <= 3.0;
                  double var30 = 3.35
                     + 0.12 * MathHelper.clamp(Math.round(ping(var8) / 50.0F), 0, 3)
                     + 2.5 * MathHelper.clamp(this.closing, 0.0, 0.35)
                     - (this.style.is("Aggressive") ? 0.2 : 0.0);
                  double var5 = var18 <= 0 ? 2.9 : (var18 <= 2 ? 3.08 + 0.1 * (var18 - 1) + this.jitter : Math.max(var30 + this.jitter, 2.9 + 0.26 * var18));
                  if (var20 < 8.0 && var16 < 4.2) {
                     this.body.flipSide();
                  }

                  double var3 = this.strafe.get() ? 1.0 : 0.0;
                  if (var8.hurtTime > this.lastHurt && var8.hurtTime >= 8) {
                     this.resetUntil = var10 + 3;
                  }

                  this.lastHurt = var8.hurtTime;
                  if (this.tapTicks > 0) {
                     this.tapTicks--;
                     this.body.drive(this.mc, var8, this.tapBack ? -1 : 0, var3 * (this.tapBack ? 0.8 : 1.0), false, false);
                  } else if ((!(var13 > 6.0) || !(var13 > var5 + 1.0)) && !(Math.abs(var9.getY() - var8.getY()) > 2.5)) {
                     if (this.critJump) {
                        if (var8.isOnGround()) {
                           if (++this.landedTicks >= 2) {
                              this.critJump = false;
                           }
                        } else {
                           this.landedTicks = 0;
                        }

                        if (this.critJump) {
                           boolean var38 = var18 <= 1 && var8.getVelocity().y < 0.1;
                           this.body.drive(this.mc, var8, var38 ? 0 : 1, var38 ? var3 * 0.35 : 0.0, !var38, false);
                           if (var18 <= 0 && var29 && !var26) {
                              InputControl.dropSprint();
                              this.strike(var8, var9, 0.95F, true);
                           }

                           return;
                        }
                     }

                     if (this.crits.get() && var8.isOnGround() && !var8.isTouchingWater() && !var28 && this.critWindow(var18, var13, var20, var24)) {
                        this.critJump = true;
                        this.landedTicks = 0;
                        this.body.drive(this.mc, var8, 1, 0.0, true, true);
                     } else if (var18 <= 0 && !var27) {
                        if (var26) {
                           this.body.drive(this.mc, var8, var13 > 3.12 ? 1 : (var13 < 2.88 ? -1 : 0), var3, false, false);
                        } else {
                           this.body.drive(this.mc, var8, var13 < 1.6 ? -1 : 1, var13 <= 3.5 ? var3 * 0.35 : 0.0, var13 >= 1.6, false);
                           if (var29) {
                              this.strike(var8, var9, 0.95F, false);
                           }
                        }
                     } else {
                        if (var27) {
                           var5 = Math.max(var5, var30);
                        }

                        int var2 = var16 > var5 + 0.12 ? 1 : (var16 < var5 - 0.12 ? -1 : 0);
                        if (var2 < 0 && var24 > 0.15) {
                           var2 = 0;
                        }

                        boolean var34 = var28 && var29 && var19 >= 0.6F && !var26 && !var27;
                        boolean var1 = this.jumpReset.get() && var10 < this.resetUntil && var8.isOnGround() && !var8.isTouchingWater();
                        if (var1) {
                           this.body.drive(this.mc, var8, 1, 0.0, true, true);
                        } else if (var18 <= 2 && !var34 && this.strafe.get() && !var27) {
                           this.body.drive(this.mc, var8, 1, 1.0, true, false);
                        } else {
                           double var36 = !this.strafe.get() ? 0.0 : (var2 == 0 ? 1.0 : (var2 < 0 ? 0.8 : 0.5));
                           this.body.drive(this.mc, var8, var2, var36, var2 > 0, false);
                        }

                        if (var34) {
                           this.strike(var8, var9, 0.6F, false);
                        }
                     }
                  } else {
                     this.body.drive(this.mc, var8, 1, 0.0, true, false);
                     if (var29 && var18 <= 0 && !var26 && !var27) {
                        this.strike(var8, var9, 0.95F, false);
                     }
                  }
               }
            }
         }
      } else {
         this.body.forget();
      }
   }

   private boolean critWindow(int i, double d, double e, double f) {
      if (i >= 7 && i <= 10 && d >= 2.3 && d <= 5.0) {
         return true;
      } else {
         return i >= 3 && i <= 10 && !(d < 2.3) && !(d > 4.2) ? e >= 100.0 && f > 0.05 || this.style.is("Aggressive") && e > 60.0 : false;
      }
   }

   private void strike(ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity, float f, boolean bl) {
      if (!CombatMacroLock.heldByOther(this) && !MeleeAttacks.actedThisTick(clientPlayerEntity) && this.mc.attackCooldown <= 0) {
         if (!(MeleeAttacks.charge(clientPlayerEntity) < f) && MeleeAttacks.isMeleeWeapon(clientPlayerEntity.getMainHandStack())) {
            if (!bl || MeleeAttacks.airborne(clientPlayerEntity) && !(clientPlayerEntity.getVelocity().y >= 0.0)) {
               Rotation var6 = ServerRotation.sent();
               if (var6 != null) {
                  Vec3d var7 = clientPlayerEntity.getEyePos();
                  double var8 = Math.min(3.0, clientPlayerEntity.getEntityInteractionRange());
                  Box var10 = playerEntity.getBoundingBox();
                  Vec3d var5 = var10.contains(var7)
                     ? var7
                     : var10.raycast(var7, var7.add(Vec3d.fromPolar(var6.pitch(), var6.yaw()).multiply(var8))).orElse(null);
                  if (var5 != null && MeleeAttacks.lineIsClear(clientPlayerEntity, var7, var5)) {
                     if (MeleeAttacks.attack(this.mc, new EntityHitResult(playerEntity, var5))) {
                        MeleeAttacks.claimTick(this.mc);
                        this.jitter = randomJitter();
                        this.tapTicks = 1;
                        boolean var12 = this.tapBack = clientPlayerEntity.getEyePos().distanceTo(TargetingUtils.closestPoint(var7, var10)) < 2.3;
                        if (this.tapBack) {
                           this.tapTicks = 2;
                        }

                        this.critJump = false;
                     }
                  }
               }
            }
         }
      }
   }

   private static double facingAngle(PlayerEntity playerEntity, ClientPlayerEntity clientPlayerEntity) {
      Vec3d var2 = Vec3d.fromPolar(playerEntity.getPitch(), playerEntity.getHeadYaw());
      Vec3d var3 = clientPlayerEntity.getEntityPos().add(0.0, 1.1, 0.0).subtract(playerEntity.getEyePos()).normalize();
      return Math.toDegrees(Math.acos(MathHelper.clamp(var2.dotProduct(var3), -1.0, 1.0)));
   }

   private static int ping(ClientPlayerEntity clientPlayerEntity) {
      PlayerListEntry var1 = clientPlayerEntity.networkHandler.getPlayerListEntry(clientPlayerEntity.getUuid());
      return var1 == null ? 0 : Math.max(0, var1.getLatency());
   }

   private static double randomJitter() {
      return ThreadLocalRandom.current().nextDouble(-0.06, 0.06);
   }

   private static void holdWeapon(ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var1 = clientPlayerEntity.getInventory();
      if (!MeleeAttacks.isMeleeWeapon(clientPlayerEntity.getMainHandStack())) {
         int var2 = -1;
         double var3 = 0.0;

         for (int var5 = 0; var5 < 9; var5++) {
            ItemStack var6 = var1.getStack(var5);
            if (MeleeAttacks.isMeleeWeapon(var6)) {
               double var7 = MeleeAttacks.attackDamage(var6);
               if (var2 < 0 || var7 > var3) {
                  var2 = var5;
                  var3 = var7;
               }
            }
         }

         if (var2 >= 0) {
            var1.setSelectedSlot(var2);
         }
      }
   }
}
