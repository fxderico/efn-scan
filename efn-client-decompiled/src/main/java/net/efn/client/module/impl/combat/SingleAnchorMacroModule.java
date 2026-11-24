package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.anchor.AnchorGuard;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.crystal.LootGuard;
import net.efn.client.module.impl.combat.crystal.LootLatch;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.misc.AutoEatModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class SingleAnchorMacroModule extends ClientModule {
   private static final String SINGLE = "Single";
   private static final String DOUBLE = "Double";
   private static final String DETONATE = "Detonate";
   private static final String CANCEL = "Cancel";
   private static final int MAX_STEPS_PER_TICK = 4;
   private static final int GUARD_TRIES = 2;
   private static final int SETTLE_TICKS = 5;
   private static final float BLAST_PROOF = 600.0F;
   private final ModeSetting mode = this.addSetting("mode", new ModeSetting("Mode", "Single", "Single", "Double"));
   private final NumberSetting delay = this.addSetting("delay", new NumberSetting("Delay", 0.0F, 0.0F, 5.0F).step(1.0F));
   private final BooleanSetting blastGuard = this.addSetting("safe-anchor", new BooleanSetting("Safe Anchor", true));
   private final ModeSetting withoutGuard = this.addSetting("without-guard", new ModeSetting("If Unguarded", "Detonate", "Detonate", "Cancel"));
   private final BooleanSetting switchBack = this.addSetting("switch-back", new BooleanSetting("Switch Back", false));
   private final BooleanSetting protectLoot = this.addSetting("protect-loot", new BooleanSetting("Protect Loot", true));
   private SingleAnchorMacroModule.Step step = SingleAnchorMacroModule.Step.IDLE;
   private int tick;
   private int readyAt;
   private int deadline;
   private int previousSlot = -1;
   private int ownSlot = -1;
   private boolean refilled;
   private BlockPos anchorPos;
   private Vec3d sightline;
   private BlockHitResult replaceHit;
   private ClientWorld activeLevel;
   private AnchorGuard.Spot guard;
   private Block guardBlock;
   private BlockState guardReported;
   private int guardTries;

   public SingleAnchorMacroModule() {
      super("anchor-macro", "Anchor Macro", "Places, charges and blows up respawn anchors on right-click.", ModuleCategory.COMBAT);
      this.withoutGuard.visibleWhen(this.blastGuard::get);
   }

   @Override
   protected void onEnable() {
      this.stop();
   }

   @Override
   protected void onDisable() {
      MinecraftClient var2 = MinecraftClient.getInstance();
      PlayerInventory var1;
      if (this.step != SingleAnchorMacroModule.Step.IDLE
         && var2.player != null
         && !(var2.currentScreen instanceof HandledScreen)
         && this.switchBackDue(var1 = var2.player.getInventory())) {
         var1.setSelectedSlot(this.previousSlot);
      }

      this.stop();
   }

   @EventHandler
   public void onTick(TickEvent tickEvent) {
      this.tick++;
      MinecraftClient var2 = MinecraftClient.getInstance();
      ClientPlayerEntity var3 = var2.player;
      if (var3 != null && var2.world != null && var2.interactionManager != null && !var3.isDead()) {
         LootLatch.update(var2, var3);
         LootGuard.update(var2.world, var3);
         PlayerInventory var4 = var3.getInventory();
         if (var2.currentScreen != null) {
            if (this.step != SingleAnchorMacroModule.Step.IDLE && this.step != SingleAnchorMacroModule.Step.SWITCH_BACK) {
               this.wrapUp(var4);
            }
         } else if (!AutoEatModule.isEating() && !AutoInvTotemModule.wantsHotbar()) {
            if (this.step != SingleAnchorMacroModule.Step.IDLE && (this.hotbarMovedByPlayer(var4) || var2.world != this.activeLevel)) {
               this.stop();
            }

            if ((LootLatch.latched() || LootLatch.killedNow())
               && this.step != SingleAnchorMacroModule.Step.IDLE
               && this.step != SingleAnchorMacroModule.Step.SWITCH_BACK) {
               this.wrapUp(var4);
            }

            if (LootLatch.latched() && this.step == SingleAnchorMacroModule.Step.IDLE) {
               if (!var3.isUsingItem() && anchorInPlay(var2, var3)) {
                  suppressUse(var2);
               }
            } else if (this.step != SingleAnchorMacroModule.Step.IDLE || this.tryStart(var2, var3)) {
               swallowClicks(var2);
               if (this.step == SingleAnchorMacroModule.Step.SWITCH_BACK) {
                  if (!MeleeAttacks.actedThisTick(var3)) {
                     if (this.switchBackDue(var4)) {
                        var4.setSelectedSlot(this.previousSlot);
                     }

                     this.stop();
                  }
               } else {
                  for (int var5 = 0;
                     var5 < 4
                        && this.tick >= this.readyAt
                        && this.step != SingleAnchorMacroModule.Step.IDLE
                        && this.step != SingleAnchorMacroModule.Step.SWITCH_BACK;
                     var5++
                  ) {
                     if (this.tick >= this.deadline) {
                        this.expire(var4);
                     } else {
                        if (MeleeAttacks.actedThisTick(var3) || !MeleeAttacks.handsFree(var2, var3)) {
                           break;
                        }

                        this.runStep(var2, var3);
                     }
                  }
               }
            }
         } else {
            this.stop();
         }
      } else {
         this.stop();
      }
   }

   @EventHandler
   public void onPacketApplied(PacketAppliedEvent packetAppliedEvent) {
      if (this.step == SingleAnchorMacroModule.Step.AWAIT_GUARD && this.guard != null) {
         BlockPos var2 = this.guard.pos();
         if (packetAppliedEvent.packet instanceof BlockUpdateS2CPacket var4) {
            if (var4.getPos().equals(var2)) {
               this.guardReported = var4.getState();
            }
         } else if (packetAppliedEvent.packet instanceof ChunkDeltaUpdateS2CPacket var6) {
            var6.visitUpdates((blockPos2, blockState) -> {
               if (blockPos2.equals(var2)) {
                  this.guardReported = blockState;
               }
            });
         }
      }
   }

   private boolean tryStart(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      if (MeleeAttacks.rightClickComing(minecraftClient) && !CombatMacroLock.heldByOther(this)) {
         if (!clientPlayerEntity.isSpectator()
            && canClick(clientPlayerEntity)
            && !MeleeAttacks.actedThisTick(clientPlayerEntity)
            && MeleeAttacks.handsFree(minecraftClient, clientPlayerEntity)
            && !clientPlayerEntity.getOffHandStack().isOf(Items.GLOWSTONE)) {
            HitResult var7 = clientPlayerEntity.raycast(clientPlayerEntity.getBlockInteractionRange(), 1.0F, false);
            BlockHitResult var6;
            if (var7 instanceof BlockHitResult && (var6 = (BlockHitResult)var7).getType() == Type.BLOCK) {
               ClientWorld var8 = minecraftClient.world;
               BlockState var9 = var8.getBlockState(var6.getBlockPos());
               boolean var10 = var9.isOf(Blocks.RESPAWN_ANCHOR);
               BlockPos var5;
               if (var10) {
                  var5 = var6.getBlockPos().toImmutable();
               } else {
                  if (!clientPlayerEntity.getMainHandStack().isOf(Items.RESPAWN_ANCHOR) || ServerRotation.pending()) {
                     return false;
                  }

                  var5 = Crystals.placedObsidian(var8, clientPlayerEntity, var6, var9);
               }

               if (var5 == null || var8.getRegistryKey() == World.NETHER) {
                  return false;
               } else if (this.protectLoot.get() && LootGuard.near(Vec3d.ofCenter(var5))) {
                  return false;
               } else {
                  PlayerInventory var11 = clientPlayerEntity.getInventory();
                  boolean var4 = chargeOf(var9) > 0;
                  if (!var4 && findHotbar(var11, Items.GLOWSTONE) == -1) {
                     return false;
                  } else if (AnchorGuard.detonatorSlot(var11) == -1) {
                     return false;
                  } else if (this.blastGuard.get() && this.withoutGuard.is("Cancel") && guardSlot(var11, this.glowstoneNeeded(var4)) == -1) {
                     return false;
                  } else if (!CombatMacroLock.tryAcquire(this)) {
                     return false;
                  } else {
                     this.previousSlot = var11.getSelectedSlot();
                     this.activeLevel = var8;
                     this.anchorPos = var5;
                     this.sightline = var6.getPos();
                     SingleAnchorMacroModule.Step var3 = this.blastGuard.get() ? SingleAnchorMacroModule.Step.GUARD : SingleAnchorMacroModule.Step.CHARGE;
                     if (var10) {
                        this.scheduleNow(var3);
                     } else {
                        AnchorGuard.click(minecraftClient, clientPlayerEntity, var6);
                        this.schedule(var3);
                     }

                     return true;
                  }
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void swallowClicks(MinecraftClient minecraftClient) {
      MeleeAttacks.takeAttackClicks(minecraftClient);
      suppressUse(minecraftClient);
   }

   private static boolean anchorInPlay(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      ItemStack var3 = clientPlayerEntity.getMainHandStack();
      if (!var3.isOf(Items.RESPAWN_ANCHOR) && !var3.isOf(Items.GLOWSTONE)) {
         HitResult var4 = minecraftClient.crosshairTarget;
         BlockHitResult var2;
         return var4 instanceof BlockHitResult
            && (var2 = (BlockHitResult)var4).getType() == Type.BLOCK
            && minecraftClient.world.getBlockState(var2.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR);
      } else {
         return true;
      }
   }

   private static void suppressUse(MinecraftClient minecraftClient) {
      KeyBinding var1 = minecraftClient.options.useKey;
      var1.setPressed(false);

      while (var1.wasPressed()) {
      }
   }

   private void runStep(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      switch (this.step) {
         case GUARD:
            this.placeGuard(minecraftClient, clientPlayerEntity);
            break;
         case AWAIT_GUARD:
            this.awaitGuard(minecraftClient, clientPlayerEntity);
            break;
         case CHARGE:
            this.charge(minecraftClient, clientPlayerEntity);
            break;
         case DETONATE:
            this.detonate(minecraftClient, clientPlayerEntity);
            break;
         case REPLACE:
            this.replace(minecraftClient, clientPlayerEntity);
      }
   }

   private void expire(PlayerInventory playerInventory) {
      if (this.step != SingleAnchorMacroModule.Step.GUARD && this.step != SingleAnchorMacroModule.Step.AWAIT_GUARD) {
         this.wrapUp(playerInventory);
      } else {
         this.skipGuard(playerInventory);
      }
   }

   private void placeGuard(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var4 = clientPlayerEntity.getInventory();
      BlockState var5 = minecraftClient.world.getBlockState(this.anchorPos);
      if (!var5.isOf(Blocks.RESPAWN_ANCHOR)) {
         this.wrapUp(var4);
      } else {
         AnchorGuard.Spot var6 = AnchorGuard.find(minecraftClient.world, clientPlayerEntity, this.anchorPos, this.sightline);
         if (var6 != null && var6.standing()) {
            this.guarded();
         } else {
            int var7 = guardSlot(var4, this.glowstoneNeeded(chargeOf(var5) > 0));
            Rotation var8 = ServerRotation.sent();
            BlockHitResult var3 = var6 != null && var8 != null ? AnchorGuard.hitAlong(minecraftClient.world, clientPlayerEntity, var8, var6) : null;
            if (var6 != null && var7 != -1 && (var3 != null || ServerRotation.canRotate(clientPlayerEntity))) {
               if (var3 == null) {
                  ServerRotation.request(RotationUtils.fromTo(clientPlayerEntity.getEyePos(), var6.aim()));
                  MeleeAttacks.claimTick(minecraftClient);
                  this.readyAt = this.tick + 1;
               } else {
                  this.guard = var6;
                  this.guardBlock = Block.getBlockFromItem(var4.getStack(var7).getItem());
                  this.guardReported = null;
                  this.select(var4, var7);
                  AnchorGuard.click(minecraftClient, clientPlayerEntity, var3);
                  ServerRotation.request(RotationUtils.fromTo(clientPlayerEntity.getEyePos(), Vec3d.ofCenter(this.anchorPos)));
                  this.guardTries++;
                  if (this.withoutGuard.is("Cancel")) {
                     this.schedule(SingleAnchorMacroModule.Step.AWAIT_GUARD);
                     this.deadline = this.readyAt + Crystals.confirmWindow(minecraftClient, clientPlayerEntity);
                  } else {
                     this.schedule(SingleAnchorMacroModule.Step.CHARGE);
                  }
               }
            } else {
               this.skipGuard(var4);
            }
         }
      }
   }

   private void awaitGuard(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      boolean var3 = minecraftClient.world.getBlockState(this.guard.pos()).isOf(this.guardBlock);
      if (var3 && this.guardReported != null && this.guardReported.isOf(this.guardBlock)) {
         this.guarded();
      } else if (var3) {
         this.readyAt = this.tick + 1;
      } else if (this.guardTries < 2) {
         this.scheduleNow(SingleAnchorMacroModule.Step.GUARD);
      } else {
         this.skipGuard(clientPlayerEntity.getInventory());
      }
   }

   private void guarded() {
      this.guard = null;
      this.scheduleNow(SingleAnchorMacroModule.Step.CHARGE);
   }

   private void skipGuard(PlayerInventory playerInventory) {
      this.guard = null;
      if (this.withoutGuard.is("Cancel")) {
         this.wrapUp(playerInventory);
      } else {
         this.scheduleNow(SingleAnchorMacroModule.Step.CHARGE);
      }
   }

   private void charge(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var3 = clientPlayerEntity.getInventory();
      BlockState var4 = minecraftClient.world.getBlockState(this.anchorPos);
      if (!var4.isOf(Blocks.RESPAWN_ANCHOR)) {
         this.wrapUp(var3);
      } else if (chargeOf(var4) > 0 && !this.refilled) {
         this.scheduleNow(SingleAnchorMacroModule.Step.DETONATE);
      } else {
         int var5 = findHotbar(var3, Items.GLOWSTONE);
         if (var5 != -1 && canClick(clientPlayerEntity)) {
            BlockHitResult var6 = AnchorGuard.anchorHit(minecraftClient.world, clientPlayerEntity, this.anchorPos);
            if (var6 == null) {
               this.readyAt = this.tick + 1;
            } else {
               this.select(var3, var5);
               AnchorGuard.click(minecraftClient, clientPlayerEntity, var6);
               this.schedule(SingleAnchorMacroModule.Step.DETONATE);
            }
         } else {
            this.wrapUp(var3);
         }
      }
   }

   private void detonate(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var4 = clientPlayerEntity.getInventory();
      if (chargeOf(minecraftClient.world.getBlockState(this.anchorPos)) != 0
         && canClick(clientPlayerEntity)
         && !clientPlayerEntity.getOffHandStack().isOf(Items.GLOWSTONE)) {
         BlockHitResult var5 = AnchorGuard.anchorHit(minecraftClient.world, clientPlayerEntity, this.anchorPos);
         if (var5 == null) {
            this.readyAt = this.tick + 1;
         } else {
            int var6 = this.mode.is("Double")
                  && !this.refilled
                  && var5.getSide() == Direction.UP
                  && this.anchorPos.getY() >= clientPlayerEntity.getBlockY()
                  && findHotbar(var4, Items.GLOWSTONE) != -1
               ? findHotbar(var4, Items.RESPAWN_ANCHOR)
               : -1;
            int var3 = var6 != -1 ? var6 : AnchorGuard.detonatorSlot(var4);
            if (var3 == -1) {
               this.wrapUp(var4);
            } else if (this.lethal(clientPlayerEntity, var4.getStack(var3))) {
               this.wrapUp(var4);
            } else {
               this.select(var4, var3);
               AnchorGuard.click(minecraftClient, clientPlayerEntity, var5);
               if (var3 == var6) {
                  this.refilled = true;
                  this.replaceHit = var5;
                  this.schedule(SingleAnchorMacroModule.Step.REPLACE);
               } else {
                  this.wrapUp(var4);
               }
            }
         }
      } else {
         this.wrapUp(var4);
      }
   }

   private void replace(MinecraftClient minecraftClient, ClientPlayerEntity clientPlayerEntity) {
      PlayerInventory var4 = clientPlayerEntity.getInventory();
      Rotation var5 = ServerRotation.sent();
      Vec3d var6 = clientPlayerEntity.getEyePos();
      boolean var3 = var5 != null
         && new Box(this.anchorPos)
            .raycast(var6, var6.add(Vec3d.fromPolar(var5.pitch(), var5.yaw()).multiply(clientPlayerEntity.getBlockInteractionRange())))
            .isPresent();
      if (this.replaceHit != null && var3 && clientPlayerEntity.getMainHandStack().isOf(Items.RESPAWN_ANCHOR) && canClick(clientPlayerEntity)) {
         AnchorGuard.click(minecraftClient, clientPlayerEntity, this.replaceHit);
         this.replaceHit = null;
         this.schedule(SingleAnchorMacroModule.Step.CHARGE);
      } else {
         this.wrapUp(var4);
      }
   }

   private boolean lethal(ClientPlayerEntity clientPlayerEntity, ItemStack itemStack) {
      if (!itemStack.isOf(Items.TOTEM_OF_UNDYING) && !clientPlayerEntity.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
         float var3 = CrystalDamage.estimate(Vec3d.ofCenter(this.anchorPos), clientPlayerEntity, 5.0F, this.anchorPos);
         return CrystalDamage.endangers(clientPlayerEntity, var3);
      } else {
         return false;
      }
   }

   private void wrapUp(PlayerInventory playerInventory) {
      if (this.switchBackDue(playerInventory)) {
         this.step = SingleAnchorMacroModule.Step.SWITCH_BACK;
      } else {
         this.stop();
      }
   }

   private boolean switchBackDue(PlayerInventory playerInventory) {
      return this.switchBack.get()
         && PlayerInventory.isValidHotbarIndex(this.previousSlot)
         && this.ownSlot != -1
         && this.ownSlot != this.previousSlot
         && playerInventory.getSelectedSlot() == this.ownSlot;
   }

   private boolean hotbarMovedByPlayer(PlayerInventory playerInventory) {
      return playerInventory.getSelectedSlot() != (this.ownSlot != -1 ? this.ownSlot : this.previousSlot);
   }

   private void stop() {
      this.step = SingleAnchorMacroModule.Step.IDLE;
      this.anchorPos = null;
      this.sightline = null;
      this.replaceHit = null;
      this.activeLevel = null;
      this.previousSlot = -1;
      this.ownSlot = -1;
      this.refilled = false;
      this.guard = null;
      this.guardBlock = null;
      this.guardReported = null;
      this.guardTries = 0;
      CombatMacroLock.release(this);
   }

   private void schedule(SingleAnchorMacroModule.Step step) {
      this.step = step;
      this.readyAt = this.tick + 1 + this.delay.getInt();
      this.deadline = this.readyAt + 5;
   }

   private void scheduleNow(SingleAnchorMacroModule.Step step) {
      this.step = step;
      this.readyAt = this.tick;
      this.deadline = this.tick + 5;
   }

   private void select(PlayerInventory playerInventory, int i) {
      if (playerInventory.getSelectedSlot() != i) {
         playerInventory.setSelectedSlot(i);
         this.ownSlot = i;
      }
   }

   private static boolean canClick(ClientPlayerEntity clientPlayerEntity) {
      return !clientPlayerEntity.shouldCancelInteraction();
   }

   private static int chargeOf(BlockState blockState) {
      return blockState.isOf(Blocks.RESPAWN_ANCHOR) ? blockState.get(RespawnAnchorBlock.CHARGES) : 0;
   }

   private int glowstoneNeeded(boolean bl) {
      int var2 = bl ? 0 : 1;
      return this.mode.is("Double") ? var2 + 1 : var2;
   }

   private static int guardSlot(PlayerInventory playerInventory, int i) {
      int var2 = -1;

      for (int var3 = 0; var3 < 9; var3++) {
         ItemStack var4 = playerInventory.getStack(var3);
         if (!var4.isOf(Items.GLOWSTONE) && AnchorGuard.isGuardBlock(var4)) {
            if (Block.getBlockFromItem(var4.getItem()).getBlastResistance() >= 600.0F) {
               return var3;
            }

            if (var2 == -1) {
               var2 = var3;
            }
         }
      }

      if (var2 != -1) {
         return var2;
      } else {
         int var5 = findHotbar(playerInventory, Items.GLOWSTONE);
         return var5 != -1 && countHotbar(playerInventory, Items.GLOWSTONE) > i ? var5 : -1;
      }
   }

   private static int findHotbar(PlayerInventory playerInventory, Item item) {
      for (int var2 = 0; var2 < 9; var2++) {
         if (playerInventory.getStack(var2).isOf(item)) {
            return var2;
         }
      }

      return -1;
   }

   private static int countHotbar(PlayerInventory playerInventory, Item item) {
      int var2 = 0;

      for (int var3 = 0; var3 < 9; var3++) {
         ItemStack var4 = playerInventory.getStack(var3);
         if (var4.isOf(item)) {
            var2 += var4.getCount();
         }
      }

      return var2;
   }

   @Environment(EnvType.CLIENT)
   private static enum Step {
      IDLE,
      GUARD,
      AWAIT_GUARD,
      CHARGE,
      DETONATE,
      REPLACE,
      SWITCH_BACK;

      private Step() {
      }
   }
}
