package net.efn.client.module.impl.render;

import java.util.function.Consumer;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.MobilePerformance;
import net.efn.client.util.combat.TotemPops;
import net.efn.client.util.glyph.GlyphCanvas;
import net.efn.client.util.glyph.GlyphSwarm;
import net.efn.client.util.glyph.GlyphTint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public final class IconParticlesModule extends ClientModule {
   private static final String GLYPH_BOLT = "Bolt";
   private static final String GLYPH_GEM = "Gem";
   private static final String GLYPH_DUST = "Dust";
   private static final String GLYPH_SPARK = "Spark";
   private static final String GLYPH_MIXED = "Mixed";
   private static final int CAPACITY = 12000 / GlyphSwarm.WORST_QUADS / 10 * 10;
   private static final float BASE_SIZE = 0.085F;
   private static final double PROJECTILE_RANGE_SQR = 6400.0;
   private static final double RESTING_SQR = 9.0E-4;
   private static final double STEP_MIN_SPEED = 0.07;
   private static final int PEARL = 0;
   private static final int ARROW = 1;
   private static final int TRIDENT = 2;
   private static final int NONE = -1;
   private static final byte[] MIXED_ORDER = new byte[]{0, 3, 1, 3, 2, 3};
   private final BooleanSetting pops = this.addSetting("pops", new BooleanSetting("Totem Pops", true));
   private final ModeSetting popGlyph = this.addSetting("pop-glyph", new ModeSetting("Pop Glyph", "Mixed", "Mixed", "Bolt", "Gem", "Dust", "Spark"));
   private final NumberSetting popBurst = this.addSetting("pop-burst", new NumberSetting("Pop Burst", 26.0F, 4.0F, 80.0F).step(1.0F));
   private final BooleanSetting ownPops = this.addSetting("own-pops", new BooleanSetting("Own Pops", true));
   private final BooleanSetting projectiles = this.addSetting("projectiles", new BooleanSetting("Projectiles", true));
   private final BooleanSetting pearls = this.addSetting("pearls", new BooleanSetting("Pearls", true));
   private final BooleanSetting arrows = this.addSetting("arrows", new BooleanSetting("Arrows", true));
   private final BooleanSetting tridents = this.addSetting("tridents", new BooleanSetting("Tridents", true));
   private final ModeSetting trailGlyph = this.addSetting("trail-glyph", new ModeSetting("Trail Glyph", "Spark", "Spark", "Dust", "Bolt", "Gem", "Mixed"));
   private final NumberSetting trailDensity = this.addSetting("trail-density", new NumberSetting("Trail Density", 2.0F, 1.0F, 6.0F).step(1.0F));
   private final BooleanSetting footsteps = this.addSetting("footsteps", new BooleanSetting("Footsteps", false));
   private final ModeSetting stepGlyph = this.addSetting("step-glyph", new ModeSetting("Step Glyph", "Dust", "Dust", "Spark", "Bolt", "Gem", "Mixed"));
   private final NumberSetting stepDensity = this.addSetting("step-density", new NumberSetting("Step Density", 2.0F, 1.0F, 6.0F).step(1.0F));
   private final NumberSetting size = this.addSetting("size", new NumberSetting("Size", 1.0F, 0.3F, 2.5F).step(0.05F));
   private final NumberSetting lifetime = this.addSetting("lifetime", new NumberSetting("Lifetime", 1.4F, 0.3F, 4.0F).step(0.1F));
   private final NumberSetting limit = this.addSetting("limit", new NumberSetting("Limit", 240.0F, 40.0F, CAPACITY).step(10.0F));
   private final BooleanSetting halo = this.addSetting("halo", new BooleanSetting("Halo", true));
   private final ModeSetting colorMode = this.addSetting("color-mode", new ModeSetting("Color", "Theme", "Theme", "Manual"));
   private final ColorSetting mainColor = this.addSetting("main-color", new ColorSetting("Main Color", -12548362));
   private final ColorSetting secondColor = this.addSetting("second-color", new ColorSetting("Second Color", -14992226));
   private final GlyphSwarm swarm = new GlyphSwarm(CAPACITY);
   private final GlyphCanvas canvas = new GlyphCanvas();
   private final GlyphTint palette = new GlyphTint();
   private final Random random = Random.method_43047();
   private final Consumer<LivingEntity> popSink = this::burst;
   private ClientWorld boundLevel;
   private long lastFrame;

   public IconParticlesModule() {
      super("particles", "Particles", "EFN particles for totem pops, projectiles and footsteps.", ModuleCategory.VISUALS);
      this.popGlyph.visibleWhen(this.pops::get);
      this.popBurst.visibleWhen(this.pops::get);
      this.ownPops.visibleWhen(this.pops::get);
      this.pearls.visibleWhen(this.projectiles::get);
      this.arrows.visibleWhen(this.projectiles::get);
      this.tridents.visibleWhen(this.projectiles::get);
      this.trailGlyph.visibleWhen(this.projectiles::get);
      this.trailDensity.visibleWhen(this.projectiles::get);
      this.stepGlyph.visibleWhen(this.footsteps::get);
      this.stepDensity.visibleWhen(this.footsteps::get);
      this.mainColor.visibleWhen(() -> this.colorMode.is("Manual"));
      this.secondColor.visibleWhen(() -> this.colorMode.is("Manual"));
   }

   @Override
   protected void onEnable() {
      this.reset();
      this.on(PacketReceiveEvent.TYPE, this::onPacket);
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      this.on(Render3DEvent.TYPE, this::onRender);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.swarm.clear();
      this.boundLevel = null;
      this.lastFrame = 0L;
   }

   private boolean bind(ClientWorld var1) {
      if (var1 == null) {
         if (this.boundLevel != null) {
            this.reset();
         }

         return false;
      } else {
         if (var1 != this.boundLevel) {
            this.swarm.clear();
            this.boundLevel = var1;
         }

         return true;
      }
   }

   private void onPacket(PacketReceiveEvent var1) {
      if (this.pops.get()) {
         TotemPops.scan(var1.packet, this.popSink);
      }
   }

   private void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (this.bind(var2.field_1687) && var3 != null) {
         this.refreshPalette();
         if (this.projectiles.get()) {
            this.trails(var2.field_1687, var3);
         }

         if (this.footsteps.get()) {
            this.steps(var3);
         }
      }
   }

   private void onRender(Render3DEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (this.bind(var2.field_1687)) {
         long var3 = System.nanoTime();
         float var5 = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(var3 - this.lastFrame) / 1.0E9F);
         this.lastFrame = var3;
         if (var2.method_1493()) {
            var5 = 0.0F;
         }

         this.swarm.trim(this.particleLimit());
         this.swarm.step(var5);
         if (this.swarm.count() != 0) {
            this.canvas.begin(var2.field_1773.method_19418());
            this.swarm.draw(this.canvas, 1.0F, this.halo.get());
            this.canvas.submit(false);
         }
      }
   }

   private void burst(LivingEntity var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (this.bind(var2.field_1687) && var1.method_73183() == var2.field_1687) {
         if (var1 != var2.field_1724 || this.ownPops.get()) {
            this.refreshPalette();
            Box var3 = var1.method_5829();
            int var4 = MobilePerformance.particles(this.popBurst.getInt(), 6);
            String var5 = this.popGlyph.get();

            for (int var6 = 0; var6 < var4; var6++) {
               byte var7 = pick(var5, var6);
               double var8 = var3.field_1323 + var3.method_17939() * this.random.method_43058();
               double var10 = var3.field_1322 + var3.method_17940() * (0.15 + 0.8 * this.random.method_43058());
               double var12 = var3.field_1321 + var3.method_17941() * this.random.method_43058();
               float var14 = this.random.method_43057() * 6.2831855F;
               float var15 = outward(var7) * (0.45F + this.random.method_43057());
               float var16 = upward(var7) * (0.55F + 0.8F * this.random.method_43057());
               this.emit(
                  var7,
                  var8,
                  var10,
                  var12,
                  MathHelper.method_15362(var14) * var15,
                  var16,
                  MathHelper.method_15374(var14) * var15,
                  1.0F,
                  1.0F,
                  this.colorFor(var7, -1)
               );
            }
         }
      }
   }

   private void trails(ClientWorld var1, ClientPlayerEntity var2) {
      int var3 = MobilePerformance.particles(this.trailDensity.getInt(), 1);
      String var4 = this.trailGlyph.get();

      for (Entity var6 : var1.method_18112()) {
         int var13 = this.role(var6);
         double var7;
         double var9;
         double var11;
         if (var13 != -1
            && !(
               (var11 = var6.method_23317() - var6.field_6014) * var11
                     + (var9 = var6.method_23318() - var6.field_6036) * var9
                     + (var7 = var6.method_23321() - var6.field_5969) * var7
                  < 9.0E-4
            )
            && !(var6.method_5858(var2) > 6400.0)) {
            double var14 = var6.method_17682() * 0.5;

            for (int var16 = 0; var16 < var3; var16++) {
               float var17 = (var16 + this.random.method_43057()) / var3;
               byte var18 = pick(var4, var6.field_6012 * var3 + var16);
               this.emit(
                  var18,
                  var6.field_6014 + var11 * var17 + this.jitter(0.06F),
                  var6.field_6036 + var9 * var17 + var14 + this.jitter(0.06F),
                  var6.field_5969 + var7 * var17 + this.jitter(0.06F),
                  this.jitter(0.3F),
                  this.jitter(0.3F) + 0.15F,
                  this.jitter(0.3F),
                  0.55F,
                  0.8F,
                  this.colorFor(var18, var13)
               );
            }
         }
      }
   }

   private void steps(ClientPlayerEntity var1) {
      if (var1.method_24828() && !var1.method_7325() && !var1.method_5765() && !var1.method_52535()) {
         double var4 = var1.method_23317() - var1.field_6014;
         double var2;
         double var6 = Math.sqrt(var4 * var4 + (var2 = var1.method_23321() - var1.field_5969) * var2);
         if (!(var6 < 0.07)) {
            int var8 = MobilePerformance.particles(this.stepDensity.getInt() + (var1.method_5624() ? 1 : 0), 1);
            String var9 = this.stepGlyph.get();

            for (int var10 = 0; var10 < var8; var10++) {
               byte var11 = pick(var9, var1.field_6012 * var8 + var10);
               double var12 = 0.2 + 0.3 * this.random.method_43058();
               this.emit(
                  var11,
                  var1.method_23317() - var4 / var6 * var12 + this.jitter(0.22F),
                  var1.method_23318() + 0.04,
                  var1.method_23321() - var2 / var6 * var12 + this.jitter(0.22F),
                  (float)(-var4 * 7.0) + this.jitter(0.5F),
                  0.8F + this.random.method_43057() * 1.1F,
                  (float)(-var2 * 7.0) + this.jitter(0.5F),
                  0.7F,
                  0.75F,
                  this.colorFor(var11, -1)
               );
            }
         }
      }
   }

   private void emit(byte var1, double var2, double var4, double var6, float var8, float var9, float var10, float var11, float var12, int var13) {
      float var14 = this.lifetime.get() * var11 * (0.75F + 0.5F * this.random.method_43057());
      float var15 = 0.085F * this.size.get() * var12 * sizeOf(var1) * (0.8F + 0.4F * this.random.method_43057());
      float var16 = spinOf(var1) * (this.random.method_43056() ? 1.0F : -1.0F) * (0.5F + this.random.method_43057());
      float var17 = var1 == 0 ? this.jitter(0.35F) : this.random.method_43057() * 6.2831855F;
      this.swarm.spawn(var1, var2, var4, var6, var8, var9, var10, var14, var15, var17, var16, this.random.method_43057(), var13, this.particleLimit());
   }

   private int particleLimit() {
      return MobilePerformance.particles(this.limit.getInt(), 40);
   }

   private void refreshPalette() {
      this.palette.resolve(this.colorMode.get(), CosmeticAccent.argb(), this.mainColor.argb(), this.secondColor.argb());
   }

   private int role(Entity var1) {
      if (var1.method_31481()) {
         return -1;
      } else if (var1 instanceof EnderPearlEntity) {
         return this.pearls.get() ? 0 : -1;
      } else if (var1 instanceof TridentEntity) {
         return this.tridents.get() ? 2 : -1;
      } else if (var1 instanceof PersistentProjectileEntity) {
         return this.arrows.get() ? 1 : -1;
      } else {
         return -1;
      }
   }

   private static byte pick(String var0, int var1) {
      return switch (var0) {
         case "Bolt" -> 0;
         case "Gem" -> 1;
         case "Dust" -> 2;
         case "Spark" -> 3;
         default -> MIXED_ORDER[Math.floorMod(var1, MIXED_ORDER.length)];
      };
   }

   private int colorFor(byte var1, int var2) {
      float var3 = this.random.method_43057();

      int var4 = switch (var1) {
         case 0 -> var3 < 0.6F ? this.palette.primary() : this.palette.highlight();
         case 1 -> var3 < 0.55F ? this.palette.secondary() : this.palette.companion();
         case 2 -> this.dust(var3);
         default -> GlyphTint.mix(this.palette.highlight(), -1, var3 * 0.35F);
      };

      return switch (var2) {
         case 0 -> GlyphTint.mix(var4, this.palette.companion(), 0.55F);
         case 2 -> GlyphTint.mix(var4, this.palette.secondary(), 0.45F);
         default -> var4;
      };
   }

   private int dust(float var1) {
      int var2 = this.palette.dust();
      if (var1 < 0.4F) {
         return var2;
      } else {
         return var1 < 0.75F ? GlyphTint.lift(var2, 0.4F) : GlyphTint.mix(var2, this.palette.secondary(), 0.3F);
      }
   }

   private static float sizeOf(byte var0) {
      return switch (var0) {
         case 0 -> 1.35F;
         case 1 -> 1.15F;
         case 2 -> 0.5F;
         default -> 0.9F;
      };
   }

   private static float spinOf(byte var0) {
      return switch (var0) {
         case 0 -> 0.5F;
         case 1 -> 1.1F;
         case 2 -> 5.0F;
         default -> 2.8F;
      };
   }

   private static float outward(byte var0) {
      return switch (var0) {
         case 0 -> 0.8F;
         case 1 -> 2.1F;
         case 2 -> 1.5F;
         default -> 3.4F;
      };
   }

   private static float upward(byte var0) {
      return switch (var0) {
         case 0 -> 1.1F;
         case 1 -> 1.7F;
         case 2 -> 2.6F;
         default -> 2.4F;
      };
   }

   private float jitter(float var1) {
      return (this.random.method_43057() * 2.0F - 1.0F) * var1;
   }
}
