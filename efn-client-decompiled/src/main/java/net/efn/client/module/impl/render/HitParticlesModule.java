package net.efn.client.module.impl.render;

import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.gui.ThemeTint;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public final class HitParticlesModule extends ClientModule {
   private static final String TYPE_CRIT = "Crit";
   private static final String TYPE_MAGIC_CRIT = "Magic Crit";
   private static final String TYPE_SPARK = "Spark";
   private static final String TYPE_FLAME = "Flame";
   private static final String TYPE_SOUL_FLAME = "Soul Flame";
   private static final String TYPE_END_ROD = "End Rod";
   private static final String TYPE_TOTEM = "Totem";
   private static final String TYPE_HEART = "Heart";
   private static final String TYPE_DUST = "Dust";
   private static final String ORIGIN_IMPACT = "Impact";
   private static final String ORIGIN_BODY = "Body";
   private static final String TARGETS_LIVING = "Living";
   private static final String TARGETS_PLAYERS = "Players";
   private static final String TARGETS_ALL = "All";
   private static final String COLOR_CUSTOM = "Custom";
   private static final String COLOR_ACCENT = "Accent";
   private static final double LIFT = 0.25;
   private static final int PEEP_DUST = 16743112;
   private final ModeSetting type = this.addSetting(
      "type", new ModeSetting("Type", "Crit", "Crit", "Magic Crit", "Spark", "Flame", "Soul Flame", "End Rod", "Totem", "Heart", "Dust")
   );
   private final NumberSetting amount = this.addSetting("amount", new NumberSetting("Amount", 10.0F, 1.0F, 40.0F).step(1.0F));
   private final NumberSetting spread = this.addSetting("spread", new NumberSetting("Spread", 1.0F, 0.25F, 3.0F).step(0.25F));
   private final ModeSetting origin = this.addSetting("origin", new ModeSetting("Origin", "Impact", "Impact", "Body"));
   private final BooleanSetting onlyCrits = this.addSetting("only-crits", new BooleanSetting("Only Crits", false));
   private final ModeSetting targets = this.addSetting("targets", new ModeSetting("Targets", "Living", "Living", "Players", "All"));
   private final ModeSetting dustColorMode = this.addSetting("dust-color-mode", new ModeSetting("Color", "Accent", "Custom", "Accent"));
   private final ColorSetting dustColor = this.addSetting("dust-color", new ColorSetting("Custom Color", -657929));
   private final NumberSetting dustSize = this.addSetting("dust-size", new NumberSetting("Dust Size", 1.0F, 0.5F, 2.0F).step(0.1F));
   private final Random random = Random.create();

   public HitParticlesModule() {
      super("hit-particles", "Hit Particles", "Spawns particles where your hits land.", ModuleCategory.VISUALS);
      this.dustColorMode.visibleWhen(() -> this.type.is("Dust"));
      this.dustColor.visibleWhen(() -> this.type.is("Dust") && this.dustColorMode.is("Custom"));
      this.dustSize.visibleWhen(() -> this.type.is("Dust"));
   }

   @Override
   protected void onEnable() {
      this.on(PlayerAttackEntityEvent.TYPE, Stage.PRE, EventPriority.LOWEST, this::onAttack);
   }

   private void onAttack(PlayerAttackEntityEvent playerAttackEntityEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.isOnThread()) {
         ClientPlayerEntity var3 = var2.player;
         ClientWorld var4 = var2.world;
         Entity var5 = playerAttackEntityEvent.target;
         if (var3 != null && var4 != null && var5 != null && var5 != var3) {
            if (var5.isAttackable() && this.accepts(var5)) {
               if (!this.onlyCrits.get() || isCritical(var3, var5)) {
                  this.spawnBurst(var2, var4, var3, var5);
               }
            }
         }
      }
   }

   private boolean accepts(Entity entity) {
      String var2 = this.targets.get();

      return switch (var2) {
         case "Players" -> entity instanceof PlayerEntity;
         case "All" -> true;
         default -> entity instanceof LivingEntity;
      };
   }

   private static boolean isCritical(ClientPlayerEntity clientPlayerEntity, Entity entity) {
      return clientPlayerEntity.getAttackCooldownProgress(0.5F) > 0.9F
         && clientPlayerEntity.fallDistance > 0.0
         && !clientPlayerEntity.isOnGround()
         && !clientPlayerEntity.isClimbing()
         && !clientPlayerEntity.isTouchingWater()
         && !clientPlayerEntity.hasBlindnessEffect()
         && !clientPlayerEntity.hasVehicle()
         && !clientPlayerEntity.isSprinting()
         && entity instanceof LivingEntity;
   }

   private void spawnBurst(MinecraftClient minecraftClient, ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, Entity entity) {
      HitParticlesModule.Burst var5 = this.burst();
      Box var6 = entity.getBoundingBox();
      Vec3d var7 = var6.getCenter();
      boolean var8 = this.origin.is("Body");
      Vec3d var9 = var8 ? null : impactPoint(minecraftClient, clientPlayerEntity, entity, var6);
      Vec3d var10 = clientPlayerEntity.getRotationVector().negate();
      double var11 = this.spread.get();
      double var13 = var5.jitter() * var11;
      int var15 = this.amount.getInt();
      float var16 = this.type.is("Dust") ? 0.0F : ThemeTint.world();
      DustParticleEffect var17 = var16 > 0.0F ? new DustParticleEffect(16743112, 0.9F) : null;

      for (int var18 = 0; var18 < var15; var18++) {
         Vec3d var19 = var8 ? this.randomPointIn(var6) : var9;
         Vec3d var20 = var19.subtract(var7);
         var20 = var20.lengthSquared() < 1.0E-6 ? var10 : var20.normalize();
         Vec3d var21 = var20.multiply(0.8).add(this.randomUnit()).add(0.0, 0.25, 0.0).normalize();
         double var22 = var5.speed() * var11 * (0.6 + 0.4 * this.random.nextDouble());
         Object var24 = var17 != null && this.random.nextFloat() < var16 ? var17 : var5.particle();
         clientWorld.addParticleClient(
            (ParticleEffect)var24,
            true,
            false,
            var19.x + this.random.nextGaussian() * var13,
            var19.y + this.random.nextGaussian() * var13,
            var19.z + this.random.nextGaussian() * var13,
            var21.x * var22,
            var21.y * var22,
            var21.z * var22
         );
      }
   }

   private HitParticlesModule.Burst burst() {
      String var1 = this.type.get();

      return switch (var1) {
         case "Magic Crit" -> new HitParticlesModule.Burst(ParticleTypes.ENCHANTED_HIT, 0.8, 0.1);
         case "Spark" -> new HitParticlesModule.Burst(ParticleTypes.ELECTRIC_SPARK, 0.16, 0.1);
         case "Flame" -> new HitParticlesModule.Burst(ParticleTypes.FLAME, 0.035, 0.12);
         case "Soul Flame" -> new HitParticlesModule.Burst(ParticleTypes.SOUL_FIRE_FLAME, 0.035, 0.12);
         case "End Rod" -> new HitParticlesModule.Burst(ParticleTypes.END_ROD, 0.09, 0.1);
         case "Totem" -> new HitParticlesModule.Burst(ParticleTypes.TOTEM_OF_UNDYING, 0.45, 0.1);
         case "Heart" -> new HitParticlesModule.Burst(ParticleTypes.HEART, 0.0, 0.35);
         case "Dust" -> new HitParticlesModule.Burst(new DustParticleEffect(this.dustRgb(), this.dustSize.get()), 1.0, 0.3);
         default -> new HitParticlesModule.Burst(ParticleTypes.CRIT, 0.8, 0.1);
      };
   }

   private int dustRgb() {
      int var1 = this.dustColorMode.is("Accent") ? CosmeticAccent.argb() : ThemeTint.world(this.dustColor.argb());
      return var1 & 16777215;
   }

   private static Vec3d impactPoint(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, Entity entity, Box box) {
      HitResult var5 = minecraftClient.crosshairTarget;
      EntityHitResult var4;
      if (var5 instanceof EntityHitResult && (var4 = (EntityHitResult)var5).getEntity() == entity) {
         return var4.getPos();
      } else {
         Vec3d var6 = clientPlayerEntity.getEyePos();
         return new Vec3d(
            MathHelper.clamp(var6.x, box.minX, box.maxX), MathHelper.clamp(var6.y, box.minY, box.maxY), MathHelper.clamp(var6.z, box.minZ, box.maxZ)
         );
      }
   }

   private Vec3d randomPointIn(Box box) {
      return new Vec3d(
         box.minX + box.getLengthX() * (0.1 + 0.8 * this.random.nextDouble()),
         box.minY + box.getLengthY() * (0.1 + 0.8 * this.random.nextDouble()),
         box.minZ + box.getLengthZ() * (0.1 + 0.8 * this.random.nextDouble())
      );
   }

   private Vec3d randomUnit() {
      Vec3d var1 = new Vec3d(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian());
      return var1.lengthSquared() < 1.0E-9 ? new Vec3d(0.0, 1.0, 0.0) : var1.normalize();
   }

   @Environment(EnvType.CLIENT)
   private record Burst(ParticleEffect particle, double speed, double jitter) {
   }
}
