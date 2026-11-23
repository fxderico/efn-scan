package net.efn.client.module.impl.movement;

import com.example.client.mixin.AccessorJesusServerboundMovePlayerPacket;
import com.example.client.mixin.AccessorServerboundMovePlayerPacket;
import java.util.Iterator;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.PlayerMoveEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.CollisionView;

@Environment(EnvType.CLIENT)
public final class JesusModule extends ClientModule {
   public static volatile JesusModule active;
   private static final String SOLID = "Solid";
   private static final String IGNORE = "Ignore";
   private static final double ASCEND_SPEED = 0.11;
   private static final double NCP_STRAFE_SPEED = 0.2873;
   private static final int NCP_BOB_INTERVAL = 15;
   private static final int OVER_WATER = 1;
   private static final int OVER_LAVA = 2;
   private final BooleanSetting powderSnow = this.addSetting("powder-snow", new BooleanSetting("Powder Snow", true));
   private final BooleanSetting ncpBypass = this.addSetting("ncp-bypass", new BooleanSetting("NCP Bypass", false));
   private final BooleanSetting slowDown = this.addSetting("slow-down", new BooleanSetting("Slow Down", false));
   private final ModeSetting waterMode = this.addSetting("water-mode", new ModeSetting("Water Mode", "Solid", "Solid", "Ignore"));
   private final BooleanSetting dipIfBurning = this.addSetting("dip-if-burning", new BooleanSetting("Dip If Burning", true));
   private final BooleanSetting dipOnSneakWater = this.addSetting("water-dip-on-sneak", new BooleanSetting("Water Dip On Sneak", true));
   private final BooleanSetting dipOnFallWater = this.addSetting("water-dip-on-fall", new BooleanSetting("Water Dip On Fall", true));
   private final NumberSetting dipFallHeightWater = this.addSetting(
      "water-dip-fall-height", new NumberSetting("Water Dip Fall Height", 4.0F, 1.0F, 255.0F).step(1.0F)
   );
   private final ModeSetting lavaMode = this.addSetting("lava-mode", new ModeSetting("Lava Mode", "Solid", "Solid", "Ignore"));
   private final BooleanSetting dipIfFireResistant = this.addSetting("dip-if-resistant", new BooleanSetting("Dip If Resistant", true));
   private final BooleanSetting dipOnSneakLava = this.addSetting("lava-dip-on-sneak", new BooleanSetting("Lava Dip On Sneak", true));
   private final BooleanSetting dipOnFallLava = this.addSetting("lava-dip-on-fall", new BooleanSetting("Lava Dip On Fall", true));
   private final NumberSetting dipFallHeightLava = this.addSetting(
      "lava-dip-fall-height", new NumberSetting("Lava Dip Fall Height", 4.0F, 1.0F, 255.0F).step(1.0F)
   );
   private int ascending = 10;
   private int swimmingTicks;
   private boolean inBubbleColumn;
   private boolean moveIsVelocity;

   public JesusModule() {
      super("jesus", "Jesus", "Walk on liquids and powder snow like Jesus.", ModuleCategory.MOVEMENT);
      this.slowDown.visibleWhen(this.ncpBypass::get);
      this.dipIfBurning.visibleWhen(() -> this.waterMode.is("Solid"));
      this.dipOnSneakWater.visibleWhen(() -> this.waterMode.is("Solid"));
      this.dipOnFallWater.visibleWhen(() -> this.waterMode.is("Solid"));
      this.dipFallHeightWater.visibleWhen(() -> this.waterMode.is("Solid") && this.dipOnFallWater.get());
      this.dipIfFireResistant.visibleWhen(() -> this.lavaMode.is("Solid"));
      this.dipOnSneakLava.visibleWhen(() -> this.lavaMode.is("Solid"));
      this.dipOnFallLava.visibleWhen(() -> this.lavaMode.is("Solid"));
      this.dipFallHeightLava.visibleWhen(() -> this.lavaMode.is("Solid") && this.dipOnFallLava.get());
   }

   @Override
   protected void onEnable() {
      this.swimmingTicks = 0;
      this.inBubbleColumn = false;
      this.moveIsVelocity = false;
      active = this;
      this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
      this.on(PlayerMoveEvent.TYPE, Stage.PRE, EventPriority.HIGHEST, this::captureMoveAliasing);
      this.on(PlayerMoveEvent.TYPE, this::onMove);
      this.on(PacketSendEvent.TYPE, this::onPacketSend);
   }

   @Override
   protected void onDisable() {
      active = null;
      this.inBubbleColumn = false;
      this.moveIsVelocity = false;
      this.swimmingTicks = 0;
   }

   private void onPostTick(TickEvent tickEvent) {
      boolean var3 = this.inBubbleColumn;
      this.inBubbleColumn = false;
      MinecraftClient var4 = MinecraftClient.getInstance();
      ClientPlayerEntity var5 = var4.player;
      if (var5 != null && var4.world != null) {
         if (!var5.isInSwimmingPose()) {
            if (!var5.isTouchingWater() || this.waterShouldBeSolid(var5)) {
               if (!var5.isInLava() || this.lavaShouldBeSolid(var5)) {
                  Object var2 = var5.hasVehicle() ? var5.getVehicle() : var5;
                  if (var2 != null) {
                     if (var3) {
                        if (var4.options.jumpKey.isPressed() && var2.getVelocity().y < 0.11) {
                           setVelocityY((Entity)var2, 0.11);
                        }
                     } else if (!var2.isTouchingWater() && !var2.isInLava()) {
                        BlockState var6 = var4.world.getBlockState(var2.getBlockPos().down());
                        boolean var7 = var6.get(Properties.WATERLOGGED, false);
                        if (this.ascending == 0) {
                           setVelocityY((Entity)var2, 0.11);
                        } else if (this.ascending == 1 && (var6.isOf(Blocks.WATER) || var6.isOf(Blocks.LAVA) || var7)) {
                           setVelocityY((Entity)var2, 0.0);
                        }

                        this.ascending++;
                     } else {
                        setVelocityY((Entity)var2, 0.11);
                        this.ascending = 0;
                     }
                  }
               }
            }
         }
      }
   }

   private static void setVelocityY(Entity entity, double d) {
      Vec3d var3 = entity.getVelocity();
      entity.setVelocity(var3.x, d, var3.z);
   }

   private void captureMoveAliasing(PlayerMoveEvent playerMoveEvent) {
      this.moveIsVelocity = false;
      if (this.ncpBypass.get()) {
         ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
         if (var2 != null) {
            Vec3d var3 = var2.getVelocity();
            this.moveIsVelocity = var3.x == playerMoveEvent.x && var3.y == playerMoveEvent.y && var3.z == playerMoveEvent.z;
         }
      }
   }

   private void onMove(PlayerMoveEvent playerMoveEvent) {
      if (this.ncpBypass.get()) {
         boolean var3 = this.moveIsVelocity;
         this.moveIsVelocity = false;
         MinecraftClient var4 = MinecraftClient.getInstance();
         ClientPlayerEntity var5 = var4.player;
         if (var5 != null && var4.world != null) {
            int var6 = overLiquid(var5);
            boolean var7 = (var6 & 1) != 0 && this.waterShouldBeSolid(var5);
            boolean var2 = (var6 & 2) != 0 && this.lavaShouldBeSolid(var5);
            if (!var7 && !var2) {
               this.swimmingTicks = 0;
            } else if (++this.swimmingTicks < 15) {
               if (var5.isOnGround()) {
                  applyStrafe(var4, var5, playerMoveEvent, isMoving(var5) ? 0.2873 : 0.0);
                  if (var3) {
                     setVelocityXZ(var5, playerMoveEvent.x, playerMoveEvent.z);
                  }
               }
            } else {
               this.swimmingTicks = 0;
               if (this.slowDown.get()) {
                  playerMoveEvent.x = 0.0;
                  playerMoveEvent.z = 0.0;
                  if (var3) {
                     setVelocityXZ(var5, 0.0, 0.0);
                  }
               }

               setVelocityY(var5, 0.08);
               if (var3) {
                  playerMoveEvent.y = 0.08;
               }
            }
         }
      }
   }

   private static void setVelocityXZ(Entity entity, double d, double e) {
      entity.setVelocity(d, entity.getVelocity().y, e);
   }

   private void onPacketSend(PacketSendEvent packetSendEvent) {
      if (packetSendEvent.packet instanceof PlayerMoveC2SPacket var4) {
         if (this.ncpBypass.get()) {
            MinecraftClient var5 = MinecraftClient.getInstance();
            if (var5.isOnThread()) {
               ClientPlayerEntity var6 = var5.player;
               if (var6 != null && var5.world != null) {
                  if (!var6.isTouchingWater() || this.waterShouldBeSolid(var6)) {
                     if (!var6.isInLava() || this.lavaShouldBeSolid(var6)) {
                        int var7 = overLiquid(var6);
                        boolean var2 = (var7 & 1) != 0 && this.waterShouldBeSolid(var6) || (var7 & 2) != 0 && this.lavaShouldBeSolid(var6);
                        if (!var6.isTouchingWater() && !var6.isInLava() && !(var6.fallDistance > 3.0) && var2) {
                           ((AccessorJesusServerboundMovePlayerPacket)var4).efn$jesusSetOnGround(false);
                           if (var6.isOnGround() && var4.changesPosition()) {
                              ((AccessorServerboundMovePlayerPacket)var4).efn$setY(var4.getY(0.0) - (0.02 + 1.0E-4 * this.swimmingTicks));
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isMoving(ClientPlayerEntity clientPlayerEntity) {
      return clientPlayerEntity.forwardSpeed != 0.0F || clientPlayerEntity.sidewaysSpeed != 0.0F;
   }

   private static void applyStrafe(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity, PlayerMoveEvent playerMoveEvent, double d) {
      Vec2f var5 = clientPlayerEntity.input.getMovementInput();
      float var6 = Math.signum(var5.y);
      float var7 = Math.signum(var5.x);
      float var8 = clientPlayerEntity.getLerpedYaw(minecraftClient.getRenderTickCounter().getTickProgress(true));
      if (var6 == 0.0F && var7 == 0.0F) {
         playerMoveEvent.x = 0.0;
         playerMoveEvent.z = 0.0;
      } else {
         float var9 = 90.0F * var7;
         if (var6 != 0.0F) {
            var9 *= var6 * 0.5F;
         }

         var8 -= var9;
         if (var6 < 0.0F) {
            var8 -= 180.0F;
         }

         double var10 = Math.toRadians(var8);
         playerMoveEvent.x = -Math.sin(var10) * d;
         playerMoveEvent.z = Math.cos(var10) * d;
      }
   }

   private static int overLiquid(ClientPlayerEntity clientPlayerEntity) {
      Entity var1 = clientPlayerEntity.getVehicle();
      Box var2 = var1 != null ? clientPlayerEntity.getBoundingBox().union(var1.getBoundingBox()) : clientPlayerEntity.getBoundingBox();
      boolean var3 = false;
      boolean var4 = false;
      Iterator var5 = clientPlayerEntity.getEntityWorld().getStatesInBox(var2.offset(0.0, -0.01, 0.0)).iterator();

      while (var5.hasNext()) {
         BlockState var6 = (BlockState)var5.next();
         if (var6.isOf(Blocks.WATER) || var6.getFluidState().getFluid() == Fluids.WATER) {
            var3 = true;
         } else if (var6.isOf(Blocks.LAVA)) {
            var4 = true;
         } else if (!var6.isAir()) {
            return 0;
         }
      }

      return (var3 ? 1 : 0) | (var4 ? 2 : 0);
   }

   private boolean waterShouldBeSolid(ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.isSpectator() || clientPlayerEntity.getAbilities().flying) {
         return false;
      } else if (clientPlayerEntity.getVehicle() instanceof AbstractBoatEntity) {
         return false;
      } else if (FlyModule.active != null) {
         return false;
      } else if (this.dipIfBurning.get() && clientPlayerEntity.isOnFire()) {
         return false;
      } else if (this.dipOnSneakWater.get() && MinecraftClient.getInstance().options.sneakKey.isPressed()) {
         return false;
      } else {
         return this.dipOnFallWater.get() && clientPlayerEntity.fallDistance > this.dipFallHeightWater.getInt() ? false : this.waterMode.is("Solid");
      }
   }

   private boolean lavaShouldBeSolid(ClientPlayerEntity clientPlayerEntity) {
      if (clientPlayerEntity.isSpectator() || clientPlayerEntity.getAbilities().flying) {
         return false;
      } else if (clientPlayerEntity.getVehicle() instanceof StriderEntity) {
         return false;
      } else if (this.isLavaDangerous(clientPlayerEntity) && this.lavaMode.is("Solid")) {
         return true;
      } else if (this.dipOnSneakLava.get() && MinecraftClient.getInstance().options.sneakKey.isPressed()) {
         return false;
      } else {
         return this.dipOnFallLava.get() && clientPlayerEntity.fallDistance > this.dipFallHeightLava.getInt() ? false : this.lavaMode.is("Solid");
      }
   }

   private boolean isLavaDangerous(ClientPlayerEntity clientPlayerEntity) {
      if (!this.dipIfFireResistant.get()) {
         return true;
      } else {
         StatusEffectInstance var2 = clientPlayerEntity.getStatusEffect(StatusEffects.FIRE_RESISTANCE);
         return var2 == null ? true : !(var2.getDuration() > 300.0 * clientPlayerEntity.getAttributeValue(EntityAttributes.BURNING_TIME));
      }
   }

   public boolean canWalkOnFluid(FluidState fluidState) {
      ClientPlayerEntity var2 = MinecraftClient.getInstance().player;
      if (var2 != null && !var2.isSwimming()) {
         Fluid var3 = fluidState.getFluid();
         return (var3 == Fluids.WATER || var3 == Fluids.FLOWING_WATER) && this.waterShouldBeSolid(var2)
            ? true
            : (var3 == Fluids.LAVA || var3 == Fluids.FLOWING_LAVA) && this.lavaShouldBeSolid(var2);
      } else {
         return false;
      }
   }

   public VoxelShape collisionShape(CollisionView collisionView, BlockState blockState, BlockPos blockPos, VoxelShape voxelShape) {
      FluidState var5 = blockState.getFluidState();
      if (var5.isEmpty()) {
         return voxelShape;
      } else {
         MinecraftClient var6 = MinecraftClient.getInstance();
         if (collisionView == var6.world && var6.isOnThread()) {
            ClientPlayerEntity var7 = var6.player;
            if (var7 == null) {
               return voxelShape;
            } else if ((blockState.isOf(Blocks.WATER) || var5.getFluid() == Fluids.WATER)
               && !var7.isTouchingWater()
               && this.waterShouldBeSolid(var7)
               && blockPos.getY() <= var7.getY() - 1.0) {
               return VoxelShapes.fullCube();
            } else {
               return !blockState.isOf(Blocks.LAVA)
                     || var7.isInLava()
                     || !this.lavaShouldBeSolid(var7)
                     || !this.isLavaDangerous(var7) && !(blockPos.getY() <= var7.getY() - 1.0)
                  ? voxelShape
                  : VoxelShapes.fullCube();
            }
         } else {
            return voxelShape;
         }
      }
   }

   public boolean canWalkOnPowderSnow() {
      return this.powderSnow.get();
   }

   public void markInBubbleColumn() {
      this.inBubbleColumn = true;
   }
}
