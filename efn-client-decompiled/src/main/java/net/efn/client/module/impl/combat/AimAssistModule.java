package net.efn.client.module.impl.combat;

import java.util.concurrent.ThreadLocalRandom;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.combat.CombatTarget;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.efn.client.util.pvp.TargetingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class AimAssistModule extends ClientModule {
   private static final long LAST_HIT_MS = 1500L;
   private static final double STICK_RANGE = 0.5;
   private static final double STICK_FOV = 1.3;
   private static final long MOUSE_IDLE_MS = 150L;
   private static final double[] STILL = new double[]{0.0, 0.0};
   private static final long MINING_QUIET_MS = 250L;
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 4.0F, 3.0F, 6.0F).step(0.1F));
   private final NumberSetting fov = this.addSetting("fov", new NumberSetting("FOV", 60.0F, 10.0F, 180.0F).step(5.0F));
   private final NumberSetting speed = this.addSetting("speed", new NumberSetting("Speed", 60.0F, 10.0F, 180.0F).step(5.0F));
   private final BooleanSetting weaponsOnly = this.addSetting("weapons-only", new BooleanSetting("Weapons Only", true));
   private final BooleanSetting vertical = this.addSetting("vertical", new BooleanSetting("Vertical", true));
   private PlayerEntity target;
   private double pace;
   private double pendingYaw;
   private double pendingPitch;
   private long movedAt;
   private double userYawSign;
   private double userPitchSign;
   private long minedAt;

   public AimAssistModule() {
      super("aim-assist", "Aim Assist", "Eases your crosshair onto the nearest enemy player while you move the mouse.", ModuleCategory.COMBAT);
   }

   @Override
   public int settingsVersion() {
      return 1;
   }

   @Override
   public void migrateSettings(int i) {
      if (i < 1) {
         this.speed.set(60.0F);
      }
   }

   @Override
   protected void onDisable() {
      this.follow(null);
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var2.interactionManager != null && var2.interactionManager.isBreakingBlock()) {
         this.minedAt = Util.getMeasuringTimeMs();
      }

      if (MeleeAttacks.canAct(var2)
         && !var3.isUsingItem()
         && !var2.options.useKey.isPressed()
         && (!this.weaponsOnly.get() || MeleeAttacks.isMeleeWeapon(var3.getMainHandStack()))
         && !ServerRotation.pending()
         && !CombatMacroLock.heldByOther(this)
         && Util.getMeasuringTimeMs() - this.minedAt >= 250L) {
         this.follow(this.choose(var2, var3));
      } else {
         this.follow(null);
      }
   }

   public double[] nudge(ClientPlayerEntity clientPlayerEntity, double d, double e, double f) {
      long var10 = Util.getMeasuringTimeMs();
      if (d != 0.0 || e != 0.0) {
         this.movedAt = var10;
      }

      if (d != 0.0) {
         this.userYawSign = Math.signum(d);
      }

      if (e != 0.0) {
         this.userPitchSign = Math.signum(e);
      }

      Rotation var9 = this.target != null && var10 - this.movedAt <= 150L
         ? this.aim(clientPlayerEntity, clientPlayerEntity.getEyePos(), core(this.target))
         : null;
      if (var9 == null) {
         this.pendingYaw = 0.0;
         this.pendingPitch = 0.0;
         this.userYawSign = 0.0;
         this.userPitchSign = 0.0;
         return STILL;
      } else {
         double var13 = Math.min(f, 0.05);
         float var15 = MathHelper.wrapDegrees(var9.yaw() - clientPlayerEntity.getYaw());
         float var8 = this.vertical.get() ? var9.pitch() - clientPlayerEntity.getPitch() : 0.0F;
         if (this.userYawSign * var15 < 0.0) {
            var15 = 0.0F;
         }

         if (this.userPitchSign * var8 < 0.0) {
            var8 = 0.0F;
         }

         double var17 = this.speed.get() * this.pace * var13;
         double var19 = 1.0 - Math.exp(-12.0 * var13);
         double var21 = RotationUtils.mouseScale(MinecraftClient.getInstance(), clientPlayerEntity);
         double var23 = (float)var21 * 0.15F;
         this.pendingYaw = this.pendingYaw + MathHelper.clamp(var15 * var19, -var17, var17) / var23;
         this.pendingPitch = this.pendingPitch + MathHelper.clamp(var8 * var19 * 0.6, -var17, var17) / var23;
         long var25 = (long)this.pendingYaw;
         long var27 = (long)this.pendingPitch;
         this.pendingYaw -= var25;
         this.pendingPitch -= var27;
         return new double[]{var25 * var21, var27 * var21};
      }
   }

   private void follow(PlayerEntity playerEntity) {
      if (playerEntity != this.target) {
         this.target = playerEntity;
         this.pace = ThreadLocalRandom.current().nextDouble(0.85, 1.15);
         this.pendingYaw = 0.0;
         this.pendingPitch = 0.0;
      }
   }

   private PlayerEntity choose(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      Vec3d var4 = clientPlayerEntity.getEyePos();
      double var5 = this.fov.get() / 2.0;
      LivingEntity var7 = CombatTarget.lastHit(1500L);
      PlayerEntity var3;
      if (var7 instanceof PlayerEntity && this.offAngle(clientPlayerEntity, var4, var3 = (PlayerEntity)var7, this.range.get()) <= var5) {
         return var3;
      } else if (this.target != null
         && this.target.getEntityWorld() == clientPlayerEntity.getEntityWorld()
         && this.offAngle(clientPlayerEntity, var4, this.target, this.range.get() + 0.5) <= var5 * 1.3) {
         return this.target;
      } else {
         PlayerEntity var8 = null;
         double var9 = var5;

         for (PlayerEntity var12 : minecraftClient.world.getPlayers()) {
            double var13 = this.offAngle(clientPlayerEntity, var4, var12, this.range.get());
            if (!(var13 > var9)) {
               var8 = var12;
               var9 = var13;
            }
         }

         return var8;
      }
   }

   private double offAngle(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, PlayerEntity playerEntity, double d) {
      if (MeleeAttacks.isEnemy(clientPlayerEntity, playerEntity) && (!playerEntity.isInvisible() || armourShows(playerEntity))) {
         if (vec3d.distanceTo(TargetingUtils.closestPoint(vec3d, playerEntity.getBoundingBox())) > d) {
            return 1.7976931348623157E308;
         } else {
            Box var6 = core(playerEntity);
            if (!MeleeAttacks.lineIsClear(clientPlayerEntity, vec3d, var6.getCenter())) {
               return 1.7976931348623157E308;
            } else {
               Rotation var7 = this.aim(clientPlayerEntity, vec3d, var6);
               return var7 == null ? 0.0 : RotationUtils.angleTo(RotationUtils.entityRotation(clientPlayerEntity), var7);
            }
         }
      } else {
         return 1.7976931348623157E308;
      }
   }

   private Rotation aim(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, Box box) {
      Vec3d var4 = clientPlayerEntity.getRotationVec(1.0F);
      if (box.raycast(vec3d, vec3d.add(var4.multiply(this.range.get() + 1.0))).isPresent()) {
         return null;
      } else {
         Vec3d var5 = TargetingUtils.closestPoint(vec3d.add(var4.multiply(vec3d.distanceTo(box.getCenter()))), box);
         return RotationUtils.fromTo(vec3d, var5);
      }
   }

   private static boolean armourShows(PlayerEntity playerEntity) {
      for (EquipmentSlot var2 : EquipmentSlot.VALUES) {
         if (var2.isArmorSlot() && !playerEntity.getEquippedStack(var2).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private static Box core(PlayerEntity playerEntity) {
      Box var1 = playerEntity.getBoundingBox();
      return var1.contract(var1.getLengthX() * 0.25, var1.getLengthY() * 0.1, var1.getLengthX() * 0.25);
   }
}
