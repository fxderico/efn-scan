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
import net.minecraft.util.math.Vec3i;
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
      MinecraftClient var2 = MinecraftClient.method_1551();
      PlayerInventory var1;
      if (this.step != SingleAnchorMacroModule.Step.IDLE
         && var2.field_1724 != null
         && !(var2.field_1755 instanceof HandledScreen)
         && this.switchBackDue(var1 = var2.field_1724.method_31548())) {
         var1.method_61496(this.previousSlot);
      }

      this.stop();
   }

   @EventHandler
   public void onTick(TickEvent var1) {
      this.tick++;
      MinecraftClient var2 = MinecraftClient.method_1551();
      ClientPlayerEntity var3 = var2.field_1724;
      if (var3 != null && var2.field_1687 != null && var2.field_1761 != null && !var3.method_29504()) {
         LootLatch.update(var2, var3);
         LootGuard.update(var2.field_1687, var3);
         PlayerInventory var4 = var3.method_31548();
         if (var2.field_1755 != null) {
            if (this.step != SingleAnchorMacroModule.Step.IDLE && this.step != SingleAnchorMacroModule.Step.SWITCH_BACK) {
               this.wrapUp(var4);
            }
         } else if (!AutoEatModule.isEating() && !AutoInvTotemModule.wantsHotbar()) {
            if (this.step != SingleAnchorMacroModule.Step.IDLE && (this.hotbarMovedByPlayer(var4) || var2.field_1687 != this.activeLevel)) {
               this.stop();
            }

            if ((LootLatch.latched() || LootLatch.killedNow())
               && this.step != SingleAnchorMacroModule.Step.IDLE
               && this.step != SingleAnchorMacroModule.Step.SWITCH_BACK) {
               this.wrapUp(var4);
            }

            if (LootLatch.latched() && this.step == SingleAnchorMacroModule.Step.IDLE) {
               if (!var3.method_6115() && anchorInPlay(var2, var3)) {
                  suppressUse(var2);
               }
            } else if (this.step != SingleAnchorMacroModule.Step.IDLE || this.tryStart(var2, var3)) {
               swallowClicks(var2);
               if (this.step == SingleAnchorMacroModule.Step.SWITCH_BACK) {
                  if (!MeleeAttacks.actedThisTick(var3)) {
                     if (this.switchBackDue(var4)) {
                        var4.method_61496(this.previousSlot);
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
   public void onPacketApplied(PacketAppliedEvent var1) {
      if (this.step == SingleAnchorMacroModule.Step.AWAIT_GUARD && this.guard != null) {
         BlockPos var2 = this.guard.pos();
         if (var1.packet instanceof BlockUpdateS2CPacket var4) {
            if (var4.method_11309().equals(var2)) {
               this.guardReported = var4.method_11308();
            }
         } else if (var1.packet instanceof ChunkDeltaUpdateS2CPacket var6) {
            var6.method_30621((var2x, var3) -> {
               if (var2x.equals(var2)) {
                  this.guardReported = var3;
               }
            });
         }
      }
   }

   private boolean tryStart(MinecraftClient var1, ClientPlayerEntity var2) {
      if (MeleeAttacks.rightClickComing(var1) && !CombatMacroLock.heldByOther(this)) {
         if (!var2.method_7325()
            && canClick(var2)
            && !MeleeAttacks.actedThisTick(var2)
            && MeleeAttacks.handsFree(var1, var2)
            && !var2.method_6079().method_31574(Items.field_8801)) {
            HitResult var7 = var2.method_5745(var2.method_55754(), 1.0F, false);
            BlockHitResult var6;
            if (var7 instanceof BlockHitResult && (var6 = (BlockHitResult)var7).method_17783() == Type.field_1332) {
               ClientWorld var8 = var1.field_1687;
               BlockState var9 = var8.method_8320(var6.method_17777());
               boolean var10 = var9.method_27852(Blocks.field_23152);
               BlockPos var5;
               if (var10) {
                  var5 = var6.method_17777().method_10062();
               } else {
                  if (!var2.method_6047().method_31574(Items.field_23141) || ServerRotation.pending()) {
                     return false;
                  }

                  var5 = Crystals.placedObsidian(var8, var2, var6, var9);
               }

               if (var5 == null || var8.method_27983() == World.field_25180) {
                  return false;
               } else if (this.protectLoot.get() && LootGuard.near(Vec3d.method_24953(var5))) {
                  return false;
               } else {
                  PlayerInventory var11 = var2.method_31548();
                  boolean var4 = chargeOf(var9) > 0;
                  if (!var4 && findHotbar(var11, Items.field_8801) == -1) {
                     return false;
                  } else if (AnchorGuard.detonatorSlot(var11) == -1) {
                     return false;
                  } else if (this.blastGuard.get() && this.withoutGuard.is("Cancel") && guardSlot(var11, this.glowstoneNeeded(var4)) == -1) {
                     return false;
                  } else if (!CombatMacroLock.tryAcquire(this)) {
                     return false;
                  } else {
                     this.previousSlot = var11.method_67532();
                     this.activeLevel = var8;
                     this.anchorPos = var5;
                     this.sightline = var6.method_17784();
                     SingleAnchorMacroModule.Step var3 = this.blastGuard.get() ? SingleAnchorMacroModule.Step.GUARD : SingleAnchorMacroModule.Step.CHARGE;
                     if (var10) {
                        this.scheduleNow(var3);
                     } else {
                        AnchorGuard.click(var1, var2, var6);
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

   private static void swallowClicks(MinecraftClient var0) {
      MeleeAttacks.takeAttackClicks(var0);
      suppressUse(var0);
   }

   private static boolean anchorInPlay(MinecraftClient var0, ClientPlayerEntity var1) {
      ItemStack var3 = var1.method_6047();
      if (!var3.method_31574(Items.field_23141) && !var3.method_31574(Items.field_8801)) {
         HitResult var4 = var0.field_1765;
         BlockHitResult var2;
         return var4 instanceof BlockHitResult
            && (var2 = (BlockHitResult)var4).method_17783() == Type.field_1332
            && var0.field_1687.method_8320(var2.method_17777()).method_27852(Blocks.field_23152);
      } else {
         return true;
      }
   }

   private static void suppressUse(MinecraftClient var0) {
      KeyBinding var1 = var0.field_1690.field_1904;
      var1.method_23481(false);

      while (var1.method_1436()) {
      }
   }

   private void runStep(MinecraftClient var1, ClientPlayerEntity var2) {
      switch (this.step) {
         case GUARD:
            this.placeGuard(var1, var2);
            break;
         case AWAIT_GUARD:
            this.awaitGuard(var1, var2);
            break;
         case CHARGE:
            this.charge(var1, var2);
            break;
         case DETONATE:
            this.detonate(var1, var2);
            break;
         case REPLACE:
            this.replace(var1, var2);
      }
   }

   private void expire(PlayerInventory var1) {
      if (this.step != SingleAnchorMacroModule.Step.GUARD && this.step != SingleAnchorMacroModule.Step.AWAIT_GUARD) {
         this.wrapUp(var1);
      } else {
         this.skipGuard(var1);
      }
   }

   private void placeGuard(MinecraftClient var1, ClientPlayerEntity var2) {
      PlayerInventory var4 = var2.method_31548();
      BlockState var5 = var1.field_1687.method_8320(this.anchorPos);
      if (!var5.method_27852(Blocks.field_23152)) {
         this.wrapUp(var4);
      } else {
         AnchorGuard.Spot var6 = AnchorGuard.find(var1.field_1687, var2, this.anchorPos, this.sightline);
         if (var6 != null && var6.standing()) {
            this.guarded();
         } else {
            int var7 = guardSlot(var4, this.glowstoneNeeded(chargeOf(var5) > 0));
            Rotation var8 = ServerRotation.sent();
            BlockHitResult var3 = var6 != null && var8 != null ? AnchorGuard.hitAlong(var1.field_1687, var2, var8, var6) : null;
            if (var6 != null && var7 != -1 && (var3 != null || ServerRotation.canRotate(var2))) {
               if (var3 == null) {
                  ServerRotation.request(RotationUtils.fromTo(var2.method_33571(), var6.aim()));
                  MeleeAttacks.claimTick(var1);
                  this.readyAt = this.tick + 1;
               } else {
                  this.guard = var6;
                  this.guardBlock = Block.method_9503(var4.method_5438(var7).method_7909());
                  this.guardReported = null;
                  this.select(var4, var7);
                  AnchorGuard.click(var1, var2, var3);
                  ServerRotation.request(RotationUtils.fromTo(var2.method_33571(), Vec3d.method_24953((Vec3i)this.anchorPos)));
                  this.guardTries++;
                  if (this.withoutGuard.is("Cancel")) {
                     this.schedule(SingleAnchorMacroModule.Step.AWAIT_GUARD);
                     this.deadline = this.readyAt + Crystals.confirmWindow(var1, var2);
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

   private void awaitGuard(MinecraftClient var1, ClientPlayerEntity var2) {
      boolean var3 = var1.field_1687.method_8320(this.guard.pos()).method_27852(this.guardBlock);
      if (var3 && this.guardReported != null && this.guardReported.method_27852(this.guardBlock)) {
         this.guarded();
      } else if (var3) {
         this.readyAt = this.tick + 1;
      } else if (this.guardTries < 2) {
         this.scheduleNow(SingleAnchorMacroModule.Step.GUARD);
      } else {
         this.skipGuard(var2.method_31548());
      }
   }

   private void guarded() {
      this.guard = null;
      this.scheduleNow(SingleAnchorMacroModule.Step.CHARGE);
   }

   private void skipGuard(PlayerInventory var1) {
      this.guard = null;
      if (this.withoutGuard.is("Cancel")) {
         this.wrapUp(var1);
      } else {
         this.scheduleNow(SingleAnchorMacroModule.Step.CHARGE);
      }
   }

   private void charge(MinecraftClient var1, ClientPlayerEntity var2) {
      PlayerInventory var3 = var2.method_31548();
      BlockState var4 = var1.field_1687.method_8320(this.anchorPos);
      if (!var4.method_27852(Blocks.field_23152)) {
         this.wrapUp(var3);
      } else if (chargeOf(var4) > 0 && !this.refilled) {
         this.scheduleNow(SingleAnchorMacroModule.Step.DETONATE);
      } else {
         int var5 = findHotbar(var3, Items.field_8801);
         if (var5 != -1 && canClick(var2)) {
            BlockHitResult var6 = AnchorGuard.anchorHit(var1.field_1687, var2, this.anchorPos);
            if (var6 == null) {
               this.readyAt = this.tick + 1;
            } else {
               this.select(var3, var5);
               AnchorGuard.click(var1, var2, var6);
               this.schedule(SingleAnchorMacroModule.Step.DETONATE);
            }
         } else {
            this.wrapUp(var3);
         }
      }
   }

   private void detonate(MinecraftClient var1, ClientPlayerEntity var2) {
      PlayerInventory var4 = var2.method_31548();
      if (chargeOf(var1.field_1687.method_8320(this.anchorPos)) != 0 && canClick(var2) && !var2.method_6079().method_31574(Items.field_8801)) {
         BlockHitResult var5 = AnchorGuard.anchorHit(var1.field_1687, var2, this.anchorPos);
         if (var5 == null) {
            this.readyAt = this.tick + 1;
         } else {
            int var6 = this.mode.is("Double")
                  && !this.refilled
                  && var5.method_17780() == Direction.field_11036
                  && this.anchorPos.method_10264() >= var2.method_31478()
                  && findHotbar(var4, Items.field_8801) != -1
               ? findHotbar(var4, Items.field_23141)
               : -1;
            int var3 = var6 != -1 ? var6 : AnchorGuard.detonatorSlot(var4);
            if (var3 == -1) {
               this.wrapUp(var4);
            } else if (this.lethal(var2, var4.method_5438(var3))) {
               this.wrapUp(var4);
            } else {
               this.select(var4, var3);
               AnchorGuard.click(var1, var2, var5);
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

   private void replace(MinecraftClient var1, ClientPlayerEntity var2) {
      PlayerInventory var4 = var2.method_31548();
      Rotation var5 = ServerRotation.sent();
      Vec3d var6 = var2.method_33571();
      boolean var3 = var5 != null
         && new Box(this.anchorPos)
            .method_992(var6, var6.method_1019(Vec3d.method_1030(var5.pitch(), var5.yaw()).method_1021(var2.method_55754())))
            .isPresent();
      if (this.replaceHit != null && var3 && var2.method_6047().method_31574(Items.field_23141) && canClick(var2)) {
         AnchorGuard.click(var1, var2, this.replaceHit);
         this.replaceHit = null;
         this.schedule(SingleAnchorMacroModule.Step.CHARGE);
      } else {
         this.wrapUp(var4);
      }
   }

   private boolean lethal(ClientPlayerEntity var1, ItemStack var2) {
      if (!var2.method_31574(Items.field_8288) && !var1.method_6079().method_31574(Items.field_8288)) {
         float var3 = CrystalDamage.estimate(Vec3d.method_24953((Vec3i)this.anchorPos), var1, 5.0F, this.anchorPos);
         return CrystalDamage.endangers(var1, var3);
      } else {
         return false;
      }
   }

   private void wrapUp(PlayerInventory var1) {
      if (this.switchBackDue(var1)) {
         this.step = SingleAnchorMacroModule.Step.SWITCH_BACK;
      } else {
         this.stop();
      }
   }

   private boolean switchBackDue(PlayerInventory var1) {
      return this.switchBack.get()
         && PlayerInventory.method_7380(this.previousSlot)
         && this.ownSlot != -1
         && this.ownSlot != this.previousSlot
         && var1.method_67532() == this.ownSlot;
   }

   private boolean hotbarMovedByPlayer(PlayerInventory var1) {
      return var1.method_67532() != (this.ownSlot != -1 ? this.ownSlot : this.previousSlot);
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

   private void schedule(SingleAnchorMacroModule.Step var1) {
      this.step = var1;
      this.readyAt = this.tick + 1 + this.delay.getInt();
      this.deadline = this.readyAt + 5;
   }

   private void scheduleNow(SingleAnchorMacroModule.Step var1) {
      this.step = var1;
      this.readyAt = this.tick;
      this.deadline = this.tick + 5;
   }

   private void select(PlayerInventory var1, int var2) {
      if (var1.method_67532() != var2) {
         var1.method_61496(var2);
         this.ownSlot = var2;
      }
   }

   private static boolean canClick(ClientPlayerEntity var0) {
      return !var0.method_21823();
   }

   private static int chargeOf(BlockState var0) {
      return var0.method_27852(Blocks.field_23152) ? (Integer)var0.method_11654(RespawnAnchorBlock.field_23153) : 0;
   }

   private int glowstoneNeeded(boolean var1) {
      int var2 = var1 ? 0 : 1;
      return this.mode.is("Double") ? var2 + 1 : var2;
   }

   private static int guardSlot(PlayerInventory var0, int var1) {
      int var2 = -1;

      for (int var3 = 0; var3 < 9; var3++) {
         ItemStack var4 = var0.method_5438(var3);
         if (!var4.method_31574(Items.field_8801) && AnchorGuard.isGuardBlock(var4)) {
            if (Block.method_9503(var4.method_7909()).method_9520() >= 600.0F) {
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
         int var5 = findHotbar(var0, Items.field_8801);
         return var5 != -1 && countHotbar(var0, Items.field_8801) > var1 ? var5 : -1;
      }
   }

   private static int findHotbar(PlayerInventory var0, Item var1) {
      for (int var2 = 0; var2 < 9; var2++) {
         if (var0.method_5438(var2).method_31574(var1)) {
            return var2;
         }
      }

      return -1;
   }

   private static int countHotbar(PlayerInventory var0, Item var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < 9; var3++) {
         ItemStack var4 = var0.method_5438(var3);
         if (var4.method_31574(var1)) {
            var2 += var4.method_7947();
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
