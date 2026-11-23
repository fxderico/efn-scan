package net.efn.client.module.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import net.efn.client.chat.ModuleChat;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
public final class AutoTrapModule extends ClientModule {
   private static final Direction[] SUPPORT_ORDER = new Direction[]{
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
   };
   private static final int TARGET_COLOR = -24512;
   private static final int CELL_COLOR = -2130706433;
   private final NumberSetting range = this.addSetting("range", new NumberSetting("Range", 4.0F, 1.0F, 6.0F).step(0.5F));
   private final BooleanSetting rotate = this.addSetting("rotate", new BooleanSetting("Rotate", true));
   private final NumberSetting perTick = this.addSetting("blocks-per-tick", new NumberSetting("Blocks Per Tick", 4.0F, 1.0F, 8.0F).step(1.0F));
   private final BooleanSetting top = this.addSetting("top-block", new BooleanSetting("Top Block", true));
   private final BooleanSetting show = this.addSetting("show-target", new BooleanSetting("Show Target", true));
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final SlotReturn slot = new SlotReturn(this);
   private PlayerEntity target;
   private List<BlockPos> plan = List.of();
   private boolean warnedEmpty;

   public AutoTrapModule() {
      super("auto-trap", "Auto Trap", "Boxes the nearest enemy in with obsidian: sides at feet and head height, then the top.", ModuleCategory.COMBAT);
      this.perTick.visibleWhen(() -> !this.rotate.get());
   }

   @Override
   protected void onEnable() {
      this.target = null;
      this.plan = List.of();
      this.warnedEmpty = false;
      this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.tick());
      this.on(Render3DEvent.TYPE, Stage.POST, render3DEvent -> this.render());
   }

   @Override
   protected void onDisable() {
      this.slot.drop(this.mc, true);
      this.target = null;
      this.plan = List.of();
   }

   private void tick() {
      ClientPlayerEntity var1 = this.mc.player;
      ClientWorld var2 = this.mc.world;
      if (var1 != null && var2 != null && this.mc.interactionManager != null && MeleeAttacks.canAct(this.mc)) {
         this.target = this.pick(var1);
         this.plan = this.target == null ? List.of() : this.cells(var2, var1, this.target);
         if (this.plan.isEmpty()) {
            this.slot.tick(this.mc, true);
         } else {
            int var3 = Crystals.hotbarSlotOf(var1.getInventory(), Items.OBSIDIAN);
            if (var3 < 0) {
               if (!this.warnedEmpty) {
                  ModuleChat.send(this, ModuleChat.body().warn("No obsidian in the hotbar"));
               }

               this.warnedEmpty = true;
               this.slot.tick(this.mc, true);
            } else {
               this.warnedEmpty = false;
               int var4 = this.rotate.get() ? 1 : Math.round(this.perTick.get());
               int var5 = 0;

               for (BlockPos var7 : this.plan) {
                  if (var5 < var4) {
                     BlockHitResult var8 = support(var2, var1, var7);
                     if (var8 == null) {
                        continue;
                     }

                     if (this.rotate.get()) {
                        BlockHitResult var9 = seenByServer(var2, var1, var8);
                        if (var9 == null) {
                           ServerRotation.request(RotationUtils.fromTo(var1.getEyePos(), var8.getPos()));
                           return;
                        }

                        var8 = var9;
                     }

                     if (var5 != 0 || !MeleeAttacks.actedThisTick(var1) && MeleeAttacks.handsFree(this.mc, var1)) {
                        if (!this.slot.take(var1, var3)) {
                           return;
                        }

                        ActionResult var10 = this.mc.interactionManager.interactBlock(var1, Hand.MAIN_HAND, var8);
                        if (var10.isAccepted()) {
                           var1.swingHand(Hand.MAIN_HAND);
                           MeleeAttacks.noteAction(var1);
                           var5++;
                        }
                        continue;
                     }

                     return;
                  }
                  break;
               }

               if (this.rotate.get() && var5 > 0) {
                  ServerRotation.request(ServerRotation.sent());
               }

               return;
            }
         }
      } else {
         this.slot.tick(this.mc, true);
      }
   }

   private PlayerEntity pick(ClientPlayerEntity clientPlayerEntity) {
      double var2 = this.range.get() + 1.5;
      PlayerEntity var4 = null;

      for (PlayerEntity var6 : this.mc.world.getPlayers()) {
         double var7;
         if (var6 != clientPlayerEntity
            && var6.isAlive()
            && MeleeAttacks.isEnemy(clientPlayerEntity, var6)
            && (var7 = var6.distanceTo(clientPlayerEntity)) <= var2
            && (var4 == null || var7 < var4.distanceTo(clientPlayerEntity))) {
            var4 = var6;
         }
      }

      return var4;
   }

   private List<BlockPos> cells(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, PlayerEntity playerEntity) {
      Box var4 = playerEntity.getBoundingBox();
      int var5 = MathHelper.floor(var4.minY + 0.01);
      LinkedHashSet var6 = new LinkedHashSet();

      for (int var7 = MathHelper.floor(var4.minX); var7 <= MathHelper.floor(var4.maxX - 1.0E-4); var7++) {
         for (int var8 = MathHelper.floor(var4.minZ); var8 <= MathHelper.floor(var4.maxZ - 1.0E-4); var8++) {
            var6.add(BlockPos.asLong(var7, 0, var8));
         }
      }

      ArrayList var17 = new ArrayList();

      for (int var18 = 0; var18 <= 1; var18++) {
         ArrayList var9 = new ArrayList();

         for (long var11 : var6) {
            BlockPos var13 = BlockPos.fromLong(var11).withY(var5 + var18);

            for (Direction var15 : Type.HORIZONTAL) {
               BlockPos var16 = var13.offset(var15);
               if (!var6.contains(BlockPos.asLong(var16.getX(), 0, var16.getZ())) && this.open(clientWorld, clientPlayerEntity, var16) && !var9.contains(var16)
                  )
                {
                  var9.add(var16);
               }
            }
         }

         var9.sort(Comparator.comparingDouble(blockPos -> blockPos.getSquaredDistance(clientPlayerEntity.getEyePos())));
         var17.addAll(var9);
      }

      if (this.top.get()) {
         for (long var20 : var6) {
            BlockPos var21 = BlockPos.fromLong(var20).withY(var5 + 2);
            if (this.open(clientWorld, clientPlayerEntity, var21)) {
               var17.add(var21);
            }
         }
      }

      return var17;
   }

   private boolean open(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      BlockState var4 = clientWorld.getBlockState(blockPos);
      if (var4.isReplaceable() && (var4.getFluidState().isEmpty() || var4.getFluidState().isStill())) {
         return blockPos.toCenterPos().distanceTo(clientPlayerEntity.getEyePos()) > clientPlayerEntity.getBlockInteractionRange()
            ? false
            : clientWorld.canPlace(Blocks.OBSIDIAN.getDefaultState(), blockPos, ShapeContext.absent());
      } else {
         return false;
      }
   }

   public static BlockHitResult support(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockPos blockPos) {
      Vec3d var3 = clientPlayerEntity.getEyePos();
      double var4 = clientPlayerEntity.getBlockInteractionRange();

      for (Direction var9 : SUPPORT_ORDER) {
         BlockPos var11 = blockPos.offset(var9);
         BlockState var12 = clientWorld.getBlockState(var11);
         if (!var12.isReplaceable() && !var12.getCollisionShape(clientWorld, var11).isEmpty()) {
            Direction var13 = var9.getOpposite();
            Vec3d var14 = Vec3d.ofCenter(var11).add(var13.getOffsetX() * 0.5, var13.getOffsetY() * 0.5, var13.getOffsetZ() * 0.5);
            BlockHitResult var10;
            if (!(var14.distanceTo(var3) > var4)
               && (var10 = clientWorld.raycast(
                        new RaycastContext(
                           var3, var14.add(Vec3d.of(var9.getVector()).multiply(0.05)), ShapeType.OUTLINE, FluidHandling.NONE, clientPlayerEntity
                        )
                     ))
                     .getType()
                  == net.minecraft.util.hit.HitResult.Type.BLOCK
               && var10.getBlockPos().equals(var11)
               && var10.getSide() == var13) {
               return new BlockHitResult(var14, var13, var11, false);
            }
         }
      }

      return null;
   }

   public static BlockHitResult seenByServer(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, BlockHitResult blockHitResult) {
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
         return var6.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK
               && var6.getBlockPos().equals(blockHitResult.getBlockPos())
               && var6.getSide() == blockHitResult.getSide()
            ? var6
            : null;
      }
   }

   private void render() {
      if (this.show.get() && this.target != null) {
         GizmoDrawing.box(this.target.getBoundingBox(), DrawStyle.stroked(-24512, 2.0F));

         for (BlockPos var2 : this.plan) {
            GizmoDrawing.box(var2, DrawStyle.stroked(-2130706433, 1.0F));
         }
      }
   }
}
