package net.efn.client.easyplace.placement;

import fi.dy.masa.litematica.world.WorldSchematic;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.efn.client.easyplace.EasyPlace;
import net.efn.client.easyplace.compat.BaritoneCompat;
import net.efn.client.easyplace.inventory.InventoryManager;
import net.efn.client.easyplace.inventory.InventoryPickResult;
import net.efn.client.easyplace.mixin.AccessorMouseHandler;
import net.efn.client.easyplace.schematic.SchematicBlockState;
import net.efn.client.easyplace.schematic.SchematicManager;
import net.efn.client.easyplace.schematic.SchematicTarget;
import net.efn.client.easyplace.schematic.TargetStatus;
import net.efn.client.easyplace.sync.BlockAcks;
import net.efn.client.easyplace.sync.ServerAckManager;
import net.efn.client.easyplace.util.BlockStateUtil;
import net.efn.client.easyplace.util.EasyPlaceDebug;
import net.efn.client.easyplace.util.PlacementUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class PlacementManager {
   private static final int ACK_TIMEOUT_TICKS = 40;
   private static final int MAX_SLOWDOWN = 20;
   private static final int MAX_RETRIES = 2;
   private static final int FAIL_BLOCK_TICKS = 20;
   private static final int FAILS_BEFORE_HOLD = 3;
   private static final int MAX_HOLD_TICKS = 600;
   private static final float TURN_STEP = 20.0F;
   private static final float TURN_EASE = 0.42F;
   private static final float TURN_SNAP = 1.0F;
   private static final int NO_CANDIDATE_COOLDOWN = 0;
   private static final int SNEAK_WAIT_TICKS = 20;
   private static final int SHIFT_STEADY_TICKS = 1;
   private final MinecraftClient mc;
   private final SchematicManager schematicManager;
   private final InventoryManager inventoryManager;
   private final DependencyManager dependencyManager;
   private final ServerAckManager ackManager;
   private final PlacementListener listener;
   private final Map<Long, Long> blockedUntil = new HashMap<>();
   private final Map<Long, Long> recentlyPlaced = new HashMap<>();
   private final Map<Long, Integer> failsAt = new HashMap<>();
   private boolean reaimed;
   private boolean turning;
   private float turnYaw;
   private float turnPitch;
   private final Random hand = new Random();
   private PlacementState state = PlacementState.IDLE;
   private SchematicTarget target;
   private InventoryPickResult inventoryPick;
   private PlacementTarget placementTarget;
   private ServerAckManager.Result ackResult = ServerAckManager.Result.IDLE;
   private int slowdown;
   private int goodInRow;
   private int refusedCount;
   private long tick;
   private int retry;
   private BlockPos lastNavTarget;
   private long noCandidateUntil;
   private long lastPlacementTick = -1L;
   private float lastYawDelta = 0.0F;
   private int rotationTicksRemaining = 0;
   private float lastSentPitch = 0.0F / 0.0F;
   private BuildScope scope;
   private PlacementManager.Outcome outcome;
   private Hand placeHand = Hand.field_5808;
   private long placedCount;
   private long lastTargetTick = -9223372036854775808L;
   private int sneakWait;
   private boolean sneakPressed;
   private boolean lastShift;
   private int shiftSteady;
   private long lastPreTick;

   public PlacementManager(MinecraftClient var1, PlacementListener var2) {
      this.mc = var1;
      this.listener = var2;
      this.schematicManager = new SchematicManager(var1);
      this.inventoryManager = new InventoryManager(var1);
      this.dependencyManager = new DependencyManager(var1);
      this.ackManager = new ServerAckManager();
   }

   public void start() {
      this.reset();
      this.state = PlacementState.FIND_TARGET;
   }

   public void setOutcome(PlacementManager.Outcome var1) {
      this.outcome = var1;
   }

   public void setScope(BuildScope var1) {
      if (this.scope != null && var1 == null || this.scope == null && var1 != null) {
         this.schematicManager.invalidate();
      }

      this.scope = var1;
   }

   public long placedCount() {
      return this.placedCount;
   }

   public int refusedCount() {
      return this.refusedCount;
   }

   public boolean placeableFrom(BlockState var1, BlockPos var2, Vec3d var3) {
      return this.dependencyManager.placeableFrom(var1, var2, var3, true);
   }

   public String debugState(BlockPos var1) {
      return "state="
         + this.state
         + " scopeAllows="
         + (this.scope == null ? "noscope" : this.scope.allows(var1))
         + " blocked="
         + this.blockedUntil.containsKey(var1.method_10063())
         + " recent="
         + this.recentlyPlaced.containsKey(var1.method_10063())
         + " noCand="
         + (this.noCandidateUntil - this.tick)
         + " lastTarget="
         + this.ticksWithoutTarget()
         + " target="
         + (this.target == null ? "-" : this.target.pos().method_23854())
         + " sincePre="
         + (this.tick - this.lastPreTick)
         + " searchNow="
         + describe(
            this.schematicManager.getSchematicWorld() == null
               ? null
               : this.dependencyManager
                  .searchForPlaceables(this.schematicManager.getSchematicWorld(), true, false, 360.0F, 180.0, 0.0F, this.scope, this.blockedUntil::containsKey)
         );
   }

   private static String describe(PlacementTarget var0) {
      return var0 == null ? "none" : var0.targetPos().method_23854();
   }

   public long ticksWithoutTarget() {
      return this.lastTargetTick == -9223372036854775808L ? 9223372036854775807L : this.tick - this.lastTargetTick;
   }

   public void abort() {
      this.failCurrent();
   }

   public boolean busy() {
      return this.state == PlacementState.CALCULATE_ROTATION
         || this.state == PlacementState.WAIT_ROTATION
         || this.state == PlacementState.EXECUTE_PLACE
         || this.state == PlacementState.WAIT_ACK;
   }

   public void tickPre(PlacementConfig var1) {
      if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1761 != null) {
         this.lastPreTick = this.tick;
         if (this.state == PlacementState.IDLE || this.state == PlacementState.FIND_TARGET) {
            if (this.lastPlacementTick >= 0L && this.tick - this.lastPlacementTick <= var1.tickDelay() + this.slowdown) {
               this.state = PlacementState.FIND_TARGET;
               return;
            }

            this.resolve(var1);
         }

         if (this.state == PlacementState.CALCULATE_ROTATION) {
            this.prepareRotation(var1);
         }

         if (this.state == PlacementState.EXECUTE_PLACE) {
            this.lastPlacementTick = this.tick;
            this.executePlace(var1);
         }
      }
   }

   public void reset() {
      this.releaseSneak();
      this.state = PlacementState.IDLE;
      this.target = null;
      this.inventoryPick = null;
      this.placementTarget = null;
      this.ackResult = ServerAckManager.Result.IDLE;
      this.retry = 0;
      this.recentlyPlaced.clear();
      this.blockedUntil.clear();
      this.failsAt.clear();
      this.reaimed = false;
      this.turning = false;
      this.ackManager.clear();
      this.inventoryManager.reset();
      this.schematicManager.clear();
      this.lastNavTarget = null;
      this.noCandidateUntil = 0L;
      this.lastYawDelta = 0.0F;
      this.rotationTicksRemaining = 0;
      this.lastSentPitch = 0.0F / 0.0F;
      this.slowdown = 0;
      this.goodInRow = 0;
      this.refusedCount = 0;
   }

   public void tick(PlacementConfig var1) {
      this.tick++;
      if (this.mc.field_1724 != null) {
         boolean var2 = this.mc.field_1724.method_5715();
         this.shiftSteady = var2 == this.lastShift ? this.shiftSteady + 1 : 0;
         this.lastShift = var2;
      }

      this.blockedUntil.entrySet().removeIf(var1x -> var1x.getValue() <= this.tick);
      this.recentlyPlaced.entrySet().removeIf(var1x -> var1x.getValue() <= this.tick);
      if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1761 != null) {
         if (this.state == PlacementState.WAIT_ROTATION && this.turning) {
            if (this.mc.field_1724 != null && this.turnTowards(this.mc.field_1724)) {
               this.turning = false;
               this.rotationTicksRemaining = 1;
            }
         } else if (this.state == PlacementState.WAIT_ROTATION) {
            if (this.rotationTicksRemaining > 0) {
               this.rotationTicksRemaining--;
            } else {
               this.state = PlacementState.EXECUTE_PLACE;
            }
         }

         if (this.mc.field_1755 == null || this.state == PlacementState.WAIT_ACK) {
            if (this.state == PlacementState.WAIT_ACK) {
               this.waitForAck(var1);
            }
         }
      } else {
         this.softReset();
      }
   }

   private void resolve(PlacementConfig var1) {
      if (!this.schematicManager.hasActiveSchematic()) {
         if (this.state != PlacementState.FIND_TARGET) {
            this.state = PlacementState.FIND_TARGET;
            this.listener.feedback("no-schematic", "No active Litematica schematic", 100);
         }
      } else if (this.tick >= this.noCandidateUntil) {
         WorldSchematic var2 = this.schematicManager.getSchematicWorld();
         if (var2 == null) {
            this.state = PlacementState.FIND_TARGET;
         } else {
            this.placementTarget = this.dependencyManager
               .searchForPlaceables(
                  var2,
                  var1.hopperForceFacing(),
                  var1.fovFilter(),
                  (float)var1.fovDegrees(),
                  var1.maxYawChange(),
                  this.lastYawDelta,
                  this.scope,
                  this.blockedUntil::containsKey
               );
            if (this.placementTarget == null) {
               if (!var1.schematicOnly()
                  && this.schematicManager
                     .getTargets(this.tick, var1.auraRange(), var1.schematicAura(), var1.allBlocks())
                     .stream()
                     .anyMatch(var0 -> var0.status() == TargetStatus.WRONG)) {
                  this.listener.feedback("wrong-safe-skip", "Wrong blocks detected; automatic breaking is disabled for safety", 100);
               }

               this.noCandidateUntil = this.tick + 0L;
               if (this.scope == null && var1.baritoneWalk() && BaritoneCompat.isAvailable()) {
                  SchematicTarget var8 = this.schematicManager.nearestMissingForNavigation(this.tick, var1.allBlocks());
                  if (var8 != null) {
                     BlockPos var9 = var8.pos();
                     if (!var9.equals(this.lastNavTarget)) {
                        this.lastNavTarget = var9;
                        int var10 = Math.max(1, (int)var1.auraRange() - 1);
                        BaritoneCompat.goNear(var9, var10);
                        this.listener.feedback("baritone-nav", "Walking to " + var9.method_23854(), 40);
                     }
                  } else if (this.lastNavTarget != null) {
                     BaritoneCompat.cancel();
                     this.lastNavTarget = null;
                  }
               }

               this.state = PlacementState.FIND_TARGET;
            } else {
               this.lastTargetTick = this.tick;
               if (this.lastNavTarget != null) {
                  BaritoneCompat.cancel();
                  this.lastNavTarget = null;
               }

               BlockPos var3 = this.placementTarget.targetPos();
               BlockState var4 = var2.method_8320(var3);
               BlockState var5 = this.mc.field_1687.method_8320(var3);
               TargetStatus var6 = BlockStateUtil.classify(var4, var5);
               this.target = new SchematicTarget(
                  var3, SchematicBlockState.of(var4), var5, var6, var3.method_46558().method_1025(this.mc.field_1724.method_33571())
               );
               Item var7 = BlockStateUtil.requiredItem(this.target.expected().state());
               if (var7 == null) {
                  this.listener.feedback("unsupported-" + this.target.expected().blockId(), "No placeable item for: " + this.target.expected().blockId(), 100);
                  this.blockedUntil.put(this.target.pos().method_10063(), this.tick + 1L);
                  this.schematicManager.invalidate();
                  this.target = null;
                  this.placementTarget = null;
                  this.state = PlacementState.FIND_TARGET;
               } else {
                  this.inventoryPick = this.inventoryManager.prepare(var1.inventoryPickSource(), var7, this.tick, this.scope != null);
                  if (this.inventoryPick.status() != InventoryPickResult.Status.WAITING) {
                     if (this.inventoryPick.status() == InventoryPickResult.Status.MISSING) {
                        this.listener
                           .feedback(
                              "missing-item-" + Registries.field_41178.method_10221(var7), "Missing item: " + Registries.field_41178.method_10221(var7), 100
                           );
                        this.blockedUntil.put(this.target.pos().method_10063(), this.tick + 1L);
                        this.schematicManager.invalidate();
                        this.target = null;
                        this.inventoryPick = null;
                        this.placementTarget = null;
                        this.state = PlacementState.FIND_TARGET;
                     } else {
                        this.placeHand = Hand.field_5808;
                        if (this.scope != null) {
                           if (this.inventoryPick.hand() == Hand.field_5810) {
                              this.placeHand = Hand.field_5810;
                           } else {
                              if (this.inventoryPick.slot() < 0 || this.inventoryPick.slot() >= 9) {
                                 this.state = PlacementState.FIND_TARGET;
                                 return;
                              }

                              this.placementTarget = withSlot(this.placementTarget, this.inventoryPick.slot());
                           }
                        }

                        this.debug(var1, "target-selected");
                        this.state = PlacementState.CALCULATE_ROTATION;
                     }
                  }
               }
            }
         }
      }
   }

   private void prepareRotation(PlacementConfig var1) {
      if (this.mc.field_1724 != null && this.placementTarget != null && this.mc.field_1761 != null) {
         ClientPlayerEntity var3 = this.mc.field_1724;
         boolean var2 = Clicks.usesClick((World)this.mc.field_1687, this.placementTarget.neighborPos(), this.scope != null) || this.placementTarget.sneak();
         if (this.scope != null) {
            this.mc.field_1690.field_1832.method_23481(var2);
            this.sneakPressed = var2;
            if (var3.method_5715() != var2 || this.shiftSteady < 1) {
               if (++this.sneakWait > 20) {
                  if (Boolean.getBoolean("easyplace.builder.debug")) {
                     EasyPlace.LOG
                        .info(
                           "[builder] sneak never settled: want {} shift {} key {} steady {} target {}",
                           new Object[]{
                              var2,
                              var3.method_5715(),
                              this.mc.field_1690.field_1832.method_1434(),
                              this.shiftSteady,
                              this.placementTarget.targetPos().method_23854()
                           }
                        );
                  }

                  this.failCurrent();
               }

               return;
            }
         } else if (var2 && !var3.method_5715()) {
            return;
         }

         this.sneakWait = 0;
         if (this.placeHand == Hand.field_5808 && var3.method_31548().method_67532() != this.placementTarget.hotbarSlot()) {
            var3.method_31548().method_61496(this.placementTarget.hotbarSlot());
         }

         if (var1.rotationSpoof()) {
            this.zeroMouseDeltas();
            float var5 = this.placementTarget.yaw();
            float var6 = this.placementTarget.pitch();
            float var7 = var3.method_36454();
            float var8 = MathHelper.method_15393(var5 - var7);
            float var9 = var7 + var8;
            float var10 = Math.abs(var6 - (Float.isNaN(this.lastSentPitch) ? var3.method_36455() : this.lastSentPitch));
            float var11 = Math.abs(var8);
            this.rotationTicksRemaining = !(var10 > 15.0F) && !(var11 > 45.0F) ? 0 : 1;
            this.lastYawDelta = var8;
            if (this.scope != null) {
               this.startTurn(var5, var6);
               return;
            }

            this.lastSentPitch = var6;
            var3.method_36456(var9);
            var3.method_36457(var6);
            this.state = PlacementState.WAIT_ROTATION;
         } else {
            this.state = PlacementState.EXECUTE_PLACE;
         }

         this.debug(var1, "rotation-prepared");
      } else {
         this.failCurrent();
      }
   }

   private void startTurn(float var1, float var2) {
      this.turnYaw = var1;
      this.turnPitch = var2;
      this.turning = true;
      this.state = PlacementState.WAIT_ROTATION;
   }

   private boolean turnTowards(ClientPlayerEntity var1) {
      this.zeroMouseDeltas();
      float var2 = MathHelper.method_15393(this.turnYaw - var1.method_36454());
      float var3 = this.turnPitch - var1.method_36455();
      if (Math.abs(var2) < 1.0F && Math.abs(var3) < 1.0F) {
         var1.method_36456(var1.method_36454() + var2);
         var1.method_36457(this.turnPitch);
         this.lastSentPitch = this.turnPitch;
         return true;
      } else {
         float var4 = 0.42F * (0.85F + this.hand.nextFloat() * 0.3F);
         float var5 = Math.abs(var2) < 1.0F ? var2 : MathHelper.method_15363(var2 * var4, -20.0F, 20.0F);
         float var6 = Math.abs(var3) < 1.0F ? var3 : MathHelper.method_15363(var3 * var4, -20.0F, 20.0F);
         var1.method_36456(var1.method_36454() + var5);
         var1.method_36457(MathHelper.method_15363(var1.method_36455() + var6, -90.0F, 90.0F));
         return false;
      }
   }

   private void zeroMouseDeltas() {
      if (this.mc.field_1729 != null) {
         AccessorMouseHandler var1 = (AccessorMouseHandler)this.mc.field_1729;
         var1.easyplace$setAccumulatedDX(0.0);
         var1.easyplace$setAccumulatedDY(0.0);
      }
   }

   private void executePlace(PlacementConfig var1) {
      if (this.mc.field_1724 != null && this.placementTarget != null && this.mc.field_1761 != null) {
         ClientPlayerEntity var2 = this.mc.field_1724;
         if (var2.method_5829().method_994(new Box(this.placementTarget.targetPos()))) {
            this.traceFail("body in the cell");
            this.failCurrent();
            this.noCandidateUntil = this.tick + 2L;
         } else {
            double var3 = var2.method_55754();
            BlockHitResult var5 = PlacementUtil.rayTraceTowards(var2, var2.method_36454(), var2.method_36455(), var3);
            if (var5 != null
               && var5.method_17783() == Type.field_1332
               && var5.method_17777().equals(this.placementTarget.neighborPos())
               && var5.method_17780() == this.placementTarget.clickedFace()) {
               if (this.scope == null
                  || this.placeHand != Hand.field_5808
                  || this.target == null
                  || this.dependencyManager
                     .verify(
                        this.target.expected().state(),
                        this.placementTarget.targetPos(),
                        var5,
                        var2.method_36454(),
                        var2.method_36455(),
                        var2.method_31548().method_67532(),
                        var2.method_5715()
                     )) {
                  BlockState var12 = this.mc.field_1687.method_8320(this.placementTarget.targetPos());
                  ActionResult var13 = this.mc.field_1761.method_2896(var2, this.placeHand, var5);
                  if (Boolean.getBoolean("easyplace.builder.debug")
                     && this.target != null
                     && this.target.expected().state().method_26204() instanceof ChestBlock) {
                     EasyPlace.LOG
                        .info(
                           "[builder] chest at {} want {} got {} (was {}) shift={} sneakFlag={} against {} face {}",
                           new Object[]{
                              this.placementTarget.targetPos().method_23854(),
                              this.target.expected().state(),
                              this.mc.field_1687.method_8320(this.placementTarget.targetPos()),
                              var12,
                              var2.method_5715(),
                              this.placementTarget.sneak(),
                              this.placementTarget.neighborPos().method_23854(),
                              var5.method_17780()
                           }
                        );
                  }

                  this.releaseSneak();
                  if (var13.method_23665()) {
                     var2.method_6104(this.placeHand);
                     this.ackManager
                        .begin(
                           this.target,
                           this.tick,
                           40,
                           this.retry,
                           BlockAcks.lastSent(this.mc.field_1687),
                           this.mc.field_1687.method_8320(this.placementTarget.targetPos())
                        );
                     this.ackResult = ServerAckManager.Result.WAITING;
                     this.state = PlacementState.WAIT_ACK;
                     this.debug(var1, "placement-sent");
                  } else {
                     this.traceFail("use rejected: " + var13);
                     this.debug(var1, "placement-rejected");
                     this.failCurrent();
                     this.noCandidateUntil = this.tick + 2L;
                  }
               } else if (!this.reaimed && this.placementTarget.hitCursor() != null) {
                  this.reaimed = true;
                  float[] var11 = PlacementUtil.calcRotation(PlacementUtil.getEyePos(var2), this.placementTarget.hitCursor());
                  if (this.scope != null) {
                     this.startTurn(var11[0], var11[1]);
                  } else {
                     this.zeroMouseDeltas();
                     var2.method_36456(var2.method_36454() + MathHelper.method_15393(var11[0] - var2.method_36454()));
                     var2.method_36457(var11[1]);
                     this.lastSentPitch = var11[1];
                     this.rotationTicksRemaining = 0;
                     this.state = PlacementState.WAIT_ROTATION;
                  }
               } else {
                  this.traceFail(
                     "would not come out right, holding "
                        + var2.method_31548().method_5438(var2.method_31548().method_67532())
                        + " shift "
                        + var2.method_5715()
                        + " face "
                        + var5.method_17780()
                        + " hit "
                        + var5.method_17784()
                        + " planned "
                        + this.placementTarget.hitCursor()
                        + " eye "
                        + PlacementUtil.getEyePos(var2)
                        + " yaw "
                        + var2.method_36454()
                        + " pitch "
                        + var2.method_36455()
                        + " got "
                        + this.dependencyManager
                           .wouldBe(
                              this.placementTarget.targetPos(),
                              var5,
                              var2.method_36454(),
                              var2.method_36455(),
                              var2.method_31548().method_67532(),
                              var2.method_5715()
                           )
                  );
                  this.failCurrent();
                  this.noCandidateUntil = this.tick + 1L;
               }
            } else {
               Vec3d var6 = PlacementUtil.getEyePos(var2);
               if (!PlacementUtil.isFaceVisibleToEye(this.placementTarget.neighborPos(), this.placementTarget.clickedFace(), var6)) {
                  this.traceFail("face not visible any more");
                  this.failCurrent();
                  this.noCandidateUntil = this.tick + 1L;
               } else {
                  Vec3d var7 = this.placementTarget.hitCursor() != null
                     ? this.placementTarget.hitCursor()
                     : PlacementUtil.faceCenterCursor(this.placementTarget.neighborPos(), this.placementTarget.clickedFace());
                  float[] var8 = PlacementUtil.calcRotation(var6, var7);
                  BlockHitResult var9 = PlacementUtil.rayTraceTowards(var2, var8[0], var8[1], var3);
                  if (var9 != null
                     && var9.method_17783() == Type.field_1332
                     && var9.method_17777().equals(this.placementTarget.neighborPos())
                     && var9.method_17780() == this.placementTarget.clickedFace()) {
                     if (this.scope != null) {
                        this.startTurn(var8[0], var8[1]);
                     } else {
                        this.zeroMouseDeltas();
                        float var10 = MathHelper.method_15393(var8[0] - var2.method_36454());
                        var2.method_36456(var2.method_36454() + var10);
                        var2.method_36457(var8[1]);
                        this.lastSentPitch = var8[1];
                        this.rotationTicksRemaining = 0;
                        this.state = PlacementState.WAIT_ROTATION;
                     }
                  } else {
                     this.traceFail("aim hits " + (var9 == null ? "nothing" : var9.method_17777().method_23854() + " " + var9.method_17780()));
                     this.failCurrent();
                     this.noCandidateUntil = this.tick + 1L;
                  }
               }
            }
         }
      } else {
         this.failCurrent();
      }
   }

   private void waitForAck(PlacementConfig var1) {
      this.ackResult = this.ackManager.poll(this.mc.field_1687, this.tick);
      if (this.ackResult != ServerAckManager.Result.WAITING) {
         if (this.ackResult == ServerAckManager.Result.CONFIRMED) {
            if (this.slowdown > 0 && ++this.goodInRow >= 10) {
               this.slowdown--;
               this.goodInRow = 0;
            }

            this.listener.feedback("confirmed-" + this.target.pos().method_10063(), "Placement confirmed: " + this.target.pos().method_23854(), 1);
            this.debug(var1, "confirmed");
            this.recentlyPlaced.put(this.target.pos().method_10063(), this.tick + 1L);
            this.failsAt.remove(this.target.pos().method_10063());
            this.reaimed = false;
            this.placedCount++;
            if (this.outcome != null) {
               this.outcome.placed(this.target.pos());
            }

            this.schematicManager.invalidate();
            this.target = null;
            this.inventoryPick = null;
            this.placementTarget = null;
            this.ackResult = ServerAckManager.Result.IDLE;
            this.retry = 0;
            this.ackManager.clear();
            this.inventoryManager.reset();
            this.state = PlacementState.FIND_TARGET;
         } else {
            this.goodInRow = 0;
            this.slowdown = Math.min(20, this.slowdown + 3);
            this.refusedCount++;
            if (Boolean.getBoolean("easyplace.builder.debug")) {
               EasyPlace.LOG
                  .info(
                     "[builder] server {} the block at {}, slowing to {} extra ticks",
                     new Object[]{
                        this.ackResult == ServerAckManager.Result.REFUSED ? "refused" : "never answered", this.target.pos().method_23854(), this.slowdown
                     }
                  );
            }

            if (this.retry < 2) {
               this.retry++;
               this.ackManager.clear();
               this.placementTarget = null;
               this.state = PlacementState.CALCULATE_ROTATION;
               this.listener.feedback("retry-" + this.target.pos().method_10063(), "Placement ACK timeout; retry " + this.retry + "/2", 10);
               this.debug(var1, "ack-timeout-retry");
            } else {
               this.failCurrent();
            }
         }
      }
   }

   private static PlacementTarget withSlot(PlacementTarget var0, int var1) {
      return var0.hotbarSlot() == var1
         ? var0
         : new PlacementTarget(
            var0.targetPos(),
            var0.neighborPos(),
            var0.placeSide(),
            var0.clickedFace(),
            var1,
            var0.yaw(),
            var0.pitch(),
            var0.score(),
            var0.hitCursor(),
            var0.sneak()
         );
   }

   private void traceFail(String var1) {
      if (Boolean.getBoolean("easyplace.builder.debug") && this.placementTarget != null) {
         EasyPlace.LOG
            .info(
               "[builder] place fail {} {}: {}",
               new Object[]{this.placementTarget.targetPos().method_23854(), this.target == null ? "?" : this.target.expected().state(), var1}
            );
      }
   }

   private void releaseSneak() {
      if (this.sneakPressed) {
         this.sneakPressed = false;
         this.sneakWait = 0;
         this.mc.field_1690.field_1832.method_23481(false);
      }
   }

   private void failCurrent() {
      this.releaseSneak();
      if (this.target != null) {
         long var1 = this.target.pos().method_10063();
         int var3 = this.failsAt.merge(var1, 1, Integer::sum);
         if (var3 >= 3) {
            this.blockedUntil.put(var1, this.tick + Math.min(600, 20 << Math.min(5, var3 - 3)));
         }

         if (this.outcome != null) {
            this.outcome.failed(this.target.pos());
         }
      }

      this.reaimed = false;
      this.turning = false;
      this.schematicManager.invalidate();
      this.target = null;
      this.inventoryPick = null;
      this.placementTarget = null;
      this.ackResult = ServerAckManager.Result.IDLE;
      this.retry = 0;
      this.ackManager.clear();
      this.inventoryManager.reset();
      this.state = PlacementState.FIND_TARGET;
   }

   private void softReset() {
      this.releaseSneak();
      this.reaimed = false;
      this.turning = false;
      this.state = PlacementState.FIND_TARGET;
      this.target = null;
      this.ackManager.clear();
      this.inventoryManager.reset();
      this.schematicManager.clear();
   }

   private void debug(PlacementConfig var1, String var2) {
      if (var1.debug()) {
         float var3 = this.placementTarget != null ? this.placementTarget.yaw() : 0.0F / 0.0F;
         float var4 = this.placementTarget != null ? this.placementTarget.pitch() : 0.0F / 0.0F;
         EasyPlaceDebug.log(var2, this.state, this.placementTarget, new float[]{var3, var4}, this.ackResult, this.retry);
      }
   }

   public PlacementState getState() {
      return this.state;
   }

   @Environment(EnvType.CLIENT)
   public interface Outcome {
      void placed(BlockPos var1);

      void failed(BlockPos var1);
   }
}
