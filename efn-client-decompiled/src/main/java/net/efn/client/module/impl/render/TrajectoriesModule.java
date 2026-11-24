package net.efn.client.module.impl.render;

import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
   private final Int2ObjectOpenHashMap<TrajectoriesModule.Track> tracks = new Int2ObjectOpenHashMap<>();
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
   private Vec3d camera = Vec3d.ZERO;
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

   private void syncLevel(ClientWorld clientWorld) {
      if (clientWorld != this.level) {
         this.reset();
         this.level = clientWorld;
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
         for (TrajectoriesModule.Track var2 : this.tracks.values()) {
            this.recycle(var2);
         }

         this.tracks.clear();
      }
   }

   private void recycle(TrajectoriesModule.Track track) {
      track.shot = null;
      track.departed = false;
      track.forget();
      if (this.spareTracks.size() < 24) {
         this.spareTracks.push(track);
      }
   }

   private void onTick(TickEvent tickEvent) {
      this.sweep++;
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientWorld var3 = var2.world;
      ClientPlayerEntity var4 = var2.player;
      this.syncLevel(var3);
      if (var3 != null && var4 != null && this.inFlight.get()) {
         double var5 = this.range.get() * this.range.get();
         PearlTrajectoryModule var7 = activePearlModule();
         boolean var8 = var7 != null && pearlToggle(var7, "others-pearls");
         boolean var9 = this.othersShots.get();
         boolean var10 = this.flightTime.get();
         Vec3d var11 = var4.getEyePos();
         int var12 = 24;

         for (Entity var14 : var3.getEntities()) {
            ProjectileEntity var19;
            if (var14 instanceof ProjectileEntity && !(var19 = (ProjectileEntity)var14).isRemoved()) {
               Entity var20 = var19.getOwner();
               boolean var18 = var20 == var4;
               Vec3d var15;
               Ballistics.Body var17;
               if ((var18 || var9)
                  && (var17 = this.bodyOf(var19, var7 != null && (var20 == null || var18 || var8))) != null
                  && !(var19.squaredDistanceTo(var4) > var5)
                  && !((var15 = var19.getVelocity()).lengthSquared() < 1.0E-4)
                  && !resting(var19)) {
                  if (var12-- == 0) {
                     break;
                  }

                  TrajectoriesModule.Track var22 = this.tracks.get(var19.getId());
                  if (var22 == null) {
                     var22 = this.spareTracks.isEmpty() ? new TrajectoriesModule.Track() : this.spareTracks.pop();
                     this.tracks.put(var19.getId(), var22);
                  }

                  var22.shot = var19;
                  var22.seen = this.sweep;
                  var22.flight
                     .launch(var17, var19.getEntityPos(), var15)
                     .bounds(var19.getWidth(), var19.getHeight())
                     .gravity(var19.hasNoGravity() ? 0.0 : var17.gravity())
                     .wet(var19.isTouchingWater())
                     .age(var19.age)
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

   private boolean stale(TrajectoriesModule.Track track) {
      if (track.seen == this.sweep) {
         return false;
      } else {
         this.recycle(track);
         return true;
      }
   }

   private static boolean resting(ProjectileEntity projectileEntity) {
      double var1 = projectileEntity.getX() - projectileEntity.lastX;
      double var3 = projectileEntity.getY() - projectileEntity.lastY;
      double var5 = projectileEntity.getZ() - projectileEntity.lastZ;
      return projectileEntity.age > 1 && var1 * var1 + var3 * var3 + var5 * var5 < 1.0E-4;
   }

   private Ballistics.Body bodyOf(ProjectileEntity projectileEntity, boolean bl) {
      if (projectileEntity instanceof TridentEntity var4) {
         return this.tridents.get() && !var4.isNoClip() ? Ballistics.Body.TRIDENT : null;
      } else if (projectileEntity instanceof PersistentProjectileEntity var3) {
         return this.bows.get() && !var3.isNoClip() ? Ballistics.Body.ARROW : null;
      } else if (!(projectileEntity instanceof EnderPearlEntity)) {
         if (!this.throwables.get()) {
            return null;
         } else if (projectileEntity instanceof PotionEntity) {
            return Ballistics.Body.FLASK;
         } else if (projectileEntity instanceof ExperienceBottleEntity) {
            return Ballistics.Body.BOTTLE;
         } else {
            return !(projectileEntity instanceof SnowballEntity) && !(projectileEntity instanceof EggEntity) ? null : Ballistics.Body.ORB;
         }
      } else {
         return this.pearls.get() && !bl ? Ballistics.Body.ORB : null;
      }
   }

   private static PearlTrajectoryModule activePearlModule() {
      PearlTrajectoryModule var0 = ModuleRegistry.get(PearlTrajectoryModule.class);
      return var0 != null && var0.isEnabled() ? var0 : null;
   }

   private static boolean pearlToggle(PearlTrajectoryModule pearlTrajectoryModule, String string) {
      for (Setting var3 : pearlTrajectoryModule.settings()) {
         if (string.equals(var3.id()) && var3 instanceof BooleanSetting var4) {
            return var4.get();
         }
      }

      return true;
   }

   private void onRender(Render3DEvent render3DEvent) {
      this.labels.clear();
      MinecraftClient var5 = MinecraftClient.getInstance();
      ClientWorld var6 = var5.world;
      ClientPlayerEntity var7 = var5.player;
      this.syncLevel(var6);
      if (var6 != null && var7 != null) {
         long var3;
         this.labelsAt = var3 = System.nanoTime();
         float var8 = (float)(var3 - EPOCH) / 1.0E9F;
         float var9 = render3DEvent.partialTick;
         this.choosePalette();
         this.onTop = this.throughWalls.get();
         this.canvas.begin(var5.gameRenderer.getCamera());
         this.camera = var5.gameRenderer.getCamera().getCameraPos();
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

   private void drawTracks(ClientPlayerEntity clientPlayerEntity, float f, float g, LivingEntity livingEntity) {
      for (TrajectoriesModule.Track var6 : this.tracks.values()) {
         ProjectileEntity var7 = var6.shot;
         if (var7 != null && !var7.isRemoved()) {
            boolean var8 = var7.getOwner() == clientPlayerEntity;
            float var9 = var8 ? 1.0F : 0.7F;
            this.drawPath(var6.flight, var7.getLerpedPos(f), 1.7976931348623157E308, var9 * 0.85F, false, false, var8, g);
            this.drawImpact(var6.flight, 1.0F, var9, g, f, livingEntity);
            this.addLabel(var6);
         }
      }
   }

   private void choosePalette() {
      boolean var1 = this.colorMode.is("Manual");
      int var2 = var1 ? this.pathColor.argb() : CosmeticAccent.argb();
      this.headTint = ColorHelper.lerp(0.35F, var2, -1);
      this.tailTint = var2;
      this.haloTint = var2;
      this.markTint = var2;
      this.ringTint = this.headTint;
      this.sheenTint = ColorHelper.lerp(0.8F, var2, -1);
      this.plateTint = ColorHelper.lerp(0.55F, var2, -1);
      this.hitTint = var1 ? this.hitColor.argb() : ColorHelper.lerp(0.5F, var2, -1);
   }

   private void drawAim(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, float f, long l, float g, LivingEntity livingEntity) {
      ItemStack var8 = heldLauncher(clientPlayerEntity);
      this.volleySize = 0;
      this.volleyGhost = false;
      if (!var8.isEmpty()) {
         this.loadVolley(clientWorld, clientPlayerEntity, var8, f);
      }

      if (this.volleySize == 0) {
         if (this.aimItem != null) {
            this.forgetAim();
         }
      } else {
         Item var9 = var8.getItem();
         if (var9 != this.aimItem) {
            this.aimItem = var9;
            this.aimSince = l;
            this.markerSince = -1L;
         }

         float var10 = easeOutExpo((float)(l - this.aimSince) / 1.0E9F / 0.32F);
         Ballistics.Flight var11 = this.volley[0].flight;
         if (var10 < 1.0F || var11.outcome() == Ballistics.Outcome.OPEN) {
            this.markerSince = -1L;
         } else if (this.markerSince < 0L) {
            this.markerSince = l;
         }

         float var12 = this.markerSince < 0L ? 0.0F : settle((float)(l - this.markerSince) / 1.0E9F);
         float var13 = this.volleyGhost ? 0.38F : 1.0F;

         for (int var14 = 0; var14 < this.volleySize; var14++) {
            Ballistics.Flight var15 = this.volley[var14].flight;
            this.drawPath(var15, null, var15.length() * var10, var13, true, !this.volleyGhost, true, g);
            if (var12 > 0.0F) {
               this.drawImpact(var15, var12, var13, g, f, livingEntity);
            }
         }

         if (!this.volleyGhost && var10 >= 1.0F) {
            this.addLabel(this.volley[0]);
         }
      }
   }

   private static ItemStack heldLauncher(ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.isUsingItem()) {
         return clientPlayerEntity.getActiveItem();
      } else {
         ItemStack var1 = clientPlayerEntity.getMainHandStack();
         return launches(var1.getItem()) ? var1 : clientPlayerEntity.getOffHandStack();
      }
   }

   private static boolean launches(Item item) {
      return item instanceof RangedWeaponItem
         || item instanceof TridentItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || item instanceof EnderPearlItem
         || item instanceof ThrowablePotionItem
         || item instanceof ExperienceBottleItem;
   }

   private void loadVolley(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, ItemStack itemStack, float f) {
      Item var6 = itemStack.getItem();
      boolean var5 = clientPlayerEntity.isUsingItem() && clientPlayerEntity.getActiveItem() == itemStack;
      if (var6 instanceof BowItem) {
         if (!this.bows.get() || clientPlayerEntity.getProjectileType(itemStack).isEmpty() || !var5 && !this.ghostArc.get()) {
            return;
         }

         float var8 = var5 ? bowPull(clientPlayerEntity.getItemUseTime() + f) : 1.0F;
         this.volleyGhost = !var5 || var8 < 0.1F;
         this.aimed(clientWorld, clientPlayerEntity, f, Ballistics.Body.ARROW, Math.max(var8, 0.1F) * 3.0F, 0.0F);
      } else if (var6 instanceof CrossbowItem) {
         if (this.bows.get()) {
            this.crossbowVolley(
               clientWorld, clientPlayerEntity, f, itemStack.getOrDefault(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT)
            );
         }
      } else if (var6 instanceof TridentItem) {
         if (!this.tridents.get() || !var5 && !this.ghostArc.get()) {
            return;
         }

         if (EnchantmentHelper.hasAnyEnchantmentsWith(itemStack, EnchantmentEffectComponentTypes.TRIDENT_SPIN_ATTACK_STRENGTH)) {
            return;
         }

         this.volleyGhost = !var5 || clientPlayerEntity.getItemUseTime() < 10;
         this.aimed(clientWorld, clientPlayerEntity, f, Ballistics.Body.TRIDENT, 2.5F, 0.0F);
      } else if (var6 instanceof EnderPearlItem) {
         PearlTrajectoryModule var9 = activePearlModule();
         if (this.pearls.get() && (var9 == null || !pearlToggle(var9, "predict"))) {
            this.aimed(clientWorld, clientPlayerEntity, f, Ballistics.Body.ORB, EnderPearlItem.POWER, 0.0F);
         }
      } else if (this.throwables.get()) {
         this.lob(clientWorld, clientPlayerEntity, f, var6);
      }
   }

   private void lob(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, float f, Item item) {
      if (item instanceof SnowballItem) {
         this.aimed(clientWorld, clientPlayerEntity, f, Ballistics.Body.ORB, SnowballItem.POWER, 0.0F);
      } else if (item instanceof EggItem) {
         this.aimed(clientWorld, clientPlayerEntity, f, Ballistics.Body.ORB, 1.5F, 0.0F);
      } else if (item instanceof ThrowablePotionItem) {
         this.aimed(clientWorld, clientPlayerEntity, f, Ballistics.Body.FLASK, ThrowablePotionItem.POWER, -20.0F);
      } else if (item instanceof ExperienceBottleItem) {
         this.aimed(clientWorld, clientPlayerEntity, f, Ballistics.Body.BOTTLE, 0.7F, -20.0F);
      }
   }

   private static float bowPull(float f) {
      float var1 = f / 20.0F;
      return Math.min(1.0F, (var1 * var1 + var1 * 2.0F) / 3.0F);
   }

   private void aimed(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, float f, Ballistics.Body body, float g, float h) {
      float var7 = clientPlayerEntity.getPitch(f);
      double var8 = Math.toRadians(var7);
      double var10 = Math.toRadians(clientPlayerEntity.getYaw(f));
      double var12 = Math.toRadians(var7 + h);
      Vec3d var14 = new Vec3d(-Math.sin(var10) * Math.cos(var8), -Math.sin(var12), Math.cos(var10) * Math.cos(var8)).normalize().multiply(g);
      Vec3d var15 = clientPlayerEntity.getMovement();
      Vec3d var16 = var14.add(var15.x, clientPlayerEntity.isOnGround() ? 0.0 : var15.y, var15.z);
      this.release(clientWorld, clientPlayerEntity, body, muzzle(clientPlayerEntity, f, 0.1), var16, 240);
   }

   private void crossbowVolley(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, float f, ChargedProjectilesComponent chargedProjectilesComponent) {
      if (!chargedProjectilesComponent.isEmpty()) {
         List var5 = chargedProjectilesComponent.getProjectiles();
         float var6 = chargedProjectilesComponent.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
         int var7 = var5.size();
         float var8 = var7 > 1 ? 20.0F / (var7 - 1) : 0.0F;
         float var9 = (var7 - 1) % 2 * var8 / 2.0F;
         Vec3d var10 = clientPlayerEntity.getRotationVec(f);
         Vec3d var11 = clientPlayerEntity.getOppositeRotationVector(f);

         for (int var12 = 0; var12 < var7; var12++) {
            ItemStack var13 = (ItemStack)var5.get(var12);
            if (!var13.isEmpty()) {
               float var14 = var9 + ((var12 & 1) == 1 ? -1.0F : 1.0F) * ((var12 + 1) / 2) * var8;
               Vec3d var15 = swing(var10, var11, var14).normalize().multiply(var6);
               if (var13.isOf(Items.FIREWORK_ROCKET)) {
                  this.release(clientWorld, clientPlayerEntity, Ballistics.Body.ROCKET, muzzle(clientPlayerEntity, f, 0.15), var15, rocketFuse(var13));
               } else {
                  this.release(clientWorld, clientPlayerEntity, Ballistics.Body.ARROW, muzzle(clientPlayerEntity, f, 0.1), var15, 240);
               }
            }
         }
      }
   }

   private static int rocketFuse(ItemStack itemStack) {
      FireworksComponent var1 = itemStack.get(DataComponentTypes.FIREWORKS);
      int var2 = 1 + (var1 != null ? var1.flightDuration() : 0);
      return 10 * var2 + 6;
   }

   private static Vec3d swing(Vec3d vec3d, Vec3d vec3d2, double d) {
      if (d == 0.0) {
         return vec3d;
      } else {
         double var4 = Math.toRadians(d);
         double var6 = Math.cos(var4);
         double var8 = Math.sin(var4);
         Vec3d var10 = vec3d2.normalize();
         return vec3d.multiply(var6).add(var10.crossProduct(vec3d).multiply(var8)).add(var10.multiply(var10.dotProduct(vec3d) * (1.0 - var6)));
      }
   }

   private static Vec3d muzzle(ClientPlayerEntity clientPlayerEntity, float f, double d) {
      Vec3d var4 = clientPlayerEntity.getLerpedPos(f);
      return new Vec3d(var4.x, var4.y + clientPlayerEntity.getStandingEyeHeight() - d, var4.z);
   }

   private void release(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, Ballistics.Body body, Vec3d vec3d, Vec3d vec3d2, int i) {
      if (this.volleySize != 3) {
         TrajectoriesModule.Aim var7;
         if (!(var7 = this.volley[this.volleySize++]).matches(body, vec3d, vec3d2, i, this.sweep)) {
            var7.hold(body, vec3d, vec3d2, i, this.sweep);
            var7.flight.launch(body, vec3d, vec3d2).lifetime(i).ignoring(clientPlayerEntity, null).solve(clientWorld);
            var7.caption(vec3d, this.flightTime.get());
         }
      }
   }

   private void drawPath(Ballistics.Flight flight, Vec3d vec3d, double d, float f, boolean bl, boolean bl2, boolean bl3, float g) {
      int var18 = flight.size();
      if (var18 >= 2 && !(f < 0.02F)) {
         double var19 = flight.length();
         if (vec3d != null) {
            double var21 = vec3d.x - flight.x(0);
            double var23 = vec3d.y - flight.y(0);
            double var25 = vec3d.z - flight.z(0);
            var19 += Math.sqrt(var21 * var21 + var23 * var23 + var25 * var25);
         }

         if (!(var19 < 1.0E-4)) {
            double var64 = Math.min(d, var19);
            boolean var65 = this.pathStyle.is("Solid");
            boolean var24 = this.pathStyle.is("Dashes");
            double var66 = var65 ? 0.6 : (var24 ? 0.72 : 0.42);
            double var27 = Math.max(1.0, var19 / (var66 * (bl ? 200 : 110)));
            double var29 = (var24 ? 0.72 : 0.42) * var27;
            double var31 = 0.44 * var27;
            double var33 = 0.6 * var27;
            double var35 = !var65 && this.flow.get() ? g * 1.3 % var29 : 0.0;
            this.strokeTotal = var19;
            this.strokeAlpha = f;
            this.strokeFade = bl;
            this.strokeSheen = bl2 ? g * 9.0 % (var19 + 14.0) - 1.6 : 0.0 / 0.0;
            this.strokeWidth = this.thickness.get();
            boolean var37 = this.strokeHalo = bl3 && this.halo.get();
            double var12;
            double var14;
            double var16;
            byte var38;
            if (vec3d != null) {
               var16 = vec3d.x;
               var14 = vec3d.y;
               var12 = vec3d.z;
               var38 = 0;
            } else {
               var16 = flight.x(0);
               var14 = flight.y(0);
               var12 = flight.z(0);
               var38 = 1;
            }

            double var39 = 0.0;

            for (int var41 = var38; var41 < var18 && var39 < var64; var41++) {
               double var50 = flight.x(var41);
               double var52 = var50 - var16;
               double var42;
               double var44;
               double var46;
               double var48;
               double var10 = Math.sqrt(
                  var52 * var52 + (var48 = (var46 = flight.y(var41)) - var14) * var48 + (var44 = (var42 = flight.z(var41)) - var12) * var44
               );
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

   private double beadLength(double d, double e, double f, double g, double h, double i, double j) {
      double var15 = d + g * j - this.camera.x;
      double var17 = e + h * j - this.camera.y;
      double var19 = f + i * j - this.camera.z;
      return MathHelper.clamp(Math.sqrt(var15 * var15 + var17 * var17 + var19 * var19) * 0.011, 0.035, 0.2);
   }

   private void stroke(double d, double e, double f, double g, double h, double i, double j, double k, double l) {
      float var22 = this.strokeAlpha;
      if (this.strokeFade) {
         var22 *= smooth((float)(l / 1.4));
      }

      if (!(var22 < 0.02F)) {
         float var23 = (float)(l / this.strokeTotal);
         int var24 = ColorHelper.lerp(var23, this.headTint, this.tailTint);
         double var20;
         if (!Double.isNaN(this.strokeSheen) && (var20 = 1.0 - Math.abs(l - this.strokeSheen) / 1.6) > 0.0) {
            var24 = ColorHelper.lerp((float)(var20 * 0.7), var24, this.sheenTint);
         }

         double var25 = d + g * j;
         double var27 = e + h * j;
         double var29 = f + i * j;
         double var31 = d + g * k;
         double var33 = e + h * k;
         double var35 = f + i * k;
         float var19;
         if (this.strokeHalo && (var19 = var22 * 0.22F * this.haloFade(var25, var27, var29)) > 0.0F) {
            this.canvas.line(var25, var27, var29, var31, var33, var35, ColorHelper.scaleAlpha(this.haloTint, var19), this.strokeWidth * 2.8F);
         }

         this.canvas.line(var25, var27, var29, var31, var33, var35, ColorHelper.scaleAlpha(var24, var22), this.strokeWidth);
      }
   }

   private float haloFade(double d, double e, double f) {
      double var7 = d - this.camera.x;
      double var9 = e - this.camera.y;
      double var11 = f - this.camera.z;
      double var13 = var7 * var7 + var9 * var9 + var11 * var11;
      if (var13 <= 576.0) {
         return 1.0F;
      } else {
         return var13 >= 1600.0 ? 0.0F : 1.0F - smooth((float)((Math.sqrt(var13) - 24.0) / 16.0));
      }
   }

   private void drawImpact(Ballistics.Flight flight, float f, float g, float h, float i, LivingEntity livingEntity) {
      switch (flight.outcome()) {
         case ENTITY:
            if (this.targetBox.get()) {
               this.drawTargetBox(flight.struck(), g, h, i, livingEntity);
            }
            break;
         case BLOCK:
            this.drawMarker(flight, f, g, h);
            break;
         case BURST:
            this.drawBurst(flight.impact(), f, g);
      }
   }

   private void drawMarker(Ballistics.Flight flight, float f, float g, float h) {
      if (!this.marker.is("None") && !(f <= 0.01F)) {
         Direction var5 = flight.face();
         Vec3d var6 = new Vec3d(var5.getOffsetX(), var5.getOffsetY(), var5.getOffsetZ());
         Vec3d var7 = new Vec3d(0.0, 1.0, 0.0);
         if (var5.getAxis() == Axis.Y) {
            Vec3d var8 = flight.heading();
            var7 = var8.x * var8.x + var8.z * var8.z < 1.0E-8 ? new Vec3d(0.0, 0.0, 1.0) : new Vec3d(var8.x, 0.0, var8.z).normalize();
         }

         Vec3d var12 = var7.crossProduct(var6).normalize();
         Vec3d var9 = flight.impact().add(var6.multiply(0.025));
         double var10 = 0.5 * this.markerScale.get() * f;
         this.gx = var9.x;
         this.gy = var9.y;
         this.gz = var9.z;
         this.rx = var12.x * var10;
         this.ry = var12.y * var10;
         this.rz = var12.z * var10;
         this.ux = var7.x * var10;
         this.uy = var7.y * var10;
         this.uz = var7.z * var10;
         if (this.marker.is("Ring")) {
            this.drawSunDisk(g, h);
         } else {
            this.drawBolt(g, h);
         }
      }
   }

   private void drawBolt(float f, float g) {
      int var3 = ColorHelper.scaleAlpha(this.markTint, f);
      this.disc(0.78, ColorHelper.scaleAlpha(this.plateTint, f * 0.16F));
      this.orbit(0.9, g * 0.6, ColorHelper.scaleAlpha(this.ringTint, f * 0.85F), this.thickness.get() * 0.75F);
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

   private void drawSunDisk(float f, float g) {
      int var3 = ColorHelper.scaleAlpha(this.markTint, f);
      float var4 = this.thickness.get();
      this.disc(0.95, ColorHelper.scaleAlpha(this.plateTint, f * 0.12F));
      this.disc(0.3, var3);
      int var5 = ColorHelper.scaleAlpha(this.ringTint, f);
      Vec3d var6 = this.at(0.42, 0.0);

      for (int var7 = 1; var7 <= 24; var7++) {
         Vec3d var8 = this.at(0.42 * RIM_COS[var7], 0.42 * RIM_SIN[var7]);
         this.line(var6, var8, var5, var4);
         var6 = var8;
      }

      double var18 = g * 0.35;

      for (int var9 = 0; var9 < 12; var9++) {
         double var10 = var18 + 6.283185307179586 * var9 / 12.0;
         double var12 = Math.cos(var10);
         double var14 = Math.sin(var10);
         double var16 = (var9 & 1) == 0 ? 0.8 : 0.66;
         this.line(this.at(0.52 * var12, 0.52 * var14), this.at(var16 * var12, var16 * var14), var3, var4);
      }
   }

   private void drawBurst(Vec3d vec3d, float f, float g) {
      int var4 = ColorHelper.scaleAlpha(this.markTint, g);
      double var5 = 0.55 * this.markerScale.get() * f;
      float var7 = this.thickness.get();

      for (double[] var11 : BURST_RAYS) {
         Vec3d var12 = new Vec3d(var11[0], var11[1], var11[2]);
         this.line(vec3d.add(var12.multiply(var5 * 0.25)), vec3d.add(var12.multiply(var5)), var4, var7);
      }
   }

   private void drawTargetBox(Entity entity, float f, float g, float h, LivingEntity livingEntity) {
      if (entity != null && !entity.isRemoved()) {
         Vec3d var6 = entity.getLerpedPos(h).subtract(entity.getEntityPos());
         Box var7 = entity.getBoundingBox().offset(var6).expand(0.06);
         if (!var7.contains(this.camera)) {
            boolean var8 = entity == livingEntity;
            float var9 = var8 ? 0.5F + 0.5F * MathHelper.sin(g * 6.5F) : 0.0F;
            int var10 = ColorHelper.scaleAlpha(var8 ? ColorHelper.lerp(0.35F * var9, this.hitTint, -1) : this.hitTint, f);
            VisibilityConfigurable var11 = GizmoDrawing.box(var7, DrawStyle.filled(ColorHelper.scaleAlpha(var10, var8 ? 0.14F + 0.12F * var9 : 0.1F)));
            if (this.onTop) {
               var11.ignoreOcclusion();
            }

            this.brackets(var7, var8 ? 0.34 + 0.06 * var9 : 0.26, var10, this.thickness.get() * (var8 ? 1.2F : 1.0F));
         }
      }
   }

   private void brackets(Box box, double d, int i, float f) {
      double var6 = (box.maxX - box.minX) * d;
      double var8 = (box.maxY - box.minY) * d;
      double var10 = (box.maxZ - box.minZ) * d;

      for (int var12 = 0; var12 < 8; var12++) {
         boolean var13 = (var12 & 1) != 0;
         boolean var14 = (var12 & 2) != 0;
         boolean var15 = (var12 & 4) != 0;
         double var16 = var13 ? box.maxX : box.minX;
         double var18 = var14 ? box.maxY : box.minY;
         double var20 = var15 ? box.maxZ : box.minZ;
         Vec3d var22 = new Vec3d(var16, var18, var20);
         this.line(var22, new Vec3d(var13 ? var16 - var6 : var16 + var6, var18, var20), i, f);
         this.line(var22, new Vec3d(var16, var14 ? var18 - var8 : var18 + var8, var20), i, f);
         this.line(var22, new Vec3d(var16, var18, var15 ? var20 - var10 : var20 + var10), i, f);
      }
   }

   private void disc(double d, int i) {
      Vec3d var4 = this.at(0.0, 0.0);

      for (int var5 = 0; var5 < 24; var5++) {
         this.facet(var4, var4, this.at(d * RIM_COS[var5], d * RIM_SIN[var5]), this.at(d * RIM_COS[var5 + 1], d * RIM_SIN[var5 + 1]), i);
      }
   }

   private void orbit(double d, double e, int i, float f) {
      double var7 = 0.6283185307179586;
      double var9 = var7 * 0.55;

      for (int var11 = 0; var11 < 10; var11++) {
         double var12 = e + var11 * var7;
         Vec3d var14 = this.at(d * Math.cos(var12), d * Math.sin(var12));

         for (int var15 = 1; var15 <= 3; var15++) {
            double var16 = var12 + var9 * var15 / 3.0;
            Vec3d var18 = this.at(d * Math.cos(var16), d * Math.sin(var16));
            this.line(var14, var18, i, f);
            var14 = var18;
         }
      }
   }

   private Vec3d at(double d, double e) {
      return new Vec3d(this.gx + this.rx * d + this.ux * e, this.gy + this.ry * d + this.uy * e, this.gz + this.rz * d + this.uz * e);
   }

   private void facet(Vec3d vec3d, Vec3d vec3d2, Vec3d vec3d3, Vec3d vec3d4, int i) {
      double var6 = vec3d3.y - vec3d.y;
      double var8 = vec3d4.z - vec3d2.z;
      double var10 = vec3d3.z - vec3d.z;
      double var12 = vec3d4.y - vec3d2.y;
      double var14 = var6 * var8 - var10 * var12;
      double var16 = vec3d4.x - vec3d2.x;
      double var18 = vec3d3.x - vec3d.x;
      double var20 = var10 * var16 - var18 * var8;
      double var22 = var18 * var12 - var6 * var16;
      double var24 = var14 * (this.camera.x - vec3d.x) + var20 * (this.camera.y - vec3d.y) + var22 * (this.camera.z - vec3d.z);
      if (var24 >= 0.0) {
         this.canvas.quad(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z, vec3d3.x, vec3d3.y, vec3d3.z, vec3d4.x, vec3d4.y, vec3d4.z, i);
      } else {
         this.canvas.quad(vec3d4.x, vec3d4.y, vec3d4.z, vec3d3.x, vec3d3.y, vec3d3.z, vec3d2.x, vec3d2.y, vec3d2.z, vec3d.x, vec3d.y, vec3d.z, i);
      }
   }

   private void line(Vec3d vec3d, Vec3d vec3d2, int i, float f) {
      this.canvas.line(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z, i, f);
   }

   private void addLabel(TrajectoriesModule.Plot plot) {
      if (this.flightTime.get() && plot.seconds != null) {
         Ballistics.Flight var2 = plot.flight;
         Vec3d var3 = var2.impact();
         Entity var4 = var2.struck();
         Vec3d var5 = var4 != null ? new Vec3d(var3.x, var4.getBoundingBox().maxY + 0.35, var3.z) : var3.add(0.0, 0.55, 0.0);
         this.labels.add(new TrajectoriesModule.Label(var5, plot.seconds, plot.detail));
      }
   }

   @Override
   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.flightTime.get();
   }

   @Override
   public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
      if (!this.labels.isEmpty() && System.nanoTime() - this.labelsAt <= 250000000L) {
         MinecraftClient var4 = MinecraftClient.getInstance();

         for (TrajectoriesModule.Label var6 : this.labels) {
            float[] var7 = WorldLabel.project(var4, var6.pos(), f, g);
            if (var7 != null) {
               WorldLabel.drawText(imDrawList, var7[0], var7[1], var6.primary(), var6.secondary(), 1.0F);
            }
         }
      }
   }

   private static float smooth(float f) {
      f = MathHelper.clamp(f, 0.0F, 1.0F);
      return f * f * (3.0F - 2.0F * f);
   }

   private static float easeOutExpo(float f) {
      return f >= 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0 * Math.max(0.0F, f));
   }

   private static float settle(float f) {
      return 1.0F - (float)(Math.exp(-5.5 * f) * Math.cos(8.5 * f));
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

      boolean matches(Ballistics.Body body, Vec3d vec3d, Vec3d vec3d2, int i, int j) {
         return this.held
            && this.stamp == j
            && this.body == body
            && this.life == i
            && Math.abs(this.x - vec3d.x) < 0.001
            && Math.abs(this.y - vec3d.y) < 0.001
            && Math.abs(this.z - vec3d.z) < 0.001
            && Math.abs(this.vx - vec3d2.x) < 5.0E-4
            && Math.abs(this.vy - vec3d2.y) < 5.0E-4
            && Math.abs(this.vz - vec3d2.z) < 5.0E-4;
      }

      void hold(Ballistics.Body body, Vec3d vec3d, Vec3d vec3d2, int i, int j) {
         this.body = body;
         this.x = vec3d.x;
         this.y = vec3d.y;
         this.z = vec3d.z;
         this.vx = vec3d2.x;
         this.vy = vec3d2.y;
         this.vz = vec3d2.z;
         this.life = i;
         this.stamp = j;
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

      void caption(Vec3d vec3d, boolean bl) {
         this.seconds = null;
         this.detail = null;
         if (bl && this.flight.outcome() != Ballistics.Outcome.OPEN && this.flight.size() >= 2) {
            Entity var3 = this.flight.struck();
            this.seconds = String.format(Locale.ROOT, "%.2fs", this.flight.ticks() / 20.0F);
            this.detail = var3 != null
               ? NameProtectModule.protect(var3.getName()).getString()
               : String.format(Locale.ROOT, "%.0fm", vec3d.distanceTo(this.flight.impact()));
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
