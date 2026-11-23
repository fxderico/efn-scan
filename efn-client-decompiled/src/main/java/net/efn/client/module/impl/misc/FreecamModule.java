package net.efn.client.module.impl.misc;

import com.example.client.mixin.AccessorMovementClientInput;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.movement.AutoWalkModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class FreecamModule extends ClientModule {
   private static final int WALK_FWD = 1;
   private static final int WALK_BACK = 2;
   private static final int WALK_LEFT = 4;
   private static final int WALK_RIGHT = 8;
   private static final int WALK_JUMP = 16;
   private static final int WALK_SNEAK = 32;
   private static final int WALK_SPRINT = 64;
   private static final int WALK_FLAT = 15;
   private static final int LEARN_TICKS = 40;
   private static final int CHUNK_CAP = 32;
   private static final float SCROLL_STEP = 1.2F;
   public static volatile FreecamModule active;
   private final NumberSetting speed = this.addSetting("speed", new NumberSetting("Speed", 1.0F, 0.1F, 50.0F).step(0.1F));
   private final NumberSetting verticalSpeed = this.addSetting("vertical-speed", new NumberSetting("Vertical", 1.0F, 0.2F, 5.0F).step(0.05F));
   private final BooleanSetting smoothing = this.addSetting("smoothing", new BooleanSetting("Smooth", true));
   private final BooleanSetting showOwnBody = this.addSetting("show-own-body", new BooleanSetting("Body", true));
   private final BooleanSetting showHands = this.addSetting("show-hands", new BooleanSetting("Hands", true));
   private final BooleanSetting bodyFollowsKeys = this.addSetting("body-uses-movement-keys", new BooleanSetting("Keys Move Body", false));
   private final BooleanSetting keepWalking = this.addSetting("keep-walking", new BooleanSetting("Keep Walking", true));
   private final BooleanSetting scrollSpeed = this.addSetting("scroll-speed", new BooleanSetting("Scroll Speed", true));
   private final BooleanSetting scrollResets = this.addSetting("scroll-resets", new BooleanSetting("Reset Scroll Speed", true));
   private final NumberSetting chunkDistance = this.addSetting("chunk-distance", new NumberSetting("Chunks", 12.0F, 2.0F, 32.0F).step(1.0F));
   private Vec3d pos = Vec3d.field_1353;
   private Vec3d prev = Vec3d.field_1353;
   private float yaw;
   private float pitch;
   private boolean flyingBefore;
   private boolean cullBefore;
   private Perspective viewBefore;
   private boolean viewPushed;
   private ChunkPos focus;
   private int radius = -1;
   private boolean waiting;
   private boolean live;
   private int walk;
   private int learn;
   private float scrolled = 1.0F;

   public FreecamModule() {
      super("freecam", "Freecam", "Detached camera.", ModuleCategory.MISC);
      this.scrollResets.visibleWhen(this.scrollSpeed::get);
      this.speed.onChange(() -> this.scrolled = 1.0F);
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.waiting = true;
      this.live = false;
      this.on(TickEvent.TYPE, this::onTick);
   }

   @Override
   protected void onDisable() {
      MinecraftClient var1 = MinecraftClient.method_1551();
      boolean var2 = this.live;
      this.waiting = false;
      this.live = false;
      if (active == this) {
         active = null;
      }

      this.walk = 0;
      this.learn = 0;
      this.scrolled = 1.0F;
      if (this.viewPushed) {
         var1.field_1690.method_31043(this.viewBefore);
         this.viewPushed = false;
      }

      if (var2) {
         var1.field_1730 = this.cullBefore;
         if (var1.field_1761 != null) {
            var1.field_1761.method_2925();
         }

         this.releaseChunks(var1);
         if (var1.field_1724 != null) {
            var1.field_1724.method_31549().field_7479 = this.flyingBefore;
         }
      }
   }

   private void onTick(TickEvent var1) {
      MinecraftClient var2 = MinecraftClient.method_1551();
      this.open(var2);
      ClientPlayerEntity var3 = var2.field_1724;
      if (this.live && var3 != null) {
         if (this.learn > 0) {
            this.learn--;
            this.absorb(var3, false);
         }

         this.prev = this.pos;
         if (var3.method_31549().field_7477) {
            var3.method_31549().field_7479 = false;
         }

         this.stepCamera(var2.field_1690);
         this.followChunks(var2);
      }
   }

   private void open(MinecraftClient var1) {
      ClientPlayerEntity var2 = var1.field_1724;
      if (this.waiting && var2 != null && var1.field_1687 != null) {
         this.flyingBefore = var2.method_31549().field_7479;
         this.pos = this.prev = new Vec3d(var2.method_23317(), var2.method_23318() + var2.method_18381(EntityPose.field_18076), var2.method_23321());
         this.yaw = var2.method_36454();
         this.pitch = var2.method_36455();
         this.viewPushed = false;
         if (this.showOwnBody.get()) {
            this.viewBefore = var1.field_1690.method_31044();
            if (this.viewBefore.method_31034()) {
               var1.field_1690.method_31043(Perspective.field_26665);
               this.viewPushed = true;
            }
         }

         this.waiting = false;
         this.live = true;
         active = this;
         this.walk = 0;
         this.absorb(var2, true);
         this.learn = 40;
         this.focus = null;
         this.radius = -1;
         this.cullBefore = var1.field_1730;
         var1.field_1730 = false;
         this.followChunks(var1);
         drive(var1);
      }
   }

   private void stepCamera(GameOptions var1) {
      double var2 = (var1.field_1894.method_1434() ? 1 : 0) - (var1.field_1881.method_1434() ? 1 : 0);
      double var4 = (var1.field_1913.method_1434() ? 1 : 0) - (var1.field_1849.method_1434() ? 1 : 0);
      double var6 = (var1.field_1903.method_1434() ? 1 : 0) - (var1.field_1832.method_1434() ? 1 : 0);
      double var8 = this.flySpeed() * (this.smoothing.get() ? 0.5 : 1.0);
      Vec3d var10 = Vec3d.method_1030(0.0F, this.yaw);
      Vec3d var11 = new Vec3d(var10.field_1350, 0.0, -var10.field_1352);
      this.pos = this.pos
         .method_1031(
            (var10.field_1352 * var2 + var11.field_1352 * var4) * var8,
            var6 * var8 * this.verticalSpeed.get(),
            (var10.field_1350 * var2 + var11.field_1350 * var4) * var8
         );
   }

   private float flySpeed() {
      return !this.scrollResets.get() ? this.speed.get() : MathHelper.method_15363(this.speed.get() * this.scrolled, this.speed.min(), this.speed.max());
   }

   private void absorb(ClientPlayerEntity var1, boolean var2) {
      GameOptions var4 = MinecraftClient.method_1551().field_1690;
      PlayerInput var5 = var1.field_3913.field_54155;
      boolean var6 = this.keepWalking.get();
      boolean var3 = var6 && (var2 || this.bodyFollowsKeys.get());
      if (var3) {
         this.setWalk(1, var4.field_1894.method_1434() || var5.comp_3159());
         this.setWalk(2, var4.field_1881.method_1434() || var5.comp_3160());
         this.setWalk(4, var4.field_1913.method_1434() || var5.comp_3161());
         this.setWalk(8, var4.field_1849.method_1434() || var5.comp_3162());
         this.setWalk(16, var4.field_1903.method_1434() || var5.comp_3163());
         this.setWalk(32, var4.field_1832.method_1434() || var5.comp_3164() || var2 && var1.method_5715());
         this.setWalk(64, var4.field_1867.method_1434() || var5.comp_3165());
      } else if (var1.method_5715()) {
         this.walk |= 32;
      }

      AutoWalkModule var8 = ModuleRegistry.get(AutoWalkModule.class);
      if (var8 != null && var8.isEnabled()) {
         this.walk |= 1;
      }

      if (var6) {
         if (var1.field_6250 > 0.02F) {
            this.walk |= 1;
         }

         if (var1.field_6250 < -0.02F) {
            this.walk |= 2;
         }

         if (var1.field_6212 > 0.02F) {
            this.walk |= 4;
         }

         if (var1.field_6212 < -0.02F) {
            this.walk |= 8;
         }

         if (!var2 || (this.walk & 15) == 0) {
            this.aimFromMotion(var1);
         }
      }
   }

   private void setWalk(int var1, boolean var2) {
      if (var2) {
         this.walk |= var1;
      }
   }

   private void aimFromMotion(ClientPlayerEntity var1) {
      Vec3d var2 = var1.method_18798();
      double var3 = var2.field_1352;
      double var5 = var2.field_1350;
      if (!(var3 * var3 + var5 * var5 < 1.0E-8)) {
         Vec3d var7 = Vec3d.method_1030(0.0F, var1.method_36454());
         double var8 = var3 * var7.field_1352 + var5 * var7.field_1350;
         double var10 = var5 * var7.field_1352 - var3 * var7.field_1350;
         if (Math.abs(var8) >= Math.abs(var10)) {
            if (var8 > 0.01) {
               this.walk |= 1;
            } else if (var8 < -0.01) {
               this.walk |= 2;
            }
         } else if (var10 > 0.01) {
            this.walk |= 4;
         } else if (var10 < -0.01) {
            this.walk |= 8;
         }
      }
   }

   public static void drive(MinecraftClient var0) {
      FreecamModule var1 = active;
      if (var1 != null && var0.field_1724 != null) {
         var1.pushWalk(var0);
      }
   }

   private void pushWalk(MinecraftClient var1) {
      GameOptions var3 = var1.field_1690;
      boolean var4 = this.bodyFollowsKeys.get();
      boolean var5 = this.on(1) || var4 && var3.field_1894.method_1434();
      boolean var6 = this.on(2) || var4 && var3.field_1881.method_1434();
      boolean var7 = this.on(4) || var4 && var3.field_1913.method_1434();
      boolean var8 = this.on(8) || var4 && var3.field_1849.method_1434();
      boolean var9 = this.on(16);
      boolean var10 = this.on(32) || var4 && var3.field_1832.method_1434();
      boolean var2 = this.on(64) || var4 && var3.field_1867.method_1434();
      if (var4) {
         if (var3.field_1881.method_1434()) {
            this.walk &= -2;
         }

         if (var3.field_1894.method_1434()) {
            this.walk &= -3;
         }

         if (var3.field_1849.method_1434()) {
            this.walk &= -5;
         }

         if (var3.field_1913.method_1434()) {
            this.walk &= -9;
         }
      }

      ClientPlayerEntity var12 = var1.field_1724;
      var12.field_3913.field_54155 = new PlayerInput(var5, var6, var7, var8, var9, var10, var2);
      float var13 = (var7 ? 1.0F : 0.0F) - (var8 ? 1.0F : 0.0F);
      float var14 = (var5 ? 1.0F : 0.0F) - (var6 ? 1.0F : 0.0F);
      ((AccessorMovementClientInput)var12.field_3913).efn$setMoveVector(new Vec2f(var13, var14).method_35581());
      var12.method_5660(var10);
   }

   private boolean on(int var1) {
      return (this.walk & var1) != 0;
   }

   private void followChunks(MinecraftClient var1) {
      if (this.live && var1.field_1687 != null && var1.field_1724 != null) {
         ClientChunkManager var3 = var1.field_1687.method_2935();
         ChunkPos var4 = new ChunkPos(MathHelper.method_15357(this.pos.field_1352) >> 4, MathHelper.method_15357(this.pos.field_1350) >> 4);
         ChunkPos var5 = var1.field_1724.method_31476();
         int var6 = Math.max(Math.abs(var4.field_9181 - var5.field_9181), Math.abs(var4.field_9180 - var5.field_9180));
         int var2;
         ChunkPos var7 = var6 > (var2 = MathHelper.method_15340(this.chunkDistance.getInt(), 2, 32))
            ? new ChunkPos(var5.field_9181 + (var4.field_9181 - var5.field_9181) / 2, var5.field_9180 + (var4.field_9180 - var5.field_9180) / 2)
            : var4;
         int var8 = MathHelper.method_15340(Math.max(var2, var6 / 2 + 3), 2, 32);
         if (var8 != this.radius) {
            var3.method_20180(var8);
            this.radius = var8;
         }

         if (this.focus == null || var7.field_9181 != this.focus.field_9181 || var7.field_9180 != this.focus.field_9180) {
            var3.method_20317(var7.field_9181, var7.field_9180);
            this.focus = var7;
            if (var1.field_1769 != null) {
               var1.field_1769.method_3292();
            }
         }
      }
   }

   private void releaseChunks(MinecraftClient var1) {
      this.focus = null;
      this.radius = -1;
      if (var1.field_1687 != null) {
         ClientChunkManager var2 = var1.field_1687.method_2935();
         if (var1.field_1724 != null) {
            ChunkPos var3 = var1.field_1724.method_31476();
            var2.method_20317(var3.field_9181, var3.field_9180);
         }

         var2.method_20180(var1.field_1690.method_38521());
         if (var1.field_1769 != null) {
            var1.field_1769.method_3292();
         }
      }
   }

   public boolean drawsSelf(Entity var1, PlayerEntity var2) {
      MinecraftClient var3 = MinecraftClient.method_1551();
      return this.showOwnBody.get() && var3.field_1724 != null && var1 == var3.field_1724 && var2 == var3.field_1724;
   }

   public boolean keepHands() {
      return this.showHands.get();
   }

   public boolean showBody() {
      return this.showOwnBody.get();
   }

   public static boolean scroll(double var0) {
      FreecamModule var2 = active;
      if (var2 != null && var2.scrollSpeed.get() && var0 != 0.0) {
         float var3 = var0 > 0.0 ? 1.2F : 0.8333333F;
         NumberSetting var4 = var2.speed;
         if (var2.scrollResets.get()) {
            var2.scrolled = MathHelper.method_15363(var2.scrolled * var3, var4.min() / var4.get(), var4.max() / var4.get());
         } else {
            var4.set(var4.get() * var3);
         }

         return true;
      } else {
         return false;
      }
   }

   public void turn(double var1, double var3) {
      this.yaw += (float)var1 * 0.15F;
      this.pitch = MathHelper.method_15363(this.pitch + (float)var3 * 0.15F, -90.0F, 90.0F);
   }

   public double getX(float var1) {
      return MathHelper.method_16436(var1, this.prev.field_1352, this.pos.field_1352);
   }

   public double getY(float var1) {
      return MathHelper.method_16436(var1, this.prev.field_1351, this.pos.field_1351);
   }

   public double getZ(float var1) {
      return MathHelper.method_16436(var1, this.prev.field_1350, this.pos.field_1350);
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }
}
