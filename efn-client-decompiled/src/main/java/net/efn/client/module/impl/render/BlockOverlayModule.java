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
   public void onRender3D(Render3DEvent render3DEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      if (var2.world != null && var2.player != null) {
         if (var2.world != this.level) {
            this.reset();
            this.level = var2.world;
         }

         long var3 = System.nanoTime();
         float var5 = this.lastFrame == 0L ? 0.0F : Math.min((float)(var3 - this.lastFrame) / 1.0E9F, 0.1F);
         this.lastFrame = var3;
         BlockHitResult var6 = target(var2);
         if (var6 != null && this.adopt(var2, var6)) {
            this.box = this.box != null && !(this.presence < 0.02F) && this.smooth.get() && !(this.box.getCenter().distanceTo(this.goal.getCenter()) > 6.0)
               ? lerp(this.box, this.goal, 1.0F - (float)Math.exp(-this.smoothSpeed.get() * var5))
               : this.goal;
            this.presence = Math.min(1.0F, this.presence + var5 / 0.12F);
         } else {
            var6 = null;
            this.presence = Math.max(0.0F, this.presence - var5 / 0.2F);
            if (this.presence == 0.0F) {
               this.reset();
               this.level = var2.world;
               return;
            }
         }

         if (this.box != null) {
            this.updateProgress(var2, var5, var6 != null);
            this.eye = var2.gameRenderer.getCamera().getCameraPos();
            this.time = var3 % 3600000000000L / 1.0E9;
            this.draw();
         }
      } else {
         this.reset();
      }
   }

   private static BlockHitResult target(MinecraftClient minecraftClient) {
      HitResult var2 = minecraftClient.crosshairTarget;
      BlockHitResult var1;
      if (!(var2 instanceof BlockHitResult) || (var1 = (BlockHitResult)var2).getType() != Type.BLOCK || var1.isInsideBlock()) {
         return null;
      } else if (!((AccessorGameRenderer)minecraftClient.gameRenderer).efn$shouldRenderBlockOutline()) {
         return null;
      } else {
         BlockPos var3 = var1.getBlockPos();
         return !minecraftClient.world.getBlockState(var3).isAir() && minecraftClient.world.getWorldBorder().contains(var3) ? var1 : null;
      }
   }

   private boolean adopt(MinecraftClient minecraftClient, BlockHitResult blockHitResult) {
      ClientWorld var5 = minecraftClient.world;
      BlockPos var6 = blockHitResult.getBlockPos();
      BlockPos var7 = partner(var5, var6, var5.getBlockState(var6));
      BlockPos var4 = var7 != null && var7.compareTo(var6) < 0 ? var7 : var6;
      BlockPos var9 = var7 == null ? null : (var4.equals(var6) ? var7 : var6);
      BlockState var10 = var5.getBlockState(var4);
      BlockState var3 = var9 == null ? null : var5.getBlockState(var9);
      if (!var4.equals(this.pos) || var10 != this.state || var3 != this.partnerState) {
         Entity var12 = minecraftClient.getCameraEntity();
         ShapeContext var13 = var12 == null ? ShapeContext.absent() : ShapeContext.of(var12);
         VoxelShape var14 = var10.getOutlineShape(var5, var4, var13);
         if (var9 != null) {
            var14 = VoxelShapes.union(var14, var3.getOutlineShape(var5, var9, var13).offset(var9.subtract(var4)));
         }

         if ((var14 = var14.simplify()).isEmpty()) {
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
         var14.forEachEdge((d, e, f, g, h, i) -> this.edges.add(new double[]{d, e, f, g, h, i}));
         this.boxes = var14.getBoundingBoxes();
         this.localBounds = var14.getBoundingBox();
         this.goal = this.localBounds.offset(var4);
      }

      this.face = blockHitResult.getSide();
      this.faceBox = this.boxOnFace(blockHitResult.getPos().subtract(this.pos.getX(), this.pos.getY(), this.pos.getZ()));
      return true;
   }

   private static BlockPos partner(ClientWorld clientWorld, BlockPos blockPos, BlockState blockState) {
      BlockPos var3 = null;
      if (blockState.contains(Properties.DOUBLE_BLOCK_HALF)) {
         var3 = blockState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? blockPos.up() : blockPos.down();
      } else if (blockState.getBlock() instanceof BedBlock) {
         var3 = blockPos.offset(BedBlock.getOppositePartDirection(blockState));
      } else if (blockState.getBlock() instanceof ChestBlock && blockState.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
         var3 = ChestBlock.getPosInFrontOf(blockPos, blockState);
      }

      return var3 != null && clientWorld.getBlockState(var3).isOf(blockState.getBlock()) ? var3 : null;
   }

   private Box boxOnFace(Vec3d vec3d) {
      double var2 = 0.001;

      for (Box var5 : this.boxes) {
         if (switch (this.face) {
            case EAST -> Math.abs(vec3d.x - var5.maxX) < var2;
            case WEST -> Math.abs(vec3d.x - var5.minX) < var2;
            case UP -> Math.abs(vec3d.y - var5.maxY) < var2;
            case DOWN -> Math.abs(vec3d.y - var5.minY) < var2;
            case SOUTH -> Math.abs(vec3d.z - var5.maxZ) < var2;
            case NORTH -> Math.abs(vec3d.z - var5.minZ) < var2;
         } && var5.expand(var2).contains(vec3d)) {
            return var5;
         }
      }

      return this.localBounds;
   }

   private void updateProgress(MinecraftClient minecraftClient, float f, boolean bl) {
      float var6 = 0.0F;
      ClientPlayerInteractionManager var7 = minecraftClient.interactionManager;
      AccessorBlockOverlayMultiPlayerGameMode var4;
      BlockPos var5;
      if (bl
         && this.miningProgress.get()
         && var7 != null
         && var7.isBreakingBlock()
         && (var5 = (var4 = (AccessorBlockOverlayMultiPlayerGameMode)var7).efn$blockOverlayDestroyPos()) != null
         && (var5.equals(this.pos) || var5.equals(this.partner))) {
         var6 = MathHelper.clamp(var4.efn$blockOverlayDestroyProgress(), 0.0F, 1.0F);
      }

      this.progress = this.progress + (var6 - this.progress) * (1.0F - (float)Math.exp(-18.0F * f));
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
      double[] var3 = axis(this.box.minX, this.box.maxX, this.goal.minX, this.goal.maxX, this.pos.getX() - this.goal.minX, var1);
      double[] var4 = axis(this.box.minY, this.box.maxY, this.goal.minY, this.goal.maxY, this.pos.getY() - this.goal.minY, var1);
      double[] var5 = axis(this.box.minZ, this.box.maxZ, this.goal.minZ, this.goal.maxZ, this.pos.getZ() - this.goal.minZ, var1);
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

   private static double[] axis(double d, double e, double f, double g, double h, double i) {
      double var12 = g - f;
      double var14 = var12 > 1.0E-6 ? (e - d) / var12 : 1.0;
      double var16 = (d + e) * 0.5;
      double var18 = var16 * (1.0 - i) + (d + h * var14) * i;
      return new double[]{var18, var14 * i};
   }

   private Vec3d world(double d, double e, double f) {
      return new Vec3d(this.ax + this.bx * d, this.ay + this.by * e, this.az + this.bz * f);
   }

   private void outline(List<double[]> list) {
      boolean var2 = !this.colorMode.is("Solid");
      ArrayList var3 = new ArrayList(list.size() * 4);
      ArrayList var4 = new ArrayList(list.size() * 4);

      for (double[] var6 : list) {
         double var7 = Math.sqrt(sq(var6[3] - var6[0]) + sq(var6[4] - var6[1]) + sq(var6[5] - var6[2]));
         int var9 = var2 ? MathHelper.clamp((int)Math.ceil(var7 / 0.125), 1, 16) : 1;

         for (int var10 = 0; var10 < var9; var10++) {
            double var11 = (double)var10 / var9;
            double var13 = (double)(var10 + 1) / var9;
            double var15 = (var11 + var13) * 0.5;
            var3.add(
               new double[]{
                  MathHelper.lerp(var11, var6[0], var6[3]),
                  MathHelper.lerp(var11, var6[1], var6[4]),
                  MathHelper.lerp(var11, var6[2], var6[5]),
                  MathHelper.lerp(var13, var6[0], var6[3]),
                  MathHelper.lerp(var13, var6[1], var6[4]),
                  MathHelper.lerp(var13, var6[2], var6[5])
               }
            );
            var4.add(this.colorAt(MathHelper.lerp(var15, var6[0], var6[3]), MathHelper.lerp(var15, var6[1], var6[4]), MathHelper.lerp(var15, var6[2], var6[5])));
         }
      }

      float var17 = this.width.get();
      if (this.glow.get()) {
         for (int var18 = 0; var18 < var3.size(); var18++) {
            double[] var20 = (double[])var3.get(var18);
            int var8 = (Integer)var4.get(var18);
            this.line(var20, ColorHelper.scaleAlpha(var8, 0.34F * this.fade), var17 + 3.0F, 0.002);
            this.line(var20, ColorHelper.scaleAlpha(var8, 0.13F * this.fade), var17 + 8.0F, 0.002);
         }
      }

      for (int var19 = 0; var19 < var3.size(); var19++) {
         this.line((double[])var3.get(var19), ColorHelper.scaleAlpha((Integer)var4.get(var19), this.fade), var17, 0.004);
      }
   }

   private void line(double[] ds, int i, float f, double d) {
      VisibilityConfigurable var6 = GizmoDrawing.line(this.lift(this.world(ds[0], ds[1], ds[2]), d), this.lift(this.world(ds[3], ds[4], ds[5]), d), i, f);
      if (this.seeThrough.get()) {
         var6.ignoreOcclusion();
      }
   }

   private Vec3d lift(Vec3d vec3d, double d) {
      Vec3d var4 = this.eye.subtract(vec3d);
      double var5 = var4.length();
      return var5 < 1.0E-6 ? vec3d : vec3d.add(var4.multiply(d / var5));
   }

   private void boxFill(Box box, float f, double d) {
      for (Direction var8 : Direction.values()) {
         this.faceFill(box, var8, f, d);
      }
   }

   private void faceFill(Box box, Direction direction, float f, double d) {
      Vec3d var6 = this.world(box.minX, box.minY, box.minZ).subtract(d, d, d);
      Vec3d var7 = this.world(box.maxX, box.maxY, box.maxZ).add(d, d, d);
      Vec3d var8 = faceCentre(box, direction);
      int var9 = this.colorAt(var8.x, var8.y, var8.z);
      int var10 = MathHelper.clamp(Math.round(f * this.fade * 255.0F), 0, 255) << 24 | var9 & 16777215;
      VisibilityConfigurable var11 = GizmoDrawing.face(var6, var7, direction, DrawStyle.filled(var10));
      if (this.seeThrough.get()) {
         var11.ignoreOcclusion();
      }
   }

   private void progressFill(float f) {
      double var2 = 0.004;
      if (this.faceOnly.get()) {
         Box var8 = this.faceBox;
         Box var5 = this.face.getAxis() == Axis.Y
            ? var8.contract(var8.getLengthX() * (1.0F - this.progress) * 0.5, 0.0, var8.getLengthZ() * (1.0F - this.progress) * 0.5)
            : new Box(var8.minX, var8.minY, var8.minZ, var8.maxX, var8.minY + var8.getLengthY() * this.progress, var8.maxZ);
         this.faceFill(var5, this.face, f, var2);
      } else {
         double var4 = this.localBounds.minY + this.localBounds.getLengthY() * this.progress;

         for (Box var7 : this.boxes) {
            if (!(var7.minY >= var4)) {
               this.boxFill(new Box(var7.minX, var7.minY, var7.minZ, var7.maxX, Math.min(var7.maxY, var4), var7.maxZ), f, var2);
            }
         }
      }
   }

   private int colorAt(double d, double e, double f) {
      boolean var10 = this.colorMode.is("Theme");
      int var9 = var10 ? CosmeticAccent.argb() : ThemeTint.world(this.color.argb());
      if (this.colorMode.is("Solid")) {
         return var9;
      } else {
         double var12 = (
               norm(d, this.localBounds.minX, this.localBounds.maxX)
                  + norm(e, this.localBounds.minY, this.localBounds.maxY)
                  + norm(f, this.localBounds.minZ, this.localBounds.maxZ)
            )
            / 3.0;
         double var7 = this.flow.get() ? this.time * 0.3499999940395355 : 0.0;
         if (this.colorMode.is("Rainbow")) {
            double var19 = var12 * 0.5 - var7;
            return ThemeTint.world(var9 & 0xFF000000 | MathHelper.hsvToRgb((float)(var19 - Math.floor(var19)), 0.65F, 1.0F) & 16777215);
         } else {
            double var16 = this.flow.get() ? 0.5 - 0.5 * Math.cos(6.283185307179586 * (var12 - var7)) : var12;
            int var18 = var10 ? ColorHelper.lerp(0.55F, var9, -1) : ThemeTint.world(this.secondColor.argb());
            return ColorHelper.lerp((float)var16, var9, var18);
         }
      }
   }

   private static double norm(double d, double e, double f) {
      return f - e < 1.0E-6 ? 0.5 : (d - e) / (f - e);
   }

   private static Vec3d faceCentre(Box box, Direction direction) {
      Vec3d var2 = box.getCenter();

      return switch (direction) {
         case EAST -> new Vec3d(box.maxX, var2.y, var2.z);
         case WEST -> new Vec3d(box.minX, var2.y, var2.z);
         case UP -> new Vec3d(var2.x, box.maxY, var2.z);
         case DOWN -> new Vec3d(var2.x, box.minY, var2.z);
         case SOUTH -> new Vec3d(var2.x, var2.y, box.maxZ);
         case NORTH -> new Vec3d(var2.x, var2.y, box.minZ);
      };
   }

   private static List<double[]> faceEdges(Box box, Direction direction) {
      double[][] var2;
      switch (direction.getAxis()) {
         case X:
            double var9 = direction == Direction.EAST ? box.maxX : box.minX;
            double[][] var12 = new double[][]{{var9, box.minY, box.minZ}, {var9, box.maxY, box.minZ}, {var9, box.maxY, box.maxZ}, null};
            var2 = var12;
            var12[3] = new double[]{var9, box.minY, box.maxZ};
            break;
         case Y:
            double var8 = direction == Direction.UP ? box.maxY : box.minY;
            double[][] var11 = new double[][]{{box.minX, var8, box.minZ}, {box.maxX, var8, box.minZ}, {box.maxX, var8, box.maxZ}, null};
            var2 = var11;
            var11[3] = new double[]{box.minX, var8, box.maxZ};
            break;
         case Z:
            double var3 = direction == Direction.SOUTH ? box.maxZ : box.minZ;
            double[][] var5 = new double[][]{{box.minX, box.minY, var3}, {box.maxX, box.minY, var3}, {box.maxX, box.maxY, var3}, null};
            var2 = var5;
            var5[3] = new double[]{box.minX, box.maxY, var3};
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

   private static Box lerp(Box box, Box box2, float f) {
      return new Box(
         MathHelper.lerp((double)f, box.minX, box2.minX),
         MathHelper.lerp((double)f, box.minY, box2.minY),
         MathHelper.lerp((double)f, box.minZ, box2.minZ),
         MathHelper.lerp((double)f, box.maxX, box2.maxX),
         MathHelper.lerp((double)f, box.maxY, box2.maxY),
         MathHelper.lerp((double)f, box.maxZ, box2.maxZ)
      );
   }

   private static double sq(double d) {
      return d * d;
   }
}
