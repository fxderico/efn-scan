package net.efn.client.module.impl.combat;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Combo;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class AutoCityModule extends ClientModule {
   private static final float CRYSTAL_POWER = 6.0F;
   private static final double FACE_INSET = 0.05;
   private static final int GIVE_UP_TICKS = 200;
   private static volatile AutoCityModule active;
   private final SlotReturn slots = new SlotReturn(this);
   private LivingEntity target;
   private BlockPos feet;
   private BlockPos block;
   private int ticks;
   private boolean broken;

   public AutoCityModule() {
      super(
         "auto-city",
         "Auto City",
         "Mines a surround block of the best enemy in a hole, then turns off so Crystal Aura can blow through it.",
         ModuleCategory.COMBAT
      );
      this.usesNotifications();
   }

   @Override
   public boolean persistEnabled() {
      return false;
   }

   @Override
   protected void onEnable() {
      this.target = null;
      this.feet = null;
      this.block = null;
      this.ticks = 0;
      this.broken = false;
      active = this;
   }

   @Override
   protected void onDisable() {
      active = null;
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (!this.broken && this.block != null && var1.interactionManager != null && var1.interactionManager.isBreakingBlock()) {
         var1.interactionManager.cancelBlockBreaking();
      }

      this.slots.drop(var1, true);
      CombatMacroLock.release(this);
      this.target = null;
      this.block = null;
   }

   public static boolean mining() {
      AutoCityModule var0 = active;
      return var0 != null && var0.block != null && !var0.broken;
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 == null || var2.world == null || var2.interactionManager == null || var3.isDead() || this.broken || userClicks(var2)) {
         this.requestDisable();
      } else if (this.block == null && !this.pick(var2, var3)) {
         this.stop("No enemy in a hole within reach");
      } else {
         ClientWorld var4 = var2.world;
         BlockState var5 = var4.getBlockState(this.block);
         if (var5.isReplaceable()) {
            this.opened();
         } else {
            this.ticks++;
            String var6 = this.problem(var3, var5);
            if (var6 != null) {
               this.stop(var6);
            } else if (MeleeAttacks.canAct(var2) && !AutoInvTotemModule.wantsHotbar() && !var3.isUsingItem() && !var3.isRiding()) {
               if (CombatMacroLock.tryAcquire(this) && this.slots.take(var3, toolSlot(var3.getInventory(), var5))) {
                  BlockHitResult var7 = aim(var4, var3, this.block);
                  if (var7 == null) {
                     this.stop("Lost sight of the block");
                  } else {
                     ServerRotation.request(RotationUtils.fromTo(var3.getEyePos(), var7.getPos()));
                     BlockHitResult var8 = held(var4, var3, this.block);
                     if (var8 != null && !MeleeAttacks.actedThisTick(var3)) {
                        if (var2.interactionManager.updateBlockBreakingProgress(this.block, var8.getSide())) {
                           var3.swingHand(Hand.MAIN_HAND);
                        }

                        MeleeAttacks.noteAction(var3);
                        if (var4.getBlockState(this.block).isReplaceable()) {
                           this.opened();
                           this.note(ModuleChat.body().text("Opened ").name(this.target.getName().getString()).text("'s surround"));
                        }
                     }
                  }
               }
            } else {
               if (var2.interactionManager.isBreakingBlock()) {
                  var2.interactionManager.cancelBlockBreaking();
               }

               CombatMacroLock.release(this);
            }
         }
      }
   }

   private void opened() {
      this.broken = true;
      CombatMacroLock.release(this);
   }

   private void stop(String string) {
      this.note(ModuleChat.body().text(string));
      this.requestDisable();
   }

   private static boolean userClicks(MinecraftClient minecraftClient) {
      return minecraftClient.currentScreen == null
         && (
            Crystals.isPhysicallyDown(minecraftClient, minecraftClient.options.attackKey)
               || MeleeAttacks.clickPending(minecraftClient)
               || Crystals.isPhysicallyDown(minecraftClient, minecraftClient.options.useKey)
         );
   }

   private boolean pick(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      ClientWorld var3 = minecraftClient.world;
      float var4 = 0.0F;

      for (LivingEntity var6 : Combo.enemies(minecraftClient, clientPlayerEntity)) {
         if (Combo.inHole(var6)) {
            BlockPos var7 = var6.getBlockPos();

            for (Direction var9 : Type.HORIZONTAL) {
               BlockPos var13 = var7.offset(var9);
               BlockState var14 = var3.getBlockState(var13);
               float var10;
               Vec3d var11;
               float var12;
               if (!(var14.getHardness(var3, var13) < 0.0F)
                  && Crystals.isBase(var3.getBlockState(var13.down()))
                  && clientPlayerEntity.canInteractWithBlockAt(var13, 0.0)
                  && toolSlot(clientPlayerEntity.getInventory(), var14) != -1
                  && aim(var3, clientPlayerEntity, var13) != null
                  && !CrystalDamage.endangers(
                     clientPlayerEntity, var12 = CrystalDamage.estimate(var11 = Crystals.explosionAt(var13.down()), clientPlayerEntity, 6.0F, var13)
                  )
                  && !((var10 = CrystalDamage.estimate(var11, var6, 6.0F, var13) - 0.5F * var12) <= var4)) {
                  var4 = var10;
                  this.target = var6;
                  this.feet = var7;
                  this.block = var13;
               }
            }
         }
      }

      return this.block != null;
   }

   private String problem(ClientPlayerEntity clientPlayerEntity, BlockState blockState) {
      BlockPos var3 = this.target.getBlockPos();
      if (!this.target.isAlive() || var3.getX() != this.feet.getX() || var3.getZ() != this.feet.getZ() || Math.abs(var3.getY() - this.feet.getY()) > 1) {
         return "Target left the hole";
      } else if (!clientPlayerEntity.canInteractWithBlockAt(this.block, 0.0)) {
         return "Block went out of reach";
      } else if (toolSlot(clientPlayerEntity.getInventory(), blockState) == -1) {
         return "No pickaxe that can break it";
      } else if (!ServerRotation.canRotate(clientPlayerEntity)) {
         return "Can't aim while flying, swimming or riding";
      } else {
         return this.ticks > 200 ? "Gave up, the block took too long" : null;
      }
   }

   private void note(ModuleChat.Body body) {
      if (this.notificationsOn()) {
         ModuleChat.actionBar(this, body);
      }
   }

   private static int toolSlot(PlayerInventory playerInventory, BlockState blockState) {
      int var2 = -1;
      float var3 = 0.0F;

      for (int var4 = 0; var4 < PlayerInventory.getHotbarSize(); var4++) {
         ItemStack var5 = playerInventory.getStack(var4);
         if (var5.isSuitableFor(blockState) && !(var5.getMiningSpeedMultiplier(blockState) <= var3)) {
            var2 = var4;
            var3 = var5.getMiningSpeedMultiplier(blockState);
         }
      }

      return var2;
   }

   private static BlockHitResult aim(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      Vec3d var3 = clientPlayerEntity.getEyePos();
      double var4 = clientPlayerEntity.getBlockInteractionRange();
      BlockHitResult var6 = null;
      double var7 = var4 * var4;

      for (Direction var12 : Direction.values()) {
         Vec3d var14 = Vec3d.ofCenter(blockPos).offset(var12, 0.5);
         double var15 = var3.squaredDistanceTo(var14);
         BlockHitResult var13;
         if (!(var15 > var7)
            && (var13 = clientWorld.raycast(new RaycastContext(var3, var14.offset(var12, -0.05), ShapeType.OUTLINE, FluidHandling.NONE, clientPlayerEntity)))
                  .getType()
               == net.minecraft.util.hit.HitResult.Type.BLOCK
            && var13.getBlockPos().equals(blockPos)
            && var13.getSide() == var12) {
            var6 = var13;
            var7 = var15;
         }
      }

      return var6;
   }

   private static BlockHitResult held(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      Rotation var4 = ServerRotation.sent();
      if (var4 == null) {
         return null;
      } else {
         Vec3d var5 = clientPlayerEntity.getEyePos();
         BlockHitResult var6 = clientWorld.raycast(
            new RaycastContext(
               var5,
               var5.add(Vec3d.fromPolar(var4.pitch(), var4.yaw()).multiply(clientPlayerEntity.getBlockInteractionRange())),
               ShapeType.OUTLINE,
               FluidHandling.NONE,
               clientPlayerEntity
            )
         );
         return var6.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK && var6.getBlockPos().equals(blockPos) ? var6 : null;
      }
   }
}
