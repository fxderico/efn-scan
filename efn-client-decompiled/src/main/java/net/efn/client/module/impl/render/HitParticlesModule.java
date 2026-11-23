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
   private final Random random = Random.method_43047();

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

   private void onAttack(PlayerAttackEntityEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.method_18854()) {
         ClientPlayerEntity var3 = var2.field_1724;
         ClientWorld var4 = var2.field_1687;
         Entity var5 = var1.target;
         if (var3 != null && var4 != null && var5 != null && var5 != var3) {
            if (var5.method_5732() && this.accepts(var5)) {
               if (!this.onlyCrits.get() || isCritical(var3, var5)) {
                  this.spawnBurst(var2, var4, var3, var5);
               }
            }
         }
      }
   }

   private boolean accepts(Entity var1) {
      String var2 = this.targets.get();

      return switch (var2) {
         case "Players" -> var1 instanceof PlayerEntity;
         case "All" -> true;
         default -> var1 instanceof LivingEntity;
      };
   }

   private static boolean isCritical(ClientPlayerEntity var0, Entity var1) {
      return var0.method_7261(0.5F) > 0.9F
         && var0.field_6017 > 0.0
         && !var0.method_24828()
         && !var0.method_6101()
         && !var0.method_5799()
         && !var0.method_74025()
         && !var0.method_5765()
         && !var0.method_5624()
         && var1 instanceof LivingEntity;
   }

   private void spawnBurst(MinecraftClient var1, ClientWorld var2, ClientPlayerEntity var3, Entity var4) {
      HitParticlesModule.Burst var5 = this.burst();
      Box var6 = var4.method_5829();
      Vec3d var7 = var6.method_1005();
      boolean var8 = this.origin.is("Body");
      Vec3d var9 = var8 ? null : impactPoint(var1, var3, var4, var6);
      Vec3d var10 = var3.method_5720().method_22882();
      double var11 = this.spread.get();
      double var13 = var5.jitter() * var11;
      int var15 = this.amount.getInt();
      float var16 = this.type.is("Dust") ? 0.0F : ThemeTint.world();
      DustParticleEffect var17 = var16 > 0.0F ? new DustParticleEffect(16743112, 0.9F) : null;

      for (int var18 = 0; var18 < var15; var18++) {
         Vec3d var19 = var8 ? this.randomPointIn(var6) : var9;
         Vec3d var20 = var19.method_1020(var7);
         var20 = var20.method_1027() < 1.0E-6 ? var10 : var20.method_1029();
         Vec3d var21 = var20.method_1021(0.8).method_1019(this.randomUnit()).method_1031(0.0, 0.25, 0.0).method_1029();
         double var22 = var5.speed() * var11 * (0.6 + 0.4 * this.random.method_43058());
         Object var24 = var17 != null && this.random.method_43057() < var16 ? var17 : var5.particle();
         var2.method_8466(
            (ParticleEffect)var24,
            true,
            false,
            var19.field_1352 + this.random.method_43059() * var13,
            var19.field_1351 + this.random.method_43059() * var13,
            var19.field_1350 + this.random.method_43059() * var13,
            var21.field_1352 * var22,
            var21.field_1351 * var22,
            var21.field_1350 * var22
         );
      }
   }

   private HitParticlesModule.Burst burst() {
      String var1 = this.type.get();

      return switch (var1) {
         case "Magic Crit" -> new HitParticlesModule.Burst(ParticleTypes.field_11208, 0.8, 0.1);
         case "Spark" -> new HitParticlesModule.Burst(ParticleTypes.field_29644, 0.16, 0.1);
         case "Flame" -> new HitParticlesModule.Burst(ParticleTypes.field_11240, 0.035, 0.12);
         case "Soul Flame" -> new HitParticlesModule.Burst(ParticleTypes.field_22246, 0.035, 0.12);
         case "End Rod" -> new HitParticlesModule.Burst(ParticleTypes.field_11207, 0.09, 0.1);
         case "Totem" -> new HitParticlesModule.Burst(ParticleTypes.field_11220, 0.45, 0.1);
         case "Heart" -> new HitParticlesModule.Burst(ParticleTypes.field_11201, 0.0, 0.35);
         case "Dust" -> new HitParticlesModule.Burst(new DustParticleEffect(this.dustRgb(), this.dustSize.get()), 1.0, 0.3);
         default -> new HitParticlesModule.Burst(ParticleTypes.field_11205, 0.8, 0.1);
      };
   }

   private int dustRgb() {
      int var1 = this.dustColorMode.is("Accent") ? CosmeticAccent.argb() : ThemeTint.world(this.dustColor.argb());
      return var1 & 16777215;
   }

   private static Vec3d impactPoint(MinecraftClient var0, ClientPlayerEntity var1, Entity var2, Box var3) {
      HitResult var5 = var0.field_1765;
      EntityHitResult var4;
      if (var5 instanceof EntityHitResult && (var4 = (EntityHitResult)var5).method_17782() == var2) {
         return var4.method_17784();
      } else {
         Vec3d var6 = var1.method_33571();
         return new Vec3d(
            MathHelper.method_15350(var6.field_1352, var3.field_1323, var3.field_1320),
            MathHelper.method_15350(var6.field_1351, var3.field_1322, var3.field_1325),
            MathHelper.method_15350(var6.field_1350, var3.field_1321, var3.field_1324)
         );
      }
   }

   private Vec3d randomPointIn(Box var1) {
      return new Vec3d(
         var1.field_1323 + var1.method_17939() * (0.1 + 0.8 * this.random.method_43058()),
         var1.field_1322 + var1.method_17940() * (0.1 + 0.8 * this.random.method_43058()),
         var1.field_1321 + var1.method_17941() * (0.1 + 0.8 * this.random.method_43058())
      );
   }

   private Vec3d randomUnit() {
      Vec3d var1 = new Vec3d(this.random.method_43059(), this.random.method_43059(), this.random.method_43059());
      return var1.method_1027() < 1.0E-9 ? new Vec3d(0.0, 1.0, 0.0) : var1.method_1029();
   }

   @Environment(EnvType.CLIENT)
   private record Burst(ParticleEffect particle, double speed, double jitter) {
   }
}
