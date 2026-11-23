package net.efn.client.module.impl.render;

import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.WorldLabel;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.efn.client.util.Ballistics;
import net.efn.client.util.combat.CombatTarget;
import net.efn.client.util.glyph.GlyphCanvas;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.VisibilityConfigurable;

@Environment(EnvType.CLIENT)
public final class TrajectoriesModule extends ClientModule implements HudOverlayProvider {
   private static final String PATH_BEADS = "Beads";
   private static final String PATH_DASHES = "Dashes";
   private static final String PATH_SOLID = "Solid";
   private static final String COLOR_THEME = "Theme";
   private static final String COLOR_MANUAL = "Manual";
   private static final String MARKER_BOLT = "Bolt";
   private static final String MARKER_SUN = "Ring";
   private static final String MARKER_NONE = "None";
   private static final int WHITE = -1;
   private static final float HEAD_LIFT = 0.35F;
   private static final float SHEEN_LIFT = 0.8F;
   private static final float PLATE_LIFT = 0.55F;
   private static final float HIT_LIFT = 0.5F;
   private static final int VOLLEY_CAP = 3;
   private static final int TRACK_CAP = 24;
   private static final int TRACK_LIFETIME = 200;
   private static final double RESTING_SPEED_SQR = 1.0E-4;
   private static final double AIM_DRIFT = 0.001;
   private static final double AIM_SWAY = 5.0E-4;
   private static final float BOW_MIN_PULL = 0.1F;
   private static final float BOW_SPEED = 3.0F;
   private static final int TRIDENT_WINDUP = 10;
   private static final float CROSSBOW_ARROW_SPEED = 3.15F;
   private static final float CROSSBOW_ROCKET_SPEED = 1.6F;
   private static final float MULTISHOT_FAN = 10.0F;
   private static final float BOTTLE_SPEED = 0.7F;
   private static final float LOB_TILT = -20.0F;
   private static final double MUZZLE_DROP = 0.1;
   private static final double ROCKET_DROP = 0.15;
   private static final int ROCKET_FUSE_SLACK = 6;
   private static final double BEAD_PERIOD = 0.42;
   private static final double BEAD_ANGLE = 0.011;
   private static final double BEAD_MIN = 0.035;
   private static final double BEAD_MAX = 0.2;
   private static final double DASH_PERIOD = 0.72;
   private static final double DASH_LENGTH = 0.44;
   private static final double SOLID_PIECE = 0.6;
   private static final double FLOW_SPEED = 1.3;
   private static final double FADE_IN = 1.4;
   private static final double SHEEN_SPEED = 9.0;
   private static final double SHEEN_REACH = 1.6;
   private static final double SHEEN_REST = 14.0;
   private static final float HALO_ALPHA = 0.22F;
   private static final float HALO_WIDTH = 2.8F;
   private static final double HALO_NEAR = 24.0;
   private static final double HALO_FAR = 40.0;
   private static final int AIM_PIECES = 200;
   private static final int TRACK_PIECES = 110;
   private static final float GHOST_ALPHA = 0.38F;
   private static final float STRAY_ALPHA = 0.7F;
   private static final float FLYING_ALPHA = 0.85F;
   private static final float UNFURL_SECONDS = 0.32F;
   private static final double MARKER_UNIT = 0.5;
   private static final double MARKER_LIFT = 0.025;
   private static final int RIM_STEPS = 24;
   private static final int ORBIT_ARCS = 10;
   private static final int SUN_RAYS = 12;
   private static final long TARGET_MEMORY_MS = 6000L;
   private static final long LABEL_TTL_NANOS = 250000000L;
   private static final long EPOCH = System.nanoTime();
   private static final String PEARL_PREDICT_ID = "predict";
   private static final String PEARL_OTHERS_ID = "others-pearls";
   private static final double[] RIM_COS = new double[25];
   private static final double[] RIM_SIN = new double[25];
   private static final double DIAGONAL = 1.0 / Math.sqrt(3.0);
   private static final double[][] BURST_RAYS = new double[][]{
      {1.0, 0.0, 0.0},
      {-1.0, 0.0, 0.0},
      {0.0, 1.0, 0.0},
      {0.0, -1.0, 0.0},
      {0.0, 0.0, 1.0},
      {0.0, 0.0, -1.0},
      {DIAGONAL, DIAGONAL, DIAGONAL},
      {-DIAGONAL, DIAGONAL, DIAGONAL},
      {DIAGONAL, -DIAGONAL, DIAGONAL},
      {DIAGONAL, DIAGONAL, -DIAGONAL},
      {-DIAGONAL, -DIAGONAL, DIAGONAL},
      {-DIAGONAL, DIAGONAL, -DIAGONAL},
      {DIAGONAL, -DIAGONAL, -DIAGONAL},
      {-DIAGONAL, -DIAGONAL, -DIAGONAL}
   };
   private final BooleanSetting aimPreview = this.addSetting("aim-preview", new BooleanSetting("Aim Preview", true));
   private final BooleanSetting ghostArc = this.addSetting("ghost-arc", new BooleanSetting("Ghost Arc", true));
   private final BooleanSetting inFlight = this.addSetting("in-flight", new BooleanSetting("In Flight", true));
   private final BooleanSetting othersShots = this.addSetting("others-shots", new BooleanSetting("Others' Shots", true));
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 64.0F, 16.0F, 160.0F).step(8.0F));
   private final BooleanSetting bows = this.addSetting("bows", new BooleanSetting("Bows & Crossbows", true));
   private final BooleanSetting tridents = this.addSetting("tridents", new BooleanSetting("Tridents", true));
   private final BooleanSetting throwables = this.addSetting("throwables", new BooleanSetting("Throwables", true));
   private final BooleanSetting pearls = this.addSetting("pearls", new BooleanSetting("Ender Pearls", true));
   private final ModeSetting pathStyle = this.addSetting("path-style", new ModeSetting("Path", "Beads", "Beads", "Dashes", "Solid"));
   private final BooleanSetting flow = this.addSetting("flow", new BooleanSetting("Flow", true));
   private final ModeSetting colorMode = this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));
   private final ColorSetting pathColor = this.addSetting("path-color", new ColorSetting("Path Color", -13338378));
   private final NumberSetting thickness = this.addSetting("thickness", new NumberSetting("Thickness", 2.25F, 0.5F, 5.0F).step(0.25F));
   private final BooleanSetting halo = this.addSetting("halo", new BooleanSetting("Halo", true));
   private final BooleanSetting throughWalls = this.addSetting("through-walls", new BooleanSetting("Through Walls", true));
   private final ModeSetting marker = this.addSetting("marker", new ModeSetting("Marker", "Bolt", "Bolt", "Ring", "None"));
   private final NumberSetting markerScale = this.addSetting("marker-scale", new NumberSetting("Marker Scale", 1.0F, 0.5F, 2.0F).step(0.1F));
   private final BooleanSetting targetBox = this.addSetting("target-box", new BooleanSetting("Target Box", true));
   private final ColorSetting hitColor = this.addSetting("hit-color", new ColorSetting("Hit Color", -6635270));
   private final BooleanSetting flightTime = this.addSetting("flight-time", new BooleanSetting("Flight Time", true));
   private final TrajectoriesModule.Aim[] volley = new TrajectoriesModule.Aim[3];
   private int volleySize;
   private boolean volleyGhost;
   private Item aimItem;
   private long aimSince = -1L;
   private long markerSince = -1L;
   private final Int2ObjectOpenHashMap<TrajectoriesModule.Track> tracks = new Int2ObjectOpenHashMap();
   private final ArrayDeque<TrajectoriesModule.Track> spareTracks = new ArrayDeque<>();
   private int sweep;
   private ClientWorld level;
   private final GlyphCanvas canvas = new GlyphCanvas();
   private final List<TrajectoriesModule.Label> labels = new ArrayList<>();
   private long labelsAt;
   private int headTint;
   private int tailTint;
   private int haloTint;
   private int markTint;
   private int ringTint;
   private int sheenTint;
   private int plateTint;
   private int hitTint;
   private boolean onTop;
   private Vec3d camera = Vec3d.field_1353;
   private double strokeTotal;
   private double strokeSheen;
   private float strokeAlpha;
   private float strokeWidth;
   private boolean strokeFade;
   private boolean strokeHalo;
   private double gx;
   private double gy;
   private double gz;
   private double rx;
   private double ry;
   private double rz;
   private double ux;
   private double uy;
   private double uz;

   public TrajectoriesModule() {
      super("trajectories", "Trajectories", "Shows where your projectiles will land.", ModuleCategory.RENDER);

      for (int var1 = 0; var1 < 3; var1++) {
         this.volley[var1] = new TrajectoriesModule.Aim();
      }

      this.ghostArc.visibleWhen(this.aimPreview::get);
      this.othersShots.visibleWhen(this.inFlight::get);
      this.range.visibleWhen(this.inFlight::get);
      this.flow.visibleWhen(() -> !this.pathStyle.is("Solid"));
      this.pathColor.visibleWhen(() -> this.colorMode.is("Manual"));
      this.markerScale.visibleWhen(() -> !this.marker.is("None"));
      this.hitColor.visibleWhen(() -> this.targetBox.get() && this.colorMode.is("Manual"));
   }

   @Override
   protected void onEnable() {
      this.reset();
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      this.on(Render3DEvent.TYPE, this::onRender);
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.forgetAim();
      this.dropTracks();
      this.labels.clear();
      this.level = null;
   }

   private void syncLevel(ClientWorld var1) {
      if (var1 != this.level) {
         this.reset();
         this.level = var1;
      }
   }

   private void forgetAim() {
      for (TrajectoriesModule.Aim var4 : this.volley) {
         var4.forget();
      }

      this.volleySize = 0;
      this.volleyGhost = false;
      this.aimItem = null;
      this.aimSince = -1L;
      this.markerSince = -1L;
   }

   private void dropTracks() {
      if (!this.tracks.isEmpty()) {
         ObjectIterator var1 = this.tracks.values().iterator();

         while (var1.hasNext()) {
            TrajectoriesModule.Track var2 = (TrajectoriesModule.Track)var1.next();
            this.recycle(var2);
         }

         this.tracks.clear();
      }
   }

   private void recycle(TrajectoriesModule.Track var1) {
      var1.shot = null;
      var1.departed = false;
      var1.forget();
      if (this.spareTracks.size() < 24) {
         this.spareTracks.push(var1);
      }
   }

   private void onTick(TickEvent var1) {
      this.sweep++;
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientWorld var3 = var2.field_1687;
      ClientPlayerEntity var4 = var2.field_1724;
      this.syncLevel(var3);
      if (var3 != null && var4 != null && this.inFlight.get()) {
         double var5 = this.range.get() * this.range.get();
         PearlTrajectoryModule var7 = activePearlModule();
         boolean var8 = var7 != null && pearlToggle(var7, "others-pearls");
         boolean var9 = this.othersShots.get();
         boolean var10 = this.flightTime.get();
         Vec3d var11 = var4.method_33571();
         int var12 = 24;

         for (Entity var14 : var3.method_18112()) {
            ProjectileEntity var19;
            if (var14 instanceof ProjectileEntity && !(var19 = (ProjectileEntity)var14).method_31481()) {
               Entity var20 = var19.method_24921();
               boolean var18 = var20 == var4;
               Vec3d var15;
               Ballistics.Body var17;
               if ((var18 || var9)
                  && (var17 = this.bodyOf(var19, var7 != null && (var20 == null || var18 || var8))) != null
                  && !(var19.method_5858(var4) > var5)
                  && !((var15 = var19.method_18798()).method_1027() < 1.0E-4)
                  && !resting(var19)) {
                  if (var12-- == 0) {
                     break;
                  }

                  TrajectoriesModule.Track var22 = (TrajectoriesModule.Track)this.tracks.get(var19.method_5628());
                  if (var22 == null) {
                     var22 = this.spareTracks.isEmpty() ? new TrajectoriesModule.Track() : this.spareTracks.pop();
                     this.tracks.put(var19.method_5628(), var22);
                  }

                  var22.shot = var19;
                  var22.seen = this.sweep;
                  var22.flight
                     .launch(var17, var19.method_73189(), var15)
                     .bounds(var19.method_17681(), var19.method_17682())
                     .gravity(var19.method_5740() ? 0.0 : var17.gravity())
                     .wet(var19.method_5799())
                     .age(var19.field_6012)
                     .lifetime(200)
                     .ignoring(var20, var19)
                     .leftShooter(var22.departed)
                     .solve(var3);
                  var22.departed = var22.flight.hasLeftShooter();
                  var22.caption(var11, var10 && (var18 || var22.flight.outcome() == Ballistics.Outcome.ENTITY));
               }
            }
         }

         if (!this.tracks.isEmpty()) {
            this.tracks.values().removeIf(this::stale);
         }
      } else {
         this.dropTracks();
      }
   }

   private boolean stale(TrajectoriesModule.Track var1) {
      if (var1.seen == this.sweep) {
         return false;
      } else {
         this.recycle(var1);
         return true;
      }
   }

   private static boolean resting(ProjectileEntity var0) {
      double var1 = var0.method_23317() - var0.field_6014;
      double var3 = var0.method_23318() - var0.field_6036;
      double var5 = var0.method_23321() - var0.field_5969;
      return var0.field_6012 > 1 && var1 * var1 + var3 * var3 + var5 * var5 < 1.0E-4;
   }

   private Ballistics.Body bodyOf(ProjectileEntity var1, boolean var2) {
      if (var1 instanceof TridentEntity var4) {
         return this.tridents.get() && !var4.method_7441() ? Ballistics.Body.TRIDENT : null;
      } else if (var1 instanceof PersistentProjectileEntity var3) {
         return this.bows.get() && !var3.method_7441() ? Ballistics.Body.ARROW : null;
      } else if (!(var1 instanceof EnderPearlEntity)) {
         if (!this.throwables.get()) {
            return null;
         } else if (var1 instanceof PotionEntity) {
            return Ballistics.Body.FLASK;
         } else if (var1 instanceof ExperienceBottleEntity) {
            return Ballistics.Body.BOTTLE;
         } else {
            return !(var1 instanceof SnowballEntity) && !(var1 instanceof EggEntity) ? null : Ballistics.Body.ORB;
         }
      } else {
         return this.pearls.get() && !var2 ? Ballistics.Body.ORB : null;
      }
   }

   private static PearlTrajectoryModule activePearlModule() {
      PearlTrajectoryModule var0 = ModuleRegistry.get(PearlTrajectoryModule.class);
      return var0 != null && var0.isEnabled() ? var0 : null;
   }

   private static boolean pearlToggle(PearlTrajectoryModule var0, String var1) {
      for (Setting var3 : var0.settings()) {
         if (var1.equals(var3.id()) && var3 instanceof BooleanSetting var4) {
            return var4.get();
         }
      }

      return true;
   }

   private void onRender(Render3DEvent var1) {
      this.labels.clear();
      MinecraftClient var5 = MinecraftClient.method_1551();
      ClientWorld var6 = var5.field_1687;
      ClientPlayerEntity var7 = var5.field_1724;
      this.syncLevel(var6);
      if (var6 != null && var7 != null) {
         long var3;
         this.labelsAt = var3 = System.nanoTime();
         float var8 = (float)(var3 - EPOCH) / 1.0E9F;
         float var9 = var1.partialTick;
         this.choosePalette();
         this.onTop = this.throughWalls.get();
         this.canvas.begin(var5.field_1773.method_19418());
         this.camera = var5.field_1773.method_19418().method_71156();
         LivingEntity var2 = this.targetBox.get() ? CombatTarget.current(6000L) : null;
         if (this.aimPreview.get()) {
            this.drawAim(var6, var7, var9, var3, var8, var2);
         } else if (this.aimItem != null || this.volleySize > 0) {
            this.forgetAim();
         }

         if (this.inFlight.get()) {
            this.drawTracks(var7, var9, var8, var2);
         }

         this.canvas.submit(this.onTop);
      }
   }

   private void drawTracks(ClientPlayerEntity var1, float var2, float var3, LivingEntity var4) {
      ObjectIterator var5 = this.tracks.values().iterator();

      while (var5.hasNext()) {
         TrajectoriesModule.Track var6 = (TrajectoriesModule.Track)var5.next();
         ProjectileEntity var7 = var6.shot;
         if (var7 != null && !var7.method_31481()) {
            boolean var8 = var7.method_24921() == var1;
            float var9 = var8 ? 1.0F : 0.7F;
            this.drawPath(var6.flight, var7.method_30950(var2), 1.7976931348623157E308, var9 * 0.85F, false, false, var8, var3);
            this.drawImpact(var6.flight, 1.0F, var9, var3, var2, var4);
            this.addLabel(var6);
         }
      }
   }

   private void choosePalette() {
      boolean var1 = this.colorMode.is("Manual");
      int var2 = var1 ? this.pathColor.argb() : CosmeticAccent.argb();
      this.headTint = ColorHelper.method_61319(0.35F, var2, -1);
      this.tailTint = var2;
      this.haloTint = var2;
      this.markTint = var2;
      this.ringTint = this.headTint;
      this.sheenTint = ColorHelper.method_61319(0.8F, var2, -1);
      this.plateTint = ColorHelper.method_61319(0.55F, var2, -1);
      this.hitTint = var1 ? this.hitColor.argb() : ColorHelper.method_61319(0.5F, var2, -1);
   }

   private void drawAim(ClientWorld var1, ClientPlayerEntity var2, float var3, long var4, float var6, LivingEntity var7) {
      ItemStack var8 = heldLauncher(var2);
      this.volleySize = 0;
      this.volleyGhost = false;
      if (!var8.method_7960()) {
         this.loadVolley(var1, var2, var8, var3);
      }

      if (this.volleySize == 0) {
         if (this.aimItem != null) {
            this.forgetAim();
         }
      } else {
         Item var9 = var8.method_7909();
         if (var9 != this.aimItem) {
            this.aimItem = var9;
            this.aimSince = var4;
            this.markerSince = -1L;
         }

         float var10 = easeOutExpo((float)(var4 - this.aimSince) / 1.0E9F / 0.32F);
         Ballistics.Flight var11 = this.volley[0].flight;
         if (var10 < 1.0F || var11.outcome() == Ballistics.Outcome.OPEN) {
            this.markerSince = -1L;
         } else if (this.markerSince < 0L) {
            this.markerSince = var4;
         }

         float var12 = this.markerSince < 0L ? 0.0F : settle((float)(var4 - this.markerSince) / 1.0E9F);
         float var13 = this.volleyGhost ? 0.38F : 1.0F;

         for (int var14 = 0; var14 < this.volleySize; var14++) {
            Ballistics.Flight var15 = this.volley[var14].flight;
            this.drawPath(var15, null, var15.length() * var10, var13, true, !this.volleyGhost, true, var6);
            if (var12 > 0.0F) {
               this.drawImpact(var15, var12, var13, var6, var3, var7);
            }
         }

         if (!this.volleyGhost && var10 >= 1.0F) {
            this.addLabel(this.volley[0]);
         }
      }
   }

   private static ItemStack heldLauncher(ClientPlayerEntity var0) {
      if (var0.method_6115()) {
         return var0.method_6030();
      } else {
         ItemStack var1 = var0.method_6047();
         return launches(var1.method_7909()) ? var1 : var0.method_6079();
      }
   }

   private static boolean launches(Item var0) {
      return var0 instanceof RangedWeaponItem
         || var0 instanceof TridentItem
         || var0 instanceof SnowballItem
         || var0 instanceof EggItem
         || var0 instanceof EnderPearlItem
         || var0 instanceof ThrowablePotionItem
         || var0 instanceof ExperienceBottleItem;
   }

   private void loadVolley(ClientWorld var1, ClientPlayerEntity var2, ItemStack var3, float var4) {
      Item var6 = var3.method_7909();
      boolean var5 = var2.method_6115() && var2.method_6030() == var3;
      if (var6 instanceof BowItem) {
         if (!this.bows.get() || var2.method_18808(var3).method_7960() || !var5 && !this.ghostArc.get()) {
            return;
         }

         float var8 = var5 ? bowPull(var2.method_6048() + var4) : 1.0F;
         this.volleyGhost = !var5 || var8 < 0.1F;
         this.aimed(var1, var2, var4, Ballistics.Body.ARROW, Math.max(var8, 0.1F) * 3.0F, 0.0F);
      } else if (var6 instanceof CrossbowItem) {
         if (this.bows.get()) {
            this.crossbowVolley(
               var1, var2, var4, (ChargedProjectilesComponent)var3.method_58695(DataComponentTypes.field_49649, ChargedProjectilesComponent.field_49298)
            );
         }
      } else if (var6 instanceof TridentItem) {
         if (!this.tridents.get() || !var5 && !this.ghostArc.get()) {
            return;
         }

         if (EnchantmentHelper.method_60142(var3, EnchantmentEffectComponentTypes.field_51651)) {
            return;
         }

         this.volleyGhost = !var5 || var2.method_6048() < 10;
         this.aimed(var1, var2, var4, Ballistics.Body.TRIDENT, 2.5F, 0.0F);
      } else if (var6 instanceof EnderPearlItem) {
         PearlTrajectoryModule var9 = activePearlModule();
         if (this.pearls.get() && (var9 == null || !pearlToggle(var9, "predict"))) {
            this.aimed(var1, var2, var4, Ballistics.Body.ORB, EnderPearlItem.field_55033, 0.0F);
         }
      } else if (this.throwables.get()) {
         this.lob(var1, var2, var4, var6);
      }
   }

   private void lob(ClientWorld var1, ClientPlayerEntity var2, float var3, Item var4) {
      if (var4 instanceof SnowballItem) {
         this.aimed(var1, var2, var3, Ballistics.Body.ORB, SnowballItem.field_55045, 0.0F);
      } else if (var4 instanceof EggItem) {
         this.aimed(var1, var2, var3, Ballistics.Body.ORB, 1.5F, 0.0F);
      } else if (var4 instanceof ThrowablePotionItem) {
         this.aimed(var1, var2, var3, Ballistics.Body.FLASK, ThrowablePotionItem.field_55046, -20.0F);
      } else if (var4 instanceof ExperienceBottleItem) {
         this.aimed(var1, var2, var3, Ballistics.Body.BOTTLE, 0.7F, -20.0F);
      }
   }

   private static float bowPull(float var0) {
      float var1 = var0 / 20.0F;
      return Math.min(1.0F, (var1 * var1 + var1 * 2.0F) / 3.0F);
   }

   private void aimed(ClientWorld var1, ClientPlayerEntity var2, float var3, Ballistics.Body var4, float var5, float var6) {
      float var7 = var2.method_5695(var3);
      double var8 = Math.toRadians(var7);
      double var10 = Math.toRadians(var2.method_5705(var3));
      double var12 = Math.toRadians(var7 + var6);
      Vec3d var14 = new Vec3d(-Math.sin(var10) * Math.cos(var8), -Math.sin(var12), Math.cos(var10) * Math.cos(var8)).method_1029().method_1021(var5);
      Vec3d var15 = var2.method_60478();
      Vec3d var16 = var14.method_1031(var15.field_1352, var2.method_24828() ? 0.0 : var15.field_1351, var15.field_1350);
      this.release(var1, var2, var4, muzzle(var2, var3, 0.1), var16, 240);
   }

   private void crossbowVolley(ClientWorld var1, ClientPlayerEntity var2, float var3, ChargedProjectilesComponent var4) {
      if (!var4.method_57442()) {
         List var5 = var4.method_57437();
         float var6 = var4.method_57438(Items.field_8639) ? 1.6F : 3.15F;
         int var7 = var5.size();
         float var8 = var7 > 1 ? 20.0F / (var7 - 1) : 0.0F;
         float var9 = (var7 - 1) % 2 * var8 / 2.0F;
         Vec3d var10 = var2.method_5828(var3);
         Vec3d var11 = var2.method_18864(var3);

         for (int var12 = 0; var12 < var7; var12++) {
            ItemStack var13 = (ItemStack)var5.get(var12);
            if (!var13.method_7960()) {
               float var14 = var9 + ((var12 & 1) == 1 ? -1.0F : 1.0F) * ((var12 + 1) / 2) * var8;
               Vec3d var15 = swing(var10, var11, var14).method_1029().method_1021(var6);
               if (var13.method_31574(Items.field_8639)) {
                  this.release(var1, var2, Ballistics.Body.ROCKET, muzzle(var2, var3, 0.15), var15, rocketFuse(var13));
               } else {
                  this.release(var1, var2, Ballistics.Body.ARROW, muzzle(var2, var3, 0.1), var15, 240);
               }
            }
         }
      }
   }

   private static int rocketFuse(ItemStack var0) {
      FireworksComponent var1 = (FireworksComponent)var0.method_58694(DataComponentTypes.field_49616);
      int var2 = 1 + (var1 != null ? var1.comp_2391() : 0);
      return 10 * var2 + 6;
   }

   private static Vec3d swing(Vec3d var0, Vec3d var1, double var2) {
      if (var2 == 0.0) {
         return var0;
      } else {
         double var4 = Math.toRadians(var2);
         double var6 = Math.cos(var4);
         double var8 = Math.sin(var4);
         Vec3d var10 = var1.method_1029();
         return var0.method_1021(var6)
            .method_1019(var10.method_1036(var0).method_1021(var8))
            .method_1019(var10.method_1021(var10.method_1026(var0) * (1.0 - var6)));
      }
   }

   private static Vec3d muzzle(ClientPlayerEntity var0, float var1, double var2) {
      Vec3d var4 = var0.method_30950(var1);
      return new Vec3d(var4.field_1352, var4.field_1351 + var0.method_5751() - var2, var4.field_1350);
   }

   private void release(ClientWorld var1, ClientPlayerEntity var2, Ballistics.Body var3, Vec3d var4, Vec3d var5, int var6) {
      if (this.volleySize != 3) {
         TrajectoriesModule.Aim var7;
         if (!(var7 = this.volley[this.volleySize++]).matches(var3, var4, var5, var6, this.sweep)) {
            var7.hold(var3, var4, var5, var6, this.sweep);
            var7.flight.launch(var3, var4, var5).lifetime(var6).ignoring(var2, null).solve(var1);
            var7.caption(var4, this.flightTime.get());
         }
      }
   }

   private void drawPath(Ballistics.Flight var1, Vec3d var2, double var3, float var5, boolean var6, boolean var7, boolean var8, float var9) {
      int var18 = var1.size();
      if (var18 >= 2 && !(var5 < 0.02F)) {
         double var19 = var1.length();
         if (var2 != null) {
            double var21 = var2.field_1352 - var1.x(0);
            double var23 = var2.field_1351 - var1.y(0);
            double var25 = var2.field_1350 - var1.z(0);
            var19 += Math.sqrt(var21 * var21 + var23 * var23 + var25 * var25);
         }

         if (!(var19 < 1.0E-4)) {
            double var64 = Math.min(var3, var19);
            boolean var65 = this.pathStyle.is("Solid");
            boolean var24 = this.pathStyle.is("Dashes");
            double var66 = var65 ? 0.6 : (var24 ? 0.72 : 0.42);
            double var27 = Math.max(1.0, var19 / (var66 * (var6 ? 200 : 110)));
            double var29 = (var24 ? 0.72 : 0.42) * var27;
            double var31 = 0.44 * var27;
            double var33 = 0.6 * var27;
            double var35 = !var65 && this.flow.get() ? var9 * 1.3 % var29 : 0.0;
            this.strokeTotal = var19;
            this.strokeAlpha = var5;
            this.strokeFade = var6;
            this.strokeSheen = var7 ? var9 * 9.0 % (var19 + 14.0) - 1.6 : 0.0 / 0.0;
            this.strokeWidth = this.thickness.get();
            boolean var37 = this.strokeHalo = var8 && this.halo.get();
            double var12;
            double var14;
            double var16;
            byte var38;
            if (var2 != null) {
               var16 = var2.field_1352;
               var14 = var2.field_1351;
               var12 = var2.field_1350;
               var38 = 0;
            } else {
               var16 = var1.x(0);
               var14 = var1.y(0);
               var12 = var1.z(0);
               var38 = 1;
            }

            double var39 = 0.0;

            for (int var41 = var38; var41 < var18 && var39 < var64; var41++) {
               double var50 = var1.x(var41);
               double var52 = var50 - var16;
               double var42;
               double var44;
               double var46;
               double var48;
               double var10 = Math.sqrt(var52 * var52 + (var48 = (var46 = var1.y(var41)) - var14) * var48 + (var44 = (var42 = var1.z(var41)) - var12) * var44);
               if (var10 > 1.0E-6) {
                  double var54 = Math.min(var39 + var10, var64);
                  if (var65) {
                     int var56 = Math.max(1, (int)Math.ceil((var54 - var39) / var33));
                     double var57 = (var54 - var39) / var56;

                     for (int var59 = 0; var59 < var56; var59++) {
                        double var60 = var39 + var59 * var57;
                        this.stroke(var16, var14, var12, var52, var48, var44, (var60 - var39) / var10, (var60 + var57 - var39) / var10, var60 + var57 * 0.5);
                     }
                  } else {
                     for (double var67 = Math.floor((var39 - var35) / var29) * var29 + var35; var67 < var54; var67 += var29) {
                        double var58 = Math.max(var67, var39);
                        double var68 = var24 ? var31 : this.beadLength(var16, var14, var12, var52, var48, var44, (var58 - var39) / var10);
                        double var62 = Math.min(var67 + var68, var54);
                        if (var62 > var58) {
                           this.stroke(var16, var14, var12, var52, var48, var44, (var58 - var39) / var10, (var62 - var39) / var10, (var58 + var62) * 0.5);
                        }
                     }
                  }
               }

               var16 = var50;
               var14 = var46;
               var12 = var42;
               var39 += var10;
            }
         }
      }
   }

   private double beadLength(double var1, double var3, double var5, double var7, double var9, double var11, double var13) {
      double var15 = var1 + var7 * var13 - this.camera.field_1352;
      double var17 = var3 + var9 * var13 - this.camera.field_1351;
      double var19 = var5 + var11 * var13 - this.camera.field_1350;
      return MathHelper.method_15350(Math.sqrt(var15 * var15 + var17 * var17 + var19 * var19) * 0.011, 0.035, 0.2);
   }

   private void stroke(double var1, double var3, double var5, double var7, double var9, double var11, double var13, double var15, double var17) {
      float var22 = this.strokeAlpha;
      if (this.strokeFade) {
         var22 *= smooth((float)(var17 / 1.4));
      }

      if (!(var22 < 0.02F)) {
         float var23 = (float)(var17 / this.strokeTotal);
         int var24 = ColorHelper.method_61319(var23, this.headTint, this.tailTint);
         double var20;
         if (!Double.isNaN(this.strokeSheen) && (var20 = 1.0 - Math.abs(var17 - this.strokeSheen) / 1.6) > 0.0) {
            var24 = ColorHelper.method_61319((float)(var20 * 0.7), var24, this.sheenTint);
         }

         double var25 = var1 + var7 * var13;
         double var27 = var3 + var9 * var13;
         double var29 = var5 + var11 * var13;
         double var31 = var1 + var7 * var15;
         double var33 = var3 + var9 * var15;
         double var35 = var5 + var11 * var15;
         float var19;
         if (this.strokeHalo && (var19 = var22 * 0.22F * this.haloFade(var25, var27, var29)) > 0.0F) {
            this.canvas.line(var25, var27, var29, var31, var33, var35, ColorHelper.method_75601(this.haloTint, var19), this.strokeWidth * 2.8F);
         }

         this.canvas.line(var25, var27, var29, var31, var33, var35, ColorHelper.method_75601(var24, var22), this.strokeWidth);
      }
   }

   private float haloFade(double var1, double var3, double var5) {
      double var7 = var1 - this.camera.field_1352;
      double var9 = var3 - this.camera.field_1351;
      double var11 = var5 - this.camera.field_1350;
      double var13 = var7 * var7 + var9 * var9 + var11 * var11;
      if (var13 <= 576.0) {
         return 1.0F;
      } else {
         return var13 >= 1600.0 ? 0.0F : 1.0F - smooth((float)((Math.sqrt(var13) - 24.0) / 16.0));
      }
   }

   private void drawImpact(Ballistics.Flight var1, float var2, float var3, float var4, float var5, LivingEntity var6) {
      switch (var1.outcome()) {
         case ENTITY:
            if (this.targetBox.get()) {
               this.drawTargetBox(var1.struck(), var3, var4, var5, var6);
            }
            break;
         case BLOCK:
            this.drawMarker(var1, var2, var3, var4);
            break;
         case BURST:
            this.drawBurst(var1.impact(), var2, var3);
      }
   }

   private void drawMarker(Ballistics.Flight var1, float var2, float var3, float var4) {
      if (!this.marker.is("None") && !(var2 <= 0.01F)) {
         Direction var5 = var1.face();
         Vec3d var6 = new Vec3d(var5.method_10148(), var5.method_10164(), var5.method_10165());
         Vec3d var7 = new Vec3d(0.0, 1.0, 0.0);
         if (var5.method_10166() == Axis.field_11052) {
            Vec3d var8 = var1.heading();
            var7 = var8.field_1352 * var8.field_1352 + var8.field_1350 * var8.field_1350 < 1.0E-8
               ? new Vec3d(0.0, 0.0, 1.0)
               : new Vec3d(var8.field_1352, 0.0, var8.field_1350).method_1029();
         }

         Vec3d var12 = var7.method_1036(var6).method_1029();
         Vec3d var9 = var1.impact().method_1019(var6.method_1021(0.025));
         double var10 = 0.5 * this.markerScale.get() * var2;
         this.gx = var9.field_1352;
         this.gy = var9.field_1351;
         this.gz = var9.field_1350;
         this.rx = var12.field_1352 * var10;
         this.ry = var12.field_1351 * var10;
         this.rz = var12.field_1350 * var10;
         this.ux = var7.field_1352 * var10;
         this.uy = var7.field_1351 * var10;
         this.uz = var7.field_1350 * var10;
         if (this.marker.is("Ring")) {
            this.drawSunDisk(var3, var4);
         } else {
            this.drawBolt(var3, var4);
         }
      }
   }

   private void drawBolt(float var1, float var2) {
      int var3 = ColorHelper.method_75601(this.markTint, var1);
      this.disc(0.78, ColorHelper.method_75601(this.plateTint, var1 * 0.16F));
      this.orbit(0.9, var2 * 0.6, ColorHelper.method_75601(this.ringTint, var1 * 0.85F), this.thickness.get() * 0.75F);
      this.facet(this.at(-0.06, 0.02), this.at(0.06, 0.02), this.at(0.15, -0.62), this.at(-0.15, -0.62), var3);
      this.facet(this.at(-0.06, 0.09), this.at(-0.06, -0.03), this.at(-0.37, -0.08), this.at(-0.37, 0.14), var3);
      this.facet(this.at(0.06, 0.09), this.at(0.06, -0.03), this.at(0.37, -0.08), this.at(0.37, 0.14), var3);

      for (int var4 = 0; var4 < 24; var4++) {
         double var5 = RIM_COS[var4];
         double var7 = RIM_SIN[var4];
         double var9 = RIM_COS[var4 + 1];
         double var11 = RIM_SIN[var4 + 1];
         this.facet(
            this.at(0.19 * var5, 0.34 + 0.27 * var7),
            this.at(0.19 * var9, 0.34 + 0.27 * var11),
            this.at(0.1 * var9, 0.34 + 0.17 * var11),
            this.at(0.1 * var5, 0.34 + 0.17 * var7),
            var3
         );
      }
   }

   private void drawSunDisk(float var1, float var2) {
      int var3 = ColorHelper.method_75601(this.markTint, var1);
      float var4 = this.thickness.get();
      this.disc(0.95, ColorHelper.method_75601(this.plateTint, var1 * 0.12F));
      this.disc(0.3, var3);
      int var5 = ColorHelper.method_75601(this.ringTint, var1);
      Vec3d var6 = this.at(0.42, 0.0);

      for (int var7 = 1; var7 <= 24; var7++) {
         Vec3d var8 = this.at(0.42 * RIM_COS[var7], 0.42 * RIM_SIN[var7]);
         this.line(var6, var8, var5, var4);
         var6 = var8;
      }

      double var18 = var2 * 0.35;

      for (int var9 = 0; var9 < 12; var9++) {
         double var10 = var18 + 6.283185307179586 * var9 / 12.0;
         double var12 = Math.cos(var10);
         double var14 = Math.sin(var10);
         double var16 = (var9 & 1) == 0 ? 0.8 : 0.66;
         this.line(this.at(0.52 * var12, 0.52 * var14), this.at(var16 * var12, var16 * var14), var3, var4);
      }
   }

   private void drawBurst(Vec3d var1, float var2, float var3) {
      int var4 = ColorHelper.method_75601(this.markTint, var3);
      double var5 = 0.55 * this.markerScale.get() * var2;
      float var7 = this.thickness.get();

      for (double[] var11 : BURST_RAYS) {
         Vec3d var12 = new Vec3d(var11[0], var11[1], var11[2]);
         this.line(var1.method_1019(var12.method_1021(var5 * 0.25)), var1.method_1019(var12.method_1021(var5)), var4, var7);
      }
   }

   private void drawTargetBox(Entity var1, float var2, float var3, float var4, LivingEntity var5) {
      if (var1 != null && !var1.method_31481()) {
         Vec3d var6 = var1.method_30950(var4).method_1020(var1.method_73189());
         Box var7 = var1.method_5829().method_997(var6).method_1014(0.06);
         if (!var7.method_1006(this.camera)) {
            boolean var8 = var1 == var5;
            float var9 = var8 ? 0.5F + 0.5F * MathHelper.method_15374(var3 * 6.5F) : 0.0F;
            int var10 = ColorHelper.method_75601(var8 ? ColorHelper.method_61319(0.35F * var9, this.hitTint, -1) : this.hitTint, var2);
            VisibilityConfigurable var11 = GizmoDrawing.method_75541(
               var7, DrawStyle.method_75539(ColorHelper.method_75601(var10, var8 ? 0.14F + 0.12F * var9 : 0.1F))
            );
            if (this.onTop) {
               var11.method_75533();
            }

            this.brackets(var7, var8 ? 0.34 + 0.06 * var9 : 0.26, var10, this.thickness.get() * (var8 ? 1.2F : 1.0F));
         }
      }
   }

   private void brackets(Box var1, double var2, int var4, float var5) {
      double var6 = (var1.field_1320 - var1.field_1323) * var2;
      double var8 = (var1.field_1325 - var1.field_1322) * var2;
      double var10 = (var1.field_1324 - var1.field_1321) * var2;

      for (int var12 = 0; var12 < 8; var12++) {
         boolean var13 = (var12 & 1) != 0;
         boolean var14 = (var12 & 2) != 0;
         boolean var15 = (var12 & 4) != 0;
         double var16 = var13 ? var1.field_1320 : var1.field_1323;
         double var18 = var14 ? var1.field_1325 : var1.field_1322;
         double var20 = var15 ? var1.field_1324 : var1.field_1321;
         Vec3d var22 = new Vec3d(var16, var18, var20);
         this.line(var22, new Vec3d(var13 ? var16 - var6 : var16 + var6, var18, var20), var4, var5);
         this.line(var22, new Vec3d(var16, var14 ? var18 - var8 : var18 + var8, var20), var4, var5);
         this.line(var22, new Vec3d(var16, var18, var15 ? var20 - var10 : var20 + var10), var4, var5);
      }
   }

   private void disc(double var1, int var3) {
      Vec3d var4 = this.at(0.0, 0.0);

      for (int var5 = 0; var5 < 24; var5++) {
         this.facet(var4, var4, this.at(var1 * RIM_COS[var5], var1 * RIM_SIN[var5]), this.at(var1 * RIM_COS[var5 + 1], var1 * RIM_SIN[var5 + 1]), var3);
      }
   }

   private void orbit(double var1, double var3, int var5, float var6) {
      double var7 = 0.6283185307179586;
      double var9 = var7 * 0.55;

      for (int var11 = 0; var11 < 10; var11++) {
         double var12 = var3 + var11 * var7;
         Vec3d var14 = this.at(var1 * Math.cos(var12), var1 * Math.sin(var12));

         for (int var15 = 1; var15 <= 3; var15++) {
            double var16 = var12 + var9 * var15 / 3.0;
            Vec3d var18 = this.at(var1 * Math.cos(var16), var1 * Math.sin(var16));
            this.line(var14, var18, var5, var6);
            var14 = var18;
         }
      }
   }

   private Vec3d at(double var1, double var3) {
      return new Vec3d(this.gx + this.rx * var1 + this.ux * var3, this.gy + this.ry * var1 + this.uy * var3, this.gz + this.rz * var1 + this.uz * var3);
   }

   private void facet(Vec3d var1, Vec3d var2, Vec3d var3, Vec3d var4, int var5) {
      double var6 = var3.field_1351 - var1.field_1351;
      double var8 = var4.field_1350 - var2.field_1350;
      double var10 = var3.field_1350 - var1.field_1350;
      double var12 = var4.field_1351 - var2.field_1351;
      double var14 = var6 * var8 - var10 * var12;
      double var16 = var4.field_1352 - var2.field_1352;
      double var18 = var3.field_1352 - var1.field_1352;
      double var20 = var10 * var16 - var18 * var8;
      double var22 = var18 * var12 - var6 * var16;
      double var24 = var14 * (this.camera.field_1352 - var1.field_1352)
         + var20 * (this.camera.field_1351 - var1.field_1351)
         + var22 * (this.camera.field_1350 - var1.field_1350);
      if (var24 >= 0.0) {
         this.canvas
            .quad(
               var1.field_1352,
               var1.field_1351,
               var1.field_1350,
               var2.field_1352,
               var2.field_1351,
               var2.field_1350,
               var3.field_1352,
               var3.field_1351,
               var3.field_1350,
               var4.field_1352,
               var4.field_1351,
               var4.field_1350,
               var5
            );
      } else {
         this.canvas
            .quad(
               var4.field_1352,
               var4.field_1351,
               var4.field_1350,
               var3.field_1352,
               var3.field_1351,
               var3.field_1350,
               var2.field_1352,
               var2.field_1351,
               var2.field_1350,
               var1.field_1352,
               var1.field_1351,
               var1.field_1350,
               var5
            );
      }
   }

   private void line(Vec3d var1, Vec3d var2, int var3, float var4) {
      this.canvas.line(var1.field_1352, var1.field_1351, var1.field_1350, var2.field_1352, var2.field_1351, var2.field_1350, var3, var4);
   }

   private void addLabel(TrajectoriesModule.Plot var1) {
      if (this.flightTime.get() && var1.seconds != null) {
         Ballistics.Flight var2 = var1.flight;
         Vec3d var3 = var2.impact();
         Entity var4 = var2.struck();
         Vec3d var5 = var4 != null ? new Vec3d(var3.field_1352, var4.method_5829().field_1325 + 0.35, var3.field_1350) : var3.method_1031(0.0, 0.55, 0.0);
         this.labels.add(new TrajectoriesModule.Label(var5, var1.seconds, var1.detail));
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.flightTime.get();
   }

   @Override
   public void renderHudOverlay(ImDrawList var1, float var2, float var3) {
      if (!this.labels.isEmpty() && System.nanoTime() - this.labelsAt <= 250000000L) {
         MinecraftClient var4 = MinecraftClient.method_1551();

         for (TrajectoriesModule.Label var6 : this.labels) {
            float[] var7 = WorldLabel.project(var4, var6.pos(), var2, var3);
            if (var7 != null) {
               WorldLabel.drawText(var1, var7[0], var7[1], var6.primary(), var6.secondary(), 1.0F);
            }
         }
      }
   }

   private static float smooth(float var0) {
      var0 = MathHelper.method_15363(var0, 0.0F, 1.0F);
      return var0 * var0 * (3.0F - 2.0F * var0);
   }

   private static float easeOutExpo(float var0) {
      return var0 >= 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0 * Math.max(0.0F, var0));
   }

   private static float settle(float var0) {
      return 1.0F - (float)(Math.exp(-5.5 * var0) * Math.cos(8.5 * var0));
   }

   static {
      for (int var0 = 0; var0 <= 24; var0++) {
         double var1 = 6.283185307179586 * var0 / 24.0;
         RIM_COS[var0] = Math.cos(var1);
         RIM_SIN[var0] = Math.sin(var1);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Aim extends TrajectoriesModule.Plot {
      Ballistics.Body body;
      double x;
      double y;
      double z;
      double vx;
      double vy;
      double vz;
      int life;
      int stamp;
      boolean held;

      private Aim() {
      }

      boolean matches(Ballistics.Body var1, Vec3d var2, Vec3d var3, int var4, int var5) {
         return this.held
            && this.stamp == var5
            && this.body == var1
            && this.life == var4
            && Math.abs(this.x - var2.field_1352) < 0.001
            && Math.abs(this.y - var2.field_1351) < 0.001
            && Math.abs(this.z - var2.field_1350) < 0.001
            && Math.abs(this.vx - var3.field_1352) < 5.0E-4
            && Math.abs(this.vy - var3.field_1351) < 5.0E-4
            && Math.abs(this.vz - var3.field_1350) < 5.0E-4;
      }

      void hold(Ballistics.Body var1, Vec3d var2, Vec3d var3, int var4, int var5) {
         this.body = var1;
         this.x = var2.field_1352;
         this.y = var2.field_1351;
         this.z = var2.field_1350;
         this.vx = var3.field_1352;
         this.vy = var3.field_1351;
         this.vz = var3.field_1350;
         this.life = var4;
         this.stamp = var5;
         this.held = true;
      }

      @Override
      void forget() {
         super.forget();
         this.held = false;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Label(Vec3d pos, String primary, String secondary) {
   }

   @Environment(EnvType.CLIENT)
   private static class Plot {
      final Ballistics.Flight flight = new Ballistics.Flight();
      String seconds;
      String detail;

      private Plot() {
      }

      void caption(Vec3d var1, boolean var2) {
         this.seconds = null;
         this.detail = null;
         if (var2 && this.flight.outcome() != Ballistics.Outcome.OPEN && this.flight.size() >= 2) {
            Entity var3 = this.flight.struck();
            this.seconds = String.format(Locale.ROOT, "%.2fs", this.flight.ticks() / 20.0F);
            this.detail = var3 != null
               ? NameProtectModule.protect(var3.method_5477()).getString()
               : String.format(Locale.ROOT, "%.0fm", var1.method_1022(this.flight.impact()));
         }
      }

      void forget() {
         this.flight.clear();
         this.seconds = null;
         this.detail = null;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Track extends TrajectoriesModule.Plot {
      ProjectileEntity shot;
      int seen;
      boolean departed;

      private Track() {
      }
   }
}
