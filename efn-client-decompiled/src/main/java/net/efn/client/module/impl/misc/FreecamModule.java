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
   private Vec3d pos = Vec3d.ZERO;
   private Vec3d prev = Vec3d.ZERO;
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
      MinecraftClient var1 = MinecraftClient.getInstance();
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
         var1.options.setPerspective(this.viewBefore);
         this.viewPushed = false;
      }

      if (var2) {
         var1.chunkCullingEnabled = this.cullBefore;
         if (var1.interactionManager != null) {
            var1.interactionManager.cancelBlockBreaking();
         }

         this.releaseChunks(var1);
         if (var1.player != null) {
            var1.player.getAbilities().flying = this.flyingBefore;
         }
      }
   }

   private void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      this.open(var2);
      ClientPlayerEntity var3 = var2.player;
      if (this.live && var3 != null) {
         if (this.learn > 0) {
            this.learn--;
            this.absorb(var3, false);
         }

         this.prev = this.pos;
         if (var3.getAbilities().creativeMode) {
            var3.getAbilities().flying = false;
         }

         this.stepCamera(var2.options);
         this.followChunks(var2);
      }
   }

   private void open(MinecraftClient minecraftClient) {
      ClientPlayerEntity var2 = minecraftClient.player;
      if (this.waiting && var2 != null && minecraftClient.world != null) {
         this.flyingBefore = var2.getAbilities().flying;
         this.pos = this.prev = new Vec3d(var2.getX(), var2.getY() + var2.getEyeHeight(EntityPose.STANDING), var2.getZ());
         this.yaw = var2.getYaw();
         this.pitch = var2.getPitch();
         this.viewPushed = false;
         if (this.showOwnBody.get()) {
            this.viewBefore = minecraftClient.options.getPerspective();
            if (this.viewBefore.isFirstPerson()) {
               minecraftClient.options.setPerspective(Perspective.THIRD_PERSON_BACK);
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
         this.cullBefore = minecraftClient.chunkCullingEnabled;
         minecraftClient.chunkCullingEnabled = false;
         this.followChunks(minecraftClient);
         drive(minecraftClient);
      }
   }

   private void stepCamera(GameOptions gameOptions) {
      double var2 = (gameOptions.forwardKey.isPressed() ? 1 : 0) - (gameOptions.backKey.isPressed() ? 1 : 0);
      double var4 = (gameOptions.leftKey.isPressed() ? 1 : 0) - (gameOptions.rightKey.isPressed() ? 1 : 0);
      double var6 = (gameOptions.jumpKey.isPressed() ? 1 : 0) - (gameOptions.sneakKey.isPressed() ? 1 : 0);
      double var8 = this.flySpeed() * (this.smoothing.get() ? 0.5 : 1.0);
      Vec3d var10 = Vec3d.fromPolar(0.0F, this.yaw);
      Vec3d var11 = new Vec3d(var10.z, 0.0, -var10.x);
      this.pos = this.pos.add((var10.x * var2 + var11.x * var4) * var8, var6 * var8 * this.verticalSpeed.get(), (var10.z * var2 + var11.z * var4) * var8);
   }

   private float flySpeed() {
      return !this.scrollResets.get() ? this.speed.get() : MathHelper.clamp(this.speed.get() * this.scrolled, this.speed.min(), this.speed.max());
   }

   private void absorb(ClientPlayerEntity clientPlayerEntity, boolean bl) {
      GameOptions var4 = MinecraftClient.getInstance().options;
      PlayerInput var5 = clientPlayerEntity.input.playerInput;
      boolean var6 = this.keepWalking.get();
      boolean var3 = var6 && (bl || this.bodyFollowsKeys.get());
      if (var3) {
         this.setWalk(1, var4.forwardKey.isPressed() || var5.forward());
         this.setWalk(2, var4.backKey.isPressed() || var5.backward());
         this.setWalk(4, var4.leftKey.isPressed() || var5.left());
         this.setWalk(8, var4.rightKey.isPressed() || var5.right());
         this.setWalk(16, var4.jumpKey.isPressed() || var5.jump());
         this.setWalk(32, var4.sneakKey.isPressed() || var5.sneak() || bl && clientPlayerEntity.isSneaking());
         this.setWalk(64, var4.sprintKey.isPressed() || var5.sprint());
      } else if (clientPlayerEntity.isSneaking()) {
         this.walk |= 32;
      }

      AutoWalkModule var8 = ModuleRegistry.get(AutoWalkModule.class);
      if (var8 != null && var8.isEnabled()) {
         this.walk |= 1;
      }

      if (var6) {
         if (clientPlayerEntity.forwardSpeed > 0.02F) {
            this.walk |= 1;
         }

         if (clientPlayerEntity.forwardSpeed < -0.02F) {
            this.walk |= 2;
         }

         if (clientPlayerEntity.sidewaysSpeed > 0.02F) {
            this.walk |= 4;
         }

         if (clientPlayerEntity.sidewaysSpeed < -0.02F) {
            this.walk |= 8;
         }

         if (!bl || (this.walk & 15) == 0) {
            this.aimFromMotion(clientPlayerEntity);
         }
      }
   }

   private void setWalk(int i, boolean bl) {
      if (bl) {
         this.walk |= i;
      }
   }

   private void aimFromMotion(ClientPlayerEntity clientPlayerEntity) {
      Vec3d var2 = clientPlayerEntity.getVelocity();
      double var3 = var2.x;
      double var5 = var2.z;
      if (!(var3 * var3 + var5 * var5 < 1.0E-8)) {
         Vec3d var7 = Vec3d.fromPolar(0.0F, clientPlayerEntity.getYaw());
         double var8 = var3 * var7.x + var5 * var7.z;
         double var10 = var5 * var7.x - var3 * var7.z;
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

   public static void drive(MinecraftClient minecraftClient) {
      FreecamModule var1 = active;
      if (var1 != null && minecraftClient.player != null) {
         var1.pushWalk(minecraftClient);
      }
   }

   private void pushWalk(MinecraftClient minecraftClient) {
      GameOptions var3 = minecraftClient.options;
      boolean var4 = this.bodyFollowsKeys.get();
      boolean var5 = this.on(1) || var4 && var3.forwardKey.isPressed();
      boolean var6 = this.on(2) || var4 && var3.backKey.isPressed();
      boolean var7 = this.on(4) || var4 && var3.leftKey.isPressed();
      boolean var8 = this.on(8) || var4 && var3.rightKey.isPressed();
      boolean var9 = this.on(16);
      boolean var10 = this.on(32) || var4 && var3.sneakKey.isPressed();
      boolean var2 = this.on(64) || var4 && var3.sprintKey.isPressed();
      if (var4) {
         if (var3.backKey.isPressed()) {
            this.walk &= -2;
         }

         if (var3.forwardKey.isPressed()) {
            this.walk &= -3;
         }

         if (var3.rightKey.isPressed()) {
            this.walk &= -5;
         }

         if (var3.leftKey.isPressed()) {
            this.walk &= -9;
         }
      }

      ClientPlayerEntity var12 = minecraftClient.player;
      var12.input.playerInput = new PlayerInput(var5, var6, var7, var8, var9, var10, var2);
      float var13 = (var7 ? 1.0F : 0.0F) - (var8 ? 1.0F : 0.0F);
      float var14 = (var5 ? 1.0F : 0.0F) - (var6 ? 1.0F : 0.0F);
      ((AccessorMovementClientInput)var12.input).efn$setMoveVector(new Vec2f(var13, var14).normalize());
      var12.setSneaking(var10);
   }

   private boolean on(int i) {
      return (this.walk & i) != 0;
   }

   private void followChunks(MinecraftClient minecraftClient) {
      if (this.live && minecraftClient.world != null && minecraftClient.player != null) {
         ClientChunkManager var3 = minecraftClient.world.getChunkManager();
         ChunkPos var4 = new ChunkPos(MathHelper.floor(this.pos.x) >> 4, MathHelper.floor(this.pos.z) >> 4);
         ChunkPos var5 = minecraftClient.player.getChunkPos();
         int var6 = Math.max(Math.abs(var4.x - var5.x), Math.abs(var4.z - var5.z));
         int var2;
         ChunkPos var7 = var6 > (var2 = MathHelper.clamp(this.chunkDistance.getInt(), 2, 32))
            ? new ChunkPos(var5.x + (var4.x - var5.x) / 2, var5.z + (var4.z - var5.z) / 2)
            : var4;
         int var8 = MathHelper.clamp(Math.max(var2, var6 / 2 + 3), 2, 32);
         if (var8 != this.radius) {
            var3.updateLoadDistance(var8);
            this.radius = var8;
         }

         if (this.focus == null || var7.x != this.focus.x || var7.z != this.focus.z) {
            var3.setChunkMapCenter(var7.x, var7.z);
            this.focus = var7;
            if (minecraftClient.worldRenderer != null) {
               minecraftClient.worldRenderer.scheduleTerrainUpdate();
            }
         }
      }
   }

   private void releaseChunks(MinecraftClient minecraftClient) {
      this.focus = null;
      this.radius = -1;
      if (minecraftClient.world != null) {
         ClientChunkManager var2 = minecraftClient.world.getChunkManager();
         if (minecraftClient.player != null) {
            ChunkPos var3 = minecraftClient.player.getChunkPos();
            var2.setChunkMapCenter(var3.x, var3.z);
         }

         var2.updateLoadDistance(minecraftClient.options.getClampedViewDistance());
         if (minecraftClient.worldRenderer != null) {
            minecraftClient.worldRenderer.scheduleTerrainUpdate();
         }
      }
   }

   public boolean drawsSelf(Entity entity, PlayerEntity playerEntity) {
      MinecraftClient var3 = MinecraftClient.getInstance();
      return this.showOwnBody.get() && var3.player != null && entity == var3.player && playerEntity == var3.player;
   }

   public boolean keepHands() {
      return this.showHands.get();
   }

   public boolean showBody() {
      return this.showOwnBody.get();
   }

   public static boolean scroll(double d) {
      FreecamModule var2 = active;
      if (var2 != null && var2.scrollSpeed.get() && d != 0.0) {
         float var3 = d > 0.0 ? 1.2F : 0.8333333F;
         NumberSetting var4 = var2.speed;
         if (var2.scrollResets.get()) {
            var2.scrolled = MathHelper.clamp(var2.scrolled * var3, var4.min() / var4.get(), var4.max() / var4.get());
         } else {
            var4.set(var4.get() * var3);
         }

         return true;
      } else {
         return false;
      }
   }

   public void turn(double d, double e) {
      this.yaw += (float)d * 0.15F;
      this.pitch = MathHelper.clamp(this.pitch + (float)e * 0.15F, -90.0F, 90.0F);
   }

   public double getX(float f) {
      return MathHelper.lerp((double)f, this.prev.x, this.pos.x);
   }

   public double getY(float f) {
      return MathHelper.lerp((double)f, this.prev.y, this.pos.y);
   }

   public double getZ(float f) {
      return MathHelper.lerp((double)f, this.prev.z, this.pos.z);
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }
}
