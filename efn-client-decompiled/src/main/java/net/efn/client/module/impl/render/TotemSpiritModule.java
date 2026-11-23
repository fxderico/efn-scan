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

   private boolean bind(ClientWorld var1) {
      if (var1 == null) {
         if (this.boundLevel != null) {
            this.reset();
         }

         return false;
      } else {
         if (var1 != this.boundLevel) {
            this.head = 0;
            this.count = 0;
            this.boundLevel = var1;
         }

         return true;
      }
   }

   private void onTick(TickEvent var1) {
      if (this.boundLevel != null) {
         this.bind(MinecraftClient.method_1551().field_1687);
      }
   }

   private void onPacket(PacketReceiveEvent var1) {
      TotemPops.scan(var1.packet, this.popSink);
   }

   private void capture(LivingEntity var1) {
      MinecraftClient var4 = MinecraftClient.method_1551();
      if (var1 instanceof PlayerEntity var3 && this.bind(var4.field_1687) && var3.method_73183() == var4.field_1687) {
         if (!var3.method_7325()) {
            boolean var2 = var3 == var4.field_1724;
            if (!var2 || this.own.get()) {
               if (this.count == 12) {
                  this.head = (this.head + 1) % 12;
                  this.count--;
               }

               TotemSpiritModule.Spirit var6 = this.spirits[(this.head + this.count) % 12];
               this.count++;
               var6.x = var3.method_23317();
               var6.y = var3.method_23318();
               var6.z = var3.method_23321();
               var6.bodyYaw = var3.field_6283;
               var6.headYaw = MathHelper.method_15393(var3.field_6241 - var3.field_6283);
               var6.headPitch = var3.method_36455();
               var6.walk = var3.field_42108.method_48569();
               var6.stride = Math.min(1.0F, var3.field_42108.method_48566());
               var6.scale = var3.method_55693();
               var6.crouch = var3.method_18276();
               var6.mine = var2;
               var6.born = System.nanoTime();
            }
         }
      }
   }

   private void onRender(Render3DEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (this.bind(var2.field_1687) && this.count != 0) {
         long var3 = System.nanoTime();

         float var5;
         for (var5 = this.kaTime.get() * 1.0E9F; this.count > 0 && (float)(var3 - this.spirits[this.head].born) >= var5; this.count--) {
            this.head = (this.head + 1) % 12;
         }

         if (this.count != 0) {
            this.palette.resolve(this.colorMode.get(), CosmeticAccent.argb(), this.bodyColor.argb(), this.auraColor.argb());
            this.canvas.begin(var2.field_1773.method_19418());

            for (int var6 = 0; var6 < this.count; var6++) {
               TotemSpiritModule.Spirit var7 = this.spirits[(this.head + var6) % 12];
               if ((!var7.mine || this.own.get())
                  && !(this.canvas.distanceSqr(var7.x, var7.y, var7.z) > 9216.0)
                  && (!MobilePerformance.active() || var1.frustum == null || var1.frustum.method_23093(this.renderBounds(var7)))) {
                  this.draw(var7, Math.min(1.0F, (float)(var3 - var7.born) / var5));
               }
            }

            this.canvas.submit(this.throughWalls.get());
         }
      }
   }

   private Box renderBounds(TotemSpiritModule.Spirit var1) {
      double var2 = Math.max(0.01F, var1.scale);
      double var4 = var2 * 2.0 + 0.25;
      return new Box(var1.x - var4, var1.y - var2, var1.z - var4, var1.x + var4, var1.y + this.ascent.get() * 1.3 + var2 * 3.5 + 0.5, var1.z + var4);
   }

   private void draw(TotemSpiritModule.Spirit var1, float var2) {
      float var4 = this.opacity.get() * Math.min(1.0F, var2 / 0.06F);
      float var5 = this.kaPose.get() ? smooth((var2 - 0.08F) / 0.45F) : 0.0F;
      float var6 = this.ascent.get() * ascend(var2);
      if (this.seal.get() && var2 < 0.6F) {
         this.groundSeal(var1, var2 / 0.6F, var4);
      }

      double var7 = Math.toRadians(var1.bodyYaw);
      float var9 = (float)Math.cos(var7);
      float var10 = (float)Math.sin(var7);
      this.leftX = var9;
      this.leftZ = var10;
      this.forwardX = -var10;
      this.forwardZ = var9;
      this.originX = var1.x;
      this.originY = var1.y + var6;
      this.originZ = var1.z;
      this.unit = 0.05859375F * var1.scale * (1.0F + 0.08F * var2);
      float var11 = var1.crouch ? 0.45F * (1.0F - var5) : 0.0F;
      this.leanSin = MathHelper.method_15374(var11);
      this.leanCos = MathHelper.method_15362(var11);
      this.drop = var1.crouch ? 3.0F * (1.0F - var5) : 0.0F;
      this.pose(var1, var5);
      this.cachedPose = MobilePerformance.active();
      if (this.cachedPose) {
         this.cachePose();
      }

      boolean var12 = this.look.is("Glass");
      int var13 = this.palette.primary();
      int var14 = this.palette.secondary();
      int var15 = this.palette.highlight();

      for (int var3 = 0; var3 < PARTS.length; var3++) {
         float var16 = var4 * dissolve(var2, PARTS[var3][9]);
         if (!(var16 < 0.01F)) {
            this.place(var3, 0.7F);
            this.fill(GlyphTint.alpha(var14, var16 * (var12 ? 0.16F : 0.1F)));
         }
      }

      for (int var18 = 0; var18 < PARTS.length; var18++) {
         float var19 = PARTS[var18][9];
         float var17 = var4 * dissolve(var2, var19);
         if (!(var17 < 0.01F)) {
            this.place(var18, 0.0F);
            if (var12) {
               this.fill(GlyphTint.alpha(GlyphTint.mix(var13, var14, (1.0F - var19) * 0.45F), var17 * 0.32F));
            }

            this.edges(GlyphTint.alpha(var15, var17 * (var12 ? 0.8F : 0.95F)), var12 ? 1.5F : 2.0F);
         }
      }

      if (this.crest.get()) {
         this.boltCrest(var1, var2, var6, var4 * dissolve(var2, 1.1F));
      }
   }

   private void pose(TotemSpiritModule.Spirit var1, float var2) {
      float var3 = MathHelper.method_15362(var1.walk * 0.6662F) * var1.stride;
      float var4 = 1.0F - var2;
      float var5 = var1.crouch ? 0.35F : 0.0F;
      this.pitch[0] = var3 * 1.4F * var4 - 0.05F * var2;
      this.pitch[1] = -var3 * 1.4F * var4 - 0.15F * var2;
      this.pitch[2] = 0.0F;
      this.pitch[3] = MathHelper.method_16439(var2, -var3 + var5, 2.75F);
      this.pitch[4] = MathHelper.method_16439(var2, var3 + var5, 2.75F);
      this.pitch[5] = -var1.headPitch * 0.017453292F * var4 + 0.3F * var2;
      this.yaw[5] = var1.headYaw * 0.017453292F * var4;
      this.roll[3] = 0.32F * var2;
      this.roll[4] = -0.32F * var2;
   }

   private void place(int var1, float var2) {
      float[] var3 = PARTS[var1];
      boolean var4 = var1 >= 2;
      int var5 = var1 * 6;
      float var6 = this.cachedPose ? this.rotations[var5] : MathHelper.method_15362(this.pitch[var1]);
      float var7 = this.cachedPose ? this.rotations[var5 + 1] : MathHelper.method_15374(this.pitch[var1]);
      float var8 = this.cachedPose ? this.rotations[var5 + 2] : MathHelper.method_15362(this.yaw[var1]);
      float var9 = this.cachedPose ? this.rotations[var5 + 3] : MathHelper.method_15374(this.yaw[var1]);
      float var10 = this.cachedPose ? this.rotations[var5 + 4] : MathHelper.method_15362(this.roll[var1]);
      float var11 = this.cachedPose ? this.rotations[var5 + 5] : MathHelper.method_15374(this.roll[var1]);

      for (int var12 = 0; var12 < 8; var12++) {
         float var13 = (var12 & 1) == 0 ? var3[3] - var2 : var3[6] + var2;
         float var14 = (var12 & 2) == 0 ? var3[4] - var2 : var3[7] + var2;
         float var15 = (var12 & 4) == 0 ? var3[5] - var2 : var3[8] + var2;
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
         this.rotations[var2] = MathHelper.method_15362(this.pitch[var1]);
         this.rotations[var2 + 1] = MathHelper.method_15374(this.pitch[var1]);
         this.rotations[var2 + 2] = MathHelper.method_15362(this.yaw[var1]);
         this.rotations[var2 + 3] = MathHelper.method_15374(this.yaw[var1]);
         this.rotations[var2 + 4] = MathHelper.method_15362(this.roll[var1]);
         this.rotations[var2 + 5] = MathHelper.method_15374(this.roll[var1]);
      }
   }

   private boolean lean() {
      return this.leanSin != 0.0F;
   }

   private void fill(int var1) {
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
               var1
            );
      }
   }

   private void edges(int var1, float var2) {
      double[] var3 = this.corners;

      for (byte var4 = 0; var4 < EDGES.length; var4 += 2) {
         int var5 = EDGES[var4] * 3;
         int var6 = EDGES[var4 + 1] * 3;
         this.canvas.line(var3[var5], var3[var5 + 1], var3[var5 + 2], var3[var6], var3[var6 + 1], var3[var6 + 2], var1, var2);
      }
   }

   private void groundSeal(TotemSpiritModule.Spirit var1, float var2, float var3) {
      float var4 = 1.0F - (1.0F - var2) * (1.0F - var2) * (1.0F - var2);
      float var5 = (0.35F + 1.15F * var4) * var1.scale;
      float var6 = var3 * (1.0F - var2) * 0.9F;
      double var7 = var1.y + 0.02;
      int var9 = this.palette.primary();
      int var10 = this.palette.secondary();
      int var11 = this.palette.highlight();
      this.canvas.ring(var1.x, var7, var1.z, var5 * 0.93F, var5, MobilePerformance.segments(40, 24), GlyphTint.alpha(var9, var6));
      this.canvas.ring(var1.x, var7, var1.z, var5, var5 * 1.12F, MobilePerformance.segments(40, 24), GlyphTint.alpha(var9, var6 * 0.25F));
      this.canvas.ring(var1.x, var7, var1.z, var5 * 0.55F, var5 * 0.6F, MobilePerformance.segments(32, 16), GlyphTint.alpha(var10, var6 * 0.7F));
      float var12 = (float)Math.toRadians(var1.bodyYaw) + var4 * 0.6F;

      for (int var13 = 0; var13 < 4; var13++) {
         float var14 = var12 + var13 * 1.5707964F;
         this.canvas
            .flat(
               GlyphShapes.BOLT,
               var1.x + MathHelper.method_15362(var14) * var5 * 0.78F,
               var7 + 0.004,
               var1.z + MathHelper.method_15374(var14) * var5 * 0.78F,
               0.13F * var1.scale,
               -var14 - 1.5707964F,
               GlyphTint.alpha(var11, var6)
            );
      }
   }

   private void boltCrest(TotemSpiritModule.Spirit var1, float var2, float var3, float var4) {
      if (!(var4 < 0.01F)) {
         float var5 = Math.min(1.0F, var2 / 0.2F);
         float var6 = 1.0F + 1.7F * (var5 - 1.0F) * (var5 - 1.0F) * (var5 - 1.0F) + 0.7F * (var5 - 1.0F) * (var5 - 1.0F);
         float var7 = 0.2F * var1.scale * Math.max(0.0F, var6);
         if (!(var7 <= 0.001F)) {
            double var8 = this.originY + (32.0F - this.drop) * this.unit + 0.35F * var1.scale + var3 * 0.3F;
            this.canvas.sprite(GlyphShapes.OCTAGON, var1.x, var8, var1.z, var7 * 2.2F, 0.0F, GlyphTint.alpha(this.palette.secondary(), var4 * 0.2F));
            this.canvas.sprite(GlyphShapes.BOLT, var1.x, var8, var1.z, var7, 0.0F, GlyphTint.alpha(this.palette.highlight(), var4));
         }
      }
   }

   private static float dissolve(float var0, float var1) {
      return 1.0F - MathHelper.method_15363((var0 - 0.45F - var1 * 0.22F) / 0.3F, 0.0F, 1.0F);
   }

   private static float ascend(float var0) {
      float var1 = Math.max(0.0F, (var0 - 0.12F) / 0.88F);
      return var1 * var1 * (2.2F - 1.2F * var1);
   }

   private static float smooth(float var0) {
      float var1 = MathHelper.method_15363(var0, 0.0F, 1.0F);
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
