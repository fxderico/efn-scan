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
import net.efn.client.util.glyph.GlyphShapes;
import net.efn.client.util.glyph.GlyphTint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class TotemSpiritModule extends ClientModule {
   private static final String LOOK_GLASS = "Glass";
   private static final String LOOK_WIRE = "Wire";
   private static final int MAX_SPIRITS = 12;
   private static final double MAX_DISTANCE_SQR = 9216.0;
   private static final float MODEL_UNIT = 0.05859375F;
   private static final float HIP = 12.0F;
   private static final float HEAD_TOP = 32.0F;
   private static final float CROUCH_LEAN = 0.45F;
   private static final float CROUCH_DROP = 3.0F;
   private static final float KA_ARM_PITCH = 2.75F;
   private static final float KA_ARM_SPREAD = 0.32F;
   private static final float SHELL = 0.7F;
   private static final float SEAL_SPAN = 0.6F;
   private static final float[][] PARTS = new float[][]{
      {-1.9F, 12.0F, 0.0F, -2.0F, -12.0F, -2.0F, 2.0F, 0.0F, 2.0F, 0.0F},
      {1.9F, 12.0F, 0.0F, -2.0F, -12.0F, -2.0F, 2.0F, 0.0F, 2.0F, 0.0F},
      {0.0F, 12.0F, 0.0F, -4.0F, 0.0F, -2.0F, 4.0F, 12.0F, 2.0F, 0.4F},
      {-5.0F, 22.0F, 0.0F, -3.0F, -10.0F, -2.0F, 1.0F, 2.0F, 2.0F, 0.6F},
      {5.0F, 22.0F, 0.0F, -1.0F, -10.0F, -2.0F, 3.0F, 2.0F, 2.0F, 0.6F},
      {0.0F, 24.0F, 0.0F, -4.0F, 0.0F, -4.0F, 4.0F, 8.0F, 4.0F, 1.0F}
   };
   private static final int FIRST_TORSO_PART = 2;
   private static final int[] FACES = new int[]{1, 3, 7, 5, 0, 4, 6, 2, 2, 6, 7, 3, 0, 1, 5, 4, 4, 5, 7, 6, 0, 2, 3, 1};
   private static final int[] EDGES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 0, 2, 1, 3, 4, 6, 5, 7, 0, 4, 1, 5, 2, 6, 3, 7};
   private final NumberSetting kaTime = this.addSetting("ka-time", new NumberSetting("Ka Time", 2.4F, 0.6F, 6.0F).step(0.1F));
   private final NumberSetting ascent = this.addSetting("ascent", new NumberSetting("Ascent", 1.6F, 0.0F, 4.0F).step(0.1F));
   private final ModeSetting look = this.addSetting("look", new ModeSetting("Look", "Glass", "Glass", "Wire"));
   private final NumberSetting opacity = this.addSetting("opacity", new NumberSetting("Opacity", 0.65F, 0.15F, 1.0F).step(0.05F));
   private final BooleanSetting kaPose = this.addSetting("ka-pose", new BooleanSetting("Ka Pose", true));
   private final BooleanSetting crest = this.addSetting("crest", new BooleanSetting("Bolt Crest", true));
   private final BooleanSetting seal = this.addSetting("seal", new BooleanSetting("Ground Seal", true));
   private final BooleanSetting own = this.addSetting("own", new BooleanSetting("Own Pops", false));
   private final BooleanSetting throughWalls = this.addSetting("through-walls", new BooleanSetting("Through Walls", false));
   private final ModeSetting colorMode = this.addSetting("color-mode", new ModeSetting("Color", "Theme", "Theme", "Manual"));
   private final ColorSetting bodyColor = this.addSetting("body-color", new ColorSetting("Body Color", -11891723));
   private final ColorSetting auraColor = this.addSetting("aura-color", new ColorSetting("Aura Color", -14860125));
   private final TotemSpiritModule.Spirit[] spirits = new TotemSpiritModule.Spirit[12];
   private final GlyphCanvas canvas = new GlyphCanvas();
   private final GlyphTint palette = new GlyphTint();
   private final Consumer<LivingEntity> popSink = this::capture;
   private final double[] corners = new double[24];
   private final float[] pitch = new float[PARTS.length];
   private final float[] yaw = new float[PARTS.length];
   private final float[] roll = new float[PARTS.length];
   private final float[] rotations = new float[PARTS.length * 6];
   private boolean cachedPose;
   private int head;
   private int count;
   private ClientWorld boundLevel;
   private double originX;
   private double originY;
   private double originZ;
   private float leftX;
   private float leftZ;
   private float forwardX;
   private float forwardZ;
   private float unit;
   private float leanSin;
   private float leanCos;
   private float drop;

   public TotemSpiritModule() {
      super("totem-spirit", "Totem Spirit", "A glowing ghost rises when a player pops a totem.", ModuleCategory.VISUALS);

      for (int var1 = 0; var1 < 12; var1++) {
         this.spirits[var1] = new TotemSpiritModule.Spirit();
      }

      this.bodyColor.visibleWhen(() -> this.colorMode.is("Manual"));
      this.auraColor.visibleWhen(() -> this.colorMode.is("Manual"));
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
      this.head = 0;
      this.count = 0;
      this.boundLevel = null;
   }

   private boolean bind(ClientWorld clientWorld) {
      if (clientWorld == null) {
         if (this.boundLevel != null) {
            this.reset();
         }

         return false;
      } else {
         if (clientWorld != this.boundLevel) {
            this.head = 0;
            this.count = 0;
            this.boundLevel = clientWorld;
         }

         return true;
      }
   }

   private void onTick(TickEvent tickEvent) {
      if (this.boundLevel != null) {
         this.bind(MinecraftClient.getInstance().world);
      }
   }

   private void onPacket(PacketReceiveEvent packetReceiveEvent) {
      TotemPops.scan(packetReceiveEvent.packet, this.popSink);
   }

   private void capture(LivingEntity livingEntity) {
      MinecraftClient var4 = MinecraftClient.getInstance();
      if (livingEntity instanceof PlayerEntity var3 && this.bind(var4.world) && var3.getEntityWorld() == var4.world) {
         if (!var3.isSpectator()) {
            boolean var2 = var3 == var4.player;
            if (!var2 || this.own.get()) {
               if (this.count == 12) {
                  this.head = (this.head + 1) % 12;
                  this.count--;
               }

               TotemSpiritModule.Spirit var6 = this.spirits[(this.head + this.count) % 12];
               this.count++;
               var6.x = var3.getX();
               var6.y = var3.getY();
               var6.z = var3.getZ();
               var6.bodyYaw = var3.bodyYaw;
               var6.headYaw = MathHelper.wrapDegrees(var3.headYaw - var3.bodyYaw);
               var6.headPitch = var3.getPitch();
               var6.walk = var3.limbAnimator.getAnimationProgress();
               var6.stride = Math.min(1.0F, var3.limbAnimator.getSpeed());
               var6.scale = var3.getScale();
               var6.crouch = var3.isInSneakingPose();
               var6.mine = var2;
               var6.born = System.nanoTime();
            }
         }
      }
   }

   private void onRender(Render3DEvent render3DEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (this.bind(var2.world) && this.count != 0) {
         long var3 = System.nanoTime();

         float var5;
         for (var5 = this.kaTime.get() * 1.0E9F; this.count > 0 && (float)(var3 - this.spirits[this.head].born) >= var5; this.count--) {
            this.head = (this.head + 1) % 12;
         }

         if (this.count != 0) {
            this.palette.resolve(this.colorMode.get(), CosmeticAccent.argb(), this.bodyColor.argb(), this.auraColor.argb());
            this.canvas.begin(var2.gameRenderer.getCamera());

            for (int var6 = 0; var6 < this.count; var6++) {
               TotemSpiritModule.Spirit var7 = this.spirits[(this.head + var6) % 12];
               if ((!var7.mine || this.own.get())
                  && !(this.canvas.distanceSqr(var7.x, var7.y, var7.z) > 9216.0)
                  && (!MobilePerformance.active() || render3DEvent.frustum == null || render3DEvent.frustum.isVisible(this.renderBounds(var7)))) {
                  this.draw(var7, Math.min(1.0F, (float)(var3 - var7.born) / var5));
               }
            }

            this.canvas.submit(this.throughWalls.get());
         }
      }
   }

   private Box renderBounds(TotemSpiritModule.Spirit spirit) {
      double var2 = Math.max(0.01F, spirit.scale);
      double var4 = var2 * 2.0 + 0.25;
      return new Box(spirit.x - var4, spirit.y - var2, spirit.z - var4, spirit.x + var4, spirit.y + this.ascent.get() * 1.3 + var2 * 3.5 + 0.5, spirit.z + var4);
   }

   private void draw(TotemSpiritModule.Spirit spirit, float f) {
      float var4 = this.opacity.get() * Math.min(1.0F, f / 0.06F);
      float var5 = this.kaPose.get() ? smooth((f - 0.08F) / 0.45F) : 0.0F;
      float var6 = this.ascent.get() * ascend(f);
      if (this.seal.get() && f < 0.6F) {
         this.groundSeal(spirit, f / 0.6F, var4);
      }

      double var7 = Math.toRadians(spirit.bodyYaw);
      float var9 = (float)Math.cos(var7);
      float var10 = (float)Math.sin(var7);
      this.leftX = var9;
      this.leftZ = var10;
      this.forwardX = -var10;
      this.forwardZ = var9;
      this.originX = spirit.x;
      this.originY = spirit.y + var6;
      this.originZ = spirit.z;
      this.unit = 0.05859375F * spirit.scale * (1.0F + 0.08F * f);
      float var11 = spirit.crouch ? 0.45F * (1.0F - var5) : 0.0F;
      this.leanSin = MathHelper.sin(var11);
      this.leanCos = MathHelper.cos(var11);
      this.drop = spirit.crouch ? 3.0F * (1.0F - var5) : 0.0F;
      this.pose(spirit, var5);
      this.cachedPose = MobilePerformance.active();
      if (this.cachedPose) {
         this.cachePose();
      }

      boolean var12 = this.look.is("Glass");
      int var13 = this.palette.primary();
      int var14 = this.palette.secondary();
      int var15 = this.palette.highlight();

      for (int var3 = 0; var3 < PARTS.length; var3++) {
         float var16 = var4 * dissolve(f, PARTS[var3][9]);
         if (!(var16 < 0.01F)) {
            this.place(var3, 0.7F);
            this.fill(GlyphTint.alpha(var14, var16 * (var12 ? 0.16F : 0.1F)));
         }
      }

      for (int var18 = 0; var18 < PARTS.length; var18++) {
         float var19 = PARTS[var18][9];
         float var17 = var4 * dissolve(f, var19);
         if (!(var17 < 0.01F)) {
            this.place(var18, 0.0F);
            if (var12) {
               this.fill(GlyphTint.alpha(GlyphTint.mix(var13, var14, (1.0F - var19) * 0.45F), var17 * 0.32F));
            }

            this.edges(GlyphTint.alpha(var15, var17 * (var12 ? 0.8F : 0.95F)), var12 ? 1.5F : 2.0F);
         }
      }

      if (this.crest.get()) {
         this.boltCrest(spirit, f, var6, var4 * dissolve(f, 1.1F));
      }
   }

   private void pose(TotemSpiritModule.Spirit spirit, float f) {
      float var3 = MathHelper.cos(spirit.walk * 0.6662F) * spirit.stride;
      float var4 = 1.0F - f;
      float var5 = spirit.crouch ? 0.35F : 0.0F;
      this.pitch[0] = var3 * 1.4F * var4 - 0.05F * f;
      this.pitch[1] = -var3 * 1.4F * var4 - 0.15F * f;
      this.pitch[2] = 0.0F;
      this.pitch[3] = MathHelper.lerp(f, -var3 + var5, 2.75F);
      this.pitch[4] = MathHelper.lerp(f, var3 + var5, 2.75F);
      this.pitch[5] = -spirit.headPitch * 0.017453292F * var4 + 0.3F * f;
      this.yaw[5] = spirit.headYaw * 0.017453292F * var4;
      this.roll[3] = 0.32F * f;
      this.roll[4] = -0.32F * f;
   }

   private void place(int i, float f) {
      float[] var3 = PARTS[i];
      boolean var4 = i >= 2;
      int var5 = i * 6;
      float var6 = this.cachedPose ? this.rotations[var5] : MathHelper.cos(this.pitch[i]);
      float var7 = this.cachedPose ? this.rotations[var5 + 1] : MathHelper.sin(this.pitch[i]);
      float var8 = this.cachedPose ? this.rotations[var5 + 2] : MathHelper.cos(this.yaw[i]);
      float var9 = this.cachedPose ? this.rotations[var5 + 3] : MathHelper.sin(this.yaw[i]);
      float var10 = this.cachedPose ? this.rotations[var5 + 4] : MathHelper.cos(this.roll[i]);
      float var11 = this.cachedPose ? this.rotations[var5 + 5] : MathHelper.sin(this.roll[i]);

      for (int var12 = 0; var12 < 8; var12++) {
         float var13 = (var12 & 1) == 0 ? var3[3] - f : var3[6] + f;
         float var14 = (var12 & 2) == 0 ? var3[4] - f : var3[7] + f;
         float var15 = (var12 & 4) == 0 ? var3[5] - f : var3[8] + f;
         float var16 = var14 * var6 + var15 * var7;
         float var17 = var15 * var6 - var14 * var7;
         float var18 = var13 * var8 - var17 * var9;
         float var19 = var13 * var9 + var17 * var8;
         float var20 = var18 * var10 - var16 * var11 + var3[0];
         float var21 = var18 * var11 + var16 * var10 + var3[1];
         float var22 = var19 + var3[2];
         if (var4 && this.lean()) {
            float var23 = var21 - 12.0F;
            float var24 = var23 * this.leanCos - var22 * this.leanSin;
            var22 = var23 * this.leanSin + var22 * this.leanCos;
            var21 = var24 + 12.0F;
         }

         int var26 = var12 * 3;
         this.corners[var26] = this.originX + (var20 * this.leftX + var22 * this.forwardX) * this.unit;
         float var25;
         this.corners[var26 + 1] = this.originY + (var25 = var21 - this.drop) * this.unit;
         this.corners[var26 + 2] = this.originZ + (var20 * this.leftZ + var22 * this.forwardZ) * this.unit;
      }
   }

   private void cachePose() {
      for (int var1 = 0; var1 < PARTS.length; var1++) {
         int var2 = var1 * 6;
         this.rotations[var2] = MathHelper.cos(this.pitch[var1]);
         this.rotations[var2 + 1] = MathHelper.sin(this.pitch[var1]);
         this.rotations[var2 + 2] = MathHelper.cos(this.yaw[var1]);
         this.rotations[var2 + 3] = MathHelper.sin(this.yaw[var1]);
         this.rotations[var2 + 4] = MathHelper.cos(this.roll[var1]);
         this.rotations[var2 + 5] = MathHelper.sin(this.roll[var1]);
      }
   }

   private boolean lean() {
      return this.leanSin != 0.0F;
   }

   private void fill(int i) {
      double[] var2 = this.corners;

      for (byte var3 = 0; var3 < FACES.length; var3 += 4) {
         int var4 = FACES[var3] * 3;
         int var5 = FACES[var3 + 1] * 3;
         int var6 = FACES[var3 + 2] * 3;
         int var7 = FACES[var3 + 3] * 3;
         this.canvas
            .quad(
               var2[var4],
               var2[var4 + 1],
               var2[var4 + 2],
               var2[var5],
               var2[var5 + 1],
               var2[var5 + 2],
               var2[var6],
               var2[var6 + 1],
               var2[var6 + 2],
               var2[var7],
               var2[var7 + 1],
               var2[var7 + 2],
               i
            );
      }
   }

   private void edges(int i, float f) {
      double[] var3 = this.corners;

      for (byte var4 = 0; var4 < EDGES.length; var4 += 2) {
         int var5 = EDGES[var4] * 3;
         int var6 = EDGES[var4 + 1] * 3;
         this.canvas.line(var3[var5], var3[var5 + 1], var3[var5 + 2], var3[var6], var3[var6 + 1], var3[var6 + 2], i, f);
      }
   }

   private void groundSeal(TotemSpiritModule.Spirit spirit, float f, float g) {
      float var4 = 1.0F - (1.0F - f) * (1.0F - f) * (1.0F - f);
      float var5 = (0.35F + 1.15F * var4) * spirit.scale;
      float var6 = g * (1.0F - f) * 0.9F;
      double var7 = spirit.y + 0.02;
      int var9 = this.palette.primary();
      int var10 = this.palette.secondary();
      int var11 = this.palette.highlight();
      this.canvas.ring(spirit.x, var7, spirit.z, var5 * 0.93F, var5, MobilePerformance.segments(40, 24), GlyphTint.alpha(var9, var6));
      this.canvas.ring(spirit.x, var7, spirit.z, var5, var5 * 1.12F, MobilePerformance.segments(40, 24), GlyphTint.alpha(var9, var6 * 0.25F));
      this.canvas.ring(spirit.x, var7, spirit.z, var5 * 0.55F, var5 * 0.6F, MobilePerformance.segments(32, 16), GlyphTint.alpha(var10, var6 * 0.7F));
      float var12 = (float)Math.toRadians(spirit.bodyYaw) + var4 * 0.6F;

      for (int var13 = 0; var13 < 4; var13++) {
         float var14 = var12 + var13 * 1.5707964F;
         this.canvas
            .flat(
               GlyphShapes.BOLT,
               spirit.x + MathHelper.cos(var14) * var5 * 0.78F,
               var7 + 0.004,
               spirit.z + MathHelper.sin(var14) * var5 * 0.78F,
               0.13F * spirit.scale,
               -var14 - 1.5707964F,
               GlyphTint.alpha(var11, var6)
            );
      }
   }

   private void boltCrest(TotemSpiritModule.Spirit spirit, float f, float g, float h) {
      if (!(h < 0.01F)) {
         float var5 = Math.min(1.0F, f / 0.2F);
         float var6 = 1.0F + 1.7F * (var5 - 1.0F) * (var5 - 1.0F) * (var5 - 1.0F) + 0.7F * (var5 - 1.0F) * (var5 - 1.0F);
         float var7 = 0.2F * spirit.scale * Math.max(0.0F, var6);
         if (!(var7 <= 0.001F)) {
            double var8 = this.originY + (32.0F - this.drop) * this.unit + 0.35F * spirit.scale + g * 0.3F;
            this.canvas.sprite(GlyphShapes.OCTAGON, spirit.x, var8, spirit.z, var7 * 2.2F, 0.0F, GlyphTint.alpha(this.palette.secondary(), h * 0.2F));
            this.canvas.sprite(GlyphShapes.BOLT, spirit.x, var8, spirit.z, var7, 0.0F, GlyphTint.alpha(this.palette.highlight(), h));
         }
      }
   }

   private static float dissolve(float f, float g) {
      return 1.0F - MathHelper.clamp((f - 0.45F - g * 0.22F) / 0.3F, 0.0F, 1.0F);
   }

   private static float ascend(float f) {
      float var1 = Math.max(0.0F, (f - 0.12F) / 0.88F);
      return var1 * var1 * (2.2F - 1.2F * var1);
   }

   private static float smooth(float f) {
      float var1 = MathHelper.clamp(f, 0.0F, 1.0F);
      return var1 * var1 * (3.0F - 2.0F * var1);
   }

   @Environment(EnvType.CLIENT)
   private static final class Spirit {
      double x;
      double y;
      double z;
      float bodyYaw;
      float headYaw;
      float headPitch;
      float walk;
      float stride;
      float scale;
      boolean crouch;
      boolean mine;
      long born;

      private Spirit() {
      }
   }
}
