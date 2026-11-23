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
   public void migrateSettings(int var1) {
      if (var1 < 1) {
         this.speed.set(60.0F);
      }
   }

   @Override
   protected void onDisable() {
      this.follow(null);
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var2.field_1761 != null && var2.field_1761.method_2923()) {
         this.minedAt = Util.method_658();
      }

      if (MeleeAttacks.canAct(var2)
         && !var3.method_6115()
         && !var2.field_1690.field_1904.method_1434()
         && (!this.weaponsOnly.get() || MeleeAttacks.isMeleeWeapon(var3.method_6047()))
         && !ServerRotation.pending()
         && !CombatMacroLock.heldByOther(this)
         && Util.method_658() - this.minedAt >= 250L) {
         this.follow(this.choose(var2, var3));
      } else {
         this.follow(null);
      }
   }

   public double[] nudge(ClientPlayerEntity var1, double var2, double var4, double var6) {
      long var10 = Util.method_658();
      if (var2 != 0.0 || var4 != 0.0) {
         this.movedAt = var10;
      }

      if (var2 != 0.0) {
         this.userYawSign = Math.signum(var2);
      }

      if (var4 != 0.0) {
         this.userPitchSign = Math.signum(var4);
      }

      Rotation var9 = this.target != null && var10 - this.movedAt <= 150L ? this.aim(var1, var1.method_33571(), core(this.target)) : null;
      if (var9 == null) {
         this.pendingYaw = 0.0;
         this.pendingPitch = 0.0;
         this.userYawSign = 0.0;
         this.userPitchSign = 0.0;
         return STILL;
      } else {
         double var13 = Math.min(var6, 0.05);
         float var15 = MathHelper.method_15393(var9.yaw() - var1.method_36454());
         float var8 = this.vertical.get() ? var9.pitch() - var1.method_36455() : 0.0F;
         if (this.userYawSign * var15 < 0.0) {
            var15 = 0.0F;
         }

         if (this.userPitchSign * var8 < 0.0) {
            var8 = 0.0F;
         }

         double var17 = this.speed.get() * this.pace * var13;
         double var19 = 1.0 - Math.exp(-12.0 * var13);
         double var21 = RotationUtils.mouseScale(MinecraftClient.method_1551(), var1);
         double var23 = (float)var21 * 0.15F;
         this.pendingYaw = this.pendingYaw + MathHelper.method_15350(var15 * var19, -var17, var17) / var23;
         this.pendingPitch = this.pendingPitch + MathHelper.method_15350(var8 * var19 * 0.6, -var17, var17) / var23;
         long var25 = (long)this.pendingYaw;
         long var27 = (long)this.pendingPitch;
         this.pendingYaw -= var25;
         this.pendingPitch -= var27;
         return new double[]{var25 * var21, var27 * var21};
      }
   }

   private void follow(PlayerEntity var1) {
      if (var1 != this.target) {
         this.target = var1;
         this.pace = ThreadLocalRandom.current().nextDouble(0.85, 1.15);
         this.pendingYaw = 0.0;
         this.pendingPitch = 0.0;
      }
   }

   private PlayerEntity choose(MinecraftClient var1, ClientPlayerEntity var2) {
      Vec3d var4 = var2.method_33571();
      double var5 = this.fov.get() / 2.0;
      LivingEntity var7 = CombatTarget.lastHit(1500L);
      PlayerEntity var3;
      if (var7 instanceof PlayerEntity && this.offAngle(var2, var4, var3 = (PlayerEntity)var7, this.range.get()) <= var5) {
         return var3;
      } else if (this.target != null
         && this.target.method_73183() == var2.method_73183()
         && this.offAngle(var2, var4, this.target, this.range.get() + 0.5) <= var5 * 1.3) {
         return this.target;
      } else {
         PlayerEntity var8 = null;
         double var9 = var5;

         for (PlayerEntity var12 : var1.field_1687.method_18456()) {
            double var13 = this.offAngle(var2, var4, var12, this.range.get());
            if (!(var13 > var9)) {
               var8 = var12;
               var9 = var13;
            }
         }

         return var8;
      }
   }

   private double offAngle(ClientPlayerEntity var1, Vec3d var2, PlayerEntity var3, double var4) {
      if (MeleeAttacks.isEnemy(var1, var3) && (!var3.method_5767() || armourShows(var3))) {
         if (var2.method_1022(TargetingUtils.closestPoint(var2, var3.method_5829())) > var4) {
            return 1.7976931348623157E308;
         } else {
            Box var6 = core(var3);
            if (!MeleeAttacks.lineIsClear(var1, var2, var6.method_1005())) {
               return 1.7976931348623157E308;
            } else {
               Rotation var7 = this.aim(var1, var2, var6);
               return var7 == null ? 0.0 : RotationUtils.angleTo(RotationUtils.entityRotation(var1), var7);
            }
         }
      } else {
         return 1.7976931348623157E308;
      }
   }

   private Rotation aim(ClientPlayerEntity var1, Vec3d var2, Box var3) {
      Vec3d var4 = var1.method_5828(1.0F);
      if (var3.method_992(var2, var2.method_1019(var4.method_1021(this.range.get() + 1.0))).isPresent()) {
         return null;
      } else {
         Vec3d var5 = TargetingUtils.closestPoint(var2.method_1019(var4.method_1021(var2.method_1022(var3.method_1005()))), var3);
         return RotationUtils.fromTo(var2, var5);
      }
   }

   private static boolean armourShows(PlayerEntity var0) {
      for (EquipmentSlot var2 : EquipmentSlot.field_54086) {
         if (var2.method_46643() && !var0.method_6118(var2).method_7960()) {
            return true;
         }
      }

      return false;
   }

   private static Box core(PlayerEntity var0) {
      Box var1 = var0.method_5829();
      return var1.method_35580(var1.method_17939() * 0.25, var1.method_17940() * 0.1, var1.method_17939() * 0.25);
   }
}
