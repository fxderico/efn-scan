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
   private final Random random = Random.create();
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

   private boolean bind(ClientWorld clientWorld) {
      if (clientWorld == null) {
         if (this.boundLevel != null) {
            this.reset();
         }

         return false;
      } else {
         if (clientWorld != this.boundLevel) {
            this.swarm.clear();
            this.boundLevel = clientWorld;
         }

         return true;
      }
   }

   private void onPacket(PacketReceiveEvent packetReceiveEvent) {
      if (this.pops.get()) {
         TotemPops.scan(packetReceiveEvent.packet, this.popSink);
      }
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (this.bind(var2.world) && var3 != null) {
         this.refreshPalette();
         if (this.projectiles.get()) {
            this.trails(var2.world, var3);
         }

         if (this.footsteps.get()) {
            this.steps(var3);
         }
      }
   }

   private void onRender(Render3DEvent render3DEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (this.bind(var2.world)) {
         long var3 = System.nanoTime();
         float var5 = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(var3 - this.lastFrame) / 1.0E9F);
         this.lastFrame = var3;
         if (var2.isPaused()) {
            var5 = 0.0F;
         }

         this.swarm.trim(this.particleLimit());
         this.swarm.step(var5);
         if (this.swarm.count() != 0) {
            this.canvas.begin(var2.gameRenderer.getCamera());
            this.swarm.draw(this.canvas, 1.0F, this.halo.get());
            this.canvas.submit(false);
         }
      }
   }

   private void burst(LivingEntity livingEntity) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (this.bind(var2.world) && livingEntity.getEntityWorld() == var2.world) {
         if (livingEntity != var2.player || this.ownPops.get()) {
            this.refreshPalette();
            Box var3 = livingEntity.getBoundingBox();
            int var4 = MobilePerformance.particles(this.popBurst.getInt(), 6);
            String var5 = this.popGlyph.get();

            for (int var6 = 0; var6 < var4; var6++) {
               byte var7 = pick(var5, var6);
               double var8 = var3.minX + var3.getLengthX() * this.random.nextDouble();
               double var10 = var3.minY + var3.getLengthY() * (0.15 + 0.8 * this.random.nextDouble());
               double var12 = var3.minZ + var3.getLengthZ() * this.random.nextDouble();
               float var14 = this.random.nextFloat() * 6.2831855F;
               float var15 = outward(var7) * (0.45F + this.random.nextFloat());
               float var16 = upward(var7) * (0.55F + 0.8F * this.random.nextFloat());
               this.emit(var7, var8, var10, var12, MathHelper.cos(var14) * var15, var16, MathHelper.sin(var14) * var15, 1.0F, 1.0F, this.colorFor(var7, -1));
            }
         }
      }
   }

   private void trails(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity) {
      int var3 = MobilePerformance.particles(this.trailDensity.getInt(), 1);
      String var4 = this.trailGlyph.get();

      for (Entity var6 : clientWorld.getEntities()) {
         int var13 = this.role(var6);
         double var7;
         double var9;
         double var11;
         if (var13 != -1
            && !((var11 = var6.getX() - var6.lastX) * var11 + (var9 = var6.getY() - var6.lastY) * var9 + (var7 = var6.getZ() - var6.lastZ) * var7 < 9.0E-4)
            && !(var6.squaredDistanceTo(clientPlayerEntity) > 6400.0)) {
            double var14 = var6.getHeight() * 0.5;

            for (int var16 = 0; var16 < var3; var16++) {
               float var17 = (var16 + this.random.nextFloat()) / var3;
               byte var18 = pick(var4, var6.age * var3 + var16);
               this.emit(
                  var18,
                  var6.lastX + var11 * var17 + this.jitter(0.06F),
                  var6.lastY + var9 * var17 + var14 + this.jitter(0.06F),
                  var6.lastZ + var7 * var17 + this.jitter(0.06F),
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

   private void steps(ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.isOnGround() && !clientPlayerEntity.isSpectator() && !clientPlayerEntity.hasVehicle() && !clientPlayerEntity.isInFluid()) {
         double var4 = clientPlayerEntity.getX() - clientPlayerEntity.lastX;
         double var2;
         double var6 = Math.sqrt(var4 * var4 + (var2 = clientPlayerEntity.getZ() - clientPlayerEntity.lastZ) * var2);
         if (!(var6 < 0.07)) {
            int var8 = MobilePerformance.particles(this.stepDensity.getInt() + (clientPlayerEntity.isSprinting() ? 1 : 0), 1);
            String var9 = this.stepGlyph.get();

            for (int var10 = 0; var10 < var8; var10++) {
               byte var11 = pick(var9, clientPlayerEntity.age * var8 + var10);
               double var12 = 0.2 + 0.3 * this.random.nextDouble();
               this.emit(
                  var11,
                  clientPlayerEntity.getX() - var4 / var6 * var12 + this.jitter(0.22F),
                  clientPlayerEntity.getY() + 0.04,
                  clientPlayerEntity.getZ() - var2 / var6 * var12 + this.jitter(0.22F),
                  (float)(-var4 * 7.0) + this.jitter(0.5F),
                  0.8F + this.random.nextFloat() * 1.1F,
                  (float)(-var2 * 7.0) + this.jitter(0.5F),
                  0.7F,
                  0.75F,
                  this.colorFor(var11, -1)
               );
            }
         }
      }
   }

   private void emit(byte b, double d, double e, double f, float g, float h, float i, float j, float k, int l) {
      float var14 = this.lifetime.get() * j * (0.75F + 0.5F * this.random.nextFloat());
      float var15 = 0.085F * this.size.get() * k * sizeOf(b) * (0.8F + 0.4F * this.random.nextFloat());
      float var16 = spinOf(b) * (this.random.nextBoolean() ? 1.0F : -1.0F) * (0.5F + this.random.nextFloat());
      float var17 = b == 0 ? this.jitter(0.35F) : this.random.nextFloat() * 6.2831855F;
      this.swarm.spawn(b, d, e, f, g, h, i, var14, var15, var17, var16, this.random.nextFloat(), l, this.particleLimit());
   }

   private int particleLimit() {
      return MobilePerformance.particles(this.limit.getInt(), 40);
   }

   private void refreshPalette() {
      this.palette.resolve(this.colorMode.get(), CosmeticAccent.argb(), this.mainColor.argb(), this.secondColor.argb());
   }

   private int role(Entity entity) {
      if (entity.isRemoved()) {
         return -1;
      } else if (entity instanceof EnderPearlEntity) {
         return this.pearls.get() ? 0 : -1;
      } else if (entity instanceof TridentEntity) {
         return this.tridents.get() ? 2 : -1;
      } else if (entity instanceof PersistentProjectileEntity) {
         return this.arrows.get() ? 1 : -1;
      } else {
         return -1;
      }
   }

   private static byte pick(String string, int i) {
      return switch (string) {
         case "Bolt" -> 0;
         case "Gem" -> 1;
         case "Dust" -> 2;
         case "Spark" -> 3;
         default -> MIXED_ORDER[Math.floorMod(i, MIXED_ORDER.length)];
      };
   }

   private int colorFor(byte b, int i) {
      float var3 = this.random.nextFloat();

      int var4 = switch (b) {
         case 0 -> var3 < 0.6F ? this.palette.primary() : this.palette.highlight();
         case 1 -> var3 < 0.55F ? this.palette.secondary() : this.palette.companion();
         case 2 -> this.dust(var3);
         default -> GlyphTint.mix(this.palette.highlight(), -1, var3 * 0.35F);
      };

      return switch (i) {
         case 0 -> GlyphTint.mix(var4, this.palette.companion(), 0.55F);
         case 2 -> GlyphTint.mix(var4, this.palette.secondary(), 0.45F);
         default -> var4;
      };
   }

   private int dust(float f) {
      int var2 = this.palette.dust();
      if (f < 0.4F) {
         return var2;
      } else {
         return f < 0.75F ? GlyphTint.lift(var2, 0.4F) : GlyphTint.mix(var2, this.palette.secondary(), 0.3F);
      }
   }

   private static float sizeOf(byte b) {
      return switch (b) {
         case 0 -> 1.35F;
         case 1 -> 1.15F;
         case 2 -> 0.5F;
         default -> 0.9F;
      };
   }

   private static float spinOf(byte b) {
      return switch (b) {
         case 0 -> 0.5F;
         case 1 -> 1.1F;
         case 2 -> 5.0F;
         default -> 2.8F;
      };
   }

   private static float outward(byte b) {
      return switch (b) {
         case 0 -> 0.8F;
         case 1 -> 2.1F;
         case 2 -> 1.5F;
         default -> 3.4F;
      };
   }

   private static float upward(byte b) {
      return switch (b) {
         case 0 -> 1.1F;
         case 1 -> 1.7F;
         case 2 -> 2.6F;
         default -> 2.4F;
      };
   }

   private float jitter(float f) {
      return (this.random.nextFloat() * 2.0F - 1.0F) * f;
   }
}
