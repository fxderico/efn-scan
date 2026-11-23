package net.efn.client.module.impl.render;

import com.example.client.mixin.AccessorBlockOverlayMultiPlayerGameMode;
import com.example.client.mixin.AccessorGameRenderer;
import java.util.ArrayList;
import java.util.List;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.gui.ThemeTint;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.VisibilityConfigurable;

@Environment(EnvType.CLIENT)
public final class BlockOverlayModule extends ClientModule {
   private static final String BOTH = "Both";
   private static final String OUTLINE = "Outline";
   private static final String FILL = "Fill";
   private static final String THEME = "Theme";
   private static final String GRADIENT = "Gradient";
   private static final String SOLID = "Solid";
   private static final String RAINBOW = "Rainbow";
   private static final float THEME_LIGHTEN = 0.55F;
   private static final float FADE_IN = 0.12F;
   private static final float FADE_OUT = 0.2F;
   private static final double SNAP_DISTANCE = 6.0;
   private static final float FLOW_SPEED = 0.35F;
   private static final double SEGMENT = 0.125;
   private static final double FILL_INFLATE = 0.002;
   private static final double LINE_LIFT = 0.004;
   private static final double GLOW_LIFT = 0.002;
   private final ModeSetting style = this.addSetting("style", new ModeSetting("Style", "Both", "Both", "Outline", "Fill"));
   private final ModeSetting colorMode = this.addSetting("color-mode", new ModeSetting("Color Mode", "Theme", "Theme", "Gradient", "Solid", "Rainbow"));
   private final ColorSetting color = this.addSetting("color", new ColorSetting("Color", -15408912));
   private final ColorSetting secondColor = this.addSetting("second-color", new ColorSetting("Second Color", -6480));
   private final BooleanSetting flow = this.addSetting("flow", new BooleanSetting("Flow", true));
   private final NumberSetting fillOpacity = this.addSetting("fill-opacity", new NumberSetting("Fill Opacity", 20.0F, 2.0F, 60.0F).step(1.0F));
   private final NumberSetting width = this.addSetting("width", new NumberSetting("Width", 2.0F, 1.0F, 5.0F).step(0.5F));
   private final BooleanSetting glow = this.addSetting("glow", new BooleanSetting("Glow", true));
   private final BooleanSetting smooth = this.addSetting("smooth", new BooleanSetting("Smooth", true));
   private final NumberSetting smoothSpeed = this.addSetting("smooth-speed", new NumberSetting("Smooth Speed", 16.0F, 4.0F, 40.0F).step(1.0F));
   private final BooleanSetting miningProgress = this.addSetting("mining-progress", new BooleanSetting("Mining Progress", true));
   private final BooleanSetting faceOnly = this.addSetting("face-only", new BooleanSetting("Face Only", false));
   private final BooleanSetting seeThrough = this.addSetting("see-through", new BooleanSetting("See Through", false));
   private ClientWorld level;
   private BlockPos pos;
   private BlockState state;
   private BlockPos partner;
   private BlockState partnerState;
   private final List<double[]> edges = new ArrayList<>();
   private List<Box> boxes = List.of();
   private Box localBounds;
   private Box goal;
   private Box box;
   private Direction face;
   private Box faceBox;
   private float presence;
   private float progress;
   private long lastFrame;
   private Vec3d eye;
   private double time;
   private float fade;
   private double ax;
   private double bx;
   private double ay;
   private double by;
   private double az;
   private double bz;

   public BlockOverlayModule() {
      super("block-overlay", "Block Overlay", "A smooth, animated outline and fill on the block you look at.", ModuleCategory.VISUALS);
      this.color.visibleWhen(() -> !this.colorMode.is("Theme"));
      this.secondColor.visibleWhen(() -> this.colorMode.is("Gradient"));
      this.flow.visibleWhen(() -> !this.colorMode.is("Solid"));
      this.fillOpacity.visibleWhen(() -> !this.style.is("Outline"));
      this.width.visibleWhen(() -> !this.style.is("Fill"));
      this.glow.visibleWhen(() -> !this.style.is("Fill"));
      this.smoothSpeed.visibleWhen(this.smooth::get);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   @EventHandler
   public void onRender3D(Render3DEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      if (var2.field_1687 != null && var2.field_1724 != null) {
         if (var2.field_1687 != this.level) {
            this.reset();
            this.level = var2.field_1687;
         }

         long var3 = System.nanoTime();
         float var5 = this.lastFrame == 0L ? 0.0F : Math.min((float)(var3 - this.lastFrame) / 1.0E9F, 0.1F);
         this.lastFrame = var3;
         BlockHitResult var6 = target(var2);
         if (var6 != null && this.adopt(var2, var6)) {
            this.box = this.box != null
                  && !(this.presence < 0.02F)
                  && this.smooth.get()
                  && !(this.box.method_1005().method_1022(this.goal.method_1005()) > 6.0)
               ? lerp(this.box, this.goal, 1.0F - (float)Math.exp(-this.smoothSpeed.get() * var5))
               : this.goal;
            this.presence = Math.min(1.0F, this.presence + var5 / 0.12F);
         } else {
            var6 = null;
            this.presence = Math.max(0.0F, this.presence - var5 / 0.2F);
            if (this.presence == 0.0F) {
               this.reset();
               this.level = var2.field_1687;
               return;
            }
         }

         if (this.box != null) {
            this.updateProgress(var2, var5, var6 != null);
            this.eye = var2.field_1773.method_19418().method_71156();
            this.time = var3 % 3600000000000L / 1.0E9;
            this.draw();
         }
      } else {
         this.reset();
      }
   }

   private static BlockHitResult target(MinecraftClient var0) {
      HitResult var2 = var0.field_1765;
      BlockHitResult var1;
      if (!(var2 instanceof BlockHitResult) || (var1 = (BlockHitResult)var2).method_17783() != Type.field_1332 || var1.method_17781()) {
         return null;
      } else if (!((AccessorGameRenderer)var0.field_1773).efn$shouldRenderBlockOutline()) {
         return null;
      } else {
         BlockPos var3 = var1.method_17777();
         return !var0.field_1687.method_8320(var3).method_26215() && var0.field_1687.method_8621().method_11952(var3) ? var1 : null;
      }
   }

   private boolean adopt(MinecraftClient var1, BlockHitResult var2) {
      ClientWorld var5 = var1.field_1687;
      BlockPos var6 = var2.method_17777();
      BlockPos var7 = partner(var5, var6, var5.method_8320(var6));
      BlockPos var4 = var7 != null && var7.method_10265(var6) < 0 ? var7 : var6;
      BlockPos var9 = var7 == null ? null : (var4.equals(var6) ? var7 : var6);
      BlockState var10 = var5.method_8320(var4);
      BlockState var3 = var9 == null ? null : var5.method_8320(var9);
      if (!var4.equals(this.pos) || var10 != this.state || var3 != this.partnerState) {
         Entity var12 = var1.method_1560();
         ShapeContext var13 = var12 == null ? ShapeContext.method_16194() : ShapeContext.method_16195(var12);
         VoxelShape var14 = var10.method_26172(var5, var4, var13);
         if (var9 != null) {
            var14 = VoxelShapes.method_1084(var14, var3.method_26172(var5, var9, var13).method_66507(var9.method_10059(var4)));
         }

         if ((var14 = var14.method_1097()).method_1110()) {
            return false;
         }

         if (!var4.equals(this.pos)) {
            this.progress = 0.0F;
         }

         this.pos = var4;
         this.state = var10;
         this.partner = var9;
         this.partnerState = var3;
         this.edges.clear();
         var14.method_1104((var1x, var3x, var5x, var7x, var9x, var11) -> this.edges.add(new double[]{var1x, var3x, var5x, var7x, var9x, var11}));
         this.boxes = var14.method_1090();
         this.localBounds = var14.method_1107();
         this.goal = this.localBounds.method_996(var4);
      }

      this.face = var2.method_17780();
      this.faceBox = this.boxOnFace(var2.method_17784().method_1023(this.pos.method_10263(), this.pos.method_10264(), this.pos.method_10260()));
      return true;
   }

   private static BlockPos partner(ClientWorld var0, BlockPos var1, BlockState var2) {
      BlockPos var3 = null;
      if (var2.method_28498(Properties.field_12533)) {
         var3 = var2.method_11654(Properties.field_12533) == DoubleBlockHalf.field_12607 ? var1.method_10084() : var1.method_10074();
      } else if (var2.method_26204() instanceof BedBlock) {
         var3 = var1.method_10093(BedBlock.method_24163(var2));
      } else if (var2.method_26204() instanceof ChestBlock && var2.method_11654(ChestBlock.field_10770) != ChestType.field_12569) {
         var3 = ChestBlock.method_72581(var1, var2);
      }

      return var3 != null && var0.method_8320(var3).method_27852(var2.method_26204()) ? var3 : null;
   }

   private Box boxOnFace(Vec3d var1) {
      double var2 = 0.001;

      for (Box var5 : this.boxes) {
         if (switch (this.face) {
            case field_11034 -> Math.abs(var1.field_1352 - var5.field_1320) < var2;
            case field_11039 -> Math.abs(var1.field_1352 - var5.field_1323) < var2;
            case field_11036 -> Math.abs(var1.field_1351 - var5.field_1325) < var2;
            case field_11033 -> Math.abs(var1.field_1351 - var5.field_1322) < var2;
            case field_11035 -> Math.abs(var1.field_1350 - var5.field_1324) < var2;
            case field_11043 -> Math.abs(var1.field_1350 - var5.field_1321) < var2;
            default -> throw new MatchException(null, null);
         } && var5.method_1014(var2).method_1006(var1)) {
            return var5;
         }
      }

      return this.localBounds;
   }

   private void updateProgress(MinecraftClient var1, float var2, boolean var3) {
      float var6 = 0.0F;
      ClientPlayerInteractionManager var7 = var1.field_1761;
      AccessorBlockOverlayMultiPlayerGameMode var4;
      BlockPos var5;
      if (var3
         && this.miningProgress.get()
         && var7 != null
         && var7.method_2923()
         && (var5 = (var4 = (AccessorBlockOverlayMultiPlayerGameMode)var7).efn$blockOverlayDestroyPos()) != null
         && (var5.equals(this.pos) || var5.equals(this.partner))) {
         var6 = MathHelper.method_15363(var4.efn$blockOverlayDestroyProgress(), 0.0F, 1.0F);
      }

      this.progress = this.progress + (var6 - this.progress) * (1.0F - (float)Math.exp(-18.0F * var2));
   }

   private void reset() {
      this.level = null;
      this.pos = null;
      this.state = null;
      this.partner = null;
      this.partnerState = null;
      this.edges.clear();
      this.boxes = List.of();
      this.localBounds = null;
      this.goal = null;
      this.box = null;
      this.face = null;
      this.faceBox = null;
      this.presence = 0.0F;
      this.progress = 0.0F;
      this.lastFrame = 0L;
   }

   private void draw() {
      this.fade = this.presence * this.presence * (3.0F - 2.0F * this.presence);
      double var1 = 0.94 + 0.06 * this.fade;
      double[] var3 = axis(
         this.box.field_1323, this.box.field_1320, this.goal.field_1323, this.goal.field_1320, this.pos.method_10263() - this.goal.field_1323, var1
      );
      double[] var4 = axis(
         this.box.field_1322, this.box.field_1325, this.goal.field_1322, this.goal.field_1325, this.pos.method_10264() - this.goal.field_1322, var1
      );
      double[] var5 = axis(
         this.box.field_1321, this.box.field_1324, this.goal.field_1321, this.goal.field_1324, this.pos.method_10260() - this.goal.field_1321, var1
      );
      this.ax = var3[0];
      this.bx = var3[1];
      this.ay = var4[0];
      this.by = var4[1];
      this.az = var5[0];
      this.bz = var5[1];
      boolean var6 = !this.style.is("Fill");
      boolean var7 = !this.style.is("Outline");
      float var8 = this.fillOpacity.get() / 100.0F;
      if (var7) {
         if (this.faceOnly.get()) {
            this.faceFill(this.faceBox, this.face, var8, 0.002);
         } else {
            for (Box var10 : this.boxes) {
               this.boxFill(var10, var8, 0.002);
            }
         }
      }

      if (this.progress > 0.01F) {
         this.progressFill(Math.min(0.45F, var8 * 1.4F + 0.16F));
      }

      if (var6) {
         List var11 = this.edges;
         if (this.faceOnly.get()) {
            var11 = faceEdges(this.faceBox, this.face);
         }

         this.outline(var11);
      }
   }

   private static double[] axis(double var0, double var2, double var4, double var6, double var8, double var10) {
      double var12 = var6 - var4;
      double var14 = var12 > 1.0E-6 ? (var2 - var0) / var12 : 1.0;
      double var16 = (var0 + var2) * 0.5;
      double var18 = var16 * (1.0 - var10) + (var0 + var8 * var14) * var10;
      return new double[]{var18, var14 * var10};
   }

   private Vec3d world(double var1, double var3, double var5) {
      return new Vec3d(this.ax + this.bx * var1, this.ay + this.by * var3, this.az + this.bz * var5);
   }

   private void outline(List<double[]> var1) {
      boolean var2 = !this.colorMode.is("Solid");
      ArrayList var3 = new ArrayList(var1.size() * 4);
      ArrayList var4 = new ArrayList(var1.size() * 4);

      for (double[] var6 : var1) {
         double var7 = Math.sqrt(sq(var6[3] - var6[0]) + sq(var6[4] - var6[1]) + sq(var6[5] - var6[2]));
         int var9 = var2 ? MathHelper.method_15340((int)Math.ceil(var7 / 0.125), 1, 16) : 1;

         for (int var10 = 0; var10 < var9; var10++) {
            double var11 = (double)var10 / var9;
            double var13 = (double)(var10 + 1) / var9;
            double var15 = (var11 + var13) * 0.5;
            var3.add(
               new double[]{
                  MathHelper.method_16436(var11, var6[0], var6[3]),
                  MathHelper.method_16436(var11, var6[1], var6[4]),
                  MathHelper.method_16436(var11, var6[2], var6[5]),
                  MathHelper.method_16436(var13, var6[0], var6[3]),
                  MathHelper.method_16436(var13, var6[1], var6[4]),
                  MathHelper.method_16436(var13, var6[2], var6[5])
               }
            );
            var4.add(
               this.colorAt(
                  MathHelper.method_16436(var15, var6[0], var6[3]),
                  MathHelper.method_16436(var15, var6[1], var6[4]),
                  MathHelper.method_16436(var15, var6[2], var6[5])
               )
            );
         }
      }

      float var17 = this.width.get();
      if (this.glow.get()) {
         for (int var18 = 0; var18 < var3.size(); var18++) {
            double[] var20 = (double[])var3.get(var18);
            int var8 = (Integer)var4.get(var18);
            this.line(var20, ColorHelper.method_75601(var8, 0.34F * this.fade), var17 + 3.0F, 0.002);
            this.line(var20, ColorHelper.method_75601(var8, 0.13F * this.fade), var17 + 8.0F, 0.002);
         }
      }

      for (int var19 = 0; var19 < var3.size(); var19++) {
         this.line((double[])var3.get(var19), ColorHelper.method_75601((Integer)var4.get(var19), this.fade), var17, 0.004);
      }
   }

   private void line(double[] var1, int var2, float var3, double var4) {
      VisibilityConfigurable var6 = GizmoDrawing.method_75546(
         this.lift(this.world(var1[0], var1[1], var1[2]), var4), this.lift(this.world(var1[3], var1[4], var1[5]), var4), var2, var3
      );
      if (this.seeThrough.get()) {
         var6.method_75533();
      }
   }

   private Vec3d lift(Vec3d var1, double var2) {
      Vec3d var4 = this.eye.method_1020(var1);
      double var5 = var4.method_1033();
      return var5 < 1.0E-6 ? var1 : var1.method_1019(var4.method_1021(var2 / var5));
   }

   private void boxFill(Box var1, float var2, double var3) {
      for (Direction var8 : Direction.values()) {
         this.faceFill(var1, var8, var2, var3);
      }
   }

   private void faceFill(Box var1, Direction var2, float var3, double var4) {
      Vec3d var6 = this.world(var1.field_1323, var1.field_1322, var1.field_1321).method_1023(var4, var4, var4);
      Vec3d var7 = this.world(var1.field_1320, var1.field_1325, var1.field_1324).method_1031(var4, var4, var4);
      Vec3d var8 = faceCentre(var1, var2);
      int var9 = this.colorAt(var8.field_1352, var8.field_1351, var8.field_1350);
      int var10 = MathHelper.method_15340(Math.round(var3 * this.fade * 255.0F), 0, 255) << 24 | var9 & 16777215;
      VisibilityConfigurable var11 = GizmoDrawing.method_75548(var6, var7, var2, DrawStyle.method_75539(var10));
      if (this.seeThrough.get()) {
         var11.method_75533();
      }
   }

   private void progressFill(float var1) {
      double var2 = 0.004;
      if (this.faceOnly.get()) {
         Box var8 = this.faceBox;
         Box var5 = this.face.method_10166() == Axis.field_11052
            ? var8.method_35580(var8.method_17939() * (1.0F - this.progress) * 0.5, 0.0, var8.method_17941() * (1.0F - this.progress) * 0.5)
            : new Box(
               var8.field_1323, var8.field_1322, var8.field_1321, var8.field_1320, var8.field_1322 + var8.method_17940() * this.progress, var8.field_1324
            );
         this.faceFill(var5, this.face, var1, var2);
      } else {
         double var4 = this.localBounds.field_1322 + this.localBounds.method_17940() * this.progress;

         for (Box var7 : this.boxes) {
            if (!(var7.field_1322 >= var4)) {
               this.boxFill(
                  new Box(var7.field_1323, var7.field_1322, var7.field_1321, var7.field_1320, Math.min(var7.field_1325, var4), var7.field_1324), var1, var2
               );
            }
         }
      }
   }

   private int colorAt(double var1, double var3, double var5) {
      boolean var10 = this.colorMode.is("Theme");
      int var9 = var10 ? CosmeticAccent.argb() : ThemeTint.world(this.color.argb());
      if (this.colorMode.is("Solid")) {
         return var9;
      } else {
         double var12 = (
               norm(var1, this.localBounds.field_1323, this.localBounds.field_1320)
                  + norm(var3, this.localBounds.field_1322, this.localBounds.field_1325)
                  + norm(var5, this.localBounds.field_1321, this.localBounds.field_1324)
            )
            / 3.0;
         double var7 = this.flow.get() ? this.time * 0.3499999940395355 : 0.0;
         if (this.colorMode.is("Rainbow")) {
            double var19 = var12 * 0.5 - var7;
            return ThemeTint.world(var9 & 0xFF000000 | MathHelper.method_15369((float)(var19 - Math.floor(var19)), 0.65F, 1.0F) & 16777215);
         } else {
            double var16 = this.flow.get() ? 0.5 - 0.5 * Math.cos(6.283185307179586 * (var12 - var7)) : var12;
            int var18 = var10 ? ColorHelper.method_61319(0.55F, var9, -1) : ThemeTint.world(this.secondColor.argb());
            return ColorHelper.method_61319((float)var16, var9, var18);
         }
      }
   }

   private static double norm(double var0, double var2, double var4) {
      return var4 - var2 < 1.0E-6 ? 0.5 : (var0 - var2) / (var4 - var2);
   }

   private static Vec3d faceCentre(Box var0, Direction var1) {
      Vec3d var2 = var0.method_1005();

      return switch (var1) {
         case field_11034 -> new Vec3d(var0.field_1320, var2.field_1351, var2.field_1350);
         case field_11039 -> new Vec3d(var0.field_1323, var2.field_1351, var2.field_1350);
         case field_11036 -> new Vec3d(var2.field_1352, var0.field_1325, var2.field_1350);
         case field_11033 -> new Vec3d(var2.field_1352, var0.field_1322, var2.field_1350);
         case field_11035 -> new Vec3d(var2.field_1352, var2.field_1351, var0.field_1324);
         case field_11043 -> new Vec3d(var2.field_1352, var2.field_1351, var0.field_1321);
         default -> throw new MatchException(null, null);
      };
   }

   private static List<double[]> faceEdges(Box var0, Direction var1) {
      double[][] var2;
      switch (var1.method_10166()) {
         case field_11048:
            double var9 = var1 == Direction.field_11034 ? var0.field_1320 : var0.field_1323;
            double[][] var12 = new double[][]{
               {var9, var0.field_1322, var0.field_1321}, {var9, var0.field_1325, var0.field_1321}, {var9, var0.field_1325, var0.field_1324}, null
            };
            var2 = var12;
            var12[3] = new double[]{var9, var0.field_1322, var0.field_1324};
            break;
         case field_11052:
            double var8 = var1 == Direction.field_11036 ? var0.field_1325 : var0.field_1322;
            double[][] var11 = new double[][]{
               {var0.field_1323, var8, var0.field_1321}, {var0.field_1320, var8, var0.field_1321}, {var0.field_1320, var8, var0.field_1324}, null
            };
            var2 = var11;
            var11[3] = new double[]{var0.field_1323, var8, var0.field_1324};
            break;
         case field_11051:
            double var3 = var1 == Direction.field_11035 ? var0.field_1324 : var0.field_1321;
            double[][] var5 = new double[][]{
               {var0.field_1323, var0.field_1322, var3}, {var0.field_1320, var0.field_1322, var3}, {var0.field_1320, var0.field_1325, var3}, null
            };
            var2 = var5;
            var5[3] = new double[]{var0.field_1323, var0.field_1325, var3};
            break;
         default:
            throw new MatchException(null, null);
      }

      double[][] var10 = var2;
      ArrayList var4 = new ArrayList(4);

      for (int var13 = 0; var13 < 4; var13++) {
         double[] var6 = var10[var13];
         double[] var7 = var10[(var13 + 1) % 4];
         var4.add(new double[]{var6[0], var6[1], var6[2], var7[0], var7[1], var7[2]});
      }

      return var4;
   }

   private static Box lerp(Box var0, Box var1, float var2) {
      return new Box(
         MathHelper.method_16436(var2, var0.field_1323, var1.field_1323),
         MathHelper.method_16436(var2, var0.field_1322, var1.field_1322),
         MathHelper.method_16436(var2, var0.field_1321, var1.field_1321),
         MathHelper.method_16436(var2, var0.field_1320, var1.field_1320),
         MathHelper.method_16436(var2, var0.field_1325, var1.field_1325),
         MathHelper.method_16436(var2, var0.field_1324, var1.field_1324)
      );
   }

   private static double sq(double var0) {
      return var0 * var0;
   }
}
