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

   private void onPostTick(TickEvent var1) {
      boolean var3 = this.inBubbleColumn;
      this.inBubbleColumn = false;
      MinecraftClient var4 = MinecraftClient.method_1551();
      ClientPlayerEntity var5 = var4.field_1724;
      if (var5 != null && var4.field_1687 != null) {
         if (!var5.method_20232()) {
            if (!var5.method_5799() || this.waterShouldBeSolid(var5)) {
               if (!var5.method_5771() || this.lavaShouldBeSolid(var5)) {
                  Object var2 = var5.method_5765() ? var5.method_5854() : var5;
                  if (var2 != null) {
                     if (var3) {
                        if (var4.field_1690.field_1903.method_1434() && var2.method_18798().field_1351 < 0.11) {
                           setVelocityY((Entity)var2, 0.11);
                        }
                     } else if (!var2.method_5799() && !var2.method_5771()) {
                        BlockState var6 = var4.field_1687.method_8320(var2.method_24515().method_10074());
                        boolean var7 = (Boolean)var6.method_61767(Properties.field_12508, false);
                        if (this.ascending == 0) {
                           setVelocityY((Entity)var2, 0.11);
                        } else if (this.ascending == 1 && (var6.method_27852(Blocks.field_10382) || var6.method_27852(Blocks.field_10164) || var7)) {
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

   private static void setVelocityY(Entity var0, double var1) {
      Vec3d var3 = var0.method_18798();
      var0.method_18800(var3.field_1352, var1, var3.field_1350);
   }

   private void captureMoveAliasing(PlayerMoveEvent var1) {
      this.moveIsVelocity = false;
      if (this.ncpBypass.get()) {
         ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
         if (var2 != null) {
            Vec3d var3 = var2.method_18798();
            this.moveIsVelocity = var3.field_1352 == var1.x && var3.field_1351 == var1.y && var3.field_1350 == var1.z;
         }
      }
   }

   private void onMove(PlayerMoveEvent var1) {
      if (this.ncpBypass.get()) {
         boolean var3 = this.moveIsVelocity;
         this.moveIsVelocity = false;
         MinecraftClient var4 = MinecraftClient.method_1551();
         ClientPlayerEntity var5 = var4.field_1724;
         if (var5 != null && var4.field_1687 != null) {
            int var6 = overLiquid(var5);
            boolean var7 = (var6 & 1) != 0 && this.waterShouldBeSolid(var5);
            boolean var2 = (var6 & 2) != 0 && this.lavaShouldBeSolid(var5);
            if (!var7 && !var2) {
               this.swimmingTicks = 0;
            } else if (++this.swimmingTicks < 15) {
               if (var5.method_24828()) {
                  applyStrafe(var4, var5, var1, isMoving(var5) ? 0.2873 : 0.0);
                  if (var3) {
                     setVelocityXZ(var5, var1.x, var1.z);
                  }
               }
            } else {
               this.swimmingTicks = 0;
               if (this.slowDown.get()) {
                  var1.x = 0.0;
                  var1.z = 0.0;
                  if (var3) {
                     setVelocityXZ(var5, 0.0, 0.0);
                  }
               }

               setVelocityY(var5, 0.08);
               if (var3) {
                  var1.y = 0.08;
               }
            }
         }
      }
   }

   private static void setVelocityXZ(Entity var0, double var1, double var3) {
      var0.method_18800(var1, var0.method_18798().field_1351, var3);
   }

   private void onPacketSend(PacketSendEvent var1) {
      if (var1.packet instanceof PlayerMoveC2SPacket var4) {
         if (this.ncpBypass.get()) {
            MinecraftClient var5 = MinecraftClient.method_1551();
            if (var5.method_18854()) {
               ClientPlayerEntity var6 = var5.field_1724;
               if (var6 != null && var5.field_1687 != null) {
                  if (!var6.method_5799() || this.waterShouldBeSolid(var6)) {
                     if (!var6.method_5771() || this.lavaShouldBeSolid(var6)) {
                        int var7 = overLiquid(var6);
                        boolean var2 = (var7 & 1) != 0 && this.waterShouldBeSolid(var6) || (var7 & 2) != 0 && this.lavaShouldBeSolid(var6);
                        if (!var6.method_5799() && !var6.method_5771() && !(var6.field_6017 > 3.0) && var2) {
                           ((AccessorJesusServerboundMovePlayerPacket)var4).efn$jesusSetOnGround(false);
                           if (var6.method_24828() && var4.method_36171()) {
                              ((AccessorServerboundMovePlayerPacket)var4).efn$setY(var4.method_12268(0.0) - (0.02 + 1.0E-4 * this.swimmingTicks));
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isMoving(ClientPlayerEntity var0) {
      return var0.field_6250 != 0.0F || var0.field_6212 != 0.0F;
   }

   private static void applyStrafe(MinecraftClient var0, ClientPlayerEntity var1, PlayerMoveEvent var2, double var3) {
      Vec2f var5 = var1.field_3913.method_3128();
      float var6 = Math.signum(var5.field_1342);
      float var7 = Math.signum(var5.field_1343);
      float var8 = var1.method_61415(var0.method_61966().method_60637(true));
      if (var6 == 0.0F && var7 == 0.0F) {
         var2.x = 0.0;
         var2.z = 0.0;
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
         var2.x = -Math.sin(var10) * var3;
         var2.z = Math.cos(var10) * var3;
      }
   }

   private static int overLiquid(ClientPlayerEntity var0) {
      Entity var1 = var0.method_5854();
      Box var2 = var1 != null ? var0.method_5829().method_991(var1.method_5829()) : var0.method_5829();
      boolean var3 = false;
      boolean var4 = false;
      Iterator var5 = var0.method_73183().method_29546(var2.method_989(0.0, -0.01, 0.0)).iterator();

      while (var5.hasNext()) {
         BlockState var6 = (BlockState)var5.next();
         if (var6.method_27852(Blocks.field_10382) || var6.method_26227().method_15772() == Fluids.field_15910) {
            var3 = true;
         } else if (var6.method_27852(Blocks.field_10164)) {
            var4 = true;
         } else if (!var6.method_26215()) {
            return 0;
         }
      }

      return (var3 ? 1 : 0) | (var4 ? 2 : 0);
   }

   private boolean waterShouldBeSolid(ClientPlayerEntity var1) {
      if (var1.method_7325() || var1.method_31549().field_7479) {
         return false;
      } else if (var1.method_5854() instanceof AbstractBoatEntity) {
         return false;
      } else if (FlyModule.active != null) {
         return false;
      } else if (this.dipIfBurning.get() && var1.method_5809()) {
         return false;
      } else if (this.dipOnSneakWater.get() && MinecraftClient.method_1551().field_1690.field_1832.method_1434()) {
         return false;
      } else {
         return this.dipOnFallWater.get() && var1.field_6017 > this.dipFallHeightWater.getInt() ? false : this.waterMode.is("Solid");
      }
   }

   private boolean lavaShouldBeSolid(ClientPlayerEntity var1) {
      if (var1.method_7325() || var1.method_31549().field_7479) {
         return false;
      } else if (var1.method_5854() instanceof StriderEntity) {
         return false;
      } else if (this.isLavaDangerous(var1) && this.lavaMode.is("Solid")) {
         return true;
      } else if (this.dipOnSneakLava.get() && MinecraftClient.method_1551().field_1690.field_1832.method_1434()) {
         return false;
      } else {
         return this.dipOnFallLava.get() && var1.field_6017 > this.dipFallHeightLava.getInt() ? false : this.lavaMode.is("Solid");
      }
   }

   private boolean isLavaDangerous(ClientPlayerEntity var1) {
      if (!this.dipIfFireResistant.get()) {
         return true;
      } else {
         StatusEffectInstance var2 = var1.method_6112(StatusEffects.field_5918);
         return var2 == null ? true : !(var2.method_5584() > 300.0 * var1.method_45325(EntityAttributes.field_51579));
      }
   }

   public boolean canWalkOnFluid(FluidState var1) {
      ClientPlayerEntity var2 = MinecraftClient.method_1551().field_1724;
      if (var2 != null && !var2.method_5681()) {
         Fluid var3 = var1.method_15772();
         return (var3 == Fluids.field_15910 || var3 == Fluids.field_15909) && this.waterShouldBeSolid(var2)
            ? true
            : (var3 == Fluids.field_15908 || var3 == Fluids.field_15907) && this.lavaShouldBeSolid(var2);
      } else {
         return false;
      }
   }

   public VoxelShape collisionShape(CollisionView var1, BlockState var2, BlockPos var3, VoxelShape var4) {
      FluidState var5 = var2.method_26227();
      if (var5.method_15769()) {
         return var4;
      } else {
         MinecraftClient var6 = MinecraftClient.method_1551();
         if (var1 == var6.field_1687 && var6.method_18854()) {
            ClientPlayerEntity var7 = var6.field_1724;
            if (var7 == null) {
               return var4;
            } else if ((var2.method_27852(Blocks.field_10382) || var5.method_15772() == Fluids.field_15910)
               && !var7.method_5799()
               && this.waterShouldBeSolid(var7)
               && var3.method_10264() <= var7.method_23318() - 1.0) {
               return VoxelShapes.method_1077();
            } else {
               return !var2.method_27852(Blocks.field_10164)
                     || var7.method_5771()
                     || !this.lavaShouldBeSolid(var7)
                     || !this.isLavaDangerous(var7) && !(var3.method_10264() <= var7.method_23318() - 1.0)
                  ? var4
                  : VoxelShapes.method_1077();
            }
         } else {
            return var4;
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
